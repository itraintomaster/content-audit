#!/bin/bash
# Eval #4: guardia determinista de idioma (título EN, instructions ES).
# Conservadora a propósito: solo marca casos inequívocos; la naturalidad la
# juzga el LLM (#7). Script de cómputo (no rutea). Solo stdlib.
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

# Palabras castellanas inequívocas (no existen en inglés) — señales de que un
# texto está en español.
SPANISH_ONLY = {
    "el", "los", "las", "una", "uno", "del", "al", "con", "para", "por",
    "escribe", "escribí", "elige", "elegí", "completa", "usa", "utiliza",
    "oración", "oraciones", "respuesta", "respuestas", "verbo", "verbos",
    "forma", "formas", "presente", "pasado", "futuro", "corta", "cortas",
    "afirmativa", "negativa", "pregunta", "preguntas", "según", "cuando",
}

# Palabras inglesas inequívocas de títulos de ejercicios (no existen en
# español): si aparecen en el TÍTULO fuera de comillas, el título quedó en
# inglés — debe ir en español. OJO: "simple" existe en ambos idiomas y NO va acá.
ENGLISH_ONLY_TITLE = {
    "the", "with", "and", "of", "sentences", "sentence", "statements",
    "statement", "questions", "question", "answers", "answer", "practice",
    "practicing", "review", "reviewing", "mixed", "negative", "affirmative",
    "short", "forms", "form", "verbs", "verb", "regular", "irregular",
    "past", "present", "continuous", "future", "writing", "write", "using",
    "round", "another",
}

# Señales de español para las instructions (funcionales frecuentes; se exige
# ≥2 para tolerar ambigüedades tipo "no"/"si" que también existen en inglés).
SPANISH_SIGNALS = SPANISH_ONLY | {
    "la", "un", "en", "de", "que", "y", "o", "si", "no", "sea", "posible",
    "correcta", "correcto", "cada", "tu", "su", "sus", "es", "sin",
}

# Palabras inglesas frecuentes de consigna: si aparecen SIN citar en las
# instructions, la consigna está (parcialmente) en inglés.
ENGLISH_SIGNALS = {
    "write", "complete", "choose", "use", "using", "answer", "answers",
    "sentence", "sentences", "the", "when", "possible", "short", "forms",
    "correct", "with", "form", "verb", "verbs", "and", "your",
}

ACCENTED = re.compile(r"[áéíóúüñ¿¡]", re.IGNORECASE)

def strip_quoted(text):
    """Saca los términos destacados: comillas simples ('be'), glosas entre
    paréntesis («sin contracciones (short forms)») y palabras clave entre
    $...$ ($be$ — negrita naranja en la UI) — las tres convenciones del
    curso para el término inglés dentro de texto en español."""
    text = re.sub(r"'[^']*'", " ", text or "")
    text = re.sub(r"\$[^$]*\$", " ", text)
    return re.sub(r"\([^)]*\)", " ", text)

def words(text):
    return [w for w in re.findall(r"[a-záéíóúüñ]+", (text or "").lower()) if w]

try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
except Exception as e:
    sys.stderr.write("eval_language: candidate ilegible: %s\n" % e)
    print(json.dumps({"route": "done", "title_lang_ok": True,
                      "instructions_lang_ok": True, "lang_detail": ""}))
    sys.exit(0)

title_bare = strip_quoted(cand.get("title") or "")
instr_bare = strip_quoted(cand.get("instructions") or "")

detail = []

# --- Título: español (el inglés va solo citado entre comillas) ---
title_english_hits = [w for w in words(title_bare) if w in ENGLISH_ONLY_TITLE]
title_lang_ok = not title_english_hits
if not title_lang_ok:
    detail.append("título con inglés fuera de comillas (%s) — el título va en ESPAÑOL"
                  % ", ".join(sorted(set(title_english_hits))))

# --- Instructions: español ---
instr_words = words(instr_bare)
spanish_hits = [w for w in instr_words if w in SPANISH_SIGNALS]
english_hits = [w for w in instr_words if w in ENGLISH_SIGNALS and w not in SPANISH_SIGNALS]
has_spanish = bool(ACCENTED.search(instr_bare)) or len(set(spanish_hits)) >= 2
instructions_lang_ok = has_spanish and len(set(english_hits)) <= 1
if not instructions_lang_ok:
    if not has_spanish:
        detail.append("instructions sin señales de español")
    if len(set(english_hits)) > 1:
        detail.append("instructions con inglés sin citar (%s)"
                      % ", ".join(sorted(set(english_hits))))

sys.stderr.write("[eval] title_lang_ok=%s instructions_lang_ok=%s%s\n"
                 % (title_lang_ok, instructions_lang_ok,
                    (" — " + "; ".join(detail)) if detail else ""))

print(json.dumps({
    "route": "done",
    "title_lang_ok": title_lang_ok,
    "instructions_lang_ok": instructions_lang_ok,
    "lang_detail": "; ".join(detail),
}, ensure_ascii=False))
PY
