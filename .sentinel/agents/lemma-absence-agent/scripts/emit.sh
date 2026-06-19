#!/bin/bash
# Salida del run (R003/R009): escribe el artifact `quizCandidate` con el
# CHAMPION (mejor candidato visto). Fallback al último candidato si por algún
# motivo no hubo snapshot de champion. Solo stdlib (pitfall #2).
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
    # Nada emitible: el adaptador Java lo tratará como run sin artifact ->
    # ProposalStrategyFailedException (gate estructural de FEAT-LAPS).
    print(json.dumps({"route": "done", "emitted": False}), file=sys.stdout)
    sys.stderr.write("emit: no emittable candidate (champion+candidate missing/empty)\n")
    sys.exit(0)

# Exactamente un QuizCandidate {quizSentence, translation} (R003).
out = {"quizSentence": chosen["quizSentence"], "translation": chosen["translation"]}
out_path = os.path.join(run_dir, "quizCandidate.json")
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False)

# Señalamos el artifact por si el runtime lo registra desde el stdout además
# de via completion.produces.
print(json.dumps({"route": "done", "emitted": True,
                  "artifact_quizCandidate": out_path}))
PY
