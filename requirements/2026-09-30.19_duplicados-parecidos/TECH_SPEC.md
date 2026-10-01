---
patch: FEAT-DUPAR
requirement: 2026-09-30.19_duplicados-parecidos
generated: 2026-10-01T00:05:00Z
---

# Tech Spec: Duplicados parecidos, dos ejercicios casi iguales del mismo tema (FEAT-DUPAR)

Paquete B de la ronda 2, rebasado el 30/9 a la noche sobre el `sentinel.yaml` de `ronda1/integracion`. Este parche trae
sólo el analizador `near-duplicates`. El modelo —`EmbeddingModel`, con su adaptador de Ollama— y la decisión de publicar
son de FEAT-MLOCAL, que se aplica antes. De la ronda 1 usa, sin declarar otras, `CorrectorMatcher` (FEAT-RESPC) para «la
misma respuesta, comparada como el corrector», `QuizStudentViewRenderer` (gancho A-01) para el ejercicio terminado y la
evidencia como la ve el alumno, y el runtime de los jueces (`JudgeRuntime`) con el evaluador de claude -p de FEAT-JEXAM.
Lo decidido por José el 30/9: cuenta como error de gravedad baja, con umbral 0,95; y, esa noche, sin Ollama claude -p
juzga los pares candidatos por parecido de texto.

## Comparar sólo lo que pide la misma respuesta, dentro del tema

Sin esa condición, los pares más parecidos del curso son contrastes a propósito: «I observed him clean the room.» y
«…cleaning the room.» dan 0,995 y la diferencia es justo lo que se enseña (R001). La misma respuesta es la del corrector,
con la contraída igual a la larga: `CorrectorMatcher.sameAnswer`, la única tabla de equivalencias (decisión 1 de José),
sin contar el pronombre o el demostrativo que el alumno copia del enunciado. Los iguales tal cual son de
`exact-duplicates`, y se descartan con la misma comparación que usa FEAT-DUPEX, así que ningún par cuenta en los dos. El
comodín tapa pronombres, demostrativos, posesivos y el tiempo del auxiliar, y devuelve qué cambió para la evidencia
(«He → She»): solo, encuentra 24 pares y 5 de los 6 del 077. `wordSimilarity` es el parecido de palabras de R008 —1 menos
la distancia de edición por palabras sobre el largo mayor—, texto puro y barato: los 24.865 pares que el comodín no
resuelve se recorren en segundos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        visibility: public
        interfaces:
          - name: NearDuplicateRules
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "sameRequest(AuditableQuiz first,AuditableQuiz second): boolean"
              - signature: "wildcardDifference(AuditableQuiz first,AuditableQuiz second): Optional<String>"
              - signature: "wordSimilarity(AuditableQuiz first,AuditableQuiz second): double"
        implementations:
          - name: DefaultNearDuplicateRules
            _change: add
            implements: [NearDuplicateRules]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Un vector por ejercicio, guardado con su versión

El parecido de R002 inv. 2 es el coseno de dos vectores de nomic-embed-text, cada uno del ejercicio terminado con el
prefijo «clustering: », que el modelo pide y con el que se midió. Cada vector se guarda en el libro de evaluaciones con
la versión examinada —el modelo con su huella y el `methodId` del prefijo— en el id del evaluador: el curso entero
cuesta unos 4 minutos la primera vez y después sólo se pide el de lo que cambió (F-MLOCAL-R002 inv. 2), y el coseno sale
siempre de los mismos vectores, así que dos corridas marcan lo mismo. Su `evaluatorVersion()` es esa versión examinada:
el runtime frena con la misma clave de examen que `LocalToolGate`. El umbral, 0,95, el prefijo y el parecido de
palabras de los candidatos de claude -p, 0,6, quedan en la configuración, a la vista en `config analyzer near-duplicates`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        visibility: public
        interfaces:
          - name: NearDuplicatesConfig
            _change: add
            visibility: internal
            sealed: false
            extends: [SelfDescribingConfig]
            exposes:
              - signature: "methodId(): String"
              - signature: "inputPrefix(): String"
              - signature: "similarityThreshold(): double"
              - signature: "candidateWordSimilarity(): double"
        implementations:
          - name: SentenceEmbeddingEvaluator
            _change: add
            implements: [Evaluator]
            requiresInject:
              - { name: embeddingModel, type: EmbeddingModel }
              - { name: config, type: NearDuplicatesConfig }
              - { name: examinedVersion, type: String }
          - name: DefaultNearDuplicatesConfig
            _change: add
            implements: [NearDuplicatesConfig]
```

## Sin Ollama, claude -p juzga los pares de texto parecido

Sin el modelo local no hay vectores, así que la condición 2 la decide claude -p (R008): se le pregunta por cada par que
pide la misma respuesta, que el comodín no resuelve y que cambia como mucho 4 de cada 10 palabras —699 pares en el curso,
276 en A1—, si para el alumno es el mismo ejercicio. `SameExerciseQuestion` es esa pregunta: arma lo que lee el juez,
los dos ejercicios como los ve el alumno en el orden del tema, y lee su veredicto, `{"sameExercise", "reason"}`. El
contenido sale de `QuizStudentViewRenderer`, cuyo formato ya congelan cuatro jueces (C-07): ahora son cinco, y cambiarlo
después de pagar vuelve a cobrar también estos pares (gancho B-22). Un veredicto que no se puede leer deja el par sin
juzgar, nunca lo da por casi duplicado.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        visibility: public
        models:
          - name: SameExerciseVerdict
            _change: add
            type: record
            visibility: internal
            fields:
              - { name: sameExercise, type: boolean }
              - { name: reason, type: String }
        interfaces:
          - name: SameExerciseQuestion
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "subjectOf(AuditableQuiz kept,AuditableQuiz copy): EvaluationSubject"
              - signature: "read(String payload): Optional<SameExerciseVerdict>"
        implementations:
          - name: DefaultSameExerciseQuestion
            _change: add
            implements: [SameExerciseQuestion]
            requiresInject:
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## La copia lleva el hallazgo; el primero del tema queda

La comparación es entre hermanos: en `onKnowledge`, que el motor llama con los ejercicios del tema en el orden del curso,
como `exact-duplicates`, se arman los pares de cada ejercicio con los anteriores, se resuelve el comodín y se piden las
consultas —un vector por ejercicio con Ollama, un juicio por par candidato sin él—; en `onCourseComplete` se resuelven
todas juntas con el plan y `resolveAll` del runtime, como los jueces de C, y se decide: el primero con el que un
ejercicio es casi igual es el que queda (R003), así que hay un hallazgo por copia, nunca en el que queda, y su identidad
—la regla y el ejercicio que queda— no cambia entre corridas. Si se cumplen varias condiciones gana el comodín, que no
depende de ningún modelo. Es bajo, a la mesa y cuenta como error (R004), con el costo de su regla: `same-but-wildcard`,
`same-meaning` o `same-exercise`. El comodín corre siempre; un ejercicio con alguna consulta que el tope o una falla
dejaron sin resolver queda a medias (`partiallyEvaluatedBy`, F-RONDA1-R006), y una versión sin examen aprobado deja todo
sin evaluar (F-MLOCAL-R006). La evidencia trae los dos ejercicios como los ve el alumno, el que queda como nodo
relacionado y una línea con qué cambia, cuánto se parecen o cuántas palabras cambian y la razón de claude -p (R005).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        visibility: public
        models:
          - name: NearDuplicateCondition
            _change: add
            type: enum
            fields:
              - { name: WILDCARD }
              - { name: SIMILARITY }
              - { name: JUDGED }
          - name: NearDuplicateDiagnosis
            _change: add
            type: record
            fields:
              - { name: keptQuizId, type: String }
              - { name: condition, type: NearDuplicateCondition }
              - { name: difference, type: String }
              - { name: similarity, type: double }
              - { name: wordSimilarity, type: double }
              - { name: judgeReason, type: String }
              - { name: finishedExercise, type: String }
              - { name: keptFinishedExercise, type: String }
              - { name: score, type: double }
        implementations:
          - name: NearDuplicatesAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: rules, type: NearDuplicateRules }
              - { name: question, type: SameExerciseQuestion }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: config, type: NearDuplicatesConfig }
              - { name: gateDecision, type: LocalToolGateDecision }
```

## El proveedor elige la ruta y arma la ficha

En `create(policy)` el proveedor pide la decisión a `LocalToolGate` y abre por el runtime de los jueces: con
nomic-embed-text disponible, su `SentenceEmbeddingEvaluator` con la política sin tope ni pedido de la decisión; sin él,
la alternativa de claude -p que recibe hecha, con la política de la corrida, que sin `--budget near-duplicates=N` tiene
tope 0. Ya no recibe la fábrica de sesiones. La ficha: pregunta «¿Dos ejercicios del mismo tema piden casi lo mismo?»,
lee el ejercicio terminado y la respuesta de cada ejercicio del tema, reglas `same-but-wildcard` instantánea,
`same-meaning` de modelo local y `same-exercise` de modelo pago, meta 0 copias por tema, familia errores, evalúa QUIZ,
resolución mesa y costo modelo pago, el mayor de sus reglas. Lleva dos tarjetas, nomic-embed-text y claude -p, con su
examen (B-01), y no tiene ligadura al plan: qué tarea crea lo decide E (B-18). No entra en la corrida base, como decidió
José; si se adopta A-09 tal como está, lo metería por su regla instantánea (B-10).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        visibility: public
        implementations:
          - name: NearDuplicatesAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: embeddingModel, type: EmbeddingModel }
              - { name: alternativeJudge, type: Evaluator }
              - { name: judgeRuntime, type: JudgeRuntime }
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: localToolGate, type: LocalToolGate }
```

## Cómo se prueba y cómo se mide

Las pruebas no llaman a Ollama ni a claude: usan un doble de `EmbeddingModel` con los vectores que grabó el prototipo
(`scratchpad/ronda2-B/dup/emb-nomic.json`, sólo los de los casos), un `Evaluator` doble para la alternativa que contesta
`{"sameExercise", "reason"}` y cuenta cuántas veces lo llaman, y el runtime de C con libros en memoria. Casos reales:
«He is certainly leaving.» / «She is certainly leaving.» (A2, en el 077), por el comodín; «Have you ever been to Rome?» /
«Have you ever visited Rome?» (B1, en el 077), por parecido 0,99 o, sin Ollama, candidato de 0,67 que claude -p juzga;
«I am sorry.» / «I am hot.» (A1), candidato que claude -p descarta; «That is his car.» / «That car is his.» (A1, 0,50),
que sin Ollama ni se consulta; «I observed him clean the room.» / «…cleaning the room.», otra respuesta, sin marca; y un
par exacto, que queda para `exact-duplicates`. Fijan además que sin tope no se consulta y el ejercicio queda a medias
con los hallazgos del comodín. La prueba con nomic-embed-text de verdad lleva la etiqueta `herramienta-real`. El examen
(R007) se rinde con `exam run near-duplicates` y se mide con `medir.py`; el de claude -p, con `CONTENT_AUDIT_OLLAMA_URL=off`:
los candidatos de los temas de cada mitad, 437 en `escribir/` y 262 en `examinar/`.

## Registrar FEAT-DUPAR

El parche registra la feature con su código para que `feature sync --feature FEAT-DUPAR` anote J001 después de aplicar;
la ubicación del journey es del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
features:
  - id: FEAT-DUPAR
    code: F-DUPAR
```

## Declarar las 38 pruebas y ubicar el journey

Las 38 son nuevas: no hay pruebas previas de estas reglas que portar. Cada una traza a una regla de FEAT-DUPAR (R001 5,
R002 8, R003 3, R004 3, R005 3, R006 2, R007 3, R008 11) y lleva un par del curso del 29/9 con los parecidos que midió el
prototipo (`scratchpad/ronda2-B/dup/pares.json`, `marcados-0.95.json` y `arq-B2/candidatos-lex-0.6.json`); el doble de
`EmbeddingModel` devuelve sus vectores y el de claude -p, un veredicto fijo por par.

- **El par candidato (R001)**: el pronombre copiado del enunciado no cuenta (`…1ea1` / `…1eac`), otra respuesta no es
  candidato (`…41cb` / `…41cc`, el contraste a propósito de 0,995), «'ll wash» = «will wash», sólo dentro del tema, y el
  par exacto de Práctica mixta (`…224b` / `…2260`) queda para `exact-duplicates` mientras los dos son copias casi
  iguales de `…2231`.
- **Las dos condiciones (R002)**: el comodín con «he → she» (`…208f` / `…2097`) y con el tiempo del auxiliar (`…2b6b` /
  `…2b7f`), que no ve otro verbo (`…46ac` / `…46b0`); el parecido 0,98 marca ese par y 0,94 no (`…3083` / `…30a5`); el
  vector con «clustering: » y la versión en el id del evaluador.
- **La copia y su hallazgo (R003 a R005)**: el primero del tema queda, n − 1 en un grupo (Respuestas cortas, `…251c` a
  `…251e`), las mismas copias en dos corridas, bajo, a la mesa, 0 y 1, el costo de la regla que lo encontró —si lo
  encuentran el comodín y el vector, el comodín (`…1f5e` / `…25e8`)— y las tres evidencias.
- **La ficha y el examen (R006, R007)**: tres reglas y dos tarjetas, todo sin evaluar sin examen aprobado, comodín
  incluido, el mismo resultado al rendir dos veces y que otro prefijo es otra versión.
- **Sin Ollama (R008)**: el parecido de palabras (0,67 en `…355e` / `…4242`), qué se consulta y qué no (`…1e61` /
  `…1e64` sí, 0,67; `…23f9` / `…23fe` no, 0,50), el «sí» y el «no» de claude -p (`…2725` / `…2fd5`), el comodín siempre,
  a medias sin tope, una consulta por par hasta el tope, el contenido y el veredicto ilegible.

Algunos pares son del examen del 077 (`…2b6b`, `…355e`, `…25e8`, los de Respuestas cortas, `…224b` y `…2260`): son los
del REQUIREMENT o del prototipo, y las pruebas los usan con dobles. **No se declara** su tipo de tarea
`NEAR_DUPLICATES`, que prueba FEAT-RONDA2 después de aplicar (F-RONDA2-R008).

**El journey** va a `audit-domain`, paquete `nearduplicates`: el analizador y sus piezas son package-private ahí y los
puertos se simulan, como F-DUPEX-J001. `generate` crearía `FDuparJ001JourneyTest` con 5 caminos (`marca` llega por dos
salidas).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: add
        implementations:
          - name: NearDuplicatesAnalyzer
            _change: add
            handwrittenTests:
              - name: "should leave the two exact «They weren't at the park.» of Práctica mixta to exact-duplicates and still mark both as near copies of «You weren't at the park.»"
                traceability: { feature: FEAT-DUPAR, rule: F-DUPAR-R001 }
              - name: "should leave half evaluated an exercise with a candidate left unjudged for lack of budget, keeping its wildcard findings and saying how many pairs are missing"
                traceability: { feature: FEAT-DUPAR, rule: F-DUPAR-R008 }
features:
  - id: FEAT-DUPAR
    code: F-DUPAR
    journeys:
      - { id: F-DUPAR-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.nearduplicates }
```
