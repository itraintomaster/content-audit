<!-- SENTINEL MANAGED FILE - DO NOT EDIT -->
# Module: course-infrastructure (isolated)

**This module is isolated.** Your scope is limited to this module and the contracts (models and interfaces) of its dependencies. Do not access information from other modules.

Infrastructure module for course persistence. Contains the filesystem adapter that reads/writes the hierarchical directory structure with MongoDB Extended JSON format. Handles directory traversal, JSON parsing/serialization, slug generation, and $oid/$numberDouble format preservation.

## Implementations

### FileSystemCourseRepository

**Implements:** CourseRepository

**Types:** Repository

**Dependencies (constructor injection):**

- `courseValidator`: `CourseValidator`

**Tests that must pass:**

- Given a valid course entity, when save is called, then validator is invoked and no exception is thrown → F-COURSE/F-COURSE-R014
- Given an invalid course entity, when save is called, then validator throws CourseValidationException → F-COURSE/F-COURSE-R014
- Given a course entity with validator passing, when save is called, then no exception is thrown → F-COURSE
- Given a null course entity, when save is called, then an exception is thrown → F-COURSE/F-COURSE-R009
- Given validator rejects course with duplicate IDs, when save is called, then CourseValidationException propagates → F-COURSE/F-COURSE-R006
- Given validator rejects course with broken parent references, when save is called, then CourseValidationException propagates → F-COURSE/F-COURSE-R008
- Given validator rejects milestone with no topics, when save is called, then CourseValidationException propagates → F-COURSE/F-COURSE-R015
- Given a valid course directory, when load is called, then returns CourseEntity with 5-level hierarchy including ROOT node → F-COURSE/F-COURSE-R001
- Given a course with ordered milestones and topics, when load is called, then child order matches parent children lists → F-COURSE/F-COURSE-R002
- Given a loaded course, when saved and reloaded, then the result is semantically equivalent to the original → F-COURSE/F-COURSE-R003
- Given a course directory with quiz templates, when load is called, then every knowledge has at least one quiz template → F-COURSE/F-COURSE-R004
- Given a course directory with consistent IDs, when load is called, then all child ID references resolve to existing entities → F-COURSE/F-COURSE-R005
- Given a course directory with unique IDs, when load is called, then no duplicate IDs exist across any hierarchy level → F-COURSE/F-COURSE-R006
- Given a valid directory structure, when load is called, then each directory level contains its expected descriptor file → F-COURSE/F-COURSE-R007
- Given a loaded course, when inspecting parent references, then each child parentId matches its actual parent id → F-COURSE/F-COURSE-R008
- Given a course with all required fields populated, when load is called, then all mandatory fields are non-null → F-COURSE/F-COURSE-R009
- Given a course with empty string and null field values, when saved and reloaded, then empty and null values are preserved exactly → F-COURSE/F-COURSE-R010
- Given quiz templates with dual id and oidId fields, when load is called, then both fields contain the same value → F-COURSE/F-COURSE-R011
- Given quiz templates with numberDouble format values, when saved to JSON, then numeric fields preserve MongoDB Extended JSON format → F-COURSE/F-COURSE-R012
- Given a loaded course, when inspecting order fields, then milestones topics and knowledges have sequential 1-based order within their parent → F-COURSE/F-COURSE-R013
- Given a milestone with empty children list, when load is called, then validation rejects the course with a descriptive error → F-COURSE/F-COURSE-R015
- Given entities with labels, when saved to disk, then directory names are deterministic slugs derived from the labels → F-COURSE/F-COURSE-R016
- Given a course directory with full hierarchy, when loading step by step, then all levels are resolved with correct hierarchy order and validation → F-COURSE
- Given a course in memory, when saving to a target directory, then the directory structure and JSON files are written correctly → F-COURSE
- Given a course loaded from files, when saved to a new directory and reloaded, then the reloaded course equals the original → F-COURSE
- Given a loaded course, when navigating from ROOT to milestones to topics to knowledges to quizzes, then each level is accessible and correctly ordered → F-COURSE
- Given a loaded course, when a knowledge label is modified and the course is saved and reloaded, then the change is reflected and unmodified data remains intact → F-COURSE
- Given a nonexistent path or missing descriptor or malformed JSON, when load is called, then a descriptive error is thrown and no partial course is returned → F-COURSE

## Dependency Contracts

The following models and interfaces are available from dependencies. You can use these types but cannot see their implementations.

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

### FormEntity (`record`)

| Field | Type |
|-------|------|
| kind | `String` |
| incidence | `double` |
| label | `String` |
| name | `String` |
| sentenceParts | `List<SentencePartEntity>` |

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

### CourseRepository (port)

Methods:

- `load(Path path): CourseEntity`
- `save(CourseEntity course, Path path): void`

### CourseValidator (service)

Methods:

- `validate(CourseEntity course): void`

