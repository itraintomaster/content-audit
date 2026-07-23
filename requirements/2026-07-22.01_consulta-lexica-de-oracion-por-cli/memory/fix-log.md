# Fix Log

2026-07-22 — qa-tester — Propuesto el patch de cobertura F-CLEX (12 handwrittenTests + placement de F-CLEX-J001). `patch propose` OK (0 add, 4 mod, sin conflictos).
  Tests por regla: R001 x1, R002 x2 (FILL/REWRITE), R003 x1, R004 x2 (rank + rank null), R005 x2 (omite justificada / vacia=limpia) sobre DefaultSentenceLexicalEvaluator; R006 x2 (flags exactos audit / no flaggea justificadas por rebaja-frase+metalenguaje) sobre DefaultSentenceLexicalScorer; R001/R002/R007 sobre LexisCmd. Placement J001: audit-cli / com.learney.contentaudit.auditcli.commands (LexisCmd es package-private en `commands`; el journey cubre reject_dsl/reject_level que solo valida el comando).
  why: R006 se cubre en el scorer compartido porque el analyzer delega en el (TECH_SPEC, fuente unica); la paridad analyzer-side la sigue guardando la bateria FEAT-LABS.

2026-07-22 — qa-tester — `patch apply --as=qa-tester` RECHAZA este patch por dos falsos positivos del guard (no son leak real; el contenido es 100% handwrittenTests + testModule/testPackage):
  (1) `modules[*].packages` — el guard no acepta handwrittenTests anidados en packages, pero los impls viven en packages (labs/lexicalflags/commands), unica via valida de direccionarlos.
  (2) `journeys[F-CLEX-J001].validation` — `patch propose` inyecta `validation: AUTO_VALIDATED` desde sentinel.yaml (persiste incluso con --replace); el guard lo lee como "journey body".
  Handoff: aplicar con `sentinel patch apply -p <patch>` PLANO (sin --as), como el patch tests-only de lemma-absence 2026-07-22T20-56 (mismo shape con packages) que si se aplico; luego `sentinel generate`. NO forzar quitando --as por mi parte (AGENTS.md Rule E: aplica el usuario).

2026-07-23 — test-writer — Confirmo independientemente (desde audit-cli) el bug de visibilidad ya
  escalado arriba: `mvn install -pl audit-domain` falla porque `SentenceLexicalScorer` (package labs,
  visibility=internal) se inyecta en `DefaultSentenceLexicalEvaluator` (package lexicalflags, publico) ->
  package-private cruzando paquetes hermanos del mismo modulo. Esto bloquea tambien `mvn test-compile -pl
  audit-cli` (LexisCmd no resuelve `SentenceLexicalEvaluator` porque el jar instalado de audit-domain es
  anterior al paquete lexicalflags). LexisCmdTest y FClexJ001JourneyTest quedan escritos y revisados a
  mano contra el contrato, pendientes de recompilar cuando @architect resuelva la visibilidad.

2026-07-23 — architect — FIX wrong_visibility (F-CLEX): SentenceLexicalScorer estaba `visibility: internal` => interface package-private en Java => javac "not public in ...labs; cannot be accessed from outside package" al ser inyectada en DefaultSentenceLexicalEvaluator (pkg lexicalflags) y cableada en Main. Corregido a `visibility: public` MANTENIENDOSE en el package labs (que sigue internal).
  why: el ocultamiento cross-modulo lo da la visibility del PACKAGE (labs internal), no la de la interface. internal en la interface era redundante y ademas la volvia package-private, rompiendo el wiring same-module cross-package. SentenceLexicalScore (model, default public) y DefaultSentenceLexicalScorer (public) no requerian cambio. Patch correctivo propuesto (2 mods, 0 conflicts), SIN aplicar. Nota: TECH_SPEC.md conserva la narrativa de diseño completa del patch aplicado; el proposal activo es solo el fix de visibilidad.

2026-07-23 — developer — Extraida la logica de `scoreQuiz` (LemmaByLevelAbsenceAnalyzer) 1:1 a
  `DefaultSentenceLexicalScorer.score(tokens, sentenceLevel)`; el analyzer paso a delegar y descarto los
  helpers privados que quedaron muertos (`isExcludedFromPlacement`, `isAlphabetic`,
  `normalizeFrequencyRank`, `outOfCatalogDiscount`, `PROPER_NOUN_POS`) mas el import `Locale` (ya sin uso).
  `DefaultSentenceLexicalEvaluatorTest` (2 nuevos casos) y `DefaultSentenceLexicalScorerTest` (4 nuevos)
  pasaron al primer intento porque el mapeo de campos coincide 1:1 con lo que ya emitia `scoreQuiz`
  (`MisplacedLemma.observedPos`->flag.pos, `.foundInLevel`->flag.sentenceLevel, `.expectedLevel`->
  flag.expectedLevel; `OutOfCatalogWord.observedPos`/`frequencyRank` directos).
  why: no repliqué logica nueva — F-CLEX-R006 exige la MISMA fuente, así que copiar el cuerpo exacto
  (no una reescritura) es lo unico que garantiza paridad estructural sin depender de que ambos lados
  se mantengan sincronizados a mano.

2026-07-23 — developer — Al inyectar el 4º dependency (`sentenceLexicalScorer`) el constructor de
  `LemmaByLevelAbsenceAnalyzer` rompio 3 tests de FEAT-LABS con `@InjectMocks` (LemmaByLevelAbsenceAnalyzerTest,
  FLabsJ001JourneyTest, FLabsJ006JourneyTest): Mockito deja el nuevo parametro en null porque no hay un
  `@Mock` de ese tipo declarado. Fix: agregado `@Mock private SentenceLexicalScorer sentenceLexicalScorer`
  + un `@BeforeEach` que stubea `.score(any(), any())` delegando a un `DefaultSentenceLexicalScorer` REAL
  construido con los mismos 3 mocks (evpCatalogPort/contentWordFilter/lemmaAbsenceConfig) que el test ya
  tenia. Es wiring puro (cero assertions/logica de test tocada): el escenario en rojo era "constructor
  aditivo sin el mock nuevo", NO una divergencia del scoring extraido — por eso el fix va en el setup, no
  en las assertions ni en la produccion.
  why: usar el scorer REAL (no un stub que devuelva valores fijos) es lo que preserva el comportamiento
  EXACTO que esos 354 tests ya verificaban linea por linea; un mock con .thenReturn(valorFijo) hubiese
  hecho pasar los tests sin probar nada del pipeline real.

2026-07-23 — developer — Wireado FEAT-CLEX en `Main` (audit-cli): `sentenceLexicalScorer` se construye
  una vez (mismo evpCatalog/DefaultContentWordFilter/lemmaAbsenceConfig ya usados por el analyzer) y se
  comparte entre `LemmaByLevelAbsenceAnalyzer` (4º arg) y `DefaultSentenceLexicalEvaluator`; subcomando
  "lexis" registrado junto a "tokens" con `quizSentenceConverter`/`nlpTokenizer` ya existentes + el nuevo
  evaluator. `LexisCmd.queryLexis` imprime `SentenceLexicalFlags` vía `new ObjectMapper().writeValueAsString`
  (mismo patron que GetCmd) en vez de serializacion manual.
  why: compartir la MISMA instancia del scorer entre analyzer y CLI hace la paridad F-CLEX-R006 verificable
  por construccion (un solo objeto, un solo EvpCatalogPort resuelto una vez), no solo por tener la misma clase.
