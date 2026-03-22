# Propuesta: NGSLCoverageAnalyzer

> **ESTADO: PROPUESTA** — Este analyzer no existe en el sistema actual. Este documento describe cómo se podría implementar y qué valor agregaría.

## Qué es la NGSL

La **New General Service List** (NGSL) es una lista de **2,809 lemas** creada por Dr. Charles Browne y Dr. Brent Culligan (2013, actualizada en 2023 como NGSL 1.2). Está basada en un corpus de **273 millones de palabras** del Cambridge English Corpus.

**El dato clave:** esas ~2,800 palabras cubren **más del 92% del inglés general** que un estudiante encontrará en textos, conversaciones, TV, exámenes, etc.

### ¿Por qué es relevante?

Los analyzers actuales evalúan:
- Si las palabras son del nivel correcto (EVP / Absence)
- Si la frecuencia es apropiada (COCA / CocaBuckets)
- Si las palabras se repiten bien (Recurrence)
- Si las oraciones tienen longitud correcta (SentenceLength)

**Pero ninguno responde a esta pregunta fundamental:**

> "¿Cuánto del inglés esencial cubre este curso? Si un estudiante completa el nivel A2, ¿qué porcentaje del inglés que va a encontrar en la vida real puede comprender?"

La NGSL responde exactamente eso.

---

## Valor que Agrega

### 1. Métrica de Cobertura Acumulativa

```
Después de completar A1: el estudiante conoce X% de las palabras NGSL
Después de completar A2: el estudiante conoce Y% de las palabras NGSL
Después de completar B1: el estudiante conoce Z% de las palabras NGSL
Después de completar B2: el estudiante conoce W% de las palabras NGSL
```

Esto traduce el progreso del curso a una métrica que **cualquier persona no técnica entiende**: "Al terminar A2, vas a entender el 65% de lo que leas en inglés".

<!-- Sugerencia: Esta métrica es un diferenciador comercial. Poder decirle a un estudiante "al terminar este nivel entenderás el X% del inglés cotidiano" es un argumento de venta potente. -->

### 2. Detección de Huecos en el Vocabulario Esencial

La NGSL está rankeada por frecuencia. Si el curso enseña la palabra #2500 pero no la #200, hay un hueco grave. Esto complementa el análisis de ausencia actual (que usa EVP) con una perspectiva de **utilidad práctica**.

### 3. Benchmark Objetivo e Independiente

El EVP es de Cambridge. La NGSL es un proyecto académico independiente basado en un corpus diferente. Tener **dos fuentes que validen** la selección de vocabulario es más robusto que depender de una sola.

### 4. Listas Suplementarias para Cursos Especializados

La NGSL viene con listas complementarias:

| Lista | Palabras | Cobertura adicional | Uso |
|-------|----------|-------------------|-----|
| **NAWL** (New Academic Word List) | ~960 | +4% en textos académicos | Cursos de inglés académico |
| **TSL** (TOEIC Service List) | ~1,200 | Hasta 99% en TOEIC | Preparación de exámenes |
| **BSL** (Business Service List) | ~1,700 | ~97% en contextos de negocio | Business English |

<!-- Sugerencia: Si Learney tiene o planea cursos especializados (Business English, Academic English, TOEIC prep), estas listas suplementarias ya están listas para usar. No se solapan con la NGSL (0% de overlap), así que se suman directamente. -->

---

## Datos de la NGSL que Respaldan el Diseño

### Distribución por Nivel CEFR

De las 2,809 palabras NGSL, el **97.2%** tiene mapeo al EVP:

| Nivel CEFR | Palabras en NGSL | Porcentaje |
|------------|-----------------|------------|
| A1-A2 (Basic User) | ~1,010 | 36% |
| B1-B2 (Independent User) | ~1,461 | 52.2% |
| C1-C2 (Proficient User) | ~252 | 9% |
| Sin mapeo EVP | ~86 | 2.8% |

### Cobertura por Bandas de Frecuencia

| Banda | Palabras | Cobertura acumulada aproximada |
|-------|----------|-------------------------------|
| Top 100 | 100 | ~50% del texto general |
| Top 500 | 500 | ~75% |
| Top 1000 | 1,000 | ~84% |
| Top 2000 | 2,000 | ~90% |
| Top 2809 (completa) | 2,809 | ~92% |

<!-- Cuidado aquí: Estos porcentajes de cobertura son promedios sobre textos generales. En textos especializados (medicina, derecho, tecnología), la cobertura será menor. La NGSL está diseñada para inglés general. -->

---

## Diseño Funcional Propuesto

### Nombre: `NGSLCoverageAnalyzer`

### Datos de Entrada

- **CourseSentences**: Lemas presentes por nivel (reutiliza `getLemmasByLevel()`)
- **NGSL Word List**: Archivo `ngsl.json` o `ngsl.csv` (nuevo recurso a agregar)
- **Configuración**: Targets de cobertura por nivel

### Configuración Propuesta

```yaml
# Agregado a config.yaml -> targets
ngslCoverage:
  # Porcentaje mínimo de palabras NGSL que el curso debe cubrir acumulativamente
  # para cada nivel CEFR
  cumulativeTargets:
    A1:
      min: 15    # Al menos 15% de NGSL cubierto al completar A1
      optimal: 20
    A2:
      min: 35
      optimal: 45
    B1:
      min: 55
      optimal: 65
    B2:
      min: 75
      optimal: 85

  # Análisis de bandas: qué porcentaje de cada banda NGSL se cubre
  bandTargets:
    # Las primeras 500 palabras NGSL son las más críticas
    top500:
      byEndOfA1: 40   # Al menos 40% de las top 500 al terminar A1
      byEndOfA2: 70
      byEndOfB1: 90
      byEndOfB2: 95
    # Las siguientes 500
    top1000:
      byEndOfA2: 40
      byEndOfB1: 60
      byEndOfB2: 80

  # Palabras NGSL que se consideran "críticas" si están ausentes
  # (las top N más frecuentes)
  criticalThreshold: 500
```

### Lógica Funcional

#### Paso 1: Cargar la NGSL

```
Cargar ngsl.json con 2,809 lemas rankeados
Cada entrada: { rank, lemma, pos, frequency }
```

#### Paso 2: Calcular Cobertura Acumulativa

```
Para cada nivel (A1 -> B2):
    lemas_curso_acumulados += lemas presentes en este nivel
    lemas_ngsl_cubiertos = intersección(lemas_curso_acumulados, ngsl)
    cobertura = |lemas_ngsl_cubiertos| / |ngsl| * 100

    Ejemplo:
        A1 enseña 200 palabras NGSL de 2809 -> cobertura = 7.1%
        A1+A2 enseñan 600 palabras NGSL     -> cobertura = 21.4%
        A1+A2+B1 enseñan 1200              -> cobertura = 42.7%
        A1+A2+B1+B2 enseñan 2000           -> cobertura = 71.2%
```

<!-- Cuidado aquí: La cobertura es ACUMULATIVA. Esto significa que lo que se enseña en A1 "cuenta" para todos los niveles posteriores. Esto refleja la realidad pedagógica: el estudiante no olvida lo que aprendió antes. -->

#### Paso 3: Análisis por Bandas de Frecuencia NGSL

```
Para cada banda (top500, top1000, top2000, full):
    Para cada nivel:
        palabras_banda = filtrar NGSL donde rank <= banda
        cubiertas = intersección(lemas_curso_acumulados, palabras_banda)
        cobertura_banda = |cubiertas| / |palabras_banda| * 100
```

Esto responde: "¿Cuántas de las 500 palabras más esenciales del inglés cubre este curso al final de A2?"

#### Paso 4: Detección de Huecos Críticos

```
huecos_criticos = []
Para cada palabra en NGSL donde rank <= criticalThreshold:
    si palabra NO está en lemas_curso (ningún nivel):
        huecos_criticos.append(palabra)

Ordenar huecos por rank (las más frecuentes primero)
```

Estas son palabras que TODO estudiante necesita y que el curso no enseña.

#### Paso 5: Scoring

```
score = promedio ponderado de:
    - cobertura_acumulativa_vs_target por nivel (peso: 0.5)
    - cobertura_top500 (peso: 0.3)
    - ausencia_de_huecos_criticos (peso: 0.2)
```

### Resultado Propuesto (NGSLCoverageResult)

```json
{
  "cumulativeCoverage": {
    "A1": {
      "ngslWordsIntroduced": 200,
      "cumulativeTotal": 200,
      "coveragePercentage": 7.1,
      "target": 15,
      "status": "DEFICIENT"
    },
    "A2": {
      "ngslWordsIntroduced": 400,
      "cumulativeTotal": 600,
      "coveragePercentage": 21.4,
      "target": 35,
      "status": "DEFICIENT"
    },
    "B1": {
      "ngslWordsIntroduced": 600,
      "cumulativeTotal": 1200,
      "coveragePercentage": 42.7,
      "target": 55,
      "status": "ADEQUATE"
    },
    "B2": {
      "ngslWordsIntroduced": 800,
      "cumulativeTotal": 2000,
      "coveragePercentage": 71.2,
      "target": 75,
      "status": "ADEQUATE"
    }
  },
  "bandCoverage": {
    "top500": {
      "A1": 40.0,
      "A2": 72.0,
      "B1": 91.0,
      "B2": 96.0
    },
    "top1000": {
      "A1": 22.0,
      "A2": 48.0,
      "B1": 68.0,
      "B2": 82.0
    }
  },
  "criticalGaps": [
    {"rank": 45, "lemma": "between", "pos": "ADP"},
    {"rank": 78, "lemma": "become", "pos": "VERB"},
    {"rank": 102, "lemma": "point", "pos": "NOUN"}
  ],
  "estimatedRealWorldComprehension": {
    "A1": "~50% de textos generales",
    "A2": "~65% de textos generales",
    "B1": "~78% de textos generales",
    "B2": "~88% de textos generales"
  },
  "overallScore": 0.68
}
```

<!-- Sugerencia: El campo "estimatedRealWorldComprehension" es una estimación basada en la investigación de la NGSL. No es un cálculo exacto, pero es una proyección comunicable. Se calcula como: base (50% por top 100 conocidas) + cobertura incremental según bandas NGSL. -->

---

## Integración con el Sistema Actual

### Opción A: Como VocabularyAnalyzer (Recomendada)

```java
public class NGSLCoverageAnalyzer implements VocabularyAnalyzer<NGSLCoverageResult> {

    private final NGSLWordList ngslWordList;
    private final NGSLCoverageConfig config;

    @Override
    public NGSLCoverageResult analyze(CourseSentences courseSentences,
                                       CourseStats.Builder courseStatsBuilder) {
        // Reutiliza getLemmasByLevel() de CourseSentences
        // Calcula cobertura acumulativa
        // Detecta huecos
        // Genera scores
    }

    @Override
    public void addToResult(FrameworkVocabularyAnalysisResult.Builder builder,
                            NGSLCoverageResult result) {
        builder.ngslCoverageResult(result);
    }
}
```

**Ventaja:** Se integra naturalmente al framework existente. Se agrega a la lista de analyzers del orquestador y listo.

### Cambios Necesarios

1. **Nuevo recurso:** `src/main/resources/vocabulary/ngsl.json` (o CSV)
2. **Nuevo analyzer:** `NGSLCoverageAnalyzer.java`
3. **Nuevo result:** `NGSLCoverageResult.java`
4. **Ampliar config.yaml:** Sección `ngslCoverage`
5. **Ampliar FrameworkVocabularyAnalysisResult:** Agregar campo `ngslCoverageResult`
6. **Registrar en orquestador:** Agregar a la lista de analyzers

### Datos que Reutiliza (Ya Existentes)

| Dato | Fuente | Ya existe |
|------|--------|-----------|
| Lemas por nivel | `CourseSentences.getLemmasByLevel()` | Sí |
| ContentWordFilter | Shared | Sí |
| CEFRLevel | Model | Sí |
| CourseStats.Builder | Framework | Sí |

### Datos Nuevos Necesarios

| Dato | Fuente | Formato |
|------|--------|---------|
| Lista NGSL 1.2 | [newgeneralservicelist.com](https://www.newgeneralservicelist.com) | CSV/JSON, 2,809 entradas, open source, gratis |
| Listas suplementarias (opcional) | Mismo sitio | NAWL (~960), TSL (~1,200), BSL (~1,700) |

---

## Comparación con Analyzers Existentes

| Aspecto | CocaBuckets | LemmaAbsence | **NGSLCoverage** |
|---------|-------------|--------------|------------------|
| **Pregunta que responde** | ¿Las palabras son de la frecuencia correcta? | ¿Faltan palabras del EVP? | ¿Cuánto del inglés real cubre el curso? |
| **Perspectiva** | Distribución de frecuencia | Completitud curricular | Utilidad práctica |
| **Fuente de referencia** | COCA ranking | EVP (Cambridge) | NGSL (académico independiente) |
| **Granularidad** | Por oracion/topic/level | Por nivel | Acumulativo por nivel |
| **Público del resultado** | Diseñadores de contenido | Diseñadores de contenido | **Comercial + diseñadores** |

<!-- Sugerencia: Fíjate que CocaBuckets y LemmaAbsence son herramientas para diseñadores de contenido. NGSL Coverage podría ser una herramienta tanto para diseñadores como para marketing/ventas del curso. "Nuestro curso cubre el 85% del vocabulario esencial del inglés" es un claim poderoso y respaldado por investigación. -->

---

## Esfuerzo de Implementación

| Tarea | Complejidad | Estimación |
|-------|-------------|------------|
| Descargar y parsear NGSL 1.2 | Baja | Es un CSV de 2,809 filas |
| Mapear NGSL lemma -> LemmaAndPos | Baja | Match por lemma, POS universal |
| Implementar cálculo de cobertura | Baja | Set intersection básica |
| Implementar análisis de bandas | Baja | Filtro por rank + intersection |
| Implementar detección de huecos | Baja | Difference set |
| Integrar al framework | Media | Seguir patrón existente |
| Definir targets de cobertura | Media | Requiere calibración con cursos reales |
| **Total** | **Baja-Media** | — |

<!-- Sugerencia: Este analyzer sería probablemente el MÁS SIMPLE de implementar del sistema. La lógica es intersección de conjuntos. La complejidad real está en calibrar los targets (¿cuánta cobertura es razonable esperar en A1?). -->

---

## Listas Suplementarias: Cuándo Usarlas

### Para Cursos de Inglés General
Solo NGSL (2,809 palabras). No se necesita nada más.

### Para Cursos de Inglés Académico
NGSL + **NAWL** (New Academic Word List, ~960 palabras). Las 960 palabras de la NAWL agregan ~4% de cobertura en textos académicos. Combinadas: ~96% de cobertura académica.

### Para Preparación TOEIC
NGSL + **TSL** (TOEIC Service List, ~1,200 palabras). Combinadas: hasta 99% de cobertura en exámenes TOEIC.

### Para Business English
NGSL + **BSL** (Business Service List, ~1,700 palabras). Combinadas: ~97% de cobertura en contextos de negocio.

**Ninguna lista suplementaria se solapa con la NGSL** (0% de overlap), así que se pueden combinar aditivamente.

---

## Mi Opinión

**Este analyzer debería ser el primero en implementarse en la migración**, por varias razones:

1. **Es el más simple** de todos los analyzers: la lógica core es intersección de conjuntos
2. **Es el más comunicable**: "tu curso cubre el X% del vocabulario esencial" es una métrica que cualquiera entiende
3. **Es complementario, no redundante**: Responde una pregunta que NINGÚN analyzer actual responde
4. **Los datos son gratuitos y open source**: La NGSL 1.2 se descarga gratis de newgeneralservicelist.com
5. **Es el que más ROI da** en términos de esfuerzo vs valor: bajo esfuerzo, alto impacto

**Lo que haría:**

1. Descargarlo hoy: la lista NGSL 1.2 de [newgeneralservicelist.com](https://www.newgeneralservicelist.com/new-general-service-list)
2. Implementar primero solo la cobertura acumulativa (lo más simple y de mayor valor)
3. Después agregar el análisis por bandas y los huecos críticos
4. Calibrar los targets con 2-3 cursos existentes para ver valores realistas
5. Si hay cursos especializados, agregar NAWL/TSL/BSL como módulos opcionales

<!-- Cuidado aquí: Un riesgo es fijar targets demasiado altos. Un curso de A1-B2 no puede cubrir el 100% de la NGSL porque el 9% son palabras C1-C2 que están fuera del scope. Los targets de B2 deberían estar alrededor de 80-85%, no 100%. Calibrar con datos reales antes de poner targets definitivos. -->

---

## Fuentes

- [New General Service List Project (Official Site)](https://www.newgeneralservicelist.com)
- [NGSL 1.2 Word List](https://www.newgeneralservicelist.com/new-general-service-list)
- [NGSL on Wikipedia](https://en.wikipedia.org/wiki/New_General_Service_List)
- [EAP Foundation - NGSL Overview](https://www.eapfoundation.com/vocab/general/ngsl/)
- [Cambridge Blog - Core Vocabulary for EFL](https://www.cambridge.org/elt/blog/2018/05/29/general-service-list/)
- [NGSL Supplementary Lists (NAWL, TSL, BSL)](https://www.newgeneralservicelist.com/modularize-learning)
