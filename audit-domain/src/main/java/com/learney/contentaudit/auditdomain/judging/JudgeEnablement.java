package com.learney.contentaudit.auditdomain.judging;

import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.time.Instant;

/**
 * F-ALDIA-R003: a motor with which the version of today of a judge is enabled, and since when. Its
 * {@code status} says how: {@code APPROVED} when its last exam with that motor approved ({@code since}
 * is that exam), {@code PROMOTED} when José promoted it with that motor and no exam measured after with
 * that motor replaced the promotion ({@code since} is the promotion).
 */
public record JudgeEnablement(EvaluationRuntimeIdentity runtimeIdentity, JudgeExamStatus status, Instant since) {
}
