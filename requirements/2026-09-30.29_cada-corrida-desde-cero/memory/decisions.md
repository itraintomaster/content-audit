# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — F-CERO-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`, como F-CDIFF-J001.
  why: el operador pide la vista consolidada y la prueba la compara con una invocación nueva de analyze; GetConsolidatedCmd, DefaultGetConsolidatedCommand y AnalyzeCmd son package-private ahí.
2026-10-01 — qa-tester — Las pruebas van en piezas que ya existen (IAuditEngine, los cuatro proveedores con estado, DefaultCourseProjector, DefaultImpactPreviewComputer); se distinguen de la de F-HALL-R014 porque piden lo mismo que una invocación nueva y que el primer informe no cambie.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura.

2026-09-30 — architect — Sin estructura propia: el modelo con HALL aplicado ya no inyecta un `ContentAnalyzer` en ninguna implementación y `IAuditEngine` pide un analizador nuevo por corrida al catálogo. Verificado en el código: el estado vive sólo en los cuatro analizadores de curso; agregación, recolector, calculador, puntuador léxico, mapeador (vacía sus mapas en cada `map`) y libro de evaluaciones (lee el archivo en cada consulta) no arrastran nada. El parche sólo registra FEAT-CERO.
  why: le queda propio la prueba y una regla para el developer (un proveedor no reusa entre dos `create` nada que el analizador modifique).

2026-09-30 — analyst — Confirmado con `get-consolidated` del CLI real sobre un workdir de scratch: mismos valores de COCA por nivel que el prototipo en la foto con pendientes y 55 topics con 0 tokens COCA en la foto con lo aprobado.

2026-09-30 — analyst — Código FEAT-CERO / F-CERO: nuevo.

2026-09-30 — analyst — Medido con prototipo (mismo cableado que Main, base del 29/9): la misma instancia del motor corrida dos veces sobre el mismo curso da 72,90 / 92,38 · 89,99 · 82,48 · 66,70 contra 73,92 / 96,54 · 94,16 · 79,45 · 63,51; cambian 60 nodos (curso, 4 niveles, 55 topics). Dos motores nuevos dan idéntico.
  why: COCA duplica sus listas de topics (los tokens de nivel se duplican: A1 11.433 → 22.866; los cuartos 3 y 4 quedan vacíos) y lemma-recurrence sigue su posición global (8,0 → 4,0).

2026-09-30 — analyst — R002 porque la segunda corrida escribe sobre los nodos de la primera: los 55 topics del primer informe quedan con diagnóstico COCA vacío.
  why: CocaBucketsAnalyzer busca el topic por id en su lista acumulada y encuentra primero el nodo de la corrida anterior.

2026-09-30 — analyst — lemma-absence y lemma-count arrastran la unión de los cursos (por código); en el caso medido no movieron números.
  why: las 4 propuestas no sacaron la última aparición de ninguna palabra.

2026-09-30 — analyst — Dónde pega hoy: `get consolidated` con pendientes (el dashboard muestra la foto con pendientes en los paneles de nivel, topic y tema) y `revise-instructions`, que calcula hasta 20 impactos por invocación.
