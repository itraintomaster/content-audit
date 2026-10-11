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
import secrets
import tempfile
from datetime import datetime, timezone

from .destination import destination_label, destination_of
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

# La salida de los escritores de producción.
EXIT_OK = 0         # ensayo limpio o aplicación verificada
EXIT_PROBLEMS = 1   # ensayo con problemas, aplicación abortada o verificación fallida
EXIT_REFUSED = 2    # negada antes de conectarse


class RegistryError(Exception):
    """No se pudo leer o escribir el registro."""


class RegistryBusy(RegistryError):
    """Otra corrida tiene tomado el registro."""


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


def is_database_error(error):
    """Una excepción de pymongo o bson: se muestra solo por su clase."""
    return type(error).__module__.split(".")[0] in ("pymongo", "bson")


class RunLog:
    """Una corrida de un escritor de producción: lo que imprime, sus renglones y su detalle.

    La usan el paso por tanda (cli.Run) y los escritores del camino viejo (LegacyRecorder).
    """

    def __init__(self, workdir, tool, mode, clock, out=None):
        self.clock = clock
        self.started = clock()
        self.run_id = new_run_id(self.started)
        self.registry = Registry(workdir)
        self.tool = tool
        self.mode = mode
        self.out = out
        self.destination = None
        self.detail_path = None
        self.last_result = None
        self.row_lost = False  # no se pudo dejar el renglón final: la corrida no puede salir con 0
        self.writes_began = False  # quedó el renglón de inicio: desde acá puede haber escrito en la base

    def say(self, text=""):
        print(text, file=self.out)

    def row(self, result, **extra):
        row = {"runId": self.run_id, "at": utc_iso(self.clock()), "tool": self.tool, "mode": self.mode,
               "result": result, "destination": self.destination}
        if self.detail_path is not None:
            row["detail"] = self.registry.relative(self.detail_path)
        row.update(extra)
        return row

    def record(self, result, **extra):
        """Agrega el renglón. Devuelve False (y lo dice) si no pudo.

        `last_result` cambia recién cuando el renglón quedó o cuando el registro dijo que no: si un
        corte llega a mitad de la escritura del renglón, la corrida sigue sin renglón de fin y el
        corte deja el suyo (mejor un renglón de más que uno de menos).
        """
        try:
            self.registry.append(self.row(result, **extra))
        except RegistryError as e:
            self.last_result = result
            self.say("!! %s" % e)
            if result != STARTED:  # sin el de inicio no se escribe; sin el final, se avisa y sale mal
                self.row_lost = True
                self.say("!! La corrida no quedó registrada (%s): anotala a mano." % result)
            return False
        self.last_result = result
        if result == STARTED:
            self.writes_began = True
        return True

    def cut_after_start(self):
        """Si la corrida dejó su renglón de inicio y todavía no el de fin (F-PUBPROD-R008, inv. 6)."""
        return self.writes_began and self.last_result == STARTED

    def say_cut(self, written, unknown, planned=None):
        """Lo que alcanzó a escribir una aplicación cortada, lo que no se sabe y dónde está el respaldo."""
        of = " de %d" % planned if planned is not None else ""
        self.say("Alcanzó a escribir %d documento(s)%s; no se sabe si quedaron %d." % (len(written), of, len(unknown)))
        for key in unknown:
            self.say("  NO SE SABE si quedó escrito %s: revisalo contra el respaldo (old) del detalle." % key)
        if self.detail_path is not None:
            self.say("El respaldo (old y new de cada campo a escribir) está en %s."
                     % self.registry.relative(self.detail_path))

    def write_detail(self, detail):
        """El detalle de la corrida (el respaldo, si va a escribir). Devuelve False (y lo dice) si no pudo."""
        base = {"runId": self.run_id, "tool": self.tool, "mode": self.mode, "destination": self.destination,
                "at": utc_iso(self.clock())}
        base.update(detail)
        try:
            self.detail_path = self.registry.write_run(self.run_id, base)
            return True
        except RegistryError as e:
            self.say("!! %s" % e)
            return False

    def begin_writing(self, backup, **row):
        """El respaldo y el renglón de inicio, antes de la primera escritura (F-PUBPROD-R008).

        Devuelve None si quedaron los dos. Si no, el motivo para mostrar: no se escribe nada
        (y si lo que faltó es el respaldo, queda un renglón ABORTED).
        """
        if not self.write_detail(backup):
            self.record(ABORTED, reason={"code": "DETAIL_NOT_WRITTEN",
                                         "message": "no se pudo escribir el respaldo; no se escribió nada"}, **row)
            return "no se pudo escribir el respaldo de la corrida; no se escribió nada"
        if not self.record(STARTED, **row):
            return "no se pudo dejar el renglón de inicio en el registro; no se escribió nada"
        return None


class LegacyRecorder(RunLog):
    """Los renglones de los otros escritores de producción (F-PUBPROD-R008, inv. 4).

    `publish_revisions.py --plan`, `publish_knowledge_labels.py` y
    `publish_knowledge_instructions.py` dejan su renglón en
    <repo>/.content-audit/production/registry.jsonl, con el mismo destino
    {host, db} y los mismos resultados. El valor previo de lo que escriben va a
    runs/<runId>.json antes de la primera escritura.
    """

    def __init__(self, workdir, tool, uri, db, apply):
        super().__init__(workdir, tool, APPLY if apply else DRY_RUN, lambda: datetime.now(timezone.utc))
        try:
            self.destination = destination_of(uri, db)
            self.destination_error = None
        except ValueError as e:
            self.destination_error = str(e)
        self.written = []    # «colección/id» de las escrituras que la base terminó
        self.in_flight = []  # los de la escritura en curso: si se corta, no se sabe si quedaron

    @contextlib.contextmanager
    def writing(self, ids):
        """Envuelve una escritura a la base (F-PUBPROD-R008, inv. 6).

        Si termina, sus «colección/id» quedan como escritos; si se corta (la base falla, un
        error, un Ctrl-C), quedan como «no se sabe»: una escritura en lote pudo haber llegado
        en parte. Un id puede quedar en las dos listas si una escritura anterior suya terminó.
        """
        self.in_flight = list(ids)
        yield
        for key in self.in_flight:
            if key not in self.written:
                self.written.append(key)
        self.in_flight = []

    def _cut(self):
        """Lo que suma el renglón de fin de una aplicación cortada después de su inicio; nada si no empezó."""
        if not self.cut_after_start():
            return {}
        self.say_cut(self.written, self.in_flight)
        return {"writtenIds": list(self.written), "indeterminate": list(self.in_flight), "verified": False}

    def refuse_bad_destination(self):
        """Si el URI no se puede leer: lo dice sin repetirlo y deja el renglón."""
        self.say("NEGADO: no se puede leer el destino: %s. No se conectó a ninguna base." % self.destination_error)
        self.record(REFUSED, reason={"code": "BAD_URI", "message": self.destination_error})
        return EXIT_REFUSED

    def database_failed(self, error, **extra):
        """La base falló: se muestra solo la clase y se registra ABORTED."""
        name = type(error).__name__
        self.say("ABORTADO: la base falló (%s)." % name)
        if self.unfinished():
            self.record(ABORTED, reason={"code": "STORE_ERROR", "message": "la base falló (%s)" % name},
                        errorClass=name, **self._cut(), **extra)
        return EXIT_PROBLEMS

    def unfinished(self):
        """Todavía no hay renglón final: no se dejó ninguno o el último es STARTED."""
        return self.last_result in (None, STARTED)

    def run(self, body, **extra):
        """Corre el cuerpo del script; si termina mal sin renglón final, deja ABORTED.

        Una excepción de la base se muestra solo por su clase y sale con EXIT_PROBLEMS; las
        demás se registran y siguen su curso (con su traza, que no trae el URI). Si ya había
        empezado a escribir, el ABORTED nombra lo escrito y lo que no se sabe (inv. 6).
        """
        try:
            code = body()
        except SystemExit as e:
            if self.unfinished():
                self.record(ABORTED, reason={"code": "EXIT", "message": str(e.code)[:300]}, **self._cut(), **extra)
            if self.row_lost and e.code in (0, None):
                raise SystemExit(EXIT_PROBLEMS)
            raise
        except KeyboardInterrupt:
            if self.unfinished():
                self.record(ABORTED, reason={"code": "INTERRUPTED", "message": "corrida interrumpida a mano"},
                            **self._cut(), **extra)
            raise
        except Exception as e:
            if is_database_error(e):
                return self.database_failed(e, **extra)
            if self.unfinished():
                self.record(ABORTED, errorClass=type(e).__name__,
                            reason={"code": "ERROR", "message": "falló el script (%s)" % type(e).__name__},
                            **self._cut(), **extra)
            raise
        if self.row_lost and not code:
            return EXIT_PROBLEMS
        return code
