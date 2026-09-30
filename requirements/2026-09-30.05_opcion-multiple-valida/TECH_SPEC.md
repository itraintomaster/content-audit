---
patch: FEAT-OMVAL
requirement: 2026-09-30.05_opcion-multiple-valida
generated: 2026-09-30T21:40:00Z
---

# Tech Spec: Opción múltiple válida (FEAT-OMVAL), paquete A de la ronda 1

Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL); `sentinel-baseline.yaml` es ese
modelo, armado en memoria, y queda como foto del diseño. Rebasado el 30/9 sobre el `sentinel.yaml` real, con 0.2 y 0.3
aplicados, y sobre el parche de integración de la ronda 1 (FEAT-RONDA1), que se aplica antes. De dónde sale lo que el
parche usa y no declara:

- **De 0.2**: `AuditableQuiz.multipleChoice`, `MultipleChoiceEntity` y `MultipleChoiceItemEntity` (texto e
  incidencia de cada opción, en el orden del alumno): son las opciones que revisan las reglas.
- **De 0.3**: `AnalyzerProvider`, `ContentAnalyzer.findingsAt`, `FindingDraft`, `FindingSeverity`,
  `FindingResolution`, `AnalysisCost`, `AnalyzerRuleCard`, la ficha y el tope por nombre (`--budget`).
- **Ya existe**: `EvaluationRunPolicy` y `EvaluationCoverage`, los mismos que usa el juez de consigna.
- **Del parche de FEAT-RESPC**: `CorrectorMatcher`, la comparación del corrector con su tabla.
- **Del parche de integración (FEAT-RONDA1)**: `QuizStudentViewRenderer`; el contrato del juez de C
  (`ChoiceAlternativeJudgeRunFactory`, `ChoiceAlternativeJudgeRun` y su juicio, `ChoiceAlternativeJudgment`); el lugar
  tipado del diagnóstico en `QuizDiagnoses`; la cobertura común de los jueces en `CourseDiagnoses`; y el ejercicio a
  medias (`AuditNode.partiallyEvaluatedBy`).

## Revisar las opciones con cuatro reglas instantáneas
Las reglas leen las opciones como las ve el alumno, cuál vale y dónde está el hueco, así que viven en un solo
colaborador (`OptionRuleCheck`) que recibe el ejercicio. R002 compara con `CorrectorMatcher.sameAnswer` en el contexto
del hueco (`GapContext`, de FEAT-RESPC): así «isn't» e «is not», o dos grafías de la lista, son la misma respuesta, como
decidió José. Medido sobre el 29/9: 324 por la
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

## Dejar entrar al juez de C por el contrato de la integración
José decidió que el juez de «otra opción también vale» sea parte de este analizador: una fila y una ficha. El paquete C
diseña el juez —evaluador, libro de evaluaciones, huella, prompt y examen— y el contrato entre los dos vive en el parche
de integración de la ronda 1 (`choicealternative`, FEAT-RONDA1), en dos tiempos: el analizador pide cada ejercicio en
`onQuiz` (`request`), los resuelve todos juntos en `onCourseComplete` (`resolveAll`, con las consultas a la vez de la
corrida) y lee el juicio de cada uno (`judgmentOf`); recién ahí escribe su puntaje, sus hallazgos y su diagnóstico.
Reemplaza al `judge()` por ejercicio que este parche pedía (gancho A-07): de a uno no hay consultas a la vez
(F-JEXAM-R006), y el juicio de C trae lo que `AlsoValidOption` no traía, la gravedad de cada opción y la clave invertida
(F-MCVALE-R002). El proveedor abre la corrida con la política de este analizador (su tope por nombre; sin pedido
explícito no se paga nada) y la ficha declara la regla del juez tal como la da `judgeRule()`, con su id y costo *modelo
pago*, el mayor de sus reglas (F-HALL-R006).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        implementations:
          - name: MultipleChoiceValidityAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: optionRules, type: OptionRuleCheck }
              - { name: judgeRun, type: ChoiceAlternativeJudgeRun }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Funcionar sin juez, con la misma ficha
`UnavailableChoiceAlternativeJudgeRunFactory` implementa el contrato de la integración sin consultar a nadie: cada juicio
vuelve `NOT_AVAILABLE` y la ficha igual declara la regla del juez, porque la fila es una sola. Así el analizador se
construye, se prueba y se mide con sus cuatro reglas sin esperar al juez de C; en la ronda, `Main` conecta el de C
(`DefaultChoiceAlternativeJudgeRunFactory`, gancho C-15), y volver a esta fábrica es cambiar esa línea.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        implementations:
          - name: UnavailableChoiceAlternativeJudgeRunFactory
            _change: add
            visibility: public
            implements: [ChoiceAlternativeJudgeRunFactory]
```

## Contar como «a medias» el ejercicio que el juez no juzgó
El contrato de 0.3 dice que un nodo está evaluado o no, y que no hay hallazgos en uno sin evaluar. Con el juez adentro,
un ejercicio puede tener sus reglas corridas y el juez pendiente. José dejó la opción A de DOUBT-OMVAL-JUEZ-A-MEDIAS,
una de las once dudas chicas: el ejercicio cuenta como evaluado, sus hallazgos de regla valen y el nodo declara al
analizador en `AuditNode.partiallyEvaluatedBy`, el campo que suma la integración (F-RONDA1-R006), así «algún error» se
lee como piso. El diagnóstico guarda el juicio con su estado; la cobertura del juez en la corrida no tiene un diagnóstico
propio: el analizador publica en el curso la que da `ChoiceAlternativeJudgeRun.coverage()`, en la ranura común de los
jueces (`CourseDiagnoses.getJudgeCoverages`, gancho C-04, que reemplazó a A-05).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        visibility: public
        models:
          - name: MultipleChoiceValidityDiagnosis
            _change: add
            type: record
            fields:
              - { name: issues, type: "List<OptionIssue>" }
              - { name: judgment, type: ChoiceAlternativeJudgment }
              - { name: score, type: double }
```

## Registrarse en el catálogo con su ficha
Ficha: pregunta «¿Hay una sola opción correcta, y ninguna se delata por la forma?», lee las opciones, cuál vale y el
enunciado, reglas R001 a R004 instantáneas más la del juez de C, familia errores, evalúa QUIZ de opción múltiple,
resoluciones regla y mesa, costo *modelo pago* por el juez. Como tiene reglas instantáneas, la corrida base (la de la
vista consolidada y la vista previa) lo corre igual, con tope 0 para el juez: es la opción A de DOUBT-OMVAL-CORRIDA-BASE,
que José dejó con su recomendación, y la implementa la integración (F-RONDA1-R007); así esas vistas no pierden sus 354
errores de regla. Sin `planBinding` ni `config`: los arreglos esperan a que José decida si se corrige la opción múltiple
(DOUBT-CORREGIR-MC), y en esta ronda el plan lo declara como sin convertir (F-RONDA1-R008).

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
              - { name: judgeRunFactory, type: ChoiceAlternativeJudgeRunFactory }
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

## Declarar las 24 pruebas y ubicar el journey
Las 24 son nuevas: no hay tests previos de estas reglas que portar. Con el criterio del qa-tester de la opción
múltiple, sólo se declara lo que verifica una regla, y cada nombre lleva un caso real. Las cinco reglas quedan
cubiertas (R001 3, R002 4, R003 7, R004 3, R005 7). De dónde sale cada una:

- **Los casos del REQUIREMENT**: «___ a child? (He)», «___ golf is difficult to learn.», «___ excited man», «A: Can you
  call Miss Green? B: No, ___ .», «He's an ___ amazing man.» y «It was an ___ wonderful experience.».
- **El curso del 29/9** (últimos 4 del id): las dos incorrectas que son la misma respuesta, «'re going to» y «are going
  to» en «Are you busy? If so, I ___ do it for you.» 26ef (Elegir la forma natural) y «'ve got to» y «have got to» en
  «David ___ call his mother now. (must)» 2e0c (Obligación); el hueco después de «B:» en «A: What did you find? B: ___ ,
  it was empty.» 2c5e; el artículo que no delata en «My brother is a ___ (good / well) driver.» 256a; y el ejercicio
  limpio, «The performance was ___ awful.» 48f1, al que las pruebas de R001 le cambian el puntaje de una opción.
- **Lo que depende de otros**: las gemelas por contracción usan la tabla del corrector nuevo, porque con la copia de
  hoy sólo cuentan la mayúscula y los signos (A-15); dos pruebas usan el juez de C por el contrato de la integración
  (A-07, reconciliado con C-09: cambian los métodos que llaman, no lo que fijan); la del ejercicio que el juez no vio, el
  campo «a medias» (A-08, F-RONDA1-R006); la evidencia, el renderizador (A-01).
- **No se declara**, porque ninguna regla lo pide, que la fábrica «sin juez» conteste NOT_AVAILABLE: su prueba es la de
  la ficha, que tiene que declarar la regla del juez aunque no haya juez conectado.
- **El journey** va a `audit-domain`, paquete `multiplechoicevalidity`. `generate` crearía `FOmvalJ001JourneyTest` con
  6 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: multiplechoicevalidity
        _change: add
        implementations:
          - name: DefaultOptionRuleCheck
            _change: add
            handwrittenTests:
              - name: "should report two incorrect options that are the same answer, as «'re going to» and «are going to» in «Are you busy? If so, I ___ do it for you.» of Elegir la forma natural"
                traceability: { feature: FEAT-OMVAL, rule: F-OMVAL-R002 }
features:
  - id: FEAT-OMVAL
    code: F-OMVAL
    journeys:
      - { id: F-OMVAL-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.multiplechoicevalidity }
```
