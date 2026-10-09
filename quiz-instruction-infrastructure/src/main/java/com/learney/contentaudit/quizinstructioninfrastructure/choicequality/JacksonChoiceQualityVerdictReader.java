package com.learney.contentaudit.quizinstructioninfrastructure.choicequality;

import com.fasterxml.jackson.databind.*;
import com.learney.contentaudit.auditdomain.choicequality.*;
import java.util.Set;

/** Strict boundary: incomplete output must not become a durable passing verdict. */
public final class JacksonChoiceQualityVerdictReader implements ChoiceQualityVerdictReader {
    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    @Override public ChoiceQualityVerdict read(String payload) {
        try {
            JsonNode node = json.readTree(payload);
            Set<String> required = Set.of("status", "objective", "introductory", "issues", "rationale");
            if (node == null || !node.isObject() || node.size() != required.size()
                    || !required.stream().allMatch(node::has)
                    || !node.get("status").isTextual() || !node.get("objective").isTextual()
                    || !node.get("introductory").isBoolean() || !node.get("issues").isArray()
                    || !node.get("rationale").isTextual()) throw new IllegalArgumentException("Veredicto de distractores incompleto");
            Set<String> issueKeys = Set.of("kind", "severity", "labels", "reason", "misconception");
            for (JsonNode issue : node.get("issues")) {
                if (!issue.isObject() || issue.size() != issueKeys.size() || !issueKeys.stream().allMatch(issue::has)
                        || !issue.get("kind").isTextual() || !issue.get("severity").isTextual()
                        || !issue.get("labels").isArray() || !issue.get("reason").isTextual()
                        || !issue.get("misconception").isTextual()) throw new IllegalArgumentException("Problema de distractores incompleto");
                for (JsonNode label : issue.get("labels")) {
                    if (!label.isTextual()) throw new IllegalArgumentException("Las opciones deben ser texto");
                }
            }
            return json.treeToValue(node, ChoiceQualityVerdict.class);
        } catch (Exception failure) {
            throw new IllegalArgumentException("Veredicto de distractores ilegible: " + failure.getMessage(), failure);
        }
    }
}
