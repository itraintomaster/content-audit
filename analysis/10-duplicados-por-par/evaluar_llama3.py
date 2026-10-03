#!/usr/bin/env python3
"""llama3:8b (Ollama, local y gratis) sobre los pares inventados, con la pregunta de sí o no de
OllamaYesNoModel: un token, temperatura 0, semilla 1, las 20 alternativas del primer token; P(sí)
es la suma de «yes»/«y» y P(no) la de «no»/«n». También el coseno de nomic-embed-text de los dos
ejercicios terminados, para ver que los pares caen donde caen los candidatos del curso."""
import json
import math
import sys
import time
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
CORPUS = json.loads((HERE.parents[2] / ".sentinel/agents/near-duplicates-pair-judge/eval/pares-inventados.json").read_text(encoding="utf-8"))
URL = "http://localhost:11434"

PROMPTS = {
    "es": (
        "Sos el juez de «duplicados parecidos» de un curso de inglés para hispanohablantes. Te llegan dos "
        "ejercicios del mismo tema, cada uno como lo ve el alumno —el enunciado con ___ en el hueco, la pista "
        "entre paréntesis y, si es de opción múltiple, sus opciones— y resuelto. Los dos piden la misma "
        "respuesta. Decí si para el alumno es el mismo ejercicio: si el segundo es la misma oración que el "
        "primero con cambios que no cambian lo que el alumno resuelve (otro sujeto, una palabra de más o de "
        "menos, un sinónimo fuera del hueco, otro orden, la negativa o la pregunta de la misma oración). No es "
        "el mismo si es otra oración, con otra idea, que pide la misma respuesta, o si la diferencia es justo "
        "lo que el tema contrasta. Contestá sólo yes o no.",
        "Tema: {title}\n\nPrimero:\n{first}\n\nSegundo:\n{second}\n\n¿Para el alumno es el mismo ejercicio? "
        "Contestá yes o no."),
    "en": (
        "You judge near-duplicate exercises in an English course for Spanish speakers. You get two "
        "fill-in-the-gap exercises from the same topic, each as the student sees it (the sentence with ___ for "
        "the gap, the hint in parentheses and, for multiple choice, the options) and solved. Both need the same "
        "answer. Say whether, for the student, they are the same exercise: the second is the same sentence as "
        "the first with changes that do not change what the student has to decide (another subject, a word "
        "added or removed, a synonym outside the gap, another word order, the negative or the question of the "
        "same sentence). They are not the same exercise when the second is a different sentence with a "
        "different idea that happens to need the same answer, or when the difference is exactly what the topic "
        "contrasts. Answer only yes or no.",
        "Topic: {title}\n\nFirst:\n{first}\n\nSecond:\n{second}\n\nFor the student, is it the same exercise? "
        "Answer yes or no."),
    "fewshot": (
        "You compare two fill-in-the-gap exercises of an English course. Answer yes only when the second "
        "exercise repeats the first one: the same sentence, maybe with another subject, a word more or less, a "
        "synonym, or turned into a question. Answer no when the second is a different sentence that only "
        "happens to need the same answer.\n\nExamples:\n"
        "First: He ___ (leave) now. Second: She ___ (leave) now. -> yes\n"
        "First: Offer: I ___ (wash) the dishes for you. Second: Offer: I ___ (wash) the dishes. -> yes\n"
        "First: They cooked lunch for ___ . Second: They prepared lunch for ___ . -> yes\n"
        "First: I ___ sorry. Second: I ___ hot. -> no\n"
        "First: That is ___ car. Second: That car is ___ . -> no\n"
        "First: They have spent a lot of time in Paris. Second: They often visit Paris. -> no\n"
        "Answer only yes or no.",
        "Topic: {title}\n\nFirst:\n{first}\n\nSecond:\n{second}\n\nDoes the second exercise repeat the first one? "
        "Answer yes or no."),
}


def post(path, body):
    request = urllib.request.Request(URL + path, data=json.dumps(body).encode("utf-8"),
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.loads(response.read().decode("utf-8"))


def embed(text):
    return post("/api/embed", {"model": "nomic-embed-text", "input": "clustering: " + text})["embeddings"][0]


def cosine(a, b):
    dot = sum(x * y for x, y in zip(a, b))
    return dot / (math.sqrt(sum(x * x for x in a)) * math.sqrt(sum(y * y for y in b)))


def yes_no(system, prompt):
    answer = post("/api/generate", {
        "model": "llama3:8b", "system": system, "prompt": prompt, "stream": False, "logprobs": True,
        "top_logprobs": 20, "options": {"temperature": 0, "num_predict": 1, "seed": 1}, "keep_alive": "10m"})
    first = (answer.get("logprobs") or [{}])[0]
    alternatives = first.get("top_logprobs") or ([first] if "token" in first else [])
    yes = sum(math.exp(a["logprob"]) for a in alternatives if a["token"].strip().lower() in ("yes", "y"))
    no = sum(math.exp(a["logprob"]) for a in alternatives if a["token"].strip().lower() in ("no", "n"))
    return yes, no


def finished(view):
    return view.split("Resuelto: ", 1)[1].strip()


def main():
    rows = []
    started = time.time()
    for case in CORPUS["cases"]:
        row = {"caseId": case["caseId"], "expected": case["expected"],
               "cosine": cosine(embed(finished(case["first"])), embed(finished(case["second"])))}
        for name, (system, template) in PROMPTS.items():
            prompt = template.format(title=case["title"], first=case["first"], second=case["second"])
            yes, no = yes_no(system, prompt)
            row[name] = (yes, no)
        rows.append(row)
    elapsed = time.time() - started
    asked = len(rows) * len(PROMPTS)
    print("consultas a llama3: %d en %.1f s (%.2f s cada una, con los vectores)" % (asked, elapsed, elapsed / asked))
    scored = [r for r in rows if r["expected"] is not None]
    print("coseno de los pares inventados: min %.3f, mediana %.3f, max %.3f" % (
        min(r["cosine"] for r in rows), sorted(r["cosine"] for r in rows)[len(rows) // 2],
        max(r["cosine"] for r in rows)))
    for name in PROMPTS:
        tp = sum(1 for r in scored if r["expected"] and r[name][0] > r[name][1])
        fn = sum(1 for r in scored if r["expected"] and not r[name][0] > r[name][1])
        fp = sum(1 for r in scored if not r["expected"] and r[name][0] > r[name][1])
        tn = sum(1 for r in scored if not r["expected"] and not r[name][0] > r[name][1])
        # AUC of P(yes) normalized: does it rank the same above the different?
        def p(r):
            y, n = r[name]
            return y / (y + n) if y + n > 0 else 0.5
        pos = [p(r) for r in scored if r["expected"]]
        neg = [p(r) for r in scored if not r["expected"]]
        auc = sum((a > b) + 0.5 * (a == b) for a in pos for b in neg) / (len(pos) * len(neg))
        print("prompt %s: dice sí a %d de %d mismos y a %d de %d distintos; aciertos %d/%d; AUC %.2f" % (
            name, tp, tp + fn, fp, fp + tn, tp + tn, len(scored), auc))
    print()
    for r in rows:
        exp = {True: "mismo", False: "otro", None: "dudoso"}[r["expected"]]
        print("%-36s %-6s cos %.3f  es %.2f/%.2f  en %.2f/%.2f" % (
            r["caseId"], exp, r["cosine"], r["es"][0], r["es"][1], r["en"][0], r["en"][1]))
    (HERE / "llama3-resultados.json").write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
