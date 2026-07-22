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

- **Grupo A - Identificacion de lemas ausentes (R001-R005, R039-R044)**: reglas que describen como se determinan los lemas esperados, presentes y ausentes para cada nivel CEFR, incluyendo como se interpreta el catalogo de vocabulario en toda consulta de nivel (que entradas fijan el nivel de un lema, que entradas solo pueden rebajarlo y que categoria gramatical rige el emparejamiento).
- **Grupo B - Clasificacion del tipo de ausencia (R006-R010)**: reglas que describen como se clasifica cada lema ausente segun donde aparece (o no) en el curso.
- **Grupo C - Prioridad y enriquecimiento (R011-R016)**: reglas que describen como se asigna prioridad basada en frecuencia COCA y como se enriquece cada lema con informacion adicional.
- **Grupo D - Scoring por oracion y vocabulario avanzado (R017-R020, R033-R038)**: reglas que describen como se evalua el impacto de los lemas mal ubicados en cada oracion del curso, incluyendo el vocabulario de nivel superior al rango del curso (C1/C2) y las palabras de contenido fuera de catalogo.
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

La comparacion usa la presencia relajada por lema (R039): un lema esperado se considera presente si el nivel contiene ese lema bajo **cualquier** categoria gramatical. Si el EVP espera "run" como verbo en A2 y el curso tiene "run" como sustantivo en A2, "run" **no** se marca como ausente: el etiquetado automatico de categorias no es lo bastante confiable como para afirmar la ausencia sobre esa distincion (resolucion de DOUBT-POS-FALLBACK-ALCANCE).

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

La exclusion de entradas de frase aplica a **toda consulta de nivel sobre el catalogo**, no solo a la construccion de los conjuntos de lemas esperados: las entradas de frase tampoco participan del emparejamiento de mal-ubicacion (R017/R036, tanto en el match exacto por lema+POS como en el fallback por lema). Ejemplo: la entrada de frase "sleep on it" (C2, lema "sleep") no debe hacer que el lema "sleep" responda nivel C2 en ninguna consulta — sin esta exclusion, oraciones basicas con "sleep" se marcarian falsamente como vocabulario C1/C2 mal ubicado (caso confirmado 2026-07-19 en el primer plan real: ~251 ocurrencias falsas via lemas de entradas phrasal como "sleep", "train", "own", "mind").

El mismo principio se extiende, con su propia regla, a las **formas derivadas y compuestas** cuya palabra encabezado es una palabra nueva construida a partir del lema (por ejemplo "next-door" para el lema "next"): tampoco fijan el nivel del lema base (R040). No caen en esa exclusion las meras variantes flexivas u ortograficas del mismo vocablo (plurales lexicalizados, siglas con distinta caja), que si fijan el nivel del lema (R040). Y la **categoria gramatical** con la que una entrada participa del emparejamiento por par lema+categoria se toma de la fuente EVP, no del etiquetado automatico (R041).

La exclusion de R005 es la cara "nunca inflar" del criterio: una frase de nivel **superior** al del lema no lo sube. Su contracara asimetrica es R042: una entrada de frase de nivel **inferior** al del lema (que suele documentar el sentido mas basico, por ejemplo "live in/at, etc." A1 para el lema "live") **si puede rebajarlo**, nunca subirlo. R005 y R042 son complementarias: R005 bloquea la inflacion, R042 habilita solo la rebaja.

**Error**: N/A (esta regla describe un criterio de filtrado)

### Rule[F-LABS-R040] - Las formas derivadas y compuestas no definen el nivel del lema base
**Severity**: critical | **Validation**: AUTO_VALIDATED

En el catalogo de vocabulario enriquecido, algunas entradas comparten la clave de un lema base aunque su palabra encabezado (headword) este escrita de forma distinta del lema. Hay que distinguir dos situaciones, porque solo una de ellas debe excluirse:

1. **Formas genuinamente derivadas y compuestas**: son **palabras nuevas** construidas a partir del lema por sufijacion derivativa, composicion o guionizacion, como "next-door" (nivel B1, lema "next"), "working" y "working-class" (B1/C1, lema "work"), "planning" (B2/C1, lema "plan") u "open-minded" (C1, lema "open"). Como no contienen espacios, la exclusion de frases multipalabra de R005 no las alcanza. **Estas son las que esta regla excluye.**
2. **Variantes flexivas y ortograficas de la misma palabra**: es el **mismo vocablo** escrito en plural o con distinta caja, no una palabra nueva. Por ejemplo "clothes" (lema "clothe"), "maths" (lema "math"), "trousers" (lema "trouser"), "species" (lema "specie") — plurales lexicalizados cuyo lema del tokenizador quedo en singular —, siglas con distinta caja como "DVD" (lema "dvd"), "CD" (lema "cd"), "TV" (lema "tv"), "PC" (lema "pc"), "OK" (lema "ok"), o caja anomala como "FALSE" (lema "false") y "Ms" (lema "ms"). En todos estos casos la palabra encabezado y el lema son la **misma** palabra; la diferencia es puramente flexiva u ortografica. **Estas entradas SI definen el nivel del lema** y no deben excluirse.

**Criterio de discriminacion**: una entrada **NO** es forma derivada — y por lo tanto **SI** participa de las consultas de nivel del lema — cuando su palabra encabezado, comparada **sin distincion de mayusculas/minusculas**, es igual al lema o difiere de el unicamente por el sufijo de plural regular ("-s" o "-es"). Cualquier otra diferencia (sufijo derivativo, guion, segunda palabra, composicion) la marca como forma derivada y la excluye. Con este criterio "working"->work, "next-door"->next, "planning"->plan y "open-minded"->open **siguen excluidas** (ninguna es igual al lema ni un plural regular de el), mientras que "clothes"->clothe, "maths"->math, "trousers"->trouser y "DVD"->dvd **vuelven a fijar** el nivel del lema.

Las entradas que **si** son formas derivadas **no participan de ninguna consulta de nivel del lema base**: ni de la obtencion de lemas esperados por nivel (R001), ni del match exacto por lema+categoria, ni del fallback por lema de R036. Rige el mismo espiritu y alcance que la exclusion de frases de R005: una entrada cuya palabra encabezado es una **forma derivada o compuesta** del lema (y no una simple variante flexiva u ortografica) no puede fijar el nivel de ese lema. Igual que las frases, estas entradas pueden seguir participando de consultas que **no** son de nivel (por ejemplo, la deteccion de que una entrada es una forma compuesta).

Consecuencias observables:

1. "next" usado como adjetivo en una oracion responde nivel **A2** (las filas adjetivo del EVP), ya no B1 (que provenia de la forma derivada "next-door"): "next-door" **sigue sin definir** el nivel de "next".
2. "work" usado como verbo responde **A1** (la fila verbo del EVP, guideword DO JOB), ya no B1 (que provenia de "working").
3. "clothes" (lema "clothe", nivel A1 en EVP) **vuelve a definir** nivel A1 para el lema "clothe": las oraciones A1 que usan "clothes" dejan de penalizar como palabra fuera de catalogo y recuperan puntuacion plena. Lo mismo aplica a "maths"/math, "trousers"/trouser, "DVD"/dvd y demas variantes flexivas u ortograficas.
4. Si tras excluir una forma derivada el par exacto lema+categoria queda sin entradas, el lema cae al **fallback por lema de R036** (menor nivel entre las categorias restantes). Si el lema entero queda sin entradas de nivel, pasa a **fuera de catalogo** (R034). La exclusion nunca inventa un nivel.

Este defecto, combinado con la categoria gramatical poco confiable (R041), producia falsos positivos de mal-ubicacion: pares poblados **exclusivamente** por formas derivadas eran la unica evidencia de nivel del lema (por ejemplo, el par (next, adjetivo) solo existia via "next-door" B1; el par (work, verbo) solo via "working" B1). Casos confirmados (audit 2026-07-20): de 2967 ocurrencias de mal-ubicacion, 254 (9%) provenian de pares cuyo match respondia un nivel mayor que el minimo real del lema, y 246 de ellas de pares poblados unicamente por formas derivadas. Los lemas mas afectados: "work" verbo (77 ocurrencias), "plan" verbo (42), "next" adjetivo (32), "open" adjetivo (20). Ejemplo end-to-end: la oracion A1 "Let's visit Italy next year." reportaba falsamente "next A1->B1" (el EVP dice A2 adjetivo, y A1 la frase "next week/year").

La primera version de esta regla comparaba palabra encabezado y lema de forma **literal**, y por eso barria tambien las variantes flexivas y ortograficas descritas arriba, que **no** son formas derivadas. Esa comparacion literal introdujo una regresion medida (audit 2026-07-21T23-48-04 contra el pre-R040 de 2026-07-21T20-52-13): en el catalogo, 109 lemas quedaron sin ninguna entrada de palabra que fijara su nivel por culpa de R040, de los cuales 44 eran variantes flexivas o de mayusculas obvias. El efecto observable fue de **52 ocurrencias nuevas de vocabulario fuera de catalogo** que antes resolvian nivel: clothe 0->8, dvd 0->8, trouser 0->8, math 0->6, cd 0->6, jean 0->4, y stair/sunglass/scissor/downstair/belonging/pc 0->2 cada uno. El criterio de discriminacion por flexion y caja elimina esta regresion sin re-admitir ninguna de las formas genuinamente derivadas que motivaron R040.

**Error**: N/A (esta regla describe un criterio de filtrado del catalogo)

### Rule[F-LABS-R041] - La categoria gramatical autoritativa de una entrada es la declarada por EVP
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para el emparejamiento por par **lema + categoria gramatical**, la categoria de una entrada del catalogo se toma de la **categoria declarada por la fuente EVP**, no de la categoria calculada automaticamente (que el catalogo obtiene etiquetando la palabra de forma aislada, sin el contexto de una oracion). La categoria declarada por EVP se traduce al mismo conjunto de etiquetas universales que usan los tokens de las oraciones del curso:

| Categoria declarada por EVP | Etiqueta universal |
|-----------------------------|--------------------|
| noun | NOUN |
| verb | VERB |
| adjective | ADJ |
| adverb | ADV |
| preposition | ADP |
| pronoun | PRON |
| determiner | DET |
| conjunction | CCONJ |
| exclamation | INTJ |
| number | NUM |
| modal verb | AUX |

Cuando la entrada **no declara categoria EVP** (campo vacio; por ejemplo "cattle", "lot"), se conserva como respaldo la categoria calculada automaticamente — el comportamiento actual, sin cambios.

Las categorias EVP "phrase" y "phrasal verb" **no forman parte** de esta traduccion: las entradas multipalabra ya se rigen por la exclusion de frases de R005, y las entradas de una sola palabra con categoria "phrase" (por ejemplo "let's") quedan gobernadas por la rebaja asimetrica de frases de **R042** (solo pueden bajar el nivel del lema, nunca subirlo). Para "let's" (A2) esto reproduce el comportamiento actual: "let" queda en A2. Esto cierra Doubt[DOUBT-PHRASE-MONOPALABRA], resuelta.

Motiva esta regla que la categoria calculada automaticamente es poco confiable: el **26,4% de las entradas de palabra simple** (2913 de 11035) tiene una categoria calculada que **contradice** su propia categoria EVP. Consecuencias observables:

1. "next" (adjetivo EVP A2, con la categoria calculada mal fijada como adverbio) responde A2 cuando se lo consulta como adjetivo, en vez de no encontrar las filas A2.
2. "work" (verbo EVP A1, con la categoria calculada mal fijada como sustantivo) responde A1 cuando se lo consulta como verbo, en vez de no encontrar la fila A1.
3. "wish" (sustantivo EVP B2/C2, con la categoria calculada mal fijada como verbo) deja de inflar el nivel del par (wish, verbo).
4. "last" (adverbio B1/B2 y verbo B1/C1, con la categoria calculada mal fijada como adjetivo) deja de inflar el par (last, adjetivo).

**Error**: N/A (esta regla describe el criterio de categoria gramatical de las entradas del catalogo)

### Rule[F-LABS-R042] - Las entradas de frase solo pueden rebajar el nivel de un lema, nunca subirlo
**Severity**: critical | **Validation**: AUTO_VALIDATED

La exclusion de frases (R005) impide correctamente que una frase de nivel alto **infle** el nivel de un lema (que "sleep on it" C2 haga responder C2 al lema suelto "sleep"). Pero esa misma exclusion total tambien descarta entradas de frase que documentan el sentido **mas basico** del lema, y por eso el sentido A1 canonico queda sin registrar y el lema responde el nivel (mas alto) de sus entradas de palabra sola.

Se corrige con un principio **asimetrico**, extension natural del beneficio de la duda ya escrito en R036 ("nunca se penaliza mas"): una entrada de frase participa de la consulta de nivel de su lema **unicamente para bajarlo** (se toma el minimo), **nunca para subirlo**. Si el nivel de la frase es inferior al que resuelven las entradas de palabra, lo rebaja; si es igual o superior, no tiene efecto. Racional pedagogico: si el EVP espera que un alumno de nivel X produzca la frase, ese alumno ya conoce el lema; en cambio un idiom de C2 no acredita el lema suelto.

Alcance:

1. **Cubre tanto las frases multipalabra como las entradas de una sola palabra con categoria EVP "phrase"/"phrasal verb"** (por ejemplo "let's"). Estas ultimas estaban excluidas de la traduccion de categoria de R041 a la espera de esta decision (Doubt[DOUBT-PHRASE-MONOPALABRA]); R042 las gobierna ahora de forma uniforme como frases-que-solo-bajan.
2. **La rebaja aplica en los dos pasos del emparejamiento de R036**: al match exacto por par lema+categoria **y** al fallback por lema. Si aplicara solo al fallback, el par exacto poblado por entradas de palabra (por ejemplo (live, verbo)=B1) seguiria ganando por precedencia y la correccion no operaria.
3. **La rebaja nunca inventa un nivel**: solo reduce el nivel que ya resuelven las entradas de palabra (contenido) del lema. Si el lema no tiene ninguna entrada de palabra alcanzable, permanece fuera de catalogo (R034); una frase por si sola no lo coloca en catalogo. Asi se preserva el "nunca inflar" incluso para un hipotetico lema que solo existiera como frase.

Consecuencias observables (evidencia: audit 2026-07-21T20-52-49):

| Lema | Entradas de palabra | Entrada de frase que rebaja | Nivel resuelto | Ocurrencias corregidas |
|------|---------------------|-----------------------------|----------------|------------------------|
| live | verbo/adjetivo B1 | "live in/at, etc." A1 | A1 (antes B1) | 69 (el par mas frecuente del audit) |
| last | adjetivo A2 | "last week/year/Monday, etc." A1, "last night" A1 | A1 (antes A2) | 37 |
| next | adjetivo A2 (residuo de iter 1) | "next week/year/Monday, etc." A1 | A1 (antes A2) | 17 |
| sleep | verbo A1 | "sleep on it" C2 (mayor: no baja) | A1 (sin cambio) | -- (caso de R005 intacto) |
| let | verbo B1 | "let's"/"let sb know" A2 ("let alone" C1 no sube) | A2 (sin cambio) | -- (reproduce el comportamiento actual) |

El caso "let" cierra Doubt[DOUBT-PHRASE-MONOPALABRA]: tratada "let's" (A2) uniformemente como frase-que-solo-baja, "let" queda en min(B1, A2)=A2, exactamente el comportamiento actual.

**Error**: N/A (esta regla describe un criterio de resolucion de nivel del catalogo)

### Rule[F-LABS-R043] - Las categorias gramaticales no de contenido rebajan el nivel del lema
**Severity**: critical | **Validation**: AUTO_VALIDATED

La evaluacion de mal-ubicacion solo alcanza tokens que son **palabras de contenido** (NOUN, VERB, ADJ, ADV); las palabras funcionales estan excluidas de la evaluacion (R001, R035). En consecuencia, las entradas del catalogo cuya categoria declarada por EVP **no es de contenido** — preposicion (ADP), pronombre (PRON), determinante (DET), conjuncion (CCONJ), exclamacion (INTJ), numero (NUM) y verbo modal (AUX), segun la traduccion de R041 — **nunca pueden ser emparejadas por el match exacto de un token de contenido**: son inalcanzables. Sin embargo, para ciertas palabras el EVP describe con una categoria no-de-contenido **el mismo uso** que el tokenizador etiqueta como palabra de contenido de nivel mas alto.

Estas entradas no-de-contenido participan de la consulta de nivel del lema **unicamente para bajarlo** (se toma el minimo), nunca para subirlo. Es la misma asimetria de R042 y con la misma garantia de seguridad: como ningun token de contenido podria haberlas emparejado por el par exacto, permitir que rebajen **solo puede eliminar falsos positivos**, jamas crear falsos negativos.

Consecuencias observables (evidencia: audit 2026-07-21T20-52-49):

1. "much" responde **A1** (la fila determiner A1 rebaja la fila adverbio B1). El token "much" etiquetado ADV en "how much...?" deja de marcarse B1. **27 ocurrencias corregidas.**
2. **No afecta** a "snow" (sustantivo A1 / verbo A2, ambas categorias de contenido y sin entrada no-de-contenido): sigue flaggeando el verbo A2 en oraciones A1. Tampoco afecta a "early" ni "late" (adjetivo/adverbio, sin entrada no-de-contenido): siguen flaggeando.
3. 30 lemas del catalogo tienen una entrada no-de-contenido de nivel inferior a su minimo de contenido; la mayoria son palabras funcionales ya excluidas de la evaluacion, de modo que el efecto observable se concentra en los pocos lemas con uso de contenido (much, inside, opposite, past, welcome, please, entre otros).

**Error**: N/A (esta regla describe un criterio de resolucion de nivel del catalogo)

### Rule[F-LABS-R044] - Puente sustantivo-adverbio para expresiones temporales y locativas
**Severity**: major | **Validation**: AUTO_VALIDATED

Para ciertos lemas el EVP y el tokenizador clasifican **el mismo uso** con etiquetas de contenido distintas: el EVP lo lista como adverbio y el tokenizador lo etiqueta NOUN (o viceversa), y como ambas categorias son de contenido, el match exacto responde el nivel de la categoria equivocada. Es tipico de expresiones **temporales y locativas** (tonight, home, today): "See you tonight" se etiqueta NOUN y matchea la fila sustantivo, cuando el uso es el que el EVP llama adverbio.

Se corrige con un **puente acotado sustantivo-adverbio**: cuando un lema tiene entradas de sustantivo **y** de adverbio, la consulta de nivel bajo **cualquiera** de las dos responde el **minimo** de ambas. El puente se limita al par NOUN-ADV: **no** puentea NOUN-VERB (protege "snow") ni ADJ-ADV (protege "early" y "late").

El alcance acotado a NOUN-ADV fue confirmado por el usuario (Doubt[DOUBT-NOUN-ADV-BRIDGE], resuelta). Analisis del catalogo enriquecido (2026-07-21): solo **11 lemas** tienen entradas de sustantivo y adverbio a niveles distintos (round, flat, worse, top, inside, tonight, second, home, half, fine, back). Los enmascaramientos colaterales recaen en sentidos avanzados y raros (round C2, worse C1, el sentido C1 de "home", second C2) de palabras que son basicas en su otra categoria — aceptable bajo el principio de beneficio de la duda que ya gobierna la feature (R036, R039). Ninguno de los positivos verdaderos protegidos (snow, early, late) es del par NOUN-ADV.

Consecuencias observables (evidencia: audit 2026-07-21T20-52-49):

1. "tonight" responde **A1** (adverbio A1 y sustantivo A2 -> min A1). "See you tonight" etiquetado NOUN deja de marcarse A2. **34 ocurrencias corregidas.**
2. "home" responde **A1** (sustantivo A1 y adverbio A2 -> min A1). **16 ocurrencias corregidas.**
3. snow, early y late no se ven afectados (no son del par NOUN-ADV): siguen flaggeando.

**Error**: N/A (esta regla describe un criterio de resolucion de nivel del catalogo)

### Rule[F-LABS-R039] - Presencia de lemas bajo cualquier categoria gramatical
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para el calculo de ausencia por nivel (R003) y la busqueda de lemas ausentes en otros niveles (R004), un lema esperado cuenta como **presente** cuando el nivel contiene ese lema bajo **cualquier** categoria gramatical, no solo bajo la categoria esperada por el catalogo.

Consecuencias observables:

1. Un lema esperado como verbo que solo aparece etiquetado como sustantivo en el nivel **no** se reporta ausente. Ejemplo: "run" esperado como verbo en A2 con "run" presente como sustantivo en A2 -> presente.
2. Dos entradas esperadas del mismo lema con distinta categoria (por ejemplo "run" verbo y "run" sustantivo, ambas esperadas en A2) quedan **ambas cubiertas** por una unica ocurrencia del lema en el nivel.
3. La clasificacion del tipo de ausencia (R006/R007) usa la misma presencia relajada al buscar el lema en otros niveles: si "run" verbo esta ausente de A2 pero "run" (bajo cualquier categoria) aparece en B1, el tipo es APPEARS_TOO_LATE, no COMPLETELY_ABSENT.

Esta regla es la contracara en el Grupo A del fallback por lema de R036 (Grupo D), y elimina el riesgo de falsos "ausentes" por inconsistencia de etiquetas descrito en R015 (el lado de falsos positivos de DOUBT-POS-CONSISTENCY): la distincion por categoria gramatical del catalogo enriquecido no es lo bastante confiable como para fundar una ausencia.

**Error**: N/A (esta regla describe el criterio de presencia)

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

Para cada oracion del curso, el sistema identifica los lemas cuyo nivel esperado (segun EVP) es **superior** al nivel al que pertenece la oracion. Un lema se considera "mal ubicado" cuando es una palabra de un nivel mas avanzado que aparece en una oracion de un nivel inferior.

Ejemplo: una oracion del nivel A1 que contiene el lema "invest" (esperado en B2). El lema esta mal ubicado porque es una palabra demasiado avanzada para A1.

Los lemas cuyo nivel esperado es **igual o inferior** al de la oracion no se consideran mal ubicados. Una palabra de A1 que aparece en B2 es vocabulario basico reutilizado, no un error de contenido. La accion correctiva para esos lemas (si estan ausentes en su nivel esperado) se maneja a nivel de Level mediante los tipos de ausencia TOO_LATE y COMPLETELY_ABSENT (R006).

La evaluacion cubre **todos los niveles del catalogo de vocabulario**, incluidos los superiores al rango del curso (C1 y C2, ver R033). Las palabras de contenido que no figuran en el catalogo se evaluan mediante la heuristica de frecuencia (R034); los nombres propios y tokens no lexicos se excluyen (R035). El emparejamiento entre token y entrada de catalogo usa el fallback por lema definido en R036, con la categoria gramatical de cada entrada tomada de la fuente EVP (R041) y las formas derivadas y compuestas excluidas de las consultas de nivel (R040). Sobre el nivel asi resuelto operan ademas las rebajas asimetricas del catalogo: las entradas de frase (R042) y las de categorias no de contenido (R043) solo pueden bajar el nivel del lema, y el puente sustantivo-adverbio (R044) responde el minimo entre ambas categorias.

**Error**: N/A (esta regla describe un mecanismo de deteccion)

### Rule[F-LABS-R018] - Descuento por distancia de nivel
**Severity**: major | **Validation**: AUTO_VALIDATED

Para cada lema mal ubicado en una oracion (nivel esperado superior al de la oracion), se calcula un **descuento** proporcional a la distancia:

descuento = 0.1 * (nivel esperado del lema - nivel de la oracion)

La distancia se calcula como la diferencia en el orden numerico de los niveles CEFR (A1=1, A2=2, B1=3, B2=4, y para los niveles superiores al curso C1=5, C2=6 — ver R033). Solo se aplica cuando el nivel esperado es mayor que el de la oracion.

Ejemplos:
- Lema B2 en oracion A1: distancia = 4-1 = 3, descuento = 0.3
- Lema B1 en oracion A2: distancia = 3-2 = 1, descuento = 0.1
- Lema C2 en oracion A1: distancia = 6-1 = 5, descuento = 0.5
- Lema A1 en oracion B2: no se penaliza (vocabulario basico reutilizado)

El factor de 0.1 por nivel de distancia es un valor fijo configurable via `getDiscountPerLevel()`.

**Error**: N/A (esta regla describe una formula de calculo)

### Rule[F-LABS-R019] - Calculo de score por oracion
**Severity**: critical | **Validation**: AUTO_VALIDATED

La puntuacion de una oracion se calcula como:

score de la oracion = 1.0 - maximo descuento entre todos los lemas mal ubicados

Se toma el **maximo** descuento (no la suma) porque la presencia de un solo lema severamente mal ubicado es suficiente para penalizar la oracion.

Ejemplos:
- Oracion sin lemas mal ubicados: score = 1.0 (perfecta)
- Oracion A1 con lema B1 (distancia 2): score = 1.0 - 0.2 = 0.8
- Oracion A1 con lemas B1 y B2 (distancias 2 y 3): score = 1.0 - max(0.2, 0.3) = 0.7
- Oracion A1 con lema C2 (distancia 5): score = 1.0 - 0.5 = 0.5
- Oracion B2 con lema A1: score = 1.0 (no se penaliza, vocabulario basico)

El score minimo posible es 0.5 (lema a maxima distancia de 5 niveles: lema C2 en oracion A1, ver R033). Los descuentos de la heuristica de fuera-de-catalogo (R034) compiten en el mismo maximo.

**Error**: "Score calculado fuera de rango [0.0, 1.0] para la oracion {sentenceId}: {score}"

### Rule[F-LABS-R020] - Puntuacion perfecta solo con vocabulario integramente justificado
**Severity**: major | **Validation**: AUTO_VALIDATED

Una oracion recibe la puntuacion maxima (**1.0**) unicamente cuando **cada una** de sus palabras de contenido esta justificada por alguno de estos motivos:

1. Es un lema del catalogo cuyo nivel esperado es **igual o inferior** al nivel de la oracion (aplicando el fallback por lema de R036).
2. Es una palabra fuera de catalogo **frecuente** segun las bandas de R034 (ranking dentro de la banda sin penalidad para el nivel de la oracion).
3. Es un nombre propio u otro token excluido segun R035.

Desconocer el nivel esperado de una palabra **no otorga puntuacion perfecta por defecto**: un lema de catalogo de nivel superior al curso (C1/C2, R033) o una palabra fuera de catalogo infrecuente (R034) penalizan la oracion. Esta regla elimina el punto ciego por el cual las palabras mas avanzadas eran exactamente las invisibles al analisis (caso confirmado 2026-07-16: la quiz A1 "It hails every year." con "hail" de nivel C2 puntuaba 1.0).

Las oraciones cuyo vocabulario esta integramente justificado no contribuyen negativamente al score del nivel.

**Error**: N/A (esta regla describe la condicion del caso base)

### Rule[F-LABS-R033] - Vocabulario de nivel superior al curso (C1/C2)
**Severity**: critical | **Validation**: AUTO_VALIDATED

El catalogo de vocabulario **conserva las entradas de niveles C1 y C2** para la evaluacion de mal-ubicacion, aun cuando los niveles del curso son A1-B2 (hoy esas entradas se descartan al cargar el catalogo, lo que genera el punto ciego de R020). Un lema cuyo nivel esperado es C1 o C2 esta **mal ubicado en cualquier oracion del curso**, porque ningun nivel del curso alcanza ese vocabulario.

La distancia se calcula con la extension del orden numerico de R018 (C1=5, C2=6), con la misma pendiente de descuento (0.1 por nivel). El piso del score por oracion pasa de 0.7 a 0.5.

Ejemplos:
- "hail" (C2) en oracion A1: distancia = 6-1 = 5, descuento = 0.5, score = 0.5
- "possess" (C1) en oracion A2: distancia = 5-2 = 3, descuento = 0.3, score = 0.7
- "shatter" (C2) en oracion B2: distancia = 6-4 = 2, descuento = 0.2, score = 0.8

La extension lineal de la escala existente (en lugar de una penalidad especial mas dura para C1/C2 en niveles iniciales) fue confirmada por el usuario. Ver Doubt[DOUBT-C1C2-SEVERIDAD], resuelta.

**Error**: N/A (esta regla extiende el mecanismo de deteccion de R017/R018)

### Rule[F-LABS-R034] - Palabras de contenido fuera de catalogo: bandas de frecuencia por nivel
**Severity**: critical | **Validation**: ASSUMPTION

Las palabras de contenido (sustantivos, verbos, adjetivos, adverbios) que aparecen en una oracion y **no figuran en el catalogo de vocabulario bajo ninguna categoria gramatical** (es decir, tras agotar el fallback de R036) se evaluan por su **ranking de frecuencia** (dato que cada token del audit ya trae) contra **bandas graduadas que dependen del nivel de la oracion**: cuanto mas basico el nivel, mas frecuente debe ser una palabra desconocida para no penalizar.

| Nivel de la oracion | Sin penalidad (ranking <=) | Descuento leve 0.1 (ranking <=) | Descuento fuerte 0.3 |
|---------------------|----------------------------|----------------------------------|-----------------------|
| A1 | 1000 | 3000 | ranking > 3000 o sin ranking |
| A2 | 2000 | 5000 | ranking > 5000 o sin ranking |
| B1 | 3500 | 8000 | ranking > 8000 o sin ranking |
| B2 | 5000 | 10000 | ranking > 10000 o sin ranking |

Las palabras sin ranking de frecuencia disponible caen siempre en la banda de descuento fuerte (una palabra tan rara que no figura en la lista de frecuencia no puede presumirse adecuada). El descuento compite en el maximo de R019 junto a los descuentos por distancia de nivel. Las exclusiones de R035 (nombres propios, tokens no lexicos) se aplican **antes** de estas bandas.

Ejemplos:
- Palabra fuera de catalogo con ranking 6081 en oracion A1: descuento 0.3 (supera la banda de A1)
- La misma palabra en oracion B2: descuento 0.1 (banda leve de B2)
- Palabra fuera de catalogo con ranking 900: sin penalidad en cualquier nivel

[ASSUMPTION] El esquema de bandas graduadas por nivel fue confirmado por el usuario ("bandas graduadas. Para A1, debajo de 1000, por ejemplo" — Doubt[DOUBT-OOC-HEURISTICA], resuelta). Los valores numericos exactos de la tabla (umbral sin-penalidad de A1 en 1000 anclado por el usuario; el resto de la tabla y los descuentos 0.1/0.3) son propuesta del requerimiento, configurables por nivel.

**Limitacion conocida (aceptada, sin correccion en esta ronda)**: el lematizador puede producir lemas artefacto que no figuran en el catalogo aunque la palabra si este (caso confirmado 2026-07-19: "cookies" lematizado como "cooky" mientras el catalogo lista "cookie"; 19 ocurrencias). Esas ocurrencias caen en la via fuera-de-catalogo y pueden penalizar como falso positivo residual de baja prioridad. Se documenta para no re-diagnosticarlo; una normalizacion de lemas artefacto queda fuera del alcance de esta version.

**Error**: N/A (esta regla describe una heuristica de scoring)

### Rule[F-LABS-R035] - Exclusion de nombres propios y tokens no lexicos
**Severity**: critical | **Validation**: AUTO_VALIDATED

No penalizan ni participan de la evaluacion de mal-ubicacion (R017, R033, R034):

1. Los tokens etiquetados como **nombre propio** por el procesamiento linguistico (nombres de personas, lugares, marcas). Un nombre propio no tiene nivel pedagogico: "Maria vive en London" no contiene vocabulario avanzado.
2. Los tokens que no cumplen los criterios de **token lexico evaluable**: longitud minima de 2 caracteres y todos sus caracteres alfabeticos (sin digitos, sin puntuacion embebida, sin apostrofos). Estos criterios son condicion de **entrada** a la evaluacion de R033/R034: un token que no los cumple no se evalua ni penaliza. Quedan asi excluidos, entre otros: las letras sueltas de etiquetas de dialogo ("b" en oraciones "A: ... B: ..."), los tokens con puntuacion pegada ("i."), y los artefactos de tokenizacion de contracciones ("weren't" cuando llega como token unico con apostrofo).
3. Las **palabras funcionales**, ya excluidas por el filtro general de palabras de contenido (R001).

La etiqueta de nombre propio del procesamiento linguistico es el **unico** criterio de exclusion; no se mantiene lista blanca manual y los errores residuales del etiquetado se toleran (confirmado por el usuario, ver Doubt[DOUBT-NOMBRES-PROPIOS], resuelta).

**Error**: N/A (esta regla describe un criterio de exclusion)

### Rule[F-LABS-R036] - Emparejamiento con fallback por lema (relajacion de la categoria gramatical)
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para la evaluacion de mal-ubicacion por oracion (Grupo D), el emparejamiento entre un token y el catalogo de vocabulario se realiza en dos pasos:

1. **Match exacto** por el par lema + categoria gramatical (LemmaAndPos), como hasta ahora. La categoria gramatical de cada entrada del catalogo es la declarada por la fuente EVP (R041), y las entradas de formas derivadas y compuestas — palabras nuevas construidas a partir del lema, no meras variantes flexivas u ortograficas — no participan (R040).
2. **Fallback por lema**: si no existe entrada para ese par pero el catalogo contiene el lema bajo otras categorias gramaticales, se usa la entrada de **menor nivel esperado** entre las disponibles (beneficio de la duda: nunca se penaliza mas por culpa del fallback). El fallback recorre las mismas entradas admisibles del match exacto: excluye frases (R005) y formas derivadas y compuestas (R040), pero conserva las variantes flexivas y ortograficas del lema (R040).

Sobre el nivel resuelto por cualquiera de los dos pasos anteriores se aplica ademas la **rebaja asimetrica** de las entradas inalcanzables del lema, que solo puede bajar el nivel y nunca subirlo: las entradas de frase (R042) y las de categorias no de contenido (R043). La rebaja opera en **ambos** pasos — de lo contrario un par exacto poblado por entradas de palabra (por ejemplo (live, verbo)=B1) ganaria por precedencia y la rebaja de la frase A1 no operaria. Adicionalmente, para los lemas con entradas de sustantivo y adverbio rige el puente NOUN-ADV de R044 (minimo entre ambas categorias).

Motiva esta regla que el catalogo enriquecido trae categorias gramaticales poco confiables (caso confirmado: "hail" usado como verbo figura en el catalogo solo como sustantivo), lo que con match exacto produce falsos negativos incluso dentro de A1-B2. Solo cuando el lema no existe en el catalogo bajo ninguna categoria se considera fuera de catalogo y pasa a las bandas de frecuencia de R034.

El mismo criterio de relajacion se extiende al calculo de ausencia por nivel del Grupo A mediante R039 (resolucion de Doubt[DOUBT-POS-FALLBACK-ALCANCE]: un lema presente bajo cualquier categoria cuenta como presente).

**Error**: N/A (esta regla describe el mecanismo de emparejamiento)

### Rule[F-LABS-R037] - Los niveles C1/C2 no participan de la cobertura de ausencias
**Severity**: critical | **Validation**: AUTO_VALIDATED

La retencion de las entradas C1/C2 del catalogo (R033) **no altera** los Grupos A, B, C, E y F:

- Los lemas esperados por nivel (R001) siguen siendo unicamente los de los niveles presentes en el curso (A1-B2).
- No existen umbrales de tolerancia, coverage targets, pesos de nivel, alertas ni recomendaciones para C1/C2.
- Un lema C1/C2 jamas se reporta como ausente (COMPLETELY_ABSENT, APPEARS_TOO_LATE ni APPEARS_TOO_EARLY).

Su unica participacion es la deteccion de mal-ubicacion del Grupo D. Sin esta exclusion explicita, las miles de entradas C1/C2 del catalogo aparecerian como "completamente ausentes" y distorsionarian el score global, las alertas y las recomendaciones.

**Error**: N/A (esta regla describe una condicion de exclusion)

### Rule[F-LABS-R038] - Detalle observable de los lemas penalizados por oracion
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada oracion penalizada expone en su diagnostico el detalle de **cada lema que genero descuento**:

| Dato | Descripcion |
|------|-------------|
| Lema | La forma base penalizada |
| Categoria gramatical observada | La etiqueta POS del token en la oracion |
| Motivo | Nivel esperado del catalogo (incluyendo C1/C2), o "fuera de catalogo" con su ranking de frecuencia |
| Descuento aplicado | El valor que compitio en el maximo de R019 |

Para las palabras fuera de catalogo el motivo se expresa como "fuera de catalogo" con el ranking observado; no se les inventa un nivel CEFR. Este detalle alimenta el pipeline existente de planes y revisiones igual que los lemas mal ubicados actuales, de modo que las nuevas penalizaciones producen tareas corregibles.

**Error**: N/A (esta regla describe la estructura del detalle diagnostico)

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
        gate: [F-LABS-R001, F-LABS-R002, F-LABS-R003, F-LABS-R005, F-LABS-R010, F-LABS-R039]
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

### Journey[F-LABS-J006] - Detectar vocabulario avanzado invisible al rango del curso
**Validation**: AUTO_VALIDATED

Cubre el cierre del punto ciego C1/C2 y fuera-de-catalogo: una quiz de nivel inicial con vocabulario avanzado deja de puntuar 1.0, el detalle diagnostico explica el motivo, y la cobertura de ausencias por nivel permanece intacta.

```yaml
journeys:
  - id: F-LABS-J006
    name: Detectar vocabulario avanzado invisible al rango del curso
    flow:
      - id: ejecutar_auditoria
        action: "El usuario ejecuta la auditoria de ausencia de lemas sobre un curso A1-B2 que contiene oraciones con vocabulario C1/C2 y palabras fuera de catalogo"
        gate: [F-LABS-R017, F-LABS-R036]
        then: revisar_scores_oracion

      - id: revisar_scores_oracion
        action: "El usuario revisa los scores por oracion de las quizzes con vocabulario avanzado"
        outcomes:
          - when: "La oracion contiene un lema del catalogo de nivel C1 o C2"
            then: ver_penalidad_c1c2
          - when: "La oracion contiene una palabra de contenido fuera de catalogo e infrecuente"
            then: ver_penalidad_fuera_catalogo
          - when: "Las unicas palabras no justificadas de la oracion son nombres propios o tokens no lexicos"
            then: confirmar_sin_penalidad

      - id: ver_penalidad_c1c2
        action: "El usuario observa que la oracion recibe el descuento por distancia extendida (por ejemplo, quiz A1 con lema C2 puntua 0.5), aunque la categoria gramatical del catalogo difiera de la observada"
        gate: [F-LABS-R033, F-LABS-R019, F-LABS-R036]
        then: examinar_detalle

      - id: ver_penalidad_fuera_catalogo
        action: "El usuario observa que la oracion recibe el descuento de la banda de frecuencia correspondiente al nivel de la oracion (la misma palabra penaliza mas fuerte en A1 que en B2)"
        gate: [F-LABS-R034]
        then: examinar_detalle

      - id: confirmar_sin_penalidad
        action: "El usuario confirma que la oracion puntua 1.0 porque su vocabulario esta integramente justificado"
        gate: [F-LABS-R035, F-LABS-R020]
        result: success

      - id: examinar_detalle
        action: "El usuario examina el detalle diagnostico de la oracion: lema, categoria observada, motivo (nivel C1/C2 o fuera de catalogo con ranking) y descuento aplicado"
        gate: [F-LABS-R038]
        then: verificar_cobertura_intacta

      - id: verificar_cobertura_intacta
        action: "El usuario verifica que las metricas de cobertura por nivel no incluyen lemas C1/C2 como ausentes, y que los lemas presentes bajo otra categoria gramatical ya no figuran como falsos ausentes"
        gate: [F-LABS-R037, F-LABS-R039]
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

### Doubt[DOUBT-C1C2-SEVERIDAD] - Escala de penalidad para vocabulario C1/C2
**Status**: RESOLVED

R033 extiende linealmente la escala de descuento de R018 (C1=5, C2=6, pendiente 0.1), de modo que un lema C2 en A1 produce score 0.5. Alternativamente, el vocabulario por encima del rango del curso podria considerarse un defecto absoluto (nunca deberia estar en ningun nivel del curso) y penalizarse mas fuerte.

**Pregunta**: Como se penaliza un lema C1/C2 en el curso?

- [x] Opcion A: Extension lineal de la escala existente (C1=5, C2=6, descuento 0.1 por nivel; piso 0.5) — propuesta del requerimiento, consistente con R018
- [ ] Opcion B: Descuento fijo maximo (por ejemplo 0.5) para todo C1/C2 en cualquier nivel, sin gradacion por distancia
- [ ] Opcion C: Extension lineal pero con pendiente mayor para la porcion por encima de B2 (por ejemplo 0.15 por nivel a partir de C1)

**Answer**: Resuelto por el usuario el 2026-07-16: Opcion A. R033 queda como esta.

### Doubt[DOUBT-OOC-HEURISTICA] - Umbral y descuento para palabras fuera de catalogo
**Status**: RESOLVED

R034 usaba un umbral de ranking de frecuencia unico (5000) y un descuento fijo (0.2) para palabras de contenido fuera de catalogo. La mezcla real observada (~83 quizzes de A1/A2 afectadas) incluye vocabulario no-EVP genuinamente avanzado pero tambien palabras cotidianas que el catalogo simplemente no lista.

**Pregunta**: Que parametros usa la heuristica de fuera-de-catalogo?

- [ ] Opcion A: Umbral 5000 y descuento fijo 0.2 (propuesta original del requerimiento)
- [x] Opcion B: Bandas de descuento graduadas por ranking — con el matiz del usuario: los umbrales dependen del nivel de la oracion ("bandas graduadas. Para A1, debajo de 1000, por ejemplo")
- [ ] Opcion C: Penalizar solo las palabras sin ranking de frecuencia (las mas raras) y no penalizar las que tienen ranking

**Answer**: Resuelto por el usuario el 2026-07-16: bandas graduadas por ranking con umbrales dependientes del nivel de la oracion (en A1 una palabra desconocida solo queda sin penalizar si es muy frecuente, ranking <= 1000; en niveles superiores el umbral es mas laxo). R034 quedo reescrita con la tabla de bandas por nivel; los valores numericos exactos (salvo el ancla de A1 en 1000, dada por el usuario) son propuesta del requerimiento marcada como ASSUMPTION.

### Doubt[DOUBT-POS-FALLBACK-ALCANCE] - Alcance del fallback por lema
**Status**: RESOLVED

R036 definia el fallback por lema (relajacion de categoria gramatical) solo para el scoring por oracion (Grupo D). El calculo de ausencia por nivel (Grupo A, R003) mantenia el match exacto por LemmaAndPos, donde el mismo problema de categorias poco confiables puede producir falsos "ausentes" ("run" verbo marcado ausente porque el curso solo tiene "run" clasificado como sustantivo).

**Pregunta**: El fallback por lema debe extenderse tambien al calculo de ausencia por nivel?

- [ ] Opcion A: Solo Grupo D por ahora
- [x] Opcion B: Extenderlo tambien al Grupo A (un lema presente bajo cualquier categoria cuenta como presente)
- [ ] Opcion C: Extenderlo al Grupo A solo cuando la entrada del catalogo tiene categoria vacia o unica

**Answer**: Resuelto por el usuario el 2026-07-16: Opcion B. La presencia relajada quedo reglada en R039 (regla propia del Grupo A, con su test explicito), R003 y R036 fueron ajustadas en consecuencia, y la clasificacion de tipo de ausencia (R004/R006/R007) usa la misma presencia relajada.

### Doubt[DOUBT-NOMBRES-PROPIOS] - Criterio de exclusion de nombres propios
**Status**: RESOLVED

R035 excluye los tokens etiquetados como nombre propio por el procesamiento linguistico. El etiquetado automatico puede fallar en oraciones cortas o con mayusculas iniciales ambiguas (inicio de oracion), dejando pasar nombres propios como supuesto vocabulario avanzado o viceversa.

**Pregunta**: Alcanza la etiqueta del NLP como unico criterio?

- [x] Opcion A: Si, solo la etiqueta del NLP (propuesta del requerimiento; los errores residuales se toleran)
- [ ] Opcion B: Etiqueta del NLP + exclusion adicional de todo token capitalizado en posicion no inicial
- [ ] Opcion C: Etiqueta del NLP + lista blanca mantenida manualmente para falsos positivos recurrentes

**Answer**: Resuelto por el usuario el 2026-07-16: Opcion A. R035 queda como esta.

### Doubt[DOUBT-PHRASE-MONOPALABRA] - Tratamiento de las entradas monopalabra con categoria "phrase"
**Status**: RESOLVED

R041 traduce la categoria declarada por EVP al conjunto de etiquetas universales, pero excluye de esa traduccion las categorias "phrase" y "phrasal verb". Las entradas multipalabra con esas categorias ya se filtran por la exclusion de frases de R005. Sin embargo, existen entradas de **una sola palabra** con categoria "phrase" (por ejemplo "let's") que no son alcanzadas por R005 y que en la iteracion 1 resolvian su categoria por la via calculada automaticamente.

**Pregunta**: Como deberian tratarse las entradas monopalabra con categoria "phrase" en las consultas de nivel?

- [ ] Opcion A: Mantener el comportamiento de iteracion 1 (resuelven por la categoria calculada automaticamente)
- [x] Opcion B: Tratarlas como frases, ahora bajo la rebaja asimetrica de R042 (solo pueden bajar el nivel del lema, nunca subirlo)
- [ ] Opcion C: Traducir la categoria "phrase" monopalabra a una etiqueta universal especifica caso por caso

**Answer**: Resuelta en la iteracion 2. R042 gobierna de forma uniforme todas las entradas de frase, multipalabra o de una sola palabra: solo pueden **bajar** el nivel del lema. Para "let's" (categoria "phrase", A2) esto rebaja el lema "let" de su entrada de palabra (verbo B1) a min(B1, A2) = **A2**, que reproduce exactamente el comportamiento observado en iteracion 1. La resolucion es preferible a la via calculada automatica porque, siendo solo-baja, no puede inflar el nivel de un lema por una entrada monopalabra de nivel alto.

### Doubt[DOUBT-NOUN-ADV-BRIDGE] - Puente sustantivo-adverbio para la clase 2 de falsos positivos
**Status**: RESOLVED

Tras R041 las entradas del catalogo quedan bajo su categoria EVP declarada, pero los tokens de las oraciones traen la categoria contextual del tokenizador. Para ciertas palabras **temporales y locativas** ambos sistemas clasifican el mismo uso con etiquetas de contenido distintas: "tonight" es adverbio A1 para el EVP y sustantivo A2, pero el tokenizador etiqueta "See you tonight" como NOUN y matchea la fila A2 (34 ocurrencias A1->A2); "home" es sustantivo A1 / adverbio A2 y el tokenizador oscila (16 ocurrencias A1->A2). La restriccion dura es no re-enmascarar los positivos verdaderos que la iteracion 1 destapo (snow verbo A2, early adjetivo A2, late adverbio A2), donde los dos sistemas distinguen genuinamente dos usos a dos niveles.

**Pregunta**: Como se resuelve la clase 2 de expresiones temporales/locativas (tonight, home)?

- [x] Opcion A: Puente acotado NOUN-ADV (R044): cuando el lema tiene entradas de sustantivo y adverbio, la consulta bajo cualquiera responde el minimo. Corrige tonight (34) y home (16). Analisis del catalogo: solo 11 lemas tienen niveles distintos entre sustantivo y adverbio; los enmascaramientos colaterales son sentidos avanzados raros (round C2, worse C1, home C1, second C2), aceptable bajo el beneficio de la duda. No toca snow (NOUN-VERB) ni early/late (ADJ-ADV). Propuesta del requerimiento.
- [ ] Opcion B: No resolver la clase 2 de tonight/home en esta iteracion (aceptar ~50 ocurrencias de ruido); aplicar solo R042 (clase 1) y R043 (much).
- [ ] Opcion C: Puente restringido a una lista blanca manual de lemas temporales/locativos (tonight, home, today, tomorrow), evitando enmascarar los sustantivos avanzados. Descartada en principio: la feature evita listas blancas manuales (ver resolucion de Doubt[DOUBT-NOMBRES-PROPIOS]).

**Answer**: Resuelto por el usuario el 2026-07-21: Opcion A. Se adopta el puente acotado NOUN-ADV (R044), que queda como regla firme del Grupo A. La clase 1 (R042) y la clase 2a de "much" (R043) son independientes de esta decision.

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

  # Orden numerico extendido para mal-ubicacion (R033; C1/C2 solo Grupo D, ver R037)
  misplacedLevelOrders:
    A1: 1
    A2: 2
    B1: 3
    B2: 4
    C1: 5
    C2: 6

  # Bandas de frecuencia por nivel para palabras fuera de catalogo
  # (R034; DOUBT-OOC-HEURISTICA resuelta: bandas graduadas dependientes del nivel,
  #  ancla del usuario: A1 sin penalidad solo con ranking <= 1000)
  outOfCatalogBands:
    A1: { noPenaltyMaxRank: 1000, lightDiscountMaxRank: 3000 }
    A2: { noPenaltyMaxRank: 2000, lightDiscountMaxRank: 5000 }
    B1: { noPenaltyMaxRank: 3500, lightDiscountMaxRank: 8000 }
    B2: { noPenaltyMaxRank: 5000, lightDiscountMaxRank: 10000 }
  outOfCatalogLightDiscount: 0.1
  outOfCatalogHeavyDiscount: 0.3   # tambien para palabras sin ranking

  # Tipos de ausencia incluidos en el analisis
  includedAbsenceTypes:
    - COMPLETELY_ABSENT
    - APPEARS_TOO_EARLY
    - APPEARS_TOO_LATE
```
