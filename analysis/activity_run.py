#!/usr/bin/env python3
"""Publica el ciclo de vida de una corrida en la pantalla Actividad.

El proceso técnico y la aprobación semántica son cosas distintas: una corrida
que termina con exit code 0 queda en NEEDS_REVIEW hasta que el operador publica
un resultado explícito.
"""

from __future__ import annotations

import argparse
import fcntl
import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from contextlib import contextmanager

from activity_feed import build_feed, write_feed


VALID_STATUSES = {
    "RUNNING",
    "PASSED",
    "FAILED",
    "NEEDS_REVIEW",
    "WAITING",
    "CANCELLED",
}


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _safe_id(value: str) -> str:
    safe = re.sub(r"[^A-Za-z0-9._-]+", "-", value.strip()).strip("-.")
    if not safe:
        raise ValueError("el id de actividad quedó vacío")
    return safe


def _record_path(workdir: Path, run_id: str) -> Path:
    return workdir / ".content-audit" / "activity" / "runs" / f"{_safe_id(run_id)}.json"


def _write_object(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    os.replace(temporary, path)


def _read_object(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as source:
        value = json.load(source)
    if not isinstance(value, dict):
        raise ValueError(f"{path} no contiene un objeto JSON")
    return value


def _publish(workdir: Path) -> None:
    output = workdir / ".content-audit" / "activity" / "activity.json"
    write_feed(build_feed(workdir, []), output)


@contextmanager
def _activity_lock(workdir: Path):
    path = workdir / ".content-audit" / "activity" / "update.lock"
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        yield


def process_is_alive(process_pid: int | None) -> bool | None:
    if process_pid is None:
        return None
    try:
        os.kill(process_pid, 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True


def _baseline(args: argparse.Namespace) -> dict[str, int] | None:
    values = {
        "queries": getattr(args, "baseline_queries", None),
        "tasksAttempted": getattr(args, "baseline_tasks_attempted", None),
        "proposals": getattr(args, "baseline_proposals", None),
        "resolved": getattr(args, "baseline_resolved", None),
        "noWinner": getattr(args, "baseline_no_winner", None),
    }
    present = {key: max(0, int(value)) for key, value in values.items() if value is not None}
    return present or None


def start(args: argparse.Namespace) -> Path:
    with _activity_lock(args.workdir):
        return _start_locked(args)


def _start_locked(args: argparse.Namespace) -> Path:
    now = _now()
    batch_id = getattr(args, "batch_id", None)
    instruction_model = getattr(args, "instruction_model", None)
    distinct_instruction_model = (
        instruction_model
        if instruction_model and instruction_model != getattr(args, "model", None)
        else None
    )
    progress = None
    if args.total is not None:
        progress = {
            "completed": 0,
            "total": args.total,
            "unit": args.unit,
            "totalKind": getattr(args, "total_kind", "EXACT"),
        }
    record = {
        "id": args.id,
        "kind": args.kind,
        "batchId": batch_id,
        "title": args.title,
        "scope": args.scope,
        "status": "RUNNING",
        "startedAt": now,
        "updatedAt": now,
        "provider": args.provider,
        "model": args.model,
        "instructionModel": distinct_instruction_model,
        "reasoningEffort": args.reasoning_effort,
        "version": args.version,
        "progress": progress,
        "baseline": _baseline(args),
        "summary": args.summary or "La corrida está en curso.",
        "next": None,
        "stages": [],
        "facts": (
            [
                {
                    "label": "Modelo de consigna",
                    "value": distinct_instruction_model,
                    "tone": "NEUTRAL",
                }
            ]
            if distinct_instruction_model
            else []
        ),
        "artifacts": args.artifact,
    }
    path = _record_path(args.workdir, args.id)
    _write_object(path, {key: value for key, value in record.items() if value is not None})
    _publish(args.workdir)
    return path


def update(args: argparse.Namespace) -> Path:
    with _activity_lock(args.workdir):
        return _update_locked(args)


def _update_locked(args: argparse.Namespace) -> Path:
    path = _record_path(args.workdir, args.id)
    record = _read_object(path)
    # A delayed heartbeat must never resurrect a finished/cancelled run.
    if getattr(args, "only_if_running", False) and record.get("status") != "RUNNING":
        return path
    if args.status == "RUNNING" and record.get("finishedAt"):
        return path
    old_completed = (record.get("progress") or {}).get("completed", 0)
    now = _now()
    if args.status is not None:
        status = args.status.upper()
        if status not in VALID_STATUSES:
            raise ValueError(f"estado de actividad desconocido: {status}")
        record["status"] = status
    if args.completed is not None:
        progress = record.get("progress")
        if not isinstance(progress, dict):
            progress = {"completed": 0, "total": args.completed, "unit": "pasos"}
            record["progress"] = progress
        total = int(progress.get("total") or args.completed)
        progress["completed"] = max(0, min(args.completed, total))
        if progress["completed"] > old_completed:
            record["lastProgressAt"] = now
    progress_unit = getattr(args, "progress_unit", None)
    if progress_unit and isinstance(record.get("progress"), dict):
        record["progress"]["unit"] = progress_unit
    last_progress_at = getattr(args, "last_progress_at", None)
    if last_progress_at is not None:
        record["lastProgressAt"] = last_progress_at
    process_pid = getattr(args, "process_pid", None)
    if process_pid is not None:
        record["processPid"] = process_pid
    progress_estimate = getattr(args, "progress_estimate", None)
    if progress_estimate is not None:
        progress = record.get("progress")
        if isinstance(progress, dict):
            if progress_estimate:
                progress["estimate"] = progress_estimate
            else:
                progress.pop("estimate", None)
    if args.summary is not None:
        record["summary"] = args.summary
    if args.next is not None:
        record["next"] = args.next
    stages = getattr(args, "stages", None)
    if stages is not None:
        record["stages"] = stages
    facts = getattr(args, "facts", None)
    if facts is not None:
        record["facts"] = facts
    baseline = _baseline(args)
    if baseline is not None:
        record["baseline"] = {**record.get("baseline", {}), **baseline}
    if args.finish:
        record["finishedAt"] = now
    record["updatedAt"] = now
    _write_object(path, record)
    _publish(args.workdir)
    return path


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    commands = root.add_subparsers(dest="command", required=True)

    begin = commands.add_parser("start")
    begin.add_argument("--workdir", type=Path, required=True)
    begin.add_argument("--id", required=True)
    begin.add_argument("--title", required=True)
    begin.add_argument("--scope", default="Proceso de content-audit")
    begin.add_argument("--kind", default="COURSE_VALIDATION")
    begin.add_argument("--batch-id")
    begin.add_argument("--provider")
    begin.add_argument("--model")
    begin.add_argument("--instruction-model")
    begin.add_argument("--reasoning-effort")
    begin.add_argument("--version")
    begin.add_argument("--total", type=int)
    begin.add_argument("--unit", default="consultas")
    begin.add_argument("--total-kind", choices=("EXACT", "CEILING"), default="EXACT")
    begin.add_argument("--summary")
    begin.add_argument("--artifact", action="append", default=[])
    _add_baseline_arguments(begin)
    begin.set_defaults(action=start)

    change = commands.add_parser("update")
    change.add_argument("--workdir", type=Path, required=True)
    change.add_argument("--id", required=True)
    change.add_argument("--status")
    change.add_argument("--completed", type=int)
    change.add_argument("--progress-unit")
    change.add_argument("--summary")
    change.add_argument("--next")
    change.add_argument("--finish", action="store_true")
    _add_baseline_arguments(change)
    change.set_defaults(action=update)
    return root


def _add_baseline_arguments(command: argparse.ArgumentParser) -> None:
    command.add_argument("--baseline-queries", type=int)
    command.add_argument("--baseline-tasks-attempted", type=int)
    command.add_argument("--baseline-proposals", type=int)
    command.add_argument("--baseline-resolved", type=int)
    command.add_argument("--baseline-no-winner", type=int)


def main() -> int:
    args = parser().parse_args()
    print(args.action(args))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
