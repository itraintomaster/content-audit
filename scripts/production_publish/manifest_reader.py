"""Lee una tanda publicada y su lista de publicación, y niega todo lo dudoso.

Todo lo de este módulo pasa ANTES de conectarse a la base (F-PUBPROD-R006).
Lee solamente:

  <workdir>/.content-audit/batches/<batchId>.json
      status, planId, publication.manifestDigest, publication.verdictKind,
      publication.gatePassed, publication.undecidableFloors
  <workdir>/.content-audit/batch-publications/<batchId>/manifest.json
      la lista (esquema 1), cuya huella sha256 tiene que ser la registrada.

Nunca lee el árbol `db/`: lo que se escribe sale de la lista.
"""

import glob
import hashlib
import json
import os
import re

from .store import same_value

SCHEMA = 1

QUIZ_TEMPLATES = "quizTemplates"
KNOWLEDGES = "knowledges"
ALLOWED_COLLECTIONS = (KNOWLEDGES, QUIZ_TEMPLATES)

# Las rutas que la tabla de la corrección (F-TAREA-R006 / F-PUBPROD-R002) puede escribir.
# Una ruta que no esté acá se niega: la tabla es un contrato con el lado Java.
WRITABLE_PATHS = {
    QUIZ_TEMPLATES: ("form.sentences", "translation", "instructions", "miniTheory",
                     "form.sentenceParts", "form.items"),
    KNOWLEDGES: ("instructions",),
}

# Nunca, por ningún camino: el identificador y lo que el backend usa para el
# polimorfismo del formulario.
FORBIDDEN_PATHS = ("_id", "kind", "form.kind", "form.incidence", "form.label", "form.name")

REVIEW_STATUSES = ("APPROVED", "PENDING")
EXCLUDED_REASONS = ("REVIEW_REJECTED", "COLLATERAL", "RULE_EXCLUDED", "DROPPED_BY_JOSE")

BATCH_ID = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*\Z")
HEX_ID = re.compile(r"[0-9a-f]{24}\Z")
SHA256_HEX = re.compile(r"[0-9a-f]{64}\Z")

MAX_LISTED_PROBLEMS = 50


class Refusal(Exception):
    """La corrida se niega. `problems` es una lista de (código, mensaje)."""

    def __init__(self, code, message, problems=None):
        super().__init__(message)
        self.code = code
        self.message = message
        self.problems = list(problems or [(code, message)])

    def as_reason(self):
        return {
            "code": self.code,
            "message": self.message,
            "problems": [{"code": c, "message": m} for c, m in self.problems[:MAX_LISTED_PROBLEMS]],
            "problemCount": len(self.problems),
        }


class LoadedBatch:
    def __init__(self, batch_id, plan_id, batch_path, manifest_path, digest, manifest, publication):
        self.batch_id = batch_id
        self.plan_id = plan_id
        self.batch_path = batch_path
        self.manifest_path = manifest_path
        self.digest = digest
        self.manifest = manifest
        self.publication = publication


def batch_record_path(workdir, batch_id):
    return os.path.join(workdir, ".content-audit", "batches", batch_id + ".json")


def manifest_file_path(workdir, batch_id):
    return os.path.join(workdir, ".content-audit", "batch-publications", batch_id, "manifest.json")


def _strict_json(data, what):
    """JSON sin claves repetidas ni NaN/Infinity: una lista ambigua no se publica."""

    def pairs(items):
        out = {}
        for key, value in items:
            if key in out:
                raise ValueError("clave repetida «%s»" % key)
            out[key] = value
        return out

    def bad_constant(name):
        raise ValueError("valor no permitido %s" % name)

    try:
        return json.loads(data, object_pairs_hook=pairs, parse_constant=bad_constant)
    except ValueError as e:
        raise Refusal("UNREADABLE", "No se pudo leer %s: %s." % (what, e))


def load(workdir, batch_id):
    """Lee y valida la tanda y su lista. Lanza Refusal ante cualquier duda."""
    if not isinstance(batch_id, str) or not BATCH_ID.match(batch_id):
        raise Refusal("BAD_BATCH_ID", "Identidad de tanda inválida.")
    bpath = batch_record_path(workdir, batch_id)
    if not os.path.isfile(bpath):
        raise Refusal("BATCH_NOT_FOUND",
                      "No existe la tanda %s en %s (¿pasaste el --workdir de la campaña?)."
                      % (batch_id, os.path.dirname(bpath)))
    with open(bpath, "rb") as fh:
        try:
            record = _strict_json(fh.read().decode("utf-8"), "la tanda %s" % batch_id)
        except UnicodeDecodeError:
            raise Refusal("UNREADABLE", "La tanda %s no es UTF-8." % batch_id)
    if not isinstance(record, dict):
        raise Refusal("UNREADABLE", "La tanda %s no es un objeto JSON." % batch_id)
    if record.get("id") != batch_id:
        raise Refusal("BATCH_ID_MISMATCH", "El archivo de la tanda %s dice ser de otra tanda." % batch_id)
    if record.get("status") != "PUBLISHED":
        raise Refusal("NOT_PUBLISHED",
                      "La tanda %s no está publicada (estado %s): solo una tanda publicada va a producción."
                      % (batch_id, record.get("status")))
    publication = record.get("publication")
    if not isinstance(publication, dict):
        raise Refusal("NOT_PUBLISHED", "La tanda %s no registra su publicación." % batch_id)
    digest = publication.get("manifestDigest")
    if not digest:
        # Decisión 14: las tandas publicadas antes de la lista no tienen qué llevar.
        raise Refusal("NO_MANIFEST",
                      "La tanda %s se publicó sin lista de publicación (antes de este paso). "
                      "El paso a producción solo toma lo que dice la lista, así que esta tanda "
                      "no se puede llevar por acá." % batch_id)
    if not isinstance(digest, str) or not SHA256_HEX.match(digest.lower()):
        raise Refusal("BAD_DIGEST", "La huella registrada de la lista de %s no es un sha256." % batch_id)
    mpath = manifest_file_path(workdir, batch_id)
    if not os.path.isfile(mpath):
        raise Refusal("MANIFEST_MISSING", "Falta la lista de publicación de %s en %s." % (batch_id, mpath))
    with open(mpath, "rb") as fh:
        raw = fh.read()
    actual = hashlib.sha256(raw).hexdigest()
    if actual != digest.lower():
        raise Refusal("DIGEST_MISMATCH",
                      "La lista de %s no es la que se publicó: su huella es %s y la tanda registró %s. "
                      "No se publica una lista cambiada." % (batch_id, actual, digest.lower()))

    problems = []
    if publication.get("verdictKind") != "PASSES":
        problems.append(("VERDICT_NOT_PASSES",
                         "La publicación registra el veredicto «%s», no «PASSES»."
                         % publication.get("verdictKind")))
    if publication.get("gatePassed") is not True:
        problems.append(("GATE_NOT_PASSED",
                         "La publicación no registra que la compuerta del último cierre pasó "
                         "(gatePassed=%s)." % json.dumps(publication.get("gatePassed"))))
    if "undecidableFloors" not in publication:
        problems.append(("UNDECIDABLE_FLOORS", "La publicación no registra los pisos sin decidir."))
    elif not _no_floors(publication.get("undecidableFloors"), allow_null=True):
        problems.append(("UNDECIDABLE_FLOORS",
                         "La publicación registra pisos que no se pueden decidir: con «no se puede "
                         "decidir» no se publica."))
    if problems:
        raise Refusal(problems[0][0], problems[0][1], problems)

    try:
        manifest = _strict_json(raw.decode("utf-8"), "la lista de %s" % batch_id)
    except UnicodeDecodeError:
        raise Refusal("UNREADABLE", "La lista de %s no es UTF-8." % batch_id)
    plan_id = record.get("planId")
    validate_manifest(manifest, expected_batch_id=batch_id, expected_plan_id=plan_id)
    return LoadedBatch(batch_id, plan_id, bpath, mpath, actual, manifest, publication)


def _no_floors(value, allow_null):
    """Si `value` dice que no hay pisos sin decidir (lista vacía, cantidad 0, objeto con count 0)."""
    if value is None:
        # En el registro de la tanda, Java usa null para «todos los pisos decididos»
        # (DefaultBatchComparator.undecidableFloors). En la lista, el esquema pide [].
        return allow_null
    if isinstance(value, bool):
        return False
    if isinstance(value, list):
        return len(value) == 0
    if isinstance(value, int):
        return value == 0
    if isinstance(value, dict):
        count = value.get("count")
        analyzers = value.get("analyzers")
        return (isinstance(count, int) and not isinstance(count, bool) and count == 0
                and not analyzers)
    return False


def _walk_keys(value, path, problems, where):
    """Claves con `$` o `.` (JSON extendido sin convertir) y `text` nulo, en cualquier profundidad."""
    if isinstance(value, dict):
        for key, inner in value.items():
            if key.startswith("$") or "." in key:
                problems.append(("EXTENDED_JSON",
                                 "%s: «%s» trae la clave «%s» (JSON extendido sin convertir o una "
                                 "ruta dentro de un valor)." % (where, path, key)))
            if key == "text" and inner is None:
                problems.append(("NULL_TEXT",
                                 "%s: «%s» trae un text nulo; el texto vacío va como \"\"." % (where, path)))
            _walk_keys(inner, path, problems, where)
    elif isinstance(value, list):
        for inner in value:
            _walk_keys(inner, path, problems, where)


def _is_str_list(value):
    return isinstance(value, list) and all(isinstance(x, str) for x in value)


def _check_after_type(path, after, where, problems):
    if path in ("translation", "instructions", "miniTheory"):
        ok = isinstance(after, str)
        expected = "un texto"
    elif path == "form.sentences":
        ok = _is_str_list(after)
        expected = "una lista de textos"
    elif path == "form.sentenceParts":
        ok = isinstance(after, list) and all(
            isinstance(p, dict) and isinstance(p.get("text"), str) for p in after)
        expected = "una lista de partes, cada una con su text"
    elif path == "form.items":
        ok = isinstance(after, list) and all(isinstance(i, dict) for i in after)
        expected = "una lista de opciones"
    else:
        ok, expected = False, "una ruta conocida"
    if not ok:
        problems.append(("BAD_VALUE", "%s: el después de «%s» tiene que ser %s." % (where, path, expected)))


def only_labels_change(before, after):
    """Si dos listas de opciones difieren solo en la etiqueta de cada opción (TRASPASO §6).

    El id es la etiqueta en minúscula del original y la incidencia marca la correcta:
    cambiarlos, o cambiar la cantidad de opciones, no viaja por script.
    """
    if not isinstance(before, list) or not isinstance(after, list) or len(before) != len(after):
        return False
    for old, new in zip(before, after):
        if not isinstance(old, dict) or not isinstance(new, dict):
            return False
        if not same_value({k: v for k, v in old.items() if k != "label"},
                          {k: v for k, v in new.items() if k != "label"}):
            return False
    return True


def _check_multiple_choice(path, before, after, where, problems):
    """De las opciones solo se publica la etiqueta (la selección ni siquiera está en la tabla)."""
    if path == "form.items" and not only_labels_change(before, after):
        problems.append(("MULTIPLE_CHOICE_BEYOND_LABEL",
                         "%s: las opciones cambian algo más que su etiqueta (cantidad, id o incidencia); "
                         "en opción múltiple solo se publica la etiqueta de cada opción." % where))


def validate_manifest(manifest, expected_batch_id=None, expected_plan_id=None):
    """Valida la estructura y lo que se puede escribir. Lanza Refusal con todos los problemas."""
    problems = []
    if not isinstance(manifest, dict):
        raise Refusal("BAD_MANIFEST", "La lista no es un objeto JSON.")
    schema = manifest.get("schema")
    if isinstance(schema, bool) or schema != SCHEMA:
        raise Refusal("UNKNOWN_SCHEMA", "La lista es del esquema %s; este paso solo conoce el %d."
                      % (json.dumps(schema), SCHEMA))
    if expected_batch_id is not None and manifest.get("batchId") != expected_batch_id:
        problems.append(("BATCH_ID_MISMATCH", "La lista dice ser de la tanda %s, no de %s."
                         % (manifest.get("batchId"), expected_batch_id)))
    if expected_plan_id is not None and manifest.get("planId") != expected_plan_id:
        problems.append(("PLAN_ID_MISMATCH", "La lista dice ser del plan %s y la tanda es del plan %s."
                         % (manifest.get("planId"), expected_plan_id)))

    verdict = manifest.get("verdict")
    if not isinstance(verdict, dict):
        problems.append(("VERDICT_NOT_PASSES", "La lista no trae el veredicto."))
    else:
        if verdict.get("kind") != "PASSES":
            problems.append(("VERDICT_NOT_PASSES", "La lista trae el veredicto «%s», no «PASSES»."
                             % verdict.get("kind")))
        if verdict.get("gatePassed") is not True:
            problems.append(("GATE_NOT_PASSED", "La lista no dice que la compuerta pasó."))
        if "undecidableFloors" not in verdict or not _no_floors(verdict.get("undecidableFloors"),
                                                                allow_null=False):
            problems.append(("UNDECIDABLE_FLOORS",
                             "La lista trae pisos sin decidir (o no dice cuáles): con «no se puede "
                             "decidir» no se publica."))
        broken = verdict.get("brokenFloorGoalIds")
        if broken not in (None, []):
            problems.append(("BROKEN_FLOORS", "La lista trae pisos rotos: %s." % json.dumps(broken)))

    reviews = manifest.get("reviews")
    left_out = set()
    if not isinstance(reviews, dict):
        problems.append(("BAD_REVIEWS", "La lista no trae la cuenta de aprobados, sin revisar y rechazados."))
    else:
        for key in ("approved", "pending", "rejected"):
            value = reviews.get(key)
            if isinstance(value, bool) or not isinstance(value, int) or value < 0:
                problems.append(("BAD_REVIEWS", "La lista trae reviews.%s inválido." % key))
        for key in ("droppedForReviewChangeIds", "collateralDroppedChangeIds"):
            value = reviews.get(key, [])
            if not _is_str_list(value):
                problems.append(("BAD_REVIEWS", "La lista trae reviews.%s inválido." % key))
            else:
                left_out.update(value)

    excluded = manifest.get("excluded")
    if not isinstance(excluded, list):
        problems.append(("BAD_EXCLUDED", "La lista no trae los cambios que quedaron afuera."))
    else:
        for item in excluded:
            if not isinstance(item, dict) or not isinstance(item.get("changeId"), str):
                problems.append(("BAD_EXCLUDED", "Un cambio que quedó afuera no tiene changeId."))
                continue
            if item.get("reason") not in EXCLUDED_REASONS:
                problems.append(("BAD_EXCLUDED", "El cambio %s quedó afuera por un motivo desconocido «%s»."
                                 % (item["changeId"], item.get("reason"))))
            left_out.add(item["changeId"])

    unsupported = manifest.get("unsupported")
    if not isinstance(unsupported, list):
        problems.append(("BAD_UNSUPPORTED", "La lista no trae lo que no tiene lugar en producción."))
    else:
        for item in unsupported:
            if not isinstance(item, dict) or not isinstance(item.get("changeId"), str):
                problems.append(("BAD_UNSUPPORTED", "Un cambio sin lugar en producción no tiene changeId."))

    documents = manifest.get("documents")
    total_fields = 0
    by_path = {}
    if not isinstance(documents, list):
        problems.append(("BAD_DOCUMENTS", "La lista no trae los documentos."))
        documents = []
    seen_docs = set()
    for index, doc in enumerate(documents):
        if not isinstance(doc, dict):
            problems.append(("BAD_DOCUMENTS", "El documento %d de la lista no es un objeto." % index))
            continue
        collection = doc.get("collection")
        doc_id = doc.get("id")
        where = "%s %s (%s)" % (collection, doc_id, doc.get("nodeId"))
        if collection not in ALLOWED_COLLECTIONS:
            problems.append(("COLLECTION_NOT_ALLOWED",
                             "%s: la colección «%s» no se escribe; solo %s."
                             % (where, collection, " y ".join(ALLOWED_COLLECTIONS))))
            continue
        if not isinstance(doc_id, str) or not HEX_ID.match(doc_id):
            problems.append(("BAD_ID", "%s: el id tiene que ser un ObjectId en hexadecimal." % where))
            continue
        if (collection, doc_id) in seen_docs:
            problems.append(("DUPLICATE_DOCUMENT", "%s aparece dos veces en la lista." % where))
            continue
        seen_docs.add((collection, doc_id))
        node_id = doc.get("nodeId")
        if node_id is not None and not isinstance(node_id, str):
            problems.append(("BAD_DOCUMENTS", "%s: nodeId inválido." % where))
        change_ids = doc.get("changeIds")
        if not _is_str_list(change_ids) or not change_ids:
            problems.append(("BAD_DOCUMENTS", "%s: no dice qué cambios lo originaron." % where))
            change_ids = []
        if not _is_str_list(doc.get("proposalIds")):
            problems.append(("BAD_DOCUMENTS", "%s: proposalIds inválido." % where))
        travelling_out = sorted(set(change_ids) & left_out)
        if travelling_out:
            problems.append(("EXCLUDED_CHANGE_TRAVELS",
                             "%s: lo originan cambios que quedaron afuera (%s); lo rechazado no viaja."
                             % (where, ", ".join(travelling_out))))
        status = doc.get("reviewStatus")
        if status == "REJECTED":
            problems.append(("REJECTED_DOCUMENT",
                             "%s: está rechazado en la revisión y lo rechazado nunca se publica." % where))
        elif status not in REVIEW_STATUSES:
            problems.append(("BAD_REVIEW_STATUS", "%s: estado de revisión desconocido «%s»." % (where, status)))
        fields = doc.get("fields")
        if not isinstance(fields, list) or not fields:
            problems.append(("BAD_DOCUMENTS", "%s: no trae campos." % where))
            continue
        seen_paths = set()
        for field in fields:
            if not isinstance(field, dict) or not isinstance(field.get("path"), str):
                problems.append(("BAD_FIELD", "%s: un campo no tiene ruta." % where))
                continue
            path = field["path"]
            if path in FORBIDDEN_PATHS or any(path.startswith(p + ".") for p in FORBIDDEN_PATHS):
                problems.append(("FORBIDDEN_PATH", "%s: la ruta «%s» está prohibida; nunca se escribe."
                                 % (where, path)))
                continue
            if path not in WRITABLE_PATHS[collection]:
                problems.append(("PATH_NOT_ALLOWED",
                                 "%s: la ruta «%s» no está en la tabla de lo que se escribe en %s."
                                 % (where, path, collection)))
                continue
            if path in seen_paths:
                problems.append(("DUPLICATE_FIELD", "%s: la ruta «%s» aparece dos veces." % (where, path)))
                continue
            seen_paths.add(path)
            if "before" not in field or "after" not in field:
                problems.append(("BAD_FIELD", "%s: «%s» no trae antes y después." % (where, path)))
                continue
            before, after = field["before"], field["after"]
            if after is None:
                problems.append(("NULL_AFTER", "%s: el después de «%s» es nulo; nunca se escribe un nulo."
                                 % (where, path)))
                continue
            _walk_keys(before, path, problems, where)
            _walk_keys(after, path, problems, where)
            _check_after_type(path, after, where, problems)
            _check_multiple_choice(path, before, after, where, problems)
            if same_value(before, after):
                problems.append(("UNCHANGED_FIELD", "%s: «%s» trae el mismo antes y después." % (where, path)))
            total_fields += 1
            by_path[path] = by_path.get(path, 0) + 1

    counts = manifest.get("counts")
    if not isinstance(counts, dict):
        problems.append(("BAD_COUNTS", "La lista no trae sus cuentas."))
    else:
        if counts.get("documents") != len(documents):
            problems.append(("BAD_COUNTS", "La lista cuenta %s documentos y trae %d."
                             % (json.dumps(counts.get("documents")), len(documents))))
        if counts.get("fields") != total_fields:
            problems.append(("BAD_COUNTS", "La lista cuenta %s campos y trae %d."
                             % (json.dumps(counts.get("fields")), total_fields)))
        if "byPath" in counts and counts.get("byPath") != by_path:
            problems.append(("BAD_COUNTS", "Las cuentas por ruta de la lista no coinciden con sus campos."))

    if problems:
        raise Refusal(problems[0][0], "La lista de publicación no se puede llevar a producción: %s"
                      % problems[0][1], problems)
    return manifest


def find_batches_for_plan(roots, plan_id):
    """Tandas cuyo planId es `plan_id`, buscando en cada raíz y en sus campañas.

    Devuelve (coincidencias, ilegibles): listas de (ruta, id de tanda) y de rutas.
    """
    matches, unreadable = [], []
    seen = set()
    for root in roots:
        patterns = (os.path.join(root, ".content-audit", "batches", "*.json"),
                    os.path.join(root, ".content-audit", "campaigns", "*", ".content-audit", "batches", "*.json"))
        for pattern in patterns:
            for path in sorted(glob.glob(pattern)):
                real = os.path.realpath(path)
                if real in seen:
                    continue
                seen.add(real)
                try:
                    with open(path, encoding="utf-8") as fh:
                        record = json.load(fh)
                except (OSError, ValueError):
                    unreadable.append(path)
                    continue
                if isinstance(record, dict) and record.get("planId") == plan_id:
                    matches.append((path, record.get("id")))
    return matches, unreadable
