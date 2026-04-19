package com.learney.contentaudit.auditcli.formatting;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditdomain.AuditableEntity;
import java.util.List;
import java.util.Map;

public class TextReportFormatter implements ReportFormatter {
    private final DrillDownResolver resolver;

    public TextReportFormatter(DrillDownResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String format(ReportViewModel viewModel, DrillDownScope scope) {
        DrillDownView view = resolver.resolve(viewModel, scope);
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("=== %s === Score: %.1f%%\n",
                view.getNodeName(), view.getOverallScore() * 100));

        Map<String, Double> scores = view.getAnalyzerScores() != null ? view.getAnalyzerScores() : Map.of();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            sb.append(String.format("  %-35s %.1f%%\n", entry.getKey(), entry.getValue() * 100));
        }

        List<ScoreRow> children = view.getChildRows();
        if (children != null && !children.isEmpty()) {
            String childLabel = childLabel(view.getDepth());
            sb.append(String.format("\n%ss:\n", childLabel));
            for (ScoreRow child : children) {
                String header = formatHeader(child);
                sb.append(String.format("\n  --- %s --- Score: %.1f%%\n",
                        header, child.getOverallScore() * 100));
                Map<String, Double> childScores = child.getAnalyzerScores() != null
                        ? child.getAnalyzerScores() : Map.of();
                for (Map.Entry<String, Double> entry : childScores.entrySet()) {
                    sb.append(String.format("    %-33s %.1f%%\n", entry.getKey(), entry.getValue() * 100));
                }
            }
        }

        return sb.toString();
    }

    private String formatHeader(ScoreRow row) {
        AuditableEntity entity = row.getEntity();
        if (entity != null && entity.getLabel() != null) {
            return entity.getLabel();
        }
        if (row instanceof MilestoneScoreRow m) return m.getMilestoneId();
        if (row instanceof TopicScoreRow t) return t.getTopicId();
        if (row instanceof KnowledgeScoreRow k) return k.getKnowledgeId();
        if (row instanceof QuizScoreRow q) return q.getQuizId();
        if (row instanceof ChildScoreRow c) return c.getId();
        return "?";
    }

    private String childLabel(DrillDownLevel depth) {
        return switch (depth) {
            case COURSE -> "Level";
            case MILESTONE -> "Topic";
            case TOPIC -> "Knowledge";
            case KNOWLEDGE -> "Quiz";
        };
    }
}
