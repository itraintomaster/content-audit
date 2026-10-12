package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.choicequality.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.findingengine.DefaultContextNumbersCalculator;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.contentfingerprint.Sha256ContentFingerprinter;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentAnalysisServiceTest {
    static final String NAME = "multiple-choice-quality";
    final CourseEntity course = TandaFixture.course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore store = mock(BatchStore.class);
    final AuditReportStore audits = mock(AuditReportStore.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final List<AnalysisSelection> history = new ArrayList<>();
    final Map<String, AuditReport> saved = new HashMap<>();
    final QuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create());
    final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
    final ChoiceQualityAnalyzerProvider provider = new ChoiceQualityAnalyzerProvider(null, null, null, renderer);
    final AnalysisComposer composer = new DefaultAnalysisComposer(catalog, new IScoreAggregator(),
            new DefaultContextNumbersCalculator(), renderer, new Sha256ContentFingerprinter());
    DefaultCurrentAnalysisService service;

    @BeforeEach void setup() {
        when(catalog.list()).thenReturn(List.of(provider.describe()));
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(6); batch.setCorrectionRevision(1);
        batch.setClosings(List.of(new BatchClosing()));
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(store.analysisSelections("batch")).thenAnswer(i -> List.copyOf(history));
        doAnswer(i -> { history.add(i.getArgument(0)); return null; }).when(store).saveAnalysisSelection(any());
        when(audits.load(anyString())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
        when(repository.load(any())).thenReturn(course);
        when(ledger.composedCourse(any(), any())).thenReturn(course);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 6, null, List.of(), List.of(), List.of(), List.of(), List.of()));
        saved.put(batch.getBaselineAuditId(), report(false, false, "v1"));
        when(engine.runAudit(any(), any())).thenAnswer(i -> report(false, false, "v1"));
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setAuditReportStore(audits);
        config.setCourseRepository(repository); config.setCoursePath(TandaFixture.COURSE_PATH); config.setAuditEngine(engine);
        var mapper = mock(CourseMapper.class); when(mapper.map(any())).thenReturn(new AuditableCourse(List.of()));
        config.setCourseMapper(mapper); config.setAnalyzerCatalog(catalog);
        service = new DefaultCurrentAnalysisService(config, ledger, composer, c -> "content", renderer, reviews);
        run("r1", true, "v1");
    }

    AuditReport report(boolean evaluated, boolean error, String version) {
        var r = TandaFixture.report(course, List.of(), Map.of(), List.of());
        var n = DefaultAnalysisComposer.index(r.getRoot()).get(TandaFixture.CHILD);
        n.setDiagnoses(new DefaultQuizDiagnoses());
        n.getScores().put("other", .8);
        if (evaluated) {
            n.getScores().put(NAME, error ? .3 : 1.);
            var verdict = new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.PASS, "Be", false, List.of(), "Medido.");
            ((DefaultQuizDiagnoses)n.getDiagnoses()).setChoiceQualityDiagnosis(new ChoiceQualityDiagnosis(verdict, version, true));
            if (error) {
                var f = new Finding(); f.setAnalyzer(NAME); f.setRule("answer-cue"); f.setSeverity(FindingSeverity.HIGH);
                f.setResolution(FindingResolution.PANEL); f.setNode(new FindingNodeRef(AuditTarget.QUIZ, TandaFixture.CHILD, "quiz"));
                n.getFindings().add(f);
            }
        }
        return r;
    }
    void run(String id, boolean error, String version) {
        saved.put(id, report(true, error, version));
        var runs = new ArrayList<>(BatchBook.list(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run(id, "analyze", NAME, null, 1, 6, "content", "configured-version",
                "codex-cli / model / medium", id, null, List.of(), List.of(), 1, 1, 1, 1, 1, 0,
                "DONE", "human", Instant.EPOCH, Instant.EPOCH, null, false, 6, "content", null));
        batch.setSelectiveRuns(runs);
    }
    CurrentAnalysisService.View read() { return service.execute(new CurrentAnalysisService.Request("read", "batch", null, null, null, null, null, null, null, null)); }
    CurrentAnalysisService.Request select(String action, String run, String operation, String removes) {
        return new CurrentAnalysisService.Request(action, "batch", "correction", run, operation, removes,
                history.size(), 6, "content", "human");
    }
    int errors(CurrentAnalysisService.View v) {
        var errors = v.states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow()
                .digest().getRoot().getNumbers().getErrors();
        return errors == null ? 0 : errors.getWithAnyError();
    }
    @Test void looseRunDoesNotCountUntilExplicitlyIntegratedAndRemovalRestoresEverything() {
        assertEquals(0, errors(read()));
        assertEquals("v1", read().looseRuns().get(0).judgeVersion());
        var selected = service.execute(select("integrate", "r1", "s1", null));
        assertEquals(1, errors(selected)); assertEquals(1, history.size());
        assertEquals("human", history.get(0).reviewer());
        assertEquals(0, errors(service.execute(select("remove", "r1", "s2", "s1"))));
        assertEquals(2, history.size()); assertEquals(1, batch.getClosings().size());
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
        verify(engine, atLeastOnce()).runAudit(any(), argThat(s -> s.getPolicies().values().stream().allMatch(p -> p.getMaxNewEvaluations() == 0)));
    }
    @Test void replacementRequiresExplicitActionKeepsOtherAnalyzersAndCanBeUndone() {
        service.execute(select("integrate", "r1", "s1", null)); run("r2", false, "v2");
        var fit = read().looseRuns().get(1).fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow();
        assertEquals(List.of(TandaFixture.CHILD), fit.lostErrors()); assertEquals(1, fit.replaces());
        assertThrows(IllegalArgumentException.class, () -> service.execute(select("integrate", "r2", "bad", null)));
        assertEquals(0, errors(service.execute(select("replace", "r2", "s2", null))));
        var result = service.report(batch, "correction", course);
        assertEquals(.8, DefaultAnalysisComposer.index(result.getRoot()).get(TandaFixture.CHILD).getScores().get("other"));
        assertEquals(1, errors(service.execute(select("remove", "r2", "s3", "s2"))));
    }
    @Test void matchingUsesJudgeInputAndRejectsChangedInstructionButIgnoresLaterExplanation() {
        var quiz = TandaFixture.exercise(course, TandaFixture.CHILD);
        quiz.setMiniTheory("Later explanation");
        assertEquals(1, read().looseRuns().get(0).fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow().compatible());
        quiz.setInstructions("Different instruction");
        assertEquals(0, read().looseRuns().get(0).fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow().compatible());
        assertThrows(IllegalArgumentException.class, () -> service.execute(select("integrate", "r1", "s1", null)));
    }
    @Test void staleConfirmationAndDuplicateOperationCannotOverwriteAnotherSelection() {
        var stale = select("integrate", "r1", "s2", null);
        var once = select("integrate", "r1", "s1", null); service.execute(once); service.execute(once);
        assertEquals(1, history.size());
        assertThrows(IllegalStateException.class, () -> service.execute(stale));
        assertThrows(IllegalArgumentException.class, () -> service.execute(select("replace", "r1", "s1", null)));
    }

    @Test void aNewVersionOfAnotherJudgeRemainsLooseUntilExplicitReplacement() {
        var descriptor = new AnalyzerDescriptor(); descriptor.setName("single-answer");
        descriptor.setFamily(com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily.ERRORS);
        descriptor.setEvaluatedTargets(List.of(AuditTarget.QUIZ));
        when(catalog.list()).thenReturn(List.of(provider.describe(), descriptor));
        var old = report(false, false, "v1");
        var oldNode = DefaultAnalysisComposer.index(old.getRoot()).get(TandaFixture.RESIDE);
        oldNode.getScores().put("single-answer", 1.0);
        RecordedVerdictVersions.stamp(oldNode, "single-answer", "v1");
        when(engine.runAudit(any(), any())).thenReturn(old);
        when(audits.save(any())).thenAnswer(i -> { saved.put("seed", i.getArgument(0)); return "seed"; });
        service.seed(batch, course);
        var newer = report(false, false, "v1");
        var node = DefaultAnalysisComposer.index(newer.getRoot()).get(TandaFixture.RESIDE);
        node.getScores().put("single-answer", .3); RecordedVerdictVersions.stamp(node, "single-answer", "v2");
        var finding = new Finding(); finding.setAnalyzer("single-answer"); finding.setRule("different-valid-answer");
        finding.setResolution(FindingResolution.PANEL); finding.setSeverity(FindingSeverity.HIGH);
        finding.setNode(new FindingNodeRef(AuditTarget.QUIZ, TandaFixture.RESIDE, "quiz")); node.getFindings().add(finding);
        saved.put("single-v2", newer); when(engine.runAudit(any(), any())).thenReturn(newer);
        var run = new SelectiveCorrection.Run("single-v2", "reanalyze", "single-answer", null, 1, 6, "content", "v2",
                "runtime", "single-v2", null, List.of(), List.of(), 1, 1, 1, 1, 1, 0, "DONE", "human", Instant.EPOCH,
                Instant.EPOCH, null, false, 6, "content", null);
        var runs = new ArrayList<>(batch.getSelectiveRuns()); runs.add(run); batch.setSelectiveRuns(runs);
        var before = service.report(batch, "correction", course);
        assertEquals(1., DefaultAnalysisComposer.index(before.getRoot()).get(TandaFixture.RESIDE).getScores().get("single-answer"));
        assertThrows(IllegalArgumentException.class, () -> service.execute(select("integrate", "single-v2", "bad", null)));
        service.execute(select("replace", "single-v2", "replacement", null));
        var after = service.report(batch, "correction", course);
        assertEquals(.3, DefaultAnalysisComposer.index(after.getRoot()).get(TandaFixture.RESIDE).getScores().get("single-answer"));
        assertEquals("v2", DefaultAnalysisComposer.version(DefaultAnalysisComposer.index(after.getRoot()).get(TandaFixture.RESIDE), "single-answer", after));
    }
    @Test void miniTheoryUsesTheEntireTopicWhileDistractorsKeepTheirOwnInput() {
        var old = report(true, false, "v1");
        var source = DefaultAnalysisComposer.index(old.getRoot()).get(TandaFixture.CHILD);
        String theory = composer.fingerprint("mini-theory", source);
        String quality = composer.fingerprint(NAME, source);
        ((AuditableQuiz) source.getEntity()).setMiniTheory("New theory");
        assertNotEquals(theory, composer.fingerprint("mini-theory", source));
        assertEquals(quality, composer.fingerprint(NAME, source));
    }

    @Test void replacementReportsTheActualNewVersionAndRetainsItsSavedStudentView() {
        service.execute(select("integrate", "r1", "s1", null));
        run("r2", false, "v2");
        var sourceQuiz = (AuditableQuiz) DefaultAnalysisComposer.index(saved.get("r2").getRoot()).get(TandaFixture.CHILD).getEntity();
        sourceQuiz.setMiniTheory("The explanation saved with v2");
        var selected = service.execute(select("replace", "r2", "s2", null));
        var state = selected.states().stream().filter(x -> x.id().equals("correction")).findFirst().orElseThrow();
        var origin = (Map<?,?>) state.provenance().get(TandaFixture.CHILD).get(NAME);
        assertEquals("v2", origin.get("judgeVersion"));
        assertEquals("The explanation saved with v2", ((com.learney.contentaudit.auditdomain.studentview.QuizStudentView) origin.get("studentView")).getMiniTheory());
        assertFalse(state.comparableAnalyzers().get(TandaFixture.CHILD).contains(NAME));
    }

    @Test void aRemovedMarkWarnsOnlyApprovalsThatActuallyCameFromThatJudge() {
        var task = new com.learney.contentaudit.refinerdomain.RefinementTask(); task.setId("quality-task");
        var origin = new com.learney.contentaudit.refinerdomain.TaskOrigin();
        var finding = new Finding(); finding.setAnalyzer(NAME); finding.setRule("answer-cue"); origin.setFinding(finding); task.setOrigin(origin);
        var step = new BatchChangeStep(); step.setQuizId(TandaFixture.CHILD);
        var qualityApproval = new BatchReview.Item("quality-change", step, null, null, "panel", null,
                BatchReviewDecision.Status.APPROVED, "human", Instant.EPOCH, List.of(task.getId()), null);
        var otherApproval = new BatchReview.Item("another-change", step, null, null, "panel", null,
                BatchReviewDecision.Status.APPROVED, "human", Instant.EPOCH, List.of("other-task"), null);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 6, null, List.of(task), List.of(),
                List.of(qualityApproval, otherApproval), List.of(), List.of()));
        service.execute(select("integrate", "r1", "s1", null)); run("r2", false, "v2");
        service.execute(select("replace", "r2", "s2", null));
        var state = read().states().stream().filter(x -> x.id().equals("correction")).findFirst().orElseThrow();
        assertEquals(List.of("quality-change"), state.approvalWarnings());
        assertEquals(BatchReviewDecision.Status.APPROVED, qualityApproval.status());
    }

    @Test void aPartialReplacementKeepsThePreviousVerdictAndVersionOutsideItsMeasuredSlice() {
        var old = report(true, true, "v1");
        var index = DefaultAnalysisComposer.index(old.getRoot());
        var second = index.get(TandaFixture.RESIDE);
        ((AuditableQuiz)second.getEntity()).setMultipleChoice(((AuditableQuiz)index.get(TandaFixture.CHILD).getEntity()).getMultipleChoice());
        second.getScores().put(NAME, .2);
        second.setDiagnoses(new DefaultQuizDiagnoses());
        ((DefaultQuizDiagnoses)second.getDiagnoses()).setChoiceQualityDiagnosis(
                ((QuizDiagnoses)index.get(TandaFixture.CHILD).getDiagnoses()).getChoiceQualityDiagnosis().orElseThrow());
        var partial = composer.compose(old, List.of());
        var fresh = DefaultAnalysisComposer.index(partial.getRoot());
        fresh.get(TandaFixture.CHILD).getScores().put(NAME, 1.0);
        fresh.get(TandaFixture.CHILD).getFindings().removeIf(f -> NAME.equals(f.getAnalyzer()));
        ((DefaultQuizDiagnoses)fresh.get(TandaFixture.CHILD).getDiagnoses()).setChoiceQualityDiagnosis(
                new ChoiceQualityDiagnosis(new ChoiceQualityVerdict(ChoiceQualityVerdict.Status.PASS, "Be", false, List.of(), "new measurement"), "v2", true));
        fresh.get(TandaFixture.RESIDE).getScores().remove(NAME);
        fresh.get(TandaFixture.RESIDE).getUnevaluatedBy().add(NAME);
        var selected = composer.compose(old, List.of(new AnalysisSupplement("partial", "saved", NAME,
                "runtime", Instant.EPOCH, Set.of(TandaFixture.CHILD, TandaFixture.RESIDE), partial)));
        var now = DefaultAnalysisComposer.index(selected.getRoot());
        assertEquals("v2", DefaultAnalysisComposer.version(now.get(TandaFixture.CHILD)));
        assertEquals("v1", DefaultAnalysisComposer.version(now.get(TandaFixture.RESIDE)));
        assertEquals(.2, now.get(TandaFixture.RESIDE).getScores().get(NAME));
        assertEquals(.2, second.getScores().get(NAME), "saved source remains immutable");
    }

    @Test void integrationRetainsTheActualDiagnosisEvenWhenThePrimaryNeverHadOne() {
        var primary=report(false,false,"v1");
        DefaultAnalysisComposer.index(primary.getRoot()).get(TandaFixture.CHILD).setDiagnoses(null);
        var composed=composer.compose(primary,List.of(new AnalysisSupplement("r1","r1",NAME,"runtime",Instant.EPOCH,
                Set.of(TandaFixture.CHILD),saved.get("r1"))));
        var node=DefaultAnalysisComposer.index(composed.getRoot()).get(TandaFixture.CHILD);
        assertEquals("v1",DefaultAnalysisComposer.version(node));
        assertTrue(((QuizDiagnoses)node.getDiagnoses()).getChoiceQualityDiagnosis().isPresent());
        assertNull(DefaultAnalysisComposer.index(primary.getRoot()).get(TandaFixture.CHILD).getDiagnoses());
    }

    @Test void readingWithoutAnyBatchLeavesTheOrdinaryAuditBrowserAvailable() {
        var view=service.execute(new CurrentAnalysisService.Request("read",null,null,null,null,null,null,null,null,null));
        assertNull(view.batchId()); assertTrue(view.states().isEmpty()); assertTrue(view.history().isEmpty());
        verify(engine,never()).runAudit(any(),any());
    }

}
