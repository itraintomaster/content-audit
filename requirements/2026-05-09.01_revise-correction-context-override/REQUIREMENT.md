---
feature:
  id: FEAT-REVCTX
  code: F-REVCTX
  name: Override del contexto de correccion en el verbo revise
  priority: critical
---

# Override del contexto de correccion en el verbo revise

## TL;DR

**Que**: El verbo `revise task <id>` acepta opcionalmente un **`correctionContext` provisto externamente por el cliente** (shape concreto a confirmar en [DOUBT-OVERRIDE-SHAPE](#DOUBT-OVERRIDE-SHAPE)). Cuando se provee, el sistema usa ese contexto como insumo de la estrategia de revision activa, en lugar del contexto que derivaria por si mismo del plan persistente y su `AuditReport` fuente. Cuando no se provee, el comportamiento es el actual y no cambia.

**Por que**: El operador del dashboard revisa decenas de tareas y, mientras decide, ve un **plan proyectado** ([FEAT-PLANEF](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) que refleja el efecto acumulado de sus decisiones aceptadas y pendientes. Cuando finalmente aprueba una tarea, espera que la revision se ejecute sobre el contexto proyectado que efectivamente vio en la UI. Hoy `revise` deriva el contexto del plan persistido + `AuditReport` original, ignorando las pendientes; la consecuencia es que la estrategia (LLM en LEMMA_ABSENCE) puede proponer una palabra que ya consumio una propuesta previa pendiente -exactamente el problema que el plan proyectado prometia evitar visualmente. Permitir un override del contexto cierra el ciclo entre lo que el operador ve y lo que el sistema ejecuta.

## Reglas de Negocio

### Grupo A - Aceptacion del override

<a id="F-REVCTX-R001"></a>
### Rule[F-REVCTX-R001] - El verbo `revise` acepta un `correctionContext` provisto externamente, opcional
**Severity**: critical | **Validation**: VALIDATED

> El verbo `revise task <id>` ([F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015)) admite un mecanismo opt-in -sugerido por el operador como `--correction-context=<json>` o `--correction-context-file=<path>`, shape exacto a confirmar en [DOUBT-OVERRIDE-SHAPE](#DOUBT-OVERRIDE-SHAPE)- por el cual el cliente entrega un objeto `correctionContext` al iniciar la revision. Cuando se provee, ese objeto es el unico insumo de contexto que la estrategia de revision activa consume; el sistema **no** deriva contexto por su cuenta a partir del plan persistido ni del `AuditReport` para la tarea revisada. Cuando no se provee, el comportamiento de `revise` es el actual sin modificacion alguna.

<details><summary>Detail</summary>

1. La capacidad es **opt-in**. La invocacion sin override conserva el contrato actual de [F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015) y de las estrategias de revision (FEAT-LAPS y futuras), incluyendo todos los modos de aprobacion (FEAT-REVAPR), la persistencia del artefacto y los exit codes. Un cliente que no usa el override no observa ningun cambio.
2. El override entra **en el lugar** donde hoy la estrategia consume el contexto derivado. La cadena downstream (estrategia -> propuesta -> validador -> persistencia del artefacto -> aplicacion al curso) opera tal cual. El override no altera donde se persiste el artefacto, ni que campos lleva la `RevisionProposal`, ni la mecanica de aprobacion.
3. El shape concreto de la entrega del override (flag con JSON inline, flag con path a archivo, lectura por stdin) y el formato de serializacion son decisiones de implementacion ([DOUBT-OVERRIDE-SHAPE](#DOUBT-OVERRIDE-SHAPE), [DOUBT-OVERRIDE-CHANNEL](#DOUBT-OVERRIDE-CHANNEL)).

</details>

<a id="F-REVCTX-R002"></a>
### Rule[F-REVCTX-R002] - Cuando se provee override, el sistema **no** deriva contexto por su cuenta para la tarea revisada
**Severity**: critical | **Validation**: VALIDATED

> Si el cliente provee un `correctionContext` por la via de [F-REVCTX-R001](#F-REVCTX-R001), el sistema usa ese objeto como insumo de la estrategia de revision activa **sin** combinarlo, mergearlo, complementarlo ni validarlo cruzando contra el contexto que derivaria por si mismo del plan persistente o del `AuditReport`. Las invariantes observables son:
>
> 1. **Reemplazo, no merge**: la estrategia recibe el override completo. Campos ausentes en el override no se rellenan desde el contexto derivado. Campos presentes en el override no se sobrescriben con valores derivados.
> 2. **Cero derivacion**: ninguna lectura sobre el `AuditReport` fuente ni sobre el arbol de diagnosticos ocurre **para construir el contexto** de esta revision. Lecturas que sigan siendo necesarias para resolver la tarea (`taskId` -> tarea concreta dentro del plan persistente) se mantienen sin cambios; lo que se elimina es la fase de construccion del contexto, no la fase de identificar la tarea.
> 3. **Trazabilidad**: el artefacto de propuesta persistido ([F-REVBYP-R010](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) registra que el contexto consumido provino de un override externo, distinguible a posteriori del contexto derivado por el sistema. Que campos exactos se conservan (snapshot completo del override, hash, indicador de origen, etc.) es decision de arquitectura ([DOUBT-OVERRIDE-AUDITABILITY](#DOUBT-OVERRIDE-AUDITABILITY)).

<details><summary>Detail</summary>

1. La regla es deliberadamente "el cliente sabe": el override le da al cliente la responsabilidad de entregar un contexto util, y el sistema no intenta corregirlo ni aumentarlo. Si el cliente entrega un contexto incompleto, la estrategia operara con lo que recibio (y podra fallar segun sus propias precondiciones, ver [F-REVCTX-R003](#F-REVCTX-R003)).
2. La razon de no permitir merge es que cualquier composicion (override > derivado, derivado > override, union, interseccion) introduce semanticas no obvias que el cliente no puede predecir. Reemplazo total es la unica semantica donde lo que el operador vio en la UI es exactamente lo que la estrategia consume.
3. La regla obliga la trazabilidad pero no la forma exacta. Lo testeable es que el artefacto persistido permita distinguir las dos fuentes a posteriori; la estructura concreta queda abierta a la arquitectura.

</details>

---

### Grupo B - Validacion del override

<a id="F-REVCTX-R003"></a>
### Rule[F-REVCTX-R003] - El override debe respetar el contrato del `correctionContext` por `DiagnosisKind`
**Severity**: critical | **Validation**: VALIDATED

> El `correctionContext` provisto por el cliente debe respetar el contrato estructural ya definido por los features de contexto de correccion para el `DiagnosisKind` de la tarea revisada (FEAT-RCSL para `SENTENCE_LENGTH`, [FEAT-RCLA](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md) para `LEMMA_ABSENCE`, y los que existan en el futuro). Si el override no respeta ese contrato, el comando **rechaza la invocacion** antes de invocar la estrategia, sin persistir artefacto ni modificar la tarea ni el curso. El mensaje de error identifica el motivo del rechazo de forma accionable.

<details><summary>Detail</summary>

1. El contrato estructural del `correctionContext` por `DiagnosisKind` ya esta definido por los features de contexto. R003 obliga que el override respete ese contrato; **no** redefine la forma del contexto.
2. La regla cubre dos clases de error de input:
   - **Estructural**: faltan campos obligatorios, tipos incorrectos, valores fuera de dominio (e.g. `cefrLevel` no valido). Se rechaza con un mensaje que identifica el campo y el motivo.
   - **Coherencia minima**: el contexto se refiere a la tarea correcta (`taskId` consistente con el id pasado al comando). Que tan profunda es la verificacion de coherencia (solo `taskId`, o tambien `nodeId`, `cefrLevel`, etc.) es decision de arquitectura ([DOUBT-OVERRIDE-COHERENCE](#DOUBT-OVERRIDE-COHERENCE)).
3. La regla obliga el rechazo **antes** de invocar la estrategia. Esto evita que la estrategia falle con errores opacos por un input mal formado, y mantiene la propiedad de "no efectos colaterales" (ningun artefacto, ningun cambio en la tarea).
4. Errores semanticos mas profundos que el cliente comete cuando arma un override "valido pero incoherente" (e.g. lemas sugeridos vacios pero misplaced presentes, o un `quizSentence` malformado segun la DSL de FEAT-QSENT) no son responsabilidad de R003: viajan a la estrategia y se manifiestan segun las precondiciones de cada estrategia (FEAT-LAPS y futuras).

</details>

**Error**: "El correctionContext provisto no es valido para una tarea de tipo '{diagnosisKind}': {motivo}"

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
> 3. El cliente es responsable de que el `<task-id>` que pasa al comando se refiera a una tarea cuyo `nodeId` sea el mismo nodo del curso que la tarea proyectada que el operador vio. Es decir, el cliente alinea el override y el `task-id` por su cuenta antes de invocar `revise`.

<details><summary>Detail</summary>

1. La razon de no exigir que el plan resuelto coincida con el plan proyectado es practica: el plan proyectado no es persistente (FEAT-PLANEF) y por lo tanto no es resoluble. La unica entidad resoluble es el plan persistente del operador, que en el caso normal contiene la misma tarea (mismo `nodeId`) pero con el contexto basal -no proyectado- en su estado interno. Lo que el override entrega es justamente el contexto proyectado que reemplaza al basal.
2. La alineacion `task-id` <-> `nodeId` la hace el cliente porque solo el cliente sabe la correspondencia entre la tarea proyectada que el operador aprobo y la tarea persistente equivalente. content-audit no puede inferirlo: dos planes distintos sobre el mismo `AuditReport` reasignan `taskId` por reglas internas.
3. Como consecuencia, el sistema **no valida** que el `nodeId` del override coincida con el `nodeId` de la tarea persistente. Esa coherencia es responsabilidad del cliente. Si el cliente invoca con un `task-id` cuyo `nodeId` no se corresponde con el del override, la revision aplica el contexto del override sobre la tarea identificada por `task-id` -lo que el cliente pidio explicitamente-. El resultado puede ser semanticamente incorrecto, pero esa es una falla del cliente, no del comando. (Si en el futuro el operador prefiere que el comando rechace este desalineamiento, eso seria una regla nueva, no parte de este requerimiento. Ver [DOUBT-OVERRIDE-COHERENCE](#DOUBT-OVERRIDE-COHERENCE).)

</details>

---

### Grupo D - Modos de aprobacion y persistencia

<a id="F-REVCTX-R006"></a>
### Rule[F-REVCTX-R006] - El override no altera los modos de aprobacion ni la persistencia del artefacto
**Severity**: major | **Validation**: VALIDATED

> Una invocacion de `revise` con override produce el mismo flujo de aprobacion (FEAT-REVAPR: `auto`, `human`) y persiste el mismo artefacto de propuesta ([FEAT-REVBYP](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) que una invocacion sin override. El unico cambio observable es:
>
> 1. La fuente del contexto consumido por la estrategia (override en lugar de derivacion).
> 2. La trazabilidad del artefacto, que distingue las dos fuentes ([F-REVCTX-R002](#F-REVCTX-R002) #3).

<details><summary>Detail</summary>

1. Esta regla acota explicitamente el alcance del override para evitar que el feature se filtre a aprobacion / artefactos. El operador sigue eligiendo modo de aprobacion por el mecanismo existente (env var de FEAT-REVAPR), y el artefacto persistido preserva el mismo formato con el agregado de la marca de origen.
2. El exit code del comando, los mensajes de error de aprobacion, la mecanica de persistencia del curso modificado, y el handling de tareas con propuesta pendiente (FEAT-REVAPR R010) operan identico.

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
  - El verbo `revise task <id>` admite un mecanismo opt-in para recibir un `correctionContext` provisto externamente ([F-REVCTX-R001](#F-REVCTX-R001)).
  - Cuando se provee, el sistema usa el override sin combinarlo ni complementarlo con contexto derivado ([F-REVCTX-R002](#F-REVCTX-R002)).
  - El override se valida estructuralmente contra el contrato del `correctionContext` por `DiagnosisKind` ([F-REVCTX-R003](#F-REVCTX-R003)).
  - El override solo aplica a `DiagnosisKind` con contrato de contexto definido ([F-REVCTX-R004](#F-REVCTX-R004)).
  - La trazabilidad del artefacto distingue contexto override de contexto derivado ([F-REVCTX-R002](#F-REVCTX-R002) #3).
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
        action: "El cliente invoca revise task <id> con un correctionContext provisto externamente, sobre una tarea cuyo DiagnosisKind tiene contrato de contexto definido"
        gate: [F-REVCTX-R001, F-REVCTX-R004]
        outcomes:
          - when: "El override respeta el contrato estructural del correctionContext para el DiagnosisKind de la tarea"
            then: ejecutar_estrategia_con_override
          - when: "El override no respeta el contrato estructural"
            then: rechazar_override

      - id: ejecutar_estrategia_con_override
        action: "El sistema invoca la estrategia de revision activa pasandole el override completo como correctionContext, sin derivar contexto por su cuenta para la tarea"
        gate: [F-REVCTX-R002]
        then: producir_propuesta

      - id: producir_propuesta
        action: "La estrategia produce la RevisionProposal y el sistema persiste el artefacto, marcando que el contexto consumido provino de un override externo"
        gate: [F-REVCTX-R002, F-REVCTX-R006]
        result: success

      - id: rechazar_override
        action: "El sistema rechaza la invocacion antes de invocar la estrategia, sin persistir artefacto ni modificar la tarea ni el curso, con un mensaje que identifica el motivo del rechazo"
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
        action: "El cliente invoca revise task <id> sin proveer correctionContext externo"
        gate: [F-REVCTX-R001]
        then: revise_actual

      - id: revise_actual
        action: "El sistema deriva el contexto por su cuenta a partir del plan persistente y su AuditReport, ejecuta la estrategia activa y persiste el artefacto con el mismo flujo que tenia antes de este feature"
        gate: [F-REVCTX-R001, F-REVCTX-R006]
        result: success
```

---

## Open Questions

<a id="DOUBT-OVERRIDE-SHAPE"></a>
### Doubt[DOUBT-OVERRIDE-SHAPE] - Shape concreto de la entrega del override
**Status**: OPEN (para arquitecto, con preferencia del operador)

El operador sugirio dos shapes alternativos: `--correction-context=<json>` (inline) o `--correction-context-file=<path>` (referencia a archivo). Tambien existe la opcion de stdin.

- [ ] Opcion A: Solo flag con JSON inline (`--correction-context='{"sentence":...}'`). Simple pero limita el tamano (line length de la shell).
- [ ] Opcion B: Solo flag con path a archivo (`--correction-context-file=/tmp/ctx.json`). Sin limites de tamano, requiere que el cliente escriba un archivo temporal.
- [x] Opcion C: Ambos flags, mutuamente excluyentes. Cubre los dos casos de uso.
- [ ] Opcion D: Lectura por stdin (`--correction-context=- < ctx.json` o convencion fija). Mas idiomatico para integraciones programaticas, menos descubrible.

**Answer**: Pendiente. La preferencia provisoria es la Opcion C; el operador puede confirmar.

<a id="DOUBT-OVERRIDE-CHANNEL"></a>
### Doubt[DOUBT-OVERRIDE-CHANNEL] - Formato de serializacion del override
**Status**: OPEN (para arquitecto)

Asumimos JSON porque el `correctionContext` ya viaja como JSON en `get task` ([F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). Si se eligiera otro formato, deberia ser uno que el cliente ya produzca naturalmente.

- [x] Opcion A: JSON. Consistente con `get task` y con lo que el plan efimero con contexto inline ([F-PLANEF-R002](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md#F-PLANEF-R002)) emite.
- [ ] Opcion B: YAML. Mas legible pero menos consistente con el resto del CLI.

**Answer**: Pendiente. La preferencia clara es JSON.

<a id="DOUBT-OVERRIDE-COHERENCE"></a>
### Doubt[DOUBT-OVERRIDE-COHERENCE] - Profundidad de la verificacion de coherencia entre override y tarea persistente
**Status**: OPEN (para arquitecto, con confirmacion del operador)

[F-REVCTX-R003](#F-REVCTX-R003) obliga la validacion estructural. [F-REVCTX-R005](#F-REVCTX-R005) declara explicitamente que la coherencia semantica entre el `nodeId` del override y el `nodeId` de la tarea persistente es responsabilidad del cliente. Pero hay un espectro de verificaciones intermedias posibles que no son ni puro estructural ni puro semantico profundo:

- [ ] Opcion A: Solo validar estructura (R003). El `nodeId` del override no se cruza con el de la tarea. Maxima flexibilidad para el cliente. Maximo riesgo de error silencioso si el cliente se equivoca.
- [x] Opcion B: Validar que el `taskId` del override coincida con el `<task-id>` pasado al comando (sanity check minimo). Bajo costo, atrapa el error mas comun (cliente pega el contexto equivocado).
- [ ] Opcion C: Validar ademas que el `nodeId` y el `cefrLevel` del override coincidan con los de la tarea persistente. Mas restrictivo: si el cliente entrega un override "de otro nodo", se rechaza.
- [ ] Opcion D: Igual que Opcion C pero solo como **warning** (no rechazo): el comando ejecuta de todas formas pero emite un mensaje a stderr.

**Answer**: Pendiente. La preferencia provisoria es la Opcion B (sanity check minimo de `taskId`).

<a id="DOUBT-OVERRIDE-AUDITABILITY"></a>
### Doubt[DOUBT-OVERRIDE-AUDITABILITY] - Forma exacta de la trazabilidad del override en el artefacto persistido
**Status**: OPEN (para arquitecto)

[F-REVCTX-R002](#F-REVCTX-R002) #3 obliga que el artefacto persistido permita distinguir las dos fuentes de contexto. La forma concreta puede variar:

- [x] Opcion A: Snapshot completo del override embebido en el artefacto, mas un campo booleano / enum `contextSource = "override" | "derived"`. Maxima trazabilidad, maximo tamano del artefacto.
- [ ] Opcion B: Solo un campo `contextSource`, sin snapshot. El override no queda persistido textualmente; solo el efecto observable (la propuesta) si. Minima trazabilidad, minimo tamano.
- [ ] Opcion C: Hash del override + campo `contextSource`. Permite verificar a posteriori que un override entregado matcheaba con el persistido sin guardar el contenido.

**Answer**: Pendiente. La preferencia provisoria es la Opcion A para que la auditoria del artefacto sea completa, pero el arquitecto puede equilibrar con el tamano observado en planes reales.

<a id="DOUBT-OVERRIDE-MUTACION-LATERAL"></a>
### Doubt[DOUBT-OVERRIDE-MUTACION-LATERAL] - Que pasa con las invariantes de FEAT-LAPS y futuras estrategias frente a un override?
**Status**: OPEN (para confirmar con el operador y el arquitecto)

Las estrategias activas (FEAT-LAPS para LEMMA_ABSENCE) consumen el `correctionContext` y producen un candidato segun reglas internas (R007 a R016 de FEAT-LAPS). Algunas de esas reglas asumen propiedades del contexto derivado por el sistema (e.g., que `suggestedLemmas` viene del milestone ancestro filtrado por COCA, [F-RCLA-R004b](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). Cuando el cliente entrega un override, esas reglas estructurales del contexto **siguen** vigentes (R003 lo asegura) pero las invariantes secundarias (e.g. "los suggestedLemmas estan ordenados por COCA") pueden o no respetarse segun como el cliente arme el override.

Pregunta: las estrategias deben validar esas invariantes secundarias sobre el contexto que reciben (defensiva), o se asume "garbage in, garbage out" y la estrategia confia en lo que recibe?

- [x] Opcion A: La estrategia confia. Si el cliente entrega un override estructuralmente valido pero con invariantes secundarias rotas, la estrategia opera sobre lo que recibio. La calidad del candidato depende de la calidad del override. Es lo que ya hace hoy con el contexto derivado.
- [ ] Opcion B: La estrategia valida invariantes secundarias y rechaza si no se cumplen. Mas robusto pero acopla las estrategias a invariantes que hoy son emergentes del derivador, no contractuales.

**Answer**: Pendiente. La preferencia provisoria es la Opcion A (paralelismo con el comportamiento actual: la estrategia consume el contexto que recibe). Si el operador prefiere lo contrario, las invariantes secundarias deberian formalizarse en los features de contexto, no aqui.

---

## References

- **FEAT-CLIRV** ([requirement](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md)) - Define el verbo `revise task <id>` y la resolucion del plan persistente para identificar la tarea ([F-CLIRV-R015](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R015)). FEAT-REVCTX agrega un mecanismo opt-in al mismo verbo, sin alterar la resolucion del plan ni el resto del comando. Citado por [F-REVCTX-R001](#F-REVCTX-R001) y [F-REVCTX-R005](#F-REVCTX-R005).
- **FEAT-REVBYP** ([requirement](../2026-04-17.01_refiner-revision-bypass/REQUIREMENT.md)) - Define la estructura de la `RevisionProposal`, el patron de Reviser pluggable por `DiagnosisKind`, el Reviser bypass como fallback, y la persistencia del artefacto. FEAT-REVCTX cambia el insumo de contexto que la estrategia consume, sin alterar la estructura de la propuesta ni la persistencia. Citado por [F-REVCTX-R002](#F-REVCTX-R002), [F-REVCTX-R004](#F-REVCTX-R004) y [F-REVCTX-R006](#F-REVCTX-R006).
- **FEAT-LAPS** ([requirement](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md)) - Define la estrategia de propuesta MVP para `LEMMA_ABSENCE` que consume el `correctionContext` completo de FEAT-RCLA. FEAT-REVCTX permite que ese mismo consumo opere sobre un contexto provisto externamente, sin modificar la estrategia. Relacionado con [DOUBT-OVERRIDE-MUTACION-LATERAL](#DOUBT-OVERRIDE-MUTACION-LATERAL).
- **FEAT-RCLA** ([requirement](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)) - Define el contrato estructural del `correctionContext` para tareas `LEMMA_ABSENCE`. FEAT-REVCTX no redefine ese contrato; lo reusa para validar el override. Citado por [F-REVCTX-R003](#F-REVCTX-R003).
- **FEAT-RCSL** - Define el contrato estructural del `correctionContext` para tareas `SENTENCE_LENGTH`. Misma relacion que con FEAT-RCLA. Citado por [F-REVCTX-R003](#F-REVCTX-R003) implicitamente.
- **FEAT-REVAPR** - Define los modos de aprobacion (`auto`, `human`) y la regla anti-acumulacion de propuestas pendientes. FEAT-REVCTX preserva todo ese flujo sin modificacion. Citado por [F-REVCTX-R006](#F-REVCTX-R006).
- **FEAT-PLANEF** ([requirement](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md)) - Le entrega al cliente el insumo (plan efimero con `correctionContext` proyectado inline, [F-PLANEF-R002](../2026-05-07.01_plan-efimero-no-persistido/REQUIREMENT.md#F-PLANEF-R002)) que el cliente despues usa como override en `revise`. FEAT-REVCTX y FEAT-PLANEF son complementarios y forman el ciclo "el operador ve el contexto proyectado / el operador aprueba sobre el contexto proyectado".
