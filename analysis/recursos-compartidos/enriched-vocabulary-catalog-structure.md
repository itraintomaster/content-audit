# Estructura de enriched_vocabulary_catalog.json

## Ruta Original
`src/main/resources/vocabulary/enriched_vocabulary_catalog.json`

## Generacion
Este archivo es GENERADO, no manual. Combina datos de EVP + COCA + SpaCy.

**Fecha de generacion:** 2025-06-22T22:28:37
**Tiempo de generacion:** ~554 segundos
**Total de registros:** 15,696

## Estructura JSON

```json
{
  "metadata": {
    "processing_time_ms": 554425,
    "evp_source": "English Vocabulary Profile",
    "total_processed": 15696,
    "failed_enrichments": 0,
    "generated_at": "2025-06-22T22:28:37.921191",
    "frequency_source": "COCA Corpus",
    "nlp_processor": "SpaCy en_core_web_sm",
    "successful_enrichments": 15696
  },
  "enriched_words": [
    {
      "word": "cattle",
      "cefrLevel": "B1",
      "topic": "animals",
      "lemma": "cattle",
      "highFrequencyForLevel": false,
      "difficultyScore": 4.261950000000001,
      "summary": "Word: cattle | Level: B1 | Lemma: cattle | POS: NOUN | Freq: 4471 | Confidence: 0,90",
      "cefrLevelAsInteger": 3,
      "evpPartOfSpeech": "",
      "spacyPosTag": "NOUN",
      "spacyTag": "NNS",
      "isAlpha": true,
      "isStop": false,
      "namedEntity": null,
      "frequencyRank": 4471,
      "lemmaFrequency": 14338,
      "wordFrequency": 14338,
      "frequencyCategory": "MEDIUM",
      "isPhrase": false,
      "realPos": "NOUN"
    }
  ]
}
```

## Campos por Entrada

| Campo | Tipo | Descripcion | Usado por |
|-------|------|-------------|-----------|
| `word` | String | Palabra original del EVP | Lookup |
| `cefrLevel` | String | Nivel CEFR (A1-C2) | LemmaAbsence, LemmaCount |
| `topic` | String | Categoria tematica | Assessment de ausencia |
| `lemma` | String | Forma base (lematizada) | **Clave de comparacion** |
| `highFrequencyForLevel` | Boolean | Si es de alta frecuencia para su nivel | Informativo |
| `difficultyScore` | Double | Score de dificultad calculado | Informativo |
| `cefrLevelAsInteger` | Integer | Nivel como numero (A1=1, B2=4) | Comparaciones |
| `evpPartOfSpeech` | String | POS del EVP original | Referencia |
| `spacyPosTag` | String | POS universal asignado por SpaCy | **Comparaciones con oraciones** |
| `spacyTag` | String | Tag especifico de SpaCy | Detalle |
| `isAlpha` | Boolean | Si es alfabetico | Filtrado |
| `isStop` | Boolean | Si es stop word | ContentWordFilter |
| `namedEntity` | String | Entidad nombrada | Filtrado |
| `frequencyRank` | Integer | Ranking en COCA | **CocaBuckets, Absence** |
| `lemmaFrequency` | Integer | Frecuencia del lema en COCA | Informativo |
| `wordFrequency` | Integer | Frecuencia de la forma | Informativo |
| `frequencyCategory` | String | Categoria (HIGH/MEDIUM/LOW) | Informativo |
| `isPhrase` | Boolean | Si es una frase (multi-palabra) | **Filtro: frases se excluyen** |
| `realPos` | String | POS real (derivado de SpaCy) | **Comparacion con LemmaAndPos** |

<!-- Cuidado aqui: Los campos mas criticos para la migracion son: lemma, cefrLevel, realPos, frequencyRank, y isPhrase. Sin estos, los analyzers de ausencia y distribucion COCA no pueden funcionar. -->

## Modelo Java Asociado

La clase `EnrichedVocabularyEntry` mapea estos campos y es la base del `VocabularyLookupService`.
