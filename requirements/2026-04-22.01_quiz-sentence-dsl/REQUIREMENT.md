---
feature:
  id: FEAT-QSENT
  code: F-QSENT
  name: Formalizar quizSentence como concepto de primera clase del course-domain
  priority: critical
---

# Formalizar quizSentence como concepto de primera clase del course-domain

El modelo del curso (FEAT-CSTRUCT) tiene `FormEntity.sentenceParts`, una lista de `SentencePartEntity { kind: TEXT|CLOZE, text, options }` que describe la forma estructural del ejercicio (texto plano + huecos con sus respuestas). Esa estructura sirve para renderizar el ejercicio y para evaluar la respuesta del alumno. **No sirve** como representacion compacta para transmitir un ejercicio a un humano (o a un LLM) ni como formato sobre el cual un componente externo pueda proponer una variante (reescribirlo, generarlo, corregirlo).

Hoy existe una conversion implicita y parcial: el mapper de auditoria del sistema (audit-application, privado) concatena los TEXT y toma el primer elemento de `options` del CLOZE, produciendo una oracion plana para NLP. Esa conversion **tiene dos bugs conocidos**: (1) ignora las variantes aceptadas separadas por `|` dentro de las options (toma el primer elemento de la lista pero no resuelve el pipe, emitiendo literales tipo `"is|'s"` en la oracion que consume el analyzer); (2) deja los hints pedagogicos en el texto que procesa el analyzer (por ejemplo `"(loud / loudly)"` aparece tal cual en la oracion plana). Ademas, el pipeline legacy que vivia en una fase de generacion de oraciones manejaba un formato textual tipo DSL (`____ [correct|variant] (hint)`) para el mismo proposito, pero esa DSL no esta formalizada en ningun lugar del dominio — vive como convenciones sueltas en un prompt.

Este requerimiento formaliza el concepto **`quizSentence`**: una representacion textual, compacta y deterministica del ejercicio, bidireccionalmente convertible con la `sentenceParts`, con su gramatica explicita, sus invariantes y sus tests. Es condicion para que otras features (la que propone modificaciones reales de quizzes via LLM, por ejemplo) puedan operar sobre una representacion de quiz que el dominio garantiza correcta.

## Contexto

### Relacion con features existentes

- **FEAT-CSTRUCT** (course-domain): provee el modelo actual de `FormEntity`, `SentencePartEntity`, `SentencePartKind`. Este requerimiento **no modifica** ese modelo; agrega una representacion textual equivalente y las conversiones bidireccionales como funcionalidad del dominio. Tampoco cambia el schema en disco: los `quizzes.json` del curso se siguen persistiendo con la misma forma.
- **Mapper de auditoria** (audit-application): la logica de "sentenceParts -> oracion plana" que hoy vive ahi privada se mueve a course-domain como parte de este requerimiento. El mapper queda como delegador eager hacia course-domain que estampa el resultado en el `AuditableQuiz`. Los dos bugs (variantes y hints) se corrigen en el traspaso.
- **FEAT-LAPS (proxima iteracion)**: depende de este concepto. Una vez que QSENT tenga sus conversiones estables, LAPS podra referenciar la DSL como formato del candidato de quiz. Este requerimiento **no** modifica LAPS; LAPS se actualizara aparte cuando las conversiones de QSENT esten disponibles.
- **FEAT-RCLA (micro-update posterior)**: tambien dependera de QSENT para agregar `quizSentence` al `CorrectionContext`. Fuera de alcance en este requerimiento.

### Alcance deliberado

Este requerimiento es exclusivamente **de dominio**:

- Define la convencion de `SentencePartEntity.options` de forma **explicita** (hoy es implicita).
- Define la gramatica de la DSL `quizSentence`, incluyendo whitespace canonico y hints como elemento formal.
- Define las reglas de conversion bidireccional entre `sentenceParts` y `quizSentence`.
- Define la derivacion unidireccional a la plain sentence como **lista ordenada de strings** (una por variante equivalente, canonica primero, sin hints, con whitespace normalizado).
- Define las invariantes que se deben cumplir (round-trip con equivalencia whitespace-normalizada, equivalencia de plain sentence entre rutas, unidireccionalidad de la plain sentence).
- Exige cobertura de tests contra los quizzes reales del curso como fixtures.

Quedan **fuera** de este requerimiento:

- Cualquier integracion con un LLM u otro generador externo.
- Cualquier cambio al modelo de datos del curso (`SentencePartEntity` no gana campos nuevos; `FormEntity` tampoco).
- Cualquier cambio a como se persiste un quiz en disco.
- Cualquier feature que consuma el `quizSentence` (LAPS, update de RCLA) — se hacen aparte.
- Que el analyzer de auditoria analice la oracion en todas sus variantes. El audit actual analiza una sola oracion por quiz y sigue haciendo exactamente eso consumiendo `list[0]` (la variante canonica). El soporte multi-variante del analyzer es una evolucion futura del audit, no de QSENT.

### Actor principal

No es un operador humano directo: los consumidores del concepto son otros componentes del sistema (el mapper de auditoria, las estrategias de propuesta de revision, eventualmente otras features). El requerimiento formaliza un **contrato de dominio** para que esos consumidores puedan confiar en la representacion.

---

## Reglas de Negocio

### Grupo A - Convencion dentro de `options`

### Rule[F-QSENT-R001] - Todas las entries de `options` y todos los splits por `|` son variantes equivalentes de la misma respuesta correcta
**Severity**: critical | **Validation**: VALIDATED

El campo `options` de un `SentencePartEntity` de tipo CLOZE es una lista donde **todos los elementos** representan **variantes textuales equivalentes de la misma respuesta correcta** del hueco. No hay respuestas aceptadas con sentidos distintos: todas las entries, y todas las sub-variantes separadas por `|` dentro de cada entry, son formas distintas de escribir **la unica respuesta correcta** del CLOZE.

Dos dimensiones expresan el mismo concepto:

- Multiples entries en la lista `options` (por ejemplo `["He's", "He is"]`).
- Una sola entry con `|` internos (por ejemplo `["is|'s"]`).

Ambas formas son equivalentes y el dominio las trata del mismo modo: un conjunto plano de variantes textuales aceptables de una unica respuesta. En los datos actuales del curso, la forma observada mayoritaria es una sola entry con posibles `|` internos; el dominio soporta ambas formas sin preferir una.

**Error**: "El CLOZE de id '{cloze}' expone una lista de options malformada"

### Rule[F-QSENT-R002] - El caracter `|` separa variantes equivalentes dentro de una entry
**Severity**: critical | **Validation**: AUTO_VALIDATED

Dentro de un elemento individual de `options`, el caracter `|` separa **variantes equivalentes** de la misma respuesta. Esta regla es consistente con R001: sea que las variantes aparezcan como entries separadas de la lista o como splits por `|` dentro de una entry, en todos los casos son formas alternativas de escribir la unica respuesta correcta del CLOZE.

**Error**: N/A (esta regla define la semantica del pipe)

### Rule[F-QSENT-R003] - Un TEXT no puede tener options
**Severity**: critical | **Validation**: AUTO_VALIDATED

Un `SentencePartEntity` de tipo TEXT debe tener `options` en `null` o lista vacia. Cualquier otro caso es un dato invalido y las conversiones de este requerimiento deben fallar de forma explicita al encontrarlo.

**Error**: "El TEXT de indice {idx} trae options; los TEXT no pueden tener options"

### Rule[F-QSENT-R004] - Un CLOZE no puede tener options ausentes
**Severity**: critical | **Validation**: AUTO_VALIDATED

Un `SentencePartEntity` de tipo CLOZE debe tener `options` no nulo y no vacio: al menos un elemento debe estar presente. Un CLOZE sin options es un dato invalido y las conversiones deben fallar de forma explicita al encontrarlo.

**Error**: "El CLOZE de indice {idx} no tiene options; un CLOZE requiere al menos una respuesta aceptada"

---

### Grupo B - Gramatica de quizSentence

### Rule[F-QSENT-R005] - Blank se escribe como cuatro guiones bajos
**Severity**: critical | **Validation**: AUTO_VALIDATED

Un **blank** (hueco del ejercicio) se representa en `quizSentence` como exactamente cuatro guiones bajos consecutivos: `____`. Ninguna otra cantidad de guiones bajos (tres, cinco, seis) se considera un blank. Un blank siempre debe aparecer inmediatamente seguido de su bloque de respuesta (ver R006).

**Error**: "Se encontro una secuencia de guiones bajos de largo distinto de cuatro; un blank se escribe '____'"

### Rule[F-QSENT-R006] - Bloque de respuesta sigue al blank entre corchetes
**Severity**: critical | **Validation**: AUTO_VALIDATED

Inmediatamente despues de cada blank (`____`) aparece un **bloque de respuesta** encerrado entre corchetes. El bloque contiene las variantes textuales equivalentes de la unica respuesta correcta del CLOZE (R001), separadas por `|` (R002). Un blank sin bloque de respuesta posterior, un bloque de respuesta sin blank previo, y un bloque de respuesta que no este vinculado al blank que lo precede son todos casos invalidos. El vinculo entre el blank y el bloque admite exactamente un espacio entre ambos segun la regla de whitespace canonico (R010).

**Error**: "Bloque de respuesta ausente o desvinculado del blank que lo antecede en la posicion {pos}"

### Rule[F-QSENT-R007] - Hint pedagogico es un elemento formal de la DSL escrito entre parentesis inline
**Severity**: major | **Validation**: VALIDATED

Un **hint pedagogico** (scaffolding visible al alumno, p. ej. `(to be)`, `(loud / loudly)`, `(drink)`) se representa en `quizSentence` como parentesis inline dentro del tronco de texto. Es un elemento **formalmente reconocido** por la DSL: el parser sabe que un segmento entre parentesis en una posicion de texto plano es un hint, y lo identifica como tal.

Consecuencias:

- El hint **no es** respuesta ni variante.
- Dentro del texto de un TEXT en `sentenceParts`, los hints se preservan tal cual (forman parte de `text` en los datos).
- Dentro del `quizSentence` serializado, los hints se preservan tal cual (son parte del texto que ve el estudiante).
- En la derivacion a plain sentence (Grupo E), los hints **se remueven** porque la plain sentence es texto natural para NLP, no texto con scaffolding.
- La extraccion de hints es una operacion del parser sobre el texto de los TEXT parts; **no cambia el modelo de datos** (no hay un nuevo `SentencePartKind`, no hay un nuevo campo en `SentencePartEntity`).

**Error**: N/A (esta regla define que los parens pertenecen al texto y son extraibles por el parser)

### Rule[F-QSENT-R008] - Un quizSentence valido alterna texto y blank+bloque
**Severity**: critical | **Validation**: AUTO_VALIDATED

Un `quizSentence` valido es un string que, leido de principio a fin, alterna secuencias de texto plano (posiblemente con hints en parens, R007) con la construccion `____ [<bloque>]` (con whitespace canonico, R010). Cualquier otra construccion es invalida: corchetes sin blank previo, blank sin corchete posterior, corchetes o blank aparecidos en medio de un lugar donde la gramatica no los espera. Un `quizSentence` puede estar compuesto por puro texto sin blanks (si el quiz no tiene huecos), pero en ese caso no debe contener los caracteres reservados sin escapar (ver R009).

**Error**: "quizSentence malformado: {descripcion} en la posicion {pos}"

### Rule[F-QSENT-R009] - Los caracteres reservados requieren un mecanismo de escape
**Severity**: major | **Validation**: AUTO_VALIDATED

Los caracteres `[`, `]` y el token `____` son **sintacticamente reservados** por la DSL de `quizSentence`. Si aparecen literalmente en el texto de un TEXT (no como marcadores de la DSL) deben poder distinguirse de su uso sintactico. El mecanismo concreto de escape (backslash, doble caracter, restriccion de que no aparezcan literalmente) es decision de arquitectura (ver DOUBT-ESCAPE-CHARS). Mientras esa decision no exista, las conversiones deben fallar explicitamente si detectan estos caracteres en un contexto ambiguo.

Los caracteres `(`, `)` y `|` **no** son reservados a nivel global: se consideran texto normal cuando aparecen dentro del tronco del texto. Solo `|` dentro de un bloque de respuesta tiene un rol sintactico (separar variantes, R002), y los parentesis dentro del tronco de texto son marcadores de hint (R007).

**Error**: "Caracter reservado encontrado sin escapar en el texto: '{char}' en la posicion {pos}"

### Rule[F-QSENT-R010] - Whitespace canonico en la serializacion, tolerante en el parser, normalizado en la equivalencia
**Severity**: critical | **Validation**: VALIDATED

La DSL tiene una regla explicita de whitespace en tres niveles:

- **Serializacion canonica.** El serializador emite `quizSentence` con un unico espacio entre el texto plano y los marcadores sintacticos (`____`, `[...]`, `(...)`), sin espacios internos colapsables redundantes. Ejemplo canonico: dado un sentenceParts con partes `[TEXT "He", CLOZE options=["is|'s"], TEXT " (to be) great."]`, el `quizSentence` serializado es `He ____ [is|'s] (to be) great.` — espacios limpios, sin pegoteos.
- **Parser tolerante.** El parser acepta multiples espacios entre elementos (colapsa runs de whitespace a un solo espacio durante el parseo). Un `quizSentence` con dos espacios entre el blank y el corchete, o entre un hint y el texto que sigue, no es invalido: se normaliza al parsear.
- **Equivalencia whitespace-normalizada.** La igualdad entre dos `quizSentence` se evalua con whitespace normalizado (runs de espacios comprimidos a uno, trim de bordes). Dos strings que difieran unicamente en whitespace redundante son considerados el mismo `quizSentence` a efectos de las invariantes (Grupo F).

**Error**: N/A (esta regla define el whitespace canonico, la tolerancia y la equivalencia)

---

### Grupo C - Derivacion sentenceParts -> quizSentence

### Rule[F-QSENT-R011] - Cada TEXT se concatena con whitespace canonico
**Severity**: critical | **Validation**: AUTO_VALIDATED

Al serializar un `sentenceParts` a `quizSentence`, el contenido de cada `SentencePartEntity` de tipo TEXT se emite al `quizSentence`, incluidos los parens de hint que formen parte del texto (R007). La serializacion respeta la regla de whitespace canonico (R010): emite un unico espacio entre el texto plano y los marcadores sintacticos del CLOZE (`____`, `[...]`).

Ejemplo: dado `sentenceParts = [TEXT "He", CLOZE options=["is|'s"], TEXT " (to be) great."]`, el `quizSentence` serializado es `He ____ [is|'s] (to be) great.`. Los espacios entre `He` y `____`, entre `____` y `[is|'s]`, y entre `[is|'s]` y `(to be)` son espacios unicos, limpios, aunque en los datos hubiera (o faltara) whitespace redundante en los bordes de los TEXT.

**Error**: N/A (esta regla define el invariante de whitespace canonico en la serializacion)

### Rule[F-QSENT-R012] - Cada CLOZE se serializa como `____ [variantes-unidas-por-pipe]`
**Severity**: critical | **Validation**: AUTO_VALIDATED

Un `SentencePartEntity` de tipo CLOZE se serializa en `quizSentence` como `____ [<variantes>]`, donde `<variantes>` es la union de todas las variantes textuales de la unica respuesta correcta del CLOZE, separadas por `|`. La union se construye a partir de:

- Las entries de `options` en el orden de la lista.
- Dentro de cada entry, las sub-variantes separadas por `|` en su orden original.

Es decir, si `options = ["is|'s"]`, el bloque es `[is|'s]`. Si `options = ["He's", "He is"]`, el bloque es `[He's|He is]`. Si `options = ["'re going to drink|are going to drink"]`, el bloque es `['re going to drink|are going to drink]`. El whitespace entre el blank y el bloque respeta R010 (un espacio).

**Error**: N/A (esta regla define la forma canonica del blank+bloque)

### Rule[F-QSENT-R013] - La serializacion falla ante datos invalidos
**Severity**: critical | **Validation**: AUTO_VALIDATED

La conversion `sentenceParts -> quizSentence` debe fallar de forma explicita si encuentra alguno de estos casos: un TEXT con options poblado (R003), un CLOZE con options nulo o vacio (R004), o cualquier otro dato que contradiga el modelo. La falla es atomica: no se produce un `quizSentence` parcial.

**Error**: "No es posible serializar a quizSentence: {razon}"

---

### Grupo D - Derivacion quizSentence -> sentenceParts

### Rule[F-QSENT-R014] - Parseo secuencial de blank+bloque
**Severity**: critical | **Validation**: AUTO_VALIDATED

Al parsear un `quizSentence` valido, se recorre el string de izquierda a derecha buscando cada ocurrencia de `____ [...]` (con whitespace tolerante, R010). Todo lo que aparece antes del primer match (o antes del proximo match) se interpreta como un TEXT cuyo `text` es ese fragmento (con whitespace normalizado en los bordes segun R010). Cada `____ [<contenido>]` se interpreta como un CLOZE cuyo `options` es la lista de un unico elemento: `[<contenido>]`, preservando los `|` internos como separadores de variantes (R002). Lo que aparece despues del ultimo match hasta el final del string se interpreta como un TEXT final.

**Error**: N/A (esta regla define el algoritmo del parser)

### Rule[F-QSENT-R015] - Los hints se preservan dentro de los TEXT en el parseo
**Severity**: major | **Validation**: AUTO_VALIDATED

Durante el parseo `quizSentence -> sentenceParts`, los hints pedagogicos (`(hint)`, R007) **se preservan** como parte del `text` del TEXT correspondiente, exactamente como aparecen en el `quizSentence` de entrada. El parser reconoce formalmente que un segmento entre parentesis en una posicion de texto plano es un hint (R007), pero no lo extrae a un campo separado del modelo: el hint sigue viviendo dentro de `text` del TEXT. Esto es consistente con que QSENT **no modifica** el modelo de datos del curso.

La posible tematizacion de los hints como concepto propio del modelo (nuevo campo, nuevo kind) queda fuera de alcance y es alcance de otra feature si alguna vez se decide.

**Error**: N/A (esta regla acota explicitamente el parseo)

### Rule[F-QSENT-R016] - El parseo falla ante un quizSentence malformado
**Severity**: critical | **Validation**: AUTO_VALIDATED

Si el `quizSentence` de entrada viola la gramatica (blank sin bloque siguiente, bloque sin blank previo, corchetes no-escapeados en el tronco del texto, `____` de cantidad distinta a cuatro, etc.), la conversion falla de forma explicita. La falla es atomica: no se produce un `sentenceParts` parcial.

**Error**: "No es posible parsear el quizSentence: {razon}"

---

### Grupo E - Derivacion a la plain sentence (lista ordenada de variantes)

### Rule[F-QSENT-R017] - La plain sentence es una lista ordenada de strings, una por variante equivalente
**Severity**: critical | **Validation**: VALIDATED

La plain sentence derivada de un `sentenceParts` es una **lista ordenada de strings** (`List<String>`), donde cada elemento es una **oracion plana completa** que expresa una variante textual de la unica respuesta correcta del CLOZE (R001) sustituida en el texto del ejercicio.

El ordenamiento es deterministico:

- El **primer elemento** es la oracion derivada con la variante **canonica**: la primera sub-variante del primer elemento de `options` (antes del primer `|` del primer entry). Esto preserva el comportamiento del audit actual, que opera sobre una sola oracion por quiz.
- Los elementos siguientes son las oraciones derivadas con las variantes restantes, en el orden de aparicion: primero todas las sub-variantes restantes del primer entry (separadas por `|`), luego cada una de las variantes del segundo entry (con sus sub-variantes en orden), y asi sucesivamente.

En un quiz con multiples CLOZE, el numero de oraciones es el producto de variantes por CLOZE (combinatoria); el orden se define lexicograficamente respecto al orden de CLOZE en `sentenceParts` y al orden de variantes dentro de cada CLOZE. La posicion 0 sigue siendo siempre la combinacion de variantes canonicas (primera sub-variante de primer entry en cada CLOZE).

**Error**: N/A (esta regla define el contrato de la plain sentence)

### Rule[F-QSENT-R018] - La variante canonica es la primera sub-variante del primer entry de options
**Severity**: critical | **Validation**: VALIDATED

Para cada `SentencePartEntity` de tipo CLOZE, la variante **canonica** (la que contribuye al elemento 0 de la lista de plain sentences, R017) es la primera sub-variante del primer elemento de `options`:

- Dado `options = ["is|'s"]`, la variante canonica es `"is"`.
- Dado `options = ["'re going to drink|are going to drink"]`, la variante canonica es `"'re going to drink"`.
- Dado `options = ["He's", "He is"]`, la variante canonica es `"He's"`.

La variante canonica es la que aparece antes del primer `|` del primer entry; si el primer entry no contiene `|`, es el primer entry completo. Esta regla preserva y formaliza el comportamiento que el audit actual tiene **cuando funciona** sobre un CLOZE sin `|` en la primera entry; cuando hay `|`, corrige el bug historico (ver R027).

**Compatibilidad con el consumidor actual.** El audit actualmente analiza una sola oracion por quiz. Ese comportamiento se preserva consumiendo `list[0]`: siempre hay al menos una oracion en la lista (la canonica), y el audit se suscribe a ella. El soporte para que el analyzer analice todas las variantes es evolucion futura del audit, fuera de alcance de QSENT.

**Error**: N/A (esta regla define la derivacion canonica)

### Rule[F-QSENT-R019] - La plain sentence no contiene hints y tiene whitespace normalizado
**Severity**: critical | **Validation**: VALIDATED

Cada elemento de la lista de plain sentences (R017) es **texto natural, NLP-friendly**, sin marcadores de la DSL y sin scaffolding pedagogico:

- **Sin hints.** Al derivar la plain sentence, los hints en parens (R007) se **remueven** del texto de cada TEXT antes de concatenar. La plain sentence no contiene `(to be)`, `(loud / loudly)`, `(drink)` ni ningun otro hint.
- **Whitespace normalizado.** Al remover los hints pueden quedar dobles espacios o espacios al inicio/fin del fragment. La derivacion colapsa runs de espacios a uno solo y hace trim de bordes, de modo que cada elemento de la lista es texto limpio.

Ejemplo: dado `sentenceParts = [TEXT "He ", CLOZE options=["is|'s"], TEXT " (to be) great."]`, la plain sentence canonica (elemento 0 de la lista) es `"He is great."` — sin hint, sin doble espacio, con la variante canonica `is`.

Esta regla corrige uno de los dos bugs del mapper privado actual (dejar los hints en la oracion que consume el analyzer); el otro bug corregido es el de variantes (R018).

**Error**: N/A (esta regla define la forma limpia de la plain sentence)

### Rule[F-QSENT-R020] - Equivalencia de plain sentence entre las dos rutas
**Severity**: critical | **Validation**: AUTO_VALIDATED

La lista de plain sentences derivada directamente de un `sentenceParts` debe ser **identica elemento a elemento** a la lista de plain sentences derivada del `quizSentence` equivalente al mismo `sentenceParts`. Es decir, las dos rutas `sentenceParts -> plain` y `sentenceParts -> quizSentence -> plain` producen la misma lista (mismo numero de elementos, mismo orden, cada elemento igual textualmente tras normalizacion de whitespace y strip de hints). Esta regla es la que ata las Grupos C, D y E entre si.

**Error**: "Inconsistencia de plain sentence entre rutas: '{via_parts}' vs '{via_dsl}'"

---

### Grupo F - Invariantes

### Rule[F-QSENT-R021] - Round-trip sentenceParts -> quizSentence -> sentenceParts preserva la semantica
**Severity**: critical | **Validation**: AUTO_VALIDATED

Dado un `sentenceParts` valido, aplicar la serializacion a `quizSentence` (Grupo C) y luego parsear ese resultado a `sentenceParts` (Grupo D) debe producir una lista **semanticamente equivalente** al original:

- Mismo numero de partes.
- Mismo `kind` de cada parte en el mismo orden.
- Para cada TEXT, mismo `text` con equivalencia **whitespace-normalizada** (R010): runs de espacios comprimidos a uno y trim de bordes; los hints se preservan como parte del texto.
- Para cada CLOZE, las variantes se preservan semanticamente. Dado que la serializacion une todas las variantes (entries + sub-variantes) en un unico bloque con `|` (R012), el parse de ese bloque produce un `options` de un solo elemento cuyos `|` internos enumeran las mismas variantes del original. La equivalencia se evalua sobre el conjunto ordenado de variantes, no sobre la particion en entries (el round-trip puede colapsar multiples entries a una sola entry con mas `|`, lo cual es semanticamente equivalente bajo R001).

**Error**: "Round-trip rompe la equivalencia semantica del sentenceParts en {campo}"

### Rule[F-QSENT-R022] - Round-trip quizSentence -> sentenceParts -> quizSentence preserva el string con whitespace normalizado
**Severity**: critical | **Validation**: AUTO_VALIDATED

Dado un `quizSentence` valido, parsearlo a `sentenceParts` (Grupo D) y volver a serializarlo (Grupo C) debe producir un string **identico al original bajo equivalencia whitespace-normalizada** (R010). Es decir: dos strings que difieran unicamente en whitespace redundante (espacios multiples, espacios en bordes) son considerados el mismo `quizSentence`. Esto garantiza que la DSL es una representacion estable: cualquier `quizSentence` valido puede re-leerse y re-emitirse sin perder informacion, aunque la forma canonica despues del round-trip puede tener whitespace mas limpio que la entrada.

**Error**: "Round-trip rompe la estabilidad textual del quizSentence: '{original}' vs '{recuperado}'"

### Rule[F-QSENT-R023] - La derivacion a plain sentence es estrictamente unidireccional
**Severity**: critical | **Validation**: VALIDATED

La derivacion a plain sentence (Grupo E) es **estrictamente unidireccional y lossy**. No existe una conversion inversa desde una plain sentence (ni desde la lista completa de variantes) hacia un `quizSentence` o un `sentenceParts`, porque en la derivacion se pierde informacion esencial:

- La **ubicacion y cantidad de blanks** se borra (en la plain sentence ya estan rellenos con la variante).
- Los **hints pedagogicos** se eliminan.
- La **estructura TEXT/CLOZE** desaparece (solo queda texto).

Cualquier intento de reconstruir un `quizSentence` o un `sentenceParts` a partir de una o mas plain sentences es **fuera de alcance de QSENT** y corresponde a un problema distinto ("generar un quiz desde cero", no "convertir entre representaciones").

Las unicas conversiones bidireccionales son `sentenceParts <-> quizSentence` (R021, R022).

**Error**: N/A (esta regla declara la unidireccionalidad de la plain sentence)

### Rule[F-QSENT-R024] - Las conversiones fallan de forma atomica
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cualquier falla en cualquiera de las conversiones (sentenceParts -> quizSentence, quizSentence -> sentenceParts, sentenceParts -> plain sentence) es atomica: no se produce un resultado parcial ni "best effort". La conversion termina con un error explicito que indica la causa, y el llamador no recibe un `quizSentence`, un `sentenceParts` o una lista de plain sentences truncados.

**Error**: "Conversion abortada: {razon}"

---

### Grupo G - Migracion del buildSentence actual

### Rule[F-QSENT-R025] - La derivacion a plain sentence es funcionalidad publica del course-domain
**Severity**: critical | **Validation**: AUTO_VALIDATED

La logica de "`sentenceParts` -> plain sentence" debe vivir en course-domain como funcionalidad **publica** del dominio, consumible desde otros modulos del sistema. La forma concreta (metodo sobre la entidad, servicio de dominio, port+adapter) es decision de arquitectura (ver DOUBT-CONVERTER-LOCATION). Hoy esta logica vive privada en el mapper de auditoria de audit-application; este requerimiento exige su traslado.

**Error**: N/A (esta regla define el contrato de visibilidad de la funcionalidad)

### Rule[F-QSENT-R026] - La derivacion nueva corrige los dos bugs del buildSentence actual
**Severity**: critical | **Validation**: VALIDATED

La implementacion nueva de la derivacion a plain sentence corrige explicitamente **dos defectos** del `buildSentence` actual del mapper privado de auditoria:

1. **Bug de variantes.** El mapper actual toma `options.get(0)` completo sin resolver el `|`, emitiendo literales como `"is|'s"` o `"'re going to drink|are going to drink"` en la oracion que consume el analyzer. La implementacion nueva aplica R018: toma la primera sub-variante del primer entry (antes del primer `|`), emitiendo `"is"` o `"'re going to drink"`.
2. **Bug de hints.** El mapper actual deja los hints en el texto que procesa el analyzer, emitiendo literales como `"(loud / loudly)"` o `"(to be)"` dentro de la oracion plana. La implementacion nueva aplica R019: strippea los hints y normaliza whitespace, emitiendo texto natural como `"He is great."`.

No hay periodo de convivencia: a partir de la migracion, el comportamiento correcto (ambas correcciones aplicadas) es el unico comportamiento.

**Error**: N/A (esta regla define los dos fixes que acompanan al traspaso)

### Rule[F-QSENT-R027] - El mapper de auditoria delega de forma eager y estampa el resultado
**Severity**: critical | **Validation**: VALIDATED

Despues de la migracion, el mapper de auditoria en audit-application es un **delegador eager** de una sola linea: invoca la conversion publica del course-domain **una vez por quiz**, obtiene la lista de plain sentences (R017) y estampa el resultado en el `AuditableQuiz` que devuelve. No hay evaluacion lazy, no hay caches implicitos, no hay computos diferidos: el consumidor recibe el resultado ya materializado.

Complementariamente, ningun otro modulo puede reimplementar la logica de conversion: cualquier consumidor que necesite la plain sentence o alguna de las conversiones debe delegar en la funcionalidad publica del course-domain.

**Error**: "Modulo '{modulo}' reimplementa la derivacion a plain sentence: debe delegar en course-domain"

### Rule[F-QSENT-R028] - Los fixtures reales del curso son base de pruebas
**Severity**: major | **Validation**: AUTO_VALIDATED

La cobertura de tests de este requerimiento debe incluir, ademas de casos sinteticos, pruebas ejercidas sobre los **quizzes reales** del curso (`db/english-course/**/quizzes.json`). Los fixtures reales actuan como oraculo: las invariantes (Grupo F) deben cumplirse para la totalidad de los CLOZE del curso. Si al correr el test un fixture real no pasa (por ejemplo, un CLOZE con options vacio oculto en los datos), ese caso se trata como defecto de dato a resolver por separado; el requerimiento asume que los fixtures actuales son base legitima de prueba (ver Assumption 2).

**Error**: N/A (esta regla exige el uso de fixtures reales como parte del set de pruebas)

---

## User Journeys

### Journey[F-QSENT-J001] - Round-trip de dominio sobre un sentenceParts arbitrario
**Validation**: AUTO_VALIDATED

Journey de dominio (sin actor humano): describe el ciclo end-to-end que realizan los consumidores del concepto `quizSentence` cuando parten de un `sentenceParts` del curso, serializan a DSL, parsean de vuelta y derivan la lista de plain sentences. Cubre las decisiones de validez del input y de consistencia entre rutas.

```yaml
journeys:
  - id: F-QSENT-J001
    name: Round-trip de dominio sobre un sentenceParts arbitrario
    flow:
      - id: recibir_parts
        action: "El consumidor entrega al course-domain un sentenceParts a convertir"
        then: validar_parts

      - id: validar_parts
        action: "El course-domain valida que el sentenceParts cumpla la convencion de options (TEXT sin options, CLOZE con options no vacio)"
        gate: [F-QSENT-R001, F-QSENT-R003, F-QSENT-R004]
        outcomes:
          - when: "El sentenceParts cumple la convencion"
            then: serializar_dsl
          - when: "El sentenceParts viola la convencion"
            then: fallar_input_invalido

      - id: serializar_dsl
        action: "El course-domain serializa el sentenceParts a un quizSentence, concatenando TEXT con whitespace canonico y emitiendo cada CLOZE como ____ [variantes-unidas-por-pipe]"
        gate: [F-QSENT-R005, F-QSENT-R006, F-QSENT-R010, F-QSENT-R011, F-QSENT-R012]
        then: parsear_dsl

      - id: parsear_dsl
        action: "El course-domain parsea el quizSentence recien emitido recorriendo los ____ [...] y reconstruyendo los TEXT y CLOZE, preservando los hints dentro de los TEXT"
        gate: [F-QSENT-R008, F-QSENT-R014, F-QSENT-R015]
        then: derivar_plana

      - id: derivar_plana
        action: "El course-domain deriva la lista ordenada de plain sentences del sentenceParts: una por variante equivalente, con la canonica en la posicion 0, sin hints y con whitespace normalizado"
        gate: [F-QSENT-R017, F-QSENT-R018, F-QSENT-R019]
        then: verificar_invariantes

      - id: verificar_invariantes
        action: "El course-domain verifica que el sentenceParts recuperado es semanticamente equivalente al original (whitespace-normalizado), que el quizSentence re-serializado es equivalente al primero bajo la misma normalizacion, y que la lista de plain sentences derivada directamente del sentenceParts coincide elemento a elemento con la derivada via quizSentence"
        gate: [F-QSENT-R020, F-QSENT-R021, F-QSENT-R022, F-QSENT-R023]
        then: entregar_resultado

      - id: entregar_resultado
        action: "El course-domain devuelve al consumidor el quizSentence, el sentenceParts recuperado y la lista de plain sentences, todos consistentes entre si"
        result: success

      - id: fallar_input_invalido
        action: "El course-domain aborta la conversion sin producir quizSentence ni plain sentence, informando al consumidor que el sentenceParts de entrada es invalido"
        gate: [F-QSENT-R013, F-QSENT-R024]
        result: failure
```

---

## Limitaciones de alcance de esta iteracion

- **Solo CLOZE.** Si alguna vez aparecen ejercicios con otras estructuras (por ejemplo, multiple-choice con opciones incorrectas), este requerimiento no los cubre. La DSL y la convencion de `options` son estrictamente para el modelo actual de CLOZE.
- **Sin cambio al schema en disco.** Los `quizzes.json` del curso se siguen persistiendo tal cual. La DSL `quizSentence` es una representacion en memoria / en transito, no un formato de persistencia.
- **Sin cambio al modelo de datos.** `SentencePartEntity` no gana campos nuevos; los hints siguen viviendo dentro del `text` de los TEXT (R015). La formalizacion de hints como elemento DSL (R007) es una operacion del parser sobre ese texto, no una extension del modelo.
- **Sin integracion con ningun consumidor nuevo.** FEAT-LAPS consumira esto aparte; la actualizacion de FEAT-RCLA tambien es aparte. Este requerimiento se limita al contrato de dominio y sus tests.
- **Sin internacionalizacion.** La DSL es sobre la estructura del ejercicio en ingles; la traduccion al espanol es un campo separado del quiz (`translation`) y se mantiene al margen.
- **El audit actual consume solo `list[0]` de la plain sentence.** El audit sigue analizando una sola oracion por quiz (la variante canonica, posicion 0 de la lista). El soporte multi-variante (que el analyzer analice la oracion en todas las variantes equivalentes) es una evolucion futura del audit, **no** alcance de QSENT. QSENT solo se compromete a producir la lista completa y bien formada; el consumo por parte del analyzer lo decide cada consumidor.
- **La plain sentence es unidireccional.** No se puede reconstruir `quizSentence` ni `sentenceParts` a partir de una plain sentence (R023). Cualquier feature que necesite "generar un quiz desde cero" es un problema distinto.

---

## Open Questions

### Doubt[DOUBT-OPTIONS-MULTIPLE] - Semantica de options con mas de un elemento
**Status**: RESOLVED

El schema de `SentencePartEntity.options` admite lista de multiples elementos. La pregunta era si dos entries distintas representaban respuestas aceptadas con sentidos diferentes o variantes equivalentes.

- [x] Opcion A: Todas las entries de `options` y todos los splits por `|` dentro de cada entry son **variantes textuales equivalentes de la misma respuesta correcta** del CLOZE. No hay respuestas con sentidos distintos — ambas dimensiones (entries multiples y pipe-splits) expresan el mismo concepto: formas alternativas de escribir la unica respuesta. Formalizado en R001 y materializado en la serializacion R012 (todas las variantes se unen por `|` en un unico bloque).
- [ ] Opcion B: Multiples respuestas aceptadas distintas.
- [ ] Opcion C: Diferido.

**Answer**: Todas las entries son variantes equivalentes de la misma respuesta. No hay distincion semantica entre multiples entries y pipe-splits dentro de una entry.

### Doubt[DOUBT-ESCAPE-CHARS] - Mecanismo de escape de los caracteres reservados
**Status**: OPEN

Los caracteres `[`, `]` y la secuencia `____` son reservados por la DSL (R009). Si aparecen literalmente en el texto de un TEXT, se requiere un mecanismo de escape para distinguir uso sintactico de uso literal. Opciones tipicas:

- [x] Opcion A: Backslash como caracter de escape (`\[`, `\]`, secuencia equivalente para `____`).
- [ ] Opcion B: Duplicacion del caracter (`[[` para un `[` literal).
- [ ] Opcion C: Entidades con nombre (por ejemplo `&lbracket;`).
- [ ] Opcion D: Declarar que estos caracteres no pueden aparecer literalmente en un text; el dominio valida esa restriccion y falla si aparecen.

**Answer**: Pendiente. Decision de arquitectura. Mientras no exista, las conversiones deben fallar explicitamente al detectar estos caracteres en un contexto ambiguo (R009).

### Doubt[DOUBT-HINT-EXTRACTION] - Los hints son extraibles formalmente por el parser?
**Status**: RESOLVED

El hint pedagogico (`(hint)`) es un elemento **formalmente reconocido** por la DSL (R007). El parser identifica segmentos entre parentesis en posiciones de texto plano como hints. La pregunta era si esto implicaba cambiar el modelo de datos (nuevo campo, nuevo kind) o era puramente una operacion del parser.

- [x] Opcion A: Formalmente extraibles via parser. Los hints se preservan tal cual dentro de `text` en `sentenceParts` y dentro del `quizSentence` serializado. Al derivar la plain sentence (Grupo E), los hints se **remueven** porque son scaffolding pedagogico, no texto natural. **No hay cambio al modelo de datos**: el parser opera sobre el `text` de los TEXT existentes.
- [ ] Opcion B: Exponer un accessor separado en course-domain que liste los hints.
- [ ] Opcion C: Extender el modelo con un `SentencePartKind` dedicado a hints.

**Answer**: Los hints son formalmente extraibles via parser; se preservan en `quizSentence` y en `sentenceParts`; se eliminan al derivar la plain sentence; no hay cambio al modelo de datos. Formalizado en R007, R015 y R019.

### Doubt[DOUBT-CONVERTER-LOCATION] - Donde vive la funcionalidad de conversion dentro de course-domain
**Status**: OPEN (para arquitecto)

R025 exige que las conversiones sean funcionalidad publica del course-domain, pero la forma concreta (metodo sobre la entidad `FormEntity` o `QuizTemplateEntity`, servicio de dominio dedicado, port+adapter, conjunto de funciones puras) es decision de arquitectura.

- [ ] Opcion A: Metodos sobre las entidades del modelo (`FormEntity.toQuizSentence()`, `FormEntity.toPlainSentences()`, etc.).
- [ ] Opcion B: Servicio de dominio dedicado (por ejemplo, un converter separado de las entidades).
- [ ] Opcion C: Port+adapter, con interfaz en course-domain y adapter en algun modulo de infraestructura.
- [ ] Opcion D: Funciones puras expuestas a nivel de paquete.

**Answer**: Pendiente. Decision de arquitectura pura.

### Doubt[DOUBT-MAPPER-DELEGATION] - El mapper de auditoria como delega?
**Status**: RESOLVED

Despues de la migracion, el mapper de auditoria de audit-application queda como **delegador eager**: una sola linea que llama a la conversion publica del course-domain una vez por quiz y estampa el resultado en el `AuditableQuiz`. Sin evaluacion lazy, sin caches implicitos, sin computos diferidos.

- [x] Opcion A: Delegador eager (una llamada por quiz, resultado estampado en `AuditableQuiz`).
- [ ] Opcion B: Evaluacion lazy con supplier / cache.
- [ ] Opcion C: Inlinar la llamada en cada callsite y eliminar el mapper.

**Answer**: El mapper es un delegador eager. Formalizado en R027.

---

## Assumptions

1. **El modelo actual `FormEntity.sentenceParts` es suficiente como base del contrato.** Si el modelo tuviera que crecer (nuevos campos en `SentencePartEntity`, nuevo `kind` de SentencePart, por ejemplo para multiple-choice), eso se trata como un requerimiento aparte; QSENT asume el modelo de FEAT-CSTRUCT tal cual existe hoy. La formalizacion de hints como elemento DSL (R007) es una operacion del parser sobre el `text` existente, no una extension del modelo.
2. **Los quizzes actuales de `db/english-course/**` son datos validos bajo la convencion que se formalizara.** Es decir: la convencion que este requerimiento fija debe ser retrocompatible con los datos actuales. Si al formalizar encontramos quizzes que no cumplen la convencion (por ejemplo, algun CLOZE con options vacio oculto en los datos), el fix del dato es un trabajo separado; el requerimiento asume que los fixtures actuales son base legitima de prueba (R028).
3. **El `buildSentence` actual del mapper de auditoria tiene dos bugs reales pero no criticos hoy.** Por eso su correccion puede hacerse como parte de la migracion (R026) en lugar de ser un hotfix aparte. El operador acepta que los dos bugs (variantes y hints) se arreglan en el paquete de QSENT; no se hace pre-emptive fix en el mapper antes de tener la conversion publica en course-domain.
4. **La DSL se inspira en el pipeline legacy pero no es identica.** El requerimiento decide la gramatica de cero, tomando lo util del legacy (la notacion `____ [correct|variant] (hint)`) sin comprometerse a replicarlo al 100%. Las diferencias (whitespace canonico, hints como elemento formal de la DSL, escape) las decide este requerimiento o se declaran como Doubts.
5. **Los consumidores actuales de la logica de "sentenceParts -> plain sentence" son identificables.** Hoy el unico consumidor conocido es el mapper privado de auditoria en audit-application. Si aparecen otros consumidores ocultos durante la migracion, cada uno debe ajustarse a R027 (delegar en course-domain); no se permiten reimplementaciones paralelas.
6. **El audit actual consume solo `list[0]` de la plain sentence.** QSENT produce la lista completa de variantes equivalentes, pero el audit se suscribe a la posicion 0 (la canonica) y sigue analizando una sola oracion por quiz. La capacidad del analyzer para analizar todas las variantes no se implementa en QSENT y es evolucion futura del audit.
