<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: vocabulary-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Infrastructure module for linguistic reference catalogs (EVP vocabulary profiles, COCA frequency data). Provides static lookup data for vocabulary analysis. Separate from NLP processing (which handles runtime tokenization).

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
| sentenceMode | `SentenceMode` |
| topicName | `String` |

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
| instructions | `String` |
| sentenceParts | `List<SentencePartEntity>` |

### CefrLevel (`enum`)

| Field | Type |
|-------|------|
| A1 | `null` |
| A2 | `null` |
| B1 | `null` |
| B2 | `null` |
| C1 | `null` |
| C2 | `null` |

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

### EvaluationRunPolicy (`record`)

| Field | Type |
|-------|------|
| maxNewEvaluations | `int` |
| reevaluate | `boolean` |
| reevaluationScope | `String` |

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

### LemmaAbsenceConfig (port)

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
- `getOutOfCatalogNoPenaltyRankBound(CefrLevel level): int`
- `getOutOfCatalogMildDiscountRankBound(CefrLevel level): int`
- `getOutOfCatalogMildDiscount(): double`
- `getOutOfCatalogStrongDiscount(): double`

### EvpCatalogPort (port)

Methods:

- `getExpectedLemmas(CefrLevel level): Set<LemmaAndPos>`
- `isPhrase(String lemma): boolean`
- `getCocaRank(LemmaAndPos lemmaAndPos): Optional<Integer>`
- `getSemanticCategory(LemmaAndPos lemmaAndPos): Optional<String>`
- `lookupLevel(LemmaAndPos lemmaAndPos): Optional<CefrLevel>`
- `lookupLevelByLemma(String lemma): Optional<CefrLevel>`

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
- `getQuizInstructionCoverage(): Optional<QuizInstructionCoverageDiagnosis>`

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
- `getQuizInstructionDiagnosis(): Optional<QuizInstructionDiagnosis>`

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

### EvaluationAnalyzerFactory (factory)

Methods:

- `create(EvaluationRunPolicy policy): ContentAnalyzer`
- `analyzerName(): String`

### QuizInstructionVerdictReader (port)

Methods:

- `read(String payload): QuizInstructionVerdict`

### QuizInstructionConfig (port)

Methods:

- `getDefaultMaxNewEvaluations(): int`
- `getScoreFor(InstructionSeverity severity): double`

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
| sentenceMode | `SentenceMode` |

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
| sentences | `List<String>` |

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

### SentenceMode (`enum`)

| Field | Type |
|-------|------|
| REWRITE | `null` |
| FILL | `null` |

### CourseRepository (port)

Methods:

- `load(Path path): CourseEntity`
- `save(CourseEntity course, Path path): void`

### CourseValidator (service)

Methods:

- `validate(CourseEntity course): void`

### From evaluation-ledger-domain

## Models

### EvaluationKey (`record`)

| Field | Type |
|-------|------|
| evaluatorId | `String` |
| contentFingerprint | `String` |

### EvaluationSubject (`record`)

| Field | Type |
|-------|------|
| subjectRef | `String` |
| content | `Map<String,String>` |

### EvaluationRecord (`record`)

| Field | Type |
|-------|------|
| key | `EvaluationKey` |
| payload | `String` |
| subjectRef | `String` |
| recordedAt | `Instant` |
| evaluatorVersion | `String` |

### EvaluationEmitted (`record`)

| Field | Type |
|-------|------|
| payload | `String` |

### EvaluationNotEmitted (`record`)

| Field | Type |
|-------|------|
| kind | `EvaluationFailureKind` |
| reason | `String` |

### EvaluationFailureKind (`enum`)

| Field | Type |
|-------|------|
| OUTPUT_UNUSABLE | `null` |
| EVALUATOR_UNAVAILABLE | `null` |

### EvaluationResolutionKind (`enum`)

| Field | Type |
|-------|------|
| REUSED | `null` |
| EVALUATED | `null` |
| PENDING | `null` |
| FAILED | `null` |

### EvaluationResolution (`record`)

| Field | Type |
|-------|------|
| kind | `EvaluationResolutionKind` |
| payload | `String` |
| evaluatorVersion | `String` |

### EvaluationCoverage (`record`)

| Field | Type |
|-------|------|
| reached | `int` |
| withResult | `int` |
| evaluatedInRun | `int` |
| reused | `int` |
| pending | `int` |
| failed | `int` |

### EvaluationBudget (`record`)

| Field | Type |
|-------|------|
| maxNewEvaluations | `int` |

### EvaluationOutcome (port)

### EvaluationLedger (port)

Methods:

- `append(EvaluationRecord record): void`
- `findLatest(EvaluationKey key): Optional<EvaluationRecord>`
- `history(EvaluationKey key): List<EvaluationRecord>`

### ContentFingerprinter (port)

Methods:

- `fingerprint(Map<String,String> content): String`

### Evaluator (port)

Methods:

- `evaluatorId(): String`
- `evaluate(EvaluationSubject subject): EvaluationOutcome`
- `evaluatorVersion(): Optional<String>`

### EvaluationSession (port)

Methods:

- `resolve(EvaluationSubject subject): EvaluationResolution`
- `resolveForced(EvaluationSubject subject): EvaluationResolution`
- `coverage(): EvaluationCoverage`
- `consultedEvaluatorVersion(): Optional<String>`

### EvaluationSessionFactory (factory)

Methods:

- `open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession`

