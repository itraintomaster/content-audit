package com.learney.contentaudit.auditdomain.nearduplicates;

import com.learney.contentaudit.auditdomain.*;
import com.learney.contentaudit.auditdomain.analysis.DefaultAnalysisComposer;
import com.learney.contentaudit.auditdomain.studentview.DefaultQuizStudentViewRenderer;
import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NearDuplicatesSubjectBuilderTest {
    final NearDuplicatesSubjectBuilder builder = new NearDuplicatesSubjectBuilder(
            new DefaultQuizStudentViewRenderer(DefaultQuizSentenceConverter.create()));
    AuditNode quiz(String id, String sentence, AuditNode topic) {
        var q = new AuditableQuiz(); q.setId(id); q.setLabel("Topic"); q.setQuizSentence(sentence);
        q.setSentences(List.of(sentence)); q.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT, sentence, null)));
        return new AuditNode(q, AuditTarget.QUIZ, topic, new ArrayList<>(), new HashMap<>(), new HashMap<>(), new DefaultQuizDiagnoses());
    }
    AuditNode topic() {
        var k = new AuditableKnowledge(); k.setId("topic");
        var t = new AuditNode(k, AuditTarget.KNOWLEDGE, null, new ArrayList<>(), new HashMap<>(), new HashMap<>(), null);
        t.getChildren().add(quiz("first", "We are friends.", t));
        t.getChildren().add(quiz("copy", "They are friends.", t)); return t;
    }
    @Test void changingAnotherQuizOrItsOrderExpiresTheTopicsPairMeasurement() {
        var t = topic(); var copy = t.getChildren().get(1);
        var original = builder.build(copy).getContent();
        assertEquals(original, builder.build(topic().getChildren().get(1)).getContent());
        var first = (AuditableQuiz)t.getChildren().get(0).getEntity();
        first.setTranslation("Otra traducción");
        assertEquals(original, builder.build(copy).getContent());
        first.setQuizSentence("We are colleagues.");
        first.setSentences(List.of("We are colleagues."));
        first.setSentenceParts(List.of(new SentencePartEntity(SentencePartKind.TEXT, "We are colleagues.", null)));
        assertNotEquals(original, builder.build(copy).getContent());
        t = topic(); copy = t.getChildren().get(1); Collections.reverse(t.getChildren());
        assertNotEquals(original, builder.build(copy).getContent());
    }
    @Test void applicabilitySeparatesWriteInJudgesFromMultipleChoice() {
        var t = topic(); var node = t.getChildren().get(0); var q = (AuditableQuiz)node.getEntity();
        for (String name : List.of("hint-coherence", "answer-completeness", "single-answer")) assertTrue(DefaultAnalysisComposer.eligible(node,name));
        q.setMultipleChoice(new MultipleChoiceEntity());
        for (String name : List.of("hint-coherence", "answer-completeness", "single-answer")) assertFalse(DefaultAnalysisComposer.eligible(node,name));
        assertTrue(DefaultAnalysisComposer.eligible(node,"multiple-choice-quality"));
        assertTrue(DefaultAnalysisComposer.eligible(node,"near-duplicates"));
    }
}
