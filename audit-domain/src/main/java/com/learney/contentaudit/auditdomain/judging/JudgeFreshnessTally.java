package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * F-ALDIA-R005: adds up, for one judge in one state, the marks of its verdicts (F-ALDIA-R003) unit by
 * unit, and says it in a line: «miniteoría: reusa 82 temas de antes (77 de la del 5/10, 5 de la del
 * 13/10, 13:00)». It only counts what it is given. An abstention counts inside its mark, and the line says how many
 * there are and why: «nada de antes; se abstuvo en 88 (81 no aplica, 7 falta contexto)» (inv. 5).
 */
public final class JudgeFreshnessTally {
    private final JudgeFreshnessReference reference;

    private final String displayName;

    private final AuditTarget unit;

    private int upToDate;

    private int notMeasured;

    private final Map<List<Object>, Integer> stale = new LinkedHashMap<>();

    private final Map<JudgeAbstention, Integer> abstained = new EnumMap<>(JudgeAbstention.class);

    public JudgeFreshnessTally(JudgeFreshnessReference reference, String displayName, AuditTarget unit) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.displayName = displayName != null && !displayName.isBlank() ? displayName : reference.getJudgeName();
        this.unit = unit != null ? unit : AuditTarget.QUIZ;
    }

    /** {@code count} units judged by {@code version} with {@code runtime}; returns their mark. */
    public JudgeVerdictMark add(String version, EvaluationRuntimeIdentity runtime, int count) {
        JudgeVerdictMark mark = reference.mark(version, runtime);
        add(mark, version, runtime, count);
        return mark;
    }

    /** {@code count} units judged by {@code version} with {@code runtime}, abstaining for {@code abstention} when given. */
    public JudgeVerdictMark add(String version, EvaluationRuntimeIdentity runtime, int count, JudgeAbstention abstention) {
        JudgeVerdictMark mark = add(version, runtime, count);
        abstained(abstention, count);
        return mark;
    }

    /** {@code count} units with an already computed mark, abstaining for {@code abstention} when given. */
    public void add(JudgeVerdictMark mark, String version, EvaluationRuntimeIdentity runtime, int count,
            JudgeAbstention abstention) {
        add(mark, version, runtime, count);
        abstained(abstention, count);
    }

    /**
     * {@code count} units already added with their mark that are abstentions for {@code reason}: a verdict without a
     * score is never not measured (F-ALDIA-R003 inv. 6).
     */
    public void abstained(JudgeAbstention reason, int count) {
        if (reason != null && count > 0) {
            abstained.merge(reason, count, Integer::sum);
        }
    }

    /** {@code count} units with an already computed mark. */
    public void add(JudgeVerdictMark mark, String version, EvaluationRuntimeIdentity runtime, int count) {
        if (count <= 0) {
            return;
        }
        switch (mark.freshness()) {
            case UP_TO_DATE -> upToDate += count;
            case NOT_MEASURED -> notMeasured += count;
            case FROM_BEFORE -> stale.merge(key(mark.cause(), version, runtime), count, Integer::sum);
        }
    }

    public void notMeasured(int count) {
        notMeasured += Math.max(0, count);
    }

    public JudgeFreshnessCount count() {
        JudgeVersionNamer namer = reference.getNamer();
        List<JudgeStaleGroup> groups = new ArrayList<>();
        stale.forEach((key, count) -> {
            JudgeVerdictStaleCause cause = (JudgeVerdictStaleCause) key.get(0);
            String version = (String) key.get(1);
            EvaluationRuntimeIdentity runtime = (EvaluationRuntimeIdentity) key.get(2);
            groups.add(new JudgeStaleGroup(cause, version, version != null ? namer.name(version) : null,
                    version != null ? namer.firstApproval(version).orElse(null) : null,
                    runtime != null ? runtime.toString() : null, JudgeVersionNamer.runtimeName(runtime), count));
        });
        groups.sort(Comparator.comparingInt(JudgeStaleGroup::count).reversed());
        int fromBefore = groups.stream().mapToInt(JudgeStaleGroup::count).sum();
        List<JudgeAbstentionGroup> abstentions = new ArrayList<>();
        abstained.forEach((reason, count) -> abstentions.add(new JudgeAbstentionGroup(reason, reason.label(), count)));
        abstentions.sort(Comparator.comparingInt(JudgeAbstentionGroup::count).reversed());
        int abstainedUnits = abstentions.stream().mapToInt(JudgeAbstentionGroup::count).sum();
        String detail = detail(fromBefore, groups) + abstentionDetail(abstainedUnits, abstentions);
        return new JudgeFreshnessCount(reference.getJudgeName(), displayName, unit, upToDate + fromBefore + notMeasured,
                upToDate, fromBefore, notMeasured, reference.getCurrentVersion(), reference.currentVersionName(),
                reference.currentApprovedAt().orElse(null), reference.isApproved(), List.copyOf(groups),
                displayName + (reference.isApproved() ? "" : " (sin aprobar)") + ": " + detail, detail, abstainedUnits,
                List.copyOf(abstentions));
    }

    /** «; se abstuvo en 88 (81 no aplica, 7 falta contexto)», or nothing without abstentions. */
    private static String abstentionDetail(int abstainedUnits, List<JudgeAbstentionGroup> abstentions) {
        if (abstainedUnits == 0) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JudgeAbstentionGroup group : abstentions) {
            parts.add(number(group.count()) + " " + group.reasonName());
        }
        return "; se abstuvo en " + number(abstainedUnits) + " (" + String.join(", ", parts) + ")";
    }

    /** The key a verdict from before is grouped by: the version for a version, the runtime for a runtime. */
    private static List<Object> key(JudgeVerdictStaleCause cause, String version, EvaluationRuntimeIdentity runtime) {
        List<Object> key = new ArrayList<>();
        key.add(cause);
        key.add(cause == JudgeVerdictStaleCause.EARLIER_VERSION || cause == JudgeVerdictStaleCause.JUDGE_NOT_APPROVED
                ? version : null);
        key.add(cause == JudgeVerdictStaleCause.OTHER_RUNTIME ? runtime : null);
        return key;
    }

    private String detail(int fromBefore, List<JudgeStaleGroup> groups) {
        if (fromBefore == 0) {
            return "nada de antes";
        }
        List<String> parts = new ArrayList<>();
        for (JudgeStaleGroup group : groups) {
            parts.add(number(group.count()) + " de " + origin(group));
        }
        return "reusa " + number(fromBefore) + " " + unitWord(fromBefore) + " de antes (" + String.join(", ", parts) + ")";
    }

    private String origin(JudgeStaleGroup group) {
        return switch (group.cause()) {
            case OTHER_RUNTIME -> group.runtimeName();
            case UNKNOWN_PROVENANCE -> JudgeVersionNamer.UNKNOWN_PROVENANCE;
            case EARLIER_VERSION -> group.versionName();
            case JUDGE_NOT_APPROVED -> Objects.equals(group.judgeVersion(), reference.getCurrentVersion())
                    ? "la versión de hoy, sin aprobar" : group.versionName();
        };
    }

    private String unitWord(int count) {
        boolean topics = unit == AuditTarget.KNOWLEDGE;
        return count == 1 ? (topics ? "tema" : "ejercicio") : (topics ? "temas" : "ejercicios");
    }

    static String number(int value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(value);
    }
}
