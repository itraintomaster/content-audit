package com.learney.contentaudit.quizinstructioninfrastructure.choicequality;

import com.learney.contentaudit.auditdomain.choicequality.ChoiceQualityVerdict;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JacksonChoiceQualityVerdictReaderTest {
    private final JacksonChoiceQualityVerdictReader reader = new JacksonChoiceQualityVerdictReader();
    private static final String PASS = """
        {"status":"PASS","objective":"Inversión","introductory":true,"issues":[],"rationale":"Práctica introductoria legítima"}
        """;
    private static final String ISSUE = """
        {"kind":"ANSWER_CUE","severity":"HIGH","labels":["Is he"],"reason":"Sólo hay una opción con he","misconception":"Invertir be"}
        """;
    private static String fail(String issue) { return PASS.replace("\"PASS\"", "\"FAIL\"").replace("[]", "[" + issue + "]"); }

    @Test void readsPassFailAndUncertaintyWithoutConfusingTheirMeaning() {
        assertTrue(reader.read(PASS).introductory());
        assertEquals(ChoiceQualityVerdict.Kind.ANSWER_CUE, reader.read(fail(ISSUE)).issues().get(0).kind());
        assertEquals(ChoiceQualityVerdict.Status.NEEDS_CONTEXT,
                reader.read(PASS.replace("\"PASS\"", "\"NEEDS_CONTEXT\"").replace("\"Inversión\"", "\"\"")).status());
    }

    @Test void rejectsMalformedIncompleteCoercedAndContradictoryOutput() {
        for (String invalid : List.of("{}", "null", "{broken", PASS.replace("true", "\"true\""),
                PASS.replace("\"Inversión\"", "123"), PASS.replace("\"PASS\"", "\"FAIL\""),
                PASS.replace("\"PASS\"", "\"OTHER\""), PASS.replace("\"Inversión\"", "\"\""),
                PASS.replace("\"issues\":[]", "\"issues\":null"), PASS.replace("\"introductory\":true,", ""),
                PASS.replace("\"status\":", "\"extra\":0,\"status\":"),
                fail(ISSUE).replace("\"Is he\"", "12"), fail(ISSUE).replace("\"HIGH\"", "\"BLOCKING\""),
                fail(ISSUE).replace("\"ANSWER_CUE\"", "\"unknown\""),
                fail(ISSUE).replace("\"reason\":", "\"extra\":false,\"reason\":"),
                fail(ISSUE).replace("\"Invertir be\"", "false"),
                fail(ISSUE + "," + ISSUE), fail(ISSUE).replace("[\"Is he\"]", "[\"Is he\",\"Is he\"]"))) {
            assertThrows(IllegalArgumentException.class, () -> reader.read(invalid), invalid);
        }
    }
}
