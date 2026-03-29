package com.learney.contentaudit.auditcli;

import java.util.List;
import java.util.Map;

public class TableReportFormatter implements ReportFormatter {
    private final DrillDownResolver resolver;

    public TableReportFormatter(DrillDownResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String format(ReportViewModel viewModel, DrillDownScope scope) {
        DrillDownView view = resolver.resolve(viewModel, scope);
        StringBuilder sb = new StringBuilder();

        List<String> analyzers = view.getAnalyzerNames() != null ? view.getAnalyzerNames() : List.of();
        List<ChildScoreRow> children = view.getChildRows() != null ? view.getChildRows() : List.of();

        // Column widths
        int analyzerCol = analyzers.stream().mapToInt(String::length).max().orElse(8);
        analyzerCol = Math.max(analyzerCol, 8);
        int scoreCol = 10;
        int colCount = children.size() + 1; // children + parent total

        // Header: child ids as columns, plus parent node name
        sb.append(String.format("%-" + analyzerCol + "s", "Analyzer"));
        for (ChildScoreRow child : children) {
            String label = truncate(child.getId(), scoreCol);
            sb.append(String.format("  %" + scoreCol + "s", label));
        }
        sb.append(String.format("  %" + scoreCol + "s", view.getNodeName()));
        sb.append("\n");
        sb.append("-".repeat(analyzerCol + colCount * (scoreCol + 2))).append("\n");

        // One row per analyzer
        Map<String, Double> parentScores = view.getAnalyzerScores() != null
                ? view.getAnalyzerScores() : Map.of();

        for (String analyzer : analyzers) {
            sb.append(String.format("%-" + analyzerCol + "s", analyzer));
            for (ChildScoreRow child : children) {
                Map<String, Double> scores = child.getAnalyzerScores() != null
                        ? child.getAnalyzerScores() : Map.of();
                Double score = scores.get(analyzer);
                sb.append(String.format("  %" + scoreCol + "s", score != null ? formatScore(score) : "-"));
            }
            Double parentScore = parentScores.get(analyzer);
            sb.append(String.format("  %" + scoreCol + "s", parentScore != null ? formatScore(parentScore) : "-"));
            sb.append("\n");
        }

        // Footer: OVERALL row
        sb.append("-".repeat(analyzerCol + colCount * (scoreCol + 2))).append("\n");
        sb.append(String.format("%-" + analyzerCol + "s", "OVERALL"));
        for (ChildScoreRow child : children) {
            sb.append(String.format("  %" + scoreCol + "s", formatScore(child.getOverallScore())));
        }
        sb.append(String.format("  %" + scoreCol + "s", formatScore(view.getOverallScore())));
        sb.append("\n");

        return sb.toString();
    }

    private String formatScore(double score) {
        return String.format("%.1f%%", score * 100);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "?";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "~";
    }
}
