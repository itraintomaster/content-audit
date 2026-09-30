# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — La proyección y la comparación de la tanda son las del paquete F (F-PROYU-R001, F-METAS-R001/R008).

2026-09-30 — analyst — Código FEAT-TANDA / F-TANDA: nuevo.

2026-09-30 — analyst — La tanda toma sólo errores (+ LEMMA_ABSENCE que cubre un error); el largo y el resto del vocabulario quedan fuera (R001).
  why: José 11/8 (el largo no rechaza), el 077 no corrigió largos y 3.051 tareas triplicarían la mesa; siguen con revise.

2026-09-30 — analyst — Una sola tanda abierta; sus tareas no las toman revise ni revise-instructions (R002, mensaje [ASSUMPTION]).

2026-09-30 — analyst — La vista previa por propuesta (F-PIPRE-R001) se mantiene; la tanda suma el recálculo combinado.
  why: sacarla sería cambiar una decisión; su costo (~38 s cada una con el caché de hoy) es tarea del arquitecto (caché por oración).

2026-09-30 — analyst — Publicar = aprobar juntos los pendientes de la tanda; nunca producción (R009).
