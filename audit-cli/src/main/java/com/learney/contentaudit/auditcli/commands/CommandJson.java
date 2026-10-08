package com.learney.contentaudit.auditcli.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learney.contentaudit.revisiondomain.projection.ProjectionScope;
import java.util.List;
import java.util.Map;

/**
 * What the commands of package F share: their JSON -- dates as ISO text, without the empty fields -- and the scope
 * a request names with {@code --proposal} and {@code --budget}.
 */
final class CommandJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Omit absent bean properties, but preserve explicit nulls in opaque course maps.
            // Dropping those entries changes snapshots on the UI round trip and rejects valid reviews.
            .setDefaultPropertyInclusion(JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS));

    private CommandJson() {
    }

    static String write(Object value) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write the JSON: " + e.getMessage(), e);
        }
    }

    /** Large saved read models travel to the UI without indentation overhead. */
    static String writeCompact(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write the JSON: " + e.getMessage(), e);
        }
    }

    static boolean isJson(String format) {
        return format != null && "json".equalsIgnoreCase(format.trim());
    }

    static <T> T read(String value, Class<T> type) {
        try {
            return MAPPER.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON inválido: " + e.getMessage(), e);
        }
    }

    /**
     * The scope of a request: the pending proposals it names -- every pending proposal of the active plan when it
     * names none -- and the budget of the analyzers it names. Null when it names nothing.
     */
    static ProjectionScope scope(List<String> proposalIds, Map<String, Integer> analyzerBudgets) {
        boolean proposals = proposalIds != null && !proposalIds.isEmpty();
        boolean budgets = analyzerBudgets != null && !analyzerBudgets.isEmpty();
        if (!proposals && !budgets) {
            return null;
        }
        return new ProjectionScope(null, proposals ? List.copyOf(proposalIds) : null,
                budgets ? Map.copyOf(analyzerBudgets) : null);
    }
}
