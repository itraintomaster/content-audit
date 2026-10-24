package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import java.util.List;

/** Composes recorded measurements; compatibility is based on the evaluator's real input. */
public interface AnalysisComposer {
    java.util.Set<String> supportedAnalyzers();
    AuditReport compose(AuditReport primary, List<AnalysisSupplement> supplements);
    /** Keeps missing saved opinions only where the evaluator's complete input is unchanged. */
    AuditReport preserveCompatibleOpinions(AuditReport current, AuditReport previous, String previousAuditId);
    /** Projects historical reports for correction without changing their recorded measurements. */
    default AuditReport contentOnly(AuditReport report) { return report; }
    String fingerprint(String analyzer, AuditNode node);
}
