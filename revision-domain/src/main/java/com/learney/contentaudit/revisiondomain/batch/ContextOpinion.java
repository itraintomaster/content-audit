package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Measurements supplement a correction without closing it or changing any proposal. */
public final class ContextOpinion {
    private ContextOpinion() {}
    public record Request(String action, String batchId, Map<String, List<String>> selection,
            boolean wholeTopic, String operationId, Integer contentVersion, String fingerprint,
            Integer queryCap, String reviewer, String scope, String sourceRef) {
        public Request(String action, String batchId, Map<String, List<String>> selection,
                boolean wholeTopic, String operationId, Integer contentVersion, String fingerprint,
                Integer queryCap, String reviewer, String scope) {
            this(action, batchId, selection, wholeTopic, operationId, contentVersion, fingerprint, queryCap, reviewer, scope, null);
        }
        public Request(String action, String batchId, Map<String, List<String>> selection,
                boolean wholeTopic, String operationId, Integer contentVersion, String fingerprint,
                Integer queryCap, String reviewer) {
            this(action, batchId, selection, wholeTopic, operationId, contentVersion, fingerprint, queryCap, reviewer, null);
        }
    }
    public record Judge(String analyzer, String displayName, String unit, int quizzes, int topics,
            int pendingQueries, int reusedQueries, boolean allowed, String reason, String judgeVersion,
            String runtime, int selectedPendingQueries, int topicPendingQueries) {}
    public record Result(String analyzer, ChangeOutcome outcome) {}
    /** A sourced EXAM result, never a course judgment or proof that a task was repaired. */
    public record PreliminaryOpinion(String quizId, String analyzer, String evaluatorId, String judgeVersion,
            EvaluationRuntimeIdentity runtime, String namespace, String contentFingerprint, String payload,
            Instant recordedAt, String sourceRef, String sourceRecordHash, CourseElementSnapshot snapshot) {}
    public record Run(String id, Request request, int correctionRevision, int contentVersion,
            String fingerprint, int reserved, int spent, String status, Instant createdAt,
            Instant finishedAt, List<Judge> judges, List<RevisionProposal> proposals,
            List<Result> results, String detail, List<PreliminaryOpinion> preliminaryOpinions) {
        public Run(String id, Request request, int correctionRevision, int contentVersion,
                String fingerprint, int reserved, int spent, String status, Instant createdAt,
                Instant finishedAt, List<Judge> judges, List<RevisionProposal> proposals,
                List<Result> results, String detail) {
            this(id, request, correctionRevision, contentVersion, fingerprint, reserved, spent, status,
                    createdAt, finishedAt, judges, proposals, results, detail, List.of());
        }
        public Run {
            preliminaryOpinions = preliminaryOpinions == null ? List.of() : List.copyOf(preliminaryOpinions);
        }
    }
    /**
     * F-OPINA-R002: {@code pendingPairs} are the exercise-judge pairs of {@code selection} -- for «Completar opiniones»,
     * exactly what is missing -- and {@code pendingExercises} the distinct exercises among them; {@code pendingQueries}
     * is what is paid, apart: a judge of topics covers every pair of a topic with one query.
     */
    public record State(String batchId, int contentVersion, String fingerprint, boolean allowed,
            String reason, int pendingQueries, int reusedQueries, List<Judge> judges,
            Map<String, List<String>> selection, boolean wholeTopic, List<Run> runs, String scope,
            int pendingPairs, int pendingExercises) {
        public State(String batchId, int contentVersion, String fingerprint, boolean allowed,
                String reason, int pendingQueries, int reusedQueries, List<Judge> judges,
                Map<String, List<String>> selection, boolean wholeTopic, List<Run> runs, String scope) {
            this(batchId, contentVersion, fingerprint, allowed, reason, pendingQueries, reusedQueries, judges,
                    selection, wholeTopic, runs, scope, pairs(selection), exercises(selection));
        }
        public State(String batchId, int contentVersion, String fingerprint, boolean allowed,
                String reason, int pendingQueries, int reusedQueries, List<Judge> judges,
                Map<String, List<String>> selection, boolean wholeTopic, List<Run> runs) {
            this(batchId, contentVersion, fingerprint, allowed, reason, pendingQueries, reusedQueries, judges,
                    selection, wholeTopic, runs, null);
        }
        private static int pairs(Map<String, List<String>> selection) {
            return selection == null ? 0 : selection.values().stream().mapToInt(List::size).sum();
        }
        private static int exercises(Map<String, List<String>> selection) {
            return selection == null ? 0 : (int) selection.values().stream().flatMap(List::stream).distinct().count();
        }
    }
}
