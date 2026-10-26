package com.learney.contentaudit.revisiondomain.panelengine;

import static com.learney.contentaudit.revisiondomain.panelengine.MesaFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.finding.FindingSeverity;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeGateVerdict;
import com.learney.contentaudit.revisiondomain.nonregression.ChangeProbe;
import com.learney.contentaudit.revisiondomain.nonregression.LengthDeclaration;
import com.learney.contentaudit.revisiondomain.nonregression.SpokenText;
import com.learney.contentaudit.revisiondomain.nonregression.UnevaluatedMark;
import com.learney.contentaudit.revisiondomain.nonregression.UnitMeasurement;
import com.learney.contentaudit.revisiondomain.nonregressionengine.DefaultGateFactory;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidate;
import com.learney.contentaudit.revisiondomain.panel.PanelCandidateKind;
import com.learney.contentaudit.revisiondomain.panel.PanelOutcomeKind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-MESA-R006: a candidate is accepted when it resolves or improves a task without worsening anything. The lengths are
 * the ones of the camera in brief 214: 18 today, with a goal of 3 to 8.
 */
public class AdvanceDecisionTest {

    @Test
    @DisplayName("should no longer accept the camera shortened from 18 to 10 when that is all it does, though its length task measures improved")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    @Tag("F-MESA-R029")
    public void shouldNotAcceptTheCameraShortenedFrom18To10WhenThatIsAllItDoes() {
        UnitMeasurement original = measured(List.of(length()), 18, "It's not okay to use a camera here.");
        UnitMeasurement shorter = measured(List.of(length()), 10, "No cameras here. You can't use one.");

        ScriptDecision decision = script().decide(List.of(lengthTask()), original, List.of(scored(shorter, original)));
        TaskProgress progress = script().progress(List.of(lengthTask()), original, shorter);

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("sólo acorta, y el largo solo no alcanza para aceptar un cambio"
                + " — sigue abierta t-largo: largo 10 (meta 3 a 8)"), decision.getReasons().toString());
        assertEquals(List.of("t-largo"), progress.improved(), "the measure still says what it did to the length");
        assertEquals("largo 18 → 10 (meta 3 a 8)", progress.improvements().get("t-largo"));
    }

    @Test
    @DisplayName("should reject the camera lengthened from 18 to 20, saying how long it is and what the goal is")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    @Tag("F-MESA-R007")
    public void shouldRejectTheCameraLengthenedFrom18To20SayingItsLength() {
        UnitMeasurement original = measured(List.of(length()), 18, "It's not okay to use a camera here.");
        ScoredCandidate longer = scored(measured(List.of(length()), 20, "It's really not okay to use a camera here."),
                original);

        ScriptDecision decision = script().decide(List.of(lengthTask()), original, List.of(longer));

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("no resuelve ni mejora ninguna tarea del ejercicio"
                + " — sigue abierta t-largo: largo 20 (meta 3 a 8)"), decision.getReasons().toString());
    }

    @Test
    @DisplayName("should accept as an improvement a single-answer finding that goes from blocking to low")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    public void shouldAcceptAFindingThatGetsLighterAsAnImprovement() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING)), 5, "He comes every year.");
        ScoredCandidate lighter = scored(measured(List.of(come(FindingSeverity.LOW)), 5, "He comes every year."), original);

        ScriptDecision decision = script().decide(List.of(comeTask()), original, List.of(lighter));

        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(List.of("t-come"), decision.getProgress().improved());
        assertEquals("1 marca bloqueante → 1 marca baja", decision.getProgress().improvements().get("t-come"));
    }

    @Test
    @DisplayName("should reject an advance that takes the length out of its range, 5 → 12, even if its finding gets lighter")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    @Tag("F-MESA-R029")
    public void shouldRejectAnAdvanceThatTakesTheLengthAwayFromItsRange() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING)), 5, "He comes every year.");
        ScoredCandidate away = scored(measured(List.of(come(FindingSeverity.LOW)), 12, "He comes every year."), original);

        ScriptDecision decision = script().decide(List.of(comeTask()), original, List.of(away));

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("aleja el largo del rango: 5 → 12 (meta 3 a 8)"),
                decision.getReasons().toString());
    }

    @Test
    @DisplayName("should still accept a candidate that resolves its task although its length goes out of range, 5 → 12")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    @Tag("F-NOEMP-R003")
    public void shouldStillAcceptAFixThatResolvesItsTaskWithTheLengthOutOfRange() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING)), 5, "He comes every year.");
        ScoredCandidate fixed = scored(measured(List.of(), 12, "He comes every year."), original);

        ScriptDecision decision = script().decide(List.of(comeTask()), original, List.of(fixed));

        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(List.of("t-come"), decision.getResolvedTaskIds());
    }

    @Test
    @DisplayName("should reject a candidate that resolves its task but leaves heavier a finding the exercise already had")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    public void shouldRejectACandidateThatLeavesAFindingItHadHeavier() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING), saved(FindingSeverity.LOW)), 5,
                "He comes every year.");
        ScoredCandidate heavier = scored(measured(List.of(saved(FindingSeverity.MEDIUM)), 5, "He comes every year."),
                original);

        ScriptDecision decision = script().decide(List.of(comeTask()), original, List.of(heavier));

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("sube de gravedad data-coherence (saved-sentence) baja → media"),
                decision.getReasons().toString());
    }

    @Test
    @DisplayName("should not count as gone a finding whose judge did not look at the candidate")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    public void shouldNotCountAsGoneAFindingWhoseJudgeDidNotLook() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING)), 5, "He comes every year.");
        UnitMeasurement unseen = measured(List.of(), 5, "He comes every year.");
        unseen.setUnevaluated(new ArrayList<>(List.of(new UnevaluatedMark(COME, "single-answer"))));

        ScriptDecision decision = script().decide(List.of(comeTask()), original, List.of(scored(unseen, original)));

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("single-answer sin evaluar"), decision.getReasons().toString());
    }

    @Test
    @DisplayName("should prefer a candidate that resolves a task over one that only shortens the camera")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R006")
    public void shouldPreferACandidateThatResolvesOverOneThatOnlyImproves() {
        UnitMeasurement original = measured(List.of(length(), come(FindingSeverity.LOW)), 18,
                "It's not okay to use a camera here.");
        ScoredCandidate shorter = scored(measured(List.of(length(), come(FindingSeverity.LOW)), 9,
                "No cameras. You can't use cameras."), original);
        ScoredCandidate fixed = scored(measured(List.of(length()), 18, "It's not okay to use a camera here."), original);

        ScriptDecision decision = script().decide(List.of(lengthTask(), comeTask()), original, List.of(shorter, fixed));

        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(1, decision.getWinnerIndex(), decision.getReasons().toString());
        assertEquals(List.of("t-come"), decision.getResolvedTaskIds());
    }

    @Test
    @DisplayName("should say what the camera as it ends did to each task against where it started: the length improved, the single answer resolved")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R007")
    public void shouldSayWhatTheExerciseAsItEndsDidToEachTask() {
        UnitMeasurement from = measured(List.of(length(), come(FindingSeverity.BLOCKING)), 18,
                "It's not okay to use a camera here.");
        UnitMeasurement to = measured(List.of(length()), 9, "No cameras. You can't use cameras.");

        TaskProgress progress = script().progress(List.of(lengthTask(), comeTask()), from, to);

        assertEquals(List.of("t-come"), progress.resolved());
        assertEquals(List.of("t-largo"), progress.improved());
        assertEquals("largo 18 → 9 (meta 3 a 8)", progress.improvements().get("t-largo"));
    }

    @Test
    @DisplayName("should reject a candidate whose only advance is the length, even when it brings the length into its range")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R029")
    public void shouldRejectACandidateWhoseOnlyAdvanceIsTheLength() {
        UnitMeasurement original = measured(List.of(length(), come(FindingSeverity.BLOCKING)), 12, "He comes every year.");
        ScoredCandidate shorter = scored(measured(List.of(come(FindingSeverity.BLOCKING)), 8, "He comes each year."),
                original);

        ScriptDecision decision = script().decide(List.of(lengthTask(), comeTask()), original, List.of(shorter));

        assertEquals(PanelOutcomeKind.NO_WINNER, decision.getOutcome());
        assertTrue(decision.getReasons().get(0).contains("sólo acorta, y el largo solo no alcanza para aceptar un cambio"),
                decision.getReasons().toString());
    }

    @Test
    @DisplayName("should prefer the candidate that resolves a high task over the one that resolves two low ones")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R029")
    public void shouldPreferTheCandidateThatResolvesAHighTaskOverOneThatResolvesTwoLowOnes() {
        Finding duplicate = finding("exact-duplicates", "same-exercise", COME, FindingSeverity.LOW, FindingResolution.PANEL,
                null, "duplicado");
        UnitMeasurement original = measured(List.of(come(FindingSeverity.HIGH), duplicate, saved(FindingSeverity.LOW)), 5,
                "He comes every year.");
        ScoredCandidate twoLow = scored(measured(List.of(come(FindingSeverity.HIGH)), 5, "He comes every year."), original);
        ScoredCandidate oneHigh = scored(measured(List.of(duplicate, saved(FindingSeverity.LOW)), 5, "He comes every year."),
                original);

        ScriptDecision decision = script().decide(List.of(brief("t-come", come(FindingSeverity.HIGH)),
                brief("t-dup", duplicate), brief("t-saved", saved(FindingSeverity.LOW))), original, List.of(twoLow, oneHigh));

        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(1, decision.getWinnerIndex(), decision.getReasons().toString());
        assertEquals(List.of("t-come"), decision.getResolvedTaskIds());
    }

    @Test
    @DisplayName("should prefer the candidate that does not change what is heard over the one that also brings the length into its range")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R029")
    public void shouldLetTheLengthBreakATieOnlyAtTheEnd() {
        UnitMeasurement original = measured(List.of(length(), come(FindingSeverity.BLOCKING)), 12, "He comes every year.");
        ScoredCandidate keepsItsLength = scored(measured(List.of(length()), 12, "He comes every year."), original);
        ScoredCandidate alsoShorter = scored(measured(List.of(), 8, "He comes each year."), original);

        ScriptDecision decision = script().decide(List.of(lengthTask(), comeTask()), original,
                List.of(alsoShorter, keepsItsLength));

        assertEquals(PanelOutcomeKind.ACCEPTED, decision.getOutcome());
        assertEquals(1, decision.getWinnerIndex(), "resolving the length and leaving no finding of it count nothing; "
                + "not changing what is heard comes first: " + decision.getReasons());
    }

    @Test
    @DisplayName("should accept an advance that takes the length two words farther from its range, 8 → 10, and reject one that takes it three, 8 → 11")
    @Tag("FEAT-MESA")
    @Tag("F-MESA-R029")
    public void shouldAcceptAnAdvanceThatTakesTheLengthTwoWordsFartherButNotThree() {
        UnitMeasurement original = measured(List.of(come(FindingSeverity.BLOCKING)), 8, "He comes every year.");
        ScoredCandidate two = scored(measured(List.of(come(FindingSeverity.LOW)), 10, "He comes every year."), original);
        ScoredCandidate three = scored(measured(List.of(come(FindingSeverity.LOW)), 11, "He comes every year."), original);

        ScriptDecision accepted = script().decide(List.of(comeTask()), original, List.of(two));
        ScriptDecision rejected = script().decide(List.of(comeTask()), original, List.of(three));

        assertEquals(PanelOutcomeKind.ACCEPTED, accepted.getOutcome(), accepted.getReasons().toString());
        assertEquals(PanelOutcomeKind.NO_WINNER, rejected.getOutcome());
        assertTrue(rejected.getReasons().get(0).contains("aleja el largo del rango: 8 → 11 (meta 3 a 8)"),
                rejected.getReasons().toString());
    }

    // -------------------------------------------------------------------------

    private static DefaultPanelDecisionScript script() {
        return new DefaultPanelDecisionScript(corrections());
    }

    private static Finding length() {
        return finding("sentence-length", "longer-than-goal", COME, FindingSeverity.LOW, FindingResolution.PANEL, null,
                "Largo fuera de la meta de A1");
    }

    private static ErrorTaskBrief lengthTask() {
        return brief("t-largo", length(), List.of(com.learney.contentaudit.auditdomain.correction.ExercisePart.STATEMENT));
    }

    private static Finding come(FindingSeverity severity) {
        return finding("single-answer", "different-valid-answer", COME, severity, FindingResolution.PANEL, null,
                "«'s coming» también encaja");
    }

    private static ErrorTaskBrief comeTask() {
        return brief("t-come", come(FindingSeverity.BLOCKING));
    }

    private static Finding saved(FindingSeverity severity) {
        return finding("data-coherence", "saved-sentence", COME, severity, FindingResolution.PANEL, "arbol",
                "la oración guardada no es la de hoy");
    }

    /** The exercise measured: its findings, its length against a goal of 3 to 8 and what it says. */
    private static UnitMeasurement measured(List<Finding> findings, int tokens, String says) {
        return new UnitMeasurement(CefrLevel.A1, new ArrayList<>(findings), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(new SpokenText(COME, audioKey(COME), new ArrayList<>(List.of(says))))),
                new ArrayList<>(List.of(new LengthDeclaration(COME, tokens, 3, 8, tokens >= 3 && tokens <= 8))));
    }

    /** A candidate measured and passed through the real change gate against the original. */
    private static ScoredCandidate scored(UnitMeasurement after, UnitMeasurement original) {
        ChangeGateVerdict gate = new DefaultGateFactory().changeGate().check(new ChangeProbe(List.of(COME), false, original,
                after, new ArrayList<>()));
        PanelCandidate candidate = new PanelCandidate(PanelCandidateKind.WHOLE_EXERCISE, null, null, null, "porque sí");
        return new ScoredCandidate(candidate, after, gate, new ArrayList<>());
    }
}
