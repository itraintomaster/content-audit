package com.learney.contentaudit.auditdomain.judging;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** A human accepts exactly these discrepancies in one completed, measured exam. */
public record JudgeExamException(String examFingerprint, String regressionSuiteFingerprint,
        Instant examRecordedAt, List<String> acceptedRegressionCaseIds,
        List<String> acceptedMismatchedCaseIds) {
    public JudgeExamException {
        if (examFingerprint == null || examFingerprint.isBlank()
                || examRecordedAt == null) {
            throw new IllegalArgumentException("La excepción requiere examen y fecha de medición explícitos");
        }
        acceptedRegressionCaseIds = normalizedIds(acceptedRegressionCaseIds, true);
        acceptedMismatchedCaseIds = normalizedIds(acceptedMismatchedCaseIds, false);
        // A null fingerprint represents the explicitly empty permanent suite.
        // matches() still binds that absence to both the measured and current suite.
        if (regressionSuiteFingerprint == null ? !acceptedRegressionCaseIds.isEmpty()
                : regressionSuiteFingerprint.isBlank()) {
            throw new IllegalArgumentException("La excepción requiere la huella de su suite, o ausencia explícita sin regresiones");
        }
    }

    public boolean matches(JudgeExamRecord measured, JudgeRegressionSuite suite) {
        return measured != null && suite != null
                && (measured.getStatus() == JudgeExamStatus.APPROVED
                        || measured.getStatus() == JudgeExamStatus.FAILED)
                && measured.getCasesNotJudged() == 0
                && measured.getCasesJudged() > 0
                && measured.getCasesJudged() >= suite.cases().size()
                && measured.getCasesJudged() >= measured.getRegressionCaseIds().size()
                && measured.getCasesJudged() >= acceptedMismatchedCaseIds.size()
                && examFingerprint.equals(measured.getExamFingerprint())
                && Objects.equals(regressionSuiteFingerprint, measured.getRegressionSuiteFingerprint())
                && Objects.equals(regressionSuiteFingerprint, suite.fingerprint())
                && examRecordedAt.equals(measured.getRecordedAt())
                && suite.coveredBy(measured)
                && (!acceptedRegressionCaseIds.isEmpty()
                        || (suite.cases().isEmpty() && measured.getRegressionCaseIds().isEmpty()))
                && suite.caseIds().containsAll(acceptedRegressionCaseIds)
                && sameIds(acceptedRegressionCaseIds, measured.getRegressionMismatchedCaseIds())
                && sameIds(acceptedMismatchedCaseIds, measured.getMismatchedCaseIds());
    }

    private static List<String> normalizedIds(List<String> ids, boolean allowEmpty) {
        if (ids == null || (!allowEmpty && ids.isEmpty())) {
            throw new IllegalArgumentException("La excepción debe identificar las discrepancias aceptadas");
        }
        var normalized = new TreeSet<String>();
        for (String id : ids) {
            if (id == null || id.isBlank() || !normalized.add(id.strip())) {
                throw new IllegalArgumentException("Los identificadores de la excepción deben ser únicos y no vacíos");
            }
        }
        return List.copyOf(normalized);
    }

    private static boolean sameIds(List<String> accepted, List<String> actual) {
        return actual != null && actual.stream().allMatch(Objects::nonNull)
                && actual.size() == accepted.size()
                && new TreeSet<>(actual).equals(new TreeSet<>(accepted));
    }
}
