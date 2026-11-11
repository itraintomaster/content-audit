package com.learney.contentaudit.revisiondomain.batch;

import java.util.ArrayList;
import java.util.List;

/**
 * F-BANDEJA-R003 and R011: the changes José keeps as his exception -- the best candidate of an exercise without a
 * winner that he approved, or the fix he wrote as his exception. They gain what they gain: the closing does not return
 * them and the verdict of F does not count them as returning; both list them apart.
 */
public final class JosesExceptions {

    private JosesExceptions() {
    }

    /** Whether an adjustment keeps its exercise as José's exception. */
    public static boolean is(BatchAdjustmentKind kind) {
        return kind == BatchAdjustmentKind.ACCEPT_AS_EXCEPTION || kind == BatchAdjustmentKind.JOSES_EXCEPTION;
    }

    /**
     * The proposals of the batch José keeps as his exception, once each, in the order he decided them. The proposal of
     * an exercise is the batch and the exercise, as the change ledger names it.
     */
    public static List<String> proposalIdsOf(CorrectionBatch batch) {
        List<String> ids = new ArrayList<>();
        if (batch == null || batch.getAdjustments() == null) {
            return ids;
        }
        for (BatchAdjustment adjustment : batch.getAdjustments()) {
            if (adjustment != null && is(adjustment.getKind()) && adjustment.getQuizId() != null) {
                String id = batch.getId() + "-" + adjustment.getQuizId();
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
