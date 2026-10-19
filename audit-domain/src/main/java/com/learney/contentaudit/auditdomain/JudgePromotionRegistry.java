package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.util.Optional;

/** Explicit operator decisions, separate from fixed-exam measurements. */
public interface JudgePromotionRegistry {
    void record(JudgePromotionRecord promotion);
    Optional<JudgePromotionRecord> latest(String judgeName, String judgeVersion,
            EvaluationRuntimeIdentity runtimeIdentity);
}
