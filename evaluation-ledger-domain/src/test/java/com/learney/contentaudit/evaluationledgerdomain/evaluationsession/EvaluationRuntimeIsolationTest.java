package com.learney.contentaudit.evaluationledgerdomain.evaluationsession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequest;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRequestMode;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolutionKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSession;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EvaluationRuntimeIsolationTest {

    private static final EvaluationSubject SUBJECT =
            new EvaluationSubject("quiz-1", Map.of("exercise", "Choose one"));
    private static final EvaluationRuntimeIdentity CODEX_MEDIUM =
            EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    private static final EvaluationRuntimeIdentity CODEX_HIGH =
            EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "high");

    @Test
    void sameRuntimeReusesAcrossProcessVersionsButAnotherEffortDoesNotReuseOnAnyReadPath() {
        MemoryLedger ledger = new MemoryLedger();
        DefaultEvaluationSessionFactory factory = new DefaultEvaluationSessionFactory(ledger,
                content -> "same-fingerprint");

        EvaluationSession first = factory.open(new EvaluationBudget(1),
                evaluator("process-v1", CODEX_MEDIUM));
        assertEquals(EvaluationResolutionKind.EVALUATED, first.resolve(SUBJECT).getKind());

        EvaluationSession sameRuntimeNewProcess = factory.open(new EvaluationBudget(0),
                evaluator("process-v2", CODEX_MEDIUM));
        assertEquals(EvaluationResolutionKind.REUSED, sameRuntimeNewProcess.resolve(SUBJECT).getKind());
        assertEquals("process-v1", sameRuntimeNewProcess.resolveWithoutConsulting(SUBJECT)
                .getEvaluatorVersion());

        EvaluationSession otherEffort = factory.open(new EvaluationBudget(0),
                evaluator("process-v2", CODEX_HIGH));
        assertEquals(EvaluationResolutionKind.PENDING, otherEffort.resolve(SUBJECT).getKind());
        assertEquals(EvaluationResolutionKind.PENDING,
                otherEffort.resolveWithoutConsulting(SUBJECT).getKind());
        assertEquals(EvaluationResolutionKind.PENDING,
                otherEffort.resolvePreferringNew(SUBJECT).getKind());
        assertFalse(otherEffort.recorded(SUBJECT));
        assertEquals(EvaluationResolutionKind.PENDING,
                otherEffort.resolveAll(List.of(new EvaluationRequest(
                        SUBJECT, EvaluationRequestMode.REUSE_ONLY)), 2).get(0).getKind());
    }

    @Test
    void legacyHistoryIsVisibleUnderLegacyIdentityButNeverMatchesAnExplicitRuntime() {
        MemoryLedger ledger = new MemoryLedger();
        EvaluationKey legacyKey = new EvaluationKey("judge", "same-fingerprint");
        ledger.append(new EvaluationRecord(legacyKey, "legacy", "quiz-1",
                Instant.parse("2026-01-01T00:00:00Z"), "legacy-process"));

        DefaultEvaluationSessionFactory factory = new DefaultEvaluationSessionFactory(ledger,
                content -> "same-fingerprint");
        EvaluationSession explicit = factory.open(new EvaluationBudget(0),
                evaluator("process-v2", CODEX_MEDIUM));

        assertEquals(EvaluationResolutionKind.PENDING, explicit.resolve(SUBJECT).getKind());
        assertEquals(1, ledger.history(legacyKey).size(), "legacy history remains queryable");
    }

    private static Evaluator evaluator(String version, EvaluationRuntimeIdentity runtime) {
        return new Evaluator() {
            @Override
            public String evaluatorId() {
                return "judge";
            }

            @Override
            public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return new EvaluationEmitted("verdict");
            }

            @Override
            public Optional<String> evaluatorVersion() {
                return Optional.of(version);
            }

            @Override
            public EvaluationRuntimeIdentity runtimeIdentity() {
                return runtime;
            }
        };
    }

    private static final class MemoryLedger implements EvaluationLedger {
        private final List<EvaluationRecord> records = new ArrayList<>();

        @Override
        public void append(EvaluationRecord record) {
            records.add(record);
        }

        @Override
        public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            return records.stream().filter(record -> record.getKey().equals(key))
                    .reduce((first, second) -> second);
        }

        @Override
        public List<EvaluationRecord> history(EvaluationKey key) {
            return records.stream().filter(record -> record.getKey().equals(key)).toList();
        }
    }
}
