package com.learney.contentaudit.revisiondomain.batch;

import java.time.Instant;
import java.util.List;

/** A targeted supplement to the same correction, never a replacement for its aggregate analyses. */
public final class SelectiveCorrection {
    private SelectiveCorrection() {}
    public record Request(String action, String batchId, String analyzer, String operationId,
            String sourceRunId, Integer contentVersion, String fingerprint, String judgeVersion,
            Integer queryCap, List<String> taskIds, boolean includeApproved, String reviewer) {}
    public record Task(String id, String quizId, String rule, String explanation, boolean approved,
            boolean attempted, String outcome) {}
    public record Run(String id, String action, String analyzer, String sourceRunId,
            int correctionRevision, int contentVersion, String fingerprint, String judgeVersion,
            String runtime, String auditId, String planId, List<String> taskIds, List<String> quizIds,
            int queryCap, int reserved, int spent, int reached, int evaluated, int missing,
            String status, String reviewer, Instant createdAt, Instant finishedAt, String detail,
            boolean includeApproved, int resultContentVersion, String resultFingerprint, String correctorRuntime) {}
    public record State(String batchId, int correctionRevision, int contentVersion, String analyzer,
            String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
            int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
            List<Task> tasks, List<Run> runs) {}
}
