package com.learney.contentaudit.quizinstructioninfrastructure.englishquality;

import com.fasterxml.jackson.databind.*;
import com.learney.contentaudit.auditdomain.englishquality.*;
import java.util.Set;

/** Closed JSON types before semantic validation; unusable output can never become approval. */
public final class JacksonEnglishQualityVerdictReader implements EnglishQualityVerdictReader {
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    @Override public NativeUsageVerdict readNaturalness(String payload) {
        try {
            var n=json.readTree(payload); closed(n,Set.of("assessments","rationale")); text(n,"rationale");
            array(n,"assessments");
            for(var a:n.get("assessments")) {
                closed(a,Set.of("sentence","scope","status","intendedMeaning","troublesomeSpan","issue","severity","reason","neededEvidence","registerConcern","writingConcern"));
                for(var key:Set.of("sentence","scope","status","reason")) text(a,key);
                for(var key:Set.of("intendedMeaning","troublesomeSpan","issue","severity","neededEvidence","registerConcern","writingConcern")) optional(a,key);
            }
            return json.treeToValue(n,NativeUsageVerdict.class);
        } catch(Exception e) { throw new IllegalArgumentException("Naturalidad ilegible: "+e.getMessage(),e); }
    }
    @Override public A1DidacticVerdict readDidactic(String payload) {
        try {
            var n=json.readTree(payload);
            closed(n,Set.of("quizId","status","targetStructure","affectedSentences","issue","reason","neededEvidence","handoffs","proposedAction","proposedExample"));
            for(var key:Set.of("quizId","status","reason")) text(n,key);
            for(var key:Set.of("targetStructure","issue","neededEvidence","proposedAction","proposedExample")) optional(n,key);
            for(var key:Set.of("affectedSentences","handoffs")) { array(n,key); for(var value:n.get(key)) if(!value.isTextual()) throw new IllegalArgumentException("Elemento no textual"); }
            return json.treeToValue(n,A1DidacticVerdict.class);
        } catch(Exception e) { throw new IllegalArgumentException("Calidad A1 ilegible: "+e.getMessage(),e); }
    }
    private static void closed(JsonNode n,Set<String> keys) {
        if(n==null || !n.isObject() || n.size()!=keys.size() || !keys.stream().allMatch(n::has)) throw new IllegalArgumentException("Objeto incompleto o con claves desconocidas");
    }
    private static void text(JsonNode n,String key) { if(!n.get(key).isTextual()) throw new IllegalArgumentException("Campo no textual: "+key); }
    private static void optional(JsonNode n,String key) { if(!n.get(key).isTextual() && !n.get(key).isNull()) throw new IllegalArgumentException("Campo opcional no textual: "+key); }
    private static void array(JsonNode n,String key) { if(!n.get(key).isArray()) throw new IllegalArgumentException("Campo no array: "+key); }
}
