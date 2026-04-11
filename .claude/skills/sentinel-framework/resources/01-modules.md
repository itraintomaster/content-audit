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
```

## Declared Modules

### audit-domain

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditdomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 14 (AuditReport, AuditableCourse, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, NlpToken, AnalyzerDescriptor, AuditNode, SentenceLengthDiagnosis, AuditReportSummary) |
| Interfaces | 20 (AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig, LemmaAbsenceConfig, EvpCatalogPort, AuditableEntity, SelfDescribingConfig, NodeDiagnoses, CourseDiagnoses, LevelDiagnoses, TopicDiagnoses, KnowledgeDiagnoses, QuizDiagnoses, AuditReportStore) |
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
| Packages | 0 |

### refiner-domain

> Domain module for the refinement workflow. Defines the plan/task model and ports for generating and persisting refinement plans derived from audit reports.

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.refinerdomain` |
| Depends On | audit-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 6 (DiagnosisKind, RefinementTaskStatus, RefinementTask, RefinementPlan, SuggestedLemma, SentenceLengthCorrectionContext) |
| Interfaces | 3 (RefinerEngine, RefinementPlanStore, CorrectionContextResolver) |
| Implementations | 1 (DefaultCorrectionContextResolver) |
| Packages | 0 |

### audit-application

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditapplication` |
| Depends On | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 3 (AuditRunner, CourseMapper, AnalyzerRegistry) |
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
| Depends On | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 7 (AnalyzeCommand, AnalyzerListCommand, AnalyzerConfigCommand, AnalyzerStatsCommand, RefinerPlanCommand, RefinerNextCommand, RefinerListCommand) |
| Implementations | 0 |
| Packages | 2 (commands [internal], formatting [internal]) |

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

> Filesystem persistence adapters for audit reports

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditinfrastructure` |
| Depends On | audit-domain, refiner-domain |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 2 (FileSystemAuditReportStore, FileSystemRefinementPlanStore) |
| Packages | 0 |

## Dependency Graph

```
audit-domain (leaf — no dependencies)
course-domain (leaf — no dependencies)
refiner-domain ──depends──> audit-domain
audit-application ──depends──> audit-domain
audit-application ──depends──> course-domain
audit-application ──depends──> refiner-domain
audit-application ──depends──> course-infrastructure
audit-application ──depends──> nlp-infrastructure
audit-application ──depends──> vocabulary-infrastructure
audit-application ──depends──> audit-infrastructure
course-infrastructure ──depends──> course-domain
audit-cli ──depends──> audit-application
audit-cli ──depends──> audit-domain
audit-cli ──depends──> course-domain
audit-cli ──depends──> course-infrastructure
audit-cli ──depends──> nlp-infrastructure
audit-cli ──depends──> vocabulary-infrastructure
audit-cli ──depends──> audit-infrastructure
audit-cli ──depends──> refiner-domain
nlp-infrastructure ──depends──> audit-domain
vocabulary-infrastructure ──depends──> audit-domain
audit-infrastructure ──depends──> audit-domain
audit-infrastructure ──depends──> refiner-domain
```

## Access Control Matrix

| Module | Can Import From | Who Can Import This |
|--------|----------------|--------------------|
| audit-domain | (none) | (any) |
| course-domain | (none) | (any) |
| refiner-domain | audit-domain | (any) |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure | (any) |
| course-infrastructure | course-domain | (any) |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain | (any) |
| nlp-infrastructure | audit-domain | (any) |
| vocabulary-infrastructure | audit-domain | (any) |
| audit-infrastructure | audit-domain, refiner-domain | (any) |

## Enforcement Mechanisms

1. **ArchUnit Rules** (`SentinelArchitectureTest.java`) — generated per module, checks `dependsOn` and `allowedClients` at test time
2. **JPMS `module-info.java`** — generated when `allowedClients` is set, enforces access at compile time
3. **Maven POM dependencies** — inter-module `<dependency>` tags generated from `dependsOn`
