<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Filesystem persistence adapters for audit reports

## Implementations

### FileSystemAuditReportStore

**Implements:** AuditReportStore

**Types:** Repository

**Tests that must pass:**

- should save an AuditReport and load it back with identical content

### FileSystemRefinementPlanStore

**Implements:** RefinementPlanStore

**Types:** Repository

### FileSystemRevisionArtifactStore

**Implements:** RevisionArtifactStore

**Types:** Repository

**Tests that must pass:**

- Given a RevisionArtifact, when save is called, then a file is written under .content-audit/revisions/ → FEAT-REVBYP/F-REVBYP-R008
- Given an artifact for plan P1 and proposal PR1, when save is called, then the file path is .content-audit/revisions/P1/PR1.<ext> → FEAT-REVBYP/F-REVBYP-R009
- Given a persisted artifact, when load is called, then all RevisionProposal fields plus verdict and rejectionReason are recoverable → FEAT-REVBYP/F-REVBYP-R010
- Given artifacts saved under multiple plan directories, when findByProposalId(id, Optional.empty()) is called, then it scans subdirectories and returns the matching artifact → FEAT-REVAPR/F-REVAPR-R002
- Given no artifact matches the id, when findByProposalId(id, Optional.empty()) is called, then it returns Optional.empty → FEAT-REVAPR/F-REVAPR-R002
- Given an artifact saved under plan P1, when findByProposalId(id, Optional.of(P1)) is called, then it takes the direct path and returns the artifact → FEAT-REVAPR/F-REVAPR-R002
- Given no artifact under plan P1 with that id, when findByProposalId(id, Optional.of(P1)) is called, then it returns Optional.empty → FEAT-REVAPR/F-REVAPR-R002
- Given a PENDING_APPROVAL artifact whose taskId matches, when hasPendingProposalForTask is called, then it returns true → FEAT-REVAPR/F-REVAPR-R009
- Given only an APPROVED artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given only a REJECTED artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given no artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given artifacts saved under multiple plan directories, when list() is called, then it returns all of them across all plans → FEAT-REVAPR/F-REVAPR-R002
- Given the revisions directory is empty or missing, when list() is called, then it returns an empty list → FEAT-REVAPR/F-REVAPR-R002
- Given the store is constructed with a non-default baseDir, when save is called, then the artifact file lands under <baseDir>/.content-audit/revisions/<planId>/<proposalId>.* and NOT under System.getProperty('user.dir') → FEAT-REVAPR/F-REVAPR-R017

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

### From audit-domain

## Models

### AuditReport (`record`)

| Field | Type |
|-------|------|
| root | `AuditNode` |

### AuditableCourse (`record`)

| Field | Type |
|-------|------|
| milestones | `List<AuditableMilestone>` |

### AuditableKnowledge (`record`)

| Field | Type |
|-------|------|
| quizzes | `List<AuditableQuiz>` |
| title | `String` |
| instructions | `String` |
| isSentence | `boolean` |
| id | `String` |
| label | `String` |
| code | `String` |

### AuditableTopic (`record`)

| Field | Type |
|-------|------|
| knowledge | `List<AuditableKnowledge>` |
| id | `String` |
| label | `String` |
| code | `String` |

### AuditableMilestone (`record`)

| Field | Type |
|-------|------|
| topics | `List<AuditableTopic>` |
| id | `String` |
| label | `String` |
| code | `String` |

### AuditableQuiz (`record`)

| Field | Type |
|-------|------|
| tokens | `List<NlpToken>` |
| id | `String` |
| label | `String` |
| code | `String` |
| translation | `String` |
| sentences | `List<String>` |
| quizSentence | `String` |

### CefrLevel (`enum`)

| Field | Type |
|-------|------|
| A1 | `null` |
| A2 | `null` |
| B1 | `null` |
| B2 | `null` |

### TargetRange (`record`)

| Field | Type |
|-------|------|
| level | `CefrLevel` |
| minTokens | `int` |
| maxTokens | `int` |

### AuditTarget (`enum`)

| Field | Type |
|-------|------|
| QUIZ | `null` |
| KNOWLEDGE | `null` |
| TOPIC | `null` |
| MILESTONE | `null` |
| COURSE | `null` |

### NlpToken (`record`)

| Field | Type |
|-------|------|
| text | `String` |
| lemma | `String` |
| posTag | `String` |
| frequencyRank | `Integer` |
| isStop | `boolean` |
| isPunct | `boolean` |

### AnalyzerDescriptor (`record`)

| Field | Type |
|-------|------|
| name | `String` |
| description | `String` |
| target | `AuditTarget` |

### AuditNode (`record`)

| Field | Type |
|-------|------|
| entity | `AuditableEntity` |
| target | `AuditTarget` |
| parent | `AuditNode` |
| children | `List<AuditNode>` |
| scores | `Map<String,Double>` |
| metadata | `Map<String,Object>` |
| diagnoses | `NodeDiagnoses` |

### SentenceLengthDiagnosis (`record`)

| Field | Type |
|-------|------|
| tokenCount | `int` |
| targetMin | `int` |
| targetMax | `int` |
| cefrLevel | `CefrLevel` |
| delta | `int` |
| toleranceMargin | `int` |

### AuditReportSummary (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| timestamp | `Instant` |
| courseName | `String` |
| overallScore | `double` |

### AuditEngine (port)

Methods:

- `runAudit(AuditableCourse course): AuditReport`

### ContentAnalyzer (port)

Methods:

- `onKnowledge(AuditNode node): Void`
- `onQuiz(AuditNode node): Void`
- `onMilestone(AuditNode node): Void`
- `onTopic(AuditNode node): Void`
- `onCourseComplete(AuditNode rootNode): Void`
- `getName(): String`
- `getTarget(): AuditTarget`
- `getDescription(): String`

### AnalysisResult (port)

Methods:

- `getName(): String`
- `getScore(): double`
- `getTarget(): AuditTarget`

### NlpTokenizer (port)

Methods:

- `tokenize(String text): List<String>`
- `countTokens(String text): int`
- `analyzeTokens(String text): List<NlpToken>`
- `analyzeTokensBatch(List<String> sentences): Map<String,List<NlpToken>>`

### SentenceLengthConfig (port)

Methods:

- `getTargetRange(CefrLevel level): Optional<TargetRange>`
- `getToleranceMargin(): int`

### ScoreAggregator (port)

Methods:

- `aggregate(AuditNode rootNode): void`

### CocaBucketsConfig (port)

Methods:

- `getBandConfiguration(): BandConfiguration`
- `getTargetsForLevel(String levelName): List<BucketTarget>`
- `getQuarterTargetsForLevel(String levelName): List<QuarterBucketTargets>`
- `getToleranceMargin(): double`
- `getAnalysisStrategy(): AnalysisStrategy`
- `getProgressionExpectations(): List<ProgressionExpectation>`

### ContentWordFilter (port)

Methods:

- `isContentWord(NlpToken token): boolean`

### LemmaRecurrenceConfig (port)

Methods:

- `getTop(): int`
- `getSubExposedThreshold(): double`
- `getOverExposedThreshold(): double`

### LemmaAbsenceConfig (port) [sealed]

Methods:

- `getAbsoluteThreshold(CefrLevel level): int`
- `getPercentageThreshold(CefrLevel level): double`
- `getLevelWeight(CefrLevel level): double`
- `getHighPriorityBound(): int`
- `getMediumPriorityBound(): int`
- `getLowPriorityBound(): int`
- `getHighPriorityAlertThreshold(): int`
- `getMediumPriorityAlertThreshold(): int`
- `getLowPriorityAlertThreshold(): int`
- `getCriticalAbsenceThreshold(): int`
- `getAcceptableAbsenceThreshold(): int`
- `getHighReportLimit(): int`
- `getMediumReportLimit(): int`
- `getLowReportLimit(): int`
- `getDiscountPerLevel(): double`
- `getCoverageTarget(CefrLevel level): double`

### EvpCatalogPort (port)

Methods:

- `getExpectedLemmas(CefrLevel level): Set<LemmaAndPos>`
- `isPhrase(String lemma): boolean`
- `getCocaRank(LemmaAndPos lemmaAndPos): Optional<Integer>`
- `getSemanticCategory(LemmaAndPos lemmaAndPos): Optional<String>`

### AuditableEntity (port)

Methods:

- `getId(): String`
- `getLabel(): String`
- `getCode(): String`

### SelfDescribingConfig (port)

Methods:

- `describe(): Map<String,Object>`

### NodeDiagnoses (port) [sealed]

### CourseDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaAbsenceCourseDiagnosis>`
- `getCocaBucketsDiagnosis(): Optional<CocaProgressionDiagnosis>`

### LevelDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaAbsenceLevelDiagnosis>`
- `getCocaBucketsDiagnosis(): Optional<CocaBucketsLevelDiagnosis>`

### TopicDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`
- `getCocaBucketsDiagnosis(): Optional<CocaBucketsTopicDiagnosis>`

### KnowledgeDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`

### QuizDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`
- `getSentenceLengthDiagnosis(): Optional<SentenceLengthDiagnosis>`

### AuditReportStore (port)

Methods:

- `save(AuditReport report): String`
- `load(String id): Optional<AuditReport>`
- `loadLatest(): Optional<AuditReport>`
- `list(): List<AuditReportSummary>`

### From refiner-domain

## Models

### DiagnosisKind (`enum`)

| Field | Type |
|-------|------|
| SENTENCE_LENGTH | `null` |
| LEMMA_ABSENCE | `null` |
| COCA_BUCKETS | `null` |
| LEMMA_RECURRENCE | `null` |
| KNOWLEDGE_TITLE_LENGTH | `null` |
| KNOWLEDGE_INSTRUCTIONS_LENGTH | `null` |

### RefinementTaskStatus (`enum`)

| Field | Type |
|-------|------|
| PENDING | `null` |
| COMPLETED | `null` |
| SKIPPED | `null` |

### RefinementTask (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| nodeTarget | `AuditTarget` |
| nodeId | `String` |
| nodeLabel | `String` |
| diagnosisKind | `DiagnosisKind` |
| priority | `int` |
| status | `RefinementTaskStatus` |

### RefinementPlan (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| sourceAuditId | `String` |
| createdAt | `Instant` |
| tasks | `List<RefinementTask>` |

### SuggestedLemma (`record`)

| Field | Type |
|-------|------|
| lemma | `String` |
| pos | `String` |
| reason | `String` |
| cocaRank | `Integer` |

### SentenceLengthCorrectionContext (`record`)

| Field | Type |
|-------|------|
| taskId | `String` |
| sentence | `String` |
| translation | `String` |
| knowledgeTitle | `String` |
| knowledgeInstructions | `String` |
| topicLabel | `String` |
| cefrLevel | `CefrLevel` |
| tokenCount | `int` |
| targetMin | `int` |
| targetMax | `int` |
| delta | `int` |
| suggestedLemmas | `List<SuggestedLemma>` |

### MisplacedLemmaContext (`record`)

| Field | Type |
|-------|------|
| lemma | `String` |
| pos | `String` |
| expectedLevel | `CefrLevel` |
| quizLevel | `CefrLevel` |
| cocaRank | `Integer` |

### LemmaAbsenceCorrectionContext (`record`)

| Field | Type |
|-------|------|
| taskId | `String` |
| sentence | `String` |
| translation | `String` |
| knowledgeTitle | `String` |
| knowledgeInstructions | `String` |
| topicLabel | `String` |
| cefrLevel | `CefrLevel` |
| misplacedLemmas | `List<MisplacedLemmaContext>` |
| suggestedLemmas | `List<SuggestedLemma>` |
| quizSentence | `String` |

### RefinerEngine (port)

Methods:

- `plan(AuditReport report, String auditId): RefinementPlan`
- `nextTask(RefinementPlan plan): Optional<RefinementTask>`

### RefinementPlanStore (port)

Methods:

- `save(RefinementPlan plan): String`
- `load(String id): Optional<RefinementPlan>`
- `loadLatest(): Optional<RefinementPlan>`

### CorrectionContextResolver<T extends CorrectionContext> (port)

Methods:

- `resolve(AuditReport report, RefinementTask task): Optional<T>`

### CorrectionContext (port)

### From revision-domain

## Models

### RevisionVerdict (`enum`)

| Field | Type |
|-------|------|
| APPROVED | `null` |
| REJECTED | `null` |
| PENDING_APPROVAL | `null` |

### RevisionOutcomeKind (`enum`)

| Field | Type |
|-------|------|
| APPROVED_APPLIED | `null` |
| APPROVED_APPLY_FAILED | `null` |
| REJECTED | `null` |
| NO_REVISER | `null` |
| CONTEXT_UNAVAILABLE | `null` |
| ELEMENT_NOT_FOUND | `null` |
| PENDING_APPROVAL_PERSISTED | `null` |
| ALREADY_PENDING_DECISION | `null` |
| NO_ACTIVE_STRATEGY | `null` |
| STRATEGY_FAILED | `null` |

### CourseElementSnapshot (`record`)

| Field | Type |
|-------|------|
| nodeTarget | `AuditTarget` |
| nodeId | `String` |
| quiz | `QuizTemplateEntity` |

### RevisionProposal (`record`)

| Field | Type |
|-------|------|
| proposalId | `String` |
| taskId | `String` |
| planId | `String` |
| sourceAuditId | `String` |
| diagnosisKind | `DiagnosisKind` |
| nodeTarget | `AuditTarget` |
| nodeId | `String` |
| elementBefore | `CourseElementSnapshot` |
| elementAfter | `CourseElementSnapshot` |
| rationale | `String` |
| reviserKind | `String` |
| createdAt | `Instant` |
| strategyId | `StrategyId` |

### RevisionArtifact (`record`)

| Field | Type |
|-------|------|
| proposal | `RevisionProposal` |
| verdict | `RevisionVerdict` |
| rejectionReason | `String` |
| outcome | `RevisionOutcomeKind` |
| decidedAt | `Instant` |
| decisionNote | `String` |

### RevisionOutcome (`record`)

| Field | Type |
|-------|------|
| kind | `RevisionOutcomeKind` |
| artifact | `RevisionArtifact` |
| errorMessage | `String` |

### RevisionEngineConfig (`record`)

| Field | Type |
|-------|------|
| revisers | `Map<DiagnosisKind,Reviser>` |
| validator | `RevisionValidator` |
| artifactStore | `RevisionArtifactStore` |
| courseRepository | `CourseRepository` |
| elementLocator | `CourseElementLocator` |
| refinementPlanStore | `RefinementPlanStore` |
| auditReportStore | `AuditReportStore` |
| contextResolver | `CorrectionContextResolver<CorrectionContext>` |
| lemmaAbsenceStrategyRegistry | `LemmaAbsenceProposalStrategyRegistry` |
| lemmaAbsenceProposalDeriver | `LemmaAbsenceProposalDeriver` |

### ApprovalMode (`enum`)

| Field | Type |
|-------|------|
| AUTO | `null` |
| HUMAN | `null` |

### ProposalDecisionOutcomeKind (`enum`)

| Field | Type |
|-------|------|
| APPROVED_APPLIED | `null` |
| APPROVED_APPLY_FAILED | `null` |
| REJECTED | `null` |
| NOT_FOUND | `null` |
| ALREADY_DECIDED | `null` |

### ProposalDecisionOutcome (`record`)

| Field | Type |
|-------|------|
| kind | `ProposalDecisionOutcomeKind` |
| artifact | `RevisionArtifact` |
| errorMessage | `String` |

### StrategyId (`record`)

| Field | Type |
|-------|------|
| name | `String` |
| version | `String` |
| providerId | `String` |

### LemmaAbsenceQuizCandidate (`record`)

| Field | Type |
|-------|------|
| quizSentence | `String` |
| translation | `String` |

### ProposalStrategyFailedException (`exception`)

**Extends:** `RuntimeException`

**Message:** `La estrategia de propuesta '%s' no pudo generar un candidato de quiz para la tarea '%s': %s`

| Field | Type |
|-------|------|
| strategyName | `String` |
| taskId | `String` |
| reason | `String` |

### ProposalDerivationException (`exception`)

**Extends:** `RuntimeException`

**Message:** `No se pudo derivar elementAfter desde el candidato de la estrategia '%s' en la tarea '%s': %s`

| Field | Type |
|-------|------|
| strategyName | `String` |
| taskId | `String` |
| reason | `String` |

### Reviser (port)

Methods:

- `propose(RefinementTask task, CorrectionContext context, CourseElementSnapshot before): RevisionProposal`
- `handles(DiagnosisKind kind): boolean`
- `reviserKind(): String`

### RevisionValidator (port)

Methods:

- `validate(RevisionProposal proposal): RevisionValidatorResult`

### RevisionValidatorResult

Methods:

- `verdict(): RevisionVerdict`
- `rejectionReason(): Optional<String>`

### RevisionArtifactStore (port)

Methods:

- `save(RevisionArtifact artifact): String`
- `load(String planId, String proposalId): Optional<RevisionArtifact>`
- `listByPlan(String planId): List<RevisionArtifact>`
- `findByProposalId(String proposalId, Optional<String> planId): Optional<RevisionArtifact>`
- `hasPendingProposalForTask(String planId, String taskId): boolean`
- `list(): List<RevisionArtifact>`

### CourseElementLocator (port)

Methods:

- `snapshot(CourseEntity course, AuditTarget target, String nodeId): Optional<CourseElementSnapshot>`
- `replace(CourseEntity course, CourseElementSnapshot replacement): CourseEntity`

### RevisionEngine (port)

Methods:

- `revise(String planId, String taskId, Path coursePath): RevisionOutcome`

### RevisionEngineFactory (factory)

Methods:

- `create(RevisionEngineConfig config): RevisionEngine`

### RevisionValidatorFactory (factory)

Methods:

- `create(ApprovalMode mode): RevisionValidator`

### ProposalDecisionService (port)

Methods:

- `approve(String proposalId, Optional<String> planId, Optional<String> note, Path coursePath): ProposalDecisionOutcome`
- `reject(String proposalId, Optional<String> planId, Optional<String> reason): ProposalDecisionOutcome`

### ProposalDecisionServiceFactory (factory)

Methods:

- `create(RevisionEngineConfig config): ProposalDecisionService`

### LemmaAbsenceProposalStrategy (port)

Methods:

- `id(): StrategyId`
- `handles(DiagnosisKind kind): boolean`
- `propose(RefinementTask task, LemmaAbsenceCorrectionContext context): LemmaAbsenceQuizCandidate`

### LemmaAbsenceProposalStrategyRegistry (service) [sealed]

Methods:

- `active(): Optional<LemmaAbsenceProposalStrategy>`
- `byName(String name): Optional<LemmaAbsenceProposalStrategy>`
- `listAll(): List<StrategyId>`

### LemmaAbsenceProposalDeriver (service) [sealed]

Methods:

- `derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate): CourseElementSnapshot`

### From course-domain

## Models

### NodeKind (`enum`)

| Field | Type |
|-------|------|
| ROOT | `null` |
| MILESTONE | `null` |
| TOPIC | `null` |
| KNOWLEDGE | `null` |

### SentencePartKind (`enum`)

| Field | Type |
|-------|------|
| TEXT | `null` |
| CLOZE | `null` |

### CourseEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| title | `String` |
| knowledgeIds | `List<String>` |
| root | `RootNodeEntity` |
| slug | `String` |

### RootNodeEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| code | `String` |
| kind | `NodeKind` |
| label | `String` |
| children | `List<String>` |
| milestones | `List<MilestoneEntity>` |

### MilestoneEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| code | `String` |
| kind | `NodeKind` |
| label | `String` |
| oldId | `String` |
| parentId | `String` |
| children | `List<String>` |
| order | `int` |
| slug | `String` |
| topics | `List<TopicEntity>` |

### TopicEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| code | `String` |
| kind | `NodeKind` |
| label | `String` |
| oldId | `String` |
| parentId | `String` |
| children | `List<String>` |
| ruleIds | `List<String>` |
| order | `int` |
| slug | `String` |
| knowledges | `List<KnowledgeEntity>` |

### KnowledgeEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| code | `String` |
| kind | `NodeKind` |
| label | `String` |
| oldId | `String` |
| parentId | `String` |
| isRule | `boolean` |
| instructions | `String` |
| order | `int` |
| slug | `String` |
| quizTemplates | `List<QuizTemplateEntity>` |

### QuizTemplateEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| oidId | `String` |
| kind | `String` |
| knowledgeId | `String` |
| title | `String` |
| instructions | `String` |
| translation | `String` |
| theoryId | `String` |
| topicName | `String` |
| form | `FormEntity` |
| difficulty | `double` |
| retries | `double` |
| noScoreRetries | `double` |
| code | `String` |
| audioUrl | `String` |
| imageUrl | `String` |
| answerAudioUrl | `String` |
| answerImageUrl | `String` |
| miniTheory | `String` |
| successMessage | `String` |

### FormEntity (`record`)

| Field | Type |
|-------|------|
| kind | `String` |
| incidence | `double` |
| label | `String` |
| name | `String` |
| sentenceParts | `List<SentencePartEntity>` |

### SentencePartEntity (`record`)

| Field | Type |
|-------|------|
| kind | `SentencePartKind` |
| text | `String` |
| options | `List<String>` |

### CourseValidationException (`exception`)

**Extends:** `RuntimeException`

**Message:** `Error al cargar el curso desde '%s': %s. La carga fue abortada.`

| Field | Type |
|-------|------|
| path | `String` |
| detail | `String` |

### CourseRepository (port)

Methods:

- `load(Path path): CourseEntity`
- `save(CourseEntity course, Path path): void`

### CourseValidator (service)

Methods:

- `validate(CourseEntity course): void`

