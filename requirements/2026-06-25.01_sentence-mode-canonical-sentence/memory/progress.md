# Progress

Current state, last action, next step.

<!-- entries below -->

2026-06-25 — test-writer — Implementados stubs R005 y R007 en SentenceLengthAnalyzerTest
  shouldComputeTheLengthScore... (R005): REWRITE knowledge, 4 tokens (canonica), rango [3,8] margen 5.
  Afirma score 1.0 y tokenCount=4 en SentenceLengthDiagnosis. El conteo erroneo (10 tokens) daria 0.6.
  shouldScoreAREWRITE...DVDAt100Percent... (R007): caso DVD exacto del bug. Score 1.0 sobre 4 tokens.
  Ambos pasan (27/27 en SentenceLengthAnalyzerTest). Ningun test previo roto.
  Efecto secundario corregido: todos los calls 7-arg a AuditableKnowledge() actualizados a 8-arg (null
  sentenceMode) porque el constructor cambio al agregar la field. Tambien requirio reinstalar course-domain
  en el repo local (SentenceMode.class no estaba en el jar cacheado).
  Siguiente paso: ninguno en este archivo. Falta implementacion de produccion (developer).

2026-06-25 — test-writer — Journey test F-SMODE-J001 implementado: ambos paths completos
  path-1 (FILL): fixture She dances/well/(good/well). con SentenceMode.FILL → verifica
  canonica "She dances well." (R003) y ausencia del hint "good" (R005).
  path-2 (REWRITE): fixture DVD-case con SentenceMode.REWRITE → verifica canonica
  "Watch the DVD." (R003/R004) y ausencia de la fuente (R004/R005).
  Gate R001/R002 ejercido por pasar un valor valido del enum SentenceMode en cada path.
  BLOQUEANTE: DefaultQuizSentenceConverter (Generated) no implementa toPlainSentences(FormEntity, SentenceMode).
  Archivo: course-domain/src/test/java/.../quizsentenceengine/FSmodeJ001JourneyTest.java
  Siguiente paso: @developer implementa toPlainSentences(FormEntity, SentenceMode) en DefaultQuizSentenceConverter.

2026-06-25 — test-writer — Implementados los 5 stubs FEAT-SMODE en DefaultQuizSentenceConverterTest
  Stubs implementados (R003 x3, R004 x2). Agregado import SentenceMode.
  Compilacion de test syntax: OK (el error es en produccion DefaultQuizSentenceConverter que aun no implementa toPlainSentences(form, mode)).
  Siguiente paso: @developer implementa toPlainSentences(FormEntity, SentenceMode) en DefaultQuizSentenceConverter.

2026-06-25 — test-writer — Implementados stubs R006 y R007 en DefaultLemmaAbsenceProposalDeriverTest
  Stubs implementados: shouldDeriveTheRevisedQuizCanonicalPhraseUsingTheREWRITEMode... (R006) y
  shouldKeepTheLengthScoreOfAREWRITERevision... (R007). Agregado import SentenceMode.
  Fixture: quiz REWRITE con [TEXT "You should watch the DVD.", CLOZE "Watch the DVD."] (4 tokens).
  Candidato: "You should watch the film. ____ [Watch the film.]" (respuesta 4 tokens = longitud conservada).
  Firma de 3 args: derive(before, candidate, SentenceMode.REWRITE). Compilacion del archivo: OK.
  Errores pre-existentes en DispatchingReviserTest/CannedLemmaAbsenceQuizCandidateGeneratorTest: no son de este trabajo.
  Siguiente paso: @developer implementa la firma de 3 args en DefaultLemmaAbsenceProposalDeriver.

2026-06-25 — test-writer — Implementado FSmodeJ002JourneyTest: ambos paths completos
  path-1 (conserva longitud): fixture DVD-case REWRITE, candidato "Watch the film." (misma longitud).
  Gate heredar_modo (R006): pasa SentenceMode.REWRITE explícito al deriver.
  Gate frase_revisada (R003/R004): verifica que sentences[0] NO contiene la fuente, SÍ contiene la respuesta.
  Gate comparar_score (R005): split length del antes == split length del despues.
  Gate score_estable (R007): misma longitud → score idéntico (no degradado).
  path-2 (cambia longitud): candidato "Do it." (diferente longitud). Gates R003/R004: sin arrastre de fuente.
  Gate score_refleja_respuesta (R004): canonical es más corta que concatenación fuente+respuesta.
  Archivo: revision-domain/src/test/java/.../engine/FSmodeJ002JourneyTest.java. test-compile: OK (revision-domain solo).
  BLOQUEANTE: DefaultLemmaAbsenceProposalDeriver no implementa derive(before, candidate, SentenceMode).
  Siguiente paso: @developer implementa la firma de 3 args en DefaultLemmaAbsenceProposalDeriver.

2026-06-25 — test-writer — DefaultCourseElementLocatorTest implementado (F-SMODE-R006)
  Fixture: course con hierarchy root/milestone/topic/knowledge/quiz donde knowledge.sentenceMode=REWRITE.
  elementAfter: snapshot del quiz revisado (mismo nodeId). Act: locator.replace(course, elementAfter).
  Assert: resultKnowledge.getSentenceMode() == REWRITE. Falla como esperado: expected REWRITE but was null.
  Fallo correcto: producción pasa null en linea ~104 en vez de knowledge.getSentenceMode().
  Developer debe corregir DefaultCourseElementLocator.replace() para propagar sentenceMode.

2026-06-25 — developer — Implementacion FEAT-SMODE completa: 2404 tests pasan (BUILD SUCCESS)
  Modulos implementados en orden:
  1. course-domain: DefaultQuizSentenceConverter.toPlainSentences(FormEntity, SentenceMode) + PlainSentenceDeriver
     - REWRITE excluye TEXT-scaffold antes del primer CLOZE (isScaffoldSentence: termina en .?!)
     - removeSpaceBeforePunctuation() post-processor para artefactos de stripping de hints
  2. course-infrastructure: FileSystemCourseRepository lee/escribe sentenceMode de _knowledge.json
  3. audit-application: CourseToAuditableMapper.mapKnowledge pasa ke.getSentenceMode() a AuditableKnowledge
  4. refiner-domain: LemmaAbsenceContextResolver extrae sentenceMode del ancestor KNOWLEDGE en resolve() y resolveWithIndex()
  5. revision-domain: DefaultLemmaAbsenceProposalDeriver.derive(before, candidate, mode) usa toPlainSentences(form, mode)
                     DispatchingReviser pasa lemmaContext.getSentenceMode() como 3er arg
  Mechanical re-homing: AuditableKnowledge (7→8 args), LemmaAbsenceCorrectionContext (16→17 args),
  derive (2→3 args) en todos los test files del proyecto. 2404/2404 tests pasan.
