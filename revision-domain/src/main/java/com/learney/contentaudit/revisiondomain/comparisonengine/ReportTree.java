package com.learney.contentaudit.revisiondomain.comparisonengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerFamily;
import com.learney.contentaudit.auditdomain.catalog.AnalyzerMeasure;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerErrorCounts;
import com.learney.contentaudit.auditdomain.contextnumbers.AnalyzerScore;
import com.learney.contentaudit.auditdomain.contextnumbers.ContextNumbers;
import com.learney.contentaudit.auditdomain.contextnumbers.ErrorCounts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the comparison reads from a report, never recomputing it (F-METAS-R001): its nodes by target and id
 * ("root" for the course), the published number of each subject in a node, and the course the report measured,
 * rebuilt from its own entities -- the same exercises and the same tokens -- so it can be measured again.
 */
final class ReportTree {
    static final String COURSE_ID = "root";

    private ReportTree() {
    }

    /**
     * A published number of a node: the value, its share, how many exercises it left unevaluated and of how many,
     * and whether it evaluated any exercise at all -- a count of nothing evaluated is not a zero (F-FILAS-R003).
     */
    record Value(Double value, Double share, int notEvaluated, int total, boolean evaluated) {
    }

    static String key(AuditTarget target, String id) {
        return target + "|" + id;
    }

    static String idOf(AuditNode node) {
        if (node.getEntity() != null && node.getEntity().getId() != null) {
            return node.getEntity().getId();
        }
        return node.getTarget() == AuditTarget.COURSE ? COURSE_ID : null;
    }

    static String labelOf(AuditNode node) {
        if (node.getTarget() == AuditTarget.COURSE) {
            return "Curso";
        }
        return node.getEntity() != null && node.getEntity().getLabel() != null ? node.getEntity().getLabel() : idOf(node);
    }

    /** Every node of the report by target and id, in the order of the tree; parents are set where missing. */
    static Map<String, AuditNode> index(AuditReport report) {
        Map<String, AuditNode> index = new LinkedHashMap<>();
        if (report != null && report.getRoot() != null) {
            collect(report.getRoot(), null, index);
        }
        return index;
    }

    private static void collect(AuditNode node, AuditNode parent, Map<String, AuditNode> index) {
        if (node.getParent() == null && parent != null) {
            node.setParent(parent);
        }
        index.putIfAbsent(key(node.getTarget(), idOf(node)), node);
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                if (child != null) {
                    collect(child, node, index);
                }
            }
        }
    }

    /** The analyzers whose measures the report keeps at its root, in their order (the catalog's). */
    static List<String> measuredAnalyzers(AuditReport report) {
        List<String> names = new ArrayList<>();
        if (report != null && report.getRoot() != null && report.getRoot().getMeasures() != null) {
            for (AnalyzerMeasure measure : report.getRoot().getMeasures()) {
                if (measure != null && measure.getAnalyzer() != null && !names.contains(measure.getAnalyzer())) {
                    names.add(measure.getAnalyzer());
                }
            }
        }
        return names;
    }

    static Map<String, AnalyzerMeasure> measures(AuditReport report) {
        Map<String, AnalyzerMeasure> measures = new LinkedHashMap<>();
        if (report != null && report.getRoot() != null && report.getRoot().getMeasures() != null) {
            for (AnalyzerMeasure measure : report.getRoot().getMeasures()) {
                if (measure != null && measure.getAnalyzer() != null) {
                    measures.putIfAbsent(measure.getAnalyzer(), measure);
                }
            }
        }
        return measures;
    }

    /**
     * The published number of a subject in a node: the vocabulary score, «algún error», the score of a vocabulary
     * analyzer or the exercises with an error of an errors analyzer. Null when the report did not measure it --
     * an empty row, never a zero.
     */
    static Value value(AuditNode node, String subject, AnalyzerFamily family) {
        if (node == null) {
            return null;
        }
        ContextNumbers numbers = node.getNumbers();
        if (DefaultGoalBook.VOCABULARY.equals(subject)) {
            return numbers != null && numbers.getVocabularyScore() != null
                    ? new Value(numbers.getVocabularyScore(), null, 0, 0, true) : null;
        }
        if (DefaultGoalBook.ANY_ERROR.equals(subject)) {
            ErrorCounts errors = numbers != null ? numbers.getErrors() : null;
            if (errors == null || errors.getAnalyzers() == null || errors.getAnalyzers().isEmpty()) {
                return null;
            }
            return new Value((double) errors.getWithAnyError(), errors.getAnyErrorShare(),
                    errors.getNotFullyEvaluated(), errors.getQuizzes(), anyEvaluated(errors));
        }
        if (family == AnalyzerFamily.ERRORS) {
            ErrorCounts errors = numbers != null ? numbers.getErrors() : null;
            if (errors != null && errors.getAnalyzers() != null) {
                for (AnalyzerErrorCounts counts : errors.getAnalyzers()) {
                    if (subject.equals(counts.getAnalyzer())) {
                        return new Value((double) counts.getWithError(), counts.getErrorShare(), counts.getNotEvaluated(),
                                counts.getReached(), evaluated(counts));
                    }
                }
            }
            return null;
        }
        if (numbers != null && numbers.getAnalyzerScores() != null) {
            for (AnalyzerScore score : numbers.getAnalyzerScores()) {
                if (subject.equals(score.getAnalyzer())) {
                    return score.getScore() != null ? new Value(score.getScore(), null, 0, 0, true) : null;
                }
            }
            return null;
        }
        Double score = node.getScores() != null ? node.getScores().get(subject) : null;
        return score != null ? new Value(score, null, 0, 0, true) : null;
    }

    /** Whether an errors analyzer evaluated at least one exercise of the node, halfway included (F-RONDA1-R006). */
    static boolean evaluated(AnalyzerErrorCounts counts) {
        return counts.getEvaluated() > 0 || counts.getReached() - counts.getNotEvaluated() > 0
                || counts.getWithError() > 0;
    }

    /** «algún error»: whether any analyzer of the errors family evaluated an exercise of the node. */
    private static boolean anyEvaluated(ErrorCounts errors) {
        for (AnalyzerErrorCounts counts : errors.getAnalyzers()) {
            if (counts != null && evaluated(counts)) {
                return true;
            }
        }
        return false;
    }

    /** The family the report published for an analyzer, when the catalog does not know it. */
    static AnalyzerFamily publishedFamily(AuditReport report, String analyzer) {
        AuditNode root = report != null ? report.getRoot() : null;
        ContextNumbers numbers = root != null ? root.getNumbers() : null;
        if (numbers != null && numbers.getAnalyzerScores() != null) {
            for (AnalyzerScore score : numbers.getAnalyzerScores()) {
                if (analyzer.equals(score.getAnalyzer()) && score.getFamily() != null) {
                    return score.getFamily();
                }
            }
        }
        if (numbers != null && numbers.getErrors() != null && numbers.getErrors().getAnalyzers() != null) {
            for (AnalyzerErrorCounts counts : numbers.getErrors().getAnalyzers()) {
                if (analyzer.equals(counts.getAnalyzer())) {
                    return AnalyzerFamily.ERRORS;
                }
            }
        }
        return null;
    }

    /**
     * The course the report measured, rebuilt from the entities of its nodes, with the nodes of {@code replaced}
     * (by id) taken from another report instead: an exercise is its entity; a knowledge, its entity with the
     * exercises of its node. Null when a node has no entity to rebuild it from.
     */
    static AuditableCourse course(AuditReport report, Map<String, AuditNode> replaced) {
        if (report == null || report.getRoot() == null || report.getRoot().getChildren() == null) {
            return null;
        }
        List<AuditableMilestone> levels = new ArrayList<>();
        for (AuditNode level : report.getRoot().getChildren()) {
            if (!(level.getEntity() instanceof AuditableMilestone milestone)) {
                return null;
            }
            List<AuditableTopic> topics = new ArrayList<>();
            for (AuditNode topicNode : children(level)) {
                if (!(topicNode.getEntity() instanceof AuditableTopic topic)) {
                    return null;
                }
                List<AuditableKnowledge> knowledges = new ArrayList<>();
                for (AuditNode knowledgeNode : children(topicNode)) {
                    AuditNode source = replaced.getOrDefault(idOf(knowledgeNode), knowledgeNode);
                    AuditableKnowledge rebuilt = knowledge(source, replaced);
                    if (rebuilt == null) {
                        return null;
                    }
                    knowledges.add(rebuilt);
                }
                topics.add(new AuditableTopic(knowledges, topic.getId(), topic.getLabel(), topic.getCode()));
            }
            levels.add(new AuditableMilestone(topics, milestone.getId(), milestone.getLabel(), milestone.getCode()));
        }
        return new AuditableCourse(levels);
    }

    private static AuditableKnowledge knowledge(AuditNode node, Map<String, AuditNode> replaced) {
        if (!(node.getEntity() instanceof AuditableKnowledge knowledge)) {
            return null;
        }
        List<AuditableQuiz> quizzes = new ArrayList<>();
        for (AuditNode quizNode : children(node)) {
            AuditNode source = replaced.getOrDefault(idOf(quizNode), quizNode);
            if (!(source.getEntity() instanceof AuditableQuiz quiz)) {
                return null;
            }
            quizzes.add(quiz);
        }
        return new AuditableKnowledge(quizzes, knowledge.getTitle(), knowledge.getInstructions(),
                knowledge.isIsSentence(), knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                knowledge.getSentenceMode(), knowledge.getTopicName());
    }

    private static List<AuditNode> children(AuditNode node) {
        return node.getChildren() != null ? node.getChildren() : List.of();
    }
}
