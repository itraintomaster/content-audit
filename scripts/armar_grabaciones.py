#!/usr/bin/env python3
"""Arma db/grabaciones.json, el registro de grabaciones de FEAT-DATOS (gancho A-16), una sola vez.

Por cada clave de audio, el texto con que se grabó por última vez, de dónde salió y la fecha:

  1. la regrabación del 24/8/2026: el manifiesto de scripts/audio-regen-2026-08-24/regen_manifest.csv
     (clave, modo, texto), del worktree quiz-audio-regeneration de content-audit;
  2. para el resto de las claves, el generador de marzo de 2025 de ittm
     (tools/learney_course_generator/learney_simplified/*.json): el aiSentence de cada pregunta o, si
     falta, su sentence, por su localAudioPath.

Las dos fuentes viven en otros repositorios y una de ellas en un worktree que se puede borrar: por eso
el registro se copia acá, versionado con el curso, y lo actualiza cada regrabación (DOUBT-DATOS-REGISTRO,
opción A). content-audit lo lee por FileSystemAudioRecordingRegistry; no lo escribe al analizar.

Uso:
  python3 scripts/armar_grabaciones.py \\
      --manifiesto <worktree>/scripts/audio-regen-2026-08-24/regen_manifest.csv \\
      --generador  <ittm>/tools/learney_course_generator/learney_simplified \\
      [--salida db/grabaciones.json]
"""
import argparse
import csv
import glob
import json
import os
import sys
from datetime import date

FUENTE_REGRABACION = "regrabacion-2026-08-24"
FECHA_REGRABACION = "2026-08-24"
FUENTE_GENERADOR = "generador-2025-03"
FECHA_GENERADOR = "2025-03-15"  # commit f80274f99 de ittm, «generated audios, theory and translations»


def leer_generador(carpeta):
    textos = {}
    archivos = sorted(glob.glob(os.path.join(carpeta, "*.json")))
    if not archivos:
        sys.exit(f"no hay JSON del generador en {carpeta}")
    for archivo in archivos:
        with open(archivo, encoding="utf-8") as f:
            nivel = json.load(f)
        for seccion in nivel.get("sections") or []:
            for ejercicio in seccion.get("exercises") or []:
                for pregunta in ejercicio.get("questions") or []:
                    clave = (pregunta.get("localAudioPath") or "").strip()
                    texto = pregunta.get("aiSentence") or pregunta.get("sentence")
                    if not clave or not texto:
                        continue
                    if clave in textos and textos[clave] != texto:
                        sys.exit(f"la clave {clave} tiene dos textos en el generador")
                    textos[clave] = texto
    return textos


def leer_manifiesto(ruta):
    textos = {}
    with open(ruta, encoding="utf-8", newline="") as f:
        for fila in csv.DictReader(f):
            clave = (fila.get("key") or "").strip()
            texto = fila.get("text")
            if not clave or texto is None:
                continue
            if clave in textos and textos[clave] != texto:
                sys.exit(f"la clave {clave} tiene dos textos en el manifiesto")
            textos[clave] = texto
    return textos


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--manifiesto", required=True)
    parser.add_argument("--generador", required=True)
    parser.add_argument("--salida", default=os.path.join("db", "grabaciones.json"))
    args = parser.parse_args()

    generador = leer_generador(args.generador)
    manifiesto = leer_manifiesto(args.manifiesto)

    grabaciones = {}
    for clave, texto in generador.items():
        grabaciones[clave] = {"texto": texto, "fuente": FUENTE_GENERADOR, "fecha": FECHA_GENERADOR}
    for clave, texto in manifiesto.items():
        grabaciones[clave] = {"texto": texto, "fuente": FUENTE_REGRABACION, "fecha": FECHA_REGRABACION}

    registro = {
        "version": f"{date.today().isoformat()} · regrabación del 24/8 ({len(manifiesto)}) + generador de marzo"
                   f" de 2025 ({len(grabaciones) - len(manifiesto)})",
        "descripcion": "Por cada clave de audio, el texto con que se grabó por última vez, de dónde salió y la"
                       " fecha. Lo arma scripts/armar_grabaciones.py y lo actualiza cada regrabación.",
        "grabaciones": {clave: grabaciones[clave] for clave in sorted(grabaciones)},
    }
    with open(args.salida, "w", encoding="utf-8") as f:
        json.dump(registro, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"{args.salida}: {len(grabaciones)} claves ({len(manifiesto)} de la regrabación del 24/8,"
          f" {len(grabaciones) - len(manifiesto)} del generador de marzo de 2025)")


if __name__ == "__main__":
    main()
