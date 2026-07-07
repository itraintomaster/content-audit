#!/bin/bash
# Evals #1/#2: longitud ponderada del título (≤28) y de las instructions (≤70).
# MISMA función de pesos que los analyzers del audit (fuente:
# KnowledgeTitleLengthAnalyzer / KnowledgeInstructionsLengthAnalyzer).
# Script de cómputo: escribe al data bus, NO rutea. Solo stdlib. Debug a stderr.
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

TITLE_MAX = 28.0         # KnowledgeTitleLengthAnalyzer.MAX_WEIGHTED_LENGTH
INSTRUCTIONS_MAX = 70.0  # KnowledgeInstructionsLengthAnalyzer.SOFT_LIMIT

def char_weight(c):
    if c in "$*":
        return 0.0
    if c in "i,.":
        return 0.5
    if c in 'ft"':
        return 0.7
    return 1.0

def weighted_length(text):
    return sum(char_weight(c) for c in text)

try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
except Exception as e:
    # gate_candidate garantiza el archivo; si falta igual, bloqueamos.
    sys.stderr.write("eval_length: candidate ilegible: %s\n" % e)
    print(json.dumps({"route": "done", "title_length_ok": False,
                      "instructions_length_ok": False,
                      "title_weighted_length": -1.0,
                      "instructions_weighted_length": -1.0,
                      "title_max": TITLE_MAX, "instructions_max": INSTRUCTIONS_MAX}))
    sys.exit(0)

title_wl = round(weighted_length(cand.get("title") or ""), 1)
instr_wl = round(weighted_length(cand.get("instructions") or ""), 1)
title_ok = title_wl <= TITLE_MAX
instr_ok = instr_wl <= INSTRUCTIONS_MAX

sys.stderr.write("[eval] title_length_ok=%s (%.1f/%.0f) instructions_length_ok=%s (%.1f/%.0f)\n"
                 % (title_ok, title_wl, TITLE_MAX, instr_ok, instr_wl, INSTRUCTIONS_MAX))

print(json.dumps({
    "route": "done",
    "title_length_ok": title_ok,
    "instructions_length_ok": instr_ok,
    "title_weighted_length": title_wl,
    "instructions_weighted_length": instr_wl,
    "title_max": TITLE_MAX,
    "instructions_max": INSTRUCTIONS_MAX,
}))
PY
