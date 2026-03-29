package com.learney.contentaudit.auditdomain.lrec;

import com.learney.contentaudit.auditdomain.AuditContext;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.ContentWordFilter;
import com.learney.contentaudit.auditdomain.LemmaRecurrenceConfig;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.ScoredItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

/**
 * Handwritten integration test for LemmaRecurrenceAnalyzer.
 * Covers the full pipeline: onQuiz → onCourseComplete → getResults.
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

    @Test
    @DisplayName("Given two quizzes with content words, when full pipeline runs, then getResults returns scored item with correct score")
    void fullPipelineReturnsCorrectScore() {
        // Fixtures
        NlpToken catToken = new NlpToken("cat", "cat", "NOUN", 100, false, false);
        NlpToken theToken = new NlpToken("the", "the", "DET", 1, true, false);
        NlpToken catsToken = new NlpToken("cats", "cat", "NOUN", 100, false, false);
        AuditableQuiz quiz1 = new AuditableQuiz("the cat", List.of(theToken, catToken));
        AuditableQuiz quiz2 = new AuditableQuiz("the cats", List.of(theToken, catsToken));
        AuditContext ctx = new AuditContext("m1", "t1", "k1", "q1");
        AuditableCourse course = new AuditableCourse(List.of());

        // Mock contentWordFilter: NOUN=true, DET=false
        lenient().when(contentWordFilter.isContentWord(argThat(t -> t != null && "NOUN".equals(t.getPosTag())))).thenReturn(true);
        lenient().when(contentWordFilter.isContentWord(argThat(t -> t == null || !"NOUN".equals(t.getPosTag())))).thenReturn(false);

        // Process quizzes
        sut.onQuiz(quiz1, ctx);
        sut.onQuiz(quiz2, ctx);

        // Mock collaborators for onCourseComplete
        lenient().when(config.getTop()).thenReturn(2000);
        lenient().when(intervalCalculator.calculateMeanInterval(any())).thenReturn(100.0);
        lenient().when(intervalCalculator.calculateStdDevInterval(any(), anyDouble())).thenReturn(0.0);
        lenient().when(exposureClassifier.classify(anyDouble(), any())).thenReturn(ExposureStatus.NORMAL);

        sut.onCourseComplete(course, ctx);

        // Verify results
        List<ScoredItem> results = sut.getResults();
        assertEquals(1, results.size());
        ScoredItem item = results.get(0);
        assertEquals("lemma-recurrence", item.getAnalyzerName());
        assertEquals(AuditTarget.COURSE, item.getTarget());
        assertEquals(1.0, item.getScore(), 0.01);
    }
}
