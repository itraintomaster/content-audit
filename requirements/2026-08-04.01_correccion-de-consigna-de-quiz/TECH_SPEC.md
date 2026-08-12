---
patch: ARCH-QICOR
requirement: 2026-08-04.01_correccion-de-consigna-de-quiz
generated: 2026-08-04T23:30:00Z
---

# Tech Spec: Correccion del incumplimiento de consigna de un ejercicio

> **Alcance de este documento.** Describe el diseño de FEAT-QICOR en su estado consolidado, con los cuatro arreglos posteriores ya incorporados. Los fences diffean contra `sentinel-baseline.yaml`, que es el snapshot anterior a la feature, asi que se leen como "de aca fuimos a aca" aunque el cambio ya este aplicado.
>
> **Alta de la feature.** El patch incluye un bloque `features:` que da de alta `FEAT-QICOR` / `F-QICOR` con sus dos journeys, ambos `AUTO_VALIDATED` en `audit-cli` bajo `com.learney.contentaudit.journeys`. Hace falta porque `sentinel feature sync` no da de alta ids nuevos por su cuenta. Van a `audit-cli` porque los dos cruzan el sistema entero —del plan a la propuesta y de la propuesta al curso— y es el unico modulo con visibilidad de compilacion sobre todas las piezas.

## Poner el gate de aceptacion en Java y no en el grafo del agente

La decision estructural de la feature fue donde vive el lazo candidato → criterios → reintento. Habia tres opciones y la vara para elegir fue una sola: **en este proyecto la cobertura transitiva no cuenta**, cada regla necesita un test que la referencie directamente.

Dejar todo en el grafo declarativo —el espejo de lemma-absence, que ya tiene ese lazo andando con evals que invocan la medicion real via `content-audit tokens` y `content-audit lexis`— se cayo por dos motivos. El primero es la vara: R003, R004 (a)(b)(c) y R005 no tendrian ningun elemento Java que un test pueda referenciar, y un test contra un veredicto fabricado verifica la plomeria, no el criterio. El segundo lo decidio: **la correccion de consigna necesita un grafo nuevo**, asi que "reusar" los evals significaba copiarlos, y el criterio quedaba igual en dos lugares. Con el gate en Java la medicion se llama, no se copia.

Lo que **no** se movio a Java son los prompts. Los criterios de juicio se alcanzan por un puerto cuyo adaptador corre un grafo declarativo, asi que siguen iterandose sin recompilar.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: add
        visibility: public
        interfaces:
          - name: CandidateAssessor
            _change: add
            exposes:
              - signature: "assess(CandidateAssessmentInput input): CandidateAssessment"
              - signature: "catalog(): List<CorrectionCriterion>"
```

## Abrir el armado del contenido juzgado en vez de duplicarlo

R005 y R009 exigen someter el corregido y el original a la misma validacion que detecto el incumplimiento, y esa capacidad parecia encerrada en `quiz-instruction-infrastructure`, cuyo control de acceso la reserva para `audit-cli`. La tension era aparente: el juez se alcanza por el puerto `Evaluator` y su payload por `QuizInstructionVerdictReader`, ambos ya publicos. El unico hueco real era `QuizInstructionSubjectBuilder`, que estaba `internal`.

Se abrio en su lugar y gano una segunda entrada, sin tocar una coma de `serializeQuiz`. Asi el render del quiz sigue ocurriendo en **un solo lugar** y las tres rutas —auditoria, revalidacion del original, verificacion del corregido— pasan por el. Eso es tambien lo que hace barato el cambio pendiente de incluir `form.sentences` en el contenido juzgado: sera un metodo, no tres definiciones que puedan divergir. No hubo ningun cambio de `allowedClients`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstructionengine
        _change: modify
        interfaces:
          - name: QuizInstructionSubjectBuilder
            _change: modify
            visibility: public
            exposes:
              - signature: "buildFromView(QuizInstructionSubjectView view): EvaluationSubject"
                _change: add
```

## Introducir la vista juzgada con una unica factory que la arma

El armado dependia de tener un `AuditNode`, y la revision no lo tiene: tiene ejercicios fuera del arbol de auditoria. La vista desacopla las cinco entradas declaradas y viaja verbatim —el nivel es el label del milestone, no el enum— porque cualquier normalizacion distinta entre rutas produce huellas distintas.

La factory existe porque hay **dos** llamadores que arman la misma vista sobre ejercicios distintos. El render unico no alcanza si el mapeo previo —que campo del curso ocupa que entrada— se escribe dos veces.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionSubjectView
            _change: add
            type: record
            visibility: public
            fields:
              - { name: subjectRef, type: String }
              - { name: cefrLevel, type: String }
              - { name: topic, type: String }
              - { name: title, type: String }
              - { name: instructions, type: String }
              - { name: sentenceParts, type: "List<SentencePartEntity>" }
        interfaces:
          - name: QuizInstructionSubjectViewFactory
            _change: add
            stereotype: factory
            exposes:
              - signature: "fromQuiz(QuizTemplateEntity quiz, String cefrLevel, String topic, String title, String instructions): QuizInstructionSubjectView"
      - name: quizinstructionengine
        _change: modify
        implementations:
          - name: DefaultQuizInstructionSubjectViewFactory
            _change: add
            visibility: public
            implements: ["QuizInstructionSubjectViewFactory"]
```

## Exponer la verificacion con dos modos de resolucion sobre un unico criterio

Los dos verbos no son dos criterios: son dos formas de resolver el mismo. `check` permite reuso y verifica al candidato corregido, cuyo contenido es nuevo y por lo tanto casi nunca esta registrado. `revalidate` fuerza la consulta y revalida al original de R009.

Forzar no es un detalle de implementacion, es la regla. La huella del original coincide con la del veredicto viejo —el contenido no cambio—, asi que sin forzar la sesion lo reutiliza y **R009 seria un no-op**: el diagnostico caido no se detectaria jamas. Es exactamente el caso de los 3 ejercicios sobre 20 identicos que documenta el requisito. Al ir por la sesion, todo veredicto nuevo queda registrado bajo la huella del contenido, asi que la auditoria siguiente lo reutiliza y la limpieza se paga una sola vez.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionComplianceStatus
            _change: add
            type: enum
            fields:
              - { name: EVALUATED }
              - { name: UNAVAILABLE }
              - { name: UNUSABLE }
          - name: QuizInstructionComplianceResult
            _change: add
            type: record
            fields:
              - { name: status, type: QuizInstructionComplianceStatus }
              - { name: verdict, type: QuizInstructionVerdict }
              - { name: reason, type: String }
              - { name: evaluatorVersion, type: String }
              - { name: reused, type: boolean }
        interfaces:
          - name: QuizInstructionComplianceChecker
            _change: add
            stereotype: port
            exposes:
              - signature: "check(QuizInstructionSubjectView view): QuizInstructionComplianceResult"
              - signature: "revalidate(QuizInstructionSubjectView view): QuizInstructionComplianceResult"
          - name: QuizInstructionComplianceCheckerFactory
            _change: add
            stereotype: factory
            exposes:
              - signature: "create(EvaluationSessionFactory sessionFactory,Evaluator evaluator,QuizInstructionVerdictReader verdictReader,EvaluationBudget budget): QuizInstructionComplianceChecker"
      - name: quizinstructionengine
        _change: modify
        implementations:
          - name: DefaultQuizInstructionComplianceCheckerFactory
            _change: add
            visibility: public
            implements: ["QuizInstructionComplianceCheckerFactory"]
          - name: LedgerBackedQuizInstructionComplianceChecker
            _change: add
            requiresInject:
              - { name: session, type: EvaluationSession }
              - { name: subjectBuilder, type: QuizInstructionSubjectBuilder }
              - { name: verdictReader, type: QuizInstructionVerdictReader }
            implements: ["QuizInstructionComplianceChecker"]
```

## Registrar un resolver de contexto para QUIZ_INSTRUCTION

Este era el hueco que bloqueaba la feature entera: `DiagnosisKind.QUIZ_INSTRUCTION` existe y `DefaultRefinerEngine` ya emite la tarea, pero ningun resolver la atendia, asi que pedir su revision caia al comportamiento de identidad de F-REVBYP-R004.

El contexto reusa `InstructionViolation` de audit-domain en lugar de definir un carrier espejo, porque R002 exige que restriccion, evidencia y explicacion lleguen completas y un tipo paralelo es precisamente por donde se pierden. `cefrLevelLabel` convive con `cefrLevel` a proposito: uno gobierna los criterios de nivel, el otro reproduce la huella exacta que la revalidacion necesita.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: QuizInstructionCorrectionContext
        _change: add
        type: record
        implements: ["CorrectionContext"]
        fields:
          - { name: taskId, type: String }
          - { name: nodeId, type: String }
          - { name: quizSentence, type: String }
          - { name: translation, type: String }
          - { name: sentence, type: String }
          - { name: knowledgeTitle, type: String }
          - { name: knowledgeInstructions, type: String }
          - { name: topicLabel, type: String }
          - { name: cefrLevel, type: CefrLevel }
          - { name: cefrLevelLabel, type: String }
          - { name: sentenceMode, type: SentenceMode }
          - { name: violations, type: "List<InstructionViolation>" }
          - { name: verdictReason, type: String }
          - { name: severity, type: InstructionSeverity }
          - { name: siblingQuizSentences, type: "List<String>" }
          - { name: sourceAuditId, type: String }
    implementations:
      - name: QuizInstructionContextResolver
        _change: add
        visibility: public
        implements: ["CorrectionContextResolver"]
      - name: DispatchingCorrectionContextResolver
        _change: modify
        requiresInject:
          - { name: quizInstructionResolver, type: QuizInstructionContextResolver }
```

## Modelar el catalogo de criterios con un puerto deliberadamente neutro

R004 pide un catalogo fijo con un veredicto por criterio y R006 exige nombrar los que reprobaron, asi que un veredicto agregado no sirve. El catalogo activo son siete:

| Criterio | Regla | Donde se mide | Como |
|---|---|---|---|
| `SCOPED_EDIT` | R003 | revision-domain | compara partes; una parte cambiada sin evidencia que la señale reprueba |
| `LENGTH_IN_RANGE` | R004 #1 | revision-domain | `NlpTokenizer` + `SentenceLengthConfig`, directo |
| `LEVEL_VOCABULARY` | R004 #2 | revision-domain | `SentenceLexicalEvaluator`; contencion de flags contra el original |
| `DISTINCTNESS` | R004 #3 | revision-domain | puerto sobre el corpus del analisis fuente |
| `FAITHFUL_TRANSLATION` | R004 #4 | revision-infrastructure | grafo declarativo de juicio |
| `SOLVABLE_SAME_KIND` | R004 #5 | revision-infrastructure | grafo declarativo de juicio |
| `INSTRUCTION_COMPLIANCE` | R005 | revision-domain → checker | la misma validacion que detecto el incumplimiento |

La firma del puerto no menciona violaciones, ni tarea, ni diagnostico, y eso no es elegancia. La fidelidad de traduccion y la resolubilidad **ya se juzgan hoy** en el nodo `eval_quality` del grafo de lemma-absence, asi que ese criterio vive en dos lugares hasta que lemma-absence adopte el puerto; diseñarlo neutro ahora convierte esa adopcion en migracion y no en rediseño. `signalledFragments` generaliza "lo que la violacion señala" —evidencias textuales aca, lemas señalados alla—. `NOT_EVALUABLE` distingue reprobar de no haber podido juzgar, que R006 trata distinto.

La revalidacion de R009 deliberadamente **no** es un criterio del catalogo: ocurre antes de que exista candidato, y forzarla dentro del puerto lo obligaria a admitir un input sin candidato.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        models:
          - name: CorrectionCriterion
            _change: add
            type: enum
            fields:
              - { name: SCOPED_EDIT }
              - { name: LENGTH_IN_RANGE }
              - { name: LEVEL_VOCABULARY }
              - { name: DISTINCTNESS }
              - { name: FAITHFUL_TRANSLATION }
              - { name: SOLVABLE_SAME_KIND }
              - { name: INSTRUCTION_COMPLIANCE }
          - name: CriterionOutcome
            _change: add
            type: enum
            fields:
              - { name: PASSED }
              - { name: FAILED }
              - { name: NOT_EVALUABLE }
          - name: CriterionVerdict
            _change: add
            type: record
            fields:
              - { name: criterion, type: CorrectionCriterion }
              - { name: outcome, type: CriterionOutcome }
              - { name: detail, type: String }
          - name: CandidateAssessmentInput
            _change: add
            type: record
            fields:
              - { name: subjectRef, type: String }
              - { name: sourceAuditId, type: String }
              - { name: original, type: CourseElementSnapshot }
              - { name: candidate, type: CourseElementSnapshot }
              - { name: cefrLevel, type: CefrLevel }
              - { name: topicLabel, type: String }
              - { name: knowledgeTitle, type: String }
              - { name: knowledgeInstructions, type: String }
              - { name: cefrLevelLabel, type: String }
              - { name: sentenceMode, type: SentenceMode }
              - { name: siblingQuizSentences, type: "List<String>" }
              - { name: signalledFragments, type: "List<String>" }
          - name: CandidateAssessment
            _change: add
            type: record
            fields:
              - { name: accepted, type: boolean }
              - { name: verdicts, type: "List<CriterionVerdict>" }
        interfaces:
          - name: CandidateCriterionEvaluator
            _change: add
            stereotype: port
            exposes:
              - signature: "criterion(): CorrectionCriterion"
              - signature: "evaluate(CandidateAssessmentInput input): CriterionVerdict"
```

## Medir con la auditoria real en vez de aproximarla

La leccion de F-LASAG-R007 es que aprobar con una regla de medir distinta de la que despues juzga hace que la auditoria siguiente vuelva a marcar el mismo ejercicio y el trabajo se haga dos veces. Por eso cada criterio determinista inyecta el colaborador que ya usa el audit, en vez de reimplementarlo o invocarlo por subproceso. `LevelVocabularyCriterion` compara contra los flags del original, que es lo que hace que un problema lexico preexistente no bloquee la correccion.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteriaengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultCandidateAssessor
            _change: add
            requiresInject:
              - { name: evaluatorsByCriterion, type: "Map<CorrectionCriterion,CandidateCriterionEvaluator>" }
            implements: ["CandidateAssessor"]
          - name: ScopedEditCriterion
            _change: add
            implements: ["CandidateCriterionEvaluator"]
          - name: LengthInRangeCriterion
            _change: add
            requiresInject:
              - { name: tokenizer, type: NlpTokenizer }
              - { name: sentenceLengthConfig, type: SentenceLengthConfig }
            implements: ["CandidateCriterionEvaluator"]
          - name: LevelVocabularyCriterion
            _change: add
            requiresInject:
              - { name: lexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: tokenizer, type: NlpTokenizer }
            implements: ["CandidateCriterionEvaluator"]
          - name: InstructionComplianceCriterion
            _change: add
            requiresInject:
              - { name: complianceChecker, type: QuizInstructionComplianceChecker }
              - { name: subjectViewFactory, type: QuizInstructionSubjectViewFactory }
            implements: ["CandidateCriterionEvaluator"]
```

## Poner la distincion detras de un puerto aunque hoy sea un recorrido en memoria

El alcance aprobado compara completo contra los hermanos del knowledge y de forma aproximada contra el resto del curso. Hoy eso se resuelve recorriendo los ejercicios del informe del analisis fuente, que ya esta cargado. El puerto se declara igual: es barato ahora y evita la cirugia el dia que la comparacion sobre miles de ejercicios pida un indice persistente, porque cambiara el adaptador y no el criterio.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        models:
          - name: QuizSimilarity
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: quizSentence, type: String }
              - { name: score, type: double }
        interfaces:
          - name: CourseQuizCorpus
            _change: add
            stereotype: port
            exposes:
              - signature: "mostSimilar(String sourceAuditId, String candidateSentence, String excludedQuizId): Optional<QuizSimilarity>"
      - name: candidatecriteriaengine
        _change: modify
        implementations:
          - name: DistinctnessCriterion
            _change: add
            requiresInject:
              - { name: quizCorpus, type: CourseQuizCorpus }
              - { name: siblingSimilarityThreshold, type: double }
              - { name: courseSimilarityThreshold, type: double }
              - { name: courseWideDistinctness, type: boolean }
            implements: ["CandidateCriterionEvaluator"]
          - name: AuditReportQuizCorpus
            _change: add
            requiresInject:
              - { name: auditReportStore, type: AuditReportStore }
            implements: ["CourseQuizCorpus"]
```

## Cablear el catalogo por un unico seam con carrier de configuracion

Los siete criterios tienen colaboradores distintos y dos los aporta la infraestructura, asi que la composition root tendria que conocer siete clases concretas si no hubiera seam. Que los evaluadores de juicio lleguen como **lista**, y no como campos nombrados, es lo que permite mover un criterio entre Java y agente —o retirarlo— sin tocar el assessor.

El carrier pide `AuditReportStore` y no un `CourseQuizCorpus` ya construido: la unica implementacion del corpus es package-private dentro del paquete motor, asi que pedirlo habria exigido a la composition root un tipo que no puede nombrar, y abrirlo habria perforado el encapsulamiento sobre el que se apoya la invisibilidad de los criterios.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        models:
          - name: CandidateCriteriaConfig
            _change: add
            type: record
            fields:
              - { name: tokenizer, type: NlpTokenizer }
              - { name: sentenceLengthConfig, type: SentenceLengthConfig }
              - { name: lexicalEvaluator, type: SentenceLexicalEvaluator }
              - { name: complianceChecker, type: QuizInstructionComplianceChecker }
              - { name: subjectViewFactory, type: QuizInstructionSubjectViewFactory }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: judgmentEvaluators, type: "List<CandidateCriterionEvaluator>" }
              - { name: siblingSimilarityThreshold, type: Double }
              - { name: courseSimilarityThreshold, type: Double }
              - { name: courseWideDistinctness, type: Boolean }
          - name: CandidateCriteriaDefaults
            _change: add
            type: record
            fields:
              - { name: siblingSimilarityThreshold, type: double }
              - { name: courseSimilarityThreshold, type: double }
              - { name: courseWideDistinctness, type: boolean }
        interfaces:
          - name: CandidateCriteriaFactory
            _change: add
            stereotype: factory
            exposes:
              - signature: "create(CandidateCriteriaConfig config): CandidateAssessor"
      - name: candidatecriteriaengine
        _change: modify
        implementations:
          - name: DefaultCandidateCriteriaFactory
            _change: add
            visibility: public
            implements: ["CandidateCriteriaFactory"]
```

## Abrir la SPI de correccion con reintento informado

La generacion es espejo de `lemmaabsence` y `knowledgetitle`. Lo propio de esta feature esta en la firma del pedido: cada intento recibe los veredictos que reprobo el anterior. Sin eso, tres intentos son tres tiradas iguales y el tope de reintentos solo multiplica el gasto; con eso es el mecanismo que ya funciona en F-LASAG-R008.

El candidato es `LemmaAbsenceQuizCandidate` tal cual. El carrier es el par quizSentence/traduccion y no tiene nada propio de la ausencia de lema, asi que reusarlo evita duplicar la derivacion y el respeto del `sentenceMode`. Su nombre miente y se salda en un patch de FEAT-LAPS, que es su dueño.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: QuizInstructionProposalStrategy
        _change: add
        stereotype: port
        exposes:
          - signature: "id(): StrategyId"
          - signature: "handles(DiagnosisKind kind): boolean"
          - signature: "propose(RefinementTask task, QuizInstructionCorrectionContext context, List<CriterionVerdict> previousVerdicts, int attempt): LemmaAbsenceQuizCandidate"
      - name: QuizInstructionProposalStrategyRegistry
        _change: add
        sealed: true
        exposes:
          - signature: "active(): Optional<QuizInstructionProposalStrategy>"
          - signature: "byName(String name): Optional<QuizInstructionProposalStrategy>"
          - signature: "listAll(): List<StrategyId>"
    packages:
      - name: quizinstruction
        _change: add
        visibility: public
        models:
          - name: QuizInstructionGeneratorResponse
            _change: add
            type: record
            fields:
              - { name: quizSentence, type: String }
              - { name: translation, type: String }
              - { name: rationale, type: String }
          - name: QuizInstructionGenerationRequest
            _change: add
            type: record
            fields:
              - { name: context, type: QuizInstructionCorrectionContext }
              - { name: previousVerdicts, type: "List<CriterionVerdict>" }
              - { name: attempt, type: int }
        interfaces:
          - name: QuizInstructionCandidateGenerator
            _change: add
            stereotype: port
            exposes:
              - signature: "generate(QuizInstructionGenerationRequest request): QuizInstructionGeneratorResponse"
        implementations:
          - name: QuizInstructionAgentStrategy
            _change: add
            visibility: public
            requiresInject:
              - { name: generator, type: QuizInstructionCandidateGenerator }
              - { name: providerId, type: String }
            implements: ["QuizInstructionProposalStrategy"]
```

## Revalidar el original antes de gastar, y separar ese desenlace del fracaso

Sin R009 la cadena entera se ejecuta sin que ningun control la detenga: el veredicto viejo genera la tarea, la correccion reescribe un ejercicio que estaba bien, R005 pregunta si el corregido cumple y responde que si —porque siempre cumplio—, y el operador aprueba una reescritura que nadie necesitaba. El reviser revalida primero y corta ahi.

Los dos cortes se modelan como excepciones porque `Reviser.propose` no tiene otro canal para no devolver una propuesta —el mismo que ya usan `ProposalStrategyFailedException` y `ProposalDerivationException`—, pero producen desenlaces opuestos: uno es trabajo que se evaporo, el otro es trabajo que hay que hacer a mano. Agotados los intentos el reviser no propone nada: aca la correccion compite contra un ejercicio que ya funcionaba, no contra la nada, y es la ruptura deliberada con el mejor esfuerzo de F-LASAG-R009.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionOutcomeKind
        _change: modify
        fields:
          - { name: DIAGNOSIS_NOT_SUSTAINED, _change: add }
          - { name: NO_ACCEPTABLE_CANDIDATE, _change: add }
      - name: DiagnosisNotSustainedException
        _change: add
        type: exception
        fields:
          - { name: quizId, type: String }
          - { name: taskId, type: String }
      - name: NoAcceptableCandidateException
        _change: add
        type: exception
        fields:
          - { name: quizId, type: String }
          - { name: failedCriteria, type: "List<CorrectionCriterion>" }
          - { name: taskId, type: String }
    packages:
      - name: engine
        _change: modify
        models:
          - name: QuizInstructionProposalStrategyRegistryConfig
            _change: add
            type: record
            fields:
              - { name: strategies, type: "List<QuizInstructionProposalStrategy>" }
              - { name: activeStrategyName, type: String }
        implementations:
          - name: QuizInstructionReviser
            _change: add
            requiresInject:
              - { name: strategyRegistry, type: QuizInstructionProposalStrategyRegistry }
              - { name: deriver, type: LemmaAbsenceProposalDeriver }
              - { name: assessor, type: CandidateAssessor }
              - { name: complianceChecker, type: QuizInstructionComplianceChecker }
              - { name: subjectViewFactory, type: QuizInstructionSubjectViewFactory }
              - { name: config, type: QuizInstructionCorrectionConfig }
            implements: ["Reviser"]
          - name: DefaultQuizInstructionProposalStrategyRegistry
            _change: add
            visibility: public
            requiresInject:
              - { name: config, type: QuizInstructionProposalStrategyRegistryConfig }
            implements: ["QuizInstructionProposalStrategyRegistry"]
```

## Marcar la tarea caida para no volver a pagar por la misma conclusion

El veredicto de la revalidacion queda registrado bajo la huella del contenido, asi que la auditoria siguiente lo reutiliza y el plan que se genere ya no incluye la tarea: la limpieza definitiva llega sola. Pero hasta ese momento la tarea sigue en el plan actual, y sin marca la corrida siguiente la vuelve a tomar, la revalida y vuelve a pagar la consulta indefinidamente. `STALE` es distinto de `SKIPPED` —alguien decidio saltearla— y de `COMPLETED` —se corrigio—, y deja la tarea visible por si la revalidacion se equivoco.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: RefinementTaskStatus
        _change: modify
        fields:
          - { name: STALE, _change: add }
```

## Separar los cuatro desenlaces en el reporte de corrida y persistirlo

La mitad de R008 que la vuelve verificable no es el tope sino la declaracion de lo que quedo afuera, y ahi hay una asimetria: las tareas intentadas dejan un `RevisionArtifact`, las no intentadas por definicion no dejan nada. Si su unico rastro es la terminal se pierden al primer scroll y la corrida vuelve a leerse como cobertura completa.

Los recuentos van separados porque el operador reacciona distinto ante cada uno; colapsar `diagnosisStale` dentro de `notCorrected` le diria que tiene decenas de correcciones imposibles cuando lo que tiene son decenas de diagnosticos viejos que se cayeron solos. `attempted` cuenta tareas **tocadas**, no corregidas: una revalidacion tambien es una consulta pagada y consume tope.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: QuizInstructionCorrectionRunner
        _change: add
        stereotype: port
        exposes:
          - signature: "run(QuizInstructionCorrectionRunRequest request): QuizInstructionCorrectionRunReport"
      - name: QuizInstructionCorrectionRunnerFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "create(RevisionEngineConfig config, QuizInstructionCorrectionRunStore runStore, QuizInstructionCorrectionConfig correctionConfig): QuizInstructionCorrectionRunner"
      - name: QuizInstructionCorrectionRunStore
        _change: add
        stereotype: port
        exposes:
          - signature: "save(QuizInstructionCorrectionRunReport report): String"
          - signature: "load(String runId): Optional<QuizInstructionCorrectionRunReport>"
          - signature: "listByPlan(String planId): List<QuizInstructionCorrectionRunReport>"
          - signature: "latest(): Optional<QuizInstructionCorrectionRunReport>"
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionTaskOutcomeKind
            _change: add
            type: enum
            fields:
              - { name: PROPOSED }
              - { name: NOT_CORRECTED }
              - { name: DIAGNOSIS_STALE }
              - { name: NOT_ATTEMPTED }
              - { name: FAILED }
          - name: QuizInstructionTaskOutcome
            _change: add
            type: record
            fields:
              - { name: taskId, type: String }
              - { name: nodeId, type: String }
              - { name: kind, type: QuizInstructionTaskOutcomeKind }
              - { name: failedCriteria, type: "List<CorrectionCriterion>" }
              - { name: detail, type: String }
              - { name: proposalId, type: String }
              - { name: attempts, type: int }
          - name: QuizInstructionCorrectionRunReport
            _change: add
            type: record
            fields:
              - { name: runId, type: String }
              - { name: planId, type: String }
              - { name: sourceAuditId, type: String }
              - { name: startedAt, type: Instant }
              - { name: finishedAt, type: Instant }
              - { name: maxCorrections, type: int }
              - { name: eligibleTasks, type: int }
              - { name: attempted, type: int }
              - { name: proposed, type: int }
              - { name: notCorrected, type: int }
              - { name: diagnosisStale, type: int }
              - { name: failed, type: int }
              - { name: notAttempted, type: "List<String>" }
              - { name: outcomes, type: "List<QuizInstructionTaskOutcome>" }
          - name: QuizInstructionCorrectionRunRequest
            _change: add
            type: record
            fields:
              - { name: planId, type: String }
              - { name: coursePath, type: Path }
              - { name: maxCorrections, type: Integer }
      - name: engine
        _change: modify
        implementations:
          - name: DefaultQuizInstructionCorrectionRunner
            _change: add
            requiresInject:
              - { name: revisionEngine, type: RevisionEngine }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: runStore, type: QuizInstructionCorrectionRunStore }
              - { name: config, type: QuizInstructionCorrectionConfig }
            implements: ["QuizInstructionCorrectionRunner"]
          - name: DefaultQuizInstructionCorrectionRunnerFactory
            _change: add
            visibility: public
            implements: ["QuizInstructionCorrectionRunnerFactory"]
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemQuizInstructionCorrectionRunStore
        _change: add
        visibility: public
        implements: ["QuizInstructionCorrectionRunStore"]
```

## Cablear la revalidacion por el mismo objeto que verifica el candidato

`RevisionEngineConfig` recibe el checker y la factory de vistas, no un colaborador propio de la revalidacion. Es la expresion en el wiring de la decision central: hay **una** definicion de cumplimiento con dos puntos de invocacion, y el composition root no tiene forma de darle a R009 un juez distinto del de R005 aunque quisiera.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionEngineConfig
        _change: modify
        fields:
          - { name: quizInstructionStrategyRegistry, type: QuizInstructionProposalStrategyRegistry, _change: add }
          - { name: candidateAssessor, type: CandidateAssessor, _change: add }
          - { name: quizInstructionComplianceChecker, type: QuizInstructionComplianceChecker, _change: add }
          - { name: quizInstructionSubjectViewFactory, type: QuizInstructionSubjectViewFactory, _change: add }
          - { name: quizInstructionCorrectionConfig, type: QuizInstructionCorrectionConfig, _change: add }
```

## Dejar los numeros que gobiernan el gasto como configuracion

Las dudas abiertas que cambian cuanto cuesta una corrida —cuantos reintentos, cuantas tareas por corrida, si la revalidacion consume tope, con que alcance la distincion— se resolvieron con supuestos que conviene ajustar con datos de las primeras tandas. Cablearlos volveria cada ajuste un cambio de codigo. El tope cuenta tareas **tocadas** y no solo corregidas, que es la unica lectura en la que ninguna corrida puede gastar mas de lo que su tope dice.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionCorrectionConfig
            _change: add
            type: record
            fields:
              - { name: maxAttempts, type: Integer }
              - { name: maxCorrectionsPerRun, type: Integer }
          - name: QuizInstructionCorrectionDefaults
            _change: add
            type: record
            fields:
              - { name: maxAttempts, type: int }
              - { name: maxCorrectionsPerRun, type: int }
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        interfaces:
          - name: QuizInstructionCorrectionConfigResolver
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "resolve(Map<String,String> env): QuizInstructionCorrectionConfig"
        implementations:
          - name: EnvQuizInstructionCorrectionConfigResolver
            _change: add
            visibility: public
            implements: ["QuizInstructionCorrectionConfigResolver"]
```

## Alojar generacion y juicio en dos adaptadores de agente separados

El prompt de la correccion y el del juicio viven en definiciones declarativas, no en Java: el adapter solo traduce entradas, parsea la salida y clasifica fallas de runtime preservando la categoria. Que sean **dos** grafos y no uno es deliberado — si el mismo grafo generara y evaluara, el candidato se estaria corrigiendo a si mismo. La factory de juicio se indexa por `CorrectionCriterion`, que es un tipo neutro, de modo que el adaptador sirve a cualquier correccion de ejercicio. `revision-infrastructure` gana `audit-domain` y `course-domain` porque el input neutro carga `CefrLevel` y entidades de curso.

```architecture
modules:
  - name: revision-infrastructure
    _change: modify
    dependsOn: [audit-domain, course-domain]
    packages:
      - name: quizinstructionagent
        _change: add
        visibility: public
        interfaces:
          - name: QuizInstructionAgentGeneratorFactory
            _change: add
            stereotype: factory
            exposes:
              - signature: "create(LagenConfig config): QuizInstructionCandidateGenerator"
              - signature: "providerIdFor(LagenConfig config): String"
          - name: QuizInstructionAgentRuntimeLauncher
            _change: add
            visibility: internal
            exposes:
              - signature: "launch(String agentName, Map<String,String> inputs, ChatModel chatModel): RunState"
          - name: QuizInstructionAgentCandidateParser
            _change: add
            visibility: internal
            exposes:
              - signature: "parse(RunState runState, String quizId): QuizInstructionGeneratorResponse"
          - name: QuizInstructionAgentErrorClassifier
            _change: add
            visibility: internal
            exposes:
              - signature: "classify(RunState runState): LlmGenerationFailureCategory"
              - signature: "classify(Throwable cause): LlmGenerationFailureCategory"
        implementations:
          - name: DefaultQuizInstructionAgentGeneratorFactory
            _change: add
            visibility: public
            implements: ["QuizInstructionAgentGeneratorFactory"]
          - name: QuizInstructionAgentGenerator
            _change: add
            requiresInject:
              - { name: agentRuntimeLauncher, type: QuizInstructionAgentRuntimeLauncher }
              - { name: candidateParser, type: QuizInstructionAgentCandidateParser }
              - { name: errorClassifier, type: QuizInstructionAgentErrorClassifier }
              - { name: chatModel, type: ChatModel }
              - { name: config, type: LagenConfig }
              - { name: projectRoot, type: Path }
            implements: ["QuizInstructionCandidateGenerator"]
          - name: DefaultQuizInstructionAgentCandidateParser
            _change: add
            implements: ["QuizInstructionAgentCandidateParser"]
          - name: DefaultQuizInstructionAgentErrorClassifier
            _change: add
            implements: ["QuizInstructionAgentErrorClassifier"]
          - name: SharedRuntimeQuizInstructionLauncher
            _change: add
            requiresInject:
              - { name: agentGraphRunner, type: AgentGraphRunner }
            implements: ["QuizInstructionAgentRuntimeLauncher"]
      - name: candidatejudgment
        _change: add
        visibility: public
        interfaces:
          - name: CandidateJudgmentEvaluatorFactory
            _change: add
            stereotype: factory
            exposes:
              - signature: "create(LagenConfig config, CorrectionCriterion criterion): CandidateCriterionEvaluator"
          - name: CandidateJudgmentRuntimeLauncher
            _change: add
            visibility: internal
            exposes:
              - signature: "launch(String agentName, Map<String,String> inputs, ChatModel chatModel): RunState"
          - name: CandidateJudgmentVerdictParser
            _change: add
            visibility: internal
            exposes:
              - signature: "parse(RunState runState, CorrectionCriterion criterion): CriterionVerdict"
        implementations:
          - name: DefaultCandidateJudgmentEvaluatorFactory
            _change: add
            visibility: public
            implements: ["CandidateJudgmentEvaluatorFactory"]
          - name: AgentCandidateJudgmentEvaluator
            _change: add
            requiresInject:
              - { name: criterion, type: CorrectionCriterion }
              - { name: agentRuntimeLauncher, type: CandidateJudgmentRuntimeLauncher }
              - { name: verdictParser, type: CandidateJudgmentVerdictParser }
              - { name: chatModel, type: ChatModel }
              - { name: config, type: LagenConfig }
            implements: ["CandidateCriterionEvaluator"]
          - name: DefaultCandidateJudgmentVerdictParser
            _change: add
            implements: ["CandidateJudgmentVerdictParser"]
          - name: SharedRuntimeCandidateJudgmentLauncher
            _change: add
            requiresInject:
              - { name: agentGraphRunner, type: AgentGraphRunner }
            implements: ["CandidateJudgmentRuntimeLauncher"]
```

## Dar verbo propio a la corrida en vez de sumar flags a revise

`revise` corrige una tarea que el operador nombra y falla si no existe; la corrida recorre un tramo del plan y termina bien aunque no llegue a todas. Colapsarlas volveria ambiguo el codigo de salida, que es el dato con el que un operador decide si repetir. El comando declara `Callable<Integer>` como los otros cinco del modulo: es el contrato que picocli exige para registrarlo como subcomando, y sin el la corrida acotada queda escrita y probada pero inalcanzable.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: ReviseInstructionsCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "reviseInstructions(String planId, Integer maxCorrections, String coursePath): Integer"
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: ReviseInstructionsCmd
            _change: add
            requiresInject:
              - { name: correctionRunner, type: QuizInstructionCorrectionRunner }
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: runFormatter, type: QuizInstructionCorrectionRunFormatter }
            externalImplements:
              - java.util.concurrent.Callable<Integer>
            implements: ["ReviseInstructionsCommand"]
      - name: formatting
        _change: modify
        interfaces:
          - name: QuizInstructionCorrectionRunFormatter
            _change: add
            visibility: public
            exposes:
              - signature: "format(QuizInstructionCorrectionRunReport report): String"
        implementations:
          - name: DefaultQuizInstructionCorrectionRunFormatter
            _change: add
            visibility: public
            implements: ["QuizInstructionCorrectionRunFormatter"]
```

## Los cuatro arreglos posteriores y su causa comun

El diseño se aplico en un patch y necesito cuatro correcciones. Las cuatro comparten causa: **los elementos se declararon por su rol de diseño sin cruzarlos, campo por campo, contra el contrato mecanico que su entorno de ejecucion exige**. En los cuatro casos el precedente correcto estaba en el mismo paquete.

1. **El resolver generico.** Se le quito el `implements` sobre `CorrectionContextResolver` por un diagnostico equivocado. Se revirtio.
2. **Visibilidad.** El formatter y su implementacion, el store del reporte y el resolver de configuracion quedaron con visibilidad demasiado angosta para sus clientes declarados. En `audit-cli` el composition root vive en `commands`, asi que todo lo que instancia o tipa tiene que ser publico aunque su paquete sea interno: son los dos ejes de visibilidad y solo se miro uno. El corpus de distincion fue el unico que no se arreglo ensanchando —se invirtio la direccion del carrier—, porque abrirlo habria perforado el paquete motor.
3. **Superficie CLI.** `ReviseInstructionsCmd` no declaraba `Callable<Integer>` y no era registrable como subcomando: la feature quedaba inalcanzable sin ningun error de compilacion que lo delatara.
4. **La reversion del primero**, abajo.

La regla que evita la clase entera: **todo elemento nuevo que se enchufa a un framework copia la forma completa de su vecino mas cercano** —`externalImplements`, `visibility`, `implements`— y no solo la parte que expresa la intencion.

## Declarar `implements` sobre un puerto generico si funciona

`ARCH-QICOR-002` saco el `implements` de `QuizInstructionContextResolver` concluyendo que declararlo sobre un puerto generico esta roto siempre. **Es falso**, y la consecuencia fue una mina: cada `sentinel generate` dejaba la clase sin la clausula escrita a mano y el modulo dejaba de compilar.

Lo que decide el desenlace no es el DSL sino **el estado del archivo**. Sobre un stub recien generado el generador escribe las firmas del puerto tal cual y salen con `T` sin ligar, porque no hay sustitucion de variables de tipo en ningun punto. Sobre una clase ya poblada el merge conserva los metodos concretos y lo unico que el generador aporta es la clausula. `KnowledgeTitleContextResolver`, `SentenceLengthContextResolver` y `DispatchingCorrectionContextResolver` viven en ese estado estable desde siempre. La generalizacion se hizo desde una unica observacion sobre un stub vacio, que es la misma confusion stub/clase-poblada que causo el arreglo anterior.

La forma parametrizada `CorrectionContextResolver<QuizInstructionCorrectionContext>` tampoco aporta: resuelve la clausula pero los stubs siguen saliendo con `T`, y se aparta de los tres precedentes.

**Riesgo residual, preexistente y compartido por los cuatro resolvers:** si el archivo se borra, el stub regenerado sale con `Optional<T>` y no compila hasta que alguien reescriba los metodos con el tipo concreto. No lo introduce esta feature y no se puede cerrar desde el DSL.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    implementations:
      - name: QuizInstructionContextResolver
        _change: add
        visibility: public
        implements: ["CorrectionContextResolver"]
```

## Deuda conocida que esta entrega no cancela

**El criterio de calidad vive en dos lugares.** `FAITHFUL_TRANSLATION` y `SOLVABLE_SAME_KIND` se evaluan via `AgentCandidateJudgmentEvaluator`, y lo mismo se juzga hoy en el nodo `eval_quality` del grafo `.sentinel/agents/lemma-absence-agent`. Es el defecto que se uso para descartar dejar todo el lazo en el grafo, y se acepta por una sola razon: migrar lemma-absence es alcance ajeno y una feature nueva no puede romper una vieja. Se cancela cuando lemma-absence consuma `CandidateCriterionEvaluator`, y el puerto se diseño neutro para que eso sea una migracion y no un rediseño.

**El carrier y el deriver tienen nombres que mienten.** `LemmaAbsenceQuizCandidate` es el par quizSentence/traduccion y `LemmaAbsenceProposalDeriver` lo convierte en un snapshot respetando el `sentenceMode`; ninguno tiene nada propio de la ausencia de lema. Renombrarlos toca 25 archivos Java cuyos cuerpos escritos a mano el smart-merge no reescribe, y la traceability de sus tests pertenece a FEAT-LAPS, asi que el renombre completo va en un patch de esa feature.

---

# Tramo 2026-08-11 — El lazo cambia de dueño (R012–R015)

> **Que cambio y por que hay un tramo nuevo.** El diseño de arriba puso el lazo
> candidato → criterios → reintento **del lado del sistema**, y cada criterio con
> poder de veto. La primera corrida real sobre el plan entero midio lo que eso
> costaba: de 162 tareas sin corregir, **159 tenian tres candidatos producidos,
> juzgados y tirados**; los **318 reintentos rescataron cero tareas** y las 125
> propuestas exitosas salieron **todas al primer intento**. La decision del usuario
> —*"tenemos que tener la misma forma de hacer las cosas"*— alinea esta correccion
> con la generacion por ausencia de lema: quien corrige conduce su lazo, conserva su
> mejor candidato y al agotar los intentos lo **entrega**, diciendo lo que no cumple.
> Con eso el desenlace "sin corregir" pasa de 159 a 2.

## Cambiar el contrato de correccion: una solicitud, el mejor candidato de vuelta

El lazo se va a donde estan los candidatos porque **el mejor solo puede conservarlo
quien los tiene**: un lazo por fuera ve un candidato por vez y decide si lo acepta o
lo tira, y para elegir "el mejor de los tres" alguien tiene que recordar los tres.
Ademas el lazo de afuera solo sabe pedir otro candidato desde cero, mientras que quien
corrige puede partir de lo que ya escribio y tocar unicamente lo que el reproche
señala — que es lo que F-QICOR-R003 le pide de todas formas. El puerto deja entonces de
devolver *un* candidato y pasa a devolver *el mejor candidato mas su evaluacion*, para
que el sistema pueda declarar lo incumplido sin haber visto los intentos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: QuizInstructionProposalStrategy
        _change: modify
        sealed: false
        exposes:
          - signature: "propose(RefinementTask task, QuizInstructionCorrectionContext context, List<CriterionVerdict> previousVerdicts, int attempt): LemmaAbsenceQuizCandidate"
            _change: delete
          - signature: "propose(RefinementTask task, QuizInstructionCorrectionContext context, int maxAttempts): QuizInstructionBestCandidate"
            _change: add
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionBestCandidate
            _change: add
            type: record
            visibility: public
            fields:
              - { name: candidate, type: LemmaAbsenceQuizCandidate }
              - { name: reportedVerdicts, type: "List<CriterionVerdict>" }
              - { name: attempts, type: int }
```

## Pasar el tope de intentos hacia adentro y traer de vuelta lo que el lazo midio

`maxAttempts` viaja ahora **dentro** de la unica solicitud en vez de gobernar un bucle
de este lado. Que el numero lo fije la corrida y no quien corrige es lo que mantiene el
gasto acotado y previsible: sin un maximo declarado por fuera, una tarea dificil podria
intentar indefinidamente. En sentido inverso, `previousVerdicts`/`attempt` dejan de
tener sentido —eran el estado del lazo externo— y la respuesta gana los veredictos que
quien corrige obtuvo consultando el catalogo del sistema, mas cuantos intentos gasto.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionGenerationRequest
            _change: modify
            fields:
              - { name: previousVerdicts, _change: delete }
              - { name: attempt, _change: delete }
              - { name: maxAttempts, type: int, _change: add }
          - name: QuizInstructionGeneratorResponse
            _change: modify
            fields:
              - { name: verdicts, type: "List<CriterionVerdict>", _change: add }
              - { name: attempts, type: int, _change: add }
          - name: QuizInstructionCorrectionConfig
            _change: modify
            fields:
              - { name: maxAttempts, type: Integer, _change: modify }
```

## Convertir el catalogo de veto en orden, sin inventar un veredicto nuevo

La pregunta de fondo era como se expresa un criterio que **se mide, ordena y no veta**.
La respuesta es que el vocabulario de veredictos no cambia: `PASSED`/`FAILED`/
`NOT_EVALUABLE` siguen describiendo *que paso al medir*, que es ortogonal a *que
consecuencia tiene*. Inventar un cuarto valor ("reprobo pero no bloquea") habria metido
la politica adentro de la medicion y obligado a cada evaluador a saber si su criterio
decide — exactamente el acoplamiento que el puerto neutro evito. Lo que se reescribe es
la **consecuencia**, que vivia en las descripciones y ahora vive en la evaluacion.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        visibility: public
        models:
          - name: CriterionOutcome
            _change: modify
            type: enum
            fields:
              - { name: PASSED, _change: modify }
              - { name: FAILED, _change: modify }
              - { name: NOT_EVALUABLE, _change: modify }
          - name: CorrectionCriterion
            _change: modify
            type: enum
            fields:
              - { name: LENGTH_IN_RANGE, _change: modify }
              - { name: INSTRUCTION_COMPLIANCE, _change: modify }
```

## Dar a la evaluacion la forma que el orden necesita

`accepted` era la pregunta vieja —¿lo acepto?— y se cae con el veto. En su lugar la
evaluacion pasa a responder las preguntas del orden de F-QICOR-R013: si el candidato
**compite** (`eligible` = cumple su consigna y es un cambio real; un candidato que no
cumple no es un candidato peor, no es un candidato), **cuanto conserva**
(`satisfiedCriteria`, sin contar la longitud, que nunca suma), **que le falta**
(`unmetCriteria`, que es literalmente lo que la propuesta declara) y **cuanto mide**
(`lengthMeasurement`). `realChange` se expone aparte de `eligible` para que las dos
mitades de la elegibilidad se observen por separado en vez de colapsar en un booleano
que no dice cual fallo.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        models:
          - name: CandidateAssessment
            _change: modify
            type: record
            fields:
              - { name: accepted, _change: delete }
              - { name: verdicts, type: "List<CriterionVerdict>", _change: modify }
              - { name: eligible, type: boolean, _change: add }
              - { name: realChange, type: boolean, _change: add }
              - { name: satisfiedCriteria, type: int, _change: add }
              - { name: unmetCriteria, type: "List<CriterionVerdict>", _change: add }
              - { name: lengthMeasurement, type: CandidateLengthMeasurement, _change: add }
              - { name: rankKey, type: String, _change: add }
```

## Publicar el orden como una clave comparable, no como una regla que hay que copiar

El campeon lo conserva quien corrige, asi que el orden tiene que ser consumible desde
afuera — y si se lo deja escrito en el grafo, F-QICOR-R013 se queda sin superficie
Java que un test pueda referenciar, que es la vara con la que se decidio todo el diseño
anterior. `rankKey` resuelve las dos cosas: la regla vive una sola vez en Java, con su
test propio, y quien corrige la consume comparando **dos strings**. `better` existe
para que el desempate final de R013 (g) —gana el primero visto— sea una decision Java
testeable ("reemplaza al campeon solo ante clave estrictamente mayor") y no una
convencion que el grafo podria implementar al reves sin que nadie lo note.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        interfaces:
          - name: CandidateRanking
            _change: add
            stereotype: service
            visibility: public
            exposes:
              - signature: "rankKey(CandidateAssessment assessment): String"
              - signature: "better(String incumbentRankKey, String challengerRankKey): boolean"
              - signature: "precedence(): List<CorrectionCriterion>"
      - name: candidatecriteriaengine
        _change: modify
        visibility: internal
        implementations:
          - name: DefaultCandidateRanking
            _change: add
            implements: ["CandidateRanking"]
```

## Medir la longitud una sola vez, para el veredicto y para la declaracion

F-QICOR-R011 obliga a **declarar** la longitud medida y el rango del nivel en la
propuesta, y F-QICOR-R015 (b) exige que esa declaracion coincida con lo que dira el
analizador de longitud. Con la medicion enterrada adentro de `LengthInRangeCriterion`
solo salia un `detail` en texto libre: o los numeros no llegaban a la propuesta, o se
median por segunda vez en otro lado — que es el defecto que la regla existe para
impedir, un nivel mas adentro. El medidor se separa como puerto propio, el criterio
pasa a **traducir la medicion a veredicto** en vez de producirla, y la evaluacion
arrastra la misma medicion estructurada que despues se declara.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteria
        _change: modify
        models:
          - name: CandidateLengthMeasurement
            _change: add
            type: record
            visibility: public
            fields:
              - { name: tokens, type: int }
              - { name: targetMin, type: int }
              - { name: targetMax, type: int }
              - { name: inRange, type: boolean }
              - { name: measured, type: boolean }
        interfaces:
          - name: CandidateLengthMeter
            _change: add
            stereotype: port
            visibility: public
            exposes:
              - signature: "measure(CandidateAssessmentInput input): CandidateLengthMeasurement"
      - name: candidatecriteriaengine
        _change: modify
        implementations:
          - name: DefaultCandidateLengthMeter
            _change: add
            implements: ["CandidateLengthMeter"]
            requiresInject:
              - { name: tokenizer, type: NlpTokenizer }
              - { name: sentenceLengthConfig, type: SentenceLengthConfig }
          - name: LengthInRangeCriterion
            _change: modify
            implements: ["CandidateCriterionEvaluator"]
            requiresInject:
              - { name: tokenizer, _change: delete }
              - { name: sentenceLengthConfig, _change: delete }
              - { name: lengthMeter, type: CandidateLengthMeter, _change: add }
```

## Evaluar el catalogo entero por candidato, porque un orden necesita contar

El assessor cortaba al primer veredicto no-`PASSED`. Con el veto eso alcanzaba; con un
orden no: `satisfiedCriteria` es la primera clave de F-QICOR-R013 y un catalogo
evaluado a medias no permite contarlo. El costo es real y se acepta a ojos abiertos —
un candidato que reprueba el primer criterio ahora paga igual los de juicio— y se
compra a cambio de las 234 consultas de reintento que la corrida medida tiro a la
basura. El catalogo activo reducido sigue siendo la valvula: un criterio sin evaluador
registrado sale del catalogo en vez de aprobar por omision.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: candidatecriteriaengine
        _change: modify
        implementations:
          - name: DefaultCandidateAssessor
            _change: modify
            implements: ["CandidateAssessor"]
            requiresInject:
              - { name: evaluatorsByCriterion, type: "Map<CorrectionCriterion,CandidateCriterionEvaluator>" }
              - { name: lengthMeter, type: CandidateLengthMeter, _change: add }
              - { name: ranking, type: CandidateRanking, _change: add }
```

## Declarar en la propuesta lo que el ejercicio entregado no cumple

Entregar el mejor candidato solo es defendible si viaja con el reproche a la vista:
descartarlo protegia al curso a costa de no corregir nada, y entregarlo en silencio
seria peor, porque el operador aprobaria creyendo que aprueba una correccion limpia.
La declaracion va en `RevisionProposal` —no en el reporte de la corrida— porque tiene
que verse **antes** de aprobar, y se parte en dos campos a proposito: la longitud
nunca entra en la lista de criterios incumplidos, tiene su propia declaracion con la
medicion. Los dos campos son nulos/vacios para propuestas de otros diagnosticos, con
el mismo precedente que `strategyId`.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionProposal
        _change: modify
        fields:
          - { name: unmetCriteria, type: "List<CriterionVerdict>", _change: add }
          - { name: lengthMeasurement, type: CandidateLengthMeasurement, _change: add }
```

## Contar en la corrida las propuestas con reproches, sin agregar un quinto desenlace

El recuento es lo que vuelve **revisable** la concesion: si mañana casi todas las
propuestas llegaran con criterios sin cumplir, lo que hay que mirar es quien corrige y
no la regla, y sin el recuento nadie se enteraria. Son dos contadores y no uno porque
la longitud se retiro del veto por un argumento propio y merece su propio termometro.
El detalle por tarea se agrega para que los contadores sean **auditables** en vez de
pedirle al lector que les crea, y ninguno de los dos abre un desenlace nuevo: R008
sigue con sus cuatro.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionTaskOutcome
            _change: modify
            fields:
              - { name: failedCriteria, type: "List<CorrectionCriterion>", _change: modify }
              - { name: unmetCriteria, type: "List<CriterionVerdict>", _change: add }
              - { name: outOfLengthRange, type: boolean, _change: add }
          - name: QuizInstructionCorrectionRunReport
            _change: modify
            fields:
              - { name: proposedWithUnmetCriteria, type: int, _change: add }
              - { name: proposedOutOfLengthRange, type: int, _change: add }
```

## Acotar el fracaso a "no habia nada que entregar"

`NO_ACCEPTABLE_CANDIDATE` y `NOT_CORRECTED` significaban "ningun candidato paso el
catalogo". Ahora significan lo unico que quedo: **ningun candidato resulto
entregable** — o ninguno cumplio su consigna, o ninguno fue un cambio real. El cambio
es de descripcion y no de forma a proposito: los dos desenlaces siguen existiendo, con
su recuento separado y su lista de criterios reprobados, porque callarse sigue estando
prohibido. Lo que se achica es su poblacion: 159 tareas pasan a ser 2.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: RevisionOutcomeKind
        _change: modify
        type: enum
        fields:
          - { name: NO_ACCEPTABLE_CANDIDATE, _change: modify }
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: QuizInstructionTaskOutcomeKind
            _change: modify
            type: enum
            fields:
              - { name: NOT_CORRECTED, _change: modify }
```

## Un unico punto donde se arma el pedido de evaluacion

Esta es la pieza que sostiene F-QICOR-R015 (c) —"los criterios incumplidos que declara
la propuesta son exactamente los que la consulta devuelve"—. Si el subcomando armara
su propio `CandidateAssessmentInput`, el catalogo seguiria existiendo una vez pero el
**pedido** existiria dos, y bastaria con que uno poblara `signalledFragments` distinto
para que la consulta y la propuesta discreparan sin que nada lo detecte. El seam vive
en el paquete de la SPI de consigna y no en `candidatecriteria` porque ese puerto se
diseño deliberadamente neutro: es el adaptador el que sabe traducir un contexto de
correccion a un pedido de evaluacion. El reviser deja de inyectar el catalogo neutro y
pasa por el mismo objeto que responde la consulta.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        interfaces:
          - name: QuizInstructionCandidateAssessor
            _change: add
            stereotype: service
            visibility: public
            exposes:
              - signature: "assess(QuizInstructionCorrectionContext context, CourseElementSnapshot original, String candidateQuizSentence, String candidateTranslation): CandidateAssessment"
          - name: QuizInstructionCandidateAssessorFactory
            _change: add
            stereotype: factory
            visibility: public
            exposes:
              - signature: "create(CandidateAssessor assessor, LemmaAbsenceProposalDeriver deriver): QuizInstructionCandidateAssessor"
      - name: engine
        _change: modify
        visibility: internal
        implementations:
          - name: DefaultQuizInstructionCandidateAssessor
            _change: add
            implements: ["QuizInstructionCandidateAssessor"]
            requiresInject:
              - { name: assessor, type: CandidateAssessor }
              - { name: deriver, type: LemmaAbsenceProposalDeriver }
          - name: DefaultQuizInstructionCandidateAssessorFactory
            _change: add
            visibility: public
            implements: ["QuizInstructionCandidateAssessorFactory"]
          - name: QuizInstructionReviser
            _change: modify
            implements: ["Reviser"]
            requiresInject:
              - { name: assessor, _change: delete }
              - { name: candidateAssessor, type: QuizInstructionCandidateAssessor, _change: add }
```

## Exponer el catalogo como subcomando, para que se consulte en vez de copiarse

Sin este subcomando, mover el lazo significa que quien corrige estime por su cuenta si
entra en rango o si introdujo una palabra de otro nivel: elegiria su campeon con una
regla de medir y el sistema lo juzgaria con otra, que es el defecto que F-QICOR-R004
nombra como el peor posible. El precedente exacto ya existe —`eval_length.sh` sube
hasta `audit-cli.sh` y llama `content-audit tokens` para reusar el tokenizador de la
auditoria—, y esto es su equivalente para el catalogo completo. Recibe `--plan/--task`
en vez del ejercicio original porque **nada del original viaja por el agente y vuelve**:
el snapshot se toma del curso con el mismo locator que usa el motor, asi que la
comparacion de edicion acotada mide contra lo que hay, con sus partes vacias incluidas.
El comando no mide nada por su cuenta: resuelve, y delega en el seam.

```architecture
modules:
  - name: audit-cli
    _change: modify
    interfaces:
      - name: AssessCandidateCommand
        _change: add
        stereotype: port
        sealed: true
        exposes:
          - signature: "assessCandidate(String planId, String taskId, String candidateQuizSentence, String candidateTranslation, String coursePath, String format): Integer"
    packages:
      - name: commands
        _change: modify
        visibility: internal
        implementations:
          - name: AssessCandidateCmd
            _change: add
            implements: ["AssessCandidateCommand"]
            externalImplements: ["java.util.concurrent.Callable<Integer>"]
            requiresInject:
              - { name: refinementPlanStore, type: RefinementPlanStore }
              - { name: auditReportStore, type: AuditReportStore }
              - { name: correctionContextResolver, type: CorrectionContextResolver }
              - { name: courseRepository, type: CourseRepository }
              - { name: elementLocator, type: CourseElementLocator }
              - { name: candidateAssessor, type: QuizInstructionCandidateAssessor }
```

## Lo que este tramo deja afuera a proposito

**El grafo `quiz-instruction-corrector` no se toca desde el patch.** Sus nodos nuevos
—consultar, comparar campeones, decidir si reintenta— son datos en `.sentinel/agents/`,
no arquitectura; el patch le abre la puerta (`maxAttempts` hacia adentro, veredictos
hacia afuera, subcomando para consultar) y el cableado queda para desarrollo.

**No se decide DOUBT-REVERIFICACION.** El diseño deja las dos opciones abiertas como
decision de cableado y no de contrato: el sistema evalua al campeon entregado con el
mismo assessor, y que los evaluadores de juicio esten o no registrados en el catalogo
activo es lo que elige entre "rehacer todo" y "tomar del corrector lo caro". Ninguna de
las dos ramas necesita otro patch.

**No se toca `DefaultQuizInstructionSubjectBuilder`.** Ni `serializeParts` ni
`serializeQuiz`: su salida es el contenido sobre el que se calcula la huella de los
~13 000 veredictos del ledger, y este tramo no tiene ninguna razon para moverla.

**No se declaran `handwrittenTests`.** Los tres tests que F-QICOR-R012 deja sin
sustento —el que afirma que el reviser descarta su mejor candidato, el que afirma que
el lazo es del sistema y el que afirma que el assessor rechaza al primer no-PASSED—
tienen que darse vuelta o borrarse, y eso es trabajo de `@qa-tester`, no de este patch.
