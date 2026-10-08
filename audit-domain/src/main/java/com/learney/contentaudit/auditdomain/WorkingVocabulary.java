package com.learney.contentaudit.auditdomain;

import java.util.function.BiFunction;

/**
 * Run-local, provisional vocabulary, never a saved analysis or a publication verdict.
 * Updates and queries are serialized; model calls must happen outside a query. The callback
 * must return detached values, not retain or mutate the report/tree. Rebuild from the composed
 * course after cancellation or failure; no provisional state is authoritative.
 */
public interface WorkingVocabulary {
    void replaceKnowledge(AuditableKnowledge knowledge);

    <T> T query(BiFunction<AuditReport, Long, T> query);
}
