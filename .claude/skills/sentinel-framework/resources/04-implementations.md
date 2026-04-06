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

**Tests that must pass:** 4

- should score 0.5 for title of weighted length 28.5 [F-KTLEN/F-KTLEN-R003]
- should score 0.0 for title of weighted length 29 [F-KTLEN/F-KTLEN-R003]
- should score 0.0 for title of weighted length 35 [F-KTLEN/F-KTLEN-R003]
- should score 0.0 for title well beyond limit at weighted length 70 [F-KTLEN/F-KTLEN-R003]

#### KnowledgeInstructionsLengthAnalyzer

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ContentAnalyzer

**Tests that must pass:** 8

- should score 1.0 for instructions exactly at soft limit of 70 weighted chars [F-KTLEN/F-KTLEN-R005]
- should score 1.0 for instructions of 30 weighted chars within soft limit [F-KTLEN/F-KTLEN-R006]
- should score 0.5 for instructions of 71 weighted chars just above soft limit [F-KTLEN/F-KTLEN-R005]
- should score 0.5 for instructions exactly at hard limit of 100 weighted chars [F-KTLEN/F-KTLEN-R005]
- should score 0.5 for instructions of 85 weighted chars between soft and hard limits [F-KTLEN/F-KTLEN-R006]
- should score 0.0 for instructions of 101 weighted chars just above hard limit [F-KTLEN/F-KTLEN-R005]
- should score 0.0 for instructions of 200 weighted chars well above hard limit [F-KTLEN/F-KTLEN-R006]
- should use weighted character length not plain string length for scoring instructions [F-KTLEN/F-KTLEN-R005]

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

#### IScoreAggregator

**Package:** `com.learney.contentaudit.auditdomain`

**Implements:** ScoreAggregator

#### CocaBucketsAnalyzer (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `nlpTokenizer` | `NlpTokenizer` |
| `cocaBucketsConfig` | `CocaBucketsConfig` |
| `tokenClassifier` | `TokenClassifier` |
| `progressionEvaluator` | `ProgressionEvaluator` |
| `improvementPlanner` | `ImprovementPlanner` |

#### CocaTokenAccumulationAggregator (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ScoreAggregator

#### DefaultTokenClassifier (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** TokenClassifier

#### DefaultProgressionEvaluator (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ProgressionEvaluator

#### DefaultImprovementPlanner (package: coca)

**Package:** `com.learney.contentaudit.auditdomain.coca`
**Visibility:** internal
**Implements:** ImprovementPlanner

#### LemmaRecurrenceAnalyzer (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaRecurrenceConfig` | `LemmaRecurrenceConfig` |
| `intervalCalculator` | `IntervalCalculator` |
| `exposureClassifier` | `ExposureClassifier` |

#### DefaultContentWordFilter (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ContentWordFilter

**Tests:** 6

#### DefaultIntervalCalculator (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** IntervalCalculator

**Tests:** 4

#### DefaultExposureClassifier (package: lrec)

**Package:** `com.learney.contentaudit.auditdomain.lrec`
**Visibility:** internal
**Implements:** ExposureClassifier

#### LemmaByLevelAbsenceAnalyzer (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
**Implements:** ContentAnalyzer

**Constructor dependencies:**

| Name | Type |
|------|------|
| `evpCatalogPort` | `EvpCatalogPort` |
| `contentWordFilter` | `ContentWordFilter` |
| `lemmaAbsenceConfig` | `LemmaAbsenceConfig` |

#### LemmaAbsenceScoreAggregator (package: labs)

**Package:** `com.learney.contentaudit.auditdomain.labs`
**Visibility:** internal
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
| `auditEngine` | `AuditEngine` |

**Generated constructor:**
```java
public DefaultAuditRunner(CourseRepository courseRepository, CourseToAuditableMapper courseToAuditableMapper, ContentAudit contentAudit, CourseMapper courseMapper, AuditEngine auditEngine) {
    this.courseRepository = courseRepository;
    this.courseToAuditableMapper = courseToAuditableMapper;
    this.contentAudit = contentAudit;
    this.courseMapper = courseMapper;
    this.auditEngine = auditEngine;
}
```

#### DefaultCocaBucketsConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** CocaBucketsConfig

**Framework types:** Component

#### DefaultLemmaRecurrenceConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaRecurrenceConfig

**Framework types:** Component

#### DefaultLemmaAbsenceConfig

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** LemmaAbsenceConfig

**Framework types:** Component

**Tests that must pass:** 9

- should have alert thresholds non-decreasing from high to low priority [FEAT-LABS/F-LABS-R014]
- should enforce zero tolerance for high priority alert threshold [FEAT-LABS/F-LABS-R014]
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero [FEAT-LABS/F-LABS-R021]
- should have discount per level that limits max penalty to 0.3 for three-level distance [FEAT-LABS/F-LABS-R018]
- should return non-negative values for all thresholds and bounds [FEAT-LABS/F-LABS-R021]
- should return positive report limits for all priority levels [FEAT-LABS/F-LABS-R026]
- should return percentage thresholds between 0 and 100 for all levels [FEAT-LABS/F-LABS-R021]
- should return positive level weights for all CEFR levels [FEAT-LABS/F-LABS-R024]
- should return discount per level between 0 exclusive and 1 exclusive [FEAT-LABS/F-LABS-R018]

#### DefaultAnalyzerRegistry

**Package:** `com.learney.contentaudit.auditapplication`

**Implements:** AnalyzerRegistry

**Framework types:** Component

**Constructor dependencies (requiresInject):**

| Name | Type |
|------|------|
| `analyzers` | `List<ContentAnalyzer>` |
| `configs` | `List<SelfDescribingConfig>` |

**Generated constructor:**
```java
public DefaultAnalyzerRegistry(List<ContentAnalyzer> analyzers, List<SelfDescribingConfig> configs) {
    this.analyzers = analyzers;
    this.configs = configs;
}
```

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

#### Main (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

#### ContentAuditCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

#### AnalyzeCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `auditRunner` | `AuditRunner` |
| `formatterRegistry` | `FormatterRegistry` |
| `viewModelTransformer` | `ReportViewModelTransformer` |
| `rawReportFormatter` | `RawReportFormatter` |
| `drillDownResolver` | `DrillDownResolver` |
| `detailedFormatters` | `Map<String,DetailedFormatter>` |

#### AnalyzerCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

#### AnalyzerListCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerRegistry` | `AnalyzerRegistry` |

#### AnalyzerConfigCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerRegistry` | `AnalyzerRegistry` |

#### AnalyzerStatsCmd (package: commands)

**Package:** `com.learney.contentaudit.auditcli.commands`
**Visibility:** public
**Implements:** 

**Constructor dependencies:**

| Name | Type |
|------|------|
| `analyzerRegistry` | `AnalyzerRegistry` |
| `analyzerStatsTransformer` | `AnalyzerStatsTransformer` |
| `auditRunner` | `AuditRunner` |

#### TextReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### JsonReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### TableReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportFormatter

**Constructor dependencies:**

| Name | Type |
|------|------|
| `drillDownResolver` | `DrillDownResolver` |

#### DefaultFormatterRegistry (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** FormatterRegistry

#### DefaultReportViewModelTransformer (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** ReportViewModelTransformer

#### RawJsonReportFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** RawReportFormatter

#### DefaultDrillDownResolver (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DrillDownResolver

#### DefaultAnalyzerStatsTransformer (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** AnalyzerStatsTransformer

#### LemmaAbsenceDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

#### CocaBucketsDetailedFormatter (package: formatting)

**Package:** `com.learney.contentaudit.auditcli.formatting`
**Visibility:** internal
**Implements:** DetailedFormatter

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

### Module: vocabulary-infrastructure

#### FileSystemEvpCatalog (package: evp)

**Package:** `com.learney.contentaudit.vocabularyinfrastructure.evp`
**Visibility:** internal
**Implements:** EvpCatalogPort

