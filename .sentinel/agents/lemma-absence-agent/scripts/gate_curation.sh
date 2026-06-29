#!/bin/bash
# Gate (R001/R002 refuerzo): verifica DETERMINISTAMENTE que curate_lemmas REALMENTE
# invocó la tool get_suggested_lemmas en su último intento. El framework sentinel-agent
# NO fuerza tool-choice: un nodo react puede responder SIN llamar la tool (shortcut),
# inventando el pool desde el conocimiento del modelo en vez del catálogo. Este gate
# lo detecta leyendo el propio log del run (events.jsonl, que el framework escribe en
# run_dir): busca, después del último `node_started` de curate_lemmas, un `llm_response`
# con un tool_call llamado get_suggested_lemmas, o un evento `tool_result`.
#
# passed → finalize_curation. failed → curate_lemmas (reintenta; el prompt fuerza la
# llamada). Al agotar curate.max_attempts, el edge `exhausted` de curate cae a
# finalize_curation (fallback a suggestedWords) — no haltea.
#
# Gate self-contained (skill pitfall #3): computa la señal de events.jsonl, no del data
# bus que el react "debería" haber poblado.
#
# stdin:  JSON { "inputs":{...}, "data":{...}, "node":{...}, "run_dir":... }
# stdout: JSON UNA línea con route passed/failed. Solo stdlib. Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

events_path = os.path.join(run_dir, "events.jsonl")
TOOL = "get_suggested_lemmas"

called = False
err = None
try:
    lines = []
    with open(events_path, encoding="utf-8") as f:
        for ln in f:
            ln = ln.strip()
            if ln:
                lines.append(ln)

    # Acotar el escaneo al ÚLTIMO intento de curate_lemmas (en reintentos hay varios
    # node_started): así "passed" significa que el intento que produjo el pool actual
    # sí llamó la tool, no uno anterior.
    last_start = -1
    for i, ln in enumerate(lines):
        try:
            e = json.loads(ln)
        except Exception:
            continue
        if e.get("event") == "node_started" and e.get("node") == "curate_lemmas":
            last_start = i

    scan = lines[last_start:] if last_start >= 0 else lines
    for ln in scan:
        try:
            e = json.loads(ln)
        except Exception:
            continue
        ev = e.get("event")
        if ev == "tool_result" and TOOL in json.dumps(e):
            called = True
            break
        if ev == "llm_response":
            for tc in (e.get("tool_calls") or []):
                if tc.get("name") == TOOL:
                    called = True
                    break
        if called:
            break
except Exception as ex:
    err = str(ex)

if err is not None:
    # No se pudo leer events.jsonl (infra): no bloqueamos en loop — soft-pass con aviso.
    sys.stderr.write("[gate_curation] events unreadable, soft-pass: %s\n" % err)
    print(json.dumps({"route": "passed", "curation_tool_called": "unknown",
                      "gate_curation_note": "events unreadable: %s" % err}))
else:
    sys.stderr.write("[gate_curation] get_suggested_lemmas called=%s → %s\n"
                     % (called, "passed" if called else "failed (retry curation)"))
    print(json.dumps({"route": "passed" if called else "failed",
                      "curation_tool_called": called}))
PY
