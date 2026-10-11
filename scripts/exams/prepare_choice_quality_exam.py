#!/usr/bin/env python3
"""Prepare the frozen v2 exam and recover its original exam-only records.

No model, registry, course campaign or historical experiment is written. The
official CLI must still run with budget zero and measure/register its result.
"""
import argparse
import hashlib
import json
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "analysis/judge-exams/2026-10-09-choice-correction-cycle-v2"
MEMBER = ("analysis/judge-exams/2026-10-09-choice-correction-cycle-v2/ledger/"
          ".content-audit/evaluations/exam_choice-quality-judge.jsonl")
IDENTITY = {"providerName": "codex-cli", "modelName": "gpt-6.1-sol",
            "reasoningEffort": "medium", "runtimeKind": "EXPLICIT"}


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2)
        stream.write("\n")


def oid(value):
    return {"$oid": value}


def node(identifier, kind, label, parent=None, **fields):
    result = {"_id": oid(identifier), "code": identifier, "oldId": identifier,
              "kind": kind, "label": label, **fields}
    if parent is not None:
        result["parentId"] = oid(parent)
    return result


def recover(source):
    """Return only the 16 initial records, without changing a byte of them."""
    corpus = load(source / "cases.json")
    frozen = load(source / "preflight.json")
    digest = hashlib.sha256((source / "cases.json").read_bytes()).hexdigest()
    if digest != frozen["casesSha256"]:
        raise ValueError("The pre-paid corpus changed")
    cases = corpus["cases"]
    if len(cases) != 16 or len({case["id"] for case in cases}) != 16:
        raise ValueError("The exam requires 16 distinct frozen cases")
    with tarfile.open(source / "raw-evidence.tgz", "r:gz") as archive:
        raw = archive.extractfile(MEMBER).read().decode("utf-8")
    rows = [(line, json.loads(line)) for line in raw.splitlines() if line.strip()]
    selected = []
    for case in cases:
        initial = load(source / f'case-{case["caseNumber"]:02d}/initial.json')
        matches = [line for line, row in rows
                   if row.get("subjectRef") == case["id"]
                   and row.get("evaluatorId") == "exam:choice-quality-judge"
                   and row.get("evaluatorVersion") == frozen["qualityVersion"]
                   and all(row.get(key) == value for key, value in IDENTITY.items())
                   and json.loads(row["payload"]) == initial]
        if len(matches) != 1:
            raise ValueError(f'No unique original exam record for {case["id"]}')
        selected.append(matches[0])
    return corpus, frozen, selected


def write_photo(cases, photo):
    """Course-file adapter: only visible inputs; expected verdicts stay outside."""
    root_id = "da1000000000000000000000"
    levels = sorted({case["level"] for case in cases})
    level_ids = {level: f"da2{index:021x}" for index, level in enumerate(levels, 1)}
    knowledge_ids = []
    for case in cases:
        number = case["caseNumber"]
        topic_id, knowledge_id = f"da3{number:021x}", f"da4{number:021x}"
        knowledge_ids.append(oid(knowledge_id))
        topic_path = photo / case["level"].lower() / f"case-{number:02d}"
        save(topic_path / "_topic.json", node(
            topic_id, "TOPIC", case["topic"], level_ids[case["level"]],
            ruleIds=[oid(knowledge_id)], children=None))
        knowledge = topic_path / "exercise"
        save(knowledge / "_knowledge.json", node(
            knowledge_id, "KNOWLEDGE", case["topic"], topic_id,
            instructions=case["instructions"], isRule=True, sentenceMode="FILL"))
        sides = case["exercise"].split("___")
        if len(sides) != 2 or case["options"].count(case["correct"]) != 1:
            raise ValueError("A fixture must have one gap and one marked key")
        save(knowledge / "quizzes.json", [{
            "_id": oid(case["id"]), "id": case["id"], "kind": "MULTIPLE_CHOICE",
            "knowledgeId": oid(knowledge_id), "title": case["title"],
            "instructions": case["instructions"], "translation": "", "miniTheory": "",
            "form": {"kind": "MULTIPLE_CHOICE", "incidence": {"$numberDouble": "1.0"},
                     "label": "", "name": "", "selection": "SINGLE",
                     "sentenceParts": [
                         {"kind": "TEXT", "text": sides[0], "options": None},
                         {"kind": "CLOZE", "text": "", "options": None},
                         {"kind": "TEXT", "text": sides[1], "options": None}],
                     "items": [{"id": option, "label": option,
                                "incidence": {"$numberDouble": "1.0" if option == case["correct"] else "0.0"}}
                               for option in case["options"]]}}])
    for level in levels:
        topics = [oid(f'da3{case["caseNumber"]:021x}') for case in cases if case["level"] == level]
        save(photo / level.lower() / "_milestone.json", node(
            level_ids[level], "MILESTONE", level, root_id, children=topics))
    save(photo / "_course.json", {
        "_id": oid("da0000000000000000000000"), "title": "Examen sintético fijo de distractores v2",
        "knowledgeIds": knowledge_ids,
        "root": node(root_id, "ROOT", "", children=[oid(level_ids[level]) for level in levels])})


def prepare(source, output):
    corpus, frozen, records = recover(source)
    # Never merge into a campaign or overwrite an earlier attempt by accident.
    output.mkdir(parents=True, exist_ok=False)
    write_photo(corpus["cases"], output / "course")
    save(output / "cases.json", [{"ejercicio": case["id"]} for case in corpus["cases"]])
    ledger = output / "workdir/.content-audit/evaluations/exam_choice-quality-judge.jsonl"
    ledger.parent.mkdir(parents=True)
    with ledger.open("x", encoding="utf-8") as stream:
        stream.write("\n".join(records) + "\n")
    save(output / "preparation.json", {
        "source": str(source.resolve()), "sourceCasesSha256": frozen["casesSha256"],
        "sourceArchiveSha256": hashlib.sha256((source / "raw-evidence.tgz").read_bytes()).hexdigest(),
        "originalJudgeVersion": frozen["qualityVersion"], "runtime": IDENTITY,
        "recoveredInitialRecords": len(records), "newModelQueries": 0,
        "rubric": corpus["rubric"]["initial"],
        "historicalExamModified": False, "officiallyRegistered": False})
    return len(records)


def measure(source, verdicts):
    """Compare official exported results with the pre-paid, unchanged v2 rubric.

    Refuse incomplete/foreign/mixed identities. This only produces the measured
    file; approval still goes through the official service's TAKEN guard.
    """
    corpus, frozen, records = recover(source)
    rows = [json.loads(line) for line in verdicts.read_text(encoding="utf-8").splitlines() if line.strip()]
    expected_ids = {case["id"] for case in corpus["cases"]}
    if len(rows) != 16 or {row.get("ejercicio") for row in rows} != expected_ids:
        raise ValueError("Incomplete, duplicate or foreign exam cases")
    by_id = {row["ejercicio"]: row for row in rows}
    versions = {row.get("version") for row in rows}
    if versions != {frozen["qualityVersion"]}:
        raise ValueError("Missing, mixed or changed official process versions")
    groups = {"FAIL": {"grupo": "Defectos pedagógicos", "casos": 0, "comoSeEspera": 0, "noComoSeEspera": 0},
              "PASS": {"grupo": "Distractores útiles y práctica introductoria", "casos": 0, "comoSeEspera": 0, "noComoSeEspera": 0}}
    mismatches = []
    for case, raw in zip(corpus["cases"], records):
        row = by_id[case["id"]]
        if (row.get("juez") != "multiple-choice-quality"
                or (row.get("proveedor"), row.get("modelo"), row.get("esfuerzo"))
                != ("codex-cli", "gpt-6.1-sol", "medium")):
            raise ValueError("Foreign or unknown runtime identity")
        if row.get("resultado") not in ("PASSES", "FAILS"):
            raise ValueError("An unjudged case cannot count as an exam pass")
        original = json.loads(json.loads(raw)["payload"])
        actual_kinds = {kind.upper().replace("-", "_") for kind in row.get("reglas", [])}
        # Only original output may be reused, not a later repair's verdict.
        if actual_kinds != {issue["kind"] for issue in original["issues"]}:
            raise ValueError("Exported diagnosis differs from the original evidence")
        matches = row["resultado"] == {"PASS": "PASSES", "FAIL": "FAILS"}[case["expected"]]
        matches = matches and ((not actual_kinds and case["expected"] == "PASS")
                               or (bool(actual_kinds) and actual_kinds <= set(case["allowedIssueKinds"])
                                   and case["expected"] == "FAIL"))
        group = groups[case["expected"]]
        group["casos"] += 1
        group["comoSeEspera" if matches else "noComoSeEspera"] += 1
        if not matches:
            mismatches.append(case["id"])
    return {"juez": "multiple-choice-quality", "version": next(iter(versions)),
            "estado": "REPROBADO" if mismatches else "APROBADO",
            "proveedor": "codex-cli", "modelo": "gpt-6.1-sol", "esfuerzo": "medium",
            "grupos": list(groups.values()), "noDieronLoEsperado": mismatches,
            "alcanzoLaMeta": not mismatches,
            "medidoPor": "Comparación local de la exportación oficial con el corpus y la rúbrica v2 "
                          "congelados antes de la corrida del 9/10: 16/16 estados y categorías compatibles; "
                          "reutilización nativa con presupuesto cero, sin reinterpretar el examen v1.",
            "corpusSha256": frozen["casesSha256"], "newModelQueries": 0}


def recover_into(source, workdir):
    """Restore original records in the exam namespace, never in the course book.

    Existing books are refused, even if identical. This is recovery, not an
    approval: exam run with zero budget and exam record must follow separately.
    """
    _, _, records = recover(source)
    target = workdir / ".content-audit/evaluations/exam_choice-quality-judge.jsonl"
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("x", encoding="utf-8") as stream:
        stream.write("\n".join(records) + "\n")
    return len(records)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="New, isolated directory; must not exist")
    parser.add_argument("--source", type=Path, default=SOURCE)
    parser.add_argument("--measure", type=Path, help="Official verdict export to compare; output is a new JSON file")
    parser.add_argument("--recover-into", action="store_true", help="Output is a workdir: restore original exam records, never a registry")
    args = parser.parse_args()
    if args.measure and args.recover_into:
        parser.error("Choose measurement or recovery, not both")
    if args.recover_into:
        print(f"Restored {recover_into(args.source, args.output)} original exam records. No approval or model calls.")
    elif args.measure:
        result = measure(args.source, args.measure)
        save(args.output, result)
        print(f'Measured {sum(g["comoSeEspera"] for g in result["grupos"])}/16: {result["estado"]}. Not registered.')
    else:
        count = prepare(args.source, args.output)
        print(f"Prepared {count} original exam records. No model calls or exam approvals.")


if __name__ == "__main__":
    main()
