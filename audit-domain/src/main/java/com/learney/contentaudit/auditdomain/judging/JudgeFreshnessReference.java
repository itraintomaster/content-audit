package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeKind;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F-ALDIA-R003: the judge of today a verdict is marked against -- its version, the versions that are
 * that same version (F-ALDIA-R002), whether it approved its exam and the runtimes it approved with --
 * and the names of its versions (F-ALDIA-R004). Marking is pure: it consults and writes nothing, and
 * it never changes which verdict is reused (inv. 2 and 5).
 */
public final class JudgeFreshnessReference {
    private static final Pattern RUNTIME_TEXT = Pattern.compile(
            "(EXPLICIT|LEGACY_UNKNOWN)\\[provider=(.*), model=(.*), reasoning=(.*)]");

    private final String judgeName;

    private final String currentVersion;

    private final Set<String> sameVersions;

    private final boolean approved;

    private final Set<EvaluationRuntimeIdentity> approvedRuntimes;

    private final JudgeVersionNamer namer;

    public JudgeFreshnessReference(String judgeName, String currentVersion, Set<String> sameVersions,
            boolean approved, Set<EvaluationRuntimeIdentity> approvedRuntimes, JudgeVersionNamer namer) {
        this.judgeName = judgeName;
        this.currentVersion = currentVersion;
        Set<String> same = new LinkedHashSet<>();
        if (currentVersion != null) {
            same.add(currentVersion);
        }
        if (sameVersions != null) {
            same.addAll(sameVersions);
        }
        this.sameVersions = Set.copyOf(same);
        this.approved = approved && currentVersion != null;
        this.approvedRuntimes = approvedRuntimes != null ? Set.copyOf(approvedRuntimes) : Set.of();
        this.namer = namer;
    }

    /**
     * Today's judge as its standing says it, with the runtimes its version approved with: for each
     * runtime, the last exam of today's version (or one that is the same) with it is APPROVED.
     */
    public static JudgeFreshnessReference of(JudgeExamStanding standing, List<JudgeExamRecord> history,
            ZoneId zone, Clock clock) {
        Set<String> same = new LinkedHashSet<>();
        if (standing.getJudgeVersion() != null) {
            same.add(standing.getJudgeVersion());
        }
        if (standing.getCompatibleJudgeVersions() != null) {
            same.addAll(standing.getCompatibleJudgeVersions());
        }
        Map<EvaluationRuntimeIdentity, JudgeExamStatus> lastByRuntime = new LinkedHashMap<>();
        for (JudgeExamRecord record : history != null ? history : List.<JudgeExamRecord>of()) {
            if (record != null && same.contains(record.getJudgeVersion())) {
                lastByRuntime.put(record.getRuntimeIdentity(), record.getStatus());
            }
        }
        Set<EvaluationRuntimeIdentity> runtimes = new LinkedHashSet<>();
        lastByRuntime.forEach((runtime, status) -> {
            if (status == JudgeExamStatus.APPROVED) {
                runtimes.add(runtime);
            }
        });
        boolean approved = standing.getStatus() == JudgeExamStatus.APPROVED;
        if (approved && standing.getRuntimeIdentity() != null) {
            runtimes.add(standing.getRuntimeIdentity());
        }
        return new JudgeFreshnessReference(standing.getJudgeName(), standing.getJudgeVersion(), same, approved,
                runtimes, new JudgeVersionNamer(history, List.of(same), zone, clock));
    }

    /**
     * The mark of a verdict given by {@code version} with {@code runtime}. Against today's version,
     * never against the other end of a comparison (inv. 1); the runtime is looked at first.
     */
    public JudgeVerdictMark mark(String version, EvaluationRuntimeIdentity runtime) {
        if (!approved) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.JUDGE_NOT_APPROVED);
        }
        if (version == null || runtime == null || !runtime.isExplicit()) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.UNKNOWN_PROVENANCE);
        }
        if (!approvedRuntimes.contains(runtime)) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME);
        }
        if (!sameVersions.contains(version)) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.EARLIER_VERSION);
        }
        return JudgeVerdictMark.UP_TO_DATE;
    }

    /**
     * F-MEDJ-R002: whether a verdict is today's judge. An approved judge asks what {@link #mark} asks; one whose
     * version of today did not approve its exam still has a version of today, and only the version is looked at.
     */
    public boolean isOfToday(String version, EvaluationRuntimeIdentity runtime) {
        if (!approved) {
            return version != null && sameVersions.contains(version);
        }
        return mark(version, runtime).isUpToDate();
    }

    /**
     * A runtime as an analysis carries it: the identity itself, its fields as a map, or its text
     * ({@code EXPLICIT[provider=…, model=…, reasoning=…]}). Anything else has no provenance: null.
     */
    public static EvaluationRuntimeIdentity runtimeOf(Object value) {
        if (value instanceof EvaluationRuntimeIdentity identity) {
            return identity;
        }
        try {
            if (value instanceof Map<?, ?> map && map.get("kind") != null) {
                return new EvaluationRuntimeIdentity(EvaluationRuntimeKind.valueOf(map.get("kind").toString()),
                        text(map.get("providerName")), text(map.get("modelName")), text(map.get("reasoningEffort")));
            }
            if (value instanceof String text) {
                Matcher matcher = RUNTIME_TEXT.matcher(text.trim());
                if (matcher.matches()) {
                    return new EvaluationRuntimeIdentity(EvaluationRuntimeKind.valueOf(matcher.group(1)),
                            matcher.group(2), matcher.group(3), matcher.group(4));
                }
            }
        } catch (IllegalArgumentException incomplete) {
            return null;
        }
        return null;
    }

    private static String text(Object value) {
        return value != null ? value.toString() : null;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public Set<String> getSameVersions() {
        return sameVersions;
    }

    public boolean isApproved() {
        return approved;
    }

    public Set<EvaluationRuntimeIdentity> getApprovedRuntimes() {
        return approvedRuntimes;
    }

    public JudgeVersionNamer getNamer() {
        return namer;
    }

    /** The name of today's version: «la del 13/10, 22:12», or «sin aprobar (…)». */
    public String currentVersionName() {
        return approved ? namer.name(currentVersion) : "sin aprobar (" + JudgeVersionNamer.shortCode(currentVersion) + ")";
    }

    public Optional<Instant> currentApprovedAt() {
        return approved ? namer.firstApproval(currentVersion) : Optional.empty();
    }
}
