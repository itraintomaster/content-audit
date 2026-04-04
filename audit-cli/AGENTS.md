<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-cli (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

CLI entry point for running content audits from the command line

## Models

### ReportViewModel (`record`)

| Field | Type |
|-------|------|
| overallScore | `double` |
| analyzerNames | `List<String>` |
| analyzerScores | `Map<String,Double>` |
| milestoneScores | `List<MilestoneScoreRow>` |

### MilestoneScoreRow (`record`)

| Field | Type |
|-------|------|
| milestoneId | `String` |
| analyzerScores | `Map<String,Double>` |
| overallScore | `double` |
| topicScores | `List<TopicScoreRow>` |
| entity | `AuditableEntity` |

### QuizScoreRow (`record`)

| Field | Type |
|-------|------|
| quizId | `String` |
| overallScore | `double` |
| analyzerScores | `Map<String,Double>` |
| entity | `AuditableEntity` |

### KnowledgeScoreRow (`record`)

| Field | Type |
|-------|------|
| knowledgeId | `String` |
| overallScore | `double` |
| analyzerScores | `Map<String,Double>` |
| quizScores | `List<QuizScoreRow>` |
| entity | `AuditableEntity` |

### TopicScoreRow (`record`)

| Field | Type |
|-------|------|
| topicId | `String` |
| overallScore | `double` |
| analyzerScores | `Map<String,Double>` |
| knowledgeScores | `List<KnowledgeScoreRow>` |
| entity | `AuditableEntity` |

### DrillDownScope (`record`)

| Field | Type |
|-------|------|
| level | `Optional<String>` |
| topic | `Optional<String>` |
| knowledge | `Optional<String>` |

### DrillDownLevel (`enum`)

| Field | Type |
|-------|------|
| COURSE | `null` |
| MILESTONE | `null` |
| TOPIC | `null` |
| KNOWLEDGE | `null` |

### DrillDownView (`record`)

| Field | Type |
|-------|------|
| depth | `DrillDownLevel` |
| nodeName | `String` |
| overallScore | `double` |
| analyzerScores | `Map<String,Double>` |
| analyzerNames | `List<String>` |
| childRows | `List<ScoreRow>` |

### ChildScoreRow (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| overallScore | `double` |
| analyzerScores | `Map<String,Double>` |
| entity | `AuditableEntity` |

### AnalyzerStatsView (`record`)

| Field | Type |
|-------|------|
| analyzerName | `String` |
| analyzerDescription | `String` |
| courseScore | `double` |
| levelScores | `Map<String,Double>` |
| worstItems | `List<ScoredItemRow>` |
| scoreDistribution | `Map<String,Integer>` |
| subMetricsByLevel | `Map<String,Map<String,Double>>` |
| itemCount | `int` |

## Interfaces

### ReportFormatter (port)

Methods:

- `format(ReportViewModel viewModel,DrillDownScope scope): String`

### FormatterRegistry (port)

Methods:

- `getFormatter(String formatName): ReportFormatter`

### ReportViewModelTransformer (port)

Methods:

- `transform(AuditReport report): ReportViewModel`

### RawReportFormatter (port)

Methods:

- `format(AuditReport report): String`

### DrillDownResolver (port) [sealed]

Methods:

- `resolve(ReportViewModel viewModel,DrillDownScope scope): DrillDownView`

### AnalyzerStatsTransformer (port)

Methods:

- `transform(AuditReport report,String analyzerName,AnalyzerRegistry registry): AnalyzerStatsView`

### ScoreRow (port)

Methods:

- `getEntity(): AuditableEntity`
- `getOverallScore(): double`
- `getAnalyzerScores(): Map<String,Double>`

### DetailedFormatter (formatter)

Methods:

- `format(String analyzerName,AuditNode rootNode,String outputFormat): String`

## Implementations

### TextReportFormatter

**Implements:** ReportFormatter

**Dependencies (constructor injection):**

- `drillDownResolver`: `DrillDownResolver`

### JsonReportFormatter

**Implements:** ReportFormatter

**Dependencies (constructor injection):**

- `drillDownResolver`: `DrillDownResolver`

### DefaultFormatterRegistry

**Implements:** FormatterRegistry

**Types:** Component

### DefaultReportViewModelTransformer

**Implements:** ReportViewModelTransformer

### TableReportFormatter

**Implements:** ReportFormatter

**Dependencies (constructor injection):**

- `drillDownResolver`: `DrillDownResolver`

### RawJsonReportFormatter

**Implements:** RawReportFormatter

### DefaultDrillDownResolver

**Implements:** DrillDownResolver

**Types:** Component

### DefaultAnalyzerStatsTransformer

**Implements:** AnalyzerStatsTransformer

**Types:** Component

### ContentAuditCmd

**Implements:** 

**Types:** Component

### AnalyzeCmd

**Implements:** 

**Types:** Component

**Dependencies (constructor injection):**

- `auditRunner`: `AuditRunner`
- `formatterRegistry`: `FormatterRegistry`
- `viewModelTransformer`: `ReportViewModelTransformer`
- `rawReportFormatter`: `RawReportFormatter`
- `drillDownResolver`: `DrillDownResolver`
- `detailedFormatters`: `Map<String,DetailedFormatter>`

### AnalyzerCmd

**Implements:** 

**Types:** Component

### AnalyzerListCmd

**Implements:** 

**Types:** Component

**Dependencies (constructor injection):**

- `analyzerRegistry`: `AnalyzerRegistry`

### AnalyzerConfigCmd

**Implements:** 

**Types:** Component

**Dependencies (constructor injection):**

- `analyzerRegistry`: `AnalyzerRegistry`

### AnalyzerStatsCmd

**Implements:** 

**Types:** Component

**Dependencies (constructor injection):**

- `analyzerRegistry`: `AnalyzerRegistry`
- `analyzerStatsTransformer`: `AnalyzerStatsTransformer`
- `auditRunner`: `AuditRunner`

### LemmaAbsenceDetailedFormatter

**Implements:** DetailedFormatter

**Types:** Component

**Tests that must pass:**

- should format text output from typed diagnoses matching previous metadata-based output → FEAT-DLABS/F-DLABS-R013
- should format json output from typed diagnoses matching previous metadata-based output → FEAT-DLABS/F-DLABS-R013
- should format table output from typed diagnoses matching previous metadata-based output → FEAT-DLABS/F-DLABS-R013
- should read typed diagnoses from course milestone and quiz nodes for formatting → FEAT-DLABS/F-DLABS-R004, F-DLABS-R005, F-DLABS-R009
- should handle missing diagnosis gracefully when analyzer did not produce results → FEAT-DLABS/F-DLABS-R003
- should navigate from quiz node to milestone ancestor to access level diagnosis → FEAT-DLABS/F-DLABS-R011, F-DLABS-R012
- should return empty when navigating to nonexistent ancestor level → FEAT-DLABS/F-DLABS-R011

### CocaBucketsDetailedFormatter

**Implements:** DetailedFormatter

**Types:** Component

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

### From audit-application

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
- `getResults(): List<ScoredItem>`
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

### LevelDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaAbsenceLevelDiagnosis>`

### TopicDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`

### KnowledgeDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`

### QuizDiagnoses (port)

Methods:

- `getLemmaAbsenceDiagnosis(): Optional<LemmaPlacementDiagnosis>`

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

