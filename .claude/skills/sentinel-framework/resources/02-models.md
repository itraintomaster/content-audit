# Models

Models are immutable data carriers generated as Java classes with getters, setters, and constructors.
They represent the domain entities of the system and are declared in `sentinel.yaml` within modules.

## Key Rules

- Models are **generated** — marked with `@Generated`, do NOT modify them
- Each model produces a Java class in `<module>/src/main/java/<packagePrefix>/<module>/`
- Fields map to Java types: `String`, `int`, `long`, `double`, `float`, `boolean`, `UUID`, `List<T>`
- Constructor injection creates all-args constructors for record-like immutability

## Type Mapping Reference

| YAML Type | Java Type | Import Required |
|-----------|-----------|----------------|
| `String` | `String` | No |
| `int` / `Integer` | `int` / `Integer` | No |
| `long` / `Long` | `long` / `Long` | No |
| `double` / `Double` | `double` / `Double` | No |
| `float` / `Float` | `float` / `Float` | No |
| `boolean` / `Boolean` | `boolean` / `Boolean` | No |
| `UUID` | `java.util.UUID` | Yes |
| `List<T>` | `java.util.List<T>` | Yes |
| `Optional<T>` | `java.util.Optional<T>` | Yes |
| Custom (e.g. `Payment`) | Resolved from modules | Package-dependent |

## Declared Models

### Module: audit-domain

#### AuditReport

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `root` | `AuditNode` |  |

**Generated constructor:**
```java
new AuditReport(AuditNode root)
```

#### AuditableCourse

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `milestones` | `List<AuditableMilestone>` | Import `java.util.List` |

**Generated constructor:**
```java
new AuditableCourse(List<AuditableMilestone> milestones)
```

#### AuditableKnowledge

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `quizzes` | `List<AuditableQuiz>` | Import `java.util.List` |
| `title` | `String` |  |
| `instructions` | `String` |  |
| `isSentence` | `boolean` |  |
| `id` | `String` |  |
| `label` | `String` |  |
| `code` | `String` |  |
| `sentenceMode` | `SentenceMode` |  |
| `topicName` | `String` |  |

**Generated constructor:**
```java
new AuditableKnowledge(List<AuditableQuiz> quizzes, String title, String instructions, boolean isSentence, String id, String label, String code, SentenceMode sentenceMode, String topicName)
```

#### AuditableTopic

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `knowledge` | `List<AuditableKnowledge>` | Import `java.util.List` |
| `id` | `String` |  |
| `label` | `String` |  |
| `code` | `String` |  |

**Generated constructor:**
```java
new AuditableTopic(List<AuditableKnowledge> knowledge, String id, String label, String code)
```

#### AuditableMilestone

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `topics` | `List<AuditableTopic>` | Import `java.util.List` |
| `id` | `String` |  |
| `label` | `String` |  |
| `code` | `String` |  |

**Generated constructor:**
```java
new AuditableMilestone(List<AuditableTopic> topics, String id, String label, String code)
```

#### AuditableQuiz

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `tokens` | `List<NlpToken>` | Import `java.util.List` |
| `id` | `String` |  |
| `label` | `String` |  |
| `code` | `String` |  |
| `translation` | `String` |  |
| `sentences` | `List<String>` | Import `java.util.List` |
| `quizSentence` | `String` |  |
| `instructions` | `String` |  |
| `sentenceParts` | `List<SentencePartEntity>` | Import `java.util.List` |
| `multipleChoice` | `MultipleChoiceEntity` |  |
| `miniTheory` | `String` |  |
| `answerAudioKey` | `String` |  |

**Generated constructor:**
```java
new AuditableQuiz(List<NlpToken> tokens, String id, String label, String code, String translation, List<String> sentences, String quizSentence, String instructions, List<SentencePartEntity> sentenceParts, MultipleChoiceEntity multipleChoice, String miniTheory, String answerAudioKey)
```

#### CefrLevel

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `A1` | `null` |  |
| `A2` | `null` |  |
| `B1` | `null` |  |
| `B2` | `null` |  |
| `C1` | `null` |  |
| `C2` | `null` |  |

**Generated constructor:**
```java
new CefrLevel(null A1, null A2, null B1, null B2, null C1, null C2)
```

#### TargetRange

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `level` | `CefrLevel` |  |
| `minTokens` | `int` |  |
| `maxTokens` | `int` |  |

**Generated constructor:**
```java
new TargetRange(CefrLevel level, int minTokens, int maxTokens)
```

#### AuditTarget

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `QUIZ` | `null` |  |
| `KNOWLEDGE` | `null` |  |
| `TOPIC` | `null` |  |
| `MILESTONE` | `null` |  |
| `COURSE` | `null` |  |

**Generated constructor:**
```java
new AuditTarget(null QUIZ, null KNOWLEDGE, null TOPIC, null MILESTONE, null COURSE)
```

#### NlpToken

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `text` | `String` |  |
| `lemma` | `String` |  |
| `posTag` | `String` |  |
| `frequencyRank` | `Integer` |  |
| `isStop` | `boolean` |  |
| `isPunct` | `boolean` |  |

**Generated constructor:**
```java
new NlpToken(String text, String lemma, String posTag, Integer frequencyRank, boolean isStop, boolean isPunct)
```

#### AnalyzerDescriptor

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `name` | `String` |  |
| `description` | `String` |  |
| `target` | `AuditTarget` |  |
| `question` | `String` |  |
| `reads` | `String` |  |
| `rules` | `List<AnalyzerRuleCard>` | Import `java.util.List` |
| `goal` | `String` |  |
| `family` | `AnalyzerFamily` |  |
| `evaluatedTargets` | `List<AuditTarget>` | Import `java.util.List` |
| `resolutions` | `List<FindingResolution>` | Import `java.util.List` |
| `cost` | `AnalysisCost` |  |
| `displayName` | `String` |  |
| `tools` | `List<LocalToolCard>` | Import `java.util.List` |
| `explanation` | `String` |  |

**Generated constructor:**
```java
new AnalyzerDescriptor(String name, String description, AuditTarget target, String question, String reads, List<AnalyzerRuleCard> rules, String goal, AnalyzerFamily family, List<AuditTarget> evaluatedTargets, List<FindingResolution> resolutions, AnalysisCost cost, String displayName, List<LocalToolCard> tools, String explanation)
```

#### AuditNode

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `entity` | `AuditableEntity` |  |
| `target` | `AuditTarget` |  |
| `parent` | `AuditNode` |  |
| `children` | `List<AuditNode>` | Import `java.util.List` |
| `scores` | `Map<String,Double>` |  |
| `metadata` | `Map<String,Object>` |  |
| `diagnoses` | `NodeDiagnoses` |  |
| `findings` | `List<Finding>` | Import `java.util.List` |
| `numbers` | `ContextNumbers` |  |
| `unevaluatedBy` | `List<String>` | Import `java.util.List` |
| `partiallyEvaluatedBy` | `List<String>` | Import `java.util.List` |
| `measures` | `List<AnalyzerMeasure>` | Import `java.util.List` |

**Generated constructor:**
```java
new AuditNode(AuditableEntity entity, AuditTarget target, AuditNode parent, List<AuditNode> children, Map<String,Double> scores, Map<String,Object> metadata, NodeDiagnoses diagnoses, List<Finding> findings, ContextNumbers numbers, List<String> unevaluatedBy, List<String> partiallyEvaluatedBy, List<AnalyzerMeasure> measures)
```

#### SentenceLengthDiagnosis

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `tokenCount` | `int` |  |
| `targetMin` | `int` |  |
| `targetMax` | `int` |  |
| `cefrLevel` | `CefrLevel` |  |
| `delta` | `int` |  |
| `toleranceMargin` | `int` |  |

**Generated constructor:**
```java
new SentenceLengthDiagnosis(int tokenCount, int targetMin, int targetMax, CefrLevel cefrLevel, int delta, int toleranceMargin)
```

#### AuditReportSummary

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `timestamp` | `Instant` |  |
| `courseName` | `String` |  |
| `overallScore` | `double` |  |

**Generated constructor:**
```java
new AuditReportSummary(String id, Instant timestamp, String courseName, double overallScore)
```

#### ActiveAnalysisSelection

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `auditId` | `String` |  |
| `planId` | `String` |  |

**Generated constructor:**
```java
new ActiveAnalysisSelection(String auditId, String planId)
```

#### EvaluationRunPolicy

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `maxNewEvaluations` | `int` |  |
| `reevaluate` | `boolean` |  |
| `reevaluationScope` | `String` |  |
| `reevaluationSubjectIds` | `Set<String>` |  |
| `purpose` | `EvaluationRunPurpose` |  |
| `maxConcurrentEvaluations` | `Integer` |  |

**Generated constructor:**
```java
new EvaluationRunPolicy(int maxNewEvaluations, boolean reevaluate, String reevaluationScope, Set<String> reevaluationSubjectIds, EvaluationRunPurpose purpose, Integer maxConcurrentEvaluations)
```

#### EmptyReevaluationSetException

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** exception
**Extends:** `IllegalArgumentException`
**Message:** `"El pedido de re-evaluacion para el analisis '%s' declara un conjunto vacio de ejercicios: no se puede interpretar el alcance y la corrida no se ejecuta"`

| Field | Type | Notes |
|-------|------|-------|
| `analyzerName` | `String` |  |

**Generated class** extends `IllegalArgumentException` with constructor:
```java
new EmptyReevaluationSetException(String analyzerName)
```

#### AmbiguousReevaluationScopeException

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** exception
**Extends:** `IllegalArgumentException`
**Message:** `"El pedido de re-evaluacion para el analisis '%s' declara a la vez un conjunto de ejercicios y otro alcance (%s): no se puede determinar el alcance"`

| Field | Type | Notes |
|-------|------|-------|
| `analyzerName` | `String` |  |
| `declaredScope` | `String` |  |

**Generated class** extends `IllegalArgumentException` with constructor:
```java
new AmbiguousReevaluationScopeException(String analyzerName, String declaredScope)
```

#### EvaluationRunPurpose

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `COURSE` | `null` |  |
| `EXAM` | `null` |  |

**Generated constructor:**
```java
new EvaluationRunPurpose(null COURSE, null EXAM)
```

#### LemmaCountBand

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `name` | `String` |  |
| `maxCocaRank` | `Integer` |  |
| `threshold` | `int` |  |

**Generated constructor:**
```java
new LemmaCountBand(String name, Integer maxCocaRank, int threshold)
```

#### NlpResourcesUnavailableException

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"Falta %s en '%s': %s"`

| Field | Type | Notes |
|-------|------|-------|
| `resource` | `String` |  |
| `path` | `String` |  |
| `detail` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new NlpResourcesUnavailableException(String resource, String path, String detail)
```

#### Baseline

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `auditId` | `String` |  |
| `fixedAt` | `Instant` |  |

**Generated constructor:**
```java
new Baseline(String auditId, Instant fixedAt)
```

#### FrequencyBand (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `name` | `String` |
| `lowerBound` | `int` |
| `upperBound` | `int` |

#### BandConfiguration (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bands` | `List<FrequencyBand>` |
| `openEnded` | `boolean` |

#### AssessmentState (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `OPTIMAL` | `null` |
| `ADEQUATE` | `null` |
| `DEFICIENT` | `null` |
| `EXCESSIVE` | `null` |

#### TargetKind (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `AT_LEAST` | `null` |
| `AT_MOST` | `null` |

#### BucketTarget (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bandName` | `String` |
| `targetPercentage` | `double` |
| `kind` | `TargetKind` |

#### BucketResult (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bandName` | `String` |
| `count` | `int` |
| `percentage` | `double` |
| `targetPercentage` | `double` |
| `score` | `double` |
| `assessment` | `AssessmentState` |

#### QuarterBucketTargets (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `targets` | `List<BucketTarget>` |

#### QuarterResult (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `bucketResults` | `List<BucketResult>` |
| `score` | `double` |

#### LevelBucketDistribution (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `levelName` | `String` |
| `score` | `double` |
| `bucketResults` | `List<BucketResult>` |
| `quarterResults` | `List<QuarterResult>` |
| `topicDistributions` | `List<TopicBucketDistribution>` |

#### TopicBucketDistribution (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `topicId` | `String` |
| `score` | `double` |
| `bucketResults` | `List<BucketResult>` |
| `topicLabel` | `String` |

#### ProgressionState (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `ASCENDING` | `null` |
| `DESCENDING` | `null` |
| `STATIC` | `null` |
| `IRREGULAR` | `null` |

#### ProgressionExpectation (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bandName` | `String` |
| `expectedProgression` | `ProgressionState` |

#### ProgressionAssessment (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bandName` | `String` |
| `actualProgression` | `ProgressionState` |
| `expectedProgression` | `ProgressionState` |
| `matches` | `boolean` |

#### ImprovementDirectiveType (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `ENRICH` | `null` |
| `REDUCE` | `null` |

#### ImprovementDirective (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `type` | `ImprovementDirectiveType` |
| `bandName` | `String` |
| `levelName` | `String` |
| `frequencyRangeFrom` | `int` |
| `frequencyRangeTo` | `int` |
| `actualPercentage` | `double` |
| `targetPercentage` | `double` |

#### CocaBucketsDistributionResult (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `levels` | `List<LevelBucketDistribution>` |
| `progressionAssessments` | `List<ProgressionAssessment>` |
| `overallScore` | `double` |
| `improvementDirectives` | `List<ImprovementDirective>` |

#### AnalysisStrategy (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `LEVELS` | `null` |
| `QUARTERS` | `null` |

#### CocaProgressionDiagnosis (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `overallScore` | `double` |
| `progressionAssessments` | `List<ProgressionAssessment>` |
| `improvementDirectives` | `List<ImprovementDirective>` |

#### CocaBucketsLevelDiagnosis (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `totalTokens` | `int` |
| `buckets` | `List<BucketResult>` |
| `quarters` | `List<QuarterResult>` |

#### CocaBucketsTopicDiagnosis (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `totalTokens` | `int` |
| `buckets` | `List<BucketSummary>` |

#### BucketSummary (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `bandName` | `String` |
| `count` | `int` |
| `percentage` | `double` |

#### ExposureStatus (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `NORMAL` | `String` |
| `SUB_EXPOSED` | `String` |
| `OVER_EXPOSED` | `String` |

#### LemmaStats (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |
| `count` | `int` |
| `meanInterval` | `double` |
| `stdDevInterval` | `double` |
| `exposureStatus` | `ExposureStatus` |
| `occurrencePositions` | `List<Integer>` |
| `longestGap` | `int` |
| `longestGapFromLevel` | `CefrLevel` |
| `longestGapToLevel` | `CefrLevel` |
| `levels` | `List<CefrLevel>` |

#### ExposureSummary (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `normalCount` | `int` |
| `subExposedCount` | `int` |
| `overExposedCount` | `int` |

#### LemmaRecurrenceResult (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaStats` | `List<LemmaStats>` |
| `exposureSummary` | `ExposureSummary` |
| `overallScore` | `double` |

#### LevelRecurrence (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `positions` | `int` |
| `returningLemmas` | `int` |
| `normalCount` | `int` |
| `subExposedCount` | `int` |
| `overExposedCount` | `int` |
| `normalShare` | `double` |

#### LemmaRecurrenceCourseDiagnosis (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `result` | `LemmaRecurrenceResult` |
| `levels` | `List<LevelRecurrence>` |

#### AbsenceType (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `COMPLETELY_ABSENT` | `double` |
| `APPEARS_TOO_LATE` | `double` |
| `APPEARS_TOO_EARLY` | `double` |

#### PriorityLevel (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `HIGH` | `null` |
| `MEDIUM` | `null` |
| `LOW` | `null` |

#### LemmaAndPos (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |

#### AbsentLemma (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaAndPos` | `LemmaAndPos` |
| `expectedLevel` | `CefrLevel` |
| `absenceType` | `AbsenceType` |
| `presentInLevels` | `List<CefrLevel>` |
| `priorityLevel` | `PriorityLevel` |
| `cocaRank` | `Integer` |
| `semanticCategory` | `String` |

#### AbsenceAssessment (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `OPTIMAL` | `null` |
| `ACCEPTABLE` | `null` |
| `NEEDS_IMPROVEMENT` | `null` |

#### LevelAbsenceMetrics (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `totalExpected` | `int` |
| `totalAbsent` | `int` |
| `absencePercentage` | `double` |
| `absentLemmas` | `List<AbsentLemma>` |
| `score` | `double` |

#### RecommendationAction (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `ADD_VOCABULARY` | `null` |
| `INTRODUCE_EARLIER` | `null` |
| `REMOVE_FROM_LEVEL` | `null` |

#### EffortLevel (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `LOW` | `null` |
| `MEDIUM` | `null` |
| `HIGH` | `null` |

#### AbsenceRecommendation (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `action` | `RecommendationAction` |
| `description` | `String` |
| `priority` | `PriorityLevel` |
| `affectedLemmas` | `List<AbsentLemma>` |
| `effortLevel` | `EffortLevel` |
| `expectedImpact` | `double` |
| `targetLevel` | `CefrLevel` |

#### LemmaAbsenceCourseDiagnosis (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `assessment` | `AbsenceAssessment` |

#### LemmaAbsenceLevelDiagnosis (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `totalExpected` | `int` |
| `totalAbsent` | `int` |
| `absencePercentage` | `double` |
| `coverageTarget` | `double` |
| `completelyAbsentScore` | `double` |
| `tooLateScore` | `double` |
| `tooEarlyScore` | `double` |
| `assessment` | `AbsenceAssessment` |
| `absentLemmas` | `List<AbsentLemma>` |
| `misplacedLemmas` | `List<MisplacedLemma>` |
| `highPriorityCount` | `int` |
| `mediumPriorityCount` | `int` |
| `lowPriorityCount` | `int` |

#### LemmaPlacementDiagnosis (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `misplacedLemmaCount` | `int` |
| `misplacedLemmas` | `List<MisplacedLemma>` |
| `outOfCatalogWordCount` | `int` |
| `outOfCatalogWords` | `List<OutOfCatalogWord>` |

#### MisplacedLemma (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaAndPos` | `LemmaAndPos` |
| `expectedLevel` | `CefrLevel` |
| `foundInLevel` | `CefrLevel` |
| `absenceType` | `AbsenceType` |
| `cocaRank` | `Integer` |
| `semanticCategory` | `String` |
| `observedPos` | `String` |
| `discount` | `Double` |

#### OutOfCatalogWord (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `observedPos` | `String` |
| `frequencyRank` | `Integer` |
| `discount` | `double` |

#### SentenceLexicalScore (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `score` | `double` |
| `misplacedLemmas` | `List<MisplacedLemma>` |
| `outOfCatalogWords` | `List<OutOfCatalogWord>` |

#### LemmaCountStats (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaAndPos` | `LemmaAndPos` |
| `count` | `int` |
| `score` | `double` |
| `assignedLevel` | `Optional<CefrLevel>` |

#### LevelLemmaCountResult (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `score` | `double` |
| `totalLemmas` | `int` |
| `subExposedLemmas` | `List<LemmaCountStats>` |

#### UnassignedLemmaEntry (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaAndPos` | `LemmaAndPos` |
| `count` | `int` |

#### LemmaCountResult (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `thresholdN` | `int` |
| `courseScore` | `Optional<Double>` |
| `levels` | `List<LevelLemmaCountResult>` |
| `unassigned` | `List<UnassignedLemmaEntry>` |
| `bands` | `List<LemmaCountBand>` |
| `exposures` | `List<LemmaExposure>` |

#### LemmaCountCourseDiagnosis (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `result` | `LemmaCountResult` |

#### LemmaCountLevelDiagnosis (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `levelResult` | `LevelLemmaCountResult` |

#### LemmaExposure (package: lemmacount)

**Package:** `com.learney.contentaudit.auditdomain.lemmacount`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `lemmaAndPos` | `LemmaAndPos` |
| `assignedLevel` | `CefrLevel` |
| `count` | `int` |
| `cocaRank` | `Integer` |
| `band` | `String` |
| `threshold` | `int` |
| `score` | `double` |
| `knowledgeIds` | `List<String>` |

#### SentenceLexicalFlags (package: lexicalflags)

**Package:** `com.learney.contentaudit.auditdomain.lexicalflags`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `misplacedWords` | `List<MisplacedWordFlag>` |
| `outOfCatalogWords` | `List<OutOfCatalogFlag>` |

#### MisplacedWordFlag (package: lexicalflags)

**Package:** `com.learney.contentaudit.auditdomain.lexicalflags`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |
| `expectedLevel` | `CefrLevel` |
| `sentenceLevel` | `CefrLevel` |

#### OutOfCatalogFlag (package: lexicalflags)

**Package:** `com.learney.contentaudit.auditdomain.lexicalflags`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |
| `frequencyRank` | `Integer` |

#### InstructionSeverity (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `NONE` | `null` |
| `MINOR` | `null` |
| `MAJOR` | `null` |
| `CRITICAL` | `null` |

#### InstructionCheckStatus (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `PASSED` | `null` |
| `FAILED` | `null` |
| `NOT_APPLICABLE` | `null` |

#### InstructionViolation (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `code` | `String` |
| `constraint` | `String` |
| `evidence` | `String` |
| `explanation` | `String` |

#### InstructionCheck (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `category` | `String` |
| `constraint` | `String` |
| `status` | `InstructionCheckStatus` |
| `evidence` | `String` |

#### QuizInstructionVerdict (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `followsInstructions` | `boolean` |
| `confidence` | `double` |
| `severity` | `InstructionSeverity` |
| `reason` | `String` |
| `violations` | `List<InstructionViolation>` |
| `checkedConstraints` | `List<InstructionCheck>` |

#### QuizInstructionDiagnosis (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `verdict` | `QuizInstructionVerdict` |
| `score` | `double` |
| `reusedFromPreviousRun` | `boolean` |

#### QuizInstructionCoverageDiagnosis (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `coverage` | `EvaluationCoverage` |
| `consultedJudgeVersion` | `String` |
| `verdictsByJudgeVersion` | `List<JudgeVersionVerdictCount>` |
| `unmatchedReevaluationIds` | `List<String>` |
| `judge` | `JudgeCoverageDiagnosis` |

#### JudgeVersionVerdictCount (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeVersion` | `String` |
| `verdictCount` | `int` |

#### QuizInstructionSubjectView (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `subjectRef` | `String` |
| `cefrLevel` | `String` |
| `topic` | `String` |
| `title` | `String` |
| `instructions` | `String` |
| `sentenceParts` | `List<SentencePartEntity>` |
| `multipleChoice` | `MultipleChoiceEntity` |

#### QuizInstructionComplianceStatus (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `EVALUATED` | `null` |
| `UNAVAILABLE` | `null` |
| `UNUSABLE` | `null` |

#### QuizInstructionComplianceResult (package: quizinstruction)

**Package:** `com.learney.contentaudit.auditdomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `status` | `QuizInstructionComplianceStatus` |
| `verdict` | `QuizInstructionVerdict` |
| `reason` | `String` |
| `evaluatorVersion` | `String` |
| `reused` | `boolean` |

#### QuizInstructionReevaluationDecision (package: quizinstructionengine)

**Package:** `com.learney.contentaudit.auditdomain.quizinstructionengine`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `ORDINARY` | `null` |
| `AREA_REEVALUATION` | `null` |
| `DECLARED_SET_REEVALUATION` | `null` |
| `OUTSIDE_DECLARED_SET` | `null` |
| `EXAM_CASE` | `null` |
| `NOT_REQUESTED` | `null` |

#### Finding (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `rule` | `String` |
| `node` | `FindingNodeRef` |
| `severity` | `FindingSeverity` |
| `evidence` | `FindingEvidence` |
| `resolution` | `FindingResolution` |
| `cost` | `AnalysisCost` |
| `identity` | `FindingIdentity` |

#### FindingDraft (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `marker` | `String` |
| `severity` | `FindingSeverity` |
| `resolution` | `FindingResolution` |
| `evidence` | `FindingEvidence` |

#### FindingNodeRef (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `target` | `AuditTarget` |
| `nodeId` | `String` |
| `label` | `String` |

#### FindingEvidence (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `examined` | `List<EvidencePart>` |
| `observation` | `String` |
| `relatedNodes` | `List<FindingNodeRef>` |

#### EvidencePart (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `label` | `String` |
| `text` | `String` |

#### FindingIdentity (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `key` | `String` |
| `marker` | `String` |

#### FindingSeverity (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `BLOCKING` | `null` |
| `HIGH` | `null` |
| `MEDIUM` | `null` |
| `LOW` | `null` |

#### FindingResolution (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `RULE` | `null` |
| `PANEL` | `null` |
| `RANK_ONLY` | `null` |

#### AnalysisCost (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `INSTANT` | `null` |
| `LOCAL_MODEL` | `null` |
| `PAID_MODEL` | `null` |

#### FindingContractViolationException (package: finding)

**Package:** `com.learney.contentaudit.auditdomain.finding`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `analyzerName` | `String` |
| `nodeId` | `String` |
| `detail` | `String` |

#### AnalyzerFamily (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `ERRORS` | `null` |
| `VOCABULARY` | `null` |

#### AnalyzerRuleCard (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `id` | `String` |
| `description` | `String` |
| `cost` | `AnalysisCost` |

#### AnalyzerPlanBinding (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `taskKind` | `String` |
| `taskTargets` | `List<AuditTarget>` |

#### AnalyzerRunSelection (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzers` | `List<String>` |
| `policies` | `Map<String,EvaluationRunPolicy>` |

#### UnknownAnalyzerException (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `analyzerName` | `String` |

#### InvalidAnalyzerCardException (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `analyzerName` | `String` |
| `detail` | `String` |

#### AnalyzerMeasure (package: catalog)

**Package:** `com.learney.contentaudit.auditdomain.catalog`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `fingerprint` | `String` |
| `config` | `Map<String,Object>` |

#### ContextNumbers (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `vocabularyScore` | `Double` |
| `analyzerScores` | `List<AnalyzerScore>` |
| `errors` | `ErrorCounts` |
| `findingCounts` | `List<AnalyzerFindingCounts>` |

#### AnalyzerScore (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `family` | `AnalyzerFamily` |
| `score` | `Double` |
| `subMetrics` | `List<SubMetricScore>` |

#### SubMetricScore (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `name` | `String` |
| `score` | `double` |

#### ErrorCounts (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizzes` | `int` |
| `withAnyError` | `int` |
| `anyErrorShare` | `double` |
| `bySeverity` | `SeverityCounts` |
| `notFullyEvaluated` | `int` |
| `analyzers` | `List<AnalyzerErrorCounts>` |

#### AnalyzerErrorCounts (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `reached` | `int` |
| `evaluated` | `int` |
| `notEvaluated` | `int` |
| `withError` | `int` |
| `errorShare` | `double` |
| `marked` | `int` |
| `partiallyEvaluated` | `int` |

#### SeverityCounts (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `blocking` | `int` |
| `high` | `int` |
| `medium` | `int` |
| `low` | `int` |

#### AuditDigest (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `auditId` | `String` |
| `root` | `DigestNode` |

#### DigestNode (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `nodeId` | `String` |
| `target` | `AuditTarget` |
| `label` | `String` |
| `numbers` | `ContextNumbers` |
| `findings` | `List<Finding>` |
| `unevaluatedBy` | `List<String>` |
| `children` | `List<DigestNode>` |
| `partiallyEvaluatedBy` | `List<String>` |

#### AnalyzerFindingCounts (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `family` | `AnalyzerFamily` |
| `target` | `AuditTarget` |
| `reached` | `int` |
| `evaluated` | `int` |
| `withFinding` | `int` |
| `findings` | `int` |
| `bySeverity` | `SeverityCounts` |
| `byRule` | `List<RuleFindingCount>` |

#### RuleFindingCount (package: contextnumbers)

**Package:** `com.learney.contentaudit.auditdomain.contextnumbers`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `withFinding` | `int` |
| `findings` | `int` |

#### QuizStudentView (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `statement` | `String` |
| `gaps` | `List<StudentGapView>` |
| `options` | `List<StudentOptionView>` |
| `instructions` | `String` |
| `translation` | `String` |
| `miniTheory` | `String` |

#### StudentGapView (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `hint` | `String` |
| `acceptedAnswers` | `List<String>` |
| `firstAnswer` | `String` |

#### StudentOptionView (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `label` | `String` |
| `correct` | `boolean` |

#### TopicStudentView (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `auditId` | `String` |
| `knowledgeId` | `String` |
| `label` | `String` |
| `instructions` | `String` |
| `sentenceMode` | `SentenceMode` |
| `quizzes` | `List<StudentQuizEntry>` |

#### StudentQuizEntry (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `view` | `QuizStudentView` |
| `finishedSentence` | `String` |
| `answerAudioKey` | `String` |
| `recordedAudioText` | `String` |

#### StudentViewKnowledgeNotFoundException (package: studentview)

**Package:** `com.learney.contentaudit.auditdomain.studentview`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |

#### ChoiceAlternativeOption (package: choicealternative)

**Package:** `com.learney.contentaudit.auditdomain.choicealternative`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `label` | `String` |
| `severity` | `FindingSeverity` |
| `reason` | `String` |

#### ChoiceAlternativeVerdict (package: choicealternative)

**Package:** `com.learney.contentaudit.auditdomain.choicealternative`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `correctIsValid` | `boolean` |
| `validAlternatives` | `List<ChoiceAlternativeOption>` |
| `correctProblem` | `String` |
| `rationale` | `String` |

#### ChoiceAlternativeJudgmentStatus (package: choicealternative)

**Package:** `com.learney.contentaudit.auditdomain.choicealternative`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `JUDGED` | `null` |
| `PENDING` | `null` |
| `FAILED` | `null` |
| `NOT_AVAILABLE` | `null` |

#### ChoiceAlternativeJudgment (package: choicealternative)

**Package:** `com.learney.contentaudit.auditdomain.choicealternative`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `status` | `ChoiceAlternativeJudgmentStatus` |
| `verdict` | `ChoiceAlternativeVerdict` |
| `droppedAsForm` | `List<String>` |
| `findings` | `List<FindingDraft>` |
| `score` | `Double` |
| `evaluatorVersion` | `String` |
| `reused` | `boolean` |
| `notJudgedReason` | `String` |

#### HintCoherenceDiagnosis (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `hint` | `String` |
| `firstAnswer` | `String` |
| `shape` | `ExerciseShape` |
| `hintedInTopic` | `int` |
| `comparableInTopic` | `int` |
| `issues` | `List<HintIssue>` |
| `score` | `double` |

#### HintIssue (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `kind` | `HintIssueKind` |
| `word` | `String` |
| `proposedHint` | `String` |
| `resolution` | `FindingResolution` |

#### HintIssueKind (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `MISSING_HINT` | `null` |
| `HINT_WORD_ABSENT` | `null` |
| `VERB_NOT_ANNOUNCED` | `null` |
| `SUBJECT_MISMATCH` | `null` |
| `SUBJECT_NOT_ANNOUNCED` | `null` |
| `NEGATION_WITHOUT_NEGATIVE` | `null` |
| `REWRITE_WORD_NOT_ANNOUNCED` | `null` |

#### ExerciseShape (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `GAP_IN_SENTENCE` | `null` |
| `WHOLE_SENTENCE` | `null` |

#### ExerciseHint (package: hintcoherence)

**Package:** `com.learney.contentaudit.auditdomain.hintcoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `text` | `String` |
| `words` | `List<String>` |
| `subject` | `String` |
| `negated` | `boolean` |
| `choice` | `boolean` |

#### GapContext (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `before` | `String` |
| `after` | `String` |

#### FormEquivalenceTable (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `version` | `String` |
| `contractions` | `List<ContractionExpansion>` |
| `equivalentWords` | `List<EquivalentWordGroup>` |
| `invertsNegativeQuestions` | `boolean` |
| `rules` | `List<CorrectorRule>` |

#### ContractionExpansion (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `contracted` | `String` |
| `expansions` | `List<String>` |
| `wholeWord` | `boolean` |

#### EquivalentWordGroup (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `equivalenceClass` | `FormEquivalenceClass` |
| `words` | `List<String>` |

#### FormEquivalenceClass (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `CONTRACTION` | `null` |
| `BE_NEGATION` | `null` |
| `SPELLING` | `null` |
| `PREPOSITION` | `null` |

#### CorrectorRule (package: correctormatch)

**Package:** `com.learney.contentaudit.auditdomain.correctormatch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `AMBIGUOUS_CONTRACTIONS` | `null` |
| `STANDARD_IRREGULAR_PARTICIPLES` | `null` |
| `TILL_OWN_SUBJECTS` | `null` |
| `OPTIONAL_PARENTHESES` | `null` |

#### AnswerCompletenessDiagnosis (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `missingForms` | `List<MissingAcceptedForm>` |
| `formRequests` | `List<FormRequest>` |
| `tableVersion` | `String` |
| `score` | `double` |

#### MissingAcceptedForm (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `gapIndex` | `int` |
| `acceptedAnswer` | `String` |
| `missingForm` | `String` |
| `equivalenceClass` | `FormEquivalenceClass` |
| `severity` | `FindingSeverity` |

#### FormRequest (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `source` | `FormRequestSource` |
| `requestedForm` | `RequestedForm` |
| `phrase` | `String` |
| `removableByRule` | `boolean` |

#### FormRequestSource (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `INSTRUCTIONS` | `null` |
| `MINI_THEORY` | `null` |

#### RequestedForm (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `SHORT` | `null` |
| `LONG` | `null` |

#### EquivalentForm (package: answercompleteness)

**Package:** `com.learney.contentaudit.auditdomain.answercompleteness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `form` | `String` |
| `equivalenceClass` | `FormEquivalenceClass` |

#### MultipleChoiceValidityDiagnosis (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `issues` | `List<OptionIssue>` |
| `judgment` | `ChoiceAlternativeJudgment` |
| `score` | `double` |

#### OptionIssue (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `kind` | `OptionIssueKind` |
| `option` | `String` |
| `relatedOption` | `String` |
| `involvesCorrect` | `boolean` |

#### OptionIssueKind (package: multiplechoicevalidity)

**Package:** `com.learney.contentaudit.auditdomain.multiplechoicevalidity`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `NO_VALID_OPTION` | `null` |
| `SEVERAL_VALID_OPTIONS` | `null` |
| `IDENTICAL_OPTIONS` | `null` |
| `SAME_ANSWER_OTHER_FORM` | `null` |
| `LOWERCASE_SENTENCE_START` | `null` |
| `LOWERCASE_I` | `null` |
| `DASH_AT_SENTENCE_START` | `null` |
| `ARTICLE_GIVES_AWAY` | `null` |

#### AudioRecording (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `audioKey` | `String` |
| `recordedText` | `String` |
| `source` | `String` |

#### DataCoherenceDiagnosis (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `issues` | `List<CopyMismatch>` |
| `registryVersion` | `String` |
| `score` | `double` |

#### CopyMismatch (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `kind` | `CopyMismatchKind` |
| `copy` | `ExerciseCopy` |
| `expected` | `String` |
| `found` | `String` |

#### CopyMismatchKind (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `SAVED_SENTENCE_DIFFERS` | `null` |
| `SLASH_IN_ANSWER` | `null` |
| `SLASH_IN_AUDIO` | `null` |
| `AUDIO_DIFFERS` | `null` |
| `NO_AUDIO_KEY` | `null` |
| `SHARED_AUDIO_KEY` | `null` |
| `UNKNOWN_RECORDING` | `null` |

#### ExerciseCopy (package: datacoherence)

**Package:** `com.learney.contentaudit.auditdomain.datacoherence`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `TREE_SENTENCE` | `null` |
| `PRODUCTION_SENTENCE` | `null` |
| `ACCEPTED_ANSWER` | `null` |
| `AUDIO` | `null` |

#### ExactDuplicateDiagnosis (package: exactduplicates)

**Package:** `com.learney.contentaudit.auditdomain.exactduplicates`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `keptQuizId` | `String` |
| `finishedExercise` | `String` |
| `score` | `double` |

#### JudgeExamStatus (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `APPROVED` | `null` |
| `FAILED` | `null` |
| `TAKEN` | `null` |
| `NOT_TAKEN` | `null` |
| `NO_VERSION` | `null` |

#### JudgeExamGroupResult (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `group` | `String` |
| `cases` | `int` |
| `asExpected` | `int` |
| `notAsExpected` | `int` |

#### JudgeExamRecord (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeName` | `String` |
| `judgeVersion` | `String` |
| `status` | `JudgeExamStatus` |
| `casesJudged` | `int` |
| `casesNotJudged` | `int` |
| `groups` | `List<JudgeExamGroupResult>` |
| `mismatchedCaseIds` | `List<String>` |
| `goalMet` | `boolean` |
| `photo` | `String` |
| `measuredBy` | `String` |
| `recordedAt` | `Instant` |

#### JudgeExamStanding (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeName` | `String` |
| `judgeVersion` | `String` |
| `status` | `JudgeExamStatus` |
| `consultsOnCourse` | `boolean` |
| `reason` | `String` |

#### JudgeVersionStanding (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeVersion` | `String` |
| `verdictCount` | `int` |
| `examStatus` | `JudgeExamStatus` |

#### JudgeCostEstimate (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `pendingUnits` | `int` |
| `pendingConsultations` | `int` |
| `ratePerHour` | `double` |
| `rateMeasured` | `boolean` |
| `hoursOneAtATime` | `double` |
| `maxConcurrentEvaluations` | `int` |
| `hoursConcurrent` | `double` |

#### EvaluatorCoverage (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `evaluatorId` | `String` |
| `coverage` | `EvaluationCoverage` |
| `consultedVersion` | `String` |

#### JudgeCoverageDiagnosis (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeName` | `String` |
| `unit` | `AuditTarget` |
| `reached` | `int` |
| `evaluated` | `int` |
| `notEvaluated` | `int` |
| `evaluators` | `List<EvaluatorCoverage>` |
| `standing` | `JudgeExamStanding` |
| `versions` | `List<JudgeVersionStanding>` |
| `unavailableReason` | `String` |
| `cost` | `JudgeCostEstimate` |
| `unmatchedDeclaredIds` | `List<String>` |
| `quizzes` | `JudgeQuizTally` |

#### JudgeSessions (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `judgeName` | `String` |
| `judgeVersion` | `String` |
| `evaluatorIds` | `List<String>` |
| `sessions` | `List<EvaluationSession>` |
| `standing` | `JudgeExamStanding` |
| `effectivePolicy` | `EvaluationRunPolicy` |

#### JudgeRequestPlan (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `units` | `List<AuditNode>` |
| `modes` | `List<EvaluationRequestMode>` |
| `unmatchedDeclaredIds` | `List<String>` |

#### JudgeUnitTally (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `unit` | `AuditTarget` |
| `reached` | `int` |
| `evaluated` | `int` |
| `notEvaluated` | `int` |
| `verdictsByVersion` | `Map<String,Integer>` |
| `unmatchedDeclaredIds` | `List<String>` |
| `quizzes` | `JudgeQuizTally` |

#### JudgeExamOutcome (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `FAILS` | `null` |
| `MARKED` | `null` |
| `PASSES` | `null` |
| `NOT_JUDGED` | `null` |

#### JudgeExamVerdict (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `caseId` | `String` |
| `judgeName` | `String` |
| `judgeVersion` | `String` |
| `outcome` | `JudgeExamOutcome` |
| `worstSeverity` | `FindingSeverity` |
| `rules` | `List<String>` |
| `detail` | `String` |

#### JudgeQuizTally (package: judging)

**Package:** `com.learney.contentaudit.auditdomain.judging`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `reached` | `int` |
| `evaluated` | `int` |
| `notEvaluated` | `int` |

#### TranslationSenseCondition (package: translationsense)

**Package:** `com.learney.contentaudit.auditdomain.translationsense`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `SAME_MEANING` | `null` |
| `NATIVE_WOULD_SAY` | `null` |
| `WORDS_GO_TOGETHER` | `null` |
| `LEVEL_REGISTER` | `null` |

#### TranslationSenseFailure (package: translationsense)

**Package:** `com.learney.contentaudit.auditdomain.translationsense`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `condition` | `TranslationSenseCondition` |
| `severity` | `FindingSeverity` |
| `fragment` | `String` |
| `evidence` | `String` |

#### TranslationSenseVerdict (package: translationsense)

**Package:** `com.learney.contentaudit.auditdomain.translationsense`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `passes` | `boolean` |
| `failures` | `List<TranslationSenseFailure>` |
| `rationale` | `String` |

#### TranslationSenseAgreement (package: translationsense)

**Package:** `com.learney.contentaudit.auditdomain.translationsense`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `BOTH_PASS` | `null` |
| `BOTH_FAIL` | `null` |
| `SPLIT` | `null` |

#### TranslationSenseDiagnosis (package: translationsense)

**Package:** `com.learney.contentaudit.auditdomain.translationsense`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `fromEnglish` | `TranslationSenseVerdict` |
| `fromTranslation` | `TranslationSenseVerdict` |
| `agreement` | `TranslationSenseAgreement` |
| `score` | `double` |
| `reusedFromPreviousRun` | `boolean` |

#### TranslationSenseOutcome (package: translationsenseengine)

**Package:** `com.learney.contentaudit.auditdomain.translationsenseengine`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `agreement` | `TranslationSenseAgreement` |
| `findings` | `List<FindingDraft>` |
| `score` | `double` |

#### AlternativeAnswerKind (package: singleanswer)

**Package:** `com.learney.contentaudit.auditdomain.singleanswer`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `OFFERED_BY_INSTRUCTION` | `null` |
| `DIFFERENT_VALID` | `null` |

#### AlternativeAnswer (package: singleanswer)

**Package:** `com.learney.contentaudit.auditdomain.singleanswer`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `answer` | `String` |
| `gap` | `int` |
| `kind` | `AlternativeAnswerKind` |
| `severity` | `FindingSeverity` |
| `reason` | `String` |

#### SingleAnswerVerdict (package: singleanswer)

**Package:** `com.learney.contentaudit.auditdomain.singleanswer`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `alternatives` | `List<AlternativeAnswer>` |
| `rationale` | `String` |

#### SingleAnswerDiagnosis (package: singleanswer)

**Package:** `com.learney.contentaudit.auditdomain.singleanswer`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `verdict` | `SingleAnswerVerdict` |
| `counted` | `List<AlternativeAnswer>` |
| `droppedAsAccepted` | `List<String>` |
| `score` | `double` |
| `reusedFromPreviousRun` | `boolean` |

#### SingleAnswerOutcome (package: singleanswerengine)

**Package:** `com.learney.contentaudit.auditdomain.singleanswerengine`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `diagnosis` | `SingleAnswerDiagnosis` |
| `findings` | `List<FindingDraft>` |
| `score` | `double` |

#### MiniTheoryProblem (package: minitheory)

**Package:** `com.learney.contentaudit.auditdomain.minitheory`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `FALSE_RULE` | `null` |
| `OVERSTATED` | `null` |
| `CONTRADICTED_BY_EXERCISE` | `null` |
| `NOT_COVERING_EXERCISE` | `null` |

#### MiniTheoryErrorScope (package: minitheory)

**Package:** `com.learney.contentaudit.auditdomain.minitheory`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `SHOWN_ONLY` | `null` |
| `WHOLE_TOPIC` | `null` |

#### MiniTheoryAssessment (package: minitheory)

**Package:** `com.learney.contentaudit.auditdomain.minitheory`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `miniTheory` | `int` |
| `holds` | `boolean` |
| `problem` | `MiniTheoryProblem` |
| `severity` | `FindingSeverity` |
| `counterexample` | `String` |
| `exercises` | `List<Integer>` |
| `explanation` | `String` |

#### MiniTheoryVerdict (package: minitheory)

**Package:** `com.learney.contentaudit.auditdomain.minitheory`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `assessments` | `List<MiniTheoryAssessment>` |
| `rationale` | `String` |

#### MiniTheoryDiagnosis (package: minitheory)

**Package:** `com.learney.contentaudit.auditdomain.minitheory`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `miniTheory` | `String` |
| `assessments` | `List<MiniTheoryAssessment>` |
| `score` | `double` |
| `reusedFromPreviousRun` | `boolean` |

#### MiniTheoryQuizOutcome (package: minitheoryengine)

**Package:** `com.learney.contentaudit.auditdomain.minitheoryengine`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `diagnosis` | `MiniTheoryDiagnosis` |
| `findings` | `List<FindingDraft>` |
| `score` | `double` |

#### OffLevelWordsDiagnosis (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `words` | `List<OffLevelWord>` |

#### OffLevelWord (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |
| `reason` | `OffLevelReason` |
| `catalogLevel` | `CefrLevel` |
| `frequencyRank` | `Integer` |

#### OffLevelReason (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `ABOVE_LEVEL` | `null` |
| `OUT_OF_CATALOG` | `null` |

#### TaughtWords (package: offlevelwords)

**Package:** `com.learney.contentaudit.auditdomain.offlevelwords`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `titleTerms` | `List<String>` |
| `frequentAnswers` | `List<String>` |

#### LocalToolStatus (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `tool` | `String` |
| `version` | `String` |
| `available` | `boolean` |
| `unavailableReason` | `String` |
| `installHint` | `String` |

#### LocalToolCard (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `status` | `LocalToolStatus` |
| `examinedVersion` | `String` |
| `examState` | `JudgeExamStatus` |
| `examSummary` | `String` |
| `alternative` | `boolean` |

#### LocalToolGateDecision (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `publishes` | `boolean` |
| `status` | `LocalToolStatus` |
| `examinedVersion` | `String` |
| `examState` | `JudgeExamStatus` |
| `examRun` | `boolean` |
| `reason` | `String` |
| `localPolicy` | `EvaluationRunPolicy` |

#### LocalToolRunDiagnosis (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `status` | `LocalToolStatus` |
| `examinedVersion` | `String` |
| `examState` | `JudgeExamStatus` |
| `published` | `boolean` |
| `reason` | `String` |
| `alternative` | `boolean` |

#### LocalToolUnavailableException (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `analyzerName` | `String` |
| `requiredTool` | `String` |
| `installHint` | `String` |

#### EnglishVariant (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `AMERICAN` | `null` |
| `BRITISH` | `null` |

#### GrammarMatch (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `ruleId` | `String` |
| `categoryId` | `String` |
| `start` | `int` |
| `end` | `int` |
| `message` | `String` |
| `suggestions` | `List<String>` |

#### YesNoProbabilities (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `yesProbability` | `double` |
| `noProbability` | `double` |

#### SentenceEmbedding (package: localtools)

**Package:** `com.learney.contentaudit.auditdomain.localtools`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `values` | `List<Double>` |

#### AnswerSpan (package: finishedsentence)

**Package:** `com.learney.contentaudit.auditdomain.finishedsentence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `start` | `int` |
| `end` | `int` |
| `text` | `String` |

#### FinishedSentence (package: finishedsentence)

**Package:** `com.learney.contentaudit.auditdomain.finishedsentence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `text` | `String` |
| `answerIndex` | `int` |
| `answers` | `List<AnswerSpan>` |

#### GrammarIssuePlacement (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `GLUED_REPEAT` | `null` |
| `ACCEPTED_ANSWER` | `null` |
| `FIXED_TEXT` | `null` |
| `PUNCTUATION` | `null` |

#### GrammarIssue (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `rule` | `String` |
| `toolRule` | `String` |
| `marked` | `String` |
| `sentence` | `String` |
| `placement` | `GrammarIssuePlacement` |
| `severity` | `FindingSeverity` |
| `resolution` | `FindingResolution` |
| `explanation` | `String` |

#### GrammarDiagnosis (package: grammar)

**Package:** `com.learney.contentaudit.auditdomain.grammar`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sentences` | `List<String>` |
| `issues` | `List<GrammarIssue>` |
| `score` | `double` |

#### NaturalnessDiagnosis (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sentence` | `String` |
| `level` | `String` |
| `number` | `Double` |
| `threshold` | `Double` |
| `marked` | `boolean` |
| `reused` | `boolean` |
| `alternative` | `boolean` |

#### NaturalnessLevelQueue (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `level` | `String` |
| `threshold` | `Double` |
| `markedQuizIds` | `List<String>` |

#### NaturalnessQueueDiagnosis (package: naturalness)

**Package:** `com.learney.contentaudit.auditdomain.naturalness`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `examinedVersion` | `String` |
| `levels` | `List<NaturalnessLevelQueue>` |
| `alternative` | `boolean` |

#### NearDuplicateCondition (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `WILDCARD` | `null` |
| `SIMILARITY` | `null` |
| `JUDGED` | `null` |

#### NearDuplicateDiagnosis (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `keptQuizId` | `String` |
| `condition` | `NearDuplicateCondition` |
| `difference` | `String` |
| `similarity` | `double` |
| `wordSimilarity` | `double` |
| `judgeReason` | `String` |
| `finishedExercise` | `String` |
| `keptFinishedExercise` | `String` |
| `score` | `double` |

#### SameExerciseVerdict (package: nearduplicates)

**Package:** `com.learney.contentaudit.auditdomain.nearduplicates`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sameExercise` | `boolean` |
| `reason` | `String` |

#### ExercisePart (package: correction)

**Package:** `com.learney.contentaudit.auditdomain.correction`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `STATEMENT` | `null` |
| `HINT` | `null` |
| `ANSWERS` | `null` |
| `OPTIONS` | `null` |
| `FORM_KIND` | `null` |
| `TRANSLATION` | `null` |
| `MINI_THEORY` | `null` |
| `INSTRUCTIONS` | `null` |
| `TOPIC_INSTRUCTIONS` | `null` |
| `STORED_SENTENCE` | `null` |
| `AUDIO` | `null` |
| `WHOLE_EXERCISE` | `null` |

#### RuleFixRequest (package: rulefix)

**Package:** `com.learney.contentaudit.auditdomain.rulefix`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quiz` | `QuizTemplateEntity` |
| `auditQuiz` | `AuditableQuiz` |
| `sentenceMode` | `SentenceMode` |
| `findings` | `List<Finding>` |

#### RuleFixResult (package: rulefix)

**Package:** `com.learney.contentaudit.auditdomain.rulefix`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quiz` | `QuizTemplateEntity` |
| `fixed` | `List<FindingIdentity>` |
| `declined` | `List<FindingIdentity>` |
| `detail` | `String` |
| `rerecordings` | `List<RuleFixRerecording>` |

#### RuleFixRerecording (package: rulefix)

**Package:** `com.learney.contentaudit.auditdomain.rulefix`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `audioKey` | `String` |
| `text` | `String` |

### Module: course-domain

#### NodeKind

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `ROOT` | `null` |  |
| `MILESTONE` | `null` |  |
| `TOPIC` | `null` |  |
| `KNOWLEDGE` | `null` |  |

**Generated constructor:**
```java
new NodeKind(null ROOT, null MILESTONE, null TOPIC, null KNOWLEDGE)
```

#### SentencePartKind

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `TEXT` | `null` |  |
| `CLOZE` | `null` |  |

**Generated constructor:**
```java
new SentencePartKind(null TEXT, null CLOZE)
```

#### CourseEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `title` | `String` |  |
| `knowledgeIds` | `List<String>` | Import `java.util.List` |
| `root` | `RootNodeEntity` |  |
| `slug` | `String` |  |

**Generated constructor:**
```java
new CourseEntity(String id, String title, List<String> knowledgeIds, RootNodeEntity root, String slug)
```

#### RootNodeEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `code` | `String` |  |
| `kind` | `NodeKind` |  |
| `label` | `String` |  |
| `children` | `List<String>` | Import `java.util.List` |
| `milestones` | `List<MilestoneEntity>` | Import `java.util.List` |

**Generated constructor:**
```java
new RootNodeEntity(String id, String code, NodeKind kind, String label, List<String> children, List<MilestoneEntity> milestones)
```

#### MilestoneEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `code` | `String` |  |
| `kind` | `NodeKind` |  |
| `label` | `String` |  |
| `oldId` | `String` |  |
| `parentId` | `String` |  |
| `children` | `List<String>` | Import `java.util.List` |
| `order` | `int` |  |
| `slug` | `String` |  |
| `topics` | `List<TopicEntity>` | Import `java.util.List` |

**Generated constructor:**
```java
new MilestoneEntity(String id, String code, NodeKind kind, String label, String oldId, String parentId, List<String> children, int order, String slug, List<TopicEntity> topics)
```

#### TopicEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `code` | `String` |  |
| `kind` | `NodeKind` |  |
| `label` | `String` |  |
| `oldId` | `String` |  |
| `parentId` | `String` |  |
| `children` | `List<String>` | Import `java.util.List` |
| `ruleIds` | `List<String>` | Import `java.util.List` |
| `order` | `int` |  |
| `slug` | `String` |  |
| `knowledges` | `List<KnowledgeEntity>` | Import `java.util.List` |

**Generated constructor:**
```java
new TopicEntity(String id, String code, NodeKind kind, String label, String oldId, String parentId, List<String> children, List<String> ruleIds, int order, String slug, List<KnowledgeEntity> knowledges)
```

#### KnowledgeEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `code` | `String` |  |
| `kind` | `NodeKind` |  |
| `label` | `String` |  |
| `oldId` | `String` |  |
| `parentId` | `String` |  |
| `isRule` | `boolean` |  |
| `instructions` | `String` |  |
| `order` | `int` |  |
| `slug` | `String` |  |
| `quizTemplates` | `List<QuizTemplateEntity>` | Import `java.util.List` |
| `sentenceMode` | `SentenceMode` |  |

**Generated constructor:**
```java
new KnowledgeEntity(String id, String code, NodeKind kind, String label, String oldId, String parentId, boolean isRule, String instructions, int order, String slug, List<QuizTemplateEntity> quizTemplates, SentenceMode sentenceMode)
```

#### QuizTemplateEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `oidId` | `String` |  |
| `kind` | `String` |  |
| `knowledgeId` | `String` |  |
| `title` | `String` |  |
| `instructions` | `String` |  |
| `translation` | `String` |  |
| `theoryId` | `String` |  |
| `topicName` | `String` |  |
| `form` | `FormEntity` |  |
| `difficulty` | `double` |  |
| `retries` | `double` |  |
| `noScoreRetries` | `double` |  |
| `code` | `String` |  |
| `audioUrl` | `String` |  |
| `imageUrl` | `String` |  |
| `answerAudioUrl` | `String` |  |
| `answerImageUrl` | `String` |  |
| `miniTheory` | `String` |  |
| `successMessage` | `String` |  |
| `sentences` | `List<String>` | Import `java.util.List` |
| `unmodeledFields` | `Map<String,Object>` |  |

**Generated constructor:**
```java
new QuizTemplateEntity(String id, String oidId, String kind, String knowledgeId, String title, String instructions, String translation, String theoryId, String topicName, FormEntity form, double difficulty, double retries, double noScoreRetries, String code, String audioUrl, String imageUrl, String answerAudioUrl, String answerImageUrl, String miniTheory, String successMessage, List<String> sentences, Map<String,Object> unmodeledFields)
```

#### FormEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `String` |  |
| `incidence` | `double` |  |
| `label` | `String` |  |
| `name` | `String` |  |
| `sentenceParts` | `List<SentencePartEntity>` | Import `java.util.List` |
| `multipleChoice` | `MultipleChoiceEntity` |  |
| `unmodeledFields` | `Map<String,Object>` |  |

**Generated constructor:**
```java
new FormEntity(String kind, double incidence, String label, String name, List<SentencePartEntity> sentenceParts, MultipleChoiceEntity multipleChoice, Map<String,Object> unmodeledFields)
```

#### SentencePartEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `SentencePartKind` |  |
| `text` | `String` |  |
| `options` | `List<String>` | Import `java.util.List` |

**Generated constructor:**
```java
new SentencePartEntity(SentencePartKind kind, String text, List<String> options)
```

#### CourseValidationException

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"Error al cargar el curso desde '%s': %s. La carga fue abortada."`

| Field | Type | Notes |
|-------|------|-------|
| `path` | `String` |  |
| `detail` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new CourseValidationException(String path, String detail)
```

#### SentenceMode

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `REWRITE` | `null` |  |
| `FILL` | `null` |  |

**Generated constructor:**
```java
new SentenceMode(null REWRITE, null FILL)
```

#### FormKind

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `CLOZE` | `null` |  |
| `MULTIPLE_CHOICE` | `null` |  |
| `OTHER` | `null` |  |

**Generated constructor:**
```java
new FormKind(null CLOZE, null MULTIPLE_CHOICE, null OTHER)
```

#### MultipleChoiceItemEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `incidence` | `double` |  |
| `label` | `String` |  |

**Generated constructor:**
```java
new MultipleChoiceItemEntity(String id, double incidence, String label)
```

#### MultipleChoiceEntity

**Package:** `com.learney.contentaudit.coursedomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `selection` | `String` |  |
| `items` | `List<MultipleChoiceItemEntity>` | Import `java.util.List` |

**Generated constructor:**
```java
new MultipleChoiceEntity(String selection, List<MultipleChoiceItemEntity> items)
```

#### QuizSentenceSerializationException (package: quizsentence)

**Package:** `com.learney.contentaudit.coursedomain.quizsentence`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `reason` | `String` |
| `position` | `String` |

#### QuizSentenceParseException (package: quizsentence)

**Package:** `com.learney.contentaudit.coursedomain.quizsentence`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `reason` | `String` |
| `position` | `int` |

### Module: refiner-domain

#### DiagnosisKind

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `SENTENCE_LENGTH` | `null` |  |
| `LEMMA_ABSENCE` | `null` |  |
| `COCA_BUCKETS` | `null` |  |
| `LEMMA_RECURRENCE` | `null` |  |
| `KNOWLEDGE_TITLE_LENGTH` | `null` |  |
| `KNOWLEDGE_INSTRUCTIONS_LENGTH` | `null` |  |
| `QUIZ_INSTRUCTION` | `null` |  |
| `HINT_COHERENCE` | `null` |  |
| `ANSWER_COMPLETENESS` | `null` |  |
| `MULTIPLE_CHOICE_VALIDITY` | `null` |  |
| `DATA_COHERENCE` | `null` |  |
| `EXACT_DUPLICATES` | `null` |  |
| `TRANSLATION_SENSE` | `null` |  |
| `SINGLE_ANSWER` | `null` |  |
| `MINI_THEORY` | `null` |  |
| `GRAMMAR` | `null` |  |
| `NEAR_DUPLICATES` | `null` |  |

**Generated constructor:**
```java
new DiagnosisKind(null SENTENCE_LENGTH, null LEMMA_ABSENCE, null COCA_BUCKETS, null LEMMA_RECURRENCE, null KNOWLEDGE_TITLE_LENGTH, null KNOWLEDGE_INSTRUCTIONS_LENGTH, null QUIZ_INSTRUCTION, null HINT_COHERENCE, null ANSWER_COMPLETENESS, null MULTIPLE_CHOICE_VALIDITY, null DATA_COHERENCE, null EXACT_DUPLICATES, null TRANSLATION_SENSE, null SINGLE_ANSWER, null MINI_THEORY, null GRAMMAR, null NEAR_DUPLICATES)
```

#### RefinementTaskStatus

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `PENDING` | `null` |  |
| `COMPLETED` | `null` |  |
| `SKIPPED` | `null` |  |
| `STALE` | `null` |  |
| `IN_BATCH` | `null` |  |

**Generated constructor:**
```java
new RefinementTaskStatus(null PENDING, null COMPLETED, null SKIPPED, null STALE, null IN_BATCH)
```

#### RefinementTask

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `nodeTarget` | `AuditTarget` |  |
| `nodeId` | `String` |  |
| `nodeLabel` | `String` |  |
| `diagnosisKind` | `DiagnosisKind` |  |
| `priority` | `int` |  |
| `status` | `RefinementTaskStatus` |  |
| `origin` | `TaskOrigin` |  |

**Generated constructor:**
```java
new RefinementTask(String id, AuditTarget nodeTarget, String nodeId, String nodeLabel, DiagnosisKind diagnosisKind, int priority, RefinementTaskStatus status, TaskOrigin origin)
```

#### RefinementPlan

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` |  |
| `sourceAuditId` | `String` |  |
| `createdAt` | `Instant` |  |
| `tasks` | `List<RefinementTask>` | Import `java.util.List` |

**Generated constructor:**
```java
new RefinementPlan(String id, String sourceAuditId, Instant createdAt, List<RefinementTask> tasks)
```

#### SuggestedLemma

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `lemma` | `String` |  |
| `pos` | `String` |  |
| `reason` | `String` |  |
| `cocaRank` | `Integer` |  |
| `lemmaCount` | `Integer` |  |
| `lemmaCountThreshold` | `Integer` |  |
| `isUnderexposed` | `Boolean` |  |

**Generated constructor:**
```java
new SuggestedLemma(String lemma, String pos, String reason, Integer cocaRank, Integer lemmaCount, Integer lemmaCountThreshold, Boolean isUnderexposed)
```

#### SentenceLengthCorrectionContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record
**Implements:** `CorrectionContext`

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `sentence` | `String` |  |
| `translation` | `String` |  |
| `knowledgeTitle` | `String` |  |
| `knowledgeInstructions` | `String` |  |
| `topicLabel` | `String` |  |
| `cefrLevel` | `CefrLevel` |  |
| `tokenCount` | `int` |  |
| `targetMin` | `int` |  |
| `targetMax` | `int` |  |
| `delta` | `int` |  |
| `suggestedLemmas` | `List<SuggestedLemma>` | Import `java.util.List` |

**Generated constructor:**
```java
new SentenceLengthCorrectionContext(String taskId, String sentence, String translation, String knowledgeTitle, String knowledgeInstructions, String topicLabel, CefrLevel cefrLevel, int tokenCount, int targetMin, int targetMax, int delta, List<SuggestedLemma> suggestedLemmas)
```

#### MisplacedLemmaContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `lemma` | `String` |  |
| `pos` | `String` |  |
| `expectedLevel` | `CefrLevel` |  |
| `quizLevel` | `CefrLevel` |  |
| `cocaRank` | `Integer` |  |
| `discount` | `Double` |  |

**Generated constructor:**
```java
new MisplacedLemmaContext(String lemma, String pos, CefrLevel expectedLevel, CefrLevel quizLevel, Integer cocaRank, Double discount)
```

#### LemmaAbsenceCorrectionContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record
**Implements:** `CorrectionContext`

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `sentence` | `String` |  |
| `translation` | `String` |  |
| `knowledgeTitle` | `String` |  |
| `knowledgeInstructions` | `String` |  |
| `topicLabel` | `String` |  |
| `cefrLevel` | `CefrLevel` |  |
| `misplacedLemmas` | `List<MisplacedLemmaContext>` | Import `java.util.List` |
| `suggestedLemmas` | `List<SuggestedLemma>` | Import `java.util.List` |
| `quizSentence` | `String` |  |
| `tokenCount` | `Integer` |  |
| `targetMin` | `Integer` |  |
| `targetMax` | `Integer` |  |
| `delta` | `Integer` |  |
| `lengthDirection` | `LengthDirection` |  |
| `sourceAuditId` | `String` |  |
| `sentenceMode` | `SentenceMode` |  |
| `exerciseQuizzes` | `List<String>` | Import `java.util.List` |
| `nodeId` | `String` |  |
| `scarceContentWords` | `List<ScarceContentWord>` | Import `java.util.List` |
| `outOfCatalogWords` | `List<OutOfCatalogWordContext>` | Import `java.util.List` |

**Generated constructor:**
```java
new LemmaAbsenceCorrectionContext(String taskId, String sentence, String translation, String knowledgeTitle, String knowledgeInstructions, String topicLabel, CefrLevel cefrLevel, List<MisplacedLemmaContext> misplacedLemmas, List<SuggestedLemma> suggestedLemmas, String quizSentence, Integer tokenCount, Integer targetMin, Integer targetMax, Integer delta, LengthDirection lengthDirection, String sourceAuditId, SentenceMode sentenceMode, List<String> exerciseQuizzes, String nodeId, List<ScarceContentWord> scarceContentWords, List<OutOfCatalogWordContext> outOfCatalogWords)
```

#### LengthDirection

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `SHORTEN` | `null` |  |
| `LENGTHEN` | `null` |  |
| `KEEP_SAME` | `null` |  |
| `UNKNOWN` | `null` |  |

**Generated constructor:**
```java
new LengthDirection(null SHORTEN, null LENGTHEN, null KEEP_SAME, null UNKNOWN)
```

#### SuggestedLemmaQueryResult

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `cefrLevel` | `CefrLevel` |  |
| `suggestedLemmas` | `List<SuggestedLemma>` | Import `java.util.List` |

**Generated constructor:**
```java
new SuggestedLemmaQueryResult(String taskId, CefrLevel cefrLevel, List<SuggestedLemma> suggestedLemmas)
```

#### SuggestedLemmaQueryRejectedException

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"Suggested lemma query rejected for task %s: %s"`

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `reason` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new SuggestedLemmaQueryRejectedException(String taskId, String reason)
```

#### SuggestedLemmaQueryCriteria

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `limit` | `Optional<Integer>` |  |
| `partOfSpeech` | `Optional<String>` |  |
| `explicitLevel` | `Optional<CefrLevel>` |  |
| `includeExposed` | `boolean` |  |
| `regex` | `Optional<String>` |  |

**Generated constructor:**
```java
new SuggestedLemmaQueryCriteria(Optional<Integer> limit, Optional<String> partOfSpeech, Optional<CefrLevel> explicitLevel, boolean includeExposed, Optional<String> regex)
```

#### ScarceContentWord

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `lemma` | `String` |  |
| `pos` | `String` |  |
| `count` | `Integer` |  |
| `threshold` | `Integer` |  |

**Generated constructor:**
```java
new ScarceContentWord(String lemma, String pos, Integer count, Integer threshold)
```

#### KnowledgeTitleCorrectionContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record
**Implements:** `CorrectionContext`

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `knowledgeId` | `String` |  |
| `currentTitle` | `String` |  |
| `currentInstructions` | `String` |  |
| `topicLabel` | `String` |  |
| `titleTargetMax` | `Integer` |  |
| `instructionsTargetMax` | `Integer` |  |
| `expectedTitleLanguage` | `String` |  |
| `expectedInstructionsLanguage` | `String` |  |

**Generated constructor:**
```java
new KnowledgeTitleCorrectionContext(String taskId, String knowledgeId, String currentTitle, String currentInstructions, String topicLabel, Integer titleTargetMax, Integer instructionsTargetMax, String expectedTitleLanguage, String expectedInstructionsLanguage)
```

#### OutOfCatalogWordContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `lemma` | `String` |  |
| `observedPos` | `String` |  |
| `frequencyRank` | `Integer` |  |
| `discount` | `double` |  |

**Generated constructor:**
```java
new OutOfCatalogWordContext(String lemma, String observedPos, Integer frequencyRank, double discount)
```

#### QuizInstructionCorrectionContext

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record
**Implements:** `CorrectionContext`

| Field | Type | Notes |
|-------|------|-------|
| `taskId` | `String` |  |
| `nodeId` | `String` |  |
| `quizSentence` | `String` |  |
| `translation` | `String` |  |
| `sentence` | `String` |  |
| `knowledgeTitle` | `String` |  |
| `knowledgeInstructions` | `String` |  |
| `topicLabel` | `String` |  |
| `cefrLevel` | `CefrLevel` |  |
| `cefrLevelLabel` | `String` |  |
| `sentenceMode` | `SentenceMode` |  |
| `violations` | `List<InstructionViolation>` | Import `java.util.List` |
| `verdictReason` | `String` |  |
| `severity` | `InstructionSeverity` |  |
| `siblingQuizSentences` | `List<String>` | Import `java.util.List` |
| `sourceAuditId` | `String` |  |
| `planId` | `String` |  |
| `quizInstructions` | `String` |  |

**Generated constructor:**
```java
new QuizInstructionCorrectionContext(String taskId, String nodeId, String quizSentence, String translation, String sentence, String knowledgeTitle, String knowledgeInstructions, String topicLabel, CefrLevel cefrLevel, String cefrLevelLabel, SentenceMode sentenceMode, List<InstructionViolation> violations, String verdictReason, InstructionSeverity severity, List<String> siblingQuizSentences, String sourceAuditId, String planId, String quizInstructions)
```

#### UnconvertedScoreCount

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `analyzer` | `String` |  |
| `nodeCount` | `int` |  |
| `reason` | `String` |  |

**Generated constructor:**
```java
new UnconvertedScoreCount(String analyzer, int nodeCount, String reason)
```

#### TaskOrigin

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `finding` | `Finding` |  |
| `cefrLevel` | `CefrLevel` |  |
| `knowledgeId` | `String` |  |
| `knowledgeLabel` | `String` |  |
| `reachedQuizIds` | `List<String>` | Import `java.util.List` |
| `coveredFindings` | `List<Finding>` | Import `java.util.List` |

**Generated constructor:**
```java
new TaskOrigin(Finding finding, CefrLevel cefrLevel, String knowledgeId, String knowledgeLabel, List<String> reachedQuizIds, List<Finding> coveredFindings)
```

#### PlanFindingTally

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `analyzer` | `String` |  |
| `countingFindings` | `int` |  |
| `ownTasks` | `int` |  |
| `covered` | `int` |  |
| `unconverted` | `int` |  |
| `unconvertedReason` | `String` |  |

**Generated constructor:**
```java
new PlanFindingTally(String analyzer, int countingFindings, int ownTasks, int covered, int unconverted, String unconvertedReason)
```

#### TopicWordLights (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `auditId` | `String` |
| `knowledgeId` | `String` |
| `knowledgeLabel` | `String` |
| `level` | `CefrLevel` |
| `words` | `List<TopicWord>` |
| `toAdd` | `List<SuggestedLemma>` |

#### TopicWord (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `lemma` | `String` |
| `pos` | `String` |
| `level` | `CefrLevel` |
| `outOfCatalog` | `boolean` |
| `frequencyRank` | `Integer` |
| `sentenceCount` | `int` |
| `threshold` | `Integer` |
| `light` | `WordLight` |
| `greenState` | `GreenState` |
| `taughtByTopic` | `boolean` |
| `offLevelError` | `Boolean` |

#### WordLight (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `GREEN` | `null` |
| `RED` | `null` |
| `NEUTRAL` | `null` |

#### GreenState (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `UNDER_EXPOSED` | `null` |
| `SCARCE` | `null` |

#### KnowledgeNotInAnalysisException (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |

#### LemmaCountDataMissingException (package: topicwords)

**Package:** `com.learney.contentaudit.refinerdomain.topicwords`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `auditId` | `String` |

#### ErrorTaskBrief (package: errortask)

**Package:** `com.learney.contentaudit.refinerdomain.errortask`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `task` | `RefinementTask` |
| `finding` | `Finding` |
| `coveredFindings` | `List<Finding>` |
| `editScope` | `List<ExercisePart>` |
| `scopeDeclared` | `boolean` |
| `quizView` | `QuizStudentView` |
| `reachedQuizViews` | `List<QuizStudentView>` |

### Module: audit-application

#### AuditRunRequest

**Package:** `com.learney.contentaudit.auditapplication`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `includedAnalyzers` | `Set<String>` |  |
| `excludedAnalyzers` | `Set<String>` |  |
| `analyzerPolicies` | `Map<String,EvaluationRunPolicy>` |  |

**Generated constructor:**
```java
new AuditRunRequest(Set<String> includedAnalyzers, Set<String> excludedAnalyzers, Map<String,EvaluationRunPolicy> analyzerPolicies)
```

#### JudgeExamRequest

**Package:** `com.learney.contentaudit.auditapplication`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `judgeName` | `String` |  |
| `photoCourse` | `Path` |  |
| `caseIds` | `Set<String>` |  |
| `maxNewEvaluations` | `int` |  |
| `maxConcurrentEvaluations` | `Integer` |  |

**Generated constructor:**
```java
new JudgeExamRequest(String judgeName, Path photoCourse, Set<String> caseIds, int maxNewEvaluations, Integer maxConcurrentEvaluations)
```

#### JudgeExamTaken

**Package:** `com.learney.contentaudit.auditapplication`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `judgeName` | `String` |  |
| `judgeVersion` | `String` |  |
| `verdicts` | `List<JudgeExamVerdict>` | Import `java.util.List` |
| `coverage` | `JudgeCoverageDiagnosis` |  |
| `recordedAsTaken` | `boolean` |  |

**Generated constructor:**
```java
new JudgeExamTaken(String judgeName, String judgeVersion, List<JudgeExamVerdict> verdicts, JudgeCoverageDiagnosis coverage, boolean recordedAsTaken)
```

#### JudgeExamNotTakenException

**Package:** `com.learney.contentaudit.auditapplication`
**Type:** exception
**Extends:** `IllegalArgumentException`
**Message:** `"La versión %2$s del juez '%1$s' no rindió el examen completo: no se puede registrar su resultado"`

| Field | Type | Notes |
|-------|------|-------|
| `judgeName` | `String` |  |
| `judgeVersion` | `String` |  |

**Generated class** extends `IllegalArgumentException` with constructor:
```java
new JudgeExamNotTakenException(String judgeName, String judgeVersion)
```

### Module: audit-cli

#### GetTasksFilter

**Package:** `com.learney.contentaudit.auditcli`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `planId` | `Optional<String>` |  |
| `status` | `Optional<String>` |  |
| `sortByPriority` | `boolean` |  |
| `limit` | `Optional<Integer>` |  |
| `target` | `Optional<AuditTarget>` |  |
| `diagnosisKind` | `Optional<DiagnosisKind>` |  |
| `findingFilter` | `TaskFindingFilter` |  |

**Generated constructor:**
```java
new GetTasksFilter(Optional<String> planId, Optional<String> status, boolean sortByPriority, Optional<Integer> limit, Optional<AuditTarget> target, Optional<DiagnosisKind> diagnosisKind, TaskFindingFilter findingFilter)
```

#### LagenMode

**Package:** `com.learney.contentaudit.auditcli`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `LLM` | `null` |  |
| `CANNED` | `null` |  |

**Generated constructor:**
```java
new LagenMode(null LLM, null CANNED)
```

#### PlanStorageMode

**Package:** `com.learney.contentaudit.auditcli`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `DISK` | `null` |  |
| `EPHEMERAL` | `null` |  |

**Generated constructor:**
```java
new PlanStorageMode(null DISK, null EPHEMERAL)
```

#### EphemeralRenderOptions

**Package:** `com.learney.contentaudit.auditcli`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `withCorrectionContext` | `boolean` |  |

**Generated constructor:**
```java
new EphemeralRenderOptions(boolean withCorrectionContext)
```

#### SuggestedLemmasFilter

**Package:** `com.learney.contentaudit.auditcli`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `limit` | `Optional<Integer>` |  |
| `partOfSpeech` | `Optional<String>` |  |
| `level` | `Optional<CefrLevel>` |  |
| `includeExposed` | `boolean` |  |
| `regex` | `Optional<String>` |  |

**Generated constructor:**
```java
new SuggestedLemmasFilter(Optional<Integer> limit, Optional<String> partOfSpeech, Optional<CefrLevel> level, boolean includeExposed, Optional<String> regex)
```

#### AnalyzeOptions

**Package:** `com.learney.contentaudit.auditcli`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `format` | `String` |  |
| `level` | `String` |  |
| `topic` | `String` |  |
| `knowledge` | `String` |  |
| `analyzers` | `List<String>` | Import `java.util.List` |
| `excludeAnalyzers` | `List<String>` | Import `java.util.List` |
| `detailed` | `boolean` |  |
| `instructionBudget` | `Integer` |  |
| `reevaluateInstructions` | `String` |  |
| `reevaluateInstructionQuizIds` | `Set<String>` |  |
| `analyzerBudgets` | `Map<String,Integer>` |  |
| `analyzerReevaluationSets` | `Map<String,Set<String>>` |  |
| `maxConcurrentEvaluations` | `Integer` |  |

**Generated constructor:**
```java
new AnalyzeOptions(String format, String level, String topic, String knowledge, List<String> analyzers, List<String> excludeAnalyzers, boolean detailed, Integer instructionBudget, String reevaluateInstructions, Set<String> reevaluateInstructionQuizIds, Map<String,Integer> analyzerBudgets, Map<String,Set<String>> analyzerReevaluationSets, Integer maxConcurrentEvaluations)
```

#### TaskFindingFilter

**Package:** `com.learney.contentaudit.auditcli`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `analyzer` | `Optional<String>` |  |
| `resolution` | `Optional<FindingResolution>` |  |
| `severity` | `Optional<FindingSeverity>` |  |
| `level` | `Optional<CefrLevel>` |  |
| `knowledgeId` | `Optional<String>` |  |

**Generated constructor:**
```java
new TaskFindingFilter(Optional<String> analyzer, Optional<FindingResolution> resolution, Optional<FindingSeverity> severity, Optional<CefrLevel> level, Optional<String> knowledgeId)
```

#### ReportViewModel (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `overallScore` | `double` |
| `analyzerNames` | `List<String>` |
| `analyzerScores` | `Map<String,Double>` |
| `milestoneScores` | `List<MilestoneScoreRow>` |

#### MilestoneScoreRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `milestoneId` | `String` |
| `analyzerScores` | `Map<String,Double>` |
| `overallScore` | `double` |
| `topicScores` | `List<TopicScoreRow>` |
| `entity` | `AuditableEntity` |

#### QuizScoreRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `overallScore` | `double` |
| `analyzerScores` | `Map<String,Double>` |
| `entity` | `AuditableEntity` |

#### KnowledgeScoreRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `overallScore` | `double` |
| `analyzerScores` | `Map<String,Double>` |
| `quizScores` | `List<QuizScoreRow>` |
| `entity` | `AuditableEntity` |

#### TopicScoreRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `topicId` | `String` |
| `overallScore` | `double` |
| `analyzerScores` | `Map<String,Double>` |
| `knowledgeScores` | `List<KnowledgeScoreRow>` |
| `entity` | `AuditableEntity` |

#### ChildScoreRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `id` | `String` |
| `overallScore` | `double` |
| `analyzerScores` | `Map<String,Double>` |
| `entity` | `AuditableEntity` |

#### DrillDownScope (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `level` | `Optional<String>` |
| `topic` | `Optional<String>` |
| `knowledge` | `Optional<String>` |

#### DrillDownLevel (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** enum

| Field | Type |
|-------|------|
| `COURSE` | `null` |
| `MILESTONE` | `null` |
| `TOPIC` | `null` |
| `KNOWLEDGE` | `null` |

#### DrillDownView (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `depth` | `DrillDownLevel` |
| `nodeName` | `String` |
| `overallScore` | `double` |
| `analyzerScores` | `Map<String,Double>` |
| `analyzerNames` | `List<String>` |
| `childRows` | `List<ScoreRow>` |

#### AnalyzerStatsView (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `analyzerName` | `String` |
| `analyzerDescription` | `String` |
| `courseScore` | `double` |
| `levelScores` | `Map<String,Double>` |
| `worstItems` | `List<ScoredItemRow>` |
| `scoreDistribution` | `Map<String,Integer>` |
| `subMetricsByLevel` | `Map<String,Map<String,Double>>` |
| `itemCount` | `int` |

#### ScoredItemRow (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `id` | `String` |
| `label` | `String` |
| `score` | `double` |
| `level` | `String` |

#### ImpactPreviewView (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `availability` | `ImpactPreviewAvailability` |
| `unavailabilityText` | `String` |
| `levels` | `List<LevelImpactView>` |

#### LevelImpactView (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `aggregateText` | `String` |
| `dimensionRows` | `List<DimensionDeltaView>` |

#### DimensionDeltaView (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `dimension` | `String` |
| `deltaText` | `String` |

#### InvalidWorkdirException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `path` | `String` |

#### InvalidApprovalModeException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `value` | `String` |

#### InvalidProposalStrategyException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `value` | `String` |
| `registered` | `String` |

#### InvalidLagenModeException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `value` | `String` |

#### InvalidLagenConfigException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `key` | `String` |
| `detail` | `String` |

#### UnsupportedLagenProviderException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `providerName` | `String` |

#### MissingLagenProviderInputException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `providerName` | `String` |
| `missingInput` | `String` |

#### UnreadableQuizSetOriginException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `origin` | `String` |
| `detail` | `String` |

#### AmbiguousQuizSetDeclarationException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `enumeratedIds` | `String` |
| `origin` | `String` |

### Module: nlp-infrastructure

#### NlpTokenizerConfig

**Package:** `com.learney.contentaudit.nlpinfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `pythonScriptPath` | `String` |  |
| `cocaDataPath` | `String` |  |
| `timeoutSeconds` | `int` |  |

**Generated constructor:**
```java
new NlpTokenizerConfig(String pythonScriptPath, String cocaDataPath, int timeoutSeconds)
```

### Module: revision-domain

#### RevisionVerdict

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `APPROVED` | `null` |  |
| `REJECTED` | `null` |  |
| `PENDING_APPROVAL` | `null` |  |

**Generated constructor:**
```java
new RevisionVerdict(null APPROVED, null REJECTED, null PENDING_APPROVAL)
```

#### RevisionOutcomeKind

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `APPROVED_APPLIED` | `null` |  |
| `APPROVED_APPLY_FAILED` | `null` |  |
| `REJECTED` | `null` |  |
| `NO_REVISER` | `null` |  |
| `CONTEXT_UNAVAILABLE` | `null` |  |
| `ELEMENT_NOT_FOUND` | `null` |  |
| `PENDING_APPROVAL_PERSISTED` | `null` |  |
| `ALREADY_PENDING_DECISION` | `null` |  |
| `NO_ACTIVE_STRATEGY` | `null` |  |
| `STRATEGY_FAILED` | `null` |  |
| `OVERRIDE_INVALID` | `null` |  |
| `OVERRIDE_NOT_APPLICABLE` | `null` |  |
| `DIAGNOSIS_NOT_SUSTAINED` | `null` |  |
| `NO_ACCEPTABLE_CANDIDATE` | `null` |  |
| `MULTIPLE_CHOICE_UNSUPPORTED` | `null` |  |

**Generated constructor:**
```java
new RevisionOutcomeKind(null APPROVED_APPLIED, null APPROVED_APPLY_FAILED, null REJECTED, null NO_REVISER, null CONTEXT_UNAVAILABLE, null ELEMENT_NOT_FOUND, null PENDING_APPROVAL_PERSISTED, null ALREADY_PENDING_DECISION, null NO_ACTIVE_STRATEGY, null STRATEGY_FAILED, null OVERRIDE_INVALID, null OVERRIDE_NOT_APPLICABLE, null DIAGNOSIS_NOT_SUSTAINED, null NO_ACCEPTABLE_CANDIDATE, null MULTIPLE_CHOICE_UNSUPPORTED)
```

#### CourseElementSnapshot

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `nodeTarget` | `AuditTarget` |  |
| `nodeId` | `String` |  |
| `quiz` | `QuizTemplateEntity` |  |
| `knowledge` | `KnowledgeEntity` |  |

**Generated constructor:**
```java
new CourseElementSnapshot(AuditTarget nodeTarget, String nodeId, QuizTemplateEntity quiz, KnowledgeEntity knowledge)
```

#### RevisionProposal

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `proposalId` | `String` |  |
| `taskId` | `String` |  |
| `planId` | `String` |  |
| `sourceAuditId` | `String` |  |
| `diagnosisKind` | `DiagnosisKind` |  |
| `nodeTarget` | `AuditTarget` |  |
| `nodeId` | `String` |  |
| `elementBefore` | `CourseElementSnapshot` |  |
| `elementAfter` | `CourseElementSnapshot` |  |
| `rationale` | `String` |  |
| `reviserKind` | `String` |  |
| `createdAt` | `Instant` |  |
| `strategyId` | `StrategyId` |  |
| `unmetCriteria` | `List<CriterionVerdict>` | Import `java.util.List` |
| `lengthMeasurement` | `CandidateLengthMeasurement` |  |

**Generated constructor:**
```java
new RevisionProposal(String proposalId, String taskId, String planId, String sourceAuditId, DiagnosisKind diagnosisKind, AuditTarget nodeTarget, String nodeId, CourseElementSnapshot elementBefore, CourseElementSnapshot elementAfter, String rationale, String reviserKind, Instant createdAt, StrategyId strategyId, List<CriterionVerdict> unmetCriteria, CandidateLengthMeasurement lengthMeasurement)
```

#### RevisionArtifact

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `proposal` | `RevisionProposal` |  |
| `verdict` | `RevisionVerdict` |  |
| `rejectionReason` | `String` |  |
| `outcome` | `RevisionOutcomeKind` |  |
| `decidedAt` | `Instant` |  |
| `decisionNote` | `String` |  |
| `contextSource` | `CorrectionContextSource` |  |
| `contextOverridePayload` | `String` |  |

**Generated constructor:**
```java
new RevisionArtifact(RevisionProposal proposal, RevisionVerdict verdict, String rejectionReason, RevisionOutcomeKind outcome, Instant decidedAt, String decisionNote, CorrectionContextSource contextSource, String contextOverridePayload)
```

#### RevisionOutcome

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `RevisionOutcomeKind` |  |
| `artifact` | `RevisionArtifact` |  |
| `errorMessage` | `String` |  |

**Generated constructor:**
```java
new RevisionOutcome(RevisionOutcomeKind kind, RevisionArtifact artifact, String errorMessage)
```

#### RevisionEngineConfig

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `revisers` | `Map<DiagnosisKind,Reviser>` |  |
| `validator` | `RevisionValidator` |  |
| `artifactStore` | `RevisionArtifactStore` |  |
| `courseRepository` | `CourseRepository` |  |
| `elementLocator` | `CourseElementLocator` |  |
| `refinementPlanStore` | `RefinementPlanStore` |  |
| `auditReportStore` | `AuditReportStore` |  |
| `contextResolver` | `CorrectionContextResolver<CorrectionContext>` |  |
| `lemmaAbsenceStrategyRegistry` | `LemmaAbsenceProposalStrategyRegistry` |  |
| `lemmaAbsenceProposalDeriver` | `LemmaAbsenceProposalDeriver` |  |
| `courseMapper` | `CourseMapper` |  |
| `auditEngine` | `AuditEngine` |  |
| `impactPreviewStore` | `ImpactPreviewStore` |  |
| `correctionContextOverrideParser` | `CorrectionContextOverrideParser` |  |
| `knowledgeTitleStrategyRegistry` | `KnowledgeTitleProposalStrategyRegistry` |  |
| `knowledgeTitleProposalDeriver` | `KnowledgeTitleProposalDeriver` |  |
| `quizInstructionStrategyRegistry` | `QuizInstructionProposalStrategyRegistry` |  |
| `candidateAssessor` | `CandidateAssessor` |  |
| `quizInstructionComplianceChecker` | `QuizInstructionComplianceChecker` |  |
| `quizInstructionSubjectViewFactory` | `QuizInstructionSubjectViewFactory` |  |
| `quizInstructionCorrectionConfig` | `QuizInstructionCorrectionConfig` |  |

**Generated constructor:**
```java
new RevisionEngineConfig(Map<DiagnosisKind,Reviser> revisers, RevisionValidator validator, RevisionArtifactStore artifactStore, CourseRepository courseRepository, CourseElementLocator elementLocator, RefinementPlanStore refinementPlanStore, AuditReportStore auditReportStore, CorrectionContextResolver<CorrectionContext> contextResolver, LemmaAbsenceProposalStrategyRegistry lemmaAbsenceStrategyRegistry, LemmaAbsenceProposalDeriver lemmaAbsenceProposalDeriver, CourseMapper courseMapper, AuditEngine auditEngine, ImpactPreviewStore impactPreviewStore, CorrectionContextOverrideParser correctionContextOverrideParser, KnowledgeTitleProposalStrategyRegistry knowledgeTitleStrategyRegistry, KnowledgeTitleProposalDeriver knowledgeTitleProposalDeriver, QuizInstructionProposalStrategyRegistry quizInstructionStrategyRegistry, CandidateAssessor candidateAssessor, QuizInstructionComplianceChecker quizInstructionComplianceChecker, QuizInstructionSubjectViewFactory quizInstructionSubjectViewFactory, QuizInstructionCorrectionConfig quizInstructionCorrectionConfig)
```

#### ApprovalMode

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `AUTO` | `null` |  |
| `HUMAN` | `null` |  |

**Generated constructor:**
```java
new ApprovalMode(null AUTO, null HUMAN)
```

#### ProposalDecisionOutcomeKind

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `APPROVED_APPLIED` | `null` |  |
| `APPROVED_APPLY_FAILED` | `null` |  |
| `REJECTED` | `null` |  |
| `NOT_FOUND` | `null` |  |
| `ALREADY_DECIDED` | `null` |  |
| `PRESERVATION_VIOLATED` | `null` |  |
| `MULTIPLE_CHOICE_UNSUPPORTED` | `null` |  |

**Generated constructor:**
```java
new ProposalDecisionOutcomeKind(null APPROVED_APPLIED, null APPROVED_APPLY_FAILED, null REJECTED, null NOT_FOUND, null ALREADY_DECIDED, null PRESERVATION_VIOLATED, null MULTIPLE_CHOICE_UNSUPPORTED)
```

#### ProposalDecisionOutcome

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `ProposalDecisionOutcomeKind` |  |
| `artifact` | `RevisionArtifact` |  |
| `errorMessage` | `String` |  |

**Generated constructor:**
```java
new ProposalDecisionOutcome(ProposalDecisionOutcomeKind kind, RevisionArtifact artifact, String errorMessage)
```

#### StrategyId

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `name` | `String` |  |
| `version` | `String` |  |
| `providerId` | `String` |  |

**Generated constructor:**
```java
new StrategyId(String name, String version, String providerId)
```

#### LemmaAbsenceQuizCandidate

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `quizSentence` | `String` |  |
| `translation` | `String` |  |

**Generated constructor:**
```java
new LemmaAbsenceQuizCandidate(String quizSentence, String translation)
```

#### ProposalStrategyFailedException

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"La estrategia de propuesta '%s' no pudo generar un candidato de quiz para la tarea '%s': %s"`

| Field | Type | Notes |
|-------|------|-------|
| `strategyName` | `String` |  |
| `taskId` | `String` |  |
| `reason` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new ProposalStrategyFailedException(String strategyName, String taskId, String reason)
```

#### ProposalDerivationException

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"No se pudo derivar elementAfter desde el candidato de la estrategia '%s' en la tarea '%s': %s"`

| Field | Type | Notes |
|-------|------|-------|
| `strategyName` | `String` |  |
| `taskId` | `String` |  |
| `reason` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new ProposalDerivationException(String strategyName, String taskId, String reason)
```

#### ConsolidatedViewBuilderConfig

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |  |
| `auditReportStore` | `AuditReportStore` |  |
| `refinementPlanStore` | `RefinementPlanStore` |  |
| `revisionArtifactStore` | `RevisionArtifactStore` |  |
| `courseRepository` | `CourseRepository` |  |
| `courseElementLocator` | `CourseElementLocator` |  |
| `courseMapper` | `CourseMapper` |  |
| `auditEngine` | `AuditEngine` |  |
| `nodeFieldDiffer` | `NodeFieldDiffer` |  |

**Generated constructor:**
```java
new ConsolidatedViewBuilderConfig(ActiveAnalysisSelectionStore activeAnalysisSelectionStore, AuditReportStore auditReportStore, RefinementPlanStore refinementPlanStore, RevisionArtifactStore revisionArtifactStore, CourseRepository courseRepository, CourseElementLocator courseElementLocator, CourseMapper courseMapper, AuditEngine auditEngine, NodeFieldDiffer nodeFieldDiffer)
```

#### CorrectionContextSource

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `DERIVED` | `null` |  |
| `OVERRIDE` | `null` |  |

**Generated constructor:**
```java
new CorrectionContextSource(null DERIVED, null OVERRIDE)
```

#### CorrectionContextOverride

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `context` | `CorrectionContext` |  |
| `rawPayload` | `String` |  |

**Generated constructor:**
```java
new CorrectionContextOverride(CorrectionContext context, String rawPayload)
```

#### OverrideRejectedException

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"correctionContext override rejected: %s"`

| Field | Type | Notes |
|-------|------|-------|
| `reason` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new OverrideRejectedException(String reason)
```

#### DiagnosisNotSustainedException

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"El diagnostico de consigna del ejercicio '%s' ya no se sostiene: la validacion vigente declara que cumple"`

| Field | Type | Notes |
|-------|------|-------|
| `quizId` | `String` |  |
| `taskId` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new DiagnosisNotSustainedException(String quizId, String taskId)
```

#### NoAcceptableCandidateException

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** exception
**Extends:** `RuntimeException`
**Message:** `"No se logro una correccion aceptable para el ejercicio '%s': reprobaron %s"`

| Field | Type | Notes |
|-------|------|-------|
| `quizId` | `String` |  |
| `failedCriteria` | `List<CorrectionCriterion>` | Import `java.util.List` |
| `taskId` | `String` |  |

**Generated class** extends `RuntimeException` with constructor:
```java
new NoAcceptableCandidateException(String quizId, List<CorrectionCriterion> failedCriteria, String taskId)
```

#### LemmaAbsenceProposalStrategyRegistryConfig (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `registered` | `List<LemmaAbsenceProposalStrategy>` |
| `activeName` | `String` |

#### KnowledgeTitleProposalStrategyRegistryConfig (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `registered` | `List<KnowledgeTitleProposalStrategy>` |
| `activeName` | `String` |

#### QuizInstructionProposalStrategyRegistryConfig (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `strategies` | `List<QuizInstructionProposalStrategy>` |
| `activeStrategyName` | `String` |

#### AppliedProposals (package: engine)

**Package:** `com.learney.contentaudit.revisiondomain.engine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `course` | `CourseEntity` |
| `applied` | `List<RevisionProposal>` |
| `nonApplicable` | `List<NonApplicablePending>` |

#### LemmaAbsenceGeneratorResponse (package: lemmaabsence)

**Package:** `com.learney.contentaudit.revisiondomain.lemmaabsence`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizSentence` | `String` |
| `translation` | `String` |

#### ImpactPreviewAvailability (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `AVAILABLE` | `null` |
| `UNAVAILABLE` | `null` |

#### ImpactPreviewUnavailabilityReason (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `TARGET_NODE_ABSENT` | `null` |
| `BASE_AUDIT_UNAVAILABLE` | `null` |
| `SIMULATION_FAILED` | `null` |
| `OTHER` | `null` |

#### ImpactPreviewUnavailability (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `reason` | `ImpactPreviewUnavailabilityReason` |
| `detail` | `String` |

#### ScoreDelta (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `before` | `double` |
| `after` | `double` |
| `difference` | `double` |

#### DimensionDelta (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `dimension` | `String` |
| `delta` | `ScoreDelta` |

#### LevelImpact (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `aggregateDelta` | `ScoreDelta` |
| `dimensionDeltas` | `List<DimensionDelta>` |

#### ImpactPreview (package: impactpreview)

**Package:** `com.learney.contentaudit.revisiondomain.impactpreview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `computedAt` | `Instant` |
| `availability` | `ImpactPreviewAvailability` |
| `unavailability` | `ImpactPreviewUnavailability` |
| `levelImpacts` | `List<LevelImpact>` |

#### ConsolidatedViewAvailability (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `AVAILABLE` | `null` |
| `UNAVAILABLE` | `null` |

#### ConsolidatedViewUnavailabilityReason (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `NO_ACTIVE_ANALYSIS` | `null` |
| `ACTIVE_PLAN_UNAVAILABLE` | `null` |
| `INCONSISTENT_PROPOSAL` | `null` |
| `REAGGREGATION_FAILED` | `null` |
| `OTHER` | `null` |

#### ConsolidatedViewUnavailability (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `reason` | `ConsolidatedViewUnavailabilityReason` |
| `detail` | `String` |

#### NonApplicablePendingReason (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `NODE_ABSENT` | `null` |
| `ELEMENT_BEFORE_MISMATCH` | `null` |
| `OTHER` | `null` |

#### NonApplicablePending (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `reason` | `NonApplicablePendingReason` |
| `detail` | `String` |

#### NodeImpact (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `acceptedProposalIds` | `List<String>` |
| `fieldChanges` | `Map<String,FieldChange>` |
| `pendingProposalIds` | `List<String>` |

#### ConsolidatedView (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `activeAuditId` | `String` |
| `activePlanId` | `String` |
| `availability` | `ConsolidatedViewAvailability` |
| `unavailability` | `ConsolidatedViewUnavailability` |
| `computedAt` | `Instant` |
| `nodeImpacts` | `List<NodeImpact>` |
| `nonApplicablePendings` | `List<NonApplicablePending>` |

#### FieldChange (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `original` | `Object` |
| `consolidated` | `Object` |
| `pendingProjection` | `Object` |

#### FieldPath (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `raw` | `String` |

#### FieldExclusionRole (package: consolidatedview)

**Package:** `com.learney.contentaudit.revisiondomain.consolidatedview`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `OPAQUE_PERSISTENCE_ID` | `null` |
| `STRUCTURAL_REFERENCE` | `null` |
| `TIMESTAMP` | `null` |
| `INTERNAL_ORDER` | `null` |
| `NON_SEMANTIC` | `null` |

#### FieldExclusionRule (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `declaringTypeName` | `String` |
| `fieldName` | `String` |
| `role` | `FieldExclusionRole` |

#### ListIdentityKeySpec (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `fieldNames` | `List<String>` |

#### ListIdentityRule (package: fielddiff)

**Package:** `com.learney.contentaudit.revisiondomain.fielddiff`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `declaringTypeName` | `String` |
| `fieldName` | `String` |
| `keySpec` | `ListIdentityKeySpec` |

#### KnowledgeTitleCandidate (package: knowledgetitle)

**Package:** `com.learney.contentaudit.revisiondomain.knowledgetitle`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `title` | `String` |
| `instructions` | `String` |

#### KnowledgeTitleGeneratorResponse (package: knowledgetitle)

**Package:** `com.learney.contentaudit.revisiondomain.knowledgetitle`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `title` | `String` |
| `instructions` | `String` |

#### AttributeState (package: preservation)

**Package:** `com.learney.contentaudit.revisiondomain.preservation`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `VALUE_PRESENT` | `null` |
| `EMPTY_PRESENT` | `null` |
| `ABSENT` | `null` |

#### PreservationViolation (package: preservation)

**Package:** `com.learney.contentaudit.revisiondomain.preservation`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `elementId` | `String` |
| `path` | `String` |
| `beforeState` | `AttributeState` |
| `afterState` | `AttributeState` |
| `beforeValue` | `String` |
| `afterValue` | `String` |

#### RepairReport (package: preservation)

**Package:** `com.learney.contentaudit.revisiondomain.preservation`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `course` | `CourseEntity` |
| `elementsInspected` | `int` |
| `elementsRepaired` | `int` |
| `restored` | `List<PreservationViolation>` |
| `unrepairable` | `List<String>` |

#### ScopedField (package: preservation)

**Package:** `com.learney.contentaudit.revisiondomain.preservation`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `path` | `String` |
| `elementKind` | `String` |

#### CorrectionCriterion (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `SCOPED_EDIT` | `null` |
| `LENGTH_IN_RANGE` | `null` |
| `LEVEL_VOCABULARY` | `null` |
| `DISTINCTNESS` | `null` |
| `FAITHFUL_TRANSLATION` | `null` |
| `SOLVABLE_SAME_KIND` | `null` |
| `INSTRUCTION_COMPLIANCE` | `null` |
| `REAL_WORLD_SENSE` | `null` |

#### CriterionOutcome (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `PASSED` | `null` |
| `FAILED` | `null` |
| `NOT_EVALUABLE` | `null` |

#### CriterionVerdict (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `criterion` | `CorrectionCriterion` |
| `outcome` | `CriterionOutcome` |
| `detail` | `String` |

#### CandidateAssessmentInput (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `subjectRef` | `String` |
| `sourceAuditId` | `String` |
| `original` | `CourseElementSnapshot` |
| `candidate` | `CourseElementSnapshot` |
| `cefrLevel` | `CefrLevel` |
| `topicLabel` | `String` |
| `knowledgeTitle` | `String` |
| `knowledgeInstructions` | `String` |
| `cefrLevelLabel` | `String` |
| `sentenceMode` | `SentenceMode` |
| `siblingQuizSentences` | `List<String>` |
| `signalledFragments` | `List<String>` |

#### CandidateAssessment (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `verdicts` | `List<CriterionVerdict>` |
| `eligible` | `boolean` |
| `realChange` | `boolean` |
| `satisfiedCriteria` | `int` |
| `unmetCriteria` | `List<CriterionVerdict>` |
| `lengthMeasurement` | `CandidateLengthMeasurement` |
| `rankKey` | `String` |

#### QuizSimilarity (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `quizSentence` | `String` |
| `score` | `double` |

#### CandidateCriteriaConfig (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `tokenizer` | `NlpTokenizer` |
| `sentenceLengthConfig` | `SentenceLengthConfig` |
| `lexicalEvaluator` | `SentenceLexicalEvaluator` |
| `complianceChecker` | `QuizInstructionComplianceChecker` |
| `subjectViewFactory` | `QuizInstructionSubjectViewFactory` |
| `judgmentEvaluators` | `List<CandidateCriterionEvaluator>` |
| `siblingSimilarityThreshold` | `Double` |
| `courseSimilarityThreshold` | `Double` |
| `courseWideDistinctness` | `Boolean` |
| `auditReportStore` | `AuditReportStore` |
| `realWorldSenseEvaluator` | `CandidateCriterionEvaluator` |

#### CandidateCriteriaDefaults (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `siblingSimilarityThreshold` | `double` |
| `courseSimilarityThreshold` | `double` |
| `courseWideDistinctness` | `boolean` |

#### CandidateLengthMeasurement (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `tokens` | `int` |
| `targetMin` | `int` |
| `targetMax` | `int` |
| `inRange` | `boolean` |
| `measured` | `boolean` |

#### AbsoluteCriterionEvaluatorMissingException (package: candidatecriteria)

**Package:** `com.learney.contentaudit.revisiondomain.candidatecriteria`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `criterion` | `CorrectionCriterion` |

#### QuizInstructionGeneratorResponse (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizSentence` | `String` |
| `translation` | `String` |
| `rationale` | `String` |
| `verdicts` | `List<CriterionVerdict>` |
| `attempts` | `int` |

#### QuizInstructionGenerationRequest (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `context` | `QuizInstructionCorrectionContext` |
| `maxAttempts` | `int` |

#### QuizInstructionCorrectionConfig (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `maxAttempts` | `Integer` |
| `maxCorrectionsPerRun` | `Integer` |

#### QuizInstructionCorrectionDefaults (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `maxAttempts` | `int` |
| `maxCorrectionsPerRun` | `int` |

#### QuizInstructionCorrectionRunRequest (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `planId` | `String` |
| `coursePath` | `Path` |
| `maxCorrections` | `Integer` |

#### QuizInstructionTaskOutcomeKind (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `PROPOSED` | `null` |
| `NOT_CORRECTED` | `null` |
| `DIAGNOSIS_STALE` | `null` |
| `NOT_ATTEMPTED` | `null` |
| `FAILED` | `null` |
| `MULTIPLE_CHOICE_UNSUPPORTED` | `null` |

#### QuizInstructionTaskOutcome (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `taskId` | `String` |
| `nodeId` | `String` |
| `kind` | `QuizInstructionTaskOutcomeKind` |
| `failedCriteria` | `List<CorrectionCriterion>` |
| `detail` | `String` |
| `proposalId` | `String` |
| `attempts` | `int` |
| `unmetCriteria` | `List<CriterionVerdict>` |
| `outOfLengthRange` | `boolean` |

#### QuizInstructionCorrectionRunReport (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `runId` | `String` |
| `planId` | `String` |
| `sourceAuditId` | `String` |
| `startedAt` | `Instant` |
| `finishedAt` | `Instant` |
| `maxCorrections` | `int` |
| `eligibleTasks` | `int` |
| `attempted` | `int` |
| `proposed` | `int` |
| `notCorrected` | `int` |
| `diagnosisStale` | `int` |
| `failed` | `int` |
| `notAttempted` | `List<String>` |
| `outcomes` | `List<QuizInstructionTaskOutcome>` |
| `proposedWithUnmetCriteria` | `int` |
| `proposedOutOfLengthRange` | `int` |
| `multipleChoiceUnsupported` | `int` |

#### QuizInstructionBestCandidate (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `candidate` | `LemmaAbsenceQuizCandidate` |
| `reportedVerdicts` | `List<CriterionVerdict>` |
| `attempts` | `int` |

#### QuizInstructionCandidateAssessorConfig (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `assessor` | `CandidateAssessor` |
| `deriver` | `LemmaAbsenceProposalDeriver` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `auditReportStore` | `AuditReportStore` |
| `contextResolver` | `CorrectionContextResolver<CorrectionContext>` |
| `courseRepository` | `CourseRepository` |
| `elementLocator` | `CourseElementLocator` |

#### CandidateAssessmentUnavailableException (package: quizinstruction)

**Package:** `com.learney.contentaudit.revisiondomain.quizinstruction`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `taskId` | `String` |
| `reason` | `String` |

#### ProjectionScope (package: projection)

**Package:** `com.learney.contentaudit.revisiondomain.projection`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `scopeId` | `String` |
| `proposalIds` | `List<String>` |
| `analyzerBudgets` | `Map<String,Integer>` |

#### ProjectedChange (package: projection)

**Package:** `com.learney.contentaudit.revisiondomain.projection`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `pending` | `boolean` |

#### CourseProjection (package: projection)

**Package:** `com.learney.contentaudit.revisiondomain.projection`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `activeAuditId` | `String` |
| `activePlanId` | `String` |
| `scopeId` | `String` |
| `availability` | `ConsolidatedViewAvailability` |
| `unavailability` | `ConsolidatedViewUnavailability` |
| `original` | `AuditReport` |
| `approved` | `AuditReport` |
| `projected` | `AuditReport` |
| `changes` | `List<ProjectedChange>` |
| `nonApplicablePendings` | `List<NonApplicablePending>` |

#### SavedProjection (package: projection)

**Package:** `com.learney.contentaudit.revisiondomain.projection`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `projectionId` | `String` |
| `activeAuditId` | `String` |
| `activePlanId` | `String` |
| `scopeId` | `String` |
| `savedAt` | `Instant` |
| `appliedProposalIds` | `List<String>` |
| `nonApplicablePendings` | `List<NonApplicablePending>` |

#### CourseProjectorConfig (package: projection)

**Package:** `com.learney.contentaudit.revisiondomain.projection`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `auditReportStore` | `AuditReportStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `revisionArtifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `courseMapper` | `CourseMapper` |
| `auditEngine` | `AuditEngine` |

#### GoalKind (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `FLOOR` | `null` |
| `PROGRESS` | `null` |

#### GoalUnit (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `POINTS` | `null` |
| `EXERCISES` | `null` |
| `PERCENT` | `null` |

#### GoalReference (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `BASELINE` | `null` |
| `BATCH_START` | `null` |
| `ABSOLUTE` | `null` |

#### GoalOrigin (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `DECLARED` | `null` |
| `FAMILY_DEFAULT` | `null` |

#### SampleCriterion (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `OBSERVED` | `null` |
| `BOUND` | `null` |

#### Goal (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `id` | `String` |
| `kind` | `GoalKind` |
| `subject` | `String` |
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `threshold` | `double` |
| `unit` | `GoalUnit` |
| `reference` | `GoalReference` |
| `origin` | `GoalOrigin` |

#### FamilyFloor (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `family` | `AnalyzerFamily` |
| `threshold` | `double` |
| `unit` | `GoalUnit` |
| `reference` | `GoalReference` |
| `nodeTargets` | `List<AuditTarget>` |

#### BlindSampleGoal (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `size` | `int` |
| `minBetterOrEqual` | `int` |
| `maxWorse` | `int` |
| `criterion` | `SampleCriterion` |
| `responsible` | `String` |
| `secondReviewerPairs` | `int` |
| `cumulativeMaxWorseShare` | `double` |

#### GoalDeclaration (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `familyFloors` | `List<FamilyFloor>` |
| `goals` | `List<Goal>` |
| `blindSample` | `BlindSampleGoal` |

#### EffectiveGoals (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `fingerprint` | `String` |
| `goals` | `List<Goal>` |
| `blindSample` | `BlindSampleGoal` |

#### SealedGoals (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sealedAt` | `Instant` |
| `goals` | `EffectiveGoals` |

#### GoalChange (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `goalId` | `String` |
| `before` | `String` |
| `after` | `String` |

#### GoalStatus (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `MET` | `null` |
| `NOT_MET` | `null` |
| `UNDECIDABLE` | `null` |

#### RowValue (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `value` | `Double` |
| `share` | `Double` |
| `notEvaluated` | `int` |

#### RowGoalStatus (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `goalId` | `String` |
| `threshold` | `double` |
| `unit` | `GoalUnit` |
| `reference` | `GoalReference` |
| `status` | `GoalStatus` |

#### ComparisonRow (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `subject` | `String` |
| `family` | `AnalyzerFamily` |
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `label` | `String` |
| `baseline` | `RowValue` |
| `remeasuredBaseline` | `RowValue` |
| `approved` | `RowValue` |
| `projected` | `RowValue` |
| `change` | `Double` |
| `unit` | `GoalUnit` |
| `floor` | `RowGoalStatus` |
| `progress` | `RowGoalStatus` |
| `displayName` | `String` |
| `betterWhen` | `BetterDirection` |
| `changeVerdict` | `ChangeVerdict` |
| `explanation` | `String` |
| `example` | `RowExample` |

#### ChangeOutcome (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `returns` | `boolean` |
| `newFindings` | `List<Finding>` |
| `fixedFindings` | `List<Finding>` |
| `undecidedAnalyzers` | `List<String>` |

#### ChangeAttribution (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `nodeId` | `String` |
| `effect` | `double` |
| `unit` | `GoalUnit` |

#### FloorBreach (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `goalId` | `String` |
| `row` | `ComparisonRow` |
| `attributions` | `List<ChangeAttribution>` |

#### MeasureChange (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `baselineFingerprint` | `String` |
| `currentFingerprint` | `String` |
| `baselineConfig` | `Map<String,Object>` |
| `currentConfig` | `Map<String,Object>` |

#### VerdictKind (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `PASSES` | `null` |
| `FAILS` | `null` |
| `BLIND_SAMPLE_MISSING` | `null` |
| `UNDECIDABLE` | `null` |

#### BatchVerdict (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `kind` | `VerdictKind` |
| `brokenFloorGoalIds` | `List<String>` |
| `returningProposalIds` | `List<String>` |
| `sample` | `SampleOutcome` |
| `detail` | `String` |

#### BatchComparison (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `baselineAuditId` | `String` |
| `activeAuditId` | `String` |
| `activePlanId` | `String` |
| `scopeId` | `String` |
| `availability` | `ConsolidatedViewAvailability` |
| `unavailability` | `ConsolidatedViewUnavailability` |
| `computedAt` | `Instant` |
| `goals` | `EffectiveGoals` |
| `goalsSealedAt` | `Instant` |
| `goalChanges` | `List<GoalChange>` |
| `measureChanges` | `List<MeasureChange>` |
| `appliedProposalIds` | `List<String>` |
| `nonApplicablePendings` | `List<NonApplicablePending>` |
| `rows` | `List<ComparisonRow>` |
| `changes` | `List<ChangeOutcome>` |
| `breaches` | `List<FloorBreach>` |
| `verdict` | `BatchVerdict` |

#### BetterDirection (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `LOWER` | `null` |
| `HIGHER` | `null` |

#### ChangeVerdict (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `IMPROVED` | `null` |
| `WORSENED` | `null` |
| `UNCHANGED` | `null` |
| `NOT_EVALUATED` | `null` |

#### RowExample (package: comparison)

**Package:** `com.learney.contentaudit.revisiondomain.comparison`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `exercise` | `String` |
| `detail` | `String` |

#### PairAnswer (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `A_BETTER` | `null` |
| `B_BETTER` | `null` |
| `EQUAL` | `null` |
| `BOTH_WRONG` | `null` |

#### BlindSide (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `A` | `null` |
| `B` | `null` |

#### StudentSide (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `view` | `QuizStudentView` |
| `answered` | `String` |

#### BlindPair (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `proposalId` | `String` |
| `nodeId` | `String` |
| `sideA` | `StudentSide` |
| `sideB` | `StudentSide` |
| `afterSide` | `BlindSide` |

#### BlindPairView (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `sideA` | `StudentSide` |
| `sideB` | `StudentSide` |

#### PairAnswerRecord (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `pairIndex` | `int` |
| `answer` | `PairAnswer` |
| `reviewer` | `String` |
| `answeredAt` | `Instant` |

#### ProportionEstimate (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `count` | `int` |
| `total` | `int` |
| `share` | `double` |
| `lower95` | `double` |
| `upper95` | `double` |
| `upperOneSided95` | `double` |

#### ReviewerAgreement (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `responsible` | `String` |
| `other` | `String` |
| `commonPairs` | `int` |
| `agreeing` | `int` |
| `kappa` | `double` |

#### CumulativeSample (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `samples` | `int` |
| `pairs` | `int` |
| `worse` | `int` |
| `worseUpperBound` | `double` |
| `belowGoal` | `boolean` |

#### BlindSample (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sampleId` | `String` |
| `scopeId` | `String` |
| `baselineAuditId` | `String` |
| `activeAuditId` | `String` |
| `activePlanId` | `String` |
| `seed` | `long` |
| `drawnAt` | `Instant` |
| `goal` | `BlindSampleGoal` |
| `pairs` | `List<BlindPair>` |
| `answers` | `List<PairAnswerRecord>` |
| `closedAt` | `Instant` |

#### BlindSampleResult (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sampleId` | `String` |
| `scopeId` | `String` |
| `responsible` | `String` |
| `answered` | `int` |
| `betterOrEqual` | `ProportionEstimate` |
| `worse` | `ProportionEstimate` |
| `bothWrong` | `ProportionEstimate` |
| `meetsGoal` | `boolean` |
| `unmetParts` | `List<String>` |
| `agreement` | `ReviewerAgreement` |
| `cumulative` | `CumulativeSample` |
| `revealedPairs` | `List<BlindPair>` |

#### SampleClosedException (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `sampleId` | `String` |

#### SampleOutcome (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sampleId` | `String` |
| `scopeId` | `String` |
| `closed` | `boolean` |
| `answered` | `int` |
| `betterOrEqual` | `int` |
| `worse` | `int` |
| `bothWrong` | `int` |
| `meetsGoal` | `boolean` |
| `detail` | `String` |

#### ErrorRateItem (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `quizNodeId` | `String` |
| `fromBaseline` | `boolean` |
| `side` | `StudentSide` |

#### ErrorRateItemView (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `index` | `int` |
| `side` | `StudentSide` |

#### ErrorMark (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `itemIndex` | `int` |
| `hasError` | `boolean` |
| `reviewer` | `String` |
| `markedAt` | `Instant` |

#### ErrorRateSample (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sampleId` | `String` |
| `nodeTarget` | `AuditTarget` |
| `nodeId` | `String` |
| `size` | `int` |
| `seed` | `long` |
| `drawnAt` | `Instant` |
| `baselineAuditId` | `String` |
| `activeAuditId` | `String` |
| `items` | `List<ErrorRateItem>` |
| `marks` | `List<ErrorMark>` |
| `closedAt` | `Instant` |

#### ErrorRateResult (package: blindsample)

**Package:** `com.learney.contentaudit.revisiondomain.blindsample`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `sampleId` | `String` |
| `before` | `ProportionEstimate` |
| `after` | `ProportionEstimate` |
| `analyzersBefore` | `ProportionEstimate` |
| `analyzersAfter` | `ProportionEstimate` |
| `analyzersWithinMargin` | `boolean` |

#### UnitMeasurement (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `findings` | `List<Finding>` |
| `unevaluated` | `List<UnevaluatedMark>` |
| `redWords` | `List<RedWord>` |
| `spokenTexts` | `List<SpokenText>` |
| `lengths` | `List<LengthDeclaration>` |

#### UnevaluatedMark (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `nodeId` | `String` |
| `analyzer` | `String` |

#### RedWord (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `lemma` | `String` |
| `level` | `CefrLevel` |
| `outOfCatalog` | `boolean` |
| `sentence` | `String` |

#### SpokenText (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `audioKey` | `String` |
| `texts` | `List<String>` |

#### LengthDeclaration (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `tokens` | `int` |
| `targetMin` | `int` |
| `targetMax` | `int` |
| `inRange` | `boolean` |

#### AudioRerecordRequest (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `audioKey` | `String` |
| `newTexts` | `List<String>` |

#### ChangeProbe (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `changedQuizIds` | `List<String>` |
| `topicLevelChange` | `boolean` |
| `before` | `UnitMeasurement` |
| `after` | `UnitMeasurement` |
| `requestedRerecords` | `List<AudioRerecordRequest>` |

#### ChangeGateVerdict (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `passes` | `boolean` |
| `newErrors` | `List<Finding>` |
| `newRedWords` | `List<RedWord>` |
| `lengths` | `List<LengthDeclaration>` |
| `rerecords` | `List<AudioRerecordRequest>` |
| `notCompared` | `List<NotComparedCount>` |
| `reasons` | `List<String>` |

#### NotComparedCount (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `quizzes` | `int` |

#### ReturnCause (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `NEW_ERROR` | `null` |
| `NEW_RED_WORD` | `null` |
| `FLOOR` | `null` |

#### ReturnedChange (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `cause` | `ReturnCause` |
| `reason` | `String` |

#### BatchGateVerdict (package: nonregression)

**Package:** `com.learney.contentaudit.revisiondomain.nonregression`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `passes` | `boolean` |
| `verdictKind` | `VerdictKind` |
| `returned` | `List<ReturnedChange>` |
| `declaredLengthFindings` | `List<Finding>` |
| `changedOutsideScope` | `List<String>` |
| `listedExceptions` | `List<String>` |
| `notCompared` | `List<NotComparedCount>` |
| `reasons` | `List<String>` |

#### TopicWork (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `level` | `CefrLevel` |
| `milestoneId` | `String` |
| `topicId` | `String` |
| `knowledge` | `KnowledgeEntity` |
| `sentenceMode` | `SentenceMode` |
| `briefs` | `List<ErrorTaskBrief>` |
| `wordLights` | `TopicWordLights` |
| `conversion` | `ConversionOrder` |
| `attemptsUsed` | `Map<String,Integer>` |
| `carriedRejections` | `List<CandidateRejection>` |

#### ConversionOrder (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `targetKind` | `FormKind` |
| `optionCount` | `int` |

#### PanelRequest (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `level` | `CefrLevel` |
| `knowledgeId` | `String` |
| `quizId` | `String` |
| `topicView` | `List<QuizStudentView>` |
| `briefs` | `List<ErrorTaskBrief>` |
| `wordLights` | `TopicWordLights` |
| `conversion` | `ConversionOrder` |
| `attempt` | `int` |
| `previousRejections` | `List<CandidateRejection>` |
| `twoFixes` | `boolean` |

#### PanelCandidate (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `kind` | `PanelCandidateKind` |
| `quiz` | `QuizTemplateEntity` |
| `miniTheoryText` | `String` |
| `topicInstructions` | `String` |
| `rationale` | `String` |

#### PanelCandidateKind (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `WHOLE_EXERCISE` | `null` |
| `TRANSLATION_ONLY` | `null` |
| `ENGLISH_CHANGE` | `null` |
| `MINI_THEORY` | `null` |
| `MULTIPLE_CHOICE_CONVERSION` | `null` |

#### PanelProposal (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `candidates` | `List<PanelCandidate>` |
| `proposerVersion` | `String` |
| `available` | `boolean` |
| `unavailableReason` | `String` |

#### CandidateRejection (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `attempt` | `int` |
| `candidateKind` | `PanelCandidateKind` |
| `reasons` | `List<String>` |

#### PanelOutcomeKind (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `ACCEPTED` | `null` |
| `NO_LONGER_HOLDS` | `null` |
| `NO_WINNER` | `null` |
| `TIE` | `null` |
| `NOT_ATTEMPTED` | `null` |
| `NO_DECLARED_SCOPE` | `null` |

#### PanelDecision (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `quizIds` | `List<String>` |
| `taskIds` | `List<String>` |
| `outcome` | `PanelOutcomeKind` |
| `accepted` | `PanelCandidate` |
| `acceptedVerdict` | `ChangeGateVerdict` |
| `resolvedTaskIds` | `List<String>` |
| `attempts` | `int` |
| `rejections` | `List<CandidateRejection>` |
| `bestFailing` | `PanelCandidate` |
| `bestFailingVerdict` | `ChangeGateVerdict` |
| `tieFindings` | `List<Finding>` |
| `queriesSpent` | `int` |

#### PanelTopicResult (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `knowledgeId` | `String` |
| `decisions` | `List<PanelDecision>` |
| `queriesSpent` | `int` |
| `interrupted` | `boolean` |

#### PanelRunSettings (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `topicsAtOnce` | `int` |
| `queriesAtOnce` | `int` |
| `maxAttempts` | `int` |
| `analyzers` | `List<String>` |

#### PanelRunnerConfig (package: panel)

**Package:** `com.learney.contentaudit.revisiondomain.panel`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposer` | `PanelProposer` |
| `analyzerCatalog` | `AnalyzerCatalog` |
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |
| `auditEngine` | `AuditEngine` |
| `courseMapper` | `CourseMapper` |
| `quizStudentViewRenderer` | `QuizStudentViewRenderer` |
| `nlpTokenizer` | `NlpTokenizer` |
| `lexicalEvaluator` | `SentenceLexicalEvaluator` |
| `topicTeachingResolver` | `TopicTeachingResolver` |
| `spokenTextBuilder` | `SpokenTextBuilder` |
| `sentenceLengthConfig` | `SentenceLengthConfig` |

#### ScoredCandidate (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `candidate` | `PanelCandidate` |
| `measurement` | `UnitMeasurement` |
| `gate` | `ChangeGateVerdict` |
| `screenViolations` | `List<String>` |

#### ScriptDecision (package: panelengine)

**Package:** `com.learney.contentaudit.revisiondomain.panelengine`
**Visibility:** internal
**Type:** record

| Field | Type |
|-------|------|
| `outcome` | `PanelOutcomeKind` |
| `winnerIndex` | `int` |
| `resolvedTaskIds` | `List<String>` |
| `tieFindings` | `List<Finding>` |
| `reasons` | `List<String>` |

#### CorrectionBatch (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `id` | `String` |
| `level` | `CefrLevel` |
| `status` | `BatchStatus` |
| `baselineAuditId` | `String` |
| `startAuditId` | `String` |
| `planId` | `String` |
| `analyzers` | `List<String>` |
| `settings` | `BatchSettings` |
| `conversionOrders` | `List<ConversionOrder>` |
| `taskIds` | `List<String>` |
| `notTakenTaskIds` | `List<String>` |
| `unevaluated` | `List<NotComparedCount>` |
| `courseTies` | `List<Finding>` |
| `changes` | `List<BatchChange>` |
| `quizProposals` | `List<BatchProposalRef>` |
| `outcomes` | `List<BatchTaskOutcome>` |
| `panelDecisions` | `List<PanelDecision>` |
| `adjustments` | `List<BatchAdjustment>` |
| `closings` | `List<BatchClosing>` |
| `blindSampleId` | `String` |
| `queryCap` | `Integer` |
| `queriesSpent` | `int` |
| `openedAt` | `Instant` |
| `publication` | `BatchPublication` |
| `pendingRerecords` | `List<AudioRerecordRequest>` |

#### BatchStatus (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `OPEN` | `null` |
| `RUNNING` | `null` |
| `INTERRUPTED` | `null` |
| `CLOSED` | `null` |
| `PUBLISHED` | `null` |

#### BatchSettings (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `topicsAtOnce` | `int` |
| `queriesAtOnce` | `int` |
| `maxAttempts` | `int` |

#### BatchOpenRequest (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `level` | `CefrLevel` |
| `topicsAtOnce` | `Integer` |
| `queriesAtOnce` | `Integer` |
| `convertToMultipleChoice` | `List<String>` |

#### BatchRunRequest (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `queryCap` | `Integer` |

#### BatchEstimate (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `tasks` | `List<BatchTaskCount>` |
| `quizzes` | `int` |
| `topics` | `int` |
| `heaviestTopicQuizzes` | `int` |
| `conversionQuizzes` | `int` |
| `estimatedQueries` | `int` |
| `estimatedHours` | `double` |
| `topicsAtOnce` | `int` |
| `queriesAtOnce` | `int` |

#### BatchTaskCount (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `resolution` | `FindingResolution` |
| `tasks` | `int` |

#### BatchChange (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `changeId` | `String` |
| `origin` | `BatchChangeOrigin` |
| `ruleId` | `String` |
| `knowledgeId` | `String` |
| `taskIds` | `List<String>` |
| `steps` | `List<BatchChangeStep>` |
| `gateVerdict` | `ChangeGateVerdict` |
| `joseException` | `boolean` |
| `createdAt` | `Instant` |
| `droppedReason` | `String` |

#### BatchChangeOrigin (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `RULE` | `null` |
| `PANEL` | `null` |
| `CONVERSION` | `null` |
| `JOSE` | `null` |

#### BatchChangeStep (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `parts` | `List<ExercisePart>` |
| `after` | `CourseElementSnapshot` |

#### BatchProposalRef (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `quizId` | `String` |
| `proposalId` | `String` |

#### BatchTaskOutcome (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `taskId` | `String` |
| `kind` | `BatchTaskOutcomeKind` |
| `changeId` | `String` |
| `attempts` | `int` |
| `detail` | `String` |

#### BatchTaskOutcomeKind (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `PENDING` | `null` |
| `RESOLVED_BY_RULE` | `null` |
| `RESOLVED_BY_PANEL` | `null` |
| `NO_LONGER_HOLDS` | `null` |
| `COVERED` | `null` |
| `NO_WINNER` | `null` |
| `TIE` | `null` |
| `RETURNED_WITHOUT_ATTEMPTS` | `null` |
| `NO_DECLARED_SCOPE` | `null` |
| `PENDING_RERECORDING` | `null` |
| `NOT_ATTEMPTED` | `null` |

#### BatchAdjustment (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `kind` | `BatchAdjustmentKind` |
| `quizId` | `String` |
| `taskIds` | `List<String>` |
| `changeId` | `String` |
| `ruleId` | `String` |
| `candidate` | `PanelCandidate` |
| `finding` | `Finding` |
| `reason` | `String` |
| `decidedBy` | `String` |
| `decidedAt` | `Instant` |

#### BatchAdjustmentKind (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `TASK_FROM_TIE` | `null` |
| `ACCEPT_CANDIDATE` | `null` |
| `ACCEPT_AS_EXCEPTION` | `null` |
| `RETURN_TO_PANEL` | `null` |
| `KEEP_ORIGINAL` | `null` |
| `DROP_CHANGE` | `null` |
| `DROP_RULE` | `null` |
| `APPROVE_RULE_SAMPLE` | `null` |

#### BatchClosing (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `closedAt` | `Instant` |
| `scope` | `ProjectionScope` |
| `comparison` | `BatchComparison` |
| `gate` | `BatchGateVerdict` |
| `returnedChangeIds` | `List<String>` |
| `rounds` | `int` |

#### BatchReport (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `level` | `CefrLevel` |
| `status` | `BatchStatus` |
| `outcomes` | `List<OutcomeCount>` |
| `totalTasks` | `int` |
| `conversionDone` | `int` |
| `conversionNoWinner` | `int` |
| `errorsByAnalyzer` | `List<AnalyzerLevelErrors>` |
| `rerecords` | `List<AudioRerecordRequest>` |
| `lengths` | `List<LengthDeclaration>` |
| `queries` | `QueriesByRole` |
| `hours` | `double` |
| `joseDecisions` | `List<BatchAdjustment>` |

#### OutcomeCount (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `kind` | `BatchTaskOutcomeKind` |
| `tasks` | `int` |

#### AnalyzerLevelErrors (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `analyzer` | `String` |
| `level` | `CefrLevel` |
| `before` | `int` |
| `after` | `int` |

#### QueriesByRole (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `proposer` | `int` |
| `judges` | `int` |

#### BatchPublishRequest (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `approvedBy` | `String` |
| `note` | `String` |
| `excludedRuleIds` | `List<String>` |

#### BatchPublication (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `publishedAt` | `Instant` |
| `approvedBy` | `String` |
| `note` | `String` |
| `publishedAuditId` | `String` |
| `publishedPlanId` | `String` |
| `appliedProposalIds` | `List<String>` |
| `completedTaskIds` | `List<String>` |
| `reopenedTaskIds` | `List<String>` |
| `rerecords` | `List<AudioRerecordRequest>` |

#### BatchServiceConfig (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchStore` | `BatchStore` |
| `refinementPlanStore` | `RefinementPlanStore` |
| `auditReportStore` | `AuditReportStore` |
| `activeAnalysisSelectionStore` | `ActiveAnalysisSelectionStore` |
| `artifactStore` | `RevisionArtifactStore` |
| `courseRepository` | `CourseRepository` |
| `elementLocator` | `CourseElementLocator` |
| `courseMapper` | `CourseMapper` |
| `auditEngine` | `AuditEngine` |
| `refinerEngine` | `RefinerEngine` |
| `briefer` | `ErrorTaskBriefer` |
| `correctionCatalog` | `AnalyzerCorrectionCatalog` |
| `ruleFixes` | `List<RuleFix>` |
| `panelRunner` | `PanelRunner` |
| `topicMeter` | `TopicMeter` |
| `panelRunnerFactory` | `PanelRunnerFactory` |
| `batchComparator` | `BatchComparator` |
| `baselineStore` | `BaselineStore` |
| `blindSampleService` | `BlindSampleService` |
| `wordLightsQuery` | `TopicWordLightsQuery` |
| `correctionScope` | `CorrectionScope` |
| `preservationCheck` | `PreservationCheck` |
| `coursePath` | `Path` |
| `goalBook` | `GoalBook` |

#### BatchAlreadyOpenException (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `batchId` | `String` |

#### NoOpenBatchException (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `detail` | `String` |

#### TaskInOpenBatchException (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `taskId` | `String` |
| `batchId` | `String` |

#### BatchNotPublishableException (package: batch)

**Package:** `com.learney.contentaudit.revisiondomain.batch`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `reasons` | `String` |

#### Tray (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchId` | `String` |
| `blindSampleOpen` | `boolean` |
| `items` | `List<TrayItem>` |
| `publishable` | `boolean` |
| `blockers` | `List<String>` |

#### TrayItem (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `itemId` | `String` |
| `kind` | `TrayItemKind` |
| `severity` | `FindingSeverity` |
| `knowledgeId` | `String` |
| `quizId` | `String` |
| `question` | `String` |
| `answers` | `List<TrayAnswer>` |
| `before` | `QuizStudentView` |
| `after` | `QuizStudentView` |
| `findings` | `List<Finding>` |
| `reasons` | `List<String>` |
| `ruleId` | `String` |
| `samplePairs` | `List<TraySamplePair>` |
| `blindSampleId` | `String` |
| `blindPairs` | `List<BlindPairView>` |
| `rerecord` | `AudioRerecordRequest` |

#### TrayItemKind (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `BLIND_SAMPLE` | `null` |
| `TIE_ON_COURSE` | `null` |
| `TIE_ON_CANDIDATE` | `null` |
| `NO_WINNER` | `null` |
| `RULE_SAMPLE` | `null` |
| `PENDING_RERECORDING` | `null` |

#### TrayAnswer (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `IS_ERROR` | `null` |
| `NOT_ERROR` | `null` |
| `PASSES` | `null` |
| `DOES_NOT_PASS` | `null` |
| `KEEP_ORIGINAL` | `null` |
| `APPROVE_CANDIDATE` | `null` |
| `WRITE_FIX` | `null` |
| `RULE_OK` | `null` |
| `DROP_CHANGE` | `null` |
| `DROP_RULE` | `null` |
| `BLIND_PAIR` | `null` |

#### TraySamplePair (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `changeId` | `String` |
| `quizId` | `String` |
| `before` | `QuizStudentView` |
| `after` | `QuizStudentView` |

#### TrayResponse (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `itemId` | `String` |
| `answer` | `TrayAnswer` |
| `pairIndex` | `Integer` |
| `pairAnswer` | `PairAnswer` |
| `changeId` | `String` |
| `fix` | `PanelCandidate` |
| `note` | `String` |
| `decidedBy` | `String` |

#### RememberedDecision (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `identityKey` | `String` |
| `marker` | `String` |
| `contentFingerprint` | `String` |
| `answer` | `TrayAnswer` |
| `batchId` | `String` |
| `decidedAt` | `Instant` |

#### TrayServiceConfig (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `batchService` | `BatchService` |
| `batchStore` | `BatchStore` |
| `decisionStore` | `TrayDecisionStore` |
| `blindSampleService` | `BlindSampleService` |
| `panelRunner` | `PanelRunner` |
| `panelRunnerFactory` | `PanelRunnerFactory` |
| `quizStudentViewRenderer` | `QuizStudentViewRenderer` |
| `contentFingerprinter` | `ContentFingerprinter` |
| `courseRepository` | `CourseRepository` |
| `coursePath` | `Path` |

#### ProposalInOpenBatchException (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `proposalId` | `String` |
| `batchId` | `String` |

#### UnexpectedTrayAnswerException (package: tray)

**Package:** `com.learney.contentaudit.revisiondomain.tray`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `answer` | `String` |
| `itemId` | `String` |
| `allowed` | `String` |

### Module: revision-infrastructure

#### LagenConfig (package: lagen)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lagen`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `providerName` | `String` |
| `modelId` | `String` |
| `endpoint` | `String` |
| `apiKey` | `String` |
| `temperature` | `Double` |
| `maxTokens` | `Integer` |
| `timeout` | `Duration` |
| `maxSuggestedLemmaRequests` | `Integer` |
| `maxEvalRetries` | `Integer` |

#### LlmGenerationFailureCategory (package: lagen)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lagen`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `LLM_UNREACHABLE` | `null` |
| `LLM_TIMEOUT` | `null` |
| `LLM_AUTH_FAILED` | `null` |
| `LLM_RESPONSE_EMPTY` | `null` |
| `LLM_RESPONSE_MALFORMED` | `null` |
| `LLM_OTHER` | `null` |
| `LLM_LEMMA_REQUEST_BUDGET_EXHAUSTED` | `null` |
| `LLM_TOOL_CALLING_UNSUPPORTED` | `null` |

#### LagenDefaults (package: lagen)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lagen`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `temperature` | `double` |
| `maxTokens` | `int` |
| `timeout` | `Duration` |

#### InvalidProviderIdException (package: lagen)

**Package:** `com.learney.contentaudit.revisioninfrastructure.lagen`
**Visibility:** public
**Type:** exception

| Field | Type |
|-------|------|
| `providerName` | `String` |
| `modelId` | `String` |

### Module: evaluation-ledger-domain

#### EvaluationKey

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `evaluatorId` | `String` |  |
| `contentFingerprint` | `String` |  |

**Generated constructor:**
```java
new EvaluationKey(String evaluatorId, String contentFingerprint)
```

#### EvaluationSubject

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `subjectRef` | `String` |  |
| `content` | `Map<String,String>` |  |

**Generated constructor:**
```java
new EvaluationSubject(String subjectRef, Map<String,String> content)
```

#### EvaluationRecord

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `key` | `EvaluationKey` |  |
| `payload` | `String` |  |
| `subjectRef` | `String` |  |
| `recordedAt` | `Instant` |  |
| `evaluatorVersion` | `String` |  |

**Generated constructor:**
```java
new EvaluationRecord(EvaluationKey key, String payload, String subjectRef, Instant recordedAt, String evaluatorVersion)
```

#### EvaluationEmitted

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record
**Implements:** `EvaluationOutcome`

| Field | Type | Notes |
|-------|------|-------|
| `payload` | `String` |  |

**Generated constructor:**
```java
new EvaluationEmitted(String payload)
```

#### EvaluationNotEmitted

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record
**Implements:** `EvaluationOutcome`

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `EvaluationFailureKind` |  |
| `reason` | `String` |  |

**Generated constructor:**
```java
new EvaluationNotEmitted(EvaluationFailureKind kind, String reason)
```

#### EvaluationFailureKind

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `OUTPUT_UNUSABLE` | `null` |  |
| `EVALUATOR_UNAVAILABLE` | `null` |  |

**Generated constructor:**
```java
new EvaluationFailureKind(null OUTPUT_UNUSABLE, null EVALUATOR_UNAVAILABLE)
```

#### EvaluationResolutionKind

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `REUSED` | `null` |  |
| `EVALUATED` | `null` |  |
| `PENDING` | `null` |  |
| `FAILED` | `null` |  |

**Generated constructor:**
```java
new EvaluationResolutionKind(null REUSED, null EVALUATED, null PENDING, null FAILED)
```

#### EvaluationResolution

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `EvaluationResolutionKind` |  |
| `payload` | `String` |  |
| `evaluatorVersion` | `String` |  |
| `failure` | `EvaluationNotEmitted` |  |

**Generated constructor:**
```java
new EvaluationResolution(EvaluationResolutionKind kind, String payload, String evaluatorVersion, EvaluationNotEmitted failure)
```

#### EvaluationCoverage

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `reached` | `int` |  |
| `withResult` | `int` |  |
| `evaluatedInRun` | `int` |  |
| `reused` | `int` |  |
| `pending` | `int` |  |
| `failed` | `int` |  |

**Generated constructor:**
```java
new EvaluationCoverage(int reached, int withResult, int evaluatedInRun, int reused, int pending, int failed)
```

#### EvaluationBudget

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `maxNewEvaluations` | `int` |  |

**Generated constructor:**
```java
new EvaluationBudget(int maxNewEvaluations)
```

#### EvaluationRequestMode

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `REUSE_OR_EVALUATE` | `null` |  |
| `FORCE` | `null` |  |
| `PREFER_NEW` | `null` |  |
| `REUSE_ONLY` | `null` |  |

**Generated constructor:**
```java
new EvaluationRequestMode(null REUSE_OR_EVALUATE, null FORCE, null PREFER_NEW, null REUSE_ONLY)
```

#### EvaluationRequest

**Package:** `com.learney.contentaudit.evaluationledgerdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `subject` | `EvaluationSubject` |  |
| `mode` | `EvaluationRequestMode` |  |

**Generated constructor:**
```java
new EvaluationRequest(EvaluationSubject subject, EvaluationRequestMode mode)
```

### Module: agent-runtime-infrastructure

#### AgentGraphRunnerConfig

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `agentsBaseDir` | `Path` |  |
| `runsBaseDir` | `Path` |  |

**Generated constructor:**
```java
new AgentGraphRunnerConfig(Path agentsBaseDir, Path runsBaseDir)
```

#### AgentDefinitionFound

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure`
**Type:** record
**Implements:** `AgentDefinitionLocation`

| Field | Type | Notes |
|-------|------|-------|
| `directory` | `Path` |  |

**Generated constructor:**
```java
new AgentDefinitionFound(Path directory)
```

#### AgentDefinitionUnresolved

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure`
**Type:** record
**Implements:** `AgentDefinitionLocation`

| Field | Type | Notes |
|-------|------|-------|
| `agentName` | `String` |  |
| `searchedIn` | `Path` |  |

**Generated constructor:**
```java
new AgentDefinitionUnresolved(String agentName, Path searchedIn)
```

#### LlmProviderClass (package: llmprovider)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.llmprovider`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `LOCAL_SESSION` | `null` |
| `REMOTE_SERVICE` | `null` |

#### LlmProviderRejectionKind (package: llmprovider)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.llmprovider`
**Visibility:** public
**Type:** enum

| Field | Type |
|-------|------|
| `UNSUPPORTED_PROVIDER` | `null` |
| `MISSING_REQUIRED_INPUT` | `null` |

#### LlmProviderResolved (package: llmprovider)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.llmprovider`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `providerName` | `String` |
| `providerClass` | `LlmProviderClass` |

#### LlmProviderRejected (package: llmprovider)

**Package:** `com.learney.contentaudit.agentruntimeinfrastructure.llmprovider`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `providerName` | `String` |
| `missingInput` | `String` |
| `kind` | `LlmProviderRejectionKind` |

### Module: quiz-instruction-infrastructure

#### QuizInstructionJudgeConfig

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `baseUrl` | `String` |  |
| `apiKey` | `String` |  |
| `modelName` | `String` |  |
| `temperature` | `Double` |  |
| `timeoutSeconds` | `Integer` |  |
| `agentName` | `String` |  |
| `providerName` | `String` |  |

**Generated constructor:**
```java
new QuizInstructionJudgeConfig(String baseUrl, String apiKey, String modelName, Double temperature, Integer timeoutSeconds, String agentName, String providerName)
```

#### AgentJudgeSpec

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `evaluatorId` | `String` |  |
| `verdictArtifactKey` | `String` |  |
| `displayName` | `String` |  |

**Generated constructor:**
```java
new AgentJudgeSpec(String evaluatorId, String verdictArtifactKey, String displayName)
```

#### AgentJudgeConfig

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `providerName` | `String` |  |
| `baseUrl` | `String` |  |
| `apiKey` | `String` |  |
| `modelName` | `String` |  |
| `temperature` | `Double` |  |
| `timeoutSeconds` | `Integer` |  |

**Generated constructor:**
```java
new AgentJudgeConfig(String providerName, String baseUrl, String apiKey, String modelName, Double temperature, Integer timeoutSeconds)
```

#### JudgeUnavailabilityCause

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `SESSION_EXPIRED` | `null` |  |
| `QUOTA_EXHAUSTED` | `null` |  |
| `SERVICE_UNREACHABLE` | `null` |  |
| `TIMEOUT` | `null` |  |
| `NOT_INSTALLED` | `null` |  |
| `UNKNOWN` | `null` |  |

**Generated constructor:**
```java
new JudgeUnavailabilityCause(null SESSION_EXPIRED, null QUOTA_EXHAUSTED, null SERVICE_UNREACHABLE, null TIMEOUT, null NOT_INSTALLED, null UNKNOWN)
```

#### JudgeProviderReadiness

**Package:** `com.learney.contentaudit.quizinstructioninfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `ready` | `boolean` |  |
| `cause` | `JudgeUnavailabilityCause` |  |
| `detail` | `String` |  |
| `remedy` | `String` |  |

**Generated constructor:**
```java
new JudgeProviderReadiness(boolean ready, JudgeUnavailabilityCause cause, String detail, String remedy)
```

### Module: local-language-infrastructure

#### OllamaEndpoint

**Package:** `com.learney.contentaudit.locallanguageinfrastructure`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `baseUrl` | `String` |  |
| `timeoutSeconds` | `int` |  |

**Generated constructor:**
```java
new OllamaEndpoint(String baseUrl, int timeoutSeconds)
```

#### OllamaTokenProbability (package: ollama)

**Package:** `com.learney.contentaudit.locallanguageinfrastructure.ollama`
**Visibility:** public
**Type:** record

| Field | Type |
|-------|------|
| `token` | `String` |
| `logprob` | `double` |

