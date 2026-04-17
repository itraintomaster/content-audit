---
feature:
  id: FEAT-LREC
  code: F-LREC
  name: Analisis de Recurrencia de Lemas por Repeticion Espaciada
  priority: high
---

# Analisis de Recurrencia de Lemas por Repeticion Espaciada

Evaluar la **distribucion espacial de los lemas** a lo largo del curso para determinar si las palabras de contenido se repiten a intervalos regulares, aplicando el principio pedagogico de repeticion espaciada (spaced repetition). Un lema que aparece concentrado en una seccion del curso y luego desaparece es menos efectivo para la retencion que uno que se distribuye uniformemente. El analizador produce un resultado global a nivel de curso (no por oracion ni por nivel), con estadisticas individuales por lema y una puntuacion general que refleja la proporcion de lemas con recurrencia adecuada.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es pedagogicamente adecuado. Una dimension importante de la calidad del contenido es la **recurrencia del vocabulario**: para que un estudiante retenga vocabulario nuevo, las palabras deben repetirse a intervalos regulares a lo largo del material del curso (spaced repetition). Si una palabra aparece 10 veces seguidas en una leccion y luego nunca mas, no es tan efectiva para el aprendizaje como si aparece espaciada a lo largo de multiples lecciones.

Este analizador mide la distancia media entre apariciones consecutivas de cada lema en el curso y clasifica cada lema segun si su recurrencia es adecuada, insuficiente o excesiva. A diferencia de otros analizadores del sistema (como sentence-length o COCA distribution) que operan sobre cada oracion o token individual, este analizador opera a nivel **global del curso** y produce un unico resultado agregado.

### Posicion global de palabras

El concepto central es la **posicion global**: cada palabra del curso recibe un numero de posicion secuencial que refleja su orden de aparicion en el material. Las oraciones se procesan en orden CEFR (A1, A2, B1, B2) y dentro de cada nivel en orden de topics. Esto crea una secuencia lineal de todas las palabras:

```
Posicion:  1    2    3    4    5    ...  500  501  502  ...
Palabra:   the  cat  is   big  the  ...  run  the  cat  ...
```

Esta secuencia lineal permite calcular la distancia entre apariciones consecutivas de una misma palabra, independientemente de en que nivel, topic o knowledge aparezca.

### Palabras de contenido

Solo se analizan **palabras de contenido** (sustantivos, verbos, adjetivos, adverbios). Las palabras funcionales (articulos, preposiciones, conjunciones, pronombres) se excluyen porque aparecen con tanta frecuencia que su recurrencia no es pedagogicamente relevante. No tiene sentido medir si "the" o "is" se repiten a intervalos adecuados, ya que por naturaleza aparecen en casi todas las oraciones.

La clasificacion de palabras de contenido se basa en las etiquetas POS (part-of-speech) de los tokens enriquecidos producidos por el procesamiento linguistico (FEAT-NLP):

| Etiqueta POS | Tipo | Es palabra de contenido? |
|-------------|------|--------------------------|
| NOUN | Sustantivo | SI |
| VERB | Verbo | SI |
| ADJ | Adjetivo | SI |
| ADV | Adverbio | SI |
| Todas las demas | Funcionales/otras | NO |

### Intervalo medio y desviacion estandar

Para cada lema analizado, se calculan dos metricas de distribucion:

- **Intervalo medio (meanInterval)**: Promedio de la distancia (en posiciones globales) entre apariciones consecutivas del lema. Un intervalo medio bajo significa que la palabra aparece muy frecuentemente (cada pocas palabras). Un intervalo medio alto significa que la palabra aparece esporadicamente (cada cientos o miles de palabras).

- **Desviacion estandar del intervalo (stdDevInterval)**: Mide la regularidad de la distribucion. Un lema puede tener un buen intervalo medio pero si la desviacion estandar es alta, las repeticiones estan concentradas en una parte del curso y ausentes en otra. Por ejemplo, un lema con meanInterval=200 y stdDevInterval=180 tiene repeticiones muy irregulares, mientras que uno con meanInterval=200 y stdDevInterval=30 tiene repeticiones bien espaciadas.

### Clasificacion de exposicion

Cada lema se clasifica en uno de tres estados de exposicion segun su intervalo medio:

| Estado | Condicion | Significado pedagogico |
|--------|-----------|----------------------|
| normal | overExposed < meanInterval <= subExposed | La palabra se repite a intervalos adecuados para la retencion |
| sub-exposed | meanInterval > subExposed | La palabra aparece tan esporadicamente que el estudiante probablemente la olvide entre apariciones |
| over-exposed | meanInterval <= overExposed | La palabra aparece tan frecuentemente que satura al estudiante sin beneficio adicional |

Con los umbrales actuales: `normal` cuando 50 < meanInterval <= 1000, `sub-exposed` cuando meanInterval > 1000, `over-exposed` cuando meanInterval <= 50.

### Relacion con otros analizadores

Este analizador se distingue de otros analizadores del sistema en varios aspectos:

1. **Nivel de operacion**: mientras que sentence-length opera por oracion y COCA distribution opera por token y acumula por nivel, este analizador opera a nivel global del curso. No produce puntuaciones por oracion, knowledge, topic ni nivel individual.

2. **Fuente de datos**: utiliza las posiciones globales de los tokens enriquecidos del curso, procesados en orden CEFR. Requiere que los tokens tengan lema y etiqueta POS (provenientes de FEAT-NLP).

3. **Resultado**: produce un unico resultado global que se agrega al informe de auditoria del curso, no participa en la agregacion jerarquica de la plataforma.

### Volumenes esperados

El curso actual contiene aproximadamente 608 knowledges con ~11.500 quizzes. Considerando un promedio de ~10 tokens por oracion, el curso tiene ~115.000 posiciones globales. Con el filtro de palabras de contenido, se analizan aproximadamente la mitad (~57.500 posiciones). De estos, se seleccionan las 2000 lemas mas frecuentes para el analisis de recurrencia.

### Referencia: implementacion original

Esta funcionalidad se basa en el analisis documentado en `analysis/04-lemma-recurrence`. En la implementacion original, el `GlobalWordPositionTracker` se construia como parte del procesamiento de `CourseSentences`, y el resultado se entregaba directamente al `courseStatsBuilder`. La migracion integra esta logica como un analizador del sistema ContentAudit.

---

## Reglas de Negocio

Las reglas se organizan en cinco grupos segun la fase del analisis:

- **Grupo A - Posicion global y filtrado (R001-R003)**: reglas que describen como se asignan posiciones globales a las palabras y como se filtran las palabras de contenido.
- **Grupo B - Seleccion y calculo de intervalos (R004-R007)**: reglas que describen la seleccion de los lemas a analizar y el calculo de sus metricas de recurrencia.
- **Grupo C - Clasificacion y scoring (R008-R011)**: reglas que describen la clasificacion de exposicion de cada lema y el calculo de la puntuacion general.
- **Grupo D - Resultado y datos de salida (R012-R013)**: reglas que describen la estructura del resultado producido.
- **Grupo E - Configuracion y parametros (R014-R015)**: reglas que describen los parametros configurables del analisis.

---

### Grupo A - Posicion global y filtrado

### Rule[F-LREC-R001] - Asignacion de posicion global a cada palabra
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada palabra (token) del curso recibe una posicion global secuencial que refleja su orden de aparicion en el material completo. Las oraciones se procesan en el siguiente orden determinista:

1. Se recorren los niveles CEFR en orden ascendente: A1, A2, B1, B2
2. Dentro de cada nivel, se recorren los topics en su orden natural dentro del nivel
3. Dentro de cada topic, se recorren los knowledges en su orden natural
4. Dentro de cada knowledge, se recorren los quizzes en su orden natural
5. Dentro de cada quiz, se recorren los tokens en orden de aparicion en la oracion

La posicion global se incrementa en 1 por cada token procesado, comenzando desde la posicion 1. Todos los tokens participan en el conteo de posicion global, no solo las palabras de contenido. Esto asegura que las distancias reflejen la separacion real entre apariciones de un lema en el texto.

**Error**: "Error al construir las posiciones globales: el curso no contiene oraciones procesables"

### Rule[F-LREC-R002] - Orden de procesamiento determinista
**Severity**: critical | **Validation**: AUTO_VALIDATED

El orden en que se procesan las oraciones debe ser determinista: dado el mismo curso con el mismo contenido, la secuencia de posiciones globales debe ser siempre identica. Esto es fundamental porque los intervalos calculados dependen directamente de las posiciones, y un cambio en el orden de procesamiento alteraria todos los intervalos y por lo tanto el resultado del analisis.

El orden determinista se garantiza procesando los niveles en el orden CEFR fijo (A1, A2, B1, B2) y los topics, knowledges y quizzes dentro de cada nivel en su orden natural de definicion en el curso.

**Error**: N/A (esta regla describe un requisito de consistencia)

### Rule[F-LREC-R003] - Filtrado de palabras de contenido
**Severity**: critical | **Validation**: AUTO_VALIDATED

Solo las **palabras de contenido** se incluyen en el analisis de recurrencia. Una palabra de contenido es un token cuya etiqueta POS (part-of-speech) corresponde a una de las siguientes categorias gramaticales: sustantivo (NOUN), verbo (VERB), adjetivo (ADJ) o adverbio (ADV).

Todas las demas categorias (determinantes, preposiciones, pronombres, conjunciones, puntuacion, simbolos, etc.) se excluyen del analisis de recurrencia. Estos tokens SI participan en el conteo de posicion global (R001), pero NO se registran en el mapa de posiciones por lema.

El filtrado se aplica usando la etiqueta POS del token enriquecido proveniente del procesamiento linguistico (FEAT-NLP). Los tokens sin etiqueta POS o con etiqueta desconocida se excluyen del analisis.

**Error**: N/A (esta regla describe un criterio de filtrado)

---

### Grupo B - Seleccion y calculo de intervalos

### Rule[F-LREC-R004] - Registro de posiciones por lema
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada token que pasa el filtro de palabras de contenido (R003), se registra su posicion global en un mapa que asocia cada lema con la lista de posiciones donde aparece. La clave del mapa es la **forma lematizada** del token (el lema), no la forma flexionada que aparece en el texto.

Por ejemplo, las formas "running", "ran" y "runs" se lematizan a "run" y sus posiciones se registran bajo el lema "run". Esto asegura que el analisis de recurrencia mida la repeticion del concepto lexical, no de una forma gramatical especifica.

Las posiciones se mantienen ordenadas de menor a mayor para cada lema.

**Error**: N/A (esta regla describe la construccion de una estructura de datos)

### Rule[F-LREC-R005] - Seleccion de los lemas mas frecuentes (top N)
**Severity**: critical | **Validation**: AUTO_VALIDATED

De todos los lemas registrados (R004), se seleccionan los **top N lemas** con mayor numero de apariciones (count) para el analisis de recurrencia. El valor N es configurable (actualmente 2000).

Los lemas se ordenan por cantidad de apariciones de mayor a menor, y se toman los primeros N. En caso de empate en el conteo de apariciones, el orden entre lemas con el mismo conteo es indistinto.

Los lemas no seleccionados (los menos frecuentes) se excluyen del analisis. Esta exclusion es practica: lemas con muy pocas apariciones no tienen suficientes datos para calcular intervalos estadisticamente significativos. Sin embargo, esta exclusion implica que lemas con una unica aparicion (que por definicion son sub-expuestos) no se reportan.

**Error**: "No se encontraron lemas de contenido en el curso. El analisis de recurrencia no puede ejecutarse."

### Rule[F-LREC-R006] - Exclusion de lemas con menos de 2 apariciones
**Severity**: major | **Validation**: AUTO_VALIDATED

Los lemas con menos de 2 apariciones se excluyen del analisis porque no es posible calcular un intervalo entre apariciones consecutivas sin al menos 2 posiciones. Esta exclusion opera de facto a traves de la seleccion de top N (R005): los lemas con una unica aparicion naturalmente quedan fuera del top ya que tienen el conteo mas bajo posible.

Si despues de la seleccion del top N algun lema tuviera exactamente 1 aparicion (posible en cursos muy pequenos con vocabulario limitado), se excluye del calculo de intervalos.

**Error**: N/A (esta regla describe un criterio de exclusion)

### Rule[F-LREC-R007] - Calculo de intervalo medio y desviacion estandar
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada lema seleccionado con al menos 2 apariciones, se calcula:

1. **Intervalos entre apariciones consecutivas**: dada la lista ordenada de posiciones [p1, p2, p3, ..., pn], los intervalos son [p2-p1, p3-p2, ..., pn-pn-1].

2. **Intervalo medio (meanInterval)**: el promedio aritmetico de los intervalos calculados.
   - meanInterval = suma(intervalos) / cantidad(intervalos)

3. **Desviacion estandar del intervalo (stdDevInterval)**: la desviacion estandar poblacional de los intervalos.
   - stdDevInterval = sqrt(suma((intervalo_i - meanInterval)^2) / cantidad(intervalos))

Ejemplo para el lema "cat" con posiciones [10, 45, 120, 500]:
- Intervalos: [35, 75, 380]
- meanInterval: (35 + 75 + 380) / 3 = 163.3
- stdDevInterval: sqrt(((35-163.3)^2 + (75-163.3)^2 + (380-163.3)^2) / 3) = 154.7

El uso de desviacion estandar poblacional (dividiendo por N, no por N-1) es consistente con la implementacion de referencia.

**Error**: "Error al calcular intervalos para el lema '{lema}': la lista de posiciones esta vacia o tiene un solo elemento"

---

### Grupo C - Clasificacion y scoring

### Rule[F-LREC-R008] - Clasificacion de exposicion de cada lema
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema analizado se clasifica en uno de tres estados de exposicion segun su intervalo medio (meanInterval) y los umbrales configurados:

| Estado | Condicion | Umbral actual |
|--------|-----------|---------------|
| over-exposed | meanInterval <= overExposed | meanInterval <= 50 |
| normal | overExposed < meanInterval <= subExposed | 50 < meanInterval <= 1000 |
| sub-exposed | meanInterval > subExposed | meanInterval > 1000 |

Las condiciones se evaluan en el siguiente orden de prioridad:
1. Si meanInterval <= overExposed: el lema esta **sobre-expuesto**
2. Si meanInterval > subExposed: el lema esta **sub-expuesto**
3. En cualquier otro caso: el lema tiene exposicion **normal**

Los umbrales `overExposed` y `subExposed` son configurables y definen el rango de recurrencia considerado adecuado. El rango "normal" es abierto por abajo y cerrado por arriba: no incluye el valor exacto de overExposed, pero si incluye el valor exacto de subExposed.

**Error**: "Configuracion de umbrales invalida: overExposed ({overExposed}) debe ser menor que subExposed ({subExposed})"

### Rule[F-LREC-R009] - Resumen de exposicion
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado incluye un resumen de exposicion que contabiliza cuantos lemas caen en cada estado:

- **normalCount**: cantidad de lemas con estado "normal"
- **subExposedCount**: cantidad de lemas con estado "sub-exposed"
- **overExposedCount**: cantidad de lemas con estado "over-exposed"

La suma de los tres contadores debe ser igual al total de lemas analizados.

**Error**: "Inconsistencia en el resumen de exposicion: normalCount ({normal}) + subExposedCount ({sub}) + overExposedCount ({over}) != totalCount ({total})"

### Rule[F-LREC-R010] - Puntuacion general del analisis (overall score)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion general del analisis de recurrencia se calcula como la proporcion de lemas con exposicion normal respecto al total de lemas analizados:

overallScore = normalCount / totalCount

Donde:
- normalCount = cantidad de lemas con estado "normal" (R008)
- totalCount = total de lemas analizados (los seleccionados en R005 que tienen al menos 2 apariciones)

El resultado se redondea a 2 decimales. Si no hay lemas analizados (totalCount = 0), la puntuacion es 0.0.

Ejemplo: si de 2000 lemas analizados, 1500 tienen exposicion normal, 350 son sub-expuestos y 150 son sobre-expuestos:
- overallScore = 1500 / 2000 = 0.75

El scoring es **binario a nivel de lema**: un lema con estado "normal" aporta 1 al conteo de normales, mientras que un lema "sub-exposed" o "over-exposed" aporta 0. No hay penalizacion gradual: un lema con meanInterval=51 (apenas normal) contribuye lo mismo que uno con meanInterval=200 (excelente). Ver Doubt[DOUBT-GRADUAL-SCORING] para una discusion sobre scoring gradual.

**Error**: "Error al calcular la puntuacion general: totalCount es cero, no hay lemas analizados"

### Rule[F-LREC-R011] - La desviacion estandar es informativa, no participa en el scoring
**Severity**: major | **Validation**: AUTO_VALIDATED

La desviacion estandar del intervalo (stdDevInterval) se calcula y se incluye en el resultado para cada lema (R007), pero **no participa** en la clasificacion de exposicion (R008) ni en el calculo de la puntuacion general (R010). Su funcion es exclusivamente informativa: permite al usuario identificar lemas con distribucion irregular.

Un lema con buen intervalo medio pero alta desviacion estandar tiene repeticiones concentradas en una parte del curso y ausentes en otra. Esta informacion es valiosa para el creador de contenido, aunque el scoring actual no la penaliza.

Ver Doubt[DOUBT-STDDEV-SCORING] para una discusion sobre incorporar la desviacion estandar al scoring.

**Error**: N/A (esta regla describe una decision de diseno)

---

### Grupo D - Resultado y datos de salida

### Rule[F-LREC-R012] - Estructura del resultado por lema
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado del analisis incluye, para cada lema analizado, la siguiente informacion:

| Campo | Descripcion |
|-------|-------------|
| lemma | La forma lematizada de la palabra (ej: "run", "cat") |
| pos | La etiqueta POS predominante del lema (NOUN, VERB, ADJ, ADV) |
| count | Cantidad total de apariciones del lema en el curso |
| meanInterval | Intervalo medio entre apariciones consecutivas (R007) |
| stdDevInterval | Desviacion estandar del intervalo (R007) |
| exposureStatus | Estado de exposicion: "normal", "sub-exposed" o "over-exposed" (R008) |
| occurrencePositions | Lista de todas las posiciones globales donde aparece el lema |

La inclusion de las posiciones exactas (occurrencePositions) permite al creador de contenido localizar donde aparece cada lema en el curso y evaluar visualmente su distribucion.

**Error**: N/A (esta regla describe la estructura de datos del resultado)

### Rule[F-LREC-R013] - Estructura del resultado global
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado global del analisis de recurrencia contiene:

1. **lemmaStats**: lista de estadisticas individuales por lema (R012), ordenada por cantidad de apariciones de mayor a menor.

2. **exposureSummary**: resumen de conteos por estado de exposicion (R009):
   - normal: cantidad de lemas con exposicion normal
   - sub-exposed: cantidad de lemas sub-expuestos
   - over-exposed: cantidad de lemas sobre-expuestos

3. **overallScore**: puntuacion general del analisis (R010), valor entre 0.0 y 1.0.

Este resultado se agrega al informe de auditoria del curso como un resultado global. No se integra en la jerarquia de agregacion por nodo (knowledge -> topic -> nivel) de la plataforma, ya que no produce puntuaciones individuales por oracion o knowledge.

**Error**: N/A (esta regla describe la estructura de datos del resultado)

---

### Grupo E - Configuracion y parametros

### Rule[F-LREC-R014] - Parametros configurables del analisis
**Severity**: major | **Validation**: AUTO_VALIDATED

El analisis de recurrencia de lemas utiliza tres parametros configurables:

| Parametro | Valor actual | Descripcion |
|-----------|-------------|-------------|
| top | 2000 | Cantidad de lemas mas frecuentes a analizar |
| subExposed | 1000 | Umbral de sub-exposicion: si meanInterval > subExposed, el lema esta sub-expuesto |
| overExposed | 50 | Umbral de sobre-exposicion: si meanInterval <= overExposed, el lema esta sobre-expuesto |

**Restricciones de validez:**
- `top` debe ser un entero positivo mayor a 0
- `overExposed` debe ser un numero positivo mayor a 0
- `subExposed` debe ser un numero positivo mayor que `overExposed`
- La relacion debe cumplirse: 0 < overExposed < subExposed

**Error**: "Configuracion de recurrencia de lemas invalida: {detalle de la violacion}"

### Rule[F-LREC-R015] - Nombre del analizador en el informe
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador de recurrencia de lemas se identifica con el nombre **"lemma-recurrence"** en el informe de auditoria. Este nombre aparece en el resultado global del curso y permite al usuario identificar la puntuacion correspondiente a este analisis dentro del informe general.

**Error**: N/A (esta regla define un nombre de identificacion)

---

## User Journeys

### Journey[F-LREC-J001] - Auditar la recurrencia de vocabulario de un curso completo
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LREC-J001
    name: Auditar la recurrencia de vocabulario de un curso completo
    flow:
      - id: iniciar_auditoria
        action: "El usuario inicia una auditoria de recurrencia de un curso previamente cargado en ContentAudit"
        then: asignar_posiciones

      - id: asignar_posiciones
        action: "El sistema recorre la jerarquia del curso en orden CEFR (A1, A2, B1, B2) y asigna una posicion global secuencial a cada token"
        gate: [F-LREC-R001, F-LREC-R002]
        outcomes:
          - when: "El curso contiene oraciones procesables"
            then: filtrar_contenido
          - when: "El curso no contiene oraciones procesables"
            then: error_sin_oraciones

      - id: filtrar_contenido
        action: "El sistema filtra las palabras de contenido (sustantivos, verbos, adjetivos, adverbios) y registra sus posiciones globales bajo su forma lematizada"
        gate: [F-LREC-R003, F-LREC-R004]
        then: seleccionar_top_lemas

      - id: seleccionar_top_lemas
        action: "El sistema selecciona los 2000 lemas de contenido mas frecuentes del curso"
        gate: [F-LREC-R005, F-LREC-R006]
        outcomes:
          - when: "Se encontraron lemas de contenido suficientes para el analisis"
            then: calcular_intervalos
          - when: "No se encontraron lemas de contenido en el curso"
            then: error_sin_lemas

      - id: calcular_intervalos
        action: "El sistema calcula el intervalo medio y la desviacion estandar entre apariciones consecutivas para cada lema seleccionado"
        gate: [F-LREC-R007]
        then: clasificar_exposicion

      - id: clasificar_exposicion
        action: "El sistema clasifica cada lema como normal, sub-exposed u over-exposed segun su intervalo medio y los umbrales configurados"
        gate: [F-LREC-R008, F-LREC-R014]
        then: calcular_puntuacion

      - id: calcular_puntuacion
        action: "El sistema calcula la puntuacion general como la proporcion de lemas con exposicion normal sobre el total analizado"
        gate: [F-LREC-R009, F-LREC-R010]
        then: entregar_resultado

      - id: entregar_resultado
        action: "El usuario recibe el resultado global con la puntuacion, el resumen de exposicion (normal, sub-exposed, over-exposed) y las estadisticas detalladas por lema"
        gate: [F-LREC-R012, F-LREC-R013, F-LREC-R015]
        result: success

      - id: error_sin_oraciones
        action: "El sistema informa al usuario que el curso no contiene oraciones procesables y el analisis no puede ejecutarse"
        result: failure

      - id: error_sin_lemas
        action: "El sistema informa al usuario que no se encontraron lemas de contenido y el analisis de recurrencia no puede ejecutarse"
        result: failure
```

### Journey[F-LREC-J004] - Detectar lemas con distribucion irregular usando la desviacion estandar
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LREC-J004
    name: Detectar lemas con distribucion irregular usando la desviacion estandar
    flow:
      - id: revisar_estadisticas
        action: "El usuario consulta las estadisticas por lema del resultado de la auditoria de recurrencia"
        then: identificar_irregulares

      - id: identificar_irregulares
        action: "El usuario observa un lema clasificado como normal (por ejemplo, intervalo medio de 300) pero con una desviacion estandar muy alta (por ejemplo, 280)"
        gate: [F-LREC-R011]
        outcomes:
          - when: "Se encuentran lemas normales con desviacion estandar alta"
            then: consultar_posiciones
          - when: "Todos los lemas normales tienen desviacion estandar razonable"
            then: distribucion_aceptable

      - id: consultar_posiciones
        action: "El usuario consulta las posiciones de aparicion del lema y observa que las apariciones estan concentradas en clusters separados por grandes brechas (por ejemplo, posiciones [50, 55, 60, 70, 75, 1800, 1850])"
        then: evaluar_irregularidad

      - id: evaluar_irregularidad
        action: "El usuario comprende que aunque el intervalo medio es adecuado, la distribucion real es muy irregular y perjudica la retencion del estudiante"
        outcomes:
          - when: "La irregularidad es significativa y el usuario decide corregirla"
            then: redistribuir_lema
          - when: "La irregularidad es menor o no justifica cambios en el contenido"
            then: aceptar_distribucion

      - id: redistribuir_lema
        action: "El usuario redistribuye las apariciones del lema de manera mas uniforme a lo largo del curso, dispersando los clusters concentrados"
        result: success

      - id: aceptar_distribucion
        action: "El usuario acepta la distribucion actual del lema y no realiza cambios"
        result: success

      - id: distribucion_aceptable
        action: "El usuario concluye que todos los lemas normales tienen una distribucion razonablemente uniforme"
        result: success
```

---

## Open Questions

### Doubt[DOUBT-GRADUAL-SCORING] - El scoring deberia ser gradual en lugar de binario?
**Status**: OPEN

El scoring actual es binario a nivel de lema (R010): un lema con estado "normal" aporta 1 al conteo y cualquier otro estado aporta 0. Esto significa que un lema con meanInterval=51 (apenas dentro del rango normal) contribuye lo mismo a la puntuacion que un lema con meanInterval=200 (excelente). Analogamente, un lema con meanInterval=1001 (apenas sub-expuesto) se penaliza igual que uno con meanInterval=5000 (gravemente sub-expuesto).

Un scoring gradual podria reflejar mejor la calidad de la recurrencia. Por ejemplo, un lema con meanInterval=51 podria recibir una puntuacion de 0.6, uno con meanInterval=200 podria recibir 1.0, y uno con meanInterval=1001 podria recibir 0.4 en lugar de 0.0.

**Pregunta**: Se debe implementar un scoring gradual que refleje la calidad de la recurrencia de cada lema, o el scoring binario es suficiente para esta primera version?

- [ ] Opcion A: Mantener el scoring binario (simplicidad, alineado con la implementacion de referencia) -- para la primera version es suficiente y permite validar el concepto
- [ ] Opcion B: Implementar scoring gradual basado en la distancia al centro del rango normal -- mayor precision, mejor discriminacion entre lemas
- [ ] Opcion C: Implementar scoring gradual como mejora futura, documentando la limitacion del scoring binario actual

**Answer**: No entiendo qué son cada cosa.
### Doubt[DOUBT-STDDEV-SCORING] - La desviacion estandar deberia participar en el scoring?
**Status**: OPEN

Actualmente la desviacion estandar se calcula y se reporta pero no participa en la clasificacion ni en el scoring (R011). Sin embargo, es un dato valioso: un lema con buen intervalo medio pero alta desviacion estandar tiene una distribucion irregular que podria perjudicar la retencion.

Un posible enfoque seria usar el **coeficiente de variacion** (stdDevInterval / meanInterval) como factor adicional. Un coeficiente de variacion mayor a 1.0 indica que la desviacion supera al promedio, senalando una distribucion muy irregular.

**Pregunta**: Se debe incorporar la desviacion estandar al scoring, o mantenerla como dato exclusivamente informativo?

- [x] Opcion A: Mantener la desviacion estandar como dato informativo unicamente (simplicidad, alineado con la referencia)
- [ ] Opcion B: Usar el coeficiente de variacion como factor de penalizacion en el scoring -- un lema "normal" con alta variabilidad podria recibir una puntuacion reducida
- [ ] Opcion C: Agregar una segunda clasificacion independiente (distribucion regular / irregular) sin afectar el scoring principal

**Answer**: No sé bien qué representa la desviación estándar y por qué debería afectar al scoring o no. Me explicas?
### Doubt[DOUBT-TOP-N-VALUE] - El valor top=2000 es adecuado para todos los cursos?
**Status**: OPEN

El parametro `top` (actualmente 2000) determina cuantos lemas se analizan. Este valor es configurable pero arbitrario. Para cursos pequenos con vocabulario limitado, podria haber menos de 2000 lemas unicos de contenido, en cuyo caso el parametro es irrelevante. Para cursos muy extensos con vocabulario diverso, 2000 lemas podria no ser suficiente para cubrir todo el vocabulario pedagogicamente relevante.

Ademas, los lemas que quedan fuera del top N pero que aparecen solo 1-2 veces son por definicion sub-expuestos, y esta informacion se pierde al excluirlos.

**Pregunta**: El valor de 2000 es adecuado, o deberia ajustarse o hacerse dinamico?

- [ ] Opcion A: Mantener top=2000 como esta (configurable, adecuado para el curso actual)
- [x] Opcion B: Hacer el valor dinamico: analizar todos los lemas con al menos N apariciones (por ejemplo, al menos 3) independientemente de un tope fijo
- [ ] Opcion C: Mantener top=2000 como default pero reportar la cantidad de lemas excluidos que tienen pocas apariciones, para visibilidad

### Doubt[DOUBT-SINGLE-OCCURRENCE] - Como tratar los lemas con una unica aparicion?
**Status**: OPEN

Los lemas con una sola aparicion se excluyen del analisis porque no se pueden calcular intervalos (R006). Sin embargo, un lema que aparece una unica vez en todo el curso es **por definicion sub-expuesto** desde la perspectiva de repeticion espaciada: el estudiante lo ve una vez y nunca mas.

Excluir estos lemas del conteo podria inflar artificialmente la puntuacion general: si hay 500 lemas de una sola aparicion, no se cuentan como sub-expuestos y el overallScore no refleja este deficit.

**Pregunta**: Los lemas con una unica aparicion deberian incluirse en el conteo como sub-expuestos, o la exclusion actual es adecuada?

- [ ] Opcion A: Excluir del analisis principal como esta actualmente (no se pueden calcular intervalos, mantener simplicidad)
- [ ] Opcion B: Incluir en el conteo como sub-expuestos (el lema se reporta con meanInterval=infinito o con un valor convencional alto)
- [ ] Opcion C: Reportarlos en una seccion separada del resultado sin incluirlos en el scoring principal, para que el usuario tenga visibilidad

**Answer**: Me inclino por incluirlos, pero además está lemma absence, que es otra métrica, y no sé si no se pisaría.
### Doubt[DOUBT-PER-LEVEL-ANALYSIS] - Se deberia agregar analisis de recurrencia por nivel CEFR?
**Status**: OPEN

A diferencia de otros analizadores del sistema, la recurrencia de lemas se analiza **solo a nivel global del curso** (R013). No hay analisis por nivel CEFR individual. Esto significa que es imposible saber si la recurrencia es buena dentro de A1 pero mala en B2, o si un lema se repite bien en los primeros niveles pero desaparece en los avanzados.

Un analisis por nivel proporcionaria informacion mas accionable: el creador de contenido sabria en que nivel intervenir. Sin embargo, aumentaria significativamente la complejidad del analisis y los intervalos dentro de un solo nivel tendrian menos datos (menos posiciones) para ser estadisticamente significativos.

**Pregunta**: Se debe agregar un analisis de recurrencia por nivel CEFR ademas del analisis global?

- [ ] Opcion A: Mantener solo el analisis global (suficiente para esta primera version, menor complejidad)
- [x] Opcion B: Agregar analisis por nivel como funcionalidad adicional -- cada nivel tendria su propio overallScore de recurrencia
- [ ] Opcion C: Agregar analisis por nivel como mejora futura, documentando la limitacion del analisis global actual

### Doubt[DOUBT-POS-AMBIGUITY] - Como manejar lemas que aparecen con diferentes etiquetas POS?
**Status**: OPEN

Un lema puede aparecer con diferentes etiquetas POS en distintos contextos. Por ejemplo, "run" puede ser NOUN ("a morning run") o VERB ("I run every day"). El resultado incluye un campo `pos` por lema (R012), pero si el mismo lema aparece como NOUN en unas oraciones y como VERB en otras, no esta claro que valor asignar.

La implementacion de referencia registra las posiciones por lema sin distinguir por POS: todas las apariciones de "run" (sea NOUN o VERB) se agrupan bajo el mismo lema. Esto es pedagogicamente razonable porque el estudiante reconoce la misma palabra base.

**Pregunta**: Las apariciones de un lema deberian agruparse independientemente de su POS, o deberian tratarse como lemas distintos?

- [x] Opcion A: Agrupar por lema independientemente del POS (como en la referencia) -- pedagogicamente correcto, el estudiante reconoce la misma raiz
- [ ] Opcion B: Tratar cada combinacion lema+POS como una entrada distinta ("run/NOUN" y "run/VERB") -- mas precision linguistica, pero fragmenta los datos y reduce los conteos
- [ ] Opcion C: Agrupar por lema pero reportar el POS predominante (el que aparece con mayor frecuencia)
