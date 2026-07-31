# Progress — FEAT-QINST

2026-07-29 — analyst — REQUIREMENT.md creado: 14 reglas, 3 journeys de flujo (J001 puntaje+diagnostico+falla del juez, J002 cobertura parcial y reanudacion, J003 omision/exclusion/re-evaluacion) y 5 doubts abiertos. `requirement validate` [OK]; `feature list` = FEAT-QINST 14 reglas / 3 journeys.
  why: el juez de consigna (agente quiz-instruction-validator) ya existe y esta probado por separado; lo que falta es incorporarlo a la auditoria sin romper los tres supuestos de los analisis actuales (evaluar es gratis, la corrida siempre termina, repetir da lo mismo).

2026-07-29 — analyst — La capacidad de "resultado costoso reutilizable y reanudable" se separo en FEAT-EVCOST (requirements/2026-07-29.03_evaluaciones-costosas-reutilizables). QINST es su primer consumidor y la cita, no la redefine.
  why: pedido explicito del usuario — el patron se va a repetir con otros evaluadores costosos.

2026-07-29 — analyst — Proximo paso: resolver DOUBT-QUIZ-COMPLETO (bloqueante) con @architect/usuario antes de avanzar a arquitectura. Las otras 4 dudas (escala de severidad, confianza, presupuesto por defecto, alcance de ejercicios) no cambian la forma de ninguna regla; DOUBT-ALCANCE-EJERCICIOS conviene medirla antes de la primera pasada completa porque cambia cuanto se paga.

2026-07-30 — qa-tester — Diseno de cobertura propuesto: 66 handwrittenTests sobre 14 impls + placement de las 3 journeys (J001/J002 -> audit-domain.quizinstructionengine, J003 -> audit-application). Patch en architectural_patch.yaml, SIN aplicar. Las 14 reglas quedan con test explicito propio (sin cobertura transitiva).
  why: J001/J002 van a quizinstructionengine porque QuizInstructionAnalyzer y los tres colaboradores (scorer, subjectBuilder, scopeMatcher) son package-private: solo la factory es publica. J003 va a audit-application porque su punto de entrada es AuditRunner.runAudit(path, AuditRunRequest) y ahi conviven DefaultAuditRunner y DefaultQuizInstructionConfig; el mapeo de flags del CLI queda cubierto por handwrittenTests sobre AnalyzeCmd.

2026-07-30 — qa-tester — R010 se cubre por la mitad "no reutiliza" (analyzer + version resolver). La mitad "no borra" NO se duplico sobre FileSystemEvaluationLedger a proposito.
  why: es literalmente el append-only de F-EVCOST-R006 y esa impl la cubre el qa-tester de EVCOST en paralelo; tagear ahi desde este patch arriesga colision al aplicar dos patches sobre la misma implementacion.
