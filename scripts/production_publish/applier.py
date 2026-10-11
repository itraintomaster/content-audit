"""Escribe lo que dice el plan y lo verifica (F-PUBPROD R006 y R007).

  - Escribe documento por documento, solo los campos APPLY, con el «después» de
    la lista, y solo si cada campo sigue teniendo lo que se leyó al comparar
    (compare_and_set). Si un documento cambió entretanto, o la base falla, se
    deja de escribir.
  - Después relee cada documento escrito: cada campo de la lista tiene que
    tener su «después» y el resto del documento tiene que estar idéntico
    (misma huella sin las rutas escritas, antes y después).
  - Si algo no coincide: error y registro. Nunca reintenta ni corrige.
"""

from .store import fingerprint_without, get_path, same_value, short

CONCURRENT_CHANGE = "CONCURRENT_CHANGE"
STORE_ERROR = "STORE_ERROR"
INTERRUPTED = "INTERRUPTED"


class ApplyOutcome:
    def __init__(self):
        self.written = []
        self.stopped_reason = None
        self.stopped_at = None
        self.error_class = None
        self.verification = []

    @property
    def verified(self):
        return all(v["ok"] for v in self.verification)

    def failures(self):
        return [v for v in self.verification if not v["ok"]]


def apply(plan, store):
    outcome = ApplyOutcome()
    for doc in plan.to_write():
        try:
            matched = store.compare_and_set(doc.collection, doc.doc_id, doc.expected(), doc.writes())
        except KeyboardInterrupt:
            outcome.stopped_reason, outcome.stopped_at = INTERRUPTED, doc
            break
        except Exception as e:  # la clase alcanza: el mensaje de la base puede traer el destino
            outcome.stopped_reason, outcome.stopped_at = STORE_ERROR, doc
            outcome.error_class = type(e).__name__
            break
        if not matched:
            outcome.stopped_reason, outcome.stopped_at = CONCURRENT_CHANGE, doc
            break
        outcome.written.append(doc)
    outcome.verification = verify(outcome.written, store)
    return outcome


def verify(written, store):
    results = []
    for doc in written:
        entry = {"collection": doc.collection, "id": doc.doc_id, "ok": True, "problems": []}
        try:
            fresh = store.find_document(doc.collection, doc.doc_id)
        except Exception as e:
            entry["ok"] = False
            entry["problems"].append("no se pudo releer (%s)" % type(e).__name__)
            results.append(entry)
            continue
        if fresh is None:
            entry["ok"] = False
            entry["problems"].append("el documento ya no existe")
            results.append(entry)
            continue
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
        results.append(entry)
    return results
