# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Confirmado con `get-consolidated` del CLI real sobre un workdir de scratch: mismos valores de COCA por nivel que el prototipo en la foto con pendientes y 55 topics con 0 tokens COCA en la foto con lo aprobado.

2026-09-30 — analyst — Código FEAT-CERO / F-CERO: nuevo.

2026-09-30 — analyst — Medido con prototipo (mismo cableado que Main, base del 29/9): la misma instancia del motor corrida dos veces sobre el mismo curso da 72,90 / 92,38 · 89,99 · 82,48 · 66,70 contra 73,92 / 96,54 · 94,16 · 79,45 · 63,51; cambian 60 nodos (curso, 4 niveles, 55 topics). Dos motores nuevos dan idéntico.
  why: COCA duplica sus listas de topics (los tokens de nivel se duplican: A1 11.433 → 22.866; los cuartos 3 y 4 quedan vacíos) y lemma-recurrence sigue su posición global (8,0 → 4,0).

2026-09-30 — analyst — R002 porque la segunda corrida escribe sobre los nodos de la primera: los 55 topics del primer informe quedan con diagnóstico COCA vacío.
  why: CocaBucketsAnalyzer busca el topic por id en su lista acumulada y encuentra primero el nodo de la corrida anterior.

2026-09-30 — analyst — lemma-absence y lemma-count arrastran la unión de los cursos (por código); en el caso medido no movieron números.
  why: las 4 propuestas no sacaron la última aparición de ninguna palabra.

2026-09-30 — analyst — Dónde pega hoy: `get consolidated` con pendientes (el dashboard muestra la foto con pendientes en los paneles de nivel, topic y tema) y `revise-instructions`, que calcula hasta 20 impactos por invocación.
