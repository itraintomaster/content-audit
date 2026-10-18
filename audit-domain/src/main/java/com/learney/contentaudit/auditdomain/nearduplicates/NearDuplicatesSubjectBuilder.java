package com.learney.contentaudit.auditdomain.nearduplicates;

import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.QuizStudentViewRenderer;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationSubject;
import java.util.LinkedHashMap;

/** Integration depends on every possible partner in the topic, in its original order. */
public final class NearDuplicatesSubjectBuilder {
    private final SameExerciseQuestion question;

    public NearDuplicatesSubjectBuilder(QuizStudentViewRenderer renderer) {
        question = new DefaultSameExerciseQuestion(renderer);
    }

    public EvaluationSubject build(AuditNode node) {
        AuditNode topic = node.getTarget() == AuditTarget.KNOWLEDGE ? node
                : node.ancestor(AuditTarget.KNOWLEDGE).orElseThrow();
        var content = new LinkedHashMap<String, String>();
        int position = 0;
        for (AuditNode child : topic.getChildren()) if (child.getEntity() instanceof AuditableQuiz quiz) {
            String prefix = position++ + ":" + quiz.getId() + ":";
            // Reuse the exact statement, options, accepted answer and solved English used by the pair judge.
            question.subjectOf(quiz, quiz).getContent().forEach((key, value) -> content.put(prefix + key, value));
        }
        return new EvaluationSubject(topic.getEntity().getId(), content);
    }
}
