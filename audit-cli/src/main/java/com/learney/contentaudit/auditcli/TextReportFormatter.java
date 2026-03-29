package com.learney.contentaudit.auditcli;

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

        // Header: focused node
        sb.append(String.format("=== %s === Score: %.1f%%\n",
                view.getNodeName(), view.getOverallScore() * 100));

        // Focused node's analyzer scores
        Map<String, Double> scores = view.getAnalyzerScores() != null ? view.getAnalyzerScores() : Map.of();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            sb.append(String.format("  %-35s %.1f%%\n", entry.getKey(), entry.getValue() * 100));
        }

        // Children
        List<ChildScoreRow> children = view.getChildRows();
        if (children != null && !children.isEmpty()) {
            String childLabel = childLabel(view.getDepth());
            sb.append(String.format("\n%ss:\n", childLabel));
            for (ChildScoreRow child : children) {
                sb.append(String.format("\n  --- %s --- Score: %.1f%%\n",
                        child.getId(), child.getOverallScore() * 100));
                Map<String, Double> childScores = child.getAnalyzerScores() != null
                        ? child.getAnalyzerScores() : Map.of();
                for (Map.Entry<String, Double> entry : childScores.entrySet()) {
                    sb.append(String.format("    %-33s %.1f%%\n", entry.getKey(), entry.getValue() * 100));
                }
            }
        }

        return sb.toString();
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
