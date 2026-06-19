---
feature:
  id: FEAT-LASAG
  code: F-LASAG
  name: Agente sentinel-agent para generación de candidatos de lemma-absence
  priority: high
---

# Agente sentinel-agent para generación de candidatos de lemma-absence

## TL;DR

Reemplazar la ruta LLM por defecto de *lemma-absence* (single-shot FEAT-LAGEN + interactivo FEAT-LAGAG) por un **único agente sobre sentinel-agent**, que pasa a ser la única ruta LLM. Contrato externo idéntico: entra el `CorrectionContext` de FEAT-RCLA, sale **un único** `QuizCandidate {quizSentence, translation}`. El agente ejecuta un **react corto con UNA sola tool** (la consulta read-only de suggested-lemmas) para filtrar `suggestedWords`. Timeouts/errores del framework se mapean a falla de estrategia preservando categoría, **sin reintentos**.

## Recorrido de uso (estado actual)

Objetivo del consumidor: generar un `QuizCandidate` para un diagnóstico `LEMMA_ABSENCE` por la ruta LLM dentro del flujo audit→plan.

1. El operador ejecuta `PlanCommand.plan(...)` sobre un `AuditTarget`=QUIZ; el flujo necesita una propuesta para el diagnóstico `LEMMA_ABSENCE`.
2. `ProposalStrategySelector.select(...)` elige la estrategia de FEAT-LAPS según el `DiagnosisKind`; `LagenModeResolver.resolve(...)` traduce la env-var a `LagenMode.LLM` (default) y `LagenConfigResolver.resolve(...)` arma la `LagenConfig` (`providerName`, `modelId`, `timeout`, `maxSuggestedLemmaRequests`, …).
3. La estrategia entrega el `CorrectionContext` de FEAT-RCLA (lemma objetivo, `cefrLevel`, `suggestedWords`, `plainSentences`) al `QuizCandidateGenerator` LLM single-shot (FEAT-LAGEN).
4. Ese generador arma el prompt con `suggestedWords` como vocabulario preferido y **filtra/prioriza esos `suggestedWords` a mano dentro del prompt**, hace **una única** llamada al modelo y emite un QuizSentence (QSENT) con su translation.
5. El gate cerrado de FEAT-LAPS valida la salida (parse QSENT + lemma exacto en `[[...]]` + translation no vacía); si el generador falla, se propaga como `ProposalStrategyFailedException` preservando categoría, sin reintentos.
6. Para que el modelo **consulte/filtre `suggestedWords` mid-generación** en lugar de resolverlo a mano, hoy hay que cambiar a la **otra** ruta: `LagenMode.LLM_INTERACTIVE` (FEAT-LAGAG), una estrategia distinta con su propio `StrategyId.name` (`lemma-absence-llm-interactive`), que abre una sesión de consultas usando `GetCommand.get("suggested-lemmas", name, SuggestedLemmasFilter)` (semántica F-CSLATDC).

**Friction point**: hoy coexisten **dos rutas LLM divergentes** — single-shot (filtra `suggestedWords` a mano en el prompt) e interactivo (consulta suggested-lemmas mid-generación) — con dos `StrategyId.name`, dos wirings y dos provenances. El filtrado que el prompt single-shot resuelve manualmente **no reutiliza** la consulta read-only de suggested-lemmas que ya existe.

**Cambio mínimo necesario**: un único agente sobre sentinel-agent cableado bajo `LagenMode.LLM` que ejecuta un **react corto con UNA sola tool** — la consulta `GetCommand.get("suggested-lemmas", …, SuggestedLemmasFilter)` (semántica F-CSLATDC) para listar/filtrar `suggestedWords` — unificando single-shot e interactivo como única ruta LLM. Mantiene el contrato externo (`CorrectionContext` in → un `QuizCandidate` out) y el mapeo de fallas (FEAT-LAPS R015 preservando categoría, R016 sin reintentos). Presupuesto de consultas acotado, **exhaustion no es falla** (patrón FEAT-LAGAG).

## Business Rules

<a id="F-LASAG-R001"></a>
### Rule[F-LASAG-R001] - El filtrado de suggestedWords pasa por la tool de suggested-lemmas
**Severity**: high | **Validation**: AUTO_VALIDATED

> Dado un escenario controlado que conduce al agente a refinar `suggestedWords`, durante la generación del candidato el sistema invoca `GetCommand.get` con `resource == "suggested-lemmas"` y un `SuggestedLemmasFilter` (semántica F-CSLATDC) al menos una vez.

<details><summary>Detail</summary>

- **Today**: el single-shot (FEAT-LAGEN) filtra/prioriza `suggestedWords` a mano dentro del prompt y nunca llama a la consulta read-only de suggested-lemmas.
- **After**: el agente expresa ese filtrado como una invocación de la tool de suggested-lemmas.
- **Failing test**: en un escenario que fuerza al agente a refinar vocabulario, un `verify` sobre `GetCommand.get(resource=="suggested-lemmas", …)` falla hoy (jamás se invoca) y pasa con el agente (≥1 invocación).

El *at-least-once* se afirma sobre un escenario controlado, no como invariante de toda generación: el react es discrecional. Las cotas universales viven en R002 y R004.

</details>

<a id="F-LASAG-R002"></a>
### Rule[F-LASAG-R002] - Ningún otro verbo CLI durante la generación
**Severity**: high | **Validation**: AUTO_VALIDATED

> Durante la fase de generación del candidato, el sistema no invoca ningún verbo CLI distinto de `GetCommand.get` con `resource == "suggested-lemmas"`: en particular no invoca `AnalyzeCommand.analyze`, `PlanCommand.plan`, `ReviseCommand.revise`, `ApproveCommand.approve`, `RejectCommand.reject`, ni `GetCommand.get` con otro `resource`.

<details><summary>Detail</summary>

Cota universal (vale para toda generación, no solo el escenario controlado de R001). Esta cota acota **solo la fase de generación**; la fase de evals (R007) puede legítimamente correr el analyzer de length y llamadas LLM sin violarla (ver R012).

**Failing test**: `verify(never)` sobre cada verbo CLI ≠ `get suggested-lemmas` durante la generación; cualquier invocación adicional rompe la regla.

</details>

<a id="F-LASAG-R003"></a>
### Rule[F-LASAG-R003] - Contrato externo idéntico: un único QuizCandidate
**Severity**: high | **Validation**: AUTO_VALIDATED

> Dado un `CorrectionContext` de FEAT-RCLA para un diagnóstico `LEMMA_ABSENCE`, el agente consume esa entrada y devuelve exactamente un `QuizCandidate {quizSentence, translation}` — ni cero ni más de uno —, con ambos campos presentes.

<details><summary>Detail</summary>

El delta es el backend (sentinel-agent), no la firma del puerto `QuizCandidateGenerator` (FEAT-LAPS R001–R003). El agente es nueva superficie que debe respetar el contrato existente.

**Failing test**: invocar el generador con un `CorrectionContext` válido y assertear que la salida es una colección/valor de cardinalidad 1 con `quizSentence` y `translation` no nulos.

</details>

<a id="F-LASAG-R004"></a>
### Rule[F-LASAG-R004] - Presupuesto de tool por run; agotarlo no es falla
**Severity**: medium | **Validation**: AUTO_VALIDATED

> El número de invocaciones a `GetCommand.get("suggested-lemmas", …)` en un único run de generación no excede `maxSuggestedLemmaRequests` (de `LagenConfig`). Alcanzar ese tope solo impide nuevas consultas: el agente continúa y emite su candidato sin producir ninguna falla.

<details><summary>Detail</summary>

- Presupuesto **por-run de generación**: se resetea en cada reintento de eval (R008); el "no repetir pasos" se logra por carry-forward (R010), no por presupuesto compartido.
- A diferencia del patrón FEAT-LAGAG, no existe una categoría de falla "presupuesto de tool agotado": toda falla real es runtime (R005/R012) o la atrapa el gate estructural de FEAT-LAPS.

**Failing test**: configurar `maxSuggestedLemmaRequests = N`, forzar al agente a consultar hasta el tope y assertear que (a) hay ≤ N invocaciones de la tool en el run y (b) se emite un `QuizCandidate` sin excepción.

</details>

<a id="F-LASAG-R005"></a>
### Rule[F-LASAG-R005] - Error de runtime en generación → ProposalStrategyFailedException con categoría preservada
**Severity**: high | **Validation**: AUTO_VALIDATED

> Un error del runtime sentinel-agent durante la generación (timeout, auth, transporte u otro) produce un `ProposalStrategyFailedException` cuya categoría preserva el origen (`LLM_TIMEOUT`, `LLM_AUTH_FAILED`, `LLM_TRANSPORT`, `LLM_OTHER`); el sistema no degrada a propuesta vacía ni a candidato nulo.

<details><summary>Detail</summary>

Honra FEAT-LAPS R015 (preservar categoría) sobre el nuevo backend.

**Error**: `ProposalStrategyFailedException` con el prefijo de categoría correspondiente al error de runtime.

**Failing test**: inyectar un timeout del runtime durante la generación y assertear que se lanza `ProposalStrategyFailedException` con categoría `LLM_TIMEOUT` (y no un `QuizCandidate` vacío).

</details>

<a id="F-LASAG-R006"></a>
### Rule[F-LASAG-R006] - Un único intento de generación ante falla de runtime
**Severity**: high | **Validation**: AUTO_VALIDATED

> Ante una falla de runtime durante la generación, el sistema propaga la excepción de inmediato y no relanza la generación. Los turnos tool↔modelo del react dentro de un mismo run no son reintentos.

<details><summary>Detail</summary>

Honra FEAT-LAPS R016 (sin reintentos) acotado al **run de generación ante falla de runtime**; no veda el lazo de eval (R008), que se dispara por veredictos, no por fallas de infraestructura (R012).

**Failing test**: inyectar una falla de runtime en la primera generación y assertear que el runtime del agente se ejecuta exactamente una vez (no hay segundo run de generación) y la excepción se propaga.

</details>

<a id="F-LASAG-R007"></a>
### Rule[F-LASAG-R007] - Catálogo y clasificación de evals de calidad sobre el candidato emitido
**Severity**: high | **Validation**: AUTO_VALIDATED

> Tras pasar el gate estructural de FEAT-LAPS, el candidato se somete a un catálogo fijo de evals con veredicto propio cada uno, e independientemente testeable:
> 1. `quizSentence` es una oración con sentido — **bloqueante**.
> 2. el quiz es resoluble y sigue las instrucciones del ejercicio — **bloqueante**.
> 3. no-regresión de length: `length(candidate) ≥ length(original)` medido con el analyzer de length del audit sobre el quiz original pre-corrección — **bloqueante**.
> 4. pragmatismo (la oración es usable/útil en la vida real) — **deseable**: puntúa, no bloquea.
> 5. la traducción es fiel — **bloqueante**.
> Un veredicto bloqueante negativo impide emitir ese candidato como salida final; un veredicto deseable negativo no.

<details><summary>Detail</summary>

- El eval #3 es determinista y **reutiliza** el analyzer de length que ya corre en el audit de quizzes, comparando contra el baseline pre-corrección; los evals #1, #2, #4, #5 son juicios LLM.
- *Validation hint (QA)*: #1, #2, #4, #5 vía `llm_judge`; #3 vía script determinista. El comportamiento bloqueante/deseable se testea con evals stub de veredicto controlado.

**Failing test**: con eval #3 reusando el analyzer, construir un candidato más corto que el original y assertear veredicto negativo bloqueante; con un candidato que reprueba solo #4 (deseable), assertear que igualmente se emite.

</details>

<a id="F-LASAG-R008"></a>
### Rule[F-LASAG-R008] - Reintento de generación por veredicto bloqueante, acotado por maxEvalRetries
**Severity**: high | **Validation**: AUTO_VALIDATED

> Un veredicto bloqueante negativo (el eval corrió y reprobó) dispara un nuevo run de generación completo del agente (incluido su react con la tool). El número de reintentos de generación disparados por evals en una resolución no excede `maxEvalRetries` (de `LagenConfig`).

<details><summary>Detail</summary>

Lazo **separado** del de runtime: una falla de runtime nunca entra a este lazo (R012). Cada reintento es una regeneración completa, no una regeneración ciega (ver carry-forward, R010), y resetea el presupuesto de tool por-run (R004).

**Failing test**: con un eval bloqueante forzado a reprobar en el primer intento, assertear que ocurre un segundo run de generación; con `maxEvalRetries = K` y todos los intentos reprobando, assertear que se disparan a lo sumo K reintentos.

</details>

<a id="F-LASAG-R009"></a>
### Rule[F-LASAG-R009] - Exhaustion de reintentos de eval: best-effort, no es falla
**Severity**: high | **Validation**: AUTO_VALIDATED

> Al agotar `maxEvalRetries` sin obtener un candidato que pase todos los evals bloqueantes, el sistema no falla: emite el mejor candidato visto. El "mejor" se elige por (1) mayor cantidad de evals bloqueantes pasados y, a igualdad, (2) mayor score del eval deseable #4 (pragmatismo).

<details><summary>Detail</summary>

Todo candidato candidato a "mejor" **ya pasó el gate estructural de FEAT-LAPS**; el best-effort opera solo sobre la capa de calidad.

**Failing test**: forzar que todos los intentos reprueben algún bloqueante, con un intento que pasa más bloqueantes que los demás, y assertear que (a) se emite un `QuizCandidate` sin excepción y (b) es ese intego con más bloqueantes pasados (con dos empatados, el de mayor score de pragmatismo).

</details>

<a id="F-LASAG-R010"></a>
### Rule[F-LASAG-R010] - Carry-forward de contexto de fallo e historial de tool entre reintentos
**Severity**: medium | **Validation**: AUTO_VALIDATED

> Cada reintento de generación recibe como entrada adicional: (i) el contexto de fallo de los evals del intento previo (qué bloqueantes reprobaron y por qué) y (ii) el historial de llamadas a la tool previas (filtros usados y lemmas ya obtenidos).

<details><summary>Detail</summary>

Objetivo funcional: el nuevo intento mejora la propuesta y no repite pasos; el retry no es regeneración ciega.

**Failing test**: forzar un reintento (R008) y assertear que la entrada del segundo run de generación contiene el contexto de fallo de los evals del primer intento y el historial de `SuggestedLemmasFilter`/lemmas de las invocaciones de tool previas.

</details>

<a id="F-LASAG-R011"></a>
### Rule[F-LASAG-R011] - Perilla maxEvalRetries con default 3, ignorada por CANNED
**Severity**: medium | **Validation**: AUTO_VALIDATED

> `LagenConfigResolver.resolve(...)` produce una `LagenConfig` con `maxEvalRetries`; sin env-var explícita el valor es 3. Bajo `CANNED` el modo emite su candidato sin disparar ningún reintento de eval, con independencia del valor de `maxEvalRetries`.

<details><summary>Detail</summary>

Análogo a `maxSuggestedLemmaRequests`.

**Failing test**: resolver la config sin la env-var de reintentos y assertear `maxEvalRetries == 3`; ejecutar el modo `CANNED` con `maxEvalRetries` alto y assertear que no ocurre ningún reintento de generación.

</details>

<a id="F-LASAG-R012"></a>
### Rule[F-LASAG-R012] - Error de runtime en cualquier fase = falla inmediata fuera del lazo de retry
**Severity**: high | **Validation**: AUTO_VALIDATED

> Un error de runtime en **cualquier fase** (generación o evals) produce un `ProposalStrategyFailedException` inmediato con categoría preservada y no dispara el lazo de reintentos de eval. Solo un veredicto bloqueante negativo (el eval corrió y reprobó) dispara un reintento.

<details><summary>Detail</summary>

Distingue infraestructura (siempre falla inmediata, R005 extendida a la fase de evals) de veredicto (dispara retry, R008). Las llamadas LLM de los evals #1/#2/#4/#5 y el analyzer del eval #3 viven fuera de la cota de "única tool" (R002).

**Error**: `ProposalStrategyFailedException` con categoría preservada.

**Failing test**: inyectar un timeout en la llamada LLM de un eval (fase de evals) y assertear que se lanza `ProposalStrategyFailedException` con categoría preservada y que no se dispara ningún reintento de generación; contrastar con un veredicto bloqueante negativo, que sí dispara reintento.

</details>

## Context

La ruta LLM de *lemma-absence* está hoy partida en dos implementaciones del puerto `QuizCandidateGenerator`: el single-shot FEAT-LAGEN (filtra `suggestedWords` dentro del prompt, una sola llamada) y el interactivo FEAT-LAGAG (sesión de consultas mid-generación contra suggested-lemmas). Cada una arrastra su propio `StrategyId.name`, su wiring por `LagenMode` y su provenance, duplicando la lógica de prompt/selección de vocabulario.

La disponibilidad del runtime **sentinel-agent** permite expresar ambas como un **mismo agente**: un react acotado que decide cuándo consultar `suggestedWords` mediante una única tool en lugar de filtrarlos a mano o mantener una sesión interactiva separada. El delta consiste en sustituir el backend de la ruta LLM por este agente, conservando intacto el contrato del puerto (FEAT-LAPS R001–R003) y la política de fallas (R015/R016). `CANNED` se conserva como opt-in offline.

## Scope

**En alcance**

- Implementación del puerto `QuizCandidateGenerator` (FEAT-LAPS R001–R003) sobre sentinel-agent, con **contrato externo idéntico**: entrada `CorrectionContext` de FEAT-RCLA, salida **un único** `QuizCandidate {quizSentence, translation}`.
- React corto con **UNA sola tool**: listar/filtrar `suggestedWords` reutilizando `GetCommand.get("suggested-lemmas", …, SuggestedLemmasFilter)` (semántica F-CSLATDC), sustituyendo el filtrado manual del prompt.
- Presupuesto de consultas acotado; **exhaustion no es falla** (patrón FEAT-LAGAG R004/R005).
- Mapeo de timeouts/errores del framework sentinel-agent a `ProposalStrategyFailedException` preservando categoría (FEAT-LAPS R015), **sin reintentos** (R016): un único intento de generación.
- Absorción de la ruta interactiva (FEAT-LAGAG) y del single-shot (FEAT-LAGEN) en una única ruta LLM.

**Fuera de alcance**

- Modificar FEAT-RCLA, FEAT-QSENT/el DSL de QuizSentence, REVBYP, REVAPR.
- Otros `DiagnosisKind` distintos de `LEMMA_ABSENCE`.
- Reintentos, auto-validación, composición de estrategias o funcionalidad de producto nueva.
- Cambiar la firma del puerto `QuizCandidateGenerator` o la forma de selección por `DiagnosisKind`.

## User Journeys

```yaml
journeys:
  - id: F-LASAG-J001
    name: Generación exitosa de QuizCandidate por la ruta LLM (agente sentinel-agent)
    flow:
      - id: preparar_contexto
        action: "Se prepara un CorrectionContext válido de FEAT-RCLA para un diagnóstico LEMMA_ABSENCE con suggestedWords a refinar"
        then: generar
      - id: generar
        action: "Se invoca el puerto QuizCandidateGenerator con el CorrectionContext; el agente ejecuta su react corto consultando la tool de suggested-lemmas para filtrar suggestedWords"
        gate: [F-LASAG-R001, F-LASAG-R002]
        then: evaluar
      - id: evaluar
        action: "El candidato pasa el gate estructural de FEAT-LAPS y se somete al catálogo fijo de evals de calidad"
        gate: [F-LASAG-R007]
        outcomes:
          - when: "todos los evals bloqueantes (#1, #2, #3, #5) dan veredicto positivo"
            then: emitir
      - id: emitir
        action: "Se devuelve exactamente un QuizCandidate {quizSentence, translation} con ambos campos presentes"
        gate: [F-LASAG-R003]
        result: success

  - id: F-LASAG-J002
    name: Agotar el presupuesto de tool no es falla
    flow:
      - id: configurar_presupuesto
        action: "Se configura maxSuggestedLemmaRequests = N en la LagenConfig"
        then: generar
      - id: generar
        action: "Se invoca QuizCandidateGenerator y se conduce al agente a consultar la tool de suggested-lemmas hasta alcanzar el tope N"
        gate: [F-LASAG-R002, F-LASAG-R004]
        outcomes:
          - when: "el agente alcanza el tope de N consultas a la tool"
            then: continuar
      - id: continuar
        action: "Alcanzar el tope solo corta nuevas consultas; el agente continúa sin producir ninguna falla"
        then: emitir
      - id: emitir
        action: "Se emite un QuizCandidate sin excepción, con a lo sumo N invocaciones de la tool en el run"
        gate: [F-LASAG-R004, F-LASAG-R003]
        result: success

  - id: F-LASAG-J003
    name: Error de runtime en generación falla con categoría preservada e intento único
    flow:
      - id: preparar
        action: "Se prepara un CorrectionContext válido y se inyecta un error del runtime sentinel-agent (p.ej. timeout) durante la generación"
        then: generar
      - id: generar
        action: "Se invoca QuizCandidateGenerator; el runtime del agente falla durante la fase de generación"
        gate: [F-LASAG-R005, F-LASAG-R006]
        outcomes:
          - when: "el runtime arroja timeout/auth/transporte/otro error"
            then: fallar
      - id: fallar
        action: "Se propaga un ProposalStrategyFailedException con categoría preservada (p.ej. LLM_TIMEOUT), sin candidato vacío/nulo y sin un segundo run de generación"
        gate: [F-LASAG-R005, F-LASAG-R006]
        result: failure

  - id: F-LASAG-J004
    name: Veredicto bloqueante negativo dispara reintento con carry-forward, acotado por maxEvalRetries
    flow:
      - id: preparar
        action: "Se prepara un CorrectionContext válido, se configura maxEvalRetries = K y se fuerza que un eval bloqueante repruebe en el primer intento"
        then: generar
      - id: generar
        action: "El agente ejecuta un run de generación completo (react con la tool) y pasa el gate estructural"
        then: evaluar
      - id: evaluar
        action: "El candidato se somete al catálogo de evals"
        gate: [F-LASAG-R007]
        outcomes:
          - when: "un eval bloqueante corrió y reprobó, y aún quedan reintentos dentro de maxEvalRetries"
            then: reintentar
          - when: "todos los evals bloqueantes pasan"
            then: emitir
      - id: reintentar
        action: "Se dispara un nuevo run de generación completo del agente que recibe como entrada adicional el contexto de fallo de los evals previos y el historial de llamadas a la tool (filtros usados y lemmas obtenidos)"
        gate: [F-LASAG-R008, F-LASAG-R010]
        then: evaluar
      - id: emitir
        action: "Se emite el QuizCandidate que pasa todos los bloqueantes, habiéndose disparado a lo sumo K reintentos de generación"
        gate: [F-LASAG-R008, F-LASAG-R003]
        result: success

  - id: F-LASAG-J005
    name: Exhaustion de reintentos de eval es best-effort, no falla
    flow:
      - id: preparar
        action: "Se configura maxEvalRetries y se fuerza que todos los intentos reprueben algún bloqueante, con un intento que pasa más bloqueantes que los demás"
        then: generar_evaluar
      - id: generar_evaluar
        action: "El agente genera y evalúa candidatos en cada reintento hasta agotar maxEvalRetries"
        gate: [F-LASAG-R008]
        outcomes:
          - when: "se agotan los reintentos sin candidato que pase todos los bloqueantes"
            then: emitir_mejor
      - id: emitir_mejor
        action: "Se emite sin excepción el mejor candidato visto: el de mayor cantidad de evals bloqueantes pasados y, a igualdad, el de mayor score del eval deseable #4 (pragmatismo)"
        gate: [F-LASAG-R009, F-LASAG-R003]
        result: success

  - id: F-LASAG-J006
    name: Veredicto deseable negativo no bloquea la emisión
    flow:
      - id: preparar
        action: "Se prepara un candidato que pasa todos los evals bloqueantes y reprueba solo el eval deseable #4 (pragmatismo)"
        then: evaluar
      - id: evaluar
        action: "El candidato se somete al catálogo de evals"
        gate: [F-LASAG-R007]
        outcomes:
          - when: "solo el eval deseable #4 da veredicto negativo y todos los bloqueantes pasan"
            then: emitir
      - id: emitir
        action: "Se emite igualmente el QuizCandidate sin disparar ningún reintento de generación"
        gate: [F-LASAG-R007, F-LASAG-R003]
        result: success

  - id: F-LASAG-J007
    name: Error de runtime en la fase de evals falla de inmediato fuera del lazo de retry
    flow:
      - id: preparar
        action: "Se prepara un candidato que pasa el gate estructural y se inyecta un timeout en la llamada LLM de un eval (fase de evals)"
        then: evaluar
      - id: evaluar
        action: "Durante la fase de evals el runtime falla"
        gate: [F-LASAG-R012]
        outcomes:
          - when: "un eval sufre un error de runtime (timeout/transporte/otro)"
            then: fallar
      - id: fallar
        action: "Se propaga un ProposalStrategyFailedException con categoría preservada sin disparar ningún reintento de generación"
        gate: [F-LASAG-R012]
        result: failure

  - id: F-LASAG-J008
    name: Eval no-regresión de length bloquea un candidato más corto
    flow:
      - id: preparar
        action: "Se construye un candidato más corto que el quiz original pre-corrección, medido con el analyzer de length del audit"
        then: evaluar
      - id: evaluar
        action: "El eval #3 reusa el analyzer de length del audit y compara el candidato contra el baseline pre-corrección"
        gate: [F-LASAG-R007]
        outcomes:
          - when: "length(candidate) < length(original)"
            then: bloquear
      - id: bloquear
        action: "El eval #3 emite veredicto negativo bloqueante; ese candidato no puede emitirse como salida final"
        gate: [F-LASAG-R007]
        result: failure

  - id: F-LASAG-J009
    name: Perilla maxEvalRetries con default 3, ignorada por CANNED
    flow:
      - id: resolver_sin_envvar
        action: "Se invoca LagenConfigResolver.resolve(...) sin la env-var de reintentos de eval"
        gate: [F-LASAG-R011]
        outcomes:
          - when: "no hay env-var explícita de maxEvalRetries"
            then: assert_default
      - id: assert_default
        action: "La LagenConfig resultante tiene maxEvalRetries == 3"
        gate: [F-LASAG-R011]
        then: ejecutar_canned
      - id: ejecutar_canned
        action: "Se ejecuta el modo CANNED con un valor alto de maxEvalRetries"
        gate: [F-LASAG-R011]
        outcomes:
          - when: "el modo activo es CANNED"
            then: emitir
      - id: emitir
        action: "CANNED emite su candidato sin disparar ningún reintento de generación, con independencia del valor de maxEvalRetries"
        gate: [F-LASAG-R011]
        result: success
```

## Open Questions

### Doubt[DOUBT-GATE-AGENTE] - ¿El agente conserva el gate de validación cerrado de FEAT-LAPS sin cambios?
- [x] Sí: se mantiene el gate de FEAT-LAPS (R004/R005: parse QSENT + lemma exacto en `[[...]]` + translation no vacía) tal cual, fuera del agente (supuesto base, dado que auto-validación está fuera de alcance)
- [ ] Se mueve/duplica el gate dentro del agente
- [ ] Se relaja parcialmente

**Answer**: Sí. El gate estructural de FEAT-LAPS se conserva **sin cambios y fuera del agente**; el nuevo backend sentinel-agent no lo mueve ni lo duplica.

### Doubt[DOUBT-TRAZABILIDAD-AGENTE] - ¿Cómo se expone la metadata de trazabilidad del agente (modelo, config, prompt) en la provenance?
- [ ] Se reutiliza el formato `provider:model` en `StrategyId.providerId` y se mantiene `lemma-absence-llm` como `StrategyId.name`
- [ ] Se renombra la estrategia para reflejar el agente
- [ ] Se añade metadata extendida (config/prompt) a la provenance

*Pendiente: la petición lo declara como duda abierta; no se resuelve en este requirement.*

### Doubt[DOUBT-WIRING-MODO] - ¿El agente se cablea bajo `LagenMode.LLM` retirando `LLM_INTERACTIVE` y el single-shot previo?
- [x] Sí: el agente reemplaza el backend de `LagenMode.LLM` (mismo nombre) y se retiran `LLM_INTERACTIVE` y el single-shot anterior; `CANNED` se conserva (supuesto base)
- [ ] Se introduce un modo nuevo en el enum `LagenMode`
- [ ] Coexiste temporalmente con las rutas anteriores

**Answer**: Sí. El agente reemplaza el backend de `LagenMode.LLM` conservando `StrategyId.name = lemma-absence-llm`; se retiran `LLM_INTERACTIVE` y su `lemma-absence-llm-interactive`, y desaparece el single-shot previo. `LagenModeResolver.resolve(...)` deja de resolver el valor interactivo; `ProposalStrategySelector.select(...)` y la selección por `DiagnosisKind` no cambian. `CANNED` se conserva como opt-in offline. Contrato externo idéntico.

## References

- **FEAT-LAPS** (`2026-04-20.02_lemma-absence-proposal-strategy`) — contrato del puerto `QuizCandidateGenerator` (R001–R003), gate de validación cerrado (R004/R005), falla del generador → `ProposalStrategyFailedException` preservando categoría (R015) y sin reintentos (R016). El agente debe respetarlo sin cambios.
- **FEAT-LAGEN** (`2026-04-29.01_laps-llm-quiz-candidate-generator`) — generador LLM single-shot que se reemplaza: prompt desde `CorrectionContext`, `suggestedWords` como vocabulario preferido, taxonomía de fallas, provenance `provider:model`, wiring por `LagenMode`.
- **FEAT-LAGAG** (`2026-05-29.01_generacion-interactiva-suggested-lemmas`) — variante interactiva que se absorbe: consultas mid-generación a suggested-lemmas, presupuesto acotado y exhaustion no-falla. Antecedente directo del react con UNA tool.
- **F-CSLATDC** (`2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli`) — semántica de la única tool: consulta read-only de SuggestedLemmas con filtros level/POS/limit, inferencia de level, filtro cerrado, sin efectos colaterales.
- **FEAT-RCLA** (`2026-04-13.01_refiner-correction-context-labs`) — fuente del `CorrectionContext`. Fuera de alcance modificarlo; solo se consume.