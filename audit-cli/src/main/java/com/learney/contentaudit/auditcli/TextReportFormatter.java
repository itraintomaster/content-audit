package com.learney.contentaudit.auditcli;

import java.util.List;
import java.util.Map;

public class TextReportFormatter implements ReportFormatter {
    @Override
    public String format(ReportViewModel viewModel) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Audit Report ===\n");
        sb.append(String.format("Overall Score: %.1f%%\n", viewModel.getOverallScore() * 100));

        List<MilestoneScoreRow> milestoneScores = viewModel.getMilestoneScores();
        if (milestoneScores == null || milestoneScores.isEmpty()) {
            sb.append("\nNo milestones found.\n");
        } else {
            for (MilestoneScoreRow row : milestoneScores) {
                String id = row.getMilestoneId() != null ? row.getMilestoneId() : "?";
                Map<String, Double> scores = row.getAnalyzerScores() != null ? row.getAnalyzerScores() : Map.of();

                sb.append(String.format("\n--- %s --- Score: %.1f%%\n", id, row.getOverallScore() * 100));
                for (Map.Entry<String, Double> entry : scores.entrySet()) {
                    sb.append(String.format("  %-35s %.1f%%\n", entry.getKey(), entry.getValue() * 100));
                }
            }
        }
        return sb.toString();
    }
}
