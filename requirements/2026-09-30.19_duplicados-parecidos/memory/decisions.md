# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-DUPAR / F-DUPAR y nombre `near-duplicates` (estilo de `exact-duplicates`): nuevos.

2026-09-30 — analyst — Condición previa: misma respuesta (como el corrector, contraída = larga, sin el pronombre o
  demostrativo copiado del enunciado) y no exactos (R001).
  why: sin ella los pares más parecidos son contrastes a propósito («observed him clean» / «cleaning», coseno 0,995;
  «I am great» / «You are great»); con ella quedan 45 pares y los 6 casi iguales del 077 en escribir.

2026-09-30 — analyst — Dos caminos (R002): comodín para pronombres, demostrativos, posesivos y tiempo del auxiliar (24
  pares, 5 de 6 del 077) o parecido ≥ 0,95 con nomic-embed-text (21 pares más, incluido Rome / visited).
  why: el comodín solo no ve la misma idea con otro verbo; el modelo solo marca también cambios de sustantivo.

2026-09-30 — analyst — Mismo criterio de copia que FEAT-DUPEX (el primero del tema queda; n−1 hallazgos) y gravedad baja,
  mesa, que cuenta como error [ASSUMPTION, DOUBT-DUPAR-CUENTA].
  why: el 077 los contó como duplicados; la propuesta decía «para revisar».

2026-09-30 — analyst — Examen propuesto (R007): los casi iguales del 077 en las dos mitades, una muestra ciega de 40 + 40
  pares revisada por una persona y la estabilidad entre corridas.
  why: el 077 no tiene categoría propia; son 6 de los 15 de escribir/duplicados.json, pocos para medir precisión.
