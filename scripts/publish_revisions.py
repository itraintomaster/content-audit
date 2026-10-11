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
y, salvo --no-sync-quiz-titles, la sincronización de títulos:
  quizTemplates:  title := label de su knowledge
  quizzes:        template.title, template.instructions (copias del original)
Todo lo que escribe, la sincronización incluida, queda con su «old» y su «new» en
el respaldo antes de la primera escritura.

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

Tandas: el plan de una tanda publicada NO se aplica por este camino (se niega y
queda en el registro). Una tanda va a producción con su lista de publicación:
  python3 scripts/publish_revisions.py --batch <id> --workdir <campaña> --uri "$URI"
  python3 scripts/publish_revisions.py --batch <id> --workdir <campaña> --uri "$URI" \
      --apply --expect-destination <servidor>/<base>
(ver scripts/production_publish/cli.py). Cada corrida, por plan o por tanda, deja
su renglón en <repo>/.content-audit/production/registry.jsonl.
"""

import argparse
import glob
import json
import os
import sys
from collections import Counter, namedtuple
from datetime import datetime, timezone

# pymongo y bson se importan adentro de las funciones que los usan: así el script
# se puede importar (y probar) sin conectarse ni tener el driver.

SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, SCRIPTS_DIR)

from production_publish import registry as production_registry  # noqa: E402

REPO_DEFAULT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOOL = "publish_revisions"

QUIZ_SET_FIELDS = ("title", "translation")  # top-level directos
FORM_SET_FIELDS = ("sentences", "sentenceParts")  # bajo form.*
FORM_PROTECTED = ("kind", "incidence", "label", "name")


def parse_args(argv=None):
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
    return p.parse_args(argv)


def json_escape(s):
    return json.dumps(s, ensure_ascii=False)[1:-1]


def plan_quiz_title_sync(repo, db, quizzes_collection, pending):
    """Arma, sin escribir nada, la sincronización quiz.title := knowledge.label.

    quizTemplates.title es una copia desnormalizada del label del knowledge
    (verificado 2026-07-15: 10165/11487 eran copias literales del label 2025).
    Esta fase re-deriva el título de CADA quiz desde el label actual de su
    knowledge — en los archivos db/ y en Mongo — y sincroniza las copias
    embebidas en las instancias de la colección `quizzes` (template.title /
    template.instructions).

    `pending` es {_id de quizTemplates: {campo: valor}}: lo que la corrida escribe
    antes. La sincronización se arma sobre cómo va a quedar la base después de eso,
    para que el respaldo (F-PUBPROD-R008) tenga todo antes de la primera escritura.
    Devuelve {"stats", "files": {ruta: texto nuevo}, "titles": [...], "instances": [...]};
    cada entrada es {"collection", "_id", "old", "new"} y «old» es lo que hay hoy en Mongo.
    """
    from bson import ObjectId, json_util

    course_dir = os.path.join(repo, "db", "english-course")
    stats = Counter()
    files, titles, new_titles = {}, [], {}
    masters = {d["_id"]: d for d in db[quizzes_collection].find({}, {"title": 1, "instructions": 1})}

    def after_run(tid, field):
        """El campo del original después de las escrituras de la corrida."""
        if field == "title" and tid in new_titles:
            return new_titles[tid]
        return pending.get(tid, {}).get(field, masters[tid].get(field))

    for kf in glob.glob(os.path.join(course_dir, "**", "_knowledge.json"), recursive=True):
        with open(kf, encoding="utf-8") as fh:
            label = (json_util.loads(fh.read())).get("label")
        qf = os.path.join(os.path.dirname(kf), "quizzes.json")
        if not label or not os.path.exists(qf):
            continue
        with open(qf, encoding="utf-8") as fh:
            raw = fh.read()
        text = raw
        replaced_in_file = set()  # un replace-all cubre los títulos repetidos del archivo
        for q in json_util.loads(raw):
            old = q.get("title")
            if old == label:
                stats["title_already_synced"] += 1
            else:
                stats["title_file_updates"] += 1
                if old not in replaced_in_file:
                    pat = '"title" : "%s"' % json_escape(old)
                    if pat not in text:
                        stats["title_file_pattern_miss"] += 1
                    else:
                        text = text.replace(pat, '"title" : "%s"' % json_escape(label))
                        replaced_in_file.add(old)
            # El estado de Mongo se evalúa SIEMPRE, con independencia de si el
            # archivo ya estaba sincronizado: son dos destinos distintos y un
            # archivo en sync no implica un Mongo en sync (es justo lo que pasa
            # al publicar en un entorno nuevo desde archivos ya sincronizados).
            qid = q["_id"]
            if qid in masters and qid not in new_titles and after_run(qid, "title") != label:
                new_titles[qid] = label
                titles.append({"collection": quizzes_collection, "_id": qid,
                               "old": {"title": masters[qid].get("title")}, "new": {"title": label}})
        if text != raw:
            files[qf] = text
    stats["title_mongo_updates"] = len(titles)

    # Copias embebidas en instancias (colección `quizzes`): title + instructions
    instances = []
    for qz in db["quizzes"].find({}, {"template": 1}):
        t = qz.get("template") or {}
        tid = t.get("_id")
        if tid is None:
            continue
        if not isinstance(tid, ObjectId):
            try:
                tid = ObjectId(str(tid))
            except Exception:
                continue
        if tid not in masters:
            continue
        old, new = {}, {}
        for field in ("title", "instructions"):
            value = after_run(tid, field)
            if value != t.get(field):
                old["template.%s" % field], new["template.%s" % field] = t.get(field), value
        if new:
            stats["embedded_instances_synced"] += 1
            instances.append({"collection": "quizzes", "_id": qz["_id"], "old": old, "new": new})

    return {"stats": stats, "files": files, "titles": titles, "instances": instances}


def title_sync_entries(sync):
    """Las escrituras de la sincronización, con el _id como texto (para el respaldo y el informe)."""
    return [dict(entry, _id=str(entry["_id"])) for entry in sync["titles"] + sync["instances"]]


def apply_quiz_title_sync(db, sync):
    """Escribe exactamente lo que se armó (y quedó en el respaldo) y devuelve lo que no coincide al releer."""
    from bson import json_util
    from pymongo import UpdateOne

    for path, text in sync["files"].items():
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
        with open(path, encoding="utf-8") as fh:
            json_util.loads(fh.read())  # sanity parse
    if sync["titles"]:
        col = db[sync["titles"][0]["collection"]]
        result = col.bulk_write([UpdateOne({"_id": e["_id"]}, {"$set": e["new"]}) for e in sync["titles"]],
                                ordered=True)
        sync["stats"]["title_mongo_modified"] = result.modified_count
    for e in sync["instances"]:
        db[e["collection"]].update_one({"_id": e["_id"]}, {"$set": e["new"]})

    failures = []
    for e in sync["titles"] + sync["instances"]:
        doc = db[e["collection"]].find_one({"_id": e["_id"]})
        for path, expected in e["new"].items():
            if get_path(doc, path) != expected:
                failures.append(f"{e['collection']} {e['_id']} {path}")
    return failures


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
    from bson import json_util

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
    if "MULTIPLE_CHOICE" in (file_form.get("kind"), mongo_form.get("kind")):
        # content-audit no corrige la opción múltiple: su form (hueco sin options, items,
        # selection) nunca se escribe desde acá, aunque una revisión vieja lo haya tocado.
        stats["form de opción múltiple no publicado"] += 1
        return sets
    for field in FORM_SET_FIELDS:
        value = file_form.get(field)
        if field == "sentenceParts":
            value = repair_sentence_parts(value, stats)
        if value != mongo_form.get(field):
            sets[f"form.{field}"] = value
    return sets


def check_course_doc_labels(db, changed_labels):
    """Alerta si el doc de courses embebe algún label viejo de knowledge."""
    from bson import json_util

    course = db["courses"].find_one()
    if course is None:
        return ["WARN: colección courses vacía — no se pudo chequear embedding de labels"]
    blob = json_util.dumps(course)
    hits = [old for old in changed_labels if old and json.dumps(old, ensure_ascii=False)[1:-1] in blob]
    if hits:
        return [f"WARN: el doc de courses contiene {len(hits)} labels viejos de knowledges "
                f"(muestra: {hits[:3]}) — puede desnormalizar labels y requerir update aparte"]
    return []


# Lo que el camino por plan escribe en un documento: su _id, los campos con su valor nuevo,
# las propuestas que lo piden y lo que había en Mongo en esos campos (el respaldo).
Update = namedtuple("Update", "id sets proposals old")


def get_path(doc, path):
    value = doc
    for key in path.split("."):
        value = (value or {}).get(key)
    return value


def batches_of_plan(roots, plan):
    """Tandas cuyo planId es este plan, en cada raíz y en sus campañas.

    Devuelve (coincidencias, ilegibles): listas de (ruta, id de tanda) y de rutas. Un
    archivo que se ve desde dos raíces (o por un enlace) cuenta una sola vez.
    """
    matches, unreadable, seen = [], [], set()
    for root in roots:
        for pattern in (os.path.join(root, ".content-audit", "batches", "*.json"),
                        os.path.join(root, ".content-audit", "campaigns", "*", ".content-audit", "batches",
                                     "*.json")):
            for path in sorted(glob.glob(pattern)):
                real = os.path.realpath(path)
                if real in seen:
                    continue
                seen.add(real)
                try:
                    with open(path, encoding="utf-8") as fh:
                        record = json.load(fh)
                except (OSError, ValueError):
                    unreadable.append(path)
                    continue
                if isinstance(record, dict) and record.get("planId") == plan:
                    matches.append((path, record.get("id")))
    return matches, unreadable


def main(argv=None):
    argv = sys.argv[1:] if argv is None else list(argv)
    if any(a == "--batch" or a.startswith("--batch=") for a in argv):
        # Una tanda va a producción solo con su lista de publicación (F-PUBPROD-R006).
        from production_publish import cli
        return cli.main(argv)

    args = parse_args(argv)
    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"== publish_revisions [{mode}] plan={args.plan} db={args.db} ==\n")

    recorder = production_registry.LegacyRecorder(args.repo, TOOL, args.uri, args.db, args.apply)
    if recorder.destination is None:
        return recorder.refuse_bad_destination()

    batches, unreadable = batches_of_plan((args.repo, REPO_DEFAULT), args.plan)
    batch_ids = sorted({str(batch_id) for _, batch_id in batches})
    refused = _refuse_if_plan_of_batch(args, recorder, batch_ids, unreadable)
    if refused is not None:
        return refused

    # Una excepción de pymongo se muestra solo por su clase: su mensaje puede traer el destino.
    return recorder.run(lambda: publish_plan(args, mode, recorder, batch_ids), plan=args.plan)


def _refuse_if_plan_of_batch(args, recorder, batch_ids, unreadable):
    """F-PUBPROD-R006 inv. 5: el camino por plan no escribe el plan de una tanda.

    Con --apply se niega si el plan es de una tanda o si no se pudieron leer las tandas (no
    se sabe); sin --apply solo avisa. Devuelve la salida si se negó, None si se puede seguir.
    """
    if args.apply and (batch_ids or unreadable):
        if batch_ids:
            message = (f"el plan {args.plan} es de la tanda {', '.join(batch_ids)}: una tanda va a "
                       f"producción solo con su lista (--batch), nunca por plan")
        else:
            message = (f"no se pudo leer {len(unreadable)} archivo(s) de tandas ({unreadable[0]}), así que no "
                       f"se sabe si el plan {args.plan} es de una tanda")
        print(f"NEGADO: {message}. No se conectó a ninguna base y no se escribió nada.")
        recorder.record(production_registry.REFUSED, plan=args.plan, batches=batch_ids,
                        reason={"code": "PLAN_OF_BATCH" if batch_ids else "BATCHES_UNREADABLE",
                                "message": message})
        return production_registry.EXIT_REFUSED
    if batch_ids:
        print(f"AVISO: el plan {args.plan} es de la tanda {', '.join(batch_ids)}: este ensayo se puede mirar, "
              f"pero --apply se va a negar. Una tanda va a producción con --batch.\n")
    elif unreadable:
        print(f"AVISO: no se pudo leer {len(unreadable)} archivo(s) de tandas ({unreadable[0]}); "
              f"--apply se va a negar hasta que se puedan leer.\n")
    return None


def publish_plan(args, mode, recorder, batch_ids):
    from pymongo import MongoClient, UpdateOne

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
    updates = {"knowledges": [], "quizTemplates": []}  # colección -> [Update]
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
        updates["knowledges"].append(Update(fdoc["_id"], sets, proposals, {f: get_path(mdoc, f) for f in sets}))
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
        updates["quizTemplates"].append(Update(fdoc["_id"], sets, proposals, {f: get_path(mdoc, f) for f in sets}))
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

    # La sincronización de títulos se arma ahora, antes del respaldo: lo que escribe
    # también tiene su «old» y su «new» antes de la primera escritura (F-PUBPROD-R008).
    sync = None
    if not args.no_sync_quiz_titles:
        sync = plan_quiz_title_sync(args.repo, db, args.quizzes_collection,
                                    {u.id: u.sets for u in updates["quizTemplates"]})
        sync_stats = sync["stats"]
        print(f"\nsync quiz.title := knowledge.label: {sync_stats['title_file_updates']} en archivos "
              f"(ya en sync: {sync_stats['title_already_synced']}), {sync_stats['title_mongo_updates']} en Mongo, "
              f"{sync_stats['embedded_instances_synced']} instancias embebidas")
        if sync_stats.get("title_file_pattern_miss"):
            print(f"!! patrones de título no hallados en archivo: {sync_stats['title_file_pattern_miss']}")

    report = {
        "mode": mode, "plan": args.plan, "db": args.db,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "stats": dict(stats), "problems": problems,
        "updates": {col: [{"_id": str(u.id), "fields": sorted(u.sets), "proposals": u.proposals,
                           "old": u.old, "new": u.sets}
                          for u in ups] for col, ups in updates.items()},
    }
    if sync is not None:
        report["quizTitleSync"] = {"stats": dict(sync["stats"]), "writes": title_sync_entries(sync)}
    report_dir = os.path.join(args.repo, ".content-audit", "publish-reports")
    os.makedirs(report_dir, exist_ok=True)
    report_path = os.path.join(report_dir, f"{args.plan}_{mode}_{report['timestamp'].replace(':', '-')}.json")
    with open(report_path, "w") as fh:
        json.dump(report, fh, indent=2, ensure_ascii=False, default=str)
    print(f"\nReporte guardado: {report_path}")

    hard_problems = [p for p in problems if not p.startswith("WARN")]
    counts = {"knowledgeUpdates": len(updates["knowledges"]), "quizTemplateUpdates": len(updates["quizTemplates"]),
              "problems": len(problems), "hardProblems": len(hard_problems)}
    row_common = {"plan": args.plan, "batches": batch_ids, "report": os.path.relpath(report_path, args.repo),
                  "counts": counts}
    if sync is not None:
        row_common["quizTitleSync"] = dict(sync["stats"])

    if not args.apply:
        print("\nDRY-RUN: no se modificó nada. Ejecutar con --apply para aplicar.")
        # F-PUBPROD-R008: un ensayo con problemas termina con error, como el paso por tanda.
        recorder.record(production_registry.DRY_RUN_WITH_PROBLEMS if hard_problems
                        else production_registry.DRY_RUN_CLEAN, **row_common)
        return production_registry.EXIT_PROBLEMS if hard_problems else production_registry.EXIT_OK

    if hard_problems:
        recorder.record(production_registry.ABORTED, reason={"code": "HARD_PROBLEMS", "message":
                        f"{len(hard_problems)} problemas duros; no se escribió nada"}, **row_common)
        sys.exit(f"\nABORTADO: {len(hard_problems)} problemas duros; resolver antes de --apply.")

    # F-PUBPROD-R008: el respaldo y el renglón de inicio, antes de la primera escritura.
    backup = {"plan": args.plan, "report": row_common["report"],
              "note": "Respaldo: por cada campo, «old» es lo que había en Mongo y «new» lo que se escribe.",
              "documents": [{"collection": col, "_id": str(u.id), "old": u.old, "new": u.sets,
                             "proposals": u.proposals} for col, ups in updates.items() for u in ups]}
    if sync is not None:
        backup["titleSync"] = title_sync_entries(sync)
    problem = recorder.begin_writing(backup, **row_common)
    if problem:
        sys.exit(f"\nABORTADO: {problem}.")

    print("\n== Aplicando ==")
    for col_name, col in (("knowledges", col_k), ("quizTemplates", col_q)):
        ops = [UpdateOne({"_id": u.id}, {"$set": u.sets}) for u in updates[col_name]]
        if not ops:
            continue
        result = col.bulk_write(ops, ordered=True)
        print(f"{col_name}: matched={result.matched_count} modified={result.modified_count}")

    print("\n== Verificación post-apply ==")
    failures = 0
    for col_name, col in (("knowledges", col_k), ("quizTemplates", col_q)):
        for u in updates[col_name]:
            mdoc = col.find_one({"_id": u.id})
            for path, expected in u.sets.items():
                if get_path(mdoc, path) != expected:
                    failures += 1
                    print(f"  FAIL {col_name} {u.id} {path} ({u.proposals[0]})")
    total = len(updates["knowledges"]) + len(updates["quizTemplates"])
    written_ids = [f"{col}/{u.id}" for col, ups in updates.items() for u in ups]
    if failures:
        recorder.record(production_registry.VERIFY_FAILED, writtenIds=written_ids, verified=False,
                        reason={"code": "VERIFY_FAILED", "message": f"{failures} campos no coinciden"},
                        **row_common)
        sys.exit(f"VERIFICACIÓN FALLÓ: {failures} campos no coinciden.")
    print(f"OK: {total} documentos verificados campo por campo contra el estado esperado.")

    if sync is not None:
        print("\n== Sync quiz.title := knowledge.label ==")
        sync_failures = apply_quiz_title_sync(db, sync)
        for key in (f"{e['collection']}/{e['_id']}" for e in sync["titles"] + sync["instances"]):
            if key not in written_ids:
                written_ids.append(key)
        print(f"archivos reescritos: {len(sync['files'])}; títulos en Mongo: "
              f"{sync['stats'].get('title_mongo_modified', 0)} de {sync['stats']['title_mongo_updates']}; "
              f"instancias embebidas: {len(sync['instances'])}")
        if sync_failures:
            for failure in sync_failures:
                print(f"  FAIL {failure}")
            recorder.record(production_registry.VERIFY_FAILED, writtenIds=written_ids, verified=False,
                            reason={"code": "VERIFY_FAILED", "message":
                                    f"{len(sync_failures)} campos de la sincronización de títulos no coinciden"},
                            **row_common)
            sys.exit(f"VERIFICACIÓN FALLÓ: {len(sync_failures)} campos de la sincronización de títulos no coinciden.")
        print("OK: la sincronización de títulos quedó como dice el respaldo.")

    recorder.record(production_registry.APPLIED_VERIFIED, writtenIds=written_ids, verified=True, **row_common)


if __name__ == "__main__":
    raise SystemExit(main())
