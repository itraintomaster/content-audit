package com.learney.contentaudit.auditdomain.judging;

import java.time.Instant;

/**
 * F-ALDIA-R005: a part of what a judge has from before -- by cause, and by the version or the runtime
 * that gave it -- with its name (F-ALDIA-R004) and its full code, so it can be looked up.
 */
public record JudgeStaleGroup(JudgeVerdictStaleCause cause, String judgeVersion, String versionName,
        Instant firstApprovedAt, String runtime, String runtimeName, int count) {
}
