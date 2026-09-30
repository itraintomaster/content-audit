# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-RECUR / F-RECUR; el analizador conserva su nombre `lemma-recurrence`.
  why: F-HALL-R005 (un solo nombre).

2026-09-30 — analyst — Umbrales 50/1.000, top 2.000, orden A1→B2 y puntaje de curso quedan como regla (R001); nada nuevo es puntaje.
  why: regla de oro del 73,9 %; el top 2.000 no corta hoy (el puesto 2.000 cae entre los 1.041 lemas vistos una vez), que equivale a la opción B que José marcó en DOUBT-TOP-N-VALUE.

2026-09-30 — analyst — La recurrencia por nivel se mide dentro del nivel y se publica como sub-métrica (ASSUMPTION, R004); como puntaje de nivel es DOUBT-PUNTAJE-POR-NIVEL.
  why: José marcó B en DOUBT-PER-LEVEL-ANALYSIS de FEAT-LREC, pero como puntaje bajaría los niveles (A1 96,5 → 88,4); como detalle no mueve nada (F-HALL-R010).

2026-09-30 — analyst — Se publica el «hueco más largo» (nuevo) además de la desviación (F-LREC-R011 sigue informativa).
  why: responde en palabras de pantalla a la pregunta de José sobre qué es la desviación («suppose»: promedio 696, hueco 27.379).

2026-09-30 — analyst — Lemas vistos una vez: siguen excluidos (F-LREC-R006); incluirlos es DOUBT-UNA-SOLA-VEZ (bajaría a 0,05 y 73,5 %).
  why: ya son hallazgo de lemma-count (FEAT-POCOV); es la superposición que José temía.
