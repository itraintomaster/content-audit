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

### IAuditEngine (audit-domain)

- Given a course with milestones, topics and knowledges, when AuditEngine.runAudit is invoked with both KTLEN analyzers, then the report root contains aggregated knowledge-title-length and knowledge-instructions-length scores at every hierarchy level → FEAT-KTLEN/F-KTLEN-J001
- Given a course with knowledges whose title weighted length exceeds 28, when AuditEngine.runAudit is invoked, then the affected knowledge nodes carry knowledge-title-length scores below 1.0 enabling identification of overlong titles → FEAT-KTLEN/F-KTLEN-J002
- Given a course with mixed title and instruction lengths, when AuditEngine.runAudit is invoked, then per-level scores for knowledge-title-length and knowledge-instructions-length are reported independently per AuditNode allowing the user to compare both dimensions → FEAT-KTLEN/F-KTLEN-J004
- Given two AuditableCourses with identical content but milestones declared in different orders, when AuditEngine.runAudit is invoked on each with the LemmaRecurrenceAnalyzer registered, then both AuditReports produce the same lemma-recurrence score confirming a deterministic CEFR-ordered traversal independent of input declaration order → FEAT-LREC/F-LREC-R002
- should aggregate quiz-level sentence-length scores into each KNOWLEDGE node as the simple average of the scoring quizzes under it omitting from the average any quiz excluded by F-SLEN-R001 and leaving the knowledge score unavailable when no quiz under it has a score → FEAT-SLEN/F-SLEN-R003
- should aggregate knowledge-level sentence-length scores into each TOPIC node as the simple average of the scoring knowledges under it omitting from the average any knowledge without a score and leaving the topic score unavailable when no knowledge under it has a score → FEAT-SLEN/F-SLEN-R004
- should aggregate topic-level sentence-length scores into each MILESTONE node as the simple average of the scoring topics under it omitting from the average any topic without a score and leaving the level score unavailable when no topic under it has a score → FEAT-SLEN/F-SLEN-R005
- should aggregate milestone-level sentence-length scores into the COURSE root node as the simple average of the scoring milestones omitting from the average any milestone without a score and returning zero as the course overall score when no milestone has a score → FEAT-SLEN/F-SLEN-R008
- should publish the sentence-length score on every AuditNode of the hierarchy (quiz knowledge topic milestone course) so consumers can read it at any level via node.getScores().get('sentence-length') → FEAT-SLEN/F-SLEN-R016/F-SLEN-J004

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
- should score 1.0 for instructions exactly at soft limit of 70 chars → FEAT-KTLEN/F-KTLEN-R006
- should score 1.0 for instructions of 30 chars within soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 71 chars just above soft limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions exactly at hard limit of 100 chars → FEAT-KTLEN/F-KTLEN-R006
- should score 0.5 for instructions of 85 chars between soft and hard limits → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 101 chars just above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should score 0.0 for instructions of 200 chars well above hard limit → FEAT-KTLEN/F-KTLEN-R006
- should complete without error when onQuiz is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onMilestone is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onTopic is called → FEAT-KTLEN/F-KTLEN-R008
- should complete without error when onCourseComplete is called → FEAT-KTLEN/F-KTLEN-R008
- should produce correct scores for three knowledges with different instruction lengths → FEAT-KTLEN/F-KTLEN-R006
- should use weighted character length not plain string length for scoring instructions → FEAT-KTLEN/F-KTLEN-R002/F-KTLEN-J003
- should distinguish three scoring ranges 1.0 at-or-below-70 0.5 above-70-up-to-100 0.0 above-100 at the declared weighted-char thresholds → FEAT-KTLEN/F-KTLEN-R005

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
- should emit a SentenceLengthDiagnosis on the quiz node populated with tokenCount, targetMin, targetMax, cefrLevel, delta and toleranceMargin matching the analyzer computation → FEAT-DSLEN/F-DSLEN-R001
- should NOT emit a SentenceLengthDiagnosis on a quiz node that is excluded as non-sentence (no scoring produced) → FEAT-DSLEN/F-DSLEN-R002
- should NOT emit a SentenceLengthDiagnosis on knowledge topic milestone or course nodes traversed by the analyzer → FEAT-DSLEN/F-DSLEN-R003
- should make the emitted SentenceLengthDiagnosis retrievable via QuizDiagnoses getSentenceLengthDiagnosis on the same quiz node → FEAT-DSLEN/F-DSLEN-R004
- should compute each quiz score using the linguistic token count from the precomputed NLP tokenization of the quiz sentence and never from a whitespace-based string split → FEAT-SLEN/F-SLEN-R013
- should compute the length score over the mode-determined canonical phrase token count and not over a blind concatenation of all quiz parts → FEAT-SMODE/F-SMODE-R005
- should score a REWRITE quiz answer Watch the DVD at 100 percent on 4 tokens for A1 instead of 60 percent on the 10-token source-plus-answer concatenation → FEAT-SMODE/F-SMODE-R007

### IScoreAggregator (audit-domain)

- should average the quiz instruction score of a knowledge over its evaluated quizzes only, leaving the pending ones out → FEAT-QINST/F-QINST-R004

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
- should populate quizSentence on the LEMMA_ABSENCE correction context from the AuditableQuiz carrier → FEAT-RCLAQS/F-RCLAQS-R001
- should copy AuditableQuiz.quizSentence verbatim without recomputing the DSL in the resolver → FEAT-RCLAQS/F-RCLAQS-R002
- should emit sentence and quizSentence that originate from the same AuditableQuiz so both fields describe the same quiz → FEAT-RCLAQS/F-RCLAQS-R003
- should populate tokenCount on the correction context from SentenceLengthDiagnosis on the quiz node → FEAT-RCLALEN/F-RCLALEN-R001
- should populate targetMin and targetMax on the correction context from SentenceLengthDiagnosis on the quiz node → FEAT-RCLALEN/F-RCLALEN-R001
- should populate delta on the correction context from SentenceLengthDiagnosis on the quiz node → FEAT-RCLALEN/F-RCLALEN-R001
- should populate lengthDirection on the correction context as a non-null enum value derived by the resolver → FEAT-RCLALEN/F-RCLALEN-R001
- should set lengthDirection to SHORTEN when delta is greater than zero → FEAT-RCLALEN/F-RCLALEN-R002
- should set lengthDirection to LENGTHEN when delta is less than zero → FEAT-RCLALEN/F-RCLALEN-R002
- should set lengthDirection to KEEP_SAME when delta is exactly zero → FEAT-RCLALEN/F-RCLALEN-R002
- should set lengthDirection to UNKNOWN when SentenceLengthDiagnosis is not available on the quiz node → FEAT-RCLALEN/F-RCLALEN-R002
- should read SentenceLengthDiagnosis from the same quiz node already used to obtain LemmaPlacementDiagnosis → FEAT-RCLALEN/F-RCLALEN-R003
- should not introduce any new traversal of the audit tree to read SentenceLengthDiagnosis → FEAT-RCLALEN/F-RCLALEN-R003
- should still produce a correction context when LemmaPlacementDiagnosis is present but SentenceLengthDiagnosis is absent on the quiz node → FEAT-RCLALEN/F-RCLALEN-R004
- should leave non-length fields of the correction context populated normally when SentenceLengthDiagnosis is absent → FEAT-RCLALEN/F-RCLALEN-R004
- should populate suggestedLemmas with lemmaCount lemmaCountThreshold and isUnderexposed fields when the audit report carries lemma-count diagnoses for the resolved level → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R002
- should include in suggestedLemmas a lemma flagged as underexposed by lemma-count even when that lemma has no LEMMA_ABSENCE diagnosis on the quiz node → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R002
- should evaluate APPEARS_TOO_EARLY underexposure against the lema's real target level not against the level where the lema currently appears → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R004
- should apply the existing suggestedLemmas size limit only after the lemma-count ranking has been computed so the cap never increases the maximum observable list size → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R006
- should still return suggestedLemmas for a LEMMA_ABSENCE task when the audit report has no lemma-count signal leaving lemmaCount lemmaCountThreshold and isUnderexposed marked as unavailable and not throwing → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R012
- should emit every suggestedLemmas element with the three lemma-count fields either all informed or all uninformed never in a mixed state → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R013
- should emit suggestedLemmas elements whose informed lemma-count triple is internally consistent with isUnderexposed equal to lemmaCount less than lemmaCountThreshold and no negative values → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R013
- should rank suggestedLemmas into six first-match-wins tiers ordering underexposed-in-deficient-band then absent-in-deficient-band then underexposed then absent then exposed-scarce-in-deficient-band then exposed-scarce → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R003
- should break ties within the same tier by ordering suggestedLemmas by ascending cocaRank as the final tiebreak → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R005
- should not use lemma-count exposure deficit magnitude as an intra-tier tiebreaker leaving COCA ascending as the only order within a tier → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R005
- should place a COMPLETELY_ABSENT lemma behind an already-introduced underexposed lemma only when both share the same deficient-band state so tier2 stays behind tier1 and tier4 stays behind tier3 → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R007
- should place an absent lemma that belongs to a deficient band ahead of an underexposed lemma that belongs to no deficient band so tier2 outranks tier3 → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R014
- should classify a lemma with exposure exactly equal to the threshold N as exposed-but-scarce in the expanded band tiers five or six and never as underexposed → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R014
- should treat deficient-band membership as a binary key so two lemmas in different deficient bands share the same tier and resolve between themselves by ascending cocaRank → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R014
- should always place exposed-but-scarce lemmas in tiers five and six after every macro-scarcity lemma in tiers one through four regardless of deficient-band membership → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R014
- should degrade to tier order three then four then six with COCA ascending as the final tiebreak and without failing when the audit did not analyze COCA frequency bands so the band tiers stay empty → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R015
- should include in scarceContentWords only the current sentence content words whose lemma-count is strictly below the threshold and exclude those whose count is greater than or equal to the threshold → FEAT-RCLA/F-RCLA-R003b
- should populate each scarceContentWords entry with lemma pos count and threshold where count is the global exposure and threshold is the shared lemma-count threshold → FEAT-RCLA/F-RCLA-R003b
- should return an empty scarceContentWords list when the audit did not include lemma-count so no exposure signal is available → FEAT-RCLA/F-RCLA-R003c
- should return an empty scarceContentWords list when no content word of the current sentence is below the lemma-count threshold or the sentence has no content words → FEAT-RCLA/F-RCLA-R003c
- should populate outOfCatalogWords on the lemma-absence correction context from the quiz diagnosis → FEAT-LABS/F-LABS-R038
- should map the applied discount into each MisplacedLemmaContext entry → FEAT-LABS/F-LABS-R038
- should leave outOfCatalogWords empty not null when the diagnosis has no out-of-catalog penalties → FEAT-LABS/F-LABS-R038

### DispatchingCorrectionContextResolver (refiner-domain)

- should delegate to sentenceLengthResolver when task diagnosis is SENTENCE_LENGTH → FEAT-RCLA/F-RCLA-R007
- should delegate to lemmaAbsenceResolver when task diagnosis is LEMMA_ABSENCE → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind COCA_BUCKETS → FEAT-RCLA/F-RCLA-R007
- should return empty for unsupported diagnosis kind LEMMA_RECURRENCE → FEAT-RCLA/F-RCLA-R007
- should propagate empty from delegate when delegate returns empty → FEAT-RCLA/F-RCLA-R007
- should report supports=true for diagnosisKinds with a registered resolver and supports=false for kinds without one → FEAT-REVCTX/F-REVCTX-R004
- Given a task whose diagnosis is KNOWLEDGE_TITLE_LENGTH, when resolve is called, then it delegates to the knowledgeTitleResolver and returns a populated context instead of empty → FEAT-KTLR/F-KTLR-R001

### DefaultRefinerEngine (refiner-domain)

- should include LEMMA_ABSENCE tasks targeting QUIZ when quiz has lemma-absence score below 1.0 → FEAT-RCLA/F-RCLA-R001
- should not include LEMMA_ABSENCE tasks targeting MILESTONE or COURSE in the refinement plan → FEAT-RCLA/F-RCLA-R001
- should not generate LEMMA_ABSENCE task for quiz with lemma-absence score equal to 1.0 → FEAT-RCLA/F-RCLA-R001
- should still generate COCA_BUCKETS and LEMMA_RECURRENCE tasks at MILESTONE and COURSE level after re-routing → FEAT-RCLA/F-RCLA-R001

### KnowledgeTitleContextResolver (refiner-domain)

- Given a KNOWLEDGE_TITLE_LENGTH task pointing at an existing knowledge, when resolve runs, then the context exposes the current title, current instructions and the containing topic label of that knowledge → FEAT-KTLR/F-KTLR-R002
- Given a KNOWLEDGE_TITLE_LENGTH task whose knowledge does not exist in the course, when resolve runs, then it fails explicitly without producing a context and without invoking the agent → FEAT-KTLR/F-KTLR-R011

### CourseToAuditableMapper (audit-application)

- Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse → FEAT-NLP/F-NLP-R010
- Given a course with no milestones, when map is called, then returns an AuditableCourse without error → FEAT-NLP/F-NLP-R010
- Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates → FEAT-NLP/F-NLP-R008
- should stamp quizSentence on AuditableQuiz by invoking QuizSentenceConverter.serialize on the same FormEntity used for sentences → FEAT-RCLAQS/F-RCLAQS-R002
- should invoke QuizSentenceConverter.serialize exactly once per quiz in the same pass that populates sentences → FEAT-RCLAQS/F-RCLAQS-R002
- should stamp sentences[0] and quizSentence from the same FormEntity so plain sentence and DSL describe the same derivation step → FEAT-RCLAQS/F-RCLAQS-R003
- should propagate QuizSentenceSerializationException and not emit the AuditableQuiz when the original FormEntity violates FEAT-QSENT invariants → FEAT-RCLAQS/F-RCLAQS-R004
- should fail atomically without a partial quizSentence when serialize throws for a TEXT form with non-empty options → FEAT-RCLAQS/F-RCLAQS-R004
- should fail atomically without a partial quizSentence when serialize throws for a CLOZE form with null or empty options → FEAT-RCLAQS/F-RCLAQS-R004
- Given two AuditableCourses produced by mapping a course that has knowledge quizzes containing sentences with enriched-token-requiring vocabulary (frequency-ranked words via SpaCy) when AuditEngine runs both audits then every AuditableQuiz produced by the mapper carries a List<NlpToken> populated by analyzeTokensBatch covering tokenization lemmatization POS tagging frequency rank stop-word and punctuation flags so downstream analyzers can read enriched tokens without re-tokenizing → FEAT-NLP/F-NLP-R010/F-NLP-J001
- should propagate QuizTemplateEntity sentences verbatim to AuditableQuiz sentences without modification → FEAT-DBSENT/F-DBSENT-R002
- should not invoke QuizSentenceConverter.toPlainSentences for any quiz of the audited course → FEAT-DBSENT/F-DBSENT-R002
- should stamp the canonical sentence at index 0 of QuizTemplateEntity sentences as the NLP batch key for the AuditableQuiz tokens → FEAT-DBSENT/F-DBSENT-R002
- should carry every sentence part and every accepted option of the quiz template into the auditable quiz → FEAT-QINST/F-QINST-R009
- should carry the knowledge instructions and the topic name into the auditable knowledge → FEAT-QINST/F-QINST-R009

### DefaultAuditRunner (audit-application)

- Given a valid course path, when runAudit is called, then returns the audit report from the full chain → FEAT-CLI/F-CLI-R001/F-CLI-J001
- Given a valid course path, when runAudit is called, then courseRepository load is invoked with the path → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then courseToAuditableMapper map is invoked with the loaded entity → FEAT-CLI/F-CLI-R001
- Given a valid course path, when runAudit is called, then contentAudit audit is invoked with the mapped auditable course → FEAT-CLI/F-CLI-R001
- Given courseRepository throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given courseToAuditableMapper throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given contentAudit throws an exception, when runAudit is called, then the exception propagates → FEAT-CLI/F-CLI-R001
- Given a course with no milestones, when runAudit is called, then returns the report from contentAudit → FEAT-CLI/F-CLI-R001
- should expose a public runAudit(Path coursePath) method on the AuditRunner contract that returns the AuditReport produced after loading the course mapping it to AuditableCourse and running the audit engine → FEAT-CLI/F-CLI-R005
- should expose lemma-count diagnoses in the AuditReport when runAudit is invoked with both lemma-count and lemma-absence analyzers so downstream consumers can read the underexposure signal per level → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R001
- should expose lemma-count diagnoses in the AuditReport when runDetailedAudit is invoked with both lemma-count and lemma-absence analyzers so downstream consumers can read the underexposure signal per level → FEAT-LEMMA-SUGGESTIONS/F-SLEM-R001
- should include the quiz instruction analysis when the audit request says nothing about analyzers → FEAT-QINST/F-QINST-R001
- should report quiz instruction score, diagnoses and coverage on an audit asked with no options → FEAT-QINST/F-QINST-R001
- should not build the quiz instruction analyzer when the request excludes it → FEAT-QINST/F-QINST-R011
- should leave the report without quiz instruction score, diagnosis or coverage when the analysis is excluded → FEAT-QINST/F-QINST-R011
- should not run the quiz instruction analysis when the request narrows the audit to other analyzers → FEAT-QINST/F-QINST-R011
- should finish the audit and keep the results of the other analyzers when the quiz instruction judge fails → FEAT-QINST/F-QINST-R007
- should hand the quiz instruction analyzer the run policy the request declared for its judge → FEAT-QINST/F-QINST-R006

### DefaultCocaBucketsConfig (audit-application)

- should expose the configured frequency bands as a list ordered by ascending limit each band carrying a name and a positive integer limit with at most the last band carrying the open flag → FEAT-COCA/F-COCA-R003
- should expose optimalRange and adequateRange as positive global tolerance values with adequateRange strictly greater than optimalRange so the OPTIMO/ADEQUATE bands are well-defined → FEAT-COCA/F-COCA-R009
- should expose a strategy field configured to either LEVELS or QUARTERS and apply the same chosen strategy to every CEFR level of the course → FEAT-COCA/F-COCA-R014
- should expose for each CEFR level under the QUARTERS strategy the Q1 and Q4 target objectives where each target objective lists the bands and for every band the target percentage and the directionality kind (atLeast or atMost) → FEAT-COCA/F-COCA-R018
- should expose for every band an expected progression direction (e.g. DESCENDENTE for top1k ASCENDENTE for top4k) and leave bands without configured expected progression unevaluated for progression → FEAT-COCA/F-COCA-R020/F-COCA-J003

### DefaultLemmaRecurrenceConfig (audit-application)

- should expose top subExposed and overExposed values that satisfy 0 less than overExposed strictly less than subExposed and top greater than zero → FEAT-LREC/F-LREC-R014

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
- should return out-of-catalog no-penalty rank bounds 1000 2000 3500 5000 for A1 A2 B1 B2 → FEAT-LABS/F-LABS-R034
- should return out-of-catalog mild-discount rank bounds 3000 5000 8000 10000 for A1 A2 B1 B2 → FEAT-LABS/F-LABS-R034
- should return out-of-catalog mild discount 0.1 and strong discount 0.3 → FEAT-LABS/F-LABS-R034
- should have out-of-catalog rank bounds non-decreasing from A1 to B2 within each band → FEAT-LABS/F-LABS-R034

### DefaultLemmaCountConfigLoader (audit-application)

- should return a config with threshold 4 when raw threshold is null → FEAT-LCOUNT/F-LCOUNT-R007
- should reject zero as threshold value with IllegalArgumentException → FEAT-LCOUNT/F-LCOUNT-R008
- should reject a negative integer as threshold value with IllegalArgumentException → FEAT-LCOUNT/F-LCOUNT-R008
- should reject a decimal number as threshold value with IllegalArgumentException → FEAT-LCOUNT/F-LCOUNT-R008
- should reject a non-numeric string as threshold value with IllegalArgumentException → FEAT-LCOUNT/F-LCOUNT-R008
- should reject an empty string as threshold value with IllegalArgumentException → FEAT-LCOUNT/F-LCOUNT-R008

### DefaultQuizInstructionConfig (audit-application)

- should default the run budget to 500 new judge queries → FEAT-QINST/F-QINST-R006
- should map the severities none, minor, major and critical to the scores 1.0, 0.6, 0.3 and 0.0 → FEAT-QINST/F-QINST-R002

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
- should populate QuizTemplateEntity sentences literally from the quiz JSON without deriving from sentenceParts → FEAT-DBSENT/F-DBSENT-R001
- should preserve the order of every entry from the JSON sentences list when loading a quiz with multiple variants → FEAT-DBSENT/F-DBSENT-R001
- should load sentences verbatim for a transformation-pattern quiz even when sentenceParts would yield a different concatenation → FEAT-DBSENT/F-DBSENT-R001
- should preserve each quiz plain sentences identically after a load save load round-trip → FEAT-DBSENT/F-DBSENT-R004
- should preserve the plain sentences of every quiz in a multi-quiz course after a whole-course save without dropping or cross-contaminating any list → FEAT-DBSENT/F-DBSENT-R004

### FileSystemAuditReportStore (audit-infrastructure)

- should save an AuditReport and load it back with identical content

### FileSystemRevisionArtifactStore (audit-infrastructure)

- Given a RevisionArtifact, when save is called, then a file is written under .content-audit/revisions/ → FEAT-REVBYP/F-REVBYP-R008
- Given an artifact for plan P1 and proposal PR1, when save is called, then the file path is .content-audit/revisions/P1/PR1.<ext> → FEAT-REVBYP/F-REVBYP-R009
- Given a persisted artifact, when load is called, then all RevisionProposal fields plus verdict and rejectionReason are recoverable → FEAT-REVBYP/F-REVBYP-R010
- Given artifacts saved under multiple plan directories, when findByProposalId(id, Optional.empty()) is called, then it scans subdirectories and returns the matching artifact → FEAT-REVAPR/F-REVAPR-R002
- Given no artifact matches the id, when findByProposalId(id, Optional.empty()) is called, then it returns Optional.empty → FEAT-REVAPR/F-REVAPR-R002
- Given an artifact saved under plan P1, when findByProposalId(id, Optional.of(P1)) is called, then it takes the direct path and returns the artifact → FEAT-REVAPR/F-REVAPR-R002
- Given no artifact under plan P1 with that id, when findByProposalId(id, Optional.of(P1)) is called, then it returns Optional.empty → FEAT-REVAPR/F-REVAPR-R002
- Given a PENDING_APPROVAL artifact whose taskId matches, when hasPendingProposalForTask is called, then it returns true → FEAT-REVAPR/F-REVAPR-R009
- Given only an APPROVED artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given only a REJECTED artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given no artifact exists for the task, when hasPendingProposalForTask is called, then it returns false → FEAT-REVAPR/F-REVAPR-R009
- Given artifacts saved under multiple plan directories, when list() is called, then it returns all of them across all plans → FEAT-REVAPR/F-REVAPR-R002
- Given the revisions directory is empty or missing, when list() is called, then it returns an empty list → FEAT-REVAPR/F-REVAPR-R002
- Given the store is constructed with a non-default baseDir, when save is called, then the artifact file lands under <baseDir>/.content-audit/revisions/<planId>/<proposalId>.* and NOT under System.getProperty('user.dir') → FEAT-REVAPR/F-REVAPR-R017

### FileSystemImpactPreviewStore (audit-infrastructure)

- Dado un ImpactPreview ya persistido bajo un proposalId, cuando save se invoca por segunda vez con otro ImpactPreview con el mismo proposalId, entonces findByProposalId retorna el preview persistido originalmente y no el nuevo → FEAT-PIPRE/F-PIPRE-R008
- Dado un ImpactPreview persistido en disco, cuando findByProposalId se invoca con su proposalId, entonces retorna el ImpactPreview con todos sus levelImpacts, availability y unavailability intactos → FEAT-PIPRE/F-PIPRE-R008

### FileSystemActiveAnalysisSelectionStore (audit-infrastructure)

- Dado un ActiveAnalysisSelection escrito previamente con write, cuando read corre, entonces retorna un Optional con esa misma seleccion (auditId y planId) → FEAT-CDIFF/F-CDIFF-R001
- Dado un store sin seleccion previa (archivo ausente), cuando read corre, entonces retorna Optional.empty → FEAT-CDIFF/F-CDIFF-R001
- Dado un store con una seleccion ya escrita, cuando write se invoca con la misma seleccion, entonces el archivo no se reescribe (write idempotente y no destructivo a nivel observable) → FEAT-CDIFF/F-CDIFF-R002
- Dado un store con una seleccion escrita, cuando clear corre, entonces read posterior retorna Optional.empty → FEAT-CDIFF/F-CDIFF-R001
- Dado un courseRoot con AuditReports y RefinementPlans persistidos, cuando write y clear se invocan sobre el ActiveAnalysisSelectionStore, entonces ningun AuditReport, RefinementPlan, RevisionArtifact ni archivo del curso se modifica → FEAT-CDIFF/F-CDIFF-R002

### FileSystemEvaluationLedger (evaluation-ledger-infrastructure)

- should make every appended result readable by a ledger instance opened after the run was interrupted → FEAT-EVCOST/F-EVCOST-R004
- should ignore a truncated trailing entry and report its content as never registered → FEAT-EVCOST/F-EVCOST-R004
- should preserve every result when two runs append to the same evaluator ledger at the same time → FEAT-EVCOST/F-EVCOST-R004
- should keep the results of previous evaluator versions readable in history after a new version records its own → FEAT-EVCOST/F-EVCOST-R006
- should keep every registered result readable after the analysis reports under the same base directory are deleted → FEAT-EVCOST/F-EVCOST-R007
- should keep the earlier entry consultable in history when the same key is recorded again → FEAT-EVCOST/F-EVCOST-R008

