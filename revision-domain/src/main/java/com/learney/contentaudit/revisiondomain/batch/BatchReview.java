package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.conversion.MultipleChoiceConversion;
import java.time.Instant;
import java.util.List;

/** Saved data for review: no analyzer, judge or comparison is executed by this query. */
public record BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
        List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items, List<BatchClosing> analyses,
        List<Version> history, List<SelectiveCorrection.Run> selectiveRuns, List<ContextOpinion.Run> opinionRuns,
        List<QuizCorrection.ComparisonBaseline> comparisonBaselines, boolean contentFrozen, String parentBatchId,
        com.learney.contentaudit.coursedomain.CourseEntity course,
        com.learney.contentaudit.coursedomain.CourseEntity baseCourse) {
    public BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
            List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items,
            List<BatchClosing> analyses, List<Version> history, List<SelectiveCorrection.Run> selectiveRuns,
            List<ContextOpinion.Run> opinionRuns, List<QuizCorrection.ComparisonBaseline> comparisonBaselines) {
        this(batchId, correctionRevision, contentVersion, report, tasks, proposals, items, analyses, history,
                selectiveRuns, opinionRuns, comparisonBaselines, false, null, null, null);
    }
    public BatchReview { course = CorrectionEvolution.copyCourse(course); baseCourse = CorrectionEvolution.copyCourse(baseCourse); }
    @Override public com.learney.contentaudit.coursedomain.CourseEntity course() { return CorrectionEvolution.copyCourse(course); }
    @Override public com.learney.contentaudit.coursedomain.CourseEntity baseCourse() { return CorrectionEvolution.copyCourse(baseCourse); }

    public BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
            List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items,
            List<BatchClosing> analyses, List<Version> history, List<SelectiveCorrection.Run> selectiveRuns,
            List<ContextOpinion.Run> opinionRuns) {
        this(batchId, correctionRevision, contentVersion, report, tasks, proposals, items, analyses, history,
                selectiveRuns, opinionRuns, List.of());
    }
    public BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
            List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items,
            List<BatchClosing> analyses, List<Version> history) {
        this(batchId, correctionRevision, contentVersion, report, tasks, proposals, items, analyses, history, List.of(), List.of());
    }
    public BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
            List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items,
            List<BatchClosing> analyses, List<Version> history, List<SelectiveCorrection.Run> selectiveRuns) {
        this(batchId, correctionRevision, contentVersion, report, tasks, proposals, items, analyses, history, selectiveRuns, List.of());
    }
    public record Version(String batchId, int correctionRevision, int contentVersion, BatchStatus status,
            Instant openedAt, int analyses, String baselineAuditId, String startAuditId,
            String parentBatchId, boolean frozen, int analysisRevision) {
        public Version(String batchId, int correctionRevision, int contentVersion, BatchStatus status,
                Instant openedAt, int analyses, String baselineAuditId, String startAuditId) {
            this(batchId, correctionRevision, contentVersion, status, openedAt, analyses, baselineAuditId,
                    startAuditId, null, false, 0);
        }
    }

    public record Item(String changeId, BatchChangeStep step, CourseElementSnapshot before, CourseElementSnapshot after,
            String origin, String ruleId,
            BatchReviewDecision.Status status, String reviewer, Instant decidedAt,
            List<String> taskIds, String rationale, List<String> requiredJudges) {
        /** Approval requirements are supplied by the domain, not guessed by clients. */
        public Item {
            requiredJudges = ruleId != null && ruleId.startsWith(MultipleChoiceConversion.RULE + ":")
                    && !ruleId.endsWith(":RESTORE") ? MultipleChoiceConversion.JUDGES : List.of();
        }
        public Item(String changeId, BatchChangeStep step, CourseElementSnapshot before, CourseElementSnapshot after,
                String origin, String ruleId, BatchReviewDecision.Status status, String reviewer, Instant decidedAt,
                List<String> taskIds, String rationale) {
            this(changeId, step, before, after, origin, ruleId, status, reviewer, decidedAt, taskIds, rationale, List.of());
        }
        /** Legacy projections have no provenance, and must be rebuilt before being cached. */
        public Item(String changeId, BatchChangeStep step, CourseElementSnapshot before, CourseElementSnapshot after,
                String origin, String ruleId, BatchReviewDecision.Status status, String reviewer, Instant decidedAt) {
            this(changeId, step, before, after, origin, ruleId, status, reviewer, decidedAt, null, null);
        }
    }
}
