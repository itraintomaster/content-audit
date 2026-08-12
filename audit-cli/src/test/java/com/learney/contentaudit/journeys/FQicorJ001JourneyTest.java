package com.learney.contentaudit.journeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.learney.contentaudit.auditdomain.quizinstruction.QuizInstructionSubjectView;
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
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessment;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessmentInput;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateAssessor;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CandidateLengthMeasurement;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CorrectionCriterion;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionOutcome;
import com.learney.contentaudit.revisiondomain.candidatecriteria.CriterionVerdict;
import com.learney.contentaudit.revisiondomain.engine.DefaultLemmaAbsenceProposalDeriver;
import com.learney.contentaudit.revisiondomain.engine.DefaultProposalDecisionServiceFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultQuizInstructionProposalStrategyRegistry;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionEngineFactory;
import com.learney.contentaudit.revisiondomain.engine.DefaultRevisionValidatorFactory;
import com.learney.contentaudit.revisiondomain.engine.QuizInstructionProposalStrategyRegistryConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionAgentStrategy;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCandidateGenerator;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionCorrectionConfig;
import com.learney.contentaudit.revisiondomain.quizinstruction.QuizInstructionGeneratorResponse;
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
import org.mockito.ArgumentCaptor;

/**
 * Journey J001: Corregir un ejercicio que incumple su consigna, y decidir la correccion.
 *
 * path-1: la revalidacion previa (R009) declara que el original ya cumple -> diagnostico
 *         caido, un desenlace de EXITO (no un fracaso): no se genera ningun candidato.
 * path-2: el original sigue incumpliendo, el candidato pasa los criterios (R004, R005) y la
 *         propuesta se aprueba -> el curso queda con el ejercicio corregido (R001, R003, R007),
 *         conservando la parte vacia del enunciado que el candidato no trajo (R010).
 * path-3: el original sigue incumpliendo, el candidato pasa los criterios, pero el operador
 *         rechaza la propuesta -> el curso no cambia (R007).
 * path-4: el original sigue incumpliendo y, agotados los intentos, ningun candidato llego a
 *         cumplir su consigna -> desenlace explicito de fracaso, con los criterios reprobados
 *         nombrados y la longitud excluida de esa lista (R006, R011): desde R012 reprobar
 *         cualquier otro criterio del catalogo ya no alcanza para llegar a este desenlace.
 *
 * Tests en memoria: construyen un AuditReport programaticamente (course -> milestone -> topic
 * -> knowledge -> quiz), wirean el RevisionEngine real via DefaultRevisionEngineFactory,
 * poblando RevisionEngineConfig.quizInstructionStrategyRegistry / candidateAssessor /
 * quizInstructionComplianceChecker / quizInstructionSubjectViewFactory /
 * quizInstructionCorrectionConfig (espejo del wiring de FKtlrJ001/FLapsJ001). El
 * contextResolver es un doble de prueba que implementa CorrectionContextResolver
 * directamente (QuizInstructionContextResolver todavia no declara `implements` en
 * sentinel.yaml — su propia cobertura de F-QICOR-R002 vive en sus handwrittenTests
 * dedicados, no en este journey). candidateAssessor y quizInstructionComplianceChecker se
 * mockean directamente: su correccion propia (R004, R005, R006) la cubren los
 * handwrittenTests dedicados de DefaultCandidateAssessor / LedgerBackedQuizInstructionComplianceChecker;
 * este journey verifica que el motor los invoque en el orden y con el efecto que describen
 * los gates del flujo.
 */
@Generated(
        value = "com.sentinel.SentinelEngine",
        comments = "Generated by Sentinel — journey test stubs"
)
@Tag("F-QICOR")
@Tag("F-QICOR-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FQicorJ001JourneyTest {

    private static final String PLAN_ID = "plan-qicor-j001";
    private static final String AUDIT_ID = "audit-qicor-j001";
    private static final String TASK_ID = "task-qicor-j001";
    private static final String QUIZ_ID = "quiz-qicor-j001";
    private static final String KNOWLEDGE_ID = "knowledge-qicor-j001";
    private static final String TOPIC_LABEL = "Daily Routines";
    private static final String KNOWLEDGE_TITLE = "Simple Past Regular Verbs Affirmative Sentences";
    private static final String KNOWLEDGE_INSTRUCTIONS = "Completa la oracion con el verbo en pasado simple.";

    private static final String BEFORE_QUIZ_SENTENCE = "She ____ [walks] (walk) to school every day.";
    private static final String AFTER_QUIZ_SENTENCE = "She ____ [walked] (walk) to school every day.";

    private static final Path COURSE_PATH = Path.of("./db/english-course");

    // -----------------------------------------------------------------------
    // AuditReport builder helpers (same convention as FKtlrJ001/FLapsJ001)
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
        return node;
    }

    /** Build a full AuditReport rooted at COURSE -> MILESTONE -> TOPIC -> KNOWLEDGE -> QUIZ. */
    private AuditReport buildAuditReport() {
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-qicor-j001", "A2", "M002");
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-qicor-j001", TOPIC_LABEL, "T001");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), KNOWLEDGE_TITLE, KNOWLEDGE_INSTRUCTIONS,
                true, KNOWLEDGE_ID, "Past Simple", "PS1", null, null);
        AuditableQuiz quiz = new AuditableQuiz(
                List.of(), QUIZ_ID, "Past Simple Quiz 1", "Q001",
                "Ella camina a la escuela todos los dias.",
                List.of("She walks to school every day."),
                BEFORE_QUIZ_SENTENCE, null, null);

        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz);
        return new AuditReport(courseNode);
    }

    /** Build a RefinementPlan with one QUIZ_INSTRUCTION task for QUIZ_ID. */
    private RefinementPlan buildPlan() {
        RefinementTask task = new RefinementTask(
                TASK_ID, AuditTarget.QUIZ, QUIZ_ID, "Past Simple Quiz 1",
                DiagnosisKind.QUIZ_INSTRUCTION, 1, RefinementTaskStatus.PENDING);
        return new RefinementPlan(PLAN_ID, AUDIT_ID, Instant.now(), List.of(task));
    }

    /** The elementBefore snapshot: the quiz as it currently exists in the course. */
    private CourseElementSnapshot buildElementBefore() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", Arrays.asList(
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("walks")),
                new SentencePartEntity(SentencePartKind.TEXT, "(walk) to school every day.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", KNOWLEDGE_ID,
                KNOWLEDGE_TITLE, "",
                "Ella camina a la escuela todos los dias.",
                "basics.02.PastSimple", TOPIC_LABEL,
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "",
                List.of("She walks to school every day."));
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz, null);
    }

    /**
     * path-2 only (F-QICOR-R010): the same quiz as {@link #buildElementBefore()}, with one extra
     * empty leading TEXT part — zero characters, contributes nothing to the rendered/parsed quiz
     * sentence — prepended. Because it renders to nothing, {@link #AFTER_QUIZ_SENTENCE} (parsed
     * back into three parts: "She", the gap, the trailing cue+text) already looks exactly like a
     * candidate that came back without it: no other fixture in this path needs to change, only
     * the original's part COUNT and the presence of the empty part at position 0. Kept local to
     * this path so paths 1/3/4 keep using the shared buildElementBefore() (three parts, none
     * empty) untouched.
     */
    private CourseElementSnapshot buildElementBeforeWithEmptyLeadingPart() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", Arrays.asList(
                new SentencePartEntity(SentencePartKind.TEXT, "", null),
                new SentencePartEntity(SentencePartKind.TEXT, "She", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("walks")),
                new SentencePartEntity(SentencePartKind.TEXT, "(walk) to school every day.", null)
        ));
        QuizTemplateEntity quiz = new QuizTemplateEntity(
                QUIZ_ID, QUIZ_ID, "CLOZE", KNOWLEDGE_ID,
                KNOWLEDGE_TITLE, "",
                "Ella camina a la escuela todos los dias.",
                "basics.02.PastSimple", TOPIC_LABEL,
                form, 0.0, 0.0, 0.0, "", "", "", "", "", "", "",
                List.of("She walks to school every day."));
        return new CourseElementSnapshot(AuditTarget.QUIZ, QUIZ_ID, quiz, null);
    }

    /** The violation the original diagnosis of consigna recorded (F-QICOR-R002). */
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

    private QuizInstructionComplianceResult nowCompliantResult() {
        QuizInstructionVerdict verdict = new QuizInstructionVerdict(
                true, 0.95, InstructionSeverity.NONE,
                "La respuesta aceptada ya esta en pasado simple.", List.of(), List.of());
        return new QuizInstructionComplianceResult(
                QuizInstructionComplianceStatus.EVALUATED, verdict, null, "quiz-instruction-judge:v2", false);
    }

    /**
     * Correction context fed to the engine (F-QICOR-R002: violations, evidence, level, siblings).
     * QuizInstructionContextResolver has no `implements` declared yet in sentinel.yaml (its own
     * resolve/supports methods do not exist on the generated stub), so this journey wires a
     * direct CorrectionContextResolver test double instead of depending on that unfinished class.
     * R002's own fidelity is covered by QuizInstructionContextResolver's dedicated handwrittenTests.
     */
    private QuizInstructionCorrectionContext buildCorrectionContext(RefinementTask task) {
        return new QuizInstructionCorrectionContext(
                task.getId(),
                QUIZ_ID,
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
                AUDIT_ID,
                PLAN_ID);
    }

    private CorrectionContextResolver<CorrectionContext> buildContextResolver() {
        return new CorrectionContextResolver<CorrectionContext>() {
            @Override
            public Optional<CorrectionContext> resolve(AuditReport report, RefinementTask task) {
                if (task.getDiagnosisKind() != DiagnosisKind.QUIZ_INSTRUCTION) {
                    return Optional.empty();
                }
                return Optional.of(buildCorrectionContext(task));
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

    /** Builds a RevisionEngineConfig wired for QUIZ_INSTRUCTION with the given collaborators. */
    private RevisionEngineConfig buildConfig(
            ApprovalMode mode,
            RefinementPlanStore planStore,
            AuditReportStore auditReportStore,
            RevisionArtifactStore artifactStore,
            CourseRepository courseRepository,
            CourseElementLocator elementLocator,
            QuizInstructionComplianceChecker complianceChecker,
            CandidateAssessor candidateAssessor,
            QuizInstructionCandidateGenerator generator) {

        QuizInstructionAgentStrategy strategy = new QuizInstructionAgentStrategy(generator, "llm:test");
        QuizInstructionProposalStrategyRegistryConfig registryConfig =
                new QuizInstructionProposalStrategyRegistryConfig(List.of(strategy), "quiz-instruction-agent");
        DefaultQuizInstructionProposalStrategyRegistry registry =
                new DefaultQuizInstructionProposalStrategyRegistry(registryConfig);

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
        config.setQuizInstructionStrategyRegistry(registry);
        config.setCandidateAssessor(candidateAssessor);
        config.setQuizInstructionComplianceChecker(complianceChecker);
        config.setQuizInstructionSubjectViewFactory(new DefaultQuizInstructionSubjectViewFactory());
        config.setLemmaAbsenceProposalDeriver(
                new DefaultLemmaAbsenceProposalDeriver(DefaultQuizSentenceConverter.create()));
        config.setQuizInstructionCorrectionConfig(new QuizInstructionCorrectionConfig(3, 20));
        return config;
    }

    // -----------------------------------------------------------------------
    // path-1: revalidacion previa dice que el original ya cumple -> diagnostico caido -> success
    // Gate: F-QICOR-R009
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El operador pide la revision de una t... → El sistema reune los datos de partida... → El sistema somete el ejercicio origin... → La tarea termina con el desenlace de ... [La validacion vigente declara que el ejercicio original cumple su consigna: el diagnostico que origino la tarea ya no se sostiene] → success")
    public void path1_laValidacionVigenteDeclaraQueElEjercicioOriginalCumpleSuConsignaElDiagnosticoQueOriginoLaTareaYaNoSeSostiene_success(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        // R009: the judge, asked again, now says the original already complies.
        when(complianceChecker.revalidate(any(QuizInstructionSubjectView.class))).thenReturn(nowCompliantResult());

        // The candidate generator must NEVER be invoked: R009 aborts before producing any correction.
        QuizInstructionCandidateGenerator neverInvokedGenerator = request -> {
            throw new AssertionError("R009: no candidate must be generated once the original already complies");
        };

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID)).thenReturn(Optional.of(elementBefore));

        RevisionEngineConfig config = buildConfig(
                ApprovalMode.AUTO, planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, neverInvokedGenerator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: success — diagnostico caido, NOT a failure (R009) ─────────────
        assertEquals(RevisionOutcomeKind.DIAGNOSIS_NOT_SUSTAINED, outcome.getKind(),
                "path-1: a compliant original must end with DIAGNOSIS_NOT_SUSTAINED, a success outcome (R009)");
        assertNull(outcome.getArtifact(), "R009: no artifact must be produced when the diagnosis no longer holds");

        verify(candidateAssessor, never()).assess(any(CandidateAssessmentInput.class));
        verify(artifactStore, never()).save(any(RevisionArtifact.class));
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));

        ArgumentCaptor<RefinementPlan> planCaptor = ArgumentCaptor.forClass(RefinementPlan.class);
        verify(planStore).save(planCaptor.capture());
        RefinementTask savedTask = planCaptor.getValue().getTasks().stream()
                .filter(t -> TASK_ID.equals(t.getId())).findFirst().orElseThrow();
        assertEquals(RefinementTaskStatus.STALE, savedTask.getStatus(),
                "R009: the task must be marked STALE — visible, but not retried in following runs");
    }

    // -----------------------------------------------------------------------
    // path-2: sigue incumpliendo, candidato pasa los criterios, se aprueba -> success
    // Gates: F-QICOR-R001, R003, R004, R005, R007, R009, R010
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El operador pide la revision de una t... → El sistema reune los datos de partida... → El sistema somete el ejercicio origin... → El sistema obtiene un ejercicio corre... [La validacion vigente confirma que el ejercicio original sigue incumpliendo su consigna] → Queda una propuesta revisable cuyo es... [El ejercicio corregido pasa los cinco criterios de no regresion y la validacion de consigna declara que ahora cumple] → La propuesta se decide con el mismo v... → El ejercicio del curso queda con el c... [La propuesta se aprueba] → success")
    public void path2_laValidacionVigenteConfirmaQueElEjercicioOriginalSigueIncumpliendoSuConsigna_elEjercicioCorregidoPasaLosCincoCriteriosDeNoRegresionYLaValidacionDeConsignaDeclaraQueAhoraCumple_laPropuestaSeAprueba_success(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        // F-QICOR-R010: the original has an empty leading TEXT part; the candidate the generator
        // returns below parses to only three parts, without it.
        CourseElementSnapshot elementBefore = buildElementBeforeWithEmptyLeadingPart();
        CourseEntity course = mock(CourseEntity.class);
        CourseEntity updatedCourse = mock(CourseEntity.class);

        when(complianceChecker.revalidate(any(QuizInstructionSubjectView.class))).thenReturn(stillViolatingResult());

        List<CriterionVerdict> allPassedVerdicts = List.of(
                new CriterionVerdict(CorrectionCriterion.SCOPED_EDIT, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.LENGTH_IN_RANGE, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.LEVEL_VOCABULARY, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.DISTINCTNESS, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.FAITHFUL_TRANSLATION, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.SOLVABLE_SAME_KIND, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.INSTRUCTION_COMPLIANCE, CriterionOutcome.PASSED, null));
        CandidateAssessment acceptedAssessment = new CandidateAssessment(
                allPassedVerdicts, true, true, 5, List.of(),
                new CandidateLengthMeasurement(9, 6, 10, true, true), "eligible|5");
        when(candidateAssessor.assess(any(CandidateAssessmentInput.class))).thenReturn(acceptedAssessment);

        QuizInstructionCandidateGenerator happyGenerator = request ->
                new QuizInstructionGeneratorResponse(AFTER_QUIZ_SENTENCE,
                        "Ella caminaba a la escuela todos los dias.",
                        "Cambio el tiempo verbal de la respuesta aceptada a pasado simple.",
                        List.of(), 1);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID)).thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class))).thenReturn(updatedCourse);
        when(artifactStore.save(any(RevisionArtifact.class))).thenReturn(
                ".content-audit/revisions/" + PLAN_ID + "/proposal-1");

        RevisionEngineConfig config = buildConfig(
                ApprovalMode.AUTO, planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, happyGenerator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: success (APPROVED_APPLIED) ────────────────────────────────────
        assertEquals(RevisionOutcomeKind.APPROVED_APPLIED, outcome.getKind(),
                "path-2: an accepted candidate, auto-approved, must end with APPROVED_APPLIED "
                        + "(R001, R004, R005, R007)");
        assertNotNull(outcome.getArtifact(), "artifact must be present after APPROVED_APPLIED");

        RevisionArtifact artifact = outcome.getArtifact();
        QuizTemplateEntity elementAfterQuiz = artifact.getProposal().getElementAfter().getQuiz();
        assertNotNull(elementAfterQuiz, "R001: elementAfter.quiz must be populated with a state distinct from the original");
        assertEquals(List.of("She walked to school every day."), elementAfterQuiz.getSentences(),
                "R005: the corrected sentence must be the candidate's, now in past simple");
        assertEquals("Ella caminaba a la escuela todos los dias.", elementAfterQuiz.getTranslation(),
                "the translation must be the candidate's");
        assertEquals(QUIZ_ID, elementAfterQuiz.getId(), "R007: the quiz id must be preserved");
        assertEquals(KNOWLEDGE_ID, elementAfterQuiz.getKnowledgeId(), "R007: the knowledgeId must be preserved");
        assertEquals(KNOWLEDGE_TITLE, elementAfterQuiz.getTitle(),
                "R003: the title stays identical since no violation signalled it");

        verify(planStore).save(any(RefinementPlan.class));
        verify(courseRepository).save(any(CourseEntity.class), eq(COURSE_PATH));

        // R010: this path mocks the write boundary (elementLocator/courseRepository), so it
        // cannot observe a JSON round-trip — what it CAN observe, and what R010 requires here, is
        // the quiz handed to that boundary. elementBefore has an empty leading TEXT part
        // (position 0); the candidate the generator returned parses to only three parts, without
        // it. The quiz delivered to elementLocator.replace(...) — the object that becomes what
        // courseRepository.save(...) persists — must still carry all four parts, the empty one
        // back in its original position.
        ArgumentCaptor<CourseElementSnapshot> elementAfterCaptor =
                ArgumentCaptor.forClass(CourseElementSnapshot.class);
        verify(elementLocator).replace(eq(course), elementAfterCaptor.capture());
        List<SentencePartEntity> deliveredParts =
                elementAfterCaptor.getValue().getQuiz().getForm().getSentenceParts();

        assertEquals(4, deliveredParts.size(),
                "R010: the empty part the original had must not be lost when the candidate came "
                        + "back without it");
        assertEquals(SentencePartKind.TEXT, deliveredParts.get(0).getKind(),
                "R010: the conserved part must be back in its original position, the first");
        assertEquals("", deliveredParts.get(0).getText(),
                "R010: the conserved part must still be empty");
        assertEquals("She", deliveredParts.get(1).getText(),
                "R010: conserving the empty part must not shift the parts that follow it");
        assertEquals(SentencePartKind.CLOZE, deliveredParts.get(2).getKind(),
                "the corrected gap must still be the third part, right where the original had it");
        assertEquals(List.of("walked"), deliveredParts.get(2).getOptions(),
                "the gap keeps the candidate's correction — R010 does not undo R001/R005");
        assertEquals("(walk) to school every day.", deliveredParts.get(3).getText(),
                "the trailing part no violation signalled stays identical (R003)");
    }

    // -----------------------------------------------------------------------
    // path-3: sigue incumpliendo, candidato pasa los criterios, pero se rechaza -> failure
    // Gates: F-QICOR-R001, R004, R005, R007, R009
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El operador pide la revision de una t... → El sistema reune los datos de partida... → El sistema somete el ejercicio origin... → El sistema obtiene un ejercicio corre... [La validacion vigente confirma que el ejercicio original sigue incumpliendo su consigna] → Queda una propuesta revisable cuyo es... [El ejercicio corregido pasa los cinco criterios de no regresion y la validacion de consigna declara que ahora cumple] → La propuesta se decide con el mismo v... → El curso no se modifica: el ejercicio... [El operador rechaza la propuesta] → failure")
    public void path3_laValidacionVigenteConfirmaQueElEjercicioOriginalSigueIncumpliendoSuConsigna_elEjercicioCorregidoPasaLosCincoCriteriosDeNoRegresionYLaValidacionDeConsignaDeclaraQueAhoraCumple_elOperadorRechazaLaPropuesta_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        when(complianceChecker.revalidate(any(QuizInstructionSubjectView.class))).thenReturn(stillViolatingResult());

        CandidateAssessment acceptedAssessment = new CandidateAssessment(
                List.of(new CriterionVerdict(CorrectionCriterion.INSTRUCTION_COMPLIANCE,
                        CriterionOutcome.PASSED, null)),
                true, true, 0, List.of(),
                new CandidateLengthMeasurement(8, 6, 10, true, true), "eligible");
        when(candidateAssessor.assess(any(CandidateAssessmentInput.class))).thenReturn(acceptedAssessment);

        QuizInstructionCandidateGenerator happyGenerator = request ->
                new QuizInstructionGeneratorResponse(AFTER_QUIZ_SENTENCE,
                        "Ella caminaba a la escuela todos los dias.",
                        "Cambio el tiempo verbal de la respuesta aceptada a pasado simple.",
                        List.of(), 1);

        List<RevisionArtifact> savedArtifacts = new ArrayList<>();
        when(artifactStore.save(any(RevisionArtifact.class))).thenAnswer(inv -> {
            RevisionArtifact a = inv.getArgument(0);
            savedArtifacts.removeIf(existing -> existing.getProposal() != null
                    && a.getProposal() != null
                    && existing.getProposal().getProposalId().equals(a.getProposal().getProposalId()));
            savedArtifacts.add(a);
            return ".content-audit/revisions/" + PLAN_ID + "/"
                    + (a.getProposal() != null ? a.getProposal().getProposalId() : "unknown");
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
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID)).thenReturn(Optional.of(elementBefore));
        when(elementLocator.replace(eq(course), any(CourseElementSnapshot.class))).thenReturn(course);

        RevisionEngineConfig config = buildConfig(
                ApprovalMode.HUMAN, planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, happyGenerator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act: Step 1 — revise in human mode → PENDING_APPROVAL_PERSISTED ───────
        RevisionOutcome reviseOutcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);
        assertEquals(RevisionOutcomeKind.PENDING_APPROVAL_PERSISTED, reviseOutcome.getKind(),
                "human validator must produce PENDING_APPROVAL_PERSISTED for an accepted candidate");
        assertNotNull(reviseOutcome.getArtifact());
        String proposalId = reviseOutcome.getArtifact().getProposal().getProposalId();

        // ── Act: Step 2 — the operator rejects the proposal ───────────────────────
        ProposalDecisionService decisionService = new DefaultProposalDecisionServiceFactory().create(config);
        ProposalDecisionOutcome rejectOutcome = decisionService.reject(
                proposalId, Optional.of(PLAN_ID), Optional.of("La consigna quedo mal reflejada."));

        // ── Assert: failure — course untouched, original preserved (R007) ─────────
        assertEquals(ProposalDecisionOutcomeKind.REJECTED, rejectOutcome.getKind(),
                "path-3: rejection must yield REJECTED");
        assertNotNull(rejectOutcome.getArtifact());
        assertEquals(RevisionVerdict.REJECTED, rejectOutcome.getArtifact().getVerdict(),
                "the artifact's verdict must be REJECTED after the operator rejects (R007)");

        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
        assertEquals("She walks to school every day.", elementBefore.getQuiz().getSentences().get(0),
                "R007: rejecting must leave the original quiz sentence untouched");
    }

    // -----------------------------------------------------------------------
    // path-4: sigue incumpliendo y, agotados los intentos, ningun candidato llego a cumplir su
    // consigna -> failure. Reprobar la longitud -- o cualquier otro criterio del catalogo -- ya
    // no alcanza para este desenlace (R012); la longitud tampoco puede nombrarse como causa (R011).
    // Gates: F-QICOR-R006, F-QICOR-R011
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @Tag("path-4")
    @DisplayName("path-4: El operador pide la revision de una t... → El sistema reune los datos de partida... → El sistema somete el ejercicio origin... → El sistema obtiene un ejercicio corre... [La validacion vigente confirma que el ejercicio original sigue incumpliendo su consigna] → El sistema informa que no logro una c... [Agotados los intentos, ningun ejercicio corregido pasa todos los criterios] → failure")
    public void path4_laValidacionVigenteConfirmaQueElEjercicioOriginalSigueIncumpliendoSuConsigna_agotadosLosIntentosNingunEjercicioCorregidoPasaTodosLosCriterios_failure(
            ) {
        // ── Arrange ──────────────────────────────────────────────────────────────
        RefinementPlanStore planStore = mock(RefinementPlanStore.class);
        AuditReportStore auditReportStore = mock(AuditReportStore.class);
        RevisionArtifactStore artifactStore = mock(RevisionArtifactStore.class);
        CourseRepository courseRepository = mock(CourseRepository.class);
        CourseElementLocator elementLocator = mock(CourseElementLocator.class);
        QuizInstructionComplianceChecker complianceChecker = mock(QuizInstructionComplianceChecker.class);
        CandidateAssessor candidateAssessor = mock(CandidateAssessor.class);

        AuditReport auditReport = buildAuditReport();
        RefinementPlan plan = buildPlan();
        CourseElementSnapshot elementBefore = buildElementBefore();
        CourseEntity course = mock(CourseEntity.class);

        when(complianceChecker.revalidate(any(QuizInstructionSubjectView.class))).thenReturn(stillViolatingResult());

        // R006/R012: the only remaining path to "correccion no lograda" is that no candidate ever
        // resulted entregable -- here, none complied with its consigna (R005), so none ever became
        // eligible (R013(c)) no matter how many other criteria it passed. The candidate is ALSO out
        // of its level's length range, so the assertion below that the length is never named is a
        // genuine check of R011(c)/R006(c), not one that passes merely because length never failed.
        CriterionVerdict complianceFailure = new CriterionVerdict(CorrectionCriterion.INSTRUCTION_COMPLIANCE,
                CriterionOutcome.FAILED,
                "La respuesta sigue sin estar en pasado simple: 'has walked' es preterito perfecto "
                        + "compuesto, no pasado simple.");
        List<CriterionVerdict> verdictsWithComplianceFailure = List.of(
                new CriterionVerdict(CorrectionCriterion.SCOPED_EDIT, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.LENGTH_IN_RANGE, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.LEVEL_VOCABULARY, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.DISTINCTNESS, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.FAITHFUL_TRANSLATION, CriterionOutcome.PASSED, null),
                new CriterionVerdict(CorrectionCriterion.SOLVABLE_SAME_KIND, CriterionOutcome.PASSED, null),
                complianceFailure);
        CandidateAssessment neverCompliantAssessment = new CandidateAssessment(
                verdictsWithComplianceFailure,
                false,
                true,
                5,
                List.of(complianceFailure),
                new CandidateLengthMeasurement(14, 6, 10, false, true),
                "not-eligible");
        when(candidateAssessor.assess(any(CandidateAssessmentInput.class))).thenReturn(neverCompliantAssessment);

        QuizInstructionCandidateGenerator neverCompliantGenerator = request ->
                new QuizInstructionGeneratorResponse(
                        "She ____ [has walked] (walk) to school every day.",
                        "Ella ha caminado a la escuela todos los dias.",
                        "Intento de correccion: la respuesta paso a preterito perfecto compuesto, "
                                + "todavia no a pasado simple.",
                        verdictsWithComplianceFailure,
                        3);

        when(planStore.load(PLAN_ID)).thenReturn(Optional.of(plan));
        when(auditReportStore.load(AUDIT_ID)).thenReturn(Optional.of(auditReport));
        when(artifactStore.hasPendingProposalForTask(PLAN_ID, TASK_ID)).thenReturn(false);
        when(courseRepository.load(COURSE_PATH)).thenReturn(course);
        when(elementLocator.snapshot(course, AuditTarget.QUIZ, QUIZ_ID)).thenReturn(Optional.of(elementBefore));

        RevisionEngineConfig config = buildConfig(
                ApprovalMode.AUTO, planStore, auditReportStore, artifactStore, courseRepository, elementLocator,
                complianceChecker, candidateAssessor, neverCompliantGenerator);
        RevisionEngine engine = new DefaultRevisionEngineFactory().create(config);

        // ── Act ───────────────────────────────────────────────────────────────────
        RevisionOutcome outcome = engine.revise(PLAN_ID, TASK_ID, COURSE_PATH, null);

        // ── Assert: failure — NO_ACCEPTABLE_CANDIDATE, naming the failed criterion (R006) ──
        assertEquals(RevisionOutcomeKind.NO_ACCEPTABLE_CANDIDATE, outcome.getKind(),
                "path-4: exhausting the attempts without a candidate that complied with its "
                        + "consigna must yield NO_ACCEPTABLE_CANDIDATE (R006)");
        assertNull(outcome.getArtifact(), "R006: no proposal must be left pending for this task");
        assertNotNull(outcome.getErrorMessage(), "R006: the failure must be reported explicitly, not silently");
        assertTrue(outcome.getErrorMessage().contains("INSTRUCTION_COMPLIANCE"),
                "R006: the failure must name at least the criterion that kept every candidate from "
                        + "ever being entregable");
        assertFalse(outcome.getErrorMessage().contains("LENGTH_IN_RANGE"),
                "R006/R011: the length must never be named among the criteria this desenlace "
                        + "reports, even though the same candidate also measured outside its "
                        + "level's target range");

        verify(artifactStore, never()).save(any(RevisionArtifact.class));
        verify(courseRepository, never()).save(any(CourseEntity.class), any(Path.class));
        verify(planStore, never()).save(any(RefinementPlan.class));
    }
}
