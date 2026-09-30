# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — F-DUPEX-J001 en audit-domain, paquete exactduplicates: el analizador y sus colaboradores son package-private y los puertos se simulan; mismo criterio que F-QINST-J001/J002 y F-LREC.
2026-09-30 — qa-tester — Las 23: la prueba toma los 14 temas del curso del 29/9 con las copias (A1 17, A2 2, B1 4); la ausencia de marcas en los otros 399 la mide medir.py.
2026-09-30 — qa-tester — Para el analista: en Práctica mixta queda 224b (posición 44 de 80) y la copia es 2260 (65); el ejemplo de R004 los nombra al revés.

2026-09-30 — architect — La búsqueda es en onKnowledge, en el orden del curso: cada ejercicio contra los anteriores con
  CorrectorMatcher.sameAnswer; el primero que termina igual queda (estable entre corridas). Diagnóstico sólo en la copia.
2026-09-30 — architect — El ejercicio terminado lo arma el renderizador del integrador (finishedSentence con FILL: texto entero, sin
  pista, con la primera respuesta o la correcta).

2026-09-30 — analyst — DOUBT-DUPEX-PISTA → A (José, 30/9): no se compara la pista (23 copias). R001 VALIDATED.

2026-09-30 — analyst — DOUBT-DUPEX-CUAL-QUEDA → A y DOUBT-DUPEX-ENTRE-TEMAS → A (José, 30/9, con la recomendación): se marca la copia
  que viene después en el orden del tema; los iguales de temas distintos quedan afuera. R002 VALIDATED.

2026-09-30 — analyst — «Como el corrector» ahora incluye la tabla de formas equivalentes (F-RESPC-R001): medido sobre el 29/9, siguen
  siendo 23 copias (A1 17, A2 2, B1 4); ningún par del mismo tema difiere sólo en contraída y larga.

2026-09-30 — analyst — Código FEAT-DUPEX / F-DUPEX: nuevo, no aparece en ningún requirement de las ramas de ronda 1.

2026-09-30 — analyst — Se compara el ejercicio terminado como lo ve el alumno (texto fijo + primera respuesta), no la oración guardada.
  why: con la oración guardada, las reescrituras y diálogos (REWRITE guarda sólo la respuesta) daban 126 «copias», 107 en B1
  («So am I.» contestando a oraciones distintas); con el texto fijo quedan 23 y coinciden con el 077.

2026-09-30 — analyst — Todo hallazgo en el ejercicio (copia), nunca en el tema: esquiva DOUBT-HALLAZGO-DE-TEMA de FEAT-HALL.

2026-09-30 — analyst — Sin pista en la comparación [ASSUMPTION], como duda para José (23 contra 22 copias).
