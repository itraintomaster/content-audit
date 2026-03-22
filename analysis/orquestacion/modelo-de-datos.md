# Modelo de Datos — Estructuras de Entrada y Salida

Este documento describe los modelos de datos que los analyzers consumen y producen. Son esenciales para la migración porque definen **qué datos necesitas tener** y **qué estructura tienen los resultados**.

---

## 1. Datos de Entrada: Jerarquía del Curso

### CompleteCourseDTO

Estructura raíz extraída de MongoDB:

```
CompleteCourseDTO
  ├── id: ObjectId                     // ID del curso en MongoDB
  ├── title: String                    // Nombre del curso
  └── milestones: List<MilestoneCompleteDTO>
        └── topics: List<TopicCompleteDTO>     // Cada milestone tiene topics
              ├── id: ObjectId
              ├── label: String                 // Nombre del topic (ej: "Present Simple")
              └── knowledges: List<KnowledgeCompleteDTO>
                    ├── id: ObjectId
                    ├── label: String            // Título del ejercicio
                    ├── code: String
                    ├── instructions: String     // Instrucciones para el estudiante
                    ├── topicLabel: String        // Label del topic padre
                    ├── isSentence: boolean       // ¿Es una oración evaluable? (default: true)
                    └── quizzes: List<QuizTemplateDTO>
                          ├── id: ObjectId
                          ├── sentence: String    // La oración del ejercicio
                          └── translation: String // Traducción
```

**Métodos importantes de CompleteCourseDTO:**
- `getAllTopics()` — Aplana todos los topics de todos los milestones
- `getAllKnowledges()` — Recorre toda la jerarquía para obtener todos los knowledge items
- `getKnowledge(String id)` — Busca un knowledge por ID

### KnowledgeCompleteDTO — Detalle

El campo `isSentence` es crítico para SentenceLengthAnalyzer:

```
isSentence = true   -> Se evalúa como oración (longitud, COCA buckets, etc.)
isSentence = false  -> Se marca como "noSentence" y se excluye de métricas de longitud
```

<!-- Cuidado aquí: isSentence es true por default. Esto significa que si un knowledge no tiene este campo en la base de datos, se asumirá que SÍ es una oración. Si el modelo del destino no tiene este campo, todas las entradas se evaluarán como oraciones. -->

**Detección de rewrites:**
Hay un conjunto hardcodeado de labels que identifican ejercicios de "rewrite":
```
"Spelling changes with verb+ing: set 2"
"Using From ... To / Until / Till"
"Sentences using Let's"
"Making Polite Requests: Using 'Would Like'"
... (y otros)
```

Si más del 50% de los quizzes de un knowledge son rewrites, todo el knowledge se marca como rewrite.

---

## 2. Datos Intermedios: Oraciones Procesadas

### SentenceWithContext

Cada oración extraída del curso lleva su contexto completo:

```
SentenceWithContext
  ├── course: CompleteCourseDTO              // Referencia al curso completo
  ├── sentence: String                        // Texto de la oración
  ├── spacyProcessingResult: ProcessedSentence // Resultado de SpaCy
  │     ├── tokenCount: int                   // Total de tokens
  │     └── processedWords: List<ProcessedWordInSentence>
  │           ├── text: String                // Palabra original
  │           ├── lemma: String               // Forma base (lematizada)
  │           ├── posTag: String              // POS universal (NOUN, VERB, ADJ, etc.)
  │           ├── tag: String                 // Tag específico (NN, NNS, VB, etc.)
  │           ├── frequencyRank: Integer      // Ranking COCA
  │           ├── isAlpha: boolean            // ¿Es alfabético?
  │           └── isStop: boolean             // ¿Es stop word?
  └── context: SentenceContext
        ├── milestoneIndex: int
        ├── milestoneLabel: String
        ├── topicIndex: int
        ├── topicLabel: String
        ├── topicId: String
        ├── knowledgeIndex: int
        ├── knowledgeLabel: String
        ├── knowledgeId: String
        ├── quizIndex: int
        ├── quizId: String                    // ID único de la oración
        ├── quizLabel: String
        ├── level: CEFRLevel                  // Nivel CEFR (A1, A2, B1, B2)
        └── quarterIndex: int                 // Índice de trimestre dentro del nivel
```

<!-- Sugerencia: El SentenceContext es la "dirección" completa de cada oración dentro del curso. Es lo que permite construir las estadísticas jerárquicas (CourseStats). Si el modelo del destino tiene una estructura de curso diferente, este contexto debe adaptarse. -->

### LemmaAndPos

Tupla que identifica unívocamente un lema. Es la **clave de comparación** en todos los analyzers de vocabulario:

```
LemmaAndPos
  ├── lemma: String   // Forma base, minúsculas, trimmed (ej: "cat")
  └── pos: String     // POS tag universal (ej: "NOUN")
```

**Implementa:** `equals()`, `hashCode()`, `Comparable` (ordena por lemma, luego por POS)

<!-- Cuidado aquí: "run" (VERB) y "run" (NOUN) son LemmaAndPos DISTINTOS. Esto es correcto lingüísticamente pero puede causar falsos positivos si el POS tagging es inconsistente entre SpaCy y el EVP. -->

---

## 3. Datos de Salida: Jerarquía de Estadísticas

### CourseStats (Estructura Completa)

```
CourseStats
  ├── score: double                          // Score general del curso
  ├── cocaBucketsScore: double               // Score de distribución COCA
  ├── recurrenceScore: double                // Score de recurrencia
  ├── absenceScore: double                   // Score de ausencia
  ├── sentenceLengthScore: double            // Score de longitud de oraciones
  ├── exerciseLengthScore: double            // Score de longitud de títulos
  ├── lemmaCountScore: double                // Score de conteo (actualmente 0)
  └── levels: Map<String, LevelStats>
        ├── id: String                        // "A1", "A2", "B1", "B2"
        ├── score: double
        ├── cocaBuckets: CocaBucketResult
        ├── sentenceLengthScore: double
        ├── exerciseLengthScore: double
        ├── detailedScore: LevelDetailedScore
        │     ├── absence: LevelAbsenceDetailedScore
        │     │     ├── appearsTooEarly: double
        │     │     ├── appearsTooLate: double
        │     │     ├── scatteredPlacement: double
        │     │     ├── completelyAbsent: double
        │     │     └── overallScore: double
        │     └── cocaBuckets: LevelCocaBucketsDetailedScore
        │           ├── buckets: double
        │           ├── quarters: double
        │           └── overallScore: double
        ├── knowledgeLengths: KnowledgeLengths
        │     ├── correctTitlesPercentage: double
        │     └── correctInstructionsPercentage: double
        ├── lemmaCount: LemmaCount { score: double }
        └── topics: Map<String, TopicStats>
              ├── id: String
              ├── score: double
              ├── cocaBuckets: CocaBucketResult
              ├── sentenceLengthScore: double
              ├── exerciseLengthScore: double
              ├── knowledgeLengths: KnowledgeLengths
              ├── lemmaCount: LemmaCount { score: double }
              ├── absence: { missPlacedLemmaCount: int }
              └── knowledges: Map<String, KnowledgeStats>
                    ├── id: String
                    ├── score: double
                    ├── cocaBuckets: CocaBucketResult
                    ├── isSentence: boolean
                    ├── sentenceLengthScore: double
                    ├── absence: { missPlacedLemmaCount, lemmas: Set<AbsentLemma> }
                    ├── lengths: Lengths
                    │     ├── labelLength: double
                    │     ├── labelScore: double
                    │     ├── instructionsLength: double
                    │     └── instructionsScore: double
                    ├── lemmaCount: LemmaCount { score: double }
                    └── sentences: Map<String, SentenceStats>
                          ├── id: String
                          ├── score: double
                          ├── sentenceLength: SentenceLength
                          │     ├── score: double
                          │     ├── length: int
                          │     ├── minTargetLength: int
                          │     ├── maxTargetLength: int
                          │     └── isSentence: boolean
                          ├── absence: Absence
                          │     ├── score: double
                          │     └── lemmasThatAppearsTooEarly: Set<AbsentLemma>
                          └── lemmaCount: LemmaCount { score: double }
```

---

## 4. Enums de Soporte

### CEFRLevel

```
A1 (order=1, "Pre-Beginner")
A2 (order=2, "Elementary")
B1 (order=3, "Intermediate")
B2 (order=4, "Upper Intermediate")
```

Métodos: `getOrderedLevels()`, `fromString()`, `isBeginner()`, `isIntermediate()`

### CoverageStatus

```
OPTIMAL      ("Óptimo",        "#10B981")  // Verde
ADEQUATE     ("Adecuado",      "#F59E0B")  // Amarillo
DEFICIENT    ("Deficiente",    "#EF4444")  // Rojo
EXCESSIVE    ("Excesivo",      "#8B5CF6")  // Púrpura
NOT_APPLICABLE ("No Aplicable", "#6B7280") // Gris
```

### PriorityLevel

```
HIGH   ("Alta",  COCA rank ≤ 1000,  "Requiere acción inmediata",  🔴)
MEDIUM ("Media", COCA rank ≤ 3000,  "Acción recomendada",         🟡)
LOW    ("Baja",  COCA rank > 3000,  "Acción opcional",            🟢)
```

---

## 5. ContentWordFilter — Configuración por Defecto

```
topFrequencyThreshold = 100         // Excluir top 100 palabras más frecuentes

contentPosTags = NOUN, VERB, ADJ, ADV

functionWords = the, a, an, and, or, but, in, on, at, to, for, of, with,
                i, you, he, she, it, we, they, me, him, her, us, them,
                be, is, am, are, was, were, have, has, had, do, does, did,
                will, would, can, could, should, may, might, must,
                very, too, really, just, only, also, even, still, quite, rather,
                countable, uncountable, =, -, +, b, c

auxiliaryVerbs = be, have, do, will, would, can, could, should, may, might, must

excludeProperNouns = true
excludeNumbers = true
```

<!-- Cuidado aquí: La lista de function words incluye "countable", "uncountable", "=", "-", "+", "b", "c". Estos son artefactos del contenido del curso (instrucciones como "countable/uncountable nouns"). Si el contenido del destino no tiene estos artefactos, podrían removerse. -->

---

## 6. Cache de SpaCy

```
cache.enabled = true
cache.directory = vocabulary/cache
cache.expire.days = 30
```

Archivos de cache por nivel: `spacy-results-{A1,A2,B1,B2}.json` (~61 MB total)

Clave de cache: SHA-256 del contenido de todas las oraciones concatenadas con `|||` como separador.
