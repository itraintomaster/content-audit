# Fix Log - FEAT-REVCTX

2026-05-09 — test-writer — Stubs de RevisionEngine.revise() actualizados de 3 a 4 args.
  why: el arquitecto cambio la firma de RevisionEngine.revise() a 4 args
  (agrego overridePayload: String) y los test bodies existentes usaban la
  firma vieja. Todos los callsites en DefaultRevisionEngineTest y
  ReviseCmdTest actualizados para pasar null como 4to arg cuando no hay override.

2026-05-09 — test-writer — buildEngine() actualizado para incluir correctionContextOverrideParser.
  why: DefaultRevisionEngine constructor ahora recibe 11 args; el helper
  buildEngine() tenia 10. Agregado @Mock CorrectionContextOverrideParser.

2026-05-09 — test-writer — Fix UnnecessaryStubbing en R003 (OVERRIDE_INVALID).
  why: sin stub de contextResolver.supports(LEMMA_ABSENCE)=true, el engine
  detecta kind sin contrato y retorna OVERRIDE_NOT_APPLICABLE antes de llamar
  al parser. El test R003 necesita que el kind tenga contrato (supports=true)
  para que el rechazo venga del parser (OVERRIDE_INVALID).

2026-05-09 — test-writer — Fix pattern: todos los tests que usan override y
  necesitan llegar a la estrategia deben stubear contextResolver.supports(kind)=true.
  why: el engine llama supports() para decidir si el kind tiene contrato de
  contexto ANTES de llamar al parser. Si false → OVERRIDE_NOT_APPLICABLE temprana.
  R002-a, R002-b, R005, R006, R007-OVERRIDE afectados.

2026-05-09 — test-writer — Removidos stubs innecesarios de auditReportStore.load()
  en tests del override path.
  why: el engine no llama auditReportStore.load() cuando hay overridePayload
  (R002: zero derivation). Mockito strict mode falla con UnnecessaryStubbing.

2026-05-09 — developer — RevisionArtifact constructor 6→8 arg migration
  why: arch added contextSource + contextOverridePayload fields; all test
  fixtures using the old 6-arg ctor needed null, null appended. Files:
  DefaultConsolidatedViewBuilderTest, DefaultProposalDecisionServiceTest,
  DefaultRevisionEngineFactoryTest, FCdiffJ002-J003-J004-J005-J008,
  FPipreJ001JourneyTest, FRevbypJ001JourneyTest, audit-infrastructure
  FileSystemRevisionArtifactStoreTest.

2026-05-09 — developer — DefaultRevisionEngine ctor missing parser arg
  why: engine got correctionContextOverrideParser as new required inject;
  tests that constructed DefaultRevisionEngine directly needed the new arg.
  Files: FPipreJ001JourneyTest, FRevbypJ001JourneyTest.

2026-05-09 — developer — revise(planId, taskId, coursePath) → 4-arg
  why: RevisionEngine interface replaced the 3-arg method with a 4-arg one;
  tests that called engine.revise directly needed null as 4th arg.
  Files: FPipreJ001JourneyTest, FRevbypJ001JourneyTest.

2026-05-09 — developer — DefaultCorrectionContextOverrideParserTest accessor fix
  why: @test-writer used result.context() and result.rawPayload() (record
  accessors) but CorrectionContextOverride is a regular class with getters.
  Sentinel generated type:record as a JavaBean not a Java record. Fixed to
  getContext()/getRawPayload()/getSentence().

2026-05-09 — developer — validator structural validation thresholds
  why: tests expect OverrideRejectedException for incomplete payloads; added
  suggestedLemmas + misplacedLemmas as required fields for LEMMA_ABSENCE;
  tokenCount must be numeric for SENTENCE_LENGTH.

2026-05-09 — developer — auditReportStore.load call placement
  why: @test-writer removed auditReportStore.load stubs from override path
  tests (expecting zero derivation). But some override path tests (R003, R004,
  OVERRIDE_NOT_APPLICABLE) DO stub auditReportStore.load. Engine loads eagerly
  before the supports/parse checks; for tests without the stub, Mockito returns
  Optional.empty() (smart default for Optional), which is fine since we don't
  use the report in the override path. All 165 revision-domain tests pass.
