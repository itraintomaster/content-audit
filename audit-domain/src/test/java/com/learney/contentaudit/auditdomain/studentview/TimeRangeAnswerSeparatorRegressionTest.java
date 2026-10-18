package com.learney.contentaudit.auditdomain.studentview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learney.contentaudit.auditdomain.AuditableQuiz;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcher;
import com.learney.contentaudit.auditdomain.correctormatch.DefaultCorrectorMatcherTest;
import com.learney.contentaudit.auditdomain.correctormatch.GapContext;
import com.learney.contentaudit.coursedomain.FormEntity;
import com.learney.contentaudit.coursedomain.SentenceMode;
import com.learney.contentaudit.coursedomain.SentencePartEntity;
import com.learney.contentaudit.coursedomain.SentencePartKind;
import com.learney.contentaudit.coursedomain.quizsentenceengine.DefaultQuizSentenceConverter;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** PRB-7ccbcade: full alternatives must survive rendering, serialization and correction. */
@Tag("F-RESPC-R001")
@Tag("F-RONDA1-R001")
class TimeRangeAnswerSeparatorRegressionTest {
    private static final List<String> ANSWERS = List.of(
            "from 7 to 11", "from 7 until 11", "from 7 till 11");
    private static final List<String> SENTENCES = List.of(
            "I slept from 7 to 11.", "I slept from 7 until 11.", "I slept from 7 till 11.");
    private final DefaultQuizSentenceConverter converter = DefaultQuizSentenceConverter.create();
    private final DefaultQuizStudentViewRenderer renderer = new DefaultQuizStudentViewRenderer(converter);
    private final DefaultCorrectorMatcher matcher =
            new DefaultCorrectorMatcher(DefaultCorrectorMatcherTest.productionTable());
    private final GapContext context = new GapContext("I slept ", " (7 → 11).");

    @Test
    void incompletePipeEntryDoesNotDistributeTheSharedPrefixOrSuffix() {
        List<String> malformed = List.of("from 7 to|until|till 11");
        QuizStudentView view = renderer.render(quiz(malformed));

        assertEquals(List.of("from 7 to", "until", "till 11"), view.getGaps().get(0).getAcceptedAnswers());
        assertEquals(List.of("I slept from 7 to.", "I slept until.", "I slept till 11."),
                converter.toPlainSentences(form(malformed), SentenceMode.FILL));
        for (String answer : ANSWERS) {
            assertFalse(matcher.accepts(context, malformed, answer), answer);
        }
    }

    @Test
    void listOfCompleteAlternativesAcceptsAllThreeAnswersRegardlessOfStoredCanonicalSentence() {
        AuditableQuiz proposed = quiz(ANSWERS);
        // The actual after snapshot stores only this sentence. Accepted options remain the source
        // for the gap, so that saved canonical sentence must not erase the other valid answers.
        proposed.setSentences(List.of(SENTENCES.get(0)));
        QuizStudentView view = renderer.render(proposed);

        assertEquals("I slept ___ (7 → 11).", view.getStatement());
        assertEquals("(7 → 11)", view.getGaps().get(0).getHint());
        assertEquals(ANSWERS, view.getGaps().get(0).getAcceptedAnswers());
        assertEquals(ANSWERS.get(0), view.getGaps().get(0).getFirstAnswer());
        assertEquals(SENTENCES, converter.toPlainSentences(form(ANSWERS), SentenceMode.FILL));
        assertEquals(SENTENCES.get(0), renderer.finishedSentence(proposed, SentenceMode.FILL));
        for (String answer : ANSWERS) {
            assertTrue(matcher.accepts(context, ANSWERS, answer), answer);
        }
        for (String fragment : List.of("from 7 to", "until", "till 11", "from 7 to 12")) {
            assertFalse(matcher.accepts(context, ANSWERS, fragment), fragment);
        }
    }

    @Test
    void completePipeEntryAndSeparateEntriesHaveTheSameMeaningThroughTheDslRoundTrip() {
        List<String> piped = List.of(String.join("|", ANSWERS));
        FormEntity roundTripped = converter.parse(converter.serialize(form(piped)));

        assertEquals(converter.serialize(form(ANSWERS)), converter.serialize(form(piped)));
        assertEquals(SENTENCES, converter.toPlainSentences(roundTripped, SentenceMode.FILL));
        assertEquals(ANSWERS, renderer.render(quiz(piped)).getGaps().get(0).getAcceptedAnswers());
        for (String answer : ANSWERS) {
            assertTrue(matcher.accepts(context, piped, answer), answer);
            assertTrue(matcher.accepts(context, roundTripped.getSentenceParts().get(1).getOptions(), answer), answer);
        }
    }

    @Test
    void slashIsLiteralAcrossTheViewDslAndMatcher() {
        String literal = String.join(" / ", ANSWERS);
        List<String> slashed = List.of(literal);
        FormEntity roundTripped = converter.parse(converter.serialize(form(slashed)));

        assertEquals(slashed, renderer.render(quiz(slashed)).getGaps().get(0).getAcceptedAnswers());
        assertEquals(slashed, roundTripped.getSentenceParts().get(1).getOptions());
        assertEquals(List.of("I slept " + literal + "."),
                converter.toPlainSentences(roundTripped, SentenceMode.FILL));
        for (String answer : ANSWERS) {
            assertFalse(matcher.accepts(context, slashed, answer), answer);
        }
        assertTrue(matcher.accepts(context, slashed, literal));
    }

    private static List<SentencePartEntity> parts(List<String> answers) {
        return List.of(new SentencePartEntity(SentencePartKind.TEXT, "I slept ", null),
                new SentencePartEntity(SentencePartKind.CLOZE, "", answers),
                new SentencePartEntity(SentencePartKind.TEXT, " (7 → 11).", null));
    }

    private static FormEntity form(List<String> answers) {
        return new FormEntity("CLOZE", 1.0, "", "", parts(answers), null, null);
    }

    private static AuditableQuiz quiz(List<String> answers) {
        return new AuditableQuiz(List.of(), "67fab6d59930102295342462", "Rangos temporales", "",
                "Dormí de 7 a 11.", List.of(), null,
                "Crea oraciones usando 'from' ... 'to / until / till'.", parts(answers), null,
                "$from$ marca el arranque. $to$, $until$ y $till$ marcan el final.", "A1.09.04.02");
    }
}
