---
feature:
  id: FEAT-COCA
  code: F-COCA
  name: Analisis de Distribucion de Vocabulario por Frecuencia COCA
  priority: critical
---

# Analisis de Distribucion de Vocabulario por Frecuencia COCA

Evaluar si la distribucion de vocabulario a lo largo del curso es apropiada para cada nivel de dificultad esperado (CEFR), clasificando los tokens del curso en bandas de frecuencia basadas en el corpus COCA (Corpus of Contemporary American English) y comparando la distribucion real contra objetivos pedagogicos configurables. Las puntuaciones individuales se agregan a traves de la jerarquia del curso (knowledge, topic, nivel, curso) mediante el motor de agregacion generico de la plataforma ContentAudit. Adicionalmente, se evalua la progresion de la distribucion de vocabulario entre niveles y se generan directivas de mejora para el creador de contenido.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es pedagogicamente adecuado. Una de las dimensiones mas criticas es la **distribucion de vocabulario por frecuencia de uso**: en la ensenanza de idiomas, los niveles iniciales (A1) deben utilizar predominantemente palabras de alta frecuencia (las mas comunes del idioma), mientras que los niveles avanzados (B2) deben incorporar progresivamente palabras de menor frecuencia. Este principio esta bien establecido en la linguistica aplicada: los estudiantes principiantes necesitan dominar el vocabulario nuclear del idioma antes de enfrentarse a vocabulario menos frecuente.

El corpus COCA proporciona un ranking de frecuencia para las palabras del ingles. Cada palabra (en su forma lematizada) tiene un `frequencyRank` que indica su posicion en la lista de frecuencia: las palabras con ranking mas bajo son las mas frecuentes. Este ranking es la base del analisis: los tokens del curso se clasifican en bandas de frecuencia ("buckets") segun su ranking, y se evalua si la proporcion de tokens en cada banda es adecuada para el nivel CEFR correspondiente.

Este es el analizador **mas complejo** del sistema ContentAudit. A diferencia del analizador de longitud de oraciones (que opera sobre una metrica simple por oracion), este analizador opera sobre cada token individual del curso, requiere datos de frecuencia externos, soporta dos estrategias de analisis con interpolacion de objetivos, evalua progresion por banda de frecuencia, y genera directivas de mejora especificas.

### Jerarquia del curso

El curso tiene una estructura jerarquica fija (definida en FEAT-COURSE): **Curso -> Nivel (Milestone) -> Topic -> Knowledge -> Quiz**. Los **quizzes** contienen las oraciones evaluables, y cada oracion esta compuesta por tokens procesados linguisticamente. Cada token tiene asociado un `frequencyRank` que proviene de un procesamiento previo. El analizador clasifica estos tokens en bandas de frecuencia y calcula la distribucion resultante.

### Separacion entre analisis y agregacion

Esta funcionalidad involucra tres fases claramente diferenciadas:

1. **Fase de analisis (especifica de esta funcionalidad)**: El analizador de distribucion COCA clasifica los tokens de cada oracion en bandas de frecuencia, calcula la distribucion porcentual por nivel (o por trimestre si se usa la estrategia de quarters), compara contra los objetivos configurados y genera puntuaciones. Opera a nivel de **nivel/quarter** como unidad natural de evaluacion, aunque los datos se recogen desde los tokens individuales de cada quiz.

2. **Fase de agregacion (generica de la plataforma)**: Las puntuaciones por nivel son parte del arbol de resultados que la plataforma ContentAudit construye. El motor de agregacion permite navegar la jerarquia (Curso -> Nivel -> Topic -> Knowledge) con puntuaciones en cada nodo. Esta agregacion es identica para todos los analizadores del sistema.

3. **Fase de analisis de nivel (especifica de esta funcionalidad)**: Ademas de las puntuaciones, esta funcionalidad requiere evaluaciones especificas que van mas alla de la agregacion: evaluacion de progresion por banda de frecuencia entre niveles, y generacion de directivas de mejora (planner) que indican que bandas necesitan mas o menos palabras.

### Bandas de frecuencia COCA (Buckets)

Las palabras se clasifican en bandas segun su ranking de frecuencia en el corpus COCA. Las bandas configuradas actualmente son:

| Banda | Rango de ranking | Significado |
|-------|-----------------|-------------|
| top1k | 1 - 1000 | Palabras mas frecuentes del ingles |
| top2k | 1001 - 2000 | Palabras frecuentes |
| top3k | 2001 - 3000 | Palabras de frecuencia media |
| top4k | 3001 en adelante | Palabras de baja frecuencia (banda abierta) |

La ultima banda (top4k) es **abierta**: no tiene limite superior. Cualquier palabra con ranking 3001 o superior cae en esta banda. Esto incluye tanto palabras de ranking 3500 como palabras de ranking 15000. Este diseno es adecuado para los niveles A1-B2 actuales, pero podria requerir subdivision si se agregan niveles C1/C2 en el futuro.

Las bandas son configurables: su cantidad, nombres, valores limite y el indicador de banda abierta se definen en la configuracion. Sin embargo, el analisis actual opera con las cuatro bandas descritas arriba.

### Estrategias de analisis

El analizador soporta dos estrategias para evaluar la distribucion de vocabulario:

| Estrategia | Descripcion |
|------------|-------------|
| LEVELS | Analiza la distribucion por nivel CEFR completo (A1, A2, B1, B2). Un solo conjunto de objetivos por nivel. |
| QUARTERS | Subdivide cada nivel en 4 trimestres (Q1, Q2, Q3, Q4) y define objetivos especificos por trimestre con interpolacion lineal para los trimestres intermedios. |

**La configuracion actual usa la estrategia `quarters`**, que proporciona mayor granularidad y permite detectar problemas de distribucion dentro de un mismo nivel.

### Interpolacion lineal de trimestres

Cuando se usa la estrategia `quarters`, la configuracion define objetivos solo para el **trimestre inicial** (Q1) y el **trimestre final** (Q4) de cada nivel. Los trimestres intermedios (Q2 y Q3) se calculan por interpolacion lineal:

- Q1 = objetivos del trimestre inicial (configurados explicitamente)
- Q2 = Q1 + (Q4 - Q1) * (1/3)
- Q3 = Q1 + (Q4 - Q1) * (2/3)
- Q4 = objetivos del trimestre final (configurados explicitamente)

Ejemplo para A1, banda top1k: si Q1 tiene objetivo 90% y Q4 tiene objetivo 75%, entonces Q2 = 85.0% y Q3 = 80.0%. Esto produce una transicion suave: 90% -> 85% -> 80% -> 75%.

Los trimestres intermedios **heredan la direccionalidad** (kind: atLeast/atMost) del trimestre inicial, no del final.

### Direccionalidad de los objetivos (kind)

Cada objetivo tiene una direccionalidad que determina como se evalua:

- **atLeast** ("al menos"): el porcentaje real debe ser **igual o mayor** al objetivo. Estar por debajo es deficiente.
- **atMost** ("como maximo"): el porcentaje real debe ser **igual o menor** al objetivo. Estar por encima es excesivo.

La direccionalidad refleja la intencion pedagogica. Por ejemplo, en A1 el objetivo para top1k es "al menos 80%": se espera que la gran mayoria del vocabulario sea de alta frecuencia. En cambio, el objetivo para top4k en A1 es "como maximo 1%": se espera que casi no haya palabras de baja frecuencia.

### Objetivos por nivel y trimestre

Los objetivos actuales reflejan expectativas para un curso de ingles para adultos. Se presentan los objetivos por nivel CEFR (estrategia levels) y los objetivos por trimestre (estrategia quarters):

**Nivel A1**

| Banda | Objetivo nivel | Kind | Q1 | Q2 | Q3 | Q4 |
|-------|---------------|------|-----|-----|-----|-----|
| top1k | 80% | atLeast | 90% | 85.0% | 80.0% | 75% |
| top4k | 1% | atMost | 0% | 0.67% | 1.33% | 2% |

**Nivel A2**

| Banda | Objetivo nivel | Kind | Q1 | Q2 | Q3 | Q4 |
|-------|---------------|------|-----|-----|-----|-----|
| top1k | 70% | atMost | 75% | 71.67% | 68.33% | 65% |
| top4k | 10% | atMost | 2% | 6.33% | 10.67% | 15% |

**Nivel B1**

| Banda | Objetivo nivel | Kind | Q1 | Q2 | Q3 | Q4 |
|-------|---------------|------|-----|-----|-----|-----|
| top1k | 60% | atMost | 65% | 61.67% | 58.33% | 55% |
| top4k | 20% | atMost | 15% | 18.33% | 21.67% | 25% |

**Nivel B2**

| Banda | Objetivo nivel | Kind | Q1 | Q2 | Q3 | Q4 |
|-------|---------------|------|-----|-----|-----|-----|
| top1k | 50% | atMost | 55% | 51.67% | 48.33% | 45% |
| top4k | 30% | atLeast | 25% | 28.33% | 31.67% | 35% |

### Tolerancias

La evaluacion utiliza dos margenes de tolerancia configurables:

- **optimalRange**: 5 puntos porcentuales. Si la diferencia entre el porcentaje real y el objetivo es menor o igual a 5%, la evaluacion es OPTIMO.
- **adequateRange**: 10 puntos porcentuales. Si la diferencia es mayor a 5% pero menor o igual a 10%, la evaluacion es ADECUADO.

Estos valores son parametros de configuracion globales que aplican a todas las bandas y todos los niveles.

### Volumenes esperados

El curso actual contiene aproximadamente 608 knowledges distribuidos en 4 niveles (A1, A2, B1, B2), con un promedio de aproximadamente 19 quizzes por knowledge (alrededor de 11.500 quizzes en total). El analizador procesa cada token de cada oracion de cada quiz, lo que implica un volumen significativamente mayor al de otros analizadores que operan a nivel de oracion.

### Progresion esperada entre niveles

Se espera que ciertos patrones se mantengan al avanzar de un nivel al siguiente:

| Banda | Progresion esperada | Significado pedagogico |
|-------|--------------------|-----------------------|
| top1k | DESCENDENTE | Menos palabras de alta frecuencia a medida que avanza el nivel |
| top4k | ASCENDENTE | Mas palabras de baja frecuencia a medida que avanza el nivel |

### Configuracion ramp-up (variante para cursos introductorios)

Existe una configuracion alternativa para cursos "ramp-up" (introductorios) que usa bandas de frecuencia mucho mas estrechas (top 135, top 250, top 500, top 1000 palabras) y define objetivos por unidad en lugar de por nivel CEFR. Esta variante esta disenada para cursos donde el vocabulario es extremadamente restringido y la progresion es mas rapida que en un curso estandar. Ver la seccion de Open Questions para su estado actual.

---

## Reglas de Negocio

Las reglas se organizan en seis grupos segun la fase y el tema al que pertenecen:

- **Grupo A - Clasificacion de tokens y distribucion (R001-R006)**: reglas que describen como los tokens se clasifican en bandas de frecuencia y como se calcula la distribucion porcentual.
- **Grupo B - Evaluacion contra objetivos (R007-R013)**: reglas que describen como se compara la distribucion real contra los objetivos y como se calcula la puntuacion.
- **Grupo C - Estrategia de quarters e interpolacion (R014-R019)**: reglas que describen la subdivision en trimestres, la interpolacion lineal y la asignacion de contenido a trimestres.
- **Grupo D - Evaluacion de progresion (R020-R024)**: reglas que describen la evaluacion de progresion por banda de frecuencia entre niveles.
- **Grupo E - Agregacion (R025-R029)**: reglas que describen como las puntuaciones se agregan a traves de la jerarquia. Provistas por la plataforma.
- **Grupo F - Planner de directivas de mejora (R030-R034)**: reglas que describen la generacion de directivas de mejora para el creador de contenido.

---

### Grupo A - Clasificacion de tokens y distribucion

### Rule[F-COCA-R001] - Clasificacion de tokens en bandas de frecuencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada token de cada oracion del curso tiene asociado un `frequencyRank` (posicion en la lista de frecuencia COCA). El analizador clasifica cada token en la banda de frecuencia correspondiente segun su ranking:

- Un token con ranking entre 1 y 1000 se clasifica en la banda **top1k**.
- Un token con ranking entre 1001 y 2000 se clasifica en la banda **top2k**.
- Un token con ranking entre 2001 y 3000 se clasifica en la banda **top3k**.
- Un token con ranking de 3001 en adelante se clasifica en la banda **top4k**.

La clasificacion es excluyente: cada token pertenece a exactamente una banda. El criterio es que un token pertenece a la banda cuyo valor limite superior es el menor que es mayor o igual al ranking del token, excepto en la banda abierta que captura todo lo que excede el penultimo limite.

**Error**: "Token con frequencyRank invalido ({rank}) en el quiz {quizId} del knowledge {knowledgeId}"

### Rule[F-COCA-R002] - Banda abierta (top4k)
**Severity**: major | **Validation**: AUTO_VALIDATED

La ultima banda de frecuencia (top4k) es una banda **abierta**: no tiene limite superior. Esto significa que cualquier token con ranking de 3001 en adelante se agrupa en esta banda, independientemente de cuan alto sea su ranking. Un token con ranking 3500 y un token con ranking 15000 se tratan de la misma manera.

Este comportamiento es configurable a traves del indicador `open` en la definicion de bandas. Cuando `open` es verdadero, la ultima banda de la lista actua como banda abierta.

La banda abierta es adecuada para los niveles A1-B2 actuales, pero implica que no se distingue entre vocabulario de frecuencia media-baja (ranking 3001-5000) y vocabulario muy infrecuente (ranking 10000+). Esta distincion podria ser relevante para niveles C1/C2.

**Error**: N/A (esta regla describe un comportamiento de diseno)

### Rule[F-COCA-R003] - Configuracion de bandas de frecuencia
**Severity**: major | **Validation**: AUTO_VALIDATED

Las bandas de frecuencia se definen en la configuracion del sistema. Cada banda tiene un nombre identificador y un valor limite. Las bandas se ordenan de menor a mayor valor. La configuracion actual define cuatro bandas:

| Banda | Valor limite | Rango de ranking capturado |
|-------|-------------|---------------------------|
| top1k | 1000 | 1 - 1000 |
| top2k | 2000 | 1001 - 2000 |
| top3k | 3000 | 2001 - 3000 |
| top4k | 4000 | 3001 en adelante (abierta) |

Las bandas son configurables: se pueden agregar, eliminar o modificar bandas segun las necesidades del curso. Sin embargo, los objetivos por nivel deben estar alineados con las bandas definidas.

**Error**: "Configuracion de bandas invalida: los valores deben ser positivos y estar en orden ascendente"

### Rule[F-COCA-R004] - Tokens sin ranking de frecuencia
**Severity**: major | **Validation**: ASSUMPTION

Los tokens que no tienen un `frequencyRank` asociado (por ejemplo, nombres propios, numeros, o palabras no encontradas en el corpus COCA) deben ser tratados de manera definida. Se asume que estos tokens se excluyen del conteo de distribucion y no participan en el calculo de porcentajes.

[ASSUMPTION] Se asume que los tokens sin ranking de frecuencia se excluyen del analisis. Esto es razonable porque incluirlos en alguna banda distorsionaria la distribucion. Sin embargo, si la proporcion de tokens excluidos es muy alta, los resultados podrian no ser representativos. El sistema deberia reportar la cantidad de tokens excluidos para visibilidad.

**Error**: "Proporcion alta de tokens sin ranking de frecuencia en el nivel {nivel}: {porcentaje}% de tokens excluidos del analisis"

### Rule[F-COCA-R005] - Calculo de distribucion porcentual
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR (o trimestre, si se usa la estrategia quarters), se calcula el porcentaje de tokens en cada banda de frecuencia respecto al total de tokens clasificados en ese nivel/trimestre. La formula es:

porcentaje de la banda = (cantidad de tokens en la banda / total de tokens clasificados) * 100

Ejemplo: si en A1 hay 1000 tokens clasificados y 820 estan en la banda top1k, el porcentaje de top1k es 82.0%.

Los tokens se acumulan a traves de toda la cadena jerarquica: todos los tokens de todos los quizzes de todos los knowledges de todos los topics del nivel (o trimestre) se contabilizan juntos.

**Error**: "Error al calcular la distribucion porcentual para el nivel {nivel}: total de tokens clasificados es cero"

### Rule[F-COCA-R006] - Los datos de frecuencia provienen de procesamiento linguistico previo
**Severity**: critical | **Validation**: AUTO_VALIDATED

El ranking de frecuencia de cada token proviene de un procesamiento linguistico previo (lematizacion). El analizador utiliza el `frequencyRank` ya asociado a cada token; no consulta directamente el corpus COCA ni realiza busquedas de frecuencia. Esto es fundamental porque la frecuencia se basa en la forma **lematizada** de la palabra (su forma base), no en la forma flexionada que aparece en la oracion.

Por ejemplo, las formas "running", "ran", "runs" se lematizan a "run", y es el ranking de frecuencia de "run" el que se utiliza para la clasificacion.

**Error**: "Token sin datos de frecuencia en el quiz {quizId}: el procesamiento linguistico previo no asocio frequencyRank"

---

### Grupo B - Evaluacion contra objetivos

### Rule[F-COCA-R007] - Evaluacion de estado por banda (assessment)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada banda de frecuencia en cada nivel (o trimestre), se compara el porcentaje real contra el objetivo configurado. La evaluacion produce uno de cuatro estados:

| Estado | Condicion |
|--------|-----------|
| OPTIMO | La diferencia entre el porcentaje real y el objetivo esta dentro del rango optimo (5 puntos porcentuales) |
| ADECUADO | La diferencia esta fuera del rango optimo pero dentro del rango adecuado (10 puntos porcentuales) |
| DEFICIENTE | El porcentaje real esta por debajo de lo esperado, fuera del rango adecuado |
| EXCESIVO | El porcentaje real esta por encima de lo esperado, fuera del rango adecuado |

La determinacion de DEFICIENTE vs EXCESIVO depende de la direccionalidad del objetivo (kind), como se describe en R008.

**Error**: "Estado indeterminado para la banda {banda} en el nivel {nivel}: porcentaje real {real}%, objetivo {target}%"

### Rule[F-COCA-R008] - Semantica de la direccionalidad (atLeast / atMost)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La direccionalidad del objetivo (kind) determina como se interpreta la desviacion:

**Cuando kind = atLeast** ("al menos"):
- OPTIMO: el porcentaje real es mayor o igual a (objetivo - optimalRange)
- ADECUADO: el porcentaje real es mayor o igual a (objetivo - adequateRange) pero menor que (objetivo - optimalRange)
- DEFICIENTE: el porcentaje real es menor que (objetivo - adequateRange)
- No se produce estado EXCESIVO: superar un objetivo "al menos" no es un problema

**Cuando kind = atMost** ("como maximo"):
- OPTIMO: el porcentaje real es menor o igual a (objetivo + optimalRange)
- ADECUADO: el porcentaje real es menor o igual a (objetivo + adequateRange) pero mayor que (objetivo + optimalRange)
- EXCESIVO: el porcentaje real es mayor que (objetivo + adequateRange)
- No se produce estado DEFICIENTE: estar por debajo de un objetivo "como maximo" no es un problema

Ejemplo para A1 top1k (objetivo 80%, kind atLeast, optimalRange 5, adequateRange 10):
- 85% o mas: OPTIMO (>= 80 - 5 = 75%)
- 72%: ADECUADO (>= 80 - 10 = 70%, pero < 75%)
- 65%: DEFICIENTE (< 70%)

Ejemplo para A1 top4k (objetivo 1%, kind atMost, optimalRange 5, adequateRange 10):
- 4%: OPTIMO (<= 1 + 5 = 6%)
- 9%: ADECUADO (<= 1 + 10 = 11%, pero > 6%)
- 15%: EXCESIVO (> 11%)

**Error**: "Kind desconocido '{kind}' para la banda {banda} en el nivel {nivel}: debe ser 'atLeast' o 'atMost'"

### Rule[F-COCA-R009] - Tolerancias optimalRange y adequateRange
**Severity**: major | **Validation**: AUTO_VALIDATED

Las tolerancias son valores configurables globales que se aplican uniformemente a todas las bandas y todos los niveles:

- **optimalRange** (actualmente 5): define el margen en puntos porcentuales para que una evaluacion sea OPTIMO. Si la desviacion del objetivo es menor o igual a este valor, el resultado es OPTIMO.
- **adequateRange** (actualmente 10): define el margen en puntos porcentuales para que una evaluacion sea ADECUADO. Si la desviacion excede el optimalRange pero no excede el adequateRange, el resultado es ADECUADO.

El adequateRange debe ser mayor que el optimalRange. Ambos valores deben ser positivos.

**Error**: "Configuracion de tolerancias invalida: optimalRange ({optimal}) debe ser menor que adequateRange ({adequate}) y ambos deben ser positivos"

### Rule[F-COCA-R010] - Puntuacion por banda individual (bucket score)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada banda en cada nivel/trimestre recibe una puntuacion entre 0.0 y 1.0 basada en su estado de evaluacion:

| Estado | Puntuacion |
|--------|-----------|
| OPTIMO | 1.0 |
| ADECUADO | 0.8 |
| DEFICIENTE | max(0.0, 0.8 - (distancia - adequateRange) * 0.04) |
| EXCESIVO | max(0.0, 0.8 - (distancia - adequateRange) * 0.04) |

Donde "distancia" es el valor absoluto de la diferencia entre el porcentaje real y el objetivo.

Para los estados DEFICIENTE y EXCESIVO, la puntuacion parte de 0.8 y se degrada 0.04 por cada punto porcentual adicional de distancia mas alla del adequateRange. Esto significa que a 20 puntos porcentuales de distancia mas alla del rango adecuado, la puntuacion llega a 0.0.

Ejemplo: si el objetivo es 80% (atLeast), adequateRange es 10, y el porcentaje real es 60%:
- Distancia = |60 - 80| = 20
- Estado = DEFICIENTE (60 < 80 - 10 = 70)
- Puntuacion = max(0.0, 0.8 - (20 - 10) * 0.04) = max(0.0, 0.8 - 0.4) = 0.4

**Error**: "Puntuacion fuera de rango [0.0, 1.0] calculada para la banda {banda} en el nivel {nivel}: {puntuacion}"

### Rule[F-COCA-R011] - Puntuacion por trimestre (quarter score)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de un trimestre es el **promedio de las puntuaciones de todas sus bandas** que tienen objetivo definido en ese trimestre. No todas las bandas necesariamente tienen objetivo en cada trimestre; solo participan en el promedio aquellas que lo tienen.

Ejemplo: si en A1 Q1 solo top1k y top4k tienen objetivos, la puntuacion del trimestre es el promedio de las puntuaciones de top1k y top4k.

**Error**: "Error al calcular la puntuacion del trimestre Q{n} del nivel {nivel}: ninguna banda tiene objetivo definido"

### Rule[F-COCA-R012] - Puntuacion por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de un nivel depende de la estrategia utilizada:

- **Estrategia LEVELS**: la puntuacion del nivel es el promedio de las puntuaciones de las bandas que tienen objetivo definido a nivel de nivel.
- **Estrategia QUARTERS**: la puntuacion del nivel es un promedio ponderado que combina las puntuaciones de las bandas del nivel, las puntuaciones de los trimestres, y las puntuaciones de los topics dentro del nivel.

La combinacion exacta para la estrategia quarters implica considerar tanto la evaluacion a nivel global del nivel como la evaluacion granular por trimestre.

**Error**: "Error al calcular la puntuacion del nivel {nivel}: {detalle}"

### Rule[F-COCA-R013] - Puntuacion general del curso (overall score)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion general del analisis de distribucion COCA es el **promedio de las puntuaciones de todos los niveles** CEFR evaluados. Si un nivel no tiene datos (no hay tokens clasificados), no participa en el promedio.

**Error**: "Error al calcular la puntuacion general: {detalle}"

---

### Grupo C - Estrategia de quarters e interpolacion

### Rule[F-COCA-R014] - Dos estrategias de analisis
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador soporta dos estrategias de analisis configurables:

- **LEVELS**: Evalua la distribucion de vocabulario por nivel CEFR completo. Cada nivel tiene un unico conjunto de objetivos por banda.
- **QUARTERS**: Subdivide cada nivel en 4 trimestres y define objetivos por trimestre, permitiendo evaluar la progresion de vocabulario dentro de cada nivel.

La estrategia se define en la configuracion y aplica a todos los niveles del curso por igual. La configuracion actual usa la estrategia **quarters**.

Ambas estrategias comparten la misma logica de clasificacion de tokens (Grupo A) y evaluacion contra objetivos (Grupo B). La diferencia radica en la granularidad: quarters permite detectar problemas que la estrategia levels podria enmascarar al promediar un nivel completo.

**Error**: "Estrategia de analisis desconocida: '{estrategia}'. Las estrategias validas son: LEVELS, QUARTERS"

### Rule[F-COCA-R015] - Interpolacion lineal de trimestres intermedios
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando se usa la estrategia quarters, la configuracion define objetivos solo para el trimestre inicial (Q1) y el trimestre final (Q4) de cada nivel. Los trimestres intermedios (Q2, Q3) se calculan por interpolacion lineal:

Para cada banda en un nivel dado:
- objetivo_Q1 = objetivo del trimestre inicial (configurado explicitamente)
- objetivo_Q2 = objetivo_Q1 + (objetivo_Q4 - objetivo_Q1) * (1/3)
- objetivo_Q3 = objetivo_Q1 + (objetivo_Q4 - objetivo_Q1) * (2/3)
- objetivo_Q4 = objetivo del trimestre final (configurado explicitamente)

La interpolacion se aplica de manera independiente para cada banda de frecuencia. Esto produce una transicion suave y lineal de los objetivos a lo largo del nivel.

Ejemplo completo para A1:
- top1k: Q1=90%, Q2=85.0%, Q3=80.0%, Q4=75%
- top4k: Q1=0%, Q2=0.67%, Q3=1.33%, Q4=2%

**Error**: "Error al interpolar trimestres para el nivel {nivel}: faltan datos de trimestre inicial o final"

### Rule[F-COCA-R016] - Herencia de direccionalidad en trimestres interpolados
**Severity**: major | **Validation**: AUTO_VALIDATED

Los trimestres intermedios (Q2, Q3) calculados por interpolacion heredan la **direccionalidad** (kind: atLeast/atMost) del trimestre inicial (Q1), no del trimestre final (Q4). Esto significa que si Q1 tiene kind "atLeast" para una banda, Q2 y Q3 tambien tendran kind "atLeast" para esa banda, independientemente de lo que defina Q4.

Ejemplo: si en A1 el trimestre inicial define top1k con kind "atLeast" y el trimestre final define top1k con kind "atLeast", los trimestres Q2 y Q3 heredan "atLeast". Si hubiera una discrepancia entre Q1 y Q4 (lo cual seria una inconsistencia de configuracion), prevalece Q1.

**Error**: N/A (esta regla describe un comportamiento de herencia)

### Rule[F-COCA-R017] - Asignacion de contenido a trimestres
**Severity**: major | **Validation**: ASSUMPTION

Cuando se usa la estrategia quarters, las oraciones (y sus tokens) deben asignarse a uno de los cuatro trimestres de su nivel. La asignacion se basa en la **posicion ordinal de los topics** dentro del nivel: los topics se dividen equitativamente en 4 grupos, cada grupo correspondiendo a un trimestre.

[ASSUMPTION] Se asume que los topics se dividen de manera secuencial y equitativa en 4 trimestres. Si un nivel tiene 12 topics, se asignan 3 topics por trimestre. Si el numero de topics no es divisible por 4, los primeros trimestres reciben un topic adicional (distribucion por redondeo). Este mecanismo de asignacion necesita confirmacion, ya que la documentacion de analisis no detalla el algoritmo exacto de particion.

**Error**: "Error al asignar topics a trimestres para el nivel {nivel}: el nivel no tiene topics"

### Rule[F-COCA-R018] - Configuracion de objetivos por trimestre
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada nivel CEFR debe tener configurados los objetivos para el trimestre inicial y el trimestre final cuando se usa la estrategia quarters. Cada objetivo de trimestre especifica, para cada banda:
- El nombre de la banda
- El porcentaje objetivo
- La direccionalidad (kind: atLeast o atMost)

Si falta la configuracion de trimestre inicial o final para un nivel, el sistema no puede calcular los trimestres interpolados y por lo tanto no puede evaluar ese nivel con la estrategia quarters.

Los objetivos por trimestre actuales son (ver seccion Contexto para la tabla completa):
- A1: Q1 top1k 90% atLeast, top4k 0% atMost / Q4 top1k 75% atLeast, top4k 2% atMost
- A2: Q1 top1k 75% atLeast, top4k 2% atMost / Q4 top1k 65% atLeast, top4k 15% atMost
- B1: Q1 top1k 65% atLeast, top4k 15% [kind por definir] / Q4 top1k 55% atLeast, top4k 25% atMost
- B2: Q1 top1k 55% atLeast, top4k 25% [kind por definir] / Q4 top1k 45% atLeast, top4k 35% atMost

Nota: Los niveles B1 y B2 tienen campos "kind" faltantes en el trimestre inicial para la banda top4k. Ver Doubt[DOUBT-MISSING-KIND].

**Error**: "Configuracion incompleta para el trimestre {trimestre} del nivel {nivel}: falta la banda {banda}"

### Rule[F-COCA-R019] - Estrategia levels usa objetivos de nivel directamente
**Severity**: major | **Validation**: AUTO_VALIDATED

Cuando se usa la estrategia LEVELS, los objetivos se evaluan directamente a nivel CEFR completo, sin subdivision en trimestres. Cada nivel tiene un conjunto de objetivos por banda:

- A1: top1k 80% atLeast, top4k 1% atMost
- A2: top1k 70% atMost, top4k 10% atMost
- B1: top1k 60% atMost, top4k 20% atMost
- B2: top1k 50% atMost, top4k 30% atLeast

Todos los tokens del nivel se acumulan en una sola distribucion y se evaluan contra estos objetivos unicos.

**Error**: "Configuracion de objetivos no encontrada para el nivel {nivel} con estrategia LEVELS"

---

### Grupo D - Evaluacion de progresion

### Rule[F-COCA-R020] - Progresion esperada por banda de frecuencia
**Severity**: major | **Validation**: AUTO_VALIDATED

El sistema define una progresion esperada para ciertas bandas de frecuencia entre niveles CEFR. La progresion esperada indica como deberia evolucionar el porcentaje de tokens en esa banda al avanzar de un nivel al siguiente:

| Banda | Progresion esperada | Significado |
|-------|--------------------|-----------|
| top1k | DESCENDENTE | El porcentaje de palabras muy frecuentes debe disminuir de A1 a B2 |
| top4k | ASCENDENTE | El porcentaje de palabras poco frecuentes debe aumentar de A1 a B2 |

No todas las bandas tienen progresion esperada configurada. Las bandas intermedias (top2k, top3k) no tienen progresion definida en la configuracion actual, lo que implica que su comportamiento entre niveles no se evalua explicitamente.

**Error**: N/A (esta regla describe la configuracion de progresion)

### Rule[F-COCA-R021] - Algoritmo de evaluacion de progresion real
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada banda que tiene progresion esperada configurada, el sistema evalua la progresion real comparando los porcentajes entre niveles consecutivos. El algoritmo es:

1. Se toman los porcentajes reales de la banda en cada nivel CEFR (A1, A2, B1, B2)
2. Si hay menos de 2 niveles con datos, la progresion se considera ESTATICA
3. Para cada par de niveles consecutivos, se determina si el porcentaje sube, baja o se mantiene (considerando un margen)
4. Se contabilizan los incrementos y decrementos
5. Se determina el estado de progresion:

| Estado | Condicion |
|--------|-----------|
| ASCENDENTE | Solo hay incrementos (el porcentaje sube consistentemente) |
| DESCENDENTE | Solo hay decrementos (el porcentaje baja consistentemente) |
| ESTATICA | No hay cambios significativos (ni incrementos ni decrementos) |
| IRREGULAR | Hay tanto incrementos como decrementos (el porcentaje sube y baja sin patron claro) |

La progresion real se compara contra la progresion esperada (R020) para determinar si la distribucion de vocabulario evoluciona correctamente a lo largo del curso.

**Error**: "No se pudo evaluar la progresion de la banda {banda}: {detalle}"

### Rule[F-COCA-R022] - Progresion evalua solo niveles con datos
**Severity**: major | **Validation**: AUTO_VALIDATED

La evaluacion de progresion solo considera los niveles CEFR que tienen tokens clasificados. Si un nivel no tiene datos (por ejemplo, no hay quizzes con tokens en B1), se omite de la evaluacion de progresion. La progresion se evalua entre los niveles restantes en su orden natural (A1, A2, B2).

Si menos de 2 niveles tienen datos, la progresion no puede evaluarse y se reporta como ESTATICA.

**Error**: N/A (esta regla describe un comportamiento de filtrado)

### Rule[F-COCA-R023] - Margen de cambio significativo en progresion
**Severity**: minor | **Validation**: ASSUMPTION

Al evaluar si el porcentaje de una banda sube o baja entre dos niveles consecutivos, se utiliza un margen para evitar que fluctuaciones insignificantes se interpreten como cambios reales. Solo los cambios que superan este margen se contabilizan como incrementos o decrementos.

[ASSUMPTION] Se asume que existe un margen de cambio significativo, aunque la documentacion de analisis no especifica su valor exacto. Este margen evita que diferencias de decimas de punto porcentual afecten la evaluacion de progresion. El valor exacto del margen requiere confirmacion.

**Error**: N/A (esta regla describe un parametro del algoritmo)

### Rule[F-COCA-R024] - Resultado de la evaluacion de progresion
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado de la evaluacion de progresion incluye, para cada banda con progresion configurada:
- Nombre de la banda
- Progresion esperada (ASCENDENTE o DESCENDENTE, segun configuracion)
- Progresion real (ASCENDENTE, DESCENDENTE, ESTATICA o IRREGULAR, segun evaluacion)
- Indicador de si la progresion real coincide con la esperada

Este resultado permite al creador de contenido identificar si la distribucion de vocabulario evoluciona correctamente a lo largo del curso. Por ejemplo, si top1k deberia ser DESCENDENTE pero la progresion real es IRREGULAR, indica que hay niveles donde se introdujo demasiado vocabulario frecuente cuando deberia haberse reducido.

**Error**: N/A (esta regla describe la estructura del resultado)

---

### Grupo E - Agregacion de puntuaciones (provista por la plataforma)

> **Nota**: Las reglas de este grupo describen el comportamiento esperado del motor de agregacion generico de ContentAudit. No son implementadas por el analizador de distribucion COCA, sino por la plataforma. Se documentan aqui porque el usuario percibe el resultado final como parte de esta funcionalidad y necesita entender como se construye la jerarquia de resultados. Estas mismas reglas de agregacion aplican de manera identica a cualquier otro analizador del sistema.

### Rule[F-COCA-R025] - Resultados jerarquicos por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

El resultado del analisis de distribucion COCA se estructura de manera jerarquica: para cada nivel CEFR, se presenta la distribucion de bandas, las puntuaciones, y los nodos hijos (topics). Cada topic a su vez contiene la distribucion de bandas de sus tokens y sus nodos hijos (knowledges). Esta jerarquia permite al usuario navegar desde el nivel general hacia los detalles mas finos.

La estructura jerarquica es: **Nivel -> (Trimestres, si aplica) -> Topic -> Knowledge -> (Bandas de frecuencia)**.

**Error**: N/A (esta regla describe la estructura del resultado)

### Rule[F-COCA-R026] - Puntuacion por topic (agregacion de la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de distribucion COCA de un topic se calcula a partir de las puntuaciones de sus knowledges. La plataforma agrega las puntuaciones a traves de la jerarquia utilizando el mecanismo generico de agregacion. Los topics sin knowledges con datos no participan en la agregacion del nivel padre.

**Error**: "Error al calcular la puntuacion del topic {topicId}: {detalle}"

### Rule[F-COCA-R027] - Distribucion de bandas por nodo de la jerarquia
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada nodo de la jerarquia (nivel, topic, knowledge) tiene asociada su propia distribucion de bandas de frecuencia, calculada a partir de los tokens de todos los quizzes que pertenecen a ese nodo. Esto permite al usuario ver no solo la puntuacion agregada sino tambien la distribucion real de vocabulario en cada segmento del curso.

Por ejemplo, un topic especifico podria tener 70% de tokens en top1k y 15% en top4k, mientras que otro topic del mismo nivel podria tener 85% en top1k y 2% en top4k. Esta informacion es valiosa para localizar donde se concentran los problemas de distribucion.

**Error**: N/A (esta regla describe la disponibilidad de datos)

### Rule[F-COCA-R028] - Informacion por banda en cada nodo
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada banda de frecuencia en cada nodo de la jerarquia, el resultado incluye:
- Nombre de la banda
- Cantidad de tokens en la banda
- Porcentaje respecto al total de tokens clasificados del nodo
- Porcentaje objetivo (si aplica al nivel del nodo)
- Puntuacion de la banda (si aplica)
- Estado de evaluacion (OPTIMO, ADECUADO, DEFICIENTE, EXCESIVO, si aplica)

Los campos de objetivo, puntuacion y estado solo aplican a los nodos que tienen objetivos definidos (generalmente niveles y trimestres). Los topics y knowledges tienen la distribucion y el conteo pero no necesariamente una evaluacion contra objetivos propios.

**Error**: N/A (esta regla describe la estructura de datos)

### Rule[F-COCA-R029] - Puntuaciones disponibles en cada nivel de la jerarquia
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado del analisis debe hacer disponible la puntuacion de distribucion COCA en cada nivel de la jerarquia del curso. La plataforma construye un arbol de resultados donde cada nodo tiene asociada la puntuacion de cada analizador ejecutado. Esto permite al creador de contenido navegar la jerarquia y localizar exactamente donde se concentran los problemas de distribucion de vocabulario.

**Error**: N/A (esta regla describe la disponibilidad de datos)

---

### Grupo F - Planner de directivas de mejora

### Rule[F-COCA-R030] - Generacion de directivas de mejora
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada nivel (o trimestre, segun la estrategia) donde la distribucion no alcanza los objetivos, el sistema genera directivas de mejora que indican al creador de contenido que acciones tomar. Las directivas se dividen en dos categorias:

- **bucketsToEnrich**: Bandas donde faltan tokens. Indica que el nivel/trimestre necesita **mas** palabras de esa frecuencia.
- **bucketsToReduce**: Bandas donde sobran tokens. Indica que el nivel/trimestre necesita **menos** palabras de esa frecuencia.

Cada directiva incluye la banda afectada y el rango de frecuencia correspondiente para facilitar la busqueda de palabras de reemplazo.

**Error**: N/A (esta regla describe una funcionalidad informativa)

### Rule[F-COCA-R031] - Rango de frecuencia en directivas
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada directiva de mejora incluye el rango de frecuencia (from/to) de la banda afectada, para que el creador de contenido sepa en que franja de frecuencia buscar palabras:

- Si la directiva dice "enriquecer top2k", incluye el rango 1001-2000 para que el creador sepa que debe buscar palabras con ranking entre 1001 y 2000.
- Si la directiva dice "reducir top4k", incluye el rango 3001+ (o 3001-4000) para que el creador sepa que palabras de esa franja estan sobrando.

Para la banda abierta (top4k), el rango superior se indica como abierto o se utiliza el valor limite de la banda como referencia.

**Error**: N/A (esta regla describe la estructura de la directiva)

### Rule[F-COCA-R032] - Directivas se generan solo para bandas fuera de rango
**Severity**: major | **Validation**: AUTO_VALIDATED

Las directivas de mejora se generan unicamente para las bandas que tienen estado DEFICIENTE o EXCESIVO. Las bandas con estado OPTIMO o ADECUADO no generan directivas. Esto evita sobrecargar al creador de contenido con sugerencias innecesarias cuando la distribucion ya es aceptable.

- Una banda DEFICIENTE genera una directiva de tipo **bucketsToEnrich** (necesita mas palabras).
- Una banda EXCESIVA genera una directiva de tipo **bucketsToReduce** (necesita menos palabras).

**Error**: N/A (esta regla describe la logica de generacion)

### Rule[F-COCA-R033] - Directivas a nivel de trimestre (estrategia quarters)
**Severity**: minor | **Validation**: ASSUMPTION

Cuando se usa la estrategia quarters, las directivas de mejora se generan por trimestre, no solo por nivel. Esto permite al creador de contenido saber no solo que un nivel tiene problemas, sino en que segmento especifico del nivel debe actuar.

[ASSUMPTION] Se asume que las directivas se generan tanto a nivel de nivel como de trimestre cuando se usa la estrategia quarters. Esto es consistente con la granularidad del analisis por trimestres. Si las directivas solo se generan a nivel de nivel, la informacion por trimestre se pierde y el creador de contenido tendria menos precision para localizar los problemas.

**Error**: N/A (esta regla describe el nivel de granularidad)

### Rule[F-COCA-R034] - Contenido de las directivas de mejora
**Severity**: minor | **Validation**: ASSUMPTION

Cada directiva de mejora contiene la siguiente informacion:
- Tipo de accion (enriquecer o reducir)
- Banda de frecuencia afectada (nombre y rango)
- Porcentaje real de la banda
- Porcentaje objetivo de la banda
- Diferencia entre real y objetivo

[ASSUMPTION] Se asume que las directivas contienen suficiente informacion para que el creador de contenido pueda actuar sin necesidad de consultar otros datos del analisis. El contenido exacto y formato de las directivas puede refinarse segun las necesidades del equipo de contenido.

**Error**: N/A (esta regla describe la estructura de la directiva)

---

## User Journeys

### Journey[F-COCA-J001] - Auditar la distribucion de vocabulario de un curso completo
**Validation**: AUTO_VALIDATED

1. El usuario inicia una auditoria de un curso previamente cargado en el sistema ContentAudit
2. El sistema recorre la jerarquia del curso: para cada nivel (milestone), sus topics, sus knowledges, y los quizzes de cada knowledge
3. Para cada quiz, el analizador obtiene los tokens con su `frequencyRank` (proveniente del procesamiento linguistico previo)
4. El analizador clasifica cada token en su banda de frecuencia correspondiente (top1k, top2k, top3k, top4k) segun R001
5. Para cada nivel (o trimestre si se usa la estrategia quarters), el analizador calcula la distribucion porcentual de tokens por banda (R005)
6. El analizador compara la distribucion real contra los objetivos configurados para cada banda y genera puntuaciones (R007, R010)
7. Si se usa la estrategia quarters, los objetivos de los trimestres intermedios se calculan por interpolacion lineal (R015)
8. La plataforma agrega las puntuaciones a traves de la jerarquia del curso, construyendo el arbol de resultados con distribucion de bandas en cada nodo (R025-R029)
9. El sistema evalua la progresion por banda de frecuencia entre niveles (R021)
10. El sistema genera directivas de mejora para los niveles/trimestres que no alcanzan sus objetivos (R030)
11. El usuario recibe un informe con la puntuacion general, la puntuacion y distribucion por nivel, el estado de progresion por banda, y las directivas de mejora

### Journey[F-COCA-J002] - Consultar la distribucion de vocabulario de un nivel especifico
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
2. El usuario selecciona un nivel CEFR especifico (por ejemplo, A2)
3. El sistema muestra para el nivel seleccionado:
   - Puntuacion general del nivel
   - Distribucion porcentual de tokens por banda (top1k, top2k, top3k, top4k)
   - Evaluacion de cada banda contra su objetivo (OPTIMO, ADECUADO, DEFICIENTE, EXCESIVO)
   - Si la estrategia es quarters: la distribucion y evaluacion por cada trimestre (Q1-Q4)
4. El sistema muestra la lista de topics del nivel con su distribucion de bandas, permitiendo identificar que topics tienen distribucion problematica
5. El usuario puede profundizar en topics y knowledges para localizar donde se concentran las palabras de frecuencia inadecuada
6. Si hay directivas de mejora para este nivel, el sistema las muestra indicando que bandas necesitan mas o menos palabras

### Journey[F-COCA-J003] - Evaluar la progresion de vocabulario entre niveles
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
2. El usuario consulta el estado de progresion por banda de frecuencia
3. Para la banda top1k, el usuario verifica si la progresion es DESCENDENTE (lo esperado): que el porcentaje de palabras de alta frecuencia disminuya al avanzar de A1 a B2
4. Para la banda top4k, el usuario verifica si la progresion es ASCENDENTE (lo esperado): que el porcentaje de palabras de baja frecuencia aumente al avanzar de A1 a B2
5. Si la progresion de alguna banda es IRREGULAR o ESTATICA, el usuario identifica en que transicion entre niveles se rompe el patron esperado
6. El usuario revisa los porcentajes reales por nivel para localizar donde el vocabulario no evoluciona como se espera pedagogicamente
7. El usuario puede tomar acciones correctivas sobre el contenido de los niveles afectados, guiado por las directivas de mejora (J004)

### Journey[F-COCA-J004] - Usar las directivas de mejora para corregir el contenido
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria y encontrado que un nivel/trimestre tiene bandas con estado DEFICIENTE o EXCESIVO
2. El usuario consulta las directivas de mejora generadas para ese nivel/trimestre
3. Para cada directiva de tipo "enriquecer" (bucketsToEnrich), el usuario identifica que debe agregar mas palabras de la franja de frecuencia indicada. Por ejemplo: "Enriquecer top2k: agregar palabras con ranking 1001-2000"
4. Para cada directiva de tipo "reducir" (bucketsToReduce), el usuario identifica que debe reemplazar palabras de la franja de frecuencia indicada por palabras de otras franjas. Por ejemplo: "Reducir top4k: reemplazar palabras con ranking 3001+ por palabras mas frecuentes"
5. El usuario localiza los quizzes afectados navegando la jerarquia (nivel -> topic -> knowledge -> quiz)
6. El usuario modifica el contenido de los quizzes segun las directivas
7. El usuario re-ejecuta la auditoria para verificar que los cambios mejoraron la distribucion

### Journey[F-COCA-J005] - Navegar la jerarquia para localizar problemas de vocabulario
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de distribucion COCA (J001)
2. El usuario observa que el nivel B1 tiene una puntuacion baja (por ejemplo, 0.6)
3. El usuario profundiza en los trimestres de B1 (si se usa la estrategia quarters) y encuentra que Q3 tiene la puntuacion mas baja (0.4)
4. El usuario profundiza en los topics del Q3 de B1 y encuentra que el topic "Travel Vocabulary" tiene 40% de tokens en top4k (cuando el objetivo es 21.67% atMost)
5. El usuario profundiza en los knowledges de "Travel Vocabulary" y encuentra que el knowledge "Airport Procedures" tiene una concentracion inusual de vocabulario tecnico de baja frecuencia
6. El usuario identifica que varias oraciones de "Airport Procedures" usan vocabulario especializado (boarding, customs, luggage carousel) que infla la banda top4k
7. El usuario decide si simplificar el vocabulario o aceptar la desviacion como apropiada para el contexto tematico

### Journey[F-COCA-J006] - Comparar resultados entre estrategia levels y quarters
**Validation**: ASSUMPTION

1. El usuario ejecuta la auditoria con la estrategia LEVELS
2. El usuario observa que el nivel A2 tiene estado OPTIMO con puntuacion 0.95
3. El usuario cambia la configuracion a la estrategia QUARTERS y re-ejecuta la auditoria
4. El usuario descubre que si bien A2 en promedio es adecuado, Q1 de A2 tiene exceso de vocabulario frecuente (top1k al 85% cuando el objetivo Q1 es 75% atLeast) y Q4 tiene deficit de vocabulario variado (top4k al 5% cuando el objetivo Q4 es 15% atMost)
5. El usuario concluye que la estrategia quarters proporciona informacion mas granular y util para mejorar el contenido dentro de cada nivel

[ASSUMPTION] Se asume que el usuario puede cambiar de estrategia y re-ejecutar la auditoria. Esta capacidad es util para comparar perspectivas, pero requiere que ambas estrategias esten implementadas y disponibles.

---

## Open Questions

### Doubt[DOUBT-OPEN-BUCKET] - Deberia la banda abierta top4k subdividirse para futuros niveles C1/C2?
**Status**: OPEN

La banda top4k agrupa todos los tokens con ranking 3001 o superior sin distinguir entre vocabulario de frecuencia media-baja (3001-5000) y vocabulario muy infrecuente (10000+). Para los niveles A1-B2 actuales esto es funcional, pero si en el futuro se agregan niveles C1 y C2, seria necesario distinguir entre vocabulario de ranking 4001-6000, 6001-10000, y 10000+, ya que los niveles avanzados requieren precision en la estratificacion del vocabulario.

**Pregunta**: Se debe planificar la subdivision de la banda abierta desde ahora, o se deja como esta y se ajusta cuando se agreguen niveles C1/C2?

- [ ] Opcion A: Mantener top4k como banda abierta (suficiente para A1-B2, simplicidad)
- [ ] Opcion B: Subdividir ahora en top4k, top6k, top10k para preparar futuros niveles
- [ ] Opcion C: Dejar como esta pero documentar la necesidad de subdivision futura como deuda conocida

### Doubt[DOUBT-MISSING-KIND] - Campos "kind" faltantes en la configuracion de B1 y B2
**Status**: OPEN

En la configuracion actual, los niveles B1 y B2 tienen campos "kind" faltantes en el trimestre inicial (initialQuarter) para la banda top4k:

- B1 initialQuarter top4k: targetPercentage 15, **falta kind**
- B2 initialQuarter top4k: targetPercentage 25, **falta kind**

**Pregunta**: Es esto un error en la configuracion? Si es asi, que valor deberia tener el campo "kind" para estos casos?

- [ ] Opcion A: Es un error, deberia ser "atMost" (consistente con el patron de las otras bandas top4k en trimestres intermedios)
- [ ] Opcion B: Es un error, deberia ser "atLeast" (consistente con B2 nivel que tiene top4k "atLeast")
- [ ] Opcion C: No es un error, hay un valor por defecto implicito

Si el campo falta y no hay valor por defecto, los trimestres interpolados Q2 y Q3 heredarian un kind indefinido (segun R016), lo cual produciria resultados impredecibles en la evaluacion.

### Doubt[DOUBT-RAMPUP] - La configuracion ramp-up es parte de esta funcionalidad o deberia ser una funcionalidad separada?
**Status**: OPEN

Existe una configuracion alternativa de ramp-up para cursos introductorios que utiliza bandas de frecuencia mucho mas estrechas (top 135, top 250, top 500, top 1000) y define objetivos por unidad en lugar de por nivel CEFR. Esta configuracion esta presente en los archivos de analisis pero no esta claro si el flujo principal la utiliza activamente.

**Pregunta**: Deberia la configuracion ramp-up incluirse como parte de esta funcionalidad (FEAT-COCA) o deberia ser una funcionalidad separada?

- [ ] Opcion A: Incluirla como una variante de esta funcionalidad (misma logica, diferente configuracion)
- [ ] Opcion B: Crearla como funcionalidad separada (FEAT-COCA-RAMPUP) con sus propias reglas
- [ ] Opcion C: Excluirla del alcance actual (no se implementa hasta que se confirme su uso)

La documentacion de analisis indica que el orquestador principal solo referencia la configuracion de curso (`course`), no la de ramp-up. Si no se usa activamente, incluirla podria agregar complejidad innecesaria.

### Doubt[DOUBT-KIND-INCONSISTENCY] - Inconsistencia de direccionalidad entre A1 y A2+ para top1k
**Status**: OPEN

Hay una inconsistencia notable en la configuracion de los objetivos de top1k entre niveles:

- **A1**: top1k tiene kind "atLeast" (80% al menos). Estar por encima de 80% esta bien.
- **A2**: top1k tiene kind "atMost" (70% como maximo). Estar por encima de 70% es problematico.

Este cambio de semantica entre A1 y A2 significa que en A1 se premia tener mucho vocabulario frecuente, pero a partir de A2 se penaliza. Esto tiene logica pedagogica (en A1 se quiere reforzar vocabulario basico, en A2 ya se quiere diversificacion), pero el cambio abrupto de "al menos" a "como maximo" podria producir resultados confusos si el porcentaje real esta entre ambos objetivos.

**Pregunta**: Esta inconsistencia es intencional y refleja la intencion pedagogica, o deberia unificarse la semantica?

- [ ] Opcion A: Es intencional, refleja correctamente la transicion pedagogica de A1 a A2
- [ ] Opcion B: Deberia unificarse a "atMost" en todos los niveles para consistencia
- [ ] Opcion C: Se necesita una semantica mas matizada (por ejemplo, un rango min-max en lugar de atLeast/atMost)

### Doubt[DOUBT-KIND-INHERITANCE] - Los trimestres intermedios deberian heredar kind del trimestre inicial?
**Status**: OPEN

Actualmente, los trimestres interpolados (Q2, Q3) heredan la direccionalidad (kind) del trimestre inicial (Q1). Esto funciona bien cuando Q1 y Q4 tienen el mismo kind, pero podria producir resultados inesperados si Q1 y Q4 tienen kinds diferentes (aunque esto no ocurre en la configuracion actual).

**Pregunta**: La herencia de kind desde el trimestre inicial es el comportamiento correcto, o deberia considerarse una logica de transicion?

- [ ] Opcion A: Heredar del trimestre inicial es correcto (es el comportamiento actual y la configuracion no tiene conflictos de kind entre Q1 y Q4)
- [ ] Opcion B: Deberia heredarse del trimestre final, ya que Q2 y Q3 estan "en transicion" hacia Q4
- [ ] Opcion C: Deberia definirse kind explicitamente para Q2 y Q3 en la configuracion, sin interpolacion

### Doubt[DOUBT-PERFORMANCE] - Consideraciones de rendimiento para cursos grandes
**Status**: OPEN

Este analizador procesa cada token de cada oracion de cada quiz del curso. Con ~11.500 quizzes y un promedio estimado de ~10 tokens por oracion, se procesan ~115.000 tokens. Para cursos mas grandes o cursos con mas contenido por quiz, este numero podria crecer significativamente.

**Pregunta**: Se necesita algun mecanismo para gestionar el rendimiento (procesamiento incremental, cache, limites), o el volumen actual es manejable sin optimizaciones especiales?

- [ ] Opcion A: El volumen actual es manejable, no se necesitan optimizaciones
- [ ] Opcion B: Se necesita procesamiento incremental (solo re-analizar lo que cambio)
- [ ] Opcion C: Se necesitan limites o muestreo para cursos muy grandes

### Doubt[DOUBT-HIERARCHY-DEPTH] - Se necesita la jerarquia completa Level -> Quarter -> Topic -> Knowledge?
**Status**: OPEN

El resultado del analisis incluye una jerarquia profunda: Nivel -> Trimestres -> Topics -> Knowledges, cada uno con su distribucion de bandas. Esta granularidad es valiosa para la localizacion de problemas pero puede ser costosa en terminos de almacenamiento y complejidad.

**Pregunta**: Se necesita toda la profundidad de la jerarquia, o es suficiente con un subconjunto?

- [ ] Opcion A: Se necesita la jerarquia completa para permitir drill-down hasta el knowledge
- [ ] Opcion B: Es suficiente con Nivel -> Topic (sin trimestres ni knowledges individuales)
- [ ] Opcion C: La jerarquia completa es necesaria pero los detalles de knowledge pueden ser opcionales (lazy loading)
