# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-06-25 — analyst — Feature NUEVA (FEAT-SMODE), no evolucion de F-SLEN/F-QSENT/F-DBSENT
  Decision: crear FEAT-SMODE (modo de frase del knowledge REWRITE/FILL + frase
  canonica puntuable + estabilidad a traves de revision). 7 reglas (R001-R007),
  2 journeys flow (J001 scoring por modo, J002 revision REWRITE sin degradar).
  why: el concepto "que oracion es la canonica puntuable" no es de ningun feature
  existente. F-SLEN puntua una oracion opaca; F-QSENT/F-DBSENT derivan la plain
  sentence igual para todo quiz (mode-blind), que es justo el bug. El modo es un
  concern transversal nuevo (course-structure + scoring + revision) y merece
  feature propia con citas a F-SLEN/F-QSENT/F-DBSENT/F-LAPS/F-DSLEN.

2026-06-25 — analyst — Modo declarado por KNOWLEDGE, no por quiz (R001/R002)
  why: tipo pedagogico homogeneo dentro del knowledge; clasificacion empirica
  limpia (~211 REWRITE / ~397 FILL knowledges, 1 mal etiquetado). 608 = knowledges,
  no quizzes. A nivel quiz el heuristico da ~3961 REWRITE / ~7526 FILL (~11.487),
  consistente con los ~3976 quizzes que F-DBSENT reporta afectados por el patron
  transformacion.

2026-06-25 — analyst — DOUBT-SMODE-SOURCE OPEN (no bloqueante)
  Decision: dejar abierto de donde sale el modo de cada knowledge (dato explicito
  vs inferencia de la forma vs hibrido). R001-R007 describen el comportamiento
  DADO el modo, asi que no bloquea. DOUBT-SMODE-NONSENTENCE resuelta: el modo
  aplica solo a quizzes-oracion; los no-oracion ya estan excluidos por F-SLEN-R001.

2026-06-25 — analyst — Validacion: requirement validate pasa [OK]
  why: warnings son colisiones de alias del domain-glossary.yaml global (no de este
  REQUIREMENT) + nota soft de linkear "Nivel CEFR" a glossary; ninguno bloqueante.
