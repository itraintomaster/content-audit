package com.learney.contentaudit.revisiondomain.batch;

import java.util.Optional;

/** Optional, disposable read model. The batch and review events remain the source of truth. */
public interface BatchReviewCache {
    /** Changes whenever any input of a review changes. Empty when a stable version cannot be obtained. */
    Optional<String> version();

    Optional<BatchReview> load(String requestedBatchId, String version);

    void save(String requestedBatchId, String version, BatchReview review);
}
