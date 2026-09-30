---
patch: FEAT-MCVALE
requirement: 2026-09-30.11_opcion-multiple-otra-vale
generated: 2026-09-30T19:05:00Z
---

# Tech Spec: Juez de opción múltiple, ¿otra opción también vale? (FEAT-MCVALE)

Paquete C, ronda 1. José decidió el 30/9 que este juez va dentro del analizador de Opción múltiple del paquete A: una
sola fila y una sola ficha. Por eso acá no hay analizador ni proveedor: hay un juez entero —huella, evaluador en el
libro, veredicto, clasificación, puntaje, reglas para la ficha, lector y examen— y una sola puerta por la que el
analizador de A lo usa. Viene de **0.3 (FEAT-HALL)**: `AnalyzerRuleCard`, `FindingDraft` y `FindingSeverity`. De
**FEAT-JEXAM**: el runtime común, la cobertura común y el examen. Del paquete A y del integrador: el render
`QuizStudentViewRenderer` (gancho A-01) y `CorrectorMatcher` (FEAT-RESPC). De **0.2
(FEAT-OPMUL)**: `AuditableQuiz.multipleChoice` y `MultipleChoiceEntity`, de donde salen las opciones en su orden y la
correcta. Se aplica después de FEAT-JEXAM.

**Fuera del parche** (`ronda1/ganchos-C.md`): el analizador de A, que inyecta `ChoiceAlternativeJudgeRunFactory`, suma sus
reglas a su ficha, guarda el juicio en su diagnóstico y publica la cobertura del juez en la ranura común; y el cableado
en `Main`. **La definición de agente** que escribe quien construye: `.sentinel/agents/choice-alternative-judge/`, con las
cuatro entradas del mapa de contenido y un artifact `choiceAlternativeVerdict`. Su guía: probar cada opción en su
oración, decir qué incorrectas también valen y si la correcta vale, y dejar afuera lo que difiere sólo en la forma
—mayúscula, a o an, singular o plural, contraída o larga—, que son reglas de A (F-MCVALE-R002). Costo: examen fijo 13
consultas, práctica sobre `escribir/` 193, curso entero 2.810 (A1 710).

## Una sola puerta para el analizador de Opción múltiple

El analizador de A abre una corrida del juez con su política (`open`) —la factory ya sabe el nombre del analizador que
lo aloja—, le pide en `onQuiz` cada
ejercicio (`request`, que ignora lo que no es opción múltiple), lo resuelve todo junto en `onCourseComplete`
(`resolveAll`, con las consultas a la vez de la corrida) y lee el juicio de cada ejercicio (`judgmentOf`), listo para
sumar sus hallazgos y su puntaje a los de sus reglas instantáneas; `coverage` le da la cobertura del juez para el
informe. Como el nombre es el del analizador que lo aloja, el tope `--budget`, el freno del examen, el examen y la
exclusión son los de ese analizador (F-HALL-R005, F-MCVALE-R004), y A no necesita saber nada del libro ni del modelo.
`judgeRule` devuelve la regla del juez —`another-option-valid`, costo modelo pago— para que la ficha de A la declare
igual a como la emite el juez. Los nombres y los estados siguen lo que A espera (A-07) salvo en lo que haría imposible
consultar a la vez: A pide un veredicto por ejercicio dentro de `onQuiz`; acá se piden todos y se resuelven juntos.

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
      - name: choicealternativeengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultChoiceAlternativeJudgeRunFactory
            _change: add
            visibility: public
            implements: [ChoiceAlternativeJudgeRunFactory]
            requiresInject:
              - { name: hostAnalyzerName, type: String }
              - { name: runtime, type: JudgeRuntime }
              - { name: judge, type: Evaluator }
              - { name: verdictReader, type: ChoiceAlternativeVerdictReader }
              - { name: config, type: ChoiceAlternativeConfig }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: correctorMatcher, type: CorrectorMatcher }
          - name: LedgerBackedChoiceAlternativeJudgeRun
            _change: add
            implements: [ChoiceAlternativeJudgeRun]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: subjectBuilder, type: ChoiceAlternativeSubjectBuilder }
              - { name: classifier, type: ChoiceAlternativeClassifier }
              - { name: verdictReader, type: ChoiceAlternativeVerdictReader }
```

## Leer la opción múltiple como la ve el alumno

El juez lee lo que está en pantalla antes de contestar: `title`, `instructions` —la consigna del ejercicio—, `exercise`
—el enunciado con la pista— y `options`, todas en el orden en que se muestran, con la correcta marcada
(F-MCVALE-R001). No lee la traducción ni la miniteoría, que se ven al corregir. La huella es eso y nada más: dos
ejercicios que difieren sólo en cuál es la correcta tienen veredictos distintos, y cambiar la traducción no re-cobra
nada. El evaluador es `choice-alternative-judge`, distinto del de consigna aunque ese también vea las opciones: la
pregunta es otra.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: choicealternativeengine
        _change: add
        visibility: public
        interfaces:
          - name: ChoiceAlternativeSubjectBuilder
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "build(AuditNode quizNode): EvaluationSubject"
        implementations:
          - name: DefaultChoiceAlternativeSubjectBuilder
            _change: add
            implements: [ChoiceAlternativeSubjectBuilder]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## El veredicto: qué incorrectas también valen, y si la correcta vale

Son las dos cosas que decide el juez (F-MCVALE-R002): las incorrectas que también valen, cada una con su gravedad —alta
si vale igual, media si es defendible pero menos natural—, y si la correcta vale; la clave invertida, como el imposible
`…47cc` del examen fijo, es bloqueante porque el alumno no puede acertar. El lector vive junto al de los otros jueces.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: choicealternative
        _change: add
        visibility: public
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
        interfaces:
          - name: ChoiceAlternativeVerdictReader
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "read(String payload): ChoiceAlternativeVerdict"
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: choicealternativeverdict
        _change: add
        visibility: public
        implementations:
          - name: JacksonChoiceAlternativeVerdictReader
            _change: add
            visibility: public
            implements: [ChoiceAlternativeVerdictReader]
```

## Un juicio listo para sumar a las reglas de A

El clasificador convierte el veredicto en lo que el analizador de A necesita y nada más: hallazgos en borrador de la regla
`another-option-valid` —uno por opción que vale, con la opción como marca, y uno con la marca «(correcta)» si la correcta
no vale—, un puntaje 1 / 0,6 / 0,3 / 0 según el más
grave (F-MCVALE-R003) y la procedencia. Antes descarta con `CorrectorMatcher.sameAnswer` la opción que es la misma
respuesta que la correcta en otra forma, como «isn't» e «is not»: esa la marca una regla de A y no se cuenta dos veces
(F-JEXAM-R012). Un ejercicio sin veredicto queda `PENDING`, `FAILED` o `NOT_AVAILABLE`, los estados que espera A, sin puntaje ni
hallazgo del juez; se recomienda que A lo
declare como no evaluado del todo (`unevaluatedBy`), para que «algún error» se lea como piso (F-MCVALE-R005).

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: ChoiceAlternativeConfig
        _change: add
        stereotype: port
        sealed: false
        extends: [SelfDescribingConfig]
        exposes:
          - signature: "getScoreFor(FindingSeverity severity): double"
    packages:
      - name: choicealternative
        _change: add
        visibility: public
        models:
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
      - name: choicealternativeengine
        _change: add
        visibility: public
        interfaces:
          - name: ChoiceAlternativeClassifier
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "classify(ChoiceAlternativeVerdict verdict,AuditableQuiz quiz,EvaluationResolution resolution): ChoiceAlternativeJudgment"
        implementations:
          - name: DefaultChoiceAlternativeClassifier
            _change: add
            implements: [ChoiceAlternativeClassifier]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: config, type: ChoiceAlternativeConfig }
  - name: audit-application
    _change: modify
    implementations:
      - name: DefaultChoiceAlternativeConfig
        _change: add
        visibility: public
        implements: [ChoiceAlternativeConfig]
        types: [Component]
```

## Registrar FEAT-MCVALE

Al aplicar, Sentinel copia de REQUIREMENT.md la compuerta con F-MCVALE-J001; ubicarlo es del qa-tester, y el integrador
suma la carpeta a `definitions` y corre `feature sync --feature FEAT-MCVALE`. El journey corre `analyze` con el
analizador de A, así que se ubica donde A ubique los suyos.

```architecture
features:
  - id: FEAT-MCVALE
    code: F-MCVALE
modules:
  - name: audit-domain
    _change: modify
```
