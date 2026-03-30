package com.learney.contentaudit.auditdomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KnowledgeInstructionsLengthAnalyzerTest {

    private KnowledgeInstructionsLengthAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new KnowledgeInstructionsLengthAnalyzer();
    }

    // -- helpers --

    private AuditContext ctx(String milestoneId, String topicId, String knowledgeId) {
        return new AuditContext(milestoneId, topicId, knowledgeId, null, null, null, null);
    }

    private AuditableKnowledge knowledgeWithInstructions(String instructions) {
        return new AuditableKnowledge(List.of(), "Some Title", instructions, false,
                "k-id", "knowledge label", "K001");
    }

    // -- name / target --

    @Test
    @DisplayName("should return knowledge-instructions-length as analyzer name")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R008")
    public void shouldReturnKnowledgeinstructionslengthAsAnalyzerName() {
        assertEquals("knowledge-instructions-length", analyzer.getName());
    }

    @Test
    @DisplayName("should return KNOWLEDGE as audit target")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R008")
    public void shouldReturnKNOWLEDGEAsAuditTarget() {
        assertEquals(AuditTarget.KNOWLEDGE, analyzer.getTarget());
    }

    // -- null / empty instructions score 1.0 --

    @Test
    @DisplayName("should score 1.0 for knowledge with null instructions")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore10ForKnowledgeWithNullInstructions() {
        AuditableKnowledge knowledge = knowledgeWithInstructions(null);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore());
    }

    @Test
    @DisplayName("should score 1.0 for knowledge with empty instructions")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore10ForKnowledgeWithEmptyInstructions() {
        AuditableKnowledge knowledge = knowledgeWithInstructions("");
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore());
    }

    // -- within soft limit (<=70) score 1.0 --

    @Test
    @DisplayName("should score 1.0 for instructions exactly at soft limit of 70 chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore10ForInstructionsExactlyAtSoftLimitOf70Chars() {
        String instructions = "a".repeat(70);
        assertEquals(70, instructions.length());

        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore());
    }

    @Test
    @DisplayName("should score 1.0 for instructions of 30 chars within soft limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore10ForInstructionsOf30CharsWithinSoftLimit() {
        String instructions = "a".repeat(30);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore());
    }

    // -- between soft and hard limits (71..100) score 0.5 --

    @Test
    @DisplayName("should score 0.5 for instructions of 71 chars just above soft limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore05ForInstructionsOf71CharsJustAboveSoftLimit() {
        String instructions = "a".repeat(71);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.5, results.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.5 for instructions exactly at hard limit of 100 chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore05ForInstructionsExactlyAtHardLimitOf100Chars() {
        String instructions = "a".repeat(100);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.5, results.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.5 for instructions of 85 chars between soft and hard limits")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore05ForInstructionsOf85CharsBetweenSoftAndHardLimits() {
        String instructions = "a".repeat(85);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.5, results.get(0).getScore());
    }

    // -- above hard limit (>100) score 0.0 --

    @Test
    @DisplayName("should score 0.0 for instructions of 101 chars just above hard limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore00ForInstructionsOf101CharsJustAboveHardLimit() {
        String instructions = "a".repeat(101);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.0 for instructions of 200 chars well above hard limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore00ForInstructionsOf200CharsWellAboveHardLimit() {
        String instructions = "a".repeat(200);
        AuditableKnowledge knowledge = knowledgeWithInstructions(instructions);
        analyzer.onKnowledge(knowledge, ctx("m1", "t1", "k1"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore());
    }

    // -- no-op methods complete without error --

    @Test
    @DisplayName("should complete without error when onQuiz is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnQuizIsCalled() {
        AuditableQuiz quiz = new AuditableQuiz("sentence", List.of(), "q-id", "quiz label", "Q001");
        AuditContext ctx = ctx("m1", "t1", "k1");
        assertDoesNotThrow(() -> analyzer.onQuiz(quiz, ctx));
    }

    @Test
    @DisplayName("should complete without error when onMilestone is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnMilestoneIsCalled() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "m-id", "milestone label", "M001");
        AuditContext ctx = ctx("m1", null, null);
        assertDoesNotThrow(() -> analyzer.onMilestone(milestone, ctx));
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        AuditableTopic topic = new AuditableTopic(List.of(), "t-id", "topic label", "T001");
        AuditContext ctx = ctx("m1", "t1", null);
        assertDoesNotThrow(() -> analyzer.onTopic(topic, ctx));
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        AuditableCourse course = new AuditableCourse(List.of());
        AuditContext ctx = ctx(null, null, null);
        assertDoesNotThrow(() -> analyzer.onCourseComplete(course, ctx));
    }

    // -- empty results before any processing --

    @Test
    @DisplayName("should return empty list when getResults is called without prior processing")
    @Tag("F-KTLEN")
    public void shouldReturnEmptyListWhenGetResultsIsCalledWithoutPriorProcessing() {
        assertTrue(analyzer.getResults().isEmpty());
    }

    // -- multiple knowledges accumulate correctly --

    @Test
    @DisplayName("should produce correct scores for three knowledges with different instruction lengths")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldProduceCorrectScoresForThreeKnowledgesWithDifferentInstructionLengths() {
        // 30 chars -> 1.0 (within soft limit)
        AuditableKnowledge k1 = knowledgeWithInstructions("a".repeat(30));
        // 85 chars -> 0.5 (between soft and hard)
        AuditableKnowledge k2 = knowledgeWithInstructions("a".repeat(85));
        // 150 chars -> 0.0 (above hard limit)
        AuditableKnowledge k3 = knowledgeWithInstructions("a".repeat(150));

        analyzer.onKnowledge(k1, ctx("m1", "t1", "ka"));
        analyzer.onKnowledge(k2, ctx("m1", "t1", "kb"));
        analyzer.onKnowledge(k3, ctx("m1", "t1", "kc"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(3, results.size());
        assertEquals(1.0, results.get(0).getScore());
        assertEquals(0.5, results.get(1).getScore());
        assertEquals(0.0, results.get(2).getScore());
    }
}
