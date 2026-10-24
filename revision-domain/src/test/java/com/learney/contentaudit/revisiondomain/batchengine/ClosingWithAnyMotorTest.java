package com.learney.contentaudit.revisiondomain.batchengine;

import static com.learney.contentaudit.revisiondomain.batchengine.JudgePassBench.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.revisiondomain.batch.BatchCloseRequest;
import com.learney.contentaudit.revisiondomain.batch.BatchJudgeAccount;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * FEAT-CMOTOR: the closing pays each judge with a runtime it is enabled with, says which in its account and in the
 * estimate, and shares the cap the same with two runtimes. The judges are the bench's: nothing is consulted for real.
 */
@Tag("FEAT-CMOTOR")
class ClosingWithAnyMotorTest {
    static final EvaluationRuntimeIdentity GEMINI = EvaluationRuntimeIdentity.explicit("agy", "gemini-3.8-flash-high",
            "high");

    @Test
    @Tag("F-CMOTOR-R001")
    @Tag("F-CMOTOR-R003")
    @DisplayName("should pay in the closing a judge enabled only with Gemini with Gemini, and say so in its account, while the others go with the profile's")
    void theClosingPaysAJudgeEnabledOnlyWithGeminiAndItsAccountSaysSo() {
        JudgePassBench bench = new JudgePassBench();
        bench.enabledOnlyWith(MINI_THEORY, GEMINI);
        CourseEntity start = JudgePassBench.largeCourse(10, 2, false);
        CourseEntity composed = JudgePassBench.largeCourse(10, 2, true);
        CorrectionBatch batch = JudgePassBench.batch(List.of(TRANSLATION_SENSE, MINI_THEORY),
                JudgePassBench.largeExerciseIds(10, 2));

        List<BatchJudgeAccount> accounts = bench.pass().judge(batch, start, composed,
                new BatchCloseRequest(Map.of(TRANSLATION_SENSE, 1000, MINI_THEORY, 1000), null));

        BatchJudgeAccount theory = account(accounts, MINI_THEORY);
        assertTrue(theory.isApproved(), "enabled with Gemini, it is not «sin aprobar»");
        assertEquals(20, theory.getQueriesSpent(), "ten topics, before and after");
        assertEquals(20, bench.consulted(MINI_THEORY).size());
        assertEquals(GEMINI, theory.getRuntime());
        assertTrue(theory.isOtherRuntime(), "it is not the profile's runtime, and the account says so");
        BatchJudgeAccount translation = account(accounts, TRANSLATION_SENSE);
        assertEquals(80, translation.getQueriesSpent());
        assertEquals(PROFILE, translation.getRuntime(), "translation-sense goes with the profile's runtime");
        assertFalse(translation.isOtherRuntime());
        assertTrue(bench.runs.stream().allMatch(run -> run.selection().getPolicies().values().stream()
                        .allMatch(policy -> policy.isAnyEnabledRuntime())),
                "every run of the closing asks for a runtime each judge is enabled with");
    }

    @Test
    @Tag("F-CMOTOR-R003")
    @DisplayName("should say in the estimate the runtime of a judge enabled only with Gemini and count what it misses, consulting nothing")
    void theEstimateSaysTheRuntimeOfEachJudge() {
        JudgePassBench bench = new JudgePassBench();
        bench.enabledOnlyWith(MINI_THEORY, GEMINI);
        CourseEntity start = JudgePassBench.largeCourse(10, 2, false);
        CourseEntity composed = JudgePassBench.largeCourse(10, 2, true);
        CorrectionBatch batch = JudgePassBench.batch(List.of(MINI_THEORY), JudgePassBench.largeExerciseIds(10, 2));

        List<BatchJudgeAccount> accounts = bench.pass().estimate(batch, start, composed);

        BatchJudgeAccount theory = account(accounts, MINI_THEORY);
        assertTrue(theory.isApproved());
        assertEquals(20, theory.getQueriesMissing());
        assertEquals(GEMINI, theory.getRuntime());
        assertTrue(theory.isOtherRuntime());
        assertEquals(0, bench.queries.size(), "estimating consults nothing");
    }

    @Test
    @Tag("F-CMOTOR-R002")
    @DisplayName("should not pay with its budget a judge whose version failed and has no other runtime, and keep it «sin aprobar» with its reason")
    void aJudgeWithNoRuntimeItCanUseIsNotPaid() {
        JudgePassBench bench = new JudgePassBench();
        bench.withoutApprovedExam(MINI_THEORY, JudgeExamStatus.FAILED, "reprobado");
        CourseEntity start = JudgePassBench.largeCourse(10, 2, false);
        CourseEntity composed = JudgePassBench.largeCourse(10, 2, true);
        CorrectionBatch batch = JudgePassBench.batch(List.of(MINI_THEORY), JudgePassBench.largeExerciseIds(10, 2));

        List<BatchJudgeAccount> accounts = bench.pass().judge(batch, start, composed,
                new BatchCloseRequest(Map.of(MINI_THEORY, 1000), null));

        BatchJudgeAccount theory = account(accounts, MINI_THEORY);
        assertFalse(theory.isApproved());
        assertEquals("reprobado", theory.getReason());
        assertEquals(0, theory.getQueriesSpent());
        assertEquals(List.of(), bench.consulted(MINI_THEORY));
        assertFalse(theory.isOtherRuntime(), "it stays on the profile's runtime, which consults nothing");
    }

    @Test
    @Tag("F-CMOTOR-R005")
    @DisplayName("should share --cap 900 between translation-sense with the profile's runtime and mini-theory with Gemini, never passing it")
    void theCapIsSharedByJudgesOnTwoRuntimes() {
        JudgePassBench bench = new JudgePassBench();
        bench.enabledOnlyWith(MINI_THEORY, GEMINI);
        CourseEntity start = JudgePassBench.largeCourse(100, 2, false);
        CourseEntity composed = JudgePassBench.largeCourse(100, 2, true);
        CorrectionBatch batch = JudgePassBench.batch(List.of(TRANSLATION_SENSE, MINI_THEORY),
                JudgePassBench.largeExerciseIds(100, 2));

        List<BatchJudgeAccount> accounts = bench.pass().judge(batch, start, composed,
                new BatchCloseRequest(Map.of(), 900));

        BatchJudgeAccount translation = account(accounts, TRANSLATION_SENSE);
        assertEquals(800, translation.getQueriesSpent(), "first in the catalog, it pays all it needs");
        BatchJudgeAccount theory = account(accounts, MINI_THEORY);
        assertEquals(Integer.valueOf(100), theory.getBudget(), "what translation-sense left of the cap");
        assertEquals(100, theory.getQueriesSpent(), "with Gemini, from the same cap");
        assertEquals(100, theory.getQueriesMissing());
        assertEquals(900, bench.queries.size(), "two runtimes, one cap, never passed");
    }

    private static BatchJudgeAccount account(List<BatchJudgeAccount> accounts, String judge) {
        return accounts.stream().filter(account -> judge.equals(account.getJudge())).findFirst()
                .orElseThrow(() -> new AssertionError("no account for " + judge));
    }
}
