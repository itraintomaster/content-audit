# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer + test-writer (paquete D) — FEAT-SEMAF construido: 32 pruebas sin @Disabled (28 de la consulta, 4
  de GetCmd) y F-SEMAF-J001 con sus 2 caminos. DefaultTopicWordLightsQuery lee del informe: rojo de lexis sobre los tokens
  de cada ejercicio al nivel del tema, salvo lo que el tema enseña; nivel, oraciones y N de la tabla de lemma-count; las
  sugeridas con una tarea LEMMA_ABSENCE transitoria. Control sobre el curso del 29/9: «Posición del adjetivo» 157
  palabras, 42 rojas y 27 verdes con 4 (29 con 8 · 6 · 4 · 3); el análisis y el plan quedan byte a byte (73,92 %, 4.036).
  Next: el orquestador revisa el commit.

2026-09-30 — qa-tester — Ronda 1: `patch propose` exit 0, fusionado con el parche del arquitecto: 32 handwrittenTests nuevos (0 portados) y F-SEMAF-J001 en audit-cli, paquete commands; `tech-spec write` exit 0 (+1 sección). No aplicado.
  Simulado en scratch/qa-D (apply en memoria con la librería del jar, sin CLI apply ni generate), sobre el sentinel.yaml de hoy y sobre 0.2 + 0.3 aplicados antes: 32 pruebas aterrizan, 0 errores de traza, J001 ubicado, `feature status` OK, 7/7 reglas [covered]; generate agregaría 32 stubs y FSemafJ001JourneyTest con 2 caminos, sin choques de nombre.
  Next: el integrador aplica después de 0.2 y 0.3 y sin `--as` (el parche lleva la estructura del arquitecto); José corre generate; @test-writer implementa.

2026-09-30 — architect — Ronda 1: REQUIREMENT al día (3 dudas resueltas; R002 con el nivel de lemma-count, ASSUMPTION). Parche y TECH_SPEC en exit 0: paquete `topicwords` en refiner-domain, recurso `word-lights`.
  Next: el integrador aplica NIVFR y POCOV antes (o junto) y suma D-02, D-05, D-06 y D-08 de ronda1/ganchos-D.md; después `feature sync --feature FEAT-SEMAF` y @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-SEMAF (paquete D, ronda 1): 7 reglas, 1 journey, 3 dudas.
  Medido sobre el análisis 2026-09-30T11-54-02 con el prototipo del paquete (reproduce lemma-absence y lemma-count exactos).
  Referencias hacia adelante a FEAT-POCOV (umbrales) y FEAT-RECUR (recurrencia por nivel), escritos en la misma ronda.
  Next: José contesta las 3 dudas; el integrador suma la carpeta, corre `feature sync --feature FEAT-SEMAF` (J001); @architect decide el recurso de `get`.
