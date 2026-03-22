# VocabularyLookupService

## Proposito

Servicio central que carga y cachea el `enriched_vocabulary_catalog.json` y proporciona metodos de busqueda rapida para todos los analyzers.

## Ruta
`src/main/java/com/learney/etl/transform/vocabulary/VocabularyLookupService.java`

## Datos que Carga

Al inicializarse, carga `enriched_vocabulary_catalog.json` y construye mapas internos:
- **Mapa por palabra** -> `EnrichedVocabularyEntry`
- **Mapa por nivel** -> Lista de `EnrichedVocabularyEntry`
- **Mapa por lema** -> Formas de la palabra

## Metodos Principales

| Metodo | Descripcion | Usado por |
|--------|-------------|-----------|
| `lookupWord(String word)` | Busca una palabra y retorna su entrada enriquecida | LemmaAbsence |
| `containsWord(String word)` | Verifica si la palabra existe en el catalogo | Filtrado |
| `getWordsForLevel(String level)` | Retorna todas las palabras esperadas para un nivel CEFR | LemmaAbsence, LemmaCount |
| `getWordLevel(String word)` | Retorna el nivel CEFR de una palabra | Clasificacion |
| `getWordFrequency(String word)` | Retorna la frecuencia COCA | Priorizacion |
| `getWordFormsForLemma(String lemma)` | Retorna todas las formas de un lema | Enriquecimiento |
| `clearCache()` | Limpia el cache interno | Testing |

## Modelo de Cache

El servicio implementa un cache lazy-loading:
1. La primera llamada carga todo el catalogo desde disco
2. Las llamadas subsiguientes usan mapas en memoria
3. `clearCache()` fuerza una recarga en la siguiente llamada

<!-- Sugerencia: El catalogo de 15,696 entradas se carga completamente en memoria. Para cursos con miles de oraciones, la busqueda en memoria es rapida. Pero el startup puede tardar unos segundos por el parsing del JSON de 11 MB. -->

## Dependencias

| Recurso | Obligatorio | Alternativa |
|---------|-------------|-------------|
| `enriched_vocabulary_catalog.json` | SI | Ninguna - sin este archivo, el servicio falla |
| `evp.json` | NO | El enriched catalog ya contiene los datos del EVP |
| `lemmas_20k_words.txt` | NO | El enriched catalog ya contiene frecuencias |

## Filtraje de Frases

El metodo `getWordsForLevel()` incluye un filtro critico:
```java
if (!entry.isPhrase() && entry.getLemma() != null && !entry.getLemma().trim().isEmpty()) {
    // Solo incluir lemas individuales, no frases
}
```

<!-- Cuidado aqui: "Frases" como "look forward to" o "get up" se excluyen del analisis de ausencia. Esto significa que phrasal verbs no se evaluan como vocabulario esperado. Si es un requisito del destino, necesitara tratamiento especial. -->
