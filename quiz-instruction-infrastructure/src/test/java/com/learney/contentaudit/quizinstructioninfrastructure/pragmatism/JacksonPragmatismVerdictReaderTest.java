package com.learney.contentaudit.quizinstructioninfrastructure.pragmatism;

import com.learney.contentaudit.auditdomain.pragmatism.PragmatismVerdict;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JacksonPragmatismVerdictReaderTest {
    static final String PASS="""
            {"assessments":[{"sentence":"I need a clean towel.","status":"PASS","communicativePurpose":"Pedir una toalla",
            "plausibleUse":"Pedirla después de bañarse","issue":null,"severity":null,"reason":"Petición cotidiana","neededContext":null}],
            "rationale":"Tiene un uso comunicativo claro"}
            """;
    @Test void validOutputHasAnIndependentPragmaticStatus(){
        assertEquals(PragmatismVerdict.Status.PASS,new JacksonPragmatismVerdictReader().read(PASS).assessments().get(0).status());
    }
    @Test void rejectsUnknownKeysCoercionDuplicatesTrailingDataAndIncoherentStates(){
        var reader=new JacksonPragmatismVerdictReader();
        for(String bad:new String[]{PASS.replace("\"status\":\"PASS\"","\"status\":42"),PASS.replace("\"status\":\"PASS\"","\"status\":\"FAIL\""),
                PASS.replace("\"issue\":null","\"issue\":\"WORD_CHOICE\""),PASS.replace("\"plausibleUse\":\"Pedirla después de bañarse\"","\"plausibleUse\":null"),
                PASS.replace("\"neededContext\":null","\"neededContext\":\"Inventar un cuento\""),PASS.replace("\"status\":\"PASS\"","\"status\":\"PASS\",\"status\":\"FAIL\""),
                PASS.replace("\"rationale\":","\"foreign\":true,\"rationale\":"),PASS+" {}"}){
            assertThrows(IllegalArgumentException.class,()->reader.read(bad),bad);
        }
    }
}
