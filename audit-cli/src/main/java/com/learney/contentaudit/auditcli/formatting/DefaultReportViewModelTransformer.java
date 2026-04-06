package com.learney.contentaudit.auditcli.formatting;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultReportViewModelTransformer implements ReportViewModelTransformer {

    @Override
    public ReportViewModel transform(AuditReport report) {
        AuditNode root = report.getRoot();
        List<AuditNode> milestoneNodes = (root != null && root.getChildren() != null)
                ? root.getChildren() : List.of();

        Set<String> analyzerNameSet = new LinkedHashSet<>();
        for (AuditNode milestone : milestoneNodes) {
            collectAnalyzerNames(milestone, analyzerNameSet);
        }
        List<String> analyzerNames = Collections.unmodifiableList(new ArrayList<>(analyzerNameSet));

        List<MilestoneScoreRow> milestoneScoreRows = new ArrayList<>();
        for (AuditNode milestone : milestoneNodes) {
            String milestoneId = idOf(milestone);
            if (isSyntheticMilestone(milestoneId)) continue;
            milestoneScoreRows.add(toMilestoneRow(milestone));
        }

        Map<String, Double> analyzerScores = extractScores(root != null ? root.getScores() : null);

        double overallScore = avg(analyzerScores);

        return new ReportViewModel(
                overallScore,
                analyzerNames,
                analyzerScores,
                Collections.unmodifiableList(milestoneScoreRows)
        );
    }

    private MilestoneScoreRow toMilestoneRow(AuditNode milestone) {
        Map<String, Double> scores = extractScores(milestone.getScores());
        List<TopicScoreRow> topicRows = new ArrayList<>();
        if (milestone.getChildren() != null) {
            for (AuditNode topic : milestone.getChildren()) {
                topicRows.add(toTopicRow(topic));
            }
        }
        return new MilestoneScoreRow(idOf(milestone), scores, avg(scores), topicRows,
                milestone.getEntity());
    }

    private TopicScoreRow toTopicRow(AuditNode topic) {
        Map<String, Double> scores = extractScores(topic.getScores());
        List<KnowledgeScoreRow> knowledgeRows = new ArrayList<>();
        if (topic.getChildren() != null) {
            for (AuditNode knowledge : topic.getChildren()) {
                knowledgeRows.add(toKnowledgeRow(knowledge));
            }
        }
        return new TopicScoreRow(idOf(topic), avg(scores), scores, knowledgeRows,
                topic.getEntity());
    }

    private KnowledgeScoreRow toKnowledgeRow(AuditNode knowledge) {
        Map<String, Double> scores = extractScores(knowledge.getScores());
        List<QuizScoreRow> quizRows = new ArrayList<>();
        if (knowledge.getChildren() != null) {
            for (AuditNode quiz : knowledge.getChildren()) {
                quizRows.add(toQuizRow(quiz));
            }
        }
        return new KnowledgeScoreRow(idOf(knowledge), avg(scores), scores, quizRows,
                knowledge.getEntity());
    }

    private QuizScoreRow toQuizRow(AuditNode quiz) {
        Map<String, Double> scores = extractScores(quiz.getScores());
        return new QuizScoreRow(idOf(quiz), avg(scores), scores, quiz.getEntity());
    }

    private Map<String, Double> extractScores(Map<String, Double> rawScores) {
        if (rawScores == null) return Map.of();
        var filtered = new LinkedHashMap<String, Double>();
        for (var entry : rawScores.entrySet()) {
            if (!entry.getKey().contains("/")) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private double avg(Map<String, Double> scores) {
        return scores.values().stream().mapToDouble(d -> d).average().orElse(0);
    }

    private String idOf(AuditNode node) {
        if (node.getEntity() != null && node.getEntity().getId() != null) {
            return node.getEntity().getId();
        }
        return null;
    }

    private boolean isSyntheticMilestone(String milestoneId) {
        if (milestoneId == null) return true;
        return "0".equals(milestoneId) || "unknown".equals(milestoneId);
    }

    private void collectAnalyzerNames(AuditNode milestone, Set<String> names) {
        if (milestone.getScores() != null) {
            for (String key : milestone.getScores().keySet()) {
                if (!key.contains("/")) names.add(key);
            }
        }
        if (milestone.getChildren() != null) {
            for (AuditNode topic : milestone.getChildren()) {
                if (topic.getScores() != null) {
                    for (String key : topic.getScores().keySet()) {
                        if (!key.contains("/")) names.add(key);
                    }
                }
            }
        }
    }
}
