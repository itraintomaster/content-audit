package com.learney.contentaudit.auditdomain.judgingengine;

import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.judging.*;
import com.learney.contentaudit.evaluationledgerdomain.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JudgeExamExceptionRuntimeTest {
    private final Instant measuredAt = Instant.parse("2026-10-22T19:00:00Z");
    private final EvaluationRuntimeIdentity motor =
            EvaluationRuntimeIdentity.explicit("codex-cli", "test-model", "medium");
    private final JudgeRegressionSuite suite = new JudgeRegressionSuite("suite-1", List.of(
            new JudgeRegressionCase("REG-watch", "mini-theory", "PRB-test", "Resaltado",
                    "watch", "quiz-watch", false, "Resaltado completo", "Referencia editorial",
                    Map.of(), Instant.EPOCH),
            new JudgeRegressionCase("REG-other", "mini-theory", "PRB-test", "Resaltado",
                    "other", "quiz-other", true, "Falta resaltado", "Referencia editorial",
                    Map.of(), Instant.EPOCH)));
    private final JudgeExamRegistry exams = mock(JudgeExamRegistry.class);
    private final JudgePromotionRegistry promotions = mock(JudgePromotionRegistry.class);

    private JudgeExamRecord measured() {
        var result = new JudgeExamRecord("mini-theory", "v1", JudgeExamStatus.FAILED,
                62, 0, List.of(), List.of("quiz-watch"), false,
                "photo", "fixed-meter", measuredAt);
        result.setRuntimeIdentity(motor);
        result.setExamFingerprint("exam-1");
        result.setRegressionSuiteFingerprint(suite.fingerprint());
        result.setRegressionCaseIds(List.of("REG-watch", "REG-other"));
        result.setRegressionMismatchedCaseIds(List.of("REG-watch"));
        return result;
    }

    private JudgeExamException exception() {
        return new JudgeExamException("exam-1", "suite-1", measuredAt,
                List.of("REG-watch"), List.of("quiz-watch"));
    }

    private JudgePromotionRecord promotion(Instant at) {
        return new JudgePromotionRecord("mini-theory", "v1", motor, "José",
                "Acepta únicamente la discrepancia de watch; conserva 61/62",
                "approval.json", "a".repeat(64), at, exception());
    }

    private DefaultJudgeRuntime runtime(JudgeExamRecord measured, JudgePromotionRecord promotion,
            JudgeRegressionSuite currentSuite) {
        when(exams.latestCompatible(anyString(), any(), any())).thenReturn(Optional.ofNullable(measured));
        when(promotions.latest(anyString(), anyString(), any())).thenReturn(Optional.ofNullable(promotion));
        return new DefaultJudgeRuntime(null, null, exams, null, null, promotions, ignored -> currentSuite);
    }

    @Test void exactHumanAcceptanceEnablesCourseWithoutAlteringOrRerunningTheFailedExam() {
        var record = measured();
        var before = measured();
        var decision = promotion(measuredAt.plusSeconds(1));
        var runtime = runtime(record, decision, suite);
        var standing = runtime.standing("mini-theory", "v1", motor);

        assertEquals(JudgeExamStatus.PROMOTED, standing.getStatus());
        assertTrue(standing.isConsultsOnCourse());
        assertTrue(standing.getReason().contains("Aprobado con excepción"));
        assertTrue(standing.getReason().contains("watch"));
        assertTrue(standing.getReason().contains("61/62"));
        assertEquals(before, record);
        verify(exams, never()).record(any());
        verify(promotions, never()).record(any());
    }

    @Test void acceptedExceptionStillRequiresAnExplicitCourseBudgetAndDoesNotEvaluateAnything() {
        var record = measured();
        runtime(record, promotion(measuredAt.plusSeconds(1)), suite);
        var courses = mock(EvaluationSessionFactory.class);
        when(courses.open(any(), any())).thenReturn(mock(EvaluationSession.class));
        var config = mock(JudgeRuntimeConfig.class);
        when(config.getMaxConcurrentEvaluations()).thenReturn(1);
        var runtime = new DefaultJudgeRuntime(courses, mock(EvaluationSessionFactory.class),
                exams, mock(EvaluationTimeline.class), config, promotions, ignored -> suite);
        var evaluator = mock(Evaluator.class);
        when(evaluator.evaluatorId()).thenReturn("mini-theory-judge");
        when(evaluator.evaluatorVersion()).thenReturn(Optional.of("v1"));
        when(evaluator.runtimeIdentity()).thenReturn(motor);

        var opened = runtime.open("mini-theory", List.of(evaluator),
                new EvaluationRunPolicy(3, false, null, null, EvaluationRunPurpose.COURSE, 1));
        assertEquals(JudgeExamStatus.PROMOTED, opened.getStanding().getStatus());
        assertEquals(3, opened.getEffectivePolicy().getMaxNewEvaluations());
        assertEquals(0, runtime.open("mini-theory", List.of(evaluator), null)
                .getEffectivePolicy().getMaxNewEvaluations());
        assertEquals(0, runtime.open("mini-theory", List.of(evaluator),
                new EvaluationRunPolicy(3, false, null, null, EvaluationRunPurpose.READING, 1))
                .getEffectivePolicy().getMaxNewEvaluations());
        verify(evaluator, never()).evaluate(any());
        verify(exams, never()).record(any());
    }

    @Test void changedDiscrepancySetsCannotBorrowTheAcceptance() {
        List<Consumer<JudgeExamRecord>> changes = List.of(
                r -> r.setRegressionMismatchedCaseIds(List.of()),
                r -> r.setRegressionMismatchedCaseIds(List.of("REG-watch", "REG-other")),
                r -> r.setRegressionMismatchedCaseIds(List.of("REG-other")),
                r -> r.setMismatchedCaseIds(List.of()),
                r -> r.setMismatchedCaseIds(List.of("quiz-watch", "quiz-other")),
                r -> r.setMismatchedCaseIds(List.of("quiz-other")),
                r -> r.setMismatchedCaseIds(null));
        for (var change : changes) {
            var record = measured();
            change.accept(record);
            assertFalse(promotion(measuredAt.plusSeconds(1)).coversExamException(record, suite));
            assertFalse(runtime(record, promotion(measuredAt.plusSeconds(1)), suite)
                    .standing("mini-theory", "v1", motor).isConsultsOnCourse());
        }
    }

    @Test void newMeasurementVersionRuntimeExamOrIncompleteCoverageInvalidatesTheException() {
        List<Consumer<JudgeExamRecord>> changes = List.of(
                r -> r.setRecordedAt(measuredAt.plusSeconds(1)),
                r -> r.setRecordedAt(null),
                r -> r.setJudgeName("other-judge"),
                r -> r.setJudgeVersion("v2"),
                r -> r.setRuntimeIdentity(EvaluationRuntimeIdentity.explicit("codex-cli", "other", "medium")),
                r -> r.setRuntimeIdentity(EvaluationRuntimeIdentity.legacyUnknown()),
                r -> r.setExamFingerprint("exam-2"),
                r -> r.setRegressionSuiteFingerprint("suite-2"),
                r -> r.setRegressionCaseIds(List.of("REG-watch")),
                r -> r.setCasesNotJudged(1));
        for (var change : changes) {
            var record = measured();
            change.accept(record);
            assertFalse(promotion(measuredAt.plusSeconds(5)).coversExamException(record, suite));
            assertFalse(runtime(record, promotion(measuredAt.plusSeconds(5)), suite)
                    .standing("mini-theory", "v1", motor).isConsultsOnCourse());
        }
    }

    @Test void anUnmeasuredExamCannotBeAcceptedAndBothTerminalStatusesRetainTheirMeaning() {
        for (var status : List.of(JudgeExamStatus.TAKEN, JudgeExamStatus.NOT_TAKEN,
                JudgeExamStatus.PROMOTED, JudgeExamStatus.NO_VERSION)) {
            var record = measured();
            record.setStatus(status);
            assertFalse(promotion(measuredAt.plusSeconds(1)).coversExamException(record, suite));
            assertFalse(runtime(record, promotion(measuredAt.plusSeconds(1)), suite)
                    .standing("mini-theory", "v1", motor).isConsultsOnCourse());
        }
        var approved = measured();
        approved.setStatus(JudgeExamStatus.APPROVED);
        assertTrue(promotion(measuredAt.plusSeconds(1)).coversExamException(approved, suite));
    }

    @Test void impossibleJudgedCountsCannotAuthorizeAnException() {
        var decision = promotion(measuredAt.plusSeconds(1));
        for (int count : List.of(-1, 0, 1)) {
            var record = measured();
            record.setCasesJudged(count);
            assertFalse(decision.coversExamException(record, suite));
            assertFalse(runtime(record, decision, suite)
                    .standing("mini-theory", "v1", motor).isConsultsOnCourse());
        }

        var fewerThanDiscrepancies = measured();
        fewerThanDiscrepancies.setCasesJudged(2);
        fewerThanDiscrepancies.setMismatchedCaseIds(List.of("quiz-watch", "quiz-2", "quiz-3"));
        var threeDiscrepancies = new JudgeExamException("exam-1", "suite-1", measuredAt,
                List.of("REG-watch"), fewerThanDiscrepancies.getMismatchedCaseIds());
        assertFalse(threeDiscrepancies.matches(fewerThanDiscrepancies, suite));

        var fewerThanDeclaredCoverage = measured();
        fewerThanDeclaredCoverage.setCasesJudged(2);
        fewerThanDeclaredCoverage.setRegressionCaseIds(List.of("REG-watch", "REG-other", "REG-third"));
        assertFalse(decision.coversExamException(fewerThanDeclaredCoverage, suite));
    }

    @Test void acceptanceMustFollowTheExactMeasurementAndCannotBeInheritedByAliases() {
        assertFalse(promotion(measuredAt).coversExamException(measured(), suite));
        assertFalse(promotion(measuredAt.minusSeconds(1)).coversExamException(measured(), suite));
        var runtime = runtime(measured(), promotion(measuredAt.plusSeconds(1)), suite);
        assertFalse(runtime.standing("mini-theory", "v2", motor, Set.of("v1", "v2")).isConsultsOnCourse());
        assertFalse(runtime.standing("other-judge", "v1", motor).isConsultsOnCourse());
        assertFalse(runtime.standing("mini-theory", "v1",
                EvaluationRuntimeIdentity.explicit("codex-cli", "other", "medium")).isConsultsOnCourse());
    }

    @Test void changedSuiteRequiresAnExamAndAnInvalidExceptionNeverBecomesAGenericPromotion() {
        var changed = new JudgeRegressionSuite("suite-2", suite.cases());
        var runtime = runtime(measured(), promotion(measuredAt.plusSeconds(1)), changed);
        assertEquals(JudgeExamStatus.NOT_TAKEN, runtime.standing("mini-theory", "v1", motor).getStatus());
        assertFalse(runtime(measured(), promotion(measuredAt.plusSeconds(1)), JudgeRegressionSuite.empty())
                .standing("mini-theory", "v1", motor).isConsultsOnCourse());
        var legacy = new JudgePromotionRecord("mini-theory", "v1", motor, "José", "Piloto",
                "approval.json", "a".repeat(64), measuredAt.plusSeconds(1));
        assertFalse(runtime(measured(), legacy, suite).standing("mini-theory", "v1", motor).isConsultsOnCourse());
    }

    private JudgeExamRecord historicalMeasurement() {
        var record = new JudgeExamRecord("single-answer", "v-historical", JudgeExamStatus.FAILED,
                63, 0, List.of(), List.of("quiz-a", "quiz-b"), false,
                "photo-historical", "frozen-meter", measuredAt);
        record.setRuntimeIdentity(motor);
        record.setExamFingerprint("exam-historical");
        record.setRegressionSuiteFingerprint(JudgeRegressionSuite.empty().fingerprint());
        record.setRegressionCaseIds(List.of());
        record.setRegressionMismatchedCaseIds(List.of());
        return record;
    }

    private JudgePromotionRecord historicalAcceptance() {
        var exception = new JudgeExamException("exam-historical", JudgeRegressionSuite.empty().fingerprint(),
                measuredAt, List.of(), List.of("quiz-b", "quiz-a"));
        return new JudgePromotionRecord("single-answer", "v-historical", motor, "Reviewer",
                "Accept precisely these historical discrepancies", "approval.json", "a".repeat(64),
                measuredAt.plusSeconds(1), exception);
    }

    @Test void exactHistoricalDiscrepanciesCanBeAcceptedWhenThePermanentSuiteIsEmpty() {
        var record = historicalMeasurement();
        var decision = historicalAcceptance();
        assertEquals(List.of(), decision.examException().acceptedRegressionCaseIds());
        assertTrue(decision.coversExamException(record, JudgeRegressionSuite.empty()));
        var standing = runtime(record, decision, JudgeRegressionSuite.empty())
                .standing("single-answer", "v-historical", motor);
        assertTrue(standing.isConsultsOnCourse());
        assertEquals(JudgeExamStatus.PROMOTED, standing.getStatus());
        assertEquals(JudgeExamStatus.FAILED, record.getStatus());
        assertFalse(record.isGoalMet());
        assertEquals(List.of("quiz-a", "quiz-b"), record.getMismatchedCaseIds());
        verify(exams, never()).record(any());
        verify(promotions, never()).record(any());
    }

    @Test void historicalAcceptanceCannotBorrowAnotherMeasurementOrAnUnevaluatedExam() {
        List<Consumer<JudgeExamRecord>> changes = List.of(
                r -> r.setStatus(JudgeExamStatus.TAKEN),
                r -> r.setCasesNotJudged(1),
                r -> r.setCasesJudged(0),
                r -> r.setMismatchedCaseIds(List.of("quiz-a")),
                r -> r.setMismatchedCaseIds(List.of("quiz-a", "quiz-b", "quiz-c")),
                r -> r.setMismatchedCaseIds(List.of()),
                r -> r.setRecordedAt(measuredAt.plusSeconds(1)),
                r -> r.setExamFingerprint("another-exam"),
                r -> r.setRegressionCaseIds(List.of("REG-new")),
                r -> r.setRegressionMismatchedCaseIds(List.of("REG-new")),
                r -> r.setRuntimeIdentity(EvaluationRuntimeIdentity.explicit("codex-cli", "other", "medium")));
        for (var change : changes) {
            var record = historicalMeasurement();
            change.accept(record);
            assertFalse(historicalAcceptance().coversExamException(record, JudgeRegressionSuite.empty()));
            assertFalse(runtime(record, historicalAcceptance(), JudgeRegressionSuite.empty())
                    .standing("single-answer", "v-historical", motor).isConsultsOnCourse());
        }
        assertFalse(historicalAcceptance().coversExamException(null, JudgeRegressionSuite.empty()));
        assertFalse(historicalAcceptance().coversExamException(historicalMeasurement(), suite));
    }

    @Test void anEmptyAcceptedRegressionSetCannotWaiveAPermanentSuiteEvenWhenItsOtherFieldsMatch() {
        var record = measured();
        record.setRegressionMismatchedCaseIds(List.of());
        var historicalOnly = new JudgeExamException("exam-1", "suite-1", measuredAt,
                List.of(), List.of("quiz-watch"));
        assertFalse(historicalOnly.matches(record, suite));
    }

    @Test void exceptionListsAreImmutableSortedUniqueAndRequireExplicitHistoricalDiscrepancies() {
        var original = new ArrayList<>(List.of(" REG-watch ", "REG-other"));
        var exception = new JudgeExamException("exam-1", "suite-1", measuredAt,
                original, List.of("quiz-watch"));
        original.clear();
        assertEquals(List.of("REG-other", "REG-watch"), exception.acceptedRegressionCaseIds());
        assertThrows(UnsupportedOperationException.class, () -> exception.acceptedRegressionCaseIds().clear());
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", "suite-1",
                measuredAt, null, List.of("quiz-watch")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", "suite-1",
                measuredAt, List.of("REG-watch"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", "suite-1",
                measuredAt, List.of("REG-watch", " REG-watch "), List.of("quiz-watch")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", "suite-1",
                measuredAt, List.of("REG-watch"), List.of(" ")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", null,
                measuredAt, List.of("REG-watch"), List.of("quiz-watch")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", " ",
                measuredAt, List.of(), List.of("quiz-watch")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", null,
                measuredAt, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException(" ", "suite-1",
                measuredAt, List.of("REG-watch"), List.of("quiz-watch")));
        assertThrows(IllegalArgumentException.class, () -> new JudgeExamException("exam-1", "suite-1",
                null, List.of("REG-watch"), List.of("quiz-watch")));
    }
}
