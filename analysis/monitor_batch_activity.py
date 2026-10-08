#!/usr/bin/env python3
"""Mantiene viva la telemetría de una operación sobre una tanda.

Este proceso sólo observa el JSON durable de la tanda y actualiza Actividad.
No ejecuta analizadores, jueces ni modelos. Vive separado del widget que lo
inició, por lo que navegar o volver a leer la app no corta el seguimiento.
"""

from __future__ import annotations

import argparse
import fcntl
import json
import os
import time
from argparse import Namespace
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import activity_run
from progress_estimate import ProgressEstimate, ProgressEstimator


@dataclass(frozen=True)
class BatchMetrics:
    status: str
    tasks: int
    tasks_attempted: int
    proposals: int
    queries: int
    resolved: int
    no_winner: int


def _objects(value: Any) -> list[dict[str, Any]]:
    return [item for item in value if isinstance(item, dict)] if isinstance(value, list) else []


def metrics(batch: dict[str, Any]) -> BatchMetrics:
    outcomes: dict[str, int] = {}
    for outcome in _objects(batch.get("outcomes")):
        kind = str(outcome.get("kind") or "")
        amount = int(outcome.get("tasks") or 1)
        outcomes[kind] = outcomes.get(kind, 0) + amount
    task_ids = batch.get("taskIds")
    tasks = len(task_ids) if isinstance(task_ids, list) else sum(outcomes.values())
    decisions = _objects(batch.get("panelDecisions"))
    return BatchMetrics(
        status=str(batch.get("status") or "").upper(),
        tasks=tasks,
        tasks_attempted=max(0, tasks - outcomes.get("NOT_ATTEMPTED", 0)),
        proposals=len(_objects(batch.get("quizProposals"))),
        queries=sum(max(0, int(item.get("queriesSpent") or 0)) for item in decisions),
        resolved=outcomes.get("RESOLVED_BY_RULE", 0) + outcomes.get("RESOLVED_BY_PANEL", 0),
        no_winner=outcomes.get("NO_WINNER", 0),
    )


def summary(current: BatchMetrics, args: argparse.Namespace) -> str:
    delta_tasks = max(0, current.tasks_attempted - args.baseline_tasks_attempted)
    delta_proposals = max(0, current.proposals - args.baseline_proposals)
    delta_queries = max(0, current.queries - args.baseline_queries)
    delta_resolved = current.resolved - args.baseline_resolved
    delta_no_winner = current.no_winner - args.baseline_no_winner
    return (
        f"Corrección en curso: +{delta_tasks} tareas trabajadas, "
        f"+{delta_proposals} quizzes con propuesta y {delta_queries} consultas nuevas "
        f"consolidadas. Total durable: {current.tasks_attempted} de {current.tasks} "
        f"tareas; {current.proposals} propuestas. "
        f"Resueltas: {delta_resolved:+d}; sin ganador: {delta_no_winner:+d}."
    )


def _read_object(path: Path) -> dict[str, Any] | None:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError, OSError):
        return None
    return value if isinstance(value, dict) else None


def _activity_is_running(workdir: Path, activity_id: str) -> bool:
    record = _read_object(
        workdir / ".content-audit" / "activity" / "runs" / f"{activity_id}.json"
    )
    return record is not None and str(record.get("status") or "").upper() == "RUNNING"


def _publish(
    current: BatchMetrics,
    args: argparse.Namespace,
    estimate: ProgressEstimate,
) -> None:
    activity_run.update(
        Namespace(
            workdir=args.workdir,
            id=args.activity_id,
            status="RUNNING",
            completed=max(0, current.queries - args.baseline_queries),
            summary=summary(current, args),
            next=(
                "Podés navegar o volver a leer: la ejecución sigue en segundo "
                "plano y conserva cada avance durable."
            ),
            finish=False,
            baseline_queries=None,
            baseline_tasks_attempted=None,
            baseline_proposals=None,
            baseline_resolved=None,
            baseline_no_winner=None,
            progress_estimate=estimate.as_json(),
        )
    )


def _finish(current: BatchMetrics, raw: dict[str, Any], args: argparse.Namespace) -> None:
    interrupted = current.status == "INTERRUPTED"
    reason = str(raw.get("interruptionReason") or "").strip()
    final_summary = summary(current, args)
    if interrupted:
        final_summary = (
            f"La corrección se interrumpió. {final_summary}"
            f"{' Motivo: ' + reason + '.' if reason else ''}"
        )
        next_step = "Corregir la causa y reanudar la misma Corrección."
    else:
        final_summary = f"La continuación terminó técnicamente. {final_summary}"
        next_step = "Revisar las propuestas y decidir si continuar o ejecutar el análisis."
    activity_run.update(
        Namespace(
            workdir=args.workdir,
            id=args.activity_id,
            status="FAILED" if interrupted else "NEEDS_REVIEW",
            completed=max(0, current.queries - args.baseline_queries),
            summary=final_summary,
            next=next_step,
            finish=True,
            baseline_queries=None,
            baseline_tasks_attempted=None,
            baseline_proposals=None,
            baseline_resolved=None,
            baseline_no_winner=None,
            progress_estimate={"status": "COMPLETE"},
        )
    )


def monitor(args: argparse.Namespace) -> int:
    batch = args.campaign_workdir / ".content-audit" / "batches" / f"{args.batch_id}.json"
    lock_path = (
        args.workdir
        / ".content-audit"
        / "activity"
        / "monitors"
        / f"{args.activity_id}.lock"
    )
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open("w", encoding="utf-8") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0
        lock.write(str(os.getpid()))
        lock.flush()

        seen_running = False
        missing_ticks = 0
        last_published: BatchMetrics | None = None
        last_estimate: ProgressEstimate | None = None
        last_heartbeat = 0.0
        estimator = ProgressEstimator()
        while _activity_is_running(args.workdir, args.activity_id):
            raw = _read_object(batch)
            if raw is None:
                missing_ticks += 1
                if missing_ticks >= args.max_missing_ticks:
                    return 1
                time.sleep(args.interval)
                continue
            missing_ticks = 0
            current = metrics(raw)
            if current.status == "RUNNING":
                seen_running = True
                now = time.time()
                estimate = estimator.observe(
                    max(0, current.queries - args.baseline_queries),
                    args.total,
                    now=now,
                )
                if (
                    current != last_published
                    or estimate != last_estimate
                    or now - last_heartbeat >= args.heartbeat
                ):
                    _publish(current, args, estimate)
                    last_published = current
                    last_estimate = estimate
                    last_heartbeat = now
            elif seen_running:
                if current.status in {"OPEN", "INTERRUPTED"}:
                    _finish(current, raw, args)
                return 0
            time.sleep(args.interval)
    return 0


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    root.add_argument("--workdir", type=Path, required=True)
    root.add_argument("--campaign-workdir", type=Path, required=True)
    root.add_argument("--batch-id", required=True)
    root.add_argument("--activity-id", required=True)
    root.add_argument("--baseline-queries", type=int, required=True)
    root.add_argument("--baseline-tasks-attempted", type=int, required=True)
    root.add_argument("--baseline-proposals", type=int, required=True)
    root.add_argument("--baseline-resolved", type=int, required=True)
    root.add_argument("--baseline-no-winner", type=int, required=True)
    root.add_argument("--total", type=int, required=True)
    root.add_argument("--interval", type=float, default=3.0)
    root.add_argument("--heartbeat", type=float, default=5.0)
    root.add_argument("--max-missing-ticks", type=int, default=20)
    return root


def main() -> int:
    return monitor(parser().parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
