package com.learney.contentaudit.auditdomain.lrec;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.ContentWordFilter;
import com.learney.contentaudit.auditdomain.LemmaRecurrenceConfig;
import com.learney.contentaudit.auditdomain.NlpToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

/**
 * Handwritten integration test for LemmaRecurrenceAnalyzer.
 * Covers the full pipeline: onQuiz → onCourseComplete → scores on rootNode.
 *
 * This test exists because the declarative DSL cannot express:
 * - Mockito.anyDouble() for primitive double parameters
 * - Enum thenReturn values (ExposureStatus.NORMAL vs "NORMAL")
 */
@Tag("F-LREC")
class LemmaRecurrenceAnalyzerIntegrationTest {

    private ContentWordFilter contentWordFilter;
    private LemmaRecurrenceConfig config;
    private IntervalCalculator intervalCalculator;
    private ExposureClassifier exposureClassifier;
    private LemmaRecurrenceAnalyzer sut;

    @BeforeEach
    void setUp() {
        contentWordFilter = Mockito.mock(ContentWordFilter.class);
        config = Mockito.mock(LemmaRecurrenceConfig.class);
        intervalCalculator = Mockito.mock(IntervalCalculator.class);
        exposureClassifier = Mockito.mock(ExposureClassifier.class);
        sut = new LemmaRecurrenceAnalyzer(contentWordFilter, config, intervalCalculator, exposureClassifier);
    }

    // -- helpers --

    private AuditNode buildQuizNode(AuditableQuiz quiz) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    @Test
    @DisplayName("Given two quizzes with content words, when full pipeline runs, then scores contain correct score on root node")
    void fullPipelineReturnsCorrectScore() {
        // Fixtures
        NlpToken catToken = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        NlpToken theToken = new NlpToken("the", "the", "DET", 1, true, false);
        NlpToken catsToken = new NlpToken("cats", "cat", "NOUN", 100, false, false);
        AuditableQuiz quiz1 = new AuditableQuiz(List.of(theToken, catToken), null, null, null, null, List.of("the cat"), null);
        AuditableQuiz quiz2 = new AuditableQuiz(List.of(theToken, catsToken), null, null, null, null, List.of("the cats"), null);
        AuditNode quizNode1 = buildQuizNode(quiz1);
        AuditNode quizNode2 = buildQuizNode(quiz2);
        AuditNode courseNode = buildCourseNode();

        // Mock contentWordFilter: NOUN=true, DET=false
        lenient().when(contentWordFilter.isContentWord(argThat(t -> t != null && "NOUN".equals(t.getPosTag())))).thenReturn(true);
        lenient().when(contentWordFilter.isContentWord(argThat(t -> t == null || !"NOUN".equals(t.getPosTag())))).thenReturn(false);

        // Process quizzes
        sut.onQuiz(quizNode1);
        sut.onQuiz(quizNode2);

        // Mock collaborators for onCourseComplete
        lenient().when(config.getTop()).thenReturn(2000);
        lenient().when(intervalCalculator.calculateMeanInterval(any())).thenReturn(100.0);
        lenient().when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        lenient().when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        sut.onCourseComplete(courseNode);

        // Verify score written to root node
        assertTrue(courseNode.getScores().containsKey("lemma-recurrence"));
        assertEquals(1.0, courseNode.getScores().get("lemma-recurrence"), 0.01);
    }
}
