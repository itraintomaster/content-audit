package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.*;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.judging.JudgeAbstention;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessTally;
import com.learney.contentaudit.auditdomain.judging.JudgeVerdictMark;
import com.learney.contentaudit.auditdomain.judging.JudgeVersionNamer;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.function.Function;

/** One composition for every consumer. Reading and changing selections spend no model queries. */
final class DefaultCurrentAnalysisService implements CurrentAnalysisService {
    private static final String QUALITY = "multiple-choice-quality";
    @Override public Set<String> supportedAnalyzers() { return composer.supportedAnalyzers(); }
    @Override public ReanalysisScope reanalysisScope(String analyzer, AuditReport base, AuditReport correction, String requiredVersion) {
        var corrected = DefaultAnalysisComposer.index(correction.getRoot());
        var shared = new LinkedHashSet<String>(); var onlyBase = new LinkedHashSet<String>();
        var units = new LinkedHashSet<String>(); var sharedUnits = new LinkedHashSet<String>();
        for (var node : DefaultAnalysisComposer.index(base.getRoot()).values()) {
            if (!DefaultAnalysisComposer.eligible(node, analyzer)) continue;
            String id = node.getEntity().getId(); var other = corrected.get(id);
            boolean matches = other != null && DefaultAnalysisComposer.eligible(other, analyzer)
                    && composer.sameInput(analyzer, node, other)
                    && (requiredVersion == null || DefaultAnalysisComposer.evaluated(other, analyzer)
                    && Objects.equals(requiredVersion, DefaultAnalysisComposer.version(other, analyzer, correction)));
            String unit = Set.of("mini-theory", "near-duplicates").contains(analyzer) ? node.ancestor(AuditTarget.KNOWLEDGE)
                    .map(n -> n.getEntity().getId()).orElse(id) : id;
            if (matches) { shared.add(id); sharedUnits.add(unit); }
            else {
                onlyBase.add(id);
                units.add(unit);
            }
        }
        // A topic is one query unit even though it contains several quizzes.
        sharedUnits.removeAll(units);
        return new ReanalysisScope(Set.copyOf(shared), Set.copyOf(onlyBase), units.size(), sharedUnits.size());
    }
    @Override public void seed(CorrectionBatch batch, CourseEntity course) {
        if (batch.getAnalysisSeedAuditId() != null && batch.getAnalysisSeedContentVersion() == batch.getContentVersion()) return;
        AuditReport raw = primary(batch, "correction", cut(ledger.composedCourse(batch, course), batch.getLevel()));
        batch.setAnalysisSeedAuditId(config.getAuditReportStore().save(raw));
        batch.setAnalysisSeedContentVersion(batch.getContentVersion());
    }
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final AnalysisComposer composer;
    private final Function<CourseEntity, String> courseFingerprint;
    private final QuizStudentViewRenderer renderer;
    private final BatchReviewService reviews;

    DefaultCurrentAnalysisService(BatchServiceConfig config, BatchChangeLedger ledger, AnalysisComposer composer,
            Function<CourseEntity, String> courseFingerprint, QuizStudentViewRenderer renderer, BatchReviewService reviews) {
        this.config = config; this.ledger = ledger; this.composer = composer;
        this.courseFingerprint = courseFingerprint; this.renderer = renderer; this.reviews = reviews;
    }

    @Override public View execute(Request request) {
        Objects.requireNonNull(request, "Falta el pedido de análisis vigente.");
        String action = Objects.toString(request.action(), "read");
        if (!List.of("read", "preview", "integrate", "replace", "remove").contains(action))
            throw new IllegalArgumentException("Operación de análisis desconocida.");
        if ("read".equals(action) && request.batchId() == null && config.getBatchStore().open().isEmpty()
                && config.getBatchStore().list().isEmpty())
            return new View(null, 0, 0, List.of(), List.of(), List.of(), composer.supportedAnalyzers().stream().sorted().toList());
        CorrectionBatch batch = batch(request.batchId());
        CorrectionBatch owner = baseOwner(batch);
        // F-MEDJ-R012: the base is the chain's. What is integrated in it is kept once, with the batch that owns it,
        // from whichever correction it is asked.
        CorrectionBatch keeper = "base".equals(request.state()) ? owner : batch;
        List<AnalysisSelection> history = config.getBatchStore().analysisSelections(batch.getId());
        List<AnalysisSelection> kept = keeper.getId().equals(batch.getId()) ? history
                : config.getBatchStore().analysisSelections(keeper.getId());
        boolean writes = !List.of("read", "preview").contains(action);
        if (writes) {
            requireId(request.operationId());
            var duplicate = kept.stream().filter(x -> request.operationId().equals(x.operationId())).findFirst();
            if (duplicate.isPresent()) {
                var previous = duplicate.get();
                if (!Objects.equals(previous.action(), action) || !Objects.equals(previous.runId(), request.runId())
                        || !Objects.equals(previous.state(), request.state())
                        || !Objects.equals(previous.removesOperationId(), request.removesOperationId())
                        || !Objects.equals(previous.reviewer(), request.reviewer()))
                    throw new IllegalArgumentException("La identidad de operación ya pertenece a otro pedido.");
                return view(batch, owner, history, true);
            }
        }
        View view = view(batch, owner, history, true);
        if (!writes) return view;
        // F-ABIER-R004: integrating changes no exercise, so an open batch integrates like a closed one.
        for (CorrectionBatch held : new LinkedHashSet<>(List.of(batch, keeper))) {
            if (held.getStatus() == BatchStatus.PUBLISHED)
                throw new IllegalArgumentException("La tanda está publicada: su análisis ya no cambia.");
            if (held.getStatus() != BatchStatus.CLOSED && held.getStatus() != BatchStatus.OPEN)
                throw new IllegalArgumentException("La tanda está tomada por otra operación: integrá cuando termine.");
        }
        if (request.reviewer() == null || request.reviewer().isBlank()) throw new IllegalArgumentException("Falta quién confirma.");
        if (request.expectedRevision() == null || request.expectedRevision() != view.revision()
                || request.contentVersion() == null || request.contentVersion() != batch.getContentVersion())
            throw new IllegalStateException("La selección o el contenido cambió: volvé a leer.");
        State state = view.states().stream().filter(s -> s.id().equals(request.state())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Elegí la base o la corrección vigente."));
        if (!Objects.equals(state.fingerprint(), request.contentFingerprint()))
            throw new IllegalStateException("El contenido cambió: volvé a estimar la integración.");
        String runId = request.runId(), analyzer;
        List<String> ids;
        if ("remove".equals(action)) {
            AnalysisSelection removed = kept.stream().filter(e -> e.operationId().equals(request.removesOperationId())
                    && e.state().equals(request.state()) && !"remove".equals(e.action())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Elegí una integración de este estado."));
            if (kept.stream().anyMatch(e -> removed.operationId().equals(e.removesOperationId())))
                throw new IllegalArgumentException("Esa integración ya fue retirada.");
            runId = removed.runId(); analyzer = removed.analyzer(); ids = removed.nodeIds();
        } else {
            LooseRun run = view.looseRuns().stream().filter(r -> r.id().equals(request.runId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Elegí una corrida con resultados guardados."));
            Fit fit = run.fits().stream().filter(f -> f.state().equals(request.state())).findFirst().orElseThrow();
            if (fit.compatible() == 0) throw new IllegalArgumentException("La corrida no tiene resultados que encajen.");
            if ("integrate".equals(action) && fit.replaces() > 0)
                throw new IllegalArgumentException("Esta selección reemplaza resultados: revisá la diferencia y confirmá «Reemplazar».");
            if (fit.selected() == fit.compatible()) throw new IllegalArgumentException("Esta corrida ya está integrada en todo su alcance compatible.");
            analyzer = run.analyzer(); ids = fit.nodeIds();
        }
        config.getBatchStore().saveAnalysisSelection(new AnalysisSelection(request.operationId(), keeper.getId(),
                request.state(), action, runId, request.removesOperationId(), analyzer, ids, kept.size(),
                keeper.getContentVersion(), state.fingerprint(), request.reviewer().trim(), Instant.now()));
        return view(batch, owner, config.getBatchStore().analysisSelections(batch.getId()), true);
    }

    @Override public View comparisonView(CorrectionBatch batch) {
        return view(batch,baseOwner(batch),config.getBatchStore().analysisSelections(batch.getId()),false);
    }
    /**
     * {@code history} is the batch's own. A batch that shares its base also reads the base's selections, from the
     * batch that keeps them: they are part of its history and of its revision (F-MEDJ-R012).
     */
    private View view(CorrectionBatch batch, CorrectionBatch baseOwner, List<AnalysisSelection> history, boolean includeLoose) {
        // F-CONTR-R004: the reviewer's marks are read while the states are composed; they do not depend on them. If they
        // cannot be read there is no contract, and the screen keeps its own count (R008).
        CompletableFuture<Map<String, ChangeReview.Row>> reviewed = includeLoose && config.getChangeReviewService() != null
                && config.getAnalyzerCatalog() != null
                ? CompletableFuture.supplyAsync(() -> reviewerRows(batch)).exceptionally(unreadable -> null) : null;
        CourseEntity start = BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
        CourseEntity current = cut(ledger.composedCourse(batch, start), batch.getLevel());
        CourseEntity baseCourse = cut(start, batch.getLevel());
        var raw = new LinkedHashMap<String, AuditReport>();
        boolean ownBase = baseOwner.getId().equals(batch.getId());
        // F-CONTR-R007: what the instant analyzers of errors the batch lacks find, measured for free in both states.
        // A shared base is measured once, below, with the batch that owns it.
        AuditReport base = primary(batch, "base", baseCourse);
        raw.put("base", ownBase ? InstantAnalyzers.theBatchLacks(config, batch, baseCourse, base) : base);
        raw.put("correction", InstantAnalyzers.theBatchLacks(config, batch, current, primary(batch, "correction", current)));
        var baseHistory = ownBase ? List.<AnalysisSelection>of() : config.getBatchStore().analysisSelections(baseOwner.getId())
                .stream().filter(e -> "base".equals(e.state())).toList();
        var shown = new ArrayList<>(history); shown.addAll(baseHistory);
        Map<String, AuditReport> sources = new HashMap<>();
        Set<String> requiredSources = new HashSet<>();
        if (!includeLoose) {
            for (var selection : shown) if (!"remove".equals(selection.action())) requiredSources.add(selection.runId());
            for (AuditReport report : raw.values()) for (AuditNode node : DefaultAnalysisComposer.index(report.getRoot()).values())
                if (node.getMetadata() != null && node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE) instanceof Map<?,?> origins)
                    for (Object origin : origins.values()) if (origin instanceof Map<?,?> map && map.get("runId") instanceof String id)
                        requiredSources.add(id);
        }
        // F-MEDJ-R012: the measurements of the base are the chain's; a loose reading shows them from any correction, and
        // the owner of the base composes what was integrated in it from another one.
        Set<String> integrated = new HashSet<>();
        if (ownBase) for (var selection : history) if (!"remove".equals(selection.action())) integrated.add(selection.runId());
        var runs = runs(batch, baseOwner, batch, integrated, includeLoose);
        for (var recorded : runs.values()) { var run = recorded.run();
            if (run.auditId() != null && (includeLoose || requiredSources.contains(run.id())))
                config.getAuditReportStore().load(run.auditId()).ifPresent(r -> sources.put(run.id(), composer.contentOnly(r)));
        }
        var sourceIndexes = new HashMap<String, Map<String, AuditNode>>();
        sources.forEach((id, report) -> sourceIndexes.put(id, DefaultAnalysisComposer.index(report.getRoot())));
        var reports = new LinkedHashMap<String, AuditReport>();
        raw.forEach((state, report) -> reports.put(state, !ownBase && "base".equals(state)
                ? report(baseOwner, "base", baseCourse, true, batch) : compose(batch, state, report, history, runs, sources)));
        var review = reviews.read(batch.getId());
        var tasks = new ArrayList<>(BatchBook.list(review.tasks()));
        // Several runs name the batch's own plan: it is read once, since its tasks are the same reasons each time.
        var plans = new HashSet<String>();
        for (var run : BatchBook.list(batch.getSelectiveRuns())) if (run.planId() != null && plans.add(run.planId()))
            config.getRefinementPlanStore().load(run.planId()).ifPresent(p -> tasks.addAll(BatchBook.list(p.getTasks())));
        var approvals = approvals(review.items(), tasks);
        var states = new ArrayList<State>();
        var readings = new HashMap<String, EvaluatorContract.Reading>();
        for (var entry : reports.entrySet()) {
            String state = entry.getKey(); AuditReport report = entry.getValue();
            var index = DefaultAnalysisComposer.index(report.getRoot());
            var studentViews = new LinkedHashMap<String, QuizStudentView>();
            var provenance = new LinkedHashMap<String, Map<String, Object>>();
            var verdictMarks = new LinkedHashMap<String, Map<String, Map<String, Object>>>();
            var judgeMarks = new HashMap<String, Map<String, EvaluatorContract.JudgeMark>>();
            var judges = freshness(report, verdictMarks, judgeMarks);
            index.forEach((id, node) -> {
                // F-OPINA-R012: the exercise of the state is the one of its content. What a measurement read, maybe an
                // earlier version, goes only with its judge, in its provenance.
                if (node.getEntity() instanceof AuditableQuiz quiz) studentViews.put(id, renderer.render(quiz));
                if (node.getMetadata() != null && node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE) instanceof Map<?, ?> origins) {
                    var value = new LinkedHashMap<String, Object>();
                    origins.forEach((k,v) -> {
                        var origin = new LinkedHashMap<String, Object>();
                        if (v instanceof Map<?,?> o) o.forEach((key, val) -> origin.put(key.toString(), val));
                        var sourceIndex = sourceIndexes.get(origin.get("runId"));
                        var recorded = runs.get(origin.get("runId"));
                        if (recorded != null) {
                            origin.put("contentVersion", recorded.run().contentVersion());
                            origin.put("activityId", activityId(recorded.batch(), recorded.run()));
                        }
                        var savedNode = sourceIndex == null ? node : sourceIndex.get(id);
                        if (savedNode != null && savedNode.getEntity() instanceof AuditableQuiz quiz)
                            origin.put("studentView", renderer.render(quiz));
                        value.put(k.toString(), origin);
                    });
                    provenance.put(id, value);
                }
            });
            var warnings = new LinkedHashSet<>(approvalWarnings(approvals, raw.get(state), report, QUALITY));
            // A replacement keeps the earlier measurement as history, so the warning survives a reread/restart.
            for (var e : shown) if (state.equals(e.state()) && !"remove".equals(e.action()) && sources.containsKey(e.runId()))
                warnings.addAll(approvalWarnings(approvals, sources.get(e.runId()), report, e.analyzer()));
            states.add(new State(state, "base".equals(state) ? "La base" : "Corrección " + BatchBook.correctionRevision(batch),
                    courseFingerprint.apply("base".equals(state) ? baseCourse : current), digest("current-" + state, report.getRoot()),
                    studentViews, provenance, List.copyOf(warnings), comparable(reports.get("base"), report),
                    "base".equals(state) ? "A0" : BatchBook.list(batch.getClosings()).isEmpty() ? null : "A" + batch.getClosings().size(),
                    inapplicable(batch, index), judges, verdictMarks, MissingOpinions.of(report, composer.supportedAnalyzers())));
            var exercises = new LinkedHashMap<String, AuditNode>();
            index.forEach((id, node) -> { if (node.getTarget() == AuditTarget.QUIZ) exercises.put(id, node); });
            readings.put(state, new EvaluatorContract.Reading(exercises, judges.stream().map(JudgeFreshnessCount::judgeName)
                    .collect(Collectors.toSet()), judgeMarks, EvaluatorContract.measured(exercises.values())));
        }
        var loose = new ArrayList<LooseRun>();
        if (includeLoose) for (var recorded : runs.values()) {
            var run = recorded.run();
            if ("reanalyze-both".equals(run.action())) continue; // Coordination record; its two measurements have their own sources.
            AuditReport source = sources.get(run.id());
            if (source == null || !List.of("DONE", "PARTIAL").contains(run.status()) || !composer.supportedAnalyzers().contains(run.analyzer())) continue;
            var sourceNodes = DefaultAnalysisComposer.index(source.getRoot());
            int evaluated = (int) sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ
                    && DefaultAnalysisComposer.hasStoredOpinion(n, run.analyzer())).count();
            int errors = (int) sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ && error(n, run.analyzer())).count();
            var fits = new ArrayList<Fit>();
            // F-MEDJ-R012: a measurement of the base made from another correction of the chain fits only the base.
            boolean ours = recorded.batch().getId().equals(batch.getId());
            for (var entry : reports.entrySet()) if (ours || "base".equals(entry.getKey()))
                fits.add(fit(entry.getKey(), run, source, entry.getValue(), approvals));
            // The configured judge version is not evidence that a reused result came from that version.
            var versions = sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ)
                    .map(n -> DefaultAnalysisComposer.version(n, run.analyzer(), source)).filter(Objects::nonNull).distinct().sorted().toList();
            loose.add(new LooseRun(run.id(), run.analyzer(), run.auditId(), run.status(), run.contentVersion(), run.runtime(),
                    String.join(", ", versions), run.finishedAt() != null ? run.finishedAt() : run.createdAt(),
                    run.spent(), evaluated, errors, fits, config.getAnalyzerCatalog() == null ? run.analyzer()
                            : config.getAnalyzerCatalog().find(run.analyzer()).map(AnalyzerDescriptor::getDisplayName).orElse(run.analyzer()), activityId(recorded.batch(),run)));
        }
        var contract = reviewed == null ? Contract.NONE : contract(reviewed.join(), readings);
        return new View(batch.getId(), batch.getContentVersion(), shown.size(), states, loose, includeLoose ? List.copyOf(shown) : List.of(),
                composer.supportedAnalyzers().stream().sorted().toList(), contract.evaluators(), contract.evaluations());
    }

    private record Contract(List<Evaluator> evaluators, Map<String, Evaluation> evaluations) {
        static final Contract NONE = new Contract(List.of(), Map.of());
    }
    /**
     * F-CONTR: every evaluator of exercises of the catalog and the reviewer, in the correction and, for what the batch
     * changed, in production. Without the reviewer's marks there is no contract: a contract without one of its
     * evaluators would decide the list wrong, so the screen keeps its own count (R008).
     */
    private Contract contract(Map<String, ChangeReview.Row> reviewed, Map<String, EvaluatorContract.Reading> readings) {
        if (reviewed == null) return Contract.NONE;
        var correction = readings.get("correction");
        var evaluators = new ArrayList<Evaluator>();
        for (var card : BatchBook.list(config.getAnalyzerCatalog().list()))
            if (BatchBook.list(card.getEvaluatedTargets()).contains(AuditTarget.QUIZ))
                // F-CONTR-R003: a paid evaluator the correction does not mark against today's version counts any
                // version (EvaluatorContract.assess), and the screen says so.
                evaluators.add(new Evaluator(card.getName(), card.getDisplayName(), card.getCost() != AnalysisCost.INSTANT
                        && !correction.judges().contains(card.getName())
                        ? "Cuenta cualquier versión hasta que su versión de hoy rinda examen." : null));
        var evaluations = EvaluatorContract.evaluate(evaluators.stream().map(Evaluator::name).toList(),
                readings.get("base"), correction, reviewed);
        evaluators.add(new Evaluator(EvaluatorContract.REVIEWER, "El revisor", null));
        return new Contract(List.copyOf(evaluators), evaluations);
    }

    /** F-CONTR-R004: the reviewer's mark of each exercise the batch changed. Without a budget it is not consulted. */
    private Map<String, ChangeReview.Row> reviewerRows(CorrectionBatch batch) {
        var rows = new LinkedHashMap<String, ChangeReview.Row>();
        for (var row : config.getChangeReviewService().review(new ChangeReview.Request(batch.getId(), List.of(), null, null)).rows())
            rows.put(row.quizId(), row);
        return rows;
    }

    @Override public AuditReport report(CorrectionBatch batch, String state, CourseEntity course) {
        return report(batch, state, course, false, null);
    }
    /**
     * {@code instant}: F-CONTR-R007, also what the instant analyzers of errors the batch lacks find. {@code known}: a
     * batch of the chain already read, so finding the runs of a shared base does not read it again.
     */
    private AuditReport report(CorrectionBatch batch, String state, CourseEntity course, boolean instant, CorrectionBatch known) {
        CorrectionBatch owner = "base".equals(state) ? baseOwner(batch) : batch;
        if (!owner.getId().equals(batch.getId())) return report(owner, state, course, instant, batch);
        var sources = new HashMap<String, AuditReport>();
        var history = config.getBatchStore().analysisSelections(batch.getId());
        Set<String> removed = new HashSet<>();
        for (var selection : history) if ("remove".equals(selection.action())) removed.add(selection.removesOperationId());
        Set<String> required = new HashSet<>();
        for (var selection : history) if (state.equals(selection.state()) && !"remove".equals(selection.action())
                && !removed.contains(selection.operationId())) required.add(selection.runId());
        var runs = runs(batch, batch, known, required, false);
        for (var recorded : runs.values()) { var run = recorded.run();
            if (run.auditId() != null && required.contains(run.id()))
                config.getAuditReportStore().load(run.auditId()).ifPresent(r -> sources.put(run.id(), composer.contentOnly(r)));
        }
        AuditReport primary = primary(batch, state, course);
        return compose(batch, state, instant ? InstantAnalyzers.theBatchLacks(config, batch, course, primary) : primary, history, runs, sources);
    }

    /** A recorded run and the batch that recorded it. */
    private record Recorded(CorrectionBatch batch, SelectiveCorrection.Run run) {}

    /**
     * F-MEDJ-R012: the runs a reading of {@code batch} can show or compose: its own and, from the other batches of the
     * chain that shares its base, the ones that measured the base. The other batches are read only for a loose
     * reading or when {@code required} names a run that is not the batch's own; {@code known} is not read again.
     */
    private Map<String, Recorded> runs(CorrectionBatch batch, CorrectionBatch owner, CorrectionBatch known,
            Set<String> required, boolean wholeChain) {
        var result = new LinkedHashMap<String, Recorded>();
        for (var run : BatchBook.list(batch.getSelectiveRuns())) result.putIfAbsent(run.id(), new Recorded(batch, run));
        if (!wholeChain && result.keySet().containsAll(required)) return result;
        for (var other : chain(owner, known)) if (!other.getId().equals(batch.getId()))
            for (var run : BatchBook.list(other.getSelectiveRuns()))
                if (run.action() != null && run.action().endsWith("-base")) result.putIfAbsent(run.id(), new Recorded(other, run));
        return result;
    }

    /** The batch that owns the base and each next version after it, in order. */
    private List<CorrectionBatch> chain(CorrectionBatch owner, CorrectionBatch known) {
        var chain = new ArrayList<CorrectionBatch>(List.of(owner));
        Set<String> seen = new HashSet<>(Set.of(owner.getId()));
        for (CorrectionBatch at = owner; ; ) {
            String id = at.getId();
            CorrectionBatch next = known != null && known.getEvolution() != null && id.equals(known.getEvolution().parentBatchId())
                    ? known : config.getBatchStore().successor(id).orElse(null);
            if (next == null) return chain;
            if (!seen.add(next.getId())) throw new IllegalStateException("El linaje de las correcciones contiene un ciclo.");
            chain.add(next); at = next;
        }
    }

    private CorrectionBatch baseOwner(CorrectionBatch batch) {
        CorrectionBatch owner = batch;
        Set<String> seen = new HashSet<>();
        while (owner.getEvolution() != null) {
            if (!seen.add(owner.getId())) throw new IllegalStateException("El linaje de las correcciones contiene un ciclo.");
            owner = config.getBatchStore().load(owner.getEvolution().parentBatchId()).orElseThrow(
                    () -> new IllegalStateException("Falta la corrección de la que parte esta versión."));
        }
        return owner;
    }

    @Override public ComparisonReports comparisonReports(CorrectionBatch batch, CourseEntity start, CourseEntity composed) {
        // F-MIDIO-R007: the base, the start and today are read in the level of the correction, as its analysis is.
        if (batch.getLevel() != null) { start = cut(start, batch.getLevel()); composed = cut(composed, batch.getLevel()); }
        AuditReport baseline = report(batch, "base", start);
        // F-MIDIO-R004: today also with what the instant analyzers of errors the batch lacks find, measured for free.
        AuditReport projected = report(batch, "correction", composed, true, null);
        AuditReport startReport = Objects.equals(batch.getStartAuditId(), batch.getBaselineAuditId()) ? null
                : config.getAuditReportStore().load(batch.getStartAuditId())
                        .orElseThrow(() -> new IllegalStateException("Falta el análisis de inicio de la tanda."));
        AuditReport approved = startReport == null ? baseline : composer.compose(scopedSeed(startReport, start), List.of());
        // F-MIDIO-R006: the instant analyzers today ran are measured again, for free and with today's rules, on what the
        // base and the start have: a level the batch did not touch gives the same on both sides.
        List<String> instant = instantAnalyzersOf(projected);
        measureAsToday(baseline, instant);
        if (approved != baseline) measureAsToday(approved, instant);
        if (batch.getLevel() != null) measureCourseWhole(batch.getLevel(), instant, startReport, baseline, approved, projected);
        var a = DefaultAnalysisComposer.index(baseline.getRoot());
        var startIndex = DefaultAnalysisComposer.index(approved.getRoot());
        var b = DefaultAnalysisComposer.index(projected.getRoot());
        // F-MIDIO-R005: a judge the base never measured is read from today alone, as it was measured, and is left out of
        // everything that is compared.
        Set<String> onlyToday = new TreeSet<>();
        for (String judge : judgesOfExercises(b)) if (measures(b, judge) && !measures(a, judge)) onlyToday.add(judge);
        AuditReport alone = onlyToday.isEmpty() ? null : composer.compose(projected, List.of());
        for (String judge : onlyToday) for (var index : List.of(a, startIndex, b)) for (var n : index.values()) forget(n, judge);
        Set<String> startNotComparable = new TreeSet<>();
        // Missing ends or mixed criteria never prove an improvement or regression.
        for (String analyzer : composer.supportedAnalyzers()) {
            if (onlyToday.contains(analyzer)) continue;
            Set<String> same = versionsOfToday(analyzer, projected);
            for (var entry : b.entrySet()) if (entry.getValue().getTarget() == AuditTarget.QUIZ
                    && DefaultAnalysisComposer.eligible(entry.getValue(), analyzer)) {
                var before = a.get(entry.getKey()); var after = entry.getValue();
                // F-MIDIO-R002: an exercise the judge does not read in the base -- one the correction turned into
                // multiple choice -- has none of its errors there; today's opinion is counted against that zero.
                if (before != null && !DefaultAnalysisComposer.eligible(before, analyzer)) continue;
                String oldVersion = before == null ? null : DefaultAnalysisComposer.version(before, analyzer, baseline);
                String newVersion = DefaultAnalysisComposer.version(after, analyzer, projected);
                if (before == null || !DefaultAnalysisComposer.evaluated(before, analyzer)
                        || !DefaultAnalysisComposer.evaluated(after, analyzer) || !sameVersion(oldVersion, newVersion, same)
                        || !Objects.equals(runtime(before, analyzer, baseline), runtime(after, analyzer, projected))) {
                    invalidate(before, analyzer); invalidate(after, analyzer);
                }
            }
            // F-MIDIO-R002: a start that did not measure, with today's version and runtime, every exercise today measured
            // is not compared for that judge, and today keeps what it measured. When the start is the base, the base
            // already left only what both measured alike.
            if (approved != baseline && !startMeasuredAsToday(analyzer, startIndex, b, approved, projected, same)) {
                startNotComparable.add(analyzer);
                // Where the start read what today reads, today's opinion is the start's: a change gains nothing there.
                for (var entry : startIndex.entrySet()) if (entry.getValue().getTarget() == AuditTarget.QUIZ) {
                    var n = entry.getValue(); var today = b.get(entry.getKey());
                    if (today != null && DefaultAnalysisComposer.eligible(n, analyzer) && DefaultAnalysisComposer.evaluated(today, analyzer)
                            && composer.sameInput(analyzer, n, today)) adopt(n, today, analyzer);
                    else invalidate(n, analyzer);
                }
            } else for (var entry : b.entrySet()) if (DefaultAnalysisComposer.eligible(entry.getValue(), analyzer)) {
                var before = startIndex.get(entry.getKey()); var after = entry.getValue();
                if (before != null && !DefaultAnalysisComposer.eligible(before, analyzer)) continue;
                if (before == null || !DefaultAnalysisComposer.evaluated(before, analyzer)
                        || !DefaultAnalysisComposer.evaluated(after, analyzer)
                        || !sameVersion(DefaultAnalysisComposer.version(before, analyzer, approved),
                                DefaultAnalysisComposer.version(after, analyzer, projected), same)
                        || !Objects.equals(runtime(before, analyzer, approved), runtime(after, analyzer, projected))) {
                    invalidate(before, analyzer); invalidate(after, analyzer);
                }
            }
            for (var n : startIndex.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
            for (var n : a.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
            for (var n : b.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
        }
        // F-MIDIO-R006: a judge is compared exercise by exercise, on what both sides measured alike; the measure its
        // report saved at the root does not leave it undecided.
        alignJudgeMeasures(projected, baseline, approved);
        baseline = composer.compose(baseline, List.of()); projected = composer.compose(projected, List.of());
        return new ComparisonReports(baseline, composer.compose(approved, List.of()), projected, alone, onlyToday,
                startNotComparable);
    }
    /** F-MIDIO-R006: the instant analyzers of the catalog that ran in today's reading. */
    private List<String> instantAnalyzersOf(AuditReport today) {
        if (config.getAnalyzerCatalog() == null) return List.of();
        Set<String> ran = new HashSet<>();
        for (var measure : BatchBook.list(today.getRoot().getMeasures())) ran.add(measure.getAnalyzer());
        for (var n : DefaultAnalysisComposer.index(today.getRoot()).values()) ran.addAll(n.getScores().keySet());
        var names = new ArrayList<String>();
        for (var card : BatchBook.list(config.getAnalyzerCatalog().list()))
            if (card.getCost() == AnalysisCost.INSTANT && ran.contains(card.getName())) names.add(card.getName());
        return names;
    }
    /**
     * F-MIDIO-R006: {@code report} with what the instant analyzers find today on its own content -- the course it
     * measured, rebuilt from its entities --, without a query: their scores, findings, marks and measures replace the
     * saved ones.
     */
    private void measureAsToday(AuditReport report, List<String> names) {
        if (names.isEmpty() || config.getAuditEngine() == null) return;
        AuditableCourse course = MeasuredCourse.of(report, Map.of());
        if (course == null) return;
        var policies = new LinkedHashMap<String, EvaluationRunPolicy>();
        for (String name : names) policies.put(name, new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));
        AuditReport fresh = config.getAuditEngine().runAudit(course, new AnalyzerRunSelection(names, policies));
        if (fresh == null || fresh.getRoot() == null) return;
        fresh = composer.contentOnly(fresh);
        var index = DefaultAnalysisComposer.index(report.getRoot());
        for (var entry : DefaultAnalysisComposer.index(fresh.getRoot()).entrySet()) {
            AuditNode to = index.get(entry.getKey()), from = entry.getValue();
            if (to == null) continue;
            for (String name : names) {
                Double score = from.getScores().get(name);
                if (score != null) to.getScores().put(name, score); else to.getScores().remove(name);
                to.getFindings().removeIf(f -> name.equals(f.getAnalyzer()));
                for (var finding : from.getFindings()) if (name.equals(finding.getAnalyzer())) to.getFindings().add(finding);
                to.getUnevaluatedBy().remove(name);
                if (BatchBook.list(from.getUnevaluatedBy()).contains(name)) to.getUnevaluatedBy().add(name);
                to.getPartiallyEvaluatedBy().remove(name);
                if (BatchBook.list(from.getPartiallyEvaluatedBy()).contains(name)) to.getPartiallyEvaluatedBy().add(name);
            }
        }
        var measures = new ArrayList<>(BatchBook.list(report.getRoot().getMeasures()));
        measures.removeIf(m -> names.contains(m.getAnalyzer()));
        for (var measure : BatchBook.list(fresh.getRoot().getMeasures())) if (names.contains(measure.getAnalyzer())) measures.add(measure);
        report.getRoot().setMeasures(measures);
    }
    /**
     * F-MIDIO-R008: the course of a level's correction is measured whole. Each instant analyzer of vocabulary measures
     * again, for free, the course with the level as each side has it and the other levels as the start has them -- the
     * correction does not touch them --, and what it finds for the course replaces what it found on the level alone.
     * Without a start that has the other levels, the course stays the level.
     */
    private void measureCourseWhole(CefrLevel level, List<String> instant, AuditReport start, AuditReport... sides) {
        if (start == null || config.getAuditEngine() == null || config.getAnalyzerCatalog() == null) return;
        Set<String> vocabulary = new HashSet<>();
        for (var card : BatchBook.list(config.getAnalyzerCatalog().list()))
            if (card.getFamily() == AnalyzerFamily.VOCABULARY) vocabulary.add(card.getName());
        List<String> names = instant.stream().filter(vocabulary::contains).toList();
        AuditableCourse others = MeasuredCourse.of(start, Map.of());
        if (names.isEmpty() || others == null
                || others.getMilestones().stream().allMatch(m -> level.name().equalsIgnoreCase(m.getLabel()))) return;
        var policies = new LinkedHashMap<String, EvaluationRunPolicy>();
        for (String name : names) policies.put(name, new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));
        Set<AuditReport> measured = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AuditReport side : sides) {
            if (!measured.add(side)) continue;
            AuditableCourse own = MeasuredCourse.of(side, Map.of());
            if (own == null) continue;
            var levels = new ArrayList<AuditableMilestone>();
            for (var milestone : others.getMilestones())
                if (!level.name().equalsIgnoreCase(milestone.getLabel())) levels.add(milestone);
                else for (var mine : own.getMilestones()) if (level.name().equalsIgnoreCase(mine.getLabel())) levels.add(mine);
            AuditReport fresh = config.getAuditEngine().runAudit(new AuditableCourse(levels), new AnalyzerRunSelection(names, policies));
            if (fresh == null || fresh.getRoot() == null) continue;
            AuditNode to = side.getRoot(), from = fresh.getRoot();
            for (String name : names) {
                to.getScores().keySet().removeIf(key -> key.equals(name) || key.startsWith(name + "/"));
                from.getScores().forEach((key, score) -> {
                    if (score != null && (key.equals(name) || key.startsWith(name + "/"))) to.getScores().put(key, score);
                });
                to.getFindings().removeIf(f -> name.equals(f.getAnalyzer()));
                for (var finding : from.getFindings()) if (name.equals(finding.getAnalyzer())) to.getFindings().add(finding);
            }
        }
    }
    /** F-MIDIO-R006: each judge's measure at the root of {@code others} is today's, once only opinions measured alike remain. */
    private void alignJudgeMeasures(AuditReport today, AuditReport... others) {
        var current = new HashMap<String, com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure>();
        for (var measure : BatchBook.list(today.getRoot().getMeasures())) current.put(measure.getAnalyzer(), measure);
        for (AuditReport report : others) {
            var measures = new ArrayList<com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure>();
            for (var measure : BatchBook.list(report.getRoot().getMeasures()))
                measures.add(composer.supportedAnalyzers().contains(measure.getAnalyzer()) && current.containsKey(measure.getAnalyzer())
                        ? current.get(measure.getAnalyzer()) : measure);
            report.getRoot().setMeasures(measures);
        }
    }
    /** The judges a reading has for its exercises: the ones with an opinion of their own and every one the composer reads. */
    private Set<String> judgesOfExercises(Map<String, AuditNode> index) {
        Set<String> names = new TreeSet<>(composer.supportedAnalyzers());
        for (var n : index.values()) if (n.getTarget() == AuditTarget.QUIZ)
            for (String name : n.getScores().keySet()) if (!name.contains("/")) names.add(name);
        return names;
    }
    /** Whether a judge left an opinion -- a score or a saved abstention -- on any exercise of a reading. */
    private static boolean measures(Map<String, AuditNode> index, String judge) {
        for (var n : index.values()) if (n.getTarget() == AuditTarget.QUIZ && DefaultAnalysisComposer.hasStoredOpinion(n, judge)) return true;
        return false;
    }
    /** A judge out of a reading: no score, no finding and no unmeasured mark of it remains, so nothing counts it. */
    private static void forget(AuditNode node, String judge) {
        node.getScores().remove(judge);
        node.getFindings().removeIf(f -> judge.equals(f.getAnalyzer()));
        node.getUnevaluatedBy().remove(judge);
        node.getPartiallyEvaluatedBy().remove(judge);
    }
    /** F-MIDIO-R002: whether the start has, for every exercise today measured, the judge's opinion with today's version and runtime. */
    private static boolean startMeasuredAsToday(String analyzer, Map<String, AuditNode> start, Map<String, AuditNode> today,
            AuditReport approved, AuditReport projected, Set<String> same) {
        for (var entry : today.entrySet()) {
            var after = entry.getValue();
            if (after.getTarget() != AuditTarget.QUIZ || !DefaultAnalysisComposer.eligible(after, analyzer)
                    || !DefaultAnalysisComposer.evaluated(after, analyzer)) continue;
            var before = start.get(entry.getKey());
            if (before != null && !DefaultAnalysisComposer.eligible(before, analyzer)) continue;
            String version = before == null ? null : DefaultAnalysisComposer.version(before, analyzer, approved);
            if (before == null || !DefaultAnalysisComposer.evaluated(before, analyzer)
                    || !sameVersion(version, DefaultAnalysisComposer.version(after, analyzer, projected), same)
                    || !Objects.equals(runtime(before, analyzer, approved), runtime(after, analyzer, projected))) return false;
        }
        return true;
    }
    /**
     * F-ALDIA-R002 in the comparison: the versions that are today's version of {@code analyzer}, as today's photo names
     * them -- its version and the ones it declares the same, as the version a judge left counted for its tests.
     */
    static Set<String> versionsOfToday(String analyzer, AuditReport today) {
        Set<String> same = new HashSet<>();
        if (today != null && today.getRoot() != null && today.getRoot().getDiagnoses() instanceof CourseDiagnoses d)
            for (var coverage : BatchBook.list(d.getJudgeCoverages())) if (coverage != null && analyzer.equals(coverage.getJudgeName())
                    && coverage.getStanding() != null) {
                if (coverage.getStanding().getJudgeVersion() != null) same.add(coverage.getStanding().getJudgeVersion());
                if (coverage.getStanding().getCompatibleJudgeVersions() != null)
                    same.addAll(coverage.getStanding().getCompatibleJudgeVersions());
            }
        return same;
    }
    /** Two opinions of a judge are of one version when both name it: the same, or two that today's judge names as its. */
    static boolean sameVersion(String one, String other, Set<String> same) {
        return one != null && other != null && (one.equals(other) || same.contains(one) && same.contains(other));
    }
    private Map<String, List<String>> comparable(AuditReport before, AuditReport after) {
        var a = DefaultAnalysisComposer.index(before.getRoot());
        var result = new LinkedHashMap<String, List<String>>();
        var sameByJudge = new HashMap<String, Set<String>>();
        for (var n : DefaultAnalysisComposer.index(after.getRoot()).values()) if (n.getTarget() == AuditTarget.QUIZ) {
            var old = a.get(n.getEntity().getId());
            var names = new ArrayList<String>();
            if (old != null) for (String name : n.getScores().keySet()) {
                if (!DefaultAnalysisComposer.evaluated(n, name) || !DefaultAnalysisComposer.evaluated(old, name)) continue;
                if (!composer.supportedAnalyzers().contains(name) || (
                        sameVersion(DefaultAnalysisComposer.version(old, name, before), DefaultAnalysisComposer.version(n, name, after),
                                sameByJudge.computeIfAbsent(name, judge -> versionsOfToday(judge, after)))
                        && runtime(old, name, before) != null
                        && Objects.equals(runtime(old, name, before), runtime(n, name, after)))) names.add(name);
            }
            result.put(n.getEntity().getId(), names);
        }
        return result;
    }

    private Map<String, List<String>> inapplicable(CorrectionBatch batch, Map<String, AuditNode> index) {
        var names = new TreeSet<>(BatchBook.list(batch.getAnalyzers()));
        names.addAll(composer.supportedAnalyzers());
        var result = new LinkedHashMap<String, List<String>>();
        for (var entry : index.entrySet()) if (entry.getValue().getTarget() == AuditTarget.QUIZ) {
            var excluded = names.stream().filter(name -> !DefaultAnalysisComposer.eligible(entry.getValue(), name)).toList();
            if (!excluded.isEmpty()) result.put(entry.getKey(), excluded);
        }
        return result;
    }
    /**
     * F-ALDIA-R003 and R005: the mark of every verdict of the state against today's judge, and per judge how many of its
     * exercises -- or topics, for the judge of topics -- are up to date, from before and not measured. A saved abstention
     * is a verdict: it carries its mark and is counted apart by reason, never as not measured (R003 inv. 6). Reads only.
     */
    private List<JudgeFreshnessCount> freshness(AuditReport report,
            Map<String, Map<String, Map<String, Object>>> marks, Map<String, Map<String, EvaluatorContract.JudgeMark>> judgeMarks) {
        if (config.getJudgeFreshnessCatalog() == null || !(report.getRoot().getDiagnoses() instanceof CourseDiagnoses d)) return List.of();
        var index = DefaultAnalysisComposer.index(report.getRoot());
        var result = new ArrayList<JudgeFreshnessCount>();
        for (var coverage : BatchBook.list(d.getJudgeCoverages())) {
            String judge = coverage.getJudgeName();
            var reference = todaysJudge(judge);
            if (reference.isEmpty()) continue;
            AuditTarget unit = coverage.getUnit() == null ? AuditTarget.QUIZ : coverage.getUnit();
            var namer = reference.get().getNamer();
            var units = new LinkedHashMap<String, UnitVerdict>();
            for (var node : index.values()) {
                if (node.getTarget() != AuditTarget.QUIZ || !DefaultAnalysisComposer.eligible(node, judge) || !reads(node, judge)) continue;
                String id = node.getEntity().getId();
                String unitId = unit == AuditTarget.KNOWLEDGE
                        ? node.ancestor(AuditTarget.KNOWLEDGE).map(n -> n.getEntity().getId()).orElse(id) : id;
                UnitVerdict verdict = UnitVerdict.NONE;
                if (DefaultAnalysisComposer.hasStoredOpinion(node, judge)) {
                    String version = DefaultAnalysisComposer.version(node, judge, report);
                    var runtime = JudgeFreshnessReference.runtimeOf(runtime(node, judge, report));
                    var mark = reference.get().mark(version, runtime);
                    var abstention = DefaultAnalysisComposer.abstention(node, judge).orElse(null);
                    verdict = new UnitVerdict(mark, version, runtime, abstention);
                    var shown = new LinkedHashMap<String, Object>();
                    shown.put("freshness", mark.freshness()); shown.put("staleCause", mark.cause());
                    shown.put("judgeVersion", version); shown.put("versionName", version == null ? null : namer.looseName(version));
                    shown.put("runtimeName", JudgeVersionNamer.runtimeName(runtime));
                    shown.put("abstention", abstention); shown.put("abstentionName", abstention == null ? null : abstention.label());
                    marks.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(judge, shown);
                    // F-CONTR-R001: the same mark, typed, for the contract.
                    judgeMarks.computeIfAbsent(id, k -> new HashMap<>()).put(judge, new EvaluatorContract.JudgeMark(mark.freshness(), abstention));
                }
                units.merge(unitId, verdict, UnitVerdict::worse);
            }
            var tally = new JudgeFreshnessTally(reference.get(), displayName(judge), unit);
            for (var verdict : units.values()) {
                if (verdict.mark() == null) tally.notMeasured(1);
                else tally.add(verdict.mark(), verdict.version(), verdict.runtime(), 1, verdict.abstention());
            }
            result.add(tally.count());
        }
        return List.copyOf(result);
    }
    /** What a judge left on a unit; a topic is from before as soon as one of its exercises is. */
    private record UnitVerdict(JudgeVerdictMark mark, String version,
            EvaluationRuntimeIdentity runtime, JudgeAbstention abstention) {
        static final UnitVerdict NONE = new UnitVerdict(null, null, null, null);
        static UnitVerdict worse(UnitVerdict a, UnitVerdict b) {
            if (a.mark() == null) return b;
            if (b.mark() == null || !a.mark().isUpToDate()) return a;
            return b;
        }
    }
    private static boolean reads(AuditNode node, String judge) {
        return node.getScores() != null && node.getScores().containsKey(judge)
                || DefaultAnalysisComposer.hasStoredOpinion(node, judge)
                || BatchBook.list(node.getUnevaluatedBy()).contains(judge)
                || BatchBook.list(node.getPartiallyEvaluatedBy()).contains(judge);
    }
    private String displayName(String judge) {
        return config.getAnalyzerCatalog() == null ? judge
                : config.getAnalyzerCatalog().find(judge).map(AnalyzerDescriptor::getDisplayName).orElse(judge);
    }
    private static Object runtime(AuditNode node, String analyzer, AuditReport report) {
        if (node == null) return null;
        Object origins = node.getMetadata() == null ? null : node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
        Object origin = origins instanceof Map<?,?> m ? m.get(analyzer) : null;
        if (origin instanceof Map<?,?> m) return m.get("runtime");
        if (report.getRoot().getDiagnoses() instanceof CourseDiagnoses d)
            return BatchBook.list(d.getJudgeCoverages()).stream().filter(c -> analyzer.equals(c.getJudgeName()) && c.getStanding() != null)
                    .map(c -> Objects.toString(c.getStanding().getRuntimeIdentity(), "")).findFirst().orElse(null);
        return null;
    }
    /** F-MIDIO-R002: {@code node} with the opinion of {@code analyzer} that {@code from} has, read on the same input. */
    private static void adopt(AuditNode node, AuditNode from, String analyzer) {
        Double score = from.getScores().get(analyzer);
        if (score != null) node.getScores().put(analyzer, score); else node.getScores().remove(analyzer);
        node.getFindings().removeIf(f -> analyzer.equals(f.getAnalyzer()));
        for (var finding : from.getFindings()) if (analyzer.equals(finding.getAnalyzer())) node.getFindings().add(finding);
        node.getUnevaluatedBy().remove(analyzer);
        node.getPartiallyEvaluatedBy().remove(analyzer);
        if (BatchBook.list(from.getPartiallyEvaluatedBy()).contains(analyzer)) node.getPartiallyEvaluatedBy().add(analyzer);
    }
    private static void invalidate(AuditNode node, String analyzer) {
        if (node == null || !DefaultAnalysisComposer.eligible(node, analyzer)) return;
        node.getScores().remove(analyzer); node.getFindings().removeIf(f -> analyzer.equals(f.getAnalyzer()));
        if (!node.getUnevaluatedBy().contains(analyzer)) node.getUnevaluatedBy().add(analyzer);
    }

    private AuditReport primary(CorrectionBatch batch, String state, CourseEntity course) {
        if (!"base".equals(state) && batch.getAnalysisSeedAuditId() != null && batch.getAnalysisSeedContentVersion() == batch.getContentVersion()) {
            AuditReport seed = config.getAuditReportStore().load(batch.getAnalysisSeedAuditId()).orElseThrow(
                    () -> new IllegalStateException("Falta la medición congelada antes de reanalizar."));
            // Older selective operations froze only one level. A closing/publication requests the complete course:
            // remeasure its other nodes for free and merge only compatible saved opinions instead of returning A1
            // as if it were the global analysis.
            if (covers(seed, course)) return scopedSeed(seed, course);
        }
        if ("base".equals(state)) return scopedSeed(config.getAuditReportStore().load(batch.getBaselineAuditId())
                .orElseThrow(() -> new IllegalStateException("Falta el informe de la base.")),course);
        // Existing analyzers retain their ordinary zero-budget behavior. A loose judge is never selected by recency.
        var names = BatchBook.list(batch.getAnalyzers()).stream().filter(n -> !QUALITY.equals(n)).toList();
        var policies = new LinkedHashMap<String, EvaluationRunPolicy>();
        for (String name : names) policies.put(name, new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));
        AuditReport current = config.getAuditEngine().runAudit(config.getCourseMapper().map(course),
                new AnalyzerRunSelection(names, policies));
        if (batch.getAnalysisSeedAuditId() == null) return composer.contentOnly(current);
        // A new batch version changes the snapshot, not every evaluator's input. Keep saved
        // opinions with identical inputs; explicit selections are still applied afterwards.
        AuditReport previous = config.getAuditReportStore().load(batch.getAnalysisSeedAuditId())
                .orElseThrow(() -> new IllegalStateException("Falta el análisis guardado de la tanda."));
        return composer.contentOnly(composer.preserveCompatibleOpinions(current, previous, batch.getAnalysisSeedAuditId()));
    }

    private static boolean covers(AuditReport report, CourseEntity course) {
        if (course == null || course.getRoot() == null) return true;
        var indexed = DefaultAnalysisComposer.index(report.getRoot());
        for (var milestone : BatchBook.list(course.getRoot().getMilestones()))
            for (var topic : BatchBook.list(milestone.getTopics()))
                for (var knowledge : BatchBook.list(topic.getKnowledges()))
                    for (var quiz : BatchBook.list(knowledge.getQuizTemplates()))
                        if (!indexed.containsKey(quiz.getId())) return false;
        return true;
    }

    private AuditReport scopedSeed(AuditReport seed, CourseEntity course) {
        seed = composer.contentOnly(seed);
        if (course == null || course.getRoot() == null) return seed;
        Set<String> ids = new HashSet<>();
        for (var milestone : BatchBook.list(course.getRoot().getMilestones())) {
            ids.add(milestone.getId());
            for (var topic : BatchBook.list(milestone.getTopics())) {
                ids.add(topic.getId());
                for (var knowledge : BatchBook.list(topic.getKnowledges())) {
                    ids.add(knowledge.getId());
                    for (var quiz : BatchBook.list(knowledge.getQuizTemplates())) ids.add(quiz.getId());
                }
            }
        }
        if (DefaultAnalysisComposer.index(seed.getRoot()).values().stream()
                .allMatch(n -> n.getTarget() == AuditTarget.COURSE || ids.contains(n.getEntity().getId()))) return seed;
        AuditReport cropped = composer.compose(seed,List.of());
        prune(cropped.getRoot(),ids);
        return composer.compose(cropped,List.of());
    }
    private static void prune(AuditNode node, Set<String> ids) {
        node.setChildren(BatchBook.list(node.getChildren()).stream()
                .filter(child -> ids.contains(child.getEntity().getId())).toList());
        for (AuditNode child : node.getChildren()) prune(child,ids);
    }

    private AuditReport compose(CorrectionBatch batch, String state, AuditReport primary,
            List<AnalysisSelection> history, Map<String, Recorded> runs, Map<String, AuditReport> sources) {
        var supplements = new ArrayList<AnalysisSupplement>();
        Set<String> removed = new HashSet<>();
        for (var e : history) if ("remove".equals(e.action())) removed.add(e.removesOperationId());
        for (var e : history) if (state.equals(e.state()) && !"remove".equals(e.action()) && !removed.contains(e.operationId())) {
            var recorded = runs.get(e.runId());
            var run = recorded == null ? null : recorded.run();
            AuditReport report = sources.get(e.runId());
            if (run == null || report == null) throw new IllegalStateException("Falta la corrida integrada " + e.runId());
            supplements.add(new AnalysisSupplement(run.id(), run.auditId(), e.analyzer(), run.runtime(),
                    run.finishedAt(), new LinkedHashSet<>(e.nodeIds()), report));
        }
        AuditReport annotated = composer.compose(primary, List.of());
        for (var node : DefaultAnalysisComposer.index(annotated.getRoot()).values()) {
            if (node.getTarget() != AuditTarget.QUIZ) continue;
            var origins = new LinkedHashMap<String, Object>();
            Object existing = node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
            for (String analyzer : composer.supportedAnalyzers()) if (DefaultAnalysisComposer.hasStoredOpinion(node, analyzer)) {
                if (existing instanceof Map<?, ?> recorded && recorded.get(analyzer) instanceof Map<?, ?> origin) {
                    origins.put(analyzer, origin);
                    continue;
                }
                var origin = new LinkedHashMap<String, Object>();
                origin.put("auditId", "base".equals(state) ? batch.getBaselineAuditId() : batch.getAnalysisSeedAuditId());
                origin.put("judgeVersion", DefaultAnalysisComposer.version(node, analyzer, primary));
                origin.put("runtime", runtime(node, analyzer, primary)); origin.put("integrated", false);
                origin.put("fingerprint", composer.fingerprint(analyzer, node)); origins.put(analyzer, origin);
            }
            if (!origins.isEmpty()) node.getMetadata().put(DefaultAnalysisComposer.PROVENANCE, origins);
        }
        return composer.contentOnly(composer.compose(annotated, supplements));
    }

    private Fit fit(String state, SelectiveCorrection.Run run, AuditReport source, AuditReport current, List<Approval> approvals) {
        var from = DefaultAnalysisComposer.index(source.getRoot()); var to = DefaultAnalysisComposer.index(current.getRoot());
        var ids = new ArrayList<String>(); var lost = new ArrayList<String>(); var gained = new ArrayList<String>();
        int reached = 0, replaces = 0, selected = 0, toToday = 0, toOtherVersion = 0, changesResults = 0, repeats = 0, notToday = 0;
        // F-MEDJ-R002: today's judge is the one F-ALDIA marks against; the run's verdict carries the run's runtime once integrated.
        var today = todaysJudge(run.analyzer());
        var runRuntime = JudgeFreshnessReference.runtimeOf(run.runtime());
        Set<String> asked = "complete-opinions".equals(run.action()) ? new HashSet<>(BatchBook.list(run.quizIds())) : null;
        for (var node : to.values()) if (DefaultAnalysisComposer.eligible(node, run.analyzer())) {
            if (asked != null && !asked.contains(node.getEntity().getId())) continue;
            AuditableQuiz q = (AuditableQuiz) node.getEntity();
            reached++;
            AuditNode old = from.get(q.getId());
            if (old == null || !DefaultAnalysisComposer.hasStoredOpinion(old, run.analyzer())
                    || !composer.sameInput(run.analyzer(), old, node)) continue;
            ids.add(q.getId());
            String runVersion = DefaultAnalysisComposer.version(old, run.analyzer(), source);
            boolean runIsToday = isOfToday(today, runVersion, runRuntime);
            if (!runIsToday) notToday++;
            var oldMarks = marks(node, run.analyzer()); var newMarks = marks(old, run.analyzer());
            boolean losesMarks = oldMarks.stream().anyMatch(k -> !newMarks.contains(k)), gainsMarks = newMarks.stream().anyMatch(k -> !oldMarks.contains(k));
            if (losesMarks) lost.add(q.getId());
            if (gainsMarks) gained.add(q.getId());
            var origins = node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
            Object origin = origins instanceof Map<?,?> m ? m.get(run.analyzer()) : null;
            if (origin instanceof Map<?,?> m && run.id().equals(m.get("runId"))) selected++;
            else if (DefaultAnalysisComposer.hasStoredOpinion(node, run.analyzer())) {
                replaces++;
                // F-MEDJ-R001: each replaced exercise is exactly one of four kinds, judged against today's judge, not between the two ends.
                String countedVersion = DefaultAnalysisComposer.version(node, run.analyzer(), current);
                boolean countedIsToday = isOfToday(today, countedVersion,
                        JudgeFreshnessReference.runtimeOf(runtime(node, run.analyzer(), current)));
                if (runIsToday && !countedIsToday) toToday++;
                else if (!runIsToday && (countedIsToday || !Objects.equals(runVersion, countedVersion))) toOtherVersion++;
                else if (losesMarks || gainsMarks) changesResults++;
                else repeats++;
            }
        }
        var preview = composer.compose(current, List.of(new AnalysisSupplement(run.id(), run.auditId(), run.analyzer(),
                run.runtime(), run.finishedAt(), new LinkedHashSet<>(ids), source)));
        int compatibleErrors = (int) ids.stream().filter(id -> error(from.get(id), run.analyzer())).count();
        int currentErrors = (int) to.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ && error(n, run.analyzer())).count();
        int resultErrors = (int) DefaultAnalysisComposer.index(preview.getRoot()).values().stream()
                .filter(n -> n.getTarget() == AuditTarget.QUIZ && error(n, run.analyzer())).count();
        var currentVersions = to.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ
                        && DefaultAnalysisComposer.hasStoredOpinion(n, run.analyzer()))
                .map(n -> DefaultAnalysisComposer.version(n, run.analyzer(), current)).filter(Objects::nonNull).distinct().sorted().toList();
        return new Fit(state, reached, ids.size(), reached - ids.size(), replaces, selected, ids, lost, gained,
                approvalWarnings(approvals, current, preview, run.analyzer()), ids.isEmpty() ? "Midió otro contenido o no dejó veredictos utilizables." : null,
                compatibleErrors, currentErrors, resultErrors, currentVersions,
                ids.size() - selected - replaces, toToday, toOtherVersion, changesResults, repeats,
                today.isEmpty() || ids.isEmpty() ? null : notToday == 0,
                today.map(JudgeFreshnessReference::isApproved).orElse(null));
    }

    /** F-ALDIA-R001: today's judge of an analyzer, as the catalog says; none without a catalog or for an unknown analyzer. */
    private Optional<JudgeFreshnessReference> todaysJudge(String analyzer) {
        var catalog = config.getJudgeFreshnessCatalog();
        return catalog == null || analyzer == null ? Optional.empty() : catalog.reference(analyzer);
    }

    private static boolean isOfToday(Optional<JudgeFreshnessReference> today, String version, EvaluationRuntimeIdentity runtime) {
        return today.map(judge -> judge.isOfToday(version, runtime)).orElse(false);
    }

    /** An approved change of one exercise and the findings of the tasks that were its reason (F-MOTIVO-R002). */
    private record Approval(String changeId, String quizId, List<com.learney.contentaudit.auditdomain.finding.Finding> reasons) {}

    /**
     * The approvals of a reading, each with its reasons, found once per reading: the tasks are looked up by id instead
     * of walking every task of every plan for each approval, as many times as there are runs and states.
     */
    private static List<Approval> approvals(List<BatchReview.Item> items, List<com.learney.contentaudit.refinerdomain.RefinementTask> tasks) {
        var byId = new HashMap<String, List<com.learney.contentaudit.refinerdomain.RefinementTask>>();
        for (var t : BatchBook.list(tasks)) byId.computeIfAbsent(t.getId(), k -> new ArrayList<>()).add(t);
        var result = new ArrayList<Approval>();
        for (var item : items) if (item.status() == BatchReviewDecision.Status.APPROVED && item.step() != null) {
            // F-MOTIVO-R002: a task of another exercise is never the reason of this one's change.
            Set<String> quiz = item.step().getQuizId() == null ? Set.of() : Set.of(item.step().getQuizId());
            var reasons = new ArrayList<com.learney.contentaudit.auditdomain.finding.Finding>();
            for (String id : new LinkedHashSet<>(BatchBook.list(item.taskIds())))
                for (var t : byId.getOrDefault(id, List.of()))
                    if (InheritedTasks.belongs(t, quiz, null) && t.getOrigin() != null && t.getOrigin().getFinding() != null)
                        reasons.add(t.getOrigin().getFinding());
            result.add(new Approval(item.changeId(), item.step().getQuizId(), List.copyOf(reasons)));
        }
        return List.copyOf(result);
    }

    private static List<String> approvalWarnings(List<Approval> approvals, AuditReport before, AuditReport after, String analyzer) {
        if (approvals.isEmpty()) return List.of();
        var old = DefaultAnalysisComposer.index(before.getRoot()); var now = DefaultAnalysisComposer.index(after.getRoot());
        var warnings = new LinkedHashSet<String>();
        for (var approval : approvals) {
            boolean originatedHere = approval.reasons().stream().anyMatch(f -> analyzer.equals(f.getAnalyzer()));
            if (!originatedHere) continue;
            String id = approval.quizId(); var a = old.get(id); var b = now.get(id);
            if (a == null || b == null || !DefaultAnalysisComposer.evaluated(b, analyzer)) continue;
            var oldMarks = marks(a, analyzer); var newMarks = marks(b, analyzer);
            boolean disappeared = approval.reasons().stream().filter(f -> analyzer.equals(f.getAnalyzer()))
                    .map(DefaultCurrentAnalysisService::mark).anyMatch(k -> oldMarks.contains(k) && !newMarks.contains(k));
            if (disappeared) warnings.add(approval.changeId());
        }
        return List.copyOf(warnings);
    }
    private static String mark(com.learney.contentaudit.auditdomain.finding.Finding finding) {
        return Objects.toString(finding.getRule(), "") + "|" + (finding.getIdentity() == null ? "" : Objects.toString(finding.getIdentity().getMarker(), ""));
    }
    private static Set<String> marks(AuditNode node, String analyzer) {
        var result = new LinkedHashSet<String>();
        if (node != null) for (var f : BatchBook.list(node.getFindings()))
            if (analyzer.equals(f.getAnalyzer()) && f.getResolution() != com.learney.contentaudit.auditdomain.finding.FindingResolution.RANK_ONLY) result.add(mark(f));
        return result;
    }
    private static boolean error(AuditNode node, String analyzer) {
        return node != null && BatchBook.list(node.getFindings()).stream().anyMatch(f -> analyzer.equals(f.getAnalyzer())
                && f.getResolution() != com.learney.contentaudit.auditdomain.finding.FindingResolution.RANK_ONLY);
    }
    private AuditDigest digest(String id, AuditNode root) {
        return new AuditDigest(id, digestNode(root), root.getDiagnoses() instanceof CourseDiagnoses d ? d.getJudgeCoverages() : List.of());
    }
    private DigestNode digestNode(AuditNode node) {
        String label = node.getEntity() == null ? "Curso" : node.getEntity().getLabel();
        if (node.getEntity() instanceof AuditableQuiz q) label = renderer.render(q).getStatement();
        return new DigestNode(node.getEntity() == null ? "root" : node.getEntity().getId(), node.getTarget(), label,
                node.getNumbers(), node.getFindings(), node.getUnevaluatedBy(),
                BatchBook.list(node.getChildren()).stream().map(this::digestNode).toList(), node.getPartiallyEvaluatedBy());
    }
    private CorrectionBatch batch(String id) {
        if (id == null) return config.getBatchStore().open().or(() -> config.getBatchStore().list().stream()
                .max(Comparator.comparing(b -> b.getOpenedAt() == null ? Instant.EPOCH : b.getOpenedAt())))
                .orElseThrow(() -> new IllegalArgumentException("No hay una tanda."));
        requireId(id); return config.getBatchStore().load(id).orElseThrow(() -> new IllegalArgumentException("No existe la tanda."));
    }
    private static void requireId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) throw new IllegalArgumentException("Identidad inválida.");
    }
    private static String activityId(CorrectionBatch batch, SelectiveCorrection.Run run) {
        if ("complete-opinions".equals(run.action())) return run.sourceRunId();
        return BatchBook.list(batch.getSelectiveRuns()).stream().anyMatch(r -> Objects.equals(r.id(),run.sourceRunId())
                && "reanalyze-both".equals(r.action())) ? run.sourceRunId() : run.id();
    }
    static CourseEntity cut(CourseEntity course, CefrLevel level) {
        var result = new CourseEntity(course.getId(), course.getTitle(), course.getKnowledgeIds(), null, course.getSlug());
        var root = new RootNodeEntity(); root.setId(course.getRoot().getId()); root.setLabel(course.getRoot().getLabel());
        root.setMilestones(BatchBook.list(course.getRoot().getMilestones()).stream().filter(m -> level.name().equalsIgnoreCase(m.getLabel())).toList());
        result.setRoot(root); return result;
    }
}
