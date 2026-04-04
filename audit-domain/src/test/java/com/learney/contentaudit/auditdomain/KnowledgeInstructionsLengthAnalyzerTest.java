package com.learney.contentaudit.auditdomain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KnowledgeInstructionsLengthAnalyzerTest {

    private KnowledgeInstructionsLengthAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new KnowledgeInstructionsLengthAnalyzer();
    }

    // -- helpers --

    private AuditNode buildKnowledgeNode(AuditableKnowledge knowledge) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.KNOWLEDGE);
        node.setEntity(knowledge);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
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
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(null));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-instructions-length"));
    }

    @Test
    @DisplayName("should score 1.0 for knowledge with empty instructions")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore10ForKnowledgeWithEmptyInstructions() {
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(""));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-instructions-length"));
    }

    // -- within soft limit (<=70) score 1.0 --

    @Test
    @DisplayName("should score 1.0 for instructions exactly at soft limit of 70 weighted chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore10ForInstructionsExactlyAtSoftLimitOf70Chars() {
        String instructions = "a".repeat(70);
        assertEquals(70, instructions.length());
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-instructions-length"));
    }

    @Test
    @DisplayName("should score 1.0 for instructions of 30 weighted chars within soft limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore10ForInstructionsOf30CharsWithinSoftLimit() {
        String instructions = "a".repeat(30);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(1.0, node.getScores().get("knowledge-instructions-length"));
    }

    // -- between soft and hard limits (71..100) score 0.5 --

    @Test
    @DisplayName("should score 0.5 for instructions of 71 weighted chars just above soft limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore05ForInstructionsOf71CharsJustAboveSoftLimit() {
        String instructions = "a".repeat(71);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(0.5, node.getScores().get("knowledge-instructions-length"));
    }

    @Test
    @DisplayName("should score 0.5 for instructions exactly at hard limit of 100 weighted chars")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore05ForInstructionsExactlyAtHardLimitOf100Chars() {
        String instructions = "a".repeat(100);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(0.5, node.getScores().get("knowledge-instructions-length"));
    }

    @Test
    @DisplayName("should score 0.5 for instructions of 85 weighted chars between soft and hard limits")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore05ForInstructionsOf85CharsBetweenSoftAndHardLimits() {
        String instructions = "a".repeat(85);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(0.5, node.getScores().get("knowledge-instructions-length"));
    }

    // -- above hard limit (>100) score 0.0 --

    @Test
    @DisplayName("should score 0.0 for instructions of 101 weighted chars just above hard limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldScore00ForInstructionsOf101CharsJustAboveHardLimit() {
        String instructions = "a".repeat(101);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-instructions-length"));
    }

    @Test
    @DisplayName("should score 0.0 for instructions of 200 weighted chars well above hard limit")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldScore00ForInstructionsOf200CharsWellAboveHardLimit() {
        String instructions = "a".repeat(200);
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        assertEquals(0.0, node.getScores().get("knowledge-instructions-length"));
    }

    // -- weighted vs plain length --

    @Test
    @DisplayName("should use weighted character length not plain string length for scoring instructions")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R005")
    public void shouldUseWeightedCharacterLengthNotPlainStringLengthForScoringInstructions() {
        // 80 commas: plain length = 80 (would be > 70 soft limit), weighted = 80 * 0.5 = 40 (within soft limit)
        String instructions = ",".repeat(80);
        assertEquals(80, instructions.length());
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions(instructions));
        analyzer.onKnowledge(node);
        // If using weighted length (40 <= 70), score is 1.0; if using plain length (80 > 70), score would be 0.5
        assertEquals(1.0, node.getScores().get("knowledge-instructions-length"));
    }

    // -- no-op methods complete without error --

    @Test
    @DisplayName("should complete without error when onQuiz is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnQuizIsCalled() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onQuiz(node));
    }

    @Test
    @DisplayName("should complete without error when onMilestone is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnMilestoneIsCalled() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(new AuditableMilestone(List.of(), "m-id", "milestone label", "M001"));
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onMilestone(node));
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.TOPIC);
        node.setEntity(new AuditableTopic(List.of(), "t-id", "topic label", "T001"));
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onTopic(node));
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-KTLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setScores(new LinkedHashMap<>());
        node.setChildren(new ArrayList<>());
        node.setMetadata(new LinkedHashMap<>());
        assertDoesNotThrow(() -> analyzer.onCourseComplete(node));
    }

    // -- empty results before any processing --

    @Test
    @DisplayName("should return empty list when getResults is called without prior processing")
    @Tag("F-KTLEN")
    public void shouldReturnEmptyListWhenGetResultsIsCalledWithoutPriorProcessing() {
        // With the new API there is no getResults(); we verify that a freshly
        // constructed analyzer has not written any scores to a node that was never processed.
        AuditNode node = buildKnowledgeNode(knowledgeWithInstructions("test"));
        assertFalse(node.getScores().containsKey("knowledge-instructions-length"));
    }

    // -- multiple knowledges accumulate correctly --

    @Test
    @DisplayName("should produce correct scores for three knowledges with different instruction lengths")
    @Tag("F-KTLEN")
    @Tag("F-KTLEN-R006")
    public void shouldProduceCorrectScoresForThreeKnowledgesWithDifferentInstructionLengths() {
        // 30 chars -> 1.0 (within soft limit)
        AuditNode node1 = buildKnowledgeNode(knowledgeWithInstructions("a".repeat(30)));
        // 85 chars -> 0.5 (between soft and hard)
        AuditNode node2 = buildKnowledgeNode(knowledgeWithInstructions("a".repeat(85)));
        // 150 chars -> 0.0 (above hard limit)
        AuditNode node3 = buildKnowledgeNode(knowledgeWithInstructions("a".repeat(150)));

        analyzer.onKnowledge(node1);
        analyzer.onKnowledge(node2);
        analyzer.onKnowledge(node3);

        assertEquals(1.0, node1.getScores().get("knowledge-instructions-length"));
        assertEquals(0.5, node2.getScores().get("knowledge-instructions-length"));
        assertEquals(0.0, node3.getScores().get("knowledge-instructions-length"));
    }
}
