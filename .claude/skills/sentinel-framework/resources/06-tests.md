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

### KnowledgeInstructionsLengthAnalyzer (audit-domain)

- should score 1.0 for instructions exactly at soft limit of 70 weighted chars → FEAT-KTLEN/F-KTLEN-R005
- should score 1.0 for instructions of 30 weighted chars within soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 weighted chars just above soft limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions exactly at hard limit of 100 weighted chars → FEAT-KTLEN/F-KTLEN-R005
- should score 0.5 for instructions of 85 weighted chars between soft and hard limits → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 weighted chars just above hard limit → FEAT-KTLEN/F-KTLEN-R005
- should score 0.0 for instructions of 200 weighted chars well above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should use weighted character length not plain string length for scoring instructions → FEAT-KTLEN/F-KTLEN-R005/F-KTLEN-J003
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

### SentenceLengthAnalyzer (audit-domain)

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

### SentenceLengthContextResolver (refiner-domain)

- should resolve context with all fields populated from quiz diagnosis and ancestor entities → FEAT-RCSL/F-RCSL-R001
- should populate sentence and translation from AuditableQuiz entity on the quiz node → FEAT-RCSL/F-RCSL-R001
- should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor → FEAT-RCSL/F-RCSL-R001
- should populate topicLabel from AuditableTopic on topic ancestor → FEAT-RCSL/F-RCSL-R001
- should populate cefrLevel tokenCount targetMin targetMax and delta from SentenceLengthDiagnosis → FEAT-RCSL/F-RCSL-R001
- should return empty when quiz node is not found in the audit tree → FEAT-RCSL/F-RCSL-R002
- should return empty when task nodeTarget does not match any node target in the tree → FEAT-RCSL/F-RCSL-R002
- should locate the correct quiz node when multiple quiz nodes exist in the tree → FEAT-RCSL/F-RCSL-R002
- should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas and exclude APPEARS_TOO_EARLY → FEAT-RCSL/F-RCSL-R003
- should order suggested lemmas by COCA rank ascending with lowest rank first → FEAT-RCSL/F-RCSL-R003
- should place lemmas without COCA rank after lemmas with COCA rank → FEAT-RCSL/F-RCSL-R003
- should map AbsentLemma fields to SuggestedLemma fields correctly → FEAT-RCSL/F-RCSL-R003
- should return empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY → FEAT-RCSL/F-RCSL-R003
- should return suggested lemmas from the milestone ancestor of the quiz node → FEAT-RCSL/F-RCSL-R003
- should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis → FEAT-RCSL/F-RCSL-R004
- should return context with empty suggested lemmas when milestone ancestor is not found → FEAT-RCSL/F-RCSL-R004
- should return context with empty suggested lemmas when absent lemmas list is empty → FEAT-RCSL/F-RCSL-R004
- should limit suggested lemmas to 10 when more than 10 qualify after filtering → FEAT-RCSL/F-RCSL-R005
- should resolve context with negative delta for a sentence shorter than target range → FEAT-RCSL/F-RCSL-R001
- should resolve context with zero delta when sentence is within target range → FEAT-RCSL/F-RCSL-R001
- should set taskId from the RefinementTask id → FEAT-RCSL/F-RCSL-R001

### LemmaAbsenceContextResolver (refiner-domain)

- should resolve context with all fields populated from quiz diagnosis and ancestor entities → FEAT-RCLA/F-RCLA-R003
- should populate sentence and translation from AuditableQuiz entity on the quiz node → FEAT-RCLA/F-RCLA-R003
- should populate knowledgeTitle and knowledgeInstructions from AuditableKnowledge on knowledge ancestor → FEAT-RCLA/F-RCLA-R003
- should populate topicLabel from AuditableTopic on topic ancestor → FEAT-RCLA/F-RCLA-R003
- should populate cefrLevel from milestone ancestor → FEAT-RCLA/F-RCLA-R003
- should populate misplacedLemmas from LemmaPlacementDiagnosis on quiz node → FEAT-RCLA/F-RCLA-R004
- should map MisplacedLemma fields to MisplacedLemmaContext fields correctly → FEAT-RCLA/F-RCLA-R004
- should include expectedLevel and quizLevel in each MisplacedLemmaContext entry → FEAT-RCLA/F-RCLA-R004
- should include cocaRank as null in MisplacedLemmaContext when not available → FEAT-RCLA/F-RCLA-R004
- should return empty when quiz node is not found in the audit tree → FEAT-RCLA/F-RCLA-R005
- should return empty when task nodeTarget does not match any node target in the tree → FEAT-RCLA/F-RCLA-R005
- should locate the correct quiz node when multiple quiz nodes exist in the tree → FEAT-RCLA/F-RCLA-R005
- should return empty when quiz node has no LemmaPlacementDiagnosis → FEAT-RCLA/F-RCLA-R006
- should include only COMPLETELY_ABSENT and APPEARS_TOO_LATE lemmas in suggestedLemmas and exclude APPEARS_TOO_EARLY → FEAT-RCLA/F-RCLA-R004b
- should order suggested lemmas by COCA rank ascending with lowest rank first → FEAT-RCLA/F-RCLA-R004b
- should place lemmas without COCA rank after lemmas with COCA rank in suggestedLemmas → FEAT-RCLA/F-RCLA-R004b
- should map AbsentLemma fields to SuggestedLemma fields correctly → FEAT-RCLA/F-RCLA-R004b
- should limit suggested lemmas to 10 when more than 10 qualify after filtering → FEAT-RCLA/F-RCLA-R004b
- should return context with empty suggested lemmas when milestone has no LemmaAbsenceLevelDiagnosis → FEAT-RCLA/F-RCLA-R004c
- should return context with empty suggested lemmas when milestone ancestor is not found → FEAT-RCLA/F-RCLA-R004c
- should return context with empty suggested lemmas when all absent lemmas are APPEARS_TOO_EARLY → FEAT-RCLA/F-RCLA-R004c
- should set taskId from the RefinementTask id → FEAT-RCLA/F-RCLA-R003

### DispatchingCorrectionContextResolver (refiner-domain)

- should delegate to sentenceLengthResolver when task diagnosis is SENTENCE_LENGTH → FEAT-RCLA/F-RCLA-R007
- should delegate to lemmaAbsenceResolver when task diagnosis is LEMMA_ABSENCE → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind COCA_BUCKETS → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind LEMMA_RECURRENCE → FEAT-RCLA/F-RCLA-R007
- should propagate empty from delegate when delegate returns empty → FEAT-RCLA/F-RCLA-R007

### DefaultRefinerEngine (refiner-domain)

- should include LEMMA_ABSENCE tasks targeting QUIZ when quiz has lemma-absence score below 1.0 → FEAT-RCLA/F-RCLA-R001
- should not include LEMMA_ABSENCE tasks targeting MILESTONE or COURSE in the refinement plan → FEAT-RCLA/F-RCLA-R001
- should not generate LEMMA_ABSENCE task for quiz with lemma-absence score equal to 1.0 → FEAT-RCLA/F-RCLA-R001
- should still generate COCA_BUCKETS and LEMMA_RECURRENCE tasks at MILESTONE and COURSE level after re-routing → FEAT-RCLA/F-RCLA-R001

### CourseToAuditableMapper (audit-application)

- Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse → FEAT-NLP/F-NLP-R010
- Given a course with no milestones, when map is called, then returns an AuditableCourse without error → FEAT-NLP/F-NLP-R010
- Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates → FEAT-NLP/F-NLP-R008

### DefaultAuditRunner (audit-application)

- Given a valid course path, when runAudit is called, then returns the audit report from the full chain → FEAT-CLI/F-CLI-R001/F-CLI-J001
- Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course → FEAT-CLI/F-CLI-R001
- Given courseRepository throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given contentAudit throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given a course with no milestones, when runAudit is called, then returns the report from contentAudit → FEAT-CLI/F-CLI-R001

### DefaultLemmaAbsenceConfig (audit-application)

- should have alert thresholds non-decreasing from high to low priority → FEAT-LABS/F-LABS-R014
- should enforce zero tolerance for high priority alert threshold → FEAT-LABS/F-LABS-R014
- should enforce A1 zero tolerance with both absolute and percentage thresholds at zero → FEAT-LABS/F-LABS-R021
- should have discount per level that limits max penalty to 0.3 for three-level distance → FEAT-LABS/F-LABS-R018
- should return non-negative values for all thresholds and bounds → FEAT-LABS/F-LABS-R021
- should return positive report limits for all priority levels → FEAT-LABS/F-LABS-R026
- should return percentage thresholds between 0 and 100 for all levels → FEAT-LABS/F-LABS-R021
- should return positive level weights for all CEFR levels → FEAT-LABS/F-LABS-R024
- should return discount per level between 0 exclusive and 1 exclusive → FEAT-LABS/F-LABS-R018
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
- should return coverage target 0.95 for A1 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.85 for A2 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.70 for B1 → FEAT-LABS/F-LABS-R032
- should return coverage target 0.55 for B2 → FEAT-LABS/F-LABS-R032
- should have coverage targets decreasing from A1 to B2 → FEAT-LABS/F-LABS-R032
- should return coverage targets between 0 and 1 for all levels → FEAT-LABS/F-LABS-R032

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

### FileSystemAuditReportStore (audit-infrastructure)

- should save an AuditReport and load it back with identical content

### FileSystemRevisionArtifactStore (audit-infrastructure)

- Given a RevisionArtifact, when save is called, then a file is written under .content-audit/revisions/ → FEAT-REVBYP/F-REVBYP-R008
- Given an artifact for plan P1 and proposal PR1, when save is called, then the file path is .content-audit/revisions/P1/PR1.<ext> → FEAT-REVBYP/F-REVBYP-R009
- Given a persisted artifact, when load is called, then all RevisionProposal fields plus verdict and rejectionReason are recoverable → FEAT-REVBYP/F-REVBYP-R010

