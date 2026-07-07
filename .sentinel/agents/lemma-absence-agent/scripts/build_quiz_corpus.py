#!/usr/bin/env python3
"""Pre-computa el corpus de oraciones RELLENAS de TODOS los quizzes de la db para
el eval de distinción global (eval_global_distinct). Se corre una vez (o cuando
la db cambia). Salida: .content-audit/quiz-corpus.jsonl — una linea por quiz con
{id, level, knowledgeId, plain, cwords}. `plain` = oracion normalizada (rellena,
minusculas, sin puntuacion); `cwords` = palabras de contenido (para pruning).

Usa form.sentences (la oracion ya rellena) — NO el esqueleto: para detectar
duplicados "en las palabras y la frase" hay que conservar la respuesta.

Uso: python3 build_quiz_corpus.py [course_dir] [out_path]
"""
import json, re, sys, glob, os

COURSE = sys.argv[1] if len(sys.argv) > 1 else "db/english-course"
OUT = sys.argv[2] if len(sys.argv) > 2 else ".content-audit/quiz-corpus.jsonl"

STOP = set("a an the of to in on at for with and or but is are am was were be been being do does did doing have has had having not no this that these those i you he she it we they my your his her its our their me him them us as so if then than there here up down out off over under again very just really only when where while what which who whom how why can could will would shall should may might must s t re ve ll d m o".split())

def normalize(text):
    s = (text or "").lower()
    s = re.sub(r"[^a-z0-9\s]", " ", s)
    return re.sub(r"\s+", " ", s).strip()

def cwords(norm):
    return sorted(set(w for w in norm.split() if w not in STOP and len(w) > 1))

def plain_of(q):
    form = q.get("form") or {}
    sents = form.get("sentences") or q.get("sentences") or []
    if isinstance(sents, str): sents = [sents]
    return " ".join(s for s in sents if s)

n = 0
os.makedirs(os.path.dirname(OUT) or ".", exist_ok=True)
with open(OUT, "w", encoding="utf-8") as out:
    for f in glob.glob(os.path.join(COURSE, "**", "quizzes.json"), recursive=True):
        m = re.search(re.escape(COURSE) + r"/([^/]+)/", f)
        level = m.group(1) if m else "?"
        try:
            quizzes = json.load(open(f, encoding="utf-8"))
        except Exception:
            continue
        if isinstance(quizzes, dict):
            quizzes = [quizzes]
        for q in quizzes:
            if not isinstance(q, dict):
                continue
            norm = normalize(plain_of(q))
            if not norm:
                continue
            out.write(json.dumps({"id": q.get("id") or q.get("_id"), "level": level,
                                  "plain": norm, "cwords": cwords(norm)}, ensure_ascii=False) + "\n")
            n += 1
print(f"corpus: {n} quizzes -> {OUT}")
