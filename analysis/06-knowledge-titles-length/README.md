# KnowledgeTitlesLength

> **NOTA:** Este analyzer NO implementa la interfaz `VocabularyAnalyzer`. Se ejecuta de forma separada en el orquestador, directamente sobre la lista de knowledges del curso.

## Proposito

Evalua la **longitud de los titulos (labels) e instrucciones** de cada ejercicio (knowledge). La premisa es que:
- Los titulos deben ser concisos (maximo 28 caracteres)
- Las instrucciones no deben ser ni muy cortas ni excesivamente largas
- Hay un **soft limit** (70 caracteres) y un **hard limit** (100 caracteres) para instrucciones

---

## Configuracion

| Parametro | Valor | Significado |
|-----------|-------|-------------|
| `labelMaxLength` | 28 | Maximo de caracteres para el titulo del ejercicio |
| `instructionsSoftMaxLength` | 70 | Limite suave para instrucciones (score = 0.5) |
| `instructionsHardMaxLength` | 100 | Limite duro para instrucciones (score = 0.0) |

---

## Logica Funcional

### 1. Analisis de Titulos (Labels)

Para cada knowledge:
1. Se obtiene el texto del titulo
2. Se calcula la **longitud ponderada** (no es simplemente `length()`)
3. Se compara contra `labelMaxLength` (28)

### 2. Longitud Ponderada

Los caracteres NO se cuentan igual. Algunos caracteres "pesan" menos porque son visualmente mas estrechos:

| Peso | Caracteres |
|------|-----------|
| 0.0 | `$`, `*` (caracteres de formato, no cuentan) |
| 0.5 | `i`, `,`, `.` (caracteres estrechos) |
| 0.7 | `f`, `t`, `"` (caracteres medio-estrechos) |
| 1.0 | Todos los demas |

<!-- Sugerencia: Esto parece disenado para UIs con fuente proporcional donde "i" es mas angosto que "m". Si el destino usa fuente monoespaciada, todos los caracteres deberian pesar 1.0. -->

### 3. Analisis de Instrucciones

Para cada knowledge:
1. Se obtiene el texto de las instrucciones
2. Se calcula la longitud (en este caso parece ser longitud simple, no ponderada)
3. Scoring:
   - Si longitud <= softLimit (70) -> score = 1.0
   - Si longitud > softLimit Y <= hardLimit (100) -> score = 0.5
   - Si longitud > hardLimit (100) -> score = 0.0

### 4. Integracion con CourseStats

El resultado se integra en `KnowledgeStats` con:
- Score de longitud del label
- Score de longitud de instrucciones
- Se agrega al `exerciseLengthScore` en las estadisticas del nivel

---

## Resultado

Este analyzer no produce un `VocabularyAnalysisResult`. En su lugar, alimenta directamente las estadisticas de `CourseStats` a nivel de knowledge.

---

## Config Snippet

```yaml
# De config.yaml -> targets.knowledgeLengths
knowledgeLengths:
  labelMaxLength: 28
  instructionsSoftMaxLength: 70
  instructionsHardMaxLength: 100
```

---

## Mi Opinion

**Este analyzer es simple pero practico.** Verifica restricciones de UI/UX que afectan la experiencia del estudiante.

**Puntos de atencion para la migracion:**

1. **Pesos de caracteres**: El sistema de pesos (0.0, 0.5, 0.7, 1.0) parece especifico para una fuente particular. Si la UI del destino usa otra fuente, estos pesos deberian recalibrarse.

2. **No es parte del framework**: Al no implementar `VocabularyAnalyzer`, este analyzer no aporta al `overallScore` del analisis de vocabulario. Sus resultados solo aparecen en `CourseStats` como `exerciseLengthScore`.

<!-- Cuidado aqui: Si la migracion incluye un nuevo sistema de scoring, asegurate de decidir si KnowledgeTitlesLength debe integrarse al score general o mantenerse separado. Actualmente NO afecta al overallScore del FrameworkVocabularyAnalysisResult. -->

3. **Solo evalua longitud, no calidad**: No se evalua si el titulo o las instrucciones son claras, coherentes o en el idioma correcto. Solo se mide la longitud.
