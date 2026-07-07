#!/bin/bash
# Emite el artifact `titleCandidate` con el CHAMPION (mejor candidato visto).
# El framework registra el artifact de un nodo `script` SOLO si su stdout JSON
# trae un campo con la clave == kind del artifact (`titleCandidate`), cuyo
# valor es el CONTENIDO. Fallback al candidato del último intento. Solo stdlib.
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

def read_json(name):
    try:
        with open(os.path.join(run_dir, name), "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None

# Champion = best-effort; fallback al candidato del último intento.
chosen = read_json("champion.json") or read_json("candidate.json")

if not chosen or not chosen.get("title") or not chosen.get("instructions"):
    # Nada emitible: NO emitimos el campo titleCandidate → el artifact
    # requerido no se produce → el goal falla honestamente.
    print(json.dumps({"route": "done"}))
    sys.stderr.write("emit: sin candidato emitible (champion+candidate ausentes/vacíos)\n")
    sys.exit(0)

knowledge_id = (inputs.get("knowledgeId") or data.get("knowledgeId") or "")
content = json.dumps(
    {"knowledgeId": knowledge_id,
     "title": chosen["title"],
     "instructions": chosen["instructions"]},
    ensure_ascii=False,
)
print(json.dumps({"route": "done", "titleCandidate": content}, ensure_ascii=False))
PY
