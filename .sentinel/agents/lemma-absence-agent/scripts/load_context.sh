#!/bin/bash
# Carga el carry-forward (R010) al data bus: lee {run_dir}/carry_forward.md
# (feedback del intento previo, escrito por tally) y lo publica como
# `carryForward` (string). Primer intento: archivo ausente -> "". Solo stdlib.
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

carry = ""
try:
    with open(os.path.join(run_dir, "carry_forward.md"), "r", encoding="utf-8") as f:
        carry = f.read().strip()
except Exception:
    carry = ""

# Invalidar veredictos del intento anterior: cada vuelta de generate produce un
# candidato NUEVO, y un verdicts.json remanente permitiría que (a) tally combine
# el candidato nuevo con veredictos viejos si eval_quality se agota, y (b) la
# completion `produces` de eval_quality pase con un archivo stale aunque el juez
# no haya emitido nada. Primer intento: archivo ausente, no-op.
try:
    os.remove(os.path.join(run_dir, "verdicts.json"))
except FileNotFoundError:
    pass
except Exception as e:
    sys.stderr.write("load_context: no pude borrar verdicts.json stale: %s\n" % e)

# curation_cached: ¿ya corrió la curación de vocabulario en este run? finalize_curation
# escribe curated_lemmas.json con el pool resultante. Si existe y tiene palabras, los
# reintentos saltean la curación (route_curate) y reusan el pool. Primer intento:
# ausente -> False -> se cura. (Es el artefacto curatedLemmas; la spec del LLM vive
# en curation_spec.json.)
curation_cached = False
try:
    with open(os.path.join(run_dir, "curated_lemmas.json"), "r", encoding="utf-8") as f:
        curation_cached = bool((json.load(f) or {}).get("words"))
except Exception:
    curation_cached = False

print(json.dumps({"route": "done", "carryForward": carry,
                  "curation_cached": curation_cached}, ensure_ascii=False))
PY
