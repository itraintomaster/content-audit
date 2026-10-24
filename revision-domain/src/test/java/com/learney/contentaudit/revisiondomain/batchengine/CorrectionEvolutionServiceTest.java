package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.CorrectionEvolution.*;
import com.learney.contentaudit.revisiondomain.engine.DefaultCourseElementLocator;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class CorrectionEvolutionServiceTest {
    final TandaFixture.Courses courses = new TandaFixture.Courses(TandaFixture.course());
    final TandaFixture.Artifacts artifacts = new TandaFixture.Artifacts();
    final TandaFixture.Plans plans = new TandaFixture.Plans();
    final TandaFixture.Reports reports = new TandaFixture.Reports();
    final List<AnalysisSelection> selections = new ArrayList<>();
    final TandaFixture.Batches batches = new TandaFixture.Batches() {
        @Override public List<AnalysisSelection> analysisSelections(String id) {
            return selections.stream().filter(s -> id.equals(s.batchId())).toList();
        }
        @Override public List<BatchReviewDecision> reviews(String id) {
            var out = new ArrayList<BatchReviewDecision>();
            load(id).filter(b -> b.getEvolution() != null).ifPresent(b -> out.addAll(b.getEvolution().inheritedReviews()));
            out.addAll(super.reviews(id)); return out;
        }
    };
    final CurrentAnalysisService analysis = mock(CurrentAnalysisService.class);
    final RefinerEngine refiner = mock(RefinerEngine.class);
    final ActiveAnalysisSelectionStore active = mock(ActiveAnalysisSelectionStore.class);
    final BatchServiceConfig config = new BatchServiceConfig();
    final CorrectionBatch first = TandaFixture.batch("c1");
    final DefaultBatchChangeLedger ledger = new DefaultBatchChangeLedger(artifacts, new DefaultCourseElementLocator()).withBatches(batches);
    CorrectionEvolutionService service;
    String quiz;
    @BeforeEach void setup() {
        quiz = courses.course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().get(0).getId();
        first.setCorrectionRevision(1); first.setContentVersion(8); first.setQueriesSpent(19);
        batches.save(first); plans.save(TandaFixture.plan());
        config.setBatchStore(batches); config.setCourseRepository(courses); config.setCoursePath(TandaFixture.COURSE_PATH);
        config.setArtifactStore(artifacts); config.setRefinementPlanStore(plans); config.setAuditReportStore(reports);
        config.setCurrentAnalysisService(analysis); config.setRefinerEngine(refiner); config.setActiveAnalysisSelectionStore(active);
        when(analysis.report(any(), eq("correction"), any())).thenAnswer(i -> TandaFixture.report(i.getArgument(2), List.of(), Map.of(), List.of()));
        when(analysis.execute(any())).thenAnswer(i -> {
            var r = i.getArgument(0, CurrentAnalysisService.Request.class); var b = batches.load(r.batchId()).orElseThrow();
            return new CurrentAnalysisService.View(b.getId(), b.getContentVersion(), batches.analysisSelections(b.getId()).size(), List.of(), List.of(), List.of(), List.of());
        });
        when(analysis.comparisonView(any())).thenAnswer(i -> analysis.execute(new CurrentAnalysisService.Request(
                "read",i.getArgument(0,CorrectionBatch.class).getId(),null,null,null,null,null,null,null,null)));
        when(refiner.plan(any(), anyString())).thenAnswer(i -> TandaFixture.plan());
        service = new DefaultCorrectionEvolutionServiceFactory().create(config);
    }
    Estimate estimate(String id) { return service.execute(new Request("estimate", id, null, null, null, null, null)).estimate(); }
    Request request(String id, String op) {
        var e = estimate(id); return new Request("create", id, op, e.contentVersion(), e.fingerprint(), e.analysisRevision(), "José");
    }
    CorrectionBatch create(String id, String op) {
        var response = service.execute(request(id, op)); assertEquals("CREATED", response.result().status());
        return batches.load(response.result().newBatchId()).orElseThrow();
    }
    void change(CorrectionBatch batch, String id, String translation) {
        var after = BatchBook.copy(TandaFixture.exercise(ledger.composedCourse(batch, courses.course), quiz));
        after.setTranslation(translation);
        ledger.record(batch, TandaFixture.change(id, BatchChangeOrigin.JOSE, "manual", List.of(),
                TandaFixture.step(after, ExercisePart.TRANSLATION)), courses.course);
    }
    @Test void catalogListsBaseAndThreeCorrectionsWithoutReadingCoursesComposingOrEvaluatingAnalysis() {
        var second = create("c1","catalog-c2"); var third = create(second.getId(),"catalog-c3");
        first.setClosings(List.of(new BatchClosing(),new BatchClosing()));
        selections.add(new AnalysisSelection("catalog-analysis","c1","base","integrate","not-loaded",null,
                "judge",List.of(),0,8,"fingerprint","José",Instant.EPOCH));
        var unreadCourses = mock(CourseRepository.class); config.setCourseRepository(unreadCourses);
        var unusedLedger = mock(BatchChangeLedger.class); var unusedSelector = mock(BatchTaskSelector.class);
        @SuppressWarnings("unchecked")
        java.util.function.Function<CourseEntity,String> unusedFingerprint = mock(java.util.function.Function.class);
        clearInvocations(analysis,refiner,active);
        var fast = new DefaultCorrectionEvolutionService(config,unusedLedger,unusedSelector,unusedFingerprint);
        var result = fast.execute(new Request("catalog",third.getId(),null,null,null,null,null));
        assertEquals(List.of("Base","Corrección 1","Corrección 2","Corrección 3"),result.versions().stream().map(Version::label).toList());
        assertNull(result.estimate()); assertNull(result.result());
        assertEquals(List.of(true,true,true,false),result.versions().stream().map(Version::frozen).toList());
        for (var version : result.versions()) {
            assertNull(version.course()); assertNull(version.fingerprint()); assertNotNull(version.analysis());
            assertEquals(1,version.analysis().states().size());
            assertNull(version.analysis().states().get(0).digest());
            assertTrue(version.analysis().looseRuns().isEmpty()); assertTrue(version.analysis().history().isEmpty());
        }
        assertEquals("A0",result.versions().get(0).analysis().states().get(0).analysisCode());
        assertEquals("A2",result.versions().get(1).analysis().states().get(0).analysisCode());
        assertNull(result.versions().get(2).analysis().states().get(0).analysisCode());
        assertNull(result.versions().get(3).analysis().states().get(0).analysisCode());
        assertEquals(List.of(1,1,0,0),result.versions().stream().map(Version::analysisRevision).toList());
        assertEquals("base",result.versions().get(0).analysis().states().get(0).id());
        assertEquals("correction",result.versions().get(3).analysis().states().get(0).id());
        verifyNoInteractions(unreadCourses,unusedLedger,unusedSelector,unusedFingerprint,analysis,refiner,active);
    }
    @Test void chainsThreeCorrectionsWithoutPublishingAndKeepsCumulativeChanges() {
        change(first, "first-edit", "Primera");
        var second = create("c1", "next-2");
        assertEquals(2, second.getCorrectionRevision()); assertEquals("Primera", translation(second));
        change(second, "second-edit", "Segunda"); second.setContentVersion(2);
        var third = create(second.getId(), "next-3");
        assertEquals(3, third.getCorrectionRevision()); assertEquals("Segunda", translation(third));
        assertEquals("Primera", translation(first)); assertEquals(2, third.getChanges().size());
        assertTrue(batches.hasSuccessor(first.getId())); assertTrue(batches.hasSuccessor(second.getId()));
        assertFalse(batches.hasSuccessor(third.getId())); assertEquals(BatchStatus.OPEN, first.getStatus());
        assertEquals(19, first.getQueriesSpent()); assertEquals(0, third.getQueriesSpent());
        assertTrue(courses.written.isEmpty()); verifyNoInteractions(active);
        assertEquals(List.of(0,1,2,3), service.execute(new Request("view", third.getId(), null,null,null,null,null))
                .versions().stream().map(Version::correctionRevision).toList());
    }
    @Test void checkpointsAreDeepCopiesAndEveryHistoryReadIgnoresLaterLiveCourseChanges() {
        String original = TandaFixture.exercise(courses.course, quiz).getTranslation();
        change(first, "first-edit", "Primera"); var second = create("c1", "next");
        TandaFixture.exercise(courses.course, quiz).setTranslation("Externamente cambiada");
        assertEquals("Primera", translation(first)); assertEquals("Primera", translation(second));
        assertEquals(original, TandaFixture.exercise(second.getEvolution().baseCourse(), quiz).getTranslation());
        var exposed = second.getEvolution().sourceCourse(); TandaFixture.exercise(exposed, quiz).setTranslation("Mutada");
        assertEquals("Primera", TandaFixture.exercise(second.getEvolution().sourceCourse(), quiz).getTranslation());
        var review = new DefaultBatchReview(batches, artifacts, plans, new DefaultBatchReportBuilder()).read(first.getId());
        assertTrue(review.contentFrozen()); assertEquals("Primera", TandaFixture.exercise(review.course(), quiz).getTranslation());
        assertEquals(original, TandaFixture.exercise(review.baseCourse(), quiz).getTranslation());
    }
    @Test void retryIsIdempotentButAnIdentityCannotDescribeDifferentSourceSeals() {
        var request = request("c1", "same"); var created = service.execute(request);
        assertEquals("EXISTING", service.execute(request).result().status()); assertEquals(2, batches.list().size());
        assertThrows(IllegalArgumentException.class, () -> service.execute(new Request("create", "c1", "same", 9,
                request.fingerprint(), request.analysisRevision(), "José")));
        assertEquals(created.result().newBatchId(), batches.open().orElseThrow().getId());
    }
    @Test void staleContentOrAnalysisDoesNotCreateAnyVersionOrMoveTheActivePlan() {
        var old = request("c1", "old"); first.setContentVersion(9);
        assertThrows(IllegalStateException.class, () -> service.execute(old));
        assertEquals(1,batches.list().size()); verifyNoInteractions(active); verifyNoInteractions(refiner);
    }
    @Test void existingSuccessorRefusesASecondBranchAndParentContentWritesButAllowsItsOwnReanalysis() {
        create("c1", "next");
        assertThrows(IllegalStateException.class, () -> batches.requireEditableContent(first));
        assertFalse(estimate(first.getId()).allowed());
        first.setAnalysisSeedAuditId("reanalyzed-parent"); first.setAnalysisSeedContentVersion(8); batches.save(first);
        assertEquals("reanalyzed-parent", batches.load("c1").orElseThrow().getAnalysisSeedAuditId());
        assertNotEquals("reanalyzed-parent", batches.open().orElseThrow().getAnalysisSeedAuditId());
    }
    @Test void unchangedHumanApprovalsArePreservedAndNewStepsRemainPending() {
        change(first, "first-edit", "Primera");
        var reviewService = new DefaultBatchReview(batches, artifacts, plans, new DefaultBatchReportBuilder());
        var item = reviewService.read("c1").items().get(0);
        batches.saveReview(new BatchReviewDecision("c1", List.of(new BatchReviewDecision.Selection(item.changeId(),item.step(),
                item.before(),item.after())), BatchReviewDecision.Status.APPROVED, "José", Instant.now()));
        var second = create("c1", "next");
        assertEquals(BatchReviewDecision.Status.APPROVED, reviewService.read(second.getId()).items().get(0).status());
        change(second, "second-edit", "Segunda");
        assertEquals(BatchReviewDecision.Status.PENDING, reviewService.read(second.getId()).items().get(1).status());
        assertEquals(BatchReviewDecision.Status.APPROVED, reviewService.read("c1").items().get(0).status());
    }
    @Test void legacyBatchHasCorrectionOneAndCanCreateCorrectionTwo() {
        first.setCorrectionRevision(0);
        assertEquals(2, create("c1", "legacy").getCorrectionRevision());
    }
    @Test void closingAnEvolutionWithNoRemainingTasksKeepsResolvedInheritedWorkAndRejectsForeignChanges() {
        change(first, "resolved-in-c1", "Primera");
        var child = create("c1", "next");
        assertTrue(child.getTaskIds().isEmpty(), "The current report has no remaining tasks for the inherited change");
        var base = child.getEvolution().baseCourse();
        var composed = ledger.composedCourse(child, base);
        assertTrue(DefaultBatchCloser.changedOutside(child, base, composed, Map.of()).isEmpty());
        var comparator = mock(com.learney.contentaudit.revisiondomain.comparison.BatchComparator.class);
        var pass = mock(BatchJudgePass.class);
        when(pass.judge(any(),any(),any(),any())).thenReturn(List.of());
        when(pass.estimate(any(),any(),any())).thenReturn(List.of());
        var report = TandaFixture.report(composed,List.of(),Map.of(),List.of());
        when(analysis.comparisonReports(any(),any(),any())).thenReturn(
                new CurrentAnalysisService.ComparisonReports(report,report,report));
        when(comparator.compareMeasured(any(),any(),any(),any(),any())).thenAnswer(i ->
                TandaFixture.passing(i.getArgument(1),List.of()));
        var closer = new DefaultBatchCloser(comparator,
                new com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory().batchGate(),
                null,ledger,batches,TandaFixture.COURSE_PATH,pass).withPlans(plans,null).withCurrentAnalysis(analysis);
        var closed = closer.close(child,base,new BatchCloseRequest(Map.of(),0));
        assertTrue(closed.getClosings().get(0).getGate().isPasses());
        assertEquals("Primera",translation(closed));
        verify(comparator).compareMeasured(any(),any(),any(),any(),any());
        verify(comparator,never()).compareSummary(any(),any());
        var foreign = CorrectionEvolution.copyCourse(composed);
        var quizzes = foreign.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates();
        String sibling = quizzes.stream().map(QuizTemplateEntity::getId).filter(id -> !id.equals(quiz)).findFirst().orElseThrow();
        TandaFixture.exercise(foreign,sibling).setTranslation("Cambio ajeno");
        assertEquals(List.of(sibling),DefaultBatchCloser.changedOutside(child,base,foreign,Map.of()));
        TandaFixture.exercise(foreign,quiz).setTranslation("Cambio nuevo sin tarea");
        assertEquals(Set.of(quiz,sibling),new HashSet<>(DefaultBatchCloser.changedOutside(child,base,foreign,Map.of())));
    }
    @Test void withdrawingOnlyOneInheritedStepKeepsTheRemainingAuthorizedWorkWithoutAllowingTampering() {
        change(first,"translation-1","Primera");
        var modified = BatchBook.copy(TandaFixture.exercise(ledger.composedCourse(first,courses.course),quiz));
        modified.setMiniTheory("Explicación heredada");
        ledger.record(first,TandaFixture.change("theory-1",BatchChangeOrigin.JOSE,"manual",List.of(),
                TandaFixture.step(modified,ExercisePart.MINI_THEORY)),courses.course);
        var child = create("c1","next-withdrawal"); var base = child.getEvolution().baseCourse();
        ledger.drop(child,"translation-1","rechazado por José",base);
        var composed = ledger.composedCourse(child,base);
        assertEquals("Explicación heredada",TandaFixture.exercise(composed,quiz).getMiniTheory());
        assertTrue(DefaultBatchCloser.changedOutside(child,base,composed,Map.of(),batches).isEmpty());
        TandaFixture.exercise(composed,quiz).setTranslation("Nueva traducción sin tarea");
        assertEquals(List.of(quiz),DefaultBatchCloser.changedOutside(child,base,composed,Map.of(),batches));
    }
    @Test void publishedLegacyHistoryWithoutAFullCheckpointNeverClaimsTodaysLiveCourseAsItsHistoricalContent() {
        first.setStatus(BatchStatus.PUBLISHED);
        TandaFixture.exercise(courses.course,quiz).setTranslation("Contenido publicado después");
        var result = service.execute(new Request("view",first.getId(),null,null,null,null,null));
        assertFalse(result.estimate().allowed()); assertNull(result.estimate().fingerprint());
        assertEquals(2,result.versions().size());
        for (var version : result.versions()) {
            assertNull(version.course()); assertNull(version.fingerprint()); assertNull(version.analysis());
        }
        verify(analysis,never()).execute(any()); verify(analysis,never()).report(any(),anyString(),any());
    }
    @Test void closingReservesContentBeforeAnyJudgeAndRestoresIdleAfterFailure() {
        var closer = mock(BatchCloser.class);
        var batchService = new DefaultBatchService(batches, plans, reports, active, null, courses, null, null,
                null, null, null, null, ledger, closer, null, new DefaultBatchReportBuilder(),
                TandaFixture.COURSE_PATH, null, null, null);
        when(closer.close(any(), any(), any())).thenAnswer(i -> {
            assertEquals(BatchStatus.RUNNING, batches.load(first.getId()).orElseThrow().getStatus());
            assertFalse(estimate(first.getId()).allowed(), "No successor may be created during a closing");
            throw new IllegalStateException("simulated judge failure");
        });
        assertThrows(IllegalStateException.class, () -> batchService.close(new BatchCloseRequest(Map.of(),0)));
        assertEquals(BatchStatus.OPEN, first.getStatus()); assertEquals(8, first.getContentVersion());
        assertEquals(1, batches.list().size()); assertTrue(courses.written.isEmpty());
    }
    private String translation(CorrectionBatch batch) {
        return TandaFixture.exercise(ledger.composedCourse(batch, courses.course), quiz).getTranslation();
    }
}
