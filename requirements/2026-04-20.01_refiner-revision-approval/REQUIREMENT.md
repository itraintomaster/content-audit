---
feature:
  id: FEAT-REVAPR
  code: F-REVAPR
  name: Fase de revision con aprobacion humana
  priority: critical
---

# Fase de revision con aprobacion humana

FEAT-REVBYP introdujo la fase de **revision** como un flujo sincronico end-to-end: en una sola invocacion de `revise task <id>`, el sistema genera una propuesta, la valida, la persiste como artefacto y la aplica al curso. En esa iteracion el validator era el **bypass** (auto-aprueba todo), de modo que la aprobacion no involucraba al operador.

Este requerimiento introduce un **modo de aprobacion humana**: la revision se parte en dos fases separadas por un estado persistido de "pendiente de aprobacion". El operador corre primero `revise task <id>`, que produce la propuesta y la deja esperando decision; luego inspecciona el artefacto y corre `approve proposal <id>` o `reject proposal <id>` para decidir. Solo al aprobarse se reescribe el curso. El rechazo devuelve la tarea al estado previo sin tocar el curso.

El objetivo es validar que la pipeline de revision soporta un **punto de corte operativo** entre la generacion de la propuesta y su aplicacion, sin introducir todavia logica de revision real ni workflows multi-etapa.

## Contexto

### La pipeline antes y despues

Antes (FEAT-REVBYP):

```
revise task <id>
  -> propone -> valida (bypass APPROVED) -> persiste artefacto -> aplica al curso -> DONE
```

Despues de este requerimiento, segun el modo de aprobacion configurado:

```
# Modo auto (equivalente al comportamiento actual, preservado)
revise task <id>
  -> propone -> valida (auto APPROVED) -> persiste artefacto -> aplica al curso -> DONE

# Modo humano (nuevo)
revise task <id>
  -> propone -> valida (PENDING_APPROVAL) -> persiste artefacto -> tarea AWAITING_APPROVAL
# punto de corte persistido

approve proposal <id>
  -> decide APPROVED -> aplica al curso -> tarea DONE

reject proposal <id>
  -> decide REJECTED -> curso sin tocar -> tarea vuelve a PENDING
```

### Relacion con features existentes

- **FEAT-REVBYP**: aporta el `Reviser` pluggable por `DiagnosisKind` (R003/R004), el `RevisionValidator` como punto de extension (R006), la organizacion del artefacto bajo `.content-audit/revisions/<planId>/<proposalId>.*` (R008-R010), la aplicacion al curso via `CourseRepository` (R011), la no-modificacion del curso en caso de rechazo (R012), las transiciones de estado de la tarea (R013) y el orden "artefacto primero, curso despues" (R014). **Este requerimiento reutiliza todos esos mecanismos sin modificarlos**. Lo unico que cambia es:
  1. Se suma un tercer veredicto (`PENDING_APPROVAL`) a los dos ya existentes (`APPROVED`, `REJECTED`).
  2. Aparece un nuevo `RevisionValidator` (el validator "humano") que, en vez de decidir en el momento, devuelve `PENDING_APPROVAL` para que la decision la tome el operador mas tarde.
  3. Se introduce una **segunda fase** de decision (`approve` / `reject`) que toma un artefacto `PENDING_APPROVAL` y lo transiciona a `APPROVED` (aplicando al curso) o `REJECTED` (sin tocar el curso).

- **FEAT-CLIRV**: aporta la gramatica kubectl-style (verbo-recurso). Este requerimiento **extiende** esa gramatica con un recurso nuevo (`proposals`) y dos verbos nuevos (`approve`, `reject`). Las operaciones de lectura sobre el recurso nuevo siguen los patrones de R001/R006 de CLIRV (lista cuando no hay id, individual cuando hay id; formas singular y plural intercambiables).

- **Refiner plan / refiner next**: el estado de la tarea se maneja con el modelo ya existente (`PENDING`, `DONE`, etc.). Este requerimiento necesita representar el estado intermedio "esperando aprobacion"; la forma concreta (reusar un estado existente, agregar uno nuevo, o un flag ortogonal) es una decision de arquitectura (ver DOUBT-AWAITING-STATE).

### Actor principal

El operador del sistema, tipicamente desde la CLI. La fase de propuesta y la fase de decision pueden ser ejecutadas por la misma persona en sesiones distintas, o (en el futuro) por personas distintas. Este requerimiento **no** modela roles, permisos ni identidad del decisor: cualquier operador con acceso al workdir puede decidir cualquier propuesta pendiente.

### Seleccion del validator activo

La seleccion del validator activo (auto vs humano) **no** es un flag de linea de comando. Es configuracion externa al proceso, leida al arranque de la CLI. El mecanismo propuesto es una variable de entorno `CONTENT_AUDIT_APPROVAL_MODE` con dos valores admitidos:

- `auto` -> el validator del bypass (FEAT-REVBYP R007): auto-aprueba toda propuesta. Flujo monolitico, preserva el comportamiento actual.
- `human` -> el validator humano nuevo: emite `PENDING_APPROVAL` sobre toda propuesta, obligando a una fase de decision posterior.

Solo hay **un** validator activo por invocacion de la CLI. No se componen varios, no se seleccionan por tarea, no se definen perfiles declarativos. El cableado se hace manualmente en el composition root (`Main.java`). Este proyecto **no usa Spring**: no hay anotaciones `@Profile`, `@Conditional` ni mecanismos de inyeccion de contenedor. La decision del valor por defecto queda abierta (ver DOUBT-APPROVAL-MODE-DEFAULT).

### Alcance deliberado

Esta iteracion sigue siendo **prueba de pipeline**, no prueba de logica real:

- El Reviser sigue siendo el bypass (identidad). `elementAfter == elementBefore`. La aprobacion humana se ejerce sobre revisiones identidad; la escritura del curso se ejecuta igual (se preserva DOUBT-BYPASS-WRITE = Opcion B de FEAT-REVBYP).
- El Validator humano no hace inspeccion semantica: simplemente emite `PENDING_APPROVAL` y delega la decision al operador.
- La decision del operador (`approve` / `reject`) tampoco ejecuta logica semantica: aprueba o rechaza por fiat; opcionalmente acepta `--note` / `--reason` como texto libre para dejar traza.

Cualquier flujo de aprobacion multi-etapa (revisores, quorum, delegacion), notificaciones externas (Slack, email) o composicion declarativa de validators (PipelineConfig como recurso) queda explicitamente fuera de esta iteracion.

---

## Reglas de Negocio

### Grupo A - Vocabulario de veredictos y recurso `proposal`

### Rule[F-REVAPR-R001] - Se agrega `PENDING_APPROVAL` al vocabulario de veredictos
**Severity**: critical | **Validation**: AUTO_VALIDATED

El vocabulario del `RevisionValidator` de FEAT-REVBYP (R006) se extiende con un tercer veredicto:

- **APPROVED**: la propuesta puede aplicarse al curso (ya existente).
- **REJECTED**: la propuesta no se aplica (ya existente).
- **PENDING_APPROVAL** (nuevo): la propuesta queda registrada pero el sistema no decide en el momento. La decision se difiere a una fase operativa posterior (`approve proposal <id>` / `reject proposal <id>`).

El artefacto persistido despues de la fase de propuesta (R008 de FEAT-REVBYP) puede llevar cualquiera de los tres veredictos. La cadena persistida + decidida diferida nunca deja el veredicto sin resolver una vez que el operador decide.

**Error**: N/A (esta regla extiende un vocabulario existente)

### Rule[F-REVAPR-R002] - `proposal` es un recurso de primera clase en la CLI
**Severity**: critical | **Validation**: AUTO_VALIDATED

El artefacto de revision persistido bajo `.content-audit/revisions/<planId>/<proposalId>.*` (FEAT-REVBYP R008-R010) se vuelve un recurso direccionable del sistema, siguiendo la gramatica de FEAT-CLIRV (R005/R006):

- Se reconoce como resource name valido en `get` (singular `proposal` / plural `proposals`).
- El id usado para direccionar una propuesta es el `proposalId` que el sistema asigna al crearla (FEAT-REVBYP R001). Ese mismo id es el que aparece en `get proposals` y el que se le pasa a `approve proposal <id>` / `reject proposal <id>`.
- `get proposals` lista todas las propuestas conocidas por el sistema. `get proposal <id>` devuelve una sola. En ausencia, el comando falla con el mismo patron de "not found" de FEAT-CLIRV R001.

**Error**: "No proposal found with id '<id>'"

### Rule[F-REVAPR-R003] - Filtros soportados en `get proposals`
**Severity**: major | **Validation**: AUTO_VALIDATED

`get proposals` acepta, como minimo, los siguientes filtros opcionales (combinables de forma conjuntiva, siguiendo el patron de FEAT-CLIRV R008):

| Flag | Efecto |
|------|--------|
| `--plan <id>` | Restringe el resultado a las propuestas cuyo `planId` coincide con el dado. Si esta ausente, no se aplica filtro por plan. |
| `--status pending\|approved\|rejected` | Restringe por veredicto persistido en el artefacto. `pending` corresponde a `PENDING_APPROVAL`; `approved` a `APPROVED`; `rejected` a `REJECTED`. Parsing case-insensitive. |

Otros filtros (por taskId, por diagnosisKind, por reviserKind, por rango de fechas) quedan como evolucion futura, fuera de alcance en esta iteracion. La decision de ofrecer filtros extra no es bloqueante para la funcionalidad core.

**Error**: "Invalid value for --status: '<value>'. Allowed: pending, approved, rejected"

### Rule[F-REVAPR-R004] - `delete` y `prune` sobre `proposal` quedan fuera de alcance
**Severity**: minor | **Validation**: AUTO_VALIDATED

Esta iteracion **no** agrega `delete proposal <id>` ni `prune proposals --keep N`. La razon es que las propuestas son artefactos de auditoria: eliminarlas borraria la traza de intentos de revision. Si el operador quiere limpiar el historial, hoy lo puede hacer a mano sobre el filesystem; en una iteracion futura se puede considerar ofrecer `prune proposals` con una politica de retencion explicita.

**Error**: (parser default) "Unknown command 'delete proposal'" / "Unknown command 'prune proposals'"

---

### Grupo B - Seleccion del modo de aprobacion

### Rule[F-REVAPR-R005] - El modo de aprobacion se selecciona por variable de entorno
**Severity**: critical | **Validation**: AUTO_VALIDATED

El validator activo se determina al arranque de la CLI leyendo la variable de entorno `CONTENT_AUDIT_APPROVAL_MODE`. Los valores reconocidos son:

- `auto`: el validator del bypass (FEAT-REVBYP R007). Cada propuesta se marca `APPROVED` sincronicamente y se aplica al curso en la misma invocacion de `revise task <id>` (comportamiento de FEAT-REVBYP).
- `human`: el validator humano (nuevo). Cada propuesta se marca `PENDING_APPROVAL`, se persiste, y la decision se toma mas tarde con `approve proposal <id>` / `reject proposal <id>`.

Propiedades requeridas:

1. El modo se resuelve **antes** de construir el `RevisionValidator`. Todas las invocaciones de la misma CLI en un mismo proceso ven el mismo modo.
2. Parsing case-insensitive (`AUTO`, `auto`, `Auto` son equivalentes; idem `human`).
3. Si el valor no es reconocido, la CLI falla al arranque con un mensaje explicito listando los valores admitidos, antes de ejecutar cualquier verbo.
4. No existe un flag de linea de comando que cambie este modo por invocacion. La seleccion es **externa al proceso**, deliberadamente, para evitar que distintos operadores trabajen con modos distintos sin saberlo.
5. El valor por defecto (que se aplica cuando la variable no esta seteada o esta vacia) queda abierto (ver DOUBT-APPROVAL-MODE-DEFAULT).

**Error**: "Invalid value for CONTENT_AUDIT_APPROVAL_MODE: '<value>'. Allowed: auto, human"

### Rule[F-REVAPR-R006] - Hay exactamente un validator activo por invocacion
**Severity**: major | **Validation**: AUTO_VALIDATED

Durante una invocacion de la CLI no se componen ni encadenan varios validators: solo esta activo el seleccionado por R005. Esto descarta explicitamente, en esta iteracion, escenarios como "aplicar el validator humano solo para ciertos `DiagnosisKind` y el auto para el resto", o "una cadena de validators que deben aprobar en secuencia". La composicion declarativa de validators es un feature futuro explicitamente fuera de alcance (ver Limitaciones).

**Error**: N/A (esta regla define una restriccion estructural)

---

### Grupo C - Fase de propuesta en modo humano

### Rule[F-REVAPR-R007] - El validator humano emite `PENDING_APPROVAL`
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el modo activo es `human`, el validator examina cada `RevisionProposal` y emite veredicto `PENDING_APPROVAL`. No aprueba ni rechaza: deja la decision para la fase de aprobacion. Este validator no inspecciona la semantica de la propuesta (no compara `elementBefore` vs `elementAfter`, no valida coherencia): devuelve siempre `PENDING_APPROVAL` para todas las propuestas que recibe. Analogo al validator `bypass` de FEAT-REVBYP R007 pero con otro veredicto.

**Error**: N/A (esta regla define el comportamiento de una implementacion)

### Rule[F-REVAPR-R008] - Artefacto persistido con `PENDING_APPROVAL`
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el validator emite `PENDING_APPROVAL`, el sistema persiste el artefacto de la propuesta bajo `.content-audit/revisions/<planId>/<proposalId>.*` (siguiendo FEAT-REVBYP R008-R010) con el veredicto `PENDING_APPROVAL` registrado. No se toca el curso en disco. El orden del flujo es analogo a FEAT-REVBYP R014: (1) generar propuesta, (2) validar (el validator humano emite PENDING_APPROVAL), (3) persistir el artefacto, (4) **no** aplicar al curso.

El artefacto resultante cumple los requisitos de contenido minimo de FEAT-REVBYP R010 (datos suficientes para reconstruir la decision una vez tomada).

**Error**: N/A (esta regla define un paso del flujo)

### Rule[F-REVAPR-R009] - La tarea queda en estado "esperando aprobacion"
**Severity**: critical | **Validation**: AUTO_VALIDATED

Al finalizar la fase de propuesta en modo humano, la `RefinementTask` asociada queda marcada con un estado que indique "propuesta emitida, esperando decision del operador". El nombre concreto del estado (reusar uno existente del modelo del refiner, agregar uno nuevo como `AWAITING_APPROVAL`, o usar un flag ortogonal) es una decision de arquitectura (ver DOUBT-AWAITING-STATE). La intencion funcional es:

1. La tarea **no** esta DONE (todavia no hubo efecto sobre el curso).
2. La tarea **no** esta PENDING (ya hay una propuesta emitida y persistida; volver a correrle `revise task <id>` no deberia generar una nueva propuesta en silencio).
3. El estado es observable: `get tasks --status ...` debe poder distinguir tareas "esperando aprobacion" de las que estan PENDING o DONE.

**Error**: N/A (esta regla define una transicion de estado)

### Rule[F-REVAPR-R010] - Re-revisar una tarea con propuesta pendiente es un error
**Severity**: major | **Validation**: AUTO_VALIDATED

Si una `RefinementTask` ya tiene una `RevisionProposal` asociada en estado `PENDING_APPROVAL`, una nueva invocacion de `revise task <id>` sobre la misma tarea **no** debe generar en silencio otra propuesta pendiente. El comportamiento por defecto propuesto es **rechazar la invocacion** con un error explicito que identifica la propuesta pendiente y pide al operador que la decida primero (o que, eventualmente en el futuro, la supersea explicitamente con un flag). Se prohibe la acumulacion silenciosa de propuestas pendientes sobre la misma tarea.

La pregunta de si un flag futuro (`--supersede`, `--force`) deberia reemplazar la propuesta pendiente anterior por una nueva queda abierta (ver DOUBT-SUPERSEDE). La decision segura por defecto para esta iteracion es: rechazar.

**Error**: "Task '<task-id>' in plan '<plan-id>' already has a pending proposal '<proposalId>'. Decide it with 'approve proposal <proposalId>' or 'reject proposal <proposalId>' first."

---

### Grupo D - Fase de decision (approve / reject)

### Rule[F-REVAPR-R011] - `approve proposal <id>` aprueba una propuesta pendiente y aplica al curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

El verbo `approve` opera sobre el recurso `proposal`:

```
content-audit approve proposal <proposal-id> [--note "<text>"]
```

Comportamiento:

1. El sistema localiza la propuesta por `proposal-id` bajo `.content-audit/revisions/`.
2. Si no existe, falla con el error de R002 ("No proposal found with id ...").
3. Si existe pero su veredicto actual **no** es `PENDING_APPROVAL`, la invocacion se rechaza con un error que identifica el veredicto actual (ver R013, idempotencia).
4. Si el veredicto es `PENDING_APPROVAL`, el sistema transiciona el artefacto al veredicto `APPROVED` (el mecanismo concreto — reescribir el artefacto en sitio o appendear un registro de decision separado — es una decision de arquitectura; ver DOUBT-ARTIFACT-DECISION-RECORD) y dispara la aplicacion al curso usando la misma mecanica de FEAT-REVBYP R011: cargar el curso via `CourseRepository`, sustituir `elementBefore` por `elementAfter`, persistir el curso modificado.
5. En caso de exito, la `RefinementTask` asociada pasa a DONE (FEAT-REVBYP R013).
6. En caso de falla al aplicar al curso, se sigue el precedente de FEAT-REVBYP R014: el registro de decision queda como `APPROVED + aplicacion intentada + fallo reportado`; la tarea **no** avanza a DONE. La inconsistencia temporal (artefacto aprobado + curso sin actualizar) se acepta como limitacion conocida, heredada de FEAT-REVBYP DOUBT-ATOMICITY.
7. El flag opcional `--note "<text>"` adjunta un texto libre al registro de decision (util para trazabilidad: "aprobado tras revision manual del quiz X"). No es obligatorio. No modifica la semantica de la decision.

**Error**: "No proposal found with id '<id>'" / "Cannot approve proposal '<id>': its current verdict is '<verdict>', not PENDING_APPROVAL" (ver R013) / "Proposal '<id>' was approved and persisted, but the course write failed" (heredado de FEAT-REVBYP R014)

### Rule[F-REVAPR-R012] - `reject proposal <id>` rechaza una propuesta pendiente sin tocar el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

El verbo `reject` opera sobre el recurso `proposal`:

```
content-audit reject proposal <proposal-id> [--reason "<text>"]
```

Comportamiento:

1. El sistema localiza la propuesta por `proposal-id` bajo `.content-audit/revisions/`.
2. Si no existe, falla con el error de R002.
3. Si existe pero su veredicto actual **no** es `PENDING_APPROVAL`, la invocacion se rechaza (ver R013).
4. Si el veredicto es `PENDING_APPROVAL`, el sistema transiciona el artefacto al veredicto `REJECTED`. **No** se carga ni se escribe el curso (analogo a FEAT-REVBYP R012).
5. La `RefinementTask` asociada vuelve al estado que tenia antes de que se emitiera la propuesta (tipicamente PENDING; ver R014).
6. El flag opcional `--reason "<text>"` adjunta un texto libre al registro de decision, documentando por que se rechazo. No es obligatorio en esta iteracion, pero se recomienda al operador por trazabilidad.

**Error**: "No proposal found with id '<id>'" / "Cannot reject proposal '<id>': its current verdict is '<verdict>', not PENDING_APPROVAL"

### Rule[F-REVAPR-R013] - Decidir dos veces la misma propuesta es un error, no un no-op
**Severity**: major | **Validation**: AUTO_VALIDATED

Intentar `approve proposal <id>` o `reject proposal <id>` sobre una propuesta cuyo veredicto persistido **no** es `PENDING_APPROVAL` (ya fue decidida: esta `APPROVED` o `REJECTED`, o incluso `APPROVED + aplicacion fallida`) debe fallar con un error explicito. No es un no-op silencioso, ni se sobreescribe la decision anterior. La razon es proteger la trazabilidad: una propuesta ya decidida ya tuvo (o no) efecto sobre el curso; reescribirla despues genera incoherencia entre el artefacto y la historia real.

Si el operador necesita emitir una decision distinta a la registrada, el camino correcto es **iterar el ciclo**: (1) volver a invocar `revise task <id>` para generar una nueva propuesta sobre la misma tarea (si la politica de re-revision lo permite; ver R010), (2) decidir la nueva propuesta.

**Error**: "Cannot <approve|reject> proposal '<id>': its current verdict is '<verdict>', not PENDING_APPROVAL"

### Rule[F-REVAPR-R014] - Transiciones de estado de la tarea bajo el flujo aprobado
**Severity**: critical | **Validation**: AUTO_VALIDATED

El estado de la `RefinementTask` refleja en todo momento el estado de la revision, segun la siguiente tabla:

| Momento | Estado de la tarea |
|---------|--------------------|
| Antes de `revise task <id>` | El estado previo (tipicamente PENDING) |
| Despues de `revise task <id>` en modo auto + aprobado + aplicado | DONE (heredado de FEAT-REVBYP R013) |
| Despues de `revise task <id>` en modo auto + aprobado + fallo al aplicar | Estado previo (heredado de FEAT-REVBYP R014) |
| Despues de `revise task <id>` en modo human (propuesta PENDING_APPROVAL emitida y persistida) | "Esperando aprobacion" (ver R009 y DOUBT-AWAITING-STATE) |
| Despues de `approve proposal <id>` + aplicado al curso con exito | DONE |
| Despues de `approve proposal <id>` + fallo al aplicar al curso | "Esperando aprobacion" sigue siendo el estado (la propuesta esta APPROVED pero la tarea no avanzo a DONE; heredado de FEAT-REVBYP R014) |
| Despues de `reject proposal <id>` | Estado previo (tipicamente PENDING) |

La regla busca que, para un observador externo que corra `get tasks`, nunca haya una tarea que diga estar DONE sin que el curso refleje el cambio, y nunca haya una tarea que diga estar PENDING cuando existe una propuesta pendiente no decidida. Si el estado previo no era PENDING (la tarea habia sido skipeada, por ejemplo), se preserva.

**Error**: N/A (esta regla define transiciones de estado)

### Rule[F-REVAPR-R015] - `approve` y `reject` solo operan sobre el recurso `proposal`
**Severity**: major | **Validation**: AUTO_VALIDATED

Los verbos `approve` y `reject` son **especificos del recurso `proposal`** en esta iteracion. Invocarlos sobre otro recurso (`approve plan <id>`, `reject audit <id>`, etc.) se rechaza con el mismo patron de "unknown resource" de FEAT-CLIRV R005. La gramatica general sigue siendo kubectl-style (verbo-recurso), pero no todos los verbos aplican a todos los recursos.

Si en iteraciones futuras aparecen otros recursos sujetos a aprobacion (por ejemplo, un `PipelineConfig` como recurso), esta regla se revisa. Por ahora, el scope es unico.

**Error**: "Unknown resource '<name>' for verb 'approve' / 'reject'. Supported: proposal"

---

### Grupo E - Integracion con la CLI y la trazabilidad

### Rule[F-REVAPR-R016] - El id de la propuesta impreso por `revise task <id>` en modo humano se puede copiar a `approve` / `reject`
**Severity**: major | **Validation**: AUTO_VALIDATED

Analogo a FEAT-CLIRV R007 ("resource ids are presented uniformly across verbs"): cuando `revise task <id>` corre en modo humano y emite una propuesta `PENDING_APPROVAL`, la CLI debe imprimir de forma inequivoca el `proposalId` asignado, de modo que el operador lo pueda copiar directamente a la siguiente invocacion `approve proposal <proposalId>` o `reject proposal <proposalId>`. El mismo id tambien se muestra en `get proposals` y `get proposal <id>`.

**Error**: N/A (esta regla define una convencion de presentacion)

### Rule[F-REVAPR-R017] - El workdir override de FEAT-CLIRV R017 aplica a las propuestas
**Severity**: major | **Validation**: AUTO_VALIDATED

Las operaciones `get proposals`, `get proposal <id>`, `approve proposal <id>`, `reject proposal <id>` y la persistencia de propuestas durante `revise task <id>` deben respetar el mismo mecanismo de override del workdir definido en FEAT-CLIRV R017 (`--workdir` flag y `CONTENT_AUDIT_HOME` env var, con precedencia flag > env > default). Todas las propuestas leidas/escritas por una invocacion de la CLI viven bajo el `.content-audit/revisions/` resuelto por ese mecanismo, nunca por dos paths distintos dentro de la misma invocacion.

**Error**: N/A (esta regla incorpora un mecanismo ya definido en otra feature)

---

## User Journeys

### Journey[F-REVAPR-J001] - Flujo humano end-to-end: propuesta -> aprobacion -> aplicado
**Validation**: AUTO_VALIDATED

Happy path del flujo con aprobacion humana: el operador propone, inspecciona el artefacto, aprueba, y el curso queda modificado.

```yaml
journeys:
  - id: F-REVAPR-J001
    name: Flujo humano end-to-end - propuesta a aplicado
    flow:
      - id: configurar_modo_humano
        action: "La CLI arranca con CONTENT_AUDIT_APPROVAL_MODE=human configurada en el entorno; el validator activo es el humano"
        gate: [F-REVAPR-R005, F-REVAPR-R006]
        then: iniciar_revision

      - id: iniciar_revision
        action: "El operador invoca 'content-audit revise task <id>' sobre una tarea PENDING del plan mas reciente"
        then: generar_propuesta

      - id: generar_propuesta
        action: "El Reviser bypass genera una RevisionProposal con elementAfter igual a elementBefore (FEAT-REVBYP R001/R002/R004)"
        then: validar_humano

      - id: validar_humano
        action: "El validator humano emite veredicto PENDING_APPROVAL para la propuesta"
        gate: [F-REVAPR-R001, F-REVAPR-R007]
        then: persistir_artefacto_pendiente

      - id: persistir_artefacto_pendiente
        action: "El sistema persiste el artefacto bajo .content-audit/revisions/<planId>/<proposalId> con veredicto PENDING_APPROVAL; no toca el curso"
        gate: [F-REVAPR-R008, F-REVAPR-R016]
        then: marcar_tarea_esperando

      - id: marcar_tarea_esperando
        action: "La tarea asociada queda marcada como 'esperando aprobacion'; la CLI imprime el proposalId asignado"
        gate: [F-REVAPR-R009, F-REVAPR-R014, F-REVAPR-R016]
        then: operador_inspecciona

      - id: operador_inspecciona
        action: "El operador invoca 'content-audit get proposal <proposalId>' y la CLI devuelve el artefacto completo con veredicto PENDING_APPROVAL"
        gate: [F-REVAPR-R002]
        then: operador_aprueba

      - id: operador_aprueba
        action: "El operador invoca 'content-audit approve proposal <proposalId>'; el sistema verifica que el veredicto actual es PENDING_APPROVAL"
        gate: [F-REVAPR-R011, F-REVAPR-R013]
        then: transicionar_a_aprobado

      - id: transicionar_a_aprobado
        action: "El sistema transiciona el veredicto de la propuesta a APPROVED"
        gate: [F-REVAPR-R011]
        then: aplicar_curso

      - id: aplicar_curso
        action: "El sistema carga el curso via CourseRepository, sustituye el elemento identificado por elementAfter, y persiste el curso modificado (FEAT-REVBYP R011)"
        outcomes:
          - when: "La escritura del curso fue exitosa"
            then: tarea_done
          - when: "La escritura del curso fallo"
            then: fallo_aplicacion

      - id: tarea_done
        action: "El sistema marca la RefinementTask como DONE"
        gate: [F-REVAPR-R014]
        result: success

      - id: fallo_aplicacion
        action: "El sistema reporta que la propuesta fue aprobada pero el curso no pudo reescribirse; la tarea permanece en 'esperando aprobacion'"
        gate: [F-REVAPR-R011, F-REVAPR-R014]
        result: failure
```

### Journey[F-REVAPR-J002] - Flujo humano end-to-end: propuesta -> rechazo -> tarea vuelve a PENDING
**Validation**: AUTO_VALIDATED

Camino de rechazo: el operador genera una propuesta en modo humano, la inspecciona, decide rechazarla, y la tarea regresa al estado previo sin modificar el curso.

```yaml
journeys:
  - id: F-REVAPR-J002
    name: Flujo humano end-to-end - propuesta a rechazo
    flow:
      - id: configurar_modo_humano
        action: "La CLI arranca con CONTENT_AUDIT_APPROVAL_MODE=human configurada en el entorno"
        gate: [F-REVAPR-R005]
        then: iniciar_revision

      - id: iniciar_revision
        action: "El operador invoca 'content-audit revise task <id>' sobre una tarea PENDING"
        then: emitir_pendiente

      - id: emitir_pendiente
        action: "El validator humano emite PENDING_APPROVAL, el sistema persiste el artefacto y la tarea queda 'esperando aprobacion'"
        gate: [F-REVAPR-R007, F-REVAPR-R008, F-REVAPR-R009]
        then: operador_rechaza

      - id: operador_rechaza
        action: "El operador invoca 'content-audit reject proposal <proposalId> --reason \"<text>\"'; el sistema verifica que el veredicto actual es PENDING_APPROVAL"
        gate: [F-REVAPR-R012, F-REVAPR-R013]
        then: transicionar_a_rechazado

      - id: transicionar_a_rechazado
        action: "El sistema transiciona el veredicto a REJECTED; el curso en disco no se toca"
        gate: [F-REVAPR-R012]
        then: tarea_vuelve_pending

      - id: tarea_vuelve_pending
        action: "La RefinementTask vuelve a su estado previo a la propuesta (tipicamente PENDING)"
        gate: [F-REVAPR-R014]
        result: success
```

### Journey[F-REVAPR-J003] - Decidir dos veces la misma propuesta es un error
**Validation**: AUTO_VALIDATED

Cubre la idempotencia estricta de R013: decidir una propuesta ya decidida no es un no-op, es un error.

```yaml
journeys:
  - id: F-REVAPR-J003
    name: Decidir dos veces la misma propuesta es un error
    flow:
      - id: invocar_decision
        action: "El operador invoca 'content-audit approve proposal <proposalId>' o 'content-audit reject proposal <proposalId>' sobre una propuesta"
        gate: [F-REVAPR-R011, F-REVAPR-R012]
        outcomes:
          - when: "El veredicto actual es PENDING_APPROVAL"
            then: procesar_decision
          - when: "El veredicto actual ya es APPROVED o REJECTED"
            then: rechazar_por_veredicto
          - when: "La propuesta no existe"
            then: rechazar_no_encontrada

      - id: procesar_decision
        action: "El sistema procesa la decision (aprobar aplica al curso, rechazar no lo toca) y el artefacto queda con el nuevo veredicto"
        gate: [F-REVAPR-R011, F-REVAPR-R012]
        result: success

      - id: rechazar_por_veredicto
        action: "El sistema reporta que la propuesta ya fue decidida y exit con estado no-cero; el artefacto no se modifica"
        gate: [F-REVAPR-R013]
        result: failure

      - id: rechazar_no_encontrada
        action: "El sistema reporta que la propuesta no existe y exit con estado no-cero"
        gate: [F-REVAPR-R002]
        result: failure
```

### Journey[F-REVAPR-J004] - Re-revisar una tarea con propuesta pendiente es un error
**Validation**: AUTO_VALIDATED

Cubre R010: un segundo `revise task <id>` sobre la misma tarea, cuando ya hay una propuesta pendiente, se rechaza.

```yaml
journeys:
  - id: F-REVAPR-J004
    name: Re-revisar una tarea con propuesta pendiente es un error
    flow:
      - id: invocar_revise
        action: "El operador invoca 'content-audit revise task <taskId>' en modo humano"
        gate: [F-REVAPR-R005]
        outcomes:
          - when: "La tarea no tiene propuesta pendiente"
            then: generar_nueva_propuesta
          - when: "La tarea ya tiene una propuesta en PENDING_APPROVAL"
            then: rechazar_por_pendiente

      - id: generar_nueva_propuesta
        action: "El sistema genera una nueva propuesta, la persiste como PENDING_APPROVAL y marca la tarea como 'esperando aprobacion'"
        gate: [F-REVAPR-R007, F-REVAPR-R008, F-REVAPR-R009]
        result: success

      - id: rechazar_por_pendiente
        action: "El sistema reporta que ya existe una propuesta pendiente para esa tarea y pide al operador que la decida primero; exit con estado no-cero; el artefacto existente no se toca"
        gate: [F-REVAPR-R010]
        result: failure
```

### Journey[F-REVAPR-J005] - Listar propuestas por plan y por estado
**Validation**: AUTO_VALIDATED

Cubre R002/R003: el operador explora las propuestas existentes usando los verbos de lectura y los filtros.

```yaml
journeys:
  - id: F-REVAPR-J005
    name: Listar propuestas por plan y por estado
    flow:
      - id: invocar_get
        action: "El operador invoca 'content-audit get proposals' con cero o mas filtros (--plan <id>, --status pending|approved|rejected)"
        gate: [F-REVAPR-R002, F-REVAPR-R003]
        outcomes:
          - when: "Los filtros son validos (si hay --status, el valor es pending, approved o rejected)"
            then: devolver_lista
          - when: "El valor de --status no es uno de los tres permitidos"
            then: rechazar_status_invalido

      - id: devolver_lista
        action: "El sistema devuelve la lista de propuestas que coinciden con los filtros, con su proposalId, planId, taskId y veredicto actual; si no hay coincidencias, imprime una linea explicita de lista vacia y exit cero (patron de FEAT-CLIRV R011)"
        gate: [F-REVAPR-R002, F-REVAPR-R003]
        result: success

      - id: rechazar_status_invalido
        action: "El sistema reporta que el valor de --status no es valido y lista los permitidos; exit no-cero"
        gate: [F-REVAPR-R003]
        result: failure
```

---

## Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio:

- **Sin workflows multi-etapa de aprobacion**: no hay concepto de "revisor 1 aprueba, revisor 2 aprueba" ni quorum ni delegacion. Una sola decision cierra una propuesta. Cualquier flujo con multiples aprobadores queda fuera de alcance.
- **Sin identidad ni permisos de decisor**: el sistema no modela quien aprueba. Cualquier operador con acceso al workdir puede aprobar o rechazar cualquier propuesta pendiente. El campo `--note` / `--reason` es texto libre, no firma.
- **Sin notificaciones externas**: no se integra con Slack, email, webhooks, ni sistemas de ticketing. El mecanismo de "aviso" que una propuesta esta pendiente es consulta explicita del operador (`get proposals --status pending`).
- **Sin composicion declarativa de validators**: no existe un recurso `PipelineConfig` que permita encadenar `[auto, human, ...]` por tarea, ni seleccionar validator por `DiagnosisKind`. R006 lo prohibe explicitamente. Se deja para un feature futuro que el usuario ya anticipo.
- **Sin Reviser real**: sigue siendo el bypass (identidad) de FEAT-REVBYP. La aprobacion humana se ejerce sobre revisiones identidad. La decision del operador es sobre "aplicar el no-cambio" vs "descartar el no-cambio"; el ejercicio es de pipeline, no de contenido. La aplicacion al curso se ejecuta igual en caso de APPROVED (FEAT-REVBYP DOUBT-BYPASS-WRITE = Opcion B).
- **Sin `delete proposal` ni `prune proposals`**: R004. Las propuestas son artefactos de auditoria; su limpieza no se expone en esta iteracion.
- **Sin resurreccion de propuestas decididas**: R013. Una vez aprobada o rechazada, una propuesta es inmutable. Si se necesita otra decision, se genera una nueva propuesta (ciclando la tarea).
- **Sin supersede automatico**: R010. Un segundo `revise task <id>` con propuesta pendiente se rechaza, no se convierte en un reemplazo silencioso. La pregunta de si conviene ofrecer un flag explicito `--supersede` queda abierta (DOUBT-SUPERSEDE).
- **Sin atomicidad artefacto/curso**: se hereda la limitacion de FEAT-REVBYP R014 / DOUBT-ATOMICITY. Si el curso falla al escribir despues de APPROVED, queda inconsistencia documentada, sin rollback ni retry automatico.

---

## Open Questions

### Doubt[DOUBT-ARTIFACT-DECISION-RECORD] - Como se registra la decision sobre el artefacto?
**Status**: OPEN (para arquitecto)

La fase de decision (approve/reject) transiciona un artefacto de `PENDING_APPROVAL` a `APPROVED` o `REJECTED`. La cuestion es **como** se persiste esa transicion:

- [x] Opcion A: Reescribir en sitio el archivo del artefacto, actualizando el campo `verdict` de `PENDING_APPROVAL` al nuevo valor. Simple, pero pierde la traza de la existencia del estado pendiente previo.
- [ ] Opcion B: Dejar el artefacto original intacto (con `PENDING_APPROVAL`) y appendear un archivo de "decision record" separado bajo el mismo `<proposalId>` (por ejemplo `<proposalId>.decision.json`) con el veredicto final, el actor (si se modelara), el timestamp y el `--note` / `--reason`. Preserva la historia a costa de mas archivos.
- [ ] Opcion C: Un formato tipo append-only log dentro del mismo archivo (lista de eventos: `[created, decided]`). Mas complejo.

**Answer**: Pendiente. La regla funcional (R011/R012/R013) requiere que el veredicto final sea recuperable; la forma del almacenamiento es decision de arquitectura.

### Doubt[DOUBT-APPROVAL-MODE-DEFAULT] - Valor por defecto de `CONTENT_AUDIT_APPROVAL_MODE` cuando la variable no esta seteada
**Status**: RESOLVED

R005 define dos valores admitidos (`auto`, `human`) pero no fija el default cuando la variable no esta seteada o esta vacia. Opciones:

- [x] Opcion A: Default `auto`. Preserva el comportamiento historico de FEAT-REVBYP: scripts, ejemplos y tests existentes siguen funcionando sin setear nada. El modo humano es opt-in explicito. Seguro para no romper expectativas de usuarios actuales.
- [ ] Opcion B: Default `human`. Fuerza a los usuarios a declarar explicitamente si quieren modo auto. Mas seguro desde el punto de vista de "nada se aplica al curso sin revision humana", pero rompe el flujo actual.
- [ ] Opcion C: No hay default — si la variable no esta seteada, la CLI falla al arranque pidiendo explicitamente el valor. Cero ambiguedad, maximo costo operativo.

**Answer**: Opcion A. Cuando `CONTENT_AUDIT_APPROVAL_MODE` no esta seteada o esta vacia, el sistema usa `auto` (validator bypass de FEAT-REVBYP). Esto preserva el comportamiento historico: scripts, tests y flujos automatizados existentes siguen funcionando sin cambios. El modo `human` es opt-in explicito via la variable de entorno.

### Doubt[DOUBT-SUPERSEDE] - Re-revisar una tarea con propuesta pendiente: rechazar o superseder?
**Status**: OPEN

R010 fija el comportamiento por defecto: `revise task <id>` sobre una tarea con propuesta pendiente se rechaza. La pregunta es si convendria ofrecer una forma explicita (por ejemplo `revise task <id> --supersede`) que reemplace la propuesta pendiente anterior por una nueva.

- [x] Opcion A (por defecto en esta iteracion): Solo rechazar. El operador debe decidir (aprobar/rechazar) la propuesta pendiente antes de poder generar otra. Mas seguro, no pierde trazas en silencio.
- [ ] Opcion B: Agregar un flag `--supersede` que marque la propuesta pendiente anterior como `REJECTED --reason "superseded by <new-proposalId>"` y genere la nueva. Util si el operador cambio de opinion sobre el Reviser/contexto y quiere empezar de cero sin pasar por el verbo `reject`.

**Answer**: En esta iteracion se adopta Opcion A. Opcion B queda como mejora futura cuando aparezcan varios Revisers reales y la re-ejecucion sea mas comun.

### Doubt[DOUBT-AWAITING-STATE] - Como se representa "esperando aprobacion" en el modelo de estados de la tarea?
**Status**: OPEN (para arquitecto)

R009/R014 piden que el estado de la tarea sea observable como "esperando aprobacion", distinto de PENDING y de DONE. El modelo actual del refiner tiene `PENDING`, `DONE`, `SKIPPED` (y tal vez otros). Opciones:

- [ ] Opcion A: Agregar un nuevo valor al enum `RefinementTaskStatus` (por ejemplo `AWAITING_APPROVAL`). Claro y explicito; requiere migrar lectores existentes.
- [x] Opcion B: Dejar la tarea en PENDING y representar la existencia de propuesta pendiente con un campo adicional (o derivado de la existencia del artefacto). Minimiza el cambio al enum pero complica la semantica de PENDING.
- [ ] Opcion C: Reusar un estado existente (`IN_PROGRESS` si existe) con un flag auxiliar.

**Answer**: Pendiente. La regla funcional pide distinguirlo observablemente de PENDING y DONE; la representacion concreta es decision del arquitecto alineada con el modelo existente del refiner.

### Doubt[DOUBT-PROPOSAL-LOOKUP] - Busqueda de propuesta por id: plano o por directorio de plan?
**Status**: OPEN (para arquitecto)

Las propuestas se persisten bajo `.content-audit/revisions/<planId>/<proposalId>.*` (FEAT-REVBYP R009). Los verbos nuevos `approve proposal <id>` / `reject proposal <id>` / `get proposal <id>` reciben solo el `proposalId`, no el `planId`. Esto obliga al sistema a buscar el archivo recorriendo todos los subdirectorios de plan.

- [ ] Opcion A: Aceptar el barrido de subdirectorios. Simple; el numero de planes en la practica es acotado.
- [x] Opcion B: Agregar un flag opcional `--plan <id>` a estos verbos para acelerar la busqueda. Util si crece el volumen.
- [ ] Opcion C: Indice auxiliar en `.content-audit/revisions/` (por ejemplo un archivo `index.json`) que mapea `proposalId -> planId`. Mas complejo, requiere mantenimiento.

**Answer**: Pendiente. La regla funcional solo exige que el id sea suficiente (R002, R007 de FEAT-CLIRV); el mecanismo de lookup es decision de arquitectura.

---

## ASSUMPTIONS

1. **El `proposalId` asignado por FEAT-REVBYP R001 es globalmente direccionable.** El formato actual es `<taskId>-<timestamp>` (FEAT-REVBYP R009). `approve proposal <id>` y `reject proposal <id>` asumen que ese id identifica de forma unica a la propuesta dentro del workdir. Si el formato futuro introduce colisiones entre planes, esta asuncion se revisa (ver DOUBT-PROPOSAL-LOOKUP).
2. **El artefacto es suficiente para reconstruir la decision pendiente.** FEAT-REVBYP R010 garantiza contenido minimo; se asume que ese contenido sigue siendo suficiente cuando el veredicto es `PENDING_APPROVAL` — el operador puede leer `get proposal <id>` y entender que se propone sin consultar otras fuentes.
3. **La aplicacion al curso en la fase de aprobacion usa exactamente la misma mecanica de FEAT-REVBYP R011.** No hay diferencia entre "aplicar al aprobar en modo auto" y "aplicar al aprobar en modo humano". El apply-path es el mismo. Esto garantiza que el modo humano no introduce caminos alternativos de escritura del curso.
4. **La tarea previa (PENDING u otra) es recuperable al rechazar.** R014 asume que el sistema puede saber cual era el estado de la tarea antes de la fase de propuesta. Si el modelo actual del refiner no preserva esa informacion (por ejemplo, porque la transicion a "esperando aprobacion" la sobreescribe en el mismo campo), el arquitecto debe decidir como registrarla (ver DOUBT-AWAITING-STATE).
5. **Un solo workdir por invocacion, heredado de FEAT-CLIRV R017.** Todas las operaciones sobre propuestas dentro de una invocacion de CLI usan el mismo `.content-audit/` resuelto al arranque. No hay rutas mezcladas.
