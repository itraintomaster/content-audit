package com.learney.contentaudit.revisiondomain.batch;

import java.time.Instant;
import java.util.List;

/** Append-only selection history, independent of course edits and human content approvals. */
public record AnalysisSelection(String operationId, String batchId, String state, String action,
        String runId, String removesOperationId, String analyzer, List<String> nodeIds,
        int expectedRevision, int contentVersion, String contentFingerprint, String reviewer, Instant at) {
    public AnalysisSelection {
        nodeIds = nodeIds == null ? List.of() : List.copyOf(nodeIds);
    }
}
