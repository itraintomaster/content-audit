package com.learney.contentaudit.quizinstructioninfrastructure.answergiveaway;

import com.fasterxml.jackson.databind.*;
import com.learney.contentaudit.auditdomain.answergiveaway.*;
import java.util.Set;

/** Strict JSON types and closed shape before the domain's cross-field validation. */
public final class JacksonAnswerGiveawayVerdictReader implements AnswerGiveawayVerdictReader {
    private static final Set<String> KEYS = Set.of("quizId", "status", "practicedDecision", "where", "givenAway",
            "extent", "reason", "proposedAction", "handoffs");
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    @Override public AnswerGiveawayVerdict read(String payload) {
        try {
            var node = json.readTree(payload);
            if (node == null || !node.isObject() || node.size() != KEYS.size() || !KEYS.stream().allMatch(node::has))
                throw new IllegalArgumentException("Objeto incompleto o con claves desconocidas");
            for (String key : Set.of("quizId", "status", "reason"))
                if (!node.get(key).isTextual()) throw new IllegalArgumentException("Campo textual inválido: " + key);
            for (String key : Set.of("practicedDecision", "where", "givenAway", "extent", "proposedAction"))
                if (!node.get(key).isTextual() && !node.get(key).isNull())
                    throw new IllegalArgumentException("Campo opcional inválido: " + key);
            if (!node.get("handoffs").isArray()) throw new IllegalArgumentException("Traspasos inválidos");
            for (var handoff : node.get("handoffs"))
                if (!handoff.isTextual()) throw new IllegalArgumentException("Traspaso inválido");
            return json.treeToValue(node, AnswerGiveawayVerdict.class);
        } catch (Exception failure) {
            throw new IllegalArgumentException("Veredicto de «no regala» ilegible: " + failure.getMessage(), failure);
        }
    }
}
