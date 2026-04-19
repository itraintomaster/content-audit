---
feature:
  id: FEAT-REVBYP
  code: F-REVBYP
  name: Fase de revision (bypass skeleton) - aplicacion de cambios a elementos del curso
  priority: critical
---

# Fase de revision (bypass skeleton) - aplicacion de cambios a elementos del curso

Hoy la pipeline de ContentAudit cubre tres fases: `analyze` produce un reporte de auditoria, `refiner plan` genera un plan de tareas a partir del reporte, y `refiner next` resuelve el contexto de correccion de la proxima tarea. Lo que falta es la fase que efectivamente **aplica** un cambio a un elemento del curso en base a ese contexto.

Este requerimiento define esa fase, llamada **revision**. El alcance es deliberadamente minimo: se construye el esqueleto end-to-end con componentes **bypass** (identidad, auto-aprobacion) para validar que la pipeline completa funciona — que una tarea se puede tomar, construir su propuesta de revision, persistirla como artefacto auditable, aprobarla, y reescribir el curso modificado al disco. Ninguna logica real de revision (AI, reglas, transformaciones) entra en esta iteracion. Esas estrategias se enchufaran en requerimientos futuros.

## Contexto

### La pipeline antes y despues

Antes:

```
analyze -> refiner plan -> refiner next (resuelve CorrectionContext)
```

Despues de este requerimiento:

```
analyze -> refiner plan -> refiner next -> REVISION -> curso modificado en ./db/english-course/
```

La fase de revision recibe una tarea de refinamiento (identificada por su `taskId` dentro del plan vigente), construye una propuesta de cambio sobre el elemento del curso asociado a esa tarea, la persiste como artefacto, la valida y la aplica al curso en disco.

### Relacion con features existentes

- **FEAT-RCSL** y **FEAT-RCLA**: proveen el `CorrectionContext` que describe **que** hay que corregir en un quiz (la oracion actual, el diagnostico, los lemas sugeridos o fuera de nivel). La fase de revision **consume** ese contexto como insumo para generar la propuesta. No se modifica la forma en que se construye el contexto.
- **FEAT-CSTRUCT** (estructura del curso): provee el modelo de elementos del curso (`QuizTemplateEntity`, `MilestoneEntity`, etc.) y el puerto `CourseRepository` que sabe cargar y persistir el curso desde/hacia `./db/english-course/`. La fase de revision reutiliza ese puerto para escribir el curso modificado.
- **Refiner plan / refiner next**: estado de la tarea (`PENDING`, `IN_PROGRESS`, `DONE`, etc.) y la persistencia del plan estan definidos en esos features. La fase de revision interactua con el estado de la tarea al aplicarla, pero la mecanica de avance dentro del plan no se redefine aqui.

### Alcance deliberado (base case)

Esta iteracion es una **prueba de pipeline**, no una prueba de correccion de contenido. Por eso todos los componentes activos son bypass:

- **Reviser bypass**: dado el elemento actual, devuelve el mismo elemento sin modificaciones (revision identidad).
- **Validator bypass**: cualquier propuesta se aprueba automaticamente.
- **Artefacto de propuesta**: aun asi se persiste, porque lo que se esta validando es que la cadena de persistencia funciona y queda trazable.
- **Curso escrito a disco**: aun asi se reescribe, porque la idempotencia del repositorio (FEAT-CSTRUCT) asegura que un curso escrito tal cual es semanticamente equivalente al original. Esto prueba que la ruta de escritura funciona, aunque el contenido no cambie.

Cualquier logica real (un Reviser AI-backed, validadores especificos por DiagnosisKind, analisis de impacto, rollback sobre fallas parciales) queda **fuera** de esta iteracion y se trata en requerimientos futuros.

### Actor principal

El usuario que ejecuta la revision es el **operador del sistema** (tipicamente desde la CLI). No hay LLM ni integraciones externas en el base case: la revision es sincronica, bypass, y termina con el curso modificado (identico) persistido.

---

## Reglas de Negocio

### Grupo A - Modelo de la propuesta de revision

### Rule[F-REVBYP-R001] - Estructura de la RevisionProposal
**Severity**: critical | **Validation**: AUTO_VALIDATED

Para cada tarea de revision, el sistema debe poder construir una `RevisionProposal` que representa el cambio propuesto sobre un elemento del curso. La propuesta contiene los siguientes datos:

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| proposalId | Texto | Identificador unico de la propuesta (generado por el sistema) |
| taskId | Texto | Identificador de la `RefinementTask` que origino la revision |
| planId | Texto | Identificador del `RefinementPlan` al que pertenece la tarea |
| sourceAuditId | Texto | Identificador del `AuditReport` del que se derivo el plan |
| diagnosisKind | Enum | Tipo de diagnostico de la tarea (SENTENCE_LENGTH, LEMMA_ABSENCE, etc.) |
| nodeTarget | Enum | Nivel del nodo objetivo (QUIZ, KNOWLEDGE, TOPIC, MILESTONE, COURSE) |
| nodeId | Texto | Identificador del nodo objetivo en el arbol de auditoria |
| elementBefore | Snapshot del elemento | Estado actual del elemento del curso antes de la revision |
| elementAfter | Snapshot del elemento | Estado propuesto del elemento del curso despues de la revision |
| rationale | Texto | Justificacion textual de la revision (puede ser una cadena fija en el caso bypass, e.g., "bypass: identity revision") |
| reviserKind | Texto | Identificador del Reviser que produjo la propuesta (en esta iteracion siempre "bypass") |
| createdAt | Fecha/hora | Timestamp de creacion de la propuesta |

El `elementBefore` y `elementAfter` son capturas del elemento del curso afectado (por ejemplo, un quiz). Sirven para trazabilidad, diff, y reproduccion de la revision. [ASSUMPTION] La forma concreta del snapshot (documento completo, subset relevante) es una decision de arquitectura. Razon: la propuesta debe ser auto-contenida para poder re-ejecutarla o auditarla sin depender del estado actual del curso.

**Error**: N/A (esta regla define la estructura de un registro)

### Rule[F-REVBYP-R002] - En el caso bypass, elementBefore y elementAfter son semanticamente equivalentes
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el Reviser activo es el bypass (identidad), el `elementAfter` de la propuesta debe ser semanticamente equivalente al `elementBefore`. Semanticamente equivalente significa que, luego de aplicar la propuesta y releer el curso desde disco, el elemento afectado es indistinguible del original segun los criterios de igualdad definidos en FEAT-CSTRUCT (idempotencia de persistencia, R003).

**Error**: N/A (esta regla define una condicion sobre el contenido)

---

### Grupo B - Pluggabilidad del Reviser

### Rule[F-REVBYP-R003] - El Reviser es pluggable por DiagnosisKind
**Severity**: critical | **Validation**: AUTO_VALIDATED

El componente que genera una `RevisionProposal` a partir de un `CorrectionContext` y el elemento actual se llama **Reviser**. El sistema debe permitir registrar multiples Revisers, cada uno asociado a uno o mas `DiagnosisKind`. Al iniciar una revision, se selecciona el Reviser correspondiente al `diagnosisKind` de la tarea.

Este patron es analogo al de `CorrectionContextResolver` / `DispatchingCorrectionContextResolver` en el refiner: un despachador central consulta a sus Revisers registrados y delega en el que maneja el diagnostico correspondiente.

**Error**: N/A (esta regla define un patron estructural)

### Rule[F-REVBYP-R004] - Existe un Reviser bypass que actua como baseline por defecto
**Severity**: critical | **Validation**: AUTO_VALIDATED

Debe existir un Reviser llamado **bypass** que:

1. Acepta tareas de cualquier `DiagnosisKind`.
2. Devuelve una `RevisionProposal` cuyo `elementAfter` es identico al `elementBefore` (revision identidad).
3. Produce un `rationale` constante (por ejemplo, "bypass: identity revision").
4. Se usa como Reviser por defecto cuando no hay un Reviser especifico registrado para un `DiagnosisKind`, o cuando se selecciona explicitamente.

En esta iteracion, el Reviser bypass es el unico registrado. El despachador lo elige siempre.

**Error**: N/A (esta regla define el comportamiento de una implementacion baseline)

### Rule[F-REVBYP-R005] - Si no hay Reviser aplicable, la revision no se realiza
**Severity**: major | **Validation**: AUTO_VALIDATED

Si el despachador no encuentra ningun Reviser que maneje el `DiagnosisKind` de la tarea (ni siquiera el bypass), el sistema debe informar que la revision no puede realizarse y no modificar ni la propuesta, ni la tarea, ni el curso en disco.

En el base case esto no ocurre (el bypass acepta todo), pero la regla queda declarada para iteraciones futuras donde los Revisers especificos puedan no cubrir todos los diagnosticos.

**Error**: "No hay Reviser registrado para el diagnostico '{diagnosisKind}'"

---

### Grupo C - Validacion y aprobacion

### Rule[F-REVBYP-R006] - El Validator decide si una propuesta se aplica
**Severity**: critical | **Validation**: AUTO_VALIDATED

Antes de aplicarse al curso, una `RevisionProposal` pasa por un **RevisionValidator**. El validator examina la propuesta y devuelve uno de dos veredictos:

- **APPROVED**: la propuesta puede aplicarse al curso.
- **REJECTED**: la propuesta no se aplica; se registra el motivo en la propuesta y el curso queda sin modificar.

**Error**: N/A (esta regla define un paso de control de flujo)

### Rule[F-REVBYP-R007] - Existe un Validator bypass que auto-aprueba toda propuesta
**Severity**: critical | **Validation**: AUTO_VALIDATED

Debe existir un validator llamado **bypass** cuyo veredicto es siempre APPROVED. En esta iteracion es el unico validator registrado y se usa por defecto.

**Error**: N/A (esta regla define el comportamiento de una implementacion baseline)

---

### Grupo D - Persistencia del artefacto de propuesta

### Rule[F-REVBYP-R008] - Cada propuesta se persiste como artefacto bajo .content-audit/revisions/
**Severity**: critical | **Validation**: AUTO_VALIDATED

Toda `RevisionProposal` generada (sea aprobada o rechazada) debe persistirse como un artefacto en el workdir de la aplicacion, bajo el subdirectorio `.content-audit/revisions/`. La persistencia del artefacto es parte esencial del flujo: ocurre **aunque** el Reviser sea bypass y el contenido no cambie, porque el objetivo del artefacto es dejar trazabilidad del intento de revision.

[ASSUMPTION] El directorio `.content-audit/` es el workdir de la aplicacion (contiene ya `audits/` y `plans/`). No debe confundirse con `.sentinel/`, que es el directorio del framework. Razon: mantener la separacion entre artefactos generados por el framework y artefactos generados por el dominio.

**Error**: "No se pudo persistir la propuesta de revision '{proposalId}' bajo .content-audit/revisions/"

### Rule[F-REVBYP-R009] - Organizacion de los artefactos de propuesta
**Severity**: major | **Validation**: AUTO_VALIDATED

Los artefactos de propuesta se organizan dentro de `.content-audit/revisions/` de forma que sean facilmente navegables por plan y por tarea. La estructura concreta es:

```
.content-audit/revisions/
  <planId>/
    <proposalId>.<ext>
```

Donde:

- `<planId>` agrupa todas las propuestas derivadas del mismo `RefinementPlan`.
- `<proposalId>` identifica de forma unica la propuesta. La convencion es `<taskId>-<timestamp>` para permitir que una misma tarea genere multiples propuestas en ejecuciones sucesivas (por ejemplo, si el operador re-ejecuta la revision con un Reviser distinto). La iteracion actual no aprovecha esa repeticion, pero el formato lo habilita.

La extension concreta del archivo (`.json`, `.yaml`, etc.) queda como decision de arquitectura.

**Error**: N/A (esta regla define una convencion de organizacion de archivos)

### Rule[F-REVBYP-R010] - El artefacto es suficiente para reconstruir la decision
**Severity**: major | **Validation**: AUTO_VALIDATED

El contenido del artefacto persistido debe incluir, como minimo, toda la informacion de la `RevisionProposal` (R001) mas el veredicto del validator (APPROVED / REJECTED) y el motivo del rechazo si corresponde. Un lector del artefacto debe poder responder sin consultar otras fuentes:

1. Que tarea se intento corregir.
2. Que plan y auditoria la originaron.
3. Cual era el estado del elemento antes y el propuesto despues.
4. Quien (que Reviser) produjo la propuesta.
5. Si se aprobo o no, y por que.
6. Cuando se genero.

**Error**: N/A (esta regla define el contenido minimo del artefacto)

---

### Grupo E - Aplicacion al curso

### Rule[F-REVBYP-R011] - Una propuesta aprobada se aplica al curso en disco
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el validator aprueba una propuesta, el sistema debe aplicarla al curso en disco. Aplicar significa:

1. Cargar el curso actual desde `./db/english-course/` usando `CourseRepository` (FEAT-CSTRUCT).
2. Sustituir el elemento identificado por `nodeId` / `nodeTarget` por el `elementAfter` de la propuesta.
3. Persistir el curso modificado de vuelta a `./db/english-course/` usando `CourseRepository`.

En el caso bypass, como el `elementAfter` es identico al `elementBefore`, el curso escrito es semanticamente equivalente al original. Esto no elimina el paso de escritura: la pipeline ejecuta el write-back de todas formas.

**Error**: "No se pudo aplicar la propuesta de revision '{proposalId}' al curso"

### Rule[F-REVBYP-R012] - Una propuesta rechazada no modifica el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el validator rechaza una propuesta, el sistema **no** debe cargar ni escribir el curso. El artefacto queda persistido con el veredicto REJECTED (R010) y el curso en disco permanece sin modificaciones.

**Error**: N/A (esta regla define una condicion negativa de ejecucion)

### Rule[F-REVBYP-R013] - El estado de la tarea refleja el resultado de la revision
**Severity**: major | **Validation**: AUTO_VALIDATED

Al finalizar el flujo de revision, el estado de la `RefinementTask` correspondiente debe actualizarse segun el resultado:

- Si la propuesta fue aprobada y aplicada al curso con exito, la tarea queda marcada como completada (estado DONE del modelo existente en el refiner).
- Si la propuesta fue rechazada por el validator, la tarea permanece en su estado previo (tipicamente PENDING).
- Si ocurrio una falla al aplicar la propuesta al curso (ver R014), la tarea permanece en su estado previo.

[ASSUMPTION] El nombre exacto del estado "DONE" y la mecanica del cambio de estado dependen del modelo existente del refiner. Razon: esta regla describe la intencion funcional (la tarea queda cerrada cuando la revision tuvo efecto), no el detalle del estado.

**Error**: N/A (esta regla define una transicion de estado)

### Rule[F-REVBYP-R014] - El artefacto se persiste antes de aplicar al curso
**Severity**: major | **Validation**: AUTO_VALIDATED

El orden del flujo es: (1) generar propuesta, (2) validar, (3) **persistir el artefacto**, (4) aplicar al curso si fue aprobada. El artefacto queda persistido aunque la aplicacion al curso falle. Esto garantiza que siempre hay un registro del intento de revision, incluso si el write-back del curso falla.

Si la escritura del curso falla despues de que el artefacto ya fue persistido, queda una inconsistencia temporal: el artefacto declara APPROVED + aplicacion intentada, pero el curso no refleja el cambio. En esta iteracion esa inconsistencia se acepta como conocida; la recuperacion/rollback es un tema para una iteracion futura (ver DOUBT-ATOMICITY).

**Error**: "La propuesta '{proposalId}' fue aprobada y persistida como artefacto, pero la escritura del curso fallo"

---

## User Journeys

### Journey[F-REVBYP-J001] - Revision bypass end-to-end de una tarea
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-REVBYP-J001
    name: Revision bypass end-to-end de una tarea
    flow:
      - id: iniciar_revision
        action: "El operador inicia una revision para una tarea del plan identificada por su taskId"
        then: resolver_contexto

      - id: resolver_contexto
        action: "El sistema resuelve el CorrectionContext de la tarea usando la infraestructura existente (FEAT-RCSL / FEAT-RCLA)"
        outcomes:
          - when: "El contexto se resuelve correctamente"
            then: cargar_elemento
          - when: "El contexto no puede resolverse (reporte de auditoria no encontrado, diagnostico no disponible)"
            then: abortar_sin_contexto

      - id: cargar_elemento
        action: "El sistema carga el elemento actual del curso identificado por nodeTarget/nodeId usando CourseRepository"
        outcomes:
          - when: "El elemento existe en el curso cargado"
            then: elegir_reviser
          - when: "El elemento no se encuentra en el curso"
            then: abortar_elemento_no_encontrado

      - id: elegir_reviser
        action: "El despachador selecciona el Reviser para el diagnosisKind de la tarea. En esta iteracion siempre resuelve al Reviser bypass"
        gate: [F-REVBYP-R003, F-REVBYP-R004, F-REVBYP-R005]
        outcomes:
          - when: "Hay un Reviser aplicable (bypass como fallback)"
            then: generar_propuesta
          - when: "No hay ningun Reviser registrado para el diagnostico"
            then: abortar_sin_reviser

      - id: generar_propuesta
        action: "El Reviser bypass genera una RevisionProposal donde elementAfter es igual a elementBefore, con rationale 'bypass: identity revision'"
        gate: [F-REVBYP-R001, F-REVBYP-R002, F-REVBYP-R004]
        then: validar_propuesta

      - id: validar_propuesta
        action: "El RevisionValidator evalua la propuesta"
        gate: [F-REVBYP-R006, F-REVBYP-R007]
        outcomes:
          - when: "El validator aprueba la propuesta (bypass siempre aprueba)"
            then: persistir_artefacto_aprobado
          - when: "El validator rechaza la propuesta"
            then: persistir_artefacto_rechazado

      - id: persistir_artefacto_aprobado
        action: "El sistema escribe el artefacto de la propuesta bajo .content-audit/revisions/<planId>/<proposalId> con veredicto APPROVED"
        gate: [F-REVBYP-R008, F-REVBYP-R009, F-REVBYP-R010, F-REVBYP-R014]
        then: aplicar_curso

      - id: aplicar_curso
        action: "El sistema sustituye el elemento en el curso cargado por elementAfter y persiste el curso via CourseRepository a ./db/english-course/"
        gate: [F-REVBYP-R011]
        outcomes:
          - when: "La escritura del curso fue exitosa"
            then: marcar_tarea_completada
          - when: "La escritura del curso fallo"
            then: fallo_aplicacion

      - id: marcar_tarea_completada
        action: "El sistema actualiza el estado de la RefinementTask a completado"
        gate: [F-REVBYP-R013]
        result: success

      - id: persistir_artefacto_rechazado
        action: "El sistema escribe el artefacto con veredicto REJECTED y el motivo; el curso en disco no se toca"
        gate: [F-REVBYP-R008, F-REVBYP-R010, F-REVBYP-R012, F-REVBYP-R013]
        result: failure

      - id: fallo_aplicacion
        action: "El artefacto queda persistido como APPROVED pero el curso no pudo reescribirse; la tarea permanece en su estado previo"
        gate: [F-REVBYP-R014]
        result: failure

      - id: abortar_sin_contexto
        action: "El sistema informa que la revision no puede iniciarse porque el contexto de correccion no esta disponible"
        result: failure

      - id: abortar_elemento_no_encontrado
        action: "El sistema informa que el elemento objetivo no existe en el curso cargado"
        result: failure

      - id: abortar_sin_reviser
        action: "El sistema informa que no hay Reviser registrado para el diagnostico"
        gate: [F-REVBYP-R005]
        result: failure
```

---

## Open Questions

### Doubt[DOUBT-BYPASS-WRITE] - El Reviser bypass debe igual escribir el curso a disco?
**Status**: RESOLVED

Si el Reviser bypass produce `elementAfter == elementBefore`, tiene sentido igual ejecutar el write-back del curso al disco? Opciones consideradas:

- [ ] Opcion A: Saltar el write-back cuando before == after. Evita I/O innecesario.
- [x] Opcion B: Ejecutar el write-back igual. Lo que se esta validando es la **pipeline**, no el contenido.

**Answer**: Opcion B. El proposito del bypass es validar que la ruta de persistencia del curso funciona end-to-end. Saltar el write-back por optimizacion dejaria esa ruta sin ejercicio. La idempotencia de `CourseRepository` (FEAT-CSTRUCT R003) garantiza que reescribir un curso tal cual es inocuo.

### Doubt[DOUBT-ATOMICITY] - Que pasa si el artefacto se persiste pero el curso falla al escribirse?
**Status**: OPEN (fuera de alcance en esta iteracion)

La regla R014 fija el orden: primero se persiste el artefacto, despues se aplica al curso. Si la aplicacion al curso falla, queda un artefacto APPROVED sin efecto real sobre el curso.

Opciones para iteraciones futuras:

- [ ] Opcion A: Rollback del artefacto si falla la escritura del curso. Requiere diseno transaccional.
- [x] Opcion B: Dejar el artefacto con un estado adicional "APPLICATION_FAILED" y no reintentar automaticamente. Se inspecciona a mano.
- [ ] Opcion C: Reintentar la aplicacion un numero acotado de veces antes de marcar como fallo.

**Answer**: En esta iteracion se declara como limitacion conocida. R014 deja el artefacto en estado APPROVED + escritura intentada + fallo reportado. La tarea permanece sin cerrar. La decision concreta se toma en un requerimiento futuro cuando exista logica de revision real.

### Doubt[DOUBT-CLI-SURFACE] - Agregar un comando `refiner revise <taskId>` en esta iteracion?
**Status**: RESOLVED

La fase de revision necesita un punto de entrada. La CLI ya tiene `refiner plan` y `refiner next`. Un comando natural seria `refiner revise <taskId>` que dispare el flujo descripto en J001.

- [x] Opcion A: Incluir el comando CLI en esta iteracion. Sin un punto de entrada no hay forma end-to-end de validar la pipeline.
- [ ] Opcion B: Posponerlo a una iteracion siguiente. Dejar solo el componente de dominio en este requerimiento.

**Answer**: Opcion A. El objetivo de esta iteracion es probar la pipeline end-to-end. Sin un disparador, no hay "end-to-end" observable. El comando concreto y su formato son decisiones de arquitectura/UX, pero la existencia de **algun** punto de entrada operable por el usuario es parte del alcance funcional.

### Doubt[DOUBT-MULTIPLE-REVISIONS] - Puede una misma tarea generar multiples propuestas?
**Status**: RESOLVED

La convencion de `<proposalId> = <taskId>-<timestamp>` (R009) habilita tecnicamente que una misma tarea acumule propuestas en ejecuciones sucesivas.

- [x] Opcion A: Permitir multiples propuestas por tarea. Los artefactos se acumulan, la ultima aprobada manda sobre el curso.
- [ ] Opcion B: Una sola propuesta por tarea. Re-ejecutar sobreescribe el artefacto anterior.

**Answer**: Opcion A. No se fuerza unicidad de propuesta por tarea. En esta iteracion no se explota (el bypass produce siempre la misma propuesta), pero futuras iteraciones con distintos Revisers se beneficiaran de poder comparar propuestas sucesivas de una misma tarea. El ultimo artefacto APPROVED es el que refleja el estado actual del curso.

### Doubt[DOUBT-SNAPSHOT-SHAPE] - Forma concreta de elementBefore / elementAfter
**Status**: OPEN

Los campos `elementBefore` y `elementAfter` de la propuesta (R001) representan el estado del elemento. Opciones:

- [x] Opcion A: Snapshot completo del elemento (por ejemplo, el `QuizTemplateEntity` entero).
- [ ] Opcion B: Solo el subset de campos que la propuesta modifica (util para diffs chicos).
- [ ] Opcion C: Representacion generica aplicable a cualquier `NodeKind` del curso.

**Answer**: Pendiente para arquitectura. Funcionalmente la regla pide "suficiente para reconstruir la decision" (R010); la forma concreta se deja para que el arquitecto la defina alineada con los tipos ya existentes en `course-domain`.

---

## Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio. Se levantaran en iteraciones futuras:

- **Sin Reviser real**: solo existe el Reviser bypass (identidad). No hay integracion con LLMs ni logica especifica por `DiagnosisKind`. R003 habilita la extension; la extension no esta en alcance aqui.
- **Sin Validator real**: solo existe el validator bypass (auto-aprueba todo). Validaciones semanticas, de coherencia con el curso, o de impacto cruzado con otros diagnosticos quedan para iteraciones futuras.
- **Sin atomicidad entre artefacto y curso**: si la escritura del curso falla despues de persistir el artefacto, queda inconsistencia documentada pero no hay rollback ni retry automatico (ver DOUBT-ATOMICITY).
- **Sin reevaluacion del curso post-revision**: aplicar la revision no dispara una nueva `analyze`. El operador decide cuando volver a auditar.
- **Solo QUIZ como target realista**: aunque el modelo contempla cualquier `nodeTarget`, el caso de uso observable en esta iteracion es la revision de quizzes (porque las tareas LEMMA_ABSENCE y SENTENCE_LENGTH apuntan a QUIZ). Revisar un MILESTONE o un COURSE no tiene semantica definida en el base case.
- **Sin diff legible en el artefacto**: el artefacto incluye `elementBefore` y `elementAfter` como snapshots, pero no un diff calculado legible. Una vista de diff es un nice-to-have futuro.
