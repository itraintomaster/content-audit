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

Cada corrida deja su renglón en <repo>/.content-audit/production/registry.jsonl
(destino servidor/base, nunca el URI) y, antes de escribir, el respaldo de las
consignas viejas en runs/<runId>.json (F-PUBPROD-R008).
"""

import argparse
import glob
import json
import os
import sys

# pymongo y bson se importan adentro de la función que conecta: así el script
# se puede importar (y probar) sin conectarse ni tener el driver.

SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, SCRIPTS_DIR)

from production_publish import registry as production_registry  # noqa: E402

REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOL = "publish_knowledge_instructions"


def parse_args(argv=None):
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--uri", default="mongodb://localhost:27017")
    p.add_argument("--db", default="learney")
    p.add_argument("--repo", default=REPO_DEFAULT)
    p.add_argument("--collection", default="knowledges")
    p.add_argument("--apply", action="store_true", help="aplica (default: dry-run)")
    return p.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"== publish_knowledge_instructions [{mode}] db={args.db} ==\n")

    recorder = production_registry.LegacyRecorder(args.repo, TOOL, args.uri, args.db, args.apply)
    if recorder.destination is None:
        return recorder.refuse_bad_destination()
    # Una excepción de pymongo se muestra solo por su clase: su mensaje puede traer el destino.
    return recorder.run(lambda: publish_instructions(args, recorder))


def publish_instructions(args, recorder):
    from bson import ObjectId
    from pymongo import MongoClient, UpdateOne

    col = MongoClient(args.uri)[args.db][args.collection]

    pendientes = []
    leidos = 0
    patron = os.path.join(args.repo, "db", "english-course", "**", "*.json")
    for f in glob.glob(patron, recursive=True):
        if f.endswith("quizzes.json"):
            continue
        try:
            with open(f, encoding="utf-8") as fh:
                doc = json.load(fh)
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

    counts = {"knowledgesRead": leidos, "instructions": len(pendientes)}
    if not pendientes:
        print("\nNada que hacer.")
        if args.apply:
            recorder.record(production_registry.APPLIED_VERIFIED, counts=counts, writtenIds=[], verified=True)
        else:
            recorder.record(production_registry.DRY_RUN_CLEAN, counts=counts)
        return 0
    if not args.apply:
        print("\nDRY-RUN: no se modifico nada. Ejecutar con --apply para aplicar.")
        recorder.record(production_registry.DRY_RUN_CLEAN, counts=counts)
        return 0

    # F-PUBPROD-R008: el respaldo y el renglón de inicio, antes de la primera escritura.
    backup = {"note": "Respaldo: «old» es la consigna que había y «new» la que se escribe.",
              "documents": [{"collection": args.collection, "_id": oid, "label": label,
                             "old": {"instructions": viejo}, "new": {"instructions": nuevo}}
                            for oid, viejo, nuevo, label in pendientes]}
    if not recorder.write_detail(json.loads(json.dumps(backup, default=str))):
        recorder.record(production_registry.ABORTED, counts=counts, reason={
            "code": "DETAIL_NOT_WRITTEN", "message": "no se pudo escribir el respaldo; no se escribió nada"})
        print("\nABORTADO: no se pudo escribir el respaldo de la corrida; no se escribio nada.")
        return 1
    if not recorder.record(production_registry.STARTED, counts=counts):
        print("\nABORTADO: no se pudo dejar el renglon de inicio en el registro; no se escribio nada.")
        return 1

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
    written_ids = ["%s/%s" % (args.collection, oid) for oid, _, _, _ in pendientes]
    if malos:
        recorder.record(production_registry.VERIFY_FAILED, counts=counts, writtenIds=written_ids, verified=False,
                        reason={"code": "VERIFY_FAILED", "message": f"{malos} consignas no coinciden"})
        return 1
    recorder.record(production_registry.APPLIED_VERIFIED, counts=counts, writtenIds=written_ids, verified=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
