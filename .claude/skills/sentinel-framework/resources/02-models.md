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
| `overallScore` | `double` |  |
| `scores` | `NodeScores` |  |
| `milestones` | `List<MilestoneNode>` | Import `java.util.List` |

**Generated constructor:**
```java
new AuditReport(double overallScore, NodeScores scores, List<MilestoneNode> milestones)
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

#### AuditContext

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `milestoneId` | `String` |  |
| `topicId` | `String` |  |
| `knowledgeId` | `String` |  |
| `quizId` | `String` |  |

**Generated constructor:**
```java
new AuditContext(String milestoneId, String topicId, String knowledgeId, String quizId)
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

**Generated constructor:**
```java
new AuditableKnowledge(List<AuditableQuiz> quizzes, String title, String instructions, boolean isSentence)
```

#### AuditableTopic

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `knowledge` | `List<AuditableKnowledge>` | Import `java.util.List` |

**Generated constructor:**
```java
new AuditableTopic(List<AuditableKnowledge> knowledge)
```

#### AuditableMilestone

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `topics` | `List<AuditableTopic>` | Import `java.util.List` |

**Generated constructor:**
```java
new AuditableMilestone(List<AuditableTopic> topics)
```

#### AuditableQuiz

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `sentence` | `String` |  |
| `tokenCount` | `int` |  |

**Generated constructor:**
```java
new AuditableQuiz(String sentence, int tokenCount)
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

#### ScoredItem

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `analyzerName` | `String` |  |
| `target` | `AuditTarget` |  |
| `score` | `double` |  |
| `milestoneId` | `String` |  |
| `topicId` | `String` |  |
| `knowledgeId` | `String` |  |
| `quizId` | `String` |  |

**Generated constructor:**
```java
new ScoredItem(String analyzerName, AuditTarget target, double score, String milestoneId, String topicId, String knowledgeId, String quizId)
```

#### NodeScores

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `scores` | `Map<String,Double>` |  |

**Generated constructor:**
```java
new NodeScores(Map<String,Double> scores)
```

#### QuizNode

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `quizId` | `String` |  |
| `scores` | `NodeScores` |  |

**Generated constructor:**
```java
new QuizNode(String quizId, NodeScores scores)
```

#### KnowledgeNode

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `knowledgeId` | `String` |  |
| `scores` | `NodeScores` |  |
| `quizzes` | `List<QuizNode>` | Import `java.util.List` |

**Generated constructor:**
```java
new KnowledgeNode(String knowledgeId, NodeScores scores, List<QuizNode> quizzes)
```

#### TopicNode

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `topicId` | `String` |  |
| `scores` | `NodeScores` |  |
| `knowledges` | `List<KnowledgeNode>` | Import `java.util.List` |

**Generated constructor:**
```java
new TopicNode(String topicId, NodeScores scores, List<KnowledgeNode> knowledges)
```

#### MilestoneNode

**Package:** `com.learney.contentaudit.auditdomain`
**Type:** record

| Field | Type | Notes |
|-------|------|-------|
| `milestoneId` | `String` |  |
| `scores` | `NodeScores` |  |
| `topics` | `List<TopicNode>` | Import `java.util.List` |

**Generated constructor:**
```java
new MilestoneNode(String milestoneId, NodeScores scores, List<TopicNode> topics)
```

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

