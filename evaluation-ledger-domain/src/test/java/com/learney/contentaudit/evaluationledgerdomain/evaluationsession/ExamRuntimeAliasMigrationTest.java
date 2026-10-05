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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExamRuntimeAliasMigrationTest {

    @Test
    void reusesALegacyExamLineOnlyThroughTheRuntimeCoupledDigestAlias() {
        MemoryLedger ledger = new MemoryLedger();
        ledger.append(new EvaluationRecord(
                new EvaluationKey("exam:judge", "fp"), "old verdict", "case-1",
                Instant.parse("2026-10-05T00:00:00Z"), "old-runtime-coupled-digest"));
        ExamEvaluationSessionFactory factory = new ExamEvaluationSessionFactory(ledger, content -> "fp");

        var migrated = factory.open(new EvaluationBudget(0), evaluator(true));
        var withoutAlias = factory.open(new EvaluationBudget(0), evaluator(false));
        EvaluationSubject subject = new EvaluationSubject("case-1", Map.of("exercise", "x"));

        assertEquals(EvaluationResolutionKind.REUSED, migrated.resolve(subject).getKind());
        assertEquals(EvaluationResolutionKind.PENDING, withoutAlias.resolve(subject).getKind());
    }

    private static Evaluator evaluator(boolean withAlias) {
        return new Evaluator() {
            @Override public String evaluatorId() { return "judge"; }
            @Override public EvaluationOutcome evaluate(EvaluationSubject subject) {
                return new EvaluationEmitted("new verdict");
            }
            @Override public Optional<String> evaluatorVersion() { return Optional.of("process-only"); }
            @Override public EvaluationRuntimeIdentity runtimeIdentity() {
                return EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
            }
            @Override public Set<String> compatibleEvaluatorVersions() {
                return withAlias
                        ? Set.of("process-only", "old-runtime-coupled-digest")
                        : Set.of("process-only");
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
