package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.labs.AbsenceType;
import com.learney.contentaudit.auditdomain.labs.AbsentLemma;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAndPos;
import com.learney.contentaudit.auditdomain.labs.LemmaPlacementDiagnosis;
import com.learney.contentaudit.auditdomain.labs.MisplacedLemma;
import com.learney.contentaudit.auditdomain.labs.PriorityLevel;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.refinerdomain.CorrectionContext;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceCorrectionContext;
import com.learney.contentaudit.refinerdomain.LemmaAbsenceContextResolver;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.ApprovalMode;
import com.learney.contentaudit.revisiondomain.CourseElementLocator;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.RevisionOutcome;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.revisiondomain.RevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.LemmaAbsenceProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceMvpStrategy;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceGeneratorResponse;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceQuizCandidateGenerator;
import com.learney.contentaudit.revisiondomain.ProposalStrategyFailedException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Journey J001: Flujo feliz end-to-end para LEMMA_ABSENCE con validador auto.
 *
 * Paths:
 *   path-1: strategy activa + candidato producido + apply exitoso → success
 *   path-2: strategy activa + candidato producido + apply fallido → failure
 *   path-3: strategy activa + candidato NO producido → failure
 *   path-4: SIN strategy activa → failure (NO_ACTIVE_STRATEGY)
 *
 * Tests en memoria: construyen un AuditReport programaticamente con un quiz que
 * tiene LemmaPlacementDiagnosis con misplacedLemmas, e invocan el RevisionEngine
 * directamente via DefaultRevisionEngineFactory.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-LAPS")
@Tag("F-LAPS-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FLapsJ001JourneyTest {

    // -----------------------------------------------------------------------
    // Fixture constants
    // -----------------------------------------------------------------------

    private static final String PLAN_ID  = "plan-laps-j001";
    private static final String AUDIT_ID = "audit-laps-j001";
    private static final String TASK_ID  = "task-laps-j001";
    private static final String QUIZ_ID  = "quiz-laps-j001";

    /** DSL for the original quiz (elementBefore). */
    private static final String BEFORE_QUIZ_SENTENCE =
            "She ____ [reads] (read) books about advanced topics.";

    /** DSL emitted by the fake generator (elementAfter — different from before). */
    private static final String AFTER_QUIZ_SENTENCE =
            "She ____ [studies] (study) books every day.";

    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // AuditReport builder helpers (same convention as FRclaqsJ001JourneyTest)
    // -----------------------------------------------------------------------

    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    private AuditNode buildMilestoneNode(AuditNode parent, AuditableMilestone milestone) {
        AbsentLemma absent = new AbsentLemma(
                new LemmaAndPos("advanced", "ADJ"), CefrLevel.B2,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 1500, null);
        LemmaAbsenceLevelDiagnosis levelDx = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 80, 1, 10.0, 90.0, 0.7, 0.6, 0.4,
                null, List.of(absent), List.of(), 1, 2, 4);
        DefaultLevelDiagnoses dx = new DefaultLevelDiagnoses();
        dx.setLemmaAbsenceDiagnosis(levelDx);

        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(milestone);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(dx);
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildTopicNode(AuditNode parent, AuditableTopic topic) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.TOPIC);
        node.setEntity(topic);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(new DefaultTopicDiagnoses());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildKnowledgeNode(AuditNode parent, AuditableKnowledge knowledge) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.KNOWLEDGE);
        node.setEntity(knowledge);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(new DefaultKnowledgeDiagnoses());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildQuizNode(AuditNode parent, AuditableQuiz quiz) {
        MisplacedLemma misplaced = new MisplacedLemma(
                new LemmaAndPos("advanced", "ADJ"),
                CefrLevel.B2, CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, 1500, null);
        DefaultQuizDiagnoses quizDx = new DefaultQuizDiagnoses();
        quizDx.setLemmaAbsenceDiagnosis(new LemmaPlacementDiagnosis(1, List.of(misplaced)));

        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(quizDx);
        parent.getChildren().add(node);
        return node;
    }

    /** Build a full AuditReport rooted at COURSE → MILESTONE → TOPIC → KNOWLEDGE → QUIZ. */
    private AuditReport buildAuditReport() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-laps-j001", "A1", "M001");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-laps-j001", "Reading Comprehension", "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Reading at A1 level", "Write the correct form.", true,
                "knowledge-laps-j001", "Knowledge 1", "K001");
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Quiz 1", "Q001",
                "Ella lee libros sobre temas avanzados.",
                List.of("She reads books about advanced topics."),
                BEFORE_QUIZ_SENTENCE);

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz);
        return new AuditReport(courseNode);
    }

    /** Build the CourseElementSnapshot for the elementBefore quiz. */
    private CourseElementSnapshot buildElementBefore() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", Arrays.asList(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("reads")),
                new SentencePartEntity(SentencePartKind.TEXT, "(read) books about advanced topics.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", "knowledge-laps-j001",
                "She reads books about advanced topics.",
                "Write the correct form.",
                "Ella lee libros sobre temas avanzados.",
                "basics.01.Reading", "Reading Comprehension",
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "");
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz);
    }

    /** Build a RefinementPlan with one LEMMA_ABSENCE task for QUIZ_ID. */
    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Quiz about advanced topics",
                DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    /**
     * Build a RevisionEngineConfig wired for auto-approval and a happy-path generator
     * that returns AFTER_QUIZ_SENTENCE as the candidate.
     */
    private RevisionEngineConfig buildConfigAutoHappy(
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator,
            CorrectionContextResolver<CorrectionContext> contextResolver) {

        LemmaAbsenceQuizCandidateGenerator happyGenerator = ctx ->
                new LemmaAbsenceGeneratorResponse(AFTER_QUIZ_SENTENCE, "Ella estudia libros cada dia.");

        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(happyGenerator);
        LemmaAbsenceProposalStrategyRegistryConfig registryConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-mvp");
        DefaultLemmaAbsenceProposalStrategyRegistry registry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(registryConfig);

        DefaultLemmaAbsenceProposalDeriver deriver =
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create());

        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.AUTO);

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(contextResolver);
        config.setLemmaAbsenceStrategyRegistry(registry);
        config.setLemmaAbsenceProposalDeriver(deriver);
        return config;
    }

    // -----------------------------------------------------------------------
    // path-1: happy path — strategy activa + candidato OK + apply exitoso → success
    // Gates: F-LAPS-R001, R002, R003, R005, R012, R013, R014, R019
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador invoca la fase de revisio... → El sistema resuelve que estrategia de... → La estrategia recibe el CorrectionCon... [Hay una estrategia activa registrada para LEMMA_ABSENCE] → El sistema deriva el elementAfter a p... [La estrategia produce un candidato de quiz] → El flujo continua segun FEAT-REVBYP: ... → La RefinementTask queda marcada como ... [La aplicacion al curso fue exitosa] → success")
    public void path1_hayUnaEstrategiaActivaRegistradaParaLEMMAABSENCE_laEstrategiaProduceUnCandidatoDeQuiz_laAplicacionAlCursoFueExitosa_success(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        // Stores: Mockito mocks (no in-memory impl available)
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        // Context resolver: use real LemmaAbsenceContextResolver wired via a dispatching wrapper.
        // Since the AuditReport is built with a LEMMA_ABSENCE quiz node, the resolver will return
        // a populated LemmaAbsenceCorrectionContext.
        LemmaAbsenceContextResolver lapsResolver = new LemmaAbsenceContextResolver();
        CorrectionContextResolver<CorrectionContext> contextResolver =
                (report, task) -> lapsResolver.resolve(report, task).map(c -> (CorrectionContext) c);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);
        CourseEntity updatedCourse = mock(CourseEntity.class);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(updatedCourse);
        when(artifactStore.save(any(RevisionArtifact.class))).thenReturn(
                ".content-audit/revisions/" + PLAN_ID + "/proposal-1");

        RevisionEngineConfig config = buildConfigAutoHappy(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                contextResolver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // ── Assert terminal result: success (APPROVED_APPLIED) ───────────────────
        // Gate F-LAPS-R002: the LAPS strategy pipeline handled the task (not bypass)
        // Gate F-LAPS-R001: elementAfter != elementBefore (different quizSentence)
        // Gate F-LAPS-R005: proposal has strategyId "lemma-absence-mvp" and reviserKind set
        // Gate F-LAPS-R014: course.save called → apply successful
        assertEquals(RevisionOutcomeKind.APPROVED_APPLIED, outcome.getKind(),
                "path-1: happy path must end with APPROVED_APPLIED (R001, R002, R003, R005)");

        // R001: elementAfter differs from elementBefore — verified by proposal in artifact
        assertNotNull(outcome.getArtifact(), "artifact must be present after APPROVED_APPLIED");
        RevisionArtifact artifact = outcome.getArtifact();
        assertNotNull(artifact.getProposal(), "proposal must be present in artifact");

        // R005: reviserKind must be the strategy name, not "bypass"
        assertEquals("lemma-absence-mvp", artifact.getProposal().getReviserKind(),
                "R005: reviserKind must be 'lemma-absence-mvp', not 'bypass'");

        // R005: strategyId carries name and version
        assertNotNull(artifact.getProposal().getStrategyId(),
                "R005: strategyId must be present in proposal");
        assertEquals("lemma-absence-mvp", artifact.getProposal().getStrategyId().getName(),
                "R005: strategyId.name must be 'lemma-absence-mvp'");

        // R001: elementAfter title must match the candidate's DSL (not the original "reads")
        QuizTemplateEntity elementAfterQuiz = artifact.getProposal().getElementAfter().getQuiz();
        assertNotNull(elementAfterQuiz, "elementAfter quiz must be non-null (R001)");
        // The candidate DSL "She ____ [studies] (study) books every day."
        // yields plain sentence "She studies books every day." (R012)
        assertEquals("She studies books every day.", elementAfterQuiz.getTitle(),
                "R001/R012: elementAfter.title must be the canonical plain sentence from candidate DSL");

        // R001: elementAfter differs from elementBefore (title changed)
        assertNotEquals("She reads books about advanced topics.", elementAfterQuiz.getTitle(),
                "R001: elementAfter.title must differ from elementBefore.title");

        // R014: ids, instructions and knowledgeId preserved
        assertEquals(QUIZ_ID, elementAfterQuiz.getId(),
                "R014: quizId must be preserved in elementAfter");
        assertEquals("knowledge-laps-j001", elementAfterQuiz.getKnowledgeId(),
                "R014: knowledgeId must be preserved in elementAfter");
        assertEquals("Write the correct form.", elementAfterQuiz.getInstructions(),
                "R014: instructions must be preserved in elementAfter");

        // R013: task is DONE — plan was saved
        verify(planStore).save(any(RefinementPlan.class));
        // Course was written (apply successful)
        verify(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));
    }

    // -----------------------------------------------------------------------
    // path-2: apply fails — strategy OK + candidato OK + escritura al curso falla → failure
    // Gate: inherited FEAT-REVBYP-R014
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador invoca la fase de revisio... → El sistema resuelve que estrategia de... → La estrategia recibe el CorrectionCon... [Hay una estrategia activa registrada para LEMMA_ABSENCE] → El sistema deriva el elementAfter a p... [La estrategia produce un candidato de quiz] → El flujo continua segun FEAT-REVBYP: ... → La propuesta fue aprobada y persistid... [La aplicacion al curso fallo] → failure")
    public void path2_hayUnaEstrategiaActivaRegistradaParaLEMMAABSENCE_laEstrategiaProduceUnCandidatoDeQuiz_laAplicacionAlCursoFallo_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        LemmaAbsenceContextResolver lapsResolver = new LemmaAbsenceContextResolver();
        CorrectionContextResolver<CorrectionContext> contextResolver =
                (report, task) -> lapsResolver.resolve(report, task).map(c -> (CorrectionContext) c);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);
        CourseEntity updatedCourse = mock(CourseEntity.class);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(updatedCourse);
        when(artifactStore.save(any(RevisionArtifact.class))).thenReturn(
                ".content-audit/revisions/" + PLAN_ID + "/proposal-2");
        // Simulate course write failure (FEAT-REVBYP R014)
        org.mockito.Mockito.doThrow(new RuntimeException("Disk write failed"))
                .when(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));

        RevisionEngineConfig config = buildConfigAutoHappy(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                contextResolver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // ── Assert: failure → APPROVED_APPLY_FAILED (FEAT-REVBYP R14) ─────────────
        // The artifact was persisted BEFORE the course write (R014: artifact-first guarantee)
        assertEquals(RevisionOutcomeKind.APPROVED_APPLY_FAILED, outcome.getKind(),
                "path-2: course write failure must yield APPROVED_APPLY_FAILED (FEAT-REVBYP R014)");
        assertNotNull(outcome.getArtifact(),
                "artifact must be persisted even when course write fails (R014)");

        // Task must NOT be marked DONE — plan.save called without COMPLETED state
        verify(planStore, never()).save(any(RefinementPlan.class));
    }

    // -----------------------------------------------------------------------
    // path-3: strategy fails — generator lanza ProposalStrategyFailedException → STRATEGY_FAILED
    // Gates: F-LAPS-R015, R016
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El operador invoca la fase de revisio... → El sistema resuelve que estrategia de... → La estrategia recibe el CorrectionCon... [Hay una estrategia activa registrada para LEMMA_ABSENCE] → El sistema reporta que la estrategia ... [La estrategia no puede producir un candidato de quiz] → failure")
    public void path3_hayUnaEstrategiaActivaRegistradaParaLEMMAABSENCE_laEstrategiaNoPuedeProducirUnCandidatoDeQuiz_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        LemmaAbsenceContextResolver lapsResolver = new LemmaAbsenceContextResolver();
        CorrectionContextResolver<CorrectionContext> contextResolver =
                (report, task) -> lapsResolver.resolve(report, task).map(c -> (CorrectionContext) c);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        // Failing generator: simulates provider down / empty response (R015)
        LemmaAbsenceQuizCandidateGenerator failingGenerator = ctx -> {
            throw new ProposalStrategyFailedException(
                    "lemma-absence-mvp", TASK_ID, "provider down");
        };
        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(failingGenerator);
        LemmaAbsenceProposalStrategyRegistryConfig registryConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-mvp");
        DefaultLemmaAbsenceProposalStrategyRegistry registry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(registryConfig);
        DefaultLemmaAbsenceProposalDeriver deriver =
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create());
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.AUTO);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(contextResolver);
        config.setLemmaAbsenceStrategyRegistry(registry);
        config.setLemmaAbsenceProposalDeriver(deriver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // ── Assert: failure → STRATEGY_FAILED ────────────────────────────────────
        // R015: outcome is STRATEGY_FAILED; no artifact created; course not touched; task stays PENDING
        assertEquals(RevisionOutcomeKind.STRATEGY_FAILED, outcome.getKind(),
                "R015: strategy failure must yield STRATEGY_FAILED outcome");

        // R015: no artifact persisted (artifact store NOT called for save)
        assertNull(outcome.getArtifact(),
                "R015: no artifact must be created when strategy fails");

        // R015: course not modified
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // R016: no REJECTED or PENDING_APPROVAL verdict (no artifact means no verdict enum value)
        verify(artifactStore, never()).save(any(RevisionArtifact.class));

        // R015: task stays PENDING — plan not saved with COMPLETED status
        verify(planStore, never()).save(any(RefinementPlan.class));
    }

    // -----------------------------------------------------------------------
    // path-4: no active strategy — registry null/empty → NO_ACTIVE_STRATEGY
    // Gates: F-LAPS-R002, R006
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @Tag("path-4")
    @DisplayName("path-4: El operador invoca la fase de revisio... → El sistema resuelve que estrategia de... → El sistema reporta que no hay estrate... [No hay ninguna estrategia activa registrada para LEMMA_ABSENCE] → failure")
    public void path4_noHayNingunaEstrategiaActivaRegistradaParaLEMMAABSENCE_failure()
            {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        LemmaAbsenceContextResolver lapsResolver = new LemmaAbsenceContextResolver();
        CorrectionContextResolver<CorrectionContext> contextResolver =
                (report, task) -> lapsResolver.resolve(report, task).map(c -> (CorrectionContext) c);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        // Registry with NO active strategy (activeName not matching any registered strategy)
        // This simulates: lemmaAbsenceStrategyRegistry is present but returns empty for active()
        LemmaAbsenceProposalStrategyRegistryConfig registryConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(), null);
        DefaultLemmaAbsenceProposalStrategyRegistry emptyRegistry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(registryConfig);
        DefaultLemmaAbsenceProposalDeriver deriver =
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create());
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.AUTO);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(contextResolver);
        config.setLemmaAbsenceStrategyRegistry(emptyRegistry);
        config.setLemmaAbsenceProposalDeriver(deriver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // ── Assert: failure → NO_ACTIVE_STRATEGY ─────────────────────────────────
        // R006: system reports no active strategy; does NOT fall back to IdentityReviser
        // R002: bypass (IdentityReviser) is not the active reviser for LEMMA_ABSENCE
        assertEquals(RevisionOutcomeKind.NO_ACTIVE_STRATEGY, outcome.getKind(),
                "R006/R002: NO active strategy for LEMMA_ABSENCE must yield NO_ACTIVE_STRATEGY (not bypass)");

        // R006: no artifact created
        assertNull(outcome.getArtifact(),
                "R006: no artifact must be persisted when there is no active strategy");
        verify(artifactStore, never()).save(any(RevisionArtifact.class));

        // R006/R002: course must not be touched
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // R006: task stays PENDING — plan not updated
        verify(planStore, never()).save(any(RefinementPlan.class));
    }
}
