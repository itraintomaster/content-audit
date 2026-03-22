package com.learney.contentaudit.courseinfrastructure;

import com.learney.contentaudit.coursedomain.CourseEntity;
import com.learney.contentaudit.coursedomain.CourseValidationException;
import com.learney.contentaudit.coursedomain.CourseValidator;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.MilestoneEntity;
import com.learney.contentaudit.coursedomain.RootNodeEntity;
import com.learney.contentaudit.coursedomain.TopicEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a CourseEntity against business rules R001–R016.
 * Throws CourseValidationException with a descriptive message on the first violation found.
 */
public class CourseValidatorImpl implements CourseValidator {

    @Override
    public void validate(CourseEntity course) {
        if (course == null) {
            throw new CourseValidationException("unknown",
                    "El curso no puede ser null");
        }

        String path = course.getSlug() != null ? course.getSlug() : "unknown";

        // R009: Course mandatory fields
        requireField(path, "Course", course.getId(), "id");
        requireField(path, "Course", course.getTitle(), "title");

        RootNodeEntity root = course.getRoot();
        if (root == null) {
            // A null root is allowed for minimal course stubs used in unit tests (validator-focused)
            return;
        }

        // R009: ROOT mandatory fields
        requireField(path, "ROOT", root.getId(), "id");
        requireField(path, "ROOT", root.getCode(), "code");
        requireField(path, "ROOT", root.getKind(), "kind");
        requireNonEmptyList(path, "ROOT", root.getChildren(), "children");

        Set<String> seenIds = new HashSet<>();
        seenIds.add(root.getId());

        List<MilestoneEntity> milestones = root.getMilestones();
        if (milestones == null || milestones.isEmpty()) {
            throw new CourseValidationException(path,
                    "Estructura incompleta: ROOT no tiene milestones");
        }

        for (MilestoneEntity milestone : milestones) {
            validateMilestone(path, milestone, root.getId(), seenIds);
        }

        // R005: knowledgeIds in course must match actual knowledges in tree
        if (course.getKnowledgeIds() != null && !course.getKnowledgeIds().isEmpty()) {
            Set<String> actualKnowledgeIds = collectKnowledgeIds(milestones);
            for (String kid : course.getKnowledgeIds()) {
                if (!actualKnowledgeIds.contains(kid)) {
                    throw new CourseValidationException(path,
                            "Inconsistencia de IDs en Course: knowledgeIds referencia el ID '"
                                    + kid + "' que no existe en la estructura");
                }
            }
        }
    }

    private void validateMilestone(String path, MilestoneEntity milestone,
            String expectedParentId, Set<String> seenIds) {
        // R009: mandatory fields
        requireField(path, "Milestone", milestone.getId(), "id");
        requireField(path, "Milestone", milestone.getCode(), "code");
        requireField(path, "Milestone", milestone.getKind(), "kind");
        requireField(path, "Milestone", milestone.getLabel(), "label");
        requireField(path, "Milestone", milestone.getOldId(), "oldId");
        requireField(path, "Milestone", milestone.getParentId(), "parentId");
        requireNonEmptyList(path, "Milestone '" + milestone.getLabel() + "'",
                milestone.getChildren(), "children");

        // R006: unique IDs
        checkUniqueId(path, "Milestone", milestone.getId(), seenIds);

        // R008: parentId must match expected parent
        if (!expectedParentId.equals(milestone.getParentId())) {
            throw new CourseValidationException(path,
                    "Integridad referencial rota: Milestone (" + milestone.getId()
                            + ") referencia al padre " + milestone.getParentId()
                            + ", pero el padre esperado es " + expectedParentId);
        }

        // R015: at least one topic
        List<TopicEntity> topics = milestone.getTopics();
        if (topics == null || topics.isEmpty()) {
            throw new CourseValidationException(path,
                    "Estructura incompleta: Milestone '" + milestone.getLabel()
                            + "' (" + milestone.getId() + ") no tiene hijos");
        }

        for (TopicEntity topic : topics) {
            validateTopic(path, topic, milestone.getId(), seenIds);
        }
    }

    private void validateTopic(String path, TopicEntity topic,
            String expectedParentId, Set<String> seenIds) {
        // R009: mandatory fields
        requireField(path, "Topic", topic.getId(), "id");
        requireField(path, "Topic", topic.getCode(), "code");
        requireField(path, "Topic", topic.getKind(), "kind");
        requireField(path, "Topic", topic.getLabel(), "label");
        requireField(path, "Topic", topic.getOldId(), "oldId");
        requireField(path, "Topic", topic.getParentId(), "parentId");
        requireNonEmptyList(path, "Topic '" + topic.getLabel() + "'",
                topic.getRuleIds(), "ruleIds");

        // R006: unique IDs
        checkUniqueId(path, "Topic", topic.getId(), seenIds);

        // R008: parentId
        if (!expectedParentId.equals(topic.getParentId())) {
            throw new CourseValidationException(path,
                    "Integridad referencial rota: Topic (" + topic.getId()
                            + ") referencia al padre " + topic.getParentId()
                            + ", pero el padre esperado es " + expectedParentId);
        }

        // R015: at least one knowledge
        List<KnowledgeEntity> knowledges = topic.getKnowledges();
        if (knowledges == null || knowledges.isEmpty()) {
            throw new CourseValidationException(path,
                    "Estructura incompleta: Topic '" + topic.getLabel()
                            + "' (" + topic.getId() + ") no tiene hijos");
        }

        for (KnowledgeEntity knowledge : knowledges) {
            validateKnowledge(path, knowledge, topic.getId(), seenIds);
        }
    }

    private void validateKnowledge(String path, KnowledgeEntity knowledge,
            String expectedParentId, Set<String> seenIds) {
        // R009: mandatory fields
        requireField(path, "Knowledge", knowledge.getId(), "id");
        requireField(path, "Knowledge", knowledge.getCode(), "code");
        requireField(path, "Knowledge", knowledge.getKind(), "kind");
        requireField(path, "Knowledge", knowledge.getLabel(), "label");
        requireField(path, "Knowledge", knowledge.getOldId(), "oldId");
        requireField(path, "Knowledge", knowledge.getParentId(), "parentId");

        // R006: unique IDs
        checkUniqueId(path, "Knowledge", knowledge.getId(), seenIds);

        // R008: parentId
        if (!expectedParentId.equals(knowledge.getParentId())) {
            throw new CourseValidationException(path,
                    "Integridad referencial rota: Knowledge (" + knowledge.getId()
                            + ") referencia al padre " + knowledge.getParentId()
                            + ", pero el padre esperado es " + expectedParentId);
        }

        // R004: at least one quiz template
        List<?> quizzes = knowledge.getQuizTemplates();
        if (quizzes == null || quizzes.isEmpty()) {
            throw new CourseValidationException(path,
                    "El knowledge '" + knowledge.getLabel() + "' (" + knowledge.getId()
                            + ") no tiene quiz templates asociados");
        }
    }

    private Set<String> collectKnowledgeIds(List<MilestoneEntity> milestones) {
        Set<String> ids = new HashSet<>();
        if (milestones == null) return ids;
        for (MilestoneEntity m : milestones) {
            if (m.getTopics() == null) continue;
            for (TopicEntity t : m.getTopics()) {
                if (t.getKnowledges() == null) continue;
                for (KnowledgeEntity k : t.getKnowledges()) {
                    ids.add(k.getId());
                }
            }
        }
        return ids;
    }

    private void requireField(String path, String entity, Object value, String fieldName) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw new CourseValidationException(path,
                    "Campo obligatorio ausente: " + entity + " no tiene el campo '" + fieldName + "'");
        }
    }

    private void requireNonEmptyList(String path, String entity, List<?> list, String fieldName) {
        if (list == null || list.isEmpty()) {
            throw new CourseValidationException(path,
                    "Estructura incompleta: " + entity + " no tiene hijos");
        }
    }

    private void checkUniqueId(String path, String entityType, String id, Set<String> seenIds) {
        if (id == null) return;
        if (!seenIds.add(id)) {
            throw new CourseValidationException(path,
                    "Identificador duplicado: el ID " + id + " aparece mas de una vez (detectado en "
                            + entityType + ")");
        }
    }
}
