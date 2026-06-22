#!/bin/bash
# Salida del run (R003/R009): emite el artifact `quizCandidate` con el CHAMPION
# (mejor candidato visto). El framework registra el artifact de un nodo `script`
# SOLO si su stdout JSON trae un campo con la clave == kind del artifact
# (`quizCandidate`), cuyo valor es el CONTENIDO; el framework lo escribe al
# pointer ({run_dir}/quizCandidate.json) y lo registra en state.artifacts(), de
# donde lo lee el adaptador Java (AgentCandidateParser). Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

def read_json(name):
    try:
        with open(os.path.join(run_dir, name), "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None

# Champion = best-effort (R009); fallback al candidato del último intento.
chosen = read_json("champion.json") or read_json("candidate.json")

if not chosen or not chosen.get("quizSentence") or not chosen.get("translation"):
    # Nada emitible: NO emitimos el campo quizCandidate -> el artifact requerido no
    # se produce -> el goal falla -> el adaptador lanza ProposalStrategyFailedException
    # (gate estructural de FEAT-LAPS). Correcto: no hay candidato que emitir.
    print(json.dumps({"route": "done"}))
    sys.stderr.write("emit: no emittable candidate (champion+candidate missing/empty)\n")
    sys.exit(0)

# El CONTENIDO del artifact: exactamente un QuizCandidate {quizSentence, translation} (R003),
# como string JSON bajo la clave 'quizCandidate' (== kind del completion.produces del nodo).
content = json.dumps(
    {"quizSentence": chosen["quizSentence"], "translation": chosen["translation"]},
    ensure_ascii=False,
)
print(json.dumps({"route": "done", "quizCandidate": content}, ensure_ascii=False))
PY
