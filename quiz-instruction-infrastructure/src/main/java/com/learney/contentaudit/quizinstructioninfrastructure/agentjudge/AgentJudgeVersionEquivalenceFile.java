package com.learney.contentaudit.quizinstructioninfrastructure.agentjudge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.evaluationledgerdomain.EvaluatorVersionEquivalence;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * F-ALDIA-R002: where the one-time equivalences are written, beside the agent definitions they
 * speak of, one JSON object per line. It is written once and never again: the cut-over creates it
 * and refuses to touch it after, and reading honours only that first batch, so a line added later
 * by hand does not make another version the same (inv. 4).
 */
class AgentJudgeVersionEquivalenceFile {
    static final String FILE_NAME = "version-equivalences.jsonl";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** The equivalences of the cut-over, empty when it has not run in this agents folder. */
    List<EvaluatorVersionEquivalence> read(Path agentsDir) {
        Path file = agentsDir != null ? agentsDir.resolve(FILE_NAME) : null;
        if (file == null || !Files.isRegularFile(file)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Unreadable is the same as absent: no equivalence rather than a guessed one.
            return List.of();
        }
        List<EvaluatorVersionEquivalence> firstBatch = new ArrayList<>();
        Instant batch = null;
        for (String line : lines) {
            EvaluatorVersionEquivalence equivalence = parse(line);
            if (equivalence == null) {
                continue;
            }
            if (batch == null) {
                batch = equivalence.getRecordedAt();
            }
            if (batch.equals(equivalence.getRecordedAt())) {
                firstBatch.add(equivalence);
            }
        }
        return List.copyOf(firstBatch);
    }

    /**
     * Writes the cut-over's equivalences, all with the same moment. Fails when the file is already
     * there: the equivalence is once (F-ALDIA-R002 inv. 4).
     */
    void writeOnce(Path agentsDir, List<EvaluatorVersionEquivalence> equivalences) throws IOException {
        StringBuilder content = new StringBuilder();
        for (EvaluatorVersionEquivalence equivalence : equivalences) {
            ObjectNode line = objectMapper.createObjectNode();
            line.put("agent", equivalence.getEvaluatorId());
            line.put("earlierVersion", equivalence.getEarlierVersion());
            line.put("currentVersion", equivalence.getCurrentVersion());
            line.put("recordedAt", equivalence.getRecordedAt().toString());
            line.put("reason", equivalence.getReason());
            content.append(objectMapper.writeValueAsString(line)).append('\n');
        }
        Files.writeString(agentsDir.resolve(FILE_NAME), content.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    boolean exists(Path agentsDir) {
        return agentsDir != null && Files.exists(agentsDir.resolve(FILE_NAME));
    }

    private EvaluatorVersionEquivalence parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(line);
            String agent = text(node, "agent");
            String earlier = text(node, "earlierVersion");
            String current = text(node, "currentVersion");
            String recordedAt = text(node, "recordedAt");
            if (agent == null || earlier == null || current == null || recordedAt == null) {
                return null;
            }
            return new EvaluatorVersionEquivalence(agent, earlier, current, Instant.parse(recordedAt),
                    text(node, "reason"));
        } catch (IOException | DateTimeParseException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
