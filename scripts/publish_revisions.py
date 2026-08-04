#!/usr/bin/env python3
"""Publica en MongoDB las revisiones APROBADAS de un plan de content-audit.

Traduce decisiones ya tomadas y auditadas (RevisionArtifacts con verdict
APPROVED) a updates quirúrgicos sobre las colecciones `knowledges` y
`quizTemplates`. No decide nada por su cuenta:

  - QUÉ documentos tocar: sale de los artefactos aprobados del plan
    (.content-audit/revisions/<plan>/). Cada update queda trazado a su
    proposalId.
  - VALORES finales: salen del árbol db/english-course/ (writer format,
    verificado 100% contra los elementAfter aprobados).

Campos que escribe (allowlist cerrada):
  knowledges:     label, instructions
  quizTemplates:  title, translation, form.sentences, form.sentenceParts

Campos que NUNCA toca (el backend Go los usa y el round-trip de la entidad
de content-audit los anula: form.kind es el discriminador del polimorfismo
de QuizForm):
  form.kind, form.incidence, form.label, form.name, y todo lo demás.

Reparación conocida: en sentenceParts el save de content-audit convierte
text:"" en text:null; acá se restaura "" (el backend declara Text string
sin puntero).

Uso:
  python3 scripts/publish_revisions.py                # dry-run (solo lee)
  python3 scripts/publish_revisions.py --apply        # aplica y verifica
  python3 scripts/publish_revisions.py --uri mongodb://localhost:27017 \
      --db learney --plan 2026-07-03T02-00-25
"""

import argparse
import glob
import json
import os
import sys
from collections import Counter
from datetime import datetime, timezone

from bson import json_util
from pymongo import MongoClient, UpdateOne

REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

QUIZ_SET_FIELDS = ("title", "translation")  # top-level directos
FORM_SET_FIELDS = ("sentences", "sentenceParts")  # bajo form.*
FORM_PROTECTED = ("kind", "incidence", "label", "name")


def parse_args():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--uri", default="mongodb://localhost:27017")
    p.add_argument("--db", default="learney")
    p.add_argument("--plan", default="2026-07-03T02-00-25")
    p.add_argument("--repo", default=REPO_DEFAULT)
    p.add_argument("--knowledges-collection", default="knowledges")
    p.add_argument("--quizzes-collection", default="quizTemplates")
    p.add_argument("--apply", action="store_true", help="aplica los updates (default: dry-run)")
    p.add_argument("--no-sync-quiz-titles", action="store_true",
                   help="omite la fase de sincronización quiz.title := knowledge.label")
    return p.parse_args()


def json_escape(s):
    return json.dumps(s, ensure_ascii=False)[1:-1]


def sync_quiz_titles(repo, db, quizzes_collection, apply_changes):
    """quizTemplates.title es una copia desnormalizada del label del knowledge
    (verificado 2026-07-15: 10165/11487 eran copias literales del label 2025).
    Esta fase re-deriva el título de CADA quiz desde el label actual de su
    knowledge — en los archivos db/ y en Mongo — y sincroniza las copias
    embebidas en las instancias de la colección `quizzes` (template.title /
    template.instructions). Devuelve (stats, backup_entries)."""
    from bson import ObjectId

    course_dir = os.path.join(repo, "db", "english-course")
    stats = Counter()
    backup = []
    mongo_ops = []
    col = db[quizzes_collection]
    mongo_titles = {d["_id"]: d.get("title") for d in col.find({}, {"title": 1})}

    for kf in glob.glob(os.path.join(course_dir, "**", "_knowledge.json"), recursive=True):
        with open(kf, encoding="utf-8") as fh:
            label = (json_util.loads(fh.read())).get("label")
        qf = os.path.join(os.path.dirname(kf), "quizzes.json")
        if not label or not os.path.exists(qf):
            continue
        with open(qf, encoding="utf-8") as fh:
            raw = fh.read()
        quizzes = json_util.loads(raw)
        replaced_in_file = set()  # un replace-all cubre los títulos repetidos del archivo
        for q in quizzes:
            old = q.get("title")
            if old == label:
                stats["title_already_synced"] += 1
            else:
                stats["title_file_updates"] += 1
                if apply_changes and old not in replaced_in_file:
                    pat = '"title" : "%s"' % json_escape(old)
                    repl = '"title" : "%s"' % json_escape(label)
                    if pat not in raw:
                        stats["title_file_pattern_miss"] += 1
                    else:
                        raw = raw.replace(pat, repl)
                        replaced_in_file.add(old)
            # El estado de Mongo se evalúa SIEMPRE, con independencia de si el
            # archivo ya estaba sincronizado: son dos destinos distintos y un
            # archivo en sync no implica un Mongo en sync (es justo lo que pasa
            # al publicar en un entorno nuevo desde archivos ya sincronizados).
            if q["_id"] in mongo_titles and mongo_titles[q["_id"]] != label:
                backup.append({"_id": str(q["_id"]), "oldTitle": mongo_titles[q["_id"]],
                               "newTitle": label})
                mongo_ops.append(UpdateOne({"_id": q["_id"]}, {"$set": {"title": label}}))
        if apply_changes:
            with open(qf, "w", encoding="utf-8") as fh:
                fh.write(raw)
            json_util.loads(open(qf, encoding="utf-8").read())  # sanity parse

    stats["title_mongo_updates"] = len(mongo_ops)
    if apply_changes and mongo_ops:
        result = col.bulk_write(mongo_ops, ordered=True)
        stats["title_mongo_modified"] = result.modified_count

    # Copias embebidas en instancias (colección `quizzes`): title + instructions
    inst_col = db["quizzes"]
    for qz in inst_col.find({}, {"template": 1}):
        t = qz.get("template") or {}
        tid = t.get("_id")
        if tid is None:
            continue
        if not isinstance(tid, ObjectId):
            try:
                tid = ObjectId(str(tid))
            except Exception:
                continue
        master = col.find_one({"_id": tid}, {"title": 1, "instructions": 1})
        if not master:
            continue
        sets = {}
        for field in ("title", "instructions"):
            if master.get(field) != t.get(field):
                sets["template.%s" % field] = master.get(field)
        if sets:
            stats["embedded_instances_synced"] += 1
            if apply_changes:
                inst_col.update_one({"_id": qz["_id"]}, {"$set": sets})

    return stats, backup


def load_approved_artifacts(repo, plan):
    """{target: {nodeId: [proposalIds]}} solo para verdict APPROVED."""
    base = os.path.join(repo, ".content-audit", "revisions", plan)
    if not os.path.isdir(base):
        sys.exit(f"ERROR: no existe el directorio de artefactos {base}")
    approved = {"KNOWLEDGE": {}, "QUIZ": {}}
    skipped = Counter()
    for f in sorted(glob.glob(os.path.join(base, "*.json"))):
        if f.endswith(".preview.json"):
            continue
        with open(f) as fh:
            art = json.load(fh)
        if art.get("verdict") != "APPROVED":
            skipped[art.get("verdict")] += 1
            continue
        ea = art["proposal"]["elementAfter"]
        target, node = ea["nodeTarget"], ea["nodeId"]
        if target not in approved:
            skipped[f"target:{target}"] += 1
            continue
        approved[target].setdefault(node, []).append(art["proposal"]["proposalId"])
    return approved, skipped


def load_file_state(repo):
    """Estado final verificado desde db/english-course, parseado como BSON."""
    course_dir = os.path.join(repo, "db", "english-course")
    knowledges, quizzes = {}, {}
    for kf in glob.glob(os.path.join(course_dir, "**", "_knowledge.json"), recursive=True):
        with open(kf) as fh:
            doc = json_util.loads(fh.read())
        knowledges[str(doc["_id"])] = doc
    for qf in glob.glob(os.path.join(course_dir, "**", "quizzes.json"), recursive=True):
        with open(qf) as fh:
            docs = json_util.loads(fh.read())
        for doc in docs:
            quizzes[str(doc["_id"])] = doc
    return knowledges, quizzes


def repair_sentence_parts(parts, stats):
    """text:null -> "" (artefacto conocido del round-trip de la entidad)."""
    repaired = []
    for part in parts or []:
        part = dict(part)
        if part.get("text") is None:
            part["text"] = ""
            stats["text_null_repaired"] += 1
        repaired.append(part)
    return repaired


def build_knowledge_update(file_doc, mongo_doc):
    sets = {}
    for field in ("label", "instructions"):
        if file_doc.get(field) != mongo_doc.get(field):
            sets[field] = file_doc.get(field)
    return sets


def build_quiz_update(file_doc, mongo_doc, stats):
    sets = {}
    for field in QUIZ_SET_FIELDS:
        if file_doc.get(field) != mongo_doc.get(field):
            sets[field] = file_doc.get(field)
    file_form = file_doc.get("form") or {}
    mongo_form = mongo_doc.get("form") or {}
    for field in FORM_SET_FIELDS:
        value = file_form.get(field)
        if field == "sentenceParts":
            value = repair_sentence_parts(value, stats)
        if value != mongo_form.get(field):
            sets[f"form.{field}"] = value
    return sets


def check_course_doc_labels(db, changed_labels):
    """Alerta si el doc de courses embebe algún label viejo de knowledge."""
    course = db["courses"].find_one()
    if course is None:
        return ["WARN: colección courses vacía — no se pudo chequear embedding de labels"]
    blob = json_util.dumps(course)
    hits = [old for old in changed_labels if old and json.dumps(old, ensure_ascii=False)[1:-1] in blob]
    if hits:
        return [f"WARN: el doc de courses contiene {len(hits)} labels viejos de knowledges "
                f"(muestra: {hits[:3]}) — puede desnormalizar labels y requerir update aparte"]
    return []


def main():
    args = parse_args()
    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"== publish_revisions [{mode}] plan={args.plan} db={args.db} ==\n")

    approved, skipped = load_approved_artifacts(args.repo, args.plan)
    n_k, n_q = len(approved["KNOWLEDGE"]), len(approved["QUIZ"])
    print(f"Artefactos APPROVED: {n_k} knowledges, {n_q} quizzes "
          f"(omitidos por verdict/target: {dict(skipped) or 0})")

    file_k, file_q = load_file_state(args.repo)
    print(f"Estado en archivos: {len(file_k)} knowledges, {len(file_q)} quizzes\n")

    client = MongoClient(args.uri, serverSelectionTimeoutMS=5000)
    db = client[args.db]
    names = db.list_collection_names()
    for col in (args.knowledges_collection, args.quizzes_collection):
        if col not in names:
            sys.exit(f"ERROR: colección '{col}' no existe en {args.db}. Disponibles: {sorted(names)}")
    col_k, col_q = db[args.knowledges_collection], db[args.quizzes_collection]

    stats = Counter()
    updates = {"knowledges": [], "quizTemplates": []}
    problems, samples, old_labels = [], [], []

    for node_id, proposals in sorted(approved["KNOWLEDGE"].items()):
        fdoc = file_k.get(node_id)
        if fdoc is None:
            problems.append(f"KNOWLEDGE {node_id}: sin archivo _knowledge.json ({proposals})")
            continue
        mdoc = col_k.find_one({"_id": fdoc["_id"]})
        if mdoc is None:
            problems.append(f"KNOWLEDGE {node_id}: no existe en Mongo ({proposals})")
            continue
        sets = build_knowledge_update(fdoc, mdoc)
        if not sets:
            stats["knowledge_already_equal"] += 1
            continue
        old_labels.append(mdoc.get("label"))
        updates["knowledges"].append((fdoc["_id"], sets, proposals))
        stats["knowledge_updates"] += 1
        if len(samples) < 3:
            samples.append(f"  knowledge {node_id} ({proposals[0]}): "
                           f"{ {f: (mdoc.get(f), v) for f, v in list(sets.items())[:1]} }")

    for node_id, proposals in sorted(approved["QUIZ"].items()):
        fdoc = file_q.get(node_id)
        if fdoc is None:
            problems.append(f"QUIZ {node_id}: sin entrada en quizzes.json ({proposals})")
            continue
        mdoc = col_q.find_one({"_id": fdoc["_id"]})
        if mdoc is None:
            problems.append(f"QUIZ {node_id}: no existe en Mongo ({proposals})")
            continue
        sets = build_quiz_update(fdoc, mdoc, stats)
        if not sets:
            stats["quiz_already_equal"] += 1
            continue
        forbidden = [k for k in sets if k.startswith("form.") and k.split(".", 1)[1] in FORM_PROTECTED]
        assert not forbidden, f"allowlist violada: {forbidden}"
        updates["quizTemplates"].append((fdoc["_id"], sets, proposals))
        stats["quiz_updates"] += 1
        stats.update({f"field:{k.split('.')[-1]}": 1 for k in sets})

    print("== Reporte ==")
    print(f"knowledges a actualizar : {stats['knowledge_updates']} (ya iguales: {stats['knowledge_already_equal']})")
    print(f"quizTemplates a actualizar: {stats['quiz_updates']} (ya iguales: {stats['quiz_already_equal']})")
    field_counts = {k[6:]: v for k, v in stats.items() if k.startswith("field:")}
    print(f"campos seteados: {field_counts}")
    print(f"sentenceParts con text null->'' reparados: {stats['text_null_repaired']}")
    print(f"campos protegidos (jamás tocados): form.{', form.'.join(FORM_PROTECTED)}")
    for s in samples:
        print(s)
    for warn in check_course_doc_labels(db, old_labels):
        problems.append(warn)
    if problems:
        print(f"\n!! {len(problems)} problemas/avisos:")
        for p in problems[:20]:
            print(f"  - {p}")

    report = {
        "mode": mode, "plan": args.plan, "db": args.db,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "stats": dict(stats), "problems": problems,
        "updates": {col: [{"_id": str(i), "fields": sorted(s), "proposals": pr}
                          for i, s, pr in ups] for col, ups in updates.items()},
    }
    report_dir = os.path.join(args.repo, ".content-audit", "publish-reports")
    os.makedirs(report_dir, exist_ok=True)
    report_path = os.path.join(report_dir, f"{args.plan}_{mode}_{report['timestamp'].replace(':', '-')}.json")
    with open(report_path, "w") as fh:
        json.dump(report, fh, indent=2, ensure_ascii=False)
    print(f"\nReporte guardado: {report_path}")

    if not args.apply:
        if not args.no_sync_quiz_titles:
            sync_stats, _ = sync_quiz_titles(
                args.repo, db, args.quizzes_collection, apply_changes=False)
            print(f"\n[dry-run] sync quiz.title := knowledge.label pendiente: "
                  f"{sync_stats['title_file_updates']} en archivos, "
                  f"{sync_stats['title_mongo_updates']} en Mongo, "
                  f"{sync_stats['embedded_instances_synced']} instancias embebidas")
        print("\nDRY-RUN: no se modificó nada. Ejecutar con --apply para aplicar.")
        return

    hard_problems = [p for p in problems if not p.startswith("WARN")]
    if hard_problems:
        sys.exit(f"\nABORTADO: {len(hard_problems)} problemas duros; resolver antes de --apply.")

    print("\n== Aplicando ==")
    for col_name, col in (("knowledges", col_k), ("quizTemplates", col_q)):
        ops = [UpdateOne({"_id": _id}, {"$set": sets}) for _id, sets, _ in updates[col_name]]
        if not ops:
            continue
        result = col.bulk_write(ops, ordered=True)
        print(f"{col_name}: matched={result.matched_count} modified={result.modified_count}")

    print("\n== Verificación post-apply ==")
    failures = 0
    for col_name, col in (("knowledges", col_k), ("quizTemplates", col_q)):
        for _id, sets, proposals in updates[col_name]:
            mdoc = col.find_one({"_id": _id})
            for path, expected in sets.items():
                actual = mdoc
                for key in path.split("."):
                    actual = (actual or {}).get(key)
                if actual != expected:
                    failures += 1
                    print(f"  FAIL {col_name} {_id} {path} ({proposals[0]})")
    total = len(updates["knowledges"]) + len(updates["quizTemplates"])
    if failures:
        sys.exit(f"VERIFICACIÓN FALLÓ: {failures} campos no coinciden.")
    print(f"OK: {total} documentos verificados campo por campo contra el estado esperado.")

    if not args.no_sync_quiz_titles:
        print("\n== Sync quiz.title := knowledge.label ==")
        sync_stats, sync_backup = sync_quiz_titles(
            args.repo, db, args.quizzes_collection, apply_changes=True)
        print(f"títulos actualizados en archivos: {sync_stats['title_file_updates']} "
              f"(ya en sync: {sync_stats['title_already_synced']})")
        print(f"títulos actualizados en Mongo: {sync_stats.get('title_mongo_modified', 0)} "
              f"de {sync_stats['title_mongo_updates']}")
        print(f"instancias embebidas sincronizadas: {sync_stats['embedded_instances_synced']}")
        if sync_stats.get("title_file_pattern_miss"):
            print(f"!! patrones de título no hallados en archivo: {sync_stats['title_file_pattern_miss']}")
        report["quizTitleSync"] = {"stats": dict(sync_stats), "oldTitles": sync_backup}
        with open(report_path, "w") as fh:
            json.dump(report, fh, indent=2, ensure_ascii=False)
        print(f"Reporte actualizado con respaldo de títulos viejos: {report_path}")


if __name__ == "__main__":
    main()
