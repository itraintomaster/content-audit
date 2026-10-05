#!/usr/bin/env python3
"""Publica en MongoDB los `knowledges.instructions` del árbol db/english-course.

Complemento acotado de publish_revisions.py, hermano de
publish_knowledge_labels.py, para las consignas que NO tienen un
RevisionArtifact: las que se corrigieron a mano. publish_revisions.py sólo
publica lo que tiene artefacto aprobado, así que una edición manual queda
indefinidamente sin llegar a prod y sin que nada lo señale.

Allowlist cerrada: escribe únicamente `instructions`. Nada más.

Uso:
  python3 scripts/publish_knowledge_instructions.py --uri "$URI"           # dry-run
  python3 scripts/publish_knowledge_instructions.py --uri "$URI" --apply
"""

import argparse
import glob
import json
import os

from bson import ObjectId
from pymongo import MongoClient, UpdateOne

REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def main():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--uri", default="mongodb://localhost:27017")
    p.add_argument("--db", default="learney")
    p.add_argument("--repo", default=REPO_DEFAULT)
    p.add_argument("--collection", default="knowledges")
    p.add_argument("--apply", action="store_true", help="aplica (default: dry-run)")
    args = p.parse_args()

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"== publish_knowledge_instructions [{mode}] db={args.db} ==\n")

    col = MongoClient(args.uri)[args.db][args.collection]

    pendientes = []
    leidos = 0
    patron = os.path.join(args.repo, "db", "english-course", "**", "*.json")
    for f in glob.glob(patron, recursive=True):
        if f.endswith("quizzes.json"):
            continue
        try:
            doc = json.load(open(f, encoding="utf-8"))
        except Exception:
            continue
        if not isinstance(doc, dict) or doc.get("kind") != "KNOWLEDGE":
            continue
        oid = (doc.get("_id") or {}).get("$oid")
        if not oid:
            continue
        leidos += 1
        instr = doc.get("instructions")
        if instr is None:
            continue
        actual = col.find_one({"_id": ObjectId(oid)}, {"instructions": 1})
        if actual is None:
            continue
        if (actual.get("instructions") or "") != instr:
            pendientes.append((oid, actual.get("instructions"), instr, doc.get("label")))

    print(f"knowledges leidos del arbol : {leidos}")
    print(f"pendientes de publicar      : {len(pendientes)}\n")
    for oid, viejo, nuevo, label in pendientes:
        print(f"  {oid}  {label!r}")
        print(f"     prod  : {viejo!r}")
        print(f"     arbol : {nuevo!r}")

    if not pendientes:
        print("\nNada que hacer.")
        return 0
    if not args.apply:
        print("\nDRY-RUN: no se modifico nada. Ejecutar con --apply para aplicar.")
        return 0

    ops = [UpdateOne({"_id": ObjectId(oid)}, {"$set": {"instructions": nuevo}})
           for oid, _, nuevo, _ in pendientes]
    res = col.bulk_write(ops, ordered=False)
    print(f"\n== Aplicando ==\nknowledges: matched={res.matched_count} modified={res.modified_count}")

    malos = 0
    for oid, _, nuevo, _ in pendientes:
        d = col.find_one({"_id": ObjectId(oid)}, {"instructions": 1})
        if (d or {}).get("instructions") != nuevo:
            malos += 1
            print(f"  ERROR: {oid} no quedo con el valor esperado")
    print(f"== Verificacion post-apply ==\n{'OK' if malos == 0 else 'CON ERRORES'}: "
          f"{len(pendientes) - malos}/{len(pendientes)} verificados campo por campo.")
    return 1 if malos else 0


if __name__ == "__main__":
    raise SystemExit(main())
