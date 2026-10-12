package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import java.util.List;

/** Composes recorded measurements; compatibility is based on the evaluator's real input. */
public interface AnalysisComposer {
    java.util.Set<String> supportedAnalyzers();
    AuditReport compose(AuditReport primary, List<AnalysisSupplement> supplements);
    String fingerprint(String analyzer, AuditNode node);
}
