<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: refiner-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Domain module for the refinement workflow. Defines the plan/task model and ports for generating and persisting refinement plans derived from audit reports.

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

## Interfaces

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

## Implementations

### SentenceLengthContextResolver

**Implements:** CorrectionContextResolver<SentenceLengthCorrectionContext>

**Tests that must pass:**

- should resolve context with all fields populated from quiz diagnosis and ancestor entities → FEAT-RCSL/F-RCSL-R001
- should populate sentence and translation from AuditableQuiz entity on the quiz node → FEAT-RCSL/F-RCSL-R001
- should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor → FEAT-RCSL/F-RCSL-R001
- should populate topicLabel from AuditableTopic on topic ancestor → FEAT-RCSL/F-RCSL-R001
- should populate cefrLevel tokenCount targetMin targetMax and delta from SentenceLengthDiagnosis → FEAT-RCSL/F-RCSL-R001
- should return empty when quiz node is not found in the audit tree → FEAT-RCSL/F-RCSL-R002
- should return empty when task nodeTarget does not match any node target in the tree → FEAT-RCSL/F-RCSL-R002
- should locate the correct quiz node when multiple quiz nodes exist in the tree → FEAT-RCSL/F-RCSL-R002
- should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas and exclude APPEARS_TOO_EARLY → FEAT-RCSL/F-RCSL-R003
- should order suggested lemmas by COCA rank ascending with lowest rank first → FEAT-RCSL/F-RCSL-R003
- should place lemmas without COCA rank after lemmas with COCA rank → FEAT-RCSL/F-RCSL-R003
- should map AbsentLemma fields to SuggestedLemma fields correctly → FEAT-RCSL/F-RCSL-R003
- should return empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY → FEAT-RCSL/F-RCSL-R003
- should return suggested lemmas from the milestone ancestor of the quiz node → FEAT-RCSL/F-RCSL-R003
- should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis → FEAT-RCSL/F-RCSL-R004
- should return context with empty suggested lemmas when milestone ancestor is not found → FEAT-RCSL/F-RCSL-R004
- should return context with empty suggested lemmas when absent lemmas list is empty → FEAT-RCSL/F-RCSL-R004
- should limit suggested lemmas to 10 when more than 10 qualify after filtering → FEAT-RCSL/F-RCSL-R005
- should resolve context with negative delta for a sentence shorter than target range → FEAT-RCSL/F-RCSL-R001
- should resolve context with zero delta when sentence is within target range → FEAT-RCSL/F-RCSL-R001
- should set taskId from the RefinementTask id → FEAT-RCSL/F-RCSL-R001

### LemmaAbsenceContextResolver

**Implements:** CorrectionContextResolver<LemmaAbsenceCorrectionContext>

**Tests that must pass:**

- should resolve context with all fields populated from quiz diagnosis and ancestor entities → FEAT-RCLA/F-RCLA-R003
- should populate sentence and translation from AuditableQuiz entity on the quiz node → FEAT-RCLA/F-RCLA-R003
- should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor → FEAT-RCLA/F-RCLA-R003
- should populate topicLabel from AuditableTopic on topic ancestor → FEAT-RCLA/F-RCLA-R003
- should populate cefrLevel from milestone ancestor → FEAT-RCLA/F-RCLA-R003
- should populate misplacedLemmas from LemmaPlacementDiagnosis on quiz node → FEAT-RCLA/F-RCLA-R004
- should map MisplacedLemma fields to MisplacedLemmaContext fields correctly → FEAT-RCLA/F-RCLA-R004
- should include expectedLevel and quizLevel in each MisplacedLemmaContext entry → FEAT-RCLA/F-RCLA-R004
- should include cocaRank as null in MisplacedLemmaContext when not available → FEAT-RCLA/F-RCLA-R004
- should return empty when quiz node is not found in the audit tree → FEAT-RCLA/F-RCLA-R005
- should return empty when task nodeTarget does not match any node target in the tree → FEAT-RCLA/F-RCLA-R005
- should locate the correct quiz node when multiple quiz nodes exist in the tree → FEAT-RCLA/F-RCLA-R005
- should return empty when quiz node has no LemmaPlacementDiagnosis → FEAT-RCLA/F-RCLA-R006
- should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas in suggestedLemmas and exclude APPEARS_TOO_EARLY → FEAT-RCLA/F-RCLA-R004b
- should order suggested lemmas by COCA rank ascending with lowest rank first → FEAT-RCLA/F-RCLA-R004b
- should place lemmas without COCA rank after lemmas with COCA rank in suggestedLemmas → FEAT-RCLA/F-RCLA-R004b
- should map AbsentLemma fields to SuggestedLemma fields correctly → FEAT-RCLA/F-RCLA-R004b
- should limit suggested lemmas to 10 when more than 10 qualify after filtering → FEAT-RCLA/F-RCLA-R004b
- should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis → FEAT-RCLA/F-RCLA-R004c
- should return context with empty suggested lemmas when milestone ancestor is not found → FEAT-RCLA/F-RCLA-R004c
- should return context with empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY → FEAT-RCLA/F-RCLA-R004c
- should set taskId from the RefinementTask id → FEAT-RCLA/F-RCLA-R003

### DispatchingCorrectionContextResolver

**Implements:** CorrectionContextResolver<CorrectionContext>

**Dependencies (constructor injection):**

- `sentenceLengthResolver`: `SentenceLengthContextResolver`
- `lemmaAbsenceResolver`: `LemmaAbsenceContextResolver`

**Tests that must pass:**

- should delegate to sentenceLengthResolver when task diagnosis is SENTENCE_LENGTH → FEAT-RCLA/F-RCLA-R007
- should delegate to lemmaAbsenceResolver when task diagnosis is LEMMA_ABSENCE → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind COCA_BUCKETS → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind LEMMA_RECURRENCE → FEAT-RCLA/F-RCLA-R007
- should propagate empty from delegate when delegate returns empty → FEAT-RCLA/F-RCLA-R007

### DefaultRefinerEngine

**Implements:** RefinerEngine

**Tests that must pass:**

- should include LEMMA_ABSENCE tasks targeting QUIZ when quiz has lemma-absence score below 1.0 → FEAT-RCLA/F-RCLA-R001
- should not include LEMMA_ABSENCE tasks targeting MILESTONE or COURSE in the refinement plan → FEAT-RCLA/F-RCLA-R001
- should not generate LEMMA_ABSENCE task for quiz with lemma-absence score equal to 1.0 → FEAT-RCLA/F-RCLA-R001
- should still generate COCA_BUCKETS and LEMMA_RECURRENCE tasks at MILESTONE and COURSE level after re-routing → FEAT-RCLA/F-RCLA-R001

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
| sentence | `String` |
| tokens | `List<NlpToken>` |
| id | `String` |
| label | `String` |
| code | `String` |
| translation | `String` |

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

