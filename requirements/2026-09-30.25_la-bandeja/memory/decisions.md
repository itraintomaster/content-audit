# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — La muestra ciega es la de FEAT-CIEGA (paquete F): la bandeja sólo la presenta; publicar exige el
  veredicto «pasa» de F-METAS-R008 además de la compuerta.
  why: F ya define sorteo, tamaño, respuestas y margen; repetirlo acá daba dos definiciones.

2026-09-30 — analyst — Código FEAT-BANDEJA / F-BANDEJA: nuevo.

2026-09-30 — analyst — Las propuestas de la tanda quedan pendientes hasta publicar: es el modo humano con un OK por tanda (R008).
  why: no cambia F-REVAPR; `approve` de una propuesta de la tanda abierta se rechaza (mensaje [ASSUMPTION]).

2026-09-30 — analyst — «Aprobar el candidato» sin ganador queda como excepción declarada de José; la compuerta no la vuelve a contar (R003).
  why: si no, la compuerta devolvería lo que José aprobó y la tanda no cerraría.

2026-09-30 — analyst — Lo decidido se recuerda por identidad del hallazgo + huella del contenido (R006), como el libro de evaluaciones.
