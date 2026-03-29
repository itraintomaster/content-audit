package com.learney.contentaudit.auditcli;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DefaultDrillDownResolver implements DrillDownResolver {

    @Override
    public DrillDownView resolve(ReportViewModel viewModel, DrillDownScope scope) {
        if (scope.getLevel() == null || scope.getLevel().isEmpty()) {
            return courseView(viewModel);
        }

        String levelName = scope.getLevel().get();
        MilestoneScoreRow milestone = findMilestone(viewModel, levelName);
        if (milestone == null) {
            throw new IllegalArgumentException("Level '" + levelName
                    + "' not found. Available: " + milestoneIds(viewModel));
        }

        if (scope.getTopic() == null || scope.getTopic().isEmpty()) {
            return milestoneView(milestone);
        }

        String topicName = scope.getTopic().get();
        TopicScoreRow topic = findTopic(milestone, topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic '" + topicName
                    + "' not found in " + levelName + ". Available: " + topicIds(milestone));
        }

        if (scope.getKnowledge() == null || scope.getKnowledge().isEmpty()) {
            return topicView(topic);
        }

        String knowledgeName = scope.getKnowledge().get();
        KnowledgeScoreRow knowledge = findKnowledge(topic, knowledgeName);
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge '" + knowledgeName
                    + "' not found in " + topicName + ". Available: " + knowledgeIds(topic));
        }

        return knowledgeView(knowledge);
    }

    private DrillDownView courseView(ReportViewModel viewModel) {
        List<ChildScoreRow> children = new ArrayList<>();
        if (viewModel.getMilestoneScores() != null) {
            for (MilestoneScoreRow m : viewModel.getMilestoneScores()) {
                children.add(new ChildScoreRow(m.getMilestoneId(), m.getOverallScore(),
                        safe(m.getAnalyzerScores())));
            }
        }
        return new DrillDownView(DrillDownLevel.COURSE, "Course",
                viewModel.getOverallScore(), safe(viewModel.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, viewModel.getAnalyzerScores())),
                children);
    }

    private DrillDownView milestoneView(MilestoneScoreRow milestone) {
        List<ChildScoreRow> children = new ArrayList<>();
        if (milestone.getTopicScores() != null) {
            for (TopicScoreRow t : milestone.getTopicScores()) {
                children.add(new ChildScoreRow(t.getTopicId(), t.getOverallScore(),
                        safe(t.getAnalyzerScores())));
            }
        }
        return new DrillDownView(DrillDownLevel.MILESTONE, milestone.getMilestoneId(),
                milestone.getOverallScore(), safe(milestone.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, milestone.getAnalyzerScores())),
                children);
    }

    private DrillDownView topicView(TopicScoreRow topic) {
        List<ChildScoreRow> children = new ArrayList<>();
        if (topic.getKnowledgeScores() != null) {
            for (KnowledgeScoreRow k : topic.getKnowledgeScores()) {
                children.add(new ChildScoreRow(k.getKnowledgeId(), k.getOverallScore(),
                        safe(k.getAnalyzerScores())));
            }
        }
        return new DrillDownView(DrillDownLevel.TOPIC, topic.getTopicId(),
                topic.getOverallScore(), safe(topic.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, topic.getAnalyzerScores())),
                children);
    }

    private DrillDownView knowledgeView(KnowledgeScoreRow knowledge) {
        List<ChildScoreRow> children = new ArrayList<>();
        if (knowledge.getQuizScores() != null) {
            for (QuizScoreRow q : knowledge.getQuizScores()) {
                children.add(new ChildScoreRow(q.getQuizId(), q.getOverallScore(),
                        safe(q.getAnalyzerScores())));
            }
        }
        return new DrillDownView(DrillDownLevel.KNOWLEDGE, knowledge.getKnowledgeId(),
                knowledge.getOverallScore(), safe(knowledge.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, knowledge.getAnalyzerScores())),
                children);
    }

    private MilestoneScoreRow findMilestone(ReportViewModel vm, String name) {
        if (vm.getMilestoneScores() == null) return null;
        for (MilestoneScoreRow m : vm.getMilestoneScores()) {
            if (name.equalsIgnoreCase(m.getMilestoneId())) return m;
        }
        return null;
    }

    private TopicScoreRow findTopic(MilestoneScoreRow milestone, String name) {
        if (milestone.getTopicScores() == null) return null;
        for (TopicScoreRow t : milestone.getTopicScores()) {
            if (name.equalsIgnoreCase(t.getTopicId())) return t;
        }
        return null;
    }

    private KnowledgeScoreRow findKnowledge(TopicScoreRow topic, String name) {
        if (topic.getKnowledgeScores() == null) return null;
        for (KnowledgeScoreRow k : topic.getKnowledgeScores()) {
            if (name.equalsIgnoreCase(k.getKnowledgeId())) return k;
        }
        return null;
    }

    private List<String> milestoneIds(ReportViewModel vm) {
        List<String> ids = new ArrayList<>();
        if (vm.getMilestoneScores() != null) {
            for (MilestoneScoreRow m : vm.getMilestoneScores()) ids.add(m.getMilestoneId());
        }
        return ids;
    }

    private List<String> topicIds(MilestoneScoreRow m) {
        List<String> ids = new ArrayList<>();
        if (m.getTopicScores() != null) {
            for (TopicScoreRow t : m.getTopicScores()) ids.add(t.getTopicId());
        }
        return ids;
    }

    private List<String> knowledgeIds(TopicScoreRow t) {
        List<String> ids = new ArrayList<>();
        if (t.getKnowledgeScores() != null) {
            for (KnowledgeScoreRow k : t.getKnowledgeScores()) ids.add(k.getKnowledgeId());
        }
        return ids;
    }

    private Set<String> collectAnalyzerNames(List<ChildScoreRow> children, Map<String, Double> parentScores) {
        Set<String> names = new LinkedHashSet<>();
        if (parentScores != null) names.addAll(parentScores.keySet());
        for (ChildScoreRow child : children) {
            if (child.getAnalyzerScores() != null) names.addAll(child.getAnalyzerScores().keySet());
        }
        return names;
    }

    private Map<String, Double> safe(Map<String, Double> map) {
        return map != null ? map : Map.of();
    }
}
