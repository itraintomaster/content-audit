---
patch: FEAT-TANDA
requirement: 2026-09-30.24_tandas-por-nivel
generated: 2026-10-01T03:00:00Z
---

# Tech Spec: Tandas por nivel (FEAT-TANDA)

Paquete E, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La tanda no calcula nada que ya calcule otro:
toma las tareas de FEAT-TAREA, corre la mesa de FEAT-MESA, pasa la compuerta de FEAT-NOEMP y compara con la
comparación del paquete F (`BatchComparator`, `ProjectionScope`, `BaselineStore`, `BlindSampleService`). Lo nuevo es el
recurso con estado, que se guarda después de cada paso, y el libro de cambios, que deja sacar uno sin romper los demás.
Lo compartido —el cableado de `Main`, los arreglos por regla de A, el caché por oración— está en `ganchos-E.md`. Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. Trae ahora los arreglos por regla del gancho E-06 y el libro de metas de F (F-10): ver la última sección.

## Arreglar por regla con un puerto de cada analizador
Las reglas de R004 —la mayúscula de las opciones, la forma que falta con «|», la pista que sale de la respuesta, la
oración guardada que se rearma, la «/» que pasa a «|»— son conocimiento de cada analizador de A, no de la tanda. Por eso
es un puerto que implementa cada analizador en su paquete (gancho E-05) y que la tanda recorre en orden del catálogo, un
cambio por analizador y ejercicio: esa es la «regla» que José mira en su muestra y puede sacar entera. Lo que una regla
no puede arreglar en el curso —la forma que arregla el corrector— vuelve declarado y pasa a la mesa o queda sin intentar
con su motivo. Vive en la raíz de audit-domain por la misma razón que `AnalyzerProvider`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: RuleFix
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "analyzerName(): String"
          - signature: "fix(RuleFixRequest request): RuleFixResult"
    packages:
      - name: rulefix
        _change: add
        visibility: public
        models:
          - name: RuleFixRequest
            _change: add
            type: record
            fields:
              - { name: quiz, type: QuizTemplateEntity }
              - { name: auditQuiz, type: AuditableQuiz }
              - { name: sentenceMode, type: SentenceMode }
              - { name: findings, type: "List<Finding>" }
          - name: RuleFixResult
            _change: add
            type: record
            fields:
              - { name: quiz, type: QuizTemplateEntity }
              - { name: fixed, type: "List<FindingIdentity>" }
              - { name: declined, type: "List<FindingIdentity>" }
              - { name: detail, type: String }
```

## Marcar en el plan las tareas de la tanda abierta
R002 pide que `revise` y `revise-instructions` no tomen una tarea de la tanda. Un estado nuevo del plan lo resuelve casi
solo: `nextTask` y la corrida de consignas ya toman sólo pendientes, así que no la ven, y `revise` la rechaza en el
comando, con el mensaje de R002 y el id de la tanda, antes de llamar al motor, que no cambia. Al publicar, cada tarea sale
de ese estado: completada si su hallazgo ya no aparece, `STALE` si ya no se sostenía, pendiente si no se decidió.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: RefinementTaskStatus
        _change: modify
        type: enum
        fields:
          - name: IN_BATCH
            _change: add
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: ReviseCmd
            _change: modify
            requiresInject:
              - name: batchStore
                type: BatchStore
                _change: add
```

## Guardar la tanda como un recurso, después de cada paso
Una tanda de A1 son horas de consultas y se puede cortar (R007): todo lo que decide tiene que estar en disco antes del
paso siguiente. El registro guarda lo que la tanda es —nivel, línea base, punto de partida, plan, analizadores,
tareas, encargos— y lo que fue pasando: cambios, desenlaces, decisiones de la mesa y de José, cierres y publicación. Un
archivo por tanda, reescrito de forma atómica, alcanza: hay a lo sumo una abierta (R002) y los archivos son chicos. El
almacén va en un paquete de audit-infrastructure porque Sentinel no resuelve un `implements` desde la raíz contra un
paquete.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: CorrectionBatch
            _change: add
            type: record
            fields:
              - { name: id, type: String }
              - { name: level, type: CefrLevel }
              - { name: status, type: BatchStatus }
              - { name: baselineAuditId, type: String }
              - { name: startAuditId, type: String }
              - { name: planId, type: String }
              - { name: analyzers, type: "List<String>" }
              - { name: settings, type: BatchSettings }
              - { name: conversionOrders, type: "List<ConversionOrder>" }
              - { name: taskIds, type: "List<String>" }
              - { name: notTakenTaskIds, type: "List<String>" }
              - { name: unevaluated, type: "List<NotComparedCount>" }
              - { name: courseTies, type: "List<Finding>" }
              - { name: changes, type: "List<BatchChange>" }
              - { name: quizProposals, type: "List<BatchProposalRef>" }
              - { name: outcomes, type: "List<BatchTaskOutcome>" }
              - { name: panelDecisions, type: "List<PanelDecision>" }
              - { name: adjustments, type: "List<BatchAdjustment>" }
              - { name: closings, type: "List<BatchClosing>" }
              - { name: blindSampleId, type: String }
              - { name: queryCap, type: Integer }
              - { name: queriesSpent, type: int }
              - { name: openedAt, type: Instant }
              - { name: publication, type: BatchPublication }
          - name: BatchStatus
            _change: add
            type: enum
            fields:
              - { name: OPEN }
              - { name: RUNNING }
              - { name: INTERRUPTED }
              - { name: CLOSED }
              - { name: PUBLISHED }
          - name: BatchSettings
            _change: add
            type: record
            fields:
              - { name: topicsAtOnce, type: int }
              - { name: queriesAtOnce, type: int }
              - { name: maxAttempts, type: int }
        interfaces:
          - name: BatchStore
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "save(CorrectionBatch batch): void"
              - signature: "load(String batchId): Optional<CorrectionBatch>"
              - signature: "open(): Optional<CorrectionBatch>"
              - signature: "list(): List<CorrectionBatch>"
              - signature: "latestPublished(): Optional<CorrectionBatch>"
  - name: audit-infrastructure
    _change: modify
    packages:
      - name: batchstore
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemBatchStore
            _change: add
            visibility: public
            implements: [BatchStore]
```

## Abrir sobre lo publicado, con la línea base de F y el par activo en su lugar
R001 fija de dónde parte una tanda y qué toma; el paquete F fija cómo se mide. Por eso abrir lee la línea base de su
registro (`BaselineStore`, FEAT-PROYU) —sin base fijada, no abre—, toma como punto de partida la base en la primera
tanda y el análisis que publicó la anterior en las demás, y pone el par activo en ese análisis y su plan: la comparación
de F toma el inicio de la tanda del par activo, así que sin este paso compararía contra otra cosa. El selector toma las
tareas de errores del nivel con resolución regla o mesa, las de `lemma-absence` que cubren un error y los encargos de
José; deja fuera, declaradas, las que tienen una propuesta pendiente ajena (R002), junta los desacuerdos de los jueces
para la bandeja (F-TAREA-R005) y cuenta lo no evaluado por analizador. N y C son 4 y 4 si no se piden (José, 30/9).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchOpenRequest
            _change: add
            type: record
            fields:
              - { name: level, type: CefrLevel }
              - { name: topicsAtOnce, type: Integer }
              - { name: queriesAtOnce, type: Integer }
              - { name: convertToMultipleChoice, type: "List<String>" }
          - name: BatchAlreadyOpenException
            _change: add
            type: exception
            fields:
              - { name: batchId, type: String }
          - name: NoOpenBatchException
            _change: add
            type: exception
            fields:
              - { name: detail, type: String }
          - name: TaskInOpenBatchException
            _change: add
            type: exception
            fields:
              - { name: taskId, type: String }
              - { name: batchId, type: String }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchTaskSelector
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "select(RefinementPlan plan, AuditReport start, CefrLevel level, List<String> conversionTopics): CorrectionBatch"
        implementations:
          - name: DefaultBatchTaskSelector
            _change: add
            implements: [BatchTaskSelector]
            requiresInject:
              - { name: artifactStore, type: RevisionArtifactStore }
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
```

## Cotizar antes de consultar, y no consultar sin tope
R003 separa cotizar de correr. La cotización cuenta tareas por analizador y resolución, ejercicios, temas, el tema más
cargado —que es la raya de lo que dura una tanda— y las consultas y horas con la N y la C elegidas, con el ritmo medido
de 19 s por consulta. Correr exige un tope explícito: sin él las reglas corren igual, porque no consultan, y las mesas
quedan «sin intentar». El tope entra al cupo compartido de la mesa y, al llegar, no empieza ninguna consulta más.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchRunRequest
            _change: add
            type: record
            fields:
              - { name: queryCap, type: Integer }
          - name: BatchEstimate
            _change: add
            type: record
            fields:
              - { name: batchId, type: String }
              - { name: tasks, type: "List<BatchTaskCount>" }
              - { name: quizzes, type: int }
              - { name: topics, type: int }
              - { name: heaviestTopicQuizzes, type: int }
              - { name: conversionQuizzes, type: int }
              - { name: estimatedQueries, type: int }
              - { name: estimatedHours, type: double }
              - { name: topicsAtOnce, type: int }
              - { name: queriesAtOnce, type: int }
          - name: BatchTaskCount
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: resolution, type: FindingResolution }
              - { name: tasks, type: int }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchCostEstimator
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "estimate(CorrectionBatch batch, List<ErrorTaskBrief> briefs): BatchEstimate"
        implementations:
          - name: DefaultBatchCostEstimator
            _change: add
            implements: [BatchCostEstimator]
```

## Primero las reglas, después las mesas
Las reglas van antes porque son baratas y porque la mesa revalida contra el ejercicio ya arreglado (R004). Cada regla
se mide con el medidor de la mesa sin cupo —los jueces sólo reusan— y pasa la compuerta por cambio; si no pasa, no se
aplica y sus tareas van a la mesa de su tema. Después, las mesas (R005): la tanda arma un trabajo por tema con sus tareas
de mesa, su semáforo y su encargo, y guarda cada desenlace apenas llega por el sumidero de la mesa.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: RuleStage
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "apply(CorrectionBatch batch, AuditReport start, CourseEntity course): CorrectionBatch"
          - name: PanelStage
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "run(CorrectionBatch batch, AuditReport start, CourseEntity course, QueryAllowance allowance): CorrectionBatch"
        implementations:
          - name: DefaultRuleStage
            _change: add
            implements: [RuleStage]
            requiresInject:
              - { name: ruleFixes, type: "List<RuleFix>" }
              - { name: topicMeter, type: TopicMeter }
              - { name: changeGate, type: ChangeGate }
              - { name: changeLedger, type: BatchChangeLedger }
              - { name: batchStore, type: BatchStore }
          - name: DefaultPanelStage
            _change: add
            implements: [PanelStage]
            requiresInject:
              - { name: panelRunner, type: PanelRunner }
              - { name: briefer, type: ErrorTaskBriefer }
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
              - { name: wordLightsQuery, type: TopicWordLightsQuery }
              - { name: changeLedger, type: BatchChangeLedger }
              - { name: batchStore, type: BatchStore }
```

## Una propuesta por ejercicio, compuesta por pasos que se pueden sacar
Un ejercicio puede recibir un cambio por regla, la miniteoría del tema y un candidato de la mesa. Si cada uno fuera una
propuesta con su `elementBefore`, la proyección de F declararía no aplicables a los posteriores apenas José sacara uno
(F-CDIFF-R012). El libro de cambios guarda cada cambio como pasos por ejercicio con las partes que tocó, y mantiene una
sola propuesta pendiente por ejercicio: antes, el ejercicio al abrir; después, la composición de sus pasos. Sacar un
cambio (R008) quita sus pasos y los posteriores del mismo ejercicio que tocaron las mismas partes, recompone con el
resto, reemplaza la propuesta y deja pendientes sus tareas: es la opción A de DOUBT-SACAR-ENCADENADO, que queda para
José. Los ajustes de José —aceptar un candidato, devolverlo, sacar una regla— entran por el mismo libro, así que la mesa
nunca escribe el curso (F-MESA-R009) y cada propuesta lleva su artefacto (F-REVBYP-R014).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchChange
            _change: add
            type: record
            fields:
              - { name: changeId, type: String }
              - { name: origin, type: BatchChangeOrigin }
              - { name: ruleId, type: String }
              - { name: knowledgeId, type: String }
              - { name: taskIds, type: "List<String>" }
              - { name: steps, type: "List<BatchChangeStep>" }
              - { name: gateVerdict, type: ChangeGateVerdict }
              - { name: joseException, type: boolean }
              - { name: createdAt, type: Instant }
              - { name: droppedReason, type: String }
          - name: BatchChangeOrigin
            _change: add
            type: enum
            fields:
              - { name: RULE }
              - { name: PANEL }
              - { name: CONVERSION }
              - { name: JOSE }
          - name: BatchChangeStep
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: parts, type: "List<ExercisePart>" }
              - { name: after, type: CourseElementSnapshot }
          - name: BatchProposalRef
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: proposalId, type: String }
          - name: BatchAdjustment
            _change: add
            type: record
            fields:
              - { name: kind, type: BatchAdjustmentKind }
              - { name: quizId, type: String }
              - { name: taskIds, type: "List<String>" }
              - { name: changeId, type: String }
              - { name: ruleId, type: String }
              - { name: candidate, type: PanelCandidate }
              - { name: finding, type: Finding }
              - { name: reason, type: String }
              - { name: decidedBy, type: String }
              - { name: decidedAt, type: Instant }
          - name: BatchAdjustmentKind
            _change: add
            type: enum
            fields:
              - { name: TASK_FROM_TIE }
              - { name: ACCEPT_CANDIDATE }
              - { name: ACCEPT_AS_EXCEPTION }
              - { name: RETURN_TO_PANEL }
              - { name: KEEP_ORIGINAL }
              - { name: DROP_CHANGE }
              - { name: DROP_RULE }
              - { name: APPROVE_RULE_SAMPLE }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchChangeLedger
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "record(CorrectionBatch batch, BatchChange change, CourseEntity startCourse): CorrectionBatch"
              - signature: "drop(CorrectionBatch batch, String changeId, String reason, CourseEntity startCourse): CorrectionBatch"
              - signature: "composedCourse(CorrectionBatch batch, CourseEntity startCourse): CourseEntity"
        implementations:
          - name: DefaultBatchChangeLedger
            _change: add
            implements: [BatchChangeLedger]
            requiresInject:
              - { name: artifactStore, type: RevisionArtifactStore }
              - { name: elementLocator, type: CourseElementLocator }
```

## Cerrar con la comparación de F, sólo sobre lo propio
«Qué es una tanda» se contesta acá: la tanda le pasa a F un `ProjectionScope` con su id y sus propuestas pendientes, una
por ejercicio, y ninguna otra, aunque el plan tenga correcciones de vocabulario pendientes de `revise` (R006 inv. 1). La
comparación trae qué vuelve, qué pisos se rompen y el veredicto; la compuerta de NOEMP decide qué devuelve, con los nodos
que cambiaron fuera del alcance, que la tanda calcula al componer su curso. Si algo vuelve, se saca por el libro, gasta
un intento y el tema vuelve a la mesa con el motivo; se recalcula hasta que pase. Cuando pasa, pide a F la muestra ciega
de ese alcance y guarda su id: la bandeja la muestra primero. Una tarea está resuelta si su hallazgo y los que cubre ya
no aparecen en la foto con lo pendiente (inv. 2). Recalcular cuesta lo que se tokeniza de nuevo: unos 38 s hoy y unos 5
con el caché por oración (gancho E-10); el 1,7 de E y los 4 s de F medían otra cosa (ver el REQUIREMENT).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchClosing
            _change: add
            type: record
            fields:
              - { name: closedAt, type: Instant }
              - { name: scope, type: ProjectionScope }
              - { name: comparison, type: BatchComparison }
              - { name: gate, type: BatchGateVerdict }
              - { name: returnedChangeIds, type: "List<String>" }
              - { name: rounds, type: int }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchCloser
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "close(CorrectionBatch batch, CourseEntity startCourse): CorrectionBatch"
        implementations:
          - name: DefaultBatchCloser
            _change: add
            implements: [BatchCloser]
            requiresInject:
              - { name: batchComparator, type: BatchComparator }
              - { name: batchGate, type: BatchGate }
              - { name: blindSampleService, type: BlindSampleService }
              - { name: changeLedger, type: BatchChangeLedger }
              - { name: batchStore, type: BatchStore }
              - { name: coursePath, type: Path }
```

## Publicar una sola vez, con preservación y sin producción
Publicar (R009) exige el veredicto «pasa» de F con las decisiones de José ya incorporadas: si la tanda cambió desde el
último cierre, vuelve a comparar antes. Aplica la composición de cada ejercicio con el reemplazo de `approve`,
verificando por ejercicio que sólo cambió lo que declaran sus pasos (`verifyWithin`), y escribe el curso una vez. Corre
el análisis del curso escrito con los mismos analizadores —los jueces reusan todo— y lo guarda: debe dar lo mismo que el
recálculo (inv. 3). Arma su plan, cierra las tareas y mueve el par activo a ese análisis, que es de donde abre la tanda
siguiente; la línea base queda en el registro de F (inv. 4). No escribe en producción: llevar el curso y regrabar son
pasos de José. Sólo la bandeja lo llama (F-BANDEJA-R008).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchPublishRequest
            _change: add
            type: record
            fields:
              - { name: approvedBy, type: String }
              - { name: note, type: String }
              - { name: excludedRuleIds, type: "List<String>" }
          - name: BatchPublication
            _change: add
            type: record
            fields:
              - { name: publishedAt, type: Instant }
              - { name: approvedBy, type: String }
              - { name: note, type: String }
              - { name: publishedAuditId, type: String }
              - { name: publishedPlanId, type: String }
              - { name: appliedProposalIds, type: "List<String>" }
              - { name: completedTaskIds, type: "List<String>" }
              - { name: reopenedTaskIds, type: "List<String>" }
          - name: BatchNotPublishableException
            _change: add
            type: exception
            fields:
              - { name: batchId, type: String }
              - { name: reasons, type: String }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchPublisher
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "publish(CorrectionBatch batch, BatchPublishRequest request, CourseEntity startCourse): CorrectionBatch"
        implementations:
          - name: DefaultBatchPublisher
            _change: add
            implements: [BatchPublisher]
            requiresInject:
              - { name: changeLedger, type: BatchChangeLedger }
              - { name: artifactStore, type: RevisionArtifactStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: courseMapper, type: CourseMapper }
              - { name: auditEngine, type: AuditEngine }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: refinerEngine, type: RefinerEngine }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: batchComparator, type: BatchComparator }
              - { name: correctionScope, type: CorrectionScope }
              - { name: preservationCheck, type: PreservationCheck }
              - { name: coursePath, type: Path }
```

## Cerrar las cuentas en un informe
R010 pide que los desenlaces sumen las tareas, y que se vean los errores antes y después por analizador y nivel, los
audios a regrabar, las consultas por puesto y las decisiones de José. Cada tarea tiene un solo desenlace a la vez y el
informe lo cuenta; los encargos se cuentan aparte porque no son tareas (F-MESA-R015). Los números vienen de la
comparación de F, no se recalculan.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchTaskOutcome
            _change: add
            type: record
            fields:
              - { name: taskId, type: String }
              - { name: kind, type: BatchTaskOutcomeKind }
              - { name: changeId, type: String }
              - { name: attempts, type: int }
              - { name: detail, type: String }
          - name: BatchTaskOutcomeKind
            _change: add
            type: enum
            fields:
              - { name: PENDING }
              - { name: RESOLVED_BY_RULE }
              - { name: RESOLVED_BY_PANEL }
              - { name: NO_LONGER_HOLDS }
              - { name: COVERED }
              - { name: NO_WINNER }
              - { name: TIE }
              - { name: RETURNED_WITHOUT_ATTEMPTS }
              - { name: NO_DECLARED_SCOPE }
              - { name: NOT_ATTEMPTED }
          - name: BatchReport
            _change: add
            type: record
            fields:
              - { name: batchId, type: String }
              - { name: level, type: CefrLevel }
              - { name: status, type: BatchStatus }
              - { name: outcomes, type: "List<OutcomeCount>" }
              - { name: totalTasks, type: int }
              - { name: conversionDone, type: int }
              - { name: conversionNoWinner, type: int }
              - { name: errorsByAnalyzer, type: "List<AnalyzerLevelErrors>" }
              - { name: rerecords, type: "List<AudioRerecordRequest>" }
              - { name: lengths, type: "List<LengthDeclaration>" }
              - { name: queries, type: QueriesByRole }
              - { name: hours, type: double }
              - { name: joseDecisions, type: "List<BatchAdjustment>" }
          - name: OutcomeCount
            _change: add
            type: record
            fields:
              - { name: kind, type: BatchTaskOutcomeKind }
              - { name: tasks, type: int }
          - name: AnalyzerLevelErrors
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: level, type: CefrLevel }
              - { name: before, type: int }
              - { name: after, type: int }
          - name: QueriesByRole
            _change: add
            type: record
            fields:
              - { name: proposer, type: int }
              - { name: judges, type: int }
      - name: batchengine
        _change: add
        visibility: internal
        interfaces:
          - name: BatchReportBuilder
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "build(CorrectionBatch batch): BatchReport"
        implementations:
          - name: DefaultBatchReportBuilder
            _change: add
            implements: [BatchReportBuilder]
```

## Un solo servicio, con su fábrica
La CLI y la bandeja ven un solo puerto con los momentos de la tanda: abrir, cotizar, correr, cerrar, ajustar, informar y
publicar. `adjust` es la única entrada de las decisiones de José y del «sacar» de la CLI, para que todas pasen por el
libro. La fábrica recibe un registro con los puertos de afuera —almacenes, motor, mapeador, plan, mesa, comparación y
muestra de F— y arma adentro la compuerta y los colaboradores.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: BatchServiceConfig
            _change: add
            type: record
            fields:
              - { name: batchStore, type: BatchStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: artifactStore, type: RevisionArtifactStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: elementLocator, type: CourseElementLocator }
              - { name: courseMapper, type: CourseMapper }
              - { name: auditEngine, type: AuditEngine }
              - { name: refinerEngine, type: RefinerEngine }
              - { name: briefer, type: ErrorTaskBriefer }
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
              - { name: ruleFixes, type: "List<RuleFix>" }
              - { name: panelRunner, type: PanelRunner }
              - { name: topicMeter, type: TopicMeter }
              - { name: panelRunnerFactory, type: PanelRunnerFactory }
              - { name: batchComparator, type: BatchComparator }
              - { name: baselineStore, type: BaselineStore }
              - { name: blindSampleService, type: BlindSampleService }
              - { name: wordLightsQuery, type: TopicWordLightsQuery }
              - { name: correctionScope, type: CorrectionScope }
              - { name: preservationCheck, type: PreservationCheck }
              - { name: coursePath, type: Path }
        interfaces:
          - name: BatchService
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "open(BatchOpenRequest request): CorrectionBatch"
              - signature: "current(): Optional<CorrectionBatch>"
              - signature: "estimate(): BatchEstimate"
              - signature: "run(BatchRunRequest request): BatchReport"
              - signature: "close(): BatchClosing"
              - signature: "adjust(BatchAdjustment adjustment): CorrectionBatch"
              - signature: "report(String batchId): BatchReport"
              - signature: "publish(BatchPublishRequest request): BatchPublication"
          - name: BatchServiceFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "create(BatchServiceConfig config): BatchService"
      - name: batchengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultBatchServiceFactory
            _change: add
            visibility: public
            implements: [BatchServiceFactory]
          - name: DefaultBatchService
            _change: add
            implements: [BatchService]
            requiresInject:
              - { name: batchStore, type: BatchStore }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore }
              - { name: baselineStore, type: BaselineStore }
              - { name: courseRepository, type: CourseRepository }
              - { name: panelRunnerFactory, type: PanelRunnerFactory }
              - { name: briefer, type: ErrorTaskBriefer }
              - { name: taskSelector, type: BatchTaskSelector }
              - { name: costEstimator, type: BatchCostEstimator }
              - { name: ruleStage, type: RuleStage }
              - { name: panelStage, type: PanelStage }
              - { name: changeLedger, type: BatchChangeLedger }
              - { name: closer, type: BatchCloser }
              - { name: publisher, type: BatchPublisher }
              - { name: reportBuilder, type: BatchReportBuilder }
              - { name: coursePath, type: Path }
```

## Dar a la tanda su verbo en la CLI
`batch open|estimate|run|close|drop|report` sigue el molde de `exam`: un puerto sellado y su implementación en
`commands`. No hay `batch publish`: publicar se hace desde la bandeja, con un solo OK (F-BANDEJA-R008).

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: BatchCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "openBatch(String level, Integer topicsAtOnce, Integer queriesAtOnce, List<String> convertToMultipleChoice): Integer"
          - signature: "estimateBatch(): Integer"
          - signature: "runBatch(Integer queryCap): Integer"
          - signature: "closeBatch(): Integer"
          - signature: "dropFromBatch(String changeId, String ruleId, String reason): Integer"
          - signature: "reportBatch(String batchId, String format): Integer"
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: BatchCmd
            _change: add
            implements: [BatchCommand]
            types: [Component]
            externalImplements:
              - "java.util.concurrent.Callable<Integer>"
            requiresInject:
              - { name: batchService, type: BatchService }
```

## Registrar FEAT-TANDA y probarla sin modelos
La feature se registra para que `feature sync --feature FEAT-TANDA` anote J001; su ubicación es del qa-tester. Las
pruebas corren con el proponente guionado, jueces con veredictos grabados, un comparador de F armado a mano y almacenes
en memoria o en un directorio temporal: abrir A1 sobre la base del 29/9 y ver sus tareas; correr sin tope y ver las
mesas sin intentar; cortar a mitad y retomar sin repetir ni pagar dos veces; sacar la regla de mayúsculas y ver que la
oración que arregló la mesa se conserva; publicar y ver el curso escrito, el análisis igual al recálculo, el par activo
movido y la base quieta. Ninguna consulta un modelo.

```architecture
features:
  - id: FEAT-TANDA
    code: F-TANDA
modules:
  - name: revision-domain
    _change: modify
```

## Lo que suma la integración de la ronda 2
Abrir una tanda fija sus metas (F-METAS-R006, gancho F-10): el servicio recibe el libro de metas de F y llama a `seal()`
al abrir, con el mismo registro de configuración. Los arreglos por regla (R004) son clases de cada analizador que
implementan `RuleFix`: los cuatro de A (gancho E-06) y el de `grammar` (F-RONDA2-R009), en sus paquetes, porque sólo el
analizador sabe arreglar lo que marca. El paquete `batch` suma `audit-infrastructure` a sus clientes, porque su almacén
de archivos implementa `BatchStore`. Y `RefinementTaskStatus` se declara como enum, y el módulo lleva la lista entera de
patrones (cinco), porque el apply reemplaza la lista en vez de sumarla.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: modify
        implementations:
          - name: HintCoherenceRuleFix
            _change: add
            visibility: public
            implements: [RuleFix]
            requiresInject:
              - { name: irregularForms, type: IrregularFormsCatalog }
      - name: answercompleteness
        _change: modify
        implementations:
          - name: AnswerCompletenessRuleFix
            _change: add
            visibility: public
            implements: [RuleFix]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
      - name: multiplechoicevalidity
        _change: modify
        implementations:
          - { name: MultipleChoiceValidityRuleFix, _change: add, visibility: public, implements: [RuleFix] }
      - name: datacoherence
        _change: modify
        implementations:
          - name: DataCoherenceRuleFix
            _change: add
            visibility: public
            implements: [RuleFix]
            requiresInject:
              - { name: quizSentenceConverter, type: QuizSentenceConverter }
      - name: grammar
        _change: modify
        implementations:
          - { name: GrammarRuleFix, _change: add, visibility: public, implements: [RuleFix] }
  - name: revision-domain
    _change: modify
    packages:
      - name: batch
        _change: add
        models:
          - name: BatchServiceConfig
            _change: add
            fields:
              - { name: goalBook, type: GoalBook }
      - name: batchengine
        _change: add
        implementations:
          - name: DefaultBatchService
            _change: add
            requiresInject:
              - { name: goalBook, type: GoalBook }
```
