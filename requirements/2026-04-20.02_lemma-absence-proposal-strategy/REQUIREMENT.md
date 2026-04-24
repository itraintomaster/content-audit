---
feature:
  id: FEAT-LAPS
  code: F-LAPS
  name: Primera estrategia real de propuesta de revision para LEMMA_ABSENCE
  priority: critical
---

# Primera estrategia real de propuesta de revision para LEMMA_ABSENCE

Hasta ahora la fase de revision (FEAT-REVBYP / FEAT-REVAPR) produce propuestas **identidad**: el Reviser activo para cualquier `DiagnosisKind` es el bypass y `elementAfter == elementBefore`. Eso sirvio para validar la pipeline end-to-end (construccion de propuesta, persistencia del artefacto, validacion, aprobacion, escritura al curso), pero no corrige contenido.

Este requerimiento introduce la **primera propuesta real** del sistema, y la acota a tareas de tipo `LEMMA_ABSENCE`. Dada una tarea de refinamiento de ese tipo y su `CorrectionContext` completo (el que ya construye FEAT-RCLA, incluyendo el `quizSentence` del quiz original en la DSL de FEAT-QSENT), el sistema debe producir una `RevisionProposal` cuyo `elementAfter` contenga un **quiz nuevo** cuyo cambio central es el ejercicio mismo (la oracion, los blanks, la respuesta correcta y sus variantes aceptadas, todo codificado dentro del `quizSentence`), pudiendo variar tambien la traduccion al espanol, corrigiendo las palabras fuera de nivel segun los `suggestedLemmas` que el contexto ya aporta. El ejercicio se maneja como `quizSentence` (no como oracion plana) porque un quiz no es solo una oracion: su estructura (que se tapa, que respuestas son validas, que variantes se aceptan) es parte intrinseca del ejercicio. Todo el flujo posterior (validacion, aprobacion, persistencia del artefacto, escritura al curso) se reutiliza tal cual de FEAT-REVBYP y FEAT-REVAPR — este requerimiento termina cuando la propuesta quedo construida.

## Contexto

### Relacion con features existentes

- **FEAT-RCLA**: construye el `CorrectionContext` para tareas `LEMMA_ABSENCE`. Este requerimiento **consume** ese contexto como unica fuente de datos y requiere que exponga el `quizSentence` del quiz original (DSL de FEAT-QSENT), la traduccion al espanol, el titulo e instrucciones del knowledge, el label del topic, el nivel CEFR del knowledge, las `misplacedLemmas` y las `suggestedLemmas`. **Dependencia sobre FEAT-RCLA:** el paso de exponer `quizSentence` en el `CorrectionContext` es condicion previa para esta estrategia. Si el `CorrectionContext` actual solo expone la oracion plana, FEAT-RCLA debe extenderse para incluir `quizSentence` antes de que LAPS pueda correr. Ese cambio es alcance de FEAT-RCLA y se coordina aparte; este requerimiento no redefine su forma.
- **FEAT-QSENT**: formaliza la DSL `quizSentence` como representacion textual del ejercicio (tronco + blanks + respuestas aceptadas + hints) y define su conversor publico desde y hacia `sentenceParts`. LAPS **delega en FEAT-QSENT** para todo lo relacionado con la forma del ejercicio: el candidato que emite la estrategia contiene un `quizSentence` en esa DSL, y el paso de derivacion del `elementAfter` parsea ese `quizSentence` via el conversor publico de FEAT-QSENT para materializar texto plano y `quizForm`. Ningun componente de LAPS reimplementa la DSL ni la conversion.
- **FEAT-REVBYP**: aporta el modelo `RevisionProposal` (R001), el Reviser pluggable por `DiagnosisKind` (R003), el fallback bypass (R004), el flujo de validacion/persistencia/aplicacion al curso (R006-R014). Este requerimiento **enchufa** una nueva estrategia de propuesta dentro del punto de extension R003, dedicada a `LEMMA_ABSENCE`. El resto del flujo no cambia.
- **FEAT-REVAPR**: aporta el modo de aprobacion humana. El ejercicio nuevo que produce esta iteracion (ya derivado a texto plano + `quizForm` desde el `quizSentence` del candidato) es lo que el operador inspecciona antes de aprobar o rechazar en modo `human`. Ningun cambio al flujo de aprobacion.
- **Otros `DiagnosisKind`**: `SENTENCE_LENGTH`, `LEMMA_RECURRENCE`, `LEMMA_COUNT` y cualquier otro diagnostico **siguen** usando el Reviser bypass (identidad) de FEAT-REVBYP R004. Este requerimiento es estrictamente local a `LEMMA_ABSENCE`; la extension a otros diagnosticos se tratara en requerimientos futuros.

### Alcance deliberado

Esta iteracion es el **primer cambio real de contenido** de la pipeline. El alcance se acota para dejar el resto del sistema intacto:

- **Solo `LEMMA_ABSENCE`.** Otros `DiagnosisKind` siguen con bypass hasta que tengan sus propios requerimientos.
- **Termina cuando la propuesta quedo construida.** Validador, artefacto, aprobacion y apply al curso reutilizan FEAT-REVBYP y FEAT-REVAPR sin cambios.
- **Un solo tipo de transformacion:** la propuesta reemplaza el quiz del ejercicio por uno nuevo. El cambio central es el `quizSentence` (con todo lo que incluye: tronco de la oracion, blanks, respuesta correcta y variantes aceptadas); la `translation` al espanol puede variar cuando el candidato asi lo determine. Los identificadores del quiz y del arbol de contenidos, las instrucciones del ejercicio, la posicion/orden del quiz dentro del knowledge y cualquier otra metadata estructural quedan identicos.
- **Sin loops:** una sola pasada para producir la propuesta. No hay reintentos automaticos, ni verificacion post-hoc del nivel CEFR del output, ni re-prompts.
- **Sin garantias de calidad del output:** si el ejercicio nuevo usa vocabulario fuera de nivel o queda demasiado largo/corto, eso aparecera como tareas nuevas en la proxima auditoria. El filtro de calidad es el validador/operador (modo humano de FEAT-REVAPR).

### Estrategia de propuesta como concepto

El sistema contempla que, a futuro, existiran **multiples formas** de producir la propuesta de revision para un mismo `DiagnosisKind` (distintos proveedores del ejercicio nuevo, distintos modelos, distintas orquestaciones). Para dejar ese espacio abierto sin comprometer la implementacion actual, este requerimiento trata a la generacion del ejercicio nuevo como una **estrategia de propuesta**: un componente abstracto, conceptual, que:

1. Se identifica por un nombre y una version (para trazabilidad en la propuesta persistida).
2. Declara que `DiagnosisKind` puede manejar.
3. Recibe el `CorrectionContext` asociado a la tarea y produce un candidato de quiz (un `quizSentence` en la DSL de FEAT-QSENT + una `translation` al espanol, ver R009).
4. Puede coexistir con otras estrategias registradas en el sistema, pero en cualquier invocacion **una sola** esta activa por tarea.

En esta iteracion se entrega **una sola estrategia concreta** para `LEMMA_ABSENCE` (la MVP). La forma en que se eligen, registran y exponen multiples estrategias queda abierta para el arquitecto (ver DOUBT-STRATEGY-SELECTION, DOUBT-STRATEGY-REGISTRY). La MVP debe alcanzar para correr el flujo end-to-end y corregir ejercicios reales.

### Actor principal

El operador del sistema, tipicamente desde la CLI. El flujo observable no cambia: se invoca `revise task <id>` sobre una tarea `LEMMA_ABSENCE` y el sistema produce la propuesta con el ejercicio nuevo; si el modo es `human`, la propuesta queda pendiente de aprobacion con el ejercicio ya visible.

---

## Reglas de Negocio

### Grupo A - Alcance de la propuesta real para LEMMA_ABSENCE

### Rule[F-LAPS-R001] - Las tareas LEMMA_ABSENCE producen propuestas no-identidad
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cuando el operador invoca la fase de revision (FEAT-REVBYP) sobre una `RefinementTask` cuyo `diagnosisKind` es `LEMMA_ABSENCE`, la `RevisionProposal` resultante debe tener un `elementAfter` que difiere del `elementBefore` en el ejercicio (tipicamente en el tronco de la oracion, y eventualmente tambien en la respuesta correcta, sus variantes aceptadas o la traduccion al espanol). Concretamente: el ejercicio propuesto en `elementAfter` **no** es textualmente identico al del `elementBefore` siempre que exista al menos una palabra a corregir segun el `CorrectionContext`. Esto sustituye el comportamiento previo donde el bypass entregaba `elementAfter == elementBefore` para este diagnostico.

**Error**: N/A (esta regla define un cambio de comportamiento sobre una extension existente)

### Rule[F-LAPS-R002] - El bypass deja de ser el Reviser activo para LEMMA_ABSENCE
**Severity**: critical | **Validation**: AUTO_VALIDATED

El despachador de Revisers de FEAT-REVBYP R003 ya no resuelve al Reviser bypass para tareas `LEMMA_ABSENCE`. A partir de este requerimiento, `LEMMA_ABSENCE` cuenta con una estrategia de propuesta dedicada (ver Grupo B), que toma precedencia sobre el bypass. El bypass sigue disponible como fallback para los demas `DiagnosisKind` que todavia no tienen estrategia propia.

**Error**: N/A (esta regla redefine la resolucion del Reviser activo)

### Rule[F-LAPS-R003] - El ejercicio nuevo reemplaza integramente al original
**Severity**: critical | **Validation**: AUTO_VALIDATED

El `elementAfter` contiene un ejercicio nuevo completo (`quizSentence` materializado + traduccion), que reemplaza integramente al original del `elementBefore`. No se aplica edicion parcial (reemplazo palabra por palabra, parches sobre la oracion original, parches sobre la respuesta correcta), porque el objetivo del diagnostico `LEMMA_ABSENCE` es producir un ejercicio **coherente** en el nivel objetivo. La propuesta no expone un diff de palabras: expone el ejercicio final tal como quedaria en el curso.

**Error**: N/A (esta regla define la forma del output)

---

### Grupo B - La estrategia de propuesta como concepto

### Rule[F-LAPS-R004] - El sistema soporta multiples estrategias de propuesta registradas
**Severity**: major | **Validation**: AUTO_VALIDATED

El sistema admite que coexistan varias estrategias de propuesta registradas para un mismo `DiagnosisKind`. En cualquier invocacion hay **una sola** estrategia activa por tarea: el sistema resuelve cual usar al iniciar la revision y la aplica sin componerla con otras. La forma concreta en que se eligen y registran (configuracion externa, seleccion por tarea, flag, default fijo) es decision de arquitectura (ver DOUBT-STRATEGY-SELECTION, DOUBT-STRATEGY-REGISTRY).

En esta iteracion ship **una sola** estrategia concreta para `LEMMA_ABSENCE` (ver Grupo C). La regla queda declarada para que futuras estrategias puedan incorporarse sin modificar el modelo.

**Error**: N/A (esta regla define una capacidad estructural)

### Rule[F-LAPS-R005] - La identidad de la estrategia queda registrada en la propuesta
**Severity**: critical | **Validation**: AUTO_VALIDATED

Cada `RevisionProposal` generada por una estrategia de propuesta debe incluir metadata suficiente para identificar a posteriori **quien** produjo el candidato. Como minimo:

| Campo | Descripcion |
|-------|-------------|
| estrategia (nombre) | Identificador simbolico de la estrategia (p. ej. `lemma-absence-mvp`). |
| estrategia (version) | Version de la estrategia; permite distinguir dos revisiones del mismo nombre a lo largo del tiempo. |

Esta metadata se persiste como parte del artefacto de la propuesta (FEAT-REVBYP R010, "suficiente para reconstruir la decision"). El campo `reviserKind` de FEAT-REVBYP R001 deja de ser siempre `"bypass"` para tareas `LEMMA_ABSENCE`: ahora refleja la identidad de la estrategia activa. Que otros datos de la estrategia adicionales quedan registrados (identificador del proveedor de la oracion nueva, firma de prompt, etc.) es decision de arquitectura (ver DOUBT-STRATEGY-METADATA, DOUBT-PROMPT-PERSISTENCE).

**Error**: N/A (esta regla define metadata obligatoria)

### Rule[F-LAPS-R006] - Si no hay estrategia activa que soporte LEMMA_ABSENCE, la revision falla explicitamente
**Severity**: major | **Validation**: AUTO_VALIDATED

Si al iniciar la fase de revision sobre una tarea `LEMMA_ABSENCE` no hay ninguna estrategia de propuesta registrada que maneje ese `DiagnosisKind`, el sistema **no** debe caer al Reviser bypass: debe fallar explicitamente, sin persistir artefacto y sin modificar la tarea ni el curso. El mensaje informa al operador que `LEMMA_ABSENCE` requiere una estrategia dedicada y que no hay ninguna activa.

Esta regla refuerza R002: una vez que `LEMMA_ABSENCE` tiene una estrategia dedicada, la ausencia de una estrategia activa es un error de configuracion, no un caso en el que se vuelva al comportamiento identidad del bypass.

**Error**: "No hay una estrategia de propuesta activa para el diagnostico 'LEMMA_ABSENCE'"

---

### Grupo C - La estrategia MVP de esta iteracion

### Rule[F-LAPS-R007] - La estrategia MVP consume el CorrectionContext completo de FEAT-RCLA
**Severity**: critical | **Validation**: AUTO_VALIDATED

La estrategia de propuesta MVP para `LEMMA_ABSENCE` recibe como unica fuente de datos el `CorrectionContext` que FEAT-RCLA construye para la tarea, y usa los siguientes campos:

| Campo del contexto | Rol en la generacion |
|--------------------|----------------------|
| `quizSentence` | Representacion compacta del ejercicio original en la DSL de FEAT-QSENT (texto con blanks `____` seguidos de bloques `[variantes|separadas|por|pipe]` y hints inline entre parentesis). Es el input estructural sobre el que opera la estrategia: describe completamente el ejercicio (tronco, huecos, respuestas aceptadas y scaffolding) en un unico string. |
| `translation` | Traduccion al espanol del ejercicio original; sirve para preservar el sentido general en el ejercicio nuevo. |
| `knowledgeTitle` | Titulo del knowledge que contiene el quiz; orienta el tema. |
| `knowledgeInstructions` | Instrucciones del knowledge (formato, registro); orienta el tipo de oracion esperado. |
| `topicLabel` | Label del topic; refuerza el dominio semantico. |
| `cefrLevel` | Nivel CEFR objetivo (A1, A2, B1...); marca el techo de complejidad esperado. |
| `misplacedLemmas` | Lemas fuera de nivel que deben ser reemplazados. |
| `suggestedLemmas` | Lemas dentro de nivel que pueden ocupar el lugar de los misplaced. |

La estrategia **no** debe consultar el `AuditReport` ni el arbol de diagnosticos directamente: toda la informacion necesaria ya esta curada en el `CorrectionContext`.

**Dependencia sobre FEAT-RCLA.** Esta regla asume que el `CorrectionContext` construido por FEAT-RCLA expone el `quizSentence` del quiz original (DSL de FEAT-QSENT) como campo accesible a los consumidores. Si FEAT-RCLA hoy solo provee la oracion plana, debe extenderse para incluir el `quizSentence` antes de que esta estrategia pueda operar. El impacto sobre FEAT-RCLA se declara aqui como dependencia pero no se resuelve en este requerimiento.

**Error**: N/A (esta regla define el contrato de entrada de la estrategia)

### Rule[F-LAPS-R008] - La estrategia MVP produce un unico candidato de quiz por invocacion
**Severity**: critical | **Validation**: AUTO_VALIDATED

Dada una tarea `LEMMA_ABSENCE` y su `CorrectionContext`, la estrategia MVP produce **un** candidato de quiz, en una sola pasada. No hay:

- Reintentos automaticos ante salidas que pudieran considerarse de baja calidad.
- Verificacion post-hoc del nivel CEFR o del largo del candidato.
- Multiples candidatos para que el operador elija (la estrategia emite uno solo).

Si la calidad del candidato no es adecuada, el mecanismo previsto es: el operador lo rechaza en modo humano (FEAT-REVAPR), o, si lo aprueba, aparece como nuevas tareas en la siguiente auditoria.

**Error**: N/A (esta regla acota el comportamiento de la estrategia)

### Rule[F-LAPS-R009] - El candidato de quiz tiene una forma conceptual definida
**Severity**: critical | **Validation**: AUTO_VALIDATED

El candidato que produce la estrategia es una estructura compuesta por **dos** elementos conceptuales:

| Elemento | Descripcion |
|----------|-------------|
| `quizSentence` | Representacion completa del ejercicio nuevo en la DSL de FEAT-QSENT (texto con blanks `____` seguidos de bloques `[variantes|separadas|por|pipe]` y, cuando corresponda, hints inline entre parentesis). Un unico string codifica el tronco de la oracion, la ubicacion de cada blank, la respuesta correcta de cada blank y sus variantes aceptadas. Que es y como se escribe el `quizSentence` esta definido por FEAT-QSENT y este requerimiento no lo redefine. |
| `translation` | Traduccion al espanol del ejercicio nuevo. La traduccion no forma parte de la DSL de FEAT-QSENT y por lo tanto viaja como campo aparte del candidato. |

Los elementos que en iteraciones anteriores aparecian separados (oracion con blanks, respuesta correcta por blank, variantes aceptadas) quedan **colapsados** dentro del `quizSentence`: la DSL de FEAT-QSENT los codifica a todos. La estrategia no los emite como campos independientes; los emite ya embebidos en el string del `quizSentence`.

**Error**: N/A (esta regla define la forma conceptual del candidato)

### Rule[F-LAPS-R010] - La estrategia MVP es no-deterministica por defecto
**Severity**: minor | **Validation**: AUTO_VALIDATED

No se requiere que dos invocaciones sucesivas de la estrategia sobre la misma tarea produzcan el mismo candidato. Se acepta explicitamente que la salida pueda variar entre ejecuciones. Esto no es un problema operativo porque FEAT-REVAPR R010 prohibe acumular propuestas pendientes sobre la misma tarea: cualquier re-revision requiere primero decidir la propuesta previa.

**Error**: N/A (esta regla define una caracteristica de la implementacion)

---

### Grupo D - Contenido del elementAfter

### Rule[F-LAPS-R011] - El output de la estrategia es un candidato de quiz expresado en la DSL de FEAT-QSENT
**Severity**: critical | **Validation**: AUTO_VALIDATED

La estrategia no emite una oracion plana lista para colocarse en el curso y tampoco emite una estructura de campos separados para blanks, respuesta correcta y variantes. Emite un **candidato de quiz** compuesto por exactamente dos piezas (ver R009):

1. Un `quizSentence` en la DSL de FEAT-QSENT, que codifica en un unico string el tronco de la oracion, los blanks, la respuesta correcta de cada blank y sus variantes aceptadas.
2. Una `translation` al espanol, como texto plano, aparte del `quizSentence` (la DSL de FEAT-QSENT no cubre la traduccion).

Lo que termina persistido en el `elementAfter` del quiz se deriva de ese candidato en un paso separado (R012). La estrategia no produce texto plano del ejercicio, no produce el `quizForm` del ejercicio y no produce el `elementAfter`.

**Error**: N/A (esta regla define la naturaleza del output de la estrategia)

### Rule[F-LAPS-R012] - La derivacion del elementAfter a partir del candidato es deterministica
**Severity**: critical | **Validation**: AUTO_VALIDATED

A partir del candidato de quiz (`quizSentence` + `translation`) producido por la estrategia, un proceso **deterministico** deriva los campos del `elementAfter`. La derivacion materializa a texto plano y a `quizForm` el `quizSentence`, usando el conversor de FEAT-QSENT como unica via para interpretar la DSL:

| Campo derivado del `elementAfter` | Origen |
|-----------------------------------|--------|
| Oracion del quiz en texto plano | Se obtiene parseando el `quizSentence` del candidato con el conversor de FEAT-QSENT y derivando la variante canonica de la plain sentence (la primera sub-variante del primer entry de cada CLOZE, sin hints, con whitespace normalizado — FEAT-QSENT R017/R018/R019). |
| Estructura del ejercicio (`quizForm`) | Se obtiene parseando el `quizSentence` del candidato con el conversor de FEAT-QSENT a `sentenceParts` (FEAT-QSENT R014/R015), de donde salen los TEXT y CLOZE que componen el `quizForm`. |
| Traduccion al espanol | Se toma directamente del campo `translation` del candidato. |
| Identificadores del quiz y del arbol de contenidos, instrucciones del ejercicio, posicion/orden del quiz dentro del knowledge y cualquier otra metadata estructural | Se copian del `elementBefore` sin modificacion (ver R014). |

Dado el mismo candidato y el mismo `elementBefore`, la derivacion produce siempre el mismo `elementAfter`. El no-determinismo (R010) reside en la estrategia al emitir el candidato, no en este paso posterior. La estrategia **no** emite texto plano ni `quizForm` (R011, R019): la unica via por la que esos campos llegan al `elementAfter` es via este paso deterministico que parsea el `quizSentence`.

Si el `quizSentence` emitido por la estrategia es malformado segun la gramatica de FEAT-QSENT, el conversor falla de forma atomica (FEAT-QSENT R016/R024) y la derivacion aborta sin producir un `elementAfter` parcial; esa falla se trata segun R015/R016.

**Error**: N/A (esta regla define el contrato del paso de derivacion)

### Rule[F-LAPS-R013] - Campos del elemento que pueden diferir entre elementBefore y elementAfter
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los campos que pueden diferir entre `elementBefore` y `elementAfter` son los que quedan determinados por el candidato emitido por la estrategia. Concretamente:

- **Oracion del quiz en texto plano** (derivada del `quizSentence` del candidato). Es el cambio central; practicamente siempre difiere cuando la estrategia corrige la tarea.
- **Estructura del ejercicio (`quizForm`: TEXT/CLOZE, respuesta correcta por CLOZE, variantes aceptadas)** (derivada tambien del `quizSentence` del candidato). Puede diferir cuando el candidato asi lo determine.
- **Traduccion al espanol** (tomada del campo `translation` del candidato). Puede diferir cuando el candidato produjo un ejercicio cuyo sentido cambio respecto del original.

El alcance del cambio en la estructura del ejercicio depende de si la palabra fuera-de-nivel formaba parte de la respuesta correcta o del tronco de la oracion:

- Si la palabra fuera-de-nivel **era** la respuesta correcta (o parte de ella, o una variante aceptada), es esperable que la estructura del ejercicio cambie en esos slots.
- Si la palabra fuera-de-nivel estaba en el **tronco** de la oracion (no era la respuesta correcta), la respuesta correcta puede mantenerse igual y los cambios en la estructura del ejercicio pueden ser menores o nulos.

En todos los casos, el cambio concreto lo determina el candidato emitido por la estrategia; este requerimiento no prescribe un patron especifico de cambio.

**Error**: N/A (esta regla define el alcance admitido del diff entre before y after)

### Rule[F-LAPS-R014] - Campos del elemento que NO pueden diferir entre elementBefore y elementAfter
**Severity**: critical | **Validation**: AUTO_VALIDATED

Los siguientes campos se preservan identicamente entre `elementBefore` y `elementAfter`:

- `quizId` y cualquier otro identificador propio del quiz.
- Identificadores de nodo del arbol de contenidos: knowledge, topic, milestone, course.
- Instrucciones del ejercicio.
- Posicion/orden del quiz dentro del knowledge.
- Cualquier otra metadata estructural del elemento.

La propuesta nunca crea, elimina o reordena quizzes, nunca renombra identificadores, nunca reescribe instrucciones. Estos campos se copian del `elementBefore` durante la derivacion (R012).

**Error**: N/A (esta regla define invariantes estructurales)

---

### Grupo E - Manejo de fallas de la estrategia

### Rule[F-LAPS-R015] - Si la estrategia no puede producir un candidato, la revision aborta antes de persistir
**Severity**: critical | **Validation**: AUTO_VALIDATED

Si la estrategia MVP no logra producir un candidato de quiz (por cualquier motivo: servicio externo caido, respuesta vacia, `quizSentence` malformado segun la DSL de FEAT-QSENT, respuesta que no cumple un formato minimo interpretable), el flujo de revision debe abortar **antes** de persistir el artefacto de propuesta y antes de tocar el curso. Concretamente:

1. No se crea artefacto de propuesta bajo `.content-audit/revisions/` para esta invocacion.
2. El curso en disco no se modifica.
3. La `RefinementTask` permanece en su estado previo (tipicamente `PENDING`).
4. El sistema reporta al operador que la estrategia no pudo producir la propuesta; la invocacion exit con estado no-cero.

Esta regla es distinta de FEAT-REVBYP R014 (falla al aplicar al curso despues de artefacto persistido): aqui la falla ocurre **antes** de tener una propuesta construida, de modo que no hay artefacto que persistir.

**Error**: "La estrategia de propuesta '{estrategia}' no pudo generar un candidato de quiz para la tarea '{taskId}'"

### Rule[F-LAPS-R016] - Una falla de estrategia no es un REJECTED ni un PENDING_APPROVAL
**Severity**: major | **Validation**: AUTO_VALIDATED

Una falla de la estrategia (R015) **no** se registra como una propuesta con veredicto `REJECTED`, ni como `PENDING_APPROVAL`, ni como ningun otro veredicto del vocabulario de FEAT-REVAPR R001. Esos veredictos aplican a propuestas **construidas** que despues un validator decide. Cuando no se pudo construir la propuesta, simplemente no hay artefacto que decidir. El estado observable del sistema luego de la falla es: tarea sin cambios, curso sin cambios, sin artefacto nuevo.

Si queda algun rastro persistente de la falla (log de intento, registro de fallo) es decision de arquitectura (ver DOUBT-FAILURE-TRACEABILITY). La regla funcional solo prohibe contaminar el flujo de veredictos.

**Error**: N/A (esta regla define una frontera entre "falla de construccion" y "veredicto de decision")

---

### Grupo F - Limites explicitos de la MVP

### Rule[F-LAPS-R017] - La estrategia no valida el nivel CEFR del output
**Severity**: minor | **Validation**: AUTO_VALIDATED

La estrategia MVP no aplica una verificacion automatica de que el `quizSentence` producido cumpla el `cefrLevel` objetivo (no re-tokeniza el texto, no lo pasa por un chequeador de vocabulario, no recuenta `misplacedLemmas` despues). El `cefrLevel` se usa como **guia** para producir el candidato, no como criterio de validacion. Si el ejercicio nuevo incluye palabras fuera de nivel, eso aparecera como tareas en la proxima auditoria.

**Error**: N/A (esta regla acota explicitamente una funcionalidad ausente)

### Rule[F-LAPS-R018] - La estrategia no valida el largo del output
**Severity**: minor | **Validation**: AUTO_VALIDATED

La estrategia MVP no impone un limite duro al largo del `quizSentence` del candidato, ni lo rechaza por ser demasiado corto o demasiado largo. Si el ejercicio queda fuera del rango considerado aceptable por el diagnostico `SENTENCE_LENGTH`, el desajuste se detectara en la proxima auditoria como una tarea nueva de ese otro tipo.

**Error**: N/A (esta regla acota explicitamente una funcionalidad ausente)

### Rule[F-LAPS-R019] - La estrategia emite el candidato y nada mas
**Severity**: critical | **Validation**: AUTO_VALIDATED

El output de la estrategia se limita al candidato de quiz descrito en R009: el `quizSentence` (DSL de FEAT-QSENT) y la `translation` al espanol, y **nada mas**. La estrategia **no** construye la oracion del quiz en texto plano del `elementAfter`, **no** construye la estructura final del ejercicio (`quizForm`), **no** construye el `elementAfter`, **no** emite listas separadas de respuesta correcta por blank ni de variantes aceptadas (esas quedan codificadas dentro del `quizSentence`).

La derivacion del `elementAfter` a partir del candidato ocurre en un paso separado (R012), que parsea el `quizSentence` via FEAT-QSENT y copia del `elementBefore` los campos preservados (R014). La separacion entre "que emite la estrategia" (DSL + traduccion) y "que se persiste en el `elementAfter`" (texto plano + `quizForm` + traduccion + metadata preservada) es explicita y estable.

**Error**: N/A (esta regla define el contrato de salida de la estrategia)

---

## User Journeys

### Journey[F-LAPS-J001] - Flujo feliz end-to-end con validador auto
**Validation**: AUTO_VALIDATED

Happy path en modo `auto` (FEAT-REVAPR `CONTENT_AUDIT_APPROVAL_MODE=auto`): el operador corre `revise task <id>` sobre una tarea `LEMMA_ABSENCE`, la estrategia MVP produce un candidato de quiz, el sistema deriva el `elementAfter`, el validator auto aprueba, el curso queda reescrito, la tarea cierra.

```yaml
journeys:
  - id: F-LAPS-J001
    name: Flujo feliz end-to-end con validador auto
    flow:
      - id: iniciar_revision
        action: "El operador invoca la fase de revision sobre una tarea cuyo diagnosisKind es LEMMA_ABSENCE en modo auto"
        then: resolver_estrategia

      - id: resolver_estrategia
        action: "El sistema resuelve que estrategia de propuesta esta activa para LEMMA_ABSENCE"
        gate: [F-LAPS-R002, F-LAPS-R004, F-LAPS-R006]
        outcomes:
          - when: "Hay una estrategia activa registrada para LEMMA_ABSENCE"
            then: invocar_estrategia
          - when: "No hay ninguna estrategia activa registrada para LEMMA_ABSENCE"
            then: abortar_sin_estrategia

      - id: invocar_estrategia
        action: "La estrategia recibe el CorrectionContext completo de la tarea (FEAT-RCLA) y produce un candidato de quiz"
        gate: [F-LAPS-R007, F-LAPS-R008, F-LAPS-R009, F-LAPS-R011]
        outcomes:
          - when: "La estrategia produce un candidato de quiz"
            then: construir_propuesta
          - when: "La estrategia no puede producir un candidato de quiz"
            then: abortar_por_falla_estrategia

      - id: construir_propuesta
        action: "El sistema deriva el elementAfter a partir del candidato: la oracion del quiz, la estructura del ejercicio y la traduccion quedan segun lo que aporto el candidato; los identificadores, instrucciones y metadata estructural se preservan del elementBefore. Se registra la identidad de la estrategia en la propuesta"
        gate: [F-LAPS-R001, F-LAPS-R003, F-LAPS-R005, F-LAPS-R012, F-LAPS-R013, F-LAPS-R014, F-LAPS-R019]
        then: flujo_heredado_auto

      - id: flujo_heredado_auto
        action: "El flujo continua segun FEAT-REVBYP: el validator auto aprueba la propuesta, el sistema persiste el artefacto y reescribe el curso"
        outcomes:
          - when: "La aplicacion al curso fue exitosa"
            then: tarea_done
          - when: "La aplicacion al curso fallo"
            then: fallo_aplicacion_heredado

      - id: tarea_done
        action: "La RefinementTask queda marcada como completada"
        result: success

      - id: abortar_sin_estrategia
        action: "El sistema reporta que no hay estrategia activa para LEMMA_ABSENCE; no se persiste artefacto, no se toca el curso, la tarea queda como estaba"
        gate: [F-LAPS-R006]
        result: failure

      - id: abortar_por_falla_estrategia
        action: "El sistema reporta que la estrategia no pudo producir un candidato de quiz; no se persiste artefacto, no se toca el curso, la tarea queda como estaba"
        gate: [F-LAPS-R015, F-LAPS-R016]
        result: failure

      - id: fallo_aplicacion_heredado
        action: "La propuesta fue aprobada y persistida, pero la escritura del curso fallo; el comportamiento es el heredado de FEAT-REVBYP R014"
        result: failure
```

### Journey[F-LAPS-J002] - Flujo feliz end-to-end con aprobacion humana
**Validation**: AUTO_VALIDATED

Happy path en modo `human` (FEAT-REVAPR): la estrategia produce un candidato de quiz, el sistema deriva el `elementAfter`, la propuesta queda `PENDING_APPROVAL`, el operador inspecciona la propuesta y la aprueba; el curso se reescribe, la tarea cierra.

```yaml
journeys:
  - id: F-LAPS-J002
    name: Flujo feliz end-to-end con aprobacion humana
    flow:
      - id: iniciar_revision_human
        action: "El operador invoca la fase de revision sobre una tarea LEMMA_ABSENCE en modo human (FEAT-REVAPR)"
        then: resolver_estrategia_human

      - id: resolver_estrategia_human
        action: "El sistema resuelve la estrategia activa para LEMMA_ABSENCE"
        gate: [F-LAPS-R002, F-LAPS-R004]
        then: invocar_estrategia_human

      - id: invocar_estrategia_human
        action: "La estrategia consume el CorrectionContext y produce un candidato de quiz"
        gate: [F-LAPS-R007, F-LAPS-R008, F-LAPS-R009, F-LAPS-R011]
        then: construir_propuesta_human

      - id: construir_propuesta_human
        action: "El sistema deriva el elementAfter a partir del candidato: la oracion del quiz, la estructura del ejercicio y la traduccion quedan segun lo que aporto el candidato; los identificadores, instrucciones y metadata estructural se preservan del elementBefore. Se registra la metadata de la estrategia"
        gate: [F-LAPS-R001, F-LAPS-R003, F-LAPS-R005, F-LAPS-R012, F-LAPS-R013, F-LAPS-R014, F-LAPS-R019]
        then: pendiente_aprobacion

      - id: pendiente_aprobacion
        action: "El validator humano (FEAT-REVAPR) emite PENDING_APPROVAL; el sistema persiste el artefacto con el elementAfter derivado visible; el curso no se toca; la tarea queda esperando aprobacion"
        then: operador_decide

      - id: operador_decide
        action: "El operador inspecciona el artefacto y decide aprobar o rechazar la propuesta"
        outcomes:
          - when: "El operador aprueba la propuesta"
            then: aplicar_curso_tras_aprobar
          - when: "El operador rechaza la propuesta"
            then: rechazo_manual

      - id: aplicar_curso_tras_aprobar
        action: "El flujo de FEAT-REVAPR aplica la propuesta aprobada: se reescribe el curso con el elementAfter derivado y la tarea pasa a completada"
        result: success

      - id: rechazo_manual
        action: "El flujo de FEAT-REVAPR transiciona la propuesta a REJECTED, el curso no se toca, la tarea vuelve a PENDING"
        result: success
```

### Journey[F-LAPS-J003] - La estrategia falla al producir el candidato
**Validation**: AUTO_VALIDATED

Cubre R015/R016: cuando la estrategia no puede producir el candidato de quiz, la revision aborta antes de persistir nada.

```yaml
journeys:
  - id: F-LAPS-J003
    name: La estrategia falla al producir el candidato
    flow:
      - id: iniciar_revision_falla
        action: "El operador invoca la fase de revision sobre una tarea LEMMA_ABSENCE"
        then: invocar_estrategia_falla

      - id: invocar_estrategia_falla
        action: "La estrategia recibe el CorrectionContext pero no logra producir un candidato de quiz (respuesta vacia, proveedor no disponible, respuesta sin formato interpretable)"
        gate: [F-LAPS-R007, F-LAPS-R015]
        then: abortar_sin_artefacto

      - id: abortar_sin_artefacto
        action: "El sistema reporta el fallo al operador, no persiste ningun artefacto de propuesta, no modifica el curso y la tarea queda en su estado previo"
        gate: [F-LAPS-R015, F-LAPS-R016]
        result: failure
```

### Journey[F-LAPS-J004] - No hay estrategia activa para LEMMA_ABSENCE
**Validation**: AUTO_VALIDATED

Cubre R006: si al momento de revisar no hay ninguna estrategia registrada que soporte `LEMMA_ABSENCE`, la revision falla explicitamente en vez de caer al bypass.

```yaml
journeys:
  - id: F-LAPS-J004
    name: No hay estrategia activa para LEMMA_ABSENCE
    flow:
      - id: iniciar_revision_sin_estrategia
        action: "El operador invoca la fase de revision sobre una tarea LEMMA_ABSENCE en un entorno donde no hay estrategia activa registrada para ese diagnosisKind"
        then: resolver_sin_estrategia

      - id: resolver_sin_estrategia
        action: "El despachador comprueba que no hay estrategia de propuesta activa que soporte LEMMA_ABSENCE y que tampoco corresponde caer al bypass para este diagnostico"
        gate: [F-LAPS-R002, F-LAPS-R006]
        then: abortar_explicitamente

      - id: abortar_explicitamente
        action: "El sistema reporta que LEMMA_ABSENCE requiere una estrategia dedicada y no hay ninguna activa; no se persiste artefacto, no se toca el curso, la tarea queda como estaba"
        gate: [F-LAPS-R006]
        result: failure
```

### Journey[F-LAPS-J005] - El operador rechaza la propuesta en modo humano
**Validation**: AUTO_VALIDATED

Cubre el cruce con FEAT-REVAPR desde el angulo de este feature: la estrategia produjo un candidato, el sistema derivo el elementAfter y persistio la propuesta, pero el operador considera que no es aceptable y la rechaza; la tarea queda disponible para volver a intentarse.

```yaml
journeys:
  - id: F-LAPS-J005
    name: El operador rechaza la propuesta en modo humano
    flow:
      - id: iniciar_revision_rechazo
        action: "El operador invoca la fase de revision sobre una tarea LEMMA_ABSENCE en modo human"
        then: estrategia_produce_candidato

      - id: estrategia_produce_candidato
        action: "La estrategia consume el CorrectionContext y produce un candidato de quiz"
        gate: [F-LAPS-R007, F-LAPS-R008, F-LAPS-R009, F-LAPS-R011]
        then: propuesta_pendiente

      - id: propuesta_pendiente
        action: "El sistema deriva el elementAfter a partir del candidato y persiste la propuesta como PENDING_APPROVAL con la metadata de la estrategia"
        gate: [F-LAPS-R001, F-LAPS-R005, F-LAPS-R012, F-LAPS-R013]
        then: operador_evalua

      - id: operador_evalua
        action: "El operador inspecciona el elementAfter derivado en el artefacto y decide rechazarla"
        then: rechazo_heredado

      - id: rechazo_heredado
        action: "El flujo de FEAT-REVAPR transiciona la propuesta a REJECTED, el curso no se modifica y la tarea vuelve a PENDING"
        result: success
```

---

## Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio:

- **Solo LEMMA_ABSENCE.** Los demas `DiagnosisKind` (`SENTENCE_LENGTH`, `LEMMA_RECURRENCE`, `LEMMA_COUNT`, etc.) siguen usando el Reviser bypass de FEAT-REVBYP. La extension de estrategias reales a otros diagnosticos se tratara en requerimientos futuros.
- **Una sola estrategia activa por invocacion.** R004 admite varias registradas, pero solo una corre por tarea. No se componen, no se encadenan, no se votan varias estrategias.
- **Una sola estrategia concreta en esta entrega (la MVP).** Otras estrategias alternativas (distintos proveedores, distintas orquestaciones con agentes, ensembles) quedan para iteraciones futuras.
- **Una sola pasada, un solo candidato.** La estrategia emite exactamente un candidato de quiz. No hay multiples candidatos alternativos para que el operador elija, no hay reintentos automaticos en caso de baja calidad.
- **Sin verificacion automatica del output.** No se valida que el candidato cumpla el `cefrLevel` ni un rango de largo especifico. El filtro de calidad es el operador (modo humano) o la proxima auditoria.
- **Sin edicion parcial.** El candidato reemplaza integramente al quiz original; no se expone un diff palabra-por-palabra ni se aplican parches sobre el quiz existente.
- **Los identificadores y la metadata estructural del quiz se preservan.** R014. Los identificadores del quiz y del arbol de contenidos, las instrucciones del ejercicio y la posicion/orden dentro del knowledge no cambian entre `elementBefore` y `elementAfter`. El `quizSentence` (oracion, blanks, respuesta correcta, variantes aceptadas) y la traduccion al espanol **pueden cambiar** cuando el candidato asi lo determine, tipicamente si la palabra reemplazada era la respuesta correcta o parte de la estructura del ejercicio (R013).
- **Sin traceability de fallas como artefacto.** R016: una falla de estrategia no genera artefacto persistente. El registro de intentos fallidos (si existe) es decision de arquitectura (ver DOUBT-FAILURE-TRACEABILITY).
- **No deterministica por defecto.** R010. Dos invocaciones sucesivas pueden producir candidatos distintos; la politica de FEAT-REVAPR R010 (rechazar re-revise con propuesta pendiente) evita la acumulacion accidental.

---

## Open Questions

### Doubt[DOUBT-STRATEGY-SELECTION] - Como se elige la estrategia activa para una tarea?
**Status**: OPEN (para arquitecto)

R004 declara que pueden coexistir varias estrategias registradas pero que una sola corre por tarea. La cuestion es **como** se decide cual corre:

- [ ] Opcion A: Seleccion al arranque del proceso via configuracion externa (variable de entorno, archivo de configuracion). Analogo a FEAT-REVAPR R005 con `CONTENT_AUDIT_APPROVAL_MODE`.
- [ ] Opcion B: Seleccion por tarea: cada `RefinementTask` podria declarar o heredar cual estrategia quiere (por plan, por auditoria de origen, por un campo del plan).
- [ ] Opcion C: Flag en la invocacion del verbo de revision (`revise task <id> --strategy <name>`).
- [ ] Opcion D: Una sola estrategia "default" fija por `DiagnosisKind`; no hay seleccion en esta iteracion.

**Answer**: Pendiente. La regla funcional solo requiere que el sistema pueda identificar cual estrategia uso para una propuesta dada (R005); el mecanismo de seleccion en si es decision de arquitectura.

### Doubt[DOUBT-STRATEGY-REGISTRY] - Donde y como se registran las estrategias, y como se expone el catalogo al operador?
**Status**: OPEN (para arquitecto)

R004 habla de "estrategias registradas" sin definir el mecanismo. Preguntas abiertas:

- Donde vive la lista de estrategias registradas (codigo en el composition root, configuracion externa, recurso declarativo tipo `StrategyConfig`)?
- El operador puede consultar el catalogo (`get strategies`)? Necesita verlas para decidir cual activar?
- Como se enlaza una estrategia registrada con el `DiagnosisKind` que declara soportar?

**Answer**: Pendiente. La regla funcional solo requiere que el conjunto de estrategias disponibles sea extensible y que una sola corra por tarea; el mecanismo de registro y de consulta queda para el arquitecto.

### Doubt[DOUBT-STRATEGY-METADATA] - Que metadata adicional de la estrategia queda registrada en la propuesta?
**Status**: OPEN (para arquitecto)

R005 exige que al menos el nombre y la version de la estrategia queden registrados en la propuesta. Queda abierto que otros campos conviene persistir:

- [ ] Identificador del proveedor de la oracion nueva (si la estrategia delega en un servicio externo).
- [ ] Identificador del modelo subyacente (si aplicara; sin nombrar productos).
- [ ] Hash o firma de la configuracion de la estrategia al momento de la invocacion.
- [ ] Duracion de la ejecucion (util para monitoreo).

**Answer**: Pendiente. La regla funcional solo exige trazabilidad suficiente para identificar a posteriori "quien" produjo el candidato; el nivel de detalle adicional es decision de arquitectura.

### Doubt[DOUBT-PROMPT-PERSISTENCE] - La propuesta debe incluir el input exacto consumido por la estrategia?
**Status**: OPEN (para arquitecto)

Para reproducibilidad completa de una propuesta seria util persistir el input exacto que la estrategia consumio (los datos del `CorrectionContext` en el momento, eventualmente el prompt/consulta construido). Opciones:

- [ ] Opcion A: Persistir solo la identidad de la estrategia (R005) + referencia al `CorrectionContext` de la tarea; asumiendo que el contexto es recuperable desde el plan/audit de origen, alcanza.
- [ ] Opcion B: Persistir un snapshot del input efectivamente consumido por la estrategia (copia de los campos del contexto en el momento de la invocacion) dentro del artefacto.
- [ ] Opcion C: Persistir tambien el prompt/consulta exacto que la estrategia construyo (si corresponde), para reproducibilidad total.

**Answer**: Pendiente. La regla funcional solo exige que el artefacto sea suficiente para entender la decision (FEAT-REVBYP R010); la granularidad de reproducibilidad es decision de arquitectura.

### Doubt[DOUBT-CANDIDATE-NOTATION] - Como se representa concretamente el candidato de quiz?
**Status**: RESOLVED

R009 define los elementos conceptuales que componen el candidato. La pregunta era **como** se representa sintacticamente al momento de la implementacion.

- [x] Opcion A: Delegar al DSL existente de FEAT-QSENT (`quizSentence` = String con la notacion `____ [variantes|separadas|por|pipe]` y hints inline entre parentesis). La `translation` al espanol viaja como campo String aparte porque no forma parte de la DSL de FEAT-QSENT.
- [ ] Opcion B: Una estructura con campos separados por cada elemento conceptual (oracion con placeholders, lista de respuestas correctas por blank, lista de variantes aceptadas por respuesta, traduccion), sin DSL textual.
- [ ] Opcion C: Una combinacion: oracion con placeholders en texto plano, mas estructuras separadas para respuestas y variantes.

**Answer**: Resuelto por FEAT-QSENT. El candidato es exactamente `{ quizSentence: <DSL de FEAT-QSENT>, translation: <texto en espanol> }`. La DSL de FEAT-QSENT ya codifica en un unico string el tronco de la oracion, los blanks, la respuesta correcta y las variantes aceptadas; reusarla evita inventar una representacion ad-hoc. Los elementos que en versiones previas de este requerimiento aparecian como campos separados (oracion con blanks, respuesta correcta por blank, variantes aceptadas) quedan colapsados dentro del `quizSentence`. Formalizado en R009, R011, R012 y R019.

### Doubt[DOUBT-FAILURE-TRACEABILITY] - Una estrategia que falla deja algun rastro persistente?
**Status**: OPEN (para arquitecto)

R016 establece que una falla de estrategia no genera artefacto de propuesta. Pero queda abierta la pregunta de si conviene dejar algun rastro persistente del intento fallido:

- [ ] Opcion A: Cero rastro persistente; solo se reporta al operador en el momento de la invocacion (via stdout/stderr). Mas simple, pierde historial.
- [ ] Opcion B: Log de intentos (archivo `.content-audit/revisions/_failures.log` o similar) con el `taskId`, la estrategia intentada, el timestamp y el motivo. Preserva historial sin contaminar el flujo de propuestas validas.
- [ ] Opcion C: Un artefacto de tipo distinto (por ejemplo `FailedProposalAttempt`) bajo un subdirectorio separado. Mas estructurado, mas superficie.

**Answer**: Pendiente. La regla funcional solo prohibe contaminar el vocabulario de veredictos (R016); la persistencia del intento fallido es decision de arquitectura.

---

## ASSUMPTIONS

1. **El `CorrectionContext` de FEAT-RCLA es suficiente como unica fuente de datos para la estrategia.** R007 enumera los campos que la estrategia consume; se asume que `quizSentence` (DSL de FEAT-QSENT), `translation`, `knowledgeTitle`, `knowledgeInstructions`, `topicLabel`, `cefrLevel`, `misplacedLemmas` y `suggestedLemmas` estan disponibles y correctamente poblados para toda tarea `LEMMA_ABSENCE` por FEAT-RCLA. Razon: no se consulta el `AuditReport` ni el arbol de diagnosticos directamente desde la estrategia; la informacion ya esta curada. **Dependencia:** esta iteracion asume que FEAT-RCLA expone el `quizSentence` del quiz original en el `CorrectionContext`. Si FEAT-RCLA hoy solo expone la oracion plana, debe extenderse antes de que la estrategia pueda operar; ese cambio es alcance de FEAT-RCLA, no de este requerimiento.
2. **El operador, via FEAT-REVAPR en modo humano, es el filtro de calidad.** La estrategia no tiene que ser perfecta. La primera linea de defensa contra una oracion mala es el modo humano; la segunda linea es la proxima auditoria.
3. **La propuesta puede modificar la oracion del quiz, la traduccion al espanol y la estructura del ejercicio (respuesta correcta, variantes aceptadas) segun lo que aporte el candidato.** R013. Los identificadores del quiz y del arbol de contenidos, las instrucciones del ejercicio y la posicion/orden dentro del knowledge se preservan intactos (R014). El alcance concreto del cambio entre `elementBefore` y `elementAfter` depende del candidato emitido por la estrategia.
4. **Dos invocaciones sobre la misma tarea LEMMA_ABSENCE pueden producir dos candidatos distintos.** R010. La estrategia no es determinista por defecto. El comportamiento de FEAT-REVAPR R010 (rechazar re-revise con propuesta pendiente) evita acumulacion accidental de propuestas distintas sobre la misma tarea.
5. **El modelo `RevisionProposal` de FEAT-REVBYP R001 admite, via su campo `reviserKind` y la nota generica "rationale", la metadata pedida en R005.** Si no alcanza (porque se quiere persistir nombre + version estructurados en campos separados, o mas metadata segun DOUBT-STRATEGY-METADATA), se espera que el arquitecto extienda la estructura del artefacto en forma compatible con FEAT-REVBYP R010.
6. **La DSL de FEAT-QSENT es el unico formato por el que la estrategia y el derivador intercambian la forma del ejercicio.** La estrategia opera 100% sobre `quizSentence` (entrada y salida). El paso de derivacion (R012) es el unico punto del flujo que materializa texto plano y `quizForm`, usando el conversor publico de FEAT-QSENT. Ningun otro componente reinterpreta la DSL, construye texto plano a partir de la oracion, ni duplica la logica de conversion: todo atraviesa el conversor de FEAT-QSENT. Razon: alinea el modelado con la semantica real del quiz (no es solo una oracion: la estructura de blanks y respuestas es intrinseca al ejercicio) y evita representaciones ad-hoc que divergirian del contrato de dominio.
