---
patch: FEAT-RESPC
requirement: 2026-09-30.04_respuestas-completas
generated: 2026-09-30T19:05:00Z
---

# Tech Spec: Respuestas completas (FEAT-RESPC), paquete A de la ronda 1

Diseñado contra el modelo que queda después de los parches de 0.2 (FEAT-OPMUL) y 0.3 (FEAT-HALL), aplicados en ese
orden: `sentinel-baseline.yaml` es ese modelo, armado en memoria sin tocar el `sentinel.yaml` del worktree. De dónde
sale cada tipo que el parche usa y no declara:

- **De 0.3 (FEAT-HALL)**: `AnalyzerProvider` (la raíz del catálogo), `ContentAnalyzer.findingsAt`, `FindingDraft`,
  `FindingEvidence`, `EvidencePart`, `FindingSeverity`, `FindingResolution`, `AnalysisCost`, `AnalyzerRuleCard`,
  `AnalyzerFamily` y los campos nuevos de `AnalyzerDescriptor` (la ficha).
- **De 0.2 (FEAT-OPMUL)**: `AuditableQuiz.multipleChoice`, que dice qué ejercicio es de opción múltiple y por eso
  queda fuera de este analizador.
- **Del parche de FEAT-PISTA** (este mismo paquete): `IrregularFormsCatalog`.
- **Del integrador de la ronda 1**: `QuizStudentViewRenderer`, el renderizador del ejercicio como lo ve el alumno que
  0.3 le dejó; y `AuditableQuiz.miniTheory`. Están en `ronda1/ganchos-A.md` del tablero.

## Comparar como el corrector, con su tabla, en un solo lugar
Cuatro de las cinco reglas del paquete dicen «comparado como el corrector», y José decidió que haya una sola tabla de
formas equivalentes, la del corrector de Go. `CorrectorMatcher` es esa comparación: la normalización de
`cloze_sentence_part.go` más la tabla. `canonicalForms` es la primitiva —el conjunto de formas canónicas de un texto—
porque Datos necesita «uno contiene al otro» y Duplicados agrupa; `accepts` y `sameAnswer` son lo que piden
Respuestas completas y Opción múltiple. Si el paquete C la usa para su juez de «una sola respuesta», pasa a
compartida sin cambios.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: CorrectorMatcher
        _change: add
        stereotype: port
        exposes:
          - signature: "canonicalForms(String text): Set<String>"
          - signature: "accepts(List<String> acceptedOptions,String written): boolean"
          - signature: "sameAnswer(String first,String second): boolean"
          - signature: "tableVersion(): String"
    packages:
      - name: correctormatch
        _change: add
        visibility: public
        implementations:
          - name: DefaultCorrectorMatcher
            _change: add
            visibility: public
            implements: [CorrectorMatcher]
            requiresInject:
              - { name: table, type: FormEquivalenceTable }
```

## Guardar la tabla del corrector como dato, con el formato que fije Go
La tabla todavía no existe: la construye en paralelo la rama `claude/corrector-formas-equivalentes` de ittm. Estos
modelos son la interfaz que content-audit espera —contracciones con sus expansiones, grupos de palabras
equivalentes, y si el corrector mueve el *not* de una pregunta negativa— y si Go elige otra forma sólo cambian ellos
y el adaptador. «'s» y «'d» llevan dos expansiones cada uno porque, si el corrector toma «'s» sólo como *is*, quedan
345 ejercicios sin igualar en vez de 142; y `invertsNegativeQuestions` existe porque esos 142 son preguntas negativas
y *question tags*. La implementación se prueba con la copia de los casos de oro del corrector
(`casos-de-oro.json`, al lado de la tabla): el test del qa-tester corre cada caso por `DefaultCorrectorMatcher` y falla
si la copia de la tabla y los casos no coinciden.

```architecture
modules:
  - name: audit-domain
    _change: modify
    interfaces:
      - name: FormEquivalenceTableSource
        _change: add
        stereotype: port
        exposes:
          - signature: "load(): FormEquivalenceTable"
    packages:
      - name: correctormatch
        _change: add
        visibility: public
        models:
          - name: FormEquivalenceTable
            _change: add
            type: record
            fields:
              - { name: version, type: String }
              - { name: contractions, type: "List<ContractionExpansion>" }
              - { name: equivalentWords, type: "List<EquivalentWordGroup>" }
              - { name: invertsNegativeQuestions, type: boolean }
          - name: ContractionExpansion
            _change: add
            type: record
            fields:
              - { name: contracted, type: String }
              - { name: expansions, type: "List<String>" }
              - { name: wholeWord, type: boolean }
          - name: EquivalentWordGroup
            _change: add
            type: record
            fields:
              - { name: equivalenceClass, type: FormEquivalenceClass }
              - { name: words, type: "List<String>" }
          - name: FormEquivalenceClass
            _change: add
            type: enum
            fields:
              - { name: CONTRACTION }
              - { name: BE_NEGATION }
              - { name: SPELLING }
              - { name: PREPOSITION }
```

## Leer la copia que corresponde al corrector en producción
content-audit corre sin red, así que lee una copia versionada, al lado del EVP en `analysis/recursos-compartidos/`
(`corrector/formas-equivalentes.json` y `corrector/casos-de-oro.json`). La copia es la del corrector que está en
producción, no la de la rama: el orden de salida es corrector primero y contenido después, y una copia adelantada
haría sacar consignas antes de que el corrector acepte la otra forma. Hoy la copia es una tabla vacía con versión
«corrector de hoy»; se reemplaza en el mismo paso en que sale el corrector. Sin archivo la corrida no arranca: la tabla
vacía es un dato, la falta del archivo es un error de instalación.

```architecture
modules:
  - name: vocabulary-infrastructure
    _change: modify
    packages:
      - name: correctortable
        _change: add
        visibility: internal
        implementations:
          - name: FileSystemFormEquivalenceTableSource
            _change: add
            visibility: public
            implements: [FormEquivalenceTableSource]
            requiresInject:
              - { name: tablePath, type: Path }
```

## Armar las formas con reglas propias y preguntarle al corrector
El analizador genera las formas equivalentes de cada respuesta con reglas propias —las de R002 a R004, con su lista
cerrada— y le pregunta a `CorrectorMatcher` si están aceptadas. Generar con la tabla del corrector sería más corto
pero ciego: si un día la tabla pierde una equivalencia, generador y comparación la pierden juntos y nadie se entera;
así, la forma reaparece como hallazgo, y ése es el control de que nada vuelve atrás que pidió José. Para saber si
«'s» es *is* o *has* el generador pregunta si la palabra que sigue es participio a la tabla de irregulares, la misma
que usa la pista, en vez de tener otra lista. Evalúa sólo QUIZ de hueco para escribir: uno con `multipleChoice`
(0.2) no alcanza.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: answercompleteness
        _change: add
        visibility: public
        models:
          - name: EquivalentForm
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: form, type: String }
              - { name: equivalenceClass, type: FormEquivalenceClass }
        interfaces:
          - name: EquivalentFormGenerator
            _change: add
            visibility: internal
            exposes:
              - signature: "generate(String acceptedAnswer,String wordBeforeGap): List<EquivalentForm>"
        implementations:
          - name: AnswerCompletenessAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: formGenerator, type: EquivalentFormGenerator }
              - { name: requestDetector, type: FormRequestDetector }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
          - name: DefaultEquivalentFormGenerator
            _change: add
            implements: [EquivalentFormGenerator]
            requiresInject:
              - { name: irregularForms, type: IrregularFormsCatalog }
```

## Marcar la consigna que pide una forma y decir si una regla la saca
La regla nueva de José (R007) mira la consigna que ve el alumno y la miniteoría del ejercicio. `removableByRule`
separa los 654 cuyo pedido es la oración estándar de #1152 —una regla la saca entera— de los 40 de *Contracciones*,
donde el pedido es la lección y va a la mesa (DOUBT-RESPC-CONTRACCIONES). El arreglo sólo es seguro cuando el
corrector ya acepta las dos formas: quien lo aplique (paquete E) lo sabe mirando el mismo diagnóstico, porque si
todavía faltan formas de clase CONTRACTION o BE_NEGATION en ese ejercicio, el corrector no salió.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: answercompleteness
        _change: add
        visibility: public
        models:
          - name: FormRequest
            _change: add
            type: record
            fields:
              - { name: source, type: FormRequestSource }
              - { name: requestedForm, type: RequestedForm }
              - { name: phrase, type: String }
              - { name: removableByRule, type: boolean }
          - name: FormRequestSource
            _change: add
            type: enum
            fields:
              - { name: INSTRUCTIONS }
              - { name: MINI_THEORY }
          - name: RequestedForm
            _change: add
            type: enum
            fields:
              - { name: SHORT }
              - { name: LONG }
        interfaces:
          - name: FormRequestDetector
            _change: add
            visibility: internal
            exposes:
              - signature: "detect(String instructions,String miniTheory): List<FormRequest>"
        implementations:
          - name: DefaultFormRequestDetector
            _change: add
            implements: [FormRequestDetector]
```

## Dejar lo propio en un diagnóstico tipado
Lo que el hallazgo común no lleva —la respuesta de la que sale la forma, su clase, la versión de la tabla— va en el
diagnóstico tipado, nunca como campo extra del hallazgo (F-HALL-R001 inv. 3). La clase dice dónde se arregla: las
cuatro de hoy las iguala el corrector nuevo, así que esos hallazgos no se aplican al curso y desaparecen cuando se
actualiza la copia de la tabla. El diagnóstico se cuelga del slot tipado de `QuizDiagnoses`, que es un gancho del
integrador.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: answercompleteness
        _change: add
        visibility: public
        models:
          - name: AnswerCompletenessDiagnosis
            _change: add
            type: record
            fields:
              - { name: missingForms, type: "List<MissingAcceptedForm>" }
              - { name: formRequests, type: "List<FormRequest>" }
              - { name: tableVersion, type: String }
              - { name: score, type: double }
          - name: MissingAcceptedForm
            _change: add
            type: record
            fields:
              - { name: gapIndex, type: int }
              - { name: acceptedAnswer, type: String }
              - { name: missingForm, type: String }
              - { name: equivalenceClass, type: FormEquivalenceClass }
              - { name: severity, type: FindingSeverity }
```

## Registrarse en el catálogo con su ficha
Un analizador nuevo es un proveedor y nada más de lo compartido (0.3). La ficha dice: pregunta «¿El corrector acepta
todas las formas de la respuesta, y ninguna consigna pide una?», lee las respuestas aceptadas, la consigna y la
miniteoría, reglas R001 a R007 todas instantáneas, familia errores, evalúa QUIZ, resoluciones regla y mesa, costo
instantáneo, y como meta cero ejercicios con error. `planBinding()` vuelve vacío: qué tarea crea es del paquete E, y
hasta entonces el plan declara los puntajes que no convierte (F-HALL-R004 inv. 3) sin cambiar las 4.036 tareas de hoy.
`config()` también vuelve vacío: la lista cerrada es parte de la regla, no un parámetro.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: answercompleteness
        _change: add
        visibility: public
        implementations:
          - name: AnswerCompletenessAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: irregularForms, type: IrregularFormsCatalog }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
features:
  - id: FEAT-RESPC
    code: F-RESPC
```

## Cómo se mide
Con el examen de `docs/briefs/077-revision-de-ejercicios/examen/`: sólo `escribir/` y `medir.py`; `examinar/` lo corre
José. Se copia el curso (`db/english-course`) al scratchpad, porque `analyze` escribe informes, y se corre
`analyze --analyzers answer-completeness` (F-HALL-R012). Del resumen del informe (`AuditDigest`, 0.3) sale un
`marcas.json` con un `{quizId, analizador}` por ejercicio con hallazgo que cuenta: las reglas R002 a R005 van como
`respuestas`, y R007 como `respuestas-consigna`, que `medir.py` no mide porque el 077 no tenía ese criterio. Adaptar
`leer_marcas()` al resumen es un gancho. Dos corridas:

- **Con la copia de hoy** (tabla vacía): `medir.py marcas.json --parte escribir --categoria respuestas`. Meta: 95 de
  cada 100 de los 128 (el prototipo encuentra 123; el del analista, 127) y 8 de cada 10 marcas nuevas reales en la
  muestra. Es el control de que el analizador ve lo que el corrector de hoy rechaza.
- **Con la tabla del corrector nuevo**: los 128 dejan de ser errores —el corrector arregla 127 y «café» ya da bien—, y
  la meta es cero marcas de las clases que iguala la tabla. Lo que quede es lo que la tabla no cubre (142 preguntas
  negativas si el corrector no mueve el *not*).
- **R007** es un censo y se compara contra él: 694 consignas (A1 554, A2 100, B1 20, B2 20) y 3 ejercicios con
  miniteoría que pide (43 si José elige B en DOUBT-RESPC-DESCRIBE).

La presentación prometía 122 de 128; con la decisión de José el número pasa a medir el control de la primera corrida,
y el arreglo de esos 128 lo hace el corrector.

## Lo que no se mueve
answer-completeness es de la familia de errores: no entra en el puntaje de vocabulario de ningún nodo (F-HALL-R009),
así que el curso sigue en 73,9 % y los niveles en 96,5 · 94,2 · 79,5 · 63,5; sus números van aparte, como ejercicios
con error. No cambia ningún analizador existente ni el plan (sin `DiagnosisKind` nueva), y analizar no escribe el
curso (F-HALL-R015).

## Declarar las 38 pruebas y ubicar el journey
Las 38 son nuevas: no hay tests previos de estas reglas que portar. Con el criterio del qa-tester de la opción
múltiple, sólo se declara lo que verifica una regla, y cada nombre lleva un caso real. Las siete reglas quedan
cubiertas (R001 7, R002 7, R003 2, R004 3, R005 8, R006 3, R007 8). De dónde sale cada una:

- **Los casos del REQUIREMENT**: «They ___ (not / be) from Ireland.», que acepta «'re not» y enseña «aren't»; el
  «café»; «I ___ (not / eat) meat often.»; «There is a house…»; «travelled», «traveled» y «until then».
- **El curso del 29/9** (últimos 4 del id): «…cycle to school every day.» 4570; «Usa $long forms$.» en «They ___ (not /
  have) many cars.» 1ed7; la miniteoría que pide, en «It ___ (be) hot inside.» 1fa5; «I ___ (not / be) forty.» 1fa8;
  «Peter / read / a book.» 20d1; «Doesn't she dance?» 412c; «No, it ___ .» 1f5d y 26db; Contracciones 1f3b; «'s
  hated» 3f80; «'s trained» 3061; «___ a child? (He)» 1e87; «You ___ (see) cats.» 1ec3. «Usa $short forms$ si es
  posible.» es la consigna de 25ac.
- **Los casos de oro del corrector.** Dos pruebas de `DefaultCorrectorMatcher` corren los casos de `testdata/` de
  `be/domain/quiz/pkg/shared` (rama `claude/corrector-formas-equivalentes` de ittm, en construcción: hoy cuatro archivos
  y 153 casos). Con la tabla vacía de hoy tiene que decidir lo que dice su columna «hoy»; con la copia de la tabla del
  corrector nuevo, lo que dice «acepta». Fallan si la copia de content-audit y los casos de oro no coinciden.
  Dependen de A-15, que no trae una tabla sino casos (la tabla del corrector es código Go, así que la copia de
  content-audit es propia y los casos son el único contrato); de A-16, que tiene que copiar los cuatro archivos tal
  cual; y del contexto del hueco: el corrector lee «'s» y «'d» por lo que sigue (antes y después, sin pistas), y
  `CorrectorMatcher.accepts` no lo recibe.
- **Lo demás que depende de otros**: la evidencia (A-01); la miniteoría (A-02, A-03) en la gravedad alta y en R007;
  dos del detector siguen la recomendación de DOUBT-RESPC-CONTRACCIONES y DOUBT-RESPC-DESCRIBE, abiertas; «has trained»
  lee R005 así: lo que el corrector nuevo sigue rechazando (su clase «límite») se agrega con «|».
- **No se declara**, porque ninguna regla lo pide, que falte el archivo de la tabla sea un error.
- **El journey** va a `audit-domain`, paquete `answercompleteness`. `generate` crearía `FRespcJ001JourneyTest` con 5
  caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: correctormatch
        _change: add
        implementations:
          - name: DefaultCorrectorMatcher
            _change: add
            handwrittenTests:
              - name: "should decide every golden case of the backend corrector as its «hoy» column says with today's empty table, as «traveled» rejected for «She ___ (travel) to Europe.» answered «travelled»"
                traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
              - name: "should decide every golden case of the backend corrector as its «acepta» column says with the copy of the new corrector's table, reading each gap with the sentence around it, as «is» for «It ___ (be) hot inside.» answered «'s»"
                traceability: { feature: FEAT-RESPC, rule: F-RESPC-R001 }
features:
  - id: FEAT-RESPC
    code: F-RESPC
    journeys:
      - { id: F-RESPC-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.answercompleteness }
```
