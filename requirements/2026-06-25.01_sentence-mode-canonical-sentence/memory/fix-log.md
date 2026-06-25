# Fix Log

Implementation/test fixes that worked, with a short why. Newest on top.

<!-- entries below -->

2026-06-25 — developer — Python script para re-homing bulk AuditableKnowledge 7→8 args
  Usé Python para detectar todos los constructores de 7 args en /src/test/. Caso edge: cuando
  AuditableKnowledge está ANIDADO en List.of(...) dentro de otro constructor en la misma línea,
  rfind(')') agarra el cierre del outer constructor. Fix manual para esos casos.
  IAuditEngineTest.java:200-201 necesitó fix manual doble (AuditableKnowledge inner + revert null en AuditableTopic).

2026-06-25 — test-writer — SentenceLengthAnalyzerTest: AuditableKnowledge 7-arg → 8-arg en tests previos
  Constructor cambio de 7 a 8 args (+ sentenceMode) en FEAT-SMODE. Fix: null como 8vo arg en los 21
  calls existentes. Efecto nulo en comportamiento (sentenceMode=null = legacy).

2026-06-25 — test-writer — NoClassDefFoundError SentenceMode al correr tests de audit-domain
  course-domain-0.0.1.jar en .m2 era stale (pre-FEAT-SMODE, sin SentenceMode.class).
  Fix: mvn install -pl course-domain -DskipTests para regenerar el jar local.

2026-06-25 — test-writer — Stubs FEAT-SMODE implementados; SentenceMode importado en el test
  Fixture clave REWRITE: TEXT "You should watch the DVD." + CLOZE ["Watch the DVD."].
  Test null-fallback: llama toPlainSentences(form, null) y compara contra toPlainSentences(form, FILL).
  why: el metodo en la interfaz existe (describeComponent confirmado); solo falta la implementacion de produccion.

2026-06-25 — qa-tester — Cobertura de tests FEAT-SMODE propuesta (architectural_patch.yaml)
  9 handwrittenTests + placement de J001/J002. Mapeo regla -> superficie observable:
  - R003/R004 -> DefaultQuizSentenceConverter.toPlainSentences(form,mode)
    (FILL=oracion completa, REWRITE=solo respuesta excluyendo el TEXT-andamio; null->FILL).
  - R005 -> SentenceLengthAnalyzer (score sobre el tokenCount de la frase canonica del modo).
  - R006 -> DefaultLemmaAbsenceProposalDeriver.derive(before,candidate,mode) usa el modo recibido.
  - R007 -> caso DVD: en analyzer (100% / 4 tokens vs 60% / 10) y en deriver (revision conserva
    longitud -> score identico).
  why: el analyzer es mode-blind (puntua quiz.getTokens()); la seleccion de la frase canonica por
  modo vive en el converter, por eso R003/R004 se anclan al converter y R005/R007 al analyzer.

2026-06-25 — qa-tester — DUDA BLOQUEANTE de trazabilidad: R001 y R002 sin superficie observable
  R001 (todo sentence-knowledge tiene modo valido) y R002 (modo uniforme por knowledge) son
  invariantes de DATO, no de comportamiento de impl. R002 tiene Error N/A (define granularidad) y
  esta estructuralmente garantizada porque sentenceMode vive en KnowledgeEntity, no en quiz. R001
  no esta enforced por el tipo (sentenceMode es NULLABLE por diseno + null->FILL fallback); el
  unico chequeo posible seria un paso de validacion/siembra de datos, EXPLICITAMENTE fuera de
  alcance. No se fuerza tag a la regla mas cercana. Pendiente: reformular/retirar con @analyst,
  test estructural, o gate de journey (J001 ya gate-ea R001/R002 en leer_modo).

2026-06-25 — qa-tester — Gap R006 en el path de aplicación: handwrittenTest sobre DefaultCourseElementLocator
  R006 estaba [covered] solo a nivel deriver. El replace() del locator reconstruye el KnowledgeEntity
  afectado y descarta sentenceMode (línea 104: pasa null como 12vo arg, en vez de knowledge.getSentenceMode()).
  Test propuesto (TDD, hoy FALLA): course con knowledge REWRITE + quiz; replace(course, snapshot) reemplaza
  el quiz; el knowledge resultante en el course devuelto conserva REWRITE (no null). Trazabilidad DIRECTA a R006.
  Impl destino: DefaultCourseElementLocator (revision-domain, paquete engine, package-private). Sin clase de
  test previa (NO TESTS) → el stub DefaultCourseElementLocatorTest se crea al generar, en el paquete engine.
  Patch: requirements/2026-06-25.01_sentence-mode-canonical-sentence/architectural_patch.yaml (1 modification).
  Gotcha del patch: el impl vive en package 'engine'; hay que anidar bajo modules[].packages[name=engine], no
  en module root (el validador rechaza module-root con "declared at (module root) ... but ... at package engine").
  arreglo lo hace developer (pasar knowledge.getSentenceMode() en vez de null).

2026-06-25 — qa-tester — Gap R006 en el path de OVERRIDE: round-trip pierde sentenceMode (2 handwrittenTests)
  Las revisiones del dashboard van por contextSource OVERRIDE: correctionContext -> JSON -> re-parse.
  El round-trip descarta sentenceMode en dos lugares (confirmado en source, hoy ambos tests FALLAN/TDD):
  1. DefaultCorrectionContextJsonMapper.buildLemmaAbsenceContextMap (audit-cli, pkg commands, package-private):
     serializa quizSentence/sentence/... pero NO emite key "sentenceMode" (lineas 60-112). Test: serializar
     un LemmaAbsenceCorrectionContext con sentenceMode=REWRITE debe producir payload con "sentenceMode".
     Sin clase previa (handwrittenTests:[]) -> el stub DefaultCorrectionContextJsonMapperTest se crea en pkg commands.
  2. LemmaAbsenceContextStructuralValidator.validateAndBuild (revision-domain, pkg contextoverride, package-private):
     reconstruye LemmaAbsenceCorrectionContext pasando null como 17vo arg (linea 232: "sourceAuditId, null"),
     nunca lee sentenceMode del JSON. Test: parsear payload con "sentenceMode":"REWRITE" -> getSentenceMode()==REWRITE.
     Elegida esta impl (no el parser publico) porque es donde vive el bug y su validateAndBuild devuelve el context
     directo (superficie observable directa); ya tiene clase de test en el pkg correcto -> visibilidad OK.
  Trazabilidad DIRECTA a F-SMODE-R006 (la revision conserva el modo del knowledge); R006 ya estaba [covered] a nivel
  deriver+locator, pero el path OVERRIDE es otra impl que realiza la misma regla (comportamiento distribuido).
  Patch: requirements/2026-06-25.01.../architectural_patch.yaml (2 modifications, 0 conflicts). NO aplicado.
  Gotcha: patch propose REEMPLAZA el proposal file; verifique que el test previo de DefaultCourseElementLocator
  (R006) YA estaba aplicado en sentinel.yaml (lineas 8568-8577), asi que no se perdio trabajo pendiente.
