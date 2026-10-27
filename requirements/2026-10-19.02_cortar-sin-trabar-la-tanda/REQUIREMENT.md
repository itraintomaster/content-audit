---
feature:
  id: FEAT-CORTE
  code: F-CORTE
  name: Cortar una medición sin trabar la tanda - se concilia con lo que dejó guardado
  priority: major
---

# Cortar una medición sin trabar la tanda: se concilia con lo que dejó guardado

## TL;DR

**Qué**: Una medición de un juez que se corta (cancelada, forzada o caída) se **concilia**: se cierra con los veredictos
que llegó a guardar, sin consultar, y la tanda queda libre. El tablero lo hace solo cuando ve terminar el proceso. Si
igual queda una abierta, el freno dice cuál es y ofrece «Conciliar» y «Ver la corrida».

**Por qué**: Hoy una medición cortada deja su corrida y la tanda «en curso» para siempre, y eso frena a todos los
jueces. El 19/10 pasó dos veces (Miniteoría «ambas» de madrugada y Naturalidad cancelada a las 21:55) y sólo se
destrabó con un guion que editó a mano el archivo de la tanda, de 300 MB. El freno decía «conciliá los resultados»,
pero no había cómo, y su vínculo llevaba al examen de Distractores.

Fuente: [brief 162](../../docs/briefs/162-cancelar-sin-trabar-la-tanda.html). José eligió el 19/10 las opciones B
(conciliar) y C (el freno nombra la corrida), con conciliación automática y el gasto contado sólo con lo guardado.
El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

El caso es la tanda A1 el 19/10 a la noche.

1. José lanza «Medir lo que falta» de Naturalidad, con tope 1.919 ✓.
2. A las 21:55 la cancela desde Ejecuciones ✓. El proceso muere ✓.
3. ⚠️ La corrida y la tanda siguen «en curso». Ejecuciones la da por cancelada, pero la tanda no se entera.
4. Abre el diálogo para medir otro juez ⚠️. Dice «Hay una operación interrumpida o incierta. Conservá su reserva y
   conciliá los resultados antes de iniciar otra.» No dice cuál, no hay botón, y «Ver su examen» abre el examen de
   Distractores.
5. ⚠️ Nada se puede lanzar hasta que alguien edita el archivo de la tanda a mano.

**Punto de fricción**: pasos 3 a 5. Los 301 veredictos guardados alcanzaban para cerrar bien; faltaba quién lo hiciera.

**Cambio mínimo necesario**: que content-audit sepa conciliar una medición cortada, que el tablero lo pida solo al ver
terminar el proceso, y que el freno nombre la corrida y ofrezca conciliarla.

## Reglas de Negocio

Una **medición** es una corrida de un juez sobre la tanda que sólo mide (analizar, reanalizar, la base o «ambas»); no
cambia el curso. Una medición está **abierta** si quedó «en curso» o «a revisar». **Conciliar** es cerrarla con lo que
dejó guardado. El **registro de veredictos** es donde cada juez guarda cada veredicto apenas lo recibe; la próxima
medición los reutiliza sin consultar. Un **evaluador** es cada consulta que hace un juez por ejercicio (casi todos
tienen uno; el de traducción, dos).

<a id="F-CORTE-R001"></a>
### Rule[F-CORTE-R001] - Una medición cortada se cierra con lo que dejó guardado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Conciliar una medición abierta la cierra así:
> 1. el gasto son los veredictos que guardaron los evaluadores de su juez, con su mismo motor, desde que empezó hasta
>    que terminó, o hasta ahora si nunca terminó;
> 2. lo medido es lo que ya estaba medido al empezar más los ejercicios que midió, sin pasar lo que se proponía
>    alcanzar; lo que falta es la diferencia. Con dos evaluadores, un ejercicio cuenta medido cuando lo midieron los dos;
> 3. queda «hecha» si no falta nada y «parcial» si falta algo, con su hora de cierre;
> 4. su detalle dice que fue conciliada, cuántos veredictos guardó y que una consulta que estaba en vuelo al cortar (a
>    lo sumo una por evaluador) no se cuenta. Si había un motivo de falla, lo conserva;
> 5. su gasto se suma al de la tanda; la reserva queda anotada como estaba;
> 6. conciliar no consulta a ningún modelo ni lee el curso;
> 7. una medición ya cerrada no se toca: pedir conciliarla otra vez responde lo que hay, sin escribir.

<details><summary>Detail</summary>

Las mediciones de la tanda consultan de a una por evaluador. Por eso, al cortar, lo único que queda sin anotar es la
consulta que estaba en vuelo: se pagó o no, pero no dejó veredicto. José decidió el 19/10 no sumarla: con la sesión de
ChatGPT no cuesta plata y la cuenta queda exacta con lo que hay en disco.

**Criterio**, con la tanda A1 el 19/10: la medición de Naturalidad empezó a las 21:19 con tope 1.919 y se canceló a las
21:55. En esa ventana Naturalidad guardó 301 veredictos con ChatGPT medium. Conciliada: gasto 301, 301 medidos más los
que ya estaban, parcial, y la tanda suma 301 a su gasto.

</details>

<a id="F-CORTE-R002"></a>
### Rule[F-CORTE-R002] - Nunca se concilia algo que sigue corriendo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Invariantes:
> 1. si el proceso que corre la medición sigue vivo, conciliar se niega y dice que sigue corriendo;
> 2. una medición que no anotó su proceso (lanzada antes de este cambio) se concilia sólo si quien lo pide confirma que
>    su proceso terminó;
> 3. confirmar que terminó no alcanza si el proceso anotado sigue vivo;
> 4. una corrección cortada (la que cambia ejercicios) no se concilia: sigue pidiendo revisión, como hoy.

<details><summary>Detail</summary>

Una corrección cortada puede haber cambiado ejercicios y tomado tareas: cerrarla con una cuenta no alcanza. Queda para
otra vez.

**Criterio**: mientras la medición de Naturalidad corre, «Conciliar» responde «sigue corriendo» y no escribe nada.

</details>

<a id="F-CORTE-R003"></a>
### Rule[F-CORTE-R003] - «Ambas»: cada parte con lo suyo y la principal suma
**Severity**: major | **Validation**: AUTO_VALIDATED

> Una medición «ambas» mide la corrección y después la base, como dos partes de una principal. Conciliarla:
> 1. concilia cada parte abierta como en [F-CORTE-R001](#F-CORTE-R001); la parte que no llegó a empezar no se crea;
> 2. la principal gasta lo que gastaron sus partes y mide lo que midieron, sin pasar lo que se proponía; lo que falta
>    es la diferencia;
> 3. la principal queda «hecha» sólo si las dos partes existen y terminaron hechas;
> 4. pedir conciliar una parte concilia la medición entera.

<details><summary>Detail</summary>

**Criterio**, con la Miniteoría «ambas» del 19/10 de madrugada (tope 160): la corrección terminó hecha, 83 de 83 con 83
consultas; la base no llegó a empezar. Conciliada: la principal gasta 83, mide 83 de 160, faltan 77, parcial.

</details>

<a id="F-CORTE-R004"></a>
### Rule[F-CORTE-R004] - Al conciliar la última abierta, la tanda queda libre
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Invariantes:
> 1. al conciliar la última medición abierta, la tanda vuelve a quedar como estaba antes de lanzarla, abierta o
>    cerrada ([F-ABIER-R002](../2026-10-27.01_medir-con-la-tanda-abierta/REQUIREMENT.md#F-ABIER-R002));
> 2. si queda otra medición abierta, la tanda no se toca;
> 3. si a la tanda la tiene tomada otra operación (una opinión en curso), no se la libera y el detalle lo dice;
> 4. después de conciliar, la estimación de cualquier juez ya no lleva el freno de esa medición.

<a id="F-CORTE-R005"></a>
### Rule[F-CORTE-R005] - Una medición deja anotado lo necesario para conciliarla
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al empezar, antes de la primera consulta, una medición anota: el proceso que la corre, los evaluadores de su juez,
> el motor con que consulta, cuánto estaba medido y cuánto se propone alcanzar. Invariantes:
> 1. lo anota también cada parte de «ambas»;
> 2. una tanda guardada antes de este cambio se sigue leyendo: sus corridas quedan sin proceso anotado.

<a id="F-CORTE-R006"></a>
### Rule[F-CORTE-R006] - La estimación nombra cada medición que frena
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La estimación de cualquier juez, cuando una medición abierta frena los lanzamientos, nombra cada una con: su id, su
> juez (con su nombre para leer), cuándo empezó, su tope, cuántos veredictos lleva guardados y si sigue corriendo.
> Invariantes:
> 1. el motivo del freno nombra al juez de la medición, no al que se está estimando;
> 2. contar lo guardado no consulta; si la medición no anotó sus evaluadores, lo guardado no se dice.

<a id="F-CORTE-R007"></a>
### Rule[F-CORTE-R007] - El tablero concilia solo cuando ve terminar el proceso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cuando el proceso de una medición lanzada desde el tablero termina sin éxito (cancelada, forzada o caída), el
> tablero pide conciliarla, confirmando que terminó, antes de cerrar su registro en Ejecuciones. Invariantes:
> 1. si terminó bien, no pide nada;
> 2. el registro de Ejecuciones dice lo conciliado: cuántos veredictos guardó y cuántos ejercicios midió;
> 3. si conciliar falla, el registro lo dice y el freno queda con su botón ([F-CORTE-R008](#F-CORTE-R008));
> 4. no relanza nada ni consulta.

<a id="F-CORTE-R008"></a>
### Rule[F-CORTE-R008] - El freno nombra la medición y ofrece conciliarla
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En el diálogo de lanzar, una medición abierta frena con: el nombre de su juez, desde cuándo (hora de Buenos Aires),
> cuántos veredictos guardó y que se reutilizan, el botón «Conciliar» y el vínculo «Ver la corrida», que abre esa
> corrida en Ejecuciones. Invariantes:
> 1. si sigue corriendo, no ofrece conciliar: dice que sigue corriendo y lleva a la corrida;
> 2. nunca lleva al examen de un juez;
> 3. «Conciliar» confirma que el proceso terminó sólo si el tablero no lo ve vivo;
> 4. al terminar de conciliar se vuelve a estimar; si falla, el freno dice por qué.

<a id="F-CORTE-R009"></a>
### Rule[F-CORTE-R009] - La medición que paró sola dice que quedó a medias y por qué
**Severity**: major | **Validation**: AUTO_VALIDATED

> Una medición que terminó sin medir todo lo que se proponía nunca dice «Terminó bien». En Ejecuciones, tanto en el
> historial como en su detalle, dice «Quedó a medias», cuánto midió de cuánto, cuánto quedó sin medir y por qué paró.
> Invariantes:
> 1. el motivo es el que anotó la medición o, en «ambas», cada parte que quedó a medias, sin repetirlo. Si nadie anotó
>    uno y gastó todo su tope, el motivo es el tope; si no, no se dice ninguno y nunca se inventa;
> 2. su resumen nombra al juez que midió; nunca dice «Distractores» por otro juez;
> 3. una medición registrada antes de este cambio que quedó a medias también dice «Quedó a medias», con sus cifras y
>    sin motivo;
> 4. la que midió todo sigue diciendo «Terminó bien», con el nombre de su juez; lo medido por una que quedó a medias se
>    sigue integrando desde Correcciones.

<details><summary>Detail</summary>

**Criterio**, con la tanda A1 el 19/10 a las 22:29: la Miniteoría «ambas» con Gemini se cortó en 5 de 160. Sus dos
partes anotaron «el servicio no respondió a tiempo: volvé a correr más tarde o con menos consultas a la vez» (1 de 83
temas en la corrección, 4 de 83 en la base) y Ejecuciones decía «Terminó bien». Con esta regla dice «Quedó a medias:
midió 5 de 160 y quedaron 155 sin medir.» José creyó haber medido todo y el Mapa seguía con 82 de 83 temas de la
versión anterior.

También quedan a medias, sin motivo anotado, mediciones con un faltante (Calidad didáctica A1, 2.401 de 2.402) o con
las abstenciones que se contaban como sin medir antes del 19/10 (Pragmatismo, 2.314 de 2.402).

</details>

## Contexto

| Pieza | Hoy | Con esto |
|---|---|---|
| Cancelar una medición | La tanda queda trabada | Se concilia sola al morir el proceso |
| Forzar o caída | Igual | Igual que cancelar |
| «Ambas» cortada | La principal y una parte, trabadas | Cada parte con lo suyo, la principal suma |
| El freno | «Hay una operación interrumpida…» y «Ver su examen» | El juez, la hora, lo guardado, «Conciliar» y «Ver la corrida» |
| Destrabar | Un guion sobre 300 MB, a mano | Un botón, si hiciera falta |

## Alcance

- **Adentro**: conciliar mediciones (analizar, reanalizar, la base y «ambas») en content-audit; anotar lo necesario al
  empezar; nombrar las mediciones abiertas en la estimación; la conciliación automática del tablero; el freno nuevo;
  que Ejecuciones diga cuándo una medición quedó a medias y por qué (agregado el 20/10).
- **Afuera**: una corrección cortada (sigue pidiendo revisión); «Completar opiniones», que también deja la tanda en
  curso si se corta (José decidió hacerlo después, aparte); cortar con cuidado esperando la consulta en vuelo.

## User Journeys

### Journey[F-CORTE-J001] - Cancelar una medición y seguir lanzando
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-CORTE-J001
    name: Cancelar una medición y seguir lanzando
    flow:
      - id: corriendo
        action: "José mide un juez desde el tablero y la medición empieza"
        gate: [F-CORTE-R005]
        outcomes:
          - when: "José la cancela, la fuerza o el proceso se cae"
            then: termino
          - when: "Termina bien"
            then: libre
      - id: termino
        action: "El tablero ve terminar el proceso y pide conciliarla"
        gate: [F-CORTE-R007, F-CORTE-R002]
        outcomes:
          - when: "La conciliación cierra la medición"
            then: conciliada
          - when: "La conciliación falla"
            then: freno
      - id: freno
        action: "El diálogo de lanzar nombra la medición abierta y ofrece «Conciliar»"
        gate: [F-CORTE-R006, F-CORTE-R008]
        outcomes:
          - when: "José concilia"
            then: conciliada
      - id: conciliada
        action: "La medición queda parcial con lo guardado y la tanda, libre"
        gate: [F-CORTE-R001, F-CORTE-R003, F-CORTE-R004]
        outcomes:
          - when: "José vuelve a estimar"
            then: libre
      - id: libre
        action: "La estimación ya no frena y reutiliza lo guardado"
        gate: [F-CORTE-R004]
        result: success
```

## Open Questions

No quedan abiertas. José decidió el 19/10 (brief 162): conciliar y nombrar la corrida (B y C); el tablero concilia solo
al ver terminar el proceso; el gasto es sólo lo guardado; «Completar opiniones» va después, aparte.

## Cómo se prueba

Las pruebas de content-audit arman la tanda y el registro de veredictos en memoria. Las del tablero usan respuestas
grabadas. Ninguna consulta a un modelo.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| R001 | Una medición cortada se cierra parcial con los veredictos de su ventana y su motor; los de otro motor o de fuera de la ventana no cuentan; el gasto se suma a la tanda; conciliar otra vez no escribe | `SelectiveCorrectionServiceTest.aCutMeasurementClosesWithTheVerdictsSavedInItsWindowAndMotor`, `.reconcilingAClosedMeasurementAgainAnswersWithoutWriting`, `.withTwoEvaluatorsAnExerciseCountsMeasuredOnlyWhenBothSavedIt` |
| R002 | Un proceso vivo no se concilia; sin proceso anotado hace falta confirmar; una corrección cortada no se concilia | `.aMeasurementWhoseProcessStillRunsIsNeverReconciled`, `.aRunWithoutItsProcessNeedsConfirmationThatItEnded`, `.aCutCorrectionIsNeverReconciled` |
| R003 | «Ambas» cortada en la corrección o con la base sin empezar: la principal suma sus partes | `.aCutBothMeasurementAddsUpItsPartsAndTheOneNeverStartedCountsAsMissing` |
| R004 | Al conciliar la última abierta la tanda queda libre; con una opinión en curso, no | `.reconcilingTheLastOpenMeasurementFreesTheBatch`, `.aBatchHeldByARunningOpinionIsNotFreed` |
| R005 | La reserva anota proceso, evaluadores, motor, punto de partida y alcance; una tanda vieja se sigue leyendo | `.aMeasurementRecordsItsProcessEvaluatorsAndStartBeforeTheFirstQuery`, `FileSystemBatchStoreTest.aBatchSavedBeforeRunsRecordedTheirProcessIsStillRead` |
| R006 | La estimación nombra la medición que frena, con lo guardado y si sigue corriendo | `.theEstimateNamesTheOpenMeasurementThatHoldsLaunches`, `SelectiveCorrectionCmdTest.reconcileRoutesTheConfirmationAndTheAnswerNamesTheOpenMeasurements` |
| R007 | El tablero concilia al ver terminar sin éxito (cancelada o caída), no al terminar bien; si no pudo, el registro lo dice y no relanza nada | ittm `test/tablero_html/ejecuciones/cierre_de_una_medicion_cortada_test.dart` (grupo F-CORTE-R007) |
| R008 | El freno nombra juez, hora, lo guardado, «Conciliar» y «Ver la corrida»; vivo, no ofrece conciliar; nunca lleva a un examen; «Conciliar» confirma que terminó sólo si no la ve viva y vuelve a estimar | ittm `html/test/lanzar-medicion-cortada.test.js`, `test/tablero_html/ejecuciones/cierre_de_una_medicion_cortada_test.dart` (grupos F-CORTE-R008) |
| R009 | La que quedó a medias dice «Quedó a medias», sus cifras y el motivo de sus partes una sola vez; sin motivo anotado no lo inventa, con el tope gastado lo dice; nombra al juez; una vieja también queda a medias; la que midió todo terminó bien; lo medido se integra desde Correcciones | ittm `test/tablero_html/ejecuciones/distractores_test.dart` y `vista_de_ejecuciones_test.dart` (pruebas F-CORTE-R009), `html/test/ejecuciones-vistas.test.js` (F-CORTE-R009) |

## References

- **FEAT-ALDIA**: la versión de hoy de cada juez y su motor ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)).
- **FEAT-TALDIA**: el diálogo de volver a medir y «Medir lo que falta»
  ([F-TALDIA-R004](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R004),
  [F-TALDIA-R009](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R009)).
- **El brief**: [162 · Que cancelar una medición no trabe la tanda](../../docs/briefs/162-cancelar-sin-trabar-la-tanda.html).
