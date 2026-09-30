---
patch: FEAT-OMVAL
requirement: 2026-09-30.05_opcion-multiple-valida
generated: 2026-09-30T18:30:00Z
---

# Tech Spec: Opción múltiple válida (FEAT-OMVAL), paquete A de la ronda 1

Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL); `sentinel-baseline.yaml` es ese
modelo, armado en memoria. De dónde sale lo que el parche usa y no declara:

- **De 0.2**: `AuditableQuiz.multipleChoice`, `MultipleChoiceEntity` y `MultipleChoiceItemEntity` (texto e
  incidencia de cada opción, en el orden del alumno): son las opciones que revisan las reglas.
- **De 0.3**: `AnalyzerProvider`, `ContentAnalyzer.findingsAt`, `FindingDraft`, `FindingSeverity`,
  `FindingResolution`, `AnalysisCost`, `AnalyzerRuleCard`, la ficha y el tope por nombre (`--budget`).
- **Ya existe**: `EvaluationRunPolicy` y `EvaluationCoverage`, los mismos que usa el juez de consigna.
- **Del parche de FEAT-RESPC**: `CorrectorMatcher`, la comparación del corrector con su tabla.
- **Del integrador de la ronda 1**: `QuizStudentViewRenderer`, el slot de `CourseDiagnoses` para la cobertura del
  juez, y el ejercicio «a medias» del contrato (ver la sección del juez pendiente).

## Revisar las opciones con cuatro reglas instantáneas
Las reglas leen las opciones como las ve el alumno, cuál vale y dónde está el hueco, así que viven en un solo
colaborador (`OptionRuleCheck`) que recibe el ejercicio. R002 compara con `CorrectorMatcher.sameAnswer`: así «isn't» e
«is not», o dos grafías de la lista, son la misma respuesta, como decidió José. Medido sobre el 29/9: 324 por la
mayúscula al abrir la oración, 2 por el artículo, 28 con dos opciones incorrectas que son la misma (baja, sobra una) y
ninguno con dos correctas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        models:
          - name: OptionIssue
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: kind, type: OptionIssueKind }
              - { name: option, type: String }
              - { name: relatedOption, type: String }
              - { name: involvesCorrect, type: boolean }
          - name: OptionIssueKind
            _change: add
            type: enum
            fields:
              - { name: NO_VALID_OPTION }
              - { name: SEVERAL_VALID_OPTIONS }
              - { name: IDENTICAL_OPTIONS }
              - { name: SAME_ANSWER_OTHER_FORM }
              - { name: LOWERCASE_SENTENCE_START }
              - { name: LOWERCASE_I }
              - { name: DASH_AT_SENTENCE_START }
              - { name: ARTICLE_GIVES_AWAY }
        interfaces:
          - name: OptionRuleCheck
            _change: add
            visibility: internal
            exposes:
              - signature: "check(AuditableQuiz quiz): List<OptionIssue>"
        implementations:
          - name: DefaultOptionRuleCheck
            _change: add
            implements: [OptionRuleCheck]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
```

## Dejar entrar al juez de C por una sesión de la corrida
José decidió que el juez de «otra opción también vale» sea parte de este analizador: una fila y una ficha. El
paquete C diseña el juez —evaluador, libro de evaluaciones, huella, prompt y examen—; este parche declara sólo por
dónde entran sus veredictos. El proveedor abre una sesión por corrida con la política de este analizador (el tope
que se le fija por nombre, y sin pedido explícito no se paga nada), y el analizador le pide a la sesión el veredicto
de cada ejercicio con su nodo, para que C arme el contenido juzgado como quiera. La fábrica también le da a la ficha
la regla del juez con su id y su costo: el id lo pone C (FEAT-MCVALE), y así la ficha declara *modelo pago*, el mayor
de sus reglas (F-HALL-R006). Las interfaces van en la raíz del módulo porque Sentinel resuelve el `implements` desde
cualquier lugar contra la raíz, y C todavía no eligió dónde vive su implementación.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: MultipleChoiceJudgeFactory
        _change: add
        stereotype: factory
        exposes:
          - signature: "open(EvaluationRunPolicy policy): MultipleChoiceJudgeSession"
          - signature: "judgeRule(): AnalyzerRuleCard"
      - name: MultipleChoiceJudgeSession
        _change: add
        exposes:
          - signature: "judge(AuditNode quizNode): MultipleChoiceJudgement"
          - signature: "coverage(): EvaluationCoverage"
          - signature: "consultedJudgeVersion(): Optional<String>"
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        models:
          - name: MultipleChoiceJudgement
            _change: add
            type: record
            fields:
              - { name: status, type: MultipleChoiceJudgementStatus }
              - { name: alsoValidOptions, type: "List<AlsoValidOption>" }
              - { name: judgeVersion, type: String }
              - { name: reused, type: boolean }
          - name: MultipleChoiceJudgementStatus
            _change: add
            type: enum
            fields:
              - { name: JUDGED }
              - { name: PENDING }
              - { name: FAILED }
              - { name: NOT_AVAILABLE }
          - name: AlsoValidOption
            _change: add
            type: record
            fields:
              - { name: option, type: String }
              - { name: explanation, type: String }
```

## Funcionar sin juez hasta que C lo conecte
Mientras C no conecte su juez, la raíz de composición usa `UnavailableMultipleChoiceJudgeFactory`: cada veredicto
vuelve `NOT_AVAILABLE` y la ficha igual declara la regla del juez, porque la fila es una sola. Así el analizador se
construye, se prueba y se mide con sus cuatro reglas desde el primer día, y conectar el juez es cambiar una línea de
`Main`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        implementations:
          - name: UnavailableMultipleChoiceJudgeFactory
            _change: add
            visibility: public
            implements: [MultipleChoiceJudgeFactory]
```

## Contar como «a medias» el ejercicio que el juez no juzgó
El contrato de 0.3 dice que un nodo está evaluado o no, y que no hay hallazgos en uno sin evaluar. Con el juez adentro,
un ejercicio puede tener sus reglas corridas y el juez pendiente: si se lo marcara sin evaluar, se perderían sus
hallazgos de regla; si se lo diera por evaluado, «algún error» dejaría de leerse como piso (F-HALL-R008 inv. 3). El
diseño lo marca a medias: cuenta como evaluado, sus hallazgos de regla valen, y el nodo declara que el juez no lo vio.
Eso pide un campo nuevo en el contrato, que es un gancho del integrador y una duda para José
(DOUBT-OMVAL-JUEZ-A-MEDIAS). La cobertura del juez en la corrida va en el curso, como la del juez de consigna.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        models:
          - name: MultipleChoiceJudgeCoverageDiagnosis
            _change: add
            type: record
            fields:
              - { name: coverage, type: EvaluationCoverage }
              - { name: consultedJudgeVersion, type: String }
          - name: MultipleChoiceValidityDiagnosis
            _change: add
            type: record
            fields:
              - { name: issues, type: "List<OptionIssue>" }
              - { name: judgement, type: MultipleChoiceJudgement }
              - { name: score, type: double }
        implementations:
          - name: MultipleChoiceValidityAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: optionRules, type: OptionRuleCheck }
              - { name: judgeSession, type: MultipleChoiceJudgeSession }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Registrarse en el catálogo con su ficha
Ficha: pregunta «¿Hay una sola opción correcta, y ninguna se delata por la forma?», lee las opciones, cuál vale y el
enunciado, reglas R001 a R004 instantáneas más la del juez de C, familia errores, evalúa QUIZ de opción múltiple,
resoluciones regla y mesa, costo *modelo pago* por el juez. Consecuencia: la corrida base de 0.3 (la de la vista
consolidada y la vista previa) sólo corre analizadores instantáneos y lo dejaría afuera con sus 354 errores de regla;
el gancho propone que corra con tope 0, sin pagar (DOUBT-OMVAL-CORRIDA-BASE). Sin `planBinding` ni `config`: los
arreglos esperan a que José decida si se corrige la opción múltiple (DOUBT-CORREGIR-MC).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        implementations:
          - name: MultipleChoiceValidityAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: judgeFactory, type: MultipleChoiceJudgeFactory }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
features:
  - id: FEAT-OMVAL
    code: F-OMVAL
```

## Cómo se mide
Sobre una copia del curso en el scratchpad, `analyze --analyzers multiple-choice-validity` con tope 0 (sin pedido
explícito no se paga, así que el juez no corre); del resumen del informe sale `marcas.json` con `analizador: opcion`
por cada ejercicio con hallazgo, y `medir.py marcas.json --parte escribir --categoria opcion`. En `escribir/` hay 186
casos de forma (170 de mayúscula, 16 de guion o diálogo): el prototipo del analista los encuentra todos y marca 10 más
con la misma falta. Metas: 9 de cada 10 de los casos de forma de `examinar/`, que corre José, y 8 de cada 10 marcas
nuevas reales. Lo que diga el juez se mide con el examen de C, no con éste.

## Lo que no se mueve
multiple-choice-validity es de la familia de errores: el 73,9 % y los niveles (96,5 · 94,2 · 79,5 · 63,5) quedan
iguales. No corrige ninguna opción múltiple (F-OPMUL-R006): los hallazgos declaran su resolución y esperan.
