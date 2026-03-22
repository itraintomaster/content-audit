package com.learney.etl.examples;

import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyContainerWrapper;
import com.learney.etl.transform.vocabulary.analyzer.spacy.ProcessingResult;
import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyProcessingResult;

import java.util.Arrays;
import java.util.List;

/**
 * Example demonstrating how to use SpacyContainerWrapper for sentence processing with CEFR analysis.
 * 
 * This example shows both word processing and the new sentence processing functionality.
 */
public class SpacySentenceProcessingExample {

    public static void main(String[] args) {
        System.out.println("=== SpaCy Container Wrapper Example ===\n");
        
        // Example 1: Word Processing (existing functionality)
        demonstrateWordProcessing();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Example 2: Sentence Processing with CEFR Analysis (new functionality)
        demonstrateSentenceProcessing();
    }

    private static void demonstrateWordProcessing() {
        System.out.println("📝 WORD PROCESSING EXAMPLE");
        System.out.println("-".repeat(30));
        
        try (SpacyContainerWrapper wrapper = new SpacyContainerWrapper()) {
            List<SpacyContainerWrapper.WordToProcess> words = Arrays.asList(
                new SpacyContainerWrapper.WordToProcess("running", "A1"),
                new SpacyContainerWrapper.WordToProcess("beautiful", "A1"),
                new SpacyContainerWrapper.WordToProcess("sophisticated", "C1")
            );
            
            ProcessingResult<SpacyProcessingResult> result = wrapper.processSample(words);
            
            if (result.isSuccess()) {
                SpacyProcessingResult data = result.getData();
                System.out.println("✅ Successfully processed " + data.getSuccessfulWordCount() + " words\n");
                
                for (SpacyProcessingResult.ProcessedWord word : data.getProcessedWords()) {
                    System.out.println("Word: " + word.getOriginalWord());
                    System.out.println("  Lemma: " + word.getLemma());
                    System.out.println("  POS: " + word.getPos());
                    System.out.println("  Level: " + word.getLevel());
                    System.out.println("  Is Alpha: " + word.isAlpha());
                    System.out.println();
                }
            } else {
                System.out.println("❌ Word processing failed: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    private static void demonstrateSentenceProcessing() {
        System.out.println("📝 SENTENCE PROCESSING WITH CEFR ANALYSIS");
        System.out.println("-".repeat(45));
        
        try (SpacyContainerWrapper wrapper = new SpacyContainerWrapper()) {
            List<SpacyContainerWrapper.SentenceToProcess> sentences = Arrays.asList(
                new SpacyContainerWrapper.SentenceToProcess("The cat is sleeping on the bed."),
                new SpacyContainerWrapper.SentenceToProcess("She ran quickly to the store and bought some fresh apples."),
                new SpacyContainerWrapper.SentenceToProcess("I am learning about natural language processing techniques."),
                new SpacyContainerWrapper.SentenceToProcess("Ubiquitous computing paradigms necessitate robust cryptographic protocols to ensure data integrity.")
            );
            
            ProcessingResult<SpacyProcessingResult> result = wrapper.processSentences(sentences);
            
            if (result.isSuccess()) {
                SpacyProcessingResult data = result.getData();
                System.out.println("✅ Successfully processed " + data.getSuccessfulSentenceCount() + " sentences\n");
                
                for (SpacyProcessingResult.ProcessedSentence sentence : data.getProcessedSentences()) {
                    System.out.println("📄 Sentence: \"" + sentence.getOriginalSentence() + "\"");
                    System.out.println("   🎯 CEFR Level: " + sentence.getCefrLevel());
                    System.out.println("   📊 Token Count: " + sentence.getTokenCount());
                    System.out.println("   📝 Length: " + sentence.getSentenceLength() + " characters");
                    System.out.println("   🔤 Lemmas: " + sentence.getLemmas());
                    System.out.println("   🏷️  POS Tags: " + sentence.getPosTags());
                    
                    // CEFR Statistics
                    SpacyProcessingResult.CefrStats stats = sentence.getCefrStats();
                    if (stats != null) {
                        System.out.println("   📈 CEFR Statistics:");
                        System.out.println("      • Dominant Level: " + stats.getDominantLevel());
                        System.out.println("      • Confidence: " + String.format("%.2f", stats.getConfidenceScore()));
                        System.out.println("      • Total Analyzed Words: " + stats.getTotalAnalyzedWords());
                        System.out.println("      • Unknown Words: " + stats.getUnknownWords());
                        System.out.println("      • Level Distribution: " + stats.getLevelCounts());
                    }
                    
                    if (sentence.getError() != null) {
                        System.out.println("   ⚠️  Error: " + sentence.getError());
                    }
                    
                    System.out.println();
                }
                
                // Summary statistics
                System.out.println("📊 PROCESSING SUMMARY");
                System.out.println("-".repeat(20));
                System.out.println("Total sentences: " + data.getProcessedSentences().size());
                System.out.println("Successful: " + data.getSuccessfulSentenceCount());
                System.out.println("Failed: " + data.getFailedSentenceCount());
                
            } else {
                System.out.println("❌ Sentence processing failed: " + result.getErrorMessage());
                System.out.println("   Result type: " + result.getResultType());
                
                if (result.getException() != null) {
                    System.out.println("   Exception: " + result.getException().getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 