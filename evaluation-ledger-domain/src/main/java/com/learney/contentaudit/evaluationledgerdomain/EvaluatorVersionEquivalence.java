package com.learney.contentaudit.evaluationledgerdomain;

import java.time.Instant;
import java.util.Objects;

/**
 * F-ALDIA-R002: an earlier version of an evaluator that is, once and in writing, the same version as
 * its current one. It is not general compatibility between definitions: it bridges exactly one
 * change of how the version is counted, recorded with the evaluator, both versions, the moment and
 * why.
 */
public final class EvaluatorVersionEquivalence {
    private final String evaluatorId;

    private final String earlierVersion;

    private final String currentVersion;

    private final Instant recordedAt;

    private final String reason;

    public EvaluatorVersionEquivalence(String evaluatorId, String earlierVersion, String currentVersion,
            Instant recordedAt, String reason) {
        this.evaluatorId = evaluatorId;
        this.earlierVersion = earlierVersion;
        this.currentVersion = currentVersion;
        this.recordedAt = recordedAt;
        this.reason = reason;
    }

    public String getEvaluatorId() {
        return evaluatorId;
    }

    public String getEarlierVersion() {
        return earlierVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluatorVersionEquivalence that)) return false;
        return Objects.equals(evaluatorId, that.evaluatorId)
                && Objects.equals(earlierVersion, that.earlierVersion)
                && Objects.equals(currentVersion, that.currentVersion)
                && Objects.equals(recordedAt, that.recordedAt)
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evaluatorId, earlierVersion, currentVersion, recordedAt, reason);
    }

    @Override
    public String toString() {
        return evaluatorId + ": " + earlierVersion + " = " + currentVersion + " (" + recordedAt + ")";
    }
}
