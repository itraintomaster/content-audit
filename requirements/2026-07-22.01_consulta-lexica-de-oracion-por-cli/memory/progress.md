# Progress

2026-07-22 — analyst — Creada la feature FEAT-CLEX (F-CLEX) "Consulta lexica de oracion por CLI".
  Resuelve DOUBT-EVAL-LEXICO-CLI (Opcion A) de FEAT-LASAG: consulta read-only oracion+nivel -> flags lexicos.
  Estado: REQUIREMENT.md redactado y validado. Siguiente paso: arquitectura (nueva feature, aun no en sentinel.yaml).
  Nota: NO correr `feature sync` todavia — la feature no esta declarada en sentinel.yaml; la agrega el @architect.

2026-07-23 — test-writer — Implementados los 5 stubs de LexisCmdTest (R001, R002x2, R007x2) y los 4
  paths de FClexJ001JourneyTest, sobre LexisCmd (mocks de QuizSentenceConverter/NlpTokenizer/
  SentenceLexicalEvaluator, sin course store — estructuralmente cumple R001 read-only).
  BLOQUEADO: no se pudo verificar `mvn test-compile -pl audit-cli` — audit-domain no compila
  (ver fix-log). Ambos archivos de test estan completos y revisados a mano; falta re-verificar
  compilacion cuando @architect resuelva el bug de visibilidad.

2026-07-23 — test-writer — Implementados los 6 stubs de FEAT-CLEX en audit-domain:
  DefaultSentenceLexicalScorerTest (2, R006) y DefaultSentenceLexicalEvaluatorTest (4, R003/R004/R005).
  BLOQUEADO: `mvn compile -pl audit-domain` falla (preexistente, no causado por estos tests):
  `DefaultSentenceLexicalEvaluator.java` (package lexicalflags, publico) inyecta `SentenceLexicalScorer`
  (package labs, visibility=internal -> generado package-private) -> "is not public in ...labs; cannot
  be accessed from outside package". El diseño registrado en decisions.md dice "labs, interno" para el
  scorer, pero requiresInject lo referencia desde otro paquete -> inconsistencia de visibilidad en
  sentinel.yaml. Escalado a @architect (type: wrong_visibility). Los dos archivos de test quedan
  escritos y son correctos contra el contrato; no se pudo confirmar compilacion hasta que se resuelva.

2026-07-23 — developer — Implementada la produccion completa de FEAT-CLEX (fix de visibilidad del
  architect ya aplicado sobre sentinel.yaml). Piezas: (1) `DefaultSentenceLexicalScorer` — logica de
  emparejamiento MOVIDA desde `scoreQuiz` (exclusiones R035, match R036+repliegue R045, distancia
  R017-R020/R033, bandas OOC R034), sin duplicar; (2) `LemmaByLevelAbsenceAnalyzer.scoreQuiz` ahora
  delega en el scorer inyectado (constructor 3->4 deps); (3) `DefaultSentenceLexicalEvaluator` proyecta
  `SentenceLexicalScore` -> `SentenceLexicalFlags` (mapea `MisplacedLemma.observedPos`/`expectedLevel`/
  `foundInLevel` y `OutOfCatalogWord.observedPos`/`frequencyRank`, sin marcadores de justificadas); (4)
  `LexisCmd` (audit-cli, picocli `lexis -q/-m/-l`) parsea DSL vía `QuizSentenceConverter`, deriva la
  frase canonica por modo (FILL default), tokeniza con `NlpTokenizer.analyzeTokens`, llama al evaluator
  e imprime el JSON de `SentenceLexicalFlags` (Jackson `ObjectMapper`); nivel/DSL invalidos -> stderr +
  exit 2; (5) `Main` cablea `sentenceLexicalScorer` (compartido por el analyzer y el evaluator) y
  registra el subcomando "lexis" junto a "tokens".
  Verificado: audit-domain 361/361 verde (los 354 previos + 6 nuevos + 3 tests de FEAT-LABS reparados,
  ver fix-log); audit-cli 344/344 verde incl. LexisCmdTest (5) y FClexJ001JourneyTest (4). Humo real:
  `./audit-cli.sh lexis -q "My brother ____ [transferred] (transfer) money to his family." -l A1` ->
  `{"misplacedWords":[{"lemma":"transfer","pos":"VERB","expectedLevel":"B1","sentenceLevel":"A1"}],
  "outOfCatalogWords":[]}` — replica exactamente el caso motivador (2026-07-22) que ningun eval detectaba.
  Sin commit.
