package com.learney.contentaudit.auditcli.formatting;

import com.learney.contentaudit.auditdomain.AuditableEntity;
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
                    + "' not found. Available: " + describeItems(viewModel.getMilestoneScores()));
        }

        if (scope.getTopic() == null || scope.getTopic().isEmpty()) {
            return milestoneView(milestone);
        }

        String topicName = scope.getTopic().get();
        TopicScoreRow topic = findByEntityOrId(milestone.getTopicScores(), topicName);
        if (topic == null) {
            throw new IllegalArgumentException("Topic '" + topicName
                    + "' not found in " + levelName + ". Available: " + describeItems(milestone.getTopicScores()));
        }

        if (scope.getKnowledge() == null || scope.getKnowledge().isEmpty()) {
            return topicView(topic);
        }

        String knowledgeName = scope.getKnowledge().get();
        KnowledgeScoreRow knowledge = findByEntityOrId(topic.getKnowledgeScores(), knowledgeName);
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge '" + knowledgeName
                    + "' not found in " + topicName + ". Available: " + describeItems(topic.getKnowledgeScores()));
        }

        return knowledgeView(knowledge);
    }

    private DrillDownView courseView(ReportViewModel viewModel) {
        List<ScoreRow> children = new ArrayList<>();
        if (viewModel.getMilestoneScores() != null) {
            children.addAll(viewModel.getMilestoneScores());
        }
        return new DrillDownView(DrillDownLevel.COURSE, "Course",
                viewModel.getOverallScore(), safe(viewModel.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, viewModel.getAnalyzerScores())),
                children);
    }

    private DrillDownView milestoneView(MilestoneScoreRow milestone) {
        List<ScoreRow> children = new ArrayList<>();
        if (milestone.getTopicScores() != null) {
            children.addAll(milestone.getTopicScores());
        }
        return new DrillDownView(DrillDownLevel.MILESTONE, label(milestone),
                milestone.getOverallScore(), safe(milestone.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, milestone.getAnalyzerScores())),
                children);
    }

    private DrillDownView topicView(TopicScoreRow topic) {
        List<ScoreRow> children = new ArrayList<>();
        if (topic.getKnowledgeScores() != null) {
            children.addAll(topic.getKnowledgeScores());
        }
        return new DrillDownView(DrillDownLevel.TOPIC, label(topic),
                topic.getOverallScore(), safe(topic.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, topic.getAnalyzerScores())),
                children);
    }

    private DrillDownView knowledgeView(KnowledgeScoreRow knowledge) {
        List<ScoreRow> children = new ArrayList<>();
        if (knowledge.getQuizScores() != null) {
            children.addAll(knowledge.getQuizScores());
        }
        return new DrillDownView(DrillDownLevel.KNOWLEDGE, label(knowledge),
                knowledge.getOverallScore(), safe(knowledge.getAnalyzerScores()),
                List.copyOf(collectAnalyzerNames(children, knowledge.getAnalyzerScores())),
                children);
    }

    private MilestoneScoreRow findMilestone(ReportViewModel vm, String name) {
        if (vm.getMilestoneScores() == null) return null;
        for (MilestoneScoreRow m : vm.getMilestoneScores()) {
            if (matches(m, name, m.getMilestoneId())) return m;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends ScoreRow> T findByEntityOrId(List<T> items, String name) {
        if (items == null) return null;
        for (T item : items) {
            String id = idOf(item);
            if (matches(item, name, id)) return item;
        }
        return null;
    }

    private boolean matches(ScoreRow row, String name, String positionalId) {
        if (name.equalsIgnoreCase(positionalId)) return true;
        AuditableEntity entity = row.getEntity();
        if (entity == null) return false;
        return name.equalsIgnoreCase(entity.getLabel())
                || name.equalsIgnoreCase(entity.getId())
                || name.equalsIgnoreCase(entity.getCode());
    }

    private String idOf(ScoreRow row) {
        if (row instanceof TopicScoreRow t) return t.getTopicId();
        if (row instanceof KnowledgeScoreRow k) return k.getKnowledgeId();
        if (row instanceof QuizScoreRow q) return q.getQuizId();
        if (row instanceof MilestoneScoreRow m) return m.getMilestoneId();
        return null;
    }

    private String label(ScoreRow row) {
        AuditableEntity entity = row.getEntity();
        if (entity != null && entity.getLabel() != null) return entity.getLabel();
        return idOf(row);
    }

    private List<String> describeItems(List<? extends ScoreRow> items) {
        if (items == null) return List.of();
        List<String> descs = new ArrayList<>();
        for (ScoreRow item : items) {
            String id = idOf(item);
            AuditableEntity entity = item.getEntity();
            if (entity != null && entity.getLabel() != null) {
                descs.add(id + " (" + entity.getLabel() + ")");
            } else {
                descs.add(id);
            }
        }
        return descs;
    }

    private Set<String> collectAnalyzerNames(List<ScoreRow> children, Map<String, Double> parentScores) {
        Set<String> names = new LinkedHashSet<>();
        if (parentScores != null) names.addAll(parentScores.keySet());
        for (ScoreRow child : children) {
            if (child.getAnalyzerScores() != null) names.addAll(child.getAnalyzerScores().keySet());
        }
        return names;
    }

    private Map<String, Double> safe(Map<String, Double> map) {
        return map != null ? map : Map.of();
    }
}
