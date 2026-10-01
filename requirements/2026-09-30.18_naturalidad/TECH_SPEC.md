---
patch: FEAT-NATUR
requirement: 2026-09-30.18_naturalidad
generated: 2026-09-30T21:50:00Z
---

# Tech Spec: Naturalidad, qué oraciones mirar primero (FEAT-NATUR)

Paquete B de la ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. Este parche trae sólo el analizador
`naturalness`. El modelo —`YesNoModel`, con su adaptador de Ollama— y la decisión de publicar son de FEAT-MLOCAL, y la
oración terminada que lee es la de FEAT-GRAMA: se aplica después de los dos. El método es el que eligió José el 30/9: se
le pregunta a llama3 8B si un nativo diría la oración, y el número es ln(P(no)/P(sí)) del primer token; SLOR quedó
descartado. Lo compartido está en `ganchos-B.md`.

## Preguntar una vez por oración y guardar el número con su versión

Una consulta de un token por oración, local y gratis (R002). La respuesta se guarda en el libro de evaluaciones de
FEAT-EVCOST: el evaluador es propio del analizador, su contenido es la oración y su id lleva la versión examinada —el
modelo con su huella y el `methodId` de la pregunta—, así que cada versión tiene su archivo y ninguna reusa lo de otra,
tampoco si cambia la pregunta (F-MLOCAL-R002 inv. 2). Se guardan las dos probabilidades y no el número: si la cuenta
cambia, no hay que volver a preguntar. La sesión se abre sin tope salvo `--budget naturalness=N` (B-06), y una consulta
que falla deja sin evaluar ese ejercicio y no la corrida, porque 0.3 aísla las fallas de los analizadores de modelo.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        implementations:
          - name: NaturalnessEvaluator
            _change: add
            implements: [Evaluator]
            requiresInject:
              - { name: yesNoModel, type: YesNoModel }
              - { name: config, type: NaturalnessConfig }
              - { name: examinedVersion, type: String }
```

## Del sí y el no a un número

El número es el logaritmo del cociente y no la probabilidad del «no», porque ésta se amontona cerca de 1: más de 300
ejercicios por nivel pasan de 0,999 y el borde del 5 % se vuelve un empate. Si el modelo no dijo ni sí ni no —las dos
probabilidades en 0—, la respuesta no sirve y el ejercicio queda sin evaluar. Es una cuenta pura sobre lo guardado:
misma respuesta, mismo número.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        interfaces:
          - name: NaturalnessScorer
            _change: add
            visibility: internal
            sealed: false
            exposes:
              - signature: "number(String payload): Optional<Double>"
        implementations:
          - name: DefaultNaturalnessScorer
            _change: add
            implements: [NaturalnessScorer]
```

## El umbral fijo de cada nivel, atado a la versión que lo midió

José eligió un umbral fijo por nivel, el que marcaba el 5 % en la base del 29/9, para que arreglar una oración rara no
haga marcar otra que no cambió. Un umbral sólo vale para el modelo y la pregunta que dieron los números, así que la
configuración los guarda por versión examinada: para «llama3:8b@365c0bd3c000» con la pregunta del prototipo, 9,07 en A1
(116 de 2.334) y 10,2 en A2 (120 de 2.403). Los de B1 y B2 se calculan con la misma regla sobre la base antes de
registrar el examen, unos 9 minutos de modelo (B-19); un nivel sin umbral publica sus números y no marca. La pregunta y
el mensaje de sistema son los de `scratchpad/ronda2-B/nat/judge_yesno.py`, y se ven en `config analyzer naturalness`.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        interfaces:
          - name: NaturalnessConfig
            _change: add
            visibility: internal
            sealed: false
            extends: [SelfDescribingConfig]
            exposes:
              - signature: "methodId(): String"
              - signature: "system(): String"
              - signature: "question(String sentence): String"
              - signature: "thresholdFor(String toolVersion,String level): Optional<Double>"
        implementations:
          - name: DefaultNaturalnessConfig
            _change: add
            implements: [NaturalnessConfig]
```

## Marcar sin contar errores

Se lee la oración terminada con la primera respuesta aceptada (R001); sin verbo o de una sola palabra no alcanza, y eso
se sabe con los tokens de spaCy que el ejercicio ya trae. Un número por encima del umbral de su nivel da un hallazgo
bajo que sólo ordena (R003): el puntaje queda en 1, así que nunca es tarea ni error (F-HALL-R004 inv. 2) y se cuenta
como marcado (F-HALL-R008). La evidencia dice cuánto duda el modelo y contra qué umbral, sin decir por qué (R004): eso lo
decide el juez de traducción y sentido. El diagnóstico va en su ranura de `QuizDiagnoses` (B-02).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        models:
          - name: NaturalnessDiagnosis
            _change: add
            type: record
            fields:
              - { name: sentence, type: String }
              - { name: level, type: String }
              - { name: number, type: double }
              - { name: threshold, type: Double }
              - { name: marked, type: boolean }
              - { name: reused, type: boolean }
        implementations:
          - name: NaturalnessAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: session, type: EvaluationSession }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: scorer, type: NaturalnessScorer }
              - { name: config, type: NaturalnessConfig }
              - { name: gateDecision, type: LocalToolGateDecision }
```

## La lista de cada nivel, lista para un juez

R005 pide que el informe publique, por nivel, los marcados del más dudoso al menos dudoso, y que esa lista sirva tal
cual como conjunto de un juez. Se arma en `onCourseComplete`, cuando están todos los números, con el orden del curso
para los empates. Escrita un id por línea es un conjunto válido para `--reevaluate-instruction-quizzes-file`, que acepta
ids separados por saltos de línea (F-QINST-R019), y para el conjunto por juez de C (C-17). Va en una ranura del curso
(B-03); un `get naturalness-queue <nivel>` que la imprima es opcional (B-16).

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        models:
          - name: NaturalnessLevelQueue
            _change: add
            type: record
            fields:
              - { name: level, type: String }
              - { name: threshold, type: Double }
              - { name: markedQuizIds, type: "List<String>" }
          - name: NaturalnessQueueDiagnosis
            _change: add
            type: record
            fields:
              - { name: examinedVersion, type: String }
              - { name: levels, type: "List<NaturalnessLevelQueue>" }
```

## El proveedor y su ficha

En `create(policy)` el proveedor pide la decisión a `LocalToolGate` —modelo disponible y examen aprobado de la versión
examinada, o corrida de examen—, abre la sesión del libro con su evaluador y le pasa las dos cosas al analizador. La
ficha: pregunta «¿Un nativo diría así esta oración?», lee la oración terminada con la primera respuesta, regla
`unlikely-sentence` de modelo local, meta «el 5 % más dudoso de cada nivel va primero a los jueces», familia errores
(F-HALL-R007), evalúa QUIZ, resolución «sólo ordena», costo modelo local —por eso no entra en la corrida base, como
decidió José— y la tarjeta de llama3 con su examen (R007, B-01). Sin ligadura al plan: nunca produce tareas.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        visibility: public
        implementations:
          - name: NaturalnessAnalyzerProvider
            _change: add
            visibility: public
            implements: [AnalyzerProvider]
            requiresInject:
              - { name: yesNoModel, type: YesNoModel }
              - { name: sessionFactory, type: EvaluationSessionFactory }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: localToolGate, type: LocalToolGate }
```

## Cómo se prueba y cómo se mide

Las pruebas no llaman a Ollama: usan un doble de `YesNoModel` con las probabilidades que grabó el prototipo
(`scratchpad/ronda2-B/nat/yn-a1a2.jsonl`), como «She is an engineer of hers.», el más dudoso de A2 (14,68), y un libro
en memoria. Fijan que marca lo que pasa el umbral y no lo que queda en él, que los empates siguen el orden del curso,
que lo que no tiene verbo no alcanza, que una segunda corrida no consulta al modelo y que otra versión no reusa nada.
La prueba con llama3 de verdad lleva la etiqueta `herramienta-real`. El examen (R006) se rinde con `exam run
naturalness` sobre A1–A2 y se mide con `medir.py`: al menos 28 de los 186 antinaturales y sin sentido de `escribir/` en
la cola del 5 %; el prototipo trae 51. Costo: A1–A2 en unos 6 minutos y el curso en unos 15 la primera vez; después,
sólo lo que cambió.

## Registrar FEAT-NATUR

El parche registra la feature con su código para que `feature sync --feature FEAT-NATUR` anote J001 después de aplicar;
la ubicación del journey es del qa-tester.

```architecture
modules:
  - name: audit-domain
    _change: modify
features:
  - id: FEAT-NATUR
    code: F-NATUR
```
