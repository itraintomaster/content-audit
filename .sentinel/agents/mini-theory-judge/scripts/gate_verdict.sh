#!/bin/bash
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

AGENT = "mini-theory-judge"
TAG = "mini_theory_judge_gate"
ROOT_KEYS = {"assessments"}
ASSESSMENT_KEYS = {"quizId", "holds", "problem", "severity", "counterexample", "explanation"}
SEVERITY_FOR = {
    "FALSE_RULE": "HIGH",
    "INSUFFICIENT_FOR_QUIZ": "HIGH",
    "OVERSTATED_FOR_QUIZ": "MEDIUM",
    "OTHER_SUBTOPIC_CONTENT": "LOW",
    "OVER_SPECIFIC_FOR_QUIZ": "LOW",
    "THEORETICAL_JARGON": "LOW",
    "DIDACTIC_DIRECTION": "LOW",
    "MISSING_ENGLISH_HIGHLIGHT": "LOW",
}

state = json.loads(sys.argv[1])
inputs = state.get("inputs", {}) or {}
data = state.get("data", {}) or {}
node = state.get("node", {}) or {}


def resolve_run_dir():
    for raw in (
        state.get("run_dir"), inputs.get("run_dir"), data.get("run_dir"),
        node.get("run_dir"), os.environ.get("SENTINEL_RUN_DIR"),
    ):
        if isinstance(raw, str) and raw.strip():
            path = Path(raw).expanduser().resolve()
            if not path.is_dir():
                return None, "run_dir explícito no existe o no es directorio: %s" % path
            return path, None

    project_raw = (inputs.get("project_root") or inputs.get("root")
                   or data.get("project_root") or data.get("root")
                   or state.get("project_root") or state.get("root"))
    run_id = (state.get("run_id") or inputs.get("run_id")
              or data.get("run_id") or node.get("run_id"))
    if not isinstance(project_raw, str) or not project_raw.strip():
        return None, "faltan run_dir y project_root/root para resolver el run"
    if (not isinstance(run_id, str)
            or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id)
            or Path(run_id).name != run_id):
        return None, "run_id ausente o inseguro"
    project_root = Path(project_raw).expanduser().resolve()
    runs_base = (project_root / ".sentinel" / "runs" / AGENT).resolve()
    if not project_root.is_dir() or not runs_base.is_dir():
        return None, "directorio base de runs inexistente"
    direct = (runs_base / run_id).resolve()
    if direct.parent != runs_base:
        return None, "run_id escaparía del directorio de runs"
    if direct.is_dir():
        return direct, None
    matches = []
    for child in runs_base.iterdir():
        try:
            resolved = child.resolve()
            persisted = json.loads((resolved / "state.json").read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if resolved.parent == runs_base and resolved.is_dir() and persisted.get("runId") == run_id:
            matches.append(resolved)
    if len(matches) != 1:
        return None, "run_id %s coincide con %d runs; se requiere exactamente uno" % (run_id, len(matches))
    return matches[0], None


run_dir, run_dir_error = resolve_run_dir()
validated_path = run_dir / "validated-verdict.json" if run_dir else None


def fail(reason):
    if validated_path is not None:
        try:
            validated_path.unlink()
        except FileNotFoundError:
            pass
    sys.stderr.write("[%s] FAILED: %s\n" % (TAG, reason))
    print(json.dumps({"route": "failed", "error": reason}, ensure_ascii=False))
    raise SystemExit(0)


if run_dir_error:
    fail(run_dir_error)


def parse_json_value(value):
    current = value
    for _ in range(3):
        if not isinstance(current, str):
            return current
        text = current.strip()
        if text.startswith("```"):
            text = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", text)
            text = re.sub(r"\s*```$", "", text).strip()
        try:
            current = json.loads(text)
            continue
        except ValueError:
            start, end = text.find("{"), text.rfind("}")
            if start < 0 or end < start:
                return None
            try:
                current = json.loads(text[start:end + 1])
            except ValueError:
                return None
    return current


source_path = run_dir / "verdicts.json"
if not source_path.is_file():
    fail("verdicts.json no existe")
try:
    verdict = parse_json_value(source_path.read_text(encoding="utf-8"))
except OSError as exc:
    fail("verdicts.json ilegible: %s" % exc)
if not isinstance(verdict, dict) or set(verdict) != ROOT_KEYS:
    fail("la salida debe ser un objeto cerrado con la única clave assessments")

quizzes_text = inputs.get("quizzes")
if not isinstance(quizzes_text, str):
    fail("la entrada quizzes no es texto")
quiz_ids = [match.strip() for match in re.findall(r"(?m)^quizId:\s*(.+?)\s*$", quizzes_text)]
if not quiz_ids or any(not value for value in quiz_ids):
    fail("la entrada no contiene líneas quizId válidas")
if len(set(quiz_ids)) != len(quiz_ids):
    fail("la entrada repite un quizId")

assessments = verdict.get("assessments")
if not isinstance(assessments, list):
    fail("assessments tiene que ser una lista")
canonical = []
for index, raw in enumerate(assessments):
    where = "assessment %d" % (index + 1)
    if not isinstance(raw, dict) or set(raw) != ASSESSMENT_KEYS:
        fail("%s no respeta el objeto cerrado" % where)
    quiz_id = raw.get("quizId")
    holds = raw.get("holds")
    if not isinstance(quiz_id, str) or not quiz_id.strip():
        fail("%s no trae quizId" % where)
    quiz_id = quiz_id.strip()
    if type(holds) is not bool:
        fail("%s.holds debe ser boolean" % where)
    explanation = raw.get("explanation")
    if not isinstance(explanation, str) or not explanation.strip():
        fail("%s.explanation está vacío" % where)
    explanation = " ".join(explanation.split())
    if len(explanation) > 800:
        fail("%s.explanation supera 800 caracteres" % where)

    problem = raw.get("problem")
    severity = raw.get("severity")
    counterexample = raw.get("counterexample")
    if holds:
        if problem is not None or severity is not None or counterexample is not None:
            fail("%s: holds=true exige problem, severity y counterexample null" % where)
    else:
        if not isinstance(problem, str) or problem.strip().upper() not in SEVERITY_FOR:
            fail("%s.problem es desconocido" % where)
        problem = problem.strip().upper()
        if not isinstance(severity, str) or severity.strip().upper() != SEVERITY_FOR[problem]:
            fail("%s: %s exige severity %s" % (where, problem, SEVERITY_FOR[problem]))
        severity = severity.strip().upper()
        if not isinstance(counterexample, str) or not counterexample.strip():
            fail("%s.counterexample está vacío" % where)
        counterexample = " ".join(counterexample.split())
        if len(counterexample) > 300:
            fail("%s.counterexample supera 300 caracteres" % where)

    canonical.append({
        "quizId": quiz_id,
        "holds": holds,
        "problem": problem,
        "severity": severity,
        "counterexample": counterexample,
        "explanation": explanation,
    })

actual_ids = [item["quizId"] for item in canonical]
if actual_ids != quiz_ids:
    fail("assessments debe cubrir los quizId exactamente y en orden; entrada=%s salida=%s" % (quiz_ids, actual_ids))

try:
    validated_path.write_text(
        json.dumps({"assessments": canonical}, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
except OSError as exc:
    fail("no se pudo escribir validated-verdict.json: %s" % exc)
print(json.dumps({"route": "passed", "detail": "veredicto por quizId bien formado"}, ensure_ascii=False))
PY
