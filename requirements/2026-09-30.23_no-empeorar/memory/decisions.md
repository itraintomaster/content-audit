# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Alineado con el paquete F (ronda2-antes-despues, FEAT-METAS/PROYU/CIEGA, sin commitear al 30/9):
  la tanda lee la comparación y el veredicto de F; NOEMP dice qué es empeorar (grupo A, por cambio) y qué pisos debe
  cumplir esa comparación (grupo B = pisos por defecto de F-METAS-R005). Única diferencia: F propone devolver por un
  hallazgo nuevo de largo (DOUBT-VUELVE-VOCABULARIO); acá no (F-QICOR-R011, José 11/8). DOUBT-NO-EMPEORAR se contesta junto.
  why: evitar dos compuertas que digan distinto; el orquestador ve las dos ramas.

2026-09-30 — analyst — Código FEAT-NOEMP / F-NOEMP: nuevo.

2026-09-30 — analyst — La compuerta cuenta por ejercicio e identidad; la tolerancia de 0,5 puntos queda sólo para el vocabulario.
  why: medido: con grandmother ningún promedio de A1 ni del curso se movió un milésimo; sólo la cuenta 351 → 352.

2026-09-30 — analyst — «Palabra roja» = la del semáforo (lexis del nivel, salvo lo que el tema enseña); veta en la mesa (R002).
  why: es la no-regresión léxica de F-LASAG-R007 #7 y F-QICOR-R004 crit. 2, con la excepción de F-SEMAF-R004; más estricta que off-level-words (2 niveles).

2026-09-30 — analyst — El largo no veta un cambio (mantiene F-QICOR-R011, José 11/8) pero cuenta en la tanda con la tolerancia.

2026-09-30 — analyst — Conteos contra el análisis con que abrió la tanda; vocabulario contra la línea base fija, tolerancia acumulada (R005, R006) [ASSUMPTION en el 0,5].
  why: los conteos no pueden subir en ninguna tanda; los números de vocabulario de curso se comparten entre tandas.
