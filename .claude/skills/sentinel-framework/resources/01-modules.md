# Modules

Modules are the primary architectural boundaries in a Sentinel project. Each module maps to a
Maven module with its own `pom.xml`, source directories, and package namespace.

## Concepts

- **Module** = Maven module + Java package boundary (`com.learney.contentaudit.<module-name>`)
- **dependsOn** = Declares which other modules this module can import from (enforced by ArchUnit)
- **allowedClients** = Restricts which modules can depend on this module (enforced by ArchUnit + JPMS)
- **scope** = `public` (no restrictions) or `internal` (protected by JPMS if `allowedClients` set)
- Circular dependencies are **forbidden** — the build will fail

## Module Directory Structure

```
project-root/
├── sentinel.yaml          # Source of truth
├── pom.xml                # Root POM (generated)
├── audit-domain/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── course-domain/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── refiner-domain/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── audit-application/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── course-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── audit-cli/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── nlp-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── vocabulary-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── audit-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── revision-domain/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── revision-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── evaluation-ledger-domain/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── evaluation-ledger-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── agent-runtime-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
├── quiz-instruction-infrastructure/
│   ├── pom.xml            # Module POM (generated)
│   ├── src/main/java/     # Production code
│   └── src/test/java/     # Test code
```

## Declared Modules

### audit-domain

> Core business logic

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditdomain` |
| Depends On | course-domain, evaluation-ledger-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 16 (AuditReport, AuditableCourse, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, NlpToken, AnalyzerDescriptor, AuditNode, SentenceLengthDiagnosis, AuditReportSummary, ActiveAnalysisSelection, EvaluationRunPolicy) |
| Interfaces | 28 (AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig, LemmaAbsenceConfig, EvpCatalogPort, AuditableEntity, SelfDescribingConfig, NodeDiagnoses, CourseDiagnoses, LevelDiagnoses, TopicDiagnoses, KnowledgeDiagnoses, QuizDiagnoses, AuditReportStore, CourseMapper, ActiveAnalysisSelectionStore, AuditNodeIndex, AuditNodeIndexFactory, LemmaCountConfig, EvaluationAnalyzerFactory, QuizInstructionVerdictReader, QuizInstructionConfig) |
| Implementations | 5 (IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer, SentenceLengthAnalyzer, IScoreAggregator) |
| Packages | 8 (coca [internal], lrec [internal], labs [internal], auditnodeindex [internal], lemmacount [internal], lexicalflags [public], quizinstruction [public], quizinstructionengine [public]) |

### course-domain

> Domain module for course structure. Contains entity models representing the 5-level hierarchy (Course > ROOT > Milestone > Topic > Knowledge > QuizTemplate), ports for persistence and validation, and domain exceptions. All models are Java records with defensive copying. This module has no infrastructure dependencies.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.coursedomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 12 (NodeKind, SentencePartKind, CourseEntity, RootNodeEntity, MilestoneEntity, TopicEntity, KnowledgeEntity, QuizTemplateEntity, FormEntity, SentencePartEntity, CourseValidationException, SentenceMode) |
| Interfaces | 2 (CourseRepository, CourseValidator) |
| Implementations | 0 |
| Packages | 2 (quizsentence [public], quizsentenceengine [internal]) |

### refiner-domain

> Refinement engine

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.refinerdomain` |
| Depends On | audit-domain, course-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 15 (DiagnosisKind, RefinementTaskStatus, RefinementTask, RefinementPlan, SuggestedLemma, SentenceLengthCorrectionContext, MisplacedLemmaContext, LemmaAbsenceCorrectionContext, LengthDirection, SuggestedLemmaQueryResult, SuggestedLemmaQueryRejectedException, SuggestedLemmaQueryCriteria, ScarceContentWord, KnowledgeTitleCorrectionContext, OutOfCatalogWordContext) |
| Interfaces | 7 (RefinerEngine, RefinementPlanStore, CorrectionContextResolver, CorrectionContext, SuggestedLemmaQueryPort, SuggestedLemmaQuerySession, SuggestedLemmaQuerySessionFactory) |
| Implementations | 5 (SentenceLengthContextResolver, LemmaAbsenceContextResolver, DispatchingCorrectionContextResolver, DefaultRefinerEngine, KnowledgeTitleContextResolver) |
| Packages | 1 (lemmasuggestion [internal]) |

### audit-application

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditapplication` |
| Depends On | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain, evaluation-ledger-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 1 (AuditRunRequest) |
| Interfaces | 3 (AuditRunner, AnalyzerRegistry, LemmaCountConfigLoader) |
| Implementations | 10 (CourseToAuditableMapper, DefaultSentenceLengthConfig, DefaultAuditRunner, DefaultCocaBucketsConfig, DefaultLemmaRecurrenceConfig, DefaultLemmaAbsenceConfig, DefaultAnalyzerRegistry, DefaultLemmaCountConfig, DefaultLemmaCountConfigLoader, DefaultQuizInstructionConfig) |
| Packages | 0 |

### course-infrastructure

> Infrastructure module for course persistence. Contains the filesystem adapter that reads/writes the hierarchical directory structure with MongoDB Extended JSON format. Handles directory traversal, JSON parsing/serialization, slug generation, and $oid/$numberDouble format preservation.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.courseinfrastructure` |
| Depends On | course-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 1 (FileSystemCourseRepository) |
| Packages | 0 |

### audit-cli

> CLI entry point

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditcli` |
| Depends On | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure, evaluation-ledger-domain, evaluation-ledger-infrastructure, agent-runtime-infrastructure, quiz-instruction-infrastructure |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 6 (GetTasksFilter, LagenMode, PlanStorageMode, EphemeralRenderOptions, SuggestedLemmasFilter, AnalyzeOptions) |
| Interfaces | 14 (AnalyzeCommand, GetCommand, DeleteCommand, PruneCommand, PlanCommand, ReviseCommand, ConfigAnalyzerCommand, StatsAnalyzerCommand, ApproveCommand, RejectCommand, GetConsolidatedCommand, SetActiveAnalysisCommand, LexisCommand, RepairCommand) |
| Implementations | 0 |
| Packages | 3 (commands [internal], formatting [internal], bootstrap [internal]) |

### nlp-infrastructure

> Infrastructure module for NLP processing. Provides SpaCy-backed tokenization behind a factory, with internal caching. Only the factory and configuration model are public; all processing internals are package-private.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.nlpinfrastructure` |
| Depends On | audit-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 1 (NlpTokenizerConfig) |
| Interfaces | 1 (NlpTokenizerFactory) |
| Implementations | 0 |
| Packages | 1 (spacy [public]) |

### vocabulary-infrastructure

> Infrastructure module for linguistic reference catalogs (EVP vocabulary profiles, COCA frequency data). Provides static lookup data for vocabulary analysis. Separate from NLP processing (which handles runtime tokenization).

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.vocabularyinfrastructure` |
| Depends On | audit-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 0 |
| Packages | 2 (evp [internal], coca [internal]) |

### audit-infrastructure

> Filesystem persistence adapters for audit reports, refinement plans, revision artifacts, impact previews and the active analysis selection. Hosts the five adapters that the CLI composition root wires into the corresponding ports (AuditReportStore, RefinementPlanStore, RevisionArtifactStore, ImpactPreviewStore, ActiveAnalysisSelectionStore). The active-analysis-selection adapter writes a single dotfile under .content-audit/ so cualquier consumidor del CLI lo lee con la misma simetria que los demas stores filesystem.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditinfrastructure` |
| Depends On | audit-domain, refiner-domain, revision-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 5 (FileSystemAuditReportStore, FileSystemRefinementPlanStore, FileSystemRevisionArtifactStore, FileSystemImpactPreviewStore, FileSystemActiveAnalysisSelectionStore) |
| Packages | 0 |

### revision-domain

> Domain module for the revision phase of the refinement pipeline. Consumes refiner-domain (task and CorrectionContext), course-domain (course entities and the CourseRepository port owned by the caller) and audit-domain (AuditReport / AuditEngine / ActiveAnalysisSelectionStore for the eager what-if simulation that powers the impact preview and the consolidated view). Exposes Reviser/RevisionValidator/RevisionArtifactStore/CourseElementLocator/ImpactPreviewStore ports plus the RevisionEngineFactory seam, the lemmaabsence proposal-strategy SPI, the impactpreview SPI (FEAT-PIPRE) and the consolidatedview SPI (FEAT-CDIFF) — including the dynamic field-diff engine that powers FEAT-CDIFF v3 (recursive walker + central role-based exclusion registry + central list-identity registry, all inside the fielddiff package). The bypass baseline, the impact-preview internals, the consolidated-view internals and the field-diff internals live behind their respective internal packages; external modules only see factories, ports and carrier records.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.revisiondomain` |
| Depends On | audit-domain, refiner-domain, course-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 18 (RevisionVerdict, RevisionOutcomeKind, CourseElementSnapshot, RevisionProposal, RevisionArtifact, RevisionOutcome, RevisionEngineConfig, ApprovalMode, ProposalDecisionOutcomeKind, ProposalDecisionOutcome, StrategyId, LemmaAbsenceQuizCandidate, ProposalStrategyFailedException, ProposalDerivationException, ConsolidatedViewBuilderConfig, CorrectionContextSource, CorrectionContextOverride, OverrideRejectedException) |
| Interfaces | 19 (Reviser, RevisionValidator, RevisionValidatorResult, RevisionArtifactStore, CourseElementLocator, RevisionEngine, RevisionEngineFactory, RevisionValidatorFactory, ProposalDecisionService, ProposalDecisionServiceFactory, LemmaAbsenceProposalStrategy, LemmaAbsenceProposalStrategyRegistry, LemmaAbsenceProposalDeriver, ImpactPreviewStore, CorrectionContextOverrideParser, KnowledgeTitleProposalStrategy, KnowledgeTitleProposalStrategyRegistry, KnowledgeTitleProposalDeriver, PreservationFactory) |
| Implementations | 0 |
| Packages | 9 (engine [internal], lemmaabsence [public], impactpreview [public], consolidatedview [public], fielddiff [internal], contextoverride [internal], knowledgetitle [public], preservation [public], preservationengine [internal]) |

### revision-infrastructure

> Infrastructure adapter for the revision phase. Provides the LLM-backed implementation of LemmaAbsenceQuizCandidateGenerator (revision-domain.lemmaabsence port) using LangChain4j against any OpenAI-compatible HTTP endpoint (LM Studio, vLLM, OpenAI cloud, Ollama via openai compat, etc.). Exposes a single Factory Seam (LemmaAbsenceLlmGeneratorFactory + LagenConfig carrier + LlmGenerationFailureCategory enum) so the composition root wires the adapter with one call. The factory is the only piece that consumes LagenConfig: it reads the knobs, builds a ChatLanguageModel via the LangChain4j OpenAI builder, and passes that model directly into LemmaAbsenceLlmGenerator. The generator therefore depends on a chat-model abstraction (not on the LagenConfig record), keeping the inner adapter agnostic of how the model was configured. The adapter uses LangChain4j's legacy chat-message API flavor, generate(List<ChatMessage>), because F-LAGEN needs explicit system/user messages and a simple text response without exposing ChatRequest/ChatResponse in the architectural surface. The adapter, prompt builder, response parser and error classifier all live in an internal package; only the factory class is public. allowedClients=[audit-cli] enforces that this module is an implementation detail of the CLI composition root only (P8 Qualified Export).

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.revisioninfrastructure` |
| Depends On | revision-domain, refiner-domain, agent-runtime-infrastructure |
| Allowed Clients | audit-cli |
| Scope | public |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 0 |
| Packages | 3 (lagen [public], lemmaabsenceagent [internal], knowledgetitleagent [public]) |

### evaluation-ledger-domain

> FEAT-EVCOST. Capacidad general de resultados de evaluacion costosa reutilizables entre analisis. No sabe nada de auditoria, de cursos ni de LLMs: define la identidad de un resultado —evaluador + huella del contenido, y nada mas (F-EVCOST-R001)—, el registro append-only clave->payload opaco, y la coordinacion reanudable de una corrida (consultar lo registrado -> si falta, evaluar dentro del presupuesto -> registrar apenas se obtiene). La version del evaluador NO integra la identidad: es procedencia obligatoria que se registra junto a cada resultado y viaja con el en cada consulta, de modo que ningun cambio de version invalida, borra ni deja pendiente nada (F-EVCOST-R006, F-EVCOST-R008). Cada consumidor aporta tres cosas y nada mas: como construye el contenido (EvaluationSubject.content), como su evaluador se pronuncia (Evaluator -> EvaluationOutcome) y como interpreta su payload. La huella NO la calcula el consumidor: la deriva la sesion del propio mapa de contenido entregado al evaluador, de modo que F-EVCOST-R002 ("exactamente lo que el evaluador recibe, ni mas ni menos") queda garantizada por construccion y no por disciplina.


| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.evaluationledgerdomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 10 (EvaluationKey, EvaluationSubject, EvaluationRecord, EvaluationEmitted, EvaluationNotEmitted, EvaluationFailureKind, EvaluationResolutionKind, EvaluationResolution, EvaluationCoverage, EvaluationBudget) |
| Interfaces | 6 (EvaluationOutcome, EvaluationLedger, ContentFingerprinter, Evaluator, EvaluationSession, EvaluationSessionFactory) |
| Implementations | 0 |
| Packages | 2 (evaluationsession [public], contentfingerprint [public]) |

### evaluation-ledger-infrastructure

> Adaptador filesystem del registro de evaluaciones costosas. Escribe un archivo por evaluador bajo .content-audit/evaluations/, una entrada por linea, en modo append: cada resultado queda disponible apenas se obtiene (F-EVCOST-R004) y una interrupcion abrupta a lo sumo deja una ultima linea incompleta, que la lectura descarta por no parsear —"se lee completa o no existe"—. El orden de append es la unica fuente del desempate que exige F-EVCOST-R006: con mas de una entrada para la misma clave (evaluador + huella), findLatest devuelve la ULTIMA escrita y history las devuelve todas, de la mas reciente a la mas antigua, cada una con la version que la produjo. Se desempata por posicion en el archivo y no por recordedAt a proposito: el append es monotono y es la misma propiedad sobre la que se apoya la seguridad ante dos corridas simultaneas, mientras que un reloj puede empatar o retroceder; recordedAt queda como dato informativo. Append-only tambien significa que las entradas de versiones anteriores del evaluador conviven como historia consultable y nunca se borran solas (F-EVCOST-R006), y que una re-evaluacion agrega sin destruir y pasa a ganar el desempate (F-EVCOST-R008). Vive aparte de los informes de auditoria a proposito: borrar informes no toca este registro (F-EVCOST-R007).


| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.evaluationledgerinfrastructure` |
| Depends On | evaluation-ledger-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 1 (FileSystemEvaluationLedger) |
| Packages | 0 |

### agent-runtime-infrastructure

> Capacidad compartida para correr un agente declarativo de sentinel-agents in-process: carga el grafo de <agentsBaseDir>/<nombre>/, lo ejecuta con AgentRun.runRecording y persiste el run. Existe porque este lanzador ya se escribio dos veces identico dentro de revision-infrastructure (lemmaabsenceagent y knowledgetitleagent) y el juez de consigna seria la tercera copia — y no puede reusar ninguna, porque revision-infrastructure declara allowedClients=[audit-cli] y cuelga de revision-domain, mientras que este consumidor nuevo cuelga de la auditoria. Deliberadamente NO incluye la clasificacion de errores: el clasificador de cada consumidor devuelve su propia taxonomia de fallas y no es compartible sin arrastrar tipos ajenos. Declarado de forma identica en el patch principal a proposito, para que este patch valide y aplique en cualquier orden.


| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.agentruntimeinfrastructure` |
| Depends On | (none — leaf module) |
| Allowed Clients | quiz-instruction-infrastructure, audit-cli, revision-infrastructure |
| Scope | public |
| Models | 3 (AgentGraphRunnerConfig, AgentDefinitionFound, AgentDefinitionUnresolved) |
| Interfaces | 5 (AgentGraphRunner, AgentGraphRunnerFactory, AgentDefinitionLocation, AgentDefinitionLocator, AgentDefinitionLocatorFactory) |
| Implementations | 0 |
| Packages | 1 (graphexecution [public]) |

### quiz-instruction-infrastructure

> Adaptador del juez de consigna: implementa el SPI Evaluator de evaluation-ledger-domain corriendo el agente declarativo quiz-instruction-validator sobre el runtime compartido. Traduce EvaluationSubject.content a las cinco entradas declaradas del agente (cefrLevel, topic, title, instructions, quiz) verbatim, sin agregar ni quitar nada — es lo que hace que la huella cubra exactamente lo juzgado (F-EVCOST-R002). Reconoce el veredicto conservador de infraestructura del juez (confianza nula y violacion JUDGE_OUTPUT_INVALID) y lo devuelve como EvaluationNotEmitted(OUTPUT_UNUSABLE), de modo que nunca llega al registro (F-QINST-R013); toda falla de servicio, timeout, autenticacion o credito se devuelve como EvaluationNotEmitted(EVALUATOR_UNAVAILABLE) en lugar de propagarse como excepcion (F-QINST-R007). Su version es procedencia, no vigencia: se deriva de la definicion del agente y del modelo, se estampa sobre cada veredicto nuevo y se declara en el informe, pero no decide que veredictos valen (F-QINST-R010). Cuando la definicion no se puede localizar el juez no enuncia version alguna —evaluatorVersion() devuelve Optional.empty()— y con eso queda tratado como no disponible por la propia sesion. Construye el ChatModel de forma perezosa: una configuracion ausente o invalida es un evaluador no disponible, no una auditoria caida. allowedClients=[audit-cli] porque es un detalle de implementacion de la composition root.


| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.quizinstructioninfrastructure` |
| Depends On | audit-domain, evaluation-ledger-domain, agent-runtime-infrastructure |
| Allowed Clients | audit-cli |
| Scope | public |
| Models | 1 (QuizInstructionJudgeConfig) |
| Interfaces | 1 (QuizInstructionJudgeFactory) |
| Implementations | 0 |
| Packages | 2 (instructionjudge [public], instructionverdict [public]) |

## Dependency Graph

```
audit-domain ──depends──> course-domain
audit-domain ──depends──> evaluation-ledger-domain
course-domain (leaf — no dependencies)
refiner-domain ──depends──> audit-domain
refiner-domain ──depends──> course-domain
audit-application ──depends──> audit-domain
audit-application ──depends──> course-domain
audit-application ──depends──> refiner-domain
audit-application ──depends──> course-infrastructure
audit-application ──depends──> nlp-infrastructure
audit-application ──depends──> vocabulary-infrastructure
audit-application ──depends──> audit-infrastructure
audit-application ──depends──> revision-domain
audit-application ──depends──> evaluation-ledger-domain
course-infrastructure ──depends──> course-domain
audit-cli ──depends──> audit-application
audit-cli ──depends──> audit-domain
audit-cli ──depends──> course-domain
audit-cli ──depends──> course-infrastructure
audit-cli ──depends──> nlp-infrastructure
audit-cli ──depends──> vocabulary-infrastructure
audit-cli ──depends──> audit-infrastructure
audit-cli ──depends──> refiner-domain
audit-cli ──depends──> revision-domain
audit-cli ──depends──> revision-infrastructure
audit-cli ──depends──> evaluation-ledger-domain
audit-cli ──depends──> evaluation-ledger-infrastructure
audit-cli ──depends──> agent-runtime-infrastructure
audit-cli ──depends──> quiz-instruction-infrastructure
nlp-infrastructure ──depends──> audit-domain
vocabulary-infrastructure ──depends──> audit-domain
audit-infrastructure ──depends──> audit-domain
audit-infrastructure ──depends──> refiner-domain
audit-infrastructure ──depends──> revision-domain
revision-domain ──depends──> audit-domain
revision-domain ──depends──> refiner-domain
revision-domain ──depends──> course-domain
revision-infrastructure ──depends──> revision-domain
revision-infrastructure ──depends──> refiner-domain
revision-infrastructure ──depends──> agent-runtime-infrastructure
evaluation-ledger-domain (leaf — no dependencies)
evaluation-ledger-infrastructure ──depends──> evaluation-ledger-domain
agent-runtime-infrastructure (leaf — no dependencies)
quiz-instruction-infrastructure ──depends──> audit-domain
quiz-instruction-infrastructure ──depends──> evaluation-ledger-domain
quiz-instruction-infrastructure ──depends──> agent-runtime-infrastructure
```

## Access Control Matrix

| Module | Can Import From | Who Can Import This |
|--------|----------------|--------------------|
| audit-domain | course-domain, evaluation-ledger-domain | (any) |
| course-domain | (none) | (any) |
| refiner-domain | audit-domain, course-domain | (any) |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain, evaluation-ledger-domain | (any) |
| course-infrastructure | course-domain | (any) |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure, evaluation-ledger-domain, evaluation-ledger-infrastructure, agent-runtime-infrastructure, quiz-instruction-infrastructure | (any) |
| nlp-infrastructure | audit-domain | (any) |
| vocabulary-infrastructure | audit-domain | (any) |
| audit-infrastructure | audit-domain, refiner-domain, revision-domain | (any) |
| revision-domain | audit-domain, refiner-domain, course-domain | (any) |
| revision-infrastructure | revision-domain, refiner-domain, agent-runtime-infrastructure | audit-cli |
| evaluation-ledger-domain | (none) | (any) |
| evaluation-ledger-infrastructure | evaluation-ledger-domain | (any) |
| agent-runtime-infrastructure | (none) | quiz-instruction-infrastructure, audit-cli, revision-infrastructure |
| quiz-instruction-infrastructure | audit-domain, evaluation-ledger-domain, agent-runtime-infrastructure | audit-cli |

## Enforcement Mechanisms

1. **ArchUnit Rules** (`SentinelArchitectureTest.java`) — generated per module, checks `dependsOn` and `allowedClients` at test time
2. **JPMS `module-info.java`** — generated when `allowedClients` is set, enforces access at compile time
3. **Maven POM dependencies** — inter-module `<dependency>` tags generated from `dependsOn`
