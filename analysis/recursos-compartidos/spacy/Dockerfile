# SpaCy NLP Processing Container
# Base image: Python 3.11 slim for better performance
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
RUN pip install --no-cache-dir \
    spacy==3.7.2 \
    cefrpy \
    && python -m spacy download en_core_web_sm

# Copy processing script
COPY sample_processor.py /app/sample_processor.py

# Create data directory and copy frequency data
RUN mkdir -p /app/data
COPY lemmas_20k_words.txt /app/data/lemmas_20k_words.txt

# Create volume mount point
RUN mkdir -p /tmp/spacy-data

# Set permissions
RUN chmod +x /app/sample_processor.py

# Default command (can be overridden)
CMD ["python", "/app/sample_processor.py"]

# Metadata
LABEL maintainer="Learney ETL Team"
LABEL version="1.0.0"
LABEL description="spaCy NLP processing container for vocabulary analysis" 