---
patch: FEAT-MINIT
requirement: 2026-09-30.10_miniteoria
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: Juez de miniteoría (FEAT-MINIT)

Paquete C, ronda 1. El analizador `mini-theory` juzga cada tema de una vez —sus miniteorías contra todos sus
ejercicios— y cuenta el error en los ejercicios que muestran la miniteoría con problema. Viene de **0.3 (FEAT-HALL)**:
`AnalyzerProvider`, la ficha, `FindingDraft` y `FindingSeverity`. De **FEAT-JEXAM**: el runtime común, el evaluador
genérico por agente y la cobertura común, que acá cuenta temas. Del integrador: el render `QuizStudentViewRenderer`
(gancho A-01) y la miniteoría de cada ejercicio (`AuditableQuiz.miniTheory`, A-02). Se aplica después de FEAT-JEXAM. Rebasado el 30/9 sobre el `sentinel.yaml` real, con 0.2 y 0.3 aplicados, y sobre el parche de integración de la ronda 1 (FEAT-RONDA1), que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño.

**Fuera del parche** (`ronda1/ganchos-C.md`): la miniteoría en `AuditableQuiz` —hoy el curso la tiene en cada ejercicio
(`QuizTemplateEntity.miniTheory`) pero no llega a la auditoría—, que el mapeador copia tal cual; la ranura del
diagnóstico en `QuizDiagnoses`; la constante `MINI_THEORY` de `DiagnosisKind` si E decide que produce tareas; y el
cableado en `Main`. **La definición de agente** que escribe quien construye: `.sentinel/agents/mini-theory-judge/`, con
las cinco entradas del mapa de contenido y un artifact `miniTheoryVerdict`. Su guía: por cada miniteoría, si afirma
algo falso o exagera, y qué ejercicios que la muestran la contradicen o quedan afuera, con un contraejemplo; que una
miniteoría pida una forma no es asunto de este juez, lo marca el paquete A (F-JEXAM-R012). Costo: práctica sobre
`escribir/` 60 consultas, curso entero 413 (A1 83); el examen fijo, un tema por cada tema distinto de sus ejercicios.

## Registrar el analizador con un proveedor y su ficha

La ficha declara familia errores, costo modelo pago y niveles evaluados `[QUIZ]`: se consulta por tema, pero el puntaje
y los hallazgos van en el ejercicio, que es donde cuenta el error. Reglas: `false-rule` y `contradicting-exercise`
(alta), `overstated-rule` (media) y `uncovered-exercise` (baja), todas a la mesa. La ligadura al plan nombra
`MINI_THEORY` sobre el ejercicio; que la mesa resuelva una vez por miniteoría sale de la marca del hallazgo. El
proveedor implementa `AnalyzerProvider`: se había declarado como clase con sus cinco firmas porque el puerto todavía no
estaba en `sentinel.yaml`, y al rebasar sobre 0.3 pasó a `implements` (gancho C-03). En esta ronda `MINI_THEORY` no es
constante de `DiagnosisKind` y el plan lo declara (F-RONDA1-R008).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheoryengine
        _change: add
        visibility: public
        implementations:
          - name: MiniTheoryAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: runtime, type: JudgeRuntime }
              - { name: judge, type: Evaluator }
              - { name: verdictReader, type: MiniTheoryVerdictReader }
              - { name: config, type: MiniTheoryConfig }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Una consulta por tema, con miniteorías y ejercicios numerados

Una regla se juzga contra el tema entero, no contra un ejercicio, y en 259 de los 413 temas conviven dos o más
miniteorías. El juez lee `cefrLevel`, `topic`, `instructions` —la consigna del tema—, `miniTheories` —cada miniteoría
distinta, M1, M2…, en el orden en que aparece por primera vez, con los ejercicios que la muestran— y `exercises` —E1,
E2…, en el orden del tema, cada uno con su enunciado y sus respuestas o sus opciones con la correcta marcada—. No lee
traducciones, audios ni la teoría larga. La huella es todo eso, así que cambiar una miniteoría o un ejercicio vuelve a
juzgar el tema entero: es la cascada buscada de F-MINIT-R001, barata porque el curso entero son 413 consultas. Numerar en
vez de usar identificadores deja la huella en lo que el juez lee, como en el juez de consigna.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheoryengine
        _change: add
        visibility: public
        interfaces:
          - name: MiniTheorySubjectBuilder
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "build(AuditNode knowledgeNode): EvaluationSubject"
        implementations:
          - name: DefaultMiniTheorySubjectBuilder
            _change: add
            implements: [MiniTheorySubjectBuilder]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## El veredicto del tema: una evaluación por miniteoría

El veredicto dice, por cada miniteoría, si es cierta, qué problema tiene, con qué gravedad, un contraejemplo y los
ejercicios involucrados (F-MINIT-R002). Se tipa porque el reparto a los ejercicios depende del problema: una regla falsa
alcanza a todos los que la muestran, un ejercicio que la contradice sólo a ése. Los números M y E son los del mapa de
contenido. El lector vive junto al de los otros jueces.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: add
        visibility: public
        models:
          - name: MiniTheoryProblem
            _change: add
            type: enum
            fields:
              - name: FALSE_RULE
              - name: OVERSTATED
              - name: CONTRADICTED_BY_EXERCISE
              - name: NOT_COVERING_EXERCISE
          - name: MiniTheoryAssessment
            _change: add
            type: record
            fields:
              - { name: miniTheory, type: int }
              - { name: holds, type: boolean }
              - { name: problem, type: MiniTheoryProblem }
              - { name: severity, type: FindingSeverity }
              - { name: counterexample, type: String }
              - { name: exercises, type: "List<Integer>" }
              - { name: explanation, type: String }
          - name: MiniTheoryVerdict
            _change: add
            type: record
            fields:
              - { name: assessments, type: "List<MiniTheoryAssessment>" }
              - { name: rationale, type: String }
        interfaces:
          - name: MiniTheoryVerdictReader
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "read(String payload): MiniTheoryVerdict"
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: minitheoryverdict
        _change: add
        visibility: public
        implementations:
          - name: JacksonMiniTheoryVerdictReader
            _change: add
            visibility: public
            implements: [MiniTheoryVerdictReader]
```

## Contar sólo donde se muestra, con «en todo el tema» como un valor de la configuración

José decidió el 30/9 que una miniteoría con problema cuenta como error sólo en los ejercicios que la muestran, y pidió
que contar en todo el tema sea un parámetro y no otro diseño (F-MINIT-R003, ⚠ a confirmar con él). Si el hallazgo
viviera en el tema, el contrato 0.3 lo contaría en todos sus ejercicios (F-HALL-R008, decisión de la mañana); por eso el
repartidor pone un hallazgo en cada ejercicio alcanzado y el contrato no cambia. `MiniTheoryErrorScope` decide qué
ejercicios alcanza una regla falsa o exagerada —los que la muestran o todos los del tema—; el ejercicio que la contradice
o queda afuera cuenta siempre sólo donde está. La marca del hallazgo es el texto de la miniteoría: los ejercicios de una
misma miniteoría comparten identidad salvo por el nodo, y así la mesa los resuelve con una sola reescritura (F-MINIT-R007).
El repartidor numera igual que el constructor del sujeto, a partir del mismo tema.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: MiniTheoryConfig
        _change: add
        stereotype: port
        sealed: false
        extends: [SelfDescribingConfig]
        exposes:
          - signature: "getErrorScope(): MiniTheoryErrorScope"
          - signature: "getScoreFor(FindingSeverity severity): double"
    packages:
      - name: minitheory
        _change: add
        visibility: public
        models:
          - name: MiniTheoryErrorScope
            _change: add
            type: enum
            fields:
              - name: SHOWN_ONLY
              - name: WHOLE_TOPIC
      - name: minitheoryengine
        _change: add
        visibility: public
        models:
          - name: MiniTheoryQuizOutcome
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: quizId, type: String }
              - { name: diagnosis, type: MiniTheoryDiagnosis }
              - { name: findings, type: "List<FindingDraft>" }
              - { name: score, type: double }
        interfaces:
          - name: MiniTheoryDistributor
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "distribute(MiniTheoryVerdict verdict,AuditNode knowledgeNode,boolean reused): List<MiniTheoryQuizOutcome>"
        implementations:
          - name: DefaultMiniTheoryDistributor
            _change: add
            implements: [MiniTheoryDistributor]
            requiresInject:
              - { name: config, type: MiniTheoryConfig }
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultMiniTheoryConfig
        _change: add
        visibility: public
        implements: [MiniTheoryConfig]
        types: [Component]
```

## Juzgar los temas, repartir a los ejercicios y contar temas en el tope

El recorrido llega al tema antes que a sus ejercicios, así que `onKnowledge` arma el sujeto con el tema entero; en
`onCourseComplete` el analizador pide al runtime el plan —un tema entra en un conjunto declarado si su id o el de alguno
de sus ejercicios está en él— y resuelve los temas con `resolveAll`: el tope cuenta temas (F-MINIT-R005). Después reparte
cada veredicto y escribe en cada ejercicio su puntaje, su diagnóstico y sus hallazgos. Los ejercicios de un tema sin
veredicto quedan con `unevaluatedBy`, sin puntaje ni hallazgo; el tema, el nivel y el curso suben por la agregación
genérica. La cobertura común cuenta temas y dice cuántos ejercicios contienen.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheoryengine
        _change: add
        visibility: public
        implementations:
          - name: MiniTheoryAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: subjectBuilder, type: MiniTheorySubjectBuilder }
              - { name: distributor, type: MiniTheoryDistributor }
              - { name: verdictReader, type: MiniTheoryVerdictReader }
```

## Mostrar en cada ejercicio qué miniteoría ve y qué se le reprocha

El diagnóstico del ejercicio dice de qué tema salió el veredicto, el texto de la miniteoría que muestra y lo que el
veredicto le reprocha. Es lo que permite ver, en un tema con 18 miniteorías, cuál es la falsa y dónde se ve. Su lugar en
`QuizDiagnoses` es un gancho del integrador.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheory
        _change: add
        visibility: public
        models:
          - name: MiniTheoryDiagnosis
            _change: add
            type: record
            fields:
              - { name: knowledgeId, type: String }
              - { name: miniTheory, type: String }
              - { name: assessments, type: "List<MiniTheoryAssessment>" }
              - { name: score, type: double }
              - { name: reusedFromPreviousRun, type: boolean }
```

## Registrar FEAT-MINIT

Al aplicar, Sentinel copia de REQUIREMENT.md la compuerta con F-MINIT-J001; ubicarlo es del qa-tester, y el integrador
suma la carpeta a `definitions` y corre `feature sync --feature FEAT-MINIT`.

```architecture
features:
  - id: FEAT-MINIT
    code: F-MINIT
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 28 pruebas y ubicar el journey
Las 28 son nuevas y cubren las siete reglas (R001 5, R002 3, R003 4, R004 4, R005 7, R006 2, R007 3). **Ninguna
consulta un modelo pago**: el juez es un doble que devuelve veredictos grabados, con la forma del artifact
`miniTheoryVerdict`, y cuenta cuántas veces lo llaman.

**De dónde sale cada prueba.**
- **«Sólo donde se muestra» y «en todo el tema»**, los dos valores del alcance: en «Contraste inicial» (A1, 80
  ejercicios) la miniteoría falsa de «enjoy» da hallazgo en los 9 que la muestran y en ninguno de los otros 71; con «en
  todo el tema», en los 80. El ejercicio que la contradice cuenta siempre sólo donde está.
- **Los demás casos del requirement**: «Sujeto u objeto» (28 ejercicios, 16 con la de they/them y 12 con la de she/her)
  para numerar cada miniteoría una sola vez; el contraejemplo «Are you enjoying the party?» en la evidencia; las verduras,
  cuya miniteoría es cierta y no da hallazgo (R006).
- **El tope cuenta temas**: con tope 1 y dos temas nuevos se juzga uno y todos los ejercicios del otro quedan sin evaluar.

**Dependencias.** La miniteoría de cada ejercicio en `AuditableQuiz` (C-06, A-02 y A-03): sin ella no hay qué leer. El
render (C-07), la ranura del diagnóstico (C-05), la cobertura común (C-04) y los hallazgos y la ficha de 0.3. Con C-03 el
proveedor ya implementa `AnalyzerProvider` y sus pruebas no cambian. DOUBT-MINIT-VARIAS se confirma con José: si
vuelve a «en todo el tema», cambia el valor por omisión y la prueba de la configuración, no las dos del alcance.

**El journey** va a `audit-domain`, en `com.learney.contentaudit.auditdomain.minitheoryengine`, como los del juez de
consigna. `generate` crearía `FMinitJ001JourneyTest` con 3 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: minitheoryengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultMiniTheoryDistributor
            _change: add
            implements: [MiniTheoryDistributor]
            handwrittenTests:
              - name: "should put a finding of the false «enjoy» mini-theory in the 9 exercises of «Contraste inicial» that show it and in none of its other 71"
                traceability: { feature: FEAT-MINIT, rule: F-MINIT-R003 }
              - name: "should put that finding in the 80 exercises of «Contraste inicial» when the error scope is the whole topic"
                traceability: { feature: FEAT-MINIT, rule: F-MINIT-R003 }
features:
  - id: FEAT-MINIT
    code: F-MINIT
    journeys:
      - { id: F-MINIT-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.minitheoryengine }
```
