package com.learney.contentaudit.evaluationledgerdomain.evaluationsession;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationBudget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationEmitted;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationLedger;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationOutcome;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationResolutionKind;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import com.learney.contentaudit.evaluationledgerdomain.Evaluator;
import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExamRuntimeAliasMigrationTest {

    private static final EvaluationRuntimeIdentity RUNTIME =
            EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    private static final EvaluatorVersionEquivalence PROCESS_EQUIVALENCE =
            new EvaluatorVersionEquivalence("judge", "old-process-only", "process-only",
                    Instant.parse("2026-10-23T00:00:00Z"), "Only the test files stopped counting");

    @Test
    void reusesALegacyExamLineOnlyThroughTheRuntimeCoupledDigestAlias() {
        MemoryLedger ledger = new MemoryLedger();
        ledger.append(new EvaluationRecord(
                new EvaluationKey("exam:judge", "fp"), "old verdict", "case-1",
                Instant.parse("2026-10-05T00:00:00Z"), "old-runtime-coupled-digest"));
        ExamEvaluationSessionFactory factory = new ExamEvaluationSessionFactory(ledger, content -> "fp");

        var migrated = factory.open(new EvaluationBudget(0), evaluator(true));
        var withProcessEquivalence = factory.open(new EvaluationBudget(0), processEquivalentEvaluator(RUNTIME));
        var withoutAlias = factory.open(new EvaluationBudget(0), evaluator(false));
        EvaluationSubject subject = new EvaluationSubject("case-1", Map.of("exercise", "x"));

        assertEquals(EvaluationResolutionKind.REUSED, migrated.resolve(subject).getKind());
        assertEquals(EvaluationResolutionKind.REUSED, withProcessEquivalence.resolve(subject).getKind());
        assertEquals(EvaluationResolutionKind.PENDING, withoutAlias.resolve(subject).getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"judge", "exam:judge"})
    void doesNotReuseAProcessEquivalentLineWithoutItsRuntime(String recordedEvaluatorId) {
        MemoryLedger ledger = new MemoryLedger();
        ledger.append(new EvaluationRecord(
                new EvaluationKey(recordedEvaluatorId, "fp"), "old verdict", "case-1",
                Instant.parse("2026-10-05T00:00:00Z"), "old-process-only"));
        var session = new ExamEvaluationSessionFactory(ledger, content -> "fp")
                .open(new EvaluationBudget(0), processEquivalentEvaluator(RUNTIME));

        assertEquals(EvaluationResolutionKind.PENDING,
                session.resolve(new EvaluationSubject("case-1", Map.of("exercise", "x"))).getKind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"judge", "exam:judge"})
    void reusesAProcessEquivalentLineWithTheSameExplicitRuntime(String recordedEvaluatorId) {
        MemoryLedger ledger = new MemoryLedger();
        ledger.append(new EvaluationRecord(
                new EvaluationKey(recordedEvaluatorId, "fp", RUNTIME), "old verdict", "case-1",
                Instant.parse("2026-10-05T00:00:00Z"), "old-process-only"));
        var session = new ExamEvaluationSessionFactory(ledger, content -> "fp")
                .open(new EvaluationBudget(0), processEquivalentEvaluator(RUNTIME));

        assertEquals(EvaluationResolutionKind.REUSED,
                session.resolve(new EvaluationSubject("case-1", Map.of("exercise", "x"))).getKind());
    }

    @ParameterizedTest
    @CsvSource({
        "other-provider,gpt-6.1-sol,medium",
        "codex-cli,other-model,medium",
        "codex-cli,gpt-6.1-sol,high"
    })
    void doesNotReuseAProcessEquivalentLineFromAnotherRuntime(
            String provider, String model, String reasoning) {
        MemoryLedger ledger = new MemoryLedger();
        ledger.append(new EvaluationRecord(
                new EvaluationKey("exam:judge", "fp", RUNTIME), "old verdict", "case-1",
                Instant.parse("2026-10-05T00:00:00Z"), "old-process-only"));
        var otherRuntime = EvaluationRuntimeIdentity.explicit(provider, model, reasoning);
        var session = new ExamEvaluationSessionFactory(ledger, content -> "fp")
                .open(new EvaluationBudget(0), processEquivalentEvaluator(otherRuntime));

        assertEquals(EvaluationResolutionKind.PENDING,
                session.resolve(new EvaluationSubject("case-1", Map.of("exercise", "x"))).getKind());
    }

    private static Evaluator processEquivalentEvaluator(EvaluationRuntimeIdentity runtime) {
        return evaluator(Set.of("process-only", "old-process-only", "old-runtime-coupled-digest"),
                List.of(PROCESS_EQUIVALENCE), runtime);
    }

    private static Evaluator evaluator(boolean withAlias) {
        return evaluator(withAlias
                ? Set.of("process-only", "old-runtime-coupled-digest")
                : Set.of("process-only"), List.of(), RUNTIME);
    }

    private static Evaluator evaluator(Set<String> versions,
            List<EvaluatorVersionEquivalence> equivalences, EvaluationRuntimeIdentity runtime) {
        return new Evaluator() {
            @Override public String evaluatorId() { return "judge"; }
            @Override public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return new EvaluationEmitted("new verdict");
            }
            @Override public Optional<String> evaluatorVersion() { return Optional.of("process-only"); }
            @Override public EvaluationRuntimeIdentity runtimeIdentity() {
                return runtime;
            }
            @Override public Set<String> compatibleEvaluatorVersions() {
                return versions;
            }
            @Override public List<EvaluatorVersionEquivalence> versionEquivalences() {
                return equivalences;
            }
        };
    }

    private static final class MemoryLedger implements EvaluationLedger {
        private final List<EvaluationRecord> records = new ArrayList<>();
        @Override public void append(EvaluationRecord record) { records.add(record); }
        @Override public Optional<EvaluationRecord> findLatest(EvaluationKey key) {
            return records.stream().filter(record -> record.getKey().equals(key))
                    .reduce((first, second) -> second);
        }
        @Override public List<EvaluationRecord> history(EvaluationKey key) {
            return records.stream().filter(record -> record.getKey().equals(key)).toList();
        }
    }
}
