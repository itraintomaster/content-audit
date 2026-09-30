# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — Parche y TECH_SPEC de FEAT-JEXAM: `patch propose` 0, `patch validate` 0 (26 altas, 16
  cambios), `tech-spec write` 0 (14 fragmentos). Requirement corregido como analista con las decisiones de José (seis dudas
  RESOLVED, R010–R012 nuevas, DOUBT-EXAMEN-Y-CANDIDATOS nueva). Ganchos en docs/propuestas/tablero-corregir-sin-empeorar/
  ronda1/ganchos-C.md (ittm). No aplicado.
  Next: José aplica 0.2, 0.3 y el integrado; este parche antes que TRSEN, UNICA, MINIT y MCVALE; feature sync; @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-JEXAM (paquete C, ronda 1): 9 reglas, 1 journey, 6 dudas; `requirement validate` OK, 263 líneas
  (más de las 220 pedidas porque aloja lo común y las 4 dudas de proveedor, tope, muestra y paralelo que no se repiten). Aloja lo común
  a los cinco jueces (examen fijo, costo pendiente, conjunto, consultas a la vez, candidatos de la mesa) y lo que cambia
  del juez de consigna sin tocar lo que lee. Sin tocar sentinel.yaml ni requirements existentes.
  Next: José contesta las dudas; el integrador suma la carpeta a `definitions` y corre `feature sync --feature FEAT-JEXAM`
  (J001); después @architect. Promover al glosario «Juez», «Examen fijo» y «Versión del juez» cuando José apruebe.
