#!/bin/bash
# Eval no-regresion lexica (F-LASAG-R007 criterio #7): bloqueante y DETERMINISTA.
# El candidato NO debe introducir lemas flaggeados (mal ubicados para el nivel o
# fuera de catalogo) que la oracion ORIGINAL no tuviera ya: flags(candidato) debe
# ser SUBCONJUNTO de flags(original). Los flags preexistentes que la edicion no
# tocó no bloquean (residuo inevitable no vuelve la tarea infalible); solo bloquea
# un lema flaggeado NUEVO que aparece en el candidato.
#
# Reusa el MISMO analisis lexico del audit (no una replica) via la consulta
# dedicada del CLI `content-audit lexis --quiz-sentence <DSL> --level <CEFR>
# [--mode FILL|REWRITE]` (FEAT-CLEX): misma paridad de precedente directo que
# `eval_length.sh` con `content-audit tokens`. La salida es
# {"misplacedWords":[{lemma,pos,expectedLevel,sentenceLevel}],
#  "outOfCatalogWords":[{lemma,pos,frequencyRank}]}.
#
# Original vs candidato:
# - original: inputs.quizSentence (la quizSentence ORIGINAL del CorrectionContext,
#   ya en la misma DSL que consume `lexis`/`tokens`).
# - candidato: {run_dir}/candidate.json, ya canonicalizado por gate_candidate.sh
#   (misma DSL, sin `[[...]]`, con exactamente `____ [respuesta]`).
#
# Robustez (a diferencia de eval_length, que soft-pasa si no puede MEDIR): si
# `lexis` falla (exit != 0) o no produce JSON parseable para CUALQUIERA de las
# dos oraciones, este eval NO aprueba en silencio -- bloquea (lexical_ok=False)
# con un motivo TECNICO explicito. Justificacion: este eval existe precisamente
# para cerrar el hueco de un candidato que introduce vocabulario problematico sin
# que nada lo detecte (caso live 2026-07-22, remitted->transferred); un soft-pass
# silencioso ante un fallo de la propia herramienta de deteccion reproduciria ese
# mismo hueco. Bloquear (en vez de re-lanzar `lexis` acá mismo) reutiliza el lazo
# de reintento de generacion YA existente (R008/route_verdict): el candidato NO
# verificable no se emite tal cual, y carry_forward.md deja constancia tecnica del
# motivo (no un reproche de contenido) para que el proximo intento no cambie a
# ciegas algo que quizas era correcto.
#
# NOTA de numeracion: las claves internas `eval7_distinct`/`eval8_global_distinct`
# de este grafo son ANTERIORES a la numeracion actual del catalogo de REQUIREMENT.md
# (predatan la insercion del criterio #6 y de este #7); no corresponden a ninguno
# de los 7 criterios oficiales de F-LASAG-R007 -- son gates de anti-duplicacion
# adicionales. Por eso esta clave usa `lexical_ok` (sin numero) para no sumar una
# tercera colision de "#7"; el texto de feedback SI dice "criterio #7" en prosa,
# que es lo que el retry/humano necesita leer sin ambiguedad.
#
# Script de computo (como eval_length/eval_distinct): escribe al data bus, NO
# rutea (route_verdict decide tras combinar en tally).
#
# stdin:  JSON { "inputs": {...}, "data": {...}, "node": {...}, "run_dir": ... }
# stdout: JSON UNA linea con campos que se mergean al data bus.
# Solo stdlib de Python (pitfall #2). Debug a stderr.
set -euo pipefail

INPUT="$(cat)"

python3 - "$INPUT" <<'PY'
import json, sys, os, subprocess

d = json.loads(sys.argv[1])
inputs = d.get("inputs", {}) or {}
data   = d.get("data", {}) or {}
node   = d.get("node", {}) or {}

run_dir = (d.get("run_dir") or inputs.get("run_dir") or data.get("run_dir")
           or node.get("run_dir") or os.environ.get("SENTINEL_RUN_DIR") or ".")

VALID_LEVELS = ("A1", "A2", "B1", "B2", "C1", "C2")

cefr_level = (inputs.get("cefrLevel", "") or "").strip().upper()
mode = (inputs.get("sentenceMode", "") or "").strip().upper()
if mode not in ("FILL", "REWRITE"):
    mode = ""  # el CLI defaultea a FILL

original_sentence = (inputs.get("quizSentence", "") or "").strip()

candidate_sentence = ""
read_err = None
try:
    with open(os.path.join(run_dir, "candidate.json"), "r", encoding="utf-8") as f:
        cand = json.load(f)
    candidate_sentence = (cand.get("quizSentence", "") or "").strip()
except Exception as e:  # candidato ausente/ilegible -> bloquea (fallo real, no soft-pass)
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


def query_lexis(sentence, mode, level):
    """Invoca `content-audit lexis` (FEAT-CLEX) y devuelve (flags_dict|None, error|None).
    flags_dict = {"misplacedWords": [...], "outOfCatalogWords": [...]} tal cual lo
    emite el CLI -- NO se reimplementa el analisis, solo se parsea su salida."""
    root = find_project_root(run_dir)
    if root is None:
        return None, "could not locate project root (audit-cli.sh) from run_dir"
    cli = os.path.join(root, "audit-cli.sh")
    args = ["bash", cli, "lexis", "--quiz-sentence", sentence, "--level", level]
    if mode:
        args += ["--mode", mode]
    try:
        proc = subprocess.run(args, cwd=root, capture_output=True, text=True, timeout=180)
    except Exception as e:
        return None, "lexis exec failed: %s" % e
    if proc.returncode != 0:
        return None, "lexis rc=%d: %s" % (proc.returncode, (proc.stderr or "").strip()[:300])
    lines = [ln.strip() for ln in (proc.stdout or "").splitlines() if ln.strip()]
    for line in reversed(lines):  # la ultima linea limpia de stdout es el JSON
        if line.startswith("{"):
            try:
                obj = json.loads(line)
                if isinstance(obj, dict) and "misplacedWords" in obj and "outOfCatalogWords" in obj:
                    return obj, None
            except Exception:
                pass
    return None, "lexis produced no parseable output: %r" % (proc.stdout or "")[:200]


def flag_entries(flags):
    """Aplana {misplacedWords, outOfCatalogWords} a una lista de dicts uniformes
    {lemma, pos, kind, detail} para comparar por lema y para armar el mensaje de
    feedback. `kind` in {"misplaced", "out_of_catalog"}."""
    entries = []
    for mw in (flags.get("misplacedWords") or []):
        lemma = (mw.get("lemma") or "").strip().lower()
        if not lemma:
            continue
        entries.append({
            "lemma": lemma,
            "pos": mw.get("pos"),
            "kind": "misplaced",
            "expectedLevel": mw.get("expectedLevel"),
            "sentenceLevel": mw.get("sentenceLevel"),
        })
    for oc in (flags.get("outOfCatalogWords") or []):
        lemma = (oc.get("lemma") or "").strip().lower()
        if not lemma:
            continue
        entries.append({
            "lemma": lemma,
            "pos": oc.get("pos"),
            "kind": "out_of_catalog",
            "frequencyRank": oc.get("frequencyRank"),
        })
    return entries


def describe(entry):
    """Mensaje legible para carry-forward, con nivel/rango — nombra la infractora
    con su nivel, como pide el criterio #7."""
    lemma = entry["lemma"]
    if entry["kind"] == "misplaced":
        return "%s es %s, el quiz es %s — usá una palabra de %s que colocacione" % (
            lemma, entry.get("expectedLevel"), entry.get("sentenceLevel"), entry.get("sentenceLevel"))
    rank = entry.get("frequencyRank")
    rank_txt = ("frequency rank %s" % rank) if rank is not None else "sin ranking de frecuencia conocido"
    return "%s está fuera de catálogo (%s) — no la introduzcas si el original no la tenía" % (lemma, rank_txt)


lexical_ok = True
new_flags_detail = []
lexical_error = None
original_flag_count = 0
candidate_flag_count = 0

if read_err is not None:
    lexical_ok = False
    lexical_error = "candidate.json unreadable: %s" % read_err
    sys.stderr.write("eval_lexical: %s\n" % lexical_error)
elif cefr_level not in VALID_LEVELS:
    # Sin nivel valido no hay contra que resolver la mal-ubicacion (paridad con
    # F-CLEX-R007). El contexto siempre trae cefrLevel; si falta es una señal de
    # infra/config ausente, no un defecto del candidato -> SOFT-PASS VISIBLE
    # (igual criterio que eval_length ante rango de nivel ausente).
    lexical_ok = True
    sys.stderr.write("eval_lexical: cefrLevel ausente/invalido (%r) → SOFT-PASS\n" % cefr_level)
else:
    orig_flags, orig_err = query_lexis(original_sentence, mode, cefr_level) if original_sentence else ({"misplacedWords": [], "outOfCatalogWords": []}, None)
    cand_flags, cand_err = query_lexis(candidate_sentence, mode, cefr_level) if candidate_sentence else (None, "candidate quizSentence is empty")

    if orig_err is not None or cand_err is not None:
        # F-LASAG-R007 #7: un fallo de la herramienta de verificacion NO se
        # confunde con una oracion limpia -- bloquea con motivo tecnico (ver
        # comentario de cabecera). Dispara un retry de generacion (R008) via el
        # mismo camino que cualquier otro bloqueante; nunca falla el consumidor
        # (R009 best-effort sigue intacto).
        lexical_ok = False
        lexical_error = "lexis query failed — original: %s | candidate: %s" % (
            orig_err or "ok", cand_err or "ok")
        sys.stderr.write("eval_lexical: %s\n" % lexical_error)
    else:
        original_entries = flag_entries(orig_flags)
        candidate_entries = flag_entries(cand_flags)
        original_lemmas = {e["lemma"] for e in original_entries}
        candidate_lemmas = {e["lemma"] for e in candidate_entries}
        original_flag_count = len(original_entries)
        candidate_flag_count = len(candidate_entries)

        new_lemmas = candidate_lemmas - original_lemmas
        new_entries = [e for e in candidate_entries if e["lemma"] in new_lemmas]
        # Un lema puede aparecer en ambas listas (misplaced + out-of-catalog) tras
        # dedupe por texto; nos quedamos con una entrada por lema para el mensaje.
        seen = set()
        for e in new_entries:
            if e["lemma"] in seen:
                continue
            seen.add(e["lemma"])
            new_flags_detail.append(describe(e))

        lexical_ok = len(new_lemmas) == 0
        sys.stderr.write(
            "eval_lexical: original_flags=%d candidate_flags=%d new=%s → %s\n" % (
                original_flag_count, candidate_flag_count,
                sorted(new_lemmas) if new_lemmas else "[]",
                "OK" if lexical_ok else "REGRESSION"))

out = {
    "route": "done",
    "lexical_ok": lexical_ok,
    "lexical_original_flag_count": original_flag_count,
    "lexical_candidate_flag_count": candidate_flag_count,
    "lexical_new_flags": "; ".join(new_flags_detail),
}
if lexical_error:
    out["lexical_error"] = lexical_error

print(json.dumps(out, ensure_ascii=False))
PY
