---
patch: ARCH-PIPRE-001
requirement: 2026-05-03.01_proposal-impact-preview
generated: 2026-05-03T00:00:00Z
---

# Tech Spec: Preview de impacto de propuestas de revision

Este patch enchufa el preview de impacto al flujo existente de generacion de propuestas (FEAT-REVBYP / FEAT-LAPS / FEAT-LAGEN) sin tocar el `RevisionArtifact` persistido. La estrategia general es: un puerto de calculo (`ImpactPreviewComputer`) que toma `(curso actual, propuesta)` y devuelve un `ImpactPreview` (disponible o no), un puerto de persistencia hermano (`ImpactPreviewStore`) que guarda el preview indexado por `proposalId`, y un par de formatters / view-models en la CLI que convierten los valores 0..1 del dominio en porcentaje al renderear. El cómputo es eager (una sola vez por propuesta, en el momento de generación, F-PIPRE-R001) e inmutable una vez persistido (F-PIPRE-R008).

## Encapsular el SPI del preview en un package publico de revision-domain

La feature exige que el operador vea el preview "junto a la propuesta" pero el preview es informacion **derivada** del par `(curso, propuesta)`, no un campo del `RevisionArtifact`. Lo correcto es un SPI propio: carriers + puerto de calculo. Lo metemos en un package `impactpreview` dentro de `revision-domain` (no en un modulo nuevo) porque cada carrier es dato puro sobre tipos que `revision-domain` ya maneja (`AuditTarget`, `RevisionProposal`) y el computador solo necesita el `AuditEngine` ya disponible via `dependsOn`. Visibility `public` para que `audit-cli` pueda renderizar y `audit-infrastructure` pueda persistir.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: impactpreview
        _change: add
        visibility: public
        description: >
          Public SPI for the impact preview of a RevisionProposal (FEAT-PIPRE).
          Houses the eager what-if contract (ImpactPreviewComputer port) and the
          carriers the operator-facing rendering needs to read.
```

## Modelar el diff jerarquico con queries puntuales

Las reglas R004 (deltas a lo largo de la cadena nodo -> contenedor -> curso) y R005 (deltas por dimension dentro de cada nivel) piden un modelo navegable. Lo resolvemos con tres records anidados — `LevelImpact` (un nivel jerarquico), `DimensionDelta` (un analyzer dentro del nivel), `ScoreDelta` (los tres valores). El `ImpactPreview` los expone como `levelImpacts: List<LevelImpact>` ordenados de hoja a raiz, mas un par `availability + unavailability` para distinguir UNAVAILABLE de "no hubo cambios" (R009 detail 2). La diferencia se almacena pre-calculada (`after - before`) para que el consumidor no la re-derive y para chequearla puntualmente en tests; los tres valores quedan en escala 0..1 del dominio, la conversion a porcentual la hace el formatter (R013, ver mas abajo).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: impactpreview
        _change: add
        models:
          - name: ScoreDelta
            _change: add
            type: record
            visibility: public
            fields:
              - { name: before, type: double }
              - { name: after, type: double }
              - { name: difference, type: double }
          - name: DimensionDelta
            _change: add
            type: record
            visibility: public
            fields:
              - { name: dimension, type: String }
              - { name: delta, type: ScoreDelta }
          - name: LevelImpact
            _change: add
            type: record
            visibility: public
            fields:
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: aggregateDelta, type: ScoreDelta }
              - { name: dimensionDeltas, type: "List<DimensionDelta>" }
          - name: ImpactPreview
            _change: add
            type: record
            visibility: public
            fields:
              - { name: proposalId, type: String }
              - { name: computedAt, type: Instant }
              - { name: availability, type: ImpactPreviewAvailability }
              - { name: unavailability, type: ImpactPreviewUnavailability }
              - { name: levelImpacts, type: "List<LevelImpact>" }
```

## Tipar la no-disponibilidad con categoria + detalle (DOUBT-UNAVAILABILITY-TAXONOMY)

R009 enumera tres causas previstas pero deja la forma exacta abierta. Resolvemos como **Opcion C** (categoria enum + detalle texto), igual que `LlmGenerationFailureCategory` resolvio FEAT-LAGEN R006: la categoria es filtrable y enumerable, el detalle preserva contexto humano. El enum cierra el conjunto en `TARGET_NODE_ABSENT` / `BASE_AUDIT_UNAVAILABLE` / `SIMULATION_FAILED` / `OTHER` — uno por cada causa de la tabla R009 mas un escape para no-anticipadas, sin convertir el enum en un catalogo arbitrariamente largo. `ImpactPreviewAvailability` es un enum binario (AVAILABLE / UNAVAILABLE) que distingue "no hubo cambios" de "no se pudo computar" (R009 detail 2).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: impactpreview
        _change: add
        models:
          - name: ImpactPreviewAvailability
            _change: add
            type: enum
            visibility: public
            fields:
              - { name: AVAILABLE }
              - { name: UNAVAILABLE }
          - name: ImpactPreviewUnavailabilityReason
            _change: add
            type: enum
            visibility: public
            fields:
              - { name: TARGET_NODE_ABSENT }
              - { name: BASE_AUDIT_UNAVAILABLE }
              - { name: SIMULATION_FAILED }
              - { name: OTHER }
          - name: ImpactPreviewUnavailability
            _change: add
            type: record
            visibility: public
            fields:
              - { name: reason, type: ImpactPreviewUnavailabilityReason }
              - { name: detail, type: String }
```

## Definir el puerto de calculo del preview

`ImpactPreviewComputer` es el seam unico por el que el motor de revision solicita el preview. La firma toma el `CourseEntity` actual (en memoria, ya cargado por el `RevisionEngine`) y la `RevisionProposal` recien construida. Devuelve un `ImpactPreview` siempre — nunca lanza: las tres causas de R009 (nodo ausente, auditoria base no recuperable, simulacion fallida) se materializan como `availability: UNAVAILABLE` con la categoria correspondiente. Esto le permite al engine cumplir R010 (el preview no aborta la persistencia de la propuesta) sin envolver la llamada en try-catch defensivo. Lo registramos como `glossarySuggestions` por si el analista lo eleva a termino canonico.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: impactpreview
        _change: add
        interfaces:
          - name: ImpactPreviewComputer
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "compute(CourseEntity currentCourse, RevisionProposal proposal): ImpactPreview"
            glossarySuggestions:
              - name: Impact Preview
                technicalName: ImpactPreview
                kind: domain-service-candidate
                basedOn: revision-domain/impactpreview/ImpactPreviewComputer
                derivedFrom: ["Revision Proposal", "Audit Report"]
```

## Persistir el preview como sidecar (no como campo del RevisionArtifact)

R007 pide que el preview se vea junto a la propuesta, pero el `RevisionArtifact` ya tiene un contrato consolidado por FEAT-REVBYP / FEAT-REVAPR. Embeberle un campo `impactPreview` mezclaria responsabilidades y rompe los tests existentes del store. La alternativa es un **store sidecar** — `ImpactPreviewStore` — con la misma forma `save(...) / findByProposalId(...)` que `RevisionArtifactStore`. Lo ponemos en el module root (no en el package `impactpreview`) por consistencia con `RevisionArtifactStore`: es un Public Port + Hidden Adapter clasico, el adapter vive en `audit-infrastructure`. El cliente principal (`GetCmd`) carga propuesta y preview en la misma operacion de lectura, asi que el operador los ve juntos sin un comando aparte (descarta DOUBT-PREVIEW-CLI-EXPOSURE Opcion B).

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: ImpactPreviewStore
        _change: add
        stereotype: port
        visibility: public
        exposes:
          - signature: "save(ImpactPreview preview): void"
          - signature: "findByProposalId(String proposalId): Optional<ImpactPreview>"
```

## Implementar el adapter de filesystem en audit-infrastructure

`FileSystemImpactPreviewStore` reusa la convencion de directorio del `FileSystemRevisionArtifactStore` (raiz `.content-audit/revisions/<planId>/`) escribiendo un archivo hermano por preview, p.ej. `<proposalId>.preview.json`. Asi el `RevisionArtifact` serializado no cambia su forma y un consumidor que ignore el preview sigue funcionando contra el artefacto viejo. Visibility `public` porque `audit-cli` (composition root) lo instancia directamente.

```architecture
modules:
  - name: audit-infrastructure
    _change: modify
    description: >
      Filesystem persistence adapters for audit reports, refinement plans,
      revision artifacts and impact previews.
    implementations:
      - name: FileSystemImpactPreviewStore
        _change: add
        implements: [ImpactPreviewStore]
        visibility: public
        types: [Repository]
```

## Implementar el computador detras del seam interno del engine

`DefaultImpactPreviewComputer` vive en el package `engine` (visibility `internal`) de `revision-domain`, junto al resto de las internals (`DispatchingReviser`, `IdentityReviser`, etc.). Es package-private: el unico seam publico es la interface `ImpactPreviewComputer`. La implementacion encadena cuatro puertos ya existentes — `AuditReportStore` (baseline `before`), `CourseElementLocator.replace` (aplicar `elementAfter` en memoria), `CourseMapper.map` (mapear sin disco), `AuditEngine.runAudit` (recomputar scores) — y construye los deltas comparando el `AuditReport` baseline contra el simulado. Las tres causas de R009 se materializan en bloques try/catch o checks explicitos sobre cada uno de esos puertos. R002 se cumple por construccion: ningun puerto involucrado en la simulacion persiste un `AuditReport` ni asigna un `auditId`; el `runAudit` del engine es funcion pura sobre el `AuditableCourse` que recibe.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultImpactPreviewComputer
            _change: add
            implements: [ImpactPreviewComputer]
            requiresInject:
              - { name: courseMapper, type: CourseMapper }
              - { name: auditEngine, type: AuditEngine }
              - { name: elementLocator, type: CourseElementLocator }
              - { name: auditReportStore, type: AuditReportStore }
```

## Enchufar el computo dentro del DefaultRevisionEngine

El preview se computa una sola vez por propuesta (R001). El punto natural es `DefaultRevisionEngine.revise(...)`, despues de que el `Reviser` construye la `RevisionProposal` y antes/alrededor del `artifactStore.save(...)`. El engine recibe los dos puertos nuevos (`impactPreviewComputer`, `impactPreviewStore`) por inyeccion: invoca el computer, persiste el `ImpactPreview` por el store, y continua el flujo normal. Como el computer **nunca** lanza (encapsula su falla en `availability: UNAVAILABLE`), el engine no necesita try/catch defensivo y R010 se cumple por contrato del puerto.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultRevisionEngine
            _change: modify
            requiresInject:
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: contextResolver, type: "CorrectionContextResolver<CorrectionContext>" }
              - { name: reviser, type: Reviser }
              - { name: validator, type: RevisionValidator }
              - { name: artifactStore, type: RevisionArtifactStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: elementLocator, type: CourseElementLocator }
              - name: impactPreviewComputer
                type: ImpactPreviewComputer
                description: >
                  Invoked exactly once per generated RevisionProposal, after the
                  proposal is constructed (F-PIPRE-R001). Failures of the computer
                  are NEVER propagated up; the engine treats the returned
                  ImpactPreview (AVAILABLE or UNAVAILABLE) as data and persists
                  it as a sidecar.
              - name: impactPreviewStore
                type: ImpactPreviewStore
                description: >
                  Persists the ImpactPreview returned by the computer, indexed by
                  proposalId. Deliberately separate from RevisionArtifactStore
                  (DOUBT-BATCH-PREVIEW future).
```

## Extender RevisionEngineConfig con los puertos del preview

La factory `RevisionEngineFactory.create(RevisionEngineConfig)` es el seam unico de armado. Para que el composition root pueda pasarle al engine los puertos nuevos hay que extender el carrier con tres campos: `courseMapper`, `auditEngine` y `impactPreviewStore`. Los marcamos **required (no default)**: el motor no puede fabricarlos porque `CourseMapper` y `AuditEngine` viven en otros modulos (su wiring depende de `NlpTokenizer`, `QuizSentenceConverter`, `ContentAudit`) y la raiz de filesystem del store es decision del composition root. La factory ya construye el `DefaultImpactPreviewComputer` internamente con esos cuatro puertos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionEngineConfig
        _change: modify
        type: record
        fields:
          - { name: courseMapper, type: CourseMapper, _change: add }
          - { name: auditEngine, type: AuditEngine, _change: add }
          - { name: impactPreviewStore, type: ImpactPreviewStore, _change: add }
```

## Exponer auditEngine en DefaultAuditRunner para reutilizarlo en la composicion

El composition root (`audit-cli`) ya construye un `AuditEngine` para `DefaultAuditRunner`. Para evitar duplicar wiring se reutiliza esa misma instancia al armar el `RevisionEngineConfig`. Marcamos a `DefaultAuditRunner` con su `requiresInject` ya existente (incluye `auditEngine`) — ningun cambio funcional, solo deja el contrato explicito en sentinel.yaml asi el chequeo de wiring del composition root es directo.

```architecture
modules:
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultAuditRunner
        _change: modify
        implements: [AuditRunner]
        visibility: public
        types: [Service]
        requiresInject:
          - { name: courseRepository, type: CourseRepository }
          - { name: courseToAuditableMapper, type: CourseToAuditableMapper }
          - { name: contentAudit, type: ContentAudit }
          - { name: courseMapper, type: CourseMapper }
          - { name: auditEngine, type: AuditEngine }
```

## Convertir a porcentual en la capa de presentacion

R013 exige que los tres valores de cada delta se muestren al operador en notacion porcentual con signo explicito (`72% -> 81% (+9 pp)`). Los carriers del dominio quedan en escala 0..1, asi que la conversion es responsabilidad del adapter de presentacion. Introducimos `ImpactPreviewFormatter` (puerto interno del package `formatting`) y un view-model de tres records (`ImpactPreviewView`, `LevelImpactView`, `DimensionDeltaView`) que ya guardan strings pre-formateados. El formatter es el unico lugar de la base de codigo que toca la conversion 0..1 -> %, asi que un solo test cubre todas las variantes (signo, redondeo, caso UNAVAILABLE — donde `unavailabilityText` reemplaza al numero, R013 detail 4).

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: formatting
        _change: modify
        models:
          - name: DimensionDeltaView
            _change: add
            type: record
            fields:
              - { name: dimension, type: String }
              - { name: deltaText, type: String }
          - name: LevelImpactView
            _change: add
            type: record
            fields:
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: aggregateText, type: String }
              - { name: dimensionRows, type: "List<DimensionDeltaView>" }
          - name: ImpactPreviewView
            _change: add
            type: record
            fields:
              - { name: availability, type: ImpactPreviewAvailability }
              - { name: unavailabilityText, type: String }
              - { name: levels, type: "List<LevelImpactView>" }
        interfaces:
          - name: ImpactPreviewFormatter
            _change: add
            stereotype: port
            visibility: internal
            exposes:
              - signature: "format(ImpactPreview preview): ImpactPreviewView"
        implementations:
          - name: DefaultImpactPreviewFormatter
            _change: add
            implements: [ImpactPreviewFormatter]
```

## Cargar el preview en la misma operacion que el RevisionArtifact (DOUBT-PREVIEW-CLI-EXPOSURE)

R007 exige que el preview se vea junto a la propuesta, no como recurso aparte. Resolvemos DOUBT-PREVIEW-CLI-EXPOSURE como **Opcion A**: ningun comando nuevo, ningun recurso CLI nuevo. `GetCmd` (que ya maneja el resource `proposal`/`proposals` desde FEAT-REVAPR) recibe dos dependencias adicionales: `ImpactPreviewStore` para resolver el preview asociado al `proposalId` que se esta mostrando, y el `ImpactPreviewFormatter` para convertir el `ImpactPreview` en `ImpactPreviewView` y embeberlo en el output (texto o JSON). Si el store no encuentra preview para esa propuesta — caso degradado distinto del UNAVAILABLE persistido — `GetCmd` muestra "preview no disponible" sin abortar la lectura de la propuesta (R007 detail 3).

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            implements: [GetCommand]
            externalImplements:
              - "java.util.concurrent.Callable<Integer>"
            types: [Component]
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: analyzerRegistry, type: AnalyzerRegistry }
              - { name: correctionContextResolver, type: CorrectionContextResolver }
              - { name: revisionArtifactStore, type: RevisionArtifactStore }
              - name: impactPreviewStore
                type: ImpactPreviewStore
                description: >
                  Loaded in the same operation that fetches the RevisionArtifact,
                  so the operator sees the preview alongside elementBefore /
                  elementAfter without a separate command (F-PIPRE-R007).
              - name: impactPreviewFormatter
                type: ImpactPreviewFormatter
                description: >
                  Single seam that turns the ImpactPreview (domain values 0..1)
                  into operator-facing percent strings (F-PIPRE-R013).
```
