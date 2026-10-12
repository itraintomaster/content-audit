package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditReport;
import java.time.Instant;
import java.util.Set;

/** An explicitly selected, already measured contribution. No evaluator is invoked. */
public record AnalysisSupplement(String runId, String auditId, String analyzer, String runtime,
        Instant measuredAt, Set<String> nodeIds, AuditReport report) {
    public AnalysisSupplement {
        nodeIds = nodeIds == null ? Set.of() : Set.copyOf(nodeIds);
    }
}
