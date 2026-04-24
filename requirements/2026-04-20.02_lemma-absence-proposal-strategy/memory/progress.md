# Progress

2026-04-20 — analyst — refinamiento del modelo del output de la estrategia aplicado. REQUIREMENT.md validado con Sentinel CLI (OK). Pendiente: arquitectura (sentinel.yaml + contratos); resolver DOUBT-CANDIDATE-NOTATION antes de o junto con la arquitectura.

2026-04-20 — analyst — limpieza de "distractores"/"opciones" multiple-choice en REQUIREMENT.md (quizzes son CLOZE, no MCQ). REQUIREMENT.md revalidado OK. Reglas, ids y numeracion intactos; solo se editaron textos descriptivos en intro, alcance, R009, R011, R012, R013, Limitaciones, Assumption 3 y DOUBT-CANDIDATE-NOTATION.

2026-04-23 — architect — arquitectura completa. architectural_patch.yaml + TECH_SPEC.md escritos y validados (15 fences, 0 conflicts). Seis doubts resueltos (ver decisions.md). Pendiente: (1) usuario decide proveedor concreto del LemmaAbsenceQuizCandidateGenerator y su modulo destino; (2) @qa-tester escribe tests por el arbol de nuevos componentes.

2026-04-22 — analyst — alineacion del REQUIREMENT con FEAT-QSENT: candidato pasa a ser { quizSentence (DSL de FEAT-QSENT) + translation }; R007 input de la estrategia usa quizSentence en lugar de sentence; R009/R011/R019 colapsan los 3 sub-elementos (blanks/respuesta/variantes) dentro del quizSentence; R012 parsea el quizSentence via FEAT-QSENT para derivar texto plano + quizForm; DOUBT-CANDIDATE-NOTATION cerrado como resuelto por FEAT-QSENT; Assumption 6 nueva sobre la DSL como unica via de intercambio. Validado con Sentinel CLI (OK). Dependencia declarada sobre FEAT-RCLA (exponer quizSentence en CorrectionContext); REQUIREMENT de FEAT-RCLA NO modificado.
