package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.auditdomain.finding.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.util.List;

/** Explicitly budgeted new versions of selected quizzes, inside the existing correction. */
public final class QuizCorrection {
    private QuizCorrection() {}

    public record Request(String action, String batchId, String operationId, Integer contentVersion,
            String fingerprint, Integer queryCap, List<String> quizIds, boolean includeApproved, String reviewer,
            String sourceOperationId, boolean restoreRequestFormula) {
        public Request(String action, String batchId, String operationId, Integer contentVersion,
                String fingerprint, Integer queryCap, List<String> quizIds, boolean includeApproved, String reviewer,
                String sourceOperationId) {
            this(action, batchId, operationId, contentVersion, fingerprint, queryCap, quizIds, includeApproved, reviewer, sourceOperationId, false);
        }
        public Request(String action, String batchId, String operationId, Integer contentVersion,
                String fingerprint, Integer queryCap, List<String> quizIds, boolean includeApproved, String reviewer) {
            this(action, batchId, operationId, contentVersion, fingerprint, queryCap, quizIds, includeApproved, reviewer, null, false);
        }
    }

    public record Quiz(String quizId, boolean approved, boolean correctable, String reason, List<Finding> findings) {}

    /** Frozen comparison references, captured before a paid re-correction can change the quiz. */
    public record ComparisonBaseline(String operationId, int sourceContentVersion, String quizId,
            CourseElementSnapshot before, CourseElementSnapshot original,
            com.learney.contentaudit.revisiondomain.panel.DidacticReference didacticReference) {
        public ComparisonBaseline(String operationId, int sourceContentVersion, String quizId,
                CourseElementSnapshot before, CourseElementSnapshot original) {
            this(operationId, sourceContentVersion, quizId, before, original, null);
        }
        private static final ObjectMapper JSON = new ObjectMapper();
        public ComparisonBaseline {
            before = copy(before);
            original = copy(original);
        }
        // Course entities are mutable. Neither a later panel nor a projection reader may rewrite history.
        @Override public CourseElementSnapshot before() { return copy(before); }
        @Override public CourseElementSnapshot original() { return copy(original); }
        private static CourseElementSnapshot copy(CourseElementSnapshot snapshot) {
            return snapshot == null ? null : JSON.convertValue(snapshot, CourseElementSnapshot.class);
        }
    }

    public record State(String batchId, int correctionRevision, int contentVersion, String fingerprint,
            boolean allowed, String reason, List<Quiz> quizzes, int selectedQuizzes, int minimumQueries,
            String estimateNote, List<String> validationAnalyzers, String correctorRuntime,
            String runId, String status, int spent, List<SelectiveCorrection.Run> runs, Comparison comparison) {
        public State(String batchId, int correctionRevision, int contentVersion, String fingerprint,
                boolean allowed, String reason, List<Quiz> quizzes, int selectedQuizzes, int minimumQueries,
                String estimateNote, List<String> validationAnalyzers, String correctorRuntime,
                String runId, String status, int spent, List<SelectiveCorrection.Run> runs) {
            this(batchId, correctionRevision, contentVersion, fingerprint, allowed, reason, quizzes, selectedQuizzes,
                    minimumQueries, estimateNote, validationAnalyzers, correctorRuntime, runId, status, spent, runs, null);
        }
    }

    /** Read-only comparison. The before report belongs to this operation, never to the course original. */
    public record Comparison(String batchId, String operationId, int sourceContentVersion, int resultContentVersion,
            int currentContentVersion, boolean matchesResultVersion, String beforeAuditId,
            String sourceLabel, String afterLabel, List<String> warnings, List<QuizEvolution> quizzes) {}
    public record QuizEvolution(String quizId, List<ProblemEvolution> rows) {}
    /** Stable finding identities are matched; ambiguous identities are grouped by rule/node, never by evidence text. */
    public record ProblemEvolution(String analyzer, String rule, String status, String reason,
            com.learney.contentaudit.auditdomain.finding.FindingSeverity beforeSeverity,
            com.learney.contentaudit.auditdomain.finding.FindingSeverity afterSeverity,
            List<Finding> beforeFindings, List<Finding> afterFindings,
            String beforeCoverage, String afterCoverage, String beforeVersion, String afterVersion,
            String beforeRuntime, String afterRuntime, String beforeMeasureFingerprint, String afterMeasureFingerprint) {}
}
