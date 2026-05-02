---
feature:
  id: FEAT-LAGEN
  code: F-LAGEN
  name: Generador real de candidatos de quiz para LEMMA_ABSENCE basado en LLM
  priority: critical
---

# Generador real de candidatos de quiz para LEMMA_ABSENCE basado en LLM

## TL;DR

**Que**: Reemplaza el candidato canned de la estrategia de propuesta para `LEMMA_ABSENCE` por uno generado en vivo por un modelo de lenguaje, alimentado por todo el `LemmaAbsenceCorrectionContext` poblado.

**Por que**: Tras esta iteracion, lo que el operador ve archivado al correr `revise task <id>` es contenido producido por un modelo real e identificado como tal en la metadata de cada propuesta, en lugar de la respuesta fija independiente del contexto.

## Reglas de Negocio

### Grupo A - Identidad de la estrategia visible en cada propuesta

<a id="F-LAGEN-R001"></a>
### Rule[F-LAGEN-R001] - El nombre de la estrategia activa para LEMMA_ABSENCE pasa a ser `lemma-absence-llm`
**Severity**: critical | **Validation**: AUTO_VALIDATED

> A partir de este requerimiento la estrategia activa para `LEMMA_ABSENCE` se identifica como `lemma-absence-llm`; el nombre `lemma-absence-mvp` (heredado del comportamiento canned) deja de ser el default observable. Las propuestas archivadas con el nombre viejo siguen siendo legibles e interpretables.

<details><summary>Detail</summary>

1. El `StrategyId.name` que aparece en cada `RevisionProposal` producida tras esta iteracion es `lemma-absence-llm`.
2. El renombre no retira ni reescribe artefactos historicos; los `RevisionProposal` previos con `StrategyId.name = "lemma-absence-mvp"` se preservan tal cual.

El cambio cualitativo de comportamiento (R002) y la trazabilidad por proveedor ([F-LAGEN-R009](#F-LAGEN-R009)) justifican el renombre: cualquier persona que abra una propuesta archivada debe poder distinguir la version canned legacy de la version basada en modelo con solo leer `StrategyId.name`.

</details>

<a id="F-LAGEN-R002"></a>
### Rule[F-LAGEN-R002] - El comportamiento por defecto deja de ser canned
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Tras esta iteracion, los candidatos para `LEMMA_ABSENCE` ya no provienen de respuestas canned (fijas, independientes del contexto): el candidato persistido se obtiene consultando un modelo generativo de lenguaje alimentado con el `LemmaAbsenceCorrectionContext` de la tarea.

<details><summary>Detail</summary>

Cualquier mecanismo previo de respuesta fija deja de ser el comportamiento por defecto observable. Si sobrevive como recurso de testing/desarrollo offline, debe quedar claramente no activado en una corrida normal — esta decision sigue abierta en [DOUBT-CANNED-MODE-AVAILABILITY](#DOUBT-CANNED-MODE-AVAILABILITY).

</details>

---

### Grupo B - Que informacion alimenta al modelo

<a id="F-LAGEN-R003"></a>
### Rule[F-LAGEN-R003] - Toda la informacion poblada del contexto viaja al modelo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La unica fuente de datos para producir el candidato es el `LemmaAbsenceCorrectionContext` de la tarea, y todos los campos poblados de ese contexto deben informar la generacion. El sistema no consulta otras fuentes (`AuditReport`, plan de refinamiento, curso en disco, otros stores o analizadores).

<details><summary>Detail</summary>

Los campos relevantes son los que [F-LAPS-R007](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R007) enumera y FEAT-RCLAQS extiende:

| Campo del contexto | Por que el modelo lo necesita |
|--------------------|-------------------------------|
| `quizSentence` | Para entender la estructura del ejercicio que esta corrigiendo (tronco + blanks + respuestas + variantes), no solo la oracion plana. |
| `sentence` | Oracion plana del ejercicio original; redundante con `quizSentence` pero mas legible para guiar al modelo. |
| `translation` | Traduccion al espanol del ejercicio original; permite preservar el sentido general. |
| `knowledgeTitle` | Tema del knowledge que contiene el quiz. |
| `knowledgeInstructions` | Instrucciones del knowledge (formato, registro, tipo de oracion esperado). |
| `topicLabel` | Label del topic; refuerza el dominio semantico. |
| `cefrLevel` | Nivel CEFR objetivo (A1/A2/B1/B2). Funciona como guia para el modelo, no como criterio de validacion (ver [F-LAGEN-R010](#F-LAGEN-R010)). |
| `misplacedLemmas` | Palabras fuera de nivel del ejercicio original que el modelo debe reemplazar. |
| `suggestedLemmas` | Palabras dentro de nivel sugeridas como reemplazo. |

La forma exacta de presentacion (orden, etiquetas, idioma de las instrucciones, plantillas) es decision de implementacion y puede iterar sin requerir cambio de requerimiento. Lo que no puede iterar es la presencia de cada campo poblado: si en el futuro se agrega un campo nuevo al contexto, la iteracion correspondiente de la generacion debe incorporarlo.

</details>

<a id="F-LAGEN-R004"></a>
### Rule[F-LAGEN-R004] - Al modelo se le exige producir contenido en la DSL `quizSentence` con su traduccion al espanol
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La consulta al modelo debe transmitir tres responsabilidades funcionales: producir un nuevo ejercicio en la misma DSL `quizSentence` que recibio, coherente al `cefrLevel` y reemplazando los `misplacedLemmas` por opciones de `suggestedLemmas` cuando aplique; entregar exactamente un candidato; y devolver un objeto JSON con dos campos exactos `quizSentence` y `translation`, sin texto adicional.

<details><summary>Detail</summary>

1. El modelo esta corrigiendo un ejercicio de cloze de un curso de ingles; debe producir un nuevo ejercicio en la misma DSL `quizSentence` que recibio como input, coherente al `cefrLevel` y reemplazando los `misplacedLemmas` por opciones del repertorio sugerido (`suggestedLemmas`) cuando aplique.
2. El ejercicio nuevo es un solo candidato (no varios), reemplaza integramente al original (no es un parche palabra-por-palabra) y la calidad final no se verifica automaticamente — la responsabilidad cae en el operador / proxima auditoria.
3. La salida exigida es un objeto JSON con dos campos exactos: `quizSentence` (string en la DSL de FEAT-QSENT) y `translation` (string con la traduccion al espanol). Sin texto adicional, sin envoltorios, sin explicaciones.

La redaccion concreta con la que se transmiten estas responsabilidades es decision de implementacion y se espera que itere con el tiempo. Ningun cambio de redaccion debe forzar un cambio de este requerimiento, mientras se preserven las tres responsabilidades anteriores.

</details>

---

### Grupo C - Forma exigida de la respuesta del modelo

<a id="F-LAGEN-R005"></a>
### Rule[F-LAGEN-R005] - Una respuesta es utilizable si y solo si entrega un objeto JSON con `quizSentence` y `translation` no vacios
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La respuesta del modelo se considera utilizable solo si es texto interpretable como objeto JSON, contiene `quizSentence` con string no vacio y contiene `translation` con string no vacio. Cualquier otro caso es malformado y dispara la categoria `LLM_RESPONSE_MALFORMED` de [F-LAGEN-R006](#F-LAGEN-R006).

<details><summary>Detail</summary>

Si la respuesta cumple las tres condiciones, el sistema construye un `LemmaAbsenceGeneratorResponse(quizSentence, translation)` y continua el pipeline. Cualquier otro contenido del JSON (campos extra, comentarios) se ignora silenciosamente.

El sistema **no** valida en este punto el `quizSentence` contra la gramatica de FEAT-QSENT — esa validacion la realiza el deriver de [F-LAPS-R012](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R012) sobre el `LemmaAbsenceQuizCandidate` materializado y, si falla, produce `ProposalDerivationException`, que es un camino post-generacion fuera del alcance de este requerimiento.

</details>

---

### Grupo D - Categorias observables de falla durante la generacion

<a id="F-LAGEN-R006"></a>
### Rule[F-LAGEN-R006] - Toda falla de generacion se reporta con una categoria explicita en el `reason`
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando la generacion no logra producir un `LemmaAbsenceGeneratorResponse`, la falla se propaga via `ProposalStrategyFailedException` (ver [F-LAPS-R015](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R015)) con un `reason` que identifica inequivocamente la categoria de falla.

<details><summary>Detail</summary>

| Categoria | Cuando aplica |
|-----------|---------------|
| `LLM_UNREACHABLE` | El sistema no puede establecer comunicacion con el proveedor del modelo (conexion rechazada, host inalcanzable, error de DNS, endpoint mal configurado). |
| `LLM_TIMEOUT` | La consulta excedio el budget de tiempo configurado ([F-LAGEN-R008](#F-LAGEN-R008)) sin retornar respuesta. |
| `LLM_AUTH_FAILED` | El proveedor rechazo la consulta por credenciales invalidas o ausentes. Irrelevante en un default local sin credencial; relevante cuando el operador apunta a un proveedor que requiere autenticacion. |
| `LLM_RESPONSE_EMPTY` | La consulta termino exitosamente pero el modelo no produjo contenido (string vacio, ausencia total de texto). |
| `LLM_RESPONSE_MALFORMED` | El modelo produjo contenido pero no cumple [F-LAGEN-R005](#F-LAGEN-R005) (no es JSON parseable, falta `quizSentence`, falta `translation`, alguno esta vacio o no es string). |
| `LLM_OTHER` | Cualquier otra falla en tiempo de generacion no contemplada por las categorias anteriores. |

La categoria viaja en el `reason` de modo que sea legible por humanos en stderr y archivable como parte de logs. La forma exacta del string (`reason = "LLM_TIMEOUT"`, `reason = "LLM_TIMEOUT: deadline 30s exceeded"`, `reason = "LLM_RESPONSE_MALFORMED: missing field translation"`) es decision de implementacion, mientras la categoria sea identificable de forma estable.

**Error**: "La estrategia de propuesta 'lemma-absence-llm' no pudo generar un candidato de quiz para la tarea '{taskId}': {categoria}"

</details>

<a id="F-LAGEN-R007"></a>
### Rule[F-LAGEN-R007] - Una falla de generacion no produce candidato parcial ni reintento automatico
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Ante cualquier falla de [F-LAGEN-R006](#F-LAGEN-R006), el sistema no entrega `LemmaAbsenceGeneratorResponse` parcial/default/canned, no reintenta la consulta automaticamente y no envuelve la falla silenciosamente como exito.

<details><summary>Detail</summary>

La excepcion se propaga al dispatcher, que la consolida en el outcome `STRATEGY_FAILED` segun [F-LAPS-R015](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R015) y [F-LAPS-R016](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R016): sin artefacto persistido, sin tocar el curso, la tarea queda en su estado previo. Si el operador quiere un nuevo intento, lo invoca manualmente. Esta regla aplica el limite general de FEAT-LAPS al caso especifico de fallas de generacion contra el modelo.

</details>

---

### Grupo E - Trazabilidad y configuracion del operador

<a id="F-LAGEN-R008"></a>
### Rule[F-LAGEN-R008] - El operador puede ajustar el comportamiento de generacion sin recompilar el sistema
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El operador debe poder cambiar proveedor activo, endpoint, identificador de modelo, credencial, temperatura, tokens maximos y timeout sin recompilar el codigo. El conjunto debe venir con defaults razonables que permitan correr el sistema con un proveedor local levantado sin tocar configuracion.

<details><summary>Detail</summary>

| Perilla | Proposito |
|---------|-----------|
| Proveedor activo | Selecciona contra que proveedor de LLM se realiza la consulta. |
| Endpoint del proveedor | Direccion donde el sistema contacta al proveedor cuando corresponda. |
| Identificador del modelo | Nombre del modelo a usar dentro del proveedor. |
| Credencial de autenticacion | Cuando el proveedor lo exige; ausente cuando el proveedor por defecto local no la requiere. |
| Temperatura | Controla el grado de variabilidad de la respuesta del modelo. |
| Tokens maximos | Limite superior del tamano de la respuesta. |
| Timeout de la consulta | Tiempo maximo antes de declarar `LLM_TIMEOUT` ([F-LAGEN-R006](#F-LAGEN-R006)). |

Cambiar el proveedor activo (de un proveedor local a uno cloud, por ejemplo) debe ser una operacion de configuracion, no un cambio de codigo. Esta regla no fija el mecanismo concreto de configuracion ni los valores numericos; solo exige que las perillas existan y sean tuneables sin recompilar.

</details>

<a id="F-LAGEN-R009"></a>
### Rule[F-LAGEN-R009] - El `StrategyId.providerId` identifica al proveedor concreto detras de cada propuesta
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cada `RevisionProposal` producida tras esta iteracion debe poblar el campo `StrategyId.providerId` (declarado por [F-LAPS-R005](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R005)) con un identificador estable del proveedor de modelo activo, distinto entre proveedores distintos, suficiente para que un consumidor del artefacto archivado distinga el proveedor que produjo el candidato.

<details><summary>Detail</summary>

La regla funcional exige tres cosas:

1. El campo esta poblado (no nulo, no vacio).
2. Dos invocaciones realizadas contra proveedores distintos resultan en `providerId` distintos.
3. El identificador permite a un consumidor del artefacto archivado distinguir el proveedor que produjo el candidato.

El nivel de detalle exacto (solo proveedor, proveedor + modelo, proveedor + modelo + version) queda librado al implementador — ver [DOUBT-PROVIDER-ID-FORMAT](#DOUBT-PROVIDER-ID-FORMAT). Esta regla no redefine el shape del artefacto: `providerId` ya existe en `StrategyId` por [F-LAPS-R005](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R005); este requerimiento se compromete a usarlo.

</details>

---

### Grupo F - Limites explicitos heredados y reafirmados

<a id="F-LAGEN-R010"></a>
### Rule[F-LAGEN-R010] - El sistema no valida la calidad funcional del candidato producido
**Severity**: minor | **Validation**: AUTO_VALIDATED

> Heredando los limites de [F-LAPS-R017](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R017) y [F-LAPS-R018](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R018), el sistema no aplica ninguna verificacion sobre la calidad funcional del candidato. La unica validacion sobre la respuesta del modelo es la estructural definida en [F-LAGEN-R005](#F-LAGEN-R005).

<details><summary>Detail</summary>

Concretamente, el sistema:

1. **No** re-tokeniza el `quizSentence` para chequear su nivel CEFR.
2. **No** mide el largo del `quizSentence` ni lo rechaza por ser demasiado corto/largo.
3. **No** verifica que los `misplacedLemmas` realmente fueron reemplazados.
4. **No** verifica que las palabras nuevas pertenezcan a `suggestedLemmas`.
5. **No** verifica que el `quizSentence` parsee como DSL valida de FEAT-QSENT (eso lo hace el deriver de [F-LAPS-R012](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R012), post-generacion).

</details>

<a id="F-LAGEN-R011"></a>
### Rule[F-LAGEN-R011] - La generacion es no-deterministica por defecto
**Severity**: minor | **Validation**: AUTO_VALIDATED

> Heredando [F-LAPS-R010](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R010), dos invocaciones sucesivas sobre el mismo `LemmaAbsenceCorrectionContext` pueden producir candidatos distintos: la temperatura por defecto del modelo ([F-LAGEN-R008](#F-LAGEN-R008)) y la naturaleza generativa del LLM hacen al output no-deterministico.

<details><summary>Detail</summary>

Esta caracteristica no es un bug; el comportamiento de [F-REVAPR-R010](../2026-04-20.01_refiner-revision-approval/REQUIREMENT.md#F-REVAPR-R010) (rechazar re-revise mientras hay propuesta pendiente) evita que esto se convierta en acumulacion accidental de propuestas distintas.

</details>

---

### Grupo G - Modo de generacion y validacion de la configuracion

<a id="F-LAGEN-R012"></a>
### Rule[F-LAGEN-R012] - El modo de generacion fija (canned) entrega siempre el mismo candidato predeterminado
**Severity**: minor | **Validation**: AUTO_VALIDATED

> Cuando el sistema opera en modo de generacion fija (canned), el candidato producido para una tarea `LEMMA_ABSENCE` es un `quizSentence` y `translation` predeterminados, identicos en cada invocacion, independientes del `LemmaAbsenceCorrectionContext` recibido y sin contactar a ningun proveedor de modelo.

<details><summary>Detail</summary>

Materializa la opcion B de [DOUBT-CANNED-MODE-AVAILABILITY](#DOUBT-CANNED-MODE-AVAILABILITY): el modo canned se conserva como recurso explicito para escenarios sin proveedor de modelo disponible (desarrollo offline, journeys reproducibles del CLI sin dependencia externa). Como contrapartida, su comportamiento observable queda fijo para que su uso sea inequivoco:

1. El candidato devuelto es siempre el mismo par `(quizSentence, translation)` predeterminado, en cualquier invocacion.
2. El contenido del `LemmaAbsenceCorrectionContext` no influye en el candidato producido (a diferencia de [F-LAGEN-R003](#F-LAGEN-R003), que aplica unicamente al modo dinamico).
3. La invocacion no realiza ninguna consulta a un proveedor de modelo y no puede emitir las categorias de falla de [F-LAGEN-R006](#F-LAGEN-R006).

El contenido literal del par predeterminado es decision de implementacion y puede iterar sin requerir cambio de requerimiento; lo que la regla constrine es la idempotencia y la independencia del contexto, no el texto exacto.

</details>

<a id="F-LAGEN-R013"></a>
### Rule[F-LAGEN-R013] - El operador puede elegir el modo de generacion; el default es dinamico y el modo fijo es opt-in explicito
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El operador puede elegir entre dos modos de generacion de candidato: dinamico (consultando un modelo generativo, comportamiento gobernado por [F-LAGEN-R002](#F-LAGEN-R002) y [F-LAGEN-R003](#F-LAGEN-R003)) y fijo / canned (gobernado por [F-LAGEN-R012](#F-LAGEN-R012)). El default observable en una corrida sin configuracion explicita es dinamico; el modo fijo solo se activa cuando el operador lo solicita de forma explicita.

<details><summary>Detail</summary>

1. Una corrida del sistema sin ninguna intervencion del operador sobre el modo opera en modo dinamico — esto preserva [F-LAGEN-R002](#F-LAGEN-R002) (el comportamiento por defecto deja de ser canned) como una garantia visible en cada `revise task <id>`.
2. El modo fijo solo se activa cuando el operador declara explicitamente que lo quiere; nadie debe poder activarlo accidentalmente o por omision.
3. Si el operador solicita un modo no reconocido, el sistema lo reporta de forma observable antes de ejecutar cualquier operacion de revision (consistente con [F-LAGEN-R014](#F-LAGEN-R014)).

Esta regla cierra [DOUBT-CANNED-MODE-AVAILABILITY](#DOUBT-CANNED-MODE-AVAILABILITY) en favor de la opcion B y la documenta como parte de la fuente funcional, de modo que un auditor que abre este requerimiento pueda confirmar la garantia de "default dinamico, canned opt-in" sin recurrir a notas internas.

**Error**: "Modo de generacion no reconocido: '{modo}'. Valores admitidos: dinamico, fijo."

</details>

<a id="F-LAGEN-R014"></a>
### Rule[F-LAGEN-R014] - La configuracion del operador se valida atomicamente antes de ejecutar cualquier operacion de revision
**Severity**: major | **Validation**: AUTO_VALIDATED

> La configuracion declarada por el operador ([F-LAGEN-R008](#F-LAGEN-R008)) y la seleccion del modo ([F-LAGEN-R013](#F-LAGEN-R013)) se interpretan en una sola pasada al iniciar el sistema. Cualquier valor invalido (modo no reconocido, temperatura no numerica, timeout no positivo, tokens maximos no positivo, etc.) se reporta de forma observable antes de ejecutar la primera operacion de revision, no durante.

<details><summary>Detail</summary>

1. La interpretacion de la configuracion ocurre una sola vez al inicio de la corrida; las operaciones posteriores de revision asumen valores ya validados y no vuelven a parsear la configuracion del operador.
2. Si algun valor declarado por el operador es invalido, el sistema reporta el error antes de invocar al modelo, antes de tocar el curso, antes de gastar tokens o tiempo de proveedor, y antes de modificar artefactos archivados.
3. El reporte identifica que perilla resulto invalida y por que; el operador puede corregirlo sin necesidad de rastrear fallas posteriores en logs de revision.

El operador no debe descubrir un typo en su configuracion despues de haber gastado tokens contra un proveedor mal apuntado o despues de que el sistema haya tocado un curso. Esta regla protege la promesa de [F-LAGEN-R008](#F-LAGEN-R008) (perillas tuneables) garantizando que las perillas mal puestas se detecten antes de causar costo o efecto observable.

**Error**: "Configuracion invalida en '{perilla}': {detalle}. Corregir antes de re-ejecutar."

</details>

---

## Contexto

FEAT-LAPS dejo declarada la pieza funcional que produce, para una tarea `LEMMA_ABSENCE`, un `LemmaAbsenceGeneratorResponse` (`quizSentence` en la DSL de FEAT-QSENT + `translation` al espanol) a partir del `LemmaAbsenceCorrectionContext`. Hasta hoy esa pieza devolvia siempre una respuesta canned (fija, no proveniente de un modelo real): el contenido del candidato no dependia del contexto, y permitia ejercitar journeys end-to-end sin corregir contenido linguistico real.

Este requerimiento entrega la **primera version real** de esa pieza: el candidato pasa a producirse consultando un modelo generativo de lenguaje. El resto de la pipeline de revision (deriver, dispatcher, validacion gramatical, persistencia del artefacto, aprobacion humana o auto, escritura al curso) no cambia. Cuando esta iteracion ship, el operador que invoca `revise task <id>` sobre una tarea `LEMMA_ABSENCE` recibe propuestas generadas por un modelo, no propuestas canned, y eso queda documentado en cada artefacto archivado a traves de la identidad de la estrategia ([F-LAGEN-R001](#F-LAGEN-R001)) y del proveedor concreto ([F-LAGEN-R009](#F-LAGEN-R009)).

### Por que cambia la identidad de la estrategia

[F-LAPS-R005](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R005) exige que cada `RevisionProposal` registre el `StrategyId` (`name`, `version`, `providerId` opcional) que produjo el candidato. Hoy ese nombre es `lemma-absence-mvp`, heredado de la epoca canned. Cuando este requerimiento ship, el candidato persistido ya no es canned sino producto de un modelo real, y la trazabilidad documentada en cada propuesta debe reflejar ese cambio cualitativo: cualquier persona que abra una propuesta archivada debe poder distinguir si fue producida por la version canned legacy o por la version basada en modelo. Por eso este requerimiento renombra la estrategia activa a `lemma-absence-llm` ([F-LAGEN-R001](#F-LAGEN-R001)). El `providerId` dentro del `StrategyId` se utiliza para identificar el proveedor concreto del modelo que produjo el candidato ([F-LAGEN-R009](#F-LAGEN-R009)).

### Actor principal

El actor humano relevante es el operador que invoca `revise task <id>` sobre una tarea `LEMMA_ABSENCE`: tras esta iteracion, lo que ese operador ve en el artefacto archivado de la propuesta (`get task <id>`, etc.) es contenido producido por un modelo real, identificado por la nueva identidad de estrategia (`lemma-absence-llm`) y por el `providerId` del proveedor activo.

---

## Alcance

**En scope**:

- Reemplazar el candidato canned por uno producido por un modelo generativo en una sola consulta.
- Renombrar la estrategia activa a `lemma-absence-llm` y poblar `StrategyId.providerId` en cada propuesta.
- Definir las categorias observables de falla durante la generacion ([F-LAGEN-R006](#F-LAGEN-R006)) y el comportamiento ante falla ([F-LAGEN-R007](#F-LAGEN-R007)).
- Exponer las perillas de configuracion del operador ([F-LAGEN-R008](#F-LAGEN-R008)).

**Limitaciones explicitas de esta iteracion** (decisiones de alcance, no reglas de negocio):

- **Single-shot.** Una consulta al modelo, una respuesta, un candidato. Sin orquestacion multi-paso ni grafos de agentes. Si una iteracion futura requiere validacion intermedia o reintento con feedback, sera otra feature.
- **Sin validation loops post-generacion.** El sistema no re-chequea CEFR, no recuenta misplaced lemmas, no re-tokeniza. Heredado de [F-LAPS-R017](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R017) / [F-LAPS-R018](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R018).
- **Un solo candidato por invocacion.** Sin alternatives, sin top-k, sin sampling para que el operador elija.
- **Sin reintentos automaticos ante falla.** El sistema reporta y se detiene; el operador re-invoca manualmente si lo desea.
- **Diseno agnostico de proveedor; un default activo por iteracion.** Esta iteracion entrega un proveedor activo por defecto y deja la puerta abierta a otros proveedores sin cambios de codigo ([F-LAGEN-R008](#F-LAGEN-R008)).
- **Sin persistir lo enviado al modelo ni la respuesta cruda.** La trazabilidad se limita a `StrategyId.name` y `StrategyId.providerId` ([F-LAGEN-R009](#F-LAGEN-R009)). Persistir el contenido completo del intercambio queda como decision futura — DOUBT-PROMPT-PERSISTENCE de FEAT-LAPS sigue abierto y no se cierra aqui.
- **Validacion gramatical fuera de scope.** El sistema no valida el `quizSentence` contra la DSL de FEAT-QSENT. Esa validacion la hace el deriver de [F-LAPS-R012](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R012) y, si falla, produce `ProposalDerivationException` que el dispatcher consolida como `STRATEGY_FAILED`. Este requerimiento gobierna solo fallas de generacion, no fallas de derivacion.
- **Sin metricas, telemetria ni logging estructurado.** Mas alla de los mensajes de error en stderr asociados a las categorias de [F-LAGEN-R006](#F-LAGEN-R006), no se introduce instrumentacion. Ese tipo de monitoreo es decision posterior.

**Fuera de scope**: cualquier cambio al deriver, al dispatcher, al validator, al modelo `RevisionProposal` o a la DSL de FEAT-QSENT.

### Assumptions

1. **`StrategyId.providerId` (declarado por [F-LAPS-R005](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R005)) es suficiente como vehiculo de trazabilidad para el proveedor exigido por [F-LAGEN-R009](#F-LAGEN-R009).** No se extiende el shape del artefacto de propuesta. Razon: FEAT-LAPS ya declaro este campo como mecanismo de trazabilidad opcional; este requerimiento se compromete a usarlo en lugar de inventar un campo nuevo.
2. **`LemmaAbsenceCorrectionContext` con todos sus campos (incluyendo `quizSentence` de FEAT-RCLAQS) es suficiente como insumo para producir un candidato razonable.** Si el modelo necesitara mas contexto (historial del curso, ejemplos few-shot, otras tareas similares), eso seria una extension futura del contexto, fuera del alcance de este requerimiento.
3. **El proveedor por defecto corre localmente y no requiere credenciales.** [F-LAGEN-R008](#F-LAGEN-R008) lista la credencial como perilla configurable; su default es ausente. Razon: development-time el target es local-first.
4. **La temperatura por defecto del modelo es no-cero.** [F-LAGEN-R011](#F-LAGEN-R011) reafirma el no-determinismo. Razon: temperatura cero produce candidatos casi siempre identicos para el mismo input, lo cual elimina la flexibilidad esperada ([F-LAPS-R010](../2026-04-20.02_lemma-absence-proposal-strategy/REQUIREMENT.md#F-LAPS-R010)) y reduce la utilidad practica del modelo para correcciones de cloze.
5. **El renombre de la estrategia activa de `lemma-absence-mvp` a `lemma-absence-llm` no rompe consumidores existentes.** Razon: los unicos consumidores funcionales del nombre son la metadata persistida en cada artefacto y el selector de FEAT-LAPS, que se reconfigura para defaultear al nuevo nombre. Los artefactos historicos con el nombre viejo siguen siendo legibles.
6. **No se requiere que el modelo soporte streaming, function-calling, ni JSON-mode estructurado.** [F-LAGEN-R005](#F-LAGEN-R005) exige una respuesta JSON con dos campos string; cualquier modelo que devuelva texto puede cumplirla via instrucciones en la consulta. Razon: si en el futuro se quiere aprovechar JSON-mode estructurado donde este disponible, esa es una optimizacion compatible con esta regla, no un cambio de requerimiento.

---

## User Journeys

### Journey[F-LAGEN-J001] - El sistema produce un candidato valido en una invocacion
**Validation**: AUTO_VALIDATED

Happy path. El sistema recibe el contexto, consulta al modelo, obtiene una respuesta JSON bien formada con los dos campos requeridos, construye el `LemmaAbsenceGeneratorResponse` y la propuesta archivada termina identificada con la nueva estrategia y el `providerId` del proveedor activo.

```yaml
journeys:
  - id: F-LAGEN-J001
    name: El sistema produce un candidato valido en una invocacion
    flow:
      - id: invocar_generacion
        action: "El operador inicia la revision de una tarea LEMMA_ABSENCE; el sistema toma el LemmaAbsenceCorrectionContext de la tarea"
        gate: [F-LAGEN-R003]
        then: consultar_modelo

      - id: consultar_modelo
        action: "El sistema consulta al modelo generativo activo, transmitiendo toda la informacion poblada del contexto y las responsabilidades funcionales esperadas"
        gate: [F-LAGEN-R002, F-LAGEN-R004]
        then: evaluar_respuesta

      - id: evaluar_respuesta
        action: "El sistema evalua si la respuesta del modelo es un objeto JSON con quizSentence y translation no vacios"
        gate: [F-LAGEN-R005]
        outcomes:
          - when: "La respuesta cumple la estructura exigida"
            then: registrar_propuesta
          - when: "La respuesta no cumple la estructura exigida"
            then: fallar_malformado

      - id: registrar_propuesta
        action: "El sistema persiste la propuesta con StrategyId.name = lemma-absence-llm y StrategyId.providerId del proveedor activo"
        gate: [F-LAGEN-R001, F-LAGEN-R009]
        result: success

      - id: fallar_malformado
        action: "El sistema reporta la falla con categoria LLM_RESPONSE_MALFORMED y no persiste propuesta alguna"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

### Journey[F-LAGEN-J002] - El proveedor del modelo no esta accesible
**Validation**: AUTO_VALIDATED

Cubre la categoria `LLM_UNREACHABLE` de [F-LAGEN-R006](#F-LAGEN-R006): el sistema no logra establecer comunicacion con el proveedor (proveedor local apagado, endpoint mal configurado, error de DNS contra un proveedor cloud).

```yaml
journeys:
  - id: F-LAGEN-J002
    name: El proveedor del modelo no esta accesible
    flow:
      - id: invocar_generacion_unreachable
        action: "El operador inicia la revision de una tarea LEMMA_ABSENCE; el sistema toma un LemmaAbsenceCorrectionContext valido"
        then: intentar_consulta_unreachable

      - id: intentar_consulta_unreachable
        action: "El sistema intenta contactar al proveedor activo pero la comunicacion no se establece"
        then: fallar_unreachable

      - id: fallar_unreachable
        action: "El sistema reporta la falla con categoria LLM_UNREACHABLE; no persiste propuesta y no reintenta"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

### Journey[F-LAGEN-J003] - La consulta al modelo excede el timeout
**Validation**: AUTO_VALIDATED

Cubre la categoria `LLM_TIMEOUT` de [F-LAGEN-R006](#F-LAGEN-R006): la conexion se establece pero la respuesta no llega antes del budget configurado.

```yaml
journeys:
  - id: F-LAGEN-J003
    name: La consulta al modelo excede el timeout
    flow:
      - id: invocar_generacion_timeout
        action: "El operador inicia la revision de una tarea LEMMA_ABSENCE; el sistema toma un LemmaAbsenceCorrectionContext valido"
        then: esperar_respuesta_timeout

      - id: esperar_respuesta_timeout
        action: "El sistema consulta al modelo y espera la respuesta; el budget configurado para la consulta se agota antes de recibir contenido"
        gate: [F-LAGEN-R008]
        then: fallar_timeout

      - id: fallar_timeout
        action: "El sistema reporta la falla con categoria LLM_TIMEOUT; no persiste propuesta y no reintenta"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

### Journey[F-LAGEN-J004] - El proveedor rechaza la consulta por autenticacion
**Validation**: AUTO_VALIDATED

Cubre la categoria `LLM_AUTH_FAILED` de [F-LAGEN-R006](#F-LAGEN-R006): el proveedor responde con una falla de autenticacion (relevante con proveedores que requieren credencial; irrelevante en el default local sin credencial).

```yaml
journeys:
  - id: F-LAGEN-J004
    name: El proveedor rechaza la consulta por autenticacion
    flow:
      - id: invocar_generacion_auth
        action: "El operador inicia la revision en un entorno donde la credencial configurada para el proveedor activo es invalida o esta ausente"
        then: enviar_consulta_auth

      - id: enviar_consulta_auth
        action: "El sistema consulta al proveedor activo; el proveedor rechaza la consulta por credenciales"
        gate: [F-LAGEN-R008]
        then: fallar_auth

      - id: fallar_auth
        action: "El sistema reporta la falla con categoria LLM_AUTH_FAILED; no persiste propuesta y no reintenta"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

### Journey[F-LAGEN-J005] - El modelo responde sin contenido
**Validation**: AUTO_VALIDATED

Cubre la categoria `LLM_RESPONSE_EMPTY` de [F-LAGEN-R006](#F-LAGEN-R006): la consulta termina exitosamente pero el modelo no produjo contenido textual.

```yaml
journeys:
  - id: F-LAGEN-J005
    name: El modelo responde sin contenido
    flow:
      - id: invocar_generacion_empty
        action: "El operador inicia la revision de una tarea LEMMA_ABSENCE; el sistema toma un LemmaAbsenceCorrectionContext valido"
        then: recibir_respuesta_vacia

      - id: recibir_respuesta_vacia
        action: "El proveedor responde exitosamente pero el contenido textual es nulo, vacio o ausente"
        gate: [F-LAGEN-R005]
        then: fallar_empty

      - id: fallar_empty
        action: "El sistema reporta la falla con categoria LLM_RESPONSE_EMPTY; no persiste propuesta y no reintenta"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

### Journey[F-LAGEN-J006] - El modelo responde con contenido que no cumple la estructura exigida
**Validation**: AUTO_VALIDATED

Cubre la categoria `LLM_RESPONSE_MALFORMED` de [F-LAGEN-R006](#F-LAGEN-R006): la respuesta tiene contenido pero no cumple la estructura exigida por [F-LAGEN-R005](#F-LAGEN-R005) (no es JSON parseable, falta `quizSentence`, falta `translation`, o alguno de los dos esta vacio o no es string).

```yaml
journeys:
  - id: F-LAGEN-J006
    name: El modelo responde con contenido que no cumple la estructura exigida
    flow:
      - id: invocar_generacion_malformed
        action: "El operador inicia la revision de una tarea LEMMA_ABSENCE; el sistema toma un LemmaAbsenceCorrectionContext valido"
        then: recibir_respuesta_malformed

      - id: recibir_respuesta_malformed
        action: "El proveedor responde con contenido textual, pero el contenido no es un JSON parseable, o le falta alguno de los campos quizSentence/translation, o alguno de esos campos esta vacio o no es string"
        gate: [F-LAGEN-R005]
        then: fallar_malformed_journey

      - id: fallar_malformed_journey
        action: "El sistema reporta la falla con categoria LLM_RESPONSE_MALFORMED; no persiste propuesta y no reintenta"
        gate: [F-LAGEN-R006, F-LAGEN-R007]
        result: failure
```

---

## Open Questions

<a id="DOUBT-PROVIDER-ID-FORMAT"></a>
### Doubt[DOUBT-PROVIDER-ID-FORMAT] - Que nivel de granularidad expone el `providerId` que ve el consumidor del artefacto archivado?
**Status**: OPEN

[F-LAGEN-R009](#F-LAGEN-R009) exige que el `StrategyId.providerId` de cada propuesta este poblado y que dos invocaciones contra proveedores distintos resulten en `providerId` distintos. La pregunta funcional pendiente es cuanto detalle se expone a un operador que abre el artefacto archivado:

- [ ] Opcion A: **Solo el proveedor** (ejemplos: `local`, `openai`, `anthropic`). Mas estable; un cambio de modelo dentro del mismo proveedor no se refleja en el `providerId`.
- [ ] Opcion B: **Proveedor + modelo** (ejemplos: `local:gemma-2b-it`, `openai:gpt-4o-mini`). Mas detalle para diagnostico; el `providerId` cambia cada vez que el operador cambia de modelo.
- [ ] Opcion C: **Proveedor + modelo + version** (ejemplos: `openai:gpt-4o-mini:2024-07-18`). Maximo detalle; util para reproducibilidad, fragil si la version no es siempre conocida en el momento de la generacion.

**Answer**: Pendiente.

<a id="DOUBT-CANNED-MODE-AVAILABILITY"></a>
### Doubt[DOUBT-CANNED-MODE-AVAILABILITY] - El sistema debe seguir admitiendo un modo de generacion canned para tests / desarrollo offline?
**Status**: RESOLVED

Hoy el sistema produce candidatos canned. Tras esta iteracion el comportamiento por defecto deja de serlo ([F-LAGEN-R002](#F-LAGEN-R002)). La pregunta funcional pendiente era si el operador / desarrollador conserva la posibilidad de pedir explicitamente respuestas canned (por ejemplo para correr journeys de FEAT-LAPS sin un proveedor de LLM disponible):

- [ ] Opcion A: **Se elimina por completo.** Cualquier corrida del sistema requiere un proveedor de modelo disponible.
- [x] Opcion B: **Se conserva como opcion explicita** que el operador puede activar (no es el default; nadie lo activa accidentalmente).
- [ ] Opcion C: **Se conserva pero solo accesible en escenarios de testing**, sin una superficie publica que un operador pueda activar en una corrida normal.

**Answer**: Opcion B. El modo canned se conserva como opcion explicita opt-in del operador. Su comportamiento queda fijado por [F-LAGEN-R012](#F-LAGEN-R012) (idempotente, independiente del contexto, sin contactar proveedor) y la mecanica de seleccion por [F-LAGEN-R013](#F-LAGEN-R013) (default dinamico, canned solo via opt-in explicito).

---

## Referencias

- **FEAT-LAPS** (`requirements/2026-04-20.02_lemma-absence-proposal-strategy/`) — Declara los contratos funcionales que este requerimiento honra: carriers de entrada/salida, identidad de la estrategia, mecanismo de falla via `ProposalStrategyFailedException`, outcome `STRATEGY_FAILED` y limites heredados de validacion. Citado por [F-LAGEN-R003](#F-LAGEN-R003), [F-LAGEN-R005](#F-LAGEN-R005), [F-LAGEN-R006](#F-LAGEN-R006), [F-LAGEN-R007](#F-LAGEN-R007), [F-LAGEN-R009](#F-LAGEN-R009), [F-LAGEN-R010](#F-LAGEN-R010) y [F-LAGEN-R011](#F-LAGEN-R011).
- **FEAT-RCLAQS** (`requirements/2026-04-22.02_rcla-quiz-sentence/`) — Puebla `LemmaAbsenceCorrectionContext.quizSentence` con la DSL del quiz original. Citado por [F-LAGEN-R003](#F-LAGEN-R003) (forma parte de los inputs que viajan al modelo).
- **FEAT-QSENT** (`requirements/2026-04-22.01_quiz-sentence-dsl/`) — Formaliza la DSL `quizSentence`. Este requerimiento no parsea ni valida la DSL: el `quizSentence` que el modelo genera se entrega tal cual al pipeline. Citado por [F-LAGEN-R004](#F-LAGEN-R004) (responsabilidad funcional impuesta al modelo) y [F-LAGEN-R005](#F-LAGEN-R005) / [F-LAGEN-R010](#F-LAGEN-R010) (la validacion gramatical post-generacion la hace el deriver de FEAT-LAPS, no este requerimiento).
- **FEAT-REVAPR** (`requirements/2026-04-20.01_refiner-revision-approval/`) — Define el rechazo de re-revise mientras hay propuesta pendiente. Citado por [F-LAGEN-R011](#F-LAGEN-R011) como mecanismo que evita la acumulacion accidental de candidatos no-deterministicos.
- **FEAT-REVBYP** — Aporta el flujo posterior a la generacion (validacion, persistencia del artefacto, escritura al curso). Se reutiliza tal cual; no se cita inline porque ningun rule introduce restricciones nuevas sobre ese flujo.
