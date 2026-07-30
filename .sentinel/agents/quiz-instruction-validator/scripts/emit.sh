#!/bin/bash
# Emite quizInstructionVerdict como artifact del framework. Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

state = json.loads(sys.argv[1])
inputs = state.get("inputs", {}) or {}
data = state.get("data", {}) or {}
node = state.get("node", {}) or {}


def resolve_run_dir():
    explicit = (
        state.get("run_dir"),
        inputs.get("run_dir"),
        data.get("run_dir"),
        node.get("run_dir"),
        os.environ.get("SENTINEL_RUN_DIR"),
    )
    for raw in explicit:
        if isinstance(raw, str) and raw.strip():
            path = Path(raw).expanduser().resolve()
            if not path.is_dir():
                return None, "run_dir explícito no existe o no es directorio: %s" % path
            return path, None

    project_raw = (
        inputs.get("project_root")
        or inputs.get("root")
        or data.get("project_root")
        or data.get("root")
        or state.get("project_root")
        or state.get("root")
    )
    run_id = (
        state.get("run_id")
        or inputs.get("run_id")
        or data.get("run_id")
        or node.get("run_id")
    )
    if not isinstance(project_raw, str) or not project_raw.strip():
        return None, "faltan run_dir y project_root/root para resolver el run"
    if (
        not isinstance(run_id, str)
        or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id)
        or Path(run_id).name != run_id
    ):
        return None, "run_id ausente o inseguro"

    project_root = Path(project_raw).expanduser().resolve()
    if not project_root.is_dir():
        return None, "project_root/root no existe o no es directorio: %s" % project_root
    runs_base = (
        project_root / ".sentinel" / "runs" / "quiz-instruction-validator"
    ).resolve()
    if not runs_base.is_dir():
        return None, "directorio base de runs no existe: %s" % runs_base

    direct = (runs_base / run_id).resolve()
    if direct.parent != runs_base:
        return None, "run_id escaparía del directorio de runs"
    if direct.is_dir():
        return direct, None

    matches = []
    for child in runs_base.iterdir():
        try:
            resolved_child = child.resolve()
        except OSError:
            continue
        if resolved_child.parent != runs_base or not resolved_child.is_dir():
            continue
        state_path = resolved_child / "state.json"
        try:
            with state_path.open("r", encoding="utf-8") as handle:
                persisted_state = json.load(handle)
        except (OSError, json.JSONDecodeError):
            continue
        if (
            isinstance(persisted_state, dict)
            and persisted_state.get("runId") == run_id
        ):
            matches.append(resolved_child)

    if len(matches) != 1:
        return None, (
            "run_id %s coincide con %d state.json bajo %s; se requiere exactamente uno"
            % (run_id, len(matches), runs_base)
        )
    return matches[0], None


run_dir, run_dir_error = resolve_run_dir()
if run_dir_error:
    sys.stderr.write("[quiz_instruction_emit] " + run_dir_error + "\n")


def parse_quiz(raw):
    current = raw
    for _ in range(3):
        if not isinstance(current, str):
            break
        try:
            current = json.loads(current)
        except (TypeError, json.JSONDecodeError):
            return {}
    return current if isinstance(current, dict) else {}


def quiz_id():
    quiz = parse_quiz(inputs.get("quiz", data.get("quiz", "")))
    if isinstance(quiz.get("id"), str):
        return quiz["id"]
    mongo_id = quiz.get("_id")
    if isinstance(mongo_id, dict) and isinstance(mongo_id.get("$oid"), str):
        return mongo_id["$oid"]
    return ""


validated_path = run_dir / "validated-verdict.json" if run_dir else None
try:
    with validated_path.open("r", encoding="utf-8") as handle:
        verdict = json.load(handle)
except (AttributeError, OSError, json.JSONDecodeError):
    verdict = {
        "schemaVersion": "1.0",
        "quizId": quiz_id(),
        "followsInstructions": False,
        "confidence": 0.0,
        "severity": "critical",
        "reason": "No se pudo obtener un veredicto estructuralmente válido.",
        "violations": [
            {
                "code": "JUDGE_OUTPUT_INVALID",
                "constraint": "Obtener un juicio estructurado verificable",
                "evidence": "El juez agotó sus intentos de salida válida.",
                "explanation": "El resultado es conservador por una falla de infraestructura.",
            }
        ],
        "checkedConstraints": [
            {
                "category": "answer_reconstruction",
                "constraint": "Reconstruir todas las respuestas aceptadas",
                "status": "failed",
                "evidence": "No hubo salida válida del juez.",
            },
            {
                "category": "grammar",
                "constraint": "Gramática y naturalidad",
                "status": "not_applicable",
                "evidence": "No evaluado por falla de infraestructura.",
            },
            {
                "category": "meaning",
                "constraint": "Significado y traducción",
                "status": "not_applicable",
                "evidence": "No evaluado por falla de infraestructura.",
            },
            {
                "category": "solvability",
                "constraint": "Consigna y cues resolubles",
                "status": "not_applicable",
                "evidence": "No evaluado por falla de infraestructura.",
            },
            {
                "category": "explicit_instruction",
                "constraint": "Todas las restricciones explícitas",
                "status": "not_applicable",
                "evidence": "No evaluado por falla de infraestructura.",
            },
        ],
    }

content = json.dumps(verdict, ensure_ascii=False, separators=(",", ":"))
print(
    json.dumps(
        {"route": "done", "quizInstructionVerdict": content},
        ensure_ascii=False,
    )
)
PY
