package com.learney.contentaudit.auditdomain;

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
    private NlpTokenizer nlpTokenizer;

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
        return java.util.Collections.nCopies(count, token());
    }

    private void setupA1Range() {
        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should exclude quiz when milestoneId is null")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeQuizWhenMilestoneIdIsNull() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext(null, "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext(null, "t1", "k1", "q1", null, null, null);
        AuditableQuiz quiz = new AuditableQuiz("Hello world test", tokens(3), null, null, null);
        AuditContext ctxQuiz = new AuditContext(null, "t1", "k1", "q1", null, null, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        Assertions.assertEquals(List.of(), sut.getResults());
    }

    @Test
    @DisplayName("should exclude quiz when milestoneId is non-numeric")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeQuizWhenMilestoneIdIsNonnumeric() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("abc", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("abc", "t1", "k1", "q1", null, null, null);
        AuditableQuiz quiz = new AuditableQuiz("Hello world test", tokens(3), null, null, null);
        AuditContext ctxQuiz = new AuditContext("abc", "t1", "k1", "q1", null, null, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        Assertions.assertEquals(List.of(), sut.getResults());
    }

    @Test
    @DisplayName("should exclude quiz when no target range configured for level")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R012")
    public void shouldExcludeQuizWhenNoTargetRangeConfiguredForLevel() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableQuiz quiz = new AuditableQuiz("She likes apples", tokens(3), null, null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.empty());
        sut.onQuiz(quiz, ctxQuiz);

        Assertions.assertEquals(List.of(), sut.getResults());
    }

    @Test
    @DisplayName("should score only sentence quizzes when processing mixed knowledge types")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldScoreOnlySentenceQuizzesWhenProcessingMixedKnowledgeTypes() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        // Non-sentence knowledge
        AuditableKnowledge nonSentKnowledge = new AuditableKnowledge(List.of(), "Vocab", "Match", false, null, null, null);
        AuditContext ctxNonSentKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableQuiz nonSentQuiz = new AuditableQuiz("apple", tokens(1), null, null, null);
        AuditContext ctxNonSentQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        // Sentence knowledge
        AuditableKnowledge sentKnowledge = new AuditableKnowledge(List.of(), "Sentences", "Complete", true, null, null, null);
        AuditContext ctxSentKnowledge = new AuditContext("0", "t1", "k2", "q2", null, null, null);
        AuditableQuiz sentQuiz = new AuditableQuiz("She likes red apples very much", tokens(6), null, null, null);
        AuditContext ctxSentQuiz = new AuditContext("0", "t1", "k2", "q2", null, null, null);

        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        ScoredItem expectedScore = new ScoredItem("sentence-length", AuditTarget.QUIZ, 1.0, "0", "t1", "k2", "q2", sentQuiz, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(nonSentKnowledge, ctxNonSentKnowledge);
        sut.onQuiz(nonSentQuiz, ctxNonSentQuiz);
        sut.onKnowledge(sentKnowledge, ctxSentKnowledge);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);
        sut.onQuiz(sentQuiz, ctxSentQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedScore, result.get(0));
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
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 6 tokens — within [5, 8]
        AuditableQuiz quiz = new AuditableQuiz("She likes apples a lot today", tokens(6), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.75 for quiz 1 token above A1 max")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore075ForQuiz1TokenAboveA1Max() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 9 tokens — 1 above max of 8; distance=1, margin=4 → 1 - 1/4 = 0.75
        AuditableQuiz quiz = new AuditableQuiz("She really likes green apples a lot today quickly", tokens(9), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.75, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.25 for quiz 3 tokens below A1 min")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore025ForQuiz3TokensBelowA1Min() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 2 tokens — 3 below min of 5; distance=3, margin=4 → 1 - 3/4 = 0.25
        AuditableQuiz quiz = new AuditableQuiz("Go now", tokens(2), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.25, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 1.0 for quiz exactly at A1 minimum boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore10ForQuizExactlyAtA1MinimumBoundary() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 5 tokens — exactly at min
        AuditableQuiz quiz = new AuditableQuiz("I like big red cats", tokens(5), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 1.0 for quiz exactly at A1 maximum boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore10ForQuizExactlyAtA1MaximumBoundary() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 8 tokens — exactly at max
        AuditableQuiz quiz = new AuditableQuiz("I like big red cats very much here", tokens(8), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.0 for quiz 4 tokens above A1 max at tolerance boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R009")
    public void shouldScore00ForQuiz4TokensAboveA1MaxAtToleranceBoundary() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 12 tokens — 4 above max of 8; distance=4 >= margin=4 → 0.0
        AuditableQuiz quiz = new AuditableQuiz("She really likes eating big green apples from the local market", tokens(12), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should exclude non-sentence knowledge quiz from results")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeNonsentenceKnowledgeQuizFromResults() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableQuiz quiz = new AuditableQuiz("apple", tokens(1), null, null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        Assertions.assertEquals(List.of(), sut.getResults());
    }

    @Test
    @DisplayName("should score 1.0 for B2 level quiz within range")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R012")
    public void shouldScore10ForB2LevelQuizWithinRange() {
        // milestoneId "3" → index 3 → CefrLevel.B2
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("3", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Advanced grammar", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("3", "t1", "k1", "q1", null, null, null);
        // 15 tokens — within [14, 17]
        AuditableQuiz quiz = new AuditableQuiz(
                "The students should have been studying for their final exams much more carefully this semester",
                tokens(15), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("3", "t1", "k1", "q1", null, null, null);
        TargetRange rangeB2 = new TargetRange(CefrLevel.B2, 14, 17);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeB2));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.0 for quiz exactly at tolerance boundary")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R009")
    public void shouldScore00ForQuizExactlyAtToleranceBoundary() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 1 token — 4 below min of 5; distance=4 >= margin=4 → 0.0
        AuditableQuiz quiz = new AuditableQuiz("Go", tokens(1), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.get(0).getScore());
    }

    @Test
    @DisplayName("should score 0.5 for quiz 2 tokens above A1 max")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldScore05ForQuiz2TokensAboveA1Max() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Greetings", "Complete", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        // 10 tokens — 2 above max of 8; distance=2, margin=4 → 1 - 2/4 = 0.5
        AuditableQuiz quiz = new AuditableQuiz("She really likes eating big green apples from the garden", tokens(10), "q1", null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        setupA1Range();
        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.5, result.get(0).getScore());
    }

    @Test
    @DisplayName("should complete without error when onTopic is called")
    @Tag("F-SLEN")
    public void shouldCompleteWithoutErrorWhenOnTopicIsCalled() {
        AuditableTopic topic = new AuditableTopic(List.of(), null, null, null);
        AuditContext ctx = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        Assertions.assertDoesNotThrow(() -> sut.onTopic(topic, ctx));
    }

    @Test
    @DisplayName("should complete without error when onCourseComplete is called")
    @Tag("F-SLEN")
    public void shouldCompleteWithoutErrorWhenOnCourseCompleteIsCalled() {
        AuditableCourse course = new AuditableCourse(List.of());
        AuditContext ctx = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        Assertions.assertDoesNotThrow(() -> sut.onCourseComplete(course, ctx));
    }

    @Test
    @DisplayName("should produce correct scores for full milestone-knowledge-quiz sequence")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R002")
    public void shouldProduceCorrectScoresForFullMilestoneknowledgequizSequence() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Basic greetings", "Complete the sentence", true, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        // quiz1: 6 tokens, within [5,8] → score 1.0
        AuditableQuiz quiz1 = new AuditableQuiz("Hello how are you today friend", tokens(6), "q1", null, null);
        AuditContext ctxQuiz1 = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        // quiz2: 2 tokens, 3 below min 5, distance=3, margin=4 → score 0.25
        AuditableQuiz quiz2 = new AuditableQuiz("Good morning", tokens(2), "q2", null, null);
        AuditContext ctxQuiz2 = new AuditContext("0", "t1", "k1", "q2", null, null, null);

        TargetRange rangeA1 = new TargetRange(CefrLevel.A1, 5, 8);
        Mockito.lenient().when(config.getTargetRange(Mockito.any())).thenReturn(Optional.of(rangeA1));
        Mockito.lenient().when(config.getToleranceMargin()).thenReturn(4);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz1, ctxQuiz1);
        sut.onQuiz(quiz2, ctxQuiz2);

        List<ScoredItem> result = sut.getResults();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(1.0, result.get(0).getScore());
        Assertions.assertEquals(0.25, result.get(1).getScore());
        Assertions.assertEquals("q1", result.get(0).getQuizId());
        Assertions.assertEquals("q2", result.get(1).getQuizId());
    }

    @Test
    @DisplayName("should exclude non-sentence quizzes from scoring")
    @Tag("F-SLEN")
    @Tag("F-SLEN-R001")
    public void shouldExcludeNonsentenceQuizzesFromScoring() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), null, null, null);
        AuditContext ctxMilestone = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Vocabulary", "Match words", false, null, null, null);
        AuditContext ctxKnowledge = new AuditContext("0", "t1", "k1", "q1", null, null, null);
        AuditableQuiz quiz = new AuditableQuiz("apple", tokens(1), null, null, null);
        AuditContext ctxQuiz = new AuditContext("0", "t1", "k1", "q1", null, null, null);

        sut.onMilestone(milestone, ctxMilestone);
        sut.onKnowledge(knowledge, ctxKnowledge);
        sut.onQuiz(quiz, ctxQuiz);

        Assertions.assertEquals(List.of(), sut.getResults());
    }
}
