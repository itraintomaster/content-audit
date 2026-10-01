# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
