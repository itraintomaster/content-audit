---
patch: FEAT-CIEGA
requirement: 2026-09-30.28_muestra-ciega
generated: 2026-09-30T23:00:00Z
---

# Tech Spec: Muestra ciega de cada tanda (FEAT-CIEGA)

Paquete F, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La muestra lee la proyección de FEAT-PROYU y la
comparación de FEAT-METAS —sortea entre lo que no vuelve y lee su meta de la declaración de metas— y usa el
renderizador «como lo ve el alumno» que diseñó A y construye el integrador de la ronda 1 (`QuizStudentViewRenderer`,
gancho A-01, pedido 2 de G): F no arma otro. Todo lo suyo vive en revision-domain, en un paquete público de contrato y
otro de motor abierto sólo a la raíz de composición. Lo que la bandeja de E tiene que hacer con ella está en
`ganchos-F.md`.

## Guardar sólo muestras y respuestas
Responder no toca el curso, las propuestas ni el historial (R003): la muestra y sus respuestas son lo único que escribe,
en `.content-audit/blind-samples/`. El puerto va en la raíz de revision-domain, junto a `ImpactPreviewStore`, porque el
adaptador de audit-infrastructure no resuelve un `implements` contra un paquete; guarda las dos mediciones, que tienen
la misma vida: se sortean, se responden y se cierran.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: BlindSampleStore
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "savePairs(BlindSample sample): void"
          - signature: "loadPairs(String sampleId): Optional<BlindSample>"
          - signature: "listPairs(): List<BlindSample>"
          - signature: "saveErrorRate(ErrorRateSample sample): void"
          - signature: "loadErrorRate(String sampleId): Optional<ErrorRateSample>"
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemBlindSampleStore
        _change: add
        visibility: public
        types: [Repository]
        implements: [BlindSampleStore]
        requiresInject:
          - { name: baseDir, type: Path }
```

## Separar lo que se guarda de lo que se muestra
Un par guardado sabe de qué lado quedó el después; lo que ve quien revisa no puede saberlo, ni traer marcas de
analizadores, motivo, fecha o autor (R002). Por eso hay dos portadores: `BlindPair`, con el lado, que sólo se entrega
revelado al cerrar, y `BlindPairView`, que es lo único que sale mientras está abierta. Cada lado es el ejercicio como lo
ve el alumno antes de contestar y la oración terminada, como la ve después; las cuatro respuestas son las de R003.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: PairAnswer
            _change: add
            type: enum
            fields:
              - { name: A_BETTER }
              - { name: B_BETTER }
              - { name: EQUAL }
              - { name: BOTH_WRONG }
          - name: BlindSide
            _change: add
            type: enum
            fields:
              - { name: A }
              - { name: B }
          - name: StudentSide
            _change: add
            type: record
            fields:
              - { name: view, type: QuizStudentView }
              - { name: answered, type: String }
          - name: BlindPair
            _change: add
            type: record
            fields:
              - { name: index, type: int }
              - { name: proposalId, type: String }
              - { name: nodeId, type: String }
              - { name: sideA, type: StudentSide }
              - { name: sideB, type: StudentSide }
              - { name: afterSide, type: BlindSide }
          - name: BlindPairView
            _change: add
            type: record
            fields:
              - { name: index, type: int }
              - { name: sideA, type: StudentSide }
              - { name: sideB, type: StudentSide }
          - name: PairAnswerRecord
            _change: add
            type: record
            fields:
              - { name: pairIndex, type: int }
              - { name: answer, type: PairAnswer }
              - { name: reviewer, type: String }
              - { name: answeredAt, type: Instant }
```

## Sortear una muestra que se pueda repetir
La muestra se sortea entre los cambios que la comparación de FEAT-METAS no devuelve —lo que se publicaría—, con la
semilla que se publica: los candidatos se ordenan por fecha de creación y id, se barajan con un generador inicializado
con la semilla y se toman los n de la meta (100, todos si hay menos); el mismo generador sortea el lado de cada par. Así
la misma tanda con la misma semilla da los mismos pares en el mismo orden (R001). El alcance es el de la tanda de E
(`ProjectionScope`), y el antes y el después salen de las fotos con lo aprobado y con lo pendiente, renderizadas.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: BlindSample
            _change: add
            type: record
            fields:
              - { name: sampleId, type: String }
              - { name: scopeId, type: String }
              - { name: baselineAuditId, type: String }
              - { name: activeAuditId, type: String }
              - { name: activePlanId, type: String }
              - { name: seed, type: long }
              - { name: drawnAt, type: Instant }
              - { name: goal, type: BlindSampleGoal }
              - { name: pairs, type: "List<BlindPair>" }
              - { name: answers, type: "List<PairAnswerRecord>" }
              - { name: closedAt, type: Instant }
          - name: SampleClosedException
            _change: add
            type: exception
            fields:
              - { name: sampleId, type: String }
        interfaces:
          - name: BlindSampleService
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "draw(Path coursePath, ProjectionScope scope, Long seed): BlindSample"
              - signature: "pairs(String sampleId): List<BlindPairView>"
              - signature: "answer(String sampleId, int pairIndex, PairAnswer answer, String reviewer): void"
              - signature: "close(String sampleId): BlindSampleResult"
              - signature: "result(String sampleId): Optional<BlindSampleResult>"
      - name: blindsampleengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultBlindSampleService
            _change: add
            visibility: public
            implements: [BlindSampleService]
            requiresInject:
              - { name: courseProjector, type: CourseProjector }
              - { name: batchComparator, type: BatchComparator }
              - { name: goalBook, type: GoalBook }
              - { name: baselineStore, type: BaselineStore }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: store, type: BlindSampleStore }
              - { name: statistics, type: SampleStatistics }
```

## Publicar cada resultado con su margen
Cerrar revela los lados, deja de aceptar respuestas (`SampleClosedException` sobre una cerrada) y publica las tres
proporciones sobre los pares respondidos. Los intervalos son de Wilson al 95 % —95 de 100 da [88,8; 97,8], 1 da
[0,2; 5,4] y 4 da [1,6; 9,8], como fija R004— y la cota del criterio «cota» y de la suma es la exacta a una cola: 0 de
100 da 3,0 %. La meta se evalúa con el criterio que declara (José: lo contado, 90 y 1 de 100), y la suma de las
muestras cerradas desde la línea base dice si ya se puede afirmar «menos de 2 %», como meta de avance que no decide. La
estadística es un puerto aparte porque es pura y la prueba de R004 la ejerce sola.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: ProportionEstimate
            _change: add
            type: record
            fields:
              - { name: count, type: int }
              - { name: total, type: int }
              - { name: share, type: double }
              - { name: lower95, type: double }
              - { name: upper95, type: double }
              - { name: upperOneSided95, type: double }
          - name: CumulativeSample
            _change: add
            type: record
            fields:
              - { name: samples, type: int }
              - { name: pairs, type: int }
              - { name: worse, type: int }
              - { name: worseUpperBound, type: double }
              - { name: belowGoal, type: boolean }
          - name: BlindSampleResult
            _change: add
            type: record
            fields:
              - { name: sampleId, type: String }
              - { name: scopeId, type: String }
              - { name: responsible, type: String }
              - { name: answered, type: int }
              - { name: betterOrEqual, type: ProportionEstimate }
              - { name: worse, type: ProportionEstimate }
              - { name: bothWrong, type: ProportionEstimate }
              - { name: meetsGoal, type: boolean }
              - { name: unmetParts, type: "List<String>" }
              - { name: agreement, type: ReviewerAgreement }
              - { name: cumulative, type: CumulativeSample }
              - { name: revealedPairs, type: "List<BlindPair>" }
      - name: blindsampleengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        interfaces:
          - name: SampleStatistics
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "proportion(int count, int total): ProportionEstimate"
              - signature: "kappa(List<PairAnswer> responsible, List<PairAnswer> other): double"
        implementations:
          - name: DefaultSampleStatistics
            _change: add
            visibility: public
            implements: [SampleStatistics]
```

## Medir cuánto coinciden dos revisores
Cada respuesta guarda quién y cuándo, así que dos personas pueden responder la misma muestra sin pisarse. El resultado
usa sólo las respuestas del responsable que declara la meta —José— y, si otra persona respondió pares en común, publica
en cuántos coinciden y el kappa de Cohen sobre esos pares (R005). En la primera tanda el tester responde los primeros 30
de la muestra: son los mismos pares para los dos, en el mismo orden.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: ReviewerAgreement
            _change: add
            type: record
            fields:
              - { name: responsible, type: String }
              - { name: other, type: String }
              - { name: commonPairs, type: int }
              - { name: agreeing, type: int }
              - { name: kappa, type: double }
```

## Publicar el resultado para el veredicto de la tanda
El veredicto de FEAT-METAS necesita saber si la muestra de la tanda está cerrada y en meta, y la muestra necesita la
comparación para saber qué vuelve. Para que ningún parche implemente un puerto de otro, el puerto que lee el resultado
y su portador se declaran acá; y para que no haya un ciclo de construcción —el comparador necesitaría al servicio y el
servicio al comparador— lo implementa un lector aparte, que sólo lee la muestra guardada por el id de la tanda. Sin
muestra, o abierta, el veredicto queda en «falta la muestra ciega» (F-METAS-R008, F-BANDEJA-R007).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: SampleOutcome
            _change: add
            type: record
            fields:
              - { name: sampleId, type: String }
              - { name: scopeId, type: String }
              - { name: closed, type: boolean }
              - { name: answered, type: int }
              - { name: betterOrEqual, type: int }
              - { name: worse, type: int }
              - { name: bothWrong, type: int }
              - { name: meetsGoal, type: boolean }
              - { name: detail, type: String }
        interfaces:
          - name: SampleOutcomeSource
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "outcomeFor(String scopeId): Optional<SampleOutcome>"
      - name: blindsampleengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultSampleOutcomeSource
            _change: add
            visibility: public
            implements: [SampleOutcomeSource]
            requiresInject:
              - { name: store, type: BlindSampleStore }
              - { name: statistics, type: SampleStatistics }
```

## Medir la tasa de error a ciegas
Es la medida de la propuesta v2 para saber si «algún error» mide lo que dice: n ejercicios de un nivel o del curso,
cada uno en su versión de la línea base y en la de la proyección, mezclados y sin decir cuál es cuál; quien revisa
marca si tiene un error. Al cerrar publica el porcentaje antes y después con su margen —con 400, ±4,3 puntos al 25,7 %—
junto al «algún error» de los analizadores en esos mismos ejercicios, y dice si caen dentro del margen (R006). Comparte
almacén, estadística y renderizador con la muestra de pares; la hace un tester, 400 al empezar y 400 al terminar.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: blindsample
        _change: add
        visibility: public
        models:
          - name: ErrorRateItem
            _change: add
            type: record
            fields:
              - { name: index, type: int }
              - { name: quizNodeId, type: String }
              - { name: fromBaseline, type: boolean }
              - { name: side, type: StudentSide }
          - name: ErrorRateItemView
            _change: add
            type: record
            fields:
              - { name: index, type: int }
              - { name: side, type: StudentSide }
          - name: ErrorMark
            _change: add
            type: record
            fields:
              - { name: itemIndex, type: int }
              - { name: hasError, type: boolean }
              - { name: reviewer, type: String }
              - { name: markedAt, type: Instant }
          - name: ErrorRateSample
            _change: add
            type: record
            fields:
              - { name: sampleId, type: String }
              - { name: nodeTarget, type: AuditTarget }
              - { name: nodeId, type: String }
              - { name: size, type: int }
              - { name: seed, type: long }
              - { name: drawnAt, type: Instant }
              - { name: baselineAuditId, type: String }
              - { name: activeAuditId, type: String }
              - { name: items, type: "List<ErrorRateItem>" }
              - { name: marks, type: "List<ErrorMark>" }
              - { name: closedAt, type: Instant }
          - name: ErrorRateResult
            _change: add
            type: record
            fields:
              - { name: sampleId, type: String }
              - { name: before, type: ProportionEstimate }
              - { name: after, type: ProportionEstimate }
              - { name: analyzersBefore, type: ProportionEstimate }
              - { name: analyzersAfter, type: ProportionEstimate }
              - { name: analyzersWithinMargin, type: boolean }
        interfaces:
          - name: BlindErrorRateService
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "draw(Path coursePath, AuditTarget nodeTarget, String nodeId, int size, Long seed): ErrorRateSample"
              - signature: "items(String sampleId): List<ErrorRateItemView>"
              - signature: "mark(String sampleId, int itemIndex, boolean hasError, String reviewer): void"
              - signature: "close(String sampleId): ErrorRateResult"
      - name: blindsampleengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultBlindErrorRateService
            _change: add
            visibility: public
            implements: [BlindErrorRateService]
            requiresInject:
              - { name: courseProjector, type: CourseProjector }
              - { name: baselineStore, type: BaselineStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: store, type: BlindSampleStore }
              - { name: statistics, type: SampleStatistics }
```

## Responder desde la CLI
El dashboard habla con content-audit por la CLI, así que las dos mediciones tienen sus verbos, con el molde de siempre:
`sample draw|show|answer|close` y `error-rate draw|show|mark|close`, con un puerto sellado por medición. `show` sólo
entrega lo que se puede ver mientras está abierta. La bandeja de E la presenta primero y sola hasta que se cierra, y la
sortea cuando la tanda pasó su compuerta (ganchos F-08 y F-11).

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: BlindSampleCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "draw(String coursePath, List<String> proposalIds, Long seed, String format): Integer"
          - signature: "show(String sampleId, String format): Integer"
          - signature: "answer(String sampleId, int pairIndex, String answer, String reviewer): Integer"
          - signature: "close(String sampleId, String format): Integer"
      - name: ErrorRateCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "draw(String coursePath, String level, int size, Long seed, String format): Integer"
          - signature: "show(String sampleId, String format): Integer"
          - signature: "mark(String sampleId, int itemIndex, boolean hasError, String reviewer): Integer"
          - signature: "close(String sampleId, String format): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: DefaultBlindSampleCommand
            _change: add
            types: [Component]
            implements: [BlindSampleCommand]
            requiresInject:
              - { name: blindSampleService, type: BlindSampleService }
          - name: DefaultErrorRateCommand
            _change: add
            types: [Component]
            implements: [ErrorRateCommand]
            requiresInject:
              - { name: blindErrorRateService, type: BlindErrorRateService }
```

## Registrar FEAT-CIEGA
La feature se registra para que `feature sync --feature FEAT-CIEGA` anote J001; su ubicación es del qa-tester. Los
números que fijan las pruebas son los de R004 (95, 1 y 4 de 100; la cota de 3,0 % con 0 peores; 2,1 % con 300 pares y 2
peores) y el par de las verduras de R002. Se construye después del renderizador del integrador y con FEAT-PROYU y
FEAT-METAS aplicados: sus tipos se usan acá.

```architecture
features:
  - id: FEAT-CIEGA
    code: F-CIEGA
modules:
  - name: audit-cli
    _change: modify
```
