package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.ActiveAnalysisSelection;
import com.learney.contentaudit.auditdomain.ActiveAnalysisSelectionStore;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.Baseline;
import com.learney.contentaudit.auditdomain.BaselineStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.TaskOrigin;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBriefer;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustment;
import com.learney.contentaudit.revisiondomain.batch.BatchAdjustmentKind;
import com.learney.contentaudit.revisiondomain.batch.BatchAlreadyOpenException;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.BatchClosing;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchEstimate;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.BatchOpenRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchPublication;
import com.learney.contentaudit.revisiondomain.batch.BatchPublishRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchReport;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewSlice;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewDecision;
import com.learney.contentaudit.revisiondomain.batch.BatchRunRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchService;
import com.learney.contentaudit.revisiondomain.batch.BatchSettings;
import com.learney.contentaudit.revisiondomain.batch.BatchStatus;
import com.learney.contentaudit.revisiondomain.batch.BatchStore;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcome;
import com.learney.contentaudit.revisiondomain.batch.BatchTaskOutcomeKind;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import com.learney.contentaudit.revisiondomain.batch.NoOpenBatchException;
import com.learney.contentaudit.revisiondomain.comparison.GoalBook;
import com.learney.contentaudit.revisiondomain.nonregression.AudioRerecordRequest;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.panel.CandidateRejection;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.PanelRunSettings;
import com.learney.contentaudit.revisiondomain.panel.PanelRunner;
import com.learney.contentaudit.revisiondomain.panel.PanelRunnerFactory;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import com.learney.contentaudit.revisiondomain.panel.TopicWork;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.processing.Generated;

/**
 * One port with the moments of a batch (FEAT-TANDA): open, estimate, run, close, adjust, report and publish. Opening
 * reads the baseline of F's registry -- without one it does not open -- and starts from it the first time and from what
 * the last published batch left afterwards, with the active pair moved to that analysis and its plan; only one batch is
 * open at a time, its tasks leave the plan as «in batch», and the goals of the declaration are sealed then and only then
 * (F-METAS-R006, R011). Running applies the rules always and opens the panels only under an explicit cap of queries;
 * a failure of the service leaves the batch interrupted, with everything it decided, and running again resumes it.
 * {@code adjust} is the only entry for José's decisions and for the «drop» of the command line, so all of them go
 * through the ledger.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel"
)
class DefaultBatchService implements BatchService {
    private final BatchStore batchStore;
    private java.util.function.UnaryOperator<AuditReport> contentProjection = java.util.function.UnaryOperator.identity();
    DefaultBatchService withContentProjection(com.learney.contentaudit.auditdomain.analysis.AnalysisComposer composer) {
        if (composer != null) contentProjection = composer::contentOnly;
        return this;
    }

    private final RefinementPlanStore refinementPlanStore;

    private final AuditReportStore auditReportStore;

    private final ActiveAnalysisSelectionStore activeAnalysisSelectionStore;

    private final BaselineStore baselineStore;

    private final CourseRepository courseRepository;

    private final PanelRunnerFactory panelRunnerFactory;

    private final ErrorTaskBriefer briefer;

    private final BatchTaskSelector taskSelector;

    private final BatchCostEstimator costEstimator;

    private final RuleStage ruleStage;

    private final PanelStage panelStage;

    private final BatchChangeLedger changeLedger;

    private final BatchCloser closer;

    private final BatchPublisher publisher;

    private final BatchReportBuilder reportBuilder;

    private final Path coursePath;

    private final GoalBook goalBook;

    /** F-MESA-R010 inv. 4 and DOUBT-TEMAS-A-LA-VEZ: 4 topics and 4 queries at once unless others are asked. */
    static final int DEFAULT_AT_ONCE = 4;

    static final int DEFAULT_MAX_ATTEMPTS = 3;

    static final String CAP_REACHED = "se llegó al tope";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    @Override
    public CorrectionBatch open(BatchOpenRequest request) {
        Optional<CorrectionBatch> open = batchStore.open();
        if (open.isPresent()) {
            throw new BatchAlreadyOpenException(open.get().getId());
        }
        if (request == null || request.getLevel() == null) {
            throw new IllegalArgumentException("Falta el nivel de la tanda.");
        }
        Baseline baseline = baselineStore != null ? baselineStore.read().orElse(null) : null;
        if (baseline == null || baseline.getAuditId() == null) {
            throw new IllegalStateException("No hay una línea base fijada: fijala antes de abrir la primera tanda.");
        }
        Optional<CorrectionBatch> published = batchStore.latestPublished();
        String startAuditId = published.map(b -> b.getPublication().getPublishedAuditId()).orElse(baseline.getAuditId());
        AuditReport start = auditReportStore.load(startAuditId).orElseThrow(() -> new IllegalStateException(
                "No se encuentra el análisis " + startAuditId + " del que parte la tanda."));
        String planId = published.map(b -> b.getPublication().getPublishedPlanId()).orElseGet(() -> planOf(startAuditId));
        RefinementPlan plan = refinementPlanStore.load(planId).orElseThrow(() -> new IllegalStateException(
                "No se encuentra el plan " + planId + " del análisis " + startAuditId + "."));

        CorrectionBatch batch = taskSelector.select(plan, start, request.getLevel(),
                BatchBook.list(request.getConvertToMultipleChoice()), pendingRerecords());
        Instant now = Instant.now();
        batch.setId("tanda-" + request.getLevel().name() + "-" + STAMP.format(now));
        batch.setLevel(request.getLevel());
        batch.setStatus(BatchStatus.OPEN);
        batch.setBaselineAuditId(baseline.getAuditId());
        batch.setStartAuditId(startAuditId);
        batch.setPlanId(planId);
        batch.setAnalyzers(analyzersOf(start));
        batch.setSettings(new BatchSettings(atOnce(request.getTopicsAtOnce()), atOnce(request.getQueriesAtOnce()),
                DEFAULT_MAX_ATTEMPTS));
        batch.setBaseCourseSnapshot(courseRepository.load(coursePath));
        batch.setOpenedAt(now);
        batch.setQueriesSpent(0);
        // A continuation keeps its ordinal. Only opening a genuinely new batch
        // after integration advances the history, within the same level and Base.
        int previousRevision = batchStore.list().stream()
                .filter(previous -> previous.getStatus() == BatchStatus.PUBLISHED
                        && previous.getLevel() == request.getLevel()
                        && Objects.equals(previous.getBaselineAuditId(), baseline.getAuditId()))
                .mapToInt(BatchBook::correctionRevision).max().orElse(0);
        batch.setCorrectionRevision(previousRevision + 1);

        // R002: the tasks of the batch leave the plan as «in batch»; revise and revise-instructions do not see them.
        for (RefinementTask task : BatchBook.list(plan.getTasks())) {
            if (task != null && BatchBook.list(batch.getTaskIds()).contains(task.getId())) {
                task.setStatus(RefinementTaskStatus.IN_BATCH);
            }
        }
        refinementPlanStore.save(plan);
        if (activeAnalysisSelectionStore != null) {
            activeAnalysisSelectionStore.write(new ActiveAnalysisSelection(startAuditId, planId));
        }
        if (goalBook != null) {
            goalBook.seal(); // F-METAS-R006 and R011: opening fixes the goals, once
        }
        batchStore.save(batch);
        return batch;
    }

    @Override
    public Optional<CorrectionBatch> current() {
        return batchStore.open();
    }

    @Override
    public BatchEstimate estimate() {
        CorrectionBatch batch = requireOpen();
        AuditReport start = start(batch);
        Map<String, RefinementTask> tasks = refinementPlanStore.load(batch.getPlanId()).map(BatchBook::tasksById)
                .orElse(Map.of());
        List<ErrorTaskBrief> briefs = new ArrayList<>();
        for (String taskId : BatchBook.list(batch.getTaskIds())) {
            RefinementTask task = tasks.get(taskId);
            if (task != null && !ContentCorrectionTasks.audioOnly(task) && briefer != null) {
                task = ContentCorrectionTasks.content(task);
                briefer.brief(start, task).ifPresent(briefs::add);
            }
        }
        return withJudges(costEstimator.estimate(batch, briefs), batch);
    }

    /**
     * F-CJUEZ-R002: what the closing would ask of the judges, before any query: the exercises the batch changes today and,
     * per judge, what it judges, what the ledger has and what it misses -- on the course the batch started from and on the
     * course composed with its changes. Only the approved judges that are paid add to the queries and the hours: a judge
     * without an approved exam consults nothing, and a local one costs nothing (F-MLOCAL-R008). The hours follow the pace
     * of the estimate of the panel: 19 seconds a query, with the queries at once of the batch.
     */
    private BatchEstimate withJudges(BatchEstimate estimate, CorrectionBatch batch) {
        CourseEntity course = BatchCourses.start(batch, courseRepository.load(coursePath), batchStore);
        List<BatchJudgeAccount> judges = BatchBook.copy(judgePass.estimate(batch, course,
                changeLedger.composedCourse(batch, course)));
        int queries = 0;
        for (BatchJudgeAccount judge : judges) {
            if (judge != null && judge.isApproved() && !judge.isLocal()) {
                queries += judge.getQueriesMissing();
            }
        }
        BatchSettings settings = batch.getSettings();
        int atOnce = settings != null && settings.getQueriesAtOnce() > 0 ? settings.getQueriesAtOnce() : DEFAULT_AT_ONCE;
        estimate.setChangedQuizzes(BatchChanges.of(batch, course).quizCount());
        estimate.setJudges(judges);
        estimate.setJudgeQueries(queries);
        estimate.setJudgeHours(Math.round(queries * DefaultBatchCostEstimator.SECONDS_PER_QUERY / atOnce / 3600.0 * 100.0)
                / 100.0);
        return estimate;
    }

    @Override
    public BatchReport run(BatchRunRequest request) {
        CorrectionBatch batch = requireOpen();
        batchStore.requireEditableContent(batch);
        AuditReport start = start(batch);
        CourseEntity course = BatchCourses.start(batch, courseRepository.load(coursePath), batchStore);
        // Continuing the original plan is still the same correction, even after a partial analysis.
        // Content versions identify the measured snapshot without inventing a new correction cycle.
        batch.setCorrectionRevision(BatchBook.correctionRevision(batch));
        batch.setContentVersion(batch.getContentVersion() + 1);
        batch.setStatus(BatchStatus.RUNNING);
        batch.setInterruptionReason(null);
        batchStore.save(batch);

        batch = ruleStage.apply(batch, start, course);
        Integer cap = request != null ? request.getQueryCap() : null;
        BatchStatus status = BatchStatus.OPEN;
        if (cap == null) {
            // R003: without a cap the rules run, since they query nothing, and the panels stay not attempted.
            for (String taskId : BatchBook.list(batch.getTaskIds())) {
                BatchTaskOutcomeKind kind = BatchBook.kind(batch, taskId);
                if (kind == BatchTaskOutcomeKind.PENDING) {
                    BatchBook.setOutcome(batch, taskId, BatchTaskOutcomeKind.NOT_ATTEMPTED, null, null,
                            "sin tope de consultas pedido: la mesa no corrió");
                }
            }
        } else {
            batch.setQueryCap(cap);
            BatchSettings settings = batch.getSettings();
            QueryAllowance allowance = panelRunnerFactory.allowance(cap,
                    settings != null && settings.getQueriesAtOnce() > 0 ? settings.getQueriesAtOnce() : DEFAULT_AT_ONCE);
            batch = panelStage.run(batch, start, course, allowance);
            Optional<String> tripped = allowance.tripped();
            if (tripped.isPresent() && !tripped.get().startsWith(CAP_REACHED)) {
                status = BatchStatus.INTERRUPTED; // a failure of the service: everything decided is kept
                batch.setInterruptionReason(tripped.get());
            }
        }
        batch.setStatus(status);
        batchStore.save(batch);
        return reportBuilder.build(batch);
    }

    @Override
    public BatchClosing close(BatchCloseRequest request) {
        CorrectionBatch selected = requireOpen();
        BatchCloseRequest asked = request != null ? request : new BatchCloseRequest(new LinkedHashMap<>(), null);
        validate(selected, asked);
        BatchStatus idleStatus = selected.getStatus();
        CorrectionBatch batch = batchStore.withAnalysisLock(selected.getId(), () -> {
            CorrectionBatch current = batchStore.load(selected.getId()).orElseThrow();
            batchStore.requireEditableContent(current);
            if (current.getContentVersion() != selected.getContentVersion() || current.getStatus() != idleStatus
                    || current.getStatus() == BatchStatus.RUNNING)
                throw new IllegalStateException("La corrección cambió o tiene otra operación activa: volvé a leer.");
            current.setStatus(BatchStatus.RUNNING);
            batchStore.save(current);
            return current;
        });
        try {
            CourseEntity course = BatchCourses.start(batch, courseRepository.load(coursePath), batchStore);
            batch = closer.close(batch, course, asked);
            List<BatchClosing> closings = BatchBook.list(batch.getClosings());
            if (reviews != null && !closings.isEmpty()) reviews.snapshot(batch, closings.get(closings.size() - 1));
            batchStore.save(batch);
            return closings.isEmpty() ? null : closings.get(closings.size() - 1);
        } catch (RuntimeException failure) {
            batchStore.withAnalysisLock(selected.getId(), () -> {
                CorrectionBatch current = batchStore.load(selected.getId()).orElseThrow();
                if (current.getStatus() == BatchStatus.RUNNING) {
                    current.setStatus(idleStatus);
                    batchStore.save(current);
                }
                return null;
            });
            throw failure;
        }
    }

    @Override
    public CorrectionBatch adjust(BatchAdjustment adjustment) {
        CorrectionBatch selected = requireOpen();
        return batchStore.withAnalysisLock(selected.getId(), () -> adjustLocked(adjustment, selected.getId()));
    }
    private CorrectionBatch adjustLocked(BatchAdjustment adjustment, String batchId) {
        CorrectionBatch batch = batchStore.load(batchId).orElseThrow();
        batchStore.requireEditableContent(batch);
        CourseEntity course = BatchCourses.start(batch, courseRepository.load(coursePath), batchStore);
        List<BatchChangeStep> beforeSteps = activeSteps(batch);
        if (adjustment.getDecidedAt() == null) {
            adjustment.setDecidedAt(Instant.now());
        }
        boolean recalculate = false;
        switch (adjustment.getKind()) {
            case DROP_CHANGE -> {
                batch = changeLedger.drop(batch, adjustment.getChangeId(), adjustment.getReason(), course);
                recalculate = true;
            }
            case DROP_RULE -> {
                boolean any = false;
                for (BatchChange change : BatchBook.copy(batch.getChanges())) {
                    if (change != null && change.getDroppedReason() == null && change.getOrigin() == BatchChangeOrigin.RULE
                            && Objects.equals(change.getRuleId(), adjustment.getRuleId())) {
                        batch = changeLedger.drop(batch, change.getChangeId(), adjustment.getReason() != null
                                ? adjustment.getReason() : "José sacó la regla " + adjustment.getRuleId(), course);
                        any = true;
                    }
                }
                if (!any) {
                    throw new IllegalArgumentException("La tanda " + batch.getId() + " no tiene cambios de la regla "
                            + adjustment.getRuleId() + ".");
                }
                recalculate = true;
            }
            case ACCEPT_CANDIDATE, ACCEPT_AS_EXCEPTION -> batch = accept(batch, adjustment, course,
                    verdictOf(batch, adjustment.getQuizId(), adjustment.getCandidate()), "aceptado por José");
            case RETURN_TO_PANEL -> {
                if (adjustment.getCandidate() != null) {
                    batch = assessJosesFix(batch, adjustment, course); // F-BANDEJA-R003: «escribir el arreglo»
                } else {
                    // F-BANDEJA-R002: the candidate does not pass -- back to the panel with one attempt less, or
                    // without winner when none is left.
                    int maxAttempts = batch.getSettings() != null && batch.getSettings().getMaxAttempts() > 0
                            ? batch.getSettings().getMaxAttempts() : DEFAULT_MAX_ATTEMPTS;
                    String why = adjustment.getReason() != null ? adjustment.getReason() : "José la devolvió a la mesa";
                    for (String taskId : BatchBook.list(adjustment.getTaskIds())) {
                        int spent = BatchBook.outcome(batch, taskId).map(BatchTaskOutcome::getAttempts).orElse(0) + 1;
                        BatchBook.setOutcome(batch, taskId, spent >= maxAttempts ? BatchTaskOutcomeKind.NO_WINNER
                                : BatchTaskOutcomeKind.PENDING, null, spent, why);
                    }
                }
            }
            case KEEP_ORIGINAL -> {
                for (String taskId : BatchBook.list(adjustment.getTaskIds())) {
                    BatchBook.setOutcome(batch, taskId, BatchTaskOutcomeKind.NO_WINNER, null, null,
                            "José deja el original");
                }
            }
            case TASK_FROM_TIE -> batch = taskFromTie(batch, adjustment, course);
            case APPROVE_RULE_SAMPLE -> {
                // recorded as José's decision: the changes of the rule may be published
            }
        }
        List<BatchAdjustment> adjustments = BatchBook.copy(batch.getAdjustments());
        adjustments.add(adjustment);
        batch.setAdjustments(adjustments);
        if (!beforeSteps.equals(activeSteps(batch))) {
            batch.setContentVersion(batch.getContentVersion() + 1);
        }
        batchStore.save(batch);
        if (recalculate && !BatchBook.list(batch.getClosings()).isEmpty()) {
            // R008: recalculated against the gate without what left; the re-adjustment closes with no request.
            batch = closer.close(batch, course, new BatchCloseRequest(new LinkedHashMap<>(), null));
            List<BatchClosing> closings = BatchBook.list(batch.getClosings());
            if (reviews != null && !closings.isEmpty()) reviews.snapshot(batch, closings.get(closings.size() - 1));
            batchStore.save(batch);
        }
        return batch;
    }

    private static List<BatchChangeStep> activeSteps(CorrectionBatch batch) {
        return BatchBook.list(batch.getChanges()).stream()
                .filter(change -> change != null && change.getDroppedReason() == null)
                .flatMap(change -> BatchBook.list(change.getSteps()).stream()).toList();
    }

    @Override
    public BatchReport report(String batchId) {
        CorrectionBatch batch = batchId != null && !batchId.isBlank()
                ? batchStore.load(batchId).orElseThrow(() -> new NoOpenBatchException("No hay una tanda " + batchId + "."))
                : requireOpen();
        return reportBuilder.build(batch);
    }

    @Override
    public BatchPublication publish(BatchPublishRequest request) {
        CorrectionBatch batch = requireOpen();
        batchStore.requireEditableContent(batch);
        CourseEntity course = BatchCourses.start(batch, courseRepository.load(coursePath), batchStore);
        // F-PUBPROD-R010: publishing saves the batch itself, as its last step, so a save that fails is undone with the
        // rest.
        batch = publisher.publish(batch, request, course);
        return batch.getPublication();
    }

    @Override
    public BatchReview review(String batchId) { return reviews.read(batchId); }

    @Override
    public BatchReview review(BatchReviewDecision decision) { return reviews.decide(decision); }

    @Override
    public BatchReviewSlice saveReview(BatchReviewDecision decision) { return reviews.save(decision); }

    // -------------------------------------------------------------------------
    // José's decisions from the tray (FEAT-BANDEJA)
    // -------------------------------------------------------------------------

    /** [ASSUMPTION] A fix José writes is judged by the judges of a new English: four queries at most. */
    static final int QUERIES_FOR_A_FIX = 4;

    /** The panel that measures and judges a fix José writes, as any candidate; the factory hands it over. */
    private PanelRunner panelRunner;

    private final BatchJudgePass judgePass;
    private final DefaultBatchReview reviews;

    public DefaultBatchService(BatchStore batchStore, RefinementPlanStore refinementPlanStore,
            AuditReportStore auditReportStore, ActiveAnalysisSelectionStore activeAnalysisSelectionStore,
            BaselineStore baselineStore, CourseRepository courseRepository, PanelRunnerFactory panelRunnerFactory,
            ErrorTaskBriefer briefer, BatchTaskSelector taskSelector, BatchCostEstimator costEstimator,
            RuleStage ruleStage, PanelStage panelStage, BatchChangeLedger changeLedger, BatchCloser closer,
            BatchPublisher publisher, BatchReportBuilder reportBuilder, Path coursePath, GoalBook goalBook,
            BatchJudgePass judgePass, DefaultBatchReview reviews) {
        this.batchStore = batchStore;
        this.refinementPlanStore = refinementPlanStore;
        this.auditReportStore = auditReportStore;
        this.activeAnalysisSelectionStore = activeAnalysisSelectionStore;
        this.baselineStore = baselineStore;
        this.courseRepository = courseRepository;
        this.panelRunnerFactory = panelRunnerFactory;
        this.briefer = briefer;
        this.taskSelector = taskSelector;
        this.costEstimator = costEstimator;
        this.ruleStage = ruleStage;
        this.panelStage = panelStage;
        this.changeLedger = changeLedger;
        this.closer = closer;
        this.publisher = publisher;
        this.reportBuilder = reportBuilder;
        this.coursePath = coursePath;
        this.goalBook = goalBook;
        this.judgePass = judgePass;
        this.reviews = reviews;
    }

    DefaultBatchService withPanel(PanelRunner panelRunner) {
        this.panelRunner = panelRunner;
        return this;
    }

    /** A candidate into the batch, as José's change, with the verdict of its gate -- and its requests to record. */
    private CorrectionBatch accept(CorrectionBatch batch, BatchAdjustment adjustment, CourseEntity course,
            ChangeGateVerdict verdict, String detail) {
        List<BatchChangeStep> steps = BatchBook.stepsFor(changeLedger.composedCourse(batch, course),
                adjustment.getCandidate(), adjustment.getQuizId() != null ? List.of(adjustment.getQuizId()) : List.of(),
                knowledgeOfQuiz(course, adjustment.getQuizId()));
        if (steps.isEmpty()) {
            return batch;
        }
        String changeId = BatchBook.freeChangeId(batch, "jose-" + adjustment.getQuizId());
        batch = changeLedger.record(batch, new BatchChange(changeId, BatchChangeOrigin.JOSE, null,
                knowledgeOfQuiz(course, adjustment.getQuizId()), BatchBook.copy(adjustment.getTaskIds()), steps, verdict,
                adjustment.getKind() == BatchAdjustmentKind.ACCEPT_AS_EXCEPTION, Instant.now(), null), course);
        for (String taskId : BatchBook.list(adjustment.getTaskIds())) {
            int attempts = BatchBook.outcome(batch, taskId).map(BatchTaskOutcome::getAttempts).orElse(0);
            BatchBook.setOutcome(batch, taskId, BatchTaskOutcomeKind.RESOLVED_BY_PANEL, changeId, attempts, detail);
        }
        return batch;
    }

    /** The verdict of the gate the panel gave a candidate of the exercise: the accepted one, or the one José saw. */
    private static ChangeGateVerdict verdictOf(CorrectionBatch batch, String quizId, PanelCandidate candidate) {
        ChangeGateVerdict verdict = null;
        for (PanelDecision decision : BatchBook.list(batch.getPanelDecisions())) {
            if (decision == null || !BatchBook.list(decision.getQuizIds()).contains(quizId)) {
                continue;
            }
            if (candidate != null && candidate.equals(decision.getAccepted())) {
                verdict = decision.getAcceptedVerdict();
            } else if (candidate != null && candidate.equals(decision.getBestFailing())) {
                verdict = decision.getBestFailingVerdict();
            }
        }
        return verdict;
    }

    /**
     * F-BANDEJA-R003, «escribir el arreglo»: José's version is measured and judged as any candidate of the panel, with
     * the tasks of the exercise. If it passes it enters the batch as his change; if not, it goes back to him with the
     * reasons, as the best failing candidate of the exercise.
     */
    private CorrectionBatch assessJosesFix(CorrectionBatch batch, BatchAdjustment adjustment, CourseEntity course) {
        if (panelRunner == null) {
            throw new IllegalStateException("La tanda no tiene la mesa para medir y juzgar el arreglo de José.");
        }
        String quizId = adjustment.getQuizId();
        Optional<BatchBook.Place> place = BatchBook.place(changeLedger.composedCourse(batch, course), quizId);
        if (place.isEmpty() || place.get().quiz() == null) {
            throw new IllegalArgumentException("El ejercicio " + quizId + " no está en el curso de la tanda.");
        }
        AuditReport start = start(batch);
        Map<String, RefinementTask> tasks = refinementPlanStore.load(batch.getPlanId()).map(BatchBook::tasksById)
                .orElse(Map.of());
        List<ErrorTaskBrief> briefs = new ArrayList<>();
        for (String taskId : BatchBook.list(adjustment.getTaskIds())) {
            RefinementTask task = tasks.get(taskId);
            if (task != null && !ContentCorrectionTasks.audioOnly(task) && briefer != null) {
                task = ContentCorrectionTasks.content(task);
                briefer.brief(start, task).ifPresent(briefs::add);
            }
        }
        KnowledgeEntity knowledge = place.get().knowledge();
        TopicWork work = new TopicWork(batch.getId(), batch.getLevel(), place.get().milestone().getId(),
                place.get().topic().getId(), knowledge, knowledge.getSentenceMode(), briefs, null, null, new HashMap<>(),
                new ArrayList<>());
        BatchSettings settings = batch.getSettings();
        QueryAllowance allowance = panelRunnerFactory.allowance(QUERIES_FOR_A_FIX,
                settings != null && settings.getQueriesAtOnce() > 0 ? settings.getQueriesAtOnce() : DEFAULT_AT_ONCE);
        PanelDecision decision = panelRunner.assessGiven(work, quizId, adjustment.getCandidate(),
                new PanelRunSettings(settings != null ? settings.getTopicsAtOnce() : DEFAULT_AT_ONCE,
                        settings != null ? settings.getQueriesAtOnce() : DEFAULT_AT_ONCE,
                        settings != null ? settings.getMaxAttempts() : DEFAULT_MAX_ATTEMPTS,
                        BatchBook.copy(batch.getAnalyzers())), allowance);
        batch.setQueriesSpent(batch.getQueriesSpent() + (allowance != null ? allowance.spent() : 0));
        List<PanelDecision> decisions = BatchBook.copy(batch.getPanelDecisions());
        decisions.add(decision);
        batch.setPanelDecisions(decisions);
        if (decision.getOutcome() == PanelOutcomeKind.ACCEPTED) {
            batch = accept(batch, adjustment, course, decision.getAcceptedVerdict(), "el arreglo de José pasó la mesa");
            // F-MESA-R007 inv. 4: a task José's fix only improved stays open, with its measure before and after.
            for (String taskId : BatchBook.list(decision.getImprovedTaskIds())) {
                if (BatchBook.list(decision.getResolvedTaskIds()).contains(taskId)
                        || !BatchBook.list(adjustment.getTaskIds()).contains(taskId)) {
                    continue;
                }
                BatchTaskOutcome was = BatchBook.outcome(batch, taskId).orElse(null);
                BatchBook.setOutcome(batch, taskId, BatchTaskOutcomeKind.IMPROVED,
                        was != null ? was.getChangeId() : null, was != null ? was.getAttempts() : 0,
                        "mejoró, sigue abierta: " + decision.getImprovements().getOrDefault(taskId, ""));
            }
            return batch;
        }
        List<String> reasons = new ArrayList<>();
        for (CandidateRejection rejection : BatchBook.list(decision.getRejections())) {
            reasons.addAll(BatchBook.list(rejection.getReasons()));
        }
        BatchTaskOutcomeKind kind = decision.getOutcome() == PanelOutcomeKind.TIE ? BatchTaskOutcomeKind.TIE
                : BatchTaskOutcomeKind.NO_WINNER;
        for (String taskId : BatchBook.list(adjustment.getTaskIds())) {
            int attempts = BatchBook.outcome(batch, taskId).map(BatchTaskOutcome::getAttempts).orElse(0);
            BatchBook.setOutcome(batch, taskId, kind, null, attempts, "el arreglo de José no pasó"
                    + (reasons.isEmpty() ? "" : ": " + String.join("; ", reasons)));
        }
        return batch;
    }

    /**
     * F-BANDEJA-R002: José says a tie on an exercise of the course is an error. Its task enters the batch -- one task of
     * the exercise, with the tie as its finding -- and waits for the panel of its topic.
     */
    private CorrectionBatch taskFromTie(CorrectionBatch batch, BatchAdjustment adjustment, CourseEntity course) {
        Finding tie = adjustment.getFinding();
        String quizId = adjustment.getQuizId() != null ? adjustment.getQuizId()
                : tie != null && tie.getNode() != null ? tie.getNode().getNodeId() : null;
        Optional<BatchBook.Place> place = BatchBook.place(course, quizId);
        if (tie == null || place.isEmpty()) {
            throw new IllegalArgumentException("El desempate necesita su hallazgo y un ejercicio del curso.");
        }
        RefinementPlan plan = refinementPlanStore.load(batch.getPlanId()).orElseThrow(() -> new IllegalStateException(
                "No se encuentra el plan " + batch.getPlanId() + " de la tanda."));
        String taskId = "desempate-" + quizId;
        for (int n = 2; BatchBook.tasksById(plan).containsKey(taskId); n++) {
            taskId = "desempate-" + quizId + "-" + n;
        }
        DiagnosisKind kind;
        try {
            kind = DiagnosisKind.valueOf(tie.getAnalyzer().toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException | NullPointerException e) {
            kind = null;
        }
        KnowledgeEntity knowledge = place.get().knowledge();
        RefinementTask task = new RefinementTask(taskId, AuditTarget.QUIZ, quizId, quizId, kind, 1,
                RefinementTaskStatus.IN_BATCH, new TaskOrigin(tie, batch.getLevel(), knowledge.getId(), knowledge.getLabel(),
                        new ArrayList<>(List.of(quizId)), new ArrayList<>()));
        List<RefinementTask> planTasks = BatchBook.copy(plan.getTasks());
        planTasks.add(task);
        plan.setTasks(planTasks);
        refinementPlanStore.save(plan);
        List<String> taskIds = BatchBook.copy(batch.getTaskIds());
        taskIds.add(taskId);
        batch.setTaskIds(taskIds);
        adjustment.setTaskIds(new ArrayList<>(List.of(taskId)));
        BatchBook.setOutcome(batch, taskId, BatchTaskOutcomeKind.PENDING, null, 0,
                "José dijo que el desempate es un error: a la mesa de su tema");
        return batch;
    }

    // -------------------------------------------------------------------------

    private CorrectionBatch requireOpen() {
        return batchStore.open().orElseThrow(() -> new NoOpenBatchException(
                "No hay una tanda abierta: abrila con 'content-audit batch open <nivel>'."));
    }

    /**
     * F-CJUEZ-R003 inv. 4: a request is refused, before the course is read or a judge consulted, when a tope is negative or
     * names something that is not a judge of the batch -- the judges of the batch, in the order of the catalog, are the ones
     * its analyzers that ask a model or bring a local tool.
     */
    private void validate(CorrectionBatch batch, BatchCloseRequest request) {
        Map<String, Integer> budgets = request.getJudgeBudgets() != null ? request.getJudgeBudgets() : Map.of();
        for (Map.Entry<String, Integer> budget : budgets.entrySet()) {
            if (budget.getValue() != null && budget.getValue() < 0) {
                throw new IllegalArgumentException("El tope tiene que ser 0 o más: --budget " + budget.getKey() + "="
                        + budget.getValue() + ".");
            }
        }
        if (request.getQueryCap() != null && request.getQueryCap() < 0) {
            throw new IllegalArgumentException("El tope tiene que ser 0 o más: --cap " + request.getQueryCap() + ".");
        }
        if (budgets.isEmpty()) {
            return;
        }
        List<String> judges = judgePass.judges(batch);
        for (String name : budgets.keySet()) {
            if (!judges.contains(name)) {
                throw new IllegalArgumentException("«" + name + "» no es un juez de la tanda " + batch.getId()
                        + ". Sus jueces: " + (judges.isEmpty() ? "ninguno" : String.join(", ", judges)) + ".");
            }
        }
    }

    private AuditReport start(CorrectionBatch batch) {
        return contentProjection.apply(auditReportStore.load(batch.getStartAuditId()).orElseThrow(() -> new IllegalStateException(
                "No se encuentra el análisis " + batch.getStartAuditId() + " del que partió la tanda.")));
    }

    /** The plan of an analysis: the active pair's, or the latest plan made from it. */
    private String planOf(String auditId) {
        Optional<ActiveAnalysisSelection> active = activeAnalysisSelectionStore != null
                ? activeAnalysisSelectionStore.read() : Optional.empty();
        if (active.isPresent() && auditId.equals(active.get().getAuditId()) && active.get().getPlanId() != null) {
            return active.get().getPlanId();
        }
        Optional<RefinementPlan> latest = refinementPlanStore.loadLatest();
        if (latest.isPresent() && auditId.equals(latest.get().getSourceAuditId())) {
            return latest.get().getId();
        }
        throw new IllegalStateException("No hay un plan del análisis " + auditId + ": armalo con 'content-audit plan'.");
    }

    /** The analyzers of the analysis the batch starts from: the recalculation runs the same ones. */
    private static List<String> analyzersOf(AuditReport start) {
        List<String> analyzers = new ArrayList<>();
        if (start != null && start.getRoot() != null) {
            for (AnalyzerMeasure measure : BatchBook.list(start.getRoot().getMeasures())) {
                if (measure != null && measure.getAnalyzer() != null && !analyzers.contains(measure.getAnalyzer())) {
                    analyzers.add(measure.getAnalyzer());
                }
            }
        }
        return analyzers;
    }

    /** R012: per audio, the latest re-recording the published batches left pending. */
    private List<AudioRerecordRequest> pendingRerecords() {
        List<CorrectionBatch> published = new ArrayList<>();
        for (CorrectionBatch batch : BatchBook.list(batchStore.list())) {
            if (batch != null && batch.getStatus() == BatchStatus.PUBLISHED && batch.getPublication() != null) {
                published.add(batch);
            }
        }
        published.sort(Comparator.comparing(b -> b.getPublication().getPublishedAt(),
                Comparator.nullsFirst(Comparator.naturalOrder())));
        Map<String, AudioRerecordRequest> latest = new LinkedHashMap<>();
        for (CorrectionBatch batch : published) {
            for (AudioRerecordRequest request : BatchBook.list(batch.getPublication().getRerecords())) {
                String key = request.getAudioKey() != null && !request.getAudioKey().isBlank() ? request.getAudioKey()
                        : request.getQuizId();
                latest.remove(key);
                latest.put(key, request);
            }
        }
        return new ArrayList<>(latest.values());
    }

    private static int atOnce(Integer asked) {
        return asked != null && asked > 0 ? asked : DEFAULT_AT_ONCE;
    }

    private static String knowledgeOfQuiz(CourseEntity course, String quizId) {
        return BatchBook.place(course, quizId).map(p -> p.knowledge().getId()).orElse(null);
    }
}
