#!/bin/bash
# Eval #3: redundancia léxica del título vs topic y vs términos citados de las
# instructions del candidato. Script de cómputo (no rutea). Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os

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

# topicLabel lo publicó load_context al data bus.
topic_label = (data.get("topicLabel") or inputs.get("topicLabel") or "")

# Stopwords inglesas: palabras estructurales que no cuentan como redundancia
# (un "and" o "the" compartido no repite información).
STOP = {"a", "an", "the", "and", "or", "of", "in", "on", "at", "to", "for",
        "with", "vs", "v", "&"}

def tokens(text):
    """Minúsculas, sin puntuación ni comillas; solo palabras."""
    return [t for t in re.findall(r"[a-záéíóúüñ']+", (text or "").lower())
            if t.strip("'")]

def content_words(text):
    return {t.strip("'") for t in tokens(text)} - STOP - {""}

def quoted_terms(text):
    """Términos destacados: comillas simples ('be', 'short forms'), glosas
    entre paréntesis («sin contracciones (short forms)») y palabras clave
    entre $...$ ($be$ — negrita naranja en la UI) — las tres convenciones
    del curso para el término inglés dentro de instructions en español."""
    words = set()
    for m in re.findall(r"'([^']+)'", text or ""):
        words.update(content_words(m))
    for m in re.findall(r"\(([^)]+)\)", text or ""):
        words.update(content_words(m))
    for m in re.findall(r"\$([^$]+)\$", text or ""):
        words.update(content_words(m))
    return words

try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
except Exception as e:
    sys.stderr.write("eval_redundancy: candidate ilegible: %s\n" % e)
    # eval_length ya bloquea el candidato ausente; acá no duplicamos el bloqueo.
    print(json.dumps({"route": "done", "redundancy_ok": True,
                      "redundant_with_topic": "", "redundant_with_instructions": ""}))
    sys.exit(0)

title_words = content_words(cand.get("title") or "")
topic_words = content_words(topic_label)
instr_quoted = quoted_terms(cand.get("instructions") or "")

vs_topic = sorted(title_words & topic_words)
vs_instr = sorted(title_words & instr_quoted)
redundancy_ok = not vs_topic and not vs_instr

sys.stderr.write("[eval] redundancy_ok=%s topic_overlap=%s quoted_overlap=%s\n"
                 % (redundancy_ok, vs_topic, vs_instr))

print(json.dumps({
    "route": "done",
    "redundancy_ok": redundancy_ok,
    "redundant_with_topic": ", ".join(vs_topic),
    "redundant_with_instructions": ", ".join(vs_instr),
}, ensure_ascii=False))
PY
