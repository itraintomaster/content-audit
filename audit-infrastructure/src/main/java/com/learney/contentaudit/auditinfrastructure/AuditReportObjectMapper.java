package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.auditdomain.AuditableEntity;
import com.learney.contentaudit.auditdomain.AuditableKnowledge;
import com.learney.contentaudit.auditdomain.AuditableMilestone;
import com.learney.contentaudit.auditdomain.AuditableTopic;
import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.AuditNode;
import com.learney.contentaudit.auditdomain.DefaultCourseDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultKnowledgeDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultLevelDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultQuizDiagnoses;
import com.learney.contentaudit.auditdomain.DefaultTopicDiagnoses;
import com.learney.contentaudit.auditdomain.NodeDiagnoses;

/**
 * Package-private factory that produces a configured ObjectMapper for AuditReport persistence.
 *
 * Handles:
 * - Circular parent reference in AuditNode (ignored during serialization, rebuilt after load)
 * - AuditableEntity polymorphism via @type discriminator
 * - NodeDiagnoses polymorphism via @type discriminator
 * - Instant serialization via JavaTimeModule
 */
class AuditReportObjectMapper {

    private AuditReportObjectMapper() {
    }

    static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();

        // Java time support (Instant)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Java 8 Optional support
        mapper.registerModule(new Jdk8Module());

        // Don't fail on unknown properties (forward-compatibility)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Mixins
        mapper.addMixIn(AuditNode.class, AuditNodeMixin.class);
        mapper.addMixIn(AuditableEntity.class, AuditableEntityMixin.class);
        mapper.addMixIn(NodeDiagnoses.class, NodeDiagnosesMixin.class);

        return mapper;
    }

    // -------------------------------------------------------------------------
    // AuditNode mixin: suppress circular parent reference
    // -------------------------------------------------------------------------

    abstract static class AuditNodeMixin {
        @JsonIgnore
        abstract AuditNode getParent();
    }

    // -------------------------------------------------------------------------
    // AuditableEntity mixin: polymorphic type info
    // -------------------------------------------------------------------------

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = AuditableMilestone.class, name = "AuditableMilestone"),
        @JsonSubTypes.Type(value = AuditableTopic.class,     name = "AuditableTopic"),
        @JsonSubTypes.Type(value = AuditableKnowledge.class, name = "AuditableKnowledge"),
        @JsonSubTypes.Type(value = AuditableQuiz.class,      name = "AuditableQuiz")
    })
    abstract static class AuditableEntityMixin {
    }

    // -------------------------------------------------------------------------
    // NodeDiagnoses mixin: polymorphic type info mapped to concrete Default* classes
    // -------------------------------------------------------------------------

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultCourseDiagnoses.class,    name = "CourseDiagnoses"),
        @JsonSubTypes.Type(value = DefaultLevelDiagnoses.class,     name = "LevelDiagnoses"),
        @JsonSubTypes.Type(value = DefaultTopicDiagnoses.class,     name = "TopicDiagnoses"),
        @JsonSubTypes.Type(value = DefaultKnowledgeDiagnoses.class, name = "KnowledgeDiagnoses"),
        @JsonSubTypes.Type(value = DefaultQuizDiagnoses.class,      name = "QuizDiagnoses")
    })
    abstract static class NodeDiagnosesMixin {
    }
}
