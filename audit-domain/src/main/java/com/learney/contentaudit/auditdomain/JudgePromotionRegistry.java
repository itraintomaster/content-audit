package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.util.List;
import java.util.Optional;

/** Explicit operator decisions, separate from fixed-exam measurements. */
public interface JudgePromotionRegistry {
    void record(JudgePromotionRecord promotion);
    Optional<JudgePromotionRecord> latest(String judgeName, String judgeVersion,
            EvaluationRuntimeIdentity runtimeIdentity);

    /**
     * F-ALDIA-R003: every valid promotion of the judge, in the order they were recorded, for every
     * version and motor -- so the motors a version was promoted with can be asked one by one.
     */
    default List<JudgePromotionRecord> history(String judgeName) {
        return List.of();
    }
}
