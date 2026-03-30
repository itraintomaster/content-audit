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

### ScoredItemRow (`record`)

| Field | Type |
|-------|------|
| milestoneId | `String` |
| topicId | `String` |
| knowledgeId | `String` |
| quizId | `String` |
| score | `double` |
| label | `String` |

## Interfaces

### ReportFormatter (port)

Methods:

- `format(ReportViewModel viewModel,DrillDownScope scope): String`

### AuditCli (port) [sealed]

Methods:

- `run(String[] args): int`
- `call(): Integer`

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

### AnalyzerStatsTransformer (port) [sealed]

Methods:

- `transform(AuditReport report,String analyzerName,AnalyzerRegistry registry): AnalyzerStatsView`

### ScoreRow (port)

Methods:

- `getEntity(): AuditableEntity`
- `getOverallScore(): double`
- `getAnalyzerScores(): Map<String,Double>`

## Implementations

### TextReportFormatter

**Implements:** ReportFormatter

**Dependencies (constructor injection):**

- `drillDownResolver`: `DrillDownResolver`

### JsonReportFormatter

**Implements:** ReportFormatter

**Dependencies (constructor injection):**

- `drillDownResolver`: `DrillDownResolver`

### DefaultAuditCli

**Implements:** AuditCli

**Dependencies (constructor injection):**

- `auditRunner`: `AuditRunner`
- `formatterRegistry`: `FormatterRegistry`
- `viewModelTransformer`: `ReportViewModelTransformer`
- `rawReportFormatter`: `RawReportFormatter`
- `analyzerRegistry`: `AnalyzerRegistry`
- `analyzerStatsTransformer`: `AnalyzerStatsTransformer`

**Tests that must pass:**

- Given valid args with course path, when run is called, then returns exit code 0 → F-CLI/F-CLI-R004
- Given no args provided, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R002
- Given auditRunner throws RuntimeException, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R004
- Given valid args with --format json, when run is called, then json formatter is looked up and returns 0 → F-CLI/F-CLI-R003
- Given valid args without --format, when run is called, then text formatter is used by default and returns 0 → F-CLI/F-CLI-R003
- Given valid args, when run is called, then auditRunner runAudit is invoked with course path → F-CLI/F-CLI-R001
- Given an unsupported format value, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R003
- Given valid args and low audit scores, when run is called, then returns 0 regardless of score values → F-CLI/F-CLI-R004

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

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

### From audit-application

### AuditRunner (service)

Methods:

- `runAudit(Path coursePath): AuditReport`

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
| overallScore | `double` |
| scores | `NodeScores` |
| milestones | `List<MilestoneNode>` |

### AuditableCourse (`record`)

| Field | Type |
|-------|------|
| milestones | `List<AuditableMilestone>` |

### AuditContext (`record`)

| Field | Type |
|-------|------|
| milestoneId | `String` |
| topicId | `String` |
| knowledgeId | `String` |
| quizId | `String` |
| topicLabel | `String` |
| knowledgeLabel | `String` |
| quizLabel | `String` |

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

### ScoredItem (`record`)

| Field | Type |
|-------|------|
| analyzerName | `String` |
| target | `AuditTarget` |
| score | `double` |
| milestoneId | `String` |
| topicId | `String` |
| knowledgeId | `String` |
| quizId | `String` |
| source | `AuditableEntity` |

### NodeScores (`record`)

| Field | Type |
|-------|------|
| scores | `Map<String,Double>` |

### QuizNode (`record`)

| Field | Type |
|-------|------|
| quizId | `String` |
| scores | `NodeScores` |
| entity | `AuditableEntity` |

### KnowledgeNode (`record`)

| Field | Type |
|-------|------|
| knowledgeId | `String` |
| scores | `NodeScores` |
| quizzes | `List<QuizNode>` |
| entity | `AuditableEntity` |

### TopicNode (`record`)

| Field | Type |
|-------|------|
| topicId | `String` |
| scores | `NodeScores` |
| knowledges | `List<KnowledgeNode>` |
| entity | `AuditableEntity` |

### MilestoneNode (`record`)

| Field | Type |
|-------|------|
| milestoneId | `String` |
| scores | `NodeScores` |
| topics | `List<TopicNode>` |
| entity | `AuditableEntity` |

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

### ContentAudit (service)

Methods:

- `audit(AuditableCourse): AuditReport`

### AuditEngine (port)

Methods:

- `runAudit(AuditableCourse): AuditReport`

### ContentAnalyzer (port)

Methods:

- `onKnowledge(AuditableKnowledge knowledge,AuditContext ctx): Void`
- `onQuiz(AuditableQuiz quiz,AuditContext ctx): Void`
- `onMilestone(AuditableMilestone milestone,AuditContext ctx): Void`
- `onTopic(AuditableTopic topic,AuditContext ctx): Void`
- `onCourseComplete(AuditableCourse course,AuditContext ctx): Void`
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

- `aggregate(List<ScoredItem> scores,Map<String,AuditableEntity> entityMap): AuditReport`

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

