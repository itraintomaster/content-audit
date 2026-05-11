---
feature:
  id: FEAT-SLEN
  code: F-SLEN
  name: Analisis de Longitud de Oraciones por Nivel CEFR
  priority: critical
---

# Analisis de Longitud de Oraciones por Nivel CEFR

Evaluar si la longitud de las oraciones a lo largo del curso es apropiada para cada nivel de dificultad esperado (CEFR), generando puntuaciones por quiz, estadisticas por nivel, recomendaciones y un analisis de progresion entre niveles. Las puntuaciones individuales por quiz se agregan a traves de la jerarquia del curso (knowledge, topic, nivel, curso) mediante el motor de agregacion generico de la plataforma ContentAudit.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es pedagogicamente adecuado. Una de las dimensiones clave es la **longitud de las oraciones**: en la ensenanza de idiomas, las oraciones deben ser mas cortas en niveles iniciales (A1) y progresivamente mas largas en niveles avanzados (B2). Esto responde a que los estudiantes principiantes necesitan estructuras simples y breves para construir confianza, mientras que los estudiantes avanzados deben enfrentarse a oraciones mas complejas para desarrollar fluidez.

### Jerarquia del curso

El curso tiene una estructura jerarquica fija (definida en FEAT-COURSE): **Curso -> Nivel (Milestone) -> Topic -> Knowledge -> Quiz**. Los **quizzes** son los que contienen las oraciones evaluables, no los knowledges directamente. Cada knowledge agrupa un conjunto de quizzes que ejercitan un mismo concepto, cada topic agrupa knowledges relacionados tematicamente, y cada nivel (milestone) corresponde a un nivel CEFR (A1, A2, B1, B2).

### Separacion entre analisis y agregacion

Esta funcionalidad involucra dos fases claramente diferenciadas:

1. **Fase de analisis (especifica de esta funcionalidad)**: El analizador de longitud de oraciones evalua cada quiz individualmente, produciendo una puntuacion de 0.0 a 1.0 que refleja la adecuacion de la longitud al nivel CEFR. El analizador opera a nivel de **quiz** -- es su unidad natural de evaluacion. También se encarga de excluir los quizzes que no son oraciones.

2. **Fase de agregacion (generica de la plataforma)**: Las puntuaciones individuales por quiz son agregadas a traves de la jerarquia del curso por el motor de agregacion de ContentAudit. Este motor construye un arbol de resultados (Curso -> Nivel -> Topic -> Knowledge -> Quiz) y calcula promedios ascendentes. Esta agregacion es identica para todos los analizadores del sistema, no es exclusiva de la longitud de oraciones.

3. **Fase de analisis de nivel (especifica de esta funcionalidad)**: Ademas de las puntuaciones agregadas, esta funcionalidad requiere estadisticas y evaluaciones especificas a nivel CEFR que van mas alla de la simple agregacion de puntuaciones: promedio de longitud en tokens, estado de cobertura por nivel, evaluacion de progresion entre niveles y recomendaciones. Estas metricas son propias de este analisis y no las proporciona el motor de agregacion generico.

La cadena completa desde la perspectiva del usuario es: el analizador puntua cada quiz -> la plataforma agrega esas puntuaciones a traves de la jerarquia -> el analisis de nivel produce estadisticas y evaluaciones especificas de longitud.

### Oraciones: los quizzes contienen las oraciones

Es importante aclarar que las "oraciones" que este analisis evalua provienen de los **quizzes** (plantillas de ejercicios). Cada quiz tiene una estructura de oracion compuesta por partes (texto y huecos a completar). La oracion evaluable es la reconstruccion del texto completo del quiz. Un knowledge no contiene oraciones directamente; las obtiene a traves de sus quizzes.

### Ejercicios que no son oraciones

No todos los quizzes del curso son oraciones evaluables. Algunos quizzes contienen etiquetas, listas de vocabulario, instrucciones u otro contenido que no constituye una oracion propiamente dicha. Estos quizzes estan marcados con un indicador que senala que no son oraciones. El analizador debe excluirlos del calculo para evitar distorsionar los resultados. Las exclusiones quedan reflejadas implicitamente en la cantidad de quizzes sin puntuacion en cada nodo del arbol de resultados.

### Rangos objetivo por nivel

La evaluacion se basa en rangos de longitud configurables para cada nivel. Los rangos actuales reflejan expectativas para un curso de ingles para adultos:

| Nivel | Minimo de palabras | Maximo de palabras |
|-------|--------------------|--------------------|
| A1    | 5                  | 8                  |
| A2    | 8                  | 11                 |
| B1    | 11                 | 14                 |
| B2    | 14                 | 17                 |

Estos rangos son configurables y pueden ajustarse para otros tipos de cursos (por ejemplo, cursos para ninos o cursos especializados). Los rangos se definen por nivel CEFR como un par minimo-maximo de cantidad de palabras por oracion. El rango de un nivel se aplica a todos los quizzes que pertenecen a ese nivel, independientemente del topic o knowledge intermedio.

### Volumenes esperados

El curso actual contiene aproximadamente 608 knowledges distribuidos en 4 niveles (A1, A2, B1, B2), con un promedio de aproximadamente 19 quizzes por knowledge (alrededor de 11.500 quizzes en total). El analisis debe procesar todos los quizzes validos del curso.

### Analisis de nivel y progresion: fuera de alcance MVP

Una version previa de esta funcionalidad contemplaba un conjunto de **estadisticas y evaluaciones por nivel CEFR** que iban mas alla de la simple agregacion de puntuaciones: promedio de longitud en tokens por nivel, estado de cobertura (OPTIMO / DEFICIENTE / EXCESIVO / NO APLICA) comparando ese promedio contra los rangos objetivo, evaluacion de progresion entre niveles consecutivos (POSITIVA / REGRESIVA / ESTANCADA / DATOS INSUFICIENTES), recomendaciones textuales por nivel, y registro consolidado de estas estadisticas en el resultado del analisis.

Este bloque de funcionalidad queda **fuera de alcance** para esta version del feature. Las razones:

- El analizador y sus consumidores conocidos (FEAT-RCLALEN, FEAT-LAGEN, refinamiento de longitud de oraciones) trabajan sobre el diagnostico **por quiz** que produce el analizador (`SentenceLengthDiagnosis`: tokenCount, targetMin, targetMax, cefrLevel, delta, toleranceMargin). Ningun consumidor aguas abajo solicita stats agregadas por nivel CEFR.
- No existe en el sistema un componente que implemente el "procesamiento posterior" requerido para producir estas stats. Anadirlo seria un trabajo arquitectonico significativo motivado por reglas que no tienen demanda real concreta.
- El usuario que necesita identificar problemas en un nivel especifico ya puede hacerlo a traves de las puntuaciones agregadas por la plataforma (R003-R005-R008-R016) y los diagnosticos por quiz: navegando la jerarquia hasta los nodos con puntuacion baja y consultando el `SentenceLengthDiagnosis` de los quizzes problematicos.

Si en una iteracion posterior aparece un caso de uso concreto que requiera stats agregadas por nivel (por ejemplo, un panel de control que muestre cobertura de longitud por nivel CEFR, o un analizador de progresion pedagogica), esta funcionalidad puede ser disenada como un feature separado, identificando explicitamente el componente que la produce y los consumidores que la requieren. Las reglas R006, R007, R010, R011, R014 y R015 de versiones anteriores de este documento, asi como los journeys J002 y J003, describian este alcance y pueden servir como punto de partida para esa futura iteracion.

---

## Reglas de Negocio

Las reglas se organizan en dos grupos segun la fase a la que pertenecen:

- **Grupo A - Analisis por quiz (R001, R002, R009, R012, R013)**: reglas propias del analizador de longitud de oraciones, que opera a nivel de quiz individual.
- **Grupo B - Agregacion de puntuaciones (R003, R004, R005, R008, R016)**: reglas que describen como las puntuaciones individuales se agregan a traves de la jerarquia. Estas reglas son cumplidas por el motor de agregacion generico de la plataforma ContentAudit y aplican de manera identica a todos los analizadores del sistema. Se documentan aqui porque son esenciales para que el usuario entienda el resultado completo de esta funcionalidad.

> **Nota sobre numeracion**: R006, R007, R010, R011, R014 y R015 fueron retirados como reglas numeradas. Describian estadisticas y evaluaciones por nivel CEFR (promedio de longitud por nivel, estado OPTIMO/DEFICIENTE/EXCESIVO/NO APLICA, progresion entre niveles, recomendaciones, registro consolidado) que dependian de un "procesamiento posterior" que no existe en el sistema y no tiene demanda concreta de ningun consumidor aguas abajo. Su contenido se documenta en "Analisis de nivel y progresion: fuera de alcance MVP" del Contexto. Los IDs R006, R007, R010, R011, R014 y R015 quedan retirados; los demas mantienen su numeracion para no romper trazabilidad con commits historicos.

---

### Grupo A - Analisis por quiz

### Rule[F-SLEN-R001] - Exclusion de quizzes que no son oraciones
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los quizzes marcados como "no oracion" deben excluirse del analisis de longitud. El analizador simplemente no produce puntuacion para estos quizzes. Al no tener puntuacion, no participan en ninguna agregacion posterior.

Un knowledge cuyos quizzes son todos "no oracion" queda sin puntuacion de longitud (ningun quiz aporto puntuacion). Analogamente, un topic cuyos knowledges no tienen puntuacion queda tambien sin puntuacion. Este comportamiento es una consecuencia natural de la exclusion: al no haber puntuaciones de quiz que agregar, los niveles superiores tampoco tienen datos.

**Error**: "Se incluyo un quiz no-oracion en el calculo de longitud del knowledge {knowledgeId}"

### Rule[F-SLEN-R002] - Puntuacion por quiz (oracion individual)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada quiz que es una oracion valida recibe una puntuacion entre 0.0 y 1.0 que refleja que tan adecuada es su longitud para el nivel CEFR al que pertenece. Esta es la unidad atomica del analisis; todas las agregaciones superiores se derivan de estas puntuaciones.

- Si la longitud esta dentro del rango objetivo [minimo, maximo] del nivel, la puntuacion es 1.0 (perfecta).
- Si la longitud esta fuera del rango, la puntuacion disminuye linealmente segun la distancia al borde mas cercano del rango.
- La distancia se mide en tokens hacia el borde mas cercano (minimo o maximo).
- Existe un margen de tolerancia de 4 tokens: a 4 o mas tokens de distancia del rango, la puntuacion es 0.0.
- Dentro del margen, la puntuacion se calcula como: 1.0 menos (distancia dividida por 4.0).

Ejemplo para el nivel A1 (rango 5-8 palabras):

| Longitud | 1   | 2    | 3   | 4    | 5   | 6   | 7   | 8   | 9    | 10  | 11   | 12  |
|----------|-----|------|-----|------|-----|-----|-----|-----|------|-----|------|-----|
| Puntuacion | 0.0 | 0.25 | 0.5 | 0.75 | 1.0 | 1.0 | 1.0 | 1.0 | 0.75 | 0.5 | 0.25 | 0.0 |

**Error**: "Puntuacion fuera de rango [0.0, 1.0] calculada para el quiz ({quizId}) en el knowledge {knowledgeId}, nivel {nivel}: {puntuacion}"

### Rule[F-SLEN-R009] - Margen de tolerancia fijo de 4 tokens
**Severity**: minor | **Validation**: ASSUMPTION

El margen de tolerancia utilizado en la puntuacion por quiz (R002) es de 4 tokens. Este valor esta fijo y no es configurable por el usuario. Un quiz cuya oracion esta exactamente 1 token fuera del rango recibe 0.75, a 2 tokens recibe 0.50, a 3 tokens recibe 0.25, y a 4 o mas tokens recibe 0.0.

[ASSUMPTION] El margen de 4 tokens es un valor razonable para cursos de ingles para adultos. Si en el futuro se requiere ajustar este valor para otros tipos de cursos, deberia considerarse hacerlo configurable. Se mantiene fijo por simplicidad, alineado con el comportamiento actual del sistema.

**Error**: N/A (esta regla define un parametro, no una condicion de error)

### Rule[F-SLEN-R012] - Rangos objetivo configurables por nivel
**Severity**: major | **Validation**: AUTO_VALIDATED

Los rangos objetivo de longitud (minimo y maximo de palabras por oracion) se definen por nivel CEFR a traves de configuracion externa. El analizador utiliza estos rangos para calcular la puntuacion de cada quiz (R002). Si un nivel no tiene rango configurado, el analizador no puede puntuar los quizzes de ese nivel, y en consecuencia se le asigna el estado NO APLICA y no participa en la puntuacion general.

Los rangos actuales son:
- A1: 5 a 8 palabras
- A2: 8 a 11 palabras
- B1: 11 a 14 palabras
- B2: 14 a 17 palabras

**Error**: "Configuracion de rangos invalida para el nivel {nivel}: el minimo ({min}) debe ser menor o igual al maximo ({max})"

### Rule[F-SLEN-R013] - La longitud se mide en tokens linguisticos
**Severity**: critical | **Validation**: AUTO_VALIDATED

La longitud de la oracion de un quiz se mide en tokens linguisticos provenientes de un procesamiento de lenguaje natural previo, no en palabras separadas por espacios. Esto es fundamental porque la segmentacion linguistica puede diferir del conteo por espacios (por ejemplo, contracciones, posesivos, o guiones pueden segmentarse de forma distinta). Utilizar un metodo de conteo diferente produciria promedios incorrectos y evaluaciones erroneas.

**Error**: "La longitud de la oracion no se obtuvo de tokens linguisticos: fuente inesperada de datos"

---

### Grupo B - Agregacion de puntuaciones (provista por la plataforma)

> **Nota**: Las reglas de este grupo describen el comportamiento esperado del motor de agregacion generico de ContentAudit. No son implementadas por el analizador de longitud de oraciones, sino por la plataforma. Se documentan aqui porque el usuario percibe el resultado final como parte de esta funcionalidad y necesita entender como se derivan las puntuaciones en cada nivel de la jerarquia. Estas mismas reglas de agregacion aplican de manera identica a cualquier otro analizador del sistema.

### Rule[F-SLEN-R003] - Puntuacion por knowledge (agregacion de la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de longitud de un knowledge es el **promedio de las puntuaciones de sus quizzes** que tienen puntuacion (segun R002). Los quizzes excluidos por R001 (que no son oraciones) no tienen puntuacion y por lo tanto no participan en el promedio. Si un knowledge no tiene quizzes con puntuacion, su puntuacion no esta disponible y no participa en la agregacion del topic padre.

**Error**: "Error al calcular la puntuacion del knowledge {knowledgeId}: {detalle}"

### Rule[F-SLEN-R004] - Puntuacion por topic (agregacion de la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de longitud de un topic es el **promedio de las puntuaciones de sus knowledges** que tienen puntuacion disponible (segun R003). Los knowledges sin puntuacion (porque no tienen quizzes con puntuacion) no participan en el promedio. Si ningun knowledge del topic tiene puntuacion, la puntuacion del topic no esta disponible y no participa en la agregacion del nivel padre.

**Error**: "Error al calcular la puntuacion del topic {topicId}: {detalle}"

### Rule[F-SLEN-R005] - Puntuacion por nivel (agregacion de la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de longitud de un nivel es el **promedio de las puntuaciones de sus topics** que tienen puntuacion disponible (segun R004). Los topics sin puntuacion no participan en el promedio. Si ningun topic del nivel tiene puntuacion, la puntuacion del nivel no esta disponible.

**Error**: "Error al calcular la puntuacion del nivel {nivel}: {detalle}"

### Rule[F-SLEN-R008] - Puntuacion general del curso (agregacion de la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion general del analisis de longitud de oraciones es el **promedio de las puntuaciones de los niveles** CEFR que tienen puntuacion disponible (segun R005). Los niveles sin puntuacion no participan en el calculo. Si ningun nivel tiene puntuacion, la puntuacion general es cero.

La cadena completa de agregacion es: quiz (R002) -> knowledge (R003) -> topic (R004) -> nivel (R005) -> curso (R008).

**Error**: "Error al calcular la puntuacion general: {detalle}"

### Rule[F-SLEN-R016] - Puntuaciones disponibles en cada nivel de la jerarquia (agregacion de la plataforma)
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado del analisis debe hacer disponible la puntuacion de longitud en cada nivel de la jerarquia del curso. La plataforma construye un arbol de resultados donde cada nodo (quiz, knowledge, topic, nivel, curso) tiene asociada la puntuacion de cada analizador ejecutado. Esto significa que el usuario puede consultar:

- La puntuacion de un **quiz** individual (R002)
- La puntuacion de un **knowledge** (promedio de sus quizzes, R003)
- La puntuacion de un **topic** (promedio de sus knowledges, R004)
- La puntuacion de un **nivel** (promedio de sus topics, R005)
- La puntuacion del **curso** (promedio de sus niveles, R008)

Esto permite al creador de contenido navegar la jerarquia y localizar exactamente donde se concentran los problemas de longitud de oraciones: en un topic especifico, en un knowledge particular, o en quizzes individuales.

**Error**: N/A (esta regla describe la disponibilidad de datos, no una condicion de error)

---

## User Journeys

### Journey[F-SLEN-J001] - Auditar la longitud de oraciones de un curso completo
**Validation**: AUTO_VALIDATED

1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
2. El sistema recorre la jerarquia del curso de arriba hacia abajo: para cada nivel (milestone), sus topics, sus knowledges, y sus quizzes
3. Para cada quiz, el analizador de longitud determina si es una oracion valida; los quizzes que no son oraciones se excluyen del analisis (R001)
4. Para cada quiz valido, el analizador cuenta los tokens linguisticos de su oracion (R013) y calcula su puntuacion individual comparando la longitud contra el rango objetivo de su nivel CEFR (R002)
5. La plataforma agrega las puntuaciones de quizzes hacia arriba a traves de la jerarquia: para cada knowledge calcula el promedio de sus quizzes (R003), para cada topic el promedio de sus knowledges (R004), para cada nivel el promedio de sus topics (R005), y para el curso el promedio de sus niveles (R008)
6. El usuario recibe un informe con la puntuacion general y las puntuaciones disponibles en cada nivel de la jerarquia (R016), pudiendo profundizar en topics, knowledges y quizzes individuales para localizar donde se concentran los problemas de longitud

### Journey[F-SLEN-J004] - Navegar la jerarquia para localizar problemas de longitud
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de longitud de oraciones (J001)
2. El usuario observa que un nivel (por ejemplo, B1) tiene una puntuacion baja
3. El usuario profundiza en los topics del nivel B1 y encuentra que el topic "Modal Verbs" tiene la puntuacion mas baja
4. El usuario profundiza en los knowledges de "Modal Verbs" y encuentra que el knowledge "Should for advice" tiene puntuacion 0.4
5. El usuario revisa los quizzes de "Should for advice" y encuentra que varias oraciones tienen 18 tokens (demasiado largas para B1, rango 11-14)
6. El usuario identifica exactamente que oraciones necesitan simplificarse y puede tomar acciones correctivas puntuales

### Journey[F-SLEN-J005] - Ajustar rangos objetivo para un curso distinto
**Validation**: AUTO_VALIDATED

1. El usuario tiene un curso con caracteristicas diferentes al curso estandar (por ejemplo, un curso para ninos o un curso intensivo)
2. El usuario modifica los rangos objetivo de longitud por nivel en la configuracion del sistema
3. El usuario ejecuta la auditoria con los nuevos rangos
4. Los resultados reflejan los rangos actualizados: las puntuaciones de quizzes se recalculan con los nuevos valores (R002), y la plataforma reagrega las puntuaciones a traves de la jerarquia (R003-R005-R008)
5. El usuario valida que los rangos ajustados se alinean con las expectativas pedagogicas del nuevo curso navegando la jerarquia (R016) y observando como cambian las puntuaciones por nivel

---

## Open Questions

### Doubt[DOUBT-MARGIN-CONFIG] - Deberia el margen de tolerancia de 4 tokens ser configurable?
**Status**: OPEN

Actualmente el margen de tolerancia para la puntuacion por quiz (4 tokens) esta definido como un valor fijo. Esto significa que el usuario no puede ajustar la sensibilidad del scoring sin modificar el sistema.

**Pregunta**: Se necesita que este valor sea configurable por el usuario, o el valor fijo de 4 tokens es adecuado para todos los cursos?

- [ ] Opcion A: Mantener fijo en 4 tokens (simplicidad, alineado con el sistema actual)
- [x] Opcion B: Hacerlo configurable en la configuracion del sistema junto con los rangos

### Doubt[DOUBT-PROGRESSION-GAPS] - Como tratar la progresion cuando faltan niveles intermedios?
**Status**: RESOLVED (ya no aplica)

Esta pregunta dependia de la existencia de una evaluacion de progresion entre niveles, funcionalidad retirada en esta version (ver "Analisis de nivel y progresion: fuera de alcance MVP" en Contexto). Si la evaluacion de progresion se incorpora en una iteracion futura, esta pregunta debe re-abrirse junto con su diseno.

### Doubt[DOUBT-EQUAL-AVERAGES] - Que estado de progresion corresponde cuando dos niveles consecutivos tienen el mismo promedio?
**Status**: RESOLVED (ya no aplica)

Esta pregunta dependia de la existencia de una evaluacion de progresion entre niveles, funcionalidad retirada en esta version (ver "Analisis de nivel y progresion: fuera de alcance MVP" en Contexto). La respuesta tentativa (umbral del 50% del avance esperado entre niveles consecutivos calculado sobre los puntos medios de los rangos objetivo) se preserva aqui como referencia para una futura iteracion que reintroduzca la evaluacion de progresion.

Respuesta tentativa preservada: el umbral de crecimiento se calcularia como el 50% del avance esperado entre niveles consecutivos. El avance esperado se calcula como la diferencia entre los puntos medios de los rangos objetivo de cada nivel. Por ejemplo, si A1 tiene rango [5, 8] (punto medio 6.5) y A2 tiene rango [8, 11] (punto medio 9.5), el avance esperado es 3.0 y el umbral minimo de crecimiento seria 1.5. El umbral seria independiente del cumplimiento del rango objetivo: un nivel podria mostrar "crecimiento" respecto al anterior pero aun asi estar fuera de su rango objetivo.

### Doubt[DOUBT-AGGREGATION-METHOD] - La agregacion intermedia debe ser promedio simple o promedio ponderado?
**Status**: OPEN

La cadena de agregacion (quiz -> knowledge -> topic -> nivel -> curso) usa promedio simple en cada paso. Esto significa que un knowledge con 5 quizzes tiene el mismo peso que un knowledge con 30 quizzes al calcular la puntuacion del topic.

**Pregunta**: Es adecuado el promedio simple (cada entidad hija tiene el mismo peso), o deberia usarse un promedio ponderado por cantidad de quizzes validos?

- [ ] Opcion A: Promedio simple en cada nivel de agregacion (cada knowledge pesa igual, cada topic pesa igual)
- [ ] Opcion B: Promedio ponderado por cantidad de quizzes validos (un knowledge con mas quizzes tiene mas influencia)

[ASSUMPTION] Se usa promedio simple en cada nivel de agregacion porque refleja la importancia pedagogica de cada unidad de conocimiento por igual, independientemente de cuantos ejercicios tenga. Un knowledge con 5 quizzes es tan importante como uno con 30 quizzes en terminos de adecuacion del contenido. Si el product owner prefiere que knowledges con mas ejercicios tengan mas peso, deberia seleccionarse la opcion B.

### Doubt[DOUBT-LEVEL-STATS-LOCATION] - Donde residen las estadisticas especificas de longitud por nivel?
**Status**: RESOLVED (decisor: retiro del alcance MVP)

Esta pregunta se mantuvo OPEN durante varias iteraciones sin que aparezca un consumidor concreto que requiera estadisticas agregadas por nivel CEFR (promedio de longitud por nivel, estado de cobertura, progresion entre niveles, recomendaciones). Los consumidores aguas abajo conocidos (FEAT-RCLALEN, FEAT-LAGEN, refinamiento de longitud de oraciones) trabajan sobre el diagnostico por quiz que produce el analizador (`SentenceLengthDiagnosis`), no sobre stats agregadas.

**Resolucion**: Se retiran las stats por nivel del alcance MVP del feature. La pregunta de "donde residen" deja de aplicar porque la funcionalidad misma se retiro. Si en una iteracion posterior aparece un caso de uso concreto que requiera estas stats, esta pregunta debe re-abrirse junto con el diseno del componente que las produce. Ver "Analisis de nivel y progresion: fuera de alcance MVP" en Contexto para los detalles del retiro.
