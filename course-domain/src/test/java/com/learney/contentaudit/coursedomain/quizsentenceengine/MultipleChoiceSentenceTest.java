package com.learney.contentaudit.coursedomain.quizsentenceengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceConverter;
import com.learney.contentaudit.coursedomain.quizsentence.QuizSentenceSerializationException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * FEAT-QSENT guards that multiple choice must not move: a CLOZE form serializes and fails as it
 * always did. The multiple-choice cases themselves are traced to FEAT-OPMUL in
 * {@link DefaultQuizSentenceConverterTest}.
 */
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
}
