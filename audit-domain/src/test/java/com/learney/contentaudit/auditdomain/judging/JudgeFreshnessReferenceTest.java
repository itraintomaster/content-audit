package com.learney.contentaudit.auditdomain.judging;

import static com.learney.contentaudit.auditdomain.judging.JudgeFreshnessFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FEAT-ALDIA")
@Tag("F-ALDIA-R003")
class JudgeFreshnessReferenceTest {
    @Test
    void aVerdictOfTodaysApprovedVersionAndRuntimeIsUpToDate() {
        assertEquals(JudgeVerdictMark.UP_TO_DATE, today().mark(V_TODAY, XHIGH));
        assertEquals(JudgeVerdictMark.UP_TO_DATE, today().mark(V_TODAY, MEDIUM));
    }

    @Test
    void aVerdictOfAnEarlierVersionIsFromBeforeByVersion() {
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION), today().mark(V_5_10, XHIGH));
    }

    @Test
    void aMotorNoVersionOfTheJudgeApprovedWithIsAnotherMotorForTodaysAndForAnEarlierVersion() {
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), today().mark(V_5_10, LOCAL));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), today().mark(V_TODAY, LOCAL));
    }

    /**
     * Inv. 7, the Miniteoría in Correction 1 on 20/10: today's version approved only with Gemini, and
     * 82 topics keep the verdicts the versions of 5/10 and 13/10 gave with ChatGPT, which they approved
     * with. They are of an earlier version, never «another motor».
     */
    @Test
    void anEarlierVersionWithAMotorTheJudgeApprovedWithIsAnEarlierVersionEvenIfTodaysApprovedOnlyWithAnother() {
        var reference = todayWithGeminiOnly(history());
        var earlier = JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION);
        assertEquals(earlier, reference.mark(V_5_10, XHIGH));
        assertEquals(earlier, reference.mark(V_13_10_MORNING, XHIGH));
        assertEquals(earlier, reference.mark(V_TODAY, MEDIUM));
        // V_5_10 never took its exam with medium, but another version approved with it.
        assertEquals(earlier, reference.mark(V_5_10, MEDIUM));
        assertTrue(reference.getJudgeApprovedRuntimes().containsAll(Set.of(XHIGH, MEDIUM, GEMINI)));
        // Today's version with a motor it did not approve with is still another motor.
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), reference.mark(V_GEMINI, MEDIUM));
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_GEMINI, GEMINI));
    }

    @Test
    void aMotorWhoseLastExamFailedInEveryVersionThatTookItIsAnotherMotorForAnEarlierVersion() {
        var claude = EvaluationRuntimeIdentity.explicit("claude-cli", "claude-opus", "high");
        var history = new java.util.ArrayList<>(history());
        history.add(record(V_5_10, JudgeExamStatus.APPROVED, "2026-10-07T12:00:00Z", claude));
        history.add(record(V_5_10, JudgeExamStatus.FAILED, "2026-10-08T12:00:00Z", claude));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME),
                todayWithGeminiOnly(history).mark(V_13_10_MORNING, claude));

        history.add(record(V_13_10_MORNING, JudgeExamStatus.APPROVED, "2026-10-13T17:00:00Z", claude));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION),
                todayWithGeminiOnly(history).mark(V_5_10, claude));
    }

    @Test
    void aMotorJosePromotedAnEarlierVersionWithIsAnEarlierVersion() {
        var promotion = new JudgePromotionRecord(JUDGE, V_13_10_MORNING, LOCAL, "José", "Acepto el piloto local",
                "approval.json", "b".repeat(64), PROMOTED_AT);
        var reference = JudgeFreshnessReference.of(geminiStanding(), geminiHistory(history()), List.of(promotion),
                ZONE, CLOCK);
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION), reference.mark(V_5_10, LOCAL));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), reference.mark(V_GEMINI, LOCAL));
    }

    @Test
    void aVerdictWithoutVersionOrWithLegacyRuntimeHasUnknownProvenance() {
        var unknown = JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.UNKNOWN_PROVENANCE);
        assertEquals(unknown, today().mark(null, XHIGH));
        assertEquals(unknown, today().mark(V_TODAY, null));
        assertEquals(unknown, today().mark(V_TODAY, EvaluationRuntimeIdentity.legacyUnknown()));
    }

    @Test
    void whenTodaysVersionIsNotApprovedNothingIsUpToDate() {
        var reference = JudgeFreshnessReference.of(standing(V_UNAPPROVED, JudgeExamStatus.FAILED, Set.of(V_UNAPPROVED)),
                history(), ZONE, CLOCK);
        var notApproved = JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.JUDGE_NOT_APPROVED);
        assertEquals(notApproved, reference.mark(V_UNAPPROVED, XHIGH));
        assertEquals(notApproved, reference.mark(V_TODAY, XHIGH));
        assertFalse(reference.isApproved());
        assertEquals("sin aprobar (c8ca16c9)", reference.currentVersionName());
        assertTrue(reference.currentApprovedAt().isEmpty());
    }

    @Test
    void aVersionRecordedAsTheSameIsUpToDateAndSharesTheApprovedRuntimes() {
        String afterCutover = "e9f0a1b2eeeeeeee";
        var reference = JudgeFreshnessReference.of(standing(afterCutover, JudgeExamStatus.APPROVED,
                Set.of(afterCutover, V_TODAY)), history(), ZONE, CLOCK);
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_TODAY, MEDIUM));
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(afterCutover, XHIGH));
        assertEquals("la del 13/10, 22:12", reference.currentVersionName());
    }

    @Test
    void aRuntimeWhoseLastExamOfTheVersionFailedIsNotApproved() {
        var history = new java.util.ArrayList<>(history());
        history.add(record(V_TODAY, JudgeExamStatus.FAILED, "2026-10-16T12:00:00Z", MEDIUM));
        var reference = JudgeFreshnessReference.of(standing(V_TODAY, JudgeExamStatus.APPROVED, Set.of(V_TODAY)),
                history, ZONE, CLOCK);
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), reference.mark(V_TODAY, MEDIUM));
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_TODAY, XHIGH));
    }

    @Test
    void aVersionJosePromotedIsApprovedAndItsVerdictsWithThatMotorAreUpToDate() {
        // Naturalidad: its exam failed with xhigh on the 15th; José promoted it with medium on the 14th.
        var promoted = new JudgeExamStanding(JUDGE, V_UNAPPROVED, JudgeExamStatus.PROMOTED, true,
                "Promovido por José", MEDIUM, Set.of(V_UNAPPROVED));
        promoted.setEnabledWith(List.of(new JudgeEnablement(MEDIUM, JudgeExamStatus.PROMOTED, PROMOTED_AT)));
        var reference = JudgeFreshnessReference.of(promoted, history(), ZONE, CLOCK);
        assertTrue(reference.isApproved());
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_UNAPPROVED, MEDIUM));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), reference.mark(V_UNAPPROVED, XHIGH));
        assertEquals("la del 14/10", reference.currentVersionName());
        assertEquals(PROMOTED_AT, reference.currentApprovedAt().orElseThrow());
    }

    @Test
    void aPromotionReadFromThePromotionsNamesTheVersionEvenWithoutItsMotors() {
        // An analysis saved before the motors were said: the standing alone, and José's promotions.
        var promoted = new JudgeExamStanding(JUDGE, V_UNAPPROVED, JudgeExamStatus.PROMOTED, true,
                "Promovido por José", MEDIUM, Set.of(V_UNAPPROVED));
        var promotion = new JudgePromotionRecord(JUDGE, V_UNAPPROVED, MEDIUM, "José", "Acepto el piloto",
                "approval.json", "a".repeat(64), PROMOTED_AT);
        var reference = JudgeFreshnessReference.of(promoted, history(), List.of(promotion), ZONE, CLOCK);
        assertTrue(reference.isApproved());
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_UNAPPROVED, MEDIUM));
        assertEquals("la del 14/10", reference.currentVersionName());
    }

    @Test
    void aVersionApprovedWithAMotorThatIsNotTheDefaultOneIsApprovedAndThatMotorIsUpToDate() {
        // Miniteoría: approved with Gemini on the 19th; the course runs with ChatGPT by default.
        var history = new java.util.ArrayList<>(history());
        history.add(record(V_GEMINI, JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        var standing = new JudgeExamStanding(JUDGE, V_GEMINI, JudgeExamStatus.NOT_TAKEN, false,
                "La versión vigente del juez no rindió el examen fijo.", MEDIUM, Set.of(V_GEMINI));
        standing.setEnabledWith(List.of(new JudgeEnablement(GEMINI, JudgeExamStatus.APPROVED,
                java.time.Instant.parse("2026-10-19T14:30:58Z"))));
        var reference = JudgeFreshnessReference.of(standing, history, ZONE, CLOCK);
        assertTrue(reference.isApproved(), "it is not «sin aprobar» because the default motor did not take it");
        assertEquals(JudgeVerdictMark.UP_TO_DATE, reference.mark(V_GEMINI, GEMINI));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), reference.mark(V_GEMINI, MEDIUM));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION), reference.mark(V_TODAY, GEMINI));
        assertEquals("la del 19/10", reference.currentVersionName());
        assertEquals(standing.getEnabledWith(), reference.getEnabledWith());

        // The history alone says it too, for an analysis saved before the motors were said.
        standing.setEnabledWith(List.of());
        var fromHistory = JudgeFreshnessReference.of(standing, history, ZONE, CLOCK);
        assertTrue(fromHistory.isApproved());
        assertEquals(JudgeVerdictMark.UP_TO_DATE, fromHistory.mark(V_GEMINI, GEMINI));
    }

    @Test
    void aVersionThatApprovedWithNoMotorIsStillNotApproved() {
        var standing = new JudgeExamStanding(JUDGE, V_GEMINI, JudgeExamStatus.NOT_TAKEN, false,
                "La versión vigente del juez no rindió el examen fijo.", MEDIUM, Set.of(V_GEMINI));
        var reference = JudgeFreshnessReference.of(standing, history(), ZONE, CLOCK);
        assertFalse(reference.isApproved());
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.JUDGE_NOT_APPROVED), reference.mark(V_GEMINI, GEMINI));
    }

    @Test
    void bothEndsOnTheSameOldVersionAreBothFromBefore() {
        // inv. 1: compared against today's judge, never against each other.
        var reference = today();
        assertFalse(reference.mark(V_5_10, XHIGH).isUpToDate());
        assertEquals(reference.mark(V_5_10, XHIGH), reference.mark(V_5_10, XHIGH));
    }

    @Test
    void theRuntimeIsReadFromHowTheAnalysisCarriesIt() {
        assertEquals(XHIGH, JudgeFreshnessReference.runtimeOf(XHIGH));
        assertEquals(XHIGH, JudgeFreshnessReference.runtimeOf(XHIGH.toString()));
        assertEquals(XHIGH, JudgeFreshnessReference.runtimeOf(Map.of("kind", "EXPLICIT", "providerName", "codex-cli",
                "modelName", "gpt-6.1-sol", "reasoningEffort", "xhigh")));
        assertNull(JudgeFreshnessReference.runtimeOf("codex-cli / gpt-6.1-sol / xhigh"));
        assertNull(JudgeFreshnessReference.runtimeOf(Map.of("kind", "EXPLICIT")));
        assertNull(JudgeFreshnessReference.runtimeOf(null));
        assertNull(JudgeFreshnessReference.runtimeOf(List.of()));
    }
}
