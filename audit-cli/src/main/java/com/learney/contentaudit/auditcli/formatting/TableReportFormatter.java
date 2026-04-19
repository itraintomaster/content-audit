package com.learney.contentaudit.auditcli.formatting;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditdomain.AuditableEntity;
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
        List<ScoreRow> children = view.getChildRows() != null ? view.getChildRows() : List.of();

        int analyzerCol = analyzers.stream().mapToInt(String::length).max().orElse(8);
        analyzerCol = Math.max(analyzerCol, 8);
        int scoreCol = 10;
        int colCount = children.size() + 1;

        // Header: child labels as columns
        sb.append(String.format("%-" + analyzerCol + "s", "Analyzer"));
        for (ScoreRow child : children) {
            AuditableEntity entity = child.getEntity();
            String header = entity != null && entity.getLabel() != null ? entity.getLabel() : idOf(child);
            sb.append(String.format("  %" + scoreCol + "s", truncate(header, scoreCol)));
        }
        sb.append(String.format("  %" + scoreCol + "s", view.getNodeName()));
        sb.append("\n");
        sb.append("-".repeat(analyzerCol + colCount * (scoreCol + 2))).append("\n");

        Map<String, Double> parentScores = view.getAnalyzerScores() != null
                ? view.getAnalyzerScores() : Map.of();

        for (String analyzer : analyzers) {
            sb.append(String.format("%-" + analyzerCol + "s", analyzer));
            for (ScoreRow child : children) {
                Map<String, Double> scores = child.getAnalyzerScores() != null
                        ? child.getAnalyzerScores() : Map.of();
                Double score = scores.get(analyzer);
                sb.append(String.format("  %" + scoreCol + "s", score != null ? formatScore(score) : "-"));
            }
            Double parentScore = parentScores.get(analyzer);
            sb.append(String.format("  %" + scoreCol + "s", parentScore != null ? formatScore(parentScore) : "-"));
            sb.append("\n");
        }

        sb.append("-".repeat(analyzerCol + colCount * (scoreCol + 2))).append("\n");
        sb.append(String.format("%-" + analyzerCol + "s", "OVERALL"));
        for (ScoreRow child : children) {
            sb.append(String.format("  %" + scoreCol + "s", formatScore(child.getOverallScore())));
        }
        sb.append(String.format("  %" + scoreCol + "s", formatScore(view.getOverallScore())));
        sb.append("\n");

        return sb.toString();
    }

    private String idOf(ScoreRow row) {
        if (row instanceof MilestoneScoreRow m) return m.getMilestoneId();
        if (row instanceof TopicScoreRow t) return t.getTopicId();
        if (row instanceof KnowledgeScoreRow k) return k.getKnowledgeId();
        if (row instanceof QuizScoreRow q) return q.getQuizId();
        if (row instanceof ChildScoreRow c) return c.getId();
        return "?";
    }

    private String formatScore(double score) {
        return String.format("%.1f%%", score * 100);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "?";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "~";
    }
}
