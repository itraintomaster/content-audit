package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.*;
import com.learney.contentaudit.revisiondomain.comparison.ChangeOutcome;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** A budgeted pass on the current composition. No rule stage, rollback, closing or course writer. */
final class DefaultContextOpinionService implements ContextOpinionService {
    /** The judges that read the course before the exercise: the headers of the earlier topics are part of what they see. */
    static final Set<String> CURRICULUM_JUDGES = Set.of("a1-didactic", "naturalness");
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final BatchReviewService reviews;
    private final Function<CourseEntity, String> fingerprint;
    private final PreliminaryOpinionSource preliminarySource;

    DefaultContextOpinionService(BatchServiceConfig config, BatchChangeLedger ledger,
            BatchReviewService reviews, Function<CourseEntity, String> fingerprint) {
        this(config, ledger, reviews, fingerprint, null);
    }
    DefaultContextOpinionService(BatchServiceConfig config, BatchChangeLedger ledger,
            BatchReviewService reviews, Function<CourseEntity, String> fingerprint,
            PreliminaryOpinionSource preliminarySource) {
        this.config = config; this.ledger = ledger; this.reviews = reviews; this.fingerprint = fingerprint;
        this.preliminarySource = preliminarySource;
    }
    private record Scope(String analyzer, CourseEntity before, CourseEntity after,
            List<RevisionProposal> proposals, List<RevisionProposal> context, List<Integer> pendingByEvaluator, Judge judge) {}

    @Override public State execute(Request request) {
        if (request != null && List.of("estimate-preliminary", "import-preliminary").contains(request.action())) {
            if ("import-preliminary".equals(request.action()))
                return config.getBatchStore().withAnalysisLock(request.batchId(), () -> preliminary(request));
            return preliminary(request);
        }
        if (request == null || !List.of("estimate", "measure").contains(request.action()))
            throw new IllegalArgumentException("Pedí estimate o measure.");
        CorrectionBatch batch = config.getBatchStore().load(request.batchId()).orElseThrow(
                () -> new IllegalArgumentException("No existe la tanda."));
        if ("measure".equals(request.action())) {
            if (request.operationId() == null || !request.operationId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
                throw new IllegalArgumentException("Falta una identidad de operación válida.");
            for (Run run : safe(batch.getOpinionRuns())) if (request.operationId().equals(run.id())) {
                if (!Objects.equals(request, run.request()))
                    throw new IllegalArgumentException("La identidad pertenece a otro pedido.");
                return state(batch, request, run.fingerprint(), run.judges());
            }
        }
        if (batch.getStatus() != BatchStatus.CLOSED && batch.getStatus() != BatchStatus.OPEN)
            throw new IllegalArgumentException("La tanda está publicada o tiene otra operación pendiente.");
        BatchReview review = reviews.read(batch.getId());
        var start = BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
        if (request.scope() != null && !"correction".equals(request.scope()))
            throw new IllegalArgumentException("Elegí el alcance correction o ejercicios seleccionados.");
        boolean correction = "correction".equals(request.scope());
        if (correction && (batch.getStatus() != BatchStatus.CLOSED || config.getCurrentAnalysisService() == null))
            throw new IllegalArgumentException("Completar opiniones necesita una corrección cerrada con análisis vigente.");
        var composed = ledger.composedCourse(batch, start);
        // F-OPINA-R005: with a current analysis, a loose opinion reads what the analysis reads -- its level and, for the
        // judges of the curriculum, the headers before it -- so that what it leaves fits there.
        boolean asTheAnalysis = config.getCurrentAnalysisService() != null && batch.getLevel() != null;
        List<Scope> scopes = correction ? correctionScopes(batch, review, start, composed) : asTheAnalysis
                ? scopes(request, review, DefaultCurrentAnalysisService.cut(start, batch.getLevel()),
                        DefaultCurrentAnalysisService.cut(composed, batch.getLevel()), true)
                : scopes(request, review, start, composed);
        List<Judge> judges = scopes.stream().map(Scope::judge).toList();
        var selected = correction ? scopeSelection(scopes) : selection(request);
        String stamp = hash(fingerprint.apply(composed) + request.scope() + request.wholeTopic() + selected + judges
                + (correction ? config.getBatchStore().analysisSelections(batch.getId()).size() : ""));
        State estimate = state(batch, request, stamp, judges, selected);
        if ("estimate".equals(request.action())) return estimate;
        if (request.contentVersion() == null || request.contentVersion() != batch.getContentVersion()
                || !Objects.equals(request.fingerprint(), stamp))
            throw new IllegalArgumentException("El contenido, el alcance o el juez cambió: volvé a estimar.");
        if (!estimate.allowed()) throw new IllegalArgumentException(estimate.reason());
        if (request.queryCap() == null || request.queryCap() < 0 || request.queryCap() > 10000
                || request.reviewer() == null || request.reviewer().isBlank())
            throw new IllegalArgumentException("Falta un tope de 0 a 10000 y el autor del pedido.");
        // F-OPINA-R005: an opinion asked for loose exercises enters the current analysis like Completar's, where the
        // batch admits integrating (closed, or open with a later correction).
        boolean integrates = config.getCurrentAnalysisService() != null && (correction || batch.getStatus() == BatchStatus.CLOSED
                || config.getBatchStore().hasSuccessor(batch.getId()));
        BatchStatus previous = batch.getStatus();
        Instant created = Instant.now();
        var proposals = scopes.stream().flatMap(s -> s.context().stream()).distinct().toList();
        Run run = new Run(request.operationId(), request, BatchBook.correctionRevision(batch), batch.getContentVersion(),
                stamp, request.queryCap(), 0, "RUNNING", created, null, judges, proposals, List.of(), null);
        if (integrates) config.getCurrentAnalysisService().seed(batch, start);
        put(batch, run);
        batch.setStatus(BatchStatus.RUNNING);
        config.getBatchStore().saveIfVersion(batch, request.contentVersion(), previous);
        List<Result> results = new ArrayList<>();
        int spent = 0;
        try {
            for (Scope scope : scopes) {
                AuditReport before = audit(scope.before(), scope.analyzer(), 0);
                // A zero cap means reuse only for paid providers; instant/local judges still run for free.
                int perEvaluatorCap = capWithin(Math.max(0, request.queryCap() - spent), scope.pendingByEvaluator());
                AuditReport after = audit(scope.after(), scope.analyzer(), perEvaluatorCap);
                spent += spent(after, scope.analyzer());
                for (RevisionProposal proposal : scope.proposals())
                    results.add(new Result(scope.analyzer(), outcome(scope.analyzer(), proposal, before, after, review)));
                if (integrates && composes(scope)) recordSupplement(batch, request, scope, after, created);
                put(batch, new Run(run.id(), request, run.correctionRevision(), run.contentVersion(), stamp,
                        run.reserved(), spent, "RUNNING", created, null, judges, proposals, List.copyOf(results), null));
                config.getBatchStore().save(batch);
            }
            boolean complete = results.stream().allMatch(r -> safe(r.outcome().getEvaluatedAnalyzers()).contains(r.analyzer()));
            run = new Run(run.id(), request, run.correctionRevision(), run.contentVersion(), stamp, run.reserved(), spent,
                    complete ? "DONE" : "PARTIAL", created, Instant.now(), judges, proposals, List.copyOf(results), null);
        } catch (RuntimeException error) {
            run = new Run(run.id(), request, run.correctionRevision(), run.contentVersion(), stamp, run.reserved(), spent,
                    "FAILED", created, Instant.now(), judges, proposals, List.copyOf(results), "La medición se interrumpió; el gasto confirmado puede ser incompleto. Conservá el tope reservado y revisá sus resultados antes de pedir otra.");
            throw error;
        } finally {
            put(batch, run);
            batch.setQueriesSpent(batch.getQueriesSpent() + spent);
            batch.setStatus(previous);
            config.getBatchStore().save(batch);
        }
        if (integrates) integrateSupplements(batch, request, scopes.stream().filter(this::composes).toList());
        return state(batch, request, stamp, judges, selected);
    }

    /** Attaches original EXAM evidence for reading; it never opens a judge or the course analysis composer. */
    private State preliminary(Request request) {
        if (preliminarySource == null) throw new IllegalArgumentException("No hay una fuente de opiniones preliminares.");
        if (blank(request.sourceRef())) throw new IllegalArgumentException("Falta la fuente original del examen.");
        boolean importing = "import-preliminary".equals(request.action());
        CorrectionBatch batch = config.getBatchStore().load(request.batchId()).orElseThrow(
                () -> new IllegalArgumentException("No existe la tanda."));
        if (importing) {
            if (request.operationId() == null || !request.operationId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
                throw new IllegalArgumentException("Falta una identidad de operación válida.");
            for (Run run : safe(batch.getOpinionRuns())) if (request.operationId().equals(run.id())) {
                if (!"PRELIMINARY".equals(run.status()) || !Objects.equals(request, run.request()))
                    throw new IllegalArgumentException("La identidad pertenece a otro pedido.");
                return preliminaryState(batch, request, run.fingerprint(), run.judges(), preliminarySelection(run.preliminaryOpinions()));
            }
        }
        if (batch.getStatus() != BatchStatus.CLOSED && batch.getStatus() != BatchStatus.OPEN)
            throw new IllegalArgumentException("La tanda está publicada o tiene otra operación pendiente.");
        if (request.wholeTopic() || request.scope() != null)
            throw new IllegalArgumentException("La fuente preliminar sólo se adjunta a sus ejercicios medidos.");
        List<PreliminaryOpinion> sourced = safe(preliminarySource.load(request.sourceRef()));
        if (sourced.isEmpty()) throw new IllegalArgumentException("La fuente no tiene opiniones preliminares.");
        var available = preliminarySelection(sourced);
        var selected = request.selection() == null ? available : selection(request);
        if (selected.isEmpty()) throw new IllegalArgumentException("Seleccioná al menos una opinión preliminar.");
        for (var entry : selected.entrySet())
            if (!safe(available.get(entry.getKey())).containsAll(entry.getValue()))
                throw new IllegalArgumentException("La selección no existe en la fuente original.");
        List<PreliminaryOpinion> opinions = sourced.stream()
                .filter(o -> safe(selected.get(o.analyzer())).contains(o.quizId()))
                .sorted(Comparator.comparing(PreliminaryOpinion::analyzer).thenComparing(PreliminaryOpinion::quizId)).toList();
        var current = reviews.read(batch.getId()).proposals();
        var snapshots = new ArrayList<RevisionProposal>();
        var identities = new HashSet<String>();
        for (PreliminaryOpinion opinion : opinions) {
            validatePreliminary(opinion);
            if (!identities.add(opinion.analyzer() + "\u0000" + opinion.quizId()))
                throw new IllegalArgumentException("La fuente repite una opinión del mismo ejercicio y juez.");
            RevisionProposal proposal = safe(current).stream().filter(p -> p.getNodeTarget() == AuditTarget.QUIZ
                    && opinion.quizId().equals(p.getNodeId())).findFirst().orElseThrow(
                            () -> new IllegalArgumentException("Una corrección fue retirada: volvé a leer."));
            if (!Objects.equals(opinion.snapshot(), proposal.getElementAfter()))
                throw new IllegalArgumentException("El ejercicio medido no coincide con la propuesta vigente: " + opinion.quizId());
            if (!snapshots.contains(proposal)) snapshots.add(proposal);
        }
        CourseEntity start = BatchCourses.start(batch, config.getCourseRepository().load(config.getCoursePath()), config.getBatchStore());
        CourseEntity composed = ledger.composedCourse(batch, start);
        String stamp = hash(fingerprint.apply(composed) + request.sourceRef() + selected + opinions.stream()
                .map(o -> List.of(o.quizId(), o.analyzer(), o.evaluatorId(), o.judgeVersion(), o.runtime().toString(),
                        o.namespace(), o.contentFingerprint(), o.payload(), o.recordedAt().toString(),
                        o.sourceRef(), o.sourceRecordHash())).toList());
        List<Judge> judges = preliminaryJudges(opinions);
        if (!importing) return preliminaryState(batch, request, stamp, judges, selected);
        if (request.contentVersion() == null || request.contentVersion() != batch.getContentVersion()
                || !Objects.equals(request.fingerprint(), stamp))
            throw new IllegalArgumentException("El contenido o la fuente cambió: volvé a estimar.");
        if (blank(request.reviewer())) throw new IllegalArgumentException("Falta el autor de la importación.");
        if (request.queryCap() != null && request.queryCap() != 0)
            throw new IllegalArgumentException("Importar evidencia guardada no admite consultas nuevas.");
        Instant now = Instant.now();
        Run run = new Run(request.operationId(), request, BatchBook.correctionRevision(batch), batch.getContentVersion(),
                stamp, 0, 0, "PRELIMINARY", now, now, judges, List.copyOf(snapshots), List.of(),
                "Opinión preliminar: juez sin examen aprobado. Fuente EXAM; no resuelve tareas ni habilita al juez.", opinions);
        put(batch, run);
        config.getBatchStore().saveIfVersion(batch, request.contentVersion(), batch.getStatus());
        return preliminaryState(batch, request, stamp, judges, selected);
    }

    private static void validatePreliminary(PreliminaryOpinion opinion) {
        Map<String, String> evaluators = Map.of("naturalness", "exam:native-usage-judge", "a1-didactic", "exam:a1-didactic-judge",
                "pragmatism", "exam:pragmatism-judge");
        if (opinion == null || blank(opinion.quizId()) || blank(opinion.analyzer()) || !evaluators.containsKey(opinion.analyzer())
                || !Objects.equals(evaluators.get(opinion.analyzer()), opinion.evaluatorId())
                || !"EXAM".equals(opinion.namespace()) || blank(opinion.judgeVersion())
                || opinion.runtime() == null || !opinion.runtime().isExplicit()
                || blank(opinion.payload()) || blank(opinion.contentFingerprint()) || opinion.recordedAt() == null
                || blank(opinion.sourceRef()) || opinion.sourceRecordHash() == null
                || !opinion.sourceRecordHash().matches("[a-f0-9]{64}")
                || opinion.snapshot() == null || opinion.snapshot().getNodeTarget() != AuditTarget.QUIZ
                || !opinion.quizId().equals(opinion.snapshot().getNodeId()) || opinion.snapshot().getQuiz() == null
                || !opinion.quizId().equals(opinion.snapshot().getQuiz().getId()))
            throw new IllegalArgumentException("La opinión no conserva una fuente EXAM auténtica y su contenido medido.");
    }

    private static Map<String, List<String>> preliminarySelection(List<PreliminaryOpinion> opinions) {
        var grouped = new TreeMap<String, List<String>>();
        for (PreliminaryOpinion opinion : opinions) {
            validatePreliminary(opinion);
            grouped.computeIfAbsent(opinion.analyzer(), key -> new ArrayList<>()).add(opinion.quizId());
        }
        grouped.replaceAll((key, ids) -> ids.stream().distinct().sorted().toList());
        return grouped;
    }

    private List<Judge> preliminaryJudges(List<PreliminaryOpinion> opinions) {
        var result = new ArrayList<Judge>();
        for (var entry : preliminarySelection(opinions).entrySet()) {
            var owned = opinions.stream().filter(o -> entry.getKey().equals(o.analyzer())).toList();
            var first = owned.get(0);
            if (owned.stream().anyMatch(o -> !Objects.equals(first.judgeVersion(), o.judgeVersion())
                    || !Objects.equals(first.runtime(), o.runtime())))
                throw new IllegalArgumentException("La fuente mezcla versiones o procedencias del mismo juez.");
            String label = config.getAnalyzerCatalog().list().stream().filter(c -> entry.getKey().equals(c.getName()))
                    .map(AnalyzerDescriptor::getDisplayName).filter(Objects::nonNull).findFirst().orElse(entry.getKey());
            int topics = (int) owned.stream().map(o -> o.snapshot().getQuiz().getKnowledgeId()).distinct().count();
            result.add(new Judge(entry.getKey(), label, "QUIZ", entry.getValue().size(), topics, 0, 0, false,
                    "Juez sin examen aprobado. Opinión preliminar de EXAM.", first.judgeVersion(), first.runtime().toString(), 0, 0));
        }
        return result;
    }

    private static State preliminaryState(CorrectionBatch batch, Request request, String stamp, List<Judge> judges,
            Map<String, List<String>> selected) {
        return new State(batch.getId(), batch.getContentVersion(), stamp, true, null, 0, 0, judges,
                selected, false, List.copyOf(safe(batch.getOpinionRuns())), request.scope());
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private List<Scope> correctionScopes(CorrectionBatch batch, BatchReview review, CourseEntity start, CourseEntity composed) {
        CourseEntity current = DefaultCurrentAnalysisService.cut(composed, batch.getLevel());
        AuditReport active = config.getCurrentAnalysisService().report(batch, "correction", current);
        // F-OPINA-R001: the same list the analysis read says, never a count of its own.
        var missing = MissingOpinions.of(active, config.getCurrentAnalysisService().supportedAnalyzers());
        if (missing.isEmpty()) return List.of();
        // Include unchanged exercises: an opinion belongs to the correction's content, not only to its proposals.
        var snapshots = new ArrayList<RevisionProposal>();
        for (var milestone : safe(current.getRoot().getMilestones())) for (var topic : safe(milestone.getTopics()))
            for (var knowledge : safe(topic.getKnowledges())) for (var quiz : safe(knowledge.getQuizTemplates())) {
                var p = new RevisionProposal(); p.setProposalId("opinion-" + quiz.getId());
                p.setNodeTarget(AuditTarget.QUIZ); p.setNodeId(quiz.getId());
                var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, quiz.getId(), quiz, null);
                p.setElementBefore(snapshot); p.setElementAfter(snapshot); snapshots.add(p);
            }
        var all = new BatchReview(review.batchId(), review.correctionRevision(), review.contentVersion(), review.report(),
                review.tasks(), snapshots, review.items(), review.analyses(), review.history());
        return scopes(new Request("estimate", batch.getId(), missing, false, null, null, null, null, null), all,
                DefaultCurrentAnalysisService.cut(start, batch.getLevel()), current, true);
    }

    private static Map<String, List<String>> scopeSelection(List<Scope> scopes) {
        var selected = new TreeMap<String, List<String>>();
        for (Scope s : scopes) selected.put(s.analyzer(), s.proposals().stream().map(RevisionProposal::getNodeId).sorted().toList());
        return selected;
    }

    private String supplementId(Request request, String analyzer) { return "opinion-" + hash(request.operationId() + ":" + analyzer).substring(0, 24); }

    private void recordSupplement(CorrectionBatch batch, Request request, Scope scope, AuditReport after, Instant created) {
        String auditId = config.getAuditReportStore().save(after), id = supplementId(request, scope.analyzer());
        var ids = scope.proposals().stream().map(RevisionProposal::getNodeId).toList();
        int evaluated = evaluated(after, ids, scope.analyzer());
        var runs = new ArrayList<>(safe(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run(id, "complete-opinions", scope.analyzer(), request.operationId(),
                BatchBook.correctionRevision(batch), batch.getContentVersion(), fingerprint.apply(scope.after()),
                scope.judge().judgeVersion(), scope.judge().runtime(), auditId, null, List.of(), ids,
                request.queryCap(), request.queryCap(), spent(after, scope.analyzer()), ids.size(), evaluated, ids.size() - evaluated,
                evaluated == ids.size() ? "DONE" : "PARTIAL", request.reviewer(), created, Instant.now(), null,
                false, batch.getContentVersion(), fingerprint.apply(scope.after()), null));
        batch.setSelectiveRuns(runs);
    }

    /**
     * How many of {@code ids} the judge left a verdict on in {@code after}: a score, or a saved abstention, which is a
     * verdict although it carries no score (F-ALDIA-R003 inv. 6).
     */
    static int evaluated(AuditReport after, List<String> ids, String analyzer) {
        return (int) nodes(after).stream().filter(n -> n.getTarget() == AuditTarget.QUIZ && n.getEntity() != null
                && ids.contains(n.getEntity().getId())
                && (complete(n, analyzer) || DefaultAnalysisComposer.abstention(n, analyzer).isPresent())).count();
    }

    /** F-OPINA-R005 inv. 4: only a judge the current analysis composes enters it; the others stay in their run. */
    private boolean composes(Scope scope) {
        return config.getCurrentAnalysisService().supportedAnalyzers().contains(scope.analyzer());
    }

    private void integrateSupplements(CorrectionBatch batch, Request request, List<Scope> scopes) {
        for (Scope scope : scopes) {
            var service = config.getCurrentAnalysisService();
            var view = service.execute(new CurrentAnalysisService.Request("read", batch.getId(), null, null, null, null, null, null, null, null));
            var run = view.looseRuns().stream().filter(r -> r.id().equals(supplementId(request, scope.analyzer()))).findFirst().orElseThrow();
            var fit = run.fits().stream().filter(f -> "correction".equals(f.state())).findFirst().orElseThrow();
            // F-OPINA-R004: a failed, exhausted or empty query never becomes an opinion nor hides one.
            if (fit.compatible() == 0 || fit.selected() == fit.compatible()) continue;
            // F-OPINA-R005 inv. 2: it replaces an opinion only with today's version; otherwise it stays loose, to decide.
            String action = fit.replaces() == 0 ? "integrate" : Boolean.TRUE.equals(fit.runToday()) ? "replace" : null;
            if (action == null) continue;
            var state = view.states().stream().filter(s -> "correction".equals(s.id())).findFirst().orElseThrow();
            service.execute(new CurrentAnalysisService.Request(action, batch.getId(), "correction", run.id(),
                    run.id() + "-" + action, null, view.revision(), batch.getContentVersion(), state.fingerprint(), request.reviewer()));
        }
    }

    private List<Scope> scopes(Request request, BatchReview review, CourseEntity start, CourseEntity composed) {
        return scopes(request, review, start, composed, false);
    }
    private List<Scope> scopes(Request request, BatchReview review, CourseEntity start, CourseEntity composed,
            boolean correctionContext) {
        var wanted = selection(request);
        if (wanted.isEmpty()) throw new IllegalArgumentException("Seleccioná al menos un ejercicio y un juez.");
        List<Scope> result = new ArrayList<>();
        for (var entry : wanted.entrySet()) {
            String analyzer = entry.getKey();
            boolean curriculum = correctionContext && CURRICULUM_JUDGES.contains(analyzer);
            if (config.getAnalyzerCatalog().list().stream().noneMatch(c -> analyzer.equals(c.getName())))
                throw new IllegalArgumentException("Juez desconocido: " + analyzer);
            Set<String> quizIds = new LinkedHashSet<>(entry.getValue());
            var selected = review.proposals().stream().filter(p -> p.getNodeTarget() == AuditTarget.QUIZ
                    && quizIds.contains(p.getNodeId())).toList();
            if (selected.size() != quizIds.size()) throw new IllegalArgumentException("Una corrección fue retirada: volvé a leer.");
            Set<String> topics = new LinkedHashSet<>();
            selected.forEach(p -> topics.add(p.getElementAfter().getQuiz().getKnowledgeId()));
            CourseEntity fullAfter = topicCut(composed, topics, curriculum);
            AuditReport topicTrial = audit(fullAfter, analyzer, 0);
            JudgeCoverageDiagnosis coverage = coverage(topicTrial, analyzer);
            boolean thematic = coverage != null && coverage.getUnit() == AuditTarget.KNOWLEDGE;
            boolean all = thematic || request.wholeTopic();
            CourseEntity after = all ? fullAfter : cut(composed, quizIds, curriculum);
            CourseEntity before = all ? topicCut(start, topics, curriculum) : cut(start, quizIds, curriculum);
            AuditReport trial = all ? topicTrial : audit(after, analyzer, 0);
            coverage = coverage(trial, analyzer);
            int pending = pending(coverage);
            var descriptor = config.getAnalyzerCatalog().list().stream().filter(c -> analyzer.equals(c.getName())).findFirst().orElseThrow();
            boolean instant = descriptor.getCost() == AnalysisCost.INSTANT;
            boolean allowed = instant || coverage != null && (coverage.getStanding() != null && coverage.getStanding().isConsultsOnCourse()
                    && coverage.getUnavailableReason() == null);
            String reason = allowed ? null : coverage == null ? "No consta el estado de este juez." : coverage.getUnavailableReason() != null ? coverage.getUnavailableReason()
                    : coverage.getStanding() == null ? "No consta el examen de este juez." : coverage.getStanding().getReason();
            int count = nodes(trial).stream().filter(n -> n.getTarget() == AuditTarget.QUIZ).toList().size();
            var context = review.proposals().stream().filter(p -> p.getNodeTarget() == AuditTarget.QUIZ
                    && p.getElementAfter() != null && p.getElementAfter().getQuiz() != null
                    && topics.contains(p.getElementAfter().getQuiz().getKnowledgeId())).toList();
            var outputs = request.wholeTopic() ? context : selected;
            int selectedCost = thematic ? pending : pending(coverage(audit(cut(composed, quizIds, curriculum), analyzer, 0), analyzer));
            int topicCost = pending(coverage(topicTrial, analyzer));
            result.add(new Scope(analyzer, before, after, outputs, context,
                    coverage == null ? List.of() : safe(coverage.getEvaluators()).stream().map(e -> e.getCoverage().getPending() + e.getCoverage().getFailed()).toList(),
                    new Judge(analyzer, config.getAnalyzerCatalog().list().stream().filter(c -> analyzer.equals(c.getName()))
                            .findFirst().map(AnalyzerDescriptor::getDisplayName).orElse(analyzer),
                            thematic ? "KNOWLEDGE" : "QUIZ", count, topics.size(), pending, reused(coverage), allowed, reason,
                            coverage == null || coverage.getStanding() == null ? null : coverage.getStanding().getJudgeVersion(),
                            coverage == null || coverage.getStanding() == null ? null : Objects.toString(coverage.getStanding().getRuntimeIdentity(), null), selectedCost, topicCost)));
        }
        return result;
    }
    // The native policy caps each evaluator separately. Allocate a per-evaluator cap whose
    // sum of possible new consultations fits the one ceiling confirmed by the operator.
    private static int capWithin(int remaining, List<Integer> pending) {
        if (pending.isEmpty() || pending.stream().allMatch(n -> n == 0)) return 0;
        int low = 0, high = Math.min(remaining, pending.stream().mapToInt(Integer::intValue).max().orElse(0));
        while (low < high) {
            int candidate = (low + high + 1) / 2;
            long total = pending.stream().mapToLong(n -> Math.min(n, candidate)).sum();
            if (total <= remaining) low = candidate; else high = candidate - 1;
        }
        return low;
    }
    private State state(CorrectionBatch batch, Request request, String stamp, List<Judge> judges) {
        return state(batch, request, stamp, judges, selection(request));
    }
    private State state(CorrectionBatch batch, Request request, String stamp, List<Judge> judges, Map<String, List<String>> selected) {
        var blocked = judges.stream().filter(j -> !j.allowed()).findFirst();
        return new State(batch.getId(), batch.getContentVersion(), stamp, blocked.isEmpty(),
                blocked.map(Judge::reason).orElse(null), judges.stream().mapToInt(Judge::pendingQueries).sum(),
                judges.stream().mapToInt(Judge::reusedQueries).sum(), judges, selected, request.wholeTopic(),
                List.copyOf(safe(batch.getOpinionRuns())), request.scope());
    }
    private static Map<String, List<String>> selection(Request request) {
        var result = new TreeMap<String, List<String>>();
        if ("correction".equals(request.scope()) && request.selection() == null) return result;
        if (request.selection() == null || request.selection().size() > 100) throw new IllegalArgumentException("Alcance inválido.");
        request.selection().forEach((judge, ids) -> {
            if (judge == null || !judge.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}") || ids == null || ids.isEmpty()
                    || ids.size() > 10000 || ids.stream().anyMatch(id -> id == null || id.isBlank()))
                throw new IllegalArgumentException("Alcance inválido.");
            result.put(judge, ids.stream().distinct().sorted().toList());
        });
        return result;
    }
    private AuditReport audit(CourseEntity course, String analyzer, int cap) {
        return config.getAuditEngine().runAudit(config.getCourseMapper().map(course),
                new AnalyzerRunSelection(List.of(analyzer), Map.of(analyzer,
                        new EvaluationRunPolicy(cap, false, null, null, null, 1))));
    }
    private static CourseEntity cut(CourseEntity course, Set<String> ids) {
        return cut(course, ids, false);
    }
    /** Keep curriculum headers even when their exercises already have opinions. They are judge input. */
    private static CourseEntity cut(CourseEntity course, Set<String> ids, boolean curriculum) {
        return ChangedCourseCuts.cut(course, k -> {
            var quizzes = safe(k.getQuizTemplates()).stream().filter(q -> ids.contains(q.getId())).toList();
            return quizzes.isEmpty() && !curriculum ? null : new KnowledgeEntity(k.getId(), k.getCode(), k.getKind(), k.getLabel(),
                    k.getOldId(), k.getParentId(), k.isIsRule(), k.getInstructions(), k.getOrder(), k.getSlug(), quizzes, k.getSentenceMode());
        });
    }
    private static CourseEntity topicCut(CourseEntity course, Set<String> topics, boolean curriculum) {
        return ChangedCourseCuts.cut(course, k -> topics.contains(k.getId()) ? k : !curriculum ? null
                : new KnowledgeEntity(k.getId(), k.getCode(), k.getKind(), k.getLabel(), k.getOldId(), k.getParentId(),
                        k.isIsRule(), k.getInstructions(), k.getOrder(), k.getSlug(), List.of(), k.getSentenceMode()));
    }
    private static ChangeOutcome outcome(String analyzer, RevisionProposal proposal, AuditReport before,
            AuditReport after, BatchReview review) {
        AuditNode b = node(before, proposal.getNodeId()), a = node(after, proposal.getNodeId());
        List<Finding> old = new ArrayList<>(findings(b, analyzer));
        for (var task : safe(review.tasks())) if (task.getOrigin() != null) {
            for (Finding f : join(task.getOrigin().getFinding(), task.getOrigin().getCoveredFindings()))
                if (analyzer.equals(f.getAnalyzer()) && f.getNode() != null && proposal.getNodeId().equals(f.getNode().getNodeId())
                        && old.stream().noneMatch(o -> identity(o).equals(identity(f)))) old.add(f);
        }
        List<Finding> now = findings(a, analyzer);
        boolean measured = complete(a, analyzer);
        List<Finding> fixed = measured ? old.stream().filter(f -> now.stream().noneMatch(n -> identity(n).equals(identity(f)))).toList() : List.of();
        List<Finding> persistent = now.stream().filter(f -> old.stream().anyMatch(o -> identity(o).equals(identity(f)))).toList();
        List<Finding> gained = complete(b, analyzer) ? now.stream().filter(f -> old.stream().noneMatch(o -> identity(o).equals(identity(f)))).toList() : List.of();
        var outcome = new ChangeOutcome(proposal.getProposalId(), AuditTarget.QUIZ, proposal.getNodeId(), !gained.isEmpty(),
                gained, fixed, measured ? List.of() : List.of(analyzer));
        // Without a measured Before, unfamiliar findings cannot honestly be called new.
        outcome.setAfterFindings(now);
        outcome.setPersistentFindings(complete(b, analyzer) ? persistent : now);
        outcome.setEvaluatedAnalyzers(measured ? List.of(analyzer) : List.of());
        return outcome;
    }
    private static boolean complete(AuditNode node, String analyzer) {
        return node != null && node.getScores() != null && node.getScores().get(analyzer) != null
                && Double.isFinite(node.getScores().get(analyzer)) && !safe(node.getUnevaluatedBy()).contains(analyzer)
                && !safe(node.getPartiallyEvaluatedBy()).contains(analyzer);
    }
    private static String identity(Finding finding) {
        return finding.getIdentity() != null && finding.getIdentity().getKey() != null ? finding.getIdentity().getKey()
                : finding.getAnalyzer() + ":" + finding.getRule() + ":"
                        + (finding.getNode() == null ? "" : finding.getNode().getTarget() + ":" + finding.getNode().getNodeId());
    }
    private static List<Finding> join(Finding finding, List<Finding> covered) {
        var result = new ArrayList<>(safe(covered)); if (finding != null) result.add(finding); return result;
    }
    private static List<Finding> findings(AuditNode node, String analyzer) {
        return node == null ? List.of() : safe(node.getFindings()).stream().filter(f -> analyzer.equals(f.getAnalyzer())).toList();
    }
    private static List<AuditNode> nodes(AuditReport report) {
        List<AuditNode> result = new ArrayList<>(); if (report != null && report.getRoot() != null) walk(report.getRoot(), result); return result;
    }
    private static void walk(AuditNode node, List<AuditNode> result) { result.add(node); safe(node.getChildren()).forEach(n -> walk(n, result)); }
    private static AuditNode node(AuditReport report, String id) { return nodes(report).stream().filter(n -> n.getEntity() != null && id.equals(n.getEntity().getId())).findFirst().orElse(null); }
    private static JudgeCoverageDiagnosis coverage(AuditReport report, String analyzer) {
        return report != null && report.getRoot() != null && report.getRoot().getDiagnoses() instanceof CourseDiagnoses d
                ? safe(d.getJudgeCoverages()).stream().filter(c -> analyzer.equals(c.getJudgeName())).findFirst().orElse(null) : null;
    }
    private static int pending(JudgeCoverageDiagnosis c) { return c == null ? 0 : c.getCost() != null
            ? c.getCost().getPendingConsultations() : safe(c.getEvaluators()).stream().mapToInt(e -> e.getCoverage().getPending() + e.getCoverage().getFailed()).sum(); }
    private static int reused(JudgeCoverageDiagnosis c) { return c == null ? 0 : safe(c.getEvaluators()).stream().mapToInt(e -> e.getCoverage().getReused()).sum(); }
    private static int spent(AuditReport report, String analyzer) { var c = coverage(report, analyzer); return c == null ? 0 : safe(c.getEvaluators()).stream().mapToInt(e -> e.getCoverage().getEvaluatedInRun() + e.getCoverage().getFailed()).sum(); }
    private static void put(CorrectionBatch batch, Run run) {
        var runs = new ArrayList<>(safe(batch.getOpinionRuns())); runs.removeIf(r -> r.id().equals(run.id())); runs.add(run); batch.setOpinionRuns(runs);
    }
    private static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }
}
