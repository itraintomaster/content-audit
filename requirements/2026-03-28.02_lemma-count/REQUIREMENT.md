---
feature:
  id: FEAT-LCOUNT
  code: F-LCOUNT
  name: Analisis de Conteo de Apariciones de Lemas EVP
  priority: critical
---

# Analisis de Conteo de Apariciones de Lemas EVP

Evaluar si cada lema esperado del catalogo EVP (English Vocabulary Profile) aparece un numero adecuado de veces a lo largo del curso, clasificando cada lema segun su nivel de exposicion (ausente, sub-expuesto, normal, sobre-expuesto) y generando puntuaciones que reflejan la adecuacion de la frecuencia de aparicion. Las puntuaciones individuales por lema se agregan a traves de la jerarquia del curso mediante el motor de agregacion generico de la plataforma ContentAudit, permitiendo localizar donde se concentran los problemas de exposicion de vocabulario.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es pedagogicamente adecuado. Una de las dimensiones clave es la **frecuencia de aparicion del vocabulario objetivo**: en la ensenanza de idiomas, cada palabra que el estudiante debe aprender necesita aparecer un numero minimo de veces en el material del curso para que la adquisicion sea efectiva. Si una palabra aparece muy pocas veces, el estudiante no tiene suficiente exposicion para retenerla. Si aparece demasiadas veces, puede saturar el material sin aportar valor pedagogico adicional.

### Premisa pedagogica

La investigacion en adquisicion de vocabulario establece que un estudiante necesita encontrar una palabra nueva un numero minimo de veces en contexto para incorporarla a su vocabulario activo. Este analizador mide exactamente eso: cuantas veces aparece cada lema objetivo en el curso completo, y si ese numero de apariciones cae dentro del rango adecuado para el aprendizaje.

### Catalogo EVP como referencia

El English Vocabulary Profile (EVP) es un catalogo de referencia que define que palabras se esperan en cada nivel CEFR (A1, A2, B1, B2). Este catalogo es la fuente de verdad para determinar cuales son los "lemas esperados" en cada nivel. Solo se evaluan los lemas que el EVP indica como propios de cada nivel; las demas palabras que aparecen en el curso no participan en este analisis.

El EVP contiene aproximadamente 784 lemas para A1, 1.594 para A2, 2.937 para B1 y 4.164 para B2. Cada lema tiene asociado un nivel CEFR y una parte de la oracion (sustantivo, verbo, adjetivo, etc.).

### Relacion con otros analizadores de vocabulario

Este analizador es **complementario** a otros analizadores de vocabulario del sistema:

| Analizador | Que mide | Alcance | Estado |
|------------|----------|---------|--------|
| LemmaCount (este) | Cuantas veces aparece cada lema | Solo lemas del EVP | Reactivacion (era INACTIVO) |
| LemmaRecurrence | A que intervalos se repite cada lema | Top 2000 lemas por frecuencia | ACTIVO |
| LemmaAbsence | Que lemas esperados no aparecen | Solo lemas del EVP | ACTIVO |

LemmaCount y LemmaRecurrence miden aspectos diferentes de la exposicion al vocabulario: LemmaCount responde "cuantas veces aparece esta palabra?" mientras que LemmaRecurrence responde "cada cuanto aparece esta palabra?". Un lema puede aparecer 10 veces (buen conteo) pero todas concentradas en un solo topic (mala recurrencia), o puede aparecer 3 veces (conteo bajo) pero distribuidas a lo largo de todo el curso (buena recurrencia). Ambas perspectivas son necesarias para una evaluacion completa.

LemmaCount y LemmaAbsence comparten la misma fuente de referencia (el catalogo EVP) pero miden cosas distintas: LemmaAbsence detecta lemas que no aparecen en absoluto o que aparecen en el nivel incorrecto, mientras que LemmaCount evalua si los lemas que si estan presentes aparecen un numero suficiente de veces.

### Reactivacion de un analizador existente

Este analizador existia en el sistema original pero fue desactivado (comentado en el codigo fuente). Las razones de la desactivacion no estan documentadas, pero posiblemente se debio a problemas de rendimiento o a que se considero redundante con LemmaRecurrence. En la migracion a ContentAudit, se reactiva como analizador estandar para que sus resultados participen en el informe de auditoria y se beneficien de la agregacion generica de la plataforma.

La reactivacion implica adaptar el analizador al modelo estandar de la plataforma: producir puntuaciones por oracion (la unidad de evaluacion mas granular) que luego se agregan a traves de la jerarquia del curso.

### Jerarquia del curso

El curso tiene una estructura jerarquica fija: **Curso -> Nivel (Milestone) -> Topic -> Knowledge -> Quiz**. Los **quizzes** contienen las oraciones evaluables, y cada oracion esta compuesta por tokens procesados linguisticamente (lemas). El analizador debe:

1. Contar las apariciones de cada lema esperado (EVP) a lo largo de todas las oraciones del curso
2. Clasificar y puntuar cada lema individualmente
3. Puntuar cada oracion en funcion de los lemas esperados que contiene
4. Permitir que la plataforma agregue las puntuaciones por oracion a traves de la jerarquia

### Separacion entre analisis y agregacion

Al igual que en otros analizadores del sistema:

1. **Fase de analisis global (especifica de esta funcionalidad)**: El analizador recorre todas las oraciones del curso para contar las apariciones de cada lema esperado del EVP. Luego clasifica cada lema segun su nivel de exposicion y le asigna una puntuacion individual. Este procesamiento opera a nivel del curso completo porque necesita el conteo total de apariciones antes de poder evaluar.

2. **Fase de puntuacion por oracion (especifica de esta funcionalidad)**: Una vez que cada lema tiene su puntuacion individual, el analizador asigna una puntuacion a cada oracion (quiz) como el promedio de las puntuaciones de los lemas esperados que aparecen en esa oracion. Esta es la puntuacion que la plataforma utilizara para la agregacion.

3. **Fase de agregacion (generica de la plataforma)**: Las puntuaciones individuales por quiz son agregadas a traves de la jerarquia del curso por el motor de agregacion de ContentAudit. Este motor construye un arbol de resultados y calcula promedios ascendentes. Esta agregacion es identica para todos los analizadores del sistema.

### Datos de entrada

El analizador necesita dos fuentes de datos:

- **Tokens lematizados de las oraciones**: Cada oracion del curso ha sido procesada por un motor de lenguaje natural que produce tokens con su forma lematizada (forma base de la palabra) y su parte de la oracion. El analizador utiliza estos lemas, no las formas flexionadas originales.
- **Catalogo EVP de lemas esperados por nivel**: La lista de lemas que se esperan en cada nivel CEFR, con su parte de la oracion asociada. Esto permite saber, para cada nivel, cuales son los lemas "objetivo" cuya frecuencia de aparicion se debe evaluar.

### Volumenes esperados

El curso actual contiene aproximadamente 608 knowledges distribuidos en 4 niveles (A1, A2, B1, B2), con un promedio de aproximadamente 19 quizzes por knowledge (alrededor de 11.500 quizzes en total). El EVP contiene aproximadamente 9.479 lemas para los niveles A1-B2 combinados. El analizador debe contar las apariciones de cada uno de estos lemas a lo largo de todas las oraciones del curso.

---

## Reglas de Negocio

Las reglas se organizan en cuatro grupos segun la fase y el tema al que pertenecen:

- **Grupo A - Conteo y clasificacion de lemas (R001-R005)**: reglas que describen como se cuentan las apariciones de lemas esperados y como se clasifican segun su nivel de exposicion.
- **Grupo B - Puntuacion por lema y por oracion (R006-R010)**: reglas que describen como se calculan las puntuaciones individuales de cada lema y como se derivan las puntuaciones por oracion.
- **Grupo C - Agregacion de puntuaciones (R011)**: referencia a la agregacion generica de la plataforma.
- **Grupo D - Identificacion y configuracion (R012-R013)**: reglas que definen la identidad del analizador y la configurabilidad de los parametros.

> **Nota sobre agregacion**: Las reglas de agregacion de puntuaciones a traves de la jerarquia (quiz -> knowledge -> topic -> nivel -> curso) son provistas por la plataforma ContentAudit y estan documentadas en FEAT-SLEN (R003-R005, R008, R016). Aplican de manera identica a este analizador y no se repiten aqui en detalle.

---

### Grupo A - Conteo y clasificacion de lemas

### Rule[F-LCOUNT-R001] - Conteo de apariciones por lema esperado
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR, el analizador obtiene la lista de lemas esperados del catalogo EVP y cuenta cuantas veces aparece cada uno de esos lemas en el conjunto completo de oraciones del curso. El conteo se realiza sobre la forma lematizada de los tokens (forma base), no sobre la forma flexionada que aparece en la oracion.

Dos tokens diferentes que se lematizan a la misma forma base se cuentan como apariciones del mismo lema. Por ejemplo, "running", "ran" y "runs" se lematizan a "run", y cada una cuenta como una aparicion de "run".

La identificacion de un lema se basa en la combinacion de su forma lematizada y su parte de la oracion. Es decir, "run" como verbo y "run" como sustantivo se consideran lemas distintos. Si el EVP espera "run" como verbo en A1, solo se cuentan las apariciones de "run" que fueron etiquetadas como verbo por el procesamiento linguistico.

**Error**: "Error al contar apariciones del lema {lema} ({parteOracion}) en el nivel {nivel}: {detalle}"

### Rule[F-LCOUNT-R002] - El conteo abarca todo el curso, no solo el nivel del lema
**Severity**: critical | **Validation**: AUTO_VALIDATED

El conteo de apariciones de un lema esperado se realiza sobre **todas las oraciones del curso completo**, no solo sobre las oraciones del nivel CEFR donde el EVP lo clasifica. Si el EVP indica que "cat" es un lema de nivel A1, se cuentan todas las apariciones de "cat" en A1, A2, B1 y B2.

Esto responde a que el aprendizaje de vocabulario es acumulativo: un estudiante que aprendio "cat" en A1 seguira encontrandolo en niveles posteriores, y esas apariciones refuerzan el aprendizaje. Contar solo las apariciones dentro del nivel asignado subestimaria la exposicion real del estudiante al lema.

**Error**: N/A (esta regla describe un criterio de conteo, no una condicion de error)

### Rule[F-LCOUNT-R003] - Clasificacion de lemas por nivel de exposicion
**Severity**: critical | **Validation**: AUTO_VALIDATED

Una vez contadas las apariciones de cada lema esperado, se clasifica en uno de cuatro estados de exposicion segun la cantidad de apariciones y los umbrales configurados:

| Estado | Condicion | Significado pedagogico |
|--------|-----------|----------------------|
| Ausente | apariciones == 0 | El lema no aparece en ningun lugar del curso; el estudiante no tiene exposicion alguna |
| Sub-expuesto | 0 < apariciones < umbralMinimo | El lema aparece pero no las veces suficientes para que el estudiante lo retenga |
| Normal | umbralMinimo <= apariciones <= umbralMaximo | El lema aparece un numero adecuado de veces para el aprendizaje |
| Sobre-expuesto | apariciones > umbralMaximo | El lema aparece demasiadas veces, lo que puede saturar el material sin beneficio pedagogico adicional |

Los umbrales por defecto son:
- **umbralMinimo (subExposed)**: 4 apariciones
- **umbralMaximo (overExposed)**: 15 apariciones

Un lema ausente es el caso mas critico porque indica una laguna total de exposicion. Un lema sub-expuesto indica exposicion insuficiente. Un lema normal tiene exposicion adecuada. Un lema sobre-expuesto no es necesariamente un problema grave, pero indica que el espacio del curso podria aprovecharse mejor con otras palabras.

**Error**: "Estado de exposicion indeterminado para el lema {lema} ({parteOracion}): apariciones = {count}, umbrales = [{min}, {max}]"

### Rule[F-LCOUNT-R004] - Los lemas esperados se obtienen del catalogo EVP
**Severity**: critical | **Validation**: AUTO_VALIDATED

La lista de lemas esperados por nivel se obtiene del catalogo EVP (English Vocabulary Profile). Solo los lemas listados en el EVP para cada nivel participan en el analisis; las demas palabras que aparecen en el curso no se evaluan. El catalogo proporciona para cada lema su forma base, la parte de la oracion y el nivel CEFR asignado.

Se excluyen del analisis las entradas del EVP que son frases (expresiones de multiples palabras), ya que el conteo opera a nivel de tokens individuales. Solo se consideran lemas de una sola palabra.

**Error**: "No se encontraron lemas esperados en el catalogo EVP para el nivel {nivel}"

### Rule[F-LCOUNT-R005] - Los datos de lematizacion provienen de procesamiento linguistico previo
**Severity**: critical | **Validation**: AUTO_VALIDATED

La forma lematizada de cada token y su parte de la oracion provienen de un procesamiento linguistico previo. El analizador utiliza los lemas ya asociados a cada token; no realiza lematizacion propia. Esto es fundamental porque la lematizacion es un proceso que depende del contexto de la oracion (por ejemplo, "saw" puede lematizarse a "see" o a "saw" segun el contexto) y debe realizarse de manera consistente con el resto de los analizadores del sistema.

**Error**: "Token sin datos de lematizacion en el quiz {quizId}: el procesamiento linguistico previo no asocio lema"

---

### Grupo B - Puntuacion por lema y por oracion

### Rule[F-LCOUNT-R006] - Puntuacion individual por lema
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema esperado del EVP recibe una puntuacion entre 0.0 y 1.0 que refleja la adecuacion de su frecuencia de aparicion. La puntuacion se calcula segun el estado de exposicion del lema:

| Estado | Formula de puntuacion | Ejemplo |
|--------|----------------------|---------|
| Ausente | 0.0 | Un lema que no aparece nunca recibe la peor puntuacion |
| Sub-expuesto | apariciones / umbralMinimo | Con umbralMinimo=4: 1 aparicion = 0.25, 2 apariciones = 0.50, 3 apariciones = 0.75 |
| Normal | 1.0 | Cualquier lema dentro del rango adecuado recibe la puntuacion perfecta |
| Sobre-expuesto | max(0.0, 1.0 - (apariciones - umbralMaximo) / 50) | Con umbralMaximo=15: 25 apariciones = 0.80, 40 apariciones = 0.50, 65 apariciones = 0.0 |

La puntuacion de lemas sub-expuestos crece linealmente desde 0.0 hasta casi 1.0 a medida que se acercan al umbral minimo. La puntuacion de lemas sobre-expuestos decrece linealmente desde 1.0 hacia 0.0 con un factor de degradacion de 1/50 por cada aparicion adicional mas alla del umbral maximo.

**Error**: "Puntuacion fuera de rango [0.0, 1.0] calculada para el lema {lema} ({parteOracion}): {puntuacion}"

### Rule[F-LCOUNT-R007] - Puntuacion por oracion (quiz)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada oracion (quiz) del curso recibe una puntuacion entre 0.0 y 1.0 que refleja la adecuacion de los lemas esperados que contiene. La puntuacion se calcula como el **promedio de las puntuaciones individuales** (R006) de los lemas esperados del EVP que aparecen en esa oracion.

Solo participan en el promedio los lemas que:
1. Aparecen en la oracion (estan presentes como tokens lematizados)
2. Son lemas esperados del EVP (estan listados en el catalogo para algun nivel)

Si una oracion no contiene ninguno de los lemas esperados del EVP, no recibe puntuacion de este analizador y no participa en la agregacion posterior.

Ejemplo: una oracion contiene los lemas "cat" (score 1.0, normal), "run" (score 0.50, sub-expuesto) y "the" (no es lema EVP, se ignora). La puntuacion de la oracion es (1.0 + 0.50) / 2 = 0.75.

**Error**: "Error al calcular la puntuacion de la oracion del quiz {quizId} en el knowledge {knowledgeId}: {detalle}"

### Rule[F-LCOUNT-R008] - Puntuacion general del analisis (overall score)
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion general del analisis de conteo de lemas es el **promedio de las puntuaciones individuales de todos los lemas esperados** del EVP para los niveles evaluados. Todos los lemas participan en este promedio, incluyendo los ausentes (que aportan puntuacion 0.0).

Esta puntuacion general refleja la cobertura y adecuacion de la exposicion al vocabulario objetivo en el curso completo. Un curso donde la mayoria de los lemas del EVP aparecen un numero adecuado de veces tendra una puntuacion cercana a 1.0; un curso donde muchos lemas estan ausentes o sub-expuestos tendra una puntuacion baja.

Ejemplo: si el EVP define 784 lemas para A1, y de esos 600 tienen puntuacion 1.0 (normal), 100 tienen puntuacion 0.5 (sub-expuestos), 50 tienen puntuacion 0.0 (ausentes) y 34 tienen puntuacion 0.8 (sobre-expuestos), la puntuacion general seria: (600 * 1.0 + 100 * 0.5 + 50 * 0.0 + 34 * 0.8) / 784 = (600 + 50 + 0 + 27.2) / 784 = 0.863.

**Error**: "Error al calcular la puntuacion general: {detalle}"

### Rule[F-LCOUNT-R009] - Exclusion de quizzes que no son oraciones
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los quizzes marcados como "no oracion" (etiquetas, listas de vocabulario, instrucciones u otro contenido no evaluable) deben excluirse del analisis. El analizador no cuenta los tokens de estos quizzes ni les asigna puntuacion. Este comportamiento es coherente con otros analizadores del sistema que operan sobre oraciones.

Sin embargo, si un lema esperado del EVP aparece exclusivamente en quizzes no-oracion, esas apariciones **no se contabilizan**. El lema se considera ausente o sub-expuesto segun sus apariciones en oraciones validas unicamente.

**Error**: "Se incluyo un quiz no-oracion en el conteo de lemas del knowledge {knowledgeId}"

### Rule[F-LCOUNT-R010] - Resultado detallado por lema
**Severity**: major | **Validation**: AUTO_VALIDATED

El resultado del analisis debe incluir, para cada lema esperado evaluado, la siguiente informacion:

- **Lema**: la forma base de la palabra
- **Parte de la oracion**: sustantivo, verbo, adjetivo, etc.
- **Nivel CEFR asignado**: el nivel donde el EVP clasifica el lema
- **Cantidad de apariciones**: cuantas veces se encontro el lema en el curso
- **Estado de exposicion**: ausente, sub-expuesto, normal o sobre-expuesto
- **Puntuacion individual**: la puntuacion calculada segun R006

Este detalle permite al creador de contenido identificar exactamente que lemas necesitan atencion: cuales estan ausentes, cuales necesitan mas apariciones, y cuales aparecen demasiado.

**Error**: N/A (esta regla describe la estructura del resultado, no una condicion de error)

---

### Grupo C - Agregacion de puntuaciones

### Rule[F-LCOUNT-R011] - Agregacion a traves de la jerarquia (provista por la plataforma)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Las puntuaciones por quiz (R007) se agregan a traves de la jerarquia del curso por el motor de agregacion generico de ContentAudit, de la misma forma que en otros analizadores:

- **Knowledge**: promedio de las puntuaciones de sus quizzes con puntuacion
- **Topic**: promedio de las puntuaciones de sus knowledges con puntuacion
- **Nivel (Milestone)**: promedio de las puntuaciones de sus topics con puntuacion
- **Curso**: promedio de las puntuaciones de sus niveles con puntuacion

Los quizzes sin puntuacion (excluidos por R009 o que no contienen lemas EVP) no participan en el promedio. Un knowledge cuyos quizzes no tienen puntuacion queda sin puntuacion y no participa en la agregacion del topic padre.

Esto permite al usuario navegar la jerarquia y localizar donde se concentran los problemas: en un nivel especifico, en un topic particular, o en knowledges individuales.

**Error**: N/A (la agregacion es provista por la plataforma; ver FEAT-SLEN R003-R005, R008, R016 para los detalles)

---

### Grupo D - Identificacion y configuracion

### Rule[F-LCOUNT-R012] - Nombre del analizador en el informe
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador se identifica con el nombre **"lemma-count"** en el informe de auditoria. Este nombre aparece en las puntuaciones por nodo de la jerarquia y permite al usuario distinguir los resultados de este analisis de los de otros analizadores (como "lemma-recurrence" o "sentence-length").

**Error**: N/A (esta regla define un nombre de identificacion, no una condicion de error)

### Rule[F-LCOUNT-R013] - Los umbrales de exposicion no son configurables en esta version
**Severity**: minor | **Validation**: ASSUMPTION

Los umbrales de exposicion (umbralMinimo = 4, umbralMaximo = 15) y el factor de degradacion por sobre-exposicion (1/50) estan fijos y no son configurables por el usuario en esta primera version. Provienen de la implementacion de referencia.

[ASSUMPTION] Se asume que los umbrales de la referencia son adecuados como punto de partida. Sin embargo, la seccion de Open Questions documenta preocupaciones sobre la adecuacion del umbral de sobre-exposicion (15) para lemas basicos de alta frecuencia. Si se determina que los umbrales necesitan ajuste, deberia considerarse hacerlos configurables por nivel CEFR. Se mantienen fijos por simplicidad, alineado con el alcance de esta primera version.

**Error**: N/A (esta regla define parametros, no una condicion de error)

---

## User Journeys

### Journey[F-LCOUNT-J001] - Auditar el conteo de apariciones de lemas EVP de un curso completo
**Validation**: AUTO_VALIDATED

1. El usuario inicia una auditoria de un curso previamente cargado en el sistema
2. El sistema recorre todas las oraciones del curso, identificando en cada una los tokens lematizados
3. Para cada nivel CEFR, el analizador obtiene los lemas esperados del catalogo EVP
4. El analizador cuenta cuantas veces aparece cada lema esperado en el conjunto completo de oraciones del curso
5. El analizador clasifica cada lema segun su nivel de exposicion (ausente, sub-expuesto, normal, sobre-expuesto) y le asigna una puntuacion individual
6. El analizador calcula la puntuacion de cada oracion como el promedio de las puntuaciones de los lemas esperados que contiene
7. La plataforma agrega las puntuaciones de oraciones hacia arriba a traves de la jerarquia: para cada knowledge calcula el promedio de sus quizzes, para cada topic el promedio de sus knowledges, para cada nivel el promedio de sus topics, y para el curso el promedio de sus niveles
8. El usuario recibe un informe con la puntuacion general, las puntuaciones por nivel de la jerarquia, y el detalle de la clasificacion de cada lema

### Journey[F-LCOUNT-J002] - Identificar lemas con exposicion insuficiente
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
2. El usuario observa que la puntuacion general del analizador "lemma-count" es baja (por ejemplo, 0.65)
3. El usuario consulta el detalle de lemas por nivel y filtra por estado "ausente" y "sub-expuesto"
4. El usuario identifica los lemas que requieren mas apariciones en el curso, priorizando los ausentes (puntuacion 0.0) sobre los sub-expuestos
5. El usuario puede tomar acciones correctivas: agregar oraciones que incluyan los lemas faltantes o sub-expuestos, o modificar oraciones existentes para incorporarlos

### Journey[F-LCOUNT-J003] - Localizar problemas de exposicion en la jerarquia del curso
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
2. El usuario observa que un nivel (por ejemplo, B1) tiene una puntuacion baja en el analizador "lemma-count"
3. El usuario profundiza en los topics del nivel B1 y encuentra que el topic "Travel Vocabulary" tiene la puntuacion mas baja
4. El usuario profundiza en los knowledges de "Travel Vocabulary" y revisa las puntuaciones por quiz
5. El usuario identifica que varias oraciones contienen unicamente lemas sobre-expuestos (que aportan puntuaciones inferiores a 1.0) y ningun lema sub-expuesto o ausente que podria incorporarse
6. El usuario reorganiza el contenido para distribuir mejor los lemas que necesitan mas exposicion

### Journey[F-LCOUNT-J004] - Comparar exposicion de vocabulario entre niveles
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
2. El usuario consulta las puntuaciones del analizador "lemma-count" por nivel CEFR
3. El usuario compara la proporcion de lemas ausentes y sub-expuestos entre niveles
4. El usuario identifica si los problemas de exposicion se concentran en un nivel especifico (por ejemplo, B2 tiene muchos lemas ausentes porque el contenido de B2 es menos extenso) o se distribuyen uniformemente
5. El usuario prioriza las acciones correctivas en los niveles con peor cobertura de vocabulario

### Journey[F-LCOUNT-J005] - Revisar lemas sobre-expuestos para optimizar el contenido
**Validation**: AUTO_VALIDATED

1. El usuario ha ejecutado la auditoria de conteo de lemas (J001)
2. El usuario filtra los lemas con estado "sobre-expuesto" y revisa cuales son
3. El usuario encuentra que lemas como "be" y "have" (nivel A1) tienen mas de 100 apariciones cada uno
4. El usuario evalua si estas sobre-exposiciones son realmente problematicas (verbos basicos que naturalmente aparecen con alta frecuencia) o si indican contenido repetitivo que podria mejorarse
5. El usuario decide si las sobre-exposiciones de verbos basicos son aceptables o si necesitan revision, considerando que la penalizacion por sobre-exposicion puede producir falsos positivos para vocabulario de muy alta frecuencia

---

## Open Questions

### Doubt[DOUBT-OVER-EXPOSURE-THRESHOLD] - El umbral de sobre-exposicion (15) es demasiado bajo para lemas basicos?
**Status**: OPEN

El umbral de sobre-exposicion actual es de 15 apariciones. Esto significa que cualquier lema que aparece mas de 15 veces en el curso se clasifica como "sobre-expuesto" y su puntuacion comienza a degradarse. Sin embargo, para lemas de vocabulario muy basico de nivel A1 como "be", "have", "do", "go", "make", que son verbos auxiliares o de uso extremadamente frecuente, 15 apariciones en un curso de mas de 11.000 oraciones es un umbral muy bajo. Estos lemas naturalmente aparecen cientos de veces y su sobre-exposicion no es un defecto del curso sino una caracteristica inherente del idioma.

Con el factor de degradacion actual (1/50), un lema que aparece 65 veces recibe puntuacion 0.0. Para un verbo como "be" que puede aparecer 500 veces o mas en el curso, esto produce un falso positivo grave que distorsiona tanto la puntuacion del lema como la puntuacion general del analisis.

**Pregunta**: Como se debe manejar la sobre-exposicion para lemas de vocabulario basico que naturalmente tienen alta frecuencia?

- [ ] Opcion A: Mantener el umbral de 15 para todos los lemas, aceptando que algunos tendran puntuaciones artificialmente bajas. Es el comportamiento de la referencia.
- [ ] Opcion B: Definir umbrales diferenciados por nivel CEFR: un umbral mas alto para A1 (por ejemplo, 50) y progresivamente mas bajos para niveles superiores, reflejando que el vocabulario basico naturalmente aparece con mas frecuencia.
- [ ] Opcion C: Excluir del analisis de sobre-exposicion los lemas cuya frecuencia de uso en el idioma es extremadamente alta (por ejemplo, los top 100 del ranking COCA), ya que su sobre-exposicion es inevitable y no indica un problema de contenido.
- [ ] Opcion D: Ajustar el factor de degradacion en lugar de los umbrales: usar un factor mas suave (por ejemplo, 1/200 en vez de 1/50) para que la penalizacion sea mas gradual y tolere mejor las altas frecuencias.

### Doubt[DOUBT-DEGRADATION-FACTOR] - El factor de degradacion de sobre-exposicion (1/50) es adecuado?
**Status**: OPEN

El factor de degradacion para lemas sobre-expuestos es 1/50, lo que significa que por cada aparicion adicional mas alla del umbral maximo, la puntuacion se reduce en 0.02. Esto implica que la puntuacion llega a 0.0 cuando el exceso alcanza 50 apariciones (es decir, a 65 apariciones totales con umbral de 15).

Este factor es arbitrario y proviene de la implementacion de referencia sin justificacion documentada. Un factor de 1/50 produce una degradacion muy agresiva: a las 40 apariciones (25 de exceso) la puntuacion ya es 0.50.

**Pregunta**: El factor de degradacion de 1/50 es adecuado, o deberia ser diferente?

- [ ] Opcion A: Mantener 1/50 como en la referencia
- [ ] Opcion B: Usar un factor mas suave (por ejemplo, 1/100 o 1/200) para tolerar mejor las frecuencias altas naturales
- [ ] Opcion C: Usar una funcion de degradacion no lineal (por ejemplo, logaritmica) que penalice menos los primeros excesos y mas los extremos
- [ ] Opcion D: Hacer el factor configurable junto con los umbrales de exposicion

### Doubt[DOUBT-COUNT-SCOPE] - El conteo debe ser sobre todo el curso o solo sobre el nivel del lema?
**Status**: OPEN

La regla R002 establece que el conteo de apariciones de un lema se realiza sobre todo el curso completo, no solo sobre las oraciones del nivel donde el EVP lo clasifica. Esta decision tiene implicaciones:

**A favor de contar en todo el curso:**
- El aprendizaje es acumulativo: un estudiante que aprendio "cat" en A1 sigue encontrandolo en A2, B1, B2
- Refleja la exposicion total del estudiante al lema
- Es coherente con la implementacion de referencia

**A favor de contar solo en el nivel del lema:**
- Permite evaluar si cada nivel tiene suficiente exposicion a sus propios lemas objetivo
- Un lema de B2 que solo aparece en A1 no deberia considerarse "adecuado" para B2
- Da mayor precision sobre donde esta la exposicion

**Pregunta**: El conteo de apariciones debe abarcar todo el curso o solo el nivel CEFR del lema?

- [ ] Opcion A: Contar en todo el curso (comportamiento de la referencia, refleja exposicion total)
- [ ] Opcion B: Contar solo en el nivel del lema (mayor precision por nivel)
- [ ] Opcion C: Contar en todo el curso pero ponderar las apariciones fuera del nivel con un factor de descuento (por ejemplo, las apariciones en otros niveles cuentan la mitad)

### Doubt[DOUBT-CONFIGURABLE-THRESHOLDS] - Los umbrales deberian ser configurables por nivel CEFR?
**Status**: OPEN

Los umbrales actuales (umbralMinimo = 4, umbralMaximo = 15) son globales: se aplican por igual a todos los lemas independientemente de su nivel CEFR. Sin embargo, es razonable pensar que los lemas de A1 (vocabulario basico, muy frecuente) necesitan un umbral de sobre-exposicion diferente al de los lemas de B2 (vocabulario avanzado, menos frecuente).

**Pregunta**: Los umbrales de exposicion deberian poder definirse por nivel CEFR?

- [ ] Opcion A: Mantener umbrales globales (simplicidad, alineado con la referencia)
- [ ] Opcion B: Definir umbrales por nivel CEFR (mayor precision, mayor complejidad de configuracion)
- [ ] Opcion C: Definir umbrales globales en esta primera version, pero disenar la configuracion para que sea extensible a umbrales por nivel en el futuro

### Doubt[DOUBT-ABSENT-VS-LEMMA-ABSENCE] - Como se relaciona el estado "ausente" de este analizador con el analizador de LemmaAbsence?
**Status**: OPEN

El analizador LemmaAbsence (documentado en el analisis 03) ya detecta lemas del EVP que estan completamente ausentes del curso. Este analizador (LemmaCount) tambien detecta lemas ausentes como parte de su clasificacion (estado "ausente", puntuacion 0.0).

Existe una superposicion funcional: ambos analizadores detectan lemas ausentes, aunque con perspectivas diferentes. LemmaAbsence clasifica el tipo de ausencia (completamente ausente, aparece en otro nivel, disperso) y genera recomendaciones, mientras que LemmaCount solo registra que el conteo es cero y asigna puntuacion 0.0.

**Pregunta**: Como se debe manejar esta superposicion?

- [ ] Opcion A: Cada analizador opera independientemente; la superposicion es aceptable porque proveen perspectivas complementarias. El usuario ve ambos resultados y los usa para diferentes propositos.
- [ ] Opcion B: LemmaCount excluye los lemas ausentes de su analisis (solo evalua lemas que tienen al menos 1 aparicion), delegando la deteccion de ausencias a LemmaAbsence.
- [ ] Opcion C: Se define un orden de analisis donde LemmaAbsence se ejecuta primero y sus resultados informan a LemmaCount.
