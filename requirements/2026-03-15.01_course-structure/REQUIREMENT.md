---
feature:
  id: FEAT-COURSE
  code: F-COURSE
  name: Modelo de Dominio y Persistencia de Estructura de Curso
  priority: critical
---

# Modelo de Dominio y Persistencia de Estructura de Curso

Representar la estructura jerarquica completa de un curso de idiomas (Course, Milestone, Topic, Knowledge, Quiz Template) como modelo en memoria, con capacidad de lectura y escritura a archivos JSON en formato MongoDB Extended JSON, garantizando idempotencia semantica en el ciclo lectura-escritura.

## Contexto

El sistema Content Audit trabaja con cursos de idiomas que tienen una estructura jerarquica fija exportada desde una base de datos documental. La estructura sigue el patron:

**Course -> ROOT -> Milestone -> Topic -> Knowledge -> Quiz Template**

Actualmente los datos del curso existen como archivos JSON en disco, organizados en una estructura de directorios jerarquica que refleja la estructura del curso. El modulo `course` debe ser capaz de representar esta estructura completa en memoria, leerla desde archivos, y escribirla de vuelta preservando la fidelidad semantica de los datos.

Este modulo es autocontenido e independiente: no depende de ningun otro modulo del sistema. Sirve como la fuente de verdad para la estructura del curso que luego sera consumida por otros modulos (auditor, refiner, etc.).

### Modelo conceptual unificado

En la base de datos documental de origen, milestones, topics y knowledges son todos documentos de la misma coleccion, diferenciados por el campo `kind` (ROOT, MILESTONE, TOPIC, KNOWLEDGE). Esto significa que comparten una estructura base comun (id, code, kind, label, oldId, parentId) y la jerarquia se reconstruye a partir de las relaciones parent-child y el campo `children`/`ruleIds`.

### Volumenes esperados

El curso actual ("english-course") contiene:

| Entidad | Cantidad aproximada |
|---------|---------------------|
| Course | 1 |
| Milestones | 4 (A1, A2, B1, B2) |
| Topics | ~55 |
| Knowledges | ~608 |
| Quiz Templates | ~11.500 |

Estos volumenes deben cargar completamente en memoria sin problemas de rendimiento.

### Estructura de directorios

Los archivos se organizan en disco siguiendo la jerarquia natural del curso:

```
english-course/
  _course.json                          # Datos del curso
  a1/                                   # Directorio del milestone (slug)
    _milestone.json                     # Datos del milestone
    present-simple/                     # Directorio del topic (slug)
      _topic.json                       # Datos del topic
      affirmative-sentences-.../        # Directorio del knowledge (slug)
        _knowledge.json                 # Datos del knowledge
        quizzes.json                    # Array de quiz templates
      otro-knowledge/
        _knowledge.json
        quizzes.json
    otro-topic/
      _topic.json
      ...
  a2/
    _milestone.json
    ...
```

Los nombres de directorio son slugs generados automaticamente a partir del label de cada entidad (ej: "Present Simple" -> "present-simple"). Cada nivel de la jerarquia tiene su archivo JSON descriptor con prefijo `_` excepto los quiz templates que usan `quizzes.json`.

---

## Entidades del Dominio

### Course (Curso)

Representa el curso completo. Es la raiz de toda la jerarquia.

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico | Formato MongoDB ObjectId (24 caracteres hex). En el curso actual es "000000000000000000000000" |
| title | Nombre del curso | Ej: "english-course" |
| knowledgeIds | Lista ordenada de todos los IDs de knowledge del curso | Lista plana con todos los IDs de knowledge. El orden es significativo |

El course no tiene referencia directa a sus milestones en el JSON; la relacion se establece a traves de la estructura de directorios.

### Nodo ROOT

Nodo explicito que actua como intermediario entre el Course y los Milestones. En la base de datos de origen, el ROOT es un documento mas de la coleccion de knowledges con kind=ROOT. Su campo `children` contiene la lista ordenada de IDs de milestones, lo que determina el orden de procesamiento de los mismos.

En la estructura de directorios jerarquica, el nodo ROOT no tiene archivo propio: los milestones son subdirectorios directos del directorio del curso. Sin embargo, el modelo en memoria debe materializar este nodo ROOT porque:
- Permite agregar estadisticas a nivel de curso completo cuando el usuario navega al nodo raiz
- Preserva la lista `children` que define el orden de los milestones
- Mantiene la coherencia con el parentId de los milestones (que referencia al ROOT)

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico | Formato ObjectId |
| code | Codigo | Siempre "root" |
| kind | Tipo | Siempre "ROOT" |
| label | Etiqueta | Siempre vacio |
| children | Lista ordenada de IDs de milestones hijos | Define el orden de los milestones dentro del curso |

### Milestone (Hito)

Representa un nivel de proficiencia dentro del curso (ej: A1, A2, B1, B2).

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico | Formato ObjectId |
| code | Codigo de la entidad | Actualmente coincide con el id |
| kind | Tipo de nodo | Siempre "MILESTONE" |
| label | Nombre visible | Ej: "A1", "B2" |
| oldId | Identificador legacy | Formato UUID. Referencia al sistema anterior |
| parentId | ID del nodo padre | Referencia al nodo ROOT |
| children | Lista ordenada de IDs de topics hijos | El orden determina la secuencia de topics dentro del milestone |
| order | Posicion ordinal dentro del curso | Numerico, comenzando en 1. Derivado de la posicion en la lista children del ROOT |

### Topic (Tema)

Representa un area tematica dentro de un milestone (ej: "Present Simple", "Modal Verbs").

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico | Formato ObjectId |
| code | Codigo de la entidad | Actualmente coincide con el id |
| kind | Tipo de nodo | Siempre "TOPIC" |
| label | Nombre visible | Ej: "Present Simple", "Determiners" |
| oldId | Identificador legacy | Formato UUID |
| parentId | ID del milestone padre | |
| children | Lista de hijos | Siempre null en los datos actuales. Se preserva tal cual para mantener idempotencia |
| ruleIds | Lista ordenada de IDs de knowledges pertenecientes a este topic | El orden es significativo. Coincide con los knowledges presentes en los subdirectorios |
| order | Posicion ordinal dentro de su milestone | Numerico, comenzando en 1. Derivado de la posicion en la lista children del milestone padre |

### Knowledge (Conocimiento)

Representa una unidad de conocimiento concreta, un ejercicio o leccion especifica.

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico | Formato ObjectId |
| code | Codigo de la entidad | Actualmente coincide con el id |
| kind | Tipo de nodo | Siempre "KNOWLEDGE" |
| label | Nombre visible | Ej: "Affirmative sentences in the present simple" |
| oldId | Identificador legacy | Formato UUID |
| parentId | ID del topic padre | |
| isRule | Indicador de que es una regla | Siempre true en los datos actuales |
| instructions | Instrucciones del ejercicio en espanol | Ej: "Escribe la forma afirmativa en presente simple." Puede estar ausente en casos excepcionales |
| order | Posicion ordinal dentro de su topic | Numerico, comenzando en 1. Derivado de la posicion en la lista ruleIds del topic padre |

### Quiz Template (Plantilla de Quiz)

Representa una plantilla de ejercicio individual. Cada knowledge contiene entre 9 y 30 quiz templates (promedio: 18,9).

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| id | Identificador unico (campo `id`) | Formato ObjectId como string plano (no envuelto en $oid). Tambien presente como `_id` envuelto en $oid |
| kind | Tipo de quiz | Actualmente siempre "CLOZE" (completar huecos) |
| knowledgeId | ID del knowledge padre | |
| title | Titulo del quiz | Generalmente coincide con el label del knowledge |
| instructions | Instrucciones en espanol | Coincide con las del knowledge padre |
| translation | Traduccion al espanol de la oracion | Siempre presente |
| theoryId | Referencia a la teoria asociada | Formato: "{milestone}.{numero}.{nombre_teoria}". Ej: "a1.01.Present_Simple_Forms_All_Verbs_Except_Be" |
| topicName | Nombre del topic | Redundante con el label del topic padre |
| form | Estructura del formulario del ejercicio | Ver sub-estructura Form |
| difficulty | Nivel de dificultad | Numerico. Actualmente siempre 0.0 |
| retries | Reintentos permitidos | Numerico. Actualmente siempre 0.0 |
| noScoreRetries | Reintentos sin puntaje | Numerico. Actualmente siempre 0.0 |
| code | Codigo auxiliar | Actualmente siempre vacio |
| audioUrl | URL de audio del ejercicio | Actualmente siempre vacio |
| imageUrl | URL de imagen del ejercicio | Actualmente siempre vacio |
| answerAudioUrl | Referencia al audio de la respuesta | Formato: "{Milestone}.{seccion}.{subseccion}.{numero}". Ej: "A1.01.06.01" |
| answerImageUrl | URL de imagen de la respuesta | Actualmente siempre vacio |
| miniTheory | Teoria breve inline | Actualmente siempre vacio |
| successMessage | Mensaje de exito | Actualmente siempre vacio |

#### Sub-estructura: Form

Representa la estructura del ejercicio interactivo.

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| kind | Tipo de formulario | Actualmente siempre "CLOZE" |
| incidence | Peso o frecuencia | Numerico. Actualmente siempre 1.0 |
| label | Etiqueta | Actualmente siempre vacio |
| name | Nombre | Actualmente siempre vacio |
| sentenceParts | Lista ordenada de partes de la oracion | Compone la oracion del ejercicio. Ver SentencePart |

#### Sub-estructura: SentencePart

Cada parte de la oracion del ejercicio.

| Campo | Descripcion | Observaciones |
|-------|-------------|---------------|
| kind | Tipo de parte | "TEXT" (texto fijo) o "CLOZE" (hueco a completar) |
| text | Texto de la parte | Para TEXT: contiene el texto visible. Para CLOZE: siempre vacio |
| options | Opciones de respuesta | Para CLOZE: lista con la(s) respuesta(s) correcta(s) (actualmente siempre 1 opcion). Para TEXT: siempre null |

Un quiz puede tener 1 o 2 huecos CLOZE (321 de 11.487 tienen 2 huecos). La estructura tipica alterna entre partes TEXT y CLOZE: `[TEXT, CLOZE, TEXT]` o `[TEXT, CLOZE, TEXT, CLOZE, TEXT]`.

---

## Formato de datos: MongoDB Extended JSON

Todos los archivos usan el formato MongoDB Extended JSON. Esto significa que los tipos nativos de MongoDB se representan con wrappers especiales:

| Tipo | Formato en JSON | Ejemplo |
|------|----------------|---------|
| ObjectId | `{"$oid": "hex24"}` | `{"$oid": "6814dafa7d73e7209a13d389"}` |
| Double | `{"$numberDouble": "valor"}` | `{"$numberDouble": "0.0"}` |
| Date | `{"$date": {"$numberLong": "millis"}}` | `{"$date": {"$numberLong": "1744484693237"}}` |

El modelo debe preservar estos formatos al leer y escribir, para garantizar la idempotencia semantica.

---

## Reglas de Negocio

### Rule[F-COURSE-R001] - Jerarquia estricta de 5 niveles
**Severity**: critical | **Validation**: VALIDATED

La estructura del curso sigue una jerarquia fija e inmutable de exactamente 5 niveles: Course contiene un nodo ROOT, ROOT contiene Milestones, Milestone contiene Topics, Topic contiene Knowledges, Knowledge contiene Quiz Templates. No se permiten niveles intermedios adicionales ni omitir niveles. El nodo ROOT es unico por curso y actua como raiz del arbol de navegacion.

**Error**: "Estructura jerarquica invalida: se esperaban exactamente 5 niveles (Course > ROOT > Milestone > Topic > Knowledge > Quiz Template)"

### Rule[F-COURSE-R002] - Orden significativo de hijos
**Severity**: critical | **Validation**: AUTO_VALIDATED

El orden de los elementos hijos en todas las listas es significativo y debe preservarse fielmente. Esto aplica a: children del ROOT (orden de Milestones), children de Milestone (orden de Topics), ruleIds de Topic (orden de Knowledges), knowledgeIds de Course (orden global de Knowledges), sentenceParts de Form (orden de la oracion), y el array de quiz templates dentro de quizzes.json.

**Error**: "El orden de los elementos hijos fue alterado en {entidad} ({id})"

### Rule[F-COURSE-R003] - Idempotencia semantica lectura-escritura
**Severity**: critical | **Validation**: VALIDATED

Al leer la estructura de archivos, construir el modelo en memoria, y luego escribir de vuelta a archivos, el resultado debe ser semanticamente equivalente al original. Leer y escribir repetidamente no debe introducir cambios acumulativos. Esto incluye:
- Preservar el formato MongoDB Extended JSON ($oid, $numberDouble, etc.)
- Preservar los nombres de directorio exactos (slugs)
- Preservar valores null vs ausencia de campo
- Preservar el orden de elementos en arrays
- Preservar todos los campos presentes en el original, incluso si estan vacios

El orden de campos (keys) dentro de cada objeto JSON no necesita preservarse; se permite reordenar campos siempre que todos esten presentes con sus valores correctos.

**Error**: "La operacion de lectura-escritura modifico el contenido del archivo {nombre}: diferencia semantica encontrada en {detalle}"

### Rule[F-COURSE-R004] - Cada knowledge debe tener quiz templates
**Severity**: major | **Validation**: AUTO_VALIDATED

Todo knowledge debe tener al menos un quiz template asociado. En los datos actuales, cada knowledge tiene entre 9 y 30 quiz templates. Un directorio de knowledge sin archivo quizzes.json o con un array vacio se considera un error de datos.

**Error**: "El knowledge '{label}' ({id}) no tiene quiz templates asociados"

### Rule[F-COURSE-R005] - Consistencia de IDs entre niveles
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los IDs referenciados en las listas de hijos deben coincidir con las entidades que efectivamente existen en los subdirectorios. Especificamente:
- Los children del ROOT deben coincidir exactamente con los milestones existentes
- Los children del Milestone deben coincidir exactamente con los topics en sus subdirectorios
- Los ruleIds del Topic deben coincidir exactamente con los knowledges en sus subdirectorios
- Los knowledgeIds del Course deben coincidir con el total de knowledges existentes en toda la estructura

**Error**: "Inconsistencia de IDs en {entidad} ({id}): la lista {campo} referencia IDs que no existen en la estructura: {ids_faltantes}"

### Rule[F-COURSE-R006] - Identificadores unicos
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada entidad tiene un identificador unico en formato ObjectId (24 caracteres hexadecimales). No pueden existir dos entidades con el mismo ID en ningun nivel de la jerarquia.

**Error**: "Identificador duplicado: el ID {id} aparece en {entidad_1} y en {entidad_2}"

### Rule[F-COURSE-R007] - Correspondencia directorio-entidad
**Severity**: major | **Validation**: AUTO_VALIDATED

Cada directorio en la estructura de archivos debe contener exactamente su archivo JSON descriptor:
- El directorio raiz contiene `_course.json`
- Cada directorio de milestone contiene `_milestone.json`
- Cada directorio de topic contiene `_topic.json`
- Cada directorio de knowledge contiene `_knowledge.json` y `quizzes.json`

No debe haber directorios sin su archivo descriptor ni archivos descriptor huerfanos. Archivos o directorios no reconocidos (como archivos del sistema operativo, archivos temporales, u otros archivos no pertenecientes al modelo) deben ser ignorados silenciosamente durante la lectura, sin generar error.

**Error**: "El directorio '{ruta}' no contiene el archivo descriptor esperado '{archivo}'"

### Rule[F-COURSE-R008] - Integridad referencial parent-child
**Severity**: critical | **Validation**: VALIDATED

El parentId de cada entidad hija debe referenciar al ID de su entidad padre. Es decir:
- El parentId de cada Milestone referencia al nodo ROOT
- El parentId de cada Topic referencia a su Milestone padre
- El parentId de cada Knowledge referencia a su Topic padre
- El knowledgeId de cada Quiz Template referencia a su Knowledge padre

**Error**: "Integridad referencial rota: {entidad} ({id}) referencia al padre {parentId}, pero el padre esperado es {padre_real}"

### Rule[F-COURSE-R009] - Campos obligatorios por entidad
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada tipo de entidad tiene un conjunto minimo de campos que deben estar presentes con un valor no nulo ni vacio (campos obligatorios con valor):
- **Course**: id, title, knowledgeIds
- **ROOT**: id, code, kind, children
- **Milestone**: id, code, kind, label, oldId, parentId, children
- **Topic**: id, code, kind, label, oldId, parentId, ruleIds
- **Knowledge**: id, code, kind, label, oldId, parentId, isRule
- **Quiz Template**: id, _id, kind, knowledgeId, title, form, translation

Ademas, cada tipo de entidad tiene campos que deben estar presentes en el JSON pero cuyo valor puede ser vacio, null o cero (campos obligatorios sin restriccion de valor):
- **Topic**: children (actualmente siempre null)
- **Knowledge**: instructions (ausente en casos excepcionales)
- **Quiz Template**: instructions, theoryId, topicName, difficulty, retries, noScoreRetries, answerAudioUrl, code, audioUrl, imageUrl, answerImageUrl, miniTheory, successMessage

**Error**: "Campo obligatorio ausente: {entidad} ({id}) no tiene el campo '{campo}'"

### Rule[F-COURSE-R010] - Preservacion de campos vacios y valores por defecto
**Severity**: major | **Validation**: AUTO_VALIDATED

Los campos que estan presentes en el JSON pero con valores vacios (""), null, o 0.0 deben preservarse tal cual en la escritura. No deben omitirse campos vacios ni sustituirse por valores por defecto. Esto es esencial para la idempotencia (R003).

**Error**: "El campo '{campo}' en {entidad} ({id}) fue omitido o alterado durante la escritura; valor original: {valor_original}, valor escrito: {valor_escrito}"

### Rule[F-COURSE-R011] - Doble ID en Quiz Templates
**Severity**: major | **Validation**: AUTO_VALIDATED

Los quiz templates tienen dos campos de identificacion: `_id` (formato MongoDB Extended JSON con wrapper `$oid`) y `id` (string plano). Ambos contienen el mismo valor y ambos deben preservarse. Ejemplo: `"_id": {"$oid": "67fab6d5..."}` y `"id": "67fab6d5..."`.

**Error**: "Quiz template con IDs inconsistentes: _id={oid_value}, id={id_value}"

### Rule[F-COURSE-R012] - Formato numerico MongoDB Extended JSON
**Severity**: major | **Validation**: AUTO_VALIDATED

Los valores numericos en quiz templates (difficulty, retries, noScoreRetries, form.incidence) usan el formato `{"$numberDouble": "0.0"}` en lugar de numeros JSON nativos. Este formato debe preservarse exactamente al escribir.

**Error**: "Formato numerico incorrecto en {entidad} ({id}), campo '{campo}': se esperaba formato $numberDouble"

### Rule[F-COURSE-R013] - Orden jerarquico explicito
**Severity**: critical | **Validation**: VALIDATED

Cada entidad navegable (Milestone, Topic, Knowledge) debe tener un campo `order` que indica su posicion ordinal dentro de su padre. Este campo es numerico, comienza en 1, y es unico dentro de su nivel jerarquico:
- Los milestones se numeran 1, 2, 3, ... N dentro del curso (derivado de la lista `children` del ROOT)
- Los topics se numeran 1, 2, 3, ... N dentro de su milestone (derivado de la lista `children` del milestone)
- Los knowledges se numeran 1, 2, 3, ... N dentro de su topic (derivado de la lista `ruleIds` del topic)

Este campo `order` se calcula al cargar el curso a partir de la posicion del ID en la lista correspondiente del padre. Es un campo derivado, no almacenado en los archivos JSON originales.

**Error**: "Orden no asignado o duplicado: {entidad} ({id}) tiene order={order}, pero ya existe otra entidad con el mismo orden en {padre}"

### Rule[F-COURSE-R014] - Comportamiento ante datos inconsistentes durante la carga
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el sistema detecta una inconsistencia durante la carga de un curso (violacion de R005, R006, R008, o R009), la carga debe fallar completamente con un mensaje de error descriptivo. No se permiten cargas parciales ni estados intermedios. El mensaje de error debe indicar claramente cual regla se violo, en que entidad, y cual es la inconsistencia encontrada. Esto garantiza que el usuario nunca trabaje con datos corruptos sin saberlo.

**Error**: "Error al cargar el curso desde '{ruta}': {descripcion_de_la_inconsistencia}. La carga fue abortada."

### Rule[F-COURSE-R015] - Cada milestone debe tener al menos un topic
**Severity**: major | **Validation**: AUTO_VALIDATED

Todo milestone debe contener al menos un topic. Un milestone con lista de children vacia se considera un error de datos. Analogamente, todo topic debe contener al menos un knowledge (lista de ruleIds no vacia). Estas restricciones garantizan que la jerarquia no tenga ramas muertas sin contenido util.

**Error**: "Estructura incompleta: {entidad} '{label}' ({id}) no tiene hijos"

### Rule[F-COURSE-R016] - Generacion determinista de slugs desde el label
**Severity**: major | **Validation**: VALIDATED

Los nombres de directorio (slugs) se generan automaticamente a partir del label de cada entidad usando un algoritmo determinista. El algoritmo debe:
- Convertir a minusculas
- Reemplazar espacios por guiones
- Eliminar signos de puntuacion (?, !, etc.)
- Preservar caracteres alfanumericos y guiones

Dado el mismo label, el algoritmo debe producir siempre el mismo slug. Al leer la estructura existente, se utilizan los nombres de directorio tal como estan en disco. Al escribir entidades nuevas (que no tenian directorio previo), se genera el slug desde el label.

**Error**: "No se pudo generar un slug valido para la entidad '{label}' ({id})"

---

## User Journeys

### Journey[F-COURSE-J001] - Cargar un curso completo desde archivos
**Validation**: AUTO_VALIDATED

1. El usuario indica la ruta al directorio raiz del curso (ej: `db/english-course/`)
2. El sistema verifica que el directorio existe y contiene `_course.json`
3. El sistema lee `_course.json` del directorio raiz
4. El sistema descubre los subdirectorios de milestones y lee cada `_milestone.json`
5. Para cada milestone, el sistema descubre los subdirectorios de topics y lee cada `_topic.json`
6. Para cada topic, el sistema descubre los subdirectorios de knowledges y lee cada `_knowledge.json` y `quizzes.json`
7. El sistema construye la jerarquia completa en memoria con todas las relaciones padre-hijo resueltas, incluyendo el nodo ROOT
8. El sistema calcula el campo `order` para cada milestone, topic y knowledge segun su posicion en la lista de hijos del padre (R013)
9. El sistema valida la consistencia de IDs entre niveles (R005, R008) y la integridad de campos obligatorios (R009)
10. Si alguna validacion falla, la carga se aborta completamente con un mensaje descriptivo (R014)
11. El curso completo queda disponible para navegacion y consulta en memoria

### Journey[F-COURSE-J002] - Escribir un curso completo a archivos
**Validation**: AUTO_VALIDATED

1. El usuario tiene un curso cargado en memoria (posiblemente modificado)
2. El usuario indica la ruta de destino donde escribir la estructura
3. Si el directorio destino ya contiene una estructura de curso, el sistema la sobreescribe completamente: los archivos existentes se reemplazan por los nuevos
4. El sistema crea la estructura de directorios necesaria (si no existe)
5. El sistema escribe `_course.json` en el directorio raiz
6. Para cada milestone, el sistema crea su directorio (usando el slug derivado del label o preservando el nombre original si se leyo desde disco) y escribe `_milestone.json`
7. Para cada topic, el sistema crea su directorio y escribe `_topic.json`
8. Para cada knowledge, el sistema crea su directorio, escribe `_knowledge.json` y `quizzes.json`
9. Los archivos resultantes preservan el formato MongoDB Extended JSON y la equivalencia semantica con los datos en memoria

### Journey[F-COURSE-J003] - Verificar idempotencia lectura-escritura
**Validation**: AUTO_VALIDATED

1. El usuario carga un curso desde un directorio origen
2. El usuario escribe el curso a un directorio destino (puede ser el mismo u otro)
3. El usuario compara los archivos de origen y destino
4. Todos los archivos JSON deben ser semanticamente equivalentes (mismos campos, mismos valores, mismo formato de tipos MongoDB). El orden de campos (keys) dentro de un objeto puede variar
5. La estructura de directorios debe ser identica (mismos nombres, misma jerarquia)

### Journey[F-COURSE-J004] - Navegar la estructura del curso en memoria
**Validation**: AUTO_VALIDATED

1. El usuario carga el curso
2. El usuario puede posicionarse en el nodo ROOT y obtener una vista global del curso (cantidad de milestones, topics, knowledges)
3. El usuario puede obtener la lista de milestones del curso, ordenados segun su campo `order`
4. Desde un milestone, el usuario puede obtener sus topics, ordenados segun su campo `order`
5. Desde un topic, el usuario puede obtener sus knowledges, ordenados segun su campo `order`
6. Desde un knowledge, el usuario puede obtener sus quiz templates
7. El usuario puede navegar hacia arriba (de knowledge a topic, de topic a milestone, de milestone a ROOT)
8. El usuario puede acceder a cualquier entidad por su ID directamente, sin importar su tipo o nivel en la jerarquia. El sistema debe ofrecer un mecanismo de busqueda por ID que recorra todos los niveles

### Journey[F-COURSE-J005] - Modificar datos de contenido y persistir cambios
**Validation**: AUTO_VALIDATED

Este journey cubre modificaciones a los datos de contenido de entidades existentes, no cambios estructurales. Ejemplos de modificaciones de contenido: cambiar el label de un knowledge, modificar la traduccion de un quiz template, actualizar las instrucciones de un ejercicio.

Modificaciones estructurales (agregar/eliminar milestones, mover knowledges entre topics, crear nuevos quiz templates, etc.) estan fuera del alcance de esta feature y se abordaran en una feature separada si se requieren en el futuro.

1. El usuario carga el curso desde archivos
2. El usuario modifica datos de contenido del curso en memoria (ej: cambiar el label de un knowledge, modificar la traduccion de un quiz template)
3. El usuario escribe el curso modificado a archivos
4. Al releer los archivos, los cambios se reflejan correctamente
5. Los datos no modificados permanecen intactos (sin efectos colaterales)

### Journey[F-COURSE-J006] - Manejo de errores durante la carga
**Validation**: AUTO_VALIDATED

1. El usuario indica una ruta a un directorio que no existe, o que no contiene `_course.json`
2. El sistema informa que la ruta no es un directorio de curso valido
3. Alternativamente, el usuario indica una ruta valida pero con datos inconsistentes (ej: un milestone referencia un topic que no existe)
4. El sistema informa exactamente cual regla se violo y en que entidad, y aborta la carga
5. El usuario no obtiene un curso parcial o corrupto en ningun caso

---

## Open Questions

### Doubt[DOUBT-SLUG-GENERATION] - Como se generan los nombres de directorio (slugs)?
**Status**: RESOLVED

Los nombres de directorio son versiones "slug" del label de cada entidad. Ejemplos observados:
- "A1" -> "a1"
- "Present Simple" -> "present-simple"
- "Adjetives and Adverbs" -> "adjetives-and-adverbs" (nota: incluye el typo "Adjetives")
- "Good or Well?" -> "good-or-well" (se eliminan signos de puntuacion)
- "Adverbios de modo" -> "how-things-are-done-adverbs-of-manner" (este slug NO coincide con el label, es un caso anomalo)

**Pregunta**: Se deben generar los slugs automaticamente desde el label, o se deben leer los nombres de directorio existentes y preservarlos tal cual?

- [x] Opcion A: Generar slugs automaticamente desde el label con un algoritmo determinista
- [ ] Opcion B: Leer y preservar los nombres de directorio existentes, usandolos tal cual

**Answer**: Se generan automaticamente desde el label. Para los datos existentes, al leerlos se usa el nombre de directorio que ya existe en disco. Al escribir entidades nuevas, se genera el slug desde el label. Capturado en R016.

**Nota sobre el caso anomalo**: El caso "Adverbios de modo" -> "how-things-are-done-adverbs-of-manner" sugiere que el slug fue generado desde un label anterior en ingles y luego el label fue cambiado a espanol. Al leer desde disco, el slug existente se preserva. Al regenerar desde el label actual, el slug seria diferente. Esto es aceptable porque la idempotencia se mide sobre el modelo en memoria, y al escribir se preserva el slug original leido desde disco.

### Doubt[DOUBT-ROOT-NODE] - Como tratar el nodo ROOT en la estructura jerarquica?
**Status**: RESOLVED

En la exportacion plana (`knowledges.json`), existe un nodo ROOT que es padre de los milestones. Sin embargo, en la estructura de directorios jerarquica, este nodo no tiene representacion: los milestones son subdirectorios directos del directorio del curso.

**Pregunta**: El modelo en memoria debe incluir un nodo ROOT explicito, o los milestones cuelgan directamente del Course?

- [x] Opcion A: Incluir un nodo ROOT explicito que actue como intermediario entre Course y Milestones
- [ ] Opcion B: Los milestones cuelgan directamente del Course; el parentId de los milestones (que apunta al ROOT) se preserva como dato pero sin materializar el nodo ROOT

**Answer**: Se incluye un nodo ROOT explicito. De este modo, cuando el usuario navega al nodo ROOT, puede ver las estadisticas del curso completo. Sin el ROOT materializado, esto no seria posible. Capturado en R001 y en la seccion de entidades del dominio.

### Doubt[DOUBT-COURSE-MILESTONE-LINK] - Como se relaciona el Course con sus Milestones?
**Status**: RESOLVED

El archivo `_course.json` solo contiene `_id`, `title` y `knowledgeIds`. No contiene una lista de milestone IDs. La relacion Course->Milestone se establece unicamente por la estructura de directorios (los subdirectorios del directorio del curso son los milestones).

**Pregunta**: Al cargar el curso, basta con descubrir los milestones por los subdirectorios que contienen `_milestone.json`?

- [x] Opcion A: Si, descubrir milestones por presencia de `_milestone.json` en subdirectorios
- [ ] Opcion B: Usar algun otro mecanismo

**Answer**: Todos los milestones, topics y knowledges son documentos de la misma coleccion en la base de datos de origen, diferenciados por el campo KIND. Para la carga desde la estructura de directorios, se descubren los milestones por la presencia de `_milestone.json` en los subdirectorios. El orden de los milestones se obtiene de la lista `children` del nodo ROOT (ver DOUBT-MILESTONE-ORDER). El hecho de que compartan una estructura base comun queda reflejado en la seccion "Modelo conceptual unificado".

### Doubt[DOUBT-MILESTONE-ORDER] - En que orden se procesan los milestones?
**Status**: RESOLVED

El Course no tiene una lista ordenada de milestone IDs. Los milestones se descubren por estructura de directorios.

**Pregunta**: El orden de los milestones se determina alfabeticamente por nombre de directorio, o hay otro criterio?

- [ ] Opcion A: Orden alfabetico por nombre de directorio (a1, a2, b1, b2 ya ordena correctamente)
- [x] Opcion B: Usar el orden de children del nodo ROOT de la exportacion plana
- [ ] Opcion C: El orden no importa

**Answer**: El orden se determina por la lista de `children` del nodo padre correspondiente. Para los milestones, el orden viene de `children` del ROOT. Para los topics, de `children` del milestone. Para los knowledges, de `ruleIds` del topic. Ademas, se agrega un campo `order` jerarquico a cada entidad para hacer explicito el orden: milestones se numeran 1..N dentro del curso, topics 1..N dentro de su milestone, knowledges 1..N dentro de su topic. Capturado en R013.

### Doubt[DOUBT-CHILDREN-NULL] - Que significa el campo children=null en Topics?
**Status**: RESOLVED

Todos los topics tienen el campo `children` con valor `null`. Los knowledges del topic se referencian a traves del campo `ruleIds`.

**Pregunta**: Se debe preservar el campo `children: null` en los topics?

- [x] Opcion A: Si, preservar `children: null` exactamente como esta (necesario para idempotencia)
- [ ] Opcion B: Omitirlo en la escritura

**Answer**: Para garantizar la idempotencia lectura-escritura, se debe preservar. La idea es que los knowledges representen las "reglas" del conocimiento, es por eso del cambio de nomenclatura (ruleIds en lugar de children). Quiza no tenga mucho sentido ahora mismo, pero se preserva para mantener compatibilidad. Cubierto por R003 y R010.

### Doubt[DOUBT-FLAT-FILES] - El modulo debe soportar tambien la lectura de los archivos planos?
**Status**: RESOLVED

Ademas de la estructura jerarquica de directorios, existen archivos planos en `db/` (courses.json, knowledges.json, quiz-templates.json, quizzes.json) que contienen los mismos datos en formato de arrays planos.

**Pregunta**: El modulo debe soportar la lectura y escritura de ambos formatos (jerarquico y plano)?

- [x] Opcion A: Solo formato jerarquico de directorios
- [ ] Opcion B: Solo formato plano de archivos
- [ ] Opcion C: Ambos formatos

**Answer**: El modulo trabaja unicamente con el formato jerarquico de directorios. Los archivos planos quedan como respaldo externo al modulo; si ocurre algun problema, se puede reconstruir la estructura jerarquica manualmente desde ellos, pero esa operacion esta fuera del alcance de esta feature.

### Doubt[DOUBT-QUIZZES-USER-DATA] - Se debe modelar el archivo quizzes.json (datos de usuario)?
**Status**: RESOLVED

El archivo `db/quizzes.json` contiene datos de quizzes resueltos por usuarios (con campos como userId, score, learningStage, feedback). Esto es distinto a los quiz templates que son las plantillas de ejercicios.

**Pregunta**: El modulo course debe modelar estos datos de usuario, o solo las plantillas?

- [x] Opcion A: Solo plantillas de quiz (quiz templates). Los datos de usuario estan fuera del alcance
- [ ] Opcion B: Tambien modelar los datos de usuario

**Answer**: El modulo course modela la estructura del curso, no la actividad de los usuarios. Los datos de usuario pertenecen a otro dominio.

### Doubt[DOUBT-JSON-FIELD-ORDER] - Que tan estricta debe ser la preservacion del orden de campos JSON?
**Status**: RESOLVED

En los archivos actuales, el orden de campos dentro de cada objeto JSON no es consistente entre quiz templates del mismo archivo. Por ejemplo, en un quiz `translation` puede aparecer antes de `difficulty`, y en otro despues.

**Pregunta**: Al escribir, se debe preservar el orden original de campos de cada objeto, o se puede usar un orden canonico?

- [ ] Opcion A: Preservar el orden original de campos de cada objeto (maximo fidelidad, complejo)
- [ ] Opcion B: Usar un orden canonico consistente para todos los objetos del mismo tipo
- [x] Opcion C: No hay problema con el orden de los campos.

**Answer**: No se requiere preservar el orden de campos dentro de los objetos JSON. El sistema puede usar cualquier orden al escribir. La idempotencia se define a nivel semantico (mismos campos, mismos valores), no a nivel de orden de keys. Esto simplifica la escritura y es coherente con la definicion de R003.
