# LemmaCocaBucketsDistribution

## Proposito

Evalua como se **distribuyen las palabras del curso segun su frecuencia de uso en ingles** (basado en el corpus COCA). La idea pedagogica es:

- En niveles iniciales (A1), la mayoria de las palabras deben ser de **alta frecuencia** (top 1000 mas usadas)
- En niveles avanzados (B2), debe haber mas palabras de **baja frecuencia** (top 4000+)
- La transicion debe ser **gradual y consistente** de nivel a nivel

Este es el analyzer **mas complejo** del sistema.

---

## Conceptos Clave

### Bandas de Frecuencia COCA (Buckets)

Las palabras se clasifican en bandas segun su ranking de frecuencia en el corpus COCA:

| Bucket | Rango | Significado |
|--------|-------|-------------|
| **top1k** | 1 - 1000 | Palabras mas frecuentes del ingles |
| **top2k** | 1001 - 2000 | Palabras frecuentes |
| **top3k** | 2001 - 3000 | Palabras de frecuencia media |
| **top4k** | 3001 - 4000+ | Palabras de baja frecuencia (bucket abierto: incluye 4000+) |

<!-- Cuidado aqui: El bucket "top4k" es ABIERTO (open: true). Esto significa que cualquier palabra con ranking >= 3001 cae en este bucket. No hay limite superior. Esto incluye palabras raras (ranking 10000+) que quizas deberian tratarse distinto. -->

### Estrategias de Analisis

El analyzer soporta dos estrategias:

| Estrategia | Descripcion |
|------------|-------------|
| **LEVELS** | Analiza la distribucion por nivel CEFR completo (A1, A2, B1, B2) |
| **QUARTERS** | Subdivide cada nivel en 4 trimestres (Q1, Q2, Q3, Q4) con interpolacion lineal |

**La configuracion actual usa `quarters`.**

<!-- Sugerencia: La estrategia "quarters" es mas granular y permite ver progresion dentro de cada nivel. Si el sistema destino no tiene concepto de "trimestre dentro de un nivel", quizas convenga migrar solo con la estrategia "levels". -->

### Interpolacion Lineal de Trimestres

Cuando se usa la estrategia `quarters`, el sistema define targets solo para el **trimestre inicial** y el **trimestre final** de cada nivel. Los trimestres intermedios se calculan por **interpolacion lineal**:

```
Para un nivel con initialQuarter y lastQuarter:
  Q1 = initialQuarter targets
  Q2 = interpolacion(initialQuarter, lastQuarter, 1/3)
  Q3 = interpolacion(initialQuarter, lastQuarter, 2/3)
  Q4 = lastQuarter targets
```

---

## Datos de Entrada

- **CourseSentences**: Oraciones procesadas con SpaCy (cada token tiene `frequencyRank`)
- **CocaBucketDistributionConfig**: Configuracion de buckets, targets por nivel, y rangos (ver [config-snippet.yaml](./config-snippet.yaml))
- **Buckets**: Definicion de las bandas de frecuencia

---

## Logica Funcional Detallada

### 1. Clasificacion de Palabras en Buckets

Para cada oracion procesada por SpaCy:
1. Se obtiene el `frequencyRank` de cada token (posicion en la lista de frecuencia COCA)
2. Se asigna el token al bucket correspondiente segun su ranking
3. Se contabiliza cuantos tokens caen en cada bucket

### 2. Calculo de Porcentajes por Nivel

Para cada nivel CEFR (y cada quarter si aplica):
- Se calcula el **porcentaje** de tokens en cada bucket respecto al total del nivel/quarter
- Ejemplo: Si en A1 hay 100 tokens y 82 son top1k -> top1k = 82%

### 3. Evaluacion contra Targets (Assessment)

Para cada bucket en cada nivel, se compara el porcentaje real contra el target:

| Estado | Condicion |
|--------|-----------|
| `OPTIMAL` | \|real - target\| <= optimalRange (5%) |
| `ADEQUATE` | \|real - target\| <= adequateRange (10%) |
| `DEFICIENT` | real < target - adequateRange |
| `EXCESSIVE` | real > target + adequateRange |

Los targets incluyen un `kind` que define la direccion:
- `atLeast`: El porcentaje debe ser **al menos** el target
- `atMost`: El porcentaje debe ser **como maximo** el target

### 4. Evaluacion de Progresion por Bucket

Se evalua si cada bucket **progresa correctamente** de nivel a nivel:

| Bucket | Progresion Esperada | Significado |
|--------|-------------------|-------------|
| top1k | **DESCENDING** | Menos palabras frecuentes en niveles altos |
| top4k | **ASCENDING** | Mas palabras raras en niveles altos |

La evaluacion compara los porcentajes reales entre niveles y determina:
- `ASCENDING`: El porcentaje sube consistentemente
- `DESCENDING`: El porcentaje baja consistentemente
- `STATIC`: Sin cambios significativos
- `IRREGULAR`: Sube y baja sin patron claro

### 5. Scoring

Ver detalle en [scoring-algorithm.md](./scoring-algorithm.md).

---

## Targets por Nivel (Resumen)

### Nivel A1
| Bucket | Target | Kind |
|--------|--------|------|
| top1k | 80% | atLeast |
| top4k | 1% | atMost |

**Initial Quarter (Q1):** top1k >= 90%, top4k <= 0%
**Last Quarter (Q4):** top1k >= 75%, top4k <= 2%

### Nivel A2
| Bucket | Target | Kind |
|--------|--------|------|
| top1k | 70% | atMost |
| top4k | 10% | atMost |

**Q1:** top1k >= 75%, top4k <= 2%
**Q4:** top1k >= 65%, top4k <= 15%

### Nivel B1
| Bucket | Target | Kind |
|--------|--------|------|
| top1k | 60% | atMost |
| top4k | 20% | atMost |

**Q1:** top1k >= 65%, top4k <= 15%
**Q4:** top1k >= 55%, top4k <= 25%

### Nivel B2
| Bucket | Target | Kind |
|--------|--------|------|
| top1k | 50% | atMost |
| top4k | 30% | atLeast |

**Q1:** top1k >= 55%, top4k <= 25%
**Q4:** top1k >= 45%, top4k <= 35%

---

## Ramp-Up (Configuracion Especial)

Existe una configuracion alternativa para cursos "ramp-up" (introductorios) que usa bandas de frecuencia diferentes y targets por unidad:

| Bucket | Valor |
|--------|-------|
| 135 | Top 135 palabras |
| 250 | Top 250 palabras |
| 500 | Top 500 palabras |
| 1000 | Top 1000 palabras |

Ver detalles completos en [ramp-up-config.md](./ramp-up-config.md).

<!-- Cuidado aqui: La configuracion de ramp-up esta definida en el YAML pero no esta claro si se usa activamente. En el orquestador solo se referencia `targetsConfig.getCocaBucketsDistribution().getCourse()`, no la seccion rampUp. Verificar si se usa en otro flujo. -->

---

## Resultado (LemmaCocaBucketsDistributionResult)

```json
{
  "levels": [
    {
      "name": "A1",
      "score": 0.9,
      "buckets": [
        {"name": "top1k", "count": 850, "percentage": 85.0, "targetPercentage": 80.0, "score": 1.0, "assessment": "OPTIMAL"}
      ],
      "quarters": [
        {"index": 1, "buckets": [...], "score": 0.95},
        {"index": 2, "buckets": [...], "score": 0.92}
      ],
      "children": [
        {"id": "topic1", "name": "Present Simple", "buckets": [...], "children": [...]}
      ]
    }
  ],
  "bucketProgressionAssessments": [
    {"bucketName": "top1k", "actualProgression": "DESCENDING", "targetProgression": "DESCENDING"},
    {"bucketName": "top4k", "actualProgression": "ASCENDING", "targetProgression": "ASCENDING"}
  ],
  "overallScore": 0.85
}
```

<!-- Sugerencia: La estructura del resultado es JERARQUICA: Nivel -> Quarter -> Topic -> Knowledge -> Buckets. Esto permite drill-down en la UI pero puede ser pesado de serializar/almacenar. En la migracion, evaluar si se necesita toda esta granularidad. -->

---

## Planner de Mejoras (LemmaCocaBucketsPlanner)

Ademas del analisis, este modulo incluye un **planner** que genera directivas de mejora:

- Para cada nivel/quarter que no alcanza los targets, genera:
  - `bucketsToEnrich`: Bandas donde faltan palabras (necesita MAS palabras de esa frecuencia)
  - `bucketsToReduce`: Bandas donde sobran palabras (necesita MENOS palabras de esa frecuencia)
  - Cada directiva incluye el rango de frecuencia (from/to) para buscar palabras de reemplazo

---

## Integracion con CourseStats

Este analyzer alimenta estadisticas a multiples niveles:
- **Por nivel**: `LevelStats.cocaBuckets` con score y distribucion
- **Por topico**: Distribucion de buckets dentro de cada topico
- **Por knowledge**: Distribucion de buckets por ejercicio
- **Por oracion**: Score de frecuencia individual

---

## Archivos Relevantes para Migracion

| Archivo | Descripcion |
|---------|-------------|
| `config.yaml` -> seccion `cocaBucketsDistribution` | Toda la configuracion de buckets, targets, y progresion |
| `enriched_vocabulary_catalog.json` | Fuente de `frequencyRank` para cada palabra |
| `lemmas_20k_words.txt` | Datos brutos de frecuencia COCA |

---

## Mi Opinion

**Este es el analyzer mas critico y complejo del sistema.** Merece la mayor atencion en la migracion.

**Fortalezas:**
- Modelo pedagogicamente solido: la progresion de vocabulario por frecuencia es un principio bien establecido en la ensenanza de idiomas
- La interpolacion lineal de quarters permite evaluacion granular
- El planner de mejoras es un diferenciador valioso

**Puntos de atencion para la migracion:**

1. **Complejidad de la configuracion**: La configuracion YAML es extensa y tiene multiples niveles de anidamiento. Un error en los targets puede dar resultados enganosos. Recomiendo validacion automatica.

<!-- Cuidado aqui: Hay una inconsistencia en la configuracion. Para A1, top1k es "atLeast 80%" (quiere >= 80%), pero para A2, top1k es "atMost 70%". El cambio de semantica entre niveles puede causar confusion. -->

2. **Datos de frecuencia COCA**: Todo depende del `frequencyRank` de cada palabra. Si la fuente de datos de frecuencia cambia o no esta disponible, este analyzer no puede funcionar. En la migracion, el `enriched_vocabulary_catalog.json` es **absolutamente necesario**.

3. **Bucket abierto (top4k)**: Agrupa todo lo que es 3001+ en un solo bucket. Para cursos de nivel C1/C2 (si se agregan en el futuro), se necesitarian mas bandas.

4. **Ramp-Up**: La configuracion de ramp-up existe pero su uso no es claro en el flujo principal. Verificar si hay otro orquestador que la use.

5. **Jerarquia de resultados**: La estructura Topic -> Knowledge -> Sentence es rica pero costosa. Si el destino no necesita este nivel de detalle, se puede simplificar.

6. **Performance**: Este analyzer procesa CADA token de CADA oracion. Para cursos grandes, puede ser lento. Considerar procesamiento incremental si el sistema destino lo soporta.
