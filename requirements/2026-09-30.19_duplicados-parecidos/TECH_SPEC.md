---
patch: FEAT-DUPAR
requirement: 2026-09-30.19_duplicados-parecidos
generated: 2026-10-01T15:30:00Z
---

# Tech Spec: Duplicados parecidos, el paso por pares (FEAT-DUPAR, v3)

La v1 y la v2 de este requirement —el comodín, el parecido de 0,95 que marcaba solo y, sin Ollama, claude -p sobre los
pares de texto parecido— se aplicaron el 1/10 (`.applied-patches/2026-10-01T04-35-17-018Z.yaml`; su TECH_SPEC queda en
la historia de git). Esa versión reprobó su examen: de 40 pares que no marcó, 20 eran el mismo ejercicio. Este parche es
la v3, el paso por pares: el parecido ya no marca, sólo elige qué pares se le preguntan al juez de pares —claude -p con la
guía de `near-duplicates-pair-judge`, el mismo con Ollama y sin él—. No cambian el par candidato ni el comodín
(`NearDuplicateRules`), la pregunta y su veredicto (`SameExerciseQuestion`), el hallazgo en la copia (R003 a R005), ni lo
que comparten otros analizadores: `SentenceEmbeddingEvaluator`, `LocalToolGate`, `JudgeRuntime` y el libro. Tampoco los
constructores del proveedor y del analizador, que construyen las pruebas de audit-application y audit-cli. Se diseñó con
la opción A de las cuatro dudas abiertas.

## Elegir los candidatos con el coseno, nunca marcar con él

El parecido de 0,95 marcaba solo, y los vectores no separan la misma oración dicha de otro modo de otra oración que pide
la misma respuesta; bajar el umbral tampoco alcanza: de 0,85 a 0,90, 4 iguales en 30 pares. Por eso
`candidateSimilarity()`, 0,80, reemplaza a `similarityThreshold()`: el coseno sólo decide qué se le pregunta al juez (R009
inv. 1), y 0,80 está pensado para no perder ninguna copia, aunque la franja de 0,80 a 0,85 tenga 1.028 pares con 1 igual
y 5 dudosos en 70 revisados. `methodId()` sigue nombrando sólo el método de los vectores, «clustering-1», y `config
analyzer near-duplicates` muestra el método, el prefijo y los dos umbrales de candidatos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        interfaces:
          - name: NearDuplicatesConfig
            _change: modify
            sealed: false
            visibility: internal
            exposes:
              - signature: "similarityThreshold(): double"
                _change: delete
              - signature: "candidateSimilarity(): double"
                _change: add
```

## Nombrar cada versión por sus piezas

Cada ruta es una versión que rinde su examen antes de publicar (R007, F-MLOCAL-R006): con Ollama, nomic-embed-text con
su versión y su prefijo, el umbral y el juez (R010); sin Ollama, el parecido de palabras y el juez. En la v2 el método
nombraba sólo el prefijo, y cambiar el 0,95 no hacía otra versión. `vectorRouteMethodId(judgeVersion)` arma
«clustering-1 · candidatos-0,80 · juez-» con los 12 primeros caracteres de la versión del juez, y `LocalToolGate` le
antepone nomic-embed-text con su versión: «nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 ·
juez-1a2b3c4d5e6f». `wordRouteVersion(judgeVersion)` es la versión entera de la ruta sin Ollama, «palabras-0,60 ·
juez-1a2b3c4d5e6f»: cambiar el 0,6, todavía abierto en DOUBT-DUPAR-PARECIDO-TEXTO, también obliga a rendir, y una guía
nueva del juez cambia las dos. Sin versión del juez la pieza es «juez-sin-versión»: `exam run` toma la versión del examen
del juez, que no la tiene, así que esa versión nunca aprueba ni publica.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        interfaces:
          - name: NearDuplicatesConfig
            _change: modify
            sealed: false
            visibility: internal
            exposes:
              - signature: "vectorRouteMethodId(String judgeVersion): String"
                _change: add
              - signature: "wordRouteVersion(String judgeVersion): String"
                _change: add
```

## Enunciar la versión del paso sin cambiar el id

El runtime frena con la versión que enuncia cada evaluador —`open` la compone y busca su examen— y el examen sólo reusa
lo que enunció esa misma versión (F-JEXAM-R002 inv. 2). Con el juez «pelado», la ruta con Ollama quedaría frenada por el
examen del juez solo y no por el del paso entero, su examen reusaría lo que el juez contestó en otra ruta, y `exam run`
registraría el examen del paso con la versión del juez. `PairStepEvaluator` envuelve al juez y, con Ollama, también a los
vectores: consulta al envuelto tal cual y con su mismo `evaluatorId`, y enuncia la versión de la ruta si el envuelto
enuncia la suya. Como el libro identifica por `evaluatorId` y la huella del contenido, no por la versión (F-EVCOST-R001),
en el curso lo que el juez contestó de un par se reusa con Ollama y sin él (R010 inv. 2), y los vectores, guardados con
la versión de los vectores solos, se reusan mientras no cambien el modelo, el prefijo ni el ejercicio, aunque cambien el
umbral o el juez (F-MLOCAL-R002 inv. 2): sirven los de la v2. En un examen, en cambio, cada versión se rinde de verdad: el
de la ruta con Ollama vuelve a preguntar los pares que ya juzgó el de la otra y recalcula los vectores de los temas del
examen, en segundos.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        implementations:
          - name: PairStepEvaluator
            _change: add
            implements: [Evaluator]
            requiresInject:
              - name: evaluator
                type: Evaluator
                description: "El que consulta de verdad: el juez de pares o, con Ollama, también SentenceEmbeddingEvaluator con la versión de los vectores solos. Su evaluatorId es el del envoltorio: el libro identifica por id y huella del contenido, no por versión, así que lo que ya está registrado se reusa con cualquier versión del paso (R010 inv. 2, F-MLOCAL-R002 inv. 2)."
              - name: examinedVersion
                type: String
                description: "La versión examinada de la ruta, la que enuncia: con Ollama, la que LocalToolGate da al paso entero, «nomic-embed-text@0a109f422b47 · clustering-1 · candidatos-0,80 · juez-…»; sin Ollama, wordRouteVersion, «palabras-0,60 · juez-…». Con ella frena el runtime y fija el examen lo que reusa. No enuncia ninguna si es nula o si el envuelto no enuncia la suya."
```

## Abrir con Ollama dos sesiones bajo una sola versión

En `create(policy)` el proveedor lee la versión del juez sin consultarlo y le pide la decisión a `LocalToolGate` con
`vectorRouteMethodId`. Con nomic-embed-text disponible abre por el runtime dos sesiones, las dos bajo la versión examinada
de esa decisión:
- los vectores: `SentenceEmbeddingEvaluator` con la versión de los vectores solos —la que da la tarjeta de `methodId()`,
  la misma clave que en la v2— y la política de la corrida sin tope, que conserva propósito, alcance, conjunto declarado y
  consultas a la vez; sin política, una de curso sin tope;
- el juez, con la política de la corrida tal cual: tope 0 sin `--budget near-duplicates=N`, así que sin pedido nada sale
  de la máquina (F-MLOCAL-R001 inv. 2).

La política local de la decisión no sirve para los vectores: con `--budget` trae ese tope, y el tope es sólo de claude -p
(R009 inv. 3, F-MLOCAL-R008). Al analizador le da un solo `JudgeSessions` —vectores primero, juez último; la versión, el
examen y la política efectiva del juez—: una fila de cobertura y una sola versión para `exam run`. Sin Ollama abre sólo al
juez, envuelto bajo `wordRouteVersion`, con la política de la corrida, y con ese mismo juez envuelto arma la tarjeta de
claude -p. Si el juez no enuncia versión, los envoltorios no reciben ninguna y nada consulta; el motivo a la vista es el
del juez.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        implementations:
          - name: NearDuplicatesAnalyzerProvider
            _change: modify
            requiresInject:
              - name: alternativeJudge
                type: Evaluator
                _change: modify
                description: "El juez de pares de claude -p: AgentJudgeFactory con el agente near-duplicates-pair-judge (artifact sameExerciseVerdict), cableado en el cuerpo de Main. Decide los candidatos de las dos rutas, envuelto en PairStepEvaluator bajo la versión examinada de cada una: con Ollama, la del paso entero; sin Ollama, wordRouteVersion. Su versión se lee sin consultarlo, y su evaluatorId no cambia con la ruta: lo que contestó de un par se reusa con Ollama y sin él (R010 inv. 2)."
              - name: localToolGate
                type: LocalToolGate
                _change: modify
                description: "Con vectorRouteMethodId —el método de los vectores, el umbral y la versión del juez— decide la ruta y da la tarjeta local, así el freno y el examen son los del paso entero (R010). Con methodId solo, su tarjeta da la versión con que los vectores se guardan en el libro, que no cambia con el umbral ni con el juez (F-MLOCAL-R002 inv. 2). La tarjeta de claude -p la arma con el juez envuelto bajo wordRouteVersion. Su política local no sirve para los vectores: con --budget near-duplicates=N trae ese tope."
```

## Sacar same-meaning: tres reglas en la ficha

`same-meaning` sale de la ficha porque cambió lo que hace, de marcar a proponer (R006), y entra `similar-candidates`, de
modelo local, que sólo propone y no da hallazgos (R004). Es la verdad —los vectores eligen qué se le pregunta al juez—, y
la regla de modelo local deja a `near-duplicates` fuera de la corrida base, como decidió José el 30/9, sin tocar
F-RONDA2-R003. Quedan `same-but-wildcard` instantánea y `same-exercise` de modelo pago, que ahora marca con Ollama y sin
él; el costo de la ficha sigue siendo modelo pago. Las dos tarjetas son las dos versiones: nomic-embed-text con la versión
del paso entero y su examen, y claude -p con la de la ruta sin Ollama y el suyo (F-MLOCAL-R002). `SIMILARITY` queda en el
enum sólo para leer los informes de la v2: sin ella no se podrían leer.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        models:
          - name: NearDuplicateCondition
            _change: modify
            type: enum
            fields:
              - name: SIMILARITY
                _change: modify
                description: "Ya no la produce ninguna versión: desde la v3 los vectores sólo proponen candidatos (F-DUPAR-R009). Queda para leer los informes de la versión del 30/9, en la que el parecido de 0,95 marcaba solo con la regla same-meaning, que salió de la ficha."
              - name: JUDGED
                _change: modify
                description: "El juez de pares, claude -p, dice que para el alumno es el mismo ejercicio (F-DUPAR-R002 inv. 2), con Ollama o sin él: el candidato lo propusieron los vectores (R009) o el parecido de palabras (R008). Regla same-exercise, modelo pago."
```

## El analizador pide los vectores, elige los candidatos y pregunta

`onKnowledge` no cambia: arma los pares, el comodín y el parecido de palabras. En `onCourseComplete`, con Ollama, una
primera tanda pide los vectores de los ejercicios de los pares que el comodín no resuelve; con los cosenos elige los
candidatos, de 0,80 o más, y una segunda tanda se los pregunta al juez. Sin Ollama, una sola tanda, la de la v2. Un sí
marca la copia (`JUDGED`, regla `same-exercise`); un no la descarta aunque el parecido llegue a 0,80; un candidato sin
respuesta —sin tope, sesión vencida, servicio caído, veredicto ilegible— o un vector que falta deja el ejercicio a medias
con sus hallazgos del comodín, y la fila de la corrida dice cuántos pares faltan: «falta el tope» vale ahora también con
Ollama. Tres cuidados, porque `JudgeRuntime` no cambia:
- **La cobertura, con el juez solo.** El runtime suma lo pendiente de todas las sesiones y mide el ritmo con los tiempos
  de todos sus evaluadores; con los vectores adentro —unos 4.600 en un minuto— el informe diría que 1.468 pares llevan
  minutos y no 8,3 horas (R009 inv. 4, F-JEXAM-R004). Se le pide con un `JudgeSessions` que sólo trae la sesión del juez,
  y en los veredictos por versión cuentan sólo los del juez, cada uno con la versión con que quedó registrado.
- **Los modos del plan son del juez.** Re-evaluación y conjunto declarado eligen a quién se le vuelve a preguntar; los
  vectores se piden siempre para reusar o calcular, o un conjunto declarado dejaría a medias los temas de afuera.
- **El examen mira sólo su archivo.** En una corrida de examen el archivo es el conjunto declarado de la política
  efectiva: sólo cuentan los pares cuya copia está en él, se piden sólo los vectores de esos pares, y un ejercicio fuera
  del archivo no lleva puntaje ni hallazgo ni cuenta en la cobertura (R007). Con Ollama, rendir son unas 255 consultas
  como mucho, no 978.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        implementations:
          - name: NearDuplicatesAnalyzer
            _change: modify
            requiresInject:
              - name: opened
                type: JudgeSessions
                _change: modify
                description: "Las sesiones que abrió el runtime de los jueces bajo el nombre near-duplicates y la versión examinada de la ruta. Con Ollama, dos y en este orden: los vectores, sin tope aunque la corrida pida --budget near-duplicates=N, y el juez de pares, con la política de la corrida (tope 0 sin pedido); el registro lleva la versión, el examen y la política efectiva del juez. Sin Ollama, una: el juez, con la política de la corrida. El juez es siempre la última sesión, y la cobertura del informe se pide con ella sola: el runtime suma lo pendiente de todas las sesiones y mide el ritmo con todos sus evaluadores, y los vectores falsearían el costo de lo que falta de claude -p (R009 inv. 4). Los modos del plan son del juez: los vectores se piden siempre para reusar o calcular."
              - name: gateDecision
                type: LocalToolGateDecision
                _change: modify
                description: "La decisión de LocalToolGate sobre nomic-embed-text con vectorRouteMethodId: si está, si la versión examinada del paso entero —vectores, umbral y juez (R010)— publica, o por qué no. Sin Ollama sólo dice por qué falta la herramienta; la versión es la del juez envuelto, la de opened."
```

## La evidencia dice con qué parecido llegó el par al juez

R005 pide el parecido que propuso el par y la razón del juez: con Ollama, «Para claude -p es el mismo ejercicio que
…355e: parecido 0,87»; sin Ollama, «… cambian 2 de 6 palabras». `similarity` pasa a ser el coseno con que el par fue
candidato, y `wordSimilarity`, el parecido de palabras que lo propuso sin Ollama. No hace falta un campo para la ruta: un
`JUDGED` con coseno 0 es de la ruta sin Ollama, y cada corrida nombra sus herramientas en su fila.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        models:
          - name: NearDuplicateDiagnosis
            _change: modify
            type: record
            fields:
              - name: similarity
                type: double
                _change: modify
                description: "Con Ollama, el coseno de los vectores con que el par fue candidato del juez (JUDGED), redondeado a dos decimales en la evidencia: «Para claude -p es el mismo ejercicio que …355e: parecido 0,87». 0 si no se calculó: sin Ollama o por el comodín. En los informes de la versión del 30/9, el parecido con que marcó SIMILARITY."
              - name: wordSimilarity
                type: double
                _change: modify
                description: "El parecido de palabras de los dos ejercicios terminados. Sin Ollama es el que hizo al par candidato del juez (JUDGED), y la evidencia lo dice en palabras: «Para claude -p es el mismo ejercicio que …355e: cambian 2 de 6 palabras». Con Ollama la evidencia no lo usa."
```

## Poner al día la descripción del paquete

La descripción de `nearduplicates` decía que el parecido de 0,95 marcaba; ahora cuenta el paso por pares, las dos
versiones con sus piezas y el filtro del examen, que es lo que lee primero quien abre el paquete.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: nearduplicates
        _change: modify
        description: |
          El analizador near-duplicates (FEAT-DUPAR): compara cada ejercicio con los anteriores de su tema, como exact-duplicates. Un par es candidato si pide la misma respuesta comparada como el corrector y no es un duplicado exacto (R001); es casi duplicado si queda igual al tapar pronombres, demostrativos, posesivos y el tiempo del auxiliar, o si el juez de pares —claude -p con la guía de near-duplicates-pair-judge, el mismo con Ollama y sin él— dice que para el alumno es el mismo ejercicio (R002). Ningún parecido marca solo: sólo elige los candidatos del juez. Con Ollama, los vectores de nomic-embed-text desde un coseno de 0,80, que corren siempre y sin tope: --budget near-duplicates=N limita sólo a claude -p (R009); sin Ollama, el parecido de palabras desde 0,6 (R008). Las consultas —un vector por ejercicio y un juicio por par candidato— van por el runtime de los jueces, que las guarda en el libro y las reusa, aplica el freno del examen, el tope y las consultas a la vez, y declara la cobertura y el costo. Cada ruta es una versión examinada que rinde su examen: con Ollama, nomic-embed-text con su versión y su prefijo, el umbral y la versión del juez (R010); sin Ollama, el parecido de palabras y la versión del juez (R007). PairStepEvaluator hace que el juez y los vectores enuncien esa versión sin cambiar su id: el freno es el del paso entero, y el libro reusa lo que el juez ya contestó con Ollama y sin él. En un examen sólo se juzgan los pares cuya copia es un ejercicio del archivo (R007). Familia errores, evalúa el ejercicio, costo de la ficha modelo pago, con una regla de modelo local que sólo propone, similar-candidates: no entra en la corrida base. Sólo los registros y el proveedor son públicos.
```

## Lo que se toca a mano y lo que espera al qa-tester

- `Main` arma el juez con `AgentJudgeSpec("near-duplicates-pair-judge", "sameExerciseVerdict", …)`; el agente viejo,
  `near-duplicates-same-exercise`, queda en disco sin uso.
- Cuerpos después del `generate`: `DefaultNearDuplicatesConfig` (0,80 y las dos versiones; su constructor de prueba pasa
  a recibir el umbral de candidatos), `PairStepEvaluator`, el proveedor (las dos sesiones y la ficha) y el analizador (las
  dos tandas, el filtro del examen y la cobertura con el juez solo).
- Pruebas: este parche no declara ninguna. Dejan de valer las que fijan que el 0,95 marca y su evidencia «Dice lo mismo
  que …», la regla `same-meaning` en la ficha y en el costo de los hallazgos, la ruta con Ollama «nunca con claude -p» y
  el 0,95 de la configuración; `DefaultNearDuplicatesConfigTest` llama a `similarityThreshold()`, que sale, y la ficha de
  prueba de `IAuditEngineTest` todavía dice `same-meaning`. F-DUPAR-J001 cambia el camino 2 y suma el 6 y el 7. Faltan
  R009, R010 y el filtro del examen.
- Exámenes: las dos versiones son nuevas y no tienen examen; hasta que rindan y aprueben, `near-duplicates` no publica en
  el curso. Este parche se aplica antes de que el qa-tester proponga sus nombres, que van en un parche sólo de pruebas.
