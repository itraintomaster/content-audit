package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003 inv. 6: why a judge left a saved verdict without a score. An abstention is a verdict: it carries
 * its mark like any other and is never «sin medir»; measuring again without forcing reuses it.
 */
public enum JudgeAbstention {
    /** The judge says its criterion does not apply: there is no sentence to judge. */
    NOT_APPLICABLE("no aplica"),
    /** The judge cannot decide without context that the exercise does not give. */
    NEEDS_CONTEXT("falta contexto"),
    /** The judge leaves it for an editorial review. */
    REVIEW("revisar");

    private final String label;

    JudgeAbstention(String label) {
        this.label = label;
    }

    /** How the reports and the board say it. */
    public String label() {
        return label;
    }
}
