# Implementations (Adapters)

Implementations are the classes that the AI agent writes. Sentinel generates **stubs** with
`UnsupportedOperationException` — the agent must fill in the logic to satisfy the generated tests.

## Key Rules

1. **Implement exactly the interfaces** listed in `implements` — no more, no less
2. **Use constructor injection** from `requiresInject` — never use `new` for dependencies
3. **Apply framework annotations** based on `types` (e.g., `Repository` -> `@Repository`)
4. **Make all generated tests pass** — the tests define the behavioral contract
5. **Do NOT modify generated test files** — they are owned by Sentinel

## Framework Type Mapping

| Sentinel Type | Spring Boot | Jakarta EE |
|---------------|-------------|------------|
| `Component` | `@Component` | `@Named` |
| `Service` | `@Service` | `@Stateless` |
| `Repository` | `@Repository` | `@Repository` |
| `RestController` | `@RestController` | `@Path` |
| `UseCase` | *(none)* | *(none)* |

## Smart Merge Behavior

When you run `sentinel generate` after modifying an implementation:
- If the file has `@Generated` → fully regenerated (stub)
- If the file has NO `@Generated` → **smart merge**: new methods are added as stubs, existing methods preserved
- The generator never overwrites your implementation code once you start coding

## Declared Implementations

### Module: audit-domain

#### IAuditEngine

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** AuditEngine

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `contentAnalyzers` | `List<ContentAnalyzer>` |
| `scoreAggregator` | `ScoreAggregator` |

**Generated constructor:**
```java
public IAuditEngine(List<ContentAnalyzer> contentAnalyzers, ScoreAggregator scoreAggregator) {
    this.contentAnalyzers = contentAnalyzers;
    this.scoreAggregator = scoreAggregator;
}
```

#### KnowledgeTitleLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Tests that must pass:** 19

- Given a KnowledgeTitleLengthAnalyzer, when getName is called, then returns knowledge-title-length [F-KTLEN/F-KTLEN-R008]
- Given a KnowledgeTitleLengthAnalyzer, when getTarget is called, then returns KNOWLEDGE [F-KTLEN/F-KTLEN-R008]
- Given a knowledge with null title, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with empty title, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with title within limit, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with title at exactly 28 weighted chars, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R001]
- Given a knowledge with title 'fitting' (weighted 5.1), when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R002]
- Given a knowledge with zero-weight title '$$$***', when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R002]
- Given a knowledge with mixed-weight title '$if,a' (weighted 2.7), when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R002]
- Given a knowledge with title of weighted length 35, when onKnowledge is called and getResults checked, then score is 0.75 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with title of weighted length 42, when onKnowledge is called and getResults checked, then score is 0.5 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with title of weighted length 56, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R003]
- Given a knowledge with title of weighted length 70, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R003]
- Given a KnowledgeTitleLengthAnalyzer, when onQuiz is called, then it completes without error [F-KTLEN/F-KTLEN-R008]
- Given a KnowledgeTitleLengthAnalyzer, when onMilestone is called, then it completes without error [F-KTLEN]
- Given a KnowledgeTitleLengthAnalyzer, when onTopic is called, then it completes without error [F-KTLEN]
- Given a KnowledgeTitleLengthAnalyzer, when onCourseComplete is called, then it completes without error [F-KTLEN]
- Given two knowledges with different title lengths, when both are processed and getResults checked, then returns two correctly scored items [F-KTLEN/F-KTLEN-R003]
- Given no knowledges have been processed, when getResults is called, then returns empty list [F-KTLEN/F-KTLEN-R003]

#### KnowledgeInstructionsLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Tests that must pass:** 17

- Given a KnowledgeInstructionsLengthAnalyzer, when getName is called, then returns knowledge-instructions-length [F-KTLEN/F-KTLEN-R008]
- Given a KnowledgeInstructionsLengthAnalyzer, when getTarget is called, then returns KNOWLEDGE [F-KTLEN/F-KTLEN-R008]
- Given a knowledge with null instructions, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R006]
- Given a knowledge with empty instructions, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R006]
- Given a knowledge with instructions exactly at soft limit of 70 chars, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R005]
- Given a knowledge with instructions of 30 chars within soft limit, when onKnowledge is called and getResults checked, then score is 1.0 [F-KTLEN/F-KTLEN-R006]
- Given a knowledge with instructions of 71 chars just above soft limit, when onKnowledge is called and getResults checked, then score is 0.5 [F-KTLEN/F-KTLEN-R005]
- Given a knowledge with instructions exactly at hard limit of 100 chars, when onKnowledge is called and getResults checked, then score is 0.5 [F-KTLEN/F-KTLEN-R005]
- Given a knowledge with instructions of 85 chars between soft and hard limits, when onKnowledge is called and getResults checked, then score is 0.5 [F-KTLEN/F-KTLEN-R006]
- Given a knowledge with instructions of 101 chars just above hard limit, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R005]
- Given a knowledge with instructions of 200 chars well above hard limit, when onKnowledge is called and getResults checked, then score is 0.0 [F-KTLEN/F-KTLEN-R006]
- Given a KnowledgeInstructionsLengthAnalyzer, when onQuiz is called, then it completes without error [F-KTLEN]
- Given a KnowledgeInstructionsLengthAnalyzer, when onMilestone is called, then it completes without error [F-KTLEN]
- Given a KnowledgeInstructionsLengthAnalyzer, when onTopic is called, then it completes without error [F-KTLEN]
- Given a KnowledgeInstructionsLengthAnalyzer, when onCourseComplete is called, then it completes without error [F-KTLEN]
- Given a fresh KnowledgeInstructionsLengthAnalyzer, when getResults is called without prior processing, then returns empty list
- Given three knowledges with different instruction lengths, when all are processed and getResults checked, then correct scores are produced for each [F-KTLEN/F-KTLEN-R006]

#### IContentAudit

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAudit

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `auditEngine` | `AuditEngine` |

**Generated constructor:**
```java
public IContentAudit(AuditEngine auditEngine) {
    this.auditEngine = auditEngine;
}
```

#### SentenceLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `config` | `SentenceLengthConfig` |

**Generated constructor:**
```java
public SentenceLengthAnalyzer(NlpTokenizer nlpTokenizer, SentenceLengthConfig config) {
    this.nlpTokenizer = nlpTokenizer;
    this.config = config;
}
```

**Tests that must pass:** 22

- Given a null milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty [F-SLEN/F-SLEN-R001]
- Given a non-numeric milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty [F-SLEN/F-SLEN-R001]
- Given no target range configured for level, when onQuiz is called, then quiz is excluded and getResults is empty [F-SLEN/F-SLEN-R012]
- Given multiple quizzes across sentence and non-sentence knowledges, when processed, then only sentence quizzes are scored [F-SLEN/F-SLEN-R001]
- Given a SentenceLengthAnalyzer, when getName is called, then returns sentence-length
- Given a SentenceLengthAnalyzer, when getTarget is called, then returns QUIZ
- Given a quiz within A1 range, when onQuiz is called and getResults checked, then score is 1.0 [F-SLEN/F-SLEN-R002]
- Given a quiz 1 token above A1 max, when scored, then score is 0.75 [F-SLEN/F-SLEN-R002]
- Given a quiz 3 tokens below A1 min, when scored, then score is 0.25 [F-SLEN/F-SLEN-R002]
- Given a quiz exactly at A1 minimum boundary, when scored, then score is 1.0 [F-SLEN/F-SLEN-R002]
- Given a quiz exactly at A1 maximum boundary, when scored, then score is 1.0 [F-SLEN/F-SLEN-R002]
- Given a quiz 4 tokens above A1 max, when scored, then score is 0.0 [F-SLEN/F-SLEN-R009]
- Given a non-sentence knowledge, when onQuiz is called, then quiz is excluded and getResults is empty [F-SLEN/F-SLEN-R001]
- Given a B2 level quiz within range, when scored, then score is 1.0 [F-SLEN/F-SLEN-R012]
- Given a quiz exactly at tolerance boundary of 4 tokens, when scored, then score is 0.0 [F-SLEN/F-SLEN-R009]
- Given a quiz 2 tokens above A1 max, when scored, then score is 0.5 [F-SLEN/F-SLEN-R002]
- Given a SentenceLengthAnalyzer, when onTopic is called, then it completes without error
- Given a SentenceLengthAnalyzer, when onCourseComplete is called, then it completes without error
- Given a full milestone-knowledge-quiz sequence, when processed end to end, then correct scores are produced [F-SLEN/F-SLEN-R002]
- Given a SentenceLengthAnalyzer instance, when getName is called, then returns sentence-length
- Given a SentenceLengthAnalyzer instance, when getTarget is called, then returns QUIZ
- Given a knowledge with non-sentence quizzes, when onQuiz is called, then non-sentence quizzes are excluded from scoring [F-SLEN/F-SLEN-R001]

#### IScoreAggregator

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ScoreAggregator

### Module: audit-application

#### CourseToAuditableMapper

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** CourseMapper

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |

**Generated constructor:**
```java
public CourseToAuditableMapper(NlpTokenizer nlpTokenizer) {
    this.nlpTokenizer = nlpTokenizer;
}
```

**Tests that must pass:** 3

- Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse [F-NLP/F-NLP-R010]
- Given a course with no milestones, when map is called, then returns an AuditableCourse without error [F-NLP/F-NLP-R010]
- Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates [F-NLP/F-NLP-R008]

#### DefaultSentenceLengthConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** SentenceLengthConfig

**Framework types:** Component

#### DefaultAuditRunner

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** AuditRunner

**Framework types:** Service

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `courseRepository` | `CourseRepository` |
| `courseToAuditableMapper` | `CourseToAuditableMapper` |
| `contentAudit` | `ContentAudit` |
| `courseMapper` | `CourseMapper` |

**Generated constructor:**
```java
public DefaultAuditRunner(CourseRepository courseRepository, CourseToAuditableMapper courseToAuditableMapper, ContentAudit contentAudit, CourseMapper courseMapper) {
    this.courseRepository = courseRepository;
    this.courseToAuditableMapper = courseToAuditableMapper;
    this.contentAudit = contentAudit;
    this.courseMapper = courseMapper;
}
```

**Tests that must pass:** 8

- Given a valid course path, when runAudit is called, then returns the audit report from the full chain [F-CLI/F-CLI-R001]
- Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path [F-CLI/F-CLI-R001]
- Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity [F-CLI/F-CLI-R001]
- Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course [F-CLI/F-CLI-R001]
- Given courseRepository throws an exception, when runAudit is called, then the exception propagates [F-CLI/F-CLI-R001]
- Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates [F-CLI/F-CLI-R001]
- Given contentAudit throws an exception, when runAudit is called, then the exception propagates [F-CLI/F-CLI-R001]
- Given a course with no milestones, when runAudit is called, then returns the report from contentAudit [F-CLI/F-CLI-R001]

### Module: course-infrastructure

#### FileSystemCourseRepository

**Package:** `com.learney.contentaudit.courseinfrastructure`

**Implements:** CourseRepository

**Framework types:** Repository

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `courseValidator` | `CourseValidator` |

**Generated constructor:**
```java
public FileSystemCourseRepository(CourseValidator courseValidator) {
    this.courseValidator = courseValidator;
}
```

**Tests that must pass:** 28

- Given a valid course entity, when save is called, then validator is invoked and no exception is thrown [F-COURSE/F-COURSE-R014]
- Given an invalid course entity, when save is called, then validator throws CourseValidationException [F-COURSE/F-COURSE-R014]
- Given a course entity with validator passing, when save is called, then no exception is thrown [F-COURSE]
- Given a null course entity, when save is called, then an exception is thrown [F-COURSE/F-COURSE-R009]
- Given validator rejects course with duplicate IDs, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R006]
- Given validator rejects course with broken parent references, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R008]
- Given validator rejects milestone with no topics, when save is called, then CourseValidationException propagates [F-COURSE/F-COURSE-R015]
- Given a valid course directory, when load is called, then returns CourseEntity with 5-level hierarchy including ROOT node [F-COURSE/F-COURSE-R001]
- Given a course with ordered milestones and topics, when load is called, then child order matches parent children lists [F-COURSE/F-COURSE-R002]
- Given a loaded course, when saved and reloaded, then the result is semantically equivalent to the original [F-COURSE/F-COURSE-R003]
- Given a course directory with quiz templates, when load is called, then every knowledge has at least one quiz template [F-COURSE/F-COURSE-R004]
- Given a course directory with consistent IDs, when load is called, then all child ID references resolve to existing entities [F-COURSE/F-COURSE-R005]
- Given a course directory with unique IDs, when load is called, then no duplicate IDs exist across any hierarchy level [F-COURSE/F-COURSE-R006]
- Given a valid directory structure, when load is called, then each directory level contains its expected descriptor file [F-COURSE/F-COURSE-R007]
- Given a loaded course, when inspecting parent references, then each child parentId matches its actual parent id [F-COURSE/F-COURSE-R008]
- Given a course with all required fields populated, when load is called, then all mandatory fields are non-null [F-COURSE/F-COURSE-R009]
- Given a course with empty string and null field values, when saved and reloaded, then empty and null values are preserved exactly [F-COURSE/F-COURSE-R010]
- Given quiz templates with dual id and oidId fields, when load is called, then both fields contain the same value [F-COURSE/F-COURSE-R011]
- Given quiz templates with numberDouble format values, when saved to JSON, then numeric fields preserve MongoDB Extended JSON format [F-COURSE/F-COURSE-R012]
- Given a loaded course, when inspecting order fields, then milestones topics and knowledges have sequential 1-based order within their parent [F-COURSE/F-COURSE-R013]
- Given a milestone with empty children list, when load is called, then validation rejects the course with a descriptive error [F-COURSE/F-COURSE-R015]
- Given entities with labels, when saved to disk, then directory names are deterministic slugs derived from the labels [F-COURSE/F-COURSE-R016]
- Given a course directory with full hierarchy, when loading step by step, then all levels are resolved with correct hierarchy order and validation [F-COURSE]
- Given a course in memory, when saving to a target directory, then the directory structure and JSON files are written correctly [F-COURSE]
- Given a course loaded from files, when saved to a new directory and reloaded, then the reloaded course equals the original [F-COURSE]
- Given a loaded course, when navigating from ROOT to milestones to topics to knowledges to quizzes, then each level is accessible and correctly ordered [F-COURSE]
- Given a loaded course, when a knowledge label is modified and the course is saved and reloaded, then the change is reflected and unmodified data remains intact [F-COURSE]
- Given a nonexistent path or missing descriptor or malformed JSON, when load is called, then a descriptive error is thrown and no partial course is returned [F-COURSE]

### Module: audit-cli

#### TextReportFormatter

**Package:** `com.learney.contentaudit.auditcli`

**Implements:** ReportFormatter

#### JsonReportFormatter

**Package:** `com.learney.contentaudit.auditcli`

**Implements:** ReportFormatter

#### DefaultAuditCli

**Package:** `com.learney.contentaudit.auditcli`

**Implements:** AuditCli

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `auditRunner` | `AuditRunner` |
| `formatterRegistry` | `FormatterRegistry` |

**Generated constructor:**
```java
public DefaultAuditCli(AuditRunner auditRunner, FormatterRegistry formatterRegistry) {
    this.auditRunner = auditRunner;
    this.formatterRegistry = formatterRegistry;
}
```

**Tests that must pass:** 8

- Given valid args with course path, when run is called, then returns exit code 0 [F-CLI/F-CLI-R004]
- Given no args provided, when run is called, then returns non-zero exit code [F-CLI/F-CLI-R002]
- Given auditRunner throws RuntimeException, when run is called, then returns non-zero exit code [F-CLI/F-CLI-R004]
- Given valid args with --format json, when run is called, then json formatter is looked up and returns 0 [F-CLI/F-CLI-R003]
- Given valid args without --format, when run is called, then text formatter is used by default and returns 0 [F-CLI/F-CLI-R003]
- Given valid args, when run is called, then auditRunner runAudit is invoked with course path [F-CLI/F-CLI-R001]
- Given an unsupported format value, when run is called, then returns non-zero exit code [F-CLI/F-CLI-R003]
- Given valid args and low audit scores, when run is called, then returns 0 regardless of score values [F-CLI/F-CLI-R004]

#### DefaultFormatterRegistry

**Package:** `com.learney.contentaudit.auditcli`

**Implements:** FormatterRegistry

**Framework types:** Component

### Module: nlp-infrastructure

#### SpacyNlpTokenizerFactory (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizerFactory

#### SpacyNlpTokenizer (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `processRunner` | `SpacyProcessRunner` |
| `resultParser` | `SpacyResultParser` |

#### SpacyProcessRunner (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `config` | `NlpTokenizerConfig` |

#### SpacyResultParser (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** 

#### CachedNlpTokenizer (package: spacy)

**Package:** `com.learney.contentaudit.nlpinfrastructure.spacy`
**Visibility:** public
**Implements:** NlpTokenizer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `delegate` | `NlpTokenizer` |

