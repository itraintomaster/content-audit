---
feature:
  id: FEAT-CSLATDC
  code: F-CSLATDC
  name: Consulta de Suggested Lemmas a través del CLI
  priority: critical
---

# Consulta de Suggested Lemmas a través del CLI

## TL;DR

Se amplía `GetCommand.get(...)` para consultar `Suggested Lemmas` bajo demanda durante la revisión de una `Task/Quiz`.  
La consulta se identifica por `taskId`, permite pedir un `limit`, filtrar por `Part of Speech` y `Level`, y siempre devuelve resultados priorizados.  
El cambio complementa los `suggestedLemmas` estáticos del `correctionContext` sin crear un nuevo verbo CLI.

## Recorrido de uso (estado actual)

Hoy el cliente puede generar o recibir contexto de corrección usando `PlanCommand.plan(..., withCorrectionContext = true)` y luego ejecutar la revisión con `ReviseCommand.revise(taskId, planId, correctionContextJson, correctionContextFilePath)`. En ese recorrido, los `suggestedLemmas` llegan como parte estática del `correctionContext`.

Cuando el agente revisor detecta que esos `suggestedLemmas` estáticos no cubren la necesidad de la quiz —por ejemplo, la quiz necesita un verbo y la lista disponible no contiene verbos útiles— no existe hoy una consulta pública específica para pedir más `Suggested Lemmas`. `GetCommand.get(resource, name, filter)` ya es el verbo público natural para leer recursos consultables como tasks, plans, audits o analyzers, pero todavía no expone `"suggested-lemmas"` como recurso independiente.

La fricción funcional es que el agente queda limitado al top estático incluido previamente en el `correctionContext`, aunque necesite más candidatos, un tipo gramatical específico o un filtrado por `Level`. Resolverlo mediante `proposalId` o mediante versiones proyectadas temporales queda fuera de este primer alcance.

**Cambio mínimo necesario**: extender el verbo público existente `GetCommand.get(...)` para soportar `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(limit, partOfSpeech, level))`, usando `taskId` como identidad de la `Task/Quiz` en revisión, aplicando siempre filtrado por `Level`, filtrado opcional por `Part of Speech`, límite máximo solicitado y orden por prioridad de recomendación.

## Business Rules

### Rule[F-CSLATDC-R001] - Exposición pública de suggested lemmas por task
**Severity**: critical | **Validation**: VALIDATED

Cuando un cliente invoca `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(...))`, el sistema debe devolver los `Suggested Lemmas` aplicables a la `Task/Quiz` identificada por `taskId`. La identidad pública de la consulta es la task en revisión, no una propuesta.

**Error**: "No aplica."

### Rule[F-CSLATDC-R002] - Uso del verbo existente get
**Severity**: major | **Validation**: VALIDATED

La consulta dinámica de `Suggested Lemmas` debe resolverse extendiendo `GetCommand.get(...)` con el recurso `"suggested-lemmas"`. Esta primera versión no debe introducir un nuevo verbo CLI para cubrir la misma necesidad.

**Error**: "No aplica."

### Rule[F-CSLATDC-R003] - Filtrado obligatorio por level
**Severity**: critical | **Validation**: VALIDATED

Toda respuesta exitosa de `GetCommand.get("suggested-lemmas", <taskId>, ...)` debe incluir únicamente lemas adecuados al `Level` aplicable a la `Task/Quiz`. El sistema no debe devolver sugerencias sin aplicar un filtro de nivel.

**Error**: "No se pueden sugerir lemas sin un nivel aplicable para filtrar." [ASSUMPTION]

### Rule[F-CSLATDC-R004] - Inferencia de level desde la task
**Severity**: critical | **Validation**: VALIDATED

Cuando `GetCommand.get("suggested-lemmas", <taskId>, ...)` se invoca sin un `level` explícito, el sistema debe intentar inferir el `Level` desde la `Task/Quiz` o desde su contexto disponible. Si logra inferirlo, debe usar ese nivel para filtrar los resultados.

**Error**: "No aplica."

### Rule[F-CSLATDC-R005] - Validación cuando no hay level disponible
**Severity**: critical | **Validation**: VALIDATED

Cuando `GetCommand.get("suggested-lemmas", <taskId>, ...)` se invoca sin `level` explícito y el sistema no puede inferir el `Level`, la consulta debe rechazarse con una validación clara. El sistema no debe devolver `Suggested Lemmas` sin filtro de nivel.

**Error**: "No se pueden sugerir lemas porque falta el nivel necesario para filtrar." [ASSUMPTION]

### Rule[F-CSLATDC-R006] - Uso de level explícito cuando no puede inferirse
**Severity**: major | **Validation**: VALIDATED

Cuando `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(..., level = <level>))` incluye un `level` explícito y el sistema no puede inferir el `Level` desde la task, debe usar el nivel informado para filtrar los `Suggested Lemmas`.

**Error**: "No aplica."

### Rule[F-CSLATDC-R007] - Filtro por partOfSpeech
**Severity**: major | **Validation**: VALIDATED

Cuando `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(..., partOfSpeech = <partOfSpeech>))` incluye `partOfSpeech`, el sistema debe devolver únicamente lemas cuyo `Part of Speech` coincida con el filtro solicitado. El sistema no debe ampliar ni ignorar silenciosamente ese filtro.

**Error**: "No aplica."

### Rule[F-CSLATDC-R008] - Límite máximo solicitado
**Severity**: major | **Validation**: VALIDATED

Cuando `GetCommand.get("suggested-lemmas", <taskId>, GetTasksFilter(limit = N, ...))` incluye `limit = N`, el sistema debe devolver como máximo `N` `Suggested Lemmas` que cumplan los filtros aplicables.

**Error**: "No aplica."

### Rule[F-CSLATDC-R009] - Lista parcial sin error
**Severity**: major | **Validation**: VALIDATED

Cuando la cantidad de `Suggested Lemmas` disponibles para el `Level`, el `Part of Speech` y los demás criterios soportados sea menor que el `limit` solicitado, el sistema debe devolver todos los resultados disponibles sin considerar la situación como error.

**Error**: "No aplica."

### Rule[F-CSLATDC-R010] - Ordenamiento por prioridad
**Severity**: critical | **Validation**: VALIDATED

Toda respuesta exitosa con uno o más `Suggested Lemmas` debe devolverlos en orden de prioridad de recomendación. El cliente o agente revisor no debe necesitar ordenar los resultados por su cuenta.

**Error**: "No aplica."

### Rule[F-CSLATDC-R011] - Consulta no limitada al top estático del correctionContext
**Severity**: major | **Validation**: VALIDATED

Cuando el agente solicita más lemas que los incluidos originalmente en `correctionContext.suggestedLemmas`, `GetCommand.get("suggested-lemmas", <taskId>, ...)` debe poder devolver una lista más amplia que el conjunto estático histórico, respetando `limit`, `Level`, `Part of Speech` y orden de prioridad.

**Error**: "No aplica."

### Rule[F-CSLATDC-R012] - Tasks sin suggested lemmas aplicables
**Severity**: major | **Validation**: VALIDATED

Cuando una `Task/Quiz` no tenga una base funcional compatible para sugerir lemas, `GetCommand.get("suggested-lemmas", <taskId>, ...)` debe devolver una respuesta vacía con una explicación funcional clara. El sistema no debe inventar `Suggested Lemmas`.

**Error**: "No hay lemas sugeridos disponibles para esta task." [ASSUMPTION]

### Rule[F-CSLATDC-R014] - Resultado mínimo observable
**Severity**: major | **Validation**: VALIDATED

Toda respuesta exitosa de `GetCommand.get("suggested-lemmas", <taskId>, ...)` debe permitir identificar cada lema sugerido. El orden de aparición en la respuesta representa su prioridad relativa.

**Error**: "No aplica."

<a id="F-CSLATDC-R015"></a>
### Rule[F-CSLATDC-R015] - La consulta dinámica opera sobre el MISMO análisis fuente del que se derivó el contexto de la task
**Severity**: critical | **Validation**: VALIDATED

> Cuando se consulta `GetCommand.get("suggested-lemmas", <taskId>, ...)`, los `Suggested Lemmas` que devuelve el sistema deben derivarse del **mismo análisis fuente** del que se derivó el contexto de corrección de esa `Task/Quiz` —el análisis que el operador seleccionó para la revisión que está en curso, incluido el análisis seleccionado explícitamente con `--audit <id>`—. El resultado de la consulta dinámica no debe depender de "cuál es el análisis más reciente" ni de un análisis fijo resuelto por su cuenta, sino del análisis fuente de la task consultada.

<details><summary>Detalle</summary>

Esta regla cierra una inconsistencia entre dos vías por las que el agente recibe vocabulario candidato durante la revisión de una task `LEMMA_ABSENCE`:

1. La lista **estática** que viaja en el `correctionContext` de la task, derivada del análisis fuente de esa task (el análisis seleccionado para la revisión).
2. La consulta **dinámica** `get suggested-lemmas`, que el agente puede pedir para traer más candidatos.

El comportamiento previo resolvía la consulta dinámica contra un análisis elegido por su cuenta (el más reciente, o un análisis fijo), ignorando cuál es el análisis fuente de la task que se está revisando. Cuando el operador apunta la revisión a un análisis **distinto** del más reciente —en particular un análisis **proyectado**, persistido por el cliente y seleccionado con `--audit <id>` ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)), que ya refleja el efecto de las propuestas activas y pendientes ([FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md), [FEAT-CDIFF](../2026-05-06.01_consolidated-differential-view/REQUIREMENT.md))— las dos vías quedaban inconsistentes: la lista estática reflejaba el análisis proyectado, pero la consulta dinámica seguía devolviendo lemas del análisis viejo. La consecuencia observable es que la consulta dinámica reintroduce lemas que el análisis proyectado ya consideraba consumidos (por ejemplo, la palabra "butter" reaparece una y otra vez aunque una propuesta previa ya la haya consumido).

**Contrato observable**: para una misma `Task/Quiz`, los lemas que devuelve `get suggested-lemmas` deben ser consistentes con el análisis fuente del que se derivó el contexto de esa task. Verificable comparando, para una misma task:

- Si el contexto de esa task se derivó del análisis A, la consulta dinámica para esa task devuelve lemas del análisis A (mismo universo de exposición), no de otro análisis.
- Si se revisa una task cuyo análisis fuente es un análisis **proyectado** (donde un lema ya fue consumido por una propuesta previa), la consulta dinámica refleja ese análisis proyectado: ese lema ya consumido NO reaparece como candidato sub-expuesto, en paridad con lo que muestra la lista estática del mismo contexto.
- El resultado de la consulta dinámica para esa task NO cambia según cuál análisis sea el más reciente en el sistema al momento de consultar.

Esta regla NO cambia los filtros existentes (`Level`, `Part of Speech`, `limit`, orden por prioridad): sólo fija **de qué análisis** se obtiene el universo de candidatos. La opción de incluir lemas ya expuestos ([FEAT-INCEXP](../2026-06-18.01_incluir-lemas-ya-expuestos-en-suggested-lemmas/REQUIREMENT.md)) sigue operando, ahora sobre el universo del análisis fuente correcto.

**Error**: "No aplica."

</details>

## Context

Los `Suggested Lemmas` ya existen como parte del `correctionContext` usado en revisión, especialmente en escenarios vinculados a `LEMMA_ABSENCE`. Ese diseño funciona cuando la lista estática alcanza para orientar al agente, pero no cubre bien quizzes que necesitan un tipo gramatical concreto o una cantidad mayor de candidatos.

La necesidad funcional no es explorar tareas, planes o propuestas de manera externa. La necesidad es puntual: durante la revisión de una `Task/Quiz`, el agente debe poder pedir más candidatos relevantes y priorizados para completar la revisión con mejores insumos.

La solución se mantiene como una ampliación mínima del contrato público existente: se reutiliza `GetCommand.get(...)`, se agrega el recurso `"suggested-lemmas"` y se usa `taskId` como identidad pública. La consulta complementa el `correctionContext`; no redefine el contenido estático existente.

## Scope

### IN

- Extender `GetCommand.get(...)` con el recurso `"suggested-lemmas"`.
- Identificar la consulta por `taskId`.
- Permitir `limit` como cantidad máxima solicitada.
- Soportar filtro por `partOfSpeech`.
- Soportar filtrado por `level`.
- Inferir el `Level` desde la `Task/Quiz` o su contexto cuando sea posible.
- Permitir `level` explícito cuando no pueda inferirse.
- Devolver resultados siempre en orden de prioridad.
- Devolver menos resultados que el `limit` sin error.
- Devolver lista vacía con explicación funcional cuando no haya `Suggested Lemmas` aplicables.
- Derivar los `Suggested Lemmas` del **mismo análisis fuente** del que se derivó el contexto de la `Task/Quiz` consultada (incluido un análisis proyectado seleccionado con `--audit <id>`), sin depender de cuál sea el análisis más reciente.

### OUT

- No se agrega soporte inicial para `proposalId`.
- No se modelan los `Suggested Lemmas` como pertenecientes a una propuesta ya generada.
- No se resuelve en esta iteración el acceso a versiones proyectadas temporales generadas por otros flujos.
- No se agregan filtros léxicos adicionales fuera de `partOfSpeech`, `level` y `limit`.
- No se redefine el contenido estático de `correctionContext.suggestedLemmas`.
- No se incluye un flujo de exploración externa de tasks, planes o propuestas.

## User Journeys

### Journey[F-CSLATDC-J001] - El agente pide lemas sugeridos durante la revisión

```yaml
flow:
  - id: task_under_review
    action: "Existe una Task/Quiz identificada por taskId y sus suggestedLemmas estáticos no cubren la necesidad de la quiz"
    then: request_suggested_lemmas

  - id: request_suggested_lemmas
    action: "El agente invoca GetCommand.get(\"suggested-lemmas\", taskId, GetTasksFilter(limit = N, partOfSpeech = \"VERB\"))"
    then: determine_level

  - id: determine_level
    action: "El sistema determina el Level aplicable para la consulta"
    gate: [F-CSLATDC-R003, F-CSLATDC-R004]
    outcomes:
      - when: "El Level puede inferirse desde la Task/Quiz o su contexto"
        then: apply_supported_filters
      - when: "El Level no puede inferirse y no fue informado explícitamente"
        then: reject_missing_level

  - id: apply_supported_filters
    action: "El sistema aplica Level, partOfSpeech y limit sobre los Suggested Lemmas disponibles"
    gate: [F-CSLATDC-R007, F-CSLATDC-R008]
    outcomes:
      - when: "Existen al menos N lemas que cumplen los filtros"
        then: return_limited_priority_results
      - when: "Existen menos de N lemas que cumplen los filtros"
        then: return_partial_priority_results
      - when: "No existen lemas que cumplan los filtros"
        then: return_empty_available_results

  - id: return_limited_priority_results
    action: "La respuesta contiene como máximo N lemas con Part of Speech VERB, filtrados por Level y ordenados por prioridad"
    gate: [F-CSLATDC-R010, F-CSLATDC-R014]
    result: success

  - id: return_partial_priority_results
    action: "La respuesta contiene todos los lemas disponibles que cumplen los filtros, aunque sean menos que N, ordenados por prioridad"
    gate: [F-CSLATDC-R009, F-CSLATDC-R010, F-CSLATDC-R014]
    result: success

  - id: return_empty_available_results
    action: "La respuesta es una lista vacía con una explicación funcional de que no hay Suggested Lemmas disponibles para los filtros aplicados"
    gate: [F-CSLATDC-R012]
    result: success

  - id: reject_missing_level
    action: "La respuesta rechaza la consulta con una validación clara porque falta el Level necesario para filtrar"
    gate: [F-CSLATDC-R005]
    result: failure
```

### Journey[F-CSLATDC-J002] - El agente informa level explícito cuando no puede inferirse

```yaml
flow:
  - id: task_without_inferable_level
    action: "Existe una Task/Quiz identificada por taskId cuyo Level no puede inferirse desde la task ni desde su contexto disponible"
    then: request_with_explicit_level

  - id: request_with_explicit_level
    action: "El agente invoca GetCommand.get(\"suggested-lemmas\", taskId, GetTasksFilter(limit = N, partOfSpeech = \"NOUN\", level = levelSolicitado))"
    then: apply_explicit_level

  - id: apply_explicit_level
    action: "El sistema usa el level explícito para filtrar los Suggested Lemmas"
    gate: [F-CSLATDC-R003, F-CSLATDC-R006]
    then: apply_requested_filters

  - id: apply_requested_filters
    action: "El sistema aplica partOfSpeech, limit y orden de prioridad"
    gate: [F-CSLATDC-R007, F-CSLATDC-R008, F-CSLATDC-R010]
    outcomes:
      - when: "Existen lemas que cumplen el level explícito y el Part of Speech solicitado"
        then: return_filtered_priority_results
      - when: "No existen lemas que cumplan el level explícito y el Part of Speech solicitado"
        then: return_empty_filtered_results

  - id: return_filtered_priority_results
    action: "La respuesta contiene hasta N lemas que cumplen level y Part of Speech, ordenados por prioridad"
    gate: [F-CSLATDC-R014]
    result: success

  - id: return_empty_filtered_results
    action: "La respuesta es una lista vacía con una explicación funcional de que no hay Suggested Lemmas disponibles para esos filtros"
    gate: [F-CSLATDC-R012]
    result: success
```

### Journey[F-CSLATDC-J004] - La consulta dinámica supera el conjunto estático del correctionContext

```yaml
flow:
  - id: static_context_available
    action: "Existe una Task/Quiz con correctionContext que incluye suggestedLemmas estáticos insuficientes para la necesidad de la quiz"
    then: request_more_than_static_context

  - id: request_more_than_static_context
    action: "El agente invoca GetCommand.get(\"suggested-lemmas\", taskId, GetTasksFilter(limit = N, partOfSpeech = partOfSpeechSolicitado, level = levelSolicitado)) con N mayor que la cantidad estática disponible"
    then: dynamic_query_scope

  - id: dynamic_query_scope
    action: "El sistema resuelve la consulta dinámica sin limitarse al conjunto estático histórico del correctionContext"
    gate: [F-CSLATDC-R011]
    then: return_dynamic_results

  - id: return_dynamic_results
    action: "La respuesta contiene hasta N Suggested Lemmas que cumplen los filtros aplicables y vienen ordenados por prioridad"
    gate: [F-CSLATDC-R008, F-CSLATDC-R010, F-CSLATDC-R014]
    result: success
```

### Journey[F-CSLATDC-J005] - La consulta dinámica refleja el análisis fuente de la task, no el más reciente

```yaml
flow:
  - id: task_from_selected_analysis
    action: "Existe una Task/Quiz identificada por taskId cuyo contexto de corrección se derivó de un análisis fuente seleccionado para la revisión, donde un lema candidato del Level y Part of Speech pedidos ya fue consumido por una propuesta previa; además existe en el sistema otro análisis más reciente donde ese mismo lema sigue disponible"
    then: request_suggested_lemmas

  - id: request_suggested_lemmas
    action: "El agente consulta GetCommand.get(\"suggested-lemmas\", taskId, GetTasksFilter(limit = N, partOfSpeech = partOfSpeechSolicitado)) para esa task"
    then: resolve_source_analysis

  - id: resolve_source_analysis
    action: "El sistema determina de qué análisis obtiene el universo de candidatos para la consulta"
    gate: [F-CSLATDC-R015]
    outcomes:
      - when: "El análisis fuente de la task es el más reciente del sistema"
        then: return_consistent_with_source
      - when: "El análisis fuente de la task es distinto del más reciente (por ejemplo, un análisis proyectado seleccionado para la revisión)"
        then: return_consistent_with_source

  - id: return_consistent_with_source
    action: "La respuesta contiene lemas derivados del análisis fuente de la task: el lema ya consumido por la propuesta previa NO reaparece como candidato sub-expuesto, en paridad con la lista estática del contexto de esa misma task, independientemente de cuál sea el análisis más reciente"
    gate: [F-CSLATDC-R015, F-CSLATDC-R010, F-CSLATDC-R014]
    result: success
```

## Open Questions

### Doubt[DOUBT-PROJECTED-CONTEXT] - Integración con versiones proyectadas temporales
**Status**: RESUELTA PARCIALMENTE (2026-06-23)

El punto central de consistencia quedó resuelto por [F-CSLATDC-R015](#F-CSLATDC-R015): la consulta dinámica opera sobre el **mismo análisis fuente** del que se derivó el contexto de la task —incluido un análisis proyectado seleccionado para la revisión con `--audit <id>`—, y ya no contra el análisis más reciente resuelto por su cuenta. Eso elimina la inconsistencia entre la lista estática del `correctionContext` y la consulta dinámica cuando el operador apunta la revisión a un análisis proyectado.

Lo que queda diferido es estrictamente más acotado: la integración con planes efímeros que **no** persisten un análisis direccionable (un plan efímero sin análisis fuente resoluble por `--audit`, [FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) y cualquier forma de pasar un contexto proyectado inline a la consulta dinámica sin un análisis persistido detrás. R015 sólo exige consistencia con un análisis fuente **resoluble**; el caso "no hay análisis fuente resoluble" sigue fuera de alcance.

### Doubt[DOUBT-RESPONSE-METADATA] - Datos adicionales por resultado

No quedó cerrado si cada resultado debe incluir datos adicionales además del lema y su posición priorizada, por ejemplo `lemmaCount`, `lemmaCountThreshold`, `isUnderexposed` o un score explícito de prioridad.

### Doubt[DOUBT-LIMIT-DEFAULT-MAX] - Default y máximo permitido para limit

No quedó definido si `limit` tendrá valor por defecto cuando no sea informado, ni si existirá un máximo permitido.

### Doubt[DOUBT-LEVEL-CONFLICT] - Conflicto entre level inferido y level explícito

No quedó definido qué debe ocurrir si el cliente informa un `level` explícito que contradice el nivel inferido desde la task.

### Doubt[DOUBT-STATIC-DUPLICATES] - Duplicados respecto del correctionContext estático

No quedó definido si la consulta dinámica debe excluir lemas que ya estaban presentes en `correctionContext.suggestedLemmas` o si puede devolverlos nuevamente cuando sigan siendo prioritarios.

### Doubt[DOUBT-EXACT-ERROR-COPY] - Textos exactos de validación

No se acordaron textos exactos de error; solo se acordó que las validaciones deben ser claras y funcionales.

## References

- FEAT-RCLA: define `correctionContext.suggestedLemmas` para tareas vinculadas a `LEMMA_ABSENCE`.
- FEAT-RCLAQS: agrega `quizSentence` al contexto de `LEMMA_ABSENCE`.
- FEAT-LAPS / FEAT-LAGEN: consumen el contexto de corrección, incluidos `suggestedLemmas`, para asistir la generación de propuestas.
- FEAT-SLEM: enriqueció los `suggestedLemmas` con señales de exposición y prioridad, sin introducir un comando público nuevo.
- FEAT-PLANEF: permite obtener `correctionContext` proyectado en planes efímeros, y persistir un análisis proyectado direccionable por `--audit <id>`. Cuando ese análisis proyectado es el análisis fuente de la task, la consulta dinámica opera sobre él por [F-CSLATDC-R015](#F-CSLATDC-R015).
- FEAT-REVCTX: permite que `revise` reciba un `correctionContext` externo (proyectado) para la fase de revisión; F-CSLATDC-R015 es la pieza simétrica que mantiene consistente la **consulta dinámica** de lemas con el análisis fuente de esa misma task.
- FEAT-CDIFF: define la vista consolidada/proyectada de un análisis con sus propuestas aceptadas y pendientes; ese análisis proyectado es uno de los análisis fuente que [F-CSLATDC-R015](#F-CSLATDC-R015) debe respetar cuando la revisión apunta a él.
- FEAT-INCEXP: agrega la opción de incluir lemas ya expuestos en la consulta dinámica; opera sobre el universo del análisis fuente fijado por [F-CSLATDC-R015](#F-CSLATDC-R015).
- FEAT-CLIRV: define el selector `--audit <id>` ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) con el que el operador apunta la revisión a un análisis fuente explícito. Citado por [F-CSLATDC-R015](#F-CSLATDC-R015).