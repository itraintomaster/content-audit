package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Journey J002: Flujo feliz end-to-end con aprobacion humana para LEMMA_ABSENCE.
 *
 * path-1: revise (human) → PENDING_APPROVAL persisted → operator approves → course rewritten → success
 * path-2: revise (human) → PENDING_APPROVAL persisted → operator rejects → course untouched → REJECTED
 *
 * Tests en memoria: usan mocks para stores y el engine real via DefaultRevisionEngineFactory.
 * El ValidatorFactory en modo HUMAN produce PENDING_APPROVAL en el primer paso.
 * El ProposalDecisionServiceFactory gestiona la aprobacion/rechazo sobre el mismo artifactStore mock.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-LAPS")
@Tag("F-LAPS-J002")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FLapsJ002JourneyTest {

    private static final String PLAN_ID  = "plan-laps-j002";
    private static final String AUDIT_ID = "audit-laps-j002";
    private static final String TASK_ID  = "task-laps-j002";
    private static final String QUIZ_ID  = "quiz-laps-j002";
    private static final String AFTER_DSL = "She ____ [studies] (study) every morning.";
    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // Fixture helpers (same convention as J001)
    // -----------------------------------------------------------------------

    private AuditReport buildAuditReport() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-laps-j002", "A1", "M001");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-laps-j002", "Daily Routines", "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Present Simple routines", "Write the correct form.", true,
                "knowledge-laps-j002", "Knowledge 1", "K001");
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Quiz 1", "Q001",
                "Ella hace ejercicio avanzado cada manana.",
                List.of("She does advanced exercise every morning."),
                "She ____ [does] (do) advanced exercise every morning.");

        AbsentLemma absent = new AbsentLemma(
                new LemmaAndPos("advanced", "ADJ"), CefrLevel.B2,
                AbsenceType.APPEARS_TOO_LATE, List.of(), PriorityLevel.HIGH, 1500, null);
        LemmaAbsenceLevelDiagnosis levelDx = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 80, 1, 10.0, 90.0, 0.7, 0.6, 0.4,
                null, List.of(absent), List.of(), 1, 2, 4);
        DefaultLevelDiagnoses levelDiagnoses = new DefaultLevelDiagnoses();
        levelDiagnoses.setLemmaAbsenceDiagnosis(levelDx);

        MisplacedLemma misplaced = new MisplacedLemma(
                new LemmaAndPos("advanced", "ADJ"), CefrLevel.B2, CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE, 1500, null);
        DefaultQuizDiagnoses quizDx = new DefaultQuizDiagnoses();
        quizDx.setLemmaAbsenceDiagnosis(new LemmaPlacementDiagnosis(1, List.of(misplaced)));

        AuditNode courseNode = buildCourseNode();
        AuditNode msNode = buildNode(courseNode, AuditTarget.MILESTONE, milestone, levelDiagnoses);
        AuditNode topicNode = buildNode(msNode, AuditTarget.TOPIC, topic, new DefaultTopicDiagnoses());
        AuditNode knNode = buildNode(topicNode, AuditTarget.KNOWLEDGE, knowledge, new DefaultKnowledgeDiagnoses());
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
                new SentencePartEntity(SentencePartKind.TEXT, "(do) advanced exercise every morning.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", "knowledge-laps-j002",
                "She does advanced exercise every morning.",
                "Write the correct form.",
                "Ella hace ejercicio avanzado cada manana.",
                "basics.01.DailyRoutines", "Daily Routines",
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "");
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz);
    }

    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Quiz about advanced exercise",
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

        LemmaAbsenceQuizCandidateGenerator happyGenerator = ctx ->
                new LemmaAbsenceGeneratorResponse(AFTER_DSL, "Ella estudia cada manana.");

        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(happyGenerator);
        LemmaAbsenceProposalStrategyRegistryConfig regConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-mvp");
        DefaultLemmaAbsenceProposalStrategyRegistry registry =
                new DefaultLemmaAbsenceProposalStrategyRegistry(regConfig);
        DefaultLemmaAbsenceProposalDeriver deriver =
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create());
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
    // path-1: human approves — strategy produces candidate → PENDING_APPROVAL → approve → success
    // Gates: F-LAPS-R001, R002, R005, R012
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador invoca la fase de revisio... → El sistema resuelve la estrategia act... → La estrategia consume el CorrectionCo... → El sistema deriva el elementAfter a p... → El validator humano (FEAT-REVAPR) emi... → El operador inspecciona el artefacto ... → El flujo de FEAT-REVAPR aplica la pro... [El operador aprueba la propuesta] → success")
    public void path1_elOperadorApruebaLaPropuesta_success() {
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

        // Artifact store: save captures the artifact for retrieval in the approve step
        java.util.List<RevisionArtifact> savedArtifacts = new java.util.ArrayList<>();
        when(artifactStore.save(any(RevisionArtifact.class))).thenAnswer(inv -> {
            RevisionArtifact a = inv.getArgument(0);
            savedArtifacts.add(a);
            return ".content-audit/revisions/" + PLAN_ID + "/" + (a.getProposal() != null ? a.getProposal().getProposalId() : "unknown");
        });
        when(artifactStore.findByProposalId(any(), any())).thenAnswer(inv -> {
            // Return the most recently saved artifact matching the verdict PENDING_APPROVAL
            String proposalId = inv.getArgument(0);
            return savedArtifacts.stream()
                    .filter(a -> a.getProposal() != null && proposalId.equals(a.getProposal().getProposalId()))
                    .reduce((first, second) -> second); // last saved wins
        });

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(updatedCourse);

        RevisionEngineConfig config = buildConfigHumanHappy(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                contextResolver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act: Step 1 — revise in human mode ────────────────────────────────────
        RevisionOutcome reviseOutcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        // Gate: validator emits PENDING_APPROVAL → PENDING_APPROVAL_PERSISTED
        assertEquals(RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, reviseOutcome.getKind(),
                "Human validator must produce PENDING_APPROVAL_PERSISTED after revise (R001, R005)");
        assertNotNull(reviseOutcome.getArtifact(),
                "Artifact must be persisted in PENDING_APPROVAL state");

        RevisionArtifact pendingArtifact = reviseOutcome.getArtifact();
        assertEquals(RevisionVerdict.PENDING_APPROVAL, pendingArtifact.getVerdict(),
                "Artifact verdict must be PENDING_APPROVAL before operator decides");

        // Gate F-LAPS-R005: proposal carries strategy identity
        assertEquals("lemma-absence-mvp", pendingArtifact.getProposal().getReviserKind(),
                "R005: reviserKind must be 'lemma-absence-mvp' (not 'bypass')");
        assertNotNull(pendingArtifact.getProposal().getStrategyId(),
                "R005: strategyId must be present in proposal");
        assertEquals("lemma-absence-mvp", pendingArtifact.getProposal().getStrategyId().getName(),
                "R005: strategyId.name must be 'lemma-absence-mvp'");

        // Gate F-LAPS-R001: elementAfter differs from elementBefore
        QuizTemplateEntity afterQuiz = pendingArtifact.getProposal().getElementAfter().getQuiz();
        assertNotNull(afterQuiz, "R001: elementAfter quiz must not be null");
        assertEquals("She studies every morning.", afterQuiz.getTitle(),
                "R001/R012: elementAfter.title must be the canonical plain sentence from AFTER_DSL");

        // Gate F-LAPS-R012: proposal derived deterministically (title matches DSL parse)
        // Gate F-LAPS-R014: structural invariants preserved
        assertEquals(QUIZ_ID, afterQuiz.getId(), "R014: quizId preserved in elementAfter");

        // Course must NOT have been touched yet (pending approval)
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // ── Act: Step 2 — operator approves the proposal ──────────────────────────
        String proposalId = pendingArtifact.getProposal().getProposalId();
        ProposalDecisionService decisionService =
                new DefaultProposalDecisionServiceFactory().create(config);

        ProposalDecisionOutcome approveOutcome = decisionService.approve(
                proposalId,
                Optional.of(PLAN_ID),
                Optional.empty(),
                COURSE_PATH);

        // ── Assert: after approve — course written, task DONE ────────────────────
        assertEquals(ProposalDecisionOutcomeKind.APPROVED_APPLIED, approveOutcome.getKind(),
                "After approve: outcome must be APPROVED_APPLIED (R001, R005, R012)");
        // Course was written
        verify(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));
        // planStore.save called twice: once by engine (PENDING) and once by decisionService (APPROVED)
        org.mockito.Mockito.verify(planStore, org.mockito.Mockito.atLeast(1)).save(any(RefinementPlan.class));
    }

    // -----------------------------------------------------------------------
    // path-2: human rejects — strategy produces candidate → PENDING_APPROVAL → reject → REJECTED
    // Gates: F-LAPS-R001, R005, R012, R013
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador invoca la fase de revisio... → El sistema resuelve la estrategia act... → La estrategia consume el CorrectionCo... → El sistema deriva el elementAfter a p... → El validator humano (FEAT-REVAPR) emi... → El operador inspecciona el artefacto ... → El flujo de FEAT-REVAPR transiciona l... [El operador rechaza la propuesta] → success")
    public void path2_elOperadorRechazaLaPropuesta_success() {
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

        java.util.List<RevisionArtifact> savedArtifacts = new java.util.ArrayList<>();
        when(artifactStore.save(any(RevisionArtifact.class))).thenAnswer(inv -> {
            RevisionArtifact a = inv.getArgument(0);
            // Replace if same proposalId, otherwise add
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

        // ── Act: Step 1 — revise in human mode ────────────────────────────────────
        RevisionOutcome reviseOutcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH);

        assertEquals(RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, reviseOutcome.getKind(),
                "Human validator must produce PENDING_APPROVAL_PERSISTED");
        assertNotNull(reviseOutcome.getArtifact());

        RevisionArtifact pendingArtifact = reviseOutcome.getArtifact();
        String proposalId = pendingArtifact.getProposal().getProposalId();

        // Gate F-LAPS-R001: elementAfter != elementBefore
        QuizTemplateEntity afterQuiz = pendingArtifact.getProposal().getElementAfter().getQuiz();
        assertEquals("She studies every morning.", afterQuiz.getTitle(),
                "R001/R012: elementAfter.title must be from AFTER_DSL candidate");

        // Gate F-LAPS-R005: strategy identity in proposal
        assertEquals("lemma-absence-mvp", pendingArtifact.getProposal().getReviserKind(),
                "R005: reviserKind must be 'lemma-absence-mvp'");

        // Gate F-LAPS-R013: structural invariants preserved in elementAfter
        assertEquals(QUIZ_ID, afterQuiz.getId(),
                "R014: quizId preserved in elementAfter");

        // ── Act: Step 2 — operator rejects the proposal ───────────────────────────
        ProposalDecisionService decisionService =
                new DefaultProposalDecisionServiceFactory().create(config);

        ProposalDecisionOutcome rejectOutcome = decisionService.reject(
                proposalId,
                Optional.of(PLAN_ID),
                Optional.of("Exercise quality not acceptable"));

        // ── Assert: after reject — course untouched, task stays PENDING ───────────
        assertEquals(ProposalDecisionOutcomeKind.REJECTED, rejectOutcome.getKind(),
                "After reject: outcome must be REJECTED");
        assertNotNull(rejectOutcome.getArtifact(),
                "Artifact must be present after rejection");
        assertEquals(RevisionVerdict.REJECTED, rejectOutcome.getArtifact().getVerdict(),
                "Artifact verdict must be REJECTED after operator rejects");

        // Course must NOT have been written (rejection = no course change)
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // planStore.save called at least once (engine saves PENDING, decisionService saves on reject)
        org.mockito.Mockito.verify(planStore, org.mockito.Mockito.atLeast(1)).save(any(RefinementPlan.class));
    }
}
