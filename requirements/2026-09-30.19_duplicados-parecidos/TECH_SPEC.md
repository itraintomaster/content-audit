---
patch: FEAT-DUPAR
requirement: 2026-09-30.19_duplicados-parecidos
generated: 2026-09-30T22:00:00Z
---

# Tech Spec: Duplicados parecidos, dos ejercicios casi iguales del mismo tema (FEAT-DUPAR)

Paquete B de la ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. Este parche trae sólo el analizador
`near-duplicates`. El modelo —`EmbeddingModel`, con su adaptador de Ollama— y la decisión de publicar son de
FEAT-MLOCAL, que se aplica antes. De la ronda 1 usa dos piezas del paquete A, sin declarar otras: `CorrectorMatcher`
(FEAT-RESPC), para «la misma respuesta, comparada como el corrector», y `QuizStudentViewRenderer` (gancho A-01), para el
ejercicio terminado de FEAT-DUPEX y la evidencia como la ve el alumno. `patch validate` pasa hoy; `generate`, después de
integrar la ronda 1. Lo decidido por José el 30/9: cuenta como error de gravedad baja, con umbral 0,95.

## Comparar sólo lo que pide la misma respuesta, dentro del tema

Sin esa condición, los pares más parecidos del curso son contrastes a propósito: «I observed him clean the room.» y
«…cleaning the room.» dan 0,995 y la diferencia es justo lo que se enseña (R001). La misma respuesta es la del corrector,
con la contraída igual a la larga: `CorrectorMatcher.sameAnswer`, la única tabla de equivalencias (decisión 1 de José),
sin contar el pronombre o el demostrativo que el alumno copia del enunciado. Los iguales tal cual son de
`exact-duplicates`, y se descartan con la misma comparación que usa FEAT-DUPEX, así que ningún par cuenta en los dos. El
comodín tapa pronombres, demostrativos, posesivos y el tiempo del auxiliar, y devuelve qué cambió para la evidencia
(«He → She»): solo, encuentra 24 pares y 5 de los 6 del 077.

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
        implementations:
          - name: DefaultNearDuplicateRules
            _change: add
            implements: [NearDuplicateRules]
            requiresInject:
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
```

## Un vector por ejercicio, guardado con su versión

El parecido (R002 inv. 2) es el coseno de dos vectores de nomic-embed-text, cada uno del ejercicio terminado con el
prefijo «clustering: », que el modelo pide y con el que se midió. Cada vector se guarda en el libro de evaluaciones con
la versión examinada —el modelo con su huella y el `methodId` del prefijo— en el id del evaluador: el curso entero
cuesta unos 4 minutos la primera vez y después sólo se pide el de lo que cambió (F-MLOCAL-R002 inv. 2), y el coseno sale
siempre de los mismos vectores, así que dos corridas marcan lo mismo. Son unos 4 KB por ejercicio y 46 MB el curso: sin
el índice del gancho B-09, el libro relee ese archivo en cada consulta. El umbral, 0,95, y el prefijo quedan en la
configuración, a la vista en `config analyzer near-duplicates`.

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

## La copia lleva el hallazgo; el primero del tema queda

La comparación es entre hermanos, así que se hace en `onKnowledge`, que el motor llama antes que a los ejercicios del
tema y con ellos en el orden del curso, como `exact-duplicates`: cada ejercicio contra los anteriores, y el primero con
el que es casi igual es el que queda (R003). Así hay un hallazgo por copia, nunca en el que queda, y su identidad —la
regla y el ejercicio que queda— no cambia entre corridas. Es bajo, a la mesa y cuenta como error (R004): la copia tiene
0 y el que queda conserva 1. Si se cumplen las dos condiciones gana el comodín, que no depende del modelo. La evidencia
trae los dos ejercicios como los ve el alumno, el que queda como nodo relacionado y una línea que dice qué cambia o
cuánto se parecen (R005). Sin el modelo o sin examen, el analizador entero queda sin evaluar, comodín incluido
(F-MLOCAL-R003): la mitad de un analizador contaría como si lo hubiera mirado todo. El diagnóstico va en su ranura de
`QuizDiagnoses` (B-02).

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
          - name: NearDuplicateDiagnosis
            _change: add
            type: record
            fields:
              - { name: keptQuizId, type: String }
              - { name: condition, type: NearDuplicateCondition }
              - { name: difference, type: String }
              - { name: similarity, type: double }
              - { name: finishedExercise, type: String }
              - { name: keptFinishedExercise, type: String }
              - { name: score, type: double }
        implementations:
          - name: NearDuplicatesAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: session, type: EvaluationSession }
              - { name: rules, type: NearDuplicateRules }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: config, type: NearDuplicatesConfig }
              - { name: gateDecision, type: LocalToolGateDecision }
```

## El proveedor y su ficha

En `create(policy)` el proveedor pide la decisión a `LocalToolGate`, abre la sesión del libro con su evaluador y arma
las reglas con el comparador del corrector y el renderizador. La ficha: pregunta «¿Dos ejercicios del mismo tema piden
casi lo mismo?», lee el ejercicio terminado y la respuesta de cada ejercicio del tema, reglas `same-but-wildcard`
instantánea y `same-meaning` de modelo local, meta 0 copias por tema, familia errores, evalúa QUIZ, resolución mesa y
costo modelo local, el mayor de sus reglas. Por eso no entra en la corrida base, como decidió José; si se adopta A-09
tal como está, lo metería por su regla instantánea (B-10). Lleva la tarjeta de nomic-embed-text con su examen (B-01) y
no tiene ligadura al plan: qué tarea crea lo decide E (B-18).

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
              - { name: sessionFactory, type: EvaluationSessionFactory }
              - { name: correctorMatcher, type: CorrectorMatcher }
              - { name: studentViewRenderer, type: QuizStudentViewRenderer }
              - { name: localToolGate, type: LocalToolGate }
```

## Cómo se prueba y cómo se mide

Las pruebas no llaman a Ollama: usan un doble de `EmbeddingModel` con los vectores que grabó el prototipo
(`scratchpad/ronda2-B/dup/emb-nomic.json`, sólo los de los casos) y un libro en memoria. Casos reales: «He is certainly
leaving.» / «She is certainly leaving.» (A2, en el 077), por el comodín; «Have you ever been to Rome?» / «Have you ever
visited Rome?» (B1, en el 077), por parecido 0,99; «I observed him clean the room.» / «…cleaning the room.», 0,995 pero
otra respuesta, sin marca; y un par exacto, que queda para `exact-duplicates`. La prueba con nomic-embed-text de verdad
lleva la etiqueta `herramienta-real`. El examen (R007) se rinde con `exam run near-duplicates` y se mide con `medir.py`:
los 6 casi iguales de `escribir/duplicados.json` y los de `examinar/`, la muestra ciega de 40 y 40 pares que revisa una
persona y dos corridas que marcan las mismas copias. En la base del 29/9: 45 pares y 44 copias (A1 23, A2 9, B1 12).

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
