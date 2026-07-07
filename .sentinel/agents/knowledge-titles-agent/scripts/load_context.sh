#!/bin/bash
# Carga el contexto del knowledge (título/instructions actuales, topic, nivel,
# hermanos, quizzes de ejemplo) + carry-forward al data bus. Solo stdlib.
# stdin: JSON {inputs, data, node, run_dir}; stdout: UNA línea JSON (data bus).
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}

def _resolve_run_dir():
    for c in (d.get("run_dir"), inputs.get("run_dir"), data.get("run_dir"),
              node.get("run_dir"), os.environ.get("SENTINEL_RUN_DIR")):
        if c:
            return c
    # Runs lanzados por el server NO inyectan run_dir en inputs (el adaptador
    # Java sí). Derivación por el layout estándar {root}/.sentinel/runs/{agent}/:
    # exacta con el run_id de la proyección; si el framework desplegado aún no
    # proyecta run_id, fallback al run MÁS RECIENTE del agente (sus runs no
    # corren en paralelo; el framework crea el run dir antes de cualquier nodo).
    root = inputs.get("project_root") or inputs.get("root") or ""
    if root:
        base = os.path.join(root, ".sentinel", "runs", "knowledge-titles-agent")
        run_id = d.get("run_id") or ""
        if run_id and os.path.isdir(os.path.join(base, run_id)):
            return os.path.join(base, run_id)
        try:
            dirs = [p for p in (os.path.join(base, n) for n in os.listdir(base))
                    if os.path.isdir(p)]
            if dirs:
                newest = max(dirs, key=os.path.getmtime)
                sys.stderr.write("run_dir por fallback mtime (sin run_id): %s\n" % newest)
                return newest
        except Exception:
            pass
    return "."

run_dir = _resolve_run_dir()

# Dependencias: primero inputs, después data (pitfall #1 del skill).
knowledge_id = (inputs.get("knowledgeId") or data.get("knowledgeId") or "").strip()
project_root = (inputs.get("project_root") or data.get("project_root") or "").strip()
if not knowledge_id:
    sys.stderr.write("load_context: falta input knowledgeId\n")
    sys.exit(1)
if not project_root:
    sys.stderr.write("load_context: falta input project_root\n")
    sys.exit(1)

def load(name):
    with open(os.path.join(project_root, "db", name), "r", encoding="utf-8") as f:
        return json.load(f)

docs = load("knowledges.json")
by_id = {(doc.get("_id") or {}).get("$oid"): doc for doc in docs}

knowledge = by_id.get(knowledge_id)
if not knowledge or knowledge.get("kind") != "KNOWLEDGE":
    sys.stderr.write("load_context: knowledge %s no existe o no es KNOWLEDGE\n" % knowledge_id)
    sys.exit(1)

# Cadena de ancestros: KNOWLEDGE → TOPIC → MILESTONE (nivel CEFR).
topic_label, level_label = "", ""
cur = knowledge
while cur is not None:
    kind = cur.get("kind")
    if kind == "TOPIC" and not topic_label:
        topic_label = cur.get("label") or ""
    if kind == "MILESTONE" and not level_label:
        level_label = cur.get("label") or ""
    cur = by_id.get((cur.get("parentId") or {}).get("$oid"))

# Hermanos: knowledges del mismo topic (mismo parentId), excluyendo este.
parent_oid = (knowledge.get("parentId") or {}).get("$oid")
siblings = [doc.get("label") or ""
            for doc in docs
            if doc.get("kind") == "KNOWLEDGE"
            and (doc.get("parentId") or {}).get("$oid") == parent_oid
            and (doc.get("_id") or {}).get("$oid") != knowledge_id]
siblings = [s for s in siblings if s.strip()]

# Quizzes de ejemplo: hasta 5 oraciones renderizadas de quiz-templates.
samples = []
try:
    for t in load("quiz-templates.json"):
        if (t.get("knowledgeId") or {}).get("$oid") != knowledge_id:
            continue
        parts = (t.get("form") or {}).get("sentenceParts") or []
        words = [(p.get("text") or "") if p.get("kind") == "TEXT" else "____"
                 for p in parts]
        sentence = " ".join(w for w in words if w).strip()
        if sentence and sentence not in samples:
            samples.append(sentence)
        if len(samples) >= 5:
            break
except Exception as e:
    sys.stderr.write("load_context: no pude leer quiz-templates.json: %s\n" % e)

# Carry-forward del intento previo (tally o gate_candidate lo escriben).
carry = ""
try:
    with open(os.path.join(run_dir, "carry_forward.md"), "r", encoding="utf-8") as f:
        carry = f.read().strip()
except Exception:
    carry = ""

# Invalidar veredictos del intento anterior (mismo criterio que lemma-absence):
# un verdicts.json remanente dejaría que tally combine el candidato NUEVO con
# veredictos VIEJOS si el juez se agota en esta vuelta.
try:
    os.remove(os.path.join(run_dir, "verdicts.json"))
except FileNotFoundError:
    pass
except Exception as e:
    sys.stderr.write("load_context: no pude borrar verdicts.json stale: %s\n" % e)

sys.stderr.write("load_context: %s | topic=%r nivel=%r hermanos=%d quizzes=%d\n"
                 % (knowledge_id, topic_label, level_label, len(siblings), len(samples)))

print(json.dumps({
    "route": "done",
    "originalTitle": knowledge.get("label") or "",
    "originalInstructions": knowledge.get("instructions") or "",
    "topicLabel": topic_label,
    "levelLabel": level_label,
    "siblingTitles": "\n".join(siblings),
    "sampleQuizzes": "\n".join(samples),
    "carryForward": carry,
}, ensure_ascii=False))
PY
