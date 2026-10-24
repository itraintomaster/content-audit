package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorrectionEvolutionPublicationTest {
    @Test void publishesInheritedAndNewChangesTogetherWithoutPublishingThePredecessor() {
        var p = new Publishing(); var parent = p.closedBatch(); p.batches.save(parent);
        var child = successor(p, parent);
        var after = BatchBook.copy(TandaFixture.exercise(p.ledger().composedCourse(child, p.start), MesaFixture.VEGETABLES));
        after.setTranslation("Me encantan las verduras.");
        p.record(child, TandaFixture.change("c2-translation", BatchChangeOrigin.JOSE, "manual", List.of(),
                TandaFixture.step(after, ExercisePart.TRANSLATION)));
        p.close(child); p.batches.save(child);
        var result = p.publisher().publish(child, new BatchPublishRequest("José", null, List.of()), child.getEvolution().baseCourse());
        assertEquals(BatchStatus.PUBLISHED, result.getStatus()); assertEquals(BatchStatus.CLOSED, parent.getStatus());
        var published = TandaFixture.exercise(p.courses.course, MesaFixture.VEGETABLES);
        assertEquals(List.of("I love vegetables."), published.getSentences());
        assertEquals("Me encantan las verduras.", published.getTranslation());
        assertEquals(1, p.courses.written.size());
        assertTrue(parent.getChanges().stream().noneMatch(c -> "c2-translation".equals(c.getChangeId())));
        assertThrows(IllegalStateException.class, () -> p.publisher().publish(parent,
                new BatchPublishRequest("José",null,List.of()),p.start));
    }
    @Test void refusesToOverwriteAChangedPublishedCourseWithAnOldEvolutionBase() {
        var p = new Publishing(); var parent = p.closedBatch(); p.batches.save(parent);
        var child = successor(p, parent); p.close(child); p.batches.save(child);
        p.courses.course = CorrectionEvolution.copyCourse(p.start);
        TandaFixture.exercise(p.courses.course, MesaFixture.VEGETABLES).setTranslation("Otra publicación");
        var error = assertThrows(BatchNotPublishableException.class, () -> p.publisher().publish(child,
                new BatchPublishRequest("José",null,List.of()),child.getEvolution().baseCourse()));
        assertTrue(error.getMessage().contains("curso publicado cambió")); assertTrue(p.courses.written.isEmpty());
    }
    @Test void directPublicationKeepsACompleteBaseCheckpointForFutureHistoricalViews() {
        var p = new Publishing(); var root = p.closedBatch(); p.batches.save(root);
        var original = CorrectionEvolution.copyCourse(p.start);
        p.publisher().publish(root,new BatchPublishRequest("José",null,List.of()),p.start);
        assertEquals(original,root.getBaseCourseSnapshot());
        TandaFixture.exercise(p.courses.course,MesaFixture.VEGETABLES).setTranslation("Later production");
        var view = evolution(p).execute(new CorrectionEvolution.Request("view",root.getId(),null,null,null,null,null));
        var base = view.versions().get(0);
        assertNotNull(base.course());
        assertEquals(TandaFixture.exercise(original,MesaFixture.VEGETABLES).getTranslation(),
                TandaFixture.exercise(base.course(),MesaFixture.VEGETABLES).getTranslation());
        assertEquals(List.of("I love vegetables."),TandaFixture.exercise(view.versions().get(1).course(),MesaFixture.VEGETABLES).getSentences());
    }
    @Test void rejectingOneInheritedPartPublishesTheOtherWithTheCurrentAnalysisGate() {
        var p = new Publishing(); var parent = p.closedBatch();
        var translated = BatchBook.copy(TandaFixture.exercise(p.ledger().composedCourse(parent,p.start),MesaFixture.VEGETABLES));
        translated.setTranslation("Otra traducción");
        p.record(parent,TandaFixture.change("inherited-translation",BatchChangeOrigin.JOSE,"manual",List.of(),
                TandaFixture.step(translated,ExercisePart.TRANSLATION))); p.close(parent); p.batches.save(parent);
        var child = successor(p,parent);
        var review = p.review(); var item = review.read(child.getId()).items().stream()
                .filter(i -> "inherited-translation".equals(i.changeId())).findFirst().orElseThrow();
        p.batches.saveReview(new BatchReviewDecision(child.getId(),List.of(new BatchReviewDecision.Selection(item.changeId(),item.step(),
                item.before(),item.after())),BatchReviewDecision.Status.REJECTED,"José",java.time.Instant.now()));
        p.close(child); p.batches.save(child);
        var current = mock(CurrentAnalysisService.class); var report = p.measure(p.start);
        when(current.comparisonReports(any(),any(),any())).thenReturn(new CurrentAnalysisService.ComparisonReports(report,report,report));
        when(current.report(any(),eq("correction"),any())).thenAnswer(i -> p.measure(i.getArgument(2)));
        p.publisher().withCurrentAnalysis(current).publish(child,new BatchPublishRequest("José",null,List.of()),child.getEvolution().baseCourse());
        var written = TandaFixture.exercise(p.courses.course,MesaFixture.VEGETABLES);
        assertEquals(List.of("I love vegetables."),written.getSentences());
        assertEquals(TandaFixture.exercise(p.start,MesaFixture.VEGETABLES).getTranslation(),written.getTranslation());
    }
    private CorrectionBatch successor(Publishing p, CorrectionBatch parent) {
        var service = evolution(p);
        var estimate = service.execute(new CorrectionEvolution.Request("estimate",parent.getId(),null,null,null,null,null)).estimate();
        var response = service.execute(new CorrectionEvolution.Request("create",parent.getId(),"c2",estimate.contentVersion(),
                estimate.fingerprint(),estimate.analysisRevision(),"José"));
        var child = p.batches.load(response.result().newBatchId()).orElseThrow();
        assertEquals(2,p.reports.load(child.getAnalysisSeedAuditId()).orElseThrow().getRoot().getChildren().size(),"Successor seeds preserve every level for closing and publication");
        return child;
    }
    private CorrectionEvolutionService evolution(Publishing p) {
        var config = new BatchServiceConfig(); config.setBatchStore(p.batches); config.setCourseRepository(p.courses);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setArtifactStore(p.artifacts); config.setRefinementPlanStore(p.plans);
        config.setAuditReportStore(p.reports);
        var refiner = mock(com.learney.contentaudit.refinerdomain.RefinerEngine.class);
        when(refiner.plan(any(), anyString())).thenAnswer(i -> TandaFixture.plan()); config.setRefinerEngine(refiner);
        var analysis = mock(CurrentAnalysisService.class); config.setCurrentAnalysisService(analysis);
        when(analysis.report(any(), eq("correction"), any())).thenAnswer(i -> p.measure(i.getArgument(2)));
        when(analysis.execute(any())).thenAnswer(i -> new CurrentAnalysisService.View(
                i.getArgument(0,CurrentAnalysisService.Request.class).batchId(),1,0,List.of(),List.of(),List.of(),List.of()));
        when(analysis.comparisonView(any())).thenAnswer(i -> analysis.execute(new CurrentAnalysisService.Request(
                "read",i.getArgument(0,CorrectionBatch.class).getId(),null,null,null,null,null,null,null,null)));
        return new DefaultCorrectionEvolutionServiceFactory().create(config);
    }
}
