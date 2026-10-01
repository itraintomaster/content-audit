---
patch: FEAT-BANDEJA
requirement: 2026-09-30.25_la-bandeja
generated: 2026-10-01T03:00:00Z
---

# Tech Spec: La bandeja (FEAT-BANDEJA)

Paquete E, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La bandeja no decide ni cuenta: arma lo que la
tanda necesita de José, en el orden alineado con F, y convierte cada respuesta en un ajuste de la tanda por su único
puerto de entrada (`adjust`). La muestra ciega es del paquete F (`BlindSampleService`, `BlindPairView`, `PairAnswer` de
FEAT-CIEGA): la bandeja la presenta y le pasa las respuestas. El tablero la dibuja, no la arma (F-HALL-R010). Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. El paquete `tray` suma `audit-infrastructure` a sus clientes y el módulo lleva la lista entera de patrones: ver la última sección.

## Armar una vista con el orden de F
José eligió el 30/9 que la muestra ciega la responda él antes de ver la tanda (la A de DOUBT-QUIEN-CIEGA): por eso,
mientras está abierta, la vista trae sólo sus pares, tal como F los entrega —sin lado, sin motivo, sin autor—, y nada
más de la tanda (R001). Cerrada, trae las decisiones —desempates y sin ganador— por gravedad y al final las muestras de
cada regla. Cada cosa dice qué se pregunta y qué respuestas acepta: una respuesta fuera de ésas se rechaza con su
mensaje. La vista dice además si se puede publicar y, si no, por qué.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: tray
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: Tray
            _change: add
            type: record
            fields:
              - { name: batchId, type: String }
              - { name: blindSampleOpen, type: boolean }
              - { name: items, type: "List<TrayItem>" }
              - { name: publishable, type: boolean }
              - { name: blockers, type: "List<String>" }
          - name: TrayItem
            _change: add
            type: record
            fields:
              - { name: itemId, type: String }
              - { name: kind, type: TrayItemKind }
              - { name: severity, type: FindingSeverity }
              - { name: knowledgeId, type: String }
              - { name: quizId, type: String }
              - { name: question, type: String }
              - { name: answers, type: "List<TrayAnswer>" }
              - { name: before, type: QuizStudentView }
              - { name: after, type: QuizStudentView }
              - { name: findings, type: "List<Finding>" }
              - { name: reasons, type: "List<String>" }
              - { name: ruleId, type: String }
              - { name: samplePairs, type: "List<TraySamplePair>" }
              - { name: blindSampleId, type: String }
              - { name: blindPairs, type: "List<BlindPairView>" }
          - name: TrayItemKind
            _change: add
            type: enum
            fields:
              - { name: BLIND_SAMPLE }
              - { name: TIE_ON_COURSE }
              - { name: TIE_ON_CANDIDATE }
              - { name: NO_WINNER }
              - { name: RULE_SAMPLE }
          - name: TraySamplePair
            _change: add
            type: record
            fields:
              - { name: changeId, type: String }
              - { name: quizId, type: String }
              - { name: before, type: QuizStudentView }
              - { name: after, type: QuizStudentView }
          - name: UnexpectedTrayAnswerException
            _change: add
            type: exception
            fields:
              - { name: answer, type: String }
              - { name: itemId, type: String }
              - { name: allowed, type: String }
      - name: trayengine
        _change: add
        visibility: internal
        interfaces:
          - name: TrayAssembler
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "assemble(CorrectionBatch batch, List<BlindPairView> blindPairs, boolean blindSampleClosed, CourseEntity course): Tray"
        implementations:
          - name: DefaultTrayAssembler
            _change: add
            implements: [TrayAssembler]
            requiresInject:
              - { name: ruleSampleDrawer, type: RuleSampleDrawer }
              - { name: memory, type: DecisionMemory }
              - { name: quizStudentViewRenderer, type: QuizStudentViewRenderer }
```

## Convertir cada respuesta en un ajuste de la tanda
Las respuestas son un conjunto cerrado por clase de cosa, y cada una tiene un solo efecto sobre la tanda: un desempate
sobre el curso que José llama error abre una tarea en la mesa del tema (R002, F-TAREA-R005); uno sobre un candidato que
«pasa» lo acepta y uno que «no pasa» lo devuelve con un intento menos; un sin ganador se queda con el original, acepta
el candidato como excepción de José que la compuerta lista y no vuelve a contar, o mide y juzga el arreglo que él
escribe como cualquier candidato, con `assessGiven` de la mesa, y si no pasa le vuelven los motivos (R003); una muestra
de regla aprueba, saca un cambio o saca la regla (R004, F-TANDA-R008). Las respuestas de la ciega van a F con sus
cuatro opciones, sin pasar por la tanda (R005). El arreglo de José viaja como un candidato de la mesa, armado por la CLI
desde el enunciado con la notación del curso y su traducción.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: tray
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: TrayAnswer
            _change: add
            type: enum
            fields:
              - { name: IS_ERROR }
              - { name: NOT_ERROR }
              - { name: PASSES }
              - { name: DOES_NOT_PASS }
              - { name: KEEP_ORIGINAL }
              - { name: APPROVE_CANDIDATE }
              - { name: WRITE_FIX }
              - { name: RULE_OK }
              - { name: DROP_CHANGE }
              - { name: DROP_RULE }
              - { name: BLIND_PAIR }
          - name: TrayResponse
            _change: add
            type: record
            fields:
              - { name: itemId, type: String }
              - { name: answer, type: TrayAnswer }
              - { name: pairIndex, type: Integer }
              - { name: pairAnswer, type: PairAnswer }
              - { name: changeId, type: String }
              - { name: fix, type: PanelCandidate }
              - { name: note, type: String }
              - { name: decidedBy, type: String }
        interfaces:
          - name: TrayService
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "view(): Tray"
              - signature: "answer(TrayResponse response): Tray"
              - signature: "publish(String approvedBy, String note): BatchPublication"
```

## Sortear la muestra de cada regla de forma repetible
R004 pide 20 cambios al azar por regla, o todos si son menos. El sorteo usa una semilla que sale del id de la tanda y de
la regla, así que la misma tanda muestra los mismos 20 cada vez que José abre la bandeja, y una regla con 170 cambios de
mayúsculas no muestra otros 20 al volver. Los cambios de una regla sin su muestra aprobada quedan fuera al publicar
(R007).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: trayengine
        _change: add
        visibility: internal
        interfaces:
          - name: RuleSampleDrawer
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "draw(CorrectionBatch batch, String ruleId, int size): List<BatchChange>"
        implementations:
          - name: SeededRuleSampleDrawer
            _change: add
            implements: [RuleSampleDrawer]
```

## Recordar lo decidido por identidad y huella
R006 pide que una decisión sobre un desempate o un sin ganador no vuelva mientras el ejercicio no cambie, en ninguna
tanda. Se guarda con la identidad del hallazgo y la huella del ejercicio como lo ve el alumno, calculada con el mismo
`ContentFingerprinter` del libro de evaluaciones (F-EVCOST-R001): por eso revision-domain pasa a depender de
evaluation-ledger-domain. El archivo es de agregar y vive aparte de tandas e informes, así que borrar o podar análisis
no lo toca; si el ejercicio cambia, cambia la huella y la pregunta puede volver.

```architecture
modules:
  - name: revision-domain
    _change: modify
    dependsOn: [evaluation-ledger-domain]
    packages:
      - name: tray
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: RememberedDecision
            _change: add
            type: record
            fields:
              - { name: identityKey, type: String }
              - { name: marker, type: String }
              - { name: contentFingerprint, type: String }
              - { name: answer, type: TrayAnswer }
              - { name: batchId, type: String }
              - { name: decidedAt, type: Instant }
        interfaces:
          - name: TrayDecisionStore
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "remember(RememberedDecision decision): void"
              - signature: "recall(String identityKey, String marker, String contentFingerprint): Optional<RememberedDecision>"
      - name: trayengine
        _change: add
        visibility: internal
        interfaces:
          - name: DecisionMemory
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "recall(Finding finding, QuizTemplateEntity quiz): Optional<RememberedDecision>"
              - signature: "remember(Finding finding, QuizTemplateEntity quiz, TrayAnswer answer, String batchId): void"
        implementations:
          - name: FingerprintDecisionMemory
            _change: add
            implements: [DecisionMemory]
            requiresInject:
              - { name: decisionStore, type: TrayDecisionStore }
              - { name: contentFingerprinter, type: ContentFingerprinter }
  - name: audit-infrastructure
    _change: modify
    packages:
      - name: traystore
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemTrayDecisionStore
            _change: add
            visibility: public
            implements: [TrayDecisionStore]
```

## Publicar con un solo OK, y sólo desde acá
R008 hace de la bandeja el único lugar desde donde se publica: `publish` mira que la ciega esté cerrada, que las
muestras de regla estén contestadas o sus reglas queden fuera, y le pide a la tanda que publique con esas reglas
excluidas; la tanda vuelve a comparar si algo cambió y exige «pasa». Un desempate o un sin ganador sin responder no
frena (R007): queda como estaba y en el informe como sin decidir. `approve` de una propuesta de la tanda abierta se
rechaza con el mensaje de R008 y no escribe el curso; `reject` de una propuesta de la tanda es sacar sus cambios de ese
ejercicio, así que se delega en la tanda.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: tray
        _change: add
        visibility: public
        allowedClients: [audit-cli, audit-infrastructure]
        models:
          - name: TrayServiceConfig
            _change: add
            type: record
            fields:
              - { name: batchService, type: BatchService }
              - { name: batchStore, type: BatchStore }
              - { name: decisionStore, type: TrayDecisionStore }
              - { name: blindSampleService, type: BlindSampleService }
              - { name: panelRunner, type: PanelRunner }
              - { name: panelRunnerFactory, type: PanelRunnerFactory }
              - { name: quizStudentViewRenderer, type: QuizStudentViewRenderer }
              - { name: contentFingerprinter, type: ContentFingerprinter }
              - { name: courseRepository, type: CourseRepository }
              - { name: coursePath, type: Path }
          - name: ProposalInOpenBatchException
            _change: add
            type: exception
            fields:
              - { name: proposalId, type: String }
              - { name: batchId, type: String }
        interfaces:
          - name: TrayServiceFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "create(TrayServiceConfig config): TrayService"
      - name: trayengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultTrayServiceFactory
            _change: add
            visibility: public
            implements: [TrayServiceFactory]
          - name: DefaultTrayService
            _change: add
            implements: [TrayService]
            requiresInject:
              - { name: batchService, type: BatchService }
              - { name: batchStore, type: BatchStore }
              - { name: blindSampleService, type: BlindSampleService }
              - { name: panelRunner, type: PanelRunner }
              - { name: panelRunnerFactory, type: PanelRunnerFactory }
              - { name: assembler, type: TrayAssembler }
              - { name: memory, type: DecisionMemory }
              - { name: courseRepository, type: CourseRepository }
              - { name: coursePath, type: Path }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: ApproveCmd
            _change: modify
            requiresInject:
              - name: batchStore
                type: BatchStore
                _change: add
          - name: RejectCmd
            _change: modify
            requiresInject:
              - name: batchService
                type: BatchService
                _change: add
```

## Dar a la bandeja su verbo en la CLI
`tray show|answer|publish` sigue el molde de `exam`. `answer` recibe el id de la cosa, la respuesta y, según la clase,
el par de la ciega, el cambio a sacar o el archivo con el arreglo de José; con el convertidor de la notación del curso
lo pasa a un candidato. `publish` es el OK.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: TrayCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "showTray(String format): Integer"
          - signature: "answerTray(String itemId, String answer, Integer pairIndex, String changeId, String fixFile, String note): Integer"
          - signature: "publishTray(String note): Integer"
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: TrayCmd
            _change: add
            implements: [TrayCommand]
            types: [Component]
            externalImplements:
              - "java.util.concurrent.Callable<Integer>"
            requiresInject:
              - { name: trayService, type: TrayService }
              - { name: quizSentenceConverter, type: QuizSentenceConverter }
```

## Registrar FEAT-BANDEJA y probarla sin modelos
La feature se registra para que `feature sync --feature FEAT-BANDEJA` anote J001; su ubicación es del qa-tester. Las
pruebas usan una tanda armada a mano, un servicio de muestra ciega de mentira y la mesa con el proponente guionado: la
bandeja sólo muestra la ciega hasta cerrarla; un «no es error» no vuelve en la tanda siguiente con el mismo ejercicio y
sí vuelve si el ejercicio cambió; la muestra de 20 es la misma en dos lecturas; `approve` de una propuesta de la tanda
se rechaza con el mensaje de R008 y el curso queda idéntico. Ninguna consulta un modelo.

```architecture
features:
  - id: FEAT-BANDEJA
    code: F-BANDEJA
modules:
  - name: revision-domain
    _change: modify
```

## Lo que suma la integración de la ronda 2
El almacén de decisiones de archivos (`FileSystemTrayDecisionStore`, en `audit-infrastructure`) implementa
`TrayDecisionStore`, que vive en `tray`: el paquete suma ese módulo a sus clientes. Y como el apply reemplaza la lista de
patrones de `revision-domain` en vez de sumarla, este parche, el último que la toca, lleva las seis.

```architecture
modules:
  - name: revision-domain
    _change: modify
```

## Mostrar lo que hay que regrabar
Lo que una tanda pide regrabar, y lo que quedó pendiente de las publicadas (F-TANDA-R012), es trabajo de José fuera de
content-audit: la bandeja lo muestra al final, sin pregunta, y no frena la publicación (R009, R007). Una clase más de cosa
y el pedido en la cosa alcanzan; el pedido es el de la compuerta (`AudioRerecordRequest`, FEAT-NOEMP), con la clave del
audio y el texto.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: tray
        _change: add
        models:
          - name: TrayItemKind
            _change: add
            type: enum
            fields:
              - { name: BLIND_SAMPLE }
              - { name: TIE_ON_COURSE }
              - { name: TIE_ON_CANDIDATE }
              - { name: NO_WINNER }
              - { name: RULE_SAMPLE }
              - { name: PENDING_RERECORDING }
          - name: TrayItem
            _change: add
            fields:
              - { name: rerecord, type: AudioRerecordRequest }
```

## Declarar las 42 pruebas y ubicar el journey
Las 42 son nuevas y cubren las nueve reglas (R001 6, R002 5, R003 5, R004 5, R005 3, R006 5, R007 4, R008 6, R009 3).
R009 es nueva (orquestador, 1/10): la bandeja muestra lo que hay que regrabar. Van sobre
el armado de la vista (`DefaultTrayAssembler`, 13), el servicio (`DefaultTrayService`, 19), el sorteo (2), la memoria de
lo decidido (2, con su almacén de archivos), `TrayCmd` (3) y lo que ya existía: `ApproveCmd` y `RejectCmd` (3). Ninguna
consulta un modelo: una tanda armada a mano, un servicio de muestra ciega de mentira y la mesa con el proponente guionado.

**De dónde sale cada prueba.** La ciega primero y sola, con los pares como los da F; un desempate de traducción sobre un
ejercicio del curso que José llama «no es error» y no vuelve en la tanda siguiente con el mismo ejercicio, y sí vuelve si
el ejercicio cambió; un sin ganador con el original, el candidato que menos falló y los motivos de sus tres intentos, que
José aprueba como excepción suya o arregla él; la muestra de 20 de una regla de 170 cambios —las mayúsculas de A1 y A2
son 170 de 186 en `escribir/`—, la misma en dos lecturas; publicar con lo no decidido afuera y sin publicar sin la ciega
cerrada; y `approve` de una propuesta de la tanda abierta, rechazado con el mensaje de R008 y el curso idéntico.

**El journey** va a `audit-cli`, en `com.learney.contentaudit.auditcli.commands`: sus pasos son `tray show`,
`tray answer` (la ciega, las decisiones y las muestras) y `tray publish`, y `TrayCmd` es package-private ahí, como
`ExamCmd` en F-JEXAM-J001. `generate` crearía `FBandejaJ001JourneyTest` con 2 caminos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: trayengine
        _change: add
        implementations:
          - name: DefaultTrayAssembler
            _change: add
            handwrittenTests:
              - name: "should show only the blind sample, and nothing else of the batch, while the blind sample is not closed"
                traceability: { feature: FEAT-BANDEJA, rule: F-BANDEJA-R001 }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: ApproveCmd
            _change: modify
            handwrittenTests:
              - name: "should reject 'approve proposal' of a proposal of the open batch with «La propuesta <id> es de la tanda <id>: entra al curso al publicar la tanda.» and exit non-zero, leaving the course identical"
                traceability: { feature: FEAT-BANDEJA, rule: F-BANDEJA-R008 }
features:
  - id: FEAT-BANDEJA
    code: F-BANDEJA
    journeys:
      - { id: F-BANDEJA-J001, testModule: audit-cli, testPackage: com.learney.contentaudit.auditcli.commands }
```
