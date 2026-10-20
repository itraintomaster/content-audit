package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003: why a verdict is from before. A verdict of today's version is from before only by
 * its runtime. One of an earlier version is from before by its version whenever some version of the
 * judge approved with its runtime; only a runtime no version approved with is another runtime (inv. 7).
 */
public enum JudgeVerdictStaleCause {
    /**
     * Today's version gave it with a runtime it did not approve with, or an earlier version gave it
     * with a runtime no version of the judge approved with.
     */
    OTHER_RUNTIME,
    /** Another version gave it, with a runtime some version of the judge approved with. */
    EARLIER_VERSION,
    /** It carries no runtime: nothing says who gave it (F-JEXAM-R014). */
    UNKNOWN_PROVENANCE,
    /** Today's version did not approve its exam: nothing of this judge is up to date (inv. 4). */
    JUDGE_NOT_APPROVED
}
