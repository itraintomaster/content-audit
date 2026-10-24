package com.learney.contentaudit.auditinfrastructure.batchstore;

import static org.junit.jupiter.api.Assertions.*;

import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemCorrectionEvolutionTest {
    @TempDir Path root;

    @Test void commitsThreeCorrectionsWithoutRewritingParentsAndDoesNotReopenThemAfterPublication() throws IOException {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); store.save(first);
        byte[] before = Files.readAllBytes(file("c1"));
        var second = child(first, "c2"); store.commitSuccessor("c1", 4, "sealed", 0, second);
        assertArrayEquals(before, Files.readAllBytes(file("c1")));
        assertEquals("c2", new FileSystemBatchStore(root).open().orElseThrow().getId());
        var third = child(second, "c3"); store.commitSuccessor("c2", 4, "sealed", 0, third);
        assertEquals("c3", store.open().orElseThrow().getId());
        third.setStatus(BatchStatus.PUBLISHED); store.save(third);
        assertTrue(store.open().isEmpty(), "a frozen predecessor must never become the active correction again");
        assertEquals(3, store.list().size());
        assertEquals("source", store.load("c3").orElseThrow().getEvolution().sourceCourse().getTitle());
    }

    @Test void staleContentAnalysisOrFingerprintCannotCommitASuccessor() {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); store.save(first);
        var next = child(first, "c2");
        assertThrows(IllegalStateException.class, () -> store.commitSuccessor("c1", 3, "sealed", 0, next));
        assertThrows(IllegalStateException.class, () -> store.commitSuccessor("c1", 4, "another", 0, next));
        first.setStatus(BatchStatus.CLOSED); store.save(first);
        store.saveAnalysisSelection(selection("c1", "selection", 0));
        assertThrows(IllegalStateException.class, () -> store.commitSuccessor("c1", 4, "sealed", 0, next));
        assertFalse(store.hasSuccessor("c1"));
        assertTrue(store.load("c2").isEmpty());
    }

    @Test void twoStoreInstancesCannotCreateTwoSuccessorsFromTheSameParent() throws Exception {
        var firstStore = new FileSystemBatchStore(root);
        var secondStore = new FileSystemBatchStore(root);
        var first = batch("c1", 1); firstStore.save(first);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var one = pool.submit(() -> attempt(firstStore, child(first, "c2-a"), start));
            var two = pool.submit(() -> attempt(secondStore, child(first, "c2-b"), start));
            start.countDown();
            assertEquals(1, one.get(10, TimeUnit.SECONDS) + two.get(10, TimeUnit.SECONDS));
        }
        assertEquals(2, firstStore.list().size());
        assertTrue(secondStore.hasSuccessor("c1"));
    }

    @Test void frozenContentRejectsStaleSavesButPermitsIndependentAnalysis() {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); store.save(first);
        store.commitSuccessor("c1", 4, "sealed", 0, child(first, "c2"));
        var stale = store.load("c1").orElseThrow(); stale.setContentVersion(5);
        assertThrows(IllegalStateException.class, () -> store.save(stale));
        assertThrows(IllegalStateException.class, () -> store.saveIfVersion(stale, 4, BatchStatus.OPEN));
        var measured = store.load("c1").orElseThrow(); measured.setQueriesSpent(9); store.save(measured);
        store.saveAnalysisSelection(selection("c1", "later-analysis", 0));
        assertEquals(1, store.analysisSelections("c1").size());
        assertEquals(9, store.load("c1").orElseThrow().getQueriesSpent());
        assertEquals(0, store.load("c2").orElseThrow().getEvolution().parentAnalysisRevision());
    }

    @Test void inheritedReviewsAreDurableAndNewDecisionsRemainAppendOnly() {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); store.save(first);
        var second = child(first, "c2"); var source = second.getEvolution();
        var selections = List.of(new BatchReviewDecision.Selection("change", new BatchChangeStep("quiz", List.of(), null)));
        var inherited = new BatchReviewDecision("c2", selections, BatchReviewDecision.Status.APPROVED, "Human", Instant.EPOCH);
        second.setEvolution(new CorrectionEvolution.Lineage("c1", 4, 0, "op", "sealed",
                source.baseCourse(), source.sourceCourse(), List.of(inherited)));
        store.commitSuccessor("c1", 4, "sealed", 0, second);
        var reloaded = new FileSystemBatchStore(root);
        assertEquals(List.of(inherited), reloaded.reviews("c2"));
        reloaded.saveReview(new BatchReviewDecision("c2", selections, BatchReviewDecision.Status.PENDING, "Recheck", Instant.now(), 1L));
        assertEquals(List.of(BatchReviewDecision.Status.APPROVED, BatchReviewDecision.Status.PENDING),
                store.reviews("c2").stream().map(BatchReviewDecision::status).toList());
        var changedOrigin = store.load("c2").orElseThrow(); changedOrigin.setEvolution(null);
        assertThrows(IllegalStateException.class, () -> store.save(changedOrigin));
    }

    @Test void serializationFailureLeavesParentAndChildUntouched() throws IOException {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); store.save(first); byte[] before = Files.readAllBytes(file("c1"));
        var broken = new CorrectionBatch() { public String getBomb() { throw new IllegalStateException("simulated cut"); } };
        broken.setId("c2"); broken.setEvolution(child(first, "c2").getEvolution());
        assertThrows(UncheckedIOException.class, () -> store.commitSuccessor("c1", 4, "sealed", 0, broken));
        assertArrayEquals(before, Files.readAllBytes(file("c1")));
        assertFalse(Files.exists(file("c2")));
        assertFalse(store.hasSuccessor("c1"));
    }

    @Test void malformedLineageFailsFastInsteadOfHanging() throws IOException {
        Files.createDirectories(file("bad").getParent());
        Files.writeString(file("bad"), "{\"evolution\":{");
        assertThrows(UncheckedIOException.class, () -> new FileSystemBatchStore(root).hasSuccessor("c1"));
    }

    @Test void aSavedRootCheckpointCannotBeOverwrittenEvenBeforeCreatingASuccessor() {
        var store = new FileSystemBatchStore(root);
        var first = batch("c1", 1); var base = new CourseEntity(); base.setTitle("Original Base");
        first.setBaseCourseSnapshot(base); store.save(first);
        first.getBaseCourseSnapshot().setTitle("Changes only the returned copy");
        store.save(first);
        assertEquals("Original Base", store.load("c1").orElseThrow().getBaseCourseSnapshot().getTitle());
        base.setTitle("Another Base"); first.setBaseCourseSnapshot(base);
        assertThrows(IllegalStateException.class, () -> store.save(first));
        assertEquals("Original Base", store.load("c1").orElseThrow().getBaseCourseSnapshot().getTitle());
    }

    private static int attempt(FileSystemBatchStore store, CorrectionBatch child, CountDownLatch start) throws InterruptedException {
        start.await();
        try { store.commitSuccessor("c1", 4, "sealed", 0, child); return 1; }
        catch (IllegalStateException conflict) { return 0; }
    }
    private Path file(String id) { return root.resolve(".content-audit/batches/" + id + ".json"); }
    private static AnalysisSelection selection(String id, String operation, int expected) {
        return new AnalysisSelection(operation, id, "correction", "integrate", "run", null, "analyzer", List.of("quiz"),
                expected, 4, "sealed", "Human", Instant.EPOCH);
    }
    private static CorrectionBatch batch(String id, int revision) {
        var b = new CorrectionBatch(); b.setId(id); b.setLevel(CefrLevel.A1); b.setStatus(BatchStatus.OPEN);
        b.setCorrectionRevision(revision); b.setContentVersion(4); b.setBaselineAuditId("baseline");
        b.setOpenedAt(Instant.EPOCH.plusSeconds(revision)); return b;
    }
    private static CorrectionBatch child(CorrectionBatch parent, String id) {
        var child = batch(id, parent.getCorrectionRevision() + 1);
        var base = new CourseEntity(); base.setTitle("base");
        var source = new CourseEntity(); source.setTitle("source");
        child.setEvolution(new CorrectionEvolution.Lineage(parent.getId(), 4, 0, "op-" + id, "sealed", base, source, List.of()));
        return child;
    }
}
