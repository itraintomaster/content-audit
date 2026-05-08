---
feature:
  id: FEAT-PLANEF
  code: F-PLANEF
  name: Generacion de plan efimero sin persistencia en disco
  priority: major
---

# Generacion de plan efimero sin persistencia en disco

## TL;DR

**Que**: El comando `plan` admite un modo de invocacion (`--storage=none` sugerido por el operador, shape final a confirmar) que **no escribe ningun archivo en disco** y emite el plan generado por la salida estandar del CLI en formato JSON, con el mismo schema que un plan persistido.

**Por que**: Mientras el operador del dashboard revisa varias tareas dentro del mismo plan, quiere ver en cada momento como se veria el plan **si las decisiones que ya tomo (aceptadas y pendientes) estuvieran aplicadas**, sin tener que aplicarlas literalmente todavia. Ese plan-proyectado se construye una y otra vez sobre la marcha, no tiene valor historico y no debe ensuciar el historial de planes que el operador conserva como artefactos del proyecto.

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
- No introduce un modo efimero en otros comandos del CLI ([DOUBT-OTHER-COMMANDS](#DOUBT-OTHER-COMMANDS)).

---

## Uso esperado

Esta seccion es informativa: describe como el cliente arma la vista proyectada para el operador combinando capacidades existentes con la capacidad nueva de F-PLANEF. **No introduce obligaciones sobre content-audit** mas alla de R001.

1. El cliente le pide a content-audit la vista del curso con las propuestas activas (`APPROVED` + `PENDING_APPROVAL`) aplicadas, usando los mecanismos que [FEAT-CDIFF](../2026-05-06.01_consolidated-differential-view/REQUIREMENT.md) expone.
2. El cliente persiste ese estado proyectado como un analisis temporal en su propio espacio de trabajo (puede ser un workdir efimero, cualquier ubicacion que el cliente controle), de modo que tenga un `auditId` direccionable.
3. El cliente invoca `content-audit plan` apuntando explicitamente a ese analisis con el flag ya existente `--audit <id>` ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) y combinandolo con el modo efimero de R001. Recibe el plan proyectado por la salida estandar.
4. El cliente cruza el plan proyectado con el plan vigente del operador **por `nodeId`** (el id del elemento del curso, unico identificador estable entre invocaciones de `plan` -los `planId` y `taskId` se reasignan en cada invocacion-) y reemplaza en la UI los campos derivados del analisis por los del plan proyectado.

Como side-effect natural del paso 1 + paso 3, los lemas que ya consumieron las propuestas activas dejan de aparecer en las sugerencias de las tareas restantes, y vuelven a aparecer si una propuesta se rechaza. content-audit no necesita modelar eso explicitamente: es lo que `plan` produce cuando opera sobre el analisis correcto.

**Nota sobre el flag `--audit`**: la investigacion sobre los contratos visibles ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014), F-CLIRV-R007 sobre direccionabilidad uniforme de ids, F-CLIRV-R017 sobre el override del workdir via `--workdir` / `CONTENT_AUDIT_HOME`) muestra que el comando `plan` ya admite seleccionar el analisis fuente por id explicito sin tocar la `ActiveAnalysisSelection`. Por lo tanto, F-PLANEF **no necesita** agregar una capacidad para apuntar a un analisis arbitrario: alcanza con que el cliente persista su analisis temporal en un workdir donde `plan` lo pueda direccionar (combinando `--audit <id>` con `--workdir` / `CONTENT_AUDIT_HOME` si hace falta).

---

## Alcance

- **In scope**:
  - El comando `plan` admite una invocacion sin persistencia en disco que emite el plan por stdout en JSON con el mismo schema que un plan persistido.
- **Out of scope**:
  - Cualquier responsabilidad sobre el cliente que consume el plan efimero (matcheo por `nodeId`, decision de cuando invocar, reemplazo de campos en UI).
  - Cualquier modificacion a la logica interna de planificacion.
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

---

## References

- **FEAT-CLIRV** ([requirement](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md)) - Define el comando `plan` como verbo de primera clase del CLI ([F-CLIRV-R014](../2026-04-19.01_cli-resource-verb-restructure/REQUIREMENT.md#F-CLIRV-R014)) y el flag `--audit <id>` para apuntar a un analisis especifico, asi como el override de workdir via `--workdir` / `CONTENT_AUDIT_HOME` (F-CLIRV-R017). F-PLANEF agrega un modo de invocacion sobre ese mismo comando, sin alterar la logica de planificacion ni los selectores de fuente.
- **FEAT-CDIFF** ([requirement](../2026-05-06.01_consolidated-differential-view/REQUIREMENT.md)) - Provee la vista del curso con propuestas activas aplicadas en memoria. Es el insumo natural del cliente que despues invoca `plan` en modo efimero, pero F-PLANEF no depende de F-CDIFF: el modo efimero funciona contra cualquier auditoria valida que el comando `plan` ya admita.
- **FEAT-RCLA** ([requirement](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md)) - Define los `suggestedLemmas` que cada tarea `LEMMA_ABSENCE` lleva en su `correctionContext`. F-PLANEF no modifica esa estructura; al invocar `plan` sobre el analisis adecuado, F-RCLA produce su lista basal sobre ese analisis y la diferencia con el plan vigente es el efecto util que el cliente busca.
