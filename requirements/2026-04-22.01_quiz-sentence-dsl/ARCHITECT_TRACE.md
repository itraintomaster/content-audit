# Traza reconstruida del Architect — FEAT-QSENT

Este archivo reconstruye paso a paso lo que hizo el `@architect` en las dos corridas que produjeron `architectural_patch.yaml` y `TECH_SPEC.md`. Se compone a partir de: la definición del agente (`.claude/agents/architect.md`), los reportes finales de cada corrida, y los artefactos que quedaron en disco (`decisions.md`, `progress.md`, patch aplicado, snapshot).

No es un log literal de tool calls — el Agent tool solo devuelve el reporte final, no el stream de invocaciones — pero es lo más fiel que se puede reconstruir con los artefactos disponibles.

## Contexto

- **Feature:** `FEAT-QSENT` — formalizar `quizSentence` como concepto de primera clase del course-domain.
- **Agent:** `architect` (Opus 4.7, tools: `Read`, `Bash`; skills: `sentinel-arch-explore`, `sentinel-dsl-ref`, `sentinel-tech-spec`).
- **Hard rule:** el architect solo puede escribir via dos comandos CLI — `sentinel patch propose` y `sentinel tech-spec write`. No tiene `Write` ni `Edit`. Toda salida a disco pasa por esos dos comandos.
- **Workflow:** Step 1 (read + ask) → Step 2 (present options) → Step 3 (submit patch) → Step 4 (write tech spec) → Step 5 (iterate).
- **Dos invocaciones fueron necesarias:** la primera produjo una propuesta con factory + records wrapper; el usuario pidió simplificarla; la segunda la rehizo sin factory y sin wrappers.

---

## Corrida 1 — Propuesta inicial

- **agentId:** `ac461c0062f877b0c`
- **Duración:** 185s (~3 min)
- **Tool uses:** 20
- **Tokens totales:** 85.269

### Paso 1 — Carga de contexto y lectura del requerimiento

El architect arranca con una conversación fresca. Lo primero que hace es cargar el contexto que necesita:

1. **Read `REQUIREMENT.md`** (la fuente primaria, que le pasé en el brief). Contiene 27 reglas en 7 grupos, 1 journey, 5 doubts (2 para arquitectura: `DOUBT-ESCAPE-CHARS`, `DOUBT-CONVERTER-LOCATION`).

2. **Read `memory/decisions.md` y `memory/progress.md`** (protocolo de memoria del agent). Encuentra entradas del analyst con 3 doubts ya cerrados y decisiones sobre convenciones (multi-variant options, plain sentence como `List<String>`, etc.).

3. **Read `memory/fix-log.md`** — vacío en ese momento.

### Paso 2 — Overview de la arquitectura actual

El protocolo del agent dicta que su primera acción real es un `listModules` — un overview barato antes de zoom in:

```bash
java -jar sentinel-core-...jar tool listModules --root .
```

Esto le devuelve la tabla de módulos que está al final de su agent definition: 10 módulos, sus dependencias, interfaces e implementaciones. Barato, da el mapa.

### Paso 3 — Drill-down sobre los módulos tocados

Para diseñar dónde poner `QuizSentenceConverter` y cómo impacta `AuditableQuiz`/`CourseToAuditableMapper`, inspecciona los tres módulos involucrados:

```bash
java -jar sentinel-core-...jar tool inspectModule --root . --module course-domain
java -jar sentinel-core-...jar tool inspectModule --root . --module audit-domain
java -jar sentinel-core-...jar tool inspectModule --root . --module audit-application
```

Cada `inspectModule` devuelve el módulo + contratos de sus dependencias transitivas. Acá fue donde probablemente leyó:
- `FormEntity`, `SentencePartEntity`, `SentencePartKind` en course-domain.
- `AuditableQuiz` con su campo `sentence: String` en audit-domain.
- `CourseToAuditableMapper` con su dependencia a `NlpTokenizer` en audit-application.

### Paso 4 — Consulta de skills relevantes

Antes de componer el patch, carga las skills que le fueron asignadas:

- **`sentinel-dsl-ref`** — para el schema exacto del DSL de Sentinel (cómo declarar packages, visibility, patterns como Factory Seam, etc.).
- **`sentinel-arch-explore`** — para los comandos CLI de exploración (ya usados en pasos 2 y 3).
- **`sentinel-tech-spec`** — reservada para el paso 7.

### Paso 5 — Decisión arquitectural interna

Con el contexto cargado, el agent razona (sin tool calls visibles). Aplica los principios P1–P8 de su propia definition:

- **P2 (package as encapsulation unit):** la conversión tiene varios colaboradores (serializer, parser, deriver, normalizer) → pide un package con visibilidad restringida.
- **P5 (contract/carrier/engine):** interfaces públicas en un package, implementaciones en otro internal.
- Patrón aplicado: **Factory Seam** del catálogo (línea 377 del agent definition) — "Interface with stereotype: factory in the module root; its implementation public inside an otherwise internal package".

Decisiones tomadas en este paso:
- Dos packages nuevos en course-domain: `quizsentence` (public) y `quizsentenceengine` (internal).
- Records `QuizSentence` y `PlainSentences` como wrappers sobre String / List<String>.
- Factory (`QuizSentenceConverterFactory`) + impl (`DefaultQuizSentenceConverterFactory`) como seam.
- **DOUBT-ESCAPE-CHARS** → Option D (prohibición + fail-fast). Razón registrada en `decisions.md`: "real corpus (db/english-course) has zero literal brackets or four-underscore sequences; escape grammar is complexity without demand".
- **DOUBT-CONVERTER-LOCATION** → Option B (domain service en package público dedicado). Razón: "keeps entity records as pure data; canonical Public-Port-Hidden-Adapter + Factory Seam matches existing style (nlp-infrastructure / revision-domain/engine)".
- `AuditableQuiz.sentence: String` **renombrado** a `sentences: List<String>` (delete + add, no retype silencioso).

### Paso 6 — Proponer el patch

```bash
java -jar sentinel-core-...jar patch propose -i sentinel.yaml \
  --requirement-folder requirements/2026-04-22.01_quiz-sentence-dsl/ <<'PATCH'
...YAML...
PATCH
```

El CLI valida el patch contra `sentinel.yaml` + reglas ArchUnit. Respuesta: OK, **2 additions, 5 modifications, 0 conflicts**. El patch queda en `architectural_patch.yaml`, el snapshot del baseline se congela en `sentinel-baseline.yaml` (608KB — el estado "antes" frozen).

Si hubo iteraciones por errores de validación (YAML mal indentado, referencias que no resolvían, etc.), no quedó rastro en la memoria — el reporte dice "validated OK" sin mencionar retries. Probablemente entró al primer intento dado que los 20 tool uses se distribuyen bien entre lectura + propose + tech-spec + memoria.

### Paso 7 — Escribir el TECH_SPEC

Carga la skill `sentinel-tech-spec` (si no la había cargado antes) para la forma correcta: chunking por sección, fences ```architecture``` validados como subset del patch, convención del WHY.

```bash
java -jar sentinel-core-...jar tech-spec write \
  --requirement-folder requirements/2026-04-22.01_quiz-sentence-dsl/ <<'SPEC'
...markdown con fences...
SPEC
```

Cada fence se valida como subset: el CLI comprueba que cada elemento mencionado en el fence existe en `architectural_patch.yaml`. Un error típico acá es "references X which is not in the patch" — ese caso obliga a arreglar el fence o re-proponer el patch.

Resultado: **6 fences, todos subset verificado, OK**.

### Paso 8 — Update de memoria

Dos entradas appended a `decisions.md` (una por doubt resuelto + una por la decisión del rename de `AuditableQuiz`). Una entrada a `progress.md` describiendo el estado final y el siguiente paso (`Ready for @qa-tester`).

### Paso 9 — Reporte final al orchestrator

El agent devuelve el reporte al padre (yo) con: módulos tocados, tipos nuevos, resolución de los dos doubts con el WHY, impacto en `AuditableQuiz`, confirmación de que ambos CLI commands cerraron OK.

### Distribución estimada del tiempo (185s / 20 tool uses)

| Actividad | Tool uses | Tiempo aprox |
|---|---|---|
| Carga de contexto (READMEs, memory, REQUIREMENT.md) | 3-4 | 20-30s |
| Overview + inspectModule de 3 módulos | 4 | 25-35s |
| Lectura de skills (sentinel-dsl-ref, sentinel-tech-spec) | 2-3 | 15-25s |
| Composición mental del patch (inferencia Opus, sin tool) | 0 | 20-30s |
| `patch propose` (+ eventual retry) | 1-2 | 10-20s |
| `tech-spec write` (6 fences, cada una se valida) | 1 | 15-25s |
| Update de decisions.md / progress.md | 2 | 10-15s |
| Reporte final (inferencia) | 0 | 5-10s |

Total: ~150-200s, matches con los 185s reportados.

---

## Corrida 2 — Simplificación tras feedback

- **agentId:** `a5ad4d4f59a3d0c0a`
- **Duración:** 283s (~4.7 min)
- **Tool uses:** 19
- **Tokens totales:** 82.831

Esta corrida fue disparada por feedback del usuario: "Para qué hacer que QuizSentence y PlainSentence sean clases y no Strings? Para qué hacer transformers y no que el mismo Entity tenga el método de transformación?". El usuario eligió la opción (2) — interface + impl sin wrappers, sin factory.

### Paso 1 — Carga de contexto (más pesada que la corrida 1)

Como `SendMessage` no estaba disponible en este entorno, la corrida 2 **no hereda el cache** de la corrida 1. Arranca fresca. Tiene que releer:

1. **Read `REQUIREMENT.md`** otra vez (42KB).
2. **Read `architectural_patch.yaml` actual** — ahora hay uno ya propuesto para revisar y modificar.
3. **Read `TECH_SPEC.md` actual** (10KB).
4. **Read `memory/decisions.md` y `memory/progress.md`** — ya con las entradas de la corrida 1.

El cache miss acá es la razón principal por la que esta corrida demoró **más** que la primera (283s vs 185s) a pesar de tener menos tool uses (19 vs 20): cada turno paga la deuda de leer contexto desde cero.

### Paso 2 — Diff mental del patch actual vs objetivo

El brief que le mandé era muy específico: "Borrar: QuizSentenceConverterFactory, DefaultQuizSentenceConverterFactory, record QuizSentence, record PlainSentences. Dejar: interface, impl, excepciones, AuditableQuiz rename, decisiones de los doubts".

El agent internaliza el diff. Usó pocas llamadas a `inspectModule` acá — el contexto ya venía del patch leído.

### Paso 3 — Componer el patch de simplificación

Regla del protocolo (línea 230 del agent definition, "Iterating on an unapplied patch"):

> Cuando revisás un proposal que no fue aplicado, recordá que el CLI hace merge. Para **reemplazar** una estructura (mover impls a un package, renombrar, tightening visibility), emití `_change: "delete"` explícito para las definiciones que sobran.

Esto es clave. Para que el factory desaparezca del patch, no alcanza con "no mencionarlo" — el merge lo preservaría. Hay que emitir:
- `_change: "delete"` sobre la interface `QuizSentenceConverterFactory`.
- `_change: "delete"` sobre la impl `DefaultQuizSentenceConverterFactory`.
- `_change: "delete"` sobre los records `QuizSentence` y `PlainSentences`.
- `_change: "modify"` sobre las signatures de `QuizSentenceConverter` (ahora trabajan con String y List<String> crudos).
- `_change: "modify"` sobre `DefaultQuizSentenceConverter` para sacarle el campo `config` y ajustar `requiresInject`.
- `_change: "delete"` sobre el pattern declaration `Factory`.

### Paso 4 — `patch propose` (segunda vez, merge con el existente)

```bash
java -jar sentinel-core-...jar patch propose ...
```

El CLI hace merge del nuevo patch con el existente. Los `_change: "delete"` operan sobre el estado merged: remueven los elementos del proposal anterior. Resultado reportado: **2 additions, 5 modifications, 0 conflicts** — el mismo total que la corrida 1 (porque la suma final de cambios es parecida, solo que ahora hay deletes en el camino).

### Paso 5 — Reescribir el TECH_SPEC

Acá probablemente estuvo el mayor gasto. Cada fence del spec se valida como subset del patch **final mergeado**. Con los elementos borrados, cualquier fence que los referenciara rompía la validación. El agent reescribió el spec con 5 fences (uno menos que antes porque el factory desapareció), agregó dos secciones nuevas con WHY explícito:

- *"Use String and List<String> directly — no wrapper records"* — justifica la ausencia de los records.
- *"Hide the engine in an internal package with a single public seam — no factory"* — justifica la ausencia del factory citando P3 (versatility on demand, sin strategies alternativas) y contrastando con `nlp-infrastructure` (SpaCy state) y `revision-domain/engine` (pluggable Revisers).

Cada ronda de validación de subset puede forzar ajustes si el fence menciona un elemento que no existe en el patch merged.

### Paso 6 — Update de memoria

Entry nueva en `decisions.md`:
> 2026-04-20 — architect — QSENT design simplified: removed QuizSentenceConverterFactory, DefaultQuizSentenceConverterFactory, record QuizSentence, record PlainSentences. Converter signatures now use String and List<String> directly. [...]
> why: QSENT is a stateless pure function with zero alternative implementations and no costly state — a factory decides nothing [...]. Wrapper records over String and List<String> added ceremony at every call site without carrying new invariants.

Entry nueva en `progress.md`:
> 2026-04-20 — architect — simplified ARCH-QSENT patch + TECH_SPEC.md per user feedback. [...] Patch validated (2 additions, 5 modifications, 0 conflicts); tech-spec validated (5 fences, all subset). Ready for @qa-tester.

### Distribución estimada del tiempo (283s / 19 tool uses)

| Actividad | Tool uses | Tiempo aprox |
|---|---|---|
| Re-carga de contexto (sin cache): REQUIREMENT, patch actual, tech-spec actual, memory | 4-5 | 45-60s |
| `inspectModule` puntual (probablemente poco, ya tenía el patch) | 1-2 | 10-15s |
| Composición mental del patch de simplificación (inferencia pesada) | 0 | 40-60s |
| `patch propose` con merges de deletes | 1-2 | 15-25s |
| `tech-spec write` con reescritura de fences + validación subset | 1-2 | 30-45s |
| Posibles retries de tech-spec si algún fence referenciaba elementos borrados | 1-2 | 20-40s |
| Update de decisions.md / progress.md | 2 | 10-15s |

Total: ~180-280s, matches con los 283s.

**Por qué demora más que la corrida 1 con menos tool uses:**
1. **Cache miss completo al arrancar.** La corrida 1 benefició del cache warmo de mi conversación (le pasé el contexto directo); la corrida 2 también, pero tuvo que leer el patch + tech-spec existentes que no existían antes.
2. **Diff mental más complejo.** Modificar un patch existente con deletes explícitos requiere más razonamiento que proponer uno desde cero.
3. **TECH_SPEC reescrito más grande.** Dos secciones nuevas con WHY explícito justificando la ausencia de cosas (ausencia es más difícil de justificar que presencia).
4. **Validación subset en tech-spec con el patch merged.** Cualquier drift entre fence y estado final forzaba retry.

---

## Resumen de tools usadas (ambas corridas)

| Tool | Uso |
|---|---|
| `Read` (file tool) | REQUIREMENT.md, TECH_SPEC.md, patch files, memory/*.md — 8-10 reads totales entre ambas corridas |
| `Bash` → `sentinel tool listModules` | 1 por corrida: overview barato de módulos |
| `Bash` → `sentinel tool inspectModule` | 3-4 en corrida 1 (course-domain, audit-domain, audit-application); 1-2 en corrida 2 |
| `Bash` → `sentinel tool describeComponent` | Probablemente 0-1; no necesitó describir componentes individuales |
| `Bash` → `sentinel patch propose` | 1 por corrida (más eventuales retries por validación) |
| `Bash` → `sentinel tech-spec write` | 1 por corrida (más eventuales retries por subset) |
| Skills cargadas vía sistema | `sentinel-dsl-ref`, `sentinel-arch-explore`, `sentinel-tech-spec` |

No usó (porque no puede, no las tiene):
- `Write`, `Edit`, `NotebookEdit` — todas bloqueadas para el architect.
- `Grep`, `Glob` — en teoría tendría acceso a Read y Bash pero no a Grep/Glob; la exploración estructural la hace vía CLI (`listModules`, `inspectModule`).

---

## Momentos donde "fue y volvió"

La mayor parte del trabajo fue lineal (carga → decisión → propose → spec → memoria). Los puntos donde hubo ida y vuelta probable:

1. **Validación del patch.** Si algún `_change: "delete"` apuntaba a un elemento con el nombre mal escrito, el CLI rechazaba. No hay evidencia en memoria de que esto pasó, pero en esta arquitectura con 10 módulos y nombres repetidos (`QuizSentence...`) es plausible que la corrida 2 haya reintentado una o dos veces.

2. **Validación de subset en tech-spec.** Cada fence se valida como subset del patch final. Si la corrida 2 escribió inicialmente un fence que aún mencionaba el factory (por descuido), el CLI lo rechazaba hasta que se alineara.

3. **Interpretación del brief.** En la corrida 2, mi brief decía "borrar QuizSentenceConverterFactory" — el architect tuvo que traducir eso a `_change: "delete"` en el patch + eliminar las declaraciones en los fences + actualizar el texto narrativo explicando el WHY de la ausencia. Son tres lugares que tienen que quedar coherentes.

---

## Gap conocido de esta reconstrucción

**No tengo acceso al tool-use log real del agent.** Lo anterior se reconstruye a partir de:
- Reportes finales del Agent tool (totales agregados, no paso a paso).
- Artefactos en disco (memoria, patches, specs).
- Protocolo declarativo del agent definition (lo que está obligado a hacer).

Un log literal con timestamps tool-por-tool requeriría instrumentación que el harness actual no expone al orchestrator.
