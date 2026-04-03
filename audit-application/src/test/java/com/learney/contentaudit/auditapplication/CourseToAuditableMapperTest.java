package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.coursedomain.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourseToAuditableMapperTest {

    @Test
    @DisplayName("Given a course with quizzes, when map is called, then analyzeTokensBatch is invoked and returns an AuditableCourse")
    @Tag("F-NLP")
    @Tag("F-NLP-R010")
    public void givenACourseWithQuizzesWhenMapIsCalledThenAnalyzeTokensBatchIsInvokedAndReturnsAnAuditableCourse() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        NlpToken token = new NlpToken("cat", "cat", "NOUN", 0, false, false);
        when(nlpTokenizer.analyzeTokensBatch(anyList()))
                .thenReturn(Map.of("cat", List.of(token)));

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "cat", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setLabel("label");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t1");
        te.setLabel("label");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m1");
        me.setLabel("A1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer);
        AuditableCourse result = mapper.map(course);

        verify(nlpTokenizer).analyzeTokensBatch(anyList());
        assertNotNull(result);
        assertEquals(1, result.getMilestones().size());
    }

    @Test
    @DisplayName("Given a course with no milestones, when map is called, then returns an AuditableCourse without error")
    @Tag("F-NLP")
    @Tag("F-NLP-R010")
    public void givenACourseWithNoMilestonesWhenMapIsCalledThenReturnsAnAuditableCourseWithoutError() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        when(nlpTokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of());

        CourseEntity course = new CourseEntity();
        course.setRoot(new RootNodeEntity());

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer);
        AuditableCourse result = mapper.map(course);

        assertNotNull(result);
        assertTrue(result.getMilestones().isEmpty());
    }

    @Test
    @DisplayName("Given nlpTokenizer throws exception during batch processing, when map is called, then exception propagates")
    @Tag("F-NLP")
    @Tag("F-NLP-R008")
    public void givenNlpTokenizerThrowsExceptionDuringBatchProcessingWhenMapIsCalledThenExceptionPropagates() {
        NlpTokenizer nlpTokenizer = mock(NlpTokenizer.class);
        when(nlpTokenizer.analyzeTokensBatch(anyList()))
                .thenThrow(new RuntimeException("NLP failure"));

        SentencePartEntity part = new SentencePartEntity(SentencePartKind.TEXT, "cat", null);
        FormEntity form = new FormEntity(null, 0, null, null, List.of(part));
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId("q1");
        qt.setForm(form);
        KnowledgeEntity ke = new KnowledgeEntity();
        ke.setId("k1");
        ke.setQuizTemplates(List.of(qt));
        TopicEntity te = new TopicEntity();
        te.setId("t1");
        te.setKnowledges(List.of(ke));
        MilestoneEntity me = new MilestoneEntity();
        me.setId("m1");
        me.setTopics(List.of(te));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(me));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);

        CourseToAuditableMapper mapper = new CourseToAuditableMapper(nlpTokenizer);
        assertThrows(RuntimeException.class, () -> mapper.map(course));
    }
}
