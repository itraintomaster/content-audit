package com.learney.contentaudit.revisiondomain.publication;

import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import java.util.List;

/**
 * Un ejercicio o tema que la tanda cambió: cómo estaba en el curso del que partió ({@code before}), cómo queda en el
 * curso que se publica ({@code after}), su propuesta, si todos sus pasos están aprobados y los cambios que lo tocan.
 */
public record PublishedElement(CourseElementSnapshot before, CourseElementSnapshot after, String proposalId,
        DocumentReviewStatus reviewStatus, List<PublishedStep> steps) {
    public PublishedElement {
        steps = List.copyOf(steps);
    }
}
