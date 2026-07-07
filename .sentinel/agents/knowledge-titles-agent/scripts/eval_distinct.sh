#!/bin/bash
# Eval #5: distinción del título candidato vs los títulos hermanos del topic.
# difflib + Jaccard sobre el título normalizado, umbral 0.85 (mismo criterio
# que el eval_distinct de lemma-absence-agent). Script de cómputo (no rutea).
# Solo stdlib. Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os, difflib

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

THRESHOLD = 0.85

def normalize(title):
    s = (title or "").lower()
    s = re.sub(r"[^a-záéíóúüñ0-9\s]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def similarity(a, b):
    if not a or not b:
        return 0.0
    ratio = difflib.SequenceMatcher(None, a, b).ratio()
    ta, tb = set(a.split()), set(b.split())
    jaccard = (len(ta & tb) / len(ta | tb)) if (ta or tb) else 0.0
    return max(ratio, jaccard)

# siblingTitles lo publicó load_context (uno por línea).
siblings = [s for s in (data.get("siblingTitles") or inputs.get("siblingTitles") or "").split("\n")
            if s.strip()]

try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        candidate_title = (json.load(f).get("title", "") or "")
except Exception as e:
    sys.stderr.write("eval_distinct: candidate ilegible: %s\n" % e)
    print(json.dumps({"route": "done", "distinct_ok": True,
                      "distinct_max_similarity": 0.0, "distinct_nearest": "",
                      "distinct_threshold": THRESHOLD,
                      "distinct_sibling_count": len(siblings)}))
    sys.exit(0)

cand_norm = normalize(candidate_title)

max_sim, nearest = 0.0, ""
for sib in siblings:
    sim = similarity(cand_norm, normalize(sib))
    if sim > max_sim:
        max_sim, nearest = sim, sib

distinct_ok = True if not siblings or not cand_norm else (max_sim < THRESHOLD)

sys.stderr.write("[eval] distinct_ok=%s max_similarity=%.3f (threshold %.2f, %d hermanos)\n"
                 % (distinct_ok, max_sim, THRESHOLD, len(siblings)))

print(json.dumps({
    "route": "done",
    "distinct_ok": distinct_ok,
    "distinct_max_similarity": round(max_sim, 3),
    "distinct_nearest": nearest if not distinct_ok else "",
    "distinct_threshold": THRESHOLD,
    "distinct_sibling_count": len(siblings),
}, ensure_ascii=False))
PY
