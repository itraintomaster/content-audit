"""Escribe lo que dice el plan y lo verifica (F-PUBPROD R006 y R007).

  - Escribe documento por documento, solo los campos APPLY, con el «después» de
    la lista, y solo si cada campo sigue teniendo lo que se leyó al comparar
    (compare_and_set). Si un documento cambió entretanto, o la base falla, se
    deja de escribir.
  - Si la base falló (o se cortó a mano) justo en una escritura, esa escritura
    pudo haber llegado o no: se relee el documento. Si ya tiene el «después» en
    todos sus campos, cuenta como escrito y se verifica; si sigue con lo que se
    leyó, no se escribió; si no se puede saber, queda como indeterminado.
  - Después relee cada documento escrito: cada campo de la lista tiene que
    tener su «después» y el resto del documento tiene que estar idéntico
    (misma huella sin las rutas escritas, antes y después).
  - Si algo no coincide: error y registro. Nunca reintenta ni corrige.
  - Un Ctrl-C en cualquier momento no pierde el resultado: lo escrito hasta ahí
    queda en el resultado y lo que no se llegó a verificar queda sin verificar.
"""

from .store import fingerprint_without, get_path, same_value, short

CONCURRENT_CHANGE = "CONCURRENT_CHANGE"
STORE_ERROR = "STORE_ERROR"
INTERRUPTED = "INTERRUPTED"

NOT_VERIFIED = "no se llegó a verificar: se interrumpió"


class ApplyOutcome:
    def __init__(self):
        self.written = []
        self.stopped_reason = None
        self.stopped_at = None
        self.error_class = None
        self.verification = []
        self.indeterminate = []  # [{"collection", "id", "problem"}]: no se sabe si quedó escrito
        self.in_flight = None    # el documento que se está escribiendo
        self.unresolved = None   # una escritura que pudo haber llegado o no

    @property
    def verified(self):
        return all(v["ok"] for v in self.verification)

    def failures(self):
        return [v for v in self.verification if not v["ok"]]

    def indeterminate_keys(self):
        return ["%s/%s" % (i["collection"], i["id"]) for i in self.indeterminate]

    def stop(self, reason, doc, error_class=None):
        """Deja de escribir. Si la escritura en curso pudo haber llegado, queda por resolver."""
        if self.stopped_reason is not None:
            return
        self.stopped_reason, self.stopped_at, self.error_class = reason, doc, error_class
        if reason in (STORE_ERROR, INTERRUPTED) and doc is not None and doc not in self.written:
            self.unresolved = doc


def apply(plan, store, outcome=None):
    """Escribe y verifica. Si el que llama pasa `outcome`, ve lo escrito aunque se escape un error inesperado."""
    outcome = outcome if outcome is not None else ApplyOutcome()
    try:
        _write(plan, store, outcome)
    except KeyboardInterrupt:
        outcome.stop(INTERRUPTED, outcome.in_flight)
    try:
        for doc in list(outcome.written):
            outcome.verification.append(_verify(doc, store))
        _resolve(outcome, store)
    except KeyboardInterrupt:
        _interrupted_while_verifying(outcome)
    return outcome


def _write(plan, store, outcome):
    for doc in plan.to_write():
        outcome.in_flight = doc
        try:
            matched = store.compare_and_set(doc.collection, doc.doc_id, doc.expected(), doc.writes())
        except Exception as e:  # la clase alcanza: el mensaje de la base puede traer el destino
            outcome.stop(STORE_ERROR, doc, type(e).__name__)
            return
        if not matched:
            outcome.in_flight = None
            outcome.stop(CONCURRENT_CHANGE, doc)
            return
        outcome.written.append(doc)
        outcome.in_flight = None


def _resolve(outcome, store):
    """Relee la escritura que pudo haber llegado o no y decide."""
    doc = outcome.unresolved
    if doc is None:
        return
    try:
        fresh = store.find_document(doc.collection, doc.doc_id)
    except Exception as e:
        fresh, problem = None, "no se pudo releer (%s)" % type(e).__name__
    else:
        problem = "el documento ya no existe" if fresh is None else None
    if fresh is not None:
        writes, expected = doc.writes(), doc.expected()
        if all(same_value(get_path(fresh, p), v) for p, v in writes.items()):
            outcome.written.append(doc)
            outcome.unresolved = None
            outcome.verification.append(_compare(doc, fresh))
            return
        if all(same_value(get_path(fresh, p), v) for p, v in expected.items()):
            outcome.unresolved = None  # sigue con lo que se leyó: no se escribió
            return
        problem = "no tiene ni lo que se leyó ni el «después» de la lista"
    outcome.indeterminate.append({"collection": doc.collection, "id": doc.doc_id, "problem": problem})
    outcome.unresolved = None


def _interrupted_while_verifying(outcome):
    if outcome.stopped_reason is None:
        outcome.stopped_reason = INTERRUPTED
    verified = {(v["collection"], v["id"]) for v in outcome.verification}
    for doc in outcome.written:
        if (doc.collection, doc.doc_id) not in verified:
            outcome.verification.append({"collection": doc.collection, "id": doc.doc_id, "ok": False,
                                         "problems": [NOT_VERIFIED]})
    doc = outcome.unresolved
    if doc is not None:
        outcome.indeterminate.append({"collection": doc.collection, "id": doc.doc_id,
                                      "problem": "se interrumpió antes de releerlo"})
        outcome.unresolved = None


def _verify(doc, store):
    try:
        fresh = store.find_document(doc.collection, doc.doc_id)
    except Exception as e:
        return _failed(doc, "no se pudo releer (%s)" % type(e).__name__)
    if fresh is None:
        return _failed(doc, "el documento ya no existe")
    return _compare(doc, fresh)


def _failed(doc, problem):
    return {"collection": doc.collection, "id": doc.doc_id, "ok": False, "problems": [problem]}


def _compare(doc, fresh):
    entry = {"collection": doc.collection, "id": doc.doc_id, "ok": True, "problems": []}
    for f in doc.fields:
        actual = get_path(fresh, f.path)
        if not same_value(actual, f.after):
            entry["ok"] = False
            entry["problems"].append("%s quedó con %s y la lista dice %s"
                                     % (f.path, short(actual), short(f.after)))
    written_paths = list(doc.writes())
    before = fingerprint_without(doc.doc, written_paths)
    after = fingerprint_without(fresh, written_paths)
    entry["restFingerprintBefore"] = before
    entry["restFingerprintAfter"] = after
    if before != after:
        entry["ok"] = False
        entry["problems"].append("cambió algo del documento que no estaba en la lista")
    return entry
