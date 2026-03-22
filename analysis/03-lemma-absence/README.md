# LemmaByLevelAbsence

## Proposito

Detecta **lemas (formas base de palabras) que deberian estar presentes en un nivel CEFR pero no aparecen en el curso**. Utiliza el catalogo EVP (English Vocabulary Profile) como referencia de "que palabras se esperan en cada nivel".

La premisa pedagogica es que si el EVP indica que "cat" es una palabra de nivel A1, entonces "cat" (o alguna de sus formas: cats, cat's) deberia aparecer al menos una vez en el contenido de A1.

---

## Conceptos Clave

### Tipos de Ausencia

El analyzer no solo detecta palabras completamente ausentes, sino que clasifica el tipo de ausencia:

| Tipo | Descripcion | Impacto |
|------|-------------|---------|
| `COMPLETELY_ABSENT` | El lema no aparece en ninguna oracion de ningun nivel | El mas critico |
| `APPEARS_TOO_EARLY` | El lema aparece pero en un nivel anterior al esperado (ej: palabra B1 en A1) | Medio |
| `APPEARS_TOO_LATE` | El lema aparece pero en un nivel posterior al esperado (ej: palabra A1 en B2) | Medio-Alto |
| `SCATTERED_PLACEMENT` | El lema aparece en multiples niveles incorrectos sin patron claro | Bajo |

### Niveles de Prioridad

Cada lema ausente recibe un nivel de prioridad basado en su **ranking de frecuencia COCA**:

| Prioridad | Condicion | Significado |
|-----------|-----------|-------------|
| `HIGH` | cocaRank < 1000 | Palabra muy frecuente, critica para la comunicacion |
| `MEDIUM` | cocaRank < 2000 | Palabra frecuente, importante |
| `LOW` | cocaRank < 3000 | Palabra de frecuencia media |

<!-- Cuidado aqui: Los umbrales de prioridad estan en config.yaml (prioritiesByCocaRank). Pero tambien hay otros umbrales en critical-absent-lemmas-config.properties que usan rangos diferentes (1000, 2500, 5000). Verificar cual se usa realmente en la migracion. -->

### Thresholds por Nivel

Cantidad maxima de lemas ausentes aceptables antes de generar una alerta:

| Nivel | Threshold (config.yaml) |
|-------|------------------------|
| A1 | 5 |
| A2 | 10 |
| B1 | 15 |
| B2 | 20 |

<!-- Sugerencia: Los thresholds en config.yaml son mas permisivos que los de level-specific-absent-lemmas-config.properties (que define A1=0, A2=2, B1=5, B2=8). Verificar cual set de thresholds es el que se usa efectivamente. -->

---

## Datos de Entrada

- **CourseSentences**: Proporciona:
  - `getLemmasByLevel()` - Lemas presentes en cada nivel (extraidos de las oraciones procesadas por SpaCy)
  - `getExpectedLemmasByLevel()` - Lemas esperados en cada nivel (del catalogo EVP/enriched vocabulary)
- **VocabularyLookupService**: Para enriquecer la informacion de cada lema ausente (ranking COCA, categoria semantica, etc.)
- **ContentWordFilter**: Para filtrar solo palabras de contenido (excluir palabras funcionales como "the", "is", "a")
- **TargetsConfig.LemmaAbsence**: Umbrales y prioridades

---

## Logica Funcional Detallada

### 1. Identificacion de Lemas Ausentes

Para cada nivel CEFR:
1. Se obtienen los **lemas esperados** del catalogo EVP (filtrados: solo lemas, no frases; solo content words)
2. Se obtienen los **lemas presentes** en las oraciones del nivel
3. La diferencia = **lemas ausentes** en ese nivel
4. Se excluyen frases (`isPhrase() == true`) del EVP

<!-- Cuidado aqui: El comparador de lemas usa LemmaAndPos (lema + part-of-speech). Esto significa que "run" como verbo y "run" como sustantivo se consideran distintos. Si el EVP espera "run" como verbo pero el curso solo tiene "run" como sustantivo, se marca como ausente. -->

### 2. Clasificacion del Tipo de Ausencia

Para cada lema ausente:
```
Si el lema NO aparece en NINGUN nivel:
    tipo = COMPLETELY_ABSENT

Si el lema aparece en otros niveles:
    Si solo aparece en niveles ANTERIORES al esperado:
        tipo = APPEARS_TOO_EARLY
    Si solo aparece en niveles POSTERIORES al esperado:
        tipo = APPEARS_TOO_LATE
    Si aparece en niveles mezclados:
        tipo = SCATTERED_PLACEMENT
```

### 3. Enriquecimiento de Informacion

Para cada lema ausente, se enriquece con:
- **cocaRank**: Ranking de frecuencia COCA (via VocabularyLookupService)
- **semanticCategory**: Categoria semantica/topic (del catalogo EVP)
- **priorityLevel**: Prioridad basada en el cocaRank
- **totalFrequency**: Frecuencia total en el corpus
- **partOfSpeech**: Parte de la oracion

### 4. Scoring por Oracion (processSentenceStats)

Para cada oracion del curso:
1. Se identifican los lemas que aparecen en la oracion
2. Para cada lema que pertenece a un nivel distinto al de la oracion:
   - Se aplica un **descuento de 0.1 por cada nivel de diferencia**
   - Ejemplo: Si un lema A1 aparece en B1 (2 niveles de distancia) -> descuento = 0.2
3. El score de la oracion = 1.0 - max(descuentos)

### 5. Assessment Global

Se genera un assessment con tres categorias:

| Categoria | Condicion |
|-----------|-----------|
| `OPTIMAL` | Todos los niveles estan dentro del threshold |
| `ACCEPTABLE` | Algunos niveles exceden pero no criticamente |
| `NEEDS_IMPROVEMENT` | Niveles criticos (A1/A2) exceden el threshold |

Se priorizan los niveles A1 y A2 como "criticos" (los niveles iniciales son mas sensibles a la ausencia de vocabulario basico).

### 6. Generacion de Recomendaciones

Para cada nivel con problemas, se generan recomendaciones:
- **action**: Que hacer (ej: "Add missing A1 vocabulary")
- **description**: Descripcion detallada
- **priority**: HIGH/MEDIUM/LOW
- **affectedLemmas**: Lista de lemas afectados
- **effortLevel**: Esfuerzo estimado
- **expectedImpact**: Impacto esperado

---

## Resultado (LevelSpecificAbsentLemmasResult)

```json
{
  "metricsByLevel": {
    "A1": {
      "totalExpectedLemmas": 150,
      "totalAbsentLemmas": 3,
      "absencePercentage": 2.0,
      "absentLemmasByType": {
        "COMPLETELY_ABSENT": [
          {
            "lemma": "cat",
            "expectedLevel": "A1",
            "absenceType": "COMPLETELY_ABSENT",
            "cocaRank": 2145,
            "priorityLevel": "MEDIUM",
            "partOfSpeech": "NOUN"
          }
        ]
      }
    }
  },
  "overallAssessment": {
    "assessmentCategory": "ACCEPTABLE",
    "score": 0.87,
    "criticalIssues": [...],
    "globalRecommendations": [...]
  },
  "recommendations": [...],
  "overallScore": 0.87
}
```

---

## Archivos Relevantes para Migracion

| Archivo | Descripcion | Ruta |
|---------|-------------|------|
| config.yaml -> lemmaAbsence | Prioridades y thresholds | [config-snippet.yaml](./config-snippet.yaml) |
| critical-absent-lemmas-config.properties | Config detallada de criticidad | [critical-absent-lemmas-config.properties](./critical-absent-lemmas-config.properties) |
| level-specific-absent-lemmas-config.properties | Thresholds alternativos por nivel | [level-specific-absent-lemmas-config.properties](./level-specific-absent-lemmas-config.properties) |
| enriched_vocabulary_catalog.json | Fuente de lemas esperados y enriquecimiento | Ver [recursos-compartidos](../recursos-compartidos/) |
| evp.json | Fuente original del EVP | Ver [recursos-compartidos](../recursos-compartidos/) |

---

## Mi Opinion

**Este analyzer es fundamental para la calidad pedagogica.** Si faltan palabras basicas en un nivel, el estudiante tendra lagunas de vocabulario.

**Fortalezas:**
- La clasificacion de tipos de ausencia (COMPLETELY_ABSENT vs APPEARS_TOO_LATE) es muy util para priorizar correcciones
- El sistema de prioridades por frecuencia COCA ayuda a enfocarse en lo que importa
- Las recomendaciones generadas son accionables

**Puntos de atencion para la migracion:**

1. **Dependencia del EVP**: Sin el catalogo EVP (o el enriched_vocabulary_catalog.json), este analyzer no puede funcionar. Es la fuente de verdad de "que se espera en cada nivel".

2. **LemmaAndPos como clave**: La comparacion usa lema + POS. Esto es correcto linguisticamente pero puede causar falsos positivos si el POS tagging no es consistente entre el EVP y SpaCy.

<!-- Cuidado aqui: El EVP original tiene campos `partOfSpeech` a veces vacios (""). El enriched catalog usa SpaCy para asignar POS. Si hay discrepancias, un lema podria considerarse ausente porque el POS no matchea. -->

3. **Content Word Filter**: Solo se evaluan palabras de contenido. Las palabras funcionales (the, is, a, and, etc.) se excluyen. Esto es correcto pedagogicamente pero significa que si el EVP incluye "be" como verbo esperado en A1, se filtraria por ser un auxiliar.

4. **Descuento por distancia de nivel**: El scoring de oraciones aplica un descuento de 0.1 por nivel de distancia. Esto es una heuristica razonable pero los valores estan hardcodeados.

5. **Doble configuracion de thresholds**: Hay thresholds en config.yaml (5, 10, 15, 20) y en level-specific-absent-lemmas-config.properties (0, 2, 5, 8). Esto puede causar confusion. Verificar cual se usa en produccion.

6. **Assessment de niveles criticos**: A1 y A2 se priorizan como criticos. Para cursos que no tienen A1 (cursos avanzados), esta logica deberia adaptarse.
