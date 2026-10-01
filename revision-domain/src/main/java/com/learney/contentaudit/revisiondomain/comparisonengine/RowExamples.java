package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.finding.Finding;
import com.learney.contentaudit.auditdomain.finding.FindingResolution;
import com.learney.contentaudit.auditdomain.studentview.QuizStudentView;
import com.learney.contentaudit.auditdomain.studentview.StudentOptionView;
import com.learney.contentaudit.revisiondomain.comparison.RowExample;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * F-FILAS-R005: the example of every row, a real finding of the analysis chosen always the same way. One pass over
 * each photo keeps, per node and per row, the best candidate at or below the node: first a finding the batch fixes --
 * at the batch start, gone from the projection, and with its analyzer evaluating that exercise in the projection, or
 * else it vanished for lack of a budget, not because of the batch --, then one of the projection; among them the most
 * concrete, an exercise before a knowledge, a topic, a level or the course, and on the same level the lowest id, then
 * the rule and what it marks. A row of the errors family only takes what counts as an error, never what only ranks;
 * «algún error» takes an error of any analyzer and «vocabulario» a finding of any vocabulary analyzer, and their line
 * names the analyzer.
 */
final class RowExamples {
    /** What the line of a finding the batch fixes says. */
    static final String FIXED = "la tanda lo arregla";

    private static final Comparator<Candidate> ORDER = Comparator
            .comparing((Candidate candidate) -> !candidate.fixed())
            .thenComparingInt(Candidate::depth)
            .thenComparing(Candidate::nodeId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(candidate -> candidate.finding().getRule(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(candidate -> DefaultBatchComparator.identity(candidate.finding()));

    private final Map<String, AnalyzerFamily> families;

    /** By the key of a node, the best candidate of each row at or below it. */
    private final Map<String, Map<String, Candidate>> best = new HashMap<>();

    /** A finding, the node of the photo it sits on, and whether the batch fixes it. */
    private record Candidate(Finding finding, AuditNode node, boolean fixed) {
        int depth() {
            AuditTarget target = finding.getNode() != null && finding.getNode().getTarget() != null
                    ? finding.getNode().getTarget() : node.getTarget();
            if (target == null) {
                return Integer.MAX_VALUE;
            }
            return switch (target) {
                case QUIZ -> 0;
                case KNOWLEDGE -> 1;
                case TOPIC -> 2;
                case MILESTONE -> 3;
                case COURSE -> 4;
            };
        }

        String nodeId() {
            return finding.getNode() != null && finding.getNode().getNodeId() != null
                    ? finding.getNode().getNodeId() : ReportTree.idOf(node);
        }
    }

    private RowExamples(Map<String, AnalyzerFamily> families) {
        this.families = families;
    }

    /**
     * The candidates of both photos: the findings of the projection, and those of the batch start that the batch
     * fixes. {@code projectedIndex} is the index of the projection, with its parents set.
     */
    static RowExamples index(AuditReport start, AuditReport projected, Map<String, AuditNode> projectedIndex,
            Map<String, AnalyzerFamily> families) {
        RowExamples examples = new RowExamples(families);
        Set<String> projectedFindings = new HashSet<>();
        if (projected != null && projected.getRoot() != null) {
            examples.visit(projected.getRoot(), new ArrayList<>(), false, projectedFindings, projectedIndex);
        }
        if (start != null && start.getRoot() != null && start != projected) {
            examples.visit(start.getRoot(), new ArrayList<>(), true, projectedFindings, projectedIndex);
        }
        return examples;
    }

    /**
     * The example of a row at a node, or null when there is none: the exercise as the student sees it when the
     * finding is of an exercise, and its line. {@code displayName} names an analyzer in the line of a general row.
     */
    RowExample exampleFor(String subject, String nodeKey, QuizStudentViewRenderer renderer,
            Function<String, String> displayName) {
        Candidate candidate = best.getOrDefault(nodeKey, Map.of()).get(subject);
        if (candidate == null) {
            return null;
        }
        Finding finding = candidate.finding();
        boolean ofExercise = finding.getNode() != null ? finding.getNode().getTarget() == AuditTarget.QUIZ
                : candidate.node().getTarget() == AuditTarget.QUIZ;
        String quizId = ofExercise ? candidate.nodeId() : null;
        String exercise = ofExercise ? exercise(candidate, renderer) : null;
        boolean general = DefaultGoalBook.VOCABULARY.equals(subject) || DefaultGoalBook.ANY_ERROR.equals(subject);
        return new RowExample(quizId, exercise,
                detail(finding, candidate.fixed(), general ? displayName.apply(finding.getAnalyzer()) : null));
    }

    // -------------------------------------------------------------------------
    // The pass over a photo
    // -------------------------------------------------------------------------

    private void visit(AuditNode node, List<String> keys, boolean start, Set<String> projectedFindings,
            Map<String, AuditNode> projectedIndex) {
        keys.add(ReportTree.key(node.getTarget(), ReportTree.idOf(node)));
        if (node.getFindings() != null) {
            for (Finding finding : node.getFindings()) {
                if (finding == null) {
                    continue;
                }
                String identity = DefaultBatchComparator.identity(finding);
                if (!start) {
                    projectedFindings.add(identity);
                } else if (projectedFindings.contains(identity) || !evaluatedIn(projectedIndex, finding)) {
                    continue;
                }
                Candidate candidate = new Candidate(finding, node, start);
                for (String subject : subjectsOf(finding)) {
                    for (String key : keys) {
                        best.computeIfAbsent(key, k -> new HashMap<>()).merge(subject, candidate,
                                (kept, offered) -> ORDER.compare(kept, offered) <= 0 ? kept : offered);
                    }
                }
            }
        }
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                if (child != null) {
                    visit(child, keys, start, projectedFindings, projectedIndex);
                }
            }
        }
        keys.remove(keys.size() - 1);
    }

    /** The rows a finding can be the example of: its analyzer's and, if it counts there, the general one. */
    private List<String> subjectsOf(Finding finding) {
        String analyzer = finding.getAnalyzer();
        if (analyzer == null) {
            return List.of();
        }
        AnalyzerFamily family = families.get(analyzer);
        if (family == AnalyzerFamily.ERRORS) {
            return finding.getResolution() == FindingResolution.RANK_ONLY ? List.of()
                    : List.of(analyzer, DefaultGoalBook.ANY_ERROR);
        }
        if (family == AnalyzerFamily.VOCABULARY) {
            return List.of(analyzer, DefaultGoalBook.VOCABULARY);
        }
        return List.of(analyzer);
    }

    /** Whether the analyzer of a finding evaluated its node in the projection: neither it nor a parent left it open. */
    private static boolean evaluatedIn(Map<String, AuditNode> projectedIndex, Finding finding) {
        if (finding.getNode() == null || projectedIndex == null) {
            return false;
        }
        AuditNode node = projectedIndex.get(ReportTree.key(finding.getNode().getTarget(),
                finding.getNode().getNodeId()));
        if (node == null) {
            return false;
        }
        for (AuditNode up = node; up != null; up = up.getParent()) {
            if (up.getUnevaluatedBy() != null && up.getUnevaluatedBy().contains(finding.getAnalyzer())) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // What the example says
    // -------------------------------------------------------------------------

    /**
     * The exercise as the student sees it: the statement with its gaps and hints and, in a multiple choice, its
     * options in the student's order. Without a renderer or an exercise to render, the label the finding gives it.
     */
    private static String exercise(Candidate candidate, QuizStudentViewRenderer renderer) {
        if (renderer != null && candidate.node().getEntity() instanceof AuditableQuiz quiz) {
            QuizStudentView view = renderer.render(quiz);
            if (view != null && view.getStatement() != null && !view.getStatement().isBlank()) {
                StringBuilder text = new StringBuilder(view.getStatement().strip());
                List<String> options = new ArrayList<>();
                for (StudentOptionView option : view.getOptions() != null ? view.getOptions()
                        : List.<StudentOptionView>of()) {
                    if (option != null && option.getLabel() != null && !option.getLabel().isBlank()) {
                        options.add(option.getLabel().strip());
                    }
                }
                if (!options.isEmpty()) {
                    text.append(" — opciones: ").append(String.join(" / ", options));
                }
                return text.toString();
            }
        }
        String label = candidate.finding().getNode() != null ? candidate.finding().getNode().getLabel() : null;
        return label != null && !label.isBlank() ? oneLine(label) : null;
    }

    /** One line: the observation of the finding, or its rule; the analyzer first in a general row; and if fixed. */
    private static String detail(Finding finding, boolean fixed, String analyzer) {
        String observation = finding.getEvidence() != null ? finding.getEvidence().getObservation() : null;
        String line = observation != null && !observation.isBlank() ? oneLine(observation)
                : finding.getRule() != null ? finding.getRule() : String.valueOf(finding.getAnalyzer());
        if (analyzer != null) {
            line = analyzer + ": " + line;
        }
        if (fixed) {
            line = (line.endsWith(".") ? line.substring(0, line.length() - 1) : line) + " — " + FIXED;
        }
        return line;
    }

    private static String oneLine(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }
}
