package com.learney.contentaudit.auditdomain;

import com.learney.contentaudit.auditdomain.finding.FindingDraft;
import java.util.List;

public interface ContentAnalyzer {
    Void onKnowledge(AuditNode node);

    Void onQuiz(AuditNode node);

    Void onMilestone(AuditNode node);

    Void onTopic(AuditNode node);

    Void onCourseComplete(AuditNode rootNode);

    String getName();

    AuditTarget getTarget();

    String getDescription();

    /**
     * F-HALL-R001/R014: what this analyzer found on a node it evaluated, as drafts the
     * engine validates against the analyzer's card and stamps (analyzer, node, cost,
     * identity). The engine asks right after this analyzer's own traversal and before
     * aggregating, only on the levels its card declares as evaluated and only where the
     * analyzer left its own score; an empty list means the node passes.
     */
    List<FindingDraft> findingsAt(AuditNode node);
}
