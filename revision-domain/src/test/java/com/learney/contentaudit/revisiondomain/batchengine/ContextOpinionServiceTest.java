package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.*;
import java.util.*;
import org.junit.jupiter.api.*;

class ContextOpinionServiceTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final CourseMapper mapper = mock(CourseMapper.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final List<CourseEntity> mapped = new ArrayList<>();
    final List<Integer> caps = new ArrayList<>();
    final List<RevisionProposal> proposals = new ArrayList<>();
    BatchServiceConfig config;
    DefaultContextOpinionService service;
    String quiz, second;
    boolean thematic = true, approved = true, failPaid, beforeMeasured, reuseSecond;
    int evaluatorCount = 1, failed = 0;
    List<Finding> old = List.of(), current = List.of();

    @BeforeEach void setup() {
        var quizzes = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates();
        quiz = quizzes.get(0).getId(); second = quizzes.get(1).getId();
        proposals.add(proposal(quiz)); proposals.add(proposal(second));
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(2); batch.setQueriesSpent(10);
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(reviews.read("batch")).thenAnswer(i -> new BatchReview("batch", 2, batch.getContentVersion(), null,
                List.of(), proposals, List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), same(course))).thenReturn(course);
        when(mapper.map(any())).thenAnswer(i -> { mapped.add(i.getArgument(0)); return new AuditableCourse(List.of()); });
        var descriptor = new AnalyzerDescriptor(); descriptor.setName("judge"); descriptor.setDisplayName("Juez"); descriptor.setCost(AnalysisCost.PAID_MODEL);
        when(catalog.list()).thenReturn(List.of(descriptor));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            var selection = i.getArgument(1, AnalyzerRunSelection.class);
            int cap = selection.getPolicies().get("judge").getMaxNewEvaluations(); caps.add(cap);
            if (cap > 0 && failPaid) throw new IllegalStateException("provider stopped");
            var r = TandaFixture.report(mapped.get(mapped.size()-1), cap > 0 ? current : old, Map.of(), List.of("judge"));
            score(r.getRoot(), cap > 0 && failed == 0 || cap == 0 && beforeMeasured);
            var c = new JudgeCoverageDiagnosis(); c.setJudgeName("judge"); c.setUnit(thematic ? AuditTarget.KNOWLEDGE : AuditTarget.QUIZ);
            var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(approved); standing.setJudgeVersion("v1"); c.setStanding(standing);
            int units = thematic ? 1 : count(mapped.get(mapped.size()-1));
            c.setCost(new JudgeCostEstimate(units, units * (reuseSecond ? 1 : evaluatorCount), 0, false, 0, 1, 0));
            var es = new ArrayList<EvaluatorCoverage>();
            for (int e = 0; e < evaluatorCount; e++) {
                int needed = reuseSecond && e > 0 ? 0 : units;
                int evaluated = cap > 0 && failed == 0 && needed > 0 ? 1 : 0;
                es.add(new EvaluatorCoverage("e"+e,
                    new EvaluationCoverage(units, evaluated, evaluated, needed == 0 ? units : 0,
                        cap == 0 ? needed : 0, cap > 0 && needed > 0 ? failed : 0), "v1"));
            }
            c.setEvaluators(es); ((DefaultCourseDiagnoses)r.getRoot().getDiagnoses()).setJudgeCoverages(List.of(c)); return r;
        });
        config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository); config.setCoursePath(TandaFixture.COURSE_PATH);
        config.setCourseMapper(mapper); config.setAuditEngine(engine); config.setAnalyzerCatalog(catalog);
        service = new DefaultContextOpinionService(config, ledger, reviews, c -> "content");
    }
    RevisionProposal proposal(String id) {
        var p = new RevisionProposal(); p.setProposalId("p-"+id); p.setNodeTarget(AuditTarget.QUIZ); p.setNodeId(id);
        var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, id, TandaFixture.exercise(course,id), null);
        p.setElementBefore(snapshot); p.setElementAfter(snapshot); return p;
    }
    int topicCount() { return course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().get(0).getQuizTemplates().size(); }
    static int count(CourseEntity c) { return c.getRoot().getMilestones().stream().flatMap(m -> m.getTopics().stream()).flatMap(t -> t.getKnowledges().stream()).mapToInt(k -> k.getQuizTemplates().size()).sum(); }
    void score(AuditNode n, boolean measured) { n.getScores().put("judge", measured ? 1.0 : Double.NaN); n.getChildren().forEach(c -> score(c,measured)); }
    Request estimate(boolean all) { return new Request("estimate","batch",Map.of("judge", List.of(quiz,second,quiz)),all,null,null,null,null,null); }
    Request measure(State e, int cap) { return new Request("measure","batch",e.selection(),e.wholeTopic(),"op",5,e.fingerprint(),cap,"human"); }
    @Test void estimateDeduplicatesAndNeverPaysOrWrites() {
        var e = service.execute(estimate(false)); assertEquals(1,e.pendingQueries()); assertEquals(2,e.selection().get("judge").size());
        assertTrue(caps.stream().allMatch(c -> c == 0)); verify(store,never()).save(any()); verify(store,never()).saveIfVersion(any(),anyInt(),any());
        assertTrue(mapped.stream().allMatch(c -> count(c) == topicCount()));
    }
    @Test void selectedQuizScopeAndTopicScopeHaveDifferentCostButThematicAlwaysSeesWholeTopic() {
        thematic = false; var e = service.execute(new Request("estimate","batch",Map.of("judge",List.of(quiz)),false,null,null,null,null,null));
        assertEquals(1,e.pendingQueries()); assertEquals(topicCount(),e.judges().get(0).topicPendingQueries()); assertTrue(mapped.stream().anyMatch(c -> count(c)==1));
        thematic = true; mapped.clear(); e = service.execute(new Request("estimate","batch",Map.of("judge",List.of(quiz)),false,null,null,null,null,null));
        assertEquals(e.judges().get(0).selectedPendingQueries(),e.judges().get(0).topicPendingQueries()); assertTrue(mapped.stream().allMatch(c -> count(c)==topicCount()));
    }
    @Test void measurementSavesResultsWithoutClosingOrEditingAndRepetitionNeverPaysAgain() {
        var e = service.execute(estimate(true)); var request = measure(e,1); var state = service.execute(request);
        var run = state.runs().get(0); assertEquals("DONE",run.status()); assertEquals(2,run.results().size()); assertEquals(1,run.spent()); assertEquals(11,batch.getQueriesSpent());
        assertEquals(5,batch.getContentVersion()); assertEquals(2,batch.getCorrectionRevision()); assertEquals(BatchStatus.CLOSED,batch.getStatus()); assertTrue(batch.getClosings().isEmpty());
        int calls = caps.size(); batch.setContentVersion(6); service.execute(request); assertEquals(calls,caps.size());
        verify(store).saveIfVersion(batch,5,BatchStatus.CLOSED); verify(repository,never()).save(any(),any());
        assertThrows(IllegalArgumentException.class,() -> service.execute(new Request("measure","batch",e.selection(),true,"op",5,e.fingerprint(),2,"human")));
    }
    @Test void thematicSelectedOutputsKeepFullContextForFreshness() {
        var e = service.execute(new Request("estimate","batch",Map.of("judge",List.of(quiz)),false,null,null,null,null,null));
        var run = service.execute(measure(e,1)).runs().get(0); assertEquals(1,run.results().size()); assertEquals(2,run.proposals().size());
    }
    @Test void capCountsEveryEvaluatorAndFailures() {
        evaluatorCount = 2; var e = service.execute(estimate(true)); var run = service.execute(measure(e,1)).runs().get(0);
        assertEquals(0,run.spent()); assertEquals("PARTIAL",run.status()); assertTrue(caps.stream().allMatch(c -> c == 0));
        batch.setOpinionRuns(List.of()); failed=1; e=service.execute(estimate(true)); run=service.execute(measure(e,2)).runs().get(0);
        assertEquals(2,run.spent()); assertEquals("PARTIAL",run.status()); assertTrue(caps.stream().allMatch(c -> c <= 1));
    }
    @Test void oneCachedEvaluatorDoesNotMakeOnePendingConsultationUnaffordable() {
        evaluatorCount=2; reuseSecond=true; var e=service.execute(estimate(true)); assertEquals(1,e.pendingQueries());
        var run=service.execute(measure(e,1)).runs().get(0); assertEquals(1,run.spent()); assertEquals("DONE",run.status());
    }
    @Test void failureRetainsReservationKnownSpendAndRestoresBatchStatus() {
        var e=service.execute(estimate(true)); failPaid=true; assertThrows(IllegalStateException.class,()->service.execute(measure(e,2)));
        assertEquals("FAILED",batch.getOpinionRuns().get(0).status()); assertEquals(10,batch.getQueriesSpent()); assertEquals(BatchStatus.CLOSED,batch.getStatus());
    }
    Finding finding(String rule, String field) {
        var f=new Finding(); f.setAnalyzer("judge"); f.setRule(rule); f.setNode(new FindingNodeRef(AuditTarget.QUIZ,quiz,quiz));
        f.setEvidence(new FindingEvidence(List.of(new EvidencePart(field,"text")),"objection",List.of())); return f;
    }
    @Test void savesFullEvidenceAndSeparatesFixedPersistentAndNewWithoutInventingABefore() {
        beforeMeasured=true; Finding fixed=finding("fixed","Ejercicio"), persistent=finding("persistent","Miniteoría"), gained=finding("new","Traducción");
        old=List.of(fixed,persistent); current=List.of(finding("persistent","Miniteoría"),gained); var e=service.execute(estimate(true));
        var result=service.execute(measure(e,1)).runs().get(0).results().stream().filter(r -> r.outcome().getNodeId().equals(quiz)).findFirst().orElseThrow().outcome();
        assertEquals(List.of(fixed),result.getFixedFindings()); assertEquals(List.of(persistent),result.getPersistentFindings()); assertEquals(List.of(gained),result.getNewFindings());
        assertEquals("Traducción",result.getNewFindings().get(0).getEvidence().getExamined().get(0).getLabel());
    }
    @Test void staleContentChangedScopeBlockedExamAndUnknownJudgeStopBeforePaidWork() {
        var e=service.execute(estimate(true)); batch.setContentVersion(6); assertThrows(IllegalArgumentException.class,()->service.execute(measure(e,1))); batch.setContentVersion(5);
        assertThrows(IllegalArgumentException.class,()->service.execute(new Request("measure","batch",e.selection(),false,"op",5,e.fingerprint(),1,"human")));
        approved=false; assertFalse(service.execute(estimate(true)).allowed()); assertThrows(IllegalArgumentException.class,()->service.execute(measure(service.execute(estimate(true)),1)));
        assertThrows(IllegalArgumentException.class,()->service.execute(new Request("estimate","batch",Map.of("foreign",List.of(quiz)),true,null,null,null,null,null)));
        assertTrue(caps.stream().allMatch(c -> c==0)); verify(store,never()).saveIfVersion(any(),anyInt(),any());
    }

    @Test void correctionDidacticRecoveryKeepsPriorLearningAndCurriculumWithoutRepeatingExercises() {
        assertCorrectionCurriculum("a1-didactic");
    }
    @Test void correctionNaturalnessRecoveryKeepsPriorLearningAndCurriculumWithoutRepeatingExercises() {
        assertCorrectionCurriculum("naturalness");
    }
    private AuditReport correctionReport(String analyzer) {
        var canonical = DefaultCurrentAnalysisService.cut(course, batch.getLevel());
        var report = TandaFixture.report(canonical, List.of(), Map.of(), List.of(analyzer));
        com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(report.getRoot()).values()
                .forEach(n -> n.getScores().put(analyzer, 1.0));
        var active = mock(CurrentAnalysisService.class);
        when(active.supportedAnalyzers()).thenReturn(Set.of(analyzer));
        when(active.report(any(), eq("correction"), any())).thenReturn(report);
        config.setCurrentAnalysisService(active);
        return report;
    }
    private void assertCorrectionCurriculum(String analyzer) {
        var report = correctionReport(analyzer);
        var index = com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(report.getRoot());
        index.get(TandaFixture.CHILD).getScores().remove(analyzer);
        var descriptor = new AnalyzerDescriptor(); descriptor.setName(analyzer); descriptor.setCost(AnalysisCost.PAID_MODEL);
        when(catalog.list()).thenReturn(List.of(descriptor));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            assertEquals(0, i.getArgument(1, AnalyzerRunSelection.class).getPolicies().get(analyzer).getMaxNewEvaluations());
            var r = TandaFixture.report(mapped.get(mapped.size()-1), List.of(), Map.of(), List.of(analyzer));
            var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName(analyzer); coverage.setUnit(AuditTarget.QUIZ);
            var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
            coverage.setStanding(standing); coverage.setCost(new JudgeCostEstimate(1, 1, 0, false, 0, 1, 0));
            coverage.setEvaluators(List.of(new EvaluatorCoverage(analyzer, new EvaluationCoverage(1, 0, 0, 0, 1, 0), "v1")));
            ((DefaultCourseDiagnoses) r.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
            return r;
        });
        var estimate = service.execute(new Request("estimate", "batch", null, false, null, null, null, null, null, "correction"));
        assertEquals(List.of(TandaFixture.CHILD), estimate.selection().get(analyzer));
        var builder = new com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder(
                new com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer(
                        com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter.create()),
                new com.learney.contentaudit.auditdomain.finishedsentence.DefaultFinishedSentenceReader());
        var expected = builder.build(index.get(TandaFixture.CHILD)).getContent();
        assertFalse("[]".equals(expected.get("documentedPriorLearning")));
        for (var cut : mapped) {
            assertEquals(1, count(cut), "Only the missing exercise may reach the judge");
            var cutReport = TandaFixture.report(cut, List.of(), Map.of(), List.of(analyzer));
            var target = com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(cutReport.getRoot()).get(TandaFixture.CHILD);
            assertEquals(expected, builder.build(target).getContent(), "The judge must receive the same input as full Correction 1");
            assertEquals(1, cut.getRoot().getMilestones().size(), "Other levels must remain outside A1");
        }
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }
    @Test void correctionCompletionDoesNotRequestASavedNaturalnessAbstentionAgain() {
        var report = correctionReport("naturalness");
        var target = com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(report.getRoot()).get(TandaFixture.CHILD);
        target.getScores().remove("naturalness"); target.getUnevaluatedBy().add("naturalness");
        target.getMetadata().put("naturalnessInputContract", "english-quality-input-v2");
        target.getMetadata().put("naturalnessVersion", "v1");
        target.getMetadata().put("naturalnessVerdict", Map.of("assessments", List.of(Map.of("status", "NOT_APPLICABLE"))));
        var estimate = service.execute(new Request("estimate", "batch", null, false, null, null, null, null, null, "correction"));
        assertTrue(estimate.selection().isEmpty());
        assertEquals(0, estimate.pendingQueries());
        verify(engine, never()).runAudit(any(), any(AnalyzerRunSelection.class));
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }
}
