package com.learney.contentaudit.auditdomain.judging;

import java.time.Instant;
import java.util.*;

/** A frozen operator reference, independently identified from the live exercise. */
public record JudgeRegressionCase(String id, String judgeName, String problemId, String problemTitle,
        String problemCaseId, String quizId, boolean expectedDetected, String expectedReason,
        String expectedSource, Map<String, Object> snapshot, Instant confirmedAt) {
    public JudgeRegressionCase {
        Objects.requireNonNull(id); Objects.requireNonNull(judgeName); Objects.requireNonNull(problemId);
        Objects.requireNonNull(problemCaseId); Objects.requireNonNull(quizId);
        snapshot = freeze(snapshot);
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> freeze(Map<String, Object> values) {
        return (Map<String, Object>) immutable(Objects.requireNonNull(values));
    }
    private static Object immutable(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), immutable(v)));
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> list) return Collections.unmodifiableList(list.stream().map(JudgeRegressionCase::immutable).toList());
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        throw new IllegalArgumentException("A regression snapshot must contain JSON values");
    }
}
