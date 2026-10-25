package com.learney.contentaudit.auditinfrastructure.batchstore;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FEAT-GUARDA: the decisions a batch inherited live inside its file, of tens of MB. Reading the decisions of a batch
 * just loaded does not parse that file again while it stays the same, and reads it again when it changes.
 */
@Tag("FEAT-GUARDA")
@Tag("F-GUARDA-R001")
class InheritedReviewsOfALoadedBatchTest {
    @TempDir Path root;
    private BatchReviewDecision inherited;

    @BeforeEach void twoVersionsOfACorrection() {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1);
        store.save(first);
        var second = batch("c2", 2);
        var base = new CourseEntity(); base.setTitle("base");
        var source = new CourseEntity(); source.setTitle("source");
        inherited = new BatchReviewDecision("c2", List.of(new BatchReviewDecision.Selection("change",
                new BatchChangeStep("quiz", List.of(), null), snapshot("before"), snapshot("after"))),
                BatchReviewDecision.Status.APPROVED, "Human", Instant.EPOCH);
        second.setEvolution(new CorrectionEvolution.Lineage("c1", 4, 0, "op", "sealed", base, source,
                List.of(inherited)));
        store.commitSuccessor("c1", 4, "sealed", 0, second);
    }

    @Test
    @DisplayName("should read the inherited decisions of a batch just loaded without parsing its file again while it stays the same")
    void theInheritedDecisionsOfABatchJustLoadedDoNotParseItsFileAgain() throws IOException {
        var reader = new FileSystemBatchStore(root);
        reader.load("c2").orElseThrow();
        Path file = file("c2");
        byte[] original = Files.readAllBytes(file);
        var modified = Files.getLastModifiedTime(file);
        // The same file, the same size and the same time, but unreadable: only an answer from the read before works.
        byte[] garbage = new byte[original.length];
        Arrays.fill(garbage, (byte) '#');
        try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(garbage), 0);
        }
        Files.setLastModifiedTime(file, modified);
        try {
            assertEquals(List.of(inherited), reader.reviews("c2"));
            assertThrows(UncheckedIOException.class, () -> new FileSystemBatchStore(root).reviews("c2"),
                    "another store has to parse the file, and it cannot");
        } finally {
            try (var channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(original), 0);
            }
        }
    }

    @Test
    @DisplayName("should read the inherited decisions again when the file of the batch changes")
    void theInheritedDecisionsAreReadAgainWhenTheFileChanges() throws IOException {
        var reader = new FileSystemBatchStore(root);
        reader.load("c2").orElseThrow();
        assertEquals("Human", reader.reviews("c2").get(0).reviewer());
        Path file = file("c2");
        Path rewritten = file.resolveSibling("c2.json.tmp");
        Files.writeString(rewritten, Files.readString(file).replace("\"Human\"", "\"Somebody else\""));
        Files.move(rewritten, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        assertEquals("Somebody else", reader.reviews("c2").get(0).reviewer());
    }

    @Test
    @DisplayName("should answer a copy each time, as a fresh read would")
    void eachAnswerIsACopy() {
        var reader = new FileSystemBatchStore(root);
        reader.load("c2").orElseThrow();
        reader.reviews("c2").get(0).selections().get(0).before().getQuiz().setTranslation("changed by a caller");

        assertEquals("before", reader.reviews("c2").get(0).selections().get(0).before().getQuiz().getTranslation());
        assertEquals(List.of(inherited), reader.reviews("c2"));
    }

    private Path file(String id) { return root.resolve(".content-audit/batches/" + id + ".json"); }

    private static CorrectionBatch batch(String id, int revision) {
        var b = new CorrectionBatch(); b.setId(id); b.setLevel(CefrLevel.A1); b.setStatus(BatchStatus.OPEN);
        b.setCorrectionRevision(revision); b.setContentVersion(4); b.setBaselineAuditId("baseline");
        b.setOpenedAt(Instant.EPOCH.plusSeconds(revision)); return b;
    }

    private static CourseElementSnapshot snapshot(String translation) {
        var quiz = new QuizTemplateEntity(); quiz.setId("quiz"); quiz.setTranslation(translation);
        return new CourseElementSnapshot(AuditTarget.QUIZ, "quiz", quiz, null);
    }
}
