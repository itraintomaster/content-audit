package com.learney.contentaudit.revisiondomain.batch;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.contextnumbers.AuditDigest;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.coursedomain.CourseEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Authoritative current analysis and explicit, reversible selection of recorded runs. */
public interface CurrentAnalysisService {
    View execute(Request request);
    AuditReport report(CorrectionBatch batch, String state, CourseEntity course);
    java.util.Set<String> supportedAnalyzers();
    void seed(CorrectionBatch batch, CourseEntity course);
    ComparisonReports comparisonReports(CorrectionBatch batch, CourseEntity start, CourseEntity composed);
    record ComparisonReports(AuditReport baseline, AuditReport approved, AuditReport projected) {}

    record Request(String action, String batchId, String state, String runId, String operationId,
            String removesOperationId, Integer expectedRevision, Integer contentVersion,
            String contentFingerprint, String reviewer) {}
    record View(String batchId, int contentVersion, int revision, List<State> states,
            List<LooseRun> looseRuns, List<AnalysisSelection> history, List<String> supportedAnalyzers) {}
    record State(String id, String label, String fingerprint, AuditDigest digest,
            Map<String, QuizStudentView> studentViews, Map<String, Map<String, Object>> provenance,
            List<String> approvalWarnings, Map<String, List<String>> comparableAnalyzers) {}
    record LooseRun(String id, String analyzer, String auditId, String status, int contentVersion,
            String runtime, String judgeVersion, Instant measuredAt, int spent, int evaluated, int withError,
            List<Fit> fits) {}
    record Fit(String state, int reached, int compatible, int missing, int replaces, int selected,
            List<String> nodeIds, List<String> lostErrors, List<String> newErrors,
            List<String> approvalWarnings, String reason) {}
}
