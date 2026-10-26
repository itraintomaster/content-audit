package com.learney.contentaudit.quizinstructioninfrastructure.answergiveaway;

import com.learney.contentaudit.auditdomain.answergiveaway.AnswerGiveawayVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JacksonAnswerGiveawayVerdictReaderTest {
    static final String GIVES = """
            {"quizId":"q","status":"GIVES_AWAY","practicedDecision":"Elegir will o going to","where":"INSTRUCTIONS",
            "givenAway":"acabas de decidirlo","extent":"WHOLE","reason":"Le dice qué caso aplica",
            "proposedAction":"Poné el contexto en la oración","handoffs":["HINT"]}
            """;

    @Test
    @DisplayName("should read a closed verdict with where, the exact fragment and how much it gives away")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    void shouldReadAClosedVerdictWithWhereTheFragmentAndHowMuchItGivesAway() {
        AnswerGiveawayVerdict verdict = new JacksonAnswerGiveawayVerdictReader().read(GIVES);

        assertEquals(AnswerGiveawayVerdict.Status.GIVES_AWAY, verdict.status());
        assertEquals(AnswerGiveawayVerdict.Where.INSTRUCTIONS, verdict.where());
        assertEquals("acabas de decidirlo", verdict.givenAway());
        assertEquals(AnswerGiveawayVerdict.Extent.WHOLE, verdict.extent());
        assertEquals(java.util.List.of(AnswerGiveawayVerdict.Handoff.HINT), verdict.handoffs());
    }

    @Test
    @DisplayName("should reject unknown keys, wrong types, repeated keys, trailing data and a state that does not match its fields")
    @Tag("FEAT-REGALA")
    @Tag("F-REGALA-R002")
    void shouldRejectUnknownKeysWrongTypesRepeatedKeysTrailingDataAndIncoherentStates() {
        var reader = new JacksonAnswerGiveawayVerdictReader();
        for (String bad : new String[] {
                GIVES.replace("\"status\":\"GIVES_AWAY\"", "\"status\":42"),
                GIVES.replace("\"status\":\"GIVES_AWAY\"", "\"status\":\"OK\""),
                GIVES.replace("\"givenAway\":\"acabas de decidirlo\"", "\"givenAway\":null"),
                GIVES.replace("\"extent\":\"WHOLE\"", "\"extent\":\"ALL\""),
                GIVES.replace("\"where\":\"INSTRUCTIONS\"", "\"where\":\"HINT\""),
                GIVES.replace("\"handoffs\":[\"HINT\"]", "\"handoffs\":[\"HINT\",\"HINT\"]"),
                GIVES.replace("\"status\":\"GIVES_AWAY\"", "\"status\":\"GIVES_AWAY\",\"status\":\"OK\""),
                GIVES.replace("\"reason\":", "\"foreign\":true,\"reason\":"),
                GIVES.replace("\"proposedAction\":\"Poné el contexto en la oración\",", ""),
                GIVES + " {}"}) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(bad), bad);
        }
    }
}
