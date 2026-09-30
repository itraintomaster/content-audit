#!/usr/bin/env python3
"""Copia el árbol del curso proyectando la opción múltiple como CLOZE, sólo para medirlo.

PROVISORIO. El modelo Java de content-audit todavía no conoce MULTIPLE_CHOICE: en
esos ejercicios la respuesta vive en form.items y el hueco no trae options, así
que `analyze` corta en el primero ("un CLOZE requiere al menos una respuesta
aceptada"). Mientras eso no se resuelva con un requirement, este script arma una
copia del árbol en la que cada ejercicio de opción múltiple lleva en su hueco la
opción correcta (el item con incidence > 0) y su form.sentences derivada con el
mismo código que usa content-audit. Nada más cambia.

La copia es para correr `analyze` y `plan`, nunca para `revise`, `approve` ni
`repair`: esos guardarían el ejercicio como CLOZE. db/ no se toca.

Para comparar antes y después, hay que medir las dos versiones con esta misma
proyección.

Uso:
  python3 scripts/project_multiple_choice.py <curso-origen> <curso-destino>
  python3 scripts/project_multiple_choice.py db/english-course .content-audit/proyeccion-mc/english-course
"""

import argparse
import json
import os
import shutil
import sys
from collections import Counter

sys.dont_write_bytecode = True  # no dejar scripts/__pycache__ al importar el importador
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from import_backup import derive_sentences, jackson_dumps  # noqa: E402


def correct_labels(form):
    return [item["label"] for item in form.get("items") or []
            if float((item.get("incidence") or {}).get("$numberDouble", 0)) > 0]


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("source", help="árbol del curso (p. ej. db/english-course)")
    p.add_argument("target", help="carpeta nueva para la copia proyectada")
    args = p.parse_args()

    source, target = os.path.abspath(args.source), os.path.abspath(args.target)
    if os.path.exists(target):
        sys.exit("El destino ya existe: %s" % target)
    shutil.copytree(source, target)

    stats = Counter()
    pending = []  # (ruta de quizzes.json, índice, id)
    to_derive = []
    files = {}
    for dirpath, _dirs, names in os.walk(target):
        if "quizzes.json" not in names:
            continue
        mode = None
        if "_knowledge.json" in names:
            with open(os.path.join(dirpath, "_knowledge.json"), encoding="utf-8") as f:
                mode = json.load(f).get("sentenceMode")
        path = os.path.join(dirpath, "quizzes.json")
        with open(path, encoding="utf-8") as f:
            quizzes = json.load(f)
        files[path] = quizzes
        for i, quiz in enumerate(quizzes):
            form = quiz.get("form") or {}
            if form.get("kind") != "MULTIPLE_CHOICE":
                continue
            labels = correct_labels(form)
            gaps = [part for part in form.get("sentenceParts") or [] if part.get("kind") == "CLOZE"]
            if len(gaps) != 1 or not labels:
                sys.exit("%s: se esperaba un hueco y al menos una opción correcta (huecos=%d, correctas=%d)"
                         % (quiz["id"], len(gaps), len(labels)))
            gaps[0]["options"] = labels
            pending.append((path, i, quiz["id"]))
            to_derive.append({"id": quiz["id"], "mode": mode, "sentenceParts": form["sentenceParts"]})
            stats["opción múltiple proyectada"] += 1

    derived = derive_sentences(to_derive)
    for path, i, qid in pending:
        files[path][i]["form"]["sentences"] = derived[qid]
    for path, quizzes in files.items():
        with open(path, "w", encoding="utf-8") as f:
            f.write(jackson_dumps(quizzes))

    print("Copia proyectada en %s" % target)
    for key, value in sorted(stats.items()):
        print("  %-30s %d" % (key, value))


if __name__ == "__main__":
    main()
