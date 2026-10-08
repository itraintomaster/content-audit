#!/usr/bin/env python3
"""Estimaciones conservadoras para procesos con progreso durable.

La ETA nunca se inventa desde un único punto. Aprende del ritmo entre avances,
lo suaviza para que no salte con cada consulta y publica un rango en vez de un
minuto exacto. El consumidor decide si el total es trabajo exacto o un techo.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone


@dataclass(frozen=True)
class ProgressEstimate:
    status: str
    rate_per_minute: float | None = None
    remaining_seconds_low: int | None = None
    remaining_seconds_high: int | None = None
    sample_count: int = 0
    observed_at: float | None = None

    def as_json(self) -> dict[str, object]:
        result: dict[str, object] = {
            "status": self.status,
            "sampleCount": self.sample_count,
        }
        if self.rate_per_minute is not None:
            result["ratePerMinute"] = round(self.rate_per_minute, 2)
        if self.remaining_seconds_low is not None:
            result["remainingSecondsLow"] = self.remaining_seconds_low
        if self.remaining_seconds_high is not None:
            result["remainingSecondsHigh"] = self.remaining_seconds_high
        if self.observed_at is not None:
            result["observedAt"] = datetime.fromtimestamp(
                self.observed_at, tz=timezone.utc
            ).isoformat()
        return result


class ProgressEstimator:
    """EMA sobre intervalos con avance; requiere dos señales antes de estimar."""

    def __init__(
        self,
        *,
        minimum_samples: int = 2,
        minimum_elapsed: float = 8.0,
        stale_after: float = 120.0,
        smoothing: float = 0.35,
    ) -> None:
        self.minimum_samples = max(1, minimum_samples)
        self.minimum_elapsed = max(0.0, minimum_elapsed)
        self.stale_after = max(1.0, stale_after)
        self.smoothing = min(1.0, max(0.01, smoothing))
        self._started_at: float | None = None
        self._last_at: float | None = None
        self._last_advanced_at: float | None = None
        self._last_completed: int | None = None
        self._rate_per_second: float | None = None
        self._samples = 0

    def observe(self, completed: int, total: int, *, now: float) -> ProgressEstimate:
        completed = max(0, completed)
        total = max(0, total)
        if self._started_at is None:
            self._started_at = now
            self._last_at = now
            self._last_completed = completed
            self._last_advanced_at = now
        elif self._last_completed is not None and completed < self._last_completed:
            self._reset(completed, now)
        elif self._last_completed is not None and completed > self._last_completed:
            elapsed = max(
                0.0,
                now - (self._last_at if self._last_at is not None else now),
            )
            if elapsed > 0:
                instant = (completed - self._last_completed) / elapsed
                self._rate_per_second = (
                    instant
                    if self._rate_per_second is None
                    else self.smoothing * instant
                    + (1.0 - self.smoothing) * self._rate_per_second
                )
                self._samples += 1
                self._last_advanced_at = now
            self._last_completed = completed
            self._last_at = now

        if total <= 0:
            return self._estimate("CALCULATING", now=now)
        if completed >= total:
            return self._estimate(
                "COMPLETE",
                now=now,
                remaining_low=0,
                remaining_high=0,
            )
        if (
            self._last_advanced_at is not None
            and now - self._last_advanced_at >= self.stale_after
        ):
            return self._estimate("STALE", now=now)
        learned_for = now - (
            self._started_at if self._started_at is not None else now
        )
        if (
            self._samples < self.minimum_samples
            or learned_for < self.minimum_elapsed
            or self._rate_per_second is None
            or self._rate_per_second <= 0
        ):
            return self._estimate("CALCULATING", now=now)

        remaining = (total - completed) / self._rate_per_second
        # Al principio el rango es deliberadamente ancho. Después de cinco
        # avances, el ritmo ya permite una horquilla más útil sin fingir una
        # precisión que las consultas a modelos no tienen.
        low_factor, high_factor = (
            (0.8, 1.3) if self._samples >= 5 else (0.65, 1.55)
        )
        return self._estimate(
            "AVAILABLE",
            now=now,
            remaining_low=max(1, round(remaining * low_factor)),
            remaining_high=max(1, round(remaining * high_factor)),
        )

    def _reset(self, completed: int, now: float) -> None:
        self._started_at = now
        self._last_at = now
        self._last_advanced_at = now
        self._last_completed = completed
        self._rate_per_second = None
        self._samples = 0

    def _estimate(
        self,
        status: str,
        *,
        now: float,
        remaining_low: int | None = None,
        remaining_high: int | None = None,
    ) -> ProgressEstimate:
        return ProgressEstimate(
            status=status,
            rate_per_minute=(
                self._rate_per_second * 60
                if self._rate_per_second is not None
                else None
            ),
            remaining_seconds_low=remaining_low,
            remaining_seconds_high=remaining_high,
            sample_count=self._samples,
            observed_at=now,
        )
