package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Instant;

/** Acceptance for use, never a fabricated exam result or accuracy measurement. */
public record JudgePromotionRecord(String judgeName, String judgeVersion,
        EvaluationRuntimeIdentity runtimeIdentity, String reviewer, String reason,
        String evidenceFile, String evidenceSha256, Instant promotedAt, JudgeExamException examException) {
    public JudgePromotionRecord(String judgeName, String judgeVersion,
            EvaluationRuntimeIdentity runtimeIdentity, String reviewer, String reason,
            String evidenceFile, String evidenceSha256, Instant promotedAt) {
        this(judgeName, judgeVersion, runtimeIdentity, reviewer, reason,
                evidenceFile, evidenceSha256, promotedAt, null);
    }

    public JudgePromotionRecord {
        for (String value : new String[]{judgeName,judgeVersion,reviewer,reason,evidenceFile,evidenceSha256})
            if (value==null || value.isBlank()) throw new IllegalArgumentException("Promoción incompleta");
        if (!judgeName.matches("[A-Za-z0-9._-]+") || !evidenceSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Identidad o evidencia de promoción inválida");
        if (runtimeIdentity==null || !runtimeIdentity.isExplicit() || promotedAt==null)
            throw new IllegalArgumentException("La promoción requiere proveedor, modelo, esfuerzo y fecha explícitos");
    }

    /** An exception is not transferable to another version, runtime, exam or measurement. */
    public boolean coversExamException(JudgeExamRecord measured, JudgeRegressionSuite suite) {
        return examException != null && examException.matches(measured, suite)
                && judgeName.equals(measured.getJudgeName())
                && judgeVersion.equals(measured.getJudgeVersion())
                && runtimeIdentity.equals(measured.getRuntimeIdentity())
                && promotedAt.isAfter(examException.examRecordedAt());
    }
}
