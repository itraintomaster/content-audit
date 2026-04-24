package com.learney.contentaudit.journeys;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.SentenceLengthDiagnosis;
import com.learney.contentaudit.auditdomain.labs.AbsenceAssessment;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("F-RCSL")
@Tag("F-RCSL-J001")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FRcslJ001JourneyTest {

    // -----------------------------------------------------------------------
    // Shared tree builder helpers
    // -----------------------------------------------------------------------

    /**
     * Builds the COURSE→MILESTONE→TOPIC→KNOWLEDGE→QUIZ tree with parent refs.
     * The quiz node carries a SentenceLengthDiagnosis for a TOO-LONG sentence:
     *   tokenCount=15, targetMin=5, targetMax=8, delta=+7.
     * The milestone node's LemmaAbsenceLevelDiagnosis is provided by the caller
     * so individual tests can vary it.
     */
    private AuditNode buildTree(DefaultLevelDiagnoses milestoneDiagnoses) {
        // Quiz entity — the sentence that is too long
        AuditableQuiz quizEntity = new AuditableQuiz(Collections.emptyList(), "quiz-001", "Quiz 1", "Q001", "El rapido zorro marron salta sobre el perro perezoso hoy aqui", List.of("The quick brown fox jumps over the lazy dog today here"), null);

        // SentenceLengthDiagnosis: tokenCount=15, range=[5,8], delta=+7 (15-8=7)
        SentenceLengthDiagnosis slDiagnosis = new SentenceLengthDiagnosis(
                15, 5, 8, CefrLevel.A1, 7, 4);

        DefaultQuizDiagnoses quizDiagnoses = new DefaultQuizDiagnoses();
        quizDiagnoses.setSentenceLengthDiagnosis(slDiagnosis);

        AuditNode quizNode = new AuditNode(
                quizEntity, AuditTarget.QUIZ, null, Collections.emptyList(),
                Map.of(), Map.of(), quizDiagnoses);

        // Knowledge entity
        AuditableKnowledge knowledgeEntity = new AuditableKnowledge(
                List.of(quizEntity),
                "Present Tense Basics",
                "Fill in the blank with the correct verb form.",
                true,
                "knowledge-001",
                "Knowledge 1",
                "K001");

        DefaultKnowledgeDiagnoses knowledgeDiagnoses = new DefaultKnowledgeDiagnoses();

        AuditNode knowledgeNode = new AuditNode(
                knowledgeEntity, AuditTarget.KNOWLEDGE, null, List.of(quizNode),
                Map.of(), Map.of(), knowledgeDiagnoses);
        quizNode.setParent(knowledgeNode);

        // Topic entity
        AuditableTopic topicEntity = new AuditableTopic(
                List.of(knowledgeEntity),
                "topic-001",
                "Daily Actions",
                "T001");

        DefaultTopicDiagnoses topicDiagnoses = new DefaultTopicDiagnoses();

        AuditNode topicNode = new AuditNode(
                topicEntity, AuditTarget.TOPIC, null, List.of(knowledgeNode),
                Map.of(), Map.of(), topicDiagnoses);
        knowledgeNode.setParent(topicNode);

        // Milestone entity (numeric id required by SentenceLengthAnalyzer exclusion logic,
        // but resolver only needs the id to be present for ancestor navigation)
        AuditableMilestone milestoneEntity = new AuditableMilestone(
                List.of(topicEntity),
                "1",
                "Milestone 1",
                "M001");

        AuditNode milestoneNode = new AuditNode(
                milestoneEntity, AuditTarget.MILESTONE, null, List.of(topicNode),
                Map.of(), Map.of(), milestoneDiagnoses);
        topicNode.setParent(milestoneNode);

        // Course root node (COURSE target wrapping the milestone)
        AuditNode courseNode = new AuditNode(
                null, AuditTarget.COURSE, null, List.of(milestoneNode),
                Map.of(), Map.of(), null);
        milestoneNode.setParent(courseNode);

        return courseNode;
    }

    /**
     * Builds a LemmaAbsenceLevelDiagnosis with two qualifying absent lemmas:
     *   - "run" (VERB, COMPLETELY_ABSENT, cocaRank=500)
     *   - "jump" (VERB, APPEARS_TOO_LATE, cocaRank=1200)
     * and one excluded lemma:
     *   - "whenever" (ADV, APPEARS_TOO_EARLY, cocaRank=2000)
     */
    private LemmaAbsenceLevelDiagnosis buildLemmaAbsenceDiagnosis() {
        AbsentLemma completelyAbsent = new AbsentLemma(
                new LemmaAndPos("run", "VERB"),
                CefrLevel.A1,
                AbsenceType.COMPLETELY_ABSENT,
                Collections.emptyList(),
                PriorityLevel.HIGH,
                500,
                "motion");

        AbsentLemma tooLate = new AbsentLemma(
                new LemmaAndPos("jump", "VERB"),
                CefrLevel.A1,
                AbsenceType.APPEARS_TOO_LATE,
                List.of(CefrLevel.A2),
                PriorityLevel.HIGH,
                1200,
                "motion");

        AbsentLemma tooEarly = new AbsentLemma(
                new LemmaAndPos("whenever", "ADV"),
                CefrLevel.B1,
                AbsenceType.APPEARS_TOO_EARLY,
                List.of(CefrLevel.A2),
                PriorityLevel.LOW,
                2000,
                "time");

        return new LemmaAbsenceLevelDiagnosis(
                CefrLevel.A1,
                100, 3, 3.0, 0.95,
                0.9, 0.8, 0.7,
                AbsenceAssessment.NEEDS_IMPROVEMENT,
                List.of(completelyAbsent, tooLate, tooEarly),
                Collections.emptyList(),
                2, 0, 1);
    }

    @Test
    @Order(1)
    @Tag("path-1")
    @DisplayName("path-1: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El sistema extrae la oracion del quiz... [El reporte de auditoria existe y el nodo quiz se localiza correctamente] → El sistema navega al milestone ancest... → El sistema construye el contexto con ... [Hay lemas ausentes de tipo COMPLETELY_ABSENT o APPEARS_TOO_LATE disponibles] → El comando muestra la tarea junto con... → success")
    public void path1_elReporteDeAuditoriaExisteYElNodoQuizSeLocalizaCorrectamente_hayLemasAusentesDeTipoCOMPLETELYABSENTOAPPEARSTOOLATEDisponibles_success(
            ) {
        // Step: solicitar_tarea — task identifies quiz-001 as a SENTENCE_LENGTH problem
        RefinementTask task = new RefinementTask(
                "task-001", AuditTarget.QUIZ, "quiz-001", "Quiz 1",
                DiagnosisKind.SENTENCE_LENGTH, 1, RefinementTaskStatus.PENDING);

        // Step: verificar_auditoria — build audit tree with milestone lemma diagnosis
        DefaultLevelDiagnoses milestoneDiagnoses = new DefaultLevelDiagnoses();
        milestoneDiagnoses.setLemmaAbsenceDiagnosis(buildLemmaAbsenceDiagnosis());
        AuditNode courseRoot = buildTree(milestoneDiagnoses);
        AuditReport report = new AuditReport(courseRoot);

        // Step: obtener_diagnostico + buscar_lemas + construir_contexto_completo
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        var rawResult = resolver.resolve(report, task);

        // Step: presentar_contexto — assert success with populated context and ordered lemmas
        assertTrue(rawResult.isPresent(), "Context should be present");
        SentenceLengthCorrectionContext context = (SentenceLengthCorrectionContext) rawResult.get();

        assertEquals("task-001", context.getTaskId());
        assertEquals(7, context.getDelta(), "Delta must be +7 for a too-long sentence");
        assertEquals(15, context.getTokenCount());
        assertEquals(5, context.getTargetMin());
        assertEquals(8, context.getTargetMax());
        assertEquals(CefrLevel.A1, context.getCefrLevel());

        // Suggested lemmas: only COMPLETELY_ABSENT and APPEARS_TOO_LATE qualify (2 lemmas)
        assertFalse(context.getSuggestedLemmas().isEmpty(), "Suggested lemmas must not be empty");
        assertEquals(2, context.getSuggestedLemmas().size(), "Only 2 qualifying lemmas");

        // Ordered by COCA rank ascending: run(500) before jump(1200)
        assertEquals("run", context.getSuggestedLemmas().get(0).getLemma());
        assertEquals("jump", context.getSuggestedLemmas().get(1).getLemma());
        assertTrue(
                context.getSuggestedLemmas().get(0).getCocaRank()
                        <= context.getSuggestedLemmas().get(1).getCocaRank(),
                "Suggested lemmas must be ordered by COCA rank ascending");
    }

    @Test
    @Order(2)
    @Tag("path-2")
    @DisplayName("path-2: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El sistema extrae la oracion del quiz... [El reporte de auditoria existe y el nodo quiz se localiza correctamente] → El sistema navega al milestone ancest... → El sistema construye el contexto con ... [No hay lemas sugeridos disponibles] → El comando muestra la tarea junto con... → success")
    public void path2_elReporteDeAuditoriaExisteYElNodoQuizSeLocalizaCorrectamente_noHayLemasSugeridosDisponibles_success(
            ) {
        // Step: solicitar_tarea — task identifies quiz-001 as a SENTENCE_LENGTH problem
        RefinementTask task = new RefinementTask(
                "task-002", AuditTarget.QUIZ, "quiz-001", "Quiz 1",
                DiagnosisKind.SENTENCE_LENGTH, 1, RefinementTaskStatus.PENDING);

        // Step: verificar_auditoria — build audit tree with NO lemma absence diagnosis on milestone
        DefaultLevelDiagnoses milestoneDiagnoses = new DefaultLevelDiagnoses();
        // no setLemmaAbsenceDiagnosis call — leaves it absent
        AuditNode courseRoot = buildTree(milestoneDiagnoses);
        AuditReport report = new AuditReport(courseRoot);

        // Step: obtener_diagnostico + buscar_lemas + construir_contexto_sin_lemas
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        var rawResult = resolver.resolve(report, task);

        // Step: presentar_contexto — assert success with context but empty suggested lemmas
        assertTrue(rawResult.isPresent(), "Context should be present even without lemma diagnosis");
        SentenceLengthCorrectionContext context = (SentenceLengthCorrectionContext) rawResult.get();

        assertEquals("task-002", context.getTaskId());
        assertEquals(7, context.getDelta(), "Delta must be +7 for a too-long sentence");
        assertEquals(15, context.getTokenCount());
        assertEquals(5, context.getTargetMin());
        assertEquals(8, context.getTargetMax());
        assertEquals(CefrLevel.A1, context.getCefrLevel());
        assertTrue(context.getSuggestedLemmas().isEmpty(),
                "Suggested lemmas must be empty when milestone has no LemmaAbsenceLevelDiagnosis");
    }

    @Test
    @Order(3)
    @Tag("path-3")
    @DisplayName("path-3: El usuario ejecuta 'refiner next' y o... → El sistema busca el reporte de audito... → El comando muestra la tarea basica co... [El reporte no se encuentra o el nodo quiz no existe en el arbol] → failure")
    public void path3_elReporteNoSeEncuentraOElNodoQuizNoExisteEnElArbol_failure() {
        // Step: solicitar_tarea — task references a quiz that does not exist in the tree
        RefinementTask task = new RefinementTask(
                "task-003", AuditTarget.QUIZ, "non-existent-quiz-id", "Unknown Quiz",
                DiagnosisKind.SENTENCE_LENGTH, 1, RefinementTaskStatus.PENDING);

        // Step: verificar_auditoria — tree exists but the quiz node is not in it
        DefaultLevelDiagnoses milestoneDiagnoses = new DefaultLevelDiagnoses();
        AuditNode courseRoot = buildTree(milestoneDiagnoses);
        AuditReport report = new AuditReport(courseRoot);

        // Step: contexto_no_disponible — resolver cannot locate the node, returns empty
        SentenceLengthContextResolver resolver = new SentenceLengthContextResolver();
        var result = resolver.resolve(report, task);

        // Assert failure path: Optional.empty()
        assertFalse(result.isPresent(),
                "Context must be empty when the quiz node does not exist in the audit tree");
    }
}
