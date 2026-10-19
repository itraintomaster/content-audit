package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;

/** Attaching exploratory opinions must never turn them into approval or resolved course findings. */
class PreliminaryOpinionImportTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final CurrentAnalysisService analysis = mock(CurrentAnalysisService.class);
    final PreliminaryOpinionSource source = mock(PreliminaryOpinionSource.class);
    final List<RevisionProposal> proposals = new ArrayList<>();
    List<PreliminaryOpinion> opinions = new ArrayList<>();
    DefaultContextOpinionService service;
    String quiz, second;
    String courseStamp = "content-v8";
    static final String SOURCE = "saved-exam-source";
    static final Instant MEASURED = Instant.parse("2026-10-14T10:17:41Z");
    static final EvaluationRuntimeIdentity RUNTIME = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");

    @BeforeEach void setup() {
        var quizzes = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates();
        quiz = quizzes.get(0).getId(); second = quizzes.get(1).getId();
        proposals.add(proposal(quiz)); proposals.add(proposal(second));
        for (String analyzer : List.of("naturalness", "a1-didactic", "pragmatism"))
            for (String id : List.of(quiz, second)) opinions.add(opinion(analyzer, id));
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(8); batch.setQueriesSpent(10);
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(store.withAnalysisLock(eq("batch"), any())).thenAnswer(i -> ((Supplier<?>) i.getArgument(1)).get());
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), same(course))).thenReturn(course);
        when(reviews.read("batch")).thenAnswer(i -> new BatchReview("batch", 1, batch.getContentVersion(), null,
                List.of(), proposals, List.of(), List.of(), List.of()));
        when(source.load(SOURCE)).thenAnswer(i -> opinions);
        var catalog = mock(AnalyzerCatalog.class);
        when(catalog.list()).thenReturn(List.of());
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setAnalyzerCatalog(catalog); config.setAuditEngine(engine);
        config.setCurrentAnalysisService(analysis);
        service = new DefaultContextOpinionService(config, ledger, reviews, c -> courseStamp, source);
    }

    RevisionProposal proposal(String id) {
        var p = new RevisionProposal(); p.setProposalId("p-" + id); p.setNodeTarget(AuditTarget.QUIZ); p.setNodeId(id);
        var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, id, TandaFixture.exercise(course, id), null);
        p.setElementBefore(snapshot); p.setElementAfter(snapshot); return p;
    }
    PreliminaryOpinion opinion(String analyzer, String id) {
        String evaluator = switch (analyzer) {
            case "naturalness" -> "native-usage-judge";
            case "a1-didactic" -> "a1-didactic-judge";
            default -> "pragmatism-judge";
        };
        // A separate deserialization of the photo has value equality, not object identity.
        var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, id, TandaFixture.exercise(TandaFixture.course(), id), null);
        return new PreliminaryOpinion(id, analyzer, "exam:" + evaluator, "version-" + analyzer, RUNTIME, "EXAM",
                "input-" + id, "{\n  \"rationale\": \"Original model opinion\"\n}", MEASURED,
                SOURCE + "/exam_" + evaluator + ".jsonl", "a".repeat(64), snapshot);
    }
    Request estimate() { return new Request("estimate-preliminary", "batch", null, false, null, null, null, 0, null, null, SOURCE); }
    Request importing(State state) { return new Request("import-preliminary", "batch", state.selection(), false,
            "attach-exam", 8, state.fingerprint(), 0, "human", null, SOURCE); }
    PreliminaryOpinion withNamespace(PreliminaryOpinion p, String namespace) {
        return new PreliminaryOpinion(p.quizId(), p.analyzer(), p.evaluatorId(), p.judgeVersion(), p.runtime(), namespace,
                p.contentFingerprint(), p.payload(), p.recordedAt(), p.sourceRef(), p.sourceRecordHash(), p.snapshot());
    }

    @Test void estimatesAllSixSourcesWithoutRunningJudgesOrWritingAnything() {
        State state = service.execute(estimate());
        assertTrue(state.allowed(), "attaching evidence is allowed while its judges remain unapproved");
        assertEquals(3, state.selection().size());
        assertTrue(state.selection().values().stream().allMatch(ids -> ids.size() == 2));
        assertEquals(0, state.pendingQueries()); assertEquals(0, state.reusedQueries());
        assertTrue(state.judges().stream().noneMatch(Judge::allowed));
        assertTrue(state.judges().stream().allMatch(j -> j.reason().contains("sin examen aprobado")));
        verifyNoInteractions(engine, analysis);
        verify(store, never()).save(any()); verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }

    @Test void importsExactPayloadAndProvenanceWithoutApprovingResolvingChargingOrIntegrating() {
        var changes = List.copyOf(batch.getChanges()); var closings = List.copyOf(batch.getClosings());
        var before = List.copyOf(proposals);
        var state = service.execute(importing(service.execute(estimate())));
        Run run = state.runs().get(0);
        assertEquals("PRELIMINARY", run.status()); assertTrue(run.results().isEmpty());
        assertEquals(0, run.spent()); assertEquals(0, run.reserved()); assertEquals(6, run.preliminaryOpinions().size());
        assertEquals(new HashSet<>(opinions), new HashSet<>(run.preliminaryOpinions()));
        assertEquals(2, run.proposals().size());
        assertTrue(run.judges().stream().noneMatch(Judge::allowed));
        assertTrue(run.detail().contains("juez sin examen aprobado"));
        assertEquals(10, batch.getQueriesSpent()); assertEquals(8, batch.getContentVersion());
        assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertEquals(changes, batch.getChanges()); assertEquals(closings, batch.getClosings()); assertEquals(before, proposals);
        verify(store).saveIfVersion(batch, 8, BatchStatus.CLOSED);
        verifyNoInteractions(engine, analysis); verify(repository, never()).save(any(), any()); verify(reviews, never()).decide(any());
    }

    @Test void repeatedIdentityReturnsItsImmutableEvidenceAndConflictingRequestIsRejected() {
        var request = importing(service.execute(estimate())); service.execute(request);
        int loads = mockingDetails(source).getInvocations().size();
        batch.setContentVersion(9);
        Run repeated = service.execute(request).runs().get(0);
        assertEquals(6, repeated.preliminaryOpinions().size()); assertEquals(1, batch.getOpinionRuns().size());
        assertEquals(loads, mockingDetails(source).getInvocations().size());
        verify(store, times(1)).saveIfVersion(any(), anyInt(), any());
        var different = new Request(request.action(), request.batchId(), request.selection(), false, request.operationId(),
                8, request.fingerprint(), 0, "another human", null, SOURCE);
        assertThrows(IllegalArgumentException.class, () -> service.execute(different));
    }

    @Test void courseOrSourceChangesAfterEstimateRejectBeforeSaving() {
        Request request = importing(service.execute(estimate()));
        courseStamp = "changed";
        assertThrows(IllegalArgumentException.class, () -> service.execute(request));
        courseStamp = "content-v8";
        var p = opinions.get(0);
        opinions.set(0, new PreliminaryOpinion(p.quizId(), p.analyzer(), p.evaluatorId(), p.judgeVersion(), p.runtime(),
                p.namespace(), p.contentFingerprint(), "changed original output", p.recordedAt(), p.sourceRef(), "b".repeat(64), p.snapshot()));
        assertThrows(IllegalArgumentException.class, () -> service.execute(request));
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }

    @Test void mismatchedMeasuredQuizAndWithdrawnProposalCannotBeAttached() {
        opinions.get(0).snapshot().getQuiz().setInstructions("Different visible instructions");
        assertThrows(IllegalArgumentException.class, () -> service.execute(estimate()));
        opinions = new ArrayList<>(List.of(opinion("naturalness", quiz)));
        proposals.clear();
        assertThrows(IllegalArgumentException.class, () -> service.execute(estimate()));
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }

    @Test void rejectsCourseNamespaceDuplicateOpinionsAndMissingOriginalProvenance() {
        var p = opinions.get(0);
        opinions.set(0, withNamespace(p, "COURSE"));
        assertThrows(IllegalArgumentException.class, () -> service.execute(estimate()));
        opinions.set(0, p); opinions.add(p);
        assertThrows(IllegalArgumentException.class, () -> service.execute(estimate()));
        opinions = new ArrayList<>(List.of(new PreliminaryOpinion(p.quizId(), p.analyzer(), p.evaluatorId(), p.judgeVersion(),
                EvaluationRuntimeIdentity.legacyUnknown(), p.namespace(), p.contentFingerprint(), p.payload(), p.recordedAt(),
                p.sourceRef(), p.sourceRecordHash(), p.snapshot())));
        assertThrows(IllegalArgumentException.class, () -> service.execute(estimate()));
        verifyNoInteractions(engine, analysis);
    }

    @Test void requiresVersionFingerprintReviewerAndZeroNewQueries() {
        State estimate = service.execute(estimate()); Request good = importing(estimate);
        for (Request bad : List.of(
                new Request(good.action(), "batch", good.selection(), false, "attach-exam", null, good.fingerprint(), 0, "human", null, SOURCE),
                new Request(good.action(), "batch", good.selection(), false, "attach-exam", 8, null, 0, "human", null, SOURCE),
                new Request(good.action(), "batch", good.selection(), false, "attach-exam", 8, good.fingerprint(), 0, null, null, SOURCE),
                new Request(good.action(), "batch", good.selection(), false, "attach-exam", 8, good.fingerprint(), 1, "human", null, SOURCE)))
            assertThrows(IllegalArgumentException.class, () -> service.execute(bad));
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }
}
