package com.learney.contentaudit.revisioninfrastructure.changereview;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReview;
import com.learney.contentaudit.revisiondomain.changereview.ChangeReviewVerdictReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * F-MARCA-R002 and R004: strict JSON types and a closed shape, then the domain's own checks of the mark. A problem says
 * whether production already had it (F-MARCA-R010); one saved by an earlier version, which did not say, is new.
 */
public final class JacksonChangeReviewVerdictReader implements ChangeReviewVerdictReader {
    private static final Set<String> KEYS = Set.of("quizId", "mark", "problems", "reason");
    private static final Set<String> PROBLEM_KEYS = Set.of("where", "fragment", "what");
    private static final Set<String> PROBLEM_KEYS_WITH_INHERITED = Set.of("where", "fragment", "what", "inherited");
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    @Override
    public ChangeReview.Verdict read(String payload) {
        try {
            JsonNode node = json.readTree(payload);
            closed(node, KEYS);
            for (String key : List.of("quizId", "mark", "reason"))
                if (!node.get(key).isTextual()) throw new IllegalArgumentException("Campo textual inválido: " + key);
            if (!node.get("problems").isArray()) throw new IllegalArgumentException("Problemas inválidos");
            var problems = new ArrayList<ChangeReview.Problem>();
            for (JsonNode problem : node.get("problems")) {
                boolean says = problem != null && problem.has("inherited");
                closed(problem, says ? PROBLEM_KEYS_WITH_INHERITED : PROBLEM_KEYS);
                for (String key : PROBLEM_KEYS)
                    if (!problem.get(key).isTextual()) throw new IllegalArgumentException("Campo de problema inválido: " + key);
                if (says && !problem.get("inherited").isBoolean())
                    throw new IllegalArgumentException("Campo de problema inválido: inherited");
                problems.add(new ChangeReview.Problem(ChangeReview.Where.valueOf(problem.get("where").asText()),
                        problem.get("fragment").asText(), problem.get("what").asText(),
                        says && problem.get("inherited").asBoolean()));
            }
            return new ChangeReview.Verdict(node.get("quizId").asText(), ChangeReview.Mark.valueOf(node.get("mark").asText()),
                    problems, node.get("reason").asText());
        } catch (Exception failure) {
            throw new IllegalArgumentException("Marca del revisor ilegible: " + failure.getMessage(), failure);
        }
    }

    private static void closed(JsonNode node, Set<String> keys) {
        if (node == null || !node.isObject() || node.size() != keys.size() || !keys.stream().allMatch(node::has))
            throw new IllegalArgumentException("Objeto incompleto o con claves desconocidas");
    }
}
