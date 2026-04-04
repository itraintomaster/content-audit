package com.learney.contentaudit.auditdomain;


public interface ContentAnalyzer {
    Void onKnowledge(AuditNode node);

    Void onQuiz(AuditNode node);

    Void onMilestone(AuditNode node);

    Void onTopic(AuditNode node);

    Void onCourseComplete(AuditNode rootNode);

    String getName();

    AuditTarget getTarget();

    String getDescription();
}
