package com.learney.contentaudit.auditcli;

import java.util.List;
import java.util.Map;

public class TableReportFormatter implements ReportFormatter {

    @Override
    public String format(ReportViewModel viewModel) {
        StringBuilder sb = new StringBuilder();

        List<String> analyzers = viewModel.getAnalyzerNames() != null ? viewModel.getAnalyzerNames() : List.of();
        List<MilestoneScoreRow> milestoneScores = viewModel.getMilestoneScores() != null
                ? viewModel.getMilestoneScores() : List.of();

        // Build level column names: one per milestone (A1, A2, B1, B2 or L0, L1, ...)
        int levelCount = milestoneScores.size();
        String[] defaultNames = {"A1", "A2", "B1", "B2"};
        String[] levelNames = new String[levelCount];
        for (int i = 0; i < levelCount; i++) {
            levelNames[i] = i < defaultNames.length ? defaultNames[i] : "L" + i;
        }

        // Column widths
        int analyzerCol = analyzers.stream().mapToInt(String::length).max().orElse(8);
        analyzerCol = Math.max(analyzerCol, 8);
        int scoreCol = 10;

        // Header
        sb.append(String.format("%-" + analyzerCol + "s", "Analyzer"));
        for (String level : levelNames) {
            sb.append(String.format("  %" + scoreCol + "s", level));
        }
        sb.append(String.format("  %" + scoreCol + "s", "COURSE"));
        sb.append("\n");
        sb.append("-".repeat(analyzerCol + (levelCount + 1) * (scoreCol + 2))).append("\n");

        // Course-level per-analyzer scores
        Map<String, Double> courseScores = viewModel.getAnalyzerScores() != null
                ? viewModel.getAnalyzerScores() : Map.of();

        // One row per analyzer
        for (String analyzer : analyzers) {
            sb.append(String.format("%-" + analyzerCol + "s", analyzer));
            for (int i = 0; i < levelCount; i++) {
                Map<String, Double> scores = milestoneScores.get(i).getAnalyzerScores() != null
                        ? milestoneScores.get(i).getAnalyzerScores() : Map.of();
                Double score = scores.get(analyzer);
                sb.append(String.format("  %" + scoreCol + "s", score != null ? formatScore(score) : "-"));
            }
            Double courseScore = courseScores.get(analyzer);
            sb.append(String.format("  %" + scoreCol + "s", courseScore != null ? formatScore(courseScore) : "-"));
            sb.append("\n");
        }

        // Footer: OVERALL row
        sb.append("-".repeat(analyzerCol + (levelCount + 1) * (scoreCol + 2))).append("\n");
        sb.append(String.format("%-" + analyzerCol + "s", "OVERALL"));
        for (int i = 0; i < levelCount; i++) {
            sb.append(String.format("  %" + scoreCol + "s", formatScore(milestoneScores.get(i).getOverallScore())));
        }
        sb.append(String.format("  %" + scoreCol + "s", formatScore(viewModel.getOverallScore())));
        sb.append("\n");

        return sb.toString();
    }

    private String formatScore(double score) {
        return String.format("%.1f%%", score * 100);
    }
}
