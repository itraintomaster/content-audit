package com.learney.contentaudit.revisiondomain.problems;

import java.util.List;
import java.util.Optional;

/** The adapter must check the revision and commit atomically across processes. -1 creates a new record. */
public interface ProblemStore {
    /** Coordinate a multi-step annotation with other writers to this course. */
    default <T> T exclusive(java.util.function.Supplier<T> operation) { return operation.get(); }
    Optional<ContentProblem> load(String id);
    List<ContentProblem> list();
    void save(ContentProblem problem, long expectedRevision);
}
