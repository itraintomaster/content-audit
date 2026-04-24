package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.learney.contentaudit.revisiondomain.ProposalDecisionOutcome;
import com.learney.contentaudit.revisiondomain.ProposalDecisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.ProposalDecisionService;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.RevisionOutcome;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultProposalDecisionServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.LemmaAbsenceProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceMvpStrategy;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceGeneratorResponse;
import com.learney.contentaudit.revisiondomain.strategy.LemmaAbsenceQuizCandidateGenerator;
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
 * Journey J005: El operador rechaza la propuesta en modo humano.
 *
 * path-1: strategy activa → candidato producido → elementAfter derivado → PENDING_APPROVAL →
 *   operador evalua y rechaza → REJECTED, curso sin cambios, tarea vuelve a PENDING.
 *
 * Gates: F-LAPS-R001 (proposal != identity), F-LAPS-R005 (strategyId en propuesta),
 *        F-LAPS-R012 (derivacion deterministica), F-LAPS-R013 (campos que pueden diferir).
 *
 * Test en memoria: wira el engine en modo HUMAN y el ProposalDecisionService para el rechazo.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-LAPS")
@Tag("F-LAPS-J005")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FLapsJ005JourneyTest {

    private static final String PLAN_ID  = "plan-laps-j005";
    private static final String AUDIT_ID = "audit-laps-j005";
    private static final String TASK_ID  = "task-laps-j005";
    private static final String QUIZ_ID  = "quiz-laps-j005";
    private static final String AFTER_DSL = "She ____ [learns] (learn) new words daily.";
    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------

    private AuditReport buildAuditReport() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-laps-j005", "A1", "M001");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-laps-j005", "Learning", "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Learning vocabulary", "Write the correct form.", true,
                "knowledge-laps-j005", "Knowledge 1", "K001");
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Quiz 1", "Q001",
                "Ella aprende palabras complicadas a diario.",
                List.of("She learns complicated words daily."),
                "She ____ [learns] (learn) complicated words daily.");

        AbsentLemma absent = new AbsentLemma(
                new LemmaAndPos("complicated", "ADJ"), CefrLevel.B1,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.MEDIUM, 2100, null);
        LemmaAbsenceLevelDiagnosis levelDx = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 65, 1, 11.0, 89.0, 0.63, 0.52, 0.32,
                null, List.of(absent), List.of(), 1, 2, 3);
        DefaultLevelDiagnoses levelDiagnoses = new DefaultLevelDiagnoses();
        levelDiagnoses.setLemmaAbsenceDiagnosis(levelDx);

        MisplacedLemma misplaced = new MisplacedLemma(
                new LemmaAndPos("complicated", "ADJ"), CefrLevel.B1, CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, 2100, null);
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
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("learns")),
                new SentencePartEntity(SentencePartKind.TEXT, "(learn) complicated words daily.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", "knowledge-laps-j005",
                "She learns complicated words daily.",
                "Write the correct form.",
                "Ella aprende palabras complicadas a diario.",
                "basics.01.Learning", "Learning",
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "");
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz);
    }

    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Quiz about complicated learning",
                DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    private RevisionEngineConfig buildConfigHumanHappy(
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator,
            CorrectionContextResolver<CorrectionContext> contextResolver) {

        // Generator: happy path — replaces "complicated words" with "new words" (R001)
        LemmaAbsenceQuizCandidateGenerator happyGenerator = ctx ->
                new LemmaAbsenceGeneratorResponse(AFTER_DSL, "Ella aprende palabras nuevas a diario.");

        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(happyGenerator);
        LemmaAbsenceProposalStrategyRegistryConfig regConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-mvp");
        DefaultLemmaAbsenceProposalStrategyRegistry registry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(regConfig);
        DefaultLemmaAbsenceProposalDeriver deriver =
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create());
        // Human mode validator: emits PENDING_APPROVAL
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.HUMAN);

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
    // path-1: strategy produces candidate → PENDING_APPROVAL → operator rejects → REJECTED
    // Gates: F-LAPS-R001, R005, R012, R013
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador invoca la fase de revisio... → La estrategia consume el CorrectionCo... → El sistema deriva el elementAfter a p... → El operador inspecciona el elementAft... → El flujo de FEAT-REVAPR transiciona l... → success")
    public void path1_success() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        // Node: iniciar_revision_rechazo — LEMMA_ABSENCE task in human mode
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

        // Artifact store: accumulates saves; retrieval returns the latest saved artifact
        java.util.List<RevisionArtifact> savedArtifacts = new java.util.ArrayList<>();
        when(artifactStore.save(any(RevisionArtifact.class))).thenAnswer(inv -> {
            RevisionArtifact a = inv.getArgument(0);
            // Replace existing entry with same proposalId
            savedArtifacts.removeIf(existing -> existing.getProposal() != null
                    && a.getProposal() != null
                    && existing.getProposal().getProposalId().equals(a.getProposal().getProposalId()));
            savedArtifacts.add(a);
            return ".content-audit/revisions/" + PLAN_ID + "/" + (a.getProposal() != null ? a.getProposal().getProposalId() : "unknown");
        });
        when(artifactStore.findByProposalId(any(), any())).thenAnswer(inv -> {
            String proposalId = inv.getArgument(0);
            return savedArtifacts.stream()
                    .filter(a -> a.getProposal() != null && proposalId.equals(a.getProposal().getProposalId()))
                    .findFirst();
        });

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(course);

        RevisionEngineConfig config = buildConfigHumanHappy(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                contextResolver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act: Step 1 — revise in human mode (Node: estrategia_produce_candidato) ─
        RevisionOutcome reviseOutcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // Node: propuesta_pendiente — verify gates R001, R005, R012, R013
        assertEquals(RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, reviseOutcome.getKind(),
                "Human validator must produce PENDING_APPROVAL_PERSISTED");
        assertNotNull(reviseOutcome.getArtifact(), "Artifact must be persisted");

        RevisionArtifact pendingArtifact = reviseOutcome.getArtifact();
        assertEquals(RevisionVerdict.PENDING_APPROVAL, pendingArtifact.getVerdict(),
                "Artifact must have PENDING_APPROVAL verdict before operator decides");

        // Gate F-LAPS-R001: elementAfter != elementBefore (different quizSentence)
        QuizTemplateEntity afterQuiz = pendingArtifact.getProposal().getElementAfter().getQuiz();
        assertNotNull(afterQuiz, "R001: elementAfter quiz must not be null");
        // AFTER_DSL = "She ____ [learns] (learn) new words daily." → plain = "She learns new words daily."
        assertEquals("She learns new words daily.", afterQuiz.getTitle(),
                "R001/R012: elementAfter.title must be the plain sentence derived from AFTER_DSL");
        // Before was "She learns complicated words daily." — title changed (R001)

        // Gate F-LAPS-R005: proposal carries strategy identity
        assertEquals("lemma-absence-mvp", pendingArtifact.getProposal().getReviserKind(),
                "R005: reviserKind must be 'lemma-absence-mvp'");
        assertNotNull(pendingArtifact.getProposal().getStrategyId(),
                "R005: strategyId must be present");
        assertEquals("lemma-absence-mvp", pendingArtifact.getProposal().getStrategyId().getName(),
                "R005: strategyId.name must be 'lemma-absence-mvp'");

        // Gate F-LAPS-R013: translation changed (new candidate provided different translation)
        assertEquals("Ella aprende palabras nuevas a diario.", afterQuiz.getTranslation(),
                "R013: elementAfter.translation must be from the candidate");

        // Gate F-LAPS-R012: derivation is deterministic — structural invariants preserved (R014)
        assertEquals(QUIZ_ID, afterQuiz.getId(),
                "R014: quizId preserved in elementAfter (R012 invariant)");
        assertEquals("knowledge-laps-j005", afterQuiz.getKnowledgeId(),
                "R014: knowledgeId preserved in elementAfter");
        assertEquals("Write the correct form.", afterQuiz.getInstructions(),
                "R014: instructions preserved in elementAfter");

        // Course must NOT have been touched yet (pending approval)
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // ── Act: Step 2 — operator evaluates and rejects (Node: operador_evalua → rechazo_heredado) ─
        String proposalId = pendingArtifact.getProposal().getProposalId();
        ProposalDecisionService decisionService =
                new DefaultProposalDecisionServiceFactory().create(config);

        ProposalDecisionOutcome rejectOutcome = decisionService.reject(
                proposalId,
                Optional.of(PLAN_ID),
                Optional.of("The proposed exercise still contains advanced vocabulary"));

        // ── Assert: Node rechazo_heredado — FEAT-REVAPR rejection flow ────────────
        assertEquals(ProposalDecisionOutcomeKind.REJECTED, rejectOutcome.getKind(),
                "Rejection must yield ProposalDecisionOutcomeKind.REJECTED");
        assertNotNull(rejectOutcome.getArtifact(), "Rejected artifact must be present");
        assertEquals(RevisionVerdict.REJECTED, rejectOutcome.getArtifact().getVerdict(),
                "Artifact verdict must be REJECTED after operator rejects");

        // Course must NOT have been written (rejection = no course modification)
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // planStore.save called at least once: by engine (PENDING_APPROVAL) and by decisionService (reject)
        org.mockito.Mockito.verify(planStore, org.mockito.Mockito.atLeast(1)).save(any(RefinementPlan.class));
    }
}
