package com.learney.contentaudit.auditdomain.judging;

import static com.learney.contentaudit.auditdomain.judging.JudgeFreshnessFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** F-MEDJ-R002: which verdict is today's judge, for offering a loose run in the map. */
@Tag("FEAT-MEDJ")
@Tag("F-MEDJ-R002")
class JudgeOfTodayTest {
    @Test
    void anApprovedJudgeIsTodaysOnlyWhereItsVerdictIsUpToDate() {
        var reference = today();
        assertTrue(reference.isOfToday(V_TODAY, XHIGH));
        assertFalse(reference.isOfToday(V_5_10, XHIGH));
        assertFalse(reference.isOfToday(V_TODAY, LOCAL));
        assertFalse(reference.isOfToday(V_TODAY, null));
        assertFalse(reference.isOfToday(V_TODAY, EvaluationRuntimeIdentity.legacyUnknown()));
    }

    @Test
    void aVersionOfTodayThatDidNotApproveIsStillTodaysByItsVersionAlone() {
        var reference = JudgeFreshnessReference.of(standing(V_UNAPPROVED, JudgeExamStatus.FAILED, Set.of(V_UNAPPROVED)),
                history(), ZONE, CLOCK);
        assertFalse(reference.isApproved());
        assertTrue(reference.isOfToday(V_UNAPPROVED, LOCAL));
        assertTrue(reference.isOfToday(V_UNAPPROVED, null));
        assertFalse(reference.isOfToday(V_TODAY, XHIGH));
        assertFalse(reference.isOfToday(null, XHIGH));
    }

    @Test
    void aVersionRecordedAsTheSameIsAlsoTodays() {
        String afterCutover = "e9f0a1b2eeeeeeee";
        var reference = JudgeFreshnessReference.of(standing(afterCutover, JudgeExamStatus.FAILED,
                Set.of(afterCutover, V_TODAY)), history(), ZONE, CLOCK);
        assertTrue(reference.isOfToday(V_TODAY, LOCAL));
        assertTrue(reference.isOfToday(afterCutover, LOCAL));
    }
}
