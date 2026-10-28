package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.CorrectionEvolution.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Creates a checkpoint, plan and cumulative proposal ledger without publishing or changing the active analysis. */
final class DefaultCorrectionEvolutionService implements CorrectionEvolutionService {
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final BatchTaskSelector selector;
    private final Function<CourseEntity, String> fingerprint;
    DefaultCorrectionEvolutionService(BatchServiceConfig config, BatchChangeLedger ledger,
            BatchTaskSelector selector, Function<CourseEntity, String> fingerprint) {
        this.config = config; this.ledger = ledger; this.selector = selector; this.fingerprint = fingerprint;
    }
    @Override public View execute(Request request) {
        Objects.requireNonNull(request, "Falta el pedido de nueva corrección.");
        String action = Objects.toString(request.action(), "view");
        if (!Set.of("view", "read", "catalog", "estimate", "create", "tasks", "carry-tasks").contains(action))
            throw new IllegalArgumentException("Operación de evolución desconocida.");
        CorrectionBatch parent = batch(request.batchId());
        if ("catalog".equals(action)) return catalog(parent);
        if ("tasks".equals(action)) return new View(parent.getId(), List.of(), null, null, inheritedTasks(parent, false).tasks());
        if ("carry-tasks".equals(action)) return carryTasks(parent, request);
        if (!"create".equals(action)) {
            Estimate estimate = estimate(parent);
            return "estimate".equals(action) ? new View(parent.getId(), List.of(), estimate, null)
                    : view(parent, estimate, null);
        }
        requireOperation(request);
        return config.getBatchStore().withAnalysisLock(parent.getId(), () -> {
            CorrectionBatch latest = batch(parent.getId());
            Optional<CorrectionBatch> duplicate = config.getBatchStore().list().stream()
                    .filter(b -> b.getEvolution() != null && Objects.equals(b.getEvolution().operationId(), request.operationId()))
                    .findFirst();
            if (duplicate.isPresent()) {
                var child = duplicate.get(); var lineage = child.getEvolution();
                if (!latest.getId().equals(lineage.parentBatchId())
                        || !Objects.equals(request.expectedVersion(), lineage.parentContentVersion())
                        || !Objects.equals(request.analysisRevision(), lineage.parentAnalysisRevision())
                        || !Objects.equals(request.fingerprint(), lineage.parentFingerprint()))
                    throw new IllegalArgumentException("La identidad de operación ya pertenece a otro pedido.");
                return new View(latest.getId(), List.of(), estimate(latest),
                        new Result("EXISTING", child.getId(), BatchBook.correctionRevision(child)));
            }
            Estimate estimate = estimate(latest);
            if (!estimate.allowed()) throw new IllegalStateException(estimate.blockedReason());
            if (!Objects.equals(request.expectedVersion(), estimate.contentVersion())
                    || !Objects.equals(request.fingerprint(), estimate.fingerprint())
                    || !Objects.equals(request.analysisRevision(), estimate.analysisRevision()))
                throw new IllegalStateException("El contenido o el análisis cambió: volvé a preparar la siguiente corrección.");
            CourseEntity base = start(latest);
            CourseEntity source = ledger.composedCourse(latest, base);
            var analysis = requireAnalysis().report(latest, "correction", source);
            String auditId = config.getAuditReportStore().save(analysis);
            RefinementPlan plan = config.getRefinerEngine().plan(analysis, auditId);
            // A new private plan never changes the predecessor's tasks or the globally selected plan.
            String childId = "tanda-" + latest.getLevel().name() + "-c" + estimate.nextCorrectionRevision() + "-"
                    + UUID.nameUUIDFromBytes((latest.getId() + "\n" + request.operationId()).getBytes(StandardCharsets.UTF_8));
            plan.setId(childId + "-plan");
            CorrectionBatch child = selector.select(plan, analysis, latest.getLevel(), List.of(), latest.getPendingRerecords());
            child.setId(childId); child.setLevel(latest.getLevel()); child.setStatus(BatchStatus.OPEN);
            child.setBaselineAuditId(latest.getBaselineAuditId()); child.setStartAuditId(auditId); child.setPlanId(plan.getId());
            child.setCorrectionRevision(estimate.nextCorrectionRevision()); child.setContentVersion(1);
            child.setAnalyzers(new ArrayList<>(BatchBook.list(latest.getAnalyzers())));
            var settings = latest.getSettings();
            child.setSettings(settings == null ? new BatchSettings(4, 4, 3)
                    : new BatchSettings(settings.getTopicsAtOnce(), settings.getQueriesAtOnce(), settings.getMaxAttempts()));
            child.setOpenedAt(Instant.now()); child.setQueriesSpent(0);
            child.setAnalysisSeedAuditId(auditId); child.setAnalysisSeedContentVersion(1);
            var reviews = config.getBatchStore().reviews(latest.getId()).stream().map(r -> new BatchReviewDecision(
                    childId, r.selections(), r.status(), r.reviewer(), r.decidedAt(), null)).toList();
            child.setEvolution(new Lineage(latest.getId(), latest.getContentVersion(), estimate.analysisRevision(),
                    request.operationId(), estimate.fingerprint(), base, source, reviews));
            // The ledger remains cumulative, so publication includes every inherited change. New proposal ids are
            // owned by this batch. Reviews carry exact before/after snapshots and expire when these inputs change.
            ledger.inherit(child, BatchBook.list(latest.getChanges()).stream().map(DefaultCorrectionEvolutionService::copy).toList(), base);
            // F-MOTIVO-R001: every inherited change takes its tasks along, under ids of the new plan.
            Map<String, Set<String>> all = new HashMap<>();
            for (var change : BatchBook.list(child.getChanges()))
                if (change != null) all.put(change.getChangeId(), new HashSet<>(BatchBook.list(change.getTaskIds())));
            InheritedTasks.carry(plans(latest), plan, BatchBook.list(child.getChanges()), all);
            Set<String> selected = new HashSet<>(BatchBook.list(child.getTaskIds()));
            for (var task : BatchBook.list(plan.getTasks())) if (selected.contains(task.getId())) task.setStatus(RefinementTaskStatus.IN_BATCH);
            config.getRefinementPlanStore().save(plan);
            config.getBatchStore().commitSuccessor(latest.getId(), latest.getContentVersion(), estimate.fingerprint(),
                    estimate.analysisRevision(), child);
            return new View(child.getId(), List.of(), estimate(child),
                    new Result("CREATED", child.getId(), BatchBook.correctionRevision(child)));
        });
    }
    private View catalog(CorrectionBatch selected) {
        var all = config.getBatchStore().list().stream().filter(b -> b.getLevel() == selected.getLevel()
                && Objects.equals(b.getBaselineAuditId(),selected.getBaselineAuditId()))
                .sorted(Comparator.comparingInt(BatchBook::correctionRevision).thenComparing(CorrectionBatch::getId)).toList();
        Set<String> frozen = new HashSet<>();
        for (var batch : all) if (batch.getEvolution() != null) frozen.add(batch.getEvolution().parentBatchId());
        var versions = new ArrayList<Version>();
        if (!all.isEmpty()) {
            var first = all.get(0); int revision = config.getBatchStore().analysisSelections(first.getId()).size();
            versions.add(new Version("base-" + first.getId(),"Base",0,0,null,null,null,revision,true,null,
                    analysisStub(first,"base","La base","A0",revision)));
        }
        for (var batch : all) {
            int revision = config.getBatchStore().analysisSelections(batch.getId()).size();
            String label = "Corrección " + BatchBook.correctionRevision(batch);
            versions.add(new Version(batch.getId(),label,BatchBook.correctionRevision(batch),batch.getContentVersion(),
                    batch.getStatus(),batch.getEvolution() == null ? null : batch.getEvolution().parentBatchId(),
                    null,revision,frozen.contains(batch.getId()),null,
                    analysisStub(batch,"correction",label,BatchBook.list(batch.getClosings()).isEmpty()
                            ? null : "A" + BatchBook.list(batch.getClosings()).size(),revision)));
        }
        return new View(selected.getId(),List.copyOf(versions),null,null);
    }
    private CurrentAnalysisService.View analysisStub(CorrectionBatch batch, String state, String label, String code, int revision) {
        var summary = new CurrentAnalysisService.State(state,label,null,null,Map.of(),Map.of(),List.of(),Map.of(),code);
        return new CurrentAnalysisService.View(batch.getId(),batch.getContentVersion(),revision,List.of(summary),List.of(),List.of(),List.of());
    }

    private Estimate estimate(CorrectionBatch batch) {
        String reason = null;
        if (!Set.of(BatchStatus.OPEN, BatchStatus.CLOSED).contains(batch.getStatus()))
            reason = "Terminá o conciliá la operación activa antes de crear otra corrección.";
        if (config.getBatchStore().hasSuccessor(batch.getId())) reason = "Esta corrección ya tiene una siguiente versión.";
        if (BatchBook.list(batch.getSelectiveRuns()).stream().anyMatch(r -> Set.of("RUNNING", "NEEDS_REVIEW").contains(r.status()))
                || BatchBook.list(batch.getOpinionRuns()).stream().anyMatch(r -> Set.of("RUNNING", "NEEDS_REVIEW").contains(r.status())))
            reason = "Todavía hay una operación por terminar o conciliar.";
        return new Estimate(reason == null, reason, batch.getContentVersion(),
                unavailableHistory(batch) ? null : fingerprint.apply(ledger.composedCourse(batch, start(batch))),
                config.getBatchStore().analysisSelections(batch.getId()).size(), BatchBook.correctionRevision(batch) + 1);
    }
    private View view(CorrectionBatch selected, Estimate estimate, Result result) {
        var all = config.getBatchStore().list().stream().filter(b -> b.getLevel() == selected.getLevel()
                && Objects.equals(b.getBaselineAuditId(), selected.getBaselineAuditId()))
                .sorted(Comparator.comparingInt(BatchBook::correctionRevision).thenComparing(CorrectionBatch::getId)).toList();
        var versions = new ArrayList<Version>();
        var analyses = new HashMap<String, CurrentAnalysisService.View>();
        if (!all.isEmpty()) {
            var first = all.get(0); CourseEntity base = unavailableHistory(first) ? null : start(first);
            var analysis = unavailableHistory(first) ? null : analyses.computeIfAbsent(first.getId(), id -> analysisView(first));
            String baseFingerprint = base == null ? null : fingerprint.apply(base);
            base = base == null ? null : DefaultCurrentAnalysisService.cut(base, first.getLevel());
            var baseAnalysis = EvolutionProjection.analysis(analysis,"base",base);
            versions.add(new Version("base-" + first.getId(), "Base", 0, 0, null, null,
                    baseFingerprint, baseAnalysis == null ? 0 : baseAnalysis.revision(), true, base, baseAnalysis));
        }
        for (var batch : all) {
            CourseEntity fullCurrent = unavailableHistory(batch) ? null : ledger.composedCourse(batch, start(batch));
            String currentFingerprint = fullCurrent == null ? null : fingerprint.apply(fullCurrent);
            CourseEntity current = fullCurrent == null ? null : DefaultCurrentAnalysisService.cut(fullCurrent,batch.getLevel());
            versions.add(new Version(batch.getId(), "Corrección " + BatchBook.correctionRevision(batch),
                    BatchBook.correctionRevision(batch), batch.getContentVersion(), batch.getStatus(),
                    batch.getEvolution() == null ? null : batch.getEvolution().parentBatchId(), currentFingerprint,
                    config.getBatchStore().analysisSelections(batch.getId()).size(), config.getBatchStore().hasSuccessor(batch.getId()),
                    current, unavailableHistory(batch) ? null : EvolutionProjection.analysis(
                            analyses.computeIfAbsent(batch.getId(), id -> analysisView(batch)),"correction",current)));
        }
        return new View(selected.getId(), List.copyOf(versions), estimate, result);
    }
    private boolean unavailableHistory(CorrectionBatch batch) {
        // The live production file has moved on. A legacy published batch without a complete checkpoint cannot be
        // reconstructed truthfully from a subset of proposals or from today's unchanged quizzes.
        return batch.getStatus() == BatchStatus.PUBLISHED && BatchCourses.start(batch, null, config.getBatchStore()) == null;
    }
    private CurrentAnalysisService.View analysisView(CorrectionBatch batch) {
        return requireAnalysis().comparisonView(batch);
    }
    private CurrentAnalysisService requireAnalysis() {
        return Objects.requireNonNull(config.getCurrentAnalysisService(), "Falta el servicio de análisis vigente.");
    }
    private CourseEntity start(CorrectionBatch batch) {
        return BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
    }
    private CorrectionBatch batch(String id) {
        if (id == null || id.isBlank()) return config.getBatchStore().open().orElseThrow(
                () -> new IllegalArgumentException("Elegí una corrección."));
        return config.getBatchStore().load(id).orElseThrow(() -> new IllegalArgumentException("No existe esa corrección."));
    }
    private static void requireOperation(Request r) {
        if (r.operationId() == null || !r.operationId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw new IllegalArgumentException("Falta una identidad de operación válida.");
        if (r.reviewer() == null || r.reviewer().isBlank()) throw new IllegalArgumentException("Falta el autor de la nueva corrección.");
    }
    /** What {@link #inheritedTasks} found, and the plan to save with the tasks it had before. */
    private record Carry(Tasks tasks, RefinementPlan plan, List<RefinementTask> before) {}
    private static final Carry NOTHING = new Carry(new Tasks(0, 0, 0, 0, 0, false), null, List.of());
    /**
     * F-MOTIVO-R003: the inherited changes of {@code batch} whose tasks still have its predecessor's numbering, and
     * what carrying them would do. With {@code apply}, the changes of {@code batch} and its own plan are the ones
     * that change; without it, copies do.
     */
    private Carry inheritedTasks(CorrectionBatch batch, boolean apply) {
        var lineage = batch.getEvolution();
        var store = config.getRefinementPlanStore();
        if (lineage == null || store == null || batch.getPlanId() == null) return NOTHING;
        CorrectionBatch predecessor = config.getBatchStore().load(lineage.parentBatchId()).orElse(null);
        RefinementPlan own = store.load(batch.getPlanId()).orElse(null);
        if (predecessor == null || own == null) return NOTHING;
        Map<String, List<String>> predecessorIds = new HashMap<>();
        for (var change : BatchBook.list(predecessor.getChanges()))
            if (change != null) predecessorIds.put(change.getChangeId(), BatchBook.list(change.getTaskIds()));
        Map<String, RefinementTask> ownTasks = BatchBook.tasksById(own);
        Map<String, Set<String>> inherited = new HashMap<>();
        List<BatchChange> changes = new ArrayList<>();
        for (var change : BatchBook.list(batch.getChanges())) {
            if (change == null) continue;
            Set<String> ids = new HashSet<>();
            for (String id : BatchBook.list(change.getTaskIds())) {
                // An id of the predecessor's change that the batch's own plan does not resolve to this exercise.
                RefinementTask mine = ownTasks.get(id);
                if (predecessorIds.getOrDefault(change.getChangeId(), List.of()).contains(id)
                        && (mine == null || !InheritedTasks.belongs(mine, change))) ids.add(id);
            }
            if (ids.isEmpty()) continue;
            inherited.put(change.getChangeId(), ids);
            // Looking only: carrying rewrites the ids of a copy that shares the steps.
            changes.add(apply ? change : new BatchChange(change.getChangeId(), change.getOrigin(), change.getRuleId(),
                    change.getKnowledgeId(), new ArrayList<>(BatchBook.list(change.getTaskIds())), change.getSteps(),
                    change.getGateVerdict(), change.isJoseException(), change.getCreatedAt(), change.getDroppedReason()));
        }
        List<RefinementTask> before = new ArrayList<>(BatchBook.list(own.getTasks()));
        RefinementPlan target = apply ? own : new RefinementPlan(own.getId(), own.getSourceAuditId(), own.getCreatedAt(),
                new ArrayList<>(before));
        var carried = InheritedTasks.carry(plans(predecessor), target, changes, inherited);
        // A drop or a gate return of an inherited change wrote its outcome under the predecessor's id, on whatever
        // task of this batch has that number: the outcome goes to the copy. One of this batch's own tasks keeps an
        // outcome that is not the inherited change's.
        Set<String> selected = new HashSet<>(BatchBook.list(batch.getTaskIds()));
        Map<String, List<String>> dropped = new HashMap<>();
        for (var change : changes)
            if (change.getDroppedReason() != null && !change.getDroppedReason().isBlank())
                for (String id : inherited.getOrDefault(change.getChangeId(), Set.of()))
                    dropped.computeIfAbsent(id, k -> new ArrayList<>()).add(change.getDroppedReason());
        int outcomes = 0;
        for (var outcome : BatchBook.list(batch.getOutcomes())) {
            String copy = outcome == null ? null : carried.renamed().get(outcome.getTaskId());
            if (copy == null) continue;
            String detail = outcome.getDetail();
            boolean borrowed = !selected.contains(outcome.getTaskId()) || detail != null && dropped
                    .getOrDefault(outcome.getTaskId(), List.of()).stream().anyMatch(r -> detail.contains(r) || r.contains(detail));
            if (!borrowed) continue;
            if (apply) outcome.setTaskId(copy);
            outcomes++;
        }
        return new Carry(new Tasks(inherited.size(), carried.changes(), carried.tasks(), carried.unresolved(), outcomes, false),
                carried.changes() > 0 ? target : null, before);
    }
    /** F-MOTIVO-R003: repairs a batch created before its inherited changes took their tasks along. */
    private View carryTasks(CorrectionBatch selected, Request request) {
        if (request.expectedVersion() == null) throw new IllegalArgumentException("Falta la versión de la corrección que se repara.");
        return config.getBatchStore().withAnalysisLock(selected.getId(), () -> {
            CorrectionBatch batch = batch(selected.getId());
            if (!Objects.equals(request.expectedVersion(), batch.getContentVersion()))
                throw new IllegalStateException("La corrección cambió: volvé a mirar sus tareas heredadas.");
            if (BatchBook.list(batch.getSelectiveRuns()).stream().anyMatch(r -> "RUNNING".equals(r.status()))
                    || BatchBook.list(batch.getOpinionRuns()).stream().anyMatch(r -> "RUNNING".equals(r.status())))
                throw new IllegalStateException("Todavía hay una operación corriendo sobre esta corrección.");
            Carry carry = inheritedTasks(batch, true);
            Tasks tasks = carry.tasks();
            if (carry.plan() == null) return new View(batch.getId(), List.of(), null, null, tasks);
            var store = config.getRefinementPlanStore();
            store.save(carry.plan());
            try {
                config.getBatchStore().saveIfVersion(batch, batch.getContentVersion(), batch.getStatus());
            } catch (RuntimeException e) {
                // The batch was not saved: the plan goes back to the tasks it had, so trying again copies them once.
                carry.plan().setTasks(new ArrayList<>(carry.before())); store.save(carry.plan());
                throw e;
            }
            return new View(batch.getId(), List.of(), null, null,
                    new Tasks(tasks.inherited(), tasks.changes(), tasks.tasks(), tasks.unresolved(), tasks.outcomes(), true));
        });
    }
    /** The plans of {@code batch} and its predecessors, the nearest first (F-MOTIVO-R001). */
    private List<RefinementPlan> plans(CorrectionBatch batch) {
        var store = config.getRefinementPlanStore();
        var plans = new ArrayList<RefinementPlan>();
        Set<String> seen = new HashSet<>();
        for (CorrectionBatch b = batch; b != null && seen.add(b.getId()); ) {
            if (store != null && b.getPlanId() != null) store.load(b.getPlanId()).ifPresent(plans::add);
            String parentId = b.getEvolution() == null ? null : b.getEvolution().parentBatchId();
            b = parentId == null ? null : config.getBatchStore().load(parentId).orElse(null);
        }
        return plans;
    }
    private static BatchChange copy(BatchChange c) {
        return new BatchChange(c.getChangeId(), c.getOrigin(), c.getRuleId(), c.getKnowledgeId(),
                new ArrayList<>(BatchBook.list(c.getTaskIds())), BatchBook.list(c.getSteps()).stream().map(s ->
                    new BatchChangeStep(s.getQuizId(), new ArrayList<>(BatchBook.list(s.getParts())), CorrectionEvolution.copySnapshot(s.getAfter()))).toList(),
                c.getGateVerdict(), c.isJoseException(), c.getCreatedAt(), c.getDroppedReason());
    }
}
