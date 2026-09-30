package com.learney.contentaudit.refinerdomain;
import com.learney.contentaudit.auditdomain.AnalyzerCatalog;
import com.learney.contentaudit.auditdomain.AnalyzerDescriptor;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerPlanBinding;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link RefinerEngine}.
 *
 * Walks the AuditNode tree and emits a {@link RefinementTask} for each analyzer score below
 * 1.0 (F-RCLA-R001) on a level where that analyzer's plan binding makes tasks. The bindings
 * come from the catalog, one per provider, so the plan is not edited to add an analyzer: it
 * only needs the {@link DiagnosisKind} constant. Today they reproduce the old lists exactly
 * (F-HALL-R013): sentence-length, lemma-absence and quiz-instruction on the quiz,
 * knowledge-title-length and knowledge-instructions-length on the knowledge,
 * coca-buckets-distribution on the level and the course, lemma-recurrence on the course and
 * lemma-count nowhere.
 *
 * Tasks are sorted by score ascending (worst first) and assigned priorities
 * 1, 2, 3 … accordingly.
 */
public class DefaultRefinerEngine implements RefinerEngine {

    private final AnalyzerCatalog analyzerCatalog;

    public DefaultRefinerEngine(AnalyzerCatalog analyzerCatalog) {
        this.analyzerCatalog = analyzerCatalog;
    }

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC);

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

    /**
     * F-HALL-R004 inv. 3: in the errors family the plan never drops a score below 1 in
     * silence. Every node an errors analyzer evaluated below 1 that {@link #plan} does not turn
     * into a task is declared here, per analyzer: it has no task kind, its task kind is not a
     * {@link DiagnosisKind} the plan knows, or its binding makes no tasks on that level. The
     * vocabulary family keeps today's plan and is not declared (F-HALL-R013).
     */
    @Override
    public List<UnconvertedScoreCount> unconvertedScores(AuditReport report) {
        List<UnconvertedScoreCount> result = new ArrayList<>();
        if (report == null || report.getRoot() == null) {
            return result;
        }
        for (AnalyzerDescriptor card : analyzerCatalog.list()) {
            if (card.getFamily() != AnalyzerFamily.ERRORS) {
                continue;
            }
            Optional<AnalyzerPlanBinding> binding = analyzerCatalog.planBinding(card.getName());
            Map<String, Integer> countsByReason = new LinkedHashMap<>();
            countUnconverted(report.getRoot(), card, binding, countsByReason);
            for (Map.Entry<String, Integer> entry : countsByReason.entrySet()) {
                result.add(new UnconvertedScoreCount(card.getName(), entry.getValue(), entry.getKey()));
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Tree traversal
    // -------------------------------------------------------------------------

    private void walkNode(AuditNode node, List<ScoredTask> accumulator) {
        collectTasksForNode(node, accumulator);

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
            Double score = entry.getValue();

            if (score == null || score >= 1.0) {
                continue;
            }

            DiagnosisKind kind = taskKindAt(analyzerName, target);
            if (kind == null) {
                continue;
            }

            accumulator.add(new ScoredTask(score, target, nodeId, nodeLabel, kind));
        }
    }

    /**
     * The task kind the analyzer's binding makes on this level, or null when it makes none
     * there: no binding (lemma-count, a sub-metric such as a COCA quarter, a name the catalog
     * does not have), a level outside its task targets, or a kind the plan does not know.
     */
    private DiagnosisKind taskKindAt(String analyzerName, AuditTarget target) {
        Optional<AnalyzerPlanBinding> binding = analyzerCatalog.planBinding(analyzerName);
        if (binding.isEmpty() || binding.get().getTaskTargets() == null
                || !binding.get().getTaskTargets().contains(target)) {
            return null;
        }
        return knownKind(binding.get().getTaskKind());
    }

    private void countUnconverted(AuditNode node, AnalyzerDescriptor card,
            Optional<AnalyzerPlanBinding> binding, Map<String, Integer> countsByReason) {
        AuditTarget target = node.getTarget();
        List<AuditTarget> evaluated = card.getEvaluatedTargets() != null ? card.getEvaluatedTargets() : List.of();
        Double score = node.getScores() != null ? node.getScores().get(card.getName()) : null;
        if (score != null && score < 1.0 && evaluated.contains(target)) {
            String reason = unconvertedReason(binding, target);
            if (reason != null) {
                countsByReason.merge(reason, 1, Integer::sum);
            }
        }
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                countUnconverted(child, card, binding, countsByReason);
            }
        }
    }

    private static String unconvertedReason(Optional<AnalyzerPlanBinding> binding, AuditTarget target) {
        if (binding.isEmpty()) {
            return "su ficha no declara un tipo de tarea";
        }
        String taskKind = binding.get().getTaskKind();
        if (knownKind(taskKind) == null) {
            return "el tipo de tarea " + taskKind + " no existe en el plan";
        }
        if (binding.get().getTaskTargets() == null || !binding.get().getTaskTargets().contains(target)) {
            return "su ficha no hace tareas en " + target;
        }
        return null;
    }

    private static DiagnosisKind knownKind(String taskKind) {
        if (taskKind == null) {
            return null;
        }
        for (DiagnosisKind kind : DiagnosisKind.values()) {
            if (kind.name().equals(taskKind)) {
                return kind;
            }
        }
        return null;
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
