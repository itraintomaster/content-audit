package com.learney.contentaudit.quizinstructioninfrastructure.englishquality;

import com.learney.contentaudit.auditdomain.englishquality.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JacksonEnglishQualityVerdictReaderTest {
    final JacksonEnglishQualityVerdictReader reader=new JacksonEnglishQualityVerdictReader();
    final String nativePass="""
        {"assessments":[{"sentence":"Yes, i can.","scope":"SENTENCE","status":"PASS","intendedMeaning":"Aceptar ayudar","troublesomeSpan":null,"issue":null,"severity":null,"reason":"Respuesta idiomática","neededEvidence":null,"registerConcern":null,"writingConcern":"Usar I mayúscula"}],"rationale":"Formato separado"}
        """;
    final String suitable="""
        {"quizId":"q","status":"SUITABLE","targetStructure":"Respuesta corta","affectedSentences":[],"issue":null,"reason":"Ejemplo claro","neededEvidence":null,"handoffs":[],"proposedAction":null,"proposedExample":null}
        """;
    @Test void acceptsIdiomaticAnswerWithSeparateWritingConcern() {
        var verdict=reader.readNaturalness(nativePass);
        assertEquals(NativeUsageVerdict.Status.PASS,verdict.assessments().get(0).status());
        assertNotNull(verdict.assessments().get(0).writingConcern());
        assertEquals(A1DidacticVerdict.Status.SUITABLE,reader.readDidactic(suitable).status());
    }
    @Test void rejectsUnknownFieldsTrailingJsonAndDuplicateKeys() {
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(nativePass.replace("\"rationale\":","\"invented\":1,\"rationale\":")));
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(nativePass+"{}"));
        assertThrows(IllegalArgumentException.class,()->reader.readDidactic(suitable.replace("\"quizId\":\"q\"","\"quizId\":\"q\",\"quizId\":\"other\"")));
    }
    @Test void rejectsCoercedTypesAndUnjustifiedFailureOrReview() {
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(nativePass.replace("\"writingConcern\":\"Usar I mayúscula\"","\"writingConcern\":true")));
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(nativePass.replace("\"status\":\"PASS\"","\"status\":\"FAIL\"")));
        assertThrows(IllegalArgumentException.class,()->reader.readDidactic(suitable.replace("\"status\":\"SUITABLE\"","\"status\":\"REVIEW\"")));
    }
    @Test void rejectsCrossJudgePayloadsAndDuplicateAcceptedVariants() {
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(suitable));
        assertThrows(IllegalArgumentException.class,()->reader.readDidactic(nativePass));
        var start=nativePass.indexOf('{',nativePass.indexOf('['));var end=nativePass.indexOf('}',start)+1;
        var duplicate=nativePass.substring(0,end)+","+nativePass.substring(start,end)+nativePass.substring(end);
        assertThrows(IllegalArgumentException.class,()->reader.readNaturalness(duplicate));
    }
}
