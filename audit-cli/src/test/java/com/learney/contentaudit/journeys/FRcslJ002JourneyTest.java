package com.learney.contentaudit.journeys;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
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
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.labs.AbsenceType;
import com.learney.contentaudit.auditdomain.labs.AbsentLemma;
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
@Tag("F-RCSL-J002")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FRcslJ002JourneyTest {

    // ------------------------------------------------------------------
    // Tree builder helpers (mirrors DefaultCorrectionContextResolverTest)
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

    /**
     * Builds a full COURSE → MILESTONE → TOPIC → KNOWLEDGE → QUIZ tree.
     * Returns [courseNode, milestoneNode, topicNode, knowledgeNode, quizNode].
     */
    private AuditNode[] buildFullTree(AuditableMilestone milestone,
            DefaultLevelDiagnoses milestoneDiagnoses,
            AuditableTopic topic,
            AuditableKnowledge knowledge,
            AuditableQuiz quiz,
            DefaultQuizDiagnoses quizDiagnoses) {
        AuditNode courseNode = buildCourseNode();
        AuditNode milestoneNode = buildMilestoneNode(courseNode, milestone, milestoneDiagnoses);
        AuditNode topicNode = buildTopicNode(milestoneNode, topic);
        AuditNode knowledgeNode = buildKnowledgeNode(topicNode, knowledge);
        AuditNode quizNode = buildQuizNode(knowledgeNode, quiz, quizDiagnoses);
        return new AuditNode[]{courseNode, milestoneNode, topicNode, knowledgeNode, quizNode};
    }

    private RefinementTask buildSentenceLengthTask(String quizId) {
        return new RefinementTask(
                "task-j002",
                AuditTarget.QUIZ,
                quizId,
                "Short sentence quiz",
                DiagnosisKind.SENTENCE_LENGTH,
                1,
                RefinementTaskStatus.PENDING);
    }

    private DefaultLevelDiagnoses buildMilestoneDiagnosesWithLemmas(List<AbsentLemma> absentLemmas) {
        LemmaAbsenceLevelDiagnosis absenceDiagnosis = new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1, 100, absentLemmas.size(), 10.0, 90.0,
                0.8, 0.7, 0.5, null, absentLemmas, List.of(), 1, 0, 0);
        DefaultLevelDiagnoses diagnoses = new DefaultLevelDiagnoses();
        diagnoses.setLemmaAbsenceDiagnosis(absenceDiagnosis);
        return diagnoses;
    }

    private DefaultLevelDiagnoses buildMilestoneDiagnosesWithoutLemmaAbsence() {
        return new DefaultLevelDiagnoses();
    }

    // ------------------------------------------------------------------
    // Paths
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El sistema extrae la oracion y el dia... [El reporte y el nodo quiz se localizan correctamente] → El sistema navega al milestone ancest... → El sistema construye el contexto: ora... [Hay lemas sugeridos disponibles] → El comando muestra la tarea con el co... → success")
    public void path1_elReporteYElNodoQuizSeLocalizanCorrectamente_hayLemasSugeridosDisponibles_success(
            ) {
        // Step: solicitar_tarea — quiz with tokenCount=3, targetMin=5, targetMax=8 (too short)
        // tokenCount(3) - targetMin(5) = delta(-2): sentence needs 2 more tokens
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-j002-1", null, null, "Yo corro.", List.of("I run."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Daily Actions", "Complete the sentence.", true, "know-j002-1", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j002-1", "Everyday Verbs", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-j002-1", "A1", null);

        // Step: obtener_diagnostico — diagnosis with negative delta (-2)
        SentenceLengthDiagnosis sld = new SentenceLengthDiagnosis(3, 5, 8, CefrLevel.A1, -2, 4);
        DefaultQuizDiagnoses quizDiag = new DefaultQuizDiagnoses();
        quizDiag.setSentenceLengthDiagnosis(sld);

        // Step: buscar_lemas — milestone has a COMPLETELY_ABSENT lemma
        AbsentLemma absentLemma = new AbsentLemma(
                new LemmaAndPos("walk", "VERB"),
                CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT,
                List.of(),
                PriorityLevel.HIGH,
                800,
                null);
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnosesWithLemmas(List.of(absentLemma));

        // Step: verificar_auditoria — build tree and report
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        RefinementTask task = buildSentenceLengthTask("quiz-j002-1");

        // Step: construir_contexto_con_lemas — resolver builds context
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        // Step: presentar_contexto — result: success
        // Gate F-RCSL-R001: context present with negative delta
        // Gate F-RCSL-R003: suggestedLemmas not empty
        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals(-2, ctx.getDelta());
        Assertions.assertEquals(3, ctx.getTokenCount());
        Assertions.assertEquals(5, ctx.getTargetMin());
        Assertions.assertEquals(8, ctx.getTargetMax());
        Assertions.assertFalse(ctx.getSuggestedLemmas().isEmpty());
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El sistema extrae la oracion y el dia... [El reporte y el nodo quiz se localizan correctamente] → El sistema navega al milestone ancest... → El sistema construye el contexto sin ... [No hay lemas sugeridos] → El comando muestra la tarea con el co... → success")
    public void path2_elReporteYElNodoQuizSeLocalizanCorrectamente_noHayLemasSugeridos_success() {
        // Step: solicitar_tarea — quiz with tokenCount=3, targetMin=5, targetMax=8 (too short)
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-j002-2", null, null, "Ella canta.", List.of("She sings."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Music Basics", "Fill in the blank.", true, "know-j002-2", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j002-2", "Arts", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-j002-2", "A1", null);

        // Step: obtener_diagnostico — diagnosis with negative delta (-2)
        SentenceLengthDiagnosis sld = new SentenceLengthDiagnosis(3, 5, 8, CefrLevel.A1, -2, 4);
        DefaultQuizDiagnoses quizDiag = new DefaultQuizDiagnoses();
        quizDiag.setSentenceLengthDiagnosis(sld);

        // Step: buscar_lemas — milestone has no LemmaAbsenceLevelDiagnosis
        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnosesWithoutLemmaAbsence();

        // Step: verificar_auditoria — build tree and report
        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        RefinementTask task = buildSentenceLengthTask("quiz-j002-2");

        // Step: construir_contexto_sin_lemas — resolver builds context without suggestions
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        // Step: presentar_contexto — result: success
        // Gate F-RCSL-R001: context present with negative delta
        // Gate F-RCSL-R004: suggestedLemmas empty
        Assertions.assertTrue(result.isPresent());
        SentenceLengthCorrectionContext ctx = result.get();
        Assertions.assertEquals(-2, ctx.getDelta());
        Assertions.assertTrue(ctx.getSuggestedLemmas().isEmpty());
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El comando muestra la tarea sin conte... [No se puede localizar el reporte o el nodo] → failure")
    public void path3_noSePuedeLocalizarElReporteOElNodo_failure() {
        // Step: solicitar_tarea — task references a quiz that does not exist in the tree
        AuditableQuiz quiz = new AuditableQuiz(List.of(), "quiz-j002-3", null, null, "El camina.", List.of("He walks."), null);
        AuditableKnowledge knowledge = new AuditableKnowledge(
                List.of(), "Basic Verbs", "Complete.", true, "know-j002-3", null, null);
        AuditableTopic topic = new AuditableTopic(List.of(), "topic-j002-3", "Actions", null);
        AuditableMilestone milestone = new AuditableMilestone(List.of(), "ms-j002-3", "A1", null);

        SentenceLengthDiagnosis sld = new SentenceLengthDiagnosis(3, 5, 8, CefrLevel.A1, -2, 4);
        DefaultQuizDiagnoses quizDiag = new DefaultQuizDiagnoses();
        quizDiag.setSentenceLengthDiagnosis(sld);

        DefaultLevelDiagnoses milestoneDiag = buildMilestoneDiagnosesWithoutLemmaAbsence();

        AuditNode[] tree = buildFullTree(milestone, milestoneDiag, topic, knowledge, quiz, quizDiag);
        AuditReport report = new AuditReport(tree[0]);

        // Step: verificar_auditoria — task nodeId does not match any node in the tree
        RefinementTask task = buildSentenceLengthTask("quiz-does-not-exist");

        // Step: contexto_no_disponible — result: failure (empty Optional)
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        Optional<SentenceLengthCorrectionContext> result = resolver.resolve(report, task);

        Assertions.assertTrue(result.isEmpty());
    }
}
