package com.learney.contentaudit.revisiondomain.batchengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.CourseElementLocator;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrection.*;
import com.learney.contentaudit.revisiondomain.panel.QueryAllowance;
import com.learney.contentaudit.revisiondomain.panel.DidacticReference;
import com.learney.contentaudit.revisiondomain.panelengine.DidacticContracts;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** No judge is run to estimate. The panel keeps its native budget, preservation and acceptance gates. */
final class DefaultQuizCorrectionService implements QuizCorrectionService {
    private static final String ACTION = "recorrect";
    private static final String NOTE = "El tope incluye propuestas, validaciones y reintentos; el consumo depende de los cambios. "
            + "La cantidad de quizzes es una referencia de propuestas, no una estimación del total ni un gasto mínimo obligatorio.";
    private static final ObjectMapper JSON = mapper();
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final DefaultPanelStage panels;
    private final BatchReviewService reviews;
    private final Function<CourseEntity, String> courseFingerprint;
    private final String runtime;
    private final CourseElementLocator elementLocator;

    private static ObjectMapper mapper() {
        var times = new com.fasterxml.jackson.databind.module.SimpleModule();
        times.addSerializer(Instant.class, new com.fasterxml.jackson.databind.JsonSerializer<Instant>() {
            @Override public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator generator,
                    com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                generator.writeString(value.toString());
            }
        });
        return new ObjectMapper().registerModule(times).enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    DefaultQuizCorrectionService(BatchServiceConfig config, BatchChangeLedger ledger, DefaultPanelStage panels,
            BatchReviewService reviews, Function<CourseEntity, String> courseFingerprint, String runtime,
            CourseElementLocator elementLocator) {
        this.config = config; this.ledger = ledger; this.panels = panels; this.reviews = reviews;
        this.courseFingerprint = courseFingerprint; this.runtime = Objects.toString(runtime, "NOT_RECORDED");
        this.elementLocator = Objects.requireNonNull(elementLocator, "Falta el acceso a las versiones del ejercicio.");
    }

    @Override public State execute(Request request) {
        Objects.requireNonNull(request, "Falta el pedido de corrección.");
        if (!List.of("estimate", "correct", "compare").contains(Objects.toString(request.action(), "")))
            throw new IllegalArgumentException("Elegí estimar, comparar o volver a corregir.");
        CorrectionBatch batch = batch(request.batchId());
        if ("compare".equals(request.action())) return compare(batch, request.operationId());
        List<String> selected = selection(request.quizIds());
        if (request.restoreRequestFormula() && request.sourceOperationId() == null)
            throw new IllegalArgumentException("Cerrar la fórmula requiere una referencia histórica explícita.");
        if ("correct".equals(request.action())) {
            if (request.operationId() == null || !request.operationId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,100}"))
                throw new IllegalArgumentException("Falta una identidad de operación válida.");
            var existing = safe(batch.getSelectiveRuns()).stream().filter(r -> request.operationId().equals(r.id())).findFirst();
            if (existing.isPresent()) {
                var old = existing.get();
                if (!ACTION.equals(old.action()) || !Objects.equals(old.fingerprint(), request.fingerprint())
                        || !Objects.equals(old.contentVersion(), request.contentVersion())
                        || !Objects.equals(old.queryCap(), request.queryCap()) || old.includeApproved() != request.includeApproved()
                        || !new TreeSet<>(old.quizIds()).equals(new TreeSet<>(selected))
                        || !Objects.equals(old.reviewer(), request.reviewer())
                        || !Objects.equals(old.sourceRunId(), request.sourceOperationId())
                        || request.restoreRequestFormula() != safe(batch.getComparisonBaselines()).stream()
                                .filter(b -> old.id().equals(b.operationId())).map(ComparisonBaseline::didacticReference)
                                .filter(Objects::nonNull).anyMatch(DidacticReference::restoreRequestFormula))
                    throw new IllegalArgumentException("La identidad ya pertenece a otro pedido.");
                return result(batch, old); // Neither completed nor interrupted operations are paid twice.
            }
        }
        Snapshot snapshot = inspect(batch, selected, request.includeApproved(), request.sourceOperationId(), request.restoreRequestFormula());
        if ("estimate".equals(request.action())) return state(batch, snapshot, null);
        validate(request, batch, snapshot, selected);
        final CorrectionBatch asked = batch;
        // Analysis integration and the reservation are serialized, before any model can be called.
        Reservation reservation = config.getBatchStore().withAnalysisLock(batch.getId(), () -> {
            CorrectionBatch current = batch(asked.getId());
            Snapshot fresh = inspect(current, selected, request.includeApproved(), request.sourceOperationId(), request.restoreRequestFormula());
            validate(request, current, fresh, selected);
            BatchStatus idleStatus = current.getStatus();
            var baselines = BatchBook.copy(current.getComparisonBaselines());
            for (String quizId : selected) {
                var before = elementLocator.snapshot(fresh.current(), AuditTarget.QUIZ, quizId)
                        .orElseThrow(() -> new IllegalStateException("No se pudo guardar la versión actual del quiz " + quizId));
                var original = elementLocator.snapshot(fresh.start(), AuditTarget.QUIZ, quizId)
                        .orElseThrow(() -> new IllegalStateException("No se pudo guardar el original del quiz " + quizId));
                baselines.add(new ComparisonBaseline(request.operationId(), current.getContentVersion(), quizId, before, original, fresh.references().get(quizId)));
            }
            current.setComparisonBaselines(List.copyOf(baselines));
            config.getCurrentAnalysisService().seed(current, fresh.start());
            var run = run(request, current, fresh, List.of(), null, "RUNNING", 0, null);
            put(current, run); current.setStatus(BatchStatus.RUNNING);
            config.getBatchStore().saveIfVersion(current, request.contentVersion(), idleStatus);
            return new Reservation(current, idleStatus, fresh);
        });
        batch = reservation.batch();
        snapshot = reservation.snapshot();
        SelectiveCorrection.Run run = safe(batch.getSelectiveRuns()).stream().filter(r -> r.id().equals(request.operationId())).findFirst().orElseThrow();
        QueryAllowance allowance = null;
        int initialSpent = batch.getQueriesSpent();
        try {
            String auditId = config.getAuditReportStore().save(snapshot.report());
            List<RefinementTask> freshTasks = freshTasks(snapshot, selected, request.operationId());
            RefinementPlan plan = config.getRefinementPlanStore().load(batch.getPlanId()).orElseThrow();
            var joined = BatchBook.copy(plan.getTasks()); joined.addAll(freshTasks);
            // Leave the existing sourceAuditId intact: it explains the older tasks. The run names this new source.
            plan.setTasks(joined); config.getRefinementPlanStore().save(plan);
            var taskIds = freshTasks.stream().map(RefinementTask::getId).toList();
            var taken = BatchBook.copy(batch.getTaskIds()); taken.addAll(taskIds); batch.setTaskIds(taken);
            if (!taskIds.isEmpty()) batch.setContentVersion(batch.getContentVersion() + 1);
            run = run(request, batch, snapshot, taskIds, auditId, "RUNNING", 0, null);
            put(batch, run); config.getBatchStore().save(batch);
            BatchSettings settings = batch.getSettings();
            if (taskIds.isEmpty()) {
                run = finish(run, batch, snapshot.noWork().values().stream().anyMatch(v -> v.startsWith("NOT_ATTEMPTED")) ? "PARTIAL" : "DONE", 0, null);
                put(batch, run); batch.setStatus(reservation.idleStatus()); config.getBatchStore().save(batch);
                return result(batch, run);
            }
            allowance = config.getPanelRunnerFactory().allowance(request.queryCap(),
                    settings != null ? settings.getQueriesAtOnce() : 0);
            batch = panels.runSelected(batch, snapshot.report(), snapshot.start(), allowance,
                    new LinkedHashSet<>(taskIds), snapshot.validators());
            final CorrectionBatch completed = batch;
            boolean pending = snapshot.noWork().values().stream().anyMatch(v -> v.startsWith("NOT_ATTEMPTED")) || taskIds.stream().map(id -> BatchBook.kind(completed, id))
                    .anyMatch(k -> k == BatchTaskOutcomeKind.PENDING || k == BatchTaskOutcomeKind.NOT_ATTEMPTED);
            run = finish(run, batch, allowance.tripped().isPresent() || pending ? "PARTIAL" : "DONE", allowance.spent(),
                    allowance.tripped().orElse(pending ? "Quedaron quizzes sin intentar dentro del tope." : null));
        } catch (RuntimeException failure) {
            int spent = allowance == null ? 0 : allowance.spent();
            batch.setQueriesSpent(initialSpent + spent);
            run = finish(run, batch, "NEEDS_REVIEW", spent, Objects.toString(failure.getMessage(), "La operación se interrumpió")
                    + ". La reserva se conserva; revisá los resultados antes de iniciar otra operación.");
        }
        put(batch, run); batch.setStatus(reservation.idleStatus()); config.getBatchStore().save(batch);
        return result(batch, run);
    }

    private record Reservation(CorrectionBatch batch, BatchStatus idleStatus, Snapshot snapshot) {}

    private State compare(CorrectionBatch batch, String operationId) {
        var matches = safe(batch.getSelectiveRuns()).stream()
                .filter(r -> Objects.equals(operationId, r.id()) && ACTION.equals(r.action())).toList();
        if (operationId == null || matches.size() != 1)
            throw new IllegalArgumentException("Elegí una pasada de corrección guardada de esta tanda.");
        var run = matches.get(0);
        AuditReport before = run.auditId() == null ? null : config.getAuditReportStore().load(run.auditId()).orElse(null);
        CourseEntity start = BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
        CourseEntity course = DefaultCurrentAnalysisService.cut(ledger.composedCourse(batch, start), batch.getLevel());
        boolean exact = run.finishedAt() != null && batch.getContentVersion() == run.resultContentVersion()
                && Objects.equals(run.resultFingerprint(), courseFingerprint.apply(course));
        // The current-analysis reader uses zero query budgets and compatible recorded opinions.
        // A later content version cannot masquerade as this operation's result.
        AuditReport after = exact ? config.getCurrentAnalysisService().report(batch, "correction", course) : null;
        var comparison = new DefaultQuizProblemComparison(config).compare(batch, run, before, after, exact);
        return new State(batch.getId(), BatchBook.correctionRevision(batch), batch.getContentVersion(), run.fingerprint(),
                false, null, List.of(), run.quizIds().size(), 0, "Comparación de resultados guardados; no consulta modelos.",
                List.of(), run.correctorRuntime(), run.id(), run.status(), run.spent(), List.copyOf(safe(batch.getSelectiveRuns())), comparison);
    }

    private record Snapshot(CourseEntity start, CourseEntity current, AuditReport report, List<RefinementTask> tasks,
            List<Quiz> quizzes, List<String> validators, String fingerprint, boolean allowed, String reason, int selected,
            Map<String, DidacticReference> references, Set<String> repairs, Map<String, String> noWork) {}

    private Snapshot inspect(CorrectionBatch batch, List<String> selected, boolean includeApproved, String sourceOperationId, boolean restoreRequestFormula) {
        if (config.getCurrentAnalysisService() == null) throw new IllegalStateException("Falta el análisis vigente integrado.");
        CourseEntity start = BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
        CourseEntity current = DefaultCurrentAnalysisService.cut(ledger.composedCourse(batch, start), batch.getLevel());
        AuditReport report = config.getCurrentAnalysisService().report(batch, "correction", current);
        RefinementPlan plan = config.getRefinerEngine().plan(report, "current-quiz-correction");
        List<RefinementTask> tasks = safe(plan.getTasks());
        Map<String, DidacticReference> references = DidacticReferences.resolve(batch, sourceOperationId, selected,
                restoreRequestFormula, config.getBatchStore(), ledger, courseFingerprint);
        Set<String> repairs = new LinkedHashSet<>();
        for (var ref : references.values()) {
            var place = BatchBook.place(current, ref.quizId()).orElseThrow(() -> new IllegalArgumentException("Falta el quiz actual " + ref.quizId()));
            if (!DidacticContracts.violations(ref, place.quiz(), place.knowledge().getInstructions(),
                    place.knowledge().getSentenceMode()).isEmpty()) repairs.add(ref.quizId());
        }
        var index = DefaultAnalysisComposer.index(report.getRoot());
        var approved = new TreeSet<String>();
        for (var item : reviews.read(batch.getId()).items())
            if (item.status() == BatchReviewDecision.Status.APPROVED) approved.add(item.step().getQuizId());
        var rows = new ArrayList<Quiz>();
        var noWork = new LinkedHashMap<String, String>();
        for (var node : index.values()) if (node.getTarget() == AuditTarget.QUIZ) {
            String id = node.getEntity().getId();
            var findings = new LinkedHashMap<String, Finding>();
            // Topic findings are context of every affected quiz, never silently discarded by the quiz filter.
            List<RefinementTask> reached = tasks.stream().filter(t -> reaches(t, id)).toList();
            for (AuditNode ancestor = node; ancestor != null; ancestor = ancestor.getParent())
                for (Finding finding : safe(ancestor.getFindings())) if (finding != null
                        && (ancestor == node || applies(finding, id) || reached.stream()
                                .flatMap(t -> findings(t).stream()).anyMatch(f -> key(f).equals(key(finding)))))
                    findings.putIfAbsent(key(finding), finding);
            for (var task : reached) for (var finding : findings(task)) if (finding != null) findings.putIfAbsent(key(finding), finding);
            if (findings.values().stream().noneMatch(this::problem) && reached.isEmpty() && !repairs.contains(id)) {
                if (references.containsKey(id)) {
                    boolean pending = !safe(node.getUnevaluatedBy()).isEmpty() || !safe(node.getPartiallyEvaluatedBy()).isEmpty();
                    noWork.put(id, pending ? "NOT_ATTEMPTED (sin tarea vigente; quedan controles pendientes)"
                            : "NO_LONGER_HOLDS (sin tareas de corrección vigentes; se conservan las opiniones y no implica aprobación)");
                } else if (findings.isEmpty()) continue;
            }
            String reason = null;
            if (reached.isEmpty() && !repairs.contains(id) && !noWork.containsKey(id)) reason = "Estos problemas sólo tienen una opinión; todavía no hay una tarea con alcance de corrección declarado.";
            for (var finding : findings.values()) {
                if (!problem(finding)) continue; // Context, never an edit permission or a resolution requirement.
                boolean covered = reached.stream().anyMatch(t -> findings(t).stream().anyMatch(f -> key(f).equals(key(finding))));
                if (!covered) { reason = "Sin tarea de corrección declarada para " + finding.getAnalyzer() + ": " + finding.getRule(); break; }
            }
            if (reason == null) for (var task : reached) {
                if (task.getNodeTarget() != AuditTarget.QUIZ) { reason = "El problema requiere corregir el tema completo; no admite selección por quiz."; break; }
                var brief = config.getBriefer() == null ? Optional.<com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief>empty()
                        : config.getBriefer().brief(report, task);
                if (brief.isEmpty() || !brief.get().isScopeDeclared()) { reason = "Hay una tarea sin alcance de corrección declarado: " + task.getDiagnosisKind(); break; }
                if (safe(brief.get().getEditScope()).contains(ExercisePart.TOPIC_INSTRUCTIONS)) {
                    reason = "La corrección cambia la consigna del tema y requiere un alcance más amplio."; break;
                }
            }
            rows.add(new Quiz(id, approved.contains(id), reason == null, reason, List.copyOf(findings.values())));
        }
        List<String> validators = config.getAnalyzerCatalog() == null ? new ArrayList<>(safe(batch.getAnalyzers()))
                : config.getAnalyzerCatalog().list().stream().map(AnalyzerDescriptor::getName).distinct().sorted().toList();
        var all = new TreeSet<>(validators);
        rows.stream().flatMap(q -> q.findings().stream()).map(Finding::getAnalyzer).filter(Objects::nonNull).forEach(all::add);
        validators = List.copyOf(all);
        String reason = !Set.of(BatchStatus.OPEN, BatchStatus.CLOSED).contains(batch.getStatus())
                ? "La corrección debe estar disponible y sin otra operación activa." : null;
        if (reason == null && config.getBatchStore().hasSuccessor(batch.getId()))
            reason = "Esta corrección está conservada: elegí la última para volver a corregir.";
        if (reason == null && safe(batch.getSelectiveRuns()).stream().anyMatch(r -> Set.of("RUNNING", "NEEDS_REVIEW").contains(r.status())))
            reason = "Hay una operación abierta o interrumpida: revisala antes de volver a corregir.";
        if (reason == null && safe(batch.getOpinionRuns()).stream().anyMatch(r -> Set.of("RUNNING", "NEEDS_REVIEW").contains(r.status())))
            reason = "Hay una medición de contexto abierta o interrumpida.";
        Map<String, Quiz> byId = new HashMap<>(); rows.forEach(q -> byId.put(q.quizId(), q));
        for (String id : selected) {
            Quiz quiz = byId.get(id);
            if (quiz == null) { reason = "El quiz ya no tiene problemas vigentes: " + id; break; }
            if (!quiz.correctable()) { reason = quiz.reason(); break; }
            if (quiz.approved() && !includeApproved) { reason = "Hay quizzes aprobados: confirmá explícitamente su inclusión."; break; }
        }
        String stamp = stamp(batch, current, report, rows, selected, includeApproved, validators, references);
        return new Snapshot(start, current, report, tasks, rows, validators, stamp, reason == null, reason, selected.size(), references, Set.copyOf(repairs), Map.copyOf(noWork));
    }

    private String stamp(CorrectionBatch batch, CourseEntity current, AuditReport report, List<Quiz> rows,
            List<String> selected, boolean includeApproved, List<String> validators, Map<String, DidacticReference> references) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("didacticReferences", new TreeMap<>(references));
        payload.put("course", courseFingerprint.apply(current)); payload.put("version", batch.getContentVersion());
        payload.put("runtime", runtime); payload.put("selected", selected.stream().sorted().toList());
        payload.put("includeApproved", includeApproved); payload.put("quizzes", rows);
        payload.put("selections", config.getBatchStore().analysisSelections(batch.getId()).stream().map(Object::toString).toList());
        var opinions = new TreeMap<String, Object>();
        for (var node : DefaultAnalysisComposer.index(report.getRoot()).values()) {
            var versions = new TreeMap<String, String>();
            for (String analyzer : validators) versions.put(analyzer, DefaultAnalysisComposer.version(node, analyzer, report));
            opinions.put(node.getEntity() == null ? "root" : node.getEntity().getId(), Arrays.asList(versions, node.getScores(),
                    node.getMetadata() == null ? null : node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE)));
        }
        payload.put("opinions", opinions);
        var currentJudges = new TreeMap<String, Object>();
        if (report.getRoot().getDiagnoses() instanceof CourseDiagnoses diagnoses)
            for (var coverage : safe(diagnoses.getJudgeCoverages())) if (coverage.getStanding() != null) {
                var standing = coverage.getStanding();
                currentJudges.put(coverage.getJudgeName(), Arrays.asList(standing.getJudgeVersion(),
                        Objects.toString(standing.getRuntimeIdentity(), ""), standing.isConsultsOnCourse()));
            }
        if (config.getJudgeFreshnessCatalog() != null) for (String analyzer : validators)
            config.getJudgeFreshnessCatalog().reference(analyzer).ifPresent(ref -> currentJudges.put(analyzer,
                    Arrays.asList(ref.getCurrentVersion(), ref.isApproved(), ref.getApprovedRuntimes().stream().map(Object::toString).sorted().toList())));
        payload.put("judges", currentJudges);
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(payload))); }
        catch (Exception failure) { throw new IllegalStateException("No se pudo identificar el contexto de corrección.", failure); }
    }

    private void validate(Request r, CorrectionBatch batch, Snapshot snapshot, List<String> selected) {
        if (r.contentVersion() == null || r.contentVersion() != batch.getContentVersion()
                || !Objects.equals(r.fingerprint(), snapshot.fingerprint()))
            throw new IllegalStateException("El contenido, las opiniones o el contexto cambió: volvé a estimar.");
        if (!snapshot.allowed()) throw new IllegalStateException(snapshot.reason());
        if (selected.isEmpty()) throw new IllegalArgumentException("Seleccioná al menos un quiz con problemas.");
        if (r.queryCap() == null || r.queryCap() < 0) throw new IllegalArgumentException("Elegí un tope explícito de consultas, 0 o más.");
        if (r.reviewer() == null || r.reviewer().isBlank()) throw new IllegalArgumentException("Falta el autor de la operación.");
    }

    private List<RefinementTask> freshTasks(Snapshot snapshot, List<String> selected, String operation) {
        var result = new ArrayList<RefinementTask>();
        for (var task : snapshot.tasks()) if (selected.contains(task.getNodeId()) && task.getNodeTarget() == AuditTarget.QUIZ)
            result.add(new RefinementTask("recorrect-" + operation + "-" + result.size(), task.getNodeTarget(), task.getNodeId(),
                    task.getNodeLabel(), task.getDiagnosisKind(), task.getPriority(), RefinementTaskStatus.IN_BATCH, task.getOrigin()));
        for (String quizId : snapshot.repairs()) result.add(new RefinementTask(DidacticContracts.taskId(operation, quizId),
                AuditTarget.QUIZ, quizId, "Restaurar contrato didáctico de v" + snapshot.references().get(quizId).sourceContentVersion(),
                DiagnosisKind.QUIZ_INSTRUCTION, 0, RefinementTaskStatus.IN_BATCH, null));
        if (result.isEmpty() && snapshot.references().isEmpty()) throw new IllegalStateException("No hay tareas vigentes que se puedan corregir.");
        return result;
    }

    private SelectiveCorrection.Run run(Request r, CorrectionBatch batch, Snapshot snapshot, List<String> tasks,
            String audit, String status, int spent, String detail) {
        var process = safe(batch.getSelectiveRuns()).stream().filter(x -> r.operationId().equals(x.id()))
                .map(SelectiveCorrection.Run::process).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> new SelectiveCorrection.RunProcess(config.getRunningProcesses().currentPid(),
                        config.getRunningProcesses().currentStartedAt(), List.of(), null, batch.getStatus()));
        return new SelectiveCorrection.Run(r.operationId(), ACTION, "all-current-judges", r.sourceOperationId(),
                BatchBook.correctionRevision(batch), r.contentVersion(), snapshot.fingerprint(), "all-current-judges", runtime,
                audit, batch.getPlanId(), tasks, selection(r.quizIds()), r.queryCap(), r.queryCap(), spent,
                selection(r.quizIds()).size(), 0, 0, status, r.reviewer(), Instant.now(), null, detail(snapshot, detail),
                r.includeApproved(), batch.getContentVersion(), courseFingerprint.apply(snapshot.current()), runtime, process);
    }

    private SelectiveCorrection.Run finish(SelectiveCorrection.Run run, CorrectionBatch batch, String status, int spent, String detail) {
        return new SelectiveCorrection.Run(run.id(), run.action(), run.analyzer(), run.sourceRunId(), run.correctionRevision(),
                run.contentVersion(), run.fingerprint(), run.judgeVersion(), run.runtime(), run.auditId(), run.planId(), run.taskIds(),
                run.quizIds(), run.queryCap(), run.reserved(), spent, run.reached(), run.evaluated(), run.missing(), status,
                run.reviewer(), run.createdAt(), Instant.now(), join(run.detail(), detail), run.includeApproved(), batch.getContentVersion(),
                courseFingerprint.apply(DefaultCurrentAnalysisService.cut(ledger.composedCourse(batch,
                        BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore())), batch.getLevel())), runtime, run.process());
    }

    private static String detail(Snapshot snapshot, String reason) {
        String recorded = snapshot.noWork().isEmpty() ? null : "Selección conservada sin propuesta: "
                + new TreeMap<>(snapshot.noWork()).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(java.util.stream.Collectors.joining("; "));
        return join(recorded, reason);
    }
    private static String join(String before, String after) {
        return before == null ? after : after == null ? before : before + ". " + after;
    }

    private State state(CorrectionBatch batch, Snapshot snapshot, SelectiveCorrection.Run run) {
        return new State(batch.getId(), BatchBook.correctionRevision(batch), batch.getContentVersion(), snapshot.fingerprint(),
                snapshot.allowed(), snapshot.reason(), snapshot.quizzes(), snapshot.selected(), snapshot.selected(), NOTE + (snapshot.references().isEmpty() ? ""
                        : " Referencia didáctica histórica explícita; la propuesta actual sigue siendo la base editable."),
                snapshot.validators(), runtime, run == null ? null : run.id(), run == null ? null : run.status(),
                run == null ? 0 : run.spent(), List.copyOf(safe(batch.getSelectiveRuns())));
    }
    private State result(CorrectionBatch batch, SelectiveCorrection.Run run) {
        return new State(batch.getId(), BatchBook.correctionRevision(batch), batch.getContentVersion(), run.fingerprint(),
                false, run.detail(), List.of(), run.quizIds().size(), run.quizIds().size(), NOTE, List.of(), runtime,
                run.id(), run.status(), run.spent(), List.copyOf(safe(batch.getSelectiveRuns())));
    }
    private static void put(CorrectionBatch batch, SelectiveCorrection.Run run) {
        var runs = BatchBook.copy(batch.getSelectiveRuns()); runs.removeIf(r -> r.id().equals(run.id())); runs.add(run); batch.setSelectiveRuns(runs);
    }
    private CorrectionBatch batch(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Falta la corrección.");
        return config.getBatchStore().load(id).orElseThrow(() -> new IllegalArgumentException("No existe la corrección."));
    }
    private static boolean reaches(RefinementTask task, String id) {
        return task != null && (task.getNodeTarget() == AuditTarget.QUIZ && id.equals(task.getNodeId())
                || task.getOrigin() != null && safe(task.getOrigin().getReachedQuizIds()).contains(id));
    }
    private static boolean applies(Finding finding, String id) {
        return finding.getNode() != null && finding.getNode().getTarget() == AuditTarget.QUIZ
                && id.equals(finding.getNode().getNodeId()) || finding.getEvidence() != null
                && safe(finding.getEvidence().getRelatedNodes()).stream().anyMatch(n -> n.getTarget() == AuditTarget.QUIZ && id.equals(n.getNodeId()));
    }
    /** Informational opinions are delivered separately from covered errors, so they cannot widen edit permission. */
    static List<Finding> contextFindings(AuditReport report, RefinementTask task) {
        var all = new LinkedHashMap<String, Finding>();
        AuditNode node = DefaultAnalysisComposer.index(report.getRoot()).get(task.getNodeId());
        for (AuditNode ancestor = node; ancestor != null; ancestor = ancestor.getParent())
            for (Finding finding : safe(ancestor.getFindings())) if (finding != null
                    && (ancestor == node || applies(finding, task.getNodeId()) || findings(task).stream().anyMatch(f -> key(f).equals(key(finding)))))
                all.putIfAbsent(key(finding), finding);
        for (Finding finding : findings(task)) if (finding != null) all.putIfAbsent(key(finding), finding);
        return all.values().stream().filter(f -> !com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceStage.isAudioFinding(f)).toList();
    }
    private static List<Finding> findings(RefinementTask task) {
        if (task.getOrigin() == null) return List.of();
        var result = new ArrayList<Finding>();
        if (task.getOrigin().getFinding() != null) result.add(task.getOrigin().getFinding());
        result.addAll(safe(task.getOrigin().getCoveredFindings())); return result.stream().filter(f -> !com.learney.contentaudit.auditdomain.datacoherence.DataCoherenceStage.isAudioFinding(f)).toList();
    }
    private boolean problem(Finding finding) {
        if (finding == null || finding.getResolution() == FindingResolution.RANK_ONLY) return false;
        // Some judges request independent PANEL review without defining a rewrite task.
        // Their explicit context declaration keeps the observation visible to the proposer;
        // it neither grants edit permission nor certifies that observation as resolved.
        return config.getCorrectionCatalog() == null || !config.getCorrectionCatalog().find(finding.getAnalyzer())
                .map(AnalyzerCorrection::contextOnly).orElse(false);
    }
    private static String key(Finding f) {
        if (f.getIdentity() != null && f.getIdentity().getKey() != null) return f.getIdentity().getKey();
        try { return JSON.writeValueAsString(f); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static List<String> selection(List<String> ids) {
        if (ids == null) return List.of();
        if (ids.stream().anyMatch(id -> id == null || id.isBlank()) || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("Elegí quizzes distintos.");
        return List.copyOf(ids);
    }
    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }
}
