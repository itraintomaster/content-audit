# SpaCy Container Wrapper

## Overview

The SpaCy Container Wrapper provides a Docker-based solution for processing English text using spaCy NLP library with additional CEFR (Common European Framework of Reference) level analysis capabilities.

## Features

### Word Processing
- Process individual words with spaCy linguistic analysis
- Extract lemmas, POS tags, and morphological features
- Handle batch processing with error resilience

### Enhanced Sentence Processing with Individual Word Analysis
- **Sentence-level CEFR analysis**: Get overall CEFR level estimation for complete sentences
- **Individual word-level CEFR analysis**: Each word in the sentence receives its own CEFR level
- **Comprehensive linguistic information** for each word:
  - Word and lemma forms
  - Part-of-speech (POS) tags
  - CEFR level classification
  - Position within the sentence
  - Stop word identification
  - Punctuation classification
  - Confidence scores
  - Frequency rankings (extensible)

### Detailed Statistics
- Sentence-level confidence scores
- Level distribution counts
- Unknown word identification
- Processing metadata and timing

## Usage

### Basic Sentence Processing with Individual Word Analysis

```java
import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyContainerWrapper;
import com.learney.etl.transform.vocabulary.analyzer.spacy.ProcessingResult;
import com.learney.etl.transform.vocabulary.analyzer.spacy.SpacyProcessingResult;

SpacyContainerWrapper wrapper = new SpacyContainerWrapper();

List<SpacyContainerWrapper.SentenceToProcess> sentences = Arrays.asList(
    new SpacyContainerWrapper.SentenceToProcess("The cat is sleeping."),
    new SpacyContainerWrapper.SentenceToProcess("I am learning programming.")
);

ProcessingResult<SpacyProcessingResult> result = wrapper.processSentences(sentences);

if (result.isSuccess()) {
    SpacyProcessingResult data = result.getData();
    
    for (SpacyProcessingResult.ProcessedSentence sentence : data.getProcessedSentences()) {
        System.out.println("Sentence: " + sentence.getOriginalSentence());
        System.out.println("CEFR Level: " + sentence.getCefrLevel());
        
        // Access individual word analysis
        for (SpacyProcessingResult.ProcessedWordInSentence word : sentence.getProcessedWords()) {
            System.out.printf("Word: %s, Lemma: %s, CEFR: %s, POS: %s%n",
                word.getWord(), word.getLemma(), word.getCefrLevel(), word.getPosTag());
        }
    }
}
```

### Word Processing (Existing Functionality)

```java
List<SpacyContainerWrapper.WordToProcess> words = Arrays.asList(
    new SpacyContainerWrapper.WordToProcess("running", "A1"),
    new SpacyContainerWrapper.WordToProcess("sophisticated", "C1")
);

ProcessingResult<SpacyProcessingResult> result = wrapper.processSample(words);
```

## Data Structures

### ProcessedSentence
Contains comprehensive sentence-level analysis:
- `originalSentence`: The input sentence
- `cefrLevel`: Overall CEFR level estimation
- `cefrStats`: Detailed statistical analysis
- `lemmas`: List of lemmatized tokens
- `posTags`: Part-of-speech tags
- `processedWords`: Individual word analysis (NEW)
- `sentenceLength`: Character count
- `tokenCount`: Token count

### ProcessedWordInSentence (NEW)
Individual word analysis within sentence context:
- `word`: Original word form
- `lemma`: Lemmatized form
- `cefrLevel`: CEFR level for this specific word
- `posTag`: Part-of-speech tag
- `wordPosition`: Position index in sentence
- `isPunctuation`: Whether the token is punctuation
- `isStopWord`: Whether the word is a stop word
- `frequencyRank`: Word frequency ranking (extensible)
- `confidenceScore`: CEFR classification confidence

### CefrStats
Statistical analysis of CEFR levels:
- `levelCounts`: Distribution of words across CEFR levels
- `dominantLevel`: Most frequent CEFR level
- `confidenceScore`: Overall confidence score
- `totalAnalyzedWords`: Count of analyzed words
- `unknownWords`: Count of words without CEFR classification

## Docker Image

### Building the Image

```bash
cd src/main/resources/docker/spacy
./build.sh
```

### Manual Docker Usage

```bash
# Prepare input data
echo '{"sentences": ["The cat is sleeping.", "Programming is fun."]}' > input.json

# Run processing
docker run --rm -v $(pwd):/tmp/spacy-data spacy-nlp:latest python /app/sample_processor.py

# View results
cat output.json | jq '.processed_sentences[0].processed_words'
```

## Dependencies

- **spaCy**: Natural language processing library
- **cefrpy**: CEFR level analysis (optional, graceful fallback if unavailable)
- **en_core_web_sm**: English language model

## Error Handling

The wrapper provides robust error handling:
- Graceful fallback when CEFR analysis is unavailable
- Per-sentence error isolation
- Detailed error messages and stack traces
- Comprehensive logging

## Performance Considerations

- Docker container overhead: ~2-3 seconds startup time
- Processing speed: ~10-50 sentences per second (depending on complexity)
- Memory usage: ~200-500MB per container instance
- Batch processing recommended for optimal throughput

## Examples

See `SpacySentenceProcessingExample.java` for a complete working example demonstrating:
- Sentence processing with multiple complexity levels
- Individual word analysis display
- Statistical summaries
- Error handling patterns

## Limitations

- Currently supports English text only
- CEFR analysis depends on cefrpy availability
- Docker is required for execution
- Processing is synchronous (async support could be added)

## Future Enhancements

- Multi-language support
- Async processing capabilities
- Enhanced frequency data integration
- Real-time CEFR model updates
- Custom vocabulary integration 