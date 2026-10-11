package com.learney.contentaudit.revisiondomain.publication;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import java.util.Set;

/** Un cambio que queda en la tanda y las partes que declara sobre un ejercicio o tema. */
public record PublishedStep(String changeId, Set<ExercisePart> parts) {
    public PublishedStep {
        parts = Set.copyOf(parts);
    }
}
