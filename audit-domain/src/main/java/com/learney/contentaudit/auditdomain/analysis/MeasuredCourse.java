package com.learney.contentaudit.auditdomain.analysis;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableCourse;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The course a report measured, rebuilt from its own entities -- the same exercises and the same tokens -- so it can be
 * measured again: by the comparison, with today's measure (F-METAS-R007, F-MIDIO-R006), or with one change undone.
 */
public final class MeasuredCourse {
    private MeasuredCourse() {
    }

    /**
     * The course the report measured, rebuilt from the entities of its nodes, with the nodes of {@code replaced}
     * (by id) taken from another report instead: an exercise is its entity; a knowledge, its entity with the
     * exercises of its node. Null when a node has no entity to rebuild it from.
     */
    public static AuditableCourse of(AuditReport report, Map<String, AuditNode> replaced) {
        if (report == null || report.getRoot() == null || report.getRoot().getChildren() == null) {
            return null;
        }
        List<AuditableMilestone> levels = new ArrayList<>();
        for (AuditNode level : report.getRoot().getChildren()) {
            if (!(level.getEntity() instanceof AuditableMilestone milestone)) {
                return null;
            }
            List<AuditableTopic> topics = new ArrayList<>();
            for (AuditNode topicNode : children(level)) {
                if (!(topicNode.getEntity() instanceof AuditableTopic topic)) {
                    return null;
                }
                List<AuditableKnowledge> knowledges = new ArrayList<>();
                for (AuditNode knowledgeNode : children(topicNode)) {
                    AuditNode source = replaced.getOrDefault(idOf(knowledgeNode), knowledgeNode);
                    AuditableKnowledge rebuilt = knowledge(source, replaced);
                    if (rebuilt == null) {
                        return null;
                    }
                    knowledges.add(rebuilt);
                }
                topics.add(new AuditableTopic(knowledges, topic.getId(), topic.getLabel(), topic.getCode()));
            }
            levels.add(new AuditableMilestone(topics, milestone.getId(), milestone.getLabel(), milestone.getCode()));
        }
        return new AuditableCourse(levels);
    }

    private static AuditableKnowledge knowledge(AuditNode node, Map<String, AuditNode> replaced) {
        if (!(node.getEntity() instanceof AuditableKnowledge knowledge)) {
            return null;
        }
        List<AuditableQuiz> quizzes = new ArrayList<>();
        for (AuditNode quizNode : children(node)) {
            AuditNode source = replaced.getOrDefault(idOf(quizNode), quizNode);
            if (!(source.getEntity() instanceof AuditableQuiz quiz)) {
                return null;
            }
            quizzes.add(quiz);
        }
        return new AuditableKnowledge(quizzes, knowledge.getTitle(), knowledge.getInstructions(),
                knowledge.isIsSentence(), knowledge.getId(), knowledge.getLabel(), knowledge.getCode(),
                knowledge.getSentenceMode(), knowledge.getTopicName());
    }

    /** The id a node is replaced by: its entity's, or "root" for the course. */
    private static String idOf(AuditNode node) {
        if (node.getEntity() != null && node.getEntity().getId() != null) {
            return node.getEntity().getId();
        }
        return node.getTarget() == AuditTarget.COURSE ? "root" : null;
    }

    private static List<AuditNode> children(AuditNode node) {
        return node.getChildren() != null ? node.getChildren() : List.of();
    }
}
