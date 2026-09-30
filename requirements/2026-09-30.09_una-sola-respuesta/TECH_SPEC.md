---
patch: FEAT-UNICA
requirement: 2026-09-30.09_una-sola-respuesta
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: Juez de una sola respuesta (FEAT-UNICA)

Paquete C, ronda 1. El analizador `single-answer` lista, con lo que el alumno ve antes de contestar, las respuestas que
también valdrían y el corrector no acepta. Viene de **0.3 (FEAT-HALL)**: `AnalyzerProvider`, la ficha, `FindingDraft` y
`FindingSeverity`. De **FEAT-JEXAM**: el runtime común, el evaluador genérico por agente y la cobertura común. Del paquete A y
del integrador: el render `QuizStudentViewRenderer` (gancho A-01) y la comparación del corrector `CorrectorMatcher`
(FEAT-RESPC). De **0.2 (FEAT-OPMUL)**: saber qué ejercicio es de opción múltiple para dejarlo afuera. Se
aplica después de FEAT-JEXAM. Rebasado el 30/9 sobre el `sentinel.yaml` real, con 0.2 y 0.3 aplicados, y sobre el parche de integración de la ronda 1 (FEAT-RONDA1), que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño.

**Fuera del parche** (`ronda1/ganchos-C.md`): la ranura del diagnóstico en `QuizDiagnoses`, la constante
`SINGLE_ANSWER` de `DiagnosisKind` si E decide que produce tareas, el cableado en `Main`. **La definición de agente** que escribe quien
construye: `.sentinel/agents/single-answer-judge/`, con las cuatro entradas del mapa de contenido y un artifact
`singleAnswerVerdict`. Su guía: listar sólo lo que un alumno podría escribir con lo que ve; no listar nunca la forma
contraída o larga, las grafías de la lista cerrada ni until o till, que el corrector acepta (F-JEXAM-R012); separar la
opción que ofrece la consigna de la respuesta distinta; y dar la gravedad de F-UNICA-R004. Costo: examen fijo 39
consultas, práctica sobre `escribir/` 386, curso entero 8.477 (A1 1.692).

## Registrar el analizador con un proveedor y su ficha

La ficha declara familia errores, costo modelo pago, niveles evaluados `[QUIZ]` y dos reglas: `different-valid-answer`,
que va a la mesa, y `offered-alternative`, que sólo ordena y nombra a respuestas completas (paquete A) como quien la
arregla con una regla. La ligadura al plan nombra `SINGLE_ANSWER`; en esta ronda no hay constante y el plan lo declara
(F-RONDA1-R008). El proveedor implementa `AnalyzerProvider`: se había declarado como clase con sus cinco firmas porque el
puerto todavía no estaba en `sentinel.yaml`, y al rebasar sobre 0.3 pasó a `implements` (gancho C-02).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswerengine
        _change: add
        visibility: public
        implementations:
          - name: SingleAnswerAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: runtime, type: JudgeRuntime }
              - { name: judge, type: Evaluator }
              - { name: verdictReader, type: SingleAnswerVerdictReader }
              - { name: config, type: SingleAnswerConfig }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: correctorMatcher, type: CorrectorMatcher }
```

## Leer sólo lo que el alumno ve antes de contestar

La otra respuesta es un problema sólo si el alumno no tenía con qué saber cuál acepta el corrector, así que el juez lee
exactamente lo que está en pantalla antes de contestar: `title` —el nombre que la app muestra arriba—, `instructions`
—la consigna del ejercicio—, `exercise` —el enunciado con la pista— y `acceptedAnswers`, por hueco. La traducción y la
miniteoría se ven al corregir y no entran, así que cambiarlas no re-cobra nada (F-UNICA-R001). Los ejercicios de opción
múltiple no se piden: los juzga FEAT-MCVALE, que es la misma pregunta hecha sobre las opciones (José, 30/9).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswerengine
        _change: add
        visibility: public
        interfaces:
          - name: SingleAnswerSubjectBuilder
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "build(AuditNode quizNode): EvaluationSubject"
        implementations:
          - name: DefaultSingleAnswerSubjectBuilder
            _change: add
            implements: [SingleAnswerSubjectBuilder]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## El veredicto: cada respuesta que también valdría, con su clase y su gravedad

Se tipa porque de la clase depende si cuenta como error y de la gravedad, el puntaje: bloqueante si con lo que ve el
alumno no se puede saber cuál acepta, alta si rechaza otra igual de buena, media si rechaza otra correcta menos
esperable (F-UNICA-R004). El hueco va con cada respuesta porque un ejercicio puede tener varios. El lector vive junto al
de los otros jueces.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswer
        _change: add
        visibility: public
        models:
          - name: AlternativeAnswerKind
            _change: add
            type: enum
            fields:
              - name: OFFERED_BY_INSTRUCTION
              - name: DIFFERENT_VALID
          - name: AlternativeAnswer
            _change: add
            type: record
            fields:
              - { name: answer, type: String }
              - { name: gap, type: int }
              - { name: kind, type: AlternativeAnswerKind }
              - { name: severity, type: FindingSeverity }
              - { name: reason, type: String }
          - name: SingleAnswerVerdict
            _change: add
            type: record
            fields:
              - { name: alternatives, type: "List<AlternativeAnswer>" }
              - { name: rationale, type: String }
        interfaces:
          - name: SingleAnswerVerdictReader
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "read(String payload): SingleAnswerVerdict"
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: singleanswerverdict
        _change: add
        visibility: public
        implementations:
          - name: JacksonSingleAnswerVerdictReader
            _change: add
            visibility: public
            implements: [SingleAnswerVerdictReader]
```

## Descartar lo que el corrector acepta antes de contar

José decidió el 30/9 que la forma contraída o larga nunca es «otra respuesta que falta», porque el corrector la va a
aceptar, y lo mismo las grafías de la lista cerrada y until o till (F-UNICA-R002, F-JEXAM-R012). La guía se lo pide al
juez, pero la regla no puede depender de que el modelo la cumpla: el clasificador le pregunta a `CorrectorMatcher`, la comparación
del corrector del paquete A con los casos de oro del corrector de Go, si ya acepta cada respuesta, y las que sí quedan a la vista en el
diagnóstico sin dar hallazgo. Lo que queda da un hallazgo por respuesta, con la respuesta como marca: la opción que
ofrece la consigna sólo ordena y va a respuestas completas; la distinta es error y va a la mesa. La escala 1 / 0,6 /
0,3 / 0 vive en la configuración.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: SingleAnswerConfig
        _change: add
        stereotype: port
        sealed: false
        extends: [SelfDescribingConfig]
        exposes:
          - signature: "getScoreFor(FindingSeverity severity): double"
    packages:
      - name: singleanswerengine
        _change: add
        visibility: public
        models:
          - name: SingleAnswerOutcome
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: diagnosis, type: SingleAnswerDiagnosis }
              - { name: findings, type: "List<FindingDraft>" }
              - { name: score, type: double }
        interfaces:
          - name: SingleAnswerClassifier
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "classify(SingleAnswerVerdict verdict,AuditableQuiz quiz,boolean reused): SingleAnswerOutcome"
        implementations:
          - name: DefaultSingleAnswerClassifier
            _change: add
            implements: [SingleAnswerClassifier]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: config, type: SingleAnswerConfig }
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultSingleAnswerConfig
        _change: add
        visibility: public
        implements: [SingleAnswerConfig]
        types: [Component]
```

## Juntar, resolver a la vez y dejar lo sin evaluar declarado

El analizador junta en `onQuiz` los ejercicios para escribir, pide al runtime el plan (conjunto declarado, re-evaluación
o examen) y los resuelve en `onCourseComplete` con `resolveAll`, dentro del tope y con las consultas a la vez de la
corrida. Cada ejercicio con veredicto recibe su puntaje, su diagnóstico y sus hallazgos; el que quedó sin veredicto,
`unevaluatedBy`, sin puntaje ni hallazgo (F-UNICA-R005). La cobertura común declara el examen de la versión, el costo de
lo pendiente y, si pasó, por qué dejó de consultar.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswerengine
        _change: add
        visibility: public
        implementations:
          - name: SingleAnswerAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: subjectBuilder, type: SingleAnswerSubjectBuilder }
              - { name: classifier, type: SingleAnswerClassifier }
              - { name: verdictReader, type: SingleAnswerVerdictReader }
```

## Mostrar lo que contó y lo que se descartó

El diagnóstico guarda el veredicto tal como lo emitió el juez, las respuestas que cuentan y las que se descartaron porque
el corrector ya las acepta. Así se ve cuándo la guía no cumple la regla de las formas y el examen lo puede medir. Su
lugar en `QuizDiagnoses` es un gancho del integrador.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswer
        _change: add
        visibility: public
        models:
          - name: SingleAnswerDiagnosis
            _change: add
            type: record
            fields:
              - { name: verdict, type: SingleAnswerVerdict }
              - { name: counted, type: "List<AlternativeAnswer>" }
              - { name: droppedAsAccepted, type: "List<String>" }
              - { name: score, type: double }
              - { name: reusedFromPreviousRun, type: boolean }
```

## Registrar FEAT-UNICA

Al aplicar, Sentinel copia de REQUIREMENT.md la compuerta con F-UNICA-J001; ubicarlo es del qa-tester, y el integrador
suma la carpeta a `definitions` y corre `feature sync --feature FEAT-UNICA`.

```architecture
features:
  - id: FEAT-UNICA
    code: F-UNICA
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 28 pruebas y ubicar el journey
Las 28 son nuevas y cubren las siete reglas (R001 4, R002 6, R003 2, R004 6, R005 6, R006 1, R007 3). **Ninguna
consulta un modelo pago**: el juez es un doble que devuelve veredictos grabados, con la forma del artifact
`singleAnswerVerdict`, y cuenta cuántas veces lo llaman.

**De dónde sale cada prueba.**
- **Los casos del requirement**: «He was in Italy ___ .» (…2464), donde «from Friday until Sunday» y «from Friday till
  Sunday» son opciones que ofrece la consigna y dan un hallazgo que sólo ordena; las verduras (…1ecd), bloqueante;
  «Does he need water? He ___ .» (…1f57), alta por «doesn't»; «She ___ (not / come).» (…2189), media por «won't come».
- **La forma contraída y la larga**: un ejercicio que acepta «isn't» y cuyo único reparo sería «is not» no tiene hallazgo.
  Es el criterio de F-JEXAM-R012 y se prueba acá, bajo F-UNICA-R002, porque `patch propose` no acepta trazar a otra
  feature. Lo mismo las grafías de la lista cerrada y until o till.
- **El examen** (R006): juzga los CLOZE del archivo; uno de opción múltiple que venga en el archivo queda sin veredicto.

**Dependencias.** El clasificador usa `CorrectorMatcher` (C-08, del paquete A): las pruebas lo reemplazan por un doble,
porque con la tabla vacía de hoy «is not» no es «isn't»; con la tabla de verdad dependen de A-15 y A-16. El render (C-07),
la ranura del diagnóstico (C-05), la cobertura común (C-04), `AuditableQuiz.multipleChoice` de 0.2 para dejar afuera la
opción múltiple, y los hallazgos y la ficha de 0.3. Con C-02 el proveedor ya implementa `AnalyzerProvider` y sus
pruebas no cambian. El clasificador llama a `accepts` con el contexto del hueco (`GapContext`, del rebase de
FEAT-RESPC), que arma con el enunciado del ejercicio.

**El journey** va a `audit-domain`, en `com.learney.contentaudit.auditdomain.singleanswerengine`, como los del juez de
consigna. `generate` crearía `FUnicaJ001JourneyTest` con 4 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: singleanswerengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultSingleAnswerClassifier
            _change: add
            implements: [SingleAnswerClassifier]
            handwrittenTests:
              - name: "should drop «is not» listed by the judge for a quiz that accepts «isn't», leaving it without single-answer finding"
                traceability: { feature: FEAT-UNICA, rule: F-UNICA-R002 }
features:
  - id: FEAT-UNICA
    code: F-UNICA
    journeys:
      - { id: F-UNICA-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.singleanswerengine }
```
