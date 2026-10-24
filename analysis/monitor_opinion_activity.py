#!/usr/bin/env python3
"""Read-only model telemetry for batch opinions, including unfinished scopes.

Only the activity feed is written. This never starts models, edits a batch,
integrates opinions, retries a failure or treats a living process as success.
"""
from __future__ import annotations

import argparse
from collections import Counter
import fcntl
import json
import os
import time
from argparse import Namespace
from datetime import datetime, timezone
from pathlib import Path

import activity_run

FILES = {
    "a1-didactic": ("a1-didactic-judge.jsonl",),
    "naturalness": ("native-usage-judge.jsonl",),
    "pragmatism": ("pragmatism-judge.jsonl",),
    "near-duplicates": ("near-duplicates-pair-judge.jsonl",),
}
NAMES = {"a1-didactic": "Didáctica A1", "naturalness": "Naturalidad",
         "pragmatism": "Pragmatismo", "near-duplicates": "Ejercicios parecidos"}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def instant(value):
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
        return parsed.replace(tzinfo=timezone.utc) if parsed.tzinfo is None else parsed
    except (ValueError, AttributeError):
        return None


def grade(payload, evaluator):
    """One opinion per exercise (or pair), even with multiple accepted sentences."""
    if not isinstance(payload, dict):
        return "UNKNOWN"
    if evaluator == "a1-didactic-judge":
        return {"SUITABLE":"GOOD", "IMPROVE":"BAD", "REWRITE_REQUIRED":"BAD",
                "REVIEW":"REVIEW", "NOT_APPLICABLE":"NOT_APPLICABLE"}.get(payload.get("status"), "UNKNOWN")
    if evaluator == "near-duplicates-pair-judge":
        return "BAD" if payload.get("sameExercise") is True else "GOOD" if payload.get("sameExercise") is False else "UNKNOWN"
    assessments = payload.get("assessments")
    if not isinstance(assessments, list) or not assessments:
        return "UNKNOWN"
    values = [a.get("status") for a in assessments if isinstance(a, dict)]
    states = {v for v in values if isinstance(v, str)}
    if "FAIL" in states:
        return "BAD"
    if states & {"REVIEW", "NEEDS_CONTEXT"}:
        return "REVIEW"
    if len(assessments) != len(values) or len(values) != sum(isinstance(v, str) for v in values) or not states <= {"PASS", "NOT_APPLICABLE"}:
        return "UNKNOWN"
    return "GOOD" if "PASS" in states else "NOT_APPLICABLE"


def totals_label(totals):
    text = f"Bien: {totals['GOOD']} · Mal: {totals['BAD']} · Para revisar: {totals['REVIEW']} · No aplica: {totals['NOT_APPLICABLE']}"
    if totals["UNKNOWN"]:
        text += f" · Sin clasificar: {totals['UNKNOWN']}"
    return text


def saved_opinions(evaluations, filenames, started, selected, version, runtime=None):
    """Count complete valid rows, not old/imported/exam/foreign/partial data."""
    identities, latest = {}, None
    for filename in filenames:
        try:
            with (evaluations / filename).open("rb") as stream:
                for line in stream:
                    if not line.endswith(b"\n"):
                        continue
                    try:
                        record = json.loads(line)
                        at = instant(record.get("recordedAt"))
                        key = record.get("key", record)
                        payload = json.loads(record["payload"])
                        if not isinstance(payload, (dict, list)):
                            continue
                        subject = record.get("subjectRef") or ""
                        in_scope = subject in selected or (filename == "near-duplicates-pair-judge.jsonl"
                            and len(subject.split("~")) == 2 and all(s in selected for s in subject.split("~")))
                        if (at is None or at < started or not in_scope
                                or record.get("evaluatorVersion") != version):
                            continue
                        if key.get("evaluatorId") != filename.removesuffix(".jsonl"):
                            continue
                        if runtime and any(record.get(k) != v for k, v in runtime.items()):
                            continue
                        identity = (key["evaluatorId"], key["contentFingerprint"])
                        previous = identities.get(identity)
                        if previous is None or at >= previous[0]:
                            identities[identity] = (at, grade(payload, key["evaluatorId"]))
                        latest = max(latest, at) if latest else at
                    except (KeyError, ValueError, TypeError, UnicodeDecodeError):
                        continue
        except OSError:
            pass
    totals = Counter({name:0 for name in ["GOOD", "BAD", "REVIEW", "NOT_APPLICABLE", "UNKNOWN"]})
    totals.update(value[1] for value in identities.values())
    return len(identities), latest, totals


def saved_records(*args, **kwargs):
    count, latest, _ = saved_opinions(*args, **kwargs)
    return count, latest


def snapshot(campaign, activity, *, now=None):
    started = instant(activity["startedAt"])
    if started is None:
        raise ValueError("La corrida no tiene una fecha inicial verificable.")
    batch = read(campaign / ".content-audit/batches" / (activity["batchId"] + ".json"))
    run = next((r for r in batch.get("opinionRuns", []) if r["id"] == activity["id"]), None)
    if run is None:
        return {"completed": 0, "stages": [], "run": None, "last": None,
                "summary": "Preparando la opinión; todavía no hay un pedido durable."}
    selection = (run.get("request") or {}).get("selection") or {}
    stages, completed, latest, partial = [], 0, None, []
    overall = Counter({name:0 for name in ["GOOD", "BAD", "REVIEW", "NOT_APPLICABLE", "UNKNOWN"]})
    for judge in run.get("judges", []):
        analyzer = judge["analyzer"]
        count, last, totals = saved_opinions(campaign / ".content-audit/evaluations",
            FILES.get(analyzer, ()), started, set(selection.get(analyzer, [])), judge["judgeVersion"],
            {"runtimeKind":"EXPLICIT", "providerName":activity["provider"],
             "modelName":activity["model"], "reasoningEffort":activity["reasoningEffort"]})
        completed += count
        overall.update(totals)
        if last:
            latest = max(latest, last) if latest else last
        results = [r["outcome"] for r in run.get("results", []) if r["analyzer"] == analyzer]
        measured = sum(analyzer in r.get("evaluatedAnalyzers", []) for r in results)
        unresolved = len(results) - measured
        reason = None
        supplements = [s for s in batch.get("selectiveRuns", [])
            if s.get("sourceRunId") == run["id"] and s.get("analyzer") == analyzer]
        if supplements:
            audit_id = supplements[-1].get("auditId")
            try:
                audit = read(campaign / ".content-audit/audits" / ("audit-" + audit_id + ".json"))
                coverages = audit["root"]["diagnoses"].get("judgeCoverages", [])
                reason = next((c.get("unavailableReason") for c in coverages
                    if c.get("judgeName") == analyzer and c.get("unavailableReason")), None)
            except (OSError, KeyError, TypeError, ValueError):
                pass
        label = NAMES.get(analyzer, analyzer)
        if results:
            status = "NEEDS_REVIEW" if unresolved else "PASSED"
            result = f"{measured} de {len(results)} opiniones disponibles; {unresolved} pendientes."
            if unresolved:
                partial.append(label)
                if reason:
                    result += " " + reason
        else:
            status = "RUNNING" if count else "WAITING"
            result = f"{count} opiniones nuevas guardadas." if count else "Todavía no comenzó este juez."
        result = "Opiniones nuevas — " + totals_label(totals) + ". " + result
        stages.append({"label": label, "status": status, "result": result,
            "progress": {"completed": measured if results else count,
                         "total": (judge["pendingQueries"] if analyzer == "near-duplicates" and not results else judge["quizzes"]),
                         "unit": "opiniones de pares" if analyzer == "near-duplicates" and not results else "ejercicios",
                         "totalKind": "EXACT"}})
    recent = max((s for s in stages if s["status"] == "RUNNING"),
                 key=lambda s: s["progress"]["completed"], default=None)
    summary = totals_label(overall) + f". {completed} opiniones nuevas válidas guardadas."
    clock = now or datetime.now(timezone.utc)
    if recent and latest and (clock - latest).total_seconds() < 60:
        summary += f" {recent['label']} sigue avanzando."
    if partial:
        summary += " Quedaron pendientes en " + ", ".join(partial) + "; no es una corrida completa."
    if latest and (clock - latest).total_seconds() >= 60:
        summary += f" Sin otra opinión válida desde hace {int((clock-latest).total_seconds()//60)} min; el proceso sigue vivo, eso no confirma avance."
    return {"completed": completed, "stages": stages, "run": run,
            "last": latest.isoformat() if latest else None, "summary": summary, "outcomes": dict(overall)}


def tick(args):
    record = read(activity_run._record_path(args.workdir, args.activity_id))
    if record.get("status") != "RUNNING" or record.get("finishedAt"):
        return False
    if record.get("kind") != "CONTENT_CONTEXT_OPINION":
        raise ValueError("El monitor sólo admite corridas de opinión.")
    state = snapshot(args.campaign_workdir, record)
    alive = activity_run.process_is_alive(args.process_pid)
    run = state["run"]
    finished = bool(run and run.get("finishedAt") and run.get("status") in ["DONE", "PARTIAL"])
    ending = alive is False
    status = "NEEDS_REVIEW" if ending and finished else "FAILED" if ending else "RUNNING"
    summary = state["summary"]
    if ending:
        summary += (f" Terminó como {run['status']}; falta revisar." if finished
                    else " El proceso terminó sin resultado final confirmado; no se reintenta automáticamente.")
    activity_run.update(Namespace(workdir=args.workdir, id=args.activity_id,
        status=status, only_if_running=True, completed=state["completed"],
        summary=summary, next="Las opiniones válidas se conservan. Los fallos temporales no cuentan como aprobados; al terminar, completar sólo lo pendiente.",
        stages=state["stages"], facts=[
            {"label":"Proceso", "value":"Activo" if alive is not False else "Terminado", "tone":"NEUTRAL"},
            {"label":"Tope autorizado", "value":str(run['reserved']) + " consultas" if run else "Sin dato", "tone":"NEUTRAL"},
            {"label":"Contador", "value":"Opiniones nuevas válidas; excluye intentos fallidos y resultados reutilizados", "tone":"NEUTRAL"}],
        process_pid=args.process_pid, last_progress_at=state["last"],
        progress_unit="opiniones nuevas", finish=ending))
    return not ending


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workdir", type=Path, required=True)
    parser.add_argument("--campaign-workdir", type=Path, required=True)
    parser.add_argument("--activity-id", required=True)
    parser.add_argument("--process-pid", type=int, required=True)
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args()
    lockpath = args.workdir / ".content-audit/activity/monitors" / (args.activity_id + "-opinion.lock")
    lockpath.parent.mkdir(parents=True, exist_ok=True)
    with lockpath.open("w") as lock:
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
