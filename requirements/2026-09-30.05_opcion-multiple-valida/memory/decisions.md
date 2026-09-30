# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-OMVAL / F-OMVAL: nuevo.

2026-09-30 — analyst — «Abre la oración» exige que lo que sigue al hueco termine en punto, pregunta o exclamación.
  why: sin eso se marcaban los 20 fragmentos de «A o an» («___ excited man»), que no son oraciones.

2026-09-30 — analyst — «Sólo en la forma» se lee como mayúscula/signos (R002) y artículo a/an de antes del hueco (R004).
  why: la lectura literal (artículo o número de la opción) marca 73 ejercicios de 4 temas donde eso es la lección; queda como duda.

2026-09-30 — analyst — Una sola correcta se verifica contra el backend: la respuesta vale sólo si lo elegido suma el puntaje entero
  (be/domain/quiz/pkg/evaluation/evaluation_multiple_choice.go), y todas las opciones múltiples son de selección SINGLE.

2026-09-30 — analyst — Choque con F-OPMUL-R006 (no se corrige ninguna opción múltiple): detectar no choca, aplicar sí → DOUBT-OMVAL-CORREGIR.
