package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.panel.CandidateRejection;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.PanelRunSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class WhenQuestionRepairTest {
    private static final String Q = "67fab6d59930102295341eae";
    private static final String PEER = "67fab6d59930102295341e9e";

    @Test
    @Tag("F-MESA-R007")
    void retriesAnIncompleteWhenQuestionWithThePreviousDuplicateFeedbackAndRepairsTheWholeExercise() {
        KnowledgeEntity topic = afirmativas();
        QuizTemplateEntity original = withParts(exercise(topic, VEGETABLES), text("When"), cloze("is she"), text("? (She)"));
        original.setId(Q);
        original.setTranslation("¿Cuándo es ella?");
        original.setSentences(List.of("When is she?"));
        QuizTemplateEntity peer = withParts(exercise(topic, COME), text("Why"), cloze("is she"), text("at home? (She)"));
        peer.setId(PEER);
        peer.setSentences(List.of("Why is she at home?"));
        topic.setQuizTemplates(new ArrayList<>(List.of(original, peer)));
        Finding instruction = finding("quiz-instruction", "instruction-breach", Q, FindingSeverity.HIGH,
                FindingResolution.PANEL, null, "Falta un evento o estado para preguntar cuándo");
        Finding sense = finding("translation-sense", "native-would-say", Q, FindingSeverity.MEDIUM,
                FindingResolution.PANEL, null, "When is she? no expresa una pregunta completa");
        Finding duplicate = finding("near-duplicates", "same-exercise", Q, FindingSeverity.HIGH,
                FindingResolution.PANEL, PEER, "When ___ at home? queda casi igual al quiz " + PEER);
        QuizTemplateEntity repeated = withParts(original, text("When"), cloze("is she"), text("at home? (She)"));
        repeated.setTranslation("¿Cuándo está ella en casa?");
        QuizTemplateEntity repaired = withParts(original, text("When"), cloze("is her birthday"), text("? (her birthday)"));
        repaired.setTranslation("¿Cuándo es su cumpleaños?");
        repaired.setMiniTheory("Para preguntar una fecha con sujeto singular: $When + is + sujeto$?");
        RecordingProposer proposer = new RecordingProposer(script(Q,
                proposal(candidate(PanelCandidateKind.ENGLISH_CHANGE, repeated)), Q,
                proposal(candidate(PanelCandidateKind.ENGLISH_CHANGE, repaired))));
        RulesMeter meter = new RulesMeter()
                .rule("quiz-instruction", (t, q) -> Q.equals(q.getId()) && "When is she?".equals(stored(q))
                        ? List.of(instruction) : RulesMeter.none())
                .rule("translation-sense", (t, q) -> Q.equals(q.getId()) && "When is she?".equals(stored(q))
                        ? List.of(sense) : RulesMeter.none())
                .rule("near-duplicates", (t, q) -> Q.equals(q.getId()) && "When is she at home?".equals(stored(q))
                        ? List.of(duplicate) : RulesMeter.none());
        RecordingSink sink = new RecordingSink();
        List<String> validators = new ArrayList<>(ANALYZERS);
        validators.add("near-duplicates");

        panel(proposer, meter).run(work(topic, List.of(brief("task-1312", instruction), brief("task-1973", sense)),
                null, Map.of(), List.of(new CandidateRejection(0, null, List.of("Devuelto por near-duplicates: " + PEER)))),
                new PanelRunSettings(1, 1, 3, validators), allowance(2, 1), sink);

        assertEquals(PanelOutcomeKind.ACCEPTED, sink.only().getOutcome());
        assertEquals(List.of("task-1312", "task-1973"), sink.only().getResolvedTaskIds());
        assertEquals(2, sink.only().getAttempts());
        assertTrue(proposer.requests.get(0).getPreviousRejections().get(0).getReasons().get(0).contains(PEER),
                "a new explicit retry sees the previous closing feedback even at attempt one");
        assertTrue(proposer.requests.get(1).getPreviousRejections().stream().flatMap(r -> r.getReasons().stream())
                .anyMatch(r -> r.contains("near-duplicates")), "the duplicate is caught before acceptance");
        assertEquals(repaired.getTranslation(), sink.only().getAccepted().getQuiz().getTranslation());
        assertEquals(repaired.getMiniTheory(), sink.only().getAccepted().getQuiz().getMiniTheory());
        assertTrue(meter.analyzersAsked.stream().allMatch(a -> a.contains("near-duplicates") && a.contains("mini-theory")));
        assertEquals("When is she?", original.getSentences().get(0), "the original course is never written");
        assertTrue(meter.statesMeasured.stream().allMatch(t -> "Why is she at home?".equals(stored(exercise(t, PEER)))));
    }
}
