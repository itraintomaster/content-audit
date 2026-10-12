package com.learney.contentaudit.revisiondomain.batchengine;

import com.learney.contentaudit.coursedomain.*;
import com.learney.contentaudit.revisiondomain.StoredSentenceRehydrator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.Tag("F-CONVERSION-R001")
@org.junit.jupiter.api.Tag("F-CONVERSION-R003")
class ExistingChoiceConversionTest {
    static QuizTemplateEntity quiz(String hint, String answer) {
        var q = new QuizTemplateEntity(); q.setId("q"); q.setKind("CLOZE"); q.setKnowledgeId("k");
        q.setInstructions("Escribe la palabra correcta."); q.setMiniTheory("theory"); q.setAnswerAudioUrl("audio");
        q.setUnmodeledFields(Map.of("opaque", Map.of("x", 4)));
        q.setForm(new FormEntity("CLOZE", 1, "label", "name", List.of(
                new SentencePartEntity(SentencePartKind.TEXT, "Is that pen", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answer)),
                new SentencePartEntity(SentencePartKind.TEXT, hint + "?", null)), null, Map.of("unknown", "preserved")));
        q.setSentences(StoredSentenceRehydrator.rebuild(q)); return q;
    }
    static BatchBook.Place place(QuizTemplateEntity q, List<QuizTemplateEntity> siblings) {
        var k = new KnowledgeEntity(); k.setId("k"); k.setLabel("Pronombres");
        var quizzes = new ArrayList<>(siblings); quizzes.add(q); k.setQuizTemplates(quizzes);
        var m = new MilestoneEntity(); m.setLabel("A1"); m.setCode("A1");
        return new BatchBook.Place(m, new TopicEntity(), k, q);
    }
    @Test void copiesTwoLiteralOptionsInOrderAndPreservesTheWholeQuizAndSpokenSentence() {
        var original = quiz("(your / yours)", "yours"); var before = BatchBook.copy(original);
        var c = ExistingChoiceConversion.inspect(place(original, List.of()), false);
        assertTrue(c.eligible(), c.reason()); assertEquals("EXERCISE", c.source());
        assertEquals(List.of("your", "yours"), c.options());
        assertEquals(before, original); assertEquals(before, c.after().getClozeOriginal());
        assertEquals(before.getSentences(), c.after().getSentences());
        assertEquals(before.getSentences(), StoredSentenceRehydrator.rebuild(c.after()));
        assertEquals("Elige la palabra correcta.", c.after().getInstructions());
        assertEquals("?", c.after().getForm().getSentenceParts().get(2).getText());
        assertEquals("audio", c.after().getAnswerAudioUrl()); assertEquals(before.getUnmodeledFields(), c.after().getUnmodeledFields());
        var applied = BatchBook.apply(original, c.after(), BatchBook.fields(List.of(
                com.learney.contentaudit.auditdomain.correction.ExercisePart.FORM_KIND,
                com.learney.contentaudit.auditdomain.correction.ExercisePart.STATEMENT,
                com.learney.contentaudit.auditdomain.correction.ExercisePart.INSTRUCTIONS)));
        assertEquals(c.after(), applied);
    }
    @Test void rejectsRepeatedOptionsSeveralGapsWholeSentenceAndMismatchingStoredSentence() {
        assertFalse(ExistingChoiceConversion.inspect(place(quiz("(his / his)", "his"), List.of()), false).eligible());
        var q = quiz("(your / yours)", "yours"); var parts = new ArrayList<>(q.getForm().getSentenceParts());
        parts.add(new SentencePartEntity(SentencePartKind.CLOZE, "", List.of("x"))); q.getForm().setSentenceParts(parts);
        assertFalse(ExistingChoiceConversion.inspect(place(q, List.of()), false).eligible());
        assertFalse(ExistingChoiceConversion.inspect(place(quiz("", "This is a sentence."), List.of()), false).eligible());
        q = quiz("(your / yours)", "yours"); q.setSentences(List.of("different"));
        assertFalse(ExistingChoiceConversion.inspect(place(q, List.of()), false).eligible());
    }
    @Test void copiesOnlyAnUnambiguousSiblingSetAndDoesNotAutomaticallyConvertBaseHints() {
        var sibling = ExistingChoiceConversion.inspect(place(quiz("(your / yours)", "yours"), List.of()), false).after();
        var q = quiz("", "yours");
        assertEquals("SIBLINGS", ExistingChoiceConversion.inspect(place(q, List.of(sibling)), false).source());
        q = quiz("(yours)", "yours");
        assertFalse(ExistingChoiceConversion.inspect(place(q, List.of(sibling)), false).eligible());
        assertTrue(ExistingChoiceConversion.inspect(place(q, List.of(sibling)), true).eligible());
        var another = BatchBook.copy(sibling); another.getForm().setMultipleChoice(new MultipleChoiceEntity("SINGLE", List.of(
                new MultipleChoiceItemEntity("yours", 1, "yours"), new MultipleChoiceItemEntity("their", 0, "their"))));
        assertFalse(ExistingChoiceConversion.inspect(place(quiz("", "yours"), List.of(sibling, another)), false).eligible());
    }
    @Test void keepsAnExistingChooseInstructionAndNeverInventsOptions() {
        var q = quiz("(your / yours)", "yours"); q.setInstructions("Selecciona el pronombre correcto.");
        assertEquals(q.getInstructions(), ExistingChoiceConversion.inspect(place(q, List.of()), false).after().getInstructions());
        assertTrue(ExistingChoiceConversion.inspect(place(quiz("", "yours"), List.of()), true).reason().contains("examen"));
    }
}
