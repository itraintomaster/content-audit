---
patch: FEAT-QINST
requirement: 2026-07-29.02_validacion-de-consigna-de-quiz
generated: 2026-08-02T00:00:00Z
---

# Tech Spec: la version del evaluador sale de la identidad y pasa a ser procedencia

> Foto del **tercer** patch de FEAT-QINST/FEAT-EVCOST. Las dos fotos anteriores
> siguen en git: la arquitectura original en
> `git show 17449eca:requirements/2026-07-29.02_validacion-de-consigna-de-quiz/TECH_SPEC.md`
> y el patch correctivo de los cuatro huecos en el commit siguiente; ambos patches
> aplicados estan en `.applied-patches/`.

El requerimiento cambio de fondo: la version del evaluador **deja de integrar la
identidad de un resultado** y pasa a ser procedencia registrada
(`DOUBT-IDENTIDAD-VERSION`, Opcion A). El argumento del usuario es que si un
contenido cumple lo que se le pregunta es una propiedad **del contenido**, no de
quien lo juzgo, y cambiar de modelo no puede invalidar 11.487 veredictos pagados.
La contra —que al corregir un juez que juzgaba mal sus veredictos siguen puntuando
sabiendose incorrectos— se planteo y se acepto a conciencia.

Este patch traduce esa decision al contrato. No agrega ninguna capacidad nueva:
mueve un dato de un lugar donde decidia **vigencia** a un lugar donde declara
**origen**, y cambia todo lo que dependia de que estuviera en el primero.

## Sacar la version de la clave y dejar la identidad en dos datos

La identidad de un resultado pasa a ser exactamente `(evaluatorId,
contentFingerprint)`. El `evaluatorId` **se queda** y no contradice la decision:
no identifica quien juzga sino **que pregunta se hizo**, y el veredicto de un
futuro evaluador de calidad de traduccion no sustituye al del juez de consigna
sobre el mismo contenido. Las descripciones de los dos campos sobrevivientes se
reescriben porque eran justamente donde vivia la justificacion vieja.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationKey
        _change: modify
        fields:
          - name: evaluatorId
            type: String
            _change: modify
            description: "Identificador estable del evaluador. No dice quien juzga sino QUE PREGUNTA SE HIZO."
          - name: evaluatorVersion
            _change: delete
          - name: contentFingerprint
            type: String
            _change: modify
            description: "Junto con evaluatorId agota la identidad de un resultado."
```

## Registrar la version como procedencia obligatoria del resultado

Sacada de la clave, la version tiene que quedar en algun lado o se pierde. Va al
propio `EvaluationRecord` como dato **obligatorio**: tras esta decision la
procedencia es la unica señal con la que el usuario decide cuando pedir una
re-evaluacion, y un registro sin ella vuelve indefendible la opcion elegida —nadie
sabria que resultados vienen del evaluador que acaba de corregir—. Por eso
F-EVCOST-R001 le da error propio.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationRecord
        _change: modify
        fields:
          - name: evaluatorVersion
            type: String
            _change: add
            description: "Procedencia OBLIGATORIA: no participa de la identidad y no decide vigencia."
```

## Devolver la procedencia con cada resultado consultado

F-EVCOST-R001 exige que la version se devuelva **con el resultado en cada
consulta**, no solo que quede guardada. Sin esto el consumidor tendria que ir al
registro por su cuenta para saber de donde salio lo que acaba de puntuar, y el
recuento por version de F-QINST-R005 seria imposible de construir. El campo se
puebla exactamente cuando hay resultado; su ausencia en `PENDING` y `FAILED` no es
un dato faltante sino la afirmacion de que no hubo nada que atribuir.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationResolution
        _change: modify
        fields:
          - name: evaluatorVersion
            type: String
            _change: add
            description: "Poblada exactamente cuando kind es REUSED o EVALUATED."
```

## Renombrar `find` a `findLatest` porque la clave dejo de ser unica

Mientras la version estaba en la clave, `find` devolvia a lo sumo una entrada y el
nombre era exacto. Ahora una re-evaluacion explicita produce un segundo resultado
bajo la **misma** clave, y F-EVCOST-R006 fija el desempate: gana el registrado mas
recientemente. Dejar el nombre `find` esconderia esa eleccion en una descripcion;
`findLatest` la pone en la firma y obliga a cualquier implementacion futura a
pasar por ella. `history` deja de recibir dos `String` sueltos y adyacentes —una
conflacion esperando ocurrir, del mismo tipo que ya costo cinco defectos
silenciosos en esta feature— y recibe la clave, que ahora es exactamente esos dos
datos.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: EvaluationLedger
        _change: modify
        exposes:
          - signature: "find(EvaluationKey key): Optional<EvaluationRecord>"
            _change: delete
          - signature: "findLatest(EvaluationKey key): Optional<EvaluationRecord>"
            _change: add
          - signature: "history(String evaluatorId,String contentFingerprint): List<EvaluationRecord>"
            _change: delete
          - signature: "history(EvaluationKey key): List<EvaluationRecord>"
            _change: add
```

## Desempatar por orden de append y no por reloj

El adaptador filesystem es el unico lugar donde el desempate de F-EVCOST-R006 se
vuelve concreto, y la eleccion no es indiferente: se desempata por **posicion en
el archivo**, no por `recordedAt`. El append es monotono y es la misma propiedad
sobre la que ya se apoya la seguridad ante dos corridas simultaneas, mientras que
un reloj puede empatar o retroceder. `recordedAt` queda como dato informativo.

```architecture
modules:
  - name: evaluation-ledger-infrastructure
    _change: modify
    description: |
      El orden de append es la unica fuente del desempate que exige F-EVCOST-R006: findLatest devuelve la ULTIMA escrita y history las devuelve todas, de la mas reciente a la mas antigua.
```

## Hacer que un evaluador sin version sea un evaluador no disponible

La invariante 3 de F-QINST-R010 se sostenia con una **convencion de strings**: una
version que no pudo derivarse llevaba el prefijo `unresolved-agent-definition:` y
por eso no coincidia con ninguna clave registrada. Ese mecanismo dejo de proteger:
con la version fuera de la identidad nadie compara versiones, asi que un String
fabricado se registraria como procedencia legitima, contaminaria los recuentos de
F-QINST-R005 y —como nada se invalida solo— seria permanente. Se reemplaza la
convencion por un tipo: `Optional.empty()` significa "no puedo enunciar mi
version", y la sesion lo trata desde el primer sujeto como evaluador caido.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: Evaluator
        _change: modify
        exposes:
          - signature: "evaluatorVersion(): String"
            _change: delete
          - signature: "evaluatorVersion(): Optional<String>"
            _change: add
```

## Declarar la version consultada solo si la corrida consulto

`evaluatorVersion()` en la sesion afirmaba "la version bajo la que resolvi", que
era cierto porque toda resolucion pasaba por esa version. Ya no: una corrida puede
puntuar miles de veredictos **sin consultar ni una vez**, y F-QINST-R005 exige
declarar exactamente eso. El nuevo nombre dice que se responde y el `Optional`
admite el caso en que la respuesta es "ninguna" —todo reutilizado, sin
presupuesto, o juez que no pudo enunciar su version—.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    interfaces:
      - name: EvaluationSession
        _change: modify
        exposes:
          - signature: "evaluatorVersion(): String"
            _change: delete
          - signature: "consultedEvaluatorVersion(): Optional<String>"
            _change: add
    packages:
      - name: evaluationsession
        _change: modify
        description: |
          Un evaluador que devuelve Optional.empty() en evaluatorVersion() es, desde el primer sujeto, un evaluador NO DISPONIBLE, pero el reuso sigue ocurriendo intacto.
```

## Separar en la cobertura la version consultada del recuento por procedencia

F-QINST-R005 declara ahora **dos** datos de version porque responden preguntas
distintas: con que criterio se juzgo lo nuevo de esta corrida, y de que criterios
viene el puntaje que se esta leyendo. Mezclarlos reintroduce la confusion, y ademas
el primero puede no existir mientras el segundo puntua miles de veredictos.
Declarar solo la version vigente seria hoy una afirmacion falsa sobre la mayoria
del informe; declarar el conjunto de versiones sin recuentos seria honesto pero no
accionable —no distingue un veredicto viejo de ocho mil, que es justo la cifra que
decide si conviene re-evaluar—.

```architecture
modules:
  - name: audit-domain
    _change: modify
    packages:
      - name: quizinstruction
        _change: modify
        models:
          - name: JudgeVersionVerdictCount
            _change: add
            type: record
            fields:
              - name: judgeVersion
                type: String
              - name: verdictCount
                type: int
          - name: QuizInstructionCoverageDiagnosis
            _change: modify
            fields:
              - name: evaluatorVersion
                _change: delete
              - name: consultedJudgeVersion
                type: String
                _change: add
              - name: verdictsByJudgeVersion
                type: List<JudgeVersionVerdictCount>
                _change: add
```

El recuento lo construye el analizador y no la sesion, aunque la sesion ya lleve
todos los demas numeros de cobertura. La razon es que el recuento es sobre "los
veredictos que el informe **puntua**", que es una nocion del consumidor: un
veredicto de infraestructura resuelve la consulta pero no se puntua
(F-QINST-R013), y lo pendiente y lo fallido tampoco (F-QINST-R004). La sesion no
puede saber cual de sus resoluciones termino en un puntaje. Por la misma razon la
segunda suma que exige F-QINST-R005 —recuentos = con veredicto— es una invariante
verificable sobre la contabilidad propia del analizador.

## Retirar `judgeVersionOverride` en vez de dejarlo sin efecto

El campo existia para poder retocar el juez **sin invalidar el cache**. Como ya
nada invalida nada, ese proposito desaparecio por completo. Lo que quedaria de el
es peor que inutil: permitiria estampar a mano una procedencia que no deriva de la
definicion del juez, que es exactamente la enfermedad que la invariante 3 de
F-QINST-R010 declara inaceptable —solo que causada por el usuario en vez de por el
sistema— y que ahora seria permanente. Se retira, con el mismo criterio con el que
se retiro `evaluatorId()` de la factory en el patch anterior: una perilla que no
hace lo que promete es peor que su ausencia.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    models:
      - name: QuizInstructionJudgeConfig
        _change: modify
        fields:
          - name: judgeVersionOverride
            _change: delete
```

## Devolver una version ausente en vez de una version fabricada

El resolutor era el productor del String con prefijo reservado. Con la convencion
retirada, devuelve `Optional.empty()` cuando el locator no encuentra la definicion.
El cambio es el mismo de siempre en este proyecto: representar una ausencia con un
tipo en lugar de con un valor de la misma forma que uno legitimo, para que nadie
pueda confundirlos por descuido.

```architecture
modules:
  - name: quiz-instruction-infrastructure
    _change: modify
    packages:
      - name: instructionjudge
        _change: modify
        interfaces:
          - name: QuizInstructionJudgeVersionResolver
            _change: modify
            exposes:
              - signature: "resolve(QuizInstructionJudgeConfig config): String"
                _change: delete
              - signature: "resolve(QuizInstructionJudgeConfig config): Optional<String>"
                _change: add
```

## Corregir el vocabulario de la cobertura general

`reused` y `pending` describian su significado en terminos de resultado
"**vigente**", que era la palabra correcta cuando la version decidia vigencia. Hoy
no hay vigencia: hay resultado registrado o no lo hay. Dejar la palabra vieja en el
contrato es la forma mas barata de que alguien reimplemente la regla derogada.

```architecture
modules:
  - name: evaluation-ledger-domain
    _change: modify
    models:
      - name: EvaluationCoverage
        _change: modify
        fields:
          - name: reused
            type: int
            _change: modify
            description: "Resueltos con un resultado ya registrado, cualquiera sea la version que lo produjo."
          - name: pending
            type: int
            _change: modify
            description: "Cambiar la version del evaluador nunca deja nada pendiente."
```

## Nota de despliegue: el formato del registro cambia

`EvaluationKey` pierde un campo y `EvaluationRecord` gana otro, asi que las lineas
ya escritas en `.content-audit/evaluations/` dejan de parsear — y F-EVCOST-R004
manda **descartar en silencio** la linea que no se puede interpretar. Es decir: si
existieran resultados registrados, este patch los borraria sin un solo mensaje de
error, que es exactamente lo que la decision del usuario existe para evitar.
Verificado que hoy no existe ese directorio y que la corrida en vivo del juez esta
pendiente, asi que **no hay dato que migrar**. La ventana se cierra apenas se
registre el primer veredicto pagado: si el patch no se aplica antes, hace falta una
lectura compatible que levante la version desde la clave vieja.
