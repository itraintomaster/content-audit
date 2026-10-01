#!/bin/bash
# Emite naturalnessAnswer como artifact del framework. Solo stdlib.
#
# El veredicto es el que validó y canonizó gate_verdict. Si evaluate agotó sus
# intentos sin uno válido, el artifact es {"outputUnusable": true}: nunca un
# veredicto inventado. El evaluador lo devuelve como OUTPUT_UNUSABLE, no llega
# al lector ni queda en el libro, y la unidad queda sin evaluar.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

AGENT = "naturalness-yes-no"
TAG = "naturalness_yes_no_emit"
ARTIFACT = "naturalnessAnswer"
TOP_KEYS = {"answer"}

state = json.loads(sys.argv[1])
inputs = state.get("inputs", {}) or {}
data = state.get("data", {}) or {}
node = state.get("node", {}) or {}


def resolve_run_dir():
    """Resuelve el directorio del run sin asumir que run_dir viene proyectado.

    El runner in-process de content-audit agrega run_dir a las entradas; el CLI
    proyecta run_id arriba de todo y project_root/root en inputs. Un run_dir
    explícito tiene precedencia.
    """
    for raw in (
        state.get("run_dir"),
        inputs.get("run_dir"),
        data.get("run_dir"),
        node.get("run_dir"),
        os.environ.get("SENTINEL_RUN_DIR"),
    ):
        if isinstance(raw, str) and raw.strip():
            path = Path(raw).expanduser().resolve()
            if not path.is_dir():
                return None, "run_dir explícito no existe o no es directorio: %s" % path
            return path, None

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
        return None, "faltan run_dir y project_root/root para resolver el run"
    if (
        not isinstance(run_id, str)
        or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", run_id)
        or Path(run_id).name != run_id
    ):
        return None, "run_id ausente o inseguro"

    project_root = Path(project_raw).expanduser().resolve()
    if not project_root.is_dir():
        return None, "project_root/root no existe o no es directorio: %s" % project_root
    runs_base = (project_root / ".sentinel" / "runs" / AGENT).resolve()
    if not runs_base.is_dir():
        return None, "directorio base de runs no existe: %s" % runs_base

    direct = (runs_base / run_id).resolve()
    if direct.parent != runs_base:
        return None, "run_id escaparía del directorio de runs"
    if direct.is_dir():
        return direct, None

    # El run_id proyectado por el subprocess es el UUID de RunState, no el
    # basename con fecha que usa RunPersistence.
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
        except (OSError, ValueError):
            continue
        if isinstance(persisted, dict) and persisted.get("runId") == run_id:
            matches.append(resolved)

    if len(matches) != 1:
        return None, (
            "run_id %s coincide con %d state.json bajo %s; se requiere exactamente uno"
            % (run_id, len(matches), runs_base)
        )
    return matches[0], None


run_dir, run_dir_error = resolve_run_dir()
if run_dir_error:
    sys.stderr.write("[%s] %s\n" % (TAG, run_dir_error))

verdict = None
if run_dir is not None:
    try:
        with (run_dir / "validated-verdict.json").open("r", encoding="utf-8") as handle:
            parsed = json.load(handle)
    except (OSError, ValueError):
        parsed = None
    if isinstance(parsed, dict) and set(parsed) == TOP_KEYS:
        verdict = parsed

if verdict is None:
    # evaluate agotó sus intentos sin un veredicto válido (o no hay run_dir):
    # la salida conservadora no afirma nada sobre la unidad juzgada.
    sys.stderr.write("[%s] sin veredicto validado: emite outputUnusable\n" % TAG)
    verdict = {"outputUnusable": True}

content = json.dumps(verdict, ensure_ascii=False, separators=(",", ":"))
print(json.dumps({"route": "done", ARTIFACT: content}, ensure_ascii=False))
PY
