package com.learney.contentaudit.auditdomain.answergiveaway;

import java.util.List;

/**
 * F-REGALA-R002: whether something the student reads outside the sentence itself -- the instructions of the exercise or
 * a direction written in the statement -- makes the decision the topic practices. {@code givenAway} is the fragment
 * copied as it is from the field {@code where} says; the hint by the gap and the options belong to other judges and
 * only travel as {@code handoffs}.
 */
public record AnswerGiveawayVerdict(String quizId, Status status, String practicedDecision, Where where,
        String givenAway, Extent extent, String reason, String proposedAction, List<Handoff> handoffs) {
    public enum Status { OK, GIVES_AWAY, REVIEW, NOT_APPLICABLE }
    public enum Where { INSTRUCTIONS, STATEMENT }
    public enum Extent { WHOLE, PART }
    public enum Handoff { HINT, OPTIONS }

    public AnswerGiveawayVerdict {
        if (blank(quizId) || status == null || blank(reason) || handoffs == null)
            throw new IllegalArgumentException("Veredicto de «no regala» incompleto");
        handoffs = List.copyOf(handoffs);
        if (handoffs.stream().distinct().count() != handoffs.size())
            throw new IllegalArgumentException("Traspaso repetido");
        if (status != Status.NOT_APPLICABLE && blank(practicedDecision))
            throw new IllegalArgumentException("Falta qué decide el alumno");
        if (status == Status.GIVES_AWAY) {
            if (where == null || blank(givenAway) || extent == null || blank(proposedAction))
                throw new IllegalArgumentException("Regala sin decir dónde, qué, cuánto ni cómo arreglarlo");
        } else if (where != null || givenAway != null || extent != null || proposedAction != null)
            throw new IllegalArgumentException("Un estado que no regala trae lugar, fragmento, alcance o propuesta");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
