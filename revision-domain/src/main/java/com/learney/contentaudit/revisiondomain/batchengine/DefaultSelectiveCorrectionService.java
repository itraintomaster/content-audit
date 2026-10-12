package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.SelectiveCorrection.*;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** An explicit, append-only supplement. Never runs the general rule stage, conversions or closing rollback loop. */
final class DefaultSelectiveCorrectionService implements SelectiveCorrectionService {
    static final String JUDGE = "multiple-choice-quality";
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final DefaultPanelStage panels;
    private final BatchReviewService reviews;
    private final Function<CourseEntity, String> fingerprint;
    private final String correctorRuntime;

    DefaultSelectiveCorrectionService(BatchServiceConfig config, BatchChangeLedger ledger,
            DefaultPanelStage panels, BatchReviewService reviews, Function<CourseEntity, String> fingerprint) {
        this(config, ledger, panels, reviews, fingerprint, "NOT_RECORDED");
    }
    DefaultSelectiveCorrectionService(BatchServiceConfig config, BatchChangeLedger ledger,
            DefaultPanelStage panels, BatchReviewService reviews, Function<CourseEntity, String> fingerprint, String correctorRuntime) {
        this.config = config; this.ledger = ledger; this.panels = panels; this.reviews = reviews;
        this.fingerprint = fingerprint;
        this.correctorRuntime = correctorRuntime;
    }

    @Override public State execute(Request r) {
        Objects.requireNonNull(r, "Falta el pedido.");
        String judge = r.analyzer();
        var supported = config.getCurrentAnalysisService() == null ? java.util.Set.of(JUDGE) : config.getCurrentAnalysisService().supportedAnalyzers();
        if (blank(judge) || !supported.contains(judge)) fail("El juez no declara una huella de integración: " + judge);
        if ("correct".equals(r.action()) && !JUDGE.equals(judge)) fail("La corrección selectiva de este recorrido es sólo de distractores.");
        if (!List.of("read", "estimate", "analyze", "reanalyze", "analyze-base", "reanalyze-base", "correct").contains(Objects.toString(r.action(), "")))
            fail("Operación selectiva desconocida.");
        CorrectionBatch batch = batch(r.batchId());
        if (!List.of("read", "estimate").contains(r.action())) {
            if (blank(r.operationId()) || !r.operationId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
                fail("Falta una identidad de operación válida.");
            Optional<Run> existing = BatchBook.list(batch.getSelectiveRuns()).stream()
                    .filter(run -> r.operationId().equals(run.id())).findFirst();
            if (existing.isPresent()) {
                Run run = existing.get();
                if (!Objects.equals(run.analyzer(), judge) || !Objects.equals(run.action(), recordedAction(r)) || !Objects.equals(run.sourceRunId(), r.sourceRunId())
                        || !Objects.equals(run.judgeVersion(), r.judgeVersion())
                        || !Objects.equals(run.fingerprint(), r.fingerprint())
                        || !Objects.equals(run.contentVersion(), r.contentVersion())
                        || run.includeApproved() != r.includeApproved()
                        || !Objects.equals(run.reviewer(), r.reviewer()) || !Objects.equals(run.queryCap(), r.queryCap())
                        || ("correct".equals(r.action()) && !new HashSet<>(run.taskIds()).equals(new HashSet<>(safe(r.taskIds())))))
                    fail("La identidad ya pertenece a otro pedido; no se puede relanzar.");
                AuditReport recorded = run.auditId() == null ? null : config.getAuditReportStore().load(run.auditId()).orElse(null);
                return state(batch, recorded, null, judge); // Finished or uncertain operations are never paid twice.
            }
        }
        CourseEntity start = config.getCourseRepository().load(config.getCoursePath());
        boolean base = "base".equals(r.state()) || r.action().endsWith("-base");
        if (r.state() != null && !List.of("base", "correction").contains(r.state())) fail("Elegí la base o la corrección.");
        if (base && "correct".equals(r.action())) fail("Las correcciones se aplican sólo a la corrección vigente.");
        CourseEntity cut = level(base ? start : ledger.composedCourse(batch, start), batch.getLevel());
        AuditReport trial = audit(cut, judge, 0, r.force() || r.action().startsWith("reanalyze"));
        State estimate = state(batch, trial, cut, judge);
        if (List.of("read", "estimate").contains(r.action())) return estimate;
        if (batch.getStatus() != BatchStatus.CLOSED) fail("La tanda debe estar cerrada y sin otra operación activa.");
        if (r.contentVersion() == null || r.contentVersion() != batch.getContentVersion()
                || !Objects.equals(r.fingerprint(), estimate.fingerprint())
                || !Objects.equals(r.judgeVersion(), estimate.judgeVersion()))
            fail("El contenido o la versión del juez cambió: volvé a estimar.");
        if (!estimate.allowed()) fail(estimate.reason());
        if (r.queryCap() == null || r.queryCap() < 0) fail("Elegí un tope nuevo de consultas, 0 o más.");
        if (blank(r.reviewer())) fail("Falta el autor de la operación.");
        Run source = null;
        List<RefinementTask> selected = List.of();
        if ("correct".equals(r.action())) {
            source = source(batch, r.sourceRunId());
            if (source.contentVersion() != batch.getContentVersion()
                    || !Objects.equals(source.fingerprint(), estimate.fingerprint())
                    || !Objects.equals(source.judgeVersion(), estimate.judgeVersion()))
                fail("La medición elegida ya no describe este contenido: analizá nuevamente.");
            if (!List.of("DONE", "PARTIAL").contains(source.status())) fail("La medición no terminó con resultados guardados.");
            selected = selected(source, r.taskIds());
            for (RefinementTask task : selected)
                if (BatchBook.list(batch.getTaskIds()).contains(task.getId()))
                    fail("Esa tarea ya entró en una corrida; volvé a analizar antes de reintentar.");
            Set<String> approved = approved(batch);
            if (!r.includeApproved() && selected.stream().anyMatch(t -> approved.contains(t.getNodeId())))
                fail("Hay quizzes aprobados en el alcance: confirmá explícitamente su inclusión.");
            JudgeCoverageDiagnosis uniqueness = coverage(audit(cut, "multiple-choice-validity", 0), "multiple-choice-validity");
            if (!allowed(uniqueness)) fail("La comprobación de respuesta única no está habilitada; no se corrige.");
        }
        List<String> ids = selected.stream().map(RefinementTask::getId).toList();
        List<String> quizzes = selected.stream().map(RefinementTask::getNodeId).distinct().toList();
        Run run = new Run(r.operationId(), recordedAction(r), judge, r.sourceRunId(), BatchBook.correctionRevision(batch),
                batch.getContentVersion(), estimate.fingerprint(), estimate.judgeVersion(), estimate.runtime(),
                null, null, ids, quizzes, r.queryCap(), r.queryCap(), 0, 0, 0, 0, "RUNNING", r.reviewer(),
                Instant.now(), null, null, r.includeApproved(), batch.getContentVersion(), estimate.fingerprint(),
                "correct".equals(r.action()) ? correctorRuntime : null);
        if (!base && config.getCurrentAnalysisService() != null) config.getCurrentAnalysisService().seed(batch, start);
        put(batch, run);
        batch.setStatus(BatchStatus.RUNNING);
        // The entire allowance is durable before the first call. An uncertain run needs a new human decision,
        // not an automatic replay that would recover this reservation as spendable balance.
        config.getBatchStore().saveIfVersion(batch, r.contentVersion(), BatchStatus.CLOSED);
        QueryAllowance allowance = null;
        int initialSpent = batch.getQueriesSpent();
        try {
            AuditReport result;
            if (!"correct".equals(r.action())) {
                // The allowance is for the whole run, including judges with multiple evaluators.
                int evaluatorCount = Math.max(1, BatchBook.list(coverage(trial, judge).getEvaluators()).size());
                result = audit(cut, judge, r.queryCap() / evaluatorCount, r.force() || r.action().startsWith("reanalyze"));
                String auditId = config.getAuditReportStore().save(result);
                RefinementPlan plan = config.getRefinerEngine().plan(result, auditId);
                List<RefinementTask> tasks = BatchBook.list(plan.getTasks()).stream()
                        .filter(t -> t.getNodeTarget() == AuditTarget.QUIZ && t.getOrigin() != null
                                && t.getOrigin().getFinding() != null
                                && judge.equals(t.getOrigin().getFinding().getAnalyzer()))
                        .toList();
                for (RefinementTask t : tasks) t.setId("selective-" + run.id() + "-" + t.getId());
                plan.setTasks(new ArrayList<>(tasks));
                plan.setId("selective-" + run.id());
                config.getRefinementPlanStore().save(plan);
                run = evaluated(run, result, auditId, plan.getId(), tasks.stream().map(RefinementTask::getId).toList(),
                        tasks.stream().map(RefinementTask::getNodeId).distinct().toList(), spent(result, judge));
                batch.setQueriesSpent(batch.getQueriesSpent() + run.spent());
            } else {
                RefinementPlan plan = config.getRefinementPlanStore().load(batch.getPlanId()).orElseThrow();
                List<RefinementTask> joined = BatchBook.copy(plan.getTasks());
                List<String> taken = BatchBook.copy(batch.getTaskIds());
                for (RefinementTask task : selected) {
                    if (taken.contains(task.getId())) fail("Esa tarea ya entró en una corrida; volvé a analizar antes de reintentar.");
                    joined.add(new RefinementTask(task.getId(), task.getNodeTarget(), task.getNodeId(), task.getNodeLabel(),
                            task.getDiagnosisKind(), task.getPriority(), RefinementTaskStatus.IN_BATCH, task.getOrigin()));
                    taken.add(task.getId());
                }
                plan.setTasks(joined); config.getRefinementPlanStore().save(plan); batch.setTaskIds(taken);
                batch.setContentVersion(batch.getContentVersion() + 1);
                config.getBatchStore().save(batch);
                allowance = config.getPanelRunnerFactory().allowance(r.queryCap(), 1);
                List<String> validation = BatchBook.copy(batch.getAnalyzers());
                if (!validation.contains(JUDGE)) validation.add(JUDGE);
                AuditReport sourceReport = config.getAuditReportStore().load(source.auditId()).orElseThrow();
                batch = panels.runSelected(batch, sourceReport, start, allowance, new HashSet<>(ids), validation);
                // The native panel judged candidates; this zero-budget pass records/reuses their final verdicts.
                result = audit(level(ledger.composedCourse(batch, start), batch.getLevel()), JUDGE, 0);
                String auditId = config.getAuditReportStore().save(result);
                run = evaluated(run, result, auditId, batch.getPlanId(), ids, quizzes, allowance.spent());
                Optional<String> stopped = allowance.tripped();
                boolean pending = false;
                for (String id : ids) pending |= BatchBook.kind(batch, id) == BatchTaskOutcomeKind.NOT_ATTEMPTED
                        || BatchBook.kind(batch, id) == BatchTaskOutcomeKind.PENDING;
                if (stopped.isPresent() || pending)
                    run = finish(run, "PARTIAL", run.spent(), stopped.orElse("Quedaron tareas sin intentar dentro del tope."));
            }
        } catch (RuntimeException e) {
            int spent = allowance != null ? allowance.spent() : 0;
            if (allowance != null) batch.setQueriesSpent(initialSpent + spent);
            run = finish(run, "NEEDS_REVIEW", spent, e.getMessage()
                    + ". La reserva se conserva; no relanzar esta identidad ni asumir saldo.");
            // For a failure, reservation is deliberately not reported as actual consolidated spend.
            // Native results/evaluation ledger survive; there is no silent recovery.
        }
        run = new Run(run.id(), run.action(), run.analyzer(), run.sourceRunId(), run.correctionRevision(), run.contentVersion(),
                run.fingerprint(), run.judgeVersion(), run.runtime(), run.auditId(), run.planId(), run.taskIds(), run.quizIds(),
                run.queryCap(), run.reserved(), run.spent(), run.reached(), run.evaluated(), run.missing(), run.status(),
                run.reviewer(), run.createdAt(), run.finishedAt(), run.detail(), run.includeApproved(), batch.getContentVersion(),
                fingerprint.apply(level(ledger.composedCourse(batch, start), batch.getLevel())), run.correctorRuntime());
        put(batch, run);
        batch.setStatus(BatchStatus.CLOSED);
        config.getBatchStore().save(batch);
        // A finished pass carries real coverage and exam standing; never fabricate an unavailable judge from null.
        AuditReport finished = run.auditId() == null ? trial : config.getAuditReportStore().load(run.auditId()).orElse(trial);
        return state(batch, finished, cut, judge);
    }

    private State state(CorrectionBatch b, AuditReport report, CourseEntity cut, String judge) {
        JudgeCoverageDiagnosis c = coverage(report, judge);
        Run latest = BatchBook.list(b.getSelectiveRuns()).stream().filter(x -> judge.equals(x.analyzer()) && List.of("analyze", "reanalyze").contains(x.action()))
                .reduce((a, z) -> z).orElse(null);
        List<Task> tasks = new ArrayList<>();
        Set<String> approved = approved(b);
        if (latest != null && latest.planId() != null) {
            config.getRefinementPlanStore().load(latest.planId()).ifPresent(p -> {
                for (RefinementTask t : BatchBook.list(p.getTasks())) {
                    var f = t.getOrigin() != null ? t.getOrigin().getFinding() : null;
                    tasks.add(new Task(t.getId(), t.getNodeId(), f != null ? f.getRule() : "",
                            f != null && f.getEvidence() != null ? f.getEvidence().getObservation() : "",
                            approved.contains(t.getNodeId()), BatchBook.list(b.getTaskIds()).contains(t.getId()),
                            BatchBook.kind(b, t.getId()).name()));
                }
            });
        }
        boolean uncertain = BatchBook.list(b.getSelectiveRuns()).stream()
                .anyMatch(r -> List.of("RUNNING", "NEEDS_REVIEW").contains(r.status()));
        boolean enabled = b.getStatus() == BatchStatus.CLOSED && !uncertain && allowed(c);
        String reason = enabled ? null : uncertain ? "Hay una operación interrumpida o incierta. Conservá su reserva y conciliá los resultados antes de iniciar otra."
                : b.getStatus() != BatchStatus.CLOSED ? "Hay otra operación activa o la tanda no está cerrada."
                : c != null && c.getUnavailableReason() != null ? c.getUnavailableReason()
                : c != null && c.getStanding() != null ? c.getStanding().getReason() : "El juez debe aprobar su examen vigente.";
        return new State(b.getId(), BatchBook.correctionRevision(b), b.getContentVersion(), judge,
                cut != null ? fingerprint.apply(cut) : null, c != null && c.getStanding() != null ? c.getStanding().getJudgeVersion() : null,
                c != null && c.getStanding() != null ? Objects.toString(c.getStanding().getRuntimeIdentity(), "") : null,
                enabled, reason, c != null ? c.getReached() : 0, c != null ? c.getEvaluated() : 0,
                c != null ? c.getNotEvaluated() : 0,
                c != null && c.getCost() != null ? c.getCost().getPendingConsultations() : 0,
                c != null ? BatchBook.list(c.getEvaluators()).stream().filter(e -> e.getCoverage() != null)
                        .mapToInt(e -> e.getCoverage().getReused()).sum() : 0,
                tasks, BatchBook.copy(b.getSelectiveRuns()), config.getAnalyzerCatalog() == null ? judge
                        : config.getAnalyzerCatalog().find(judge).map(com.learney.contentaudit.auditdomain.AnalyzerDescriptor::getDisplayName).orElse(judge));
    }

    private Set<String> approved(CorrectionBatch b) {
        var result = new HashSet<String>();
        for (BatchReview.Item item : reviews.read(b.getId()).items())
            if (item.status() == BatchReviewDecision.Status.APPROVED) result.add(item.step().getQuizId());
        return result;
    }
    private List<RefinementTask> selected(Run source, List<String> ids) {
        if (safe(ids).isEmpty() || new HashSet<>(ids).size() != ids.size()) fail("Elegí tareas distintas de esta medición.");
        Map<String, RefinementTask> tasks = config.getRefinementPlanStore().load(source.planId()).map(BatchBook::tasksById).orElse(Map.of());
        var result = new ArrayList<RefinementTask>();
        for (String id : ids) {
            RefinementTask t = tasks.get(id);
            if (!source.taskIds().contains(id) || t == null || t.getOrigin() == null || t.getOrigin().getFinding() == null
                    || !JUDGE.equals(t.getOrigin().getFinding().getAnalyzer()) || t.getNodeTarget() != AuditTarget.QUIZ)
                fail("La tarea no pertenece al juez y a la medición elegidos: " + id);
            result.add(t);
        }
        return result;
    }
    private CorrectionBatch batch(String id) {
        if (blank(id)) fail("Falta la tanda.");
        return config.getBatchStore().load(id).orElseThrow(() -> new IllegalArgumentException("No existe la tanda."));
    }
    private static Run source(CorrectionBatch b, String id) {
        return BatchBook.list(b.getSelectiveRuns()).stream().filter(r -> Objects.equals(id, r.id()) && List.of("analyze", "reanalyze").contains(r.action()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Elegí una medición selectiva guardada."));
    }
    private AuditReport audit(CourseEntity course, String analyzer, int cap) {
        return audit(course, analyzer, cap, false);
    }
    private AuditReport audit(CourseEntity course, String analyzer, int cap, boolean force) {
        return config.getAuditEngine().runAudit(config.getCourseMapper().map(course),
                new AnalyzerRunSelection(List.of(analyzer), Map.of(analyzer, new EvaluationRunPolicy(cap, force, null, null, null, 1))));
    }
    private static JudgeCoverageDiagnosis coverage(AuditReport report, String analyzer) {
        if (report != null && report.getRoot() != null && report.getRoot().getDiagnoses() instanceof CourseDiagnoses d)
            return BatchBook.list(d.getJudgeCoverages()).stream().filter(c -> analyzer.equals(c.getJudgeName())).findFirst().orElse(null);
        return null;
    }
    private static boolean allowed(JudgeCoverageDiagnosis c) {
        return c != null && c.getStanding() != null && c.getStanding().isConsultsOnCourse() && c.getUnavailableReason() == null;
    }
    private static int spent(AuditReport r, String judge) {
        JudgeCoverageDiagnosis c = coverage(r, judge);
        return c == null ? 0 : BatchBook.list(c.getEvaluators()).stream().filter(e -> e.getCoverage() != null)
                .mapToInt(e -> e.getCoverage().getEvaluatedInRun()).sum();
    }
    private static CourseEntity level(CourseEntity c, CefrLevel level) {
        CourseEntity result = new CourseEntity(c.getId(), c.getTitle(), c.getKnowledgeIds(), null, c.getSlug());
        RootNodeEntity root = new RootNodeEntity(); root.setId(c.getRoot().getId()); root.setLabel(c.getRoot().getLabel());
        root.setMilestones(BatchBook.list(c.getRoot().getMilestones()).stream()
                .filter(m -> level.name().equalsIgnoreCase(m.getLabel())).toList());
        if (root.getMilestones().isEmpty()) fail("No existe el nivel de la tanda en el curso.");
        result.setRoot(root); return result;
    }
    private static Run evaluated(Run r, AuditReport report, String audit, String plan, List<String> ids, List<String> quizzes, int spent) {
        JudgeCoverageDiagnosis c = coverage(report, r.analyzer());
        Run evaluated = new Run(r.id(), r.action(), r.analyzer(), r.sourceRunId(), r.correctionRevision(), r.contentVersion(),
                r.fingerprint(), r.judgeVersion(), r.runtime(), audit, plan, ids, quizzes, r.queryCap(), r.reserved(), spent,
                c != null ? c.getReached() : 0, c != null ? c.getEvaluated() : 0, c != null ? c.getNotEvaluated() : 0,
                c != null && c.getNotEvaluated() == 0 && c.getUnavailableReason() == null ? "DONE" : "PARTIAL",
                r.reviewer(), r.createdAt(), Instant.now(), c != null ? c.getUnavailableReason() : "Sin cobertura comprobable.",
                r.includeApproved(), r.resultContentVersion(), r.resultFingerprint(), r.correctorRuntime());
        return evaluated;
    }
    private static Run finish(Run r, String status, int spent, String detail) {
        return new Run(r.id(), r.action(), r.analyzer(), r.sourceRunId(), r.correctionRevision(), r.contentVersion(),
                r.fingerprint(), r.judgeVersion(), r.runtime(), r.auditId(), r.planId(), r.taskIds(), r.quizIds(),
                r.queryCap(), r.reserved(), spent, r.reached(), r.evaluated(), r.missing(), status, r.reviewer(),
                r.createdAt(), Instant.now(), detail, r.includeApproved(), r.resultContentVersion(), r.resultFingerprint(), r.correctorRuntime());
    }
    private static void put(CorrectionBatch b, Run run) {
        var runs = BatchBook.copy(b.getSelectiveRuns()); runs.removeIf(r -> r.id().equals(run.id())); runs.add(run);
        b.setSelectiveRuns(runs);
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String recordedAction(Request request) {
        String action = request.force() && "analyze".equals(request.action()) ? "reanalyze" : request.action();
        return "base".equals(request.state()) && !action.endsWith("-base") ? action + "-base" : action;
    }
    private static <T> List<T> safe(List<T> l) { return l == null ? List.of() : l; }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
