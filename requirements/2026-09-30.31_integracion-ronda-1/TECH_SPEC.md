---
patch: FEAT-RONDA1
requirement: 2026-09-30.31_integracion-ronda-1
generated: 2026-09-30T22:30:00Z
---

# Tech Spec: Integración de la ronda 1 (FEAT-RONDA1)

Lo que comparten los catorce parches de la ronda 1 —A, reglas del ejercicio; C, jueces con modelo; D, vocabulario— y los
pedidos 1 a 3 de la UI, en un solo parche contra el `sentinel.yaml` real de la rama (0.2 y 0.3 aplicados). Sale de los 59
ganchos de `ronda1/ganchos-{A,C,D}.md`. Se aplica primero, y los catorce se rebasaron sobre él: el orden y la simulación
están al final. Todo lo que no cambia `sentinel.yaml` (el cableado de `Main` y los cuerpos que tocan archivos comunes)
queda listado en la penúltima sección, para que el equipo que construya la integración lo haga una sola vez.

## Dar a cada analizador nuevo su lugar tipado en el nodo
El contrato del hallazgo dejó el diagnóstico tipado como decisión del proyecto (DLABS): cada analizador guarda lo suyo en
un lugar con su tipo, no en un mapa. Los nueve lugares de ejercicio y los dos de curso van juntos acá porque las dos
interfaces las tocan once parches, y un solo cambio evita once modificaciones de la misma interfaz. La cobertura de los
jueces es una lista común (gancho C-04) y no un lugar por juez: la de opción múltiple que pedía A-05 no traía examen, costo
ni causa, y así el curso publica todas igual. Las firmas nombran tipos que declaran los parches de los paquetes, así que
este parche es el primero pero el `generate` va recién con la ronda entera aplicada.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizDiagnoses
        _change: modify
        exposes:
          - signature: "getHintCoherenceDiagnosis(): Optional<HintCoherenceDiagnosis>"
            _change: add
          - signature: "getAnswerCompletenessDiagnosis(): Optional<AnswerCompletenessDiagnosis>"
            _change: add
          - signature: "getMultipleChoiceValidityDiagnosis(): Optional<MultipleChoiceValidityDiagnosis>"
            _change: add
          - signature: "getDataCoherenceDiagnosis(): Optional<DataCoherenceDiagnosis>"
            _change: add
          - signature: "getExactDuplicateDiagnosis(): Optional<ExactDuplicateDiagnosis>"
            _change: add
          - signature: "getTranslationSenseDiagnosis(): Optional<TranslationSenseDiagnosis>"
            _change: add
          - signature: "getSingleAnswerDiagnosis(): Optional<SingleAnswerDiagnosis>"
            _change: add
          - signature: "getMiniTheoryDiagnosis(): Optional<MiniTheoryDiagnosis>"
            _change: add
          - signature: "getOffLevelWordsDiagnosis(): Optional<OffLevelWordsDiagnosis>"
            _change: add
      - name: CourseDiagnoses
        _change: modify
        exposes:
          - signature: "getJudgeCoverages(): List<JudgeCoverageDiagnosis>"
            _change: add
          - signature: "getLemmaRecurrenceDiagnosis(): Optional<LemmaRecurrenceCourseDiagnosis>"
            _change: add
```

## Llevar al informe la miniteoría y la clave del audio
Respuestas completas, el juez de miniteoría, Datos coherentes y la vista del alumno leen dos datos que el curso tiene y el
informe pierde: la miniteoría de cada ejercicio y la clave de su audio. Van en `AuditableQuiz` y los copia tal cual el
mapeador, en la misma pasada que ya copia la consigna; no se derivan de nada, así que no mueven ninguna huella. El
constructor generado crece de 10 a 12 argumentos: se ajustan sus llamados como se hizo con 0.2.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditableQuiz
        _change: modify
        fields:
          - { name: miniTheory, type: String, _change: add }
          - { name: answerAudioKey, type: String, _change: add }
```

## Armar el ejercicio como lo ve el alumno en un solo lugar
Siete analizadores y jueces nuevos, el resumen y un verbo necesitan el enunciado con el hueco y la pista, las respuestas,
las opciones y el ejercicio terminado; si cada uno lo arma a su modo, dos jueces miden cosas distintas y la evidencia
vuelve al formato interno (F-RONDA1-R001). Es un puerto con una implementación sin estado, que reusa el conversor de
oraciones del curso para el ejercicio terminado. Tiene que ser determinista porque cuatro jueces calculan su huella sobre
él: cambiar su texto después de pagar vuelve a cobrar hasta 34.274 consultas (gancho C-07).

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: QuizStudentViewRenderer
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "render(AuditableQuiz quiz): QuizStudentView"
          - signature: "finishedSentence(AuditableQuiz quiz,SentenceMode mode): String"
    packages:
      - name: studentview
        _change: add
        visibility: public
        models:
          - name: QuizStudentView
            _change: add
            type: record
            fields:
              - { name: statement, type: String }
              - { name: gaps, type: "List<StudentGapView>" }
              - { name: options, type: "List<StudentOptionView>" }
              - { name: instructions, type: String }
              - { name: translation, type: String }
              - { name: miniTheory, type: String }
          - name: StudentGapView
            _change: add
            type: record
            fields:
              - { name: index, type: int }
              - { name: hint, type: String }
              - { name: acceptedAnswers, type: "List<String>" }
              - { name: firstAnswer, type: String }
          - name: StudentOptionView
            _change: add
            type: record
            fields:
              - { name: label, type: String }
              - { name: correct, type: boolean }
        implementations:
          - name: DefaultQuizStudentViewRenderer
            _change: add
            visibility: public
            implements: [QuizStudentViewRenderer]
            requiresInject:
              - { name: quizSentenceConverter, type: QuizSentenceConverter }
```

## Nombrar cada ejercicio del resumen por su enunciado
La UI necesita distinguir los veinte ejercicios de un tema, y hoy todos llevan el título del tema (pedido 2 de G). La
etiqueta del informe completo no se toca: el juez de consigna la lee como título y cambiarla le volvería a cobrar sus
13.000 veredictos. Por eso la cambia sólo el almacén, al escribir el resumen, con el renderizador (F-RONDA1-R002).

```architecture
modules:
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemAuditReportStore
        _change: modify
        requiresInject:
          - { name: studentViewRenderer, type: QuizStudentViewRenderer, _change: add }
```

## Devolver un tema entero como lo ve el alumno
La segunda mitad del pedido 2: un verbo que la UI llama por tema, `get student-view <tema>` (F-RONDA1-R003). La consulta es
pura —recibe el informe y el tema, y usa el renderizador— para que `GetCmd` sólo decida qué análisis leer: el elegido con
`set-active-analysis`, o el último. El texto con que se grabó cada audio sale del registro de grabaciones de Datos
coherentes, y si no lo conoce queda vacío: nunca se inventa desde la oración.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: TopicStudentViewQuery
        _change: add
        stereotype: port
        sealed: false
        exposes:
          - signature: "viewOf(AuditReport report,String knowledgeId): TopicStudentView"
            throwsExceptions: [StudentViewKnowledgeNotFoundException]
    packages:
      - name: studentview
        _change: add
        visibility: public
        models:
          - name: TopicStudentView
            _change: add
            type: record
            fields:
              - { name: auditId, type: String }
              - { name: knowledgeId, type: String }
              - { name: label, type: String }
              - { name: instructions, type: String }
              - { name: sentenceMode, type: SentenceMode }
              - { name: quizzes, type: "List<StudentQuizEntry>" }
          - name: StudentQuizEntry
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: view, type: QuizStudentView }
              - { name: finishedSentence, type: String }
              - { name: answerAudioKey, type: String }
              - { name: recordedAudioText, type: String }
          - name: StudentViewKnowledgeNotFoundException
            _change: add
            type: exception
            fields:
              - { name: knowledgeId, type: String }
        implementations:
          - name: DefaultTopicStudentViewQuery
            _change: add
            visibility: public
            implements: [TopicStudentViewQuery]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: recordings, type: AudioRecordingRegistry }
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            requiresInject:
              - { name: topicStudentViewQuery, type: TopicStudentViewQuery, _change: add }
              - { name: activeAnalysisSelectionStore, type: ActiveAnalysisSelectionStore, _change: add }
```

## Poner el nombre en castellano en la ficha
Sin ese nombre la UI necesitaría una tabla de nombres por analizador, justo lo que la capa nueva quiere sacar (pedido 1
de G). Es un campo más de la ficha y no reemplaza al nombre único, que sigue siendo el único con que se nombra al
analizador (F-HALL-R005). El valor lo pone cada proveedor; la tabla de los diecisiete está en F-RONDA1-R004.

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AnalyzerDescriptor
        _change: modify
        fields:
          - { name: displayName, type: String, _change: add }
```

## Contar por gravedad y por regla, también en vocabulario
La UI dibuja «más cortas · en la meta · más largas» y el color de cada etiqueta del nivel, y no puede calcularlos (pedido
3 de G; F-HALL-R010). Es una lista aparte y no campos nuevos en `AnalyzerErrorCounts`, porque vale para las dos familias y
cuenta por tipo de nodo evaluado: un analizador de vocabulario que evalúa ejercicio y nivel da dos filas. La calcula el
mismo calculador de 0.3, una sola vez; que el largo de oración separe «más corta» de «más larga» en dos reglas es un cambio
de su ficha, sin estructura nueva (F-RONDA1-R005 inv. 4).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: contextnumbers
        _change: modify
        models:
          - name: ContextNumbers
            _change: modify
            fields:
              - { name: findingCounts, type: "List<AnalyzerFindingCounts>", _change: add }
          - name: AnalyzerFindingCounts
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: family, type: AnalyzerFamily }
              - { name: target, type: AuditTarget }
              - { name: reached, type: int }
              - { name: evaluated, type: int }
              - { name: withFinding, type: int }
              - { name: findings, type: int }
              - { name: bySeverity, type: SeverityCounts }
              - { name: byRule, type: "List<RuleFindingCount>" }
          - name: RuleFindingCount
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: withFinding, type: int }
              - { name: findings, type: int }
```

## Contar el ejercicio evaluado a medias
Con el juez adentro de Opción múltiple, un ejercicio puede tener sus reglas corridas y el juez pendiente, y el contrato de
0.3 sólo conocía evaluado o sin evaluar. José dejó la recomendación A de DOUBT-OMVAL-JUEZ-A-MEDIAS (F-RONDA1-R006): el nodo
cuenta como evaluado, sus hallazgos valen y declara al analizador «a medias», así «algún error» se lee como piso. Es un
campo en el nodo, su copia en el resumen y un conteo por analizador; el colector acepta hallazgos en un nodo a medias y
el calculador lo cuenta en `notFullyEvaluated` (C-11 queda cubierto por lo mismo).

```architecture
modules:
  - name: audit-domain
    _change: modify
    models:
      - name: AuditNode
        _change: modify
        fields:
          - { name: partiallyEvaluatedBy, type: "List<String>", _change: add }
    packages:
      - name: contextnumbers
        _change: modify
        models:
          - name: AnalyzerErrorCounts
            _change: modify
            fields:
              - { name: partiallyEvaluated, type: int, _change: add }
          - name: DigestNode
            _change: modify
            fields:
              - { name: partiallyEvaluatedBy, type: "List<String>", _change: add }
```

## Un solo contrato para el juez de opción múltiple, en dos tiempos
A esperaba `judge()` por ejercicio (A-07) y C ofrecía pedir todos en `onQuiz` y resolverlos juntos en `onCourseComplete`
(C-09). Queda el de C: con un veredicto por llamada dentro de `onQuiz` las consultas salen de a una, y José eligió 4 a la
vez (F-JEXAM-R006); además su juicio trae la gravedad de cada opción y la clave invertida, que F-MCVALE-R002 y R003 piden
y `AlsoValidOption` no llevaba. Vive acá, y no en uno de los dos paquetes, porque es la frontera entre ellos: C lo
implementa y A lo usa, y así ninguno de los dos parches depende del otro. A cede: su parche pierde sus dos interfaces y
cuatro modelos, y cambian los tipos de dos inyecciones, un campo del diagnóstico y el nombre de la fábrica «sin juez». El
puntaje del ejercicio es el de A, 0 o 1; el del juez queda como dato (C-10).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: choicealternative
        _change: add
        visibility: public
        interfaces:
          - name: ChoiceAlternativeJudgeRunFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "judgeRule(): AnalyzerRuleCard"
              - signature: "open(EvaluationRunPolicy policy): ChoiceAlternativeJudgeRun"
                throwsExceptions: [EmptyReevaluationSetException, AmbiguousReevaluationScopeException]
          - name: ChoiceAlternativeJudgeRun
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "request(AuditNode quizNode): void"
              - signature: "resolveAll(): void"
              - signature: "judgmentOf(AuditNode quizNode): ChoiceAlternativeJudgment"
              - signature: "coverage(): JudgeCoverageDiagnosis"
        models:
          - name: ChoiceAlternativeOption
            _change: add
            type: record
            fields:
              - { name: label, type: String }
              - { name: severity, type: FindingSeverity }
              - { name: reason, type: String }
          - name: ChoiceAlternativeVerdict
            _change: add
            type: record
            fields:
              - { name: correctIsValid, type: boolean }
              - { name: validAlternatives, type: "List<ChoiceAlternativeOption>" }
              - { name: correctProblem, type: String }
              - { name: rationale, type: String }
          - name: ChoiceAlternativeJudgmentStatus
            _change: add
            type: enum
            fields:
              - name: JUDGED
              - name: PENDING
              - name: FAILED
              - name: NOT_AVAILABLE
          - name: ChoiceAlternativeJudgment
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: status, type: ChoiceAlternativeJudgmentStatus }
              - { name: verdict, type: ChoiceAlternativeVerdict }
              - { name: droppedAsForm, type: "List<String>" }
              - { name: findings, type: "List<FindingDraft>" }
              - { name: score, type: Double }
              - { name: evaluatorVersion, type: String }
              - { name: reused, type: boolean }
              - { name: notJudgedReason, type: String }
```

## Darle a analyze el conjunto por juez y las consultas a la vez
Con cinco jueces, «re-evaluá estos ejercicios» tiene que decir de qué juez, y las consultas a la vez tienen que poder
bajarse o subirse sin tocar código (gancho C-17). Son dos campos de las opciones, que el comando traduce a la política de
cada juez de la corrida; `--reevaluate-instruction-quizzes` y `--instruction-budget` quedan como atajos del de consigna.

```architecture
modules:
  - name: audit-cli
    _change: modify
    models:
      - name: AnalyzeOptions
        _change: modify
        fields:
          - { name: analyzerReevaluationSets, type: "Map<String,Set<String>>", _change: add }
          - { name: maxConcurrentEvaluations, type: Integer, _change: add }
```

## Leer la configuración de los jueces nuevos del entorno
Los cuatro jueces nuevos comparten una configuración (`CONTENT_AUDIT_JUDGE_*`, por omisión `claude-cli`), y el de consigna
sigue con la suya (gancho C-16). Va como los otros resolvers del arranque: recibe el entorno como mapa, así se prueba sin
variables reales, y una configuración ausente o inválida da un evaluador no disponible, nunca un arranque caído.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        interfaces:
          - name: AgentJudgeConfigResolver
            _change: add
            exposes:
              - signature: "resolve(Map<String,String> env): AgentJudgeConfig"
        implementations:
          - name: DefaultAgentJudgeConfigResolver
            _change: add
            visibility: public
            implements: [AgentJudgeConfigResolver]
```

## Cablear el semáforo en get y la recurrencia en stats
Son dos inyecciones en comandos que comparten todos los paquetes: `get word-lights` (gancho D-05) necesita la consulta del
semáforo, y `stats analyzer lemma-recurrence` el formateador detallado que ya usa `analyze --detailed` (D-11 y D-15, que
F-RECUR-R003 pide y ningún gancho cableaba).

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: commands
        _change: modify
        implementations:
          - name: GetCmd
            _change: modify
            requiresInject:
              - { name: topicWordLightsQuery, type: TopicWordLightsQuery, _change: add }
          - name: StatsAnalyzerCmd
            _change: modify
            requiresInject:
              - { name: detailedFormatters, type: "Map<String,DetailedFormatter>", _change: add }
```

## Decir que cada palabra escasa tiene el umbral de su banda
Con las bandas 8 · 6 · 4 · 3, que José confirmó como valor por omisión, deja de ser cierto que el umbral de una palabra
escasa es el mismo para todas (gancho D-07). Cambia sólo la descripción del campo; el código que lo llena lo prueban las
pruebas de FEAT-POCOV.

```architecture
modules:
  - name: refiner-domain
    _change: modify
    models:
      - name: ScarceContentWord
        _change: modify
        fields:
          - { name: threshold, type: Integer, _change: modify }
```

## Lo que se construye sin tocar sentinel.yaml
El resto de los ganchos es código en archivos comunes, y se hace una vez, con la integración:
`Main` construye el renderizador, la consulta del tema, el registro de grabaciones, la tabla y la comparación del
corrector, el catálogo de formas irregulares, la copia de producción, el runtime común de los jueces, sus evaluadores y
el resolver de su configuración, y registra los diecisiete proveedores en el orden de F-RONDA1-R004 (con el juez de C en
Opción múltiple, no la fábrica «sin juez») y el comando `exam`; el mapeador copia la miniteoría y la clave del audio;
`DefaultQuizDiagnoses` y `DefaultCourseDiagnoses` suman sus campos; el motor corre en la corrida base los analizadores con
alguna regla instantánea, con tope 0 (F-RONDA1-R007) y pide hallazgos después de `onCourseComplete`; el colector y el
calculador cuentan lo a medias y los conteos nuevos; el plan declara `off-level-words` cubierto por LEMMA_ABSENCE
(F-NIVFR-R006); y el registro de listas del diff consolidado declara las listas nuevas. **Ningún** tipo de tarea nuevo
en `DiagnosisKind`: es del paquete E, y el plan sigue en 4.036 (F-RONDA1-R008). Los proveedores de C y D pasaron a
`implements AnalyzerProvider` en sus propios parches, al rebasar, porque ahí se declaran.

```architecture
features:
  - id: FEAT-RONDA1
    code: F-RONDA1
modules:
  - name: audit-cli
    _change: modify
```

## Devolver los clientes de cuatro paquetes, en un parche aparte al final
Cuatro paquetes declararon quién puede usarlos: `findingengine` de 0.3 y, en esta ronda, `judgingengine`, `offlevelwords`
y `topicwords`. El `sentinel.yaml` real ya no tiene los de `findingengine`: el `patch apply` del jar de hoy descarta los
`allowedClients` de un paquete al crearlo, y también al modificarlo, porque se queda con los que había. Probado en
memoria: un parche que sólo los modifica valida con 0 conflictos y no deja nada. Por eso van en un parche aparte,
`clientes-de-paquetes.yaml` en esta carpeta, hecho con `patch propose -o` y sin requirement propio: toca sólo esos cuatro
`allowedClients`. Es el paso 16 de la ronda y queda pendiente hasta arreglar `PatchApplier` en Sentinel, que son dos
líneas (borrador del bug 09 en el scratchpad del integrador). Con el arreglo, la cadena da el mismo modelo que con el jar
salvo esos cuatro campos, `generate` no aborta y `ArchUnitGenerator` vuelve a generar las cuatro reglas. Si se arregla
antes de aplicar la ronda, los tres paquetes nuevos ya entran con sus clientes; el resultado final es el mismo. No hay
fence: el parche aparte no está en `architectural_patch.yaml`.

## Declarar las 43 pruebas de FEAT-RONDA1
Las 43 son nuevas y cubren las ocho reglas. No había pruebas que portar, porque todo lo que piden las reglas es de la
integración. Van sobre implementaciones que existen cuando se aplica este parche, que es el primero de la ronda: las que
declara el parche y las que ya están en `sentinel.yaml`. Ninguna va sobre una pieza de un paquete, que todavía no
existiría. Cada nombre lleva un caso real del curso del 29/9 o el número que fija su regla. Se sumaron con `patch
propose` en modo fusión, y la comparación del parche antes y después, sin contar las pruebas, dio 0 pérdidas. Ninguna de
las implementaciones que tocó la fusión tenía `domainLinks`.

| Regla | Pruebas | Dónde | Caso real o número |
|---|---:|---|---|
| R001 | 8 | renderizador | «You ___ (see) cats.» con su pista, su respuesta y su traducción; «\|» y no «/»; «The show started ___ time.» con *on* correcta; la reescritura de *Adjetivos atributivos*; mismo texto en dos corridas |
| R002 | 3 | almacén de informes | el resumen dice «You ___ (see) cats.» y no «Afirmativas regulares»; el informe completo no cambia |
| R003 | 8 | consulta del tema (4) y `get` (4) | los 20 de *Afirmativas regulares*; la clave A1.01.06.01; audio vacío si el registro no lo conoce; tema que no está, código 1; no escribe nada |
| R004 | 5 | catálogo (2), `get`, `analyze`, `Main` | ficha sin nombre o con nombre repetido, rechazada; «Largo de oración» junto a `sentence-length`; los 17 en orden |
| R005 | 7 | calculador (4), ficha y analizador del largo (3) | las gravedades suman los marcados; las reglas suman los hallazgos; «He isn't in the living-room.» es «más larga» |
| R006 | 5 | calculador (2), colector (2), almacén | «___ a child? (He)» con el juez pendiente: cuenta, y el nivel lo declara a medias |
| R007 | 3 | motor | la corrida base corre `multiple-choice-validity` con tope 0 y deja afuera a los jueces sin reglas instantáneas |
| R008 | 4 | runner (2), plan, `analyze` | 73,86 % con 8 · 6 · 4 · 3 y 73,92 % con 4 para todas; 4.036 tareas; los 886 archivos idénticos |

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: studentview
        implementations:
          - name: DefaultQuizStudentViewRenderer
            handwrittenTests:
              - name: "should finish «The show started ___ time.» with its correct option, as «The show started on time.»"
                traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R001 }
          - name: DefaultTopicStudentViewQuery
            handwrittenTests:
              - name: "should leave the recorded audio text empty when the recordings registry does not know the key, never building it from the sentence"
                traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R003 }
      - name: findingengine
        implementations:
          - name: DefaultAnalyzerCatalog
            handwrittenTests:
              - name: "should reject at startup a card without a Spanish name with InvalidAnalyzerCardException"
                traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R004 }
          - name: DefaultContextNumbersCalculator
            handwrittenTests:
              - name: "should count the nodes with findings by their highest severity, so the four severities add up to the nodes with findings"
                traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R005 }
          - name: DefaultFindingCollector
            handwrittenTests:
              - name: "should accept the findings of an analyzer on an exercise it evaluated halfway, as multiple-choice-validity on «___ a child? (He)» with its judge pending"
                traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R006 }
    implementations:
      - name: IAuditEngine
        handwrittenTests:
          - name: "should leave quiz-instruction, translation-sense, single-answer and mini-theory out of the base run, since none has an instant rule"
            traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R007 }
  - name: audit-infrastructure
    _change: modify
    implementations:
      - name: FileSystemAuditReportStore
        handwrittenTests:
          - name: "should label each exercise of the digest with its statement as the student sees it, «You ___ (see) cats.» and not «Afirmativas regulares»"
            traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R002 }
  - name: refiner-domain
    _change: modify
    implementations:
      - name: DefaultRefinerEngine
        handwrittenTests:
          - name: "should derive from the 29/9 analysis with the seventeen analyzers of round 1 the same 4,036 tasks, declaring the scores below 1 of the new errors analyzers as without task kind and those of off-level-words as covered by LEMMA_ABSENCE"
            traceability: { feature: FEAT-RONDA1, rule: F-RONDA1-R008 }
```

## Aplicar la ronda en orden, y generar al final
Orden: esta integración; los cinco de A (PISTA, RESPC, OMVAL, DATOS, DUPEX); JEXAM y después los otros cuatro de C
(TRSEN, UNICA, MINIT, MCVALE); D con SEMAF después de NIVFR y POCOV (NIVFR, POCOV, SEMAF, RECUR); y los clientes de los
cuatro paquetes, cuando Sentinel los conserve. A va antes que C porque la comparación del corrector es de RESPC y la usan
dos jueces; JEXAM antes que TRSEN porque éste suma pruebas a una pieza de JEXAM. Simulado con la librería del jar, cada
`patch validate` pasa contra el modelo que deja el anterior, con 0 conflictos. Sobre el modelo final, `generate` no
aborta: no hay choques de nombre ni tipos sin resolver, y las 499 pruebas de la ronda están trazadas (456 de los
paquetes y 43 de esta feature). Esta feature no trae journeys, así que `generate` no espera ubicaciones.

```architecture
features:
  - id: FEAT-RONDA1
    code: F-RONDA1
modules:
  - name: audit-domain
    _change: modify
```
