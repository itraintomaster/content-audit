package com.learney.contentaudit.auditdomain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IScoreAggregator implements ScoreAggregator {
    @Override
    public AuditReport aggregate(List<ScoredItem> scores, Map<String, AuditableEntity> entityMap) {
        if (scores == null || scores.isEmpty()) {
            return new AuditReport(0.0, new NodeScores(Map.of()), List.of());
        }

        Map<String, List<ScoredItem>> byMilestone = new LinkedHashMap<>();
        for (ScoredItem item : scores) {
            String mid = item.getMilestoneId() != null ? item.getMilestoneId() : "unknown";
            byMilestone.computeIfAbsent(mid, k -> new ArrayList<>()).add(item);
        }

        double totalScore = 0.0;
        int totalCount = 0;
        List<MilestoneNode> milestones = new ArrayList<>();

        for (Map.Entry<String, List<ScoredItem>> entry : byMilestone.entrySet()) {
            List<ScoredItem> milestoneItems = entry.getValue();
            List<TopicNode> topics = buildTopicNodes(milestoneItems, entityMap);
            Map<String, Double> analyzerScores = averageByAnalyzer(milestoneItems);

            for (ScoredItem item : milestoneItems) {
                totalScore += item.getScore();
                totalCount++;
            }

            milestones.add(new MilestoneNode(entry.getKey(), new NodeScores(analyzerScores), topics,
                    entityMap.get(entry.getKey())));
        }

        double overallScore = totalCount > 0 ? totalScore / totalCount : 0.0;
        Map<String, Double> courseScores = averageByAnalyzer(scores);
        return new AuditReport(overallScore, new NodeScores(courseScores), milestones);
    }

    private List<TopicNode> buildTopicNodes(List<ScoredItem> milestoneItems, Map<String, AuditableEntity> entityMap) {
        Map<String, List<ScoredItem>> byTopic = new LinkedHashMap<>();
        for (ScoredItem item : milestoneItems) {
            String tid = item.getTopicId();
            if (tid != null) {
                byTopic.computeIfAbsent(tid, k -> new ArrayList<>()).add(item);
            }
        }

        List<TopicNode> topics = new ArrayList<>();
        for (Map.Entry<String, List<ScoredItem>> entry : byTopic.entrySet()) {
            List<ScoredItem> topicItems = entry.getValue();
            List<KnowledgeNode> knowledges = buildKnowledgeNodes(topicItems, entityMap);
            topics.add(new TopicNode(entry.getKey(), new NodeScores(averageByAnalyzer(topicItems)), knowledges,
                    entityMap.get(entry.getKey())));
        }
        return topics;
    }

    private List<KnowledgeNode> buildKnowledgeNodes(List<ScoredItem> topicItems, Map<String, AuditableEntity> entityMap) {
        Map<String, List<ScoredItem>> byKnowledge = new LinkedHashMap<>();
        for (ScoredItem item : topicItems) {
            String kid = item.getKnowledgeId();
            if (kid != null) {
                byKnowledge.computeIfAbsent(kid, k -> new ArrayList<>()).add(item);
            }
        }

        List<KnowledgeNode> knowledges = new ArrayList<>();
        for (Map.Entry<String, List<ScoredItem>> entry : byKnowledge.entrySet()) {
            List<ScoredItem> knowledgeItems = entry.getValue();
            List<QuizNode> quizzes = buildQuizNodes(knowledgeItems, entityMap);
            knowledges.add(new KnowledgeNode(entry.getKey(), new NodeScores(averageByAnalyzer(knowledgeItems)), quizzes,
                    entityMap.get(entry.getKey())));
        }
        return knowledges;
    }

    private List<QuizNode> buildQuizNodes(List<ScoredItem> knowledgeItems, Map<String, AuditableEntity> entityMap) {
        Map<String, List<ScoredItem>> byQuiz = new LinkedHashMap<>();
        for (ScoredItem item : knowledgeItems) {
            String qid = item.getQuizId();
            if (qid != null) {
                byQuiz.computeIfAbsent(qid, k -> new ArrayList<>()).add(item);
            }
        }

        List<QuizNode> quizzes = new ArrayList<>();
        for (Map.Entry<String, List<ScoredItem>> entry : byQuiz.entrySet()) {
            quizzes.add(new QuizNode(entry.getKey(), new NodeScores(averageByAnalyzer(entry.getValue())),
                    entityMap.get(entry.getKey())));
        }
        return quizzes;
    }

    private Map<String, Double> averageByAnalyzer(List<ScoredItem> items) {
        Map<String, List<Double>> byAnalyzer = new LinkedHashMap<>();
        for (ScoredItem item : items) {
            byAnalyzer.computeIfAbsent(item.getAnalyzerName(), k -> new ArrayList<>())
                    .add(item.getScore());
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> e : byAnalyzer.entrySet()) {
            result.put(e.getKey(), e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        }
        return result;
    }
}
