package com.learney.contentaudit.auditdomain.judgingengine;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgeRegressionStandingTest {
    final EvaluationRuntimeIdentity motor = EvaluationRuntimeIdentity.explicit("codex-cli", "test-model", "medium");
    final JudgeRegressionSuite suite = new JudgeRegressionSuite("suite-1", List.of(new JudgeRegressionCase("REG-1", "mini-theory", "PRB-test", "Criterio", "case-1", "quiz-1", true, "It lleva is", "Operador", Map.of(), Instant.EPOCH)));
    JudgeExamRecord approved() {
        var record = new JudgeExamRecord("mini-theory", "v1", JudgeExamStatus.APPROVED, 40, 0, List.of(), List.of(), true, "photo", "editor", Instant.EPOCH);
        record.setRuntimeIdentity(motor); return record;
    }
    DefaultJudgeRuntime runtime(JudgeExamRecord record) {
        var registry = mock(JudgeExamRegistry.class);
        when(registry.latestCompatible("mini-theory", Set.of("v1"), motor)).thenReturn(Optional.of(record));
        return new DefaultJudgeRuntime(null, null, registry, null, null, null, ignored -> suite);
    }
    @Test void anOldApprovalCannotEnableAJudgeAfterTheExamGrows() {
        var record = approved();
        var standing = runtime(record).standing("mini-theory", "v1", motor);
        assertFalse(standing.isConsultsOnCourse()); assertEquals(JudgeExamStatus.NOT_TAKEN, standing.getStatus());
        assertTrue(standing.getReason().contains("examen ampliado"));
        assertTrue(standing.getReason().contains("PRB-test"));
        assertTrue(standing.getReason().contains("Criterio"));
    }
    @Test void aPreviousOperatorPromotionCannotBypassTheExpandedExam() {
        var registry = mock(JudgeExamRegistry.class);
        when(registry.latestCompatible("mini-theory", Set.of("v1"), motor)).thenReturn(Optional.of(approved()));
        var promotions = mock(JudgePromotionRegistry.class);
        when(promotions.latest("mini-theory", "v1", motor)).thenReturn(Optional.of(new JudgePromotionRecord(
                "mini-theory", "v1", motor, "Operador", "Revisado", "evidence.json", "a".repeat(64), Instant.now())));
        var runtime = new DefaultJudgeRuntime(null, null, registry, null, null, promotions, ignored -> suite);
        assertFalse(runtime.standing("mini-theory", "v1", motor).isConsultsOnCourse());
    }
    @Test void anOldPromotionCannotEnableTheExpandedSuiteAfterTakingItWithoutMeasuring() {
        var taken = approved(); taken.setStatus(JudgeExamStatus.TAKEN);
        taken.setRegressionSuiteFingerprint("suite-1"); taken.setRegressionCaseIds(List.of("REG-1"));
        var registry = mock(JudgeExamRegistry.class);
        when(registry.latestCompatible("mini-theory", Set.of("v1"), motor)).thenReturn(Optional.of(taken));
        var promotions = mock(JudgePromotionRegistry.class);
        when(promotions.latest("mini-theory", "v1", motor)).thenReturn(Optional.of(new JudgePromotionRecord(
                "mini-theory", "v1", motor, "Operador", "Revisado antes de ampliar", "evidence.json", "a".repeat(64), Instant.EPOCH)));
        var runtime = new DefaultJudgeRuntime(null, null, registry, null, null, promotions, ignored -> suite);
        var standing = runtime.standing("mini-theory", "v1", motor);
        assertEquals(JudgeExamStatus.TAKEN, standing.getStatus()); assertFalse(standing.isConsultsOnCourse());
    }
    @Test void overviewGroupsReferencesByProblemInsteadOfRepeatingEveryCaseIdentifier() {
        var other = new JudgeRegressionCase("REG-2", "mini-theory", "PRB-test", "Criterio", "case-2", "quiz-2", true,
                "They lleva are", "Operador", Map.of(), Instant.EPOCH);
        var expanded = new JudgeRegressionSuite("suite-2", List.of(suite.cases().get(0), other));
        var registry = mock(JudgeExamRegistry.class);
        when(registry.latestCompatible("mini-theory", Set.of("v1"), motor)).thenReturn(Optional.of(approved()));
        var runtime = new DefaultJudgeRuntime(null, null, registry, null, null, null, ignored -> expanded);
        String reason = runtime.standing("mini-theory", "v1", motor).getReason();
        assertTrue(reason.contains("PRB-test «Criterio» (2 casos)"));
        assertFalse(reason.contains("case-1")); assertFalse(reason.contains("case-2"));
    }
    @Test void onlyACompleteCurrentSuiteApprovalEnablesTheJudgeAndOneRegressionFailureBlocksIt() {
        var record = approved(); record.setRegressionSuiteFingerprint("suite-1");
        assertFalse(runtime(record).standing("mini-theory", "v1", motor).isConsultsOnCourse());
        record.setRegressionCaseIds(List.of("REG-1"));
        assertTrue(runtime(record).standing("mini-theory", "v1", motor).isConsultsOnCourse());
        record.setRegressionMismatchedCaseIds(List.of("REG-1"));
        var standing = runtime(record).standing("mini-theory", "v1", motor);
        assertFalse(standing.isConsultsOnCourse()); assertEquals(JudgeExamStatus.FAILED, standing.getStatus());
        assertTrue(standing.getReason().contains("PRB-test"));
        assertTrue(standing.getReason().contains("1 caso"));
    }
}
