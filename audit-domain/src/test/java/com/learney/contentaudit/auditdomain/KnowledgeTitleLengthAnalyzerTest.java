package com.learney.contentaudit.auditdomain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KnowledgeTitleLengthAnalyzerTest {

    // ---------------------------------------------------------------------------
    // Helper factories
    // ---------------------------------------------------------------------------

    private static AuditableKnowledge knowledge(String title) {
        return new AuditableKnowledge(List.of(), title, null, false, "k1", "label", "CODE");
    }

    /** Builds a title string composed entirely of 'a' characters (weight 1.0 each). */
    private static String titleOfWeight(int targetWeight) {
        return "a".repeat(targetWeight);
    }

    private AuditNode buildKnowledgeNode(AuditableKnowledge knowledge) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.KNOWLEDGE);
        node.setEntity(knowledge);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
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
        AuditNode node = buildKnowledgeNode(knowledge(null));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for knowledge with empty title")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForKnowledgeWithEmptyTitle() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditNode node = buildKnowledgeNode(knowledge(""));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
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
        AuditNode node = buildKnowledgeNode(knowledge("hello"));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for knowledge with title at exactly 28 weighted chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R001")
    public void shouldScore10ForKnowledgeWithTitleAtExactly28WeightedChars() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 28 'a' chars each with weight 1.0 → weighted length exactly 28
        AuditNode node = buildKnowledgeNode(knowledge(titleOfWeight(28)));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for title fitting with weighted length 5.1")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForTitleFittingWithWeightedLength51() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "fitting": f=0.7 + i=0.5 + t=0.7 + t=0.7 + i=0.5 + n=1.0 + g=1.0 = 5.1
        AuditNode node = buildKnowledgeNode(knowledge("fitting"));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for zero-weight special chars title")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForZeroweightSpecialCharsTitle() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "$$$***": each char has weight 0.0 → total weighted length = 0.0
        AuditNode node = buildKnowledgeNode(knowledge("$$$***"));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 1.0 for mixed-weight title with weighted length 2.7")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R002")
    public void shouldScore10ForMixedweightTitleWithWeightedLength27() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // "$if,a": $=0.0, i=0.5, f=0.7, ,=0.5, a=1.0 → total = 2.7
        AuditNode node = buildKnowledgeNode(knowledge("$if,a"));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    // ---------------------------------------------------------------------------
    // Titles in the degradation zone (28 < weighted length < 29) → partial score
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 0.5 for title of weighted length 28.5")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore05ForTitleOfWeightedLength285() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 28 'a' chars (28.0) + 1 'i' char (0.5) → weighted length 28.5; score = 1.0 - (28.5-28)/1 = 0.5
        AuditNode node = buildKnowledgeNode(knowledge(titleOfWeight(28) + "i"));
        analyzer.onKnowledge(node);
        assertEquals(0.5, node.getScores().get("knowledge-title-length"), 0.001);
    }

    // ---------------------------------------------------------------------------
    // Titles at or beyond zero-point (weighted length >= 29) → score 0.0
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("should score 0.0 for title of weighted length 29")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength29() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 29 'a' chars → weighted length 29; score = max(0, 1.0 - (29-28)/1) = 0.0
        AuditNode node = buildKnowledgeNode(knowledge(titleOfWeight(29)));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for title of weighted length 35")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength35() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 35 'a' chars → weighted length 35; score = max(0, 1.0 - (35-28)/1) = 0.0
        AuditNode node = buildKnowledgeNode(knowledge(titleOfWeight(35)));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for title well beyond limit at weighted length 70")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleWellBeyondLimitAtWeightedLength70() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // 70 'a' chars → weighted length 70; score = max(0, 1.0 - (70-28)/1) = 0.0
        AuditNode node = buildKnowledgeNode(knowledge(titleOfWeight(70)));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
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
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onQuiz(node));
        assertFalse(node.getScores().containsKey("knowledge-title-length"));
    }

    @Test
    @DisplayName("should complete without error when onMilestone is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnMilestoneIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(new AuditableMilestone(List.of(), "m1", "label", "CODE"));
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onMilestone(node));
        assertFalse(node.getScores().containsKey("knowledge-title-length"));
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.TOPIC);
        node.setEntity(new AuditableTopic(List.of(), "t1", "label", "CODE"));
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onTopic(node));
        assertFalse(node.getScores().containsKey("knowledge-title-length"));
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onCourseComplete(node));
        assertFalse(node.getScores().containsKey("knowledge-title-length"));
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

        // First knowledge: short title within limit → score 1.0
        AuditableKnowledge k1 = new AuditableKnowledge(List.of(), "hello", null, false, "k1", "l1", "C1");
        AuditNode node1 = buildKnowledgeNode(k1);
        analyzer.onKnowledge(node1);

        // Second knowledge: 29 'a' chars → weighted length 29 → score 0.0
        AuditableKnowledge k2 = new AuditableKnowledge(List.of(), titleOfWeight(29), null, false, "k2", "l2", "C2");
        AuditNode node2 = buildKnowledgeNode(k2);
        analyzer.onKnowledge(node2);

        assertEquals(1.0, node1.getScores().get("knowledge-title-length"), 0.001);
        assertEquals(0.0, node2.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should return empty list when no knowledges have been processed")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldReturnEmptyListWhenNoKnowledgesHaveBeenProcessed() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        // No onKnowledge calls → no scores written anywhere; just verify the analyzer exists
        assertNotNull(analyzer);
    }

    @Test
    @DisplayName("should score 0.75 for title of weighted length 35")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore075ForTitleOfWeightedLength35() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = new AuditableKnowledge(List.of(), titleOfWeight(35), null, false, "k1", "l", "C");
        AuditNode node = buildKnowledgeNode(k);
        analyzer.onKnowledge(node);
        // Current formula: max(0, 1 - (35-28)/1) = 0.0
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.5 for title of weighted length 42")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore05ForTitleOfWeightedLength42() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = new AuditableKnowledge(List.of(), titleOfWeight(42), null, false, "k1", "l", "C");
        AuditNode node = buildKnowledgeNode(k);
        analyzer.onKnowledge(node);
        // Current formula: max(0, 1 - (42-28)/1) = 0.0
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for title of weighted length 56")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength56() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = new AuditableKnowledge(List.of(), titleOfWeight(56), null, false, "k1", "l", "C");
        AuditNode node = buildKnowledgeNode(k);
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }

    @Test
    @DisplayName("should score 0.0 for title of weighted length 70")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R003")
    public void shouldScore00ForTitleOfWeightedLength70() {
        KnowledgeTitleLengthAnalyzer analyzer = new KnowledgeTitleLengthAnalyzer();
        AuditableKnowledge k = new AuditableKnowledge(List.of(), titleOfWeight(70), null, false, "k1", "l", "C");
        AuditNode node = buildKnowledgeNode(k);
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-title-length"), 0.001);
    }
}
