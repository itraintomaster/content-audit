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
```

## Declared Modules

### audit-domain

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditdomain` |
| Depends On | course-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 15 (AuditReport, AuditableCourse, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, NlpToken, AnalyzerDescriptor, AuditNode, SentenceLengthDiagnosis, AuditReportSummary, ActiveAnalysisSelection) |
| Interfaces | 22 (AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig, LemmaAbsenceConfig, EvpCatalogPort, AuditableEntity, SelfDescribingConfig, NodeDiagnoses, CourseDiagnoses, LevelDiagnoses, TopicDiagnoses, KnowledgeDiagnoses, QuizDiagnoses, AuditReportStore, CourseMapper, ActiveAnalysisSelectionStore) |
| Implementations | 5 (IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer, SentenceLengthAnalyzer, IScoreAggregator) |
| Packages | 3 (coca [internal], lrec [internal], labs [internal]) |

### course-domain

> Domain module for course structure. Contains entity models representing the 5-level hierarchy (Course > ROOT > Milestone > Topic > Knowledge > QuizTemplate), ports for persistence and validation, and domain exceptions. All models are Java records with defensive copying. This module has no infrastructure dependencies.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.coursedomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 11 (NodeKind, SentencePartKind, CourseEntity, RootNodeEntity, MilestoneEntity, TopicEntity, KnowledgeEntity, QuizTemplateEntity, FormEntity, SentencePartEntity, CourseValidationException) |
| Interfaces | 2 (CourseRepository, CourseValidator) |
| Implementations | 0 |
| Packages | 2 (quizsentence [public], quizsentenceengine [internal]) |

### refiner-domain

> Domain module for the refinement workflow. Defines the plan/task model and ports for generating and persisting refinement plans derived from audit reports.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.refinerdomain` |
| Depends On | audit-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 9 (DiagnosisKind, RefinementTaskStatus, RefinementTask, RefinementPlan, SuggestedLemma, SentenceLengthCorrectionContext, MisplacedLemmaContext, LemmaAbsenceCorrectionContext, LengthDirection) |
| Interfaces | 4 (RefinerEngine, RefinementPlanStore, CorrectionContextResolver, CorrectionContext) |
| Implementations | 4 (SentenceLengthContextResolver, LemmaAbsenceContextResolver, DispatchingCorrectionContextResolver, DefaultRefinerEngine) |
| Packages | 0 |

### audit-application

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditapplication` |
| Depends On | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 2 (AuditRunner, AnalyzerRegistry) |
| Implementations | 7 (CourseToAuditableMapper, DefaultSentenceLengthConfig, DefaultAuditRunner, DefaultCocaBucketsConfig, DefaultLemmaRecurrenceConfig, DefaultLemmaAbsenceConfig, DefaultAnalyzerRegistry) |
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

> CLI entry point for running content audits from the command line

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditcli` |
| Depends On | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 3 (GetTasksFilter, LagenMode, PlanStorageMode) |
| Interfaces | 12 (AnalyzeCommand, GetCommand, DeleteCommand, PruneCommand, PlanCommand, ReviseCommand, ConfigAnalyzerCommand, StatsAnalyzerCommand, ApproveCommand, RejectCommand, GetConsolidatedCommand, SetActiveAnalysisCommand) |
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
| Models | 15 (RevisionVerdict, RevisionOutcomeKind, CourseElementSnapshot, RevisionProposal, RevisionArtifact, RevisionOutcome, RevisionEngineConfig, ApprovalMode, ProposalDecisionOutcomeKind, ProposalDecisionOutcome, StrategyId, LemmaAbsenceQuizCandidate, ProposalStrategyFailedException, ProposalDerivationException, ConsolidatedViewBuilderConfig) |
| Interfaces | 14 (Reviser, RevisionValidator, RevisionValidatorResult, RevisionArtifactStore, CourseElementLocator, RevisionEngine, RevisionEngineFactory, RevisionValidatorFactory, ProposalDecisionService, ProposalDecisionServiceFactory, LemmaAbsenceProposalStrategy, LemmaAbsenceProposalStrategyRegistry, LemmaAbsenceProposalDeriver, ImpactPreviewStore) |
| Implementations | 0 |
| Packages | 5 (engine [internal], lemmaabsence [public], impactpreview [public], consolidatedview [public], fielddiff [internal]) |

### revision-infrastructure

> Infrastructure adapter for the revision phase. Provides the LLM-backed implementation of LemmaAbsenceQuizCandidateGenerator (revision-domain.lemmaabsence port) using LangChain4j against any OpenAI-compatible HTTP endpoint (LM Studio, vLLM, OpenAI cloud, Ollama via openai compat, etc.). Exposes a single Factory Seam (LemmaAbsenceLlmGeneratorFactory + LagenConfig carrier + LlmGenerationFailureCategory enum) so the composition root wires the adapter with one call. The factory is the only piece that consumes LagenConfig: it reads the knobs, builds a ChatLanguageModel via the LangChain4j OpenAI builder, and passes that model directly into LemmaAbsenceLlmGenerator. The generator therefore depends on a chat-model abstraction (not on the LagenConfig record), keeping the inner adapter agnostic of how the model was configured. The adapter uses LangChain4j's legacy chat-message API flavor, generate(List<ChatMessage>), because F-LAGEN needs explicit system/user messages and a simple text response without exposing ChatRequest/ChatResponse in the architectural surface. The adapter, prompt builder, response parser and error classifier all live in an internal package; only the factory class is public. allowedClients=[audit-cli] enforces that this module is an implementation detail of the CLI composition root only (P8 Qualified Export).

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.revisioninfrastructure` |
| Depends On | revision-domain, refiner-domain |
| Allowed Clients | audit-cli |
| Scope | public |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 0 |
| Packages | 2 (lagen [public], lagenopenai [internal]) |

## Dependency Graph

```
audit-domain ──depends──> course-domain
course-domain (leaf — no dependencies)
refiner-domain ──depends──> audit-domain
audit-application ──depends──> audit-domain
audit-application ──depends──> course-domain
audit-application ──depends──> refiner-domain
audit-application ──depends──> course-infrastructure
audit-application ──depends──> nlp-infrastructure
audit-application ──depends──> vocabulary-infrastructure
audit-application ──depends──> audit-infrastructure
audit-application ──depends──> revision-domain
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
```

## Access Control Matrix

| Module | Can Import From | Who Can Import This |
|--------|----------------|--------------------|
| audit-domain | course-domain | (any) |
| course-domain | (none) | (any) |
| refiner-domain | audit-domain | (any) |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain | (any) |
| course-infrastructure | course-domain | (any) |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure | (any) |
| nlp-infrastructure | audit-domain | (any) |
| vocabulary-infrastructure | audit-domain | (any) |
| audit-infrastructure | audit-domain, refiner-domain, revision-domain | (any) |
| revision-domain | audit-domain, refiner-domain, course-domain | (any) |
| revision-infrastructure | revision-domain, refiner-domain | audit-cli |

## Enforcement Mechanisms

1. **ArchUnit Rules** (`SentinelArchitectureTest.java`) — generated per module, checks `dependsOn` and `allowedClients` at test time
2. **JPMS `module-info.java`** — generated when `allowedClients` is set, enforces access at compile time
3. **Maven POM dependencies** — inter-module `<dependency>` tags generated from `dependsOn`
