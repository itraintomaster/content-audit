package com.learney.contentaudit.revisiondomain.problems;

import java.time.Instant;
import java.util.*;

/** A course-owned problem. Cases are frozen JSON snapshots, independent of the source campaign. */
public record ContentProblem(String id, String title, String criterion, List<String> failures,
        List<String> parts, Status status, long revision, List<Case> cases,
        List<Activity> activity, Map<String, Object> delivery, Instant createdAt, Instant updatedAt) {
    public enum Status { COLLECTING, READY, WORKING, FOR_TESTING, RESOLVED }
    public enum Use { EXAMPLE, EXAM, GOOD }
    public enum Actor { OPERATOR, AGENT }

    public ContentProblem {
        Objects.requireNonNull(id); Objects.requireNonNull(status);
        title = required(title, "El problema necesita un nombre.");
        criterion = criterion == null ? "" : criterion;
        failures = failures == null ? List.of() : List.copyOf(failures);
        parts = parts == null ? List.of() : List.copyOf(parts);
        cases = List.copyOf(cases); activity = List.copyOf(activity);
        delivery = freeze(delivery == null ? Map.of() : delivery);
    }

    public record Case(String id, String quizId, String batchId, String workdir, int contentVersion,
            int correctionRevision, Use use, String comment, String expected,
            Map<String, Object> snapshot, Instant createdAt) {
        public Case {
            id = required(id, "Falta el id del caso.");
            quizId = required(quizId, "Falta el quiz del caso.");
            batchId = required(batchId, "Falta la tanda del caso.");
            Objects.requireNonNull(use); Objects.requireNonNull(createdAt);
            snapshot = freeze(Objects.requireNonNull(snapshot));
            comment = comment == null ? "" : comment;
            expected = expected == null ? "" : expected;
        }
        public Case withUse(Use value) {
            return new Case(id, quizId, batchId, workdir, contentVersion, correctionRevision,
                    value, comment, expected, snapshot, createdAt);
        }
    }

    public record Activity(Instant at, Actor actor, String step, String note, Status status) {}

    static String required(String text, String message) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException(message);
        return text.trim();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> freeze(Map<String, ?> values) {
        return (Map<String, Object>) freezeValue(values);
    }
    private static Object freezeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put((String) key, freezeValue(item)));
            return Collections.unmodifiableMap(copy); // Preserve explicit nulls in course snapshots.
        }
        if (value instanceof List<?> list) {
            return Collections.unmodifiableList(list.stream().map(ContentProblem::freezeValue).toList());
        }
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        throw new IllegalArgumentException("El caso debe contener datos JSON, no " + value.getClass().getName());
    }
}
