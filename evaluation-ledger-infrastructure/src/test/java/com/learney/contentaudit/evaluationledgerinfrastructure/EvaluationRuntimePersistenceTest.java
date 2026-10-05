package com.learney.contentaudit.evaluationledgerinfrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvaluationRuntimePersistenceTest {

    private static final EvaluationRuntimeIdentity CODEX =
            EvaluationRuntimeIdentity.explicit("codex-cli", "gpt-6.1-sol", "medium");
    private static final EvaluationRuntimeIdentity CLAUDE =
            EvaluationRuntimeIdentity.explicit("claude-cli", "claude-sonnet-4-5",
                    EvaluationRuntimeIdentity.NOT_APPLICABLE);

    @Test
    void persistsAndFiltersProviderModelAndReasoningAsPartOfTheKey(@TempDir Path baseDir) {
        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        EvaluationKey codexKey = new EvaluationKey("judge", "fp", CODEX);
        EvaluationKey claudeKey = new EvaluationKey("judge", "fp", CLAUDE);
        ledger.append(new EvaluationRecord(codexKey, "codex", "quiz-1",
                Instant.parse("2026-10-05T10:00:00Z"), "process-a"));
        ledger.append(new EvaluationRecord(claudeKey, "claude", "quiz-1",
                Instant.parse("2026-10-05T10:00:05Z"), "process-a"));

        FileSystemEvaluationLedger reopened = new FileSystemEvaluationLedger(baseDir);
        assertEquals("codex", reopened.findLatest(codexKey).orElseThrow().getPayload());
        assertEquals("claude", reopened.findLatest(claudeKey).orElseThrow().getPayload());
        assertEquals(List.of(Instant.parse("2026-10-05T10:00:00Z")),
                reopened.recordedTimes("judge", CODEX));
        assertEquals(2, reopened.recordedTimes("judge").size());
    }

    @Test
    void readsHistoricalJsonlAsLegacyWithoutRewritingOrMatchingExplicitRuntime(@TempDir Path baseDir)
            throws Exception {
        Path file = baseDir.resolve(".content-audit/evaluations/judge.jsonl");
        Files.createDirectories(file.getParent());
        String historical = "{\"evaluatorId\":\"judge\",\"contentFingerprint\":\"fp\","
                + "\"payload\":\"old\",\"subjectRef\":\"quiz-1\","
                + "\"recordedAt\":\"2026-01-01T00:00:00Z\",\"evaluatorVersion\":\"old-process\"}\n";
        Files.writeString(file, historical);

        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        assertTrue(ledger.findLatest(new EvaluationKey("judge", "fp")).isPresent());
        assertTrue(ledger.findLatest(new EvaluationKey("judge", "fp", CODEX)).isEmpty());
        assertEquals(historical, Files.readString(file), "reading never rewrites historical JSONL");
    }

    @Test
    void keepsPartialOrUnrecognizedRuntimeProvenanceVisibleAsLegacy(@TempDir Path baseDir)
            throws Exception {
        Path partialFile = baseDir.resolve(".content-audit/evaluations/partial.jsonl");
        Path invalidFile = baseDir.resolve(".content-audit/evaluations/invalid.jsonl");
        Files.createDirectories(partialFile.getParent());
        Files.writeString(partialFile, line("partial", "EXPLICIT", "codex-cli", null, "medium"));
        Files.writeString(invalidFile, line("invalid", "FUTURE_KIND", "codex-cli", "gpt-6.1-sol", "medium"));

        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        EvaluationRuntimeIdentity legacy = EvaluationRuntimeIdentity.legacyUnknown();

        assertTrue(ledger.findLatest(new EvaluationKey("partial", "fp", legacy)).isPresent());
        assertTrue(ledger.findLatest(new EvaluationKey("invalid", "fp", legacy)).isPresent());
        assertTrue(ledger.findLatest(new EvaluationKey("partial", "fp", CODEX)).isEmpty());
        assertTrue(ledger.findLatest(new EvaluationKey("invalid", "fp", CODEX)).isEmpty());
    }

    private static String line(String evaluatorId, String kind, String provider, String model,
            String effort) {
        return "{\"evaluatorId\":\"" + evaluatorId + "\",\"contentFingerprint\":\"fp\"," 
                + "\"runtimeKind\":\"" + kind + "\"," 
                + "\"providerName\":" + jsonString(provider) + ","
                + "\"modelName\":" + jsonString(model) + ","
                + "\"reasoningEffort\":" + jsonString(effort) + ","
                + "\"payload\":\"old\",\"subjectRef\":\"quiz-1\"," 
                + "\"recordedAt\":\"2026-01-01T00:00:00Z\",\"evaluatorVersion\":\"old-process\"}\n";
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
