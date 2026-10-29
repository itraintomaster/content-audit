package com.learney.contentaudit.auditdomain.spanishregister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.finding.EvidencePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingEvidence;
import com.learney.contentaudit.auditdomain.finding.FindingIdentity;
import com.learney.contentaudit.auditdomain.finding.FindingNodeRef;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.auditdomain.rulefix.RuleFixRequest;
import com.learney.contentaudit.auditdomain.rulefix.RuleFixResult;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SpanishRegisterRuleFixTest {
    private static final String QUIZ = "67fab6d599301022953424d1";

    @Test
    @DisplayName("should write in tú, by the table and without the panel, each text its finding names, leaving the English and the other texts as they were")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R032")
    void shouldWriteInTuByTheTableEachTextItsFindingNames() {
        // F-MESA-R032: «Verbos y sus preposiciones» of the C2 of A1 (28/10/2026), with its card and its instructions in vos.
        QuizTemplateEntity quiz = quiz("Elegí la preposición correcta.",
                "Usá $wait for$ para decir a quién esperás: $wait for us$ significa «espéranos».", "Esperamos el autobús.");
        Finding card = vos("miniteoria", FindingResolution.RULE);
        Finding instructions = vos("consigna", FindingResolution.RULE);

        RuleFixResult result = new SpanishRegisterRuleFix().fix(new RuleFixRequest(quiz, null, null, List.of(card, instructions)));

        assertEquals(List.of(card.getIdentity(), instructions.getIdentity()), result.getFixed());
        assertEquals(List.of(), result.getDeclined());
        assertEquals("Elige la preposición correcta.", result.getQuiz().getInstructions());
        assertEquals("Usa $wait for$ para decir a quién esperas: $wait for us$ significa «espéranos».",
                result.getQuiz().getMiniTheory());
        assertEquals("Esperamos el autobús.", result.getQuiz().getTranslation(), "a text without a finding stays");
        assertEquals("Elegí la preposición correcta.", quiz.getInstructions(), "the exercise it was given is not written");
        assertTrue(result.getDetail().contains("la miniteoría"), result.getDetail());
        assertTrue(result.getRerecordings().isEmpty(), "nothing the student hears changes");
    }

    @Test
    @DisplayName("should decline a text the table does not leave wholly in tú, and take no finding for the panel")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R032")
    void shouldDeclineATextTheTableDoesNotLeaveInTuAndTakeNoFindingForThePanel() {
        // F-MESA-R032: «zarpás» is not in the table; that card goes to the panel, and a PANEL finding is not the rule's.
        QuizTemplateEntity quiz = quiz("Elegí la opción.", "Si zarpás temprano, usá $early$.", null);
        Finding card = vos("miniteoria", FindingResolution.RULE);
        Finding instructions = vos("consigna", FindingResolution.PANEL);

        RuleFixResult result = new SpanishRegisterRuleFix().fix(new RuleFixRequest(quiz, null, null, List.of(card, instructions)));

        assertEquals(List.of(), result.getFixed());
        assertEquals(List.of(card.getIdentity()), result.getDeclined());
        assertSame(quiz, result.getQuiz());
        assertTrue(result.getDetail().contains("la tabla no la deja de tú: la miniteoría"), result.getDetail());
    }

    private static QuizTemplateEntity quiz(String instructions, String miniTheory, String translation) {
        QuizTemplateEntity quiz = new QuizTemplateEntity();
        quiz.setId(QUIZ);
        quiz.setInstructions(instructions);
        quiz.setMiniTheory(miniTheory);
        quiz.setTranslation(translation);
        return quiz;
    }

    private static Finding vos(String marker, FindingResolution resolution) {
        return new Finding("spanish-register", "tu-register", new FindingNodeRef(AuditTarget.QUIZ, QUIZ, QUIZ),
                FindingSeverity.MEDIUM, new FindingEvidence(List.of(new EvidencePart("De vos", "«usá»")), "de vos", List.of()),
                resolution, null, new FindingIdentity("spanish-register|tu-register|" + QUIZ + "|" + marker, marker));
    }
}
