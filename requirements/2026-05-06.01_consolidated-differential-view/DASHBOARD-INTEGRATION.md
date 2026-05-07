# FEAT-CDIFF v3 — Guía de integración para el dashboard

> **Audiencia**: agente que implementa el consumidor (dashboard / UI) de la salida consolidada del CLI `content-audit`.
>
> **Versión**: v3 (2026-05-07). **Reemplaza la v2 fallida** descripta en la sección "Cambios respecto de v2".

---

## TL;DR

El CLI emite, por cada `AuditReport` con par activo `(auditId, planId)` resoluble, **un único documento JSON** que combina:

- el baseline del análisis (los scores y diagnósticos que el motor calculó cuando se corrió el `analyze`),
- los efectos de las propuestas **aceptadas** del plan activo (foto `consolidated`),
- los efectos de las propuestas **pendientes aplicables** del plan activo (foto `pendingProjection`).

Cada nodo del árbol del curso afectado por al menos una de esas propuestas trae un **mapa de `fieldChanges`** donde cada entrada es:

```json
"<path-estable-al-leaf>": {
  "original":          <valor en el baseline>,
  "consolidated":      <valor con aceptadas>,
  "pendingProjection": <valor con aceptadas + pendientes>
}
```

El consumidor (el dashboard) **no recibe deltas precomputados** — los computa por su cuenta sobre la tripleta. Tampoco hay una sección paralela de "estadísticas afectadas": los scores cambiados aparecen como entradas más del mapa `fieldChanges` (con su path correspondiente, p. ej. `scores.SENTENCE_LENGTH`).

---

## Cambios respecto de v2 (versión fallida)

### Lo que la v2 emitía y la v3 ya **no** emite

| v2 (deprecada) | v3 (vigente) |
|---|---|
| `nodeImpacts[].snapshotBefore` (objeto crudo del nodo en baseline) | borrado — la información ahora vive descompuesta en `fieldChanges` con los paths estables (R006, R019). |
| `nodeImpacts[].snapshotAfterConsolidated` y `snapshotAfterPending` | borrado — ídem. |
| Sección `statisticImpacts[]` paralela con `acceptedDelta` y `pendingDelta` precomputados | borrada (R009). Los scores cambiados son `fieldChanges` con keys tipo `scores.<DIMENSION>` y tripleta como cualquier otro field. |
| `nodeImpacts[].pendingProposalId` (un único proposalId por nodo) | renombrado a `pendingProposalIds: string[]` (R011). Un nodo puede acumular varias pendientes que tocan fields distintos. |

### Lo que la v3 introduce

- **Forma uniforme `field → tripleta`** por nodo afectado (R006). Un solo iterador para diagnósticos tipados, contenido del nodo y scores escalares.
- **Descubrimiento dinámico de fields** (R019). Cualquier hoja escalar cuyo valor difiera entre al menos dos fotos aparece automáticamente — sin enumeración estática en el contrato. Si mañana se agrega un analizador nuevo, sus campos empiezan a aparecer sin cambios al consumidor.
- **Path estable por leaf** (R023). Cada entrada del mapa `fieldChanges` lleva una key con la secuencia de campos desde la raíz del modelo del nodo hasta el leaf, separados por `.`.
- **Diff de listas** (R022): por **identidad declarada** cuando el dominio la conoce (p.ej. `AbsentLemma` por `(lemma, pos)`), o **posicional** cuando no.
- **Exclusiones por rol funcional** (R020): timestamps, IDs opacos de persistencia (`_id`, `$oid`), referencias estructurales (`parentId`, `milestoneId`, etc.), índices internos (`position`, `index`) **no** se emiten como cambios, aunque difieran entre fotos.

### Por qué cambió

La v2 obligaba al consumidor a leer dos rutas distintas (snapshots crudos por nodo + estadísticas paralelas) y a parsear el snapshot para sacar valores. Los **diagnósticos tipados** que el motor calcula sobre el árbol consolidado y el árbol con pendientes (token count proyectado, listas de `AbsentLemma` agregadas, distribución COCA por banda, etc.) se descartaban antes de salir. La v3 los expone uniformemente.

---

## Comandos del CLI

### `set-active` — fija el par activo `(auditId, planId)`

```bash
content-audit set-active --audit <auditId> --plan <planId>
content-audit set-active --clear      # quita el par activo
```

- **No-destructivo, idempotente** (R002): no modifica `AuditReport`, plan, propuesta ni archivo del curso. Llamarlo con el par activo ya vigente es no-op.
- Sin par activo, `get-consolidated` emite `consolidatedAvailability: UNAVAILABLE` con `reason: NO_ACTIVE_ANALYSIS`.

### `get-consolidated` — emite la salida consolidada

```bash
content-audit get-consolidated <coursePath>                  # default: --format json
content-audit get-consolidated <coursePath> --format json
```

**Argumento `<coursePath>`**: ruta absoluta o relativa al directorio del curso (donde vive `_course.json`).

**Exit code**: `0` siempre que el comando termina correctamente, **incluso cuando** la salida lleva `consolidatedAvailability: UNAVAILABLE` (R013). El consumidor distingue por el campo `consolidatedAvailability` en la raíz del JSON, no por el exit code.

**No persiste nada** (R015): la operación es read-only sobre el sistema de archivos. No genera un `AuditReport` nuevo, no toca planes, propuestas ni archivos del curso.

---

## Shape del JSON de salida

### Caso `AVAILABLE` (par activo resoluble, salida construida)

```json
{
  "activeAuditId":           "2026-05-06T10-00-00",
  "activePlanId":            "2026-05-06T10-05-00",
  "consolidatedAvailability": "AVAILABLE",
  "computedAt":              "2026-05-07T14:23:11.123Z",

  "nodeImpacts": [
    {
      "nodeTarget":          "QUIZ",
      "nodeId":              "quiz-a1-adverbs-001",
      "acceptedProposalIds": ["prop-001"],
      "pendingProposalIds":  ["prop-002"],
      "fieldChanges": {
        "entity.quizSentence.text": {
          "original":          "She runs quick.",
          "consolidated":      "She runs quickly.",
          "pendingProjection": "She runs very quickly."
        },
        "diagnoses.sentenceLengthDiagnosis.tokenCount": {
          "original":          3,
          "consolidated":      3,
          "pendingProjection": 4
        },
        "scores.SENTENCE_LENGTH": {
          "original":          0.65,
          "consolidated":      0.85,
          "pendingProjection": 0.70
        }
      }
    },
    {
      "nodeTarget":          "MILESTONE",
      "nodeId":              "a1",
      "acceptedProposalIds": ["prop-001"],
      "fieldChanges": {
        "diagnoses.lemmaAbsenceLevelDiagnosis.absencePercentage": {
          "original":          0.42,
          "consolidated":      0.38,
          "pendingProjection": 0.38
        },
        "diagnoses.lemmaAbsenceLevelDiagnosis.absentLemmas[lemma=run,pos=v].weightedScore": {
          "original":          0.85,
          "consolidated":      0.85,
          "pendingProjection": 0.40
        }
      }
    }
  ],

  "pendingApplicability": [
    {
      "proposalId": "prop-003",
      "status":     "NOT_APPLICABLE",
      "reason": {
        "reason": "ELEMENT_BEFORE_MISMATCH",
        "detail": "Una aceptada anterior desplazó el elementBefore esperado por la pendiente."
      }
    }
  ]
}
```

### Caso `UNAVAILABLE` (no se pudo construir la salida)

```json
{
  "activeAuditId":            null,
  "activePlanId":             null,
  "consolidatedAvailability": "UNAVAILABLE",
  "unavailabilityReason": {
    "reason": "NO_ACTIVE_ANALYSIS",
    "detail": "No hay par activo seleccionado para este curso."
  }
}
```

`reason` puede tomar (R013):

| Categoría | Cuándo aparece |
|---|---|
| `NO_ACTIVE_ANALYSIS` | El curso no tiene par activo, o el `auditId` activo no existe en disco. |
| `ACTIVE_PLAN_UNAVAILABLE` | El `planId` activo no se ubica dentro del análisis (referencia rota / archivo ausente). |
| `INCONSISTENT_PROPOSAL` | Una propuesta aceptada referencia un `nodeId` que no existe en el subárbol al re-aplicar. |
| `REAGGREGATION_FAILED` | El motor no pudo recomputar el agregado sobre el subárbol modificado. |
| `OTHER` | Cualquier otra causa, con `detail` textual obligatorio. |

### Caso `AVAILABLE` con consolidado vacío

Si hay par activo pero ninguna propuesta aceptada ni pendiente aplicable toca el árbol, la salida es `AVAILABLE` con `nodeImpacts: []` (R003). El dashboard lo distingue de `UNAVAILABLE` por la presencia de `consolidatedAvailability: "AVAILABLE"` y por la ausencia de `unavailabilityReason`.

---

## Anatomía de la key del `fieldChanges`

La key codifica el path estable desde la raíz del nodo hasta el leaf escalar. Convenciones:

- **Separador**: `.`
- **Atributos anidados**: se concatenan con `.` (p.ej. `entity.quizSentence.text`).
- **Listas con identidad declarada**: el segmento del elemento usa la identidad natural entre corchetes, con clave-valor separada por `=` y múltiples claves separadas por `,`. Ejemplo:
  ```
  diagnoses.lemmaAbsenceLevelDiagnosis.absentLemmas[lemma=run,pos=v].weightedScore
  ```
- **Listas sin identidad declarada**: el segmento usa el índice posicional entre corchetes (`[0]`, `[1]`, …).
- **Estabilidad** (R023): la misma hoja, alcanzada por el mismo path en dos nodos del mismo tipo, **emite la misma key**. El dashboard puede correlacionar entre nodos sin parsear variantes del path.

### Identidades naturales conocidas hoy

| Tipo de elemento | Clave natural |
|---|---|
| `AbsentLemma` (en `LemmaAbsenceLevelDiagnosis.absentLemmas`) | `lemma`, `pos` (composite `lemmaAndPos`) |
| `MisplacedLemma` (en `LemmaAbsenceLevelDiagnosis.misplacedLemmas`) | `lemma`, `pos` |
| `BucketResult` (en `CocaBucketsLevelDiagnosis.bucketResults`, etc.) | `bandName` |
| `BucketSummary` (en `LevelBucketDistribution.bucketSummaries`) | `bandName` |
| `QuarterResult` (en `CocaBucketsLevelDiagnosis.quarterResults`) | `index` |
| `ProgressionAssessment` (en `CocaProgressionDiagnosis.assessments`) | `bandName` |
| `ImprovementDirective` (en `CocaProgressionDiagnosis.improvementDirectives`) | `type`, `bandName`, `levelName` |

### Campos que **nunca** aparecen como `fieldChanges` (R020)

- IDs opacos de persistencia: `_id`, `oid`, `$oid`.
- Referencias estructurales: `parentId`, `childId`, `courseId`, `milestoneId`, `topicId`, `knowledgeId`, `quizId`.
- Timestamps: `createdAt`, `updatedAt`, `lastModifiedAt`, `timestamp`.
- Órdenes internos: `index`, `position` (cuando son del almacenamiento, no parte de identidad de elemento).

---

## Algoritmo de consumo recomendado

```pseudo
1. Invocar `content-audit get-consolidated <coursePath>` y parsear el JSON.

2. Si consolidatedAvailability == "UNAVAILABLE":
     mostrar banner con unavailabilityReason.reason + detail
     usar el AuditReport (vía `get` o `analyze` previo) como fallback de lectura
     fin.

3. Para cada entry en nodeImpacts:
     a. Identificar el nodo en el árbol del curso por (nodeTarget, nodeId).
     b. Para cada (path, tripleta) en fieldChanges:
        - Resolver qué dimensión visual corresponde al path
          (p.ej. "entity.quizSentence.text" → texto del quiz;
                "scores.SENTENCE_LENGTH" → score por dimensión).
        - Decidir presentación de la tripleta:
            * solo `consolidated` (estado vigente con aceptadas)
            * `consolidated` + overlay de `pendingProjection` (preview de pendientes)
            * comparativa visual `original → consolidated → pendingProjection`
        - Computar deltas si los necesita la UI:
            acceptedDelta  = consolidated  - original           (sobre numéricos)
            pendingDelta   = pendingProjection - consolidated   (sobre numéricos)
            (en strings / objetos los "deltas" son diff textual,
            responsabilidad de la UI)
     c. Mostrar en la tarjeta del nodo qué `acceptedProposalIds` y
        `pendingProposalIds` lo afectan (links a las propuestas vía FEAT-PIPRE
        / FEAT-REVAPR).

4. Para cada entry en pendingApplicability (si existe):
     mostrar la propuesta marcada como NOT_APPLICABLE con su reason
     (categoría + detalle), con un CTA para que el operador la regenere
     o la rechace vía FEAT-REVAPR.
```

### Cómputo de deltas (lado consumidor)

El contrato no incluye `acceptedDelta` ni `pendingDelta` (R010). El consumidor los computa con la semántica que prefiera:

```ts
// numéricos
const acceptedDelta  = consolidated  - original;
const pendingDelta   = pendingProjection - consolidated;

// porcentaje relativo (cuidado con division by zero)
const acceptedPctRel = original !== 0
  ? (consolidated - original) / original
  : null;

// puntos porcentuales (cuando original/consolidated ya son ratios 0..1)
const acceptedPctPts = (consolidated - original) * 100;
```

**No reescalar**: las restas son sobre la escala interna del dominio. Decidir entre relativo (1.05x) vs absoluto (+5pp) es decisión de presentación.

### Casos típicos de la tripleta

| Caso | `original` | `consolidated` | `pendingProjection` | Significado |
|---|---|---|---|---|
| Solo aceptada | `A` | `B` | `B` | Una aceptada movió el field; no hay pendiente que lo toque. |
| Solo pendiente | `A` | `A` | `B` | No hay aceptada; una pendiente lo movería si se aceptara. |
| Aceptada + pendiente | `A` | `B` | `C` | Una aceptada lo movió; una pendiente sobre el mismo field lo movería más. |
| Aceptada + pendiente reverten | `A` | `B` | `A` | La pendiente, si se acepta, vuelve al estado original. |

Si los tres valores fuesen iguales, el field **no se emite**; nunca aparece en `fieldChanges`.

---

## Limitaciones explícitas (qué **no** entrega esta feature)

- **Multi-análisis**: la salida cubre un único `(auditId, planId)`. Si el operador quiere ver pendientes dormidas de otro análisis, debe cambiar el par activo con `set-active` (R004, R017).
- **Propuestas estructurales** (creación / eliminación / reordenamiento de nodos): excluidas del contrato (R018). Hoy todas las propuestas son sustituciones sobre `nodeId` estable.
- **Operaciones de aceptar / rechazar / regenerar**: no son de FEAT-CDIFF — usar los verbos de FEAT-REVAPR.
- **Persistencia del consolidado**: no hay (R015). Cada `get-consolidated` se computa on-demand; sin caches oficiales observables. No hay un "consolidatedReport.json" en disco.

---

## Archivos de referencia

- Contrato funcional: `requirements/2026-05-06.01_consolidated-differential-view/REQUIREMENT.md`
- Arquitectura: `requirements/2026-05-06.01_consolidated-differential-view/TECH_SPEC.md`
- Formatter (shape JSON exacto): `audit-cli/src/main/java/com/learney/contentaudit/auditcli/formatting/DefaultConsolidatedViewFormatter.java`
- CLI verbs: `audit-cli/src/main/java/com/learney/contentaudit/auditcli/commands/GetConsolidatedCmd.java` y `SetActiveCmd.java`
- Modelos de salida: `revision-domain/src/main/java/com/learney/contentaudit/revisiondomain/consolidatedview/{ConsolidatedView, NodeImpact, FieldChange, NonApplicablePending}.java`
- Tests journey end-to-end (cómo construir fixtures equivalentes al runtime real): `revision-domain/src/test/java/com/learney/contentaudit/revisiondomain/engine/FCdiffJ00{2,3,4,5,8}JourneyTest.java` y `audit-cli/src/test/java/com/learney/contentaudit/auditcli/commands/FCdiffJ00{1,6}JourneyTest.java`
