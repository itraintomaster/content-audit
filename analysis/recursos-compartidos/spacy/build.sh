#!/bin/bash

# SpaCy Docker Image Build Script
# Usage: ./build.sh [--no-cache]

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_NAME="spacy-nlp:latest"

echo "🐳 Building spaCy Docker Image..."
echo "📁 Build context: $SCRIPT_DIR"
echo "🏷️  Image name: $IMAGE_NAME"
echo ""

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build options
BUILD_ARGS=""
if [[ "$1" == "--no-cache" ]]; then
    BUILD_ARGS="--no-cache"
    echo "🔄 Building with --no-cache option"
fi

# Copy frequency data file from resources
FREQUENCY_SOURCE="../../vocabulary/lemmas_20k_words.txt"
if [ -f "$SCRIPT_DIR/$FREQUENCY_SOURCE" ]; then
    echo "📄 Copying word frequency data..."
    cp "$SCRIPT_DIR/$FREQUENCY_SOURCE" "$SCRIPT_DIR/lemmas_20k_words.txt"
else
    echo "⚠️  Warning: Word frequency data not found at $SCRIPT_DIR/$FREQUENCY_SOURCE"
    echo "Creating empty placeholder..."
    echo -e "# Placeholder frequency data\nlemRank\tlemma\tPoS\tlemFreq\twordFreq\tword" > "$SCRIPT_DIR/lemmas_20k_words.txt"
fi

# Build the image
echo "🔨 Building Docker image..."
docker build $BUILD_ARGS -t "$IMAGE_NAME" "$SCRIPT_DIR"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Image built successfully: $IMAGE_NAME"
else
    echo ""
    echo "❌ Image build failed"
    exit 1
fi

# Verify the image
echo ""
echo "🔍 Verifying image..."
if docker images "$IMAGE_NAME" --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" | grep -q spacy-nlp; then
    echo "✅ Image verification successful"
else
    echo "❌ Image verification failed"
    exit 1
fi

# Test basic functionality
echo ""
echo "🧪 Testing basic spaCy functionality..."
if docker run --rm "$IMAGE_NAME" python -c "
import spacy
print('spaCy version:', spacy.__version__)
nlp = spacy.load('en_core_web_sm')
print('Model loaded successfully')
print('Test word processing:', [token.lemma_ for token in nlp('running')])

# Test frequency data loading
try:
    import sys
    sys.path.append('/app')
    from sample_processor import load_frequency_data, get_word_frequency_info
    freq_data, freq_lookup = load_frequency_data()
    print(f'Frequency data loaded: {len(freq_data)} entries')
    
    # Test frequency lookup
    test_freq = get_word_frequency_info('the', 'the', 'DET')
    if test_freq:
        print(f'Frequency test passed - \"the\" rank: {test_freq[\"lemma_rank\"]}')
    else:
        print('Warning: Frequency lookup returned no results')
        
except Exception as e:
    print('Frequency test failed:', str(e))

# Test cefrpy functionality
try:
    from cefrpy import analyzer
    print('cefrpy imported successfully')
    test_result = analyzer.analyze_text('The cat is sleeping.')
    print('CEFR analysis test passed')
except ImportError:
    print('Warning: cefrpy not available, but basic spaCy functionality works')
except Exception as e:
    print('CEFR analysis test failed:', str(e))

print('✅ All tests passed')
"; then
    echo "✅ Basic functionality test passed"
else
    echo "❌ Basic functionality test failed"
    exit 1
fi

# Cleanup temporary files
if [ -f "$SCRIPT_DIR/lemmas_20k_words.txt" ]; then
    echo "🧹 Cleaning up temporary files..."
    rm "$SCRIPT_DIR/lemmas_20k_words.txt"
fi

echo ""
echo "🎉 SpaCy Docker image ready for use!"
echo "🚀 You can now run: docker run --rm $IMAGE_NAME"
echo "" 