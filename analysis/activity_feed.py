#!/usr/bin/env python3
"""Construye la fuente liviana que consume la pantalla Actividad.

Los informes completos de content-audit pesan decenas de MB. La UI no debe
recorrerlos ni interpretar logs: este script reúne únicamente la cobertura más
reciente de cada juez y los hitos explícitos de las corridas.
"""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


ANALYZER_ORDER = {
    "quiz-instruction": 0,
    "multiple-choice-validity": 1,
    "translation-sense": 2,
    "single-answer": 3,
    "mini-theory": 4,
    "naturalness": 5,
    "near-duplicates": 6,
}


def _read_object(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as source:
        value = json.load(source)
    if not isinstance(value, dict):
        raise ValueError(f"{path} no contiene un objeto JSON")
    return value


def latest_coverages(digest_directory: Path) -> list[dict[str, Any]]:
    """Toma la aparición más nueva de cada juez, no el último audit entero."""
    latest: dict[str, dict[str, Any]] = {}
    if not digest_directory.is_dir():
        return []

    for path in sorted(digest_directory.glob("audit-*.json")):
        digest = _read_object(path)
        audit_id = str(digest.get("auditId") or path.stem.removeprefix("audit-"))
        for coverage in digest.get("judgeCoverages") or []:
            if not isinstance(coverage, dict):
                continue
            analyzer = str(coverage.get("judgeName") or "").strip()
            if not analyzer:
                continue
            standing = coverage.get("standing") or {}
            runtime = standing.get("runtimeIdentity") or {}
            latest[analyzer] = {
                "analyzer": analyzer,
                "unit": coverage.get("unit") or "QUIZ",
                "reached": int(coverage.get("reached") or 0),
                "evaluated": int(coverage.get("evaluated") or 0),
                "pending": int(coverage.get("notEvaluated") or 0),
                "examStatus": standing.get("status") or "UNKNOWN",
                "judgeVersion": standing.get("judgeVersion"),
                "provider": runtime.get("providerName"),
                "model": runtime.get("modelName"),
                "reasoningEffort": runtime.get("reasoningEffort"),
                "sourceAuditId": audit_id,
            }

    return sorted(
        latest.values(),
        key=lambda item: (ANALYZER_ORDER.get(item["analyzer"], 999), item["analyzer"]),
    )


def load_runs(directories: Iterable[Path]) -> list[dict[str, Any]]:
    by_id: dict[str, dict[str, Any]] = {}
    for directory in directories:
        if not directory.is_dir():
            continue
        for path in sorted(directory.glob("*.json")):
            run = _read_object(path)
            run_id = str(run.get("id") or "").strip()
            if not run_id:
                raise ValueError(f"{path} no declara id")
            by_id[run_id] = run
    return sorted(
        by_id.values(),
        key=lambda run: str(
            run.get("updatedAt") or run.get("finishedAt") or run.get("startedAt") or ""
        ),
        reverse=True,
    )


def build_feed(workdir: Path, run_directories: Iterable[Path]) -> dict[str, Any]:
    activity_runs = workdir / ".content-audit" / "activity" / "runs"
    directories = [*run_directories, activity_runs]
    runs = load_runs(directories)
    current = next((run["id"] for run in runs if run.get("status") == "RUNNING"), None)
    return {
        "schemaVersion": 1,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
        "currentRunId": current,
        "coverage": latest_coverages(workdir / ".content-audit" / "audit-digests"),
        "runs": runs,
    }


def write_feed(feed: dict[str, Any], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(feed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    os.replace(temporary, output)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workdir", type=Path, required=True)
    parser.add_argument("--runs-dir", type=Path, action="append", default=[])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    output = args.output or args.workdir / ".content-audit" / "activity" / "activity.json"
    write_feed(build_feed(args.workdir, args.runs_dir), output)
    print(output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
