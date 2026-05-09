---
feature:
  id: FEAT-REVCTX
  code: F-REVCTX
  name: Override del contexto de correccion en el verbo revise
  priority: critical
---

# Override del contexto de correccion en el verbo revise

## TL;DR

**Que**: El verbo `revise task <id>` acepta opcionalmente un **`correctionContext` provisto externamente por el cliente** mediante dos flags mutuamente excluyentes: `--correction-context=<json>` (literal en linea) y `--correction-context-file=<path>` (archivo). Cuando se provee, el sistema usa ese contexto como insumo de la estrategia de revision activa, en lugar del contexto que derivaria por si mismo del plan persistente y su `AuditReport` fuente. Cuando no se provee, el comportamiento es el actual y no cambia.

**Por que**: El operador del dashboard revisa decenas de tareas y, mientras decide, ve un **plan proyectado** ([FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) que refleja el efecto acumulado de sus decisiones aceptadas y pendientes. Cuando finalmente aprueba una tarea, espera que la revision se ejecute sobre el contexto proyectado que efectivamente vio en la UI. Hoy `revise` deriva el contexto del plan persistido + `AuditReport` original, ignorando las pendientes; la consecuencia es que la estrategia (LLM en LEMMA_ABSENCE) puede proponer una palabra que ya consumio una propuesta previa pendiente -exactamente el problema que el plan proyectado prometia evitar visualmente. Permitir un override del contexto cierra el ciclo entre lo que el operador ve y lo que el sistema ejecuta.

## Reglas de Negocio

### Grupo A - Aceptacion del override

<a id="F-REVCTX-R001"></a>
### Rule[F-REVCTX-R001] - El verbo `revise` acepta un `correctionContext` provisto externamente, opcional
**Severity**: critical | **Validation**: VALIDATED

> El verbo `revise task <id>` ([F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015)) admite un mecanismo opt-in mediante dos flags mutuamente excluyentes: `--correction-context=<json>` (payload JSON literal en linea) y `--correction-context-file=<path>` (path a un archivo cuyo contenido es el payload JSON). Si el operador pasa los dos flags en la misma invocacion, el comando **falla con un error claro antes de hacer cualquier cosa** (sin invocar la estrategia, sin persistir artefacto, sin modificar la tarea ni el curso). Cuando se provee uno solo de los dos flags, el payload deserializado es el unico insumo de contexto que la estrategia de revision activa consume; el sistema **no** deriva contexto por su cuenta a partir del plan persistido ni del `AuditReport` para la tarea revisada. Cuando no se provee ninguno de los dos, el comportamiento de `revise` es el actual sin modificacion alguna.

<details><summary>Detail</summary>

1. La capacidad es **opt-in**. La invocacion sin override conserva el contrato actual de [F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015) y de las estrategias de revision (FEAT-LAPS y futuras), incluyendo todos los modos de aprobacion (FEAT-REVAPR), la persistencia del artefacto y los exit codes. Un cliente que no usa el override no observa ningun cambio.
2. El override entra **en el lugar** donde hoy la estrategia consume el contexto derivado. La cadena downstream (estrategia -> propuesta -> validador -> persistencia del artefacto -> aplicacion al curso) opera tal cual. El override no altera donde se persiste el artefacto, ni que campos lleva la `RevisionProposal`, ni la mecanica de aprobacion.
3. Los dos flags cubren los dos casos de uso reales: integraciones programaticas de tamano chico (inline) e integraciones que ya tienen el contexto materializado en disco o que exceden los limites de longitud de la shell (archivo). La exclusion mutua evita ambiguedad sobre cual de las dos fuentes el sistema deberia usar.

</details>

**Error** (cuando ambos flags se pasan a la vez): "--correction-context y --correction-context-file son mutuamente excluyentes"

<a id="F-REVCTX-R002"></a>
### Rule[F-REVCTX-R002] - Cuando se provee override, el sistema **no** deriva contexto por su cuenta para la tarea revisada
**Severity**: critical | **Validation**: VALIDATED

> Si el cliente provee un `correctionContext` por la via de [F-REVCTX-R001](#F-REVCTX-R001), el sistema usa ese objeto como insumo de la estrategia de revision activa **sin** combinarlo, mergearlo, complementarlo ni cruzarlo contra el contexto que derivaria por si mismo del plan persistente o del `AuditReport`. Las invariantes observables son:
>
> 1. **Reemplazo, no merge**: la estrategia recibe el override completo. Campos ausentes en el override no se rellenan desde el contexto derivado. Campos presentes en el override no se sobrescriben con valores derivados.
> 2. **Cero derivacion**: ninguna lectura sobre el `AuditReport` fuente ni sobre el arbol de diagnosticos ocurre **para construir el contexto** de esta revision. Lecturas que sigan siendo necesarias para resolver la tarea (`taskId` -> tarea concreta dentro del plan persistente) se mantienen sin cambios; lo que se elimina es la fase de construccion del contexto, no la fase de identificar la tarea.
> 3. **Confianza explicita en el override**: cuando hay override, la correccion procede confiando en el contenido del override. El sistema no re-deriva ni cruza-valida el contexto contra el estado actual del curso. La unica excepcion es el sanity check minimo de coherencia definido en [F-REVCTX-R003](#F-REVCTX-R003); la auditabilidad ([F-REVCTX-R007](#F-REVCTX-R007)) es la otra defensa, pero no es una validacion: es un registro a posteriori.

<details><summary>Detail</summary>

1. La regla es deliberadamente "el cliente sabe": el override le da al cliente la responsabilidad de entregar un contexto util y semanticamente coherente con el estado actual del curso, y el sistema no intenta corregirlo ni aumentarlo. Si el cliente entrega un contexto incompleto o desalineado, la estrategia operara con lo que recibio (y podra fallar segun sus propias precondiciones, ver [F-REVCTX-R003](#F-REVCTX-R003)).
2. La razon de no permitir merge es que cualquier composicion (override > derivado, derivado > override, union, interseccion) introduce semanticas no obvias que el cliente no puede predecir. Reemplazo total es la unica semantica donde lo que el operador vio en la UI es exactamente lo que la estrategia consume.
3. La unica defensa que el sistema aplica frente a un override "incoherente" es el sanity check de identidad logica de la tarea ([F-REVCTX-R003](#F-REVCTX-R003)): el override debe declarar el mismo `nodeId` y el mismo `diagnosisKind` que la tarea persistente identificada por `<task-id>`. Mas alla de ese sanity check, la coherencia semantica del payload es responsabilidad del cliente, y eso es **intencional**.

</details>

---

### Grupo B - Validacion del override

<a id="F-REVCTX-R003"></a>
### Rule[F-REVCTX-R003] - El override debe respetar el contrato del `correctionContext` y declarar la misma identidad logica que la tarea revisada
**Severity**: critical | **Validation**: VALIDATED

> El `correctionContext` provisto por el cliente debe cumplir, de forma conjunta, dos condiciones:
>
> 1. **Validez estructural**: respetar el contrato estructural ya definido por los features de contexto de correccion para el `DiagnosisKind` de la tarea revisada (FEAT-RCSL para `SENTENCE_LENGTH`, [FEAT-RCLA](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md) para `LEMMA_ABSENCE`, y los que existan en el futuro): campos obligatorios presentes, tipos correctos, valores dentro de dominio.
> 2. **Identidad logica coincidente**: el `nodeId` y el `diagnosisKind` declarados en el payload deben coincidir con los de la tarea persistente identificada por `<task-id>`. La intuicion semantica es *"es la misma tarea logica pero con el contexto actualizado"*: dos planes distintos producen `taskId` distintos para el mismo problema logico, asi que la identidad de la tarea no se chequea por `taskId` sino por la dupla `(nodeId, diagnosisKind)`.
>
> Si alguna de las dos condiciones no se cumple, el comando **rechaza la invocacion** antes de invocar la estrategia, sin persistir artefacto ni modificar la tarea ni el curso. El mensaje de error identifica el motivo del rechazo de forma accionable.

<details><summary>Detail</summary>

1. El contrato estructural del `correctionContext` por `DiagnosisKind` ya esta definido por los features de contexto. R003 obliga que el override respete ese contrato; **no** redefine la forma del contexto.
2. La verificacion de identidad logica por `(nodeId, diagnosisKind)` es deliberadamente **mas debil que `taskId`** y deliberadamente **mas fuerte que solo estructura**:
   - No se chequea por `taskId` porque dos planes distintos sobre el mismo `AuditReport` reasignan `taskId` por reglas internas (FEAT-PLANEF). Un override valido proveniente de un plan proyectado tiene un `taskId` que no es el mismo que el del plan persistente; exigir que coincidan rechazaria el caso de uso real.
   - Se chequea por `(nodeId, diagnosisKind)` porque esa dupla es la identidad **logica** de la tarea: el mismo nodo del curso con el mismo tipo de diagnostico. Si el cliente entrega un override que pertenece a otro nodo o a otro tipo de diagnostico, eso es siempre un error del cliente (pego el contexto equivocado), y se atrapa con un costo bajo.
3. La regla obliga el rechazo **antes** de invocar la estrategia. Esto evita que la estrategia falle con errores opacos por un input mal formado o desalineado, y mantiene la propiedad de "no efectos colaterales" (ningun artefacto, ningun cambio en la tarea, ningun cambio en el curso).
4. Errores semanticos mas profundos que el cliente comete cuando arma un override "valido pero incoherente con el estado actual del curso" (e.g. lemas sugeridos vacios pero misplaced presentes, o un `suggestedLemma` que el operador ya consumio en otra propuesta pendiente) **no** son responsabilidad de R003: viajan a la estrategia y se manifiestan segun las precondiciones de cada estrategia (FEAT-LAPS y futuras). El sistema no intenta defenderse de esos errores; ver [F-REVCTX-R002](#F-REVCTX-R002) #3.

</details>

**Error** (validez estructural): "El correctionContext provisto no es valido para una tarea de tipo '{diagnosisKind}': {motivo}"

**Error** (identidad logica): "El correctionContext provisto pertenece a otra tarea logica (esperado nodeId={nodeIdEsperado}, diagnosisKind={diagnosisKindEsperado}; recibido nodeId={nodeIdRecibido}, diagnosisKind={diagnosisKindRecibido})"

<a id="F-REVCTX-R004"></a>
### Rule[F-REVCTX-R004] - El override no aplica a `DiagnosisKind` que no tienen contexto definido
**Severity**: major | **Validation**: VALIDATED

> Si la tarea revisada es de un `DiagnosisKind` para el cual no existe un contrato de `correctionContext` definido (por ejemplo, los `DiagnosisKind` que actualmente caen al Reviser bypass de [FEAT-REVBYP](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)), invocar `revise` con override es **rechazado** con un mensaje claro. El bypass no consume contexto, asi que un override no tiene como afectarlo.

<details><summary>Detail</summary>

1. Hoy el Reviser bypass es la default para los `DiagnosisKind` distintos de `LEMMA_ABSENCE` (FEAT-REVBYP R004 + FEAT-LAPS R002). El bypass produce `elementAfter == elementBefore` ignorando el contexto, asi que un override no puede tener ningun efecto observable sobre el resultado.
2. La regla acota la superficie del feature al unico caso donde tiene sentido: tareas cuyo Reviser activo consume el contexto.
3. Cuando en el futuro nuevas estrategias se enchufen al despachador (futuras estrategias para `SENTENCE_LENGTH`, etc.), el override pasa a aplicar para esos `DiagnosisKind` sin necesidad de modificar este requerimiento, siempre que el contrato de `correctionContext` exista (R003).

</details>

**Error**: "El verbo revise no acepta correctionContext para tareas de tipo '{diagnosisKind}'"

---

### Grupo C - Interaccion con FEAT-PLANEF y el plan proyectado del cliente

<a id="F-REVCTX-R005"></a>
### Rule[F-REVCTX-R005] - El plan resoluble por `revise` puede ser distinto del plan que el cliente proyecto
**Severity**: critical | **Validation**: VALIDATED

> Cuando el cliente invoca `revise task <id>` con un override, el plan que el comando resuelve para identificar la tarea es el persistente segun [F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015) (default: el mas reciente; explicito: el de `--plan <id>`). Ese plan **no necesariamente** es el mismo plan proyectado que el cliente miro en la UI ([FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)): un plan efimero no tiene id resoluble entre invocaciones, ni esta direccionable por `--plan`. La invariante observable es:
>
> 1. La tarea revisada es la que el plan persistente identifica por `<task-id>`.
> 2. El contexto consumido por la estrategia es el que el cliente provee como override.
> 3. La identidad logica de la tarea (`nodeId`, `diagnosisKind`) que el override declara debe coincidir con la de la tarea persistente identificada por `<task-id>`; ese sanity check lo aplica el sistema en [F-REVCTX-R003](#F-REVCTX-R003). Mas alla de eso, la coherencia semantica del override (que los lemas sugeridos sean los proyectados, que la oracion sea la actualizada, etc.) es responsabilidad del cliente.

<details><summary>Detail</summary>

1. La razon de no exigir que el plan resuelto coincida con el plan proyectado es practica: el plan proyectado no es persistente (FEAT-PLANEF) y por lo tanto no es resoluble. La unica entidad resoluble es el plan persistente del operador, que en el caso normal contiene la misma tarea logica (mismo `nodeId`, mismo `diagnosisKind`) pero con el contexto basal -no proyectado- en su estado interno. Lo que el override entrega es justamente el contexto proyectado que reemplaza al basal.
2. El sanity check de identidad logica por `(nodeId, diagnosisKind)` -no por `taskId`- existe precisamente porque dos planes distintos sobre el mismo `AuditReport` reasignan `taskId` por reglas internas. Comparar por `taskId` rechazaria el caso de uso real (override proveniente de un plan proyectado, tarea persistente con otro `taskId`). Comparar por `(nodeId, diagnosisKind)` captura la identidad logica que es estable entre planes.
3. Mas alla del sanity check de R003, el sistema confia en el contenido del override sin re-derivar ni cruzar-validar contra el estado actual del curso (ver [F-REVCTX-R002](#F-REVCTX-R002) #3). La auditabilidad ([F-REVCTX-R007](#F-REVCTX-R007)) es la otra defensa, pero a posteriori.

</details>

---

### Grupo D - Modos de aprobacion y persistencia

<a id="F-REVCTX-R006"></a>
### Rule[F-REVCTX-R006] - El override no altera los modos de aprobacion ni la persistencia del artefacto
**Severity**: major | **Validation**: VALIDATED

> Una invocacion de `revise` con override produce el mismo flujo de aprobacion (FEAT-REVAPR: `auto`, `human`) y persiste el mismo artefacto de propuesta ([FEAT-REVBYP](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) que una invocacion sin override. El unico cambio observable es:
>
> 1. La fuente del contexto consumido por la estrategia (override en lugar de derivacion).
> 2. La trazabilidad del artefacto, que distingue las dos fuentes ([F-REVCTX-R007](#F-REVCTX-R007)).

<details><summary>Detail</summary>

1. Esta regla acota explicitamente el alcance del override para evitar que el feature se filtre a aprobacion / artefactos. El operador sigue eligiendo modo de aprobacion por el mecanismo existente (env var de FEAT-REVAPR), y el artefacto persistido preserva el mismo formato con el agregado de la marca de origen y el snapshot definidos en [F-REVCTX-R007](#F-REVCTX-R007).
2. El exit code del comando, los mensajes de error de aprobacion, la mecanica de persistencia del curso modificado, y el handling de tareas con propuesta pendiente (FEAT-REVAPR R010) operan identico.

</details>

---

### Grupo E - Trazabilidad y formato del payload

<a id="F-REVCTX-R007"></a>
### Rule[F-REVCTX-R007] - El artefacto persiste el origen del contexto y un snapshot literal del override
**Severity**: critical | **Validation**: VALIDATED

> Cuando una revision se ejecuto con override, el artefacto de propuesta persistido ([FEAT-REVBYP](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) lleva, en su forma observable a posteriori, dos elementos distinguibles:
>
> 1. **Indicador de origen del contexto**: un campo que toma uno de dos valores -uno para "el contexto fue derivado por el sistema" y otro para "el contexto fue provisto externamente como override"-. Para una invocacion sin override, el campo declara el origen derivado; para una invocacion con override, declara el origen override.
> 2. **Snapshot literal del payload entregado por el operador**: cuando el origen es override, el artefacto contiene el payload tal como fue deserializado del flag (sin re-derivacion, sin enriquecimiento, sin reordenamientos); cuando el origen es derivado, este snapshot esta ausente o vacio.
>
> Ambos elementos juntos permiten reproducir la auditoria a posteriori: dado un artefacto, un humano (o un test de regresion) puede ver exactamente que contexto recibio el sistema cuando produjo la propuesta y de donde provino ese contexto.

<details><summary>Detail</summary>

1. La motivacion es practica: cuando una propuesta resulta cuestionable -el LLM eligio una palabra extrana, el operador no entiende por que el sistema decidio lo que decidio-, la auditoria necesita responder con precision *"recibio este contexto exacto"*. Sin el snapshot literal, esa pregunta queda sin respuesta. Sin el indicador de origen, no se puede diferenciar un fallo del derivador de un fallo del cliente.
2. La regla obliga la **observabilidad** ("distinguible a posteriori"); no obliga la forma exacta del campo (booleano, enum, marcador), del nombre del campo, ni del formato del snapshot. Esos son detalles de arquitectura.
3. La razon de exigir snapshot literal y no solo un hash es que la auditoria humana a menudo necesita leer el contexto, no solo verificar que algo no fue alterado. Un hash sirve para integridad pero no para diagnosis.

</details>

<a id="F-REVCTX-R008"></a>
### Rule[F-REVCTX-R008] - El payload del override es JSON con la misma forma observable que la salida de `get task`
**Severity**: critical | **Validation**: VALIDATED

> El payload que el operador entrega via `--correction-context` o `--correction-context-file` ([F-REVCTX-R001](#F-REVCTX-R001)) es **JSON**, y su forma espeja exactamente la del `correctionContext` que el sistema emite cuando el operador inspecciona una tarea con `get task` (ver [F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md) para `LEMMA_ABSENCE` y los features de contexto equivalentes para otros `DiagnosisKind`). El proposito es que el operador pueda **copiar-pegar la salida de `get task`, editar los campos que cambiaron por efecto de las propuestas previas, y entregar el resultado como override sin transformacion intermedia**.
>
> Como contrato observable: para una tarea cuyo contexto derivado se obtiene via `get task`, pasar exactamente esa misma salida JSON como override (sin modificarla) debe pasar la validacion estructural de [F-REVCTX-R003](#F-REVCTX-R003) y producir un resultado equivalente al de invocar `revise` sin override sobre la misma tarea (modulo la trazabilidad de origen).

<details><summary>Detail</summary>

1. El simetria con `get task` es intencional: el cliente del dashboard ya consume `get task` (o un flujo equivalente que reusa el mismo serializador) para mostrar contexto al operador. Reusar la misma forma del payload elimina una clase de errores de integracion y reduce la documentacion necesaria.
2. La equivalencia "copiar-pegar la salida de `get task` sin modificarla = mismo resultado que sin override" es la propiedad concreta verificable: un test puede tomar la salida de `get task` para una tarea, pasarla a `revise --correction-context=<...>` y comparar la propuesta resultante con la del flujo sin override.
3. Esta regla **no** redefine el contrato estructural del `correctionContext` por `DiagnosisKind` (eso queda en FEAT-RCLA, FEAT-RCSL y futuros). Lo que define es que la **serializacion** del override sea la misma serializacion que ya emite `get task`, para minimizar friccion del cliente.

</details>

---

## Contexto

### El problema observado

El operador del dashboard de Learney revisa muchas tareas dentro del mismo plan. Mientras decide, ve un **plan proyectado** ([FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) que refleja el efecto acumulado de las decisiones que ya tomo (`APPROVED` + `PENDING_APPROVAL`). Para cada tarea proyectada, el cliente le muestra al operador un `correctionContext` proyectado: oracion, traduccion, lemas fuera de nivel, lemas sugeridos -estos ultimos descontando ya los lemas que las propuestas anteriores ya consumieron-.

Cuando el operador finalmente aprueba la tarea ("aplicar la revision"), el cliente invoca `revise task <id>`. Pero el verbo `revise` opera contra el plan **persistido**: la cadena downstream construye su `correctionContext` a partir del `AuditReport` original, sin saber nada del estado proyectado que el operador efectivamente vio en la UI. Esto crea una desincronizacion concreta y dolorosa:

- El operador vio que la tarea sugiere reemplazar la palabra X por una palabra de un set de candidatos `S_proyectado` (el set ya filtrado de los que consumieron las propuestas previas).
- La estrategia, al ejecutar `revise`, consume `S_basal` (el set original, sin filtrar). El LLM puede elegir un candidato que ya consumio otra propuesta pendiente.
- El operador ve un resultado coherente con su decision **solo por suerte**.

### Lo que aporta este feature

Un mecanismo opt-in para que el verbo `revise` reciba el `correctionContext` desde el cliente y lo use tal cual como insumo de la estrategia. El cliente tiene la unica vista completa de "que vio el operador realmente cuando aprobo": le entrega ese contexto al sistema, el sistema lo consume sin tocarlo. La cadena de revision (estrategia -> propuesta -> validador -> persistencia -> escritura del curso) sigue identica.

### Lo que NO aporta este feature

- No redefine la forma del `correctionContext` por `DiagnosisKind`. FEAT-RCSL y FEAT-RCLA siguen siendo la fuente.
- No introduce un Reviser nuevo ni una estrategia nueva: el override entra como input a las estrategias ya existentes (FEAT-LAPS para `LEMMA_ABSENCE`, futuras estrategias para otros kinds).
- No modifica la mecanica de planes persistidos, la resolucion de planes (default vs `--plan`), la mecanica de aprobacion (FEAT-REVAPR), ni la persistencia del artefacto.
- No automatiza la construccion del override por parte del sistema. Producir un override **proyectado** es responsabilidad del cliente; FEAT-PLANEF le entrega el insumo (contexto inline en el plan efimero, [F-PLANEF-R002](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md#F-PLANEF-R002)), pero la decision de que contexto pasar a `revise` la toma el cliente.
- No valida la coherencia semantica profunda del override (e.g. que las palabras sugeridas no se solapen con las pendientes del operador). El cliente arma el override; el sistema confia en lo que recibe.

---

## Alcance

- **In scope**:
  - El verbo `revise task <id>` admite un mecanismo opt-in con dos flags mutuamente excluyentes (`--correction-context=<json>` y `--correction-context-file=<path>`) para recibir un `correctionContext` provisto externamente ([F-REVCTX-R001](#F-REVCTX-R001)).
  - Cuando se provee, el sistema usa el override sin combinarlo ni complementarlo con contexto derivado ni con el estado actual del curso ([F-REVCTX-R002](#F-REVCTX-R002)).
  - El override se valida estructuralmente contra el contrato del `correctionContext` por `DiagnosisKind` y debe declarar la misma identidad logica `(nodeId, diagnosisKind)` que la tarea persistente ([F-REVCTX-R003](#F-REVCTX-R003)).
  - El override solo aplica a `DiagnosisKind` con contrato de contexto definido ([F-REVCTX-R004](#F-REVCTX-R004)).
  - El artefacto persistido lleva un indicador de origen del contexto y, cuando hubo override, un snapshot literal del payload entregado ([F-REVCTX-R007](#F-REVCTX-R007)).
  - El payload es JSON con la misma forma observable que la salida de `get task` ([F-REVCTX-R008](#F-REVCTX-R008)).
- **Out of scope**:
  - Modificar el contrato de `correctionContext` por `DiagnosisKind`.
  - Modificar la mecanica de aprobacion (FEAT-REVAPR), persistencia del artefacto (FEAT-REVBYP), o las estrategias activas (FEAT-LAPS).
  - Validar coherencia semantica profunda del override (es responsabilidad del cliente).
  - Construir el override proyectado por el sistema (es responsabilidad del cliente; [FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md) le entrega el insumo).
  - Aceptar override en otros verbos del CLI (`analyze`, `plan`, etc.) -solo `revise` lo necesita por el caso de uso del dashboard-.

---

## User Journeys

### Journey[F-REVCTX-J001] - El operador aprueba una tarea sobre el contexto proyectado y `revise` consume el override
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-REVCTX-J001
    name: El operador aprueba una tarea sobre el contexto proyectado y revise consume el override
    flow:
      - id: invocar_revise_con_override
        action: "El cliente invoca revise task <id> con un unico flag de override (correction-context inline o correction-context-file), sobre una tarea cuyo DiagnosisKind tiene contrato de contexto definido, y el payload es JSON con la forma observable de get task"
        gate: [F-REVCTX-R001, F-REVCTX-R004, F-REVCTX-R008]
        outcomes:
          - when: "El override respeta el contrato estructural del correctionContext y declara el mismo (nodeId, diagnosisKind) que la tarea persistente"
            then: ejecutar_estrategia_con_override
          - when: "El override no respeta el contrato estructural o declara un (nodeId, diagnosisKind) distinto al de la tarea persistente"
            then: rechazar_override

      - id: ejecutar_estrategia_con_override
        action: "El sistema invoca la estrategia de revision activa pasandole el override completo como correctionContext, sin derivar contexto por su cuenta ni cruzar-validar contra el estado actual del curso para la tarea"
        gate: [F-REVCTX-R002]
        then: producir_propuesta

      - id: producir_propuesta
        action: "La estrategia produce la RevisionProposal y el sistema persiste el artefacto con el indicador de origen 'override' y el snapshot literal del payload entregado"
        gate: [F-REVCTX-R006, F-REVCTX-R007]
        result: success

      - id: rechazar_override
        action: "El sistema rechaza la invocacion antes de invocar la estrategia, sin persistir artefacto ni modificar la tarea ni el curso, con un mensaje que identifica el motivo del rechazo (validez estructural o identidad logica)"
        gate: [F-REVCTX-R003]
        result: failure
```

### Journey[F-REVCTX-J002] - El cliente invoca `revise` con override sobre una tarea de un kind que no tiene contrato de contexto
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-REVCTX-J002
    name: El cliente invoca revise con override sobre una tarea de un kind sin contrato de contexto
    flow:
      - id: invocar_revise_con_override_no_aplicable
        action: "El cliente invoca revise task <id> con un correctionContext provisto externamente, sobre una tarea cuyo DiagnosisKind cae al Reviser bypass por no tener estrategia dedicada"
        gate: [F-REVCTX-R004]
        then: rechazar_kind_no_aplicable

      - id: rechazar_kind_no_aplicable
        action: "El sistema rechaza la invocacion con un mensaje claro indicando que revise no acepta correctionContext para ese DiagnosisKind, sin persistir artefacto ni modificar la tarea ni el curso"
        gate: [F-REVCTX-R004]
        result: failure
```

### Journey[F-REVCTX-J003] - El cliente invoca `revise` **sin** override y el comportamiento es el actual
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-REVCTX-J003
    name: Invocar revise sin override preserva el comportamiento actual
    flow:
      - id: invocar_revise_sin_override
        action: "El cliente invoca revise task <id> sin proveer ninguno de los flags de correctionContext externo"
        gate: [F-REVCTX-R001]
        then: revise_actual

      - id: revise_actual
        action: "El sistema deriva el contexto por su cuenta a partir del plan persistente y su AuditReport, ejecuta la estrategia activa y persiste el artefacto con el indicador de origen 'derived' y el mismo flujo que tenia antes de este feature"
        gate: [F-REVCTX-R001, F-REVCTX-R006, F-REVCTX-R007]
        result: success
```

### Journey[F-REVCTX-J004] - El cliente invoca `revise` con los dos flags de override a la vez
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-REVCTX-J004
    name: El cliente pasa correction-context y correction-context-file en la misma invocacion
    flow:
      - id: invocar_revise_con_ambos_flags
        action: "El cliente invoca revise task <id> pasando simultaneamente --correction-context y --correction-context-file"
        gate: [F-REVCTX-R001]
        then: rechazar_flags_excluyentes

      - id: rechazar_flags_excluyentes
        action: "El sistema rechaza la invocacion con un mensaje que indica que los dos flags son mutuamente excluyentes, antes de leer ninguno de los dos payloads, sin invocar la estrategia, sin persistir artefacto y sin modificar la tarea ni el curso"
        gate: [F-REVCTX-R001]
        result: failure
```

---

## Open Questions

<a id="DOUBT-OVERRIDE-SHAPE"></a>
### Doubt[DOUBT-OVERRIDE-SHAPE] - Shape concreto de la entrega del override
**Status**: RESOLVED (2026-05-09)

El operador sugirio dos shapes alternativos: `--correction-context=<json>` (inline) o `--correction-context-file=<path>` (referencia a archivo). Tambien existe la opcion de stdin.

- [ ] Opcion A: Solo flag con JSON inline (`--correction-context='{"sentence":...}'`). Simple pero limita el tamano (line length de la shell).
- [ ] Opcion B: Solo flag con path a archivo (`--correction-context-file=/tmp/ctx.json`). Sin limites de tamano, requiere que el cliente escriba un archivo temporal.
- [x] Opcion C: Ambos flags, mutuamente excluyentes. Cubre los dos casos de uso.
- [ ] Opcion D: Lectura por stdin (`--correction-context=- < ctx.json` o convencion fija). Mas idiomatico para integraciones programaticas, menos descubrible.

**Answer**: Opcion C. Los dos flags `--correction-context=<json>` y `--correction-context-file=<path>` se aceptan, mutuamente excluyentes. Si el operador pasa los dos en la misma invocacion, el comando falla con un error claro antes de leer ninguno de los dos payloads, sin efectos colaterales. Quedo formalizado en [F-REVCTX-R001](#F-REVCTX-R001).

<a id="DOUBT-OVERRIDE-CHANNEL"></a>
### Doubt[DOUBT-OVERRIDE-CHANNEL] - Formato de serializacion del override
**Status**: RESOLVED (2026-05-09)

Asumimos JSON porque el `correctionContext` ya viaja como JSON en `get task` ([F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). Si se eligiera otro formato, deberia ser uno que el cliente ya produzca naturalmente.

- [x] Opcion A: JSON. Consistente con `get task` y con lo que el plan efimero con contexto inline ([F-PLANEF-R002](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md#F-PLANEF-R002)) emite.
- [ ] Opcion B: YAML. Mas legible pero menos consistente con el resto del CLI.

**Answer**: Opcion A. El payload del override es **JSON**, y su forma espeja exactamente la salida observable del comando `get task` para que el operador pueda copiar-pegar y editar sin transformacion intermedia. El contrato observable se formalizo en la regla nueva [F-REVCTX-R008](#F-REVCTX-R008).

<a id="DOUBT-OVERRIDE-COHERENCE"></a>
### Doubt[DOUBT-OVERRIDE-COHERENCE] - Profundidad de la verificacion de coherencia entre override y tarea persistente
**Status**: RESOLVED (2026-05-09)

[F-REVCTX-R003](#F-REVCTX-R003) obliga la validacion estructural. [F-REVCTX-R005](#F-REVCTX-R005) hablaba originalmente de delegar al cliente la coherencia semantica. Hay un espectro de verificaciones intermedias posibles. La opcion B de la lista original (validar `taskId`) fue **descartada** por el operador con el siguiente argumento: dos planes distintos producen `taskId` distintos para el mismo problema logico, asi que `taskId` no es la identidad correcta para el sanity check. Lo que identifica "la misma tarea logica" es la dupla `(nodeId, diagnosisKind)`.

- [ ] Opcion A: Solo validar estructura. El `nodeId` del override no se cruza con el de la tarea. Maxima flexibilidad para el cliente. Maximo riesgo de error silencioso.
- [ ] Opcion B (descartada): Validar `taskId`. Es el chequeo intuitivo pero **incorrecto**: dos planes distintos producen `taskId` distintos para el mismo problema logico.
- [x] Opcion B' (elegida): Validar que el `nodeId` y el `diagnosisKind` declarados en el payload coincidan con los de la tarea persistente identificada por `<task-id>`. Captura la identidad logica estable entre planes; rechaza el override "de otra tarea".
- [ ] Opcion C: Validar ademas `cefrLevel`, mas otros campos. Mas restrictivo, riesgo de rechazar overrides validos cuando el contexto proyectado refleja un cambio legitimo.
- [ ] Opcion D: Igual que C pero solo como warning.

**Answer**: Opcion B'. El override es aceptado solo si el `nodeId` y el `diagnosisKind` declarados en el payload coinciden con los de la tarea persistente identificada por `<task-id>`. Si no coinciden, el comando rechaza el override antes de invocar la estrategia, sin efectos colaterales. La lectura semantica es *"es la misma tarea logica pero con el contexto actualizado"*. Quedo formalizado en [F-REVCTX-R003](#F-REVCTX-R003) y reflejado en [F-REVCTX-R005](#F-REVCTX-R005).

<a id="DOUBT-OVERRIDE-AUDITABILITY"></a>
### Doubt[DOUBT-OVERRIDE-AUDITABILITY] - Forma exacta de la trazabilidad del override en el artefacto persistido
**Status**: RESOLVED (2026-05-09)

El artefacto persistido debe permitir distinguir las dos fuentes de contexto. La forma concreta puede variar:

- [x] Opcion A: Snapshot literal del override embebido en el artefacto, mas un indicador de origen (`derived` vs `override`). Maxima trazabilidad.
- [ ] Opcion B: Solo el indicador de origen, sin snapshot. Minima trazabilidad, minimo tamano.
- [ ] Opcion C: Hash del override + indicador de origen. Permite verificar identidad a posteriori sin guardar contenido, pero no permite leer el contexto.

**Answer**: Opcion A. Cuando una revision uso override, el artefacto persistido lleva (a) un indicador de origen del contexto (`derived` vs `override`) y (b) un snapshot literal del payload entregado por el operador. Esto permite reproducir auditorias a posteriori y entender exactamente que contexto recibio el sistema. La razon de exigir snapshot literal y no solo un hash es que la auditoria humana suele necesitar **leer** el contexto, no solo verificar integridad. Quedo formalizado en la regla nueva [F-REVCTX-R007](#F-REVCTX-R007).

<a id="DOUBT-OVERRIDE-MUTACION-LATERAL"></a>
### Doubt[DOUBT-OVERRIDE-MUTACION-LATERAL] - Que pasa con las invariantes de FEAT-LAPS y futuras estrategias frente a un override?
**Status**: RESOLVED (2026-05-09)

Las estrategias activas (FEAT-LAPS para LEMMA_ABSENCE) consumen el `correctionContext` y producen un candidato segun reglas internas (R007 a R016 de FEAT-LAPS). Algunas de esas reglas asumen propiedades del contexto derivado por el sistema (e.g., que `suggestedLemmas` viene del milestone ancestro filtrado por COCA, [F-RCLA-R004b](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). Cuando el cliente entrega un override, esas reglas estructurales del contexto **siguen** vigentes (R003 lo asegura) pero las invariantes secundarias pueden o no respetarse segun como el cliente arme el override.

- [x] Opcion A: La estrategia confia. Si el cliente entrega un override estructuralmente valido pero con invariantes secundarias rotas, la estrategia opera sobre lo que recibio. La calidad del candidato depende de la calidad del override.
- [ ] Opcion B: La estrategia valida invariantes secundarias y rechaza si no se cumplen.

**Answer**: Opcion A. Cuando hay override, la correccion procede confiando en el contenido del override: el sistema **no** re-deriva ni cruza-valida el contexto contra el estado actual del curso. El operador es responsable de la coherencia semantica del contexto que entrega. Las dos unicas defensas son: (1) la auditabilidad ([F-REVCTX-R007](#F-REVCTX-R007)), que permite reconstruir a posteriori que paso, y (2) el sanity check de identidad logica `(nodeId, diagnosisKind)` ([F-REVCTX-R003](#F-REVCTX-R003)), que atrapa el error mas comun (cliente pega el contexto equivocado). Eso es **intencional**. Quedo reflejado en [F-REVCTX-R002](#F-REVCTX-R002) #3.

---

## References

- **FEAT-CLIRV** ([requirement](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md)) - Define el verbo `revise task <id>` y la resolucion del plan persistente para identificar la tarea ([F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015)). FEAT-REVCTX agrega un mecanismo opt-in al mismo verbo, sin alterar la resolucion del plan ni el resto del comando. Citado por [F-REVCTX-R001](#F-REVCTX-R001) y [F-REVCTX-R005](#F-REVCTX-R005).
- **FEAT-REVBYP** ([requirement](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) - Define la estructura de la `RevisionProposal`, el patron de Reviser pluggable por `DiagnosisKind`, el Reviser bypass como fallback, y la persistencia del artefacto. FEAT-REVCTX cambia el insumo de contexto que la estrategia consume, sin alterar la estructura de la propuesta ni la persistencia. Citado por [F-REVCTX-R002](#F-REVCTX-R002), [F-REVCTX-R004](#F-REVCTX-R004) y [F-REVCTX-R006](#F-REVCTX-R006).
- **FEAT-LAPS** ([requirement](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md)) - Define la estrategia de propuesta MVP para `LEMMA_ABSENCE` que consume el `correctionContext` completo de FEAT-RCLA. FEAT-REVCTX permite que ese mismo consumo opere sobre un contexto provisto externamente, sin modificar la estrategia. Relacionado con [DOUBT-OVERRIDE-MUTACION-LATERAL](#DOUBT-OVERRIDE-MUTACION-LATERAL).
- **FEAT-RCLA** ([requirement](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)) - Define el contrato estructural del `correctionContext` para tareas `LEMMA_ABSENCE` y la salida observable de `get task` ([F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). FEAT-REVCTX no redefine ese contrato; lo reusa para validar el override y exige que el payload de override espeje exactamente la salida observable de `get task`. Citado por [F-REVCTX-R003](#F-REVCTX-R003) y [F-REVCTX-R008](#F-REVCTX-R008).
- **FEAT-RCSL** - Define el contrato estructural del `correctionContext` para tareas `SENTENCE_LENGTH`. Misma relacion que con FEAT-RCLA. Citado por [F-REVCTX-R003](#F-REVCTX-R003) implicitamente.
- **FEAT-REVAPR** - Define los modos de aprobacion (`auto`, `human`) y la regla anti-acumulacion de propuestas pendientes. FEAT-REVCTX preserva todo ese flujo sin modificacion. Citado por [F-REVCTX-R006](#F-REVCTX-R006).
- **FEAT-PLANEF** ([requirement](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) - Le entrega al cliente el insumo (plan efimero con `correctionContext` proyectado inline, [F-PLANEF-R002](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md#F-PLANEF-R002)) que el cliente despues usa como override en `revise`. FEAT-REVCTX y FEAT-PLANEF son complementarios y forman el ciclo "el operador ve el contexto proyectado / el operador aprueba sobre el contexto proyectado".
