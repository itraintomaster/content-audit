package com.learney.contentaudit.auditcli;

import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.KnowledgeNode;
import com.learney.contentaudit.auditdomain.MilestoneNode;
import com.learney.contentaudit.auditdomain.NodeScores;
import com.learney.contentaudit.auditdomain.QuizNode;
import com.learney.contentaudit.auditdomain.TopicNode;
import java.util.List;
import java.util.Map;

public final class RawJsonReportFormatter implements RawReportFormatter {

    @Override
    public String format(AuditReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"overallScore\": ").append(report.getOverallScore());

        // Course-level scores map
        sb.append(",\n  \"scores\": ");
        appendScoresMap(sb, report.getScores(), 2);

        // Milestones
        sb.append(",\n  \"milestones\": [");
        List<MilestoneNode> milestones = report.getMilestones();
        if (milestones != null && !milestones.isEmpty()) {
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

    private void appendMilestoneNode(StringBuilder sb, MilestoneNode milestone, int indent) {
        String pad = " ".repeat(indent);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"milestoneId\": \"").append(escape(milestone.getMilestoneId())).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, milestone.getScores(), indent + 2);

        List<TopicNode> topics = milestone.getTopics();
        sb.append(",\n").append(pad).append("  \"topics\": [");
        if (topics != null && !topics.isEmpty()) {
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

    private void appendTopicNode(StringBuilder sb, TopicNode topic, int indent) {
        String pad = " ".repeat(indent);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"topicId\": \"").append(escape(topic.getTopicId())).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, topic.getScores(), indent + 2);

        List<KnowledgeNode> knowledges = topic.getKnowledges();
        sb.append(",\n").append(pad).append("  \"knowledges\": [");
        if (knowledges != null && !knowledges.isEmpty()) {
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

    private void appendKnowledgeNode(StringBuilder sb, KnowledgeNode knowledge, int indent) {
        String pad = " ".repeat(indent);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"knowledgeId\": \"").append(escape(knowledge.getKnowledgeId())).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, knowledge.getScores(), indent + 2);

        List<QuizNode> quizzes = knowledge.getQuizzes();
        sb.append(",\n").append(pad).append("  \"quizzes\": [");
        if (quizzes != null && !quizzes.isEmpty()) {
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

    private void appendQuizNode(StringBuilder sb, QuizNode quiz, int indent) {
        String pad = " ".repeat(indent);
        sb.append(pad).append("{\n");
        sb.append(pad).append("  \"quizId\": \"").append(escape(quiz.getQuizId())).append("\"");
        sb.append(",\n").append(pad).append("  \"scores\": ");
        appendScoresMap(sb, quiz.getScores(), indent + 2);
        sb.append("\n").append(pad).append("}");
    }

    private void appendScoresMap(StringBuilder sb, NodeScores nodeScores, int indent) {
        String pad = " ".repeat(indent);
        if (nodeScores == null || nodeScores.getScores() == null || nodeScores.getScores().isEmpty()) {
            sb.append("{}");
            return;
        }
        Map<String, Double> scores = nodeScores.getScores();
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

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
