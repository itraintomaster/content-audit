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

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableEntity;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.NodeDiagnoses;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.NodeKind;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.refinerdomain.CorrectionContext;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.KnowledgeTitleContextResolver;
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
import com.learney.contentaudit.revisiondomain.ProposalStrategyFailedException;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionEngine;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.RevisionOutcome;
import com.learney.contentaudit.revisiondomain.RevisionOutcomeKind;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;
import com.learney.contentaudit.revisiondomain.engine.DefaultKnowledgeTitleProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultKnowledgeTitleProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultProposalDecisionServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.KnowledgeTitleProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleAgentStrategy;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleCandidateGenerator;
import com.learney.contentaudit.revisiondomain.knowledgetitle.KnowledgeTitleGeneratorResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
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
 * Journey J001: Revisar y aprobar una correccion de titulo de knowledge (revise → propuesta
 * → aprobar/rechazar, KNOWLEDGE_TITLE_LENGTH).
 *
 * path-1: knowledge existe + agente produce candidato + propuesta aprobada (auto) → success
 * path-2: knowledge existe + agente produce candidato + operador rechaza → failure
 * path-3: knowledge existe + agente no produce candidato valido → failure
 * path-4: knowledge senalado no existe en el curso → failure
 *
 * Tests en memoria: construyen un AuditReport programaticamente (course → topic → knowledge),
 * usan KnowledgeTitleContextResolver real para resolver el contexto, y wirean el RevisionEngine
 * real via DefaultRevisionEngineFactory poblando RevisionEngineConfig.knowledgeTitleStrategyRegistry
 * / knowledgeTitleProposalDeriver (espejo del par LAPS), tal como decidio @architect (ARCH-KTLR-WIRE).
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-KTLR")
@Tag("F-KTLR-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FKtlrJ001JourneyTest {

    private static final String PLAN_ID = "plan-ktlr-j001";
    private static final String AUDIT_ID = "audit-ktlr-j001";
    private static final String TASK_ID = "task-ktlr-j001";
    private static final String KNOWLEDGE_ID = "knowledge-ktlr-j001";
    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // AuditReport builder helpers (same convention as FLapsJ001)
    // -----------------------------------------------------------------------

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

    /** Build a full AuditReport rooted at COURSE → TOPIC → KNOWLEDGE, with the knowledge present. */
    private AuditReport buildAuditReportWithKnowledge() {
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-ktlr-j001", "Verb Tenses", "VT1");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Present Simple Affirmative Sentences With Third Person Singular Verbs",
                "Completa la oracion con la forma correcta del verbo en presente simple.",
                true, KNOWLEDGE_ID, "Present Simple", "PS1", null, null);

        AuditNode courseNode = buildCourseNode();
        AuditNode topicNode = buildNode(courseNode, AuditTarget.TOPIC, topic, new DefaultTopicDiagnoses());
        buildNode(topicNode, AuditTarget.KNOWLEDGE, knowledge, new DefaultKnowledgeDiagnoses());
        return new AuditReport(courseNode);
    }

    /** Build a full AuditReport rooted at COURSE → TOPIC, with NO knowledge node (path-4). */
    private AuditReport buildAuditReportWithoutKnowledge() {
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-ktlr-j001", "Verb Tenses", "VT1");
        AuditNode courseNode = buildCourseNode();
        buildNode(courseNode, AuditTarget.TOPIC, topic, new DefaultTopicDiagnoses());
        return new AuditReport(courseNode);
    }

    /** Build a RefinementPlan with one KNOWLEDGE_TITLE_LENGTH task for KNOWLEDGE_ID. */
    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.KNOWLEDGE, KNOWLEDGE_ID, "Present Simple knowledge",
                DiagnosisKind.KNOWLEDGE_TITLE_LENGTH, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    /** The elementBefore snapshot: the knowledge as it currently exists in the course. */
    private CourseElementSnapshot buildElementBefore() {
        KnowledgeEntity knowledge = new KnowledgeEntity(
                KNOWLEDGE_ID, KNOWLEDGE_ID, NodeKind.KNOWLEDGE,
                "Present Simple Affirmative Sentences With Third Person Singular Verbs",
                "old-uuid-ktlr-j001", "topic-ktlr-j001", true,
                "Completa la oracion con la forma correcta del verbo en presente simple.",
                1, "present-simple-affirmative", List.of(), SentenceMode.FILL);
        return new CourseElementSnapshot(AuditTarget.KNOWLEDGE, KNOWLEDGE_ID, null, knowledge);
    }

    /** Wraps KnowledgeTitleContextResolver as a CorrectionContextResolver<CorrectionContext>. */
    private CorrectionContextResolver<CorrectionContext> buildContextResolver() {
        KnowledgeTitleContextResolver ktlrResolver = new KnowledgeTitleContextResolver();
        return new CorrectionContextResolver<CorrectionContext>() {
            @Override public Optional<CorrectionContext> resolve(AuditReport report, RefinementTask task) {
                return ktlrResolver.resolve(report, task).map(c -> (CorrectionContext) c);
            }
            @Override public Optional<CorrectionContext> resolveWithIndex(
                    com.learney.contentaudit.auditdomain.AuditNodeIndex idx, AuditReport report, RefinementTask task) {
                return resolve(report, task);
            }
            @Override public boolean supports(DiagnosisKind kind) { return ktlrResolver.supports(kind); }
        };
    }

    /** Builds a RevisionEngineConfig wired for KNOWLEDGE_TITLE_LENGTH with a happy-path generator. */
    private RevisionEngineConfig buildConfigHappy(
            ApprovalMode mode,
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator) {

        KnowledgeTitleCandidateGenerator happyGenerator = ctx ->
                new KnowledgeTitleGeneratorResponse(
                        "Present Simple Affirmative",
                        "Completa la oracion con el verbo en presente simple.");

        KnowledgeTitleAgentStrategy strategy = new KnowledgeTitleAgentStrategy(happyGenerator, "llm:test");
        KnowledgeTitleProposalStrategyRegistryConfig registryConfig =
                new KnowledgeTitleProposalStrategyRegistryConfig(List.of(strategy), "knowledge-title-agent");
        DefaultKnowledgeTitleProposalStrategyRegistry registry =
                new DefaultKnowledgeTitleProposalStrategyRegistry(registryConfig);
        DefaultKnowledgeTitleProposalDeriver deriver = new DefaultKnowledgeTitleProposalDeriver();

        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(mode);

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(buildContextResolver());
        config.setKnowledgeTitleStrategyRegistry(registry);
        config.setKnowledgeTitleProposalDeriver(deriver);
        return config;
    }

    // -----------------------------------------------------------------------
    // path-1: happy path — knowledge existe + candidato OK + aprobada (auto) → success
    // Gates: F-KTLR-R001, R002, R003, R004, R005, R006, R007
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador invoca la revision sobre ... → El sistema resuelve el contexto del k... → El agente de titulos recibe el contex... [El knowledge existe y se resuelve el contexto] → El sistema envuelve el candidato en u... [El agente produce un candidato valido] → La propuesta se decide segun el modo ... → El sistema escribe en el knowledge de... [La propuesta se aprueba (auto o humano)] → La tarea queda marcada como completad... → success")
    public void path1_elKnowledgeExisteYSeResuelveElContexto_elAgenteProduceUnCandidatoValido_laPropuestaSeApruebaAutoOHumano_success(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        AuditReport auditReport = buildAuditReportWithKnowledge();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);
        CourseEntity updatedCourse = mock(CourseEntity.class);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.KNOWLEDGE, KNOWLEDGE_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(updatedCourse);
        when(artifactStore.save(any(RevisionArtifact.class))).thenReturn(
                ".content-audit/revisions/" + PLAN_ID + "/proposal-1");

        RevisionEngineConfig config = buildConfigHappy(
                ApprovalMode.AUTO, planStore, auditReportStore, artifactStore, courseRepository, elementLocator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: success (APPROVED_APPLIED) ───────────────────────────────────
        assertEquals(RevisionOutcomeKind.APPROVED_APPLIED, outcome.getKind(),
                "path-1: happy path must end with APPROVED_APPLIED (R001-R007)");
        assertNotNull(outcome.getArtifact(), "artifact must be present after APPROVED_APPLIED");

        RevisionArtifact artifact = outcome.getArtifact();
        KnowledgeEntity afterKnowledge = artifact.getProposal().getElementAfter().getKnowledge();
        assertNotNull(afterKnowledge, "elementAfter.knowledge must not be null (R006)");
        assertEquals("Present Simple Affirmative", afterKnowledge.getLabel(),
                "R006/R007: elementAfter.knowledge.label must be the candidate's title");
        assertEquals("Completa la oracion con el verbo en presente simple.", afterKnowledge.getInstructions(),
                "R006/R007: elementAfter.knowledge.instructions must be the candidate's instructions");
        assertEquals(KNOWLEDGE_ID, afterKnowledge.getId(),
                "R007: knowledge id must be preserved");

        verify(planStore).save(any(RefinementPlan.class));
        verify(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));
    }

    // -----------------------------------------------------------------------
    // path-2: knowledge existe + candidato OK + operador rechaza → failure
    // Gates: F-KTLR-R001..R005, R009
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador invoca la revision sobre ... → El sistema resuelve el contexto del k... → El agente de titulos recibe el contex... [El knowledge existe y se resuelve el contexto] → El sistema envuelve el candidato en u... [El agente produce un candidato valido] → La propuesta se decide segun el modo ... → El curso no se modifica; el knowledge... [El operador rechaza la propuesta] → failure")
    public void path2_elKnowledgeExisteYSeResuelveElContexto_elAgenteProduceUnCandidatoValido_elOperadorRechazaLaPropuesta_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        AuditReport auditReport = buildAuditReportWithKnowledge();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        List<RevisionArtifact> savedArtifacts = new ArrayList<>();
        when(artifactStore.save(any(RevisionArtifact.class))).thenAnswer(inv -> {
            RevisionArtifact a = inv.getArgument(0);
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
        when(elementLocator.snapshot(course, AuditTarget.KNOWLEDGE, KNOWLEDGE_ID))
                .thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class)))
                .thenReturn(course);

        RevisionEngineConfig config = buildConfigHappy(
                ApprovalMode.HUMAN, planStore, auditReportStore, artifactStore, courseRepository, elementLocator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act: Step 1 — revise in human mode → PENDING_APPROVAL_PERSISTED ───────
        RevisionOutcome reviseOutcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        assertEquals(RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, reviseOutcome.getKind(),
                "Human validator must produce PENDING_APPROVAL_PERSISTED (R005)");
        assertNotNull(reviseOutcome.getArtifact());

        RevisionArtifact pendingArtifact = reviseOutcome.getArtifact();
        String proposalId = pendingArtifact.getProposal().getProposalId();

        // ── Act: Step 2 — operator rejects the proposal ───────────────────────────
        ProposalDecisionService decisionService = new DefaultProposalDecisionServiceFactory().create(config);
        ProposalDecisionOutcome rejectOutcome = decisionService.reject(
                proposalId, Optional.of(PLAN_ID), Optional.of("Title not good enough"));

        // ── Assert: failure — course untouched, knowledge keeps original title/instructions (R009) ──
        assertEquals(ProposalDecisionOutcomeKind.REJECTED, rejectOutcome.getKind(),
                "path-2: rejection must yield REJECTED");
        assertNotNull(rejectOutcome.getArtifact());
        assertEquals(RevisionVerdict.REJECTED, rejectOutcome.getArtifact().getVerdict(),
                "artifact verdict must be REJECTED after operator rejects (R009)");

        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        // The original knowledge (elementBefore) keeps its title/instructions unchanged (R009)
        assertEquals("Present Simple Affirmative Sentences With Third Person Singular Verbs",
                elementBefore.getKnowledge().getLabel(),
                "R009: the knowledge conserves its original title since the course was never modified");
        assertEquals("Completa la oracion con la forma correcta del verbo en presente simple.",
                elementBefore.getKnowledge().getInstructions(),
                "R009: the knowledge conserves its original instructions since the course was never modified");
    }

    // -----------------------------------------------------------------------
    // path-3: knowledge existe + agente NO produce candidato valido → failure
    // Gates: F-KTLR-R001, R002, R010
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El operador invoca la revision sobre ... → El sistema resuelve el contexto del k... → El agente de titulos recibe el contex... [El knowledge existe y se resuelve el contexto] → El sistema reporta que el agente no p... [El agente no puede producir un candidato valido] → failure")
    public void path3_elKnowledgeExisteYSeResuelveElContexto_elAgenteNoPuedeProducirUnCandidatoValido_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        AuditReport auditReport = buildAuditReportWithKnowledge();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        // Failing generator: simulates provider down / empty response (R010)
        KnowledgeTitleCandidateGenerator failingGenerator = ctx -> {
            throw new ProposalStrategyFailedException(
                    "knowledge-title-agent", TASK_ID, "provider down");
        };
        KnowledgeTitleAgentStrategy strategy = new KnowledgeTitleAgentStrategy(failingGenerator, "llm:test");
        KnowledgeTitleProposalStrategyRegistryConfig registryConfig =
                new KnowledgeTitleProposalStrategyRegistryConfig(List.of(strategy), "knowledge-title-agent");
        DefaultKnowledgeTitleProposalStrategyRegistry registry =
                new DefaultKnowledgeTitleProposalStrategyRegistry(registryConfig);
        DefaultKnowledgeTitleProposalDeriver deriver = new DefaultKnowledgeTitleProposalDeriver();
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.AUTO);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.KNOWLEDGE, KNOWLEDGE_ID))
                .thenReturn(Optional.of(elementBefore));

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(buildContextResolver());
        config.setKnowledgeTitleStrategyRegistry(registry);
        config.setKnowledgeTitleProposalDeriver(deriver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: failure → STRATEGY_FAILED (R010: no proposal, course untouched, task unchanged) ──
        assertEquals(RevisionOutcomeKind.STRATEGY_FAILED, outcome.getKind(),
                "R010: agent failure must yield STRATEGY_FAILED, not a partial proposal");
        assertNull(outcome.getArtifact(), "R010: no artifact must be created when the agent fails");
        verify(artifactStore, never()).save(any(RevisionArtifact.class));
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
        verify(planStore, never()).save(any(RefinementPlan.class));
    }

    // -----------------------------------------------------------------------
    // path-4: knowledge senalado NO existe en el curso → failure
    // Gate: F-KTLR-R011
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @Tag("path-4")
    @DisplayName("path-4: El operador invoca la revision sobre ... → El sistema resuelve el contexto del k... → El sistema reporta que el knowledge n... [El knowledge senalado no existe en el curso] → failure")
    public void path4_elKnowledgeSenaladoNoExisteEnElCurso_failure() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

        // AuditReport WITHOUT the knowledge node (R011: knowledge does not exist)
        AuditReport auditReport = buildAuditReportWithoutKnowledge();
        RefinementPlan plan = buildPlan();
        CourseEntity course = mock(CourseEntity.class);

        // The generator would signal a bug if invoked at all: R011 must abort BEFORE calling the agent.
        KnowledgeTitleCandidateGenerator neverInvokedGenerator = ctx -> {
            throw new AssertionError("R011: the agent must NOT be invoked when the knowledge does not exist");
        };
        KnowledgeTitleAgentStrategy strategy = new KnowledgeTitleAgentStrategy(neverInvokedGenerator, "llm:test");
        KnowledgeTitleProposalStrategyRegistryConfig registryConfig =
                new KnowledgeTitleProposalStrategyRegistryConfig(List.of(strategy), "knowledge-title-agent");
        DefaultKnowledgeTitleProposalStrategyRegistry registry =
                new DefaultKnowledgeTitleProposalStrategyRegistry(registryConfig);
        DefaultKnowledgeTitleProposalDeriver deriver = new DefaultKnowledgeTitleProposalDeriver();
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.AUTO);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(buildContextResolver());
        config.setKnowledgeTitleStrategyRegistry(registry);
        config.setKnowledgeTitleProposalDeriver(deriver);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: failure — no context, no agent invocation, no proposal, course untouched (R011) ──
        // The context resolver returns empty because the knowledge is absent from the report,
        // so the engine cannot even reach the reviser/strategy — reported as CONTEXT_UNAVAILABLE.
        assertEquals(RevisionOutcomeKind.CONTEXT_UNAVAILABLE, outcome.getKind(),
                "R011: a missing knowledge must abort with CONTEXT_UNAVAILABLE (no context resolved)");
        assertNull(outcome.getArtifact(), "R011: no artifact must be created when the knowledge does not exist");
        verify(artifactStore, never()).save(any(RevisionArtifact.class));
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
        verify(planStore, never()).save(any(RefinementPlan.class));
    }
}
