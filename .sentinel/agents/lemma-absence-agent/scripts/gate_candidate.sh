#!/bin/bash
# Normaliza + valida la FORMA y el FORMATO DSL del candidato del react (antes de
# evaluarlo). Replica la gramática estricta de QuizSentenceParser (course-domain)
# para que un candidato malformado se REINTENTE con feedback, en vez de pasar los
# evals LLM y morir después en el gate estructural de FEAT-LAPS.
# - strippea ```fences``` y extrae el primer objeto JSON balanceado.
# - exige {quizSentence, translation} no vacíos, sin `[[...]]`, sin schemas ajenos.
# - QSENT: todo `[`/`]` debe ser parte de un bloque `____\s*[respuesta]`; ningún
#   `____` suelto; ningún `[` fuera de bloque (p.ej. hint entre `____` y `[`).
# - Si pasa: reescribe candidate.json canónico → route passed.
# - Si no: escribe carry_forward.md con el motivo → route failed (→ load_context).
# Solo stdlib. UNA línea JSON a stdout; debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

cand_path = os.path.join(run_dir, "candidate.json")

def write_carry(reason, prev_qs=None):
    """Deja feedback para el reintento (load_context lo cargará a {carryForward})."""
    lines = ["# Carry-forward — feedback del intento anterior", "",
             "- ❌ formato DSL inválido: " + reason]
    if prev_qs:
        lines.append("")
        lines.append("Candidato previo (no repitas el mismo error): " + prev_qs)
    try:
        with open(os.path.join(run_dir, "carry_forward.md"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    except Exception:
        pass

def fail(reason, prev_qs=None):
    sys.stderr.write("[gate_candidate] FAILED: " + reason + "\n")
    write_carry(reason, prev_qs)
    print(json.dumps({"route": "failed"}))
    sys.exit(0)

try:
    with open(cand_path, "r", encoding="utf-8") as f:
        raw = f.read()
except Exception as e:
    fail("candidate.json ilegible: " + str(e))

text = raw.strip()
if text.startswith("```"):
    text = re.sub(r"^```[a-zA-Z0-9]*\s*", "", text)
    text = re.sub(r"\s*```$", "", text).strip()

# Extraer el primer objeto {...} balanceado (tolera prosa alrededor).
obj = None
start = text.find("{")
if start != -1:
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                try:
                    obj = json.loads(text[start:i+1])
                except Exception:
                    obj = None
                break

if not isinstance(obj, dict):
    fail("no se encontró un objeto JSON en el candidato")

qs = obj.get("quizSentence")
tr = obj.get("translation")
if not isinstance(qs, str) or not qs.strip():
    fail("falta quizSentence o está vacío")
if not isinstance(tr, str) or not tr.strip():
    fail("falta translation o está vacío", prev_qs=qs)
if "[[" in qs:
    fail("quizSentence usa corchetes dobles `[[...]]` (prohibido; usá un corchete simple `[respuesta]`)", prev_qs=qs)
foreign = {"exercise", "gaps", "answerKey", "passage", "options"}
if foreign.intersection(obj.keys()):
    fail("el JSON trae un schema ajeno (" + ",".join(sorted(foreign.intersection(obj.keys()))) + "); usá solo {quizSentence, translation}", prev_qs=qs)

# --- Validación QSENT (replica QuizSentenceParser de course-domain) ---
# 1) runs de guiones bajos: solo se permite exactamente '____' (4).
for m in re.finditer(r"_{3,}", qs):
    if (m.end() - m.start()) != 4:
        fail("un blank se escribe con EXACTAMENTE cuatro guiones bajos '____' (encontré %d)" % (m.end()-m.start()), prev_qs=qs)
# 2) sacar los bloques válidos `____\s*[...]`; lo que queda no puede tener [ ] ni ____.
BLOCK = re.compile(r"____\s*\[[^\]]*?\]")
if not BLOCK.search(qs):
    fail("falta un hueco válido `____ [respuesta]` (el `____` debe ir inmediatamente seguido de `[respuesta]`)", prev_qs=qs)
residual = BLOCK.sub("", qs)
if "[" in residual or "]" in residual:
    fail("hay `[` o `]` fuera de un bloque `____ [respuesta]` — el hint `(...)` va DESPUÉS del `]`, NUNCA entre `____` y `[`", prev_qs=qs)
if "____" in residual:
    fail("hay un `____` sin su `[respuesta]` inmediatamente después (entre `____` y `[` solo puede haber espacios)", prev_qs=qs)

# OK → canonicalizar candidate.json con exactamente las dos claves.
with open(cand_path, "w", encoding="utf-8") as f:
    json.dump({"quizSentence": qs, "translation": tr}, f, ensure_ascii=False)

print(json.dumps({"route": "passed"}))
PY
