---
feature:
  id: FEAT-KTLR
  code: F-KTLR
  name: Revision de longitud de titulo de knowledge con propuesta del agente
  priority: high
---

# Revision de longitud de titulo de knowledge con propuesta del agente

## TL;DR

**Que**: Cierra el puente revise→propuesta→aprobar para tareas de diagnostico
`KNOWLEDGE_TITLE_LENGTH`: el sistema resuelve el contexto del knowledge, invoca
al agente de titulos, transforma su salida (nuevo titulo + instructions) en una
propuesta revisable y, al aprobarla, **persiste el nuevo titulo e instructions
en el knowledge** del curso.

**Por que**: Hoy `plan` ya emite estas tareas y el agente ya existe, pero al
aprobar una propuesta el sistema solo sabe escribir cambios sobre quizzes; una
correccion de titulo de knowledge nunca llega al curso. Esta feature elimina ese
bloqueo sin tocar el flujo de revision de quizzes ya existente.

## Business Rules

Las reglas se organizan en cuatro grupos:

- **Grupo A - Resolucion de contexto** (R001, R002): que informacion del
  knowledge se entrega al agente.
- **Grupo B - Propuesta a partir del agente** (R003, R004, R005): como la salida
  del agente se transforma en una propuesta revisable.
- **Grupo C - Persistencia al aprobar** (R006, R007, R008, R009): el bloqueo
  principal — aprobar escribe titulo e instructions en el knowledge y preserva
  intacto el flujo de quizzes.
- **Grupo D - Manejo de fallas** (R010, R011): outcomes observables ante fallas
  del agente, salida invalida o knowledge inexistente.

---

### Grupo A - Resolucion del contexto del knowledge

<a id="F-KTLR-R001"></a>
### Rule[F-KTLR-R001] - Las tareas KNOWLEDGE_TITLE_LENGTH resuelven el contexto del knowledge
**Severity**: high | **Validation**: AUTO_VALIDATED

> Cuando el operador pide revisar una tarea cuyo diagnostico es
> `KNOWLEDGE_TITLE_LENGTH`, el sistema resuelve un contexto de correccion para
> ese knowledge, en lugar de devolver "sin contexto" como ocurre hoy.

<details><summary>Detail</summary>

Hoy la resolucion de contexto solo cubre `SENTENCE_LENGTH` y `LEMMA_ABSENCE`;
para `KNOWLEDGE_TITLE_LENGTH` no se resuelve nada, de modo que la revision no
puede arrancar. Esta regla exige que ese diagnostico tenga su propia resolucion
de contexto, dedicada al knowledge (no al quiz).

**Failing test**: pedir el contexto de correccion para una tarea
`KNOWLEDGE_TITLE_LENGTH` valida hoy devuelve vacio; con esta feature devuelve un
contexto poblado con los campos de R002.

</details>

<a id="F-KTLR-R002"></a>
### Rule[F-KTLR-R002] - El contexto expone los datos que el agente necesita
**Severity**: high | **Validation**: AUTO_VALIDATED

> El contexto resuelto para una tarea `KNOWLEDGE_TITLE_LENGTH` expone el titulo
> actual, las instructions actuales, el label del topic contenedor, los limites
> objetivo de longitud y el idioma esperado del contenido, todo referido al
> knowledge que la tarea senala.

<details><summary>Detail</summary>

| Campo del contexto | Rol |
|--------------------|-----|
| Titulo actual del knowledge | Punto de partida a mejorar. |
| Instructions actuales del knowledge | Se comprimen sin perder la consigna. |
| Label del topic contenedor | El titulo no debe repetir lo que ya dice el topic. |
| Limites objetivo de longitud | Techo ponderado del titulo y de las instructions (los que aplica el audit). |
| Idioma esperado | Titulo en ingles, instructions en espanol (convencion del contenido). |

El contexto se refiere al knowledge que la tarea senala; no incluye datos de
ningun quiz. Que campos adicionales se agreguen (p.ej. titulos hermanos,
ejemplos de quizzes del knowledge) es decision de arquitectura; esta regla fija
el minimo que la propuesta necesita para ser generada y revisada.

**Failing test**: resolver el contexto de una tarea `KNOWLEDGE_TITLE_LENGTH` y
assertear que el titulo actual, las instructions actuales y el label del topic
del knowledge senalado estan presentes y correctamente poblados.

</details>

---

### Grupo B - Propuesta a partir del agente

<a id="F-KTLR-R003"></a>
### Rule[F-KTLR-R003] - La revision de KNOWLEDGE_TITLE_LENGTH invoca al agente de titulos
**Severity**: high | **Validation**: AUTO_VALIDATED

> Al revisar una tarea `KNOWLEDGE_TITLE_LENGTH`, el sistema entrega el contexto
> resuelto (R002) al agente de titulos y usa su salida para construir la
> propuesta; no cae al comportamiento identidad (propuesta que no cambia nada).

<details><summary>Detail</summary>

El agente de titulos ya existe y produce, para un knowledge, un titulo e
instructions mejorados. Esta feature lo enchufa como la estrategia de propuesta
dedicada a `KNOWLEDGE_TITLE_LENGTH`, de manera analoga a como `LEMMA_ABSENCE`
tiene su propia estrategia. Para cualquier otro diagnostico el comportamiento no
cambia.

**Failing test**: al revisar una tarea `KNOWLEDGE_TITLE_LENGTH`, el agente de
titulos es invocado con el contexto del knowledge y su salida alimenta la
propuesta resultante.

</details>

<a id="F-KTLR-R004"></a>
### Rule[F-KTLR-R004] - La salida del agente es un unico candidato de titulo por revision
**Severity**: high | **Validation**: AUTO_VALIDATED

> Por cada revision, el agente produce exactamente un candidato de correccion
> compuesto por un nuevo titulo y unas nuevas instructions para el knowledge —
> ni cero ni mas de uno —, con ambos campos presentes.

<details><summary>Detail</summary>

El candidato es la unidad conceptual que la propuesta encapsula: `{ titulo,
instructions }` referidos al knowledge de la tarea. No se emiten multiples
candidatos alternativos para que el operador elija; el filtro de calidad es el
operador en modo humano o la proxima auditoria.

**Failing test**: invocar la generacion con un contexto valido y assertear que
la salida tiene cardinalidad 1 con titulo e instructions no vacios.

</details>

<a id="F-KTLR-R005"></a>
### Rule[F-KTLR-R005] - El candidato se transforma en una propuesta revisable
**Severity**: high | **Validation**: AUTO_VALIDATED

> El sistema envuelve el candidato del agente en una propuesta con los mismos
> estados que las demas (pendiente / aprobar / rechazar), de modo que el
> operador la inspecciona y decide igual que cualquier otra revision.

<details><summary>Detail</summary>

La propuesta hace observable, en el estado "antes", el titulo e instructions
actuales del knowledge, y en el estado "despues", el titulo e instructions
propuestos por el agente. El vocabulario de decision (pendiente, aprobada,
rechazada) es el mismo del flujo de revision existente; esta feature no
introduce estados nuevos. En modo automatico la propuesta se aprueba sin
intervencion; en modo humano queda pendiente hasta que el operador decide.

**Failing test**: tras revisar una tarea `KNOWLEDGE_TITLE_LENGTH`, existe una
propuesta cuyo estado "despues" contiene el titulo e instructions del candidato
y cuyo estado "antes" contiene los valores actuales del knowledge.

</details>

---

### Grupo C - Persistencia al aprobar (bloqueo principal)

<a id="F-KTLR-R006"></a>
### Rule[F-KTLR-R006] - Aprobar una correccion de titulo escribe el knowledge en el curso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al aprobar una propuesta de `KNOWLEDGE_TITLE_LENGTH`, el nuevo titulo y las
> nuevas instructions quedan persistidos en el knowledge correspondiente del
> curso. Este es el bloqueo que la feature resuelve: hoy aprobar solo sabe
> escribir cambios sobre quizzes y una correccion de titulo de knowledge nunca
> llega al curso.

<details><summary>Detail</summary>

Antes de esta feature, la aprobacion escribe exclusivamente quizzes; una
propuesta cuyo objeto es un knowledge no tiene forma de materializarse en el
curso. Despues, aprobar reemplaza en el knowledge senalado su titulo (label) y
sus instructions por los del candidato aprobado.

El mecanismo interno por el que el objeto revisado deja de ser solo un quiz para
poder ser tambien un knowledge (el snapshot polimorfico y como se localiza y
reescribe el knowledge en el curso) es decision de arquitectura; ver
[DOUBT-SNAPSHOT-POLIMORFICO](#DOUBT-SNAPSHOT-POLIMORFICO). La regla funcional
solo exige que, aprobada la propuesta, el knowledge del curso quede con el nuevo
titulo e instructions.

**Failing test**: aprobar una propuesta `KNOWLEDGE_TITLE_LENGTH` sobre un
knowledge existente y, releyendo el curso, assertear que ese knowledge tiene el
titulo e instructions propuestos.

</details>

<a id="F-KTLR-R007"></a>
### Rule[F-KTLR-R007] - Alcance acotado de la correccion de titulo
**Severity**: critical | **Validation**: VALIDATED

> Aprobar una correccion de titulo produce exactamente tres efectos, y ninguno
> mas:
> 1. Cambia el titulo y las instructions del knowledge de la tarea.
> 2. Alinea con el nuevo titulo el titulo de los quizzes de ese knowledge
>    ([F-RPRES-R004](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R004)).
> 3. Nada mas cambia: ni el identificador del knowledge, ni su posicion en la
>    jerarquia, ni ningun otro knowledge, topic o nivel del curso, ni el
>    **contenido** de los quizzes — que se toma del estado actual del curso y
>    nunca de la version congelada al construir la propuesta.

<details><summary>Detail</summary>

**Que cambio respecto de la formulacion anterior.** Esta regla decia "no altera
sus quizzes". Eso quedo desactualizado: la alineacion del titulo de los quizzes
con la etiqueta de su knowledge es comportamiento correcto y esta declarada en
[F-RPRES-R004](../2026-07-29.01_preservacion-del-contenido-no-revisado/REQUIREMENT.md#F-RPRES-R004),
que es posterior y explicita. Mantener las dos redacciones dejaba dos reglas
contradictorias en el repositorio. La reformulacion **acota** el alcance en lugar
de negarlo: cambia el titulo del quiz, y nada mas del quiz.

**Lo que esta regla sigue protegiendo, y es lo importante.** El invariante 3 es
el que atrapo un bug real: la propuesta lleva adentro una foto del knowledge
tomada al construirla, y esa foto incluye los quizzes tal como estaban en ese
momento. Si al aplicar la correccion de titulo se escribieran los quizzes de la
foto, cualquier revision de quiz aprobada **despues** de esa foto quedaria
revertida — quizzes corregidos resucitando en su version vieja. Eso sigue
prohibido. La correccion de titulo alcanza un unico atributo del quiz (su
titulo); su contenido —la oracion, el enunciado, la traduccion— proviene siempre
del estado actual del curso.

**Distincion clave**: alinear un atributo derivado (el titulo, que es copia de la
etiqueta del knowledge) y reescribir el contenido de un quiz son cosas
distintas. La primera es el efecto deseado de esta correccion; la segunda es
perdida de trabajo ajeno.

**Failing test**: aprobar una propuesta `KNOWLEDGE_TITLE_LENGTH` sobre un
knowledge cuyos quizzes cambiaron **despues** de construida la propuesta, y
assertear que (a) el titulo y las instructions del knowledge son los corregidos,
(b) el titulo de sus quizzes quedo alineado con el nuevo titulo, (c) el contenido
de esos quizzes es el actual del curso y no el congelado en la propuesta, y (d)
el identificador del knowledge y los demas nodos del curso quedan sin cambios.

**Nota para el diseno de tests**: la verificacion vigente usa el **titulo** del
quiz como testigo de que gano la version actual del curso frente a la congelada.
Bajo el invariante 2 el titulo dejo de servir para eso, porque es justamente el
atributo que se re-alinea. El testigo correcto es el contenido del quiz (su
oracion), que no se toca. Es un cambio de asercion, no de alcance de la regla.

</details>

<a id="F-KTLR-R008"></a>
### Rule[F-KTLR-R008] - El flujo de revision de quizzes queda intacto
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Introducir la persistencia de correcciones de titulo de knowledge no cambia el
> comportamiento observable de la revision de quizzes: aprobar una propuesta de
> `SENTENCE_LENGTH` o `LEMMA_ABSENCE` sigue escribiendo el quiz en el curso
> exactamente como antes.

<details><summary>Detail</summary>

La persistencia deja de ser exclusivamente "de quizzes" para admitir tambien
"de knowledges", pero el camino de quizzes se preserva sin regresion. Esta regla
protege explicitamente las features de revision existentes.

**Failing test**: aprobar una propuesta de revision de quiz (`SENTENCE_LENGTH` o
`LEMMA_ABSENCE`) y assertear que el quiz corregido queda persistido en el curso
igual que antes de esta feature.

</details>

<a id="F-KTLR-R009"></a>
### Rule[F-KTLR-R009] - Rechazar deja el knowledge sin cambios
**Severity**: high | **Validation**: AUTO_VALIDATED

> Si el operador rechaza la propuesta, el curso no se modifica: el knowledge
> conserva su titulo e instructions originales y la tarea vuelve a quedar
> disponible para volver a intentarse.

<details><summary>Detail</summary>

Simetria con el rechazo del flujo de quizzes: un rechazo no toca el curso.

**Failing test**: rechazar una propuesta `KNOWLEDGE_TITLE_LENGTH` y assertear
que el knowledge del curso conserva su titulo e instructions originales.

</details>

---

### Grupo D - Manejo de fallas

<a id="F-KTLR-R010"></a>
### Rule[F-KTLR-R010] - Si el agente no produce un candidato valido, la revision aborta sin tocar el curso
**Severity**: high | **Validation**: AUTO_VALIDATED

> Si el agente falla al producir el candidato (falla del proveedor LLM, salida
> vacia o no interpretable, o cualquier error de runtime), la revision aborta:
> no se crea propuesta, el curso no se modifica, la tarea queda en su estado
> previo y el sistema reporta el fallo al operador.

<details><summary>Detail</summary>

Analogo a como la revision de `LEMMA_ABSENCE` maneja las fallas de estrategia:
una falla de generacion ocurre **antes** de tener una propuesta que decidir, de
modo que no hay artefacto que persistir ni curso que reescribir. La falla se
reporta con estado no-cero; no se registra como una propuesta rechazada ni
pendiente.

**Error**: "El agente de titulos no pudo generar una correccion para el
knowledge '{knowledgeId}'"

**Failing test**: inyectar una falla del agente y assertear que no se crea
propuesta, el curso no cambia y la tarea sigue en su estado previo.

</details>

<a id="F-KTLR-R011"></a>
### Rule[F-KTLR-R011] - Si el knowledge senalado no existe, la revision falla explicitamente
**Severity**: medium | **Validation**: AUTO_VALIDATED

> Si la tarea senala un knowledge que no existe en el curso, la revision falla
> explicitamente sin invocar al agente, sin crear propuesta y sin tocar el
> curso; el sistema informa que el knowledge no existe.

<details><summary>Detail</summary>

La ausencia del knowledge es un error de datos que se detecta al resolver el
contexto (R001), antes de gastar una invocacion del agente. Se distingue de
R010, que cubre fallas del agente ya invocado.

**Error**: "El knowledge '{knowledgeId}' referido por la tarea no existe en el
curso"

**Failing test**: revisar una tarea `KNOWLEDGE_TITLE_LENGTH` cuyo knowledge no
existe y assertear que el agente no se invoca, no hay propuesta y el curso no
cambia.

</details>

## Context

`plan` ya emite tareas con diagnostico `KNOWLEDGE_TITLE_LENGTH`: el analizador de
longitud de titulos de knowledge (FEAT-KTLEN) produce el score y el planner ya
mapea el diagnostico a la tarea. El agente que mejora titulo e instructions de un
knowledge tambien ya existe: respeta limites ponderados de longitud, evita que el
titulo repita informacion del topic o de las instructions y soporta ingles y
espanol.

Lo que falta es el **puente** entre esas dos piezas y el flujo de revision.
Precedente directo: `LEMMA_ABSENCE` ya recorre revise→propuesta→aprobar de punta
a punta ([F-LAPS-R001](#F-LAPS-R001) enchufa una estrategia dedicada; FEAT-LASAG
la implementa con un agente). La diferencia decisiva es el **objeto** de la
correccion: `LEMMA_ABSENCE` reemplaza un **quiz**, mientras que esta feature
modifica un **knowledge** (su titulo e instructions).

Esa diferencia toca el punto mas sensible del flujo: hoy, al aprobar una
propuesta, el sistema solo sabe escribir cambios sobre quizzes. El objeto que
viaja "antes"/"despues" en una propuesta es exclusivamente un quiz. Para que una
correccion de titulo de knowledge llegue al curso, ese objeto debe poder ser
tambien un knowledge, y la escritura al curso debe saber localizarlo y
reescribirlo. Resolver ese cambio sin romper la revision de quizzes existente es
el corazon de esta feature ([F-KTLR-R006](#F-KTLR-R006),
[F-KTLR-R008](#F-KTLR-R008)) y su unica duda arquitectonica abierta
([DOUBT-SNAPSHOT-POLIMORFICO](#DOUBT-SNAPSHOT-POLIMORFICO)).

## Scope

- **En alcance**: resolucion de contexto para `KNOWLEDGE_TITLE_LENGTH` (R001,
  R002); invocacion del agente de titulos y armado de la propuesta revisable
  (R003-R005); persistencia del nuevo titulo e instructions en el knowledge al
  aprobar (R006, R007); preservacion del flujo de revision de quizzes (R008);
  rechazo sin cambios (R009); manejo de fallas del agente y de knowledge
  inexistente (R010, R011).
- **Fuera de alcance**: cambios al analizador de longitud de titulos
  (FEAT-KTLEN) o al planner; modificar el flujo de revision de quizzes
  (`SENTENCE_LENGTH`, `LEMMA_ABSENCE`) mas alla de preservarlo; corregir
  longitud de *instructions* como diagnostico propio
  (`KNOWLEDGE_INSTRUCTIONS_LENGTH`); verificacion automatica de que el titulo
  producido cumple el limite (el filtro es el operador o la proxima auditoria);
  correcciones sobre topics, niveles o el curso.

## User Journeys

### Journey[F-KTLR-J001] - Revisar y aprobar una correccion de titulo de knowledge
**Validation**: AUTO_VALIDATED

Cubre el flujo principal: resolver contexto, invocar al agente, armar propuesta,
decidir (auto/humano) y persistir el titulo e instructions en el knowledge.

```yaml
journeys:
  - id: F-KTLR-J001
    name: Revisar y aprobar una correccion de titulo de knowledge
    flow:
      - id: iniciar_revision
        action: "El operador invoca la revision sobre una tarea cuyo diagnostico es KNOWLEDGE_TITLE_LENGTH"
        then: resolver_contexto

      - id: resolver_contexto
        action: "El sistema resuelve el contexto del knowledge senalado (titulo actual, instructions actuales, label del topic, limites objetivo, idioma)"
        gate: [F-KTLR-R001, F-KTLR-R002, F-KTLR-R011]
        outcomes:
          - when: "El knowledge existe y se resuelve el contexto"
            then: invocar_agente
          - when: "El knowledge senalado no existe en el curso"
            then: abortar_knowledge_inexistente

      - id: invocar_agente
        action: "El agente de titulos recibe el contexto y produce un candidato con nuevo titulo e instructions"
        gate: [F-KTLR-R003, F-KTLR-R004]
        outcomes:
          - when: "El agente produce un candidato valido"
            then: armar_propuesta
          - when: "El agente no puede producir un candidato valido"
            then: abortar_falla_agente

      - id: armar_propuesta
        action: "El sistema envuelve el candidato en una propuesta revisable: el estado antes tiene el titulo e instructions actuales, el estado despues los propuestos"
        gate: [F-KTLR-R005]
        then: decidir

      - id: decidir
        action: "La propuesta se decide segun el modo de aprobacion vigente"
        outcomes:
          - when: "La propuesta se aprueba (auto o humano)"
            then: persistir_knowledge
          - when: "El operador rechaza la propuesta"
            then: rechazo

      - id: persistir_knowledge
        action: "El sistema escribe en el knowledge del curso el nuevo titulo e instructions, sin tocar quizzes, identificadores ni el resto del arbol"
        gate: [F-KTLR-R006, F-KTLR-R007]
        then: tarea_completada

      - id: tarea_completada
        action: "La tarea queda marcada como completada y el curso refleja el knowledge corregido"
        result: success

      - id: rechazo
        action: "El curso no se modifica; el knowledge conserva su titulo e instructions originales y la tarea vuelve a quedar disponible"
        gate: [F-KTLR-R009]
        result: failure

      - id: abortar_falla_agente
        action: "El sistema reporta que el agente no pudo generar la correccion; no se crea propuesta, no se toca el curso, la tarea queda como estaba"
        gate: [F-KTLR-R010]
        result: failure

      - id: abortar_knowledge_inexistente
        action: "El sistema reporta que el knowledge no existe; no se invoca al agente, no se crea propuesta, no se toca el curso"
        gate: [F-KTLR-R011]
        result: failure
```

### Journey[F-KTLR-J002] - La revision de quizzes sigue intacta
**Validation**: AUTO_VALIDATED

Cubre R008 desde el angulo observable: aprobar una revision de quiz sigue
escribiendo el quiz en el curso, sin regresion por la nueva ruta de knowledge.

```yaml
journeys:
  - id: F-KTLR-J002
    name: La revision de quizzes sigue intacta
    flow:
      - id: revisar_quiz
        action: "El operador revisa y aprueba una tarea de revision de quiz (SENTENCE_LENGTH o LEMMA_ABSENCE)"
        then: persistir_quiz

      - id: persistir_quiz
        action: "El sistema escribe el quiz corregido en el curso por el camino de quizzes ya existente"
        gate: [F-KTLR-R008]
        outcomes:
          - when: "El quiz corregido queda persistido igual que antes de esta feature"
            then: quiz_ok

      - id: quiz_ok
        action: "El curso refleja el quiz corregido y la revision de quizzes no cambio de comportamiento"
        gate: [F-KTLR-R008]
        result: success
```

## Open Questions

### Doubt[DOUBT-SNAPSHOT-POLIMORFICO] - Como puede una propuesta portar y persistir un knowledge en vez de un quiz?
**Status**: OPEN (para arquitecto)

Hoy el objeto que viaja en el estado "antes"/"despues" de una propuesta es
exclusivamente un quiz, y la escritura al curso solo sabe localizar y reescribir
quizzes. Para que una correccion de titulo de knowledge se persista
([F-KTLR-R006](#F-KTLR-R006)), ese objeto debe poder ser tambien un knowledge, y
la escritura debe saber localizar y reescribir el knowledge por su identificador,
preservando el flujo de quizzes intacto ([F-KTLR-R008](#F-KTLR-R008)).

- [ ] Opcion A: El snapshot de la propuesta pasa a ser polimorfico (puede portar
  un quiz **o** un knowledge, discriminado por el tipo de nodo de la tarea); la
  escritura al curso enruta segun ese tipo.
- [ ] Opcion B: Un tipo de propuesta/snapshot separado para knowledges, con su
  propia ruta de escritura, sin tocar el snapshot de quizzes.
- [ ] Opcion C: Otra forma que el arquitecto considere que preserva
  [F-KTLR-R008](#F-KTLR-R008) sin duplicar logica.

**Answer**: Pendiente. La regla funcional solo exige que, aprobada la propuesta,
el knowledge del curso quede con el nuevo titulo e instructions
([F-KTLR-R006](#F-KTLR-R006), [F-KTLR-R007](#F-KTLR-R007)) sin romper la revision
de quizzes ([F-KTLR-R008](#F-KTLR-R008)); la forma concreta (snapshot polimorfico
vs ruta separada) es decision de arquitectura.

### Doubt[DOUBT-INSTRUCTIONS-SIEMPRE] - El agente reescribe instructions aunque el diagnostico sea solo de titulo?
**Status**: OPEN

El diagnostico que dispara la tarea es de **longitud de titulo**
(`KNOWLEDGE_TITLE_LENGTH`), pero el agente produce titulo **e** instructions
juntos (el alumno los ve junto al topic). Queda abierto si aprobar debe escribir
siempre ambos campos o solo el titulo.

- [ ] Opcion A: Aprobar escribe siempre ambos campos (titulo + instructions), ya
  que el agente los mejora en conjunto y el alumno los ve juntos. (Supuesto base
  reflejado en R006/R002.)
- [ ] Opcion B: Aprobar escribe solo el titulo; las instructions se dejan
  intactas salvo que exista una tarea `KNOWLEDGE_INSTRUCTIONS_LENGTH`.

**Answer**: Supuesto base = Opcion A (aprobar persiste titulo e instructions).
Marcado como duda para confirmacion del usuario; si se elige B, R002/R006 se
acotan a solo titulo.

## References

- **FEAT-KTLEN** (`2026-03-22.01_knowledge-titles-length`) — Define el analizador
  `knowledge-title-length` que produce el score y, via el planner, la tarea
  `KNOWLEDGE_TITLE_LENGTH` que esta feature revisa. Fuera de alcance modificarlo.
- **FEAT-LAPS** (`2026-04-20.02_lemma-absence-proposal-strategy`) — Precedente
  del puente revise→propuesta→aprobar: estrategia de propuesta dedicada por
  diagnostico, propuesta revisable y manejo de fallas antes de persistir. Modelo
  a replicar sobre knowledge en lugar de quiz. Citado por
  [F-KTLR-R003](#F-KTLR-R003) y [F-KTLR-R010](#F-KTLR-R010).
- **FEAT-LASAG** (`2026-06-18.01_lemma-absence-agent-migration`) — Precedente de
  un agente como backend de la estrategia LLM (contrato externo estable, un unico
  candidato, best-effort, runtime error = falla). Analogo al agente de titulos de
  esta feature. Citado por [F-KTLR-R004](#F-KTLR-R004).
- **FEAT-RPRES** (`2026-07-29.01_preservacion-del-contenido-no-revisado`) —
  Declara que corregir la etiqueta de un knowledge alinea el titulo de sus
  quizzes, como unica excepcion al radio de impacto de una revision. Gobierna el
  invariante 2 de [F-KTLR-R007](#F-KTLR-R007), que antes afirmaba lo contrario.
  Citado por [F-KTLR-R007](#F-KTLR-R007).
