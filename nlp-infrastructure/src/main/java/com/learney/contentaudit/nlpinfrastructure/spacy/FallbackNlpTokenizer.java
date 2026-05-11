package com.learney.contentaudit.nlpinfrastructure.spacy;

import com.learney.contentaudit.auditdomain.NlpToken;
import com.learney.contentaudit.auditdomain.NlpTokenizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// R030: when SpaCy is unavailable, returns whitespace-split basic tokens instead of throwing,
// so count-only analyzers (e.g. SentenceLengthAnalyzer) continue to work.
class FallbackNlpTokenizer implements NlpTokenizer {

    private final NlpTokenizer primary;

    FallbackNlpTokenizer(NlpTokenizer primary) {
        this.primary = primary;
    }

    @Override
    public List<String> tokenize(String sentence) {
        try {
            return primary.tokenize(sentence);
        } catch (RuntimeException e) {
            if (sentence == null || sentence.isBlank()) return List.of();
            return Arrays.asList(sentence.split("\\s+"));
        }
    }

    @Override
    public List<NlpToken> analyzeTokens(String sentence) {
        try {
            return primary.analyzeTokens(sentence);
        } catch (RuntimeException e) {
            return toBasicTokens(sentence);
        }
    }

    @Override
    public Map<String, List<NlpToken>> analyzeTokensBatch(List<String> sentences) {
        try {
            return primary.analyzeTokensBatch(sentences);
        } catch (RuntimeException e) {
            return sentences.stream()
                    .collect(Collectors.toMap(s -> s, this::toBasicTokens,
                            (a, b) -> a, java.util.LinkedHashMap::new));
        }
    }

    @Override
    public int countTokens(String sentence) {
        return analyzeTokens(sentence).size();
    }

    private List<NlpToken> toBasicTokens(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return List.of();
        }
        return Arrays.stream(sentence.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .map(text -> new NlpToken(text, null, null, null, false, false))
                .toList();
    }
}
