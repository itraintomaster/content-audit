package com.learney.contentaudit.auditinfrastructure;
import javax.annotation.processing.Generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.revisiondomain.RevisionArtifact;
import com.learney.contentaudit.revisiondomain.RevisionArtifactStore;
import com.learney.contentaudit.revisiondomain.RevisionVerdict;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Filesystem adapter that persists RevisionArtifact objects as JSON files.
 *
 * Storage location: {@code <baseDir>/.content-audit/revisions/<planId>/<proposalId>.json}
 *
 * Layout:
 * <pre>
 *   .content-audit/
 *     revisions/
 *       &lt;planId&gt;/
 *         &lt;proposalId&gt;.json
 * </pre>
 *
 * Rules: R008 (stored under .content-audit/revisions/), R009 (&lt;planId&gt;/&lt;proposalId&gt; organisation),
 * R010 (all fields including verdict and rejectionReason recoverable on load).
 */
public class FileSystemRevisionArtifactStore implements RevisionArtifactStore {

    private static final String REVISIONS_SUBDIR = ".content-audit/revisions";
    private static final String FILE_SUFFIX = ".json";

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    /**
     * Production constructor: resolves storage relative to current working directory.
     */
    public FileSystemRevisionArtifactStore() {
        this(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Testable constructor: resolves storage relative to the supplied base path.
     *
     * @param baseDir root directory under which {@code .content-audit/revisions/} is created
     */
    public FileSystemRevisionArtifactStore(Path baseDir) {
        this.baseDir = baseDir;
        this.objectMapper = createObjectMapper();
    }

    // -------------------------------------------------------------------------
    // RevisionArtifactStore
    // -------------------------------------------------------------------------

    @Override
    public String save(RevisionArtifact artifact) {
        String planId = artifact.getProposal().getPlanId();
        String proposalId = artifact.getProposal().getProposalId();

        Path planDir = resolveRevisionsDir().resolve(planId);
        try {
            Files.createDirectories(planDir);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to create revisions directory for plan " + planId + ": " + e.getMessage(), e);
        }

        Path file = planDir.resolve(proposalId + FILE_SUFFIX);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), artifact);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to write revision artifact to " + file + ": " + e.getMessage(), e);
        }
        return proposalId;
    }

    @Override
    public Optional<RevisionArtifact> load(String planId, String proposalId) {
        Path file = resolveRevisionsDir().resolve(planId).resolve(proposalId + FILE_SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(loadFromFile(file));
    }

    @Override
    public List<RevisionArtifact> listByPlan(String planId) {
        Path planDir = resolveRevisionsDir().resolve(planId);
        if (!Files.isDirectory(planDir)) {
            return new ArrayList<>();
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(planDir)) {
            files = stream
                    .filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to list revision files for plan " + planId + ": " + e.getMessage(), e);
        }

        List<RevisionArtifact> artifacts = new ArrayList<>();
        for (Path file : files) {
            artifacts.add(loadFromFile(file));
        }
        return artifacts;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Path resolveRevisionsDir() {
        return baseDir.resolve(REVISIONS_SUBDIR);
    }

    private RevisionArtifact loadFromFile(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), RevisionArtifact.class);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to read revision artifact from " + file + ": " + e.getMessage(), e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java time support (Instant)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail on unknown properties (forward-compatibility)
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        return mapper;
    }

    @Override
    public Optional<RevisionArtifact> findByProposalId(String proposalId, Optional<String> planId) {
        if (planId.isPresent()) {
            // Direct path: look under <baseDir>/.content-audit/revisions/<planId>/<proposalId>.json
            return load(planId.get(), proposalId);
        }

        // Scan path: iterate all plan subdirectories
        Path revisionsDir = resolveRevisionsDir();
        if (!Files.isDirectory(revisionsDir)) {
            return Optional.empty();
        }

        List<Path> planDirs;
        try (Stream<Path> stream = Files.list(revisionsDir)) {
            planDirs = stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to list plan directories under " + revisionsDir + ": " + e.getMessage(), e);
        }

        for (Path planDir : planDirs) {
            Path candidate = planDir.resolve(proposalId + FILE_SUFFIX);
            if (Files.exists(candidate)) {
                return Optional.of(loadFromFile(candidate));
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean hasPendingProposalForTask(String planId, String taskId) {
        Path planDir = resolveRevisionsDir().resolve(planId);
        if (!Files.isDirectory(planDir)) {
            return false;
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(planDir)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(FILE_SUFFIX)).toList();
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to list revision files for plan " + planId + ": " + e.getMessage(), e);
        }

        for (Path file : files) {
            RevisionArtifact artifact = loadFromFile(file);
            if (taskId.equals(artifact.getProposal().getTaskId())
                    && RevisionVerdict.PENDING_APPROVAL == artifact.getVerdict()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<RevisionArtifact> list() {
        Path revisionsDir = resolveRevisionsDir();
        if (!Files.isDirectory(revisionsDir)) {
            return new ArrayList<>();
        }

        List<Path> planDirs;
        try (Stream<Path> stream = Files.list(revisionsDir)) {
            planDirs = stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to list plan directories under " + revisionsDir + ": " + e.getMessage(), e);
        }

        List<RevisionArtifact> all = new ArrayList<>();
        for (Path planDir : planDirs) {
            String dirPlanId = planDir.getFileName().toString();
            all.addAll(listByPlan(dirPlanId));
        }
        return all;
    }

}
