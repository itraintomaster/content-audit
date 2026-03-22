# Declarative Tests

Sentinel tests are **behavioral contracts** defined in YAML and compiled into immutable JUnit 5 + Mockito code.
The AI agent must write implementation code that passes these tests **without modifying them**.

## Test Anatomy: Four Phases

Every test follows the Arrange-Act-Assert pattern with four phases:

### Phase 1: Fixtures (Object Setup)

```yaml
fixtures:
  - id: "taskInput"             # Reference ID (used in mocks/invoke/assert)
    type: "com.app.domain.Task"  # Fully qualified type
    construct:                    # Constructor arguments
      id: "uuid-1"
      title: "Buy Milk"
      completed: false
```

**Generated code:** `Task taskInput = new Task("uuid-1", "Buy Milk", false);`

- UUID strings are wrapped in `UUID.fromString()`
- Fixtures can reference other fixtures by ID
- Complex objects default to `null` with a TODO comment

### Phase 2: Mocks (Behavior Setup)

```yaml
mocks:
  - dependency: "repository"   # Field name in the implementation class
    method: "save"              # Method to mock
    args:                        # Argument matchers
      - match: "eq"             # Exact match
        value: "expected-id"
      - match: "any"            # Any value
    thenReturn: "fixture-id"    # Return value (fixture reference or literal)
```

**Argument Matchers:**

| YAML | Generated Code | Use Case |
|------|---------------|----------|
| `match: eq, value: "X"` | `Mockito.eq("X")` | Exact value match |
| `match: any` | `Mockito.any()` | Accept any argument |
| `match: isA, type: "com.app.Task"` | `Mockito.isA(Task.class)` | Type-based match |
| `match: capture` | `captor.capture()` | Capture for later assertion |

**Exception Mocking:**

```yaml
mocks:
  - dependency: "gateway"
    method: "charge"
    thenThrow:
      type: "java.lang.RuntimeException"
      message: "Payment declined"
```

When `thenThrow` is used, Mockito `lenient()` mode is applied to prevent `UnnecessaryStubbingException`.

### Phase 3: Invoke (Action)

```yaml
invoke:
  method: "processUser"     # Method name on the SUT
  args: ["userInput"]        # Arguments (fixture IDs or literals)
```

**Generated code:** `Object result = sut.processUser(userInput);`

### Phase 4: Assert (Verification)

```yaml
assert:
  doesNotThrow: true          # Asserts no exception is thrown
  returns: "expectedValue"    # Asserts return value equals this
  assertThrows: "java.lang.IllegalArgumentException"  # Expects exception
  verifyCall:                  # Verifies side-effect
    dependency: "repository"
    method: "save"
    times: 1
    message: "Save must be called once"
```

**Assert options:**

| Field | Effect |
|-------|---------|
| `doesNotThrow: true` | `Assertions.assertDoesNotThrow(() -> ...)` |
| `returns: "X"` | `Assertions.assertEquals(X, result)` |
| `assertThrows: "ExType"` | `Assertions.assertThrows(ExType.class, () -> ...)` |
| `verifyCall` | `Mockito.verify(dep, times(N)).method(...)` |

## Integration Tests (Multi-Step)

```yaml
- name: "Full lifecycle test"
  type: integration
  fixtures: [...]              # Shared fixtures
  steps:
    - mocks: [...]             # Step-specific mocks
      invoke: { method: "create", args: [...] }
      assert: { doesNotThrow: true }
    - mocks: [...]             # Different mocks for step 2
      invoke: { method: "find", args: [...] }
      assert: { returns: "expected" }
```

Steps execute sequentially, each with independent mocks and assertions.

## Generated Test Structure

```java
@Generated(value = "com.sentinel.SentinelEngine")
@ExtendWith(MockitoExtension.class)
public class MyAdapterSentinelTest {
    @Mock private SomeDependency dependency;
    @InjectMocks private MyAdapter sut;

    @Test
    @Tag("FEAT-001")
    void shouldDoSomething() {
        // Fixtures
        // Mocks (Mockito.when...)
        // Invoke (sut.method(...))
        // Assert (Assertions + Mockito.verify)
    }
}
```

## Tests in This System

### SentenceLengthAnalyzer (audit-domain)

**Test class:** `SentenceLengthAnalyzerSentinelTest`

#### Given a null milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty

- **Type:** integration
- **Fixtures:** milestone, ctxMilestoneNull, knowledge, ctxKnowledge, quiz, ctxQuiz
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R001
- **Steps:** 4 integration steps

#### Given a non-numeric milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty

- **Type:** integration
- **Fixtures:** milestone, ctxMilestoneAbc, knowledge, ctxKnowledge, quiz, ctxQuiz
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R001
- **Steps:** 4 integration steps

#### Given no target range configured for level, when onQuiz is called, then quiz is excluded and getResults is empty

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R012
- **Steps:** 4 integration steps

#### Given a valid sentence quiz, when onQuiz is called, then nlpTokenizer countTokens is called with quiz sentence

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R013
- **Steps:** 3 integration steps

#### Given multiple quizzes across sentence and non-sentence knowledges, when processed, then only sentence quizzes are scored

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, nonSentenceKnowledge, ctxNonSentKnowledge, nonSentQuiz, ctxNonSentQuiz, sentenceKnowledge, ctxSentKnowledge, sentQuiz, ctxSentQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R001
- **Steps:** 6 integration steps

#### Given a SentenceLengthAnalyzer, when getName is called, then returns sentence-length

- **Type:** unit
- **Invokes:** `getName()`
- **Asserts:** returns=sentence-length

#### Given a SentenceLengthAnalyzer, when getTarget is called, then returns QUIZ

- **Type:** unit
- **Invokes:** `getTarget()`
- **Asserts:** returns=QUIZ

#### Given a quiz within A1 range, when onQuiz is called and getResults checked, then score is 1.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a quiz 1 token above A1 max, when scored, then score is 0.75

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a quiz 3 tokens below A1 min, when scored, then score is 0.25

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a quiz exactly at A1 minimum boundary, when scored, then score is 1.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a quiz exactly at A1 maximum boundary, when scored, then score is 1.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a quiz 4 tokens above A1 max, when scored, then score is 0.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R009
- **Steps:** 4 integration steps

#### Given a non-sentence knowledge, when onQuiz is called, then quiz is excluded and getResults is empty

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R001
- **Steps:** 4 integration steps

#### Given a B2 level quiz within range, when scored, then score is 1.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeB2, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R012
- **Steps:** 4 integration steps

#### Given a quiz exactly at tolerance boundary of 4 tokens, when scored, then score is 0.0

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R009
- **Steps:** 4 integration steps

#### Given a quiz 2 tokens above A1 max, when scored, then score is 0.5

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz, rangeA1, expectedScore
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 4 integration steps

#### Given a SentenceLengthAnalyzer, when onTopic is called, then it completes without error

- **Type:** unit
- **Fixtures:** topic, ctx
- **Invokes:** `onTopic(ref:topic, ref:ctx)`
- **Asserts:** doesNotThrow

#### Given a SentenceLengthAnalyzer, when onCourseComplete is called, then it completes without error

- **Type:** unit
- **Fixtures:** course, ctx
- **Invokes:** `onCourseComplete(ref:course, ref:ctx)`
- **Asserts:** doesNotThrow

#### Given a full milestone-knowledge-quiz sequence, when processed end to end, then correct scores are produced

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz1, ctxQuiz1, quiz2, ctxQuiz2, rangeA1, expectedScore1, expectedScore2
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R002
- **Steps:** 5 integration steps

#### Given a SentenceLengthAnalyzer instance, when getName is called, then returns sentence-length

- **Type:** unit
- **Invokes:** `getName()`
- **Asserts:** returns=sentence-length

#### Given a SentenceLengthAnalyzer instance, when getTarget is called, then returns QUIZ

- **Type:** unit
- **Invokes:** `getTarget()`
- **Asserts:** returns=QUIZ

#### Given a knowledge with non-sentence quizzes, when onQuiz is called, then non-sentence quizzes are excluded from scoring

- **Type:** integration
- **Fixtures:** milestone, ctxMilestone, knowledge, ctxKnowledge, quiz, ctxQuiz
- **Traceability:** feature=F-SLEN, rule=F-SLEN-R001
- **Steps:** 4 integration steps

### FileSystemCourseRepository (course-infrastructure)

**Test class:** `FileSystemCourseRepositorySentinelTest`

#### Given a valid course entity, when save is called, then validator is invoked and no exception is thrown

- **Type:** unit
- **Fixtures:** sentencePart1, sentencePart2, form1, quiz1, knowledge1, topic1, milestone1, rootNode, course1, targetPath
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:course1, ref:targetPath)`
- **Asserts:** verifyCall
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R014

#### Given an invalid course entity, when save is called, then validator throws CourseValidationException

- **Type:** unit
- **Fixtures:** invalidCourse, targetPath
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:invalidCourse, ref:targetPath)`
- **Asserts:** assertThrows=CourseValidationException
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R014

#### Given a course entity with validator passing, when save is called, then no exception is thrown

- **Type:** unit
- **Fixtures:** sentencePart1, sentencePart2, form1, quiz1, knowledge1, topic1, milestone1, rootNode, validCourse, targetPath
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:validCourse, ref:targetPath)`
- **Asserts:** doesNotThrow
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J002

#### Given a null course entity, when save is called, then an exception is thrown

- **Type:** unit
- **Invokes:** `save(null, /tmp/test-null-output)`
- **Asserts:** assertThrows=IllegalArgumentException
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R009

#### Given validator rejects course with duplicate IDs, when save is called, then CourseValidationException propagates

- **Type:** unit
- **Fixtures:** duplicateIdCourse
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:duplicateIdCourse, /tmp/test-dup-output)`
- **Asserts:** assertThrows=CourseValidationException
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R006

#### Given validator rejects course with broken parent references, when save is called, then CourseValidationException propagates

- **Type:** unit
- **Fixtures:** brokenRefCourse
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:brokenRefCourse, /tmp/test-broken-ref-output)`
- **Asserts:** assertThrows=CourseValidationException
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R008

#### Given validator rejects milestone with no topics, when save is called, then CourseValidationException propagates

- **Type:** unit
- **Fixtures:** emptyMilestoneCourse
- **Mocks:** courseValidator.validate
- **Invokes:** `save(ref:emptyMilestoneCourse, /tmp/test-empty-milestone-output)`
- **Asserts:** assertThrows=CourseValidationException
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R015

#### Given a valid course directory, when load is called, then returns CourseEntity with 5-level hierarchy including ROOT node

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R001

#### Given a course with ordered milestones and topics, when load is called, then child order matches parent children lists

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R002

#### Given a loaded course, when saved and reloaded, then the result is semantically equivalent to the original

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R003

#### Given a course directory with quiz templates, when load is called, then every knowledge has at least one quiz template

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R004

#### Given a course directory with consistent IDs, when load is called, then all child ID references resolve to existing entities

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R005

#### Given a course directory with unique IDs, when load is called, then no duplicate IDs exist across any hierarchy level

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R006

#### Given a valid directory structure, when load is called, then each directory level contains its expected descriptor file

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R007

#### Given a loaded course, when inspecting parent references, then each child parentId matches its actual parent id

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R008

#### Given a course with all required fields populated, when load is called, then all mandatory fields are non-null

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R009

#### Given a course with empty string and null field values, when saved and reloaded, then empty and null values are preserved exactly

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R010

#### Given quiz templates with dual id and oidId fields, when load is called, then both fields contain the same value

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R011

#### Given quiz templates with numberDouble format values, when saved to JSON, then numeric fields preserve MongoDB Extended JSON format

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R012

#### Given a loaded course, when inspecting order fields, then milestones topics and knowledges have sequential 1-based order within their parent

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R013

#### Given a milestone with empty children list, when load is called, then validation rejects the course with a descriptive error

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R015

#### Given entities with labels, when saved to disk, then directory names are deterministic slugs derived from the labels

- **Type:** integration
- **Traceability:** feature=F-COURSE, rule=F-COURSE-R016

#### Given a course directory with full hierarchy, when loading step by step, then all levels are resolved with correct hierarchy order and validation

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J001

#### Given a course in memory, when saving to a target directory, then the directory structure and JSON files are written correctly

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J002

#### Given a course loaded from files, when saved to a new directory and reloaded, then the reloaded course equals the original

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J003

#### Given a loaded course, when navigating from ROOT to milestones to topics to knowledges to quizzes, then each level is accessible and correctly ordered

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J004

#### Given a loaded course, when a knowledge label is modified and the course is saved and reloaded, then the change is reflected and unmodified data remains intact

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J005

#### Given a nonexistent path or missing descriptor or malformed JSON, when load is called, then a descriptive error is thrown and no partial course is returned

- **Type:** integration
- **Traceability:** feature=F-COURSE, journey=F-COURSE-J006

