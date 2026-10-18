package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003: where a verdict of a judge stands against the judge of today. {@link #UP_TO_DATE}
 * when today's version gave it with a runtime it approved its exam with; {@link #FROM_BEFORE}
 * otherwise, with its {@link JudgeVerdictStaleCause}; {@link #NOT_MEASURED} for an exercise or
 * topic the judge has no verdict on.
 */
public enum JudgeVerdictFreshness {
    UP_TO_DATE,
    FROM_BEFORE,
    NOT_MEASURED
}
