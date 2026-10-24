package com.learney.contentaudit.auditdomain.judging;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The fingerprint identifies the complete permanent reference, independently of judge versions. */
public record JudgeRegressionSuite(String fingerprint, List<JudgeRegressionCase> cases) {
    public JudgeRegressionSuite { cases = List.copyOf(cases); }
    public static JudgeRegressionSuite empty() { return new JudgeRegressionSuite(null, List.of()); }
    public Set<String> caseIds() { return cases.stream().map(JudgeRegressionCase::id).collect(Collectors.toUnmodifiableSet()); }
    public boolean coveredBy(JudgeExamRecord record) {
        return cases.isEmpty() || record != null && java.util.Objects.equals(fingerprint, record.getRegressionSuiteFingerprint())
                && record.getRegressionCaseIds().containsAll(caseIds());
    }
}
