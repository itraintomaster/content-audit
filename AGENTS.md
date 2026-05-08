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
| **Test Writer** | `@test-writer` | Implement handwritten test stubs. Reads requirements, rules, and journeys referenced by @Tag annotations to produce requirement-faithful test code. Works on one test at a time. |

**When to delegate:**
- Architecture changes (new module, new interface, change dependencies) → `@architect`
- Implementation work (write adapter, satisfy interface, pass tests) → `@developer`
- Test design (propose test names for an implementation) → `@qa-tester`
- Test implementation (implement handwritten test stub bodies) → `@test-writer`
- You should NOT do architecture, implementation, test design, or test implementation work yourself — use the agents.

## Rule 0 - Generated Files

Never modify files annotated with `@Generated` or containing the header `SENTINEL MANAGED FILE`.
These files are owned by Sentinel and will be overwritten on regeneration.

## Rule 0b - Shell Commands

Do not prefix bash commands with `cd <project-dir>` — the working directory is already the project root. Using `cd` is redundant and causes unnecessary permission prompts.

## Rule A - Source of Truth

Before writing any code, read `sentinel.yaml`. It defines the contracts, boundaries, and expected implementations.

**Never edit `sentinel.yaml` directly.** All architectural changes (adding modules, interfaces, models, implementations, changing dependencies) must go through the **architect agent** (`@architect`). The architect agent proposes changes as patches, validates them, and integrates them with the Architect Studio.

## Rule B - Sealed Interfaces

The following interfaces are `sealed`. Only the listed classes may implement them:

- `LemmaAbsenceConfig` permits: DefaultLemmaAbsenceConfig
- `NodeDiagnoses` permits: (none declared)
- `AnalyzeCommand` permits: (none declared)
- `GetCommand` permits: (none declared)
- `DeleteCommand` permits: (none declared)
- `PruneCommand` permits: (none declared)
- `PlanCommand` permits: (none declared)
- `ReviseCommand` permits: (none declared)
- `ConfigAnalyzerCommand` permits: (none declared)
- `StatsAnalyzerCommand` permits: (none declared)
- `ApproveCommand` permits: (none declared)
- `RejectCommand` permits: (none declared)
- `GetConsolidatedCommand` permits: (none declared)
- `SetActiveAnalysisCommand` permits: (none declared)
- `LemmaAbsenceProposalStrategyRegistry` permits: (none declared)
- `LemmaAbsenceProposalDeriver` permits: (none declared)

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

All architectural changes (modules, models, interfaces, implementations, dependencies, patterns) must go through the **architect agent** (`@architect`), which proposes validated patches using `sentinel patch propose`. The CLI validates the patch, merges it with any existing proposal, and writes it to `.sentinel/proposals/architectural_patch.yaml`.

**Test declarations** (`handwrittenTests`) are NOT architectural changes — they go through the **QA tester agent** (`@qa-tester`), which also uses `sentinel patch propose`.

## Rule W - Development Workflow

The correct development workflow follows these phases **in strict order**:

```
Requirement → Architecture → [accept & sentinel generate] → Test Design → [sentinel generate] → Test Writing → Development
```

### Phases

1. **Requirement** — Define or refine features, business rules, user journeys, and doubts in `REQUIREMENT.md`.
2. **Architecture** — Design the solution in `sentinel.yaml`: modules, models, interfaces, implementations, dependencies.
3. **Accept & Generate (Architecture)** — User accepts the architecture. Run `sentinel generate` to produce contracts and implementation stubs.
4. **Test Design** — `@qa-tester` proposes test names and executes `sentinel patch propose` to add them as `handwrittenTests` in `sentinel.yaml`. Do NOT delegate this to the Architect.
5. **Generate Test Stubs** — Run `sentinel generate` to produce JUnit stub classes.
6. **Test Writing** — `@test-writer` implements the body of each handwritten test stub. It reads REQUIREMENT.md, the specific rule/journey referenced by @Tag, and the models/interfaces needed for fixtures. Does NOT read production code (TDD).
7. **Development** — Implement the production contracts generated by Sentinel. Do NOT implement handwrittenTests here — that was done in the Test Writing phase.

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

## Rule F - Framework Compliance

These rules address observed behavioral violations. They apply to **ALL** agents.

### F1 - Never Remove @Generated

Never remove or alter the `@Generated` annotation from any file. The annotation is Sentinel's ownership marker — removing it means Sentinel can no longer verify, regenerate, or smart-merge the file. If `sentinel verify` fails on a `@Generated` file, the problem is in `sentinel.yaml`, not in the annotation. Escalate to `@architect`.

### F2 - Never Modify @Generated Files

Do not modify files containing `@Generated(value = "com.sentinel.engine.CodeGenerator")` — not even to fix compilation errors. Do not change types, add `implements`, change visibility, add fields, or edit method signatures. If a generated file does not compile or does not match what you need, the root cause is in `sentinel.yaml`. Escalate to `@architect`.

### F3 - Escalation Limit

If you have attempted the same task 3 times and it still fails, **STOP** and escalate. Do not retry indefinitely. Emit an ESCALATION with `type: repeated_failure`, include a summary of the 3 attempts, and let the orchestrator route it.

### F4 - Stay in Your Lane

Each agent has a defined responsibility. Do not perform another agent's work:

- Orchestrators do NOT write tests, implementation code, or architecture patches
- Developers do NOT create test files or propose test names
- Test-writers do NOT move files to other modules or modify production code
- QA testers do NOT implement test bodies
- Architects do NOT propose `handwrittenTests`

See the **Agent Responsibility Matrix** below.

### F5 - No False Bug Reports

Before reporting a problem as a "Sentinel bug" or "Sentinel limitation", verify:

1. Read the relevant section of `sentinel.yaml`
2. Check the `sentinel-dsl-ref` skill for the correct DSL schema
3. Confirm the generated code actually **disagrees** with the DSL definition

If the DSL definition is correct and the generated code matches it, the problem is in your approach, not in Sentinel. Adjust your implementation or escalate.

### F6 - Read Before You Diagnose

Before diagnosing any Sentinel-related issue (visibility, missing types, compilation errors on generated code), you MUST:

1. Read `sentinel.yaml` (or use `tool inspectModule`) to check the actual definition
2. Read the `sentinel-dsl-ref` skill if unsure about DSL schema or capabilities
3. Only then form your diagnosis

Acting on assumptions about what Sentinel "should" generate, without reading the DSL, is prohibited.

## Agent Responsibility Matrix

| Responsibility | @architect | @developer | @qa-tester | @test-writer | Orchestrator |
|---|:---:|:---:|:---:|:---:|:---:|
| Design architecture (sentinel.yaml patches) | **YES** | no | no | no | no |
| Implement production code | no | **YES** | no | no | no |
| Propose test names (handwrittenTests) | no | no | **YES** | no | no |
| Implement test stubs | no | no | no | **YES** | no |
| Move files between modules | no | no | no | no | no |
| Modify @Generated files | no | no | no | no | no |
| Create requirement files | analyst | no | no | no | no |

If your task falls outside your column, delegate to the correct agent.

## Orchestration Guardrails

These rules apply to the **orchestrator** (the agent coordinating specialized agents on behalf of the user). They exist because forced-tag traceability propagates downstream silently — catching it at the orchestration layer is the last line of defense before tests reach `@test-writer`.

### OG1 - Traceability doubts are blocking

When `@qa-tester` reports doubts about rule-to-test traceability — or when you notice forced "closest rule" tagging in a proposed patch — **pause and resolve with the user** before advancing to `@test-writer`. Forced traceability propagates downstream and produces tests that claim to verify rules they do not actually verify.

### OG2 - Respect validator failures on handwrittenTests

`sentinel patch propose` and `sentinel generate` cross-reference every `handwrittenTests[].traceability` against `REQUIREMENT.md`. When the validator rejects a patch with an error like:

```
Handwritten test "shouldXxx" on implementation "YyyZzz" references rule
'F-REVBYP-R099' which does not exist in FEAT-REVBYP (available: F-REVBYP-R001..F-REVBYP-R014).
```

**Do NOT** ask `@qa-tester` to re-tag to the "closest" rule. The validator caught a real problem — resolve it at the source:

1. **Rule was deleted from `REQUIREMENT.md`** → mark the test `_change: delete` via `@qa-tester`.
2. **Test is valid but no existing rule covers it** → route to `@analyst` to add the rule, then re-propose via `@qa-tester`.
3. **Test is mis-tagged** → route to `@qa-tester` to re-tag to the real rule (or drop).
4. **Test verifies internal wiring, not a rule** → drop the test. Pattern-level invariants belong in ArchUnit, not in `handwrittenTests`.

Forcing a tag to make the validator pass is precisely what OG1 prohibits.

### OG3 - Handle `inconsistent_traceability` escalations

When `@test-writer` escalates with `type: inconsistent_traceability`, the test body it was asked to implement does not actually verify the rule its `@Tag` claims. Do NOT instruct it to implement anyway. Route the escalation to `@qa-tester` for re-design (re-tag, drop the test, or propose a new rule via `@analyst`) and surface the decision to the user.

## Module Map

### audit-domain

**Depends on:** course-domain

**Models:** AuditReport, AuditableCourse, AuditableKnowledge, AuditableTopic, AuditableMilestone, AuditableQuiz, CefrLevel, TargetRange, AuditTarget, NlpToken, AnalyzerDescriptor, AuditNode, SentenceLengthDiagnosis, AuditReportSummary, ActiveAnalysisSelection

**Interfaces:** AuditEngine, ContentAnalyzer, AnalysisResult, NlpTokenizer, SentenceLengthConfig, ScoreAggregator, CocaBucketsConfig, ContentWordFilter, LemmaRecurrenceConfig, LemmaAbsenceConfig, EvpCatalogPort, AuditableEntity, SelfDescribingConfig, NodeDiagnoses, CourseDiagnoses, LevelDiagnoses, TopicDiagnoses, KnowledgeDiagnoses, QuizDiagnoses, AuditReportStore, CourseMapper, ActiveAnalysisSelectionStore

**Implementations:** IAuditEngine, KnowledgeTitleLengthAnalyzer, KnowledgeInstructionsLengthAnalyzer, SentenceLengthAnalyzer, IScoreAggregator

### course-domain

**Models:** NodeKind, SentencePartKind, CourseEntity, RootNodeEntity, MilestoneEntity, TopicEntity, KnowledgeEntity, QuizTemplateEntity, FormEntity, SentencePartEntity, CourseValidationException

**Interfaces:** CourseRepository, CourseValidator

### refiner-domain

**Depends on:** audit-domain

**Models:** DiagnosisKind, RefinementTaskStatus, RefinementTask, RefinementPlan, SuggestedLemma, SentenceLengthCorrectionContext, MisplacedLemmaContext, LemmaAbsenceCorrectionContext, LengthDirection

**Interfaces:** RefinerEngine, RefinementPlanStore, CorrectionContextResolver, CorrectionContext

**Implementations:** SentenceLengthContextResolver, LemmaAbsenceContextResolver, DispatchingCorrectionContextResolver, DefaultRefinerEngine

### audit-application

**Depends on:** audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain

**Interfaces:** AuditRunner, AnalyzerRegistry

**Implementations:** CourseToAuditableMapper, DefaultSentenceLengthConfig, DefaultAuditRunner, DefaultCocaBucketsConfig, DefaultLemmaRecurrenceConfig, DefaultLemmaAbsenceConfig, DefaultAnalyzerRegistry

### course-infrastructure

**Depends on:** course-domain

**Implementations:** FileSystemCourseRepository

### audit-cli

**Depends on:** audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure

**Models:** GetTasksFilter, LagenMode, PlanStorageMode

**Interfaces:** AnalyzeCommand, GetCommand, DeleteCommand, PruneCommand, PlanCommand, ReviseCommand, ConfigAnalyzerCommand, StatsAnalyzerCommand, ApproveCommand, RejectCommand, GetConsolidatedCommand, SetActiveAnalysisCommand

### nlp-infrastructure

**Depends on:** audit-domain

**Models:** NlpTokenizerConfig

**Interfaces:** NlpTokenizerFactory

### vocabulary-infrastructure

**Depends on:** audit-domain

### audit-infrastructure

**Depends on:** audit-domain, refiner-domain, revision-domain

**Implementations:** FileSystemAuditReportStore, FileSystemRefinementPlanStore, FileSystemRevisionArtifactStore, FileSystemImpactPreviewStore, FileSystemActiveAnalysisSelectionStore

### revision-domain

**Depends on:** audit-domain, refiner-domain, course-domain

**Models:** RevisionVerdict, RevisionOutcomeKind, CourseElementSnapshot, RevisionProposal, RevisionArtifact, RevisionOutcome, RevisionEngineConfig, ApprovalMode, ProposalDecisionOutcomeKind, ProposalDecisionOutcome, StrategyId, LemmaAbsenceQuizCandidate, ProposalStrategyFailedException, ProposalDerivationException, ConsolidatedViewBuilderConfig

**Interfaces:** Reviser, RevisionValidator, RevisionValidatorResult, RevisionArtifactStore, CourseElementLocator, RevisionEngine, RevisionEngineFactory, RevisionValidatorFactory, ProposalDecisionService, ProposalDecisionServiceFactory, LemmaAbsenceProposalStrategy, LemmaAbsenceProposalStrategyRegistry, LemmaAbsenceProposalDeriver, ImpactPreviewStore

### revision-infrastructure

**Depends on:** revision-domain, refiner-domain

## Features & Business Rules

Features, business rules, and user journeys for this project are defined in `REQUIREMENT.md` (and any other files referenced from `sentinel.yaml#definitions`).

**Only `@analyst` needs the full requirement detail.** Other agents should work against the contracts in their own module's `AGENTS.md` and delegate to `@analyst` when they need rule or journey context.

## Boundaries

| Module | Can Access |
|--------|------------|
| audit-domain | course-domain |
| course-domain | (none) |
| refiner-domain | audit-domain |
| audit-application | audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain |
| course-infrastructure | course-domain |
| audit-cli | audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure |
| nlp-infrastructure | audit-domain |
| vocabulary-infrastructure | audit-domain |
| audit-infrastructure | audit-domain, refiner-domain, revision-domain |
| revision-domain | audit-domain, refiner-domain, course-domain |
| revision-infrastructure | revision-domain, refiner-domain |

**Access Control:**

| Module | Allowed Clients |
|--------|----------------|
| revision-infrastructure | audit-cli |

