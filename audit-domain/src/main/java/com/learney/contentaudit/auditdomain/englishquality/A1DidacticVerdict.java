package com.learney.contentaudit.auditdomain.englishquality;

import java.util.List;

/** Teaching quality never changes whether a student's idiomatic variant is valid. */
public record A1DidacticVerdict(String quizId, Status status, String targetStructure,
        List<String> affectedSentences, Issue issue, String reason, String neededEvidence,
        List<Owner> handoffs, String proposedAction, String proposedExample) {
    public enum Status { SUITABLE, IMPROVE, REWRITE_REQUIRED, REVIEW, NOT_APPLICABLE }
    public enum Issue { TARGET_ALIGNMENT, OPAQUE_EXAMPLE, INCIDENTAL_STRUCTURE, SPECIAL_SENSE_OR_REGISTER, MODIFIER_SCOPE }
    public enum Owner { NATURALNESS, PRAGMATISM, FORMAT, INSTRUCTION_OR_ANSWER, INPUT }
    public A1DidacticVerdict {
        NativeUsageVerdict.required(quizId); NativeUsageVerdict.required(reason);
        if (status == null || affectedSentences == null || handoffs == null)
            throw new IllegalArgumentException("Veredicto didáctico incompleto");
        affectedSentences = List.copyOf(affectedSentences); handoffs = List.copyOf(handoffs);
        if (affectedSentences.stream().distinct().count() != affectedSentences.size()
                || handoffs.stream().distinct().count() != handoffs.size())
            throw new IllegalArgumentException("Evidencia duplicada");
        if (status != Status.NOT_APPLICABLE) NativeUsageVerdict.required(targetStructure);
        if (status == Status.IMPROVE || status == Status.REWRITE_REQUIRED) {
            if (issue == null && handoffs.isEmpty()) throw new IllegalArgumentException("Mejora sin causa ni responsable");
            NativeUsageVerdict.required(proposedAction);
        } else if (status != Status.REVIEW && issue != null)
            throw new IllegalArgumentException("Estado aprobado con causa de defecto");
        if (status == Status.REVIEW) NativeUsageVerdict.required(neededEvidence);
        else if (neededEvidence != null) throw new IllegalArgumentException("Sólo REVIEW solicita evidencia");
        if ((status == Status.SUITABLE || status == Status.NOT_APPLICABLE)
                && (proposedAction != null || proposedExample != null))
            throw new IllegalArgumentException("Estado aprobado no propone sustituir el ejercicio");
        NativeUsageVerdict.optional(proposedExample);
        NativeUsageVerdict.optional(proposedAction);
        NativeUsageVerdict.optional(targetStructure);
    }
}
