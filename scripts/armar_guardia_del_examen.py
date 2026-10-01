#!/usr/bin/env python3
"""Arma la lista que usa el guardián del examen: qué ejercicios y qué temas no pueden aparecer en ninguna definición de
agente de `.sentinel/agents/`.

    python3 scripts/armar_guardia_del_examen.py <carpeta-del-examen> [--partes escribir,jueces[,examinar]]

<carpeta-del-examen> es la del 077 (`docs/briefs/077-revision-de-ejercicios/examen/` de ittm). Por cada parte que se
pide, toma:
- de cada archivo de categoría, el `quizId` de cada caso y el de su gemelo, si lo tiene; de la categoría `miniteoria`,
  además, su tema (`knowledgeId`);
- de `jueces/examen-fijo.json`, el `quizId` de cada caso de cada grupo, y el tema de los grupos que rinden todos los
  jueces (todo grupo salvo `b_imposibles`, que es sólo del juez de consigna): ésos son los temas del examen de
  miniteoría.

`universo.json` no es un caso: no se lee.

Escribe `quiz-instruction-infrastructure/src/test/resources/agentguard/examen.json`, que lee
`AgentDefinitionsExamGuardIntegrationTest`. Guarda sólo ids, nunca lo esperado: los textos los saca la prueba del curso
(`db/english-course`). Sólo biblioteca estándar.
"""
import argparse
import datetime
import json
import os
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SALIDA = os.path.join(RAIZ, "quiz-instruction-infrastructure", "src", "test", "resources", "agentguard", "examen.json")
SOLO_CONSIGNA = {"b_imposibles"}


def gemelos(caso):
    g = caso.get("gemelo")
    if isinstance(g, str):
        return [g]
    if isinstance(g, dict) and g.get("quizId"):
        return [g["quizId"]]
    if isinstance(g, list):
        return [x if isinstance(x, str) else x.get("quizId") for x in g if x]
    return []


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("examen")
    ap.add_argument("--partes", default="escribir,jueces")
    a = ap.parse_args(argv)
    partes = [p.strip() for p in a.partes.split(",") if p.strip()]
    casos, temas = set(), set()
    for parte in partes:
        carpeta = os.path.join(a.examen, parte)
        if not os.path.isdir(carpeta):
            sys.exit("no existe la parte %s en %s" % (parte, a.examen))
        for nombre in sorted(os.listdir(carpeta)):
            if not nombre.endswith(".json") or nombre == "universo.json":
                continue
            with open(os.path.join(carpeta, nombre), encoding="utf-8") as f:
                datos = json.load(f)
            if "grupos" in datos:
                for grupo, contenido in datos["grupos"].items():
                    for caso in contenido.get("casos", []):
                        casos.add(caso["quizId"])
                        if grupo not in SOLO_CONSIGNA and caso.get("knowledgeId"):
                            temas.add(caso["knowledgeId"])
            else:
                for caso in datos.get("casos", []):
                    casos.add(caso["quizId"])
                    casos.update(x for x in gemelos(caso) if x)
                    if datos.get("categoria") == "miniteoria" and caso.get("knowledgeId"):
                        temas.add(caso["knowledgeId"])
    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    with open(SALIDA, "w", encoding="utf-8") as f:
        json.dump({"generado": datetime.date.today().isoformat(), "partes": partes,
                   "casos": sorted(casos), "temasDeMiniteoria": sorted(temas)}, f, ensure_ascii=False, indent=0)
        f.write("\n")
    print("%s: %d casos y %d temas de miniteoría, de %s" % (os.path.relpath(SALIDA, RAIZ), len(casos), len(temas),
                                                            ", ".join(partes)))


if __name__ == "__main__":
    main()
