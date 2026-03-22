# SentenceLengthAnalyzer

## Proposito

Evalua si la **longitud promedio de las oraciones** en cada nivel CEFR es apropiada para el nivel de dificultad esperado. La premisa pedagogica es que las oraciones deben ser mas cortas en niveles iniciales (A1) y progresivamente mas largas en niveles avanzados (B2).

---

## Datos de Entrada

- **CourseSentences**: Oraciones del curso agrupadas por nivel CEFR
- Cada oracion tiene un `SpacyProcessingResult` con `tokenCount` (numero de tokens/palabras)
- Configuracion: `TargetsConfig.SentenceLength` (ver [config-snippet.yaml](./config-snippet.yaml))

---

## Logica Funcional Detallada

### 1. Iteracion por nivel CEFR

Para cada nivel (A1, A2, B1, B2):

1. Se obtienen todas las oraciones del nivel
2. **Filtro de no-oraciones**: Si un `Knowledge` esta marcado como `isSentence() == false`, se excluye del calculo y se marca como `noSentence` en las estadisticas
   <!-- Sugerencia: Este filtro es clave. Hay ejercicios que no son oraciones propiamente dichas (ej: etiquetas, listas). Asegurate de migrar la logica de `isSentence()` del modelo Knowledge. -->
3. Se cuenta el total de palabras (`tokenCount` de SpaCy) y el total de oraciones validas
4. Se calcula el **promedio de longitud** = total palabras / total oraciones

### 2. Evaluacion de Estado (CoverageStatus)

Compara el promedio contra los rangos objetivo del nivel:

| Estado | Condicion |
|--------|-----------|
| `OPTIMAL` | promedio >= min AND promedio <= max |
| `DEFICIENT` | promedio < min |
| `EXCESSIVE` | promedio > max |
| `NOT_APPLICABLE` | No hay target o promedio = 0 |

### 3. Scoring por Oracion Individual

Cada oracion individual tambien recibe un score con la funcion `getScore(length, targetMin, targetMax)`:

```
Si length esta dentro del rango [min, max] -> score = 1.0
Si esta fuera del rango:
    distance = distancia al borde mas cercano del rango
    Si distance >= 4 -> score = 0.0
    Si distance < 4  -> score = 1.0 - (distance / 4.0)
```

<!-- Cuidado aqui: El scoring es LINEAL con una pendiente fija de 4 tokens de margen. Esto significa que una oracion que esta 2 tokens fuera del rango recibe 0.5, y 3 tokens fuera recibe 0.25. Este valor de "4" esta hardcodeado, no es configurable. -->

### 4. Evaluacion de Progresion (ProgressionStatus)

Evalua si la longitud promedio **crece** de nivel en nivel (A1 -> A2 -> B1 -> B2):

| Estado | Significado |
|--------|-------------|
| `POSITIVE` | La longitud promedio crece consistentemente |
| `REGRESSIVE` | La longitud promedio decrece consistentemente |
| `STAGNANT` | La longitud sube y baja (o es plana) |
| `INSUFFICIENT_DATA` | Menos de 2 niveles con datos |

### 5. Score General

El `overallScore` es el **promedio** de los scores de todos los niveles que tienen metricas.

---

## Rangos Objetivo por Nivel

(Ver archivo completo en [config-snippet.yaml](./config-snippet.yaml))

| Nivel | Min palabras | Max palabras |
|-------|-------------|-------------|
| A1 | 5 | 8 |
| A2 | 8 | 11 |
| B1 | 11 | 14 |
| B2 | 14 | 17 |

---

## Resultado (SentenceLengthResult)

```json
{
  "metricsByLevel": {
    "A1": {
      "level": "A1",
      "sentenceCount": 120,
      "totalWords": 780,
      "averageLength": 6.5,
      "targetMin": 5,
      "targetMax": 8,
      "status": "OPTIMAL",
      "recommendation": "Sentence length is within the optimal range for this level."
    }
  },
  "overallAverage": 10.3,
  "progressionStatus": "POSITIVE",
  "overallScore": 0.85
}
```

---

## Integracion con CourseStats

Este analyzer alimenta las estadisticas a nivel de oracion individual:
- `sentenceBuilder.sentenceLength(score, length, targetMin, targetMax)` - Score de longitud por oracion
- `sentenceBuilder.noSentence()` - Marca oraciones que no son oraciones

Y a nivel del builder general:
- `courseStatsBuilder.sentenceLengthResult(result)` - Se registra en el builder cuando el orquestador detecta el tipo de resultado

---

## Archivos Relevantes para Migracion

| Archivo | Descripcion |
|---------|-------------|
| `config.yaml` -> seccion `sentenceLength` | Rangos objetivo por nivel |
| SpaCy `tokenCount` | La longitud se mide en tokens de SpaCy, no en palabras separadas por espacios |
| Knowledge.isSentence() | Flag que determina si un ejercicio es una oracion evaluable |

---

## Mi Opinion

<!-- Sugerencia: Este es el analyzer mas simple y directo del sistema. La migracion deberia ser straightforward. -->

**Fortalezas:**
- Logica clara y facil de replicar
- Configuracion sencilla (solo rangos min/max por nivel)
- El scoring lineal con margen de 4 tokens es razonable

**Puntos de atencion para la migracion:**

1. **Dependencia de SpaCy tokenCount**: La longitud no se mide en "palabras separadas por espacios" sino en tokens linguesticos de SpaCy. Por ejemplo, "don't" podria ser 1 o 2 tokens dependiendo del tokenizer. Esto es importante replicar exactamente.

2. **El filtro `isSentence()`**: Es critico que el modelo de datos del destino tenga esta distincion. Sin ella, se mezclarian etiquetas o instrucciones con oraciones reales, distorsionando los promedios.

3. **Valor magico "4"**: El margen de scoring (4 tokens) esta hardcodeado. Si necesitas ajustarlo, tendras que parametrizarlo.

<!-- Cuidado aqui: Los rangos configurados asumen contenido de cursos de ingles para adultos. Si se migran cursos para ninos o cursos especializados, los rangos podrian necesitar ajuste. -->

4. **La progresion se evalua solo entre niveles presentes**: Si un curso no tiene contenido en B1, se evalua A1->A2->B2 directamente. Esto puede dar resultados enganosos.
