package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableEntity;
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
import com.learney.contentaudit.auditdomain.NodeDiagnoses;
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
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.LemmaAbsenceProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceMvpStrategy;
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
 * Journey J003: La estrategia falla al producir el candidato.
 *
 * path-1: strategy activa + generator lanza ProposalStrategyFailedException →
 *   abortar_sin_artefacto → failure (STRATEGY_FAILED).
 *
 * Gates: F-LAPS-R007 (context consumed), F-LAPS-R015 (no artifact, no course change,
 * task stays PENDING), F-LAPS-R016 (no REJECTED or PENDING_APPROVAL verdict).
 *
 * Test en memoria: wira un generator configurable para que lance la excepcion.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-LAPS")
@Tag("F-LAPS-J003")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FLapsJ003JourneyTest {

    private static final String PLAN_ID  = "plan-laps-j003";
    private static final String AUDIT_ID = "audit-laps-j003";
    private static final String TASK_ID  = "task-laps-j003";
    private static final String QUIZ_ID  = "quiz-laps-j003";
    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    private AuditReport buildAuditReport() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-laps-j003", "A1", "M001");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-laps-j003", "Grammar", "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Simple Present", "Write the correct form.", true,
                "knowledge-laps-j003", "Knowledge 1", "K001");
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Quiz 1", "Q001",
                "Ella hace cosas complicadas.",
                List.of("She does complicated things."),
                "She ____ [does] (do) complicated things.");

        AbsentLemma absent = new AbsentLemma(
                new LemmaAndPos("complicated", "ADJ"), CefrLevel.B1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.MEDIUM, 2000, null);
        LemmaAbsenceLevelDiagnosis levelDx = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 60, 1, 10.0, 90.0, 0.6, 0.5, 0.3,
                null, List.of(absent), List.of(), 1, 2, 3);
        DefaultLevelDiagnoses levelDiagnoses = new DefaultLevelDiagnoses();
        levelDiagnoses.setLemmaAbsenceDiagnosis(levelDx);

        MisplacedLemma misplaced = new MisplacedLemma(
                new LemmaAndPos("complicated", "ADJ"), CefrLevel.B1, CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, 2000, null);
        DefaultQuizDiagnoses quizDx = new DefaultQuizDiagnoses();
        quizDx.setLemmaAbsenceDiagnosis(new LemmaPlacementDiagnosis(1, List.of(misplaced)));

        AuditNode courseNode = buildCourseNode();
        AuditNode msNode = buildNode(courseNode, AuditTarget.MILESTONE, milestone, levelDiagnoses);
        AuditNode tpNode = buildNode(msNode, AuditTarget.TOPIC, topic, new DefaultTopicDiagnoses());
        AuditNode knNode = buildNode(tpNode, AuditTarget.KNOWLEDGE, knowledge, new DefaultKnowledgeDiagnoses());
        buildNode(knNode, AuditTarget.QUIZ, quiz, quizDx);
        return new AuditReport(courseNode);
    }

    private AuditNode buildCourseNode() {
        AuditNode n = new AuditNode();
        n.setTarget(AuditTarget.COURSE);
        n.setChildren(new ArrayList<>());
        n.setScores(new LinkedHashMap<>());
        n.setMetadata(new LinkedHashMap<>());
        return n;
    }

    private AuditNode buildNode(AuditNode parent, AuditTarget target, AuditableEntity entity, NodeDiagnoses diagnoses) {
        AuditNode n = new AuditNode();
        n.setTarget(target);
        n.setEntity(entity);
        n.setParent(parent);
        n.setChildren(new ArrayList<>());
        n.setScores(new LinkedHashMap<>());
        n.setMetadata(new LinkedHashMap<>());
        n.setDiagnoses(diagnoses);
        parent.getChildren().add(n);
        return n;
    }

    private CourseElementSnapshot buildElementBefore() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", Arrays.asList(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("does")),
                new SentencePartEntity(SentencePartKind.TEXT, "(do) complicated things.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", "knowledge-laps-j003",
                "She does complicated things.",
                "Write the correct form.",
                "Ella hace cosas complicadas.",
                "basics.01.Grammar", "Grammar",
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "");
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz);
    }

    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Quiz about complicated things",
                DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    // -----------------------------------------------------------------------
    // path-1: strategy fails → abortar_sin_artefacto → failure (STRATEGY_FAILED)
    // Gates: F-LAPS-R007, F-LAPS-R015, F-LAPS-R016
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador invoca la fase de revisio... → La estrategia recibe el CorrectionCon... → El sistema reporta el fallo al operad... → failure")
    public void path1_failure() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        // Node: iniciar_revision_falla — setup LEMMA_ABSENCE task with real context
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

        // Node: invocar_estrategia_falla — generator configured to fail (R015: strategy cannot produce candidate)
        // Gate F-LAPS-R007: the strategy receives the CorrectionContext before failing
        LemmaAbsenceQuizCandidateGenerator failingGenerator = ctx -> {
            // R015: any reason — provider down, empty response, uninterpretable output
            throw new ProposalStrategyFailedException(
                    "lemma-absence-mvp", TASK_ID, "provider not available");
        };
        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(failingGenerator);
        LemmaAbsenceProposalStrategyRegistryConfig regConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-mvp");
        DefaultLemmaAbsenceProposalStrategyRegistry registry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(regConfig);
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

        // ── Assert: Node abortar_sin_artefacto (gates R015, R016) ─────────────────
        // R015: outcome kind is STRATEGY_FAILED
        assertEquals(RevisionOutcomeKind.STRATEGY_FAILED, outcome.getKind(),
                "R015: strategy failure must yield STRATEGY_FAILED");

        // R015: no artifact was created
        assertNull(outcome.getArtifact(),
                "R015: no artifact must exist when strategy fails before producing a candidate");

        // R015: artifact store was never asked to save (no artifact persisted)
        verify(artifactStore, never()).save(any(RevisionArtifact.class));

        // R015: course not modified
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // R015: task stays in its previous state (PENDING) — plan not saved with COMPLETED
        verify(planStore, never()).save(any(RefinementPlan.class));

        // R016: failure is not REJECTED nor PENDING_APPROVAL — those verdictos only apply to
        // persisted artifacts. Since no artifact exists, no verdict was emitted (R016).
        // The STRATEGY_FAILED outcome carries null artifact — no RevisionVerdict enum value.
    }
}
