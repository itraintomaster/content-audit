package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.time.Instant;
import java.util.List;

/** A human review is not an application. Its exact steps are kept independently of a running batch. */
public record BatchReviewDecision(String batchId, List<Selection> selections, Status status,
        String reviewer, Instant decidedAt) {
    public enum Status { APPROVED, REJECTED, PENDING }

    public record Selection(String changeId, BatchChangeStep step,
            CourseElementSnapshot before, CourseElementSnapshot after) {
        public Selection(String changeId, BatchChangeStep step) {
            this(changeId, step, null, null);
        }
    }
}
