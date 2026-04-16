package com.learney.contentaudit.journeys;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.labs.AbsenceType;
import com.learney.contentaudit.auditdomain.labs.AbsentLemma;
import com.learney.contentaudit.auditdomain.labs.AbsenceAssessment;
import com.learney.contentaudit.auditdomain.labs.LemmaAbsenceLevelDiagnosis;
import com.learney.contentaudit.auditdomain.labs.LemmaAndPos;
import com.learney.contentaudit.auditdomain.labs.PriorityLevel;
import com.learney.contentaudit.refinerdomain.SentenceLengthContextResolver;
import com.learney.contentaudit.refinerdomain.DiagnosisKind;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.RefinementTaskStatus;
import com.learney.contentaudit.refinerdomain.SentenceLengthCorrectionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Tag("F-RCSL")
@Tag("F-RCSL-J003")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FRcslJ003JourneyTest {

    // ------------------------------------------------------------------
    // Tree builder helpers
    // ------------------------------------------------------------------

    private AuditNode buildCourseNode() {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.COURSE);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        return node;
    }

    private AuditNode buildMilestoneNode(AuditNode parent, AuditableMilestone milestone,
            DefaultLevelDiagnoses diagnoses) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.MILESTONE);
        node.setEntity(milestone);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(diagnoses);
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

    private AuditNode buildQuizNode(AuditNode parent, AuditableQuiz quiz,
            DefaultQuizDiagnoses diagnoses) {
        AuditNode node = new AuditNode();
        node.setTarget(AuditTarget.QUIZ);
        node.setEntity(quiz);
        node.setParent(parent);
        node.setChildren(new ArrayList<>());
        node.setScores(new LinkedHashMap<>());
        node.setMetadata(new LinkedHashMap<>());
        node.setDiagnoses(diagnoses);
        parent.getChildren().add(node);
        return node;
    }

    private DefaultLevelDiagnoses buildMilestoneDiagnoses(List<AbsentLemma> absentLemmas) {
        LemmaAbsenceLevelDiagnosis absenceDiagnosis = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 50, absentLemmas.size(), 5.0, 95.0,
                0.9, 0.8, 0.7, AbsenceAssessment.OPTIMAL, absentLemmas, List.of(), 0, 0, 0);
        DefaultLevelDiagnoses diagnoses = new DefaultLevelDiagnoses();
        diagnoses.setLemmaAbsenceDiagnosis(absenceDiagnosis);
        return diagnoses;
    }

    private DefaultQuizDiagnoses buildQuizDiagnoses() {
        SentenceLengthDiagnosis sld = new SentenceLengthDiagnosis(8, 5, 10, CefrLevel.A1, 0, 4);
        DefaultQuizDiagnoses quizDiagnoses = new DefaultQuizDiagnoses();
        quizDiagnoses.setSentenceLengthDiagnosis(sld);
        return quizDiagnoses;
    }

    private RefinementTask buildTask(String quizId) {
        return new RefinementTask(
                "task-j003",
                AuditTarget.QUIZ,
                quizId,
                "Quiz label",
                DiagnosisKind.SENTENCE_LENGTH,
                1,
                RefinementTaskStatus.PENDING);
    }

    // ------------------------------------------------------------------
    // Path tests
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El usuario ejecuta 'refiner next' y o... → El sistema localiza el nodo quiz en e... → El sistema navega al milestone ancest... → El sistema construye el contexto con ... [El milestone existe pero su LemmaAbsenceLevelDiagnosis indica que todos los lemas esperados estan presentes (no hay ausencias)] → El comando muestra la tarea con el co... → success")
    public void path1_elMilestoneExistePeroSuLemmaAbsenceLevelDiagnosisIndicaQueTodosLosLemasEsperadosEstanPresentesNoHayAusencias_success(
            ) {
        // Step: solicitar_tarea — User requests a SENTENCE_LENGTH task
        AuditableQuiz quiz = new AuditableQuiz(
                "The dog runs fast.", List.of(), "quiz-j003-p1", null, null, "El perro corre rapido.");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Animals in Motion", "Complete the sentence.", true, "know-j003-p1", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j003-p1", "Animals", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-j003-p1", "A1", null);

        // Step: navegar_milestone — milestone has LemmaAbsenceLevelDiagnosis with empty absentLemmas
        // (all expected lemmas are present, no absences)
        DefaultLevelDiagnoses milestoneDiagnoses = buildMilestoneDiagnoses(List.of());

        // Step: localizar_quiz (gate: F-RCSL-R002) — build full tree and locate quiz node
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiagnoses);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnoses());
        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-j003-p1");

        // Step: sin_lemas_disponibles (gate: F-RCSL-R004) — resolver builds context with empty suggestedLemmas
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        // Step: presentar_contexto (gate: F-RCSL-R006, F-RCSL-R008) — success: context present, suggestedLemmas empty
        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals("The dog runs fast.", ctx.getSentence());
        Assertions.assertEquals(CefrLevel.A1, ctx.getCefrLevel());
        Assertions.assertEquals(8, ctx.getTokenCount());
        Assertions.assertNotNull(ctx.getSuggestedLemmas());
        Assertions.assertTrue(ctx.getSuggestedLemmas().isEmpty());
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El usuario ejecuta 'refiner next' y o... → El sistema localiza el nodo quiz en e... → El sistema navega al milestone ancest... → El sistema construye el contexto con ... [El milestone existe pero todas las ausencias son de tipo APPEARS_TOO_EARLY (excluidas por R003)] → El comando muestra la tarea con el co... → success")
    public void path2_elMilestoneExistePeroTodasLasAusenciasSonDeTipoAPPEARSTOOEARLYExcluidasPorR003_success(
            ) {
        // Step: solicitar_tarea — User requests a SENTENCE_LENGTH task
        AuditableQuiz quiz = new AuditableQuiz(
                "She reads a book.", List.of(), "quiz-j003-p2", null, null, "Ella lee un libro.");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Daily Activities", "Complete the sentence.", true, "know-j003-p2", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j003-p2", "Routines", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-j003-p2", "A1", null);

        // Step: navegar_milestone — milestone has LemmaAbsenceLevelDiagnosis where all absences
        // are APPEARS_TOO_EARLY (excluded by F-RCSL-R003 filter)
        AbsentLemma earlyLemma1 = new AbsentLemma(
                new LemmaAndPos("challenge", "NOUN"),
                CefrLevel.B2,
                AbsenceType.APPEARS_TOO_EARLY,
                List.of(CefrLevel.A1),
                PriorityLevel.LOW,
                3500,
                null);
        AbsentLemma earlyLemma2 = new AbsentLemma(
                new LemmaAndPos("determine", "VERB"),
                CefrLevel.B1,
                AbsenceType.APPEARS_TOO_EARLY,
                List.of(CefrLevel.A2),
                PriorityLevel.MEDIUM,
                2800,
                null);
        DefaultLevelDiagnoses milestoneDiagnoses = buildMilestoneDiagnoses(
                List.of(earlyLemma1, earlyLemma2));

        // Step: localizar_quiz (gate: F-RCSL-R002) — build full tree and locate quiz node
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiagnoses);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnoses());
        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-j003-p2");

        // Step: sin_lemas_disponibles (gate: F-RCSL-R004) — resolver filters out all APPEARS_TOO_EARLY lemmas
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        // Step: presentar_contexto (gate: F-RCSL-R006, F-RCSL-R008) — success: context present, suggestedLemmas empty
        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals("She reads a book.", ctx.getSentence());
        Assertions.assertEquals(CefrLevel.A1, ctx.getCefrLevel());
        Assertions.assertEquals(8, ctx.getTokenCount());
        Assertions.assertNotNull(ctx.getSuggestedLemmas());
        Assertions.assertTrue(ctx.getSuggestedLemmas().isEmpty());
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El usuario ejecuta 'refiner next' y o... → El sistema localiza el nodo quiz en e... → El sistema navega al milestone ancest... → El sistema construye el contexto con ... [No se encuentra el milestone ancestro (estructura incompleta)] → El comando muestra la tarea con el co... → success")
    public void path3_noSeEncuentraElMilestoneAncestroEstructuraIncompleta_success() {
        // Step: solicitar_tarea — User requests a SENTENCE_LENGTH task
        AuditableQuiz quiz = new AuditableQuiz(
                "He plays football.", List.of(), "quiz-j003-p3", null, null, "El juega al futbol.");
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Sports", "Complete the sentence.", true, "know-j003-p3", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j003-p3", "Leisure", null);

        // Step: localizar_quiz (gate: F-RCSL-R002) — build tree WITHOUT milestone: COURSE→TOPIC→KNOWLEDGE→QUIZ
        AuditNode courseNode = buildCourseNode();
        AuditNode topicNode = buildTopicNode(courseNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        buildQuizNode(knowledgeNode, quiz, buildQuizDiagnoses());
        AuditReport report = new AuditReport(courseNode);
        RefinementTask task = buildTask("quiz-j003-p3");

        // Step: navegar_milestone — ancestor(MILESTONE) returns empty (no milestone in tree)
        // Step: sin_lemas_disponibles (gate: F-RCSL-R004) — resolver builds context with empty suggestedLemmas
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        // Step: presentar_contexto (gate: F-RCSL-R006, F-RCSL-R008) — success: context present with sentence
        // and diagnosis populated, suggestedLemmas empty
        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals("He plays football.", ctx.getSentence());
        Assertions.assertEquals(CefrLevel.A1, ctx.getCefrLevel());
        Assertions.assertEquals(8, ctx.getTokenCount());
        Assertions.assertNotNull(ctx.getSuggestedLemmas());
        Assertions.assertTrue(ctx.getSuggestedLemmas().isEmpty());
    }
}
