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
}
