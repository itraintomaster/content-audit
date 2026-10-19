package com.learney.contentaudit.quizinstructioninfrastructure.pragmatism;

import com.fasterxml.jackson.databind.*;
import com.learney.contentaudit.auditdomain.pragmatism.*;
import java.util.Set;

/** Strict JSON types and closed shape before the domain's cross-field validation. */
public final class JacksonPragmatismVerdictReader implements PragmatismVerdictReader {
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    @Override public PragmatismVerdict read(String payload) {
        try {
            var node = json.readTree(payload);
            closed(node, Set.of("assessments", "rationale"));
            if (!node.get("assessments").isArray() || !node.get("rationale").isTextual())
                throw new IllegalArgumentException("Veredicto pragmático incompleto");
            for (var a : node.get("assessments")) {
                closed(a, Set.of("sentence", "status", "communicativePurpose", "plausibleUse", "issue", "severity", "reason", "neededContext"));
                for (String key : Set.of("sentence", "status", "communicativePurpose", "reason"))
                    if (!a.get(key).isTextual()) throw new IllegalArgumentException("Campo textual inválido: " + key);
                for (String key : Set.of("plausibleUse", "issue", "severity", "neededContext"))
                    if (!a.get(key).isTextual() && !a.get(key).isNull())
                        throw new IllegalArgumentException("Campo opcional inválido: " + key);
            }
            return json.treeToValue(node, PragmatismVerdict.class);
        } catch (Exception failure) {
            throw new IllegalArgumentException("Veredicto pragmático ilegible: " + failure.getMessage(), failure);
        }
    }
    private static void closed(JsonNode node, Set<String> keys) {
        if (node == null || !node.isObject() || node.size() != keys.size() || !keys.stream().allMatch(node::has))
            throw new IllegalArgumentException("Objeto incompleto o con claves desconocidas");
    }
}
