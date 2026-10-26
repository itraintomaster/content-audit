package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.COME;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.STATE_VERBS;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.VEGETABLES;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.afirmativas;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.brief;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.candidate;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.cloze;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.contraste;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.exercise;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.finding;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.stateVerbExercises;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.text;
import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.withParts;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class TopicStateTest {

    private static final String NEW_MINI_THEORY = "Para preguntar, pon el auxiliar antes del sujeto.";

    @Test
    @DisplayName("should apply a mini-theory candidate only to its explicit quiz id even when a sibling has the same old text")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R012")
    void shouldApplyAMinitheoryCandidateOnlyToItsExplicitQuizIdEvenWhenASiblingHasTheSameOldText() {
        KnowledgeEntity original = contraste();
        String q1 = stateVerbExercises().get(0);
        String q2 = stateVerbExercises().get(1);
        Finding finding = finding("mini-theory", "explains-this-quiz", q1, FindingSeverity.HIGH,
                FindingResolution.PANEL, STATE_VERBS, "la miniteoría no explica el caso de este quiz");
        ErrorTaskBrief brief = brief("t-mini-q1", finding);
        PanelCandidate candidate = new PanelCandidate(PanelCandidateKind.MINI_THEORY, null, NEW_MINI_THEORY, null,
                "corrige sólo el caso de q1");

        KnowledgeEntity applied = TopicState.apply(original, q1, candidate, List.of(brief));

        assertEquals(NEW_MINI_THEORY, exercise(applied, q1).getMiniTheory());
        assertEquals(STATE_VERBS, exercise(applied, q2).getMiniTheory(), "q2 keeps its independent mini-theory");
        assertEquals(STATE_VERBS, exercise(original, q1).getMiniTheory(), "apply keeps the source topic untouched");
        assertEquals(STATE_VERBS, exercise(original, q2).getMiniTheory());
    }

    @Test
    @DisplayName("should keep the answer alone as the stored sentence of a transformation whatever an applied candidate changes, as «Mary is always at home.» when only its translation changes")
    @Tag("FEAT-DATOS")
    @Tag("F-DATOS-R001")
    void shouldKeepTheAnswerAloneAsTheStoredSentenceOfATransformationWhateverAnAppliedCandidateChanges() {
        // F-DATOS-R001 (José, 25/10): the candidate changes only the translation of «Mary is at home. (always) ___», whose
        // stored sentence had the sentence to transform glued; measured, it keeps its answer alone.
        KnowledgeEntity original = afirmativas();
        QuizTemplateEntity mary = exercise(original, VEGETABLES);
        mary.getForm().setSentenceParts(new java.util.ArrayList<>(List.of(text("Mary is at home. (always)"),
                cloze("Mary is always at home."))));
        mary.setSentences(new java.util.ArrayList<>(List.of("Mary is at home. Mary is always at home.")));

        KnowledgeEntity applied = TopicState.apply(original, VEGETABLES, candidate(PanelCandidateKind.WHOLE_EXERCISE,
                MesaFixture.withTranslation(mary, "Mary siempre está en casa.")), List.of());

        assertEquals(List.of("Mary is always at home."), exercise(applied, VEGETABLES).getSentences());
        assertEquals("Mary siempre está en casa.", exercise(applied, VEGETABLES).getTranslation());
    }

    @Test
    @DisplayName("should rebuild the stored sentence from changed CLOZE parts when an accepted candidate has no sentences")
    @Tag("FEAT-DATOS")
    @Tag("F-DATOS-R001")
    void shouldRebuildTheStoredSentenceFromChangedClozePartsWhenAnAcceptedCandidateHasNoSentences() {
        KnowledgeEntity original = afirmativas();
        QuizTemplateEntity originalQuiz = exercise(original, VEGETABLES);
        String originalStoredSentence = originalQuiz.getSentences().get(0);
        String siblingStoredSentence = exercise(original, COME).getSentences().get(0);

        QuizTemplateEntity changed = withParts(originalQuiz, text("My cousin"), cloze("reads", "studies"),
                text("(read) every night."));
        changed.setSentences(null);

        KnowledgeEntity applied = TopicState.apply(original, VEGETABLES,
                candidate(PanelCandidateKind.WHOLE_EXERCISE, changed), List.of());

        QuizTemplateEntity appliedQuiz = exercise(applied, VEGETABLES);
        assertEquals("My cousin", appliedQuiz.getForm().getSentenceParts().get(0).getText());
        assertEquals(List.of("reads", "studies"), appliedQuiz.getForm().getSentenceParts().get(1).getOptions());
        assertEquals("(read) every night.", appliedQuiz.getForm().getSentenceParts().get(2).getText());
        assertEquals(List.of("My cousin reads every night."), appliedQuiz.getSentences(),
                "the stored sentence uses the first CLOZE answer and omits the parenthesized hint");

        assertEquals(originalStoredSentence, exercise(original, VEGETABLES).getSentences().get(0),
                "apply keeps the source quiz untouched");
        assertEquals(List.of("love"),
                exercise(original, VEGETABLES).getForm().getSentenceParts().get(1).getOptions());
        assertEquals(siblingStoredSentence, exercise(applied, COME).getSentences().get(0),
                "apply keeps the sibling quiz untouched");
        assertEquals("(come) every year.",
                exercise(applied, COME).getForm().getSentenceParts().get(2).getText());
    }
}
