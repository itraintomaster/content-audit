package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.contextnumbers.*;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.time.Instant;
import java.util.*;
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
                    && Objects.equals(composer.fingerprint(analyzer, node), composer.fingerprint(analyzer, other))
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
        List<AnalysisSelection> history = config.getBatchStore().analysisSelections(batch.getId());
        boolean writes = !List.of("read", "preview").contains(action);
        if (writes) {
            requireId(request.operationId());
            var duplicate = history.stream().filter(x -> request.operationId().equals(x.operationId())).findFirst();
            if (duplicate.isPresent()) {
                var previous = duplicate.get();
                if (!Objects.equals(previous.action(), action) || !Objects.equals(previous.runId(), request.runId())
                        || !Objects.equals(previous.state(), request.state())
                        || !Objects.equals(previous.removesOperationId(), request.removesOperationId())
                        || !Objects.equals(previous.reviewer(), request.reviewer()))
                    throw new IllegalArgumentException("La identidad de operación ya pertenece a otro pedido.");
                return view(batch, history);
            }
        }
        View view = view(batch, history);
        if (!writes) return view;
        if (batch.getStatus() != BatchStatus.CLOSED) throw new IllegalArgumentException("La tanda debe estar cerrada.");
        if (request.reviewer() == null || request.reviewer().isBlank()) throw new IllegalArgumentException("Falta quién confirma.");
        if (request.expectedRevision() == null || request.expectedRevision() != history.size()
                || request.contentVersion() == null || request.contentVersion() != batch.getContentVersion())
            throw new IllegalStateException("La selección o el contenido cambió: volvé a leer.");
        State state = view.states().stream().filter(s -> s.id().equals(request.state())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Elegí la base o la corrección vigente."));
        if (!Objects.equals(state.fingerprint(), request.contentFingerprint()))
            throw new IllegalStateException("El contenido cambió: volvé a estimar la integración.");
        String runId = request.runId(), analyzer;
        List<String> ids;
        if ("remove".equals(action)) {
            AnalysisSelection removed = history.stream().filter(e -> e.operationId().equals(request.removesOperationId())
                    && e.state().equals(request.state()) && !"remove".equals(e.action())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Elegí una integración de este estado."));
            if (history.stream().anyMatch(e -> removed.operationId().equals(e.removesOperationId())))
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
        config.getBatchStore().saveAnalysisSelection(new AnalysisSelection(request.operationId(), batch.getId(),
                request.state(), action, runId, request.removesOperationId(), analyzer, ids, history.size(),
                batch.getContentVersion(), state.fingerprint(), request.reviewer().trim(), Instant.now()));
        return view(batch, config.getBatchStore().analysisSelections(batch.getId()));
    }

    private View view(CorrectionBatch batch, List<AnalysisSelection> history) {
        CourseEntity start = config.getCourseRepository().load(config.getCoursePath());
        CourseEntity current = cut(ledger.composedCourse(batch, start), batch.getLevel());
        CourseEntity baseCourse = cut(start, batch.getLevel());
        var raw = new LinkedHashMap<String, AuditReport>();
        raw.put("base", primary(batch, "base", baseCourse));
        raw.put("correction", primary(batch, "correction", current));
        Map<String, AuditReport> sources = new HashMap<>();
        for (var run : BatchBook.list(batch.getSelectiveRuns())) if (run.auditId() != null)
            config.getAuditReportStore().load(run.auditId()).ifPresent(r -> sources.put(run.id(), r));
        var sourceIndexes = new HashMap<String, Map<String, AuditNode>>();
        sources.forEach((id, report) -> sourceIndexes.put(id, DefaultAnalysisComposer.index(report.getRoot())));
        var reports = new LinkedHashMap<String, AuditReport>();
        raw.forEach((state, report) -> reports.put(state, compose(batch, state, report, history, sources)));
        var review = reviews.read(batch.getId());
        var items = review.items();
        var tasks = new ArrayList<>(BatchBook.list(review.tasks()));
        for (var run : BatchBook.list(batch.getSelectiveRuns())) if (run.planId() != null)
            config.getRefinementPlanStore().load(run.planId()).ifPresent(p -> tasks.addAll(BatchBook.list(p.getTasks())));
        var states = new ArrayList<State>();
        for (var entry : reports.entrySet()) {
            String state = entry.getKey(); AuditReport report = entry.getValue();
            var index = DefaultAnalysisComposer.index(report.getRoot());
            var studentViews = new LinkedHashMap<String, QuizStudentView>();
            var provenance = new LinkedHashMap<String, Map<String, Object>>();
            var verdictMarks = new LinkedHashMap<String, Map<String, Map<String, Object>>>();
            var judges = freshness(report, verdictMarks);
            index.forEach((id, node) -> {
                if (node.getEntity() instanceof AuditableQuiz quiz) studentViews.put(id, renderer.render(quiz));
                if (node.getMetadata() != null && node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE) instanceof Map<?, ?> origins) {
                    var value = new LinkedHashMap<String, Object>();
                    origins.forEach((k,v) -> {
                        var origin = new LinkedHashMap<String, Object>();
                        if (v instanceof Map<?,?> o) o.forEach((key, val) -> origin.put(key.toString(), val));
                        var sourceIndex = sourceIndexes.get(origin.get("runId"));
                        BatchBook.list(batch.getSelectiveRuns()).stream()
                                .filter(r -> r.id().equals(origin.get("runId"))).findFirst()
                                .ifPresent(r -> { origin.put("contentVersion", r.contentVersion()); origin.put("activityId", activityId(batch,r)); });
                        var savedNode = sourceIndex == null ? node : sourceIndex.get(id);
                        if (savedNode != null && savedNode.getEntity() instanceof AuditableQuiz quiz)
                            origin.put("studentView", renderer.render(quiz));
                        value.put(k.toString(), origin);
                    });
                    provenance.put(id, value);
                    // Show the exact saved quiz that this measurement read, not a quiz fetched from the active base.
                    for (Object origin : origins.values()) if (origin instanceof Map<?,?> o && o.get("runId") instanceof String runId) {
                        AuditReport source = sources.get(runId);
                        if (source != null) {
                            var sourceNode = sourceIndexes.get(runId).get(id);
                            if (sourceNode != null && sourceNode.getEntity() instanceof AuditableQuiz quiz)
                                studentViews.put(id, renderer.render(quiz));
                        }
                    }
                }
            });
            var warnings = new LinkedHashSet<>(approvalWarnings(items, tasks, raw.get(state), report, QUALITY));
            // A replacement keeps the earlier measurement as history, so the warning survives a reread/restart.
            for (var e : history) if (state.equals(e.state()) && !"remove".equals(e.action()) && sources.containsKey(e.runId()))
                warnings.addAll(approvalWarnings(items, tasks, sources.get(e.runId()), report, e.analyzer()));
            states.add(new State(state, "base".equals(state) ? "La base" : "Corrección " + BatchBook.correctionRevision(batch),
                    courseFingerprint.apply("base".equals(state) ? baseCourse : current), digest("current-" + state, report.getRoot()),
                    studentViews, provenance, List.copyOf(warnings), comparable(reports.get("base"), report),
                    "base".equals(state) ? "A0" : BatchBook.list(batch.getClosings()).isEmpty() ? null : "A" + batch.getClosings().size(),
                    inapplicable(batch, index), judges, verdictMarks));
        }
        var loose = new ArrayList<LooseRun>();
        for (var run : BatchBook.list(batch.getSelectiveRuns())) {
            if ("reanalyze-both".equals(run.action())) continue; // Coordination record; its two measurements have their own sources.
            AuditReport source = sources.get(run.id());
            if (source == null || !List.of("DONE", "PARTIAL").contains(run.status()) || !composer.supportedAnalyzers().contains(run.analyzer())) continue;
            var sourceNodes = DefaultAnalysisComposer.index(source.getRoot());
            int evaluated = (int) sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ
                    && DefaultAnalysisComposer.hasStoredOpinion(n, run.analyzer())).count();
            int errors = (int) sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ && error(n, run.analyzer())).count();
            var fits = new ArrayList<Fit>();
            for (var entry : reports.entrySet()) fits.add(fit(entry.getKey(), run, source, entry.getValue(), items, tasks));
            // The configured judge version is not evidence that a reused result came from that version.
            var versions = sourceNodes.values().stream().filter(n -> n.getTarget() == AuditTarget.QUIZ)
                    .map(n -> DefaultAnalysisComposer.version(n, run.analyzer(), source)).filter(Objects::nonNull).distinct().sorted().toList();
            loose.add(new LooseRun(run.id(), run.analyzer(), run.auditId(), run.status(), run.contentVersion(), run.runtime(),
                    String.join(", ", versions), run.finishedAt() != null ? run.finishedAt() : run.createdAt(),
                    run.spent(), evaluated, errors, fits, config.getAnalyzerCatalog() == null ? run.analyzer()
                            : config.getAnalyzerCatalog().find(run.analyzer()).map(AnalyzerDescriptor::getDisplayName).orElse(run.analyzer()), activityId(batch,run)));
        }
        return new View(batch.getId(), batch.getContentVersion(), history.size(), states, loose, List.copyOf(history), composer.supportedAnalyzers().stream().sorted().toList());
    }

    @Override public AuditReport report(CorrectionBatch batch, String state, CourseEntity course) {
        var sources = new HashMap<String, AuditReport>();
        for (var run : BatchBook.list(batch.getSelectiveRuns())) if (run.auditId() != null)
            config.getAuditReportStore().load(run.auditId()).ifPresent(r -> sources.put(run.id(), r));
        return compose(batch, state, primary(batch, state, course),
                config.getBatchStore().analysisSelections(batch.getId()), sources);
    }

    @Override public ComparisonReports comparisonReports(CorrectionBatch batch, CourseEntity start, CourseEntity composed) {
        AuditReport baseline = report(batch, "base", start);
        AuditReport projected = report(batch, "correction", composed);
        AuditReport approved = Objects.equals(batch.getStartAuditId(), batch.getBaselineAuditId()) ? baseline
                : composer.compose(config.getAuditReportStore().load(batch.getStartAuditId())
                        .orElseThrow(() -> new IllegalStateException("Falta el análisis de inicio de la tanda.")), List.of());
        var a = DefaultAnalysisComposer.index(baseline.getRoot());
        var startIndex = DefaultAnalysisComposer.index(approved.getRoot());
        var b = DefaultAnalysisComposer.index(projected.getRoot());
        // Missing ends or mixed criteria never prove an improvement or regression.
        for (String analyzer : composer.supportedAnalyzers()) {
            for (var entry : b.entrySet()) if (entry.getValue().getTarget() == AuditTarget.QUIZ
                    && DefaultAnalysisComposer.eligible(entry.getValue(), analyzer)) {
                var before = a.get(entry.getKey()); var after = entry.getValue();
                String oldVersion = before == null ? null : DefaultAnalysisComposer.version(before, analyzer, baseline);
                String newVersion = DefaultAnalysisComposer.version(after, analyzer, projected);
                if (before == null || !DefaultAnalysisComposer.evaluated(before, analyzer)
                        || !DefaultAnalysisComposer.evaluated(after, analyzer) || oldVersion == null || newVersion == null
                        || !Objects.equals(oldVersion, newVersion)
                        || !Objects.equals(runtime(before, analyzer, baseline), runtime(after, analyzer, projected))) {
                    invalidate(before, analyzer); invalidate(after, analyzer);
                }
            }
            for (var entry : b.entrySet()) if (DefaultAnalysisComposer.eligible(entry.getValue(), analyzer)) {
                var before = startIndex.get(entry.getKey()); var after = entry.getValue();
                if (before == null || !DefaultAnalysisComposer.evaluated(before, analyzer)
                        || !DefaultAnalysisComposer.evaluated(after, analyzer)
                        || DefaultAnalysisComposer.version(before, analyzer, approved) == null
                        || !Objects.equals(DefaultAnalysisComposer.version(before, analyzer, approved), DefaultAnalysisComposer.version(after, analyzer, projected))
                        || !Objects.equals(runtime(before, analyzer, approved), runtime(after, analyzer, projected))) {
                    invalidate(before, analyzer); invalidate(after, analyzer);
                }
            }
            for (var n : startIndex.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
            for (var n : a.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
            for (var n : b.values()) if (n.getTarget() != AuditTarget.QUIZ) n.getScores().remove(analyzer);
        }
        baseline = composer.compose(baseline, List.of()); projected = composer.compose(projected, List.of());
        return new ComparisonReports(baseline, composer.compose(approved, List.of()), projected);
    }
    private Map<String, List<String>> comparable(AuditReport before, AuditReport after) {
        var a = DefaultAnalysisComposer.index(before.getRoot());
        var result = new LinkedHashMap<String, List<String>>();
        for (var n : DefaultAnalysisComposer.index(after.getRoot()).values()) if (n.getTarget() == AuditTarget.QUIZ) {
            var old = a.get(n.getEntity().getId());
            var names = new ArrayList<String>();
            if (old != null) for (String name : n.getScores().keySet()) {
                if (!DefaultAnalysisComposer.evaluated(n, name) || !DefaultAnalysisComposer.evaluated(old, name)) continue;
                if (!composer.supportedAnalyzers().contains(name) || (
                        DefaultAnalysisComposer.version(old, name, before) != null
                        && Objects.equals(DefaultAnalysisComposer.version(old, name, before), DefaultAnalysisComposer.version(n, name, after))
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
     * exercises -- or topics, for the judge of topics -- are up to date, from before and not measured. Reads only.
     */
    private List<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount> freshness(AuditReport report,
            Map<String, Map<String, Map<String, Object>>> marks) {
        var catalog = config.getJudgeFreshnessCatalog();
        if (catalog == null || !(report.getRoot().getDiagnoses() instanceof CourseDiagnoses d)) return List.of();
        var index = DefaultAnalysisComposer.index(report.getRoot());
        var result = new ArrayList<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessCount>();
        for (var coverage : BatchBook.list(d.getJudgeCoverages())) {
            String judge = coverage.getJudgeName();
            var reference = judge == null ? Optional.<com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference>empty()
                    : catalog.reference(judge);
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
                if (DefaultAnalysisComposer.evaluated(node, judge)) {
                    String version = DefaultAnalysisComposer.version(node, judge, report);
                    var runtime = com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference.runtimeOf(runtime(node, judge, report));
                    var mark = reference.get().mark(version, runtime);
                    verdict = new UnitVerdict(mark, version, runtime);
                    var shown = new LinkedHashMap<String, Object>();
                    shown.put("freshness", mark.freshness()); shown.put("staleCause", mark.cause());
                    shown.put("judgeVersion", version); shown.put("versionName", version == null ? null : namer.looseName(version));
                    shown.put("runtimeName", com.learney.contentaudit.auditdomain.judging.JudgeVersionNamer.runtimeName(runtime));
                    marks.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(judge, shown);
                }
                units.merge(unitId, verdict, UnitVerdict::worse);
            }
            var tally = new com.learney.contentaudit.auditdomain.judging.JudgeFreshnessTally(reference.get(), displayName(judge), unit);
            for (var verdict : units.values()) {
                if (verdict.mark() == null) tally.notMeasured(1);
                else tally.add(verdict.mark(), verdict.version(), verdict.runtime(), 1);
            }
            result.add(tally.count());
        }
        return List.copyOf(result);
    }
    /** What a judge left on a unit; a topic is from before as soon as one of its exercises is. */
    private record UnitVerdict(com.learney.contentaudit.auditdomain.judging.JudgeVerdictMark mark, String version,
            com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity runtime) {
        static final UnitVerdict NONE = new UnitVerdict(null, null, null);
        static UnitVerdict worse(UnitVerdict a, UnitVerdict b) {
            if (a.mark() == null) return b;
            if (b.mark() == null || !a.mark().isUpToDate()) return a;
            return b;
        }
    }
    private static boolean reads(AuditNode node, String judge) {
        return node.getScores() != null && node.getScores().containsKey(judge)
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
    private static void invalidate(AuditNode node, String analyzer) {
        if (node == null || !DefaultAnalysisComposer.eligible(node, analyzer)) return;
        node.getScores().remove(analyzer); node.getFindings().removeIf(f -> analyzer.equals(f.getAnalyzer()));
        if (!node.getUnevaluatedBy().contains(analyzer)) node.getUnevaluatedBy().add(analyzer);
    }

    private AuditReport primary(CorrectionBatch batch, String state, CourseEntity course) {
        if (!"base".equals(state) && batch.getAnalysisSeedAuditId() != null && batch.getAnalysisSeedContentVersion() == batch.getContentVersion())
            return config.getAuditReportStore().load(batch.getAnalysisSeedAuditId()).orElseThrow(() -> new IllegalStateException("Falta la medición congelada antes de reanalizar."));
        if ("base".equals(state)) return config.getAuditReportStore().load(batch.getBaselineAuditId())
                .orElseThrow(() -> new IllegalStateException("Falta el informe de la base."));
        // Existing analyzers retain their ordinary zero-budget behavior. A loose judge is never selected by recency.
        var names = BatchBook.list(batch.getAnalyzers()).stream().filter(n -> !QUALITY.equals(n)).toList();
        var policies = new LinkedHashMap<String, EvaluationRunPolicy>();
        for (String name : names) policies.put(name, new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));
        return config.getAuditEngine().runAudit(config.getCourseMapper().map(course), new AnalyzerRunSelection(names, policies));
    }

    private AuditReport compose(CorrectionBatch batch, String state, AuditReport primary,
            List<AnalysisSelection> history, Map<String, AuditReport> sources) {
        var supplements = new ArrayList<AnalysisSupplement>();
        Set<String> removed = new HashSet<>();
        for (var e : history) if ("remove".equals(e.action())) removed.add(e.removesOperationId());
        for (var e : history) if (state.equals(e.state()) && !"remove".equals(e.action()) && !removed.contains(e.operationId())) {
            var run = BatchBook.list(batch.getSelectiveRuns()).stream().filter(r -> r.id().equals(e.runId())).findFirst().orElse(null);
            AuditReport report = sources.get(e.runId());
            if (run == null || report == null) throw new IllegalStateException("Falta la corrida integrada " + e.runId());
            supplements.add(new AnalysisSupplement(run.id(), run.auditId(), e.analyzer(), run.runtime(),
                    run.finishedAt(), new LinkedHashSet<>(e.nodeIds()), report));
        }
        AuditReport annotated = composer.compose(primary, List.of());
        for (var node : DefaultAnalysisComposer.index(annotated.getRoot()).values()) {
            if (node.getTarget() != AuditTarget.QUIZ) continue;
            var origins = new LinkedHashMap<String, Object>();
            for (String analyzer : composer.supportedAnalyzers()) if (DefaultAnalysisComposer.hasStoredOpinion(node, analyzer)) {
                var origin = new LinkedHashMap<String, Object>();
                origin.put("auditId", "base".equals(state) ? batch.getBaselineAuditId() : batch.getAnalysisSeedAuditId());
                origin.put("judgeVersion", DefaultAnalysisComposer.version(node, analyzer, primary));
                origin.put("runtime", runtime(node, analyzer, primary)); origin.put("integrated", false);
                origin.put("fingerprint", composer.fingerprint(analyzer, node)); origins.put(analyzer, origin);
            }
            if (!origins.isEmpty()) node.getMetadata().put(DefaultAnalysisComposer.PROVENANCE, origins);
        }
        return composer.compose(annotated, supplements);
    }

    private Fit fit(String state, SelectiveCorrection.Run run, AuditReport source, AuditReport current, List<BatchReview.Item> items, List<com.learney.contentaudit.refinerdomain.RefinementTask> tasks) {
        var from = DefaultAnalysisComposer.index(source.getRoot()); var to = DefaultAnalysisComposer.index(current.getRoot());
        var ids = new ArrayList<String>(); var lost = new ArrayList<String>(); var gained = new ArrayList<String>();
        int reached = 0, replaces = 0, selected = 0;
        for (var node : to.values()) if (DefaultAnalysisComposer.eligible(node, run.analyzer())) {
            if ("complete-opinions".equals(run.action()) && !run.quizIds().contains(node.getEntity().getId())) continue;
            AuditableQuiz q = (AuditableQuiz) node.getEntity();
            reached++;
            AuditNode old = from.get(q.getId());
            if (old == null || !DefaultAnalysisComposer.hasStoredOpinion(old, run.analyzer())
                    || !Objects.equals(composer.fingerprint(run.analyzer(), old), composer.fingerprint(run.analyzer(), node))) continue;
            ids.add(q.getId());
            var origins = node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
            Object origin = origins instanceof Map<?,?> m ? m.get(run.analyzer()) : null;
            if (origin instanceof Map<?,?> m && run.id().equals(m.get("runId"))) selected++;
            else if (DefaultAnalysisComposer.hasStoredOpinion(node, run.analyzer())) replaces++;
            var oldMarks = marks(node, run.analyzer()); var newMarks = marks(old, run.analyzer());
            if (oldMarks.stream().anyMatch(k -> !newMarks.contains(k))) lost.add(q.getId());
            if (newMarks.stream().anyMatch(k -> !oldMarks.contains(k))) gained.add(q.getId());
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
                approvalWarnings(items, tasks, current, preview, run.analyzer()), ids.isEmpty() ? "Midió otro contenido o no dejó veredictos utilizables." : null,
                compatibleErrors, currentErrors, resultErrors, currentVersions);
    }

    private static List<String> approvalWarnings(List<BatchReview.Item> items, List<com.learney.contentaudit.refinerdomain.RefinementTask> tasks, AuditReport before, AuditReport after, String analyzer) {
        var old = DefaultAnalysisComposer.index(before.getRoot()); var now = DefaultAnalysisComposer.index(after.getRoot());
        var warnings = new LinkedHashSet<String>();
        for (var item : items) if (item.status() == BatchReviewDecision.Status.APPROVED && item.step() != null) {
            boolean originatedHere = BatchBook.list(tasks).stream().anyMatch(t -> BatchBook.list(item.taskIds()).contains(t.getId())
                    && t.getOrigin() != null && t.getOrigin().getFinding() != null
                    && analyzer.equals(t.getOrigin().getFinding().getAnalyzer()));
            if (!originatedHere) continue;
            String id = item.step().getQuizId(); var a = old.get(id); var b = now.get(id);
            if (a == null || b == null || !DefaultAnalysisComposer.evaluated(b, analyzer)) continue;
            var oldMarks = marks(a, analyzer); var newMarks = marks(b, analyzer);
            boolean disappeared = BatchBook.list(tasks).stream().filter(t -> BatchBook.list(item.taskIds()).contains(t.getId()))
                    .filter(t -> t.getOrigin() != null && t.getOrigin().getFinding() != null)
                    .map(t -> t.getOrigin().getFinding()).filter(f -> analyzer.equals(f.getAnalyzer()))
                    .map(DefaultCurrentAnalysisService::mark).anyMatch(k -> oldMarks.contains(k) && !newMarks.contains(k));
            if (disappeared) warnings.add(item.changeId());
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
