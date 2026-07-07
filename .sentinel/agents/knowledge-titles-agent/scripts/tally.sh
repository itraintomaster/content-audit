#!/bin/bash
# Combina veredictos deterministas (data bus) + juez (verdicts.json), mantiene
# el champion y escribe carry_forward. Publica all_blocking_pass. Solo stdlib.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os, re

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}

def _resolve_run_dir():
    for c in (d.get("run_dir"), inputs.get("run_dir"), data.get("run_dir"),
              node.get("run_dir"), os.environ.get("SENTINEL_RUN_DIR")):
        if c:
            return c
    # Runs lanzados por el server NO inyectan run_dir en inputs (el adaptador
    # Java sí). Derivación por el layout estándar {root}/.sentinel/runs/{agent}/:
    # exacta con el run_id de la proyección; si el framework desplegado aún no
    # proyecta run_id, fallback al run MÁS RECIENTE del agente (sus runs no
    # corren en paralelo; el framework crea el run dir antes de cualquier nodo).
    root = inputs.get("project_root") or inputs.get("root") or ""
    if root:
        base = os.path.join(root, ".sentinel", "runs", "knowledge-titles-agent")
        run_id = d.get("run_id") or ""
        if run_id and os.path.isdir(os.path.join(base, run_id)):
            return os.path.join(base, run_id)
        try:
            dirs = [p for p in (os.path.join(base, n) for n in os.listdir(base))
                    if os.path.isdir(p)]
            if dirs:
                newest = max(dirs, key=os.path.getmtime)
                sys.stderr.write("run_dir por fallback mtime (sin run_id): %s\n" % newest)
                return newest
        except Exception:
            pass
    return "."

run_dir = _resolve_run_dir()

def _parse_json_lenient(raw):
    """Parsea JSON tolerando ```fences```, prosa y llaves de cierre faltantes
    (mismo criterio que gate_verdicts; acá es red de seguridad para el caso
    `exhausted` en que el juez degradó sin pasar por el gate)."""
    text = (raw or "").strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        pass
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z0-9]*\s*", "", text)
        text = re.sub(r"\s*```$", "", text).strip()
        try:
            return json.loads(text)
        except Exception:
            pass
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
        if depth > 0:
            candidate = text[start:].rstrip().rstrip(",")
            try:
                return json.loads(candidate + "}" * depth)
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
verdicts = read_json("verdicts.json", None)
verdicts_readable = isinstance(verdicts, dict) and bool(verdicts)
if not verdicts_readable:
    sys.stderr.write("[eval] verdicts.json ausente/ilegible → evals LLM no evaluados (infra)\n")
    verdicts = {}

def passed(key):
    v = verdicts.get(key) or {}
    return bool(v.get("pass", False))

# Deterministas: publicados al data bus por eval_length / eval_redundancy /
# eval_language / eval_distinct. Los defaults son conservadores para los de
# longitud (False: sin medida no se emite) y permisivos para los que solo
# penalizan con señal presente (mismo criterio que lemma-absence).
blocking = {
    "eval1_title_length":        bool(data.get("title_length_ok", False)),
    "eval2_instructions_length": bool(data.get("instructions_length_ok", False)),
    "eval3_redundancy":          bool(data.get("redundancy_ok", True)),
    "eval4_language_guard":      bool(data.get("title_lang_ok", True)) and bool(data.get("instructions_lang_ok", True)),
    "eval5_distinct":            bool(data.get("distinct_ok", True)),
    "eval6_fidelity":            passed("eval6_fidelity"),
    "eval7_language":            passed("eval7_language"),
    "eval8_no_redundancy":       passed("eval8_no_redundancy"),
}
all_blocking_pass = all(blocking.values())
blocking_passed_count = sum(1 for ok in blocking.values() if ok)

_failed = [k for k, ok in blocking.items() if not ok]
sys.stderr.write("[eval] blocking passed %d/%d%s\n" % (
    blocking_passed_count, len(blocking),
    ("" if all_blocking_pass else " — FAILED: " + ", ".join(_failed))))

try:
    style = float((verdicts.get("eval9_style") or {}).get("score", 0.0))
except (TypeError, ValueError):
    style = 0.0

# Historial por intento (visibilidad de la progresión entre reintentos).
_title = candidate.get("title", "")
_instr = candidate.get("instructions", "")
sys.stderr.write("[eval]   candidato: %r | %r\n" % (_title, _instr))
try:
    with open(os.path.join(run_dir, "attempts.jsonl"), "a", encoding="utf-8") as af:
        af.write(json.dumps({
            "title": _title,
            "instructions": _instr,
            "blocking_passed": blocking_passed_count,
            "all_blocking_pass": all_blocking_pass,
            "failed": _failed,
            "style": style,
            "verdicts_readable": verdicts_readable,
        }, ensure_ascii=False) + "\n")
except Exception as e:
    sys.stderr.write("[eval]   (no pude appendear attempts.jsonl: %s)\n" % e)

# --- Champion: mejor = más bloqueantes pasados; desempate por estilo (#9).
meta = read_json("champion_meta.json", None)
better = (meta is None
          or blocking_passed_count > meta.get("blocking_passed_count", -1)
          or (blocking_passed_count == meta.get("blocking_passed_count", -1)
              and style > meta.get("style", -1.0)))
if better and candidate:
    with open(os.path.join(run_dir, "champion.json"), "w", encoding="utf-8") as f:
        json.dump(candidate, f, ensure_ascii=False)
    with open(os.path.join(run_dir, "champion_meta.json"), "w", encoding="utf-8") as f:
        json.dump({"blocking_passed_count": blocking_passed_count,
                   "style": style, "all_blocking_pass": all_blocking_pass}, f)

# --- Candidato repetido (modelo terco): comparar contra el intento previo.
_repeated = False
try:
    att = [json.loads(l) for l in open(os.path.join(run_dir, "attempts.jsonl"), encoding="utf-8") if l.strip()]
    if len(att) >= 2 and att[-1].get("title") == att[-2].get("title") \
            and att[-1].get("instructions") == att[-2].get("instructions"):
        _repeated = True
except Exception:
    _repeated = False

# --- Carry-forward: en fallo, feedback accionable para el próximo intento.
if not all_blocking_pass:
    lines = ["# Carry-forward — feedback del intento anterior", ""]
    if _repeated:
        lines.append("- 🛑 REPETISTE EL MISMO CANDIDATO que ya fue rechazado. NO lo vuelvas a "
                     "generar: cambiá el título y/o las instructions Y corregí los puntos de abajo.")
    if not blocking["eval1_title_length"]:
        lines.append("- ❌ #1 longitud del título: %.1f ponderado, el máximo es %.0f. Acortá el "
                     "título (apuntá a ≤ 24; sacá palabras que no distingan al ejercicio)."
                     % (float(data.get("title_weighted_length", -1)),
                        float(data.get("title_max", 28))))
    if not blocking["eval2_instructions_length"]:
        lines.append("- ❌ #2 longitud de instructions: %.1f ponderado, el máximo es %.0f. "
                     "Comprimí la redacción SIN quitar restricciones de la consigna."
                     % (float(data.get("instructions_weighted_length", -1)),
                        float(data.get("instructions_max", 70))))
    if not blocking["eval3_redundancy"]:
        _t = data.get("redundant_with_topic", "")
        _i = data.get("redundant_with_instructions", "")
        msg = "- ❌ #3 redundancia: el título repite palabras que el alumno ya ve"
        if _t:
            msg += " en el topic (%s)" % _t
        if _i:
            msg += (" y" if _t else " en") + " los términos citados de las instructions (%s)" % _i
        msg += ". Sacalas: el título solo dice lo que DISTINGUE a este ejercicio."
        lines.append(msg)
    if not blocking["eval4_language_guard"]:
        lines.append("- ❌ #4 idioma: %s. El título va en INGLÉS y las instructions en ESPAÑOL "
                     "(términos técnicos citados entre comillas simples)."
                     % (data.get("lang_detail") or "mezcla de idiomas detectada"))
    if not blocking["eval5_distinct"]:
        _near = data.get("distinct_nearest", "")
        _sim = data.get("distinct_max_similarity", "")
        msg = "- ❌ #5 distinción: el título es demasiado parecido al de un ejercicio hermano"
        if _sim != "":
            msg += " (similitud %s)" % _sim
        if _near:
            msg += ": «%s»" % _near
        msg += ". Elegí OTRO rasgo distintivo del ejercicio para el título."
        lines.append(msg)
    if verdicts_readable:
        for key, label in [("eval6_fidelity", "#6 fidelidad de la consigna"),
                           ("eval7_language", "#7 idioma natural"),
                           ("eval8_no_redundancy", "#8 redundancia semántica")]:
            v = verdicts.get(key) or {}
            if not v.get("pass", False):
                lines.append("- ❌ %s: %s" % (label, v.get("reason", "(sin detalle)")))
    else:
        # Sin veredictos reales NO inventamos reproches (feedback falso
        # desorienta al generador — lección de lemma-absence-agent).
        lines.append("- ⚠️ los evals de calidad (#6/#7/#8) no pudieron ejecutarse en esta vuelta "
                     "(problema de infraestructura del juez, NO de tu candidato). Si los demás "
                     "puntos de arriba están resueltos, mantené tu candidato con ajustes mínimos.")
    if _title or _instr:
        lines.append("")
        lines.append("Candidato previo (no repitas el mismo error): "
                     + json.dumps({"title": _title, "instructions": _instr}, ensure_ascii=False))
    with open(os.path.join(run_dir, "carry_forward.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

print(json.dumps({
    "route": "done",
    "all_blocking_pass": all_blocking_pass,
    "blocking_passed_count": blocking_passed_count,
    "style": style,
}))
PY
