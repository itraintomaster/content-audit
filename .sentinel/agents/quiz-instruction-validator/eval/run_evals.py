#!/usr/bin/env python3
"""Validate the eval corpora and optionally score an external agent CLI.

The runner has no third-party dependencies. By default it verifies fixture and
regression structure, exact source fidelity, corpus composition, expected
labels, and the verdict schema.

To exercise a locally available CLI, pass it after ``--command``. The command
receives one JSON object on stdin with the seven agent dependencies, the
exercise as the student sees it (F-CALUM-R001): ``cefrLevel``, ``topic``, the
``title`` and ``instructions`` of the quiz copy, ``exercise`` (the statement
with ``___`` in every gap and its hint), ``acceptedAnswers`` (one line per gap,
``Hueco N: a | b``) and ``options`` (multiple choice only). It must print
either the verdict itself or an object whose ``quizInstructionVerdict`` field
contains the verdict (object or JSON string). No particular Sentinel CLI syntax
is assumed here.

To score OpenAI Codex directly, use the explicit, capped mode ``--codex
--max N``. Each case runs in an empty temporary directory with a read-only
sandbox, an ephemeral session, the judge's closed output schema, and
``--output-last-message``. The evaluator prompt is read from ``evaluate.md``;
``expected``, ``rationale`` and ``excludedContext`` never reach the model.
Examples, from the project root:

    python3 .sentinel/agents/quiz-instruction-validator/eval/run_evals.py \
      --codex --dataset regressions --max 8 \
      --results analysis/evaluaciones/qinst-codex-regressions.json
    python3 .sentinel/agents/quiz-instruction-validator/eval/run_evals.py \
      --codex --dataset regressions --max 2 \
      --cases qinst-r020-optional-accepted-segment,qinst-r020-unrequested-preposition-repair \
      --model gpt-6.1-sol --reasoning-effort medium \
      --results analysis/evaluaciones/qinst-codex-piloto.json
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

AGENT_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = Path(__file__).resolve().parents[4]
DEFAULT_FIXTURES = Path(__file__).with_name("fixtures.json")
DEFAULT_REGRESSIONS = Path(__file__).with_name("regressions.json")
EVALUATE_NODE = AGENT_DIR / "nodes" / "evaluate.md"
VERDICT_SCHEMA = AGENT_DIR / "schema" / "verdict.schema.json"
DEFAULT_CODEX_MODEL = "gpt-6.1-sol"
DEFAULT_REASONING_EFFORT = "medium"
CODEX_ERROR_TAIL_CHARS = 3000
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
REGRESSION_CASE_KEYS = {"caseId", "scenario", "input", "excludedContext", "expected"}
REGRESSION_INPUT_KEYS = {
    "cefrLevel",
    "topic",
    "title",
    "instructions",
    "exercise",
    "acceptedAnswers",
    "options",
}
REGRESSION_LABELS = {
    "optional-accepted-segment": True,
    "unrequested-preposition-repair": False,
    "mechanical-some-any": True,
    "exact-skeleton-reordering": True,
    "unaccepted-alternative-out-of-scope": True,
    "defensible-distractor-out-of-scope": True,
    "unrelated-general-grammar": True,
    "excluded-wrong-mini-theory": True,
}

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


def json_schema_type(value: Any) -> str:
    """Return the JSON Schema primitive type for one concrete JSON value."""
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, int):
        return "integer"
    if isinstance(value, float):
        return "number"
    if isinstance(value, str):
        return "string"
    if isinstance(value, list):
        return "array"
    if isinstance(value, dict):
        return "object"
    raise ValidationError("cannot infer JSON Schema type for %r" % type(value))


def normalize_codex_output_schema(value: Any) -> Any:
    """Copy a schema and add only types implied by its const/enum constraints.

    OpenAI structured outputs rejects an otherwise valid schema when a
    property uses ``const`` without an explicit ``type``. The agent schema and
    the real gate remain the source of truth; this normalized copy exists only
    for ``codex exec --output-schema``.
    """
    if isinstance(value, list):
        return [normalize_codex_output_schema(item) for item in value]
    if not isinstance(value, dict):
        return value

    normalized = {
        key: normalize_codex_output_schema(item) for key, item in value.items()
    }
    if "type" in normalized:
        return normalized

    inferred: list[str] = []
    if "const" in normalized:
        inferred = [json_schema_type(normalized["const"])]
    elif isinstance(normalized.get("enum"), list) and normalized["enum"]:
        inferred = list(dict.fromkeys(
            json_schema_type(item) for item in normalized["enum"]
        ))
        if "number" in inferred and set(inferred) <= {"integer", "number"}:
            inferred = ["number"]

    if len(inferred) == 1:
        normalized["type"] = inferred[0]
    elif inferred:
        order = ("null", "boolean", "integer", "number", "string", "array", "object")
        normalized["type"] = [name for name in order if name in inferred]
    return normalized


def validate_codex_schema_normalizer() -> None:
    """Offline regression: normalization is recursive and non-mutating."""
    original = load_json(VERDICT_SCHEMA)
    before = json.dumps(original, ensure_ascii=False, sort_keys=True)
    normalized = normalize_codex_output_schema(original)

    require(
        json.dumps(original, ensure_ascii=False, sort_keys=True) == before,
        "Codex schema normalization mutated the agent schema",
    )
    require(
        "type" not in original["properties"]["schemaVersion"],
        "the regression fixture no longer exercises a missing const type",
    )
    require(
        normalized["properties"]["schemaVersion"].get("type") == "string",
        "Codex schema normalization did not type schemaVersion as string",
    )

    probe = {
        "properties": {
            "flag": {"const": True},
            "count": {"enum": [1, 2]},
            "score": {"enum": [1, 2.5]},
            "nullableLabel": {"enum": ["ready", None]},
            "nested": {"items": {"const": "value"}},
        }
    }
    probe_before = json.dumps(probe, sort_keys=True)
    typed = normalize_codex_output_schema(probe)
    require(json.dumps(probe, sort_keys=True) == probe_before, "normalizer mutated probe")
    require(typed["properties"]["flag"]["type"] == "boolean", "const bool type")
    require(typed["properties"]["count"]["type"] == "integer", "enum integer type")
    require(typed["properties"]["score"]["type"] == "number", "numeric enum type")
    require(
        typed["properties"]["nullableLabel"]["type"] == ["null", "string"],
        "mixed enum type",
    )
    require(
        typed["properties"]["nested"]["items"]["type"] == "string",
        "recursive const type",
    )


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


def validate_regressions(path: Path) -> dict[str, Any]:
    corpus = load_json(path)
    require(isinstance(corpus, dict), "regression root must be an object")
    require(
        set(corpus) == {"name", "requirement", "cases"},
        "regression root must contain exactly name/requirement/cases",
    )
    require(
        isinstance(corpus["name"], str) and bool(corpus["name"].strip()),
        "regression name must be non-empty",
    )
    require(
        corpus["requirement"] == "F-QINST-R020",
        "regression corpus must trace to F-QINST-R020",
    )
    cases = corpus["cases"]
    require(
        isinstance(cases, list) and len(cases) == len(REGRESSION_LABELS),
        "regression corpus must contain exactly the eight required scenarios",
    )

    case_ids: set[str] = set()
    scenarios: set[str] = set()
    serialized_inputs: set[str] = set()
    for index, case in enumerate(cases):
        prefix = f"regressions[{index}]"
        require(
            isinstance(case, dict) and set(case) == REGRESSION_CASE_KEYS,
            f"{prefix} has invalid keys",
        )
        case_id = case["caseId"]
        require(
            isinstance(case_id, str) and bool(case_id.strip()),
            f"{prefix}.caseId must be non-empty",
        )
        require(case_id not in case_ids, f"{prefix} duplicate caseId")
        case_ids.add(case_id)

        scenario = case["scenario"]
        require(scenario in REGRESSION_LABELS, f"{prefix} has unknown scenario")
        require(scenario not in scenarios, f"{prefix} duplicate scenario")
        scenarios.add(scenario)

        payload = case["input"]
        require(
            isinstance(payload, dict) and set(payload) == REGRESSION_INPUT_KEYS,
            f"{prefix}.input must contain exactly the seven judge inputs",
        )
        for key in REGRESSION_INPUT_KEYS:
            require(isinstance(payload[key], str), f"{prefix}.input.{key} must be text")
            if key != "options":
                require(bool(payload[key].strip()), f"{prefix}.input.{key} must be non-empty")
        require(
            re.search(r"^Hueco 1: .+", payload["acceptedAnswers"], re.MULTILINE) is not None,
            f"{prefix}.input.acceptedAnswers must use the rendered gap format",
        )
        serialized = json.dumps(payload, ensure_ascii=False, sort_keys=True)
        require(serialized not in serialized_inputs, f"{prefix} duplicate judge input")
        serialized_inputs.add(serialized)

        excluded = case["excludedContext"]
        require(isinstance(excluded, dict), f"{prefix}.excludedContext must be an object")
        if scenario == "excluded-wrong-mini-theory":
            require(
                set(excluded) == {"miniTheory"}
                and isinstance(excluded["miniTheory"], str)
                and bool(excluded["miniTheory"].strip()),
                f"{prefix} must document the excluded miniTheory",
            )
        else:
            require(not excluded, f"{prefix}.excludedContext must be empty")
        require(
            REGRESSION_INPUT_KEYS.isdisjoint(excluded),
            f"{prefix}.excludedContext overlaps a judge input",
        )

        expected = case["expected"]
        require(
            isinstance(expected, dict)
            and set(expected) == {"followsInstructions", "rationale"}
            and expected["followsInstructions"] is REGRESSION_LABELS[scenario]
            and isinstance(expected["rationale"], str)
            and bool(expected["rationale"].strip()),
            f"{prefix}.expected disagrees with the required semantic label",
        )

    require(
        scenarios == set(REGRESSION_LABELS),
        "regression corpus is missing a required semantic scenario",
    )
    return corpus


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

        # Sometimes the filler key has no JSON value at all ("status_": undefined). The
        # gate reads it as null and drops the key; the word inside a text stays.
        undefined = sample_pass_verdict(quiz["id"])
        undefined["checkedConstraints"][0]["evidence"] = "dice undefined adentro del texto"
        raw = json.dumps(undefined, ensure_ascii=False, indent=2).replace(
            '"status": "passed",', '"status": "passed",\n      "status_": undefined,', 1
        )
        verdict_path.write_text(raw, encoding="utf-8")
        gate_output = run_script(
            AGENT_DIR / "scripts" / "gate_verdict.sh", state
        )
        require(
            gate_output.get("route") == "passed",
            "a filler key with undefined failed the gate",
        )
        canonical = load_json(run_dir / "validated-verdict.json")
        validate_verdict(canonical, quiz["id"])
        require(
            canonical["checkedConstraints"][0]["evidence"]
            == "dice undefined adentro del texto",
            "the gate touched undefined inside a text",
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


def prompt_parts() -> tuple[str, str]:
    """Read the real evaluator system prompt and user template."""
    text = EVALUATE_NODE.read_text(encoding="utf-8")
    match = re.match(r"\A---\n.*?\n---\n?(.*)\Z", text, re.DOTALL)
    require(match is not None, "evaluate.md has no valid frontmatter")
    body = match.group(1)
    sections = re.split(
        r"^# User prompt template\s*$", body, maxsplit=1, flags=re.MULTILINE
    )
    require(len(sections) == 2, "evaluate.md has no user prompt template")
    system_sections = re.split(
        r"^# System prompt\s*$", sections[0], maxsplit=1, flags=re.MULTILINE
    )
    require(len(system_sections) == 2, "evaluate.md has no system prompt")
    system = system_sections[1].strip()
    template = sections[1].strip()
    placeholders = re.findall(r"\{([A-Za-z_][\w.-]*)\}", template)
    require(
        sorted(placeholders) == sorted(REGRESSION_INPUT_KEYS),
        "evaluate.md must use exactly the seven judge inputs",
    )
    return system, template


def render_user_prompt(template: str, payload: dict[str, str]) -> str:
    require(
        set(payload) == REGRESSION_INPUT_KEYS,
        "Codex payload must contain exactly the seven judge inputs",
    )
    rendered = template
    for key in REGRESSION_INPUT_KEYS:
        rendered = rendered.replace("{%s}" % key, payload[key])
    require(
        not re.search(r"\{([A-Za-z_][\w.-]*)\}", rendered),
        "unresolved placeholder in the rendered user prompt",
    )
    return rendered


def codex_error_tail(stderr: str, stdout: str) -> str:
    """Keep the useful end of Codex diagnostics without persisting credentials."""
    detail = stderr if stderr and stderr.strip() else stdout
    detail = (detail or "no output").strip()
    detail = re.sub(r"\x1b\[[0-?]*[ -/]*[@-~]", "", detail)
    detail = re.sub(
        r"(?i)(\bauthorization\s*:\s*bearer\s+)\S+",
        r"\1[REDACTED]",
        detail,
    )
    detail = re.sub(r"\bsk-[A-Za-z0-9_-]{8,}\b", "[REDACTED]", detail)
    detail = re.sub(
        r"(?i)(\b(?:OPENAI|CODEX|ANTHROPIC)?_?(?:API_?KEY|AUTH_?TOKEN|ACCESS_?TOKEN)\b\s*[:=]\s*)\S+",
        r"\1[REDACTED]",
        detail,
    )
    detail = "".join(
        char if char in "\n\t" or ord(char) >= 32 else " " for char in detail
    ).strip()
    if len(detail) > CODEX_ERROR_TAIL_CHARS:
        detail = "… [keeping the end of the diagnostic]\n" + detail[-CODEX_ERROR_TAIL_CHARS:]
    return detail


def ask_codex(
    system: str,
    user: str,
    model: str,
    reasoning_effort: str,
    timeout_seconds: float,
) -> str:
    """Run one isolated Codex turn and return its captured final message."""
    prompt = "# System prompt\n\n%s\n\n# User\n\n%s\n" % (system, user)
    with tempfile.TemporaryDirectory(prefix="quiz-instruction-codex-") as temp:
        temp_dir = Path(temp)
        output_path = temp_dir / "last-message.json"
        codex_schema_path = temp_dir / "output-schema.json"
        codex_schema_path.write_text(
            json.dumps(
                normalize_codex_output_schema(load_json(VERDICT_SCHEMA)),
                ensure_ascii=False,
                indent=2,
            ) + "\n",
            encoding="utf-8",
        )
        command = [
            "codex",
            "exec",
            "--model",
            model,
            "--sandbox",
            "read-only",
            "--ephemeral",
            "--skip-git-repo-check",
            "--cd",
            str(temp_dir),
            "--output-schema",
            str(codex_schema_path),
            "--output-last-message",
            str(output_path),
            "--color",
            "never",
            "--config",
            'model_reasoning_effort="%s"' % reasoning_effort,
            "-",
        ]
        completed = subprocess.run(
            command,
            input=prompt,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
            check=False,
        )
        if completed.returncode != 0:
            detail = codex_error_tail(completed.stderr, completed.stdout)
            raise ValidationError(
                "codex exec failed with exit %d: %s"
                % (completed.returncode, detail)
            )
        try:
            output = output_path.read_text(encoding="utf-8").strip()
        except OSError as exc:
            raise ValidationError(
                "codex exec did not leave its final message: %s" % exc
            ) from exc
        require(bool(output), "codex exec left an empty final message")
        return output


def gate_live_output(output: str) -> dict[str, Any]:
    """Pass one model response through the judge's real gate."""
    with tempfile.TemporaryDirectory(prefix="quiz-instruction-live-gate-") as temp:
        run_dir = Path(temp)
        (run_dir / "verdicts.json").write_text(output, encoding="utf-8")
        state = {
            "inputs": {"run_dir": str(run_dir)},
            "data": {},
            "node": {},
            "run_id": "codex-live-eval",
        }
        gate_output = run_script(AGENT_DIR / "scripts" / "gate_verdict.sh", state)
        require(
            gate_output.get("route") == "passed",
            "real gate rejected the response: %s" % gate_output.get("error", "unknown"),
        )
        return validate_verdict(load_json(run_dir / "validated-verdict.json"), "")


def choice_is_correct(item: dict[str, Any]) -> bool:
    """Read both numeric incidences and the course's Mongo Extended JSON."""
    incidence = item.get("incidence")
    if isinstance(incidence, dict):
        incidence = incidence.get("$numberDouble")
    return float(incidence or 0) > 0


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
    items = []
    if form.get("kind") == "MULTIPLE_CHOICE":
        # The course form is authoritative, including an explicitly empty list.
        # Nested multipleChoice belongs only to older internal snapshots.
        items = (form.get("items") if "items" in form
                 else (form.get("multipleChoice") or {}).get("items")) or []
    if items:
        correct = [item.get("label") for item in items if choice_is_correct(item)][:1]
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
            "- %s%s" % (item.get("label") or "", " (correcta)" if choice_is_correct(item) else "")
            for item in items),
    }


def run_payload(
    command: list[str], payload: dict[str, str], case_id: str, timeout_seconds: float
) -> dict[str, Any]:
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
            "agent exhausted structured-output retries for case %s" % case_id
        )
    return verdict


def run_command(
    command: list[str], case: dict[str, Any], timeout_seconds: float
) -> dict[str, Any]:
    return run_payload(
        command,
        student_view_inputs(case),
        case["quiz"]["id"],
        timeout_seconds,
    )


def run_regression_command(
    command: list[str], case: dict[str, Any], timeout_seconds: float
) -> dict[str, Any]:
    # Copy only the declared judge input. In particular, excludedContext is
    # corpus documentation and must never reach the command under evaluation.
    payload = dict(case["input"])
    require(
        set(payload) == REGRESSION_INPUT_KEYS,
        "regression payload leaked context outside the seven judge inputs",
    )
    return run_payload(command, payload, case["caseId"], timeout_seconds)


def resolve_results_path(raw: str) -> Path:
    path = Path(raw).expanduser()
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    path = path.resolve()
    try:
        path.relative_to(PROJECT_ROOT)
    except ValueError as exc:
        raise ValidationError(
            "--results must stay inside %s" % PROJECT_ROOT
        ) from exc
    require(not path.exists(), "--results already exists; refusing overwrite: %s" % path)
    return path


def write_codex_results(
    path: Path,
    args: argparse.Namespace,
    selected: list[tuple[str, dict[str, str], bool]],
    records: list[dict[str, Any]],
    matches: int,
    failures: int,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    document = {
        "schemaVersion": "1.0",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "agent": AGENT_DIR.name,
        "provider": "openai-codex",
        "model": args.model,
        "reasoningEffort": args.reasoning_effort,
        "dataset": args.dataset,
        "requestedMax": args.max,
        "caseIds": [case_id for case_id, _, _ in selected],
        "summary": {
            "total": len(records),
            "passed": matches,
            "mismatched": len(records) - matches - failures,
            "gateOrRuntimeErrors": failures,
        },
        "cases": records,
    }
    with path.open("x", encoding="utf-8") as handle:
        json.dump(document, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def codex_cases(
    fixture: dict[str, Any],
    regressions: dict[str, Any],
    dataset: str,
) -> list[tuple[str, dict[str, str], bool]]:
    """Return only ids, seven-input payloads and labels; never rationales."""
    selected: list[tuple[str, dict[str, str], bool]] = []
    if dataset in {"fixtures", "all"}:
        for case in fixture["cases"] + fixture["invented"]:
            selected.append(
                (
                    case["caseId"],
                    student_view_inputs(case),
                    case["expected"]["followsInstructions"],
                )
            )
    if dataset in {"regressions", "all"}:
        for case in regressions["cases"]:
            payload = dict(case["input"])
            require(
                set(payload) == REGRESSION_INPUT_KEYS,
                "regression payload leaked context outside the seven judge inputs",
            )
            selected.append(
                (case["caseId"], payload, case["expected"]["followsInstructions"])
            )
    return selected


def run_codex_eval(
    args: argparse.Namespace,
    fixture: dict[str, Any],
    regressions: dict[str, Any],
) -> int:
    system, template = prompt_parts()
    selected = codex_cases(fixture, regressions, args.dataset)
    if args.cases:
        wanted = [item.strip() for item in args.cases.split(",") if item.strip()]
        known = {case_id: (case_id, payload, expected)
                 for case_id, payload, expected in selected}
        missing = [case_id for case_id in wanted if case_id not in known]
        require(not missing, "unknown caseIds for selected dataset: %s" % missing)
        selected = [known[case_id] for case_id in wanted]
    selected = selected[: args.max]

    print(
        "codex: %d live cases (dataset=%s, max=%d, model=%s, effort=%s)"
        % (len(selected), args.dataset, args.max, args.model, args.reasoning_effort)
    )
    matches = 0
    failures = 0
    records: list[dict[str, Any]] = []
    for case_id, payload, expected in selected:
        try:
            user = render_user_prompt(template, payload)
            output = ask_codex(
                system,
                user,
                args.model,
                args.reasoning_effort,
                args.timeout,
            )
            verdict = gate_live_output(output)
            actual = verdict["followsInstructions"]
            matched = actual == expected
            matches += int(matched)
            marker = "PASS" if matched else "MISMATCH"
            print(
                "%s %s expected=%s actual=%s confidence=%.2f"
                % (marker, case_id, str(expected).lower(), str(actual).lower(),
                   verdict["confidence"])
            )
            records.append(
                {
                    "caseId": case_id,
                    "expected": expected,
                    "actual": actual,
                    "passed": matched,
                    "verdict": verdict,
                    "gateError": None,
                }
            )
        except (OSError, json.JSONDecodeError, subprocess.TimeoutExpired, ValidationError) as exc:
            failures += 1
            print("ERROR %s: %s" % (case_id, exc))
            records.append(
                {
                    "caseId": case_id,
                    "expected": expected,
                    "actual": None,
                    "passed": False,
                    "verdict": None,
                    "gateError": str(exc),
                }
            )
    print(
        "codex: %d/%d matched; %d without a usable gated verdict"
        % (matches, len(selected) - failures, failures)
    )
    write_codex_results(args.results, args, selected, records, matches, failures)
    print("results: %s" % args.results)
    return 1 if matches != len(selected) or failures else 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--fixtures", type=Path, default=DEFAULT_FIXTURES)
    parser.add_argument("--regressions", type=Path, default=DEFAULT_REGRESSIONS)
    parser.add_argument("--timeout", type=float, default=120.0)
    parser.add_argument(
        "--codex", action="store_true", help="run a capped live eval with codex exec"
    )
    parser.add_argument(
        "--max", type=int, default=None, help="required live-query cap for --codex"
    )
    parser.add_argument(
        "--dataset",
        choices=("fixtures", "regressions", "all"),
        default="all",
        help="corpus scored by --codex (default: %(default)s)",
    )
    parser.add_argument(
        "--cases", default=None, help="comma-separated caseIds within --dataset"
    )
    parser.add_argument(
        "--results",
        default=None,
        help="new JSON path inside content-audit; required with --codex",
    )
    parser.add_argument(
        "--model", default=DEFAULT_CODEX_MODEL,
        help="Codex model (default: %(default)s)",
    )
    parser.add_argument(
        "--reasoning-effort",
        choices=("low", "medium", "high", "xhigh", "max", "ultra"),
        default=DEFAULT_REASONING_EFFORT,
        help="Codex reasoning effort (default: %(default)s)",
    )
    parser.add_argument(
        "--command",
        nargs=argparse.REMAINDER,
        help="External agent CLI and arguments; receives one case on stdin.",
    )
    args = parser.parse_args()
    if args.codex and args.command:
        parser.error("--codex and --command are mutually exclusive")
    if args.codex and (args.max is None or args.max <= 0):
        parser.error("--codex requires --max N, with N greater than zero")
    if args.codex and not args.results:
        parser.error("--codex requires --results <file.json>")
    if args.results and not args.codex:
        parser.error("--results is only valid with --codex")
    if args.codex:
        try:
            args.results = resolve_results_path(args.results)
        except ValidationError as exc:
            parser.error(str(exc))
    return args


def main() -> int:
    args = parse_args()
    try:
        fixture = validate_fixture(args.fixtures)
        regressions = validate_regressions(args.regressions)
        schema = load_json(VERDICT_SCHEMA)
        require(
            schema.get("additionalProperties") is False
            and set(schema.get("required", [])) == TOP_KEYS,
            "JSON schema is not closed or disagrees with required keys",
        )
        validate_codex_schema_normalizer()
        fails = sorted(EXPECTED_FAIL_IDS)
        print(
            "fixture: OK (10 real quizzes, %d pass, %d fail%s; %d invented, all fail)"
            % (10 - len(fails), len(fails), (": " + ", ".join(fails)) if fails else "",
               len(fixture["invented"]))
        )
        regression_passes = sum(
            case["expected"]["followsInstructions"] for case in regressions["cases"]
        )
        print(
            "regressions: OK (F-QINST-R020, %d cases, %d pass, %d fail; "
            "seven-input boundary validated)"
            % (
                len(regressions["cases"]),
                regression_passes,
                len(regressions["cases"]) - regression_passes,
            )
        )
        print("schema: OK (closed verdict contract)")
        print("codex schema: OK (temporary inferred types; agent schema unchanged)")
        validate_scripts(fixture)
        print(
            "scripts: OK (state.json UUID resolution, ambiguity rejection, "
            "gate pass/fail, empty extra keys dropped, dangling commas and undefined read, filler dropped, "
            "emit fallback)"
        )

        if args.codex:
            return run_codex_eval(args, fixture, regressions)

        if not args.command:
            print(
                "agent: SKIPPED (use --codex --max N, or pass --command <cli ...>)"
            )
            return 0

        results: list[tuple[str, bool, dict[str, Any]]] = []
        for case in fixture["cases"] + fixture["invented"]:
            verdict = run_command(args.command, case, args.timeout)
            expected = case["expected"]["followsInstructions"]
            results.append((case["quiz"]["id"], expected, verdict))
        for case in regressions["cases"]:
            verdict = run_regression_command(args.command, case, args.timeout)
            expected = case["expected"]["followsInstructions"]
            results.append((case["caseId"], expected, verdict))

        mismatches = 0
        for case_id, expected, verdict in results:
            actual = verdict["followsInstructions"]
            marker = "PASS" if actual == expected else "MISMATCH"
            mismatches += int(actual != expected)
            print(
                "%s %s expected=%s actual=%s confidence=%.2f"
                % (
                    marker,
                    case_id,
                    str(expected).lower(),
                    str(actual).lower(),
                    verdict["confidence"],
                )
            )
        print("agent: %d/%d matched" % (len(results) - mismatches, len(results)))
        return 1 if mismatches else 0
    except (OSError, json.JSONDecodeError, subprocess.TimeoutExpired, ValidationError) as exc:
        print("ERROR: %s" % exc, file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
