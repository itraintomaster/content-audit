---
patch: FEAT-METAS
requirement: 2026-09-30.27_antes-y-despues-con-metas
generated: 2026-10-01T03:45:00Z
---

# Tech Spec: Antes y después de una tanda contra la línea base, con metas (FEAT-METAS)

Paquete F, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La comparación es una salida nueva que lee la
proyección de FEAT-PROYU (tipos `CourseProjector`, `ProjectionScope`, `CourseProjection`, `BaselineStore`) y los
números que FEAT-HALL publica en cada nodo; no cambia el contrato de la vista consolidada (F-CDIFF-R010). Todo vive en
revision-domain, donde ya están la vista consolidada y la vista previa: un paquete público con el contrato y otro con el
motor, abierto sólo a la raíz de composición. Lo compartido —la medida de cada analizador en el informe, el cableado y
lo que E tiene que llamar— está en `ganchos-F.md`. Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. La comparación recibe lo que el tema enseña, de D (gancho F-14): ver la última sección.

## Declarar las metas en un solo lugar
R005 pide una sola declaración que se pueda listar y que cubra a un analizador nuevo sin tocarla. La declaración trae
tres cosas: los pisos por familia (errores y vocabulario, con su referencia), las metas nombradas —las de avance que
José fija antes de cada tanda y cualquier piso propio— y la meta de la muestra ciega, que vive acá para que FEAT-CIEGA
la lea sin que F dependa de ella al revés. La referencia es un eje explícito porque José eligió contra qué se mide
cada piso: vocabulario contra la línea base, acumulado; errores y nodos cambiados contra el inicio de la tanda. El
archivo lo edita José y va versionado en el repo; el formato es del developer.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: GoalDeclarationSource
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "read(): GoalDeclaration"
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: GoalKind
            _change: add
            type: enum
            fields:
              - { name: FLOOR }
              - { name: PROGRESS }
          - name: GoalUnit
            _change: add
            type: enum
            fields:
              - { name: POINTS }
              - { name: EXERCISES }
              - { name: PERCENT }
          - name: GoalReference
            _change: add
            type: enum
            fields:
              - { name: BASELINE }
              - { name: BATCH_START }
              - { name: ABSOLUTE }
          - name: GoalOrigin
            _change: add
            type: enum
            fields:
              - { name: DECLARED }
              - { name: FAMILY_DEFAULT }
          - name: SampleCriterion
            _change: add
            type: enum
            fields:
              - { name: OBSERVED }
              - { name: BOUND }
          - name: Goal
            _change: add
            type: record
            fields:
              - { name: id, type: String }
              - { name: kind, type: GoalKind }
              - { name: subject, type: String }
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: threshold, type: double }
              - { name: unit, type: GoalUnit }
              - { name: reference, type: GoalReference }
              - { name: origin, type: GoalOrigin }
          - name: FamilyFloor
            _change: add
            type: record
            fields:
              - { name: family, type: AnalyzerFamily }
              - { name: threshold, type: double }
              - { name: unit, type: GoalUnit }
              - { name: reference, type: GoalReference }
              - { name: nodeTargets, type: "List<AuditTarget>" }
          - name: BlindSampleGoal
            _change: add
            type: record
            fields:
              - { name: size, type: int }
              - { name: minBetterOrEqual, type: int }
              - { name: maxWorse, type: int }
              - { name: criterion, type: SampleCriterion }
              - { name: responsible, type: String }
              - { name: secondReviewerPairs, type: int }
              - { name: cumulativeMaxWorseShare, type: double }
          - name: GoalDeclaration
            _change: add
            type: record
            fields:
              - { name: familyFloors, type: "List<FamilyFloor>" }
              - { name: goals, type: "List<Goal>" }
              - { name: blindSample, type: BlindSampleGoal }
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemGoalDeclarationSource
        _change: add
        visibility: public
        types: [Repository]
        implements: [GoalDeclarationSource]
        requiresInject:
          - { name: declarationFile, type: Path }
```

## Poner a cada analizador el piso de su familia
Las metas efectivas son las declaradas más, para cada analizador del catálogo que la declaración no nombra, el piso de
su familia, marcado como tal: así un analizador nuevo de A, C o D queda cubierto el día que entra al catálogo (R005). El
libro de metas es el único que hace esa cuenta, y la usan el comparador, `get-goals` y la muestra ciega; si falta el
piso de una familia en el archivo, pone el que José aceptó el 30/9 (errores 0 contra el inicio; vocabulario 0,5 puntos
contra la base, en niveles y curso). La huella de las efectivas es lo que se fija.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: EffectiveGoals
            _change: add
            type: record
            fields:
              - { name: fingerprint, type: String }
              - { name: goals, type: "List<Goal>" }
              - { name: blindSample, type: BlindSampleGoal }
        interfaces:
          - name: GoalBook
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "effective(): EffectiveGoals"
              - signature: "seal(): SealedGoals"
              - signature: "sealed(): Optional<SealedGoals>"
      - name: comparisonengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultGoalBook
            _change: add
            visibility: public
            implements: [GoalBook]
            requiresInject:
              - { name: declarationSource, type: GoalDeclarationSource }
              - { name: sealedGoalsStore, type: SealedGoalsStore }
              - { name: analyzerCatalog, type: AnalyzerCatalog }
```

## Fijar las metas antes de correr
José fija las metas antes de cada tanda: fijar es un acto propio (`set-goals`), no un efecto de cambiar el par activo,
porque desde el paquete E el activo cambia al publicar y eso registraría metas que nadie revisó. Se guardan las metas
efectivas enteras y no sólo la huella, porque R006 pide mostrar el valor de antes y el de ahora; la comparación dice qué
cambió o que nunca se fijaron. Abrir una tanda también fija: es un llamado de E a `seal()` (gancho F-10). Fijar la línea
base no toca las metas.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: SealedGoalsStore
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "read(): Optional<SealedGoals>"
          - signature: "write(SealedGoals sealedGoals): void"
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: SealedGoals
            _change: add
            type: record
            fields:
              - { name: sealedAt, type: Instant }
              - { name: goals, type: EffectiveGoals }
          - name: GoalChange
            _change: add
            type: record
            fields:
              - { name: goalId, type: String }
              - { name: before, type: String }
              - { name: after, type: String }
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemSealedGoalsStore
        _change: add
        visibility: public
        types: [Repository]
        implements: [SealedGoalsStore]
        requiresInject:
          - { name: baseDir, type: Path }
  - name: audit-cli
    _change: modify
    interfaces:
      - name: GoalsCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "listGoals(String format): Integer"
          - signature: "setGoals(String format): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultGoalsCommand
            _change: add
            types: [Component]
            implements: [GoalsCommand]
            requiresInject:
              - { name: goalBook, type: GoalBook }
```

## Publicar una fila por número, con sus tres fotos y su estado
La UI no resta ni decide con cortes propios (regla 1.1 de G), así que la fila trae todo hecho: el número de la línea
base, el del inicio de la tanda y el de la proyección, copiados de los números publicados (F-HALL-R008 a R010), el
cambio en su unidad y el estado de su piso y de su meta de avance. Trae el inicio de la tanda porque contra él se decide
el piso de errores, y la línea base re-medida cuando la medida cambió (R007). «No se puede decidir» sale de los
ejercicios sin evaluar: si contándolos todos como error el piso igual se cumple, se cumple; si no, no se puede decidir.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: GoalStatus
            _change: add
            type: enum
            fields:
              - { name: MET }
              - { name: NOT_MET }
              - { name: UNDECIDABLE }
          - name: RowValue
            _change: add
            type: record
            fields:
              - { name: value, type: Double }
              - { name: share, type: Double }
              - { name: notEvaluated, type: int }
          - name: RowGoalStatus
            _change: add
            type: record
            fields:
              - { name: goalId, type: String }
              - { name: threshold, type: double }
              - { name: unit, type: GoalUnit }
              - { name: reference, type: GoalReference }
              - { name: status, type: GoalStatus }
          - name: ComparisonRow
            _change: add
            type: record
            fields:
              - { name: subject, type: String }
              - { name: family, type: AnalyzerFamily }
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: label, type: String }
              - { name: baseline, type: RowValue }
              - { name: remeasuredBaseline, type: RowValue }
              - { name: approved, type: RowValue }
              - { name: projected, type: RowValue }
              - { name: change, type: Double }
              - { name: unit, type: GoalUnit }
              - { name: floor, type: RowGoalStatus }
              - { name: progress, type: RowGoalStatus }
```

## Decir qué cambios vuelven y por qué
«Grandmother» no mueve ningún número de nivel: el piso que la devuelve es por nodo cambiado. Por cada cambio aplicado
de la tanda, el comparador cruza por identidad (F-HALL-R014) los hallazgos del nodo al inicio de la tanda y en la
proyección, más los de otros nodos que lo nombran en su evidencia, y marca que vuelve si ganó uno que cuenta: de
errores que no sólo ordena, o de vocabulario de gravedad media o más, que es la palabra de otro nivel o fuera del
catálogo, salvo la que el tema enseña. El largo es de gravedad baja y nunca devuelve (José, 11/8), que es lo mismo que la
compuerta de E cuenta como invariante 1 (F-NOEMP-R005). Lo que el tema enseña lo decide el `TopicTeachingResolver` de
D, el mismo del semáforo y de `off-level-words`, para que la mesa, la compuerta y la comparación no difieran (gancho
F-14). Lo arreglado y lo que un analizador no evaluó se publican aparte.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: ChangeOutcome
            _change: add
            type: record
            fields:
              - { name: proposalId, type: String }
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: returns, type: boolean }
              - { name: newFindings, type: "List<Finding>" }
              - { name: fixedFindings, type: "List<Finding>" }
              - { name: undecidedAnalyzers, type: "List<String>" }
```

## Nombrar a los cambios que rompen un piso
Un número de nivel no es de ningún ejercicio, así que cuando un piso de nivel o de curso no se cumple la comparación
quita cada cambio de a uno y mide lo que pasa (R004). El atribuidor es un puerto aparte porque es el único flujo caro
y tiene su propia prueba. No vuelve a leer el curso: arma cada variante con los ejercicios ya mapeados de las dos fotos
—el del cambio vuelve a su versión del inicio—, así que cada una cuesta una corrida del motor, 0,05 a 0,1 s: 800
cambios, un minuto y medio, y se corre una vez por cambio para todos los pisos rotos juntos. La compuerta de E lee el
orden para devolver del primero en adelante (F-NOEMP-R008).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: ChangeAttribution
            _change: add
            type: record
            fields:
              - { name: proposalId, type: String }
              - { name: nodeId, type: String }
              - { name: effect, type: double }
              - { name: unit, type: GoalUnit }
          - name: FloorBreach
            _change: add
            type: record
            fields:
              - { name: goalId, type: String }
              - { name: row, type: ComparisonRow }
              - { name: attributions, type: "List<ChangeAttribution>" }
      - name: comparisonengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        interfaces:
          - name: ChangeAttributor
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "attribute(CourseProjection projection, List<FloorBreach> breaches): List<FloorBreach>"
        implementations:
          - name: LeaveOneOutChangeAttributor
            _change: add
            visibility: public
            implements: [ChangeAttributor]
            requiresInject:
              - { name: auditEngine, type: AuditEngine }
```

## Mostrar aparte un cambio de medida
Con las bandas 8 · 6 · 4 · 3 que José confirmó, el curso pasa de 73,92 a 73,86 y A1 de 96,54 a 96,02 sin tocar una
palabra: restado contra la base, rompería el piso en A1, A2 y B1. Cada informe trae en su raíz la huella y la
configuración de la medida de cada analizador que corrió (gancho F-01 a F-04); el comparador compara esa huella con la
de hoy y, si difiere, vuelve a correr los analizadores de la base sobre las entidades de su propio informe —el mismo
curso, los mismos tokens, una corrida— y cuenta la tanda desde ese número. Vale mientras el modelo del ejercicio no
cambie; si cambia, José fija una línea base nueva.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: MeasureChange
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: baselineFingerprint, type: String }
              - { name: currentFingerprint, type: String }
              - { name: baselineConfig, type: "Map<String,Object>" }
              - { name: currentConfig, type: "Map<String,Object>" }
```

## Dar un veredicto que incluye la muestra ciega
El veredicto necesita el resultado de la muestra ciega, y la muestra necesita lo que vuelve. Para que ningún parche
implemente un puerto de otro, el resultado y el puerto que lo lee los declara e implementa FEAT-CIEGA, que es quien lo
publica; acá el veredicto sólo lo lleva. El orden es el de R008: «no pasa» si algo falla, «no se puede decidir» si
ningún piso falla pero alguno queda abierto, «falta la muestra ciega» si sólo falta ella, y «pasa». Las metas de avance
nunca entran.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: VerdictKind
            _change: add
            type: enum
            fields:
              - { name: PASSES }
              - { name: FAILS }
              - { name: BLIND_SAMPLE_MISSING }
              - { name: UNDECIDABLE }
          - name: BatchVerdict
            _change: add
            type: record
            fields:
              - { name: kind, type: VerdictKind }
              - { name: brokenFloorGoalIds, type: "List<String>" }
              - { name: returningProposalIds, type: "List<String>" }
              - { name: sample, type: SampleOutcome }
              - { name: detail, type: String }
```

## Comparar una tanda contra la línea base
El comparador junta todo sin recalcular: pide al proyector las fotos de la tanda —el alcance que le pasa E, o todo el
plan activo si no hay tanda—, carga la línea base fija (FEAT-PROYU), arma filas, cambios, pisos y veredicto, y no
escribe nada (R009). La línea base queda fija mientras el activo avanza porque la lee de su propio registro, no del par
activo; el inicio de la tanda es la foto con lo aprobado del análisis activo. Así E no compara por su cuenta: su
compuerta lee `changes` y `breaches`, y su publicación exige `verdict` en «pasa». Lo caro se paga sólo cuando hace
falta: la base entera se carga si cambió una medida, y la atribución corre si se rompió un piso.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparison
        _change: add
        visibility: public
        models:
          - name: BatchComparison
            _change: add
            type: record
            fields:
              - { name: baselineAuditId, type: String }
              - { name: activeAuditId, type: String }
              - { name: activePlanId, type: String }
              - { name: scopeId, type: String }
              - { name: availability, type: ConsolidatedViewAvailability }
              - { name: unavailability, type: ConsolidatedViewUnavailability }
              - { name: computedAt, type: Instant }
              - { name: goals, type: EffectiveGoals }
              - { name: goalsSealedAt, type: Instant }
              - { name: goalChanges, type: "List<GoalChange>" }
              - { name: measureChanges, type: "List<MeasureChange>" }
              - { name: appliedProposalIds, type: "List<String>" }
              - { name: nonApplicablePendings, type: "List<NonApplicablePending>" }
              - { name: rows, type: "List<ComparisonRow>" }
              - { name: changes, type: "List<ChangeOutcome>" }
              - { name: breaches, type: "List<FloorBreach>" }
              - { name: verdict, type: BatchVerdict }
        interfaces:
          - name: BatchComparator
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "compare(Path coursePath, ProjectionScope scope): BatchComparison"
      - name: comparisonengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultBatchComparator
            _change: add
            visibility: public
            implements: [BatchComparator]
            requiresInject:
              - { name: courseProjector, type: CourseProjector }
              - { name: baselineStore, type: BaselineStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: goalBook, type: GoalBook }
              - { name: analyzerCatalog, type: AnalyzerCatalog }
              - { name: auditEngine, type: AuditEngine }
              - { name: changeAttributor, type: ChangeAttributor }
              - { name: sampleOutcomeSource, type: SampleOutcomeSource }
```

## Pedir la comparación desde la CLI
`compare` es el verbo que lee el dashboard para la vista «Antes y después»: acepta las pendientes de una tanda y el
tope por analizador, y el formateador escribe el cambio en puntos con su signo y la palabra —«−0,5 puntos», nunca
«%»—, en JSON para la UI y en texto para la terminal. Sigue el molde de `get-consolidated`: puerto sellado en la raíz,
implementación en `commands` y formateador en `formatting`.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: CompareCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "compare(String coursePath, List<String> proposalIds, Map<String,Integer> analyzerBudgets, String format): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultCompareCommand
            _change: add
            types: [Component]
            implements: [CompareCommand]
            requiresInject:
              - { name: batchComparator, type: BatchComparator }
              - { name: formatter, type: BatchComparisonFormatter }
      - name: formatting
        _change: modify
        interfaces:
          - name: BatchComparisonFormatter
            _change: add
            stereotype: service
            sealed: false
            visibility: public
            exposes:
              - signature: "format(BatchComparison comparison, String format): String"
        implementations:
          - name: DefaultBatchComparisonFormatter
            _change: add
            types: [Component]
            visibility: public
            implements: [BatchComparisonFormatter]
```

## Registrar FEAT-METAS
La feature se registra para que `feature sync --feature FEAT-METAS` anote J001; su ubicación es del qa-tester. Los casos
de prueba salen del prototipo del 30/9 y de los números de D: con las cuatro propuestas de las verduras vuelve sólo
«grandmother», con el motivo de `lemma-absence` y su ejercicio de 1,0 a 0,9; ningún número de nivel se mueve; con las
bandas nuevas el cambio de medida es 73,92 → 73,86 en el curso y 96,54 → 96,02 en A1. Cambios de cuerpo que el parche
no declara: los picocli de `compare`, `get-goals` y `set-goals`, y el cableado en `Main` (gancho F-06).

```architecture
features:
  - id: FEAT-METAS
    code: F-METAS
modules:
  - name: audit-cli
    _change: modify
```

## Lo que suma la integración de la ronda 2
Una palabra que el tema enseña no hace volver un cambio (R003, alineada con F-SEMAF-R004), y la mesa de E ya la
excluye con el `TopicTeachingResolver` de D (FEAT-NIVFR). Si la comparación no recibe el mismo, devolvería un cambio que
la mesa aceptó: por eso el comparador lo inyecta (gancho F-14). El paquete `offlevelwords` suma `revision-domain` a sus
clientes en el paso de los `allowedClients` de FEAT-RONDA2, el último de la ronda.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparisonengine
        _change: add
        implementations:
          - name: DefaultBatchComparator
            _change: add
            requiresInject:
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
```

## Declarar las 53 pruebas de FEAT-METAS y ubicar su journey
Las 53 son nuevas: la comparación, las metas y el veredicto no existen en el código. Casi todas caen en
`DefaultBatchComparator`, que es el que arma filas, cambios que vuelven, pisos y veredicto; las metas, en el libro y su
declaración; la atribución, en su propio puerto. Los casos salen del REQUIREMENT y del curso del 29/9.

| Pieza | Reglas | Pruebas | Qué fija |
|---|---|---:|---|
| `DefaultBatchComparator` | R001–R009 | 36 | las filas y sus tres fotos copiadas; `lemma-absence` de «Afirmativas regulares» 100,0 → 99,5 (−0,5 puntos) y de Present Simple 99,55 → 99,52; A1 y el curso en 96,54 y 73,92 sin cambio; filas de errores vacías con la base del 29/9; «no se puede decidir» con 30 de A1 sin juzgar; vuelve sólo «My grandmother ___ (come) every year.», con su motivo; el largo de «He isn't in the living-room.» nunca; invest en «$Invest time$ + gerundio» no; lo arreglado y lo sin decidir; vocabulario contra la base, acumulado, y errores contra el inicio de la tanda; «piso de vocabulario 0,5 → 1,0»; el cambio de medida 73,92 → 73,86, 96,54 → 96,02 y 67,0 → 66,5, y el de `naturalness` con claude -p; los cuatro veredictos; comparar no escribe |
| `DefaultGoalBook` | R005, R006 | 5 | el piso de la familia para el que la declaración no nombra, como `hint-coherence`; los pisos del 30/9 si falta una familia; la meta de la muestra; fija las metas enteras con su huella |
| `LeaveOneOutChangeAttributor` | R004 | 2 | el único cambio que baja A1 un punto va primero con −1; el orden por efecto propio |
| `FileSystemGoalDeclarationSource`, `FileSystemSealedGoalsStore` | R005, R006 | 2 | una sola declaración; `metas-fijadas.json` |
| `DefaultGoalsCommand` | R005, R006 | 2 | `get-goals` con su origen; `set-goals` |
| `DefaultCompareCommand` | R009 | 1 | el curso byte a byte y `get audits` igual |
| `DefaultSetBaselineCommand`, `DefaultSetActiveAnalysisCommand` | R006 | 2 | fijar la línea base o cambiar el par activo no toca las metas fijadas |
| `DefaultBatchComparisonFormatter` | R001, R006, R007 | 3 | «−0,5 puntos», nunca «%»; las metas cambiadas; «cambio de medida: 73,92 → 73,86» |

Por regla: R001 7, R002 4, R003 8, R004 3, R005 7, R006 9, R007 6, R008 7 y R009 2.

**El journey.** F-METAS-J001 va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`: su camino empieza con
`set-goals` y sigue con `compare`, cuyos comandos son package-private ahí; el comparador y el libro de metas están en
`comparisonengine`, público para `audit-cli`. `generate` crearía `FMetasJ001JourneyTest` con 6 caminos.

**Dependencias.** La medida en la raíz del informe (F-RONDA2-R004), lo que el tema enseña de D (F-14), la proyección de
FEAT-PROYU y el resultado de la muestra de FEAT-CIEGA, que el veredicto lee por `SampleOutcomeSource`.

**Para el analista.** (1) R006 dice que abrir una tanda también fija las metas, pero ninguna prueba puede trazarlo: la
pieza que abre la tanda (`DefaultBatchService`, de FEAT-TANDA) no existe cuando se aplica este parche, y ninguna regla
de FEAT-TANDA lo dice. Hace falta sumarlo a F-TANDA-R001 o declarar esa prueba en un parche de METAS posterior a TANDA.
(2) R001 publica filas «para cada analizador que corrió en la línea base», pero su detalle dice que con la base del 29/9
las filas de errores salen vacías, y R007 deja en «no se puede decidir» las filas de una medida nueva: ¿un analizador
que no corrió en la base tiene filas vacías o no tiene filas? La prueba sigue el detalle: vacías, no en cero.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: comparisonengine
        _change: add
        implementations:
          - name: DefaultBatchComparator
            _change: add
            handwrittenTests:
              - name: "should return «My grandmother ___ (come) every year.» with the reason «lemma-absence · palabra de otro nivel: grandmother es A2», and none of the other three proposals of «Afirmativas regulares»"
                traceability: { feature: FEAT-METAS, rule: F-METAS-R003 }
features:
  - id: FEAT-METAS
    code: F-METAS
    journeys:
      - { id: F-METAS-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
