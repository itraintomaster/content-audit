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

    @Test void aGapInsideTheSentenceStillStoresTheWholeCompletedSentence() {
        assertEquals(List.of("She likes apples."),
                StoredSentenceRehydrator.rebuild(quiz(text("She"), gap("likes"), text("(like) apples."))));
        assertEquals(List.of("Is he a child?"), StoredSentenceRehydrator.rebuild(quiz(gap("Is he"), text("a child? (He)"))));
        assertEquals(List.of("Look! It is raining."),
                StoredSentenceRehydrator.rebuild(quiz(text("Look! It"), gap("is raining"), text("(rain)."))));
    }
}
