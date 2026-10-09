package com.learney.contentaudit.auditinfrastructure.minitheory;
import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.minitheorycorrection.MiniTheoryCorrection.*;
import com.learney.contentaudit.auditinfrastructure.batchstore.FileSystemBatchStore;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemMiniTheoryWorkspaceStoreTest {
    @TempDir Path directory;
    @Test void readDoesNotCreateFilesAndRefusesTraversal() {
        var store = new FileSystemMiniTheoryWorkspaceStore(directory);
        assertEquals(Workspace.empty(), store.load("batch"));
        assertFalse(Files.exists(directory.resolve(".content-audit")));
        assertThrows(IllegalArgumentException.class, () -> store.load("../../other"));
    }
    @Test void atomicJournalKeepsChargesAndFeedbackAcrossInstances() {
        var store = new FileSystemMiniTheoryWorkspaceStore(directory);
        var data = new Workspace(List.of(), List.of(new Feedback("feedback", "topic", List.of("quiz"), "context", "José", 5, Instant.now())),
                List.of(new Job("paid", "rewrite", "topic", List.of("quiz"), List.of(), "RUNNING", 1, 1, 0, null, null, null, Instant.now())));
        store.exclusive("batch", () -> { store.save("batch", data); return null; });
        assertEquals(data, new FileSystemMiniTheoryWorkspaceStore(directory).load("batch"));
    }
    @Test void anotherWriterCannotEnterButReadersRemainAvailable() {
        var one = new FileSystemMiniTheoryWorkspaceStore(directory);
        var two = new FileSystemMiniTheoryWorkspaceStore(directory);
        one.exclusive("batch", () -> {
            assertThrows(IllegalStateException.class, () -> two.exclusive("batch", () -> null));
            assertEquals(Workspace.empty(), two.load("batch")); return null;
        });
        assertDoesNotThrow(() -> two.exclusive("batch", () -> null));
    }
    @Test void effectiveJudgeIdentityIsStableRegardlessOfSetAndMapInsertionOrder() {
        var one = new FileSystemMiniTheoryWorkspaceStore(directory);
        var two = new FileSystemMiniTheoryWorkspaceStore(directory);
        assertEquals(one.fingerprintIdentity(Map.of("versions", List.of("b", "a"), "provider", "codex-cli")),
                two.fingerprintIdentity(Map.of("provider", "codex-cli", "versions", List.of("a", "b"))));
        assertNotEquals(one.fingerprintIdentity(Map.of("model", "one")), one.fingerprintIdentity(Map.of("model", "two")));
    }
    @Test void batchCasRefusesLateEditAndLeavesNewVersionIntact() {
        var store = new FileSystemBatchStore(directory);
        var original = new CorrectionBatch(); original.setId("batch"); original.setStatus(BatchStatus.CLOSED); original.setContentVersion(5);
        store.save(original);
        var late = store.load("batch").orElseThrow(); late.setContentVersion(6);
        original.setContentVersion(7); store.save(original);
        assertThrows(IllegalStateException.class, () -> store.saveIfVersion(late, 5, BatchStatus.CLOSED));
        assertEquals(7, store.load("batch").orElseThrow().getContentVersion());
    }
    @Test void analysisTreeAndOptionalDiagnosesRoundTripWithoutCircularParentSerialization() {
        var root = new com.learney.contentaudit.auditdomain.AuditNode(null, com.learney.contentaudit.auditdomain.AuditTarget.COURSE,
                null, new ArrayList<>(), Map.of(), Map.of(), new com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses());
        var child = new com.learney.contentaudit.auditdomain.AuditNode(null, com.learney.contentaudit.auditdomain.AuditTarget.QUIZ,
                root, List.of(), Map.of(), Map.of(), new com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses());
        root.getChildren().add(child);
        var report = new com.learney.contentaudit.auditdomain.AuditReport(root);
        var data = new Workspace(List.of(), List.of(), List.of(new Job("analysis", "analyze", "topic", List.of("quiz"), List.of(),
                "DONE", 2, 2, 2, "context", null, report, Instant.now())));
        var store = new FileSystemMiniTheoryWorkspaceStore(directory); store.save("batch", data);
        var loaded = new FileSystemMiniTheoryWorkspaceStore(directory).load("batch").jobs().get(0).report();
        assertInstanceOf(com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses.class, loaded.getRoot().getDiagnoses());
        assertEquals(1, loaded.getRoot().getChildren().size());
        assertInstanceOf(com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses.class, loaded.getRoot().getChildren().get(0).getDiagnoses());
    }
}
