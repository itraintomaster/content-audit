---
feature:
  id: FEAT-NLP
  code: F-NLP
  name: Evolucion del Tokenizador NLP para Tokenizacion Rica con Datos Linguisticos
  priority: critical
---

# Evolucion del Tokenizador NLP para Tokenizacion Rica con Datos Linguisticos

Evolucionar la interfaz `NlpTokenizer` del sistema ContentAudit para que produzca **tokens enriquecidos** con informacion linguistica completa (lema, parte de la oracion, ranking de frecuencia, indicadores de stop word y puntuacion), reemplazando la tokenizacion actual basada en division por espacios en blanco. Esta evolucion es un prerequisito critico para que el analizador de distribucion COCA (FEAT-COCA) y futuros analizadores linguisticos puedan funcionar, ya que estos requieren datos de frecuencia y categorias gramaticales que la tokenizacion actual no provee.

## Contexto

### Brecha entre el estado actual y las necesidades del sistema

El tokenizador actual del sistema ContentAudit es extremadamente basico: divide el texto por espacios en blanco y retorna una lista de cadenas de texto sin ninguna informacion linguistica asociada. La interfaz actual expone dos operaciones:

- `tokenize(texto)`: retorna una lista de palabras (cadenas de texto simples)
- `countTokens(texto)`: retorna la cantidad de tokens en el texto

Esta simpleza fue suficiente para el primer analizador del sistema (`SentenceLengthAnalyzer`, FEAT-SLENGTH), que solo necesita contar cuantos tokens tiene una oracion. Sin embargo, el proximo analizador critico del sistema -- el analizador de distribucion de vocabulario por frecuencia COCA (FEAT-COCA) -- necesita que cada token tenga asociados datos linguisticos ricos, particularmente el `frequencyRank` (posicion en la lista de frecuencia COCA), el lema (forma base de la palabra) y la parte de la oracion (POS tag).

Segun la regla F-COCA-R006: *"El ranking de frecuencia de cada token proviene de un procesamiento linguistico previo (lematizacion). El analizador utiliza el `frequencyRank` ya asociado a cada token; no consulta directamente el corpus COCA ni realiza busquedas de frecuencia."* Esto significa que el tokenizador NLP es el responsable de entregar tokens ya enriquecidos con toda la informacion que los analizadores necesitan.

### El modelo de datos objetivo (AuditableQuiz)

Actualmente, el modelo `AuditableQuiz` solo almacena la oracion como texto y el conteo de tokens (un entero). Con la evolucion del tokenizador, cada quiz debera tener asociada una lista de tokens enriquecidos que contengan la informacion linguistica completa. Esto impacta el proceso de mapeo que convierte los datos del curso en datos auditables (`CourseToAuditableMapper`), que actualmente solo invoca `countTokens` pero debera invocar el tokenizador enriquecido.

### El sistema original de referencia

El sistema original de analisis de contenido (previo a ContentAudit) utilizaba un contenedor Docker con SpaCy (modelo `en_core_web_sm`, version 3.7.2) para procesar las oraciones. El script Python (`sample_processor.py`) realizaba las siguientes funciones:

1. **Lematizacion**: convierte cada palabra a su forma base (ej: "running" -> "run", "was" -> "be")
2. **Etiquetado gramatical (POS tagging)**: asigna a cada token su categoria gramatical usando las etiquetas de Universal Dependencies (NOUN, VERB, ADJ, ADV, PRON, DET, ADP, etc.)
3. **Busqueda de frecuencia COCA**: consulta los datos de frecuencia del corpus COCA (`lemmas_20k_words.txt`) usando el lema y la categoria gramatical para obtener el ranking de frecuencia
4. **Deteccion de stop words**: identifica palabras funcionales comunes (the, is, a, etc.)
5. **Deteccion de puntuacion**: identifica tokens de puntuacion
6. **Deteccion de tokens alfabeticos**: distingue tokens compuestos solo por letras

Cada token procesado por el sistema original producia la siguiente informacion: texto original, lema, etiqueta POS (Universal Dependencies), etiqueta detallada (Penn Treebank), ranking de frecuencia COCA, indicador de palabra alfabetica, indicador de stop word, y opcionalmente relacion de dependencia sintactica y nivel CEFR.

### Consumidores de los tokens enriquecidos

La tokenizacion enriquecida no solo beneficia a FEAT-COCA. Multiples funcionalidades presentes y futuras del sistema dependen de tokens con informacion linguistica:

| Consumidor | Datos que necesita del token |
|-----------|----------------------------|
| Analizador COCA (FEAT-COCA) | frequencyRank, lema |
| ContentWordFilter (filtro de palabras de contenido) | posTag, isStop |
| Futuros analizadores de ausencia de lemas | lema, posTag, frequencyRank |
| Futuros analizadores de recurrencia de lemas | lema, posTag |

El ContentWordFilter es un componente critico que determina si una palabra es "de contenido" (sustantivos, verbos, adjetivos, adverbios) o "funcional" (articulos, preposiciones, conjunciones). Esta distincion se basa directamente en la etiqueta POS del token:

| Etiqueta POS | Tipo | Es palabra de contenido? |
|-------------|------|--------------------------|
| NOUN | Sustantivo | SI |
| VERB | Verbo | SI |
| ADJ | Adjetivo | SI |
| ADV | Adverbio | SI |
| PROPN | Nombre propio | Configurable |
| NUM | Numero | Configurable |
| ADP | Preposicion | NO |
| AUX | Auxiliar | NO |
| CCONJ | Conjuncion coordinante | NO |
| DET | Determinante | NO |
| INTJ | Interjection | NO |
| PART | Particula | NO |
| PRON | Pronombre | NO |
| SCONJ | Conjuncion subordinante | NO |
| PUNCT | Puntuacion | NO |
| SYM | Simbolo | NO |
| X | Otro | NO |

### Analisis de estrategias de procesamiento NLP

Para proveer tokenizacion enriquecida, el sistema necesita un motor de procesamiento de lenguaje natural. Se analizan cuatro estrategias posibles:

**Estrategia 1 - Contenedor Docker con SpaCy (enfoque original)**

El sistema original ejecutaba SpaCy dentro de un contenedor Docker. Java comunicaba las oraciones escribiendo un archivo JSON de entrada, invocaba el contenedor, y leia el archivo JSON de salida.

| Aspecto | Evaluacion |
|---------|-----------|
| Calidad linguistica | Excelente: SpaCy con en_core_web_sm es una solucion madura |
| Configuracion | Requiere Docker instalado y la imagen construida (~500 MB) |
| Rendimiento | Overhead de inicio del contenedor (~2-3 segundos); procesamiento ~10-50 oraciones/seg |
| Mantenimiento | La imagen debe reconstruirse si cambia el script o las dependencias |
| Portabilidad | Limitada: requiere Docker en el entorno de ejecucion |

**Estrategia 2 - Ejecucion directa de Python (recomendada)**

En lugar de usar Docker, Java invoca el script Python directamente usando un mecanismo de ejecucion de procesos del sistema operativo. El script ya existe (`sample_processor.py`) y maneja toda la logica: carga del modelo SpaCy, lematizacion, etiquetado POS, y busqueda de frecuencia COCA. La comunicacion se realiza mediante archivos JSON (entrada y salida) o mediante entrada/salida estandar del proceso.

| Aspecto | Evaluacion |
|---------|-----------|
| Calidad linguistica | Identica a Docker: mismo script, mismo modelo SpaCy |
| Configuracion | Requiere Python 3.11+ y SpaCy instalados en el host |
| Rendimiento | Sin overhead de Docker; inicio mas rapido del proceso Python |
| Mantenimiento | Mas simple: el script se edita directamente sin reconstruir imagenes |
| Portabilidad | Requiere Python + SpaCy en el entorno, pero esto es mas comun que Docker |

**Estrategia 3 - Java puro con catalogo enriquecido precalculado**

Usar el archivo `enriched_vocabulary_catalog.json` (15.696 entradas pre-generadas con lema, POS, frequencyRank) como fuente de datos para una busqueda en memoria sin necesidad de SpaCy ni Python.

| Aspecto | Evaluacion |
|---------|-----------|
| Calidad linguistica | Limitada: solo funciona para palabras que estan en el catalogo |
| Configuracion | Minima: solo requiere el archivo JSON |
| Rendimiento | Excelente: busqueda en memoria, sin procesos externos |
| Mantenimiento | El catalogo debe regenerarse si cambia el vocabulario |
| Cobertura | Solo 15.696 palabras; no puede lematizar formas flexionadas nuevas |

Esta estrategia es insuficiente como solucion principal porque no puede procesar palabras que no estan en el catalogo precalculado, ni puede lematizar correctamente formas flexionadas arbitrarias. Sin embargo, podria servir como **cache o fallback**.

**Estrategia 4 - Microservicio Python con SpaCy**

SpaCy corriendo como un servicio persistente que recibe solicitudes y retorna resultados procesados. El servicio se inicia una vez y permanece activo durante toda la sesion de auditoria.

| Aspecto | Evaluacion |
|---------|-----------|
| Calidad linguistica | Identica a las opciones 1 y 2 |
| Configuracion | Requiere Python + SpaCy + un framework de servicio |
| Rendimiento | Excelente despues del inicio: el modelo SpaCy se carga una sola vez |
| Mantenimiento | Complejidad adicional del servicio y gestion de ciclo de vida |
| Portabilidad | Similar a la ejecucion directa |

**Recomendacion: Estrategia 2 (Ejecucion directa de Python)**

Se recomienda la ejecucion directa de Python como estrategia principal por las siguientes razones:

1. El script `sample_processor.py` ya existe y esta probado
2. Elimina la dependencia de Docker, simplificando el entorno de desarrollo y ejecucion
3. Mantiene la calidad linguistica completa de SpaCy
4. Es mas rapido que Docker al eliminar el overhead del contenedor
5. Es mas facil de depurar y mantener que un contenedor o microservicio

La estrategia 3 (catalogo precalculado) podria usarse como **complemento** para optimizar el rendimiento mediante cache: si un token ya esta en el catalogo, se usa la informacion precalculada sin invocar a Python.

### Pipeline de enriquecimiento de tokens

El flujo de datos para enriquecer una oracion es el siguiente:

```
Oracion (texto plano)
  |
  v
Procesador NLP (SpaCy via Python)
  |
  +-- Tokenizacion: divide la oracion en tokens
  +-- Lematizacion: obtiene la forma base de cada token
  +-- POS Tagging: asigna la categoria gramatical (Universal Dependencies)
  +-- Deteccion de stop words
  +-- Deteccion de puntuacion
  |
  v
Busqueda de frecuencia COCA
  |
  +-- Mapeo de etiqueta POS: SpaCy (Universal Dependencies) -> COCA (codigos propios)
  +-- Busqueda por lema + POS mapeado en los datos COCA
  +-- Fallback: busqueda solo por lema si no se encuentra por lema+POS
  +-- Fallback: busqueda por forma exacta de la palabra
  |
  v
Token enriquecido
  |
  +-- text: texto original ("running")
  +-- lemma: forma base ("run")
  +-- posTag: categoria gramatical ("VERB")
  +-- frequencyRank: ranking COCA (245)
  +-- isStop: indicador de stop word (false)
  +-- isPunct: indicador de puntuacion (false)
```

### Mapeo de etiquetas POS: SpaCy (Universal Dependencies) a COCA

Un aspecto critico del procesamiento es el mapeo entre las etiquetas POS que usa SpaCy (basadas en el esquema Universal Dependencies) y los codigos POS que usa el corpus COCA para organizar sus datos de frecuencia. Sin este mapeo, la busqueda de frecuencia por lema+POS no puede realizarse correctamente.

El mapeo completo, extraido del script original `sample_processor.py`, es el siguiente:

| Etiqueta SpaCy (UD) | Codigo COCA | Significado SpaCy | Significado COCA |
|---------------------|-------------|-------------------|-----------------|
| NOUN | n | Sustantivo comun | Noun |
| PROPN | n | Nombre propio | Noun |
| VERB | v | Verbo | Verb |
| AUX | v | Verbo auxiliar | Verb |
| ADJ | j | Adjetivo | Adjective |
| ADV | r | Adverbio | Adverb |
| PRON | p | Pronombre | Pronoun |
| DET | a | Determinante | Article/Determiner |
| ADP | i | Preposicion | Preposition |
| CONJ | c | Conjuncion | Conjunction |
| CCONJ | c | Conjuncion coordinante | Conjunction |
| SCONJ | c | Conjuncion subordinante | Conjunction |
| NUM | m | Numeral | Number |
| PART | t | Particula | Particle |
| PRT | t | Particula | Particle |
| INTJ | u | Interjection | Interjection |
| SYM | x | Simbolo | Other |
| PUNCT | x | Puntuacion | Other |
| SPACE | x | Espacio | Other |
| X | x | Otro | Other |

Este mapeo es necesario porque la busqueda de frecuencia se realiza por **lema + codigo POS COCA**, no por lema + etiqueta SpaCy. Por ejemplo, para buscar la frecuencia de "run" como verbo, la clave de busqueda es `run#v`, no `run#VERB`.

### Estructura de los datos de frecuencia COCA (lemmas_20k_words.txt)

Los datos de frecuencia provienen del archivo `lemmas_20k_words.txt`, que contiene las 20.000 lemas mas frecuentes del ingles segun el corpus COCA (Corpus of Contemporary American English). El formato es TSV (valores separados por tabulacion) con la siguiente estructura:

| Columna | Tipo | Descripcion |
|---------|------|-------------|
| lemRank | Entero | Ranking del lema (1 = mas frecuente) |
| lemma | Texto | Forma base (lema) |
| PoS | Caracter | Codigo POS del COCA (n, v, j, r, p, a, i, etc.) |
| lemFreq | Entero | Frecuencia total del lema en el corpus |
| wordFreq | Entero | Frecuencia de esta forma especifica |
| word | Texto | Forma especifica de la palabra |

Cada lema tiene multiples filas, una por cada forma flexionada. Por ejemplo, el lema "be" (ranking 2) tiene formas: is, was, 's, be, are, were, 're, been, 'm, being, am, s.

Las primeras 8 lineas del archivo son comentarios/metadatos y la linea 9 es la cabecera de columnas. Al parsear el archivo, estas lineas deben omitirse.

### Estrategia de busqueda de frecuencia

El script original implementa una estrategia de busqueda en tres niveles de especificidad, en orden de prioridad:

1. **Busqueda por forma exacta de la palabra**: se busca la palabra tal como aparece en el texto (en minusculas). Si se encuentra en los datos COCA, se retorna su ranking de lema.
2. **Busqueda por lema + POS COCA**: se construye la clave `lema#posCoca` (ej: `run#v`) y se busca. Si hay multiples entradas para la misma clave, se retorna la de mejor (menor) ranking.
3. **Busqueda solo por lema (fallback)**: se busca solo por el lema sin considerar el POS. Esto es un fallback para cuando el POS mapeado no coincide con el del COCA.

Si ninguna de las tres estrategias encuentra resultado, el token queda sin ranking de frecuencia (`frequencyRank` nulo).

### Catalogo de vocabulario enriquecido como complemento

El archivo `enriched_vocabulary_catalog.json` contiene 15.696 entradas precalculadas que combinan datos del English Vocabulary Profile (EVP), el corpus COCA y SpaCy. Cada entrada incluye: palabra, nivel CEFR, lema, etiqueta POS de SpaCy, ranking de frecuencia, indicador de stop word, indicador de frase, y otros campos.

Este catalogo podria usarse como **complemento** al procesamiento con SpaCy:

- **Como cache**: antes de invocar a SpaCy, verificar si la palabra ya esta en el catalogo. Si esta, usar los datos precalculados. Si no, procesar con SpaCy.
- **Como enriquecimiento**: agregar datos del catalogo (como el nivel CEFR y el tema) que SpaCy no provee directamente.

Sin embargo, el catalogo **no puede ser la fuente primaria** porque: (a) solo contiene 15.696 palabras del EVP, no todas las palabras posibles del ingles, y (b) no puede lematizar formas flexionadas que no estan explicitamente listadas.

---

## Reglas de Negocio

Las reglas se organizan en seis grupos:

- **Grupo A - Modelo NlpToken (R001-R005)**: datos que cada token enriquecido debe contener.
- **Grupo B - Evolucion de la interfaz NlpTokenizer (R006-R010)**: como cambia la interfaz del tokenizador y compatibilidad con analizadores existentes.
- **Grupo C - Busqueda de frecuencia COCA (R011-R016)**: como se obtiene el ranking de frecuencia de cada token.
- **Grupo D - Integracion con SpaCy (R017-R022)**: como se invoca el procesamiento NLP desde Python/SpaCy.
- **Grupo E - Cache de resultados (R023-R027)**: como se cachean los resultados para evitar reprocesamiento.
- **Grupo F - Manejo de errores y fallbacks (R028-R033)**: que ocurre cuando el procesamiento falla o no se encuentra informacion.

---

### Grupo A - Modelo NlpToken

### Rule[F-NLP-R001] - Campos obligatorios del token enriquecido
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada token producido por el tokenizador enriquecido debe contener, como minimo, los siguientes campos:

| Campo | Tipo | Descripcion | Ejemplo |
|-------|------|-------------|---------|
| text | Texto | Texto original del token tal como aparece en la oracion | "running" |
| lemma | Texto | Forma base (lematizada) del token | "run" |
| posTag | Texto | Etiqueta de parte de la oracion (Universal Dependencies) | "VERB" |
| frequencyRank | Entero (nullable) | Ranking de frecuencia COCA basado en el lema | 245 |
| isStop | Booleano | Indica si el token es una stop word | false |
| isPunct | Booleano | Indica si el token es un signo de puntuacion | false |

Estos seis campos son el **minimo indispensable** para que los analizadores actuales y planeados del sistema puedan funcionar. `frequencyRank` es nullable porque no todos los tokens tienen entrada en el corpus COCA (ver R028).

**Error**: "Token incompleto: falta el campo obligatorio '{campo}' para el token '{text}'"

### Rule[F-NLP-R002] - Campos opcionales del token enriquecido
**Severity**: minor | **Validation**: AUTO_VALIDATED

Ademas de los campos obligatorios, el token enriquecido puede incluir los siguientes campos opcionales que provee el procesamiento SpaCy:

| Campo | Tipo | Descripcion | Ejemplo |
|-------|------|-------------|---------|
| tag | Texto | Etiqueta detallada Penn Treebank | "VBG" |
| isAlpha | Booleano | Indica si el token esta compuesto solo por letras | true |
| dep | Texto | Relacion de dependencia sintactica | "nsubj" |

Estos campos no son requeridos por ninguno de los analizadores actualmente planificados, pero pueden ser utiles para futuros analizadores o para diagnostico. Su inclusion es opcional y no debe bloquear el funcionamiento del sistema si estan ausentes.

**Error**: N/A (esta regla describe campos opcionales)

### Rule[F-NLP-R003] - Lematizacion produce la forma base del token
**Severity**: critical | **Validation**: AUTO_VALIDATED

El campo `lemma` de cada token debe contener la **forma base** (forma canonica) de la palabra, normalizada a minusculas. La lematizacion reduce las formas flexionadas a su raiz lexica. Ejemplos:

| Texto original | Lema esperado | Explicacion |
|---------------|---------------|-------------|
| running | run | Forma verbal flexionada -> infinitivo |
| was | be | Verbo conjugado -> infinitivo |
| children | child | Plural irregular -> singular |
| better | well (o good) | Comparativo -> forma base |
| cats | cat | Plural regular -> singular |
| She | she | Pronombre -> minuscula |
| quickly | quickly | Adverbio (sin forma base distinta) |

La calidad de la lematizacion depende del modelo NLP utilizado (SpaCy `en_core_web_sm`). El tokenizador delega esta tarea al motor de procesamiento linguistico y retorna el resultado tal como lo produce el modelo.

**Error**: "Error de lematizacion: el token '{text}' no pudo ser lematizado"

### Rule[F-NLP-R004] - Etiquetas POS usan el esquema Universal Dependencies
**Severity**: critical | **Validation**: AUTO_VALIDATED

Las etiquetas de parte de la oracion (`posTag`) deben seguir el esquema **Universal Dependencies** (UD), que es el esquema que utiliza SpaCy por defecto. Las etiquetas validas son:

NOUN, VERB, ADJ, ADV, PROPN, NUM, ADP, AUX, CCONJ, DET, INTJ, PART, PRON, SCONJ, PUNCT, SYM, SPACE, X

Este esquema es el estandar en linguistica computacional moderna y es el que esperan los analizadores del sistema. Cualquier etiqueta que no pertenezca a este conjunto debe reportarse como anomalia.

El uso del esquema Universal Dependencies es critico porque el ContentWordFilter (que distingue palabras de contenido de palabras funcionales) opera directamente sobre estas etiquetas para determinar si un token es un sustantivo, verbo, adjetivo o adverbio.

**Error**: "Etiqueta POS desconocida '{posTag}' para el token '{text}'. Se esperaba una etiqueta del esquema Universal Dependencies"

### Rule[F-NLP-R005] - El token enriquecido reemplaza a la representacion de cadena de texto
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los tokens enriquecidos reemplazan la representacion actual de tokens como cadenas de texto simples. Donde antes el sistema trabajaba con una lista de cadenas de texto (["She", "likes", "cats"]), ahora trabaja con una lista de tokens enriquecidos, cada uno con toda la informacion linguistica descrita en R001 y R002.

Este reemplazo afecta al modelo `AuditableQuiz`, que actualmente solo almacena la oracion y un conteo de tokens (entero). Con la evolucion, cada quiz debera tener asociada su lista completa de tokens enriquecidos. El conteo de tokens sigue siendo derivable (es simplemente la cantidad de tokens en la lista).

**Error**: N/A (esta regla describe un cambio de modelo)

---

### Grupo B - Evolucion de la interfaz NlpTokenizer

### Rule[F-NLP-R006] - Nueva operacion de tokenizacion enriquecida
**Severity**: critical | **Validation**: AUTO_VALIDATED

La interfaz del tokenizador NLP debe ofrecer una nueva operacion que, dada una oracion (texto plano), retorne una lista de tokens enriquecidos con toda la informacion linguistica descrita en R001. Esta es la operacion principal que usaran los analizadores que requieren datos linguisticos.

La nueva operacion reemplaza funcionalmente a la operacion `tokenize` actual (que retorna cadenas de texto), aunque la operacion anterior podria mantenerse temporalmente por compatibilidad.

**Error**: "Error al tokenizar la oracion: '{oracion}' - {detalle del error}"

### Rule[F-NLP-R007] - Compatibilidad con analizadores existentes que usan conteo de tokens
**Severity**: major | **Validation**: AUTO_VALIDATED

El analizador de longitud de oraciones (`SentenceLengthAnalyzer`, FEAT-SLENGTH) actualmente utiliza el conteo de tokens (`tokenCount`) que se calcula durante el mapeo del curso. Este analizador no necesita tokens enriquecidos; solo necesita saber cuantos tokens tiene la oracion.

Con la evolucion del tokenizador, el conteo de tokens se puede derivar de la lista de tokens enriquecidos (contando los elementos de la lista). Por lo tanto, no se requiere mantener la operacion `countTokens` como operacion independiente, siempre que el conteo siga estando disponible a traves del modelo de datos.

Los analizadores existentes no deben romperse con la evolucion del tokenizador. Si un analizador solo necesita el conteo, debe poder obtenerlo sin procesar la lista completa de tokens.

**Error**: N/A (esta regla describe un requisito de compatibilidad)

### Rule[F-NLP-R008] - Procesamiento de multiples oraciones en lote
**Severity**: major | **Validation**: AUTO_VALIDATED

El tokenizador debe soportar el procesamiento de multiples oraciones en una sola invocacion (procesamiento en lote). Esto es fundamental por rendimiento: un curso tipico tiene aproximadamente 11.500 quizzes, cada uno con al menos una oracion. Invocar el procesamiento NLP oracion por oracion seria prohibitivamente lento.

El procesamiento en lote permite enviar todas las oraciones de un nivel (o de todo el curso) al motor NLP en una sola invocacion, reduciendo drasticamente el overhead de inicializacion y comunicacion.

**Error**: "Error en procesamiento en lote: {cantidad} de {total} oraciones fallaron durante la tokenizacion"

### Rule[F-NLP-R009] - El tokenizador es un servicio del dominio, no un analizador
**Severity**: major | **Validation**: AUTO_VALIDATED

El tokenizador NLP es un **servicio de infraestructura** que provee capacidades de procesamiento linguistico a los analizadores. No es un analizador en si mismo: no produce puntuaciones ni evaluaciones. Su responsabilidad es exclusivamente transformar texto plano en tokens enriquecidos.

Los analizadores (como el de distribucion COCA o el de longitud de oraciones) consumen los tokens producidos por el tokenizador para realizar sus evaluaciones especificas. Esta separacion es importante porque permite que multiples analizadores compartan el mismo resultado de tokenizacion sin reprocesar.

**Error**: N/A (esta regla describe una responsabilidad de diseno)

### Rule[F-NLP-R010] - El mapeo del curso debe utilizar la tokenizacion enriquecida
**Severity**: critical | **Validation**: AUTO_VALIDATED

El proceso que convierte los datos del curso en datos auditables (`CourseToAuditableMapper`) actualmente invoca al tokenizador para obtener el conteo de tokens de cada quiz. Con la evolucion, este proceso debe invocar la tokenizacion enriquecida para cada oracion y almacenar la lista completa de tokens enriquecidos en el modelo `AuditableQuiz`.

Esto significa que los tokens enriquecidos se generan **una sola vez** durante el mapeo del curso, no cada vez que un analizador los necesita. Todos los analizadores trabajan sobre los mismos tokens ya enriquecidos. Esto evita reprocesamiento y garantiza consistencia entre analizadores.

**Error**: "Error al mapear el quiz '{quizId}': la tokenizacion enriquecida fallo para la oracion"

---

### Grupo C - Busqueda de frecuencia COCA

### Rule[F-NLP-R011] - El ranking de frecuencia se obtiene del lema, no de la forma flexionada
**Severity**: critical | **Validation**: AUTO_VALIDATED

El `frequencyRank` de un token se basa en su **lema** (forma base), no en la forma flexionada que aparece en la oracion. Esto es porque los datos de frecuencia COCA estan organizados por lema: el lema "run" tiene un ranking unico que aplica a todas sus formas (runs, running, ran). Si se buscara por forma flexionada, muchas palabras no se encontrarian o tendrian rankings inconsistentes.

Ejemplo: la oracion "She was running quickly" se procesa asi:
- "She" -> lema "she" -> ranking basado en el lema "she"
- "was" -> lema "be" -> ranking basado en el lema "be"
- "running" -> lema "run" -> ranking basado en el lema "run"
- "quickly" -> lema "quickly" -> ranking basado en el lema "quickly"

**Error**: N/A (esta regla describe el criterio de busqueda)

### Rule[F-NLP-R012] - Mapeo de etiquetas POS para busqueda de frecuencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para buscar la frecuencia de un token en los datos COCA, primero se debe mapear la etiqueta POS de SpaCy (esquema Universal Dependencies) al codigo POS del COCA. Los datos COCA usan un esquema propio de codigos de un solo caracter:

| Etiqueta SpaCy | Codigo COCA | Descripcion |
|----------------|-------------|-------------|
| NOUN | n | Sustantivo |
| PROPN | n | Nombre propio (mapeado a sustantivo) |
| VERB | v | Verbo |
| AUX | v | Auxiliar (mapeado a verbo) |
| ADJ | j | Adjetivo |
| ADV | r | Adverbio |
| PRON | p | Pronombre |
| DET | a | Determinante/Articulo |
| ADP | i | Preposicion |
| CONJ | c | Conjuncion |
| CCONJ | c | Conjuncion coordinante |
| SCONJ | c | Conjuncion subordinante |
| NUM | m | Numero |
| PART | t | Particula |
| PRT | t | Particula |
| INTJ | u | Interjection |
| SYM | x | Simbolo |
| PUNCT | x | Puntuacion |
| SPACE | x | Espacio |
| X | x | Otro |

Si una etiqueta POS de SpaCy no tiene mapeo conocido, se asigna el codigo por defecto 'x' (otro).

**Error**: N/A (esta regla describe una tabla de mapeo)

### Rule[F-NLP-R013] - Estrategia de busqueda de frecuencia en tres niveles
**Severity**: critical | **Validation**: AUTO_VALIDATED

La busqueda de frecuencia COCA sigue una estrategia de tres niveles, ejecutados en orden hasta encontrar un resultado:

1. **Busqueda por forma exacta de la palabra** (prioridad mas alta): se busca la palabra original (en minusculas) directamente en los datos COCA. Esto es util para formas que aparecen explicitamente en el archivo de frecuencias.

2. **Busqueda por lema + codigo POS COCA**: se construye la clave `lema#posCoca` (ejemplo: `run#v`) y se busca en los datos. Si hay multiples entradas para la misma clave, se retorna la de mejor (menor) ranking.

3. **Busqueda solo por lema** (fallback): se busca unicamente por el lema sin considerar el POS. Se retorna la entrada con mejor ranking. Esto cubre casos donde el POS mapeado no coincide exactamente con el del COCA.

Si ninguno de los tres niveles encuentra resultado, el token queda con `frequencyRank` nulo (ver R028).

**Error**: N/A (esta regla describe la estrategia de busqueda)

### Rule[F-NLP-R014] - Estructura de los datos de frecuencia COCA
**Severity**: major | **Validation**: AUTO_VALIDATED

Los datos de frecuencia COCA provienen del archivo `lemmas_20k_words.txt`, un archivo TSV con las 20.000 lemas mas frecuentes del ingles y sus formas. El archivo tiene la siguiente estructura:

- Las primeras 8 lineas son comentarios/metadatos y deben omitirse al procesar
- La linea 9 es la cabecera de columnas
- A partir de la linea 10, cada fila contiene: ranking del lema, lema, codigo POS, frecuencia del lema, frecuencia de la forma, forma de la palabra

Cada lema puede tener multiples filas (una por cada forma flexionada). Al construir el indice de busqueda:

- Para la clave `lema#pos`: se almacena la entrada con mejor (menor) ranking si hay duplicados.
- Para la clave solo por lema: se almacena la entrada con mejor (menor) ranking si hay duplicados.
- Para la clave por forma exacta de la palabra: se almacena la entrada con mayor frecuencia de palabra (`wordFreq`) si hay duplicados.

**Error**: "Error al cargar datos de frecuencia COCA: {detalle}"

### Rule[F-NLP-R015] - Los datos de frecuencia se cargan una sola vez al inicio
**Severity**: major | **Validation**: AUTO_VALIDATED

El archivo de datos de frecuencia COCA debe cargarse en memoria una sola vez al inicio del procesamiento. Las busquedas de frecuencia posteriores operan sobre el indice en memoria. Esto es fundamental por rendimiento: con ~11.500 quizzes y ~10 tokens por oracion (~115.000 tokens), cada token necesita una busqueda de frecuencia. Recargar el archivo para cada busqueda seria prohibitivamente lento.

El indice en memoria debe soportar las tres estrategias de busqueda descritas en R013: por forma exacta, por lema+POS, y solo por lema.

**Error**: "No se pudieron cargar los datos de frecuencia COCA al inicio"

### Rule[F-NLP-R016] - El ranking de frecuencia retornado es el ranking del lema
**Severity**: major | **Validation**: AUTO_VALIDATED

El `frequencyRank` asociado a cada token es el `lemRank` (ranking del lema) del dato COCA, no la frecuencia de la forma especifica. Esto significa que todas las formas flexionadas de un mismo lema comparten el mismo ranking. Por ejemplo:

- "is", "was", "are", "were", "been" -> lema "be" -> frequencyRank = 2
- "runs", "running", "ran" -> lema "run" -> frequencyRank = 245

Este comportamiento es consistente con lo que espera el analizador COCA (F-COCA-R001): clasifica tokens en bandas de frecuencia usando el `frequencyRank`, que debe reflejar la frecuencia del lema, no de la forma individual.

**Error**: N/A (esta regla describe el valor retornado)

---

### Grupo D - Integracion con SpaCy

### Rule[F-NLP-R017] - SpaCy como motor de procesamiento linguistico
**Severity**: critical | **Validation**: AUTO_VALIDATED

El motor de procesamiento linguistico principal es **SpaCy** (version 3.7.2) con el modelo **en_core_web_sm** para ingles. SpaCy provee tokenizacion, lematizacion, etiquetado POS, deteccion de stop words, y deteccion de puntuacion en una sola pasada.

SpaCy se ejecuta como un proceso Python externo. El sistema ContentAudit (Java) invoca al script Python que carga SpaCy, procesa las oraciones, y retorna los resultados. El modelo `en_core_web_sm` es un modelo compacto (~12 MB) que ofrece un buen balance entre precision y velocidad.

**Error**: "No se pudo iniciar el motor SpaCy: {detalle}"

### Rule[F-NLP-R018] - Comunicacion con el proceso Python via archivos JSON
**Severity**: major | **Validation**: ASSUMPTION

La comunicacion entre ContentAudit (Java) y el script Python se realiza mediante archivos JSON:

1. ContentAudit escribe un archivo JSON de entrada con las oraciones a procesar
2. ContentAudit invoca el proceso Python pasando las rutas de los archivos de entrada y salida
3. El proceso Python lee el archivo de entrada, procesa las oraciones con SpaCy y los datos COCA, y escribe un archivo JSON de salida
4. ContentAudit lee el archivo JSON de salida y convierte los resultados en tokens enriquecidos

El formato del archivo de entrada es:
```
{
  "sentences": ["She likes cats.", "He was running quickly."]
}
```

El formato del archivo de salida contiene, para cada oracion, la lista de tokens procesados con su informacion linguistica y de frecuencia.

[ASSUMPTION] Se asume que la comunicacion via archivos JSON es la estrategia elegida. Alternativas como stdin/stdout o un servicio persistente podrian ofrecer mejor rendimiento para volumenes grandes, pero la comunicacion por archivos es la mas simple y es consistente con el script original. Si el rendimiento resulta insuficiente, se puede evolucionar a stdin/stdout sin cambiar la logica de negocio.

**Error**: "Error en la comunicacion con el proceso Python: no se pudo leer/escribir el archivo {ruta}"

### Rule[F-NLP-R019] - El proceso Python integra lematizacion y busqueda de frecuencia
**Severity**: critical | **Validation**: AUTO_VALIDATED

El script Python realiza **todo el procesamiento en un solo paso**: tokenizacion, lematizacion, etiquetado POS, deteccion de stop words, y busqueda de frecuencia COCA. No hay separacion entre el procesamiento SpaCy y la busqueda de frecuencia; ambos ocurren dentro del mismo proceso Python.

Esto es importante porque la busqueda de frecuencia depende del lema y del POS, que son productos del procesamiento SpaCy. Mantener ambas operaciones en el mismo proceso evita transferencias intermedias de datos y simplifica la logica.

El script carga los datos de frecuencia COCA (`lemmas_20k_words.txt`) al inicio y los mantiene en memoria durante toda la sesion de procesamiento. El modelo SpaCy tambien se carga una sola vez al inicio.

**Error**: N/A (esta regla describe la arquitectura del proceso Python)

### Rule[F-NLP-R020] - Requisitos del entorno de ejecucion
**Severity**: major | **Validation**: AUTO_VALIDATED

Para que el tokenizador enriquecido funcione, el entorno de ejecucion debe tener instalados:

1. **Python 3.11 o superior**: el runtime necesario para ejecutar el script de procesamiento
2. **SpaCy 3.7.2**: la biblioteca de procesamiento de lenguaje natural
3. **Modelo en_core_web_sm**: el modelo de SpaCy para ingles (se instala via `python -m spacy download en_core_web_sm`)
4. **El archivo lemmas_20k_words.txt**: los datos de frecuencia COCA deben estar accesibles para el script Python

Si alguno de estos componentes no esta disponible, el tokenizador no puede producir tokens enriquecidos y debe reportar el error de manera clara (ver R029).

**Error**: "Requisito de entorno faltante: {componente}. Consulte la documentacion de instalacion."

### Rule[F-NLP-R021] - La ruta del script Python y de los datos COCA es configurable
**Severity**: minor | **Validation**: ASSUMPTION

Las rutas del script Python (`sample_processor.py`) y del archivo de datos de frecuencia COCA (`lemmas_20k_words.txt`) deben ser configurables, no estar hardcodeadas. Esto permite:
- Diferentes entornos (desarrollo, CI, produccion) con diferentes ubicaciones de archivos
- Versionar el script y los datos de forma independiente
- Facilitar las pruebas con datos o scripts alternativos

[ASSUMPTION] Se asume que la configuracion se realiza mediante parametros de configuracion del sistema. El mecanismo exacto de configuracion (archivo de propiedades, variables de entorno, parametros de linea de comandos) se definira en la fase de diseno.

**Error**: "Ruta del script Python no configurada o no encontrada: {ruta}"

### Rule[F-NLP-R022] - El procesamiento es sincrono
**Severity**: minor | **Validation**: ASSUMPTION

El procesamiento de tokenizacion enriquecida es sincrono: ContentAudit invoca al proceso Python, espera a que termine, y luego procesa los resultados. No hay procesamiento asincrono ni paralelo de multiples lotes simultaneos.

[ASSUMPTION] Se asume que el procesamiento sincrono es suficiente para los volumenes actuales del curso (~11.500 quizzes). Si se necesita procesar cursos significativamente mas grandes o multiples cursos en paralelo, se podria evolucionar a procesamiento asincrono, pero eso queda fuera del alcance de esta funcionalidad.

**Error**: N/A (esta regla describe el modelo de ejecucion)

---

### Grupo E - Cache de resultados

### Rule[F-NLP-R023] - Cache en memoria de tokens enriquecidos por oracion
**Severity**: major | **Validation**: AUTO_VALIDATED

Los resultados de la tokenizacion enriquecida deben cachearse en memoria por oracion: si la misma oracion se procesa mas de una vez, la segunda invocacion retorna los resultados cacheados sin invocar al proceso Python nuevamente.

Esto es relevante porque un curso puede tener oraciones repetidas (el mismo quiz en multiples contextos) y porque el decorador `CachedNlpTokenizer` ya implementa este patron para la tokenizacion simple. Con la evolucion, el cache debe adaptarse para almacenar tokens enriquecidos en lugar de listas de cadenas de texto.

**Error**: N/A (esta regla describe un mecanismo de optimizacion)

### Rule[F-NLP-R024] - El cache usa la oracion completa como clave
**Severity**: minor | **Validation**: AUTO_VALIDATED

La clave del cache de tokenizacion es la oracion completa (texto exacto). Dos oraciones identicas producen los mismos tokens enriquecidos. Si la oracion difiere en un solo caracter (incluyendo mayusculas, espacios o puntuacion), se trata como una oracion diferente y se procesa independientemente.

**Error**: N/A (esta regla describe el criterio de identidad del cache)

### Rule[F-NLP-R025] - Cache persistente opcional en disco
**Severity**: minor | **Validation**: ASSUMPTION

El sistema original mantenia un cache persistente en disco de los resultados de SpaCy (archivos `spacy-results-{A1,A2,B1,B2}.json`, totalizando ~61 MB). Este cache se identificaba por un hash SHA-256 del contenido de las oraciones y tenia una expiracion de 30 dias.

[ASSUMPTION] Se asume que el cache persistente en disco es una optimizacion deseable pero **no es obligatorio** para la primera version de esta funcionalidad. El cache en memoria (R023) es suficiente para una sesion de auditoria individual. El cache en disco evita reprocesar las mismas oraciones en ejecuciones sucesivas, lo cual es valioso pero puede implementarse en una iteracion posterior.

**Error**: N/A (esta regla describe una optimizacion opcional)

### Rule[F-NLP-R026] - El cache del CachedNlpTokenizer debe evolucionar
**Severity**: major | **Validation**: AUTO_VALIDATED

El decorador `CachedNlpTokenizer` actualmente cachea dos tipos de resultados: listas de cadenas de texto (de `tokenize`) y conteos enteros (de `countTokens`). Con la evolucion, debe adaptarse para cachear listas de tokens enriquecidos producidos por la nueva operacion de tokenizacion.

El principio del decorador permanece igual: intercepta la llamada al tokenizador, busca en el cache, y si no encuentra el resultado, delega al tokenizador real y almacena el resultado en cache.

**Error**: N/A (esta regla describe la evolucion de un componente existente)

### Rule[F-NLP-R027] - Volumetria del cache
**Severity**: minor | **Validation**: ASSUMPTION

Un curso tipico tiene ~11.500 quizzes con una oracion por quiz. Con ~10 tokens enriquecidos por oracion y ~100 bytes por token enriquecido (estimacion conservadora para los campos de R001), el cache en memoria para un curso completo ocuparia aproximadamente 11 MB.

[ASSUMPTION] Se asume que 11 MB es un tamano aceptable para un cache en memoria durante una sesion de auditoria. Si los cursos son significativamente mas grandes o si se auditan multiples cursos en la misma sesion, podria necesitarse un mecanismo de eviccion del cache (como LRU) o un limite de tamano. Para la primera version, se asume que el consumo de memoria es manejable.

**Error**: N/A (esta regla describe una estimacion de recursos)

---

### Grupo F - Manejo de errores y fallbacks

### Rule[F-NLP-R028] - Tokens sin ranking de frecuencia
**Severity**: major | **Validation**: AUTO_VALIDATED

Cuando la busqueda de frecuencia COCA no encuentra resultado para un token (ninguna de las tres estrategias de R013 encuentra coincidencia), el token se crea con `frequencyRank` nulo. El token es valido y contiene el resto de su informacion linguistica (lema, POS, isStop, isPunct).

Los tokens con `frequencyRank` nulo se pasan a los analizadores sin modificacion. Es responsabilidad de cada analizador decidir como tratar estos tokens. Segun F-COCA-R004, el analizador COCA los excluye del analisis de distribucion.

Ejemplos de tokens que tipicamente no tendran ranking: nombres propios poco comunes, neologismos, errores tipograficos, y palabras extremadamente raras no incluidas en las 20.000 lemas del COCA.

**Error**: N/A (esta regla describe un comportamiento esperado, no un error)

### Rule[F-NLP-R029] - Fallo del proceso Python
**Severity**: critical | **Validation**: AUTO_VALIDATED

Si el proceso Python falla (no se puede iniciar, termina con error, o produce una salida invalida), el tokenizador debe reportar el error de manera clara y no retornar tokens parciales o corruptos. Las causas posibles de fallo incluyen:

- Python no esta instalado o no se encuentra en la ruta del sistema
- SpaCy no esta instalado o el modelo `en_core_web_sm` no esta descargado
- El archivo `lemmas_20k_words.txt` no se encuentra o no es legible
- El script Python tiene un error de ejecucion
- El archivo de salida JSON no se puede crear o tiene formato invalido
- El proceso Python se agota por tiempo (timeout)

En todos estos casos, la auditoria debe poder continuar con funcionalidad reducida si es posible (ver R030), o terminar con un mensaje de error claro si el procesamiento NLP es un prerequisito indispensable.

**Error**: "El procesamiento NLP fallo: {causa}. Verifique que Python 3.11+, SpaCy y el modelo en_core_web_sm estan instalados."

### Rule[F-NLP-R030] - Fallback cuando SpaCy no esta disponible
**Severity**: major | **Validation**: ASSUMPTION

Si el motor SpaCy no esta disponible (Python no instalado, SpaCy no instalado, etc.), el sistema debe ofrecer un modo de **funcionalidad reducida** que permita ejecutar al menos los analizadores que no requieren datos linguisticos enriquecidos.

[ASSUMPTION] Se asume que el fallback consiste en: (a) el tokenizador regresa a la tokenizacion basica por espacios en blanco, (b) los analizadores que solo necesitan conteo de tokens (como SentenceLengthAnalyzer) siguen funcionando normalmente, y (c) los analizadores que requieren tokens enriquecidos (como el analizador COCA) reportan que no pueden ejecutarse por falta de datos linguisticos. El usuario recibe un mensaje claro indicando que funcionalidades estan disponibles y cuales no.

Alternativa: se podria usar el catalogo enriquecido (`enriched_vocabulary_catalog.json`) como fuente de datos de fallback para tokens que esten en el catalogo, aunque con cobertura limitada.

**Error**: "SpaCy no disponible: los analizadores que requieren datos linguisticos enriquecidos no se ejecutaran. Los analizadores basicos (longitud de oracion) funcionan normalmente."

### Rule[F-NLP-R031] - Timeout del proceso Python
**Severity**: major | **Validation**: ASSUMPTION

El proceso Python debe tener un timeout configurable. Si el proceso no completa dentro del tiempo limite, se termina forzosamente y se reporta como error.

[ASSUMPTION] Se asume un timeout por defecto de 5 minutos (300 segundos) para el procesamiento de un lote de oraciones. Este valor es una estimacion basada en el volumen del curso actual (~11.500 oraciones) y el rendimiento reportado del procesamiento SpaCy (~10-50 oraciones por segundo). Un curso completo deberia procesarse en 2-20 minutos dependiendo del hardware. El timeout debe ser configurable para adaptarse a diferentes entornos.

**Error**: "El procesamiento NLP excedio el tiempo limite de {timeout} segundos. Considere procesar en lotes mas pequenos o aumentar el timeout."

### Rule[F-NLP-R032] - Errores de tokens individuales no detienen el lote completo
**Severity**: major | **Validation**: AUTO_VALIDATED

Si el procesamiento de un token individual falla dentro de SpaCy (situacion rara pero posible con caracteres especiales o texto malformado), el error se registra pero no detiene el procesamiento del resto de las oraciones del lote. El token fallido se crea con datos basicos de fallback:

- `text`: el texto original del token
- `lemma`: el texto original en minusculas (fallback simple)
- `posTag`: "X" (etiqueta de Unknown)
- `frequencyRank`: nulo
- `isStop`: falso
- `isPunct`: se determina por inspeccion del caracter

Esto garantiza que un token problematico no impida la auditoria de todo el curso.

**Error**: "Error al procesar el token '{text}' en la oracion '{oracion}': {detalle}. Se uso fallback basico."

### Rule[F-NLP-R033] - Signos de puntuacion y espacios no participan como tokens linguisticos
**Severity**: minor | **Validation**: ASSUMPTION

Los signos de puntuacion (comas, puntos, signos de interrogacion, etc.) y los espacios producen tokens con `isPunct = true` y/o `posTag = PUNCT/SPACE`. Estos tokens se incluyen en la lista de tokens enriquecidos pero los analizadores los filtran segun sus necesidades:

- El analizador COCA (FEAT-COCA) no cuenta tokens de puntuacion para la distribucion de frecuencia (solo cuenta tokens con `frequencyRank` valido)
- El ContentWordFilter excluye explicitamente los tokens con POS "PUNCT" y "SYM"

[ASSUMPTION] Se asume que los tokens de puntuacion se incluyen en la salida del tokenizador para mantener la fidelidad con el texto original, pero que cada analizador los filtra segun su logica. Alternativamente, se podrian excluir desde el tokenizador, pero esto reduciria la flexibilidad para futuros analizadores que si necesiten la puntuacion (por ejemplo, analisis de complejidad sintactica).

**Error**: N/A (esta regla describe el tratamiento de puntuacion)

---

## User Journeys

### Journey[F-NLP-J001] - Auditar un curso con tokenizacion enriquecida
**Validation**: AUTO_VALIDATED

1. El usuario inicia una auditoria de un curso en ContentAudit
2. El sistema comienza el mapeo del curso: recorre todos los niveles, topics, knowledges y quizzes
3. Para cada quiz, el sistema extrae la oracion del texto del quiz
4. El sistema agrupa las oraciones y las envia al tokenizador NLP enriquecido en un lote
5. El tokenizador invoca al proceso Python/SpaCy, que procesa todas las oraciones: tokeniza, lematiza, asigna etiquetas POS, busca frecuencias COCA, y retorna la lista de tokens enriquecidos
6. El sistema almacena los tokens enriquecidos en el modelo de cada quiz (AuditableQuiz)
7. Los analizadores procesan los quizzes con sus tokens enriquecidos: el analizador de longitud de oraciones usa el conteo de tokens, el analizador COCA usa el `frequencyRank` y el lema de cada token
8. El usuario recibe los resultados de la auditoria con las evaluaciones de todos los analizadores

### Journey[F-NLP-J002] - El usuario no tiene SpaCy instalado
**Validation**: ASSUMPTION

1. El usuario inicia una auditoria sin tener Python o SpaCy instalados en su entorno
2. El sistema intenta invocar el proceso Python para la tokenizacion enriquecida
3. El proceso falla porque Python no esta disponible (o SpaCy no esta instalado)
4. El sistema reporta un mensaje claro: "SpaCy no disponible. Los analizadores que requieren datos linguisticos (distribucion COCA) no se ejecutaran."
5. El sistema ejecuta la auditoria con funcionalidad reducida: solo los analizadores que no requieren tokens enriquecidos (como el analizador de longitud de oraciones) producen resultados
6. El usuario recibe los resultados parciales con una nota indicando que analizadores no se ejecutaron y por que
7. El usuario consulta la documentacion para instalar Python, SpaCy y el modelo requerido

[ASSUMPTION] Se asume que el sistema puede operar en modo reducido. Si la tokenizacion enriquecida es absolutamente obligatoria (porque todos los analizadores la requieren), la auditoria deberia terminar con un error en lugar de producir resultados parciales. La decision depende de si existen analizadores que funcionen sin tokens enriquecidos.

### Journey[F-NLP-J003] - Diagnosticar problemas de tokenizacion en una oracion especifica
**Validation**: ASSUMPTION

1. El usuario ejecuta la auditoria y observa resultados inesperados en el analizador COCA para un knowledge especifico
2. El usuario consulta los tokens enriquecidos de una oracion problematica
3. El usuario observa que un token tiene `frequencyRank` nulo cuando esperaba un valor
4. El usuario verifica el lema y el POS del token para entender por que la busqueda COCA fallo
5. El usuario descubre que el lema fue asignado incorrectamente por SpaCy (por ejemplo, "lead" como sustantivo en lugar de verbo) lo que provoco que la busqueda por lema+POS no encontrara resultado
6. El usuario anota el caso como una limitacion del modelo de SpaCy y decide si ajustar el contenido o aceptar la limitacion

[ASSUMPTION] Se asume que el usuario tiene acceso a los detalles de tokenizacion de cada oracion para propositos de diagnostico. El formato y mecanismo de acceso a esta informacion de detalle se definira en la implementacion.

---

## Open Questions

### Doubt[DOUBT-BATCH-SIZE] - Tamano optimo de lote para el procesamiento SpaCy
**Status**: RESOLVED

El procesamiento en lote de oraciones (R008) requiere definir un tamano de lote. Enviar las ~11.500 oraciones del curso en un solo lote maximiza la eficiencia pero podria causar problemas de memoria en el proceso Python. Dividir en lotes mas pequenos reduce el riesgo de memoria pero incrementa el overhead de inicializacion.

**Pregunta**: Deberian enviarse todas las oraciones en un solo lote, o deberian dividirse en lotes de tamano configurable?

- [x] Opcion A: Un solo lote con todas las oraciones (maximo rendimiento, mayor consumo de memoria)
- [ ] Opcion B: Lotes de tamano configurable (por defecto 1000 oraciones) con multiples invocaciones al proceso Python
- [ ] Opcion C: Lotes por nivel CEFR (A1, A2, B1, B2) para alinear con la estructura del curso

**Resolucion**: Se envian todas las oraciones del curso en un solo lote. Esto simplifica la implementacion y maximiza el rendimiento al cargar el modelo SpaCy y los datos COCA una sola vez. El volumen actual (~11.500 oraciones) es manejable en memoria para un proceso Python.

### Doubt[DOUBT-PYTHON-LIFECYCLE] - Ciclo de vida del proceso Python
**Status**: RESOLVED

Cada invocacion del proceso Python implica cargar el modelo SpaCy (~2-3 segundos) y los datos de frecuencia COCA. Si se procesan multiples lotes (ver DOUBT-BATCH-SIZE), el modelo se cargaria multiples veces.

**Pregunta**: Deberia el proceso Python iniciarse una vez y mantenerse activo para procesar multiples lotes, o deberia iniciarse y terminarse por cada lote?

- [x] Opcion A: Un proceso por lote (mas simple, pero con overhead de carga del modelo en cada lote)
- [ ] Opcion B: Un proceso persistente que procesa multiples lotes via stdin/stdout (mejor rendimiento, mayor complejidad)
- [ ] Opcion C: Un proceso persistente que actua como servicio ligero (Estrategia 4 del analisis)

**Resolucion**: Un proceso por lote. Dado que DOUBT-BATCH-SIZE se resolvio como un solo lote, el proceso Python se inicia una vez, procesa todas las oraciones, y termina. No hay necesidad de persistencia ni gestion de ciclo de vida compleja.

### Doubt[DOUBT-ENRICHED-CATALOG-ROLE] - Rol del catalogo enriquecido como cache/complemento
**Status**: RESOLVED

El `enriched_vocabulary_catalog.json` contiene 15.696 entradas con lema, POS, frequencyRank y otros datos precalculados. Podria usarse como un cache de primera linea: si un token ya esta en el catalogo, usar sus datos sin invocar a SpaCy.

**Pregunta**: Deberia el sistema consultar primero el catalogo enriquecido antes de invocar a SpaCy, o deberia depender exclusivamente de SpaCy para la tokenizacion?

- [ ] Opcion A: SpaCy siempre (consistencia total, sin ambiguedad de fuentes)
- [ ] Opcion B: Catalogo primero, SpaCy como fallback (mejor rendimiento para palabras conocidas)
- [x] Opcion C: SpaCy para lematizacion y POS, catalogo para complementar con datos adicionales (nivel CEFR, tema)

**Resolucion**: SpaCy es la fuente primaria para lematizacion, POS tagging y datos linguisticos base. El catalogo enriquecido (`enriched_vocabulary_catalog.json`) se usa como complemento para agregar datos adicionales como nivel CEFR y tema que SpaCy no provee. La busqueda de frecuencia COCA se mantiene en el proceso Python via `lemmas_20k_words.txt`.

### Doubt[DOUBT-CEFRPY] - Deberia incluirse el analisis de nivel CEFR por token?
**Status**: RESOLVED

El script Python original (`sample_processor.py`) incluye analisis de nivel CEFR por token usando la biblioteca `cefrpy`. Este dato no es requerido por FEAT-COCA ni por los analizadores actualmente planificados, pero podria ser util para futuros analizadores de adecuacion de vocabulario por nivel.

**Pregunta**: Deberia incluirse el nivel CEFR como campo del token enriquecido?

- [ ] Opcion A: No incluirlo (no es necesario ahora, reduce complejidad y dependencias)
- [ ] Opcion B: Incluirlo como campo opcional (aprovecha que el script ya lo calcula)
- [x] Opcion C: Incluirlo como funcionalidad separada (no en el tokenizador, sino como un enriquecimiento posterior)

**Resolucion**: El nivel CEFR por token NO se incluye en el NlpToken ni en el tokenizador. Se implementara como funcionalidad separada en una fase posterior, posiblemente como un enriquecimiento que use el catalogo enriquecido (DOUBT-ENRICHED-CATALOG-ROLE). Esto mantiene el tokenizador enfocado en datos linguisticos base y frecuencia COCA.

### Doubt[DOUBT-PERSISTENT-PROCESS] - Proceso Python persistente vs invocacion por demanda
**Status**: RESOLVED

Relacionado con DOUBT-PYTHON-LIFECYCLE pero mas especifico: si se opta por un proceso persistente, esto cambia significativamente la complejidad de la integracion (hay que manejar el ciclo de vida del proceso, la comunicacion bidireccional, la deteccion de fallos, etc.).

**Pregunta**: La complejidad adicional de un proceso persistente se justifica para los volumenes actuales del curso?

- [ ] Opcion A: No, la invocacion por demanda es suficiente y mas simple
- [ ] Opcion B: Si, la carga del modelo SpaCy (2-3 seg) justifica mantener el proceso vivo
- [x] Opcion C: Depende del resultado de DOUBT-BATCH-SIZE; si todo va en un solo lote, no hace falta persistencia

**Resolucion**: No se necesita proceso persistente. Con un solo lote (DOUBT-BATCH-SIZE = Opcion A), se invoca un unico proceso Python que carga SpaCy una vez, procesa todas las oraciones, y termina. Sin complejidad adicional de ciclo de vida.

### Doubt[DOUBT-SPACY-VERSION] - Deberia actualizarse la version de SpaCy?
**Status**: RESOLVED

El sistema original usa SpaCy 3.7.2 (del Dockerfile original). La version actual de SpaCy podria ser mas reciente y ofrecer mejoras en precision de lematizacion y POS tagging.

**Pregunta**: Se debe mantener la version 3.7.2 para reproducibilidad, o actualizar a la ultima version estable?

- [ ] Opcion A: Mantener 3.7.2 para garantizar que los resultados son identicos al sistema original
- [x] Opcion B: Actualizar a la ultima version estable para beneficiarse de mejoras
- [ ] Opcion C: Permitir configurar la version como parametro para flexibilidad

**Resolucion**: Actualizar a la ultima version estable de SpaCy. No hay necesidad de mantener compatibilidad exacta con el sistema original, y las versiones mas recientes ofrecen mejoras en precision y rendimiento.
