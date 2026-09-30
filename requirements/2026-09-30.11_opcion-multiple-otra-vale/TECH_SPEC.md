---
patch: FEAT-MCVALE
requirement: 2026-09-30.11_opcion-multiple-otra-vale
generated: 2026-09-30T21:30:00Z
---

# Tech Spec: Juez de opción múltiple, ¿otra opción también vale? (FEAT-MCVALE)

Paquete C, ronda 1. José decidió el 30/9 que este juez va dentro del analizador de Opción múltiple del paquete A: una
sola fila y una sola ficha. Por eso acá no hay analizador ni proveedor: hay un juez entero —huella, evaluador en el
libro, veredicto, clasificación, puntaje, reglas para la ficha, lector y examen— y una sola puerta por la que el
analizador de A lo usa. Viene de **0.3 (FEAT-HALL)**: `AnalyzerRuleCard`, `FindingDraft` y `FindingSeverity`. De
**FEAT-JEXAM**: el runtime común, la cobertura común y el examen. Del **parche de integración de la ronda 1
(FEAT-RONDA1)**: el render `QuizStudentViewRenderer` (gancho A-01) y el contrato del juez con el analizador de A
(paquete `choicealternative`: las dos interfaces, el juicio y el veredicto). Del paquete A: `CorrectorMatcher`
(FEAT-RESPC). De **0.2 (FEAT-OPMUL)**: `AuditableQuiz.multipleChoice` y `MultipleChoiceEntity`, de donde salen las
opciones en su orden y la correcta. Se aplica después de la integración y de FEAT-JEXAM: rebasado el 30/9 sobre el
`sentinel.yaml` real, con 0.2 y 0.3 aplicados; `sentinel-baseline.yaml` queda como foto del diseño.

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
igual a como la emite el juez. Al integrar quedó este contrato en dos tiempos, y no el `judge()` por ejercicio que
esperaba A (A-07): es el único que deja consultar a la vez (F-JEXAM-R006) y su juicio trae la gravedad de cada opción y la
clave invertida. Por ser la frontera entre dos paquetes, las interfaces y el juicio pasaron al parche de integración
(FEAT-RONDA1); acá quedan sus implementaciones.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
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
El veredicto (`ChoiceAlternativeVerdict`, con sus `ChoiceAlternativeOption`) viaja dentro del juicio que lee A, así que
vive con el contrato en el parche de integración; acá queda sólo su lector, que suma al paquete que crea la integración.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: choicealternative
        _change: modify
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
(F-JEXAM-R012). Un ejercicio sin veredicto queda `PENDING`, `FAILED` o `NOT_AVAILABLE`, sin puntaje ni hallazgo del juez,
y A lo declara evaluado a medias (`AuditNode.partiallyEvaluatedBy`, F-RONDA1-R006), para que «algún error» se lea como
piso (F-MCVALE-R005). El juicio y sus estados son parte del contrato de la integración.

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

Al aplicar, Sentinel copia de REQUIREMENT.md la compuerta con F-MCVALE-J001, ya ubicado por el qa-tester: no hace falta
sumar la carpeta a `definitions` ni correr `feature sync`, porque `patch apply` registra la feature y Sentinel lee cada
REQUIREMENT.md de `requirements/`. El journey corre `analyze` con el analizador de A, así que se ubica donde A ubica los
suyos.

```architecture
features:
  - id: FEAT-MCVALE
    code: F-MCVALE
modules:
  - name: audit-domain
    _change: modify
```

## Declarar las 24 pruebas y ubicar el journey
Las 24 son nuevas y cubren las siete reglas (R001 4, R002 3, R003 5, R004 3, R005 6, R006 1, R007 2). **Ninguna
consulta un modelo pago**: el juez es un doble que devuelve veredictos grabados, con la forma del artifact
`choiceAlternativeVerdict`, y cuenta cuántas veces lo llaman.

**De dónde sale cada prueba.**
- **Los casos del requirement**: «Are you thirsty? ___ , let's drink water soon.» (…47cc, la clave invertida del examen
  fijo), bloqueante con la marca «(correcta)»; «I don't enjoy extra food ___ .» (…2f1b), alta por «a lot»; «There's a cat
  ___ the window.» (…2cb8), media por «in»; «She wants ___ hot tea.» (…232e), donde «a» también vale.
- **La forma contraída y la larga**: una opción que difiere de la correcta sólo como «isn't» e «is not» no es otra opción
  que vale; la marca una regla del paquete A.
- **El examen** (R006): juzga sólo la opción múltiple del archivo, con …47cc, y no las verduras, que son CLOZE.

**Dependencias.** Las pruebas de `LedgerBackedChoiceAlternativeJudgeRun` y de la factory siguen la interfaz que ofrece C
(`request`, `resolveAll`, `judgmentOf`, `coverage`), todavía sin reconciliar con la que espera A (gancho C-09 contra A-07):
si el integrador conserva los nombres de A, cambian los métodos que llaman, no lo que fijan. El puntaje del ejercicio lo
decide A (C-10): estas pruebas fijan `ChoiceAlternativeJudgment.score` como dato. El clasificador usa `sameAnswer` de
`CorrectorMatcher` (C-08) a través de un doble. También dependen de C-11 (el ejercicio a medias), del render (C-07), de
`AuditableQuiz.multipleChoice` de 0.2 y de los hallazgos de 0.3.

**El journey** va a `audit-domain`, en `com.learney.contentaudit.auditdomain.multiplechoicevalidity`, donde el paquete A
ubica F-OMVAL-J001: corre `analyze` con el analizador de A, que aloja al juez. El stub se genera aunque falte el parche de
A, pero implementarlo necesita ese parche y C-09. `generate` crearía `FMcvaleJ001JourneyTest` con 4 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: choicealternativeengine
        _change: add
        visibility: public
        implementations:
          - name: DefaultChoiceAlternativeClassifier
            _change: add
            implements: [ChoiceAlternativeClassifier]
            handwrittenTests:
              - name: "should drop «is not» listed as also valid for a quiz whose correct option is «isn't», leaving it without finding of another-option-valid"
                traceability: { feature: FEAT-MCVALE, rule: F-MCVALE-R002 }
features:
  - id: FEAT-MCVALE
    code: F-MCVALE
    journeys:
      - { id: F-MCVALE-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.multiplechoicevalidity }
```
