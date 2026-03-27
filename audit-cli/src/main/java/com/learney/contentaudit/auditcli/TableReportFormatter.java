package com.learney.contentaudit.auditcli;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.KnowledgeNode;
import com.learney.contentaudit.auditdomain.MilestoneNode;
import com.learney.contentaudit.auditdomain.TopicNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableReportFormatter implements ReportFormatter {

    @Override
    public String format(AuditReport report) {
        StringBuilder sb = new StringBuilder();

        // Collect all analyzer names across all milestones
        Set<String> analyzerNames = new LinkedHashSet<>();
        List<MilestoneNode> milestones = report.getMilestones();
        if (milestones != null) {
            for (MilestoneNode m : milestones) {
                collectAnalyzerNames(m, analyzerNames);
            }
        }

        List<String> analyzers = List.copyOf(analyzerNames);

        // Header
        sb.append(String.format("%-12s", "Level"));
        for (String a : analyzers) {
            sb.append(String.format("  %-20s", a));
        }
        sb.append("  OVERALL\n");
        sb.append("-".repeat(14 + analyzers.size() * 22 + 9)).append("\n");

        // Milestone rows
        String[] levelNames = {"A1", "A2", "B1", "B2"};
        if (milestones != null) {
            for (int i = 0; i < milestones.size(); i++) {
                MilestoneNode m = milestones.get(i);
                String level = i < levelNames.length ? levelNames[i] : "L" + i;
                sb.append(String.format("%-12s", level));

                Map<String, Double> scores = m.getScores() != null && m.getScores().getScores() != null
                        ? m.getScores().getScores() : Map.of();

                for (String a : analyzers) {
                    Double score = scores.get(a);
                    if (score != null) {
                        sb.append(String.format("  %-20s", formatScore(score)));
                    } else {
                        sb.append(String.format("  %-20s", "-"));
                    }
                }

                // Milestone overall (average of all analyzer scores)
                if (!scores.isEmpty()) {
                    double avg = scores.values().stream().mapToDouble(d -> d).average().orElse(0);
                    sb.append(String.format("  %s", formatScore(avg)));
                } else {
                    sb.append("  -");
                }
                sb.append("\n");
            }
        }

        // Footer: course overall
        sb.append("-".repeat(14 + analyzers.size() * 22 + 9)).append("\n");
        sb.append(String.format("%-12s", "COURSE"));

        if (report.getScores() != null && report.getScores().getScores() != null) {
            Map<String, Double> courseScores = report.getScores().getScores();
            for (String a : analyzers) {
                Double score = courseScores.get(a);
                if (score != null) {
                    sb.append(String.format("  %-20s", formatScore(score)));
                } else {
                    sb.append(String.format("  %-20s", "-"));
                }
            }
        } else {
            for (String ignored : analyzers) {
                sb.append(String.format("  %-20s", "-"));
            }
        }
        sb.append(String.format("  %s", formatScore(report.getOverallScore())));
        sb.append("\n");

        return sb.toString();
    }

    private void collectAnalyzerNames(MilestoneNode milestone, Set<String> names) {
        if (milestone.getScores() != null && milestone.getScores().getScores() != null) {
            names.addAll(milestone.getScores().getScores().keySet());
        }
        if (milestone.getTopics() != null) {
            for (TopicNode topic : milestone.getTopics()) {
                if (topic.getScores() != null && topic.getScores().getScores() != null) {
                    names.addAll(topic.getScores().getScores().keySet());
                }
            }
        }
    }

    private String formatScore(double score) {
        return String.format("%.1f%%", score * 100);
    }
}
