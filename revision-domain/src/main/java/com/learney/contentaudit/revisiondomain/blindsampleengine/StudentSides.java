package com.learney.contentaudit.revisiondomain.blindsampleengine;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditReport;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.revisiondomain.blindsample.StudentSide;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * What both blind samples share (F-CIEGA-R002, R006): an exercise of a photo as the student sees it -- before
 * answering, the view the renderer builds; after, the finished sentence in the mode of its knowledge -- and
 * nothing else of the exercise, the exercises below a node of a photo, and the id of a new sample.
 */
final class StudentSides {
    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")
            .withZone(ZoneOffset.UTC);

    private StudentSides() {
    }

    /** The exercise as the student sees it, before and after answering. */
    static StudentSide side(AuditNode quizNode, QuizStudentViewRenderer renderer) {
        AuditableQuiz quiz = (AuditableQuiz) quizNode.getEntity();
        SentenceMode mode = SentenceMode.FILL;
        if (quizNode.getParent() != null && quizNode.getParent().getEntity() instanceof AuditableKnowledge knowledge
                && knowledge.getSentenceMode() != null) {
            mode = knowledge.getSentenceMode();
        }
        return new StudentSide(renderer.render(quiz), renderer.finishedSentence(quiz, mode));
    }

    /** The exercises of a photo below a node, by id, in the order of the course; parents set where missing. */
    static Map<String, AuditNode> quizzes(AuditNode node) {
        Map<String, AuditNode> quizzes = new LinkedHashMap<>();
        collect(node, null, quizzes);
        return quizzes;
    }

    static Map<String, AuditNode> quizzes(AuditReport report) {
        return report != null && report.getRoot() != null ? quizzes(report.getRoot()) : new LinkedHashMap<>();
    }

    private static void collect(AuditNode node, AuditNode parent, Map<String, AuditNode> quizzes) {
        if (node == null) {
            return;
        }
        if (node.getParent() == null && parent != null) {
            node.setParent(parent);
        }
        if (node.getTarget() == AuditTarget.QUIZ && node.getEntity() instanceof AuditableQuiz quiz
                && quiz.getId() != null) {
            quizzes.putIfAbsent(quiz.getId(), node);
        }
        if (node.getChildren() != null) {
            for (AuditNode child : node.getChildren()) {
                collect(child, node, quizzes);
            }
        }
    }

    /** «<prefix>-2026-10-01T10-00-00», with -2, -3… when that one is taken. */
    static String newId(String prefix, Instant now, Predicate<String> taken) {
        String base = prefix + "-" + ID_TIME.format(now);
        String id = base;
        for (int n = 2; taken.test(id); n++) {
            id = base + "-" + n;
        }
        return id;
    }
}
