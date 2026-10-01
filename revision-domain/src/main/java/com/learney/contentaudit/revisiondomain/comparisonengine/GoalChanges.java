package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.revisiondomain.comparison.BlindSampleGoal;
import com.learney.contentaudit.revisiondomain.comparison.EffectiveGoals;
import com.learney.contentaudit.revisiondomain.comparison.Goal;
import com.learney.contentaudit.revisiondomain.comparison.GoalChange;
import com.learney.contentaudit.revisiondomain.comparison.GoalKind;
import com.learney.contentaudit.revisiondomain.comparison.GoalOrigin;
import com.learney.contentaudit.revisiondomain.comparison.GoalReference;
import com.learney.contentaudit.revisiondomain.comparison.GoalUnit;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What changed between the goals José sealed and the ones a comparison evaluates with (F-METAS-R006), as he reads
 * it: the floor of a family in one line -- «piso de vocabulario 0,5 → 1,0» --, every named goal by its id, and the
 * goal of the blind sample. A value that only changed its number shows the numbers; anything else, the whole goal.
 */
final class GoalChanges {
    static final String NONE = "—";

    static final String BLIND_SAMPLE = "muestra ciega";

    private GoalChanges() {
    }

    static List<GoalChange> between(EffectiveGoals sealed, EffectiveGoals current) {
        List<GoalChange> changes = new ArrayList<>();
        if (sealed == null || current == null) {
            return changes;
        }
        if (sealed.getFingerprint() != null && sealed.getFingerprint().equals(current.getFingerprint())) {
            return changes;
        }
        for (String prefix : List.of(DefaultGoalBook.VOCABULARY_FLOOR, DefaultGoalBook.ERRORS_FLOOR)) {
            familyChanges(prefix, familyGoals(sealed, prefix), familyGoals(current, prefix), changes);
        }
        Map<String, Goal> before = declared(sealed);
        Map<String, Goal> after = declared(current);
        Set<String> ids = new LinkedHashSet<>(before.keySet());
        ids.addAll(after.keySet());
        for (String id : ids) {
            Goal was = before.get(id);
            Goal is = after.get(id);
            if (was != null && is != null) {
                if (!sameGoal(was, is)) {
                    boolean onlyNumber = sameButThreshold(was, is);
                    changes.add(new GoalChange(id, onlyNumber ? number(was.getThreshold()) : describe(was),
                            onlyNumber ? number(is.getThreshold()) : describe(is)));
                }
            } else if (was != null) {
                changes.add(new GoalChange(id, describe(was), NONE));
            } else {
                changes.add(new GoalChange(id, NONE, describe(is)));
            }
        }
        if (!Objects.equals(sealed.getBlindSample(), current.getBlindSample())) {
            changes.add(new GoalChange(BLIND_SAMPLE, describe(sealed.getBlindSample()),
                    describe(current.getBlindSample())));
        }
        return changes;
    }

    /** The floor of a family, read from its family defaults: one value, and the subjects it covers. */
    private record FamilyShape(double threshold, GoalUnit unit, GoalReference reference, Set<AuditTarget> targets,
            Set<String> subjects) {
        boolean sameFloor(FamilyShape other) {
            return Double.compare(threshold, other.threshold) == 0 && unit == other.unit
                    && reference == other.reference && targets.equals(other.targets);
        }

        boolean sameButThreshold(FamilyShape other) {
            return unit == other.unit && reference == other.reference && targets.equals(other.targets);
        }

        String text() {
            return number(threshold) + " " + unitWord(unit) + " " + referenceWords(reference) + " en "
                    + String.join(", ", targets.stream().map(Enum::name).toList());
        }
    }

    private static FamilyShape familyGoals(EffectiveGoals goals, String prefix) {
        Double threshold = null;
        GoalUnit unit = null;
        GoalReference reference = null;
        Set<AuditTarget> targets = new LinkedHashSet<>();
        Set<String> subjects = new LinkedHashSet<>();
        for (Goal goal : goals.getGoals() != null ? goals.getGoals() : List.<Goal>of()) {
            if (goal.getOrigin() != GoalOrigin.FAMILY_DEFAULT || goal.getId() == null
                    || !goal.getId().startsWith(prefix + " · ")) {
                continue;
            }
            if (threshold == null) {
                threshold = goal.getThreshold();
                unit = goal.getUnit();
                reference = goal.getReference();
            }
            targets.add(goal.getNodeTarget());
            subjects.add(goal.getSubject());
        }
        return threshold == null ? null : new FamilyShape(threshold, unit, reference, targets, subjects);
    }

    private static void familyChanges(String prefix, FamilyShape before, FamilyShape after, List<GoalChange> changes) {
        if (before == null && after == null) {
            return;
        }
        if (before == null || after == null) {
            changes.add(new GoalChange(prefix, before != null ? before.text() : NONE,
                    after != null ? after.text() : NONE));
            return;
        }
        if (!before.sameFloor(after)) {
            boolean onlyNumber = before.sameButThreshold(after);
            changes.add(new GoalChange(prefix, onlyNumber ? number(before.threshold()) : before.text(),
                    onlyNumber ? number(after.threshold()) : after.text()));
        }
        for (String subject : after.subjects()) {
            if (!before.subjects().contains(subject)) {
                changes.add(new GoalChange(prefix + " · " + subject, NONE, after.text()));
            }
        }
        for (String subject : before.subjects()) {
            if (!after.subjects().contains(subject)) {
                changes.add(new GoalChange(prefix + " · " + subject, before.text(), NONE));
            }
        }
    }

    private static Map<String, Goal> declared(EffectiveGoals goals) {
        Map<String, Goal> declared = new LinkedHashMap<>();
        for (Goal goal : goals.getGoals() != null ? goals.getGoals() : List.<Goal>of()) {
            if (goal.getOrigin() != GoalOrigin.FAMILY_DEFAULT && goal.getId() != null) {
                declared.putIfAbsent(goal.getId(), goal);
            }
        }
        return declared;
    }

    private static boolean sameGoal(Goal a, Goal b) {
        return sameButThreshold(a, b) && Double.compare(a.getThreshold(), b.getThreshold()) == 0;
    }

    private static boolean sameButThreshold(Goal a, Goal b) {
        return a.getKind() == b.getKind() && Objects.equals(a.getSubject(), b.getSubject())
                && a.getNodeTarget() == b.getNodeTarget() && Objects.equals(a.getNodeId(), b.getNodeId())
                && a.getUnit() == b.getUnit() && a.getReference() == b.getReference();
    }

    static String describe(Goal goal) {
        StringBuilder text = new StringBuilder();
        text.append(goal.getKind() == GoalKind.FLOOR ? "piso" : "avance");
        text.append(' ').append(goal.getSubject()).append(" en ").append(goal.getNodeTarget());
        if (goal.getNodeId() != null) {
            text.append(' ').append(goal.getNodeId());
        }
        text.append(": ").append(number(goal.getThreshold())).append(' ').append(unitWord(goal.getUnit()))
                .append(' ').append(referenceWords(goal.getReference()));
        return text.toString();
    }

    static String describe(BlindSampleGoal goal) {
        if (goal == null) {
            return NONE;
        }
        return goal.getSize() + " pares, al menos " + goal.getMinBetterOrEqual() + " mejor o igual y a lo sumo "
                + goal.getMaxWorse() + " peor, " + (goal.getCriterion() != null ? goal.getCriterion().name() : "—")
                + ", responde " + goal.getResponsible();
    }

    /** A number with a decimal comma and at least one decimal: 0,5; 1,0; 0,25. */
    static String number(double value) {
        DecimalFormat format = new DecimalFormat("0.0##", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-AR")));
        return format.format(value);
    }

    static String unitWord(GoalUnit unit) {
        if (unit == null) {
            return "";
        }
        return switch (unit) {
            case POINTS -> "puntos";
            case EXERCISES -> "ejercicios";
            case PERCENT -> "%";
        };
    }

    static String referenceWords(GoalReference reference) {
        if (reference == null) {
            return "";
        }
        return switch (reference) {
            case BASELINE -> "contra la línea base";
            case BATCH_START -> "contra el inicio de la tanda";
            case ABSOLUTE -> "como valor";
        };
    }
}
