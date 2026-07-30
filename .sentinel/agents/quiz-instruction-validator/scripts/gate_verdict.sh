#!/bin/bash
# Gate binario de forma para el juez de instrucciones.
# stdin: estado del framework. stdout: UNA línea JSON. Solo Python stdlib.
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
    """Resolve the framework run directory without assuming run_dir is projected.

    The live CLI projects run_id at top level and project_root/root in inputs.
    Explicit run_dir values keep precedence for in-process launchers.
    """
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

    # El run_id proyectado por el subprocess es el UUID de RunState, no
    # necesariamente el basename timestamped que usa RunPersistence.
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
source_path = run_dir / "verdicts.json" if run_dir else None
validated_path = run_dir / "validated-verdict.json" if run_dir else None


def fail(reason):
    if validated_path is not None:
        try:
            validated_path.unlink()
        except FileNotFoundError:
            pass
    sys.stderr.write("[quiz_instruction_gate] FAILED: " + reason + "\n")
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
        except json.JSONDecodeError:
            start = text.find("{")
            if start < 0:
                return None
            depth = 0
            in_string = False
            escaped = False
            for index in range(start, len(text)):
                char = text[index]
                if in_string:
                    if escaped:
                        escaped = False
                    elif char == "\\":
                        escaped = True
                    elif char == '"':
                        in_string = False
                    continue
                if char == '"':
                    in_string = True
                elif char == "{":
                    depth += 1
                elif char == "}":
                    depth -= 1
                    if depth == 0:
                        try:
                            current = json.loads(text[start:index + 1])
                        except json.JSONDecodeError:
                            return None
                        break
            else:
                return None
    return current


try:
    with source_path.open("r", encoding="utf-8") as handle:
        verdict = parse_json_value(handle.read())
except OSError as exc:
    fail("verdicts.json no se puede leer: %s" % exc)

if not isinstance(verdict, dict):
    fail("la salida no contiene un objeto JSON parseable")

TOP_KEYS = {
    "schemaVersion",
    "quizId",
    "followsInstructions",
    "confidence",
    "severity",
    "reason",
    "violations",
    "checkedConstraints",
}
if set(verdict) != TOP_KEYS:
    missing = sorted(TOP_KEYS - set(verdict))
    extra = sorted(set(verdict) - TOP_KEYS)
    fail("claves top-level inválidas; faltan=%s extras=%s" % (missing, extra))

if verdict["schemaVersion"] != "1.0":
    fail("schemaVersion debe ser '1.0'")
if not isinstance(verdict["quizId"], str):
    fail("quizId debe ser string")
if not isinstance(verdict["followsInstructions"], bool):
    fail("followsInstructions debe ser boolean")
confidence = verdict["confidence"]
if (
    not isinstance(confidence, (int, float))
    or isinstance(confidence, bool)
    or not 0 <= confidence <= 1
):
    fail("confidence debe ser un número entre 0 y 1")
if verdict["severity"] not in {"none", "minor", "major", "critical"}:
    fail("severity fuera del enum")
if (
    not isinstance(verdict["reason"], str)
    or not verdict["reason"].strip()
    or len(verdict["reason"]) > 400
):
    fail("reason debe ser string no vacío de hasta 400 caracteres")


def expected_quiz_id(raw):
    parsed = parse_json_value(raw)
    if not isinstance(parsed, dict):
        return ""
    identifier = parsed.get("id")
    if isinstance(identifier, str):
        return identifier
    mongo_id = parsed.get("_id")
    if isinstance(mongo_id, dict) and isinstance(mongo_id.get("$oid"), str):
        return mongo_id["$oid"]
    return ""


source_quiz_id = expected_quiz_id(inputs.get("quiz", data.get("quiz", "")))
if source_quiz_id and verdict["quizId"] != source_quiz_id:
    fail("quizId no coincide con el quiz de entrada")

violations = verdict["violations"]
if not isinstance(violations, list):
    fail("violations debe ser array")
VIOLATION_KEYS = {"code", "constraint", "evidence", "explanation"}
for index, item in enumerate(violations):
    if not isinstance(item, dict) or set(item) != VIOLATION_KEYS:
        fail("violations[%d] no respeta el objeto cerrado" % index)
    if not isinstance(item["code"], str) or not re.fullmatch(
        r"[A-Z][A-Z0-9_]*", item["code"]
    ):
        fail("violations[%d].code debe ser UPPER_SNAKE_CASE" % index)
    for key, maximum in (
        ("constraint", 240),
        ("evidence", 300),
        ("explanation", 300),
    ):
        value = item[key]
        if not isinstance(value, str) or not value.strip() or len(value) > maximum:
            fail("violations[%d].%s es inválido" % (index, key))

checks = verdict["checkedConstraints"]
if not isinstance(checks, list) or len(checks) < 5:
    fail("checkedConstraints debe contener al menos cinco checks")
CHECK_KEYS = {"category", "constraint", "status", "evidence"}
CATEGORIES = {
    "answer_reconstruction",
    "grammar",
    "meaning",
    "solvability",
    "explicit_instruction",
}
CORE = CATEGORIES - {"explicit_instruction"}
category_counts = {category: 0 for category in CATEGORIES}
failed_checks = 0
for index, item in enumerate(checks):
    if not isinstance(item, dict) or set(item) != CHECK_KEYS:
        fail("checkedConstraints[%d] no respeta el objeto cerrado" % index)
    category = item["category"]
    if category not in CATEGORIES:
        fail("checkedConstraints[%d].category fuera del enum" % index)
    category_counts[category] += 1
    if item["status"] not in {"passed", "failed", "not_applicable"}:
        fail("checkedConstraints[%d].status fuera del enum" % index)
    if item["status"] == "failed":
        failed_checks += 1
    for key, maximum in (("constraint", 240), ("evidence", 300)):
        value = item[key]
        if not isinstance(value, str) or not value.strip() or len(value) > maximum:
            fail("checkedConstraints[%d].%s es inválido" % (index, key))

if any(category_counts[category] != 1 for category in CORE):
    fail("cada check base debe aparecer exactamente una vez")
if category_counts["explicit_instruction"] < 1:
    fail("debe existir al menos un check explicit_instruction")

passes = verdict["followsInstructions"]
if passes:
    if verdict["severity"] != "none":
        fail("un veredicto pass debe usar severity='none'")
    if violations:
        fail("un veredicto pass debe tener violations vacío")
    if failed_checks:
        fail("un veredicto pass no puede contener checks failed")
else:
    if verdict["severity"] == "none":
        fail("un veredicto fail no puede usar severity='none'")
    if not violations:
        fail("un veredicto fail debe explicar al menos una violation")
    if failed_checks == 0:
        fail("un veredicto fail debe contener al menos un check failed")

canonical = {
    "schemaVersion": "1.0",
    "quizId": verdict["quizId"],
    "followsInstructions": passes,
    "confidence": float(confidence),
    "severity": verdict["severity"],
    "reason": verdict["reason"].strip(),
    "violations": violations,
    "checkedConstraints": checks,
}
with validated_path.open("w", encoding="utf-8") as handle:
    json.dump(canonical, handle, ensure_ascii=False, separators=(",", ":"))

print(json.dumps({"route": "passed"}, ensure_ascii=False))
PY
