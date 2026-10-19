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
    default ReanalysisScope reanalysisScope(String analyzer, AuditReport base, AuditReport correction, String requiredVersion) {
        throw new UnsupportedOperationException("El servicio no declara el encaje de reanálisis.");
    }
    record ReanalysisScope(java.util.Set<String> sharedQuizIds, java.util.Set<String> baseOnlyQuizIds, int baseOnlyUnits, int sharedUnits) {}
    record ComparisonReports(AuditReport baseline, AuditReport approved, AuditReport projected) {}

    record Request(String action, String batchId, String state, String runId, String operationId,
            String removesOperationId, Integer expectedRevision, Integer contentVersion,
            String contentFingerprint, String reviewer) {}
    record View(String batchId, int contentVersion, int revision, List<State> states,
            List<LooseRun> looseRuns, List<AnalysisSelection> history, List<String> supportedAnalyzers) {}
    record State(String id, String label, String fingerprint, AuditDigest digest,
            Map<String, QuizStudentView> studentViews, Map<String, Map<String, Object>> provenance,
            List<String> approvalWarnings, Map<String, List<String>> comparableAnalyzers, String analysisCode,
            Map<String, List<String>> inapplicableAnalyzers,
            List<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount> judges,
            Map<String, Map<String, Map<String, Object>>> verdictMarks) {
        public State(String id, String label, String fingerprint, AuditDigest digest,
                Map<String, QuizStudentView> studentViews, Map<String, Map<String, Object>> provenance,
                List<String> approvalWarnings, Map<String, List<String>> comparableAnalyzers, String analysisCode) {
            this(id, label, fingerprint, digest, studentViews, provenance, approvalWarnings, comparableAnalyzers, analysisCode, Map.of(), List.of(), Map.of());
        }
    }
    record LooseRun(String id, String analyzer, String auditId, String status, int contentVersion,
            String runtime, String judgeVersion, Instant measuredAt, int spent, int evaluated, int withError,
            List<Fit> fits, String displayName, String activityId) {}
    /**
     * Where a recorded run fits in a state. F-MEDJ-R001: what fits is split, without overlap, in {@code selected}
     * (already counted), {@code gaps} (nothing counted there yet) and the four kinds of {@code replaces}:
     * {@code toToday} (brings today's judge where the counted verdict is not), {@code toOtherVersion} (brings a version
     * that is not today's), {@code changesResults} (same version, other marks) and {@code repeats}. F-MEDJ-R002:
     * {@code runToday} says whether every verdict that fits is today's judge and {@code todayApproved} whether today's
     * version approved its exam; both are null when the judge has no version of today, and {@code runToday} also
     * when nothing fits.
     */
    record Fit(String state, int reached, int compatible, int missing, int replaces, int selected,
            List<String> nodeIds, List<String> lostErrors, List<String> newErrors,
            List<String> approvalWarnings, String reason, int compatibleWithError,
            int currentWithError, int resultWithError, List<String> currentVersions,
            int gaps, int toToday, int toOtherVersion, int changesResults, int repeats,
            Boolean runToday, Boolean todayApproved) {}
}
