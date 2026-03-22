# Flujo de Datos - Diagrama Detallado

## Vista General de Datos

```
                    FUENTES DE DATOS
                    ================

    MongoDB                 enriched_vocabulary        config.yaml
    (cursos)                catalog.json               (targets)
       |                         |                         |
       v                         v                         v
  CourseExtractor      VocabularyLookupService        TargetsConfig
       |                         |                         |
       v                         |                         |
  CompleteCourseDTO              |                         |
       |                         |                         |
       v                         |                         |
  VocabularySentenceProcessor    |                         |
  (SpaCy Docker)                 |                         |
       |                         |                         |
       v                         |                         |
  Map<CEFRLevel,                 |                         |
   List<SentenceWithContext>>    |                         |
       |                         |                         |
       v                         v                         |
  +------------------------------+                         |
  |     CourseSentences          |                         |
  | (cache compartido)           |                         |
  |                              |                         |
  | - sentencesByLevel     [lazy]|                         |
  | - lemmasByLevel        [lazy]|                         |
  | - expectedLemmasByLevel[lazy]|                         |
  | - globalWordPosition   [lazy]|                         |
  +------------------------------+                         |
       |                                                   |
       |  +------------------------------------------------+
       |  |
       v  v
  +------------------+    +------------------+    +------------------+
  | SentenceLength   |    | CocaBuckets      |    | LemmaAbsence     |
  | Analyzer         |    | Distribution     |    |                  |
  |                  |    |                  |    |                  |
  | Lee:             |    | Lee:             |    | Lee:             |
  | - sentencesByLvl |    | - sentencesByLvl |    | - lemmasByLevel  |
  | - tokenCount     |    | - frequencyRank  |    | - expectedLemmas |
  |                  |    |                  |    | - lookupService  |
  | Config:          |    | Config:          |    | Config:          |
  | - targetsByLevel |    | - buckets        |    | - thresholds     |
  |   (min/max)      |    | - levels/quarter |    | - priorities     |
  |                  |    | - progression    |    |                  |
  | Produce:         |    | Produce:         |    | Produce:         |
  | SentenceLength   |    | CocaBuckets      |    | LevelSpecific    |
  | Result           |    | DistResult       |    | AbsentLemmasRes  |
  +------------------+    +------------------+    +------------------+
       |                       |                       |
       |                       |                       |
       v                       v                       v
  +------------------+
  | LemmaRecurrence  |
  |                  |
  | Lee:             |
  | - globalWordPos  |
  |                  |
  | Config:          |
  | - top            |
  | - subExposed     |
  | - overExposed    |
  |                  |
  | Produce:         |
  | Individual       |
  | LemmaRecurrence  |
  | Result           |
  +------------------+
       |
       v
  +-------------------------------------------------+
  | FrameworkVocabularyAnalysisResult                |
  |                                                 |
  | - courseId                                      |
  | - analysisDate                                  |
  | - overallScore (promedio de todos los analyzers)|
  | - sentenceLengthResult                          |
  | - cocaBucketsDistribution                       |
  | - lemmaAbsence                                  |
  | - lemmaRecurrenceResult                         |
  +-------------------------------------------------+
       |
       v
  +-------------------------------------------------+
  | CourseStats                                     |
  |                                                 |
  | - score (promedio de niveles)                   |
  | - cocaBucketsScore                              |
  | - recurrenceScore                               |
  | - absenceScore                                  |
  | - sentenceLengthScore                           |
  | - exerciseLengthScore                           |
  | - lemmaCountScore                               |
  | - levels: Map<String, LevelStats>               |
  |     +-- topics: Map<String, TopicStats>         |
  |         +-- knowledges: Map<String, KnStats>    |
  |             +-- sentences: Map<String, SStats>  |
  +-------------------------------------------------+
```

## Que Lee Cada Analyzer de CourseSentences

| Analyzer | Metodo de CourseSentences | Datos que obtiene |
|----------|--------------------------|-------------------|
| SentenceLengthAnalyzer | `getSentencesByLevel()` | Oraciones con tokenCount |
| CocaBucketsDistribution | `getSentencesByLevel()` | Oraciones con frequencyRank por token |
| LemmaByLevelAbsence | `getLemmasByLevel()` + `getExpectedLemmasByLevel()` | Sets de LemmaAndPos (presentes vs esperados) |
| LemmaRecurrence | `getGlobalWordPositionTracker()` | Posiciones globales de content words |
| LemmaCountAnalyzer* | `getExpectedLemmasByLevel()` + `getLemmasByLevelWithContext()` | Conteo de apariciones por lema |

*Inactivo

## Modelo de Datos Clave: LemmaAndPos

Todos los analyzers que trabajan con lemas usan esta tupla como clave de identificacion:

```java
public record LemmaAndPos(String lemma, String pos) {}
```

Ejemplo: `LemmaAndPos("cat", "NOUN")` != `LemmaAndPos("cat", "VERB")`

<!-- Cuidado aqui: LemmaAndPos es un record de Java, lo que significa que usa equals() y hashCode() basados en ambos campos. Si el lemma coincide pero el POS no, se consideran DIFERENTES. Esto puede causar falsos positivos en el analisis de ausencia. -->

## Modelo de Datos Clave: SentenceContext

Cada oracion lleva su contexto para poder mapear resultados a la estructura jerarquica:

```java
SentenceContext {
    String quizId;        // ID unico de la oracion
    String knowledgeId;   // Ejercicio contenedor
    String topicId;       // Tema contenedor
    CEFRLevel level;      // Nivel CEFR
}
```

Este contexto es esencial para construir CourseStats en su forma jerarquica.
