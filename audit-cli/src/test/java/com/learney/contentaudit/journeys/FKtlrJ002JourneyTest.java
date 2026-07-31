package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
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
import com.learney.contentaudit.revisiondomain.lemmaabsence.LemmaAbsenceGeneratorResponse;
import com.learney.contentaudit.revisiondomain.lemmaabsence.LemmaAbsenceMvpStrategy;
import com.learney.contentaudit.revisiondomain.lemmaabsence.LemmaAbsenceQuizCandidateGenerator;
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
 * Journey J002: La revision de quizzes sigue intacta (regresion, R008).
 *
 * Cubre R008 desde el angulo observable: aprobar una revision de quiz (LEMMA_ABSENCE en este
 * test, analoga a SENTENCE_LENGTH) sigue escribiendo el quiz en el curso exactamente como antes
 * de introducir la persistencia de correcciones de titulo de knowledge — sin regresion por la
 * nueva ruta KNOWLEDGE.
 *
 * path-1: quiz corregido queda persistido igual que antes de esta feature → success
 *
 * Test en memoria: replica el flujo feliz de FLapsJ001 path-1 (LEMMA_ABSENCE) para demostrar
 * que la ruta de quizzes no cambio de comportamiento con la introduccion de KNOWLEDGE_TITLE_LENGTH.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-KTLR")
@Tag("F-KTLR-J002")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FKtlrJ002JourneyTest {

    private static final String PLAN_ID = "plan-ktlr-j002";
    private static final String AUDIT_ID = "audit-ktlr-j002";
    private static final String TASK_ID = "task-ktlr-j002";
    private static final String QUIZ_ID = "quiz-ktlr-j002";

    private static final String BEFORE_QUIZ_SENTENCE =
            "She ____ [reads] (read) books about advanced topics.";
    private static final String AFTER_QUIZ_SENTENCE =
            "She ____ [studies] (study) books every day.";

    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // AuditReport builder helpers (same convention as FLapsJ001)
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
                AbsenceType.APPEARS_TOO_LATE, 1500, null, "ADJ", null);
        DefaultQuizDiagnoses quizDx = new DefaultQuizDiagnoses();
        quizDx.setLemmaAbsenceDiagnosis(new LemmaPlacementDiagnosis(1, List.of(misplaced), 0, List.of()));

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
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-ktlr-j002", "A1", "M001");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-ktlr-j002", "Reading Comprehension", "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Reading at A1 level", "Write the correct form.", true,
                "knowledge-ktlr-j002", "Knowledge 1", "K001", null, null);
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Quiz 1", "Q001",
                "Ella lee libros sobre temas avanzados.",
                List.of("She reads books about advanced topics."),
                BEFORE_QUIZ_SENTENCE, null, null);

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
                QUIZ_ID, QUIZ_ID, "CLOZE", "knowledge-ktlr-j002",
                "She reads books about advanced topics.",
                "Write the correct form.",
                "Ella lee libros sobre temas avanzados.",
                "basics.01.Reading", "Reading Comprehension",
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "",
                List.of("She reads books about advanced topics."));
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz, null);
    }

    /** Build a RefinementPlan with one LEMMA_ABSENCE task for QUIZ_ID. */
    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Quiz about advanced topics",
                DiagnosisKind.LEMMA_ABSENCE, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    /** Wraps LemmaAbsenceContextResolver as a CorrectionContextResolver<CorrectionContext>. */
    private CorrectionContextResolver<CorrectionContext> buildContextResolver() {
        LemmaAbsenceContextResolver lapsResolver = new LemmaAbsenceContextResolver();
        return new CorrectionContextResolver<CorrectionContext>() {
            @Override public Optional<CorrectionContext> resolve(AuditReport report, RefinementTask task) {
                return lapsResolver.resolve(report, task).map(c -> (CorrectionContext) c);
            }
            @Override public Optional<CorrectionContext> resolveWithIndex(
                    com.learney.contentaudit.auditdomain.AuditNodeIndex idx, AuditReport report, RefinementTask task) {
                return resolve(report, task);
            }
            @Override public boolean supports(DiagnosisKind kind) { return lapsResolver.supports(kind); }
        };
    }

    /**
     * Build a RevisionEngineConfig wired for auto-approval and a happy-path generator that
     * returns AFTER_QUIZ_SENTENCE as the candidate. Note: the KNOWLEDGE_TITLE_LENGTH fields
     * (knowledgeTitleStrategyRegistry / knowledgeTitleProposalDeriver) are left null on purpose —
     * this journey exercises the pre-existing quiz route and must not depend on the new
     * KNOWLEDGE branch to prove R008 (no regression).
     */
    private RevisionEngineConfig buildConfigAutoHappy(
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator) {

        LemmaAbsenceQuizCandidateGenerator happyGenerator = ctx ->
                new LemmaAbsenceGeneratorResponse(AFTER_QUIZ_SENTENCE, "Ella estudia libros cada dia.");

        LemmaAbsenceMvpStrategy strategy = new LemmaAbsenceMvpStrategy(happyGenerator, null);
        LemmaAbsenceProposalStrategyRegistryConfig registryConfig =
                new LemmaAbsenceProposalStrategyRegistryConfig(List.of(strategy), "lemma-absence-llm");
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
        config.setContextResolver(buildContextResolver());
        config.setLemmaAbsenceStrategyRegistry(registry);
        config.setLemmaAbsenceProposalDeriver(deriver);
        return config;
    }

    // -----------------------------------------------------------------------
    // path-1: quiz corregido queda persistido igual que antes de esta feature → success
    // Gate: F-KTLR-R008
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador revisa y aprueba una tare... → El sistema escribe el quiz corregido ... → El curso refleja el quiz corregido y ... [El quiz corregido queda persistido igual que antes de esta feature] → success")
    public void path1_elQuizCorregidoQuedaPersistidoIgualQueAntesDeEstaFeature_success() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);

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
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: success (APPROVED_APPLIED), quiz route unaffected by the KNOWLEDGE route (R008) ──
        assertEquals(RevisionOutcomeKind.APPROVED_APPLIED, outcome.getKind(),
                "path-1: quiz revision must still end with APPROVED_APPLIED, unchanged by KTLR (R008)");
        assertNotNull(outcome.getArtifact(), "artifact must be present after APPROVED_APPLIED");

        RevisionArtifact artifact = outcome.getArtifact();
        QuizTemplateEntity elementAfterQuiz = artifact.getProposal().getElementAfter().getQuiz();
        assertNotNull(elementAfterQuiz, "elementAfter quiz must be non-null — quiz route intact (R008)");
        assertEquals(List.of("She studies books every day."), elementAfterQuiz.getSentences(),
                "R008: elementAfter.sentences must be the canonical plain sentence from the candidate DSL, exactly as before this feature");
        assertNotEquals(List.of("She reads books about advanced topics."), elementAfterQuiz.getSentences(),
                "R008: elementAfter.sentences must differ from elementBefore.sentences (revision applied)");
        assertEquals(QUIZ_ID, elementAfterQuiz.getId(), "R008: quizId must be preserved in elementAfter");
        assertEquals("knowledge-ktlr-j002", elementAfterQuiz.getKnowledgeId(),
                "R008: knowledgeId must be preserved in elementAfter");
        // F-LAPS-R014/F-RPRES-R004: the quiz route's title behavior is also unchanged by KTLR —
        // a lexical correction never touches the title, which stays copied from elementBefore.
        assertEquals("She reads books about advanced topics.", elementAfterQuiz.getTitle(),
                "R008: elementAfter.title must be preserved from elementBefore, exactly as before this feature");

        // Task marked DONE and the course was written — same as before this feature (R008)
        verify(planStore).save(any(RefinementPlan.class));
        verify(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));
    }
}
