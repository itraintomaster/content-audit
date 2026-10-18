package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The five alternatives of How ___ ? (She) must be repaired as one exercise, including its card. */
class SingleAnswerWholeExerciseRepairTest {
    private static final String Q = "67fab6d59930102295341e9b";
    private static final String OLD_CARD = "La palabra wh- primero y después be + sujeto: $Where is he$?";
    private static final String NEW_CARD = "Con $she$, $be$ en presente simple toma la forma $is$.";

    @Test
    @Tag("F-MESA-R007")
    @Tag("F-MESA-R012")
    void repairsTheSentenceAndCardTogetherBeforeSendingAllFiveTasksToReview() {
        KnowledgeEntity topic = afirmativas();
        QuizTemplateEntity original = withParts(exercise(topic, VEGETABLES), text("How"), cloze("is she"), text("? (She)"));
        original.setId(Q);
        original.setTranslation("¿Cómo está ella?");
        original.setMiniTheory(OLD_CARD);
        QuizTemplateEntity sibling = exercise(topic, COME);
        sibling.setMiniTheory(OLD_CARD);
        topic.setQuizTemplates(new ArrayList<>(List.of(original, sibling)));

        List<Finding> findings = List.of("busy is she", "happy is she", "heavy is she", "old is she", "tall is she")
                .stream().map(a -> finding("single-answer", "different-valid-answer", Q,
                        FindingSeverity.BLOCKING, FindingResolution.PANEL, a, "También vale: " + a)).toList();
        List<ErrorTaskBrief> tasks = new ArrayList<>();
        for (int i = 0; i < findings.size(); i++) tasks.add(brief("task-" + (71 + i), findings.get(i)));

        QuizTemplateEntity phraseOnly = withParts(original, text("How"), cloze("is"), text("she? (be)"));
        QuizTemplateEntity complete = withParts(original, text("How"), cloze("is"), text("she? (be)"));
        complete.setMiniTheory(NEW_CARD);
        RecordingProposer proposer = new RecordingProposer(script(Q,
                proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, phraseOnly)),
                Q, proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, complete))));
        RulesMeter meter = new RulesMeter()
                .rule("single-answer", (t, q) -> Q.equals(q.getId())
                        && q.getForm().getSentenceParts().stream().anyMatch(p -> p.getOptions() != null
                                && p.getOptions().contains("is she")) ? findings : RulesMeter.none())
                .rule("mini-theory", (t, q) -> Q.equals(q.getId()) && OLD_CARD.equals(q.getMiniTheory())
                        && q.getForm().getSentenceParts().stream().anyMatch(p -> p.getOptions() != null
                                && p.getOptions().contains("is"))
                        ? List.of(finding("mini-theory", "insufficient-for-quiz", Q, FindingSeverity.HIGH,
                                FindingResolution.PANEL, OLD_CARD, "La tarjeta no explica is con she")) : RulesMeter.none());
        RecordingSink sink = new RecordingSink();

        panel(proposer, meter).run(work(topic, tasks), settings(), allowance(2, 1), sink);

        assertEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
        assertEquals(2, sink.only().getAttempts());
        assertEquals(tasks.stream().map(t -> t.getTask().getId()).toList(), sink.only().getResolvedTaskIds());
        assertEquals(NEW_CARD, sink.only().getAccepted().getQuiz().getMiniTheory());
        assertEquals(2, proposer.requests.size());
        assertTrue(proposer.requests.get(1).getPreviousRejections().stream()
                .flatMap(r -> r.getReasons().stream()).anyMatch(r -> r.contains("mini-theory")));
        assertTrue(proposer.requests.get(1).getBriefs().stream()
                .allMatch(b -> b.getEditScope().containsAll(List.of(ExercisePart.STATEMENT,
                        ExercisePart.TRANSLATION, ExercisePart.MINI_THEORY))));
        assertTrue(meter.analyzersAsked.stream().allMatch(a -> a.contains("mini-theory")));
        assertEquals(OLD_CARD, original.getMiniTheory(), "the panel never edits the original course");
        assertTrue(meter.statesMeasured.stream().allMatch(t -> OLD_CARD.equals(exercise(t, COME).getMiniTheory())),
                "a shared card on a sibling is never rewritten");
    }

    @Test
    void rechecksAnUnchangedCardWhenItsQuizChangesButDoesNotConsultItForAudioAlone() {
        for (ExercisePart part : List.of(ExercisePart.STATEMENT, ExercisePart.HINT, ExercisePart.ANSWERS,
                ExercisePart.OPTIONS, ExercisePart.INSTRUCTIONS, ExercisePart.FORM_KIND)) {
            assertTrue(AnalyzerReads.readsAny("mini-theory", java.util.Set.of(part)), part.name());
        }
        assertFalse(AnalyzerReads.readsAny("mini-theory", java.util.Set.of(ExercisePart.AUDIO)));
    }
}
