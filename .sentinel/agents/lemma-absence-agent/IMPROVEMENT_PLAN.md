# Lemma-Absence Agent — Plan de mejoras (las métricas AGREGADAS empeoran)

> Documento durable para ejecutar por fases en contextos separados. Cada fase se
> VALIDA corriendo revisiones reales desde cero e inspeccionando el LOG del agente
> (no basta con "completed": el PROCESO tiene que ser correcto). Iterar hasta que
> funcione. Relacionado con la memoria [[project_lemma_curation_node]],
> [[project_eval_length_tokenizer_reuse]], [[project_repeated_lemmas_gap]].

## Diagnóstico (por qué caen las agregadas aunque cada quiz "mejora +5pp")
Al revisar un quiz de LEMMA_ABSENCE el agente **reescribe la oración entera** (no solo
el lema mal ubicado). Ej: "They pass a lot of time in the mountains" → "Many people
speak English". El "+5pp" es el score LOCAL de ese quiz (ya no tiene lema fuera de
nivel). Pero las agregadas miden el curso completo:
- **lemma-absence (cobertura) −18pp milestone / −12pp course**: cada reescritura tira
  vocabulario A1 que daba cobertura (mountains/bicycle/stories…). 400 reescrituras =
  mucha cobertura perdida.
- **coca-buckets −10pp (Q2)**: los reemplazos se concentran en palabras comunes →
  sesga la distribución de frecuencia.
- **lemma-count +1pp**: menos lemas distintos, más repetición.
- **sentence-length −1/−2pp**: hay un GAP real (ver Fase 1 / Tarea 3).

Tensión local-vs-global: optimizar cada quiz por separado degrada el perfil de
vocabulario del curso. **Muchas métricas a gobernar son GLOBALES**, pero el agente
corre POR-TAREA contra un audit CONGELADO (`context.getSourceAuditId()`); no ve los
cambios que otras revisiones del mismo batch van haciendo. Eso limita cuánto se puede
arreglar sin proyección (Fase 3).

Dato verificado: la tool `get_suggested_lemmas` se ata al audit congelado, así que las
palabras removidas NO vuelven a ofrecerse dentro del mismo batch (solo tras re-auditar
el curso proyectado/aplicado). El coca-analyzer YA calcula el déficit por bucket
(`DefaultImprovementPlanner` → `ImprovementDirective{bandName, frequencyRangeFrom/To,
actualPercentage, targetPercentage}`, `BucketResult{count, targetPercentage}`).

---

## Metodología de validación (COMÚN a todas las fases) — LEER SIEMPRE

**Regla de oro:** validar cada cambio corriendo revisiones REALES desde cero y
revisando el LOG del agente. No alcanza con `status: completed`; hay que confirmar que
el PROCESO fue correcto (la tool se llamó como se espera, el pool salió del catálogo,
el flujo de nodos fue el correcto, no hubo soft-pass silencioso, el candidato cumple).
Ajustar (prompt/script/código) y re-correr hasta que esté bien.

### Cómo correr una revisión (modo HUMAN → NO toca la db)
```bash
CONTENT_AUDIT_APPROVAL_MODE=HUMAN \
CONTENT_AUDIT_LAGEN_PROVIDER=claude-cli \
CONTENT_AUDIT_LAGEN_MODEL=sonnet \
CONTENT_AUDIT_LAGEN_ENDPOINT=http://localhost \
CONTENT_AUDIT_LAGEN_TIMEOUT=180 \
CONTENT_AUDIT_LAGEN_MAX_EVAL_RETRIES=5 \
bash audit-cli.sh revise task <TASK_ID> --plan 2026-06-26T02-16-38 -c db/english-course
```
- Provider default `claude-cli` (claude -p, sin API key); ENDPOINT/MODEL son placeholders
  requeridos igual. `claude` tiene que estar en PATH (el wrapper lo agrega).
- Correr desde la raíz del repo (spaCy resuelve rutas por `user.dir`). En background si tarda.
- Cambios en `.sentinel/agents/**` son **runtime** (sin rebuild). Cambios en Java →
  `mvn -q -DskipTests -Dexec.skip=true install` antes de correr (el wrapper usa target/classes).

### Selección de casos (el usuario rechaza el batch → las tareas quedan revisables)
- Plan activo: **`2026-06-26T02-16-38`** (source audit `2026-06-26T02-16-28`), ~6350 tareas.
- Listar candidatos: `bash audit-cli.sh get tasks --diagnosis LEMMA_ABSENCE --plan 2026-06-26T02-16-38 -f json`
- Ver el quiz de una tarea (nodeId = id del quiz): buscar en `db/english-course/**/quizzes.json`.
- Semilla conocida: **`task-3667`** = "browse - ___ [browsing]" (verb+ing, A1, transformación).
- Elegir 3-5 casos por fase según los criterios de cada fase (abajo). VERIFICAR las
  propiedades del caso ANTES de correr (que sea sentence-style, que tenga palabra de
  count bajo, que el nivel tenga déficit de bucket, etc.).

### Inspección del log (checklist de "proceso correcto")
Run dir: `.sentinel/runs/lemma-absence-agent/<ID>/` (el más reciente).
- `run.json` → `status: completed` (no `failed`/HALTED) + `terminationReason`.
- `events.jsonl` → flujo de nodos (`node_started`): load_context → route_curate →
  curate_lemmas → gate_curation → finalize_curation → generate → gate_candidate →
  eval_length → eval_distinct → eval_quality → tally → route_verdict → emit.
- Tool: `llm_response type=tool_calls name=get_suggested_lemmas` + `tool_result`
  (¿args esperados? ¿includeExposed? ¿resultado NO vacío?).
- Artefactos: `curation_spec.json` (filtros+veto del LLM) y `curated_lemmas.json`
  (`{words, source:"tool", excluded, ...}`) — `source` debe ser `tool`, no fallback.
- Evals a stderr del CLI: `[curate] source=tool words=N`, `eval_length: candidate=N
  tokens, target=A-B → OK`, `[eval] blocking passed 6/6`, `candidate: ...`.
- Comparar BEFORE (quiz original) vs AFTER (candidato) según el criterio de la fase.

### Cleanup después de cada corrida
Rechazar la propuesta de prueba para dejar la tarea revisable:
`CONTENT_AUDIT_APPROVAL_MODE=HUMAN bash audit-cli.sh reject proposal <PROPOSAL_ID> --reason "validation artifact"`
(el PROPOSAL_ID lo imprime revise; formato `task-XXXX-<num>`). NUNCA `approve` (tocaría la db).

---

## FASE 1 — Barata, ataca la raíz, sin proyección (empezar por acá)
Objetivo: que las 3 agregadas dejen de CAER. Con 1a + 3 solo, debería alcanzar para eso.

### Tarea 1a — Edición mínima en `generate`
**Qué:** que `generate` cambie SOLO el/los lema(s) mal ubicado(s) y CONSERVE el resto de
la oración; que no reescriba de cero. Reduce el churn colateral que hunde absence/coca.
- Archivos: `nodes/generate.md` (prompt). Declarativo/runtime.
- Ojo con `eval_distinct` (#7): hoy empuja a diferenciarse de los hermanos → fomenta
  reescrituras grandes. Balancear: preferir cambio mínimo salvo que colisione con un
  hermano; recién ahí variar más (contexto/sujeto), no por defecto.
- Mantener molde (transformación vs oración) y los otros gates (longitud, sentido, etc.).
**Validación (casos):** 3-5 quizzes SENTENCE-style con UN lema mal ubicado y buen
vocabulario A1 alrededor (ej. "You desire a new bicycle" → debería quedar "You need a
new bicycle", NO "You need a new house"). Incluir 1 transformación (task-3667) para
confirmar que el molde no se rompe.
**Criterio de éxito:** en el AFTER, las content-words NO mal-ubicadas se preservan
(diff palabra-a-palabra: cambia el lema objetivo, el resto queda). 6/6 blocking. Log OK.

### Tarea 3 — `eval_length` no-regresión SIEMPRE (+ revisar soft-pass)
**Qué:** hoy `eval_length` solo chequea el rango cuando el audit marcó problema de
longitud (`lengthGuidance` presente); para LEMMA_ABSENCE la longitud suele venir "none"
→ eval_length es NO-OP → la reescritura corre la longitud fuera de rango sin freno → cae
el agregado. Fix: chequear SIEMPRE contra el rango del nivel (no-regresión), aunque el
original no tuviera flag. Hay CEFR level; hay que exponer el target range del nivel
siempre (hoy solo viaja en `lengthGuidance` cuando hay problema → probablemente toque
Java: `LemmaAbsenceAgentGenerator.buildInputs` para pasar el rango del nivel siempre, o
un input `lengthRange`). → REBUILD si toca Java.
**Además:** confirmar que `content-audit tokens` NO hace soft-pass silencioso en el
batch (si el subprocess falla, eval_length pasa sin medir = bug persistente). Revisar
`scripts/eval_length.sh` + el log (`measure unavailable` no debe aparecer).
**Validación (casos):** quizzes con `lengthGuidance="none"` y oración larga (cerca del
máx del nivel) donde una reescritura podría pasarse. Incluir uno donde el candidato se
pase para confirmar que ahora BLOQUEA (antes pasaba).
**Criterio de éxito:** el candidato queda dentro del rango del nivel SIEMPRE; el log
muestra `eval_length: candidate=N tokens, target=A-B → OK` (midió de verdad, no soft-pass).

### Tarea 4 — `includeExposed` como fallback (no default)
**Qué:** revertir el default agresivo. 1ª consulta de `get_suggested_lemmas` SIN
`includeExposed` (trae AUSENTES = mejoran cobertura); solo escalar a `includeExposed:
true` si vuelve vacío/insuficiente. Opcional: combinar con ampliar el umbral de
apariciones (`<4` → `<10`) como otra vía de traer más sin exponer genéricos (ver 2b).
- Archivos: `nodes/curate_lemmas.md` (prompt/lógica). Declarativo/runtime.
**Validación (casos):** (A) task-3667 (browse): includeExposed=false devuelve
excite/favourite/practise (no vacío) → el log NO debe mostrar escalada a includeExposed=true.
(B) un caso donde includeExposed=false vuelva vacío → el log DEBE mostrar la 2ª llamada
con includeExposed=true.
**Criterio de éxito:** el log de tool_calls refleja la política (primero false; true solo
como fallback). Pool sigue `source=tool`.

### Validación integral de Fase 1
Correr 5-8 revisiones variadas (sentence-style + transformación + oración larga),
inspeccionar logs, y confirmar (idealmente) vía el consolidated/proyectado que las
agregadas ya no caen (o caen mucho menos). Iterar 1a/3/4 hasta que el proceso y el
resultado sean correctos en todos los casos elegidos.

---

## FASE 2 — Dirigida (media). Requiere Java (rebuild); parte, gobernado.
Objetivo: que las revisiones MEJOREN cobertura y balance de buckets, no solo "no rompan".

### Tarea 1b — Señal de apariciones por palabra (preservar load-bearing)
**Qué:** enriquecer el contexto con, por cada content-word de la oración original, su
`lemma+pos+count` global (de lemma-count). El agente usa eso para saber qué palabras son
"load-bearing" (count bajo = frágil, NO quitar; ej. bicycle 1/4). Umbral = el threshold
de lemma-count (config, default 4). Complementa 1a: no solo "cambiá poco" sino "estas
puntualmente NO las toques".
- Toca Java: el resolver tokeniza la oración + join con lemma-count stats + nuevo
  input/campo al grafo. Posible cambio de modelo (gobernado) si se agrega al
  CorrectionContext. → REBUILD.
**Validación (casos):** oraciones con una content-word de count=1 (buscar vía
lemma-count). **Criterio:** el agente preserva esa palabra en el AFTER.

### Tarea 2 / 2b — Ranking bucket-aware + filtrado
**Qué:** ordenar `suggestedWords` según necesidad de bucket. Reusar el déficit que ya
calcula el coca-analyzer (`DefaultImprovementPlanner`/`ImprovementDirective`): mapear el
`cocaRank` de cada lema a su bucket (via `frequencyRangeFrom/To`) y priorizar los del
bucket deficitario (actual% < target%). 2b: si el bucket no está equilibrado, ampliar el
umbral de apariciones (`<4`→`<10`) y poner primero los del bucket necesitado; marcar en
el output de la tool que ESAS son las prioritarias (el agente debe saberlo).
- Toca Java: ranking en `LemmaAbsenceContextResolver.buildEnrichedSuggestedLemmas` o en
  el query port + cablear la señal de bucket. → REBUILD, parte gobernado.
- **Gate de bucket — OJO:** el bucket es métrica GLOBAL; un quiz lo mueve una fracción y
  el agente no ve el agregado. NO hacer gate por-quiz de "mejoró el bucket". Sí:
  (a) proxy por-tarea = "el lema elegido pertenece al bucket que había que reforzar"
  (medible localmente); (b) chequeo AGREGADO post-batch sobre el proyectado. El gate
  agregado real necesita Fase 3.
**Validación (casos):** un nivel con un bucket bajo target (identificar vía coca
analysis / `get analyzers` o el consolidated). **Criterio:** las suggestedWords priorizan
ese bucket; el lema elegido cae en él; el log muestra la señal de bucket.

### Validación integral de Fase 2
Correr revisiones en casos con load-bearing words y con déficit de bucket; confirmar en
logs que se preservan las frágiles y que se elige del bucket correcto. Idealmente, un
mini-batch + consolidated para ver que coca-buckets y lemma-absence AGREGADOS suben (o
al menos no bajan).

---

## FASE 3 — Arquitectónica (grande). Habilita el control real de las agregadas.
Objetivo: análisis PROYECTADO vivo durante el batch → (a) las palabras removidas vuelven
a ofrecerse; (b) gate AGREGADO de bucket/cobertura de verdad.

### Tarea — Análisis proyectado durante el batch
**Qué:** que la tool / el agente consulten un análisis que refleje los cambios
acumulados del batch (pending+approved aplicados y re-auditados), no el audit congelado.
Es el gap de [[project_repeated_lemmas_gap]] / Track-2 (dashboard proyectado + `--audit`).
Habilita que bicycle/mountains reaparezcan como ausentes → se re-ofrecen, y que se pueda
gate-ear el agregado (bucket/cobertura) de verdad.
- Costo alto: re-proyección corriente entre revisiones o análisis proyectado inyectado.
  Definir alcance con el usuario antes de encarar.
**Validación:** batch chico donde una revisión remueve una palabra y la siguiente la
ve reaparecer en suggestedWords (proyectado); y las agregadas del consolidated mejoran
o se mantienen sobre el batch completo.

---

## Estado actual (base sobre la que se construye) — commit `72addfd`
Ya funcionan y están commiteados: tool real (gate_curation), binding nodeId, pool
determinista + veto (curationSpec/curatedLemmas), eval_length reusa tokenizer del audit,
generate respeta el orden del ranking. Este plan corrige los EFECTOS AGREGADOS.

## Estado FASE 1 — DONE & commiteada `ba676d5` (2026-07-01)
#11/#12/#13 implementadas y validadas: determinista (audit-cli 320 tests verdes,
`sentinel verify` OK, `tokens --level` + `eval_length.sh` e2e) + 4 revisiones live GREEN
(3667 transf · 3731/3702 KEEP_SAME · 3800 SHORTEN): edición mínima en las 4, longitud
medida contra el rango del nivel (no soft-pass), includeExposed nunca escaló a true.
Commiteados solo 6 archivos código/spec (generate.md, curate_lemmas.md, eval_length.md,
eval_length.sh, TokensCmd.java, Main.java), sin db/meta ni este doc.

**Hallazgos:** (a) ninguna tarea LEMMA_ABSENCE trae `lengthGuidance="none"` (todas tienen
dirección) → `eval_length` YA enforzaba; el valor real de #12 fue soft-pass VISIBLE +
reuse (`tokens --level`) + red de seguridad. (b) el pool solo-ausentes (#13) a veces da
palabras válidas pero raras ('cow'/'fine') → lo mejora Fase 2 #16 (ranking bucket-aware).
(c) confirmación AGREGADA (mini-batch + consolidated proyectado) DIFERIDA: a esta escala un
mini-batch no mueve el agregado medible. Para re-correr una tarea del plan hay que rechazar
antes su propuesta pendiente del batch (`.content-audit/revisions/<plan>/task-XXXX-*.json`).
