package com.learney.contentaudit.vocabularyinfrastructure.evp;
import javax.annotation.processing.Generated;

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
    // Reverse index: lemma+pos -> its CEFR level (lowest level wins on collision)
    private final Map<LemmaAndPos, CefrLevel> levelByLemma = new HashMap<>();
    // F-LABS-R036 (paso 2): lemma (sin POS) -> MENOR nivel entre todas sus entradas.
    // Solo se consulta cuando no existe entrada exacta para el par lemma+POS.
    private final Map<String, CefrLevel> lowestLevelByLemmaOnly = new HashMap<>();
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
                // F-LABS-R033: C1/C2 se retienen (participan solo de la mal-ubicacion, Grupo D).
                // Solo se descartan niveles no representables en el enum.
                if (level == null) continue;

                LemmaAndPos lp = new LemmaAndPos(lemma, posTag);
                expectedByLevel.get(level).add(lp);

                // Detect phrases: multi-word entries. La fuente de verdad de "fila
                // phrasal" es el headword del catalogo con espacios (misma señal que
                // alimenta isPhrase).
                boolean phrasal = word != null && word.contains(" ");
                if (phrasal) {
                    phrases.add(lemma);
                    phrases.add(word);
                }

                // Los indices de consulta de nivel (lookupLevel / lookupLevelByLemma)
                // solo admiten filas palabra-simple: una entrada phrasal ("sleep on it",
                // C2, lemma=sleep) no define el nivel del lema suelto — con la
                // precedencia del match exacto (F-LABS-R036 paso 1) contaminaria el
                // scoring con falsos C1/C2. El filtrado de frases en la cobertura
                // (F-LABS-R005) sigue siendo via isPhrase sobre getExpectedLemmas,
                // que queda intacto.
                if (!phrasal) {
                    // Keep the lowest level if a lemma+pos appears in multiple levels
                    levelByLemma.merge(lp, level, FileSystemEvpCatalog::lowest);
                    // F-LABS-R036 (paso 2): menor nivel del lema bajo cualquier POS
                    lowestLevelByLemmaOnly.merge(lemma, level, FileSystemEvpCatalog::lowest);
                }

                if (freqRank > 0) {
                    cocaRanks.putIfAbsent(lp, freqRank);
                }
                if (topic != null && !topic.isEmpty()) {
                    semanticCategories.putIfAbsent(lp, topic);
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

    private static CefrLevel lowest(CefrLevel a, CefrLevel b) {
        return a.ordinal() <= b.ordinal() ? a : b;
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

    @Override
    public Optional<CefrLevel> lookupLevel(LemmaAndPos lemmaAndPos) {
        return Optional.ofNullable(levelByLemma.get(lemmaAndPos));
    }


    /**
     * F-LABS-R036 (paso 2, fallback por lema): devuelve el MENOR nivel entre todas las
     * entradas del catalogo para el lema, bajo cualquier categoria gramatical (beneficio
     * de la duda: el fallback nunca penaliza mas). Optional.empty() = fuera de catalogo.
     * NO reemplaza el match exacto por par de {@link #lookupLevel(LemmaAndPos)}, que
     * tiene precedencia estricta.
     */
    @Override
    public Optional<CefrLevel> lookupLevelByLemma(String lemma) {
        if (lemma == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lowestLevelByLemmaOnly.get(lemma));
    }

}
