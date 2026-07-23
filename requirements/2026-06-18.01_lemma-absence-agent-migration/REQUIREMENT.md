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

Cota universal (vale para toda generación, no solo el escenario controlado de R001). Esta cota acota **solo la fase de generación**; la fase de evals (R007) puede legítimamente correr el analyzer de length (criterio #3), la consulta léxica del análisis (criterio #7) y llamadas LLM sin violarla (ver R012).

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

> Alcanzar el tope de consultas de vocabulario configurado para un run de generación solo impide nuevas consultas: el consumidor recibe igualmente exactamente un `QuizCandidate`, sin ninguna falla. El consumidor nunca observa más vocabulario consultado del presupuesto declarado.

<details><summary>Detail</summary>

Esta regla tiene dos mitades con superficies de validación distintas:

- **Observable en el límite del sistema (AUTO_VALIDATED, suite JUnit)**: "agotar el presupuesto no es falla". Configurado `maxSuggestedLemmaRequests = N`, el consumidor recibe un `QuizCandidate` sin excepción aunque el agente haya alcanzado el tope. Esta es la mitad cubierta por los handwrittenTests existentes.
- **Conducta interna del agente declarativo (validada por su harness de evals, no por JUnit)**: el conteo efectivo `≤ N` de consultas dentro de un run, su reseteo en cada reintento de eval (R008) y el "no repetir pasos" por carry-forward (R010) son disciplina del lazo react del agente; el consumidor no observa el conteo, solo el resultado.

A diferencia del patrón FEAT-LAGAG, no existe una categoría de falla "presupuesto de tool agotado": toda falla real es runtime (R005/R012) o la atrapa el gate estructural de FEAT-LAPS.

**Validación (mitad observable)**: configurar `maxSuggestedLemmaRequests = N`, conducir al agente hasta el tope y assertear que se emite un `QuizCandidate` sin excepción.

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
**Severity**: high | **Validation**: VALIDATED *(validada por el harness de evals del agente, no por el suite JUnit)*

> Antes de entregarse como salida final, todo candidato se somete a un catálogo fijo de criterios de calidad, cada uno con veredicto propio e independiente:
> 1. el `quizSentence` es correcto y está bien formado **para el mismo tipo/molde de ejercicio que el quiz original** — **bloqueante**. Si el original es una oración, debe ser una oración con sentido; si el original es de otro tipo (transformación, par `palabra → forma`, construcción por cues), debe ser un ejemplar válido de ese tipo, aunque no sea una oración. El candidato debe respetar el tipo del ejercicio original, no convertirlo siempre a oración.
> 2. el quiz es resoluble y sigue las instrucciones del ejercicio — **bloqueante**.
> 3. la longitud del candidato está **dentro del rango esperado** (target range) del nivel, medida con el mismo criterio de longitud que aplica el audit; no importa si queda más corto o más largo que el quiz original — **bloqueante**.
> 4. pragmatismo (la oración es usable/útil en la vida real) — **deseable**: puntúa, no bloquea.
> 5. la traducción es fiel — **bloqueante**.
> 6. reutilización de la respuesta finita: cuando la respuesta del hueco (`[...]`) del quiz original pertenece a un conjunto cerrado/finito (p.ej. formas de *to be* am/is/are/was/were; clasificación de contabilidad countable/uncountable/singular countable/plural countable; existenciales there is/there are; artículos a/an/the; demostrativos this/that/these/those; auxiliares do/does/did, have/has; cuantificadores cerrados some/any, much/many) **y** el ejercicio admite seguir usando esa misma respuesta, el candidato debe reutilizar la misma respuesta del original (no cambiar la categoría de la respuesta) — **bloqueante (cuando aplica)**. Es N/A (no aplica, no bloquea) cuando la respuesta del original es de vocabulario abierto o cuando reutilizarla no daría un ejercicio válido/natural; en ese caso el veredicto es positivo.
> 7. no-regresión léxica: el candidato **no introduce** palabras **mal ubicadas para el nivel** ni **fuera de catálogo** que la oración original no tuviera ya — **bloqueante**. Verificado de forma **determinista** con el **mismo análisis léxico del audit** (mismo tokenizador y mismo catálogo con todas sus reglas de nivel, no una réplica aproximada): el conjunto de lemas flaggeados del candidato debe estar **contenido** en el de la oración original, `flags(candidato) ⊆ flags(original)`. Los flags preexistentes que la edición no tocó **no bloquean** (una tarea con residuo inevitable no se vuelve infalible por ello); solo bloquea un lema flaggeado que aparece en el candidato **sin estar ya flaggeado en el original**. Al reprobar, el contexto de fallo del reintento nombra las palabras infractoras **con su nivel** (p.ej. "transfer es B1, el quiz es A1 — usá un verbo A1 que colocacione").
> Un veredicto bloqueante negativo impide entregar ese candidato como salida final; un veredicto deseable negativo no.

<details><summary>Detail</summary>

**Superficie de validación**: este catálogo y su clasificación bloqueante/deseable son conducta del agente declarativo y **no tienen superficie observable en el suite JUnit** del adaptador Java: el consumidor invoca el generador y recibe un único `QuizCandidate`, sin observar la corrida del catálogo. La validación de esta regla vive en el **harness de evals del propio agente** (criterios de juicio + corrida de evals), no en una clase de test JUnit. Por eso la clasificación es `VALIDATED` (confirmada por el usuario como conducta del harness) y **no** `AUTO_VALIDATED` (que afirmaría cobertura por el suite JUnit, inexistente aquí).

- Los criterios **#3 y #7 son deterministas** y reutilizan el **análisis real del audit** (no una réplica aproximada): #3 aplica el **mismo criterio de longitud** que ya usa el audit de quizzes, verificando que la longitud del candidato caiga **dentro del rango esperado (target range) del nivel** (no compara contra la longitud del quiz original; no importa si queda más corto o más largo que él); #7 aplica el **mismo análisis léxico** del audit, verificando que el candidato no introduzca palabras mal ubicadas para el nivel ni fuera de catálogo ausentes en el original (`flags(candidato) ⊆ flags(original)`). Los criterios #1, #2, #4, #5, #6 son juicios cualitativos sobre el texto generado; #1 además exige que el candidato respete el tipo/molde del ejercicio original; #6 exige que, cuando la respuesta del original es de un conjunto cerrado/finito y el ejercicio lo permite, el candidato conserve la misma respuesta (misma categoría de respuesta) del original.
- *Pista de validación (harness)*: #1, #2, #4, #5, #6 como juicios cualitativos; #3 como verificación determinista de longitud contra el rango esperado del nivel; #7 como verificación determinista de no-regresión léxica (subconjunto de lemas flaggeados). La conducta bloqueante/deseable se ejercita con criterios de veredicto controlado dentro del harness de evals; para #6, el harness ejercita tanto el caso en que aplica y bloquea (la respuesta cerrada del original es reutilizable y el candidato la cambió) como el caso N/A (vocabulario abierto o reutilización no natural), donde el veredicto es positivo; para #7, el harness ejercita el caso que reprueba (el candidato introduce un lema flaggeado ausente en el original) y el caso que aprueba (todos los lemas flaggeados del candidato ya estaban flaggeados en el original, o el candidato no tiene ninguno).
- **Dependencia del criterio #7 — consulta léxica del análisis**: la no-regresión léxica exige verificar el candidato con **la misma** verificación léxica del audit, **no** con una réplica. Igual que #3 dejó de aproximar la longitud con un proxy y pasó a reutilizar el conteo real del audit a través de la consulta de tokens del CLI (`content-audit tokens`), #7 consume el **análogo léxico**: una **consulta propia del CLI** que, dada una `quizSentence` y un `nivel`, devuelve la lista de palabras que el análisis flaggearía —cada una con su **lema**, su **nivel esperado** y su **tipo** (mal ubicada para el nivel | fuera de catálogo)—. El eval compara el conjunto de lemas de esa lista para el candidato contra el mismo conjunto para la oración original. Esa consulta es una **capacidad propia del CLI**, con su propio contrato observable y su propia regla, especificada en una **feature dedicada aparte** (resolución de [DOUBT-EVAL-LEXICO-CLI](#DOUBT-EVAL-LEXICO-CLI), Opción A); este requirement solo la **cita como dependencia** de #7 y no la especifica.

**Conducta esperada (harness)**: un candidato cuya longitud cae fuera del rango esperado del nivel recibe veredicto negativo bloqueante por #3; un candidato cuyo `quizSentence` no respeta el tipo del ejercicio original recibe veredicto negativo bloqueante por #1; un candidato que solo reprueba #4 (deseable) se entrega igualmente.

**Conducta esperada (harness) — criterio #6**: dado un quiz original cuya respuesta del hueco pertenece a un conjunto cerrado/finito y cuyo ejercicio admite seguir usando esa respuesta (p.ej. original `sadness ____ [uncountable]`), un candidato que cambia la categoría de la respuesta (p.ej. pasa a `[singular countable]`) recibe veredicto negativo bloqueante por #6, mientras que un candidato que conserva la misma respuesta (otro sustantivo incontable, `[uncountable]`) la aprueba. Cuando la respuesta del original es de vocabulario abierto, o cuando reutilizarla no daría un ejercicio válido/natural, #6 es N/A y su veredicto es positivo (no bloquea). Como todo criterio bloqueante, su veredicto negativo dispara una regeneración dentro del lazo de reintentos ([F-LASAG-R008](#F-LASAG-R008)) pero **nunca impide la emisión final**: al agotar los reintentos, el best-effort de [F-LASAG-R009](#F-LASAG-R009) emite el mejor candidato visto y la reutilización deja de ser un gate duro para pasar a pesar solo como **preferencia** en la elección de ese mejor candidato.

**Conducta esperada (harness) — criterio #7**: dado un quiz original A1 cuya oración flaggea solo `remitted` (fuera de catálogo), un candidato que la reemplaza por `transferred` —un verbo mal ubicado para el nivel (`transfer` es B1 en un quiz A1)— recibe veredicto negativo bloqueante por #7, porque `{transfer} ⊄ {remit}` (introdujo un lema flaggeado ausente en el original); el contexto de fallo del reintento nombra `transfer` con su nivel (B1) para que la regeneración elija un verbo del nivel que colocacione ([F-LASAG-R010](#F-LASAG-R010)). Un candidato que reemplaza `remitted` por un verbo del nivel que colocaciona (p.ej. `sent`) no agrega ningún lema flaggeado (`flags(candidato) ⊆ flags(original)`) y aprueba #7. Un candidato que **conserva** un flag preexistente que la edición no tocó (residuo inevitable) tampoco reprueba por #7: ese lema ya estaba flaggeado en el original, así que el candidato sigue siendo subconjunto. Como todo criterio bloqueante, su veredicto negativo dispara regeneración ([F-LASAG-R008](#F-LASAG-R008)) pero nunca propaga una falla al consumidor: al agotar los reintentos, el best-effort de [F-LASAG-R009](#F-LASAG-R009) emite el mejor candidato visto.

**Escenario de longitud (sin journey propio)**: el caso "un candidato fuera del rango esperado del nivel es bloqueado por #3" es conducta del harness de evals y **no tiene un resultado de falla observable en el límite del sistema**: por el best-effort de [F-LASAG-R009](#F-LASAG-R009), un veredicto bloqueante nunca propaga una falla al consumidor — al agotar los reintentos se emite el mejor candidato visto. Por eso este escenario se valida dentro del catálogo de evals (criterio #3) y no como un journey con terminal de falla.

</details>

<a id="F-LASAG-R008"></a>
### Rule[F-LASAG-R008] - Reintento de generación por veredicto bloqueante, acotado por maxEvalRetries
**Severity**: high | **Validation**: VALIDATED *(validada por el harness de evals del agente, no por el suite JUnit)*

> Cuando un criterio bloqueante de calidad corre y reprueba un candidato, el sistema vuelve a generar un candidato completo en lugar de entregar el reprobado. La cantidad de regeneraciones disparadas por reprobaciones de calidad en una misma resolución no excede el tope configurado `maxEvalRetries`.

<details><summary>Detail</summary>

**Superficie de validación**: el disparo de regeneración por veredicto bloqueante y su cota por `maxEvalRetries` son conducta del lazo de calidad del agente declarativo; **no son observables en el suite JUnit** del adaptador (el consumidor recibe un único `QuizCandidate` final, sin observar cuántas regeneraciones ocurrieron). Esta regla se valida en el **harness de evals del agente**, por eso `VALIDATED` y no `AUTO_VALIDATED`.

Lazo **separado** del de runtime: una falla de runtime nunca entra a este lazo (ver [F-LASAG-R012](#F-LASAG-R012)). Cada regeneración es completa, no ciega (ver carry-forward, [F-LASAG-R010](#F-LASAG-R010)), y resetea el presupuesto de consultas por-run ([F-LASAG-R004](#F-LASAG-R004)). El default de `maxEvalRetries` (3) y su independencia del modo CANNED sí son observables en Java y viven en [F-LASAG-R011](#F-LASAG-R011).

**Conducta esperada (harness)**: con un criterio bloqueante que reprueba el primer candidato, ocurre una segunda generación completa; con `maxEvalRetries = K` y todos los candidatos reprobando, se disparan a lo sumo K regeneraciones.

</details>

<a id="F-LASAG-R009"></a>
### Rule[F-LASAG-R009] - Exhaustion de reintentos de eval: best-effort, no es falla
**Severity**: high | **Validation**: VALIDATED *(validada por el harness de evals del agente, no por el suite JUnit)*

> Si se agota el tope de regeneraciones sin obtener un candidato que pase todos los criterios bloqueantes, el sistema no falla: entrega el mejor candidato visto. "Mejor" se decide por (1) mayor cantidad de criterios bloqueantes superados y, a igualdad, (2) mayor puntaje del criterio deseable #4 (pragmatismo).

<details><summary>Detail</summary>

**Superficie de validación**: el carácter best-effort de la exhaustion y la regla de selección del "mejor" candidato son conducta del lazo de calidad del agente declarativo; **no son observables en el suite JUnit** del adaptador (el consumidor recibe un `QuizCandidate` sin observar la comparación entre candidatos visitados). Se validan en el **harness de evals del agente**, por eso `VALIDATED` y no `AUTO_VALIDATED`.

Todo candidato elegible como "mejor" **ya pasó el gate estructural de FEAT-LAPS**; el best-effort opera solo sobre la capa de calidad.

**Conducta esperada (harness)**: con todos los candidatos reprobando algún bloqueante y uno superando más bloqueantes que el resto, se entrega ese candidato sin excepción (a igualdad de bloqueantes superados, el de mayor puntaje de pragmatismo).

</details>

<a id="F-LASAG-R010"></a>
### Rule[F-LASAG-R010] - Carry-forward de contexto de fallo e historial de tool entre reintentos
**Severity**: medium | **Validation**: VALIDATED *(validada por el harness de evals del agente, no por el suite JUnit)*

> Cada regeneración disparada por una reprobación de calidad recibe como entrada adicional: (i) el contexto de fallo del intento previo (qué criterios bloqueantes reprobaron y por qué) y (ii) el historial de consultas de vocabulario previas (filtros usados y lemmas ya obtenidos), de modo que el nuevo intento mejore la propuesta y no repita pasos.

<details><summary>Detail</summary>

**Superficie de validación**: el carry-forward entre regeneraciones es conducta interna del lazo de calidad del agente declarativo; **no es observable en el suite JUnit** del adaptador (el consumidor no observa las entradas de cada regeneración, solo el candidato final). Se valida en el **harness de evals del agente** — comprobando que un segundo intento, alimentado con el contexto de fallo y el historial de consultas del primero, converge mejor —, por eso `VALIDATED` y no `AUTO_VALIDATED`.

Objetivo funcional: el retry no es regeneración ciega; el nuevo intento se beneficia de lo aprendido en el anterior (ver [F-LASAG-R008](#F-LASAG-R008)).

**Conducta esperada (harness)**: forzada una regeneración, el segundo intento dispone del contexto de fallo del primero y del historial de filtros/lemmas ya consultados.

</details>

<a id="F-LASAG-R011"></a>
### Rule[F-LASAG-R011] - Perilla maxEvalRetries con default 3, ignorada por CANNED
**Severity**: medium | **Validation**: AUTO_VALIDATED

> `LagenConfigResolver.resolve(...)` produce una `LagenConfig` con `maxEvalRetries`; sin env-var explícita el valor es 3. Bajo `CANNED` el sistema emite su candidato sin disparar ninguna regeneración por calidad, con independencia del valor de `maxEvalRetries`.

<details><summary>Detail</summary>

Esta regla tiene dos mitades con superficies de validación distintas:

- **Observable en el límite del sistema (AUTO_VALIDATED, suite JUnit)**: el **default 3** de `maxEvalRetries` cuando no hay env-var explícita. Es resoluble y aseverable directamente sobre `LagenConfigResolver.resolve(...)`. Esta mitad está cubierta por los handwrittenTests existentes; análogo a `maxSuggestedLemmaRequests`.
- **Conducta interna del agente declarativo (validada por su harness de evals, no por JUnit)**: que **CANNED ignore `maxEvalRetries`** (emita su candidato fijo sin disparar el lazo de calidad) es conducta del agente, no observable en el resolver; vive junto al lazo de regeneración de [F-LASAG-R008](#F-LASAG-R008).

**Validación (mitad observable)**: resolver la config sin la env-var de reintentos y assertear `maxEvalRetries == 3`.

</details>

<a id="F-LASAG-R012"></a>
### Rule[F-LASAG-R012] - Error de runtime en cualquier fase = falla inmediata fuera del lazo de retry
**Severity**: high | **Validation**: AUTO_VALIDATED

> Un error de runtime produce un `ProposalStrategyFailedException` inmediato con categoría preservada y nunca se confunde con una reprobación de calidad: solo una reprobación de calidad (el criterio corrió y reprobó) dispara una regeneración.

<details><summary>Detail</summary>

Esta regla tiene dos mitades con superficies de validación distintas:

- **Observable en el límite del sistema (AUTO_VALIDATED, suite JUnit)**: la **clasificación de un error de runtime → `ProposalStrategyFailedException` con categoría preservada y falla inmediata** (sin degradar a candidato vacío/nulo). Es observable directamente sobre el clasificador de errores del adaptador y está cubierta por los handwrittenTests existentes (extiende [F-LASAG-R005](#F-LASAG-R005)).
- **Conducta interna del agente declarativo (validada por su harness de evals, no por JUnit)**: la **distinción de fase** (que un error de runtime *durante la fase de evals* no entre al lazo de regeneración) y la asimetría "runtime = falla inmediata / veredicto bloqueante = retry" son conducta del lazo de calidad del agente; el consumidor solo observa la excepción final, no qué fase la originó ni si hubo o no regeneración.

Distingue infraestructura (siempre falla inmediata) de veredicto (dispara retry, [F-LASAG-R008](#F-LASAG-R008)). Las verificaciones de calidad #1/#2/#4/#5 y las verificaciones deterministas de longitud (#3) y de no-regresión léxica (#7) —esta última vía la consulta léxica del análisis— viven fuera de la cota de "única consulta" ([F-LASAG-R002](#F-LASAG-R002)): corren en la **fase de evals**, no en la de generación.

**Error**: `ProposalStrategyFailedException` con categoría preservada.

**Validación (mitad observable)**: inyectar un error de runtime y assertear que se lanza `ProposalStrategyFailedException` con la categoría preservada, sin candidato vacío/nulo.

</details>

<a id="F-LASAG-R013"></a>
### Rule[F-LASAG-R013] - Las palabras fuera de catálogo alimentan los insumos de generación
**Severity**: high | **Validation**: AUTO_VALIDATED

> Los insumos que la generación transmite al modelo incluyen las **palabras fuera de catálogo** del contexto de corrección ([F-RCLA-R003d](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R003d)), formateadas con al menos su lema y una indicación de si tienen rango de frecuencia conocido. Cuando la lista de palabras fuera de catálogo del contexto está vacía ([F-RCLA-R003e](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R003e)), el insumo correspondiente aparece como `none`.

<details><summary>Detail</summary>

- **Today**: los insumos de generación se arman con la oración original, los `misplacedLemmas` formateados (o `none`) y `suggestedWords`/`curatedWords`, pero **no transmiten las palabras fuera de catálogo**. Para una tarea cuyo único diagnóstico son palabras fuera de catálogo (lemas mal ubicados vacíos), el modelo ve `misplaced: none` y no recibe ninguna señal de qué corregir — revisaría a ciegas.
- **After**: los insumos incluyen un ítem dedicado a las palabras fuera de catálogo, formateado con su lema y una indicación de rango de frecuencia conocido (o `none` cuando la lista viene vacía), en pie de igualdad con el ítem de `misplacedLemmas`.
- **Failing test**: construir un contexto con `outOfCatalogWords` no vacío y assertear que los insumos transmitidos a la generación contienen ese ítem formateado (lema + indicación de rango); con la lista vacía, el ítem es `none`. Falla hoy (el ítem no existe) y pasa tras el cambio.

Esta regla es análoga a la presencia de cada campo poblado del contexto en la generación ([F-LAGEN-R003](../2026-04-29.01_laps-llm-quiz-candidate-generator/REQUIREMENT.md#F-LAGEN-R003), que ya exige incorporar todo campo nuevo del contexto) y al patrón `none`/`(none)` de la lista vacía ([F-RCLA-R003e](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R003e)). La forma exacta del formato (etiquetas, orden, cómo se indica el rango conocido) es decisión de implementación; lo que la regla fija es la **presencia** del ítem formateado y el marcador `none` cuando la lista está vacía.

</details>

<a id="F-LASAG-R014"></a>
### Rule[F-LASAG-R014] - Edición mínima: las palabras objetivo son la unión de mal ubicadas y fuera de catálogo
**Severity**: high | **Validation**: VALIDATED *(validada por el harness de evals del agente, no por el suite JUnit)*

> La política de edición mínima toma como palabras a reemplazar la **unión** de los lemas mal ubicados y las palabras fuera de catálogo del contexto; todo lo demás de la oración (sujeto, objeto, estructura y las restantes content-words) se conserva. Para una palabra fuera de catálogo, la corrección preferida es:
> 1. si es un **error ortográfico evidente** de una palabra del nivel, su ortografía correcta — siempre que la palabra corregida sea del nivel;
> 2. si es una palabra **genuina pero avanzada/rara**, un equivalente del nivel, con preferencia por los `suggestedWords`/`curatedWords` cuando colocacionan;
>
> en ambos casos con las mismas restricciones de colocación y de preservación del objeto/complemento que ya rigen para los lemas mal ubicados.

<details><summary>Detail</summary>

**Superficie de validación**: la política de edición mínima y la corrección dirigida de las palabras fuera de catálogo son conducta del nodo de generación del agente declarativo; **no son observables en el suite JUnit** del adaptador (el consumidor recibe un único `QuizCandidate` final, sin observar qué palabras se tomaron como objetivo ni cómo se corrigieron). Se validan en el **harness de evals del agente**, por eso `VALIDATED` y no `AUTO_VALIDATED`.

- **Today**: la edición mínima toma como objetivo **solo** los lemas mal ubicados; las palabras fuera de catálogo no entran en el conjunto a reemplazar.
- **After**: el conjunto objetivo es la unión de mal ubicadas y fuera de catálogo; el resto de la oración se preserva igual que hoy.

Las restricciones heredadas de los lemas mal ubicados (preservar el objeto/complemento, elegir un reemplazo que colocacione con el mismo objeto, no reescribir la oración de cero, la preferencia por `suggestedWords`/`curatedWords` solo cuando encajan sin reescribir) aplican idénticas a las palabras fuera de catálogo.

**Conducta esperada (harness)**: dado un contexto cuya única palabra objetivo es un error ortográfico evidente de una palabra del nivel, el candidato corrige la ortografía y conserva el resto; dado un contexto cuya única palabra objetivo es una palabra genuina avanzada/rara, el candidato la reemplaza por un equivalente del nivel (prefiriendo `suggestedWords`/`curatedWords` cuando colocacionan) preservando el objeto y el resto de la oración.

</details>

<a id="F-LASAG-R015"></a>
### Rule[F-LASAG-R015] - Una tarea sin lemas mal ubicados pero con palabras fuera de catálogo es generación válida
**Severity**: high | **Validation**: AUTO_VALIDATED

> Un contexto de corrección con la lista de lemas mal ubicados **vacía** y al menos una palabra fuera de catálogo es un caso válido de generación: el sistema ejecuta la generación y emite exactamente un `QuizCandidate`; no es un caso degenerado que se saltee ni que falle por ausencia de lemas mal ubicados.

<details><summary>Detail</summary>

Esta regla tiene dos mitades con superficies de validación distintas:

- **Observable en el límite del sistema (AUTO_VALIDATED, suite JUnit)**: que un contexto con `misplacedLemmas` vacío y `outOfCatalogWords` no vacío **no cortocircuite** la generación. Configurado ese contexto, el consumidor recibe un `QuizCandidate` sin excepción y sin degradación a candidato vacío/nulo. Es la mitad testeable sobre el adaptador.
- **Conducta interna del agente declarativo (validada por su harness de evals, no por JUnit)**: que la generación resultante sea **dirigida** — que las palabras fuera de catálogo sean las palabras objetivo efectivamente corregidas en el candidato — es conducta del nodo de generación (ver [F-LASAG-R014](#F-LASAG-R014)); el consumidor solo observa que se emitió un candidato.

- **Today**: la generación se conducía por los lemas mal ubicados; con `misplacedLemmas` vacío no había señal de objetivo (`misplaced: none`), dejando el caso fuera-de-catálogo-only sin orientación.
- **After**: el caso fuera-de-catálogo-only produce igualmente un `QuizCandidate` dirigido por las palabras fuera de catálogo ([F-LASAG-R013](#F-LASAG-R013), [F-LASAG-R014](#F-LASAG-R014)).

**Validación (mitad observable)**: invocar el generador con un contexto de `misplacedLemmas` vacío y `outOfCatalogWords` no vacío y assertear que se emite exactamente un `QuizCandidate` (con `quizSentence` y `translation` presentes), sin excepción.

</details>

## Context

La ruta LLM de *lemma-absence* está hoy partida en dos implementaciones del puerto `QuizCandidateGenerator`: el single-shot FEAT-LAGEN (filtra `suggestedWords` dentro del prompt, una sola llamada) y el interactivo FEAT-LAGAG (sesión de consultas mid-generación contra suggested-lemmas). Cada una arrastra su propio `StrategyId.name`, su wiring por `LagenMode` y su provenance, duplicando la lógica de prompt/selección de vocabulario.

La disponibilidad del runtime **sentinel-agent** permite expresar ambas como un **mismo agente**: un react acotado que decide cuándo consultar `suggestedWords` mediante una única tool en lugar de filtrarlos a mano o mantener una sesión interactiva separada. El delta consiste en sustituir el backend de la ruta LLM por este agente, conservando intacto el contrato del puerto (FEAT-LAPS R001–R003) y la política de fallas (R015/R016). `CANNED` se conserva como opt-in offline.

### Evidencia (2026-07-22): tareas fuera-de-catálogo revisadas a ciegas

El plan activo (`2026-07-22T05-26-12`) tiene 1420 tareas `LEMMA_ABSENCE` de quiz: 1112 con lemas mal ubicados y **308 cuyo único diagnóstico son palabras fuera de catálogo** (typos tratados como palabra desconocida, palabras genuinas pero avanzadas/raras, nombres propios mal etiquetados). El contexto de corrección ya expone esas palabras en `outOfCatalogWords` (lema, categoría, rango de frecuencia —típicamente nulo en typos/no-palabras— y descuento; [F-RCLA-R003d](../2026-04-13.01_refiner-correction-context-labs/REQUIREMENT.md#F-RCLA-R003d)), pero los insumos que el agente transmite al modelo se arman **solo con los lemas mal ubicados**: para esas 308 tareas el modelo ve `misplaced: none` y no sabe qué corregir. El delta de esta ronda expone las palabras fuera de catálogo en los insumos ([F-LASAG-R013](#F-LASAG-R013)), extiende la edición mínima para tomarlas como palabras objetivo ([F-LASAG-R014](#F-LASAG-R014)) y reconoce la tarea fuera-de-catálogo-only como caso válido de generación ([F-LASAG-R015](#F-LASAG-R015)).

### Evidencia (prueba live 2026-07-22, run `20260722T214253-b4c83c4a`): fuera-de-catálogo reemplazado por mal-ubicada

Primera corrida live del agente sobre una tarea fuera-de-catálogo-only (quiz A1 "My brother remitted money to his family.", conocimiento past-simple-spelling-modifications): la mecánica nueva funcionó (curación vía tool, edición mínima quirúrgica, 7/7 evals bloqueantes existentes en verde), pero el candidato final fue "My brother transferred money to his family." — y `transfer` es **B1/B2** en el catálogo. Es decir, se reemplazó una palabra fuera de catálogo por una **mal ubicada para el nivel**, que el próximo audit volverá a flaggear. Causa raíz: cuando ninguna palabra curada colocaciona (las 2 sugeridas eran object/smoke, ninguna colocaciona con "money to his family"), el prompt permite "una palabra del nivel que sí encaje", pero el modelo no tiene forma de **verificar** el nivel y **ningún eval lo comprueba**: había evals de longitud, distinctness y calidad, pero ninguno corría el análisis léxico sobre el candidato. El criterio #7 de no-regresión léxica ([F-LASAG-R007](#F-LASAG-R007)) cierra ese hueco: bloquea al candidato que introduce un lema mal ubicado o fuera de catálogo ausente en el original y devuelve el nivel de la palabra infractora al reintento.

## Scope

**En alcance**

- Implementación del puerto `QuizCandidateGenerator` (FEAT-LAPS R001–R003) sobre sentinel-agent, con **contrato externo idéntico**: entrada `CorrectionContext` de FEAT-RCLA, salida **un único** `QuizCandidate {quizSentence, translation}`.
- React corto con **UNA sola tool**: listar/filtrar `suggestedWords` reutilizando `GetCommand.get("suggested-lemmas", …, SuggestedLemmasFilter)` (semántica F-CSLATDC), sustituyendo el filtrado manual del prompt.
- Presupuesto de consultas acotado; **exhaustion no es falla** (patrón FEAT-LAGAG R004/R005).
- Mapeo de timeouts/errores del framework sentinel-agent a `ProposalStrategyFailedException` preservando categoría (FEAT-LAPS R015), **sin reintentos** (R016): un único intento de generación.
- Absorción de la ruta interactiva (FEAT-LAGAG) y del single-shot (FEAT-LAGEN) en una única ruta LLM.
- Exposición de las **palabras fuera de catálogo** del contexto en los insumos de generación ([F-LASAG-R013](#F-LASAG-R013)) y su corrección dirigida bajo edición mínima ([F-LASAG-R014](#F-LASAG-R014)), incluyendo la tarea fuera-de-catálogo-only ([F-LASAG-R015](#F-LASAG-R015)).
- Nuevo eval determinista bloqueante de **no-regresión léxica** (criterio #7 de [F-LASAG-R007](#F-LASAG-R007)): el candidato no introduce palabras mal ubicadas para el nivel ni fuera de catálogo ausentes en el original, verificado con el **mismo** análisis léxico del audit vía una consulta determinista (oración + nivel → flags), con feedback al reintento que nombra la palabra infractora con su nivel.

**Fuera de alcance**

- Modificar FEAT-RCLA, FEAT-QSENT/el DSL de QuizSentence, REVBYP, REVAPR.
- Otros `DiagnosisKind` distintos de `LEMMA_ABSENCE`.
- Reintentos, auto-validación, composición de estrategias o funcionalidad de producto nueva.
- Cambiar la firma del puerto `QuizCandidateGenerator` o la forma de selección por `DiagnosisKind`.
- Definir la **forma concreta** de la consulta léxica del análisis (nombre de comando, flags, formato de salida) y su eventual regla/contrato propio en la feature donde vive `content-audit tokens`: es decisión del arquitecto/equipo ([DOUBT-EVAL-LEXICO-CLI](#DOUBT-EVAL-LEXICO-CLI)). Este requirement solo fija que #7 reutilice el **mismo** análisis léxico del audit, no una réplica.

## User Journeys

### Journey[F-LASAG-J001] - Generación exitosa de QuizCandidate por la ruta LLM (agente sentinel-agent)
**Validation**: AUTO_VALIDATED

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
          - when: "todos los evals bloqueantes (#1, #2, #3, #5, #6, #7) dan veredicto positivo"
            then: emitir
      - id: emitir
        action: "Se devuelve exactamente un QuizCandidate {quizSentence, translation} con ambos campos presentes"
        gate: [F-LASAG-R003]
        result: success
```

### Journey[F-LASAG-J002] - Agotar el presupuesto de tool no es falla
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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
```

### Journey[F-LASAG-J003] - Error de runtime en generación falla con categoría preservada e intento único
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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
```

### Journey[F-LASAG-J004] - Veredicto bloqueante negativo dispara reintento con carry-forward, acotado por maxEvalRetries
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-LASAG-J004
    name: Veredicto bloqueante negativo dispara reintento con carry-forward, acotado por maxEvalRetries
    flow:
      - id: preparar
        action: "Se prepara un CorrectionContext válido, se configura maxEvalRetries = K y se fuerza que un eval bloqueante repruebe en el primer intento"
        then: generar
      - id: generar
        action: "El agente ejecuta un run de generación completo (react con la tool) y pasa el gate estructural"
        then: evaluar_primer_intento
      - id: evaluar_primer_intento
        action: "El primer candidato se somete al catálogo de evals"
        gate: [F-LASAG-R007]
        outcomes:
          - when: "un eval bloqueante corrió y reprobó, y aún quedan reintentos dentro de maxEvalRetries"
            then: reintentar
          - when: "todos los evals bloqueantes pasan en el primer intento"
            then: emitir
      - id: reintentar
        action: "Se dispara un nuevo run de generación completo del agente que recibe como entrada adicional el contexto de fallo de los evals previos y el historial de llamadas a la tool (filtros usados y lemmas obtenidos)"
        gate: [F-LASAG-R008, F-LASAG-R010]
        then: evaluar_reintento
      - id: evaluar_reintento
        action: "El candidato regenerado se somete nuevamente al catálogo de evals, dentro del tope de maxEvalRetries reintentos"
        gate: [F-LASAG-R007, F-LASAG-R008]
        outcomes:
          - when: "todos los evals bloqueantes pasan tras el reintento"
            then: emitir
      - id: emitir
        action: "Se emite el QuizCandidate que pasa todos los bloqueantes, habiéndose disparado a lo sumo K reintentos de generación"
        gate: [F-LASAG-R008, F-LASAG-R003]
        result: success
```

### Journey[F-LASAG-J005] - Exhaustion de reintentos de eval es best-effort, no falla
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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
```

### Journey[F-LASAG-J006] - Veredicto deseable negativo no bloquea la emisión
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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
```

### Journey[F-LASAG-J007] - Error de runtime en la fase de evals falla de inmediato fuera del lazo de retry
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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
```

### Journey[F-LASAG-J009] - Perilla maxEvalRetries con default 3, ignorada por CANNED
**Validation**: AUTO_VALIDATED

```yaml
journeys:
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

<a id="DOUBT-EVAL-LEXICO-CLI"></a>
### Doubt[DOUBT-EVAL-LEXICO-CLI] - ¿La consulta léxica determinista (oración + nivel → flags) merece regla/contrato propio en la feature donde vive `content-audit tokens`?

El criterio #7 de [F-LASAG-R007](#F-LASAG-R007) exige verificar el candidato con **el mismo** análisis léxico del audit (mismo tokenizador y catálogo), reutilizándolo a través de una consulta determinista: entrada `quizSentence` + `nivel`, salida lista de flags con `lema`, `nivel esperado` y `tipo` (mal ubicada para el nivel | fuera de catálogo). Es el análogo léxico de lo que `content-audit tokens` ya hizo para la longitud (reemplazar un proxy aproximado por el conteo real del audit).

- [x] **Opción A** — La consulta se especifica como **capacidad propia del CLI**, con su propio contrato observable en el límite del sistema y su propia regla, en una **feature nueva dedicada** (precedente F-CSLATDC para la consulta de suggested-lemmas); el eval #7 la cita como dependencia. Se evita duplicar reglas de catálogo/nivel en el harness.
- [ ] **Opción B** — La consulta es un detalle interno del harness del agente que reutiliza el análisis del audit, sin regla ni contrato observable propios; #7 sigue validado solo por el harness de evals.
- [ ] **Opción C** — Otra (a decidir por el equipo).

**Answer** (2026-07-22): Opción A. La consulta léxica del análisis vive como **capacidad propia del CLI** —entrada `quizSentence` + `nivel`, salida lista de flags con `lema`, `nivel esperado` y `tipo`— con su contrato observable y su regla propia en una feature nueva dedicada, análoga a F-CSLATDC. El eval #7 de [F-LASAG-R007](#F-LASAG-R007) la consume como dependencia; su especificación no forma parte de este requirement.

## References

- **FEAT-LAPS** (`2026-04-20.02_lemma-absence-proposal-strategy`) — contrato del puerto `QuizCandidateGenerator` (R001–R003), gate de validación cerrado (R004/R005), falla del generador → `ProposalStrategyFailedException` preservando categoría (R015) y sin reintentos (R016). El agente debe respetarlo sin cambios.
- **FEAT-LAGEN** (`2026-04-29.01_laps-llm-quiz-candidate-generator`) — generador LLM single-shot que se reemplaza: prompt desde `CorrectionContext`, `suggestedWords` como vocabulario preferido, taxonomía de fallas, provenance `provider:model`, wiring por `LagenMode`.
- **FEAT-LAGAG** (`2026-05-29.01_generacion-interactiva-suggested-lemmas`) — variante interactiva que se absorbe: consultas mid-generación a suggested-lemmas, presupuesto acotado y exhaustion no-falla. Antecedente directo del react con UNA tool.
- **F-CSLATDC** (`2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli`) — semántica de la única tool: consulta read-only de SuggestedLemmas con filtros level/POS/limit, inferencia de level, filtro cerrado, sin efectos colaterales.
- **FEAT-RCLA** (`2026-04-13.01_refiner-correction-context-labs`) — fuente del `CorrectionContext`. Fuera de alcance modificarlo; solo se consume. Provee la lista `outOfCatalogWords` (estructura en `F-RCLA-R003d`, siempre-presente/vacía en `F-RCLA-R003e`), que esta feature transmite a la generación y toma como palabras objetivo. Citado por [F-LASAG-R013](#F-LASAG-R013), [F-LASAG-R014](#F-LASAG-R014) y [F-LASAG-R015](#F-LASAG-R015).