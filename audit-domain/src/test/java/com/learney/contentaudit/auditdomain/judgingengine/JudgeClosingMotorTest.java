package com.learney.contentaudit.auditdomain.judgingengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgeFreshnessReference;
import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeSessions;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationTimeline;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * F-CMOTOR-R001, R002, R004: the closing consults each judge with the profile's runtime when its version of today is
 * enabled with it, and if not with the one it was enabled with most recently that it can be moved to; reading for free
 * follows the same runtime. Every judge is a fake: nothing is consulted.
 */
@Tag("FEAT-CMOTOR")
class JudgeClosingMotorTest {
    static final String JUDGE = "mini-theory";
    static final EvaluationRuntimeIdentity CHATGPT = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    static final EvaluationRuntimeIdentity GEMINI = EvaluationRuntimeIdentity.explicit("agy", "gemini-3.8-flash-high", "high");
    static final EvaluationRuntimeIdentity CLAUDE = EvaluationRuntimeIdentity.explicit("claude-cli", "opus", "high");

    final List<JudgeExamRecord> exams = new ArrayList<>();
    final List<JudgePromotionRecord> promotions = new ArrayList<>();
    final EvaluationSessionFactory courses = mock(EvaluationSessionFactory.class);

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should consult in the closing a judge enabled only with another runtime with that runtime, and remember the profile's")
    void theClosingConsultsAJudgeEnabledOnlyWithAnotherRuntimeWithThatRuntime() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator gemini = judge("v1", GEMINI);
        Evaluator own = judge("v1", CHATGPT);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(gemini));

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(200));

        assertTrue(opened.getStanding().isConsultsOnCourse());
        assertEquals(JudgeExamStatus.APPROVED, opened.getStanding().getStatus());
        assertEquals(GEMINI, opened.getStanding().getRuntimeIdentity());
        assertEquals(GEMINI, opened.getRuntimeIdentity());
        assertEquals(CHATGPT, opened.getStanding().getOwnRuntimeIdentity());
        assertEquals(200, opened.getEffectivePolicy().getMaxNewEvaluations());
        assertEquals("v1", opened.getJudgeVersion());
        ArgumentCaptor<Evaluator> consulted = ArgumentCaptor.forClass(Evaluator.class);
        verify(courses).open(eq(new EvaluationBudget(200)), consulted.capture());
        assertSame(gemini, consulted.getValue(), "the session consults the judge built with Gemini");
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should keep the profile's runtime when the version is enabled with it, even if another was enabled after")
    void theProfileRuntimeWinsWhenTheVersionIsEnabledWithIt() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T15:29:00Z", CHATGPT));
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(200));

        assertEquals(CHATGPT, opened.getRuntimeIdentity());
        assertNull(opened.getStanding().getOwnRuntimeIdentity());
        assertEquals(200, opened.getEffectivePolicy().getMaxNewEvaluations());
        verify(own, never()).withRuntime(any());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should take, among the other runtimes, the one the version was enabled with most recently")
    void amongTheOtherRuntimesTheMostRecentlyEnabledGoesFirst() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T15:29:00Z", CLAUDE));
        promotions.add(new JudgePromotionRecord(JUDGE, "v1", GEMINI, "José", "Acepto el piloto", "approval.json",
                "a".repeat(64), Instant.parse("2026-10-19T09:00:00Z")));
        exams.add(exam("v1", JudgeExamStatus.FAILED, "2026-10-20T10:00:00Z", CHATGPT));
        Evaluator own = judge("v1", CHATGPT);
        Evaluator geminiJudge = judge("v1", GEMINI);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(geminiJudge));
        Evaluator claudeJudge = judge("v1", CLAUDE);
        when(own.withRuntime(CLAUDE)).thenReturn(Optional.of(claudeJudge));

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(10));

        assertEquals(GEMINI, opened.getRuntimeIdentity());
        assertEquals(JudgeExamStatus.PROMOTED, opened.getStanding().getStatus());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should skip a runtime the judge cannot be moved to, as one that needs a key, and try the next")
    void aRuntimeTheJudgeCannotBeMovedToIsSkipped() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T15:29:00Z", CLAUDE));
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.empty());
        Evaluator claudeJudge = judge("v1", CLAUDE);
        when(own.withRuntime(CLAUDE)).thenReturn(Optional.of(claudeJudge));

        assertEquals(CLAUDE, runtime().open(JUDGE, List.of(own), closing(10)).getRuntimeIdentity());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should not move a judge whose version would be another one with the other runtime: the runtime never changes the version")
    void aJudgeWhoseVersionWouldChangeWithTheOtherRuntimeDoesNotMove() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);
        Evaluator otherVersionOnGemini = judge("v2", GEMINI);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(otherVersionOnGemini));

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(10));

        assertEquals(CHATGPT, opened.getRuntimeIdentity());
        assertFalse(opened.getStanding().isConsultsOnCourse());
        assertEquals(0, opened.getEffectivePolicy().getMaxNewEvaluations());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should move a judge of two evaluators only when both can be moved to the same runtime")
    void aJudgeOfTwoEvaluatorsMovesOnlyWhenBothCan() {
        exams.add(exam("v1+v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator fromEnglish = judge("v1", CHATGPT);
        Evaluator fromTranslation = judge("v1", CHATGPT);
        Evaluator fromEnglishOnGemini = judge("v1", GEMINI);
        when(fromEnglish.withRuntime(GEMINI)).thenReturn(Optional.of(fromEnglishOnGemini));
        when(fromTranslation.withRuntime(GEMINI)).thenReturn(Optional.empty());

        JudgeSessions stays = runtime().open(JUDGE, List.of(fromEnglish, fromTranslation), closing(10));
        assertEquals(CHATGPT, stays.getRuntimeIdentity());
        assertEquals(0, stays.getEffectivePolicy().getMaxNewEvaluations());

        Evaluator fromTranslationOnGemini = judge("v1", GEMINI);
        when(fromTranslation.withRuntime(GEMINI)).thenReturn(Optional.of(fromTranslationOnGemini));
        JudgeSessions moves = runtime().open(JUDGE, List.of(fromEnglish, fromTranslation), closing(10));
        assertEquals(GEMINI, moves.getRuntimeIdentity());
        assertEquals(10, moves.getEffectivePolicy().getMaxNewEvaluations());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should keep the exam, a reanalysis and every run that does not ask for it on the profile's runtime")
    void onlyTheClosingMovesARunThatPays() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);
        Evaluator geminiJudge = judge("v1", GEMINI);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(geminiJudge));

        EvaluationRunPolicy course = new EvaluationRunPolicy(10, false, null, null, EvaluationRunPurpose.COURSE, 1);
        EvaluationRunPolicy exam = new EvaluationRunPolicy(10, false, null, null, EvaluationRunPurpose.EXAM, 1);
        exam.setAnyEnabledRuntime(true);
        EvaluationRunPolicy reanalysis = new EvaluationRunPolicy(10, false, null, null,
                EvaluationRunPurpose.REANALYSIS, 1);
        reanalysis.setAnyEnabledRuntime(true);
        for (EvaluationRunPolicy policy : Arrays.asList(null, course, exam, reanalysis)) {
            JudgeSessions opened = runtime().open(JUDGE, List.of(own), policy);
            assertEquals(CHATGPT, opened.getRuntimeIdentity(), "no silent change of runtime for "
                    + (policy == null ? "a run without a policy" : String.valueOf(policy.getPurpose())));
            assertNull(opened.getStanding().getOwnRuntimeIdentity());
        }
        verify(own, never()).withRuntime(any());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should count what the other runtime said with today's version as up to date: only the version decides")
    void whatTheOtherRuntimeSaidWithTodaysVersionIsUpToDate() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);
        Evaluator geminiJudge = judge("v1", GEMINI);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(geminiJudge));

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(10));
        JudgeFreshnessReference today = JudgeFreshnessReference.of(opened.getStanding(), exams, ZoneId.of("UTC"),
                Clock.systemUTC());

        assertTrue(today.mark("v1", GEMINI).isUpToDate());
        assertTrue(today.mark("v1", CHATGPT).isUpToDate(), "the runtime never makes a verdict of today's version old");
        assertFalse(today.mark("v0", GEMINI).isUpToDate());
    }

    @Test
    @Tag("F-CMOTOR-R002")
    @DisplayName("should leave a judge enabled with no runtime it can be moved to without consulting, as before")
    void aJudgeEnabledWithNoRuntimeItCanUseStaysWithoutConsulting() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        exams.add(exam("v1", JudgeExamStatus.FAILED, "2026-10-20T10:00:00Z", CHATGPT));
        Evaluator own = judge("v1", CHATGPT);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.empty());

        JudgeSessions opened = runtime().open(JUDGE, List.of(own), closing(200));

        assertEquals(CHATGPT, opened.getRuntimeIdentity());
        assertEquals(JudgeExamStatus.FAILED, opened.getStanding().getStatus());
        assertFalse(opened.getStanding().isConsultsOnCourse());
        assertNull(opened.getStanding().getOwnRuntimeIdentity());
        assertEquals(0, opened.getEffectivePolicy().getMaxNewEvaluations());
    }

    @Test
    @Tag("F-CMOTOR-R004")
    @DisplayName("should read for free with the same runtime as the closing without being asked, and consult nothing")
    void readingForFreeFollowsTheClosingRuntime() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator gemini = judge("v1", GEMINI);
        Evaluator own = judge("v1", CHATGPT);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(gemini));

        JudgeSessions opened = runtime().open(JUDGE, List.of(own),
                new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1));

        assertEquals(GEMINI, opened.getRuntimeIdentity());
        assertEquals(0, opened.getEffectivePolicy().getMaxNewEvaluations());
        verify(courses).open(eq(new EvaluationBudget(0)), same(gemini));
        verify(gemini, never()).evaluate(any());
    }

    @Test
    @Tag("F-CMOTOR-R001")
    @DisplayName("should take, between two runtimes enabled at the same time or with no date, the first one Exámenes lists")
    void aTieOrAMissingDateGoesInTheOrderOfExamenes() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", CLAUDE));
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        Evaluator own = judge("v1", CHATGPT);
        Evaluator geminiJudge = judge("v1", GEMINI);
        Evaluator claudeJudge = judge("v1", CLAUDE);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(geminiJudge));
        when(own.withRuntime(CLAUDE)).thenReturn(Optional.of(claudeJudge));

        JudgeSessions tie = runtime().open(JUDGE, List.of(own), closing(10));
        assertEquals(CLAUDE, tie.getStanding().getEnabledWith().get(0).runtimeIdentity(), "the order of Exámenes");
        assertEquals(CLAUDE, tie.getRuntimeIdentity(), "a tie goes in the order of Exámenes");

        exams.clear();
        JudgeExamRecord undated = exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", CLAUDE);
        undated.setRecordedAt(null);
        exams.add(undated);
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T10:00:00Z", GEMINI));
        assertEquals(GEMINI, runtime().open(JUDGE, List.of(own), closing(10)).getRuntimeIdentity(),
                "a runtime with no date goes after every dated one");
    }

    @Test
    @Tag("F-CMOTOR-R004")
    @DisplayName("should read for free with the very runtime the closing consults with, with the profile's enabled and without it")
    void theClosingAndAFreeReadingTakeTheSameRuntime() {
        EvaluationRunPolicy reading = new EvaluationRunPolicy(0, false, null, null, EvaluationRunPurpose.READING, 1);
        Evaluator own = judge("v1", CHATGPT);
        Evaluator geminiJudge = judge("v1", GEMINI);
        when(own.withRuntime(GEMINI)).thenReturn(Optional.of(geminiJudge));

        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        JudgeSessions closed = runtime().open(JUDGE, List.of(own), closing(10));
        JudgeSessions read = runtime().open(JUDGE, List.of(own), reading);
        assertEquals(GEMINI, closed.getRuntimeIdentity());
        assertEquals(closed.getRuntimeIdentity(), read.getRuntimeIdentity());
        assertEquals(closed.getStanding().getStatus(), read.getStanding().getStatus());

        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-20T10:00:00Z", CHATGPT));
        closed = runtime().open(JUDGE, List.of(own), closing(10));
        read = runtime().open(JUDGE, List.of(own), reading);
        assertEquals(CHATGPT, closed.getRuntimeIdentity());
        assertEquals(closed.getRuntimeIdentity(), read.getRuntimeIdentity());
    }

    private static EvaluationRunPolicy closing(int budget) {
        EvaluationRunPolicy policy = new EvaluationRunPolicy(budget, false, null, null, null, 1);
        policy.setAnyEnabledRuntime(true);
        return policy;
    }

    private DefaultJudgeRuntime runtime() {
        JudgeExamRegistry registry = new JudgeExamRegistry() {
            @Override
            public void record(JudgeExamRecord record) {
                throw new AssertionError("opening a judge never records an exam");
            }

            @Override
            public Optional<JudgeExamRecord> latest(String judgeName, String judgeVersion) {
                return Optional.empty();
            }

            @Override
            public List<JudgeExamRecord> history(String judgeName) {
                return List.copyOf(exams);
            }
        };
        JudgePromotionRegistry promoted = new JudgePromotionRegistry() {
            @Override
            public void record(JudgePromotionRecord promotion) {
                throw new AssertionError("opening a judge never promotes");
            }

            @Override
            public Optional<JudgePromotionRecord> latest(String judgeName, String judgeVersion,
                    EvaluationRuntimeIdentity runtimeIdentity) {
                return promotions.stream().filter(p -> p.judgeName().equals(judgeName)
                        && p.judgeVersion().equals(judgeVersion) && p.runtimeIdentity().equals(runtimeIdentity))
                        .reduce((a, b) -> b);
            }

            @Override
            public List<JudgePromotionRecord> history(String judgeName) {
                return List.copyOf(promotions);
            }
        };
        when(courses.open(any(), any())).thenReturn(mock(EvaluationSession.class));
        JudgeRuntimeConfig config = mock(JudgeRuntimeConfig.class);
        when(config.getMaxConcurrentEvaluations()).thenReturn(1);
        return new DefaultJudgeRuntime(courses, mock(EvaluationSessionFactory.class), registry,
                mock(EvaluationTimeline.class), config, promoted);
    }

    private static Evaluator judge(String version, EvaluationRuntimeIdentity runtime) {
        Evaluator evaluator = mock(Evaluator.class);
        when(evaluator.evaluatorId()).thenReturn("mini-theory-judge");
        when(evaluator.evaluatorVersion()).thenReturn(Optional.of(version));
        when(evaluator.runtimeIdentity()).thenReturn(runtime);
        return evaluator;
    }

    private static JudgeExamRecord exam(String version, JudgeExamStatus status, String at,
            EvaluationRuntimeIdentity runtime) {
        JudgeExamRecord record = new JudgeExamRecord(JUDGE, version, status, 40, 0, List.of(), List.of(),
                status == JudgeExamStatus.APPROVED, "photo", "José", Instant.parse(at));
        record.setRuntimeIdentity(runtime);
        return record;
    }
}
