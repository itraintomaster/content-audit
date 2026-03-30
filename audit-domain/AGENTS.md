<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: audit-domain (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

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

## Interfaces

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

## Implementations

### IAuditEngine

**Implements:** AuditEngine

**Dependencies (constructor injection):**

- `contentAnalyzers`: `List<ContentAnalyzer>`
- `scoreAggregator`: `ScoreAggregator`

### KnowledgeTitleLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- should return knowledge-title-length as analyzer name → F-KTLEN/F-KTLEN-R008
- should return KNOWLEDGE as audit target → F-KTLEN/F-KTLEN-R008
- should score 0.0 for knowledge with null title → F-KTLEN/F-KTLEN-R003
- should score 0.0 for knowledge with empty title → F-KTLEN/F-KTLEN-R003
- should score 1.0 for knowledge with title within limit → F-KTLEN/F-KTLEN-R003
- should score 1.0 for knowledge with title at exactly 28 weighted chars → F-KTLEN/F-KTLEN-R001
- should score 1.0 for title fitting with weighted length 5.1 → F-KTLEN/F-KTLEN-R002
- should score 1.0 for zero-weight special chars title → F-KTLEN/F-KTLEN-R002
- should score 1.0 for mixed-weight title with weighted length 2.7 → F-KTLEN/F-KTLEN-R002
- should score 0.75 for title of weighted length 35 → F-KTLEN/F-KTLEN-R003
- should score 0.5 for title of weighted length 42 → F-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 56 → F-KTLEN/F-KTLEN-R003
- should score 0.0 for title of weighted length 70 → F-KTLEN/F-KTLEN-R003
- should complete without error when onQuiz is called → F-KTLEN/F-KTLEN-R008
- should complete without error when onMilestone is called → F-KTLEN
- should complete without error when onTopic is called → F-KTLEN
- should complete without error when onCourseComplete is called → F-KTLEN
- should return two correctly scored items for two knowledges with different title lengths → F-KTLEN/F-KTLEN-R003
- should return empty list when no knowledges have been processed → F-KTLEN/F-KTLEN-R003

### KnowledgeInstructionsLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- should return knowledge-instructions-length as analyzer name → F-KTLEN/F-KTLEN-R008
- should return KNOWLEDGE as audit target → F-KTLEN/F-KTLEN-R008
- should score 1.0 for knowledge with null instructions → F-KTLEN/F-KTLEN-R006
- should score 1.0 for knowledge with empty instructions → F-KTLEN/F-KTLEN-R006
- should score 1.0 for instructions exactly at soft limit of 70 chars → F-KTLEN/F-KTLEN-R005
- should score 1.0 for instructions of 30 chars within soft limit → F-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 chars just above soft limit → F-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions exactly at hard limit of 100 chars → F-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions of 85 chars between soft and hard limits → F-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 chars just above hard limit → F-KTLEN/F-KTLEN-R005
- should score 0.0 for instructions of 200 chars well above hard limit → F-KTLEN/F-KTLEN-R006
- should complete without error when onQuiz is called → F-KTLEN
- should complete without error when onMilestone is called → F-KTLEN
- should complete without error when onTopic is called → F-KTLEN
- should complete without error when onCourseComplete is called → F-KTLEN
- should return empty list when getResults is called without prior processing → F-KTLEN
- should produce correct scores for three knowledges with different instruction lengths → F-KTLEN/F-KTLEN-R006

### IContentAudit

**Implements:** ContentAudit

**Dependencies (constructor injection):**

- `auditEngine`: `AuditEngine`

### SentenceLengthAnalyzer

**Implements:** ContentAnalyzer

**Dependencies (constructor injection):**

- `nlpTokenizer`: `NlpTokenizer`
- `config`: `SentenceLengthConfig`

**Tests that must pass:**

- should exclude quiz when milestoneId is null → F-SLEN/F-SLEN-R001
- should exclude quiz when milestoneId is non-numeric → F-SLEN/F-SLEN-R001
- should exclude quiz when no target range configured for level → F-SLEN/F-SLEN-R012
- should score only sentence quizzes when processing mixed knowledge types → F-SLEN/F-SLEN-R001
- should return sentence-length as analyzer name → F-SLEN
- should return QUIZ as audit target → F-SLEN
- should score 1.0 for quiz within A1 range → F-SLEN/F-SLEN-R002
- should score 0.75 for quiz 1 token above A1 max → F-SLEN/F-SLEN-R002
- should score 0.25 for quiz 3 tokens below A1 min → F-SLEN/F-SLEN-R002
- should score 1.0 for quiz exactly at A1 minimum boundary → F-SLEN/F-SLEN-R002
- should score 1.0 for quiz exactly at A1 maximum boundary → F-SLEN/F-SLEN-R002
- should score 0.0 for quiz 4 tokens above A1 max at tolerance boundary → F-SLEN/F-SLEN-R009
- should exclude non-sentence knowledge quiz from results → F-SLEN/F-SLEN-R001
- should score 1.0 for B2 level quiz within range → F-SLEN/F-SLEN-R012
- should score 0.0 for quiz exactly at tolerance boundary → F-SLEN/F-SLEN-R009
- should score 0.5 for quiz 2 tokens above A1 max → F-SLEN/F-SLEN-R002
- should complete without error when onTopic is called → F-SLEN
- should complete without error when onCourseComplete is called → F-SLEN
- should produce correct scores for full milestone-knowledge-quiz sequence → F-SLEN/F-SLEN-R002
- should exclude non-sentence quizzes from scoring → F-SLEN/F-SLEN-R001

### IScoreAggregator

**Implements:** ScoreAggregator

