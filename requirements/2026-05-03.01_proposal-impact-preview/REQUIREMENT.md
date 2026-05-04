---
feature:
  id: FEAT-PIPRE
  code: F-PIPRE
  name: Preview de impacto de propuestas de revision sobre los scores de auditoria
  priority: major
---

# Preview de impacto de propuestas de revision sobre los scores de auditoria

## TL;DR

**Que**: Al generar una `RevisionProposal`, el sistema computa y deja disponible un "preview de impacto" que describe como cambiarian los scores de auditoria del curso (a nivel del nodo afectado, sus contenedores y el curso completo) si esa propuesta se aprobara, discriminado por dimension de diagnostico, sin modificar el curso real.

**Por que**: Hoy la unica forma de saber si una propuesta mejora la auditoria es aprobarla y volver a auditar; el operador necesita ese feedback antes de decidir, especialmente en modo humano (FEAT-REVAPR), para distinguir propuestas que mueven la aguja de las que no.

## Reglas de Negocio

### Grupo A - Cuando se computa el preview y a que esta atado

<a id="F-PIPRE-R001"></a>
### Rule[F-PIPRE-R001] - Cada `RevisionProposal` recien generada lleva asociado un preview de impacto
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada vez que el sistema construye una `RevisionProposal` (sea la propuesta producto del flujo de FEAT-REVBYP, FEAT-REVAPR o de cualquier estrategia real como FEAT-LAPS), el sistema computa de inmediato un preview de impacto asociado a esa propuesta y lo deja disponible junto al artefacto persistido de la propuesta.

<details><summary>Detail</summary>

1. El preview se calcula **una sola vez por propuesta**, en el mismo flujo en que se construye la propuesta. No se recalcula en cada lectura.
2. El preview esta atado a una unica `RevisionProposal` (su `proposalId`). Cuando se lista o consulta la propuesta, el preview asociado debe ser recuperable en la misma operacion.
3. La regla aplica por igual al modo `auto` y al modo `human` de FEAT-REVAPR. En modo `auto` el preview queda disponible aunque la propuesta se aplique al instante; en modo `human` el preview es justamente la informacion clave para que el operador decida.
4. El preview es informacion **derivada** del par `(curso actual, propuesta)`; no requiere que la propuesta se decida ni se aplique para existir.

</details>

<a id="F-PIPRE-R002"></a>
### Rule[F-PIPRE-R002] - El preview es estrictamente "what-if" y no modifica el curso ni emite un `AuditReport`
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Computar el preview de impacto no modifica el curso real, no persiste un `AuditReport` nuevo y no genera un nuevo `auditId` en el historial de auditorias.

<details><summary>Detail</summary>

El preview es una proyeccion simulada: el sistema toma el curso actual, simula como se veria si se aplicara `elementAfter` en lugar de `elementBefore`, recomputa los scores sobre esa simulacion y entrega los deltas. La simulacion vive solo el tiempo necesario para computar el preview; despues queda solamente el preview como valor escalar. El curso en disco, el `AuditReport` oficial y el plan de refinamiento no se ven afectados por el calculo del preview.

Esto distingue al preview de una "auditoria oficial post-aprobacion": esa solo ocurre tras aplicar la propuesta (FEAT-REVBYP / FEAT-REVAPR) y volver a correr la auditoria por separado.

</details>

<a id="F-PIPRE-R003"></a>
### Rule[F-PIPRE-R003] - El preview se computa contra el estado del curso al momento de generar la propuesta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El preview compara dos estados: (a) el curso tal como esta cuando la propuesta se genera, y (b) el mismo curso con `elementBefore` reemplazado por `elementAfter`. La auditoria contra la que se calculan los deltas es la que corresponde a ese momento.

<details><summary>Detail</summary>

1. El estado base del preview es el del curso cuando la propuesta se construye, no el del curso al momento futuro en que el operador la consulte.
2. Si entre la generacion de la propuesta y su decision (especialmente en modo humano) el curso cambia (porque otra propuesta fue aprobada antes, por ejemplo), el preview asociado a esta propuesta sigue reflejando los deltas calculados originalmente y queda potencialmente desactualizado. Como debe comportarse el sistema en ese caso lo cubre [F-PIPRE-R009](#F-PIPRE-R009).
3. La eleccion de "momento de generacion" como base, en lugar de "momento de consulta", evita recalcular el preview cada vez que alguien lo lee y mantiene la asociacion 1:1 entre `proposalId` y preview.

</details>

---

### Grupo B - Que contiene el preview

<a id="F-PIPRE-R004"></a>
### Rule[F-PIPRE-R004] - El preview reporta deltas a nivel del nodo afectado y de cada contenedor hasta el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El preview describe como cambiaria el score de auditoria a lo largo de toda la cadena jerarquica que contiene al elemento afectado: el nodo objetivo de la propuesta, los contenedores intermedios (knowledge, milestone, topic) y el curso completo.

<details><summary>Detail</summary>

| Nivel reportado | Descripcion |
|-----------------|-------------|
| Nodo objetivo | El nodo identificado por el `nodeTarget` y `nodeId` de la propuesta (FEAT-REVBYP R001), tipicamente un quiz para `LEMMA_ABSENCE`. |
| Contenedor inmediato | El knowledge que contiene al nodo objetivo (cuando aplica). |
| Contenedores superiores | Milestone y topic que contienen al knowledge, en ese orden. |
| Curso completo | El nivel raiz, asociado al `overallScore` del `AuditReportSummary`. |

Si el nodo objetivo coincide con uno de los niveles superiores (por ejemplo, una propuesta sobre el knowledge entero), los niveles intermedios que quedan vacios se omiten en lugar de reportarse como cero. El preview no inventa niveles que no existen en el arbol del curso.

</details>

<a id="F-PIPRE-R005"></a>
### Rule[F-PIPRE-R005] - El preview discrimina por dimension de diagnostico
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada nivel reportado, el preview no se limita al score agregado: tambien describe como cambiaria por cada dimension de diagnostico (`LEMMA_ABSENCE`, `SENTENCE_LENGTH`, `COCA_BUCKETS`, `LEMMA_RECURRENCE`, etc.) que aporte score en ese nivel.

<details><summary>Detail</summary>

1. El operador debe poder leer del preview, por ejemplo: "en el quiz X, +2% en `LEMMA_ABSENCE`, -1% en `SENTENCE_LENGTH`; en el knowledge contenedor, +0,5% en `LEMMA_ABSENCE`, sin cambio en otras dimensiones; en el curso, +0,3% global".
2. La feature es **agnostica de la dimension** que origino la propuesta. Una propuesta producida por la estrategia de `LEMMA_ABSENCE` (FEAT-LAPS) puede mover dimensiones distintas: por ejemplo, el ejercicio nuevo puede corregir `LEMMA_ABSENCE` pero alterar la longitud de la oracion y por lo tanto mover `SENTENCE_LENGTH`. El preview debe exponer esos efectos cruzados, no limitarse a la dimension nominal de la tarea.
3. Si en un nivel una dimension no se evalua (porque el analizador correspondiente no aporta diagnosticos a ese nivel), el preview omite esa dimension en lugar de reportar delta cero. Lo que aparece en el preview son los pares `(nivel, dimension)` que efectivamente se evaluan.

</details>

<a id="F-PIPRE-R006"></a>
### Rule[F-PIPRE-R006] - Cada delta del preview lleva tres valores: antes, despues y diferencia
**Severity**: major | **Validation**: AUTO_VALIDATED

> Para cada par `(nivel, dimension)` reportado y para el score agregado de cada nivel, el preview incluye el valor antes (sobre el curso actual), el valor despues (sobre la simulacion con `elementAfter`) y la diferencia entre ambos.

<details><summary>Detail</summary>

1. Reportar antes y despues por separado evita ambiguedad sobre el signo (mejora vs empeora) y deja al consumidor del preview elegir como presentar los valores.
2. Los tres valores del preview son los del dominio (escalas tal como las computa el motor de auditoria, tipicamente entre 0 y 1). La forma en que se presentan al operador en la salida estandar esta normada por [F-PIPRE-R013](#F-PIPRE-R013).
3. Si el valor "antes" no esta disponible (por ejemplo porque el ultimo `AuditReport` no cubre la dimension en ese nivel), el sistema sigue las reglas de [F-PIPRE-R009](#F-PIPRE-R009): no fabrica un cero ficticio.

</details>

---

### Grupo C - Que ve el operador

<a id="F-PIPRE-R007"></a>
### Rule[F-PIPRE-R007] - El preview se muestra al operador junto con la propuesta misma
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando el operador inspecciona una propuesta (modo humano de FEAT-REVAPR, lectura post-hoc del artefacto), el preview de impacto asociado se muestra junto al `elementBefore`/`elementAfter`, no como un recurso separado al que haya que ir a buscar.

<details><summary>Detail</summary>

1. El operador no debe necesitar correr un comando aparte para ver el impacto: la pantalla / output que ya muestra una propuesta debe incluir el preview asociado.
2. La forma concreta de presentacion (tabla, json, listado por nivel) es decision de implementacion mientras se respete la estructura de [F-PIPRE-R004](#F-PIPRE-R004) y [F-PIPRE-R005](#F-PIPRE-R005).
3. Si el preview no esta disponible para una propuesta dada (por las razones de [F-PIPRE-R009](#F-PIPRE-R009)), la propuesta sigue siendo legible: en el lugar del preview aparece la causa de no-disponibilidad, no un error que impida ver la propuesta.

</details>

<a id="F-PIPRE-R008"></a>
### Rule[F-PIPRE-R008] - El preview es de solo lectura una vez asociado a la propuesta
**Severity**: major | **Validation**: AUTO_VALIDATED

> El preview asociado a una propuesta es inmutable: no se reescribe con datos nuevos al volver a leer la propuesta, no se "actualiza" cuando el curso cambia, no se borra al aprobar o rechazar la propuesta.

<details><summary>Detail</summary>

1. La inmutabilidad sigue el mismo principio de trazabilidad que [F-REVAPR-R013](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R013) sobre veredictos: el preview es la foto del impacto en el momento en que se construyo la propuesta y debe quedar como traza historica, aunque despues quede desactualizado.
2. Si el operador necesita un preview "fresco" porque el curso cambio, el camino correcto es generar una propuesta nueva (FEAT-REVAPR R010), que vendra con su propio preview recien calculado. No existe en esta iteracion una operacion explicita de "refrescar el preview".
3. Aprobar o rechazar la propuesta no toca el preview: tras la decision, el preview sigue siendo recuperable como parte del registro de la propuesta.

</details>

<a id="F-PIPRE-R013"></a>
### Rule[F-PIPRE-R013] - Los deltas se presentan al operador en notacion porcentual
**Severity**: major | **Validation**: VALIDATED

> Cuando el preview se muestra al operador, cada uno de los tres valores de un delta (antes, despues, diferencia) se presenta como porcentaje (`72% -> 81% (+9 pp)` o equivalente), no como el valor crudo de la escala interna del dominio.

<details><summary>Detail</summary>

1. Los valores que el preview almacena son los del dominio (escala tipica entre 0 y 1, segun [F-PIPRE-R006](#F-PIPRE-R006)). La conversion a porcentaje es una transformacion de presentacion al operador, no un cambio del modelo subyacente.
2. Aplica a los tres valores de cada delta, a todos los niveles reportados por [F-PIPRE-R004](#F-PIPRE-R004) y a todos los pares `(nivel, dimension)` reportados por [F-PIPRE-R005](#F-PIPRE-R005).
3. La diferencia entre antes y despues se muestra acompañada de signo explicito y unidad apropiada (puntos porcentuales para la diferencia, signos `+` / `-` para distinguir mejora de empeora). El operador no debe tener que inferir el signo ni hacer la cuenta.
4. El estado **no disponible** ([F-PIPRE-R009](#F-PIPRE-R009)) no es un valor numerico y por lo tanto no esta sujeto a esta regla: se sigue mostrando como texto explicativo de la causa.

</details>

---

### Grupo D - Cuando el preview no se puede computar

<a id="F-PIPRE-R009"></a>
### Rule[F-PIPRE-R009] - Si el preview no se puede computar, el sistema lo declara explicitamente con causa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si por algun motivo el sistema no puede producir un preview de impacto (por ejemplo, porque el nodo objetivo no se encuentra en el curso simulado, porque la auditoria base no cubre alguno de los niveles, o porque la simulacion del curso modificado no se puede recomputar), no se debe rellenar con valores arbitrarios. El preview asociado a la propuesta queda marcado como **no disponible** y se registra una causa explicita y legible.

<details><summary>Detail</summary>

1. Casos previstos en los que el preview puede quedar **no disponible**:

   | Causa | Descripcion |
   |-------|-------------|
   | Nodo objetivo ausente en el curso | El `nodeId` referenciado por la propuesta no existe en el curso al momento de simular (por ejemplo porque otra propuesta sobre el mismo nodo se aprobo antes y lo elimino o lo renombro). |
   | Auditoria base no recuperable | El sistema no puede acceder al `AuditReport` que corresponde al estado actual del curso para usarlo como linea base. |
   | Falla de simulacion | La simulacion (curso con `elementAfter` aplicado en memoria) no se puede ejecutar a traves de la auditoria, sea por una falla del motor de auditoria sobre la simulacion o por inconsistencia entre el snapshot del elemento y el resto del curso. |

2. El estado **no disponible** es un valor de primera clase del preview, distinto de "ningun cambio detectado" (esto ultimo significa que el preview se computo y no encontro deltas). El operador debe poder distinguir ambos casos al leer la propuesta.
3. La no-disponibilidad del preview no debe abortar la generacion de la propuesta. La propuesta sigue siendo valida y persiste; solo el preview asociado queda marcado con la causa.
4. La causa registrada es texto legible para el operador. La taxonomia exacta de causas (la lista de la tabla u otra mas granular) y su forma persistida son decision de arquitectura (ver [DOUBT-UNAVAILABILITY-TAXONOMY](#DOUBT-UNAVAILABILITY-TAXONOMY)).

**Error**: "Preview de impacto no disponible para la propuesta '<proposalId>': <causa>"

</details>

<a id="F-PIPRE-R010"></a>
### Rule[F-PIPRE-R010] - Una falla al computar el preview no afecta la persistencia de la propuesta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si el calculo del preview falla, la propuesta misma se persiste igual (cumpliendo FEAT-REVBYP R008/R010 y FEAT-REVAPR R008): el preview no es un componente bloqueante.

<details><summary>Detail</summary>

1. La generacion de la propuesta y la persistencia del artefacto son el flujo critico que hereda este requerimiento. El preview es informacion **adicional** y no debe romper ese flujo si falla.
2. La falla del preview se trata segun [F-PIPRE-R009](#F-PIPRE-R009): preview marcado como no disponible, causa registrada, propuesta persistida normalmente con su veredicto inicial (`PENDING_APPROVAL` en modo humano, `APPROVED` en modo auto).
3. Esta regla es paralela a [F-LAPS-R015](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R015) pero con el signo opuesto: alli, la falla de la **estrategia de propuesta** aborta porque sin candidato no hay propuesta; aqui, la falla del **preview** no aborta porque ya hay propuesta.

</details>

---

### Grupo E - Limites explicitos de esta iteracion

<a id="F-PIPRE-R011"></a>
### Rule[F-PIPRE-R011] - El preview se calcula propuesta por propuesta, no en combinaciones
**Severity**: major | **Validation**: AUTO_VALIDATED

> En esta iteracion el preview de cada propuesta refleja el impacto de aplicar **solo esa propuesta** sobre el curso actual. No se computan previews combinados (impacto de aplicar varias propuestas pendientes a la vez, impacto agregado por plan, etc.).

<details><summary>Detail</summary>

1. Si hay varias propuestas pendientes contra el mismo curso, cada una tiene su propio preview, calculado independientemente sobre el curso actual al momento de su generacion. La superposicion de efectos no se modela.
2. El operador que quiera estimar el efecto agregado debe leer cada preview por separado y tomar su propia decision; el sistema no combina ni suma deltas entre propuestas.
3. La feature de combinar varias propuestas y ver impacto agregado queda explicitamente como evolucion futura ([DOUBT-BATCH-PREVIEW](#DOUBT-BATCH-PREVIEW)).

</details>

<a id="F-PIPRE-R012"></a>
### Rule[F-PIPRE-R012] - El preview cubre solo cambios sobre nodos existentes
**Severity**: minor | **Validation**: AUTO_VALIDATED

> El preview supone que la propuesta sustituye un `elementBefore` por un `elementAfter` sobre un nodo identificado por `nodeId` que existe en el curso (es decir, el patron de FEAT-REVBYP / FEAT-LAPS R014 donde los identificadores estructurales se preservan). No se modela el preview de propuestas que crearian, eliminarian o reordenarian nodos.

<details><summary>Detail</summary>

Esta restriccion no es nueva: es una consecuencia de [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014) y de la forma actual de `RevisionProposal` (FEAT-REVBYP R001), que asume `nodeId` estable. Si una iteracion futura introduce propuestas de creacion / eliminacion / reordenamiento, este requerimiento se revisa.

</details>

---

## Contexto

### El gap funcional

Hoy el operador que corre `revise task <id>` (FEAT-REVBYP, FEAT-REVAPR) recibe una `RevisionProposal` con `elementBefore` y `elementAfter`, pero no tiene forma de saber si aprobarla mejorara los scores de la auditoria. El unico mecanismo disponible es:

1. Aprobar la propuesta (en modo `auto`, esto pasa solo; en modo `human`, requiere `approve proposal <id>`).
2. Ver como queda el curso aplicado.
3. Volver a correr la auditoria desde cero.
4. Comparar manualmente el reporte nuevo con el anterior.

El paso 1 ya **modifica** el curso real, lo cual hace que la decision sea costosa de revertir si el resultado no era el esperado. El operador no tiene una forma barata de "preguntar antes de comprar". En modo humano (FEAT-REVAPR), donde justamente se busca dar al operador la informacion para decidir, esto es especialmente notorio.

### El que y el por que del preview

Este requerimiento llena el gap proveyendo, **al momento mismo** en que se genera la propuesta, un calculo simulado del impacto: que pasaria con los scores si aprobaramos esa propuesta. El operador puede entonces leer el preview asociado y decidir con esa informacion sin tocar el curso real.

El preview es deliberadamente **eager y por propuesta individual**. Eager porque computarlo en el momento de generacion lo asocia 1:1 al `proposalId` y evita el costo de recalcular cada vez que alguien consulta la propuesta. Por propuesta individual porque el caso de uso primario es decidir una propuesta a la vez; combinaciones de varias propuestas se discuten en [DOUBT-BATCH-PREVIEW](#DOUBT-BATCH-PREVIEW) y quedan fuera de alcance.

### Agnostico de la dimension

Aunque el caso disparador es `LEMMA_ABSENCE` (la unica dimension con estrategia real hoy, FEAT-LAPS), el preview es **agnostico de la dimension** del diagnostico que origino la propuesta. Una propuesta de `LEMMA_ABSENCE` puede mover scores de otras dimensiones (por ejemplo, alterar `SENTENCE_LENGTH` si la oracion nueva es mas larga o mas corta), y el preview debe exponer esos efectos cruzados. La feature aplica por igual a cualquier propuesta de revision, sea producto del bypass (FEAT-REVBYP), de FEAT-LAPS o de cualquier estrategia futura para otros `DiagnosisKind`.

---

## Alcance

- **En alcance**: Calculo eager del preview por cada `RevisionProposal` generada. Reporte de deltas por nivel jerarquico (nodo objetivo, knowledge, milestone, topic, curso). Reporte de deltas por dimension de diagnostico. Marcado de no-disponibilidad con causa. Visibilidad del preview junto a la propuesta.
- **Fuera de alcance**: Preview combinado de varias propuestas (batch). Preview "lazy" recomputado cada vez que se consulta. Operacion explicita de "refrescar" el preview de una propuesta vieja. Preview para propuestas que crean / eliminan / reordenan nodos. Operaciones nuevas de CLI especificas para preview (verbos / recursos nuevos): el preview se expone a traves de la lectura existente de la propuesta.

---

## User Journeys

### Journey[F-PIPRE-J001] - El preview se computa y queda asociado al generar la propuesta
**Validation**: AUTO_VALIDATED

Camino feliz: el sistema construye una propuesta, computa el preview de impacto y deja ambos disponibles juntos.

```yaml
journeys:
  - id: F-PIPRE-J001
    name: Preview computado al generar la propuesta
    flow:
      - id: construir_propuesta
        action: "El sistema construye una RevisionProposal sobre una tarea (cualquier diagnosisKind, sea bypass o estrategia real)"
        then: computar_preview
      - id: computar_preview
        action: "El sistema computa el preview de impacto usando el curso actual como base y la simulacion con elementAfter aplicado como what-if"
        gate: [F-PIPRE-R001, F-PIPRE-R002, F-PIPRE-R003]
        outcomes:
          - when: "El preview se computa exitosamente"
            then: persistir_propuesta_con_preview
          - when: "El preview no se puede computar"
            then: persistir_propuesta_sin_preview
      - id: persistir_propuesta_con_preview
        action: "El sistema persiste el artefacto de la propuesta junto con el preview, que reporta deltas por nivel y por dimension"
        gate: [F-PIPRE-R004, F-PIPRE-R005, F-PIPRE-R006, F-PIPRE-R008]
        result: success
      - id: persistir_propuesta_sin_preview
        action: "El sistema persiste el artefacto de la propuesta y deja el preview marcado como 'no disponible' con la causa registrada"
        gate: [F-PIPRE-R009, F-PIPRE-R010]
        result: success
```

### Journey[F-PIPRE-J002] - El operador inspecciona una propuesta y ve su preview
**Validation**: AUTO_VALIDATED

El operador que consulta una propuesta (tipicamente en modo humano de FEAT-REVAPR antes de decidir) ve el preview asociado en la misma operacion de lectura.

```yaml
journeys:
  - id: F-PIPRE-J002
    name: Inspeccion de propuesta con preview asociado
    flow:
      - id: operador_consulta_propuesta
        action: "El operador consulta una propuesta existente (por ejemplo en modo humano antes de decidir)"
        then: leer_preview
      - id: leer_preview
        action: "El sistema recupera la propuesta junto con su preview asociado"
        gate: [F-PIPRE-R007, F-PIPRE-R008]
        outcomes:
          - when: "El preview esta disponible"
            then: mostrar_preview_completo
          - when: "El preview esta marcado como no disponible"
            then: mostrar_no_disponible
      - id: mostrar_preview_completo
        action: "El sistema entrega al operador la propuesta junto con los deltas por nivel y por dimension del preview, presentados en notacion porcentual"
        gate: [F-PIPRE-R004, F-PIPRE-R005, F-PIPRE-R006, F-PIPRE-R013]
        result: success
      - id: mostrar_no_disponible
        action: "El sistema entrega al operador la propuesta junto con la marca de preview no disponible y la causa explicita"
        gate: [F-PIPRE-R009]
        result: success
```

### Journey[F-PIPRE-J003] - Una propuesta sobre un nodo que ya no existe queda con preview no disponible
**Validation**: AUTO_VALIDATED

Cubre el caso explicito de [F-PIPRE-R009](#F-PIPRE-R009): el preview no se puede computar porque la simulacion no localiza el nodo objetivo en el curso al momento de aplicar la propuesta.

```yaml
journeys:
  - id: F-PIPRE-J003
    name: Preview no disponible por nodo ausente
    flow:
      - id: construir_propuesta_sobre_nodo
        action: "El sistema construye una RevisionProposal cuyo nodeId apunta a un nodo del curso"
        then: intentar_preview
      - id: intentar_preview
        action: "El sistema intenta computar el preview de impacto sobre la simulacion del curso"
        gate: [F-PIPRE-R001, F-PIPRE-R002]
        outcomes:
          - when: "El nodo objetivo se localiza en el curso simulado"
            then: preview_ok
          - when: "El nodo objetivo no se localiza en el curso simulado"
            then: preview_no_disponible_por_nodo_ausente
      - id: preview_ok
        action: "El preview se computa con deltas por nivel y por dimension y queda asociado a la propuesta"
        gate: [F-PIPRE-R004, F-PIPRE-R005, F-PIPRE-R006]
        result: success
      - id: preview_no_disponible_por_nodo_ausente
        action: "El sistema marca el preview como no disponible con causa 'nodo objetivo ausente en el curso'; la propuesta se persiste igual con su veredicto inicial"
        gate: [F-PIPRE-R009, F-PIPRE-R010]
        result: success
```

---

## Open Questions

<a id="DOUBT-BASE-COURSE-VERSION"></a>
### Doubt[DOUBT-BASE-COURSE-VERSION] - Sobre que version del curso se computa el preview?
**Status**: RESOLVED (con seguimiento abierto)

[F-PIPRE-R003](#F-PIPRE-R003) elige "el estado del curso al momento de generar la propuesta". Las opciones consideradas:

- [x] Opcion A: Estado del curso al momento de **generar** la propuesta. Preview eager calculado una sola vez. Asociacion 1:1 estable entre `proposalId` y preview. Riesgo: si el curso cambia entre generacion y decision (otra propuesta aprobada antes), el preview queda potencialmente desactualizado y debe marcarse como tal en una iteracion futura.
- [ ] Opcion B: Estado del curso al momento de **consultar** la propuesta (lazy). Preview siempre actual. Costo: recomputar en cada lectura, dificil de cachear, complica la asociacion con el `proposalId`.
- [ ] Opcion C: Hibrido — eager en generacion + recomputo opcional bajo demanda. Mas flexible pero introduce dos rutas de calculo y dos representaciones del preview.

**Answer**: Opcion A. Preview eager al momento de generar la propuesta, asociado al `proposalId`, inmutable ([F-PIPRE-R008](#F-PIPRE-R008)). Si un preview queda obsoleto por cambios posteriores en el curso, el camino correcto es generar una propuesta nueva. La deteccion de "preview obsoleto" como senal explicita queda abierta como evolucion futura (ver [DOUBT-STALENESS-DETECTION](#DOUBT-STALENESS-DETECTION)).

<a id="DOUBT-PRESENTATION-FORMAT"></a>
### Doubt[DOUBT-PRESENTATION-FORMAT] - Como se presenta el delta al operador (porcentaje, absoluto, ambos)?
**Status**: RESOLVED

[F-PIPRE-R006](#F-PIPRE-R006) garantiza que cada delta lleva los tres valores (antes, despues, diferencia). La pregunta era en que forma se muestran al operador en la salida estandar:

- [x] Opcion A: Porcentaje (`72% -> 81% (+9 pp)` o equivalente). Los scores del dominio van entre 0 y 1, asi que la presentacion porcentual es la mas legible para humanos sin perder informacion utilizable.
- [ ] Opcion B: Solo valor crudo entre 0 y 1 (`+0,023`). Descartada: poco amigable para el operador, que es quien debe decidir.
- [ ] Opcion C: Ambos, porcentaje y valor crudo, en ese orden (`+2,3% (+0,023)`). Descartada: redundante; agrega ruido sin informacion adicional para el operador.
- [ ] Opcion D: Decision diferida al consumidor de la CLI. Descartada: deja la decision sin tomar y permite consumidores que muestren la escala interna del dominio (lo que justamente queremos evitar).

**Answer**: Opcion A. Notacion porcentual para los tres valores de cada delta. Los valores subyacentes (los datos del dominio en 0..1) son una decision de modelado interna del arquitecto y no se ven en la salida estandar al operador. La regla [F-PIPRE-R013](#F-PIPRE-R013) hace esta presentacion testeable de forma directa.

<a id="DOUBT-UNAVAILABILITY-TAXONOMY"></a>
### Doubt[DOUBT-UNAVAILABILITY-TAXONOMY] - Que taxonomia exacta de causas de no-disponibilidad se persiste?
**Status**: OPEN (para arquitecto)

[F-PIPRE-R009](#F-PIPRE-R009) lista tres causas previstas (nodo objetivo ausente, auditoria base no recuperable, falla de simulacion) pero deja abierta la forma exacta de la taxonomia y como se persiste:

- [ ] Opcion A: Texto libre como causa, sin enum. Maxima flexibilidad, dificil de filtrar / agrupar.
- [ ] Opcion B: Enum cerrado de causas con un texto explicativo opcional. Filtrable, requiere mantener el enum sincronizado.
- [ ] Opcion C: Categoria principal (enum) + detalle (texto libre), analogo a la taxonomia de fallas de FEAT-LAGEN R006.

**Answer**: Pendiente. La regla funcional solo exige que la causa sea legible. Estructura interna es decision de arquitectura.

<a id="DOUBT-PROPOSAL-LIFECYCLE"></a>
### Doubt[DOUBT-PROPOSAL-LIFECYCLE] - Cada propuesta tiene su propio preview, incluso despues de un rechazo previo sobre la misma tarea?
**Status**: RESOLVED

Si una propuesta se rechaza (FEAT-REVAPR R012) y luego se vuelve a invocar `revise task <id>` para esa misma tarea (lo cual produce una propuesta nueva con `proposalId` distinto), debe la propuesta nueva tener su propio preview, independiente del de la propuesta rechazada?

- [x] Opcion A: Si. Cada `RevisionProposal` lleva su propio preview, calculado en el momento de su generacion. La propuesta vieja conserva el suyo (inmutable, [F-PIPRE-R008](#F-PIPRE-R008)) como traza historica.
- [ ] Opcion B: Reusar el preview de la propuesta vieja si el `nodeId` y el `elementBefore` coinciden. Optimizacion no justificada en esta iteracion.

**Answer**: Opcion A. Por [F-PIPRE-R001](#F-PIPRE-R001), la asociacion preview ↔ proposalId es 1:1 y no se reutiliza entre propuestas distintas, aunque sean para la misma tarea.

<a id="DOUBT-BATCH-PREVIEW"></a>
### Doubt[DOUBT-BATCH-PREVIEW] - Combinar varias propuestas y ver el impacto agregado (feature.future)
**Status**: OPEN (feature.future)

El usuario anticipo este caso: tener varias propuestas pendientes y poder pedir el preview agregado de aplicarlas todas juntas (en algun orden) sobre el curso. Ejemplos de uso:

- "Si apruebo todas las propuestas pendientes del plan X, como queda el score global?"
- "Cual es el conjunto minimo de propuestas que llevan el score por encima de un umbral?"

Esta iteracion **no** lo cubre ([F-PIPRE-R011](#F-PIPRE-R011)). Razones:

1. Modelar correctamente la composicion de propuestas requiere decidir orden de aplicacion, manejo de conflictos (dos propuestas sobre el mismo nodo) y representacion del preview agregado.
2. El caso de uso primario hoy es decidir una propuesta a la vez; el agregado es exploratorio.

**Answer**: Pendiente, fuera de alcance en esta iteracion. Se trata como feature.future, a abordar en un requerimiento posterior una vez consolidado el preview por propuesta individual.

<a id="DOUBT-STALENESS-DETECTION"></a>
### Doubt[DOUBT-STALENESS-DETECTION] - Como se senaliza al operador que un preview eager quedo desactualizado?
**Status**: OPEN (feature.future)

[F-PIPRE-R003](#F-PIPRE-R003) y [F-PIPRE-R008](#F-PIPRE-R008) admiten que el preview puede quedar desactualizado si entre generacion y decision el curso cambia (por ejemplo otra propuesta sobre un nodo cercano fue aprobada antes). Esta iteracion no detecta automaticamente ese caso. La pregunta es como deberia hacerlo en el futuro:

- [ ] Opcion A: Anclar el preview a un `auditId` o un hash del curso al momento de la generacion; comparar contra el actual al consultarlo y marcar "obsoleto" si difiere.
- [ ] Opcion B: Recalcular el preview bajo demanda y comparar con el almacenado; mostrar el delta del delta.
- [ ] Opcion C: Solo comparar el `nodeId` de la propuesta y los nodos cercanos modificados; senalar "potencialmente afectado" sin afirmar nada.

**Answer**: Pendiente, fuera de alcance en esta iteracion. Se trata como feature.future. Hoy [F-PIPRE-R009](#F-PIPRE-R009) cubre el caso extremo (nodo ausente o simulacion imposible); la deteccion mas fina de obsolescencia llega despues.

<a id="DOUBT-PREVIEW-CLI-EXPOSURE"></a>
### Doubt[DOUBT-PREVIEW-CLI-EXPOSURE] - El preview se expone como recurso propio en la CLI?
**Status**: OPEN (para arquitecto)

[F-PIPRE-R007](#F-PIPRE-R007) pide que el preview se vea junto a la propuesta en la lectura actual (FEAT-REVAPR R002 sobre el recurso `proposal`). Queda abierto si tambien conviene un recurso propio (`get preview <proposalId>`, `get previews --plan <id>`):

- [ ] Opcion A: Solo via la lectura de la propuesta. Sin verbo / recurso nuevo.
- [ ] Opcion B: Recurso `preview` de primera clase, listable y filtrable, analogo a `proposal` de FEAT-REVAPR R002.

**Answer**: Pendiente. La regla funcional solo exige visibilidad junto a la propuesta. Un recurso propio es decision de arquitectura / UX y puede esperar a una iteracion posterior.

---

## References

- **FEAT-REVBYP** — Define la `RevisionProposal` y el flujo de generacion / persistencia base. Citado por [F-PIPRE-R001](#F-PIPRE-R001), [F-PIPRE-R004](#F-PIPRE-R004), [F-PIPRE-R010](#F-PIPRE-R010), [F-PIPRE-R012](#F-PIPRE-R012).
- **FEAT-REVAPR** — Aporta el modo de aprobacion humana sobre propuestas pendientes; es el contexto principal en el que el operador consume el preview antes de decidir. Citado por [F-PIPRE-R001](#F-PIPRE-R001), [F-PIPRE-R007](#F-PIPRE-R007), [F-PIPRE-R008](#F-PIPRE-R008), [F-PIPRE-R010](#F-PIPRE-R010).
- **FEAT-LAPS** — Estrategia real de propuesta para `LEMMA_ABSENCE`; primer caso de uso real del preview, aunque la feature es agnostica de la dimension. Citado por [F-PIPRE-R010](#F-PIPRE-R010), [F-PIPRE-R012](#F-PIPRE-R012).
- **FEAT-LAGEN** — Aporta una taxonomia de causas de falla en el dominio de generacion, util como modelo para [DOUBT-UNAVAILABILITY-TAXONOMY](#DOUBT-UNAVAILABILITY-TAXONOMY).

---

## ASSUMPTIONS

1. **El motor de auditoria puede recomputar scores sobre una version del curso en memoria sin emitir un `AuditReport` oficial.** [F-PIPRE-R002](#F-PIPRE-R002) asume que existe (o se construira) una via para correr la auditoria sobre una variante simulada del curso sin que esa corrida produzca registros oficiales (nuevo `auditId`, nuevo plan derivado, etc.). Si no existe, el arquitecto debe definir como aislar la corrida simulada de la oficial.
2. **El estado base contra el que se calculan los deltas es el ultimo `AuditReport` valido del curso.** [F-PIPRE-R003](#F-PIPRE-R003) y [F-PIPRE-R006](#F-PIPRE-R006) asumen que el sistema puede ubicar inequivocamente la auditoria base correspondiente al estado actual del curso. Si esa correspondencia no se puede establecer (por ejemplo, porque el curso cambio sin auditarse), el preview cae en [F-PIPRE-R009](#F-PIPRE-R009) como "auditoria base no recuperable".
3. **Las dimensiones reportadas en el preview son las mismas dimensiones que aporta el motor de auditoria al construir el `AuditReport` (`LEMMA_ABSENCE`, `SENTENCE_LENGTH`, `COCA_BUCKETS`, `LEMMA_RECURRENCE`, etc.).** No se inventan dimensiones nuevas para el preview ni se ocultan dimensiones existentes. La inclusion de una dimension en el preview de un nivel sigue exactamente el criterio que el motor usa para incluirla en el `AuditReport`.
4. **La identidad del nodo objetivo es estable entre `elementBefore` y `elementAfter`.** Heredado de [F-LAPS-R014](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R014). El preview compara scores en torno al mismo `nodeId` antes y despues; no modela renombres ni reubicaciones del nodo. Cubierto explicitamente por [F-PIPRE-R012](#F-PIPRE-R012).
5. **El preview se persiste asociado a la propuesta de forma recuperable junto con ella.** [F-PIPRE-R007](#F-PIPRE-R007) asume que existe una via para que la lectura actual de la propuesta (la que ya consume el operador, herencia de FEAT-REVAPR R002) entregue el preview asociado, sin requerir un store paralelo que el operador deba consultar aparte. La forma concreta de persistencia (mismo archivo del artefacto, archivo hermano `<proposalId>.preview.*`, etc.) es decision de arquitectura.
