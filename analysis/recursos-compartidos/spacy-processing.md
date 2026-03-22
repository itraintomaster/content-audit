# Procesamiento SpaCy

## Descripcion

Todas las oraciones del curso pasan por un procesamiento NLP con **SpaCy** (modelo `en_core_web_sm`) que se ejecuta en un **contenedor Docker**. Este procesamiento es un requisito previo para que los analyzers funcionen.

## Pipeline de Procesamiento

```
Oracion (texto)
    -> SpaCy (Docker container)
    -> SpacyProcessingResult
        -> tokens (palabras individuales)
            -> lemma (forma base)
            -> posTag (parte de la oracion: NOUN, VERB, ADJ, etc.)
            -> tag (etiqueta especifica: NN, NNS, VB, VBD, etc.)
            -> frequencyRank (ranking COCA)
            -> tokenCount (total de tokens en la oracion)
```

## SpacyProcessingResult

Estructura producida por el procesamiento:

```json
{
  "tokenCount": 7,
  "processedWords": [
    {
      "text": "She",
      "lemma": "she",
      "posTag": "PRON",
      "tag": "PRP",
      "frequencyRank": 31,
      "isAlpha": true,
      "isStop": true
    },
    {
      "text": "likes",
      "lemma": "like",
      "posTag": "VERB",
      "tag": "VBZ",
      "frequencyRank": 87,
      "isAlpha": true,
      "isStop": false
    }
  ]
}
```

## Campos Criticos para los Analyzers

| Campo | Usado por | Para que |
|-------|-----------|----------|
| `tokenCount` | SentenceLengthAnalyzer | Longitud de la oracion |
| `lemma` | Todos | Forma base para comparaciones |
| `posTag` | LemmaAbsence, ContentWordFilter | Identificar parte de la oracion |
| `frequencyRank` | CocaBuckets | Clasificar en bandas de frecuencia |

## Cache de Resultados SpaCy

Los resultados se cachean para evitar reprocesamiento:
- **Directorio:** `src/main/resources/vocabulary/cache/`
- **Archivos:** `spacy-results-{A1,A2,B1,B2}.json`
- **Clave:** Hash SHA-256 del contenido de las oraciones
- **Expiracion:** 30 dias
- **Tamano total:** ~61 MB (los 4 niveles juntos)

<!-- Sugerencia: Si se migra el sistema, los archivos de cache actuales se pueden reutilizar directamente siempre que las oraciones no cambien. Esto ahorra el tiempo de ejecutar SpaCy nuevamente. -->

## Docker Container

SpaCy se ejecuta dentro de un contenedor Docker manejado por `SpacyContainerWrapper`. El container:
1. Se inicia automaticamente cuando se necesita
2. Recibe las oraciones via HTTP
3. Retorna los resultados procesados
4. Se detiene al finalizar

<!-- Cuidado aqui: Si el entorno de migracion no tiene Docker disponible, se necesita una alternativa para el procesamiento SpaCy. Opciones: servidor SpaCy standalone, API Python directa, o un servicio cloud de NLP. -->
