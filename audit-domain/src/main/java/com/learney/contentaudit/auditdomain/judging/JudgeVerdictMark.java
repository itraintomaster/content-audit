package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003: the mark a verdict carries wherever it goes. The cause is null unless the verdict
 * is {@link JudgeVerdictFreshness#FROM_BEFORE}.
 */
public record JudgeVerdictMark(JudgeVerdictFreshness freshness, JudgeVerdictStaleCause cause) {
    public static final JudgeVerdictMark UP_TO_DATE = new JudgeVerdictMark(JudgeVerdictFreshness.UP_TO_DATE, null);

    public static final JudgeVerdictMark NOT_MEASURED = new JudgeVerdictMark(JudgeVerdictFreshness.NOT_MEASURED, null);

    public static JudgeVerdictMark fromBefore(JudgeVerdictStaleCause cause) {
        return new JudgeVerdictMark(JudgeVerdictFreshness.FROM_BEFORE, cause);
    }

    public boolean isUpToDate() {
        return freshness == JudgeVerdictFreshness.UP_TO_DATE;
    }
}
