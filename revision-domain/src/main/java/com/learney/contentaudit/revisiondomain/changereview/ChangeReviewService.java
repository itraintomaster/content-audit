package com.learney.contentaudit.revisiondomain.changereview;

/** FEAT-MARCA: marks the exercises a batch changed; it never changes a correction, a decision, a closing or a publication. */
public interface ChangeReviewService {
    ChangeReview.Result review(ChangeReview.Request request);
}
