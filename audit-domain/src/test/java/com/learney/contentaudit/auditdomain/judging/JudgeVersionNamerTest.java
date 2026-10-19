package com.learney.contentaudit.auditdomain.judging;

import static com.learney.contentaudit.auditdomain.judging.JudgeFreshnessFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FEAT-ALDIA")
@Tag("F-ALDIA-R004")
class JudgeVersionNamerTest {
    final JudgeVersionNamer namer = new JudgeVersionNamer(history(), List.of(), ZONE, CLOCK);

    @Test
    void aVersionIsNamedByTheLocalDayItFirstApproved() {
        // 2026-10-06T02:14Z is still the 5th in Buenos Aires.
        assertEquals("la del 5/10", namer.name(V_5_10));
        assertEquals(Instant.parse("2026-10-06T02:14:16Z"), namer.firstApproval(V_5_10).orElseThrow());
    }

    @Test
    void theHourJoinsWhenAnotherVersionFirstApprovedTheSameLocalDay() {
        assertEquals("la del 13/10, 13:00", namer.name(V_13_10_MORNING));
        assertEquals("la del 13/10, 22:12", namer.name(V_TODAY));
    }

    @Test
    void reApprovingDoesNotRenameAVersion() {
        assertEquals(Instant.parse("2026-10-14T01:12:33Z"), namer.firstApproval(V_TODAY).orElseThrow());
        assertEquals("la del 13/10, 22:12", namer.name(V_TODAY));
    }

    @Test
    void aPromotedVersionIsNamedByThePromotionWhenItCameBeforeItsFirstApproval() {
        // inv. 7: Naturalidad never approved its exam; José promoted it on the 14th.
        var promoted = new JudgeVersionNamer(history(), List.of(), java.util.Map.of(V_UNAPPROVED, PROMOTED_AT), ZONE, CLOCK);
        assertEquals("la del 14/10", promoted.name(V_UNAPPROVED));
        assertTrue(promoted.approved(V_UNAPPROVED));
        // A promotion after the first approval does not rename it.
        var late = new JudgeVersionNamer(history(), List.of(), java.util.Map.of(V_5_10, PROMOTED_AT), ZONE, CLOCK);
        assertEquals("la del 5/10", late.name(V_5_10));
    }

    @Test
    void theYearJoinsWhenItIsNotTheCurrentOne() {
        var old = new JudgeVersionNamer(List.of(record("aaaa1111", JudgeExamStatus.APPROVED,
                "2025-12-20T15:00:00Z", XHIGH)), List.of(), ZONE, CLOCK);
        assertEquals("la del 20/12/2025", old.name("aaaa1111"));
    }

    @Test
    void aVersionThatNeverApprovedIsNamedByItsShortCode() {
        assertEquals("sin aprobar (c8ca16c9)", namer.name(V_UNAPPROVED));
        assertEquals("sin aprobar (c8ca16c9)", namer.looseName(V_UNAPPROVED));
        assertFalse(namer.approved(V_UNAPPROVED));
        assertEquals("1234abcd+5678efab", JudgeVersionNamer.shortCode("1234abcdXXXX+5678efabYYYY"));
    }

    @Test
    void aLooseMarkSaysVersionOf() {
        assertEquals("versión del 5/10", namer.looseName(V_5_10));
        assertEquals("versión del 13/10, 13:00", namer.looseName(V_13_10_MORNING));
    }

    @Test
    void anotherRuntimeIsNamedByItsRuntimeAndNoProvenanceIsSaid() {
        assertEquals("llama3 local", JudgeVersionNamer.runtimeName(LOCAL));
        assertEquals("gpt-6.1-sol en codex-cli, xhigh", JudgeVersionNamer.runtimeName(XHIGH));
        assertEquals("procedencia desconocida", JudgeVersionNamer.runtimeName(null));
        assertEquals("procedencia desconocida", JudgeVersionNamer.runtimeName(EvaluationRuntimeIdentity.legacyUnknown()));
    }

    @Test
    void versionsThatAreTheSameShareTheirFirstApproval() {
        var same = new JudgeVersionNamer(history(), List.of(Set.of("e9f0a1b2", V_TODAY)), ZONE, CLOCK);
        assertEquals("la del 13/10, 22:12", same.name("e9f0a1b2"));
    }

    @Test
    void aVersionIsNeverNamedWithANumber() {
        for (String version : List.of(V_5_10, V_13_10_MORNING, V_TODAY)) {
            assertFalse(namer.name(version).matches(".*\\b(v|versión )\\d+\\b.*"), namer.name(version));
            assertFalse(namer.name(version).contains(version.substring(0, 8)), namer.name(version));
        }
    }
}
