---
feature:
  id: FEAT-CORRI
  code: F-CORRI
  name: Las corridas - cada corrida está una vez en la pestaña, por su número, y el registro de actividad es el único que dice si corre
  priority: major
---

# Las corridas: una corrida, una copia, y un solo «corriendo»

## TL;DR

**Qué**: Cada corrida está una sola vez en la pestaña, por su número, y todas las pantallas la leen de ahí: Ejecuciones
(la franja, el historial y el detalle), Vueltas, Problemas y Exámenes dicen lo mismo de la misma corrida. A Ejecuciones
le queda sólo lo que estás mirando: la corrida elegida y el filtro, que van en la dirección. Cancelar, reconocer y
conciliar son pedidos al tablero. En el tablero, el registro de actividad es el único que dice si una corrida corre: un
trabajo se cierra cuando el registro dice que su corrida terminó, aunque no haya dejado su salida.

**Por qué**: Hasta ahora una corrida vivía en cinco copias en la pestaña (lo que corre, el historial y el detalle de
Ejecuciones, las corridas de Vueltas y el trabajo de Problemas y Exámenes), cada una releída a su ritmo: durante unos
segundos decían cosas distintas. Y en el tablero, siete lugares llevaban su propio «corriendo»: al reconocer una
huérfana, el trabajo de su examen esperaba una salida que nunca llega y Exámenes contestaba «Este examen ya está
rindiendo» para siempre.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 3 «Las
corridas». El diagnóstico está en el brief 173 y el diseño aprobado en el brief 174. Las decisiones son de José
(20 y 21/10/2026): el «corriendo» único llega sólo al tablero, no al CLI; los exámenes registrados se ven en Ejecuciones,
aparte de las corridas; conciliar sigue en el diálogo de lanzar; Problemas y Exámenes conservan su reloj de 10 s hasta
las etapas 5 y 8; cada corrida se guarda una vez, por número; «cortada» entra en esta etapa; y la espera propia de 30 s
al cancelar se va. El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José prueba una solución en Problemas; la corrida de su examen arranca ✓.
2. El proceso de la corrida muere: Ejecuciones la muestra huérfana ✓.
3. José la reconoce en Ejecuciones, que pasa a decir «falló» ✓.
4. Problemas sigue diciendo que la corrida corre ⚠️, y la prueba de la solución no avanza.
5. En Exámenes, rendir de nuevo contesta «Este examen ya está rindiendo» ⚠️, para siempre.
6. Cuando una corrida termina bien, Ejecuciones, Problemas y Exámenes lo dicen cada una a su ritmo ⚠️: durante unos
   segundos, una dice «terminó» y otra «corriendo».

**Punto de fricción**: pasos 4 a 6. Cada pantalla y cada lugar del tablero guardan su copia de la corrida y deciden por
su cuenta si corre.

**Cambio mínimo necesario**: que la pestaña guarde cada corrida una sola vez y que el tablero le pregunte al registro de
actividad, que es el único que sabe.

## Reglas de Negocio

Una **corrida** es una ejecución que el tablero lanzó o vio lanzar, con su número. El **registro de actividad** es donde
cada corrida dice si corre, cómo va y cómo terminó. Su **situación** es una de cuatro: **viva** (corre), **huérfana**
(dice que corre pero su proceso ya no está), **terminada** (con su estado y su motivo) o **desconocida** (el registro no
sabe de ella). Un **trabajo** es lo que Problemas y Exámenes lanzan y esperan: el paso de un problema, el examen fijo, la
prueba de una solución. **Lo que mirás** es la corrida elegida en Ejecuciones y el filtro por tanda o por tipo. Un
**examen registrado** es el resultado de un examen anotado sin correr nada en el tablero. Una **lectura** es una
respuesta del tablero a una pregunta de la pestaña.

<a id="F-CORRI-R001"></a>
### Rule[F-CORRI-R001] - Una corrida dice lo mismo en todas las pantallas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Ejecuciones (la franja, el historial y el detalle), Vueltas, Problemas y Exámenes muestran el mismo estado de una
>    corrida: el de la última lectura de la pestaña, sea cual fuere la pantalla que la pidió.
> 2. Cuando termina la corrida de un problema, las pantallas lo dicen con la misma lectura: ninguna sigue diciendo
>    «corriendo». Lo mismo con una huérfana que se reconoce.
> 3. Si dos lecturas dicen cosas distintas de la misma corrida, vale la más nueva según el reloj del tablero; si son del
>    mismo momento, vale la de lo que corre.
> 4. Una lectura que llega tarde nunca pisa a una más nueva.

<a id="F-CORRI-R002"></a>
### Rule[F-CORRI-R002] - Ninguna pantalla guarda corridas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada corrida está una sola vez en la pestaña, por su número. Ninguna pantalla guarda su propia copia de una corrida
>    ni de una lista de corridas.
> 2. A Ejecuciones le quedan sólo lo que mirás y lo de la pantalla: el filtro por estado, la pregunta abierta, el
>    registro abierto y su aviso.
> 3. Problemas y Exámenes guardan el número de la corrida de cada trabajo y leen la corrida de la pestaña.

<a id="F-CORRI-R003"></a>
### Rule[F-CORRI-R003] - Lo que mirás va en la dirección
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. La corrida elegida y el filtro por tanda o por tipo van en la dirección de la página: recargarla o mandar el enlace
>    muestra lo mismo. Lo que no se reconoce se descarta en silencio.
> 2. Atrás y adelante llevan a lo que dice la dirección.
> 3. Se puede mirar una corrida que no está en el historial: más vieja que las últimas 200 o fuera del filtro.

<a id="F-CORRI-R004"></a>
### Rule[F-CORRI-R004] - Se pone al día sola, sin releer de más
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Con cada aviso de «corridas», o a su ritmo si no hay canal, la pestaña relee lo que corre; con eso se ponen al día
>    las filas, la franja y el detalle de las corridas vivas.
> 2. El historial se relee cuando arranca, termina o se reconoce una corrida; un aviso sin esos cambios no lo relee.
> 3. El detalle y las últimas líneas de la corrida elegida se releen con cada lectura de lo que corre mientras corre, y
>    una vez más cuando termina.
> 4. Los exámenes registrados se releen con el aviso de «jueces».
> 5. Lo que ninguna pantalla mira no se relee; al volver a mirarlo, se lee de nuevo.

<details><summary>Detail</summary>

Afina [F-AVISO-R005](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R005) inciso 2: lo que se ve de la lista
de Ejecuciones se sigue poniendo al día con cada aviso de «corridas», pero ya no releyendo la lista entera: las filas de
las corridas vivas se ponen al día con lo que corre, que es la misma corrida.

</details>

<a id="F-CORRI-R005"></a>
### Rule[F-CORRI-R005] - Cancelar, reconocer y conciliar son pedidos al tablero
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cancelar, reconocer o conciliar no cambia por su cuenta lo que muestra la pestaña: se lo pide al tablero y muestra
>    lo que el tablero diga. La pestaña que lo pidió relee enseguida
>    ([F-CORRE-R004](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R004)); las demás, con el aviso.
> 2. Mientras el pedido viaja, su botón lo dice; si falla, se dice con la razón del tablero.
> 3. «Cancelando…» y «Forzar» los dice el tablero: «Forzar» aparece cuando el tablero dice que la corrida sigue viva
>    pasado su plazo. La pantalla no lleva su propio reloj.
> 4. Una cancelación pedida desde otra pestaña se ve igual: «Cancelando…» y después cortada.

<a id="F-CORRI-R006"></a>
### Rule[F-CORRI-R006] - Vueltas lee el mismo historial, filtrado por su tanda
**Severity**: minor | **Validation**: AUTO_VALIDATED

> 1. Las corridas de una vuelta son las del historial filtrado por su tanda. Si Ejecuciones mira el mismo filtro, es la
>    misma lectura y se pide una sola vez.

<a id="F-CORRI-R007"></a>
### Rule[F-CORRI-R007] - Problemas y Exámenes muestran la situación de la corrida
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Un trabajo con su corrida muestra lo que dice la corrida: corriendo, con su cifra (la misma de Ejecuciones);
>    huérfana, con el enlace para reconocerla en Ejecuciones; o terminada, mientras el trabajo trae su resultado.
> 2. Nunca dice «corriendo» si la corrida no corre.
> 3. Si la pestaña todavía no sabe nada de la corrida, muestra lo que dice el trabajo, como antes.
> 4. Cuando la corrida termina, el trabajo se relee enseguida, sin esperar su reloj.

<a id="F-CORRI-R008"></a>
### Rule[F-CORRI-R008] - Los exámenes registrados no son corridas
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Un examen registrado no aparece en lo que corre ni como una corrida que terminó, y registrar uno no relee lo que
>    corre.
> 2. Se ven en el historial de Ejecuciones, marcados como registro de examen y con su detalle, como hasta ahora.
> 3. El enlace «Ver este registro en Ejecuciones» de Exámenes sigue abriendo su detalle.
> 4. Un registro de exámenes roto da un error que lo dice y no rompe lo que corre.

<a id="F-CORRI-R009"></a>
### Rule[F-CORRI-R009] - Un trabajo se cierra cuando el registro dice que su corrida terminó
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El trabajo de un problema, el examen fijo y la prueba de una solución se cierran cuando el registro de actividad dice
>    que su corrida terminó, con su estado y su motivo, aunque la corrida no haya dejado su salida.
> 2. Reconocer una huérfana la da por terminada: su trabajo se cierra como fallido, con el motivo, en 2 s o menos y
>    aunque nadie mire.
> 3. Si la corrida terminó sin dejar su salida, se esperan 30 s por si llega: si llega, se cierra con lo que dice, como
>    siempre.
> 4. Con la corrida viva o huérfana, el trabajo sigue abierto.
> 5. Si el registro no sabe de la corrida, se esperan 30 s y se cierra como un lanzamiento incompleto, como antes.
> 6. Fuera de Ejecuciones, el tablero contesta si una corrida corre de una sola manera: preguntándole al registro.

<details><summary>Detail</summary>

Extiende [F-CORRE-R007](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R007): el trabajo se sigue cerrando al
terminar su corrida aunque nadie mire, y ahora también cuando la corrida terminó sin salida o se reconoció huérfana.
Con salida, el cierre es el de siempre.

</details>

<a id="F-CORRI-R010"></a>
### Rule[F-CORRI-R010] - «Ya está rindiendo» sólo si la corrida está viva
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Preparar el examen fijo se frena con «Este examen ya está rindiendo» sólo si su corrida está viva.
> 2. Si su corrida quedó huérfana, se frena diciendo que hay que reconocerla en Ejecuciones.
> 3. Si su corrida terminó, aunque el trabajo todavía no lo sepa, no se frena.
> 4. El resumen del examen no lo da por rindiendo si su corrida no corre.
> 5. La prueba de una solución que espera una corrida huérfana dice cuál espera; cuando se reconoce, la prueba se pausa
>    con el motivo.

<a id="F-CORRI-R011"></a>
### Rule[F-CORRI-R011] - Lo que el archivo da en curso y ya no corre se muestra cortado
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Una opinión de la tanda o una medición (selectiva o suelta) que su archivo da en curso, pero cuya corrida el
>    registro da por terminada o huérfana, se muestra «cortada», con el motivo, y no «midiendo».
> 2. Al estimar, una cortada no cuenta como corriendo: frena pidiendo conciliarla
>    ([F-CORTE-R008](../2026-10-19.02_cortar-sin-trabar-la-tanda/REQUIREMENT.md#F-CORTE-R008)).
> 3. Si el registro no sabe de esa corrida, se muestra lo que dice el archivo.
> 4. Mostrarla cortada no libera la tanda: sigue pendiente hasta conciliarla.
> 5. Reconocer o cancelar una corrida avisa también «tanda» y «análisis», y la tanda se vuelve a armar entera.

<details><summary>Detail</summary>

Liberar la tanda con una opinión cortada necesita al CLI, que esta etapa no toca: queda pendiente.

</details>

## Journeys

```yaml
journeys:
  - id: F-CORRI-J001
    name: Termina la corrida de un problema
    flow:
      - id: corre
        action: "José mira la corrida de un problema en Problemas, Exámenes y Ejecuciones"
        gate: [F-CORRI-R001, F-CORRI-R007]
        outcomes:
          - when: "La corrida termina"
            then: termina
      - id: termina
        action: "Con la misma lectura, las tres pantallas dicen que terminó"
        gate: [F-CORRI-R001, F-CORRI-R004]
        outcomes:
          - when: "El trabajo todavía no trajo su resultado"
            then: resultado
      - id: resultado
        action: "Problemas y Exámenes releen el trabajo enseguida y muestran su resultado"
        gate: [F-CORRI-R007, F-CORRI-R009]
        result: success
  - id: F-CORRI-J002
    name: Una huérfana reconocida destraba el examen
    flow:
      - id: huerfana
        action: "La corrida del examen fijo queda huérfana"
        gate: [F-CORRI-R007, F-CORRI-R010]
        outcomes:
          - when: "José intenta rendir de nuevo"
            then: frenado
      - id: frenado
        action: "Exámenes dice que hay que reconocerla en Ejecuciones"
        gate: [F-CORRI-R010]
        outcomes:
          - when: "José la reconoce en Ejecuciones"
            then: reconocida
      - id: reconocida
        action: "El tablero cierra el trabajo como fallido, con el motivo, en 2 s o menos"
        gate: [F-CORRI-R005, F-CORRI-R009]
        outcomes:
          - when: "José vuelve a Exámenes"
            then: rendir
      - id: rendir
        action: "Puede rendir de nuevo"
        gate: [F-CORRI-R010]
        result: success
  - id: F-CORRI-J003
    name: Cancelar una corrida desde Ejecuciones
    flow:
      - id: elegir
        action: "José elige la corrida en Ejecuciones y la cancela"
        gate: [F-CORRI-R003, F-CORRI-R005]
        outcomes:
          - when: "El tablero acepta el pedido"
            then: cancelando
          - when: "El tablero lo rechaza"
            then: rechazo
      - id: cancelando
        action: "Todas las pestañas dicen «Cancelando…» y después la ven cortada"
        gate: [F-CORRI-R005, F-CORRI-R001]
        result: success
      - id: rechazo
        action: "El botón dice por qué no se pudo"
        gate: [F-CORRI-R005]
        result: failure
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta a un modelo. Las del navegador están en `html/test/` (node --test, con un servidor de mentira y
el store de verdad); las del tablero, en `test/tablero_html/` (con carpetas de verdad y un CLI falso). Cada prueba
nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| R001 | Con las mismas respuestas, Ejecuciones, Problemas y Exámenes leen el mismo objeto y dicen la misma cifra; terminada o huérfana reconocida, las tres a la vez; la lectura más nueva gana, empate por lo que corre, una tardía no pisa | `html/test/corridas-de-los-trabajos.test.js`, `html/test/estado-corridas.test.js` |
| R002 | Guardia: en todos los estados de los recorridos de Ejecuciones, Vueltas, Problemas y Exámenes cada corrida está una vez, en la tabla; Ejecuciones sólo tiene `ui`; Vueltas no guarda corridas; sólo `js/estado` nombra las rutas de una corrida y escribe la tabla; «__proto__» se guarda como cualquier número | `html/test/corridas-de-los-trabajos.test.js`, `html/test/ejecuciones-estado.test.js`, `html/test/vueltas-operaciones.test.js`, `html/test/estado-corridas.test.js`, `html/test/arquitectura.test.js` |
| R003 | La dirección lleva la corrida y el filtro, ida y vuelta; lo que no se reconoce se descarta; atrás y adelante; una corrida fuera del historial se pide igual | `html/test/ejecuciones-direccion.test.js`, `html/test/estado-corridas.test.js`, `html/test/ejecuciones-operaciones.test.js` |
| R004 | Contando pedidos: arrancar o terminar relee el historial una vez; el detalle de la elegida viva se relee con cada lectura y una vez más al terminar; «jueces» relee los registrados; lo que nadie mira no se pide, y al volver a mirarlo sí; el detalle sin el log (`?lineas=0`) y el log aparte | `html/test/ejecuciones-operaciones.test.js`, `html/test/estado-avisos.test.js`, `test/tablero_html/ejecuciones/registro_y_registros_test.dart` |
| R005 | Cancelar, reconocer y conciliar son un pedido cada uno y releen lo que corre; el botón dice que pide y la razón si falla; «Forzar» sólo cuando el tablero lo dice; cancelada en otra pestaña se ve igual | `html/test/ejecuciones-operaciones.test.js`, `html/test/ejecuciones-selectores.test.js`, `html/test/ejecuciones-ayuda.test.js`, `html/test/lanzar-medicion-cortada.test.js` |
| R006 | Vueltas y Ejecuciones con la misma tanda hacen un solo pedido; releer la tanda relee su historial | `html/test/vueltas-operaciones.test.js` |
| R007 | Viva con la cifra de Ejecuciones, huérfana con el enlace, terminada mientras trae el resultado, en Probar, en los pasos y en la rendición; nunca «En ejecución» si no corre; sin lectura, lo del trabajo o lo que dijo el servidor; al terminar se relee ya y a los 2 s | `html/test/corridas-de-los-trabajos.test.js` |
| R008 | Los registros no están en lo que corre ni en las terminadas; su lista y su detalle; el enlace de Exámenes; registrar no relee lo que corre; un registro roto o que no se deja leer da 502 y no rompe lo que corre | `test/tablero_html/ejecuciones/registro_y_registros_test.dart`, `test/tablero_html/novedades_test.dart`, `html/test/ejecuciones-operaciones.test.js`, `html/test/examenes-operaciones.test.js`, `html/test/examenes-vistas.test.js` |
| R009 | Viva y huérfana dejan el trabajo abierto; reconocer lo cierra con el vigilante de verdad; terminada sin salida espera y cierra con el registro, en una sola escritura con el error; la salida que llega gana; nadie fuera de Ejecuciones interpreta el registro por su cuenta | `test/tablero_html/ejecuciones/situacion_de_la_corrida_test.dart`, `test/tablero_html/problemas/cierre_por_el_registro_test.dart`, `test/tablero_html/ejecuciones/aviso_del_vigilante_test.dart` |
| R010 | Preparar con la corrida viva, huérfana y terminada; el resumen dice la corrida como está ahora aunque la vista esté guardada; Probar con una huérfana y después de reconocerla | `test/tablero_html/examenes/examen_fijo_test.dart`, `test/tablero_html/problemas/pasos_de_problemas_test.dart` |
| R011 | Opinión y medición en curso con la corrida terminada o huérfana salen cortadas; viva sigue; sin dato, el archivo; al estimar no corre; reconocer y cancelar avisan tanda y análisis | `html/test/corrida-cortada.test.js`, `test/tablero_html/cortadas_de_la_tanda_test.dart`, `test/tablero_html/opinion_en_contexto_test.dart`, `test/tablero_html/analizadores/selectiva_cortada_test.dart`, `test/tablero_html/ejecuciones/estimacion_para_lanzar_test.dart`, `test/tablero_html/novedades_test.dart` |

Además, en una copia de verificación de la campaña (sin modelos, el 21/10): una rendición del examen fijo con su
corrida simulada se vio igual en la franja, Ejecuciones y Exámenes mientras corría; cancelada, quedó huérfana porque
nadie cerró el registro, y al reconocerla el vigilante la marcó cancelada, cerró el trabajo y Exámenes volvió a ofrecer
rendir; otra que terminó sola pasó a «Terminó de rendir» con la misma lectura que la franja, y su resultado llegó a los
7 s. Detalle en el brief 175.

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 3.
- **Briefs**: 173 (diagnóstico y preguntas), 174 (diseño aprobado) y 175 (lo hecho).
- **FEAT-CORRE**: lo que corre en un solo lugar. [F-CORRE-R004](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R004)
  (releer enseguida después de una acción) sigue valiendo; [F-CORRI-R009](#F-CORRI-R009) extiende
  [F-CORRE-R007](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R007).
- **FEAT-AVISO**: [F-CORRI-R004](#F-CORRI-R004) afina
  [F-AVISO-R005](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R005).
- **FEAT-CORTE**: la cortada frena pidiendo conciliar
  ([F-CORTE-R008](../2026-10-19.02_cortar-sin-trabar-la-tanda/REQUIREMENT.md#F-CORTE-R008)).
