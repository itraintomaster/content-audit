<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Agent Rules: ContentAudit

**Version:** 0.0.1
**Architecture:** hexagonal
**Package:** com.learney.contentaudit

## Specialized Agents

This project has specialized AI agents. **Delegate to them instead of doing their work yourself.**

| Agent | Invoke | Purpose |
|-------|--------|---------|
| **Architect** | `@architect` | Design and evolve architecture. Proposes validated patches to `sentinel.yaml` via CLI. Only agent that can change the architecture. |
| **Developer** | `@developer` | Implement code within a module. Reads generated contracts, writes implementation code, respects module boundaries, and escalates missing contracts. |
| **QA Tester** | `@qa-tester` | Design test coverage for implementations. Analyzes contracts, dependencies, and requirements to propose test names with traceability. Tests are added as `handwrittenTests` in sentinel.yaml. |

**When to delegate:**
- Architecture changes (new module, new interface, change dependencies) → `@architect`
- Implementation work (write adapter, satisfy interface, pass tests) → `@developer`
- Test design (propose test names for an implementation) → `@qa-tester`
- You should NOT do architecture, implementation, or test design work yourself — use the agents.

## Rule 0 - Generated Files

Never modify files annotated with `@Generated` or containing the header `SENTINEL MANAGED FILE`.
These files are owned by Sentinel and will be overwritten on regeneration.

## Rule A - Source of Truth

Before writing any code, read `sentinel.yaml`. It defines the contracts, boundaries, and expected implementations.

**Never edit `sentinel.yaml` directly.** All architectural changes (adding modules, interfaces, models, implementations, changing dependencies) must go through the **architect agent** (`@architect`). The architect agent proposes changes as patches, validates them, and integrates them with the Architect Studio.

## Rule B - Sealed Interfaces

The following interfaces are `sealed`. Only the listed classes may implement them:

- `LemmaAbsenceConfig` permits: DefaultLemmaAbsenceConfig
- `AuditCli` permits: DefaultAuditCli
- `DrillDownResolver` permits: DefaultDrillDownResolver
- `AnalyzerStatsTransformer` permits: DefaultAnalyzerStatsTransformer

## Rule C - Dependency Injection

Never use `new` to instantiate dependencies. Use constructor injection as declared in `requiresInject`.

## Rule D - Framework Types

| Type | Spring Annotation |
|------|-------------------|
| Service | `@Service` |
| Repository | `@Repository` |
| Controller | `@Controller` |
| Component | `@Component` |

## Rule E - Architectural Patches

**NEVER** write directly to `.sentinel/proposals/` or create `architectural_patch.yaml` files manually.
**NEVER** run `sentinel patch apply` or any command that modifies `sentinel.yaml` — only the user applies patches via the Architect Studio or the CLI.

All architectural changes must go through the **architect agent** (`@architect`), which proposes validated patches using `sentinel patch propose`. The CLI validates the patch, merges it with any existing proposal, and writes it to `.sentinel/proposals/architectural_patch.yaml`.

## Rule W - Development Workflow

The correct development workflow follows these phases **in strict order**:

```
Requirement → Architecture → [accept & sentinel generate] → Test Design → [sentinel generate] → Development
```

### Phases

1. **Requirement** — Define or refine features, business rules, user journeys, and doubts in `REQUIREMENT.md`.
2. **Architecture** — Design the solution in `sentinel.yaml`: modules, models, interfaces, implementations, dependencies.
3. **Accept & Generate (Architecture)** — User accepts the architecture. Run `sentinel generate` to produce contracts and implementation stubs.
4. **Test Design** — Define test names under `handwrittenTests` in `sentinel.yaml`. Use `@qa-tester` to propose test coverage with traceability.
5. **Generate Test Stubs** — Run `sentinel generate` to produce JUnit stub classes.
6. **Development** — Implement the contracts and the test stubs. Only now should production code be written.

### Enforcement

If the user requests work that **skips a phase**, do NOT proceed silently. Instead:

1. Identify which phase the user is trying to jump to and which phase should come next.
2. Warn the user with a message like:

   > ⚠️ **Workflow checkpoint:** You are at the *[current phase]* but requesting to jump to *[requested phase]*. The recommended next step is [pending action]. What do you prefer?
   >
   > 1. **Follow the workflow** — [description of the pending action]
   > 2. **Skip anyway** — Jump directly to [requested phase] (at your own risk)
   > 3. **Something else** — Tell me what you have in mind

3. Wait for the user's explicit choice before proceeding.

## Module Map

### audit-domain

**Models:** AuditReport, AuditableCourse, AuditContext, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, ScoredItem, NodeScores, QuizNode, KnowledgeNode, TopicNode, MilestoneNode, NlpToken, AnalyzerDescriptor

**Interfaces:** ContentAudit, AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig, LemmaAbsenceConfig, EvpCatalogPort, AuditableEntity, SelfDescribingConfig

**Implementations:** IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer, IContentAudit, SentenceLengthAnalyzer, IScoreAggregator

### course-domain

Domain module for course structure. Contains entity models representing the 5-level hierarchy (Course > ROOT > Milestone > Topic > Knowledge > QuizTemplate), ports for persistence and validation, and domain exceptions. All models are Java records with defensive copying. This module has no infrastructure dependencies.

**Models:** NodeKind, SentencePartKind, CourseEntity, RootNodeEntity, MilestoneEntity, TopicEntity, KnowledgeEntity, QuizTemplateEntity, FormEntity, SentencePartEntity, CourseValidationException

**Interfaces:** CourseRepository, CourseValidator

### refiner-domain

### audit-application

**Depends on:** audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure

**Interfaces:** AuditRunner, CourseMapper, AnalyzerRegistry

**Implementations:** CourseToAuditableMapper, DefaultSentenceLengthConfig, DefaultAuditRunner, DefaultCocaBucketsConfig, DefaultLemmaRecurrenceConfig, DefaultLemmaAbsenceConfig, DefaultAnalyzerRegistry

### course-infrastructure

Infrastructure module for course persistence. Contains the filesystem adapter that reads/writes the hierarchical directory structure with MongoDB Extended JSON format. Handles directory traversal, JSON parsing/serialization, slug generation, and $oid/$numberDouble format preservation.

**Depends on:** course-domain

**Implementations:** FileSystemCourseRepository

### audit-cli

CLI entry point for running content audits from the command line

**Depends on:** audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure

**Models:** ReportViewModel, MilestoneScoreRow, QuizScoreRow, KnowledgeScoreRow, TopicScoreRow, DrillDownScope, DrillDownLevel, DrillDownView, ChildScoreRow, AnalyzerStatsView, ScoredItemRow

**Interfaces:** ReportFormatter, AuditCli, FormatterRegistry, ReportViewModelTransformer, RawReportFormatter, DrillDownResolver, AnalyzerStatsTransformer, ScoreRow

**Implementations:** TextReportFormatter, JsonReportFormatter, DefaultAuditCli, DefaultFormatterRegistry, DefaultReportViewModelTransformer, TableReportFormatter, RawJsonReportFormatter, DefaultDrillDownResolver, DefaultAnalyzerStatsTransformer

### nlp-infrastructure

Infrastructure module for NLP processing. Provides SpaCy-backed tokenization behind a factory, with internal caching. Only the factory and configuration model are public; all processing internals are package-private.

**Depends on:** audit-domain

**Models:** NlpTokenizerConfig

**Interfaces:** NlpTokenizerFactory

### vocabulary-infrastructure

Infrastructure module for linguistic reference catalogs (EVP vocabulary profiles, COCA frequency data). Provides static lookup data for vocabulary analysis. Separate from NLP processing (which handles runtime tokenization).

**Depends on:** audit-domain

## Boundaries

| Module | Can Access |
|--------|------------|
| audit-domain | (none) |
| course-domain | (none) |
| refiner-domain | (none) |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure |
| course-infrastructure | course-domain |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure |
| nlp-infrastructure | audit-domain |
| vocabulary-infrastructure | audit-domain |

