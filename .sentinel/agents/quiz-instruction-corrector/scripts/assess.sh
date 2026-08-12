#!/bin/bash
# F-QICOR-R015: consulta el catálogo de criterios sobre el candidato actual.
# NO mide nada acá: delega en `content-audit assess-candidate`, que resuelve el
# contexto y el snapshot del original con las mismas piezas que el motor. Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, os, subprocess, sys

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data = d.get("data", {}) or {}
node = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")


def find_project_root(start):
    """Sube desde run_dir hasta encontrar audit-cli.sh, el wrapper del CLI.
    Mismo mecanismo que eval_length.sh de lemma-absence-agent."""
    cur = os.path.abspath(start)
    for _ in range(20):
        if os.path.isfile(os.path.join(cur, "audit-cli.sh")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur:
            break
        cur = parent
    return None


def fail(reason):
    """Un fallo de infraestructura NO es un candidato reprobado: si no se pudo
    consultar el catálogo, no sabemos nada del candidato. Se marca inelegible
    para que no compita, pero con el motivo a la vista, en vez de inventarle
    un veredicto que nadie emitió."""
    out = {"eligible": False, "satisfied_count": -1, "rank_key": "",
           "all_criteria_met": False, "unmet": [], "error": reason}
    with open(os.path.join(run_dir, "assessment.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False)
    print(json.dumps({"route": "done", "eligible": False, "satisfied_count": -1,
                      "rank_key": "", "all_criteria_met": False,
                      "assess_error": reason}, ensure_ascii=False))
    sys.exit(0)


try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
except Exception as e:
    fail("no se pudo leer candidate.json: %s" % e)

sentence = (cand.get("quizSentence") or "").strip()
translation = (cand.get("translation") or "").strip()
if not sentence:
    fail("el candidato no trae quizSentence")

task_id = (inputs.get("taskId") or data.get("taskId") or "").strip()
plan_id = (inputs.get("planId") or data.get("planId") or "").strip()
if not task_id or not plan_id:
    fail("faltan planId/taskId para resolver el contexto del original")

root = find_project_root(run_dir)
if root is None:
    fail("no se encontro la raiz del proyecto (audit-cli.sh) desde run_dir")

args = ["bash", os.path.join(root, "audit-cli.sh"), "assess-candidate",
        "--plan", plan_id, "--task", task_id,
        "--candidate-sentence", sentence,
        "--candidate-translation", translation,
        "-f", "json"]
try:
    proc = subprocess.run(args, cwd=root, capture_output=True, text=True, timeout=300)
except Exception as e:
    fail("assess-candidate no se pudo ejecutar: %s" % e)
if proc.returncode != 0:
    fail("assess-candidate rc=%d: %s" % (proc.returncode, (proc.stderr or "").strip()[:300]))

# La última línea limpia de stdout es el JSON del assessment.
obj = None
for line in reversed([l.strip() for l in (proc.stdout or "").splitlines() if l.strip()]):
    if line.startswith("{"):
        try:
            obj = json.loads(line)
            break
        except Exception:
            continue
if obj is None:
    fail("assess-candidate no devolvio JSON parseable")

eligible = bool(obj.get("eligible"))
satisfied = obj.get("satisfiedCriteria") or []
unmet = obj.get("unmetCriteria") or []
rank_key = obj.get("rankKey") or ""
# R013: elegible y sin criterios incumplidos = no hay nada que ganar reintentando.
all_met = eligible and not unmet

with open(os.path.join(run_dir, "assessment.json"), "w", encoding="utf-8") as f:
    json.dump(obj, f, ensure_ascii=False)

print(json.dumps({"route": "done",
                  "eligible": eligible,
                  "satisfied_count": len(satisfied),
                  "rank_key": rank_key,
                  "all_criteria_met": all_met}, ensure_ascii=False))
PY
