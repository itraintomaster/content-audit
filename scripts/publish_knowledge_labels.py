#!/usr/bin/env python3
"""Publica en MongoDB los `knowledges.label` del árbol db/english-course.

Complemento acotado de publish_revisions.py para el tramo de labels que NO
tiene un RevisionArtifact sobreviviente (rondas cuyos artefactos se
archivaron o purgaron). Es necesario porque la fase de sync de títulos de
publish_revisions.py deriva `quizTemplates.title := knowledge.label` leyendo
el label del ARCHIVO: si ese label no se publica también en la colección
`knowledges`, el destino queda internamente inconsistente — el título del
quiz muestra el label nuevo y el knowledge sigue con el viejo.

Allowlist cerrada: escribe únicamente `label`. Nada más.

Uso:
  python3 scripts/publish_knowledge_labels.py --uri "$URI"           # dry-run
  python3 scripts/publish_knowledge_labels.py --uri "$URI" --apply
"""

import argparse
import glob
import json
import os
from datetime import datetime, timezone

from bson import json_util
from pymongo import MongoClient, UpdateOne

REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def parse_args():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--uri", default="mongodb://localhost:27017")
    p.add_argument("--db", default="learney")
    p.add_argument("--repo", default=REPO_DEFAULT)
    p.add_argument("--collection", default="knowledges")
    p.add_argument("--apply", action="store_true", help="aplica (default: dry-run)")
    return p.parse_args()


def main():
    args = parse_args()
    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"== publish_knowledge_labels [{mode}] db={args.db} ==\n")

    course_dir = os.path.join(args.repo, "db", "english-course")
    col = MongoClient(args.uri, serverSelectionTimeoutMS=20000)[args.db][args.collection]

    pending, missing = [], []
    for kf in sorted(glob.glob(os.path.join(course_dir, "**", "_knowledge.json"), recursive=True)):
        doc = json_util.loads(open(kf, encoding="utf-8").read())
        label = doc.get("label")
        if not label:
            continue
        current = col.find_one({"_id": doc["_id"]}, {"label": 1})
        if current is None:
            missing.append(str(doc["_id"]))
            continue
        if current.get("label") != label:
            pending.append({"_id": doc["_id"], "oldLabel": current.get("label"), "newLabel": label})

    print(f"labels a actualizar: {len(pending)}")
    if missing:
        print(f"!! {len(missing)} knowledges del árbol no existen en la base: {missing[:5]}")
    for e in pending[:5]:
        print(f"  {e['oldLabel']!r} -> {e['newLabel']!r}")

    if not args.apply:
        print("\nDRY-RUN: no se modificó nada. Ejecutar con --apply para aplicar.")
        return

    if pending:
        res = col.bulk_write([UpdateOne({"_id": e["_id"]}, {"$set": {"label": e["newLabel"]}})
                              for e in pending], ordered=True)
        print(f"\nmatched={res.matched_count} modified={res.modified_count}")

    failures = [e for e in pending
                if (col.find_one({"_id": e["_id"]}, {"label": 1}) or {}).get("label") != e["newLabel"]]
    print(f"verificación: {'OK' if not failures else f'FALLARON {len(failures)}'} "
          f"({len(pending)} documentos)")

    ts = datetime.now(timezone.utc).isoformat()
    report_dir = os.path.join(args.repo, ".content-audit", "publish-reports")
    os.makedirs(report_dir, exist_ok=True)
    path = os.path.join(report_dir, f"knowledge-labels_{mode}_{ts.replace(':', '-')}.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump({"mode": mode, "db": args.db, "timestamp": ts, "count": len(pending),
                   "missing": missing,
                   "oldLabels": [{"_id": str(e["_id"]), "oldLabel": e["oldLabel"],
                                  "newLabel": e["newLabel"]} for e in pending]},
                  fh, indent=2, ensure_ascii=False)
    print(f"Reporte con respaldo de labels viejos: {path}")


if __name__ == "__main__":
    main()
