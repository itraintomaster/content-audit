---
patch: FEAT-MESA
requirement: 2026-09-30.22_la-mesa-por-tema
generated: 2026-10-01T03:00:00Z
---

# Tech Spec: La mesa por tema (FEAT-MESA)

Paquete E, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La mesa no trae jueces ni reglas propias: usa los
analizadores del catálogo tal como corren en `analyze`, el libro de evaluaciones para no pagar dos veces, la compuerta
de FEAT-NOEMP y la vista de tarea de FEAT-TAREA. Lo nuevo es el lazo por tema —revalidar, proponer, medir, juzgar,
decidir—, el cupo compartido de consultas y el puerto de quien propone. Depende de tres piezas de la ronda 1 que todavía
no están en el modelo (el renderizador A-01, el semáforo de D y lo que dice el audio de A): ver `ganchos-E.md`. Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. Lleva la lista entera de patrones de `revision-domain`: ver la última sección. Lo que dice el audio lo construye la pieza de A que FEAT-RONDA2 abre (`SpokenTextBuilder`).

## Darle a la mesa el tema entero, como está en la tanda
Una mesa toma un tema y no un ejercicio porque las tres tareas del ejercicio 11 de *Afirmativas regulares* son un solo
candidato (R001) y porque quien propone tiene que ver el tema como lo ve el alumno, con lo ya aceptado (R003). El tema
viaja como la entidad del curso con los cambios de la tanda ya aplicados, más sus tareas con su vista de FEAT-TAREA, el
semáforo de D y, si José lo pidió, el encargo de pasarlo a opción múltiple (R015). Los intentos ya gastados y los
motivos viajan con el tema para que un cambio que vuelve de la compuerta de la tanda gaste uno menos (F-NOEMP-R008).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        models:
          - name: TopicWork
            _change: add
            type: record
            fields:
              - { name: batchId, type: String }
              - { name: level, type: CefrLevel }
              - { name: milestoneId, type: String }
              - { name: topicId, type: String }
              - { name: knowledge, type: KnowledgeEntity }
              - { name: sentenceMode, type: SentenceMode }
              - { name: briefs, type: "List<ErrorTaskBrief>" }
              - { name: wordLights, type: TopicWordLights }
              - { name: conversion, type: ConversionOrder }
              - { name: attemptsUsed, type: "Map<String,Integer>" }
              - { name: carriedRejections, type: "List<CandidateRejection>" }
          - name: ConversionOrder
            _change: add
            type: record
            fields:
              - { name: knowledgeId, type: String }
              - { name: targetKind, type: FormKind }
              - { name: optionCount, type: int }
```

## Pedir candidatos del ejercicio entero a un puerto
Quien propone es un puerto porque hay dos: el agente, que consulta un modelo pago, y uno guionado, que devuelve
propuestas escritas de antemano por ejercicio e intento. El guionado es lo que deja probar la mesa entera, y repetir una
corrida grabada, sin pagar una consulta (lo que José pidió: «sin pedido explícito no se paga nada»). El pedido lleva
lo que R003 enumera y nada más: nunca veredictos de un candidato ajeno. `twoFixes` lo prende la declaración de
`translation-sense` (FEAT-TAREA): entonces la respuesta trae uno que cambia sólo la traducción y otro que cambia el
inglés. El candidato es el ejercicio entero como quedaría; la miniteoría y la consigna del tema viajan como texto porque
se aplican a varios ejercicios (R012, R015). Una respuesta no disponible corta el cupo y no gasta intentos (R010 inv. 2).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        models:
          - name: PanelRequest
            _change: add
            type: record
            fields:
              - { name: batchId, type: String }
              - { name: level, type: CefrLevel }
              - { name: knowledgeId, type: String }
              - { name: quizId, type: String }
              - { name: topicView, type: "List<QuizStudentView>" }
              - { name: briefs, type: "List<ErrorTaskBrief>" }
              - { name: wordLights, type: TopicWordLights }
              - { name: conversion, type: ConversionOrder }
              - { name: attempt, type: int }
              - { name: previousRejections, type: "List<CandidateRejection>" }
              - { name: twoFixes, type: boolean }
          - name: PanelCandidate
            _change: add
            type: record
            fields:
              - { name: kind, type: PanelCandidateKind }
              - { name: quiz, type: QuizTemplateEntity }
              - { name: miniTheoryText, type: String }
              - { name: topicInstructions, type: String }
              - { name: rationale, type: String }
          - name: PanelCandidateKind
            _change: add
            type: enum
            fields:
              - { name: WHOLE_EXERCISE }
              - { name: TRANSLATION_ONLY }
              - { name: ENGLISH_CHANGE }
              - { name: MINI_THEORY }
              - { name: MULTIPLE_CHOICE_CONVERSION }
          - name: PanelProposal
            _change: add
            type: record
            fields:
              - { name: candidates, type: "List<PanelCandidate>" }
              - { name: proposerVersion, type: String }
              - { name: available, type: boolean }
              - { name: unavailableReason, type: String }
          - name: CandidateRejection
            _change: add
            type: record
            fields:
              - { name: attempt, type: int }
              - { name: candidateKind, type: PanelCandidateKind }
              - { name: reasons, type: "List<String>" }
        interfaces:
          - name: PanelProposer
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "propose(PanelRequest request): PanelProposal"
        implementations:
          - name: ScriptedPanelProposer
            _change: add
            visibility: public
            implements: [PanelProposer]
            requiresInject:
              - { name: script, type: "Map<String,List<PanelProposal>>" }
```

## Medir con la misma vara del análisis, sobre el tema recortado
R004 pide que el candidato dé los hallazgos que daría en el curso, así que se mide con el motor, el mapeador y los
analizadores del análisis de partida, no con criterios propios (F-QICOR-R015). Se mide un curso recortado al camino
nivel → topic → tema: todo analizador que da hallazgos de ejercicio o de tema los calcula dentro del tema y del nivel
—el largo, las palabras de otro nivel, la pista del 75 %, los duplicados, los jueces—, y el recorte hace que un juez
sólo pueda consultar ejercicios de ese tema. Los números de nivel y de curso de ese recorte no se leen: son del
recálculo de la tanda. Los jueces corren con el libro de evaluaciones (R005): un juez que no lee nada de lo que cambió
tiene la misma huella y reusa su veredicto sin consultar, y el que sí, consulta y deja su veredicto por huella, que se
reusa si el candidato entra al curso. Las palabras rojas salen de la consulta léxica de cada oración y de cada opción
puesta en el hueco, sin las que enseña el tema (D), y lo que se escucha, del constructor de A. Cada candidato tokeniza
sólo sus oraciones nuevas si entra el caché por oración (gancho E-10): un lanzamiento de spaCy, unos 2 s.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        interfaces:
          - name: TopicMeter
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "measure(TopicWork work, KnowledgeEntity knowledgeState, List<String> analyzers, QueryAllowance allowance): UnitMeasurement"
      - name: panelengine
        _change: add
        visibility: internal
        implementations:
          - name: CatalogTopicMeter
            _change: add
            implements: [TopicMeter]
            requiresInject:
              - { name: auditEngine, type: AuditEngine }
              - { name: courseMapper, type: CourseMapper }
              - { name: nlpTokenizer, type: NlpTokenizer }
              - { name: lexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
              - { name: spokenTextBuilder, type: SpokenTextBuilder }
              - { name: sentenceLengthConfig, type: SentenceLengthConfig }
```

## Filtrar antes de pagar y decidir con un script
Lo estructural se mira antes de medir, porque no cuesta consultas: que el candidato cambie sólo lo que declaran sus
tareas (F-TAREA-R006), que una opción múltiple siga siéndolo con una sola correcta y la misma cantidad de opciones
(R011), que la miniteoría llegue a todos los que muestran el texto y a ninguno más (R012) y que un encargo cumpla sus
tres condiciones (R015). Después decide un script sin modelo (R006): acepta si resuelve al menos una tarea y la
compuerta pasa; entre varios, el de menos hallazgos por gravedad, después el que no cambia lo que se escucha, después el
que queda en rango, después el primero. El desacuerdo de los jueces de traducción sobre el candidato lo lee de la regla
que declara `translation-sense` como desempate y lo manda a José sin gastar intentos (R008). Refutan los controles de
siempre: si un juez de otra respuesta o la consulta léxica encuentran un contraejemplo que el original no tenía, la
compuerta lo cuenta como error o palabra roja nueva y rechaza (R013).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panelengine
        _change: add
        visibility: internal
        interfaces:
          - name: CandidateScreen
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "screen(TopicWork work, String quizId, PanelCandidate candidate): List<String>"
          - name: PanelDecisionScript
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "decide(List<ErrorTaskBrief> tasks, UnitMeasurement original, List<ScoredCandidate> candidates): ScriptDecision"
        models:
          - name: ScoredCandidate
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: candidate, type: PanelCandidate }
              - { name: measurement, type: UnitMeasurement }
              - { name: gate, type: ChangeGateVerdict }
              - { name: screenViolations, type: "List<String>" }
          - name: ScriptDecision
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: outcome, type: PanelOutcomeKind }
              - { name: winnerIndex, type: int }
              - { name: resolvedTaskIds, type: "List<String>" }
              - { name: tieFindings, type: "List<Finding>" }
              - { name: reasons, type: "List<String>" }
        implementations:
          - name: DefaultCandidateScreen
            _change: add
            implements: [CandidateScreen]
            requiresInject:
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
          - name: DefaultPanelDecisionScript
            _change: add
            implements: [PanelDecisionScript]
            requiresInject:
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
```

## Llevar cada tema hasta un desenlace y avisarlo apenas se decide
Por tema, la mesa revalida primero con la misma medición (R002): una tarea cuyo hallazgo ya no está termina «ya no se
sostiene» sin candidato. Resuelve antes la miniteoría y el encargo, que cambian lo que ven los demás candidatos del tema
(R012), y después cada ejercicio, con hasta tres intentos (R007). Cada desenlace sale por un sumidero en el momento en
que se decide, y la tanda lo guarda antes de seguir: así una tanda cortada no pierde lo decidido ni paga dos veces
(F-TANDA-R007). Un tipo sin alcance declarado nunca se intenta y queda con ese desenlace (F-TAREA-R006). La mesa no
escribe el curso: el candidato aceptado viaja en el desenlace y la tanda lo guarda como propuesta pendiente (R009).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        models:
          - name: PanelOutcomeKind
            _change: add
            type: enum
            fields:
              - { name: ACCEPTED }
              - { name: NO_LONGER_HOLDS }
              - { name: NO_WINNER }
              - { name: TIE }
              - { name: NOT_ATTEMPTED }
              - { name: NO_DECLARED_SCOPE }
          - name: PanelDecision
            _change: add
            type: record
            fields:
              - { name: knowledgeId, type: String }
              - { name: quizIds, type: "List<String>" }
              - { name: taskIds, type: "List<String>" }
              - { name: outcome, type: PanelOutcomeKind }
              - { name: accepted, type: PanelCandidate }
              - { name: acceptedVerdict, type: ChangeGateVerdict }
              - { name: resolvedTaskIds, type: "List<String>" }
              - { name: attempts, type: int }
              - { name: rejections, type: "List<CandidateRejection>" }
              - { name: bestFailing, type: PanelCandidate }
              - { name: bestFailingVerdict, type: ChangeGateVerdict }
              - { name: tieFindings, type: "List<Finding>" }
              - { name: queriesSpent, type: int }
          - name: PanelTopicResult
            _change: add
            type: record
            fields:
              - { name: knowledgeId, type: String }
              - { name: decisions, type: "List<PanelDecision>" }
              - { name: queriesSpent, type: int }
              - { name: interrupted, type: boolean }
        interfaces:
          - name: PanelDecisionSink
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "accept(PanelDecision decision): void"
      - name: panelengine
        _change: add
        visibility: internal
        interfaces:
          - name: TopicPanel
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "run(TopicWork work, PanelRunSettings settings, QueryAllowance allowance, PanelDecisionSink sink): PanelTopicResult"
              - signature: "assess(TopicWork work, String quizId, PanelCandidate candidate, PanelRunSettings settings, QueryAllowance allowance): PanelDecision"
        implementations:
          - name: DefaultTopicPanel
            _change: add
            implements: [TopicPanel]
            requiresInject:
              - { name: proposer, type: PanelProposer }
              - { name: meter, type: TopicMeter }
              - { name: screen, type: CandidateScreen }
              - { name: script, type: PanelDecisionScript }
              - { name: changeGate, type: ChangeGate }
              - { name: quizStudentViewRenderer, type: QuizStudentViewRenderer }
```

## Correr N temas y C consultas con un cupo compartido
R010 pide dos topes a la vez —temas abiertos y consultas en curso— y el tope total de la tanda (F-TANDA-R003). Un solo
cupo compartido los lleva: cada mesa reserva antes de consultar, a quien propone o a los jueces, y liquida lo gastado
según la cobertura que devuelve el motor; cada corrida de medición pide a los jueces de a una consulta, así que con N
mesas nunca hay más de N en curso, y con C menor que N esperan. Una falla del servicio corta el cupo: no empieza
ninguna consulta más, lo respondido queda en el libro y lo no decidido queda «sin intentar». Como las decisiones salen
sólo de mediciones y veredictos, cambiar N y C cambia el tiempo y no las decisiones (inv. 3). Por omisión N = 4 y C = 4
(José, 30/9); 8 y 8 se piden al abrir la tanda siguiente si A1 corre sin cortes. `assessGiven` mide y juzga un
candidato que no vino de quien propone, como el arreglo que escribe José en la bandeja (F-BANDEJA-R003).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        models:
          - name: PanelRunSettings
            _change: add
            type: record
            fields:
              - { name: topicsAtOnce, type: int }
              - { name: queriesAtOnce, type: int }
              - { name: maxAttempts, type: int }
              - { name: analyzers, type: "List<String>" }
        interfaces:
          - name: QueryAllowance
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "tryReserve(int queries): boolean"
              - signature: "settle(int reserved, int spent): void"
              - signature: "trip(String cause): void"
              - signature: "tripped(): Optional<String>"
              - signature: "spent(): int"
          - name: PanelRunner
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "runTopics(List<TopicWork> topics, PanelRunSettings settings, QueryAllowance allowance, PanelDecisionSink sink): List<PanelTopicResult>"
              - signature: "assessGiven(TopicWork work, String quizId, PanelCandidate candidate, PanelRunSettings settings, QueryAllowance allowance): PanelDecision"
      - name: panelengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultPanelRunner
            _change: add
            implements: [PanelRunner]
            requiresInject:
              - { name: topicPanel, type: TopicPanel }
          - name: CappedQueryAllowance
            _change: add
            implements: [QueryAllowance]
```

## Armar la mesa desde afuera con una fábrica
La raíz de composición sólo ve la fábrica y un registro con los puertos que la mesa necesita: el motor, el mapeador, el
catálogo, las declaraciones de corrección, el renderizador, la consulta léxica, lo que el tema enseña, lo que dice el
audio y el rango de largo. La fábrica arma adentro la compuerta por cambio (FEAT-NOEMP) y los colaboradores de paquete;
también entrega el medidor suelto, que la tanda usa para la compuerta de sus reglas, y el cupo, que la tanda crea con
su tope y su C.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panel
        _change: add
        visibility: public
        allowedClients: [audit-cli, revision-infrastructure]
        models:
          - name: PanelRunnerConfig
            _change: add
            type: record
            fields:
              - { name: proposer, type: PanelProposer }
              - { name: analyzerCatalog, type: AnalyzerCatalog }
              - { name: correctionCatalog, type: AnalyzerCorrectionCatalog }
              - { name: auditEngine, type: AuditEngine }
              - { name: courseMapper, type: CourseMapper }
              - { name: quizStudentViewRenderer, type: QuizStudentViewRenderer }
              - { name: nlpTokenizer, type: NlpTokenizer }
              - { name: lexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: topicTeachingResolver, type: TopicTeachingResolver }
              - { name: spokenTextBuilder, type: SpokenTextBuilder }
              - { name: sentenceLengthConfig, type: SentenceLengthConfig }
        interfaces:
          - name: PanelRunnerFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "create(PanelRunnerConfig config): PanelRunner"
              - signature: "meter(PanelRunnerConfig config): TopicMeter"
              - signature: "allowance(Integer cap, int queriesAtOnce): QueryAllowance"
      - name: panelengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultPanelRunnerFactory
            _change: add
            visibility: public
            implements: [PanelRunnerFactory]
```

## Poner al agente que propone junto a los otros agentes
El agente vive en revision-infrastructure con el molde exacto del de la corrección de consigna —fábrica pública,
lanzador sobre el runtime compartido de sentinel-agents, lector de la salida— porque es otro agente generador con otra
entrada. Recibe la configuración de generación de siempre (`LagenConfig`) y la sesión del proveedor que eligió José
(claude-cli). La definición del agente en `.sentinel/agents/` no es de Sentinel y va en los ganchos. Sólo consulta
cuando la mesa le reservó cupo.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    packages:
      - name: panelagent
        _change: add
        visibility: public
        interfaces:
          - name: PanelAgentProposerFactory
            _change: add
            stereotype: factory
            sealed: false
            visibility: public
            exposes:
              - signature: "create(LagenConfig config): PanelProposer"
              - signature: "providerIdFor(LagenConfig config): String"
          - name: PanelAgentRuntimeLauncher
            _change: add
            sealed: false
            visibility: internal
            exposes:
              - signature: "launch(String agentName, Map<String,String> inputs, ChatModel chatModel): RunState"
          - name: PanelAgentProposalParser
            _change: add
            sealed: false
            visibility: internal
            exposes:
              - signature: "parse(RunState runState, PanelRequest request): PanelProposal"
        implementations:
          - name: DefaultPanelAgentProposerFactory
            _change: add
            visibility: public
            implements: [PanelAgentProposerFactory]
          - name: AgentPanelProposer
            _change: add
            implements: [PanelProposer]
            requiresInject:
              - { name: agentRuntimeLauncher, type: PanelAgentRuntimeLauncher }
              - { name: proposalParser, type: PanelAgentProposalParser }
              - { name: chatModel, type: ChatModel }
              - { name: config, type: LagenConfig }
              - { name: projectRoot, type: Path }
          - name: DefaultPanelAgentProposalParser
            _change: add
            implements: [PanelAgentProposalParser]
          - name: SharedRuntimePanelAgentLauncher
            _change: add
            implements: [PanelAgentRuntimeLauncher]
            requiresInject:
              - { name: agentGraphRunner, type: AgentGraphRunner }
```

## Registrar FEAT-MESA y probarla sin pagar
La feature se registra para que `feature sync --feature FEAT-MESA` anote J001; su ubicación es del qa-tester. Todas las
pruebas usan el proponente guionado y jueces de mentira o con veredictos grabados en un libro de prueba: el ejercicio
11 de *Afirmativas regulares* con sus dos arreglos (sólo la traducción vuelve por no sonar natural; el inglés pasa y
pide regrabar), «grandmother» rechazado y «Winter» aceptado, tres intentos sin ganador, un desacuerdo de traducción, la
miniteoría de *Contraste inicial* reescrita en sus 9 ejercicios, un ejercicio de «Contracciones» pasado a opción múltiple
con tres opciones, y las mismas decisiones con 1 y 1, 4 y 4, y 8 y 8 a la vez. Ninguna consulta un modelo.

```architecture
features:
  - id: FEAT-MESA
    code: F-MESA
modules:
  - name: revision-domain
    _change: modify
```

## Lo que suma la integración de la ronda 2
`patch apply` reemplaza la lista de patrones de un módulo por la del parche, no la suma: NOEMP, MESA, TANDA y BANDEJA
declaraban cada uno su fábrica en `revision-domain`, y aplicados en orden quedaba sólo la última, sin las dos que el
módulo ya tenía. Este parche lleva la lista entera hasta él (cuatro), armada contra el modelo que deja el anterior.

```architecture
modules:
  - name: revision-domain
    _change: modify
```

## Declarar las 55 pruebas y ubicar el journey
Las 55 son nuevas y cubren las quince reglas (R001 4, R002 4, R003 6, R004 3, R005 5, R006 5, R007 3, R008 2, R009 2,
R010 6, R011 3, R012 2, R013 2, R014 3, R015 5). Van sobre el motor de la mesa —`DefaultTopicPanel` 19,
`DefaultPanelRunner` 4, `CappedQueryAllowance` 2, `CatalogTopicMeter` 10, `DefaultPanelDecisionScript` 8 y
`DefaultCandidateScreen` 6—, sobre el agente que propone (3) y, para R014, sobre lo que corrige fuera de las tandas:
`ReviseCmd`, `DefaultQuizInstructionCorrectionRunner` y `DefaultQuizInstructionCandidateAssessor`, que ya existen.
Ninguna consulta un modelo: proponen `ScriptedPanelProposer` o un agente con su runtime simulado, y los jueces son de
mentira o leen veredictos grabados en un libro de prueba.

**De dónde sale cada prueba.** El ejercicio 11 de *Afirmativas regulares* (`…1ecd`), «I ___ a lot of vegetables.», con
sus tres tareas en un solo candidato; después de que la regla de la tanda le pone «(love)», `single-answer` ya no lo marca
y su tarea «ya no se sostiene» sin gastar un intento; sus dos arreglos: sólo la traducción vuelve por no sonar natural y el
inglés pasa y pide regrabar. «My grandmother ___ (come) every year.» rechazado con grandmother (A2) como evidencia y
«Winter ___ (come) every year.» aceptado en el segundo intento; tres intentos sin ganador; un desacuerdo de los dos
jueces de traducción, que queda para José sin gastar intentos. La miniteoría de los verbos de estado de «Contraste
inicial», en sus nueve ejercicios y antes que sus candidatos. «Contracciones» (A1) pasado a opción múltiple: «You ___
(do not) work at the bank.» con «don't» y dos contracciones más pasa; «She ___ (is not) very happy.» con «isn't» al lado
de la correcta «'s not» no. Y las mismas decisiones con 1 y 1, 4 y 4, y 8 y 8 a la vez.

**Lo que cumplen otras piezas.** Que el candidato aceptado quede como propuesta pendiente con su artefacto (R009) lo
guarda la tanda, y se prueba con F-TANDA-R007; acá, que la mesa lo entrega en su decisión y no escribe el curso. El tope
total de consultas de la tanda es F-TANDA-R003. Una parte de F-TAREA-R006 vive en `DefaultTopicPanel`: que un tipo sin
alcance declarado nunca se intenta; desde el 1/10 se prueba en la tanda con F-TANDA-R005, que nombra ese desenlace. Y la
mesa nunca recibe la tarea de un audio pendiente de regrabar (F-TANDA-R012): ninguna tanda cambia un ejercicio para que
diga lo que dice un audio viejo.

**El journey** va a `revision-domain`, en `com.learney.contentaudit.revisiondomain.panelengine`: el corredor, la mesa, el
medidor, el filtro y el guion son package-private ahí, y quien propone y la compuerta se arman con sus piezas públicas
(`ScriptedPanelProposer`, `DefaultGateFactory`). Sus pasos son del sistema, como en F-PIPRE-J001. `generate` crearía
`FMesaJ001JourneyTest` con 4 caminos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: panelengine
        _change: add
        implementations:
          - name: DefaultPanelDecisionScript
            _change: add
            handwrittenTests:
              - name: "should accept «Winter ___ (come) every year.» because it solves its task and the change gate passes"
                traceability: { feature: FEAT-MESA, rule: F-MESA-R006 }
              - name: "should reject «My grandmother ___ (come) every year.», which solves its task but adds grandmother (A2)"
                traceability: { feature: FEAT-MESA, rule: F-MESA-R006 }
features:
  - id: FEAT-MESA
    code: F-MESA
    journeys:
      - { id: F-MESA-J001, testModule: revision-domain, testPackage: com.learney.contentaudit.revisiondomain.panelengine }
```
