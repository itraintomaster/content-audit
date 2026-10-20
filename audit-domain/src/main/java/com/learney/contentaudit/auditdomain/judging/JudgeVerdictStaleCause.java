package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003: why a verdict is from before. Only the version decides: the motor that gave a
 * verdict never makes it from before (inv. 7).
 */
public enum JudgeVerdictStaleCause {
    /**
     * No longer given (F-ALDIA-R003 inv. 7, 20/10): the motor does not decide whether a verdict is
     * from before. Kept so that an analysis read before that day is still understood.
     */
    OTHER_RUNTIME,
    /** Another version than today's gave it, with whatever motor. */
    EARLIER_VERSION,
    /** It carries no version or no runtime: nothing says who gave it (F-JEXAM-R014). */
    UNKNOWN_PROVENANCE,
    /** Today's version did not approve its exam: nothing of this judge is up to date (inv. 4). */
    JUDGE_NOT_APPROVED
}
