package com.learney.contentaudit.auditdomain;
import javax.annotation.processing.Generated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IScoreAggregator implements ScoreAggregator {

    @Override
    public void aggregate(AuditNode rootNode) {
        bubbleUp(rootNode);
    }

    /**
     * For each node, if an analyzer has scores in children but not in the node itself,
     * compute the average of children scores and set it on the node.
     * Processes bottom-up (children first).
     */
    private void bubbleUp(AuditNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }

        // First, recurse into children
        for (AuditNode child : node.getChildren()) {
            bubbleUp(child);
        }

        // Collect all analyzer names that appear in children
        Map<String, List<Double>> childScoresByAnalyzer = new LinkedHashMap<>();
        for (AuditNode child : node.getChildren()) {
            if (child.getScores() != null) {
                for (Map.Entry<String, Double> entry : child.getScores().entrySet()) {
                    childScoresByAnalyzer.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
        }

        // For each analyzer: if node doesn't have a score yet, compute average from children
        if (node.getScores() == null) {
            node.setScores(new LinkedHashMap<>());
        }
        for (Map.Entry<String, List<Double>> entry : childScoresByAnalyzer.entrySet()) {
            if (!node.getScores().containsKey(entry.getKey())) {
                double avg = entry.getValue().stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                node.getScores().put(entry.getKey(), avg);
            }
        }
    }
}
