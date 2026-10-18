package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/** The real exam history of the mini-theory judge, as its records say it (metadata only). */
final class JudgeFreshnessFixture {
    static final String JUDGE = "mini-theory";
    static final String V_5_10 = "738388a5aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    static final String V_13_10_MORNING = "33e026fabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    static final String V_TODAY = "1eee07a9cccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    static final String V_UNAPPROVED = "c8ca16c9dddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
    static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T15:00:00Z"), ZONE);
    static final EvaluationRuntimeIdentity XHIGH = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "xhigh");
    static final EvaluationRuntimeIdentity MEDIUM = EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    static final EvaluationRuntimeIdentity LOCAL = EvaluationRuntimeIdentity.explicit("ollama", "llama3", "NOT_APPLICABLE");

    private JudgeFreshnessFixture() {
    }

    static JudgeExamRecord record(String version, JudgeExamStatus status, String recordedAt,
            EvaluationRuntimeIdentity runtime) {
        var record = new JudgeExamRecord(JUDGE, version, status, 20, 0, List.of(), List.of(),
                status == JudgeExamStatus.APPROVED, null, "human", Instant.parse(recordedAt));
        record.setRuntimeIdentity(runtime);
        return record;
    }

    static List<JudgeExamRecord> history() {
        return List.of(
                record(V_5_10, JudgeExamStatus.APPROVED, "2026-10-06T02:14:16Z", XHIGH),
                record(V_13_10_MORNING, JudgeExamStatus.APPROVED, "2026-10-13T16:00:29Z", XHIGH),
                record(V_TODAY, JudgeExamStatus.APPROVED, "2026-10-14T01:12:33Z", XHIGH),
                record(V_TODAY, JudgeExamStatus.APPROVED, "2026-10-14T15:29:00Z", MEDIUM),
                record(V_UNAPPROVED, JudgeExamStatus.FAILED, "2026-10-15T12:00:00Z", XHIGH));
    }

    static JudgeExamStanding standing(String version, JudgeExamStatus status, Set<String> compatible) {
        return new JudgeExamStanding(JUDGE, version, status, true, null, XHIGH, compatible);
    }

    static JudgeFreshnessReference today() {
        return JudgeFreshnessReference.of(standing(V_TODAY, JudgeExamStatus.APPROVED, Set.of(V_TODAY)),
                history(), ZONE, CLOCK);
    }
}
