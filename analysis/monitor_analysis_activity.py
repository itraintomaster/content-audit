#!/usr/bin/env python3
"""Publica avance durable mientras una Corrección está siendo analizada.

El cierre de una tanda sólo consolida sus contadores al terminar. Durante la
corrida, en cambio, cada veredicto válido ya queda anexado al ledger JSONL del
juez. Este monitor observa exclusivamente esos ledgers y actualiza Actividad;
no ejecuta analizadores, jueces ni modelos.
"""

from __future__ import annotations

import argparse
import fcntl
import json
import os
import time
from argparse import Namespace
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

import activity_run
from progress_estimate import ProgressEstimate, ProgressEstimator


JUDGE_FILES: dict[str, tuple[str, ...]] = {
    "quiz-instruction": ("quiz-instruction-validator.jsonl",),
    "multiple-choice-validity": ("choice-alternative-judge.jsonl",),
    "multiple-choice-quality": ("choice-quality-judge.jsonl",),
    "translation-sense": (
        "translation-sense-from-english.jsonl",
        "translation-sense-from-translation.jsonl",
    ),
    "single-answer": ("single-answer-judge.jsonl",),
    "mini-theory": ("mini-theory-judge.jsonl",),
}

JUDGE_NAMES = {
    "quiz-instruction": "Consigna que no se cumple",
    "multiple-choice-validity": "Opción múltiple mal armada",
    "multiple-choice-quality": "Calidad de distractores",
    "translation-sense": "Traducción que no corresponde",
    "single-answer": "Más de una respuesta válida",
    "mini-theory": "Miniteoría con error",
}

PROGRESS_PREFIX = "@@CONTENT_AUDIT_PROGRESS "


@dataclass(frozen=True)
class JudgeProgress:
    judge: str
    completed: int
    budget: int
    last_result_at: float

    @property
    def label(self) -> str:
        return JUDGE_NAMES.get(self.judge, self.judge)


@dataclass(frozen=True)
class AnalysisMetrics:
    completed: int
    total: int
    current: JudgeProgress | None
    judges: tuple[JudgeProgress, ...]

    @property
    def last_result_at(self) -> float:
        return max((item.last_result_at for item in self.judges), default=0.0)


@dataclass(frozen=True)
class AttributionProgress:
    completed: int
    total: int

    @property
    def fraction(self) -> float:
        return 0.0 if self.total <= 0 else min(1.0, self.completed / self.total)


def _assignments(values: Iterable[str], *, label: str) -> dict[str, int]:
    result: dict[str, int] = {}
    for raw in values:
        key, separator, amount = raw.partition("=")
        key = key.strip()
        if not separator or not key:
            raise ValueError(f"{label} inválido: {raw}")
        value = int(amount)
        if value < 0:
            raise ValueError(f"{label} negativo: {raw}")
        result[key] = value
    return result


def _appended_records(path: Path, baseline_byte: int) -> tuple[int, float]:
    try:
        size = path.stat().st_size
        if size <= baseline_byte:
            return 0, 0.0
        with path.open("rb") as source:
            source.seek(baseline_byte)
            appended = source.read()
        # Sólo cuenta registros cerrados. Una escritura parcial todavía no es
        # evidencia durable y aparecerá en el siguiente ciclo.
        completed = appended.count(b"\n")
        return completed, path.stat().st_mtime if completed else 0.0
    except (FileNotFoundError, OSError):
        return 0, 0.0


def _instant(value: str) -> datetime | None:
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def baseline_byte_at(path: Path, started_at: datetime) -> int:
    """Ubica el primer registro de esta ejecución para recuperar monitores."""
    try:
        with path.open("rb") as source:
            while True:
                offset = source.tell()
                raw = source.readline()
                if not raw:
                    return offset
                try:
                    record = json.loads(raw)
                except (json.JSONDecodeError, UnicodeDecodeError):
                    continue
                recorded_at = _instant(str(record.get("recordedAt") or ""))
                if recorded_at is not None and recorded_at >= started_at:
                    return offset
    except OSError:
        return 0


def metrics(
    evaluations: Path,
    budgets: dict[str, int],
    baseline_bytes: dict[str, int],
) -> AnalysisMetrics:
    per_judge: list[JudgeProgress] = []
    for judge, budget in budgets.items():
        completed = 0
        last_result_at = 0.0
        for filename in JUDGE_FILES.get(judge, ()):
            records, modified = _appended_records(
                evaluations / filename,
                baseline_bytes.get(filename, 0),
            )
            completed += records
            last_result_at = max(last_result_at, modified)
        per_judge.append(
            JudgeProgress(
                judge=judge,
                completed=min(completed, budget),
                budget=budget,
                last_result_at=last_result_at,
            )
        )

    with_results = [item for item in per_judge if item.last_result_at > 0]
    current = max(with_results, key=lambda item: item.last_result_at, default=None)
    if current is None:
        current = next((item for item in per_judge if item.budget > 0), None)
    return AnalysisMetrics(
        completed=sum(item.completed for item in per_judge),
        total=sum(item.budget for item in per_judge),
        current=current,
        judges=tuple(per_judge),
    )


def _elapsed_label(seconds: float) -> str:
    seconds = max(0, int(seconds))
    if seconds < 60:
        return f"{seconds} s"
    minutes = seconds // 60
    if minutes < 60:
        return f"{minutes} min"
    return f"{minutes // 60} h {minutes % 60} min"


def _process_is_alive(process_pid: int | None) -> bool | None:
    return activity_run.process_is_alive(process_pid)


def attribution_progress(output: Path) -> AttributionProgress | None:
    """Reads the latest structured leave-one-out marker written by the CLI."""
    latest: AttributionProgress | None = None
    try:
        lines = output.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError:
        return None
    for line in lines:
        marker = line.find(PROGRESS_PREFIX)
        if marker < 0:
            continue
        try:
            record = json.loads(line[marker + len(PROGRESS_PREFIX) :])
            if record.get("phase") != "ATTRIBUTING_REGRESSIONS":
                continue
            completed = int(record.get("completed", -1))
            total = int(record.get("total", -1))
        except (json.JSONDecodeError, TypeError, ValueError, AttributeError):
            continue
        if completed < 0 or total < 0 or completed > total:
            continue
        latest = AttributionProgress(completed, total)
    return latest


def summary(
    current: AnalysisMetrics,
    *,
    now: float | None = None,
    process_alive: bool | None = None,
    settle_after: float = 20.0,
    attribution: AttributionProgress | None = None,
) -> str:
    now = time.time() if now is None else now
    prefix = (
        f"{current.completed} consultas nuevas ya quedaron guardadas; "
        f"{current.total} es el máximo estimado, no una meta obligatoria."
    )
    result_age = now - current.last_result_at if current.last_result_at else 0
    if process_alive is False:
        return (
            f"{prefix} El proceso técnico terminó; la app está comprobando "
            "si el cierre de Análisis 1 quedó guardado."
        )
    if attribution is not None:
        return (
            f"{prefix} Ahora está atribuyendo regresiones: "
            f"{attribution.completed} de {attribution.total} cambios "
            "comparados dejando cada uno afuera una vez."
        )
    if current.last_result_at and result_age >= settle_after:
        return (
            f"El proceso sigue activo. {prefix} El contador no cambia desde "
            f"hace {_elapsed_label(result_age)} porque ahora puede estar "
            "reutilizando resultados, ejecutando analizadores locales o "
            "consolidando el cierre."
        )
    active = current.current
    if active is None:
        return f"Análisis en curso. {prefix} Preparando el primer analizador."
    if active.completed == 0:
        return f"Análisis en curso. {prefix} Preparando {active.label}."
    return (
        f"Análisis en curso. {prefix} Último juez con resultados: "
        f"{active.label}, {active.completed} guardados."
    )


def _activity_is_running(workdir: Path, activity_id: str) -> bool:
    path = (
        workdir
        / ".content-audit"
        / "activity"
        / "runs"
        / f"{activity_id}.json"
    )
    try:
        record: Any = activity_run._read_object(path)
    except (FileNotFoundError, OSError, ValueError):
        return False
    return isinstance(record, dict) and str(record.get("status") or "").upper() == "RUNNING"


def _has_persisted_closure(args: argparse.Namespace) -> bool:
    """Confirm an actual closing, never infer success from an absent process."""
    try:
        run = activity_run._read_object(activity_run._record_path(args.workdir, args.activity_id))
        batch_id = run.get("batchId")
        if not batch_id:
            return False
        batch = activity_run._read_object(
            args.campaign_workdir / ".content-audit/batches" / f"{batch_id}.json"
        )
        started = _instant(str(run.get("startedAt") or ""))
        return started is not None and any(
            (closed := _instant(str(closing.get("closedAt") or ""))) is not None and closed >= started
            for closing in batch.get("closings", []) if isinstance(closing, dict)
        )
    except (OSError, ValueError):
        return False


def _publish(
    current: AnalysisMetrics,
    args: argparse.Namespace,
    *,
    now: float,
    process_alive: bool | None,
    attribution: AttributionProgress | None,
    model_estimate: ProgressEstimate,
    attribution_estimate: ProgressEstimate | None,
) -> None:
    result_age = now - current.last_result_at if current.last_result_at else 0
    quiet = bool(current.last_result_at and result_age >= args.settle_after)
    if quiet:
        model_status = "WAITING"
        model_result = (
            f"{current.completed} resultados guardados. {current.total} era "
            "el techo estimado; no tienen que coincidir."
        )
        local_status = "RUNNING" if process_alive is not False else "WAITING"
        if attribution is not None:
            local_result = (
                f"Atribuyendo regresiones: {attribution.completed} de "
                f"{attribution.total} cambios comparados."
            )
        else:
            local_result = (
                "Proceso principal activo; puede estar reutilizando resultados, "
                "ejecutando analizadores locales o consolidando el cierre."
                if process_alive is not False
                else "El proceso terminó; falta confirmar el cierre guardado."
            )
    else:
        model_status = "RUNNING"
        model_result = (
            f"{current.completed} resultados guardados de hasta "
            f"{current.total}."
        )
        local_status = "WAITING"
        local_result = "Comienza al terminar o reutilizar los veredictos aplicables."
    activity_run.update(
        Namespace(
            workdir=args.workdir,
            id=args.activity_id,
            status="RUNNING",
            only_if_running=True,
            process_pid=args.process_pid,
            last_progress_at=(
                datetime.fromtimestamp(current.last_result_at, timezone.utc).isoformat()
                if current.last_result_at else None
            ),
            completed=current.completed,
            summary=summary(
                current,
                now=now,
                process_alive=process_alive,
                settle_after=args.settle_after,
                attribution=attribution,
            ),
            next=(
                "Podés navegar o volver a leer: el análisis sigue en segundo "
                "plano y cada veredicto válido queda guardado."
            ),
            stages=[
                {
                    "label": "Consultas con modelos",
                    "status": model_status,
                    "result": model_result,
                    "progress": {
                        "completed": current.completed,
                        "total": current.total,
                        "unit": "consultas",
                        "totalKind": "CEILING",
                        "estimate": model_estimate.as_json(),
                    },
                },
                {
                    "label": "Analizadores locales y cierre",
                    "status": local_status,
                    "result": local_result,
                    **(
                        {
                            "progress": {
                                "completed": attribution.completed,
                                "total": attribution.total,
                                "unit": "cambios",
                                "totalKind": "EXACT",
                                **(
                                    {
                                        "estimate": attribution_estimate.as_json()
                                    }
                                    if attribution_estimate is not None
                                    else {}
                                ),
                            }
                        }
                        if attribution is not None
                        else {}
                    ),
                },
            ],
            facts=[
                {
                    "label": "Señal del proceso",
                    "value": "Activa" if process_alive is not False else "Sin proceso",
                    "tone": "POSITIVE" if process_alive is not False else "WARNING",
                },
                {
                    "label": "Máximo estimado",
                    "value": f"{current.total} consultas",
                    "tone": "NEUTRAL",
                },
                *(
                    [
                        {
                            "label": "Atribución de regresiones",
                            "value": f"{attribution.completed} / {attribution.total} cambios",
                            "tone": "NEUTRAL",
                        }
                    ]
                    if attribution is not None
                    else []
                ),
            ],
            finish=False,
            baseline_queries=None,
            baseline_tasks_attempted=None,
            baseline_proposals=None,
            baseline_resolved=None,
            baseline_no_winner=None,
            progress_estimate=(
                model_estimate.as_json() if not quiet else {}
            ),
        )
    )


def monitor(args: argparse.Namespace) -> int:
    budgets = _assignments(args.judge_budget, label="presupuesto")
    baseline_bytes = _assignments(args.baseline_byte, label="posición inicial")
    evaluations = args.campaign_workdir / ".content-audit" / "evaluations"
    started_at = _instant(args.started_at or "")
    if started_at is not None:
        for judge in budgets:
            for filename in JUDGE_FILES.get(judge, ()):
                baseline_bytes.setdefault(
                    filename,
                    baseline_byte_at(evaluations / filename, started_at),
                )
    lock_path = (
        args.workdir
        / ".content-audit"
        / "activity"
        / "monitors"
        / f"{args.activity_id}-analysis.lock"
    )
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    with lock_path.open("w", encoding="utf-8") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            return 0
        lock.write(str(os.getpid()))
        lock.flush()

        last_published: AnalysisMetrics | None = None
        last_attribution: AttributionProgress | None = None
        last_model_estimate: ProgressEstimate | None = None
        last_attribution_estimate: ProgressEstimate | None = None
        last_heartbeat = 0.0
        model_estimator = ProgressEstimator()
        attribution_estimator = ProgressEstimator()
        output = (
            args.workdir
            / ".content-audit"
            / "activity"
            / "output"
            / f"{args.activity_id}.log"
        )
        while _activity_is_running(args.workdir, args.activity_id):
            current = metrics(evaluations, budgets, baseline_bytes)
            attribution = attribution_progress(output)
            now = time.time()
            process_alive = _process_is_alive(args.process_pid)
            if process_alive is False:
                closed = _has_persisted_closure(args)
                activity_run.update(Namespace(
                    workdir=args.workdir, id=args.activity_id,
                    status="NEEDS_REVIEW" if closed else "FAILED", only_if_running=True,
                    completed=current.completed,
                    summary=("El análisis terminó y su cierre quedó guardado. Falta revisar la calidad."
                             if closed else "El proceso de análisis terminó sin un cierre nuevo confirmado. Lo ya juzgado se conserva."),
                    next="Revisar el cierre; si falta cobertura, completar sólo lo pendiente.",
                    finish=True,
                ))
                return 0
            model_estimate = model_estimator.observe(
                current.completed,
                current.total,
                now=now,
            )
            attribution_estimate = (
                attribution_estimator.observe(
                    attribution.completed,
                    attribution.total,
                    now=now,
                )
                if attribution is not None
                else None
            )
            if (
                current != last_published
                or attribution != last_attribution
                or model_estimate != last_model_estimate
                or attribution_estimate != last_attribution_estimate
                or now - last_heartbeat >= args.heartbeat
            ):
                _publish(
                    current,
                    args,
                    now=now,
                    process_alive=process_alive,
                    attribution=attribution,
                    model_estimate=model_estimate,
                    attribution_estimate=attribution_estimate,
                )
                last_published = current
                last_attribution = attribution
                last_model_estimate = model_estimate
                last_attribution_estimate = attribution_estimate
                last_heartbeat = now
            time.sleep(args.interval)
    return 0


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser()
    root.add_argument("--workdir", type=Path, required=True)
    root.add_argument("--campaign-workdir", type=Path, required=True)
    root.add_argument("--activity-id", required=True)
    root.add_argument("--judge-budget", action="append", default=[])
    root.add_argument("--baseline-byte", action="append", default=[])
    root.add_argument("--started-at")
    root.add_argument("--process-pid", type=int)
    root.add_argument("--interval", type=float, default=3.0)
    root.add_argument("--heartbeat", type=float, default=5.0)
    root.add_argument("--settle-after", type=float, default=20.0)
    return root


def main() -> int:
    return monitor(parser().parse_args())


if __name__ == "__main__":
    raise SystemExit(main())
