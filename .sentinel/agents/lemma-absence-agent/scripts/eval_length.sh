#!/bin/bash
# Eval #3 (R007): no-regresión de longitud, determinista y bloqueante.
# Exige que el candidato quede DENTRO del rango de longitud del NIVEL — SIEMPRE, aunque
# el audit NO haya marcado problema de longitud (lengthGuidance="none"). Antes solo se
# chequeaba cuando el audit venía con "target range" en lengthGuidance; para LEMMA_ABSENCE
# eso casi nunca pasa → el eval era NO-OP y una reescritura podía correr la longitud fuera
# de rango sin freno (hundía el agregado de sentence-length).
#
# El rango del nivel y el conteo del candidato salen del MISMO código que el audit, vía
# `content-audit tokens --level <CEFR>`: parse del DSL -> toPlainSentences canónica ->
# NlpTokenizer.countTokens (spaCy) para el conteo, y SentenceLengthConfig.getTargetRange
# (la fuente única de rangos: A1 3-8, A2 5-12, B1 8-18, B2 10-25) para el rango. Una sola
# invocación trae ambos como JSON {"tokens":N,"min":A,"max":B}.
#
# Script de cómputo: escribe `length_ok` al data bus; no rutea (route_verdict decide).
# - candidato ilegible → length_ok=False (fallo real del candidato, bloquea).
# - no se puede medir/ejecutar el tokenizer (infra) → SOFT-PASS **visible** (stderr): no
#   penaliza al candidato por infra; el audit real lo verificaría al aplicar.
# - sin rango de nivel (cefrLevel ausente/desconocido y sin rango en lengthGuidance) →
#   SOFT-PASS **visible**: preferimos no bloquear por falta de señal, pero lo logueamos
#   para que NUNCA sea silencioso.
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

# --- Nivel CEFR: para pedir el rango del nivel (fuente única = SentenceLengthConfig).
cefr_level = (inputs.get("cefrLevel", "") or "").strip().upper()
if cefr_level not in ("A1", "A2", "B1", "B2"):
    cefr_level = ""  # desconocido → no forzamos --level; caeremos a lengthGuidance/soft-pass

# --- Rango declarado en lengthGuidance (solo cuando el audit marcó longitud). Se usa
#     como FALLBACK del rango del nivel; mismo origen (SentenceLengthConfig), así que
#     coincide con el del nivel cuando ambos están.
length_guidance = inputs.get("lengthGuidance", "") or ""
m = re.search(r"target range:\s*(\d+)\s*-\s*(\d+)", length_guidance)
guidance_min = int(m.group(1)) if m else None
guidance_max = int(m.group(2)) if m else None
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

def measure(sentence, mode, level):
    """Cuenta tokens y (si level) trae el rango del nivel, con el MISMO código que el audit
    vía `content-audit tokens`. Con --level el CLI devuelve JSON {tokens,min,max}; sin él,
    el entero pelado. Devuelve (tokens|None, min|None, max|None, error|None)."""
    root = find_project_root(run_dir)
    if root is None:
        return None, None, None, "could not locate project root (audit-cli.sh) from run_dir"
    cli = os.path.join(root, "audit-cli.sh")
    args = ["bash", cli, "tokens", "--quiz-sentence", sentence]
    if mode:
        args += ["--mode", mode]
    if level:
        args += ["--level", level]
    try:
        proc = subprocess.run(args, cwd=root, capture_output=True, text=True, timeout=180)
    except Exception as e:
        return None, None, None, "tokens exec failed: %s" % e
    if proc.returncode != 0:
        return None, None, None, "tokens rc=%d: %s" % (proc.returncode, (proc.stderr or "").strip()[:300])
    lines = [ln.strip() for ln in (proc.stdout or "").splitlines() if ln.strip()]
    for line in reversed(lines):  # la última línea limpia de stdout es el resultado
        if level and line.startswith("{"):  # JSON {"tokens":N,"min":A,"max":B}
            try:
                obj = json.loads(line)
                tk = obj.get("tokens")
                if isinstance(tk, int):
                    mn = obj.get("min"); mx = obj.get("max")
                    return tk, (mn if isinstance(mn, int) else None), (mx if isinstance(mx, int) else None), None
            except Exception:
                pass
        if re.fullmatch(r"\d+", line):  # entero pelado (sin --level)
            return int(line), None, None, None
    return None, None, None, "tokens produced no parseable output: %r" % (proc.stdout or "")[:200]

mode = (inputs.get("sentenceMode", "") or "").strip().upper()
if mode not in ("FILL", "REWRITE"):
    mode = ""  # el CLI defaultea a FILL

cand_tokens = None
target_min = None
target_max = None
range_source = None
measure_err = None

if read_err is not None:
    # candidato ilegible → bloquea (es un fallo real del candidato, no de infra).
    length_ok = False
else:
    cand_tokens, lvl_min, lvl_max, measure_err = measure(candidate_sentence, mode, cefr_level)
    # Rango a enforcar: preferimos el del NIVEL (autoritativo, siempre disponible).
    # Fallback: el de lengthGuidance (mismo origen) si el CLI no lo trajo.
    if lvl_min is not None and lvl_max is not None:
        target_min, target_max, range_source = lvl_min, lvl_max, "level:%s" % (cefr_level or "?")
    elif guidance_min is not None and guidance_max is not None:
        target_min, target_max, range_source = guidance_min, guidance_max, "lengthGuidance"

    if cand_tokens is None:
        # No se pudo medir (infra) → SOFT-PASS visible.
        length_ok = True
        sys.stderr.write("eval_length: measure unavailable, SOFT-PASS (no se pudo medir el candidato): %s\n"
                         % measure_err)
    elif target_min is None or target_max is None:
        # Medimos, pero no hay rango → SOFT-PASS visible (no debería pasar si cefrLevel llega bien).
        length_ok = True
        sys.stderr.write("eval_length: candidate=%d tokens pero SIN rango (cefrLevel=%r, guidance=%r) → SOFT-PASS\n"
                         % (cand_tokens, cefr_level, length_guidance[:60]))
    else:
        length_ok = (target_min <= cand_tokens <= target_max)
        sys.stderr.write(
            "eval_length: candidate=%d tokens, target=%d-%d (%s), original=%s → %s\n" % (
                cand_tokens, target_min, target_max, range_source,
                original_tokens if original_tokens is not None else "?",
                "OK" if length_ok else "OUT OF RANGE"))

out = {
    "route": "done",
    "length_ok": length_ok,
    "length_candidate_tokens": cand_tokens if cand_tokens is not None else "",
    "length_original_tokens": original_tokens if original_tokens is not None else "",
    "length_target_min": target_min if target_min is not None else "",
    "length_target_max": target_max if target_max is not None else "",
    "length_range_source": range_source if range_source is not None else "",
}
if read_err:
    out["length_error"] = read_err
    sys.stderr.write("eval_length: candidate unreadable: %s\n" % read_err)
if measure_err:
    out["length_measure_error"] = measure_err

print(json.dumps(out))
PY
