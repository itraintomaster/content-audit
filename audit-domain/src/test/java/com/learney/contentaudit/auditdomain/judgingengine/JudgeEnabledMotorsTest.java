package com.learney.contentaudit.auditdomain.judgingengine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.learney.contentaudit.auditdomain.EvaluationRunPolicy;
import com.learney.contentaudit.auditdomain.EvaluationRunPurpose;
import com.learney.contentaudit.auditdomain.JudgeExamRegistry;
import com.learney.contentaudit.auditdomain.JudgePromotionRegistry;
import com.learney.contentaudit.auditdomain.JudgeRuntimeConfig;
import com.learney.contentaudit.auditdomain.judging.JudgeEnablement;
import com.learney.contentaudit.auditdomain.judging.JudgeExamRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeExamStatus;
import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.auditdomain.judging.JudgeSessions;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSessionFactory;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationTimeline;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-ALDIA-R003: opening a judge says with which motors its version of today is enabled -- the ones it
 * approved with and the ones José promoted it with --, its own first. It reads the exams and the
 * promotions only: it consults nobody, and the session still consults only with its own motor.
 */
@Tag("FEAT-ALDIA")
@Tag("F-ALDIA-R003")
class JudgeEnabledMotorsTest {
    static final String JUDGE = "mini-theory";
    static final EvaluationRuntimeIdentity CHATGPT = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    static final EvaluationRuntimeIdentity XHIGH = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "xhigh");
    static final EvaluationRuntimeIdentity GEMINI = EvaluationRuntimeIdentity.explicit("agy", "gemini-3.8-flash-high", "high");
    static final EvaluationRuntimeIdentity CLAUDE = EvaluationRuntimeIdentity.explicit("claude-cli", "opus", "high");

    final List<JudgeExamRecord> exams = new ArrayList<>();
    final List<JudgePromotionRecord> promotions = new ArrayList<>();
    final EvaluationSessionFactory courses = mock(EvaluationSessionFactory.class);

    @Test
    @DisplayName("should say the motors the version approved with, its own first, and leave out the one whose last exam failed")
    void opensWithTheMotorsTheVersionApprovedWithItsOwnFirst() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T15:29:00Z", CHATGPT));
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-14T01:12:33Z", XHIGH));
        exams.add(exam("v1", JudgeExamStatus.FAILED, "2026-10-16T12:00:00Z", XHIGH));
        exams.add(exam("v0", JudgeExamStatus.APPROVED, "2026-10-05T12:00:00Z", CLAUDE));

        JudgeSessions opened = runtime().open(JUDGE, List.of(judge("v1", CHATGPT)), null);

        assertEquals(List.of(
                new JudgeEnablement(CHATGPT, JudgeExamStatus.APPROVED, Instant.parse("2026-10-14T15:29:00Z")),
                new JudgeEnablement(GEMINI, JudgeExamStatus.APPROVED, Instant.parse("2026-10-19T14:30:58Z"))),
                opened.getStanding().getEnabledWith());
    }

    @Test
    @DisplayName("should keep a version approved only with another motor from consulting with its own, and say the other")
    void aVersionApprovedOnlyWithAnotherMotorStillConsultsOnlyWithItsOwn() {
        exams.add(exam("v1", JudgeExamStatus.APPROVED, "2026-10-19T14:30:58Z", GEMINI));

        JudgeSessions opened = runtime().open(JUDGE, List.of(judge("v1", CHATGPT)),
                new EvaluationRunPolicy(3, false, null, null, EvaluationRunPurpose.COURSE, 1));

        assertEquals(JudgeExamStatus.NOT_TAKEN, opened.getStanding().getStatus());
        assertFalse(opened.getStanding().isConsultsOnCourse(), "no silent fallback to the motor that approved");
        assertEquals(0, opened.getEffectivePolicy().getMaxNewEvaluations());
        assertEquals(List.of(new JudgeEnablement(GEMINI, JudgeExamStatus.APPROVED, Instant.parse("2026-10-19T14:30:58Z"))),
                opened.getStanding().getEnabledWith());
    }

    @Test
    @DisplayName("should count a promotion of today's version as enabling its motor, until an exam measured after with that motor replaces it")
    void aPromotionEnablesItsMotorUntilALaterMeasuredExamReplacesIt() {
        Instant promotedAt = Instant.parse("2026-10-14T11:37:18Z");
        promotions.add(new JudgePromotionRecord(JUDGE, "v1", CHATGPT, "José", "Acepto el piloto", "approval.json",
                "a".repeat(64), promotedAt));
        promotions.add(new JudgePromotionRecord(JUDGE, "v0", CLAUDE, "José", "Otra versión", "approval.json",
                "a".repeat(64), promotedAt));

        JudgeSessions opened = runtime().open(JUDGE, List.of(judge("v1", GEMINI)), null);
        assertEquals(List.of(new JudgeEnablement(CHATGPT, JudgeExamStatus.PROMOTED, promotedAt)),
                opened.getStanding().getEnabledWith(), "a promotion of another version enables nothing");

        exams.add(exam("v1", JudgeExamStatus.FAILED, "2026-10-15T12:00:00Z", CHATGPT));
        assertEquals(List.of(), runtime().open(JUDGE, List.of(judge("v1", GEMINI)), null).getStanding().getEnabledWith());
    }

    @Test
    @DisplayName("should say no motor for a version that approved with none")
    void aVersionThatApprovedWithNoMotorIsEnabledWithNone() {
        exams.add(exam("v0", JudgeExamStatus.APPROVED, "2026-10-05T12:00:00Z", CHATGPT));
        exams.add(exam("v1", JudgeExamStatus.FAILED, "2026-10-19T12:00:00Z", GEMINI));

        assertEquals(List.of(), runtime().open(JUDGE, List.of(judge("v1", CHATGPT)), null).getStanding().getEnabledWith());
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
