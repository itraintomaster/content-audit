<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: nlp-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Infrastructure module for NLP processing. Provides SpaCy-backed tokenization behind a factory, with internal caching. Only the factory and configuration model are public; all processing internals are package-private.

## Models

### NlpTokenizerConfig (`record`)

| Field | Type |
|-------|------|
| pythonScriptPath | `String` |
| cocaDataPath | `String` |
| timeoutSeconds | `int` |

## Interfaces

### NlpTokenizerFactory (factory)

Methods:

- `create(NlpTokenizerConfig config): NlpTokenizer`

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

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

### AuditableKnowledge (`record`)

| Field | Type |
|-------|------|
| quizzes | `List<AuditableQuiz>` |
| title | `String` |
| instructions | `String` |
| isSentence | `boolean` |

### AuditableTopic (`record`)

| Field | Type |
|-------|------|
| knowledge | `List<AuditableKnowledge>` |

### AuditableMilestone (`record`)

| Field | Type |
|-------|------|
| topics | `List<AuditableTopic>` |

### AuditableQuiz (`record`)

| Field | Type |
|-------|------|
| sentence | `String` |
| tokens | `List<NlpToken>` |

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

### NodeScores (`record`)

| Field | Type |
|-------|------|
| scores | `Map<String,Double>` |

### QuizNode (`record`)

| Field | Type |
|-------|------|
| quizId | `String` |
| scores | `NodeScores` |

### KnowledgeNode (`record`)

| Field | Type |
|-------|------|
| knowledgeId | `String` |
| scores | `NodeScores` |
| quizzes | `List<QuizNode>` |

### TopicNode (`record`)

| Field | Type |
|-------|------|
| topicId | `String` |
| scores | `NodeScores` |
| knowledges | `List<KnowledgeNode>` |

### MilestoneNode (`record`)

| Field | Type |
|-------|------|
| milestoneId | `String` |
| scores | `NodeScores` |
| topics | `List<TopicNode>` |

### NlpToken (`record`)

| Field | Type |
|-------|------|
| text | `String` |
| lemma | `String` |
| posTag | `String` |
| frequencyRank | `Integer` |
| isStop | `boolean` |
| isPunct | `boolean` |

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

- `aggregate(List<ScoredItem> scores): AuditReport`

### CocaBucketsConfig (port)

Methods:

- `getBandConfiguration(): BandConfiguration`
- `getTargetsForLevel(String levelName): List<BucketTarget>`
- `getQuarterTargetsForLevel(String levelName): List<QuarterBucketTargets>`
- `getToleranceMargin(): double`
- `getAnalysisStrategy(): AnalysisStrategy`
- `getProgressionExpectations(): List<ProgressionExpectation>`

