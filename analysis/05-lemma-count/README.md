# LemmaCountAnalyzer

> **ESTADO: INACTIVO** - Este analyzer NO esta registrado en el orquestador. Existe en el codigo pero esta comentado. Se documenta por completitud y porque podria reactivarse.

## Proposito

Evalua cuantas veces aparece cada **lema esperado** (del catalogo EVP) en el curso completo. La premisa es que un lema de vocabulario objetivo deberia aparecer un numero minimo de veces para que el estudiante lo aprenda, pero no tantas veces que sature.

---

## Configuracion

| Parametro | Valor | Significado |
|-----------|-------|-------------|
| `subExposed` | 4 | Si un lema aparece menos de 4 veces -> sub-expuesto |
| `overExposed` | 15 | Si un lema aparece mas de 15 veces -> sobre-expuesto |

---

## Logica Funcional

### 1. Recoleccion de Datos

Para cada nivel CEFR:
1. Se obtienen los **lemas esperados** del EVP
2. Se obtienen los **lemas presentes** en las oraciones (con contexto de oracion)
3. Se cuenta cuantas veces aparece cada lema esperado

### 2. Clasificacion

| Estado | Condicion | Score |
|--------|-----------|-------|
| `absent` | count == 0 | 0.0 |
| `sub-exposed` | 0 < count < 4 | count / 4 (lineal) |
| `normal` | 4 <= count <= 15 | 1.0 |
| `over-exposed` | count > 15 | max(0, 1 - (count - 15) / 50) |

### 3. Scoring

- **Por lema**: Score individual segun la tabla anterior
- **Por oracion**: Promedio de los scores de los lemas esperados que aparecen en esa oracion
- **General**: Promedio de todos los scores de lemas individuales

---

## Diferencia con LemmaRecurrence

| Aspecto | LemmaCount | LemmaRecurrence |
|---------|------------|-----------------|
| Que mide | Cuantas veces aparece un lema | A que intervalos aparece un lema |
| Alcance | Solo lemas del EVP | Top 2000 lemas por frecuencia |
| Granularidad | Score por oracion | Solo score global |
| Estado | INACTIVO | ACTIVO |

<!-- Sugerencia: LemmaCount y LemmaRecurrence son complementarios. LemmaCount mide "cuanto" y LemmaRecurrence mide "cuando". Para una migracion completa, considerar reactivar LemmaCount. -->

---

## Resultado (LemmaCountResult)

```json
{
  "lemmaStats": [
    {
      "lemma": {"lemma": "cat", "pos": "NOUN"},
      "level": "A1",
      "count": 6,
      "exposureStatus": "normal",
      "score": 1.0
    },
    {
      "lemma": {"lemma": "ameliorate", "pos": "VERB"},
      "level": "B2",
      "count": 1,
      "exposureStatus": "sub-exposed",
      "score": 0.25
    }
  ],
  "overallScore": 0.72
}
```

---

## Evidencia de Inactividad

En `FrameworkVocabularyAnalysisResult.java`:
```java
//    @JsonProperty("lemmaCountResult")
//    private final LemmaCountResult lemmaCountResult;
```

En `VocabularyAnalyzerOrchestrator.main()`, no aparece en la lista de analyzers.

En `CourseStats.Builder.build()`, hay codigo comentado para LemmaCount:
```java
//  if (lemmaCountResult != null) { ... }
```

---

## Mi Opinion

**Este analyzer es valioso y deberia considerarse para reactivacion en la migracion.**

El conteo de apariciones es un metrica fundamental para evaluar si el estudiante tiene suficiente exposicion a cada palabra. La razon por la que esta inactivo no es clara - posiblemente por performance o porque se considero redundante con LemmaRecurrence.

<!-- Cuidado aqui: El scoring de "over-exposed" degrada linealmente con factor 1/50. Esto significa que un lema que aparece 65 veces tendria score = 0. Pero para palabras como "be", "have", "the" que naturalmente aparecen cientos de veces, esto seria un falso positivo. El analyzer ya filtra por "lemas esperados del EVP" pero aun asi, verbos basicos como "be" en A1 facilmente superan las 15 apariciones. -->

**Puntos de atencion:**
1. El umbral de `overExposed = 15` puede ser bajo para lemas basicos de A1
2. La degradacion lineal con factor 1/50 es arbitraria
3. A diferencia de LemmaRecurrence, SI genera scores por oracion (mas granular)
