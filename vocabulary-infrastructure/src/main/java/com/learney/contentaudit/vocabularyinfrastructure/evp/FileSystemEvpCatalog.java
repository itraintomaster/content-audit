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
import java.util.Locale;
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
                String spacyPos = textOrNull(entry, "spacyPosTag");
                String evpPos = textOrNull(entry, "evpPartOfSpeech");
                String levelStr = textOrNull(entry, "cefrLevel");
                String topic = textOrNull(entry, "topic");
                int freqRank = entry.has("frequencyRank") ? entry.get("frequencyRank").asInt(0) : 0;

                if (lemma == null || spacyPos == null || levelStr == null) continue;

                CefrLevel level = parseCefrLevel(levelStr);
                // F-LABS-R033: C1/C2 se retienen (participan solo de la mal-ubicacion, Grupo D).
                // Solo se descartan niveles no representables en el enum.
                if (level == null) continue;

                // F-LABS-R041: la categoria autoritativa del par lema+POS es la declarada
                // por EVP, traducida al tagset universal. Si EVP no declara categoria (o
                // declara "phrase"/"phrasal verb", fuera del alcance de la traduccion), se
                // conserva la categoria calculada automaticamente como respaldo.
                String posTag = resolveUniversalPos(evpPos, spacyPos);

                LemmaAndPos lp = new LemmaAndPos(lemma, posTag);

                // Detect phrases: multi-word entries. La fuente de verdad de "fila
                // phrasal" es el headword del catalogo con espacios (misma señal que
                // alimenta isPhrase).
                boolean phrasal = word != null && word.contains(" ");
                if (phrasal) {
                    phrases.add(lemma);
                    phrases.add(word);
                }

                // F-LABS-R040: formas derivadas/compuestas cuya palabra encabezado difiere
                // literalmente del lema (por ejemplo "next-door" para el lema "next") no
                // fijan el nivel del lema base. No contienen espacios, por lo que la
                // exclusion de frases no las alcanza; se excluyen aqui con su propio criterio
                // (mutuamente excluyente con phrasal por construccion).
                // Las entradas monopalabra cuya categoria EVP es "phrase"/"phrasal verb"
                // (por ejemplo "let's") quedan fuera de este criterio: su tratamiento esta
                // explicitamente diferido (Doubt[DOUBT-PHRASE-MONOPALABRA], F-LABS-R041) y
                // no son formas derivadas/compuestas en el sentido de R040.
                boolean evpPhraseCategory = evpPos != null
                        && (evpPos.equalsIgnoreCase("phrase") || evpPos.equalsIgnoreCase("phrasal verb"));
                boolean derivedForm = !phrasal && !evpPhraseCategory && word != null && !word.equals(lemma);

                if (!derivedForm) {
                    expectedByLevel.get(level).add(lp);
                }

                // Los indices de consulta de nivel (lookupLevel / lookupLevelByLemma)
                // solo admiten filas palabra-simple, base: una entrada phrasal ("sleep on
                // it", C2, lemma=sleep) o derivada/compuesta ("next-door", B1, lemma=next)
                // no define el nivel del lema suelto — con la precedencia del match exacto
                // (F-LABS-R036 paso 1) contaminaria el scoring con falsos niveles. El
                // filtrado de frases en la cobertura (F-LABS-R005) sigue siendo via
                // isPhrase sobre getExpectedLemmas, que queda intacto.
                if (!phrasal && !derivedForm) {
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

    /**
     * F-LABS-R041: resuelve la categoria gramatical autoritativa de una entrada para el
     * emparejamiento por par lema+categoria. La categoria declarada por EVP tiene
     * precedencia, traducida al tagset universal de {@link #translateEvpPos(String)}.
     * Cuando EVP no declara categoria, o declara una categoria fuera del alcance de la
     * traduccion ("phrase", "phrasal verb", o cualquier valor no reconocido), se conserva
     * como respaldo la categoria calculada automaticamente (comportamiento actual).
     */
    private static String resolveUniversalPos(String evpPartOfSpeech, String spacyPosTag) {
        if (evpPartOfSpeech != null) {
            String translated = translateEvpPos(evpPartOfSpeech);
            if (translated != null) {
                return translated;
            }
        }
        return spacyPosTag;
    }

    /**
     * Traduce la categoria gramatical declarada por EVP al tagset universal usado por
     * los tokens de las oraciones del curso (F-LABS-R041). "phrase" y "phrasal verb"
     * quedan explicitamente fuera de esta traduccion (Doubt[DOUBT-PHRASE-MONOPALABRA]),
     * al igual que cualquier valor no reconocido: en ambos casos se devuelve null para
     * que el llamador conserve la categoria calculada automaticamente.
     */
    private static String translateEvpPos(String evpPartOfSpeech) {
        switch (evpPartOfSpeech.toLowerCase(Locale.ROOT)) {
            case "noun": return "NOUN";
            case "verb": return "VERB";
            case "adjective": return "ADJ";
            case "adverb": return "ADV";
            case "preposition": return "ADP";
            case "pronoun": return "PRON";
            case "determiner": return "DET";
            case "conjunction": return "CCONJ";
            case "exclamation": return "INTJ";
            case "number": return "NUM";
            case "modal verb": return "AUX";
            default: return null;
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
