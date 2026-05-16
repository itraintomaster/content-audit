---
feature:
  id: FEAT-LEMMA-SUGGESTIONS
  code: F-SLEM
  name: LemmaCount como variable de SuggestedLemmas
  priority: critical
---

# LemmaCount como variable de SuggestedLemmas

## TL;DR

Agregar la señal de `lemma-count` a `correctionContext.suggestedLemmas` en tareas `LEMMA_ABSENCE`.  
El cambio prioriza lemas ya introducidos pero sub-expuestos antes que lemas completamente ausentes.  
La funcionalidad se observa mediante `AuditRunner.runAudit(...)`, `AuditRunner.runDetailedAudit(...)`, `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)`.  
No se introduce un comando público nuevo.

## Recorrido de uso (estado actual)

1. El cliente ejecuta `AuditRunner.runAudit(coursePath, analyzerNames)` o `AuditRunner.runDetailedAudit(coursePath, analyzerNames)` para auditar un curso.
2. Si incluye el análisis que produce `LEMMA_ABSENCE`, la auditoría permite generar tareas de revisión relacionadas con lemas ausentes o mal ubicados.
3. Si también incluye `lemma-count`, la auditoría dispone de señal sobre lemas que aparecen en menos oraciones distintas que el umbral configurado.
4. El cliente ejecuta `PlanCommand.plan(auditId, storageMode, true)` para proyectar o crear tareas con `correctionContext`.
5. Hoy, en tareas `LEMMA_ABSENCE`, `correctionContext.suggestedLemmas` se basa en los diagnósticos de ausencia o ubicación temporal de lemas, con orden por COCA ascendente y límite existente.
6. El cliente puede observar esas tareas mediante `GetCommand.get("tasks", name, filter)`.
7. El cliente puede ejecutar `ReviseCommand.revise(taskId, planId, null, null)` para que la revisión use el `correctionContext` derivado por el sistema.
8. El cliente también puede ejecutar `ReviseCommand.revise(taskId, planId, correctionContextJson, null)` o `ReviseCommand.revise(taskId, planId, null, correctionContextFilePath)` para aportar un `correctionContext` externo con la misma forma observable que devuelve `GetCommand.get(...)`.

**Punto de fricción:** aunque `lemma-count` ya identifica lemas sub-expuestos, esa señal no participa en `suggestedLemmas`. Como resultado, las sugerencias pueden priorizar palabras completamente nuevas antes que palabras ya introducidas que todavía no alcanzan el mínimo de exposición esperado.

**Cambio mínimo necesario:** enriquecer y reordenar `task.correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE` usando la señal disponible de `lemma-count`, sin crear un verbo público nuevo, conservando el límite existente de sugerencias y manteniendo paridad entre `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)`.

## Business Rules

### Rule[F-SLEM-R001] - La auditoría con lemma-count provee la señal funcional para suggestedLemmas
**Severity**: critical | **Validation**: VALIDATED

Cuando `AuditRunner.runAudit(...)` o `AuditRunner.runDetailedAudit(...)` se ejecuta incluyendo `lemma-count` junto con el análisis que produce diagnósticos `LEMMA_ABSENCE`, el resultado de auditoría debe dejar disponible la señal de `lemma-count` para identificar lemas con exposición menor al umbral aplicable.

Esa señal debe ser la fuente funcional usada después por `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)` para enriquecer `correctionContext.suggestedLemmas`.

**Error**: "No se pudo usar la señal de lemma-count disponible en la auditoría para enriquecer suggestedLemmas"

### Rule[F-SLEM-R002] - Plan con correctionContext enriquece suggestedLemmas para LEMMA_ABSENCE
**Severity**: critical | **Validation**: VALIDATED

Cuando `PlanCommand.plan(auditId, storageMode, true)` genera tareas desde una auditoría con diagnósticos `LEMMA_ABSENCE` y señal disponible de `lemma-count`, cada tarea `LEMMA_ABSENCE` debe exponer `correctionContext.suggestedLemmas` enriquecido con esa señal.

La lista debe poder incluir lemas sub-expuestos por `lemma-count` aunque esos lemas no tengan diagnóstico propio `LEMMA_ABSENCE`, siempre evaluados en su nivel correspondiente.

**Error**: "Las sugerencias de lemas no incorporan la señal disponible de lemma-count"

### Rule[F-SLEM-R003] - Ranking principal prioriza lemas sub-expuestos ya introducidos
**Severity**: critical | **Validation**: VALIDATED

Cuando el sistema construye `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE` y existe señal de `lemma-count`, los lemas sugeridos deben ordenarse por estos grupos de prioridad:

1. Lemas con `lemmaCount < threshold` y `APPEARS_TOO_LATE`.
2. Lemas con `lemmaCount < threshold` y `APPEARS_TOO_EARLY`, evaluados en el nivel target real.
3. Lemas con `lemmaCount < threshold` sin diagnóstico `LEMMA_ABSENCE`.
4. Lemas con `lemmaCount < threshold` y otro tipo de `LEMMA_ABSENCE`, si aplica.
5. Lemas con `LEMMA_ABSENCE` sin sub-exposición por `lemma-count`, dejando `COMPLETELY_ABSENT` al final.

**Error**: "El ranking de suggestedLemmas no respeta la prioridad acordada con lemma-count"

### Rule[F-SLEM-R004] - APPEARS_TOO_EARLY se evalúa en el nivel target real
**Severity**: critical | **Validation**: VALIDATED

Cuando un lema tiene diagnóstico `APPEARS_TOO_EARLY` y existe señal de `lemma-count`, la exposición mínima debe evaluarse contra el nivel target real del lema.

Las apariciones tempranas no deben considerarse suficientes para satisfacer la exposición esperada en el nivel correcto.

**Error**: "APPEARS_TOO_EARLY fue evaluado fuera de su nivel target real"

### Rule[F-SLEM-R005] - COCA ascendente desempata dentro de cada grupo de prioridad
**Severity**: major | **Validation**: VALIDATED

Cuando dos o más lemas pertenecen al mismo grupo de prioridad de `correctionContext.suggestedLemmas`, el sistema debe ordenarlos por COCA ascendente.

El sistema no debe usar mayor déficit de exposición como desempate dentro del mismo grupo.

**Error**: "El desempate de suggestedLemmas no respeta COCA ascendente"

### Rule[F-SLEM-R006] - El límite existente de suggestedLemmas se aplica después del nuevo ranking
**Severity**: major | **Validation**: VALIDATED

Cuando el sistema construye `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE`, debe aplicar primero el ranking con `lemma-count` y después conservar el límite existente de elementos sugeridos.

El enriquecimiento con `lemma-count` no debe aumentar el tamaño máximo observable de `suggestedLemmas`.

**Error**: "El límite de suggestedLemmas no se aplicó después del ranking con lemma-count"

### Rule[F-SLEM-R007] - COMPLETELY_ABSENT queda detrás de lemas ya introducidos
**Severity**: critical | **Validation**: VALIDATED

Cuando `correctionContext.suggestedLemmas` contiene lemas `COMPLETELY_ABSENT` y lemas ya introducidos pero sub-expuestos, los lemas `COMPLETELY_ABSENT` deben quedar detrás de los lemas que necesitan alcanzar el mínimo de exposición.

Para un lema `COMPLETELY_ABSENT` la señal de `lemma-count` no es aplicable porque el curso nunca introdujo ese lema. En consecuencia, el elemento de `suggestedLemmas` correspondiente a un lema `COMPLETELY_ABSENT` no expone valor observable de `lemmaCount`, ni de `lemmaCountThreshold`, ni de `isUnderexposed` (los tres quedan no informados), incluso cuando la auditoría sí incluyó `lemma-count`.

**Error**: "COMPLETELY_ABSENT fue priorizado antes que un lema ya introducido y sub-expuesto"

### Rule[F-SLEM-R008] - Plan proyectado y consulta de tareas muestran el mismo correctionContext enriquecido
**Severity**: major | **Validation**: VALIDATED

Cuando una tarea `LEMMA_ABSENCE` con `correctionContext` enriquecido se observa mediante `PlanCommand.plan(..., true)` o mediante `GetCommand.get("tasks", name, filter)`, la forma observable y el orden de `task.correctionContext.suggestedLemmas` deben ser consistentes entre ambos caminos.

**Error**: "El correctionContext observado en el plan no coincide con el observado en la consulta de tareas"

### Rule[F-SLEM-R009] - Revise consume el correctionContext enriquecido derivado por el sistema
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(taskId, planId, null, null)` se ejecuta sobre una tarea `LEMMA_ABSENCE` cuyo `correctionContext` fue generado por el sistema, la revisión debe consumir `correctionContext.suggestedLemmas` enriquecido y ordenado con la señal de `lemma-count`.

**Error**: "La revisión no recibió el correctionContext enriquecido con lemma-count"

### Rule[F-SLEM-R010] - Revise acepta override externo válido con la forma observable enriquecida
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(taskId, planId, correctionContextJson, null)` o `ReviseCommand.revise(taskId, planId, null, correctionContextFilePath)` recibe un `correctionContext` externo para una tarea `LEMMA_ABSENCE`, el sistema debe aceptar la misma forma observable de `suggestedLemmas` enriquecido que devuelve `GetCommand.get(...)`, siempre que la señal de `lemma-count` sea válida y consistente.

La validación del override usa la forma observable definida por [F-SLEM-R013](#F-SLEM-R013): cada elemento de `suggestedLemmas` puede traer `lemmaCount`, `lemmaCountThreshold` y `isUnderexposed`, o bien tener esos tres valores no informados (señal no aplicable o no disponible).

**Error**: "El correctionContext externo no coincide con la forma válida de suggestedLemmas enriquecido"

### Rule[F-SLEM-R011] - Revise rechaza override externo con señal de lemma-count inconsistente
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(...)` recibe un `correctionContext` externo con datos inconsistentes de `lemma-count` dentro de `suggestedLemmas`, el sistema debe rechazar el override como `correctionContext` inválido y no debe aplicar la revisión con esos datos.

Ejemplos de inconsistencia funcional incluyen conteos negativos o una marca de sub-exposición incompatible con el umbral informado.

**Error**: "El correctionContext es inválido por inconsistencia en la señal de lemma-count dentro de suggestedLemmas"

### Rule[F-SLEM-R012] - Auditoría sin lemma-count conserva suggestedLemmas sin fallar
**Severity**: major | **Validation**: VALIDATED

Cuando `PlanCommand.plan(..., true)` o `GetCommand.get("tasks", ...)` observa tareas `LEMMA_ABSENCE` derivadas de una auditoría que no incluyó `lemma-count`, el sistema debe seguir devolviendo `correctionContext.suggestedLemmas` para `LEMMA_ABSENCE`.

La señal de `lemma-count` debe quedar claramente no disponible y no debe causar falla en la generación, proyección o consulta del contexto. Concretamente, cuando la auditoría no incluyó `lemma-count`, ningún elemento de `correctionContext.suggestedLemmas` expone valor observable de `lemmaCount`, ni de `lemmaCountThreshold`, ni de `isUnderexposed` (los tres quedan no informados en todos los lemas sugeridos). El orden y el límite siguen aplicándose con los demás criterios disponibles ([F-SLEM-R005](#F-SLEM-R005), [F-SLEM-R006](#F-SLEM-R006)).

**Error**: "La ausencia de lemma-count impidió generar o consultar suggestedLemmas"

### Rule[F-SLEM-R013] - Forma observable de la señal de lemma-count dentro de suggestedLemmas
**Severity**: critical | **Validation**: VALIDATED

Cuando un cliente observa `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE` mediante `PlanCommand.plan(..., true)`, `GetCommand.get("tasks", ...)` o `ReviseCommand.revise(...)`, cada elemento expone como mucho tres valores asociados a la señal de `lemma-count`:

1. `lemmaCount`: cantidad observada de exposiciones del lema relevante para el nivel evaluado.
2. `lemmaCountThreshold`: umbral mínimo de exposición usado para decidir sub-exposición en ese nivel.
3. `isUnderexposed`: indicador de si `lemmaCount` está por debajo de `lemmaCountThreshold`.

Los tres valores son opcionales y siempre se comportan en bloque:

- Cuando la auditoría incluyó `lemma-count` y la señal aplica al lema sugerido, los tres valores están informados y son consistentes entre sí (es decir, `isUnderexposed` coincide con la comparación entre `lemmaCount` y `lemmaCountThreshold`).
- Cuando la auditoría no incluyó `lemma-count` ([F-SLEM-R012](#F-SLEM-R012)) o la señal no es aplicable al lema (por ejemplo `COMPLETELY_ABSENT`, [F-SLEM-R007](#F-SLEM-R007)), los tres valores quedan no informados.

No se exponen otros valores derivados de `lemma-count` (por ejemplo, déficit explícito) en `suggestedLemmas`.

**Error**: "La forma observable de lemma-count dentro de suggestedLemmas no respeta el contrato acordado"

## Context

`suggestedLemmas` alimenta el contexto que recibe la revisión de tareas `LEMMA_ABSENCE`. Antes de este cambio, la lista favorecía lemas detectados por ausencia o ubicación temporal y resolvía el orden con COCA ascendente.

Con la incorporación funcional de `lemma-count`, el sistema puede distinguir entre:

- lemas completamente ausentes;
- lemas ya introducidos, pero con exposición insuficiente;
- lemas mal ubicados temporalmente;
- lemas sin diagnóstico `LEMMA_ABSENCE`, pero sub-expuestos en su nivel correspondiente.

La prioridad de negocio acordada es reforzar primero palabras ya introducidas que todavía no alcanzan el mínimo esperado de exposición, antes de introducir palabras nuevas.

Este cambio no crea un contexto de corrección independiente para `lemma-count`. La señal se incorpora dentro de `correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE`, usando los verbos públicos existentes.

## Scope

### IN

- Enriquecer `task.correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE` con señal proveniente de `lemma-count`.
- Usar `lemma-count` como señal de priorización, no solo como dato visible.
- Permitir que lemas sub-expuestos entren en `suggestedLemmas` aunque no tengan diagnóstico `LEMMA_ABSENCE`.
- Priorizar lemas ya introducidos pero sub-expuestos antes que lemas `COMPLETELY_ABSENT`.
- Tratar `APPEARS_TOO_EARLY` como tipo formal de `LEMMA_ABSENCE`.
- Evaluar `APPEARS_TOO_EARLY` en el nivel target real.
- Mantener COCA ascendente como desempate dentro de cada grupo de prioridad.
- Mantener el límite existente de `suggestedLemmas` después de aplicar el ranking.
- Exponer el mismo contexto enriquecido en `PlanCommand.plan(..., true)` y `GetCommand.get("tasks", ...)`.
- Permitir que `ReviseCommand.revise(...)` consuma el contexto enriquecido derivado por el sistema.
- Permitir que `ReviseCommand.revise(...)` acepte overrides externos válidos con la nueva forma observable.

### OUT

- No se introduce un nuevo comando público.
- No se crea un nuevo tipo independiente de `correctionContext` para `lemma-count`.
- No se cambia la finalidad general de `LEMMA_ABSENCE`.
- No se cambia la configuración funcional de `lemma-count`, salvo usar el umbral ya disponible.
- No se prioriza por mayor déficit de exposición.
- No se define la estrategia interna de revisión; solo se define la información y el orden que recibe.
- No se recalcula `lemma-count` ad hoc durante la revisión.
- No se modifica el comportamiento de analizadores o contextos no relacionados con `LEMMA_ABSENCE` y `lemma-count`.

## User Journeys

### Journey[F-SLEM-J001] - Generar plan con suggestedLemmas enriquecido

```yaml
flow:
  - id: audit_course
    action: "El cliente ejecuta AuditRunner.runDetailedAudit(...) o AuditRunner.runAudit(...) para un curso"
    then: audit_has_lemma_count

  - id: audit_has_lemma_count
    action: "La auditoría queda disponible para planificación"
    outcomes:
      - when: "La auditoría incluyó lemma-count y diagnósticos LEMMA_ABSENCE"
        then: plan_with_context
      - when: "La auditoría incluyó diagnósticos LEMMA_ABSENCE pero no incluyó lemma-count"
        then: plan_without_lemma_count_signal

  - id: plan_with_context
    action: "El cliente ejecuta PlanCommand.plan(auditId, storageMode, true)"
    gate: [F-SLEM-R001, F-SLEM-R002]
    then: assert_ranked_suggestions

  - id: assert_ranked_suggestions
    action: "Las tareas LEMMA_ABSENCE del plan exponen correctionContext.suggestedLemmas enriquecido y ordenado con lemma-count"
    gate: [F-SLEM-R003, F-SLEM-R004, F-SLEM-R005, F-SLEM-R006, F-SLEM-R007, F-SLEM-R013]
    result: success

  - id: plan_without_lemma_count_signal
    action: "El cliente ejecuta PlanCommand.plan(auditId, storageMode, true)"
    gate: [F-SLEM-R012]
    then: assert_suggestions_without_signal

  - id: assert_suggestions_without_signal
    action: "Las tareas LEMMA_ABSENCE conservan suggestedLemmas con los tres valores de lemma-count no informados en todos los elementos"
    gate: [F-SLEM-R013]
    result: success
```

### Journey[F-SLEM-J002] - Consultar tareas con paridad respecto del plan

```yaml
flow:
  - id: setup_planned_tasks
    action: "Existe una tarea LEMMA_ABSENCE generada con PlanCommand.plan(..., true)"
    then: get_tasks

  - id: get_tasks
    action: "El cliente ejecuta GetCommand.get(\"tasks\", name, filter)"
    gate: [F-SLEM-R008]
    then: assert_same_context

  - id: assert_same_context
    action: "La tarea consultada expone el mismo correctionContext.suggestedLemmas enriquecido y ordenado que el plan"
    result: success
```

### Journey[F-SLEM-J003] - Revisar usando correctionContext derivado por el sistema

```yaml
flow:
  - id: setup_task_with_system_context
    action: "Existe una tarea LEMMA_ABSENCE con correctionContext.suggestedLemmas enriquecido por el sistema"
    then: revise_without_override

  - id: revise_without_override
    action: "El cliente ejecuta ReviseCommand.revise(taskId, planId, null, null)"
    gate: [F-SLEM-R009]
    then: assert_revision_receives_context

  - id: assert_revision_receives_context
    action: "La revisión consume el correctionContext.suggestedLemmas enriquecido y ordenado con lemma-count"
    result: success
```

### Journey[F-SLEM-J004] - Revisar usando correctionContext externo enriquecido

```yaml
flow:
  - id: setup_task_for_override
    action: "Existe una tarea LEMMA_ABSENCE que acepta correctionContext externo"
    then: override_validity

  - id: override_validity
    action: "El cliente aporta un correctionContext externo mediante ReviseCommand.revise(...)"
    outcomes:
      - when: "El correctionContext externo tiene la misma forma observable válida que GetCommand.get(...)"
        then: accept_override
      - when: "El correctionContext externo contiene datos inconsistentes de lemma-count"
        then: reject_override

  - id: accept_override
    action: "El sistema acepta el correctionContext externo y lo consume para la revisión"
    gate: [F-SLEM-R010, F-SLEM-R013]
    result: success

  - id: reject_override
    action: "El sistema rechaza el correctionContext externo como inválido y no aplica la revisión"
    gate: [F-SLEM-R011]
    result: failure
```

## Open Questions

### Doubt[DOUBT-SUGGESTED-LEMMA-FIELDS] - Campos observables de lemma-count dentro de suggestedLemmas
- [x] Cada elemento de `suggestedLemmas` expone como mucho tres valores: `lemmaCount`, `lemmaCountThreshold` e `isUnderexposed`. No se exponen otros valores derivados (por ejemplo, déficit explícito) ni un campo `suggestionReason`.

**Answer**: Resuelto. La forma observable se formaliza en [F-SLEM-R013](#F-SLEM-R013). Los tres valores se comportan en bloque: o los tres están informados (cuando la auditoría incluyó `lemma-count` y la señal aplica al lema), o los tres quedan no informados (cuando no aplica o no está disponible).

### Doubt[DOUBT-LEMMA-COUNT-UNAVAILABLE] - Representación de señal no disponible
- [x] Cuando la auditoría no incluyó `lemma-count`, los tres valores (`lemmaCount`, `lemmaCountThreshold`, `isUnderexposed`) quedan no informados en todos los elementos de `suggestedLemmas`. La generación y la consulta del contexto siguen funcionando con los criterios restantes.

**Answer**: Resuelto. La representación observable queda fijada en [F-SLEM-R012](#F-SLEM-R012) y [F-SLEM-R013](#F-SLEM-R013): "señal no disponible" significa que los tres valores quedan no informados en bloque.

### Doubt[DOUBT-COMPLETELY-ABSENT-EXPOSURE] - Representación observable de COMPLETELY_ABSENT
- [x] La señal de `lemma-count` no es aplicable a un lema `COMPLETELY_ABSENT` (el curso nunca lo introdujo). En consecuencia, los tres valores quedan no informados para esos elementos.

**Answer**: Resuelto. La prioridad ya estaba clara ([F-SLEM-R007](#F-SLEM-R007)). La representación observable se formaliza ahora en [F-SLEM-R007](#F-SLEM-R007) y [F-SLEM-R013](#F-SLEM-R013): `lemmaCount`, `lemmaCountThreshold` e `isUnderexposed` quedan no informados, incluso si la auditoría incluyó `lemma-count`.

### Doubt[DOUBT-ERROR-TEXT] - Texto exacto para override inválido
- [x] El mensaje exacto queda a discreción de la implementación, siempre que comunique inconsistencia de la señal de `lemma-count` dentro de `suggestedLemmas`.

**Answer**: Resuelto. La regla funcional ([F-SLEM-R011](#F-SLEM-R011)) ya define qué debe rechazarse. No se fija un literal exacto; lo que se valida observablemente es que la revisión no se aplique con datos inconsistentes.

## References

- `FEAT-LCOUNT`: aporta la señal funcional de `lemma-count`, incluyendo el umbral de exposición.
- `FEAT-REVCTX`: define que `ReviseCommand.revise(...)` puede recibir un `correctionContext` externo con la misma forma observable que `GetCommand.get(...)`.
- `FEAT-PLANEF`: define que los planes efímeros pueden exponer `correctionContext` proyectado mediante `PlanCommand.plan(..., true)`.
- `FEAT-LAPS` / `FEAT-LAGEN`: consumen el contexto de revisión de `LEMMA_ABSENCE`, incluyendo `suggestedLemmas`.