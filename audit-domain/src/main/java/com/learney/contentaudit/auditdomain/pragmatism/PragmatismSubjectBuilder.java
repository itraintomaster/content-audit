package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Only completed accepted sentences and visible context; never a later translation or theory. */
public final class PragmatismSubjectBuilder {
    private final QuizStudentViewRenderer renderer;
    public PragmatismSubjectBuilder(QuizStudentViewRenderer renderer) { this.renderer = Objects.requireNonNull(renderer); }
    public EvaluationSubject build(AuditNode node) {
        if (node == null || !(node.getEntity() instanceof AuditableQuiz quiz))
            throw new IllegalArgumentException("Pragmatismo juzga ejercicios");
        var sentences = sentences(quiz);
        if (sentences.isEmpty()) throw new IllegalArgumentException("Falta la oración terminada aceptada");
        var content = new LinkedHashMap<String, String>();
        content.put("level", node.ancestor(AuditTarget.MILESTONE).map(AuditNode::getEntity)
                .filter(AuditableMilestone.class::isInstance).map(AuditableMilestone.class::cast)
                .map(m -> text(m.getLabel())).orElse(""));
        content.put("topic", node.ancestor(AuditTarget.TOPIC).map(AuditNode::getEntity)
                .filter(AuditableTopic.class::isInstance).map(AuditableTopic.class::cast)
                .map(t -> text(t.getLabel())).orElse(""));
        content.put("title", text(quiz.getLabel()));
        content.put("instructions", text(quiz.getInstructions()));
        var view = renderer.render(quiz);
        content.put("exercise", view != null ? text(view.getStatement()) : "");
        content.put("acceptedSentences", sentences.stream().map(PragmatismSubjectBuilder::quote)
                .collect(Collectors.joining(",", "[", "]")));
        return new EvaluationSubject(quiz.getId(), content);
    }
    public static List<String> sentences(AuditableQuiz quiz) {
        return quiz.getSentences() == null ? List.of() : quiz.getSentences().stream()
                .filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().toList();
    }
    private static String text(String value) { return value == null ? "" : value; }
    private static String quote(String value) {
        var out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
