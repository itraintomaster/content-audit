package com.learney.contentaudit.revisiondomain.panel;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.refinerdomain.RefinementTask;
import com.learney.contentaudit.refinerdomain.errortask.ErrorTaskBrief;
import java.util.List;

/** An explicit preservation order; deliberately has no Finding, analyzer or invented judge verdict. */
public final class DidacticRepairBrief extends ErrorTaskBrief {
    private final DidacticReference reference;
    public DidacticRepairBrief(RefinementTask task, DidacticReference reference) {
        super(task, null, List.of(), scope(reference), true, null, List.of());
        this.reference = reference;
    }
    private static List<ExercisePart> scope(DidacticReference reference) {
        var scope = new java.util.ArrayList<>(List.of(ExercisePart.STATEMENT, ExercisePart.ANSWERS,
                ExercisePart.MINI_THEORY, ExercisePart.STORED_SENTENCE));
        if (reference != null && reference.operatorInstruction() != null) scope.add(ExercisePart.INSTRUCTIONS);
        return List.copyOf(scope);
    }
    public DidacticReference getReference() { return reference; }
    @Override public boolean equals(Object other) {
        return super.equals(other) && other instanceof DidacticRepairBrief that && java.util.Objects.equals(reference, that.reference);
    }
    @Override public int hashCode() { return java.util.Objects.hash(super.hashCode(), reference); }
}
