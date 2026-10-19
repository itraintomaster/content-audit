package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.auditdomain.AuditTarget;
import java.time.Instant;
import java.util.List;

/**
 * F-ALDIA-R005: for a judge in a state, how many of its exercises (or topics) are up to date, from
 * before and not measured -- the three add up to {@code total} -- and today's version with its name
 * and the instant it first approved. {@code summary} is the line the reports print, and {@code detail}
 * the same without the judge, for a line that already names it. {@code abstained} says how many of the
 * up to date and from before are abstentions (inv. 5), by reason in {@code abstentions}; they are never
 * not measured (F-ALDIA-R003 inv. 6).
 */
public record JudgeFreshnessCount(String judgeName, String displayName, AuditTarget unit, int total, int upToDate,
        int fromBefore, int notMeasured, String currentVersion, String currentVersionName, Instant currentApprovedAt,
        boolean approved, List<JudgeStaleGroup> fromBeforeGroups, String summary, String detail, int abstained,
        List<JudgeAbstentionGroup> abstentions) {

    /** A count without abstentions. */
    public JudgeFreshnessCount(String judgeName, String displayName, AuditTarget unit, int total, int upToDate,
            int fromBefore, int notMeasured, String currentVersion, String currentVersionName, Instant currentApprovedAt,
            boolean approved, List<JudgeStaleGroup> fromBeforeGroups, String summary, String detail) {
        this(judgeName, displayName, unit, total, upToDate, fromBefore, notMeasured, currentVersion, currentVersionName,
                currentApprovedAt, approved, fromBeforeGroups, summary, detail, 0, List.of());
    }
}
