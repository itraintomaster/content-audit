package com.learney.contentaudit.revisiondomain.batch;

import java.util.List;

/**
 * F-GUARDA-R002: a part of the review of a batch, without the rest of it. The batch, its version and its correction,
 * how many decisions it has saved, and some of its corrections exactly as {@link BatchReview#items()} shows them: the
 * ones a saved decision may have changed, or the ones of the exercises asked for.
 */
public record BatchReviewSlice(String batchId, int correctionRevision, int contentVersion, BatchStatus status,
        int reviewCount, List<BatchReview.Item> items) {
    public BatchReviewSlice {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
