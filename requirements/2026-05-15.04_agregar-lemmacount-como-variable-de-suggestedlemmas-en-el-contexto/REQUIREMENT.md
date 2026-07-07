---
feature:
  id: FEAT-LEMMA-SUGGESTIONS
  code: F-SLEM
  name: LemmaCount como variable de SuggestedLemmas
  priority: critical
---

# LemmaCount como variable de SuggestedLemmas

## TL;DR

Agregar la señal de `lemma-count` a `correctionContext.suggestedLemmas` en tareas `LEMMA_ABSENCE`, ordenando por un esquema de **seis tiers de prioridad** (first-match-wins) donde la pertenencia a una **banda de frecuencia COCA deficitaria del nivel** es clave secundaria fuerte dentro de la macro-escasez, por encima de la distinción sub-expuesto/ausente ([F-SLEM-R003](#F-SLEM-R003), [F-SLEM-R014](#F-SLEM-R014)).  
El estado de exposición se corta con el umbral `N` de `lemma-count` (sub-expuesto = `1..N-1`, ausente = sin exposición, expuesto-pero-escaso = `N..2N-1`), nunca con literales; COCA ascendente es el desempate final dentro de cada tier ([F-SLEM-R005](#F-SLEM-R005)). Si la auditoría no analizó bandas, los tiers de banda quedan vacíos y el orden degrada sin fallar ([F-SLEM-R015](#F-SLEM-R015)).  
La funcionalidad se observa mediante `AuditRunner.runAudit(...)`, `AuditRunner.runDetailedAudit(...)`, `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)`, sin introducir un comando público nuevo ni nuevos campos observables por lema.

## Recorrido de uso (estado actual)

1. El cliente ejecuta `AuditRunner.runAudit(coursePath, analyzerNames)` o `AuditRunner.runDetailedAudit(coursePath, analyzerNames)` para auditar un curso.
2. Si incluye el análisis que produce `LEMMA_ABSENCE`, la auditoría permite generar tareas de revisión relacionadas con lemas ausentes o mal ubicados.
3. Si también incluye `lemma-count`, la auditoría dispone de señal sobre lemas que aparecen en menos oraciones distintas que el umbral configurado.
4. El cliente ejecuta `PlanCommand.plan(auditId, storageMode, true)` para proyectar o crear tareas con `correctionContext`.
5. Hoy, en tareas `LEMMA_ABSENCE`, `correctionContext.suggestedLemmas` se basa en los diagnósticos de ausencia o ubicación temporal de lemas, con orden por COCA ascendente y límite existente.
6. El cliente puede observar esas tareas mediante `GetCommand.get("tasks", name, filter)`.
7. El cliente puede ejecutar `ReviseCommand.revise(taskId, planId, null, null)` para que la revisión use el `correctionContext` derivado por el sistema.
8. El cliente también puede ejecutar `ReviseCommand.revise(taskId, planId, correctionContextJson, null)` o `ReviseCommand.revise(taskId, planId, null, correctionContextFilePath)` para aportar un `correctionContext` externo con la misma forma observable que devuelve `GetCommand.get(...)`.

**Punto de fricción:** aunque `lemma-count` ya identifica lemas sub-expuestos, esa señal no participa en `suggestedLemmas`. Como resultado, las sugerencias pueden priorizar palabras completamente nuevas antes que palabras ya introducidas que todavía no alcanzan el mínimo de exposición esperado.

**Cambio mínimo necesario:** enriquecer y reordenar `task.correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE` usando la señal disponible de `lemma-count`, sin crear un verbo público nuevo, conservando el límite existente de sugerencias y manteniendo paridad entre `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)`.

## Business Rules

### Rule[F-SLEM-R001] - La auditoría con lemma-count provee la señal funcional para suggestedLemmas
**Severity**: critical | **Validation**: VALIDATED

Cuando `AuditRunner.runAudit(...)` o `AuditRunner.runDetailedAudit(...)` se ejecuta incluyendo `lemma-count` junto con el análisis que produce diagnósticos `LEMMA_ABSENCE`, el resultado de auditoría debe dejar disponible la señal de `lemma-count` para identificar lemas con exposición menor al umbral aplicable.

Esa señal debe ser la fuente funcional usada después por `PlanCommand.plan(..., true)`, `GetCommand.get(...)` y `ReviseCommand.revise(...)` para enriquecer `correctionContext.suggestedLemmas`.

**Error**: "No se pudo usar la señal de lemma-count disponible en la auditoría para enriquecer suggestedLemmas"

### Rule[F-SLEM-R002] - Plan con correctionContext enriquece suggestedLemmas para LEMMA_ABSENCE
**Severity**: critical | **Validation**: VALIDATED

Cuando `PlanCommand.plan(auditId, storageMode, true)` genera tareas desde una auditoría con diagnósticos `LEMMA_ABSENCE` y señal disponible de `lemma-count`, cada tarea `LEMMA_ABSENCE` debe exponer `correctionContext.suggestedLemmas` enriquecido con esa señal.

La lista debe poder incluir lemas sub-expuestos por `lemma-count` aunque esos lemas no tengan diagnóstico propio `LEMMA_ABSENCE`, siempre evaluados en su nivel correspondiente.

**Error**: "Las sugerencias de lemas no incorporan la señal disponible de lemma-count"

### Rule[F-SLEM-R003] - Ranking por seis tiers de prioridad con banda deficitaria como clave secundaria fuerte
**Severity**: critical | **Validation**: VALIDATED

> Los lemas de `correctionContext.suggestedLemmas` se ordenan por seis tiers de prioridad (first-match-wins), donde pertenecer a una banda de frecuencia COCA deficitaria del nivel empuja hacia arriba dentro de la macro-escasez, y los lemas expuestos-pero-escasos van siempre al final. Dentro de cada tier, el desempate final es COCA ascendente.

Cuando el sistema construye `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE` y existe señal de `lemma-count`, cada lema sugerido cae en **exactamente un tier** según el primer criterio que satisface (first-match-wins), con `N` = umbral de exposición de `lemma-count` del nivel evaluado:

| Tier | Condición (funcional) | Intuición |
|------|-----------------------|-----------|
| 1 | ya introducido y **sub-expuesto** (exposición `1..N-1`) **y** pertenece a una banda deficitaria del nivel | refuerza y tapa banda |
| 2 | **completamente ausente** (sin exposición previa) **y** pertenece a una banda deficitaria del nivel | nuevo que tapa banda |
| 3 | ya introducido y **sub-expuesto** (exposición `1..N-1`) | refuerza |
| 4 | **completamente ausente** (sin exposición previa) | nuevo |
| 5 | ya introducido y **expuesto-pero-escaso** (exposición `N..2N-1`) **y** pertenece a una banda deficitaria del nivel | expuesto-pero-escaso que tapa banda |
| 6 | ya introducido y **expuesto-pero-escaso** (exposición `N..2N-1`) | expuesto-pero-escaso |

La formalización de tiers, cortes por `N` y el desempate final está en [F-SLEM-R014](#F-SLEM-R014).

<details><summary>Detail</summary>

**Invariantes del esquema:**

- (a) **Sub-expuesto siempre le gana a ausente dentro del mismo estado de banda**: tier 1 > tier 2 y tier 3 > tier 4.
- (b) **La banda deficitaria empuja hacia arriba dentro de la macro-escasez** (`0..N-1`), al punto de que un **ausente-deficitario (tier 2) le gana a un sub-expuesto-no-deficitario (tier 3)**. Esto es DELIBERADO: tapar una banda de frecuencia que le falta al nivel pesa más que la distinción sub-expuesto/ausente cuando el lema deficitario está ausente.
- (c) **Los expuestos-escasos (`N..2N-1`, tiers 5-6) van siempre al final**, nunca superan a la macro-escasez (`0..N-1`). Que la banda sea deficitaria solo los ordena entre sí (5 antes que 6), nunca por encima de un tier `1..4`.

**Subsunción del subtipo de ausencia.** El subtipo de `LEMMA_ABSENCE` (`APPEARS_TOO_LATE`, `APPEARS_TOO_EARLY`, `COMPLETELY_ABSENT`) se **subsume** en las condiciones de count: "completamente ausente" (tiers 2/4) corresponde a un lema sin exposición previa en su nivel (típicamente `COMPLETELY_ABSENT`), y "sub-expuesto" (tiers 1/3) corresponde a un lema ya introducido con exposición `1..N-1` (incluye lemas con `APPEARS_TOO_LATE`/`APPEARS_TOO_EARLY` que ya aparecieron pero no alcanzan el umbral). El orden fino por subtipo dentro de un mismo estado de count/banda **no** se conserva por defecto (ver [DOUBT-ABSENCE-SUBTYPE-TIEBREAK](#DOUBT-ABSENCE-SUBTYPE-TIEBREAK)). `APPEARS_TOO_EARLY` sigue evaluándose en el nivel target real ([F-SLEM-R004](#F-SLEM-R004)), lo que gobierna *cómo se computa* la exposición que alimenta los tiers, no el orden entre tiers.

**Degradación.** Cuando la auditoría no incluyó el análisis de bandas de frecuencia COCA, los tiers 1, 2 y 5 quedan vacíos (ningún lema puede satisfacer "pertenece a banda deficitaria") y el orden degrada a 3 → 4 → 6, sin ramas condicionales ([F-SLEM-R015](#F-SLEM-R015)).

**Error**: "El ranking de suggestedLemmas no respeta los seis tiers de prioridad acordados con lemma-count"

</details>

### Rule[F-SLEM-R004] - APPEARS_TOO_EARLY se evalúa en el nivel target real
**Severity**: critical | **Validation**: VALIDATED

Cuando un lema tiene diagnóstico `APPEARS_TOO_EARLY` y existe señal de `lemma-count`, la exposición mínima debe evaluarse contra el nivel target real del lema.

Las apariciones tempranas no deben considerarse suficientes para satisfacer la exposición esperada en el nivel correcto.

**Error**: "APPEARS_TOO_EARLY fue evaluado fuera de su nivel target real"

### Rule[F-SLEM-R005] - COCA ascendente es el desempate final dentro de un mismo tier
**Severity**: major | **Validation**: VALIDATED

> Cuando dos o más lemas caen en el **mismo tier de prioridad** ([F-SLEM-R003](#F-SLEM-R003), [F-SLEM-R014](#F-SLEM-R014)), el sistema los ordena entre sí por COCA ascendente. Es el desempate final, después de que el tier ya quedó fijado.

Como la pertenencia a banda deficitaria ya está incorporada en la **definición** de los tiers ([F-SLEM-R003](#F-SLEM-R003)), dentro de un mismo tier todos los lemas comparten el mismo estado de banda; por lo tanto, COCA ascendente es el único criterio de desempate que queda dentro del tier. Cuando la auditoría no incluyó el análisis de bandas de frecuencia COCA, los tiers de banda (1, 2, 5) quedan vacíos y COCA ascendente sigue siendo el desempate final dentro de los tiers 3, 4 y 6 ([F-SLEM-R015](#F-SLEM-R015)).

El sistema **no** debe usar el mayor déficit de **exposición de lemma-count** de un lema individual como desempate dentro del mismo tier.

**Error**: "El desempate de suggestedLemmas no respeta COCA ascendente como desempate final dentro del tier"

### Rule[F-SLEM-R006] - El límite existente de suggestedLemmas se aplica después del nuevo ranking
**Severity**: major | **Validation**: VALIDATED

Cuando el sistema construye `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE`, debe aplicar primero el ranking con `lemma-count` y después conservar el límite existente de elementos sugeridos.

El enriquecimiento con `lemma-count` no debe aumentar el tamaño máximo observable de `suggestedLemmas`.

**Error**: "El límite de suggestedLemmas no se aplicó después del ranking con lemma-count"

### Rule[F-SLEM-R007] - COMPLETELY_ABSENT queda detrás del sub-expuesto solo dentro del mismo estado de banda
**Severity**: critical | **Validation**: VALIDATED

> Un lema `COMPLETELY_ABSENT` (sin exposición previa) queda detrás de un lema ya introducido y sub-expuesto **cuando ambos comparten el mismo estado de banda deficitaria** (tier 4 detrás de tier 3, tier 2 detrás de tier 1). La preferencia por el sub-expuesto **no** es absoluta: un ausente-deficitario (tier 2) va antes que un sub-expuesto-no-deficitario (tier 3), de forma deliberada.

Con el esquema de seis tiers ([F-SLEM-R003](#F-SLEM-R003)), la regla "el sub-expuesto le gana al completamente ausente" vale **solo dentro del mismo estado de banda deficitaria**: tier 1 (sub-expuesto + banda) le gana a tier 2 (ausente + banda), y tier 3 (sub-expuesto) le gana a tier 4 (ausente). La banda deficitaria es una clave secundaria más fuerte que esa distinción dentro de la macro-escasez: por eso un lema completamente ausente que tapa una banda deficitaria (tier 2) queda **antes** que un sub-expuesto que no tapa ninguna banda deficitaria (tier 3). Esta relajación es intencional.

<details><summary>Detail</summary>

**Qué NO cambia.** La señal de `lemma-count` sigue siendo **no aplicable** a un lema completamente ausente: el curso nunca lo introdujo, así que no hay una exposición observada `1..N-1` que medir. En consecuencia, el elemento de `suggestedLemmas` correspondiente a un lema completamente ausente **no** expone valor observable de `lemmaCount`, ni de `lemmaCountThreshold`, ni de `isUnderexposed` (los tres quedan no informados), incluso cuando la auditoría sí incluyó `lemma-count` ([F-SLEM-R013](#F-SLEM-R013)). El count "sin exposición previa" que ubica al lema en los tiers 2/4 se deriva de la ausencia de diagnóstico de exposición, no de un `lemmaCount` observable en el elemento.

**Relación con R003.** Esta regla es la lectura por-par del esquema de tiers de [F-SLEM-R003](#F-SLEM-R003): describe cómo se resuelve el orden entre un lema ausente y un lema sub-expuesto según el estado de banda de cada uno.

**Error**: "COMPLETELY_ABSENT fue ordenado incorrectamente respecto de un lema sub-expuesto dado su estado de banda deficitaria"

</details>

### Rule[F-SLEM-R008] - Plan proyectado y consulta de tareas muestran el mismo correctionContext enriquecido
**Severity**: major | **Validation**: VALIDATED

Cuando una tarea `LEMMA_ABSENCE` con `correctionContext` enriquecido se observa mediante `PlanCommand.plan(..., true)` o mediante `GetCommand.get("tasks", name, filter)`, la forma observable y el orden de `task.correctionContext.suggestedLemmas` deben ser consistentes entre ambos caminos.

**Error**: "El correctionContext observado en el plan no coincide con el observado en la consulta de tareas"

### Rule[F-SLEM-R009] - Revise consume el correctionContext enriquecido derivado por el sistema
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(taskId, planId, null, null)` se ejecuta sobre una tarea `LEMMA_ABSENCE` cuyo `correctionContext` fue generado por el sistema, la revisión debe consumir `correctionContext.suggestedLemmas` enriquecido y ordenado con la señal de `lemma-count`.

**Error**: "La revisión no recibió el correctionContext enriquecido con lemma-count"

### Rule[F-SLEM-R010] - Revise acepta override externo válido con la forma observable enriquecida
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(taskId, planId, correctionContextJson, null)` o `ReviseCommand.revise(taskId, planId, null, correctionContextFilePath)` recibe un `correctionContext` externo para una tarea `LEMMA_ABSENCE`, el sistema debe aceptar la misma forma observable de `suggestedLemmas` enriquecido que devuelve `GetCommand.get(...)`, siempre que la señal de `lemma-count` sea válida y consistente.

La validación del override usa la forma observable definida por [F-SLEM-R013](#F-SLEM-R013): cada elemento de `suggestedLemmas` puede traer `lemmaCount`, `lemmaCountThreshold` y `isUnderexposed`, o bien tener esos tres valores no informados (señal no aplicable o no disponible).

**Error**: "El correctionContext externo no coincide con la forma válida de suggestedLemmas enriquecido"

### Rule[F-SLEM-R011] - Revise rechaza override externo con señal de lemma-count inconsistente
**Severity**: major | **Validation**: VALIDATED

Cuando `ReviseCommand.revise(...)` recibe un `correctionContext` externo con datos inconsistentes de `lemma-count` dentro de `suggestedLemmas`, el sistema debe rechazar el override como `correctionContext` inválido y no debe aplicar la revisión con esos datos.

Ejemplos de inconsistencia funcional incluyen conteos negativos o una marca de sub-exposición incompatible con el umbral informado.

**Error**: "El correctionContext es inválido por inconsistencia en la señal de lemma-count dentro de suggestedLemmas"

### Rule[F-SLEM-R012] - Auditoría sin lemma-count conserva suggestedLemmas sin fallar
**Severity**: major | **Validation**: VALIDATED

Cuando `PlanCommand.plan(..., true)` o `GetCommand.get("tasks", ...)` observa tareas `LEMMA_ABSENCE` derivadas de una auditoría que no incluyó `lemma-count`, el sistema debe seguir devolviendo `correctionContext.suggestedLemmas` para `LEMMA_ABSENCE`.

La señal de `lemma-count` debe quedar claramente no disponible y no debe causar falla en la generación, proyección o consulta del contexto. Concretamente, cuando la auditoría no incluyó `lemma-count`, ningún elemento de `correctionContext.suggestedLemmas` expone valor observable de `lemmaCount`, ni de `lemmaCountThreshold`, ni de `isUnderexposed` (los tres quedan no informados en todos los lemas sugeridos). Sin señal de `lemma-count` no puede computarse el estado de exposición por `N`, de modo que el esquema de seis tiers ([F-SLEM-R003](#F-SLEM-R003)) no aplica; el orden se resuelve por los criterios disponibles (COCA ascendente, [F-SLEM-R005](#F-SLEM-R005)) y el límite se aplica igual ([F-SLEM-R006](#F-SLEM-R006)).

**Error**: "La ausencia de lemma-count impidió generar o consultar suggestedLemmas"

### Rule[F-SLEM-R013] - Forma observable de la señal de lemma-count dentro de suggestedLemmas
**Severity**: critical | **Validation**: VALIDATED

Cuando un cliente observa `correctionContext.suggestedLemmas` para una tarea `LEMMA_ABSENCE` mediante `PlanCommand.plan(..., true)`, `GetCommand.get("tasks", ...)` o `ReviseCommand.revise(...)`, cada elemento expone como mucho tres valores asociados a la señal de `lemma-count`:

1. `lemmaCount`: cantidad observada de exposiciones del lema relevante para el nivel evaluado.
2. `lemmaCountThreshold`: umbral mínimo de exposición usado para decidir sub-exposición en ese nivel.
3. `isUnderexposed`: indicador de si `lemmaCount` está por debajo de `lemmaCountThreshold`.

Los tres valores son opcionales y siempre se comportan en bloque:

- Cuando la auditoría incluyó `lemma-count` y la señal aplica al lema sugerido, los tres valores están informados y son consistentes entre sí (es decir, `isUnderexposed` coincide con la comparación entre `lemmaCount` y `lemmaCountThreshold`).
- Cuando la auditoría no incluyó `lemma-count` ([F-SLEM-R012](#F-SLEM-R012)) o la señal no es aplicable al lema (por ejemplo `COMPLETELY_ABSENT`, [F-SLEM-R007](#F-SLEM-R007)), los tres valores quedan no informados.

No se exponen otros valores derivados de `lemma-count` (por ejemplo, déficit explícito) en `suggestedLemmas`.

**Error**: "La forma observable de lemma-count dentro de suggestedLemmas no respeta el contrato acordado"

### Rule[F-SLEM-R014] - Definición formal de los seis tiers: estado de exposición, banda deficitaria y COCA ascendente
**Severity**: major | **Validation**: VALIDATED

> Los seis tiers de [F-SLEM-R003](#F-SLEM-R003) se computan con una única función de prioridad: (1) estado de exposición cortado por el umbral `N` de `lemma-count` (sub-expuesto = `1..N-1`, ausente = sin exposición, expuesto-pero-escaso = `N..2N-1`), (2) pertenencia **binaria** a una banda deficitaria del nivel, (3) COCA ascendente como desempate final dentro del tier.

Con `N` = umbral de exposición de `lemma-count` del nivel evaluado, cada lema sugerido cae en exactamente un tier según su **estado de exposición** y su **estado de banda**:

**Estados de exposición (cortados por `N`, sin literales):**

- **sub-expuesto**: exposición en `1..N-1` (ya introducido, todavía no alcanza el umbral).
- **completamente ausente**: sin exposición previa en el nivel.
- **expuesto-pero-escaso**: exposición en `N..2N-1` (banda ampliada; ya alcanzó el umbral pero sigue escaso). El borde inferior de esta banda es `count == N`: un lema con exactamente `N` exposiciones ya **no** es sub-expuesto y entra a "expuesto-pero-escaso"; la banda ampliada arranca en `N` y termina en `2N-1` inclusive (ver [DOUBT-EXPANDED-BAND-EDGE](#DOUBT-EXPANDED-BAND-EDGE)).

**Estado de banda (binario):** un lema "pertenece a una banda deficitaria" cuando su rango de frecuencia COCA (`cocaRank`) cae dentro del rango de frecuencias de una banda que está **por debajo de su objetivo** en ese nivel (una banda que el nivel necesita reforzar). Es pertenece / no pertenece; el sistema **no** ordena por magnitud del déficit de la banda.

La combinación produce el orden de tiers de [F-SLEM-R003](#F-SLEM-R003): dentro de la macro-escasez (`0..N-1`) la banda deficitaria es clave secundaria fuerte (tier 1 y 2 antes que 3 y 4), y dentro del mismo estado de banda el sub-expuesto le gana al ausente (1>2, 3>4). Los expuestos-escasos (`N..2N-1`) van siempre al final (tiers 5-6), ordenados entre sí por banda deficitaria (5>6). Dentro de cada tier, el desempate final es COCA ascendente ([F-SLEM-R005](#F-SLEM-R005)).

<details><summary>Detail</summary>

**Cortes con el umbral, no literales.** Los tres estados de exposición se expresan siempre con `N`, nunca con valores fijos como `4` o `10`. Sub-expuesto = `1..N-1`; ausente = sin exposición; expuesto-pero-escaso = `N..2N-1`. El borde `count == N` pertenece a "expuesto-pero-escaso" (la banda ampliada arranca en `N`), no a "sub-expuesto" — decisión formalizada en [DOUBT-EXPANDED-BAND-EDGE](#DOUBT-EXPANDED-BAND-EDGE).

**Naturaleza binaria del estado de banda.** La pertenencia a banda deficitaria es binaria. Dos lemas que pertenecen a bandas deficitarias distintas comparten el mismo estado de banda, caen en el mismo tier, y se resuelven entre sí por COCA ascendente. No hay "más deficitario primero".

**Una sola función de prioridad, sin branch de banda.** El orden se computa con una única función, sin ramas condicionales "hay / no hay banda deficitaria" ([F-SLEM-R015](#F-SLEM-R015)). Si el nivel no tiene bandas deficitarias (o la auditoría no analizó bandas), ningún lema satisface "pertenece a banda deficitaria": los tiers 1, 2 y 5 quedan vacíos y la misma función degrada a 3 → 4 → 6.

**Distinción respecto del déficit de exposición.** El estado de banda usa el déficit de **banda de frecuencia COCA del nivel** (cuánta cobertura le falta a una banda en ese nivel), señal distinta del déficit de **exposición de lemma-count** de un lema individual (cuánto le falta a un lema para su umbral). El déficit de exposición sigue **sin** usarse como criterio de orden dentro de un tier ([F-SLEM-R005](#F-SLEM-R005) y la cláusula OUT correspondiente). El **estado** de exposición (sub-expuesto / ausente / expuesto-escaso) sí determina el tier; su **magnitud** no.

**Sin campo observable nuevo por lema.** Esta regla no agrega ningún valor observable nuevo por lema ([F-SLEM-R013](#F-SLEM-R013)). El orden se observa combinando el `cocaRank` ya expuesto de cada lema, la distribución de bandas deficitarias del nivel y la señal de exposición (`lemmaCount`/`isUnderexposed`) que [F-SLEM-R013](#F-SLEM-R013) ya define.

**Precedencia completa (función de prioridad única):**

| Clave | Rol |
|-------|-----|
| Estado de exposición cortado por `N` (macro-escasez `0..N-1` vs expuesto-escaso `N..2N-1`) | Clave primaria: la macro-escasez precede siempre a expuesto-escaso |
| Pertenece a banda deficitaria del nivel (binario) | Clave secundaria fuerte dentro de la macro-escasez (tiers 1-2 sobre 3-4) y separadora de 5 sobre 6 |
| Sub-expuesto vs ausente dentro del mismo estado de banda | Terciaria: sub-expuesto le gana al ausente (1>2, 3>4) |
| COCA ascendente ([F-SLEM-R005](#F-SLEM-R005)) | Desempate final dentro del mismo tier |

**Error**: "El cómputo de tiers no aplicó el estado de exposición por N, la pertenencia binaria a banda deficitaria y COCA ascendente en el orden acordado"

</details>

### Rule[F-SLEM-R015] - Degradación elegante: los tiers de banda quedan vacíos, sin branch condicional
**Severity**: major | **Validation**: VALIDATED

> Cuando la auditoría **no** incluyó el análisis de bandas de frecuencia COCA del nivel (o el nivel no tiene bandas deficitarias), ningún lema satisface "pertenece a banda deficitaria": los tiers 1, 2 y 5 quedan vacíos y la **misma** función de prioridad ([F-SLEM-R014](#F-SLEM-R014)) degrada a 3 → 4 → 6, sin fallar en la generación, proyección ni consulta de `correctionContext.suggestedLemmas`.

La degradación **no** introduce una rama condicional "hay / no hay banda deficitaria": es la misma función de [F-SLEM-R014](#F-SLEM-R014) evaluada cuando el conjunto de bandas deficitarias es vacío. En ese caso solo quedan poblados los tiers 3 (sub-expuesto), 4 (completamente ausente) y 6 (expuesto-pero-escaso), y COCA ascendente ([F-SLEM-R005](#F-SLEM-R005)) sigue siendo el desempate final dentro de cada uno.

<details><summary>Detail</summary>

Esta degradación elegante espeja el patrón de [F-SLEM-R012](#F-SLEM-R012) (auditoría sin `lemma-count`): la ausencia de una señal de auditoría opcional no rompe la construcción del contexto; simplemente algunos tiers quedan vacíos. El esquema de tiers de [F-SLEM-R003](#F-SLEM-R003) y el límite de [F-SLEM-R006](#F-SLEM-R006) siguen aplicándose igual. Esta regla tampoco expone ningún valor observable nuevo por lema ([F-SLEM-R013](#F-SLEM-R013)).

Nótese que el estado de exposición cortado por `N` (sub-expuesto / ausente / expuesto-escaso) **sigue** determinando los tiers 3/4/6 incluso sin bandas: la degradación afecta solo la clave secundaria de banda, no la macro-escasez.

**Error**: "La ausencia del análisis de bandas de frecuencia COCA impidió construir suggestedLemmas con la función de prioridad degradada a 3-4-6"

</details>

## Context

`suggestedLemmas` alimenta el contexto que recibe la revisión de tareas `LEMMA_ABSENCE`. Antes de este cambio, la lista favorecía lemas detectados por ausencia o ubicación temporal y resolvía el orden con COCA ascendente.

Con la incorporación funcional de `lemma-count`, el sistema puede distinguir entre:

- lemas completamente ausentes;
- lemas ya introducidos, pero con exposición insuficiente;
- lemas mal ubicados temporalmente;
- lemas sin diagnóstico `LEMMA_ABSENCE`, pero sub-expuestos en su nivel correspondiente.

La prioridad de negocio acordada combina dos ejes: reforzar primero la escasez de exposición (palabras ya introducidas que todavía no alcanzan el mínimo, y palabras nuevas), y **dentro de esa escasez**, empujar hacia arriba las palabras que cubren una banda de frecuencia COCA que le falta al nivel. La pertenencia a banda deficitaria se elevó de "desempate intra-grupo" a **clave secundaria fuerte**: pesa más que la distinción sub-expuesto/ausente dentro de la macro-escasez.

Este cambio no crea un contexto de corrección independiente para `lemma-count`. La señal se incorpora dentro de `correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE`, usando los verbos públicos existentes.

### Esquema de seis tiers de prioridad

El ranking se define por **seis tiers** (first-match-wins), no por grupos por subtipo de ausencia ([F-SLEM-R003](#F-SLEM-R003), [F-SLEM-R014](#F-SLEM-R014)). Se combinan dos ejes:

- **Estado de exposición**, cortado por el umbral `N` de `lemma-count`: sub-expuesto (`1..N-1`), completamente ausente (sin exposición) y expuesto-pero-escaso (`N..2N-1`). La macro-escasez es `0..N-1`; los expuestos-escasos (`N..2N-1`) son una banda ampliada que va siempre al final.
- **Estado de banda** (binario): pertenece / no pertenece a una banda de frecuencia COCA del nivel que está **por debajo de su objetivo** ("banda deficitaria"). La auditoría determina, por nivel CEFR, qué bandas están por debajo de su objetivo e informa su rango de frecuencia (from/to); un lema pertenece cuando su `cocaRank` cae en el rango de una banda deficitaria.

Los tres invariantes del esquema: (a) sub-expuesto le gana a ausente **dentro del mismo estado de banda** (tier 1>2, 3>4); (b) la banda deficitaria empuja hacia arriba dentro de la macro-escasez, al punto de que un **ausente-deficitario (tier 2) le gana a un sub-expuesto-no-deficitario (tier 3)** —deliberado—; (c) los expuestos-escasos (tiers 5-6) van siempre al final, nunca superan a la macro-escasez. Dentro de cada tier, COCA ascendente es el desempate final ([F-SLEM-R005](#F-SLEM-R005)).

El subtipo de `LEMMA_ABSENCE` (`APPEARS_TOO_LATE` / `APPEARS_TOO_EARLY` / `COMPLETELY_ABSENT`) se **subsume** en las condiciones de count: completamente ausente alimenta los tiers 2/4, sub-expuesto alimenta los tiers 1/3. El orden fino por subtipo dentro de un mismo estado de count/banda no se conserva por defecto (ver [DOUBT-ABSENCE-SUBTYPE-TIEBREAK](#DOUBT-ABSENCE-SUBTYPE-TIEBREAK)). `APPEARS_TOO_EARLY` sigue evaluándose en el nivel target real ([F-SLEM-R004](#F-SLEM-R004)): eso gobierna *cómo se computa* la exposición que alimenta los tiers, es ortogonal al orden.

**Relación con `includeExposed` (FEAT-INCEXP).** El esquema de tiers define solo el **ORDEN**. *Qué* lemas entran al pool —en particular si se incluyen lemas ya expuestos con exposición `N..2N-1`— lo sigue gobernando la política existente de `includeExposed` ([FEAT-INCEXP](#references)), que es un **fallback opcional** (no default). Esta regla **no** cambia cuándo se traen los expuestos; solo define que, cuando están presentes, se ordenan en los tiers 5-6, siempre al final.

**Sin campo observable nuevo por lema.** El `cocaRank` ya expuesto de cada lema ([F-SLEM-R013](#F-SLEM-R013)), combinado con la distribución de bandas deficitarias del nivel que produce la auditoría y la señal de exposición (`lemmaCount`/`isUnderexposed`), alcanza para observar y verificar el orden resultante. La degradación cuando no hay bandas deficitarias es la misma función con los tiers de banda vacíos ([F-SLEM-R015](#F-SLEM-R015)), sin ramas condicionales.

## Scope

### IN

- Enriquecer `task.correctionContext.suggestedLemmas` para tareas `LEMMA_ABSENCE` con señal proveniente de `lemma-count`.
- Usar `lemma-count` como señal de priorización, no solo como dato visible.
- Permitir que lemas sub-expuestos entren en `suggestedLemmas` aunque no tengan diagnóstico `LEMMA_ABSENCE`.
- Ordenar `suggestedLemmas` por un esquema de **seis tiers de prioridad** (first-match-wins), combinando estado de exposición cortado por el umbral `N` y pertenencia binaria a banda deficitaria del nivel ([F-SLEM-R003](#F-SLEM-R003), [F-SLEM-R014](#F-SLEM-R014)).
- Cortar el estado de exposición con el umbral `N` (sub-expuesto = `1..N-1`, ausente = sin exposición, expuesto-pero-escaso = `N..2N-1`), sin literales; el borde `count == N` inicia la banda ampliada ([F-SLEM-R014](#F-SLEM-R014)).
- Elevar la pertenencia a banda deficitaria a clave secundaria fuerte: dentro de la macro-escasez, un ausente-deficitario (tier 2) queda antes que un sub-expuesto-no-deficitario (tier 3), de forma deliberada.
- Mantener, dentro del mismo estado de banda, que el sub-expuesto le gana al completamente ausente (tier 1>2, 3>4) ([F-SLEM-R007](#F-SLEM-R007)).
- Ubicar los lemas expuestos-pero-escasos (`N..2N-1`) siempre al final (tiers 5-6), ordenados entre sí por banda deficitaria (5>6).
- Evaluar `APPEARS_TOO_EARLY` en el nivel target real (gobierna el cómputo de exposición que alimenta los tiers, es ortogonal al orden) ([F-SLEM-R004](#F-SLEM-R004)).
- Mantener COCA ascendente como desempate final dentro de cada tier ([F-SLEM-R005](#F-SLEM-R005)).
- Degradar con la misma función de prioridad cuando la auditoría no incluyó el análisis de bandas: los tiers de banda (1, 2, 5) quedan vacíos y el orden degrada a 3 → 4 → 6, sin ramas condicionales ni fallar ([F-SLEM-R015](#F-SLEM-R015)).
- Mantener el límite existente de `suggestedLemmas` después de aplicar el ranking.
- Exponer el mismo contexto enriquecido en `PlanCommand.plan(..., true)` y `GetCommand.get("tasks", ...)`.
- Permitir que `ReviseCommand.revise(...)` consuma el contexto enriquecido derivado por el sistema.
- Permitir que `ReviseCommand.revise(...)` acepte overrides externos válidos con la nueva forma observable.

### OUT

- No se introduce un nuevo comando público.
- No se crea un nuevo tipo independiente de `correctionContext` para `lemma-count`.
- No se cambia la finalidad general de `LEMMA_ABSENCE`.
- No se cambia la configuración funcional de `lemma-count`, salvo usar el umbral `N` ya disponible.
- No se prioriza por **magnitud** del déficit de **exposición de lemma-count**. El déficit de exposición de un lema individual (cuánto le falta a un lema para su umbral) sigue **sin** usarse como criterio de orden dentro de un tier. Lo que sí determina el tier es el **estado** de exposición (sub-expuesto `1..N-1` / ausente / expuesto-escaso `N..2N-1`), no su magnitud. Es una señal distinta del **estado de banda de frecuencia COCA del nivel**, que se usa como clave secundaria binaria ([F-SLEM-R014](#F-SLEM-R014)).
- No se ordena por magnitud del déficit de banda de frecuencia COCA. El estado de banda de [F-SLEM-R014](#F-SLEM-R014) es binario (pertenece / no pertenece a una banda deficitaria del nivel), no "más deficitario primero".
- No cambia **qué** lemas entran al pool. La inclusión de lemas ya expuestos (`N..2N-1`) la sigue gobernando la política existente de `includeExposed` ([FEAT-INCEXP](#references)) como fallback opcional; el esquema de tiers solo define el orden cuando esos lemas están presentes (tiers 5-6, siempre al final).
- No se agrega ningún valor observable nuevo por lema ([F-SLEM-R013](#F-SLEM-R013)): el mecanismo es solo el ORDEN.
- No se define la estrategia interna de revisión; solo se define la información y el orden que recibe.
- No se recalcula `lemma-count` ad hoc durante la revisión.
- No se modifica el comportamiento de analizadores o contextos no relacionados con `LEMMA_ABSENCE` y `lemma-count`.

## User Journeys

### Journey[F-SLEM-J001] - Generar plan con suggestedLemmas enriquecido

```yaml
flow:
  - id: audit_course
    action: "El cliente ejecuta AuditRunner.runDetailedAudit(...) o AuditRunner.runAudit(...) para un curso"
    then: audit_has_lemma_count

  - id: audit_has_lemma_count
    action: "La auditoría queda disponible para planificación"
    outcomes:
      - when: "La auditoría incluyó lemma-count y el análisis de bandas de frecuencia COCA del nivel, junto con diagnósticos LEMMA_ABSENCE"
        then: plan_with_context_and_bands
      - when: "La auditoría incluyó lemma-count y diagnósticos LEMMA_ABSENCE, pero no incluyó el análisis de bandas de frecuencia COCA del nivel"
        then: plan_with_context_no_bands
      - when: "La auditoría incluyó diagnósticos LEMMA_ABSENCE pero no incluyó lemma-count"
        then: plan_without_lemma_count_signal

  - id: plan_with_context_and_bands
    action: "El cliente ejecuta PlanCommand.plan(auditId, storageMode, true)"
    gate: [F-SLEM-R001, F-SLEM-R002]
    then: assert_ranked_suggestions

  - id: assert_ranked_suggestions
    action: "Las tareas LEMMA_ABSENCE del plan exponen correctionContext.suggestedLemmas ordenado por los seis tiers: dentro de la macro-escasez un ausente-deficitario (tier 2) queda antes que un sub-expuesto-no-deficitario (tier 3), los expuestos-escasos (N..2N-1) quedan al final (tiers 5-6), y COCA ascendente desempata dentro de cada tier"
    gate: [F-SLEM-R003, F-SLEM-R004, F-SLEM-R005, F-SLEM-R006, F-SLEM-R007, F-SLEM-R013, F-SLEM-R014]
    result: success

  - id: plan_with_context_no_bands
    action: "El cliente ejecuta PlanCommand.plan(auditId, storageMode, true)"
    gate: [F-SLEM-R001, F-SLEM-R002]
    then: assert_ranked_suggestions_coca_only

  - id: assert_ranked_suggestions_coca_only
    action: "Sin señal de bandas, los tiers de banda (1, 2, 5) quedan vacíos y el orden degrada a 3 (sub-expuesto) → 4 (ausente) → 6 (expuesto-escaso), con COCA ascendente como desempate final dentro de cada tier, sin fallar"
    gate: [F-SLEM-R003, F-SLEM-R005, F-SLEM-R006, F-SLEM-R013, F-SLEM-R015]
    result: success

  - id: plan_without_lemma_count_signal
    action: "El cliente ejecuta PlanCommand.plan(auditId, storageMode, true)"
    gate: [F-SLEM-R012]
    then: assert_suggestions_without_signal

  - id: assert_suggestions_without_signal
    action: "Las tareas LEMMA_ABSENCE conservan suggestedLemmas con los tres valores de lemma-count no informados en todos los elementos"
    gate: [F-SLEM-R013]
    result: success
```

### Journey[F-SLEM-J002] - Consultar tareas con paridad respecto del plan

```yaml
flow:
  - id: setup_planned_tasks
    action: "Existe una tarea LEMMA_ABSENCE generada con PlanCommand.plan(..., true)"
    then: get_tasks

  - id: get_tasks
    action: "El cliente ejecuta GetCommand.get(\"tasks\", name, filter)"
    gate: [F-SLEM-R008]
    then: assert_same_context

  - id: assert_same_context
    action: "La tarea consultada expone el mismo correctionContext.suggestedLemmas enriquecido y ordenado que el plan"
    result: success
```

### Journey[F-SLEM-J003] - Revisar usando correctionContext derivado por el sistema

```yaml
flow:
  - id: setup_task_with_system_context
    action: "Existe una tarea LEMMA_ABSENCE con correctionContext.suggestedLemmas enriquecido por el sistema"
    then: revise_without_override

  - id: revise_without_override
    action: "El cliente ejecuta ReviseCommand.revise(taskId, planId, null, null)"
    gate: [F-SLEM-R009]
    then: assert_revision_receives_context

  - id: assert_revision_receives_context
    action: "La revisión consume el correctionContext.suggestedLemmas enriquecido y ordenado con lemma-count"
    result: success
```

### Journey[F-SLEM-J004] - Revisar usando correctionContext externo enriquecido

```yaml
flow:
  - id: setup_task_for_override
    action: "Existe una tarea LEMMA_ABSENCE que acepta correctionContext externo"
    then: override_validity

  - id: override_validity
    action: "El cliente aporta un correctionContext externo mediante ReviseCommand.revise(...)"
    outcomes:
      - when: "El correctionContext externo tiene la misma forma observable válida que GetCommand.get(...)"
        then: accept_override
      - when: "El correctionContext externo contiene datos inconsistentes de lemma-count"
        then: reject_override

  - id: accept_override
    action: "El sistema acepta el correctionContext externo y lo consume para la revisión"
    gate: [F-SLEM-R010, F-SLEM-R013]
    result: success

  - id: reject_override
    action: "El sistema rechaza el correctionContext externo como inválido y no aplica la revisión"
    gate: [F-SLEM-R011]
    result: failure
```

## Open Questions

### Doubt[DOUBT-SUGGESTED-LEMMA-FIELDS] - Campos observables de lemma-count dentro de suggestedLemmas
- [x] Cada elemento de `suggestedLemmas` expone como mucho tres valores: `lemmaCount`, `lemmaCountThreshold` e `isUnderexposed`. No se exponen otros valores derivados (por ejemplo, déficit explícito) ni un campo `suggestionReason`.

**Answer**: Resuelto. La forma observable se formaliza en [F-SLEM-R013](#F-SLEM-R013). Los tres valores se comportan en bloque: o los tres están informados (cuando la auditoría incluyó `lemma-count` y la señal aplica al lema), o los tres quedan no informados (cuando no aplica o no está disponible).

### Doubt[DOUBT-LEMMA-COUNT-UNAVAILABLE] - Representación de señal no disponible
- [x] Cuando la auditoría no incluyó `lemma-count`, los tres valores (`lemmaCount`, `lemmaCountThreshold`, `isUnderexposed`) quedan no informados en todos los elementos de `suggestedLemmas`. La generación y la consulta del contexto siguen funcionando con los criterios restantes.

**Answer**: Resuelto. La representación observable queda fijada en [F-SLEM-R012](#F-SLEM-R012) y [F-SLEM-R013](#F-SLEM-R013): "señal no disponible" significa que los tres valores quedan no informados en bloque.

### Doubt[DOUBT-COMPLETELY-ABSENT-EXPOSURE] - Representación observable de COMPLETELY_ABSENT
- [x] La señal de `lemma-count` no es aplicable a un lema `COMPLETELY_ABSENT` (el curso nunca lo introdujo). En consecuencia, los tres valores quedan no informados para esos elementos.

**Answer**: Resuelto. La prioridad ya estaba clara ([F-SLEM-R007](#F-SLEM-R007)). La representación observable se formaliza ahora en [F-SLEM-R007](#F-SLEM-R007) y [F-SLEM-R013](#F-SLEM-R013): `lemmaCount`, `lemmaCountThreshold` e `isUnderexposed` quedan no informados, incluso si la auditoría incluyó `lemma-count`.

### Doubt[DOUBT-ERROR-TEXT] - Texto exacto para override inválido
- [x] El mensaje exacto queda a discreción de la implementación, siempre que comunique inconsistencia de la señal de `lemma-count` dentro de `suggestedLemmas`.

**Answer**: Resuelto. La regla funcional ([F-SLEM-R011](#F-SLEM-R011)) ya define qué debe rechazarse. No se fija un literal exacto; lo que se valida observablemente es que la revisión no se aplique con datos inconsistentes.

### Doubt[DOUBT-BAND-DEFICIT-MARKER] - ¿Hace falta un marcador observable por lema para "pertenece a banda deficitaria"?
- [ ] Opción A — Exponer un valor observable nuevo por lema en `suggestedLemmas` (por ejemplo, un indicador `belongsToDeficientBand`) para poder afirmar directamente el estado de banda de cada lema.
- [x] Opción B (recomendada) — **No** agregar ningún valor observable nuevo por lema. El mecanismo es SOLO el ORDEN de `suggestedLemmas`. El `cocaRank` ya observable de cada lema ([F-SLEM-R013](#F-SLEM-R013)), combinado con la distribución de bandas de frecuencia COCA del nivel que produce la auditoría, alcanza para observar y verificar el orden resultante ([F-SLEM-R014](#F-SLEM-R014)).

**Recomendación del analista**: Opción B. Respeta el contrato de [F-SLEM-R013](#F-SLEM-R013) ("no se exponen otros valores derivados") y mantiene el cambio como una re-ordenación pura, sin nuevos campos. Un test puede fijar `cocaRank` de cada lema, la señal de exposición (`lemmaCount`/`isUnderexposed`) y una distribución de bandas del nivel con al menos una banda deficitaria conocida, y afirmar que el orden observable respeta los seis tiers (por ejemplo, que un ausente-deficitario queda antes que un sub-expuesto-no-deficitario), con COCA ascendente como desempate final dentro del tier.

**Answer**: Preferencia del analista fijada (Opción B) y encodeada así en [F-SLEM-R014](#F-SLEM-R014) y [F-SLEM-R013](#F-SLEM-R013). Queda ABIERTA para confirmación del usuario: si al implementar el test el equipo determina que el orden no es observable sin un marcador por lema, escalar a `@analyst` antes de agregar cualquier campo (no agregarlo por cuenta propia).

### Doubt[DOUBT-BAND-RANGE-OVERLAP] - ¿Puede un `cocaRank` caer en más de una banda deficitaria?
- [x] Las bandas de frecuencia COCA del nivel son rangos contiguos y disjuntos (top1k = 1–1000, top2k = 1001–2000, etc.), por lo que un `cocaRank` cae en a lo sumo una banda. El desempate binario ("pertenece / no pertenece a alguna banda deficitaria") no depende de resolver solapamientos.

**Answer**: Resuelto por el modelo de bandas existente (rangos from/to contiguos y disjuntos, ver FEAT-COCA). La pertenencia a banda deficitaria es una condición binaria bien definida: el `cocaRank` de un lema pertenece a una banda deficitaria si cae en el rango de alguna banda del nivel que está por debajo de su objetivo. No se ordena por magnitud ni se resuelven solapamientos.

<a id="DOUBT-ABSENCE-SUBTYPE-TIEBREAK"></a>
### Doubt[DOUBT-ABSENCE-SUBTYPE-TIEBREAK] - ¿Se conserva el orden fino por subtipo de ausencia dentro de un mismo tier?
- [x] Opción A (recomendada) — **Colapsar** el orden fino por subtipo. El subtipo de `LEMMA_ABSENCE` (`APPEARS_TOO_LATE` / `APPEARS_TOO_EARLY` / `COMPLETELY_ABSENT`) se subsume en las condiciones de count (ausente → tiers 2/4; sub-expuesto → tiers 1/3). Dentro de un mismo tier, el desempate es directamente COCA ascendente, sin sub-orden por subtipo. Es lo que encodean [F-SLEM-R003](#F-SLEM-R003) y [F-SLEM-R014](#F-SLEM-R014) hoy.
- [ ] Opción B — Conservar "APPEARS_TOO_LATE primero" como desempate **más profundo** dentro de un mismo estado de count y banda, ANTES de COCA ascendente. Recupera la preferencia del ranking anterior por lemas que aparecen demasiado tarde, al costo de un criterio de orden adicional.

**Recomendación del analista**: Opción A. El esquema nuevo prioriza explícitamente por estado de exposición (count) y banda deficitaria; el subtipo de ausencia queda subsumido y su orden fino relativo pierde señal frente a esos dos ejes. Perder "APPEARS_TOO_LATE primero" es una regresión menor: dentro de un mismo estado de count y banda, dos lemas con distinto subtipo hoy se resuelven por COCA ascendente, que sigue siendo un criterio razonable. Si el usuario considera que "APPEARS_TOO_LATE primero" aporta valor de negocio suficiente, se puede reintroducir como desempate más profundo (después de banda, antes de COCA) — pero no se inventa por cuenta propia.

**Answer**: PENDIENTE de confirmación del usuario. Encodeado provisionalmente como Opción A (subtipo colapsado) en [F-SLEM-R003](#F-SLEM-R003) y [F-SLEM-R014](#F-SLEM-R014). Si el usuario elige Opción B, el analista agrega el desempate por subtipo como capa entre "estado de banda" y "COCA ascendente" dentro del tier.

<a id="DOUBT-EXPANDED-BAND-EDGE"></a>
### Doubt[DOUBT-EXPANDED-BAND-EDGE] - ¿La banda ampliada arranca en `N` o en `N+1`?
- [x] Opción A (recomendada) — La banda ampliada (expuesto-pero-escaso, tiers 5-6) arranca en `count == N` y termina en `2N-1` inclusive. Un lema con exactamente `N` exposiciones ya **no** es sub-expuesto (sub-expuesto = `1..N-1`, estrictamente por debajo del umbral) y entra a "expuesto-pero-escaso". Con default `N=4`: sub-expuesto = 1..3, expuesto-escaso = 4..7.
- [ ] Opción B — La banda ampliada arranca en `N+1`, dejando `count == N` fuera de todos los tiers de escasez (ni sub-expuesto ni expuesto-escaso). Requeriría definir qué tier recibe a un lema con exactamente `N` exposiciones.

**Recomendación del analista**: Opción A. Es coherente con la semántica de umbral existente de `lemma-count`: `isUnderexposed` es verdadero cuando `lemmaCount < lemmaCountThreshold` ([F-SLEM-R013](#F-SLEM-R013)), es decir sub-expuesto = `1..N-1`. Un lema con `count == N` ya alcanzó el umbral (`isUnderexposed = false`), por lo que naturalmente sale de la macro-escasez y, si sigue escaso (`< 2N`), cae en la banda ampliada. Opción B dejaría un hueco en `count == N` sin tier asignado, lo que contradice "cada lema cae en exactamente un tier".

**Answer**: Encodeado como Opción A en [F-SLEM-R014](#F-SLEM-R014) (banda ampliada = `N..2N-1`, borde inferior `count == N`). Queda ABIERTA para confirmación del usuario del borde exacto; si el usuario prefiere Opción B, hay que definir explícitamente el tier de `count == N`.

## References

- `FEAT-LCOUNT`: aporta la señal funcional de `lemma-count`, incluyendo el umbral de exposición.
- `FEAT-REVCTX`: define que `ReviseCommand.revise(...)` puede recibir un `correctionContext` externo con la misma forma observable que `GetCommand.get(...)`.
- `FEAT-PLANEF`: define que los planes efímeros pueden exponer `correctionContext` proyectado mediante `PlanCommand.plan(..., true)`.
- `FEAT-LAPS` / `FEAT-LAGEN`: consumen el contexto de revisión de `LEMMA_ABSENCE`, incluyendo `suggestedLemmas`.
- `FEAT-COCA` / `FEAT-DCOCA`: aportan, por nivel, qué bandas de frecuencia COCA están por debajo de su objetivo ("bandas deficitarias") y el rango de frecuencia (from/to) de cada banda. Es la señal funcional que habilita el estado de banda de [F-SLEM-R014](#F-SLEM-R014). Su ausencia se maneja como degradación elegante en [F-SLEM-R015](#F-SLEM-R015).
- `FEAT-INCEXP`: gobierna, como fallback opcional (no default), **qué** lemas ya expuestos (exposición `N..2N-1`) entran al pool de candidatos. El esquema de tiers de esta feature no cambia esa política; solo define que, cuando esos lemas están presentes, se ordenan en los tiers 5-6, siempre al final ([F-SLEM-R003](#F-SLEM-R003), [F-SLEM-R014](#F-SLEM-R014)).