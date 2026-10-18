package com.learney.contentaudit.auditdomain.judging;

import java.util.Optional;

/**
 * F-ALDIA-R003 and R005: where today's judge is read to mark the verdicts of an analysis, without
 * consulting anyone. Empty for a judge it does not know: its verdicts go unmarked rather than
 * marked against a guess.
 */
public interface JudgeFreshnessCatalog {
    Optional<JudgeFreshnessReference> reference(String judgeName);
}
