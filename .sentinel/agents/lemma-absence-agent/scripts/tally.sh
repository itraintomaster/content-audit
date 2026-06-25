#!/bin/bash
# Combina veredictos (eval #3 longitud via data.length_ok + evals LLM en
# verdicts.json), mantiene el champion (R009) y escribe carry_forward (R010).
# Publica `all_blocking_pass` al data bus. Solo stdlib (pitfall #2).
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os, re

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}
run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

def _parse_json_lenient(raw):
    """Parsea JSON tolerando ```fences``` y prosa alrededor (mismo criterio que
    gate_candidate.sh para candidate.json). El juez (eval_quality) a veces
    envuelve el veredicto en ```json pese a que el prompt lo prohíbe; sin esto,
    json.load falla, read_json devuelve {} y TODOS los evals LLM se cuentan como
    reprobados → retry espurio aunque el juez los haya aprobado."""
    text = (raw or "").strip()
    if not text:
        return None
    # 1) intento directo (caso normal: JSON limpio).
    try:
        return json.loads(text)
    except Exception:
        pass
    # 2) sacar code fences ```lang ... ```.
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z0-9]*\s*", "", text)
        text = re.sub(r"\s*```$", "", text).strip()
        try:
            return json.loads(text)
        except Exception:
            pass
    # 3) extraer el primer objeto {...} balanceado (tolera prosa alrededor).
    start = text.find("{")
    if start != -1:
        depth = 0
        for i in range(start, len(text)):
            c = text[i]
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    try:
                        return json.loads(text[start:i + 1])
                    except Exception:
                        return None
    return None

def read_json(name, default=None):
    try:
        with open(os.path.join(run_dir, name), "r", encoding="utf-8") as f:
            raw = f.read()
    except Exception:
        return default
    parsed = _parse_json_lenient(raw)
    return parsed if parsed is not None else default

candidate = read_json("candidate.json", {}) or {}
verdicts  = read_json("verdicts.json", {}) or {}

# length_ok lo publicó eval_length al data bus.
length_ok = bool(data.get("length_ok", False))

def passed(key):
    v = verdicts.get(key) or {}
    return bool(v.get("pass", False))

# Bloqueantes: #1 sense, #2 solvable, #3 length, #5 translation, #6 finite-reuse.
# (#4 deseable.) #6 auto-pasa (N/A) cuando la respuesta no es de conjunto finito
# o reutilizarla no daría un ejercicio válido — el juez lo emite como pass:true.
blocking = {
    "eval1_sense":        passed("eval1_sense"),
    "eval2_solvable":     passed("eval2_solvable"),
    "eval3_length":       length_ok,
    "eval5_translation":  passed("eval5_translation"),
    "eval6_finite_reuse": passed("eval6_finite_reuse"),
}
all_blocking_pass = all(blocking.values())
blocking_passed_count = sum(1 for ok in blocking.values() if ok)

# Visibilidad (#3): resumen a stderr -> aparece en el output del CLI/UI sin
# tener que bucear events.jsonl. Lista qué bloqueantes reprobaron.
_failed = [k for k, ok in blocking.items() if not ok]
sys.stderr.write(
    "[eval] blocking passed %d/%d%s\n" % (
        blocking_passed_count, len(blocking),
        ("" if all_blocking_pass else " — FAILED: " + ", ".join(_failed)),
    )
)
for _k in _failed:
    _v = verdicts.get(_k) or {}
    _reason = _v.get("reason") if isinstance(_v, dict) else None
    if _k == "eval3_length":
        _tmin, _tmax = data.get("length_target_min", ""), data.get("length_target_max", "")
        if _tmin != "" and _tmax != "":
            _reason = "length %s words out of expected range %s-%s" % (
                data.get("length_candidate_words", "?"), _tmin, _tmax)
        else:
            _reason = "length %s words does not meet the expected length" % data.get("length_candidate_words", "?")
    sys.stderr.write("[eval]   - %s: %s\n" % (_k, _reason or "(no reason)"))

try:
    pragmatism = float((verdicts.get("eval4_pragmatism") or {}).get("score", 0.0))
except (TypeError, ValueError):
    pragmatism = 0.0

# Historial por intento (visibilidad): cada evaluación deja una línea en
# attempts.jsonl con el candidato evaluado + resultado, así se ve la progresión
# completa (¿cambió entre reintentos? ¿se alargó?) — candidate.json solo guarda
# el último. También logueamos el quizSentence a stderr.
_qs = candidate.get("quizSentence", "")
sys.stderr.write("[eval]   candidate: %s\n" % _qs)
try:
    with open(os.path.join(run_dir, "attempts.jsonl"), "a", encoding="utf-8") as _af:
        _af.write(json.dumps({
            "quizSentence": _qs,
            "translation": candidate.get("translation", ""),
            "blocking_passed": blocking_passed_count,
            "all_blocking_pass": all_blocking_pass,
            "failed": _failed,
            "pragmatism": pragmatism,
        }, ensure_ascii=False) + "\n")
except Exception as _e:
    sys.stderr.write("[eval]   (could not append attempts.jsonl: %s)\n" % _e)

# --- Champion (R009): mejor = más bloqueantes pasados; desempate por pragmatismo.
meta = read_json("champion_meta.json", None)
better = (meta is None
          or blocking_passed_count > meta.get("blocking_passed_count", -1)
          or (blocking_passed_count == meta.get("blocking_passed_count", -1)
              and pragmatism > meta.get("pragmatism", -1.0)))
if better and candidate:
    with open(os.path.join(run_dir, "champion.json"), "w", encoding="utf-8") as f:
        json.dump(candidate, f, ensure_ascii=False)
    with open(os.path.join(run_dir, "champion_meta.json"), "w", encoding="utf-8") as f:
        json.dump({"blocking_passed_count": blocking_passed_count,
                   "pragmatism": pragmatism, "all_blocking_pass": all_blocking_pass}, f)

# --- Detección de candidato repetido (modelo terco): comparar contra el intento
# previo registrado en attempts.jsonl (el actual ya se appendeó arriba, así que el
# previo es la penúltima línea).
_repeated = False
try:
    _att = [json.loads(l) for l in open(os.path.join(run_dir, "attempts.jsonl"), encoding="utf-8") if l.strip()]
    if len(_att) >= 2 and _att[-1].get("quizSentence") == _att[-2].get("quizSentence"):
        _repeated = True
except Exception:
    _repeated = False

# --- Carry-forward (R010): en fallo, dejá feedback para el próximo intento.
if not all_blocking_pass:
    lines = ["# Carry-forward — feedback del intento anterior", ""]
    if _repeated:
        lines.append("- 🛑 REPETISTE EL MISMO CANDIDATO que ya fue rechazado. NO lo vuelvas a generar: "
                     "cambiá la oración (otro sujeto/contexto/vocabulario) Y corregí los puntos de abajo.")
    if not length_ok:
        _cw = data.get('length_candidate_words', '?')
        _tmin, _tmax = data.get('length_target_min', ''), data.get('length_target_max', '')
        if _tmin != '' and _tmax != '':
            lines.append(f"- ❌ #3 longitud: el candidato ({_cw} palabras) está FUERA del rango esperado "
                         f"({_tmin}-{_tmax}). Ajustá la longitud para que quede DENTRO del rango "
                         f"(si está por debajo, agregá contexto; si está por encima, acortá).")
        else:
            lines.append(f"- ❌ #3 longitud: el candidato ({_cw} palabras) no cumple la longitud esperada. Ajustá.")
    for key, label in [("eval1_sense","#1 sentido"),("eval2_solvable","#2 resoluble"),
                       ("eval5_translation","#5 traducción"),
                       ("eval6_finite_reuse","#6 reutilizar respuesta finita")]:
        v = verdicts.get(key) or {}
        if not v.get("pass", False):
            lines.append(f"- ❌ {label}: {v.get('reason','(sin detalle)')}")
    prev = candidate.get("quizSentence", "")
    if prev:
        lines.append("")
        lines.append(f"Candidato previo (no repitas el mismo error): {prev}")
    with open(os.path.join(run_dir, "carry_forward.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

print(json.dumps({
    "route": "done",
    "all_blocking_pass": all_blocking_pass,
    "blocking_passed_count": blocking_passed_count,
    "pragmatism": pragmatism,
}))
PY
