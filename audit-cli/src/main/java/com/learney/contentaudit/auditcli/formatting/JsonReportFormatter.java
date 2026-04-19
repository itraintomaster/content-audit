package com.learney.contentaudit.auditcli.formatting;
import javax.annotation.processing.Generated;

import com.learney.contentaudit.auditdomain.AuditableEntity;
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

        sb.append(",\n  \"scores\": ");
        appendScoresMap(sb, view.getAnalyzerScores(), 2);

        List<ScoreRow> children = view.getChildRows();
        String childKey = childKey(view.getDepth());
        sb.append(",\n  \"").append(childKey).append("\": [");
        if (children != null && !children.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < children.size(); i++) {
                ScoreRow child = children.get(i);
                sb.append("    {\n");
                sb.append("      \"id\": \"").append(escape(idOf(child))).append("\"");
                AuditableEntity entity = child.getEntity();
                if (entity != null && entity.getLabel() != null) {
                    sb.append(",\n      \"label\": \"").append(escape(entity.getLabel())).append("\"");
                }
                if (entity != null && entity.getCode() != null) {
                    sb.append(",\n      \"code\": \"").append(escape(entity.getCode())).append("\"");
                }
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

    private String idOf(ScoreRow row) {
        if (row instanceof MilestoneScoreRow m) return m.getMilestoneId();
        if (row instanceof TopicScoreRow t) return t.getTopicId();
        if (row instanceof KnowledgeScoreRow k) return k.getKnowledgeId();
        if (row instanceof QuizScoreRow q) return q.getQuizId();
        if (row instanceof ChildScoreRow c) return c.getId();
        return "?";
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
