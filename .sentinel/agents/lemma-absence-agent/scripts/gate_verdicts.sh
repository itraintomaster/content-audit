#!/bin/bash
# Valida la FORMA de {run_dir}/verdicts.json (salida del juez eval_quality) antes
# de que tally lo combine. Parse tolerante (mismo criterio que tally + reparación
# de llaves faltantes) y validación de las 5 claves del catálogo (R007). Si pasa,
# reescribe verdicts.json en forma canónica; si no, route failed → eval_quality
# re-emite. Motivación: el juez a veces pierde una `}` (p.ej. run
# 20260706T114517-7b05fcca: 6 `{` vs 5 `}`) y tally leía {} → todos los evals
# LLM "reprobados" → carry-forward falso y reintento espurio de generate.
# Solo stdlib (pitfall #2). UNA línea JSON a stdout; debug a stderr.
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

path = os.path.join(run_dir, "verdicts.json")

def fail(reason):
    sys.stderr.write("[gate_verdicts] FAILED: " + reason + "\n")
    print(json.dumps({"route": "failed"}))
    sys.exit(0)

try:
    with open(path, "r", encoding="utf-8") as f:
        raw = f.read()
except Exception as e:
    fail("verdicts.json ilegible: %s" % e)

def parse_lenient(raw):
    """JSON directo → sin ```fences``` → primer {...} balanceado → reparación de
    llaves de cierre faltantes (el modo de fallo real del juez)."""
    text = (raw or "").strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        pass
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z0-9]*\s*", "", text)
        text = re.sub(r"\s*```$", "", text).strip()
        try:
            return json.loads(text)
        except Exception:
            pass
    start = text.find("{")
    if start == -1:
        return None
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                try:
                    return json.loads(text[start:i + 1])
                except Exception:
                    return None
    # depth > 0: el objeto nunca cerró (llaves faltantes al final) → reparar.
    if depth > 0:
        candidate = text[start:].rstrip().rstrip(",")
        try:
            return json.loads(candidate + "}" * depth)
        except Exception:
            return None
    return None

verdicts = parse_lenient(raw)
if not isinstance(verdicts, dict):
    fail("verdicts.json no es un objeto JSON parseable (¿JSON truncado/malformado?)")

REQUIRED = ("eval1_sense", "eval2_solvable", "eval4_pragmatism",
            "eval5_translation", "eval6_finite_reuse")
missing = [k for k in REQUIRED if not isinstance(verdicts.get(k), dict)]
if missing:
    fail("faltan claves top-level (¿objetos anidados por una `}` perdida?): "
         + ", ".join(missing))

for k in ("eval1_sense", "eval2_solvable", "eval5_translation", "eval6_finite_reuse"):
    if not isinstance(verdicts[k].get("pass"), bool):
        fail("'%s.pass' debe ser booleano" % k)
score = verdicts["eval4_pragmatism"].get("score")
if not isinstance(score, (int, float)) or isinstance(score, bool):
    fail("'eval4_pragmatism.score' debe ser numérico (0.0-1.0)")

# OK → canonicalizar: SOLO las claves del catálogo, JSON limpio para tally.
canonical = {k: verdicts[k] for k in REQUIRED}
with open(path, "w", encoding="utf-8") as f:
    json.dump(canonical, f, ensure_ascii=False)

print(json.dumps({"route": "passed"}))
PY
