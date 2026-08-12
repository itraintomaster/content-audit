#!/bin/bash
# Emite correctionCandidate como artifact del framework. Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

AGENT = "quiz-instruction-corrector"

state = json.loads(sys.argv[1])
inputs = state.get("inputs", {}) or {}
data = state.get("data", {}) or {}
node = state.get("node", {}) or {}


def resolve_run_dir():
    for raw in (
        state.get("run_dir"),
        inputs.get("run_dir"),
        data.get("run_dir"),
        node.get("run_dir"),
        os.environ.get("SENTINEL_RUN_DIR"),
    ):
        if isinstance(raw, str) and raw.strip():
            path = Path(raw).expanduser().resolve()
            if path.is_dir():
                return path
            return None

    project_raw = (
        inputs.get("project_root") or inputs.get("root")
        or data.get("project_root") or data.get("root")
        or state.get("project_root") or state.get("root")
    )
    run_id = (
        state.get("run_id") or inputs.get("run_id")
        or data.get("run_id") or node.get("run_id")
    )
    if not isinstance(project_raw, str) or not project_raw.strip():
        return None
    if (
        not isinstance(run_id, str)
        or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id)
        or Path(run_id).name != run_id
    ):
        return None

    project_root = Path(project_raw).expanduser().resolve()
    runs_base = (project_root / ".sentinel" / "runs" / AGENT).resolve()
    if not runs_base.is_dir():
        return None

    direct = (runs_base / run_id).resolve()
    if direct.parent == runs_base and direct.is_dir():
        return direct

    matches = []
    for child in runs_base.iterdir():
        try:
            resolved = child.resolve()
        except OSError:
            continue
        if resolved.parent != runs_base or not resolved.is_dir():
            continue
        try:
            with (resolved / "state.json").open("r", encoding="utf-8") as handle:
                persisted = json.load(handle)
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(persisted, dict) and persisted.get("runId") == run_id:
            matches.append(resolved)
    return matches[0] if len(matches) == 1 else None


def read(run_dir, name):
    if run_dir is None:
        return None
    try:
        with (run_dir / name).open("r", encoding="utf-8") as handle:
            parsed = json.load(handle)
        return parsed if isinstance(parsed, dict) else None
    except (OSError, json.JSONDecodeError):
        return None


run_dir = resolve_run_dir()

# F-QICOR-R013: se entrega el MEJOR candidato visto, no el último. Cuando
# route_verdict llega acá porque el candidato cumplió todo, ese candidato ES el
# champion (tally lo acaba de snapshotear). Cuando se llega por el edge
# `exhausted` de generate, el champion es el mejor de los intentos que hubo.
# Preferirlo siempre evita que la salida dependa de por dónde se entró.
candidate = read(run_dir, "champion.json")
assessment = read(run_dir, "champion_assessment.json") or {}
meta = read(run_dir, "champion_meta.json") or {}

# Sin champion NO se cae al último candidato generado: uno que no cumple su
# consigna reabre el mismo diagnóstico y le quema al operador una revisión
# (R005/R013). Se declara que no hubo nada entregable, y ya.

attempts = 0
try:
    with (run_dir / "attempts.jsonl").open("r", encoding="utf-8") as handle:
        attempts = sum(1 for line in handle if line.strip())
except (OSError, TypeError, AttributeError):
    attempts = 0

if candidate is not None:
    artifact = {
        "generated": True,
        "quizSentence": candidate.get("quizSentence", ""),
        "translation": candidate.get("translation", ""),
        "rationale": candidate.get("rationale", ""),
        "reason": "",
        # R014: lo que el campeón NO cumplió viaja con él. Entregarlo sin decirlo
        # es lo único que la regla sigue prohibiendo.
        "unmetCriteria": assessment.get("unmetCriteria") or [],
        "lengthMeasurement": assessment.get("lengthMeasurement") or {},
        "attempts": attempts,
        "allCriteriaMet": bool(meta.get("allCriteriaMet")),
    }
else:
    # generate agotó sus intentos sin producir JSON válido. NO se fabrica un
    # candidato: uno inventado acá entraría al catálogo de criterios como si
    # fuera una propuesta real, y el operador terminaría revisando algo que
    # nadie propuso. El adaptador Java traduce generated=false en una falla de
    # generación, que es lo que efectivamente pasó.
    artifact = {
        "generated": False,
        "quizSentence": "",
        "translation": "",
        "rationale": "",
        "reason": "Ningun candidato resulto entregable: o no se emitio JSON valido, "
                  "o ninguno cumplio la consigna ni constituyo un cambio real.",
        "unmetCriteria": [],
        "lengthMeasurement": {},
        "attempts": attempts,
        "allCriteriaMet": False,
    }

content = json.dumps(artifact, ensure_ascii=False, separators=(",", ":"))
print(json.dumps({"route": "done", "correctionCandidate": content}, ensure_ascii=False))
PY
