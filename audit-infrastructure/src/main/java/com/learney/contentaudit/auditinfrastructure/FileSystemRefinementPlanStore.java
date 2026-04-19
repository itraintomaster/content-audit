package com.learney.contentaudit.auditinfrastructure;
import javax.annotation.processing.Generated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import com.learney.contentaudit.refinerdomain.RefinementPlanStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Filesystem adapter that persists {@link RefinementPlan} objects as JSON files.
 *
 * Storage location: {@code <baseDir>/.content-audit/plans/}
 * File naming:      {@code plan-<id>.json}
 *
 * The plan is serialized with JavaTimeModule for Instant support.
 * The model contains no polymorphic types or circular references.
 */
public class FileSystemRefinementPlanStore implements RefinementPlanStore {

    private static final String PLANS_SUBDIR = ".content-audit/plans";
    private static final String FILE_PREFIX = "plan-";
    private static final String FILE_SUFFIX = ".json";

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    /**
     * Production constructor: resolves storage relative to current working directory.
     */
    public FileSystemRefinementPlanStore() {
        this(Path.of(System.getProperty("user.dir")));
    }

    /**
     * Testable constructor: resolves storage relative to the supplied base path.
     *
     * @param baseDir root directory under which {@code .content-audit/plans/} is created
     */
    public FileSystemRefinementPlanStore(Path baseDir) {
        this.baseDir = baseDir;
        this.objectMapper = createObjectMapper();
    }

    // -------------------------------------------------------------------------
    // RefinementPlanStore
    // -------------------------------------------------------------------------

    @Override
    public String save(RefinementPlan plan) {
        Path plansDir = resolvePlansDir();
        try {
            Files.createDirectories(plansDir);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to create plan storage directory: " + e.getMessage(), e);
        }

        String id = plan.getId();
        Path file = plansDir.resolve(FILE_PREFIX + id + FILE_SUFFIX);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), plan);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to write refinement plan to " + file + ": " + e.getMessage(), e);
        }
        return id;
    }

    @Override
    public Optional<RefinementPlan> load(String id) {
        Path file = resolvePlansDir().resolve(FILE_PREFIX + id + FILE_SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(loadFromFile(file));
    }

    @Override
    public Optional<RefinementPlan> loadLatest() {
        Path plansDir = resolvePlansDir();
        if (!Files.isDirectory(plansDir)) {
            return Optional.empty();
        }

        try (Stream<Path> stream = Files.list(plansDir)) {
            Optional<Path> latestFile = stream
                    .filter(p -> p.getFileName().toString().startsWith(FILE_PREFIX)
                              && p.getFileName().toString().endsWith(FILE_SUFFIX))
                    .max(Comparator.comparing(p -> p.getFileName().toString()));

            return latestFile.map(this::loadFromFile);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to list plan files: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Path resolvePlansDir() {
        return baseDir.resolve(PLANS_SUBDIR);
    }

    private RefinementPlan loadFromFile(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), RefinementPlan.class);
        } catch (IOException e) {
            throw new AuditPersistenceException(
                    "Failed to read refinement plan from " + file + ": " + e.getMessage(), e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        return mapper;
    }
}
