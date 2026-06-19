---
feature:
  id: FEAT-INCEXP
  code: F-INCEXP
  name: Incluir lemas ya expuestos en la consulta de Suggested Lemmas
  priority: major
---

# Incluir lemas ya expuestos en la consulta de Suggested Lemmas

## TL;DR

**Qué**: Agrega una opción booleana al filtro de `GetCommand.get("suggested-lemmas", <taskId>, ...)` que, cuando se activa, amplía el universo de candidatos para incluir también los lemas del `Level` y `Part of Speech` pedidos que ya alcanzaron su umbral de exposición.

**Por qué**: Con la consulta por defecto, pedir un `Part of Speech` específico en un `Level` puede traer muy pocos resultados porque sólo se recomiendan lemas que todavía necesitan exposición; la opción permite al agente revisor disponer de más candidatos cuando los pocos sub-expuestos no alcanzan.

## Business Rules

<a id="F-INCEXP-R001"></a>
### Rule[F-INCEXP-R001] - Comportamiento por defecto inalterado
**Severity**: critical | **Validation**: VALIDATED

> Cuando la consulta no activa la nueva opción, `GetCommand.get("suggested-lemmas", <taskId>, ...)` se comporta exactamente como hoy: sólo recomienda lemas que todavía necesitan exposición.

<details><summary>Detalle</summary>

Esta regla fija la paridad con el comportamiento histórico de FEAT-CSLATDC. Sin la opción activada, los lemas que ya alcanzaron su umbral de exposición siguen quedando fuera del conjunto de candidatos, igual que antes. La señal de prioridad sigue actuando como filtro de exclusión cuando la opción está desactivada.

El contraste central de esta feature es: por defecto la prioridad EXCLUYE; con la opción activada la prioridad sólo ORDENA ([F-INCEXP-R002](#F-INCEXP-R002), [F-INCEXP-R004](#F-INCEXP-R004)).

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R002"></a>
### Rule[F-INCEXP-R002] - La opción amplía el universo a los lemas ya expuestos
**Severity**: critical | **Validation**: VALIDATED

> Cuando la consulta activa la nueva opción, el universo de candidatos se amplía para incluir TODOS los lemas aplicables al `Level` y `Part of Speech` pedidos, incluyendo los que ya alcanzaron su umbral de exposición.

<details><summary>Detalle</summary>

Sin la opción, un lema que ya apareció las veces suficientes no necesita más exposición y por eso no entra al conjunto de candidatos. Con la opción activada, ese lema sí entra: la condición de "ya suficientemente expuesto" deja de excluirlo.

El efecto es directamente observable comparando, para el mismo `taskId`, `Level` y `Part of Speech`, la respuesta con la opción desactivada contra la respuesta con la opción activada: la segunda incluye al menos los mismos lemas que la primera y, cuando existan lemas ya expuestos de ese tipo y nivel, también esos.

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R003"></a>
### Rule[F-INCEXP-R003] - El orden se mantiene por prioridad de recomendación
**Severity**: critical | **Validation**: VALIDATED

> Con la opción activada, los lemas que todavía necesitan exposición aparecen primero y los lemas ya suficientemente expuestos aparecen al final, manteniendo el orden por prioridad de recomendación.

<details><summary>Detalle</summary>

La opción no cambia el criterio de ordenamiento existente, sólo el universo sobre el que se aplica. Los lemas sub-expuestos o ausentes conservan su prioridad de recomendación y se ubican antes que cualquier lema ya suficientemente expuesto. Los lemas ya expuestos quedan agrupados al final de la respuesta, después de todos los que aún necesitan exposición.

Este orden permite que el agente revisor siga viendo primero lo más relevante, y disponga de los lemas ya expuestos sólo como candidatos adicionales al final.

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R004"></a>
### Rule[F-INCEXP-R004] - El desempate sigue siendo frecuencia COCA ascendente
**Severity**: major | **Validation**: VALIDATED

> Cuando dos o más lemas comparten el mismo grado de prioridad de recomendación, el desempate sigue siendo por frecuencia COCA ascendente, tanto entre lemas que necesitan exposición como entre lemas ya expuestos.

<details><summary>Detalle</summary>

La opción no introduce un nuevo criterio de desempate ni reemplaza el existente. Dentro del bloque de lemas que aún necesitan exposición y dentro del bloque de lemas ya expuestos, el orden interno sigue resolviéndose por frecuencia COCA ascendente, igual que hoy.

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R005"></a>
### Rule[F-INCEXP-R005] - El filtro obligatorio por level sigue aplicando
**Severity**: critical | **Validation**: VALIDATED

> Activar la opción no relaja el filtro de `Level`: la respuesta sigue incluyendo únicamente lemas adecuados al `Level` aplicable a la `Task/Quiz`.

<details><summary>Detalle</summary>

La opción amplía el universo únicamente respecto de la condición de exposición. El filtro de nivel se mantiene exactamente como en la consulta por defecto: la inferencia de `Level` desde la `Task/Quiz`, el uso de `Level` explícito y el rechazo cuando no hay nivel aplicable siguen rigiendo sin cambios ([F-INCEXP-R008](#F-INCEXP-R008)).

**Error**: "No se pueden sugerir lemas sin un nivel aplicable para filtrar." [ASSUMPTION]

</details>

<a id="F-INCEXP-R006"></a>
### Rule[F-INCEXP-R006] - El filtro por partOfSpeech sigue aplicando
**Severity**: major | **Validation**: VALIDATED

> Activar la opción no relaja el filtro de `Part of Speech`: cuando se pide un `Part of Speech`, la respuesta sigue incluyendo únicamente lemas de ese tipo, ahora también entre los ya expuestos.

<details><summary>Detalle</summary>

La ampliación del universo opera dentro del `Part of Speech` solicitado. Si se pide `VERB`, los lemas ya expuestos que se incorporan también deben ser verbos; la opción no amplía el resultado a otros tipos gramaticales.

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R007"></a>
### Rule[F-INCEXP-R007] - El limit sigue recortando el resultado final
**Severity**: major | **Validation**: VALIDATED

> Activar la opción no relaja el `limit`: la respuesta sigue devolviendo como máximo la cantidad solicitada, recortando después de ampliar el universo y ordenar por prioridad.

<details><summary>Detalle</summary>

El recorte por `limit` se aplica como último paso, sobre el universo ampliado y ya ordenado. Como los lemas que necesitan exposición van primero ([F-INCEXP-R003](#F-INCEXP-R003)), cuando el `limit` es menor que la cantidad total de candidatos, los lemas ya expuestos son los primeros en quedar fuera del recorte.

Dado que el universo ahora puede ser grande, el valor por defecto y el máximo permitido de `limit` cobran más relevancia (ver [DOUBT-LIMIT-DEFAULT-MAX-INCEXP](#DOUBT-LIMIT-DEFAULT-MAX-INCEXP)).

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R008"></a>
### Rule[F-INCEXP-R008] - La inferencia y el level explícito siguen funcionando con la opción activada
**Severity**: major | **Validation**: VALIDATED

> Con la opción activada, la inferencia de `Level` desde la `Task/Quiz` y el uso de `Level` explícito siguen funcionando igual que en la consulta por defecto.

<details><summary>Detalle</summary>

La resolución del nivel aplicable no depende de la opción. Cuando la opción está activada, el sistema infiere el `Level` desde la `Task/Quiz` o su contexto cuando puede, usa el `Level` explícito cuando se informa, y rechaza la consulta con una validación clara cuando no hay nivel aplicable, exactamente como en la consulta por defecto.

**Error**: "No se pueden sugerir lemas porque falta el nivel necesario para filtrar." [ASSUMPTION]

</details>

<a id="F-INCEXP-R009"></a>
### Rule[F-INCEXP-R009] - Respuesta vacía sin error cuando no hay lemas del tipo y nivel pedidos
**Severity**: major | **Validation**: VALIDATED

> Cuando, incluso con la opción activada, no existe ningún lema aplicable al `Level` y `Part of Speech` pedidos, la respuesta es una lista vacía con explicación funcional clara, sin error.

<details><summary>Detalle</summary>

La opción amplía el universo pero no garantiza resultados. Si para ese `Level` y `Part of Speech` no existe ningún lema, ni sub-expuesto ni ya expuesto, la respuesta es una lista vacía con una explicación funcional, en paridad con el comportamiento de "lista vacía sin error" de la consulta por defecto. El sistema no inventa lemas para llenar la respuesta.

**Error**: "No hay lemas sugeridos disponibles para esta task." [ASSUMPTION]

</details>

<a id="F-INCEXP-R010"></a>
### Rule[F-INCEXP-R010] - Lista parcial sin error sigue valiendo
**Severity**: major | **Validation**: VALIDATED

> Cuando la cantidad de lemas aplicables sea menor que el `limit` solicitado, la respuesta devuelve todos los disponibles sin considerarlo error, igual con la opción activada que desactivada.

<details><summary>Detalle</summary>

Recibir menos resultados que el `limit` solicitado nunca es un error. Esto vale tanto en la consulta por defecto como con la opción activada: si tras ampliar el universo aún hay menos lemas que el `limit`, se devuelven todos los disponibles.

**Error**: "No aplica."

</details>

<a id="F-INCEXP-R011"></a>
### Rule[F-INCEXP-R011] - La forma observable de cada lema sugerido se mantiene
**Severity**: major | **Validation**: VALIDATED

> Cada lema sugerido sigue siendo identificable, su posición en la respuesta representa su prioridad relativa, y los campos opcionales de exposición se exponen cuando apliquen, igual que en la consulta por defecto.

<details><summary>Detalle</summary>

La opción no cambia la forma observable de cada resultado. Cada lema sugerido es identificable y su posición en la respuesta indica su prioridad relativa. Los campos opcionales de exposición definidos por FEAT-SLEM (`lemmaCount`, `lemmaCountThreshold`, `isUnderexposed`) siguen comportándose como están especificados en [F-SLEM-R013](#F-SLEM-R013): informados cuando la señal de exposición aplica al lema, y no informados cuando no aplica.

Para un lema ya suficientemente expuesto incorporado por la opción, la señal de exposición es aplicable y, por lo tanto, esos campos quedan informados, con `isUnderexposed` indicando que el lema NO está sub-expuesto.

**Error**: "No aplica."

</details>

## Context

La consulta dinámica de `Suggested Lemmas` introducida por FEAT-CSLATDC recomienda, por diseño, sólo los lemas que la auditoría marcó como que necesitan exposición: ausentes, sub-expuestos o mal ubicados temporalmente. Un lema del nivel que ya alcanzó su umbral de exposición no está marcado como faltante y, por lo tanto, no entra al conjunto de candidatos.

Esto genera fricción cuando el agente revisor pide un `Part of Speech` específico en un `Level` y existen pocos lemas de ese tipo que todavía necesiten exposición: la respuesta trae muy pocos resultados, aunque en ese nivel y tipo gramatical existan otros lemas perfectamente válidos que ya fueron suficientemente expuestos.

La necesidad funcional es ofrecer una opción explícita que amplíe el universo de candidatos a todos los lemas aplicables al `Level` y `Part of Speech` pedidos, incluyendo los ya expuestos, sin cambiar el orden de prioridad ni relajar los demás filtros. El cambio convierte la señal de prioridad de un filtro de EXCLUSIÓN ([F-INCEXP-R001](#F-INCEXP-R001)) en un criterio de ORDENAMIENTO ([F-INCEXP-R002](#F-INCEXP-R002), [F-INCEXP-R003](#F-INCEXP-R003)) cuando la opción está activada.

Esta es una evolución aditiva: la consulta por defecto no cambia en absoluto. La opción es opcional y desactivada por defecto.

## Scope

### IN

- Agregar una opción booleana al filtro de `GetCommand.get("suggested-lemmas", <taskId>, ...)` que controla la inclusión de lemas ya expuestos.
- Mantener el comportamiento por defecto idéntico al de FEAT-CSLATDC cuando la opción está desactivada.
- Ampliar el universo de candidatos, con la opción activada, a todos los lemas aplicables al `Level` y `Part of Speech` pedidos, incluyendo los ya expuestos.
- Conservar el orden por prioridad de recomendación, con los lemas que necesitan exposición primero y los ya expuestos al final.
- Conservar el desempate por frecuencia COCA ascendente dentro de cada grado de prioridad.
- Conservar el filtro obligatorio por `Level`, el filtro por `Part of Speech` y el recorte por `limit`.
- Conservar la inferencia de `Level` y el uso de `Level` explícito con la opción activada.
- Devolver lista vacía con explicación funcional cuando no haya lemas aplicables, sin error.
- Devolver lista parcial sin error cuando haya menos lemas que el `limit`.
- Conservar la forma observable de cada lema sugerido, incluidos los campos opcionales de exposición cuando apliquen.

### OUT

- No se cambia el comportamiento por defecto de la consulta.
- No se introduce un nuevo verbo de consulta.
- No se agregan otros filtros léxicos fuera de la nueva opción y de los ya soportados (`level`, `partOfSpeech`, `limit`).
- No se redefine el contenido estático de los `suggestedLemmas` del `correctionContext`.
- No se cambia el criterio de prioridad ni el de desempate existentes.
- No se resuelve la posible duplicación respecto del conjunto estático del `correctionContext` (ver [DOUBT-STATIC-DUPLICATES-INCEXP](#DOUBT-STATIC-DUPLICATES-INCEXP)).
- No se define en esta iteración el valor por defecto ni el máximo de `limit` (ver [DOUBT-LIMIT-DEFAULT-MAX-INCEXP](#DOUBT-LIMIT-DEFAULT-MAX-INCEXP)).

## User Journeys

### Journey[F-INCEXP-J001] - El agente amplía la consulta para incluir lemas ya expuestos

```yaml
flow:
  - id: few_underexposed_available
    action: "Existe una Task/Quiz identificada por taskId donde, para el Level y Part of Speech pedidos, hay pocos lemas que todavía necesitan exposición"
    then: request_with_include_exposed

  - id: request_with_include_exposed
    action: "El agente consulta los suggested-lemmas para esa task pidiendo limit, partOfSpeech y activando la opción de incluir lemas ya expuestos"
    then: resolve_level

  - id: resolve_level
    action: "El sistema determina el Level aplicable para la consulta"
    gate: [F-INCEXP-R005, F-INCEXP-R008]
    outcomes:
      - when: "El Level puede inferirse desde la Task/Quiz o fue informado explícitamente"
        then: widen_universe
      - when: "El Level no puede inferirse y no fue informado explícitamente"
        then: reject_missing_level

  - id: widen_universe
    action: "El sistema amplía el universo de candidatos para incluir todos los lemas del Level y Part of Speech pedidos, incluyendo los ya expuestos"
    gate: [F-INCEXP-R002, F-INCEXP-R006]
    then: order_and_limit

  - id: order_and_limit
    action: "El sistema ordena por prioridad de recomendación con desempate por frecuencia COCA ascendente, y luego recorta por limit"
    gate: [F-INCEXP-R003, F-INCEXP-R004, F-INCEXP-R007]
    outcomes:
      - when: "Existen lemas aplicables al Level y Part of Speech pedidos"
        then: return_widened_results
      - when: "No existe ningún lema aplicable al Level y Part of Speech pedidos"
        then: return_empty_results

  - id: return_widened_results
    action: "La respuesta contiene hasta limit lemas del Part of Speech pedido y del Level aplicable, con los que necesitan exposición primero y los ya expuestos al final, identificables y con su forma observable habitual"
    gate: [F-INCEXP-R002, F-INCEXP-R003, F-INCEXP-R010, F-INCEXP-R011]
    result: success

  - id: return_empty_results
    action: "La respuesta es una lista vacía con una explicación funcional de que no hay lemas aplicables para esos filtros"
    gate: [F-INCEXP-R009]
    result: success

  - id: reject_missing_level
    action: "La respuesta rechaza la consulta con una validación clara porque falta el Level necesario para filtrar"
    gate: [F-INCEXP-R005, F-INCEXP-R008]
    result: failure
```

### Journey[F-INCEXP-J002] - Contraste entre la consulta por defecto y la consulta con la opción

```yaml
flow:
  - id: setup_level_and_pos
    action: "Existe una Task/Quiz identificada por taskId donde, para el mismo Level y Part of Speech, hay lemas que necesitan exposición y también lemas que ya alcanzaron su umbral de exposición"
    then: query_default

  - id: query_default
    action: "El agente consulta los suggested-lemmas para esa task sin activar la opción"
    gate: [F-INCEXP-R001]
    then: query_with_option

  - id: query_with_option
    action: "El agente repite la consulta para la misma task, Level y Part of Speech, activando la opción de incluir lemas ya expuestos"
    gate: [F-INCEXP-R002]
    then: compare_results

  - id: compare_results
    action: "La consulta con la opción activada incluye al menos los mismos lemas que la consulta por defecto y, además, los lemas ya expuestos del Level y Part of Speech pedidos, ubicados al final por prioridad"
    gate: [F-INCEXP-R002, F-INCEXP-R003]
    result: success
```

## Open Questions

<a id="DOUBT-STATIC-DUPLICATES-INCEXP"></a>
### Doubt[DOUBT-STATIC-DUPLICATES-INCEXP] - Duplicados respecto del correctionContext estático al ampliar el universo

Al ampliar el universo con la opción activada, crece la probabilidad de devolver lemas que ya estaban presentes en los `suggestedLemmas` estáticos del `correctionContext`. No quedó definido si la consulta ampliada debe excluir esos lemas ya presentes o si puede devolverlos nuevamente. Esta duda hereda y extiende `DOUBT-STATIC-DUPLICATES` de [FEAT-CSLATDC](#references); la decisión debería ser consistente entre ambas.

<a id="DOUBT-LIMIT-DEFAULT-MAX-INCEXP"></a>
### Doubt[DOUBT-LIMIT-DEFAULT-MAX-INCEXP] - Default y máximo de limit cobran más relevancia con la opción activada

Como la opción amplía el universo de candidatos a todos los lemas aplicables del `Level` y `Part of Speech`, el resultado puede volverse grande. Esto hace más relevante definir un valor por defecto y un máximo permitido para `limit`. No quedó definido ese default ni ese máximo. Esta duda hereda y extiende `DOUBT-LIMIT-DEFAULT-MAX` de [FEAT-CSLATDC](#references); la decisión debería ser consistente entre ambas.

<a id="DOUBT-EXPOSED-TIE-WITH-UNDEREXPOSED"></a>
### Doubt[DOUBT-EXPOSED-TIE-WITH-UNDEREXPOSED] - Granularidad de la prioridad entre ya expuestos

Quedó claro que los lemas ya expuestos van al final y que el desempate dentro de cada grado de prioridad es COCA ascendente ([F-INCEXP-R003](#F-INCEXP-R003), [F-INCEXP-R004](#F-INCEXP-R004)). No quedó definido si, dentro del bloque de lemas ya expuestos, debe distinguirse algún sub-grado adicional de prioridad antes de aplicar el desempate por COCA, o si todos los ya expuestos se consideran un único grado.

## References

- **FEAT-CSLATDC** — Define la consulta dinámica de `Suggested Lemmas` por `taskId` que esta feature evoluciona de forma aditiva: identidad por task, filtro obligatorio por `Level`, filtro por `Part of Speech`, `limit`, orden por prioridad y lista vacía sin error. El comportamiento por defecto preservado por [F-INCEXP-R001](#F-INCEXP-R001) es el de esta feature. Las dudas [DOUBT-STATIC-DUPLICATES-INCEXP](#DOUBT-STATIC-DUPLICATES-INCEXP) y [DOUBT-LIMIT-DEFAULT-MAX-INCEXP](#DOUBT-LIMIT-DEFAULT-MAX-INCEXP) heredan `DOUBT-STATIC-DUPLICATES` y `DOUBT-LIMIT-DEFAULT-MAX` de esta feature.
- **FEAT-SLEM** — Define la forma observable opcional por lema (`lemmaCount`, `lemmaCountThreshold`, `isUnderexposed`) y la señal de exposición que distingue lemas sub-expuestos de lemas ya suficientemente expuestos. Citada por [F-INCEXP-R011](#F-INCEXP-R011).
