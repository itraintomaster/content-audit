package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.contextnumbers.*;
import com.learney.contentaudit.auditdomain.finding.*;
import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.revisiondomain.batch.CurrentAnalysisService;
import java.util.*;

/** A comparison summary. Full evidence and run provenance remain available through the original analysis APIs. */
final class EvolutionProjection {
    private EvolutionProjection() {}
    private static final Set<String> MARKS = Set.of("judgeVersion", "runtime", "runtimeName", "abstention", "abstained",
            "evaluated", "status", "today", "fingerprint");
    static CurrentAnalysisService.View analysis(CurrentAnalysisService.View source, String state, CourseEntity course) {
        if (source == null) return null;
        Set<String> ids = ids(course);
        var states = source.states().stream().filter(s -> state.equals(s.id())).map(s -> new CurrentAnalysisService.State(
                s.id(),s.label(),s.fingerprint(),digest(s.digest(),ids),filter(s.studentViews(),ids),provenance(s.provenance(),ids),
                s.approvalWarnings(),s.comparableAnalyzers(),s.analysisCode(),s.inapplicableAnalyzers(),s.judges(),marks(s.verdictMarks(),ids))).toList();
        return new CurrentAnalysisService.View(source.batchId(),source.contentVersion(),source.revision(),states,
                List.of(),List.of(),source.supportedAnalyzers());
    }
    private static Set<String> ids(CourseEntity course) {
        var ids = new HashSet<String>();
        if (course == null) return ids;
        ids.add(course.getId());
        if (course.getRoot() == null) return ids;
        ids.add(course.getRoot().getId());
        for (var milestone : BatchBook.list(course.getRoot().getMilestones())) {
            ids.add(milestone.getId());
            for (var topic : BatchBook.list(milestone.getTopics())) {
                ids.add(topic.getId());
                for (var knowledge : BatchBook.list(topic.getKnowledges())) {
                    ids.add(knowledge.getId());
                    for (var quiz : BatchBook.list(knowledge.getQuizTemplates())) ids.add(quiz.getId());
                }
            }
        }
        return ids;
    }
    private static AuditDigest digest(AuditDigest digest, Set<String> ids) {
        return digest == null ? null : new AuditDigest(digest.getAuditId(),node(digest.getRoot(),ids),List.of());
    }
    private static DigestNode node(DigestNode node, Set<String> ids) {
        if (node == null || (node.getTarget() != AuditTarget.COURSE && !ids.contains(node.getNodeId()))) return null;
        return new DigestNode(node.getNodeId(),node.getTarget(),node.getLabel(),node.getNumbers(),
                BatchBook.list(node.getFindings()).stream().map(EvolutionProjection::finding).toList(),
                node.getUnevaluatedBy(),BatchBook.list(node.getChildren()).stream().map(n -> node(n,ids))
                        .filter(Objects::nonNull).toList(),node.getPartiallyEvaluatedBy());
    }
    private static Finding finding(Finding f) {
        var evidence = f.getEvidence(); String observation = evidence == null ? null : evidence.getObservation();
        if (observation != null && observation.length() > 600) observation = observation.substring(0,599) + "…";
        var summary = evidence == null ? null : new FindingEvidence(List.of(),observation,List.of());
        return new Finding(f.getAnalyzer(),f.getRule(),f.getNode(),f.getSeverity(),summary,f.getResolution(),null,f.getIdentity());
    }
    private static <T> Map<String,T> filter(Map<String,T> source, Set<String> ids) {
        var result = new LinkedHashMap<String,T>();
        if (source != null) source.forEach((id,value) -> { if (ids.contains(id)) result.put(id,value); });
        return result;
    }
    private static Map<String,Object> values(Map<?,?> source) {
        var result = new LinkedHashMap<String,Object>();
        source.forEach((key,value) -> { if (MARKS.contains(key.toString())) result.put(key.toString(),value); });
        return result;
    }
    private static Map<String,Map<String,Object>> provenance(Map<String,Map<String,Object>> source, Set<String> ids) {
        var result = new LinkedHashMap<String,Map<String,Object>>();
        filter(source,ids).forEach((id,analyzers) -> {
            var entries = new LinkedHashMap<String,Object>();
            analyzers.forEach((analyzer,origin) -> { if (origin instanceof Map<?,?> map) entries.put(analyzer,values(map)); });
            result.put(id,entries);
        });
        return result;
    }
    private static Map<String,Map<String,Map<String,Object>>> marks(Map<String,Map<String,Map<String,Object>>> source, Set<String> ids) {
        var result = new LinkedHashMap<String,Map<String,Map<String,Object>>>();
        filter(source,ids).forEach((id,analyzers) -> {
            var entries = new LinkedHashMap<String,Map<String,Object>>();
            analyzers.forEach((analyzer,origin) -> entries.put(analyzer,values(origin)));
            result.put(id,entries);
        });
        return result;
    }
}
