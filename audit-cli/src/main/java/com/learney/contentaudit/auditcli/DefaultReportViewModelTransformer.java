package com.learney.contentaudit.auditcli;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.KnowledgeNode;
import com.learney.contentaudit.auditdomain.MilestoneNode;
import com.learney.contentaudit.auditdomain.NodeScores;
import com.learney.contentaudit.auditdomain.QuizNode;
import com.learney.contentaudit.auditdomain.TopicNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultReportViewModelTransformer implements ReportViewModelTransformer {

    @Override
    public ReportViewModel transform(AuditReport report) {
        List<MilestoneNode> milestones = report.getMilestones() != null
                ? report.getMilestones() : List.of();

        Set<String> analyzerNameSet = new LinkedHashSet<>();
        for (MilestoneNode milestone : milestones) {
            collectAnalyzerNames(milestone, analyzerNameSet);
        }
        List<String> analyzerNames = Collections.unmodifiableList(new ArrayList<>(analyzerNameSet));

        List<MilestoneScoreRow> milestoneScoreRows = new ArrayList<>();
        for (MilestoneNode milestone : milestones) {
            if (isSyntheticMilestone(milestone.getMilestoneId())) continue;
            milestoneScoreRows.add(toMilestoneRow(milestone));
        }

        Map<String, Double> analyzerScores = extractScores(report.getScores());

        return new ReportViewModel(
                report.getOverallScore(),
                analyzerNames,
                analyzerScores,
                Collections.unmodifiableList(milestoneScoreRows)
        );
    }

    private MilestoneScoreRow toMilestoneRow(MilestoneNode milestone) {
        Map<String, Double> scores = extractScores(milestone.getScores());
        List<TopicScoreRow> topicRows = new ArrayList<>();
        if (milestone.getTopics() != null) {
            for (TopicNode topic : milestone.getTopics()) {
                topicRows.add(toTopicRow(topic));
            }
        }
        return new MilestoneScoreRow(milestone.getMilestoneId(), scores, avg(scores), topicRows,
                milestone.getEntity());
    }

    private TopicScoreRow toTopicRow(TopicNode topic) {
        Map<String, Double> scores = extractScores(topic.getScores());
        List<KnowledgeScoreRow> knowledgeRows = new ArrayList<>();
        if (topic.getKnowledges() != null) {
            for (KnowledgeNode knowledge : topic.getKnowledges()) {
                knowledgeRows.add(toKnowledgeRow(knowledge));
            }
        }
        return new TopicScoreRow(topic.getTopicId(), avg(scores), scores, knowledgeRows,
                topic.getEntity());
    }

    private KnowledgeScoreRow toKnowledgeRow(KnowledgeNode knowledge) {
        Map<String, Double> scores = extractScores(knowledge.getScores());
        List<QuizScoreRow> quizRows = new ArrayList<>();
        if (knowledge.getQuizzes() != null) {
            for (QuizNode quiz : knowledge.getQuizzes()) {
                quizRows.add(toQuizRow(quiz));
            }
        }
        return new KnowledgeScoreRow(knowledge.getKnowledgeId(), avg(scores), scores, quizRows,
                knowledge.getEntity());
    }

    private QuizScoreRow toQuizRow(QuizNode quiz) {
        Map<String, Double> scores = extractScores(quiz.getScores());
        return new QuizScoreRow(quiz.getQuizId(), avg(scores), scores, quiz.getEntity());
    }

    private Map<String, Double> extractScores(NodeScores nodeScores) {
        if (nodeScores == null || nodeScores.getScores() == null) return Map.of();
        var filtered = new java.util.LinkedHashMap<String, Double>();
        for (var entry : nodeScores.getScores().entrySet()) {
            if (!entry.getKey().contains("/")) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private double avg(Map<String, Double> scores) {
        return scores.values().stream().mapToDouble(d -> d).average().orElse(0);
    }

    private boolean isSyntheticMilestone(String milestoneId) {
        if (milestoneId == null) return true;
        return "0".equals(milestoneId) || "unknown".equals(milestoneId);
    }

    private void collectAnalyzerNames(MilestoneNode milestone, Set<String> names) {
        if (milestone.getScores() != null && milestone.getScores().getScores() != null) {
            for (String key : milestone.getScores().getScores().keySet()) {
                if (!key.contains("/")) names.add(key);
            }
        }
        if (milestone.getTopics() != null) {
            for (TopicNode topic : milestone.getTopics()) {
                if (topic.getScores() != null && topic.getScores().getScores() != null) {
                    for (String key : topic.getScores().getScores().keySet()) {
                        if (!key.contains("/")) names.add(key);
                    }
                }
            }
        }
    }
}
