package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.auditdomain.judging.JudgeEnablement;
import java.time.Instant;
import java.util.List;

/** A targeted supplement to the same correction, never a replacement for its aggregate analyses. */
public final class SelectiveCorrection {
    private SelectiveCorrection() {}
    public record Request(String action, String batchId, String analyzer, String operationId,
            String sourceRunId, Integer contentVersion, String fingerprint, String judgeVersion,
            Integer queryCap, List<String> taskIds, boolean includeApproved, String reviewer, String state, boolean force) {
        public Request(String action, String batchId, String analyzer, String operationId, String sourceRunId,
                Integer contentVersion, String fingerprint, String judgeVersion, Integer queryCap,
                List<String> taskIds, boolean includeApproved, String reviewer) {
            this(action, batchId, analyzer, operationId, sourceRunId, contentVersion, fingerprint, judgeVersion,
                    queryCap, taskIds, includeApproved, reviewer, "correction", false);
        }
    }
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
            List<Task> tasks, List<Run> runs, String displayName, List<ReanalysisState> scopes,
            List<JudgeEnablement> enabledWith) {
        /**
         * F-ALDIA-R007 inv. 3: {@code enabledWith} says with which motors the version of today is
         * enabled, so a launch with a motor it did not approve with says with which ones it could.
         */
        public State {
            enabledWith = enabledWith != null ? List.copyOf(enabledWith) : List.of();
        }
        public State(String batchId, int correctionRevision, int contentVersion, String analyzer,
                String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
                int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
                List<Task> tasks, List<Run> runs, String displayName, List<ReanalysisState> scopes) {
            this(batchId, correctionRevision, contentVersion, analyzer, fingerprint, judgeVersion, runtime, allowed,
                    reason, reached, evaluated, missing, pendingQueries, reusedQueries, tasks, runs, displayName, scopes,
                    List.of());
        }
        public State(String batchId, int correctionRevision, int contentVersion, String analyzer,
                String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
                int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
                List<Task> tasks, List<Run> runs, String displayName) {
            this(batchId, correctionRevision, contentVersion, analyzer, fingerprint, judgeVersion, runtime, allowed,
                    reason, reached, evaluated, missing, pendingQueries, reusedQueries, tasks, runs, displayName, List.of());
        }
        public State(String batchId, int correctionRevision, int contentVersion, String analyzer,
                String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
                int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
                List<Task> tasks, List<Run> runs) {
            this(batchId, correctionRevision, contentVersion, analyzer, fingerprint, judgeVersion, runtime,
                    allowed, reason, reached, evaluated, missing, pendingQueries, reusedQueries, tasks, runs, analyzer, List.of());
        }
    }
    public record ReanalysisState(String state, int quizzes, int queries, int sharedQuizzes) {}
}
