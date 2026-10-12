package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.SelectiveCorrection.*;
import com.learney.contentaudit.revisiondomain.panel.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SelectiveCorrectionServiceTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch", "unrelated");
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final CourseMapper mapper = mock(CourseMapper.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final AuditReportStore audits = mock(AuditReportStore.class);
    final RefinementPlanStore plans = mock(RefinementPlanStore.class);
    final RefinerEngine refiner = mock(RefinerEngine.class);
    final DefaultPanelStage panel = mock(DefaultPanelStage.class);
    final PanelRunnerFactory factory = mock(PanelRunnerFactory.class);
    final QueryAllowance allowance = mock(QueryAllowance.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final Map<String, RefinementPlan> savedPlans = new HashMap<>();
    final JudgeCoverageDiagnosis quality = coverage("multiple-choice-quality");
    final JudgeCoverageDiagnosis unique = coverage("multiple-choice-validity");
    final BatchClosing closing = new BatchClosing();
    DefaultSelectiveCorrectionService service;
    AuditReport report;
    String topic, quiz;
    int auditNumber;

    static JudgeCoverageDiagnosis coverage(String name) {
        var c = new JudgeCoverageDiagnosis(); c.setJudgeName(name); c.setReached(2); c.setEvaluated(1); c.setNotEvaluated(1);
        var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
        standing.setRuntimeIdentity(com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium")); c.setStanding(standing);
        c.setCost(new JudgeCostEstimate(1, 2, 0, false, 0, 1, 0)); return c;
    }
    @BeforeEach void setup() {
        var knowledge = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        topic = knowledge.getId(); quiz = knowledge.getQuizTemplates().get(0).getId();
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(1); batch.setQueriesSpent(10);
        batch.setClosings(List.of(closing));
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(reviews.read("batch")).thenAnswer(i -> new BatchReview("batch", 1, batch.getContentVersion(), null,
                List.of(), List.of(), List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), same(course))).thenReturn(course);
        when(mapper.map(any())).thenReturn(new AuditableCourse(List.of()));
        report = TandaFixture.report(course, List.of(), Map.of(), List.of());
        ((DefaultCourseDiagnoses)report.getRoot().getDiagnoses()).setJudgeCoverages(List.of(quality, unique));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            var s = i.getArgument(1, AnalyzerRunSelection.class);
            var ec = new EvaluationCoverage(); ec.setEvaluatedInRun(s.getPolicies().get(s.getAnalyzers().get(0)).getMaxNewEvaluations() > 0 ? 1 : 0);
            quality.setEvaluators(List.of(new EvaluatorCoverage("quality", ec, "v1"))); return report;
        });
        when(audits.save(any())).thenAnswer(i -> "audit-" + ++auditNumber);
        when(audits.load(anyString())).thenReturn(Optional.of(report));
        savedPlans.put(batch.getPlanId(), TandaFixture.plan(TandaFixture.lengthTask("unrelated", quiz)));
        when(plans.load(anyString())).thenAnswer(i -> Optional.ofNullable(savedPlans.get(i.getArgument(0))));
        doAnswer(i -> { var p = i.getArgument(0, RefinementPlan.class); savedPlans.put(p.getId(), p); return null; }).when(plans).save(any());
        Finding f = new Finding(); f.setAnalyzer("multiple-choice-quality"); f.setRule("weak-distractors");
        f.setNode(new FindingNodeRef(AuditTarget.QUIZ, quiz, quiz));
        var task = TandaFixture.task("quality", f, topic, CefrLevel.A1);
        when(refiner.plan(any(), anyString())).thenReturn(TandaFixture.plan(task, TandaFixture.lengthTask("foreign", quiz)));
        when(factory.allowance(anyInt(), anyInt())).thenReturn(allowance);
        when(allowance.tripped()).thenReturn(Optional.empty());
        doAnswer(i -> {
            var b = i.getArgument(0, CorrectionBatch.class);
            assertEquals(Set.of("selective-analysis-quality"), i.getArgument(4));
            assertTrue(i.getArgument(5, List.class).contains("multiple-choice-quality"));
            BatchBook.setOutcome(b, "selective-analysis-quality", BatchTaskOutcomeKind.RESOLVED_BY_PANEL, null, 1, null);
            b.setQueriesSpent(b.getQueriesSpent() + 1); when(allowance.spent()).thenReturn(1); return b;
        }).when(panel).runSelected(any(), any(), any(), any(), any(), any());
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAuditReportStore(audits); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setPanelRunnerFactory(factory);
        service = new DefaultSelectiveCorrectionService(config, ledger, panel, reviews, c -> "context");
    }
    Request req(String action, String id, List<String> tasks, Integer cap, boolean approved) {
        return new Request(action, "batch", "multiple-choice-quality", id, action.equals("correct") ? "analysis" : null,
                5, "context", "v1", cap, tasks, approved, "human");
    }
    State analyze() { return service.execute(req("analyze", "analysis", List.of(), 2, false)); }
    @Test void explicitReanalysisForcesNewVerdictsAndFinishedStateRetainsExamStanding() {
        var state = service.execute(req("reanalyze", "new-version", List.of(), 2, false));
        assertTrue(state.allowed());
        verify(engine, times(2)).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("multiple-choice-quality"))
                && s.getPolicies().get("multiple-choice-quality").isReevaluate()));
        assertEquals("reanalyze", state.runs().get(0).action());
    }
    @Test void aMultiEvaluatorJudgeCannotSpendMoreThanTheSingleReservedAllowance() {
        quality.setJudgeName("translation-sense");
        var current = mock(CurrentAnalysisService.class); when(current.supportedAnalyzers()).thenReturn(Set.of("translation-sense"));
        // The generic route shares the same native config; correction remains restricted to quality.
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAuditReportStore(audits); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setCurrentAnalysisService(current);
        when(engine.runAudit(any(), any())).thenAnswer(i -> {
            var selection = i.getArgument(1, AnalyzerRunSelection.class);
            var c = new EvaluationCoverage(); c.setEvaluatedInRun(selection.getPolicies().get("translation-sense").getMaxNewEvaluations() > 0 ? 1 : 0);
            quality.setEvaluators(List.of(new EvaluatorCoverage("english", c, "v1"), new EvaluatorCoverage("translation", c, "v1")));
            return report;
        });
        var generic = new DefaultSelectiveCorrectionService(config, ledger, panel, reviews, c -> "context");
        var request = new Request("reanalyze", "batch", "translation-sense", "two-evaluators", null, 5, "context", "v1", 3, List.of(), false, "human");
        var result = generic.execute(request);
        assertEquals(2, result.runs().get(0).spent()); assertEquals(3, result.runs().get(0).reserved());
        verify(engine).runAudit(any(), argThat(s -> s.getPolicies().get("translation-sense").getMaxNewEvaluations() == 1));
    }
    @Test void estimateUsesOnlySelectedJudgeAtZeroBudgetAndDoesNotWriteOrStartPanel() {
        assertTrue(service.execute(req("estimate", null, List.of(), null, false)).allowed());
        verify(engine).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("multiple-choice-quality"))
                && s.getPolicies().get("multiple-choice-quality").getMaxNewEvaluations() == 0));
        verify(store, never()).save(any()); verify(store, never()).saveIfVersion(any(), anyInt(), any());
        verifyNoInteractions(audits, plans, panel, factory); verify(repository, never()).save(any(), any());
    }
    @Test void analysisAddsOnlyJudgeTasksAndPreservesRevisionClosingsAndCourse() {
        var state = analyze(); var run = state.runs().get(0);
        assertEquals(List.of("selective-analysis-quality"), run.taskIds()); assertEquals("PARTIAL", run.status());
        assertEquals(2, run.reserved()); assertEquals(1, run.spent()); assertEquals(11, batch.getQueriesSpent());
        assertEquals(1, batch.getCorrectionRevision()); assertEquals(5, batch.getContentVersion());
        assertEquals(List.of("unrelated"), batch.getTaskIds()); assertSame(closing, batch.getClosings().get(0));
        assertEquals("weak-distractors", state.tasks().get(0).rule());
        verify(store).saveIfVersion(batch, 5, BatchStatus.CLOSED); verifyNoInteractions(panel);
        verify(repository, never()).save(any(), any());
    }
    @Test void explicitCorrectionDoesNotTakeOtherPendingTasksOrCreateAnotherCorrection() {
        analyze(); var sourceTask = savedPlans.get("selective-analysis").getTasks().get(0);
        var state = service.execute(req("correct", "correction", List.of("selective-analysis-quality"), 7, false));
        assertEquals(6, batch.getContentVersion()); assertEquals(1, batch.getCorrectionRevision());
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "unrelated"));
        assertEquals(RefinementTaskStatus.PENDING, sourceTask.getStatus());
        assertTrue(state.tasks().get(0).attempted()); assertSame(closing, batch.getClosings().get(0));
        assertEquals(6, state.runs().get(1).resultContentVersion()); assertEquals(12, batch.getQueriesSpent());
        verify(repository, never()).save(any(), any());
    }
    @Test void duplicateIdentitiesNeverSpendAgainEvenAfterContentVersionChanges() {
        var r = req("analyze", "analysis", List.of(), 2, false); service.execute(r);
        batch.setContentVersion(6); service.execute(r);
        verify(audits, times(1)).save(any());
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("analyze", "analysis", List.of(), 3, false)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("analyze", "analysis", List.of(), 2, true)));
    }
    @Test void rejectsMissingBudgetForeignTasksStaleContentChangedJudgeAndExamBlockBeforePaidWork() {
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("analyze", "a", List.of(), null, false)));
        quality.getStanding().setConsultsOnCourse(false);
        assertThrows(IllegalArgumentException.class, () -> analyze());
        quality.getStanding().setConsultsOnCourse(true); quality.getStanding().setJudgeVersion("v2");
        assertThrows(IllegalArgumentException.class, () -> analyze()); quality.getStanding().setJudgeVersion("v1");
        batch.setContentVersion(6); assertThrows(IllegalArgumentException.class, () -> analyze()); batch.setContentVersion(5);
        verify(audits, never()).save(any()); analyze();
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("correct", "c", List.of("unrelated"), 9, false)));
        verifyNoInteractions(panel);
    }
    @Test void approvedQuizNeedsSeparateExplicitConfirmation() {
        analyze(); var step = TandaFixture.step(TandaFixture.exercise(course, quiz), com.learney.contentaudit.auditdomain.correction.ExercisePart.MINI_THEORY);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(),
                List.of(new BatchReview.Item("old", step, step.getAfter(), step.getAfter(), "PANEL", null,
                        BatchReviewDecision.Status.APPROVED, "human", null)), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.execute(req("correct", "c", List.of("selective-analysis-quality"), 8, false)));
        service.execute(req("correct", "c", List.of("selective-analysis-quality"), 8, true));
        verify(panel, times(1)).runSelected(any(), any(), any(), any(), any(), any());
    }
    @Test void failedRunKeepsReservationKnownSpendAndBlocksNewRunsWithoutAutomaticRecovery() {
        analyze(); when(allowance.spent()).thenReturn(2);
        doThrow(new IllegalStateException("stopped")).when(panel).runSelected(any(), any(), any(), any(), any(), any());
        var r = req("correct", "c", List.of("selective-analysis-quality"), 8, false);
        var state = service.execute(r); service.execute(r);
        assertEquals("NEEDS_REVIEW", state.runs().get(1).status()); assertEquals(8, state.runs().get(1).reserved());
        assertEquals(13, batch.getQueriesSpent());
        assertFalse(service.execute(req("estimate", null, List.of(), null, false)).allowed());
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("analyze", "batch", "multiple-choice-quality", "new",
                null, 6, "context", "v1", 4, List.of(), false, "human")));
        verify(panel, times(1)).runSelected(any(), any(), any(), any(), any(), any());
    }
}
