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

- **FEAT-DLABS** (diagnosticos tipados de lemma-absence): provee el `LemmaPlacementDiagnosis` a nivel quiz con la lista de `MisplacedLemma`, y el `LemmaAbsenceLevelDiagnosis` a nivel milestone. Este requerimiento consume el diagnostico de quiz; el de milestone no se ve afectado.
- **FEAT-RCSL** (contexto de correccion para SENTENCE_LENGTH): establecio el patron de contexto de correccion para el refiner. Ese feature ya consume los lemas ausentes del milestone como `suggestedLemmas` para oraciones demasiado cortas. La cobertura de lemas a nivel milestone sigue siendo util como enriquecedor de contexto en SENTENCE_LENGTH, aunque deje de generar tareas propias.
- **FEAT-CSTRUCT** (estructura del curso): provee la oracion original del quiz a traves de la entidad AuditableQuiz.

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

Este contexto reune informacion de multiples fuentes: la tarea de refinamiento, el nodo quiz del arbol de auditoria (con su diagnostico de placement y la entidad AuditableQuiz), los nodos ancestros knowledge y topic (para el contexto pedagogico), la entidad QuizTemplateEntity (para la traduccion), y el nodo milestone ancestro (con su diagnostico de ausencia de lemas para las sugerencias de reemplazo).

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
    "suggestedLemmas": [
      { "lemma": "like", "pos": "VERB", "reason": "COMPLETELY_ABSENT", "cocaRank": 52 },
      { "lemma": "want", "pos": "VERB", "reason": "APPEARS_TOO_LATE", "cocaRank": 89 },
      { "lemma": "big", "pos": "ADJ", "reason": "COMPLETELY_ABSENT", "cocaRank": 201 }
    ]
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
    Suggested replacements:
      1. like (VERB) - COMPLETELY_ABSENT [COCA #52]
      2. want (VERB) - APPEARS_TOO_LATE [COCA #89]
      3. big (ADJ) - COMPLETELY_ABSENT [COCA #201]
```

Si no hay lemas sugeridos, la seccion "Suggested replacements" muestra "(none available)".

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
        action: "El sistema extrae la oracion del quiz y el LemmaPlacementDiagnosis del nodo. El diagnostico contiene la lista de palabras que estan fuera de nivel (e.g., 'negotiate' es B2 pero el quiz es A1)"
        gate: [F-RCLA-R003, F-RCLA-R004]
        outcomes:
          - when: "El LemmaPlacementDiagnosis existe y tiene al menos un MisplacedLemma"
            then: buscar_sugerencias
          - when: "El LemmaPlacementDiagnosis no esta disponible"
            then: diagnostico_no_disponible

      - id: buscar_sugerencias
        action: "El sistema navega al milestone ancestro y obtiene los lemas ausentes del nivel CEFR como candidatos de reemplazo"
        gate: [F-RCLA-R004b]
        outcomes:
          - when: "Hay lemas ausentes de tipo COMPLETELY_ABSENT o APPEARS_TOO_LATE disponibles"
            then: construir_contexto_completo
          - when: "No hay lemas sugeridos disponibles"
            then: construir_contexto_sin_sugerencias

      - id: construir_contexto_completo
        action: "El sistema construye el contexto con la oracion, la traduccion, el contexto pedagogico, la lista de lemas fuera de nivel, y los top 10 lemas sugeridos como candidatos de reemplazo"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R004b]
        then: presentar_contexto

      - id: construir_contexto_sin_sugerencias
        action: "El sistema construye el contexto con la oracion, la traduccion, el contexto pedagogico y la lista de lemas fuera de nivel, pero sin sugerencias de reemplazo"
        gate: [F-RCLA-R003, F-RCLA-R004, F-RCLA-R004c]
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
