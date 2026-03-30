package com.learney.contentaudit.vocabularyinfrastructure.evp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learney.contentaudit.auditdomain.CefrLevel;
import com.learney.contentaudit.auditdomain.EvpCatalogPort;
import com.learney.contentaudit.auditdomain.labs.LemmaAndPos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the enriched vocabulary catalog JSON and exposes it through the EvpCatalogPort interface.
 * The catalog is loaded once at construction time and kept in memory.
 */
public class FileSystemEvpCatalog implements EvpCatalogPort {

    private final Map<CefrLevel, Set<LemmaAndPos>> expectedByLevel = new EnumMap<>(CefrLevel.class);
    private final Map<LemmaAndPos, Integer> cocaRanks = new HashMap<>();
    private final Map<LemmaAndPos, String> semanticCategories = new HashMap<>();
    private final Set<String> phrases = new HashSet<>();

    public FileSystemEvpCatalog(Path catalogPath) {
        for (CefrLevel level : CefrLevel.values()) {
            expectedByLevel.put(level, new HashSet<>());
        }
        loadCatalog(catalogPath);
    }

    private void loadCatalog(Path catalogPath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(catalogPath.toFile());
            JsonNode words = root.get("enriched_words");
            if (words == null || !words.isArray()) return;

            for (JsonNode entry : words) {
                String word = textOrNull(entry, "word");
                String lemma = textOrNull(entry, "lemma");
                String posTag = textOrNull(entry, "spacyPosTag");
                String levelStr = textOrNull(entry, "cefrLevel");
                String topic = textOrNull(entry, "topic");
                int freqRank = entry.has("frequencyRank") ? entry.get("frequencyRank").asInt(0) : 0;

                if (lemma == null || posTag == null || levelStr == null) continue;

                CefrLevel level = parseCefrLevel(levelStr);
                if (level == null) continue; // skip C1, C2

                LemmaAndPos lp = new LemmaAndPos(lemma, posTag);
                expectedByLevel.get(level).add(lp);

                if (freqRank > 0) {
                    cocaRanks.putIfAbsent(lp, freqRank);
                }
                if (topic != null && !topic.isEmpty()) {
                    semanticCategories.putIfAbsent(lp, topic);
                }

                // Detect phrases: multi-word entries
                if (word != null && word.contains(" ")) {
                    phrases.add(lemma);
                    phrases.add(word);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load EVP catalog from " + catalogPath, e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        String text = child.asText();
        return text.isEmpty() ? null : text;
    }

    private static CefrLevel parseCefrLevel(String str) {
        try {
            return CefrLevel.valueOf(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Set<LemmaAndPos> getExpectedLemmas(CefrLevel level) {
        return expectedByLevel.getOrDefault(level, Set.of());
    }

    @Override
    public boolean isPhrase(String lemma) {
        return lemma != null && (lemma.contains(" ") || phrases.contains(lemma));
    }

    @Override
    public Optional<Integer> getCocaRank(LemmaAndPos lemmaAndPos) {
        return Optional.ofNullable(cocaRanks.get(lemmaAndPos));
    }

    @Override
    public Optional<String> getSemanticCategory(LemmaAndPos lemmaAndPos) {
        return Optional.ofNullable(semanticCategories.get(lemmaAndPos));
    }
}
