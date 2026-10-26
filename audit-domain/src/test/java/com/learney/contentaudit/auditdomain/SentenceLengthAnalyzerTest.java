package com.learney.contentaudit.auditdomain;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;

import com.learney.contentaudit.coursedomain.SentenceMode;
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("Hello world test"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("Hello world test"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("abc", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should exclude quiz when no target range configured for level")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R012")
    public void shouldExcludeQuizWhenNoTargetRangeConfiguredForLevel() {
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("She likes apples"), null, null, null, null, null, null);
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
        AuditableKnowledge nonSentKnowledge = new AuditableKnowledge(List.of(), "Vocab", "Match", false, null, null, null, null, null);
        AuditNode nonSentKnowledgeNode = buildKnowledgeNode(topicNode, nonSentKnowledge);
        AuditableQuiz nonSentQuiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null, null, null, null, null, null);
        AuditNode nonSentQuizNode = buildQuizNode(nonSentKnowledgeNode, nonSentQuiz);

        // Sentence knowledge
        AuditableKnowledge sentKnowledge = new AuditableKnowledge(List.of(), "Sentences", "Complete", true, null, null, null, null, null);
        AuditNode sentKnowledgeNode = buildKnowledgeNode(topicNode, sentKnowledge);
        AuditableQuiz sentQuiz = new AuditableQuiz(tokens(6), null, null, null, null, List.of("She likes red apples very much"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(6), "q1", null, null, null, List.of("She likes apples a lot today"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(9), "q1", null, null, null, List.of("She really likes green apples a lot today quickly"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(2), "q1", null, null, null, List.of("Go now"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(5), "q1", null, null, null, List.of("I like big red cats"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(8), "q1", null, null, null, List.of("I like big red cats very much here"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(12), "q1", null, null, null, List.of("She really likes eating big green apples from the local market"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Advanced grammar", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(15), "q1", null, null, null, List.of("The students should have been studying for their final exams much more carefully this semester"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), "q1", null, null, null, List.of("Go"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(10), "q1", null, null, null, List.of("She really likes eating big green apples from the garden"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Basic greetings", "Complete the sentence", true, null, null, null, null, null);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);

        // quiz1: 6 tokens, within [5,8] → score 1.0
        AuditableQuiz quiz1 = new AuditableQuiz(tokens(6), "q1", null, null, null, List.of("Hello how are you today friend"), null, null, null, null, null, null);
        AuditNode quizNode1 = buildQuizNode(knowledgeNode, quiz1);

        // quiz2: 2 tokens, 3 below min 5, distance=3, margin=4 → score 0.25
        AuditableQuiz quiz2 = new AuditableQuiz(tokens(2), "q2", null, null, null, List.of("Good morning"), null, null, null, null, null, null);
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
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(1), null, null, null, null, List.of("apple"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"));
    }

    @Test
    @DisplayName("should emit a SentenceLengthDiagnosis on the quiz node populated with tokenCount, targetMin, targetMax, cefrLevel, delta and toleranceMargin matching the analyzer computation")
    @Tag("FEAT-DSLEN")
    @Tag("F-DSLEN-R001")
    public void shouldEmitASentenceLengthDiagnosisOnTheQuizNodePopulatedWithTokenCountTargetMinTargetMaxCefrLevelDeltaAndToleranceMarginMatchingTheAnalyzerComputation() {
        // R001: quiz with 11 tokens in A1 (range 5-8, margin 4) → delta=11-8=3 (over max)
        setupA1Range();
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "K", "instr", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(11), "q1", null, null, null, List.of("sentence quiz"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        NodeDiagnoses diag = quizNode.getDiagnoses();
        Assertions.assertNotNull(diag, "R001: quiz node must carry a NodeDiagnoses after scoring");
        Assertions.assertInstanceOf(QuizDiagnoses.class, diag, "R001: diagnoses must be QuizDiagnoses");

        Optional<SentenceLengthDiagnosis> sld = ((QuizDiagnoses) diag).getSentenceLengthDiagnosis();
        Assertions.assertTrue(sld.isPresent(), "R001: SentenceLengthDiagnosis must be present");

        SentenceLengthDiagnosis d = sld.get();
        Assertions.assertEquals(11, d.getTokenCount(), "R001: tokenCount must match actual tokens");
        Assertions.assertEquals(5, d.getTargetMin(), "R001: targetMin must match config range min");
        Assertions.assertEquals(8, d.getTargetMax(), "R001: targetMax must match config range max");
        Assertions.assertEquals(CefrLevel.A1, d.getCefrLevel(), "R001: cefrLevel must be A1 from milestone");
        Assertions.assertEquals(3, d.getDelta(), "R001: delta=tokenCount-targetMax=11-8=3 (over max)");
        Assertions.assertEquals(4, d.getToleranceMargin(), "R001: toleranceMargin must match config margin");
    }

    @Test
    @DisplayName("should NOT emit a SentenceLengthDiagnosis on a quiz node that is excluded as non-sentence (no scoring produced)")
    @Tag("FEAT-DSLEN")
    @Tag("F-DSLEN-R002")
    public void shouldNOTEmitASentenceLengthDiagnosisOnAQuizNodeThatIsExcludedAsNonsentenceNoScoringProduced() {
        // R002: quiz excluded because knowledge.isSentence() = false → no scoring → no diagnosis
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(3), null, null, null, null, List.of("apple"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertFalse(quizNode.getScores().containsKey("sentence-length"),
                "R002: excluded quiz must not have a sentence-length score");
        NodeDiagnoses diag = quizNode.getDiagnoses();
        if (diag instanceof QuizDiagnoses qd) {
            Assertions.assertFalse(qd.getSentenceLengthDiagnosis().isPresent(),
                    "R002: excluded quiz must not carry a SentenceLengthDiagnosis");
        }
    }

    @Test
    @DisplayName("should NOT emit a SentenceLengthDiagnosis on knowledge topic milestone or course nodes traversed by the analyzer")
    @Tag("FEAT-DSLEN")
    @Tag("F-DSLEN-R003")
    public void shouldNOTEmitASentenceLengthDiagnosisOnKnowledgeTopicMilestoneOrCourseNodesTraversedByTheAnalyzer() {
        // R003: non-quiz nodes visited by the analyzer must not carry a SentenceLengthDiagnosis
        setupA1Range();

        AuditNode courseNode = buildCourseNode();
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, "A1", null);
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditableTopic topic = new AuditableTopic(List.of(), null, null, null);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "K", "instr", true, null, null, null, null, null);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);

        sut.onMilestone(milestoneNode);
        sut.onTopic(topicNode);
        sut.onKnowledge(knowledgeNode);

        for (AuditNode node : List.of(courseNode, milestoneNode, topicNode, knowledgeNode)) {
            NodeDiagnoses diag = node.getDiagnoses();
            if (diag instanceof QuizDiagnoses qd) {
                Assertions.assertFalse(qd.getSentenceLengthDiagnosis().isPresent(),
                        "R003: non-quiz node must not carry SentenceLengthDiagnosis");
            }
        }
    }

    @Test
    @DisplayName("should make the emitted SentenceLengthDiagnosis retrievable via QuizDiagnoses getSentenceLengthDiagnosis on the same quiz node")
    @Tag("FEAT-DSLEN")
    @Tag("F-DSLEN-R004")
    public void shouldMakeTheEmittedSentenceLengthDiagnosisRetrievableViaQuizDiagnosesGetSentenceLengthDiagnosisOnTheSameQuizNode() {
        // R004: getSentenceLengthDiagnosis() on the same quiz node returns the diagnosis without unsafe casts
        setupA1Range();
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "K", "instr", true, null, null, null, null, null);
        AuditableQuiz quiz = new AuditableQuiz(tokens(6), "q1", null, null, null, List.of("six token sentence here"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        NodeDiagnoses diag = quizNode.getDiagnoses();
        Assertions.assertInstanceOf(QuizDiagnoses.class, diag, "R004: quiz node diagnoses must be QuizDiagnoses");

        Optional<SentenceLengthDiagnosis> sldOpt = ((QuizDiagnoses) diag).getSentenceLengthDiagnosis();
        Assertions.assertTrue(sldOpt.isPresent(), "R004: getSentenceLengthDiagnosis() must return present Optional");
        Assertions.assertNotNull(sldOpt.get(), "R004: SentenceLengthDiagnosis must not be null");
        // Accessing fields without unsafe casts confirms typed retrieval (R004 contract)
        Assertions.assertEquals(6, sldOpt.get().getTokenCount(), "R004: tokenCount accessible as typed int");
        Assertions.assertEquals(CefrLevel.A1, sldOpt.get().getCefrLevel(), "R004: cefrLevel accessible as typed CefrLevel");
    }

    @Test
    @DisplayName("should compute each quiz score using the linguistic token count from the precomputed NLP tokenization of the quiz sentence and never from a whitespace-based string split")
    @Tag("FEAT-SLEN")
    @Tag("F-SLEN-R013")
    public void shouldComputeEachQuizScoreUsingTheLinguisticTokenCountFromThePrecomputedNLPTokenizationOfTheQuizSentenceAndNeverFromAWhitespacebasedStringSplit() {
        // R013: token count comes from quiz.getTokens().size(), not sentence.split(" ").length
        // "She's running" → whitespace split = 2, but NLP tokenization = 3 ("She", "'s", "running")
        // We model this by giving the quiz 3 NlpTokens but a sentence string with only 2 whitespace words.
        setupA1Range(); // [5,8] range

        // 3 tokens from NLP (in-range for A1 → but wait, range is [5,8], so 3 tokens → out of range)
        // Use 6 tokens to get a clear in-range score, with sentence text having only 2 words
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Sentences", "Complete", true, "k1", "K", null, null, null);
        // 6 linguistic tokens but only 2 whitespace-delimited words in the sentence text
        List<NlpToken> sixTokens = tokens(6);
        AuditableQuiz quiz = new AuditableQuiz(
                sixTokens, "q1", "Q", null, null,
                List.of("She's running"), // 2 whitespace words, 3 would be out-of-range; NLP gives 6
                null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        // Score must be present and reflect 6 linguistic tokens (in range [5,8] → score 1.0)
        Assertions.assertTrue(quizNode.getScores().containsKey("sentence-length"),
                "R013: sentence-length score must be produced");
        double score = quizNode.getScores().get("sentence-length");
        Assertions.assertEquals(1.0, score, 0.001,
                "R013: score must reflect 6 NLP tokens (in range [5,8]), not 2 whitespace words");
    }

    @Test
    @DisplayName("should compute the length score over the mode-determined canonical phrase token count and not over a blind concatenation of all quiz parts")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R005")
    public void shouldComputeTheLengthScoreOverTheModedeterminedCanonicalPhraseTokenCountAndNotOverABlindConcatenationOfAllQuizParts() {
        // R005: el score se calcula sobre la frase canonica determinada por el modo (quiz.getTokens()),
        // no sobre una concatenacion de todas las partes del quiz.
        //
        // Setup: knowledge REWRITE, nivel A1, rango [3,8] margen 5.
        // La frase canonica del quiz es solo la respuesta: "Watch the DVD." → 4 tokens (en rango → 1.0).
        // Si se concatenara ciegamente fuente + respuesta: 10 tokens → 1-(10-8)/5 = 0.6.
        // El diagnostico debe reflejar tokenCount=4 (la canonica) y el score debe ser 1.0.
        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 3, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(5);

        // El knowledge declara modo REWRITE: la seleccion de la frase canonica ya ocurrio upstream;
        // quiz.getTokens() contiene SOLO los tokens de la respuesta (4 tokens).
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Rewrite exercises", "Transform the sentence", true, "k1", null, null, SentenceMode.REWRITE, null);
        AuditableQuiz quiz = new AuditableQuiz(
                tokens(4), "q1", null, null, null,
                List.of("Watch the DVD."), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        // El score refleja 4 tokens (en rango [3,8]) → 1.0
        Assertions.assertTrue(quizNode.getScores().containsKey("sentence-length"),
                "R005: el quiz REWRITE debe producir score de sentence-length");
        Assertions.assertEquals(1.0, quizNode.getScores().get("sentence-length"), 0.001,
                "R005: el score debe ser 1.0 sobre 4 tokens de la frase canonica, no 0.6 sobre 10 tokens de concatenacion");

        // El diagnostico refleja tokenCount=4 (la frase canonica determinada por el modo)
        NodeDiagnoses diag = quizNode.getDiagnoses();
        Assertions.assertInstanceOf(QuizDiagnoses.class, diag);
        Optional<SentenceLengthDiagnosis> sld = ((QuizDiagnoses) diag).getSentenceLengthDiagnosis();
        Assertions.assertTrue(sld.isPresent(), "R005: SentenceLengthDiagnosis debe estar presente");
        Assertions.assertEquals(4, sld.get().getTokenCount(),
                "R005: tokenCount debe reflejar la frase canonica (4 tokens de la respuesta), no 10 tokens de concatenacion");
    }

    @Test
    @DisplayName("should score a REWRITE quiz answer Watch the DVD at 100 percent on 4 tokens for A1 instead of 60 percent on the 10-token source-plus-answer concatenation")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R007")
    public void shouldScoreAREWRITEQuizAnswerWatchTheDVDAt100PercentOn4TokensForA1InsteadOf60PercentOnThe10tokenSourceplusanswerConcatenation() {
        // R007 — caso de aceptacion del bug real (F-SMODE-R004, Detalle):
        // Quiz REWRITE A1 cuya respuesta es "Watch the DVD." (4 tokens).
        // Nivel A1: rango [3,8], margen 5.
        //   - Correcto (frase canonica = solo la respuesta): 4 tokens, dentro del rango → score 1.0 (100%).
        //   - Erroneo (concatenacion ciega fuente+respuesta): "You should watch the DVD. Watch the DVD."
        //     = 10 tokens → distancia al max (8) = 2, 1 - 2/5 = 0.6 (60%).
        // El test afirma 1.0 (100%), demostrando que el analyzer puntua solo la respuesta.
        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 3, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(5);

        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Rewrite imperatives", "Transform the sentence to imperative", true, "k2", null, null, SentenceMode.REWRITE, null);
        // quiz.getTokens() = 4 tokens (solo la respuesta "Watch the DVD.", frase canonica)
        AuditableQuiz quiz = new AuditableQuiz(
                tokens(4), "q-dvd", null, null, null,
                List.of("Watch the DVD."), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);

        Assertions.assertTrue(quizNode.getScores().containsKey("sentence-length"),
                "R007: el quiz REWRITE 'Watch the DVD.' debe tener score de sentence-length");
        double score = quizNode.getScores().get("sentence-length");
        Assertions.assertEquals(1.0, score, 0.001,
                "R007: score debe ser 1.0 (100%) sobre 4 tokens de la respuesta, no 0.6 (60%) sobre 10 tokens de la concatenacion erronea");
    }

    @Test
    @DisplayName("should grade low and resolve at the panel the finding of «He isn't in the living-room.», one token over the 3 to 8 of A1")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R002")
    public void shouldGradeLowAndResolveAtThePanelTheFindingOfHeIsntInTheLivingroomOneTokenOverThe3To8OfA1() {
        // R002 / DOUBT-GRAVEDAD-EXISTENTES: the rest of the vocabulary is low; sentence-length
        // is corrected at the panel. «He isn't in the living-room.» has 9 tokens in A1 (3 to 8).
        AuditNode quizNode = livingRoomQuiz();

        sut.onQuiz(quizNode);
        List<FindingDraft> drafts = sut.findingsAt(quizNode);

        Assertions.assertEquals(0.8, quizNode.getScores().get("sentence-length"), 1e-9,
                "one token over a margin of 5 scores 0,8");
        Assertions.assertEquals(1, drafts.size(), "the quiz below 1 has exactly one sentence-length finding");
        Assertions.assertEquals(FindingSeverity.LOW, drafts.get(0).getSeverity(),
                "R002: a sentence-length finding is graded low");
        Assertions.assertEquals(FindingResolution.PANEL, drafts.get(0).getResolution(),
                "R002: a sentence-length finding is resolved at the panel");
    }

    @Test
    @DisplayName("should give the finding of «He isn't in the living-room.» as evidence the sentence as the student reads it and its measure next to its goal, 9 tokens against the 3 to 8 of A1, never the internal format of the course")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R003")
    public void shouldGiveTheFindingOfHeIsntInTheLivingroomAsEvidenceTheSentenceAsTheStudentReadsItAndItsMeasureNextToItsGoal9TokensAgainstThe3To8OfA1NeverTheInternalFormatOfTheCourse() {
        // R003: what it looked at, as the student reads it, and the measure next to its goal --
        // never the internal format of the course (the DSL with brackets and the gap).
        AuditNode quizNode = livingRoomQuiz();

        sut.onQuiz(quizNode);
        FindingDraft finding = sut.findingsAt(quizNode).get(0);

        List<EvidencePart> examined = finding.getEvidence().getExamined();
        Assertions.assertTrue(examined.stream().anyMatch(p -> LIVING_ROOM.equals(p.getText())),
                "R003: the evidence shows the sentence as the student reads it: " + texts(examined));
        Assertions.assertTrue(examined.stream().anyMatch(p -> p.getText().contains("9 tokens")),
                "R003: the evidence carries the measure it took, 9 tokens: " + texts(examined));
        Assertions.assertTrue(examined.stream().anyMatch(p -> p.getText().contains("A1 de 3 a 8")),
                "R003: the evidence carries the goal next to the measure, A1 from 3 to 8: " + texts(examined));
        Assertions.assertTrue(examined.stream().noneMatch(p -> p.getText().contains(LIVING_ROOM_DSL)
                        || p.getText().contains("[") || p.getText().contains("____")),
                "R003: the internal format of the course never reaches the evidence: " + texts(examined));
        Assertions.assertTrue(finding.getEvidence().getObservation().contains("9 tokens")
                        && finding.getEvidence().getObservation().contains("A1 de 3 a 8"),
                "R003: one line that names the problem with the words of the node: "
                        + finding.getEvidence().getObservation());
    }

    @Test
    @DisplayName("should keep emitting for «He isn't in the living-room.» the same SentenceLengthDiagnosis as before, 9 tokens against 3 to 8 with delta 1 and margin 5, with nothing of it copied into its finding")
    @Tag("FEAT-HALL")
    @Tag("F-HALL-R001")
    public void shouldKeepEmittingForHeIsntInTheLivingroomTheSameSentenceLengthDiagnosisAsBefore9TokensAgainst3To8WithDelta1AndMargin5WithNothingOfItCopiedIntoItsFinding() {
        // R001 inv. 3: the typed diagnosis keeps being emitted exactly as before, and the finding
        // only carries the fixed fields of the contract -- what belongs to the analyzer stays in
        // its typed diagnosis, which findingsAt reads and never rewrites.
        AuditNode quizNode = livingRoomQuiz();
        SentenceLengthDiagnosis expected = new SentenceLengthDiagnosis(9, 3, 8, CefrLevel.A1, 1, 5);

        sut.onQuiz(quizNode);
        DefaultQuizDiagnoses diagnoses = (DefaultQuizDiagnoses) quizNode.getDiagnoses();
        Assertions.assertEquals(Optional.of(expected), diagnoses.getSentenceLengthDiagnosis(),
                "R001: the SentenceLengthDiagnosis is the one of before: 9 tokens, 3 to 8, A1, delta 1, margin 5");

        List<FindingDraft> drafts = sut.findingsAt(quizNode);

        Assertions.assertEquals(Optional.of(expected), diagnoses.getSentenceLengthDiagnosis(),
                "R001: asking for the findings leaves the typed diagnosis as it was");
        Assertions.assertEquals(1, drafts.size());
        FindingDraft finding = drafts.get(0);
        Assertions.assertNotNull(finding.getRule(), "the finding names the rule of the card");
        Assertions.assertNull(finding.getMarker(),
                "sentence-length marks nothing inside the quiz: one finding per quiz, no marker");
        Assertions.assertEquals(new FindingDraft(finding.getRule(), null, finding.getSeverity(),
                        finding.getResolution(), finding.getEvidence()), finding,
                "R001 inv. 3: the finding is its fixed fields and nothing else");
    }

    // ------------------------------------------------------------------
    // FEAT-HALL fixtures: «He isn't in the living-room.», an A1 quiz of the 29/9 base
    // ------------------------------------------------------------------

    private static final String LIVING_ROOM = "He isn't in the living-room.";

    private static final String LIVING_ROOM_DSL = "He ____ [isn't] in the living-room.";

    /** The quiz as the 29/9 base has it: 9 spaCy tokens in A1, where the code applies 3 to 8. */
    private AuditNode livingRoomQuiz() {
        Mockito.lenient().when(config.getTargetRange(CefrLevel.A1))
                .thenReturn(Optional.of(new TargetRange(CefrLevel.A1, 3, 8)));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(5);
        List<NlpToken> spacyTokens = new ArrayList<>();
        for (String text : List.of("He", "is", "n't", "in", "the", "living", "-", "room", ".")) {
            spacyTokens.add(new NlpToken(text, text.toLowerCase(), "X", 100, false, ".".equals(text)));
        }
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Be: preguntas yes / no",
                "Completa con la forma correcta.", true, "k-be", "Be: preguntas yes / no", "K", null, null);
        AuditableQuiz quiz = new AuditableQuiz(spacyTokens, "67fab6d599301022953425aa", LIVING_ROOM, "Q", null,
                List.of(LIVING_ROOM), LIVING_ROOM_DSL, null, null, null, null, null);
        return fullTree("A1", knowledge, quiz);
    }

    private static List<String> texts(List<EvidencePart> parts) {
        List<String> result = new ArrayList<>();
        for (EvidencePart part : parts) {
            result.add(part.getLabel() + ": " + part.getText());
        }
        return result;
    }

    @Test
    @DisplayName("should mark «He isn't in the living-room.», 9 tokens against the 3 to 8 of A1, with the rule for longer sentences")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R005")
    public void shouldMarkHeIsntInTheLivingroom9TokensAgainstThe3To8OfA1WithTheRuleForLongerSentences() {
        // R005 inv. 4: «He isn't in the living-room.», 9 tokens against the 3 to 8 of A1, is longer
        // than the goal of its level: its finding is the card's rule for longer sentences.
        AuditNode quizNode = livingRoomQuiz();

        sut.onQuiz(quizNode);
        List<FindingDraft> drafts = sut.findingsAt(quizNode);

        Assertions.assertEquals(0.8, quizNode.getScores().get("sentence-length"), 1e-9);
        Assertions.assertEquals(1, drafts.size());
        Assertions.assertEquals("longer-than-goal", drafts.get(0).getRule(), "R005 inv. 4: the rule for longer sentences");
        Assertions.assertTrue(drafts.get(0).getEvidence().getObservation().contains("1 de más"),
                drafts.get(0).getEvidence().getObservation());
    }

    @Test
    @DisplayName("should mark a sentence under the minimum of its level with the rule for shorter sentences")
    @Tag("FEAT-RONDA1")
    @Tag("F-RONDA1-R005")
    public void shouldMarkASentenceUnderTheMinimumOfItsLevelWithTheRuleForShorterSentences() {
        // R005 inv. 4: «an apple» (…234f, A1, Elegir entre a y an), 2 spaCy tokens against the 3 to 8
        // of A1, is shorter than the goal: its finding is the card's rule for shorter sentences.
        Mockito.lenient().when(config.getTargetRange(CefrLevel.A1))
                .thenReturn(Optional.of(new TargetRange(CefrLevel.A1, 3, 8)));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(5);
        List<NlpToken> spacyTokens = List.of(new NlpToken("an", "an", "DET", 4, true, false),
                new NlpToken("apple", "apple", "NOUN", 1327, false, false));
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Elegir entre $a$ y $an$",
                "Elige a o an.", true, "k-a-an", "Elegir entre $a$ y $an$", "K", null, null);
        AuditableQuiz quiz = new AuditableQuiz(spacyTokens, "67fab6d5993010229534234f", "Elegir entre $a$ y $an$", "Q",
                null, List.of("an apple"), null, null, null, null, null, null);
        AuditNode quizNode = fullTree("A1", knowledge, quiz);

        sut.onQuiz(quizNode);
        List<FindingDraft> drafts = sut.findingsAt(quizNode);

        Assertions.assertEquals(0.8, quizNode.getScores().get("sentence-length"), 1e-9, "one token short, margin 5");
        Assertions.assertEquals(1, drafts.size());
        Assertions.assertEquals("shorter-than-goal", drafts.get(0).getRule(), "R005 inv. 4: the rule for shorter sentences");
        Assertions.assertTrue(drafts.get(0).getEvidence().getObservation().contains("1 de menos"),
                drafts.get(0).getEvidence().getObservation());
    }

    // ------------------------------------------------------------------
    // F-SMODE-R004: a rewrite exercise is measured on its answer alone
    // ------------------------------------------------------------------

    /** Words and marks, cut at the spaces like spaCy: enough to tell how many tokens a sentence takes. */
    private static List<NlpToken> wordsAndMarks(String sentence) {
        List<NlpToken> words = new ArrayList<>();
        for (String chunk : sentence.trim().split("\\s+")) {
            String word = chunk.replaceAll("[.?!,]+$", "");
            if (!word.isEmpty()) words.add(new NlpToken(word, word, "X", 0, false, false));
            if (word.length() < chunk.length()) {
                String mark = chunk.substring(word.length());
                words.add(new NlpToken(mark, mark, "PUNCT", 0, false, true));
            }
        }
        return words;
    }

    private static com.learney.contentaudit.coursedomain.SentencePartEntity text(String text) {
        return new com.learney.contentaudit.coursedomain.SentencePartEntity(
                com.learney.contentaudit.coursedomain.SentencePartKind.TEXT, text, null);
    }

    private static com.learney.contentaudit.coursedomain.SentencePartEntity gap(String... answers) {
        return new com.learney.contentaudit.coursedomain.SentencePartEntity(
                com.learney.contentaudit.coursedomain.SentencePartKind.CLOZE, "", List.of(answers));
    }

    /** An exercise of A1 as the analysis maps it: its parts, its stored sentence and that sentence's tokens. */
    private AuditNode rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode mode, String id, String stored,
            com.learney.contentaudit.coursedomain.SentencePartEntity... parts) {
        Mockito.lenient().when(config.getTargetRange(Mockito.any()))
                .thenReturn(Optional.of(new TargetRange(CefrLevel.A1, 3, 8)));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(5);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Pedidos: formal o informal", "Transforma.",
                true, "k-" + id, "Pedidos: formal o informal", "K", mode, null);
        AuditableQuiz quiz = new AuditableQuiz(wordsAndMarks(stored), id, "Pedidos", "Q", null, List.of(stored), null,
                null, List.of(parts), null, null, null);
        return fullTree("A1", knowledge, quiz);
    }

    private static SentenceLengthAnalyzer measuringAnswers(NlpTokenizer tokenizer, SentenceLengthConfig config) {
        return new SentenceLengthAnalyzer(tokenizer, config, new com.learney.contentaudit.auditdomain.studentview
                .DefaultQuizStudentViewRenderer(com.learney.contentaudit.coursedomain.quizsentenceengine
                .DefaultQuizSentenceConverter.create()));
    }

    private static int counted(AuditNode quizNode) {
        return ((DefaultQuizDiagnoses) quizNode.getDiagnoses()).getSentenceLengthDiagnosis().orElseThrow().getTokenCount();
    }

    @Test
    @DisplayName("should measure only the answer of a rewrite exercise whose stored sentence is its source and its answer together: «I want you to help me. Could you help me?» counts the 5 tokens of «Could you help me?»")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R004")
    public void shouldMeasureOnlyTheAnswerOfARewriteExerciseWhoseStoredSentenceIsItsSourceAndItsAnswerTogether() {
        // Three real exercises of C2 (25/10/2026), as the panel left them: the source in its own TEXT, with a hint after
        // it, or sharing its TEXT with the start of the answer. Each was measured over source and answer, out of range.
        var REWRITE = com.learney.contentaudit.coursedomain.SentenceMode.REWRITE;
        AuditNode help = rewriteQuiz(REWRITE, "2dd7", "I want you to help me. Could you help me?",
                text("I want you to help me. "), gap("Could you help me?", "Can you help me?"));
        AuditNode peter = rewriteQuiz(REWRITE, "2589", "Peter has breakfast at home. Peter always has breakfast at home.",
                text("Peter has breakfast at home. (always) "), gap("Peter always has breakfast at home."));
        AuditNode camera = rewriteQuiz(REWRITE, "2de8", "It's not okay to use a camera here. You can't use a camera here.",
                text("It's not okay to use a camera here. You "), gap("can't"), text(" use a camera here."));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        for (AuditNode quiz : List.of(help, peter, camera)) analyzer.onQuiz(quiz);

        Assertions.assertEquals(List.of(5, 7, 7), List.of(counted(help), counted(peter), counted(camera)),
                "the answer's tokens alone: 12, 13 and 16 with the source");
        for (AuditNode quiz : List.of(help, peter, camera)) {
            Assertions.assertEquals(1.0, quiz.getScores().get("sentence-length"), 1e-9, "within 3 to 8");
            Assertions.assertEquals(List.of(), analyzer.findingsAt(quiz));
        }
    }

    @Test
    @DisplayName("should give as evidence of a rewrite exercise measured on its answer alone that answer, «They want a bigger house. They'd like a bigger house.», with the tokens of that answer")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R004")
    public void shouldGiveAsEvidenceOfARewriteExerciseMeasuredOnItsAnswerAloneThatAnswerWithItsTokens() {
        // A real C2 exercise whose answer asks to write the source again: still too long on its answer alone.
        String answer = "They want a bigger house. They'd like a bigger house.";
        AuditNode house = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.REWRITE, "2535",
                "They want a bigger house. " + answer, text("They want a bigger house. "),
                text("(Escribe primero la oración original y después la versión amable.) "),
                gap(answer, "They want a bigger house. They would like a bigger house."));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        analyzer.onQuiz(house);
        List<EvidencePart> examined = analyzer.findingsAt(house).get(0).getEvidence().getExamined();

        Assertions.assertEquals(answer, examined.get(0).getText());
        Assertions.assertEquals("12 tokens", examined.get(1).getText());
    }

    @Test
    @DisplayName("should keep measuring the stored sentence of a rewrite exercise that already holds its answer alone, a fill exercise not cut to its answer, and every exercise as before without the finished sentence")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R004")
    public void shouldKeepMeasuringTheStoredSentenceOfARewriteExerciseThatAlreadyHoldsItsAnswerAloneAndAFillWhole() {
        // «She visits her father (every week)» has no period, so built from its parts it would read the source twice;
        // its stored sentence is the answer and stays what is measured. A fill exercise is not cut to its answer: its two
        // sentences fit the range together, so they are measured whole (F-SMODE-R008).
        AuditNode father = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.REWRITE, "2f0e",
                "She visits her father every week.", text("She visits her father (every week)"),
                gap("She visits her father every week."), text(""));
        AuditNode hungry = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "fill",
                "I'm hungry. I want a sandwich.", text("I'm hungry. I "), gap("want"), text(" a sandwich."));
        AuditNode withoutRenderer = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.REWRITE, "2dd7",
                "I want you to help me. Could you help me?", text("I want you to help me. "), gap("Could you help me?"));

        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);
        analyzer.onQuiz(father);
        analyzer.onQuiz(hungry);
        new SentenceLengthAnalyzer(null, config).onQuiz(withoutRenderer);

        Assertions.assertEquals(List.of(7, 8, 12), List.of(counted(father), counted(hungry), counted(withoutRenderer)));
    }

    // ------------------------------------------------------------------
    // F-SMODE-R008: a statement of several sentences is measured by its longest one
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should measure the longest line of a dialogue: «A: Is he tired? B: No, he isn't.» counts only «B: No, he isn't.», within the 3 to 8 of A1")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R008")
    public void shouldMeasureTheLongestLineOfADialogue() {
        // A real exercise of A1 (C2, 26/10/2026): 14 tokens whole, so the corrector cut «No,» or the question to reach 8.
        AuditNode tired = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "tired",
                "A: Is he tired? B: No, he isn't.", text("A: Is he tired? B: No, he "), gap("isn't"), text("."));
        AuditNode water = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "1f57",
                "Does he need water? Yes, he does.", text("Does he need water? Yes, he "), gap("does"), text(" ."));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        analyzer.onQuiz(tired);
        analyzer.onQuiz(water);

        // The words and marks of this test keep «B:» as one token, as spaCy does not: 6 here, 7 with spaCy.
        Assertions.assertEquals(List.of(6, 5), List.of(counted(tired), counted(water)), "12 and 10 whole");
        Assertions.assertEquals(List.of(), analyzer.findingsAt(tired));
        Assertions.assertEquals(1.0, water.getScores().get("sentence-length"), 1e-9);
    }

    @Test
    @DisplayName("should give as evidence of a statement of several sentences the longest one, with its tokens, when it is still too long")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R008")
    public void shouldGiveAsEvidenceOfAStatementOfSeveralSentencesTheLongestOne() {
        String question = "A: Am I parking in the right space near the door?";
        AuditNode parking = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "262e",
                question + " B: No, you aren't.", text(question + " B: No, you "), gap("aren't"), text("."));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        analyzer.onQuiz(parking);
        List<EvidencePart> examined = analyzer.findingsAt(parking).get(0).getEvidence().getExamined();

        Assertions.assertEquals(question, examined.get(0).getText());
        Assertions.assertEquals("12 tokens", examined.get(1).getText());
    }

    @Test
    @DisplayName("should keep measuring whole a dialogue that fits the range, «A: Hi! B: Hello.», and a too long one whose longest sentence is below the range")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R008")
    public void shouldKeepMeasuringWholeAStatementThatFitsTheRangeOrWhoseLongestSentenceIsTooShort() {
        // Measured by its longest line, «A: Hi!», the greeting would be 3 tokens, shorter than the 5 to 8 of the range:
        // a dialogue of short lines is not short. Five one-word answers are 10 tokens and 2 the longest: whole too.
        AuditNode greeting = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "hi",
                "A: Hi! B: Hello.", text("A: Hi! B: "), gap("Hello"), text("."));
        AuditNode answers = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "short",
                "Yes. No. Maybe. Sure. Okay.", text("Yes. No. Maybe. Sure. "), gap("Okay"), text("."));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        analyzer.onQuiz(greeting);
        analyzer.onQuiz(answers);

        Assertions.assertEquals(List.of(6, 10), List.of(counted(greeting), counted(answers)));
        Assertions.assertEquals(List.of(), analyzer.findingsAt(greeting));
    }

    @Test
    @DisplayName("should keep measuring whole a rewrite answer of two sentences, «Mr. Brown is here.» as one sentence, and a statement its tokens do not spell")
    @Tag("FEAT-SMODE")
    @Tag("F-SMODE-R008")
    public void shouldKeepMeasuringWholeARewriteAnswerOfTwoSentencesAnAbbreviationAndTokensThatDoNotSpellIt() {
        // A transformation asks for one sentence: an answer of two is not cut, so its length still shows the glue.
        AuditNode glued = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.REWRITE, "2535",
                "They want a bigger house. They'd like a bigger house.",
                gap("They want a bigger house. They'd like a bigger house."));
        AuditNode mister = rewriteQuiz(com.learney.contentaudit.coursedomain.SentenceMode.FILL, "mr",
                "Mr. Brown is my new English teacher at school.", text("Mr. Brown "), gap("is"),
                text(" my new English teacher at school."));
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "T", "Completa.", true, "k-w", "T", "K",
                com.learney.contentaudit.coursedomain.SentenceMode.FILL, null);
        AuditNode unspelled = fullTree("A1", knowledge, new AuditableQuiz(tokens(10), "w", "T", "Q", null,
                List.of("Is he here? Yes, he is."), null, null, List.of(), null, null, null));
        SentenceLengthAnalyzer analyzer = measuringAnswers(null, config);

        for (AuditNode quiz : List.of(glued, mister, unspelled)) analyzer.onQuiz(quiz);

        // «Mr.» is two tokens with the words and marks of this test, one with spaCy: 11 here, whole.
        Assertions.assertEquals(List.of(12, 11, 10), List.of(counted(glued), counted(mister), counted(unspelled)));
    }
}
