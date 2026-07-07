#!/bin/bash
# Valida la FORMA de {run_dir}/verdicts.json (salida del juez eval_quality).
# Parse tolerante (fences / prosa / llaves de cierre perdidas) + validación de
# las 4 claves del catálogo. Si pasa, canonicaliza; si no, route failed.
# Solo stdlib. UNA línea JSON a stdout; debug a stderr.
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
    """JSON directo → sin ```fences``` → primer {...} balanceado → reparación
    de llaves de cierre faltantes (modo de fallo real del juez)."""
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

REQUIRED = ("eval6_fidelity", "eval7_language", "eval8_no_redundancy", "eval9_style")
missing = [k for k in REQUIRED if not isinstance(verdicts.get(k), dict)]
if missing:
    fail("faltan claves top-level (¿objetos anidados por una `}` perdida?): "
         + ", ".join(missing))

for k in ("eval6_fidelity", "eval7_language", "eval8_no_redundancy"):
    if not isinstance(verdicts[k].get("pass"), bool):
        fail("'%s.pass' debe ser booleano" % k)
score = verdicts["eval9_style"].get("score")
if not isinstance(score, (int, float)) or isinstance(score, bool):
    fail("'eval9_style.score' debe ser numérico (0.0-1.0)")

# OK → canonicalizar: SOLO las claves del catálogo, JSON limpio para tally.
canonical = {k: verdicts[k] for k in REQUIRED}
with open(path, "w", encoding="utf-8") as f:
    json.dump(canonical, f, ensure_ascii=False)

print(json.dumps({"route": "passed"}))
PY
