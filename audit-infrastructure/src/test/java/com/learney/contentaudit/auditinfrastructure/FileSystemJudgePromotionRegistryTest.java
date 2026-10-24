package com.learney.contentaudit.auditinfrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.auditdomain.judging.JudgeExamException;
import com.learney.contentaudit.auditdomain.judging.JudgePromotionRecord;
import com.learney.contentaudit.evaluationledgerdomain.EvaluationRuntimeIdentity;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemJudgePromotionRegistryTest {
    @TempDir Path directory;
    final EvaluationRuntimeIdentity identity=EvaluationRuntimeIdentity.explicit("codex-cli","synthetic","medium");
    JudgePromotionRecord promotion(String version) {return new JudgePromotionRecord("naturalness",version,identity,"José","Acepto el piloto","approval.json","a".repeat(64),Instant.parse("2026-10-14T06:00:00Z"));}
    JudgeExamException exception() {
        return new JudgeExamException("sha256:"+"b".repeat(64),"sha256:"+"c".repeat(64),
                Instant.parse("2026-10-22T18:54:24Z"),List.of("REG-watch"),List.of("quiz-watch"));
    }
    JudgePromotionRecord promotionWithException(String version,EvaluationRuntimeIdentity runtime,JudgeExamException exception) {
        return new JudgePromotionRecord("naturalness",version,runtime,"José","Acepto solamente la discrepancia de watch",
                "approval-watch.json","d".repeat(64),Instant.parse("2026-10-22T20:00:00Z"),exception);
    }
    @Test void persistsSeparatelyAndDoesNotGrantToDifferentVersionOrRuntime() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);store.record(promotion("v1"));
        var reloaded=new FileSystemJudgePromotionRegistry(directory);
        assertEquals(promotion("v1"),reloaded.latest("naturalness","v1",identity).orElseThrow());
        assertTrue(reloaded.latest("naturalness","v2",identity).isEmpty());
        assertTrue(reloaded.latest("naturalness","v1",EvaluationRuntimeIdentity.explicit("codex-cli","other","medium")).isEmpty());
        assertFalse(Files.exists(directory.resolve(".content-audit/judge-exams")));
    }
    @Test @org.junit.jupiter.api.Tag("FEAT-ALDIA") @org.junit.jupiter.api.Tag("F-ALDIA-R003")
    void historyListsEveryValidPromotionOfTheJudgeInTheOrderTheyWereRecorded() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);
        assertEquals(java.util.List.of(),store.history("naturalness"));
        store.record(promotion("v1"));store.record(promotion("v2"));
        Files.writeString(directory.resolve(".content-audit/judge-promotions/naturalness.jsonl"),"{\"judgeName\":\"naturalness\"}\n",StandardOpenOption.APPEND);
        assertEquals(java.util.List.of(promotion("v1"),promotion("v2")),new FileSystemJudgePromotionRegistry(directory).history("naturalness"));
        assertEquals(java.util.List.of(),store.history("a1-didactic"));
    }
    @Test void preservesOldLinesAndIgnoresAnIncompleteRecord() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);store.record(promotion("v1"));
        var file=directory.resolve(".content-audit/judge-promotions/naturalness.jsonl");String before=Files.readString(file);
        store.record(promotion("v2"));assertTrue(Files.readString(file).startsWith(before));
        Files.writeString(file,"{\"judgeName\":\"naturalness\"}\n",StandardOpenOption.APPEND);
        assertEquals(promotion("v1"),store.latest("naturalness","v1",identity).orElseThrow());
        assertTrue(store.latest("naturalness","v1",EvaluationRuntimeIdentity.legacyUnknown()).isEmpty());
    }

    @Test void roundTripsTheExactExamExceptionWithoutRewritingLegacyRecords() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);
        store.record(promotion("v1"));
        var file=directory.resolve(".content-audit/judge-promotions/naturalness.jsonl");
        String legacy=Files.readString(file);
        assertFalse(new ObjectMapper().readTree(legacy).has("examException"));
        var scoped=promotionWithException("v2",identity,exception());
        store.record(scoped);

        assertTrue(Files.readString(file).startsWith(legacy));
        var reloaded=new FileSystemJudgePromotionRegistry(directory);
        assertEquals(List.of(promotion("v1"),scoped),reloaded.history("naturalness"));
        assertNull(reloaded.latest("naturalness","v1",identity).orElseThrow().examException());
        assertEquals(exception(),reloaded.latest("naturalness","v2",identity).orElseThrow().examException());
        JsonNode persisted=new ObjectMapper().readTree(Files.readAllLines(file).get(1)).path("examException");
        assertEquals(5,persisted.size());
        assertEquals(exception().examFingerprint(),persisted.path("examFingerprint").asText());
        assertEquals(exception().regressionSuiteFingerprint(),persisted.path("regressionSuiteFingerprint").asText());
        assertEquals(exception().examRecordedAt().toString(),persisted.path("examRecordedAt").asText());
        assertEquals("REG-watch",persisted.path("acceptedRegressionCaseIds").get(0).asText());
        assertEquals("quiz-watch",persisted.path("acceptedMismatchedCaseIds").get(0).asText());
    }

    @Test void rejectsTheWholePromotionWhenAnExceptionFieldIsMalformedOrIncomplete() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);
        store.record(promotionWithException("v2",identity,exception()));
        var file=directory.resolve(".content-audit/judge-promotions/naturalness.jsonl");
        var mapper=new ObjectMapper();
        ObjectNode valid=(ObjectNode)mapper.readTree(Files.readString(file));
        List<JsonNode> invalidScopes=new ArrayList<>();
        invalidScopes.add(mapper.nullNode());
        invalidScopes.add(mapper.getNodeFactory().textNode("legacy"));
        invalidScopes.add(mapper.createArrayNode());
        invalidScopes.add(mapper.createObjectNode());
        for (String field:List.of("examFingerprint","regressionSuiteFingerprint","examRecordedAt",
                "acceptedRegressionCaseIds","acceptedMismatchedCaseIds")) {
            ObjectNode missing=((ObjectNode)valid.get("examException")).deepCopy();
            missing.remove(field);
            invalidScopes.add(missing);
            ObjectNode wrongType=((ObjectNode)valid.get("examException")).deepCopy();
            wrongType.put(field,7);
            invalidScopes.add(wrongType);
        }
        ObjectNode badDate=((ObjectNode)valid.get("examException")).deepCopy();
        badDate.put("examRecordedAt","not-an-instant");
        invalidScopes.add(badDate);
        for (String field:List.of("acceptedRegressionCaseIds","acceptedMismatchedCaseIds")) {
            ObjectNode badCase=((ObjectNode)valid.get("examException")).deepCopy();
            badCase.putArray(field).add(7);
            invalidScopes.add(badCase);
            // Sin regresiones aceptadas es válido (la suite permanente puede estar vacía); sin discrepancias, no.
            if (field.equals("acceptedMismatchedCaseIds")) {
                ObjectNode emptyCases=((ObjectNode)valid.get("examException")).deepCopy();
                emptyCases.putArray(field);
                invalidScopes.add(emptyCases);
            }
            ObjectNode blankCase=((ObjectNode)valid.get("examException")).deepCopy();
            blankCase.putArray(field).add(" ");
            invalidScopes.add(blankCase);
            ObjectNode duplicateCases=((ObjectNode)valid.get("examException")).deepCopy();
            duplicateCases.putArray(field).add("same-case").add("same-case");
            invalidScopes.add(duplicateCases);
        }
        for (String field:List.of("examFingerprint","regressionSuiteFingerprint")) {
            ObjectNode blankFingerprint=((ObjectNode)valid.get("examException")).deepCopy();
            blankFingerprint.put(field," ");
            invalidScopes.add(blankFingerprint);
        }
        for (JsonNode invalid:invalidScopes) {
            ObjectNode record=valid.deepCopy();
            record.set("examException",invalid);
            Files.writeString(file,mapper.writeValueAsString(record)+"\n");
            var reloaded=new FileSystemJudgePromotionRegistry(directory);
            assertTrue(reloaded.history("naturalness").isEmpty(),invalid.toString());
            assertTrue(reloaded.latest("naturalness","v2",identity).isEmpty(),invalid.toString());
        }
    }

    @Test void keepsExceptionBoundToEachVersionAndRuntimeAcrossMultiplePromotions() throws Exception {
        var store=new FileSystemJudgePromotionRegistry(directory);
        var otherRuntime=EvaluationRuntimeIdentity.explicit("codex-cli","other","medium");
        var original=promotionWithException("v1",identity,exception());
        var otherVersion=promotionWithException("v2",identity,new JudgeExamException("sha256:"+"e".repeat(64),
                "sha256:"+"f".repeat(64),Instant.parse("2026-10-23T18:54:24Z"),List.of("REG-next"),List.of("quiz-next")));
        var otherModel=promotionWithException("v1",otherRuntime,exception());
        store.record(original);store.record(otherVersion);store.record(otherModel);

        var reloaded=new FileSystemJudgePromotionRegistry(directory);
        assertEquals(List.of(original,otherVersion,otherModel),reloaded.history("naturalness"));
        assertEquals(original,reloaded.latest("naturalness","v1",identity).orElseThrow());
        assertEquals(otherVersion,reloaded.latest("naturalness","v2",identity).orElseThrow());
        assertEquals(otherModel,reloaded.latest("naturalness","v1",otherRuntime).orElseThrow());
        assertTrue(reloaded.latest("naturalness","v2",otherRuntime).isEmpty());
    }
}
