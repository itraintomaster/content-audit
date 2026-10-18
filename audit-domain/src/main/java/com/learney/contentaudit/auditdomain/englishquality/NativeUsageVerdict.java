package com.learney.contentaudit.auditdomain.englishquality;

import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.List;

/** Formulation only: valid language and suitability for a beginner are separate decisions. */
public record NativeUsageVerdict(List<Assessment> assessments, String rationale) {
    public enum Status { PASS, FAIL, REVIEW, NOT_APPLICABLE }
    public enum Scope { SENTENCE, COMMUNICATIVE_ELLIPSIS, LEXICAL_PHRASE, LABEL_OR_FORM }
    public enum Issue { GRAMMAR, COLLOCATION, WORD_CHOICE, WORD_ORDER, CALQUE }
    public record Assessment(String sentence, Scope scope, Status status, String intendedMeaning,
            String troublesomeSpan, Issue issue, FindingSeverity severity, String reason,
            String neededEvidence, String registerConcern, String writingConcern) {
        public Assessment {
            required(sentence); required(reason);
            if (scope == null || status == null) throw new IllegalArgumentException("Falta alcance o estado");
            if (status == Status.FAIL) {
                required(troublesomeSpan);
                if (!sentence.contains(troublesomeSpan) || issue == null
                        || (severity != FindingSeverity.HIGH && severity != FindingSeverity.MEDIUM))
                    throw new IllegalArgumentException("Fallo sin fragmento literal, causa o gravedad válidos");
            } else if ((status != Status.REVIEW && troublesomeSpan != null) || issue != null || severity != null)
                throw new IllegalArgumentException("Sólo FAIL declara un defecto de formulación");
            if (status == Status.REVIEW && troublesomeSpan != null
                    && (troublesomeSpan.isBlank() || !sentence.contains(troublesomeSpan)))
                throw new IllegalArgumentException("El fragmento dudoso debe ser literal, sin declarar un defecto");
            if (status == Status.REVIEW) required(neededEvidence);
            else if (neededEvidence != null) throw new IllegalArgumentException("Sólo REVIEW solicita evidencia");
            if (status == Status.PASS || status == Status.FAIL) required(intendedMeaning);
            optional(intendedMeaning);
            if (status == Status.NOT_APPLICABLE && scope != Scope.LABEL_OR_FORM)
                throw new IllegalArgumentException("Una elipsis o frase solicitada no es una etiqueta");
            if (scope == Scope.LABEL_OR_FORM && status != Status.NOT_APPLICABLE)
                throw new IllegalArgumentException("No aprobar ni condenar etiquetas como oraciones");
            optional(registerConcern); optional(writingConcern);
        }
    }
    public NativeUsageVerdict {
        required(rationale);
        if (assessments == null || assessments.isEmpty()) throw new IllegalArgumentException("Sin evaluaciones");
        assessments = List.copyOf(assessments);
        if (assessments.stream().map(Assessment::sentence).distinct().count() != assessments.size())
            throw new IllegalArgumentException("Variante duplicada");
    }
    static void required(String s) { if (s == null || s.isBlank()) throw new IllegalArgumentException("Texto requerido vacío"); }
    static void optional(String s) { if (s != null && s.isBlank()) throw new IllegalArgumentException("Nota opcional vacía"); }
}
