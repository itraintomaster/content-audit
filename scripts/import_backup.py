#!/usr/bin/env python3
"""Rearma db/ desde un backup de producción (salida de mongodump).

Lee courses.bson, knowledges.bson y quizTemplates.bson del backup y escribe:

  db/english-course/            el árbol que lee FileSystemCourseRepository
    _course.json                  curso + nodo ROOT
    <milestone>/_milestone.json
    <milestone>/<topic>/_topic.json
    <milestone>/<topic>/<knowledge>/_knowledge.json
    <milestone>/<topic>/<knowledge>/quizzes.json
  db/courses.json               exports planos, la colección entera tal cual
  db/knowledges.json            (bsondump | jq -s, como el import original)
  db/quiz-templates.json
  db/.import-metadata.json      fecha, ruta, sha256 y conteos

El árbol es el curso vivo tal como lo arma el backend: ROOT.children →
MILESTONE.children → TOPIC.ruleIds, que tiene que coincidir con
course.knowledgeIds (si no coincide, aborta). Los knowledges que no están en el
curso (p. ej. los fundidos) y sus ejercicios quedan sólo en los exports planos.

Los documentos se escriben completos, sin convertir nada: los campos que el
modelo Java no conoce (form.items y form.selection de la opción múltiple,
formCloze, los respaldos *Antes*) se conservan después de los conocidos. El
formato es el del writer de Jackson, así que un load->save del repositorio no
reformatea los archivos. Los ejercicios de cada knowledge van ordenados por _id.

Dos datos son de content-audit y no están en producción; se arrastran del
árbol anterior (db/english-course) por id:

  - _knowledge.json.sentenceMode (FEAT-SMODE).
  - form.sentences de los ejercicios CLOZE (F-DBSENT): si el backup ya la trae
    y el contenido no cambió, queda la del backup; si no la trae y el
    contenido (textos + respuestas) es igual al del árbol anterior, se copia
    del árbol anterior; si el contenido cambió, se deriva con el mismo código
    que usa content-audit al aplicar una corrección (DerivePlainSentences.java).
    La opción múltiple no se toca: el modelo Java todavía no la conoce.

Uso:
  python3 scripts/import_backup.py <dir-del-backup>            # escribe db/
  python3 scripts/import_backup.py <dir-del-backup> --dry-run  # sólo informa
  python3 scripts/import_backup.py --verify <dir-del-backup>   # compara db/ contra el backup

Requiere bsondump y jq (brew install mongodb-database-tools jq), y el proyecto
compilado (usa el classpath de audit-cli.sh para derivar oraciones).
"""

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unicodedata
from collections import Counter, OrderedDict
from datetime import datetime, timezone

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DERIVER = os.path.join(REPO, "scripts", "DerivePlainSentences.java")

# Colección del backup -> export plano en db/
COLLECTIONS = (("courses", "courses"), ("knowledges", "knowledges"), ("quizTemplates", "quiz-templates"))

# Orden de campos del writer (FileSystemCourseRepository). Lo que no está acá va después, en su orden original.
ROOT_KEYS = ("_id", "code", "kind", "label", "children")
MILESTONE_KEYS = ("_id", "children", "code", "kind", "label", "oldId", "parentId")
TOPIC_KEYS = ("_id", "children", "code", "kind", "label", "oldId", "parentId", "ruleIds")
KNOWLEDGE_KEYS = ("_id", "code", "isRule", "kind", "label", "oldId", "parentId", "instructions", "sentenceMode")
QUIZ_KEYS = ("_id", "id", "kind", "knowledgeId", "title", "instructions", "translation", "theoryId", "topicName",
             "difficulty", "retries", "noScoreRetries", "code", "audioUrl", "imageUrl", "answerAudioUrl",
             "answerImageUrl", "miniTheory", "successMessage", "form")
FORM_KEYS = ("kind", "incidence", "label", "name", "sentenceParts", "sentences")
PART_KEYS = ("kind", "text", "options")


# ---------------------------------------------------------------------------
# Formato: DefaultPrettyPrinter de Jackson (objectMapper.writerWithDefaultPrettyPrinter)
# ---------------------------------------------------------------------------

_SHORT_ESCAPES = {'"': '\\"', "\\": "\\\\", "\b": "\\b", "\t": "\\t", "\n": "\\n", "\f": "\\f", "\r": "\\r"}


def _jackson_string(s):
    out = []
    for ch in s:
        if ch in _SHORT_ESCAPES:
            out.append(_SHORT_ESCAPES[ch])
        elif ord(ch) < 0x20:
            out.append("\\u%04X" % ord(ch))
        else:
            out.append(ch)
    return '"' + "".join(out) + '"'


def jackson_dumps(value, level=0):
    if isinstance(value, dict):
        if not value:
            return "{ }"
        inner = "\n" + "  " * (level + 1)
        entries = [_jackson_string(k) + " : " + jackson_dumps(v, level + 1) for k, v in value.items()]
        return "{" + inner + ("," + inner).join(entries) + "\n" + "  " * level + "}"
    if isinstance(value, list):
        if not value:
            return "[ ]"
        return "[ " + ", ".join(jackson_dumps(v, level) for v in value) + " ]"
    if isinstance(value, str):
        return _jackson_string(value)
    if value is True:
        return "true"
    if value is False:
        return "false"
    if value is None:
        return "null"
    return json.dumps(value)


def ordered(doc, keys):
    """Campos conocidos en el orden del writer; los demás después, en su orden original."""
    out = OrderedDict((k, doc[k]) for k in keys if k in doc)
    for k, v in doc.items():
        if k not in out:
            out[k] = v
    return out


# ---------------------------------------------------------------------------
# Lectura
# ---------------------------------------------------------------------------

def oid(value):
    return value.get("$oid") if isinstance(value, dict) else value


def sha256_of(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_bson(path):
    out = subprocess.run(["bsondump", "--quiet", path], check=True, capture_output=True, text=True).stdout
    return [json.loads(line) for line in out.splitlines() if line.strip()]


def slugify(label):
    """Mismo algoritmo que FileSystemCourseRepository.generateSlug."""
    s = unicodedata.normalize("NFD", label or "")
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Mn").lower()
    s = re.sub(r"\s+", "-", s)
    s = re.sub(r"[^a-z0-9\-]", "", s)
    s = re.sub(r"-{2,}", "-", s)
    return s.strip("-")


def content_signature(form):
    """Lo que se lee y se responde: textos fijos y respuestas aceptadas de cada hueco."""
    sig = []
    for part in (form or {}).get("sentenceParts") or []:
        if part.get("kind") == "TEXT":
            sig.append(("T", part.get("text")))
        else:
            sig.append(("C", tuple(part.get("options") or ())))
    return sig


def read_previous_tree(course_dir):
    """Índice del árbol anterior: carpeta de cada nodo, sentenceMode y form.sentences por id."""
    prev = {"paths": {}, "sentence_mode": {}, "quizzes": {}}
    if not os.path.isdir(course_dir):
        return prev
    for dirpath, _dirs, files in os.walk(course_dir):
        rel = os.path.relpath(dirpath, course_dir)
        for name in ("_milestone.json", "_topic.json", "_knowledge.json"):
            if name in files:
                with open(os.path.join(dirpath, name), encoding="utf-8") as f:
                    node = json.load(f)
                prev["paths"][oid(node["_id"])] = rel
                if name == "_knowledge.json" and node.get("sentenceMode"):
                    prev["sentence_mode"][oid(node["_id"])] = node["sentenceMode"]
        if "quizzes.json" in files:
            with open(os.path.join(dirpath, "quizzes.json"), encoding="utf-8") as f:
                for quiz in json.load(f):
                    form = quiz.get("form") or {}
                    prev["quizzes"][quiz["id"]] = (content_signature(form), form.get("sentences"))
    return prev


# ---------------------------------------------------------------------------
# Curso vivo
# ---------------------------------------------------------------------------

def build_live_course(courses, knowledges, quizzes):
    if len(courses) != 1:
        sys.exit("El backup trae %d cursos; se espera exactamente uno." % len(courses))
    course = courses[0]
    by_id = {oid(k["_id"]): k for k in knowledges}
    roots = [k for k in knowledges if k.get("kind") == "ROOT"]
    if len(roots) != 1:
        sys.exit("El backup trae %d nodos ROOT; se espera exactamente uno." % len(roots))
    root = roots[0]

    quizzes_by_knowledge = {}
    for q in quizzes:
        quizzes_by_knowledge.setdefault(oid(q["knowledgeId"]), []).append(q)

    milestones = []
    walked = []
    for mid in map(oid, root.get("children") or []):
        m = by_id.get(mid)
        if not m or m.get("kind") != "MILESTONE" or oid(m.get("parentId")) != oid(root["_id"]):
            sys.exit("ROOT.children referencia %s, que no es un MILESTONE hijo de ROOT." % mid)
        topics = []
        for tid in map(oid, m.get("children") or []):
            t = by_id.get(tid)
            if not t or t.get("kind") != "TOPIC" or oid(t.get("parentId")) != mid:
                sys.exit("El milestone %s referencia %s, que no es un TOPIC hijo suyo." % (mid, tid))
            ks = []
            for kid in map(oid, t.get("ruleIds") or []):
                k = by_id.get(kid)
                if not k or k.get("kind") != "KNOWLEDGE" or oid(k.get("parentId")) != tid:
                    sys.exit("El topic %s referencia %s, que no es un KNOWLEDGE hijo suyo." % (tid, kid))
                qs = sorted(quizzes_by_knowledge.get(kid, []), key=lambda q: oid(q["_id"]))
                if not qs:
                    sys.exit("El knowledge %s (%s) no tiene ejercicios." % (kid, k.get("label")))
                for q in qs:
                    if q.get("id") != oid(q["_id"]):
                        sys.exit("El ejercicio %s tiene id distinto de _id." % oid(q["_id"]))
                ks.append((k, qs))
                walked.append(kid)
            topics.append((t, ks))
        milestones.append((m, topics))

    course_ids = [oid(x) for x in course.get("knowledgeIds") or []]
    if walked != course_ids:
        sys.exit("El árbol ROOT→milestones→topics→ruleIds (%d knowledges) no coincide con course.knowledgeIds (%d)."
                 % (len(walked), len(course_ids)))
    return course, root, milestones


# ---------------------------------------------------------------------------
# Oraciones (form.sentences)
# ---------------------------------------------------------------------------

def audit_cli_classpath():
    with open(os.path.join(REPO, "audit-cli.sh"), encoding="utf-8") as f:
        match = re.search(r'-cp "([^"]+)"', f.read())
    if not match:
        sys.exit("No encontré el classpath en audit-cli.sh.")
    return match.group(1)


def derive_sentences(items):
    """Corre DerivePlainSentences.java sobre [{id, mode, sentenceParts}] y devuelve {id: [oraciones]}."""
    if not items:
        return {}
    with tempfile.TemporaryDirectory(prefix="import-backup-") as tmp:
        src, dst = os.path.join(tmp, "in.json"), os.path.join(tmp, "out.json")
        with open(src, "w", encoding="utf-8") as f:
            json.dump(items, f, ensure_ascii=False)
        subprocess.run(["java", "-cp", audit_cli_classpath(), DERIVER, src, dst], check=True)
        with open(dst, encoding="utf-8") as f:
            result = json.load(f)
    if result["errors"]:
        for qid, msg in sorted(result["errors"].items()):
            print("  no se pudo derivar %s: %s" % (qid, msg), file=sys.stderr)
        sys.exit("Falló la derivación de %d oraciones." % len(result["errors"]))
    return result["sentences"]


def _squeeze(sentences):
    return [re.sub(r"\s+", "", s) for s in sentences or []]


def assign_sentences(milestones, prev):
    """Decide form.sentences de cada CLOZE. Devuelve ({quiz_id: oraciones}, estadística, avisos, ids derivados).

    La fuente de form.sentences es content-audit: si el árbol anterior tiene el mismo contenido,
    manda su oración. Si no, se deriva; la del backup se conserva sólo si dice lo mismo que la
    derivada (salvo espacios), porque producción puede traer una form.sentences que no se
    recalculó cuando se corrigieron las respuestas.
    """
    chosen, stats, notes, to_derive = {}, Counter(), [], []
    backup_sentences = {}
    for _m, topics in milestones:
        for _t, ks in topics:
            for k, qs in ks:
                mode = prev["sentence_mode"].get(oid(k["_id"]))
                for q in qs:
                    form = q.get("form") or {}
                    if form.get("kind") != "CLOZE":
                        stats["sin form.sentences (%s)" % form.get("kind")] += 1
                        continue
                    prev_sig, prev_sentences = prev["quizzes"].get(q["id"], (None, None))
                    if prev_sentences and prev_sig == content_signature(form):
                        chosen[q["id"]] = prev_sentences
                        if not form.get("sentences"):
                            stats["del árbol anterior (el backup no la trae)"] += 1
                        elif form["sentences"] == prev_sentences:
                            stats["del árbol anterior (igual a la del backup)"] += 1
                        else:
                            stats["del árbol anterior (la del backup está desactualizada)"] += 1
                        continue
                    to_derive.append({"id": q["id"], "mode": mode, "sentenceParts": form.get("sentenceParts") or []})
                    if form.get("sentences"):
                        backup_sentences[q["id"]] = form["sentences"]
    derived = derive_sentences(to_derive)
    for item in to_derive:
        qid = item["id"]
        from_backup = backup_sentences.get(qid)
        if from_backup and _squeeze(from_backup) == _squeeze(derived[qid]):
            chosen[qid] = from_backup
            stats["del backup (coincide con la derivada)"] += 1
            continue
        chosen[qid] = derived[qid]
        stats["derivadas (contenido nuevo o cambiado)"] += 1
        if from_backup:
            stats["derivadas: la del backup estaba desactualizada"] += 1
            notes.append("%s: la form.sentences del backup no corresponde a sus respuestas: %r" % (qid, from_backup[0]))
        _sig, before = prev["quizzes"].get(qid, (None, None))
        if before and before[:1] != derived[qid][:1]:
            notes.append("%s: la oración cambia: %r -> %r" % (qid, before[0], derived[qid][0]))
    derived_ids = [i["id"] for i in to_derive if chosen[i["id"]] is derived[i["id"]]]
    return chosen, stats, notes, derived_ids


# ---------------------------------------------------------------------------
# Escritura
# ---------------------------------------------------------------------------

def plan_tree(course, root, milestones, prev, sentences):
    """Devuelve [(ruta relativa, documento)] del árbol nuevo."""
    files = []
    course_doc = OrderedDict([("_id", course["_id"]), ("title", course["title"]),
                              ("knowledgeIds", course["knowledgeIds"])])
    for k, v in course.items():
        if k not in course_doc:
            course_doc[k] = v
    course_doc["root"] = ordered(root, ROOT_KEYS)
    files.append(("_course.json", course_doc))

    def folder(node, parent_rel, taken):
        rel = prev["paths"].get(oid(node["_id"]))
        if rel is not None and os.path.dirname(rel) == parent_rel:
            name = os.path.basename(rel)
        else:
            name = slugify(node.get("label")) or oid(node["_id"])
        base, n = name, 2
        while name in taken:
            name, n = "%s-%d" % (base, n), n + 1
        taken.add(name)
        return name if parent_rel == "" else parent_rel + "/" + name

    taken_m = set()
    for m, topics in milestones:
        m_rel = folder(m, "", taken_m)
        files.append((m_rel + "/_milestone.json", ordered(m, MILESTONE_KEYS)))
        taken_t = set()
        for t, ks in topics:
            t_rel = folder(t, m_rel, taken_t)
            files.append((t_rel + "/_topic.json", ordered(t, TOPIC_KEYS)))
            taken_k = set()
            for k, qs in ks:
                k_rel = folder(k, t_rel, taken_k)
                kdoc = dict(k)
                mode = prev["sentence_mode"].get(oid(k["_id"]))
                if mode and "sentenceMode" not in kdoc:
                    kdoc["sentenceMode"] = mode
                files.append((k_rel + "/_knowledge.json", ordered(kdoc, KNOWLEDGE_KEYS)))
                quiz_docs = []
                for q in qs:
                    form = dict(q["form"])
                    if q["id"] in sentences:
                        form["sentences"] = sentences[q["id"]]
                    form["sentenceParts"] = [ordered(p, PART_KEYS) for p in form.get("sentenceParts") or []]
                    qdoc = dict(q)
                    qdoc["form"] = ordered(form, FORM_KEYS)
                    quiz_docs.append(ordered(qdoc, QUIZ_KEYS))
                files.append((k_rel + "/quizzes.json", quiz_docs))
    return files


def write_tree(course_dir, files):
    parent = os.path.dirname(course_dir)
    staging = tempfile.mkdtemp(prefix=".import-staging-", dir=parent)
    for rel, doc in files:
        path = os.path.join(staging, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(jackson_dumps(doc))
    if os.path.isdir(course_dir):
        shutil.rmtree(course_dir)  # el árbol anterior queda en git; main() exige db/ limpio antes de llegar acá
    os.rename(staging, course_dir)


def write_flat_export(bson_path, json_path):
    with open(json_path, "w", encoding="utf-8") as out:
        dump = subprocess.Popen(["bsondump", "--quiet", bson_path], stdout=subprocess.PIPE)
        subprocess.run(["jq", "-s", "."], stdin=dump.stdout, stdout=out, check=True)
        if dump.wait() != 0:
            sys.exit("bsondump falló sobre %s" % bson_path)


def git_dirty(path):
    out = subprocess.run(["git", "-C", REPO, "status", "--porcelain", "--", path],
                         capture_output=True, text=True, check=True).stdout
    return out.strip()


# ---------------------------------------------------------------------------
# Verificación
# ---------------------------------------------------------------------------

def strip_local(doc, kind):
    """Saca lo que agrega content-audit, para comparar contra el backup."""
    doc = json.loads(json.dumps(doc))
    if kind == "knowledge":
        doc.pop("sentenceMode", None)
    return doc


def verify(db_dir, backup_dir):
    course_dir = os.path.join(db_dir, "english-course")
    courses = read_bson(os.path.join(backup_dir, "courses.bson"))
    knowledges = read_bson(os.path.join(backup_dir, "knowledges.bson"))
    quizzes = read_bson(os.path.join(backup_dir, "quizTemplates.bson"))
    course, root, milestones = build_live_course(courses, knowledges, quizzes)
    backup_quiz = {q["id"]: q for q in quizzes}
    backup_node = {oid(k["_id"]): k for k in knowledges}
    problems = []
    counts = Counter()

    with open(os.path.join(course_dir, "_course.json"), encoding="utf-8") as f:
        cdoc = json.load(f)
    if [oid(x) for x in cdoc["knowledgeIds"]] != [oid(x) for x in course["knowledgeIds"]]:
        problems.append("_course.json: knowledgeIds distinto del backup")
    if cdoc["root"] != root:
        problems.append("_course.json: nodo ROOT distinto del backup")

    seen_knowledges = []
    for dirpath, _dirs, files in sorted(os.walk(course_dir)):
        for name, kind in (("_milestone.json", "milestone"), ("_topic.json", "topic"), ("_knowledge.json", "knowledge")):
            if name not in files:
                continue
            with open(os.path.join(dirpath, name), encoding="utf-8") as f:
                node = json.load(f)
            counts[kind] += 1
            if strip_local(node, kind) != backup_node.get(oid(node["_id"])):
                problems.append("%s: difiere del backup" % os.path.relpath(os.path.join(dirpath, name), db_dir))
            if kind == "knowledge":
                seen_knowledges.append(oid(node["_id"]))
                if not node.get("sentenceMode"):
                    counts["knowledges sin sentenceMode"] += 1
        if "quizzes.json" in files:
            with open(os.path.join(dirpath, "quizzes.json"), encoding="utf-8") as f:
                for q in json.load(f):
                    counts["quizzes"] += 1
                    counts["form.kind=%s" % q["form"].get("kind")] += 1
                    b = backup_quiz.get(q["id"])
                    if b is None:
                        problems.append("%s: no está en el backup" % q["id"])
                        continue
                    local = json.loads(json.dumps(q))
                    remote = json.loads(json.dumps(b))
                    local_sentences = local["form"].pop("sentences", None)
                    remote_sentences = remote["form"].pop("sentences", None)
                    if local != remote:
                        problems.append("%s: difiere del backup" % q["id"])
                    elif remote_sentences is not None and local_sentences != remote_sentences:
                        # El backup trae form.sentences que no corresponde a sus propias partes
                        # (se corrigieron las respuestas sin recalcularla): queda la derivada.
                        counts["form.sentences del backup desactualizada (reemplazada)"] += 1
                    if q["form"].get("kind") == "CLOZE" and not q["form"].get("sentences"):
                        problems.append("%s: CLOZE sin form.sentences" % q["id"])

    live = [oid(x) for x in course["knowledgeIds"]]
    if sorted(seen_knowledges) != sorted(live):
        problems.append("el árbol tiene %d knowledges; el curso vivo, %d" % (len(seen_knowledges), len(live)))
    expected_quizzes = sum(len(qs) for _m, ts in milestones for _t, ks in ts for _k, qs in ks)
    if counts["quizzes"] != expected_quizzes:
        problems.append("el árbol tiene %d ejercicios; el curso vivo, %d" % (counts["quizzes"], expected_quizzes))

    for key in sorted(counts):
        print("  %-32s %d" % (key, counts[key]))
    if problems:
        print("\n%d problemas:" % len(problems))
        for p in problems[:50]:
            print("  - " + p)
        return 1
    print("\nOK: el árbol coincide documento por documento con el backup (salvo sentenceMode y form.sentences).")
    return 0


# ---------------------------------------------------------------------------

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("backup", help="directorio del backup (el que tiene courses.bson, knowledges.bson, quizTemplates.bson)")
    p.add_argument("--db", default=os.path.join(REPO, "db"), help="directorio db/ (default: el del repo)")
    p.add_argument("--dry-run", action="store_true", help="informa qué haría, sin escribir")
    p.add_argument("--verify", action="store_true", help="sólo compara db/ contra el backup")
    p.add_argument("--allow-dirty", action="store_true", help="escribe aunque db/ tenga cambios sin commitear")
    args = p.parse_args()

    backup = os.path.abspath(args.backup)
    db_dir = os.path.abspath(args.db)
    course_dir = os.path.join(db_dir, "english-course")
    if args.verify:
        sys.exit(verify(db_dir, backup))

    for tool in ("bsondump", "jq", "java"):
        if shutil.which(tool) is None:
            sys.exit("Falta %s en el PATH." % tool)
    if not args.dry_run and not args.allow_dirty and git_dirty(os.path.relpath(db_dir, REPO)):
        sys.exit("db/ tiene cambios sin commitear; commitealos o usá --allow-dirty.")

    print("Backup: %s" % backup)
    courses = read_bson(os.path.join(backup, "courses.bson"))
    knowledges = read_bson(os.path.join(backup, "knowledges.bson"))
    quizzes = read_bson(os.path.join(backup, "quizTemplates.bson"))
    print("  cursos %d · knowledges %d · quizTemplates %d" % (len(courses), len(knowledges), len(quizzes)))

    course, root, milestones = build_live_course(courses, knowledges, quizzes)
    live_k = [k for _m, ts in milestones for _t, ks in ts for k, _qs in ks]
    live_q = [q for _m, ts in milestones for _t, ks in ts for _k, qs in ks for q in qs]
    print("Curso vivo: %d milestones · %d topics · %d knowledges · %d ejercicios"
          % (len(milestones), sum(len(ts) for _m, ts in milestones), len(live_k), len(live_q)))
    print("  fuera del curso: %d knowledges y %d ejercicios (quedan en los exports planos)"
          % (sum(1 for k in knowledges if k.get("kind") == "KNOWLEDGE") - len(live_k), len(quizzes) - len(live_q)))

    prev = read_previous_tree(course_dir)
    missing_mode = [oid(k["_id"]) for k in live_k if oid(k["_id"]) not in prev["sentence_mode"]]
    if missing_mode:
        print("  AVISO: %d knowledges sin sentenceMode en el árbol anterior: %s" % (len(missing_mode), ", ".join(missing_mode)))

    sentences, stats, notes, derived_ids = assign_sentences(milestones, prev)
    print("form.sentences:")
    for key in sorted(stats):
        print("  %-42s %d" % (key, stats[key]))
    for note in notes:
        print("  AVISO: " + note)

    files = plan_tree(course, root, milestones, prev, sentences)
    if args.dry_run:
        print("Dry-run: escribiría %d archivos en %s" % (len(files), course_dir))
        return

    write_tree(course_dir, files)
    print("Árbol escrito: %d archivos en %s" % (len(files), course_dir))

    collections = []
    for bson_name, out_name in COLLECTIONS:
        bson_path = os.path.join(backup, bson_name + ".bson")
        json_path = os.path.join(db_dir, out_name + ".json")
        write_flat_export(bson_path, json_path)
        with open(json_path, encoding="utf-8") as f:
            count = len(json.load(f))
        collections.append(OrderedDict([
            ("name", out_name), ("bsonSource", bson_name + ".bson"), ("bsonSha256", sha256_of(bson_path)),
            ("documentCount", count), ("fileSizeBytes", os.path.getsize(json_path)), ("sha256", sha256_of(json_path)),
        ]))
        print("  %s.json: %d documentos" % (out_name, count))

    prelude_path = os.path.join(backup, "prelude.json")
    prelude = json.load(open(prelude_path, encoding="utf-8")) if os.path.exists(prelude_path) else None
    kinds = Counter(q["form"].get("kind") for q in live_q)
    metadata = OrderedDict([
        ("collections", collections),
        ("importedAt", datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")),
        ("dumpPath", backup),
        ("dumpPrelude", prelude),
        ("importer", "scripts/import_backup.py"),
        ("tree", OrderedDict([
            ("path", "english-course"),
            ("milestones", len(milestones)),
            ("topics", sum(len(ts) for _m, ts in milestones)),
            ("knowledges", len(live_k)),
            ("quizzes", len(live_q)),
            ("formKinds", OrderedDict(sorted(kinds.items()))),
            ("sentences", OrderedDict(sorted(stats.items()))),
            ("derivedSentenceQuizIds", derived_ids),
            ("outsideCourse", OrderedDict([
                ("knowledges", sum(1 for k in knowledges if k.get("kind") == "KNOWLEDGE") - len(live_k)),
                ("quizzes", len(quizzes) - len(live_q)),
            ])),
        ])),
    ])
    if os.path.exists(os.path.join(db_dir, "quizzes.json")):
        metadata["notRefreshed"] = [OrderedDict([
            ("file", "quizzes.json"),
            ("reason", "instancias de usuario (no contenido) del import anterior; el backup no trae quizzes.bson "
                       "y ningún código del repo lee este archivo"),
        ])]
    with open(os.path.join(db_dir, ".import-metadata.json"), "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("Metadata: %s" % os.path.join(db_dir, ".import-metadata.json"))


if __name__ == "__main__":
    main()
