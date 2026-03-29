package com.learney.contentaudit.auditcli;

import java.util.List;
import java.util.Map;

public class JsonReportFormatter implements ReportFormatter {
    private final DrillDownResolver resolver;

    public JsonReportFormatter(DrillDownResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String format(ReportViewModel viewModel, DrillDownScope scope) {
        DrillDownView view = resolver.resolve(viewModel, scope);
        StringBuilder sb = new StringBuilder();

        sb.append("{\n");
        sb.append("  \"depth\": \"").append(view.getDepth()).append("\"");
        sb.append(",\n  \"name\": \"").append(escape(view.getNodeName())).append("\"");
        sb.append(",\n  \"overallScore\": ").append(view.getOverallScore());

        // Analyzer scores
        sb.append(",\n  \"scores\": ");
        appendScoresMap(sb, view.getAnalyzerScores(), 2);

        // Children
        List<ChildScoreRow> children = view.getChildRows();
        String childKey = childKey(view.getDepth());
        sb.append(",\n  \"").append(childKey).append("\": [");
        if (children != null && !children.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < children.size(); i++) {
                ChildScoreRow child = children.get(i);
                sb.append("    {\n");
                sb.append("      \"id\": \"").append(escape(child.getId())).append("\"");
                sb.append(",\n      \"overallScore\": ").append(child.getOverallScore());
                sb.append(",\n      \"scores\": ");
                appendScoresMap(sb, child.getAnalyzerScores(), 6);
                sb.append("\n    }");
                if (i < children.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ");
        }
        sb.append("]\n}");

        return sb.toString();
    }

    private void appendScoresMap(StringBuilder sb, Map<String, Double> scores, int indent) {
        String pad = " ".repeat(indent);
        if (scores == null || scores.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            sb.append(pad).append("  \"").append(escape(entry.getKey())).append("\": ").append(entry.getValue());
            if (i < scores.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append(pad).append("}");
    }

    private String childKey(DrillDownLevel depth) {
        return switch (depth) {
            case COURSE -> "levels";
            case MILESTONE -> "topics";
            case TOPIC -> "knowledges";
            case KNOWLEDGE -> "quizzes";
        };
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
