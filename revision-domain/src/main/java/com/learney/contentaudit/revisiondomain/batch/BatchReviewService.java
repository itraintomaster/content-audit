package com.learney.contentaudit.revisiondomain.batch;

/** Saved workspace and human decisions; neither operation requires models, analyzers or course writers. */
public interface BatchReviewService {
    BatchReview read(String batchId);

    BatchReview decide(BatchReviewDecision decision);
}
