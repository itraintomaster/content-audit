---
patch: ARCH-LCOUNT-001
requirement: 2026-03-28.02_lemma-count
generated: 2026-03-28T14:00:00Z
---

# Tech Spec: Lemma Repetition Analysis (F-LCOUNT)

## Define Domain Models for Lemma Counting
We need to represent the frequency of content-word lemmas and the overall analysis summary. Using records ensures immutability for these data transfer objects, while a specific exception handles failures in the NLP pipeline.

```architecture
modules:
  - name: "lcount-domain"
    _change: "add"
    layer: "domain"
    models:
      - name: "LemmaCount"
        type: "record"
        fields:
          - { name: "lemma", type: "String" }
          - { name: "count", type: "int" }
          - { name: "posTag", type: "String" }
      - name: "AnalysisResult"
        type: "record"
        fields:
          - { name: "counts", type: "java.util.List<LemmaCount>" }
          - { name: "totalContentWords", type: "int" }
      - name: "LemmaAnalysisException"
        type: "exception"
        message: "Failed to analyze text: %s"
        fields:
          - { name: "reason", type: "String" }
```

## Define LemmaAnalyzer Port
Decoupling the analysis logic from specific NLP libraries allows for future flexibility, such as switching providers or upgrading versions without affecting the domain. The port defines the contract for text processing and lemma extraction.

```architecture
modules:
  - name: "lcount-domain"
    _change: "add"
    interfaces:
      - name: "LemmaAnalyzer"
        stereotype: "port"
        exposes:
          - signature: "analyze(String text): AnalysisResult"
            throws: ["LemmaAnalysisException"]
```

## Implement NLP-based Lemma Analysis
The infrastructure layer provides the concrete implementation using OpenNLP. This keeps the domain clean of external library dependencies while providing the necessary NLP capabilities for Part-of-Speech tagging and lemmatization.

```architecture
modules:
  - name: "lcount-infra"
    _change: "add"
    layer: "infrastructure"
    dependsOn: ["lcount-domain"]
    implementations:
      - name: "OpenNlpLemmaAnalyzer"
        visibility: "public"
        implements: ["LemmaAnalyzer"]
        types: ["Adapter"]
```

## Orchestrate Lemma Analysis Use Case
The application layer coordinates the flow, injecting the required analyzer and exposing a clean API for external callers. This orchestrator serves as the primary entry point for the lemma counting feature.

```architecture
modules:
  - name: "lcount-app"
    _change: "add"
    layer: "application"
    dependsOn: ["lcount-domain", "lcount-infra"]
    implementations:
      - name: "LCountOrchestrator"
        stereotype: "class"
        visibility: "public"
        requiresInject:
          - { name: "analyzer", type: "LemmaAnalyzer" }
        exposes:
          - signature: "processText(String text): AnalysisResult"
            throws: ["LemmaAnalysisException"]
        types: ["UseCase"]
```