#!/bin/bash
# Eval #3 (R007): no-regresión de longitud, determinista y bloqueante.
# Compara la longitud de la oración del candidato contra la oración original
# (baseline pre-corrección). Proxy = conteo de palabras de la oración, que
# refleja la métrica de sentence-length del audit. Script de cómputo: escribe
# `length_ok` al data bus; no rutea (route_verdict decide).
#
# stdin:  JSON { "inputs": {...}, "data": {...}, "node": {...}, "run_dir": ... }
# stdout: JSON UNA línea con campos que se mergean al data bus.
# Solo stdlib de Python (pitfall #2). Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}

# run_dir puede llegar por varias vías según el runtime; probamos en orden.
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

def words(s):
    return len([w for w in re.findall(r"[A-Za-z0-9']+", s or "")])

# La oración original (baseline) viene como input aplanado del CorrectionContext.
original = inputs.get("sentence") or inputs.get("quizSentence") or ""

# El candidato lo produjo `generate` como artifact en {run_dir}/candidate.json.
candidate_sentence = ""
err = None
try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
    candidate_sentence = cand.get("quizSentence", "") or ""
except Exception as e:  # candidato ausente/ilegible -> length_ok=False (bloquea)
    err = str(e)

orig_w = words(original)
cand_w = words(candidate_sentence)
length_ok = (err is None) and (cand_w >= orig_w)

out = {
    "route": "done",
    "length_ok": length_ok,
    "length_original_words": orig_w,
    "length_candidate_words": cand_w,
}
if err:
    out["length_error"] = err
    print(f"eval_length: candidate unreadable: {err}", file=sys.stderr)

print(json.dumps(out))
PY
