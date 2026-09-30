---
patch: FEAT-JEXAM
requirement: 2026-09-30.12_juez-de-consigna-examen
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: El examen fijo de los jueces y el juez de consigna, sin tocar su huella (FEAT-JEXAM)

Paquete C, ronda 1. Este parche lleva lo común a los cinco jueces con modelo —el de consigna, `translation-sense`,
`single-answer`, `mini-theory` y el juez de opción múltiple que aloja el analizador del paquete A— y lo que cambia del
juez de consigna. Se diseñó contra el modelo que queda después de 0.2 y 0.3; los otros cuatro parches del paquete usan
sus tipos, así que éste se aplica antes que ellos. Rebasado el 30/9 sobre el `sentinel.yaml` real, con 0.2 y 0.3
aplicados, y sobre el parche de integración de la ronda 1 (FEAT-RONDA1), que se aplica primero y trae los ganchos de este
paquete que caen en archivos comunes (C-04, C-05, C-09, C-16, C-17); `sentinel-baseline.yaml` queda como foto del diseño.

**Tipos que vienen de 0.3 (FEAT-HALL) y no se declaran acá**: `AnalyzerProvider` (que `DefaultQuizInstructionAnalyzerFactory`
implementa desde 0.3), `AnalyzerDescriptor` con sus campos nuevos (reglas, familia, `evaluatedTargets`, costo),
`AnalyzerRuleCard`, `AnalyzerCatalog`, `FindingSeverity`, `FindingDraft`, `ContentAnalyzer.findingsAt`,
`AuditNode.unevaluatedBy` y el tope por nombre `--budget nombre=N` (`AnalyzeOptions.analyzerBudgets`). **De 0.2
(FEAT-OPMUL)**: `AuditableQuiz.multipleChoice` y `MultipleChoiceEntity`, que lee el render de los jueces nuevos; el juez
de consigna ya renderiza la opción múltiple con su constructor de hoy.

**Lo que se reusa del paquete A y del integrador, en vez de declararlo dos veces**: el render «como lo ve el alumno»
(`QuizStudentViewRenderer`, gancho A-01, del que los jueces nuevos arman su mapa de contenido: su formato queda
congelado por la huella, porque cambiarlo re-cobra sus veredictos) y la comparación del corrector (`CorrectorMatcher`,
de FEAT-RESPC), la única tabla de equivalencias, que imita al corrector de Go (José, 30/9).

**Fuera del parche, como ganchos del integrador** (`ronda1/ganchos-C.md`): la ranura común de cobertura de jueces en
`CourseDiagnoses`, el cableado en `Main`, las opciones de `analyze` para declarar un conjunto por juez y consultas a la
vez, la resolución de la configuración de los jueces desde el entorno y el registro de `ExamCmd`. Las definiciones de
los agentes (`.sentinel/agents/…`) no son de Sentinel: las escribe quien construye, con el contrato de entradas y salida
que fija cada parche.

## Resolver en lote, con consultas a la vez y sin cambiar qué se consulta

Hoy la sesión resuelve de a un sujeto por llamada y el juez de consigna consulta de a una dentro de `onQuiz`; con cinco
jueces y 41.700 consultas eso son 220 horas. `resolveAll` recibe todos los pedidos de la corrida con su modo —los cuatro
métodos de a uno pasan a ser valores de `EvaluationRequestMode`— y decide en el hilo que llama, en el orden de la lista,
cuáles consultan dentro del tope: exactamente los mismos que de a uno (F-JEXAM-R006 inv. 1 y 3). Sólo después reparte
esas consultas entre N hilos, que hacen dos cosas nada más: `evaluate` y `append`, cuyo alta por línea ya es atómica en
el adaptador de archivos. La primera consulta sale sola: si el servicio no está, falla una y no N (F-JEXAM-R011), y tras
una falla del servicio no empieza ninguna más (inv. 2). `recorded` dice si un contenido ya tiene resultado sin contarlo
en la cobertura, que es lo que el juez de traducción necesita para repartir el tope de a pares; `unavailableReason`
conserva el motivo de la primera falla del servicio para el informe.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationRequestMode
        _change: add
        type: enum
        fields:
          - name: REUSE_OR_EVALUATE
          - name: FORCE
          - name: PREFER_NEW
          - name: REUSE_ONLY
      - name: EvaluationRequest
        _change: add
        type: record
        fields:
          - { name: subject, type: EvaluationSubject }
          - { name: mode, type: EvaluationRequestMode }
    interfaces:
      - name: EvaluationSession
        _change: modify
        sealed: false
        exposes:
          - signature: "resolveAll(List<EvaluationRequest> requests,int maxConcurrent): List<EvaluationResolution>"
            _change: add
          - signature: "recorded(EvaluationSubject subject): boolean"
            _change: add
          - signature: "unavailableReason(): Optional<String>"
            _change: add
```

## Medir el ritmo con el propio libro, en un puerto aparte

El costo de lo pendiente se declara al ritmo medido de cada juez: la mediana entre veredictos seguidos (F-JEXAM-R004).
El libro ya guarda la hora de cada registro, pero su puerto sólo busca por clave. Se agrega un puerto chico en vez de
un método más en `EvaluationLedger` porque hay libros falsos escritos a mano en los tests de FEVCOST y QINST que
dejarían de compilar; el adaptador de archivos implementa los dos y el libro de examen, ninguno de más.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: EvaluationTimeline
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "recordedTimes(String evaluatorId): List<Instant>"
  - name: evaluation-ledger-infrastructure
    _change: modify
    implementations:
      - name: FileSystemEvaluationLedger
        _change: modify
        implements: [EvaluationLedger, EvaluationTimeline]
```

## Rendir el examen sin escribir en el libro del curso

Si el examen escribiera donde lee el curso, una versión reprobada quedaría vigente en esos ejercicios, porque en el curso
gana el veredicto más reciente (F-EVCOST-R006): `analyze` no daría lo mismo antes y después de rendir (F-JEXAM-R002). Las
sesiones del examen son las de siempre sobre un libro que se ve distinto: `ExamScopedEvaluationLedger` devuelve sólo
registros de la versión que rinde —primero del espacio de examen y después del curso, así la versión que ya rindió o
que ya juzgó ese contenido en el curso vuelve a rendir sin consultar— y escribe sólo en el espacio de examen, un
evaluador con prefijo `exam:` que el adaptador guarda en su propio archivo. `DefaultEvaluationSession` no cambia de
reglas: la diferencia está entera en el libro que recibe.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    packages:
      - name: evaluationsession
        _change: modify
        visibility: public
        implementations:
          - name: ExamEvaluationSessionFactory
            _change: add
            visibility: public
            implements: [EvaluationSessionFactory]
            requiresInject:
              - { name: ledger, type: EvaluationLedger }
              - { name: fingerprinter, type: ContentFingerprinter }
          - name: ExamScopedEvaluationLedger
            _change: add
            implements: [EvaluationLedger]
            requiresInject:
              - { name: ledger, type: EvaluationLedger }
              - { name: pinnedVersion, type: String }
```

## Decir en la política para qué se corre y cuántas a la vez

Desde 0.3 lo único que llega al juez por corrida es la política (`AnalyzerProvider.create(policy)`), así que el propósito
y las consultas a la vez viajan ahí o no viajan. Null en los dos es lo de siempre —corrida sobre el curso, y las 4 a la
vez que decidió José—, de modo que ningún llamador de hoy cambia de comportamiento. `EXAM` es lo que usa el servicio del
examen; un candidato de la mesa se juzga como un curso más, con `COURSE`, sobre un curso candidato cuyo informe no se
guarda (F-JEXAM-R005 inv. 2), así que no hace falta un tercer valor. Cuesta 29 construcciones con la aridad nueva, casi
todas en tests.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: EvaluationRunPurpose
        _change: add
        type: enum
        fields:
          - name: COURSE
          - name: EXAM
      - name: EvaluationRunPolicy
        _change: modify
        type: record
        fields:
          - { name: purpose, type: EvaluationRunPurpose, _change: add }
          - { name: maxConcurrentEvaluations, type: Integer, _change: add }
```

## Un runtime común de jueces: el freno del examen y los valores por omisión en un solo lugar

Cinco jueces que abren sus sesiones cada uno a su manera son cinco lugares donde olvidar el freno del examen o el tope 0.
`JudgeRuntime.open` es el único que abre: calcula la versión del juez —la de cada evaluador, en orden, unidas por «+»—,
completa la política (propósito, tope por omisión, consultas a la vez), y en una corrida `COURSE` consulta el registro
del examen: sin examen aprobado, el tope efectivo es 0 aunque se haya pedido otro, y el porqué queda en el
`JudgeExamStanding` (F-JEXAM-R003). En `EXAM` abre sobre el libro de examen y no frena. `plan` decide qué pide cada unidad
con la misma semántica que el juez de consigna ya tiene para la re-evaluación por área y por conjunto (F-QINST-R012,
R018), y en `EXAM` pide sólo las unidades del archivo; en el de miniteoría un tema entra si su id o el de alguno de sus
ejercicios está declarado. Rechaza el pedido ininterpretable antes de abrir nada, como hoy la fábrica del juez de
consigna.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: judging
        _change: add
        visibility: public
        models:
          - name: JudgeExamStatus
            _change: add
            type: enum
            fields:
              - name: APPROVED
              - name: FAILED
              - name: TAKEN
              - name: NOT_TAKEN
              - name: NO_VERSION
          - name: JudgeExamStanding
            _change: add
            type: record
            fields:
              - { name: judgeName, type: String }
              - { name: judgeVersion, type: String }
              - { name: status, type: JudgeExamStatus }
              - { name: consultsOnCourse, type: boolean }
              - { name: reason, type: String }
          - name: JudgeSessions
            _change: add
            type: record
            fields:
              - { name: judgeName, type: String }
              - { name: judgeVersion, type: String }
              - { name: evaluatorIds, type: "List<String>" }
              - { name: sessions, type: "List<EvaluationSession>" }
              - { name: standing, type: JudgeExamStanding }
              - { name: effectivePolicy, type: EvaluationRunPolicy }
          - name: JudgeRequestPlan
            _change: add
            type: record
            fields:
              - { name: units, type: "List<AuditNode>" }
              - { name: modes, type: "List<EvaluationRequestMode>" }
              - { name: unmatchedDeclaredIds, type: "List<String>" }
        interfaces:
          - name: JudgeRuntime
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "open(String judgeName,List<Evaluator> evaluators,EvaluationRunPolicy policy): JudgeSessions"
                throwsExceptions: [EmptyReevaluationSetException, AmbiguousReevaluationScopeException]
              - signature: "plan(List<AuditNode> units,EvaluationRunPolicy policy): JudgeRequestPlan"
              - signature: "coverage(JudgeSessions opened,JudgeUnitTally tally): JudgeCoverageDiagnosis"
              - signature: "standing(String judgeName,String judgeVersion): JudgeExamStanding"
      - name: judgingengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultJudgeRuntime
            _change: add
            visibility: public
            implements: [JudgeRuntime]
            requiresInject:
              - { name: courseSessions, type: EvaluationSessionFactory }
              - { name: examSessions, type: EvaluationSessionFactory }
              - { name: examRegistry, type: JudgeExamRegistry }
              - { name: timeline, type: EvaluationTimeline }
              - { name: config, type: JudgeRuntimeConfig }
```

## Declarar la cobertura con el examen, el costo y la causa, igual para todos

Una cobertura común hace que el informe, `stats analyzer`, el tablero y el examen lean a los cinco jueces de la misma
manera. El juez cuenta sus unidades (`JudgeUnitTally`) y el runtime completa lo demás: la cobertura de cada evaluador y
la versión que consultó (F-QINST-R005), el examen de la versión vigente y de cada versión que aporta veredictos
(F-JEXAM-R001 inv. 2), el costo de lo pendiente (F-JEXAM-R004) y por qué dejó de consultar (F-JEXAM-R011). Las consultas
que faltan son la suma de lo pendiente y fallido de cada evaluador —el de traducción cuenta dos por ejercicio—; el ritmo
es el promedio entre veredictos seguidos de sus evaluadores dentro de cada corrida, y sin historia el de la
configuración; con N a la vez las horas se dividen por N. Promedio y no mediana: en el libro del juez de consigna la
mediana da 15,5 s y el promedio 20,4 s, y las horas son la suma de los intervalos. La unidad dice si se consultó por ejercicio o por tema; los hallazgos van siempre en el
ejercicio.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: judging
        _change: add
        visibility: public
        models:
          - name: JudgeUnitTally
            _change: add
            type: record
            fields:
              - { name: unit, type: AuditTarget }
              - { name: reached, type: int }
              - { name: evaluated, type: int }
              - { name: notEvaluated, type: int }
              - { name: verdictsByVersion, type: "Map<String,Integer>" }
              - { name: unmatchedDeclaredIds, type: "List<String>" }
          - name: EvaluatorCoverage
            _change: add
            type: record
            fields:
              - { name: evaluatorId, type: String }
              - { name: coverage, type: EvaluationCoverage }
              - { name: consultedVersion, type: String }
          - name: JudgeVersionStanding
            _change: add
            type: record
            fields:
              - { name: judgeVersion, type: String }
              - { name: verdictCount, type: int }
              - { name: examStatus, type: JudgeExamStatus }
          - name: JudgeCostEstimate
            _change: add
            type: record
            fields:
              - { name: pendingUnits, type: int }
              - { name: pendingConsultations, type: int }
              - { name: ratePerHour, type: double }
              - { name: rateMeasured, type: boolean }
              - { name: hoursOneAtATime, type: double }
              - { name: maxConcurrentEvaluations, type: int }
              - { name: hoursConcurrent, type: double }
          - name: JudgeCoverageDiagnosis
            _change: add
            type: record
            fields:
              - { name: judgeName, type: String }
              - { name: unit, type: AuditTarget }
              - { name: reached, type: int }
              - { name: evaluated, type: int }
              - { name: notEvaluated, type: int }
              - { name: evaluators, type: "List<EvaluatorCoverage>" }
              - { name: standing, type: JudgeExamStanding }
              - { name: versions, type: "List<JudgeVersionStanding>" }
              - { name: unavailableReason, type: String }
              - { name: cost, type: JudgeCostEstimate }
              - { name: unmatchedDeclaredIds, type: "List<String>" }
```

## Registrar el examen por versión, con el oro afuera

El oro del examen fijo lo tiene quien mide y el juez no lo ve: content-audit recibe un archivo con sólo los ejercicios,
escribe un veredicto por ejercicio y después registra el resultado que se midió afuera (F-JEXAM-R001). El registro es
append-only, como el libro, en `.content-audit/judge-exams/<juez>.jsonl`, y el vigente de una versión es su último
registro. `TAKEN` se escribe al rendir todos los ejercicios y es la condición para aceptar una medición: nadie puede
aprobar una versión que no rindió. El puerto vive en la raíz del módulo porque su adaptador vive en la raíz de
audit-infrastructure y Sentinel no resuelve una implementación de raíz contra una interfaz de un paquete ajeno.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: JudgeExamRegistry
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "record(JudgeExamRecord record): void"
          - signature: "latest(String judgeName,String judgeVersion): Optional<JudgeExamRecord>"
          - signature: "history(String judgeName): List<JudgeExamRecord>"
    packages:
      - name: judging
        _change: add
        visibility: public
        models:
          - name: JudgeExamGroupResult
            _change: add
            type: record
            fields:
              - { name: group, type: String }
              - { name: cases, type: int }
              - { name: asExpected, type: int }
              - { name: notAsExpected, type: int }
          - name: JudgeExamRecord
            _change: add
            type: record
            fields:
              - { name: judgeName, type: String }
              - { name: judgeVersion, type: String }
              - { name: status, type: JudgeExamStatus }
              - { name: casesJudged, type: int }
              - { name: casesNotJudged, type: int }
              - { name: groups, type: "List<JudgeExamGroupResult>" }
              - { name: mismatchedCaseIds, type: "List<String>" }
              - { name: goalMet, type: boolean }
              - { name: photo, type: String }
              - { name: measuredBy, type: String }
              - { name: recordedAt, type: Instant }
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemJudgeExamRegistry
        _change: add
        visibility: public
        implements: [JudgeExamRegistry]
        requiresInject:
          - { name: baseDir, type: Path }
```

## Configurar los valores por omisión de los jueces en un solo lugar

Tres decisiones de José viven acá y en ningún otro lado: tope 0 sin pedido para los cinco, el de consigna incluido
(F-JEXAM-R010), 4 consultas a la vez (F-JEXAM-R006) y el ritmo por omisión, 189 por hora y 60 para el de miniteoría, que
lee temas enteros. El tope de 500 de `QuizInstructionConfig` sigue existiendo, pero sólo para la verificación de
candidatos de `revise-instructions`, que ya es un pedido explícito; `analyze` deja de usarlo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: JudgeRuntimeConfig
        _change: add
        stereotype: port
        sealed: false
        extends: [SelfDescribingConfig]
        exposes:
          - signature: "getMaxConcurrentEvaluations(): int"
          - signature: "getDefaultMaxNewEvaluations(String judgeName): int"
          - signature: "getDefaultRatePerHour(String judgeName): double"
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultJudgeRuntimeConfig
        _change: add
        visibility: public
        implements: [JudgeRuntimeConfig]
        types: [Component]
```

## Rendir el examen por el mismo camino que analyze

«Leído igual que en el curso» (F-JEXAM-R001) se garantiza por construcción: el servicio corre `AuditRunner` sobre la
foto congelada del 29/9 con un solo analizador —el que aloja al juez— y una política `EXAM` cuyo conjunto declarado son
los ejercicios del archivo. El informe no se guarda. El exportador lee sólo contratos de 0.3: en cada ejercicio del
archivo, los hallazgos de las reglas de modelo pago de la ficha dan `FAILS` si alguno cuenta como error, `MARKED` si
sólo ordenan y `PASSES` si no hay; `unevaluatedBy` da `NOT_JUDGED`. Así el mismo servicio examina al juez de opción
múltiple dentro del analizador de A sin saber de él. Con tope 0 es un ensayo: reusa lo de esa versión y dice cuánto
falta, sin pagar (F-JEXAM-R010).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: judging
        _change: add
        visibility: public
        models:
          - name: JudgeExamOutcome
            _change: add
            type: enum
            fields:
              - name: FAILS
              - name: MARKED
              - name: PASSES
              - name: NOT_JUDGED
          - name: JudgeExamVerdict
            _change: add
            type: record
            fields:
              - { name: caseId, type: String }
              - { name: judgeName, type: String }
              - { name: judgeVersion, type: String }
              - { name: outcome, type: JudgeExamOutcome }
              - { name: worstSeverity, type: FindingSeverity }
              - { name: rules, type: "List<String>" }
              - { name: detail, type: String }
        interfaces:
          - name: JudgeExamVerdictExporter
            _change: add
            stereotype: service
            sealed: false
            exposes:
              - signature: "export(AuditReport report,AnalyzerDescriptor card,Set<String> caseIds,JudgeCoverageDiagnosis coverage): List<JudgeExamVerdict>"
      - name: judgingengine
        _change: add
        visibility: public
        allowedClients: [audit-cli]
        implementations:
          - name: DefaultJudgeExamVerdictExporter
            _change: add
            visibility: public
            implements: [JudgeExamVerdictExporter]
  - name: audit-application
    _change: modify
    models:
      - name: JudgeExamRequest
        _change: add
        type: record
        fields:
          - { name: judgeName, type: String }
          - { name: photoCourse, type: Path }
          - { name: caseIds, type: "Set<String>" }
          - { name: maxNewEvaluations, type: int }
          - { name: maxConcurrentEvaluations, type: Integer }
      - name: JudgeExamTaken
        _change: add
        type: record
        fields:
          - { name: judgeName, type: String }
          - { name: judgeVersion, type: String }
          - { name: verdicts, type: "List<JudgeExamVerdict>" }
          - { name: coverage, type: JudgeCoverageDiagnosis }
          - { name: recordedAsTaken, type: boolean }
      - name: JudgeExamNotTakenException
        _change: add
        type: exception
        extends: IllegalArgumentException
        fields:
          - { name: judgeName, type: String }
          - { name: judgeVersion, type: String }
    interfaces:
      - name: JudgeExamService
        _change: add
        stereotype: service
        sealed: false
        exposes:
          - signature: "take(JudgeExamRequest request): JudgeExamTaken"
          - signature: "record(JudgeExamRecord measured): JudgeExamStanding"
            throwsExceptions: [JudgeExamNotTakenException]
          - signature: "history(String judgeName): List<JudgeExamRecord>"
    implementations:
      - name: DefaultJudgeExamService
        _change: add
        visibility: public
        implements: [JudgeExamService]
        requiresInject:
          - { name: auditRunner, type: AuditRunner }
          - { name: analyzerCatalog, type: AnalyzerCatalog }
          - { name: exporter, type: JudgeExamVerdictExporter }
          - { name: examRegistry, type: JudgeExamRegistry }
          - { name: runtime, type: JudgeRuntime }
```

## Un comando fino para los tres archivos del examen

`exam run <juez> --cases <archivo> --course <foto> [--budget N] [--concurrency N] --out <veredictos>` lee
`[{"ejercicio": "<id>"}, …]`, rinde y escribe una línea JSON por ejercicio con `ejercicio`, `juez`, `version`,
`resultado`, `gravedad`, `reglas` y `detalle`; sin `--budget` es un ensayo. `exam record <resultado>` lee lo que midió
quien tiene el oro —juez, versión, `APROBADO` o `REPROBADO`, grupos, los que no dieron lo esperado, si alcanzó la meta,
la foto y quién midió— y lo registra; `exam status <juez>` muestra la historia. El comando sólo lee y escribe archivos:
todo lo demás es del servicio.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: ExamCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "takeExam(String judgeName,String casesFile,String photoCourse,Integer budget,Integer concurrency,String verdictsOut): Integer"
          - signature: "recordExam(String resultFile): Integer"
          - signature: "showExams(String judgeName): Integer"
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: ExamCmd
            _change: add
            implements: [ExamCommand]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            types: [Component]
            requiresInject:
              - { name: examService, type: JudgeExamService }
```

## El juez de consigna: freno, costo y a la vez, sin tocar lo que lee

Lo que lee el juez de consigna y su huella no cambian (F-JEXAM-R007): su constructor de sujetos no se toca y los
~3.900 veredictos siguen reusándose. Lo que cambia es cómo abre y cómo resuelve. La fábrica deja de abrir la sesión por
su cuenta y la pide al runtime, que aplica el freno del examen (José, 30/9: «sin aprobar, no paga») y el tope 0 sin
pedido; el analizador junta los pedidos en `onQuiz` y los resuelve en `onCourseComplete` con `resolveAll`, y su decisión
de alcance suma los dos casos del examen. La cobertura de hoy suma un campo con lo común de todo juez. El veredicto
entero de F-JEXAM-R008 —cada restricción revisada con su estado, y la confianza— sale del diagnóstico que ya existe y
va en la evidencia del hallazgo (`findingsAt`, de 0.3) y en su vista detallada: es cuerpo, no contrato. Costo: 38
construcciones del analizador, 7 de la cobertura y 2 de la fábrica cambian de aridad.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        visibility: public
        models:
          - name: QuizInstructionCoverageDiagnosis
            _change: modify
            type: record
            fields:
              - { name: judge, type: JudgeCoverageDiagnosis, _change: add }
      - name: quizinstructionengine
        _change: modify
        visibility: public
        models:
          - name: QuizInstructionReevaluationDecision
            _change: modify
            type: enum
            visibility: internal
            fields:
              - { name: EXAM_CASE, _change: add }
              - { name: NOT_REQUESTED, _change: add }
        implementations:
          - name: DefaultQuizInstructionAnalyzerFactory
            _change: modify
            requiresInject:
              - { name: sessionFactory, type: EvaluationSessionFactory, _change: delete }
              - { name: runtime, type: JudgeRuntime }
          - name: QuizInstructionAnalyzer
            _change: modify
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
```

## Un evaluador genérico por agente para los jueces nuevos, en el módulo del juez de consigna

Los cuatro jueces nuevos tienen la misma forma que el de consigna: correr un agente con el mapa de contenido como
entradas, leer un artifact JSON, separar el veredicto conservador de la falla de servicio y derivar la versión de la
definición, el modelo y el proveedor. `AgentJudge` lo hace una vez, parametrizado por `AgentJudgeSpec`; cada juez es un
spec y una definición de agente. Vive en el módulo del juez de consigna porque ese módulo ya es exactamente esto —un
adaptador de juez sobre el runtime de agentes, sólo para la raíz de composición—: un módulo nuevo pediría tocar los
`allowedClients` de agent-runtime-infrastructure y las dependencias de audit-cli, y sólo cambiaría el nombre. El nombre
queda como deuda anotada; la descripción ya dice lo que aloja. El juez de consigna conserva su adaptador y su versión:
migrarlo cambiaría la versión que declara y no hace falta. `patterns` se declara completo porque al aplicar reemplaza la
lista del módulo.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    models:
      - name: AgentJudgeSpec
        _change: add
        type: record
        fields:
          - { name: evaluatorId, type: String }
          - { name: verdictArtifactKey, type: String }
          - { name: displayName, type: String }
      - name: AgentJudgeConfig
        _change: add
        type: record
        fields:
          - { name: providerName, type: String }
          - { name: baseUrl, type: String }
          - { name: apiKey, type: String }
          - { name: modelName, type: String }
          - { name: temperature, type: Double }
          - { name: timeoutSeconds, type: Integer }
    interfaces:
      - name: AgentJudgeFactory
        _change: add
        stereotype: factory
        sealed: false
        exposes:
          - signature: "create(AgentJudgeSpec spec,AgentJudgeConfig config,AgentGraphRunner agentGraphRunner,AgentDefinitionLocator agentDefinitionLocator): Evaluator"
    patterns:
      - type: Factory
        interface: QuizInstructionJudgeFactory
        implementations: [DefaultQuizInstructionJudgeFactory]
      - type: Factory
        interface: AgentJudgeFactory
        implementations: [DefaultAgentJudgeFactory]
    packages:
      - name: agentjudge
        _change: add
        visibility: public
        interfaces:
          - name: AgentJudgeVersionResolver
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "resolve(AgentJudgeSpec spec,AgentJudgeConfig config,LlmProviderResolved provider): Optional<String>"
          - name: AgentJudgeChatModelFactory
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "create(AgentJudgeConfig config,LlmProviderResolved provider): ChatModel"
        implementations:
          - name: DefaultAgentJudgeFactory
            _change: add
            visibility: public
            implements: [AgentJudgeFactory]
            requiresInject:
              - { name: llmProviderResolver, type: LlmProviderResolver }
              - { name: readinessProbe, type: JudgeProviderReadinessProbe }
          - name: AgentJudge
            _change: add
            implements: [Evaluator]
            requiresInject:
              - { name: spec, type: AgentJudgeSpec }
              - { name: config, type: AgentJudgeConfig }
              - { name: agentGraphRunner, type: AgentGraphRunner }
              - { name: versionResolver, type: AgentJudgeVersionResolver }
              - { name: chatModelFactory, type: AgentJudgeChatModelFactory }
              - { name: provider, type: LlmProviderResolved }
              - { name: readinessProbe, type: JudgeProviderReadinessProbe }
          - name: DefaultAgentJudgeVersionResolver
            _change: add
            implements: [AgentJudgeVersionResolver]
            requiresInject:
              - { name: agentDefinitionLocator, type: AgentDefinitionLocator }
          - name: SentinelAgentsAgentJudgeChatModelFactory
            _change: add
            implements: [AgentJudgeChatModelFactory]
```

## Saber sin pagar si la sesión del proveedor venció

La sesión local de `claude-cli`, el proveedor que eligió José, venció el 30/9. Hoy eso se ve como una consulta que
falla con un texto crudo del CLI y todo lo demás pendiente, sin decir por qué ni cómo se arregla. Antes de su primera
consulta, cada juez le pide a la verificación que mire la sesión sin costo: para un proveedor de sesión local corre la
herramienta de estado del propio CLI (`claude auth status`), que no llama a ningún modelo; para uno remoto no puede
saberlo sin consultar y lo deja pasar. Si la verificación no alcanza y la primera consulta vuelve por falta de sesión o
de cuota, `explain` clasifica el texto de la falla. En los dos casos el evaluador devuelve `EVALUATOR_UNAVAILABLE` con
la causa y el remedio («sesión vencida: renovala con `claude auth login`»), la sesión deja de consultar (F-EVCOST-R009)
y el motivo llega a la cobertura. El juez de consigna usa la misma verificación: no cambia su versión ni su huella. El
comando que pregunta por la sesión (`claude auth status`) se inyecta: lo pasa `Main`, y una prueba puede darle uno falso
sin tocar el PATH (pendiente que dejó el qa-tester del paquete, resuelto al integrar).

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    models:
      - name: JudgeUnavailabilityCause
        _change: add
        type: enum
        fields:
          - name: SESSION_EXPIRED
          - name: QUOTA_EXHAUSTED
          - name: SERVICE_UNREACHABLE
          - name: TIMEOUT
          - name: NOT_INSTALLED
          - name: UNKNOWN
      - name: JudgeProviderReadiness
        _change: add
        type: record
        fields:
          - { name: ready, type: boolean }
          - { name: cause, type: JudgeUnavailabilityCause }
          - { name: detail, type: String }
          - { name: remedy, type: String }
    interfaces:
      - name: JudgeProviderReadinessProbe
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "probe(LlmProviderResolved provider): JudgeProviderReadiness"
          - signature: "explain(LlmProviderResolved provider,String failure): JudgeProviderReadiness"
    packages:
      - name: agentjudge
        _change: add
        visibility: public
        implementations:
          - name: DefaultJudgeProviderReadinessProbe
            _change: add
            visibility: public
            implements: [JudgeProviderReadinessProbe]
            requiresInject:
              - { name: authStatusCommand, type: "List<String>" }
      - name: instructionjudge
        _change: modify
        visibility: public
        implementations:
          - name: DefaultQuizInstructionJudgeFactory
            _change: modify
            requiresInject:
              - { name: readinessProbe, type: JudgeProviderReadinessProbe }
          - name: QuizInstructionAgentJudge
            _change: modify
            requiresInject:
              - { name: readinessProbe, type: JudgeProviderReadinessProbe }
```

## Registrar FEAT-JEXAM

Sin registro la feature no llega a la generación ni a la trazabilidad. Al aplicar, Sentinel copia de REQUIREMENT.md la
compuerta con F-JEXAM-J001; ubicarlo es del qa-tester, y el integrador suma la carpeta a `definitions` y corre
`feature sync --feature FEAT-JEXAM`.

```architecture
features:
  - id: FEAT-JEXAM
    code: F-JEXAM
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 90 pruebas y ubicar el journey
Las 90 son nuevas y cubren de R001 a R011 (R001 25, R002 9, R003 8, R004 6, R005 5, R006 7, R007 4, R008 3, R009 2,
R010 6, R011 15). Van sobre las implementaciones del parche y sobre `DefaultEvaluationSession`, que el parche no nombra
pero es la que cumple el contrato nuevo de `EvaluationSession`: consultas a la vez, la primera sola y el motivo de la
falla. **Ninguna consulta un modelo pago**: el evaluador es un doble que devuelve veredictos grabados, con la forma que
emite cada agente, y cuenta cuántas veces lo llaman; la sesión de `claude-cli` se verifica contra una respuesta grabada de
`claude auth status`, nunca contra la de la máquina.

**De dónde sale cada prueba.**
- **Los casos del requirement**: el tope 0 sin pedido (R010, 500 → 0 para el de consigna); la sesión vencida con tope 100,
  que llama a lo sumo una vez y dice «sesión vencida: renovala con claude auth login» (R011); el ritmo como promedio entre
  veredictos seguidos, cortado en pausas de más de 10 minutos (R004, con números chicos: 352 pendientes a 176 por hora
  son 2 horas de a una y media hora con 4); las verduras (…1ecd), que el juez de consigna dio por buenas en agosto (R009).
- **La huella del juez de consigna** (R007): un libro grabado antes de FEAT-JEXAM, con la huella que calcula el código de
  hoy, se tiene que seguir encontrando sin consultar, y la versión del juez de consigna no cambia con la verificación nueva.
- **El examen** (R001, R002, gancho C-24): un archivo sintético del qa-tester —las verduras, cuatro ejercicios de
  `escribir/` y un id que no está en la foto—, nunca el examen fijo, que tiene el orquestador. Su entrada es
  `[{"ejercicio": "67fab6d59930102295341ecd"}, …]` y cada línea de salida
  `{"ejercicio", "juez", "version", "resultado", "gravedad", "reglas", "detalle"}`; con el id que falta sale
  `NOT_JUDGED`, la versión no queda TAKEN y `exam record` rechaza su resultado.

**Dependencias.** Los hallazgos, la ficha y `unevaluatedBy` son de 0.3 (R001 en el exportador, R008). Del integrador:
C-04 (la cobertura común en `CourseDiagnoses`), C-12 y C-13 (cableado), C-16 (la configuración del entorno) y, para el
journey, C-17 (`--budget`, `--reevaluate` por nombre y `--concurrency`), C-18 (el comando `exam`) y C-25 (la foto).
`DefaultJudgeProviderReadinessProbe` recibe por inyección el comando de `claude auth status` (`authStatusCommand`, del
rebase): sus dos pruebas de `claude-cli` le pasan uno falso. La prueba de hoy
«should build the analyzer with the configured default budget when the run declares no policy for the judge»
(F-QINST-R006) afirma 500 y R010 lo lleva a 0: se reescribe su cuerpo, igual que el constructor de las 27 de
`QuizInstructionAnalyzer` y las 4 de su fábrica. R012 no tiene prueba propia: la cumplen los clasificadores de FEAT-UNICA y
FEAT-MCVALE, donde se prueba su criterio, y `patch propose` no acepta trazar a otra feature. Con
DOUBT-EXAMEN-Y-CANDIDATOS abierta, la prueba del candidato usa una versión aprobada y el freno sobre candidatos no se
declara.

**El journey** va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`: sus pasos son `exam run`,
`exam record` y `analyze`, y `ExamCmd` y `AnalyzeCmd` son package-private ahí, como en F-CLEX-J001. `generate` crearía
`FJexamJ001JourneyTest` con 2 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: judgingengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultJudgeRuntime
            _change: add
            visibility: public
            implements: [JudgeRuntime]
            handwrittenTests:
              - name: "should open the sessions of a judge with budget 0 when the run asks no budget for it, even when its version passed the exam"
                traceability: { feature: FEAT-JEXAM, rule: F-JEXAM-R010 }
      - name: quizinstructionengine
        _change: modify
        visibility: public
        implementations:
          - name: QuizInstructionAnalyzer
            _change: modify
            handwrittenTests:
              - name: "should look up each quiz under the same fingerprint the ledger recorded for it before FEAT-JEXAM"
                traceability: { feature: FEAT-JEXAM, rule: F-JEXAM-R007 }
features:
  - id: FEAT-JEXAM
    code: F-JEXAM
    journeys:
      - { id: F-JEXAM-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
