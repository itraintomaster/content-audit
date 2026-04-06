package com.learney.contentaudit.auditcli.formatting;

import java.util.Objects;

/**
 * A simple row representing a leaf-level scored item for per-analyzer stats views.
 * Holds path identifiers and score so worst-items lists can be displayed.
 */
public class ScoredItemRow {
    private final String milestoneId;
    private final String topicId;
    private final String knowledgeId;
    private final String quizId;
    private final double score;
    private final String label;

    public ScoredItemRow(String milestoneId, String topicId, String knowledgeId, String quizId,
            double score, String label) {
        this.milestoneId = milestoneId;
        this.topicId = topicId;
        this.knowledgeId = knowledgeId;
        this.quizId = quizId;
        this.score = score;
        this.label = label;
    }

    public String getMilestoneId() {
        return milestoneId;
    }

    public String getTopicId() {
        return topicId;
    }

    public String getKnowledgeId() {
        return knowledgeId;
    }

    public String getQuizId() {
        return quizId;
    }

    public double getScore() {
        return score;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoredItemRow that = (ScoredItemRow) o;
        return Double.compare(that.score, score) == 0
                && Objects.equals(milestoneId, that.milestoneId)
                && Objects.equals(topicId, that.topicId)
                && Objects.equals(knowledgeId, that.knowledgeId)
                && Objects.equals(quizId, that.quizId)
                && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(milestoneId, topicId, knowledgeId, quizId, score, label);
    }
}
