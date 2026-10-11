package com.learney.contentaudit.revisiondomain.batchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditTarget;
import com.learney.contentaudit.auditdomain.correction.ExercisePart;
import com.learney.contentaudit.coursedomain.KnowledgeEntity;
import com.learney.contentaudit.coursedomain.QuizTemplateEntity;
import com.learney.contentaudit.revisiondomain.CourseElementSnapshot;
import com.learney.contentaudit.revisiondomain.batch.BatchChange;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeOrigin;
import com.learney.contentaudit.revisiondomain.batch.BatchChangeStep;
import com.learney.contentaudit.revisiondomain.batch.CorrectionBatch;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * F-TANDA-R009 y F-PUBPROD-R001: lo que la preservación y la lista leen de los pasos que quedan en la tanda.
 */
public class PublishedScopeTest {

    private static BatchChange change(String id, String dropped, BatchChangeStep... steps) {
        return new BatchChange(id, BatchChangeOrigin.RULE, "rule-" + id, "k1", List.of(), List.of(steps), null,
                false, Instant.parse("2026-10-11T10:00:00Z"), dropped);
    }

    private static BatchChangeStep quizStep(String quizId, ExercisePart... parts) {
        return new BatchChangeStep(quizId, List.of(parts),
                new CourseElementSnapshot(AuditTarget.QUIZ, quizId, new QuizTemplateEntity(), null));
    }

    @Test
    @DisplayName("should read the parts of each active change on the exercise, adding the stored sentence and what is heard to a step that touches what the exercise says, and leave out dropped changes")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R009")
    public void shouldReadThePartsOfEachActiveChangeOnTheExercise() {
        CorrectionBatch batch = new CorrectionBatch();
        batch.setChanges(List.of(
                change("c1", null, quizStep("q1", ExercisePart.TRANSLATION), quizStep("q2", ExercisePart.STATEMENT)),
                change("c2", null, quizStep("q1", ExercisePart.ANSWERS)),
                change("c3", "José lo sacó", quizStep("q1", ExercisePart.MINI_THEORY))));

        PublishedScope scope = PublishedScope.of(batch, "q1");

        assertEquals(AuditTarget.QUIZ, scope.target());
        Map<String, Set<ExercisePart>> byChange = scope.partsByChange();
        assertEquals(List.of("c1", "c2"), List.copyOf(byChange.keySet()));
        assertEquals(EnumSet.of(ExercisePart.TRANSLATION), byChange.get("c1"));
        assertEquals(EnumSet.of(ExercisePart.ANSWERS, ExercisePart.STORED_SENTENCE, ExercisePart.AUDIO),
                byChange.get("c2"));
        assertEquals(EnumSet.of(ExercisePart.TRANSLATION, ExercisePart.ANSWERS, ExercisePart.STORED_SENTENCE,
                ExercisePart.AUDIO), scope.parts());
        assertTrue(PublishedScope.of(batch, "q9").parts().isEmpty());
    }

    @Test
    @DisplayName("should read a step whose after is a topic as a topic, and take its snapshot from the topic of the course")
    @Tag("FEAT-PUBPROD")
    @Tag("F-PUBPROD-R001")
    @Tag("FEAT-TANDA")
    @Tag("F-TANDA-R009")
    public void shouldReadAStepWhoseAfterIsATopicAsATopic() {
        KnowledgeEntity topic = new KnowledgeEntity();
        topic.setId("k1");
        topic.setInstructions("Antes");
        CorrectionBatch batch = new CorrectionBatch();
        batch.setChanges(List.of(change("c1", null, new BatchChangeStep("k1", List.of(ExercisePart.TOPIC_INSTRUCTIONS),
                new CourseElementSnapshot(AuditTarget.KNOWLEDGE, "k1", null, topic)))));

        PublishedScope scope = PublishedScope.of(batch, "k1");

        assertEquals(AuditTarget.KNOWLEDGE, scope.target());
        assertEquals(EnumSet.of(ExercisePart.TOPIC_INSTRUCTIONS), scope.parts());
        assertTrue(scope.snapshot(null).isEmpty());
    }
}
