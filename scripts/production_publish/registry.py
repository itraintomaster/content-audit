"""El registro de producción (F-PUBPROD R008 y R009).

  <workdir>/.content-audit/production/registry.jsonl
      Append-only, un renglón JSON por corrida (ensayo, aplicación o negada),
      escrito con fsync. Una aplicación deja un renglón STARTED antes de la
      primera escritura y uno final. Si no se puede escribir el renglón de
      inicio, no se escribe en la base.
  <workdir>/.content-audit/production/runs/<runId>.json
      El detalle de la corrida: ids y, por campo escrito, `old` y `new`
      (el respaldo). Se escribe antes de la primera escritura y se completa al final.

El destino se registra como {host, db}: el servidor y la base, nunca usuario,
clave ni el URI (que puede traer credenciales en el usuario o en sus opciones).
"""

import contextlib
import json
import os
import re
import secrets
import tempfile
from datetime import datetime, timezone
from urllib.parse import urlsplit

from .store import to_json_safe

DRY_RUN = "DRY_RUN"
APPLY = "APPLY"

DRY_RUN_CLEAN = "DRY_RUN_CLEAN"
DRY_RUN_WITH_PROBLEMS = "DRY_RUN_WITH_PROBLEMS"
REFUSED = "REFUSED"
ABORTED = "ABORTED"
STARTED = "STARTED"
APPLIED_VERIFIED = "APPLIED_VERIFIED"
VERIFY_FAILED = "VERIFY_FAILED"

URI_SCHEMES = ("mongodb", "mongodb+srv")
# Un servidor: nombre o [IPv6], con puerto numérico opcional. Nada más.
HOST = re.compile(r"(\[[0-9a-f:.]+\]|[a-z0-9._%-]+)(:[0-9]{1,5})?\Z")


class RegistryError(Exception):
    """No se pudo leer o escribir el registro."""


class RegistryBusy(RegistryError):
    """Otra corrida tiene tomado el registro."""


def destination_of(uri, db):
    """{host, db} del URI: solo los servidores (con puerto si lo trae) y la base.

    Lanza ValueError con un mensaje que nunca repite el URI.
    """
    if not isinstance(uri, str) or not uri:
        raise ValueError("falta el URI de la base")
    try:
        parts = urlsplit(uri)
    except ValueError:
        raise ValueError("el URI de la base no se puede leer")
    if parts.scheme not in URI_SCHEMES:
        raise ValueError("el URI de la base tiene que empezar con mongodb:// o mongodb+srv://")
    # Una @ después de los servidores es una clave con caracteres sin escapar: lo que
    # urlsplit llamaría servidor podría ser parte de la clave. No se registra nada.
    if "@" in parts.path or "@" in parts.query or "@" in parts.fragment:
        raise ValueError("el URI de la base está mal formado (¿la clave tiene caracteres sin escapar?)")
    hosts = parts.netloc.rpartition("@")[2]
    names = [h.strip().lower() for h in hosts.split(",") if h.strip()]
    if not names or not all(HOST.match(n) for n in names):
        raise ValueError("el URI de la base no dice a qué servidor conectarse")
    if not isinstance(db, str) or not db:
        raise ValueError("falta el nombre de la base")
    return {"host": ",".join(names), "db": db}


def destination_label(destination):
    return "%s/%s" % (destination["host"], destination["db"]) if destination else "(sin destino)"


def parse_expected_destination(text):
    """`host/db` tal como lo escribe José en --expect-destination."""
    if not isinstance(text, str) or "/" not in text:
        raise ValueError("--expect-destination tiene que ser servidor/base, por ejemplo "
                         "cluster-ejemplo.mongodb.net/learney")
    host, _, db = text.rpartition("/")
    names = [h.strip().lower() for h in host.split(",") if h.strip()]
    host = ",".join(names)
    if not names or not all(HOST.match(n) for n in names) or not db or ":" in db or "@" in db:
        raise ValueError("--expect-destination tiene que ser servidor/base, sin usuario ni clave")
    return {"host": host, "db": db}


def new_run_id(now):
    return "%s-%s" % (now.strftime("%Y%m%dT%H%M%SZ"), secrets.token_hex(3))


def utc_iso(now):
    return now.strftime("%Y-%m-%dT%H:%M:%SZ")


def _dumps(value):
    return json.dumps(to_json_safe(value), ensure_ascii=False, sort_keys=False)


def _fsync_dir(path):
    try:
        fd = os.open(path, os.O_RDONLY)
    except OSError:
        return
    try:
        os.fsync(fd)
    except OSError:
        pass
    finally:
        os.close(fd)


class Registry:
    def __init__(self, workdir):
        self.workdir = workdir
        self.dir = os.path.join(workdir, ".content-audit", "production")
        self.path = os.path.join(self.dir, "registry.jsonl")
        self.runs_dir = os.path.join(self.dir, "runs")
        self.lock_path = os.path.join(self.dir, "registry.lock")

    def run_path(self, run_id):
        return os.path.join(self.runs_dir, run_id + ".json")

    def relative(self, path):
        return os.path.relpath(path, self.workdir)

    def append(self, row):
        """Agrega un renglón y lo baja a disco. Lanza RegistryError si no puede."""
        line = _dumps(row)
        if "\n" in line:
            raise RegistryError("un renglón del registro no puede tener saltos de línea")
        try:
            os.makedirs(self.dir, exist_ok=True)
            existed = os.path.exists(self.path)
            fd = os.open(self.path, os.O_WRONLY | os.O_APPEND | os.O_CREAT, 0o644)
            try:
                prefix = ""
                if existed and os.path.getsize(self.path) > 0:
                    with open(self.path, "rb") as fh:
                        fh.seek(-1, os.SEEK_END)
                        if fh.read(1) != b"\n":
                            prefix = "\n"  # un renglón cortado queda solo en el suyo
                data = (prefix + line + "\n").encode("utf-8")
                written = os.write(fd, data)
                if written != len(data):
                    raise OSError("escritura incompleta")
                os.fsync(fd)
            finally:
                os.close(fd)
            if not existed:
                _fsync_dir(self.dir)
        except OSError as e:
            raise RegistryError("no se pudo escribir el registro %s (%s)" % (self.path, type(e).__name__))

    def rows(self):
        """Todos los renglones. Lanza RegistryError si alguno no se puede leer."""
        if not os.path.exists(self.path):
            return []
        try:
            with open(self.path, "rb") as fh:
                data = fh.read()
        except OSError as e:
            raise RegistryError("no se pudo leer el registro %s (%s)" % (self.path, type(e).__name__))
        rows = []
        for number, line in enumerate(data.split(b"\n"), start=1):
            if not line.strip():
                continue
            try:
                row = json.loads(line.decode("utf-8"))  # un renglón que no es UTF-8 tampoco se lee
            except ValueError:
                raise RegistryError("el renglón %d del registro %s no se puede leer; revisalo a mano "
                                    "antes de seguir" % (number, self.path))
            if not isinstance(row, dict):
                raise RegistryError("el renglón %d del registro %s no es un objeto" % (number, self.path))
            rows.append(row)
        return rows

    def write_run(self, run_id, detail):
        """Escribe el detalle de la corrida de forma atómica (temporal + fsync + rename)."""
        path = self.run_path(run_id)
        try:
            os.makedirs(self.runs_dir, exist_ok=True)
            fd, tmp = tempfile.mkstemp(prefix="run-", suffix=".tmp", dir=self.runs_dir)
            try:
                with os.fdopen(fd, "w", encoding="utf-8") as fh:
                    json.dump(to_json_safe(detail), fh, ensure_ascii=False, indent=2)
                    fh.write("\n")
                    fh.flush()
                    os.fsync(fh.fileno())
                os.replace(tmp, path)
            finally:
                if os.path.exists(tmp):
                    os.unlink(tmp)
            _fsync_dir(self.runs_dir)
        except OSError as e:
            raise RegistryError("no se pudo escribir el detalle %s (%s)" % (path, type(e).__name__))
        return path

    @contextlib.contextmanager
    def lock(self):
        """Una corrida por vez sobre este registro."""
        try:
            import fcntl
        except ImportError:  # sin flock (no es el caso en macOS ni Linux): no se bloquea
            yield
            return
        try:
            os.makedirs(self.dir, exist_ok=True)
            fh = open(self.lock_path, "a")
        except OSError as e:
            raise RegistryError("no se pudo abrir %s (%s)" % (self.lock_path, type(e).__name__))
        try:
            try:
                fcntl.flock(fh.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except OSError:
                raise RegistryBusy("hay otra corrida en curso sobre este registro (%s)" % self.lock_path)
            try:
                yield
            finally:
                fcntl.flock(fh.fileno(), fcntl.LOCK_UN)
        finally:
            fh.close()


def last_attempt(rows, tool, batch_id, digest, destination):
    """El último ensayo o aplicación de esa lista contra ese destino (las negadas no cuentan)."""
    last = None
    for row in rows:
        if (row.get("tool") == tool and row.get("batchId") == batch_id
                and row.get("manifestDigest") == digest and row.get("destination") == destination
                and row.get("mode") in (DRY_RUN, APPLY) and row.get("result") != REFUSED):
            last = row
    return last


def clean_dry_run_problem(rows, tool, batch_id, digest, destination, flags):
    """None si hay un ensayo limpio previo que habilita aplicar; si no, por qué no."""
    last = last_attempt(rows, tool, batch_id, digest, destination)
    if last is None:
        return ("Antes de aplicar hay que ensayar: no hay un ensayo de esta lista (huella %s) contra %s "
                "en el registro." % (digest[:12], destination_label(destination)))
    if last.get("mode") != DRY_RUN:
        return ("Después de la aplicación %s (%s) hay que volver a ensayar antes de aplicar otra vez."
                % (last.get("runId"), last.get("result")))
    if last.get("result") != DRY_RUN_CLEAN:
        return ("El último ensayo de esta lista contra %s (%s) no salió limpio (%s): resolvé lo que "
                "encontró y ensayá de nuevo." % (destination_label(destination), last.get("runId"),
                                                 last.get("result")))
    if last.get("flags") != flags:
        return ("El último ensayo limpio (%s) se hizo con otras opciones (%s); aplicá con las mismas o "
                "ensayá de nuevo." % (last.get("runId"), json.dumps(last.get("flags"), sort_keys=True)))
    return None


def record_legacy(workdir, row, out=None):
    """Para los scripts del camino viejo: agrega el renglón y avisa si no pudo.

    Devuelve True si quedó registrado.
    """
    try:
        Registry(workdir).append(row)
        return True
    except RegistryError as e:
        if out is not None:
            print("!! %s" % e, file=out)
        return False


def is_database_error(error):
    """Una excepción de pymongo o bson: se muestra solo por su clase."""
    return type(error).__module__.split(".")[0] in ("pymongo", "bson")


class LegacyRecorder:
    """Los renglones de los otros escritores de producción (F-PUBPROD-R008, inv. 4).

    `publish_revisions.py --plan`, `publish_knowledge_labels.py` y
    `publish_knowledge_instructions.py` dejan su renglón en
    <repo>/.content-audit/production/registry.jsonl, con el mismo destino
    {host, db} y los mismos resultados. El valor previo de lo que escriben va a
    runs/<runId>.json antes de la primera escritura.
    """

    def __init__(self, workdir, tool, uri, db, apply):
        self.clock = lambda: datetime.now(timezone.utc)
        self.registry = Registry(workdir)
        self.tool = tool
        self.mode = APPLY if apply else DRY_RUN
        self.run_id = new_run_id(self.clock())
        self.detail_path = None
        self.last_result = None
        self.row_lost = False  # no se pudo dejar el renglón final: la corrida no puede salir con 0
        try:
            self.destination = destination_of(uri, db)
            self.destination_error = None
        except ValueError as e:
            self.destination = None
            self.destination_error = str(e)

    def say(self, text):
        print(text)

    def record(self, result, **extra):
        self.last_result = result
        row = {"runId": self.run_id, "at": utc_iso(self.clock()), "tool": self.tool, "mode": self.mode,
               "result": result, "destination": self.destination}
        if self.detail_path is not None:
            row["detail"] = self.registry.relative(self.detail_path)
        row.update(extra)
        try:
            self.registry.append(row)
            return True
        except RegistryError as e:
            self.say("!! %s" % e)
            if result != STARTED:  # sin el de inicio no se escribe; sin el final, se avisa y sale mal
                self.row_lost = True
                self.say("!! La corrida no quedó registrada (%s): anotala a mano." % result)
            return False

    def write_detail(self, detail):
        """El respaldo previo a escribir. Devuelve False (y lo dice) si no pudo."""
        try:
            base = {"runId": self.run_id, "tool": self.tool, "mode": self.mode, "destination": self.destination,
                    "at": utc_iso(self.clock())}
            base.update(detail)
            self.detail_path = self.registry.write_run(self.run_id, base)
            return True
        except RegistryError as e:
            self.say("!! %s" % e)
            return False

    def refuse_bad_destination(self):
        """Si el URI no se puede leer: lo dice sin repetirlo, deja el renglón y devuelve 2."""
        self.say("NEGADO: no se puede leer el destino: %s. No se conectó a ninguna base." % self.destination_error)
        self.record(REFUSED, reason={"code": "BAD_URI", "message": self.destination_error})
        return 2

    def database_failed(self, error, **extra):
        """La base falló: se muestra solo la clase, se registra ABORTED y devuelve 1."""
        name = type(error).__name__
        self.say("ABORTADO: la base falló (%s)." % name)
        if self.unfinished():
            self.record(ABORTED, reason={"code": "STORE_ERROR", "message": "la base falló (%s)" % name},
                        errorClass=name, **extra)
        return 1

    def unfinished(self):
        """Todavía no hay renglón final: no se dejó ninguno o el último es STARTED."""
        return self.last_result in (None, STARTED)

    def run(self, body, **extra):
        """Corre el cuerpo del script; si termina mal sin renglón final, deja ABORTED.

        Una excepción de la base se muestra solo por su clase y devuelve 1; las
        demás se registran y siguen su curso (con su traza, que no trae el URI).
        """
        try:
            code = body()
        except SystemExit as e:
            if self.unfinished():
                self.record(ABORTED, reason={"code": "EXIT", "message": str(e.code)[:300]}, **extra)
            if self.row_lost and e.code in (0, None):
                raise SystemExit(1)
            raise
        except KeyboardInterrupt:
            if self.unfinished():
                self.record(ABORTED, reason={"code": "INTERRUPTED", "message": "corrida interrumpida a mano"},
                            **extra)
            raise
        except Exception as e:
            if is_database_error(e):
                return self.database_failed(e, **extra)
            if self.unfinished():
                self.record(ABORTED, errorClass=type(e).__name__,
                            reason={"code": "ERROR", "message": "falló el script (%s)" % type(e).__name__},
                            **extra)
            raise
        if self.row_lost and not code:
            return 1
        return code
