# Vocabulary Analyzers - Documentacion Funcional para Migracion

## Descripcion General

El sistema de **Vocabulary Analyzers** es un framework de analisis de vocabulario para cursos de ingles organizados por niveles CEFR (A1, A2, B1, B2). Su objetivo es evaluar la calidad pedagogica del vocabulario utilizado en las oraciones de cada curso.

El sistema procesa las oraciones del curso a traves de un pipeline de NLP (SpaCy) y luego ejecuta multiples analyzers que evaluan diferentes aspectos del vocabulario.

---

## Arquitectura General

```
VocabularyAnalyzerOrchestrator
    |
    +-- Extrae datos del curso (MongoDB)
    +-- Procesa oraciones con SpaCy (Docker container)
    +-- Crea CourseSentences (cache de datos procesados)
    +-- Ejecuta cada Analyzer registrado:
    |       |
    |       +-- SentenceLengthAnalyzer        -> Longitud de oraciones
    |       +-- LemmaCocaBucketsDistribution   -> Distribucion de frecuencia COCA
    |       +-- LemmaByLevelAbsence            -> Ausencia de lemas esperados
    |       +-- LemmaRecurrence                -> Recurrencia de lemas
    |       |
    |       +-- [INACTIVO] LemmaCountAnalyzer  -> Conteo de lemas
    |
    +-- Ejecuta KnowledgeTitlesLength (analyzer separado, no VocabularyAnalyzer)
    +-- Construye FrameworkVocabularyAnalysisResult
    +-- Construye CourseStats (estadisticas detalladas por nivel/topico/knowledge/sentence)
```

---

## Que Pregunta Responde Cada Analyzer

Cada analyzer ataca una **dimension diferente** de la calidad del vocabulario. Ninguno es redundante con otro:

```
                                  CALIDAD DEL CURSO
                                        |
            +---------------------------+---------------------------+
            |                           |                           |
      COMPLEJIDAD                  VOCABULARIO                   UTILIDAD
      ESTRUCTURAL                  CURRICULAR                   PRACTICA
            |                           |                           |
    +-------+-------+          +--------+--------+                  |
    |               |          |                 |                  |
 Sentence      Knowledge    Absence          CocaBuckets         NGSL
 Length         Titles      (EVP)            (COCA)            Coverage
    |               |          |                 |             (propuesta)
    |               |          |                 |                  |
"Son las       "Los         "Tiene las      "Las palabras      "Cuanto del
 oraciones     titulos       palabras que    son de la          ingles real
 del largo     son claros    deberia         frecuencia         cubre este
 correcto?"    y concisos?"  tener?"         correcta?"         curso?"
                                    |
                              +-----+-----+
                              |           |
                          Recurrence   LemmaCount
                              |        (inactivo)
                              |           |
                          "Se repiten  "Cuantas
                           a buen      veces aparece
                           ritmo?"     cada una?"
```

### Las 5 Preguntas Fundamentales

| # | Pregunta | Analyzer | Fuente de datos | Ejemplo de hallazgo |
|---|----------|----------|-----------------|---------------------|
| 1 | **¿Las oraciones son apropiadas para el nivel?** | SentenceLengthAnalyzer | SpaCy tokenCount | "Las oraciones de A1 promedian 12 palabras — deberian tener 5 a 8" |
| 2 | **¿Las palabras son de la dificultad correcta?** | CocaBucketsDistribution | COCA frequency rank | "El nivel A1 tiene un 15% de palabras top4k — deberia tener maximo 1%" |
| 3 | **¿Faltan palabras que deberian estar?** | LemmaByLevelAbsence | EVP (Cambridge) | "La palabra 'between' (A2, COCA #85) no aparece en ningun nivel" |
| 4 | **¿Las palabras se refuerzan adecuadamente?** | LemmaRecurrence | Posiciones globales | "'cat' aparece 10 veces en A1 y nunca mas — sobre-expuesto localmente, ausente despues" |
| 5 | **¿Cuanto del ingles real cubre el curso?** | NGSLCoverage *(propuesta)* | NGSL 2,809 palabras | "Al completar B2, el estudiante cubre el 71% del vocabulario esencial" |

---

## Diferencias Clave entre Analyzers Similares

### CocaBuckets vs LemmaAbsence — ¿No miden lo mismo?

**No.** Atacan el vocabulario desde angulos opuestos:

| | CocaBuckets | LemmaAbsence |
|---|-------------|--------------|
| **Pregunta** | ¿Las palabras *que estan* son de la frecuencia correcta? | ¿Las palabras *que deberian estar* estan? |
| **Direccion** | Mira lo que HAY y lo evalua | Mira lo que FALTA |
| **Referencia** | COCA (corpus de uso real) | EVP (curriculo Cambridge) |
| **Granularidad** | Por oracion, topic, nivel, quarter | Por nivel |
| **Ejemplo** | "Hay demasiadas palabras raras en A1" | "Falta 'cat' en A1 — es vocabulario esperado" |

Un curso podria tener buena distribucion COCA (pocas palabras raras en niveles bajos) pero aun asi faltar palabras especificas del EVP. Y viceversa: podria tener todas las palabras del EVP pero con una distribucion de frecuencias desequilibrada.

### LemmaRecurrence vs LemmaCount — ¿No miden lo mismo?

**No.** Uno mide **cantidad** y el otro mide **distribucion temporal**:

| | LemmaCount *(inactivo)* | LemmaRecurrence |
|---|------------------------|-----------------|
| **Pregunta** | ¿Cuantas veces aparece cada lema? | ¿A que intervalos aparece? |
| **Metrica** | Conteo absoluto (ej: "cat" aparece 8 veces) | Intervalo medio (ej: "cat" aparece cada 200 palabras) |
| **Problema que detecta** | Sub-exposicion (< 4 veces) o sobre-exposicion (> 15) | Repeticion concentrada vs espaciada |
| **Ejemplo** | "'cat' aparece 2 veces — sub-expuesto" | "'cat' aparece 10 veces pero todas en los primeros 100 tokens — mal espaciado" |

Un lema puede aparecer 10 veces (buen count) pero todas concentradas en una leccion (mal recurrence). O puede aparecer 3 veces (sub-expuesto en count) pero perfectamente espaciadas cada 500 palabras (buen recurrence).

<!-- Sugerencia: LemmaCount y LemmaRecurrence son complementarios. LemmaCount esta inactivo pero su logica es valiosa. En la migracion, consideraria reactivarlo. Juntos forman un analisis completo de exposicion: "cuanto" + "cuando". -->

### SentenceLength vs KnowledgeTitlesLength — ¿No miden longitud los dos?

**Si, pero de cosas distintas:**

| | SentenceLength | KnowledgeTitlesLength |
|---|----------------|----------------------|
| **Que mide** | Longitud de las **oraciones del ejercicio** | Longitud del **titulo e instrucciones** |
| **Unidad** | Tokens linguisticos (SpaCy) | Caracteres (con peso visual) |
| **Objetivo** | Complejidad linguistica apropiada al nivel | Legibilidad de la interfaz (UI/UX) |
| **Framework** | Implementa VocabularyAnalyzer | Separado, no aporta al overallScore |

---

## Mapa de Cobertura: Que Nivel Evalua Cada Analyzer

| Analyzer | Oracion | Knowledge | Topic | Nivel | Curso completo |
|----------|---------|-----------|-------|-------|---------------|
| SentenceLengthAnalyzer | Score individual | — | — | Promedio + status | Promedio + progresion |
| CocaBucketsDistribution | Score frecuencia | Distribucion | Distribucion | Distribucion + quarters | Progresion de buckets |
| LemmaByLevelAbsence | Descuento por distancia | — | — | Metricas de ausencia | Assessment global |
| LemmaRecurrence | — | — | — | — | Solo global (intervalos) |
| LemmaCount *(inactivo)* | Score por lema | — | — | — | Score global |
| KnowledgeTitlesLength | — | Score titulo + instrucciones | — | — | — |
| NGSLCoverage *(propuesta)* | — | — | — | Cobertura acumulativa | Cobertura total + huecos |

<!-- Cuidado aqui: LemmaRecurrence es el unico analyzer que NO produce estadisticas por nivel. Analiza el curso como un bloque global. Esto significa que no puedes saber si la recurrencia es buena en A1 pero mala en B2. Es una limitacion de diseno. -->

---

## Opinion General: Relevancia de Cada Analyzer

### Tier 1 — Imprescindibles para la migracion

**CocaBucketsDistribution** — Es el corazon del sistema. Mide si el vocabulario progresa correctamente en dificultad de nivel a nivel. Sin esto, no hay forma objetiva de saber si un curso de A1 realmente usa vocabulario de A1. Es el mas complejo pero tambien el mas valioso.

**LemmaByLevelAbsence** — Complemento directo de CocaBuckets. Mientras CocaBuckets mira la distribucion general, Absence verifica que no falten palabras especificas que el curriculo exige. Un curso puede tener buena distribucion de frecuencias pero omitir palabras criticas como "family" o "school" en A1.

### Tier 2 — Muy importantes

**SentenceLengthAnalyzer** — Simple pero efectivo. La complejidad de las oraciones es un indicador directo de la dificultad del material. Es rapido, facil de entender, y da feedback accionable ("simplifica las oraciones de A1"). Deberia estar en cualquier migracion.

**LemmaRecurrence** — El concepto es pedagogicamente solido (spaced repetition) pero la implementacion tiene limitaciones: scoring binario, no hay analisis por nivel, y la desviacion estandar se calcula pero no se usa. Es valioso como **detector de problemas** mas que como metrica de calidad.

<!-- Sugerencia: Si tuviera que elegir entre LemmaRecurrence y LemmaCount para la migracion, elegiria LemmaRecurrence porque detecta un problema mas sutil (mala distribucion temporal) que es invisible al simple conteo. Pero lo ideal es tener ambos. -->

### Tier 3 — Complementarios

**KnowledgeTitlesLength** — Util para UX pero no es un analisis de vocabulario propiamente dicho. Es mas bien una validacion de formato. Migrarlo es trivial.

**LemmaCount** *(inactivo)* — Tiene valor real pero esta desactivado. Si se migra, deberia ajustarse el umbral de `overExposed` (15 es bajo para palabras basicas como "be" o "have"). Combinado con LemmaRecurrence, formaria un analisis de exposicion completo.

### Tier 4 — Nuevo, alto valor

**NGSLCoverage** *(propuesta)* — No existe aun, pero resuelve una pregunta que ningun analyzer actual responde: "¿cuanto del ingles real cubre este curso?". Es el mas facil de implementar y el mas comunicable fuera del equipo tecnico (ventas, marketing, estudiantes). Recomiendo fuertemente incluirlo en la migracion.

---

## Resumen Visual de Relevancia

```
IMPRESCINDIBLE    ██████████  CocaBucketsDistribution (distribucion de frecuencia)
IMPRESCINDIBLE    ██████████  LemmaByLevelAbsence (vocabulario curricular)
MUY IMPORTANTE    ████████░░  SentenceLengthAnalyzer (complejidad de oraciones)
MUY IMPORTANTE    ███████░░░  LemmaRecurrence (espaciado de repeticion)
COMPLEMENTARIO    █████░░░░░  KnowledgeTitlesLength (longitud de titulos)
COMPLEMENTARIO    █████░░░░░  LemmaCount [inactivo] (conteo de apariciones)
ALTO VALOR NUEVO  ████████░░  NGSLCoverage [propuesta] (cobertura de ingles esencial)
```

---

## Analyzers Activos (Registrados en el Orquestador)

| # | Analyzer | Carpeta | Que evalua |
|---|----------|---------|------------|
| 1 | **SentenceLengthAnalyzer** | [01-sentence-length](./01-sentence-length/) | Longitud promedio de oraciones por nivel CEFR |
| 2 | **LemmaCocaBucketsDistribution** | [02-coca-buckets-distribution](./02-coca-buckets-distribution/) | Distribucion de frecuencia de palabras en bandas COCA |
| 3 | **LemmaByLevelAbsence** | [03-lemma-absence](./03-lemma-absence/) | Lemas esperados (EVP) que faltan en cada nivel |
| 4 | **LemmaRecurrence** | [04-lemma-recurrence](./04-lemma-recurrence/) | Espaciado y repeticion de lemas a lo largo del curso |

## Analyzers Inactivos / Complementarios

| # | Analyzer | Carpeta | Estado |
|---|----------|---------|--------|
| 5 | **LemmaCountAnalyzer** | [05-lemma-count](./05-lemma-count/) | INACTIVO - Comentado en el orquestador |
| 6 | **KnowledgeTitlesLength** | [06-knowledge-titles-length](./06-knowledge-titles-length/) | ACTIVO pero separado del framework VocabularyAnalyzer |

## Propuestas para la Migracion

| # | Propuesta | Carpeta | Descripcion |
|---|-----------|---------|-------------|
| 7 | **NGSLCoverageAnalyzer** | [07-propuesta-ngsl-coverage](./07-propuesta-ngsl-coverage/) | Nuevo analyzer propuesto: mide cobertura del vocabulario esencial (NGSL) |

## Recursos Compartidos

| Recurso | Carpeta |
|---------|---------|
| Archivos de datos y configuracion transversales | [recursos-compartidos](./recursos-compartidos/) |
| Flujo de orquestacion y datos de entrada | [orquestacion](./orquestacion/) |

---

## Dependencias Externas Criticas

<!-- Sugerencia: Estas dependencias son fundamentales para la migracion. Sin ellas, ningun analyzer puede funcionar. -->

1. **SpaCy (Docker)**: Procesamiento NLP de oraciones (lematizacion, POS tagging, tokenizacion)
2. **MongoDB**: Fuente de datos de los cursos (estructura CompleteCourseDTO)
3. **EVP (English Vocabulary Profile)**: Catalogo de vocabulario esperado por nivel CEFR
4. **COCA (Corpus of Contemporary American English)**: Rankings de frecuencia de palabras
5. **Enriched Vocabulary Catalog**: Catalogo enriquecido que combina EVP + COCA + SpaCy
6. **NGSL (propuesta)**: New General Service List - lista de 2,809 palabras esenciales para L2 (ver [propuesta](./07-propuesta-ngsl-coverage/))

---

## Modelo de Scoring

Cada analyzer produce un `overallScore` entre 0.0 y 1.0. El score final del curso es el **promedio** de los scores de todos los analyzers activos.

Ademas, se generan `CourseStats` con estadisticas desglosadas:
- Por nivel CEFR (A1, A2, B1, B2)
- Por topico dentro de cada nivel
- Por knowledge (ejercicio) dentro de cada topico
- Por sentence (oracion) dentro de cada knowledge

<!-- Cuidado aqui: El score del orquestador es un promedio simple. Si un analyzer tiene un score desproporcionadamente bajo, afecta mucho al resultado final. En la migracion, considerar si conviene usar un promedio ponderado. -->

---

## Checklist de Completitud para la Migracion

### Logica funcional de los analyzers

| Pieza | Documentada | Donde |
|-------|:-----------:|-------|
| SentenceLengthAnalyzer — logica completa | ✅ | [01-sentence-length](./01-sentence-length/) |
| CocaBucketsDistribution — logica completa | ✅ | [02-coca-buckets-distribution](./02-coca-buckets-distribution/) |
| CocaBuckets — interpolacion de quarters | ✅ | [02-coca-buckets-distribution/interpolacion-quarters.md](./02-coca-buckets-distribution/interpolacion-quarters.md) |
| CocaBuckets — configuracion ramp-up | ✅ | [02-coca-buckets-distribution/ramp-up-config.md](./02-coca-buckets-distribution/ramp-up-config.md) |
| LemmaByLevelAbsence — logica completa | ✅ | [03-lemma-absence](./03-lemma-absence/) |
| LemmaAbsence — tipos de ausencia | ✅ | [03-lemma-absence/tipos-de-ausencia.md](./03-lemma-absence/tipos-de-ausencia.md) |
| LemmaRecurrence — logica completa | ✅ | [04-lemma-recurrence](./04-lemma-recurrence/) |
| LemmaCount — logica completa (inactivo) | ✅ | [05-lemma-count](./05-lemma-count/) |
| KnowledgeTitlesLength — logica completa | ✅ | [06-knowledge-titles-length](./06-knowledge-titles-length/) |
| Algoritmos de scoring (todos los analyzers) | ✅ | Archivo `scoring-algorithm.md` en cada carpeta |

### Configuracion y targets

| Pieza | Documentada | Donde |
|-------|:-----------:|-------|
| config.yaml completo (copia) | ✅ | [recursos-compartidos/config-completo.yaml](./recursos-compartidos/config-completo.yaml) |
| Snippet de config por analyzer | ✅ | Archivo `config-snippet.yaml` en cada carpeta |
| Properties de ausencia critica | ✅ | [03-lemma-absence/critical-absent-lemmas-config.properties](./03-lemma-absence/critical-absent-lemmas-config.properties) |
| Properties de ausencia por nivel | ✅ | [03-lemma-absence/level-specific-absent-lemmas-config.properties](./03-lemma-absence/level-specific-absent-lemmas-config.properties) |
| Properties de pipeline | ✅ | [recursos-compartidos/pipeline-config.properties](./recursos-compartidos/pipeline-config.properties) |
| ContentWordFilter — valores por defecto | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |

### Modelo de datos

| Pieza | Documentada | Donde |
|-------|:-----------:|-------|
| CompleteCourseDTO (estructura del curso) | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| SentenceWithContext + SentenceContext | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| LemmaAndPos (clave de comparacion) | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| CourseStats (estructura completa de salida) | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| LevelStats, TopicStats, KnowledgeStats, SentenceStats | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| Enums: CEFRLevel, CoverageStatus, PriorityLevel | ✅ | [orquestacion/modelo-de-datos.md](./orquestacion/modelo-de-datos.md) |
| FrameworkVocabularyAnalysisResult (resultado principal) | ✅ | [orquestacion/README.md](./orquestacion/README.md) |

### Recursos de datos

| Pieza | Documentada | Donde |
|-------|:-----------:|-------|
| evp.json — estructura | ✅ | [recursos-compartidos/evp-structure.md](./recursos-compartidos/evp-structure.md) |
| enriched_vocabulary_catalog.json — estructura | ✅ | [recursos-compartidos/enriched-vocabulary-catalog-structure.md](./recursos-compartidos/enriched-vocabulary-catalog-structure.md) |
| lemmas_20k_words.txt — estructura | ✅ | [recursos-compartidos/lemmas-20k-structure.md](./recursos-compartidos/lemmas-20k-structure.md) |
| VocabularyLookupService — API | ✅ | [recursos-compartidos/vocabulary-lookup-service.md](./recursos-compartidos/vocabulary-lookup-service.md) |
| ContentWordFilter — logica | ✅ | [recursos-compartidos/content-word-filter.md](./recursos-compartidos/content-word-filter.md) |
| SpaCy processing — pipeline | ✅ | [recursos-compartidos/spacy-processing.md](./recursos-compartidos/spacy-processing.md) |
| Fix sentence prompt (GenAI) | ✅ | [recursos-compartidos/fix-sentence-prompt.md](./recursos-compartidos/fix-sentence-prompt.md) |

### Archivos de datos copiados (listos para usar)

| Archivo | Copiado | Donde |
|---------|:-------:|-------|
| config.yaml (completo) | ✅ | recursos-compartidos/config-completo.yaml |
| critical-absent-lemmas-config.properties | ✅ | 03-lemma-absence/ |
| level-specific-absent-lemmas-config.properties | ✅ | 03-lemma-absence/ |
| global-lemma-distribution-config.properties | ✅ | recursos-compartidos/ |
| lemma-progression-config.properties | ✅ | recursos-compartidos/ |
| pipeline-config.properties | ✅ | recursos-compartidos/ |
| fix-sentence-prompt.md | ✅ | recursos-compartidos/ |
| evp.json (~3.4 MB) | ❌ | Demasiado grande para copiar. Ruta: `src/main/resources/vocabulary/evp.json` |
| enriched_vocabulary_catalog.json (~11 MB) | ❌ | Demasiado grande para copiar. Ruta: `src/main/resources/vocabulary/enriched_vocabulary_catalog.json` |
| lemmas_20k_words.txt (~1.5 MB) | ❌ | Demasiado grande para copiar. Ruta: `src/main/resources/vocabulary/lemmas_20k_words.txt` |

<!-- Sugerencia: Los tres archivos grandes (evp.json, enriched_vocabulary_catalog.json, lemmas_20k_words.txt) deben copiarse directamente del proyecto original. Sus estructuras estan documentadas en recursos-compartidos/ para que sepas que contienen. -->

---

## Orden de Lectura Recomendado

1. **Este README** — Para entender que hay, como se relaciona, y que priorizar
2. [orquestacion](./orquestacion/) — Flujo completo + modelo de datos
3. [recursos-compartidos](./recursos-compartidos/) — Datos de entrada y componentes transversales
4. Cada analyzer en orden numerico (01 a 06)
5. [propuesta de NGSL Coverage](./07-propuesta-ngsl-coverage/) — Nuevo analyzer
