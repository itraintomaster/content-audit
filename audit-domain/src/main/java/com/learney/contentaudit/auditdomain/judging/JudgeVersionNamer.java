package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * F-ALDIA-R004: how a version of a judge is named to José -- by the day it first approved its exam, in
 * local time: «la del 5/10». The hour joins the day when another version of the judge first approved
 * that same local day, and the year when it is not the current one. A version that never approved is
 * «sin aprobar» with its short code; a version is never named with a number.
 *
 * <p>Versions that are the same version (today's and the earlier one it is recorded as equal to,
 * F-ALDIA-R002) share their first approval. It reads only the history it is given.
 */
public final class JudgeVersionNamer {
    static final String UNKNOWN_PROVENANCE = "procedencia desconocida";

    private static final int SHORT_CODE = 8;

    private final Map<String, Set<String>> groupOf = new HashMap<>();

    private final Map<Set<String>, Instant> firstApprovalOf = new HashMap<>();

    private final ZoneId zone;

    private final int currentYear;

    public JudgeVersionNamer(List<JudgeExamRecord> history, Collection<? extends Collection<String>> sameVersions,
            ZoneId zone, Clock clock) {
        this.zone = zone;
        this.currentYear = ZonedDateTime.ofInstant(clock.instant(), zone).getYear();
        for (Collection<String> same : sameVersions != null ? sameVersions : List.<Collection<String>>of()) {
            Set<String> group = new LinkedHashSet<>();
            for (String version : same) {
                if (version != null) {
                    group.addAll(groupOf.getOrDefault(version, Set.of(version)));
                }
            }
            for (String version : group) {
                groupOf.put(version, group);
            }
        }
        for (JudgeExamRecord record : history != null ? history : List.<JudgeExamRecord>of()) {
            if (record == null || record.getJudgeVersion() == null || record.getRecordedAt() == null
                    || record.getStatus() != JudgeExamStatus.APPROVED) {
                continue;
            }
            // Re-approving does not rename a version: only its first approval counts (inv. 2).
            firstApprovalOf.merge(group(record.getJudgeVersion()), record.getRecordedAt(),
                    (a, b) -> a.isBefore(b) ? a : b);
        }
    }

    /** The instant the version first approved its exam, empty when it never did. */
    public Optional<Instant> firstApproval(String version) {
        return version == null ? Optional.empty() : Optional.ofNullable(firstApprovalOf.get(group(version)));
    }

    public boolean approved(String version) {
        return firstApproval(version).isPresent();
    }

    /** «la del 5/10», «la del 13/10, 22:12», or «sin aprobar (c8ca16c9)». */
    public String name(String version) {
        return date(version).map(date -> "la del " + date).orElseGet(() -> unapproved(version));
    }

    /** The name of a loose mark: «versión del 5/10», or «sin aprobar (c8ca16c9)». */
    public String looseName(String version) {
        return date(version).map(date -> "versión del " + date).orElseGet(() -> unapproved(version));
    }

    /** A runtime as its provenance says it: «llama3 local», «gpt-6.1-sol en codex-cli, medium», or «procedencia desconocida». */
    public static String runtimeName(EvaluationRuntimeIdentity runtime) {
        if (runtime == null || !runtime.isExplicit()) {
            return UNKNOWN_PROVENANCE;
        }
        String provider = runtime.getProviderName().toLowerCase(Locale.ROOT);
        if (provider.contains("ollama") || provider.contains("local") || provider.contains("lmstudio")
                || provider.contains("llama.cpp")) {
            return runtime.getModelName() + " local";
        }
        String effort = runtime.getReasoningEffort();
        return runtime.getModelName() + " en " + runtime.getProviderName()
                + (EvaluationRuntimeIdentity.NOT_APPLICABLE.equals(effort) ? "" : ", " + effort);
    }

    /** The first characters of the code; each part of a composed version keeps its own. */
    public static String shortCode(String version) {
        if (version == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String part : version.split("\\+", -1)) {
            parts.add(part.length() > SHORT_CODE ? part.substring(0, SHORT_CODE) : part);
        }
        return String.join("+", parts);
    }

    private String unapproved(String version) {
        return "sin aprobar (" + shortCode(version) + ")";
    }

    private Optional<String> date(String version) {
        Optional<Instant> first = firstApproval(version);
        if (first.isEmpty()) {
            return Optional.empty();
        }
        ZonedDateTime local = ZonedDateTime.ofInstant(first.get(), zone);
        LocalDate day = local.toLocalDate();
        StringBuilder date = new StringBuilder().append(day.getDayOfMonth()).append('/').append(day.getMonthValue());
        if (day.getYear() != currentYear) {
            date.append('/').append(day.getYear());
        }
        Set<String> own = group(version);
        boolean sharedDay = firstApprovalOf.entrySet().stream().anyMatch(other -> !other.getKey().equals(own)
                && ZonedDateTime.ofInstant(other.getValue(), zone).toLocalDate().equals(day));
        if (sharedDay) {
            date.append(String.format(Locale.ROOT, ", %d:%02d", local.getHour(), local.getMinute()));
        }
        return Optional.of(date.toString());
    }

    private Set<String> group(String version) {
        return groupOf.getOrDefault(version, Set.of(version));
    }
}
