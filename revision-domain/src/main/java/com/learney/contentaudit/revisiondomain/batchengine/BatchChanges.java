package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import com.learney.contentaudit.revisiondomain.batch.BatchProposalRef;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * F-CJUEZ-R001: what a batch changes, counted once for everyone who needs it -- the judges' pass, to choose what to
 * judge, and the service, for the «sobre los N ejercicios que la tanda cambia hoy» of the estimate -- so that figure can
 * never say anything but what is judged. Each pending proposal of the batch names a node of the course it started from:
 * an exercise changes by itself; a topic -- its own instruction -- changes with every exercise it has. It is the same
 * count the comparison makes of the exercises under each projected change.
 */
final class BatchChanges {

    /** The ids of the exercises that change, in the order of the course the batch started from. */
    private final Set<String> quizzes;

    private BatchChanges(Set<String> quizzes) {
        this.quizzes = quizzes;
    }

    /** What the pending proposals of the batch change in the course it started from; an unknown node changes nothing. */
    static BatchChanges of(CorrectionBatch batch, CourseEntity startCourse) {
        Set<String> asked = new LinkedHashSet<>();
        for (BatchProposalRef ref : BatchBook.list(batch != null ? batch.getQuizProposals() : null)) {
            if (ref != null && ref.getQuizId() != null) {
                asked.add(ref.getQuizId());
            }
        }
        Set<String> quizzes = new LinkedHashSet<>();
        if (!asked.isEmpty() && startCourse != null && startCourse.getRoot() != null) {
            for (MilestoneEntity milestone : BatchBook.list(startCourse.getRoot().getMilestones())) {
                for (TopicEntity topic : BatchBook.list(milestone.getTopics())) {
                    for (KnowledgeEntity knowledge : BatchBook.list(topic.getKnowledges())) {
                        boolean wholeTopic = asked.contains(knowledge.getId());
                        for (QuizTemplateEntity quiz : BatchBook.list(knowledge.getQuizTemplates())) {
                            if (quiz != null && quiz.getId() != null && (wholeTopic || asked.contains(quiz.getId()))) {
                                quizzes.add(quiz.getId());
                            }
                        }
                    }
                }
            }
        }
        return new BatchChanges(quizzes);
    }

    /** How many distinct exercises change. */
    int quizCount() {
        return quizzes.size();
    }

    boolean isEmpty() {
        return quizzes.isEmpty();
    }

    boolean changes(QuizTemplateEntity quiz) {
        return quiz != null && quizzes.contains(quiz.getId());
    }

    /** A topic is touched when any of its exercises changes: a topic with a proposal of its own changes all of them. */
    boolean touches(KnowledgeEntity knowledge) {
        for (QuizTemplateEntity quiz : BatchBook.list(knowledge != null ? knowledge.getQuizTemplates() : null)) {
            if (changes(quiz)) {
                return true;
            }
        }
        return false;
    }
}
