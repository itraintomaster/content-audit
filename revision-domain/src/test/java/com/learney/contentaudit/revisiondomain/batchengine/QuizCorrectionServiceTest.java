package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.refinerdomain.errortask.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.QuizCorrection.*;
import com.learney.contentaudit.revisiondomain.panel.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class QuizCorrectionServiceTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch", "old-task");
    final BatchServiceConfig config = new BatchServiceConfig();
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final DefaultPanelStage panels = mock(DefaultPanelStage.class);
    final CurrentAnalysisService current = mock(CurrentAnalysisService.class);
    final RefinementPlanStore plans = mock(RefinementPlanStore.class);
    final RefinerEngine refiner = mock(RefinerEngine.class);
    final AuditReportStore audits = mock(AuditReportStore.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final ErrorTaskBriefer briefer = mock(ErrorTaskBriefer.class);
    final PanelRunnerFactory runner = mock(PanelRunnerFactory.class);
    final QueryAllowance allowance = mock(QueryAllowance.class);
    final Map<String, RefinementPlan> savedPlans = new HashMap<>();
    final List<RefinementTask> sourceTasks = new ArrayList<>();
    final List<AnalysisSelection> selections = new ArrayList<>();
    String quiz, knowledge, sibling;
    Finding instruction, translation;
    AuditReport report;
    DefaultQuizCorrectionService service;

    @BeforeEach void setup() {
        var topic = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0);
        quiz = topic.getQuizTemplates().get(0).getId(); sibling = topic.getQuizTemplates().get(1).getId(); knowledge = topic.getId();
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(1); batch.setQueriesSpent(10);
        instruction = finding("quiz-instruction", "bad-instruction", quiz);
        translation = finding("translation-sense", "wrong-sense", quiz);
        sourceTasks.add(TandaFixture.task("instruction", instruction, knowledge, CefrLevel.A1));
        sourceTasks.add(TandaFixture.task("translation", translation, knowledge, CefrLevel.A1));
        report = TandaFixture.report(course, List.of(instruction, translation), Map.of(), List.of());
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(store.analysisSelections("batch")).thenAnswer(i -> List.copyOf(selections));
        doAnswer(i -> ((java.util.function.Supplier<?>)i.getArgument(1)).get()).when(store).withAnalysisLock(anyString(), any());
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(), List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), same(course))).thenReturn(course);
        when(current.report(any(), eq("correction"), any())).thenAnswer(i -> report);
        when(refiner.plan(any(), anyString())).thenAnswer(i -> TandaFixture.plan(sourceTasks.toArray(RefinementTask[]::new)));
        savedPlans.put(batch.getPlanId(), TandaFixture.plan(TandaFixture.lengthTask("old-task", quiz)));
        when(plans.load(anyString())).thenAnswer(i -> Optional.ofNullable(savedPlans.get(i.getArgument(0))));
        doAnswer(i -> { var plan = i.getArgument(0, RefinementPlan.class); savedPlans.put(plan.getId(), plan); return null; }).when(plans).save(any());
        when(briefer.brief(any(), any())).thenAnswer(i -> {
            var task = i.getArgument(1, RefinementTask.class);
            return Optional.of(new ErrorTaskBrief(task, task.getOrigin().getFinding(), task.getOrigin().getCoveredFindings(),
                    List.of(ExercisePart.STATEMENT, ExercisePart.TRANSLATION), true, null, List.of()));
        });
        when(audits.save(any())).thenReturn("new-source");
        when(runner.allowance(anyInt(), anyInt())).thenReturn(allowance);
        when(allowance.tripped()).thenReturn(Optional.empty());
        when(allowance.spent()).thenReturn(2);
        doAnswer(i -> {
            CorrectionBatch running = i.getArgument(0);
            Set<String> ids = i.getArgument(4);
            for (String id : ids) BatchBook.setOutcome(running, id, BatchTaskOutcomeKind.RESOLVED_BY_PANEL, null, 1, null);
            running.setQueriesSpent(running.getQueriesSpent() + 2);
            return running;
        }).when(panels).runSelected(any(), any(), any(), any(), any(), any());
        config.setBatchStore(store); config.setCurrentAnalysisService(current); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setAuditReportStore(audits); config.setBriefer(briefer); config.setPanelRunnerFactory(runner);
        config.setElementLocator(TandaFixture.locator());
        service = new DefaultQuizCorrectionService(config, ledger, panels, reviews, c -> "course-fingerprint", "codex-cli/model/medium", config.getElementLocator());
    }

    static Finding finding(String analyzer, String rule, String id) {
        return new Finding(analyzer, rule, new FindingNodeRef(AuditTarget.QUIZ, id, id), FindingSeverity.HIGH,
                new FindingEvidence(List.of(new EvidencePart("frase", "The current exercise")), "A problem", List.of()),
                FindingResolution.PANEL, null, new FindingIdentity(analyzer + ":" + id + ":" + rule, rule));
    }
    Request estimate(List<String> ids, boolean approved) { return new Request("estimate", "batch", null, null, null, null, ids, approved, null); }
    Request correct(State state, List<String> ids, boolean approved) { return new Request("correct", "batch", "operation", 5, state.fingerprint(), 8, ids, approved, "Jose"); }

    @Test void comparisonReadsTheExactOperationWithoutStartingAnotherCorrection() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        when(audits.load("new-source")).thenReturn(Optional.of(report));
        clearInvocations(store, audits, plans, panels, runner, current);
        var compared = service.execute(new Request("compare", "batch", "operation", null, null, null, null, false, null));
        assertEquals("operation", compared.runId());
        assertNotNull(compared.comparison());
        assertEquals(5, compared.comparison().sourceContentVersion());
        assertEquals(6, compared.comparison().resultContentVersion());
        assertTrue(compared.comparison().matchesResultVersion());
        verify(audits).load("new-source");
        verifyNoInteractions(panels, runner);
        verify(plans, never()).save(any());
        verify(store, never()).save(any());
        verify(audits, never()).save(any());
        verify(current, never()).seed(any(), any());
    }

    @Test void comparisonDoesNotAttributeLaterContentToTheSelectedOperation() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        when(audits.load("new-source")).thenReturn(Optional.of(report));
        batch.setContentVersion(7);
        clearInvocations(current, panels, runner);
        var compared = service.execute(new Request("compare", "batch", "operation", null, null, null, null, false, null)).comparison();
        assertFalse(compared.matchesResultVersion()); assertEquals(7, compared.currentContentVersion());
        assertTrue(compared.warnings().stream().anyMatch(w -> w.contains("cambios posteriores")));
        assertTrue(compared.quizzes().get(0).rows().stream().allMatch(r -> r.status().equals("PENDING")));
        verify(current, never()).report(any(), any(), any()); verifyNoInteractions(panels, runner);
    }

    @Test void comparisonRejectsChangedContentEvenWhenVersionWasNotBumped() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        when(audits.load("new-source")).thenReturn(Optional.of(report));
        var changed = new DefaultQuizCorrectionService(config, ledger, panels, reviews, c -> "different-content", "runtime", config.getElementLocator());
        clearInvocations(current, panels, runner);
        assertFalse(changed.execute(new Request("compare", "batch", "operation", null, null, null, null, false, null))
                .comparison().matchesResultVersion());
        verify(current, never()).report(any(), any(), any()); verifyNoInteractions(panels, runner);
    }

    @Test void comparisonRequiresTheExactSavedRecorrectionIdentity() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        clearInvocations(current, audits, panels, runner);
        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new Request("compare", "batch", "other-operation", null, null, null, null, false, null)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(
                new Request("compare", "batch", null, null, null, null, null, false, null)));
        verifyNoInteractions(current, audits, panels, runner);
    }

    @Test void comparisonOfARunningPassKeepsAfterUnknownWithoutStartingWork() {
        var completed = service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false)).runs().get(0);
        var running = new SelectiveCorrection.Run(completed.id(), completed.action(), completed.analyzer(), completed.sourceRunId(),
                completed.correctionRevision(), completed.contentVersion(), completed.fingerprint(), completed.judgeVersion(),
                completed.runtime(), completed.auditId(), completed.planId(), completed.taskIds(), completed.quizIds(), completed.queryCap(),
                completed.reserved(), 0, completed.reached(), completed.evaluated(), completed.missing(), "RUNNING", completed.reviewer(),
                completed.createdAt(), null, null, false, completed.resultContentVersion(), completed.resultFingerprint(), completed.correctorRuntime());
        batch.setSelectiveRuns(List.of(running));
        when(audits.load("new-source")).thenReturn(Optional.of(report));
        clearInvocations(current, panels, runner);
        var comparison = service.execute(new Request("compare", "batch", "operation", null, null, null, null, false, null)).comparison();
        assertFalse(comparison.matchesResultVersion()); assertTrue(comparison.warnings().stream().anyMatch(w -> w.contains("todavía no terminó")));
        assertTrue(comparison.quizzes().get(0).rows().stream().allMatch(r -> r.afterCoverage().equals("UNKNOWN")));
        verify(current, never()).report(any(), any(), any()); verifyNoInteractions(panels, runner);
    }

    @Test void correctionUsesTheBatchFourQueryConcurrencyAndKeepsTheRequestedCap() {
        assertConfiguredConcurrency(new BatchSettings(4, 4, 3), 4);
    }

    @Test void correctionHonorsAnExplicitSingleQueryLimit() {
        assertConfiguredConcurrency(new BatchSettings(4, 1, 3), 1);
    }

    @Test void correctionLetsTheFactoryApplyDefaultConcurrencyWhenSettingsAreMissing() {
        assertConfiguredConcurrency(null, 0);
    }

    @Test void correctionLetsTheFactoryApplyDefaultConcurrencyForUnspecifiedSettings() {
        assertConfiguredConcurrency(new BatchSettings(0, 0, 3), 0);
    }

    private void assertConfiguredConcurrency(BatchSettings settings, int queriesAtOnce) {
        batch.setSettings(settings);
        State result = service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        assertEquals("DONE", result.status());
        verify(runner).allowance(8, queriesAtOnce);
        assertSame(settings, batch.getSettings(), "a selective correction does not rewrite batch settings");
        assertEquals(8, result.runs().get(0).queryCap());
        assertEquals(2, result.spent());
    }

    @Test void estimateUsesAllCurrentJudgesWithoutModelsOrWritesAndDoesNotInventTotalCost() {
        State state = service.execute(estimate(List.of(), false));
        assertTrue(state.allowed()); assertEquals(0, state.selectedQuizzes());
        assertEquals(1, state.quizzes().size()); assertEquals(2, state.quizzes().get(0).findings().size());
        assertTrue(state.estimateNote().contains("no una estimación del total"));
        verifyNoInteractions(panels, runner, audits);
        verify(plans, never()).save(any()); verify(store, never()).save(any()); verify(current, never()).seed(any(), any());
    }

    @Test void correctionAppendsFreshTasksWithEveryJudgeToSameRevisionAndRetainsOldPlan() {
        State estimate = service.execute(estimate(List.of(quiz), false));
        State result = service.execute(correct(estimate, List.of(quiz), false));
        assertEquals("DONE", result.status()); assertEquals(6, result.contentVersion()); assertEquals(1, result.correctionRevision());
        assertEquals(2, result.spent()); assertEquals(12, batch.getQueriesSpent());
        var plan = savedPlans.get(batch.getPlanId()); assertEquals(3, plan.getTasks().size());
        assertEquals("old-task", plan.getTasks().get(0).getId());
        assertEquals(Set.of("quiz-instruction", "translation-sense"), plan.getTasks().subList(1, 3).stream()
                .map(t -> t.getOrigin().getFinding().getAnalyzer()).collect(java.util.stream.Collectors.toSet()));
        verify(panels).runSelected(same(batch), same(report), same(course), same(allowance),
                eq(Set.of("recorrect-operation-0", "recorrect-operation-1")), argThat(a -> a.containsAll(List.of("quiz-instruction", "translation-sense"))));
        assertEquals("new-source", result.runs().get(0).auditId());
        verify(current).seed(batch, course);
    }

    @Test void reservationFreezesComposedCurrentVersionAndOriginalBeforeAnyPanelCall() throws Exception {
        var originalQuiz = TandaFixture.exercise(course, quiz);
        String originalTranslation = originalQuiz.getTranslation();
        var after = BatchBook.copy(originalQuiz); after.setTranslation("Earlier correction, before this operation");
        var realLedger = new DefaultBatchChangeLedger(new TandaFixture.Artifacts(), TandaFixture.locator());
        realLedger.record(batch, new BatchChange("earlier", BatchChangeOrigin.PANEL, "earlier", knowledge,
                List.of("old-task"), List.of(new BatchChangeStep(quiz, List.of(ExercisePart.TRANSLATION),
                        new CourseElementSnapshot(AuditTarget.QUIZ, quiz, after, null))),
                null, false, Instant.now(), null), course);
        when(ledger.composedCourse(any(), same(course))).thenAnswer(i -> realLedger.composedCourse(i.getArgument(0), course));
        doAnswer(i -> {
            var reserved = i.getArgument(0, CorrectionBatch.class);
            assertEquals(1, reserved.getComparisonBaselines().size());
            var baseline = reserved.getComparisonBaselines().get(0);
            assertEquals("operation", baseline.operationId()); assertEquals(5, baseline.sourceContentVersion());
            assertEquals("Earlier correction, before this operation", baseline.before().getQuiz().getTranslation());
            assertEquals(originalTranslation, baseline.original().getQuiz().getTranslation());
            return null;
        }).when(store).saveIfVersion(any(), eq(5), eq(BatchStatus.CLOSED));
        var request = correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false);
        service.execute(request);
        var saved = batch.getComparisonBaselines().get(0);
        after.setTranslation("Mutated later"); originalQuiz.setTranslation("Original mutated later");
        saved.before().getQuiz().setTranslation("Reader attempted rewrite");
        assertEquals("Earlier correction, before this operation", saved.before().getQuiz().getTranslation());
        assertEquals(originalTranslation, saved.original().getQuiz().getTranslation());
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var restored = json.readValue(json.writeValueAsBytes(saved), ComparisonBaseline.class);
        assertEquals(saved, restored);
        service.execute(request);
        assertEquals(1, batch.getComparisonBaselines().size());
        verify(runner, times(1)).allowance(anyInt(), anyInt());
    }

    @Test void checkpointIncludesEverySelectedQuizWithZeroBudgetAndRetainsEarlierOperations() {
        var existing = new ComparisonBaseline("previous-operation", 4, quiz,
                TandaFixture.locator().snapshot(course, AuditTarget.QUIZ, quiz).orElseThrow(),
                TandaFixture.locator().snapshot(course, AuditTarget.QUIZ, quiz).orElseThrow());
        batch.setComparisonBaselines(List.of(existing));
        var siblingFinding = finding("translation-sense", "wrong-sense", sibling);
        sourceTasks.add(TandaFixture.task("sibling", siblingFinding, knowledge, CefrLevel.A1));
        report = TandaFixture.report(course, List.of(instruction, translation, siblingFinding), Map.of(), List.of());
        when(allowance.spent()).thenReturn(0);
        when(allowance.tripped()).thenReturn(Optional.of("Tope de consultas: 0"));
        doAnswer(i -> i.getArgument(0)).when(panels).runSelected(any(), any(), any(), any(), any(), any());
        var selected = List.of(quiz, sibling);
        State estimate = service.execute(estimate(selected, false));
        var request = new Request("correct", "batch", "operation", 5, estimate.fingerprint(), 0, selected, false, "Jose");
        var result = service.execute(request);
        assertEquals("PARTIAL", result.status()); assertEquals(0, result.spent());
        assertEquals(3, batch.getComparisonBaselines().size()); assertSame(existing, batch.getComparisonBaselines().get(0));
        assertEquals(Set.of(quiz, sibling), batch.getComparisonBaselines().subList(1, 3).stream()
                .map(ComparisonBaseline::quizId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(10, batch.getQueriesSpent());
    }

    @Test void failureBeforePanelKeepsFrozenBaselineAndCanBeReadAfterJsonRoundTrip() throws Exception {
        doThrow(new IllegalStateException("source cannot be saved")).when(audits).save(any());
        var request = correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false);
        assertEquals("NEEDS_REVIEW", service.execute(request).status());
        assertEquals(5, batch.getContentVersion());
        assertEquals(1, batch.getComparisonBaselines().size());
        var json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        // BatchStore implementations serialize these optional references as part of the normal batch checkpoint.
        var checkpoint = new CorrectionBatch(); checkpoint.setComparisonBaselines(batch.getComparisonBaselines());
        var restored = json.readValue(json.writeValueAsBytes(checkpoint), CorrectionBatch.class);
        assertEquals(checkpoint, restored);
        service.execute(request); assertEquals(1, batch.getComparisonBaselines().size());
        verifyNoInteractions(runner, panels);
    }

    @Test void identicalRetryDoesNotReserveOrPayAgainAndChangedRequestIsRejected() {
        Request request = correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false);
        State first = service.execute(request); State again = service.execute(request);
        assertEquals(first.runId(), again.runId()); verify(runner, times(1)).allowance(anyInt(), anyInt());
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("correct", "batch", "operation", 5,
                request.fingerprint(), 9, List.of(quiz), false, "Jose")));
    }

    @Test void changedOpinionOrIntegrationRejectsSameCourseVersionBeforeReservation() {
        State estimate = service.execute(estimate(List.of(quiz), false));
        translation.getEvidence().setObservation("Changed evidence, same content version");
        assertThrows(IllegalStateException.class, () -> service.execute(correct(estimate, List.of(quiz), false)));
        verifyNoInteractions(runner, panels, audits);
        State nextEstimate = service.execute(estimate(List.of(quiz), false));
        selections.add(new AnalysisSelection("integration", "batch", "correction", "replace", "run", null, "translation-sense",
                List.of(quiz), 0, 5, "course-fingerprint", "Jose", Instant.now()));
        State previous = nextEstimate;
        assertThrows(IllegalStateException.class, () -> service.execute(correct(previous, List.of(quiz), false)));
    }

    @Test void changedRuntimeOrSelectionRejectsEstimate() {
        State estimate = service.execute(estimate(List.of(quiz), false));
        var other = new DefaultQuizCorrectionService(config, ledger, panels, reviews, c -> "course-fingerprint", "other-provider/model/medium", config.getElementLocator());
        assertThrows(IllegalStateException.class, () -> other.execute(correct(estimate, List.of(quiz), false)));
        assertThrows(IllegalStateException.class, () -> service.execute(correct(estimate, List.of(), false)));
    }

    @Test void approvedQuizzesAreVisibleButProtectedUntilExplicitlyIncluded() {
        var step = new BatchChangeStep(); step.setQuizId(quiz);
        var item = new BatchReview.Item("change", step, null, null, null, null, BatchReviewDecision.Status.APPROVED,
                "Jose", Instant.now(), List.of(), null);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(), List.of(item), List.of(), List.of()));
        var state = service.execute(estimate(List.of(quiz), false));
        assertTrue(state.quizzes().get(0).approved()); assertFalse(state.allowed());
        assertThrows(IllegalStateException.class, () -> service.execute(correct(state, List.of(quiz), false)));
        assertTrue(service.execute(estimate(List.of(quiz), true)).allowed());
        verifyNoInteractions(runner, panels);
    }

    @Test void unsupportedCurrentProblemBlocksInsteadOfOmittingItAndCoveredFindingsAreRetained() {
        Finding unsupported = finding("unknown-judge", "unsupported", quiz);
        report = TandaFixture.report(course, List.of(instruction, translation, unsupported), Map.of(), List.of());
        State state = service.execute(estimate(List.of(quiz), false));
        assertFalse(state.allowed()); assertEquals(3, state.quizzes().get(0).findings().size()); assertTrue(state.reason().contains("unknown-judge"));
        sourceTasks.get(0).getOrigin().setCoveredFindings(List.of(unsupported));
        state = service.execute(estimate(List.of(quiz), false)); assertTrue(state.allowed());
        service.execute(correct(state, List.of(quiz), false));
        assertEquals(List.of(unsupported), savedPlans.get(batch.getPlanId()).getTasks().get(1).getOrigin().getCoveredFindings());
    }

    @Test void relatedTopicProblemBlocksOnlyAffectedQuizzesAndDoesNotWidenSelection() {
        Finding topicFinding = finding("near-duplicates", "duplicate", quiz);
        topicFinding.setNode(new FindingNodeRef(AuditTarget.KNOWLEDGE, knowledge, knowledge));
        sourceTasks.add(TandaFixture.topicTask("topic", topicFinding, knowledge, List.of(sibling)));
        report = TandaFixture.report(course, List.of(instruction, translation, topicFinding), Map.of(), List.of());
        State state = service.execute(estimate(List.of(quiz), false));
        assertTrue(state.allowed());
        var other = state.quizzes().stream().filter(q -> q.quizId().equals(sibling)).findFirst().orElseThrow();
        assertFalse(other.correctable()); assertEquals(1, other.findings().size());
    }

    @Test void interruptionKeepsReservationAndBlocksAutomaticReplayAndLaterRuns() {
        doThrow(new IllegalStateException("interrupted")).when(panels).runSelected(any(), any(), any(), any(), any(), any());
        Request request = correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false);
        State result = service.execute(request);
        assertEquals("NEEDS_REVIEW", result.status()); assertEquals(8, result.runs().get(0).reserved());
        assertEquals("NEEDS_REVIEW", service.execute(request).status());
        assertFalse(service.execute(estimate(List.of(quiz), false)).allowed());
        verify(runner, times(1)).allowance(anyInt(), anyInt());
    }

    @Test void launchRequiresExplicitBudgetAndAuthorAndClosedBatch() {
        State state = service.execute(estimate(List.of(quiz), false));
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("correct", "batch", "operation", 5,
                state.fingerprint(), null, List.of(quiz), false, "Jose")));
        batch.setStatus(BatchStatus.RUNNING); assertFalse(service.execute(estimate(List.of(quiz), false)).allowed());
        verifyNoInteractions(runner, panels);
    }

    @Test void idleOpenCorrectionCanAppendNewVersionsWithoutPayingForFullClosingAndRemainsOpen() {
        batch.setStatus(BatchStatus.OPEN);
        State estimate = service.execute(estimate(List.of(quiz), false));
        assertTrue(estimate.allowed());
        State result = service.execute(correct(estimate, List.of(quiz), false));
        assertEquals("DONE", result.status()); assertEquals(6, result.contentVersion());
        assertEquals(1, result.correctionRevision()); assertEquals(BatchStatus.OPEN, batch.getStatus());
        assertEquals(BatchStatus.OPEN, result.runs().get(0).process().idleStatus());
        verify(store).saveIfVersion(same(batch), eq(5), eq(BatchStatus.OPEN));
        verify(panels).runSelected(any(), same(report), same(course), same(allowance), any(), any());
    }

    @Test void interruptedOrPublishedCorrectionCannotLaunchSelectedQuizzes() {
        for (BatchStatus status : List.of(BatchStatus.RUNNING, BatchStatus.INTERRUPTED, BatchStatus.PUBLISHED)) {
            batch.setStatus(status);
            State estimate = service.execute(estimate(List.of(quiz), false));
            assertFalse(estimate.allowed());
            assertThrows(IllegalStateException.class, () -> service.execute(correct(estimate, List.of(quiz), false)));
        }
        verifyNoInteractions(runner, panels);
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }

    @Test void rankingOpinionsRemainVisibleAndReachProposerAsContextWithoutPermissionOrCompletionSemantics() {
        Finding naturalness = finding("naturalness", "odd-usage", quiz); naturalness.setResolution(FindingResolution.RANK_ONLY);
        Finding siblingOpinion = finding("pragmatism", "awkward", sibling); siblingOpinion.setResolution(FindingResolution.RANK_ONLY);
        report = TandaFixture.report(course, List.of(instruction, translation, naturalness, siblingOpinion), Map.of(), List.of());
        State state = service.execute(estimate(List.of(quiz), false)); assertTrue(state.allowed());
        assertEquals(3, state.quizzes().stream().filter(q -> q.quizId().equals(quiz)).findFirst().orElseThrow().findings().size());
        assertFalse(state.quizzes().stream().filter(q -> q.quizId().equals(sibling)).findFirst().orElseThrow().correctable());
        var context = DefaultQuizCorrectionService.contextFindings(report, sourceTasks.get(0));
        assertEquals(List.of(instruction, translation, naturalness), context);
        assertFalse(sourceTasks.get(0).getOrigin().getCoveredFindings().contains(naturalness));
    }

    @Test void declaredContextOnlyJudgeNeverNeedsAnInventedTaskAndStillReachesTheCorrector() {
        Finding didactic = finding("a1-didactic", "a1-example-quality", quiz);
        Finding onlyContext = finding("a1-didactic", "a1-example-quality", sibling);
        report = TandaFixture.report(course, List.of(instruction, translation, didactic, onlyContext), Map.of(), List.of());
        AnalyzerCorrectionCatalog catalog = mock(AnalyzerCorrectionCatalog.class);
        AnalyzerCorrection declaration = mock(AnalyzerCorrection.class);
        when(catalog.find(any())).thenReturn(Optional.empty());
        when(catalog.find("a1-didactic")).thenReturn(Optional.of(declaration));
        when(declaration.contextOnly()).thenReturn(true);
        config.setCorrectionCatalog(catalog);
        State state = service.execute(estimate(List.of(quiz), false));
        assertTrue(state.allowed(), "context does not prevent correcting existing scoped tasks");
        assertEquals(3, state.quizzes().stream().filter(q -> q.quizId().equals(quiz)).findFirst().orElseThrow().findings().size());
        assertFalse(state.quizzes().stream().filter(q -> q.quizId().equals(sibling)).findFirst().orElseThrow().correctable(),
                "context alone grants no edit permission");
        assertEquals(List.of(instruction, translation, didactic),
                DefaultQuizCorrectionService.contextFindings(report, sourceTasks.get(0)));
        assertEquals(FindingResolution.PANEL, didactic.getResolution(), "saved judgment is not rewritten");
        assertFalse(sourceTasks.get(0).getOrigin().getCoveredFindings().contains(didactic),
                "context is not a task completion requirement");
        when(declaration.contextOnly()).thenReturn(false);
        assertFalse(service.execute(estimate(List.of(quiz), false)).allowed(),
                "an undeclared unsupported error must still block");
        verifyNoInteractions(panels, runner, audits);
    }

    @Test void provenanceWithRecordedTimeCanBeFingerprintedAndChangedWithoutChangingCourse() {
        var node = DefaultAnalysisComposer.index(report.getRoot()).get(quiz);
        var origin = new LinkedHashMap<String, Object>(); origin.put("measuredAt", Instant.parse("2026-10-23T10:00:00Z"));
        origin.put("runtime", "codex-cli/model/medium"); origin.put("runId", "measurement");
        node.getMetadata().put(DefaultAnalysisComposer.PROVENANCE, Map.of("quiz-instruction", origin));
        State first = service.execute(estimate(List.of(quiz), false));
        assertEquals(first.fingerprint(), service.execute(estimate(List.of(quiz), false)).fingerprint());
        origin.put("runtime", "other-provider/model/medium");
        assertNotEquals(first.fingerprint(), service.execute(estimate(List.of(quiz), false)).fingerprint());
    }

    @Test void factoryWithoutConfiguredLocatorCanReserveAndFinishAZeroBudgetRun() {
        // Main intentionally leaves the locator null; the factory must inject its effective default
        // into both the ledger and the pre-pass comparison capture, not just into the ledger.
        config.setElementLocator(null);
        config.setArtifactStore(new TandaFixture.Artifacts());
        PanelRunner panelRunner = mock(PanelRunner.class);
        config.setPanelRunner(panelRunner);
        when(panelRunner.runTopics(any(), any(), any(), any(), isNull())).thenReturn(List.of());
        when(allowance.spent()).thenReturn(0);
        when(allowance.tripped()).thenReturn(Optional.of("tope cero"));
        var actual = new DefaultQuizCorrectionServiceFactory().create(config, c -> "course-fingerprint", "codex-cli/model/medium");
        var state = actual.execute(estimate(List.of(quiz), false));
        var result = actual.execute(new Request("correct", "batch", "factory-default-locator", 5,
                state.fingerprint(), 0, List.of(quiz), false, "test"));
        assertEquals("PARTIAL", result.status());
        assertEquals(0, result.spent());
        assertEquals(6, result.contentVersion());
        assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertEquals(10, batch.getQueriesSpent());
        assertEquals(1, batch.getComparisonBaselines().size());
        assertEquals(quiz, batch.getComparisonBaselines().get(0).before().getNodeId());
        assertEquals(quiz, batch.getComparisonBaselines().get(0).original().getNodeId());
        assertEquals(0, batch.getSelectiveRuns().get(0).queryCap());
        verify(store).saveIfVersion(any(), eq(5), eq(BatchStatus.CLOSED));
        assertNull(config.getElementLocator(), "the factory must not mutate the caller's configuration");
        verifyNoInteractions(panels);
    }

    @Test void factoryDeliversEveryCurrentOpinionToNativePanelBriefsWithoutChangingCoveredFindings() {
        Finding naturalness = finding("naturalness", "odd-usage", quiz); naturalness.setResolution(FindingResolution.RANK_ONLY);
        report = TandaFixture.report(course, List.of(instruction, translation, naturalness), Map.of(), List.of());
        config.setArtifactStore(new TandaFixture.Artifacts()); config.setElementLocator(TandaFixture.locator());
        PanelRunner panelRunner = mock(PanelRunner.class); config.setPanelRunner(panelRunner);
        var seen = new ArrayList<com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief>();
        when(panelRunner.runTopics(any(), any(), any(), any(), isNull())).thenAnswer(i -> {
            List<TopicWork> works = i.getArgument(0);
            works.forEach(w -> seen.addAll(w.getBriefs())); return List.of();
        });
        var nativeService = new DefaultQuizCorrectionServiceFactory().create(config, c -> "course-fingerprint", "codex-cli/model/medium");
        var state = nativeService.execute(estimate(List.of(quiz), false));
        nativeService.execute(correct(state, List.of(quiz), false));
        assertEquals(2, seen.size());
        for (var brief : seen) {
            assertEquals(List.of(instruction, translation, naturalness), brief.getContextFindings());
            assertFalse(brief.getCoveredFindings().contains(naturalness));
        }
    }
    private void historicalPass() {
        var q = TandaFixture.exercise(course, quiz);
        q.setInstructions("Transforma con can o could.");
        q.setForm(com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(q,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("Polite request: Bring me tea."),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("Could you bring me tea?")).getForm());
        q.setMiniTheory("$Can$ para normal, $Could$ para cortés. Verbo base.");
        BatchBook.place(course, quiz).orElseThrow().knowledge().setSentenceMode(SentenceMode.REWRITE);
        service.execute(correct(service.execute(estimate(List.of(quiz, sibling), false)), List.of(quiz, sibling), false));
    }
    private Request historicalEstimate(List<String> ids) {
        return new Request("estimate", "batch", null, null, null, null, ids, false, null, "operation");
    }
    private Request historicalCorrect(State state, List<String> ids) {
        return new Request("correct", "batch", "repair", batch.getContentVersion(), state.fingerprint(), 8, ids, false, "Jose", "operation");
    }
    private void removeCurrentProblems() {
        sourceTasks.clear(); report = TandaFixture.report(course, List.of(), Map.of(), List.of());
    }
    private void loseSelector() {
        var q = TandaFixture.exercise(course, quiz);
        q.setForm(com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(q,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("Bring me tea."),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("Could you bring me tea?", "Can you bring me tea?")).getForm());
    }

    @Test void historicalScopeKeepsHealthySelectionsAndAddsAnExplicitRepairWithoutJudgeOrigins() {
        // The first ordinary pass needs findings for both selected quizzes.
        var siblingProblem = finding("quiz-instruction", "bad-instruction", sibling);
        sourceTasks.add(TandaFixture.task("sibling", siblingProblem, knowledge, CefrLevel.A1));
        report = TandaFixture.report(course, List.of(instruction, translation, siblingProblem), Map.of(), List.of());
        historicalPass(); loseSelector(); removeCurrentProblems(); clearInvocations(panels);
        var state = service.execute(historicalEstimate(List.of(quiz, sibling)));
        assertTrue(state.allowed(), state.reason()); assertEquals(2, state.selectedQuizzes());
        var result = service.execute(historicalCorrect(state, List.of(quiz, sibling)));
        var run = result.runs().stream().filter(r -> r.id().equals("repair")).findFirst().orElseThrow();
        assertEquals("operation", run.sourceRunId()); assertEquals(List.of(quiz, sibling).stream().sorted().toList(), run.quizIds());
        assertTrue(run.detail().contains(sibling + "=NO_LONGER_HOLDS"));
        assertEquals(1, run.taskIds().size());
        var task = savedPlans.get(batch.getPlanId()).getTasks().stream().filter(t -> run.taskIds().contains(t.getId())).findFirst().orElseThrow();
        assertNull(task.getOrigin(), "the explicit order must not invent a judge finding");
        assertTrue(task.getNodeLabel().contains("contrato"));
        assertEquals(7, batch.getContentVersion());
        assertEquals(2, batch.getComparisonBaselines().stream().filter(b -> b.operationId().equals("repair")).count());
        clearInvocations(panels, runner);
        service.execute(new Request("correct", "batch", "repair", 6, state.fingerprint(), 8, List.of(quiz, sibling), false, "Jose", "operation"));
        verifyNoInteractions(panels, runner);
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("correct", "batch", "repair", 6,
                state.fingerprint(), 8, List.of(quiz, sibling), false, "Jose", null)));
    }

    @Test void historicalReferencesAreFingerprintBoundAndRejectMissingOrAmbiguousSnapshotsBeforeReservation() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        var regular = service.execute(estimate(List.of(quiz), false));
        var historical = service.execute(historicalEstimate(List.of(quiz)));
        assertNotEquals(regular.fingerprint(), historical.fingerprint());
        assertThrows(IllegalArgumentException.class, () -> service.execute(historicalEstimate(List.of(sibling))));
        var snapshots = new ArrayList<>(batch.getComparisonBaselines()); snapshots.add(snapshots.get(0)); batch.setComparisonBaselines(snapshots);
        clearInvocations(store, panels, runner);
        assertThrows(IllegalArgumentException.class, () -> service.execute(historicalEstimate(List.of(quiz))));
        verify(store, never()).saveIfVersion(any(), anyInt(), any()); verifyNoInteractions(panels, runner);
    }

    @Test void aFullyHealthyHistoricalSelectionFinishesWithoutModelsTasksOrAContentVersionBump() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        removeCurrentProblems(); clearInvocations(panels, runner);
        var state = service.execute(historicalEstimate(List.of(quiz)));
        assertTrue(state.allowed());
        var result = service.execute(historicalCorrect(state, List.of(quiz)));
        assertEquals("DONE", result.status()); assertEquals(0, result.spent()); assertEquals(6, result.contentVersion());
        assertTrue(result.reason().contains("NO_LONGER_HOLDS"));
        var run = result.runs().stream().filter(r -> r.id().equals("repair")).findFirst().orElseThrow();
        assertTrue(run.taskIds().isEmpty()); verifyNoInteractions(panels, runner);
    }

    @Test void factoryTransportsTheExactHistoricalReferenceAndExplicitBriefWhileKeepingCurrentContent() {
        // Save the source operation with a single quiz, then simulate its lost selector in the editable version.
        var q = TandaFixture.exercise(course, quiz);
        q.setInstructions("Usa can o could."); q.setMiniTheory("Verbo base tras can o could.");
        q.setForm(com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(q,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("Polite: Bring me tea."),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("Could you bring me tea?")).getForm());
        BatchBook.place(course, quiz).orElseThrow().knowledge().setSentenceMode(SentenceMode.REWRITE);
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        loseSelector(); removeCurrentProblems();
        var contextOnly = finding("naturalness", "context-opinion", quiz); contextOnly.setResolution(FindingResolution.RANK_ONLY);
        report = TandaFixture.report(course, List.of(contextOnly), Map.of(), List.of());
        config.setArtifactStore(new TandaFixture.Artifacts());
        PanelRunner actualRunner = mock(PanelRunner.class); config.setPanelRunner(actualRunner);
        var seen = new ArrayList<TopicWork>();
        when(actualRunner.runTopics(any(), any(), any(), any(), isNull())).thenAnswer(i -> {
            seen.addAll(i.getArgument(0)); return List.of();
        });
        var actual = new DefaultQuizCorrectionServiceFactory().create(config, c -> "course-fingerprint", "runtime");
        var estimate = actual.execute(historicalEstimate(List.of(quiz)));
        assertTrue(estimate.allowed(), estimate.reason());
        actual.execute(historicalCorrect(estimate, List.of(quiz)));
        assertEquals(1, seen.size());
        assertEquals("Bring me tea.", seen.get(0).getKnowledge().getQuizTemplates().stream().filter(x -> x.getId().equals(quiz))
                .findFirst().orElseThrow().getForm().getSentenceParts().get(0).getText());
        assertEquals("Polite: Bring me tea.", seen.get(0).getDidacticReferences().get(quiz).sourceQuiz()
                .getForm().getSentenceParts().get(0).getText());
        assertEquals(1, seen.get(0).getBriefs().size());
        assertInstanceOf(DidacticRepairBrief.class, seen.get(0).getBriefs().get(0));
        assertNull(seen.get(0).getBriefs().get(0).getFinding());
        assertEquals(List.of(contextOnly), seen.get(0).getBriefs().get(0).getContextFindings());
        assertEquals(Set.of(quiz), seen.get(0).getNewEvaluationSubjectIds());
    }

    @Test void aHistoricalQuizWithOnlyContextOpinionsIsKeptWithoutInventingACorrectionTask() {
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        removeCurrentProblems();
        var opinion = finding("naturalness", "opinion", quiz); opinion.setResolution(FindingResolution.RANK_ONLY);
        report = TandaFixture.report(course, List.of(opinion), Map.of(), List.of());
        clearInvocations(panels, runner);
        var estimate = service.execute(historicalEstimate(List.of(quiz)));
        assertTrue(estimate.allowed(), estimate.reason()); assertEquals(List.of(opinion), estimate.quizzes().get(0).findings());
        var result = service.execute(historicalCorrect(estimate, List.of(quiz)));
        assertEquals("DONE", result.status()); assertEquals(0, result.spent()); assertEquals(6, result.contentVersion());
        assertTrue(result.reason().contains("NO_LONGER_HOLDS")); verifyNoInteractions(panels, runner);
    }

    @Test void closedFormulaIsExplicitFingerprintBoundFrozenAndCheckedOnIdempotentRetry() {
        var q = TandaFixture.exercise(course, quiz);
        q.setInstructions("Usa can o could.");
        q.setForm(com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(q,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("Play the radio. Polite:"),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("Could you play the radio?")).getForm());
        BatchBook.place(course, quiz).orElseThrow().knowledge().setSentenceMode(SentenceMode.REWRITE);
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        removeCurrentProblems();
        var ordinary = service.execute(historicalEstimate(List.of(quiz)));
        var explicit = service.execute(new Request("estimate", "batch", null, null, null, null, List.of(quiz), false, null, "operation", true));
        assertNotEquals(ordinary.fingerprint(), explicit.fingerprint());
        var request = new Request("correct", "batch", "closed-formula", 6, explicit.fingerprint(), 8, List.of(quiz), false, "Jose", "operation", true);
        var result = service.execute(request);
        var run = result.runs().stream().filter(r -> r.id().equals("closed-formula")).findFirst().orElseThrow();
        assertEquals(1, run.taskIds().size(), "the intact radio still receives the new explicit formula order");
        var reference = batch.getComparisonBaselines().stream().filter(x -> x.operationId().equals("closed-formula")).findFirst().orElseThrow().didacticReference();
        assertTrue(reference.restoreRequestFormula()); assertTrue(reference.operatorInstruction().contains("Could + sujeto + verbo base"));
        clearInvocations(panels, runner); service.execute(request); verifyNoInteractions(panels, runner);
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("correct", "batch", "closed-formula", 6,
                explicit.fingerprint(), 8, List.of(quiz), false, "Jose", "operation", false)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("estimate", "batch", null, null, null, null,
                List.of(quiz), false, null, null, true)));
    }

    @Test void ancestorReferenceIsFingerprintBoundAndFrozenWhileTheSuccessorRemainsTheEditableBase() {
        var q = TandaFixture.exercise(course, quiz);
        q.setInstructions("Transforma con can o could.");
        q.setForm(com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(q,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("Polite: Bring me tea."),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("Could you bring me tea?")).getForm());
        BatchBook.place(course, quiz).orElseThrow().knowledge().setSentenceMode(SentenceMode.REWRITE);
        service.execute(correct(service.execute(estimate(List.of(quiz), false)), List.of(quiz), false));
        loseSelector();
        var child = TandaFixture.batch("child"); child.setContentVersion(3); child.setCorrectionRevision(2);
        child.setOpenedAt(batch.getSelectiveRuns().get(0).finishedAt().plusSeconds(1));
        child.setPlanId("child-plan");
        var childPlan = TandaFixture.plan(); childPlan.setId("child-plan"); savedPlans.put("child-plan", childPlan);
        child.setEvolution(new CorrectionEvolution.Lineage("batch", 6, 0, "next", "course-fingerprint", course, course, List.of()));
        var childCourse = CorrectionEvolution.copyCourse(course);
        TandaFixture.exercise(childCourse, quiz).setTranslation("Una mejora válida de la corrección actual.");
        when(store.load("child")).thenReturn(Optional.of(child));
        when(reviews.read("child")).thenReturn(new BatchReview("child", 2, 3, null, List.of(), List.of(), List.of(), List.of(), List.of()));
        when(ledger.composedCourse(same(batch), any())).thenReturn(course);
        when(ledger.composedCourse(same(child), any())).thenReturn(childCourse);
        sourceTasks.clear(); report = TandaFixture.report(childCourse, List.of(), Map.of(), List.of());
        var request = new Request("estimate", "child", null, null, null, null, List.of(quiz), false, null, "operation", true);
        var state = service.execute(request); assertTrue(state.allowed(), state.reason());
        var saved = batch.getComparisonBaselines();
        var old = saved.get(0); var changed = old.before(); changed.getQuiz().setMiniTheory("Otro contrato histórico");
        batch.setComparisonBaselines(List.of(new ComparisonBaseline(old.operationId(), old.sourceContentVersion(), quiz, changed, old.original())));
        assertNotEquals(state.fingerprint(), service.execute(request).fingerprint());
        var correct = new Request("correct", "child", "repair-child", 3, state.fingerprint(), 8, List.of(quiz), false, "Jose", "operation", true);
        clearInvocations(store, runner, panels);
        assertThrows(IllegalStateException.class, () -> service.execute(correct));
        verify(store, never()).saveIfVersion(any(), anyInt(), any()); verifyNoInteractions(runner, panels);
        batch.setComparisonBaselines(saved);
        var result = service.execute(correct);
        var frozen = child.getComparisonBaselines().get(0);
        assertEquals("Una mejora válida de la corrección actual.", frozen.before().getQuiz().getTranslation());
        assertEquals("Polite: Bring me tea.", frozen.didacticReference().sourceQuiz().getForm().getSentenceParts().get(0).getText());
        assertEquals("batch", frozen.didacticReference().provenance().sourceBatchId());
        assertEquals(5, frozen.didacticReference().sourceContentVersion());
        assertEquals(4, result.contentVersion()); assertEquals(6, batch.getContentVersion());
        assertEquals(saved, batch.getComparisonBaselines());
        verify(store, never()).save(same(batch));
        // Replay uses the reserved source, even if the ancestor is unavailable afterwards.
        when(store.load("batch")).thenReturn(Optional.empty()); clearInvocations(store, runner, panels);
        assertEquals("repair-child", service.execute(correct).runId());
        verify(store, never()).load("batch"); verifyNoInteractions(runner, panels);
    }

}
