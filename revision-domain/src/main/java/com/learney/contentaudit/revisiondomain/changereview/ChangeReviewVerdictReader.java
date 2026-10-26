package com.learney.contentaudit.revisiondomain.changereview;

/** Reads what the reviewer answered; anything it cannot read is an {@link IllegalArgumentException}. */
public interface ChangeReviewVerdictReader {
    ChangeReview.Verdict read(String payload);
}
