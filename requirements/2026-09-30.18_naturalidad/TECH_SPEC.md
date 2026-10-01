---
patch: FEAT-NATUR
requirement: 2026-09-30.18_naturalidad
generated: 2026-09-30T23:58:00Z
---

# Tech Spec: Naturalidad, qué oraciones mirar primero (FEAT-NATUR)

Paquete B de la ronda 2, rebasado el 30/9 a la noche sobre el `sentinel.yaml` de `ronda1/integracion`. Este parche trae
sólo el analizador `naturalness`. El modelo —`YesNoModel`, con su adaptador de Ollama— y la decisión de publicar son de
FEAT-MLOCAL; la oración terminada que lee es la de FEAT-GRAMA; y el runtime de los jueces (`JudgeRuntime`) y el
evaluador de claude -p son de FEAT-JEXAM: se aplica después de los tres. El método es el que eligió José el 30/9: se le
pregunta a llama3 8B si un nativo diría la oración, y el número es ln(P(no)/P(sí)) del primer token. Sin Ollama, la misma
pregunta va a claude -p (José, 30/9 a la noche), que contesta sí o no. Lo compartido está en `ganchos-B.md`.

## Elegir la ruta y abrir las dos por el runtime de los jueces

En `create(policy)` el proveedor le pide la decisión a `LocalToolGate` —el modelo local disponible y su examen, o la
corrida de examen— y elige: con llama3 disponible, su `NaturalnessEvaluator` con la política sin tope ni pedido que
trae la decisión; sin llama3, la alternativa, el evaluador de claude -p que recibe hecho, con la política de la corrida
tal cual. Las dos rutas abren con `JudgeRuntime.open("naturalness", [evaluador], política)`: el freno del examen de la
versión que se usa, el tope 0 sin pedido sólo para claude -p (porque la ruta local nunca le pasa la política vacía), la
sesión verificada antes de la primera consulta de claude -p y el libro de examen cuando la corrida es el examen. Así B
no abre sesiones por su cuenta ni copia el freno: ya no recibe la fábrica de sesiones.

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
              - { name: alternativeJudge, type: Evaluator }
              - { name: judgeRuntime, type: JudgeRuntime }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: localToolGate, type: LocalToolGate }
```

## Preguntar una vez por oración y guardar la respuesta con su versión

Una consulta por oración (R002, R008). Con llama3, el evaluador es propio del analizador: su contenido es la oración y su
id lleva la versión examinada —el modelo con su huella y el `methodId` de la pregunta—, así que cada versión tiene su
archivo y ninguna reusa lo de otra (F-MLOCAL-R002 inv. 2); su `evaluatorVersion()` es esa misma versión examinada, así
que el runtime frena con la misma clave de examen que `LocalToolGate`. Se guardan las dos probabilidades y no el número,
para no volver a preguntar si la cuenta cambia. Con claude -p, el contenido es el mensaje de sistema y la pregunta ya armada con
la oración, los mismos que recibe llama3 (`NaturalnessConfig.system()` y `question(...)`): «la misma pregunta» queda por
construcción, y cambiar la pregunta cambia la huella y vuelve a consultar. Ese formato queda congelado por la huella: el
gancho B-22 lo cuida. Una consulta que falla deja sin evaluar ese ejercicio y no la corrida.

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

## Del sí y el no a un número, o a una respuesta

El número es el logaritmo del cociente y no la probabilidad del «no», porque ésta se amontona cerca de 1: más de 300
ejercicios por nivel pasan de 0,999 y el borde del 5 % se vuelve un empate. Si llama3 no dijo ni sí ni no —las dos
probabilidades en 0—, la respuesta no sirve y el ejercicio queda sin evaluar. claude -p no da probabilidades: su
veredicto es una palabra, y `nativeWouldSay` la lee; una respuesta que no es ni sí ni no tampoco sirve. Las dos son
cuentas puras sobre lo guardado: misma respuesta, mismo resultado.

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
              - signature: "nativeWouldSay(String payload): Optional<Boolean>"
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
registrar el examen, unos 9 minutos de modelo (B-19); un nivel sin umbral publica sus números y no marca. claude -p no
tiene umbral: marca su «no» (R008). La pregunta y el mensaje de sistema son los de
`scratchpad/ronda2-B/nat/judge_yesno.py`, se ven en `config analyzer naturalness` y son los que lee también claude -p.

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

## Marcar sin contar errores, por cualquiera de las dos rutas

El analizador junta en `onQuiz` las oraciones que alcanzan (R001: sin verbo o de una sola palabra no se pregunta, y eso
se sabe con los tokens de spaCy que el ejercicio ya trae) y en `onCourseComplete` resuelve todo junto con el plan y
`resolveAll` del runtime, como los jueces de C: así un conjunto declarado (`--reevaluate naturalness=…`) y el examen
funcionan sin código propio. Con llama3, un número por encima del umbral del nivel da un hallazgo de `unlikely-sentence`;
con claude -p, un «no» da uno de `native-says-no`; los dos bajos y que sólo ordenan (R003, R008): el puntaje queda en 1,
así que nunca es tarea ni error (F-HALL-R004 inv. 2) y se cuenta como marcado (F-HALL-R008). La evidencia dice qué dijo
el modelo, sin decir por qué (R004). El diagnóstico va en su ranura de `QuizDiagnoses` (B-02) y dice qué ruta contestó;
la cobertura y el costo, en la ranura común de jueces del curso (C-04), y la fila de la herramienta, en la del B-03.

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
              - { name: number, type: Double }
              - { name: threshold, type: Double }
              - { name: marked, type: boolean }
              - { name: reused, type: boolean }
              - { name: alternative, type: boolean }
        implementations:
          - name: NaturalnessAnalyzer
            _change: add
            implements: [ContentAnalyzer]
            requiresInject:
              - { name: opened, type: JudgeSessions }
              - { name: runtime, type: JudgeRuntime }
              - { name: sentenceReader, type: FinishedSentenceReader }
              - { name: scorer, type: NaturalnessScorer }
              - { name: config, type: NaturalnessConfig }
              - { name: gateDecision, type: LocalToolGateDecision }
```

## La lista de cada nivel, lista para un juez

R005 pide que el informe publique, por nivel, los marcados del más dudoso al menos dudoso, y que esa lista sirva tal
cual como conjunto de un juez. Se arma en `onCourseComplete`, cuando están todas las respuestas: con llama3 por número y,
a igual número, en el orden del curso; con claude -p, en el orden del curso, porque no hay número. Escrita un id por
línea es un conjunto válido para `--reevaluate-instruction-quizzes-file` (F-QINST-R019) y para el conjunto por juez de
C (C-17). Va en una ranura del curso (B-03); un `get naturalness-queue <nivel>` que la imprima es opcional (B-16).

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
              - name: level
                type: String
                domainLinks:
                  - { term: Nivel CEFR, role: carries }
              - { name: threshold, type: Double }
              - { name: markedQuizIds, type: "List<String>" }
          - name: NaturalnessQueueDiagnosis
            _change: add
            type: record
            fields:
              - { name: examinedVersion, type: String }
              - { name: levels, type: "List<NaturalnessLevelQueue>" }
              - { name: alternative, type: boolean }
```

## La ficha: dos reglas y dos herramientas

La pregunta «¿Un nativo diría así esta oración?», lee la oración terminada con la primera respuesta, meta «las más
dudosas de cada nivel van primero a los jueces», familia errores (F-HALL-R007), evalúa QUIZ y resolución «sólo ordena».
Dos reglas: `unlikely-sentence`, de modelo local, y `native-says-no`, de modelo pago, la de claude -p. El costo de la ficha
es el mayor de sus reglas, modelo pago (F-HALL-R006 inv. 2): no entra en la corrida base, como decidió José, y acepta
`--budget naturalness=N`. Lleva dos tarjetas, la de llama3 y la de claude -p, cada una con su examen (B-01). Sin
ligadura al plan: nunca produce tareas. El proveedor está en la primera sección.

## Cómo se prueba y cómo se mide

Las pruebas no llaman a Ollama ni a claude: usan un doble de `YesNoModel` con las probabilidades que grabó el prototipo
(`scratchpad/ronda2-B/nat/yn-a1a2.jsonl`), como «She is an engineer of hers.», el más dudoso de A2 (14,68), un
`Evaluator` doble para la alternativa que contesta «yes» o «no» y cuenta cuántas veces lo llaman, y el runtime de C con
libros en memoria. Fijan que marca lo que pasa el umbral y no lo que queda en él, que los empates siguen el orden del
curso, que lo que no tiene verbo no se pregunta, que una segunda corrida no consulta al modelo, que otra versión local no
reusa nada, que con llama3 disponible nunca se llama a la alternativa y que sin tope la alternativa no consulta y deja el
costo declarado. La prueba con llama3 de verdad lleva la etiqueta `herramienta-real`. El examen (R006) se rinde con
`exam run naturalness` y se mide con `medir.py`: con llama3, al menos 28 de los 186 de `escribir/` en la cola del 5 %
(el prototipo trae 51); con claude -p —`CONTENT_AUDIT_OLLAMA_URL=off`—, los 186 más 400 al azar, 586 consultas.

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

## Declarar las 38 pruebas y ubicar el journey

Las 38 son nuevas: no hay pruebas previas de estas reglas que portar. Cada una traza a una regla de FEAT-NATUR (R001 3,
R002 8, R003 8, R004 2, R005 3, R006 3, R007 2, R008 9). Los números son los del prototipo
(`scratchpad/ronda2-B/nat/yn-a1a2.jsonl`), que el doble de `YesNoModel` devuelve tal cual: «They don't like people.»
(`…1f4a`, 9,0752) queda por encima del umbral de A1 y «Are these your feet?» (`…22fb`, 9,0664) no; «She is an engineer
of hers.» (`…2c0d`, 14,68) es el más dudoso de A2. Ninguno de los casos es del examen.

- **Las dos rutas**, en el proveedor: con llama3, su evaluador y la política sin tope de la decisión, nunca claude -p;
  sin llama3, claude -p con la política de la corrida tal cual, que sin `--budget` tiene tope 0. La ficha, sus dos
  tarjetas y que no tiene ligadura al plan.
- **El analizador**: la primera respuesta aceptada (`…2fd3`), lo que no alcanza (`…2305`), la contraída como se lee
  (`…1f4a`), la marca por encima del umbral y no en él, el hallazgo bajo que sólo ordena con puntaje 1, el umbral fijo, el
  nivel sin umbral, las dos evidencias, la lista por nivel y sus empates, el freno del examen y la corrida de examen; y
  con claude -p, la misma pregunta, la marca por su «no», el diagnóstico sin número, la lista en el orden del curso, el
  tope pedido y el «falta el tope» con su costo, sin consultar.
- **Las cuentas y la configuración**: el número como ln(P(no)/P(sí)), 0 en el empate y sin número si no dijo ni sí ni no;
  la lectura del sí o el no de claude -p; los umbrales por versión examinada; la pregunta del prototipo; y la versión
  del evaluador, que es la del examen.
- **Fuera del paquete**: la suma de las formas de «yes» y «no» en `OllamaYesNoModel` (de FEAT-MLOCAL) y la lista de un
  nivel como conjunto declarado de un juez en `DefaultReevaluationQuizSetResolver`.

**El journey** va a `audit-domain`, paquete `naturalness`: el analizador y sus piezas son package-private ahí y los
puertos se simulan, con el runtime de los jueces y libros en memoria, como F-TRSEN-J001. `generate` crearía
`FNaturJ001JourneyTest` con 5 caminos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: naturalness
        _change: add
        implementations:
          - name: NaturalnessAnalyzer
            _change: add
            handwrittenTests:
              - name: "should mark «They don't like people.», 9.0752 above the threshold of A1, 9.07, and not «Are these your feet?», 9.0664"
                traceability: { feature: FEAT-NATUR, rule: F-NATUR-R003 }
              - name: "should mark an exercise to which claude -p answers no with a low finding of native-says-no that only ranks, and leave none when it answers yes"
                traceability: { feature: FEAT-NATUR, rule: F-NATUR-R008 }
features:
  - id: FEAT-NATUR
    code: F-NATUR
    journeys:
      - { id: F-NATUR-J001, testModule: audit-domain, testPackage: com.learney.contentaudit.auditdomain.naturalness }
```
