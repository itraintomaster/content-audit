package com.learney.contentaudit.auditdomain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KnowledgeTitleLengthAnalyzerTest {

    // ---------------------------------------------------------------------------
    // Helper factories
    // ---------------------------------------------------------------------------

    private static AuditContext ctx(String milestoneId, String topicId, String knowledgeId) {
        return new AuditContext(milestoneId, topicId, knowledgeId, null, null, null, null);
    }

    private static AuditableKnowledge knowledge(String title) {
        return new AuditableKnowledge(List.of(), title, null, false, "k1", "label", "CODE");
    }

    /** Builds a title string composed entirely of 'a' characters (weight 1.0 each). */
    private static String titleOfWeight(int targetWeight) {
        return "a".repeat(targetWeight);
    }

    // ---------------------------------------------------------------------------
    // getName / getTarget
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should return knowledge-title-length as analyzer name")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R008")
    public void shouldReturnKnowledgetitlelengthAsAnalyzerName() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        assertEquals("knowledge-title-length", analyzer.getName());
    }

    @Test
    @DisplayName("should return KNOWLEDGE as audit target")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R008")
    public void shouldReturnKNOWLEDGEAsAuditTarget() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        assertEquals(AuditTarget.KNOWLEDGE, analyzer.getTarget());
    }

    // ---------------------------------------------------------------------------
    // null / empty title → score 0.0
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 0.0 for knowledge with null title")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForKnowledgeWithNullTitle() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = knowledge(null);
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for knowledge with empty title")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForKnowledgeWithEmptyTitle() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = knowledge("");
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore(), 0.001);
    }

    // ---------------------------------------------------------------------------
    // Titles within soft limit (weighted length <= 28) → score 1.0
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 1.0 for knowledge with title within limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore10ForKnowledgeWithTitleWithinLimit() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "hello" has 5 default-weight chars → weighted length 5, well within 28
        AuditableKnowledge k = knowledge("hello");
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for knowledge with title at exactly 28 weighted chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R001")
    public void shouldScore10ForKnowledgeWithTitleAtExactly28WeightedChars() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 28 'a' chars each with weight 1.0 → weighted length exactly 28
        AuditableKnowledge k = knowledge(titleOfWeight(28));
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for title fitting with weighted length 5.1")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForTitleFittingWithWeightedLength51() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "fitting": f=0.7 + i=0.5 + t=0.7 + t=0.7 + i=0.5 + n=1.0 + g=1.0 = 5.1
        AuditableKnowledge k = knowledge("fitting");
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for zero-weight special chars title")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForZeroweightSpecialCharsTitle() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "$$$***": each char has weight 0.0 → total weighted length = 0.0
        AuditableKnowledge k = knowledge("$$$***");
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for mixed-weight title with weighted length 2.7")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForMixedweightTitleWithWeightedLength27() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "$if,a": $=0.0, i=0.5, f=0.7, ,=0.5, a=1.0 → total = 2.7
        AuditableKnowledge k = knowledge("$if,a");
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
    }

    // ---------------------------------------------------------------------------
    // Titles in the degradation zone (28 < weighted length < 56) → partial score
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 0.75 for title of weighted length 35")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore075ForTitleOfWeightedLength35() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 35 'a' chars → weighted length 35; score = 1.0 - (35-28)/28 = 0.75
        AuditableKnowledge k = knowledge(titleOfWeight(35));
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.75, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 0.5 for title of weighted length 42")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore05ForTitleOfWeightedLength42() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 42 'a' chars → weighted length 42; score = 1.0 - (42-28)/28 = 0.5
        AuditableKnowledge k = knowledge(titleOfWeight(42));
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.5, results.get(0).getScore(), 0.001);
    }

    // ---------------------------------------------------------------------------
    // Titles at or beyond hard limit (weighted length >= 56) → score 0.0
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 0.0 for title of weighted length 56")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength56() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 56 'a' chars → weighted length 56; score = max(0, 1.0 - 28/28) = 0.0
        AuditableKnowledge k = knowledge(titleOfWeight(56));
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore(), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for title of weighted length 70")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength70() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 70 'a' chars → weighted length 70; score = max(0, 1.0 - 42/28) = 0.0
        AuditableKnowledge k = knowledge(titleOfWeight(70));
        analyzer.onKnowledge(k, ctx("m1", "t1", "k1"));
        List<ScoredItem> results = analyzer.getResults();
        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getScore(), 0.001);
    }

    // ---------------------------------------------------------------------------
    // No-op lifecycle methods complete without error
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should complete without error when onQuiz is called")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R008")
    public void shouldCompleteWithoutErrorWhenOnQuizIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableQuiz quiz = new AuditableQuiz("sentence", List.of(), "q1", "label", "CODE");
        assertDoesNotThrow(() -> analyzer.onQuiz(quiz, ctx("m1", "t1", "k1")));
        assertTrue(analyzer.getResults().isEmpty());
    }

    @Test
    @DisplayName("should complete without error when onMilestone is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnMilestoneIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "m1", "label", "CODE");
        assertDoesNotThrow(() -> analyzer.onMilestone(milestone, ctx("m1", null, null)));
        assertTrue(analyzer.getResults().isEmpty());
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableTopic topic = new AuditableTopic(List.of(), "t1", "label", "CODE");
        assertDoesNotThrow(() -> analyzer.onTopic(topic, ctx("m1", "t1", null)));
        assertTrue(analyzer.getResults().isEmpty());
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableCourse course = new AuditableCourse(List.of());
        assertDoesNotThrow(() -> analyzer.onCourseComplete(course, ctx(null, null, null)));
        assertTrue(analyzer.getResults().isEmpty());
    }

    // ---------------------------------------------------------------------------
    // Multiple results accumulate correctly
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should return two correctly scored items for two knowledges with different title lengths")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldReturnTwoCorrectlyScoredItemsForTwoKnowledgesWithDifferentTitleLengths() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();

        // First knowledge: short title within soft limit → score 1.0
        AuditableKnowledge k1 = new AuditableKnowledge(List.of(), "hello", null, false, "k1", "l1", "C1");
        analyzer.onKnowledge(k1, ctx("m1", "t1", "k1"));

        // Second knowledge: 35 chars → score 0.75
        AuditableKnowledge k2 = new AuditableKnowledge(List.of(), titleOfWeight(35), null, false, "k2", "l2", "C2");
        analyzer.onKnowledge(k2, ctx("m1", "t1", "k2"));

        List<ScoredItem> results = analyzer.getResults();
        assertEquals(2, results.size());
        assertEquals(1.0, results.get(0).getScore(), 0.001);
        assertEquals(0.75, results.get(1).getScore(), 0.001);
    }

    @Test
    @DisplayName("should return empty list when no knowledges have been processed")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldReturnEmptyListWhenNoKnowledgesHaveBeenProcessed() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        assertTrue(analyzer.getResults().isEmpty());
    }
}
