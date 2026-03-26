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
| tokenCount | `int` |

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

### AnalysisResult (port)

Methods:

- `getName(): String`
- `getScore(): double`
- `getTarget(): AuditTarget`

### NlpTokenizer (port)

Methods:

- `tokenize(String text): List<String>`
- `countTokens(String text): int`

### SentenceLengthConfig (port)

Methods:

- `getTargetRange(CefrLevel level): Optional<TargetRange>`
- `getToleranceMargin(): int`

### ScoreAggregator (port)

Methods:

- `aggregate(List<ScoredItem> scores): AuditReport`

## Implementations

### IAuditEngine

**Implements:** AuditEngine

**Dependencies (constructor injection):**

- `contentAnalyzers`: `List<ContentAnalyzer>`
- `scoreAggregator`: `ScoreAggregator`

### KnowledgeTitleLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- Given a KnowledgeTitleLengthAnalyzer, when getName is called, then returns knowledge-title-length → F-KTLEN/F-KTLEN-R008
- Given a KnowledgeTitleLengthAnalyzer, when getTarget is called, then returns KNOWLEDGE → F-KTLEN/F-KTLEN-R008
- Given a knowledge with null title, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with empty title, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with title within limit, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with title at exactly 28 weighted chars, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R001
- Given a knowledge with title 'fitting' (weighted 5.1), when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R002
- Given a knowledge with zero-weight title '$$$***', when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R002
- Given a knowledge with mixed-weight title '$if,a' (weighted 2.7), when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R002
- Given a knowledge with title of weighted length 35, when onKnowledge is called and getResults checked, then score is 0.75 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with title of weighted length 42, when onKnowledge is called and getResults checked, then score is 0.5 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with title of weighted length 56, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R003
- Given a knowledge with title of weighted length 70, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R003
- Given a KnowledgeTitleLengthAnalyzer, when onQuiz is called, then it completes without error → F-KTLEN/F-KTLEN-R008
- Given a KnowledgeTitleLengthAnalyzer, when onMilestone is called, then it completes without error → F-KTLEN
- Given a KnowledgeTitleLengthAnalyzer, when onTopic is called, then it completes without error → F-KTLEN
- Given a KnowledgeTitleLengthAnalyzer, when onCourseComplete is called, then it completes without error → F-KTLEN
- Given two knowledges with different title lengths, when both are processed and getResults checked, then returns two correctly scored items → F-KTLEN/F-KTLEN-R003
- Given no knowledges have been processed, when getResults is called, then returns empty list → F-KTLEN/F-KTLEN-R003

### KnowledgeInstructionsLengthAnalyzer

**Implements:** ContentAnalyzer

**Tests that must pass:**

- Given a KnowledgeInstructionsLengthAnalyzer, when getName is called, then returns knowledge-instructions-length → F-KTLEN/F-KTLEN-R008
- Given a KnowledgeInstructionsLengthAnalyzer, when getTarget is called, then returns KNOWLEDGE → F-KTLEN/F-KTLEN-R008
- Given a knowledge with null instructions, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R006
- Given a knowledge with empty instructions, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R006
- Given a knowledge with instructions exactly at soft limit of 70 chars, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R005
- Given a knowledge with instructions of 30 chars within soft limit, when onKnowledge is called and getResults checked, then score is 1.0 → F-KTLEN/F-KTLEN-R006
- Given a knowledge with instructions of 71 chars just above soft limit, when onKnowledge is called and getResults checked, then score is 0.5 → F-KTLEN/F-KTLEN-R005
- Given a knowledge with instructions exactly at hard limit of 100 chars, when onKnowledge is called and getResults checked, then score is 0.5 → F-KTLEN/F-KTLEN-R005
- Given a knowledge with instructions of 85 chars between soft and hard limits, when onKnowledge is called and getResults checked, then score is 0.5 → F-KTLEN/F-KTLEN-R006
- Given a knowledge with instructions of 101 chars just above hard limit, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R005
- Given a knowledge with instructions of 200 chars well above hard limit, when onKnowledge is called and getResults checked, then score is 0.0 → F-KTLEN/F-KTLEN-R006
- Given a KnowledgeInstructionsLengthAnalyzer, when onQuiz is called, then it completes without error → F-KTLEN
- Given a KnowledgeInstructionsLengthAnalyzer, when onMilestone is called, then it completes without error → F-KTLEN
- Given a KnowledgeInstructionsLengthAnalyzer, when onTopic is called, then it completes without error → F-KTLEN
- Given a KnowledgeInstructionsLengthAnalyzer, when onCourseComplete is called, then it completes without error → F-KTLEN
- Given a fresh KnowledgeInstructionsLengthAnalyzer, when getResults is called without prior processing, then returns empty list
- Given three knowledges with different instruction lengths, when all are processed and getResults checked, then correct scores are produced for each → F-KTLEN/F-KTLEN-R006

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

- Given a null milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R001
- Given a non-numeric milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R001
- Given no target range configured for level, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R012
- Given a valid sentence quiz, when onQuiz is called, then nlpTokenizer countTokens is called with quiz sentence → F-SLEN/F-SLEN-R013
- Given multiple quizzes across sentence and non-sentence knowledges, when processed, then only sentence quizzes are scored → F-SLEN/F-SLEN-R001
- Given a SentenceLengthAnalyzer, when getName is called, then returns sentence-length
- Given a SentenceLengthAnalyzer, when getTarget is called, then returns QUIZ
- Given a quiz within A1 range, when onQuiz is called and getResults checked, then score is 1.0 → F-SLEN/F-SLEN-R002
- Given a quiz 1 token above A1 max, when scored, then score is 0.75 → F-SLEN/F-SLEN-R002
- Given a quiz 3 tokens below A1 min, when scored, then score is 0.25 → F-SLEN/F-SLEN-R002
- Given a quiz exactly at A1 minimum boundary, when scored, then score is 1.0 → F-SLEN/F-SLEN-R002
- Given a quiz exactly at A1 maximum boundary, when scored, then score is 1.0 → F-SLEN/F-SLEN-R002
- Given a quiz 4 tokens above A1 max, when scored, then score is 0.0 → F-SLEN/F-SLEN-R009
- Given a non-sentence knowledge, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R001
- Given a B2 level quiz within range, when scored, then score is 1.0 → F-SLEN/F-SLEN-R012
- Given a quiz exactly at tolerance boundary of 4 tokens, when scored, then score is 0.0 → F-SLEN/F-SLEN-R009
- Given a quiz 2 tokens above A1 max, when scored, then score is 0.5 → F-SLEN/F-SLEN-R002
- Given a SentenceLengthAnalyzer, when onTopic is called, then it completes without error
- Given a SentenceLengthAnalyzer, when onCourseComplete is called, then it completes without error
- Given a full milestone-knowledge-quiz sequence, when processed end to end, then correct scores are produced → F-SLEN/F-SLEN-R002
- Given a SentenceLengthAnalyzer instance, when getName is called, then returns sentence-length
- Given a SentenceLengthAnalyzer instance, when getTarget is called, then returns QUIZ
- Given a knowledge with non-sentence quizzes, when onQuiz is called, then non-sentence quizzes are excluded from scoring → F-SLEN/F-SLEN-R001

### IScoreAggregator

**Implements:** ScoreAggregator

