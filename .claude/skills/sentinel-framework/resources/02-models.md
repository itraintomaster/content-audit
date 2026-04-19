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

**Generated constructor:**
```java
new AuditableKnowledge(List<AuditableQuiz> quizzes, String title, String instructions, boolean isSentence, String id, String label, String code)
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
| `sentence` | `String` |  |
| `tokens` | `List<NlpToken>` | Import `java.util.List` |
| `id` | `String` |  |
| `label` | `String` |  |
| `code` | `String` |  |
| `translation` | `String` |  |

**Generated constructor:**
```java
new AuditableQuiz(String sentence, List<NlpToken> tokens, String id, String label, String code, String translation)
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

**Generated constructor:**
```java
new CefrLevel(null A1, null A2, null B1, null B2)
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

**Generated constructor:**
```java
new AnalyzerDescriptor(String name, String description, AuditTarget target)
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

**Generated constructor:**
```java
new AuditNode(AuditableEntity entity, AuditTarget target, AuditNode parent, List<AuditNode> children, Map<String,Double> scores, Map<String,Object> metadata, NodeDiagnoses diagnoses)
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

**Generated constructor:**
```java
new KnowledgeEntity(String id, String code, NodeKind kind, String label, String oldId, String parentId, boolean isRule, String instructions, int order, String slug, List<QuizTemplateEntity> quizTemplates)
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

**Generated constructor:**
```java
new QuizTemplateEntity(String id, String oidId, String kind, String knowledgeId, String title, String instructions, String translation, String theoryId, String topicName, FormEntity form, double difficulty, double retries, double noScoreRetries, String code, String audioUrl, String imageUrl, String answerAudioUrl, String answerImageUrl, String miniTheory, String successMessage)
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

**Generated constructor:**
```java
new FormEntity(String kind, double incidence, String label, String name, List<SentencePartEntity> sentenceParts)
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

**Generated constructor:**
```java
new DiagnosisKind(null SENTENCE_LENGTH, null LEMMA_ABSENCE, null COCA_BUCKETS, null LEMMA_RECURRENCE, null KNOWLEDGE_TITLE_LENGTH, null KNOWLEDGE_INSTRUCTIONS_LENGTH)
```

#### RefinementTaskStatus

**Package:** `com.learney.contentaudit.refinerdomain`
**Type:** enum

| Field | Type | Notes |
|-------|------|-------|
| `PENDING` | `null` |  |
| `COMPLETED` | `null` |  |
| `SKIPPED` | `null` |  |

**Generated constructor:**
```java
new RefinementTaskStatus(null PENDING, null COMPLETED, null SKIPPED)
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

**Generated constructor:**
```java
new RefinementTask(String id, AuditTarget nodeTarget, String nodeId, String nodeLabel, DiagnosisKind diagnosisKind, int priority, RefinementTaskStatus status)
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

**Generated constructor:**
```java
new SuggestedLemma(String lemma, String pos, String reason, Integer cocaRank)
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

**Generated constructor:**
```java
new MisplacedLemmaContext(String lemma, String pos, CefrLevel expectedLevel, CefrLevel quizLevel, Integer cocaRank)
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

**Generated constructor:**
```java
new LemmaAbsenceCorrectionContext(String taskId, String sentence, String translation, String knowledgeTitle, String knowledgeInstructions, String topicLabel, CefrLevel cefrLevel, List<MisplacedLemmaContext> misplacedLemmas, List<SuggestedLemma> suggestedLemmas)
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

**Generated constructor:**
```java
new GetTasksFilter(Optional<String> planId, Optional<String> status, boolean sortByPriority, Optional<Integer> limit, Optional<AuditTarget> target, Optional<DiagnosisKind> diagnosisKind)
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

#### InvalidWorkdirException (package: bootstrap)

**Package:** `com.learney.contentaudit.auditcli.bootstrap`
**Visibility:** internal
**Type:** exception

| Field | Type |
|-------|------|
| `path` | `String` |

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

**Generated constructor:**
```java
new RevisionVerdict(null APPROVED, null REJECTED)
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

**Generated constructor:**
```java
new RevisionOutcomeKind(null APPROVED_APPLIED, null APPROVED_APPLY_FAILED, null REJECTED, null NO_REVISER, null CONTEXT_UNAVAILABLE, null ELEMENT_NOT_FOUND)
```

#### CourseElementSnapshot

**Package:** `com.learney.contentaudit.revisiondomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `nodeTarget` | `AuditTarget` |  |
| `nodeId` | `String` |  |
| `quiz` | `QuizTemplateEntity` |  |

**Generated constructor:**
```java
new CourseElementSnapshot(AuditTarget nodeTarget, String nodeId, QuizTemplateEntity quiz)
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

**Generated constructor:**
```java
new RevisionProposal(String proposalId, String taskId, String planId, String sourceAuditId, DiagnosisKind diagnosisKind, AuditTarget nodeTarget, String nodeId, CourseElementSnapshot elementBefore, CourseElementSnapshot elementAfter, String rationale, String reviserKind, Instant createdAt)
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

**Generated constructor:**
```java
new RevisionArtifact(RevisionProposal proposal, RevisionVerdict verdict, String rejectionReason, RevisionOutcomeKind outcome)
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

**Generated constructor:**
```java
new RevisionEngineConfig(Map<DiagnosisKind,Reviser> revisers, RevisionValidator validator, RevisionArtifactStore artifactStore, CourseRepository courseRepository, CourseElementLocator elementLocator, RefinementPlanStore refinementPlanStore, AuditReportStore auditReportStore, CorrectionContextResolver<CorrectionContext> contextResolver)
```

