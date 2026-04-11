---
feature:
  id: FEAT-RCSL
  code: F-RCSL
  name: Contexto de Correccion para SENTENCE_LENGTH en el Refiner
  priority: critical
---

# Contexto de Correccion para SENTENCE_LENGTH en el Refiner

Cuando el refiner identifica una tarea de tipo SENTENCE_LENGTH, actualmente muestra **que** hay que corregir (cual quiz, que analizador la detecto, con que prioridad) pero no provee el **contexto** necesario para que un LLM pueda reescribir la oracion. Este requerimiento agrega la informacion contextual que un LLM necesita para generar una correccion informada: la oracion actual, el diagnostico de longitud, y una lista de lemas sugeridos que el LLM puede incorporar en la nueva oracion.

## Contexto

### El problema actual

El comando `refiner next` muestra la siguiente informacion para una tarea SENTENCE_LENGTH:

- Target: QUIZ
- Node: nombre del quiz y su id
- Diagnosis: SENTENCE_LENGTH
- Priority: un numero

Con esta informacion, un humano o un LLM sabe **donde** esta el problema pero no tiene lo necesario para resolverlo. Le faltan tres piezas fundamentales:

1. **La oracion actual**: el texto literal del quiz que debe ser reescrito.
2. **El diagnostico de longitud**: cuantos tokens tiene la oracion, cual es el rango esperado para su nivel CEFR, y cuanto se desvio. Sin esto, el LLM no sabe si debe acortar o alargar, ni en cuanto.
3. **Sugerencias de vocabulario**: lemas que estan ausentes en el nivel CEFR del quiz y que el LLM podria incorporar al reescribir la oracion, matando dos pajaros de un tiro (corrigiendo la longitud y cubriendo vocabulario faltante).

### Relacion con features existentes

Este requerimiento consume datos producidos por tres features anteriores:

- **FEAT-DSLEN** (diagnosticos tipados de sentence-length): provee el `SentenceLengthDiagnosis` con tokenCount, targetMin, targetMax, delta, cefrLevel.
- **FEAT-DLABS** (diagnosticos tipados de lemma-absence): provee el `LemmaAbsenceLevelDiagnosis` con la lista de `AbsentLemma` del nivel CEFR, y el mecanismo de navegacion hacia ancestros para obtenerlo.
- **FEAT-CSTRUCT** (estructura del curso): provee la oracion original del quiz a traves de la entidad AuditableQuiz.

No modifica ninguno de estos features; solo los consume para construir un paquete de contexto enriquecido.

### Prerequisito: sourceAuditId

El `RefinementPlan` tiene un campo `sourceAuditId` que vincula el plan con el `AuditReport` del cual fue derivado. Para poder navegar del plan al arbol de auditoria y extraer diagnosticos y oraciones, este campo debe contener el id real del reporte. [ASSUMPTION] Se asume que el sourceAuditId se propaga correctamente al crear el plan. Si actualmente llega vacio, es un bug previo que debe corregirse antes o como parte de esta feature. Razon: sin el sourceAuditId, no hay forma de vincular una tarea con su arbol de auditoria.

### Alcance de esta iteracion

Esta iteracion es deliberadamente simple:

- **Solo SENTENCE_LENGTH**: el contexto de correccion se define unicamente para tareas de tipo SENTENCE_LENGTH. Otros tipos de diagnostico (LEMMA_ABSENCE, COCA_BUCKETS, etc.) tendran sus propios contextos en futuras iteraciones.
- **Lemas sugeridos simples**: se toman de la lista de lemas ausentes del nivel CEFR correspondiente, filtrados por tipo de ausencia y ordenados por frecuencia COCA. No se cruza con COCA Buckets para balanceo de bandas de frecuencia.
- **Sin analisis semantico**: no se garantiza que los lemas sugeridos sean semanticamente compatibles con la oracion. El LLM decide cuales usar.
- **Sin analisis gramatical**: no se analiza la estructura gramatical de la oracion para sugerir donde insertar o eliminar palabras.

### Actor principal

El consumidor de este contexto es un **LLM** (modelo de lenguaje) que recibe la informacion y genera una oracion corregida. El LLM no forma parte del sistema ContentAudit; ContentAudit se limita a preparar y entregar el contexto.

---

## Reglas de Negocio

### Grupo A - Modelo del contexto de correccion

### Rule[F-RCSL-R001] - Estructura del contexto de correccion para SENTENCE_LENGTH
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada tarea de refinamiento de tipo SENTENCE_LENGTH, el sistema debe poder construir un contexto de correccion que contiene los siguientes datos:

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| taskId | Texto | RefinementTask.id | Identificador de la tarea que se esta corrigiendo |
| sentence | Texto | AuditableQuiz.sentence | La oracion actual del quiz tal como esta escrita (texto plano) |
| translation | Texto | QuizTemplateEntity.translation | Traduccion al espanol de la oracion actual |
| knowledgeTitle | Texto | AuditableKnowledge.title | Titulo del knowledge al que pertenece el quiz (e.g., "Affirmative sentences in the present simple") |
| knowledgeInstructions | Texto | AuditableKnowledge.instructions | Instrucciones del ejercicio (e.g., "Escribe la forma afirmativa") |
| topicLabel | Texto | AuditableTopic.label | Nombre del topic (e.g., "Present Simple") |
| cefrLevel | Nivel CEFR | SentenceLengthDiagnosis.cefrLevel | Nivel CEFR del quiz (A1, A2, B1, B2) |
| tokenCount | Entero | SentenceLengthDiagnosis.tokenCount | Cantidad actual de tokens en la oracion |
| targetMin | Entero | SentenceLengthDiagnosis.targetMin | Minimo de tokens esperados para el nivel CEFR |
| targetMax | Entero | SentenceLengthDiagnosis.targetMax | Maximo de tokens esperados para el nivel CEFR |
| delta | Entero | SentenceLengthDiagnosis.delta | Desviacion respecto al rango (positivo = exceso, negativo = carencia, 0 = dentro del rango) |
| suggestedLemmas | Lista de lemas sugeridos | Derivado de LemmaAbsenceLevelDiagnosis | Lista de lemas ausentes que el LLM podria incorporar (ver R003) |

Este contexto reune informacion de multiples fuentes: la tarea de refinamiento, el nodo quiz del arbol de auditoria (con su diagnostico de longitud y la entidad AuditableQuiz), los nodos ancestros knowledge y topic (para el contexto pedagogico del ejercicio), la entidad QuizTemplateEntity (para la traduccion), y el nodo milestone ancestro (con su diagnostico de ausencia de lemas).

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-RCSL-R002] - Resolucion del nodo quiz desde una tarea de refinamiento
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para construir el contexto de correccion, el sistema debe poder localizar el nodo quiz correspondiente en el arbol de auditoria a partir de la tarea de refinamiento. El proceso es:

1. Obtener el `sourceAuditId` del plan de refinamiento.
2. Cargar el `AuditReport` correspondiente.
3. Buscar en el arbol del reporte el nodo cuyo `nodeId` coincida con el `nodeId` de la tarea y cuyo nivel coincida con el `nodeTarget` de la tarea.

Una vez localizado el nodo quiz, se accede a:
- La oracion original a traves de la entidad AuditableQuiz asociada al nodo.
- El diagnostico de longitud a traves de los diagnosticos tipados del nodo (SentenceLengthDiagnosis, definido en FEAT-DSLEN).

Si el reporte de auditoria no se encuentra (el sourceAuditId es invalido o el reporte fue eliminado), el sistema debe indicar que no puede construir el contexto.

**Error**: "No se pudo cargar el reporte de auditoria '{sourceAuditId}' necesario para construir el contexto de correccion"

### Rule[F-RCSL-R003] - Obtencion de lemas sugeridos desde el milestone ancestro
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los lemas sugeridos se obtienen del `LemmaAbsenceLevelDiagnosis` del nodo milestone ancestro del quiz. El proceso es:

1. Desde el nodo quiz, navegar al nodo milestone ancestro usando el mecanismo de navegacion del arbol (FEAT-DLABS R011).
2. Obtener el `LemmaAbsenceLevelDiagnosis` del milestone (FEAT-DLABS R005).
3. De la lista `absentLemmas`, filtrar solo los lemas cuyo tipo de ausencia sea COMPLETELY_ABSENT o APPEARS_TOO_LATE. Se excluyen los APPEARS_TOO_EARLY porque no son candidatos utiles para incorporar: estos lemas ya aparecen en niveles anteriores al esperado y su inclusion no resolveria una carencia de vocabulario.
4. Ordenar los lemas filtrados por rango COCA ascendente (los mas frecuentes primero). Los lemas sin rango COCA se colocan al final. [ASSUMPTION] Se asume que ordenar por frecuencia COCA es la heuristica mas util para un LLM. Razon: las palabras mas frecuentes del idioma son mas versatiles y faciles de incorporar en cualquier oracion.
5. Tomar los primeros 10 lemas de la lista ordenada.

Cada lema sugerido incluye:
| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| lemma | Texto | AbsentLemma.lemmaAndPos.lemma | Forma base de la palabra |
| pos | Texto | AbsentLemma.lemmaAndPos.pos | Parte de la oracion |
| reason | Tipo de ausencia | AbsentLemma.absenceType | COMPLETELY_ABSENT o APPEARS_TOO_LATE |
| cocaRank | Entero (opcional) | AbsentLemma.cocaRank | Rango de frecuencia COCA (puede no estar disponible) |

**Error**: N/A (la ausencia de lemas sugeridos no es un error; ver R004)

### Rule[F-RCSL-R004] - Contexto sin lemas sugeridos
**Severity**: major | **Validation**: AUTO_VALIDATED

Si no hay lemas sugeridos disponibles (porque no existe diagnostico de ausencia de lemas en el milestone, o porque todos los lemas del nivel son de tipo APPEARS_TOO_EARLY, o porque el milestone ancestro no se encontro), el contexto de correccion se construye igualmente con la lista de lemas sugeridos vacia. La informacion de la oracion y el diagnostico de longitud siguen siendo suficientes para que un LLM reescriba la oracion ajustando la longitud, aunque sin sugerencias de vocabulario.

**Error**: N/A (la ausencia de sugerencias no impide la correccion)

### Rule[F-RCSL-R005] - Limite de lemas sugeridos
**Severity**: minor | **Validation**: AUTO_VALIDATED

La lista de lemas sugeridos se limita a un maximo de 10 elementos. Este limite mantiene el contexto manejable para el LLM sin sobrecargarlo con opciones. [ASSUMPTION] Se asume que 10 lemas es un limite razonable para la primera iteracion. Razon: provee suficiente variedad sin exceder el espacio de atencion util del LLM. Este valor podria ajustarse en futuras iteraciones basandose en resultados reales.

**Error**: N/A (esta regla define un limite)

---

### Grupo B - Integracion con el comando refiner next

### Rule[F-RCSL-R006] - El comando refiner next incluye el contexto de correccion para tareas SENTENCE_LENGTH
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el comando `refiner next` muestra una tarea de tipo SENTENCE_LENGTH, debe incluir el contexto de correccion ademas de los datos basicos de la tarea. Esto aplica tanto al formato texto como al formato JSON.

Para tareas de otros tipos de diagnostico (LEMMA_ABSENCE, COCA_BUCKETS, etc.), el comando continua mostrando solo los datos basicos de la tarea como hasta ahora. El contexto de correccion es especifico de SENTENCE_LENGTH en esta iteracion.

**Error**: N/A (esta regla define un comportamiento de presentacion)

### Rule[F-RCSL-R007] - Formato JSON del contexto de correccion
**Severity**: critical | **Validation**: AUTO_VALIDATED

En formato JSON, el contexto de correccion se incluye como un campo adicional `correctionContext` en la salida existente del comando `refiner next`. Ejemplo:

```
{
  "taskNumber": 1,
  "totalTasks": 42,
  "id": "task-014",
  "target": "QUIZ",
  "nodeId": "quiz-id-123",
  "nodeLabel": "Quiz 14 - L1.T2.K3",
  "diagnosis": "SENTENCE_LENGTH",
  "priority": 1,
  "status": "PENDING",
  "correctionContext": {
    "sentence": "She plays tennis every afternoon with her friends",
    "translation": "Ella juega tenis todas las tardes con sus amigas",
    "knowledgeTitle": "Affirmative sentences in the present simple",
    "knowledgeInstructions": "Escribe la forma afirmativa",
    "topicLabel": "Present Simple",
    "cefrLevel": "A1",
    "tokenCount": 15,
    "targetRange": {
      "min": 5,
      "max": 8
    },
    "delta": 7,
    "suggestedLemmas": [
      { "lemma": "like", "pos": "VERB", "reason": "COMPLETELY_ABSENT", "cocaRank": 52 },
      { "lemma": "want", "pos": "VERB", "reason": "APPEARS_TOO_LATE", "cocaRank": 89 },
      { "lemma": "big", "pos": "ADJ", "reason": "COMPLETELY_ABSENT", "cocaRank": 201 }
    ]
  }
}
```

Si el contexto de correccion no puede construirse (por ejemplo, porque el reporte de auditoria no se encuentra), el campo `correctionContext` debe ser nulo y se incluye un campo `correctionContextError` con un mensaje descriptivo.

**Error**: "No se pudo construir el contexto de correccion: {motivo}"

### Rule[F-RCSL-R008] - Formato texto del contexto de correccion
**Severity**: major | **Validation**: AUTO_VALIDATED

En formato texto, el contexto de correccion se muestra debajo de los datos basicos de la tarea, separado visualmente. Ejemplo:

```
Next task (#1 of 42):
  Target:    QUIZ
  Node:      Quiz 14 - L1.T2.K3 (id: quiz-id-123)
  Diagnosis: SENTENCE_LENGTH
  Priority:  1
  Status:    PENDING

  Correction context:
    Sentence:     She plays tennis every afternoon with her friends
    Translation:  Ella juega tenis todas las tardes con sus amigas
    Knowledge:    Affirmative sentences in the present simple
    Instructions: Escribe la forma afirmativa
    Topic:        Present Simple
    CEFR Level:   A1
    Tokens:       15 (target: 5-8, delta: +7)
    Suggested lemmas:
      1. like (VERB) - COMPLETELY_ABSENT [COCA #52]
      2. want (VERB) - APPEARS_TOO_LATE [COCA #89]
      3. big (ADJ) - COMPLETELY_ABSENT [COCA #201]
```

Si no hay lemas sugeridos, la seccion "Suggested lemmas" muestra "(none available)". Si el contexto no puede construirse, se muestra un aviso en lugar de la seccion de contexto.

**Error**: N/A (esta regla define un formato de presentacion)

---

### Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio. Se levantaran en futuras iteraciones:

- **No se cruza con COCA Buckets**: los lemas sugeridos no se cruzan con el diagnostico de COCA Buckets (CocaBucketsLevelDiagnosis) para balancear bandas de frecuencia. La seleccion se basa unicamente en el tipo de ausencia y el rango COCA individual del lema.

- **No se valida compatibilidad semantica**: los lemas sugeridos se ofrecen sin garantia de compatibilidad semantica con la oracion actual. El LLM es responsable de elegir cuales usar y como integrarlos.

- **La sentence se entrega como texto plano**: el contexto no incluye la estructura de formulario TEXT/CLOZE del quiz. La transformacion de la sentence generada por el LLM a la estructura de formulario es responsabilidad de una etapa posterior (persistencia).

- **No se define el output del LLM**: este requerimiento solo cubre el contexto de entrada. El formato de la respuesta del LLM, la validacion de esa respuesta, y su persistencia en el curso son requerimientos separados.

- **No se define donde se guarda la correccion**: la persistencia de correcciones en el curso (modelo de propuesta, aplicacion, etc.) es un requerimiento separado.

---

## User Journeys

### Journey[F-RCSL-J001] - LLM recibe contexto para corregir una oracion demasiado larga
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RCSL-J001
    name: LLM recibe contexto para corregir una oracion demasiado larga
    flow:
      - id: solicitar_tarea
        action: "El usuario ejecuta 'refiner next' y obtiene una tarea SENTENCE_LENGTH"
        then: verificar_auditoria

      - id: verificar_auditoria
        action: "El sistema busca el reporte de auditoria asociado al plan usando el sourceAuditId"
        gate: [F-RCSL-R002]
        outcomes:
          - when: "El reporte de auditoria existe y el nodo quiz se localiza correctamente"
            then: obtener_diagnostico
          - when: "El reporte no se encuentra o el nodo quiz no existe en el arbol"
            then: contexto_no_disponible

      - id: obtener_diagnostico
        action: "El sistema extrae la oracion del quiz y el SentenceLengthDiagnosis del nodo"
        gate: [F-RCSL-R001]
        then: buscar_lemas

      - id: buscar_lemas
        action: "El sistema navega al milestone ancestro y obtiene los lemas ausentes del nivel CEFR"
        gate: [F-RCSL-R003, F-RCSL-R005]
        outcomes:
          - when: "Hay lemas ausentes de tipo COMPLETELY_ABSENT o APPEARS_TOO_LATE disponibles"
            then: construir_contexto_completo
          - when: "No hay lemas sugeridos disponibles"
            then: construir_contexto_sin_lemas

      - id: construir_contexto_completo
        action: "El sistema construye el contexto con la oracion, el diagnostico (delta positivo = oracion demasiado larga) y los top 10 lemas sugeridos ordenados por frecuencia COCA"
        gate: [F-RCSL-R001, F-RCSL-R003, F-RCSL-R005]
        then: presentar_contexto

      - id: construir_contexto_sin_lemas
        action: "El sistema construye el contexto con la oracion y el diagnostico, pero sin sugerencias de vocabulario"
        gate: [F-RCSL-R004]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra la tarea junto con el contexto de correccion en el formato solicitado (texto o JSON)"
        gate: [F-RCSL-R006, F-RCSL-R007, F-RCSL-R008]
        result: success

      - id: contexto_no_disponible
        action: "El comando muestra la tarea basica con un aviso de que el contexto de correccion no esta disponible"
        gate: [F-RCSL-R007]
        result: failure
```

### Journey[F-RCSL-J002] - LLM recibe contexto para corregir una oracion demasiado corta
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RCSL-J002
    name: LLM recibe contexto para corregir una oracion demasiado corta
    flow:
      - id: solicitar_tarea
        action: "El usuario ejecuta 'refiner next' y obtiene una tarea SENTENCE_LENGTH cuyo quiz tiene una oracion mas corta que el rango esperado"
        then: verificar_auditoria

      - id: verificar_auditoria
        action: "El sistema busca el reporte de auditoria y localiza el nodo quiz en el arbol"
        gate: [F-RCSL-R002]
        outcomes:
          - when: "El reporte y el nodo quiz se localizan correctamente"
            then: obtener_diagnostico
          - when: "No se puede localizar el reporte o el nodo"
            then: contexto_no_disponible

      - id: obtener_diagnostico
        action: "El sistema extrae la oracion y el diagnostico. El delta es negativo, indicando que la oracion necesita mas tokens para alcanzar el rango minimo"
        gate: [F-RCSL-R001]
        then: buscar_lemas

      - id: buscar_lemas
        action: "El sistema navega al milestone ancestro y busca lemas ausentes del nivel"
        gate: [F-RCSL-R003]
        outcomes:
          - when: "Hay lemas sugeridos disponibles"
            then: construir_contexto_con_lemas
          - when: "No hay lemas sugeridos"
            then: construir_contexto_sin_lemas

      - id: construir_contexto_con_lemas
        action: "El sistema construye el contexto: oracion corta, delta negativo (necesita mas tokens), y lemas sugeridos que el LLM puede usar para alargar la oracion cubriendo vocabulario faltante"
        gate: [F-RCSL-R001, F-RCSL-R003, F-RCSL-R005]
        then: presentar_contexto

      - id: construir_contexto_sin_lemas
        action: "El sistema construye el contexto sin sugerencias de vocabulario. El LLM debera alargar la oracion por su cuenta"
        gate: [F-RCSL-R004]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra la tarea con el contexto de correccion, incluyendo el delta negativo que indica cuantos tokens faltan"
        gate: [F-RCSL-R006]
        result: success

      - id: contexto_no_disponible
        action: "El comando muestra la tarea sin contexto de correccion, con un aviso del motivo"
        result: failure
```

### Journey[F-RCSL-J003] - Contexto de correccion sin lemas sugeridos disponibles
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RCSL-J003
    name: Contexto de correccion sin lemas sugeridos disponibles
    flow:
      - id: solicitar_tarea
        action: "El usuario ejecuta 'refiner next' y obtiene una tarea SENTENCE_LENGTH"
        then: localizar_quiz

      - id: localizar_quiz
        action: "El sistema localiza el nodo quiz en el arbol de auditoria"
        gate: [F-RCSL-R002]
        then: navegar_milestone

      - id: navegar_milestone
        action: "El sistema navega al milestone ancestro del quiz"
        outcomes:
          - when: "El milestone existe pero su LemmaAbsenceLevelDiagnosis indica que todos los lemas esperados estan presentes (no hay ausencias)"
            then: sin_lemas_disponibles
          - when: "El milestone existe pero todas las ausencias son de tipo APPEARS_TOO_EARLY (excluidas por R003)"
            then: sin_lemas_disponibles
          - when: "No se encuentra el milestone ancestro (estructura incompleta)"
            then: sin_lemas_disponibles

      - id: sin_lemas_disponibles
        action: "El sistema construye el contexto con la lista de lemas sugeridos vacia. El LLM recibe la oracion y el diagnostico de longitud, lo cual es suficiente para reescribir la oracion ajustando la cantidad de tokens"
        gate: [F-RCSL-R004]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra la tarea con el contexto de correccion. En formato texto, la seccion de lemas sugeridos indica '(none available)'"
        gate: [F-RCSL-R006, F-RCSL-R008]
        result: success
```

---

## Open Questions

### Doubt[DOUBT-LIMIT] - Limite de lemas sugeridos
**Status**: RESOLVED

Cuantos lemas sugeridos incluir como maximo en el contexto de correccion?

- [x] Opcion A: 10 lemas (equilibrio entre variedad y concision)
- [ ] Opcion B: 5 lemas (mas conciso, menos opciones para el LLM)
- [ ] Opcion C: 20 lemas (mas opciones, pero potencialmente ruidoso)

**Answer**: 10 lemas como limite maximo. Provee suficiente variedad para que el LLM encuentre al menos uno semanticamente compatible con la oracion, sin sobrecargar el contexto. [ASSUMPTION] Razon: es un punto de partida conservador que puede ajustarse basandose en resultados reales.

### Doubt[DOUBT-COCA-NULL] - Ordenamiento de lemas sin rango COCA
**Status**: RESOLVED

Como tratar los lemas que no tienen rango COCA al ordenar por frecuencia?

- [x] Opcion A: Colocarlos al final de la lista (despues de todos los que si tienen rango)
- [ ] Opcion B: Excluirlos de las sugerencias
- [ ] Opcion C: Asignarles un rango por defecto muy alto (por ejemplo, 99999)

**Answer**: Se colocan al final de la lista. [ASSUMPTION] Razon: un lema sin rango COCA puede ser igualmente util; simplemente no se puede priorizar por frecuencia, pero no deberia excluirse.

### Doubt[DOUBT-SOURCE-AUDIT] - Propagacion del sourceAuditId
**Status**: OPEN

El campo sourceAuditId del RefinementPlan actualmente llega como cadena vacia al crear un plan. Este campo es imprescindible para esta feature.

**Pregunta**: Se corrige como parte de esta feature o como un fix previo independiente?

- [ ] Opcion A: Corregir como prerequisito independiente antes de implementar esta feature. Mantiene la separacion de concerns.
- [x] Opcion B: Corregir como parte de esta feature. Reduce el numero de iteraciones pero mezcla un bugfix con una nueva funcionalidad.
