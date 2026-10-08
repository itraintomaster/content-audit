package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewCache;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewService;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;

/** Builds the review workspace, sharing the correction service's implementation without a model runtime. */
public final class DefaultBatchReviewServiceFactory {
    public BatchReviewService create(BatchStore batches, RevisionArtifactStore artifacts,
            RefinementPlanStore plans, BatchReviewCache cache) {
        return new DefaultBatchReview(batches, artifacts, plans, new DefaultBatchReportBuilder(), cache);
    }
}
