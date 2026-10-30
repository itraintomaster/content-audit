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
import com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStanding;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    BatchServiceConfig config;

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
        config = new BatchServiceConfig(); config.setBatchStore(store); config.setAuditReportStore(audits);
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
    void run(String id, boolean error, String version) { run(id, error, version, "codex-cli / model / medium"); }
    void run(String id, boolean error, String version, String runtime) {
        saved.put(id, report(true, error, version));
        var runs = new ArrayList<>(BatchBook.list(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run(id, "analyze", NAME, null, 1, 6, "content", "configured-version",
                runtime, id, null, List.of(), List.of(), 1, 1, 1, 1, 1, 0,
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
    @Test void comparisonViewKeepsExactlyTheSelectedStatesWithoutCalculatingLooseRunPreviews() {
        var full = read();
        var summary = service.comparisonView(batch);
        assertFalse(full.looseRuns().isEmpty()); assertTrue(summary.looseRuns().isEmpty());
        assertEquals(full.states(),summary.states()); assertEquals(full.revision(),summary.revision());
        assertTrue(summary.history().isEmpty());
        run("unused",false,"v1");
        service.execute(select("integrate","r1","selected-summary",null));
        assertEquals(read().states(),service.comparisonView(batch).states());
        service.execute(select("remove","r1","removed-summary","selected-summary"));
        assertEquals(read().states(),service.comparisonView(batch).states());
        clearInvocations(audits);
        service.comparisonView(batch);
        verify(audits,never()).load("unused");
        verify(repository,never()).save(any(),any()); verify(store,never()).save(any());
    }
    @Test void aCompleteSeedIsCutForTheUiAndAPartialLegacySeedIsExpandedForPublicationWithZeroQueries() {
        var cut = DefaultCurrentAnalysisService.cut(course,CefrLevel.A1);
        batch.setAnalysisSeedAuditId("seed-scope"); batch.setAnalysisSeedContentVersion(6);
        saved.put("seed-scope",TandaFixture.report(course,List.of(),Map.of(),List.of()));
        var ui = service.report(batch,"correction",cut);
        assertEquals(1,ui.getRoot().getChildren().size());
        assertEquals(2,saved.get("seed-scope").getRoot().getChildren().size(),"a UI read never mutates the complete checkpoint");
        saved.put("seed-scope",TandaFixture.report(cut,List.of(),Map.of(),List.of()));
        var mapped = new java.util.concurrent.atomic.AtomicReference<CourseEntity>();
        when(config.getCourseMapper().map(any())).thenAnswer(i -> { mapped.set(i.getArgument(0)); return new AuditableCourse(List.of()); });
        when(engine.runAudit(any(),any())).thenAnswer(i -> TandaFixture.report(mapped.get(),List.of(),Map.of(),List.of()));
        var publication = service.report(batch,"correction",course);
        assertEquals(2,publication.getRoot().getChildren().size());
        verify(engine,atLeastOnce()).runAudit(any(),argThat(s -> s.getPolicies().values().stream().allMatch(p -> p.getMaxNewEvaluations()==0)));
    }
    @Test void baseAnalysisIsSharedAcrossSuccessorsAndItsLaterSelectionsAreVisibleEverywhere() {
        var child = TandaFixture.batch("child"); child.setCorrectionRevision(2); child.setContentVersion(1);
        child.setEvolution(new CorrectionEvolution.Lineage("batch",6,0,"next","content",course,course,List.of()));
        when(store.load("child")).thenReturn(Optional.of(child)); when(store.list()).thenReturn(List.of(batch,child));
        when(reviews.read("child")).thenReturn(new BatchReview("child",2,1,null,List.of(),List.of(),List.of(),List.of(),List.of()));
        service.execute(new CurrentAnalysisService.Request("integrate","batch","base","r1","base-selection",null,0,6,"content","human"));
        var childView = service.execute(new CurrentAnalysisService.Request("read","child",null,null,null,null,null,null,null,null));
        var base = childView.states().stream().filter(s -> "base".equals(s.id())).findFirst().orElseThrow();
        assertEquals(1,base.digest().getRoot().getNumbers().getErrors().getWithAnyError());
        service.execute(new CurrentAnalysisService.Request("remove","batch","base","r1","base-remove","base-selection",1,6,"content","human"));
        childView = service.execute(new CurrentAnalysisService.Request("read","child",null,null,null,null,null,null,null,null));
        base = childView.states().stream().filter(s -> "base".equals(s.id())).findFirst().orElseThrow();
        var remaining = base.digest().getRoot().getNumbers().getErrors();
        assertEquals(0,remaining == null ? 0 : remaining.getWithAnyError());
        assertThrows(IllegalArgumentException.class,() -> service.execute(new CurrentAnalysisService.Request(
                "integrate","child","base","r1","wrong-base",null,0,1,"content","human")));
    }
    @Test void frozenOpenPredecessorCanIntegrateAndRemoveAnalysisWithoutChangingItsContent() {
        batch.setStatus(BatchStatus.OPEN);
        when(store.hasSuccessor("batch")).thenReturn(true);
        var before = CorrectionEvolution.copyCourse(course);
        assertEquals(1, errors(service.execute(select("integrate", "r1", "historical-analysis", null))));
        assertEquals(0, errors(service.execute(select("remove", "r1", "historical-remove", "historical-analysis"))));
        assertEquals(6, batch.getContentVersion()); assertEquals(before, course);
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
    }

    @Test @Tag("FEAT-ABIER") @Tag("F-ABIER-R004")
    void anOpenBatchIntegratesAndRemovesAnalysisWithoutChangingItsContent() {
        batch.setStatus(BatchStatus.OPEN);
        var before = CorrectionEvolution.copyCourse(course);
        assertEquals(1, errors(service.execute(select("integrate", "r1", "open-analysis", null))));
        assertThrows(IllegalStateException.class, () -> service.execute(new CurrentAnalysisService.Request("integrate", "batch",
                "correction", "r1", "stale", null, 0, 6, "content", "human")), "A selection read before still stops it");
        assertEquals(0, errors(service.execute(select("remove", "r1", "open-remove", "open-analysis"))));
        assertEquals(BatchStatus.OPEN, batch.getStatus());
        assertEquals(6, batch.getContentVersion()); assertEquals(before, course);
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
    }

    @Test @Tag("FEAT-ABIER") @Tag("F-ABIER-R004")
    void aBatchHeldByAnotherOperationOrPublishedDoesNotIntegrate() {
        batch.setStatus(BatchStatus.RUNNING);
        assertEquals("La tanda está tomada por otra operación: integrá cuando termine.", assertThrows(IllegalArgumentException.class,
                () -> service.execute(select("integrate", "r1", "held", null))).getMessage());
        batch.setStatus(BatchStatus.PUBLISHED);
        assertEquals("La tanda está publicada: su análisis ya no cambia.", assertThrows(IllegalArgumentException.class,
                () -> service.execute(select("integrate", "r1", "published", null))).getMessage());
        assertTrue(history.isEmpty());
    }

    @Test void looseRunDoesNotCountUntilExplicitlyIntegratedAndRemovalRestoresEverything() {
        assertEquals(0, errors(read()));
        assertEquals("v1", read().looseRuns().get(0).judgeVersion());
        var preview = read().looseRuns().get(0).fits().get(0);
        assertEquals(1, preview.compatibleWithError());
        assertEquals(0, preview.currentWithError());
        assertEquals(1, preview.resultWithError());
        var selected = service.execute(select("integrate", "r1", "s1", null));
        assertEquals(1, errors(selected)); assertEquals(1, history.size());
        assertEquals("human", history.get(0).reviewer());
        assertEquals(0, errors(service.execute(select("remove", "r1", "s2", "s1"))));
        assertEquals(2, history.size()); assertEquals(1, batch.getClosings().size());
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
        verify(engine, atLeastOnce()).runAudit(any(), argThat(s -> s.getPolicies().values().stream().allMatch(p -> p.getMaxNewEvaluations() == 0)));
    }

    @Test void jointReanalysisSharesOnlyIdenticalInputsAndOnlyFreshVerdictsAtTheRequiredVersion() {
        var base = report(false, false, "v1");
        var fresh = report(true, true, "v2");
        var planned = service.reanalysisScope(NAME, base, fresh, null);
        assertTrue(planned.sharedQuizIds().contains(TandaFixture.CHILD));
        assertTrue(service.reanalysisScope(NAME, base, fresh, "v2").sharedQuizIds().contains(TandaFixture.CHILD));
        assertFalse(service.reanalysisScope(NAME, base, fresh, "v1").sharedQuizIds().contains(TandaFixture.CHILD));
        ((AuditableQuiz)DefaultAnalysisComposer.index(fresh.getRoot()).get(TandaFixture.CHILD).getEntity()).setInstructions("Otra consigna");
        assertTrue(service.reanalysisScope(NAME, base, fresh, "v2").baseOnlyQuizIds().contains(TandaFixture.CHILD));
    }

    @Test void oneQuizConversionKeepsCompatibleSeedOpinionsWithoutRewritingTheSeedOrSelections() {
        String pairJudge = "near-duplicates", quizJudge = "quiz-instruction", siblingId = "unchanged-sibling";
        var descriptors = new ArrayList<AnalyzerDescriptor>();
        for (String name : List.of(pairJudge, quizJudge)) {
            var descriptor = new AnalyzerDescriptor(); descriptor.setName(name); descriptor.setDisplayName(name);
            descriptor.setCost(AnalysisCost.PAID_MODEL); descriptor.setTarget(AuditTarget.QUIZ);
            descriptor.setFamily(com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily.ERRORS);
            descriptor.setEvaluatedTargets(List.of(AuditTarget.QUIZ));
            descriptors.add(descriptor);
        }
        when(catalog.list()).thenReturn(descriptors);
        batch.setAnalyzers(List.of(pairJudge, quizJudge));
        batch.setSelectiveRuns(List.of());
        batch.setContentVersion(8);

        var changedQuiz = TandaFixture.exercise(course, TandaFixture.RESIDE);
        var sibling = QuizTemplateEntities.copyOf(changedQuiz); sibling.setId(siblingId); sibling.setOidId(siblingId);
        var changedTopic = course.getRoot().getMilestones().get(0).getTopics().get(0).getKnowledges().stream()
                .filter(k -> TandaFixture.RESIDE_TOPIC.equals(k.getId())).findFirst().orElseThrow();
        changedTopic.getQuizTemplates().add(sibling);
        List<String> quizIds = List.of(TandaFixture.RESIDE, siblingId, TandaFixture.CHILD);
        var seed = TandaFixture.report(course, List.of(), Map.of(), List.of(pairJudge, quizJudge));
        var seedNodes = DefaultAnalysisComposer.index(seed.getRoot());
        String measuredAt = "2026-10-18T12:00:00Z";
        var originalOrigins = new LinkedHashMap<String, Map<String, Object>>();
        for (String id : quizIds) {
            var node = seedNodes.get(id);
            var origins = new LinkedHashMap<String, Object>();
            for (String judge : List.of(pairJudge, quizJudge)) {
                node.getScores().put(judge, 1.0);
                RecordedVerdictVersions.stamp(node, judge, "saved-v1");
                var origin = Map.<String, Object>of("auditId", "original-measurement", "judgeVersion", "saved-v1",
                        "runtime", "codex-cli / synthetic / medium", "measuredAt", measuredAt,
                        "integrated", false, "fingerprint", composer.fingerprint(judge, node));
                origins.put(judge, origin);
            }
            node.getMetadata().put(DefaultAnalysisComposer.PROVENANCE, origins);
            originalOrigins.put(id, Map.copyOf(origins));
        }
        saved.put("seed-v8", seed);
        batch.setAnalysisSeedAuditId("seed-v8"); batch.setAnalysisSeedContentVersion(8);

        // Only this quiz changes form; its stored sentence and the other quizzes remain identical.
        changedQuiz.setKind("MULTIPLE_CHOICE");
        changedQuiz.setInstructions("Elige la preposición correcta.");
        changedQuiz.setForm(new FormEntity("MULTIPLE_CHOICE", 1, "", "", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "We reside", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, "London with our granddad.", null)),
                new MultipleChoiceEntity("SINGLE", List.of(new MultipleChoiceItemEntity("in", 1, "in"),
                        new MultipleChoiceItemEntity("at", 0, "at"))), null));
        batch.setContentVersion(9);
        var current = TandaFixture.report(course, List.of(), Map.of(
                TandaFixture.RESIDE, List.of(pairJudge, quizJudge), siblingId, List.of(pairJudge, quizJudge),
                TandaFixture.CHILD, List.of(pairJudge, quizJudge)), List.of(pairJudge, quizJudge));
        var currentNodes = DefaultAnalysisComposer.index(current.getRoot());
        assertNotEquals(composer.fingerprint(pairJudge, seedNodes.get(siblingId)),
                composer.fingerprint(pairJudge, currentNodes.get(siblingId)), "pairs depend on the whole topic");
        assertEquals(composer.fingerprint(quizJudge, seedNodes.get(siblingId)),
                composer.fingerprint(quizJudge, currentNodes.get(siblingId)), "a quiz judge reads only its unchanged quiz");
        assertEquals(composer.fingerprint(pairJudge, seedNodes.get(TandaFixture.CHILD)),
                composer.fingerprint(pairJudge, currentNodes.get(TandaFixture.CHILD)), "the other topic has not changed");
        when(engine.runAudit(any(), any())).thenReturn(current);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 9, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        var report = service.report(batch, "correction", course);
        var result = DefaultAnalysisComposer.index(report.getRoot());
        for (String judge : List.of(pairJudge, quizJudge)) {
            assertTrue(DefaultAnalysisComposer.evaluated(result.get(TandaFixture.CHILD), judge),
                    "the other topic must retain its " + judge + " opinion after one conversion");
            assertFalse(DefaultAnalysisComposer.evaluated(result.get(TandaFixture.RESIDE), judge),
                    "the converted quiz must not borrow an opinion about the old input");
            assertTrue(result.get(TandaFixture.RESIDE).getUnevaluatedBy().contains(judge));
        }
        assertTrue(DefaultAnalysisComposer.evaluated(result.get(siblingId), quizJudge));
        assertFalse(DefaultAnalysisComposer.evaluated(result.get(siblingId), pairJudge),
                "an unchanged sibling still needs a new pair opinion when its topic changes");
        assertTrue(result.get(siblingId).getUnevaluatedBy().contains(pairJudge));
        for (String id : List.of(TandaFixture.CHILD, siblingId)) {
            var origins = (Map<?, ?>) result.get(id).getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
            var judges = id.equals(siblingId) ? List.of(quizJudge) : List.of(pairJudge, quizJudge);
            for (String judge : judges) {
                assertEquals(originalOrigins.get(id).get(judge), origins.get(judge),
                        "preserved opinions retain the original measurement's provenance");
            }
        }
        for (String id : quizIds) {
            assertEquals(originalOrigins.get(id), seedNodes.get(id).getMetadata().get(DefaultAnalysisComposer.PROVENANCE));
            for (String judge : List.of(pairJudge, quizJudge)) assertTrue(DefaultAnalysisComposer.evaluated(seedNodes.get(id), judge));
        }
        assertNull(((AuditableQuiz) seedNodes.get(TandaFixture.RESIDE).getEntity()).getMultipleChoice());
        assertEquals(8, batch.getAnalysisSeedContentVersion());
        assertEquals("seed-v8", batch.getAnalysisSeedAuditId());
        assertTrue(history.isEmpty());
        verify(audits, never()).save(any()); verify(store, never()).saveAnalysisSelection(any());
        verify(store, never()).save(any()); verify(repository, never()).save(any(), any());
        verify(engine, atLeastOnce()).runAudit(any(), argThat(selection ->
                selection.getPolicies().values().stream().allMatch(policy -> policy.getMaxNewEvaluations() == 0
                        && policy.getPurpose() == EvaluationRunPurpose.READING)));
    }

    @Test void versionBumpNeverOverwritesAnOpinionAlreadyPresentInTheCurrentReading() {
        var seed = report(true, false, "seed-v1");
        saved.put("seed-v8", seed);
        batch.setAnalysisSeedAuditId("seed-v8"); batch.setAnalysisSeedContentVersion(8); batch.setContentVersion(9);
        var current = report(true, true, "current-v2");
        when(engine.runAudit(any(), any())).thenReturn(current);

        var result = service.report(batch, "correction", course);
        var node = DefaultAnalysisComposer.index(result.getRoot()).get(TandaFixture.CHILD);
        assertEquals(.3, node.getScores().get(NAME));
        assertEquals("current-v2", DefaultAnalysisComposer.version(node, NAME, result));
        assertTrue(node.getFindings().stream().anyMatch(f -> NAME.equals(f.getAnalyzer())));
        var original = DefaultAnalysisComposer.index(seed.getRoot()).get(TandaFixture.CHILD);
        assertEquals(1., original.getScores().get(NAME));
        assertTrue(original.getFindings().isEmpty());
        verify(audits, never()).save(any()); verify(store, never()).saveAnalysisSelection(any());
        verify(store, never()).save(any()); verify(repository, never()).save(any(), any());
    }

    @Test void versionBumpPreservesAStoredAbstentionAndItsPendingMarkWithoutApprovingTheQuiz() {
        var seed = abstained("v2");
        saved.put("seed-v8", seed);
        batch.setAnalysisSeedAuditId("seed-v8"); batch.setAnalysisSeedContentVersion(8); batch.setContentVersion(9);
        var judges = List.of("pragmatism", "naturalness", "a1-didactic");
        config.setJudgeFreshnessCatalog(name -> judges.contains(name) ? Optional.of(todaysJudge(name)) : Optional.empty());
        var current = report(false, false, "v2");
        DefaultAnalysisComposer.index(current.getRoot()).get(TandaFixture.CHILD).getUnevaluatedBy().addAll(judges);
        when(engine.runAudit(any(), any())).thenReturn(current);

        var report = service.report(batch, "correction", course);
        var node = DefaultAnalysisComposer.index(report.getRoot()).get(TandaFixture.CHILD);
        assertTrue(DefaultAnalysisComposer.hasStoredOpinion(node, "pragmatism"));
        assertFalse(DefaultAnalysisComposer.evaluated(node, "pragmatism"));
        assertTrue(node.getUnevaluatedBy().contains("pragmatism"));
        assertFalse(node.getScores().containsKey("pragmatism"));
        assertEquals(((QuizDiagnoses) DefaultAnalysisComposer.index(seed.getRoot()).get(TandaFixture.CHILD)
                        .getDiagnoses()).getPragmatismDiagnosis().orElseThrow(),
                ((QuizDiagnoses) node.getDiagnoses()).getPragmatismDiagnosis().orElseThrow());
        var correction = read().states().stream().filter(s -> "correction".equals(s.id())).findFirst().orElseThrow();
        var mark = correction.verdictMarks().get(TandaFixture.CHILD).get("pragmatism");
        assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeAbstention.NEEDS_CONTEXT, mark.get("abstention"));
        assertEquals("v2", mark.get("judgeVersion"));
        assertEquals(1, correction.judges().stream().filter(j -> "pragmatism".equals(j.judgeName()))
                .findFirst().orElseThrow().abstained());
        verify(audits, never()).save(any()); verify(store, never()).saveAnalysisSelection(any());
        verify(store, never()).save(any()); verify(repository, never()).save(any(), any());
        verify(engine, atLeastOnce()).runAudit(any(), argThat(selection -> selection.getPolicies().values().stream()
                .allMatch(policy -> policy.getMaxNewEvaluations() == 0 && policy.getPurpose() == EvaluationRunPurpose.READING)));
    }

    @ParameterizedTest @ValueSource(ints = {1, 0, -1})
    void legacySeedRetainsOnlyMeasuredCoverageProvenanceAndNeverInfersItFromStanding(int measuredVerdicts) {
        boolean measuredVersions = measuredVerdicts > 0;
        String judge = "near-duplicates";
        var descriptor = new AnalyzerDescriptor(); descriptor.setName(judge); descriptor.setDisplayName(judge);
        descriptor.setCost(AnalysisCost.PAID_MODEL); descriptor.setTarget(AuditTarget.QUIZ);
        descriptor.setFamily(com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily.ERRORS);
        descriptor.setEvaluatedTargets(List.of(AuditTarget.QUIZ));
        when(catalog.list()).thenReturn(List.of(descriptor));
        batch.setAnalyzers(List.of(judge));
        var legacyRuntime = EvaluationRuntimeIdentity.explicit("codex-cli", "previous-model", "medium");
        var seed = report(false, false, "unused");
        var source = DefaultAnalysisComposer.index(seed.getRoot()).get(TandaFixture.CHILD);
        source.getScores().put(judge, 1.0);
        var oldCoverage = new JudgeCoverageDiagnosis(); oldCoverage.setJudgeName(judge);
        oldCoverage.setUnit(AuditTarget.KNOWLEDGE);
        oldCoverage.setStanding(new JudgeExamStanding(judge, "legacy-v1", JudgeExamStatus.APPROVED,
                true, null, legacyRuntime, Set.of("legacy-v1")));
        // -1 represents absent historical versions; zero represents a version listed without any measured verdict.
        if (measuredVerdicts >= 0) oldCoverage.setVersions(List.of(
                new com.learney.contentaudit.auditdomain.judging.JudgeVersionStanding("legacy-v1", measuredVerdicts,
                        JudgeExamStatus.APPROVED, legacyRuntime)));
        cover(seed, oldCoverage);
        assertFalse(source.getMetadata().containsKey(DefaultAnalysisComposer.PROVENANCE));
        saved.put("seed-v8", seed);
        batch.setAnalysisSeedAuditId("seed-v8"); batch.setAnalysisSeedContentVersion(8); batch.setContentVersion(9);

        var current = report(false, false, "unused");
        DefaultAnalysisComposer.index(current.getRoot()).get(TandaFixture.CHILD).getUnevaluatedBy().add(judge);
        var currentCoverage = new JudgeCoverageDiagnosis(); currentCoverage.setJudgeName(judge);
        currentCoverage.setUnit(AuditTarget.KNOWLEDGE);
        currentCoverage.setStanding(new JudgeExamStanding(judge, "current-v2", JudgeExamStatus.NOT_TAKEN,
                false, "sin examen", EvaluationRuntimeIdentity.explicit("codex-cli", "new-model", "high"), Set.of("current-v2")));
        // A different quiz has a measured current opinion; its provenance must not leak into the restored child.
        var otherCurrent = DefaultAnalysisComposer.index(current.getRoot()).get(TandaFixture.RESIDE);
        otherCurrent.getScores().put(judge, 1.0); RecordedVerdictVersions.stamp(otherCurrent, judge, "current-v2");
        currentCoverage.setVersions(List.of(new com.learney.contentaudit.auditdomain.judging.JudgeVersionStanding(
                "current-v2", 1, JudgeExamStatus.NOT_TAKEN,
                EvaluationRuntimeIdentity.explicit("codex-cli", "new-model", "high"))));
        cover(current, currentCoverage);
        when(engine.runAudit(any(), any())).thenReturn(current);

        var result = service.report(batch, "correction", course);
        var node = DefaultAnalysisComposer.index(result.getRoot()).get(TandaFixture.CHILD);
        assertTrue(DefaultAnalysisComposer.evaluated(node, judge));
        assertEquals(1.0, node.getScores().get(judge));
        assertEquals(measuredVersions ? "legacy-v1" : null, DefaultAnalysisComposer.version(node, judge, result),
                "a retained verdict must not acquire the current reader's version");
        var origins = (Map<?, ?>) node.getMetadata().get(DefaultAnalysisComposer.PROVENANCE);
        var origin = (Map<?, ?>) origins.get(judge);
        assertEquals("seed-v8", origin.get("auditId"), "the seed is the only known historical source");
        assertEquals(measuredVersions ? "legacy-v1" : null, origin.get("judgeVersion"));
        assertEquals(measuredVersions ? legacyRuntime.toString() : null, origin.get("runtime"),
                "the standing is permission to run, not evidence of which runtime produced an old verdict");
        assertNull(origin.get("measuredAt"), "an unknown measurement date must not become today's date");
        assertEquals(false, origin.get("integrated"));
        assertFalse(source.getMetadata().containsKey(DefaultAnalysisComposer.PROVENANCE));
        assertEquals(1.0, source.getScores().get(judge));
        assertEquals("legacy-v1", oldCoverage.getStanding().getJudgeVersion());
        assertEquals(legacyRuntime, oldCoverage.getStanding().getRuntimeIdentity());
        verify(audits, never()).save(any()); verify(store, never()).saveAnalysisSelection(any());
        verify(store, never()).save(any()); verify(repository, never()).save(any(), any());
    }

    @Test void integratesDidacticReviewWithoutTreatingItAsApproval() {
        assertStoredAbstentionCanBeIntegrated("a1-didactic", false);
    }
    @Test void integratesNaturalnessNotApplicableWithoutInventingAScore() {
        assertStoredAbstentionCanBeIntegrated("naturalness", false);
    }
    @Test void integratesPragmatismNeedsContextWithoutTreatingItAsProviderFailure() {
        assertStoredAbstentionCanBeIntegrated("pragmatism", false);
    }
    @Test void doesNotIntegrateUnmeasuredProviderFailure() {
        assertStoredAbstentionCanBeIntegrated("naturalness", true);
    }
    @Test void differentPriorLearningStillPreventsIntegrationOfAnIdenticalExercise() {
        assertStoredAbstentionCanBeIntegrated("a1-didactic", false, true);
    }

    private void assertStoredAbstentionCanBeIntegrated(String analyzer, boolean providerFailure) {
        assertStoredAbstentionCanBeIntegrated(analyzer, providerFailure, false);
    }
    private void assertStoredAbstentionCanBeIntegrated(String analyzer, boolean providerFailure, boolean changedCurriculum) {
        var source = report(false, false, "v1");
        var node = DefaultAnalysisComposer.index(source.getRoot()).get(TandaFixture.CHILD);
        node.getUnevaluatedBy().add(analyzer);
        if (!providerFailure) {
            if ("pragmatism".equals(analyzer)) {
                var verdict = new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict(List.of(
                        new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Assessment(
                                "A sentence.", com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Status.NEEDS_CONTEXT,
                                "", null, null, null, "Missing context", "Who is speaking?")), "Context required");
                ((DefaultQuizDiagnoses) node.getDiagnoses()).setPragmatismDiagnosis(
                        new com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis(verdict, "saved-version", false));
            } else {
                node.getMetadata().put(analyzer + "InputContract", "english-quality-input-v2");
                node.getMetadata().put(analyzer + "Version", "saved-version");
                node.getMetadata().put(analyzer + "Verdict", "naturalness".equals(analyzer)
                        ? Map.of("assessments", List.of(Map.of("status", "NOT_APPLICABLE")))
                        : Map.of("status", "REVIEW"));
            }
        }
        if (changedCurriculum) {
            var topic = node.ancestor(AuditTarget.TOPIC).orElseThrow();
            var knowledge = new AuditableKnowledge();
            knowledge.setId("different-prior-learning"); knowledge.setLabel("Afirmativas con be");
            topic.getChildren().add(0, new AuditNode(knowledge, AuditTarget.KNOWLEDGE, topic,
                    new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), null));
        }
        saved.put("abstention", source);
        batch.setSelectiveRuns(List.of(new SelectiveCorrection.Run("abstention", "analyze", analyzer,
                null, 1, 6, "content", "saved-version", "codex-cli / model / medium", "abstention",
                null, List.of(), List.of(), 0, 0, 0, 1, 0, 1, "PARTIAL", "human", Instant.EPOCH,
                Instant.EPOCH, null, false, 6, "content", null)));
        var fit = read().looseRuns().get(0).fits().stream().filter(f -> "correction".equals(f.state())).findFirst().orElseThrow();
        assertEquals(providerFailure || changedCurriculum ? 0 : 1, fit.compatible());
        if (providerFailure || changedCurriculum) return;
        var integrated = service.execute(select("integrate", "abstention", "saved-abstention", null));
        var state = integrated.states().stream().filter(s -> "correction".equals(s.id())).findFirst().orElseThrow();
        assertTrue(state.provenance().get(TandaFixture.CHILD).containsKey(analyzer));
        var result = DefaultAnalysisComposer.index(service.report(batch, "correction", course).getRoot()).get(TandaFixture.CHILD);
        assertTrue(DefaultAnalysisComposer.hasStoredOpinion(result, analyzer));
        assertFalse(DefaultAnalysisComposer.evaluated(result, analyzer));
        assertFalse(result.getScores().containsKey(analyzer));
        verify(repository, never()).save(any(), any());
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

    @Tag("FEAT-MOTIVO") @Tag("F-MOTIVO-R002")
    @Test void aTaskOfAnotherExerciseNeverWarnsAnApprovalOfThisOne() {
        // The change of CHILD names, by number, a task of RESIDE: its mark going away says nothing about CHILD.
        var task = new com.learney.contentaudit.refinerdomain.RefinementTask(); task.setId("borrowed-task");
        task.setNodeTarget(com.learney.contentaudit.auditdomain.AuditTarget.QUIZ); task.setNodeId(TandaFixture.RESIDE);
        var origin = new com.learney.contentaudit.refinerdomain.TaskOrigin();
        var finding = new Finding(); finding.setAnalyzer(NAME); finding.setRule("answer-cue"); origin.setFinding(finding); task.setOrigin(origin);
        var own = new com.learney.contentaudit.refinerdomain.RefinementTask(); own.setId("own-task");
        own.setNodeTarget(com.learney.contentaudit.auditdomain.AuditTarget.QUIZ); own.setNodeId(TandaFixture.CHILD); own.setOrigin(origin);
        var step = new BatchChangeStep(); step.setQuizId(TandaFixture.CHILD);
        var borrowed = new BatchReview.Item("borrowed-change", step, null, null, "panel", null,
                BatchReviewDecision.Status.APPROVED, "human", Instant.EPOCH, List.of(task.getId()), null);
        var mine = new BatchReview.Item("own-change", step, null, null, "panel", null,
                BatchReviewDecision.Status.APPROVED, "human", Instant.EPOCH, List.of(own.getId()), null);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 6, null, List.of(task, own), List.of(),
                List.of(borrowed, mine), List.of(), List.of()));
        service.execute(select("integrate", "r1", "s1", null)); run("r2", false, "v2");
        service.execute(select("replace", "r2", "s2", null));
        var state = read().states().stream().filter(x -> x.id().equals("correction")).findFirst().orElseThrow();
        assertEquals(List.of("own-change"), state.approvalWarnings());
    }

    /** A run of the batch that names the plan where its tasks live, as the corrections do. */
    void runWithPlan(String id, boolean error, String version, String planId) {
        saved.put(id, report(true, error, version));
        var runs = new ArrayList<>(BatchBook.list(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run(id, "analyze", NAME, null, 1, 6, "content", "configured-version",
                "codex-cli / model / medium", id, planId, List.of(), List.of(), 1, 1, 1, 1, 1, 0,
                "DONE", "human", Instant.EPOCH, Instant.EPOCH, null, false, 6, "content", null));
        batch.setSelectiveRuns(runs);
    }
    /** A task of CHILD whose reason is the mark the quality judge leaves on it. */
    static com.learney.contentaudit.refinerdomain.RefinementTask qualityTask(String id) {
        var task = new com.learney.contentaudit.refinerdomain.RefinementTask(); task.setId(id);
        task.setNodeTarget(AuditTarget.QUIZ); task.setNodeId(TandaFixture.CHILD);
        var origin = new com.learney.contentaudit.refinerdomain.TaskOrigin();
        var finding = new Finding(); finding.setAnalyzer(NAME); finding.setRule("answer-cue"); origin.setFinding(finding);
        task.setOrigin(origin);
        return task;
    }
    static BatchReview.Item approved(String changeId, String taskId) {
        var step = new BatchChangeStep(); step.setQuizId(TandaFixture.CHILD);
        return new BatchReview.Item(changeId, step, null, null, "panel", null,
                BatchReviewDecision.Status.APPROVED, "human", Instant.EPOCH, List.of(taskId), null);
    }

    @Tag("FEAT-OPINA") @Tag("F-OPINA-R011")
    @Test void aPlanNamedBySeveralRunsIsReadOnceAndItsTasksStillWarnTheirApprovals() {
        // The C2 of 29/10 named its own plan from 19 of its 74 runs: each named the same tasks again.
        var plans = mock(com.learney.contentaudit.refinerdomain.RefinementPlanStore.class);
        var plan = new com.learney.contentaudit.refinerdomain.RefinementPlan();
        plan.setTasks(List.of(qualityTask("task-001")));
        when(plans.load("batch-plan")).thenReturn(Optional.of(plan));
        config.setRefinementPlanStore(plans);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 6, null, List.of(), List.of(),
                List.of(approved("quality-change", "task-001"), approved("unrelated-change", "task-002")), List.of(), List.of()));
        service.execute(select("integrate", "r1", "s1", null));
        for (String id : List.of("r2", "r3", "r4")) runWithPlan(id, false, "v2", "batch-plan");
        service.execute(select("replace", "r2", "s2", null));
        clearInvocations(plans);

        var view = read();

        var state = view.states().stream().filter(x -> x.id().equals("correction")).findFirst().orElseThrow();
        assertEquals(List.of("quality-change"), state.approvalWarnings());
        var r3 = view.looseRuns().stream().filter(r -> r.id().equals("r3")).findFirst().orElseThrow();
        assertEquals(List.of(), r3.fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow().approvalWarnings());
        var r1 = view.looseRuns().stream().filter(r -> r.id().equals("r1")).findFirst().orElseThrow();
        assertEquals(List.of(), r1.fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow().approvalWarnings());
        verify(plans, times(1)).load("batch-plan");
    }

    @Tag("FEAT-OPINA") @Tag("F-OPINA-R011")
    @Test void readingDoesNotGrowWithApprovalsTimesTasksTimesRuns() {
        // At the C2's scale -- 151 approvals, a plan per run with its tasks numbered again from task-00000, 60 runs --
        // the reading walked every task of every plan for each approval, once per run and state: about 5 minutes.
        var tasks = new ArrayList<com.learney.contentaudit.refinerdomain.RefinementTask>();
        for (int i = 0; i < 8_000; i++) tasks.add(qualityTask(String.format("task-%05d", i)));
        var plans = mock(com.learney.contentaudit.refinerdomain.RefinementPlanStore.class);
        when(plans.load(anyString())).thenAnswer(i -> {
            var plan = new com.learney.contentaudit.refinerdomain.RefinementPlan(); plan.setTasks(tasks); return Optional.of(plan);
        });
        config.setRefinementPlanStore(plans);
        var items = new ArrayList<BatchReview.Item>();
        for (int i = 0; i < 150; i++) items.add(approved("change-" + i, String.format("task-%05d", i)));
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 6, null, List.of(), List.of(), items, List.of(), List.of()));
        service.execute(select("integrate", "r1", "s1", null));
        for (int i = 2; i <= 60; i++) runWithPlan("r" + i, false, "v2", "plan-of-r" + i);
        service.execute(select("replace", "r2", "s2", null));

        var view = assertTimeoutPreemptively(java.time.Duration.ofSeconds(10), this::read);

        var state = view.states().stream().filter(x -> x.id().equals("correction")).findFirst().orElseThrow();
        assertEquals(150, state.approvalWarnings().size());
        assertEquals(60, view.looseRuns().size());
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


    private DefaultContextOpinionService completingService(List<Integer> caps) { return completingService(caps, NAME); }
    private DefaultContextOpinionService completingService(List<Integer> caps, String judge) {
        if ("near-duplicates".equals(judge)) {
            var descriptor = new AnalyzerDescriptor(); descriptor.setName(judge); descriptor.setDisplayName("Ejercicio casi repetido");
            descriptor.setCost(AnalysisCost.PAID_MODEL); descriptor.setTarget(AuditTarget.QUIZ);
            descriptor.setFamily(com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily.ERRORS);
            descriptor.setEvaluatedTargets(List.of(AuditTarget.QUIZ)); descriptor.setResolutions(List.of(FindingResolution.PANEL));
            when(catalog.list()).thenReturn(List.of(descriptor));
            var names = new ArrayList<>(batch.getAnalyzers()); names.add(judge); batch.setAnalyzers(names);
        }
        var bridge = mock(CurrentAnalysisService.class, org.mockito.AdditionalAnswers.delegatesTo(service));
        doReturn(Set.of(judge)).when(bridge).supportedAnalyzers();
        config.setCurrentAnalysisService(bridge);
        when(audits.save(any())).thenAnswer(i -> {
            String id = "saved-" + saved.size(); saved.put(id, i.getArgument(0)); return id;
        });
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            var selection = i.getArgument(1, AnalyzerRunSelection.class);
            int cap = selection.getPolicies().containsKey(judge) ? selection.getPolicies().get(judge).getMaxNewEvaluations() : 0;
            caps.add(cap);
            var report = report(NAME.equals(judge) && cap > 0, true, "v1");
            if ("near-duplicates".equals(judge)) for (var node : DefaultAnalysisComposer.index(report.getRoot()).values()) {
                if (!DefaultAnalysisComposer.eligible(node, judge)) continue;
                node.getScores().put(judge, 1.0); node.getMetadata().put("verdictVersions", Map.of(judge, "v1"));
                if (TandaFixture.CHILD.equals(node.getEntity().getId()) && cap == 0) node.getPartiallyEvaluatedBy().add(judge);
            }
            var standing = new com.learney.contentaudit.auditdomain.judging.JudgeExamStanding();
            standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
            var coverage = new com.learney.contentaudit.auditdomain.judging.JudgeCoverageDiagnosis();
            coverage.setJudgeName(judge); coverage.setStanding(standing); coverage.setUnit("near-duplicates".equals(judge) ? AuditTarget.KNOWLEDGE : AuditTarget.QUIZ);
            coverage.setCost(new com.learney.contentaudit.auditdomain.judging.JudgeCostEstimate(1, cap > 0 ? 0 : 1, 0, false, 0, 1, 0));
            coverage.setEvaluators(List.of(new com.learney.contentaudit.auditdomain.judging.EvaluatorCoverage(judge,
                    new com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage(1, cap > 0 ? 1 : 0, cap > 0 ? 1 : 0, 0, cap > 0 ? 0 : 1, 0), "v1")));
            ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
            return report;
        });
        return new DefaultContextOpinionService(config, ledger, reviews, c -> "content");
    }
    @Test void completionIncludesUnchangedExercisesAndIntegratesIntoTheExistingAnalysisWithoutChangingDecisions() {
        var caps = new ArrayList<Integer>(); var opinions = completingService(caps);
        var estimate = opinions.execute(new ContextOpinion.Request("estimate", "batch", null, false,
                null, null, null, null, null, "correction"));
        assertEquals("correction", estimate.scope());
        assertTrue(estimate.selection().get(NAME).contains(TandaFixture.CHILD));
        assertTrue(caps.stream().allMatch(c -> c == 0)); assertTrue(history.isEmpty());
        var request = new ContextOpinion.Request("measure", "batch", estimate.selection(), false,
                "complete-1", 6, estimate.fingerprint(), 1, "human", "correction");
        opinions.execute(request);
        assertEquals(1, history.size()); assertEquals("integrate", history.get(0).action());
        assertEquals("correction", history.get(0).state());
        assertEquals(1, errors(read())); assertEquals(6, batch.getContentVersion()); assertEquals(1, batch.getCorrectionRevision());
        assertEquals(1, batch.getClosings().size()); assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertEquals(1, caps.stream().filter(c -> c > 0).count());
        assertEquals("complete-1", read().looseRuns().stream().filter(r -> r.id().equals(history.get(0).runId())).findFirst().orElseThrow().activityId());
        int calls = caps.size(); opinions.execute(request); assertEquals(calls, caps.size()); assertEquals(1, history.size());
    }
    @Test void completingNeverReplacesASelectedOpinionAndZeroBudgetLeavesUnmeasuredExercisesPending() {
        service.execute(select("integrate", "r1", "selected", null));
        var caps = new ArrayList<Integer>(); var opinions = completingService(caps);
        var estimate = opinions.execute(new ContextOpinion.Request("estimate", "batch", null, false,
                null, null, null, null, null, "correction"));
        assertFalse(estimate.selection().getOrDefault(NAME, List.of()).contains(TandaFixture.CHILD));
        opinions.execute(new ContextOpinion.Request("measure", "batch", estimate.selection(), false,
                "complete-2", 6, estimate.fingerprint(), 0, "human", "correction"));
        assertEquals(1, errors(read())); assertEquals(1, history.size());
        assertTrue(caps.stream().allMatch(c -> c == 0));
    }

    @Test void authoritativeApplicabilityDoesNotCallAMultipleChoiceHintPending() {
        var names = new ArrayList<>(batch.getAnalyzers()); names.add("hint-coherence"); names.add("answer-completeness"); batch.setAnalyzers(names);
        var state = read().states().stream().filter(x -> "correction".equals(x.id())).findFirst().orElseThrow();
        assertTrue(state.inapplicableAnalyzers().get(TandaFixture.CHILD).containsAll(List.of("hint-coherence", "answer-completeness", "single-answer")));
        assertTrue(service.supportedAnalyzers().contains("near-duplicates"));
    }

    @Test void completionFindsAPartialPairJudgeAndIntegratesItWithoutStartingAnotherCorrection() {
        var caps = new ArrayList<Integer>(); var opinions = completingService(caps, "near-duplicates");
        var estimate = opinions.execute(new ContextOpinion.Request("estimate", "batch", null, false,
                null, null, null, null, null, "correction"));
        assertEquals(List.of(TandaFixture.CHILD), estimate.selection().get("near-duplicates"));
        assertEquals("KNOWLEDGE", estimate.judges().get(0).unit()); assertEquals(1, estimate.pendingQueries());
        assertTrue(estimate.judges().get(0).quizzes() > 1, "The pair judge must read the other quizzes of the topic too");
        assertTrue(caps.stream().allMatch(c -> c == 0));
        opinions.execute(new ContextOpinion.Request("measure", "batch", estimate.selection(), false,
                "complete-pairs", 6, estimate.fingerprint(), 1, "human", "correction"));
        assertEquals(1, history.size()); assertEquals("near-duplicates", history.get(0).analyzer());
        assertEquals("correction", history.get(0).state());
        var current = service.report(batch, "correction", course);
        assertTrue(DefaultAnalysisComposer.evaluated(DefaultAnalysisComposer.index(current.getRoot()).get(TandaFixture.CHILD), "near-duplicates"));
        assertEquals(1, batch.getCorrectionRevision()); assertEquals(6, batch.getContentVersion()); assertEquals(1, batch.getClosings().size());
        assertEquals(1, caps.stream().filter(c -> c > 0).count());
    }

    @Test @Tag("FEAT-ALDIA") @Tag("F-ALDIA-R005") @Tag("F-ALDIA-R003")
    void eachStateCountsItsJudgeAgainstTodaysVersionAndEveryVerdictCarriesItsMark() {
        var zone = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
        var clock = java.time.Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), zone);
        var xhigh = com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "xhigh");
        var old = new com.learney.contentaudit.auditdomain.judging.JudgeExamRecord(NAME, "v1",
                com.learney.contentaudit.auditdomain.judging.JudgeExamStatus.APPROVED, 20, 0, List.of(), List.of(), true, null,
                "human", Instant.parse("2026-10-06T02:14:16Z"));
        old.setRuntimeIdentity(xhigh);
        var today = new com.learney.contentaudit.auditdomain.judging.JudgeExamRecord(NAME, "v2",
                com.learney.contentaudit.auditdomain.judging.JudgeExamStatus.APPROVED, 20, 0, List.of(), List.of(), true, null,
                "human", Instant.parse("2026-10-14T01:12:33Z"));
        today.setRuntimeIdentity(xhigh);
        var standing = new com.learney.contentaudit.auditdomain.judging.JudgeExamStanding(NAME, "v2",
                com.learney.contentaudit.auditdomain.judging.JudgeExamStatus.APPROVED, true, null, xhigh, Set.of("v2"));
        var reference = com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference.of(standing, List.of(old, today), zone, clock);
        config.setJudgeFreshnessCatalog(name -> NAME.equals(name) ? Optional.of(reference) : Optional.empty());
        var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName(NAME); coverage.setUnit(AuditTarget.QUIZ);
        coverage.setStanding(standing);
        for (String id : List.of("r1")) {
            cover(saved.get(id), coverage);
        }
        saved.put("r2", report(true, false, "v2"));
        cover(saved.get("r2"), coverage);
        var runs = new ArrayList<>(BatchBook.list(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run("r2", "analyze", NAME, null, 1, 6, "content", "v2", xhigh.toString(), "r2", null,
                List.of(), List.of(), 1, 1, 1, 1, 1, 0, "DONE", "human", Instant.EPOCH, Instant.EPOCH, null, false, 6, "content", null));
        batch.setSelectiveRuns(runs);
        var primary = report(false, false, "v1");
        cover(primary, coverage);
        when(engine.runAudit(any(), any())).thenReturn(primary);

        service.execute(select("integrate", "r1", "s1", null));
        var before = read().states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow();
        var count = before.judges().stream().filter(j -> NAME.equals(j.judgeName())).findFirst().orElseThrow();
        assertEquals(1, count.fromBefore());
        assertEquals(count.total(), count.upToDate() + count.fromBefore() + count.notMeasured());
        assertEquals("la del 13/10", count.currentVersionName());
        assertEquals(Instant.parse("2026-10-14T01:12:33Z"), count.currentApprovedAt());
        // r1 left its runtime as free text: it is not guessed, it has no known provenance.
        assertTrue(count.summary().endsWith("reusa 1 ejercicio de antes (1 de procedencia desconocida)"), count.summary());
        var mark = before.verdictMarks().get(TandaFixture.CHILD).get(NAME);
        assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeVerdictFreshness.FROM_BEFORE, mark.get("freshness"));
        assertEquals("v1", mark.get("judgeVersion"));
        assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeVerdictStaleCause.UNKNOWN_PROVENANCE, mark.get("staleCause"));
        assertEquals("versión del 5/10", mark.get("versionName"));

        service.execute(select("replace", "r2", "s2", null));
        var after = read().states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow();
        var fresh = after.judges().stream().filter(j -> NAME.equals(j.judgeName())).findFirst().orElseThrow();
        assertEquals(0, fresh.fromBefore());
        assertEquals(1, fresh.upToDate());
        assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeVerdictFreshness.UP_TO_DATE,
                after.verdictMarks().get(TandaFixture.CHILD).get(NAME).get("freshness"));
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
    }

    static final EvaluationRuntimeIdentity XHIGH = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "xhigh");
    /** Today's judge is v2, approved with XHIGH or not approved at all. */
    static JudgeFreshnessReference todaysJudge(JudgeExamStatus status) {
        var zone = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
        var exam = new JudgeExamRecord(NAME, "v2", status, 20, 0, List.of(), List.of(), true, null, "human", Instant.parse("2026-10-14T01:12:33Z"));
        exam.setRuntimeIdentity(XHIGH);
        return JudgeFreshnessReference.of(new JudgeExamStanding(NAME, "v2", status, true, null, XHIGH, Set.of("v2")),
                List.of(exam), zone, java.time.Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), zone));
    }
    static CurrentAnalysisService.Fit fit(CurrentAnalysisService.View view, String run, String state) {
        return view.looseRuns().stream().filter(r -> r.id().equals(run)).findFirst().orElseThrow()
                .fits().stream().filter(f -> f.state().equals(state)).findFirst().orElseThrow();
    }
    /** gaps, toToday, toOtherVersion, changesResults, repeats, selected. */
    static List<Integer> parts(CurrentAnalysisService.Fit f) {
        return List.of(f.gaps(), f.toToday(), f.toOtherVersion(), f.changesResults(), f.repeats(), f.selected());
    }

    @Test @Tag("FEAT-MEDJ") @Tag("F-MEDJ-R001")
    void whatALooseRunBringsIsSplitIntoGapsTodaysJudgeAnotherVersionOtherResultsAndRepeats() {
        config.setJudgeFreshnessCatalog(name -> NAME.equals(name) ? Optional.of(todaysJudge(JudgeExamStatus.APPROVED)) : Optional.empty());
        service.execute(select("integrate", "r1", "s1", null)); // Counted in the correction: v1, with error.
        run("repeats", true, "v1"); run("changes", false, "v1");
        run("today", true, "v2", XHIGH.toString()); run("other", true, "v0", XHIGH.toString());
        var view = read();

        assertEquals(List.of(0, 0, 0, 0, 0, 1), parts(fit(view, "r1", "correction")));
        assertEquals(List.of(0, 0, 0, 0, 1, 0), parts(fit(view, "repeats", "correction")));
        assertEquals(List.of(0, 0, 0, 1, 0, 0), parts(fit(view, "changes", "correction")));
        // Today's judge counts even with the same marks; a version that is not today's never does.
        assertEquals(List.of(0, 1, 0, 0, 0, 0), parts(fit(view, "today", "correction")));
        assertEquals(List.of(0, 0, 1, 0, 0, 0), parts(fit(view, "other", "correction")));
        // In the base nothing is counted: every run fills the gap, and a gap is never "other results" although it marks.
        var base = fit(view, "repeats", "base");
        assertEquals(List.of(1, 0, 0, 0, 0, 0), parts(base));
        assertEquals(List.of(TandaFixture.CHILD), base.newErrors());
        for (var run : view.looseRuns()) for (var f : run.fits()) {
            assertEquals(f.compatible(), parts(f).stream().mapToInt(Integer::intValue).sum(), run.id() + " " + f.state());
            assertEquals(f.replaces(), f.toToday() + f.toOtherVersion() + f.changesResults() + f.repeats(), run.id() + " " + f.state());
            assertEquals(Boolean.TRUE, f.todayApproved());
        }
        assertEquals(Boolean.TRUE, fit(view, "today", "correction").runToday());
        // r1 left its runtime as free text: integrated, it would be marked from before, so it is not today's judge.
        assertEquals(Boolean.FALSE, fit(view, "r1", "correction").runToday());
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
    }

    @Test @Tag("FEAT-MEDJ") @Tag("F-MEDJ-R002")
    void todaysVersionWithoutApprovalIsStillTodaysAndAJudgeWithoutOneNeverBringsTodays() {
        config.setJudgeFreshnessCatalog(name -> NAME.equals(name) ? Optional.of(todaysJudge(JudgeExamStatus.FAILED)) : Optional.empty());
        service.execute(select("integrate", "r1", "s1", null));
        run("today", true, "v2"); // Without approval only the version is looked at, not the runtime.
        var unapproved = fit(read(), "today", "correction");
        assertEquals(List.of(0, 1, 0, 0, 0, 0), parts(unapproved));
        assertEquals(Boolean.TRUE, unapproved.runToday());
        assertEquals(Boolean.FALSE, unapproved.todayApproved());

        config.setJudgeFreshnessCatalog(name -> Optional.empty());
        var unknown = fit(read(), "today", "correction");
        assertEquals(List.of(0, 0, 1, 0, 0, 0), parts(unknown));
        assertNull(unknown.runToday());
        assertNull(unknown.todayApproved());

        config.setJudgeFreshnessCatalog(null);
        assertEquals(parts(unknown), parts(fit(read(), "today", "correction")));
        assertNull(fit(read(), "today", "correction").todayApproved());
    }

    /** Today's judge {@code judge} is v2, approved with XHIGH. */
    static JudgeFreshnessReference todaysJudge(String judge) {
        var zone = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
        var exam = new JudgeExamRecord(judge, "v2", JudgeExamStatus.APPROVED, 20, 0, List.of(), List.of(), true, null, "human",
                Instant.parse("2026-10-14T14:13:57Z"));
        exam.setRuntimeIdentity(XHIGH);
        return JudgeFreshnessReference.of(new JudgeExamStanding(judge, "v2", JudgeExamStatus.APPROVED, true, null, XHIGH, Set.of("v2")),
                List.of(exam), zone, java.time.Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"), zone));
    }
    /**
     * The correction as each judge left it: on the child, pragmatism lacks context, naturalness does not apply and the
     * A1 didactics asks for a review, all three by {@code version} and without a score.
     */
    AuditReport abstained(String version) {
        var r = TandaFixture.report(course, List.of(), Map.of(), List.of());
        var n = DefaultAnalysisComposer.index(r.getRoot()).get(TandaFixture.CHILD);
        n.setDiagnoses(new DefaultQuizDiagnoses());
        var verdict = new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict(List.of(
                new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Assessment("A sentence.",
                        com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Status.NEEDS_CONTEXT, "", null, null,
                        null, "Missing context", "Who is speaking?")), "Context required");
        ((DefaultQuizDiagnoses) n.getDiagnoses()).setPragmatismDiagnosis(
                new com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis(verdict, version, true));
        n.getUnevaluatedBy().add("pragmatism");
        for (String judge : List.of("naturalness", "a1-didactic")) {
            n.getMetadata().put(judge + "InputContract", com.learney.contentaudit.auditdomain.englishquality.EnglishQualitySubjectBuilder.CONTRACT);
            n.getMetadata().put(judge + "Version", version);
        }
        n.getMetadata().put("verdictVersions", Map.of("naturalness", version, "a1-didactic", version));
        n.getMetadata().put("naturalnessVerdict", Map.of("assessments", List.of(Map.of("status", "NOT_APPLICABLE"))));
        n.getMetadata().put("a1-didacticVerdict", Map.of("status", "REVIEW"));
        n.getUnevaluatedBy().add("a1-didactic"); // naturalness closes a «no aplica» without leaving it pending
        for (String judge : List.of("pragmatism", "naturalness", "a1-didactic")) {
            var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName(judge); coverage.setUnit(AuditTarget.QUIZ);
            coverage.setStanding(new JudgeExamStanding(judge, "v2", JudgeExamStatus.APPROVED, true, null, XHIGH, Set.of("v2")));
            cover(r, coverage);
        }
        return r;
    }

    @Test @Tag("FEAT-ALDIA") @Tag("F-ALDIA-R003") @Tag("F-ALDIA-R005")
    void aStoredAbstentionIsAVerdictWithItsMarkAndNeverNotMeasured() {
        var judges = List.of("pragmatism", "naturalness", "a1-didactic");
        config.setJudgeFreshnessCatalog(name -> judges.contains(name) ? Optional.of(todaysJudge(name)) : Optional.empty());
        when(engine.runAudit(any(), any())).thenAnswer(i -> abstained("v2"));
        var today = read().states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow();
        var reasons = Map.of("pragmatism", com.learney.contentaudit.auditdomain.judging.JudgeAbstention.NEEDS_CONTEXT,
                "naturalness", com.learney.contentaudit.auditdomain.judging.JudgeAbstention.NOT_APPLICABLE,
                "a1-didactic", com.learney.contentaudit.auditdomain.judging.JudgeAbstention.REVIEW);
        for (String judge : judges) {
            var count = today.judges().stream().filter(j -> judge.equals(j.judgeName())).findFirst().orElseThrow();
            // Only the child has a verdict: every other exercise of the cut is really not measured.
            assertEquals(1, count.upToDate(), judge);
            assertEquals(count.total() - 1, count.notMeasured(), judge);
            assertEquals(count.total(), count.upToDate() + count.fromBefore() + count.notMeasured(), judge);
            assertEquals(1, count.abstained(), judge);
            assertEquals(reasons.get(judge), count.abstentions().get(0).reason(), judge);
            assertTrue(count.summary().endsWith("; se abstuvo en 1 (1 " + reasons.get(judge).label() + ")"), count.summary());
            var mark = today.verdictMarks().get(TandaFixture.CHILD).get(judge);
            assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeVerdictFreshness.UP_TO_DATE, mark.get("freshness"), judge);
            assertEquals(reasons.get(judge), mark.get("abstention"), judge);
            assertEquals(reasons.get(judge).label(), mark.get("abstentionName"), judge);
        }

        // An abstention of an earlier version is from before: measuring it again with today's judge may give a score.
        when(engine.runAudit(any(), any())).thenAnswer(i -> abstained("v1"));
        var earlier = read().states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow();
        var pragmatism = earlier.judges().stream().filter(j -> "pragmatism".equals(j.judgeName())).findFirst().orElseThrow();
        assertEquals(1, pragmatism.fromBefore());
        assertEquals(0, pragmatism.upToDate());
        assertEquals(1, pragmatism.abstained());
        assertEquals(com.learney.contentaudit.auditdomain.judging.JudgeVerdictStaleCause.EARLIER_VERSION,
                pragmatism.fromBeforeGroups().get(0).cause());
        verify(repository, never()).save(any(), any()); verify(store, never()).save(any());
    }

    CurrentAnalysisService.State correction(CurrentAnalysisService.View view) {
        return view.states().stream().filter(s -> s.id().equals("correction")).findFirst().orElseThrow();
    }
    /** F-OPINA-R001: the read says, per state, what is missing; what an integration brings stops being missing. */
    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void theReadSaysWhatIsMissingAndAnIntegrationTakesItOff() {
        var before = correction(read()).missingOpinions();
        assertTrue(before.get(NAME).contains(TandaFixture.CHILD));
        assertFalse(before.get(NAME).contains(TandaFixture.RESIDE), "The judge of options does not apply to a gap");
        assertTrue(composer.supportedAnalyzers().containsAll(before.keySet()), "Only judges that Completar covers");
        assertEquals(MissingOpinions.of(service.report(batch, "correction", DefaultCurrentAnalysisService.cut(course, batch.getLevel())),
                composer.supportedAnalyzers()), before);

        service.execute(select("integrate", "r1", "op-r1", null));

        var after = correction(read()).missingOpinions();
        assertFalse(after.getOrDefault(NAME, List.of()).contains(TandaFixture.CHILD));
    }
    /** F-OPINA-R004: a later run that left no verdict does not fit, is never integrated and hides nothing. */
    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R004")
    void aLaterRunWithoutVerdictsNeverHidesTheIntegratedOne() {
        service.execute(select("integrate", "r1", "op-r1", null));
        int errorsBefore = errors(read());
        saved.put("empty", report(false, false, "v1"));
        var runs = new ArrayList<>(BatchBook.list(batch.getSelectiveRuns()));
        runs.add(new SelectiveCorrection.Run("empty", "complete-opinions", NAME, "op-empty", 1, 6, "content", "v1",
                "codex-cli / model / medium", "empty", null, List.of(), List.of(TandaFixture.CHILD), 1, 1, 0, 1, 0, 1,
                "PARTIAL", "human", Instant.EPOCH, Instant.EPOCH, null, false, 6, "content", null));
        batch.setSelectiveRuns(runs);

        var view = read();
        var fit = view.looseRuns().stream().filter(r -> r.id().equals("empty")).findFirst().orElseThrow()
                .fits().stream().filter(f -> f.state().equals("correction")).findFirst().orElseThrow();
        assertEquals(0, fit.compatible());
        assertThrows(IllegalArgumentException.class, () -> service.execute(select("integrate", "empty", "op-empty-i", null)));
        assertEquals(errorsBefore, errors(read()));
        assertFalse(correction(read()).missingOpinions().getOrDefault(NAME, List.of()).contains(TandaFixture.CHILD));
    }
    static void cover(AuditReport report, JudgeCoverageDiagnosis coverage) {
        var d = (DefaultCourseDiagnoses) report.getRoot().getDiagnoses();
        var list = new ArrayList<>(BatchBook.list(d.getJudgeCoverages())); list.add(coverage); d.setJudgeCoverages(list);
    }
    @Test void historicalSeedAndIntegratedReportsProjectAudioOutOfCurrentSeverityWithoutChangingSavedEvidence() {
        var audio = new Finding("data-coherence","audio-matches",new FindingNodeRef(AuditTarget.QUIZ,TandaFixture.CHILD,"quiz"),
                FindingSeverity.MEDIUM,new FindingEvidence(List.of(),"old audio",List.of()),FindingResolution.RULE,null,null);
        var content = new Finding("data-coherence","saved-sentence",new FindingNodeRef(AuditTarget.QUIZ,TandaFixture.CHILD,"quiz"),
                FindingSeverity.MEDIUM,new FindingEvidence(List.of(),"saved text",List.of()),FindingResolution.RULE,null,null);
        var savedSeed = TandaFixture.report(course,List.of(audio,content),Map.of(),List.of("data-coherence"));
        var node = DefaultAnalysisComposer.index(savedSeed.getRoot()).get(TandaFixture.CHILD);
        node.setDiagnoses(new DefaultQuizDiagnoses()); node.getScores().put("data-coherence",0.0);
        saved.put("audio-seed",savedSeed); batch.setAnalysisSeedAuditId("audio-seed"); batch.setAnalysisSeedContentVersion(6);
        saved.put(batch.getBaselineAuditId(),savedSeed);
        for (String state : List.of("base","correction")) {
            var report = service.report(batch,state,course);
            var current = DefaultAnalysisComposer.index(report.getRoot()).get(TandaFixture.CHILD);
            assertEquals(List.of(content),current.getFindings());
            assertEquals(0.0,current.getScores().get("data-coherence"));
            assertEquals("CONTENT_WITHOUT_AUDIO_V1",report.getRoot().getMetadata().get("analysisProjection"));
        }
        assertEquals(List.of(audio,content),node.getFindings());
        assertNull(savedSeed.getRoot().getMetadata().get("analysisProjection"));
    }

}
