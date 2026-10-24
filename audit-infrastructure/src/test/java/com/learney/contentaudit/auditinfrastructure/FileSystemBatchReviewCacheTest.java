package com.learney.contentaudit.auditinfrastructure;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemBatchReviewCacheTest {
    @TempDir Path workdir;

    @Test void legacyProjectionCannotHideNewSnapshotAndLineageFields() throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        for (String field : List.of("batch-review-v1", "batches", "batch-reviews", "revisions", "plans")) {
            digest.update(field.getBytes(java.nio.charset.StandardCharsets.UTF_8)); digest.update((byte) 0);
        }
        String legacy = java.util.HexFormat.of().formatHex(digest.digest());
        source("batch-review-cache/batch-batch.json", JsonFiles.mapper().writeValueAsString(
                java.util.Map.of("version", legacy, "review", review())));
        var cache = new FileSystemBatchReviewCache(workdir);
        assertNotEquals(legacy, cache.version().orElseThrow());
        assertTrue(cache.load("batch", cache.version().orElseThrow()).isEmpty());
    }

    @Test void projectionSurvivesAnotherProcessAndDoesNotChangeItsInputs() throws Exception {
        var cache = new FileSystemBatchReviewCache(workdir);
        source("batches/batch.json", "checkpoint");
        var version = cache.version().orElseThrow();
        var review = review();
        cache.save(null, version, review);
        assertEquals(review, new FileSystemBatchReviewCache(workdir).load(null, version).orElseThrow());
        assertEquals("checkpoint", Files.readString(workdir.resolve(".content-audit/batches/batch.json")));
        assertEquals(version, cache.version().orElseThrow(), "the cache does not invalidate itself");
    }

    @Test void everySourceFamilyInvalidatesTheProjectionIncludingReviewUndoAndHistory() throws Exception {
        var cache = new FileSystemBatchReviewCache(workdir);
        for (String path : List.of("batches/first.json", "batch-reviews/batch/decision.json",
                "revisions/plan/proposal.json", "plans/plan.json", "batches/next.json", "analysis-selections/batch.json")) {
            String before = cache.version().orElseThrow();
            cache.save("batch", before, review());
            source(path, "new input");
            String after = cache.version().orElseThrow();
            assertNotEquals(before, after, path);
            assertTrue(cache.load("batch", after).isEmpty(), path);
        }
    }

    @Test void replacementOrRemovalIsNotAnUnchangedInput() throws Exception {
        var cache = new FileSystemBatchReviewCache(workdir);
        var file = source("batches/batch.json", "one");
        var originalTime = Files.getLastModifiedTime(file);
        var version = cache.version().orElseThrow();
        var replacement = Files.createTempFile(file.getParent(), "replacement", ".tmp");
        Files.writeString(replacement, "two");
        Files.setLastModifiedTime(replacement, originalTime);
        Files.move(replacement, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertNotEquals(version, cache.version().orElseThrow(), "atomic replacement changes the file identity");
        version = cache.version().orElseThrow();
        Files.delete(file);
        assertNotEquals(version, cache.version().orElseThrow());
    }

    @Test void corruptCacheIsRebuiltAndAConcurrentInputCannotBeCachedAsCurrent() throws Exception {
        var cache = new FileSystemBatchReviewCache(workdir);
        String version = cache.version().orElseThrow();
        cache.save("batch", version, review());
        source("batch-review-cache/batch-batch.json", "broken JSON");
        assertTrue(cache.load("batch", version).isEmpty());
        source("batch-reviews/batch/new.json", "a decision");
        cache.save("batch", version, review());
        assertEquals("broken JSON", Files.readString(workdir.resolve(".content-audit/batch-review-cache/batch-batch.json")));
    }

    @Test void equalLengthEditWithPreservedTimestampStillInvalidatesCachedApprovals() throws Exception {
        var cache = new FileSystemBatchReviewCache(workdir);
        var file = source("batch-reviews/batch/decision.json", "APPROVED");
        var originalTime = Files.getLastModifiedTime(file);
        var version = cache.version().orElseThrow();
        Files.writeString(file, "REJECTED");
        Files.setLastModifiedTime(file, originalTime);
        assertNotEquals(version, cache.version().orElseThrow());
    }

    @Test void selectedHistoryCannotEscapeCampaignOrReuseCurrentAlias() {
        var cache = new FileSystemBatchReviewCache(workdir);
        String version = cache.version().orElseThrow();
        cache.save(null, version, review());
        assertTrue(cache.load("another", version).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> cache.load("../outside", version));
        assertThrows(IllegalArgumentException.class, () -> cache.save("..", version, review()));
    }

    @Test void anUnavailableCacheNeverFailsAnAuthoritativeReadOrAnAlreadySavedDecision() throws Exception {
        Files.createDirectories(workdir.resolve(".content-audit"));
        Files.writeString(workdir.resolve(".content-audit/batch-review-cache"), "not a directory");
        var cache = new FileSystemBatchReviewCache(workdir);
        String version = cache.version().orElseThrow();
        assertDoesNotThrow(() -> cache.save(null, version, review()));
        assertTrue(cache.load(null, version).isEmpty());
    }

    private Path source(String relative, String content) throws Exception {
        Path path = workdir.resolve(".content-audit").resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }

    private static BatchReview review() {
        return new BatchReview("batch", 1, 2, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
