#!/bin/bash
# Emite criterionVerdict como artifact del framework. Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

AGENT = "candidate-judgment"

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


run_dir = resolve_run_dir()
verdict = None
if run_dir is not None:
    validated_path = run_dir / "validated-verdict.json"
    try:
        with validated_path.open("r", encoding="utf-8") as handle:
            parsed = json.load(handle)
        if isinstance(parsed, dict):
            verdict = parsed
    except (OSError, json.JSONDecodeError):
        verdict = None

if verdict is not None:
    artifact = {
        "criterion": verdict.get("criterion", ""),
        "outcome": verdict.get("outcome", ""),
        "detail": verdict.get("detail", ""),
    }
else:
    # judge agotó sus intentos sin producir JSON válido. El veredicto es
    # NOT_EVALUABLE, nunca FAILED: reprobar afirma que el candidato empeoró
    # algo, y una falla de infraestructura no afirma nada sobre el candidato.
    # Degradarlo a FAILED descartaría candidatos buenos y le diría al operador
    # que la corrección es imposible cuando lo que pasó es que el juez no
    # contestó.
    asked = (inputs.get("criterion") or data.get("criterion") or "").strip()
    artifact = {
        "criterion": asked,
        "outcome": "NOT_EVALUABLE",
        "detail": "El juez agotó sus intentos sin emitir un veredicto bien formado.",
    }

content = json.dumps(artifact, ensure_ascii=False, separators=(",", ":"))
print(json.dumps({"route": "done", "criterionVerdict": content}, ensure_ascii=False))
PY
