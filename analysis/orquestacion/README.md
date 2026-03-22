# Orquestacion del Pipeline de Analisis

## Descripcion

El `VocabularyAnalyzerOrchestrator` es el punto de entrada principal que coordina todo el flujo de analisis de vocabulario. Recibe un ID de curso y produce un resultado completo con metricas, scores y estadisticas.

### Documentos de esta seccion

| Documento | Contenido |
|-----------|-----------|
| [README.md](./README.md) (este archivo) | Flujo del orquestador paso a paso |
| [flujo-de-datos.md](./flujo-de-datos.md) | Diagrama de qué lee cada analyzer |
| [modelo-de-datos.md](./modelo-de-datos.md) | Estructuras de entrada (CompleteCourseDTO, SentenceContext) y salida (CourseStats completo) |

---

## Flujo Completo

```
main() / API call
    |
    v
[1] Cargar Configuracion
    TargetsConfig.loadConfig("/pipeline/vocabulary-analyzer/config.yaml")
    |
    v
[2] Conectar a MongoDB
    MongoManager -> base de datos "learney"
    |
    v
[3] Crear Instancias
    +-- CourseExtractor (extrae curso de Mongo)
    +-- VocabularyLookupService (carga enriched_vocabulary_catalog.json)
    +-- SpacyContainerWrapper (arranca Docker con SpaCy)
    +-- VocabularySentenceProcessor (procesa oraciones con SpaCy)
    +-- Instanciar analyzers con config:
    |       +-- SentenceLengthAnalyzer(config.sentenceLength)
    |       +-- LemmaCocaBucketsDistribution.fromConfig(config.cocaBucketsDistribution.course)
    |       +-- LemmaByLevelAbsence(vocabularyLookupService, config.lemmaAbsence)
    |       +-- LemmaRecurrence(config.lemmaRecurrence)
    +-- KnowledgeTitlesLength(config.knowledgeLengths)
    |
    v
[4] processCourseVocabulary(courseId, vocabularyLookupService)
    |
    +--[4a] Extraer datos del curso
    |       CompleteCourseDTO = courseExtractor.extractSingleCourse(courseId)
    |       (contiene: milestones, knowledges, quizzes, sentences)
    |
    +--[4b] Procesar oraciones
    |       sentenceProcessor.extractSentencesWithContext(courseData)
    |       -> Map<CEFRLevel, List<SentenceWithContext>>
    |       Cada oracion pasa por SpaCy si no esta en cache
    |
    +--[4c] Crear contexto compartido
    |       CourseSentences(vocabularyLookupService, sentencesByLevel)
    |       CourseStats.Builder(completeCourseDTO)
    |
    +--[4d] Ejecutar cada analyzer (en secuencia)
    |       Para cada analyzer:
    |           result = analyzer.analyze(courseSentences, courseStatsBuilder)
    |           analyzer.addToResult(resultBuilder, result)
    |           acumular overallScore
    |
    +--[4e] Ejecutar KnowledgeTitlesLength (separado)
    |       knowledgeTitlesLengthAnalyzer.analyze(knowledges, courseStatsBuilder)
    |
    +--[4f] Construir resultados finales
    |       resultBuilder.overallScore(promedio de scores)
    |       FrameworkVocabularyAnalysisResult = resultBuilder.build()
    |       CourseStats = courseStatsBuilder.build()
    |
    v
[5] Retornar Result(analysisResult, courseStats)
```

---

## Datos de Entrada

### CompleteCourseDTO

Estructura que representa un curso completo extraido de MongoDB:

```
CompleteCourseDTO
    +-- id: ObjectId
    +-- milestones: List<Milestone>
    |       +-- levels: List<Level>
    |               +-- topics: List<Topic>
    |                       +-- knowledges: List<Knowledge>
    |                               +-- id: String
    |                               +-- label: String (titulo)
    |                               +-- instructions: String
    |                               +-- isSentence: boolean
    |                               +-- quizzes: List<Quiz>
    |                                       +-- id: String
    |                                       +-- sentence: String
    |                                       +-- translation: String
```

<!-- Sugerencia: La estructura del curso es jerarquica: Milestone -> Level -> Topic -> Knowledge -> Quiz. Los analyzers trabajan principalmente a nivel de Quiz (la oracion) con contexto del Knowledge, Topic y Level. -->

### SentenceWithContext

Cada oracion procesada incluye su contexto de ubicacion:

```
SentenceWithContext
    +-- sentence: String (texto de la oracion)
    +-- spacyProcessingResult: SpacyProcessingResult (tokens procesados)
    +-- knowledge: Knowledge (ejercicio contenedor)
    +-- context: SentenceContext
            +-- quizId: String
            +-- knowledgeId: String
            +-- topicId: String
            +-- level: CEFRLevel
```

---

## Resultados de Salida

### FrameworkVocabularyAnalysisResult

Resultado del analisis completo:

```json
{
    "courseId": "000000000000000000000000",
    "analysisDate": "2025-09-21T10:40:00.000Z",
    "overallScore": 0.78,
    "cocaBucketsDistribution": { ... },
    "sentenceLength": { ... },
    "lemmaAbsence": { ... },
    "lemmaRecurrenceResult": { ... }
}
```

### CourseStats

Estadisticas desglosadas por nivel del curso:

```json
{
    "score": 0.78,
    "cocaBucketsScore": 0.82,
    "recurrenceScore": 0.75,
    "absenceScore": 0.87,
    "sentenceLengthScore": 0.85,
    "exerciseLengthScore": 0.91,
    "lemmaCountScore": 0.0,
    "levels": {
        "A1": {
            "score": 0.85,
            "topics": {
                "topic1": {
                    "knowledges": {
                        "knowledge1": {
                            "sentences": {
                                "quiz1": {
                                    "sentenceLength": {...},
                                    "cocaBuckets": {...},
                                    "absence": {...}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

<!-- Cuidado aqui: CourseStats puede ser MUY grande para cursos con muchos ejercicios. Cada oracion tiene su propia entrada con multiples metricas. Para un curso con 1000 oraciones, el JSON de CourseStats puede pesar varios MB. -->

---

## Orden de Ejecucion de Analyzers

Los analyzers se ejecutan en este orden estricto (secuencial):

1. **SentenceLengthAnalyzer** - El mas rapido, solo cuenta tokens
2. **LemmaCocaBucketsDistribution** - Clasifica tokens en bandas de frecuencia
3. **LemmaByLevelAbsence** - Compara lemas presentes vs esperados
4. **LemmaRecurrence** - Calcula intervalos entre apariciones (usa GlobalWordPositionTracker)

Despues de los analyzers VocabularyAnalyzer:
5. **KnowledgeTitlesLength** - Evalua longitud de titulos e instrucciones

<!-- Sugerencia: El orden importa porque CourseSentences usa lazy-loading. Si un analyzer llama a getLemmasByLevel() por primera vez, se cachea para los siguientes. El primer analyzer que necesite datos de lemas "pagara" el costo de extraccion. -->

---

## Score Final

```
overallScore = (score_analyzer_1 + score_analyzer_2 + ... + score_analyzer_n) / n
```

Donde n = cantidad de analyzers activos (actualmente 4).

<!-- Cuidado aqui: Es un PROMEDIO SIMPLE. Un analyzer con score muy bajo afecta proporcionalmente. Por ejemplo, si SentenceLength=0.95, CocaBuckets=0.90, Absence=0.85, Recurrence=0.10, el score final seria 0.70, arrastrado por Recurrence. -->

---

## Mi Opinion

**La orquestacion es solida y extensible** gracias al patron Strategy (interfaz VocabularyAnalyzer).

**Fortalezas:**
- Patron plug-and-play: agregar un nuevo analyzer es tan simple como implementar la interfaz y agregarlo a la lista
- El CourseSentences compartido con lazy-loading evita procesamiento redundante
- La separacion en Result (analisis) y Stats (estadisticas detalladas) permite diferentes usos

**Puntos criticos para la migracion:**

1. **Dependencia de MongoDB**: El sistema actual extrae datos directamente de MongoDB. Si la fuente de datos cambia, se necesita adaptar CourseExtractor.

2. **Dependencia de Docker/SpaCy**: Sin Docker, no hay procesamiento NLP. Alternativas: API Python directa, servicio cloud NLP, o pre-procesamiento offline.

3. **Score promedio simple**: Considerar si un promedio ponderado seria mas apropiado para el destino.

4. **Ejecucion secuencial**: Los analyzers se ejecutan uno a uno. Si el rendimiento es critico, algunos podrian paralelizarse (SentenceLength y CocaBuckets son independientes entre si).

5. **Acoplamiento con el modelo de datos**: El orquestador tiene un `instanceof` check para detectar el tipo de resultado y registrarlo en CourseStats. Esto viola el Open/Closed principle. En la migracion, considerar que cada analyzer registre sus propios stats.

6. **KnowledgeTitlesLength separado**: Este analyzer no sigue el patron VocabularyAnalyzer y se ejecuta aparte. Si se migra, decidir si integrarlo al framework o mantenerlo separado.
