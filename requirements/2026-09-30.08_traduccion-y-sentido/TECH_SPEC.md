---
patch: FEAT-TRSEN
requirement: 2026-09-30.08_traduccion-y-sentido
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: Juez de traducción y sentido (FEAT-TRSEN)

Paquete C, ronda 1. El analizador `translation-sense` juzga cada ejercicio como queda, sin compararlo con un original,
con dos jueces que no se hablan. Todo lo que no es propio de este juez viene de otros parches: de **0.3 (FEAT-HALL)**
`AnalyzerProvider` (el proveedor lo implementa), la ficha (`AnalyzerDescriptor` con
reglas, familia, niveles evaluados y costo), `AnalyzerRuleCard`, `AnalyzerPlanBinding`, `FindingDraft` y
`FindingSeverity`; de **FEAT-JEXAM** el runtime común (`JudgeRuntime`, `JudgeSessions`, `JudgeUnitTally`), el evaluador
genérico por agente y la cobertura común; del integrador, el render `QuizStudentViewRenderer` (gancho A-01); de **0.2
(FEAT-OPMUL)** la oración guardada de la opción múltiple, que ya trae la opción correcta en el hueco. Se aplica después
de FEAT-JEXAM. Rebasado el 30/9 sobre el `sentinel.yaml` real, con 0.2 y 0.3 aplicados, y sobre el parche de integración de la ronda 1 (FEAT-RONDA1), que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño.

**Fuera del parche** (`ronda1/ganchos-C.md`): la ranura del diagnóstico en `QuizDiagnoses`, la constante
`TRANSLATION_SENSE` de `DiagnosisKind` si el paquete E decide que produce tareas, y el cableado en `Main` de los dos
evaluadores (`AgentJudgeFactory` con dos specs) y del proveedor. **Las dos definiciones de agente** que escribe quien
construye: `.sentinel/agents/translation-sense-from-english/` y `.sentinel/agents/translation-sense-from-translation/`,
con el grafo de tres nodos del juez de consigna (juzgar → validar forma → emitir), las cinco entradas del mapa de
contenido como `dependencies` y un artifact `translationSenseVerdict`; si agotan sus intentos emiten
`{"outputUnusable": true}`. Las guías usan la vara de «traducción fiel» y de «sentido en el mundo real» de
`candidate-judgment` en modo absoluto, no reprueban por estilo ni por sinónimos válidos, y nunca tratan una forma
contraída o larga como problema (F-JEXAM-R012). Costo: examen fijo 102 consultas, práctica sobre `escribir/` 1.170,
curso entero 22.574 (A1 4.804).

## Registrar el analizador con un proveedor y su ficha

Desde 0.3 un analizador existe si y sólo si tiene un proveedor registrado; el catálogo, `get analyzers`, `config`,
`stats`, la validación de `analyze`, el tope por nombre y el plan salen de él. La ficha declara familia errores, costo
modelo pago, niveles evaluados `[QUIZ]`, resoluciones mesa y sólo ordena, y cinco reglas: `same-meaning`,
`native-would-say`, `words-go-together`, `level-register` y `judges-disagree`. La ligadura al plan nombra
`TRANSLATION_SENSE`: si el paquete E todavía no agregó esa constante, el plan lo declara en vez de perder las tareas
(F-HALL-R004 inv. 3); en esta ronda no hay constante (F-RONDA1-R008). El proveedor implementa `AnalyzerProvider`: se
había declarado como clase con sus cinco firmas porque el puerto todavía no estaba en `sentinel.yaml`, y al rebasar sobre
0.3 pasó a `implements` (gancho C-01), como los proveedores de A y D.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsenseengine
        _change: add
        visibility: public
        implementations:
          - name: TranslationSenseAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: runtime, type: JudgeRuntime }
              - { name: fromEnglishJudge, type: Evaluator }
              - { name: fromTranslationJudge, type: Evaluator }
              - { name: verdictReader, type: TranslationSenseVerdictReader }
              - { name: config, type: TranslationSenseConfig }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Dos jueces que no se hablan, con la misma huella

Los dos jueces son dos evaluadores propios en el libro, así el veredicto de uno nunca pisa al del otro y cada uno se
reusa por separado (F-TRSEN-R003). Leen el mismo mapa de contenido, así que la huella es una sola y la clave difiere sólo
por el evaluador. El mapa es exactamente lo que dice F-TRSEN-R001, en este orden: `cefrLevel`, `instructions` —la
consigna del ejercicio, la que muestra la app—, `exercise` —el enunciado con su pista—, `acceptedSentences` —la oración
completa con cada respuesta aceptada, o con la correcta en opción múltiple— y `translation`. El enunciado sale de
`QuizStudentView`, el render del integrador; las oraciones, de `AuditableQuiz.sentences`. No entran la miniteoría, el
audio, las opciones incorrectas ni otros ejercicios: cambiarlos no re-cobra nada.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsenseengine
        _change: add
        visibility: public
        interfaces:
          - name: TranslationSenseSubjectBuilder
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "build(AuditNode quizNode): EvaluationSubject"
        implementations:
          - name: DefaultTranslationSenseSubjectBuilder
            _change: add
            implements: [TranslationSenseSubjectBuilder]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## El veredicto de cada juez: cuatro condiciones, cada una con su gravedad

El veredicto se tipa en vez de guardarse como texto porque la combinación, el puntaje y la evidencia dependen de qué
condición falla y con qué gravedad (F-TRSEN-R002, R004): alta si enseña algo falso, media si se entiende pero está mal,
baja si sólo es el registro; nunca bloqueante, porque la traducción se ve después de contestar. Cada falla trae el
fragmento que falla y una línea en castellano, que es lo que la evidencia del hallazgo muestra (F-HALL-R003). El lector
del JSON vive en la infraestructura de los jueces, junto al del juez de consigna, y es público porque el analizador lee
veredictos reusados que nunca pasan por el evaluador.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsense
        _change: add
        visibility: public
        models:
          - name: TranslationSenseCondition
            _change: add
            type: enum
            fields:
              - name: SAME_MEANING
              - name: NATIVE_WOULD_SAY
              - name: WORDS_GO_TOGETHER
              - name: LEVEL_REGISTER
          - name: TranslationSenseFailure
            _change: add
            type: record
            fields:
              - { name: condition, type: TranslationSenseCondition }
              - { name: severity, type: FindingSeverity }
              - { name: fragment, type: String }
              - { name: evidence, type: String }
          - name: TranslationSenseVerdict
            _change: add
            type: record
            fields:
              - { name: passes, type: boolean }
              - { name: failures, type: "List<TranslationSenseFailure>" }
              - { name: rationale, type: String }
        interfaces:
          - name: TranslationSenseVerdictReader
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "read(String payload): TranslationSenseVerdict"
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: translationsenseverdict
        _change: add
        visibility: public
        implementations:
          - name: JacksonTranslationSenseVerdictReader
            _change: add
            visibility: public
            implements: [TranslationSenseVerdictReader]
```

## Combinar los dos veredictos: error, marcado para la bandeja o pasa

Es la decisión de José del 30/9 (DOUBT-TRSEN-COMBINAR, opción C): los dos «no» dan un hallazgo por cada condición que
marcó alguno, con la gravedad más alta de los dos, y cuentan como error; si no coinciden, un solo hallazgo
`judges-disagree` que sólo ordena, con los dos veredictos en la evidencia, y el ejercicio queda con puntaje 1 y marcado
para la bandeja. Así «algún error» no se infla con desacuerdos y el que desempata los ve. La escala 1 / 0,8 / 0,6 / 0,3
vive en la configuración, como la del juez de consigna, para que el examen pueda moverla sin tocar el combinador.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: TranslationSenseConfig
        _change: add
        stereotype: port
        sealed: false
        extends: [SelfDescribingConfig]
        exposes:
          - signature: "getScoreFor(FindingSeverity severity): double"
    packages:
      - name: translationsense
        _change: add
        visibility: public
        models:
          - name: TranslationSenseAgreement
            _change: add
            type: enum
            fields:
              - name: BOTH_PASS
              - name: BOTH_FAIL
              - name: SPLIT
      - name: translationsenseengine
        _change: add
        visibility: public
        models:
          - name: TranslationSenseOutcome
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: agreement, type: TranslationSenseAgreement }
              - { name: findings, type: "List<FindingDraft>" }
              - { name: score, type: double }
        interfaces:
          - name: TranslationSenseCombiner
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "combine(TranslationSenseVerdict fromEnglish,TranslationSenseVerdict fromTranslation): TranslationSenseOutcome"
        implementations:
          - name: DefaultTranslationSenseCombiner
            _change: add
            implements: [TranslationSenseCombiner]
            requiresInject:
              - { name: config, type: TranslationSenseConfig }
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultTranslationSenseConfig
        _change: add
        visibility: public
        implements: [TranslationSenseConfig]
        types: [Component]
```

## Repartir el tope de a pares y resolver los dos jueces a la vez

El tope cuenta las consultas de los dos jueces (F-TRSEN-R005), y un ejercicio sólo está evaluado con los dos veredictos:
si cada juez gastara el tope por su cuenta, con tope 10 y veinte ejercicios nuevos el primero consultaría diez y el
segundo ninguno, y no quedaría ninguno evaluado. Por eso el analizador junta los pedidos en `onQuiz` y en
`onCourseComplete` recorre los ejercicios en orden, pregunta a cada sesión si ya tiene resultado (`recorded`, que no
cuenta) y asigna consultas mientras alcance el tope, de a pares como las haría una persona de a una; lo no asignado se
pide como sólo reuso. Después resuelve un juez y el otro con `resolveAll` y las consultas a la vez del runtime, combina,
y escribe en cada ejercicio el puntaje, el diagnóstico y, si le falta algún veredicto, `unevaluatedBy`. Con el examen
aprobado y tope 0 no consulta: reusa y declara el costo de lo pendiente.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsenseengine
        _change: add
        visibility: public
        implementations:
          - name: TranslationSenseAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: subjectBuilder, type: TranslationSenseSubjectBuilder }
              - { name: combiner, type: TranslationSenseCombiner }
              - { name: verdictReader, type: TranslationSenseVerdictReader }
```

## Dejar los dos veredictos a la vista en el diagnóstico del ejercicio

El hallazgo lleva lo que falla; el diagnóstico lleva los dos veredictos enteros, su acuerdo y si se reusaron, que es lo
que necesita quien desempata en la bandeja y lo que muestra el detalle del ejercicio. Su lugar en `QuizDiagnoses` es un
gancho del integrador, porque las ranuras tipadas son compartidas (decisión de José sobre el cajón tipado, 0.3).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsense
        _change: add
        visibility: public
        models:
          - name: TranslationSenseDiagnosis
            _change: add
            type: record
            fields:
              - { name: fromEnglish, type: TranslationSenseVerdict }
              - { name: fromTranslation, type: TranslationSenseVerdict }
              - { name: agreement, type: TranslationSenseAgreement }
              - { name: score, type: double }
              - { name: reusedFromPreviousRun, type: boolean }
```

## Registrar FEAT-TRSEN

Al aplicar, Sentinel copia de REQUIREMENT.md la compuerta con F-TRSEN-J001; ubicarlo es del qa-tester, y el integrador
suma la carpeta a `definitions` y corre `feature sync --feature FEAT-TRSEN`.

```architecture
features:
  - id: FEAT-TRSEN
    code: F-TRSEN
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 32 pruebas y ubicar el journey
Las 32 son nuevas y cubren las siete reglas (R001 5, R002 3, R003 6, R004 5, R005 10, R006 1, R007 2). **Ninguna
consulta un modelo pago**: los dos jueces son dobles que devuelven veredictos grabados, con la forma del artifact
`translationSenseVerdict`, y cuentan cuántas veces los llaman.

**De dónde sale cada prueba.**
- **Los casos del requirement**: las verduras (…1ecd), que los dos jueces reprueban y dan un hallazgo por condición; «It's
  the moment ___ dinner!» (…2d9e), que reprueba uno solo y da un único `judges-disagree` que sólo ordena, con puntaje 1: el
  caso que va a la bandeja (José, 30/9); Yakarta (…350b), la oración con la opción correcta; «a lot of» contra «mucho»,
  la evidencia.
- **El tope de a pares**, del parche: con tope 10 y veinte ejercicios nuevos quedan cinco con los dos veredictos, no diez
  con uno; y a un ejercicio que ya tiene el veredicto de un juez sólo se le consulta el otro.
- **El examen** (R006): los dos jueces juzgan cada ejercicio del archivo y ningún otro de la foto; la aprobación la
  calcula quien tiene el oro.
- **Dos pruebas van sobre `AgentJudge`**, de FEAT-JEXAM: la salida conservadora (`outputUnusable`) como salida
  inutilizable y el mapa de contenido tal cual como entradas. Son de todo juez nuevo, pero sólo se pueden trazar a uno;
  entran porque FEAT-JEXAM se aplica antes: si no, `AgentJudge` entraría como una implementación vacía.

**Dependencias.** El render (C-07, A-01) en el constructor del sujeto; la ranura del diagnóstico (C-05) y la cobertura
común (C-04); los hallazgos, la ficha y `unevaluatedBy` de 0.3; el runtime común de FEAT-JEXAM. Con C-01 el proveedor
ya implementa `AnalyzerProvider` y sus pruebas no cambian; sin `TRANSLATION_SENSE` en `DiagnosisKind` (C-21) la
ligadura se prueba igual, por su nombre.

**El journey** va a `audit-domain`, en `com.learney.contentaudit.auditdomain.translationsenseengine`, como los del juez
de consigna: ahí son visibles el analizador, el combinador y el constructor del sujeto, que no son públicos.
`generate` crearía `FTrsenJ001JourneyTest` con 4 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: translationsenseengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultTranslationSenseCombiner
            _change: add
            implements: [TranslationSenseCombiner]
            handwrittenTests:
              - name: "should give a single rank-only judges-disagree finding with both verdicts in its evidence, and score 1, when only one judge fails «It's the moment ___ dinner!»"
                traceability: { feature: FEAT-TRSEN, rule: F-TRSEN-R003 }
features:
  - id: FEAT-TRSEN
    code: F-TRSEN
    journeys:
      - { id: F-TRSEN-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.translationsenseengine }
```
