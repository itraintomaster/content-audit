#!/bin/bash
# Gate binario de forma para el juez de un criterio.
# stdin: estado del framework. stdout: UNA línea JSON. Solo Python stdlib.
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
    """Resuelve el directorio del run sin asumir que run_dir viene proyectado.

    El CLI vivo proyecta run_id arriba de todo y project_root/root en inputs.
    Un run_dir explícito tiene precedencia, para los lanzadores in-process.
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
    # basename timestamped que usa RunPersistence.
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

    if len(matches) != 1:
        return None, (
            "run_id %s coincide con %d state.json bajo %s; se requiere exactamente uno"
            % (run_id, len(matches), runs_base)
        )
    return matches[0], None


run_dir, run_dir_error = resolve_run_dir()
source_path = run_dir / "verdict.json" if run_dir else None
validated_path = run_dir / "validated-verdict.json" if run_dir else None


def fail(reason):
    if validated_path is not None:
        try:
            validated_path.unlink()
        except FileNotFoundError:
            pass
    sys.stderr.write("[candidate_judgment_gate] FAILED: " + reason + "\n")
    print(json.dumps({"route": "failed", "error": reason}, ensure_ascii=False))
    raise SystemExit(0)


if run_dir_error:
    fail(run_dir_error)


def parse_json_value(value):
    """Desenvuelve hasta 3 niveles de string-JSON, tolerando fences.

    El modelo a veces envuelve la salida en ```json ... ```. Rechazar por eso
    manda a reintentar un candidato que estaba bien, y se paga otra consulta
    para llegar al mismo lugar.
    """
    current = value
    for _ in range(3):
        if not isinstance(current, str):
            return current
        text = current.strip()
        if text.startswith("```"):
            text = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", text)
            text = re.sub(r"\s*```$", "", text).strip()
        try:
            current = json.loads(text)
        except json.JSONDecodeError:
            return None
    return current if not isinstance(current, str) else None


if source_path is None or not source_path.is_file():
    fail("verdict.json no existe en %s" % (source_path or "<run_dir desconocido>"))

try:
    raw = source_path.read_text(encoding="utf-8")
except OSError as exc:
    fail("verdict.json ilegible: %s" % exc)

verdict = parse_json_value(raw)
if not isinstance(verdict, dict):
    fail("verdict.json no es un objeto JSON")

REQUIRED = {"criterion", "outcome", "detail"}
present = set(verdict)
if present != REQUIRED:
    missing = sorted(REQUIRED - present)
    extra = sorted(present - REQUIRED)
    fail(
        "verdict.json no es un objeto cerrado; faltan=%s sobran=%s"
        % (missing or "[]", extra or "[]")
    )

for key in sorted(REQUIRED):
    value = verdict[key]
    if not isinstance(value, str):
        fail("verdict.%s debe ser string, es %s" % (key, type(value).__name__))
    if not value.strip():
        fail("verdict.%s está vacío" % key)

OUTCOMES = {"PASSED", "FAILED", "NOT_EVALUABLE"}
outcome = verdict["outcome"].strip()
if outcome not in OUTCOMES:
    fail("verdict.outcome '%s' fuera del enum %s" % (outcome, sorted(OUTCOMES)))

# El evaluador de Java se indexa por criterio: un veredicto que dice juzgar otro
# se archivaria bajo la casilla equivocada sin que nada lo delate.
asked = (inputs.get("criterion") or data.get("criterion") or "").strip()
got = verdict["criterion"].strip()
if asked and got != asked:
    fail("verdict.criterion '%s' no coincide con el criterio pedido '%s'" % (got, asked))

canonical = {"criterion": got, "outcome": outcome, "detail": verdict["detail"].strip()}

try:
    validated_path.write_text(
        json.dumps(canonical, ensure_ascii=False) + "\n", encoding="utf-8"
    )
except OSError as exc:
    fail("no se pudo escribir validated-verdict.json: %s" % exc)

print(json.dumps({"route": "passed", "detail": "veredicto bien formado"}, ensure_ascii=False))
PY
