#!/usr/bin/env python3
"""Show durable selective-analysis progress without invoking models.

The selective run keeps its counters at zero until it finishes. Read its
campaign ledger instead, matching the evaluator version, runtime and start
time. These are saved evaluations, never a claim about confirmed spending.
The launcher remains responsible for the final status and spending.
"""
from __future__ import annotations

import argparse
import fcntl
import json
import os
import time
from argparse import Namespace
from datetime import datetime, timezone
from pathlib import Path

import activity_run
from monitor_analysis_activity import JUDGE_FILES, JUDGE_NAMES


FILES = {
    **JUDGE_FILES,
    "naturalness": ("native-usage-judge.jsonl",),
    "a1-didactic": ("a1-didactic-judge.jsonl",),
    "pragmatism": ("pragmatism-judge.jsonl",),
    "near-duplicates": ("near-duplicates-pair-judge.jsonl",),
}
NAMES = {**JUDGE_NAMES, "naturalness": "Naturalidad idiomática",
         "a1-didactic": "Calidad didáctica A1", "pragmatism": "Pragmatismo comunicativo",
         "near-duplicates": "Ejercicios parecidos"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def instant(value):
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        return parsed.replace(tzinfo=timezone.utc) if parsed.tzinfo is None else parsed
    except (ValueError, AttributeError, TypeError):
        return None


def snapshot(campaign, activity):
    batch = read(campaign / ".content-audit/batches" / (activity["batchId"] + ".json"))
    # A combined operation stores one child per state; the aggregate is not
    # another evaluator invocation. Deduplicate ledger identities across them.
    runs = [r for r in batch.get("selectiveRuns", []) if isinstance(r, dict)
            and (r.get("id") == activity["id"] or r.get("sourceRunId") == activity["id"])]
    started = instant(activity.get("startedAt"))
    if started is None:
        raise ValueError("La corrida no tiene una fecha inicial verificable.")
    runtime = {"runtimeKind": "EXPLICIT", "providerName": activity.get("provider"),
               "modelName": activity.get("model"), "reasoningEffort": activity.get("reasoningEffort")}
    identities = {}
    for run in runs:
        # Rewriting and judging have different runtimes and accounting. Do not
        # turn a corrector's output into selective-analysis progress.
        if run.get("action") not in {"analyze", "analyze-base", "reanalyze", "reanalyze-base", "reanalyze-both"}:
            continue
        run_started = max(started, instant(run.get("createdAt")) or started)
        for filename in FILES.get(run.get("analyzer"), ()):
            try:
                with (campaign / ".content-audit/evaluations" / filename).open("rb") as stream:
                    for line in stream:
                        if not line.endswith(b"\n"):
                            continue
                        try:
                            row = json.loads(line)
                            at = instant(row.get("recordedAt"))
                            if (at is None or at < run_started
                                    or not run.get("judgeVersion")
                                    or row.get("evaluatorVersion") != run["judgeVersion"]
                                    or any(not v or row.get(k) != v for k, v in runtime.items())
                                    or row.get("evaluatorId") != filename.removesuffix(".jsonl")
                                    or not row.get("contentFingerprint")
                                    or not isinstance(json.loads(row["payload"]), dict)):
                                continue
                            selected = run.get("quizIds") or []
                            if selected and row.get("subjectRef") not in selected:
                                continue
                            key = (row["evaluatorId"], row["contentFingerprint"])
                            identities[key] = max(identities.get(key, at), at)
                        except (KeyError, ValueError, TypeError, AttributeError, UnicodeDecodeError):
                            continue
            except FileNotFoundError:
                pass
    return len(identities), max(identities.values(), default=None)


def tick(args):
    activity = read(activity_run._record_path(args.workdir, args.activity_id))
    if activity.get("status") != "RUNNING" or activity.get("finishedAt"):
        return False
    if activity.get("kind") != "CONTENT_SELECTIVE_ANALYSIS":
        raise ValueError("El monitor sólo admite análisis selectivos.")
    if activity_run.process_is_alive(args.process_pid) is not True:
        return False
    completed, last = snapshot(args.campaign_workdir, activity)
    summary = f"{completed} evaluaciones nuevas guardadas."
    if last:
        age = (datetime.now(timezone.utc) - last).total_seconds()
        summary += (" El análisis sigue recibiendo respuestas." if age < 60 else
                    f" Sin otra evaluación guardada desde hace {int(age // 60)} min.")
    else:
        summary += " Esperando la primera evaluación guardada."
    activity_run.update(Namespace(
        workdir=args.workdir, id=args.activity_id, status="RUNNING", only_if_running=True,
        completed=completed, progress_unit="evaluaciones nuevas", last_progress_at=last.isoformat() if last else None,
        process_pid=args.process_pid, summary=summary,
        next="Cada evaluación válida se conserva. El gasto se confirma cuando termina la corrida.",
        stages=[{"label": "Evaluaciones guardadas", "status": "RUNNING", "result": summary}],
        facts=[{"label": "Contador", "value": "Evaluaciones nuevas guardadas; no es gasto confirmado", "tone": "NEUTRAL"}],
        finish=False))
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workdir", type=Path, required=True)
    parser.add_argument("--campaign-workdir", type=Path, required=True)
    parser.add_argument("--activity-id", required=True)
    parser.add_argument("--process-pid", type=int, required=True)
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args()
    path = args.workdir / ".content-audit/activity/monitors" / (args.activity_id + "-selective.lock")
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0
        lock.write(str(os.getpid())); lock.flush()
        while tick(args):
            if args.once:
                break
            time.sleep(5)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
