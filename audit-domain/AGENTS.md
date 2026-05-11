<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Core business logic

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

### ActiveAnalysisSelection (`record`)

| Field | Type |
|-------|------|
| auditId | `String` |
| planId | `String` |

## Interfaces

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
- `lookupLevel(LemmaAndPos lemmaAndPos): Optional<CefrLevel>`

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
- `getLemmaCountDiagnosis(): Optional<LemmaCountCourseDiagnosis>`

### LevelDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaAbsenceLevelDiagnosis>`
- `getCocaBucketsDiagnosis(): Optional<CocaBucketsLevelDiagnosis>`
- `getLemmaCountDiagnosis(): Optional<LemmaCountLevelDiagnosis>`

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

### CourseMapper (port)

Methods:

- `map(CourseEntity course): AuditableCourse`

### ActiveAnalysisSelectionStore (port)

Methods:

- `read(): Optional<ActiveAnalysisSelection>`
- `write(ActiveAnalysisSelection selection): void`
- `clear(): void`

### AuditNodeIndex (port)

Methods:

- `find(String nodeId, AuditTarget nodeTarget): Optional<AuditNode>`

### AuditNodeIndexFactory (factory)

Methods:

- `build(AuditReport report): AuditNodeIndex`

### LemmaCountConfig (port) [sealed]

Methods:

- `getThreshold(): int`

## Implementations

### IAuditEngine

**Implements:** AuditEngine

**Dependencies (constructor injection):**

- `contentAnalyzers`: `List<ContentAnalyzer>`
- `scoreAggregator`: `ScoreAggregator`

**Tests that must pass:**

- Given a course with milestones, topics and knowledges, when AuditEngine.runAudit is invoked with both KTLEN analyzers, then the report root contains aggregated knowledge-title-length and knowledge-instructions-length scores at every hierarchy level → FEAT-KTLEN
- Given a course with knowledges whose title weighted length exceeds 28, when AuditEngine.runAudit is invoked, then the affected knowledge nodes carry knowledge-title-length scores below 1.0 enabling identification of overlong titles → FEAT-KTLEN
- Given a course with mixed title and instruction lengths, when AuditEngine.runAudit is invoked, then per-level scores for knowledge-title-length and knowledge-instructions-length are reported independently per AuditNode allowing the user to compare both dimensions → FEAT-KTLEN
- Given two AuditableCourses with identical content but milestones declared in different orders, when AuditEngine.runAudit is invoked on each with the LemmaRecurrenceAnalyzer registered, then both AuditReports produce the same lemma-recurrence score confirming a deterministic CEFR-ordered traversal independent of input declaration order → FEAT-LREC/F-LREC-R002

### KnowledgeTitleLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- should score 0.5 for title of weighted length 28.5 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 29 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 35 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for title well beyond limit at weighted length 70 → FEAT-KTLEN/F-KTLEN-R003
- should return knowledge-title-length as analyzer name → FEAT-KTLEN/F-KTLEN-R008
- should return KNOWLEDGE as audit target → FEAT-KTLEN/F-KTLEN-R008
- should score 0.0 for knowledge with null title → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for knowledge with empty title → FEAT-KTLEN/F-KTLEN-R003
- should score 1.0 for knowledge with title within limit → FEAT-KTLEN/F-KTLEN-R003
- should score 1.0 for knowledge with title at exactly 28 weighted chars → FEAT-KTLEN/F-KTLEN-R001
- should score 1.0 for title fitting with weighted length 5.1 → FEAT-KTLEN/F-KTLEN-R002
- should score 1.0 for zero-weight special chars title → FEAT-KTLEN/F-KTLEN-R002
- should score 1.0 for mixed-weight title with weighted length 2.7 → FEAT-KTLEN/F-KTLEN-R002
- should score 0.75 for title of weighted length 35 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.5 for title of weighted length 42 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 56 → FEAT-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 70 → FEAT-KTLEN/F-KTLEN-R003
- should complete without error when onQuiz is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onMilestone is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onTopic is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onCourseComplete is called → FEAT-KTLEN/F-KTLEN-R008
- should return two correctly scored items for two knowledges with different title lengths → FEAT-KTLEN/F-KTLEN-R003
- should return empty list when no knowledges have been processed → FEAT-KTLEN/F-KTLEN-R003

### KnowledgeInstructionsLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- should score 1.0 for instructions exactly at soft limit of 70 weighted chars → FEAT-KTLEN/F-KTLEN-R005
- should score 1.0 for instructions of 30 weighted chars within soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 weighted chars just above soft limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions exactly at hard limit of 100 weighted chars → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions of 85 weighted chars between soft and hard limits → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 weighted chars just above hard limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.0 for instructions of 200 weighted chars well above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should use weighted character length not plain string length for scoring instructions → FEAT-KTLEN/F-KTLEN-R005
- should return knowledge-instructions-length as analyzer name → FEAT-KTLEN/F-KTLEN-R008
- should return KNOWLEDGE as audit target → FEAT-KTLEN/F-KTLEN-R008
- should score 1.0 for knowledge with null instructions → FEAT-KTLEN/F-KTLEN-R006
- should score 1.0 for knowledge with empty instructions → FEAT-KTLEN/F-KTLEN-R006
- should score 1.0 for instructions exactly at soft limit of 70 chars → FEAT-KTLEN/F-KTLEN-R006
- should score 1.0 for instructions of 30 chars within soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 chars just above soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions exactly at hard limit of 100 chars → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 85 chars between soft and hard limits → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 chars just above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 200 chars well above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should complete without error when onQuiz is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onMilestone is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onTopic is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onCourseComplete is called → FEAT-KTLEN/F-KTLEN-R008
- should produce correct scores for three knowledges with different instruction lengths → FEAT-KTLEN/F-KTLEN-R006
- should use weighted character length not plain string length for scoring instructions → FEAT-KTLEN/F-KTLEN-R002
- should distinguish three scoring ranges 1.0 at-or-below-70 0.5 above-70-up-to-100 0.0 above-100 at the declared weighted-char thresholds → FEAT-KTLEN/F-KTLEN-R005

### SentenceLengthAnalyzer

**Implements:** ContentAnalyzer

**Dependencies (constructor injection):**

- `nlpTokenizer`: `NlpTokenizer`
- `config`: `SentenceLengthConfig`

**Tests that must pass:**

- should exclude quiz when milestoneId is null → FEAT-SLEN/F-SLEN-R001
- should exclude quiz when milestoneId is non-numeric → FEAT-SLEN/F-SLEN-R001
- should exclude quiz when no target range configured for level → FEAT-SLEN/F-SLEN-R012
- should score only sentence quizzes when processing mixed knowledge types → FEAT-SLEN/F-SLEN-R001
- should return sentence-length as analyzer name → FEAT-SLEN/F-SLEN-R001
- should return QUIZ as audit target → FEAT-SLEN/F-SLEN-R001
- should score 1.0 for quiz within A1 range → FEAT-SLEN/F-SLEN-R002
- should score 0.75 for quiz 1 token above A1 max → FEAT-SLEN/F-SLEN-R002
- should score 0.25 for quiz 3 tokens below A1 min → FEAT-SLEN/F-SLEN-R002
- should score 1.0 for quiz exactly at A1 minimum boundary → FEAT-SLEN/F-SLEN-R002
- should score 1.0 for quiz exactly at A1 maximum boundary → FEAT-SLEN/F-SLEN-R002
- should score 0.0 for quiz 4 tokens above A1 max at tolerance boundary → FEAT-SLEN/F-SLEN-R009
- should exclude non-sentence knowledge quiz from results → FEAT-SLEN/F-SLEN-R001
- should score 1.0 for B2 level quiz within range → FEAT-SLEN/F-SLEN-R012
- should score 0.0 for quiz exactly at tolerance boundary → FEAT-SLEN/F-SLEN-R009
- should score 0.5 for quiz 2 tokens above A1 max → FEAT-SLEN/F-SLEN-R002
- should complete without error when onTopic is called → FEAT-SLEN/F-SLEN-R001
- should complete without error when onCourseComplete is called → FEAT-SLEN/F-SLEN-R001
- should produce correct scores for full milestone-knowledge-quiz sequence → FEAT-SLEN/F-SLEN-R002
- should exclude non-sentence quizzes from scoring → FEAT-SLEN/F-SLEN-R001
- should emit a SentenceLengthDiagnosis on the quiz node populated with tokenCount, targetMin, targetMax, cefrLevel, delta and toleranceMargin matching the analyzer computation → FEAT-DSLEN/F-DSLEN-R001
- should NOT emit a SentenceLengthDiagnosis on a quiz node that is excluded as non-sentence (no scoring produced) → FEAT-DSLEN/F-DSLEN-R002
- should NOT emit a SentenceLengthDiagnosis on knowledge topic milestone or course nodes traversed by the analyzer → FEAT-DSLEN/F-DSLEN-R003
- should make the emitted SentenceLengthDiagnosis retrievable via QuizDiagnoses getSentenceLengthDiagnosis on the same quiz node → FEAT-DSLEN/F-DSLEN-R004

### IScoreAggregator

**Implements:** ScoreAggregator

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

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

