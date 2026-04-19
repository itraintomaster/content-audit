package com.learney.contentaudit.auditinfrastructure;
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
 * The report is serialized with polymorphic type information for
 * {@code AuditableEntity} and {@code NodeDiagnoses}. The circular
 * {@code AuditNode.parent} reference is omitted during serialization
 * and reconstructed on deserialization.
 */
public class FileSystemAuditReportStore implements AuditReportStore {

    private static final String AUDITS_SUBDIR = ".content-audit/audits";
    private static final String FILE_PREFIX = "audit-";
    private static final String FILE_SUFFIX = ".json";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    /**
     * Production constructor: resolves storage relative to current working directory.
     */
    public FileSystemAuditReportStore() {
        this(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Testable constructor: resolves storage relative to the supplied base path.
     *
     * @param baseDir root directory under which {@code .content-audit/audits/} is created
     */
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
        return id;
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
                AuditReport report = loadFromFile(file);
                String id = extractIdFromFilename(file.getFileName().toString());
                Instant timestamp = parseTimestampFromId(id);
                String courseName = extractCourseName(report);
                double overallScore = extractOverallScore(report);
                summaries.add(new AuditReportSummary(id, timestamp, courseName, overallScore));
            } catch (Exception e) {
                // Skip files that cannot be parsed
            }
        }
        return summaries;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Path resolveAuditsDir() {
        return baseDir.resolve(AUDITS_SUBDIR);
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

    private double extractOverallScore(AuditReport report) {
        if (report.getRoot() == null || report.getRoot().getScores() == null) {
            return 0.0;
        }
        return report.getRoot().getScores().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
}
