package com.learney.contentaudit.auditapplication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Maps a course with the real sentence converter, as analyze does. */
@Tag("multiple-choice")
class CourseToAuditableMapperMultipleChoiceTest {

    private static QuizTemplateEntity quiz(String id, FormEntity form, String sentence) {
        QuizTemplateEntity qt = new QuizTemplateEntity();
        qt.setId(id);
        qt.setKind(form.getKind());
        qt.setForm(form);
        qt.setSentences(List.of(sentence));
        return qt;
    }

    private static CourseEntity course(QuizTemplateEntity... quizzes) {
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId("k1");
        knowledge.setLabel("Be");
        knowledge.setQuizTemplates(List.of(quizzes));
        TopicEntity topic = new TopicEntity();
        topic.setId("t1");
        topic.setLabel("Present Simple");
        topic.setKnowledges(List.of(knowledge));
        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setId("m1");
        milestone.setLabel("A1");
        milestone.setTopics(List.of(topic));
        RootNodeEntity root = new RootNodeEntity();
        root.setMilestones(List.of(milestone));
        CourseEntity course = new CourseEntity();
        course.setRoot(root);
        return course;
    }

    private static List<AuditableQuiz> mappedQuizzes(AuditableCourse course) {
        return course.getMilestones().get(0).getTopics().get(0).getKnowledge().get(0).getQuizzes();
    }

    @Test
    @DisplayName("A course with multiple choice maps without failing: the MC quiz has no DSL, keeps its options and is measured on its materialized sentence")
    void mapsMultipleChoiceNextToCloze() {
        FormEntity multipleChoice = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null)), null, null);
        MultipleChoiceEntity options = new MultipleChoiceEntity("SINGLE", List.of(
                new MultipleChoiceItemEntity("am", 0.0, "am"),
                new MultipleChoiceItemEntity("is", 1.0, "is")));
        multipleChoice.setMultipleChoice(options);
        FormEntity cloze = new FormEntity("CLOZE", 1.0, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "They", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("are")),
                new SentencePartEntity(SentencePartKind.TEXT, "here.", null)), null, null);

        NlpTokenizer tokenizer = mock(NlpTokenizer.class);
        NlpToken she = new NlpToken("She", "she", "PRON", 0, true, false);
        when(tokenizer.analyzeTokensBatch(anyList())).thenReturn(Map.of("She is English.", List.of(she)));
        CourseToAuditableMapper mapper = new CourseToAuditableMapper(tokenizer, DefaultQuizSentenceConverter.create());

        List<AuditableQuiz> quizzes = mappedQuizzes(mapper.map(course(
                quiz("mc", multipleChoice, "She is English."),
                quiz("cloze", cloze, "They are here."))));

        AuditableQuiz mapped = quizzes.get(0);
        assertNull(mapped.getQuizSentence());
        assertSame(options, mapped.getMultipleChoice());
        assertEquals(List.of("She is English."), mapped.getSentences());
        assertEquals(List.of(she), mapped.getTokens());

        AuditableQuiz clozeMapped = quizzes.get(1);
        assertEquals("They ____ [are] here.", clozeMapped.getQuizSentence());
        assertNull(clozeMapped.getMultipleChoice());
    }
}
