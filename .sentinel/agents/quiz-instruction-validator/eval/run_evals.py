#!/usr/bin/env python3
"""Validate the real-quiz corpus and optionally score an external agent CLI.

The runner has no third-party dependencies. By default it verifies fixture
structure, exact source fidelity, corpus composition, and the verdict schema.

To exercise a locally available CLI, pass it after ``--command``. The command
receives one JSON object on stdin with the five agent dependencies; ``quiz`` is
a complete JSON string. It must print either the verdict itself or an object
whose ``quizInstructionVerdict`` field contains the verdict (object or JSON
string). No particular Sentinel CLI syntax is assumed here.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

AGENT_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = Path(__file__).resolve().parents[4]
DEFAULT_FIXTURES = Path(__file__).with_name("fixtures.json")
EXPECTED_FAIL_ID = "67fab6d59930102295342145"

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
VIOLATION_KEYS = {"code", "constraint", "evidence", "explanation"}
CHECK_KEYS = {"category", "constraint", "status", "evidence"}
CATEGORIES = {
    "answer_reconstruction",
    "grammar",
    "meaning",
    "solvability",
    "explicit_instruction",
}
CORE_CATEGORIES = CATEGORIES - {"explicit_instruction"}


class ValidationError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def parse_nested_json(value: Any) -> Any:
    current = value
    for _ in range(3):
        if not isinstance(current, str):
            return current
        current = json.loads(current)
    return current


def validate_verdict(verdict: Any, expected_quiz_id: str) -> dict[str, Any]:
    verdict = parse_nested_json(verdict)
    require(isinstance(verdict, dict), "verdict must be an object")
    require(set(verdict) == TOP_KEYS, "verdict top-level keys are not exact")
    require(verdict["schemaVersion"] == "1.0", "schemaVersion must be 1.0")
    require(
        isinstance(verdict["quizId"], str)
        and verdict["quizId"] == expected_quiz_id,
        "quizId does not match input",
    )
    require(
        isinstance(verdict["followsInstructions"], bool),
        "followsInstructions must be boolean",
    )
    confidence = verdict["confidence"]
    require(
        isinstance(confidence, (int, float))
        and not isinstance(confidence, bool)
        and 0 <= confidence <= 1,
        "confidence must be numeric in [0, 1]",
    )
    require(
        verdict["severity"] in {"none", "minor", "major", "critical"},
        "severity is outside enum",
    )
    require(
        isinstance(verdict["reason"], str)
        and 0 < len(verdict["reason"].strip()) <= 400,
        "reason is invalid",
    )

    violations = verdict["violations"]
    require(isinstance(violations, list), "violations must be an array")
    for index, violation in enumerate(violations):
        require(
            isinstance(violation, dict) and set(violation) == VIOLATION_KEYS,
            f"violations[{index}] is not a closed object",
        )
        require(
            isinstance(violation["code"], str)
            and re.fullmatch(r"[A-Z][A-Z0-9_]*", violation["code"]) is not None,
            f"violations[{index}].code is invalid",
        )
        for key in ("constraint", "evidence", "explanation"):
            require(
                isinstance(violation[key], str) and bool(violation[key].strip()),
                f"violations[{index}].{key} is invalid",
            )

    checks = verdict["checkedConstraints"]
    require(
        isinstance(checks, list) and len(checks) >= 5,
        "checkedConstraints must have at least five entries",
    )
    counts = {category: 0 for category in CATEGORIES}
    failed_checks = 0
    for index, check in enumerate(checks):
        require(
            isinstance(check, dict) and set(check) == CHECK_KEYS,
            f"checkedConstraints[{index}] is not a closed object",
        )
        category = check["category"]
        require(category in CATEGORIES, f"checkedConstraints[{index}] bad category")
        counts[category] += 1
        require(
            check["status"] in {"passed", "failed", "not_applicable"},
            f"checkedConstraints[{index}] bad status",
        )
        failed_checks += int(check["status"] == "failed")
        for key in ("constraint", "evidence"):
            require(
                isinstance(check[key], str) and bool(check[key].strip()),
                f"checkedConstraints[{index}].{key} is invalid",
            )

    require(
        all(counts[category] == 1 for category in CORE_CATEGORIES),
        "each base category must appear exactly once",
    )
    require(
        counts["explicit_instruction"] >= 1,
        "at least one explicit_instruction check is required",
    )

    if verdict["followsInstructions"]:
        require(verdict["severity"] == "none", "pass requires severity none")
        require(not violations, "pass requires no violations")
        require(not failed_checks, "pass cannot contain failed checks")
    else:
        require(verdict["severity"] != "none", "fail requires non-none severity")
        require(bool(violations), "fail requires violations")
        require(failed_checks > 0, "fail requires a failed check")
    return verdict


def validate_fixture(path: Path) -> dict[str, Any]:
    fixture = load_json(path)
    require(isinstance(fixture, dict), "fixture root must be an object")
    require(
        set(fixture) == {"name", "source", "cases"},
        "fixture root must contain exactly name/source/cases",
    )
    cases = fixture["cases"]
    require(isinstance(cases, list) and len(cases) == 10, "fixture needs 10 cases")

    source_path = PROJECT_ROOT / fixture["source"]
    source_quizzes = load_json(source_path)
    require(isinstance(source_quizzes, list), "source quizzes must be an array")
    source_by_id = {quiz["id"]: quiz for quiz in source_quizzes}

    case_ids: set[str] = set()
    quiz_ids: set[str] = set()
    fail_ids: list[str] = []
    for index, case in enumerate(cases):
        prefix = f"cases[{index}]"
        require(
            isinstance(case, dict)
            and set(case)
            == {
                "caseId",
                "cefrLevel",
                "topic",
                "title",
                "instructions",
                "quiz",
                "expected",
            },
            f"{prefix} has invalid keys",
        )
        require(case["caseId"] not in case_ids, f"{prefix} duplicate caseId")
        case_ids.add(case["caseId"])
        require(case["cefrLevel"] == "A1", f"{prefix} must use CEFR A1")
        for key in ("topic", "title", "instructions"):
            require(
                isinstance(case[key], str) and bool(case[key].strip()),
                f"{prefix}.{key} must be non-empty",
            )
        quiz = case["quiz"]
        require(isinstance(quiz, dict), f"{prefix}.quiz must be a full object")
        quiz_id = quiz.get("id")
        require(
            isinstance(quiz_id, str) and quiz_id in source_by_id,
            f"{prefix}.quiz id not found in source",
        )
        require(quiz_id not in quiz_ids, f"{prefix} duplicate quiz id")
        quiz_ids.add(quiz_id)
        require(
            quiz == source_by_id[quiz_id],
            f"{prefix}.quiz is not an exact full copy of source",
        )
        require(
            case["topic"] == quiz["topicName"]
            and case["title"] == quiz["title"]
            and case["instructions"] == quiz["instructions"],
            f"{prefix} context disagrees with full quiz",
        )
        expected = case["expected"]
        require(
            isinstance(expected, dict)
            and set(expected) == {"followsInstructions", "rationale"},
            f"{prefix}.expected has invalid keys",
        )
        require(
            isinstance(expected["followsInstructions"], bool)
            and isinstance(expected["rationale"], str)
            and bool(expected["rationale"].strip()),
            f"{prefix}.expected is invalid",
        )
        if not expected["followsInstructions"]:
            fail_ids.append(quiz_id)

    require(
        fail_ids == [EXPECTED_FAIL_ID],
        f"expected exactly fail {EXPECTED_FAIL_ID}, got {fail_ids}",
    )
    required_ids = {
        "67fab6d59930102295342139",
        "67fab6d5993010229534213a",
        "67fab6d5993010229534213b",
        "67fab6d5993010229534213c",
        "67fab6d5993010229534213d",
        "67fab6d59930102295342140",
        EXPECTED_FAIL_ID,
    }
    require(
        required_ids <= quiz_ids,
        "corpus is missing required nominal-subject/direct-question edge cases",
    )
    return fixture


def sample_pass_verdict(quiz_id: str) -> dict[str, Any]:
    checks = []
    for category, label in (
        ("answer_reconstruction", "Reconstrucción completa"),
        ("grammar", "Gramática y naturalidad"),
        ("meaning", "Significado y traducción"),
        ("solvability", "Resolubilidad"),
        ("explicit_instruction", "Uso de will y short forms"),
    ):
        checks.append(
            {
                "category": category,
                "constraint": label,
                "status": "passed",
                "evidence": "Respuesta reconstruida y comprobada.",
            }
        )
    return {
        "schemaVersion": "1.0",
        "quizId": quiz_id,
        "followsInstructions": True,
        "confidence": 0.99,
        "severity": "none",
        "reason": "El quiz cumple todas las restricciones comprobadas.",
        "violations": [],
        "checkedConstraints": checks,
    }


def run_script(script: Path, state: dict[str, Any]) -> dict[str, Any]:
    completed = subprocess.run(
        [str(script)],
        input=json.dumps(state, ensure_ascii=False),
        capture_output=True,
        text=True,
        check=False,
    )
    require(
        completed.returncode == 0,
        "%s failed: %s" % (script.name, completed.stderr.strip()),
    )
    try:
        output = json.loads(completed.stdout)
    except json.JSONDecodeError as exc:
        raise ValidationError(
            "%s did not emit exactly one JSON value: %s" % (script.name, exc)
        ) from exc
    require(isinstance(output, dict), f"{script.name} output must be an object")
    return output


def validate_scripts(fixture: dict[str, Any]) -> None:
    case = fixture["cases"][3]
    quiz = case["quiz"]
    with tempfile.TemporaryDirectory(prefix="quiz-instruction-validator-") as temp:
        project_root = Path(temp)
        run_id = "3ac62128-e81c-4a7e-af94-440b2f1c35fa"
        run_dir = (
            project_root
            / ".sentinel"
            / "runs"
            / "quiz-instruction-validator"
            / "20260729T222051-fixture"
        )
        run_dir.mkdir(parents=True)
        (run_dir / "state.json").write_text(
            json.dumps({"runId": run_id}), encoding="utf-8"
        )
        state = {
            "inputs": {
                "project_root": temp,
                "quiz": json.dumps(quiz, ensure_ascii=False),
            },
            "data": {},
            "node": {},
            "run_id": run_id,
        }
        verdict_path = run_dir / "verdicts.json"
        verdict_path.write_text(
            json.dumps(sample_pass_verdict(quiz["id"]), ensure_ascii=False),
            encoding="utf-8",
        )
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(gate_output.get("route") == "passed", "valid verdict failed gate")
        canonical = load_json(run_dir / "validated-verdict.json")
        validate_verdict(canonical, quiz["id"])

        malformed = sample_pass_verdict(quiz["id"])
        malformed["unexpected"] = True
        verdict_path.write_text(
            json.dumps(malformed, ensure_ascii=False), encoding="utf-8"
        )
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "failed" and gate_output.get("error"),
            "malformed verdict did not fail gate with an error",
        )
        require(
            not (run_dir / "validated-verdict.json").exists(),
            "failed gate left a stale canonical verdict",
        )

        emit_output = run_script(AGENT_DIR / "scripts" / "emit.sh", state)
        require(
            "quizInstructionVerdict" in emit_output,
            "emit did not produce the declared artifact field",
        )
        fallback = validate_verdict(
            emit_output["quizInstructionVerdict"], quiz["id"]
        )
        require(
            fallback["confidence"] == 0.0
            and fallback["followsInstructions"] is False
            and fallback["violations"][0]["code"] == "JUDGE_OUTPUT_INVALID",
            "emit fallback is not conservative and explicit",
        )

        ambiguous_dir = run_dir.parent / "20260729T222052-ambiguous"
        ambiguous_dir.mkdir()
        (ambiguous_dir / "state.json").write_text(
            json.dumps({"runId": run_id}), encoding="utf-8"
        )
        verdict_path.write_text(
            json.dumps(sample_pass_verdict(quiz["id"]), ensure_ascii=False),
            encoding="utf-8",
        )
        ambiguous_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            ambiguous_output.get("route") == "failed"
            and "exactamente uno" in ambiguous_output.get("error", ""),
            "ambiguous state.json runId matches were not rejected",
        )


def extract_verdict(output: str) -> Any:
    parsed = json.loads(output)
    if isinstance(parsed, dict) and "quizInstructionVerdict" in parsed:
        return parsed["quizInstructionVerdict"]
    artifacts = parsed.get("artifacts") if isinstance(parsed, dict) else None
    if isinstance(artifacts, dict) and "quizInstructionVerdict" in artifacts:
        return artifacts["quizInstructionVerdict"]
    return parsed


def run_command(
    command: list[str], case: dict[str, Any], timeout_seconds: float
) -> dict[str, Any]:
    payload = {
        "cefrLevel": case["cefrLevel"],
        "topic": case["topic"],
        "title": case["title"],
        "instructions": case["instructions"],
        "quiz": json.dumps(case["quiz"], ensure_ascii=False, separators=(",", ":")),
    }
    completed = subprocess.run(
        command,
        input=json.dumps(payload, ensure_ascii=False),
        capture_output=True,
        text=True,
        timeout=timeout_seconds,
        check=False,
    )
    if completed.returncode != 0:
        raise ValidationError(
            "agent command failed with exit %d: %s"
            % (completed.returncode, completed.stderr.strip())
        )
    verdict = validate_verdict(
        extract_verdict(completed.stdout), case["quiz"]["id"]
    )
    if (
        verdict["confidence"] == 0.0
        and verdict["violations"]
        and verdict["violations"][0]["code"] == "JUDGE_OUTPUT_INVALID"
    ):
        raise ValidationError(
            "agent exhausted structured-output retries for quiz %s"
            % case["quiz"]["id"]
        )
    return verdict


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixtures", type=Path, default=DEFAULT_FIXTURES)
    parser.add_argument("--timeout", type=float, default=120.0)
    parser.add_argument(
        "--command",
        nargs=argparse.REMAINDER,
        help="External agent CLI and arguments; receives one case on stdin.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        fixture = validate_fixture(args.fixtures)
        schema = load_json(AGENT_DIR / "schema" / "verdict.schema.json")
        require(
            schema.get("additionalProperties") is False
            and set(schema.get("required", [])) == TOP_KEYS,
            "JSON schema is not closed or disagrees with required keys",
        )
        print(
            "fixture: OK (10 real quizzes, 9 pass, 1 fail: %s)"
            % EXPECTED_FAIL_ID
        )
        print("schema: OK (closed verdict contract)")
        validate_scripts(fixture)
        print(
            "scripts: OK (state.json UUID resolution, ambiguity rejection, "
            "gate pass/fail, emit fallback)"
        )

        if not args.command:
            print("agent: SKIPPED (pass --command <cli ...> to run live evals)")
            return 0

        mismatches = 0
        for case in fixture["cases"]:
            verdict = run_command(args.command, case, args.timeout)
            expected = case["expected"]["followsInstructions"]
            actual = verdict["followsInstructions"]
            marker = "PASS" if actual == expected else "MISMATCH"
            mismatches += int(actual != expected)
            print(
                "%s %s expected=%s actual=%s confidence=%.2f"
                % (
                    marker,
                    case["quiz"]["id"],
                    str(expected).lower(),
                    str(actual).lower(),
                    verdict["confidence"],
                )
            )
        print("agent: %d/10 matched" % (10 - mismatches))
        return 1 if mismatches else 0
    except (OSError, json.JSONDecodeError, subprocess.TimeoutExpired, ValidationError) as exc:
        print("ERROR: %s" % exc, file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
