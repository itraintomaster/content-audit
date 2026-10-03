#!/usr/bin/env python3
"""Validate the real-quiz corpus and optionally score an external agent CLI.

The runner has no third-party dependencies. By default it verifies fixture
structure, exact source fidelity, corpus composition, and the verdict schema.

To exercise a locally available CLI, pass it after ``--command``. The command
receives one JSON object on stdin with the seven agent dependencies, the
exercise as the student sees it (F-CALUM-R001): ``cefrLevel``, ``topic``, the
``title`` and ``instructions`` of the quiz copy, ``exercise`` (the statement
with ``___`` in every gap and its hint), ``acceptedAnswers`` (one line per gap,
``Hueco N: a | b``) and ``options`` (multiple choice only). It must print
either the verdict itself or an object whose ``quizInstructionVerdict`` field
contains the verdict (object or JSON string). No particular Sentinel CLI syntax
is assumed here.
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
# The real quizzes of the corpus that do not follow their instructions in the course of today (the
# 29/9 photo plus 0.2). There is none: «I will stay» (…2145), the one that used to fail, was
# corrected on 11/8 to «I 'll stay», and the instructions now ask for short forms «si es posible».
EXPECTED_FAIL_IDS: frozenset = frozenset()
CONTRACTED_AFFIRMATIVE_ID = "67fab6d59930102295342145"
# So the corpus also carries invented quizzes of the same topic that do not follow it: built like the
# real ones, absent from the course, never taken from an exam. Every one of them must fail.
INVENTED_PREFIX = "inventado-"
MIN_INVENTED = 2
CASE_KEYS = {"caseId", "cefrLevel", "topic", "title", "instructions", "quiz", "expected"}

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
        set(fixture) == {"name", "source", "cases", "invented"},
        "fixture root must contain exactly name/source/cases/invented",
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
            isinstance(case, dict) and set(case) == CASE_KEYS,
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
        set(fail_ids) == EXPECTED_FAIL_IDS and len(fail_ids) == len(EXPECTED_FAIL_IDS),
        f"expected exactly fail {sorted(EXPECTED_FAIL_IDS)}, got {fail_ids}",
    )
    required_ids = {
        "67fab6d59930102295342139",
        "67fab6d59930102295342142",
        "67fab6d5993010229534213b",
        "67fab6d5993010229534213c",
        "67fab6d5993010229534213d",
        "67fab6d59930102295342140",
        CONTRACTED_AFFIRMATIVE_ID,
    }
    require(
        required_ids <= quiz_ids,
        "corpus is missing required nominal-subject/direct-question edge cases",
    )

    invented = fixture["invented"]
    require(
        isinstance(invented, list) and len(invented) >= MIN_INVENTED,
        f"fixture needs at least {MIN_INVENTED} invented failing cases",
    )
    for index, case in enumerate(invented):
        prefix = f"invented[{index}]"
        require(
            isinstance(case, dict) and set(case) == CASE_KEYS,
            f"{prefix} has invalid keys",
        )
        require(case["caseId"] not in case_ids, f"{prefix} duplicate caseId")
        case_ids.add(case["caseId"])
        require(case["cefrLevel"] == "A1", f"{prefix} must use CEFR A1")
        quiz = case["quiz"]
        require(isinstance(quiz, dict), f"{prefix}.quiz must be a full object")
        quiz_id = quiz.get("id")
        require(
            isinstance(quiz_id, str) and quiz_id.startswith(INVENTED_PREFIX),
            f"{prefix}.quiz id must start with {INVENTED_PREFIX}",
        )
        require(quiz_id not in source_by_id and quiz_id not in quiz_ids,
                f"{prefix}.quiz id must be new")
        quiz_ids.add(quiz_id)
        require(
            set(quiz) == set(source_by_id[next(iter(source_by_id))]),
            f"{prefix}.quiz must have the fields of a quiz of the course",
        )
        require(
            case["topic"] == quiz["topicName"]
            and case["title"] == quiz["title"]
            and case["instructions"] == quiz["instructions"],
            f"{prefix} context disagrees with full quiz",
        )
        sentences = quiz.get("form", {}).get("sentences") or []
        require(
            bool(sentences)
            and all(sentence not in {s for q in source_quizzes for s in q.get("form", {}).get("sentences") or []}
                    for sentence in sentences),
            f"{prefix} must not repeat a sentence of the course",
        )
        expected = case["expected"]
        require(
            isinstance(expected, dict)
            and set(expected) == {"followsInstructions", "rationale"}
            and expected["followsInstructions"] is False
            and isinstance(expected["rationale"], str)
            and bool(expected["rationale"].strip()),
            f"{prefix}.expected must be a failure with its rationale",
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

        # The judge sometimes closes a check with an empty filler key after a stray comma
        # ("status2": null, "evidenceNote": ""): it says nothing, so the gate drops it and the
        # canonical verdict keeps the closed object.
        for empty in (None, "", "  "):
            empty_extra = sample_pass_verdict(quiz["id"])
            empty_extra["checkedConstraints"][0]["status2"] = empty
            verdict_path.write_text(
                json.dumps(empty_extra, ensure_ascii=False), encoding="utf-8"
            )
            gate_output = run_script(
                AGENT_DIR / "scripts" / "gate_verdict.sh", state
            )
            require(
                gate_output.get("route") == "passed",
                "an empty extra key (%r) failed the gate" % (empty,),
            )
            validate_verdict(load_json(run_dir / "validated-verdict.json"), quiz["id"])

        # An extra key with content still fails: the object stays closed.
        noted = sample_pass_verdict(quiz["id"])
        noted["checkedConstraints"][0]["note"] = "una nota"
        verdict_path.write_text(json.dumps(noted, ensure_ascii=False), encoding="utf-8")
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "failed"
            and "objeto cerrado" in gate_output.get("error", ""),
            "an extra key with content did not fail the gate",
        )

        # Other times the judge leaves the comma with no key after it ("evidence": "…",
        # and then }). That is not JSON, but the comma says nothing: the gate reads the
        # verdict without it. A comma inside a text stays as it is.
        commas = sample_pass_verdict(quiz["id"])
        commas["checkedConstraints"][0]["evidence"] = "queda igual: «a, }» y «b, ]»"
        verdict_path.write_text(
            re.sub(
                r'([^\s,{\[])(\n\s*[}\]])',
                r"\1,\2",
                json.dumps(commas, ensure_ascii=False, indent=2),
            ),
            encoding="utf-8",
        )
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "passed",
            "a dangling comma before } or ] failed the gate",
        )
        canonical = load_json(run_dir / "validated-verdict.json")
        validate_verdict(canonical, quiz["id"])
        require(
            canonical["checkedConstraints"][0]["evidence"]
            == "queda igual: «a, }» y «b, ]»",
            "the gate touched a comma inside a text",
        )

        # An approving judge sometimes fills the list with an empty element instead of
        # leaving it empty ({"code": "NONE", "constraint": "", …}). It says nothing: the
        # gate drops it and the verdict passes with violations empty.
        filler = sample_pass_verdict(quiz["id"])
        filler["violations"] = [
            {"code": "NONE", "constraint": "", "evidence": "", "explanation": ""}
        ]
        verdict_path.write_text(json.dumps(filler, ensure_ascii=False), encoding="utf-8")
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "passed",
            "an empty filler violation failed the gate",
        )
        canonical = load_json(run_dir / "validated-verdict.json")
        validate_verdict(canonical, quiz["id"])
        require(canonical["violations"] == [], "the filler violation was kept")

        # A violation with a real code but no text is not filler: it still fails.
        coded = sample_pass_verdict(quiz["id"])
        coded["violations"] = [
            {"code": "WRONG_FORM", "constraint": "", "evidence": "", "explanation": ""}
        ]
        verdict_path.write_text(json.dumps(coded, ensure_ascii=False), encoding="utf-8")
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "failed",
            "a coded violation without text passed the gate",
        )

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


def student_view_inputs(case: dict[str, Any]) -> dict[str, str]:
    """The seven inputs of the judge, built as DefaultQuizStudentViewRenderer and
    DefaultQuizInstructionSubjectBuilder build them from a quiz of the course."""
    quiz = case["quiz"]
    form = quiz.get("form") or {}
    parts = form.get("sentenceParts") or []
    tokens = []
    gaps = []
    for part in parts:
        if part.get("kind") == "CLOZE":
            tokens.append("___")
            accepted = [variant.strip() for option in (part.get("options") or []) if option is not None
                        for variant in str(option).split("|") if variant.strip()]
            gaps.append(accepted)
        elif (part.get("text") or "").strip():
            tokens.append(part["text"].strip())
    items = (form.get("multipleChoice") or {}).get("items") or []
    if items:
        correct = [item.get("label") for item in items if (item.get("incidence") or 0) > 0][:1]
        gaps = [correct for _ in gaps]
    return {
        "cefrLevel": case["cefrLevel"],
        "topic": case["topic"],
        "title": quiz.get("title") or "",
        "instructions": quiz.get("instructions") or "",
        "exercise": " ".join(tokens),
        "acceptedAnswers": "\n".join(
            "Hueco %d: %s" % (index + 1, " | ".join(accepted)) for index, accepted in enumerate(gaps)),
        "options": "\n".join(
            "- %s%s" % (item.get("label") or "", " (correcta)" if (item.get("incidence") or 0) > 0 else "")
            for item in items),
    }


def run_command(
    command: list[str], case: dict[str, Any], timeout_seconds: float
) -> dict[str, Any]:
    payload = student_view_inputs(case)
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
    # The judge never receives the quiz identifier (F-QINST-R009), so its quizId stays empty.
    verdict = validate_verdict(extract_verdict(completed.stdout), "")
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
        fails = sorted(EXPECTED_FAIL_IDS)
        print(
            "fixture: OK (10 real quizzes, %d pass, %d fail%s; %d invented, all fail)"
            % (10 - len(fails), len(fails), (": " + ", ".join(fails)) if fails else "",
               len(fixture["invented"]))
        )
        print("schema: OK (closed verdict contract)")
        validate_scripts(fixture)
        print(
            "scripts: OK (state.json UUID resolution, ambiguity rejection, "
            "gate pass/fail, empty extra keys dropped, dangling commas read, filler dropped, "
            "emit fallback)"
        )

        if not args.command:
            print("agent: SKIPPED (pass --command <cli ...> to run live evals)")
            return 0

        mismatches = 0
        scored = fixture["cases"] + fixture["invented"]
        for case in scored:
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
        print("agent: %d/%d matched" % (len(scored) - mismatches, len(scored)))
        return 1 if mismatches else 0
    except (OSError, json.JSONDecodeError, subprocess.TimeoutExpired, ValidationError) as exc:
        print("ERROR: %s" % exc, file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
