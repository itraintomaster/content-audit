# LemmaRecurrence

## Proposito

Evalua la **distribucion espacial de las palabras** a lo largo del curso. La premisa pedagogica es que para que un estudiante retenga vocabulario nuevo, las palabras deben **repetirse a intervalos regulares** (spaced repetition). Si una palabra aparece 10 veces seguidas y luego nunca mas, no es tan efectiva como si aparece espaciada a lo largo del material.

---

## Conceptos Clave

### Posicion Global de Palabras

El sistema asigna una **posicion global** a cada palabra del curso, procesando las oraciones en orden CEFR (A1 -> A2 -> B1 -> B2) y dentro de cada nivel en orden de topics. Esto crea una secuencia lineal de todas las palabras:

```
Posicion:  1    2    3    4    5    ...  500  501  502  ...
Palabra:   the  cat  is   big  the  ...  run  the  cat  ...
```

### Content Words

Solo se analizan **palabras de contenido** (sustantivos, verbos, adjetivos, adverbios). Las palabras funcionales (articulos, preposiciones, conjunciones) se excluyen porque aparecen con tanta frecuencia que su recurrencia no es pedagogicamente relevante.

### Intervalo Medio y Desviacion Estandar

Para cada lema, se calcula:
- **meanInterval**: Promedio de la distancia (en posiciones globales) entre apariciones consecutivas
- **stdDevInterval**: Desviacion estandar de esos intervalos

Un meanInterval bajo = la palabra aparece muy frecuentemente (cada pocas palabras)
Un meanInterval alto = la palabra aparece esporadicamente (cada cientos de palabras)

---

## Datos de Entrada

- **CourseSentences**: Proporciona el `GlobalWordPositionTracker` que registra las posiciones de todas las palabras
- **TargetsConfig.LemmaRecurrence**: Configuracion de umbrales (ver [config-snippet.yaml](./config-snippet.yaml))

---

## Configuracion

| Parametro | Valor | Significado |
|-----------|-------|-------------|
| `top` | 2000 | Solo analizar las top N lemas mas frecuentes del curso |
| `subExposed` | 1000 | Si meanInterval > 1000 posiciones -> sub-expuesto |
| `overExposed` | 50 | Si meanInterval <= 50 posiciones -> sobre-expuesto |

<!-- Cuidado aqui: El nombre "subExposed" en la config se refiere al umbral para considerar un lema como SUB-expuesto (aparece muy poco). Esto es contraintuitivo porque un valor ALTO de subExposed significa que se tolera mayor distancia entre apariciones. -->

---

## Logica Funcional Detallada

### 1. Obtencion de Posiciones de Palabras

El `GlobalWordPositionTracker` se construye en `CourseSentences` procesando todas las oraciones en orden CEFR:

```
Para cada nivel (A1 -> B2):
    Para cada oracion del nivel:
        Para cada token procesado por SpaCy:
            1. Registrar posicion global -> info del token
            2. Si es content word:
                Registrar posicion en el mapa lemma -> [posiciones]
            3. Incrementar posicion global
```

### 2. Seleccion de Top Lemas

Se seleccionan las `top` (2000) lemas con mayor numero de apariciones (count). Esto excluye lemas que aparecen muy pocas veces y que no tienen suficientes datos para calcular intervalos significativos.

### 3. Calculo de Intervalos

Para cada lema seleccionado:
1. Se ordenan sus posiciones
2. Se calculan los intervalos entre posiciones consecutivas
3. Se calcula el intervalo medio y la desviacion estandar

```
Ejemplo para "cat" en posiciones [10, 45, 120, 500]:
    Intervalos: [35, 75, 380]
    meanInterval: (35 + 75 + 380) / 3 = 163.3
    stdDevInterval: sqrt(((35-163.3)^2 + (75-163.3)^2 + (380-163.3)^2) / 3) = 154.7
```

<!-- Sugerencia: Una stdDevInterval alta respecto al meanInterval indica distribucion irregular. Un lema puede tener un buen meanInterval pero si la desviacion es alta, las repeticiones estan concentradas en una parte del curso y ausentes en otra. Esto no se captura en el scoring actual. -->

### 4. Clasificacion de Exposicion

| Estado | Condicion | Significado |
|--------|-----------|-------------|
| `normal` | 50 < meanInterval <= 1000 | Recurrencia adecuada |
| `sub-exposed` | meanInterval > 1000 | La palabra aparece muy esporadicamente |
| `over-exposed` | meanInterval <= 50 | La palabra aparece con demasiada frecuencia |

### 5. Scoring

```
overallScore = normalCount / totalCount
```

Donde:
- `normalCount` = cantidad de lemas con estado "normal"
- `totalCount` = total de lemas analizados (top 2000)

El score se redondea a 2 decimales.

<!-- Cuidado aqui: El scoring es binario a nivel de lema: un lema "normal" aporta 1 al conteo, un lema "sub-exposed" o "over-exposed" aporta 0. No hay penalizacion gradual. Un lema con meanInterval=51 (justo normal) aporta lo mismo que uno con meanInterval=200 (perfecto). -->

---

## Resultado (IndividualLemmaRecurrenceResult)

```json
{
  "lemmaStats": [
    {
      "lemma": "cat",
      "pos": "NOUN",
      "count": 15,
      "meanInterval": 234.5,
      "stdDevInterval": 89.2,
      "exposureStatus": "normal",
      "occurrencePositions": [10, 45, 120, 500, ...]
    },
    {
      "lemma": "ameliorate",
      "pos": "VERB",
      "count": 2,
      "meanInterval": 3500.0,
      "stdDevInterval": 0.0,
      "exposureStatus": "sub-exposed",
      "occurrencePositions": [1200, 4700]
    }
  ],
  "exposureSummary": {
    "normal": 1500,
    "sub-exposed": 350,
    "over-exposed": 150
  },
  "overallScore": 0.75
}
```

---

## Integracion con CourseStats

Este analyzer **no alimenta** estadisticas a nivel de oracion individual. Solo produce un resultado global que se agrega al `courseStatsBuilder.individualLemmaRecurrenceResult(result)`.

El `recurrenceScore` global se extrae directamente del `overallScore` del resultado.

---

## Archivos Relevantes para Migracion

| Archivo | Descripcion |
|---------|-------------|
| config.yaml -> lemmaRecurrence | Umbrales de exposicion |
| GlobalWordPositionTracker | Estructura critica que trackea posiciones de palabras |
| ContentWordFilter | Define que es una "content word" |

---

## Mi Opinion

**Este analyzer implementa un concepto pedagogico importante (spaced repetition) pero con una implementacion simplificada.**

**Fortalezas:**
- El concepto de medir intervalos entre apariciones es pedagogicamente valido
- El GlobalWordPositionTracker es una abstraccion util que podria reutilizarse
- El resultado incluye las posiciones exactas de cada lema, util para debugging

**Puntos de atencion para la migracion:**

1. **Scoring binario**: El mayor problema es que el scoring es todo-o-nada. Un lema con meanInterval=51 (apenas normal) puntua igual que uno con meanInterval=200 (excelente). En la migracion, considerar un scoring gradual.

2. **Desviacion estandar no usada**: Se calcula `stdDevInterval` pero NO se usa para el scoring ni para la clasificacion. Es solo informativo. Sin embargo, es un dato valioso: un lema con buen promedio pero alta desviacion tiene repeticiones irregulares.

<!-- Sugerencia: Para la migracion, considerar usar stdDevInterval como factor adicional de scoring. Un coeficiente de variacion (stdDev/mean) > 1 indica distribucion muy irregular. -->

3. **Top 2000 arbitrario**: El limite de 2000 lemas a analizar es configurable pero arbitrario. Para cursos pequenos, podria ser excesivo; para cursos grandes, podria ser insuficiente.

4. **Dependencia del orden de procesamiento**: La posicion global depende del orden en que se procesan los niveles y topics. Si el orden cambia, los intervalos cambian. Asegurarse de que el orden sea determinista.

5. **Lemas con pocas apariciones**: Los lemas con menos de 2 apariciones se excluyen (no se pueden calcular intervalos). Pero un lema que aparece solo 1 vez es por definicion "sub-exposed" y no se reporta.

6. **No hay analisis por nivel**: A diferencia de otros analyzers, este analiza el curso COMPLETO de forma global, no por nivel CEFR. Esto hace que sea imposible saber si la recurrencia es buena dentro de A1 pero mala en B2.
