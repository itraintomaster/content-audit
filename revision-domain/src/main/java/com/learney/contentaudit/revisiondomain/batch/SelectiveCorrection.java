package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.auditdomain.judging.JudgeEnablement;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
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
            boolean includeApproved, int resultContentVersion, String resultFingerprint, String correctorRuntime,
            RunProcess process) {
        /** A run saved before F-CORTE-R005, or one that is not a measurement: no process recorded. */
        public Run(String id, String action, String analyzer, String sourceRunId,
                int correctionRevision, int contentVersion, String fingerprint, String judgeVersion,
                String runtime, String auditId, String planId, List<String> taskIds, List<String> quizIds,
                int queryCap, int reserved, int spent, int reached, int evaluated, int missing,
                String status, String reviewer, Instant createdAt, Instant finishedAt, String detail,
                boolean includeApproved, int resultContentVersion, String resultFingerprint, String correctorRuntime) {
            this(id, action, analyzer, sourceRunId, correctionRevision, contentVersion, fingerprint, judgeVersion, runtime,
                    auditId, planId, taskIds, quizIds, queryCap, reserved, spent, reached, evaluated, missing, status, reviewer,
                    createdAt, finishedAt, detail, includeApproved, resultContentVersion, resultFingerprint, correctorRuntime, null);
        }
    }
    /**
     * F-CORTE-R005: what a measurement leaves recorded before its first query, so that a cut can be reconciled
     * later: the process that runs it, the evaluators of its judge and the motor they consult with.
     */
    public record RunProcess(long pid, Instant startedAt, List<String> evaluators, EvaluationRuntimeIdentity runtime,
            BatchStatus idleStatus) {
        public RunProcess(long pid, Instant startedAt, List<String> evaluators, EvaluationRuntimeIdentity runtime) {
            this(pid, startedAt, evaluators, runtime, null);
        }
        public RunProcess {
            evaluators = evaluators != null ? List.copyOf(evaluators) : List.of();
        }
    }
    /**
     * F-CORTE-R006: an open measurement that holds every launch of the batch, named for whoever estimates: its judge,
     * when it started, its cap, how many verdicts it left saved ({@code null} when it did not record its evaluators)
     * and whether its process still runs.
     */
    public record OpenRun(String id, String action, String analyzer, String displayName, String status,
            Instant createdAt, int queryCap, Integer saved, boolean running) {}
    public record State(String batchId, int correctionRevision, int contentVersion, String analyzer,
            String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
            int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
            List<Task> tasks, List<Run> runs, String displayName, List<ReanalysisState> scopes,
            List<JudgeEnablement> enabledWith, List<OpenRun> openRuns) {
        /**
         * F-ALDIA-R007 inv. 3: {@code enabledWith} says with which motors the version of today is
         * enabled, so a launch with a motor it did not approve with says with which ones it could.
         * F-CORTE-R006: {@code openRuns} names the measurements that hold every launch of the batch.
         */
        public State {
            enabledWith = enabledWith != null ? List.copyOf(enabledWith) : List.of();
            openRuns = openRuns != null ? List.copyOf(openRuns) : List.of();
        }
        public State(String batchId, int correctionRevision, int contentVersion, String analyzer,
                String fingerprint, String judgeVersion, String runtime, boolean allowed, String reason,
                int reached, int evaluated, int missing, int pendingQueries, int reusedQueries,
                List<Task> tasks, List<Run> runs, String displayName, List<ReanalysisState> scopes,
                List<JudgeEnablement> enabledWith) {
            this(batchId, correctionRevision, contentVersion, analyzer, fingerprint, judgeVersion, runtime, allowed,
                    reason, reached, evaluated, missing, pendingQueries, reusedQueries, tasks, runs, displayName, scopes,
                    enabledWith, List.of());
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
