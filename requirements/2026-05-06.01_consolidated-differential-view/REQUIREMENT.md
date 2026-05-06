---
feature:
  id: FEAT-CDIFF
  code: F-CDIFF
  name: Estructura consolidada de un analisis con sus propuestas aceptadas y pendientes
  priority: critical
---

# Estructura consolidada de un analisis con sus propuestas aceptadas y pendientes

## TL;DR

**Que**: El CLI de content-audit expone, para cada `AuditReport`, una **estructura consolidada** que combina el baseline del analisis con los efectos de las propuestas aceptadas y pendientes de su plan activo. La salida agrega, sobre cada nodo del arbol y sobre cada estadistica afectada, los campos `consolidated`, `acceptedDelta`, `pendingProjection` y `pendingDelta` con semantica precisa, de modo que cualquier consumidor pueda reconstruir el efecto de las decisiones del operador sin recomputar la auditoria.

**Por que**: Hoy, para reflejar el efecto de aceptar una propuesta hay que correr un nuevo analisis. Esto no escala (potencialmente miles de analisis por curso) y obliga a cada consumidor a recomputar el cruce baseline/decisiones por su cuenta. Esta feature define el contrato de datos canonico que content-audit emite para que un consumidor cualquiera (la UI actual o cualquier otra) lo lea directo.

## Reglas de Negocio

### Grupo A - Identificacion del par activo

<a id="F-CDIFF-R001"></a>
### Rule[F-CDIFF-R001] - El sistema expone un par activo `(auditId, planId)` consultable
**Severity**: critical | **Validation**: VALIDATED

> El sistema mantiene un par activo `(auditId, planId)` por curso y lo expone en la salida del CLI. Cualquier consumidor puede consultar cual es el activo en una sola operacion, sin tener que inferirlo a partir de timestamps o del orden de creacion de los analisis.

<details><summary>Detail</summary>

1. La estructura consolidada se construye en funcion del par activo (Grupo C en adelante). Sin par activo, no hay consolidado.
2. La salida CLI que entrega la estructura consolidada incluye, en su raiz, los campos `activeAuditId` y `activePlanId` con los identificadores resueltos al momento de servir la peticion.
3. Como se persiste o resuelve el par activo (archivo dedicado, parametro de comando, etc.) es decision de arquitectura ([DOUBT-ACTIVE-PERSISTENCE](#DOUBT-ACTIVE-PERSISTENCE)).
4. Si no hay par activo resoluble, la salida sigue las reglas del Grupo G (no-disponibilidad).

</details>

<a id="F-CDIFF-R002"></a>
### Rule[F-CDIFF-R002] - Cambiar el par activo es idempotente y no destructivo
**Severity**: major | **Validation**: VALIDATED

> La operacion del CLI que cambia el par activo no modifica ningun `AuditReport`, ningun plan, ningun artefacto de propuesta y ningun archivo del curso. Es una seleccion de cual estructura consolidada el CLI debe servir cuando se la pidan; nada mas. Apuntar al activo actual es un no-op.

<details><summary>Detail</summary>

1. La regla habilita que el operador (o un consumidor automatizado) navegue libremente entre analisis -incluido un analisis viejo con sus pendientes dormidas- sin riesgo de alterar el estado del sistema.
2. La forma concreta del verbo CLI (nombre, sintaxis, flags) y de la persistencia es decision de arquitectura. Lo que esta regla obliga es la idempotencia y la no-destructividad observables.

</details>

---

### Grupo B - Apilado y vigencia de propuestas

<a id="F-CDIFF-R003"></a>
### Rule[F-CDIFF-R003] - Un nuevo `AuditReport` corre contra el estado del curso real (con aceptadas ya materializadas)
**Severity**: critical | **Validation**: VALIDATED

> Cuando el CLI genera un `AuditReport` nuevo, su baseline se computa sobre el curso tal como esta en disco al momento de correr el analisis. Las propuestas aceptadas en analisis anteriores ya estan aplicadas al curso por [F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011), por lo que el nuevo baseline las refleja como contenido normal del curso. La estructura consolidada de ese analisis recien creado, en consecuencia, tiene `acceptedDelta` y `pendingDelta` ausentes (no hay decisiones propias todavia) y su `consolidated` es igual al baseline.

<details><summary>Detail</summary>

1. La regla establece que **el curso es la fuente de verdad** entre analisis. El historial de aceptadas no se "replica" sobre el nuevo analisis: ya quedo materializado en el curso y reaparece como parte del baseline.
2. Esta regla habilita la **equivalencia post-stack** que cubre [F-CDIFF-R014](#F-CDIFF-R014).
3. La regla no obliga a que el nuevo `AuditReport` herede deltas del anterior. La estructura consolidada de cada analisis se construye exclusivamente con su propio baseline y su propio plan activo.

</details>

<a id="F-CDIFF-R004"></a>
### Rule[F-CDIFF-R004] - Las propuestas pendientes del plan anterior quedan dormidas, ligadas a su `auditId` original
**Severity**: critical | **Validation**: VALIDATED

> Cuando se crea un nuevo `AuditReport`, las propuestas con veredicto `PENDING_APPROVAL` que pertenezcan a planes de analisis anteriores **no se borran ni se migran**: siguen asociadas a su `auditId` y `planId` originales y siguen siendo visibles si el consumidor pide la estructura consolidada de aquel analisis (cambiando el par activo). El plan del analisis nuevo no las contiene.

<details><summary>Detail</summary>

1. La pendiente conserva su `proposalId`, su `planId`, su `sourceAuditId` ([F-REVBYP-R001](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md)) y su veredicto. Es informacion historica recuperable.
2. La estructura consolidada de un analisis solo combina el baseline de **ese** analisis con las propuestas de **su** plan activo ([F-CDIFF-R017](#F-CDIFF-R017)). No se mezclan planes de analisis distintos.

</details>

<a id="F-CDIFF-R005"></a>
### Rule[F-CDIFF-R005] - Las propuestas rechazadas no aparecen en la estructura consolidada
**Severity**: major | **Validation**: VALIDATED

> Una propuesta con veredicto `REJECTED` ([F-REVAPR-R012](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R012)) no contribuye a `consolidated`, `acceptedDelta`, `pendingProjection` ni `pendingDelta` de ningun nodo o estadistica. Para todos los efectos del contrato, es como si esa propuesta no existiera.

<details><summary>Detail</summary>

1. La trazabilidad del rechazo (quien rechazo, cuando, por que) se mantiene en el artefacto de la propuesta segun [F-REVAPR-R013](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R013), pero **no es visible en la estructura consolidada**: este contrato describe el estado vigente del curso, no el historial de decisiones.

</details>

---

### Grupo C - Contrato de salida a nivel hoja

<a id="F-CDIFF-R006"></a>
### Rule[F-CDIFF-R006] - Cada nodo hoja afectado se emite con `consolidated` y, opcionalmente, `pendingProjection`
**Severity**: critical | **Validation**: VALIDATED

> Para cada nodo hoja del curso (tipicamente un quiz) que esta tocado por al menos una propuesta aceptada o pendiente del plan activo, la estructura consolidada incluye:
>
> | Campo | Presencia | Significado |
> |-------|-----------|-------------|
> | `consolidated` | siempre | Snapshot del nodo en su estado vigente: si una aceptada toco este nodo, es el `elementAfter` de la aceptada; si no, es el snapshot original del baseline. |
> | `pendingProjection` | solo si hay pendiente | Snapshot del nodo si la propuesta pendiente del plan activo se aceptara, computado sobre el `consolidated`. |
>
> La salida nunca incluye un tercer snapshot que represente el baseline original "previo a la aceptada": si hay aceptada, el original ya quedo desplazado por ella y no se reexpone.

<details><summary>Detail</summary>

1. Casos cubiertos por la combinacion de `consolidated` + `pendingProjection`:

   | Caso | `consolidated` | `pendingProjection` |
   |------|----------------|---------------------|
   | Sin aceptada, sin pendiente | snapshot del baseline | ausente |
   | Sin aceptada, con pendiente | snapshot del baseline | snapshot con la pendiente aplicada |
   | Con aceptada, sin pendiente | snapshot de la aceptada | ausente |
   | Con aceptada y pendiente sobre el mismo nodo | snapshot de la aceptada | snapshot con la pendiente aplicada sobre la aceptada |

2. La pendiente del cuarto caso se compara contra el `consolidated` (que ya tiene la aceptada aplicada), no contra el snapshot original del baseline. Es la unica decision coherente con que el curso real ya tiene la aceptada materializada.
3. La granularidad de "nodo afectado" la define el patron de propuesta vigente: si una propuesta sustituye un `elementBefore` por un `elementAfter` sobre un `nodeId` ([F-REVBYP-R001](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md)), el nodo hoja afectado es el identificado por ese `nodeId` y el campo emitido es el snapshot completo del nodo. La distincion campo-a-campo dentro del snapshot queda fuera de alcance ([DOUBT-FIELD-IDENTITY](#DOUBT-FIELD-IDENTITY)).

</details>

<a id="F-CDIFF-R007"></a>
### Rule[F-CDIFF-R007] - Las referencias a las propuestas que produjeron `consolidated` y `pendingProjection` estan en la salida
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando la estructura consolidada de un nodo emite `consolidated` o `pendingProjection`, debe incluir tambien la lista de `proposalId` cuyas decisiones produjeron cada uno de esos snapshots. Esto permite al consumidor enlazar la salida consolidada con los artefactos individuales de propuesta (FEAT-REVAPR R002).

<details><summary>Detail</summary>

1. `consolidated` lleva un campo `acceptedProposalIds` con la lista de `proposalId` aceptados que afectaron al nodo (ordenados por `createdAt` ascendente, para que la cadena de aplicacion sea reconstruible). Si no hay aceptadas sobre el nodo, la lista esta vacia o el campo se omite -decision de arquitectura, ambas alternativas cumplen el contrato siempre que el comportamiento sea uniforme-.
2. `pendingProjection`, si esta presente, lleva un campo `pendingProposalId` con el `proposalId` de la pendiente que produjo la proyeccion.
3. La regla hace explicita la **trazabilidad** que evita que el consumidor tenga que recompilar el cruce de propuestas por su cuenta.

</details>

---

### Grupo D - Contrato de salida a nivel padre (agregaciones)

<a id="F-CDIFF-R008"></a>
### Rule[F-CDIFF-R008] - Cada nodo padre afectado se emite con `consolidated` y, opcionalmente, `pendingProjection`
**Severity**: critical | **Validation**: VALIDATED

> Para cada nodo padre (knowledge, topic, milestone, course) cuyo subarbol contiene al menos un nodo hoja afectado, la estructura consolidada emite:
>
> | Campo | Presencia | Significado |
> |-------|-----------|-------------|
> | `consolidated` | siempre que el padre tenga aceptadas o pendientes en su subarbol | Valor agregado del padre re-computado sobre el subarbol con las aceptadas aplicadas e ignorando las pendientes. |
> | `pendingProjection` | solo si el subarbol tiene al menos una pendiente aplicable | Valor agregado del padre re-computado sobre el subarbol con aceptadas + pendientes aplicadas. |
>
> Si un padre solo tiene pendientes en su subarbol (sin aceptadas), `consolidated` coincide numericamente con el baseline del analisis para ese padre y se sigue emitiendo, para que el consumidor pueda comparar consolidated vs proyeccion sin pedir el baseline aparte.

<details><summary>Detail</summary>

1. Si el subarbol no tiene aceptadas ni pendientes, el padre **no aparece** como afectado: el consumidor que lo necesite lo lee del baseline directamente.
2. La re-agregacion se hace respetando la **estrategia de agregacion** declarada por el analizador correspondiente (promedio simple para sentence-length, acumulacion de conteos para coca-buckets, etc., conforme a `F-COCA-R029` de FEAT-COCA). En particular, `pendingProjection(padre)` no es la suma ni el promedio de `pendingProjection` de los hijos: se obtiene re-agregando el subarbol con las pendientes aplicadas.
3. Esta regla se traduce, en la salida JSON del CLI, en que el padre lleva los mismos dos campos (`consolidated`, `pendingProjection`) que cualquier nodo afectado, mas los campos del Grupo E si tiene estadisticas afectadas.

</details>

---

### Grupo E - Estadisticas afectadas

<a id="F-CDIFF-R009"></a>
### Rule[F-CDIFF-R009] - Cada estadistica afectada se emite con cuatro campos: `original`, `consolidated`, `acceptedDelta`, `pendingDelta`
**Severity**: critical | **Validation**: VALIDATED

> Para cada estadistica del `AuditReport` que se vea tocada por al menos una propuesta aceptada o pendiente del plan activo, la salida del CLI emite cuatro campos numericos:
>
> | Campo | Presencia | Definicion |
> |-------|-----------|------------|
> | `original` | siempre | Valor de la estadistica en el baseline del analisis (lo que el motor reporto cuando se computo el `AuditReport`). |
> | `consolidated` | siempre | Valor con las aceptadas aplicadas, recomputado con la misma estrategia de agregacion. |
> | `acceptedDelta` | siempre | Diferencia `consolidated - original`, en puntos absolutos sobre la escala de la estadistica. |
> | `pendingDelta` | solo si hay pendientes que tocan la estadistica | Diferencia `pendingProjection - consolidated`, en puntos absolutos sobre la escala de la estadistica. |
>
> Adicionalmente, si la estadistica tiene `pendingDelta`, la salida tambien incluye `pendingProjection` (el valor que tendria si se aceptaran las pendientes).

<details><summary>Detail</summary>

1. Una estadistica que no esta tocada por ninguna decision **no se emite** en la lista de afectadas. El consumidor que la quiera leer la obtiene del baseline del `AuditReport` como hasta hoy.
2. Una estadistica solo tocada por pendientes (sin aceptadas) tiene `consolidated == original` y `acceptedDelta == 0`. Aun asi se emite, porque tiene `pendingDelta` no nulo.
3. Una estadistica solo tocada por aceptadas (sin pendientes) tiene `pendingDelta` y `pendingProjection` ausentes. Se emite con `original`, `consolidated` y `acceptedDelta`.
4. Si la estadistica tiene granularidad por nivel jerarquico (curso, milestone, topic, knowledge, quiz) y por dimension de diagnostico, los cuatro campos se emiten **por cada par `(nivel, dimension)` afectado**, no como un unico bloque agregado. Esto preserva la granularidad que el baseline ya expone.

</details>

<a id="F-CDIFF-R010"></a>
### Rule[F-CDIFF-R010] - Los deltas son **puntos absolutos** sobre la escala del dominio, no variaciones relativas
**Severity**: critical | **Validation**: VALIDATED

> Los campos `acceptedDelta` y `pendingDelta` se calculan como **diferencias aritmeticas absolutas** sobre la escala interna de la estadistica:
>
> - `acceptedDelta = consolidated - original`
> - `pendingDelta = pendingProjection - consolidated`
>
> El CLI **no calcula** `(consolidated - original) / original` ni ninguna variante porcentual / relativa. Si la escala interna del dominio es 0..1, los deltas son numeros entre -1 y 1; si la escala es porcentual (0..100), los deltas son puntos porcentuales. La presentacion `+5%` o `+5 pp` que un consumidor tipo UI pueda renderizar es decision del consumidor, no del contrato.

<details><summary>Detail</summary>

1. Esta regla evita la ambiguedad clasica entre "5% mas" (relativo: 1.05x el original) y "5 puntos porcentuales mas" (absoluto: original + 5 sobre la misma escala). El contrato es siempre el segundo.
2. La regla es testeable de forma directa: dado `original` y `consolidated`, el `acceptedDelta` emitido por el CLI debe coincidir aritmeticamente con `consolidated - original` con la precision de la escala. Lo mismo para `pendingDelta`.
3. La separacion entre datos del dominio y notacion de presentacion es identica a la de [F-PIPRE-R013](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R013). Aquella regla aplica a la presentacion textual del preview por-propuesta; aqui la analoga corre por cuenta del consumidor del contrato.

</details>

<a id="F-CDIFF-R011"></a>
### Rule[F-CDIFF-R011] - La salida cubre los mismos pares `(nivel, dimension)` que el baseline expone
**Severity**: major | **Validation**: AUTO_VALIDATED

> Para cada par `(nivel, dimension)` que el motor de auditoria reporta en el baseline del `AuditReport` (las dimensiones tipicas: `LEMMA_ABSENCE`, `SENTENCE_LENGTH`, `COCA_BUCKETS`, `LEMMA_RECURRENCE`, etc.), la estructura consolidada lo incluye en la misma granularidad si esta tocado. La salida no inventa pares que el baseline no tenga, ni omite pares que el baseline si tenga, salvo por la regla de "no tocado -> no emitido" de [F-CDIFF-R009](#F-CDIFF-R009).1.

<details><summary>Detail</summary>

1. La regla establece la **paridad estructural** entre el baseline del `AuditReport` y el consolidado: cualquier consumidor que sepa leer el primero sabe leer el segundo. La forma del par `(nivel, dimension)` en la salida JSON del CLI es la misma que la de FEAT-PIPRE F-PIPRE-R005.
2. La feature es agnostica de la dimension que origino la propuesta. Una pendiente de `LEMMA_ABSENCE` puede mover `SENTENCE_LENGTH` y la salida debe reflejar ese cruce, igual que [F-PIPRE-R005](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R005).

</details>

---

### Grupo F - Pendientes no aplicables

<a id="F-CDIFF-R012"></a>
### Rule[F-CDIFF-R012] - Una pendiente que no se puede aplicar se emite con `pendingApplicability: NOT_APPLICABLE` y causa
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si al construir las proyecciones una propuesta pendiente individual no puede aplicarse (su `nodeId` no existe en el subarbol vigente, o su `elementBefore` ya no coincide con el `consolidated` porque otra aceptada lo desplazo), la salida del CLI incluye esa propuesta en una lista `pendingApplicability` con campos:
>
> | Campo | Significado |
> |-------|-------------|
> | `proposalId` | Identificador de la propuesta. |
> | `status` | `NOT_APPLICABLE`. |
> | `reason` | Causa legible (categoria + detalle). |
>
> La pendiente no aplicable **no contribuye** a `pendingProjection` ni a `pendingDelta` de ningun nodo o estadistica.

<details><summary>Detail</summary>

1. El resto de la estructura consolidada se entrega normalmente. Una pendiente conflictiva no rompe la salida.
2. La gestion del ciclo de vida de la pendiente (rechazar, regenerar) corresponde a FEAT-REVAPR; este requerimiento solo cubre que el contrato la marque y siga sirviendo el resto de los datos.
3. Esta regla se distingue de [F-CDIFF-R013](#F-CDIFF-R013) en que aca la salida **se entrega** con la pendiente marcada; en R013 la salida entera se considera no-disponible.

</details>

---

### Grupo G - Salida no disponible

<a id="F-CDIFF-R013"></a>
### Rule[F-CDIFF-R013] - Si la estructura consolidada no se puede construir, el CLI emite `consolidatedAvailability: UNAVAILABLE` con causa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si por algun motivo el sistema no puede construir la estructura consolidada (no hay par activo, el plan activo no se puede recuperar, una propuesta aceptada referencia un nodo inexistente, la re-agregacion del subarbol falla), la salida del CLI:
>
> 1. **No** se rellena con valores arbitrarios.
> 2. Incluye el campo `consolidatedAvailability: UNAVAILABLE` con un objeto `unavailabilityReason` (categoria enum + detalle string), siguiendo el mismo patron que [F-PIPRE-R009](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R009).
> 3. El baseline del `AuditReport` sigue siendo recuperable por las operaciones existentes del CLI (no esta bloqueado por la falla del consolidado).

<details><summary>Detail</summary>

1. Casos previstos de no-disponibilidad:

   | Categoria | Descripcion |
   |-----------|-------------|
   | `NO_ACTIVE_ANALYSIS` | El curso no tiene par activo seleccionado o el `auditId` activo no existe. |
   | `ACTIVE_PLAN_UNAVAILABLE` | El `planId` activo no se puede ubicar dentro del analisis (referencia rota, archivo ausente). |
   | `INCONSISTENT_PROPOSAL` | Una propuesta aceptada referencia un `nodeId` que no se encuentra en el subarbol al momento de re-agregar. |
   | `REAGGREGATION_FAILED` | El motor no puede recomputar el agregado sobre el subarbol modificado. |
   | `OTHER` | Cualquier otra causa, con detalle textual obligatorio. |

2. La salida con `consolidatedAvailability: UNAVAILABLE` es de primera clase: el consumidor debe poder distinguirla de "consolidado vacio" (cuando si hay par activo pero ninguna propuesta aceptada o pendiente, en cuyo caso el consolidado se emite con todos los nodos del baseline sin afectar y sin lista de afectados).

**Error**: "Estructura consolidada no disponible para el analisis '<auditId>': <categoria> - <detalle>"

</details>

---

### Grupo H - Equivalencia post-stack como propiedad de testeo

<a id="F-CDIFF-R014"></a>
### Rule[F-CDIFF-R014] - El baseline de un analisis nuevo coincide con el `consolidated` del analisis anterior
**Severity**: major | **Validation**: VALIDATED

> Si el operador genera un nuevo `AuditReport` justo despues de aceptar propuestas en el analisis vigente, sin haber introducido cambios externos al curso entre medio, los valores del baseline del nuevo analisis coinciden, nodo por nodo y dimension por dimension, con los valores del campo `consolidated` del analisis anterior. La unica diferencia visible en la salida del CLI es estructural: el nuevo analisis emite los nodos del baseline sin lista de afectados, sin campos `acceptedDelta` ni `pendingDelta`, porque las decisiones anteriores ya estan materializadas en el curso y este analisis empieza limpio.

<details><summary>Detail</summary>

1. Esta regla es la consecuencia funcional de [F-CDIFF-R003](#F-CDIFF-R003) y [F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011): si las aceptadas se materializan en el curso, el siguiente analisis las ve como contenido normal.
2. La regla es **una propiedad del contrato** observable en una secuencia "aceptar -> generar nuevo analisis -> comparar". Es testeable end-to-end ejecutando esa secuencia sobre un curso fixture.
3. La equivalencia se rompe si entre la aceptacion y el nuevo analisis se modifico el curso por otros caminos (edicion manual, importacion, etc.). En ese caso el nuevo baseline refleja el curso al momento de correr el analisis y la diferencia con el `consolidated` anterior se debe a esos cambios externos, no a la feature.

</details>

---

### Grupo I - Limites explicitos del contrato

<a id="F-CDIFF-R015"></a>
### Rule[F-CDIFF-R015] - El consolidado no es un nuevo `AuditReport` ni emite uno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Servir la estructura consolidada **no genera** un nuevo `auditId`, **no persiste** un `AuditReport` y **no modifica** el plan ni los artefactos de propuesta. Es una proyeccion derivada del par `(AuditReport, propuestas del plan activo)` que vive el tiempo necesario para entregarsela al consumidor del CLI.

<details><summary>Detail</summary>

1. La regla preserva la separacion entre **historia oficial** (los `AuditReport` que el motor escribio) y **vista derivada** (lo que el contrato consolidado emite). Es la misma logica de [F-PIPRE-R002](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R002) generalizada al curso entero y a multiples propuestas.
2. La forma exacta en que el sistema construye la salida (calculo on-demand, materializada con cache, etc.) es decision de arquitectura ([DOUBT-MATERIALIZATION](#DOUBT-MATERIALIZATION)). Lo que esta regla prohibe es que el calculo escriba un `AuditReport` o cambie cualquier artefacto persistido.
3. Aceptar o rechazar propuestas no se hace via el contrato consolidado: esas operaciones siguen siendo las de FEAT-REVAPR y operan sobre los artefactos de propuesta.

</details>

<a id="F-CDIFF-R016"></a>
### Rule[F-CDIFF-R016] - El consolidado supersede el caso de "preview combinado" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) dejo abierto
**Severity**: major | **Validation**: VALIDATED

> [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) y [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW) marcaron como fuera de alcance el caso "varias propuestas combinadas, ver impacto agregado". Esta feature lo cubre por la via de la estructura consolidada: la salida ya combina baseline + aceptadas + pendientes del plan activo en un unico documento. FEAT-PIPRE sigue siendo el contrato del preview por-propuesta-individual (la foto del impacto al momento de generar la propuesta, [F-PIPRE-R008](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R008)); FEAT-CDIFF aporta el contrato de la lectura agregada.

<details><summary>Detail</summary>

1. Las dos features no se pisan: el preview por-propuesta sigue produciendose y persistiendose en cada generacion de `RevisionProposal` y conserva su rol historico. El consolidado se construye al momento de servir la salida y combina muchas propuestas a la vez.
2. La diferencia operativa: el preview por-propuesta es **eager y por-`proposalId`** ([F-PIPRE-R001](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R001)); el consolidado es **derivado al momento de la peticion** ([F-CDIFF-R015](#F-CDIFF-R015)).

</details>

<a id="F-CDIFF-R017"></a>
### Rule[F-CDIFF-R017] - El consolidado se construye sobre el plan activo, no combina planes
**Severity**: major | **Validation**: VALIDATED

> El consolidado de un `AuditReport` solo combina su baseline con las propuestas (aceptadas y pendientes) del **plan activo** asociado a ese mismo analisis. No mezcla propuestas de planes ajenos, ni combina varios analisis en una sola salida. Una pendiente dormida de un analisis viejo solo se incorpora a la salida si el operador hace activo a ese analisis.

<details><summary>Detail</summary>

1. Esta restriccion es deliberada y simplifica la semantica: para cada `AuditReport`, hay como mucho un plan activo y un consolidado bien definido.
2. Combinar varios analisis en una sola salida queda fuera de alcance ([DOUBT-MULTI-ANALYSIS-VIEW](#DOUBT-MULTI-ANALYSIS-VIEW)).

</details>

<a id="F-CDIFF-R018"></a>
### Rule[F-CDIFF-R018] - El consolidado cubre solo cambios sobre nodos existentes
**Severity**: minor | **Validation**: AUTO_VALIDATED

> El contrato asume el patron actual de propuesta (sustitucion de un `elementBefore` por un `elementAfter` sobre un `nodeId` que existe en el curso). No modela el efecto de propuestas que crearian, eliminarian o reordenarian nodos.

<details><summary>Detail</summary>

Heredado de [F-PIPRE-R012](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R012) y [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014). Si una iteracion futura introduce propuestas estructurales (creacion / eliminacion / reordenamiento), el contrato de salida se revisa.

</details>

---

## Contexto

### Que es content-audit y donde encaja esta feature

Content-audit es un **CLI / backend** que audita cursos de idiomas y produce dos clases de artefactos:
- `AuditReport`: la historia oficial de scores y diagnosticos por nodo.
- `RevisionProposal`: propuestas de cambio sobre nodos especificos, con ciclo de vida `PENDING_APPROVAL` -> `APPROVED` (aplicada al curso) o `REJECTED` (descartada).

Hasta hoy, cuando un operador acepta una propuesta, la unica forma de ver "como queda el curso ahora" reflejado en scores es **correr un nuevo analisis**. Esto produce un nuevo `AuditReport` por aceptacion. Para un plan con decenas o cientos de propuestas y un operador que las decide a goteo, esto significa generar -y despues navegar- decenas o cientos de analisis casi-iguales, cada uno con la misma estructura pero con un nodo cambiado. No escala ni desde computo ni desde la perspectiva de un consumidor que quiere mostrar el cambio acumulado.

### El contrato que esta feature introduce

FEAT-CDIFF define el **contrato de datos consolidado** que el CLI emite por cada `AuditReport`. La salida combina, en un unico documento, el baseline del analisis con los efectos de las decisiones (aceptadas y pendientes) del plan activo. Lo central es que un consumidor cualquiera -sea la UI actual, sea otra herramienta, sea un script de CI- puede leer este documento y reconstruir el estado del curso considerando todas las decisiones, sin recomputar.

Los campos clave del contrato:

| Campo | Donde aparece | Significado |
|-------|---------------|-------------|
| `consolidated` | Cada nodo afectado y cada estadistica afectada | Valor vigente: baseline + aceptadas. |
| `pendingProjection` | Cada nodo afectado por al menos una pendiente, y cada estadistica con `pendingDelta` | Valor proyectado: consolidated + pendientes del plan activo. |
| `acceptedDelta` | Cada estadistica afectada | `consolidated - original`, en puntos absolutos. |
| `pendingDelta` | Cada estadistica con pendientes | `pendingProjection - consolidated`, en puntos absolutos. |
| `acceptedProposalIds` | Cada nodo con aceptadas | Lista de `proposalId` aceptados que afectaron al nodo. |
| `pendingProposalId` | Cada nodo con pendiente | `proposalId` de la pendiente que produjo `pendingProjection`. |
| `pendingApplicability` | Raiz de la salida | Lista de pendientes marcadas como NO aplicables, con causa. |
| `consolidatedAvailability` + `unavailabilityReason` | Raiz de la salida | UNAVAILABLE + (categoria, detalle) cuando el consolidado no se puede construir. |

### Caso de uso del consumidor

El consumidor tipico es una UI que quiere mostrar el curso entero con overlays diferenciales: un quiz cuyo enunciado cambio por una aceptada y al que un refinador ahora propone un cambio adicional como pendiente; un topic cuyo score subio +5 puntos por las aceptadas y subiria otros +2 si las pendientes se aceptaran; un milestone donde una pendiente sobre `COCA_BUCKETS` proyecta una mejora pero todavia no se aplico.

El consumidor renderiza:
- La presentacion visual (colores, tamanos, posicionamiento, grafos) en su capa, eligiendo como destacar `consolidated` vs `pendingProjection`.
- La conversion de los deltas absolutos a la notacion que prefiera (`+5%` con la convencion que use, barras, semaforos).
- La interaccion con el operador (aceptar, rechazar, regenerar), que sigue cayendo en los verbos existentes de FEAT-REVAPR.

Lo que esta feature **define** es el contrato canonico que ese consumidor lee. Lo que **no define** son los detalles de presentacion ni la operativa de aceptar/rechazar; ambos quedan fuera del alcance ([F-CDIFF-R015](#F-CDIFF-R015)).

### Apilado entre analisis sucesivos

El comportamiento de "apilado" es central:

1. Las propuestas aceptadas hasta el momento ya estan **materializadas en el curso** ([F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011)). El nuevo analisis las ve como contenido normal ([F-CDIFF-R003](#F-CDIFF-R003)).
2. Las propuestas pendientes del plan anterior **no se migran** al plan nuevo ([F-CDIFF-R004](#F-CDIFF-R004)). Quedan dormidas, recuperables cambiando el par activo.
3. Las propuestas rechazadas **desaparecen** del consolidado ([F-CDIFF-R005](#F-CDIFF-R005)).

La consecuencia es la **equivalencia post-stack** ([F-CDIFF-R014](#F-CDIFF-R014)): el baseline del analisis nuevo y el `consolidated` del anterior describen el mismo estado del curso, desde dos angulos. La diferencia es que el nuevo no carga deltas: empieza limpio.

### Generalizacion del impact preview

FEAT-PIPRE responde "que pasaria si aceptara esta propuesta sola" y persiste un preview eager por cada `RevisionProposal` ([F-PIPRE-R001](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R001)). FEAT-CDIFF responde "como esta el curso considerando todo lo aceptado y pendiente del plan activo, en un unico documento" y se construye al momento de servir la salida.

Las dos features conviven: el preview por-propuesta sigue siendo la foto historica del impacto al generar la propuesta ([F-PIPRE-R008](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R008)); el consolidado es la lectura agregada que el consumidor pide para navegar el curso entero. [F-CDIFF-R016](#F-CDIFF-R016) hace explicita esta supersesion: el caso "preview combinado de varias propuestas" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) y [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW) habian dejado fuera, queda cubierto por la via del consolidado y no requiere un mecanismo extra dentro de FEAT-PIPRE.

---

## Alcance

- **En alcance**: Contrato de datos del CLI que expone el consolidado por `AuditReport`. Identificacion del par activo `(auditId, planId)`. Campos `consolidated`, `pendingProjection`, `acceptedDelta`, `pendingDelta` con semantica precisa. Trazabilidad de propuestas (`acceptedProposalIds`, `pendingProposalId`). Marcado de pendientes no aplicables. Marcado de no-disponibilidad. Equivalencia post-stack como propiedad observable. Supersesion explicita de [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) / [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW).
- **Fuera de alcance**: Decisiones de presentacion del consumidor (colores, tamanos, posicionamiento). Conversion de los deltas absolutos a notacion porcentual relativa. Operaciones de aceptar / rechazar / regenerar (siguen siendo de FEAT-REVAPR). Persistencia automatica del consolidado como nuevo `AuditReport` ([F-CDIFF-R015](#F-CDIFF-R015)). Combinacion de varios analisis o planes en una unica salida ([F-CDIFF-R017](#F-CDIFF-R017), [DOUBT-MULTI-ANALYSIS-VIEW](#DOUBT-MULTI-ANALYSIS-VIEW)). Distincion campo-a-campo dentro del snapshot de un nodo ([DOUBT-FIELD-IDENTITY](#DOUBT-FIELD-IDENTITY)). Cambios a la forma del `AuditReport` en disco. La sintaxis exacta del verbo CLI y de la estructura JSON detallada de la salida queda como decision de arquitectura, mientras se respete la semantica de los campos.

---

## User Journeys

### Journey[F-CDIFF-J001] - El CLI emite el consolidado del par activo
**Validation**: VALIDATED

El consumidor pide la salida consolidada del CLI. El sistema resuelve el par activo, combina baseline + propuestas del plan activo y entrega el documento, o emite el estado de no-disponibilidad explicito.

```yaml
journeys:
  - id: F-CDIFF-J001
    name: Salida consolidada del CLI
    flow:
      - id: pedir_salida
        action: "El consumidor invoca el verbo del CLI que entrega la estructura consolidada del curso"
        then: resolver_activo
      - id: resolver_activo
        action: "El sistema resuelve el par (activeAuditId, activePlanId)"
        gate: [F-CDIFF-R001]
        outcomes:
          - when: "El par activo se resuelve y el plan se puede recuperar"
            then: combinar_y_emitir
          - when: "No se puede resolver el par activo o el plan"
            then: emitir_no_disponible
      - id: combinar_y_emitir
        action: "El CLI emite la estructura consolidada con consolidated/pendingProjection por nodo afectado, original/consolidated/acceptedDelta/pendingDelta por estadistica afectada y la trazabilidad de propuestas"
        gate: [F-CDIFF-R006, F-CDIFF-R007, F-CDIFF-R008, F-CDIFF-R009, F-CDIFF-R010, F-CDIFF-R011, F-CDIFF-R015]
        result: success
      - id: emitir_no_disponible
        action: "El CLI emite consolidatedAvailability: UNAVAILABLE con unavailabilityReason (categoria + detalle); el AuditReport sigue accesible por las operaciones existentes"
        gate: [F-CDIFF-R013]
        result: success
```

### Journey[F-CDIFF-J002] - Nodo hoja con aceptada y pendiente sobre el mismo `nodeId`
**Validation**: VALIDATED

Cubre [F-CDIFF-R006](#F-CDIFF-R006) y [F-CDIFF-R007](#F-CDIFF-R007): el `consolidated` lleva el snapshot de la aceptada y `pendingProjection` lleva el snapshot con la pendiente aplicada sobre el `consolidated`. La salida no incluye un tercer snapshot del baseline original.

```yaml
journeys:
  - id: F-CDIFF-J002
    name: Nodo hoja con aceptada y pendiente
    flow:
      - id: situacion_inicial
        action: "El plan activo tiene una propuesta aceptada que toco un nodo y una propuesta pendiente sobre el mismo nodeId"
        then: pedir_salida_del_nodo
      - id: pedir_salida_del_nodo
        action: "El consumidor pide la estructura consolidada del curso (que incluye al nodo)"
        then: emitir_consolidated
      - id: emitir_consolidated
        action: "El CLI emite consolidated igual al snapshot de la aceptada y registra el proposalId aceptado en acceptedProposalIds"
        gate: [F-CDIFF-R006, F-CDIFF-R007]
        then: emitir_proyeccion
      - id: emitir_proyeccion
        action: "El CLI emite pendingProjection igual al snapshot que se obtendria aplicando la pendiente sobre el consolidated, con pendingProposalId; no se incluye un tercer snapshot del baseline original"
        gate: [F-CDIFF-R006, F-CDIFF-R007]
        result: success
```

### Journey[F-CDIFF-J003] - Padre con aceptadas y pendientes en su subarbol
**Validation**: VALIDATED

Cubre [F-CDIFF-R008](#F-CDIFF-R008): `consolidated` se obtiene re-agregando el subarbol con las aceptadas (sin pendientes); `pendingProjection` se obtiene re-agregando con aceptadas + pendientes, respetando la estrategia de agregacion del analizador correspondiente.

```yaml
journeys:
  - id: F-CDIFF-J003
    name: Padre con consolidated y pendingProjection
    flow:
      - id: situacion_inicial
        action: "Un topic tiene aceptadas aplicadas en algunos quizzes del subarbol y pendientes en otros"
        then: emitir_consolidated_padre
      - id: emitir_consolidated_padre
        action: "El CLI emite consolidated del topic re-agregando el subarbol con las aceptadas aplicadas e ignorando las pendientes"
        gate: [F-CDIFF-R008]
        then: decidir_proyeccion
      - id: decidir_proyeccion
        action: "El sistema verifica si el subarbol tiene al menos una pendiente aplicable"
        outcomes:
          - when: "Hay al menos una pendiente aplicable"
            then: emitir_pending_projection
          - when: "No hay pendientes aplicables en el subarbol"
            then: omitir_pending_projection
      - id: emitir_pending_projection
        action: "El CLI emite pendingProjection del topic re-agregando con aceptadas + pendientes, respetando la estrategia del analizador (sin sumar deltas de hijos)"
        gate: [F-CDIFF-R008, F-CDIFF-R011]
        result: success
      - id: omitir_pending_projection
        action: "El CLI emite el padre con consolidated solamente; pendingProjection esta ausente"
        gate: [F-CDIFF-R008]
        result: success
```

### Journey[F-CDIFF-J004] - Estadistica afectada con cuatro campos numericos y deltas absolutos
**Validation**: VALIDATED

Cubre [F-CDIFF-R009](#F-CDIFF-R009), [F-CDIFF-R010](#F-CDIFF-R010) y [F-CDIFF-R011](#F-CDIFF-R011): para cada par `(nivel, dimension)` afectado, el CLI emite original/consolidated/acceptedDelta/pendingDelta, con `acceptedDelta = consolidated - original` y `pendingDelta = pendingProjection - consolidated`, en puntos absolutos.

```yaml
journeys:
  - id: F-CDIFF-J004
    name: Estadistica afectada con cuatro campos
    flow:
      - id: pedir_estadisticas
        action: "El consumidor lee la lista de estadisticas afectadas en la salida"
        then: resolver_estadistica
      - id: resolver_estadistica
        action: "El sistema selecciona los pares (nivel, dimension) reportados por el baseline que esten tocados por aceptadas o pendientes del plan activo"
        gate: [F-CDIFF-R009, F-CDIFF-R011]
        outcomes:
          - when: "El par esta tocado por aceptadas, pendientes o ambas"
            then: emitir_campos
          - when: "El par no esta tocado"
            then: omitir_par
      - id: emitir_campos
        action: "El CLI emite original (baseline), consolidated (con aceptadas), acceptedDelta (consolidated - original), y, si hay pendientes, pendingProjection y pendingDelta (pendingProjection - consolidated). Todos los deltas son puntos absolutos sobre la escala de la estadistica"
        gate: [F-CDIFF-R009, F-CDIFF-R010]
        result: success
      - id: omitir_par
        action: "El CLI no incluye el par en la lista de afectados; sigue disponible al leer el baseline directamente"
        gate: [F-CDIFF-R009]
        result: success
```

### Journey[F-CDIFF-J005] - Pendiente no aplicable: la salida la marca y sigue sirviendo
**Validation**: VALIDATED

Cubre [F-CDIFF-R012](#F-CDIFF-R012): cuando una pendiente no se puede aplicar (nodeId ausente, elementBefore desplazado por una aceptada anterior), el CLI la lista en `pendingApplicability` con causa y la excluye del calculo, pero el resto del consolidado se entrega normalmente.

```yaml
journeys:
  - id: F-CDIFF-J005
    name: Pendiente marcada como no aplicable
    flow:
      - id: situacion_inicial
        action: "El plan activo tiene una pendiente cuyo elementBefore no coincide con el consolidated del nodo (porque otra aceptada lo desplazo) o cuyo nodeId no se encuentra"
        then: intentar_aplicar
      - id: intentar_aplicar
        action: "El sistema intenta aplicar la pendiente sobre el subarbol vigente"
        outcomes:
          - when: "La pendiente se puede aplicar"
            then: incluir_en_proyeccion
          - when: "La pendiente no se puede aplicar"
            then: marcar_no_aplicable
      - id: incluir_en_proyeccion
        action: "La pendiente contribuye a pendingProjection y pendingDelta donde corresponda"
        gate: [F-CDIFF-R008, F-CDIFF-R009]
        result: success
      - id: marcar_no_aplicable
        action: "El CLI agrega un entry { proposalId, status: NOT_APPLICABLE, reason } en pendingApplicability; la pendiente se excluye del calculo de proyeccion pero el resto del consolidado se entrega normalmente"
        gate: [F-CDIFF-R012]
        result: success
```

### Journey[F-CDIFF-J006] - Cambio de par activo: idempotente y no destructivo
**Validation**: VALIDATED

Cubre [F-CDIFF-R002](#F-CDIFF-R002) y [F-CDIFF-R004](#F-CDIFF-R004): cambiar el par activo no toca ningun artefacto, y la salida posterior corresponde al nuevo par activo, recuperando las pendientes dormidas del plan al que apunta.

```yaml
journeys:
  - id: F-CDIFF-J006
    name: Cambio de par activo y recuperacion de pendientes dormidas
    flow:
      - id: situacion_inicial
        action: "Existe un analisis A con pendientes en su plan y un analisis B mas nuevo cuyo planId esta como activo"
        then: pedir_salida_b
      - id: pedir_salida_b
        action: "El consumidor pide la estructura consolidada (corresponde a B)"
        gate: [F-CDIFF-R001, F-CDIFF-R017]
        then: cambiar_activo_a_a
      - id: cambiar_activo_a_a
        action: "El consumidor invoca la operacion del CLI que cambia el par activo a (auditId de A, planId de A); ningun AuditReport, plan, propuesta o curso se modifica"
        gate: [F-CDIFF-R002]
        then: pedir_salida_a
      - id: pedir_salida_a
        action: "El consumidor pide la estructura consolidada; ahora corresponde a A y las pendientes dormidas de A aparecen como pendingProjection/pendingDelta sobre los nodos y estadisticas tocadas"
        gate: [F-CDIFF-R001, F-CDIFF-R004, F-CDIFF-R006, F-CDIFF-R009]
        result: success
```

### Journey[F-CDIFF-J007] - Equivalencia post-stack entre analisis sucesivos
**Validation**: VALIDATED

Cubre [F-CDIFF-R014](#F-CDIFF-R014) y [F-CDIFF-R003](#F-CDIFF-R003): si se genera un nuevo analisis justo despues de aceptar propuestas, su baseline es equivalente al `consolidated` del analisis anterior.

```yaml
journeys:
  - id: F-CDIFF-J007
    name: Equivalencia post-stack entre analisis sucesivos
    flow:
      - id: aceptar_propuestas
        action: "El operador acepta una o mas propuestas del plan activo, materializandolas en el curso (FEAT-REVAPR R011)"
        then: leer_consolidated_anterior
      - id: leer_consolidated_anterior
        action: "El consumidor pide la estructura consolidada del analisis activo y registra los valores del campo consolidated por nodo y por estadistica"
        gate: [F-CDIFF-R008, F-CDIFF-R009]
        then: generar_nuevo_analisis
      - id: generar_nuevo_analisis
        action: "El operador genera un nuevo AuditReport sobre el curso, sin haber introducido cambios externos al curso entre medio"
        gate: [F-CDIFF-R003]
        then: comparar
      - id: comparar
        action: "El baseline del nuevo AuditReport coincide nodo por nodo y dimension por dimension con los valores del consolidated del analisis anterior; la salida del nuevo analisis no carga acceptedDelta ni pendingDelta porque empieza limpio"
        gate: [F-CDIFF-R014]
        result: success
```

### Journey[F-CDIFF-J008] - Supersesion explicita del preview combinado de FEAT-PIPRE
**Validation**: VALIDATED

Cubre [F-CDIFF-R016](#F-CDIFF-R016): la salida consolidada cubre el caso de uso "ver impacto agregado de varias propuestas" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) y [DOUBT-BATCH-PREVIEW](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#DOUBT-BATCH-PREVIEW) habian dejado fuera. El preview por-propuesta de FEAT-PIPRE sigue produciendose intacto.

```yaml
journeys:
  - id: F-CDIFF-J008
    name: Supersesion del caso de preview combinado
    flow:
      - id: situacion_inicial
        action: "Existen varias propuestas (aceptadas y pendientes) en el plan activo; cada una de ellas tiene su preview eager por-propuesta producido por FEAT-PIPRE al ser generada"
        then: pedir_salida_consolidada
      - id: pedir_salida_consolidada
        action: "El consumidor que querria 'ver el impacto si acepto todas estas pendientes juntas' pide la estructura consolidada en lugar de un preview combinado"
        then: emitir_combinacion
      - id: emitir_combinacion
        action: "El CLI emite una unica salida que combina baseline + aceptadas + pendientes del plan activo en consolidated/pendingProjection/acceptedDelta/pendingDelta; los previews por-propuesta de FEAT-PIPRE quedan disponibles como antes (no se borran ni se modifican)"
        gate: [F-CDIFF-R015, F-CDIFF-R016]
        result: success
```

---

## Open Questions

<a id="DOUBT-FIELD-IDENTITY"></a>
### Doubt[DOUBT-FIELD-IDENTITY] - Granularidad del "campo afectado" dentro del snapshot
**Status**: RESOLVED (en esta iteracion)

[F-CDIFF-R006](#F-CDIFF-R006) define la unidad afectada como **el nodo entero** (snapshot completo del `elementBefore`/`elementAfter` de la propuesta). Las opciones consideradas:

- [ ] Opcion A: Diff sobre el snapshot completo del nodo, comparando campo a campo. Requiere identidad de campo segun el esquema del snapshot.
- [ ] Opcion B: La propuesta lleva un descriptor explicito de los campos tocados.
- [x] Opcion C: Granularidad coarse al nivel del nodo entero (no se distingue campo a campo). El consumidor que necesite un diff fino lo computa por su cuenta comparando `consolidated` con `pendingProjection`.

**Answer**: Opcion C. Hoy el snapshot solo carga `quiz: QuizTemplateEntity` para QUIZ; los demas niveles tienen snapshot vacio. Sin propuestas multi-campo reales, distinguir "campo dentro del nodo" es ingenieria especulativa. Si una iteracion futura introduce propuestas con descriptores de campo, [F-CDIFF-R006](#F-CDIFF-R006) se revisa para emitir granularidad fina.

<a id="DOUBT-PENDING-CONFLICT"></a>
### Doubt[DOUBT-PENDING-CONFLICT] - Que pasa si dos pendientes del plan activo se pisan entre si?
**Status**: RESOLVED

[F-REVAPR-R010](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R010) (no acumular pendientes sobre la misma tarea) cubre el caso comun. Para el caso teorico de "dos pendientes sobre el mismo nodo desde tareas distintas":

- [x] Opcion A: Aplicar las pendientes en orden por `createdAt` ascendente; la segunda en chocar cae en [F-CDIFF-R012](#F-CDIFF-R012) (`elementBefore` no coincide con el `consolidated`) y se marca como NOT_APPLICABLE. No se requiere mecanismo nuevo.
- [ ] Opcion B: Detectar conflictos por adelantado y rechazar todas las pendientes en conflicto.
- [ ] Opcion C: Modelar explicitamente un orden de aplicacion (priority, etc.).

**Answer**: Opcion A. Es coherente con [F-CDIFF-R012](#F-CDIFF-R012) y no requiere ampliar el contrato.

<a id="DOUBT-MATERIALIZATION"></a>
### Doubt[DOUBT-MATERIALIZATION] - On-demand vs cacheada
**Status**: OPEN (decision de arquitectura)

[F-CDIFF-R015](#F-CDIFF-R015) prohibe que la salida escriba un `AuditReport`, pero deja abierto si:

- [ ] Opcion A: Calculo completo on-demand cada vez. Sin caches.
- [ ] Opcion B: Materializacion como artefacto interno (no oficial) invalidado por cambios.
- [ ] Opcion C: Cache parcial (por nodo o por estadistica) invalidado por cambios.

**Answer**: Pendiente. Para el MVP, Opcion A es suficiente. Si se mide costo prohibitivo en cursos grandes (~11.500 quizzes), evolucionar a B/C sin cambiar el contrato observable.

<a id="DOUBT-ACTIVE-PERSISTENCE"></a>
### Doubt[DOUBT-ACTIVE-PERSISTENCE] - Donde vive el par activo
**Status**: OPEN (decision de arquitectura)

[F-CDIFF-R001](#F-CDIFF-R001) y [F-CDIFF-R002](#F-CDIFF-R002) requieren un par activo `(auditId, planId)` consultable y mutable, pero no fijan el storage:

- [ ] Opcion A: Archivo dedicado bajo `.content-audit/active-analysis.json` con `{auditId, planId}`.
- [ ] Opcion B: Campo dentro de un archivo existente (indice de analisis del curso).
- [ ] Opcion C: Parametro de la operacion (sin estado persistido del activo): el consumidor pasa `auditId` y `planId` cada vez que pide la salida.

**Answer**: Pendiente. La regla funcional admite cualquiera mientras se respete idempotencia y no-destructividad. Opcion A se alinea con la simetria de los demas stores filesystem.

<a id="DOUBT-MULTI-ANALYSIS-VIEW"></a>
### Doubt[DOUBT-MULTI-ANALYSIS-VIEW] - Combinar varios analisis en una sola salida (feature.future)
**Status**: OPEN (feature.future)

[F-CDIFF-R017](#F-CDIFF-R017) limita la salida al plan activo de un unico `AuditReport`. Casos de uso anticipados que quedan fuera: cruzar pendientes dormidas de los ultimos N analisis sin cambiar el activo, comparar el consolidated de A con el de B. Cruzar planes y analisis introduce decisiones de orden de aplicacion entre planes y reglas para resolver propuestas que tocan los mismos nodos en planes distintos. Demasiado rico para esta iteracion.

**Answer**: Pendiente, fuera de alcance. Se trata como feature.future.

---

## ASSUMPTIONS

1. **Existe el concepto de "plan activo" asociado a un `AuditReport` y es uno solo por analisis.** [F-CDIFF-R001](#F-CDIFF-R001) y [F-CDIFF-R017](#F-CDIFF-R017) asumen que la relacion analisis -> plan activo es 1:1. La forma concreta de seleccionarlo (default, multiples planes posibles, etc.) es decision de arquitectura.
2. **El curso es la fuente de verdad entre analisis sucesivos: las aceptadas se materializan ahi.** Heredado de [F-REVAPR-R011](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R011). [F-CDIFF-R003](#F-CDIFF-R003) y [F-CDIFF-R014](#F-CDIFF-R014) dependen de esta propiedad.
3. **El motor de auditoria puede recomputar agregaciones sobre un subarbol modificado en memoria sin emitir un `AuditReport` oficial.** Heredado de FEAT-PIPRE (assumption 1). FEAT-CDIFF amplia el alcance: ahora se aplican varias propuestas a la vez, no una sola.
4. **Los pares `(nivel, dimension)` reportados por la salida son los mismos del baseline.** [F-CDIFF-R011](#F-CDIFF-R011). No se inventan pares nuevos ni se ocultan existentes.
5. **Las propuestas siguen el patron de sustitucion sobre `nodeId` estable.** Heredado de [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014). [F-CDIFF-R018](#F-CDIFF-R018) lo declara explicito.
6. **Aceptar / rechazar propuestas es una operacion separada de "leer la salida consolidada".** [F-CDIFF-R015](#F-CDIFF-R015). Las decisiones siguen la pipeline de FEAT-REVAPR.
7. **La sintaxis exacta del verbo CLI y el shape JSON detallado quedan como decision de arquitectura.** El requerimiento define la semantica de los campos y su presencia condicional, no su nombre exacto en la salida ni la forma del comando que la emite. Mientras la semantica se respete, el arquitecto puede elegir la forma.

---

## References

- **FEAT-COURSE** — Define la jerarquia del curso (Course -> Milestone -> Topic -> Knowledge -> Quiz Template) sobre la que se construyen los nodos hoja y los padres de la salida. Citado por [F-CDIFF-R008](#F-CDIFF-R008).
- **FEAT-REVAPR** — Define el ciclo de vida de las propuestas (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`) y la regla de aplicacion al curso. La salida consolidada lee esos veredictos. Citado por [F-CDIFF-R003](#F-CDIFF-R003), [F-CDIFF-R004](#F-CDIFF-R004), [F-CDIFF-R005](#F-CDIFF-R005), [F-CDIFF-R014](#F-CDIFF-R014), [F-CDIFF-R015](#F-CDIFF-R015).
- **FEAT-REVBYP** — Define la `RevisionProposal` con sus campos (`elementBefore`, `elementAfter`, `nodeId`, `planId`, `sourceAuditId`). El contrato consolidado se apoya en esa estructura. Citado por [F-CDIFF-R004](#F-CDIFF-R004), [F-CDIFF-R006](#F-CDIFF-R006), [F-CDIFF-R018](#F-CDIFF-R018).
- **FEAT-PIPRE** — Preview de impacto por propuesta individual. FEAT-CDIFF lo extiende con un contrato de salida agregado por analisis. Supersede explicitamente el escenario "preview combinado" que [F-PIPRE-R011](../2026-05-03.01_proposal-impact-preview/REQUIREMENT.md#F-PIPRE-R011) habia dejado fuera. Citado por [F-CDIFF-R010](#F-CDIFF-R010), [F-CDIFF-R011](#F-CDIFF-R011), [F-CDIFF-R013](#F-CDIFF-R013), [F-CDIFF-R015](#F-CDIFF-R015), [F-CDIFF-R016](#F-CDIFF-R016).
- **FEAT-LAPS** — Estrategia de propuesta para `LEMMA_ABSENCE`. Aporta el patron de sustitucion sobre `nodeId` estable que la salida hereda. Citado por [F-CDIFF-R018](#F-CDIFF-R018).
- **FEAT-COCA** — Ejemplo de analizador con estrategia de agregacion no-lineal (acumulacion de conteos por banda). [F-CDIFF-R008](#F-CDIFF-R008).2 lo cita como caso de prueba para que la re-agregacion respete la estrategia declarada.
