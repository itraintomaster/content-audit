package com.learney.contentaudit.refinerdomain;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link RefinerEngine}.
 *
 * Walks the AuditNode tree and emits a {@link RefinementTask} for each
 * leaf-level analyzer score that is below 1.0:
 * <ul>
 *   <li>QUIZ nodes  — sentence-length (and any other quiz-level analyzers)</li>
 *   <li>KNOWLEDGE nodes — knowledge-title-length, knowledge-instructions-length</li>
 *   <li>COURSE nodes — coca-buckets-distribution, lemma-recurrence (course-level)</li>
 *   <li>MILESTONE nodes — coca-buckets-distribution, lemma-recurrence (level-level)</li>
 *   <li>TOPIC nodes — skipped (only aggregated scores)</li>
 * </ul>
 *
 * Tasks are sorted by score ascending (worst first) and assigned priorities
 * 1, 2, 3 … accordingly.
 */
public class DefaultRefinerEngine implements RefinerEngine {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

    // Analyzer names that are meaningful at the KNOWLEDGE node level
    private static final List<String> KNOWLEDGE_ANALYZERS = List.of(
            "knowledge-title-length",
            "knowledge-instructions-length"
    );

    // Analyzer names that are meaningful at COURSE / MILESTONE level
    private static final List<String> COURSE_LEVEL_ANALYZERS = List.of(
            "coca-buckets-distribution",
            "lemma-recurrence"
    );

    // -------------------------------------------------------------------------
    // RefinerEngine
    // -------------------------------------------------------------------------

    @Override
    public RefinementPlan plan(AuditReport report, String auditId) {
        List<ScoredTask> rawTasks = new ArrayList<>();

        if (report.getRoot() != null) {
            walkNode(report.getRoot(), rawTasks);
        }

        // Sort by score ascending (worst first), then assign sequential priorities/IDs
        rawTasks.sort(Comparator.comparingDouble(t -> t.score));

        List<RefinementTask> tasks = new ArrayList<>();
        for (int i = 0; i < rawTasks.size(); i++) {
            ScoredTask st = rawTasks.get(i);
            String taskId = String.format("task-%03d", i + 1);
            tasks.add(new RefinementTask(
                    taskId,
                    st.nodeTarget,
                    st.nodeId,
                    st.nodeLabel,
                    st.diagnosisKind,
                    i + 1,
                    RefinementTaskStatus.PENDING
            ));
        }

        String planId = TIMESTAMP_FORMATTER.format(Instant.now());

        return new RefinementPlan(planId, auditId != null ? auditId : "", Instant.now(), tasks);
    }

    @Override
    public Optional<RefinementTask> nextTask(RefinementPlan plan) {
        if (plan.getTasks() == null) {
            return Optional.empty();
        }
        return plan.getTasks().stream()
                .filter(t -> t.getStatus() == RefinementTaskStatus.PENDING)
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // Tree traversal
    // -------------------------------------------------------------------------

    private void walkNode(AuditNode node, List<ScoredTask> accumulator) {
        AuditTarget target = node.getTarget();

        if (target != AuditTarget.TOPIC) {
            collectTasksForNode(node, accumulator);
        }

        List<AuditNode> children = node.getChildren();
        if (children != null) {
            for (AuditNode child : children) {
                walkNode(child, accumulator);
            }
        }
    }

    private void collectTasksForNode(AuditNode node, List<ScoredTask> accumulator) {
        Map<String, Double> scores = node.getScores();
        if (scores == null || scores.isEmpty()) {
            return;
        }

        AuditTarget target = node.getTarget();
        String nodeId = resolveId(node);
        String nodeLabel = resolveLabel(node);

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String analyzerName = entry.getKey();
            double score = entry.getValue();

            if (score >= 1.0) {
                continue;
            }

            if (!isRelevantForTarget(analyzerName, target)) {
                continue;
            }

            DiagnosisKind kind = mapToDiagnosisKind(analyzerName);
            if (kind == null) {
                continue;
            }

            accumulator.add(new ScoredTask(score, target, nodeId, nodeLabel, kind));
        }
    }

    /**
     * Returns true when the given analyzer score is a leaf-level (non-aggregated)
     * score for the specified target type.
     */
    private boolean isRelevantForTarget(String analyzerName, AuditTarget target) {
        switch (target) {
            case QUIZ:
                // All quiz-level analyzer scores are primary here
                return !KNOWLEDGE_ANALYZERS.contains(analyzerName)
                        && !COURSE_LEVEL_ANALYZERS.contains(analyzerName);
            case KNOWLEDGE:
                return KNOWLEDGE_ANALYZERS.contains(analyzerName);
            case MILESTONE:
            case COURSE:
                return COURSE_LEVEL_ANALYZERS.contains(analyzerName);
            default:
                // TOPIC — already excluded before this method is called
                return false;
        }
    }

    private DiagnosisKind mapToDiagnosisKind(String analyzerName) {
        switch (analyzerName) {
            case "sentence-length":              return DiagnosisKind.SENTENCE_LENGTH;
            case "lemma-absence":                return DiagnosisKind.LEMMA_ABSENCE;
            case "coca-buckets-distribution":    return DiagnosisKind.COCA_BUCKETS;
            case "lemma-recurrence":             return DiagnosisKind.LEMMA_RECURRENCE;
            case "knowledge-title-length":       return DiagnosisKind.KNOWLEDGE_TITLE_LENGTH;
            case "knowledge-instructions-length":return DiagnosisKind.KNOWLEDGE_INSTRUCTIONS_LENGTH;
            default:                             return null;
        }
    }

    private String resolveId(AuditNode node) {
        if (node.getEntity() == null) {
            return "root";
        }
        String id = node.getEntity().getId();
        return id != null ? id : "root";
    }

    private String resolveLabel(AuditNode node) {
        if (node.getEntity() == null) {
            return "Course";
        }
        String label = node.getEntity().getLabel();
        return label != null ? label : "Course";
    }

    // -------------------------------------------------------------------------
    // Internal value type for pre-sort accumulation
    // -------------------------------------------------------------------------

    private static final class ScoredTask {
        final double score;
        final AuditTarget nodeTarget;
        final String nodeId;
        final String nodeLabel;
        final DiagnosisKind diagnosisKind;

        ScoredTask(double score, AuditTarget nodeTarget, String nodeId,
                   String nodeLabel, DiagnosisKind diagnosisKind) {
            this.score = score;
            this.nodeTarget = nodeTarget;
            this.nodeId = nodeId;
            this.nodeLabel = nodeLabel;
            this.diagnosisKind = diagnosisKind;
        }
    }
}
