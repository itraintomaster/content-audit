package com.learney.contentaudit.auditdomain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SentenceLengthAnalyzerTest {

    @Mock
    private SentenceLengthConfig config;

    @InjectMocks
    private SentenceLengthAnalyzer sut;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static NlpToken token() {
        return new NlpToken("w", "w", "NOUN", 1, false, false);
    }

    private static List<NlpToken> tokens(int count) {
        return Collections.nCopies(count, token());
    }

    private void setupA1Range() {
        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);
    }

    /**
     * Builds a minimal course-level root node (no entity needed).
     */
    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    /**
     * Builds a milestone node. The milestone label is used by SentenceLengthAnalyzer
     * to derive the CefrLevel via CefrLevel.valueOf(label).
     */
    private AuditNode buildMilestoneNode(AuditNode parent, AuditableMilestone milestone) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(milestone);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildTopicNode(AuditNode parent, AuditableTopic topic) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.TOPIC);
        node.setEntity(topic);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildKnowledgeNode(AuditNode parent, AuditableKnowledge knowledge) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.KNOWLEDGE);
        node.setEntity(knowledge);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildQuizNode(AuditNode parent, AuditableQuiz quiz) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        parent.getChildren().add(node);
        return node;
    }

    /**
     * Builds the full tree: course -> milestone(label) -> topic -> knowledge -> quiz
     * and returns the quiz node.
     */
    private AuditNode fullTree(String milestoneLabel, AuditableKnowledge knowledge, AuditableQuiz quiz) {
        AuditNode courseNode = buildCourseNode();
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, milestoneLabel, null);
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditableTopic topic = new AuditableTopic(List.of(), null, null, null);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        return buildQuizNode(knowledgeNode, quiz);
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should exclude quiz when milestoneId is null")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeQuizWhenMilestoneIdIsNull() {
        // milestone label null → CefrLevel.valueOf(null) → null → skip
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("Hello world test"), null);
        AuditNode quizNode = fullTree(null, knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should exclude quiz when milestoneId is non-numeric")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeQuizWhenMilestoneIdIsNonnumeric() {
        // milestone label "abc" is not a valid CefrLevel → skip
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("Hello world test"), null);
        AuditNode quizNode = fullTree("abc", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should exclude quiz when no target range configured for level")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R012")
    public void shouldExcludeQuizWhenNoTargetRangeConfiguredForLevel() {
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("She likes apples"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.empty());
        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should score only sentence quizzes when processing mixed knowledge types")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldScoreOnlySentenceQuizzesWhenProcessingMixedKnowledgeTypes() {
        AuditNode courseNode = buildCourseNode();
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, "A1", null);
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditableTopic topic = new AuditableTopic(List.of(), null, null, null);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);

        // Non-sentence knowledge
        AuditableKnowledge nonSentKnowledge = new AuditableKnowledge(List.of(), "Vocab", "Match", false, null, null, null);
        AuditNode nonSentKnowledgeNode = buildKnowledgeNode(topicNode, nonSentKnowledge);
        AuditableQuiz nonSentQuiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null);
        AuditNode nonSentQuizNode = buildQuizNode(nonSentKnowledgeNode, nonSentQuiz);

        // Sentence knowledge
        AuditableKnowledge sentKnowledge = new AuditableKnowledge(List.of(), "Sentences", "Complete", true, null, null, null);
        AuditNode sentKnowledgeNode = buildKnowledgeNode(topicNode, sentKnowledge);
        AuditableQuiz sentQuiz = new AuditableQuiz(tokens(6), null, null, null, null, List.of("She likes red apples very much"), null);
        AuditNode sentQuizNode = buildQuizNode(sentKnowledgeNode, sentQuiz);

        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);

        sut.onQuiz(nonSentQuizNode);
        sut.onQuiz(sentQuizNode);

        Assertions.assertFalse(nonSentQuizNode.getScores().containsKey("sentence-length"));
        Assertions.assertTrue(sentQuizNode.getScores().containsKey("sentence-length"));
        Assertions.assertEquals(1.0, sentQuizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should return sentence-length as analyzer name")
    @Tag("F-SLEN")
    public void shouldReturnSentencelengthAsAnalyzerName() {
        Assertions.assertEquals("sentence-length", sut.getName());
    }

    @Test
    @DisplayName("should return QUIZ as audit target")
    @Tag("F-SLEN")
    public void shouldReturnQUIZAsAuditTarget() {
        Assertions.assertEquals(AuditTarget.QUIZ, sut.getTarget());
    }

    @Test
    @DisplayName("should score 1.0 for quiz within A1 range")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore10ForQuizWithinA1Range() {
        // 6 tokens — within [5, 8]
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(6), "q1", null, null, null, List.of("She likes apples a lot today"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(1.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 0.75 for quiz 1 token above A1 max")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore075ForQuiz1TokenAboveA1Max() {
        // 9 tokens — 1 above max of 8; distance=1, margin=4 → 1 - 1/4 = 0.75
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(9), "q1", null, null, null, List.of("She really likes green apples a lot today quickly"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(0.75, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 0.25 for quiz 3 tokens below A1 min")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore025ForQuiz3TokensBelowA1Min() {
        // 2 tokens — 3 below min of 5; distance=3, margin=4 → 1 - 3/4 = 0.25
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(2), "q1", null, null, null, List.of("Go now"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(0.25, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 1.0 for quiz exactly at A1 minimum boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore10ForQuizExactlyAtA1MinimumBoundary() {
        // 5 tokens — exactly at min
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(5), "q1", null, null, null, List.of("I like big red cats"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(1.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 1.0 for quiz exactly at A1 maximum boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore10ForQuizExactlyAtA1MaximumBoundary() {
        // 8 tokens — exactly at max
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(8), "q1", null, null, null, List.of("I like big red cats very much here"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(1.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 0.0 for quiz 4 tokens above A1 max at tolerance boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R009")
    public void shouldScore00ForQuiz4TokensAboveA1MaxAtToleranceBoundary() {
        // 12 tokens — 4 above max of 8; distance=4 >= margin=4 → 0.0
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(12), "q1", null, null, null, List.of("She really likes eating big green apples from the local market"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(0.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should exclude non-sentence knowledge quiz from results")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeNonsentenceKnowledgeQuizFromResults() {
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should score 1.0 for B2 level quiz within range")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R012")
    public void shouldScore10ForB2LevelQuizWithinRange() {
        // milestone label "B2" → CefrLevel.B2; 15 tokens within [14, 17]
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Advanced grammar", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(15), "q1", null, null, null, List.of("The students should have been studying for their final exams much more carefully this semester"), null);
        AuditNode quizNode = fullTree("B2", knowledge, quiz);

        TargetRange rangeB2 = new TargetRange(CefrLevel.B2, 14, 17);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeB2));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);

        sut.onQuiz(quizNode);

        Assertions.assertEquals(1.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 0.0 for quiz exactly at tolerance boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R009")
    public void shouldScore00ForQuizExactlyAtToleranceBoundary() {
        // 1 token — 4 below min of 5; distance=4 >= margin=4 → 0.0
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), "q1", null, null, null, List.of("Go"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(0.0, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should score 0.5 for quiz 2 tokens above A1 max")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore05ForQuiz2TokensAboveA1Max() {
        // 10 tokens — 2 above max of 8; distance=2, margin=4 → 1 - 2/4 = 0.5
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(10), "q1", null, null, null, List.of("She really likes eating big green apples from the garden"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        setupA1Range();
        sut.onQuiz(quizNode);

        Assertions.assertEquals(0.5, quizNode.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-SLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        AuditNode topicNode = new AuditNode();
        topicNode.setTarget(AuditTarget.TOPIC);
        topicNode.setScores(new LinkedHashMap<>());
        topicNode.setChildren(new ArrayList<>());
        topicNode.setMetadata(new LinkedHashMap<>());
        Assertions.assertDoesNotThrow(() -> sut.onTopic(topicNode));
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-SLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        AuditNode courseNode = buildCourseNode();
        Assertions.assertDoesNotThrow(() -> sut.onCourseComplete(courseNode));
    }

    @Test
    @DisplayName("should produce correct scores for full milestone-knowledge-quiz sequence")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldProduceCorrectScoresForFullMilestoneknowledgequizSequence() {
        AuditNode courseNode = buildCourseNode();
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, "A1", null);
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditableTopic topic = new AuditableTopic(List.of(), null, null, null);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Basic greetings", "Complete the sentence", true, null, null, null);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);

        // quiz1: 6 tokens, within [5,8] → score 1.0
        AuditableQuiz quiz1 = new AuditableQuiz(tokens(6), "q1", null, null, null, List.of("Hello how are you today friend"), null);
        AuditNode quizNode1 = buildQuizNode(knowledgeNode, quiz1);

        // quiz2: 2 tokens, 3 below min 5, distance=3, margin=4 → score 0.25
        AuditableQuiz quiz2 = new AuditableQuiz(tokens(2), "q2", null, null, null, List.of("Good morning"), null);
        AuditNode quizNode2 = buildQuizNode(knowledgeNode, quiz2);

        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);

        sut.onMilestone(milestoneNode);
        sut.onKnowledge(knowledgeNode);
        sut.onQuiz(quizNode1);
        sut.onQuiz(quizNode2);

        Assertions.assertEquals(1.0, quizNode1.getScores().get("sentence-length"));
        Assertions.assertEquals(0.25, quizNode2.getScores().get("sentence-length"));
    }

    @Test
    @DisplayName("should exclude non-sentence quizzes from scoring")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeNonsentenceQuizzesFromScoring() {
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }
}
