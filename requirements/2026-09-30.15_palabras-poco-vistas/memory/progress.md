# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer + test-writer (paquete D) — FEAT-POCOV construido: 38 pruebas sin @Disabled. Cargador con «4» y
  «8/6/4/3» (por omisión 8·6·4·3, getThreshold() queda en 4 de referencia), resolveCocaRank del catálogo, tabla
  LemmaExposure por palabra (nivel, banda, oraciones, puesto) y hallazgos F-POCOV-R003 por palabra y por nivel en el
  curso (C1 incluido), sugeridas e inventario con el N de la banda de cada palabra (D-07, D-08).
  Fixture: audit-domain/src/test/resources/pocov-2026-09-30/lemmas.tsv.gz (3.070 lemas, nivel y puesto EVP): da
  0,6698448 con 4 y 0,6652835 con 8·6·4·3 exactos; el curso real da 73,86 % (0,7385823).

2026-09-30 — qa-tester — Ronda 1: `patch propose` exit 0, fusionado con el parche del arquitecto: 36 handwrittenTests nuevos (0 portados) y F-POCOV-J001 en audit-cli, paquete commands; `tech-spec write` exit 0 (+1 sección). No aplicado.
  Simulado en scratch/qa-D (apply en memoria con la librería del jar, sin CLI apply ni generate), sobre el sentinel.yaml de hoy y sobre 0.2 + 0.3 aplicados antes: 36 pruebas aterrizan, 0 errores de traza, J001 ubicado, `feature status` OK, 7/7 reglas [covered]; generate agregaría 36 stubs y FPocovJ001JourneyTest con 2 caminos, sin choques de nombre.
  Next: el integrador aplica después de 0.2 y 0.3 y sin `--as` (el parche lleva la estructura del arquitecto); José corre generate; @test-writer implementa.

2026-09-30 — architect — Ronda 1: REQUIREMENT al día (bandas B y sin tareas, José 30/9; nueva R007; dudas nuevas PUESTO-DE-LA-BANDA y LEMATIZADOR). Parche y TECH_SPEC en exit 0: tabla de bandas en la configuración, `LemmaExposure` en `LemmaCountResult`, `resolveCocaRank`.
  Next: José confirma las bandas y el puesto; el integrador suma D-07 a D-09 y D-12 de ronda1/ganchos-D.md; `feature sync --feature FEAT-POCOV` y @qa-tester (prueba de los dos números en el TECH_SPEC).

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-POCOV (evolución de lemma-count, paquete D): 6 reglas, 1 journey, 2 dudas.
  El prototipo reproduce lemma-count exacto (A1 515/62, A2 810/418, B1 885/688, B2 384/366, C1 3/1, curso 0,6698, 473 sin nivel).
  Next: José decide DOUBT-UMBRALES-POR-BANDA (mueve el 73,9 %) y DOUBT-TAREAS; después @architect (hallazgos de nivel y curso, orden por banda, temas por palabra).
