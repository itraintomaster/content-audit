package com.learney.contentaudit.auditdomain.quizinstructionengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionSubjectView;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("multiple-choice")
class DefaultQuizInstructionSubjectBuilderMultipleChoiceTest {

    private final DefaultQuizInstructionSubjectBuilder builder = new DefaultQuizInstructionSubjectBuilder();

    private static AuditNode node(AuditTarget target, Object entity, AuditNode parent) {
        AuditNode node = new AuditNode();
        node.setTarget(target);
        node.setEntity((com.learney.contentaudit.auditdomain.AuditableEntity) entity);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        if (parent != null) {
            parent.getChildren().add(node);
        }
        return node;
    }

    private static String judgedQuiz(DefaultQuizInstructionSubjectBuilder builder, AuditableQuiz quiz) {
        AuditNode course = node(AuditTarget.COURSE, null, null);
        AuditNode milestone = node(AuditTarget.MILESTONE, new AuditableMilestone(List.of(), null, "A1", null), course);
        AuditNode topic = node(AuditTarget.TOPIC, new AuditableTopic(List.of(), null, null, null), milestone);
        AuditableKnowledge knowledge = new AuditableKnowledge(List.of(), "Be", "Elige la forma de be.", true,
                "k1", "Be", "K", null, "Present Simple");
        AuditNode knowledgeNode = node(AuditTarget.KNOWLEDGE, knowledge, topic);
        AuditNode quizNode = node(AuditTarget.QUIZ, quiz, knowledgeNode);
        return builder.build(quizNode).getContent().get("quiz");
    }

    private static List<SentencePartEntity> stem(List<String> gapOptions) {
        return List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", gapOptions),
                new SentencePartEntity(SentencePartKind.TEXT, "English.", null));
    }

    private static AuditableQuiz quiz(List<SentencePartEntity> parts) {
        return new AuditableQuiz(List.of(), "q1", "Be", "", null, List.of("She is English."), null,
                "Elige la forma de be.", parts);
    }

    private static MultipleChoiceEntity choices(String correct) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (String label : List.of("am", "is", "are")) {
            items.add(new MultipleChoiceItemEntity(label, label.equals(correct) ? 1.0 : 0.0, label));
        }
        return new MultipleChoiceEntity("SINGLE", items);
    }

    @Test
    @DisplayName("The CLOZE render is pinned: it must not move, or every verdict already paid for stops being reused")
    void clozeRenderIsPinned() {
        assertEquals("TEXT:She:|CLOZE::is|TEXT:English.:|", judgedQuiz(builder, quiz(stem(List.of("is")))));
    }

    @Test
    @DisplayName("A quiz with an optionless gap and no multiple-choice payload still renders the CLOZE way")
    void optionlessGapWithoutPayloadRendersAsBefore() {
        assertEquals("TEXT:She:|CLOZE::|TEXT:English.:|", judgedQuiz(builder, quiz(stem(null))));
    }

    @Test
    @DisplayName("A multiple-choice quiz shows the judge every option, in display order, with the correct one marked")
    void multipleChoiceRenderShowsOptionsAndCorrect() {
        AuditableQuiz quiz = quiz(stem(null));
        quiz.setMultipleChoice(choices("is"));
        assertEquals("TEXT:She:|MULTIPLE_CHOICE::am,is[CORRECT],are|TEXT:English.:|", judgedQuiz(builder, quiz));
    }

    @Test
    @DisplayName("Two multiple-choice quizzes that differ only in the correct option get different judged content")
    void correctOptionEntersTheFingerprint() {
        AuditableQuiz first = quiz(stem(null));
        first.setMultipleChoice(choices("is"));
        AuditableQuiz second = quiz(stem(null));
        second.setMultipleChoice(choices("am"));
        assertNotEquals(judgedQuiz(builder, first), judgedQuiz(builder, second));
    }

    @Test
    @DisplayName("The revalidation and candidate path renders a multiple-choice quiz exactly as the analysis does")
    void viewPathRendersMultipleChoiceLikeTheAnalysis() {
        FormEntity form = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", stem(null));
        form.setMultipleChoice(choices("is"));
        QuizTemplateEntity template = new QuizTemplateEntity();
        template.setId("q1");
        template.setForm(form);
        AuditableQuiz audited = quiz(stem(null));
        audited.setMultipleChoice(choices("is"));

        QuizInstructionSubjectView view = new DefaultQuizInstructionSubjectViewFactory()
                .fromQuiz(template, "A1", "Present Simple", "Be", "Elige la forma de be.");

        assertEquals(judgedQuiz(builder, audited), builder.buildFromView(view).getContent().get("quiz"));
    }
}
