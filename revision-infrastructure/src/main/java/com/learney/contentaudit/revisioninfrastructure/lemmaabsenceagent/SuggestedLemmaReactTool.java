package com.learney.contentaudit.revisioninfrastructure.lemmaabsenceagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.refinerdomain.SuggestedLemma;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQueryCriteria;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQueryRejectedException;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQueryResult;
import com.learney.contentaudit.refinerdomain.SuggestedLemmaQuerySession;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.Optional;

/**
 * Hand-written (non-generated) langchain4j {@code @Tool} wrapper over a bound
 * {@link SuggestedLemmaQuerySession}.
 *
 * <p>This class is intentionally NOT declared in {@code sentinel.yaml}: the Sentinel generator
 * cannot emit {@code @Tool}/{@code @P} annotations, and {@code @Generated} files cannot be
 * hand-edited.  A non-governed helper is therefore the correct approach (sanctioned by the user).
 *
 * <p>The tool name {@code "get_suggested_lemmas"} must match the tool id declared in the
 * declarative agent graph (F-CSLATDC-R015).
 */
class SuggestedLemmaReactTool implements SuggestedLemmaAgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SuggestedLemmaQuerySession session;

    SuggestedLemmaReactTool(SuggestedLemmaQuerySession session) {
        this.session = session;
    }

    @Tool("Devuelve lemmas sugeridos para la task (del audit fuente de la revisión), filtrables. Devuelve JSON con la lista de lemmas.")
    public String get_suggested_lemmas(
            @P(value = "part of speech: NOUN/VERB/ADJ/ADV/... (vacío = sin filtro)", required = false)
            String partOfSpeech,
            @P(value = "regex sobre el TEXTO del lema: match parcial/grep, case-insensitive; anclá con ^ y $ (ej 'ing$', '^[aeiou]') (vacío = sin filtro)", required = false)
            String regex,
            @P(value = "máximo de resultados, ej '40' (vacío = default)", required = false)
            String limit,
            @P(value = "'true' para incluir lemas ya expuestos (vacío/false = no)", required = false)
            String includeExposed,
            @P(value = "nivel CEFR explícito, ej 'A1' (vacío = el de la task)", required = false)
            String level) {

        // Parse limit (null/blank/invalid → empty)
        Optional<Integer> parsedLimit = Optional.empty();
        if (limit != null && !limit.isBlank()) {
            try {
                parsedLimit = Optional.of(Integer.parseInt(limit.trim()));
            } catch (NumberFormatException ignored) {
                // non-numeric → no limit filter
            }
        }

        // Parse partOfSpeech (null/blank → empty)
        Optional<String> parsedPos = (partOfSpeech != null && !partOfSpeech.isBlank())
                ? Optional.of(partOfSpeech.trim())
                : Optional.empty();

        // Parse regex (null/blank → empty)
        Optional<String> parsedRegex = (regex != null && !regex.isBlank())
                ? Optional.of(regex.trim())
                : Optional.empty();

        // Parse includeExposed ("true" case-insensitive → true, else false)
        boolean parsedIncludeExposed = "true".equalsIgnoreCase(
                includeExposed != null ? includeExposed.trim() : null);

        // Parse explicit CEFR level (null/blank/invalid → empty)
        Optional<CefrLevel> parsedLevel = Optional.empty();
        if (level != null && !level.isBlank()) {
            try {
                parsedLevel = Optional.of(CefrLevel.valueOf(level.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // unrecognized level → no explicit level filter
            }
        }

        SuggestedLemmaQueryCriteria criteria = new SuggestedLemmaQueryCriteria(
                parsedLimit,
                parsedPos,
                parsedLevel,
                parsedIncludeExposed,
                parsedRegex);

        try {
            SuggestedLemmaQueryResult result = session.requestMore(criteria);
            return serializeResult(result);
        } catch (SuggestedLemmaQueryRejectedException e) {
            try {
                ObjectNode errorNode = MAPPER.createObjectNode();
                errorNode.put("error", e.getMessage());
                errorNode.put("taskId", e.getTaskId());
                errorNode.put("reason", e.getReason());
                return MAPPER.writeValueAsString(errorNode);
            } catch (Exception jsonEx) {
                return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            }
        }
    }

    /**
     * Satisfies {@link SuggestedLemmaAgentTool#fetchSuggestedLemmas} — delegates to the session
     * directly (used by tests that verify the tool contract via the interface).
     */
    @Override
    public SuggestedLemmaQueryResult fetchSuggestedLemmas(SuggestedLemmaQueryCriteria criteria) {
        return session.requestMore(criteria);
    }

    // -----------------------------------------------------------------------
    // Serialization helpers
    // -----------------------------------------------------------------------

    private static String serializeResult(SuggestedLemmaQueryResult result) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("taskId", result.getTaskId());
            root.put("cefrLevel",
                    result.getCefrLevel() != null ? result.getCefrLevel().name() : null);

            ArrayNode lemmasArray = MAPPER.createArrayNode();
            List<SuggestedLemma> lemmas = result.getSuggestedLemmas();
            if (lemmas != null) {
                for (SuggestedLemma sl : lemmas) {
                    ObjectNode lemmaNode = MAPPER.createObjectNode();
                    lemmaNode.put("lemma", sl.getLemma());
                    lemmaNode.put("pos", sl.getPos());
                    lemmaNode.put("reason", sl.getReason());
                    if (sl.getCocaRank() != null) {
                        lemmaNode.put("cocaRank", sl.getCocaRank());
                    } else {
                        lemmaNode.putNull("cocaRank");
                    }
                    // lemma-count triple (optional)
                    if (sl.getLemmaCount() != null) {
                        lemmaNode.put("lemmaCount", sl.getLemmaCount());
                    }
                    if (sl.getLemmaCountThreshold() != null) {
                        lemmaNode.put("lemmaCountThreshold", sl.getLemmaCountThreshold());
                    }
                    if (sl.getIsUnderexposed() != null) {
                        lemmaNode.put("isUnderexposed", sl.getIsUnderexposed());
                    }
                    lemmasArray.add(lemmaNode);
                }
            }
            root.set("suggestedLemmas", lemmasArray);

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\":\"serialization failed: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
