package com.learney.etl.examples;

import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyContainerWrapper;
import com.learney.etl.transform.vocabulary.analyzer.spacy.ProcessingResult;
import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyProcessingResult;

import java.util.Arrays;
import java.util.List;

/**
 * Example demonstrating how to use SpacyContainerWrapper for sentence processing 
 * with individual word CEFR analysis.
 * 
 * This example shows the new enhanced sentence processing functionality that provides:
 * - Sentence-level CEFR analysis
 * - Individual word-level CEFR analysis
 * - Detailed linguistic information for each word (lemma, POS, etc.)
 */
public class SpacySentenceProcessingExample {

    public static void main(String[] args) {
        System.out.println("=== Enhanced SpaCy Sentence Processing with Individual Word Analysis ===\\n");
        
        SpacyContainerWrapper wrapper = new SpacyContainerWrapper();
        
        // Example sentences with different complexity levels
        List<SpacyContainerWrapper.SentenceToProcess> sentences = Arrays.asList(
            new SpacyContainerWrapper.SentenceToProcess("The cat is sleeping on the bed."),
            new SpacyContainerWrapper.SentenceToProcess("I am currently learning advanced programming techniques."),
            new SpacyContainerWrapper.SentenceToProcess("The implementation of sophisticated algorithms requires comprehensive understanding.")
        );
        
        System.out.println("📝 Processing " + sentences.size() + " sentences...\\n");
        
        ProcessingResult<SpacyProcessingResult> result = wrapper.processSentences(sentences);
        
        if (result.isSuccess()) {
            SpacyProcessingResult spacyResult = result.getData();
            
            System.out.println("✅ Processing completed successfully!");
            System.out.println("📊 Metadata:");
            System.out.printf("   - spaCy version: %s%n", spacyResult.getModel());
            System.out.printf("   - Total sentences: %d%n", spacyResult.getTotalSentenceCount());
            System.out.printf("   - Successfully processed: %d%n", spacyResult.getSuccessfulSentenceCount());
            System.out.println();
            
            // Display detailed analysis for each sentence
            List<SpacyProcessingResult.ProcessedSentence> processedSentences = spacyResult.getProcessedSentences();
            
            for (int i = 0; i < processedSentences.size(); i++) {
                SpacyProcessingResult.ProcessedSentence sentence = processedSentences.get(i);
                
                System.out.printf("🔍 Sentence %d Analysis:%n", i + 1);
                System.out.printf("   Original: \"%s\"%n", sentence.getOriginalSentence());
                System.out.printf("   CEFR Level: %s%n", sentence.getCefrLevel());
                System.out.printf("   Length: %d characters, %d tokens%n", 
                    sentence.getSentenceLength(), sentence.getTokenCount());
                
                // Display CEFR statistics
                if (sentence.getCefrStats() != null) {
                    System.out.printf("   CEFR Stats: Confidence %.2f, Unknown words: %d%n",
                        sentence.getCefrStats().getConfidenceScore(),
                        sentence.getCefrStats().getUnknownWords());
                }
                
                // Display individual word analysis
                if (sentence.getProcessedWords() != null && !sentence.getProcessedWords().isEmpty()) {
                    System.out.println("   📋 Individual Word Analysis:");
                    System.out.printf("   %-15s %-15s %-10s %-8s %-10s %-10s%n", 
                        "Word", "Lemma", "CEFR", "POS", "StopWord", "Confidence");
                    System.out.println("   " + "-".repeat(80));
                    
                    for (SpacyProcessingResult.ProcessedWordInSentence word : sentence.getProcessedWords()) {
                        if (!word.isPunctuation()) { // Skip punctuation for cleaner display
                            System.out.printf("   %-15s %-15s %-10s %-8s %-10s %-10s%n",
                                word.getWord(),
                                word.getLemma(),
                                word.getCefrLevel().length() > 10 ? word.getCefrLevel().substring(0, 10) + "..." : word.getCefrLevel(),
                                word.getPosTag(),
                                word.isStopWord() ? "Yes" : "No",
                                word.getConfidenceScore() != null ? 
                                    String.format("%.2f", word.getConfidenceScore()) : "N/A"
                            );
                        }
                    }
                }
                
                System.out.println();
            }
            
            // Summary statistics
            System.out.println("📈 Summary Statistics:");
            int totalWords = processedSentences.stream()
                .mapToInt(s -> s.getProcessedWords() != null ? 
                    (int) s.getProcessedWords().stream().filter(w -> !w.isPunctuation()).count() : 0)
                .sum();
            
            long stopWords = processedSentences.stream()
                .flatMap(s -> s.getProcessedWords() != null ? s.getProcessedWords().stream() : null)
                .filter(w -> w != null && !w.isPunctuation() && w.isStopWord())
                .count();
            
            System.out.printf("   - Total content words analyzed: %d%n", totalWords);
            System.out.printf("   - Stop words: %d (%.1f%%)%n", stopWords, totalWords > 0 ? (stopWords * 100.0 / totalWords) : 0);
            
        } else {
            System.err.println("❌ Processing failed: " + result.getErrorMessage());
            if (result.getException() != null) {
                result.getException().printStackTrace();
            }
        }
    }
} 