---
feature:
  id: FEAT-LABS
  code: F-LABS
  name: Analisis de Ausencia de Lemas por Nivel CEFR
  priority: critical
---

# Analisis de Ausencia de Lemas por Nivel CEFR

Detectar y clasificar los **lemas (formas base de palabras) que deberian estar presentes en un nivel CEFR pero estan ausentes del curso**, utilizando el catalogo EVP (English Vocabulary Profile) como referencia de vocabulario esperado por nivel. El analizador no solo identifica que lemas faltan, sino que clasifica el tipo de ausencia (completamente ausente, aparece demasiado temprano, aparece demasiado tarde, ubicacion dispersa), asigna una prioridad basada en la frecuencia COCA del lema, y genera recomendaciones accionables para que el creador de contenido corrija las brechas de vocabulario. Adicionalmente, evalua el impacto de los lemas mal ubicados en las oraciones del curso mediante un sistema de scoring por oracion.

## Contexto

El sistema ContentAudit audita cursos de idiomas para garantizar que el contenido es pedagogicamente adecuado. Una de las dimensiones mas criticas es la **cobertura de vocabulario esperado**: si el EVP indica que "cat" es una palabra de nivel A1, entonces "cat" (o alguna de sus formas flexionadas: cats, cat's) deberia aparecer al menos una vez en el contenido de A1. La ausencia de vocabulario basico en un nivel genera lagunas en el aprendizaje del estudiante.

### Premisa pedagogica

La investigacion en adquisicion de vocabulario establece que los estudiantes necesitan exposicion sistematica al vocabulario propio de su nivel. Si palabras fundamentales estan ausentes en el nivel correspondiente, el estudiante no tiene oportunidad de aprenderlas en el contexto adecuado. Peor aun, si una palabra de nivel A1 solo aparece en B2, el estudiante llega al nivel avanzado sin haber consolidado vocabulario basico, lo cual genera lagunas acumulativas. Este analizador permite identificar y priorizar estas brechas para que el creador de contenido pueda corregirlas.

### Catalogo EVP como referencia

El English Vocabulary Profile (EVP) es la fuente de verdad que define que lemas se esperan en cada nivel CEFR (A1, A2, B1, B2). Cada entrada del EVP asocia un lema con un nivel CEFR esperado y una parte de la oracion (part-of-speech). Solo se evaluan lemas individuales (no frases compuestas) que sean palabras de contenido. Las frases multipalabra que el EVP pueda contener se excluyen del analisis.

El catalogo enriquecido agrega informacion adicional a cada lema: ranking de frecuencia COCA, categoria semantica y etiquetas POS normalizadas. Esta informacion permite priorizar los lemas ausentes por impacto comunicativo.

### Comparacion por lema y parte de la oracion

La unidad de comparacion es el par **lema + parte de la oracion** (LemmaAndPos). Esto significa que "run" como verbo y "run" como sustantivo se consideran lemas distintos. Si el EVP espera "run" como verbo en A2 pero el curso solo contiene "run" como sustantivo en A2, el verbo "run" se marca como ausente. Esta distincion es linguisticamente correcta porque el significado y uso de una misma palabra varia segun su funcion gramatical.

### Palabras de contenido vs. palabras funcionales

Solo se evaluan **palabras de contenido** (sustantivos, verbos, adjetivos, adverbios). Las palabras funcionales (articulos, preposiciones, conjunciones, pronombres) se excluyen del analisis de ausencia porque su frecuencia de aparicion es tan alta que nunca estarian genuinamente "ausentes" del curso.

Sin embargo, existe una excepcion importante: determinadas palabras funcionales son pedagogicamente criticas en niveles iniciales (por ejemplo, los pronombres personales "I", "you", "he", los auxiliares "be", "have", "do", las preposiciones basicas "in", "on", "at"). Estas palabras tienen un tratamiento especial definido en las reglas de negocio.

### Relacion con otros analizadores de vocabulario

Este analizador es **complementario** a otros analizadores del sistema:

| Analizador | Que mide | Relacion con LemmaAbsence |
|------------|----------|---------------------------|
| LemmaAbsence (este) | Que lemas esperados no aparecen o estan mal ubicados | -- |
| LemmaCount (FEAT-LCOUNT) | Cuantas veces aparece cada lema presente | Complementario: LemmaAbsence detecta lo que falta, LemmaCount evalua si lo presente aparece suficientes veces |
| LemmaRecurrence (FEAT-LREC) | A que intervalos se repite cada lema | Complementario: un lema presente pero con mala distribucion temporal es problema de recurrencia, no de ausencia |
| COCA Distribution (FEAT-COCA) | Si la distribucion por frecuencia es adecuada | Relacionado: ambos usan el ranking COCA, pero COCA evalua distribucion global y LemmaAbsence evalua cobertura individual |

### Jerarquia del curso

El curso tiene una estructura jerarquica fija (definida en FEAT-COURSE): **Curso -> Nivel (Milestone) -> Topic -> Knowledge -> Quiz**. Los **quizzes** contienen las oraciones evaluables, y cada oracion esta compuesta por tokens procesados linguisticamente. El analizador opera comparando los lemas presentes en las oraciones de cada nivel contra los lemas esperados del EVP para ese nivel.

### Niveles criticos: A1 y A2

Los niveles A1 y A2 se consideran **criticos** para la evaluacion de ausencia. La ausencia de vocabulario basico en estos niveles tiene mayor impacto pedagogico porque son los cimientos sobre los que se construye todo el aprendizaje posterior. Un estudiante que no domina el vocabulario de A1 no puede progresar eficazmente a A2 y niveles superiores. Por este motivo, los umbrales de tolerancia son mas estrictos para A1 y A2.

---

## Reglas de Negocio

Las reglas se organizan en seis grupos segun la fase y el tema al que pertenecen:

- **Grupo A - Identificacion de lemas ausentes (R001-R005)**: reglas que describen como se determinan los lemas esperados, presentes y ausentes para cada nivel CEFR.
- **Grupo B - Clasificacion del tipo de ausencia (R006-R010)**: reglas que describen como se clasifica cada lema ausente segun donde aparece (o no) en el curso.
- **Grupo C - Prioridad y enriquecimiento (R011-R016)**: reglas que describen como se asigna prioridad basada en frecuencia COCA y como se enriquece cada lema con informacion adicional.
- **Grupo D - Scoring por oracion (R017-R020)**: reglas que describen como se evalua el impacto de los lemas mal ubicados en cada oracion del curso.
- **Grupo E - Assessment global y metricas por nivel (R021-R026)**: reglas que describen la evaluacion global, los umbrales de tolerancia y las metricas resultantes por nivel.
- **Grupo F - Recomendaciones (R027-R031)**: reglas que describen la generacion de recomendaciones accionables para corregir las brechas de vocabulario.

---

### Grupo A - Identificacion de lemas ausentes

### Rule[F-LABS-R001] - Obtencion de lemas esperados por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR (A1, A2, B1, B2), se obtienen los **lemas esperados** del catalogo EVP. El conjunto de lemas esperados se filtra aplicando dos criterios:

1. **Solo lemas individuales**: se excluyen las frases multipalabra (entradas del EVP marcadas como frases). Solo participan lemas de una sola palabra.
2. **Solo palabras de contenido**: se excluyen las palabras funcionales (articulos, preposiciones, conjunciones, pronombres, etc.) segun el filtro de palabras de contenido. Las excepciones de lemas funcionales criticos se manejan en R016.

Cada lema esperado se identifica por el par **lema + parte de la oracion** (LemmaAndPos). Dos entradas con el mismo lema pero distinta parte de la oracion se tratan como lemas esperados independientes.

**Error**: "No se pudieron obtener los lemas esperados para el nivel {nivel}: el catalogo EVP no contiene entradas para este nivel"

### Rule[F-LABS-R002] - Obtencion de lemas presentes por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR, se obtienen los **lemas presentes** en las oraciones del curso correspondientes a ese nivel. Los lemas presentes provienen del procesamiento linguistico previo (FEAT-NLP): cada token de cada oracion tiene asociada una forma lematizada y una etiqueta POS. Los lemas presentes se representan tambien como pares **lema + parte de la oracion** (LemmaAndPos).

Se consideran todos los lemas presentes en todas las oraciones de todos los quizzes de todos los knowledges de todos los topics del nivel. Un lema se considera presente en un nivel si aparece al menos una vez en cualquier oracion de ese nivel.

**Error**: "No se pudieron obtener los lemas presentes para el nivel {nivel}: no hay oraciones procesadas en este nivel"

### Rule[F-LABS-R003] - Calculo de lemas ausentes por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR, los **lemas ausentes** son la diferencia entre los lemas esperados y los lemas presentes:

lemas ausentes del nivel = lemas esperados del nivel - lemas presentes del nivel

La comparacion se realiza por el par LemmaAndPos: un lema se considera ausente si no existe ninguna ocurrencia con el mismo lema y la misma parte de la oracion en el nivel correspondiente. Si el EVP espera "run" como verbo en A2 y el curso tiene "run" como sustantivo en A2 (pero no como verbo), entonces "run" como verbo se marca como ausente en A2.

**Error**: N/A (esta regla describe un calculo de conjuntos)

### Rule[F-LABS-R004] - Busqueda de lemas ausentes en otros niveles
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada lema identificado como ausente en su nivel esperado, el sistema busca si ese lema (con su misma parte de la oracion) aparece en **otros niveles** del curso. El resultado es una lista de niveles donde el lema esta presente (que puede estar vacia si el lema no aparece en ningun nivel).

Esta informacion es esencial para la clasificacion del tipo de ausencia (Grupo B): si un lema esperado en A2 aparece en B1, no es "completamente ausente" sino que "aparece demasiado tarde". La busqueda cubre todos los niveles del curso, independientemente de su relacion con el nivel esperado.

**Error**: N/A (esta regla describe una busqueda de datos)

### Rule[F-LABS-R005] - Exclusion de frases multipalabra del EVP
**Severity**: major | **Validation**: AUTO_VALIDATED

El catalogo EVP puede contener tanto lemas individuales como frases multipalabra (por ejemplo, "look for", "get up", "a lot of"). Solo los lemas individuales participan en el analisis de ausencia. Las frases multipalabra se excluyen porque su deteccion requiere analisis de secuencias de tokens, lo cual excede el alcance de este analizador que opera a nivel de lema individual.

La determinacion de si una entrada del EVP es un lema individual o una frase multipalabra se basa en el indicador de frase del catalogo.

**Error**: N/A (esta regla describe un criterio de filtrado)

---

### Grupo B - Clasificacion del tipo de ausencia

### Rule[F-LABS-R006] - Tipos de ausencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema ausente se clasifica en uno de tres tipos de ausencia, segun donde aparece (o no) en el curso:

| Tipo de ausencia | Descripcion | Impacto pedagogico | Accion correctiva |
|------------------|-------------|-------------------|-------------------|
| COMPLETELY_ABSENT | El lema no aparece en ninguna oracion de ningun nivel del curso | El mas grave: el estudiante no tiene ninguna exposicion al lema | Agregar oraciones con el lema en el nivel esperado |
| APPEARS_TOO_LATE | El lema aparece pero solo en niveles posteriores al esperado | Grave: el estudiante no tiene acceso al lema cuando lo necesita | Agregar el lema al nivel esperado |
| APPEARS_TOO_EARLY | El lema aparece en al menos un nivel anterior al esperado | Moderado: el lema se introdujo antes de lo adecuado | Quitar o reemplazar el lema en los niveles donde no corresponde |

La clasificacion prioriza la deteccion de apariciones tempranas: si un lema aparece en cualquier nivel anterior al esperado, se clasifica como APPEARS_TOO_EARLY independientemente de que tambien aparezca en niveles posteriores. Esto orienta la accion correctiva hacia la remocion del lema de niveles inadecuados.

La prioridad de correccion: los lemas completamente ausentes son los mas urgentes, seguidos por los que aparecen demasiado tarde.

**Error**: N/A (esta regla describe una clasificacion)

### Rule[F-LABS-R007] - Algoritmo de clasificacion de ausencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

El algoritmo para determinar el tipo de ausencia de un lema es el siguiente:

1. Se obtiene la lista de niveles del curso donde el lema esta presente (R004).
2. Si la lista esta **vacia** (el lema no aparece en ningun nivel): tipo = **COMPLETELY_ABSENT**
3. Si la lista **no esta vacia**, se compara cada nivel presente con el nivel esperado usando el orden numerico de los niveles CEFR (A1=1, A2=2, B1=3, B2=4):
   - Si **al menos un** nivel presente tiene un orden **menor** que el nivel esperado: tipo = **APPEARS_TOO_EARLY** (independientemente de que tambien aparezca en niveles posteriores)
   - Si **todos** los niveles presentes tienen un orden **mayor** que el nivel esperado: tipo = **APPEARS_TOO_LATE**

Ejemplo: lema esperado en A2 (orden 2). Si aparece en A1 (orden 1) -> APPEARS_TOO_EARLY. Si aparece en A1 (orden 1) y B2 (orden 4) -> APPEARS_TOO_EARLY (prioriza la deteccion de aparicion temprana). Si solo aparece en B1 (orden 3) y B2 (orden 4) -> APPEARS_TOO_LATE.

**Error**: N/A (esta regla describe un algoritmo de clasificacion)

### Rule[F-LABS-R008] - Puntuacion de impacto por tipo de ausencia
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada tipo de ausencia tiene asociada una **puntuacion de impacto** que refleja su gravedad pedagogica:

| Tipo de ausencia | Puntuacion de impacto |
|------------------|-----------------------|
| COMPLETELY_ABSENT | 1.0 (impacto maximo) |
| APPEARS_TOO_LATE | 0.8 |
| APPEARS_TOO_EARLY | 0.6 |

La puntuacion de impacto se utiliza para ponderar la importancia de cada lema ausente en el calculo de metricas por nivel y en la priorizacion de recomendaciones. Un lema completamente ausente con impacto 1.0 contribuye mas al deterioro de la puntuacion que un lema que aparece demasiado temprano (impacto 0.6).

**Error**: N/A (esta regla describe valores fijos de puntuacion)

### Rule[F-LABS-R009] - APPEARS_TOO_LATE es mas grave que APPEARS_TOO_EARLY
**Severity**: major | **Validation**: AUTO_VALIDATED

La clasificacion APPEARS_TOO_LATE tiene mayor impacto (0.8) que APPEARS_TOO_EARLY (0.6) porque sus consecuencias pedagogicas son distintas:

- **APPEARS_TOO_LATE**: El estudiante no tuvo acceso al vocabulario cuando lo necesitaba. Una palabra de A1 que recien aparece en B2 significa que el estudiante paso tres niveles sin exposicion a vocabulario que deberia haber dominado desde el principio. Esto genera una laguna acumulativa.

- **APPEARS_TOO_EARLY**: El estudiante fue expuesto al vocabulario antes de tiempo. Por ejemplo, "nevertheless" en A1 no es adecuado para ese nivel. La accion correctiva es quitar o reemplazar el lema en los niveles donde no corresponde. Es menos grave que TOO_LATE porque el vocabulario al menos esta presente en el curso.

**Error**: N/A (esta regla describe una justificacion de diseno)

### Rule[F-LABS-R010] - Un lema presente en su nivel esperado no es ausente
**Severity**: critical | **Validation**: AUTO_VALIDATED

Si un lema aparece en su nivel esperado (ademas de en otros niveles), **no se considera ausente** en ese nivel, independientemente de que tambien aparezca en otros niveles. La clasificacion de ausencia solo aplica cuando el lema no esta presente en el nivel donde el EVP indica que deberia estar.

Ejemplo: si "cat" (NOUN) es esperado en A1 y aparece en A1, A2 y B1, no es ausente en A1. Solo si "cat" (NOUN) no aparece en A1 se activa el analisis de ausencia.

**Error**: N/A (esta regla describe una condicion de exclusion)

---

### Grupo C - Prioridad y enriquecimiento

### Rule[F-LABS-R011] - Asignacion de prioridad por frecuencia COCA
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada lema ausente recibe un nivel de prioridad basado en su **ranking de frecuencia COCA** (Corpus of Contemporary American English). El ranking COCA indica cuan frecuente es una palabra en el uso real del idioma: las palabras con ranking mas bajo son las mas frecuentes y por lo tanto las mas criticas de cubrir.

Las bandas de prioridad son:

| Prioridad | Rango de ranking COCA | Significado |
|-----------|----------------------|-------------|
| HIGH | 1 - 1000 | Palabra de muy alta frecuencia, critica para la comunicacion basica |
| MEDIUM | 1001 - 3000 | Palabra frecuente, importante para la comunicacion general |
| LOW | 3001 - 5000 | Palabra de frecuencia media, relevante pero no critica |

Los lemas con ranking COCA superior a 5000 no se incluyen en el analisis priorizado (se consideran de frecuencia baja y su ausencia tiene menor impacto).

[ASSUMPTION] Los rangos de prioridad se unifican tomando los valores de la configuracion por nivel (1000, 3000, 5000) que son mas amplios y cubren mejor el espectro de frecuencia. La configuracion original del sistema tenia dos conjuntos de rangos inconsistentes: uno en config.yaml (HIGH<1000, MEDIUM<2000, LOW<3000) y otro en la configuracion por nivel (HIGH<1000, MEDIUM<3000, LOW<5000). Se propone unificar en los rangos mas amplios porque el rango [2001-5000] contiene vocabulario relevante que no deberia quedar fuera del analisis priorizado. Ver Doubt[DOUBT-PRIORITY-BANDS].

**Error**: "No se pudo determinar la prioridad del lema '{lema}': ranking COCA no disponible"

### Rule[F-LABS-R012] - Enriquecimiento de informacion de lemas ausentes
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada lema ausente, el sistema enriquece la informacion con los siguientes datos:

| Dato | Fuente | Descripcion |
|------|--------|-------------|
| Ranking COCA | Servicio de consulta de vocabulario | Posicion en la lista de frecuencia del corpus COCA |
| Categoria semantica | Catalogo EVP | Topico o area semantica del lema (ej: "animals", "food", "travel") |
| Nivel de prioridad | Calculado segun R011 | HIGH, MEDIUM o LOW basado en el ranking COCA |
| Frecuencia total | Servicio de consulta de vocabulario | Frecuencia absoluta del lema en el corpus de referencia |
| Parte de la oracion | Catalogo EVP / procesamiento linguistico | Categoria gramatical del lema (sustantivo, verbo, adjetivo, etc.) |

La informacion enriquecida permite al creador de contenido entender no solo que lemas faltan, sino por que importan y en que contexto semantico deberian incorporarse.

**Error**: "No se pudo enriquecer la informacion del lema '{lema}': {detalle del error}"

### Rule[F-LABS-R013] - Lemas sin ranking COCA disponible
**Severity**: major | **Validation**: ASSUMPTION

Algunos lemas del EVP pueden no tener un ranking COCA asociado (por ejemplo, lemas muy especializados, variantes britanicas, o errores en el catalogo). Estos lemas se incluyen en el analisis de ausencia pero reciben prioridad **LOW** por defecto, ya que la ausencia de ranking COCA sugiere que no son palabras de uso frecuente.

[ASSUMPTION] Se asume que los lemas sin ranking COCA reciben prioridad LOW. Esto es razonable porque si una palabra no aparece en el corpus COCA (uno de los corpus mas completos del ingles), es probable que sea de uso infrecuente. Sin embargo, existe el riesgo de subestimar lemas que son frecuentes en ingles britanico pero no en ingles americano (el COCA es un corpus americano).

**Error**: "Lema '{lema}' sin ranking COCA disponible: se asigna prioridad LOW por defecto"

### Rule[F-LABS-R014] - Umbrales de alerta por prioridad
**Severity**: major | **Validation**: AUTO_VALIDATED

El sistema define umbrales de alerta que determinan la cantidad maxima de lemas ausentes aceptables para cada nivel de prioridad antes de generar una alerta:

| Prioridad | Umbral de alerta |
|-----------|-----------------|
| HIGH | 0 (cero tolerancia: cualquier lema de alta prioridad ausente genera alerta) |
| MEDIUM | 3 (hasta 3 lemas de prioridad media ausentes antes de alerta) |
| LOW | 10 (hasta 10 lemas de baja prioridad ausentes antes de alerta) |

Estos umbrales se aplican por nivel CEFR. Si en A2 hay 2 lemas de prioridad HIGH ausentes, se genera una alerta de alta prioridad para A2.

**Error**: N/A (esta regla describe parametros de configuracion)

### Rule[F-LABS-R015] - Consistencia de etiquetas POS entre EVP y procesamiento linguistico
**Severity**: major | **Validation**: ASSUMPTION

La comparacion de lemas se realiza por el par LemmaAndPos (R003). Para que esta comparacion sea precisa, las etiquetas POS del catalogo EVP deben ser compatibles con las etiquetas POS asignadas por el procesamiento linguistico. Si el EVP clasifica "run" como "verb" y el procesamiento linguistico lo clasifica como "VERB", debe existir una normalizacion que haga compatibles ambas clasificaciones.

[ASSUMPTION] Se asume que el catalogo enriquecido ya tiene las etiquetas POS normalizadas al mismo esquema que usa el procesamiento linguistico. El catalogo EVP original tiene campos POS que a veces estan vacios o usan nomenclatura distinta; el catalogo enriquecido resuelve estas discrepancias usando el procesamiento linguistico para asignar POS estandarizados. Sin embargo, persiste el riesgo de inconsistencias: si el EVP espera "run" como verbo pero el procesamiento linguistico clasifica una ocurrencia de "run" como sustantivo en cierto contexto, el verbo "run" podria marcarse incorrectamente como ausente. Ver Doubt[DOUBT-POS-CONSISTENCY].

**Error**: "Inconsistencia de POS detectada para el lema '{lema}': EVP indica '{posEVP}' pero el procesamiento linguistico asigna '{posNLP}'"

### Rule[F-LABS-R016] - Lemas funcionales criticos
**Severity**: major | **Validation**: AUTO_VALIDATED

Aunque las palabras funcionales se excluyen del analisis general de ausencia (R001), existe un conjunto de **lemas funcionales criticos** que reciben tratamiento especial. Estos son lemas funcionales tan fundamentales para la comunicacion basica que su ausencia en niveles iniciales seria un problema grave:

| Categoria | Lemas |
|-----------|-------|
| Pronombres personales | I, you, he, she, it, we, they |
| Auxiliares basicos | be, have, do, will, would, can, could, should, might, must |
| Preposiciones basicas | in, on, at, for, with, by, from, to, of, about |
| Conectores basicos | and, but, or, so, because, when, if, while, since |

Estos lemas funcionales criticos reciben **siempre prioridad HIGH**, independientemente de su ranking COCA. Si alguno de estos lemas esta ausente en niveles A1 o A2, se marca como una alerta critica.

[ASSUMPTION] Se asume que los lemas funcionales criticos solo se evaluan en los niveles A1 y A2 (niveles criticos donde estos lemas son imprescindibles). En niveles B1 y B2, su ausencia seria inusual pero no se considera critica porque el estudiante ya deberia haberlos adquirido. Ver Doubt[DOUBT-FUNCTIONAL-SCOPE].

**Error**: N/A (esta regla describe una lista de excepcion al filtrado de palabras funcionales)

---

### Grupo D - Scoring por oracion

### Rule[F-LABS-R017] - Identificacion de lemas mal ubicados en una oracion
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada oracion del curso, el sistema identifica los lemas que aparecen en la oracion pero cuyo nivel esperado (segun el EVP) difiere del nivel al que pertenece la oracion. Un lema se considera "mal ubicado" en una oracion si la oracion pertenece a un nivel distinto al nivel esperado del lema.

Ejemplo: una oracion del nivel B1 que contiene el lema "cat" (esperado en A1). El lema "cat" esta mal ubicado en esta oracion porque se introdujo dos niveles despues de lo esperado.

Solo los lemas que estan en el catalogo EVP participan en esta evaluacion. Los lemas que no aparecen en el EVP no se consideran mal ubicados.

**Error**: N/A (esta regla describe un mecanismo de deteccion)

### Rule[F-LABS-R018] - Descuento por distancia de nivel
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada lema mal ubicado en una oracion, se calcula un **descuento** proporcional a la distancia entre el nivel de la oracion y el nivel esperado del lema:

descuento = 0.1 * distancia de niveles

La distancia de niveles se calcula como la diferencia absoluta en el orden numerico de los niveles CEFR (A1=1, A2=2, B1=3, B2=4).

Ejemplos:
- Lema A1 en oracion A2: distancia = |2-1| = 1, descuento = 0.1
- Lema A1 en oracion B1: distancia = |3-1| = 2, descuento = 0.2
- Lema A1 en oracion B2: distancia = |4-1| = 3, descuento = 0.3

El factor de 0.1 por nivel de distancia es un valor fijo.

[ASSUMPTION] Se asume que el factor de descuento (0.1 por nivel) es un valor fijo no configurable. El analisis original lo describe como un valor hardcodeado. Si bien podria ser beneficioso hacerlo configurable, la simplicidad del valor actual (10% de penalizacion por cada nivel de separacion) es razonable y facil de interpretar. Ver Doubt[DOUBT-DISCOUNT-FACTOR].

**Error**: N/A (esta regla describe una formula de calculo)

### Rule[F-LABS-R019] - Calculo de score por oracion
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de una oracion se calcula como:

score de la oracion = 1.0 - maximo descuento entre todos los lemas mal ubicados

Se toma el **maximo** descuento (no la suma) porque la presencia de un solo lema severamente mal ubicado es suficiente para penalizar la oracion. El descuento mayor refleja el peor caso de descolocacion en esa oracion.

Ejemplos:
- Oracion sin lemas mal ubicados: score = 1.0 (perfecta)
- Oracion con un lema a 1 nivel de distancia: score = 1.0 - 0.1 = 0.9
- Oracion con lemas a distancia 1 y 3: score = 1.0 - max(0.1, 0.3) = 0.7
- Oracion con un lema a 3 niveles de distancia: score = 1.0 - 0.3 = 0.7

El score minimo posible por oracion es 0.7 (un lema a la maxima distancia de 3 niveles, por ejemplo A1 en B2).

**Error**: "Score calculado fuera de rango [0.0, 1.0] para la oracion {sentenceId}: {score}"

### Rule[F-LABS-R020] - Oraciones sin lemas mal ubicados
**Severity**: minor | **Validation**: AUTO_VALIDATED

Las oraciones que no contienen ningun lema mal ubicado (todos sus lemas EVP pertenecen al nivel correcto, o la oracion no contiene lemas del EVP) reciben una puntuacion de **1.0** (maxima). Estas oraciones no contribuyen negativamente al score del nivel.

**Error**: N/A (esta regla describe un caso base)

---

### Grupo E - Assessment global y metricas por nivel

### Rule[F-LABS-R021] - Umbrales de tolerancia por nivel CEFR
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada nivel CEFR tiene configurado un **umbral de tolerancia** que define la cantidad maxima de lemas ausentes aceptables y un **porcentaje maximo** de ausencia respecto al total de lemas esperados:

| Nivel | Umbral absoluto (lemas) | Porcentaje maximo |
|-------|------------------------|-------------------|
| A1 | 0 | 0% |
| A2 | 2 | 5% |
| B1 | 5 | 10% |
| B2 | 8 | 15% |

Los umbrales son **acumulativos**: un nivel excede su umbral si supera **cualquiera** de los dos limites (el absoluto o el porcentual). Ejemplo: si A2 tiene 1 lema ausente pero esto representa el 6% de sus lemas esperados, excede el umbral porcentual (5%) aunque no exceda el absoluto (2).

Los niveles A1 y A2 tienen umbrales mas estrictos (especialmente A1 con cero tolerancia) porque la ausencia de vocabulario basico en niveles iniciales tiene consecuencias pedagogicas desproporcionadas.

[ASSUMPTION] Se adoptan los umbrales de la configuracion por nivel (0, 2, 5, 8) en lugar de los umbrales globales (5, 10, 15, 20) que estaban en config.yaml. Los umbrales por nivel son mas estrictos y especificos, lo que permite una evaluacion mas precisa. Los umbrales globales probablemente eran una version simplificada anterior. Ver Doubt[DOUBT-DUAL-THRESHOLDS].

**Error**: "Configuracion de umbrales invalida para el nivel {nivel}: los valores deben ser no negativos"

### Rule[F-LABS-R022] - Categorias de assessment global
**Severity**: critical | **Validation**: AUTO_VALIDATED

El assessment global clasifica el estado de la ausencia de vocabulario en el curso completo en una de tres categorias:

| Categoria | Condicion |
|-----------|-----------|
| OPTIMAL | Todos los niveles estan dentro de sus umbrales de tolerancia |
| ACCEPTABLE | Algunos niveles exceden sus umbrales, pero ningun nivel critico (A1, A2) los excede |
| NEEDS_IMPROVEMENT | Al menos un nivel critico (A1 o A2) excede su umbral de tolerancia |

La evaluacion prioriza los niveles A1 y A2: incluso si B1 y B2 estan perfectos, si A1 excede su umbral el assessment es NEEDS_IMPROVEMENT.

**Error**: N/A (esta regla describe una logica de clasificacion)

### Rule[F-LABS-R023] - Metricas por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR, el resultado incluye las siguientes metricas:

| Metrica | Descripcion |
|---------|-------------|
| Total de lemas esperados | Cantidad de lemas del EVP para este nivel (despues de filtrado) |
| Total de lemas ausentes | Cantidad de lemas que no aparecen en el nivel |
| Porcentaje de cobertura | (lemas presentes / lemas esperados) * 100 |
| Coverage target | Porcentaje objetivo de cobertura para este nivel (configurable, R032) |
| Lemas ausentes por tipo | Desglose de lemas ausentes agrupados por tipo de ausencia (R006) |

Cada lema ausente en el desglose incluye la informacion enriquecida (R012): lema, nivel esperado, tipo de ausencia, ranking COCA, nivel de prioridad y parte de la oracion.

**Error**: N/A (esta regla describe la estructura del resultado)

### Rule[F-LABS-R024] - Puntuacion por nivel relativa al coverage target
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de cada nivel se calcula como la proporcion entre la cobertura actual y el coverage target del nivel:

1. Se calcula la **cobertura actual** del nivel: `1.0 - (ausencia ponderada por impact / total esperado)`. La ponderacion usa los impact scores de R008.
2. Si la cobertura alcanza o supera el coverage target -> **score = 1.0** (objetivo cumplido).
3. Si no -> **score = cobertura / target** (proporcion de avance hacia el objetivo).

Ejemplo: A1 con cobertura 0.68 y target 0.95 -> score = 0.68 / 0.95 = 0.72. Cuando la cobertura alcance 0.95, el score sera 1.0.

La **puntuacion global** es el promedio ponderado de las puntuaciones por nivel:

| Nivel | Peso |
|-------|------|
| A1 | 2.0 |
| A2 | 2.0 |
| B1 | 1.0 |
| B2 | 1.0 |

**Error**: "Error al calcular la puntuacion global: {detalle}"

### Rule[F-LABS-R032] - Coverage targets configurables por nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada nivel CEFR tiene un **coverage target** configurable que define el porcentaje de cobertura de vocabulario esperado para considerar el nivel como completo:

| Nivel | Target por defecto | Justificacion |
|-------|--------------------|---------------|
| A1 | 95% | Vocabulario fundamental, cobertura casi total requerida |
| A2 | 85% | Todavia basico, el estudiante ya tiene base A1 |
| B1 | 70% | Vocabulario mas amplio, el estudiante infiere por contexto |
| B2 | 55% | EVP muy extenso, el curso cubre lo esencial |

Los targets son configurables via `LemmaAbsenceConfig.getCoverageTarget(CefrLevel)`. Los niveles iniciales requieren mayor cobertura porque son la base del aprendizaje. Los niveles avanzados tienen targets menores porque el EVP es mas extenso y el estudiante ya tiene herramientas para inferir vocabulario por contexto.

**Error**: N/A (esta regla describe valores configurables)

### Rule[F-LABS-R025] - Umbrales de assessment global
**Severity**: major | **Validation**: AUTO_VALIDATED

Los umbrales que definen los limites entre las categorias de assessment son:

| Parametro | Valor | Descripcion |
|-----------|-------|-------------|
| Umbral critico | 10 | Si la suma total de lemas ausentes de alta prioridad excede este valor, el assessment no puede ser OPTIMAL |
| Umbral aceptable | 5 | Si la suma total de lemas ausentes de alta prioridad esta entre este valor y el critico, el assessment es ACCEPTABLE |
| Umbral optimo | 0 | Solo si no hay lemas ausentes de alta prioridad el assessment puede ser OPTIMAL (combinado con R022) |

Estos umbrales se aplican como condicion adicional a las reglas de R022, proporcionando un criterio numerico absoluto ademas del criterio por nivel.

**Error**: N/A (esta regla describe parametros de configuracion)

### Rule[F-LABS-R026] - Limites de reporte por prioridad
**Severity**: minor | **Validation**: AUTO_VALIDATED

El resultado del analisis limita la cantidad de lemas ausentes reportados por prioridad para evitar sobrecargar al creador de contenido:

| Prioridad | Maximo de lemas reportados |
|-----------|---------------------------|
| HIGH | 20 |
| MEDIUM | 30 |
| LOW | 50 |

Si hay mas lemas ausentes que el limite, se reportan los mas criticos (por impacto de ausencia y ranking COCA) y se indica la cantidad total para visibilidad. Ejemplo: "Se reportan 20 de 35 lemas ausentes de prioridad HIGH. Se muestran los de mayor impacto."

**Error**: N/A (esta regla describe un limite de visualizacion)

---

### Grupo F - Recomendaciones

### Rule[F-LABS-R027] - Generacion de recomendaciones por nivel
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada nivel CEFR que excede su umbral de tolerancia, el sistema genera una o mas recomendaciones accionables. Cada recomendacion incluye:

| Campo | Descripcion |
|-------|-------------|
| Accion | Que hacer (ej: "Agregar vocabulario A1 ausente", "Reubicar lemas de B1 que aparecen en A1") |
| Descripcion | Explicacion detallada del problema y la solucion propuesta |
| Prioridad | HIGH, MEDIUM o LOW, basada en el nivel afectado y la gravedad de la ausencia |
| Lemas afectados | Lista de lemas que la recomendacion pretende resolver |
| Nivel de esfuerzo | Estimacion del esfuerzo necesario (LOW, MEDIUM, HIGH) |
| Impacto esperado | Cuanto mejoraria la puntuacion si se implementa la recomendacion |

**Error**: N/A (esta regla describe la estructura de las recomendaciones)

### Rule[F-LABS-R028] - Tipos de accion en recomendaciones
**Severity**: major | **Validation**: AUTO_VALIDATED

Las recomendaciones se generan segun el tipo de ausencia predominante en el nivel:

| Tipo de ausencia | Accion recomendada |
|------------------|--------------------|
| COMPLETELY_ABSENT | "Agregar oraciones que contengan los lemas ausentes en el nivel {nivel}" |
| APPEARS_TOO_LATE | "Introducir los lemas ausentes en el nivel esperado ({nivelEsperado}) ademas de donde ya aparecen" |
| APPEARS_TOO_EARLY | "Quitar o reemplazar los lemas en los niveles anteriores donde no corresponden para que aparezcan desde {nivelEsperado} en adelante" |

**Error**: N/A (esta regla describe la logica de recomendaciones)

### Rule[F-LABS-R029] - Prioridad de las recomendaciones
**Severity**: major | **Validation**: AUTO_VALIDATED

La prioridad de cada recomendacion se determina combinando dos factores:

1. **Nivel afectado**: recomendaciones para A1 o A2 tienen mayor prioridad que para B1 o B2.
2. **Prioridad de los lemas afectados**: recomendaciones que involucran lemas de prioridad HIGH tienen mayor prioridad.

| Nivel afectado | Prioridad de lemas | Prioridad de la recomendacion |
|---------------|--------------------|-----------------------------|
| A1 o A2 | HIGH | HIGH |
| A1 o A2 | MEDIUM o LOW | HIGH |
| B1 o B2 | HIGH | MEDIUM |
| B1 o B2 | MEDIUM | MEDIUM |
| B1 o B2 | LOW | LOW |

Cualquier recomendacion para niveles criticos (A1, A2) recibe prioridad HIGH, independientemente de la prioridad de los lemas, porque la ausencia de vocabulario en niveles iniciales siempre es critica.

**Error**: N/A (esta regla describe una tabla de decision)

### Rule[F-LABS-R030] - Nivel de esfuerzo estimado
**Severity**: minor | **Validation**: ASSUMPTION

El nivel de esfuerzo de una recomendacion se estima segun la cantidad de lemas afectados:

| Cantidad de lemas | Nivel de esfuerzo |
|-------------------|-------------------|
| 1 - 5 | LOW |
| 6 - 15 | MEDIUM |
| 16 o mas | HIGH |

[ASSUMPTION] Se asume que la cantidad de lemas afectados es un proxy razonable para el esfuerzo del creador de contenido. En la practica, el esfuerzo depende de otros factores (complejidad de los lemas, disponibilidad de contextos naturales, etc.), pero una estimacion basada en cantidad es una primera aproximacion util.

**Error**: N/A (esta regla describe una estimacion)

### Rule[F-LABS-R031] - Nombre del analizador en el informe
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador de ausencia de lemas se identifica con el nombre **"lemma-absence"** en el informe de auditoria. Este nombre aparece en el resultado global del curso y permite al usuario identificar la puntuacion correspondiente a este analisis dentro del informe general.

**Error**: N/A (esta regla define un nombre de identificacion)

---

## User Journeys

### Journey[F-LABS-J001] - Auditar la ausencia de vocabulario de un curso completo
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LABS-J001
    name: Auditar la ausencia de vocabulario de un curso completo
    flow:
      - id: iniciar_auditoria
        action: "El usuario inicia una auditoria de ausencia de lemas de un curso previamente cargado en ContentAudit"
        then: ejecutar_analisis

      - id: ejecutar_analisis
        action: "ContentAudit analiza el curso y presenta los resultados de ausencia de vocabulario"
        gate: [F-LABS-R001, F-LABS-R002, F-LABS-R003, F-LABS-R005, F-LABS-R010]
        outcomes:
          - when: "El analisis se completa exitosamente"
            then: revisar_resumen
          - when: "No se dispone del catalogo de vocabulario esperado para los niveles del curso"
            then: error_sin_catalogo
          - when: "El curso no contiene oraciones procesadas linguisticamente"
            then: error_sin_oraciones

      - id: revisar_resumen
        action: "El usuario revisa el resumen general: assessment global (OPTIMAL, ACCEPTABLE, NEEDS_IMPROVEMENT), puntuacion global, y cantidad de lemas ausentes por nivel"
        gate: [F-LABS-R022, F-LABS-R023, F-LABS-R024]
        outcomes:
          - when: "El assessment es OPTIMAL y todos los niveles estan dentro de los umbrales"
            then: confirmar_cobertura
          - when: "Hay niveles que exceden los umbrales de lemas ausentes"
            then: explorar_detalle_nivel

      - id: explorar_detalle_nivel
        action: "El usuario selecciona un nivel para ver el detalle: lemas ausentes clasificados por tipo de ausencia (completamente ausente, aparece tarde, aparece temprano, disperso) y priorizados por frecuencia COCA"
        gate: [F-LABS-R006, F-LABS-R007, F-LABS-R008, F-LABS-R009, F-LABS-R011, F-LABS-R012]
        then: revisar_recomendaciones

      - id: revisar_recomendaciones
        action: "El usuario revisa las recomendaciones accionables generadas para corregir las brechas de vocabulario del nivel"
        gate: [F-LABS-R027, F-LABS-R028, F-LABS-R029, F-LABS-R030, F-LABS-R031]
        result: success

      - id: confirmar_cobertura
        action: "El usuario confirma que la cobertura de vocabulario del curso es adecuada y no requiere ajustes"
        result: success

      - id: error_sin_catalogo
        action: "ContentAudit informa que no se dispone del catalogo de vocabulario esperado y el analisis no puede ejecutarse"
        result: failure

      - id: error_sin_oraciones
        action: "ContentAudit informa que el curso no contiene oraciones procesadas y el analisis no puede ejecutarse"
        result: failure
```

### Journey[F-LABS-J002] - Investigar lemas completamente ausentes de alta prioridad
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LABS-J002
    name: Investigar lemas completamente ausentes de alta prioridad
    flow:
      - id: revisar_assessment
        action: "El usuario revisa el resultado de la auditoria y observa que el assessment es NEEDS_IMPROVEMENT"
        then: consultar_nivel_critico

      - id: consultar_nivel_critico
        action: "El usuario consulta el detalle de un nivel critico (por ejemplo A1) y observa lemas ausentes de prioridad HIGH"
        outcomes:
          - when: "Hay lemas que no aparecen en ningun nivel del curso"
            then: revisar_completamente_ausentes
          - when: "Los lemas ausentes aparecen en otros niveles pero no en el esperado"
            then: revisar_mal_ubicados

      - id: revisar_completamente_ausentes
        action: "El usuario examina la lista de lemas completamente ausentes, con su ranking COCA, categoria semantica y parte de la oracion"
        then: decidir_accion

      - id: revisar_mal_ubicados
        action: "El usuario examina los lemas que aparecen en otros niveles, verificando donde se encuentran y la distancia al nivel esperado"
        then: decidir_accion

      - id: decidir_accion
        action: "El usuario evalua el impacto comunicativo de cada lema ausente"
        outcomes:
          - when: "Los lemas ausentes son criticos para la comunicacion basica del nivel"
            then: agregar_contenido
          - when: "Los lemas ausentes son relevantes pero no urgentes"
            then: priorizar_para_despues

      - id: agregar_contenido
        action: "El usuario decide agregar oraciones con los lemas ausentes criticos en el nivel correspondiente"
        result: success

      - id: priorizar_para_despues
        action: "El usuario agrega los lemas a una lista de correcciones pendientes, priorizadas por impacto"
        result: success
```

### Journey[F-LABS-J003] - Corregir lemas que aparecen demasiado tarde
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LABS-J003
    name: Corregir lemas que aparecen demasiado tarde
    flow:
      - id: filtrar_too_late
        action: "El usuario filtra los lemas ausentes por tipo APPEARS_TOO_LATE para un nivel especifico (por ejemplo, A2)"
        then: revisar_ubicacion

      - id: revisar_ubicacion
        action: "El usuario revisa en que niveles posteriores aparecen estos lemas y la distancia al nivel esperado"
        outcomes:
          - when: "El lema aparece un solo nivel despues del esperado (por ejemplo, A2 esperado, aparece en B1)"
            then: reforzar_nivel
          - when: "El lema aparece dos o mas niveles despues del esperado (por ejemplo, A1 esperado, aparece en B2)"
            then: introducir_progresion

      - id: reforzar_nivel
        action: "El usuario agrega el lema en su nivel esperado, manteniendo las apariciones posteriores como refuerzo"
        result: success

      - id: introducir_progresion
        action: "El usuario introduce el lema en su nivel esperado y en niveles intermedios para construir una progresion adecuada"
        result: success
```

### Journey[F-LABS-J004] - Revisar el impacto por oracion de los lemas mal ubicados
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LABS-J004
    name: Revisar el impacto por oracion de los lemas mal ubicados
    flow:
      - id: consultar_scores
        action: "El usuario consulta los scores por oracion del analisis de ausencia"
        gate: [F-LABS-R017, F-LABS-R019, F-LABS-R020]
        then: identificar_oraciones_bajas

      - id: identificar_oraciones_bajas
        action: "El usuario identifica las oraciones con scores mas bajos, que contienen lemas significativamente desplazados de su nivel esperado"
        outcomes:
          - when: "Hay oraciones con scores por debajo de 0.8"
            then: examinar_detalle
          - when: "Todas las oraciones tienen scores aceptables (0.8 o superior)"
            then: confirmar_sin_problemas

      - id: examinar_detalle
        action: "El usuario examina que lemas estan desplazados en las oraciones de score bajo y la distancia al nivel esperado"
        gate: [F-LABS-R018]
        outcomes:
          - when: "Los lemas desplazados son de niveles muy distantes (por ejemplo, lemas A1 en oraciones B2)"
            then: decidir_reubicacion
          - when: "Los lemas desplazados estan a un solo nivel de distancia"
            then: aceptar_desviacion

      - id: decidir_reubicacion
        action: "El usuario decide si agregar el lema en un nivel mas cercano al esperado, o si la presencia en el nivel actual se justifica por el contexto tematico"
        result: success

      - id: aceptar_desviacion
        action: "El usuario acepta que la desviacion de un solo nivel es tolerable y no requiere accion"
        result: success

      - id: confirmar_sin_problemas
        action: "El usuario confirma que los scores de oraciones son satisfactorios"
        result: success
```

### Journey[F-LABS-J005] - Planificar mejoras de contenido a partir de las recomendaciones
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LABS-J005
    name: Planificar mejoras de contenido a partir de las recomendaciones
    flow:
      - id: revisar_recomendaciones
        action: "El usuario revisa la lista de recomendaciones generadas por el analisis de ausencia"
        gate: [F-LABS-R027, F-LABS-R028]
        then: filtrar_por_prioridad

      - id: filtrar_por_prioridad
        action: "El usuario filtra las recomendaciones por prioridad para abordar primero las mas criticas"
        outcomes:
          - when: "Hay recomendaciones de prioridad HIGH"
            then: revisar_detalle_critico
          - when: "Solo hay recomendaciones de prioridad MEDIUM o LOW"
            then: revisar_prioridades_menores

      - id: revisar_detalle_critico
        action: "El usuario revisa los lemas afectados, el nivel de esfuerzo estimado y el impacto esperado de cada recomendacion HIGH"
        gate: [F-LABS-R029, F-LABS-R030]
        then: estimar_volumen

      - id: revisar_prioridades_menores
        action: "El usuario revisa las recomendaciones de prioridad MEDIUM y LOW, decidiendo cuales abordar"
        then: estimar_volumen

      - id: estimar_volumen
        action: "El usuario estima el volumen total de trabajo basandose en la cantidad de lemas afectados y los niveles de esfuerzo"
        outcomes:
          - when: "El volumen es manejable"
            then: implementar_directamente
          - when: "El volumen es alto"
            then: crear_plan_iterativo

      - id: implementar_directamente
        action: "El usuario implementa las correcciones y re-ejecuta la auditoria para verificar la mejora"
        result: success

      - id: crear_plan_iterativo
        action: "El usuario crea un plan de mejora iterativo, priorizando las recomendaciones de mayor impacto para abordarlas en multiples ciclos"
        result: success
```

---

## Open Questions

### Doubt[DOUBT-DUAL-THRESHOLDS] - Unificacion de umbrales de tolerancia
**Status**: OPEN

El sistema original tiene dos conjuntos de umbrales de tolerancia inconsistentes:

| Parametro | config.yaml (global) | Configuracion por nivel |
|-----------|---------------------|------------------------|
| A1 | 5 | 0 |
| A2 | 10 | 2 |
| B1 | 15 | 5 |
| B2 | 20 | 8 |

Los umbrales globales (5, 10, 15, 20) son mucho mas permisivos. Los umbrales por nivel (0, 2, 5, 8) son mas estrictos y estan acompanados de porcentajes maximos (0%, 5%, 10%, 15%).

**Propuesta del requerimiento**: Adoptar los umbrales por nivel (0, 2, 5, 8) como los valores oficiales, ya que son mas coherentes con la importancia pedagogica de cada nivel y estan acompanados de umbrales porcentuales que proporcionan una segunda dimension de control. Los umbrales globales podrian ser un residuo de una version anterior.

- [ ] Opcion A: Adoptar los umbrales estrictos por nivel (0, 2, 5, 8) con porcentajes (0%, 5%, 10%, 15%)
- [ ] Opcion B: Adoptar los umbrales permisivos de config.yaml (5, 10, 15, 20)
- [ ] Opcion C: Usar los umbrales por nivel para el assessment y los de config.yaml como "alerta temprana" adicional

### Doubt[DOUBT-PRIORITY-BANDS] - Unificacion de bandas de prioridad COCA
**Status**: OPEN

El sistema original tiene dos conjuntos de bandas de prioridad por ranking COCA inconsistentes:

| Prioridad | config.yaml | critical-absent-lemmas | level-specific |
|-----------|-------------|----------------------|----------------|
| HIGH | < 1000 | 1 - 1000 | < 1000 |
| MEDIUM | < 2000 | 1001 - 2500 | < 3000 |
| LOW | < 3000 | 2501 - 5000 | < 5000 |

Las tres fuentes coinciden en HIGH (< 1000), pero difieren significativamente en MEDIUM y LOW. Config.yaml termina en 3000, mientras que los otros llegan a 5000.

**Propuesta del requerimiento**: Adoptar los rangos mas amplios (1000, 3000, 5000) para cubrir mejor el espectro de vocabulario relevante. Un lema con ranking 3500 es suficientemente comun como para ser evaluado.

- [ ] Opcion A: Adoptar rangos amplios (HIGH < 1000, MEDIUM < 3000, LOW < 5000)
- [ ] Opcion B: Adoptar rangos de config.yaml (HIGH < 1000, MEDIUM < 2000, LOW < 3000)
- [ ] Opcion C: Adoptar rangos mixtos de critical-absent (HIGH < 1000, MEDIUM < 2500, LOW < 5000)

### Doubt[DOUBT-POS-CONSISTENCY] - Riesgo de falsos positivos por inconsistencia POS
**Status**: OPEN

La comparacion por LemmaAndPos (lema + parte de la oracion) puede generar falsos positivos si las etiquetas POS no son consistentes entre el catalogo EVP y el procesamiento linguistico:

1. El EVP original tiene campos POS que a veces estan vacios ("").
2. El catalogo enriquecido usa el procesamiento linguistico para asignar POS, pero el contexto de asignacion puede diferir.
3. Un mismo lema puede recibir POS distintos segun el contexto oracional (por ejemplo, "run" como verbo vs. sustantivo depende de la oracion).

**Pregunta**: Como se manejan las entradas del EVP con POS vacio o ambiguo?

- [ ] Opcion A: Las entradas con POS vacio se comparan solo por lema (ignorando POS), reduciendo falsos positivos
- [ ] Opcion B: Las entradas con POS vacio se excluyen del analisis
- [ ] Opcion C: Las entradas con POS vacio se enriquecen obligatoriamente con POS del procesamiento linguistico antes del analisis

### Doubt[DOUBT-DISCOUNT-FACTOR] - El factor de descuento por distancia deberia ser configurable?
**Status**: OPEN

El factor de descuento por distancia de nivel (0.1 por nivel) esta actualmente hardcodeado. Esto produce:

- 1 nivel de distancia: -0.1 (score 0.9)
- 2 niveles: -0.2 (score 0.8)
- 3 niveles: -0.3 (score 0.7)

**Pregunta**: Deberia hacerse configurable este factor, o el valor 0.1 es suficientemente universal?

- [ ] Opcion A: Mantener hardcodeado (0.1 es un valor razonable y universal)
- [ ] Opcion B: Hacerlo configurable como parametro global
- [ ] Opcion C: Hacerlo configurable y ademas permitir pesos distintos por direccion (penalty mayor si el lema aparece demasiado tarde que demasiado temprano)

### Doubt[DOUBT-FUNCTIONAL-SCOPE] - Alcance del analisis de lemas funcionales criticos
**Status**: OPEN

La R016 define lemas funcionales criticos (pronombres, auxiliares, preposiciones, conectores) que reciben tratamiento especial. La duda es en que niveles se evaluan.

**Pregunta**: Los lemas funcionales criticos se evaluan solo en A1/A2 o en todos los niveles?

- [ ] Opcion A: Solo en A1 y A2 (son imprescindibles en niveles basicos, en niveles avanzados el estudiante ya los domina)
- [ ] Opcion B: En todos los niveles (la ausencia de "be" en cualquier nivel es siempre un problema)
- [ ] Opcion C: En todos los niveles pero con prioridad decreciente (HIGH en A1/A2, MEDIUM en B1, LOW en B2)

### Doubt[DOUBT-COURSE-SCOPE] - Cursos que no cubren todos los niveles CEFR
**Status**: OPEN

El analisis actual asume que el curso cubre los niveles A1 a B2. Sin embargo, podrian existir cursos que solo cubran un subconjunto de niveles (por ejemplo, un curso B1-B2 para estudiantes avanzados).

**Pregunta**: Como se comporta el analisis para cursos que no tienen niveles A1 o A2?

- [ ] Opcion A: El analisis se adapta automaticamente: solo evalua los niveles presentes en el curso, y la definicion de "niveles criticos" se ajusta al nivel mas bajo disponible
- [ ] Opcion B: Si faltan A1 o A2, el assessment no puede ser peor que ACCEPTABLE (ya que no se pueden evaluar niveles criticos)
- [ ] Opcion C: Se requiere que todos los niveles esten presentes para ejecutar el analisis; si falta algun nivel, se reporta un error de configuracion

---

## Configuracion

### Parametros de configuracion unificados

La configuracion propuesta unifica los parametros dispersos en multiples archivos del sistema original:

```yaml
lemmaAbsence:
  # Bandas de prioridad por ranking COCA (ver DOUBT-PRIORITY-BANDS)
  prioritiesByCocaRank:
    HIGH: 1000       # Lemas con ranking 1-1000
    MEDIUM: 3000     # Lemas con ranking 1001-3000
    LOW: 5000        # Lemas con ranking 3001-5000

  # Umbrales de tolerancia por nivel (ver DOUBT-DUAL-THRESHOLDS)
  thresholds:
    A1:
      maxAbsent: 0
      maxPercentage: 0.0
    A2:
      maxAbsent: 2
      maxPercentage: 5.0
    B1:
      maxAbsent: 5
      maxPercentage: 10.0
    B2:
      maxAbsent: 8
      maxPercentage: 15.0

  # Umbrales de alerta por prioridad
  alertThresholds:
    HIGH: 0
    MEDIUM: 3
    LOW: 10

  # Umbrales de assessment global
  assessmentThresholds:
    critical: 10
    acceptable: 5
    optimal: 0

  # Limites de reporte
  reportLimits:
    HIGH: 20
    MEDIUM: 30
    LOW: 50

  # Factor de descuento por distancia de nivel (ver DOUBT-DISCOUNT-FACTOR)
  discountPerLevelDistance: 0.1

  # Tipos de ausencia incluidos en el analisis
  includedAbsenceTypes:
    - COMPLETELY_ABSENT
    - APPEARS_TOO_EARLY
    - APPEARS_TOO_LATE
```
