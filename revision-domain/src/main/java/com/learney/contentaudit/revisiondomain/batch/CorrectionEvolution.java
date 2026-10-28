package com.learney.contentaudit.revisiondomain.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.util.List;

/** An append-only chain of corrections. Content checkpoints are independent from later analysis selections. */
public final class CorrectionEvolution {
    private CorrectionEvolution() {}
    private static final ObjectMapper JSON = new ObjectMapper();

    public record Request(String action, String batchId, String operationId, Integer expectedVersion,
            String fingerprint, Integer analysisRevision, String reviewer) {}
    public record View(String batchId, List<Version> versions, Estimate estimate, Result result, Tasks tasks) {
        public View(String batchId, List<Version> versions, Estimate estimate, Result result) {
            this(batchId, versions, estimate, result, null);
        }
    }
    /**
     * F-MOTIVO-R003: the inherited changes whose tasks the batch still reads in a predecessor's numbering
     * ({@code inherited}), the ones that point to copies after carrying ({@code changes}), the copies made
     * ({@code tasks}), the ids no predecessor resolves ({@code unresolved}), the outcomes written under a
     * predecessor's id that go to its copy ({@code outcomes}) and whether it was saved.
     */
    public record Tasks(int inherited, int changes, int tasks, int unresolved, int outcomes, boolean applied) {}
    public record Estimate(boolean allowed, String blockedReason, int contentVersion, String fingerprint,
            int analysisRevision, int nextCorrectionRevision) {}
    public record Result(String status, String newBatchId, int correctionRevision) {}
    public record Version(String batchId, String label, int correctionRevision, int contentVersion,
            BatchStatus status, String parentBatchId, String fingerprint, int analysisRevision,
            boolean frozen, CourseEntity course, CurrentAnalysisService.View analysis) {
        public Version { course = copyCourse(course); }
        @Override public CourseEntity course() { return copyCourse(course); }
    }
    public record Lineage(String parentBatchId, int parentContentVersion, int parentAnalysisRevision,
            String operationId, String parentFingerprint, CourseEntity baseCourse, CourseEntity sourceCourse,
            List<BatchReviewDecision> inheritedReviews) {
        public Lineage {
            baseCourse = copyCourse(baseCourse); sourceCourse = copyCourse(sourceCourse);
            inheritedReviews = copyReviews(inheritedReviews);
        }
        @Override public CourseEntity baseCourse() { return copyCourse(baseCourse); }
        @Override public CourseEntity sourceCourse() { return copyCourse(sourceCourse); }
        @Override public List<BatchReviewDecision> inheritedReviews() { return copyReviews(inheritedReviews); }
    }
    public static CourseEntity copyCourse(CourseEntity course) {
        return course == null ? null : JSON.convertValue(JSON.valueToTree(course), CourseEntity.class);
    }
    public static CourseElementSnapshot copySnapshot(CourseElementSnapshot snapshot) {
        return snapshot == null ? null : JSON.convertValue(JSON.valueToTree(snapshot), CourseElementSnapshot.class);
    }
    public static List<BatchReviewDecision> copyReviews(List<BatchReviewDecision> reviews) {
        if (reviews == null) return List.of();
        return reviews.stream().map(r -> new BatchReviewDecision(r.batchId(), r.selections().stream().map(s ->
            new BatchReviewDecision.Selection(s.changeId(), new BatchChangeStep(s.step().getQuizId(),
                    s.step().getParts() == null ? null : List.copyOf(s.step().getParts()), copySnapshot(s.step().getAfter())),
                    copySnapshot(s.before()), copySnapshot(s.after()))).toList(), r.status(), r.reviewer(),
                    r.decidedAt(), r.expectedReviewCount())).toList();
    }
}
