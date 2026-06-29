#!/bin/bash
# Eval #7 (R013): distinción respecto a los quizzes ya existentes del ejercicio.
# Determinista y bloqueante: el candidato NO debe ser demasiado parecido a algún
# quiz hermano del mismo ejercicio (los otros quizzes bajo el mismo knowledge).
# Evita sugerir un quiz que, para el alumno, sea prácticamente el mismo que uno
# que ya existe (misma oración con solo otra palabra en el hueco).
#
# Estrategia: normaliza ambos quizSentence al "esqueleto" de la oración
# (minúsculas, se quita la respuesta `[...]` y la pista `(...)`, el hueco `____`
# se colapsa a un placeholder, se saca puntuación) y compara con similitud de
# secuencia (difflib) + Jaccard de tokens; toma el máximo. Si el más parecido de
# los hermanos supera el umbral, distinct_ok=False.
#
# Script de cómputo (como eval_length): escribe al data bus, NO rutea
# (route_verdict decide tras combinar en tally).
#
# stdin:  JSON { "inputs": {...}, "data": {...}, "node": {...}, "run_dir": ... }
# stdout: JSON UNA línea con campos que se mergean al data bus.
# Solo stdlib de Python (pitfall #2). Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os, difflib

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

# Umbral de similitud por encima del cual dos quizzes se consideran "demasiado
# parecidos". 0.85 sobre el esqueleto normalizado: atrapa "misma oración, otra
# palabra en el hueco" (→ 1.0) sin penalizar oraciones que comparten estructura
# pero difieren en las palabras de contenido fuera del hueco.
THRESHOLD = 0.85


def normalize(qs):
    """Reduce un quizSentence DSL a su esqueleto comparable."""
    s = (qs or "").lower()
    s = re.sub(r"\[[^\]]*\]", " ", s)   # respuesta `[...]`
    s = re.sub(r"\([^)]*\)", " ", s)    # pista `(...)`
    s = re.sub(r"_{2,}", " blank ", s)  # hueco `____` -> placeholder
    s = re.sub(r"[^a-z0-9\s]", " ", s)  # puntuación
    s = re.sub(r"\s+", " ", s).strip()
    return s


def similarity(a, b):
    """max(ratio de secuencia, Jaccard de tokens) sobre los esqueletos."""
    if not a or not b:
        return 0.0
    ratio = difflib.SequenceMatcher(None, a, b).ratio()
    ta, tb = set(a.split()), set(b.split())
    jaccard = (len(ta & tb) / len(ta | tb)) if (ta or tb) else 0.0
    return max(ratio, jaccard)


# Hermanos del ejercicio: input aplanado del CorrectionContext (uno por línea).
siblings = [s for s in (inputs.get("exerciseQuizzes", "") or "").split("\n") if s.strip()]

# El candidato lo produjo `generate` como artifact en {run_dir}/candidate.json.
candidate_sentence = ""
try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        candidate_sentence = (json.load(f).get("quizSentence", "") or "")
except Exception as e:
    # Candidato ausente/ilegible: eval_length ya bloquea ese caso; acá no
    # duplicamos el bloqueo (no hay con qué comparar) -> distinct_ok=True.
    print(f"eval_distinct: candidate unreadable: {e}", file=sys.stderr)
    print(json.dumps({"route": "done", "distinct_ok": True,
                      "distinct_max_similarity": 0.0, "distinct_nearest": "",
                      "distinct_threshold": THRESHOLD, "distinct_sibling_count": len(siblings)}))
    sys.exit(0)

cand_norm = normalize(candidate_sentence)

max_sim = 0.0
nearest = ""
for sib in siblings:
    sim = similarity(cand_norm, normalize(sib))
    if sim > max_sim:
        max_sim = sim
        nearest = sib

# Sin hermanos o candidato vacío → nada que comparar → pasa.
distinct_ok = True if not siblings or not cand_norm else (max_sim < THRESHOLD)

out = {
    "route": "done",
    "distinct_ok": distinct_ok,
    "distinct_max_similarity": round(max_sim, 3),
    "distinct_nearest": nearest if not distinct_ok else "",
    "distinct_threshold": THRESHOLD,
    "distinct_sibling_count": len(siblings),
}
sys.stderr.write("[eval] distinct_ok=%s max_similarity=%.3f (threshold %.2f, %d siblings)\n"
                 % (distinct_ok, max_sim, THRESHOLD, len(siblings)))
print(json.dumps(out, ensure_ascii=False))
PY
