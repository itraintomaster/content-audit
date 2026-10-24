package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;

class MiniTheoryCorrectionServiceTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore batches = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final TandaFixture.Artifacts artifacts = new TandaFixture.Artifacts();
    final CourseRepository repository = mock(CourseRepository.class);
    final CourseMapper mapper = mock(CourseMapper.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final MiniTheoryRewriter rewriter = mock(MiniTheoryRewriter.class);
    final Memory workspace = new Memory();
    MiniTheoryCorrectionService service;
    KnowledgeEntity topic;
    String quiz;
    CourseEntity measured;
    int factories;
    JudgeCoverageDiagnosis coverage;

    @BeforeEach void setup() {
        topic = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        quiz = topic.getQuizTemplates().get(0).getId();
        topic.getQuizTemplates().forEach(q -> q.setMiniTheory("Regla existente"));
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(1);
        when(batches.load("batch")).thenReturn(Optional.of(batch));
        doAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(1)).get()).when(batches).withAnalysisLock(anyString(), any());
        when(reviews.read("batch")).thenAnswer(i -> new BatchReview("batch", 1, batch.getContentVersion(), null,
                List.of(), List.of(), List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(mapper.map(any())).thenAnswer(i -> { measured = i.getArgument(0); return new AuditableCourse(List.of()); });
        coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName("mini-theory");
        var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
        coverage.setStanding(standing); coverage.setCost(new JudgeCostEstimate(1, 2, 0, false, 0, 1, 0));
        coverage.setNotEvaluated(1);
        var report = TandaFixture.report(course, List.of(), Map.of(), List.of());
        ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenReturn(report);
        service = new DefaultMiniTheoryCorrectionServiceFactory().create(batches, reviews, artifacts, repository,
                TandaFixture.COURSE_PATH, workspace, mapper, engine, () -> { factories++; return rewriter; });
    }
    Request request(String action, String op, List<String> scope, List<String> drafts, boolean approved, String text, String source, Integer cap, String fingerprint) {
        return new Request(action, op, "batch", topic.getId(), scope, drafts, 5, approved, text,
                "You va con are en presente simple", source, cap, fingerprint, "José");
    }
    Request req(String action) { return request(action, action, List.of(quiz), List.of(), false, "Nueva regla", null, null, null); }
    @Test void readingAndHumanDraftNeverCallModelsOrWriteCourseOrCheckpoint() {
        assertFalse(service.execute(req("read")).candidates().isEmpty());
        var state = service.execute(req("save"));
        assertEquals("Nueva regla", state.drafts().get(0).text());
        assertEquals("Regla existente", topic.getQuizTemplates().get(0).getMiniTheory());
        assertEquals(5, batch.getContentVersion());
        verifyNoInteractions(engine, rewriter); assertEquals(0, factories);
        verify(repository, never()).save(any(), any()); verify(batches, never()).save(any());
    }
    @Test void choosingAnExistingCopiesLiteralTextAndKeepsSourceWithoutGenerator() {
        var state = service.execute(request("save", "reuse", List.of(quiz), List.of(), false,
                "Regla existente", "course:" + quiz, null, null));
        assertEquals("EXISTING", state.drafts().get(0).origin());
        assertEquals("course:" + quiz, state.drafts().get(0).sourceId());
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("save", "altered", List.of(quiz), List.of(), false,
                "Otra", "course:" + quiz, null, null)));
        verifyNoInteractions(rewriter);
    }
    @Test void rejectsWrongScopeStaleContentAndUnboundedQueriesWithoutSpending() {
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("save", "foreign", List.of("other"), List.of(), false, "x", null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("rewrite")));
        batch.setContentVersion(6); assertThrows(IllegalArgumentException.class, () -> service.execute(req("save")));
        assertEquals(0, factories); assertTrue(workspace.value.drafts().isEmpty());
    }
    @Test void excludesApprovalsUnlessExplicitAndKeepsFeedbackHistory() {
        var step = TandaFixture.step(topic.getQuizTemplates().get(0), com.learney.contentaudit.auditdomain.correction.ExercisePart.MINI_THEORY);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(),
                List.of(new BatchReview.Item("old", step, step.getAfter(), step.getAfter(), "PANEL", null,
                        BatchReviewDecision.Status.APPROVED, "José", null)), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("save")));
        service.execute(request("feedback", "confirmed", List.of(quiz), List.of(), true, null, null, null, null));
        assertEquals(List.of(quiz), workspace.value.feedback().get(0).quizIds());
        assertEquals(0, factories);
    }
    @Test void uncertainRewriteChargesBeforeCallingAndDoesNotRepeatEvenAfterVersionChange() {
        when(rewriter.rewrite(any())).thenAnswer(i -> {
            assertEquals(1, workspace.value.jobs().get(0).reserved());
            throw new IllegalStateException("interrumpido");
        });
        var request = request("rewrite", "paid", List.of(quiz), List.of(), false, null, null, 1, null);
        var state = service.execute(request);
        assertEquals("FAILED", state.jobs().get(0).status()); assertEquals(1, state.jobs().get(0).reserved());
        batch.setContentVersion(6); service.execute(request);
        verify(rewriter, times(1)).rewrite(any()); assertEquals(1, factories);
    }
    @Test void aRewriteCannotInventAnExistingSourceOrAlterItsLiteralText() {
        when(rewriter.rewrite(any())).thenReturn(new RewriteResult("changed", "course:" + quiz, "fake"));
        var state = service.execute(request("rewrite", "paid", List.of(quiz), List.of(), false, null, null, 1, null));
        assertEquals("FAILED", state.jobs().get(0).status()); assertTrue(state.drafts().isEmpty());
    }
    @Test void explicitCommitTouchesOnlyMiniTheoryAndPreservesCorrectionAndHistoricalClosings() {
        var saved = service.execute(req("save")); var closing = new BatchClosing(); batch.setClosings(List.of(closing));
        doAnswer(i -> { assertEquals(5, i.getArgument(1, Integer.class)); return null; }).when(batches).saveIfVersion(any(), anyInt(), any());
        var request = request("commit", "incorporate", List.of(quiz), List.of(saved.drafts().get(0).id()), false, null, null, null, null);
        service.execute(request); service.execute(request);
        assertEquals(6, batch.getContentVersion()); assertEquals(1, batch.getCorrectionRevision());
        assertSame(closing, batch.getClosings().get(0)); assertEquals(1, batch.getChanges().size());
        assertEquals(List.of(com.learney.contentaudit.auditdomain.correction.ExercisePart.MINI_THEORY), batch.getChanges().get(0).getSteps().get(0).getParts());
        assertEquals("Regla existente", topic.getQuizTemplates().get(0).getMiniTheory());
        verify(batches, times(1)).saveIfVersion(any(), anyInt(), any()); verify(repository, never()).save(any(), any());
    }
    @Test void estimateIsZeroBudgetAndAnalysisUsesExactDraftWithAllSiblingContextAndOnlyMiniJudge() {
        var saved = service.execute(req("save"));
        var request = request("estimate", null, List.of(quiz), List.of(saved.drafts().get(0).id()), false, null, null, null, null);
        var estimate = service.execute(request).estimate();
        assertEquals(topic.getQuizTemplates().size(), estimate.topicQuizzes());
        assertEquals("Nueva regla", TandaFixture.exercise(measured, quiz).getMiniTheory());
        verify(engine).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("mini-theory"))
                && s.getPolicies().get("mini-theory").getMaxNewEvaluations() == 0));
        service.execute(request("analyze", "analysis", List.of(quiz), request.draftIds(), false, null, null, 2, estimate.fingerprint()));
        assertEquals("PARTIAL", workspace.value.jobs().get(0).status());
        assertEquals(2, workspace.value.jobs().get(0).reserved());
        assertEquals(5, batch.getContentVersion()); assertTrue(batch.getClosings().isEmpty());
        verifyNoInteractions(rewriter);
    }
    @Test void changedJudgeOrContextInvalidatesPreflightWithoutPaidAudit() {
        String fingerprint = service.execute(req("estimate")).estimate().fingerprint();
        coverage.getStanding().setJudgeVersion("v2");
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("analyze", "analysis", List.of(quiz), List.of(), false, null, null, 2, fingerprint)));
        verify(engine, never()).runAudit(any(), argThat(s -> s.getPolicies().get("mini-theory").getMaxNewEvaluations() > 0));
    }
    @Test void operationIdentityCannotBeReusedForAnotherActionOrScope() {
        service.execute(req("save"));
        service.execute(req("save"));
        assertEquals(1, workspace.value.drafts().size());
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("feedback", "save", List.of(quiz), List.of(), false, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(request("save", "save", List.of(quiz), List.of(), false, "Different text", null, null, null)));
        verifyNoInteractions(engine, rewriter);
    }
    static final class Memory implements MiniTheoryWorkspaceStore {
        Workspace value = Workspace.empty();
        public Workspace load(String id) { return value; }
        public void save(String id, Workspace value) { this.value = value; }
        public String fingerprint(CourseEntity course) { return Integer.toHexString(course.hashCode()); }
        public String fingerprintIdentity(Object identity) { return Integer.toHexString(identity.hashCode()); }
        public <T> T exclusive(String id, Supplier<T> action) { return action.get(); }
    }
}
