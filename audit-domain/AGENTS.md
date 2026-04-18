<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

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

## Implementations

### IAuditEngine

**Implements:** AuditEngine

**Dependencies (constructor injection):**

- `contentAnalyzers`: `List<ContentAnalyzer>`
- `scoreAggregator`: `ScoreAggregator`

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
- should score 1.0 for instructions exactly at soft limit of 70 chars → FEAT-KTLEN/F-KTLEN-R005
- should score 1.0 for instructions of 30 chars within soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 chars just above soft limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions exactly at hard limit of 100 chars → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions of 85 chars between soft and hard limits → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 chars just above hard limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.0 for instructions of 200 chars well above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should complete without error when onQuiz is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onMilestone is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onTopic is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onCourseComplete is called → FEAT-KTLEN/F-KTLEN-R008
- should produce correct scores for three knowledges with different instruction lengths → FEAT-KTLEN/F-KTLEN-R006

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

### IScoreAggregator

**Implements:** ScoreAggregator

