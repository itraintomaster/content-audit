<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-cli (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

CLI entry point

## Models

### GetTasksFilter (`record`)

| Field | Type |
|-------|------|
| planId | `Optional<String>` |
| status | `Optional<String>` |
| sortByPriority | `boolean` |
| limit | `Optional<Integer>` |
| target | `Optional<AuditTarget>` |
| diagnosisKind | `Optional<DiagnosisKind>` |
| findingFilter | `TaskFindingFilter` |

### LagenMode (`enum`)

| Field | Type |
|-------|------|
| LLM | `null` |
| CANNED | `null` |

### PlanStorageMode (`enum`)

| Field | Type |
|-------|------|
| DISK | `null` |
| EPHEMERAL | `null` |

### EphemeralRenderOptions (`record`)

| Field | Type |
|-------|------|
| withCorrectionContext | `boolean` |

### SuggestedLemmasFilter (`record`)

| Field | Type |
|-------|------|
| limit | `Optional<Integer>` |
| partOfSpeech | `Optional<String>` |
| level | `Optional<CefrLevel>` |
| includeExposed | `boolean` |
| regex | `Optional<String>` |

### AnalyzeOptions (`record`)

| Field | Type |
|-------|------|
| format | `String` |
| level | `String` |
| topic | `String` |
| knowledge | `String` |
| analyzers | `List<String>` |
| excludeAnalyzers | `List<String>` |
| detailed | `boolean` |
| instructionBudget | `Integer` |
| reevaluateInstructions | `String` |
| reevaluateInstructionQuizIds | `Set<String>` |
| analyzerBudgets | `Map<String,Integer>` |
| analyzerReevaluationSets | `Map<String,Set<String>>` |
| maxConcurrentEvaluations | `Integer` |

### TaskFindingFilter (`record`)

| Field | Type |
|-------|------|
| analyzer | `Optional<String>` |
| resolution | `Optional<FindingResolution>` |
| severity | `Optional<FindingSeverity>` |
| level | `Optional<CefrLevel>` |
| knowledgeId | `Optional<String>` |

## Interfaces

### AnalyzeCommand (port)

Methods:

- `analyze(String coursePath, String format, String level, String topic, String knowledge, List<String> analyzers, boolean detailed): Integer`
- `analyze(String coursePath,AnalyzeOptions options): Integer`

### GetCommand (port) [sealed]

Methods:

- `get(String resource, String name, GetTasksFilter filter): Integer`
- `get(String resource, String name, SuggestedLemmasFilter filter): Integer`

### DeleteCommand (port) [sealed]

Methods:

- `delete(String resource, String id): Integer`

### PruneCommand (port) [sealed]

Methods:

- `prune(String resource, int keep): Integer`

### PlanCommand (port) [sealed]

Methods:

- `plan(String auditId, PlanStorageMode storageMode, boolean withCorrectionContext): Integer`

### ReviseCommand (port)

Methods:

- `revise(String taskId, String planId, String correctionContextJson, String correctionContextFilePath): Integer`

### ConfigAnalyzerCommand (port) [sealed]

Methods:

- `showConfig(String analyzerName): Integer`

### StatsAnalyzerCommand (port) [sealed]

Methods:

- `showStats(String analyzerName, String coursePath): Integer`

### ApproveCommand (port) [sealed]

Methods:

- `approve(String resource, String proposalId, String planId, String note): Integer`

### RejectCommand (port) [sealed]

Methods:

- `reject(String resource, String proposalId, String planId, String reason): Integer`

### GetConsolidatedCommand (port) [sealed]

Methods:

- `getConsolidated(String coursePath, String format): Integer`
- `getConsolidated(String coursePath, String format, Map<String,Integer> analyzerBudgets): Integer`

### SetActiveAnalysisCommand (port) [sealed]

Methods:

- `setActive(String auditId, String planId): Integer`

### LexisCommand (port) [sealed]

Methods:

- `queryLexis(String quizSentence, String mode, String level): Integer`

### RepairCommand (port) [sealed]

Methods:

- `repair(String resource, String coursePath, boolean dryRun): Integer`

### ReviseInstructionsCommand (port) [sealed]

Methods:

- `reviseInstructions(String planId, Integer maxCorrections, String coursePath): Integer`

### AssessCandidateCommand (port) [sealed]

Methods:

- `assessCandidate(String planId, String taskId, String candidateQuizSentence, String candidateTranslation, String coursePath, String format): Integer`

### ExamCommand (port) [sealed]

Methods:

- `takeExam(String judgeName,String casesFile,String photoCourse,Integer budget,Integer concurrency,String verdictsOut): Integer`
- `recordExam(String resultFile): Integer`
- `showExams(String judgeName): Integer`

### SetBaselineCommand (port) [sealed]

Methods:

- `setBaseline(String auditId): Integer`
- `showBaseline(String format): Integer`

### SaveProjectionCommand (port) [sealed]

Methods:

- `saveProjection(String coursePath, List<String> proposalIds, Map<String,Integer> analyzerBudgets, String format): Integer`

### CompareCommand (port) [sealed]

Methods:

- `compare(String coursePath, List<String> proposalIds, Map<String,Integer> analyzerBudgets, String format): Integer`

### GoalsCommand (port) [sealed]

Methods:

- `listGoals(String format): Integer`
- `setGoals(String format): Integer`

### BlindSampleCommand (port) [sealed]

Methods:

- `draw(String coursePath, List<String> proposalIds, Long seed, String format): Integer`
- `show(String sampleId, String format): Integer`
- `answer(String sampleId, int pairIndex, String answer, String reviewer): Integer`
- `close(String sampleId, String format): Integer`

### ErrorRateCommand (port) [sealed]

Methods:

- `draw(String coursePath, String level, int size, Long seed, String format): Integer`
- `show(String sampleId, String format): Integer`
- `mark(String sampleId, int itemIndex, boolean hasError, String reviewer): Integer`
- `close(String sampleId, String format): Integer`

### BatchCommand (port) [sealed]

Methods:

- `openBatch(String level, Integer topicsAtOnce, Integer queriesAtOnce, List<String> convertToMultipleChoice): Integer`
- `estimateBatch(): Integer`
- `runBatch(Integer queryCap): Integer`
- `closeBatch(): Integer`
- `dropFromBatch(String changeId, String ruleId, String reason): Integer`
- `reportBatch(String batchId, String format): Integer`

### TrayCommand (port) [sealed]

Methods:

- `showTray(String format): Integer`
- `answerTray(String itemId, String answer, Integer pairIndex, String changeId, String fixFile, String note): Integer`
- `publishTray(String note): Integer`

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

### From audit-application

## Models

### AuditRunRequest (`record`)

| Field | Type |
|-------|------|
| includedAnalyzers | `Set<String>` |
| excludedAnalyzers | `Set<String>` |
| analyzerPolicies | `Map<String,EvaluationRunPolicy>` |

### JudgeExamRequest (`record`)

| Field | Type |
|-------|------|
| judgeName | `String` |
| photoCourse | `Path` |
| caseIds | `Set<String>` |
| maxNewEvaluations | `int` |
| maxConcurrentEvaluations | `Integer` |

### JudgeExamTaken (`record`)

| Field | Type |
|-------|------|
| judgeName | `String` |
| judgeVersion | `String` |
| verdicts | `List<JudgeExamVerdict>` |
| coverage | `JudgeCoverageDiagnosis` |
| recordedAsTaken | `boolean` |

### JudgeExamNotTakenException (`exception`)

**Extends:** `IllegalArgumentException`

**Message:** `La versión %2$s del juez '%1$s' no rindió el examen completo: no se puede registrar su resultado`

| Field | Type |
|-------|------|
| judgeName | `String` |
| judgeVersion | `String` |

### AuditRunner (service)

Methods:

- `runAudit(Path coursePath,Set<String> analyzerNames): AuditReport`
- `runDetailedAudit(Path coursePath,String analyzerName): AuditNode`
- `runAudit(Path coursePath,AuditRunRequest request): AuditReport`
- `runDetailedAudit(Path coursePath,AuditRunRequest request): AuditNode`

### AnalyzerRegistry (service)

Methods:

- `listAnalyzers(): List<AnalyzerDescriptor>`
- `getAnalyzerConfig(String analyzerName): Optional<Map<String,Object>>`

### LemmaCountConfigLoader (port)

Methods:

- `load(String rawThreshold): LemmaCountConfig`

### JudgeExamService (service)

Methods:

- `take(JudgeExamRequest request): JudgeExamTaken`
- `record(JudgeExamRecord measured): JudgeExamStanding` throws JudgeExamNotTakenException
- `history(String judgeName): List<JudgeExamRecord>`

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
| multipleChoice | `MultipleChoiceEntity` |
| miniTheory | `String` |
| answerAudioKey | `String` |

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
| question | `String` |
| reads | `String` |
| rules | `List<AnalyzerRuleCard>` |
| goal | `String` |
| family | `AnalyzerFamily` |
| evaluatedTargets | `List<AuditTarget>` |
| resolutions | `List<FindingResolution>` |
| cost | `AnalysisCost` |
| displayName | `String` |
| tools | `List<LocalToolCard>` |

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
| findings | `List<Finding>` |
| numbers | `ContextNumbers` |
| unevaluatedBy | `List<String>` |
| partiallyEvaluatedBy | `List<String>` |
| measures | `List<AnalyzerMeasure>` |

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
| reevaluationSubjectIds | `Set<String>` |
| purpose | `EvaluationRunPurpose` |
| maxConcurrentEvaluations | `Integer` |

### EmptyReevaluationSetException (`exception`)

**Extends:** `IllegalArgumentException`

**Message:** `El pedido de re-evaluacion para el analisis '%s' declara un conjunto vacio de ejercicios: no se puede interpretar el alcance y la corrida no se ejecuta`

| Field | Type |
|-------|------|
| analyzerName | `String` |

### AmbiguousReevaluationScopeException (`exception`)

**Extends:** `IllegalArgumentException`

**Message:** `El pedido de re-evaluacion para el analisis '%s' declara a la vez un conjunto de ejercicios y otro alcance (%s): no se puede determinar el alcance`

| Field | Type |
|-------|------|
| analyzerName | `String` |
| declaredScope | `String` |

### EvaluationRunPurpose (`enum`)

| Field | Type |
|-------|------|
| COURSE | `null` |
| EXAM | `null` |

### LemmaCountBand (`record`)

| Field | Type |
|-------|------|
| name | `String` |
| maxCocaRank | `Integer` |
| threshold | `int` |

### NlpResourcesUnavailableException (`exception`)

**Extends:** `RuntimeException`

**Message:** `Falta %s en '%s': %s`

| Field | Type |
|-------|------|
| resource | `String` |
| path | `String` |
| detail | `String` |

### Baseline (`record`)

| Field | Type |
|-------|------|
| auditId | `String` |
| fixedAt | `Instant` |

### AuditEngine (port)

Methods:

- `runAudit(AuditableCourse course): AuditReport`
- `runAudit(AuditableCourse course, AnalyzerRunSelection selection): AuditReport` throws UnknownAnalyzerException

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
- `findingsAt(AuditNode node): List<FindingDraft>`

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
- `getJudgeCoverages(): List<JudgeCoverageDiagnosis>`
- `getLemmaRecurrenceDiagnosis(): Optional<LemmaRecurrenceCourseDiagnosis>`
- `getLocalToolRuns(): List<LocalToolRunDiagnosis>`
- `getNaturalnessQueue(): Optional<NaturalnessQueueDiagnosis>`

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
- `getHintCoherenceDiagnosis(): Optional<HintCoherenceDiagnosis>`
- `getAnswerCompletenessDiagnosis(): Optional<AnswerCompletenessDiagnosis>`
- `getMultipleChoiceValidityDiagnosis(): Optional<MultipleChoiceValidityDiagnosis>`
- `getDataCoherenceDiagnosis(): Optional<DataCoherenceDiagnosis>`
- `getExactDuplicateDiagnosis(): Optional<ExactDuplicateDiagnosis>`
- `getTranslationSenseDiagnosis(): Optional<TranslationSenseDiagnosis>`
- `getSingleAnswerDiagnosis(): Optional<SingleAnswerDiagnosis>`
- `getMiniTheoryDiagnosis(): Optional<MiniTheoryDiagnosis>`
- `getOffLevelWordsDiagnosis(): Optional<OffLevelWordsDiagnosis>`
- `getGrammarDiagnosis(): Optional<GrammarDiagnosis>`
- `getNaturalnessDiagnosis(): Optional<NaturalnessDiagnosis>`
- `getNearDuplicateDiagnosis(): Optional<NearDuplicateDiagnosis>`

### AuditReportStore (port)

Methods:

- `save(AuditReport report): String`
- `load(String id): Optional<AuditReport>`
- `loadLatest(): Optional<AuditReport>`
- `list(): List<AuditReportSummary>`
- `loadDigest(String id): Optional<AuditDigest>`

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
- `getBands(): List<LemmaCountBand>`

### QuizInstructionVerdictReader (port)

Methods:

- `read(String payload): QuizInstructionVerdict`

### QuizInstructionConfig (port)

Methods:

- `getDefaultMaxNewEvaluations(): int`
- `getScoreFor(InstructionSeverity severity): double`

### AnalyzerProvider (factory)

Methods:

- `analyzerName(): String`
- `describe(): AnalyzerDescriptor`
- `create(EvaluationRunPolicy policy): ContentAnalyzer`
- `planBinding(): Optional<AnalyzerPlanBinding>`
- `config(): Optional<SelfDescribingConfig>`

### AnalyzerCatalog (port)

Methods:

- `list(): List<AnalyzerDescriptor>`
- `find(String analyzerName): Optional<AnalyzerDescriptor>`
- `provider(String analyzerName): Optional<AnalyzerProvider>`
- `planBinding(String analyzerName): Optional<AnalyzerPlanBinding>`
- `measure(String analyzerName): Optional<AnalyzerMeasure>`

### FindingCollector (port)

Methods:

- `collect(AuditNode root, ContentAnalyzer analyzer, AnalyzerDescriptor card): void` throws FindingContractViolationException

### ContextNumbersCalculator (port)

Methods:

- `compute(AuditNode root, List<AnalyzerDescriptor> analyzers): void`

### QuizStudentViewRenderer (port)

Methods:

- `render(AuditableQuiz quiz): QuizStudentView`
- `finishedSentence(AuditableQuiz quiz,SentenceMode mode): String`

### TopicStudentViewQuery (port)

Methods:

- `viewOf(AuditReport report,String knowledgeId): TopicStudentView` throws StudentViewKnowledgeNotFoundException

### IrregularFormsCatalog (port)

Methods:

- `formsOf(String baseWord): Set<String>`
- `isPastParticiple(String word): boolean`

### CorrectorMatcher (port)

Methods:

- `canonicalForms(String text): Set<String>`
- `accepts(GapContext context,List<String> acceptedOptions,String written): boolean`
- `sameAnswer(String first,String second): boolean`
- `sameAnswer(GapContext context,String first,String second): boolean`
- `tableVersion(): String`

### FormEquivalenceTableSource (port)

Methods:

- `load(): FormEquivalenceTable`

### AudioRecordingRegistry (port)

Methods:

- `recordingOf(String audioKey): Optional<AudioRecording>`
- `version(): String`

### ProductionSentenceSource (port)

Methods:

- `savedSentences(String quizId): Optional<List<String>>`

### JudgeExamRegistry (port)

Methods:

- `record(JudgeExamRecord record): void`
- `latest(String judgeName,String judgeVersion): Optional<JudgeExamRecord>`
- `history(String judgeName): List<JudgeExamRecord>`

### JudgeRuntimeConfig (port)

Methods:

- `getMaxConcurrentEvaluations(): int`
- `getDefaultMaxNewEvaluations(String judgeName): int`
- `getDefaultRatePerHour(String judgeName): double`

### TranslationSenseConfig (port)

Methods:

- `getScoreFor(FindingSeverity severity): double`

### SingleAnswerConfig (port)

Methods:

- `getScoreFor(FindingSeverity severity): double`

### MiniTheoryConfig (port)

Methods:

- `getErrorScope(): MiniTheoryErrorScope`
- `getScoreFor(FindingSeverity severity): double`

### ChoiceAlternativeConfig (port)

Methods:

- `getScoreFor(FindingSeverity severity): double`

### BaselineStore (port)

Methods:

- `read(): Optional<Baseline>`
- `write(Baseline baseline): void`

### AnalyzerCorrection (port)

Methods:

- `analyzerName(): String`
- `editScope(FindingResolution resolution): List<ExercisePart>`
- `coveringAnalyzer(): Optional<String>`
- `groupsByTopic(): boolean`
- `tieRules(): List<String>`
- `offersTwoFixes(): boolean`
- `concernsAudio(Finding finding): boolean`

### AnalyzerCorrectionCatalog (port)

Methods:

- `find(String analyzerName): Optional<AnalyzerCorrection>`
- `list(): List<AnalyzerCorrection>`

### RuleFix (port)

Methods:

- `analyzerName(): String`
- `fix(RuleFixRequest request): RuleFixResult`

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
| unmodeledFields | `Map<String,Object>` |

### FormEntity (`record`)

| Field | Type |
|-------|------|
| kind | `String` |
| incidence | `double` |
| label | `String` |
| name | `String` |
| sentenceParts | `List<SentencePartEntity>` |
| multipleChoice | `MultipleChoiceEntity` |
| unmodeledFields | `Map<String,Object>` |

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

### FormKind (`enum`)

| Field | Type |
|-------|------|
| CLOZE | `null` |
| MULTIPLE_CHOICE | `null` |
| OTHER | `null` |

### MultipleChoiceItemEntity (`record`)

| Field | Type |
|-------|------|
| id | `String` |
| incidence | `double` |
| label | `String` |

### MultipleChoiceEntity (`record`)

| Field | Type |
|-------|------|
| selection | `String` |
| items | `List<MultipleChoiceItemEntity>` |

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
| failure | `EvaluationNotEmitted` |

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

### EvaluationRequestMode (`enum`)

| Field | Type |
|-------|------|
| REUSE_OR_EVALUATE | `null` |
| FORCE | `null` |
| PREFER_NEW | `null` |
| REUSE_ONLY | `null` |

### EvaluationRequest (`record`)

| Field | Type |
|-------|------|
| subject | `EvaluationSubject` |
| mode | `EvaluationRequestMode` |

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
- `resolvePreferringNew(EvaluationSubject subject): EvaluationResolution`
- `resolveWithoutConsulting(EvaluationSubject subject): EvaluationResolution`
- `resolveAll(List<EvaluationRequest> requests,int maxConcurrent): List<EvaluationResolution>`
- `recorded(EvaluationSubject subject): boolean`
- `unavailableReason(): Optional<String>`

### EvaluationSessionFactory (factory)

Methods:

- `open(EvaluationBudget budget,Evaluator evaluator): EvaluationSession`

### EvaluationTimeline (port)

Methods:

- `recordedTimes(String evaluatorId): List<Instant>`

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
| QUIZ_INSTRUCTION | `null` |
| HINT_COHERENCE | `null` |
| ANSWER_COMPLETENESS | `null` |
| MULTIPLE_CHOICE_VALIDITY | `null` |
| DATA_COHERENCE | `null` |
| EXACT_DUPLICATES | `null` |
| TRANSLATION_SENSE | `null` |
| SINGLE_ANSWER | `null` |
| MINI_THEORY | `null` |
| GRAMMAR | `null` |
| NEAR_DUPLICATES | `null` |

### RefinementTaskStatus (`enum`)

| Field | Type |
|-------|------|
| PENDING | `null` |
| COMPLETED | `null` |
| SKIPPED | `null` |
| STALE | `null` |
| IN_BATCH | `null` |

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
| origin | `TaskOrigin` |

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
| lemmaCount | `Integer` |
| lemmaCountThreshold | `Integer` |
| isUnderexposed | `Boolean` |

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
| discount | `Double` |

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
| tokenCount | `Integer` |
| targetMin | `Integer` |
| targetMax | `Integer` |
| delta | `Integer` |
| lengthDirection | `LengthDirection` |
| sourceAuditId | `String` |
| sentenceMode | `SentenceMode` |
| exerciseQuizzes | `List<String>` |
| nodeId | `String` |
| scarceContentWords | `List<ScarceContentWord>` |
| outOfCatalogWords | `List<OutOfCatalogWordContext>` |

### LengthDirection (`enum`)

| Field | Type |
|-------|------|
| SHORTEN | `null` |
| LENGTHEN | `null` |
| KEEP_SAME | `null` |
| UNKNOWN | `null` |

### SuggestedLemmaQueryResult (`record`)

| Field | Type |
|-------|------|
| taskId | `String` |
| cefrLevel | `CefrLevel` |
| suggestedLemmas | `List<SuggestedLemma>` |

### SuggestedLemmaQueryRejectedException (`exception`)

**Extends:** `RuntimeException`

**Message:** `Suggested lemma query rejected for task %s: %s`

| Field | Type |
|-------|------|
| taskId | `String` |
| reason | `String` |

### SuggestedLemmaQueryCriteria (`record`)

| Field | Type |
|-------|------|
| limit | `Optional<Integer>` |
| partOfSpeech | `Optional<String>` |
| explicitLevel | `Optional<CefrLevel>` |
| includeExposed | `boolean` |
| regex | `Optional<String>` |

### ScarceContentWord (`record`)

| Field | Type |
|-------|------|
| lemma | `String` |
| pos | `String` |
| count | `Integer` |
| threshold | `Integer` |

### KnowledgeTitleCorrectionContext (`record`)

| Field | Type |
|-------|------|
| taskId | `String` |
| knowledgeId | `String` |
| currentTitle | `String` |
| currentInstructions | `String` |
| topicLabel | `String` |
| titleTargetMax | `Integer` |
| instructionsTargetMax | `Integer` |
| expectedTitleLanguage | `String` |
| expectedInstructionsLanguage | `String` |

### OutOfCatalogWordContext (`record`)

| Field | Type |
|-------|------|
| lemma | `String` |
| observedPos | `String` |
| frequencyRank | `Integer` |
| discount | `double` |

### QuizInstructionCorrectionContext (`record`)

| Field | Type |
|-------|------|
| taskId | `String` |
| nodeId | `String` |
| quizSentence | `String` |
| translation | `String` |
| sentence | `String` |
| knowledgeTitle | `String` |
| knowledgeInstructions | `String` |
| topicLabel | `String` |
| cefrLevel | `CefrLevel` |
| cefrLevelLabel | `String` |
| sentenceMode | `SentenceMode` |
| violations | `List<InstructionViolation>` |
| verdictReason | `String` |
| severity | `InstructionSeverity` |
| siblingQuizSentences | `List<String>` |
| sourceAuditId | `String` |
| planId | `String` |

### UnconvertedScoreCount (`record`)

| Field | Type |
|-------|------|
| analyzer | `String` |
| nodeCount | `int` |
| reason | `String` |

### TaskOrigin (`record`)

| Field | Type |
|-------|------|
| finding | `Finding` |
| cefrLevel | `CefrLevel` |
| knowledgeId | `String` |
| knowledgeLabel | `String` |
| reachedQuizIds | `List<String>` |
| coveredFindings | `List<Finding>` |

### PlanFindingTally (`record`)

| Field | Type |
|-------|------|
| analyzer | `String` |
| countingFindings | `int` |
| ownTasks | `int` |
| covered | `int` |
| unconverted | `int` |
| unconvertedReason | `String` |

### RefinerEngine (port)

Methods:

- `plan(AuditReport report, String auditId): RefinementPlan`
- `nextTask(RefinementPlan plan): Optional<RefinementTask>`
- `unconvertedScores(AuditReport report): List<UnconvertedScoreCount>`
- `findingTally(AuditReport report, RefinementPlan plan): List<PlanFindingTally>`

### RefinementPlanStore (port)

Methods:

- `save(RefinementPlan plan): String`
- `load(String id): Optional<RefinementPlan>`
- `loadLatest(): Optional<RefinementPlan>`

### CorrectionContextResolver<T extends CorrectionContext> (port)

Methods:

- `resolve(AuditReport report, RefinementTask task): Optional<T>`
- `resolveWithIndex(AuditNodeIndex nodeIndex, AuditReport report, RefinementTask task): Optional<T>`
- `supports(DiagnosisKind kind): boolean`

### CorrectionContext (port)

### SuggestedLemmaQueryPort (port)

Methods:

- `query(AuditReport report, RefinementTask task, SuggestedLemmaQueryCriteria criteria): SuggestedLemmaQueryResult`

### SuggestedLemmaQuerySession (port)

Methods:

- `requestMore(SuggestedLemmaQueryCriteria criteria): SuggestedLemmaQueryResult`

### SuggestedLemmaQuerySessionFactory (factory)

Methods:

- `bindForTask(String sourceAuditId, RefinementTask task): SuggestedLemmaQuerySession`

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
| OVERRIDE_INVALID | `null` |
| OVERRIDE_NOT_APPLICABLE | `null` |
| DIAGNOSIS_NOT_SUSTAINED | `null` |
| NO_ACCEPTABLE_CANDIDATE | `null` |
| MULTIPLE_CHOICE_UNSUPPORTED | `null` |

### CourseElementSnapshot (`record`)

| Field | Type |
|-------|------|
| nodeTarget | `AuditTarget` |
| nodeId | `String` |
| quiz | `QuizTemplateEntity` |
| knowledge | `KnowledgeEntity` |

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
| unmetCriteria | `List<CriterionVerdict>` |
| lengthMeasurement | `CandidateLengthMeasurement` |

### RevisionArtifact (`record`)

| Field | Type |
|-------|------|
| proposal | `RevisionProposal` |
| verdict | `RevisionVerdict` |
| rejectionReason | `String` |
| outcome | `RevisionOutcomeKind` |
| decidedAt | `Instant` |
| decisionNote | `String` |
| contextSource | `CorrectionContextSource` |
| contextOverridePayload | `String` |

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
| courseMapper | `CourseMapper` |
| auditEngine | `AuditEngine` |
| impactPreviewStore | `ImpactPreviewStore` |
| correctionContextOverrideParser | `CorrectionContextOverrideParser` |
| knowledgeTitleStrategyRegistry | `KnowledgeTitleProposalStrategyRegistry` |
| knowledgeTitleProposalDeriver | `KnowledgeTitleProposalDeriver` |
| quizInstructionStrategyRegistry | `QuizInstructionProposalStrategyRegistry` |
| candidateAssessor | `CandidateAssessor` |
| quizInstructionComplianceChecker | `QuizInstructionComplianceChecker` |
| quizInstructionSubjectViewFactory | `QuizInstructionSubjectViewFactory` |
| quizInstructionCorrectionConfig | `QuizInstructionCorrectionConfig` |

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
| PRESERVATION_VIOLATED | `null` |
| MULTIPLE_CHOICE_UNSUPPORTED | `null` |

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

### ConsolidatedViewBuilderConfig (`record`)

| Field | Type |
|-------|------|
| activeAnalysisSelectionStore | `ActiveAnalysisSelectionStore` |
| auditReportStore | `AuditReportStore` |
| refinementPlanStore | `RefinementPlanStore` |
| revisionArtifactStore | `RevisionArtifactStore` |
| courseRepository | `CourseRepository` |
| courseElementLocator | `CourseElementLocator` |
| courseMapper | `CourseMapper` |
| auditEngine | `AuditEngine` |
| nodeFieldDiffer | `NodeFieldDiffer` |

### CorrectionContextSource (`enum`)

| Field | Type |
|-------|------|
| DERIVED | `null` |
| OVERRIDE | `null` |

### CorrectionContextOverride (`record`)

| Field | Type |
|-------|------|
| context | `CorrectionContext` |
| rawPayload | `String` |

### OverrideRejectedException (`exception`)

**Extends:** `RuntimeException`

**Message:** `correctionContext override rejected: %s`

| Field | Type |
|-------|------|
| reason | `String` |

### DiagnosisNotSustainedException (`exception`)

**Extends:** `RuntimeException`

**Message:** `El diagnostico de consigna del ejercicio '%s' ya no se sostiene: la validacion vigente declara que cumple`

| Field | Type |
|-------|------|
| quizId | `String` |
| taskId | `String` |

### NoAcceptableCandidateException (`exception`)

**Extends:** `RuntimeException`

**Message:** `No se logro una correccion aceptable para el ejercicio '%s': reprobaron %s`

| Field | Type |
|-------|------|
| quizId | `String` |
| failedCriteria | `List<CorrectionCriterion>` |
| taskId | `String` |

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
- `alignQuizTitles(CourseEntity course, String knowledgeId): CourseEntity`

### RevisionEngine (port)

Methods:

- `revise(String planId, String taskId, Path coursePath, String overridePayload): RevisionOutcome`

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

- `derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate, SentenceMode mode): CourseElementSnapshot`

### ImpactPreviewStore (port)

Methods:

- `save(ImpactPreview preview): void`
- `findByProposalId(String proposalId): Optional<ImpactPreview>`

### CorrectionContextOverrideParser (port)

Methods:

- `parse(String rawJson, DiagnosisKind expectedDiagnosisKind, String expectedNodeId): CorrectionContextOverride`

### KnowledgeTitleProposalStrategy (port)

Methods:

- `id(): StrategyId`
- `handles(DiagnosisKind kind): boolean`
- `propose(RefinementTask task, KnowledgeTitleCorrectionContext context): KnowledgeTitleCandidate`

### KnowledgeTitleProposalStrategyRegistry (service) [sealed]

Methods:

- `active(): Optional<KnowledgeTitleProposalStrategy>`
- `byName(String name): Optional<KnowledgeTitleProposalStrategy>`
- `listAll(): List<StrategyId>`

### KnowledgeTitleProposalDeriver (service) [sealed]

Methods:

- `derive(CourseElementSnapshot before, KnowledgeTitleCandidate candidate): CourseElementSnapshot`

### PreservationFactory (factory)

Methods:

- `createCheck(): PreservationCheck`
- `createRepair(RevisionArtifactStore artifactStore): PreservationRepair`

### QuizInstructionProposalStrategy (port)

Methods:

- `id(): StrategyId`
- `handles(DiagnosisKind kind): boolean`
- `propose(RefinementTask task, QuizInstructionCorrectionContext context, int maxAttempts): QuizInstructionBestCandidate`

### QuizInstructionProposalStrategyRegistry (service) [sealed]

Methods:

- `active(): Optional<QuizInstructionProposalStrategy>`
- `byName(String name): Optional<QuizInstructionProposalStrategy>`
- `listAll(): List<StrategyId>`

### QuizInstructionCorrectionRunner (port)

Methods:

- `run(QuizInstructionCorrectionRunRequest request): QuizInstructionCorrectionRunReport`

### QuizInstructionCorrectionRunnerFactory (factory)

Methods:

- `create(RevisionEngineConfig config, QuizInstructionCorrectionRunStore runStore, QuizInstructionCorrectionConfig correctionConfig): QuizInstructionCorrectionRunner`

### QuizInstructionCorrectionRunStore (port)

Methods:

- `save(QuizInstructionCorrectionRunReport report): String`
- `load(String runId): Optional<QuizInstructionCorrectionRunReport>`
- `listByPlan(String planId): List<QuizInstructionCorrectionRunReport>`
- `latest(): Optional<QuizInstructionCorrectionRunReport>`

### ProjectionStore (port)

Methods:

- `save(CourseProjection projection): SavedProjection`
- `load(String projectionId): Optional<AuditReport>`

### GoalDeclarationSource (port)

Methods:

- `read(): GoalDeclaration`

### SealedGoalsStore (port)

Methods:

- `read(): Optional<SealedGoals>`
- `write(SealedGoals sealedGoals): void`

### BlindSampleStore (port)

Methods:

- `savePairs(BlindSample sample): void`
- `loadPairs(String sampleId): Optional<BlindSample>`
- `listPairs(): List<BlindSample>`
- `saveErrorRate(ErrorRateSample sample): void`
- `loadErrorRate(String sampleId): Optional<ErrorRateSample>`

### From agent-runtime-infrastructure

## Models

### AgentGraphRunnerConfig (`record`)

| Field | Type |
|-------|------|
| agentsBaseDir | `Path` |
| runsBaseDir | `Path` |

### AgentDefinitionFound (`record`)

| Field | Type |
|-------|------|
| directory | `Path` |

### AgentDefinitionUnresolved (`record`)

| Field | Type |
|-------|------|
| agentName | `String` |
| searchedIn | `Path` |

### AgentGraphRunner (port)

Methods:

- `run(String agentName,Map<String,String> inputs,ChatModel chatModel,Map<String,Object> tools): RunState`

### AgentGraphRunnerFactory (factory)

Methods:

- `create(AgentGraphRunnerConfig config): AgentGraphRunner`

### AgentDefinitionLocation

### AgentDefinitionLocator (port)

Methods:

- `locate(String agentName): AgentDefinitionLocation`
- `locateUnderProjectRoot(String projectRoot,String agentName): AgentDefinitionLocation`

### AgentDefinitionLocatorFactory (factory)

Methods:

- `create(AgentGraphRunnerConfig config): AgentDefinitionLocator`

### From quiz-instruction-infrastructure

## Models

### QuizInstructionJudgeConfig (`record`)

| Field | Type |
|-------|------|
| baseUrl | `String` |
| apiKey | `String` |
| modelName | `String` |
| temperature | `Double` |
| timeoutSeconds | `Integer` |
| agentName | `String` |
| providerName | `String` |

### AgentJudgeSpec (`record`)

| Field | Type |
|-------|------|
| evaluatorId | `String` |
| verdictArtifactKey | `String` |
| displayName | `String` |

### AgentJudgeConfig (`record`)

| Field | Type |
|-------|------|
| providerName | `String` |
| baseUrl | `String` |
| apiKey | `String` |
| modelName | `String` |
| temperature | `Double` |
| timeoutSeconds | `Integer` |

### JudgeUnavailabilityCause (`enum`)

| Field | Type |
|-------|------|
| SESSION_EXPIRED | `null` |
| QUOTA_EXHAUSTED | `null` |
| SERVICE_UNREACHABLE | `null` |
| TIMEOUT | `null` |
| NOT_INSTALLED | `null` |
| UNKNOWN | `null` |

### JudgeProviderReadiness (`record`)

| Field | Type |
|-------|------|
| ready | `boolean` |
| cause | `JudgeUnavailabilityCause` |
| detail | `String` |
| remedy | `String` |

### QuizInstructionJudgeFactory (factory)

Methods:

- `create(QuizInstructionJudgeConfig config,AgentGraphRunner agentGraphRunner,AgentDefinitionLocator agentDefinitionLocator): Evaluator`

### AgentJudgeFactory (factory)

Methods:

- `create(AgentJudgeSpec spec,AgentJudgeConfig config,AgentGraphRunner agentGraphRunner,AgentDefinitionLocator agentDefinitionLocator): Evaluator`

### JudgeProviderReadinessProbe (port)

Methods:

- `probe(LlmProviderResolved provider): JudgeProviderReadiness`
- `explain(LlmProviderResolved provider,String failure): JudgeProviderReadiness`

### From local-language-infrastructure

## Models

### OllamaEndpoint (`record`)

| Field | Type |
|-------|------|
| baseUrl | `String` |
| timeoutSeconds | `int` |

### OllamaModelFactory (factory)

Methods:

- `yesNoModel(OllamaEndpoint endpoint,String model): YesNoModel`
- `embeddingModel(OllamaEndpoint endpoint,String model): EmbeddingModel`

