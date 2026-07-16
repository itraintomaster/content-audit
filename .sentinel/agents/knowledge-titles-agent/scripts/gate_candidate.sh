#!/bin/bash
# Normaliza + valida la FORMA del candidato {title, instructions} del react.
# Parse tolerante (fences/prosa), claves exactas, sin caracteres de formato en
# el título. Si pasa reescribe candidate.json canónico; si no, deja feedback en
# carry_forward.md y rutea failed. Solo stdlib. Debug a stderr.
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

cand_path = os.path.join(run_dir, "candidate.json")

def write_carry(reason, prev=None):
    lines = ["# Carry-forward — feedback del intento anterior", "",
             "- ❌ formato inválido: " + reason]
    if prev:
        lines.append("")
        lines.append("Candidato previo (no repitas el mismo error): " + prev)
    try:
        with open(os.path.join(run_dir, "carry_forward.md"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
    except Exception:
        pass

def fail(reason, prev=None):
    sys.stderr.write("[gate_candidate] FAILED: " + reason + "\n")
    write_carry(reason, prev)
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

title = obj.get("title")
instructions = obj.get("instructions")
if not isinstance(title, str) or not title.strip():
    fail("falta `title` o está vacío")
if not isinstance(instructions, str) or not instructions.strip():
    fail("falta `instructions` o está vacío", prev=title)

title = title.strip()
instructions = instructions.strip()

for ch, name in (("*", "'*'"), ('"', "comillas dobles")):
    if ch in title:
        fail("el título no puede contener %s (caracteres de formato de la UI); "
             "citá términos ingleses con $...$" % name, prev=title)

if "\n" in title:
    fail("el título debe ser UNA sola línea", prev=title)

# Título: $...$ marca términos ingleses citados (negrita naranja en la UI).
# Misma regla que instructions: pares balanceados y sin $$ vacíos.
if title.count("$") % 2 != 0:
    fail("el título tiene un `$` sin cerrar — los términos citados van "
         "en pares $palabra$", prev=title)
if any(not seg.strip() for seg in title.split("$")[1::2]):
    fail("el título tiene un par `$...$` vacío — cada par debe envolver "
         "un término", prev=title)

# Instructions: $...$ marca palabras clave (negrita naranja en la UI).
# Deben venir en pares balanceados y sin $$ vacíos. Con conteo par, los
# segmentos impares del split son el interior de cada par.
if instructions.count("$") % 2 != 0:
    fail("las instructions tienen un `$` sin cerrar — las palabras clave van "
         "en pares $palabra$", prev=title + " / " + instructions)
if any(not seg.strip() for seg in instructions.split("$")[1::2]):
    fail("las instructions tienen un par `$...$` vacío — cada par debe "
         "envolver una palabra clave", prev=title + " / " + instructions)

# OK → canonicalizar candidate.json con exactamente las dos claves.
with open(cand_path, "w", encoding="utf-8") as f:
    json.dump({"title": title, "instructions": instructions}, f, ensure_ascii=False)

print(json.dumps({"route": "passed"}))
PY
