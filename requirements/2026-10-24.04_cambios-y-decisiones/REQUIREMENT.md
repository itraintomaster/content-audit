---
feature:
  id: FEAT-DECIDE
  code: F-DECIDE
  name: Cambios y decisiones - lo que propone la tanda y lo que se decidió, leídos aparte y una vez por pestaña, con un aviso por cada decisión
  priority: major
---

# Cambios y decisiones: lo propuesto y lo decidido, aparte

## TL;DR

**Qué**: El tablero lee aparte lo que propone la tanda (los **cambios**) y lo que se decidió de cada corrección (las
**decisiones**), una vez por pestaña. Decidir cambia sólo las decisiones: se ve al instante, avisa «decisiones» y las
otras pestañas lo muestran en menos de un segundo, sin volver a bajar los cambios. La cola de guardado avisa cada paso:
nadie le pregunta cada 2 s. Incorporar una miniteoría y sugerir o restaurar una conversión avisan «cambios». Publicar
vuelve a contar con cada decisión. Correcciones se queda con lo suyo: el modo, los filtros, deshacer y sus formularios.

**Por qué**: Correcciones guardaba su propia copia de la tanda, con lo decidido adentro: 43 MB por pestaña. Decidir
avisaba «tanda» a todas, y la cola de guardado se preguntaba cada 2 s mientras guardaba algo. Una decisión tomada desde
la terminal o desde la app de Flutter no llegaba a ninguna pestaña hasta releer a mano, y Publicar no contaba lo
aprobado hasta que alguien lo volviera a leer.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 7
«Cambios y decisiones». Las decisiones son de José (24/10/2026):
- el alcance es «Cambios y decisiones»: lo que la propuesta pone en esta etapa, nada más;
- si el servidor acepta una decisión pero content-audit no la puede guardar, queda en la cola como hoy («sin
  guardar», Reintentar, Descartar);
- las decisiones tomadas desde la terminal o desde la app de Flutter también llegan solas;
- el segundo de [R004](#F-DECIDE-R004) se cuenta desde que content-audit guarda la decisión, no desde el clic: la
  pestaña que decide la muestra al instante, y guardar tarda unos 6 s. Bajar esos 6 s va aparte (brief 208, decidido
  después de hacerlo, el 25/10, en el brief 213).

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José abre Correcciones en dos pestañas ✓: cada una baja la tanda entera, 43 MB, con lo decidido adentro ⚠️.
2. Aprueba una corrección en la primera ✓. La segunda no se entera ⚠️: el aviso es «tanda», y la tanda no cambió.
3. Mientras la decisión se guarda, cada pestaña le pregunta a la cola cada 2 s ⚠️.
4. Aprueba otra desde la terminal ⚠️: ninguna pestaña la muestra hasta que José relee a mano.
5. Mira Publicar ⚠️: no cuenta lo aprobado hasta que la relee.

**Punto de fricción**: pasos 1 a 5. Lo decidido viaja dentro de la tanda, y decidir no avisa a nadie.

**Cambio mínimo necesario**: leer aparte lo propuesto y lo decidido, y que decidir avise «decisiones».

## Reglas de Negocio

Los **cambios** de una tanda son lo que propone: sus temas y ejercicios, cada corrección con su antes y su después y por
qué se propuso. Lo que opinan los jueces viaja aparte, en las opiniones de la etapa 6
([FEAT-OPINA](../2026-10-24.03_analisis-y-opiniones/REQUIREMENT.md)), y Correcciones lo junta al dibujar. Las **decisiones** son lo decidido: de cada corrección, si está aprobada,
rechazada o sin revisar, quién y cuándo; la **cola de guardado**, con lo que todavía no llegó a content-audit; y las
aprobaciones de los ejercicios sin cambios. La **edición** de los cambios cambia cuando cambia lo propuesto
(incorporar una miniteoría, una conversión, volver a corregir); decidir no la cambia.

<a id="F-DECIDE-R001"></a>
### Rule[F-DECIDE-R001] - Los cambios se leen una vez por pestaña
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada pestaña que mira Correcciones lee los cambios de la tanda que mira una vez. Las que miran otra pantalla no
>    los leen.
> 2. Los cambios no traen lo decidido.
> 3. Correcciones no guarda su copia de los cambios.
> 4. Si la pestaña pasa a mirar otra tanda, sigue mostrando la de antes hasta que llegan los cambios de la elegida. Una
>    lectura de la tanda de antes que llega tarde nunca queda en lugar de la elegida.

<a id="F-DECIDE-R002"></a>
### Rule[F-DECIDE-R002] - Las decisiones se leen una vez por pestaña, aparte
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada pestaña que mira Correcciones lee las decisiones de la tanda que mira, aparte de los cambios: lo guardado de
>    cada corrección, la cola de guardado y las aprobaciones de los ejercicios sin cambios.
> 2. Ninguna pantalla guarda una decisión ni una copia de los cambios.
> 3. Una respuesta de las decisiones más vieja que la que ya se tiene no pisa nada.
> 4. Si los cambios y las decisiones dicen ediciones distintas, la pestaña vuelve a leer una vez el que quedó atrás y
>    queda con los de la misma edición. Mientras corre una medición, que arma otra edición con cada lectura de los
>    cambios, tampoco entra en un ir y venir de lecturas.

<a id="F-DECIDE-R003"></a>
### Rule[F-DECIDE-R003] - Decidir se ve al instante, y si el servidor dice que no, vuelve
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Aprobar o rechazar se ve en la pestaña apenas se decide, antes de que conteste el servidor.
> 2. Si el servidor la rechaza, o no contesta, la corrección vuelve a lo que estaba (por ejemplo, «sin revisar») y la
>    pantalla dice por qué.
> 3. Si el servidor la aceptó pero content-audit no la pudo guardar, queda en la cola como hasta ahora: «sin guardar»,
>    con Reintentar y Descartar.
> 4. Cuando el servidor la acepta, la pestaña pasa de lo que mandó a lo guardado sin mostrar «sin revisar» en el medio.

<a id="F-DECIDE-R004"></a>
### Rule[F-DECIDE-R004] - Decidir avisa «decisiones»
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Aprobar o rechazar una corrección, y aprobar o reabrir un ejercicio sin cambios, avisan «decisiones», no «tanda»
>    ni «cambios».
> 2. Cada pestaña que mira esa tanda muestra la decisión en un segundo o menos desde que content-audit la guarda. Lo
>    que tarda guardar no cuenta en ese segundo: la pestaña que decide ya la muestra al instante
>    ([R003](#F-DECIDE-R003)), y las otras no muestran lo que content-audit todavía puede rechazar.
> 3. Ninguna pestaña vuelve a leer los cambios por una decisión.

<a id="F-DECIDE-R005"></a>
### Rule[F-DECIDE-R005] - La cola de guardado avisa su avance
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Cada paso de la cola de guardado avisa «decisiones»: encolar, empezar a guardar, guardar, fallar, reintentar y
>    descartar.
> 2. La pantalla muestra cada paso con el aviso. Ninguna pantalla le pregunta a la cola cada 2 s.
> 3. Con el canal de avisos, las decisiones se controlan una vez por minuto. Sin el canal, cada 3 s mientras la cola
>    guarda algo y cada 30 s si no, siempre con la pestaña a la vista. Lo que espera detrás de una decisión que no se
>    guardó no cuenta como «guarda algo»: no se mueve solo.

<a id="F-DECIDE-R006"></a>
### Rule[F-DECIDE-R006] - Publicar cuenta lo decidido
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Con cada aviso de «decisiones», Publicar vuelve a contar lo que viaja, sin que nadie lo relea.
> 2. Cuenta la decisión en un segundo o menos desde que content-audit la guardó.
>
>    *Medido el 24/10 con el CLI de verdad:* guardar una decisión tarda unos 6 s en content-audit; Publicar la cuenta
>    después de eso, no apenas se aprueba.

<a id="F-DECIDE-R007"></a>
### Rule[F-DECIDE-R007] - Lo que cambia lo propuesto avisa «cambios»
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Incorporar una miniteoría y sugerir o restaurar una conversión avisan «cambios» y «decisiones». Guardar un
>    borrador de miniteoría o dar una opinión sobre ella no avisan nada nuevo.
> 2. Con el aviso, las pestañas que miran esa tanda vuelven a leer sus cambios. Las que miran otra preguntan si su
>    edición cambió y, si no, no bajan nada.

<a id="F-DECIDE-R008"></a>
### Rule[F-DECIDE-R008] - Decidir no vuelve a armar los cambios
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. El servidor arma los cambios una vez por edición. Una decisión no los vuelve a armar.
> 2. Una pestaña que ya tiene esa edición no la vuelve a bajar: el servidor le contesta que es igual, y la pantalla
>    sigue con lo que tenía.

<a id="F-DECIDE-R009"></a>
### Rule[F-DECIDE-R009] - Lo decidido desde afuera llega solo
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Una decisión tomada desde la terminal o desde la app de Flutter avisa «decisiones» en un segundo o menos, y las
>    pestañas la muestran sin releer a mano.
> 2. El tablero vuelve a leer sólo lo decidido: no vuelve a leer la tanda entera.
> 3. Mientras corre una medición, que cambia la tanda dos veces por segundo, leer las decisiones no vuelve a leer la
>    tanda si lo decidido no cambió.

<a id="F-DECIDE-R010"></a>
### Rule[F-DECIDE-R010] - Aprobar escribe por el mismo camino
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Aprobar y rechazar siguen escribiendo como hasta ahora: el tablero manda la decisión con la tanda, su versión y
>    su corrección, la pone en la cola con su diario y content-audit la guarda.
> 2. Las mismas verificaciones de antes: una decisión sobre una tanda que cambió, publicada o de otra corrección no se
>    guarda.

<a id="F-DECIDE-R011"></a>
### Rule[F-DECIDE-R011] - A Correcciones le queda lo suyo
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Correcciones conserva el modo, los filtros, deshacer y sus formularios: la muestra ciega, la miniteoría, la
>    conversión, volver a corregir, el problema y la evolución de los problemas.
> 2. Deshacer sigue funcionando como antes: vuelve la corrección a lo que estaba y lo guarda.

## Lo que queda afuera

- **El Mapa** no vuelve a contar con cada decisión: su estado pesa unos 28 MB y releerlo por cada aprobación cuesta más
  de lo que da. Sigue contándolas cuando se relee, como hasta ahora.
- **Vueltas** cuenta las rechazadas cuando se relee, como hasta ahora.
- **Lo que opinan los jueces y lo que falta medir** son de la etapa 6 (las opiniones y el análisis): esta etapa no los
  toca. Las opiniones se leen aparte de los cambios
  ([F-OPINA-R007](../2026-10-24.03_analisis-y-opiniones/REQUIREMENT.md#F-OPINA-R007)).
- Volver a corregir y elegir el análisis siguen avisando «tanda», como hasta ahora. Anotar o deshacer un problema avisa
  «decisiones» desde [F-PROBS-R009](../2026-10-24.08_los-problemas/REQUIREMENT.md#F-PROBS-R009).

## Journeys

```yaml
journeys:
  - id: F-DECIDE-J001
    name: Aprobar con dos pestañas abiertas
    flow:
      - id: abrir
        action: "José abre Correcciones en dos pestañas"
        gate: [F-DECIDE-R001, F-DECIDE-R002]
        outcomes:
          - when: "Cada pestaña lee una vez los cambios y una vez las decisiones"
            then: aprobar
      - id: aprobar
        action: "Aprueba una corrección en la primera"
        gate: [F-DECIDE-R003, F-DECIDE-R010]
        outcomes:
          - when: "El servidor la acepta y avisa «decisiones»"
            then: otra
          - when: "El servidor la rechaza"
            then: vuelve
      - id: otra
        action: "La segunda pestaña la muestra en un segundo, sin bajar los cambios"
        gate: [F-DECIDE-R004, F-DECIDE-R008]
        outcomes:
          - when: "La cola la guarda"
            then: publicar
      - id: publicar
        action: "Publicar ya la cuenta"
        gate: [F-DECIDE-R005, F-DECIDE-R006]
        result: success
      - id: vuelve
        action: "La corrección vuelve a «sin revisar» y la pantalla dice por qué"
        gate: [F-DECIDE-R003]
        result: failure
  - id: F-DECIDE-J002
    name: Decidir desde la terminal
    flow:
      - id: terminal
        action: "José aprueba una corrección desde la terminal"
        gate: [F-DECIDE-R009]
        outcomes:
          - when: "El tablero ve la decisión y avisa «decisiones»"
            then: pestañas
      - id: pestañas
        action: "Las pestañas la muestran sin releer a mano, sin volver a leer la tanda"
        gate: [F-DECIDE-R004, F-DECIDE-R009]
        result: success
  - id: F-DECIDE-J003
    name: Incorporar una miniteoría
    flow:
      - id: incorporar
        action: "José incorpora una miniteoría"
        gate: [F-DECIDE-R007]
        outcomes:
          - when: "El tablero avisa «cambios» y «decisiones»"
            then: releer
      - id: releer
        action: "Las pestañas que miran esa tanda bajan la edición nueva; las que miran otra no bajan nada"
        gate: [F-DECIDE-R007, F-DECIDE-R008]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta a un modelo ni corre una medición de verdad. Las del navegador están en `html/test/` (node
--test, con un servidor de mentira; las de dos pestañas, con el store y el canal de avisos de verdad); las del tablero,
en `test/tablero_html/` (con carpetas de verdad y un CLI falso). Cada prueba nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-DECIDE-R001) | Una lectura de los cambios por pestaña de Correcciones y ninguna en otra pantalla; sin lo decidido; pasar a otra tanda con una lectura en camino; sólo `js/estado/cambios.js` nombra la ruta y las de antes no existen; el servidor arma los cambios sin estados, sin la cola y sin las aprobaciones, y las rutas viejas dan 404 | `html/test/cambios-y-decisiones.test.js`, `html/test/otra-tanda.test.js`, `html/test/arquitectura.test.js`, `html/test/api.test.js`, `test/tablero_html/sesion_de_correcciones_test.dart`, `test/tablero_html/servidor_del_tablero_test.dart`, `test/tablero_html/tablero_con_el_cli_test.dart`, `test/tablero_html/correcciones/rutas_de_correcciones_test.dart` |
| [R002](#F-DECIDE-R002) | Ninguna pantalla guarda lo decidido ni la vista, en ningún estado; una respuesta vieja no pisa; ediciones distintas: se relee una vez el que quedó atrás, también mientras corre una medición; la forma de las decisiones y cuándo sube su versión | `html/test/ninguna-pantalla-guarda-una-decision.test.js`, `html/test/cambios-y-decisiones.test.js`, `html/test/nadie-le-ve-problemas.test.js`, `html/test/arquitectura.test.js`, `html/test/api.test.js`, `sesion_de_correcciones_test.dart`, `servidor_del_tablero_test.dart`, `tablero_con_el_cli_test.dart`, `test/tablero_html/correcciones/dos_pestanas_test.dart` |
| [R003](#F-DECIDE-R003) | Aprobar se ve antes de que conteste el servidor; rechazada o sin conexión, vuelve a «sin revisar» y lo dice; nunca «sin revisar» en el medio; la cola con lo que no se guardó, marcado «sin guardar», con Reintentar; las escrituras devuelven todas las decisiones | `html/test/cambios-y-decisiones.test.js`, `html/test/operaciones.test.js`, `html/test/selectores.test.js`, `sesion_de_correcciones_test.dart`, `servidor_del_tablero_test.dart`, `tablero_con_el_cli_test.dart` |
| [R004](#F-DECIDE-R004) | Decidir en una pestaña llega a la otra en menos de 1 s sin bajar los cambios; decidir sube «decisiones» y no «tanda» ni «cambios»; la otra pestaña recibe el aviso en menos de 1 s y `/api/decisiones` ya la trae | `html/test/cambios-y-decisiones.test.js`, `html/test/estado-avisos.test.js`, `html/test/nadie-le-ve-problemas.test.js`, `sesion_de_correcciones_test.dart`, `test/tablero_html/novedades_test.dart`, `test/tablero_html/canal_de_avisos_test.dart`, `tablero_con_el_cli_test.dart`, `test/tablero_html/correcciones/aprobaciones_de_quiz_test.dart` |
| [R005](#F-DECIDE-R005) | La cola paso a paso con el aviso; el ritmo con y sin canal; lo trabado no cuenta; sin el sondeo de 2 s; el servidor avisa cada paso de la cola | `html/test/cambios-y-decisiones.test.js`, `html/test/operaciones.test.js`, `sesion_de_correcciones_test.dart` |
| [R006](#F-DECIDE-R006) | Con cada aviso de «decisiones», Publicar vuelve a contar, y con los de otros temas no; Publicar y el Mapa cuentan la decisión sin releer la tanda; lo que se pide mientras se guarda una decisión espera esa decisión y no relee la tanda | `html/test/estado-conectar.test.js`, `tablero_con_el_cli_test.dart`, `test/tablero_html/revision_compartida_test.dart` |
| [R007](#F-DECIDE-R007) | El aviso «cambios» relee con la edición que se tiene; incorporar y convertir releen; incorporar avisa, guardar un borrador o una opinión no, y un incorporar rechazado tampoco | `html/test/cambios-y-decisiones.test.js`, `html/test/estado-avisos.test.js`, `html/test/miniteoria.test.js`, `html/test/conversion.test.js`, `novedades_test.dart`, `rutas_de_correcciones_test.dart`, `dos_pestanas_test.dart` |
| [R008](#F-DECIDE-R008) | La misma edición contesta que es igual y la pantalla sigue con el mismo objeto; decidir no rearma el cuerpo de los cambios; otra edición se rearma y no avisa «decisiones» | `html/test/cambios-y-decisiones.test.js`, `sesion_de_correcciones_test.dart`, `servidor_del_tablero_test.dart`, `dos_pestanas_test.dart` |
| [R009](#F-DECIDE-R009) | Una decisión desde la terminal llega con el aviso y se ve; el vigía sube «decisiones» con `batch-reviews`; se leen sólo los estados, sin bucle; con otro contenido y las mismas decisiones no se llama al CLI, también durante una medición; una decisión propia que se está guardando no obliga a leer, ni a quien pregunta mientras tanto | `html/test/cambios-y-decisiones.test.js`, `revision_compartida_test.dart`, `sesion_de_correcciones_test.dart`, `tablero_con_el_cli_test.dart`, `novedades_test.dart` |
| [R010](#F-DECIDE-R010) | Aprobar manda `POST /api/correcciones/decisiones` con la tanda, su versión y su corrección; pasa por la cola y su diario; se rechaza sobre otra tanda, versión o corrección y sobre una tanda publicada | `html/test/cambios-y-decisiones.test.js`, `sesion_de_correcciones_test.dart` |
| [R011](#F-DECIDE-R011) | Las partes que le quedan a Correcciones; deshacer | `html/test/estado.test.js`, `html/test/operaciones.test.js` |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 7.
- **FEAT-AVISO**: el canal de avisos ([F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002));
  esta etapa suma «cambios» y «decisiones».
- **FEAT-LATAN**: la tanda que mira cada pestaña ([F-LATAN-R002](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R002),
  [F-LATAN-R007](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R007)).
- **FEAT-OPINA**: el análisis y las opiniones ([etapa 6](../2026-10-24.03_analisis-y-opiniones/REQUIREMENT.md)), que
  salieron de la tanda en paralelo con esta etapa. Juntas, la vista de Correcciones son los cambios, las decisiones y
  las opiniones.
