package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * F-CJUEZ-R001: the four cuts of the course the judges read when the batch closes -- the exercises that change and the
 * topics they belong to, each as the course the batch started from has them (before) and as the batch leaves them (after).
 * A cut keeps the real ancestors of what it holds -- level, topic, topic of the exercises, with their labels and the
 * instruction of the topic --, because that is what a judge reads above an exercise: the fingerprints of a cut are the
 * ones of the whole course, and what is judged in a cut is what the comparison finds in the ledger afterwards. Only lists
 * of children are filtered; the levels, topic groups and topics that end up empty are left out. A cut with nothing in it
 * is null.
 *
 * <p>On the after side an exercise identical to its version before -- a change that only asks to record its audio again --
 * is left out: the before side covers it (R001 inv. 3). So is a topic whose content did not move.
 *
 * @param exercisesBefore the exercises that change, in the start course
 * @param exercisesAfter  the ones that change and differ, in the composed course
 * @param topicsBefore    the topics with an exercise that changes, whole, in the start course
 * @param topicsAfter     the ones whose content differs, whole, in the composed course
 */
record ChangedCourseCuts(CourseEntity exercisesBefore, CourseEntity exercisesAfter, CourseEntity topicsBefore,
        CourseEntity topicsAfter) {

    static ChangedCourseCuts of(CourseEntity start, CourseEntity composed, BatchChanges changes) {
        Map<String, QuizTemplateEntity> startQuizzes = new HashMap<>();
        Map<String, KnowledgeEntity> startTopics = new HashMap<>();
        index(start, startQuizzes, startTopics);
        return new ChangedCourseCuts(
                cut(start, knowledge -> exercises(knowledge, changes, quiz -> true)),
                cut(composed, knowledge -> exercises(knowledge, changes,
                        quiz -> !Objects.equals(quiz, startQuizzes.get(quiz.getId())))),
                cut(start, knowledge -> changes.touches(knowledge) ? knowledge : null),
                cut(composed, knowledge -> changes.touches(knowledge)
                        && !Objects.equals(knowledge, startTopics.get(knowledge.getId())) ? knowledge : null));
    }

    /** The topic with only the exercises of it that change and that {@code keep} holds; null when none is left. */
    private static KnowledgeEntity exercises(KnowledgeEntity knowledge, BatchChanges changes,
            Predicate<QuizTemplateEntity> keep) {
        List<QuizTemplateEntity> kept = new ArrayList<>();
        for (QuizTemplateEntity quiz : BatchBook.list(knowledge.getQuizTemplates())) {
            if (changes.changes(quiz) && keep.test(quiz)) {
                kept.add(quiz);
            }
        }
        if (kept.isEmpty()) {
            return null;
        }
        return new KnowledgeEntity(knowledge.getId(), knowledge.getCode(), knowledge.getKind(), knowledge.getLabel(),
                knowledge.getOldId(), knowledge.getParentId(), knowledge.isIsRule(), knowledge.getInstructions(),
                knowledge.getOrder(), knowledge.getSlug(), kept, knowledge.getSentenceMode());
    }

    /** The exercises and the topics of a course by id. */
    private static void index(CourseEntity course, Map<String, QuizTemplateEntity> quizzes,
            Map<String, KnowledgeEntity> topics) {
        if (course == null || course.getRoot() == null) {
            return;
        }
        for (MilestoneEntity milestone : BatchBook.list(course.getRoot().getMilestones())) {
            for (TopicEntity topic : BatchBook.list(milestone.getTopics())) {
                for (KnowledgeEntity knowledge : BatchBook.list(topic.getKnowledges())) {
                    topics.put(knowledge.getId(), knowledge);
                    for (QuizTemplateEntity quiz : BatchBook.list(knowledge.getQuizTemplates())) {
                        if (quiz != null) {
                            quizzes.put(quiz.getId(), quiz);
                        }
                    }
                }
            }
        }
    }

    /** The course with each topic replaced by what {@code keep} makes of it, and without what ends up empty. */
    private static CourseEntity cut(CourseEntity course, Function<KnowledgeEntity, KnowledgeEntity> keep) {
        if (course == null || course.getRoot() == null) {
            return null;
        }
        RootNodeEntity root = course.getRoot();
        List<MilestoneEntity> milestones = new ArrayList<>();
        for (MilestoneEntity milestone : BatchBook.list(root.getMilestones())) {
            List<TopicEntity> topics = new ArrayList<>();
            for (TopicEntity topic : BatchBook.list(milestone.getTopics())) {
                List<KnowledgeEntity> knowledges = new ArrayList<>();
                for (KnowledgeEntity knowledge : BatchBook.list(topic.getKnowledges())) {
                    KnowledgeEntity kept = knowledge != null ? keep.apply(knowledge) : null;
                    if (kept != null) {
                        knowledges.add(kept);
                    }
                }
                if (!knowledges.isEmpty()) {
                    topics.add(new TopicEntity(topic.getId(), topic.getCode(), topic.getKind(), topic.getLabel(),
                            topic.getOldId(), topic.getParentId(), topic.getChildren(), topic.getRuleIds(),
                            topic.getOrder(), topic.getSlug(), knowledges));
                }
            }
            if (!topics.isEmpty()) {
                milestones.add(new MilestoneEntity(milestone.getId(), milestone.getCode(), milestone.getKind(),
                        milestone.getLabel(), milestone.getOldId(), milestone.getParentId(), milestone.getChildren(),
                        milestone.getOrder(), milestone.getSlug(), topics));
            }
        }
        if (milestones.isEmpty()) {
            return null;
        }
        return new CourseEntity(course.getId(), course.getTitle(), course.getKnowledgeIds(),
                new RootNodeEntity(root.getId(), root.getCode(), root.getKind(), root.getLabel(), root.getChildren(),
                        milestones), course.getSlug());
    }
}
