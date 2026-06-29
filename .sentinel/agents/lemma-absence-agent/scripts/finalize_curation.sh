#!/bin/bash
# Finaliza la curación. El POOL es determinista y sale del CATÁLOGO: se arma con los
# lemmas que la tool get_suggested_lemmas devolvió (leídos de events.jsonl), EN EL
# ORDEN en que la tool los rankeó (ausentes/subexpuestos primero, luego por COCA),
# unidos a través de todas las llamadas de curate_lemmas. El LLM NO elige ni reordena
# las palabras: solo CLASIFICA (requiredPos/requiredRegex) y puede VETAR algunas
# (campo `exclude`, con razón en `note`) — p.ej. verbos válidos pero raros como
# 'sentence'/'favourite'. Se filtra por requiredRegex (red de seguridad) y se quitan
# las vetadas, preservando el orden.
#
# Salvaguarda: si el veto vacía el pool, se ignora (no permitir que el LLM "padee" por
# la ventana de fallback). Fallback (solo si la tool no aportó nada): suggestedWords.
#
# Lee la spec del LLM de curation_spec.json (artefacto curationSpec: filtros + veto).
# Publica `curatedWords` (coma-sep, en orden) y escribe el POOL RESULTANTE en
# curated_lemmas.json (artefacto curatedLemmas + marca de curación → route_curate
# saltea la curación en reintentos). Script de cómputo: no rutea. Solo stdlib
# (pitfall #2). Debug a stderr.
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

def parse_lenient(raw):
    """Tolera ```fences``` y prosa alrededor (mismo criterio que gate_candidate/tally)."""
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
    return None

def read_json(name):
    try:
        with open(os.path.join(run_dir, name), "r", encoding="utf-8") as f:
            return parse_lenient(f.read())
    except Exception:
        return None

# --- 1) Pool primario: lemmas que devolvió la tool, en orden de aparición (ranking),
#     unidos a través de todas las llamadas (leído del log del run, events.jsonl).
tool_lemmas = []
seen = set()
try:
    with open(os.path.join(run_dir, "events.jsonl"), encoding="utf-8") as f:
        for ln in f:
            ln = ln.strip()
            if not ln:
                continue
            try:
                e = json.loads(ln)
            except Exception:
                continue
            if e.get("event") != "tool_result":
                continue
            res = e.get("result")
            if res is None:
                res = e.get("content")
            parsed = parse_lenient(res) if isinstance(res, str) else (res if isinstance(res, dict) else None)
            if not isinstance(parsed, dict):
                continue
            for sl in (parsed.get("suggestedLemmas") or []):
                lemma = (sl.get("lemma") or "").strip()
                if lemma and lemma.lower() not in seen:
                    seen.add(lemma.lower())
                    tool_lemmas.append(lemma)
except Exception as ex:
    sys.stderr.write("[curate] events.jsonl ilegible (%s); voy a fallback\n" % ex)

cur = read_json("curation_spec.json") or {}
required_regex = cur.get("requiredRegex")
exclude = set(str(w).strip().lower() for w in (cur.get("exclude") or []) if str(w).strip())
llm_words = [str(w).strip() for w in (cur.get("words") or []) if str(w).strip()]  # solo fallback legacy
reason_bits = []

def apply_regex(lst):
    if not required_regex:
        return list(lst)
    try:
        pat = re.compile(required_regex, re.IGNORECASE)
    except re.error as e:
        reason_bits.append("requiredRegex inválido /%s/ (%s); ignorado" % (required_regex, e))
        return list(lst)
    kept = [w for w in lst if pat.search(w)]
    dropped = [w for w in lst if not pat.search(w)]
    if dropped:
        reason_bits.append("regex /%s/ descartó: %s" % (required_regex, ", ".join(dropped)))
    return kept

source = None
pool = []

if tool_lemmas:
    source = "tool"
    filtered = apply_regex(tool_lemmas)
    after_veto = [w for w in filtered if w.lower() not in exclude]
    vetoed = [w for w in filtered if w.lower() in exclude]
    if vetoed:
        reason_bits.append("LLM vetó: %s" % ", ".join(vetoed))
    if after_veto:
        pool = after_veto
    elif filtered:
        # El veto vació el pool: lo ignoramos (no dejar que el veto fuerce el fallback).
        pool = filtered
        reason_bits.append("veto vaciaba el pool → ignorado")

# Fallback (solo si la tool no aportó nada): legacy words del LLM, luego suggestedWords.
if not pool and llm_words:
    source = "llm_words(fallback)"
    pool = apply_regex(llm_words)
    reason_bits.append("sin lemmas de la tool; fallback a words del LLM")
if not pool:
    fb = [w.strip() for w in (inputs.get("suggestedWords", "") or "").split(",") if w.strip()]
    if fb:
        source = "suggestedWords(fallback)"
        pool = fb
        reason_bits.append("fallback a suggestedWords")

curation_ok = bool(pool) and source == "tool"
curated_words = ", ".join(pool)
# Artefacto `curatedLemmas`: el POOL RESULTANTE de la curación (los lemmas que
# realmente salieron, en orden). También es la marca de curación-hecha que lee
# load_context (route_curate saltea la curación en reintentos si este archivo
# tiene words). Distinto de `curation_spec.json` (lo que pidió el LLM: filtros+veto).
final = {"words": pool, "count": len(pool), "source": source,
         "excluded": sorted(exclude), "requiredPos": cur.get("requiredPos"),
         "requiredRegex": required_regex, "exerciseType": cur.get("exerciseType")}
if source != "tool":
    final["fallback"] = True

reason = "; ".join(reason_bits)
sys.stderr.write("[curate] source=%s words=%d%s\n"
                 % (source, len(pool), (" — " + reason) if reason else ""))

final_str = json.dumps(final, ensure_ascii=False)
# Escribimos el pointer directamente para que load_context (próximo pase) vea la marca
# de curación-hecha de inmediato (el cache de route_curate depende de esto).
with open(os.path.join(run_dir, "curated_lemmas.json"), "w", encoding="utf-8") as f:
    f.write(final_str)
# Y además emitimos la clave == kind (`curatedLemmas`) en stdout: así el framework
# REGISTRA el artifact del nodo `script` (lo escribe al pointer + state.artifacts).
# `curatedWords`/`curation_ok` van al data bus.
print(json.dumps({"route": "done", "curatedWords": curated_words,
                  "curation_ok": curation_ok,
                  "curatedLemmas": final_str},
                 ensure_ascii=False))
PY
