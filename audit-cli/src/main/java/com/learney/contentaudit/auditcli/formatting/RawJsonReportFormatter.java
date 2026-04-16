package com.learney.contentaudit.auditcli.formatting;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import java.util.List;
import java.util.Map;

public class RawJsonReportFormatter implements RawReportFormatter {

    @Override
    public String format(AuditReport report) {
        AuditNode root = report.getRoot();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        Map<String, Double> rootScores = root != null ? root.getScores() : null;
        double overallScore = avgScores(rootScores);
        sb.append("  \"overallScore\": ").append(overallScore);

        // Course-level scores map
        sb.append(",\n  \"scores\": ");
        appendScoresMap(sb, rootScores, 2);

        // Milestones (children of root)
        sb.append(",\n  \"milestones\": [");
        List<AuditNode> milestones = (root != null && root.getChildren() != null)
                ? root.getChildren() : List.of();
        if (!milestones.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < milestones.size(); i++) {
                appendMilestoneNode(sb, milestones.get(i), 4);
                if (i < milestones.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ");
        }
        sb.append("]\n}");
        return sb.toString();
    }

    private void appendMilestoneNode(StringBuilder sb, AuditNode milestone, int indent) {
        String pad = " ".repeat(indent);
        String milestoneId = entityId(milestone);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"milestoneId\": \"").append(escape(milestoneId)).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, milestone.getScores(), indent + 2);

        List<AuditNode> topics = milestone.getChildren() != null ? milestone.getChildren() : List.of();
        sb.append(",\n").append(pad).append("  \"topics\": [");
        if (!topics.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < topics.size(); i++) {
                appendTopicNode(sb, topics.get(i), indent + 4);
                if (i < topics.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(pad).append("  ");
        }
        sb.append("]");
        sb.append("\n").append(pad).append("}");
    }

    private void appendTopicNode(StringBuilder sb, AuditNode topic, int indent) {
        String pad = " ".repeat(indent);
        String topicId = entityId(topic);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"topicId\": \"").append(escape(topicId)).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, topic.getScores(), indent + 2);

        List<AuditNode> knowledges = topic.getChildren() != null ? topic.getChildren() : List.of();
        sb.append(",\n").append(pad).append("  \"knowledges\": [");
        if (!knowledges.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < knowledges.size(); i++) {
                appendKnowledgeNode(sb, knowledges.get(i), indent + 4);
                if (i < knowledges.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(pad).append("  ");
        }
        sb.append("]");
        sb.append("\n").append(pad).append("}");
    }

    private void appendKnowledgeNode(StringBuilder sb, AuditNode knowledge, int indent) {
        String pad = " ".repeat(indent);
        String knowledgeId = entityId(knowledge);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"knowledgeId\": \"").append(escape(knowledgeId)).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, knowledge.getScores(), indent + 2);

        List<AuditNode> quizzes = knowledge.getChildren() != null ? knowledge.getChildren() : List.of();
        sb.append(",\n").append(pad).append("  \"quizzes\": [");
        if (!quizzes.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < quizzes.size(); i++) {
                appendQuizNode(sb, quizzes.get(i), indent + 4);
                if (i < quizzes.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(pad).append("  ");
        }
        sb.append("]");
        sb.append("\n").append(pad).append("}");
    }

    private void appendQuizNode(StringBuilder sb, AuditNode quiz, int indent) {
        String pad = " ".repeat(indent);
        String quizId = entityId(quiz);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"quizId\": \"").append(escape(quizId)).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, quiz.getScores(), indent + 2);
        sb.append("\n").append(pad).append("}");
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

    private double avgScores(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) return 0.0;
        return scores.values().stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    private String entityId(AuditNode node) {
        if (node.getEntity() != null && node.getEntity().getId() != null) {
            return node.getEntity().getId();
        }
        return "";
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
