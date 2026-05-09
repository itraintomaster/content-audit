---
feature:
  id: FEAT-PLANEF
  code: F-PLANEF
  name: Generacion de plan efimero sin persistencia en disco
  priority: major
---

# Generacion de plan efimero sin persistencia en disco

## TL;DR

**Que**: El comando `plan` admite un modo de invocacion (`--storage=none` sugerido por el operador, shape final a confirmar) que **no escribe ningun archivo en disco** y emite el plan generado por la salida estandar del CLI en formato JSON, con el mismo schema que un plan persistido. Opcionalmente, en ese mismo modo, cada tarea del plan emitido puede llevar **inline su contexto de correccion** ([F-PLANEF-R002](#F-PLANEF-R002)), evitando que el cliente tenga que pedirlo aparte sobre un plan que no existe en disco.

**Por que**: Mientras el operador del dashboard revisa varias tareas dentro del mismo plan, quiere ver en cada momento como se veria el plan **si las decisiones que ya tomo (aceptadas y pendientes) estuvieran aplicadas**, sin tener que aplicarlas literalmente todavia. Ese plan-proyectado se construye una y otra vez sobre la marcha, no tiene valor historico y no debe ensuciar el historial de planes que el operador conserva como artefactos del proyecto. El contexto de correccion inline cierra el ciclo: el cliente recibe en una sola pasada todo lo que el operador necesita ver para cada tarea proyectada, sin un round-trip adicional contra un plan que ni siquiera tiene id resoluble.

## Reglas de Negocio

<a id="F-PLANEF-R001"></a>
### Rule[F-PLANEF-R001] - Modo de invocacion del comando `plan` que no persiste y emite el plan por stdout
**Severity**: critical | **Validation**: VALIDATED

> El comando `plan` ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) acepta una opcion de invocacion -sugerida por el operador como `--storage=none`, shape exacto a confirmar en [DOUBT-FLAG-SHAPE](#DOUBT-FLAG-SHAPE)- que selecciona el modo "efimero". Cuando esa opcion esta activa, el comando se comporta de la siguiente manera **observable** desde la linea de comando:
>
> 1. **No escribe en disco**: tras la invocacion, ningun archivo bajo los directorios donde `plan` escribe en su modo persistente (en particular `.content-audit/plans/`) cambia de contenido ni de timestamp de modificacion. La invariante se observa comparando el filesystem antes y despues de la invocacion.
> 2. **Emite el plan por stdout en JSON**: la salida estandar del CLI contiene el plan generado en formato JSON, con el **mismo schema** que un plan persistido recuperado por las operaciones de lectura existentes. Mismo conjunto de campos, mismas claves, misma estructura de tareas. Un cliente que ya parsea un plan persistido parsea un plan efimero sin cambios.
> 3. **No interfiere con el resto de los flags ya soportados**: la opcion es ortogonal a los selectores de fuente del comando (en particular `--audit <id>`, [F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)). El plan que se calcula es exactamente el mismo que se calcularia en modo persistente sobre el mismo input; lo unico que cambia es donde va el resultado.

<details><summary>Detail</summary>

1. La regla obliga la equivalencia de **schema**, no la igualdad byte-a-byte: identificadores generados (`planId`, `taskId`, timestamps) pueden diferir entre invocaciones, igual que difieren entre dos invocaciones persistidas del mismo input.
2. Mensajes informativos (banner del CLI, logs de progreso, advertencias no fatales) no deben mezclarse con el JSON en stdout. La regla obliga que la corriente principal sea JSON parseable; el canal exacto al que se redirige el resto (tipicamente `stderr`) y que mensajes se conservan o se suprimen son decisiones de implementacion ([DOUBT-DIAGNOSTIC-OUTPUT](#DOUBT-DIAGNOSTIC-OUTPUT)).
3. La regla aplica al comando `plan` para todos los `DiagnosisKind` que el plan genere. No se restringe a `LEMMA_ABSENCE`: un plan efimero contiene exactamente las mismas tareas que tendria un plan persistido sobre el mismo input.

</details>

<a id="F-PLANEF-R002"></a>
### Rule[F-PLANEF-R002] - El plan efimero puede emitir el contexto de correccion inline por tarea, opt-in
**Severity**: critical | **Validation**: VALIDATED

> En el modo efimero ([F-PLANEF-R001](#F-PLANEF-R001)), el comando `plan` acepta una **opcion adicional opt-in** -sugerida por el operador como `--with-correction-context`, shape exacto a confirmar en [DOUBT-CTX-FLAG-SHAPE](#DOUBT-CTX-FLAG-SHAPE)- que pide que cada tarea del plan emitido lleve **inline** su contexto de correccion. Cuando esa opcion esta activa, el comportamiento observable del CLI es:
>
> 1. **Contexto inline por tarea**: cada tarea del plan emitido por stdout incluye un campo `correctionContext` con la misma estructura y semantica que entrega hoy el comando `get task` / `get tasks` para esa misma tarea sobre un plan persistido ([F-RCLA-R007](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R007), [F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R008)). Un consumidor que ya parsea un `correctionContext` proveniente de `get task` parsea uno emitido por `plan` efimero sin cambios.
> 2. **Equivalencia funcional con `get task`**: para una misma tarea derivada de un mismo `AuditReport`, el `correctionContext` que viaja inline en el plan efimero es funcionalmente equivalente al que `get task` entregaria si ese plan fuera persistido y la tarea consultada por id. "Funcionalmente equivalente" significa misma forma, mismas claves, y mismos valores de campos derivados del analisis (oracion, traduccion, contexto pedagogico, lemas fuera de nivel, lemas sugeridos, etc.). Identificadores generados al vuelo o timestamps pueden diferir, igual que entre dos invocaciones del comando `plan` sobre el mismo input.
> 3. **Opt-in**: cuando la opcion no esta activa, el plan efimero conserva su forma actual ([F-PLANEF-R001](#F-PLANEF-R001)) sin `correctionContext` por tarea. El default no se cambia silenciosamente.
> 4. **Tareas sin contexto construible**: si para una tarea individual el contexto no se puede construir (por ejemplo, falta el diagnostico tipado, [F-RCLA-R006](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R006)), esa tarea aparece en el plan efimero con `correctionContext` ausente o nulo y un campo descriptivo del motivo, con la misma semantica que [F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R008) ya define para `get task`. El comando no falla globalmente: emite el plan completo con las tareas que si tienen contexto, y deja la falla por tarea visible para el cliente.
> 5. **Ortogonal a la fuente del analisis**: la opcion no cambia que `AuditReport` se usa como fuente; sigue siendo el resuelto por los selectores existentes del comando `plan` (en particular `--audit <id>`, [F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)). Si el cliente quiere ver el contexto proyectado, persiste su analisis proyectado y apunta `plan` ahi (igual que en el [Uso esperado](#uso-esperado) de R001).

<details><summary>Detail</summary>

1. La motivacion concreta de R002 es cerrar el gap operativo del cliente: hoy `get task` solo opera sobre planes persistidos (los carga via la persistencia del plan por id), por lo que **un plan efimero no es interrogable** para hidratar contexto. Sin R002, el cliente recibe metadata proyectada de cada tarea pero no tiene como pedir el `correctionContext` proyectado de esas tareas. R002 cierra ese gap moviendo el contexto inline al plan, en una sola pasada.
2. El schema base del plan emitido en modo efimero (sin la opcion de R002) sigue siendo el de [F-PLANEF-R001](#F-PLANEF-R001). R002 agrega un campo `correctionContext` opcional dentro de cada tarea cuando la opcion esta activa; no toca el resto de la salida. Un consumidor que parsea el plan efimero sin la opcion no rompe cuando otro consumidor la activa, porque el campo agregado es opt-in y aditivo.
3. El alcance del `correctionContext` por `DiagnosisKind` es el ya definido en los features de contexto (FEAT-RCSL para SENTENCE_LENGTH, FEAT-RCLA para LEMMA_ABSENCE). R002 no introduce kinds nuevos ni redefine la forma del contexto: reusa la que `get task` ya entrega.
4. La opcion de R002 **solo tiene efecto** en modo efimero. Activarla en modo persistente es ambigua: el comando `plan` persistente hoy entrega un plan **sin** contexto inline (el cliente lo obtiene aparte via `get task`). Que pase exactamente cuando la opcion se invoca con `--storage=disk` (rechazo, no-op silencioso, persistir contexto inline) es decision de arquitectura ([DOUBT-CTX-IN-DISK-MODE](#DOUBT-CTX-IN-DISK-MODE)).
5. La viabilidad estructural ya se verifico para LEMMA_ABSENCE: el contexto se construye a partir del `AuditReport` por tarea con costo dominado por la localizacion del nodo en el arbol. No hay bloqueador estructural de performance que impida construirlo en linea durante la generacion del plan. Para SENTENCE_LENGTH la mecanica es la misma (FEAT-RCSL).

</details>

---

## Contexto

### El problema observado

El operador del dashboard de Learney trabaja sobre un plan de revision con muchas tareas (decenas, a veces mas) que apuntan al mismo curso. Las decisiones que toma sobre las propuestas -aceptar, dejar pendiente, rechazar- son progresivas: revisa una tarea, decide, pasa a la siguiente, y asi durante una sesion larga.

Lo que el operador quiere ver, mientras navega por las tareas restantes, es **el plan tal como se veria si todas las decisiones que ya tomo estuvieran aplicadas**, aunque no las haya aplicado literalmente al curso. El caso concreto donde esto duele: dos tareas consecutivas terminan ofreciendo la misma palabra como sugerencia de reemplazo, porque el plan original -el que se persistio al inicio de la sesion- no sabe que la propuesta anterior, todavia pendiente de aprobacion, ya consumio esa palabra.

Hoy el unico camino disponible es aceptar todo en el momento, o reanalizar y regenerar el plan despues de cada decision. Lo primero rompe el flujo de revision; lo segundo es lento y sobre todo **contamina el historial de planes** del proyecto: cada regeneracion deja un plan persistido que el operador no necesita conservar y termina opacando los planes que si son hitos reales del proyecto.

### Lo que aporta este feature

Una capacidad acotada al comando `plan`: poder invocarlo en un modo donde el plan se emite por la salida estandar y nada se escribe en disco. El cliente (en este caso, el dashboard) consume ese plan transitoriamente y lo descarta. La logica de planificacion es la misma; lo unico que cambia es que el resultado no queda en el historial.

Combinado con capacidades existentes ([Uso esperado](#uso-esperado)), eso le alcanza al cliente para mostrarle al operador, en cada momento, la vista proyectada que necesita.

### Lo que NO aporta este feature

- No modela "consumo de palabras", "sugerencias dinamicas" ni ningun concepto similar como capacidad propia. El efecto sobre las sugerencias se da naturalmente al re-invocar `plan` sobre el analisis adecuado.
- No cambia la logica de planificacion en si (que tareas se generan, en que orden, con que prioridades).
- No redefine la forma del `correctionContext` por `DiagnosisKind`: R002 reusa la estructura ya definida por FEAT-RCSL y FEAT-RCLA. La unica novedad es el canal de entrega (inline en el plan efimero, en lugar de via `get task` sobre un plan persistido).
- No extiende `get task` ni `get tasks` para que operen sobre planes efimeros. Esos comandos siguen requiriendo un plan persistido. La via que F-PLANEF habilita para hidratar contexto sobre un plan efimero es R002 (inline al momento de emitirlo).
- No habilita al cliente a **ejecutar** una revision (`revise`) sobre el plan proyectado. F-PLANEF entrega vista; la habilitacion de `revise` para que opere con un contexto proyectado es responsabilidad de un feature aparte ([FEAT-REVCTX](../2026-05-09.01_revise-correction-context-override/REQUIREMENT.md)).
- No introduce un modo efimero en otros comandos del CLI ([DOUBT-OTHER-COMMANDS](#DOUBT-OTHER-COMMANDS)).

---

## Uso esperado

Esta seccion es informativa: describe como el cliente arma la vista proyectada para el operador combinando capacidades existentes con las capacidades nuevas de F-PLANEF. **No introduce obligaciones sobre content-audit** mas alla de R001 y R002.

1. El cliente le pide a content-audit la vista del curso con las propuestas activas (`APPROVED` + `PENDING_APPROVAL`) aplicadas, usando los mecanismos que [FEAT-CDIFF](../2026-05-06.01_consolidated-differential-view/REQUIREMENT.md) expone.
2. El cliente persiste ese estado proyectado como un analisis temporal en su propio espacio de trabajo (puede ser un workdir efimero, cualquier ubicacion que el cliente controle), de modo que tenga un `auditId` direccionable.
3. El cliente invoca `content-audit plan` apuntando explicitamente a ese analisis con el flag ya existente `--audit <id>` ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) y combinandolo con el modo efimero de R001. Si quiere recibir el contexto inline para mostrar al operador en la UI sin un round-trip adicional, activa la opcion de R002. Recibe el plan proyectado (con o sin `correctionContext` inline segun corresponda) por la salida estandar.
4. El cliente cruza el plan proyectado con el plan vigente del operador **por `nodeId`** (el id del elemento del curso, unico identificador estable entre invocaciones de `plan` -los `planId` y `taskId` se reasignan en cada invocacion-) y reemplaza en la UI los campos derivados del analisis por los del plan proyectado, incluyendo el `correctionContext` por tarea si pidio el inline.

Como side-effect natural del paso 1 + paso 3, los lemas que ya consumieron las propuestas activas dejan de aparecer en las sugerencias de las tareas restantes, y vuelven a aparecer si una propuesta se rechaza. content-audit no necesita modelar eso explicitamente: es lo que `plan` produce cuando opera sobre el analisis correcto.

**Nota sobre el flag `--audit`**: la investigacion sobre los contratos visibles ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014), F-CLIRV-R007 sobre direccionabilidad uniforme de ids, F-CLIRV-R017 sobre el override del workdir via `--workdir` / `CONTENT_AUDIT_HOME`) muestra que el comando `plan` ya admite seleccionar el analisis fuente por id explicito sin tocar la `ActiveAnalysisSelection`. Por lo tanto, F-PLANEF **no necesita** agregar una capacidad para apuntar a un analisis arbitrario: alcanza con que el cliente persista su analisis temporal en un workdir donde `plan` lo pueda direccionar (combinando `--audit <id>` con `--workdir` / `CONTENT_AUDIT_HOME` si hace falta).

**Nota sobre por que el contexto va inline en `plan`, no via `get task`**: hoy `get task` opera contra planes persistidos (los carga por id desde el almacenamiento del plan). Un plan efimero no tiene almacenamiento ni id resoluble entre invocaciones, asi que **`get task` no puede hidratarlo**. En lugar de extender `get task` para que acepte un plan inyectado por stdin (cambio de superficie mayor), R002 elige el camino mas barato: cuando el plan ya se esta calculando para emitir efimero, calcular ademas el contexto y emitirlo en la misma pasada. El cliente recibe todo lo que necesita en una sola invocacion.

---

## Alcance

- **In scope**:
  - El comando `plan` admite una invocacion sin persistencia en disco que emite el plan por stdout en JSON con el mismo schema que un plan persistido ([F-PLANEF-R001](#F-PLANEF-R001)).
  - En ese mismo modo efimero, `plan` admite una opcion opt-in que emite por tarea el `correctionContext` inline, funcionalmente equivalente al que `get task` entrega para esa misma tarea sobre un plan persistido ([F-PLANEF-R002](#F-PLANEF-R002)).
- **Out of scope**:
  - Cualquier responsabilidad sobre el cliente que consume el plan efimero (matcheo por `nodeId`, decision de cuando invocar, reemplazo de campos en UI).
  - Cualquier modificacion a la logica interna de planificacion.
  - Cualquier modificacion a la forma del `correctionContext` por `DiagnosisKind` (FEAT-RCSL / FEAT-RCLA siguen siendo la fuente).
  - Habilitar a `get task` / `get tasks` para que operen sobre planes efimeros.
  - Habilitar a `revise` para que ejecute una revision sobre un contexto proyectado (cubierto por [FEAT-REVCTX](../2026-05-09.01_revise-correction-context-override/REQUIREMENT.md)).
  - Modos efimeros para otros comandos (`analyze`, `revise`, etc.).

---

## User Journeys

### Journey[F-PLANEF-J001] - Invocar `plan` en modo efimero entrega el plan por stdout y no escribe archivos
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-PLANEF-J001
    name: Invocar plan en modo efimero entrega el plan por stdout y no escribe archivos
    flow:
      - id: invocar_plan_efimero
        action: "El operador (o un cliente automatizado) invoca el comando plan con la opcion que selecciona el modo efimero, contra una auditoria valida"
        gate: [F-PLANEF-R001]
        outcomes:
          - when: "La planificacion se completa con exito"
            then: emitir_json
          - when: "La planificacion falla (auditoria fuente no encontrada u otra falla del comando)"
            then: reportar_error

      - id: emitir_json
        action: "El sistema emite el plan generado por la salida estandar en formato JSON, con el mismo schema que un plan persistido"
        gate: [F-PLANEF-R001]
        then: verificar_filesystem_intacto

      - id: verificar_filesystem_intacto
        action: "Tras la invocacion, ningun archivo bajo los directorios donde plan escribe en su modo persistente cambio de contenido ni de timestamp"
        gate: [F-PLANEF-R001]
        result: success

      - id: reportar_error
        action: "El sistema reporta el error por los canales habituales del CLI; ningun archivo bajo los directorios donde plan escribe en su modo persistente cambio de contenido ni de timestamp"
        gate: [F-PLANEF-R001]
        result: failure
```

### Journey[F-PLANEF-J002] - Invocar `plan` efimero con la opcion de contexto inline emite el plan con `correctionContext` por tarea
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-PLANEF-J002
    name: Invocar plan efimero con la opcion de contexto inline emite el plan con correctionContext por tarea
    flow:
      - id: invocar_plan_efimero_con_contexto
        action: "El operador (o un cliente automatizado) invoca el comando plan en modo efimero y con la opcion que pide contexto inline, contra una auditoria valida"
        gate: [F-PLANEF-R001, F-PLANEF-R002]
        outcomes:
          - when: "La planificacion se completa con exito y todas las tareas tienen contexto construible"
            then: emitir_plan_con_contexto_completo
          - when: "La planificacion se completa pero al menos una tarea no tiene contexto construible (por ejemplo, falta el diagnostico tipado)"
            then: emitir_plan_con_contexto_parcial
          - when: "La planificacion falla (auditoria fuente no encontrada u otra falla del comando)"
            then: reportar_error_con_contexto

      - id: emitir_plan_con_contexto_completo
        action: "El sistema emite el plan por stdout en JSON; cada tarea lleva un campo correctionContext con la misma estructura que get task entregaria sobre un plan persistido equivalente"
        gate: [F-PLANEF-R002]
        then: verificar_filesystem_intacto_con_contexto

      - id: emitir_plan_con_contexto_parcial
        action: "El sistema emite el plan por stdout en JSON; las tareas con contexto construible llevan correctionContext y las que no lo tienen llevan el campo nulo o ausente con el motivo descriptivo, con la misma semantica que get task ya define"
        gate: [F-PLANEF-R002]
        then: verificar_filesystem_intacto_con_contexto

      - id: verificar_filesystem_intacto_con_contexto
        action: "Tras la invocacion, ningun archivo bajo los directorios donde plan escribe en su modo persistente cambio de contenido ni de timestamp"
        gate: [F-PLANEF-R001]
        result: success

      - id: reportar_error_con_contexto
        action: "El sistema reporta el error por los canales habituales del CLI; ningun archivo bajo los directorios donde plan escribe en su modo persistente cambio de contenido ni de timestamp; no se emite plan parcial por stdout"
        gate: [F-PLANEF-R001]
        result: failure
```

### Journey[F-PLANEF-J003] - Invocar `plan` efimero **sin** la opcion de contexto preserva la salida actual
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-PLANEF-J003
    name: Invocar plan efimero sin la opcion de contexto preserva la salida actual
    flow:
      - id: invocar_plan_efimero_sin_contexto
        action: "El operador invoca el comando plan en modo efimero sin activar la opcion que pide contexto inline"
        gate: [F-PLANEF-R001, F-PLANEF-R002]
        then: emitir_plan_sin_contexto

      - id: emitir_plan_sin_contexto
        action: "El sistema emite el plan por stdout en JSON con el schema base de R001; ninguna tarea lleva correctionContext inline"
        gate: [F-PLANEF-R002]
        result: success
```

---

## Open Questions

<a id="DOUBT-FLAG-SHAPE"></a>
### Doubt[DOUBT-FLAG-SHAPE] - Shape concreto del flag para activar el modo efimero
**Status**: OPEN (para arquitecto, con preferencia del operador)

El operador sugirio `--storage=none`. Otras opciones razonables:

- [x] Opcion A: `--storage=none`. Sugerida por el operador. Habilita en el futuro otros valores (`--storage=memory`, `--storage=tmp`) si el sistema crece a admitirlos.
- [ ] Opcion B: `--no-persist` o `--ephemeral`. Booleano simple, mas tradicional en CLIs.
- [ ] Opcion C: Variable de entorno (`CONTENT_AUDIT_PLAN_STORAGE=none`). Consistente con el patron de `CONTENT_AUDIT_APPROVAL_MODE` de F-REVAPR, pero menos explicito en la linea de comando.

**Answer**: Pendiente. R001 obliga la existencia del modo, no el shape concreto del flag.

<a id="DOUBT-DIAGNOSTIC-OUTPUT"></a>
### Doubt[DOUBT-DIAGNOSTIC-OUTPUT] - A donde van los mensajes auxiliares en modo efimero?
**Status**: OPEN (para arquitecto)

R001 obliga que la salida estandar contenga JSON parseable. Cualquier mensaje informativo (banner del CLI, log de progreso, advertencia no fatal) debe ir por otro canal (tipicamente `stderr`) o suprimirse en este modo. Que canal se usa exactamente y que mensajes se conservan o se suprimen es decision de implementacion.

**Answer**: Pendiente.

<a id="DOUBT-OTHER-COMMANDS"></a>
### Doubt[DOUBT-OTHER-COMMANDS] - El modo efimero deberia existir tambien para `analyze` u otros comandos?
**Status**: OPEN (para confirmar con el operador)

El caso de uso del dashboard solo requiere el modo efimero en `plan`, porque la simulacion del analisis ya esta cubierta por F-CDIFF y por `--audit` + `--workdir`. Pero hay otros verbos que tambien escriben artefactos (`analyze`, `revise`) y podrian beneficiarse del mismo patron en el futuro.

- [ ] Opcion A: Solo `plan` lo necesita por ahora; cualquier extension futura es feature aparte.
- [ ] Opcion B: Disenar el flag de forma uniforme para que cualquier comando que escriba artefactos pueda adoptarlo (sin implementarlo todavia para los demas).

**Answer**: Pendiente.

<a id="DOUBT-CTX-FLAG-SHAPE"></a>
### Doubt[DOUBT-CTX-FLAG-SHAPE] - Shape concreto de la opcion para activar contexto inline
**Status**: OPEN (para arquitecto, con preferencia del operador)

El operador sugirio `--with-correction-context`. Otras opciones razonables:

- [x] Opcion A: `--with-correction-context`. Booleano simple, descriptivo. Sugerida por el operador.
- [ ] Opcion B: Reutilizar la familia `--storage`: `--storage=none --include=correction-context` (o similar). Mas extensible para futuros campos opt-in (`--include=...,...`).
- [ ] Opcion C: Variable de entorno (`CONTENT_AUDIT_PLAN_INCLUDE_CONTEXT=true`). Consistente con [DOUBT-FLAG-SHAPE](#DOUBT-FLAG-SHAPE) opcion C, pero menos explicito en la linea de comando.

**Answer**: Pendiente. R002 obliga la existencia del modo opt-in y la equivalencia funcional con `get task`, no el shape concreto.

<a id="DOUBT-CTX-IN-DISK-MODE"></a>
### Doubt[DOUBT-CTX-IN-DISK-MODE] - Que pasa si la opcion de contexto inline se activa en modo persistente?
**Status**: OPEN (para arquitecto)

R002 acota su efecto al modo efimero. Si el operador (o el cliente) activa la opcion combinada con `--storage=disk` (o el equivalente del modo persistente), las opciones razonables son:

- [ ] Opcion A: Rechazar con error claro ("la opcion --with-correction-context solo aplica al modo efimero"). Conservador.
- [ ] Opcion B: No-op silencioso: el plan se persiste sin contexto inline (comportamiento actual del modo persistente) y la opcion no tiene efecto. Simple pero confuso.
- [ ] Opcion C: Persistir el contexto inline tambien en modo disk. Cambia el schema del plan persistido y obliga a una migracion. Probablemente fuera de alcance de este feature.

**Answer**: Pendiente. R002 #4 lo declara como decision de arquitectura. La preferencia provisoria es la Opcion A (rechazo explicito) para no introducir comportamientos silenciosos.

<a id="DOUBT-CTX-DEFAULT"></a>
### Doubt[DOUBT-CTX-DEFAULT] - El contexto inline deberia ser default en modo efimero?
**Status**: OPEN (para confirmar con el operador)

R002 lo modela como **opt-in** para no romper el contrato actual del plan efimero ni inflar el JSON cuando el cliente solo quiere metadata. Pero si el caso de uso real es siempre "dashboard que muestra contexto", el opt-out (default ON, opt-out con un flag) podria ser mas conveniente.

- [x] Opcion A: Default OFF (opt-in). Conserva la salida actual de R001 y deja al cliente decidir.
- [ ] Opcion B: Default ON en modo efimero (opt-out). Asume que cualquier cliente que invoca efimero quiere contexto.

**Answer**: Pendiente. La preferencia provisoria es la Opcion A; el operador puede confirmar si el cliente real siempre lo activa.

---

## References

- **FEAT-CLIRV** ([requirement](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md)) - Define el comando `plan` como verbo de primera clase del CLI ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) y el flag `--audit <id>` para apuntar a un analisis especifico, asi como el override de workdir via `--workdir` / `CONTENT_AUDIT_HOME` (F-CLIRV-R017). F-PLANEF agrega modos de invocacion sobre ese mismo comando, sin alterar la logica de planificacion ni los selectores de fuente. Citado por [F-PLANEF-R001](#F-PLANEF-R001) y [F-PLANEF-R002](#F-PLANEF-R002).
- **FEAT-CDIFF** ([requirement](../2026-05-06.01_consolidated-differential-view/REQUIREMENT.md)) - Provee la vista del curso con propuestas activas aplicadas en memoria. Es el insumo natural del cliente que despues invoca `plan` en modo efimero, pero F-PLANEF no depende de F-CDIFF: el modo efimero funciona contra cualquier auditoria valida que el comando `plan` ya admita.
- **FEAT-RCLA** ([requirement](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)) - Define la forma del `correctionContext` para tareas `LEMMA_ABSENCE`, incluyendo `suggestedLemmas`, `misplacedLemmas` y el contexto pedagogico ([F-RCLA-R003](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md), [F-RCLA-R007](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md), [F-RCLA-R008](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)). F-PLANEF reusa esa estructura sin modificarla; lo unico nuevo es el canal de entrega (inline en el plan efimero, [F-PLANEF-R002](#F-PLANEF-R002)).
- **FEAT-RCSL** - Define la forma del `correctionContext` para tareas `SENTENCE_LENGTH`. Misma relacion que con FEAT-RCLA: F-PLANEF reusa la estructura, no la redefine. Citado por [F-PLANEF-R002](#F-PLANEF-R002) implicitamente al hablar de "todos los `DiagnosisKind` que el plan genere".
- **FEAT-REVCTX** ([requirement](../2026-05-09.01_revise-correction-context-override/REQUIREMENT.md)) - Habilita al verbo `revise` a ejecutar la fase de revision con un `correctionContext` provisto externamente por el cliente, cerrando el ciclo del operador que aprueba una tarea sobre el contexto proyectado que F-PLANEF le entrega. F-PLANEF y FEAT-REVCTX son complementarios: F-PLANEF entrega vista; FEAT-REVCTX recibe la decision para ejecutar.
