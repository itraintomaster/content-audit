package com.learney.contentaudit.coursedomain.quizsentenceengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceEntity;
import com.learney.contentaudit.coursedomain.MultipleChoiceItemEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceSerializationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("multiple-choice")
class MultipleChoiceSentenceTest {

    private final QuizSentenceConverter converter = DefaultQuizSentenceConverter.create();

    private static SentencePartEntity text(String t) {
        return new SentencePartEntity(SentencePartKind.TEXT, t, null);
    }

    private static SentencePartEntity gap() {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", null);
    }

    private static SentencePartEntity cloze(String... options) {
        return new SentencePartEntity(SentencePartKind.CLOZE, "", Arrays.asList(options));
    }

    private static FormEntity multipleChoice(String correct, List<String> wrong, SentencePartEntity... parts) {
        List<MultipleChoiceItemEntity> items = new ArrayList<>();
        for (String label : wrong) {
            items.add(new MultipleChoiceItemEntity(label, 0.0, label));
        }
        items.add(new MultipleChoiceItemEntity(correct, 1.0, correct));
        FormEntity form = new FormEntity("MULTIPLE_CHOICE", 1.0, "", "", Arrays.asList(parts), null, null);
        form.setMultipleChoice(new MultipleChoiceEntity("SINGLE", items));
        return form;
    }

    @Test
    @DisplayName("A multiple-choice form has no quizSentence DSL: serialize returns null instead of failing on the optionless gap")
    void serializeReturnsNullForMultipleChoice() {
        FormEntity form = multipleChoice("is", List.of("am", "are"), text("She"), gap(), text("English."));
        assertNull(converter.serialize(form));
    }

    @Test
    @DisplayName("A CLOZE form still serializes exactly as before")
    void serializeClozeUnchanged() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", List.of(text("She"), cloze("is"), text("English.")), null, null);
        assertEquals("She ____ [is] English.", converter.serialize(form));
    }

    @Test
    @DisplayName("A CLOZE gap without options still fails fast (R004): only multiple choice is exempt")
    void clozeWithoutOptionsStillFails() {
        FormEntity form = new FormEntity("CLOZE", 1.0, "", "", List.of(text("She"), gap(), text("English.")), null, null);
        assertThrows(QuizSentenceSerializationException.class, () -> converter.serialize(form));
        assertThrows(QuizSentenceSerializationException.class, () -> converter.toPlainSentences(form));
    }

    @Test
    @DisplayName("The measured sentence of a multiple-choice form is the stem with the correct option in the gap, as-is")
    void plainSentenceIsStemWithCorrectOption() {
        FormEntity form = multipleChoice("is he", List.of("are he"), gap(), text("a doctor?"));
        assertEquals(List.of("is he a doctor?"), converter.toPlainSentences(form, SentenceMode.FILL));
        assertEquals(List.of("is he a doctor?"), converter.toPlainSentences(form));
    }

    @Test
    @DisplayName("It matches the sentence of the CLOZE whose only accepted answer is the correct option")
    void plainSentenceMatchesEquivalentCloze() {
        FormEntity mc = multipleChoice("loudly", List.of("loud"), text("She sang"), gap(), text("(loud / loudly)."));
        FormEntity cloze = new FormEntity("CLOZE", 1.0, "", "",
                List.of(text("She sang"), cloze("loudly"), text("(loud / loudly).")), null, null);
        assertEquals(converter.toPlainSentences(cloze, SentenceMode.FILL),
                converter.toPlainSentences(mc, SentenceMode.FILL));
        assertEquals(List.of("She sang loudly."), converter.toPlainSentences(mc, SentenceMode.FILL));
    }

    @Test
    @DisplayName("A multiple-choice form ignores REWRITE: the sentence before the gap stays in the measured sentence")
    void multipleChoiceIgnoresRewriteMode() {
        FormEntity form = multipleChoice("So", List.of("Although"), text("It was raining."), gap(), text("we stayed home."));
        assertEquals(List.of("It was raining. So we stayed home."),
                converter.toPlainSentences(form, SentenceMode.REWRITE));
    }

    @Test
    @DisplayName("A pipe inside the correct option's label is literal: one sentence, never alternatives")
    void labelWithPipeIsLiteral() {
        FormEntity form = multipleChoice("a|b", List.of("c"), text("Pick"), gap(), text("now."));
        assertEquals(List.of("Pick a|b now."), converter.toPlainSentences(form, SentenceMode.FILL));
    }

    @Test
    @DisplayName("A multiple-choice form without a correct option cannot produce a sentence and fails fast")
    void noCorrectOptionFails() {
        FormEntity form = multipleChoice("is", List.of("am"), text("She"), gap(), text("English."));
        form.getMultipleChoice().getItems().forEach(item -> item.setIncidence(0.0));
        assertThrows(QuizSentenceSerializationException.class, () -> converter.toPlainSentences(form));
    }

    @Test
    @DisplayName("A multiple-choice form with more than one gap is rejected: the correct option belongs to a single gap")
    void twoGapsFail() {
        FormEntity form = multipleChoice("is", List.of("am"), text("She"), gap(), text("and he"), gap(), text("."));
        assertThrows(QuizSentenceSerializationException.class, () -> converter.toPlainSentences(form));
    }

    @Test
    @DisplayName("parseOnto keeps what the DSL does not encode: the multiple-choice payload and the unmodeled fields of the base")
    void parseOntoKeepsNewFields() {
        FormEntity base = new FormEntity("CLOZE", 1.0, "", "", List.of(text("She"), cloze("is"), text("English.")), null, null);
        MultipleChoiceEntity payload = new MultipleChoiceEntity("SINGLE",
                List.of(new MultipleChoiceItemEntity("is", 1.0, "is")));
        base.setMultipleChoice(payload);
        base.setUnmodeledFields(Map.of("futureKey", "kept"));

        FormEntity result = converter.parseOnto("She ____ [was] English.", base);

        assertSame(payload, result.getMultipleChoice());
        assertEquals(Map.of("futureKey", "kept"), result.getUnmodeledFields());
        assertEquals(List.of("was"), result.getSentenceParts().get(1).getOptions());
    }
}
