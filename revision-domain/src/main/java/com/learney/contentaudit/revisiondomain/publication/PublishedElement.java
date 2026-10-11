package com.learney.contentaudit.revisiondomain.publication;

import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Un ejercicio o tema que la tanda cambió: cómo estaba en el curso del que partió ({@code before}), cómo queda en el
 * curso que se publica ({@code after}), su propuesta, si todos sus pasos están aprobados y, por cada cambio que queda en
 * la tanda y lo toca, en el orden de la tanda, las partes que declara sobre él.
 */
public record PublishedElement(CourseElementSnapshot before, CourseElementSnapshot after, String proposalId,
        DocumentReviewStatus reviewStatus, Map<String, Set<ExercisePart>> partsByChange) {
    public PublishedElement {
        Map<String, Set<ExercisePart>> copy = new LinkedHashMap<>();
        partsByChange.forEach((changeId, parts) -> copy.put(changeId, Set.copyOf(parts)));
        partsByChange = Collections.unmodifiableMap(copy);
    }
}
