package com.learney.contentaudit.evaluationledgerinfrastructure;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationKey;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ledger keeps each file parsed while it stays the same file, so an analysis does not re-read
 * the whole file once per exercise. These tests pin what that must never cost: a result written
 * after the first read -- by another run, by this instance, or completed after a crash -- is
 * always found, and a file that is pruned or removed stops answering with what it no longer has.
 */
class FileSystemEvaluationLedgerReadOnceIntegrationTest {

    private static final String EVALUATOR_ID = "quiz-instruction-validator";

    private static EvaluationRecord record(String fingerprint, String payload, Instant recordedAt) {
        return new EvaluationRecord(new EvaluationKey(EVALUATOR_ID, fingerprint), payload,
                "quiz-" + fingerprint, recordedAt, "v1");
    }

    private static Path ledgerFile(Path baseDir) throws IOException {
        try (var files = Files.walk(baseDir)) {
            List<Path> regular = files.filter(Files::isRegularFile).collect(Collectors.toList());
            assertEquals(1, regular.size(), "one ledger file: " + regular);
            return regular.get(0);
        }
    }

    @Test
    @DisplayName("should find a result another run appended after this ledger had already read the file")
    void shouldFindAResultAnotherRunAppendedAfterThisLedgerHadAlreadyReadTheFile(@TempDir Path baseDir) {
        FileSystemEvaluationLedger reader = new FileSystemEvaluationLedger(baseDir);
        FileSystemEvaluationLedger otherRun = new FileSystemEvaluationLedger(baseDir);
        otherRun.append(record("fp-1", "{\"v\":1}", Instant.parse("2026-09-30T10:00:00Z")));

        assertTrue(reader.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-1")).isPresent());
        assertFalse(reader.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2")).isPresent());

        otherRun.append(record("fp-2", "{\"v\":2}", Instant.parse("2026-09-30T10:00:20Z")));
        otherRun.append(record("fp-1", "{\"v\":3}", Instant.parse("2026-09-30T10:00:40Z")));

        assertEquals("{\"v\":2}", reader.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2"))
                .map(EvaluationRecord::getPayload).orElse(null), "the new key is found");
        assertEquals("{\"v\":3}", reader.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-1"))
                .map(EvaluationRecord::getPayload).orElse(null), "the latest of a key re-recorded is the one that applies");
        assertEquals(2, reader.history(new EvaluationKey(EVALUATOR_ID, "fp-1")).size());
        assertEquals(List.of(Instant.parse("2026-09-30T10:00:00Z"), Instant.parse("2026-09-30T10:00:20Z"),
                Instant.parse("2026-09-30T10:00:40Z")), reader.recordedTimes(EVALUATOR_ID));
    }

    @Test
    @DisplayName("should find what this same ledger appended right after a lookup")
    void shouldFindWhatThisSameLedgerAppendedRightAfterALookup(@TempDir Path baseDir) {
        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        EvaluationKey key = new EvaluationKey(EVALUATOR_ID, "fp-1");

        assertFalse(ledger.findLatest(key).isPresent(), "nothing before the file exists");
        ledger.append(record("fp-1", "{\"v\":1}", Instant.parse("2026-09-30T10:00:00Z")));
        assertTrue(ledger.findLatest(key).isPresent(), "found once the file exists");
        ledger.append(record("fp-1", "{\"v\":2}", Instant.parse("2026-09-30T10:00:20Z")));
        assertEquals("{\"v\":2}", ledger.findLatest(key).map(EvaluationRecord::getPayload).orElse(null));
    }

    @Test
    @DisplayName("should find an entry whose half-written line was completed after the first read")
    void shouldFindAnEntryWhoseHalfWrittenLineWasCompletedAfterTheFirstRead(@TempDir Path baseDir)
            throws IOException {
        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        ledger.append(record("fp-1", "{\"v\":1}", Instant.parse("2026-09-30T10:00:00Z")));
        Path file = ledgerFile(baseDir);
        String full = "{\"evaluatorId\":\"" + EVALUATOR_ID + "\",\"contentFingerprint\":\"fp-2\","
                + "\"payload\":\"{}\",\"subjectRef\":\"quiz-fp-2\",\"recordedAt\":\"2026-09-30T10:00:20Z\","
                + "\"evaluatorVersion\":\"v1\"}\n";
        int cut = full.length() / 2;
        Files.writeString(file, full.substring(0, cut), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        assertFalse(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2")).isPresent(),
                "a half-written line is never registered");

        Files.writeString(file, full.substring(cut), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        assertTrue(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2")).isPresent(),
                "once the line is whole, it is read");
        assertTrue(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-1")).isPresent());
    }

    @Test
    @DisplayName("should stop answering with what a pruned or removed file no longer has")
    void shouldStopAnsweringWithWhatAPrunedOrRemovedFileNoLongerHas(@TempDir Path baseDir) throws IOException {
        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        ledger.append(record("fp-1", "{\"v\":1}", Instant.parse("2026-09-30T10:00:00Z")));
        ledger.append(record("fp-2", "{\"v\":2}", Instant.parse("2026-09-30T10:00:20Z")));
        assertTrue(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-1")).isPresent());

        Path file = ledgerFile(baseDir);
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Files.write(file, lines.subList(1, 2), StandardCharsets.UTF_8);

        assertFalse(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-1")).isPresent(),
                "the pruned line is gone");
        assertTrue(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2")).isPresent());

        Files.delete(file);

        assertFalse(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-2")).isPresent(),
                "a removed file has nothing registered");
        assertTrue(ledger.recordedTimes(EVALUATOR_ID).isEmpty());
    }

    @Test
    @DisplayName("should give each caller its own copy, so changing a returned record changes nothing registered")
    void shouldGiveEachCallerItsOwnCopySoChangingAReturnedRecordChangesNothingRegistered(@TempDir Path baseDir) {
        FileSystemEvaluationLedger ledger = new FileSystemEvaluationLedger(baseDir);
        EvaluationKey key = new EvaluationKey(EVALUATOR_ID, "fp-1");
        ledger.append(record("fp-1", "{\"v\":1}", Instant.parse("2026-09-30T10:00:00Z")));

        Optional<EvaluationRecord> first = ledger.findLatest(key);
        first.orElseThrow().setPayload("tampered");
        first.orElseThrow().getKey().setContentFingerprint("fp-9");
        ledger.history(key).get(0).setPayload("tampered too");

        assertEquals("{\"v\":1}", ledger.findLatest(key).map(EvaluationRecord::getPayload).orElse(null));
        assertFalse(ledger.findLatest(new EvaluationKey(EVALUATOR_ID, "fp-9")).isPresent());
    }
}
