#!/bin/bash
# Eval #3 (R007): no-regresión de longitud, determinista y bloqueante.
# Mide la longitud del candidato DENTRO del rango esperado (target range del audit,
# parseado de lengthGuidance). El candidato se cuenta con EXACTAMENTE el mismo camino
# que el audit de sentence-length: `content-audit tokens` (parse del DSL ->
# toPlainSentences canónica -> NlpTokenizer.countTokens, spaCy). Antes esto se
# aproximaba con un conteo de palabras en bash, que NO contaba, p.ej., el guion de
# "smoke - smoking" como token y por eso difería del audit (2 vs 3) → todos los
# candidatos de transformación fallaban #3. Ahora el código de conteo es el MISMO que
# usa el audit (reuse vía CLI).
#
# Script de cómputo: escribe `length_ok` al data bus; no rutea (route_verdict decide).
# Si no hay rango declarado (lengthGuidance="none") → sin restricción de longitud.
# Si el tokenizer no se puede ejecutar (infra), soft-pass (no penaliza al candidato
# por un problema de infraestructura; el audit real lo detectaría al aplicar).
#
# stdin:  JSON { "inputs": {...}, "data": {...}, "node": {...}, "run_dir": ... }
# stdout: JSON UNA línea con campos que se mergean al data bus.
# Solo stdlib de Python (pitfall #2). Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, re, sys, os, subprocess

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}

# run_dir puede llegar por varias vías según el runtime; probamos en orden.
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

# --- Rango esperado: viene en lengthGuidance ("...; target range: A-B tokens; ...").
# Sin rango (el audit no marcó problema de longitud → lengthGuidance="none") → no hay
# restricción de longitud y no hace falta tokenizar.
length_guidance = inputs.get("lengthGuidance", "") or ""
m = re.search(r"target range:\s*(\d+)\s*-\s*(\d+)", length_guidance)
target_min = int(m.group(1)) if m else None
target_max = int(m.group(2)) if m else None
mc = re.search(r"current token count:\s*(\d+)", length_guidance)
original_tokens = int(mc.group(1)) if mc else None

# --- Candidato: lo produjo `generate` como artifact en {run_dir}/candidate.json.
candidate_sentence = ""
read_err = None
try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
    candidate_sentence = (cand.get("quizSentence", "") or "").strip()
except Exception as e:  # candidato ausente/ilegible -> length_ok=False (bloquea)
    read_err = str(e)

def find_project_root(start):
    """Sube desde run_dir hasta encontrar audit-cli.sh (el wrapper del CLI)."""
    cur = os.path.abspath(start)
    for _ in range(20):
        if os.path.isfile(os.path.join(cur, "audit-cli.sh")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur:
            break
        cur = parent
    return None

def count_tokens(sentence, mode):
    """Cuenta tokens con el MISMO pipeline que el audit, vía `content-audit tokens`.
    Devuelve (count|None, error|None)."""
    root = find_project_root(run_dir)
    if root is None:
        return None, "could not locate project root (audit-cli.sh) from run_dir"
    cli = os.path.join(root, "audit-cli.sh")
    args = ["bash", cli, "tokens", "--quiz-sentence", sentence]
    if mode:
        args += ["--mode", mode]
    try:
        proc = subprocess.run(args, cwd=root, capture_output=True, text=True, timeout=180)
    except Exception as e:
        return None, "tokens exec failed: %s" % e
    if proc.returncode != 0:
        return None, "tokens rc=%d: %s" % (proc.returncode, (proc.stderr or "").strip()[:300])
    out = (proc.stdout or "").strip().splitlines()
    for line in reversed(out):  # el conteo es la última línea limpia de stdout
        line = line.strip()
        if re.fullmatch(r"\d+", line):
            return int(line), None
    return None, "tokens produced no integer on stdout: %r" % (proc.stdout or "")[:200]

mode = (inputs.get("sentenceMode", "") or "").strip().upper()
if mode not in ("FILL", "REWRITE"):
    mode = ""  # el CLI defaultea a FILL

cand_tokens = None
measure_err = None

if read_err is not None:
    # candidato ilegible → bloquea (es un fallo real del candidato, no de infra).
    length_ok = False
elif target_min is None or target_max is None:
    # sin rango declarado → sin restricción de longitud; ni siquiera tokenizamos.
    length_ok = True
else:
    cand_tokens, measure_err = count_tokens(candidate_sentence, mode)
    if cand_tokens is None:
        # No se pudo medir (infra). Soft-pass: no penalizamos al candidato por un
        # problema de tokenización; el audit real lo verificaría al aplicar.
        length_ok = True
        sys.stderr.write("eval_length: measure unavailable, soft-pass: %s\n" % measure_err)
    else:
        length_ok = (target_min <= cand_tokens <= target_max)
        sys.stderr.write(
            "eval_length: candidate=%d tokens, target=%s-%s, original=%s → %s\n" % (
                cand_tokens, target_min, target_max,
                original_tokens if original_tokens is not None else "?",
                "OK" if length_ok else "OUT OF RANGE"))

out = {
    "route": "done",
    "length_ok": length_ok,
    "length_candidate_tokens": cand_tokens if cand_tokens is not None else "",
    "length_original_tokens": original_tokens if original_tokens is not None else "",
    "length_target_min": target_min if target_min is not None else "",
    "length_target_max": target_max if target_max is not None else "",
}
if read_err:
    out["length_error"] = read_err
    sys.stderr.write("eval_length: candidate unreadable: %s\n" % read_err)
if measure_err:
    out["length_measure_error"] = measure_err

print(json.dumps(out))
PY
