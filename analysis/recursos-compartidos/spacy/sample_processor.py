#!/usr/bin/env python3
"""
SpaCy Sample Processor
Processes sample words and generates enriched JSON output with CEFR and word frequency analysis.
"""

import json
import sys
import spacy
import traceback
from datetime import datetime
from pathlib import Path
import csv

# Global frequency data cache
FREQUENCY_DATA = None
FREQUENCY_LOOKUP = None

def load_frequency_data():
    """Load COCA word frequency data from lemmas_20k_words.txt"""
    global FREQUENCY_DATA, FREQUENCY_LOOKUP
    
    if FREQUENCY_DATA is not None:
        return FREQUENCY_DATA, FREQUENCY_LOOKUP
    
    frequency_file = "/app/data/lemmas_20k_words.txt"
    frequency_data = []
    frequency_lookup = {}
    
    try:
        print(f"Loading word frequency data from: {frequency_file}", file=sys.stderr)
        
        with open(frequency_file, 'r', encoding='utf-8') as f:
            # Skip header lines (first 8 lines are metadata, line 9 is the column header)
            for i in range(9):
                next(f)
            
            # Read tab-separated data
            reader = csv.reader(f, delimiter='\t')
            
            for line_num, row in enumerate(reader, start=10):
                try:
                    if len(row) >= 6:
                        lemma_rank = int(row[0])
                        lemma = row[1].strip().lower()
                        pos = row[2].strip()
                        lemma_freq = int(row[3])
                        word_freq = int(row[4])
                        word = row[5].strip().lower()
                        
                        entry = {
                            'lemma_rank': lemma_rank,
                            'lemma': lemma,
                            'pos': pos,
                            'lemma_freq': lemma_freq,
                            'word_freq': word_freq,
                            'word': word
                        }
                        
                        frequency_data.append(entry)
                        
                        # Create lookup keys for lemma+POS and just lemma
                        lemma_pos_key = f"{lemma}#{pos}"
                        
                        # Store by lemma+POS (most precise)
                        if lemma_pos_key not in frequency_lookup:
                            frequency_lookup[lemma_pos_key] = entry
                        elif entry['lemma_rank'] < frequency_lookup[lemma_pos_key]['lemma_rank']:
                            # Keep the entry with better (lower) rank
                            frequency_lookup[lemma_pos_key] = entry
                        
                        # Store by lemma only (fallback)
                        if lemma not in frequency_lookup:
                            frequency_lookup[lemma] = entry
                        elif entry['lemma_rank'] < frequency_lookup[lemma]['lemma_rank']:
                            frequency_lookup[lemma] = entry
                        
                        # Store by exact word form
                        if word not in frequency_lookup:
                            frequency_lookup[word] = entry
                        elif entry['word_freq'] > frequency_lookup[word]['word_freq']:
                            # For exact words, prefer higher word frequency
                            frequency_lookup[word] = entry
                            
                except (ValueError, IndexError) as e:
                    print(f"Warning: Skipping malformed line {line_num}: {e}", file=sys.stderr)
                    continue
        
        print(f"Loaded {len(frequency_data)} frequency entries", file=sys.stderr)
        FREQUENCY_DATA = frequency_data
        FREQUENCY_LOOKUP = frequency_lookup
        
    except FileNotFoundError:
        print(f"Warning: Frequency data file not found: {frequency_file}", file=sys.stderr)
        FREQUENCY_DATA = []
        FREQUENCY_LOOKUP = {}
    except Exception as e:
        print(f"Warning: Error loading frequency data: {e}", file=sys.stderr)
        FREQUENCY_DATA = []
        FREQUENCY_LOOKUP = {}
    
    return FREQUENCY_DATA, FREQUENCY_LOOKUP

def map_spacy_pos_to_coca_pos(spacy_pos):
    """Map spaCy POS tags to COCA POS tags for frequency lookup"""
    mapping = {
        # Core content words
        'NOUN': 'n',        # common noun -> n (noun)
        'PROPN': 'n',       # proper noun -> n (noun)
        'VERB': 'v',        # verb -> v (verb)
        'AUX': 'v',         # auxiliary verb -> v (verb)
        'ADJ': 'j',         # adjective -> j (adjective)
        'ADV': 'r',         # adverb -> r (adverb)
        
        # Function words
        'PRON': 'p',        # pronoun -> p (pronoun)
        'DET': 'a',         # determiner -> a (article/determiner) - FIXED: was 'd'
        'ADP': 'i',         # adposition/preposition -> i (preposition)
        'CONJ': 'c',        # conjunction -> c (conjunction)
        'CCONJ': 'c',       # coordinating conjunction -> c (conjunction)
        'SCONJ': 'c',       # subordinating conjunction -> c (conjunction)
        
        # Numbers and particles
        'NUM': 'm',         # numeral -> m (number)
        'PART': 't',        # particle -> t (particle)
        'PRT': 't',         # particle -> t (particle)
        
        # Other categories
        'INTJ': 'u',        # interjection -> u (interjection)
        'SYM': 'x',         # symbol -> x (not/negation)
        'PUNCT': 'x',       # punctuation -> x (not/negation)
        'SPACE': 'x',       # space -> x (not/negation)
        'X': 'x',           # other -> x (not/negation)
        
        # Additional mappings for potential edge cases
        '_SP': 'x',         # space -> x
        'EOL': 'x',         # end of line -> x
        'NIL': 'x'          # nil -> x
    }
    return mapping.get(spacy_pos, 'x')

def get_word_frequency_info(word, lemma, spacy_pos):
    """Get frequency information for a word based on multiple lookup strategies"""
    _, frequency_lookup = load_frequency_data()
    
    if not frequency_lookup:
        return None
    
    word_lower = word.lower()
    lemma_lower = lemma.lower()
    coca_pos = map_spacy_pos_to_coca_pos(spacy_pos)
    
    # Strategy 1: Try exact word match first
    if word_lower in frequency_lookup:
        return frequency_lookup[word_lower]
    
    # Strategy 2: Try lemma + POS combination
    lemma_pos_key = f"{lemma_lower}#{coca_pos}"
    if lemma_pos_key in frequency_lookup:
        return frequency_lookup[lemma_pos_key]
    
    # Strategy 3: Try lemma only (fallback)
    if lemma_lower in frequency_lookup:
        return frequency_lookup[lemma_lower]
    
    return None

def load_spacy_model():
    """Load the spaCy English model."""
    try:
        nlp = spacy.load("en_core_web_sm")
        return nlp
    except Exception as e:
        print(f"Error loading spaCy model: {e}", file=sys.stderr)
        sys.exit(1)

def process_words(nlp, words):
    """Process list of words with spaCy and extract linguistic features including frequency data."""
    processed_words = []
    
    for word_entry in words:
        word = word_entry.get("word", "").strip()
        level = word_entry.get("level", "unknown")
        
        if not word:
            continue
            
        try:
            # Process with spaCy
            doc = nlp(word)
            token = doc[0] if len(doc) > 0 else None
            
            if token:
                # Get frequency information
                freq_info = get_word_frequency_info(token.text, token.lemma_, token.pos_)
                frequency_rank = freq_info['lemma_rank'] if freq_info else None
                
                processed_word = {
                    "original_word": word,
                    "level": level,
                    "lemma": token.lemma_,
                    "pos_": token.pos_,
                    "tag_": token.tag_,
                    "is_alpha": token.is_alpha,
                    "is_stop": token.is_stop,
                    "is_oov": token.is_oov,
                    "frequency_rank": frequency_rank,
                    "frequency_info": freq_info
                }
                processed_words.append(processed_word)
                
        except Exception as e:
            print(f"Error processing word '{word}': {e}", file=sys.stderr)
            # Add basic fallback entry
            processed_words.append({
                "original_word": word,
                "level": level,
                "lemma": word.lower(),
                "pos_": "UNKNOWN",
                "tag_": "UNKNOWN",
                "is_alpha": word.isalpha(),
                "is_stop": False,
                "is_oov": True,
                "frequency_rank": None,
                "frequency_info": None,
                "error": str(e)
            })
    
    return processed_words

def get_cefr_level_and_lemmas(nlp, sentences):
    """
    Processes a list of English sentences, returns lemmas and attempts to
    estimate the CEFR level for each sentence.

    Args:
        nlp: spaCy language model
        sentences (list): A list of strings, where each string is an English sentence.

    Returns:
        list: A list of dictionaries, where each dictionary contains
              'original_sentence', 'lemmas', 'cefr_level', 'cefr_stats', 
              'processed_words', and other metadata for the corresponding input sentence.
    """
    try:
        import cefrpy
        analyzer = cefrpy.CEFRAnalyzer()
    except ImportError:
        print("Warning: cefrpy not available, falling back to basic analysis", file=sys.stderr)
        return get_basic_sentence_analysis(nlp, sentences)
    except Exception as e:
        print(f"Warning: Error initializing cefrpy analyzer: {e}, falling back to basic analysis", file=sys.stderr)
        return get_basic_sentence_analysis(nlp, sentences)
    
    results = []
    for sentence in sentences:
        try:
            doc = nlp(sentence)
            
            # Extract lemmas and POS tags (exclude punctuation and spaces)
            lemmas = []
            pos_tags = []
            processed_words = []
            word_cefr_levels = []
            
            for i, token in enumerate(doc):
                if not token.is_punct and not token.is_space:
                    lemmas.append(token.lemma_)
                    pos_tags.append(token.pos_)
                
                # Process each word individually for CEFR analysis
                word_cefr_level = "N/A"
                confidence_score = None
                
                # Try to get CEFR level for individual word
                try:
                    if not token.is_punct and not token.is_space and len(token.text.strip()) > 0:
                        word_lower = token.text.lower()
                        
                        if analyzer.is_word_in_database(word_lower):
                            # Get CEFR level specific to the POS tag if available
                            spacy_to_penn_pos = {
                                'NOUN': 'NN', 'VERB': 'VB', 'ADJ': 'JJ', 'ADV': 'RB',
                                'PRON': 'PRP', 'DET': 'DT', 'ADP': 'IN', 'NUM': 'CD',
                                'CONJ': 'CC', 'PRT': 'RP', 'AUX': 'VB', 'PROPN': 'NNP',
                                'INTJ': 'UH', 'PUNCT': '.', 'SPACE': 'SP', 'X': 'FW'
                            }
                            
                            penn_pos = spacy_to_penn_pos.get(token.pos_, None)
                            
                            # Try POS-specific level first, then fall back to average
                            if penn_pos:
                                pos_level = analyzer.get_word_pos_level_CEFR(word_lower, penn_pos)
                                if pos_level:
                                    word_cefr_level = str(pos_level)
                                    confidence_score = 0.9  # High confidence for exact POS match
                            
                            # Fall back to average level if POS-specific not found
                            if word_cefr_level == "N/A":
                                avg_level = analyzer.get_average_word_level_CEFR(word_lower)
                                if avg_level:
                                    word_cefr_level = str(avg_level)
                                    confidence_score = 0.7  # Medium confidence for average level
                        else:
                            word_cefr_level = "Unknown"
                            
                except Exception as e:
                    print(f"Warning: Could not analyze CEFR for word '{token.text}': {e}", file=sys.stderr)
                    word_cefr_level = "Error"
                
                # Get frequency information for this word
                freq_info = get_word_frequency_info(token.text, token.lemma_, token.pos_)
                frequency_rank = freq_info['lemma_rank'] if freq_info else None
                
                # Add word info regardless of punctuation for completeness
                processed_words.append({
                    "word": token.text,
                    "lemma": token.lemma_,
                    "cefr_level": word_cefr_level,
                    "pos_tag": token.pos_,
                    "dep": token.dep_,
                    "head_position": token.head.i,
                    "word_position": i,
                    "is_punctuation": token.is_punct,
                    "is_stop_word": token.is_stop,
                    "frequency_rank": frequency_rank,
                    "confidence_score": confidence_score,
                    "frequency_info": freq_info
                })
                
                # Collect CEFR levels for sentence-level analysis
                if not token.is_punct and not token.is_space and word_cefr_level not in ["N/A", "Unknown", "Error"]:
                    word_cefr_levels.append(word_cefr_level)
            
            # Analyze sentence-level CEFR based on individual word levels
            try:
                if word_cefr_levels:
                    # Count occurrences of each CEFR level
                    level_counts = {}
                    for level in word_cefr_levels:
                        level_counts[level] = level_counts.get(level, 0) + 1
                    
                    # Find dominant level
                    dominant_cefr = max(level_counts.keys(), key=level_counts.get)
                    total_words = len(word_cefr_levels)
                    confidence = level_counts[dominant_cefr] / total_words if total_words > 0 else 0.0
                    
                    # Calculate unknown words
                    unknown_words = len([w for w in processed_words 
                                       if not w["is_punctuation"] and w["cefr_level"] in ["N/A", "Unknown", "Error"]])
                else:
                    dominant_cefr = "Unknown"
                    level_counts = {}
                    confidence = 0.0
                    unknown_words = len([w for w in processed_words if not w["is_punctuation"]])
                
                detailed_cefr_stats = {
                    "level_counts": level_counts,
                    "dominant_level": dominant_cefr,
                    "confidence_score": confidence,
                    "total_analyzed_words": len(word_cefr_levels),
                    "unknown_words": unknown_words
                }
                
            except Exception as e:
                print(f"Warning: Could not analyze sentence CEFR for '{sentence}': {e}", file=sys.stderr)
                dominant_cefr = "Error analyzing CEFR"
                detailed_cefr_stats = {
                    "level_counts": {},
                    "dominant_level": "N/A",
                    "confidence_score": 0.0,
                    "total_analyzed_words": 0,
                    "unknown_words": len([w for w in processed_words if not w["is_punctuation"]])
                }
            
            result = {
                "original_sentence": sentence,
                "lemmas": lemmas,
                "cefr_level": dominant_cefr,
                "cefr_stats": detailed_cefr_stats,
                "pos_tags": pos_tags,
                "sentence_length": len(sentence),
                "token_count": len([t for t in doc if not t.is_punct and not t.is_space]),
                "processed_words": processed_words,
                "processing_time_ms": None  # Could be enhanced with timing
            }
            
            results.append(result)
            
        except Exception as e:
            print(f"Error processing sentence '{sentence}': {e}", file=sys.stderr)
            results.append({
                "original_sentence": sentence,
                "lemmas": [],
                "cefr_level": "Error",
                "cefr_stats": {
                    "level_counts": {},
                    "dominant_level": "N/A",
                    "confidence_score": 0.0,
                    "total_analyzed_words": 0,
                    "unknown_words": 0
                },
                "pos_tags": [],
                "sentence_length": len(sentence),
                "token_count": 0,
                "processed_words": [],
                "error": str(e)
            })
    
    return results

def get_basic_sentence_analysis(nlp, sentences):
    """
    Fallback analysis without cefrpy - provides basic linguistic analysis.
    """
    results = []
    for sentence in sentences:
        try:
            doc = nlp(sentence)
            
            # Extract lemmas and POS tags (exclude punctuation and spaces)
            lemmas = []
            pos_tags = []
            processed_words = []
            
            for i, token in enumerate(doc):
                if not token.is_punct and not token.is_space:
                    lemmas.append(token.lemma_)
                    pos_tags.append(token.pos_)
                
                # Get frequency information for this word
                freq_info = get_word_frequency_info(token.text, token.lemma_, token.pos_)
                frequency_rank = freq_info['lemma_rank'] if freq_info else None
                
                # Process each word without CEFR analysis
                processed_words.append({
                    "word": token.text,
                    "lemma": token.lemma_,
                    "cefr_level": "N/A (cefrpy not available)",
                    "pos_tag": token.pos_,
                    "dep": token.dep_,
                    "head_position": token.head.i,
                    "word_position": i,
                    "is_punctuation": token.is_punct,
                    "is_stop_word": token.is_stop,
                    "frequency_rank": frequency_rank,
                    "confidence_score": None,
                    "frequency_info": freq_info
                })
            
            result = {
                "original_sentence": sentence,
                "lemmas": lemmas,
                "cefr_level": "N/A (cefrpy not available)",
                "cefr_stats": {
                    "level_counts": {},
                    "dominant_level": "N/A",
                    "confidence_score": 0.0,
                    "total_analyzed_words": 0,
                    "unknown_words": len([w for w in processed_words if not w["is_punctuation"]])
                },
                "pos_tags": pos_tags,
                "sentence_length": len(sentence),
                "token_count": len([t for t in doc if not t.is_punct and not t.is_space]),
                "processed_words": processed_words,
                "processing_time_ms": None
            }
            
            results.append(result)
            
        except Exception as e:
            print(f"Error processing sentence '{sentence}': {e}", file=sys.stderr)
            results.append({
                "original_sentence": sentence,
                "lemmas": [],
                "cefr_level": "Error",
                "cefr_stats": {
                    "level_counts": {},
                    "dominant_level": "N/A",
                    "confidence_score": 0.0,
                    "total_analyzed_words": 0,
                    "unknown_words": 0
                },
                "pos_tags": [],
                "sentence_length": len(sentence),
                "token_count": 0,
                "processed_words": [],
                "error": str(e)
            })
    
    return results

def main():
    """Main processing function."""
    if len(sys.argv) < 3:
        input_file = "/tmp/spacy-data/input.json"
        output_file = "/tmp/spacy-data/output.json"
    else:
        input_file = sys.argv[1]
        output_file = sys.argv[2]
    
    try:
        # Load spaCy model
        print("Loading spaCy model...", file=sys.stderr)
        nlp = load_spacy_model()
        
        # Read input data
        print(f"Reading input from: {input_file}", file=sys.stderr)
        input_path = Path(input_file)
        
        if not input_path.exists():
            raise FileNotFoundError(f"Input file not found: {input_file}")
        
        with open(input_path, 'r', encoding='utf-8') as f:
            input_data = json.load(f)
        
        sample_words = input_data.get("sample_words", [])
        sentences = input_data.get("sentences", [])
        
        processed_words = []
        processed_sentences = []
        
        # Process words if they exist
        if sample_words:
            print(f"Processing {len(sample_words)} words...", file=sys.stderr)
            processed_words = process_words(nlp, sample_words)
        
        # Process sentences if they exist
        if sentences:
            print(f"Processing {len(sentences)} sentences...", file=sys.stderr)
            processed_sentences = get_cefr_level_and_lemmas(nlp, sentences)
        
        # Prepare output
        output_data = {
            "metadata": {
                "processed_at": datetime.now().isoformat(),
                "spacy_version": spacy.__version__,
                "model": "en_core_web_sm",
                "total_words": len(sample_words),
                "successful_words": len([w for w in processed_words if "error" not in w]),
                "failed_words": len([w for w in processed_words if "error" in w]),
                "total_sentences": len(sentences),
                "successful_sentences": len([s for s in processed_sentences if "error" not in s]),
                "failed_sentences": len([s for s in processed_sentences if "error" in s])
            },
            "processed_words": processed_words,
            "processed_sentences": processed_sentences
        }
        
        # Write output
        print(f"Writing output to: {output_file}", file=sys.stderr)
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(output_data, f, indent=2, ensure_ascii=False)
        
        print(f"Successfully processed {len(processed_words)} words and {len(processed_sentences)} sentences", file=sys.stderr)
        
    except Exception as e:
        print(f"Processing failed: {e}", file=sys.stderr)
        print(traceback.format_exc(), file=sys.stderr)
        
        # Create error output
        error_output = {
            "metadata": {
                "processed_at": datetime.now().isoformat(),
                "status": "error",
                "error_message": str(e)
            },
            "processed_words": [],
            "processed_sentences": []
        }
        
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(error_output, f, indent=2)
        except:
            pass
        
        sys.exit(1)

if __name__ == "__main__":
    main() 