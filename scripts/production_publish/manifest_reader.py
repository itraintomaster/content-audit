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

import hashlib
import json
import os
import re

from .store import same_value
from . import conversion

SCHEMA = 1

QUIZ_TEMPLATES = "quizTemplates"
KNOWLEDGES = "knowledges"
# Las colecciones que la lista puede tocar, en orden de escritura (las copias embebidas
# en las instancias van después de todas).
COLLECTIONS = (KNOWLEDGES, QUIZ_TEMPLATES)

# Las rutas que la tabla de la corrección (F-TAREA-R006 / F-PUBPROD-R002) puede escribir.
# Una ruta que no esté acá se niega: la tabla es un contrato con el lado Java.
WRITABLE_PATHS = {
    QUIZ_TEMPLATES: ("form.sentences", "translation", "instructions", "miniTheory",
                     "form.sentenceParts", "form.items", "kind", "form", "clozeOriginal"),
    KNOWLEDGES: ("instructions",),
}

# Nunca, por ningún camino: el identificador y lo que el backend usa para el
# polimorfismo del formulario.
FORBIDDEN_PATHS = ("_id", "kind", "form.kind", "form.incidence", "form.label", "form.name")

REVIEW_STATUSES = ("APPROVED", "PENDING")
EXCLUDED_REASONS = ("REVIEW_REJECTED", "COLLATERAL", "RULE_EXCLUDED", "DROPPED_BY_JOSE")

# Las claves exactas del esquema 1, con los nombres del lado Java (PublicationManifest y sus
# records). Una clave de más o de menos no se adivina: se niega.
MANIFEST_KEYS = ("schema", "batchId", "planId", "level", "publishedAt", "approvedBy", "source", "verdict",
                 "reviews", "documents", "unsupported", "excluded", "counts")
SOURCE_KEYS = ("baselineAuditId", "startAuditId", "publishedAuditId")
VERDICT_KEYS = ("kind", "gatePassed", "undecidableFloors", "brokenFloorGoalIds", "detail")
REVIEWS_KEYS = ("approved", "pending", "rejected", "droppedForReviewChangeIds", "collateralDroppedChangeIds")
DOCUMENT_KEYS = ("collection", "id", "nodeId", "changeIds", "proposalIds", "reviewStatus", "fields")
FIELD_KEYS = ("path", "before", "after")
UNSUPPORTED_KEYS = ("changeId", "nodeId", "part", "reason")
EXCLUDED_KEYS = ("changeId", "reason")
COUNTS_KEYS = ("documents", "fields", "byPath")

# Como lo escribe Instant.toString() del lado Java: siempre en UTC, con segundos.
PUBLISHED_AT = re.compile(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,9})?Z\Z")

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
    def __init__(self, batch_id, plan_id, manifest_path, digest, manifest):
        self.batch_id = batch_id
        self.plan_id = plan_id
        self.manifest_path = manifest_path
        self.digest = digest
        self.manifest = manifest


def batch_record_path(workdir, batch_id):
    return os.path.join(workdir, ".content-audit", "batches", batch_id + ".json")


def manifest_file_path(workdir, batch_id):
    return os.path.join(workdir, ".content-audit", "batch-publications", batch_id, "manifest.json")


def _strict_json(data, what):
    """Los bytes de un JSON en UTF-8, sin claves repetidas ni NaN/Infinity: una lista ambigua no se publica."""

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
        text = data.decode("utf-8")
    except UnicodeDecodeError:
        raise Refusal("UNREADABLE", "No se pudo leer %s: no es UTF-8." % what)
    try:
        return json.loads(text, object_pairs_hook=pairs, parse_constant=bad_constant)
    except ValueError as e:
        raise Refusal("UNREADABLE", "No se pudo leer %s: %s." % (what, e))


def load(workdir, batch_id):
    """Lee y valida la tanda y su lista. Lanza Refusal ante cualquier duda."""
    if not isinstance(batch_id, str) or not BATCH_ID.match(batch_id):
        raise Refusal("BAD_BATCH_ID", "Identidad de tanda inválida.")
    record = _published_record(workdir, batch_id)
    publication = record["publication"]
    digest = _registered_digest(publication, batch_id)
    manifest_path, raw = _published_manifest(workdir, batch_id, digest)
    problems = []
    _verdict_problems("La publicación de la tanda", publication.get("verdictKind"), publication.get("gatePassed"),
                      "undecidableFloors" in publication and _no_floors(publication["undecidableFloors"]),
                      problems)
    if problems:
        raise Refusal(problems[0][0], problems[0][1], problems)
    manifest = _strict_json(raw, "la lista de %s" % batch_id)
    plan_id = record.get("planId")
    validate_manifest(manifest, expected_batch_id=batch_id, expected_plan_id=plan_id)
    return LoadedBatch(batch_id, plan_id, manifest_path, digest, manifest)


def _published_record(workdir, batch_id):
    """El registro de la tanda, si es de esta tanda, está publicada y registra su publicación."""
    path = batch_record_path(workdir, batch_id)
    if not os.path.isfile(path):
        raise Refusal("BATCH_NOT_FOUND",
                      "No existe la tanda %s en %s (¿pasaste el --workdir de la campaña?)."
                      % (batch_id, os.path.dirname(path)))
    with open(path, "rb") as fh:
        record = _strict_json(fh.read(), "la tanda %s" % batch_id)
    if not isinstance(record, dict):
        raise Refusal("UNREADABLE", "La tanda %s no es un objeto JSON." % batch_id)
    if record.get("id") != batch_id:
        raise Refusal("BATCH_ID_MISMATCH", "El archivo de la tanda %s dice ser de otra tanda." % batch_id)
    if record.get("status") != "PUBLISHED":
        raise Refusal("NOT_PUBLISHED",
                      "La tanda %s no está publicada (estado %s): solo una tanda publicada va a producción."
                      % (batch_id, record.get("status")))
    if not isinstance(record.get("publication"), dict):
        raise Refusal("NOT_PUBLISHED", "La tanda %s no registra su publicación." % batch_id)
    return record


def _registered_digest(publication, batch_id):
    """La huella sha256 de la lista que registró la publicación, en minúscula."""
    digest = publication.get("manifestDigest")
    if not digest:
        # Decisión 14: las tandas publicadas antes de la lista no tienen qué llevar.
        raise Refusal("NO_MANIFEST",
                      "La tanda %s se publicó sin lista de publicación (antes de este paso). "
                      "El paso a producción solo toma lo que dice la lista, así que esta tanda "
                      "no se puede llevar por acá." % batch_id)
    if not isinstance(digest, str) or not SHA256_HEX.match(digest.lower()):
        raise Refusal("BAD_DIGEST", "La huella registrada de la lista de %s no es un sha256." % batch_id)
    return digest.lower()


def _published_manifest(workdir, batch_id, digest):
    """(ruta, bytes) de la lista, si es la que se publicó: una lista cambiada se niega."""
    path = manifest_file_path(workdir, batch_id)
    if not os.path.isfile(path):
        raise Refusal("MANIFEST_MISSING", "Falta la lista de publicación de %s en %s." % (batch_id, path))
    with open(path, "rb") as fh:
        raw = fh.read()
    actual = hashlib.sha256(raw).hexdigest()
    if actual != digest:
        raise Refusal("DIGEST_MISMATCH",
                      "La lista de %s no es la que se publicó: su huella es %s y la tanda registró %s. "
                      "No se publica una lista cambiada." % (batch_id, actual, digest))
    return path, raw


def _verdict_problems(where, kind, gate_passed, floors_clear, problems):
    """Lo que habilita publicar: veredicto «PASSES», compuerta pasada y ningún piso sin decidir.

    Se mira dos veces (F-PUBPROD-R005): en la publicación que registró la tanda y en la lista.
    """
    if kind != "PASSES":
        problems.append(("VERDICT_NOT_PASSES", "%s registra el veredicto %s, no «PASSES»." % (where, _shown(kind))))
    if gate_passed is not True:
        problems.append(("GATE_NOT_PASSED", "%s no registra que la compuerta del último cierre pasó "
                                            "(gatePassed=%s)." % (where, _shown(gate_passed))))
    if not floors_clear:
        problems.append(("UNDECIDABLE_FLOORS", "%s registra pisos sin decidir (o no dice cuáles): con «no se "
                                               "puede decidir» no se publica." % where))


def _no_floors(value):
    """Si la publicación de la tanda dice que no hay pisos sin decidir.

    Java usa null para «todos los pisos decididos» (DefaultBatchComparator.undecidableFloors);
    también valen la lista vacía, la cantidad 0 o un objeto con count 0 y sin analizadores.
    En la lista, en cambio, el esquema pide exactamente [].
    """
    if value is None:
        return True
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
    if path == "kind":
        ok, expected = after in ("CLOZE", "MULTIPLE_CHOICE"), "un tipo de conversión conocido"
    elif path in ("form", "clozeOriginal"):
        ok, expected = isinstance(after, dict), "un documento completo"
    elif path in ("translation", "instructions", "miniTheory"):
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


def _is_text(value):
    return isinstance(value, str) and value != ""


def _is_text_or_null(value):
    """Solo donde el lado Java manda null (source.publishedAuditId y verdict.detail)."""
    return value is None or isinstance(value, str)


def _is_count(value):
    """Un entero no negativo de verdad: en Python True vale 1 y 3.0 == 3."""
    return type(value) is int and value >= 0


def _shown(value):
    text = json.dumps(value, ensure_ascii=False)
    return text if len(text) <= 60 else text[:57] + "..."


def _exact_keys(value, keys, where, code, problems):
    """Si `value` es un objeto con exactamente las claves del esquema. Lo que sobra o falta se niega."""
    if not isinstance(value, dict):
        problems.append((code, "%s no es un objeto." % where))
        return False
    for key in value:
        if key not in keys:
            problems.append(("UNKNOWN_KEY", "%s trae la clave «%s», que el esquema %d no tiene."
                             % (where, key, SCHEMA)))
    for key in keys:
        if key not in value:
            problems.append(("MISSING_KEY", "%s no trae la clave «%s»." % (where, key)))
    return True


def _require(obj, key, ok, expected, where, code, problems):
    """El tipo de una clave que vino (si no vino, ya se dijo MISSING_KEY)."""
    if key in obj and not ok(obj[key]):
        problems.append((code, "%s: «%s» tiene que ser %s y es %s." % (where, key, expected, _shown(obj[key]))))


def _section(manifest, key, keys, code, problems):
    """Una parte de la lista que es un objeto, con sus claves exactas. None si falta o no sirve."""
    if key not in manifest:
        return None
    value = manifest[key]
    return value if _exact_keys(value, keys, "«%s»" % key, code, problems) else None


def _items(manifest, key, code, problems):
    """Una parte de la lista que es una lista. [] si falta o no es una lista."""
    if key not in manifest:
        return []
    value = manifest[key]
    if not isinstance(value, list):
        problems.append((code, "«%s» no es una lista." % key))
        return []
    return value


def _header_problems(manifest, expected_batch_id, expected_plan_id, problems):
    where = "La lista"
    for key in ("batchId", "planId", "level", "approvedBy"):
        _require(manifest, key, _is_text, "un texto", where, "BAD_HEADER", problems)
    _require(manifest, "publishedAt", lambda v: isinstance(v, str) and PUBLISHED_AT.match(v),
             "un instante UTC como 2026-10-12T03:10:44Z", where, "BAD_HEADER", problems)
    if expected_batch_id is not None and "batchId" in manifest and manifest["batchId"] != expected_batch_id:
        problems.append(("BATCH_ID_MISMATCH", "La lista dice ser de la tanda %s, no de %s."
                         % (manifest["batchId"], expected_batch_id)))
    if expected_plan_id is not None and "planId" in manifest and manifest["planId"] != expected_plan_id:
        problems.append(("PLAN_ID_MISMATCH", "La lista dice ser del plan %s y la tanda es del plan %s."
                         % (manifest["planId"], expected_plan_id)))


def _source_problems(source, problems):
    for key in ("baselineAuditId", "startAuditId"):
        _require(source, key, _is_text, "un texto", "«source»", "BAD_SOURCE", problems)
    _require(source, "publishedAuditId", _is_text_or_null, "un texto o null", "«source»", "BAD_SOURCE", problems)


def _manifest_verdict_problems(verdict, problems):
    _verdict_problems("La lista", verdict.get("kind"), verdict.get("gatePassed"),
                      verdict.get("undecidableFloors") == [], problems)
    if verdict.get("brokenFloorGoalIds") != []:
        problems.append(("BROKEN_FLOORS", "La lista trae pisos rotos (o no dice cuáles): %s."
                         % _shown(verdict.get("brokenFloorGoalIds"))))
    _require(verdict, "detail", _is_text_or_null, "un texto o null", "«verdict»", "BAD_VERDICT", problems)


def _reviews_problems(reviews, problems):
    """Las cuentas de la revisión. Devuelve los cambios que quedaron afuera por la revisión."""
    for key in ("approved", "pending", "rejected"):
        _require(reviews, key, _is_count, "un entero no negativo", "«reviews»", "BAD_REVIEWS", problems)
    left_out = set()
    for key in ("droppedForReviewChangeIds", "collateralDroppedChangeIds"):
        _require(reviews, key, _is_str_list, "una lista de textos", "«reviews»", "BAD_REVIEWS", problems)
        if _is_str_list(reviews.get(key)):
            left_out.update(reviews[key])
    return left_out


def _excluded_problems(excluded, problems):
    """Los cambios que quedaron afuera. Devuelve sus changeId."""
    left_out = set()
    for index, item in enumerate(excluded):
        where = "excluded[%d]" % index
        if not _exact_keys(item, EXCLUDED_KEYS, where, "BAD_EXCLUDED", problems):
            continue
        _require(item, "changeId", _is_text, "un texto", where, "BAD_EXCLUDED", problems)
        if "reason" in item and item["reason"] not in EXCLUDED_REASONS:
            problems.append(("BAD_EXCLUDED", "%s quedó afuera por un motivo desconocido %s."
                             % (where, _shown(item["reason"]))))
        if isinstance(item.get("changeId"), str):
            left_out.add(item["changeId"])
    return left_out


def _unsupported_problems(unsupported, problems):
    """Lo que no tiene lugar en producción. La parte es un enum del lado Java: no se enumera."""
    for index, item in enumerate(unsupported):
        where = "unsupported[%d]" % index
        if _exact_keys(item, UNSUPPORTED_KEYS, where, "BAD_UNSUPPORTED", problems):
            for key in UNSUPPORTED_KEYS:
                _require(item, key, _is_text, "un texto", where, "BAD_UNSUPPORTED", problems)


def _field_problems(field, collection, where, seen_paths, problems, atomic_conversion=False):
    """Un campo de un documento. Devuelve su ruta si cuenta, o None."""
    if not _exact_keys(field, FIELD_KEYS, "%s: un campo" % where, "BAD_FIELD", problems):
        return None
    path = field.get("path")
    if not isinstance(path, str):
        problems.append(("BAD_FIELD", "%s: un campo no tiene ruta." % where))
        return None
    if (path in FORBIDDEN_PATHS or any(path.startswith(p + ".") for p in FORBIDDEN_PATHS)) and not (atomic_conversion and path == "kind"):
        problems.append(("FORBIDDEN_PATH", "%s: la ruta «%s» está prohibida; nunca se escribe." % (where, path)))
        return None
    if path not in WRITABLE_PATHS[collection] or path in ("form", "clozeOriginal") and not atomic_conversion:
        problems.append(("PATH_NOT_ALLOWED", "%s: la ruta «%s» no está en la tabla de lo que se escribe en %s."
                         % (where, path, collection)))
        return None
    if path in seen_paths:
        problems.append(("DUPLICATE_FIELD", "%s: la ruta «%s» aparece dos veces." % (where, path)))
        return None
    seen_paths.add(path)
    if "before" not in field or "after" not in field:
        return None  # ya se dijo MISSING_KEY
    before, after = field["before"], field["after"]
    restored_null_instruction = atomic_conversion and path == "instructions" and after is None
    if after is None and not restored_null_instruction:
        problems.append(("NULL_AFTER", "%s: el después de «%s» es nulo; nunca se escribe un nulo." % (where, path)))
        return None
    _walk_keys(before, path, problems, where)
    _walk_keys(after, path, problems, where)
    if not restored_null_instruction:
        _check_after_type(path, after, where, problems)
    _check_multiple_choice(path, before, after, where, problems)
    if same_value(before, after) and not (atomic_conversion and path in conversion.PATHS):
        problems.append(("UNCHANGED_FIELD", "%s: «%s» trae el mismo antes y después." % (where, path)))
    return path


def _document_problems(index, doc, left_out, seen_docs, by_path, problems):
    """Un documento de la lista; suma sus campos a `by_path`."""
    if not _exact_keys(doc, DOCUMENT_KEYS, "El documento %d de la lista" % index, "BAD_DOCUMENTS", problems):
        return
    collection, doc_id = doc.get("collection"), doc.get("id")
    where = "%s %s (%s)" % (collection, doc_id, doc.get("nodeId"))
    if collection not in COLLECTIONS:
        problems.append(("COLLECTION_NOT_ALLOWED", "%s: la colección «%s» no se escribe; solo %s."
                         % (where, collection, " y ".join(COLLECTIONS))))
        return
    if not isinstance(doc_id, str) or not HEX_ID.match(doc_id):
        problems.append(("BAD_ID", "%s: el id tiene que ser un ObjectId en hexadecimal." % where))
        return
    if (collection, doc_id) in seen_docs:
        problems.append(("DUPLICATE_DOCUMENT", "%s aparece dos veces en la lista." % where))
        return
    seen_docs.add((collection, doc_id))
    _require(doc, "nodeId", _is_text, "un texto", where, "BAD_DOCUMENTS", problems)
    conversion_problem = conversion.problem(doc)
    if conversion_problem:
        problems.append(("BAD_CONVERSION", "%s: %s" % (where, conversion_problem)))
    change_ids = doc.get("changeIds")
    if "changeIds" in doc and (not _is_str_list(change_ids) or not change_ids):
        problems.append(("BAD_DOCUMENTS", "%s: no dice qué cambios lo originaron." % where))
    _require(doc, "proposalIds", _is_str_list, "una lista de textos", where, "BAD_DOCUMENTS", problems)
    travelling_out = sorted(set(change_ids if _is_str_list(change_ids) else []) & left_out)
    if travelling_out:
        problems.append(("EXCLUDED_CHANGE_TRAVELS", "%s: lo originan cambios que quedaron afuera (%s); lo "
                         "rechazado no viaja." % (where, ", ".join(travelling_out))))
    status = doc.get("reviewStatus")
    if status == "REJECTED":
        problems.append(("REJECTED_DOCUMENT",
                         "%s: está rechazado en la revisión y lo rechazado nunca se publica." % where))
    elif status not in REVIEW_STATUSES:
        problems.append(("BAD_REVIEW_STATUS", "%s: estado de revisión desconocido %s." % (where, _shown(status))))
    fields = doc.get("fields")
    if not isinstance(fields, list) or not fields:
        problems.append(("BAD_DOCUMENTS", "%s: no trae campos." % where))
        return
    seen_paths = set()
    for field in fields:
        path = _field_problems(field, collection, where, seen_paths, problems, conversion.is_conversion(doc.get("fields", [])) and conversion_problem is None)
        if path is not None:
            by_path[path] = by_path.get(path, 0) + 1


def _counts_problems(counts, documents, by_path, problems):
    total_fields = sum(by_path.values())
    if "documents" in counts and (not _is_count(counts["documents"]) or counts["documents"] != len(documents)):
        problems.append(("BAD_COUNTS", "La lista cuenta %s documentos y trae %d."
                         % (_shown(counts["documents"]), len(documents))))
    if "fields" in counts and (not _is_count(counts["fields"]) or counts["fields"] != total_fields):
        problems.append(("BAD_COUNTS", "La lista cuenta %s campos y trae %d."
                         % (_shown(counts["fields"]), total_fields)))
    if "byPath" in counts:
        declared = counts["byPath"]
        if (not isinstance(declared, dict) or not all(_is_count(v) for v in declared.values())
                or declared != by_path):
            problems.append(("BAD_COUNTS", "Las cuentas por ruta de la lista no coinciden con sus campos."))


def validate_manifest(manifest, expected_batch_id=None, expected_plan_id=None):
    """Valida la estructura (esquema 1, claves exactas) y lo que se puede escribir.

    Lanza Refusal con todos los problemas juntos.
    """
    if not isinstance(manifest, dict):
        raise Refusal("BAD_MANIFEST", "La lista no es un objeto JSON.")
    schema = manifest.get("schema")
    if type(schema) is not int or schema != SCHEMA:
        raise Refusal("UNKNOWN_SCHEMA", "La lista es del esquema %s; este paso solo conoce el %d."
                      % (_shown(schema), SCHEMA))
    problems = []
    _exact_keys(manifest, MANIFEST_KEYS, "La lista", "BAD_MANIFEST", problems)
    _header_problems(manifest, expected_batch_id, expected_plan_id, problems)
    source = _section(manifest, "source", SOURCE_KEYS, "BAD_SOURCE", problems)
    if source is not None:
        _source_problems(source, problems)
    verdict = _section(manifest, "verdict", VERDICT_KEYS, "BAD_VERDICT", problems)
    if verdict is not None:
        _manifest_verdict_problems(verdict, problems)
    reviews = _section(manifest, "reviews", REVIEWS_KEYS, "BAD_REVIEWS", problems)
    left_out = _reviews_problems(reviews, problems) if reviews is not None else set()
    left_out |= _excluded_problems(_items(manifest, "excluded", "BAD_EXCLUDED", problems), problems)
    _unsupported_problems(_items(manifest, "unsupported", "BAD_UNSUPPORTED", problems), problems)
    documents = _items(manifest, "documents", "BAD_DOCUMENTS", problems)
    seen_docs, by_path = set(), {}
    for index, doc in enumerate(documents):
        _document_problems(index, doc, left_out, seen_docs, by_path, problems)
    counts = _section(manifest, "counts", COUNTS_KEYS, "BAD_COUNTS", problems)
    if counts is not None:
        _counts_problems(counts, documents, by_path, problems)
    if problems:
        raise Refusal(problems[0][0], "La lista de publicación no se puede llevar a producción: %s"
                      % problems[0][1], problems)
    return manifest

