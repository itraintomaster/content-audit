# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-24 — analyst — R016 nueva y R006/J001 enlazadas: la aceptación preserva objetivo, fórmula y selectores,
  además de resolver y no empeorar. Fuente: corrección humana de la propuesta can/could del 24/10.
  Next: alinear propuestas y evaluación con la preservación; sin cambios al curso ni consultas reales por esta tarea.

2026-10-05 — analyst — F-MESA-R001/R012 y DOUBT-MINITEORIA-ALCANCE alineadas: candidato y reescritura de miniteoria por
  quiz, sin agrupamiento ni propagacion por texto.
  Next: alinear mesa, preservacion y pruebas antes de corregir miniteorias.

2026-10-01 — developer — El agente que propone (revision-infrastructure/panelagent: lanzador, lector, fábrica) y su grafo
  `.sentinel/agents/panel-proposer/` (gancho E-15: generate → gate → emit, artifact `panelProposal`), con sus 3 pruebas;
  las tres de R014 (revise, revise-instructions y assess-candidate siguen como hoy fuera de una tanda). FEAT-MESA sin
  pruebas apagadas. El candidato trae sólo lo que dice (partes, opciones, traducción…) y la mesa lo apoya sobre el
  ejercicio como estaba (`TopicState.merged`).

2026-10-01 — developer — El motor de la mesa implementado (`DefaultTopicPanel`, `DefaultPanelRunner`, `CatalogTopicMeter`,
  `DefaultCandidateScreen`, `DefaultPanelDecisionScript`, `CappedQueryAllowance`, `ScriptedPanelProposer`, la fábrica) con
  54 pruebas en verde (20 + 4 + 10 + 6 + 8 + 2 y F-MESA-J001 con sus 4 caminos), sin ninguna consulta a un modelo: proponen
  `ScriptedPanelProposer` y los jueces son de mentira (`MesaFixture.RulesMeter`) o con libro de veredictos sobre el motor
  real (`MeterBench`). Next: el agente que propone (revision-infrastructure) y las tres de R014.

2026-10-01 — qa-tester — `patch propose` (fusión): 55 handwrittenTests (R001 4, R002 4, R003 6, R004 3, R005 5, R006 5,
  R007 3, R008 2, R009 2, R010 6, R011 3, R012 2, R013 2, R014 3, R015 5) y F-MESA-J001 en revision-domain, paquete
  panelengine. Los cuatro patrones de revision-domain intactos. `tech-spec write` y `patch validate -i` contra M11 del
  integrador y contra la cadena con pruebas: exit 0, 0 conflictos. Sin aplicar.

2026-09-30 — architect — Parche (3 altas, 2 cambios, 0 conflictos) y TECH_SPEC (9 fragmentos) en exit 0. Next: el orquestador revisa; se aplica después de la ronda 1 integrada y de F, en el orden TAREA → NOEMP → MESA → TANDA → BANDEJA; luego `definitions`, `feature sync`, @qa-tester. Ganchos en tablero/ronda2/ganchos-E.md.

2026-09-30 — analyst — REQUIREMENT v2 con José, 30/9: cuatro dudas RESOLVED (B, A, A, A), R015 nueva («Contracciones» a opción múltiple) y DOUBT-OPCIONES-CONTRACCIONES abierta. `requirement validate` → [OK].

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-MESA: 14 reglas, 1 journey, 4 dudas (temas a la vez, opción múltiple,
  miniteoría, quién refuta). Prototipo en el scratchpad (`e-correccion/temas_a_la_vez.py`, `carga_por_tema.py`): A1 278
  ejercicios de mesa en 63 temas (máx. 40), A2 451 en 74 (máx. 30); 4 temas y 4 consultas → A1 2,9 h, A2 4,7 h.
  `requirement validate` → [OK].
  Next: José contesta las dudas del paquete E; el integrador suma la carpeta a `definitions` y corre `feature sync`
  (no se tocó sentinel.yaml ni el glosario); después @architect, que parte de las notas para el arquitecto del informe.
