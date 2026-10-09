package com.learney.contentaudit.auditdomain.choicequality;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Collectors;

/** Its own fingerprint: student-visible content plus level and topic, never the later explanation. */
public final class ChoiceQualitySubjectBuilder {
    private final QuizStudentViewRenderer renderer;
    public ChoiceQualitySubjectBuilder(QuizStudentViewRenderer renderer) { this.renderer = renderer; }

    public EvaluationSubject build(AuditNode node) {
        if (node == null || !(node.getEntity() instanceof AuditableQuiz quiz)
                || quiz.getMultipleChoice() == null) {
            throw new IllegalArgumentException("Calidad de distractores juzga sólo opción múltiple");
        }
        var view = renderer.render(quiz);
        var content = new LinkedHashMap<String, String>();
        String level = node.ancestor(AuditTarget.MILESTONE)
                .map(AuditNode::getEntity).filter(AuditableMilestone.class::isInstance)
                .map(AuditableMilestone.class::cast)
                .map(m -> text(m.getLabel()).isBlank() ? text(m.getCode()) : text(m.getLabel()))
                .orElse("");
        String topic = node.ancestor(AuditTarget.KNOWLEDGE)
                .map(AuditNode::getEntity).filter(AuditableKnowledge.class::isInstance)
                .map(AuditableKnowledge.class::cast).map(k -> text(k.getTitle())).orElse("");
        content.put("level", level.toUpperCase(Locale.ROOT));
        content.put("topic", topic);
        content.put("title", text(quiz.getLabel()));
        content.put("instructions", text(quiz.getInstructions()));
        content.put("exercise", view != null ? text(view.getStatement()) : "");
        content.put("options", view != null && view.getOptions() != null
                ? view.getOptions().stream().map(ChoiceQualitySubjectBuilder::option).collect(Collectors.joining("\n"))
                : "");
        return new EvaluationSubject(quiz.getId(), content);
    }
    private static String option(StudentOptionView option) {
        if (option == null) throw new IllegalArgumentException("Opción vacía");
        return "- " + text(option.getLabel()) + (option.isCorrect() ? " (correcta)" : "");
    }
    private static String text(String text) { return text != null ? text : ""; }
}
