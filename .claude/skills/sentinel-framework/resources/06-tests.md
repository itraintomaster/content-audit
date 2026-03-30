# Tests

Sentinel tests follow a **handwritten stub** model. Test names are declared in `sentinel.yaml` under `handwrittenTests`, and `sentinel generate` creates JUnit stub classes. The developer writes the actual test implementation.

## Test Stub Structure

```java
@Generated(value = "com.sentinel.SentinelEngine")
public class MyAdapterTest {

    @Test
    @DisplayName("should save and return entity")
    @Tag("FEAT-001")
    public void shouldSaveAndReturnEntity() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
```

Replace the `throw` with real test code. The class has no Mockito annotations — choose your own testing approach (Mockito, fakes, integration, etc.).

## Smart Merge

Running `sentinel generate` again adds new stub methods for new test names without overwriting existing implementations.

## Tests in This System

### KnowledgeTitleLengthAnalyzer (audit-domain)

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

### KnowledgeInstructionsLengthAnalyzer (audit-domain)

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

### SentenceLengthAnalyzer (audit-domain)

- Given a null milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R001
- Given a non-numeric milestoneId, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R001
- Given no target range configured for level, when onQuiz is called, then quiz is excluded and getResults is empty → F-SLEN/F-SLEN-R012
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

### CourseToAuditableMapper (audit-application)

- Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse → F-NLP/F-NLP-R010
- Given a course with no milestones, when map is called, then returns an AuditableCourse without error → F-NLP/F-NLP-R010
- Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates → F-NLP/F-NLP-R008

### DefaultAuditRunner (audit-application)

- Given a valid course path, when runAudit is called, then returns the audit report from the full chain → F-CLI/F-CLI-R001/F-CLI-J001
- Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path → F-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity → F-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course → F-CLI/F-CLI-R001
- Given courseRepository throws an exception, when runAudit is called, then the exception propagates → F-CLI/F-CLI-R001
- Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates → F-CLI/F-CLI-R001
- Given contentAudit throws an exception, when runAudit is called, then the exception propagates → F-CLI/F-CLI-R001
- Given a course with no milestones, when runAudit is called, then returns the report from contentAudit → F-CLI/F-CLI-R001

### DefaultLemmaAbsenceConfig (audit-application)

- should return absolute threshold 0 for A1 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 2 for A2 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 5 for B1 → FEAT-LABS/F-LABS-R021
- should return absolute threshold 8 for B2 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 0.0 for A1 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 5.0 for A2 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 10.0 for B1 → FEAT-LABS/F-LABS-R021
- should return percentage threshold 15.0 for B2 → FEAT-LABS/F-LABS-R021
- should return level weight 2.0 for A1 → FEAT-LABS/F-LABS-R024
- should return level weight 2.0 for A2 → FEAT-LABS/F-LABS-R024
- should return level weight 1.0 for B1 → FEAT-LABS/F-LABS-R024
- should return level weight 1.0 for B2 → FEAT-LABS/F-LABS-R024
- should return high priority bound of 1000 → FEAT-LABS/F-LABS-R011
- should return medium priority bound of 3000 → FEAT-LABS/F-LABS-R011
- should return low priority bound of 5000 → FEAT-LABS/F-LABS-R011
- should return high priority alert threshold of 0 → FEAT-LABS/F-LABS-R014
- should return medium priority alert threshold of 3 → FEAT-LABS/F-LABS-R014
- should return low priority alert threshold of 10 → FEAT-LABS/F-LABS-R014
- should return critical absence threshold of 10 → FEAT-LABS/F-LABS-R025
- should return acceptable absence threshold of 5 → FEAT-LABS/F-LABS-R025
- should return high report limit of 20 → FEAT-LABS/F-LABS-R026
- should return medium report limit of 30 → FEAT-LABS/F-LABS-R026
- should return low report limit of 50 → FEAT-LABS/F-LABS-R026
- should return discount per level of 0.1 → FEAT-LABS/F-LABS-R018
- should have absolute thresholds increasing from A1 to B2 → FEAT-LABS/F-LABS-R021
- should have percentage thresholds increasing from A1 to B2 → FEAT-LABS/F-LABS-R021
- should have priority bounds ordered high less than medium less than low → FEAT-LABS/F-LABS-R011
- should weight critical levels A1 and A2 higher than B1 and B2 → FEAT-LABS/F-LABS-R024
- should have report limits increasing from high to low priority → FEAT-LABS/F-LABS-R026
- should have critical absence threshold greater than acceptable absence threshold → FEAT-LABS/F-LABS-R025
- should have alert thresholds non-decreasing from high to low priority → FEAT-LABS/F-LABS-R014
- should enforce zero tolerance for high priority alert threshold → FEAT-LABS/F-LABS-R014
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero → FEAT-LABS/F-LABS-R021
- should have discount per level that limits max penalty to 0.3 for three-level distance → FEAT-LABS/F-LABS-R018
- should return non-negative values for all thresholds and bounds → FEAT-LABS/F-LABS-R021
- should return positive report limits for all priority levels → FEAT-LABS/F-LABS-R026
- should return percentage thresholds between 0 and 100 for all levels → FEAT-LABS/F-LABS-R021
- should return positive level weights for all CEFR levels → FEAT-LABS/F-LABS-R024
- should return discount per level between 0 exclusive and 1 exclusive → FEAT-LABS/F-LABS-R018

### FileSystemCourseRepository (course-infrastructure)

- Given a valid course entity, when save is called, then validator is invoked and no exception is thrown → F-COURSE/F-COURSE-R014
- Given an invalid course entity, when save is called, then validator throws CourseValidationException → F-COURSE/F-COURSE-R014
- Given a course entity with validator passing, when save is called, then no exception is thrown → F-COURSE/F-COURSE-J002
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
- Given a course directory with full hierarchy, when loading step by step, then all levels are resolved with correct hierarchy order and validation → F-COURSE/F-COURSE-J001
- Given a course in memory, when saving to a target directory, then the directory structure and JSON files are written correctly → F-COURSE/F-COURSE-J002
- Given a course loaded from files, when saved to a new directory and reloaded, then the reloaded course equals the original → F-COURSE/F-COURSE-J003
- Given a loaded course, when navigating from ROOT to milestones to topics to knowledges to quizzes, then each level is accessible and correctly ordered → F-COURSE/F-COURSE-J004
- Given a loaded course, when a knowledge label is modified and the course is saved and reloaded, then the change is reflected and unmodified data remains intact → F-COURSE/F-COURSE-J005
- Given a nonexistent path or missing descriptor or malformed JSON, when load is called, then a descriptive error is thrown and no partial course is returned → F-COURSE/F-COURSE-J006

### DefaultAuditCli (audit-cli)

- Given valid args with course path, when run is called, then returns exit code 0 → F-CLI/F-CLI-R004
- Given no args provided, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R002
- Given auditRunner throws RuntimeException, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R004
- Given valid args with --format json, when run is called, then json formatter is looked up and returns 0 → F-CLI/F-CLI-R003
- Given valid args without --format, when run is called, then text formatter is used by default and returns 0 → F-CLI/F-CLI-R003
- Given valid args, when run is called, then auditRunner runAudit is invoked with course path → F-CLI/F-CLI-R001
- Given an unsupported format value, when run is called, then returns non-zero exit code → F-CLI/F-CLI-R003
- Given valid args and low audit scores, when run is called, then returns 0 regardless of score values → F-CLI/F-CLI-R004

