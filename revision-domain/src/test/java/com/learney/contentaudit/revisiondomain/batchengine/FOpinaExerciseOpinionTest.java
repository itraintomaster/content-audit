package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.revisiondomain.*;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.Request;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * F-OPINA-R004 and R005: an opinion asked for one exercise enters the current analysis like Completar's; what left no
 * verdict never enters nor hides one.
 */
class FOpinaExerciseOpinionTest {
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final CourseMapper mapper = mock(CourseMapper.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final AuditReportStore audits = mock(AuditReportStore.class);
    final CurrentAnalysisService analysis = mock(CurrentAnalysisService.class);
    final List<CourseEntity> mapped = new ArrayList<>();
    final List<CurrentAnalysisService.Request> writes = new ArrayList<>();
    String judge = "judge";
    int compatible = 1, replaces = 0;
    Boolean runToday = true;
    DefaultContextOpinionService service;

    @BeforeEach void setup() {
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(1);
        when(store.load("batch")).thenReturn(Optional.of(batch));
        var proposal = new RevisionProposal(); proposal.setProposalId("p-reside"); proposal.setNodeTarget(AuditTarget.QUIZ);
        proposal.setNodeId(TandaFixture.RESIDE);
        var snapshot = new CourseElementSnapshot(AuditTarget.QUIZ, TandaFixture.RESIDE, TandaFixture.exercise(course, TandaFixture.RESIDE), null);
        proposal.setElementBefore(snapshot); proposal.setElementAfter(snapshot);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(proposal), List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), any())).thenReturn(course);
        when(mapper.map(any())).thenAnswer(i -> { mapped.add(i.getArgument(0)); return new AuditableCourse(List.of()); });
        when(audits.save(any())).thenReturn("audit-of-the-opinion");
        when(catalog.list()).thenAnswer(i -> List.of(descriptor(judge)));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            int cap = i.getArgument(1, AnalyzerRunSelection.class).getPolicies().get(judge).getMaxNewEvaluations();
            var r = TandaFixture.report(mapped.get(mapped.size() - 1), List.of(), Map.of(), List.of(judge));
            com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(r.getRoot()).values()
                    .forEach(n -> n.getScores().put(judge, cap > 0 ? 1.0 : Double.NaN));
            var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName(judge); coverage.setUnit(AuditTarget.QUIZ);
            var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
            coverage.setStanding(standing); coverage.setCost(new JudgeCostEstimate(1, 1, 0, false, 0, 1, 0));
            coverage.setEvaluators(List.of(new EvaluatorCoverage(judge, new EvaluationCoverage(1, cap > 0 ? 1 : 0,
                    cap > 0 ? 1 : 0, 0, cap > 0 ? 0 : 1, 0), "v1")));
            ((DefaultCourseDiagnoses) r.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
            return r;
        });
        when(analysis.supportedAnalyzers()).thenAnswer(i -> Set.of(judge));
        when(analysis.execute(any())).thenAnswer(i -> {
            CurrentAnalysisService.Request request = i.getArgument(0);
            if (!"read".equals(request.action())) writes.add(request);
            return view();
        });
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAnalyzerCatalog(catalog); config.setAuditReportStore(audits); config.setCurrentAnalysisService(analysis);
        service = new DefaultContextOpinionService(config, ledger, reviews, c -> "content");
    }

    static AnalyzerDescriptor descriptor(String name) {
        var descriptor = new AnalyzerDescriptor(); descriptor.setName(name); descriptor.setDisplayName(name);
        descriptor.setCost(AnalysisCost.PAID_MODEL); return descriptor;
    }

    /** The analysis read: the supplement the opinion recorded, fitting in the correction as the test says. */
    CurrentAnalysisService.View view() {
        var loose = BatchBook.list(batch.getSelectiveRuns()).stream().map(run -> new CurrentAnalysisService.LooseRun(
                run.id(), run.analyzer(), run.auditId(), run.status(), 5, run.runtime(), "v1", Instant.EPOCH, 1, compatible, 0,
                List.of(new CurrentAnalysisService.Fit("correction", 1, compatible, 1 - compatible, replaces, 0,
                        compatible == 0 ? List.of() : List.of(TandaFixture.RESIDE), List.of(), List.of(), List.of(), null,
                        0, 0, 0, List.of(), compatible - replaces, 0, 0, 0, replaces, compatible == 0 ? null : runToday, true)),
                run.analyzer(), run.sourceRunId())).toList();
        var state = new CurrentAnalysisService.State("correction", "Corrección 1", "fingerprint-of-correction", null,
                Map.of(), Map.of(), List.of(), Map.of(), "A1");
        return new CurrentAnalysisService.View("batch", 5, 3, List.of(state), loose, List.of(), List.of(judge));
    }

    ContextOpinion.State ask() {
        var estimate = service.execute(new Request("estimate", "batch", Map.of(judge, List.of(TandaFixture.RESIDE)), false,
                null, null, null, null, null));
        return service.execute(new Request("measure", "batch", estimate.selection(), false, "op-reside", 5,
                estimate.fingerprint(), 1, "José"));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R005")
    void aVerdictAskedForOneExerciseEntersTheCurrentAnalysis() {
        ask();

        verify(analysis).seed(same(batch), any());
        assertEquals(1, batch.getSelectiveRuns().size(), "The opinion is recorded as a measurement of the analysis");
        var run = batch.getSelectiveRuns().get(0);
        assertEquals(List.of(TandaFixture.RESIDE), run.quizIds());
        assertEquals("op-reside", run.sourceRunId());
        assertEquals(1, writes.size());
        var write = writes.get(0);
        assertEquals("integrate", write.action());
        assertEquals("correction", write.state());
        assertEquals(run.id(), write.runId());
        assertEquals(3, write.expectedRevision());
        assertEquals("fingerprint-of-correction", write.contentFingerprint());
        assertEquals("José", write.reviewer());
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R005")
    void itReplacesAnEarlierOpinionOnlyWithTodaysVersion() {
        replaces = 1; runToday = true;
        ask();
        assertEquals(List.of("replace"), writes.stream().map(CurrentAnalysisService.Request::action).toList());

        writes.clear(); batch.setOpinionRuns(List.of()); batch.setSelectiveRuns(List.of()); runToday = false;
        var estimate = service.execute(new Request("estimate", "batch", Map.of(judge, List.of(TandaFixture.RESIDE)), false,
                null, null, null, null, null));
        service.execute(new Request("measure", "batch", estimate.selection(), false, "op-reside-2", 5, estimate.fingerprint(), 1, "José"));
        assertTrue(writes.isEmpty(), "Not of today's version: it stays loose, to decide in the dialog");
        assertEquals(1, batch.getSelectiveRuns().size());
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R004")
    void aRunThatLeftNoVerdictIsNeverIntegrated() {
        compatible = 0;
        var state = ask();

        assertTrue(writes.isEmpty(), "Nothing that fits never becomes an opinion nor hides one");
        assertEquals("DONE", state.runs().get(0).status());
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R005")
    void aJudgeTheAnalysisDoesNotComposeStaysInItsRun() {
        when(analysis.supportedAnalyzers()).thenReturn(Set.of("another-judge"));
        var state = ask();

        assertTrue(BatchBook.list(batch.getSelectiveRuns()).isEmpty());
        assertTrue(writes.isEmpty());
        assertEquals(1, state.runs().get(0).results().size(), "Its opinion stays in its run, shown as until now");
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R005")
    void anOpenBatchEntersOnlyWithALaterCorrection() {
        batch.setStatus(BatchStatus.OPEN);
        ask();
        verify(analysis, never()).seed(any(), any());
        assertTrue(BatchBook.list(batch.getSelectiveRuns()).isEmpty());
        assertTrue(writes.isEmpty());

        when(store.hasSuccessor("batch")).thenReturn(true); batch.setOpinionRuns(List.of());
        var estimate = service.execute(new Request("estimate", "batch", Map.of(judge, List.of(TandaFixture.RESIDE)), false,
                null, null, null, null, null));
        service.execute(new Request("measure", "batch", estimate.selection(), false, "op-reside-2", 5, estimate.fingerprint(), 1, "José"));
        assertEquals(List.of("integrate"), writes.stream().map(CurrentAnalysisService.Request::action).toList());
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R005")
    void aLooseOpinionReadsWhatTheAnalysisReads() {
        judge = "naturalness";
        ask();

        assertFalse(mapped.isEmpty());
        for (var cut : mapped) {
            assertEquals(List.of("A1"), cut.getRoot().getMilestones().stream().map(MilestoneEntity::getLabel).toList(),
                    "Only the level of the correction");
            var knowledges = cut.getRoot().getMilestones().get(0).getTopics().stream().flatMap(t -> t.getKnowledges().stream()).toList();
            assertTrue(knowledges.size() > 1, "The curriculum before the exercise is judge input, as in the analysis");
            assertEquals(List.of(TandaFixture.RESIDE), knowledges.stream().flatMap(k -> k.getQuizTemplates().stream())
                    .map(QuizTemplateEntity::getId).toList());
        }
    }
}
