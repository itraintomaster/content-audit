package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.refinerdomain.RefinementPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Package-private filesystem helper for delete and list operations on the
 * .content-audit/ store directories, following the same naming conventions as
 * FileSystemAuditReportStore and FileSystemRefinementPlanStore.
 *
 * This helper exists because the AuditReportStore and RefinementPlanStore interfaces
 * do not expose delete() or list() for plans, and modifying those interfaces or
 * infrastructure implementations is out of scope for this CLI restructure (R019).
 */
final class StoreHelper {

    private static final String AUDITS_SUBDIR = ".content-audit/audits";
    private static final String PLANS_SUBDIR = ".content-audit/plans";
    private static final String AUDIT_PREFIX = "audit-";
    private static final String PLAN_PREFIX = "plan-";
    private static final String JSON_SUFFIX = ".json";

    private StoreHelper() {}

    // -------------------------------------------------------------------------
    // Audits
    // -------------------------------------------------------------------------

    static boolean deleteAudit(Path baseDir, String auditId) {
        if (baseDir == null) {
            baseDir = Path.of(System.getProperty("user.dir"));
        }
        Path file = baseDir.resolve(AUDITS_SUBDIR).resolve(AUDIT_PREFIX + auditId + JSON_SUFFIX);
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Files.delete(file);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete audit file: " + file + ": " + e.getMessage(), e);
        }
    }

    /** Returns audit ids sorted by filename (timestamp) ascending. */
    static List<String> listAuditIds(Path baseDir) {
        if (baseDir == null) {
            baseDir = Path.of(System.getProperty("user.dir"));
        }
        Path auditsDir = baseDir.resolve(AUDITS_SUBDIR);
        if (!Files.isDirectory(auditsDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(auditsDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(AUDIT_PREFIX)
                              && p.getFileName().toString().endsWith(JSON_SUFFIX))
                    .map(p -> {
                        String fn = p.getFileName().toString();
                        return fn.substring(AUDIT_PREFIX.length(), fn.length() - JSON_SUFFIX.length());
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list audit files: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Plans
    // -------------------------------------------------------------------------

    static boolean deletePlan(Path baseDir, String planId) {
        if (baseDir == null) {
            baseDir = Path.of(System.getProperty("user.dir"));
        }
        Path file = baseDir.resolve(PLANS_SUBDIR).resolve(PLAN_PREFIX + planId + JSON_SUFFIX);
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Files.delete(file);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete plan file: " + file + ": " + e.getMessage(), e);
        }
    }

    /** Returns plan files sorted by filename ascending. Each entry is the plan-id (not filename). */
    static List<String> listPlanIds(Path baseDir) {
        if (baseDir == null) {
            baseDir = Path.of(System.getProperty("user.dir"));
        }
        Path plansDir = baseDir.resolve(PLANS_SUBDIR);
        if (!Files.isDirectory(plansDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(plansDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(PLAN_PREFIX)
                              && p.getFileName().toString().endsWith(JSON_SUFFIX))
                    .map(p -> {
                        String fn = p.getFileName().toString();
                        return fn.substring(PLAN_PREFIX.length(), fn.length() - JSON_SUFFIX.length());
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list plan files: " + e.getMessage(), e);
        }
    }

    /**
     * Loads plan metadata (id + createdAt) from a plan JSON file without full deserialization.
     * Returns empty if the file cannot be read or does not match expected shape.
     */
    static Optional<RefinementPlan> loadPlan(Path baseDir, String planId) {
        if (baseDir == null) {
            baseDir = Path.of(System.getProperty("user.dir"));
        }
        Path file = baseDir.resolve(PLANS_SUBDIR).resolve(PLAN_PREFIX + planId + JSON_SUFFIX);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            ObjectMapper om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return Optional.of(om.readValue(file.toFile(), RefinementPlan.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Lists all plans sorted by createdAt descending (most recent first).
     * Plans whose createdAt cannot be read are sorted last.
     */
    static List<RefinementPlan> listPlansSortedByRecency(Path baseDir) {
        List<String> ids = listPlanIds(baseDir);
        List<RefinementPlan> plans = new ArrayList<>();
        for (String id : ids) {
            loadPlan(baseDir, id).ifPresent(plans::add);
        }
        plans.sort(Comparator.comparing(
                p -> p.getCreatedAt() != null ? p.getCreatedAt() : java.time.Instant.EPOCH,
                Comparator.reverseOrder()
        ));
        return plans;
    }

    /**
     * Lists all plans referencing the given audit id.
     */
    static List<String> listPlanIdsReferencingAudit(Path baseDir, String auditId) {
        List<String> ids = listPlanIds(baseDir);
        List<String> result = new ArrayList<>();
        for (String id : ids) {
            Optional<RefinementPlan> planOpt = loadPlan(baseDir, id);
            planOpt.ifPresent(p -> {
                if (auditId.equals(p.getSourceAuditId())) {
                    result.add(id);
                }
            });
        }
        return result;
    }
}
