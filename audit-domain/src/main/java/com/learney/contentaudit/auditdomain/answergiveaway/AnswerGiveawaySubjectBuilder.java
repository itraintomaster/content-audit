package com.learney.contentaudit.auditdomain.answergiveaway;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.auditdomain.studentview.StudentGapView;
import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.*;

/**
 * F-REGALA-R001: what the student sees before answering -- the name of the topic, the instructions of the exercise, the
 * statement with its hint and the options --, with the unit of the course as context and the accepted answers, which
 * the student does not see, so the judge knows the answer. Never the translation, the mini theory or the audio.
 */
public final class AnswerGiveawaySubjectBuilder {
    public static final String CONTRACT = "answer-giveaway-input-v1";
    private final QuizStudentViewRenderer renderer;

    public AnswerGiveawaySubjectBuilder(QuizStudentViewRenderer renderer) { this.renderer = Objects.requireNonNull(renderer); }

    public EvaluationSubject build(AuditNode node) {
        if (node == null || !(node.getEntity() instanceof AuditableQuiz quiz))
            throw new IllegalArgumentException("El juez de «no regala» juzga ejercicios");
        QuizStudentView view = renderer.render(quiz);
        if (view == null || view.getStatement() == null || view.getStatement().isBlank())
            throw new IllegalArgumentException("Falta el enunciado que ve el alumno");
        var content = new LinkedHashMap<String, String>();
        content.put("inputContract", CONTRACT);
        content.put("quizId", quiz.getId());
        content.put("level", label(node, AuditTarget.MILESTONE));
        content.put("topic", label(node, AuditTarget.TOPIC));
        content.put("title", text(quiz.getLabel()));
        content.put("instructions", text(quiz.getInstructions()));
        content.put("sentenceMode", node.ancestor(AuditTarget.KNOWLEDGE).map(AuditNode::getEntity)
                .filter(AuditableKnowledge.class::isInstance).map(AuditableKnowledge.class::cast)
                .map(AuditableKnowledge::getSentenceMode).map(Enum::name).orElse(""));
        content.put("exercise", view.getStatement());
        List<StudentOptionView> options = view.getOptions() == null ? List.of()
                : view.getOptions().stream().filter(Objects::nonNull).toList();
        content.put("visibleChoices", String.join("\n", options.stream().map(o -> "- " + text(o.getLabel())).toList()));
        content.put("acceptedAnswers", accepted(view.getGaps(), options));
        return new EvaluationSubject(quiz.getId(), content);
    }

    /** In a multiple choice, the correct options; otherwise one line per gap, «Hueco N: a | b». */
    private static String accepted(List<StudentGapView> gaps, List<StudentOptionView> options) {
        if (!options.isEmpty())
            return "Opción correcta: " + String.join(" | ", options.stream().filter(StudentOptionView::isCorrect)
                    .map(o -> text(o.getLabel())).toList());
        var lines = new ArrayList<String>();
        if (gaps != null) for (int i = 0; i < gaps.size(); i++) {
            var gap = gaps.get(i);
            var answers = gap == null || gap.getAcceptedAnswers() == null ? List.<String>of() : gap.getAcceptedAnswers();
            if (!answers.isEmpty()) lines.add("Hueco " + (i + 1) + ": " + String.join(" | ", answers));
        }
        return String.join("\n", lines);
    }

    private static String label(AuditNode node, AuditTarget target) {
        return node.ancestor(target).map(AuditNode::getEntity).map(entity -> {
            if (entity instanceof AuditableMilestone milestone) return text(milestone.getLabel()).strip();
            if (entity instanceof AuditableTopic topic) return text(topic.getLabel());
            return "";
        }).orElse("");
    }

    private static String text(String value) { return value == null ? "" : value; }
}
