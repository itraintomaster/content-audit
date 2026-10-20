package com.learney.contentaudit.auditdomain.judging;

import static com.learney.contentaudit.auditdomain.judging.JudgeFreshnessFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.AuditTarget;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FEAT-ALDIA")
@Tag("F-ALDIA-R005")
class JudgeFreshnessTallyTest {
    @Test
    void theCorrectionOneCaseSaysWhatIsReusedFromBeforeByVersion() {
        var tally = new JudgeFreshnessTally(today(), "miniteoría", AuditTarget.KNOWLEDGE);
        assertTrue(tally.add(V_TODAY, XHIGH, 1).isUpToDate());
        tally.add(V_13_10_MORNING, XHIGH, 5);
        tally.add(V_5_10, XHIGH, 77);
        var count = tally.count();
        assertEquals("miniteoría: reusa 82 temas de antes (77 de la del 5/10, 5 de la del 13/10, 13:00)", count.summary());
        assertEquals(83, count.total());
        assertEquals(1, count.upToDate());
        assertEquals(82, count.fromBefore());
        assertEquals(0, count.notMeasured());
        assertEquals(count.total(), count.upToDate() + count.fromBefore() + count.notMeasured());
        assertEquals("la del 13/10, 22:12", count.currentVersionName());
        assertEquals(Instant.parse("2026-10-14T01:12:33Z"), count.currentApprovedAt());
        var first = count.fromBeforeGroups().get(0);
        assertEquals(V_5_10, first.judgeVersion());
        assertEquals(JudgeVerdictStaleCause.EARLIER_VERSION, first.cause());
        assertEquals(77, first.count());
    }

    /**
     * F-ALDIA-R003 inv. 7, Correction 1 on 20/10: today's Miniteoría approved only with Gemini, and the
     * 82 topics measured with ChatGPT by versions that approved with it are named by their version.
     */
    @Test
    @Tag("F-ALDIA-R003")
    void anEarlierVersionWithAMotorTheJudgeApprovedWithIsReusedByVersionNotByMotor() {
        var tally = new JudgeFreshnessTally(todayWithGeminiOnly(history()), "miniteoría", AuditTarget.KNOWLEDGE);
        assertTrue(tally.add(V_GEMINI, GEMINI, 1).isUpToDate());
        tally.add(V_5_10, MEDIUM, 76);
        tally.add(V_13_10_MORNING, MEDIUM, 5);
        tally.add(V_TODAY, XHIGH, 1);
        var count = tally.count();
        assertEquals("miniteoría: reusa 82 temas de antes (76 de la del 5/10, 5 de la del 13/10, 13:00, "
                + "1 de la del 13/10, 22:12)", count.summary());
        assertEquals(1, count.upToDate());
        assertEquals(82, count.fromBefore());
        assertTrue(count.fromBeforeGroups().stream().allMatch(g -> g.cause() == JudgeVerdictStaleCause.EARLIER_VERSION));
        assertEquals("la del 19/10", count.currentVersionName());
    }

    @Test
    void otherRuntimeUnknownAndNotMeasuredAreCountedApartAndSumToTheUnit() {
        var tally = new JudgeFreshnessTally(today(), "miniteoría", AuditTarget.QUIZ);
        tally.add(V_TODAY, XHIGH, 1_200);
        tally.add(V_TODAY, LOCAL, 3);
        tally.add(null, null, 2);
        tally.notMeasured(4);
        var count = tally.count();
        assertEquals(1_209, count.total());
        assertEquals(4, count.notMeasured());
        assertEquals("reusa 5 ejercicios de antes (3 de llama3 local, 2 de procedencia desconocida)", count.detail());
        var local = count.fromBeforeGroups().get(0);
        assertEquals(LOCAL.toString(), local.runtime());
        assertEquals("llama3 local", local.runtimeName());
    }

    @Test
    void nothingFromBeforeSaysSo() {
        var tally = new JudgeFreshnessTally(today(), "miniteoría", AuditTarget.KNOWLEDGE);
        tally.add(V_TODAY, XHIGH, 1_500);
        assertEquals("miniteoría: nada de antes", tally.count().summary());
    }

    /** The case of Correction 1 on 19/10: pragmatism abstained in 88 exercises, all with today's judge. */
    @Test
    @Tag("F-ALDIA-R003")
    void abstentionsAreCountedInsideTheirMarkAndTheThreeCountsStillAddUp() {
        var tally = new JudgeFreshnessTally(today(), "Pragmatismo comunicativo", AuditTarget.QUIZ);
        tally.add(V_TODAY, XHIGH, 2_314);
        assertTrue(tally.add(V_TODAY, XHIGH, 81, JudgeAbstention.NOT_APPLICABLE).isUpToDate());
        tally.add(V_TODAY, XHIGH, 7, JudgeAbstention.NEEDS_CONTEXT);
        var count = tally.count();
        assertEquals(2_402, count.total());
        assertEquals(2_402, count.upToDate());
        assertEquals(0, count.notMeasured());
        assertEquals(88, count.abstained());
        assertEquals(List.of(new JudgeAbstentionGroup(JudgeAbstention.NOT_APPLICABLE, "no aplica", 81),
                new JudgeAbstentionGroup(JudgeAbstention.NEEDS_CONTEXT, "falta contexto", 7)), count.abstentions());
        assertEquals("Pragmatismo comunicativo: nada de antes; se abstuvo en 88 (81 no aplica, 7 falta contexto)",
                count.summary());

        // An abstention of an earlier version stays from before: re-measuring it may give a score.
        var before = new JudgeFreshnessTally(today(), "Pragmatismo comunicativo", AuditTarget.QUIZ);
        assertFalse(before.add(V_5_10, XHIGH, 1, JudgeAbstention.REVIEW).isUpToDate());
        before.notMeasured(2);
        var stale = before.count();
        assertEquals(1, stale.fromBefore());
        assertEquals(1, stale.abstained());
        assertEquals(2, stale.notMeasured());
        assertEquals(stale.total(), stale.upToDate() + stale.fromBefore() + stale.notMeasured());
        assertEquals("reusa 1 ejercicio de antes (1 de la del 5/10); se abstuvo en 1 (1 revisar)", stale.detail());

        // Without abstentions nothing changes in what the reports print.
        var none = new JudgeFreshnessTally(today(), "miniteoría", AuditTarget.KNOWLEDGE);
        none.add(V_TODAY, XHIGH, 3);
        assertEquals(0, none.count().abstained());
        assertEquals(List.of(), none.count().abstentions());
        assertEquals("miniteoría: nada de antes", none.count().summary());
    }

    @Test
    void anUnapprovedJudgeSaysItAndNamesTodaysVersion() {
        var reference = JudgeFreshnessReference.of(standing(V_UNAPPROVED, JudgeExamStatus.FAILED, Set.of(V_UNAPPROVED)),
                history(), ZONE, CLOCK);
        var tally = new JudgeFreshnessTally(reference, "miniteoría", AuditTarget.KNOWLEDGE);
        tally.add(V_UNAPPROVED, XHIGH, 2);
        tally.add(V_5_10, XHIGH, 1);
        var count = tally.count();
        assertFalse(count.approved());
        assertEquals("miniteoría (sin aprobar): reusa 3 temas de antes (2 de la versión de hoy, sin aprobar, 1 de la del 5/10)",
                count.summary());
    }

    @Test
    void aJudgeApprovedWithAnotherMotorSaysWithWhichAndIsNotSinAprobar() {
        var standing = new JudgeExamStanding(JUDGE, V_GEMINI, JudgeExamStatus.NOT_TAKEN, false,
                "La versión vigente del juez no rindió el examen fijo.", MEDIUM, Set.of(V_GEMINI));
        var enabled = List.of(new JudgeEnablement(GEMINI, JudgeExamStatus.APPROVED, Instant.parse("2026-10-19T14:30:58Z")));
        standing.setEnabledWith(enabled);
        var history = new java.util.ArrayList<>(history());
        history.add(record(V_GEMINI, JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        var tally = new JudgeFreshnessTally(JudgeFreshnessReference.of(standing, history, ZONE, CLOCK),
                "miniteoría", AuditTarget.KNOWLEDGE);
        tally.add(V_GEMINI, GEMINI, 1);
        tally.add(V_5_10, XHIGH, 82);
        tally.add(V_GEMINI, XHIGH, 2);
        var count = tally.count();
        assertTrue(count.approved());
        assertEquals(enabled, count.enabledWith());
        assertEquals(1, count.upToDate());
        // F-ALDIA-R003 inv. 7: the version of 5/10 approved with that motor, so the 82 are of an earlier
        // version; today's version with a motor it did not approve with is another motor.
        assertEquals("miniteoría: reusa 84 temas de antes (82 de la del 5/10, 2 de gpt-6.1-sol en codex-cli, xhigh)",
                count.summary());
        assertEquals("la del 19/10", count.currentVersionName());
    }
}
