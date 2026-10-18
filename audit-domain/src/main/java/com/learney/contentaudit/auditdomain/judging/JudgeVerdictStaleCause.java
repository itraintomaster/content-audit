package com.learney.contentaudit.auditdomain.judging;

/**
 * F-ALDIA-R003: why a verdict is from before. The runtime is looked at first, because a different
 * runtime is not fixed by a version.
 */
public enum JudgeVerdictStaleCause {
    /** Its runtime is none that today's version approved its exam with. */
    OTHER_RUNTIME,
    /** Its runtime is one of those, and another version gave it. */
    EARLIER_VERSION,
    /** It carries no runtime: nothing says who gave it (F-JEXAM-R014). */
    UNKNOWN_PROVENANCE,
    /** Today's version did not approve its exam: nothing of this judge is up to date (inv. 4). */
    JUDGE_NOT_APPROVED
}
