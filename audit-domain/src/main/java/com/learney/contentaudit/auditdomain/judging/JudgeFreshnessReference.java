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

    private final Set<EvaluationRuntimeIdentity> judgeApprovedRuntimes;

    private final JudgeVersionNamer namer;

    private final List<JudgeEnablement> enabledWith;

    public JudgeFreshnessReference(String judgeName, String currentVersion, Set<String> sameVersions,
            boolean approved, Set<EvaluationRuntimeIdentity> approvedRuntimes, JudgeVersionNamer namer) {
        this(judgeName, currentVersion, sameVersions, approved, approvedRuntimes, namer, List.of());
    }

    public JudgeFreshnessReference(String judgeName, String currentVersion, Set<String> sameVersions,
            boolean approved, Set<EvaluationRuntimeIdentity> approvedRuntimes, JudgeVersionNamer namer,
            List<JudgeEnablement> enabledWith) {
        this(judgeName, currentVersion, sameVersions, approved, approvedRuntimes, Set.of(), namer, enabledWith);
    }

    /**
     * {@code judgeApprovedRuntimes} are the runtimes some version of the judge approved with --
     * today's, the one that gave a verdict or any other --, which decide whether a verdict of an
     * earlier version is from before by its version or by its runtime (F-ALDIA-R003 inv. 7).
     */
    public JudgeFreshnessReference(String judgeName, String currentVersion, Set<String> sameVersions,
            boolean approved, Set<EvaluationRuntimeIdentity> approvedRuntimes,
            Set<EvaluationRuntimeIdentity> judgeApprovedRuntimes, JudgeVersionNamer namer,
            List<JudgeEnablement> enabledWith) {
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
        Set<EvaluationRuntimeIdentity> everApproved = new LinkedHashSet<>(this.approvedRuntimes);
        if (judgeApprovedRuntimes != null) {
            everApproved.addAll(judgeApprovedRuntimes);
        }
        this.judgeApprovedRuntimes = Set.copyOf(everApproved);
        this.namer = namer;
        this.enabledWith = enabledWith != null ? List.copyOf(enabledWith) : List.of();
    }

    /**
     * Today's judge as its standing says it, with the runtimes its version approved with: for each
     * runtime, the last exam of today's version (or one that is the same) with it is APPROVED.
     */
    public static JudgeFreshnessReference of(JudgeExamStanding standing, List<JudgeExamRecord> history,
            ZoneId zone, Clock clock) {
        return of(standing, history, List.of(), zone, clock);
    }

    /**
     * F-ALDIA-R003: today's judge is approved when its version is enabled with some motor -- it
     * approved its exam with it, or José promoted it with it --, whichever motor the course runs with
     * by default; its verdicts are up to date with each of those motors. {@code promotions} name a
     * promoted version by its promotion when it came before its first approval (F-ALDIA-R004 inv. 7).
     */
    public static JudgeFreshnessReference of(JudgeExamStanding standing, List<JudgeExamRecord> history,
            List<JudgePromotionRecord> promotions, ZoneId zone, Clock clock) {
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
        Map<String, Instant> promotedAt = new LinkedHashMap<>();
        for (JudgeEnablement enablement : standing.getEnabledWith()) {
            if (enablement.runtimeIdentity() != null) {
                runtimes.add(enablement.runtimeIdentity());
            }
            if (enablement.status() == JudgeExamStatus.PROMOTED && enablement.since() != null
                    && standing.getJudgeVersion() != null) {
                promotedAt.merge(standing.getJudgeVersion(), enablement.since(), (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        for (JudgePromotionRecord promotion : promotions != null ? promotions : List.<JudgePromotionRecord>of()) {
            if (promotion != null && promotion.judgeVersion() != null && promotion.promotedAt() != null) {
                promotedAt.merge(promotion.judgeVersion(), promotion.promotedAt(), (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        boolean consults = standing.getStatus() == JudgeExamStatus.APPROVED
                || standing.getStatus() == JudgeExamStatus.PROMOTED;
        if (consults && standing.getRuntimeIdentity() != null) {
            runtimes.add(standing.getRuntimeIdentity());
        }
        boolean approved = consults || runtimes.stream().anyMatch(EvaluationRuntimeIdentity::isExplicit);
        return new JudgeFreshnessReference(standing.getJudgeName(), standing.getJudgeVersion(), same, approved,
                runtimes, approvedByAnyVersion(history, promotions),
                new JudgeVersionNamer(history, List.of(same), promotedAt, zone, clock), standing.getEnabledWith());
    }

    /**
     * Inv. 7: the runtimes some version of the judge approved with -- the last exam of that version
     * with that runtime approved, or José promoted it with it --, whichever version it was.
     */
    private static Set<EvaluationRuntimeIdentity> approvedByAnyVersion(List<JudgeExamRecord> history,
            List<JudgePromotionRecord> promotions) {
        Map<String, Map<EvaluationRuntimeIdentity, JudgeExamStatus>> lastByVersion = new LinkedHashMap<>();
        for (JudgeExamRecord record : history != null ? history : List.<JudgeExamRecord>of()) {
            if (record != null && record.getJudgeVersion() != null && record.getRuntimeIdentity().isExplicit()) {
                lastByVersion.computeIfAbsent(record.getJudgeVersion(), version -> new LinkedHashMap<>())
                        .put(record.getRuntimeIdentity(), record.getStatus());
            }
        }
        Set<EvaluationRuntimeIdentity> runtimes = new LinkedHashSet<>();
        lastByVersion.values().forEach(byRuntime -> byRuntime.forEach((runtime, status) -> {
            if (status == JudgeExamStatus.APPROVED) {
                runtimes.add(runtime);
            }
        }));
        for (JudgePromotionRecord promotion : promotions != null ? promotions : List.<JudgePromotionRecord>of()) {
            if (promotion != null && promotion.runtimeIdentity() != null) {
                runtimes.add(promotion.runtimeIdentity());
            }
        }
        return runtimes;
    }

    /**
     * The mark of a verdict given by {@code version} with {@code runtime}. Against today's version,
     * never against the other end of a comparison (inv. 1). A verdict of today's version is from
     * before only by its runtime. One of an earlier version is from before by its version when some
     * version of the judge approved with its runtime, and by its runtime only when none did (inv. 7).
     */
    public JudgeVerdictMark mark(String version, EvaluationRuntimeIdentity runtime) {
        if (!approved) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.JUDGE_NOT_APPROVED);
        }
        if (version == null || runtime == null || !runtime.isExplicit()) {
            return JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.UNKNOWN_PROVENANCE);
        }
        if (sameVersions.contains(version)) {
            return approvedRuntimes.contains(runtime)
                    ? JudgeVerdictMark.UP_TO_DATE
                    : JudgeVerdictMark.fromBefore(JudgeVerdictStaleCause.OTHER_RUNTIME);
        }
        return JudgeVerdictMark.fromBefore(judgeApprovedRuntimes.contains(runtime)
                ? JudgeVerdictStaleCause.EARLIER_VERSION
                : JudgeVerdictStaleCause.OTHER_RUNTIME);
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

    /** The runtimes some version of the judge approved with, today's included (inv. 7). */
    public Set<EvaluationRuntimeIdentity> getJudgeApprovedRuntimes() {
        return judgeApprovedRuntimes;
    }

    public JudgeVersionNamer getNamer() {
        return namer;
    }

    /** The motors with which today's version is enabled, as its standing says them. */
    public List<JudgeEnablement> getEnabledWith() {
        return enabledWith;
    }

    /** The name of today's version: «la del 13/10, 22:12», or «sin aprobar (…)». */
    public String currentVersionName() {
        return approved ? namer.name(currentVersion) : "sin aprobar (" + JudgeVersionNamer.shortCode(currentVersion) + ")";
    }

    public Optional<Instant> currentApprovedAt() {
        return approved ? namer.firstApproval(currentVersion) : Optional.empty();
    }
}
