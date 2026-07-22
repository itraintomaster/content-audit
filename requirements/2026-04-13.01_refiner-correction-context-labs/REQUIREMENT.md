---
feature:
  id: FEAT-RCLA
  code: F-RCLA
  name: Re-routing y Contexto de Correccion para LEMMA_ABSENCE en el Refiner
  priority: critical
---

# Re-routing y Contexto de Correccion para LEMMA_ABSENCE en el Refiner

El refiner actualmente genera tareas LEMMA_ABSENCE a nivel MILESTONE y COURSE. Estas tareas no son accionables: dicen "al nivel B1 le faltan estos lemas" pero no apuntan a un quiz especifico que un LLM pueda corregir. Mientras tanto, el analyzer ya detecta vocabulario fuera de nivel en quizzes individuales (un quiz de A1 que usa una palabra de B2), pero el refiner ignora esos hallazgos.

Este requerimiento hace dos cosas: (1) re-rutea las tareas LEMMA_ABSENCE para que apunten a quizzes individuales en lugar de milestones/cursos, y (2) agrega el contexto de correccion que un LLM necesita para reescribir la oracion reemplazando el vocabulario fuera de nivel, incluyendo sugerencias de lemas ausentes del nivel que pueden servir como reemplazo.

## Contexto

### El problema actual

El comando `refiner plan` genera tareas LEMMA_ABSENCE con target MILESTONE o COURSE:

```
Target:    MILESTONE
Node:      B1
Diagnosis: LEMMA_ABSENCE
Priority:  2345
```

Esta tarea no es accionable porque:
1. No apunta a un quiz especifico. Un milestone tiene cientos de quizzes; el LLM no sabe cual modificar.
2. La informacion de cobertura de lemas a nivel milestone (cuantos lemas faltan, cuales son) ya vive en el `AuditReport` como diagnostico analitico. Duplicarla en el plan de refinamiento es redundante.
3. Cuando un lema falta a nivel milestone, el lugar natural para incorporarlo es un quiz con oracion demasiado corta — que ya tiene su propia tarea SENTENCE_LENGTH con `suggestedLemmas` (FEAT-RCSL).

Sin embargo, el analyzer `LemmaByLevelAbsenceAnalyzer` ya escribe un score de `lemma-absence` en cada nodo quiz y almacena un `LemmaPlacementDiagnosis` con la lista de `MisplacedLemma` — palabras cuyo nivel CEFR esperado (segun EVP) es mas alto que el nivel del quiz donde aparecen. Un quiz de A1 que usa "negotiate" (B2) tiene un score < 1.0 y un MisplacedLemma registrado. Pero el refiner ignora este score en quizzes porque `"lemma-absence"` esta clasificado como `COURSE_LEVEL_ANALYZER`.

### Relacion con features existentes

- **FEAT-DLABS** (diagnosticos tipados de lemma-absence): provee el `LemmaPlacementDiagnosis` a nivel quiz con la lista de `MisplacedLemma` **y la lista de palabras fuera de catalogo** (`OutOfCatalogWord`: palabras de contenido que no figuran en el catalogo de vocabulario y fueron penalizadas por frecuencia, ver F-LABS), y el `LemmaAbsenceLevelDiagnosis` a nivel milestone. Este requerimiento consume el diagnostico de quiz —tanto los lemas mal ubicados como las palabras fuera de catalogo—; el de milestone no se ve afectado.
- **FEAT-RCSL** (contexto de correccion para SENTENCE_LENGTH): establecio el patron de contexto de correccion para el refiner. Ese feature ya consume los lemas ausentes del milestone como `suggestedLemmas` para oraciones demasiado cortas. La cobertura de lemas a nivel milestone sigue siendo util como enriquecedor de contexto en SENTENCE_LENGTH, aunque deje de generar tareas propias.
- **FEAT-CSTRUCT** (estructura del curso): provee la oracion original del quiz a traves de la entidad AuditableQuiz.
- **FEAT-LCOUNT** (analisis de conteo de repeticiones de lemas): provee la señal de `lemma-count` (cantidad de oraciones distintas del curso donde aparece cada lema content-word) y su umbral de exposicion. Este requerimiento la consume para marcar las palabras de contenido escasas de la oracion actual (R003b). Es la misma señal y el mismo umbral que ya usa **F-SLEM** para enriquecer `suggestedLemmas`.

### Evidencia (2026-07-22): tarjetas "mudas" por palabras fuera de catalogo

El diagnostico de placement del quiz ya trae las palabras fuera de catalogo (el contexto de correccion las materializa en su lista interna desde la iteracion de fuera-de-catalogo de F-LABS), pero **ni el formato JSON ni el formato texto de la tarea las mostraban**. La consecuencia observada: las tareas cuyo unico motivo es una palabra fuera de catalogo se muestran como tarjetas "mudas" —sin ninguna seccion que diga cual es la palabra problematica—, porque su lista de lemas mal ubicados esta vacia y la de palabras fuera de catalogo, aunque poblada, no se renderiza.

Casos reales del audit activo:

- **"He sipps a lot of tea."** — el typo "sipps" se detecta como palabra fuera de catalogo con descuento 0.3. La tarea no tiene lemas mal ubicados; sin la nueva seccion, la tarjeta no indica que "sipps" es el problema.
- **"Sheep run. Verb in infinitive = run"** — "Sheep" se detecta como palabra fuera de catalogo con rango de frecuencia 4450. Misma situacion: tarjeta muda.

En el audit activo hay **646 ocurrencias de palabras fuera de catalogo**; cualquier tarea motivada exclusivamente por ellas es hoy indescifrable desde la tarjeta. Este requerimiento cierra ese hueco exponiendo `outOfCatalogWords` en ambos formatos (R008, R009), con la estructura de R003d y la degradacion de R003e.

### Prerequisito: sourceAuditId

Mismo prerequisito que FEAT-RCSL. El `sourceAuditId` del plan de refinamiento debe contener el id real del reporte de auditoria para poder navegar al arbol y extraer diagnosticos.

### Alcance de esta iteracion

- **Solo LEMMA_ABSENCE a nivel QUIZ**: el re-routing afecta unicamente a `"lemma-absence"`. Los demas analyzers de `COURSE_LEVEL_ANALYZERS` (`"coca-buckets-distribution"`, `"lemma-recurrence"`) mantienen su comportamiento actual.
- **Sin analisis de impacto en sentence-length**: al reescribir la oracion para reemplazar vocabulario, la longitud podria cambiar. No se valida que la nueva oracion siga dentro del rango de tokens esperado. Si queda fuera de rango, aparecera como una nueva tarea SENTENCE_LENGTH en la proxima auditoria.
- **La sentence se entrega como texto plano**: misma limitacion que FEAT-RCSL.

### Actor principal

El consumidor del contexto es un **LLM** que recibe la informacion y genera una oracion corregida reemplazando las palabras fuera de nivel con vocabulario apropiado. ContentAudit se limita a preparar y entregar el contexto.

---

## Reglas de Negocio

### Grupo A - Re-routing de tareas LEMMA_ABSENCE

### Rule[F-RCLA-R001] - Las tareas LEMMA_ABSENCE deben apuntar a QUIZ, no a MILESTONE ni COURSE
**Severity**: critical | **Validation**: AUTO_VALIDATED

El refiner debe generar tareas LEMMA_ABSENCE unicamente para nodos QUIZ. Un quiz genera una tarea cuando su score de `lemma-absence` es menor que 1.0, lo que indica que contiene al menos una palabra cuyo nivel CEFR esperado (segun EVP) es mas alto que el nivel del quiz.

Las tareas de LEMMA_ABSENCE a nivel MILESTONE y COURSE se eliminan del plan de refinamiento. La informacion diagnostica a esos niveles (`LemmaAbsenceLevelDiagnosis` en milestones, `LemmaAbsenceCourseDiagnosis` en el curso) permanece intacta en el `AuditReport` como metrica analitica, y sigue siendo consumida por otros features (FEAT-RCSL la usa para `suggestedLemmas` en el contexto de SENTENCE_LENGTH).

**Error**: N/A (esta regla define un cambio de routing)

---

### Grupo B - Modelo del contexto de correccion

### Rule[F-RCLA-R003] - Estructura del contexto de correccion para LEMMA_ABSENCE
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada tarea de refinamiento de tipo LEMMA_ABSENCE, el sistema debe poder construir un contexto de correccion que contiene los siguientes datos:

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| taskId | Texto | RefinementTask.id | Identificador de la tarea que se esta corrigiendo |
| sentence | Texto | AuditableQuiz.sentence | La oracion actual del quiz tal como esta escrita (texto plano) |
| translation | Texto | QuizTemplateEntity.translation | Traduccion al espanol de la oracion actual |
| knowledgeTitle | Texto | AuditableKnowledge.title | Titulo del knowledge al que pertenece el quiz (e.g., "Affirmative sentences in the present simple") |
| knowledgeInstructions | Texto | AuditableKnowledge.instructions | Instrucciones del ejercicio (e.g., "Escribe la forma afirmativa") |
| topicLabel | Texto | AuditableTopic.label | Nombre del topic (e.g., "Present Simple") |
| cefrLevel | Nivel CEFR | Determinado por el milestone ancestro | Nivel CEFR del quiz (A1, A2, B1, B2) |
| misplacedLemmas | Lista de lemas fuera de nivel | LemmaPlacementDiagnosis.misplacedLemmas | Palabras del quiz cuyo nivel esperado es mas alto que el del quiz (ver R004) |
| suggestedLemmas | Lista de lemas sugeridos | Derivado de LemmaAbsenceLevelDiagnosis | Lemas ausentes del nivel CEFR que el LLM podria usar como reemplazo de las palabras fuera de nivel (ver R004b) |
| scarceContentWords | Lista de palabras escasas de la oración | Derivado de la señal de lemma-count del curso sobre las palabras de contenido de la oración actual | Palabras de contenido de la oración actual con exposición global baja (por debajo del umbral de lemma-count), que el LLM NO debería quitar al reescribir (ver R003b) |
| outOfCatalogWords | Lista de palabras fuera de catálogo de la oración | LemmaPlacementDiagnosis.outOfCatalogWords | Palabras de contenido de la oración actual que no figuran en el catálogo de vocabulario y fueron penalizadas por frecuencia; son las que el LLM debería reemplazar o corregir (ver R003d) |

Este contexto reune informacion de multiples fuentes: la tarea de refinamiento, el nodo quiz del arbol de auditoria (con su diagnostico de placement, que aporta tanto los lemas mal ubicados como las palabras fuera de catalogo, y la entidad AuditableQuiz), los nodos ancestros knowledge y topic (para el contexto pedagogico), la entidad QuizTemplateEntity (para la traduccion), el nodo milestone ancestro (con su diagnostico de ausencia de lemas para las sugerencias de reemplazo), y la señal de `lemma-count` del curso aplicada a las palabras de contenido de la oracion actual (para las palabras escasas; ver R003b).

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-RCLA-R004] - Estructura de cada lema fuera de nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada entrada en la lista `misplacedLemmas` del contexto de correccion incluye:

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| lemma | Texto | MisplacedLemma.lemmaAndPos.lemma | Forma base de la palabra (e.g., "negotiate") |
| pos | Texto | MisplacedLemma.lemmaAndPos.pos | Parte de la oracion (e.g., "VERB") |
| expectedLevel | Nivel CEFR | MisplacedLemma.expectedLevel | Nivel CEFR al que pertenece esta palabra segun EVP (e.g., B2). Siempre es mas alto que el nivel del quiz |
| quizLevel | Nivel CEFR | MisplacedLemma.foundInLevel | Nivel CEFR del quiz donde se encontro la palabra (e.g., A1) |
| cocaRank | Entero (opcional) | MisplacedLemma.cocaRank | Rango de frecuencia COCA. Puede no estar disponible |

La combinacion de `expectedLevel` y `quizLevel` le da al LLM la informacion clave: "esta palabra es de nivel B2 pero estas escribiendo para A1, buscá una alternativa de A1".

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-RCLA-R004b] - Obtencion de lemas sugeridos como candidatos de reemplazo
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los lemas sugeridos se obtienen del `LemmaAbsenceLevelDiagnosis` del nodo milestone ancestro del quiz, con la misma mecanica definida en FEAT-RCSL R003:

1. Desde el nodo quiz, navegar al nodo milestone ancestro usando el mecanismo de navegacion del arbol (FEAT-DLABS R011).
2. Obtener el `LemmaAbsenceLevelDiagnosis` del milestone (FEAT-DLABS R005).
3. De la lista `absentLemmas`, filtrar solo los lemas cuyo tipo de ausencia sea COMPLETELY_ABSENT o APPEARS_TOO_LATE. Se excluyen los APPEARS_TOO_EARLY porque ya aparecen en niveles anteriores y no resolverian una carencia de vocabulario.
4. Ordenar los lemas filtrados por rango COCA ascendente (los mas frecuentes primero). Los lemas sin rango COCA se colocan al final.
5. Tomar los primeros 10 lemas de la lista ordenada.

Cada lema sugerido incluye:
| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| lemma | Texto | AbsentLemma.lemmaAndPos.lemma | Forma base de la palabra |
| pos | Texto | AbsentLemma.lemmaAndPos.pos | Parte de la oracion |
| reason | Tipo de ausencia | AbsentLemma.absenceType | COMPLETELY_ABSENT o APPEARS_TOO_LATE |
| cocaRank | Entero (opcional) | AbsentLemma.cocaRank | Rango de frecuencia COCA (puede no estar disponible) |

El proposito de estos lemas sugeridos es doble: cuando el LLM reemplaza una palabra fuera de nivel, puede elegir de esta lista un lema que (a) sea del nivel correcto y (b) cubra vocabulario que falta en el curso, matando dos pajaros de un tiro.

**Error**: N/A (la ausencia de lemas sugeridos no es un error; ver R004c)

### Rule[F-RCLA-R004c] - Contexto sin lemas sugeridos
**Severity**: major | **Validation**: AUTO_VALIDATED

Si no hay lemas sugeridos disponibles (porque no existe diagnostico de ausencia de lemas en el milestone, o porque todos los lemas del nivel son de tipo APPEARS_TOO_EARLY, o porque el milestone ancestro no se encontro), el contexto de correccion se construye igualmente con la lista de lemas sugeridos vacia. La informacion de las palabras fuera de nivel sigue siendo suficiente para que el LLM reescriba la oracion con vocabulario apropiado, aunque sin sugerencias especificas de reemplazo.

**Error**: N/A (la ausencia de sugerencias no impide la correccion)

### Rule[F-RCLA-R003b] - Palabras de contenido escasas de la oracion actual
**Severity**: major | **Validation**: ASSUMPTION

El contexto de correccion incluye una lista `scarceContentWords`: las palabras de contenido de la oracion actual del quiz cuya exposicion global en el curso es baja, es decir, las que aparecen en menos oraciones distintas que el umbral de lemma-count. El proposito es complementar la edicion minima: le avisan al LLM que esas palabras son escasas en el curso para que **no las quite** al reescribir la oracion reemplazando el vocabulario fuera de nivel. Quitar una palabra de baja exposicion reduciria la cobertura de vocabulario del curso.

La lista se construye a partir de la oracion actual del quiz (la misma `sentence` del contexto) y la señal de `lemma-count` del curso (FEAT-LCOUNT / F-SLEM):

1. Considerar solo las **palabras de contenido** de la oracion actual: las que son sustantivo (NOUN), verbo (VERB), adjetivo (ADJ) o adverbio (ADV). Las palabras funcionales (articulos, preposiciones, conjunciones, pronombres, auxiliares, etc.) se excluyen: no son vocabulario que interese preservar.
2. Para cada una, obtener su exposicion global en el curso (`count`) segun la señal de `lemma-count`: la cantidad de oraciones distintas del curso donde aparece ese lema (identificado por su forma base mas su POS).
3. Incluir en `scarceContentWords` **unicamente** las palabras cuyo `count` esta **estrictamente por debajo** del umbral de lemma-count (`count < threshold`). Las palabras que igualan o superan el umbral (`count >= threshold`) NO son escasas: ya tienen suficiente exposicion en otras oraciones y su reemplazo no reduce cobertura, por lo que se omiten.

El umbral usado es el mismo umbral de exposicion de `lemma-count` (el valor `threshold` / N de FEAT-LCOUNT, con el que se decide sub-exposicion), y se expone junto a cada palabra para que el LLM entienda la escala.

Cada entrada en la lista `scarceContentWords` incluye:

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| lemma | Texto | Forma base de la palabra de contenido de la oracion | Forma base de la palabra (e.g., "butter") |
| pos | Texto | Parte de la oracion de la palabra | Parte de la oracion (NOUN, VERB, ADJ o ADV) |
| count | Entero | Señal de lemma-count del curso | Apariciones globales: cantidad de oraciones distintas del curso donde aparece el lema. Siempre menor que `threshold` |
| threshold | Entero | Umbral de exposicion de lemma-count | Umbral minimo de exposicion de lemma-count usado para decidir baja exposicion. Es el mismo umbral para todas las entradas de la lista |

**Error**: N/A (esta regla define la estructura de un registro y su criterio de inclusion)

### Rule[F-RCLA-R003c] - Contexto sin palabras escasas
**Severity**: major | **Validation**: ASSUMPTION

Si no hay palabras de contenido escasas que reportar —porque la auditoria no incluyo `lemma-count` y por lo tanto no hay señal de exposicion disponible, o porque ninguna palabra de contenido de la oracion actual esta por debajo del umbral, o porque la oracion no tiene palabras de contenido— el contexto de correccion se construye igualmente con la lista `scarceContentWords` **vacia**, sin impedir la construccion del contexto ni degradar el resto de la informacion. La ausencia de palabras escasas no es un error: simplemente significa que no hay palabras escasas de la oracion actual que advertir al LLM. Espeja el comportamiento de R004c para las sugerencias.

**Error**: N/A (la ausencia de palabras escasas no impide la correccion)

### Rule[F-RCLA-R003d] - Estructura de cada palabra fuera de catalogo
**Severity**: critical | **Validation**: VALIDATED

El contexto de correccion incluye una lista `outOfCatalogWords`: las palabras de contenido de la oracion actual del quiz que **no figuran en el catalogo de vocabulario** y que la auditoria penalizo por frecuencia. Son las palabras que, junto con los lemas mal ubicados, motivan la tarea LEMMA_ABSENCE. Provienen del mismo diagnostico de placement del quiz que ya alimenta `misplacedLemmas` (la señal de fuera-de-catalogo definida en FEAT-DLABS / F-LABS).

El proposito es que el LLM sepa exactamente cual es la palabra problematica **incluso cuando la tarea no tiene ningun lema mal ubicado** y su unico motivo es una palabra fuera de catalogo. Casos tipicos: un error de tipeo que el analisis trata como palabra desconocida (e.g., "sipps" por "sips"), o un nombre propio tratado como palabra comun (e.g., "Sheep"). Sin esta lista, esas tareas se muestran sin ninguna palabra señalada y resultan indescifrables desde la tarjeta (ver Evidencia 2026-07-22 en Contexto).

Cada entrada en la lista `outOfCatalogWords` incluye:

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| lemma | Texto | OutOfCatalogWord.lemma | Forma base de la palabra fuera de catalogo (e.g., "sipps", "Sheep") |
| pos | Texto | OutOfCatalogWord.observedPos | Parte de la oracion observada para la palabra (e.g., "VERB", "NOUN") |
| frequencyRank | Entero (opcional) | OutOfCatalogWord.frequencyRank | Rango de frecuencia observado de la palabra. Puede ser nulo cuando la palabra no tiene rango conocido |
| discount | Numero | OutOfCatalogWord.discount | Descuento de puntuacion que la palabra aplico a la oracion en la auditoria (e.g., 0.3) |

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-RCLA-R003e] - Contexto sin palabras fuera de catalogo
**Severity**: major | **Validation**: VALIDATED

El campo `outOfCatalogWords` esta **siempre presente** en el contexto de correccion. Cuando la oracion actual no tiene ninguna palabra fuera de catalogo penalizada —o el reporte de auditoria fuente es previo a la deteccion de palabras fuera de catalogo y por lo tanto no las registra— la lista se construye **vacia**, sin impedir la construccion del contexto ni degradar el resto de la informacion. Es el mismo patron siempre-presente que ya usan `scarceContentWords` (R003c) y `suggestedLemmas` (R004c): la ausencia de palabras fuera de catalogo no es un error, solo significa que la tarea no tiene ninguna palabra desconocida que señalar.

**Error**: N/A (la ausencia de palabras fuera de catalogo no impide la correccion)

### Rule[F-RCLA-R005] - Resolucion del nodo quiz desde una tarea de refinamiento
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para construir el contexto de correccion, el sistema debe localizar el nodo quiz correspondiente en el arbol de auditoria a partir de la tarea de refinamiento. El proceso es identico al definido en FEAT-RCSL R002:

1. Obtener el `sourceAuditId` del plan de refinamiento.
2. Cargar el `AuditReport` correspondiente.
3. Buscar en el arbol del reporte el nodo cuyo `nodeId` coincida con el `nodeId` de la tarea y cuyo nivel coincida con el `nodeTarget` de la tarea (QUIZ).

Una vez localizado el nodo quiz, se accede a:
- La oracion original a traves de la entidad AuditableQuiz asociada al nodo.
- El diagnostico de placement a traves de los diagnosticos tipados del nodo (LemmaPlacementDiagnosis, definido en FEAT-DLABS).
- Los nodos ancestros knowledge y topic para el contexto pedagogico.

Si el reporte de auditoria no se encuentra, el sistema debe indicar que no puede construir el contexto.

**Error**: "No se pudo cargar el reporte de auditoria '{sourceAuditId}' necesario para construir el contexto de correccion"

### Rule[F-RCLA-R006] - Contexto cuando el diagnostico de placement no esta disponible
**Severity**: major | **Validation**: AUTO_VALIDATED

Si el nodo quiz se localiza pero no tiene un `LemmaPlacementDiagnosis` (porque el quiz no fue procesado por el analyzer de lemma-absence, o porque la estructura de diagnosticos es incompatible), el contexto de correccion no puede construirse de forma util. A diferencia del contexto de SENTENCE_LENGTH donde la oracion y el diagnostico de longitud son suficientes sin lemas sugeridos, en LEMMA_ABSENCE la lista de lemas fuera de nivel ES la informacion central. Sin ella, el LLM no sabe que palabras reemplazar.

En este caso, el sistema indica que el contexto no esta disponible.

**Error**: "No se pudo obtener el diagnostico de placement de lemas para el quiz '{nodeId}'"

---

### Grupo C - Integracion con el comando que muestra una tarea individual

### Rule[F-RCLA-R007] - El comando que muestra una tarea individual incluye el contexto de correccion para tareas LEMMA_ABSENCE
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el comando `get task` (o la combinacion de filtros equivalente: `get tasks --status pending --sort priority --limit 1`) muestra una tarea de tipo LEMMA_ABSENCE, debe incluir el contexto de correccion ademas de los datos basicos de la tarea. Esto aplica tanto al formato texto como al formato JSON.

Dado que ahora las tareas LEMMA_ABSENCE apuntan a QUIZ (por R001), el target mostrado sera QUIZ en lugar de MILESTONE o COURSE.

**Error**: N/A (esta regla define un comportamiento de presentacion)

### Rule[F-RCLA-R008] - Formato JSON del contexto de correccion
**Severity**: critical | **Validation**: AUTO_VALIDATED

En formato JSON, el contexto de correccion se incluye como un campo `correctionContext` en la salida existente de los comandos `get task` y `get tasks` (cuando muestran una tarea individual o el primer match de un filtro, e.g., `get tasks --status pending --sort priority --limit 1`). Ejemplo:

```
{
  "taskNumber": 1,
  "totalTasks": 42,
  "id": "task-014",
  "target": "QUIZ",
  "nodeId": "quiz-id-123",
  "nodeLabel": "Quiz 14 - L1.T2.K3",
  "diagnosis": "LEMMA_ABSENCE",
  "priority": 1,
  "status": "PENDING",
  "correctionContext": {
    "sentence": "She needs to negotiate the contract before Friday",
    "translation": "Ella necesita negociar el contrato antes del viernes",
    "knowledgeTitle": "Affirmative sentences in the present simple",
    "knowledgeInstructions": "Escribe la forma afirmativa",
    "topicLabel": "Present Simple",
    "cefrLevel": "A1",
    "misplacedLemmas": [
      { "lemma": "negotiate", "pos": "VERB", "expectedLevel": "B2", "quizLevel": "A1", "cocaRank": 2840 },
      { "lemma": "contract", "pos": "NOUN", "expectedLevel": "B1", "quizLevel": "A1", "cocaRank": 1205 }
    ],
    "outOfCatalogWords": [],
    "suggestedLemmas": [
      { "lemma": "like", "pos": "VERB", "reason": "COMPLETELY_ABSENT", "cocaRank": 52 },
      { "lemma": "want", "pos": "VERB", "reason": "APPEARS_TOO_LATE", "cocaRank": 89 },
      { "lemma": "big", "pos": "ADJ", "reason": "COMPLETELY_ABSENT", "cocaRank": 201 }
    ],
    "scarceContentWords": [
      { "lemma": "friday", "pos": "NOUN", "count": 1, "threshold": 4 },
      { "lemma": "before", "pos": "ADV", "count": 3, "threshold": 4 }
    ]
  }
}
```

Los campos `scarceContentWords` y `outOfCatalogWords` estan **siempre presentes** en `correctionContext`; cuando no hay nada que reportar (ver R003c y R003e) son listas vacias `[]`. Cada entrada de `outOfCatalogWords` incluye `lemma`, `pos`, `frequencyRank` (numero, o `null` si la palabra no tiene rango conocido) y `discount` (ver R003d).

Cuando la tarea esta motivada **exclusivamente** por una palabra fuera de catalogo (sin lemas mal ubicados), `misplacedLemmas` sale vacia y `outOfCatalogWords` es la unica lista que señala el problema. Ejemplo real ("He sipps a lot of tea."):

```
{
  "target": "QUIZ",
  "diagnosis": "LEMMA_ABSENCE",
  "correctionContext": {
    "sentence": "He sipps a lot of tea.",
    "cefrLevel": "A1",
    "misplacedLemmas": [],
    "outOfCatalogWords": [
      { "lemma": "sipps", "pos": "VERB", "frequencyRank": 6081, "discount": 0.3 }
    ],
    "suggestedLemmas": [],
    "scarceContentWords": []
  }
}
```

Si el contexto de correccion no puede construirse (por ejemplo, porque el reporte de auditoria no se encuentra o el diagnostico no esta disponible), el campo `correctionContext` debe ser nulo y se incluye un campo `correctionContextError` con un mensaje descriptivo.

**Error**: "No se pudo construir el contexto de correccion: {motivo}"

### Rule[F-RCLA-R009] - Formato texto del contexto de correccion
**Severity**: major | **Validation**: AUTO_VALIDATED

En formato texto, el contexto de correccion se muestra debajo de los datos basicos de la tarea, separado visualmente. Aplica tanto a `get task` como a `get tasks` cuando devuelven una tarea individual (e.g., `get tasks --status pending --sort priority --limit 1`). Ejemplo:

```
Next task (#1 of 42):
  Target:    QUIZ
  Node:      Quiz 14 - L1.T2.K3 (id: quiz-id-123)
  Diagnosis: LEMMA_ABSENCE
  Priority:  1
  Status:    PENDING

  Correction context:
    Sentence:     She needs to negotiate the contract before Friday
    Translation:  Ella necesita negociar el contrato antes del viernes
    Knowledge:    Affirmative sentences in the present simple
    Instructions: Escribe la forma afirmativa
    Topic:        Present Simple
    CEFR Level:   A1
    Misplaced lemmas:
      1. negotiate (VERB) - expected B2, found in A1 [COCA #2840]
      2. contract (NOUN) - expected B1, found in A1 [COCA #1205]
    Out-of-catalog words:
      (none)
    Suggested replacements:
      1. like (VERB) - COMPLETELY_ABSENT [COCA #52]
      2. want (VERB) - APPEARS_TOO_LATE [COCA #89]
      3. big (ADJ) - COMPLETELY_ABSENT [COCA #201]
    Scarce content words (do not remove):
      1. friday (NOUN) - appears in 1/4 sentences
      2. before (ADV) - appears in 3/4 sentences
```

La seccion "Out-of-catalog words:" se muestra **tras** "Misplaced lemmas:". Cada palabra fuera de catalogo se lista con su forma base, su categoria gramatical entre parentesis, su rango de frecuencia y su descuento: `lemma (pos) - rank {frequencyRank}, discount {discount}`. Cuando la palabra no tiene rango conocido, se muestra `rank n/d`.

Cada palabra escasa se muestra con su exposicion global frente al umbral (`count`/`threshold`), para que quede claro cuan escasa es en el curso.

Cuando la tarea esta motivada **exclusivamente** por una palabra fuera de catalogo (sin lemas mal ubicados), "Misplaced lemmas:" sale vacia y "Out-of-catalog words:" es la unica seccion que señala el problema. Ejemplo real ("He sipps a lot of tea."):

```
    Misplaced lemmas:
      (none)
    Out-of-catalog words:
      1. sipps (VERB) - rank 6081, discount 0.3
```

Si no hay lemas sugeridos, la seccion "Suggested replacements" muestra "(none available)".

Si no hay palabras escasas (ver R003c), la seccion "Scarce content words" muestra "(none)".

Si no hay palabras fuera de catalogo (ver R003e), la seccion "Out-of-catalog words" muestra "(none)".

Si el contexto no puede construirse, se muestra un aviso en lugar de la seccion de contexto.

**Error**: N/A (esta regla define un formato de presentacion)

---

### Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio:

- **Sin validacion de impacto en sentence-length**: reemplazar una palabra podria cambiar la cantidad de tokens de la oracion. Si la nueva oracion queda fuera del rango esperado, aparecera como una tarea SENTENCE_LENGTH en la proxima auditoria. No se intenta optimizar ambos criterios simultaneamente en esta iteracion.

- **Sin analisis semantico de reemplazos**: no se valida que la palabra de reemplazo sea semanticamente compatible con el contexto de la oracion. El LLM elige el reemplazo apropiado.

- **La sentence se entrega como texto plano**: el contexto no incluye la estructura de formulario TEXT/CLOZE del quiz. Misma limitacion que FEAT-RCSL.

- **Solo APPEARS_TOO_EARLY a nivel quiz**: por la logica actual del analyzer, los MisplacedLemma a nivel quiz siempre tienen absenceType = APPEARS_TOO_EARLY. No se filtran ni transforman; se pasan tal cual.

---

## User Journeys

### Journey[F-RCLA-J001] - LLM recibe contexto para corregir un quiz con vocabulario fuera de nivel
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RCLA-J001
    name: LLM recibe contexto para corregir un quiz con vocabulario fuera de nivel
    flow:
      - id: generar_plan
        action: "El usuario genera un plan de refinamiento. Las tareas LEMMA_ABSENCE ahora apuntan a quizzes individuales en lugar de milestones"
        gate: [F-RCLA-R001]
        then: solicitar_tarea

      - id: solicitar_tarea
        action: "El usuario ejecuta 'content-audit get tasks --status pending --sort priority --limit 1' y obtiene una tarea LEMMA_ABSENCE con target QUIZ"
        then: verificar_auditoria

      - id: verificar_auditoria
        action: "El sistema busca el reporte de auditoria asociado al plan usando el sourceAuditId"
        gate: [F-RCLA-R005]
        outcomes:
          - when: "El reporte de auditoria existe y el nodo quiz se localiza correctamente"
            then: obtener_diagnostico
          - when: "El reporte no se encuentra o el nodo quiz no existe en el arbol"
            then: contexto_no_disponible

      - id: obtener_diagnostico
        action: "El sistema extrae la oracion del quiz y el LemmaPlacementDiagnosis del nodo. El diagnostico contiene las palabras que estan fuera de nivel (e.g., 'negotiate' es B2 pero el quiz es A1) y/o las palabras fuera de catalogo (e.g., el typo 'sipps')"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R003d]
        outcomes:
          - when: "El LemmaPlacementDiagnosis existe (con al menos un lema mal ubicado o una palabra fuera de catalogo que señale el problema)"
            then: buscar_sugerencias
          - when: "El LemmaPlacementDiagnosis no esta disponible"
            then: diagnostico_no_disponible

      - id: buscar_sugerencias
        action: "El sistema navega al milestone ancestro y obtiene los lemas ausentes del nivel CEFR como candidatos de reemplazo"
        gate: [F-RCLA-R004b]
        outcomes:
          - when: "Hay lemas ausentes de tipo COMPLETELY_ABSENT o APPEARS_TOO_LATE disponibles"
            then: marcar_escasas
          - when: "No hay lemas sugeridos disponibles"
            then: construir_contexto_sin_sugerencias

      - id: marcar_escasas
        action: "El sistema evalua las palabras de contenido de la oracion actual con la señal de lemma-count del curso y marca como escasas las que estan por debajo del umbral de exposicion"
        gate: [F-RCLA-R003b, F-RCLA-R003c]
        outcomes:
          - when: "La auditoria incluyo lemma-count y al menos una palabra de contenido de la oracion esta por debajo del umbral"
            then: construir_contexto_completo
          - when: "No hay palabras de contenido por debajo del umbral, o la auditoria no incluyo lemma-count"
            then: construir_contexto_con_escasas_vacio

      - id: construir_contexto_completo
        action: "El sistema construye el contexto con la oracion, la traduccion, el contexto pedagogico, la lista de lemas fuera de nivel, la lista de palabras fuera de catalogo de la oracion, los top 10 lemas sugeridos como candidatos de reemplazo, y la lista de palabras de contenido escasas que el LLM no debe quitar"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R004b, F-RCLA-R003b, F-RCLA-R003d, F-RCLA-R003e]
        then: presentar_contexto

      - id: construir_contexto_con_escasas_vacio
        action: "El sistema construye el contexto con la oracion, la traduccion, el contexto pedagogico, la lista de lemas fuera de nivel, la lista de palabras fuera de catalogo y los lemas sugeridos, pero con la lista de palabras escasas vacia"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R004b, F-RCLA-R003c, F-RCLA-R003d, F-RCLA-R003e]
        then: presentar_contexto

      - id: construir_contexto_sin_sugerencias
        action: "El sistema construye el contexto con la oracion, la traduccion, el contexto pedagogico, la lista de lemas fuera de nivel y la lista de palabras fuera de catalogo, pero sin sugerencias de reemplazo. Igual evalua e incluye las palabras de contenido escasas de la oracion actual"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R004c, F-RCLA-R003b, F-RCLA-R003c, F-RCLA-R003d, F-RCLA-R003e]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra la tarea junto con el contexto de correccion en el formato solicitado (texto o JSON)"
        gate: [F-RCLA-R007, F-RCLA-R008, F-RCLA-R009]
        result: success

      - id: contexto_no_disponible
        action: "El comando muestra la tarea basica con un aviso de que el contexto de correccion no esta disponible porque el reporte de auditoria no se encontro"
        gate: [F-RCLA-R008]
        result: failure

      - id: diagnostico_no_disponible
        action: "El comando muestra la tarea basica con un aviso de que el diagnostico de placement no esta disponible"
        gate: [F-RCLA-R006, F-RCLA-R008]
        result: failure
```

### Journey[F-RCLA-J002] - El plan de refinamiento ya no contiene tareas LEMMA_ABSENCE de milestone
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RCLA-J002
    name: El plan de refinamiento ya no contiene tareas LEMMA_ABSENCE de milestone
    flow:
      - id: generar_plan
        action: "El usuario genera un plan de refinamiento a partir de un reporte de auditoria donde hay milestones con lemas ausentes y quizzes con vocabulario fuera de nivel"
        gate: [F-RCLA-R001]
        then: verificar_tareas

      - id: verificar_tareas
        action: "El usuario lista las tareas del plan. Las tareas LEMMA_ABSENCE apuntan exclusivamente a nodos QUIZ. No aparecen tareas LEMMA_ABSENCE con target MILESTONE ni COURSE"
        gate: [F-RCLA-R001]
        then: verificar_auditoria

      - id: verificar_auditoria
        action: "El usuario consulta el reporte de auditoria original. Los diagnosticos de LemmaAbsenceLevelDiagnosis siguen presentes en los nodos milestone y course, con toda su informacion intacta. El cambio solo afecto al plan de refinamiento, no al reporte"
        result: success
```

---

## Open Questions

### Doubt[DOUBT-EMPTY-MISPLACED] - Quiz con score < 1.0 pero sin MisplacedLemma
**Status**: RESOLVED

Puede un quiz tener score de lemma-absence < 1.0 pero una lista vacia de MisplacedLemma?

Segun la implementacion actual del analyzer (`scoreQuiz()`), el score se calcula como `1.0 - maxDiscount`, donde `maxDiscount` solo es > 0 si se encuentra al menos un MisplacedLemma. Por lo tanto, score < 1.0 implica al menos un MisplacedLemma. Sin embargo, el diagnostico podria no estar disponible si la estructura de diagnosticos del nodo no es `DefaultQuizDiagnoses`.

**Answer**: No puede ocurrir por logica del analyzer, pero el diagnostico podria no estar disponible por incompatibilidad de tipos. R006 cubre este caso.

### Doubt[DOUBT-SCARCE-FIELD-NAME] - Nombre del campo de palabras escasas
**Status**: OPEN

Se eligio `scarceContentWords` para el campo nuevo (R003b), con entradas `{ lemma, pos, count, threshold }`. El nombre describe el concepto (palabras de contenido escasas de la oracion actual que no conviene quitar) y evita nombres genericos. Alternativas consideradas: `fragileContentWords`, `lowExposureContentWords`, `preserveLemmas`.

- [x] `scarceContentWords`
- [ ] `lowExposureContentWords`
- [ ] `preserveContentWords`

**Answer**: Confirmado por el usuario: `scarceContentWords` (tipo record `ScarceContentWord`). Cerrada.

### Doubt[DOUBT-SCARCE-COUNT-SOURCE] - Palabras de contenido de la oracion sin señal de lemma-count
**Status**: OPEN

R003b usa la señal de `lemma-count`, que solo mide lemas content-word que aparecen al menos una vez en el curso (FEAT-LCOUNT R003). Toda palabra de contenido de la oracion actual aparece, por definicion, al menos en esa oracion, por lo que siempre tiene un `count >= 1` medible. La decision asumida es: si `lemma-count` no formo parte de la auditoria, `scarceContentWords` queda vacia (R003c); no se recalcula la señal ad hoc durante la construccion del contexto (mismo criterio de no-recalculo que F-SLEM). 

**Answer**: (pendiente de confirmacion del usuario) Se asume: sin auditoria de `lemma-count`, lista vacia; no hay recalculo ad hoc. Si el usuario quiere que el contexto recalcule la exposicion cuando falta `lemma-count`, seria un cambio de alcance a evaluar con el arquitecto.
