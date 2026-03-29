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
```

## Declared Modules

### audit-domain

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditdomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 17 (AuditReport, AuditableCourse, AuditContext, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, ScoredItem, NodeScores, QuizNode, KnowledgeNode, TopicNode, MilestoneNode, NlpToken) |
| Interfaces | 10 (ContentAudit, AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig) |
| Implementations | 6 (IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer, IContentAudit, SentenceLengthAnalyzer, IScoreAggregator) |
| Packages | 2 (coca [public], lrec [internal]) |

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

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.refinerdomain` |
| Depends On | (none — leaf module) |
| Allowed Clients | (unrestricted) |
| Scope | internal |
| Models | 0 |
| Interfaces | 0 |
| Implementations | 0 |
| Packages | 0 |

### audit-application

| Property | Value |
|----------|-------|
| Package | `com.learney.contentaudit.auditapplication` |
| Depends On | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 0 |
| Interfaces | 2 (AuditRunner, CourseMapper) |
| Implementations | 5 (CourseToAuditableMapper, DefaultSentenceLengthConfig, DefaultAuditRunner, DefaultCocaBucketsConfig, DefaultLemmaRecurrenceConfig) |
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
| Depends On | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure |
| Allowed Clients | (unrestricted) |
| Scope | public |
| Models | 5 (ReportViewModel, MilestoneScoreRow, QuizScoreRow, KnowledgeScoreRow, TopicScoreRow) |
| Interfaces | 5 (ReportFormatter, AuditCli, FormatterRegistry, ReportViewModelTransformer, RawReportFormatter) |
| Implementations | 7 (TextReportFormatter, JsonReportFormatter, DefaultAuditCli, DefaultFormatterRegistry, DefaultReportViewModelTransformer, TableReportFormatter, RawJsonReportFormatter) |
| Packages | 0 |

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

## Dependency Graph

```
audit-domain (leaf — no dependencies)
course-domain (leaf — no dependencies)
refiner-domain (leaf — no dependencies)
audit-application ──depends──> audit-domain
audit-application ──depends──> course-domain
audit-application ──depends──> refiner-domain
audit-application ──depends──> course-infrastructure
audit-application ──depends──> nlp-infrastructure
course-infrastructure ──depends──> course-domain
audit-cli ──depends──> audit-application
audit-cli ──depends──> audit-domain
audit-cli ──depends──> course-domain
audit-cli ──depends──> course-infrastructure
audit-cli ──depends──> nlp-infrastructure
nlp-infrastructure ──depends──> audit-domain
```

## Access Control Matrix

| Module | Can Import From | Who Can Import This |
|--------|----------------|--------------------|
| audit-domain | (none) | (any) |
| course-domain | (none) | (any) |
| refiner-domain | (none) | (any) |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure | (any) |
| course-infrastructure | course-domain | (any) |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure | (any) |
| nlp-infrastructure | audit-domain | (any) |

## Enforcement Mechanisms

1. **ArchUnit Rules** (`SentinelArchitectureTest.java`) — generated per module, checks `dependsOn` and `allowedClients` at test time
2. **JPMS `module-info.java`** — generated when `allowedClients` is set, enforces access at compile time
3. **Maven POM dependencies** — inter-module `<dependency>` tags generated from `dependsOn`
