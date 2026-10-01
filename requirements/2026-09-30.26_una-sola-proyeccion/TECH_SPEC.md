---
patch: FEAT-PROYU
requirement: 2026-09-30.26_una-sola-proyeccion
generated: 2026-10-01T03:45:00Z
---

# Tech Spec: Una sola proyección del curso con sus propuestas (FEAT-PROYU)

Paquete F, ronda 2. Se diseña contra el `sentinel.yaml` con 0.2 y 0.3 (FEAT-HALL) aplicados. Lo que motiva cada
decisión se leyó en el código de este worktree: `DefaultConsolidatedViewBuilder` aplica las pendientes a mano y corre
el motor dos veces; `DefaultImpactPreviewComputer` compara contra `loadLatest()` y, para cada contenedor, toma el
primero de su tipo; `PruneCmd` borra los más viejos sin mirar nada. Lo compartido que este parche necesita y no trae
—la medida de cada analizador en el informe, la resolución del id de una proyección por `AuditReportStore.load`, el
caché de tokens por oración y el cableado de `Main`— está en `ganchos-F.md`, uno por línea. Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. `GetConsolidatedCommand` repite `sealed: true`: el apply la abría si el parche no lo decía (bug 09 de Sentinel).

## Separar la línea base del par activo
Hasta hoy la línea base era el par activo, pero desde el paquete E el activo avanza: cada tanda abre sobre el análisis
del curso que publicó la anterior, y las metas de vocabulario se miden contra la misma base en las cuatro tandas. Un
registro aparte, con su puerto y su archivo en `.content-audit/`, es la forma más chica de que la base no se mueva con
el activo; va en la raíz de audit-domain junto a `ActiveAnalysisSelection`, que es su gemela. Sólo guarda el id y cuándo
se fijó: las metas se fijan en otro acto (FEAT-METAS), así que fijar la base no las toca.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: Baseline
        _change: add
        type: record
        fields:
          - { name: auditId, type: String }
          - { name: fixedAt, type: Instant }
    interfaces:
      - name: BaselineStore
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "read(): Optional<Baseline>"
          - signature: "write(Baseline baseline): void"
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemBaselineStore
        _change: add
        visibility: public
        types: [Repository]
        implements: [BaselineStore]
        requiresInject:
          - { name: baseDir, type: Path }
```

## Fijar la línea base con un verbo propio
`set-baseline <id>` sigue el molde de `set-active`: un puerto sellado en la raíz de audit-cli y su implementación en
`commands`. Rechaza un id que no está en el historial —una proyección guardada no lo está (R004)— y `showBaseline` la
muestra, porque ningún otro verbo la dice. E nunca lo llama: al publicar una tanda mueve el par activo con `set-active`
y la base queda.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: SetBaselineCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "setBaseline(String auditId): Integer"
          - signature: "showBaseline(String format): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultSetBaselineCommand
            _change: add
            types: [Component]
            implements: [SetBaselineCommand]
            requiresInject:
              - { name: baselineStore, type: BaselineStore }
              - { name: auditReportStore, type: AuditReportStore }
```

## Que delete y prune no borren ni la base ni el activo
`DeleteCmd` y `PruneCmd` hoy sólo ven el directorio de informes, por eso `prune audits --keep 3` después de tres
análisis nuevos se lleva la base. Reciben los dos puertos de selección: `delete audit` rechaza con el mensaje de R006, y
`prune` saca a los dos de la cuenta y lo dice («Kept the baseline …»), que es la opción A de José y cambia
F-CLIRV-R003. En la ronda 2 sólo F toca estos dos comandos; las inyecciones de siempre se listan porque al aplicar se
unen por nombre.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DeleteCmd
            _change: modify
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: baselineStore, type: BaselineStore }
          - name: PruneCmd
            _change: modify
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: baselineStore, type: BaselineStore }
```

## Dar a la proyección un solo contrato
Hay tres caminos que calculan el mismo después y no coinciden. El paquete público `projection` es el único: un puerto
que devuelve las tres fotos —el análisis activo guardado, el curso con lo aprobado y el curso con lo pendiente— y las
listas de lo que entró y lo que no, que todo número proyectado tiene que declarar (R002). `ProjectionScope` es la
respuesta a «qué es una tanda»: F no conoce el modelo de E, así que E le pasa las pendientes de su tanda y su id, y sin
tanda se proyecta todo el plan activo, como hoy. El tope por analizador viaja en el mismo alcance y vale 0 si nadie lo
pide (R003). Las causas de indisponibilidad son las de la vista consolidada, para no tener dos taxonomías de lo mismo.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: projection
        _change: add
        visibility: public
        models:
          - name: ProjectionScope
            _change: add
            type: record
            fields:
              - { name: scopeId, type: String }
              - { name: proposalIds, type: "List<String>" }
              - { name: analyzerBudgets, type: "Map<String,Integer>" }
          - name: ProjectedChange
            _change: add
            type: record
            fields:
              - { name: proposalId, type: String }
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: pending, type: boolean }
          - name: CourseProjection
            _change: add
            type: record
            fields:
              - { name: activeAuditId, type: String }
              - { name: activePlanId, type: String }
              - { name: scopeId, type: String }
              - { name: availability, type: ConsolidatedViewAvailability }
              - { name: unavailability, type: ConsolidatedViewUnavailability }
              - { name: original, type: AuditReport }
              - { name: approved, type: AuditReport }
              - { name: projected, type: AuditReport }
              - { name: changes, type: "List<ProjectedChange>" }
              - { name: nonApplicablePendings, type: "List<NonApplicablePending>" }
        interfaces:
          - name: CourseProjector
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "project(Path coursePath, ProjectionScope scope): CourseProjection"
```

## Construir el proyector desde afuera con una fábrica
La comparación y la muestra ciega de F, y la tanda de E, necesitan un proyector que no pueden armar solas: el motor vive
en `engine`, junto al localizador de nodos, que es de paquete. La fábrica pública con su registro de configuración es el
molde que ya usa `ConsolidatedViewBuilderFactory`; el registro lleva los siete puertos que el proyector necesita y
ninguno de sus colaboradores internos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: projection
        _change: add
        visibility: public
        models:
          - name: CourseProjectorConfig
            _change: add
            type: record
            fields:
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: revisionArtifactStore, type: RevisionArtifactStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: courseMapper, type: CourseMapper }
              - { name: auditEngine, type: AuditEngine }
        interfaces:
          - name: CourseProjectorFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "create(CourseProjectorConfig config): CourseProjector"
      - name: engine
        _change: modify
        implementations:
          - name: DefaultCourseProjectorFactory
            _change: add
            visibility: public
            implements: [CourseProjectorFactory]
```

## Aplicar las pendientes en un solo lugar
Hoy la vista consolidada aplica la primera y deja afuera la otra, el dashboard deja ganar a la última y el impacto no
mira `elementBefore`: 143 ejercicios del plan del 30/9 tienen dos tareas. `PendingApplier` es la regla de R002 escrita
una vez —nodo presente, `elementBefore` igual al nodo en el curso con lo aprobado, orden por fecha de creación, y la que
no entra declarada con su causa— y la usan el proyector y el impacto. Es interno de `engine` porque su única
dependencia, `DefaultCourseElementLocator`, es de paquete, y escribe como `approve` (reemplazo del nodo, F-REVAPR-R011).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        interfaces:
          - name: PendingApplier
            _change: add
            stereotype: port
            visibility: internal
            sealed: false
            exposes:
              - signature: "apply(CourseEntity course, List<RevisionProposal> proposals): AppliedProposals"
        models:
          - name: AppliedProposals
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: course, type: CourseEntity }
              - { name: applied, type: "List<RevisionProposal>" }
              - { name: nonApplicable, type: "List<NonApplicablePending>" }
        implementations:
          - name: DefaultPendingApplier
            _change: add
            implements: [PendingApplier]
            requiresInject:
              - { name: courseElementLocator, type: CourseElementLocator }
```

## Medir cada foto con los analizadores del análisis de partida
La foto con pendientes da 92,38 en A1 donde `analyze` da 96,54 por el arrastre del motor, que FEAT-HALL ya resuelve
armando analizadores nuevos en cada corrida (ver FEAT-CERO). Queda otra diferencia, que hoy no se ve porque la base del
29/9 corrió sólo los siete instantáneos: la vista corre la selección base, y una base que corrió un juez o un
analizador nuevo no se proyectaría con los mismos (R003). `ProjectionMeasurer` es el único punto que mapea un curso y
lo mide: toma los nombres de la medida que el análisis de partida guardó en su raíz (gancho F-01 a F-04), arma la
selección en orden del catálogo con tope 0 salvo pedido y llama a `runAudit(course, selection)`. Un informe sin medida —anterior al contrato— cae a la
corrida base de hoy y lo declara. Así «la misma medida» (R001, R003, R007) sale por construcción.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        interfaces:
          - name: ProjectionMeasurer
            _change: add
            stereotype: port
            visibility: internal
            sealed: false
            exposes:
              - signature: "measure(CourseEntity course, AuditReport startingAnalysis, Map<String,Integer> analyzerBudgets): AuditReport"
        implementations:
          - name: DefaultProjectionMeasurer
            _change: add
            implements: [ProjectionMeasurer]
            requiresInject:
              - { name: courseMapper, type: CourseMapper }
              - { name: auditEngine, type: AuditEngine }
```

## Armar las tres fotos en el proyector
`DefaultCourseProjector` hace lo que hoy hace el constructor de la vista consolidada, pero una vez y para todos: lee el
par activo, carga el análisis y las propuestas del plan, aplica las aprobadas y las pendientes del alcance con el
aplicador y mide las dos fotos con el medidor. Con cuatro pendientes y los tokens en caché, la vista consolidada real
tardó 4 s: lee el informe base de 146 MB, mapea dos cursos (0,85 s cada uno, medido) y corre el motor dos veces (0,05 a
0,1 s, medido). Sin caché, leer un curso con una oración cambiada son 38 s; el caché por oración (gancho F-07) lo baja a
lo que tarda spaCy con las oraciones nuevas.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultCourseProjector
            _change: add
            implements: [CourseProjector]
            requiresInject:
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: revisionArtifactStore, type: RevisionArtifactStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: pendingApplier, type: PendingApplier }
              - { name: projectionMeasurer, type: ProjectionMeasurer }
```

## Que la vista consolidada sólo compare
El constructor pierde ocho inyecciones y gana el proyector: ya no aplica ni mide, sólo recorre las tres fotos con el
comparador de campos, y ahora también la raíz, con el id que el resumen de FEAT-HALL le da al curso, que es lo que hoy
falta (el curso no aparece en ninguna foto). Su fábrica arma el proyector con los mismos campos de
`ConsolidatedViewBuilderConfig`, así que el registro y `Main` no cambian para `get-consolidated`; el pedido con tope
entra por una sobrecarga nueva del puerto y del comando.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: consolidatedview
        _change: modify
        interfaces:
          - name: ConsolidatedViewBuilder
            _change: modify
            exposes:
              - signature: "build(Path coursePath, ProjectionScope scope): ConsolidatedView"
                _change: add
      - name: engine
        _change: modify
        implementations:
          - name: DefaultConsolidatedViewBuilder
            _change: modify
            requiresInject:
              - { name: revisionArtifactStore, type: RevisionArtifactStore, _change: delete }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore, _change: delete }
              - { name: auditReportStore, type: AuditReportStore, _change: delete }
              - { name: refinementPlanStore, type: RefinementPlanStore, _change: delete }
              - { name: courseRepository, type: CourseRepository, _change: delete }
              - { name: courseElementLocator, type: CourseElementLocator, _change: delete }
              - { name: courseMapper, type: CourseMapper, _change: delete }
              - { name: auditEngine, type: AuditEngine, _change: delete }
              - { name: courseProjector, type: CourseProjector }
              - { name: nodeFieldDiffer, type: NodeFieldDiffer }
  - name: audit-cli
    _change: modify
    interfaces:
      - name: GetConsolidatedCommand
        _change: modify
        sealed: true
        exposes:
          - signature: "getConsolidated(String coursePath, String format, Map<String,Integer> analyzerBudgets): Integer"
            _change: add
```

## Calcular el impacto de una propuesta con la misma medida
El impacto se rehace sobre la proyección, como eligió José: la propuesta sola se aplica con el mismo aplicador sobre el
curso de ese momento, que ya trae lo aprobado porque `approve` lo escribe, y las dos puntas se miden con el mismo
medidor y con los analizadores del análisis del que salió la tarea (`sourceAuditId`), nunca con `loadLatest`. La
cadena de contenedores se recorre hacia arriba desde el nodo con `ancestor`, y el número agregado de cada nivel es el
puntaje de vocabulario publicado (F-HALL-R010). La fábrica del motor de revisión lo arma con los campos que
`RevisionEngineConfig` ya trae, así que el registro no cambia.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultImpactPreviewComputer
            _change: modify
            requiresInject:
              - { name: courseMapper, type: CourseMapper, _change: delete }
              - { name: auditEngine, type: AuditEngine, _change: delete }
              - { name: elementLocator, type: CourseElementLocator, _change: delete }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: pendingApplier, type: PendingApplier }
              - { name: projectionMeasurer, type: ProjectionMeasurer }
```

## Guardar la proyección fuera del historial
El plan proyectado y las palabras sugeridas necesitan un análisis fuente por id (F-PLANEF-R002, F-CSLATDC-R015), y hoy
el dashboard lo consigue escribiendo en el historial principal, donde pasa a ser «el último». `ProjectionStore` guarda
sólo la foto con pendientes en `.content-audit/projections/`, hermano de `audits/`, así que `get audits`,
`loadLatest()` y `prune` no la ven por construcción (R004); un archivo por par activo hace que guardar otra la
reemplace (R005). El puerto vive en la raíz de revision-domain, junto a `ImpactPreviewStore`, porque el adaptador de
audit-infrastructure no resuelve un `implements` contra un paquete. Que `plan --audit <id>` y la consulta de palabras
sugeridas acepten ese id es un cambio de cuerpo en `FileSystemAuditReportStore.load` (gancho F-05). `save-projection`
imprime el id con las pendientes aplicadas y las que no (R002).

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: ProjectionStore
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "save(CourseProjection projection): SavedProjection"
          - signature: "load(String projectionId): Optional<AuditReport>"
    packages:
      - name: projection
        _change: add
        visibility: public
        models:
          - name: SavedProjection
            _change: add
            type: record
            fields:
              - { name: projectionId, type: String }
              - { name: activeAuditId, type: String }
              - { name: activePlanId, type: String }
              - { name: scopeId, type: String }
              - { name: savedAt, type: Instant }
              - { name: appliedProposalIds, type: "List<String>" }
              - { name: nonApplicablePendings, type: "List<NonApplicablePending>" }
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemProjectionStore
        _change: add
        visibility: public
        types: [Repository]
        implements: [ProjectionStore]
        requiresInject:
          - { name: baseDir, type: Path }
  - name: audit-cli
    _change: modify
    interfaces:
      - name: SaveProjectionCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "saveProjection(String coursePath, List<String> proposalIds, Map<String,Integer> analyzerBudgets, String format): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultSaveProjectionCommand
            _change: add
            types: [Component]
            implements: [SaveProjectionCommand]
            requiresInject:
              - { name: courseProjector, type: CourseProjector }
              - { name: projectionStore, type: ProjectionStore }
```

## Registrar FEAT-PROYU
La feature se registra en el parche para que `feature sync --feature FEAT-PROYU` anote J001 después de aplicar; su
ubicación es del qa-tester. Las pruebas que fijan números salen del prototipo del 30/9 sobre la base del 29/9: con las
cuatro propuestas de «Afirmativas regulares», curso 73,92 y niveles 96,54 · 94,16 · 79,45 · 63,51 en la foto con
pendientes, el ejercicio de «grandmother» de 1,0 a 0,9 en `lemma-absence`, su tema de 100,0 a 99,5 y 143 ejercicios con
dos tareas para el orden de R002. Cambios de cuerpo que el parche no declara: las fábricas de la vista consolidada y del
motor de revisión arman aplicador, medidor y proyector; la raíz entra al índice de la vista; `prune` imprime lo que
conserva.

```architecture
features:
  - id: FEAT-PROYU
    code: F-PROYU
modules:
  - name: audit-cli
    _change: modify
```

## Declarar las 46 pruebas de FEAT-PROYU y ubicar su journey
Las 46 son nuevas: ningún test de hoy verifica estas reglas, porque el proyector, el aplicador, el medidor, la línea
base y la proyección guardada todavía no están en el código. Cada una fija una regla en la pieza que la cumple, y donde
la regla trae un número, la prueba lo fija con el caso del 29/9: el informe 2026-09-30T11-54-02, su plan
2026-09-30T11-54-12 y las cuatro propuestas de «Afirmativas regulares».

| Pieza | Reglas | Pruebas | Qué fija |
|---|---|---:|---|
| `DefaultCourseProjector` | R001, R002, R003, R006 | 7 | 73,92 y 96,54 · 94,16 · 79,45 · 63,51 en la foto con pendientes, no 92,38 en A1; «My grandmother ___ (come) every year.» de 1,0 a 0,9 sin mover ningún topic; sin aprobadas, la foto con lo aprobado es el análisis activo, con los 55 topics; las aplicadas y las no aplicables; el alcance de una tanda; las dos fotos con los mismos analizadores y topes; parte del par activo |
| `DefaultPendingApplier` | R001, R002 | 4 | escribe como `approve`; entra si el nodo existe y su `elementBefore` coincide; orden de creación, con los 143 ejercicios de dos tareas; nodo ausente |
| `DefaultProjectionMeasurer` | R001, R003 | 5 | mide como `analyze`; corre los analizadores de la medida del análisis de partida; sin medida, la corrida base (la del 29/9 no corre `quiz-instruction`); tope 0 a los jueces de claude -p; reusa veredictos |
| `DefaultConsolidatedViewBuilder` | R001, R002 | 2 | el curso entra con el id del resumen; las mismas no aplicables que la proyección |
| `DefaultImpactPreviewComputer` | R002, R004, R007 | 5 | no aplica lo que la proyección no aplicaría; el análisis de la tarea, nunca el último; «Afirmativas regulares» de 100,0 a 99,5 y no «Afirmativas con $be$»; los contenedores reales; las dos puntas con los analizadores de la tarea |
| `FileSystemProjectionStore` | R004, R005 | 3 | escribe en `projections/`; una por par activo; se carga por su id |
| `FileSystemAuditReportStore` | R004, R005 | 2 | `list` y `loadLatest` no la ven; `load` la resuelve por id (gancho F-05) |
| `FileSystemBaselineStore` | R006 | 2 | su propio archivo; se lee con el id y la fecha |
| `DefaultSetBaselineCommand` | R006 | 4 | 2026-09-30T11-54-02; el error de un id fuera del historial; una proyección guardada no; el par activo no se mueve |
| `DefaultSaveProjectionCommand` | R002, R004, R005 | 3 | guarda e imprime el id; las aplicadas y las no aplicables; `get audits` igual |
| `DeleteCmd` y `PruneCmd` | R004, R006 | 5 | los dos errores de `delete audit`; «Kept the baseline 2026-09-30T11-54-02» con `--keep 3`; el activo, aunque sea viejo; las proyecciones no cuentan |
| `PlanCmd` | R004, R005 | 2 | `plan --audit <id de la proyección>` efímero y con contexto; sin `--audit`, el último del historial |
| `DefaultGetConsolidatedCommand` | R003 | 1 | el tope de `--budget` llega a la proyección |
| `DefaultSuggestedLemmaQuerySessionFactory` | R005 | 1 | las palabras sugeridas de una tarea proyectada salen de la proyección |

Por regla: R001 6, R002 8, R003 6, R004 6, R005 6, R006 11 y R007 3.

**El journey.** F-PROYU-J001 va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`, con el criterio de
F-JEXAM-J001 y F-CDIFF-J001: su camino cruza `get consolidated`, `save-projection`, `plan --audit`, `get audits` y
`prune audits`, cuyos comandos son package-private ahí; el proyector se alcanza por su fábrica pública y los almacenes
son públicos. `generate` crearía `FProyuJ001JourneyTest` con 2 caminos.

**Dependencias.** Las de R003 leen la medida de cada analizador en la raíz del informe (F-RONDA2-R004); la de
`FileSystemAuditReportStore.load`, el gancho F-05; las de los números, el informe y el plan del 29/9.

**Lo que no se declara.** Que el número de cada nivel del impacto sea el puntaje de vocabulario publicado (R007) ya lo
fija la prueba de F-HALL-R010 en `DefaultImpactPreviewComputer`, y no se repite.

**Para el analista.** R003 dice «0 si no se pide» para todo analizador que consulta un modelo, y F-RONDA2-R002 dice que,
sin `--budget`, un modelo local no tiene tope y la política llega vacía al proveedor. Si el análisis activo corrió
`naturalness` o `near-duplicates` con Ollama, ¿la proyección los corre sin tope, como `analyze` (y R001 se cumple), o
con 0, como dice R003 (y los ejercicios cambiados quedan sin evaluar)? Las pruebas de R003 fijan sólo el caso de
claude -p, en el que las dos reglas coinciden.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: engine
        _change: modify
        implementations:
          - name: DefaultCourseProjector
            _change: add
            handwrittenTests:
              - name: "should give the photo with the four pending proposals of «Afirmativas regulares» the numbers of analyze on the copy with them written, 73.92% in the course and 96.54, 94.16, 79.45 and 63.51 in the levels, and not 92.38 in A1"
                traceability: { feature: FEAT-PROYU, rule: F-PROYU-R001 }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: PruneCmd
            _change: modify
            handwrittenTests:
              - name: "should keep the baseline 2026-09-30T11-54-02 after three new analyses and prune audits --keep 3, saying «Kept the baseline 2026-09-30T11-54-02»"
                traceability: { feature: FEAT-PROYU, rule: F-PROYU-R006 }
features:
  - id: FEAT-PROYU
    code: F-PROYU
    journeys:
      - { id: F-PROYU-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
