package com.learney.contentaudit.auditdomain.pragmatism;

import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import java.util.List;

/** Communicative usefulness is independent of whether the wording is idiomatic. */
public record PragmatismVerdict(List<Assessment> assessments, String rationale) {
    public enum Status { PASS, FAIL, NEEDS_CONTEXT, NOT_APPLICABLE }
    public enum Issue {
        INCOHERENT_SCENARIO("incoherent-scenario"),
        UNLICENSED_SCENARIO("unlicensed-scenario"),
        DISCOURSE_MISMATCH("discourse-mismatch");
        private final String rule;
        Issue(String rule) { this.rule = rule; }
        public String rule() { return rule; }
    }
    public record Assessment(String sentence, Status status, String communicativePurpose,
            String plausibleUse, Issue issue, FindingSeverity severity, String reason, String neededContext) {
        public Assessment {
            if (sentence == null || sentence.isBlank() || status == null || communicativePurpose == null
                    || reason == null || reason.isBlank()) throw new IllegalArgumentException("Evaluación pragmática incompleta");
            if ((status == Status.FAIL) != (issue != null && severity != null)
                    || (status != Status.FAIL && (issue != null || severity != null))
                    || severity == FindingSeverity.BLOCKING || severity == FindingSeverity.LOW)
                throw new IllegalArgumentException("Estado y problema pragmático incoherentes");
            if ((status == Status.PASS) != (plausibleUse != null && !plausibleUse.isBlank())
                    || (status != Status.PASS && plausibleUse != null))
                throw new IllegalArgumentException("Sólo PASS declara un uso plausible");
            if ((status == Status.NEEDS_CONTEXT) != (neededContext != null && !neededContext.isBlank())
                    || (status != Status.NEEDS_CONTEXT && neededContext != null))
                throw new IllegalArgumentException("Sólo NEEDS_CONTEXT declara contexto faltante");
            if ((status == Status.PASS || status == Status.FAIL) && communicativePurpose.isBlank())
                throw new IllegalArgumentException("Falta la intención comunicativa evaluada");
        }
    }
    public PragmatismVerdict {
        if (assessments == null || assessments.isEmpty() || rationale == null || rationale.isBlank())
            throw new IllegalArgumentException("Veredicto pragmático incompleto");
        assessments = List.copyOf(assessments);
        if (assessments.stream().map(Assessment::sentence).distinct().count() != assessments.size())
            throw new IllegalArgumentException("Oración evaluada más de una vez");
    }
}
