# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect (C) — La causa de un FAILED es EvaluationResolution.failure: EvaluationNotEmitted, el modelo
  que ya devolvía el evaluador (tipo + motivo). Un campo y no dos: no se inventa otro par tipo/motivo.
2026-09-30 — architect (C) — La cuenta de ejercicios es un tipo propio, JudgeQuizTally, en JudgeUnitTally y en
  JudgeCoverageDiagnosis: el runtime la copia sin recalcular; con unidad QUIZ repite los números de las unidades.
2026-09-30 — architect (C) — El renderizador se inyecta en los tres clasificadores (requiresInject), no se pasa por
  parámetro: los proveedores que los crean ya lo tienen (gancho C-15).
2026-09-30 — architect (C) — DefaultQuizInstructionConfig recibe JudgeRuntimeConfig para describir el tope de analyze sin
  copiar el 0 (F-JEXAM-R010); su tope propio sigue siendo el de revise-instructions (500).
2026-09-30 — analyst (C) — La carpeta es la .32 a mano: `feature create` numera por cantidad de carpetas del día (30) y
  habría creado otra .31.
