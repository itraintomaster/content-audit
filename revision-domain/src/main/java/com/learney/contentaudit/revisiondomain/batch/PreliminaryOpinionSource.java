package com.learney.contentaudit.revisiondomain.batch;

import java.util.List;

/** Reads and verifies original EXAM records and their measured snapshots without consulting a model.
 * The adapter preserves payload bytes, runtime, evaluator/version, namespace and original record hash.
 * Reading a source must never copy its records into the course evaluation namespace.
 */
public interface PreliminaryOpinionSource {
    List<ContextOpinion.PreliminaryOpinion> load(String sourceRef);
}
