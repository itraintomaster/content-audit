---
patch: ARCH-001
requirement: 2026-07-29.02_validacion-de-consigna-de-quiz
generated: 2026-07-30T00:00:00Z
---

# Tech Spec: Analisis de cumplimiento de consigna (FEAT-QINST) sobre evaluaciones costosas reutilizables (FEAT-EVCOST)

Este patch cubre dos requerimientos que no se pueden disenar por separado: FEAT-EVCOST
define la capacidad general de "resultado caro, reutilizable y reanudable" y FEAT-QINST es
su primer consumidor. Se propone un solo patch porque el consumidor declara dependencias de
modulo sobre la capacidad, y un patch que las declarara sin traer la capacidad no validaria.

Existe un **segundo patch, separado**, en
`requirements/2026-07-29.03_evaluaciones-costosas-reutilizables/`: consolida los dos lanzadores
de grafos duplicados de `revision-infrastructure` contra el modulo compartido que este patch
introduce. Se aplica preferentemente **despues** de este, aunque los dos ordenes dan el mismo
resultado.

## Resolver DOUBT-QUIZ-COMPLETO por la Opcion A: el ejercicio viaja completo en la representacion auditable

Se elige la **Opcion A**. La Opcion B —leer el contenido completo desde la fuente del curso
al momento de evaluar— obliga a demostrar que esa segunda lectura devuelve el mismo estado
que audita la corrida; si no lo devuelve, la huella certifica un contenido que la auditoria
nunca vio, que es exactamente el error silencioso que F-QINST-R009 existe para evitar. La
Opcion A elimina la pregunta: hay una sola lectura del curso y el contenido juzgado sale del
mismo grafo de objetos que recorren todos los analizadores.

El campo se copia **verbatim** desde `QuizTemplateEntity.form.sentenceParts` en el mismo paso
del mapper. Nunca se re-deriva parseando el DSL de `quizSentence`, porque
`QuizSentenceParser.buildForm()` fabrica un `FormEntity` vacio y la huella terminaria
calculada sobre contenido mutilado. El costo aceptado es explicito: los informes de auditoria
pesan hoy ~117 MB y las partes con sus opciones agregan del orden de +15%, que pagan todos
los analizadores aunque solo este las use. Se acepta porque el sobrecosto es de almacenamiento
—no de tiempo de auditoria ni de dinero— y porque cualquier evaluador de juicio futuro va a
necesitar lo mismo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - { name: instructions, type: String, _change: add }
          - { name: sentenceParts, type: "List<SentencePartEntity>", _change: add }
      - name: AuditableKnowledge
        _change: modify
        fields:
          - { name: topicName, type: String, _change: add }
```

## Crear el registro de resultados como estado propio, con identidad por huella

El registro es un modulo de dominio sin dependencias porque no es de la auditoria: es una
capacidad que cualquier evaluador de juicio va a consumir. La identidad es
`(evaluador, version, huella)` y el identificador del elemento del curso queda **fuera** de
ella, en `subjectRef`, como dato informativo: corregir un ejercicio no cambia su
identificador, asi que una identidad por id devolveria para siempre el juicio de la version
anterior. `history()` existe porque F-EVCOST-R006 pide que los resultados de versiones
anteriores queden consultables — comparar como juzgo el mismo contenido cada version es la
unica forma de saber si un cambio del evaluador lo mejoro.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: add
    layer: domain
    dependsOn: []
    models:
      - name: EvaluationKey
        _change: add
        type: record
        fields:
          - { name: evaluatorId, type: String }
          - { name: evaluatorVersion, type: String }
          - { name: contentFingerprint, type: String }
      - name: EvaluationRecord
        _change: add
        type: record
        fields:
          - { name: key, type: EvaluationKey }
          - { name: payload, type: String }
          - { name: subjectRef, type: String }
          - { name: recordedAt, type: Instant }
    interfaces:
      - name: EvaluationLedger
        _change: add
        stereotype: port
        exposes:
          - signature: "find(EvaluationKey key): Optional<EvaluationRecord>"
          - signature: "append(EvaluationRecord record): void"
          - signature: "history(String evaluatorId,String contentFingerprint): List<EvaluationRecord>"
```

## Hacer estructural, y no disciplinar, la regla de la huella

F-EVCOST-R002 tiene dos mitades y las dos se rompen por descuido: un dato juzgado fuera de la
huella produce reuso silencioso de un resultado obsoleto, y un dato ajeno dentro de la huella
invalida resultados vigentes. Si el consumidor calculara la huella por su cuenta, ambas
mitades quedarian a cargo de que nadie se olvide de nada.

Por eso el consumidor **no** calcula la huella. Entrega un `EvaluationSubject` cuyo `content`
es el mapa exacto de entradas del evaluador, y la sesion lo usa dos veces: lo canonicaliza
para derivar la huella y se lo pasa tal cual al `Evaluator`. Una sola representacion, dos
usos: agregar un dato al juicio sin agregarlo a la huella deja de ser posible. `subjectRef`
viaja aparte precisamente para que no pueda contaminar la identidad.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationSubject
        _change: add
        type: record
        fields:
          - { name: subjectRef, type: String }
          - { name: content, type: "Map<String,String>" }
    interfaces:
      - name: ContentFingerprinter
        _change: add
        stereotype: port
        exposes:
          - signature: "fingerprint(Map<String,String> content): String"
    packages:
      - name: contentfingerprint
        _change: add
        visibility: public
        implementations:
          - name: Sha256ContentFingerprinter
            _change: add
            visibility: public
            implements: [ContentFingerprinter]
```

## Cerrar el conjunto de desenlaces del evaluador en dos casos

La distincion entre "el evaluador se pronuncio" y "el evaluador no llego a pronunciarse" es
la mas facil de confundir y la mas cara de equivocar: registrar una falla transitoria congela
un problema de infraestructura como si fuera un juicio sobre el contenido, y nadie lo vuelve
a intentar. En vez de un booleano o un payload nulable, el evaluador devuelve uno de dos
tipos. Solo `EvaluationEmitted` tiene payload, asi que "registrar una falla como si fuera
resultado" no es una decision que se pueda tomar mal: es codigo que no compila.

`EvaluationFailureKind` separa ademas la falla de UN contenido de la falla del SERVICIO. Es
lo que permite que el veredicto de infraestructura del juez (F-QINST-R013) cuente como
fallido y la corrida siga, mientras que un servicio caido o un credito agotado
(F-QINST-R007) corte las consultas sin abortar la auditoria.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationEmitted
        _change: add
        type: record
        implements: [EvaluationOutcome]
        fields:
          - { name: payload, type: String }
      - name: EvaluationNotEmitted
        _change: add
        type: record
        implements: [EvaluationOutcome]
        fields:
          - { name: kind, type: EvaluationFailureKind }
          - { name: reason, type: String }
      - name: EvaluationFailureKind
        _change: add
        type: enum
        fields:
          - { name: OUTPUT_UNUSABLE }
          - { name: EVALUATOR_UNAVAILABLE }
    interfaces:
      - name: EvaluationOutcome
        _change: add
        stereotype: port
        exposes: []
      - name: Evaluator
        _change: add
        stereotype: port
        exposes:
          - signature: "evaluatorId(): String"
          - signature: "evaluatorVersion(): String"
          - signature: "evaluate(EvaluationSubject subject): EvaluationOutcome"
```

## Extraer la coordinacion reanudable a una sesion por corrida

Esta es la pieza que el segundo consumidor no deberia tener que reescribir: consultar el
resultado vigente, evaluar solo si falta y queda presupuesto, registrar apenas se obtiene,
cortar cuando el servicio se cae y llevar la cuenta de la cobertura. El estado es de la
corrida, no del analizador, asi que vive en la sesion y se abre una por corrida y por
evaluador.

`resolve` y `resolveForced` son dos metodos y no un booleano porque son dos flujos de
ejecucion distintos —uno consulta la vigencia y el otro la saltea— y cada uno tiene que poder
trazarse y testearse por separado. El presupuesto viaja en `EvaluationRunPolicy` pero su
valor y su defecto los pone el consumidor: con DOUBT-PRESUPUESTO-GENERAL todavia en Opcion A,
la capacidad aporta el mecanismo y no la politica.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationRunPolicy
        _change: add
        type: record
        fields:
          - { name: maxNewEvaluations, type: int }
          - { name: reevaluate, type: boolean }
          - { name: reevaluationScope, type: String }
      - name: EvaluationResolution
        _change: add
        type: record
        fields:
          - { name: kind, type: EvaluationResolutionKind }
          - { name: payload, type: String }
      - name: EvaluationResolutionKind
        _change: add
        type: enum
        fields:
          - { name: REUSED }
          - { name: EVALUATED }
          - { name: PENDING }
          - { name: FAILED }
      - name: EvaluationCoverage
        _change: add
        type: record
        fields:
          - { name: reached, type: int }
          - { name: withResult, type: int }
          - { name: evaluatedInRun, type: int }
          - { name: reused, type: int }
          - { name: pending, type: int }
          - { name: failed, type: int }
    interfaces:
      - name: EvaluationSession
        _change: add
        stereotype: port
        exposes:
          - signature: "resolve(EvaluationSubject subject): EvaluationResolution"
          - signature: "resolveForced(EvaluationSubject subject): EvaluationResolution"
          - signature: "coverage(): EvaluationCoverage"
      - name: EvaluationSessionFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "open(EvaluationRunPolicy policy,Evaluator evaluator): EvaluationSession"
    packages:
      - name: evaluationsession
        _change: add
        visibility: public
        implementations:
          - name: DefaultEvaluationSessionFactory
            _change: add
            visibility: public
            implements: [EvaluationSessionFactory]
          - name: DefaultEvaluationSession
            _change: add
            implements: [EvaluationSession]
```

## Persistir el registro como bitacora de una entrada por linea

"Cada resultado disponible apenas se obtiene" y "se lee completo o no existe" son la misma
decision de formato. Un archivo por evaluador, una entrada por linea, escritura en modo
append: una interrupcion abrupta a lo sumo deja la ultima linea incompleta, y la lectura la
descarta por no parsear. Una entrada truncada se comporta como si nunca se hubiera intentado
registrar, que es lo que R004 exige.

Append-only resuelve gratis otras dos reglas: las entradas de versiones anteriores del
evaluador conviven como historia y no se borran solas (R006), y una re-evaluacion agrega sin
destruir la anterior (R008). El modulo es hermano de audit-infrastructure y no parte de el
porque el registro no pertenece a ningun informe: borrar informes no debe tocarlo (R007).

```architecture
modules:
  - name: evaluation-ledger-infrastructure
    _change: add
    layer: infrastructure
    dependsOn: [evaluation-ledger-domain]
    uses: [jackson-databind]
    implementations:
      - name: FileSystemEvaluationLedger
        _change: add
        visibility: public
        implements: [EvaluationLedger]
        requiresInject:
          - { name: baseDir, type: Path }
```

## Extraer el runtime de agentes declarativos antes de duplicarlo por tercera vez

El lanzamiento in-process de un grafo de sentinel-agents ya esta escrito dos veces identico
dentro de revision-infrastructure (`AgentRuntimeLauncher` en lemmaabsenceagent y
`KnowledgeTitleAgentRuntimeLauncher` en knowledgetitleagent). El juez de consigna seria la
tercera copia y ademas no puede reusar ninguna de las dos: revision-infrastructure declara
`allowedClients=[audit-cli]` y cuelga de revision-domain, mientras que este consumidor cuelga
de la auditoria.

Se extrae **solo el lanzador**, que es la pieza genuinamente neutra. La clasificacion de
errores se queda donde esta: cada consumidor la mapea a su propia taxonomia de fallas
(`LlmGenerationFailureCategory` en revision, `EvaluationFailureKind` aca) y compartirla
obligaria a arrastrar tipos ajenos. `allowedClients` incluye ya a `revision-infrastructure`
aunque este patch no la migre: la migracion es un patch aparte y **los dos declaran la misma
lista de tres clientes a proposito**, para que el resultado no dependa de cual se aplique
primero.

```architecture
modules:
  - name: agent-runtime-infrastructure
    _change: add
    layer: infrastructure
    dependsOn: []
    uses: [sentinel-agents, langchain4j-core]
    allowedClients: [quiz-instruction-infrastructure, audit-cli, revision-infrastructure]
    models:
      - name: AgentGraphRunnerConfig
        _change: add
        type: record
        fields:
          - { name: agentsBaseDir, type: Path }
          - { name: runsBaseDir, type: Path }
    interfaces:
      - name: AgentGraphRunner
        _change: add
        stereotype: port
        exposes:
          - signature: "run(String agentName,Map<String,String> inputs,ChatModel chatModel,Map<String,Object> tools): RunState"
      - name: AgentGraphRunnerFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "create(AgentGraphRunnerConfig config): AgentGraphRunner"
    packages:
      - name: graphexecution
        _change: add
        visibility: public
        implementations:
          - name: DefaultAgentGraphRunnerFactory
            _change: add
            visibility: public
            implements: [AgentGraphRunnerFactory]
          - name: DefaultAgentGraphRunner
            _change: add
            implements: [AgentGraphRunner]
```

## Envolver al juez detras del SPI Evaluator, con la traduccion de fallas como contrato

El juez ya existe y esta probado; lo que falta es un adaptador que lo presente como
`Evaluator`. El adaptador tiene tres responsabilidades que son contrato y no detalle:
traduce `EvaluationSubject.content` a las cinco entradas declaradas del agente sin agregar ni
quitar nada; reconoce el veredicto conservador de infraestructura del juez —confianza nula y
violacion `JUDGE_OUTPUT_INVALID`— y lo devuelve como `EvaluationNotEmitted(OUTPUT_UNUSABLE)`,
de modo que nunca llegue al registro; y convierte toda falla de servicio, timeout,
autenticacion o credito en `EvaluationNotEmitted(EVALUATOR_UNAVAILABLE)` en lugar de
propagarla como excepcion. Esa ultima es la que hace que la auditoria termine igual.

`QuizInstructionJudgeVersionResolver` deriva la version del digest de la definicion del
agente mas el modelo: retocar el prompt cambia la version sola, que es justo lo que R010
quiere —invalidar vigencia sin borrar ni disparar re-evaluacion—.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: add
    layer: infrastructure
    dependsOn: [audit-domain, evaluation-ledger-domain, agent-runtime-infrastructure]
    uses: [jackson-databind, langchain4j-core, langchain4j-openai, sentinel-agents]
    allowedClients: [audit-cli]
    models:
      - name: QuizInstructionJudgeConfig
        _change: add
        type: record
        fields:
          - { name: baseUrl, type: String }
          - { name: apiKey, type: String }
          - { name: modelName, type: String }
          - { name: temperature, type: Double }
          - { name: timeoutSeconds, type: Integer }
          - { name: agentName, type: String }
          - { name: judgeVersionOverride, type: String }
    interfaces:
      - name: QuizInstructionJudgeFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "create(QuizInstructionJudgeConfig config,AgentGraphRunner agentGraphRunner): Evaluator"
    packages:
      - name: instructionjudge
        _change: add
        visibility: public
        interfaces:
          - name: QuizInstructionJudgeVersionResolver
            _change: add
            visibility: internal
            exposes:
              - signature: "resolve(QuizInstructionJudgeConfig config): String"
        implementations:
          - name: DefaultQuizInstructionJudgeFactory
            _change: add
            visibility: public
            implements: [QuizInstructionJudgeFactory]
          - name: QuizInstructionAgentJudge
            _change: add
            implements: [Evaluator]
          - name: DefaultQuizInstructionJudgeVersionResolver
            _change: add
            implements: [QuizInstructionJudgeVersionResolver]
```

## Separar la lectura del payload del adaptador que lo produce

El 100% de los veredictos reutilizados llegan desde el registro sin pasar nunca por el juez.
Si la interpretacion del payload viviera dentro del adaptador del juez, la ruta barata —la
que la feature entera existe para privilegiar— dependeria del modulo caro. Por eso el puerto
de lectura es del dominio y el adaptador Jackson vive en un paquete publico del modulo de
infraestructura, alcanzable por la composition root sin abrir nada mas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizInstructionVerdictReader
        _change: add
        stereotype: port
        exposes:
          - signature: "read(String payload): QuizInstructionVerdict"
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: instructionverdict
        _change: add
        visibility: public
        implementations:
          - name: JacksonQuizInstructionVerdictReader
            _change: add
            visibility: public
            implements: [QuizInstructionVerdictReader]
```

## Modelar el veredicto tipado y su diagnostico por ejercicio

El diagnostico no repite el veredicto: lo envuelve y le agrega el puntaje que se derivo de el
y si se reutilizo. Se evita asi tener dos records casi iguales que se desincronizan a la
primera modificacion del esquema del juez.

`QuizInstructionVerdict` **no** lleva el identificador del ejercicio, aunque el esquema del
juez lo declare: el veredicto describe contenido y no elemento, y dos ejercicios de contenido
identico comparten uno solo. Ese campo es tambien la razon por la que el contenido entregado
al juez se despoja de identificadores — si viajara el id, entraria en la huella por R002 y
contradiria R001. La trazabilidad al ejercicio no se pierde: vive en
`EvaluationRecord.subjectRef`, que es informativo y esta fuera de la identidad.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: add
        visibility: public
        models:
          - name: InstructionSeverity
            _change: add
            type: enum
            fields:
              - { name: NONE }
              - { name: MINOR }
              - { name: MAJOR }
              - { name: CRITICAL }
          - name: InstructionCheckStatus
            _change: add
            type: enum
            fields:
              - { name: PASSED }
              - { name: FAILED }
              - { name: NOT_APPLICABLE }
          - name: InstructionViolation
            _change: add
            type: record
            fields:
              - { name: code, type: String }
              - { name: constraint, type: String }
              - { name: evidence, type: String }
              - { name: explanation, type: String }
          - name: InstructionCheck
            _change: add
            type: record
            fields:
              - { name: category, type: String }
              - { name: constraint, type: String }
              - { name: status, type: InstructionCheckStatus }
              - { name: evidence, type: String }
          - name: QuizInstructionVerdict
            _change: add
            type: record
            fields:
              - { name: followsInstructions, type: boolean }
              - { name: confidence, type: double }
              - { name: severity, type: InstructionSeverity }
              - { name: reason, type: String }
              - { name: violations, type: "List<InstructionViolation>" }
              - { name: checkedConstraints, type: "List<InstructionCheck>" }
          - name: QuizInstructionDiagnosis
            _change: add
            type: record
            fields:
              - { name: verdict, type: QuizInstructionVerdict }
              - { name: score, type: double }
              - { name: reusedFromPreviousRun, type: boolean }
```

## Colgar el diagnostico y la cobertura de los contenedores tipados que ya existen

El diagnostico por ejercicio se suma a `QuizDiagnoses`, junto a los de longitud y lemas, sin
inventar un canal nuevo. La cobertura se suma a `CourseDiagnoses` porque es un dato de la
corrida entera y no de un nodo: sin ella el puntaje es directamente enganoso —un 0,95 sobre
40 ejercicios de 11.500 se lee igual que un 0,95 sobre el curso—. Se guarda junto a la
version del juez para que la cobertura siga siendo interpretable despues de cambiarlo.

Nada hay que tocar en la agregacion: `IScoreAggregator` ya promedia solo los puntajes
presentes, asi que "no puntuar lo no evaluado" funciona sin modificaciones.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizDiagnoses
        _change: modify
        exposes:
          - signature: "getQuizInstructionDiagnosis(): Optional<QuizInstructionDiagnosis>"
            _change: add
      - name: CourseDiagnoses
        _change: modify
        exposes:
          - signature: "getQuizInstructionCoverage(): Optional<QuizInstructionCoverageDiagnosis>"
            _change: add
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionCoverageDiagnosis
            _change: add
            type: record
            fields:
              - { name: coverage, type: EvaluationCoverage }
              - { name: evaluatorVersion, type: String }
```

## Abrir un seam de analizadores construidos por corrida

Todos los analizadores actuales son singletons construidos una vez en la composition root,
antes de que picocli parsee los argumentos. Este no puede serlo: el presupuesto, el pedido de
re-evaluacion y el recuento de cobertura son estado de la corrida. El seam generico
`EvaluationAnalyzerFactory` resuelve eso sin que el runner conozca ningun evaluador concreto
—recibe una lista de factories y busca la politica por `evaluatorId`—, de modo que el segundo
evaluador costoso se enchufa agregando una factory.

`AuditRunRequest` reemplaza al `Set<String>` suelto y agrega la exclusion explicita. Las
politicas van en un mapa por evaluador y no en campos con nombre, por la misma razon: que el
segundo consumidor no obligue a cambiar el record ni el runner.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: EvaluationAnalyzerFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "evaluatorId(): String"
          - signature: "create(EvaluationRunPolicy policy): ContentAnalyzer"
  - name: audit-application
    _change: modify
    dependsOn: [audit-domain, course-domain, refiner-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, revision-domain, evaluation-ledger-domain]
    models:
      - name: AuditRunRequest
        _change: add
        type: record
        fields:
          - { name: includedAnalyzers, type: "Set<String>" }
          - { name: excludedAnalyzers, type: "Set<String>" }
          - { name: evaluationPolicies, type: "Map<String,EvaluationRunPolicy>" }
    interfaces:
      - name: AuditRunner
        _change: modify
        exposes:
          - signature: "runAudit(Path coursePath,AuditRunRequest request): AuditReport"
            _change: add
    implementations:
      - name: DefaultAuditRunner
        _change: modify
        requiresInject:
          - { name: evaluationAnalyzerFactories, type: "List<EvaluationAnalyzerFactory>" }
```

## Aislar el motor del analizador detras de su factory

El motor tiene tres piezas separadas porque tienen tres razones de cambio distintas y tres
conjuntos de pruebas distintos: armar el contenido juzgado, traducir veredicto a puntaje y
decidir el alcance de una re-evaluacion. Solo la factory es publica.

`QuizInstructionSubjectBuilder` es la pieza critica: su contrato es que el mapa que produce
contiene exactamente las cinco entradas del juez y **ningun** identificador. Sin
identificadores porque la identidad es el contenido; con todo lo demas porque un dato juzgado
fuera del mapa produce reuso silencioso de un veredicto obsoleto.
`QuizInstructionScorer` concentra la escala y tambien las dos combinaciones inconsistentes
del veredicto, resueltas siempre por la lectura mas conservadora: dar por bueno un ejercicio
malo lo manda al alumno, mientras que un falso positivo solo cuesta una revision humana.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstructionengine
        _change: add
        visibility: public
        interfaces:
          - name: QuizInstructionSubjectBuilder
            _change: add
            visibility: internal
            exposes:
              - signature: "build(AuditNode quizNode): EvaluationSubject"
          - name: QuizInstructionScorer
            _change: add
            visibility: internal
            exposes:
              - signature: "score(QuizInstructionVerdict verdict): double"
          - name: QuizInstructionScopeMatcher
            _change: add
            visibility: internal
            exposes:
              - signature: "matches(AuditNode quizNode,String reevaluationScope): boolean"
        implementations:
          - name: DefaultQuizInstructionAnalyzerFactory
            _change: add
            visibility: public
            implements: [EvaluationAnalyzerFactory]
          - name: QuizInstructionAnalyzer
            _change: add
            implements: [ContentAnalyzer]
          - name: DefaultQuizInstructionSubjectBuilder
            _change: add
            implements: [QuizInstructionSubjectBuilder]
          - name: DefaultQuizInstructionScorer
            _change: add
            implements: [QuizInstructionScorer]
          - name: DefaultQuizInstructionScopeMatcher
            _change: add
            implements: [QuizInstructionScopeMatcher]
```

## Alojar las dudas abiertas en una configuracion inspeccionable

El defecto del presupuesto (500 por corrida) y la escala de puntaje (1,0 / 0,6 / 0,3 / 0,0)
son las dos decisiones que el requerimiento deja marcadas como ASSUMPTION y pide revisar con
datos reales. Ponerlas detras de un puerto de configuracion —el mismo patron que
`SentenceLengthConfig`— permite ajustarlas sin tocar el analizador y las expone en
`content-audit config analyzer quiz-instruction`, que es donde el usuario va a mirar cuando
quiera saber por que un ejercicio saco 0,6.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizInstructionConfig
        _change: add
        stereotype: port
        exposes:
          - signature: "getDefaultMaxNewEvaluations(): int"
          - signature: "getScoreFor(InstructionSeverity severity): double"
        extends:
          - SelfDescribingConfig
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultQuizInstructionConfig
        _change: add
        visibility: public
        implements: [QuizInstructionConfig]
```

## Agregar la superficie de CLI sin romper la firma existente

`analyze` ya tenia siete parametros posicionales y hacen falta tres opciones nuevas:
exclusion, presupuesto y re-evaluacion. En vez de reescribir la firma —lo que obligaria a
tocar todos los tests que la ejercitan— se agrega una sobrecarga con un record de opciones y
la firma vieja queda viva delegando con defectos. Es el mismo recurso que ya usa `GetCommand`
con sus dos filtros.

El resolutor de configuracion del juez sigue el patron de `LagenConfigResolver` y con una
regla explicita: una configuracion ausente no rompe la auditoria, se resuelve a una config
incompleta y el adaptador la reporta como evaluador no disponible. El formateador detallado
existe porque la cobertura y las violaciones con evidencia no se leen en una tabla de
puntajes.

```architecture
modules:
  - name: audit-cli
    _change: modify
    dependsOn: [audit-application, audit-domain, course-domain, course-infrastructure, nlp-infrastructure, vocabulary-infrastructure, audit-infrastructure, refiner-domain, revision-domain, revision-infrastructure, evaluation-ledger-domain, evaluation-ledger-infrastructure, agent-runtime-infrastructure, quiz-instruction-infrastructure]
    models:
      - name: AnalyzeOptions
        _change: add
        type: record
        fields:
          - { name: format, type: String }
          - { name: level, type: String }
          - { name: topic, type: String }
          - { name: knowledge, type: String }
          - { name: analyzers, type: "List<String>" }
          - { name: excludeAnalyzers, type: "List<String>" }
          - { name: detailed, type: boolean }
          - { name: instructionBudget, type: Integer }
          - { name: reevaluateInstructions, type: String }
    interfaces:
      - name: AnalyzeCommand
        _change: modify
        exposes:
          - signature: "analyze(String coursePath,AnalyzeOptions options): Integer"
            _change: add
    packages:
      - name: bootstrap
        _change: modify
        interfaces:
          - name: QuizInstructionJudgeConfigResolver
            _change: add
            visibility: internal
            exposes:
              - signature: "resolve(): QuizInstructionJudgeConfig"
        implementations:
          - name: DefaultQuizInstructionJudgeConfigResolver
            _change: add
            implements: [QuizInstructionJudgeConfigResolver]
      - name: formatting
        _change: modify
        implementations:
          - name: QuizInstructionDetailedFormatter
            _change: add
            implements: [DetailedFormatter]
```
