package com.learney.contentaudit.auditdomain.spanishregister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.AnalysisCost;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SpanishRegisterAnalyzerTest {
    @Test
    @DisplayName("should mark as medium each text of an exercise the student reads in vos, naming its words, as «Creá un adverbio… y agregalo» of «Adverbios de modo»")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R025")
    void shouldMarkAsMediumEachTextOfAnExerciseTheStudentReadsInVos() {
        // F-MESA-R025: the texts in vos the first correction left in «Adverbios de modo» and «Pedidos» of A1.
        AuditNode node = analyze(quiz("Creá un adverbio a partir del adjetivo y agregalo a la oración.",
                "Para formar el adverbio, añadí $-ly$ al adjetivo.", "¿Me prestás tu libro?"));
        List<FindingDraft> findings = found(node);
        assertEquals(List.of("consigna", "miniteoria", "traduccion"), findings.stream().map(FindingDraft::getMarker).toList());
        for (FindingDraft finding : findings) {
            assertEquals("tu-register", finding.getRule());
            assertEquals(FindingSeverity.MEDIUM, finding.getSeverity());
        }
        assertEquals("«Creá», «agregalo»", part(findings.get(0), "De vos"));
        assertEquals("«añadí»", part(findings.get(1), "De vos"));
        assertEquals("«prestás»", part(findings.get(2), "De vos"));
        assertTrue(findings.get(0).getEvidence().getObservation().startsWith("la consigna va de tú y dice «Creá»"),
                findings.get(0).getEvidence().getObservation());
        assertEquals(0.0, node.getScores().get("spanish-register"));
    }

    @Test
    @DisplayName("should send a text in vos to the rule when the table leaves it wholly in tú, and to the panel when a word stays")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R032")
    void shouldSendATextInVosToTheRuleWhenTheTableLeavesItInTuAndToThePanelOtherwise() {
        // F-MESA-R032: «usá» and «agregalo» are in the table; «zarpás» is not, and that card needs the panel.
        List<FindingDraft> findings = found(analyze(quiz("Creá un adverbio a partir del adjetivo y agregalo a la oración.",
                "Si zarpás temprano, usá $early$.", "¿Me prestás tu libro?")));
        assertEquals(List.of(FindingResolution.RULE, FindingResolution.PANEL, FindingResolution.RULE),
                findings.stream().map(FindingDraft::getResolution).toList());
    }

    @Test
    @DisplayName("should leave the tú of the course, the English between $…$ and the past or the future of a translation, scoring 1")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R025")
    void shouldLeaveTheTuOfTheCourseTheEnglishAndThePastOrTheFutureOfATranslation() {
        // F-MESA-R025: the instructions of the course in tú, and «Escribí» and «dibujará», which are not vos.
        AuditNode node = analyze(quiz("Crea un adverbio a partir del adjetivo y añádelo a la oración.",
                "Usa $is$ con $she$; después, el verbo irá al final.", "Escribí tres oraciones y mañana dibujará una casa."));
        assertEquals(List.of(), found(node));
        assertEquals(1.0, node.getScores().get("spanish-register"));
    }

    @Test
    @DisplayName("should describe spanish-register as an errors analyzer of exercises, instant, resolved by rule or at the panel through SPANISH_REGISTER tasks that may only touch the instructions, the mini theory and the translation")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R025")
    @Tag("F-MESA-R032")
    void shouldDescribeSpanishRegisterAndLetItsFixTouchOnlyWhatTheStudentReadsInSpanish() {
        // F-MESA-R025 and R032: a text in vos is written again in tú, by rule or at the panel, never the English.
        SpanishRegisterAnalyzerProvider provider = new SpanishRegisterAnalyzerProvider();
        var card = provider.describe();
        assertEquals("spanish-register", provider.analyzerName());
        assertEquals("¿Lo que lee el alumno en español va de tú?", card.getQuestion());
        assertEquals(AnalyzerFamily.ERRORS, card.getFamily());
        assertEquals(List.of(AuditTarget.QUIZ), card.getEvaluatedTargets());
        assertEquals(List.of(FindingResolution.RULE, FindingResolution.PANEL), card.getResolutions());
        assertEquals(AnalysisCost.INSTANT, card.getCost());
        assertEquals(Optional.of(new AnalyzerPlanBinding("SPANISH_REGISTER", List.of(AuditTarget.QUIZ))),
                provider.planBinding());
        SpanishRegisterCorrection correction = new SpanishRegisterCorrection();
        assertEquals("spanish-register", correction.analyzerName());
        assertEquals(List.of(ExercisePart.INSTRUCTIONS, ExercisePart.MINI_THEORY, ExercisePart.TRANSLATION),
                correction.editScope(FindingResolution.PANEL));
        assertEquals(correction.editScope(FindingResolution.PANEL), correction.editScope(FindingResolution.RULE));
        assertTrue(correction.editScope(FindingResolution.RANK_ONLY).isEmpty());
        assertFalse(correction.contextOnly());
    }

    private final SpanishRegisterAnalyzer analyzer = new SpanishRegisterAnalyzer();

    private AuditNode analyze(AuditableQuiz quiz) {
        AuditNode node = new AuditNode(quiz, AuditTarget.QUIZ, null, new ArrayList<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new DefaultQuizDiagnoses());
        analyzer.onQuiz(node);
        return node;
    }

    private List<FindingDraft> found(AuditNode node) {
        return analyzer.findingsAt(node);
    }

    private static AuditableQuiz quiz(String instructions, String miniTheory, String translation) {
        return new AuditableQuiz(List.of(), "67fab6d59930102295342548", "Adverbios de modo", "", translation,
                List.of("She smiled sweetly."), null, instructions,
                List.of(new SentencePartEntity(SentencePartKind.TEXT, "She smiled (sweet).", null),
                        new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("She smiled sweetly."))),
                null, miniTheory, "A1.06.01.01");
    }

    private static String part(FindingDraft draft, String label) {
        return draft.getEvidence().getExamined().stream().filter(p -> p.getLabel().equals(label))
                .map(EvidencePart::getText).findFirst().orElse(null);
    }
}
