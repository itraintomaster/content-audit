package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerRunSelection;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationCoverage;
import com.learney.contentaudit.revisiondomain.batch.*;
import com.learney.contentaudit.revisiondomain.batch.ContextOpinion.Request;
import com.learney.contentaudit.revisiondomain.panelengine.MesaFixture;
import java.util.*;
import org.junit.jupiter.api.*;

/**
 * F-OPINA-R001 and R002: «Completar opiniones» asks exactly the list of what is missing, and says pairs, exercises
 * and queries apart. The judge measures by topic: one query covers every pair of a topic.
 */
class FOpinaMissingOpinionsTest {
    static final String JUDGE = "judge";
    final CourseEntity course = course();
    final CorrectionBatch batch = TandaFixture.batch("batch");
    final BatchStore store = mock(BatchStore.class);
    final BatchReviewService reviews = mock(BatchReviewService.class);
    final CourseRepository repository = mock(CourseRepository.class);
    final CourseMapper mapper = mock(CourseMapper.class);
    final AuditEngine engine = mock(AuditEngine.class);
    final AnalyzerCatalog catalog = mock(AnalyzerCatalog.class);
    final BatchChangeLedger ledger = mock(BatchChangeLedger.class);
    final CurrentAnalysisService analysis = mock(CurrentAnalysisService.class);
    final List<CourseEntity> mapped = new ArrayList<>();
    AuditReport current;
    DefaultContextOpinionService service;

    /** One topic of A1 with two knowledges of five exercises each: two topics for the judge. */
    static CourseEntity course() {
        var knowledges = new ArrayList<KnowledgeEntity>();
        for (int k = 1; k <= 2; k++) {
            var quizzes = new ArrayList<QuizTemplateEntity>();
            for (int q = 1; q <= 5; q++) {
                var quiz = MesaFixture.quiz(String.format("q%02d", (k - 1) * 5 + q), "Traducción.",
                        MesaFixture.text("I"), MesaFixture.cloze("work"), MesaFixture.text("here."));
                quiz.setKnowledgeId("k" + k);
                quizzes.add(quiz);
            }
            knowledges.add(MesaFixture.topic("k" + k, "Tema " + k, quizzes));
        }
        var topic = new TopicEntity("t-present", null, NodeKind.TOPIC, "Present Simple", null, "m-a1", null, null, 0, null,
                knowledges);
        var a1 = new MilestoneEntity("m-a1", null, NodeKind.MILESTONE, "A1", null, "root", null, 0, null,
                new ArrayList<>(List.of(topic)));
        return new CourseEntity("curso", "Learney", null, new RootNodeEntity("root", null, NodeKind.ROOT, "Curso", null,
                new ArrayList<>(List.of(a1))), null);
    }

    static List<String> ids(CourseEntity course) {
        return course.getRoot().getMilestones().stream().flatMap(m -> m.getTopics().stream())
                .flatMap(t -> t.getKnowledges().stream()).flatMap(k -> k.getQuizTemplates().stream())
                .map(QuizTemplateEntity::getId).sorted().toList();
    }

    static long knowledges(CourseEntity course) {
        return course.getRoot().getMilestones().stream().flatMap(m -> m.getTopics().stream())
                .flatMap(t -> t.getKnowledges().stream()).filter(k -> !k.getQuizTemplates().isEmpty()).count();
    }

    @BeforeEach void setup() {
        batch.setStatus(BatchStatus.CLOSED); batch.setContentVersion(5); batch.setCorrectionRevision(1);
        when(store.load("batch")).thenReturn(Optional.of(batch));
        when(reviews.read("batch")).thenReturn(new BatchReview("batch", 1, 5, null, List.of(), List.of(), List.of(), List.of(), List.of()));
        when(repository.load(TandaFixture.COURSE_PATH)).thenReturn(course);
        when(ledger.composedCourse(any(), any())).thenReturn(course);
        when(mapper.map(any())).thenAnswer(i -> { mapped.add(i.getArgument(0)); return new AuditableCourse(List.of()); });
        var descriptor = new AnalyzerDescriptor(); descriptor.setName(JUDGE); descriptor.setDisplayName("Juez por tema");
        descriptor.setCost(AnalysisCost.PAID_MODEL);
        when(catalog.list()).thenReturn(List.of(descriptor));
        when(engine.runAudit(any(), any(AnalyzerRunSelection.class))).thenAnswer(i -> {
            var cut = mapped.get(mapped.size() - 1);
            var r = TandaFixture.report(cut, List.of(), Map.of(), List.of(JUDGE));
            var coverage = new JudgeCoverageDiagnosis(); coverage.setJudgeName(JUDGE); coverage.setUnit(AuditTarget.KNOWLEDGE);
            var standing = new JudgeExamStanding(); standing.setConsultsOnCourse(true); standing.setJudgeVersion("v1");
            coverage.setStanding(standing);
            int topics = (int) knowledges(cut);
            coverage.setCost(new JudgeCostEstimate(topics, topics, 0, false, 0, 1, 0));
            coverage.setEvaluators(List.of(new EvaluatorCoverage(JUDGE, new EvaluationCoverage(topics, 0, 0, 0, topics, 0), "v1")));
            ((DefaultCourseDiagnoses) r.getRoot().getDiagnoses()).setJudgeCoverages(List.of(coverage));
            return r;
        });
        current = TandaFixture.report(DefaultCurrentAnalysisService.cut(course, batch.getLevel()), List.of(), Map.of(), List.of(JUDGE));
        when(analysis.supportedAnalyzers()).thenReturn(Set.of(JUDGE));
        when(analysis.report(any(), eq("correction"), any())).thenAnswer(i -> current);
        var config = new BatchServiceConfig(); config.setBatchStore(store); config.setCourseRepository(repository);
        config.setCoursePath(TandaFixture.COURSE_PATH); config.setCourseMapper(mapper); config.setAuditEngine(engine);
        config.setAnalyzerCatalog(catalog); config.setCurrentAnalysisService(analysis);
        service = new DefaultContextOpinionService(config, ledger, reviews, c -> "content");
    }

    ContextOpinion.State completar() {
        return service.execute(new Request("estimate", "batch", null, false, null, null, null, null, null, "correction"));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R002")
    void aJudgeOfTopicsWithTwoTopicsAndTenExercisesIsTenPairsAndTwoQueries() {
        var estimate = completar();

        assertEquals(10, estimate.pendingPairs());
        assertEquals(10, estimate.pendingExercises());
        assertEquals(2, estimate.pendingQueries(), "One query per topic");
        assertEquals(2, estimate.judges().get(0).topics());
        assertEquals(ids(course), estimate.selection().get(JUDGE));
        verify(store, never()).saveIfVersion(any(), anyInt(), any());
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R001")
    void completarAsksExactlyTheListTheAnalysisSays() {
        var index = com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(current.getRoot());
        index.get("q01").getScores().put(JUDGE, 1.0);
        index.get("q07").getScores().put(JUDGE, 0.4);

        var estimate = completar();

        assertEquals(MissingOpinions.of(current, Set.of(JUDGE)), estimate.selection());
        assertEquals(8, estimate.pendingPairs());
        assertFalse(estimate.selection().get(JUDGE).contains("q01"));
        assertFalse(estimate.selection().get(JUDGE).contains("q07"));
    }

    @Test @Tag("FEAT-OPINA") @Tag("F-OPINA-R002")
    void withNothingMissingCompletarAsksNothing() {
        com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer.index(current.getRoot()).values().stream()
                .filter(n -> n.getTarget() == AuditTarget.QUIZ).forEach(n -> n.getScores().put(JUDGE, 1.0));

        var estimate = completar();

        assertEquals(0, estimate.pendingPairs());
        assertEquals(0, estimate.pendingQueries());
        assertTrue(estimate.selection().isEmpty());
        verify(engine, never()).runAudit(any(), any(AnalyzerRunSelection.class));
    }
}
