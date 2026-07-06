#!/bin/bash
# Eval GLOBAL de distinción: el candidato NO debe ser casi-idéntico (en palabras y
# frase) a NINGÚN quiz ya existente del curso (corpus .content-audit/quiz-corpus.jsonl)
# ni a otra propuesta pending del batch (glob .content-audit/revisions/*/). Evita
# generar quizzes prácticamente iguales entre sí (p.ej. varios "She showers").
#
# Complementa eval_distinct (solo hermanos del knowledge): éste mira TODO el curso.
# Compara la ORACIÓN RELLENA normalizada (mantiene la respuesta) por similitud
# (difflib + Jaccard), umbral 0.85, con pruning por content-words.
#
# CLAVE — no penalizar la edición mínima de Fase 1: el candidato es, a propósito,
# muy parecido a su ORIGINAL. Por eso EXCLUYE del corpus toda entrada que sea
# >=0.85 similar a la oración ORIGINAL (input `sentence`): eso saca el propio quiz
# y sus casi-iguales, y deja detectar solo duplicados de OTROS quizzes distintos.
#
# Determinista y bloqueante (computa, no rutea). stdin: JSON {inputs,data,node,run_dir}.
# stdout: 1 línea JSON al data bus. Solo stdlib. SOFT-PASS VISIBLE si falta el corpus.
set -euo pipefail
INPUT="$(cat)"
python3 - "$INPUT" <<'PY'
import json, re, sys, os, glob, difflib

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or (d.get("data") or {}).get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

THRESHOLD = 0.85          # rechazo si el candidato es >= esto a OTRO quiz
OWN_EXCL  = 0.85          # excluye del corpus lo que sea >= esto al ORIGINAL
CORPUS = ".content-audit/quiz-corpus.jsonl"
REVGLOB = ".content-audit/revisions/*/*.json"

STOP = set("a an the of to in on at for with and or but is are am was were be been being do does did doing have has had having not no this that these those i you he she it we they my your his her its our their me him them us as so if then than there here up down out off over under again very just really only when where while what which who whom how why can could will would shall should may might must s t re ve ll d m o".split())

def norm(text):
    s = (text or "").lower()
    s = re.sub(r"[^a-z0-9\s]", " ", s)
    return re.sub(r"\s+", " ", s).strip()

def plain_from_dsl(qs):
    """Rellena el quizSentence DSL: '____ [ans] (hint)' -> 'ans'; dropea (hint)."""
    s = qs or ""
    s = re.sub(r"\(([^)]*)\)", " ", s)                 # dropea (pista)
    s = re.sub(r"_{2,}\s*\[([^\]]*)\]", r" \1 ", s)    # '____ [ans]' -> ans
    s = re.sub(r"\[([^\]]*)\]", r" \1 ", s)            # '[ans]' -> ans
    s = re.sub(r"_{2,}", " ", s)                       # huecos sueltos
    return norm(s)

def cwset(n):
    return set(w for w in n.split() if w not in STOP and len(w) > 1)

def sim(a, b):
    if not a or not b: return 0.0
    ratio = difflib.SequenceMatcher(None, a, b).ratio()
    ta, tb = set(a.split()), set(b.split())
    jac = (len(ta & tb) / len(ta | tb)) if (ta or tb) else 0.0
    return max(ratio, jac)

def emit(ok, s=0.0, nearest="", nlevel="", compared=0, note=""):
    if note: print("eval_global_distinct: " + note, file=sys.stderr)
    print(json.dumps({"route": "done", "global_distinct_ok": ok,
        "global_distinct_max_similarity": round(s, 3), "global_distinct_nearest": nearest[:120],
        "global_distinct_nearest_level": nlevel, "global_distinct_threshold": THRESHOLD,
        "global_distinct_compared": compared}))
    sys.exit(0)

# --- candidato (rellenado a oración plana) ---
try:
    cand_qs = json.load(open(os.path.join(run_dir, "candidate.json"), encoding="utf-8")).get("quizSentence", "") or ""
except Exception as e:
    emit(True, note=f"candidate unreadable ({e}) -> no bloquea (eval_length ya cubre)")
cand = plain_from_dsl(cand_qs)
cand_cw = cwset(cand)
if not cand:
    emit(True, note="candidato vacío -> no bloquea")

# oración ORIGINAL (ya viene plana en el input `sentence`) para auto-exclusión
orig = norm(inputs.get("sentence", "") or "")
task_id = inputs.get("taskId", "") or ""

if not os.path.exists(CORPUS):
    emit(True, note=f"SOFT-PASS: falta corpus {CORPUS} (correr build_quiz_corpus.py) -> no bloquea")

best_sim, best_plain, best_lvl, compared = 0.0, "", "", 0
def consider(plain, cw, lvl):
    global best_sim, best_plain, best_lvl, compared
    if not plain: return
    if cand_cw and cw and not (cand_cw & cw): return    # pruning: sin content-word en común
    if orig and sim(plain, orig) >= OWN_EXCL: return    # excluir el propio quiz / casi-iguales del original
    compared += 1
    s = sim(cand, plain)
    if s > best_sim:
        best_sim, best_plain, best_lvl = s, plain, lvl

with open(CORPUS, encoding="utf-8") as f:
    for line in f:
        try: r = json.loads(line)
        except: continue
        consider(r.get("plain", ""), set(r.get("cwords", [])), r.get("level", ""))

# propuestas PENDING del batch (dedup intra-batch)
for pf in glob.glob(REVGLOB):
    if pf.endswith(".preview.json"): continue
    if task_id and os.path.basename(pf).startswith(task_id + "-"): continue   # excluir la propia tarea
    try: pj = json.load(open(pf, encoding="utf-8"))
    except: continue
    if (pj.get("verdict") or "") != "PENDING_APPROVAL": continue
    quiz = (((pj.get("proposal") or {}).get("elementAfter") or {}).get("quiz")) or {}
    form = quiz.get("form") or {}
    sents = form.get("sentences") or quiz.get("sentences") or []
    if isinstance(sents, str): sents = [sents]
    p = norm(" ".join(s for s in sents if s))
    consider(p, cwset(p), "pending")

emit(best_sim <= THRESHOLD, best_sim, best_plain, best_lvl, compared)
PY
