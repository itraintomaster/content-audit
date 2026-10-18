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
    void theRuntimeIsLookedAtBeforeTheVersion() {
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), today().mark(V_5_10, LOCAL));
        assertEquals(JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME), today().mark(V_TODAY, LOCAL));
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
