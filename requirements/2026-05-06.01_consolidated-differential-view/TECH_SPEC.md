---
patch: ARCH-CDIFF-001
requirement: 2026-05-06.01_consolidated-differential-view
generated: 2026-05-06T11:00:00Z
---

# Tech Spec: FEAT-CDIFF — Estructura consolidada de un analisis con propuestas aceptadas y pendientes

## Par activo (auditId, planId) como concepto audit-level

`F-CDIFF-R001` requiere exponer un par activo `(auditId, planId)` consultable. Lo modelamos como `ActiveAnalysisSelection` (record) y el puerto `ActiveAnalysisSelectionStore` en **audit-domain**, no en revision-domain: la seleccion de "que AuditReport esta vigente" es un concepto del bounded context de auditoria, no de revision. El consolidador es solo uno de sus consumidores; futuros (CLI `get audit --active`, otros visualizadores) lo leen del mismo store. La signatura del puerto cubre las tres operaciones que el contrato pide: `read()` para el consolidador, `write()` para el verbo set-active (con idempotencia delegada al adapter, F-CDIFF-R002), `clear()` para borrar la seleccion.

```architecture
modules:
  - name: "audit-domain"
    _change: "modify"
    models:
      - name: "ActiveAnalysisSelection"
        _change: "add"
        type: "record"
    interfaces:
      - name: "ActiveAnalysisSelectionStore"
        _change: "add"
        stereotype: "port"
        exposes:
          - signature: "read(): Optional<ActiveAnalysisSelection>"
          - signature: "write(ActiveAnalysisSelection selection): void"
          - signature: "clear(): void"
```

## Adapter filesystem en audit-infrastructure

`DOUBT-ACTIVE-PERSISTENCE` lo resolvimos a Opcion A (archivo dedicado bajo `.content-audit/`). `FileSystemActiveAnalysisSelectionStore` es simetrico con `FileSystemAuditReportStore` y `FileSystemRefinementPlanStore`: un solo escritor, atomic-replace en `write()`, lectura via `Optional` que retorna empty si el archivo no existe. La idempotencia funcional (`write()` con el mismo par es no-op, F-CDIFF-R002) la implementa el adapter comparando contra el contenido actual antes de reescribir. Esto centraliza las garantias de R002 en un solo lugar y permite testearlas directamente sobre el adapter. `audit-infrastructure` ahora depende explicitamente de `revision-domain` para mantener simetria con los otros adapters del modulo (RevisionArtifactStore, ImpactPreviewStore) — este puerto no, pero la dependencia ya estaba implicita en el modulo y la hago explicita aqui.

```architecture
modules:
  - name: "audit-infrastructure"
    _change: "modify"
    dependsOn:
      - "audit-domain"
      - "refiner-domain"
      - "revision-domain"
    implementations:
      - name: "FileSystemActiveAnalysisSelectionStore"
        _change: "add"
        visibility: "public"
        implements:
          - "ActiveAnalysisSelectionStore"
```

## Package consolidatedview en revision-domain

La feature consume `RevisionArtifact` + `AuditReport` + `CourseEntity` — exactamente las dependsOn que revision-domain ya tiene. Crear un modulo `consolidation-domain` aparte solo duplicaria el grafo sin ganar encapsulacion. El package `consolidatedview` (visibility: `public`) se ubica al lado de `impactpreview` (FEAT-PIPRE) y agrupa **todos los carriers que el consumidor CLI necesita serializar** mas el puerto de construccion y su factory. Esto sigue el mismo "Public Port + Hidden Adapter, intra-modulo" que ya usamos para FEAT-PIPRE: la implementacion vive en el package interno `engine`; el package `consolidatedview` solo expone contrato y factory.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "add"
        visibility: "public"
```

## Availability tagging y taxonomia cerrada de no-disponibilidad

`F-CDIFF-R013` pide una taxonomia con cinco causas (NO_ACTIVE_ANALYSIS / ACTIVE_PLAN_UNAVAILABLE / INCONSISTENT_PROPOSAL / REAGGREGATION_FAILED / OTHER) mas detalle textual. Reusamos exactamente el patron de `ImpactPreviewUnavailability` de FEAT-PIPRE (categoria enum + detalle string), por consistencia entre features y porque el patron ya esta validado en el codebase. La separacion en dos tipos (`ConsolidatedViewAvailability` enum binario + `ConsolidatedViewUnavailability` record con `(reason, detail)`) replica el shape de FEAT-PIPRE para que un futuro lector de ambos documentos los procese con la misma maquinaria.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        models:
          - name: "ConsolidatedViewAvailability"
            _change: "add"
            type: "enum"
          - name: "ConsolidatedViewUnavailabilityReason"
            _change: "add"
            type: "enum"
          - name: "ConsolidatedViewUnavailability"
            _change: "add"
            type: "record"
```

## Pendientes no aplicables: causa categorica, sin status redundante

`F-CDIFF-R012` pide listar pendientes con `(proposalId, status: NOT_APPLICABLE, reason)`. Aterrizamos `status` implicito: una pendiente que aparece en `ConsolidatedView.nonApplicablePendings` esta, por definicion, en NOT_APPLICABLE — no necesitamos el campo. La taxonomia `NonApplicablePendingReason` cubre los dos casos del texto de R012 (`NODE_ABSENT`, `ELEMENT_BEFORE_MISMATCH`) mas escape `OTHER`. El record `NonApplicablePending` lleva proposalId + reason + detail: simetrico a `ConsolidatedViewUnavailability` pero a granularidad de propuesta individual.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        models:
          - name: "NonApplicablePendingReason"
            _change: "add"
            type: "enum"
          - name: "NonApplicablePending"
            _change: "add"
            type: "record"
```

## NodeImpact: granularidad coarse a nodo entero (DOUBT-FIELD-IDENTITY Opcion C)

El consumidor recibe el snapshot completo del nodo en `consolidated` y `pendingProjection`, no un diff campo-a-campo. Razon (acordada con analyst): hoy todas las propuestas son QUIZ-target via FEAT-LAPS; `CourseElementSnapshot` solo carga `quiz: QuizTemplateEntity` para QUIZ y vacio para los demas niveles. Distinguir "campo dentro del quiz" sin caso de uso real es ingenieria especulativa que tocaria `RevisionProposal` (alto blast radius sobre FEAT-REVBYP/REVAPR/PIPRE/LAPS). Por eso `NodeImpact` reusa `CourseElementSnapshot` (ya existente, sin tocarlo) como tipo de `consolidated` y `pendingProjection`. Los snapshots solo se pueblan para hojas (QUIZ); para padres ambos son null y el impacto se expresa via `StatisticImpact` que comparten `(nodeTarget, nodeId)`. La trazabilidad (`acceptedProposalIds`, `pendingProposalId`) cubre `F-CDIFF-R007`: lista ordenada por createdAt para que la cadena de aplicacion sea reconstruible.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        models:
          - name: "NodeImpact"
            _change: "add"
            type: "record"
            fields:
              - name: "nodeTarget"
                type: "AuditTarget"
              - name: "nodeId"
                type: "String"
              - name: "consolidated"
                type: "CourseElementSnapshot"
              - name: "pendingProjection"
                type: "CourseElementSnapshot"
              - name: "acceptedProposalIds"
                type: "List<String>"
              - name: "pendingProposalId"
                type: "String"
```

## StatisticImpact: cuatro valores numericos con la ecuacion literal de R010

`F-CDIFF-R009` pide `original / consolidated / acceptedDelta / pendingDelta` por par `(nivel, dimension)` afectado, y `F-CDIFF-R010` exige las ecuaciones literales `acceptedDelta = consolidated - original` y `pendingDelta = pendingProjection - consolidated`. El record `StatisticImpact` declara los seis campos (los cuatro de R009 mas `pendingProjection` cuando exista, mas la dimension como String abierto consistente con `AuditNode.scores`). Los nullables (`pendingProjection`, `pendingDelta`) usan `Double` boxed para distinguir "no hay pendiente" de "delta cero": una estadistica solo tocada por aceptadas tiene ambos null; una solo tocada por pendientes tiene `acceptedDelta=0.0` pero `pendingDelta!=null`. Los valores son siempre escala interna del dominio (0..1 tipicamente); la conversion porcentual de presentacion la hace el formatter (mismo patron que F-PIPRE-R013 con respecto al preview por-propuesta).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        models:
          - name: "StatisticImpact"
            _change: "add"
            type: "record"
            fields:
              - name: "nodeTarget"
                type: "AuditTarget"
              - name: "nodeId"
                type: "String"
              - name: "dimension"
                type: "String"
              - name: "original"
                type: "double"
              - name: "consolidated"
                type: "double"
              - name: "acceptedDelta"
                type: "double"
              - name: "pendingProjection"
                type: "Double"
              - name: "pendingDelta"
                type: "Double"
```

## ConsolidatedView: el documento raiz que el CLI emite

`ConsolidatedView` agrega el par activo resuelto, la availability, las dos listas de impactos (nodos + estadisticas) y la lista de pendientes no aplicables. El campo `nonApplicablePendings` se entrega aunque `availability=AVAILABLE`: una pendiente conflictiva no degrada la salida (F-CDIFF-R012 detail 1). `computedAt` permite distinguir lecturas sucesivas sin que el documento tenga semantica de "version persistida" (F-CDIFF-R015). Los campos `activeAuditId` / `activePlanId` quedan null cuando UNAVAILABLE para que el consumidor sepa que no hay par resuelto.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        models:
          - name: "ConsolidatedView"
            _change: "add"
            type: "record"
            fields:
              - name: "activeAuditId"
                type: "String"
              - name: "activePlanId"
                type: "String"
              - name: "availability"
                type: "ConsolidatedViewAvailability"
              - name: "unavailability"
                type: "ConsolidatedViewUnavailability"
              - name: "computedAt"
                type: "Instant"
              - name: "nodeImpacts"
                type: "List<NodeImpact>"
              - name: "statisticImpacts"
                type: "List<StatisticImpact>"
              - name: "nonApplicablePendings"
                type: "List<NonApplicablePending>"
```

## Builder on-demand y factory seam

El puerto `ConsolidatedViewBuilder` tiene una signatura narrow (`build(coursePath): ConsolidatedView`) que esconde toda la complejidad del algoritmo y deja libre la decision de cache para una iteracion futura. `DOUBT-MATERIALIZATION` se resuelve a Opcion A (on-demand sin cache) para el MVP: dos invocaciones de `AuditEngine.runAudit` por request (consolidated + pendingProjection) con ~11.500 quizzes puede ser caro pero todavia no lo medimos; meter cache especulativo seria over-engineering. El factory (`ConsolidatedViewBuilderFactory`) es publico para la composition root; el config `ConsolidatedViewBuilderConfig` vive en el module-root (no en el package consolidatedview) siguiendo el patron de `RevisionEngineConfig`, y todas sus dependencias son required — la composition root es la unica que sabe instanciar correctamente cada puerto (dependencias cross-module).

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    models:
      - name: "ConsolidatedViewBuilderConfig"
        _change: "add"
        type: "record"
    packages:
      - name: "consolidatedview"
        _change: "modify"
        interfaces:
          - name: "ConsolidatedViewBuilder"
            _change: "add"
            stereotype: "port"
            exposes:
              - signature: "build(Path coursePath): ConsolidatedView"
          - name: "ConsolidatedViewBuilderFactory"
            _change: "add"
            stereotype: "factory"
            exposes:
              - signature: "create(ConsolidatedViewBuilderConfig config): ConsolidatedViewBuilder"
```

## DefaultConsolidatedViewBuilder: algoritmo escondido en el package engine

La implementacion package-private vive en `revision-domain.engine` junto con `DefaultImpactPreviewComputer` y reusa la misma maquinaria (CourseElementLocator + CourseMapper + AuditEngine). El builder NUNCA llama `AuditReportStore.save` (F-CDIFF-R015) — el inject de `AuditReportStore` es solo para `load`. Las pendientes que choquen contra el consolidado se materializan como `NonApplicablePending` con la causa correspondiente (`NODE_ABSENT` / `ELEMENT_BEFORE_MISMATCH`), siguiendo F-CDIFF-R012 / DOUBT-PENDING-CONFLICT Opcion A (orden por createdAt; el segundo en chocar cae en R012). Las fallas semanticas se materializan como `availability=UNAVAILABLE` con la causa correspondiente; solo errores de programacion lanzan.

```architecture
modules:
  - name: "revision-domain"
    _change: "modify"
    packages:
      - name: "engine"
        _change: "modify"
        implementations:
          - name: "DefaultConsolidatedViewBuilderFactory"
            _change: "add"
            visibility: "public"
            implements:
              - "ConsolidatedViewBuilderFactory"
          - name: "DefaultConsolidatedViewBuilder"
            _change: "add"
            requiresInject:
              - name: "activeAnalysisSelectionStore"
                type: "ActiveAnalysisSelectionStore"
              - name: "auditReportStore"
                type: "AuditReportStore"
              - name: "refinementPlanStore"
                type: "RefinementPlanStore"
              - name: "revisionArtifactStore"
                type: "RevisionArtifactStore"
              - name: "courseRepository"
                type: "CourseRepository"
              - name: "courseElementLocator"
                type: "CourseElementLocator"
              - name: "courseMapper"
                type: "CourseMapper"
              - name: "auditEngine"
                type: "AuditEngine"
            implements:
              - "ConsolidatedViewBuilder"
```

## CLI verbs: get consolidated y set-active

Introducimos dos verbos CLI dedicados, sealed siguiendo el patron de los demas (`AnalyzeCommand`, `GetCommand`, etc.). `GetConsolidatedCommand` no se mete dentro de `GetCommand` porque su shape de respuesta (un ConsolidatedView complejo) es estructuralmente distinto del retorno simple de los recursos existentes; un solo verbo dedicado evita acoplar dos contratos en una signatura. `SetActiveAnalysisCommand` cubre R002: si ambos parametros son null, equivale a clear; idempotencia delegada al adapter. La sintaxis exacta de la CLI (flags, formatos) queda como decision de implementacion, alineada con el espiritu de la assumption 7 del REQUIREMENT.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    interfaces:
      - name: "GetConsolidatedCommand"
        _change: "add"
        stereotype: "port"
        sealed: true
        exposes:
          - signature: "getConsolidated(String coursePath, String format): Integer"
      - name: "SetActiveAnalysisCommand"
        _change: "add"
        stereotype: "port"
        sealed: true
        exposes:
          - signature: "setActive(String auditId, String planId): Integer"
```

## Implementaciones CLI y formatter publico intra-modulo

`DefaultGetConsolidatedCommand` y `DefaultSetActiveAnalysisCommand` viven en `audit-cli.commands` (package internal a nivel modulo). El formatter sigue el patron drift-corregido de FEAT-PIPRE: `ConsolidatedViewFormatter` es **public a nivel Java** (no a nivel package: el package formatting sigue siendo internal a nivel modulo) porque `DefaultGetConsolidatedCommand` lo importa desde el package hermano `commands`, y la composition root (Main) lo instancia con `new`. La implementacion `DefaultConsolidatedViewFormatter` tambien es publica a nivel Java por la misma razon. Es el patron que ya validamos en ARCH-PIPRE-VIS para `ImpactPreviewFormatter` / `DefaultImpactPreviewFormatter`.

```architecture
modules:
  - name: "audit-cli"
    _change: "modify"
    packages:
      - name: "commands"
        _change: "modify"
        implementations:
          - name: "DefaultGetConsolidatedCommand"
            _change: "add"
            implements:
              - "GetConsolidatedCommand"
          - name: "DefaultSetActiveAnalysisCommand"
            _change: "add"
            implements:
              - "SetActiveAnalysisCommand"
      - name: "formatting"
        _change: "modify"
        interfaces:
          - name: "ConsolidatedViewFormatter"
            _change: "add"
            stereotype: "service"
            visibility: "public"
            exposes:
              - signature: "format(ConsolidatedView view, String format): String"
        implementations:
          - name: "DefaultConsolidatedViewFormatter"
            _change: "add"
            visibility: "public"
            implements:
              - "ConsolidatedViewFormatter"
```
