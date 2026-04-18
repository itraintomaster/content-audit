<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-application (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

## Interfaces

### AuditRunner (service)

Methods:

- `runAudit(Path coursePath,Set<String> analyzerNames): AuditReport`
- `runDetailedAudit(Path coursePath,String analyzerName): AuditNode`

### CourseMapper (port)

Methods:

- `map(CourseEntity course): AuditableCourse`

### AnalyzerRegistry (service)

Methods:

- `listAnalyzers(): List<AnalyzerDescriptor>`
- `getAnalyzerConfig(String analyzerName): Optional<Map<String,Object>>`

## Implementations

### CourseToAuditableMapper

**Implements:** CourseMapper

**Types:** Component

**Dependencies (constructor injection):**

- `nlpTokenizer`: `NlpTokenizer`

**Tests that must pass:**

- Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse → FEAT-NLP/F-NLP-R010
- Given a course with no milestones, when map is called, then returns an AuditableCourse without error → FEAT-NLP/F-NLP-R010
- Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates → FEAT-NLP/F-NLP-R008

### DefaultSentenceLengthConfig

**Implements:** SentenceLengthConfig

**Types:** Component

### DefaultAuditRunner

**Implements:** AuditRunner

**Types:** Service

**Dependencies (constructor injection):**

- `courseRepository`: `CourseRepository`
- `courseToAuditableMapper`: `CourseToAuditableMapper`
- `contentAudit`: `ContentAudit`
- `courseMapper`: `CourseMapper`
- `auditEngine`: `AuditEngine`

**Tests that must pass:**

- Given a valid course path, when runAudit is called, then returns the audit report from the full chain → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course → FEAT-CLI/F-CLI-R001
- Given courseRepository throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given contentAudit throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given a course with no milestones, when runAudit is called, then returns the report from contentAudit → FEAT-CLI/F-CLI-R001

### DefaultCocaBucketsConfig

**Implements:** CocaBucketsConfig

**Types:** Component

### DefaultLemmaRecurrenceConfig

**Implements:** LemmaRecurrenceConfig

**Types:** Component

### DefaultLemmaAbsenceConfig

**Implements:** LemmaAbsenceConfig

**Types:** Component

**Tests that must pass:**

- should have alert thresholds non-decreasing from high to low priority → FEAT-LABS/F-LABS-R014
- should enforce zero tolerance for high priority alert threshold → FEAT-LABS/F-LABS-R014
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero → FEAT-LABS/F-LABS-R021
- should have discount per level that limits max penalty to 0.3 for three-level distance → FEAT-LABS/F-LABS-R018
- should return non-negative values for all thresholds and bounds → FEAT-LABS/F-LABS-R021
- should return positive report limits for all priority levels → FEAT-LABS/F-LABS-R026
- should return percentage thresholds between 0 and 100 for all levels → FEAT-LABS/F-LABS-R021
- should return positive level weights for all CEFR levels → FEAT-LABS/F-LABS-R024
- should return discount per level between 0 exclusive and 1 exclusive → FEAT-LABS/F-LABS-R018
- should return absolute threshold 0 for A1 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 2 for A2 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 5 for B1 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 8 for B2 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 0.0 for A1 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 5.0 for A2 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 10.0 for B1 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 15.0 for B2 → FEAT-LABS/F-LABS-R021
- should return level weight 2.0 for A1 → FEAT-LABS/F-LABS-R024
- should return level weight 2.0 for A2 → FEAT-LABS/F-LABS-R024
- should return level weight 1.0 for B1 → FEAT-LABS/F-LABS-R024
- should return level weight 1.0 for B2 → FEAT-LABS/F-LABS-R024
- should return high priority bound of 1000 → FEAT-LABS/F-LABS-R011
- should return medium priority bound of 3000 → FEAT-LABS/F-LABS-R011
- should return low priority bound of 5000 → FEAT-LABS/F-LABS-R011
- should return high priority alert threshold of 0 → FEAT-LABS/F-LABS-R014
- should return medium priority alert threshold of 3 → FEAT-LABS/F-LABS-R014
- should return low priority alert threshold of 10 → FEAT-LABS/F-LABS-R014
- should return critical absence threshold of 10 → FEAT-LABS/F-LABS-R025
- should return acceptable absence threshold of 5 → FEAT-LABS/F-LABS-R025
- should return high report limit of 20 → FEAT-LABS/F-LABS-R026
- should return medium report limit of 30 → FEAT-LABS/F-LABS-R026
- should return low report limit of 50 → FEAT-LABS/F-LABS-R026
- should return discount per level of 0.1 → FEAT-LABS/F-LABS-R018
- should have absolute thresholds increasing from A1 to B2 → FEAT-LABS/F-LABS-R021
- should have percentage thresholds increasing from A1 to B2 → FEAT-LABS/F-LABS-R021
- should have priority bounds ordered high less than medium less than low → FEAT-LABS/F-LABS-R011
- should weight critical levels A1 and A2 higher than B1 and B2 → FEAT-LABS/F-LABS-R024
- should have report limits increasing from high to low priority → FEAT-LABS/F-LABS-R026
- should have critical absence threshold greater than acceptable absence threshold → FEAT-LABS/F-LABS-R025
- should have alert thresholds non-decreasing from high to low priority → FEAT-LABS/F-LABS-R014
- should enforce zero tolerance for high priority alert threshold → FEAT-LABS/F-LABS-R014
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero → FEAT-LABS/F-LABS-R021
- should have discount per level that limits max penalty to 0.3 for three-level distance → FEAT-LABS/F-LABS-R018
- should return non-negative values for all thresholds and bounds → FEAT-LABS/F-LABS-R021
- should return positive report limits for all priority levels → FEAT-LABS/F-LABS-R026
- should return percentage thresholds between 0 and 100 for all levels → FEAT-LABS/F-LABS-R021
- should return positive level weights for all CEFR levels → FEAT-LABS/F-LABS-R024
- should return discount per level between 0 exclusive and 1 exclusive → FEAT-LABS/F-LABS-R018
- should return coverage target 0.95 for A1 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.85 for A2 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.70 for B1 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.55 for B2 → FEAT-LABS/F-LABS-R032
- should have coverage targets decreasing from A1 to B2 → FEAT-LABS/F-LABS-R032
- should return coverage targets between 0 and 1 for all levels → FEAT-LABS/F-LABS-R032

### DefaultAnalyzerRegistry

**Implements:** AnalyzerRegistry

**Types:** Component

**Dependencies (constructor injection):**

- `analyzers`: `List<ContentAnalyzer>`
- `configs`: `List<SelfDescribingConfig>`

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

### From nlp-infrastructure

## Models

### NlpTokenizerConfig (`record`)

| Field | Type |
|-------|------|
| pythonScriptPath | `String` |
| cocaDataPath | `String` |
| timeoutSeconds | `int` |

### NlpTokenizerFactory (factory)

Methods:

- `create(NlpTokenizerConfig config): NlpTokenizer`

