package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.time.Instant;
import java.util.List;

/** Saved data for review: no analyzer, judge or comparison is executed by this query. */
public record BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
        List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items, List<BatchClosing> analyses,
        List<Version> history, List<SelectiveCorrection.Run> selectiveRuns) {
    public BatchReview(String batchId, int correctionRevision, int contentVersion, BatchReport report,
            List<RefinementTask> tasks, List<RevisionProposal> proposals, List<Item> items,
            List<BatchClosing> analyses, List<Version> history) {
        this(batchId, correctionRevision, contentVersion, report, tasks, proposals, items, analyses, history, List.of());
    }
    public record Version(String batchId, int correctionRevision, int contentVersion, BatchStatus status,
            Instant openedAt, int analyses, String baselineAuditId, String startAuditId) {}

    public record Item(String changeId, BatchChangeStep step, CourseElementSnapshot before, CourseElementSnapshot after,
            String origin, String ruleId,
            BatchReviewDecision.Status status, String reviewer, Instant decidedAt) {}
}
