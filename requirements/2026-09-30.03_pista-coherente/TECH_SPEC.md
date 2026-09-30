---
patch: FEAT-PISTA
requirement: 2026-09-30.03_pista-coherente
generated: 2026-09-30T19:05:00Z
---

# Tech Spec: Pista coherente (FEAT-PISTA), paquete A de la ronda 1

Diseñado contra el modelo que queda después de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL); `sentinel-baseline.yaml` es ese
modelo, armado en memoria. De dónde sale lo que el parche usa y no declara:

- **De 0.3**: `AnalyzerProvider`, `ContentAnalyzer.findingsAt`, `FindingDraft`, `FindingEvidence`, `FindingSeverity`,
  `FindingResolution`, `AnalysisCost`, `AnalyzerRuleCard`, `AnalyzerFamily` y la ficha (`AnalyzerDescriptor`).
- **De 0.2**: `AuditableQuiz.multipleChoice`, para dejar afuera la opción múltiple (R001).
- **Ya existe**: `AuditableQuiz.tokens` (los `NlpToken` de la oración guardada, con su lema), `sentenceParts`,
  `AuditableKnowledge.sentenceMode` y `quizzes`. No hay una segunda lectura del curso ni otra tokenización.
- **Del integrador de la ronda 1**: `QuizStudentViewRenderer`, para la evidencia como la ve el alumno.

## Comparar la pista con los tokens y lemas que el curso ya trae
La regla necesita saber si cada palabra de la pista está en la oración terminada, igual, flexionada o por su lema. Los
lemas ya están: los calcula el tokenizador compartido sobre la oración guardada y viajan en `AuditableQuiz.tokens`.
El analizador no llama al tokenizador ni lematiza nada nuevo: lee la pista de las partes (`HintReader`), la compara
con esos tokens (`HintAnswerCheck`) y en `onKnowledge`, que el motor llama antes que a los ejercicios del tema, cuenta
cuántos del mismo tipo llevan pista para el umbral de tres cuartos que eligió José.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: add
        visibility: public
        models:
          - name: ExerciseHint
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: text, type: String }
              - { name: words, type: "List<String>" }
              - { name: subject, type: String }
              - { name: negated, type: boolean }
              - { name: choice, type: boolean }
          - name: ExerciseShape
            _change: add
            type: enum
            fields:
              - { name: GAP_IN_SENTENCE }
              - { name: WHOLE_SENTENCE }
        interfaces:
          - name: HintReader
            _change: add
            visibility: internal
            exposes:
              - signature: "read(AuditableQuiz quiz,SentenceMode mode): Optional<ExerciseHint>"
              - signature: "shapeOf(AuditableQuiz quiz,SentenceMode mode): ExerciseShape"
          - name: HintAnswerCheck
            _change: add
            visibility: internal
            exposes:
              - signature: "check(ExerciseHint hint,AuditableQuiz quiz): List<HintIssue>"
        implementations:
          - name: HintCoherenceAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: hintReader, type: HintReader }
              - { name: answerCheck, type: HintAnswerCheck }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
          - name: DefaultHintReader
            _change: add
            implements: [HintReader]
          - name: DefaultHintAnswerCheck
            _change: add
            implements: [HintAnswerCheck]
            requiresInject:
              - { name: irregularForms, type: IrregularFormsCatalog }
```

## Reconocer los irregulares con una tabla, sin tocar el lematizador compartido
El lematizador compartido falla con algunos irregulares: *hated* da *hat*, *swum* sale adjetivo. Medido sobre el curso
del 29/9 con el mismo modelo, de 4.490 palabras de pista 761 se encuentran sólo por el lema, 75 con el lema equivocado
pero flexionadas regulares y 24 sólo con una tabla de irregulares (*good* → *well*, *better*). Arreglar el lematizador
mueve los números de vocabulario de todos, así que no se toca en esta ronda (DOUBT-PISTA-LEMATIZADOR): la pista acepta
la forma flexionada regular y consulta una tabla cerrada de formas irregulares, dato de referencia como el EVP, que no
lematiza el curso. Respuestas completas la usa también para saber si «'s» es *is* o *has*.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: IrregularFormsCatalog
        _change: add
        stereotype: port
        exposes:
          - signature: "formsOf(String baseWord): Set<String>"
          - signature: "isPastParticiple(String word): boolean"
  - name: vocabulary-infrastructure
    _change: modify
    packages:
      - name: irregularforms
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemIrregularFormsCatalog
            _change: add
            visibility: public
            implements: [IrregularFormsCatalog]
            requiresInject:
              - { name: catalogPath, type: Path }
```

## Decidir la resolución por el tipo de falta
José eligió que la regla arregle la pista por defecto y que la mesa vea cuando lo que está mal es la respuesta. Una
regla sin modelo no sabe qué enseña el tema, así que la resolución sale del tipo: si la palabra de la pista no está en
la respuesta (`HINT_WORD_ABSENT`), arreglar la pista cambiaría lo que pide el ejercicio y va a la mesa; el resto se
arregla completando la pista con el verbo base, el sujeto y el *not* de la primera respuesta, sin tocar el audio. Es
la opción recomendada de DOUBT-PISTA-CUANDO-MESA y vive en un solo lugar: si José elige otra, cambia ese mapeo. Con el
prototipo del analista, 60 de los 87 hallazgos irían a la mesa.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: add
        visibility: public
        models:
          - name: HintIssue
            _change: add
            type: record
            fields:
              - { name: rule, type: String }
              - { name: kind, type: HintIssueKind }
              - { name: word, type: String }
              - { name: proposedHint, type: String }
              - { name: resolution, type: FindingResolution }
          - name: HintIssueKind
            _change: add
            type: enum
            fields:
              - { name: MISSING_HINT }
              - { name: HINT_WORD_ABSENT }
              - { name: VERB_NOT_ANNOUNCED }
              - { name: SUBJECT_MISMATCH }
              - { name: SUBJECT_NOT_ANNOUNCED }
              - { name: NEGATION_WITHOUT_NEGATIVE }
```

## Dejar lo propio en un diagnóstico tipado
Lo que el hallazgo común no lleva —la pista, la primera respuesta, cuántos del tema la llevan, la pista propuesta—
va en el diagnóstico tipado, colgado del slot de `QuizDiagnoses` que agrega el integrador. La pista propuesta es el
arreglo por regla ya calculado, así que el paquete E no tiene que volver a leer el ejercicio para aplicarlo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: add
        visibility: public
        models:
          - name: HintCoherenceDiagnosis
            _change: add
            type: record
            fields:
              - { name: hint, type: String }
              - { name: firstAnswer, type: String }
              - { name: shape, type: ExerciseShape }
              - { name: hintedInTopic, type: int }
              - { name: comparableInTopic, type: int }
              - { name: issues, type: "List<HintIssue>" }
              - { name: score, type: double }
```

## Registrarse en el catálogo con su ficha
Como todo analizador nuevo de 0.3, es un proveedor. Ficha: pregunta «¿La pista le anuncia al alumno lo que tiene que
escribir?», lee la pista y la primera respuesta, reglas R001 a R004 instantáneas, familia errores, evalúa QUIZ,
resoluciones regla y mesa, costo instantáneo, meta cero ejercicios con error y el umbral de tres cuartos escrito en
la meta. Sin `planBinding` ni `config`: las tareas son del paquete E y el plan declara lo que no convierte.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: add
        visibility: public
        implementations:
          - name: HintCoherenceAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: irregularForms, type: IrregularFormsCatalog }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
features:
  - id: FEAT-PISTA
    code: F-PISTA
```

## Cómo se mide
Sobre una copia del curso en el scratchpad, `analyze --analyzers hint-coherence`; del resumen del informe sale
`marcas.json` con `analizador: pista` por cada ejercicio con hallazgo, y `medir.py marcas.json --parte escribir
--categoria pista`. La categoría del examen son los imposibles del 077 por cualquier causa: en `escribir/` hay 46 (15
anotados de pista). Metas: 9 de cada 10 de los anotados de pista en `examinar/` (lo corre José), cerca de 4 de cada 10
de la categoría entera, y 8 de cada 10 marcas nuevas reales en la muestra. El prototipo del analista encontraba los 15
anotados y 21 de los 46.

## Lo que no se mueve
hint-coherence es de la familia de errores: el puntaje de vocabulario del curso (73,9 %) y de los niveles (96,5 · 94,2
· 79,5 · 63,5) queda igual, y sus números van aparte, como ejercicios con error. El lematizador compartido no cambia,
así que ningún analizador de vocabulario cambia de número.

## Declarar las 28 pruebas y ubicar el journey
Las 28 son nuevas: content-audit no tiene tests de estas reglas, así que no hay nada que portar. Con el criterio del
qa-tester de la opción múltiple, sólo se declara lo que verifica una regla, y cada nombre lleva un caso real del curso.
Las cinco reglas quedan cubiertas (R001 5, R002 11, R003 4, R004 2, R005 6). De dónde sale cada una:

- **Los casos del REQUIREMENT** (con su id ahí): las verduras, «Who ___ (I / see)?», «What ___ (it / be)?», «___ (not /
  dream) about her.», «This idea ___ (not / sound)…», «It ___ (be) soon.», «That was ___ (bad)…», «This sentence ___
  (mean) 'hope'.», «You ___ (see) cats.» (1ec3) y los temas El posesivo y One y ones.
- **El curso del 29/9** (`db/english-course`, sólo lectura; los últimos 4 del id, como en los casos de oro): «She
  dances ___ (good / well).» 255d, «They ___ (swim) in the ocean» 3f4e, «She ___ (hate) broccoli…» 3f80, «She ___
  (drink) juice…» 47e3, «(him) ___» 2420, «A: Am I wrong? B: No, you ___ .» 1f53, «She / assist you.» 37ba, «Polite
  request: I want to eat some cake.» 24f7, «She ___ (live) there. (present simple)» 2069, «Where / you / going? (be
  extra polite)» 3e0b y «My brother is a ___ (good / well) driver.» 256a.
- **El umbral de tres cuartos** tiene su propia prueba: tres cuartos exactos alcanzan, y One y ones (12 de 20 huecos
  para escribir con pista) queda sin hallazgo.
- **Lo que depende de otros**: la evidencia, del renderizador del integrador (A-01); mandar a la mesa la palabra que no
  está en la respuesta, de la opción A de DOUBT-PISTA-CUANDO-MESA, abierta; las dos del adaptador, de la tabla instalada
  (A-16) si la leen en vez de un archivo de prueba. Que «'ve» cuente como auxiliar es lo que ata `isPastParticiple` a R002.
- **El journey** va a `audit-domain`, paquete `hintcoherence`, junto al analizador y sus colaboradores package-private,
  como los de F-QINST y F-LREC. `generate` crearía `FPistaJ001JourneyTest` con 3 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: hintcoherence
        _change: add
        implementations:
          - name: HintCoherenceAnalyzer
            _change: add
            handwrittenTests:
              - name: "should take exactly three quarters of the topic as reaching the threshold and leave a topic below it without finding, as «I'd love ___ .» in One y ones, where 12 of 20 carry a hint"
                traceability: { feature: FEAT-PISTA, rule: F-PISTA-R001 }
features:
  - id: FEAT-PISTA
    code: F-PISTA
    journeys:
      - { id: F-PISTA-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.hintcoherence }
```
