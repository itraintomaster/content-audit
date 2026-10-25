package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.panel.CandidateRejection;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelDecision;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import com.learney.contentaudit.revisiondomain.panel.PanelProposal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-MESA-R007: three attempts, each from the last candidate accepted in the pass; up to five while the last one
 * improved; at the end, one change from how the exercise was. The story is the camera of brief 214, counted in words by
 * the meter of the fixture: 13 words today, with a range of 3 to 8.
 */
public class AdvancingAttemptsTest {

    private static final String CAMERA = "67fab6d59930102295342de8";

    private static final String NOT_OKAY = "It's not okay to use a camera here. You";

    @Test
    @DisplayName("should start each attempt from the camera the last accepted candidate left, 13 → 11 → 9, until 7 words resolve its length, and keep it as one change from 13")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldStartEachAttemptFromTheLastAcceptedCandidateAndKeepOneChange() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are not okay here. You", "use a camera here."),  // 11 words
                CAMERA, says("No cameras here. You", "use a camera here."),            // 9 words
                CAMERA, says("No cameras. You", "use cameras here.")));                // 7 words
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(List.of("t-largo"), decision.getResolvedTaskIds());
        assertEquals(3, decision.getAttempts());
        assertEquals("No cameras. You can't use cameras here.", stored(decision.getAccepted().getQuiz()),
                "one change, from how the exercise was to how the last candidate left it");
        assertEquals(PanelCandidateKind.WHOLE_EXERCISE, decision.getAccepted().getKind());
        assertTrue(decision.getAcceptedVerdict().isPasses(), "the gate against the original");
        assertEquals("Cameras are not okay here. You ___ use a camera here.",
                proposer.requests.get(1).getTopicView().get(0).getStatement(),
                "the second attempt sees the first candidate as its starting point");
        assertEquals("No cameras here. You ___ use a camera here.",
                proposer.requests.get(2).getTopicView().get(0).getStatement());
        List<String> back = reasons(proposer.requests.get(1).getPreviousRejections());
        assertTrue(back.stream().anyMatch(r -> r.startsWith("se guardó como punto de partida")
                && r.contains("largo 13 → 11 (meta 3 a 8)") && r.contains("sigue abierta t-largo: largo 11 (meta 3 a 8)")),
                "R007 inv. 3: the advance goes back with what it improved and what is left: " + back);
        assertTrue(back.contains("propuso «Cameras are not okay here. You ___ use a camera here.»"), back.toString());
    }

    @Test
    @DisplayName("should give the camera a fourth and a fifth attempt while each one shortens it, 13 → 12 → 11 → 10 → 9 → 8 words, and stop at five")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldGiveUpToFiveAttemptsWhileEachOneImproves() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("It is not okay to use cameras here. You", "use cameras."),  // 12
                CAMERA, says("It is not okay to use cameras. You", "use cameras."),       // 11
                CAMERA, says("It's not okay to use cameras. You", "use cameras."),        // 10
                CAMERA, says("Not okay to use cameras. You", "use cameras."),             // 9
                CAMERA, says("No cameras here, please. You", "use cameras."),             // 8: resolves
                CAMERA, says("No cameras. You", "use them.")));                           // never asked
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        assertEquals(5, proposer.requests.size(), "three, and two more because the last one improved");
        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(5, decision.getAttempts());
        assertEquals(List.of("t-largo"), decision.getResolvedTaskIds());
    }

    @Test
    @DisplayName("should keep the camera at 9 words after five attempts that only shortened it, with its task improved but open and its measure 13 → 9")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldKeepTheAdvanceWithItsTaskImprovedButOpenAfterFiveAttempts() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("It is not okay to use cameras here. You", "use cameras."),  // 12
                CAMERA, says("It is not okay to use cameras. You", "use cameras."),       // 11
                CAMERA, says("It's not okay to use cameras. You", "use cameras."),        // 10
                CAMERA, says("Not okay to use cameras. You", "use cameras."),             // 9
                CAMERA, says("It's not okay to use cameras here. You", "use cameras.")));  // 11: worse
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(5, decision.getAttempts());
        assertTrue(decision.getResolvedTaskIds().isEmpty());
        assertEquals(List.of("t-largo"), decision.getImprovedTaskIds());
        assertEquals("largo 13 → 9 (meta 3 a 8)", decision.getImprovements().get("t-largo"));
        assertEquals("Not okay to use cameras. You can't use cameras.", stored(decision.getAccepted().getQuiz()));
    }

    @Test
    @DisplayName("should not give a fourth attempt when the third one did not improve, and keep the 11 words of the first")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldNotGiveMoreAttemptsWhenTheLastOneDidNotImprove() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are not okay here. You", "use a camera here."),          // 11: improves
                CAMERA, says("Cameras are really not okay here. You", "use a camera here."),   // 12: worse
                CAMERA, says("Cameras are not okay in here. You", "use a camera here."),       // 12: worse
                CAMERA, says("No cameras. You", "use cameras here.")));                         // never asked
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        assertEquals(3, proposer.requests.size());
        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals("largo 13 → 11 (meta 3 a 8)", decision.getImprovements().get("t-largo"));
    }

    @Test
    @DisplayName("should give a fourth attempt when only the third one improved, and stop when the fourth does not")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldGiveAnotherAttemptOnlyAfterOneThatImproved() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are really not okay in here. You", "use a camera here."),  // 13: same
                CAMERA, says("Cameras are really not okay to use here. You", "use a camera."),   // 13: same
                CAMERA, says("Cameras are not okay here. You", "use a camera here."),            // 11: improves
                CAMERA, says("Cameras are really not okay here. You", "use a camera here."),     // 12: worse
                CAMERA, says("No cameras. You", "use cameras here.")));                           // never asked
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        assertEquals(4, proposer.requests.size());
        assertEquals(4, sink.only().getAttempts());
        assertEquals(List.of("t-largo"), sink.only().getImprovedTaskIds());
    }

    @Test
    @DisplayName("should leave the camera as it was after three attempts that never shorten it, sending back what each proposed and how long it was")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldLeaveTheExerciseAsItWasWhenNoAttemptImproves() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are really not okay in here. You", "use a camera here."),  // 13
                CAMERA, says("It is really not okay to use a camera here. You", "use one."),     // 14
                CAMERA, says("Cameras are really not okay in here at all. You", "use one.")));   // 13
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertEquals(3, proposer.requests.size());
        List<String> back = reasons(proposer.requests.get(1).getPreviousRejections());
        assertTrue(back.contains("propuso «Cameras are really not okay in here. You ___ use a camera here.»"), back.toString());
        assertTrue(back.stream().anyMatch(r -> r.contains("no resuelve ni mejora ninguna tarea del ejercicio")
                && r.contains("sigue abierta t-largo: largo 13 (meta 3 a 8)")),
                "R007 inv. 2: the rejection says how long it still is: " + back);
    }

    @Test
    @DisplayName("should keep the 11 words of the first attempt when the judges of the translation disagree on the second, without more attempts")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldKeepTheAdvanceWhenTheJudgesDisagreeAfterIt() {
        KnowledgeEntity topic = camera();
        String disputed = "No cameras here. You can't use a camera here.";
        RulesMeter meter = lengthMeter().rule("translation-sense", (t, quiz) -> disputed.equals(stored(quiz))
                ? new ArrayList<>(List.of(finding("translation-sense", "judges-disagree", CAMERA, FindingSeverity.HIGH,
                        FindingResolution.RANK_ONLY, null, "Los jueces no coinciden")))
                : RulesMeter.none());
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are not okay here. You", "use a camera here."),  // 11: improves
                CAMERA, says("No cameras here. You", "use a camera here."),            // the judges disagree
                CAMERA, says("No cameras. You", "use cameras here.")));                // never asked
        RecordingSink sink = new RecordingSink();

        panel(proposer, meter).run(work(topic, List.of(lengthTask())), settings(), allowance(50, 4), sink);

        assertEquals(2, proposer.requests.size(), "R007 inv. 6: no more attempts after the disagreement");
        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals("Cameras are not okay here. You can't use a camera here.", stored(decision.getAccepted().getQuiz()));
        assertTrue(reasons(decision.getRejections()).stream().anyMatch(r -> r.contains("no coinciden; decide José")),
                reasons(decision.getRejections()).toString());
    }

    @Test
    @DisplayName("should keep the 11 words of the first attempt when the allowance runs out before the second")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    @Tag("F-MESA-R010")
    public void shouldKeepTheAdvanceWhenTheAllowanceRunsOut() {
        KnowledgeEntity topic = camera();
        RecordingProposer proposer = new RecordingProposer(script(
                CAMERA, says("Cameras are not okay here. You", "use a camera here."),
                CAMERA, says("No cameras. You", "use cameras here.")));
        RecordingSink sink = new RecordingSink();

        panel(proposer, lengthMeter()).run(work(topic, List.of(lengthTask())), settings(), allowance(1, 1), sink);

        assertEquals(1, proposer.requests.size());
        PanelDecision decision = sink.only();
        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(List.of("t-largo"), decision.getImprovedTaskIds());
    }

    // -------------------------------------------------------------------------

    /** «It's not okay to use a camera here. You ___ (can't) use a camera.»: 13 words, 3 to 8 in A1. */
    private static KnowledgeEntity camera() {
        return topic(AFIRMATIVAS, "Permiso general", List.of(quiz(CAMERA, "No está permitido usar una cámara aquí.",
                text(NOT_OKAY), cloze("can't"), text("use a camera."))));
    }

    private static PanelProposal says(String before, String after) {
        QuizTemplateEntity quiz = withParts(camera().getQuizTemplates().get(0), text(before), cloze("can't"), text(after));
        return proposal(candidate(PanelCandidateKind.WHOLE_EXERCISE, quiz));
    }

    /** The length analyzer: one finding while the stored sentence has more than 8 words. */
    private static RulesMeter lengthMeter() {
        return new RulesMeter().rule("sentence-length", (t, quiz) -> stored(quiz).split("\\s+").length > 8
                ? new ArrayList<>(List.of(lengthFinding())) : RulesMeter.none());
    }

    private static Finding lengthFinding() {
        return finding("sentence-length", "longer-than-goal", CAMERA, FindingSeverity.LOW, FindingResolution.PANEL, null,
                "más largo que la meta de A1");
    }

    private static ErrorTaskBrief lengthTask() {
        return brief("t-largo", lengthFinding(), List.of(ExercisePart.STATEMENT, ExercisePart.TRANSLATION,
                ExercisePart.STORED_SENTENCE));
    }

    private static List<String> reasons(List<CandidateRejection> rejections) {
        List<String> all = new ArrayList<>();
        for (CandidateRejection rejection : rejections) {
            all.addAll(rejection.getReasons());
        }
        return all;
    }
}
