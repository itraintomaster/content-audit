#!/usr/bin/env python3
"""Project durable results of one recorrection into its running activity.

Only exact operation/task attribution is used. No ledger payloads, provider
processes, global batch spending or model calls are consulted. The launcher
owns final reconciliation; this monitor only reports saved partial results.
"""
from __future__ import annotations

import argparse
import fcntl
import json
import os
import time
from argparse import Namespace
from pathlib import Path

import activity_run


KIND = "CONTENT_QUIZ_CORRECTION"
OUTCOMES = {"ACCEPTED", "TIE", "NO_WINNER", "NO_LONGER_HOLDS", "NOT_ATTEMPTED", "NO_DECLARED_SCOPE"}
QUERY_LABEL = "consultas confirmadas en resultados guardados"
POLL_SECONDS = 5


def read(path):
    with path.open(encoding="utf-8") as source:
        return json.load(source)


def ids(value):
    if not isinstance(value, list) or any(not isinstance(v, str) or not v for v in value):
        return set()
    return set(value)


def nonnegative(value):
    return value if isinstance(value, int) and not isinstance(value, bool) and value >= 0 else None


def summarize(batch, operation_id):
    """Keep only small attributed metadata, never retain candidate/verdict payloads."""
    raw_runs, raw_decisions = batch.get("selectiveRuns", []), batch.get("panelDecisions", [])
    if not isinstance(raw_runs, list) or not isinstance(raw_decisions, list):
        raise ValueError("La tanda no contiene listas válidas de pasadas y decisiones.")
    runs = [r for r in raw_runs if isinstance(r, dict)
            and r.get("id") == operation_id and r.get("action") == "recorrect"]
    if len(runs) != 1:
        return {"run": None, "reason": "Esperando un registro verificable de esta pasada."}
    run = runs[0]
    tasks, quizzes = ids(run.get("taskIds")), ids(run.get("quizIds"))
    decisions = {}
    for decision in raw_decisions:
        if not isinstance(decision, dict):
            continue
        own_tasks, own_quizzes = ids(decision.get("taskIds")), ids(decision.get("quizIds"))
        resolved = ids(decision.get("resolvedTaskIds", []))
        outcome = decision.get("outcome")
        if (not own_tasks or not own_tasks <= tasks or not own_quizzes or not own_quizzes <= quizzes
                or not resolved <= tasks or outcome not in OUTCOMES):
            continue
        # A persisted panel decision has no separate ID. Its task set, target,
        # outcome and attempt identify it; duplicate copies must not add cost.
        key = (tuple(sorted(own_tasks)), tuple(sorted(own_quizzes)), outcome,
               nonnegative(decision.get("attempts")), tuple(sorted(resolved)))
        queries = nonnegative(decision.get("queriesSpent"))
        previous = decisions.get(key)
        if previous is None or (queries is not None and (previous["queries"] is None or queries > previous["queries"])):
            decisions[key] = {"quizzes": own_quizzes, "outcome": outcome, "queries": queries}
    decided = set().union(*(d["quizzes"] for d in decisions.values())) if decisions else set()
    process = run.get("process")
    if process is not None and not isinstance(process, dict):
        raise ValueError("La pasada no identifica un proceso válido.")
    return {
        "run": {"id": operation_id, "status": run.get("status"), "finishedAt": run.get("finishedAt"),
                "processPid": (process or {}).get("pid")},
        "total": len(quizzes), "decided": len(decided), "remaining": len(quizzes - decided),
        "accepted": sum(d["outcome"] == "ACCEPTED" for d in decisions.values()),
        "ties": sum(d["outcome"] == "TIE" for d in decisions.values()),
        "confirmed": sum(d["queries"] or 0 for d in decisions.values()),
        "withoutCost": sum(d["queries"] is None for d in decisions.values()),
        "decisions": len(decisions), "cap": nonnegative(run.get("queryCap")),
    }


class SnapshotCache:
    """Parse a large batch once per file revision; cache only the small projection."""
    def __init__(self):
        self.key = None
        self.value = None

    @staticmethod
    def signature(path):
        stat = path.stat()
        return stat.st_dev, stat.st_ino, stat.st_mtime_ns, stat.st_size

    def load(self, path, operation_id):
        before = self.signature(path)
        key = (path, operation_id, before)
        if key == self.key:
            return self.value
        batch = read(path)
        if not isinstance(batch, dict):
            raise ValueError("La tanda no contiene un objeto JSON.")
        value = summarize(batch, operation_id)
        if self.signature(path) != before:
            raise OSError("La tanda cambió durante la lectura; se leerá la próxima foto completa.")
        self.key, self.value = key, value
        return value


def snapshot(campaign, activity, cache=None):
    batch_id = activity.get("batchId")
    if not isinstance(batch_id, str) or activity_run._safe_id(batch_id) != batch_id:
        raise ValueError("La actividad no identifica una tanda válida.")
    path = campaign / ".content-audit/batches" / (batch_id + ".json")
    return (cache or SnapshotCache()).load(path, activity["id"])


def presentation(state):
    if state["run"] is None:
        summary = state["reason"] + " El proceso está activo; eso no confirma avance."
        return {"summary": summary, "next": "Esperar el primer resultado guardado de esta pasada.",
                "completed": 0, "stages": [], "facts": [
                    {"label": "Seguimiento", "value": "Sin resultados atribuibles todavía", "tone": "NEUTRAL"}]}
    cap = state["cap"]
    limit = str(cap) if cap is not None else "sin dato"
    ties = f"{state['ties']} " + ("empate" if state["ties"] == 1 else "empates")
    summary = (f"{state['decided']} de {state['total']} quizzes con decisión guardada. "
               f"{state['accepted']} propuestas que pasaron la validación, pendientes de tu revisión; {ties} para revisar. "
               f"{state['confirmed']} {QUERY_LABEL}; tope {limit}. "
               "El proceso está activo; las consultas aún sin resultado guardado no están incluidas.")
    exceeded = cap is not None and state["confirmed"] > cap
    if exceeded:
        summary += f" Atención: los resultados guardados superan el tope en {state['confirmed'] - cap} consultas."
    facts = [
        {"label": "Gasto parcial", "value": f"{state['confirmed']} {QUERY_LABEL}", "tone": "NEUTRAL"},
        {"label": "Tope autorizado", "value": f"{limit} consultas", "tone": "NEUTRAL"},
    ]
    if state["withoutCost"]:
        facts.append({"label": "Gasto sin dato", "value": f"{state['withoutCost']} decisiones no informan consultas", "tone": "NEUTRAL"})
    if exceeded:
        facts.append({"label": "Tope superado", "value": f"{state['confirmed']} registradas frente a {cap} autorizadas", "tone": "NEUTRAL"})
    # Changing outcome counts belong in summary/stage: the launcher replaces the
    # summary and the UI hides RUNNING stages on closure, but preserves facts.
    # The shared activity writer clips its headline bar to the authorized cap.
    # Preserve the true number here and in both plain-language summaries.
    stages = [{"label": "Resultados de esta pasada", "status": "RUNNING", "result": summary,
               "progress": {"completed": state["confirmed"], "total": cap, "unit": QUERY_LABEL}}]
    return {"summary": summary, "next": "Comparar las propuestas guardadas con la versión anterior y revisar los empates; la pasada sigue en curso.",
            "completed": state["confirmed"], "stages": stages, "facts": facts}


def tick(args):
    activity = read(activity_run._record_path(args.workdir, args.activity_id))
    if activity.get("status") != "RUNNING" or activity.get("finishedAt"):
        return False
    if activity.get("kind") != KIND or activity.get("id") != args.activity_id:
        raise ValueError("El monitor sólo admite la actividad exacta de una recorrección.")
    if activity_run.process_is_alive(args.process_pid) is not True:
        return False
    cache = getattr(args, "_recorrect_cache", None)
    if cache is None:
        cache = args._recorrect_cache = SnapshotCache()
    try:
        state = snapshot(args.campaign_workdir, activity, cache)
    except (OSError, ValueError, TypeError):
        # Atomic replacement or a temporarily incomplete file is not new progress.
        return True
    run = state["run"]
    if run and (run.get("status") != "RUNNING" or run.get("finishedAt")):
        return False
    if run and run.get("processPid") is not None and run["processPid"] != args.process_pid:
        return False
    shown = presentation(state)
    # Compare against durable activity, also after reattachment. Another run may
    # change the huge batch without adding any evidence to this operation.
    if all(activity.get(field) == shown[field] for field in ("summary", "next", "stages", "facts")):
        return True
    activity_run.update(Namespace(
        workdir=args.workdir, id=args.activity_id, status="RUNNING", only_if_running=True,
        completed=shown["completed"], progress_unit="consultas", process_pid=args.process_pid,
        summary=shown["summary"], next=shown["next"], stages=shown["stages"], facts=shown["facts"],
        # There are no timestamps on panel decisions. The shared writer records
        # when a larger confirmed count is observed; never invent a result time
        # or refresh that clock just because a monitor reattached.
        finish=False))
    return True


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--workdir", type=Path, required=True)
    parser.add_argument("--campaign-workdir", type=Path, required=True)
    parser.add_argument("--activity-id", required=True)
    parser.add_argument("--process-pid", type=int, required=True)
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args(argv)
    if activity_run._safe_id(args.activity_id) != args.activity_id or args.process_pid <= 0:
        parser.error("La actividad y el PID deben ser válidos.")
    path = args.workdir / ".content-audit/activity/monitors" / (args.activity_id + "-recorrect.lock")
    path.parent.mkdir(parents=True, exist_ok=True)
    # Open without truncating: an unsuccessful second monitor must not alter the owner lock.
    with path.open("a+", encoding="utf-8") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0
        lock.seek(0)
        lock.truncate()
        lock.write(str(os.getpid()))
        lock.flush()
        while tick(args):
            if args.once:
                break
            time.sleep(POLL_SECONDS)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
