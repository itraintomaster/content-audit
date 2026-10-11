"""Comparar-y-fijar: qué hay que escribir en cada documento (F-PUBPROD-R006).

Por cada campo de la lista se lee el valor actual en la base:

  - igual al «después»  -> ALREADY (ya aplicado, no se escribe)
  - igual al «antes»    -> APPLY   (se escribe el «después» de la lista)
  - cualquier otra cosa -> DRIFT   (alguien lo cambió: no se pisa)

Un documento con algún campo en DRIFT, o que no existe en la base (MISSING),
frena toda la corrida salvo `--skip-drift`, que saltea esos documentos enteros
(nunca un campo suelto: un ejercicio a medio publicar es peor que uno sin publicar).

La oración guardada ausente (o nula) en la base cuenta como lista vacía: así la
lleva la lista (F-PUBPROD-R002, inv. 3). El filtro de la escritura igual usa el
valor tal como se leyó.

Las copias embebidas de la consigna en las instancias (`quizzes.template`),
acotadas a los ejercicios de la lista, se llevan al «después» solo si estaban al
día (tenían el «antes»). Una copia que ya estaba desfasada antes de esta corrida
no se toca y no frena nada: queda registrada como desfasada. Es lo que
sincronizaba `sync_quiz_titles` del camino por plan, sin pisar lo que no se sabe.
"""

from .manifest_reader import KNOWLEDGES, QUIZ_TEMPLATES
from .store import MISSING, fingerprint_without, get_path, same_value, short, to_json_safe

APPLY = "APPLY"
ALREADY = "ALREADY"
DRIFT = "DRIFT"
MISSING_DOC = "MISSING"

# Por qué se saltea un documento.
SKIP_DRIFT = "DRIFT"                # --skip-drift: deriva o inexistente
SKIP_STALE_COPY = "STALE_COPY"      # copia embebida que ya estaba desfasada
SKIP_MASTER = "MASTER_SKIPPED"      # instancia de un ejercicio salteado

# F-PUBPROD-R002 inv. 3: la lista lleva la oración guardada ausente como lista vacía.
ABSENT_AS_EMPTY_LIST = ("form.sentences",)

INSTANCES = "quizzes"
# Campo del template -> copia embebida en la instancia (como sync_quiz_titles).
EMBEDDED_COPIES = {"instructions": "template.instructions", "title": "template.title"}

# Orden de escritura: knowledges -> quizTemplates -> copias embebidas en quizzes.
COLLECTION_ORDER = (KNOWLEDGES, QUIZ_TEMPLATES)


def as_listed(path, value):
    """El valor de la base tal como lo representa la lista."""
    if path in ABSENT_AS_EMPTY_LIST and (value is MISSING or value is None):
        return []
    return value


class FieldPlan:
    def __init__(self, path, before, after, current):
        self.path = path
        self.before = before
        self.after = after
        self.current = current  # tal como se leyó: es el filtro de la escritura y el respaldo
        listed = as_listed(path, current)
        if same_value(listed, after):
            self.status = ALREADY
        elif same_value(listed, before):
            self.status = APPLY
        else:
            self.status = DRIFT


class DocPlan:
    """Un documento de la lista (o una instancia con la copia embebida) y su comparación."""

    def __init__(self, collection, doc_id, fields, doc, node_id=None, review_status=None,
                 change_ids=None, template_id=None):
        self.collection = collection
        self.doc_id = doc_id
        self.node_id = node_id
        self.review_status = review_status
        self.change_ids = list(change_ids or [])
        self.template_id = template_id
        self.fields = fields
        self.doc = doc
        self.skipped = False
        self.skip_reason = None
        if doc is None:
            self.status = MISSING_DOC
        elif any(f.status == DRIFT for f in fields):
            self.status = DRIFT
        elif all(f.status == ALREADY for f in fields):
            self.status = ALREADY
        else:
            self.status = APPLY

    @property
    def is_instance(self):
        return self.template_id is not None

    @property
    def key(self):
        return "%s/%s" % (self.collection, self.doc_id)

    def writes(self):
        return {f.path: f.after for f in self.fields if f.status == APPLY}

    def expected(self):
        return {f.path: f.current for f in self.fields if f.status == APPLY}

    def rest_fingerprint(self):
        return fingerprint_without(self.doc, list(self.writes()))

    def describe(self):
        label = self.key
        if self.node_id:
            label += " (%s)" % self.node_id
        if self.is_instance:
            label += " [instancia del ejercicio %s]" % self.template_id
        return label

    def skip(self, reason):
        self.skipped = True
        self.skip_reason = reason

    def detail(self, include_values):
        out = {"collection": self.collection, "id": self.doc_id, "status": self.status,
               "skipped": self.skipped}
        if self.skip_reason is not None:
            out["skipReason"] = self.skip_reason
        if self.node_id is not None:
            out["nodeId"] = self.node_id
        if self.review_status is not None:
            out["reviewStatus"] = self.review_status
        if self.change_ids:
            out["changeIds"] = self.change_ids
        if self.template_id is not None:
            out["templateId"] = self.template_id
        fields = []
        for f in self.fields:
            entry = {"path": f.path, "status": f.status}
            if include_values and f.status == APPLY:
                # El respaldo: lo que había (old) y lo que se escribe (new).
                entry["old"] = to_json_safe(f.current)
                entry["new"] = to_json_safe(f.after)
            elif f.status == DRIFT:
                entry["current"] = to_json_safe(f.current)
                entry["expectedBefore"] = to_json_safe(f.before)
                entry["after"] = to_json_safe(f.after)
            fields.append(entry)
        out["fields"] = fields
        if self.doc is not None and include_values and self.status == APPLY:
            out["restFingerprintBefore"] = self.rest_fingerprint()
        return out


class WritePlan:
    def __init__(self, documents, instances, skip_drift):
        self.documents = documents
        self.instances = instances
        self.skip_drift = skip_drift

    def all(self):
        return list(self.documents) + list(self.instances)

    def problems(self):
        """Documentos que frenan la corrida: deriva o inexistentes, sin saltear."""
        return [d for d in self.all() if d.status in (DRIFT, MISSING_DOC) and not d.skipped]

    def skipped(self, reason=None):
        return [d for d in self.all() if d.skipped and (reason is None or d.skip_reason == reason)]

    def is_clean(self):
        return not self.problems()

    def to_write(self):
        """En orden: knowledges, quizTemplates y después las instancias."""
        return [d for d in self.all() if d.status == APPLY and not d.skipped]

    def counts(self):
        counts = {"documents": len(self.documents), "instances": len(self.instances),
                  "apply": 0, "already": 0, "drift": 0, "missing": 0,
                  "skippedDrift": len(self.skipped(SKIP_DRIFT)),
                  "staleCopies": len(self.skipped(SKIP_STALE_COPY)),
                  "skippedWithMaster": len(self.skipped(SKIP_MASTER)),
                  "documentsToWrite": len(self.to_write()),
                  "fieldsToWrite": 0, "fieldsAlready": 0, "fieldsDrift": 0, "byPath": {}}
        for d in self.all():
            if d.skip_reason == SKIP_STALE_COPY:
                continue  # ya estaba desfasada antes de esta corrida: va en staleCopies
            counts[{APPLY: "apply", ALREADY: "already", DRIFT: "drift", MISSING_DOC: "missing"}[d.status]] += 1
            for f in d.fields:
                if d.status == MISSING_DOC:
                    continue
                if f.status == APPLY and d.status == APPLY and not d.skipped:
                    counts["fieldsToWrite"] += 1
                    counts["byPath"][f.path] = counts["byPath"].get(f.path, 0) + 1
                elif f.status == ALREADY:
                    counts["fieldsAlready"] += 1
                elif f.status == DRIFT:
                    counts["fieldsDrift"] += 1
        return counts

    def pending_documents(self, docs):
        """Cuántos de esos documentos (no instancias) viajan sin que nadie los haya revisado."""
        return sum(1 for d in docs if not d.is_instance and d.review_status == "PENDING")

    def report_lines(self, limit=30):
        lines = []
        for d in self.all():
            if d.status == MISSING_DOC:
                lines.append("  %s %s: no existe en el destino" % ("SALTEADO" if d.skipped else "FALTA",
                                                                  d.describe()))
            elif d.status == DRIFT:
                tag = {SKIP_DRIFT: "SALTEADO", SKIP_STALE_COPY: "COPIA DESFASADA (no se toca)",
                       SKIP_MASTER: "SALTEADO CON SU EJERCICIO"}.get(d.skip_reason, "DERIVA")
                for f in d.fields:
                    if f.status == DRIFT:
                        lines.append("  %s %s %s: en el destino hay %s; la lista esperaba %s"
                                     % (tag, d.describe(), f.path, short(f.current), short(f.before)))
        if len(lines) > limit:
            lines = lines[:limit] + ["  … y %d más (todo el detalle queda en el registro de la corrida)"
                                     % (len(lines) - limit)]
        return lines


def build(manifest, store, skip_drift):
    """Lee cada documento de la lista y arma el plan. No escribe nada."""
    ordered = sorted(manifest["documents"], key=lambda d: COLLECTION_ORDER.index(d["collection"]))
    documents = []
    for entry in ordered:
        doc = store.find_document(entry["collection"], entry["id"])
        fields = [FieldPlan(f["path"], f["before"], f["after"],
                            get_path(doc, f["path"]) if doc is not None else MISSING)
                  for f in entry["fields"]]
        documents.append(DocPlan(entry["collection"], entry["id"], fields, doc,
                                 node_id=entry.get("nodeId"), review_status=entry.get("reviewStatus"),
                                 change_ids=entry.get("changeIds")))

    # Copias embebidas: solo de los ejercicios de la lista que cambian un campo copiado.
    masters = {}
    for d in documents:
        if d.collection != QUIZ_TEMPLATES or d.doc is None:
            continue
        copied = [f for f in d.fields if f.path in EMBEDDED_COPIES]
        if copied:
            masters[d.doc_id] = (d, copied)
    instances = []
    if masters:
        for instance_id, inst in store.find_instances(sorted(masters)):
            template = inst.get("template") if isinstance(inst, dict) else None
            template_id = str(template.get("_id")) if isinstance(template, dict) else None
            if template_id not in masters:
                continue
            master, copied = masters[template_id]
            fields = [FieldPlan(EMBEDDED_COPIES[f.path], f.before, f.after,
                                get_path(inst, EMBEDDED_COPIES[f.path])) for f in copied]
            instances.append(DocPlan(INSTANCES, instance_id, fields, inst, node_id=master.node_id,
                                     template_id=template_id))

    plan = WritePlan(documents, instances, skip_drift)
    for inst in instances:
        if inst.status == DRIFT:
            inst.skip(SKIP_STALE_COPY)
    if skip_drift:
        for d in documents:
            if d.status in (DRIFT, MISSING_DOC):
                d.skip(SKIP_DRIFT)
        skipped_masters = {d.doc_id for d in documents if d.skipped and d.collection == QUIZ_TEMPLATES}
        for inst in instances:
            if inst.template_id in skipped_masters and not inst.skipped:
                inst.skip(SKIP_MASTER)
    return plan
