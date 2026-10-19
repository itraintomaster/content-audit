package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.translationsense.TranslationSenseVerdict;
import com.learney.contentaudit.auditdomain.translationsenseengine.TranslationSenseAnalyzerProvider;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.evaluationledgerdomain.*;
import com.learney.contentaudit.refinerdomain.*;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
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
    final AnalyzerCorrectionCatalog corrections = mock(AnalyzerCorrectionCatalog.class);
    final BatchServiceConfig config = new BatchServiceConfig();
    final Map<String, RefinementPlan> savedPlans = new HashMap<>();
    final JudgeCoverageDiagnosis quality = coverage("multiple-choice-quality");
    final JudgeCoverageDiagnosis unique = coverage("multiple-choice-validity");
    final JudgeCoverageDiagnosis single = coverage("single-answer");
    final JudgeCoverageDiagnosis instruction = coverage("quiz-instruction");
    final JudgeCoverageDiagnosis translation = coverage("translation-sense");
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
        ((DefaultCourseDiagnoses)report.getRoot().getDiagnoses()).setJudgeCoverages(List.of(quality, unique, single, instruction, translation));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            var s = i.getArgument(1, AnalyzerRunSelection.class);
            var ec = new EvaluationCoverage(); ec.setEvaluatedInRun(s.getPolicies().get(s.getAnalyzers().get(0)).getMaxNewEvaluations() > 0 ? 1 : 0);
            for (var c : List.of(quality, unique, single, instruction, translation))
                if (s.getAnalyzers().contains(c.getJudgeName())) c.setEvaluators(List.of(new EvaluatorCoverage(c.getJudgeName(), ec, "v1")));
            return report;
        });
        when(audits.save(any())).thenAnswer(i -> "audit-" + ++auditNumber);
        when(audits.load(anyString())).thenReturn(Optional.of(report));
        savedPlans.put(batch.getPlanId(), TandaFixture.plan(TandaFixture.lengthTask("unrelated", quiz)));
        when(plans.load(anyString())).thenAnswer(i -> Optional.ofNullable(savedPlans.get(i.getArgument(0))));
        doAnswer(i -> { var p = i.getArgument(0, RefinementPlan.class); savedPlans.put(p.getId(), p); return null; }).when(plans).save(any());
        Finding f = new Finding(); f.setAnalyzer("multiple-choice-quality"); f.setRule("weak-distractors");
        f.setResolution(FindingResolution.PANEL);
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
        config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAuditReportStore(audits); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setPanelRunnerFactory(factory);
        config.setCorrectionCatalog(corrections);
        for (String analyzer : List.of("multiple-choice-quality", "single-answer", "quiz-instruction", "translation-sense")) {
            var correction = mock(AnalyzerCorrection.class);
            when(correction.editScope(FindingResolution.PANEL)).thenReturn(switch (analyzer) {
                case "single-answer" -> List.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS, ExercisePart.MINI_THEORY, ExercisePart.TRANSLATION);
                case "quiz-instruction" -> List.of(ExercisePart.INSTRUCTIONS, ExercisePart.STATEMENT);
                case "translation-sense" -> List.of(ExercisePart.TRANSLATION);
                default -> List.of(ExercisePart.OPTIONS);
            });
            when(corrections.find(analyzer)).thenReturn(Optional.of(correction));
        }
        service = new DefaultSelectiveCorrectionService(config, ledger, panel, reviews, c -> "context");
    }
    Request req(String action, String id, List<String> tasks, Integer cap, boolean approved) {
        return new Request(action, "batch", "multiple-choice-quality", id, action.equals("correct") ? "analysis" : null,
                5, "context", "v1", cap, tasks, approved, "human");
    }
    State analyze() { return service.execute(req("analyze", "analysis", List.of(), 2, false)); }
    Request singleReq(String action, String id, List<String> tasks, Integer cap, boolean approved) {
        return new Request(action, "batch", "single-answer", id, action.equals("correct") ? "single-analysis" : null,
                5, "context", "v1", cap, tasks, approved, "human");
    }
    List<RefinementTask> singleTasks() {
        var tasks = new ArrayList<RefinementTask>();
        for (String answer : List.of("busy", "happy", "heavy", "old", "tall")) {
            Finding f = new Finding("single-answer", "different-valid-answer", new FindingNodeRef(AuditTarget.QUIZ, quiz, quiz),
                    FindingSeverity.MEDIUM, new FindingEvidence(List.of(), "«" + answer + "» también forma una respuesta válida", List.of()),
                    FindingResolution.PANEL, null, new FindingIdentity("single-" + answer, answer));
            tasks.add(TandaFixture.task(answer, f, topic, CefrLevel.A1));
        }
        return tasks;
    }
    State singleAnalysis() {
        when(refiner.plan(any(), anyString())).thenReturn(TandaFixture.plan(singleTasks().toArray(RefinementTask[]::new)));
        return service.execute(singleReq("analyze", "single-analysis", List.of(), 2, false));
    }
    @Test void bothStatesShareOneCapReuseOnlyFreshSharedInputsAndDoNotAutomaticallyReplay() {
        var current = mock(CurrentAnalysisService.class);
        when(current.supportedAnalyzers()).thenReturn(Set.of("multiple-choice-quality"));
        when(current.reanalysisScope(anyString(), any(), any(), any())).thenReturn(
                new CurrentAnalysisService.ReanalysisScope(Set.of("shared"), Set.of(quiz), 1, 1));
        doAnswer(i -> ((java.util.function.Supplier<?>)i.getArgument(1)).get()).when(store).withAnalysisLock(anyString(),any());
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAuditReportStore(audits); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setCurrentAnalysisService(current);
        var joint = new DefaultSelectiveCorrectionService(config,ledger,panel,reviews,c -> "context");
        var estimate = joint.execute(new Request("estimate","batch","multiple-choice-quality",null,null,null,null,null,null,List.of(),false,"human","both",true));
        assertEquals(3,estimate.pendingQueries()); assertEquals(2,estimate.scopes().size());
        assertEquals(1,estimate.scopes().get(1).sharedQuizzes());
        var request = new Request("reanalyze","batch","multiple-choice-quality","joint",null,5,estimate.fingerprint(),"v1",2,List.of(),false,"human","both",true);
        var result = joint.execute(request);
        var operation = result.runs().stream().filter(x -> x.id().equals("joint")).findFirst().orElseThrow();
        assertEquals(2,operation.spent()); assertEquals(12,batch.getQueriesSpent());
        assertTrue(result.runs().stream().anyMatch(x -> x.id().equals("joint-base")));
        verify(engine).runAudit(any(),argThat(s -> s.getPolicies().get("multiple-choice-quality").getMaxNewEvaluations()==1
                && s.getPolicies().get("multiple-choice-quality").getPurpose()==EvaluationRunPurpose.REANALYSIS
                && s.getPolicies().get("multiple-choice-quality").getReevaluationSubjectIds().equals(Set.of(quiz))));
        clearInvocations(engine);
        joint.execute(request);
        verifyNoInteractions(engine);
    }
    @Test void explicitReanalysisForcesNewVerdictsAndFinishedStateRetainsExamStanding() {
        var state = service.execute(req("reanalyze", "new-version", List.of(), 2, false));
        assertTrue(state.allowed());
        verify(engine, times(2)).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("multiple-choice-quality"))
                && s.getPolicies().get("multiple-choice-quality").isReevaluate()));
        assertEquals("reanalyze", state.runs().get(0).action());
    }
    @Test void independentEvaluatorBudgetsCannotSpendMoreThanTheSingleReservedAllowance() {
        quality.setJudgeName("independent-evaluators");
        var current = mock(CurrentAnalysisService.class); when(current.supportedAnalyzers()).thenReturn(Set.of("independent-evaluators"));
        // Other judges can be analyzed without enabling their selective correction.
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAuditReportStore(audits); config.setRefinementPlanStore(plans); config.setRefinerEngine(refiner);
        config.setCurrentAnalysisService(current);
        when(engine.runAudit(any(), any())).thenAnswer(i -> {
            var selection = i.getArgument(1, AnalyzerRunSelection.class);
            var c = new EvaluationCoverage(); c.setEvaluatedInRun(selection.getPolicies().get("independent-evaluators").getMaxNewEvaluations() > 0 ? 1 : 0);
            quality.setEvaluators(List.of(new EvaluatorCoverage("english", c, "v1"), new EvaluatorCoverage("translation", c, "v1")));
            return report;
        });
        var generic = new DefaultSelectiveCorrectionService(config, ledger, panel, reviews, c -> "context");
        var request = new Request("reanalyze", "batch", "independent-evaluators", "two-evaluators", null, 5, "context", "v1", 3, List.of(), false, "human");
        var result = generic.execute(request);
        assertEquals(2, result.runs().get(0).spent()); assertEquals(3, result.runs().get(0).reserved());
        verify(engine).runAudit(any(), argThat(s -> s.getPolicies().get("independent-evaluators").getMaxNewEvaluations() == 1));
    }

    @Test void actualTranslationAnalyzerReceivesTheWholeSharedBudgetAndKeepsItsVerdictPairAtomic() {
        for (int cap : List.of(1, 2, 3)) {
            boolean[] recorded = {false, false};
            var runtime = mock(JudgeRuntime.class);
            when(runtime.plan(anyList(), any())).thenAnswer(i -> {
                List<AuditNode> nodes = i.getArgument(0);
                AuditNode target = nodes.stream().filter(n -> quiz.equals(n.getEntity().getId())).findFirst().orElseThrow();
                return new JudgeRequestPlan(List.of(target), List.of(EvaluationRequestMode.FORCE), List.of());
            });
            when(runtime.open(anyString(), anyList(), any())).thenAnswer(i -> {
                EvaluationRunPolicy policy = i.getArgument(2);
                var sessions = new ArrayList<EvaluationSession>();
                for (int evaluator = 0; evaluator < 2; evaluator++) {
                    int index = evaluator;
                    int[] tally = {0, 0, 0};
                    var session = mock(EvaluationSession.class);
                    when(session.recorded(any())).thenAnswer(call -> recorded[index]);
                    when(session.resolveAll(anyList(), anyInt())).thenAnswer(call -> {
                        List<EvaluationRequest> requests = call.getArgument(0);
                        var answers = new ArrayList<EvaluationResolution>();
                        for (EvaluationRequest request : requests) {
                            boolean consults = request.getMode() != EvaluationRequestMode.REUSE_ONLY;
                            if (consults) { tally[0]++; recorded[index] = true; }
                            else if (recorded[index]) tally[1]++;
                            else tally[2]++;
                            answers.add(new EvaluationResolution(consults ? EvaluationResolutionKind.EVALUATED
                                    : recorded[index] ? EvaluationResolutionKind.REUSED : EvaluationResolutionKind.PENDING,
                                    recorded[index] ? "passes" : null, "v1", null));
                        }
                        return answers;
                    });
                    when(session.coverage()).thenAnswer(call -> new EvaluationCoverage(1, tally[0] + tally[1], tally[0], tally[1], tally[2], 0));
                    sessions.add(session);
                }
                return new JudgeSessions("translation-sense", "v1", List.of("english", "translation"), sessions,
                        translation.getStanding(), policy, translation.getStanding().getRuntimeIdentity(), Set.of("v1"));
            });
            when(runtime.coverage(any(), any())).thenAnswer(i -> {
                JudgeSessions opened = i.getArgument(0); JudgeUnitTally tally = i.getArgument(1);
                var c = coverage("translation-sense");
                c.setReached(tally.getReached()); c.setEvaluated(tally.getEvaluated()); c.setNotEvaluated(tally.getNotEvaluated());
                var evaluators = new ArrayList<EvaluatorCoverage>(); int pending = 0;
                for (int j = 0; j < opened.getSessions().size(); j++) {
                    EvaluationCoverage evaluation = opened.getSessions().get(j).coverage(); pending += evaluation.getPending();
                    evaluators.add(new EvaluatorCoverage(opened.getEvaluatorIds().get(j), evaluation, "v1"));
                }
                c.setEvaluators(evaluators); c.setCost(new JudgeCostEstimate(tally.getNotEvaluated(), pending, 0, false, 0, 1, 0));
                return c;
            });
            var provider = new TranslationSenseAnalyzerProvider(runtime, mock(Evaluator.class), mock(Evaluator.class),
                    payload -> new TranslationSenseVerdict(true, List.of(), "Mismo significado"), mock(TranslationSenseConfig.class),
                    new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
            when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
                var selection = i.getArgument(1, AnalyzerRunSelection.class);
                AuditReport result = TandaFixture.report(course, List.of(), Map.of(), List.of());
                var analyzer = provider.create(selection.getPolicies().get("translation-sense"));
                var nodes = new ArrayList<>(List.of(result.getRoot()));
                while (!nodes.isEmpty()) {
                    AuditNode node = nodes.remove(0);
                    if (node.getTarget() == AuditTarget.QUIZ) analyzer.onQuiz(node);
                    nodes.addAll(BatchBook.list(node.getChildren()));
                }
                analyzer.onCourseComplete(result.getRoot());
                return result;
            });
            clearInvocations(engine);
            State state = service.execute(new Request("reanalyze", "batch", "translation-sense", "paired-" + cap, null,
                    5, "context", "v1", cap, List.of(), false, "human"));
            Run run = state.runs().get(state.runs().size() - 1);
            assertEquals(cap, run.reserved()); assertEquals(cap < 2 ? 0 : 2, run.spent());
            assertEquals(cap < 2 ? "PARTIAL" : "DONE", run.status());
            assertEquals(cap < 2 ? 1 : 0, run.missing()); assertTrue(run.spent() <= run.reserved());
            verify(engine).runAudit(any(), argThat(s -> s.getPolicies().get("translation-sense").getMaxNewEvaluations() == cap));
            verifyNoInteractions(panel, factory);
        }
        assertEquals(14, batch.getQueriesSpent()); verify(repository, never()).save(any(), any());
    }
    @Test void estimateUsesOnlySelectedJudgeAtZeroBudgetAndDoesNotWriteOrStartPanel() {
        assertTrue(service.execute(req("estimate", null, List.of(), null, false)).allowed());
        verify(engine).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("multiple-choice-quality"))
                && s.getPolicies().get("multiple-choice-quality").getMaxNewEvaluations() == 0));
        verify(store, never()).save(any()); verify(store, never()).saveIfVersion(any(), anyInt(), any());
        verifyNoInteractions(audits, plans, panel, factory); verify(repository, never()).save(any(), any());
    }
    /** Pragmatism reached 2 exercises, scored 1 and abstained in the other: its coverage says 1 not evaluated. */
    @Test @Tag("FEAT-ALDIA") @Tag("F-ALDIA-R003")
    void aSavedAbstentionCountsAsEvaluatedInTheEstimateAndInTheRunSummary() {
        var pragmatism = coverage("pragmatism"); pragmatism.setUnit(AuditTarget.QUIZ);
        var coverages = new ArrayList<>(((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).getJudgeCoverages());
        coverages.add(pragmatism);
        ((DefaultCourseDiagnoses) report.getRoot().getDiagnoses()).setJudgeCoverages(coverages);
        var node = com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(report.getRoot()).get(quiz);
        if (!(node.getDiagnoses() instanceof DefaultQuizDiagnoses)) node.setDiagnoses(new DefaultQuizDiagnoses());
        var verdict = new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict(List.of(
                new com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Assessment("stop - stopping",
                        com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict.Status.NOT_APPLICABLE, "", null, null,
                        null, "Dos formas de un verbo, no una oración.", null)), "No es una oración.");
        ((DefaultQuizDiagnoses) node.getDiagnoses()).setPragmatismDiagnosis(
                new com.learney.contentaudit.auditdomain.pragmatism.PragmatismDiagnosis(verdict, "v1", true));
        node.getUnevaluatedBy().add("pragmatism");
        var current = mock(CurrentAnalysisService.class);
        when(current.supportedAnalyzers()).thenReturn(Set.of("pragmatism"));
        config.setCurrentAnalysisService(current);

        var estimate = service.execute(new Request("estimate", "batch", "pragmatism", null, null, 5, "context", "v1", null,
                List.of(), false, "human"));
        assertEquals(2, estimate.reached());
        assertEquals(2, estimate.evaluated());
        assertEquals(0, estimate.missing());

        var state = service.execute(new Request("analyze", "batch", "pragmatism", "pragmatism-analysis", null, 5, "context", "v1",
                2, List.of(), false, "human"));
        var run = state.runs().stream().filter(r -> r.id().equals("pragmatism-analysis")).findFirst().orElseThrow();
        assertEquals(2, run.evaluated());
        assertEquals(0, run.missing());
        assertEquals("DONE", run.status());

        // A pending exercise without a verdict is still missing.
        ((DefaultQuizDiagnoses) node.getDiagnoses()).setPragmatismDiagnosis(null);
        var pending = service.execute(new Request("estimate", "batch", "pragmatism", null, null, 5, "context", "v1", null,
                List.of(), false, "human"));
        assertEquals(1, pending.evaluated());
        assertEquals(1, pending.missing());
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
    /** Miniteoría approved with Gemini; the estimate asked with ChatGPT says with which motor it could. */
    @Test @Tag("FEAT-ALDIA") @Tag("F-ALDIA-R007")
    void anEstimateWithAMotorTheJudgeDidNotApproveWithSaysTheMotorsItIsEnabledWith() {
        var gemini = new com.learney.contentaudit.auditdomain.judging.JudgeEnablement(
                com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity.explicit("agy", "gemini-3.8-flash-high", "high"),
                com.learney.contentaudit.auditdomain.judging.JudgeExamStatus.APPROVED, java.time.Instant.parse("2026-10-19T14:30:58Z"));
        quality.getStanding().setConsultsOnCourse(false);
        quality.getStanding().setReason("La versión vigente del juez no rindió el examen fijo.");
        quality.getStanding().setEnabledWith(List.of(gemini));
        var estimate = service.execute(req("estimate", null, List.of(), null, false));
        assertFalse(estimate.allowed(), "a motor that did not approve does not measure");
        assertEquals(List.of(gemini), estimate.enabledWith());
        verify(store, never()).save(any());
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

    @Test void fiveSingleAnswerFindingsOnOneQuizReachOneReviewProposalWithSentenceAndMiniTheoryTogether() {
        var tasks = singleTasks();
        var ranking = TandaFixture.task("rank", new Finding("single-answer", "rank-only", new FindingNodeRef(AuditTarget.QUIZ, quiz, quiz),
                FindingSeverity.LOW, null, FindingResolution.RANK_ONLY, null, new FindingIdentity("rank", null)), topic, CefrLevel.A1);
        when(refiner.plan(any(), anyString())).thenReturn(TandaFixture.plan(tasks.get(0), tasks.get(1), tasks.get(2), tasks.get(3), tasks.get(4), ranking));
        var artifacts = new TandaFixture.Artifacts();
        var nativeLedger = new DefaultBatchChangeLedger(artifacts, TandaFixture.locator());
        String otherQuiz = com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.COME;
        var previous = TandaFixture.step(TandaFixture.edited(TandaFixture.exercise(course, otherQuiz),
                q -> q.setTranslation("Mi abuela viene todos los años.")), ExercisePart.TRANSLATION);
        nativeLedger.record(batch, TandaFixture.change("previous", BatchChangeOrigin.PANEL, null, List.of("unrelated"), previous), course);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(),
                List.of(new BatchReview.Item("previous", previous, previous.getAfter(), previous.getAfter(), "PANEL", null,
                        BatchReviewDecision.Status.APPROVED, "human", null)), List.of(), List.of()));
        service = new DefaultSelectiveCorrectionService(config, nativeLedger, panel, reviews, c -> "context", "codex-cli/gpt-6.1-sol/medium");
        var analysis = service.execute(singleReq("analyze", "single-analysis", List.of(), 2, false));
        List<String> ids = analysis.runs().get(0).taskIds();
        assertEquals(List.of("selective-single-analysis-busy", "selective-single-analysis-happy", "selective-single-analysis-heavy",
                "selective-single-analysis-old", "selective-single-analysis-tall"), ids);
        assertEquals(List.of(quiz), analysis.runs().get(0).quizIds());
        assertEquals(5, analysis.tasks().size(), "ranking evidence never becomes a correction task");
        doAnswer(i -> {
            CorrectionBatch current = i.getArgument(0);
            assertEquals(new HashSet<>(ids), i.getArgument(4));
            assertTrue(i.getArgument(5, List.class).contains("single-answer"));
            var after = com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(TandaFixture.exercise(course, quiz),
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("How"),
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("is"),
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("she feeling today?"));
            after.setMiniTheory("Con she usamos is. Preguntamos por cómo se siente hoy: How is she feeling today?");
            after.setTranslation("¿Cómo se siente ella hoy?");
            var step = TandaFixture.step(after, ExercisePart.STATEMENT, ExercisePart.ANSWERS,
                    ExercisePart.MINI_THEORY, ExercisePart.TRANSLATION);
            nativeLedger.record(current, TandaFixture.change("single-repair", BatchChangeOrigin.PANEL, null, ids, step), course);
            for (String id : ids) BatchBook.setOutcome(current, id, BatchTaskOutcomeKind.RESOLVED_BY_PANEL, "single-repair", 1, null);
            when(allowance.spent()).thenReturn(2); current.setQueriesSpent(current.getQueriesSpent() + 2);
            return current;
        }).when(panel).runSelected(any(), any(), any(), any(), any(), any());
        clearInvocations(engine, factory, panel);
        var request = singleReq("correct", "single-repair-operation", ids, 3, false);
        State state = service.execute(request);
        var run = state.runs().get(1);
        assertEquals("single-answer", run.analyzer()); assertEquals("single-analysis", run.sourceRunId());
        assertEquals(3, run.reserved()); assertEquals(2, run.spent()); assertEquals(13, batch.getQueriesSpent());
        assertEquals("v1", run.judgeVersion());
        assertEquals(Objects.toString(single.getStanding().getRuntimeIdentity(), ""), run.runtime());
        assertEquals("codex-cli/gpt-6.1-sol/medium", run.correctorRuntime());
        assertEquals(ids, run.taskIds()); assertEquals(List.of(quiz), run.quizIds());
        assertEquals(6, run.resultContentVersion()); assertEquals("context", run.resultFingerprint());
        assertEquals(6, batch.getContentVersion()); assertEquals(1, batch.getCorrectionRevision());
        assertSame(closing, batch.getClosings().get(0)); assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "unrelated"));
        assertEquals(List.of("previous", "single-repair"), batch.getChanges().stream().map(BatchChange::getChangeId).toList());
        assertNull(batch.getChanges().get(0).getDroppedReason());
        var pending = artifacts.pending().stream().filter(a -> a.getProposal().getNodeId().equals(quiz)).toList();
        assertEquals(1, pending.size()); assertEquals(RevisionVerdict.PENDING_APPROVAL, pending.get(0).getVerdict());
        var after = pending.get(0).getProposal().getElementAfter().getQuiz();
        assertEquals("she feeling today?", after.getForm().getSentenceParts().get(2).getText());
        assertTrue(after.getMiniTheory().contains("Con she usamos is"));
        assertEquals(5, state.tasks().stream().filter(Task::attempted).count());
        verify(factory).allowance(3, 1);
        verify(engine, times(2)).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("single-answer"))
                && s.getPolicies().get("single-answer").getMaxNewEvaluations() == 0));
        verify(repository, never()).save(any(), any());
        clearInvocations(engine, factory, panel, audits, refiner, plans, store);
        batch.setContentVersion(7); service.execute(request);
        verifyNoInteractions(engine, factory, panel, refiner);
        verify(audits).load(run.auditId()); verify(audits, never()).save(any()); verifyNoMoreInteractions(audits);
        verify(plans, never()).save(any()); verify(store, never()).save(any());
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
        assertEquals(13, batch.getQueriesSpent()); assertEquals(2, batch.getChanges().size());
    }

    @Test void singleAnswerCorrectionRejectsAnotherAnalyzersMeasurementAndForeignOrRankOnlyTasksBeforeReservation() {
        analyze();
        var wrongSource = new Request("correct", "batch", "single-answer", "wrong-source", "analysis", 5, "context", "v1", 3,
                List.of("selective-analysis-quality"), false, "human");
        assertThrows(IllegalArgumentException.class, () -> service.execute(wrongSource));
        var state = singleAnalysis(); List<String> ids = state.runs().get(1).taskIds();
        clearInvocations(store, factory, panel);
        var selectedPlan = savedPlans.get("selective-single-analysis");
        selectedPlan.getTasks().get(0).getOrigin().getFinding().setAnalyzer("multiple-choice-quality");
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "foreign", ids, 3, false)));
        selectedPlan.getTasks().get(0).getOrigin().getFinding().setAnalyzer("single-answer");
        selectedPlan.getTasks().get(0).getOrigin().getFinding().setResolution(FindingResolution.RANK_ONLY);
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "ranking", ids, 3, false)));
        verify(store, never()).saveIfVersion(any(), anyInt(), any()); verify(store, never()).save(any());
        verifyNoInteractions(factory, panel);
    }

    @Test void singleAnswerCorrectionRequiresDeclaredScopeFreshVersionBudgetAndExplicitApprovalInclusion() {
        var state = singleAnalysis(); List<String> ids = state.runs().get(0).taskIds();
        var step = TandaFixture.step(TandaFixture.exercise(course, quiz), ExercisePart.MINI_THEORY);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(),
                List.of(new BatchReview.Item("old", step, step.getAfter(), step.getAfter(), "PANEL", null,
                        BatchReviewDecision.Status.APPROVED, "human", null)), List.of(), List.of()));
        clearInvocations(store, factory, panel);
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "no-cap", ids, null, true)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "negative-cap", ids, -1, true)));
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "protected", ids, 3, false)));
        single.getStanding().setJudgeVersion("v2");
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "changed-judge", ids, 3, true)));
        single.getStanding().setJudgeVersion("v1"); batch.setContentVersion(6);
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "stale", ids, 3, true)));
        batch.setContentVersion(5); single.getStanding().setConsultsOnCourse(false);
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "blocked-exam", ids, 3, true)));
        single.getStanding().setConsultsOnCourse(true);
        when(corrections.find("single-answer")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.execute(singleReq("correct", "no-scope", ids, 3, true)));
        verify(store, never()).saveIfVersion(any(), anyInt(), any()); verify(store, never()).save(any());
        verifyNoInteractions(factory, panel);
    }

    @Test void whenTranslationCorrectionValidatesInstructionsAndNearDuplicatesWhilePreservingReturnedTasksAndOtherApprovals() {
        var original = TandaFixture.exercise(course, quiz);
        original.getForm().setSentenceParts(new ArrayList<>(List.of(
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("When"),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("is"),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("she?"))));
        original.setTranslation("¿Cuándo está ella?");
        Finding wrongInstruction = new Finding("quiz-instruction", "incomplete-question", new FindingNodeRef(AuditTarget.QUIZ, quiz, "When is she?"),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(), "La pregunta necesita completar qué situación tiene fecha u horario.", List.of()),
                FindingResolution.PANEL, null, new FindingIdentity("when-instruction", null));
        Finding wrongTranslation = new Finding("translation-sense", "different-meaning", new FindingNodeRef(AuditTarget.QUIZ, quiz, "When is she?"),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(), "El inglés no completa la situación que expresa la traducción.", List.of()),
                FindingResolution.PANEL, null, new FindingIdentity("when-translation", null));
        var oldInstruction = TandaFixture.task("task-1312", wrongInstruction, topic, CefrLevel.A1);
        var oldTranslation = TandaFixture.task("task-1973", wrongTranslation, topic, CefrLevel.A1);
        var originalPlan = savedPlans.get(batch.getPlanId());
        originalPlan.setTasks(new ArrayList<>(List.of(originalPlan.getTasks().get(0), oldInstruction, oldTranslation)));
        batch.setTaskIds(new ArrayList<>(List.of("unrelated", "task-1312", "task-1973")));
        String duplicateReason = "near-duplicates: When is she at home? repite otro ejercicio del tema";
        BatchBook.setOutcome(batch, "task-1312", BatchTaskOutcomeKind.PENDING, null, 2, duplicateReason);
        BatchBook.setOutcome(batch, "task-1973", BatchTaskOutcomeKind.PENDING, null, 2, duplicateReason);
        var validation = BatchBook.copy(batch.getAnalyzers());
        for (String analyzer : List.of("translation-sense", "quiz-instruction", "near-duplicates"))
            if (!validation.contains(analyzer)) validation.add(analyzer);
        batch.setAnalyzers(validation);
        when(refiner.plan(any(), anyString())).thenReturn(TandaFixture.plan(
                TandaFixture.task("task-1973", wrongTranslation, topic, CefrLevel.A1),
                TandaFixture.task("task-1312", wrongInstruction, topic, CefrLevel.A1)));
        var artifacts = new TandaFixture.Artifacts();
        var nativeLedger = new DefaultBatchChangeLedger(artifacts, TandaFixture.locator());
        String otherQuiz = com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.COME;
        var previous = TandaFixture.step(TandaFixture.edited(TandaFixture.exercise(course, otherQuiz),
                q -> q.setTranslation("Mi abuela viene todos los años.")), ExercisePart.TRANSLATION);
        nativeLedger.record(batch, TandaFixture.change("previous", BatchChangeOrigin.PANEL, null, List.of("unrelated"), previous), course);
        var rejectedQuiz = com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(original,
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("When"),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("is"),
                com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("she at home?"));
        var returned = TandaFixture.change("returned-when", BatchChangeOrigin.PANEL, null, List.of("task-1312", "task-1973"),
                TandaFixture.step(rejectedQuiz, ExercisePart.STATEMENT));
        returned.setDroppedReason(DefaultBatchCloser.RETURNED_BY_GATE + duplicateReason);
        var changes = BatchBook.copy(batch.getChanges()); changes.add(returned); batch.setChanges(changes);
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(),
                List.of(new BatchReview.Item("previous", previous, previous.getAfter(), previous.getAfter(), "PANEL", null,
                        BatchReviewDecision.Status.APPROVED, "human", null)), List.of(), List.of()));
        service = new DefaultSelectiveCorrectionService(config, nativeLedger, panel, reviews, c -> "context", "codex-cli/gpt-6.1-sol/medium");
        var analysis = service.execute(new Request("analyze", "batch", "translation-sense", "when-analysis", null,
                5, "context", "v1", 2, List.of(), false, "human"));
        assertEquals(List.of("selective-when-analysis-task-1973"), analysis.runs().get(0).taskIds());
        assertEquals(List.of(quiz), analysis.runs().get(0).quizIds());
        assertEquals("task-1312", oldInstruction.getId()); assertEquals("task-1973", oldTranslation.getId());
        List<String> ids = analysis.runs().get(0).taskIds();
        doAnswer(i -> {
            CorrectionBatch current = i.getArgument(0);
            assertEquals(new HashSet<>(ids), i.getArgument(4));
            List<String> analyzers = i.getArgument(5);
            assertTrue(analyzers.containsAll(List.of("translation-sense", "quiz-instruction", "near-duplicates")));
            var after = com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts(original,
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("When"),
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze("is"),
                    com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text("she ready for work?"));
            after.setTranslation("¿Cuándo está ella lista para trabajar?");
            nativeLedger.record(current, TandaFixture.change("when-repair", BatchChangeOrigin.PANEL, null, ids,
                    TandaFixture.step(after, ExercisePart.STATEMENT, ExercisePart.TRANSLATION)), course);
            BatchBook.setOutcome(current, ids.get(0), BatchTaskOutcomeKind.RESOLVED_BY_PANEL, "when-repair", 1, null);
            when(allowance.spent()).thenReturn(1); current.setQueriesSpent(current.getQueriesSpent() + 1);
            return current;
        }).when(panel).runSelected(any(), any(), any(), any(), any(), any());
        clearInvocations(engine, factory, panel);
        var correction = new Request("correct", "batch", "translation-sense", "when-repair-operation", "when-analysis",
                5, "context", "v1", 4, ids, false, "human");
        State state = service.execute(correction);
        assertEquals("translation-sense", state.runs().get(1).analyzer());
        assertEquals("when-analysis", state.runs().get(1).sourceRunId());
        assertEquals(4, state.runs().get(1).reserved()); assertEquals(1, state.runs().get(1).spent());
        assertEquals(12, batch.getQueriesSpent());
        assertSame(closing, batch.getClosings().get(0)); assertEquals(1, batch.getCorrectionRevision());
        assertEquals(6, batch.getContentVersion()); assertEquals(BatchStatus.CLOSED, batch.getStatus());
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "task-1312"));
        assertEquals(BatchTaskOutcomeKind.PENDING, BatchBook.kind(batch, "task-1973"));
        assertEquals(2, BatchBook.outcome(batch, "task-1312").orElseThrow().getAttempts());
        assertEquals(duplicateReason, BatchBook.outcome(batch, "task-1973").orElseThrow().getDetail());
        assertNull(batch.getChanges().get(0).getDroppedReason());
        assertSame(returned, batch.getChanges().get(1)); assertEquals(DefaultBatchCloser.RETURNED_BY_GATE + duplicateReason, returned.getDroppedReason());
        assertEquals(1, artifacts.pending().stream().filter(a -> quiz.equals(a.getProposal().getNodeId())).count());
        verify(factory).allowance(4, 1);
        verify(engine, times(2)).runAudit(any(), argThat(s -> s.getAnalyzers().equals(List.of("translation-sense"))
                && s.getPolicies().get("translation-sense").getMaxNewEvaluations() == 0));
        verify(repository, never()).save(any(), any());
    }

    @Test void instructionAndTranslationCorrectionStillRequireDeclaredPanelScopeAndExactMeasurementOrigin() {
        for (String analyzer : List.of("quiz-instruction", "translation-sense")) {
            var correction = mock(AnalyzerCorrection.class);
            when(correction.editScope(FindingResolution.PANEL)).thenReturn(List.of());
            when(corrections.find(analyzer)).thenReturn(Optional.of(correction));
            var request = new Request("correct", "batch", analyzer, analyzer + "-disabled", "analysis", 5, "context", "v1", 2,
                    List.of("selective-analysis-quality"), false, "human");
            assertThrows(IllegalArgumentException.class, () -> service.execute(request));
            when(correction.editScope(FindingResolution.PANEL)).thenReturn(List.of(ExercisePart.INSTRUCTIONS));
        }
        analyze();
        clearInvocations(factory, panel, store);
        for (String analyzer : List.of("quiz-instruction", "translation-sense")) {
            var request = new Request("correct", "batch", analyzer, analyzer + "-wrong-source", "analysis", 5, "context", "v1", 2,
                    List.of("selective-analysis-quality"), false, "human");
            assertThrows(IllegalArgumentException.class, () -> service.execute(request));
        }
        verifyNoInteractions(factory, panel); verify(store, never()).saveIfVersion(any(), anyInt(), any());
        verify(store, never()).save(any());
    }
}
