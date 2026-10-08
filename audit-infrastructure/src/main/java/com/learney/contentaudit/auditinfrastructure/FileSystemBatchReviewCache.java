package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.revisiondomain.batch.BatchReview;
import com.learney.contentaudit.revisiondomain.batch.BatchReviewCache;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/** Disposable, campaign-local projection; never rewrites a batch, artifact, plan or review event. */
public final class FileSystemBatchReviewCache implements BatchReviewCache {
    private static final String SCHEMA = "batch-review-v1";
    private static final List<String> INPUTS = List.of("batches", "batch-reviews", "revisions", "plans");
    private final Path auditDir;
    private final ObjectMapper mapper = JsonFiles.mapper();

    public FileSystemBatchReviewCache(Path baseDir) {
        auditDir = baseDir.resolve(".content-audit");
    }

    @Override
    public Optional<String> version() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, SCHEMA);
            for (String input : INPUTS) {
                Path dir = auditDir.resolve(input);
                update(digest, input);
                if (!Files.isDirectory(dir)) continue;
                try (var files = Files.walk(dir)) {
                    for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json"))
                            .sorted().toList()) {
                        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                        update(digest, auditDir.relativize(file).toString());
                        update(digest, attributes.size() + ":" + attributes.lastModifiedTime() + ":" + attributes.fileKey());
                        // Content is part of the key too: tools may preserve timestamps or rewrite in place.
                        try (var content = Files.newInputStream(file)) {
                            byte[] buffer = new byte[64 * 1024];
                            int read;
                            while ((read = content.read(buffer)) >= 0) digest.update(buffer, 0, read);
                        }
                        var after = Files.readAttributes(file, BasicFileAttributes.class);
                        if (attributes.size() != after.size()
                                || !attributes.lastModifiedTime().equals(after.lastModifiedTime())
                                || !java.util.Objects.equals(attributes.fileKey(), after.fileKey())) return Optional.empty();
                    }
                }
            }
            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | java.io.UncheckedIOException e) {
            // Concurrent replacement/removal or an unreadable source: read the authoritative stores instead.
            return Optional.empty();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    @Override
    public Optional<BatchReview> load(String requestedBatchId, String version) {
        Path file = file(requestedBatchId);
        try {
            var cached = JsonFiles.read(mapper, file, Entry.class);
            return cached.filter(entry -> version.equals(entry.version()) && entry.review() != null
                            && (requestedBatchId == null || requestedBatchId.equals(entry.review().batchId())))
                    .map(Entry::review);
        } catch (AuditPersistenceException e) {
            return Optional.empty(); // A missing/corrupt cache is not missing review data.
        }
    }

    @Override
    public void save(String requestedBatchId, String version, BatchReview review) {
        Path file = file(requestedBatchId);
        if (!version().filter(version::equals).isPresent()) return;
        try {
            JsonFiles.writeAtomically(mapper, file, new Entry(version, review));
        } catch (AuditPersistenceException e) {
            // The optimization must not fail a read, nor an already persisted human decision.
        }
    }

    private Path file(String batchId) {
        if (batchId != null && !batchId.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException("Identidad de tanda inválida.");
        }
        return auditDir.resolve("batch-review-cache").resolve(batchId == null ? "current.json" : "batch-" + batchId + ".json");
    }

    private static void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    private record Entry(String version, BatchReview review) {}
}
