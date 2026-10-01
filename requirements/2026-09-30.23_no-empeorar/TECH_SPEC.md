---
patch: FEAT-NOEMP
requirement: 2026-09-30.23_no-empeorar
generated: 2026-10-01T03:00:00Z
---

# Tech Spec: No empeorar (FEAT-NOEMP)

Paquete E, ronda 2, contra el `sentinel.yaml` con 0.2 y 0.3 aplicados. La compuerta tiene dos niveles con dos entradas
distintas: por cambio, dos mediciones de la misma unidad que arma la mesa; por tanda, la comparación que publica el
paquete F (`BatchComparison` de FEAT-METAS), que E lee y no recalcula. Ninguna de las dos consulta modelos ni escribe
nada: deciden con lo que reciben, y por eso las mismas entradas dan siempre la misma salida (R009). Rebasado por el integrador de la ronda 2 sobre el `sentinel.yaml` de `ronda1/integracion` (la ronda 1 aplicada y construida) y sobre FEAT-RONDA2, que se aplica antes; `sentinel-baseline.yaml` queda como foto del diseño. Lleva la lista entera de patrones de `revision-domain`: ver la última sección.

## Medir una unidad una sola vez, en una forma que se pueda comparar
La compuerta por cambio no puede comparar promedios: «grandmother» no movió ningún número de A1 y sí la cuenta de
ejercicios con palabras marcadas (351 → 352). Compara la unidad —el ejercicio con su tema, porque un duplicado o una
contradicción con la miniteoría aparece en otro ejercicio del tema— por identidad de hallazgo (F-HALL-R014), con lo no
evaluado marcado por nodo para no contarlo (R010). Las palabras rojas vienen aparte y por oración, porque en una opción
múltiple cada opción puesta en el hueco también cuenta (R002) y `lemma-absence` sólo mide la oración guardada. El largo
y lo que dice el audio viajan medidos para que la compuerta los declare sin volver a medir.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: nonregression
        _change: add
        visibility: public
        models:
          - name: UnitMeasurement
            _change: add
            type: record
            fields:
              - { name: level, type: CefrLevel }
              - { name: findings, type: "List<Finding>" }
              - { name: unevaluated, type: "List<UnevaluatedMark>" }
              - { name: redWords, type: "List<RedWord>" }
              - { name: spokenTexts, type: "List<SpokenText>" }
              - { name: lengths, type: "List<LengthDeclaration>" }
          - name: UnevaluatedMark
            _change: add
            type: record
            fields:
              - { name: nodeId, type: String }
              - { name: analyzer, type: String }
          - name: RedWord
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: lemma, type: String }
              - { name: level, type: CefrLevel }
              - { name: outOfCatalog, type: boolean }
              - { name: sentence, type: String }
          - name: SpokenText
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: audioKey, type: String }
              - { name: texts, type: "List<String>" }
          - name: LengthDeclaration
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: tokens, type: int }
              - { name: targetMin, type: int }
              - { name: targetMax, type: int }
              - { name: inRange, type: boolean }
```

## Vetar por cambio sólo lo que José decidió que veta
La A de José del 30/9 fija qué frena un cambio: un error nuevo (R001) o una palabra roja nueva, salvo las que enseña el
tema (R002). El largo se declara y nunca veta (R003, José 11/8), y lo que se escucha cambia sólo con su pedido de
regrabar (R004): un cambio que toca la traducción, la pista o una respuesta alternativa no cambia el texto hablado y no
lo pide. El veredicto lleva un motivo por regla con su evidencia, que es lo que vuelve a quien propone en el intento
siguiente (F-MESA-R007) y lo que la bandeja muestra en un sin ganador.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: nonregression
        _change: add
        visibility: public
        models:
          - name: AudioRerecordRequest
            _change: add
            type: record
            fields:
              - { name: quizId, type: String }
              - { name: audioKey, type: String }
              - { name: newTexts, type: "List<String>" }
          - name: ChangeProbe
            _change: add
            type: record
            fields:
              - { name: changedQuizIds, type: "List<String>" }
              - { name: topicLevelChange, type: boolean }
              - { name: before, type: UnitMeasurement }
              - { name: after, type: UnitMeasurement }
          - name: ChangeGateVerdict
            _change: add
            type: record
            fields:
              - { name: passes, type: boolean }
              - { name: newErrors, type: "List<Finding>" }
              - { name: newRedWords, type: "List<RedWord>" }
              - { name: lengths, type: "List<LengthDeclaration>" }
              - { name: rerecords, type: "List<AudioRerecordRequest>" }
              - { name: notCompared, type: "List<NotComparedCount>" }
              - { name: reasons, type: "List<String>" }
          - name: NotComparedCount
            _change: add
            type: record
            fields:
              - { name: analyzer, type: String }
              - { name: quizzes, type: int }
        interfaces:
          - name: ChangeGate
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "check(ChangeProbe probe): ChangeGateVerdict"
```

## Leer la comparación de F para decidir qué vuelve
Para no tener dos compuertas que digan distinto, la tanda no compara: lee `BatchComparison`, que ya trae por cambio si
vuelve y por qué (`changes`), los pisos rotos con los cambios que más los empeoran (`breaches`, F-METAS-R004) y el
veredicto con la muestra ciega. F ya aplica la reconciliación que pidió José —vuelve por errores y por la palabra de
otro nivel, el largo nunca (F-NOEMP-R003)—, y la compuerta la vuelve a aplicar sobre lo que lee, así un cambio de F no
la saltea. Suma dos cosas que F no ve: los nodos que cambiaron sin ser de la tanda (R007), que calcula la tanda al
componer su curso, y las excepciones que José aprobó en la bandeja, que se listan y no se cuentan (R005, F-BANDEJA-R003).
Para un piso roto devuelve desde el primer culpable hasta que la suma de sus efectos lo haga entrar; el recálculo
siguiente lo confirma (R008).

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: nonregression
        _change: add
        visibility: public
        models:
          - name: ReturnCause
            _change: add
            type: enum
            fields:
              - { name: NEW_ERROR }
              - { name: NEW_RED_WORD }
              - { name: FLOOR }
          - name: ReturnedChange
            _change: add
            type: record
            fields:
              - { name: proposalId, type: String }
              - { name: cause, type: ReturnCause }
              - { name: reason, type: String }
          - name: BatchGateVerdict
            _change: add
            type: record
            fields:
              - { name: passes, type: boolean }
              - { name: verdictKind, type: VerdictKind }
              - { name: returned, type: "List<ReturnedChange>" }
              - { name: declaredLengthFindings, type: "List<Finding>" }
              - { name: changedOutsideScope, type: "List<String>" }
              - { name: listedExceptions, type: "List<String>" }
              - { name: notCompared, type: "List<NotComparedCount>" }
              - { name: reasons, type: "List<String>" }
        interfaces:
          - name: BatchGate
            _change: add
            stereotype: port
            sealed: false
            exposes:
              - signature: "evaluate(BatchComparison comparison, List<String> changedOutsideScope, List<String> exceptionProposalIds): BatchGateVerdict"
```

## Esconder las dos compuertas detrás de una fábrica
Las compuertas no tienen estado ni colaboradores, así que no hay nada que configurar: la fábrica existe sólo para que la
mesa y la tanda, que viven en otros paquetes del mismo módulo, obtengan las implementaciones sin verlas. La raíz de
composición nunca las construye: las arman las fábricas de la mesa y de la tanda, y por eso sus registros de
configuración no llevan compuertas.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: nonregression
        _change: add
        visibility: public
        interfaces:
          - name: GateFactory
            _change: add
            stereotype: factory
            sealed: false
            exposes:
              - signature: "changeGate(): ChangeGate"
              - signature: "batchGate(): BatchGate"
      - name: nonregressionengine
        _change: add
        visibility: internal
        implementations:
          - name: DefaultGateFactory
            _change: add
            visibility: public
            implements: [GateFactory]
          - name: DefaultChangeGate
            _change: add
            implements: [ChangeGate]
          - name: DefaultBatchGate
            _change: add
            implements: [BatchGate]
```

## Registrar FEAT-NOEMP y fijar sus casos sin modelos
La feature se registra para que `feature sync --feature FEAT-NOEMP` anote J001; su ubicación es del qa-tester. Las
pruebas usan mediciones armadas a mano y comparaciones de F armadas a mano: «My grandmother ___ (come) every year.» vuelve
con *grandmother* (A2) y «Winter ___ (come) every year.» pasa (R002); los cuatro arreglos de las verduras piden cuatro
regrabaciones (R004); un ejercicio de B2 que sale del rango por un token se declara y no vuelve (R003); con A1 351 → 352
en la cuenta de palabras marcadas la tanda no pasa (R005). Ninguna consulta un modelo.

```architecture
features:
  - id: FEAT-NOEMP
    code: F-NOEMP
modules:
  - name: revision-domain
    _change: modify
```

## Lo que suma la integración de la ronda 2
`patch apply` reemplaza la lista de patrones de un módulo por la del parche, no la suma: NOEMP, MESA, TANDA y BANDEJA
declaraban cada uno su fábrica en `revision-domain`, y aplicados en orden quedaba sólo la última, sin las dos que el
módulo ya tenía. Este parche lleva la lista entera hasta él (tres), armada contra el modelo que deja el anterior.

```architecture
modules:
  - name: revision-domain
    _change: modify
```

## Declarar las 34 pruebas y ubicar el journey
Las 34 son nuevas y cubren las diez reglas (R001 5, R002 5, R003 3, R004 2, R005 5, R006 4, R007 1, R008 3, R009 4,
R010 2), sobre las dos compuertas: 17 en `DefaultChangeGate` (el grupo A, R009 y R010) y 17 en `DefaultBatchGate` (el
grupo B, R008 a R010). Ninguna consulta un modelo: la compuerta por cambio recibe dos mediciones armadas a mano, y la de la
tanda una `BatchComparison` de F armada a mano. La fábrica no lleva prueba: ninguna regla la pide.

**De dónde sale cada prueba.** Lo medido el 30/9 con las cuatro propuestas del 077 para *Afirmativas regulares*:
«It ___ (come) every year.» → «My grandmother ___ (come) every year.» suma grandmother (A2) y vuelve, y «Winter ___ (come)
every year.» pasa; los cuatro cambian lo que se escucha y piden cuatro regrabaciones; sus largos (6, 5, 4 y 4, dentro de
3 a 8) se declaran; A1 pasa de 351 a 352 ejercicios con palabras rojas y la tanda no pasa; ningún número de vocabulario de
A1 ni del curso (73,923) se mueve. Además: spread y quilt, que ya eran rojas en «She spread the warm blue quilt on the
bed.»; reside, fuera del catálogo; «All the sheep in the field are still ___ (alive / living).» (B2), que con 9 tokens
queda uno abajo del rango de 10 a 25 y no vuelve; la miniteoría de «Contraste inicial» en sus nueve ejercicios, para un
cambio del tema; y «aren't» después de «'re not» en «They ___ (not / be) from Ireland.», que no cambia lo que se escucha.

**Lo que cumplen otras piezas.** La compuerta decide; mover lo que decide es de quien la llama, y se prueba allí con su
regla: que un cambio devuelto gaste un intento y, sin intentos, vaya a la bandeja como sin ganador (R008), en el cierre
de la tanda y la mesa (F-TANDA-R006, que pasa la compuerta de R005 a R008, y F-MESA-R007); que la tanda liste los audios
(R004), en su informe (F-TANDA-R010); y que el curso recalculado deje igual lo que no es de la tanda (R007), en el libro
de cambios (F-TANDA-R006, inv. 1). Acá se prueba que un nodo cambiado fuera de la tanda la frena.

**El journey** va a `revision-domain`, en `com.learney.contentaudit.revisiondomain.batchengine`: recalcular, comparar,
devolver y volver a recalcular es el lazo de `DefaultBatchCloser`, package-private ahí, y la compuerta de la tanda se
obtiene por `DefaultGateFactory`, pública, como la obtiene la tanda. Es el criterio de F-PIPRE-J001: los pasos son del
sistema y no de un comando. `generate` crearía `FNoempJ001JourneyTest` con 3 caminos.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: nonregressionengine
        _change: add
        implementations:
          - name: DefaultChangeGate
            _change: add
            handwrittenTests:
              - name: "should reject «My grandmother ___ (come) every year.» for «It ___ (come) every year.» (A1) because grandmother (A2) is a new red word, naming grandmother and A2 in its reason"
                traceability: { feature: FEAT-NOEMP, rule: F-NOEMP-R002 }
          - name: DefaultBatchGate
            _change: add
            handwrittenTests:
              - name: "should not pass the A1 batch when its comparison shows the exercises of A1 with red words going from 351 to 352, as with «My grandmother ___ (come) every year.»"
                traceability: { feature: FEAT-NOEMP, rule: F-NOEMP-R005 }
features:
  - id: FEAT-NOEMP
    code: F-NOEMP
    journeys:
      - { id: F-NOEMP-J001, testModule: revision-domain, testPackage: com.learney.contentaudit.revisiondomain.batchengine }
```
