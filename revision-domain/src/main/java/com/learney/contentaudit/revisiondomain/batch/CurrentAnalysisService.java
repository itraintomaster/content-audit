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
    /** Same selected states, without previews for every unselected historical run. */
    default View comparisonView(CorrectionBatch batch) {
        return execute(new Request("read",batch.getId(),null,null,null,null,null,null,null,null));
    }
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
    /**
     * {@code evaluators} and {@code evaluations}: F-CONTR, where each evaluator leaves each exercise and what that
     * decides; only the full reading carries them, the comparison leaves both empty.
     */
    record View(String batchId, int contentVersion, int revision, List<State> states,
            List<LooseRun> looseRuns, List<AnalysisSelection> history, List<String> supportedAnalyzers,
            List<Evaluator> evaluators, Map<String, Evaluation> evaluations) {
        public View {
            evaluators = evaluators == null ? List.of() : evaluators;
            evaluations = evaluations == null ? Map.of() : evaluations;
        }
        public View(String batchId, int contentVersion, int revision, List<State> states,
                List<LooseRun> looseRuns, List<AnalysisSelection> history, List<String> supportedAnalyzers) {
            this(batchId, contentVersion, revision, states, looseRuns, history, supportedAnalyzers, List.of(), Map.of());
        }
    }
    /** F-CONTR-R001: the four states an evaluator leaves an exercise in. */
    enum Standing { NOT_APPLICABLE, MISSING, PASSES, FAILS }
    /** F-CONTR-R001: an evaluator's state on an exercise and the step that decided it, as a short code. */
    record Assessment(Standing standing, String cause) {}
    /** F-CONTR-R005: the list the exercise goes to, before your approval splits it. */
    enum Route { ALL_PASS, OPINIONS_MISSING, HAS_A_PROBLEM }
    /** F-CONTR-R006: the verdict of a change, one per changed exercise. */
    enum ChangeVerdict { WORSENED, NOT_SHOWN_FIXED, NOT_MEASURED, CONSIDERED_GOOD }
    /**
     * F-CONTR: an exercise of the correction. {@code now} is every evaluator's state in the correction and {@code before}
     * in production, only for an exercise the batch changed; {@code change} is null when it did not change it, and
     * {@code worsened} names the evaluators that went to FAILS (F-CONTR-R006), so the screen reads them instead of
     * working them out again.
     */
    record Evaluation(Route route, ChangeVerdict change, List<String> worsened, Map<String, Assessment> now,
            Map<String, Assessment> before) {}
    /** F-CONTR-R003: an evaluator of exercises; {@code notice} says what the screen warns about it, or null. */
    record Evaluator(String name, String displayName, String notice) {}
    /**
     * {@code studentViews}: F-OPINA-R012, each exercise as the student sees it in this state's content; what a judge
     * read is the {@code studentView} of its provenance. {@code missingOpinions}: F-OPINA-R001, per judge the exercises
     * it applies to and has not opined on.
     */
    record State(String id, String label, String fingerprint, AuditDigest digest,
            Map<String, QuizStudentView> studentViews, Map<String, Map<String, Object>> provenance,
            List<String> approvalWarnings, Map<String, List<String>> comparableAnalyzers, String analysisCode,
            Map<String, List<String>> inapplicableAnalyzers,
            List<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount> judges,
            Map<String, Map<String, Map<String, Object>>> verdictMarks,
            Map<String, List<String>> missingOpinions) {
        public State {
            missingOpinions = missingOpinions == null ? Map.of() : missingOpinions;
        }
        public State(String id, String label, String fingerprint, AuditDigest digest,
                Map<String, QuizStudentView> studentViews, Map<String, Map<String, Object>> provenance,
                List<String> approvalWarnings, Map<String, List<String>> comparableAnalyzers, String analysisCode,
                Map<String, List<String>> inapplicableAnalyzers,
                List<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount> judges,
                Map<String, Map<String, Map<String, Object>>> verdictMarks) {
            this(id, label, fingerprint, digest, studentViews, provenance, approvalWarnings, comparableAnalyzers,
                    analysisCode, inapplicableAnalyzers, judges, verdictMarks, Map.of());
        }
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
