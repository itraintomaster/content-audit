#!/bin/bash
# F-QICOR-R013: mantiene el champion entre intentos. El ORDEN no vive acá: viene
# resuelto en rankKey desde Java (CandidateRanking). Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, os, sys

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data = d.get("data", {}) or {}
node = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")


def read(name, default=None):
    try:
        with open(os.path.join(run_dir, name), "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return default


candidate = read("candidate.json")
assessment = read("assessment.json", {}) or {}
rank_key = assessment.get("rankKey") or ""
eligible = bool(assessment.get("eligible"))
unmet = assessment.get("unmetCriteria") or []
all_met = eligible and not unmet

attempt = inputs.get("attempt") or data.get("attempt") or 0
try:
    attempt = int(attempt)
except Exception:
    attempt = 0

# Registro de intentos: lo consume emit para declarar cuántos hubo (R012).
with open(os.path.join(run_dir, "attempts.jsonl"), "a", encoding="utf-8") as f:
    f.write(json.dumps({"attempt": attempt, "rankKey": rank_key,
                        "eligible": eligible, "unmet": len(unmet)},
                       ensure_ascii=False) + "\n")

# --- Champion (R013).
# Un candidato inelegible NUNCA compite: no cumple su consigna o no es un cambio
# real, y entregarlo seria proponer algo que reabre el mismo diagnostico.
meta = read("champion_meta.json")
if candidate is not None and eligible:
    # Estrictamente mayor: ante clave igual se conserva el que ya estaba, que es
    # como se implementa "gana el primero visto" (R013). Si empatar destronara al
    # campeon, el resultado dependeria de cuantos intentos se gastaron.
    if meta is None or rank_key > (meta.get("rankKey") or ""):
        with open(os.path.join(run_dir, "champion.json"), "w", encoding="utf-8") as f:
            json.dump(candidate, f, ensure_ascii=False)
        with open(os.path.join(run_dir, "champion_assessment.json"), "w", encoding="utf-8") as f:
            json.dump(assessment, f, ensure_ascii=False)
        with open(os.path.join(run_dir, "champion_meta.json"), "w", encoding="utf-8") as f:
            json.dump({"rankKey": rank_key, "attempt": attempt,
                       "allCriteriaMet": all_met}, f, ensure_ascii=False)

print(json.dumps({"route": "done", "all_criteria_met": all_met}, ensure_ascii=False))
PY
