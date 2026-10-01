# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — PENDIENTE PARA DESPUÉS DEL APPLY de los cuatro de B (no antes): segundo parche de pruebas de
  MLOCAL en esta carpeta, sólo handwrittenTests trazadas a FEAT-MLOCAL sobre GrammarAnalyzer, NaturalnessAnalyzer y
  NearDuplicatesAnalyzer (la fila de la corrida en el curso con herramienta, versión examinada, si publicó, si usó la
  alternativa y el motivo: sin herramienta, sin examen o «falta el tope»), backup antes del propose (bug 08) y `patch
  apply --as=qa-tester`. Lo decidió el coordinador (OG1).

2026-10-01 — qa-tester — Cadena B con pruebas simulada en memoria (`qa-B2/sim/B01..B14`, librería del jar): FEAT-RONDA2
  + los cuatro de B, y encima F y E tal como están; ninguna prueba de B se pierde. `generate` simulado (DryRunGenerate)
  con FEAT-RONDA2 + B: 0 errores de definición, de traza y de ubicación, crearía los 4 journeys (5 + 3 + 5 + 5 caminos)
  y los 152 stubs. Con la cadena entera, 9 journeys sin ubicar, todos de E y F: B ya no lo hace abortar.

2026-10-01 — qa-tester — 32 handwrittenTests nuevas, las 8 reglas [covered]; F-MLOCAL-J001 en audit-cli,
  `com.learney.contentaudit.auditcli.commands` (5 caminos). `patch propose` con fusión (sin pérdidas), `tech-spec write`
  (+1 fragmento) y `patch validate -i` contra el modelo con FEAT-RONDA2 (`integrador-2/sim/real/M01`): exit 0. Simulado
  en memoria en `qa-B2/sim`. Sin aplicar.
  Next: el integrador toma B desde el commit del qa-tester (`rebase.py`, ORIGEN["B"]) y rehace la cadena.

2026-09-30 — architect — Parche y TECH_SPEC rebasados sobre `ronda1/integracion` con la alternativa: `patch propose
  --replace`, `tech-spec write --replace` (10 fragmentos) y `patch validate -i` contra su sentinel.yaml en exit 0; la cadena
  MLOCAL → GRAMA → NATUR → DUPAR simulada sobre ese modelo, válida. Ganchos nuevos B-21 a B-24 en ittm `…/ronda2/ganchos-B.md`.
  Next: el integrador de la ronda 2; José contesta DOUBT-MLOCAL-TOPE-ALTERNATIVA y DOUBT-MLOCAL-MESA.

2026-09-30 — analyst — REQUIREMENT v2 con la alternativa de claude -p: R001, R003, R004 y R006 ajustadas, R007 y R008
  nuevas, J001 con cinco salidas, dos dudas nuevas (SIN-OLLAMA resuelta, TOPE-ALTERNATIVA abierta). `requirement validate` [OK].
  Next: @architect ajusta parche y TECH_SPEC para usar el runtime de los jueces de C.

2026-09-30 — architect — Diseño propuesto, sin aplicar: `architectural_patch.yaml` (3 altas, 2 cambios, 0 conflictos; `patch
  validate` exit 0) y `TECH_SPEC.md` (10 fragmentos). Simulados en memoria MLOCAL → GRAMA → NATUR → DUPAR: válidos. Lo
  compartido, 20 ganchos, en ittm `…/ronda2/ganchos-B.md`. Dudas de José resueltas; queda abierta DOUBT-MLOCAL-MESA.
  Next: José contesta DOUBT-MLOCAL-MESA; el orquestador aplica los cuatro en ese orden, después de la ronda 1 integrada
  (usan `JudgeExamRegistry`, `CorrectorMatcher` y `QuizStudentViewRenderer`); `feature sync` de las cuatro; @qa-tester.

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-MLOCAL (paquete B, ronda 2): 6 reglas, 1 journey, 3 dudas (dependencias, dónde
  corre cada modelo, si corren sin pedirlos).
  Next: José contesta las dudas; el integrador suma la carpeta y corre `feature sync --feature FEAT-MLOCAL` (J001); después
  @architect, que decide cómo se instala cada herramienta.
