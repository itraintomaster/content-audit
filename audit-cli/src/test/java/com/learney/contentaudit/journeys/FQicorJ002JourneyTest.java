package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.learney.contentaudit.auditdomain.quizinstruction.InstructionSeverity;
import com.learney.contentaudit.auditdomain.quizinstruction.InstructionViolation;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionComplianceChecker;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionComplianceResult;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionComplianceStatus;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionDiagnosis;
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionVerdict;
import com.learney.contentaudit.auditdomain.quizinstructionengine.DefaultQuizInstructionSubjectViewFactory;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseRepository;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import com.learney.contentaudit.refinerdomain.CorrectionContext;
import com.learney.contentaudit.refinerdomain.CorrectionContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.QuizInstructionCorrectionContext;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.revisiondomain.ApprovalMode;
import com.learney.contentaudit.revisiondomain.CourseElementLocator;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.QuizInstructionCorrectionRunStore;
import com.learney.contentaudit.revisiondomain.QuizInstructionCorrectionRunner;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionEngineConfig;
import com.learney.contentaudit.revisiondomain.RevisionValidator;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessment;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessmentInput;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessor;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CorrectionCriterion;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionOutcome;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionVerdict;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionCorrectionRunnerFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.QuizInstructionProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionAgentStrategy;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCandidateGenerator;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionRunReport;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionRunRequest;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionGeneratorResponse;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionTaskOutcome;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionTaskOutcomeKind;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Journey J002: Corregir un tramo acotado de las tareas de consigna del plan.
 *
 * path-1: el plan tiene mas tareas de este tipo que el tope de la corrida -> la corrida
 *         termina sin error, toca a lo sumo el tope, y nombra las tareas no intentadas (R008).
 * path-2: el plan tiene tantas tareas como el tope o menos -> se intentan todas, ninguna
 *         queda sin intentar, y el reporte separa en cuatro recuentos distintos la
 *         correccion propuesta, la no lograda y el diagnostico caido (R008, R009).
 *
 * Tests en memoria: construyen un AuditReport con un knowledge y tres ejercicios de
 * incumplimiento de consigna, y wirean QuizInstructionCorrectionRunner real via
 * DefaultQuizInstructionCorrectionRunnerFactory sobre un RevisionEngineConfig equivalente
 * al de FQicorJ001JourneyTest (mismo contextResolver de prueba, mismo candidateAssessor /
 * quizInstructionComplianceChecker mockeados — su correccion propia la cubren los
 * handwrittenTests dedicados de DefaultCandidateAssessor y
 * LedgerBackedQuizInstructionComplianceChecker). ApprovalMode.HUMAN: una corrida por lotes
 * deja las propuestas pendientes de revision humana, no las aplica sola.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-QICOR")
@Tag("F-QICOR-J002")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FQicorJ002JourneyTest {

    private static final String PLAN_ID = "plan-qicor-j002";
    private static final String AUDIT_ID = "audit-qicor-j002";
    private static final String KNOWLEDGE_ID = "knowledge-qicor-j002";
    private static final String TOPIC_LABEL = "Daily Routines";
    private static final String KNOWLEDGE_TITLE = "Simple Past Regular Verbs Affirmative Sentences";
    private static final String KNOWLEDGE_INSTRUCTIONS = "Completa la oracion con el verbo en pasado simple.";
    private static final Path COURSE_PATH = Path.of("./db/english-course");

    private static final String TASK_A_ID = "task-qicor-j002-a";
    private static final String TASK_B_ID = "task-qicor-j002-b";
    private static final String TASK_C_ID = "task-qicor-j002-c";
    private static final String QUIZ_A_ID = "quiz-qicor-j002-a";
    private static final String QUIZ_B_ID = "quiz-qicor-j002-b";
    private static final String QUIZ_C_ID = "quiz-qicor-j002-c";

    private static final String BEFORE_QUIZ_SENTENCE = "She ____ [walks] (walk) to school every day.";

    // -----------------------------------------------------------------------
    // AuditReport builder helpers (same convention as FQicorJ001JourneyTest)
    // -----------------------------------------------------------------------

    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    private AuditNode buildMilestoneNode(AuditNode parent) {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-qicor-j002", "A2", "M002");
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(milestone);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(new DefaultLevelDiagnoses());
        parent.getChildren().add(node);
        return node;
    }

    private AuditNode buildTopicNode(AuditNode parent) {
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-qicor-j002", TOPIC_LABEL, "T001");
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

    private AuditNode buildKnowledgeNode(AuditNode parent) {
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), KNOWLEDGE_TITLE, KNOWLEDGE_INSTRUCTIONS,
                true, KNOWLEDGE_ID, "Past Simple", "PS1", null, null);
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

    private void buildQuizNode(AuditNode parent, String quizId) {
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), quizId, "Past Simple Quiz " + quizId, "Q-" + quizId,
                "Ella camina a la escuela todos los dias.",
                List.of("She walks to school every day."),
                BEFORE_QUIZ_SENTENCE, null, null);

        DefaultQuizDiagnoses quizDx = new DefaultQuizDiagnoses();
        quizDx.setQuizInstructionDiagnosis(new QuizInstructionDiagnosis(stillViolatingVerdict(), 40.0, false));

        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(quizDx);
        parent.getChildren().add(node);
    }

    /** Build a full AuditReport with 3 sibling QUIZ_INSTRUCTION quizzes under one knowledge. */
    private AuditReport buildAuditReport() {
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode);
        AuditNode topicNode = buildTopicNode(milestoneNode);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode);
        buildQuizNode(knowledgeNode, QUIZ_A_ID);
        buildQuizNode(knowledgeNode, QUIZ_B_ID);
        buildQuizNode(knowledgeNode, QUIZ_C_ID);
        return new AuditReport(courseNode);
    }

    /** Build a RefinementPlan with 3 QUIZ_INSTRUCTION tasks, priority order A, B, C. */
    private RefinementPlan buildPlan() {
        RefinementTask taskA = new RefinementTask(
                TASK_A_ID, AuditTarget.QUIZ, QUIZ_A_ID, "Quiz A",
                DiagnosisKind.QUIZ_INSTRUCTION, 1, RefinementTaskStatus.PENDING);
        RefinementTask taskB = new RefinementTask(
                TASK_B_ID, AuditTarget.QUIZ, QUIZ_B_ID, "Quiz B",
                DiagnosisKind.QUIZ_INSTRUCTION, 2, RefinementTaskStatus.PENDING);
        RefinementTask taskC = new RefinementTask(
                TASK_C_ID, AuditTarget.QUIZ, QUIZ_C_ID, "Quiz C",
                DiagnosisKind.QUIZ_INSTRUCTION, 3, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(taskA, taskB, taskC));
    }

    private CourseElementSnapshot buildElementBefore(String quizId) {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", Arrays.asList(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("walks")),
                new SentencePartEntity(SentencePartKind.TEXT, "(walk) to school every day.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                quizId, quizId, "CLOZE", KNOWLEDGE_ID,
                KNOWLEDGE_TITLE, "",
                "Ella camina a la escuela todos los dias.",
                "basics.02.PastSimple", TOPIC_LABEL,
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "",
                List.of("She walks to school every day."));
        return new CourseElementSnapshot(AuditTarget.QUIZ, quizId, quiz, null);
    }

    private InstructionViolation tenseMismatchViolation() {
        return new InstructionViolation(
                "TENSE_MISMATCH",
                "La respuesta aceptada del hueco debe estar en pasado simple",
                "[walks]",
                "La consigna del knowledge exige el pasado simple pero la unica respuesta "
                        + "aceptada del hueco esta en presente.");
    }

    private QuizInstructionVerdict stillViolatingVerdict() {
        return new QuizInstructionVerdict(
                false, 0.9, InstructionSeverity.MAJOR,
                "La respuesta aceptada esta en presente, no en pasado simple.",
                List.of(tenseMismatchViolation()), List.of());
    }

    private QuizInstructionComplianceResult stillViolatingResult() {
        return new QuizInstructionComplianceResult(
                QuizInstructionComplianceStatus.EVALUATED, stillViolatingVerdict(), null,
                "quiz-instruction-judge:v2", false);
    }

    private QuizInstructionComplianceResult staleResult() {
        QuizInstructionVerdict verdict = new QuizInstructionVerdict(
                true, 0.95, InstructionSeverity.NONE,
                "La respuesta aceptada ya esta en pasado simple.", List.of(), List.of());
        return new QuizInstructionComplianceResult(
                QuizInstructionComplianceStatus.EVALUATED, verdict, null, "quiz-instruction-judge:v2", false);
    }

    /**
     * Correction context per task (F-QICOR-R002). Same rationale as FQicorJ001JourneyTest:
     * QuizInstructionContextResolver has no `implements` declared yet in sentinel.yaml, so this
     * journey wires a direct CorrectionContextResolver test double keyed by task/quiz id instead.
     */
    private QuizInstructionCorrectionContext buildContext(RefinementTask task) {
        return new QuizInstructionCorrectionContext(
                task.getId(),
                task.getNodeId(),
                BEFORE_QUIZ_SENTENCE,
                "Ella camina a la escuela todos los dias.",
                "She walks to school every day.",
                KNOWLEDGE_TITLE,
                KNOWLEDGE_INSTRUCTIONS,
                TOPIC_LABEL,
                CefrLevel.A2,
                "A2",
                SentenceMode.FILL,
                List.of(tenseMismatchViolation()),
                "El ejercicio no cumple su consigna: la respuesta aceptada del hueco esta en "
                        + "presente, no en pasado.",
                InstructionSeverity.MAJOR,
                List.of(),
                AUDIT_ID);
    }

    private CorrectionContextResolver<CorrectionContext> buildContextResolver() {
        return new CorrectionContextResolver<CorrectionContext>() {
            @Override
            public Optional<CorrectionContext> resolve(AuditReport report, RefinementTask task) {
                if (task.getDiagnosisKind() != DiagnosisKind.QUIZ_INSTRUCTION) {
                    return Optional.empty();
                }
                return Optional.of(buildContext(task));
            }
            @Override
            public Optional<CorrectionContext> resolveWithIndex(
                    com.learney.contentaudit.auditdomain.AuditNodeIndex nodeIndex, AuditReport report,
                    RefinementTask task) {
                return resolve(report, task);
            }
            @Override
            public boolean supports(DiagnosisKind kind) {
                return kind == DiagnosisKind.QUIZ_INSTRUCTION;
            }
        };
    }

    private RevisionEngineConfig buildConfig(
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator,
            QuizInstructionComplianceChecker complianceChecker,
            CandidateAssessor candidateAssessor,
            QuizInstructionCandidateGenerator generator,
            QuizInstructionCorrectionConfig correctionConfig) {

        QuizInstructionAgentStrategy strategy = new QuizInstructionAgentStrategy(generator, "llm:test");
        QuizInstructionProposalStrategyRegistryConfig registryConfig =
                new QuizInstructionProposalStrategyRegistryConfig(List.of(strategy), "quiz-instruction-agent");
        DefaultQuizInstructionProposalStrategyRegistry registry =
                new DefaultQuizInstructionProposalStrategyRegistry(registryConfig);

        // A batch run leaves proposals pending for human review rather than auto-applying them.
        RevisionValidator validator = new DefaultRevisionValidatorFactory().create(ApprovalMode.HUMAN);

        RevisionEngineConfig config = new RevisionEngineConfig();
        config.setRevisers(java.util.Map.of());
        config.setValidator(validator);
        config.setArtifactStore(artifactStore);
        config.setCourseRepository(courseRepository);
        config.setElementLocator(elementLocator);
        config.setRefinementPlanStore(planStore);
        config.setAuditReportStore(auditReportStore);
        config.setContextResolver(buildContextResolver());
        config.setQuizInstructionStrategyRegistry(registry);
        config.setCandidateAssessor(candidateAssessor);
        config.setQuizInstructionComplianceChecker(complianceChecker);
        config.setQuizInstructionSubjectViewFactory(new DefaultQuizInstructionSubjectViewFactory());
        config.setLemmaAbsenceProposalDeriver(
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create()));
        config.setQuizInstructionCorrectionConfig(correctionConfig);
        return config;
    }

    // -----------------------------------------------------------------------
    // path-1: el plan tiene mas tareas que el tope -> tramo acotado -> success
    // Gate: F-QICOR-R008
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador pide corregir las tareas ... → El sistema intenta corregir tareas ha... → La corrida termina sin error habiendo... [El plan tiene mas tareas de este tipo que el tope de la corrida] → success")
    public void path1_elPlanTieneMasTareasDeEsteTipoQueElTopeDeLaCorrida_success() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);
        QuizInstructionCorrectionRunStore runStore = mock(QuizInstructionCorrectionRunStore.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseEntity course = mock(CourseEntity.class);

        // Every touched task revalidates as already compliant: no candidate must ever be requested.
        when(complianceChecker.revalidate(any())).thenReturn(staleResult());
        QuizInstructionCandidateGenerator neverInvokedGenerator = request -> {
            throw new AssertionError("R009: no candidate must be generated once the diagnosis no longer holds");
        };

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(eq(PLAN_ID), anyString())).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_A_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_A_ID)));
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_B_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_B_ID)));
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_C_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_C_ID)));

        QuizInstructionCorrectionConfig correctionConfig = new QuizInstructionCorrectionConfig(3, 20);
        RevisionEngineConfig config = buildConfig(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, neverInvokedGenerator, correctionConfig);

        QuizInstructionCorrectionRunner runner = new DefaultQuizInstructionCorrectionRunnerFactory()
                .create(config, runStore, correctionConfig);

        // ── Act: request a run capped at 2, with 3 eligible tasks in the plan ─────
        QuizInstructionCorrectionRunReport report = runner.run(
                new QuizInstructionCorrectionRunRequest(PLAN_ID, COURSE_PATH, 2));

        // ── Assert: success — the run stops at the cap and names what it left out (R008) ──
        assertEquals(3, report.getEligibleTasks(), "3 QUIZ_INSTRUCTION tasks are eligible in the plan");
        assertEquals(2, report.getAttempted(), "the run must touch at most the 2-task cap");
        assertEquals(0, report.getProposed());
        assertEquals(0, report.getNotCorrected());
        assertEquals(2, report.getDiagnosisStale());
        assertEquals(0, report.getFailed());
        assertEquals(List.of(TASK_C_ID), report.getNotAttempted(),
                "R008: the task the cap left out must be named, not just counted, and stays available");

        verify(candidateAssessor, never()).assess(any(CandidateAssessmentInput.class));
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
    }

    // -----------------------------------------------------------------------
    // path-2: el plan tiene tantas tareas como el tope o menos -> todas intentadas -> success
    // Gates: F-QICOR-R008, R009
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador pide corregir las tareas ... → El sistema intenta corregir tareas ha... → La corrida intenta todas las tareas d... [El plan tiene tantas tareas de este tipo como el tope o menos] → success")
    public void path2_elPlanTieneTantasTareasDeEsteTipoComoElTopeOMenos_success() {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);
        QuizInstructionCorrectionRunStore runStore = mock(QuizInstructionCorrectionRunStore.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseEntity course = mock(CourseEntity.class);

        // C's diagnosis no longer holds; A and B still violate their consigna (default).
        when(complianceChecker.revalidate(any())).thenReturn(stillViolatingResult());
        when(complianceChecker.revalidate(argThat(view -> view != null && QUIZ_C_ID.equals(view.getSubjectRef()))))
                .thenReturn(staleResult());

        // A's candidate passes every criterion; B's candidate always fails LENGTH_IN_RANGE.
        CandidateAssessment acceptedAssessment = new CandidateAssessment(true, List.of(
                new CriterionVerdict(CorrectionCriterion.INSTRUCTION_COMPLIANCE, CriterionOutcome.PASSED, null)));
        CandidateAssessment rejectedAssessment = new CandidateAssessment(false, List.of(
                new CriterionVerdict(CorrectionCriterion.LENGTH_IN_RANGE, CriterionOutcome.FAILED,
                        "La oracion corregida cae fuera del rango de longitud objetivo del nivel A2.")));
        when(candidateAssessor.assess(argThat(input -> input != null && QUIZ_A_ID.equals(input.getSubjectRef()))))
                .thenReturn(acceptedAssessment);
        when(candidateAssessor.assess(argThat(input -> input != null && QUIZ_B_ID.equals(input.getSubjectRef()))))
                .thenReturn(rejectedAssessment);

        QuizInstructionCandidateGenerator generator = request ->
                new QuizInstructionGeneratorResponse(
                        "She ____ [walked] (walk) to school every day.",
                        "Ella caminaba a la escuela todos los dias.",
                        "Cambio el tiempo verbal de la respuesta aceptada a pasado simple.");

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(eq(PLAN_ID), anyString())).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_A_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_A_ID)));
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_B_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_B_ID)));
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_C_ID))
                .thenReturn(Optional.of(buildElementBefore(QUIZ_C_ID)));
        when(artifactStore.save(any(RevisionArtifact.class))).thenReturn(
                ".content-audit/revisions/" + PLAN_ID + "/proposal-a");

        QuizInstructionCorrectionConfig correctionConfig = new QuizInstructionCorrectionConfig(3, 20);
        RevisionEngineConfig config = buildConfig(
                planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, generator, correctionConfig);

        QuizInstructionCorrectionRunner runner = new DefaultQuizInstructionCorrectionRunnerFactory()
                .create(config, runStore, correctionConfig);

        // ── Act: request a run with a cap larger than the 3 eligible tasks ────────
        QuizInstructionCorrectionRunReport report = runner.run(
                new QuizInstructionCorrectionRunRequest(PLAN_ID, COURSE_PATH, 10));

        // ── Assert: success — every task attempted, four separate counts (R008, R009) ──
        assertEquals(3, report.getEligibleTasks());
        assertEquals(3, report.getAttempted(), "with a cap above the eligible count, every task must be touched");
        assertTrue(report.getNotAttempted().isEmpty(), "R008: with enough cap, no task is declared unattempted");
        assertEquals(1, report.getProposed());
        assertEquals(1, report.getNotCorrected());
        assertEquals(1, report.getDiagnosisStale());
        assertEquals(0, report.getFailed());

        Map<String, QuizInstructionTaskOutcome> outcomesByTask = report.getOutcomes().stream()
                .collect(Collectors.toMap(QuizInstructionTaskOutcome::getTaskId, o -> o));

        QuizInstructionTaskOutcome outcomeA = outcomesByTask.get(TASK_A_ID);
        assertEquals(QuizInstructionTaskOutcomeKind.PROPOSED, outcomeA.getKind(),
                "R004/R005: an accepted candidate must be reported as a proposed correction");
        assertNotNull(outcomeA.getProposalId(), "R008: a proposed correction must carry the proposal id to review");

        QuizInstructionTaskOutcome outcomeB = outcomesByTask.get(TASK_B_ID);
        assertEquals(QuizInstructionTaskOutcomeKind.NOT_CORRECTED, outcomeB.getKind(),
                "R006: a candidate that never passes the catalog must be reported as not corrected");
        assertTrue(outcomeB.getFailedCriteria().contains(CorrectionCriterion.LENGTH_IN_RANGE),
                "R006: a correction not achieved must name the criterion that failed");

        QuizInstructionTaskOutcome outcomeC = outcomesByTask.get(TASK_C_ID);
        assertEquals(QuizInstructionTaskOutcomeKind.DIAGNOSIS_STALE, outcomeC.getKind(),
                "R009: a diagnosis that no longer holds must be reported apart from a failed correction");
        assertEquals(0, outcomeC.getAttempts(), "R009: a fallen diagnosis spends no generation attempt");
        assertTrue(outcomeC.getFailedCriteria().isEmpty(),
                "R009: a fallen diagnosis has no criterion to blame — it never reached candidate generation");

        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
    }
}
