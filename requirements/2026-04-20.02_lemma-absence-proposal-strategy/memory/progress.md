# Progress

2026-04-20 — analyst — refinamiento del modelo del output de la estrategia aplicado. REQUIREMENT.md validado con Sentinel CLI (OK). Pendiente: arquitectura (sentinel.yaml + contratos); resolver DOUBT-CANDIDATE-NOTATION antes de o junto con la arquitectura.

2026-04-20 — analyst — limpieza de "distractores"/"opciones" multiple-choice en REQUIREMENT.md (quizzes son CLOZE, no MCQ). REQUIREMENT.md revalidado OK. Reglas, ids y numeracion intactos; solo se editaron textos descriptivos en intro, alcance, R009, R011, R012, R013, Limitaciones, Assumption 3 y DOUBT-CANDIDATE-NOTATION.

2026-04-23 — architect — arquitectura completa. architectural_patch.yaml + TECH_SPEC.md escritos y validados (15 fences, 0 conflicts). Seis doubts resueltos (ver decisions.md). Pendiente: (1) usuario decide proveedor concreto del LemmaAbsenceQuizCandidateGenerator y su modulo destino; (2) @qa-tester escribe tests por el arbol de nuevos componentes.

2026-04-22 — analyst — alineacion del REQUIREMENT con FEAT-QSENT: candidato pasa a ser { quizSentence (DSL de FEAT-QSENT) + translation }; R007 input de la estrategia usa quizSentence en lugar de sentence; R009/R011/R019 colapsan los 3 sub-elementos (blanks/respuesta/variantes) dentro del quizSentence; R012 parsea el quizSentence via FEAT-QSENT para derivar texto plano + quizForm; DOUBT-CANDIDATE-NOTATION cerrado como resuelto por FEAT-QSENT; Assumption 6 nueva sobre la DSL como unica via de intercambio. Validado con Sentinel CLI (OK). Dependencia declarada sobre FEAT-RCLA (exponer quizSentence en CorrectionContext); REQUIREMENT de FEAT-RCLA NO modificado.

2026-04-22 — architect — re-apertura de FEAT-LAPS. Patch de precision escrito (7 mods, 0 adds, 0 deletes) y tech spec reescrito (5 fences). Validados vs. sentinel.yaml, 0 conflicts. El scaffolding Java existente (7311a9b) queda INTACTO: las 4 firmas de port que ganan `throws` son las mismas clases @Generated ya existentes — Sentinel reenganchara el throws al regenerar, sin orfanizar ningun archivo. Archivos editados: architectural_patch.yaml (regenerado), TECH_SPEC.md (reemplazado --replace), memory/decisions.md, memory/progress.md. Pendiente: (1) developer regenera/re-implementa DispatchingReviser.java (drift vs. sentinel.yaml, no scope architect); (2) usuario decide adapter concreto para LemmaAbsenceQuizCandidateGenerator; (3) @qa-tester cubre las 6 superficies de test enumeradas al final del TECH_SPEC.

2026-04-24 — test-writer — 34 handwritten stubs + 9 journey paths implementados. revision-domain: 77/77 tests pasan (0 fallos). audit-cli: 3/3 DefaultProposalStrategySelectorTest pasan. Journey: FLapsJ001 path-4 pasa (R006 contract), paths 1-3 @Disabled (generator no wired); FLapsJ002/J003/J005 @Disabled; FLapsJ004 pasa. Clave: DispatchingReviser ya implementado por developer — lanza excepciones (no null), tests corregidos para matchear.

2026-04-24 — test-writer — 5 LAPS journey tests completamente reescritos como tests en memoria (no CLI subprocess). 9/9 paths pasan, 0 @Disabled, 0 skipped. Todos los stores son mocks Mockito; engine creado via DefaultRevisionEngineFactory; generator es lambda inline configurable por path. No regressions en audit-cli (FRevaprJ002/J003 ya fallaban antes por tiny-course fixture — preexistente).

2026-04-24 — developer — implementacion completa de FEAT-LAPS (6 componentes + wiring). Archivos implementados:
  - DefaultLemmaAbsenceProposalStrategyRegistry (active/byName/listAll por nombre).
  - DefaultLemmaAbsenceProposalDeriver (parse quizSentence via QSENT, copia ids/instrucciones del before, R012/R014).
  - LemmaAbsenceMvpStrategy (id=STRATEGY_ID constante, handles=LEMMA_ABSENCE, propose delega al generator).
  - DispatchingReviser extendido: constructor 4-args (byKind, fallback, strategyRegistry, deriver); rama LEMMA_ABSENCE; NoActiveStrategyException interna para R006.
  - DefaultRevisionEngine: catch NoActiveStrategyException->NO_ACTIVE_STRATEGY y ProposalStrategyFailedException->STRATEGY_FAILED en Step 5.
  - DefaultRevisionEngineFactory: pasa registry+deriver al DispatchingReviser.
  - DefaultProposalStrategySelector (env var CONTENT_AUDIT_LAPS_STRATEGY, default lemma-absence-mvp, InvalidProposalStrategyException si invalido).
  - Main.java: wiring completo con stub generator deterministico (fixture quizSentence valido en DSL FEAT-QSENT).
  - DefaultLemmaAbsenceProposalStrategyRegistry y DefaultLemmaAbsenceProposalDeriver: hechas public para acceso desde composition root.
  Resultados: revision-domain 60/60 tests pasan (8 stubs de DispatchingReviser+LemmaAbsenceMvpStrategy pendientes para @test-writer). audit-cli passes 9/9 LemmaAbsenceMvpStrategyTest, 5/5 DefaultLemmaAbsenceProposalStrategyRegistryTest, 9/9 DefaultLemmaAbsenceProposalDeriverTest, 3/3 DefaultProposalStrategySelectorTest — los REVAPR journey failures son pre-existentes (fixture sin tasks o arquitectura de test + classpath). Pendiente: adapter concreto LemmaAbsenceQuizCandidateGenerator (fuera de scope LAPS).
