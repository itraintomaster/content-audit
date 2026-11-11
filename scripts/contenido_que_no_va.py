#!/usr/bin/env python3
"""Reemplaza en producción el contenido que no va en el curso (brief 284, 11/11/2026).

José, 11/11: aplicar los reemplazos del brief 284 y regenerar sus audios. La lista, ejercicio
por ejercicio y campo por campo, está en `contenido-que-no-va.json`, al lado: cada cambio
lleva el valor de antes (el del respaldo de producción del 29/9) y el de después.

Cada ejercicio se escribe entero o no se escribe: sólo si TODOS sus campos todavía tienen el
valor de antes. Si alguien tocó uno después, el ejercicio se saltea y se cuenta aparte (un
ejercicio a medio cambiar es peor que uno sin cambiar). Lo anterior queda guardado en el mismo
documento, en `contenidoQueNoVaAntes`, para poder volver atrás.

Además de quizTemplates, pone el mismo valor en las copias que todavía pueden aparecer en
pantalla, como be/scripts/enunciados-de-preguntas.py de ittm: los `quizzes` que no están
SOLVED (el evaluador usa el form de esa copia) y los exposures de `studySessions` que no están
SOLVED. En las copias también se cambia sólo si tienen el valor de antes.

Grupos (--grupo): `ahora` (los 14 de A2, B1 y B2; es el que va por defecto) y
`despues-de-la-c2` (1e82, de A1: la C2 le cambia la miniteoría, así que va recién después de
publicarla; su «antes» es la miniteoría que deja la C2).

    # ver qué haría, sobre un respaldo (mongodump), sin conectarse a ninguna base
    python3 scripts/contenido_que_no_va.py --desde ~/projects/learney/mongo-buckups/<fecha>/learney

    # ver qué haría contra producción (lee el URI del config de ittm; nunca lo imprime)
    python3 scripts/contenido_que_no_va.py

    # aplicarlo
    python3 scripts/contenido_que_no_va.py --aplicar [--grupo ahora]

    # volver atrás (con el mismo grupo)
    python3 scripts/contenido_que_no_va.py --revertir [--grupo ahora]

    # la lista de audios para scripts/regenerate_quiz_audios.py (regen_manifest.json)
    python3 scripts/contenido_que_no_va.py --manifiesto-audio <carpeta> [--grupo ahora]

Revertir deja cada ejercicio con sus valores de antes sólo si todavía tiene todos los que dejó
este script. Nunca restaurar documentos enteros de quizTemplates desde un respaldo: otras
tareas cambian otros campos en la misma colección. El audio no se toca acá: la clave
(answerAudioUrl) queda igual y el mp3 se pisa en S3 con regenerate_quiz_audios.py.
"""

import argparse
import collections
import json
import os
import re
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
DATOS = os.path.join(AQUI, "contenido-que-no-va.json")
CONFIG_ITTM = os.path.expanduser(
    "~/projects/learney/ittm/be/application/serverless/aws-lambda-handler/cmd/config.yaml")
GUARDADO = "contenidoQueNoVaAntes"
SOLVED = "SOLVED"
GRUPOS = ("ahora", "despues-de-la-c2")


def leer_ejercicios(grupo):
    datos = json.load(open(DATOS, encoding="utf-8"))
    return [e for e in datos["ejercicios"] if e["grupo"] == grupo]


def obtener(doc, camino):
    for parte in camino.split("."):
        if isinstance(doc, list):
            i = int(parte)
            if i >= len(doc):
                return None
            doc = doc[i]
        elif isinstance(doc, dict):
            doc = doc.get(parte)
        else:
            return None
    return doc


def copias(campo):
    """Dónde está cada campo en la copia del ejercicio que guarda una instancia."""
    caminos = ["template." + campo]
    if campo == "miniTheory":
        caminos.append("feedback.miniTheory")
    return caminos


def estado(ejercicio, doc):
    """'antes', 'despues', 'no existe' o 'cambio por otro lado' (con el detalle)."""
    if doc is None:
        return "no existe", []
    valores = [(c, obtener(doc, c["campo"])) for c in ejercicio["cambios"]]
    if all(v == c["antes"] for c, v in valores):
        return "antes", []
    if all(v == c["despues"] for c, v in valores):
        return "despues", []
    distintos = [(c["campo"], v) for c, v in valores if v not in (c["antes"], c["despues"])]
    mezcla = [(c["campo"], v) for c, v in valores if v == c["despues"]]
    return "cambio por otro lado", distintos or mezcla


def mirar(ejercicios, docs):
    cuenta = collections.Counter()
    for e in ejercicios:
        st, detalle = estado(e, docs.get(e["_id"]))
        cuenta[st] += 1
        marca = {"antes": "  sin aplicar", "despues": "  ya aplicado", "no existe": "  NO EXISTE"}.get(
            st, "  CAMBIÓ POR OTRO LADO, se saltea")
        print(f"{marca}  {e['corto']} ({e['kind']}, audio {e['audio']}): «{e['dice']}»")
        for campo, valor in detalle:
            print(f"      {campo}: hay {valor!r}")
    return cuenta


def leer_respaldo(ruta):
    import bson
    ruta = os.path.expanduser(ruta)
    if os.path.isdir(ruta):
        ruta = os.path.join(ruta, "quizTemplates.bson")
    with open(ruta, "rb") as fh:
        return {str(d["_id"]): d for d in bson.decode_file_iter(fh)}


def uri_de_produccion():
    for linea in open(CONFIG_ITTM):
        m = re.search(r"(mongodb\+srv://\S+)", linea)
        if m:
            return m.group(1)
    raise SystemExit("no encontré el URI en el config de ittm")


def base(uri, nombre):
    from pymongo import MongoClient
    print(f"destino: {uri.split('@')[-1].split('/')[0].split('?')[0]} / {nombre}")
    return MongoClient(uri, serverSelectionTimeoutMS=20000)[nombre]


def escribir(db, ejercicios, revertir=False):
    from bson import ObjectId
    col = db["quizTemplates"]
    hechos, ya, saltados = [], [], []
    copias_hechas = collections.Counter()
    for e in ejercicios:
        de = "despues" if revertir else "antes"
        a = "antes" if revertir else "despues"
        filtro = {"_id": ObjectId(e["_id"])}
        for c in e["cambios"]:
            filtro[c["campo"]] = c[de]
        sets = {c["campo"]: c[a] for c in e["cambios"]}
        if revertir:
            r = col.update_one(filtro, {"$set": sets, "$unset": {GUARDADO: ""}})
        else:
            sets[GUARDADO] = [{"campo": c["campo"], "valor": c["antes"]} for c in e["cambios"]]
            r = col.update_one(filtro, {"$set": sets})
        if r.modified_count:
            hechos.append(e["corto"])
        else:
            doc = col.find_one({"_id": ObjectId(e["_id"])})
            (ya if estado(e, doc)[0] == a else saltados).append(e["corto"])
            if estado(e, doc)[0] != a:
                continue  # el ejercicio no se tocó: sus copias tampoco

        # Las copias: quizzes sin resolver y exposures sin resolver, siempre de este ejercicio.
        for c in e["cambios"]:
            for camino in copias(c["campo"]):
                r = db["quizzes"].update_many(
                    {"template._id": e["_id"], "status": {"$ne": SOLVED}, camino: c[de]},
                    {"$set": {camino: c[a]}})
                copias_hechas["campos en quizzes sin resolver"] += r.modified_count
                r = db["studySessions"].update_many(
                    {"exposures": {"$elemMatch": {"content.template.id": e["_id"],
                                                  "content.status": {"$ne": SOLVED},
                                                  "content." + camino: c[de]}}},
                    {"$set": {"exposures.$[e].content." + camino: c[a]}},
                    array_filters=[{"e.content.template.id": e["_id"], "e.content.status": {"$ne": SOLVED},
                                    "e.content." + camino: c[de]}])
                copias_hechas["sesiones con una copia sin resolver"] += r.modified_count
    verbo = "vueltos atrás" if revertir else "escritos"
    print(f"\n  ejercicios {verbo}: {len(hechos)} {' '.join(hechos)}")
    print(f"  ya estaban así: {len(ya)} {' '.join(ya)}")
    print(f"  cambiaron por otro lado y quedaron como están: {len(saltados)} {' '.join(saltados)}")
    for k, n in copias_hechas.items():
        print(f"  {k}: {n} {verbo}")

    # Verificación: se vuelve a leer cada ejercicio de la base.
    docs = {str(d["_id"]): d for d in col.find({"_id": {"$in": [ObjectId(e["_id"]) for e in ejercicios]}})}
    esperado = "antes" if revertir else "despues"
    mal = [e["corto"] for e in ejercicios if estado(e, docs.get(e["_id"]))[0] != esperado]
    print(f"\n  verificación: {len(ejercicios) - len(mal)} de {len(ejercicios)} con los valores {esperado}"
          + (f"; NO: {' '.join(mal)}" if mal else ""))
    return 1 if mal or saltados else 0


def manifiesto_audio(ejercicios, carpeta):
    os.makedirs(carpeta, exist_ok=True)
    filas = [{"key": e["audio"], "mode": "FILL", "text": e["dice"], "source": "contenido-que-no-va"}
             for e in ejercicios]
    json.dump(filas, open(os.path.join(carpeta, "regen_manifest.json"), "w"), indent=1, ensure_ascii=False)
    print(f"{len(filas)} audios en {os.path.join(carpeta, 'regen_manifest.json')}")
    for f in filas:
        print(f"  {f['key']}: «{f['text']}»")


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--db", default="learney")
    ap.add_argument("--uri", help="conexión a Mongo (sin esto, el de producción del config de ittm)")
    ap.add_argument("--desde", help="respaldo de mongodump (carpeta o quizTemplates.bson), en vez de la base")
    ap.add_argument("--grupo", default="ahora", choices=GRUPOS)
    ap.add_argument("--aplicar", action="store_true")
    ap.add_argument("--revertir", action="store_true")
    ap.add_argument("--manifiesto-audio", metavar="CARPETA",
                    help="sólo escribe regen_manifest.json para regenerate_quiz_audios.py")
    args = ap.parse_args()
    if args.desde and (args.aplicar or args.revertir):
        ap.error("--desde es sólo para mirar: para escribir, la base")
    if args.aplicar and args.revertir:
        ap.error("--aplicar o --revertir, no los dos")

    ejercicios = leer_ejercicios(args.grupo)
    print(f"grupo {args.grupo}: {len(ejercicios)} ejercicios, "
          f"{sum(len(e['cambios']) for e in ejercicios)} campos")
    if args.manifiesto_audio:
        return manifiesto_audio(ejercicios, args.manifiesto_audio)

    if args.desde:
        docs = leer_respaldo(args.desde)
    else:
        db = base(args.uri or uri_de_produccion(), args.db)
        from bson import ObjectId
        docs = {str(d["_id"]): d for d in db["quizTemplates"].find(
            {"_id": {"$in": [ObjectId(e["_id"]) for e in ejercicios]}})}
    cuenta = mirar(ejercicios, docs)
    print("  " + " · ".join(f"{k}: {n}" for k, n in cuenta.items()))

    if args.revertir:
        return escribir(db, ejercicios, revertir=True)
    if not args.aplicar:
        print("\nEn seco. Con --aplicar se escribe.")
        return 0
    return escribir(db, ejercicios)


if __name__ == "__main__":
    sys.exit(main())
