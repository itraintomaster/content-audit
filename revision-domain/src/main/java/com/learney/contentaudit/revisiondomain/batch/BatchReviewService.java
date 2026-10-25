package com.learney.contentaudit.revisiondomain.batch;

import java.util.Collection;

/** Saved workspace and human decisions; neither operation requires models, analyzers or course writers. */
public interface BatchReviewService {
    BatchReview read(String batchId);

    /** Saves like {@link #save} and answers the whole review of the batch, read again. */
    BatchReview decide(BatchReviewDecision decision);

    /**
     * F-GUARDA-R001, F-GUARDA-R002: validates and saves the decision against what is saved of the exercises it names,
     * without reading the whole review again, and answers only the corrections whose state it may have changed.
     */
    BatchReviewSlice save(BatchReviewDecision decision);

    /** F-GUARDA-R003: the corrections of these exercises as {@link #read} would show them, without the rest. */
    BatchReviewSlice slice(String batchId, Collection<String> quizIds);
}
