package com.learney.contentaudit.revisiondomain;

import static org.junit.jupiter.api.Assertions.*;
import com.learney.contentaudit.coursedomain.*;
import java.util.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("F-SMODE-R004")
class StoredSentenceRehydratorTest {
    static QuizTemplateEntity quiz(SentencePartEntity... parts) {
        var q = new QuizTemplateEntity(); q.setId("q"); q.setKind("CLOZE");
        q.setForm(new FormEntity("CLOZE", 1, "label", "name", List.of(parts), null, Map.of()));
        return q;
    }
    static SentencePartEntity text(String text) { return new SentencePartEntity(SentencePartKind.TEXT, text, null); }
    static SentencePartEntity gap(String... answers) { return new SentencePartEntity(SentencePartKind.CLOZE, "", List.of(answers)); }

    @Test void aSentenceToTransformIsStoredAsTheAnswerAloneAsTheCourseKeepsIt() {
        assertEquals(List.of("Could you help me?"),
                StoredSentenceRehydrator.rebuild(quiz(text("Polite request: I want you to help me."), gap("Could you help me?"))));
        assertEquals(List.of("That is Mary's house."), StoredSentenceRehydrator.rebuild(
                quiz(text("That house belongs to Mary. "), gap("That is Mary's house.|That's Mary's house."))));
        assertEquals(List.of("I slept badly today."),
                StoredSentenceRehydrator.rebuild(quiz(text("I slept today. (bad)"), gap("I slept badly today."))));
    }

    @Test
    @Tag("F-DATOS-R001")
    void aTransformationThatKeptItsSentenceToTransformGluedGetsItsAnswerAloneAndNothingElseDoes() {
        // F-DATOS-R001 (José, 25/10): «Mary is at home. Mary is always at home.» (…257c) gets its answer alone; the reply
        // of a dialog and a word to choose keep their context, and an answer already alone has nothing to change.
        var mary = quiz(text("Mary is at home. (always)"), gap("Mary is always at home."));
        mary.setSentences(List.of("Mary is at home. Mary is always at home."));
        assertEquals(Optional.of(List.of("Mary is always at home.")), StoredSentenceRehydrator.answerAloneInsteadOf(mary));
        mary.setSentences(List.of("Mary is always at home."));
        assertEquals(Optional.empty(), StoredSentenceRehydrator.answerAloneInsteadOf(mary));
        var neither = quiz(text("A: He hasn't done anything. (I) B:"), gap("Neither have I."));
        neither.setSentences(List.of("A: He hasn't done anything. B: Neither have I."));
        assertEquals(Optional.empty(), StoredSentenceRehydrator.answerAloneInsteadOf(neither));
        var nobody = quiz(text("Everybody / Nobody enjoys paying for shaves. We all dislike it!"), gap("Nobody"));
        nobody.setSentences(List.of("Everybody / Nobody enjoys paying for shaves. We all dislike it! Nobody"));
        assertEquals(Optional.empty(), StoredSentenceRehydrator.answerAloneInsteadOf(nobody));
    }

    @Test void aGapInsideTheSentenceStillStoresTheWholeCompletedSentence() {
        assertEquals(List.of("She likes apples."),
                StoredSentenceRehydrator.rebuild(quiz(text("She"), gap("likes"), text("(like) apples."))));
        assertEquals(List.of("Is he a child?"), StoredSentenceRehydrator.rebuild(quiz(gap("Is he"), text("a child? (He)"))));
        assertEquals(List.of("Look! It is raining."),
                StoredSentenceRehydrator.rebuild(quiz(text("Look! It"), gap("is raining"), text("(rain)."))));
    }
}
