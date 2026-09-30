package com.learney.contentaudit.auditinfrastructure;
import com.learney.contentaudit.auditdomain.contextnumbers.AuditDigest;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.contextnumbers.DigestNode;
import javax.annotation.processing.Generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditReportStore;
import com.learney.contentaudit.auditdomain.AuditReportSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Filesystem adapter that persists AuditReport objects as JSON files.
 *
 * Storage location: {@code <baseDir>/.content-audit/audits/}
 * File naming:      {@code audit-<timestamp>.json}
 *
 * Next to each report it writes its digest (F-HALL-R010), in a sibling directory so that
 * {@link #list()} and {@link #loadLatest()} never take it for a report:
 * {@code <baseDir>/.content-audit/audit-digests/audit-<timestamp>.json}. The digest has no course
 * entities -- ids, labels, numbers, findings and what was left unevaluated on each node, copied
 * from the report and never recomputed -- so whoever draws reads 10 to 15 MB instead of the 146
 * of the report.
 *
 * The report is serialized with polymorphic type information for
 * {@code AuditableEntity} and {@code NodeDiagnoses}. The circular
 * {@code AuditNode.parent} reference is omitted during serialization
 * and reconstructed on deserialization.
 */
public class FileSystemAuditReportStore implements AuditReportStore {

    private static final String AUDITS_SUBDIR = ".content-audit/audits";
    static final String DIGESTS_SUBDIR = ".content-audit/audit-digests";
    /** The course node has no entity; the digest names it as the plan and the findings do. */
    private static final String COURSE_NODE_ID = "root";
    private static final String COURSE_NODE_LABEL = "Curso";
    private static final String FILE_PREFIX = "audit-";
    private static final String FILE_SUFFIX = ".json";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

    private final Path baseDir;
    private final ObjectMapper objectMapper;

public FileSystemAuditReportStore(Path baseDir) {
    this.baseDir = baseDir;
    this.objectMapper = AuditReportObjectMapper.create();
}

    // -------------------------------------------------------------------------
    // AuditReportStore
    // -------------------------------------------------------------------------

    @Override
    public String save(AuditReport report) {
        Path auditsDir = resolveAuditsDir();
        try {
            Files.createDirectories(auditsDir);
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to create audit storage directory: " + e.getMessage(), e);
        }

        String id = TIMESTAMP_FORMATTER.format(Instant.now());
        Path file = auditsDir.resolve(FILE_PREFIX + id + FILE_SUFFIX);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), report);
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to write audit report to " + file + ": " + e.getMessage(), e);
        }
        saveDigest(id, report);
        return id;
    }

    private void saveDigest(String id, AuditReport report) {
        Path digestsDir = resolveDigestsDir();
        Path file = digestsDir.resolve(FILE_PREFIX + id + FILE_SUFFIX);
        try {
            Files.createDirectories(digestsDir);
            objectMapper.writeValue(file.toFile(), toDigest(id, report));
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to write audit digest to " + file + ": " + e.getMessage(), e);
        }
    }

    /** Copies, never computes: every number, finding and declaration comes from the report. */
    static AuditDigest toDigest(String id, AuditReport report) {
        return new AuditDigest(id, report.getRoot() != null ? toDigestNode(report.getRoot()) : null);
    }

    private static DigestNode toDigestNode(AuditNode node) {
        String nodeId = COURSE_NODE_ID;
        String label = COURSE_NODE_LABEL;
        if (node.getEntity() != null) {
            nodeId = node.getEntity().getId() != null ? node.getEntity().getId() : COURSE_NODE_ID;
            label = node.getEntity().getLabel() != null ? node.getEntity().getLabel() : nodeId;
        }
        List<DigestNode> children = new ArrayList<>();
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                children.add(toDigestNode(child));
            }
        }
        return new DigestNode(nodeId, node.getTarget(), label, node.getNumbers(),
                node.getFindings() != null ? node.getFindings() : List.of(),
                node.getUnevaluatedBy() != null ? node.getUnevaluatedBy() : List.of(),
                children);
    }

    @Override
    public Optional<AuditReport> load(String id) {
        Path file = resolveAuditsDir().resolve(FILE_PREFIX + id + FILE_SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(loadFromFile(file));
    }

    @Override
    public Optional<AuditReport> loadLatest() {
        Path auditsDir = resolveAuditsDir();
        if (!Files.isDirectory(auditsDir)) {
            return Optional.empty();
        }

        try (Stream<Path> stream = Files.list(auditsDir)) {
            Optional<Path> latestFile = stream
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX)
                              && p.getFileName().toString().endsWith(FILE_SUFFIX))
                    .max(Comparator.comparing(p -> p.getFileName().toString()));

            return latestFile.map(this::loadFromFile);
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to list audit files: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditReportSummary> list() {
        Path auditsDir = resolveAuditsDir();
        if (!Files.isDirectory(auditsDir)) {
            return new ArrayList<>();
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(auditsDir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX)
                              && p.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to list audit files: " + e.getMessage(), e);
        }

        List<AuditReportSummary> summaries = new ArrayList<>();
        for (Path file : files) {
            try {
                String id = extractIdFromFilename(file.getFileName().toString());
                Instant timestamp = parseTimestampFromId(id);
                // F-HALL-R010: the score of an analysis is the vocabulary score the engine
                // published on its course, read from the digest when it exists -- never an
                // average of the course keys (the eleven of them gave 73,4 % instead of 73,9 %).
                Optional<AuditDigest> digest = loadDigest(id);
                if (digest.isPresent()) {
                    summaries.add(new AuditReportSummary(id, timestamp, "",
                            publishedScore(digest.get().getRoot() != null ? digest.get().getRoot().getNumbers() : null)));
                    continue;
                }
                AuditReport report = loadFromFile(file);
                String courseName = extractCourseName(report);
                double overallScore = publishedScore(report.getRoot() != null ? report.getRoot().getNumbers() : null);
                summaries.add(new AuditReportSummary(id, timestamp, courseName, overallScore));
            } catch (Exception e) {
                // Skip files that cannot be parsed
            }
        }
        return summaries;
    }

    @Override
    public Optional<AuditDigest> loadDigest(String id) {
        Path file = resolveDigestsDir().resolve(FILE_PREFIX + id + FILE_SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), AuditDigest.class));
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to read audit digest from " + file + ": " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Path resolveAuditsDir() {
        return baseDir.resolve(AUDITS_SUBDIR);
    }

    private Path resolveDigestsDir() {
        return baseDir.resolve(DIGESTS_SUBDIR);
    }

    private AuditReport loadFromFile(Path file) {
        try {
            AuditReport report = objectMapper.readValue(file.toFile(), AuditReport.class);
            if (report.getRoot() != null) {
                rebuildParentReferences(report.getRoot(), null);
            }
            return report;
        } catch (IOException e) {
            throw new AuditPersistenceException("Failed to read audit report from " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Walks the tree and sets each node's parent field so that
     * {@link AuditNode#ancestor(com.learney.contentaudit.auditdomain.AuditTarget)} works correctly.
     */
    private void rebuildParentReferences(AuditNode node, AuditNode parent) {
        node.setParent(parent);
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                rebuildParentReferences(child, node);
            }
        }
    }

    private String extractIdFromFilename(String filename) {
        // "audit-2026-04-05T10-30-00.json" → "2026-04-05T10-30-00"
        String withoutPrefix = filename.substring(FILE_PREFIX.length());
        return withoutPrefix.substring(0, withoutPrefix.length() - FILE_SUFFIX.length());
    }

    private Instant parseTimestampFromId(String id) {
        try {
            return TIMESTAMP_FORMATTER.parse(id, Instant::from);
        } catch (Exception e) {
            return Instant.EPOCH;
        }
    }

    private String extractCourseName(AuditReport report) {
        if (report.getRoot() == null || report.getRoot().getEntity() == null) {
            return "";
        }
        String label = report.getRoot().getEntity().getLabel();
        return label != null ? label : "";
    }

    /**
     * The published vocabulary score, or NaN when the analysis published none (a report saved
     * before the contract): it is shown as unknown, never recomputed from the keys.
     */
    private static double publishedScore(ContextNumbers numbers) {
        if (numbers == null || numbers.getVocabularyScore() == null) {
            return Double.NaN;
        }
        return numbers.getVocabularyScore();
    }

}
