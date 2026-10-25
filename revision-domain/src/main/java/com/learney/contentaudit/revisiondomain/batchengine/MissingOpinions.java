package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * F-OPINA-R001: what is missing, counted once. A pair is an exercise and a judge that applies to it; it is missing when
 * the report keeps no opinion of that judge on that exercise. A saved abstention is an opinion, and so is a verdict of
 * an earlier version of the judge: neither is missing. The analysis read and «Completar opiniones» use this one list.
 */
final class MissingOpinions {
    private MissingOpinions() {}

    /** Per judge of {@code judges}, the sorted ids of the exercises it applies to and has no opinion on; no empty entries. */
    static SortedMap<String, List<String>> of(AuditReport report, Collection<String> judges) {
        var result = new TreeMap<String, List<String>>();
        if (report == null || report.getRoot() == null) return result;
        var nodes = DefaultAnalysisComposer.index(report.getRoot()).values();
        for (String judge : judges) {
            var ids = nodes.stream().filter(n -> n.getTarget() == AuditTarget.QUIZ
                    && DefaultAnalysisComposer.eligible(n, judge) && !DefaultAnalysisComposer.hasStoredOpinion(n, judge))
                    .map(n -> n.getEntity().getId()).distinct().sorted().toList();
            if (!ids.isEmpty()) result.put(judge, ids);
        }
        return result;
    }
}
