package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/** Bounded draft work, separate from construction and from the historical batch analyses. */
final class DefaultMiniTheoryCorrectionService implements MiniTheoryCorrectionService {
    private final BatchStore batches;
    private final BatchReviewService reviews;
    private final BatchChangeLedger ledger;
    private final CourseRepository courses;
    private final Path coursePath;
    private final MiniTheoryWorkspaceStore store;
    private final CourseMapper mapper;
    private final AuditEngine engine;
    private final Supplier<MiniTheoryRewriter> rewriter;

    DefaultMiniTheoryCorrectionService(BatchStore batches, BatchReviewService reviews, BatchChangeLedger ledger,
            CourseRepository courses, Path coursePath, MiniTheoryWorkspaceStore store,
            CourseMapper mapper, AuditEngine engine, Supplier<MiniTheoryRewriter> rewriter) {
        this.batches = batches; this.reviews = reviews; this.ledger = ledger; this.courses = courses;
        this.coursePath = coursePath; this.store = store; this.mapper = mapper; this.engine = engine;
        this.rewriter = rewriter;
    }

    @Override public State execute(Request request) {
        Objects.requireNonNull(request, "Falta el pedido.");
        if (request.action() == null) fail("Falta la operación de miniteoría.");
        if (request.batchId() == null || request.knowledgeId() == null) fail("Falta tanda o ejercicio.");
        if ("read".equals(request.action()) || "estimate".equals(request.action())) return work(request);
        if (request.operationId() == null || request.operationId().isBlank()) fail("Falta identidad de operación.");
        if (request.reviewer() == null || request.reviewer().isBlank()) fail("Falta el autor de la operación.");
        return store.exclusive(request.batchId(), () -> work(request));
    }

    private State work(Request r) {
        CorrectionBatch batch = batch(r.batchId());
        CourseEntity start = BatchCourses.start(batch, courses.load(coursePath), batches);
        CourseEntity composed = ledger.composedCourse(batch, start);
        KnowledgeEntity topic = BatchBook.place(composed, r.knowledgeId()).orElseThrow(
                () -> new IllegalArgumentException("No existe ese ejercicio en el curso.")).knowledge();
        if (!topic.getId().equals(r.knowledgeId())) fail("El alcance debe identificar un ejercicio, no un quiz.");
        Workspace workspace = store.load(batch.getId());
        var savedReview = reviews.read(batch.getId());
        Set<String> approved = new HashSet<>();
        for (QuizTemplateEntity q : BatchBook.list(topic.getQuizTemplates())) {
            var items = savedReview.items().stream().filter(i -> q.getId().equals(i.step().getQuizId())).toList();
            if (!items.isEmpty() && items.stream().allMatch(i -> i.status() == BatchReviewDecision.Status.APPROVED)) {
                approved.add(q.getId());
            }
        }
        List<Candidate> candidates = candidates(topic, workspace, savedReview, batch.getContentVersion());
        KnowledgeEntity original = BatchBook.place(start, r.knowledgeId()).orElseThrow().knowledge();
        for (var q : BatchBook.list(original.getQuizTemplates())) if (!blank(q.getMiniTheory())) {
            candidates.add(new Candidate("course:" + q.getId(), q.getId(), q.getMiniTheory(), "COURSE", "UNREVIEWED"));
        }
        catalog(candidates, composed, start, savedReview, topic.getId());
        for (Draft d : workspace.drafts()) if (!topic.getId().equals(d.knowledgeId()) && d.contentVersion() == batch.getContentVersion()
                && candidates.stream().noneMatch(c -> c.text().equals(d.text()))) {
            candidates.add(new Candidate(d.id(), d.quizId(), d.text(), "DRAFT", "PENDING", d.knowledgeId(),
                    BatchBook.place(composed, d.knowledgeId()).map(p -> p.knowledge().getLabel()).orElse(d.knowledgeId())));
        }
        if ("read".equals(r.action())) return state(batch, topic, approved, candidates, workspace, null);
        if (r.operationId() != null && (job(workspace, r.operationId()) != null
                || workspace.drafts().stream().anyMatch(d -> d.id().startsWith(r.operationId() + ":"))
                || workspace.feedback().stream().anyMatch(f -> f.id().equals(r.operationId()))
                || BatchBook.list(batch.getChanges()).stream().anyMatch(c -> Objects.equals(c.getChangeId(), "mini-" + r.operationId())))) {
            checkIdentity(r, workspace, batch);
            return state(batch, topic, approved, candidates, workspace, null);
        }
        if (!"estimate".equals(r.action())) writable(batch, r);
        if ("commit".equals(r.action())) batches.requireEditableContent(batch);
        List<QuizTemplateEntity> selected = selected(topic, r.quizIds());
        if (!List.of("estimate", "analyze").contains(r.action()) && !r.includeApproved()
                && selected.stream().anyMatch(q -> approved.contains(q.getId()))) {
            fail("Hay quizzes aprobados: excluilos o confirmá explícitamente su inclusión.");
        }
        if (List.of("save", "feedback").contains(r.action())) {
            if ("feedback".equals(r.action())) {
                if (blank(r.feedback())) fail("Escribí el feedback.");
                var feedback = new ArrayList<>(workspace.feedback());
                feedback.add(new Feedback(r.operationId(), topic.getId(), ids(selected), r.feedback().trim(),
                        r.reviewer(), batch.getContentVersion(), Instant.now()));
                workspace = new Workspace(workspace.drafts(), feedback, workspace.jobs());
            } else {
                if (blank(r.text())) fail("La miniteoría no puede quedar vacía.");
                Candidate source = source(r.sourceId(), candidates);
                if (source != null && !source.text().equals(r.text())) fail("La existente cambió: elegí edición manual.");
                var drafts = new ArrayList<>(workspace.drafts());
                for (var q : selected) drafts.add(draft(r, batch, q, r.text(), source == null ? "MANUAL" : "EXISTING",
                        r.sourceId(), "edición humana"));
                workspace = new Workspace(drafts, workspace.feedback(), workspace.jobs());
            }
            store.save(batch.getId(), workspace);
        } else if ("rewrite".equals(r.action())) {
            workspace = rewrite(r, batch, topic, selected, candidates, workspace);
        } else if ("commit".equals(r.action())) {
            final Workspace pending = workspace;
            final String pendingBatchId = batch.getId();
            final KnowledgeEntity pendingTopic = topic;
            workspace = batches.withAnalysisLock(pendingBatchId, () -> {
                CorrectionBatch latest = batch(pendingBatchId);
                batches.requireEditableContent(latest); writable(latest, r);
                return commit(r, latest, start, pendingTopic, pending);
            });
            batch = batch(batch.getId());
            topic = BatchBook.place(ledger.composedCourse(batch, start), r.knowledgeId()).orElseThrow().knowledge();
            return work(new Request("read", null, r.batchId(), r.knowledgeId(), null, null, null,
                    false, null, null, null, null, null, null));
        } else if ("estimate".equals(r.action()) || "analyze".equals(r.action())) {
            var drafts = requestedDrafts(r, workspace, topic, batch);
            CourseEntity cut = analysisCourse(composed, topic, drafts);
            Estimate estimate = estimate(cut);
            if ("estimate".equals(r.action())) return state(batch, topic, approved, candidates, workspace, estimate);
            workspace = analyze(r, cut, estimate, workspace);
            return state(batch, topic, approved, candidates, workspace, estimate);
        } else fail("Operación desconocida: " + r.action());
        return work(new Request("read", null, r.batchId(), r.knowledgeId(), null, null, null,
                false, null, null, null, null, null, null));
    }

    private Workspace rewrite(Request r, CorrectionBatch batch, KnowledgeEntity topic,
            List<QuizTemplateEntity> selected, List<Candidate> candidates, Workspace workspace) {
        Job previous = job(workspace, r.operationId());
        if (previous != null) return workspace; // Never repeat an uncertain/finished paid operation.
        if (blank(r.feedback())) fail("Escribí el feedback.");
        int cap = cap(r);
        if (cap < selected.size()) fail("El tope debe cubrir como máximo una consulta por quiz seleccionado.");
        Job job = new Job(r.operationId(), "rewrite", topic.getId(), ids(selected), List.of(), "RUNNING",
                cap, 0, 0, null, null, null, Instant.now());
        var contexts = new ArrayList<>(workspace.feedback());
        contexts.add(new Feedback(r.operationId(), topic.getId(), ids(selected), r.feedback(), r.reviewer(), batch.getContentVersion(), Instant.now()));
        workspace = new Workspace(workspace.drafts(), contexts, workspace.jobs());
        workspace = put(workspace, job); store.save(batch.getId(), workspace);
        var draftIds = new ArrayList<String>();
        try {
            MiniTheoryRewriter agent = rewriter.get();
            var options = new ArrayList<>(candidates);
            for (var q : selected) {
                // Charge before launching: a crash cannot silently restore this allowance.
                job = new Job(job.id(), job.action(), job.knowledgeId(), job.quizIds(), List.copyOf(draftIds),
                        "RUNNING", cap, job.reserved() + 1, job.spent(), null, null, null, job.createdAt());
                workspace = put(workspace, job); store.save(batch.getId(), workspace);
                RewriteResult result = agent.rewrite(new RewriteInput(r.feedback(), q.getId(), topic, q,
                        relevant(options, topic, q, r.feedback())));
                if (result == null || blank(result.text())) fail("El agente no devolvió una miniteoría válida.");
                Candidate source = source(result.sourceId(), options);
                if (source != null && !source.text().equals(result.text())) fail("Reutilizar exige copiar el texto literal.");
                Draft draft = draft(r, batch, q, result.text(), source == null ? "AGENT" : "EXISTING",
                        result.sourceId(), result.provenance());
                var drafts = new ArrayList<>(workspace.drafts()); drafts.add(draft); draftIds.add(draft.id());
                options.add(new Candidate(draft.id(), q.getId(), draft.text(), "DRAFT", "PENDING"));
                job = new Job(job.id(), job.action(), job.knowledgeId(), job.quizIds(), List.copyOf(draftIds),
                        "RUNNING", cap, job.reserved(), job.spent() + 1, null, null, null, job.createdAt());
                workspace = put(new Workspace(drafts, workspace.feedback(), workspace.jobs()), job);
                store.save(batch.getId(), workspace);
            }
            job = finish(job, "DONE", job.spent(), null, null);
        } catch (RuntimeException e) {
            job = finish(job, "FAILED", job.spent(), e.getMessage(), null);
        }
        workspace = put(workspace, job); store.save(batch.getId(), workspace); return workspace;
    }

    private Workspace analyze(Request r, CourseEntity cut, Estimate estimate, Workspace workspace) {
        if (job(workspace, r.operationId()) != null) return workspace;
        if (!Objects.equals(r.fingerprint(), estimate.fingerprint())) fail("El contexto cambió: volvé a estimar.");
        if (!estimate.allowed()) fail(estimate.reason());
        int cap = cap(r);
        Job job = new Job(r.operationId(), "analyze", r.knowledgeId(), r.quizIds(), r.draftIds(), "RUNNING",
                cap, cap, 0, estimate.fingerprint(), null, null, Instant.now());
        workspace = put(workspace, job); store.save(r.batchId(), workspace);
        try {
            AuditReport report = audit(cut, cap);
            JudgeCoverageDiagnosis coverage = coverage(report);
            int spent = coverage == null ? 0 : BatchBook.list(coverage.getEvaluators()).stream()
                    .filter(e -> e != null && e.getCoverage() != null)
                    .mapToInt(e -> e.getCoverage().getEvaluatedInRun()).sum();
            job = finish(job, coverage != null && coverage.getNotEvaluated() == 0 ? "DONE" : "PARTIAL", spent, null, report);
        } catch (RuntimeException e) { job = finish(job, "FAILED", 0, e.getMessage(), null); }
        workspace = put(workspace, job); store.save(r.batchId(), workspace); return workspace;
    }

    private Workspace commit(Request r, CorrectionBatch batch, CourseEntity start, KnowledgeEntity topic, Workspace workspace) {
        if (BatchBook.list(batch.getChanges()).stream().anyMatch(c -> Objects.equals(c.getChangeId(), "mini-" + r.operationId()))) {
            return workspace;
        }
        var drafts = requestedDrafts(r, workspace, topic, batch);
        if (drafts.isEmpty()) fail("Elegí un borrador para incorporar.");
        var steps = new ArrayList<BatchChangeStep>();
        for (Draft d : drafts) {
            QuizTemplateEntity q = BatchBook.copy(d.before().getQuiz()); q.setMiniTheory(d.text());
            if (!Objects.equals(d.text(), d.before().getQuiz().getMiniTheory())) steps.add(new BatchChangeStep(d.quizId(),
                    List.of(ExercisePart.MINI_THEORY), new CourseElementSnapshot(AuditTarget.QUIZ, d.quizId(), q, null)));
        }
        if (steps.isEmpty()) return workspace;
        // Existing ledger composes primitives and preserves every historical closing and review event.
        ledger.record(batch, new BatchChange("mini-" + r.operationId(), BatchChangeOrigin.JOSE,
                "human-mini-theory", topic.getId(), List.of(), steps, null, false, Instant.now(), null), start);
        batch.setContentVersion(batch.getContentVersion() + 1);
        batches.saveIfVersion(batch, r.contentVersion(), BatchStatus.CLOSED);
        return workspace;
    }

    private List<Draft> requestedDrafts(Request r, Workspace workspace, KnowledgeEntity topic, CorrectionBatch batch) {
        var found = new ArrayList<Draft>(); var quizzes = new HashSet<String>();
        for (String id : safe(r.draftIds())) {
            Draft d = workspace.drafts().stream().filter(x -> id.equals(x.id())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No existe el borrador " + id));
            if (!topic.getId().equals(d.knowledgeId()) || !safe(r.quizIds()).contains(d.quizId()) || !quizzes.add(d.quizId())) {
                fail("El borrador no pertenece al alcance o hay dos versiones del mismo quiz.");
            }
            QuizTemplateEntity current = BatchBook.list(topic.getQuizTemplates()).stream()
                    .filter(q -> q.getId().equals(d.quizId())).findFirst().orElseThrow();
            if (d.contentVersion() != batch.getContentVersion() || !Objects.equals(current, d.before().getQuiz())) {
                fail("El borrador parte de contenido anterior: releé y guardá una nueva versión.");
            }
            found.add(d);
        }
        return found;
    }

    private CourseEntity analysisCourse(CourseEntity composed, KnowledgeEntity topic, List<Draft> drafts) {
        var selector = new CorrectionBatch();
        selector.setQuizProposals(BatchBook.list(topic.getQuizTemplates()).stream()
                .map(q -> new BatchProposalRef(q.getId(), q.getId())).toList());
        // Identical before/after deliberately uses topicsBefore: it still contains every quiz of this topic.
        CourseEntity cut = ChangedCourseCuts.of(composed, composed, BatchChanges.of(selector, composed)).topicsBefore();
        var temporary = new CorrectionBatch(); var changes = new ArrayList<BatchChange>();
        for (Draft d : drafts) {
            var q = BatchBook.copy(d.before().getQuiz()); q.setMiniTheory(d.text());
            changes.add(new BatchChange(d.id(), BatchChangeOrigin.JOSE, null, topic.getId(), List.of(),
                    List.of(new BatchChangeStep(d.quizId(), List.of(ExercisePart.MINI_THEORY),
                            new CourseElementSnapshot(AuditTarget.QUIZ, d.quizId(), q, null))), null, false, null, null));
        }
        temporary.setChanges(changes);
        return ledger.composedCourse(temporary, cut);
    }

    private Estimate estimate(CourseEntity cut) {
        AuditReport report = audit(cut, 0);
        JudgeCoverageDiagnosis c = coverage(report);
        boolean allowed = c != null && c.getStanding() != null && c.getStanding().isConsultsOnCourse()
                && c.getUnavailableReason() == null;
        int pending = c == null ? 0 : c.getCost() != null ? c.getCost().getPendingConsultations()
                : BatchBook.list(c.getEvaluators()).stream().filter(e -> e != null && e.getCoverage() != null)
                        .mapToInt(e -> e.getCoverage().getPending() + e.getCoverage().getFailed()).sum();
        String reason = allowed ? null : c != null && c.getUnavailableReason() != null ? c.getUnavailableReason()
                : "Miniteoría no tiene un juez vigente habilitado para este contexto.";
        int count = cut == null ? 0 : countQuizzes(cut);
        // Same content under another effective judge is a different confirmation.
        String identity = c == null || c.getStanding() == null ? "unavailable" : store.fingerprintIdentity(
                Map.of("standing", c.getStanding(), "versions", BatchBook.list(c.getVersions())));
        return new Estimate(store.fingerprint(cut) + ":" + identity, count, pending, allowed, reason, List.of("mini-theory"));
    }

    private AuditReport audit(CourseEntity course, int cap) {
        return engine.runAudit(mapper.map(course), new AnalyzerRunSelection(List.of("mini-theory"),
                Map.of("mini-theory", new EvaluationRunPolicy(cap, false, null, null, null, 1))));
    }
    private static JudgeCoverageDiagnosis coverage(AuditReport r) {
        if (r != null && r.getRoot() != null && r.getRoot().getDiagnoses() instanceof CourseDiagnoses d) {
            return BatchBook.list(d.getJudgeCoverages()).stream().filter(c -> "mini-theory".equals(c.getJudgeName()))
                    .findFirst().orElse(null);
        }
        return null;
    }
    private static int countQuizzes(CourseEntity c) {
        return BatchBook.list(c.getRoot().getMilestones()).stream().flatMap(m -> BatchBook.list(m.getTopics()).stream())
                .flatMap(t -> BatchBook.list(t.getKnowledges()).stream()).mapToInt(k -> BatchBook.list(k.getQuizTemplates()).size()).sum();
    }
    private State state(CorrectionBatch b, KnowledgeEntity topic, Set<String> approved, List<Candidate> candidates,
            Workspace w, Estimate estimate) {
        return new State(b.getId(), topic.getId(), b.getContentVersion(), BatchBook.correctionRevision(b),
                BatchBook.list(topic.getQuizTemplates()).stream().map(q -> new Quiz(q.getId(), q.getMiniTheory(),
                        new CourseElementSnapshot(AuditTarget.QUIZ, q.getId(), q, null), approved.contains(q.getId()))).toList(),
                candidates, w.drafts().stream().filter(d -> topic.getId().equals(d.knowledgeId())).toList(),
                w.feedback().stream().filter(f -> topic.getId().equals(f.knowledgeId())).toList(),
                w.jobs().stream().filter(j -> topic.getId().equals(j.knowledgeId())).toList(), estimate);
    }
    private static List<Candidate> candidates(KnowledgeEntity topic, Workspace w, BatchReview review, int version) {
        var candidates = new ArrayList<Candidate>();
        for (var q : BatchBook.list(topic.getQuizTemplates())) if (!blank(q.getMiniTheory())) {
            var latest = review.items().stream().filter(i -> q.getId().equals(i.step().getQuizId())
                    && BatchBook.list(i.step().getParts()).contains(ExercisePart.MINI_THEORY)).reduce((a, b) -> b);
            if (latest.isPresent() && latest.get().status() == BatchReviewDecision.Status.REJECTED) continue;
            candidates.add(new Candidate("quiz:" + q.getId(), q.getId(), q.getMiniTheory(), "CORRECTION",
                    latest.map(i -> i.status().name()).orElse("UNREVIEWED")));
        }
        for (Draft d : w.drafts()) if (topic.getId().equals(d.knowledgeId()) && d.contentVersion() == version) {
            candidates.add(new Candidate(d.id(), d.quizId(), d.text(), "DRAFT", "PENDING"));
        }
        return candidates;
    }
    private static void catalog(List<Candidate> result, CourseEntity composed, CourseEntity original, BatchReview review, String topicId) {
        var texts = new HashSet<String>(); result.forEach(c -> texts.add(c.text()));
        for (var course : List.of(composed, original)) {
            for (var m : BatchBook.list(course.getRoot().getMilestones())) for (var t : BatchBook.list(m.getTopics())) {
                for (var k : BatchBook.list(t.getKnowledges())) if (!k.getId().equals(topicId)) {
                    for (var q : BatchBook.list(k.getQuizTemplates())) if (!blank(q.getMiniTheory())) {
                        var latest = review.items().stream().filter(i -> q.getId().equals(i.step().getQuizId())
                                && BatchBook.list(i.step().getParts()).contains(ExercisePart.MINI_THEORY)).reduce((a, b) -> b);
                        boolean proposal = course == composed && latest.isPresent();
                        if (proposal && latest.get().status() == BatchReviewDecision.Status.REJECTED) continue;
                        if (texts.add(q.getMiniTheory())) result.add(new Candidate((proposal ? "proposal:" : "course:") + q.getId(),
                                q.getId(), q.getMiniTheory(), proposal ? "CORRECTION" : "COURSE",
                                proposal ? latest.get().status().name() : "UNREVIEWED", k.getId(), k.getLabel()));
                    }
                }
            }
        }
    }
    private static List<Candidate> relevant(List<Candidate> candidates, KnowledgeEntity topic, QuizTemplateEntity quiz, String feedback) {
        // Retrieval is only lexical; the agent/human must decide semantic equivalence. Never marks an explanation valid.
        Set<String> words = new HashSet<>(Arrays.asList((feedback + " " + quiz.getMiniTheory() + " " + topic.getLabel())
                .toLowerCase(Locale.ROOT).split("[^\\p{L}]+")));
        var unique = new LinkedHashMap<String, Candidate>();
        for (var c : candidates) if (!unique.containsKey(c.text()) || "APPROVED".equals(c.status())) unique.put(c.text(), c);
        return unique.values().stream().sorted(Comparator.comparingInt((Candidate c) -> {
            int score = c.knowledgeId() == null || topic.getId().equals(c.knowledgeId()) ? 1000 : 0;
            for (String word : c.text().toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) if (word.length() > 2 && words.contains(word)) score++;
            return score;
        }).reversed()).limit(100).toList();
    }
    private CorrectionBatch batch(String id) {
        return batches.load(id).orElseThrow(() -> new IllegalArgumentException("No existe la tanda " + id));
    }
    private static void writable(CorrectionBatch b, Request r) {
        if (b.getStatus() != BatchStatus.CLOSED) fail("Esta operación requiere una corrección cerrada y sin ejecución activa.");
        if (r.contentVersion() == null || r.contentVersion() != b.getContentVersion()) fail("El contenido cambió: volvé a leer.");
    }
    private static List<QuizTemplateEntity> selected(KnowledgeEntity topic, List<String> ids) {
        if (safe(ids).isEmpty() || new HashSet<>(ids).size() != ids.size()) fail("Elegí quizzes distintos dentro del ejercicio.");
        var result = new ArrayList<QuizTemplateEntity>();
        for (String id : ids) result.add(BatchBook.list(topic.getQuizTemplates()).stream().filter(q -> id.equals(q.getId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Quiz ajeno al ejercicio: " + id)));
        return result;
    }
    private static Draft draft(Request r, CorrectionBatch b, QuizTemplateEntity q, String text, String origin, String sourceId, String provenance) {
        return new Draft(r.operationId() + ":" + q.getId(), r.knowledgeId(), q.getId(),
                new CourseElementSnapshot(AuditTarget.QUIZ, q.getId(), BatchBook.copy(q), null), "EXISTING".equals(origin) ? text : text.trim(), origin,
                sourceId, r.feedback(), r.reviewer(), provenance, b.getContentVersion(), Instant.now());
    }
    private static Candidate source(String id, List<Candidate> options) {
        return blank(id) ? null : options.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La explicación elegida ya no está vigente."));
    }
    private static Job job(Workspace w, String id) { return w.jobs().stream().filter(j -> id.equals(j.id())).findFirst().orElse(null); }
    private static void checkIdentity(Request r, Workspace w, CorrectionBatch b) {
        Job previous = job(w, r.operationId());
        if (previous != null) {
            if (!previous.action().equals(r.action()) || !previous.knowledgeId().equals(r.knowledgeId())
                    || !sameIds(previous.quizIds(), r.quizIds()) || previous.cap() != (r.cap() == null ? -1 : r.cap())
                    || ("analyze".equals(r.action()) && (!sameIds(previous.draftIds(), r.draftIds())
                        || !Objects.equals(previous.fingerprint(), r.fingerprint())))) {
                fail("La identidad ya pertenece a otra operación; releé su resultado, no reutilices su presupuesto.");
            }
            return;
        }
        var drafts = w.drafts().stream().filter(d -> d.id().startsWith(r.operationId() + ":")).toList();
        if (!drafts.isEmpty()) {
            if (!"save".equals(r.action()) || !sameIds(drafts.stream().map(Draft::quizId).toList(), r.quizIds())
                    || drafts.stream().anyMatch(d -> !d.knowledgeId().equals(r.knowledgeId())
                        || !Objects.equals(d.text(), "EXISTING".equals(d.origin()) ? r.text() : r.text() == null ? null : r.text().trim()))) {
                fail("La identidad ya pertenece a otro borrador.");
            }
            return;
        }
        var feedback = w.feedback().stream().filter(f -> f.id().equals(r.operationId())).findFirst();
        if (feedback.isPresent()) {
            var f = feedback.get();
            if (!"feedback".equals(r.action()) || !f.knowledgeId().equals(r.knowledgeId())
                    || !sameIds(f.quizIds(), r.quizIds()) || !Objects.equals(f.text(), r.feedback() == null ? null : r.feedback().trim())) {
                fail("La identidad ya pertenece a otro feedback.");
            }
            return;
        }
        var change = BatchBook.list(b.getChanges()).stream().filter(c -> Objects.equals(c.getChangeId(), "mini-" + r.operationId())).findFirst();
        if (!"commit".equals(r.action()) || change.isEmpty()
                || !Objects.equals(change.get().getKnowledgeId(), r.knowledgeId())) fail("La identidad ya fue incorporada en otra operación.");
    }
    private static boolean sameIds(List<String> a, List<String> b) {
        return safe(a).size() == safe(b).size() && new HashSet<>(safe(a)).equals(new HashSet<>(safe(b)));
    }
    private static Workspace put(Workspace w, Job j) {
        var jobs = new ArrayList<>(w.jobs()); jobs.removeIf(old -> j.id().equals(old.id())); jobs.add(j);
        return new Workspace(w.drafts(), w.feedback(), jobs);
    }
    private static Job finish(Job j, String status, int spent, String error, AuditReport report) {
        return new Job(j.id(), j.action(), j.knowledgeId(), j.quizIds(), j.draftIds(), status, j.cap(),
                j.reserved(), spent, j.fingerprint(), error, report, j.createdAt());
    }
    private static int cap(Request r) { if (r.cap() == null || r.cap() < 0) fail("Confirmá un tope explícito no negativo."); return r.cap(); }
    private static List<String> ids(List<QuizTemplateEntity> q) { return q.stream().map(QuizTemplateEntity::getId).toList(); }
    private static <T> List<T> safe(List<T> l) { return l == null ? List.of() : l; }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
