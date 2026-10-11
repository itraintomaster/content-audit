package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.RevisionProposal;
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
    private final BatchServiceConfig config;
    private final BatchChangeLedger ledger;
    private final BatchReviewService reviews;
    private final Function<CourseEntity, String> fingerprint;

    DefaultContextOpinionService(BatchServiceConfig config, BatchChangeLedger ledger,
            BatchReviewService reviews, Function<CourseEntity, String> fingerprint) {
        this.config = config; this.ledger = ledger; this.reviews = reviews; this.fingerprint = fingerprint;
    }
    private record Scope(String analyzer, CourseEntity before, CourseEntity after,
            List<RevisionProposal> proposals, List<RevisionProposal> context, List<Integer> pendingByEvaluator, Judge judge) {}

    @Override public State execute(Request request) {
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
        var start = config.getCourseRepository().load(config.getCoursePath());
        var composed = ledger.composedCourse(batch, start);
        List<Scope> scopes = scopes(request, review, start, composed);
        List<Judge> judges = scopes.stream().map(Scope::judge).toList();
        String stamp = hash(fingerprint.apply(composed) + request.wholeTopic() + selection(request) + judges);
        State estimate = state(batch, request, stamp, judges);
        if ("estimate".equals(request.action())) return estimate;
        if (request.contentVersion() == null || request.contentVersion() != batch.getContentVersion()
                || !Objects.equals(request.fingerprint(), stamp))
            throw new IllegalArgumentException("El contenido, el alcance o el juez cambió: volvé a estimar.");
        if (!estimate.allowed()) throw new IllegalArgumentException(estimate.reason());
        if (request.queryCap() == null || request.queryCap() < 0 || request.queryCap() > 10000
                || request.reviewer() == null || request.reviewer().isBlank())
            throw new IllegalArgumentException("Falta un tope de 0 a 10000 y el autor del pedido.");
        BatchStatus previous = batch.getStatus();
        Instant created = Instant.now();
        var proposals = scopes.stream().flatMap(s -> s.context().stream()).distinct().toList();
        Run run = new Run(request.operationId(), request, BatchBook.correctionRevision(batch), batch.getContentVersion(),
                stamp, request.queryCap(), 0, "RUNNING", created, null, judges, proposals, List.of(), null);
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
        return state(batch, request, stamp, judges);
    }

    private List<Scope> scopes(Request request, BatchReview review, CourseEntity start, CourseEntity composed) {
        var wanted = selection(request);
        if (wanted.isEmpty()) throw new IllegalArgumentException("Seleccioná al menos un ejercicio y un juez.");
        List<Scope> result = new ArrayList<>();
        for (var entry : wanted.entrySet()) {
            String analyzer = entry.getKey();
            if (config.getAnalyzerCatalog().list().stream().noneMatch(c -> analyzer.equals(c.getName())))
                throw new IllegalArgumentException("Juez desconocido: " + analyzer);
            Set<String> quizIds = new LinkedHashSet<>(entry.getValue());
            var selected = review.proposals().stream().filter(p -> p.getNodeTarget() == AuditTarget.QUIZ
                    && quizIds.contains(p.getNodeId())).toList();
            if (selected.size() != quizIds.size()) throw new IllegalArgumentException("Una corrección fue retirada: volvé a leer.");
            Set<String> topics = new LinkedHashSet<>();
            selected.forEach(p -> topics.add(p.getElementAfter().getQuiz().getKnowledgeId()));
            CourseEntity fullAfter = ChangedCourseCuts.cut(composed, k -> topics.contains(k.getId()) ? k : null);
            AuditReport topicTrial = audit(fullAfter, analyzer, 0);
            JudgeCoverageDiagnosis coverage = coverage(topicTrial, analyzer);
            boolean thematic = coverage != null && coverage.getUnit() == AuditTarget.KNOWLEDGE;
            boolean all = thematic || request.wholeTopic();
            CourseEntity after = all ? fullAfter : cut(composed, quizIds);
            CourseEntity before = all ? ChangedCourseCuts.cut(start, k -> topics.contains(k.getId()) ? k : null) : cut(start, quizIds);
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
            int selectedCost = thematic ? pending : pending(coverage(audit(cut(composed, quizIds), analyzer, 0), analyzer));
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
        var blocked = judges.stream().filter(j -> !j.allowed()).findFirst();
        return new State(batch.getId(), batch.getContentVersion(), stamp, blocked.isEmpty(),
                blocked.map(Judge::reason).orElse(null), judges.stream().mapToInt(Judge::pendingQueries).sum(),
                judges.stream().mapToInt(Judge::reusedQueries).sum(), judges, selection(request), request.wholeTopic(),
                List.copyOf(safe(batch.getOpinionRuns())));
    }
    private static Map<String, List<String>> selection(Request request) {
        var result = new TreeMap<String, List<String>>();
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
        return ChangedCourseCuts.cut(course, k -> {
            var quizzes = safe(k.getQuizTemplates()).stream().filter(q -> ids.contains(q.getId())).toList();
            return quizzes.isEmpty() ? null : new KnowledgeEntity(k.getId(), k.getCode(), k.getKind(), k.getLabel(),
                    k.getOldId(), k.getParentId(), k.isIsRule(), k.getInstructions(), k.getOrder(), k.getSlug(), quizzes, k.getSentenceMode());
        });
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
