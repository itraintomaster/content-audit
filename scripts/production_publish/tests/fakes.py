"""Colección falsa en memoria y armado de tandas en directorios temporales.

Nada de acá se conecta a ninguna base ni escribe fuera de `tempfile`.
"""

import copy
import hashlib
import json
import os
import shutil
import tempfile
from datetime import datetime, timedelta, timezone

from production_publish.store import Store, get_path, same_value, set_path

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")
EXAMPLE_MANIFEST = os.path.join(FIXTURES, "manifest-ejemplo.json")

BATCH_ID = "a1-v11-tanda-01"
PLAN_ID = "2026-10-05T10-00-00"

QUIZ_1 = "665f00000000000000000001"
QUIZ_2 = "665f00000000000000000002"
QUIZ_3 = "665f00000000000000000003"
KNOWLEDGE = "665f000000000000000000a1"
INSTANCE_IN_SYNC = "770000000000000000000001"
INSTANCE_STALE = "770000000000000000000002"
INSTANCE_OTHER = "770000000000000000000003"

# Credenciales inventadas: las pruebas buscan que nunca aparezcan en ningún lado.
FAKE_USER = "usuarioinventadozq"
FAKE_PASSWORD = "claveInventada-9x7Q"
URI = ("mongodb+srv://%s:%s@cluster-falso.abc12.mongodb.net/?retryWrites=true&authSource=admin"
       % (FAKE_USER, FAKE_PASSWORD))
HOST = "cluster-falso.abc12.mongodb.net"
DB = "learney"
DESTINATION = HOST + "/" + DB
# Otro servidor inventado, con la misma forma que el real: nombrarlo no tiene que alcanzar.
OTHER_DESTINATION = "otro-cluster-falso.xyz98.mongodb.net/" + DB


def example_manifest():
    with open(EXAMPLE_MANIFEST, encoding="utf-8") as fh:
        return json.load(fh)


def production_documents():
    """Lo que hay «en producción» antes de publicar: el antes de la lista, con otro orden
    de claves, más los campos que nunca se tocan."""
    return {
        "quizTemplates": {
            QUIZ_1: {
                "_id": QUIZ_1, "kind": "FILL_GAPS", "title": "To be", "translation": "Estoy feliz.",
                "instructions": "Completá.", "miniTheory": "", "audio": "s3://audio/1.mp3",
                "form": {"name": "fill", "kind": "FILL_GAPS", "incidence": 1, "label": "Completar",
                         "sentences": ["I am happy."],
                         "sentenceParts": [{"text": "I ", "kind": "TEXT"},
                                           {"answers": ["am"], "text": "am", "kind": "GAP"},
                                           {"text": " happy.", "kind": "TEXT"}]},
                "tags": ["a1", "to-be"],
            },
            QUIZ_2: {
                "_id": QUIZ_2, "kind": "REWRITE", "title": "To be", "translation": "Ella es alta.",
                "instructions": "Reescribí.",
                "form": {"kind": "REWRITE", "incidence": 1, "label": "Reescribir", "name": "rewrite",
                         "sentences": None,
                         "sentenceParts": [{"kind": "TEXT", "text": "She is tall."}]},
            },
            QUIZ_3: {
                "_id": QUIZ_3, "kind": "MULTIPLE_CHOICE", "title": "To be", "instructions": "Elegí.",
                "form": {"kind": "MULTIPLE_CHOICE", "incidence": 1, "label": "Elegir", "name": "mc",
                         "selection": "SINGLE",
                         "items": [{"label": "am", "id": "am", "incidence": 1.0},
                                   {"incidence": 0.0, "id": "is", "label": "Is"}]},
            },
            "665f00000000000000000099": {"_id": "665f00000000000000000099", "title": "Otro", "instructions": "x"},
        },
        "knowledges": {
            KNOWLEDGE: {"_id": KNOWLEDGE, "label": "El verbo $to be$", "instructions": "El verbo to be.",
                        "order": 3},
        },
        "quizzes": {
            INSTANCE_IN_SYNC: {"_id": INSTANCE_IN_SYNC, "user": "u1", "score": 0.5,
                               "template": {"_id": QUIZ_1, "title": "To be", "instructions": "Completá."}},
            INSTANCE_STALE: {"_id": INSTANCE_STALE, "user": "u2",
                             "template": {"_id": QUIZ_1, "title": "To be", "instructions": "Completa la oración."}},
            INSTANCE_OTHER: {"_id": INSTANCE_OTHER, "user": "u3",
                             "template": {"_id": "665f00000000000000000099", "instructions": "x"}},
        },
    }


class FakeStore(Store):
    """Colección en memoria con la semántica que usa el paso a producción.

    Fallas que se pueden inyectar:
      fail_open        excepción al abrir (la lanza FakeStoreFactory)
      fail_find        excepción al leer
      fail_write_on    {"colección/id": excepción} al escribir ese documento
      lose_race_on     {"colección/id"}: alguien lo cambió justo antes (no coincide el filtro)
      after_write      {"colección/id": función(doc)}: corrompe el documento después de escribir
      on_write         función(store, colección, id) llamada antes de cada escritura
    """

    def __init__(self, documents=None):
        self.data = copy.deepcopy(documents if documents is not None else production_documents())
        self.writes = []
        self.attempts = []
        self.fail_find = None
        self.fail_write_on = {}
        self.lose_race_on = set()
        self.after_write = {}
        self.on_write = None
        self.closed = False

    def collections(self):
        return list(self.data)

    def find_document(self, collection, doc_id):
        if self.fail_find is not None:
            raise self.fail_find
        doc = self.data.get(collection, {}).get(doc_id)
        return copy.deepcopy(doc) if doc is not None else None

    def find_instances(self, template_ids):
        if self.fail_find is not None:
            raise self.fail_find
        ids = set(template_ids)
        found = []
        for instance_id, doc in sorted(self.data.get("quizzes", {}).items()):
            template = doc.get("template")
            if isinstance(template, dict) and str(template.get("_id")) in ids:
                found.append((instance_id, copy.deepcopy(doc)))
        return found

    def compare_and_set(self, collection, doc_id, expected, sets):
        key = "%s/%s" % (collection, doc_id)
        self.attempts.append(key)
        if self.on_write is not None:
            self.on_write(self, collection, doc_id)
        if key in self.fail_write_on:
            raise self.fail_write_on[key]
        if key in self.lose_race_on:
            return False
        doc = self.data.get(collection, {}).get(doc_id)
        if doc is None:
            return False
        for path, value in expected.items():
            if not same_value(get_path(doc, path), value):
                return False
        for path, value in sets.items():
            set_path(doc, path, copy.deepcopy(value))
        self.writes.append((collection, doc_id, copy.deepcopy(sets)))
        if key in self.after_write:
            self.after_write[key](doc)
        return True

    def close(self):
        self.closed = True


class FakeStoreFactory:
    """Hace de `store_factory`: cuenta las conexiones y guarda con qué se llamó."""

    def __init__(self, store=None, fail_open=None):
        self.store = store if store is not None else FakeStore()
        self.fail_open = fail_open
        self.calls = []

    def __call__(self, uri, db):
        self.calls.append((uri, db))
        if self.fail_open is not None:
            raise self.fail_open
        return self.store


class Clock:
    """Reloj que avanza un segundo por lectura (los renglones quedan ordenados)."""

    def __init__(self):
        self.t = datetime(2026, 10, 12, 4, 0, 0, tzinfo=timezone.utc)

    def __call__(self):
        self.t += timedelta(seconds=1)
        return self.t


def temp_workdir(test):
    path = tempfile.mkdtemp(prefix="pubprod-")
    test.addCleanup(shutil.rmtree, path, True)
    return path


def manifest_bytes(manifest):
    return (json.dumps(manifest, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def write_batch(workdir, manifest=None, *, batch_id=BATCH_ID, plan_id=PLAN_ID, status="PUBLISHED",
                digest=None, publication=None, write_manifest=True, raw_manifest=None):
    """Escribe la tanda y su lista como las deja el lado Java. Devuelve la huella."""
    manifest = example_manifest() if manifest is None else manifest
    data = raw_manifest if raw_manifest is not None else manifest_bytes(manifest)
    real_digest = hashlib.sha256(data).hexdigest()
    if write_manifest:
        mdir = os.path.join(workdir, ".content-audit", "batch-publications", batch_id)
        os.makedirs(mdir, exist_ok=True)
        with open(os.path.join(mdir, "manifest.json"), "wb") as fh:
            fh.write(data)
    pub = {
        "publishedAt": "2026-10-12T03:10:44Z",
        "approvedBy": "José",
        "auditId": "audit-publicado",
        "manifestDigest": real_digest if digest is None else digest,
        "verdictKind": "PASSES",
        "gatePassed": True,
        "undecidableFloors": None,
        "stepsApproved": 2,
        "stepsUnreviewed": 3,
        "stepsRejected": 1,
    }
    if publication:
        pub.update(publication)
    record = {"id": batch_id, "planId": plan_id, "level": "A1", "status": status, "publication": pub}
    bdir = os.path.join(workdir, ".content-audit", "batches")
    os.makedirs(bdir, exist_ok=True)
    with open(os.path.join(bdir, batch_id + ".json"), "w", encoding="utf-8") as fh:
        json.dump(record, fh, ensure_ascii=False, indent=2)
    return real_digest


def registry_rows(workdir):
    path = os.path.join(workdir, ".content-audit", "production", "registry.jsonl")
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def run_detail(workdir, row):
    with open(os.path.join(workdir, row["detail"]), encoding="utf-8") as fh:
        return json.load(fh)


def all_text_under(path):
    """Todo el texto de los archivos bajo `path` (para buscar credenciales)."""
    chunks = []
    for root, _dirs, files in os.walk(path):
        for name in files:
            with open(os.path.join(root, name), "rb") as fh:
                chunks.append(fh.read().decode("utf-8", errors="replace"))
    return "\n".join(chunks)


APPLY_ARGS = ("--apply", "--expect-destination", DESTINATION)


def run_cli(workdir, *extra, factory=None, clock=None, uri=URI, batch_id=BATCH_ID):
    """Corre el paso a producción con la colección falsa. Devuelve (salida, texto, fábrica)."""
    import io

    from production_publish import cli

    out = io.StringIO()
    factory = factory if factory is not None else FakeStoreFactory()
    code = cli.main(["--batch", batch_id, "--workdir", workdir, "--uri", uri, "--db", DB] + list(extra),
                    store_factory=factory, out=out, now=clock if clock is not None else Clock())
    return code, out.getvalue(), factory
