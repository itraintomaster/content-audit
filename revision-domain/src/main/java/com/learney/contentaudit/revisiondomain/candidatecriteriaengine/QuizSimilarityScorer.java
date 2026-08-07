package com.learney.contentaudit.revisiondomain.candidatecriteriaengine;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Small deterministic similarity helper shared by {@link DistinctnessCriterion}
 * (siblings, compared fully -- DOUBT-DISTINCION-ALCANCE opcion A) and {@link
 * AuditReportQuizCorpus} (rest of the course, compared approximately -- same
 * doubt). Not a Sentinel-governed component -- a plain internal utility, same
 * status as e.g. {@code SuggestedLemmaTierRanker} in refiner-domain.
 *
 * <p>Jaccard similarity over lower-cased word tokens: cheap, deterministic,
 * order-independent, and good enough to catch the gross near-duplicates this
 * criterion exists to block, without the cost of a real diff algorithm.
 */
final class QuizSimilarityScorer {

    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");

    private QuizSimilarityScorer() {
    }

    static double similarity(String a, String b) {
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);
        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / (double) union.size();
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String word : WORD_SPLIT.split(text.toLowerCase())) {
            if (!word.isBlank()) {
                tokens.add(word);
            }
        }
        return tokens;
    }
}
