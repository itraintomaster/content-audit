---
feature:
  id: FEAT-CORRE
  code: F-CORRE
  name: Lo que corre - las diez pantallas del tablero dicen lo mismo, a la vez, y avisan si la sesión venció
  priority: major
---

# Lo que corre: las diez pantallas del tablero dicen lo mismo, a la vez

## TL;DR

**Qué**: En el tablero, lo que está corriendo se pregunta una sola vez por pestaña y las diez pantallas lo leen de esa
única respuesta: la franja «en curso» de arriba, la insignia de Ejecuciones en la barra, Ejecuciones y el diálogo de
lanzar dicen siempre lo mismo. Se relee cada 3 s mientras algo corre y cada 30 s si no, al volver a la pestaña, al volver
la conexión y apenas se lanza, cancela o reconoce algo. Si el tablero se reinicia, todas las pantallas lo dicen y ofrecen
recargar; si se corta la conexión, lo dicen arriba. Y el trabajo de un paso de Problemas se cierra cuando termina su
corrida, aunque nadie esté mirando.

**Por qué**: Hoy cada pantalla tiene su propia copia de lo que corre (la franja, Ejecuciones y el diálogo, cada uno con
la suya, más una foto de 30 s guardada en el navegador), y cada copia se relee cuando le toca: dos pantallas pueden decir
distinto durante unos segundos. «Sesión vencida» sólo lo decía la franja. Y el trabajo de un problema se cerraba recién
cuando alguien leía Problemas o Exámenes: por eso Problemas llegó 20 s tarde.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 1 «Lo que
corre», aprobada por José el 20/10/2026. El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José lanza «Medir lo que falta» de un juez desde Exámenes ✓.
2. Tiene abiertas Ejecuciones y Correcciones en otras pestañas ⚠️: cada una se entera cuando le toca preguntar; por unos
   segundos una dice «corriendo» y la otra no.
3. La corrida termina ⚠️: si la lanzó un paso de un Problema, el paso sigue «corriendo» hasta que alguien abre Problemas
   o Exámenes.
4. José reinicia el tablero ⚠️: las pestañas abiertas fallan en cada pedido; sólo la franja dice que la sesión venció.

**Punto de fricción**: pasos 2 a 4. Lo que corre está copiado en varios lugares que se ponen al día cada uno a su ritmo,
y cerrar un trabajo depende de que alguien mire.

**Cambio mínimo necesario**: una sola pregunta por pestaña, que leen todas las partes de la pantalla; un cartel de
sesión vencida y un aviso de sin conexión iguales en todas las pantallas; y que el trabajo de un problema lo cierre el
tablero al terminar su corrida, no la lectura.

## Reglas de Negocio

**Lo que corre** es lo que el tablero sabe de las corridas: cuáles están corriendo, cuáles quedaron **huérfanas** (sin
proceso), cuáles terminaron hace poco, la última que terminó y si se puede lanzar otra (y, si no, por qué). La
**franja** es el renglón «en curso» de arriba de cada pantalla; la **insignia** es el número sobre Ejecuciones en la
barra. Una **pestaña** es una pantalla del tablero abierta en el navegador. La **sesión** es la que el tablero le da a
cada página al servirla; vence cuando el tablero se reinicia.

<a id="F-CORRE-R001"></a>
### Rule[F-CORRE-R001] - Todas las pantallas dicen lo mismo de lo que corre
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El tablero contesta lo que corre en una sola respuesta: las corridas que corren, las huérfanas y las recientes,
>    cada corrida una vez, la última que terminó y si se puede lanzar (`libre` u `ocupado`, con el motivo). Dice lo
>    mismo que el resumen de Ejecuciones, porque sale del mismo registro de actividad, y sin la sesión no contesta.
> 2. En las diez pantallas, la franja, la insignia, Ejecuciones y el diálogo de lanzar dibujan esa misma respuesta.
>    Ninguna guarda otra copia de lo que corre ni la recuerda entre pantallas.
> 3. Cada pantalla pone la franja, la insignia y el cartel de la sesión de la misma manera, después de la barra.

<details><summary>Detail</summary>

Antes Ejecuciones guardaba su propia copia «viva» y el diálogo de lanzar otra; ahora leen la misma respuesta que la
franja. La foto de 30 s que se guardaba en el navegador para no parpadear al cambiar de pantalla ya no está: la primera
respuesta llega en milisegundos. Los avisos de corridas terminadas que José cerró con la cruz siguen sin volver al
cambiar de pantalla, como antes.

</details>

<a id="F-CORRE-R002"></a>
### Rule[F-CORRE-R002] - Una sola pregunta por pestaña
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Aunque la franja, la insignia, la pantalla y el diálogo quieran saber qué corre a la vez, la pestaña pregunta una
>    sola vez.
> 2. Que una parte deje de mirar no corta a las otras; que vuelva a mirar no pregunta de nuevo si la respuesta está al
>    día.
> 3. Ninguna pantalla pregunta lo que corre por su cuenta ni tiene un reloj propio para eso.

<a id="F-CORRE-R003"></a>
### Rule[F-CORRE-R003] - Cuándo se vuelve a preguntar
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Mientras algo corre (también un lanzamiento a medias), la pestaña vuelve a preguntar cada 3 s.
> 2. Si no corre nada, cada 30 s. Cuando algo deja de correr, puede preguntar una vez más a los 3 s antes de pasar a 30.
> 3. Con la pestaña oculta no pregunta; al volver a la pestaña pregunta enseguida.

<a id="F-CORRE-R004"></a>
### Rule[F-CORRE-R004] - Después de lanzar, cancelar o reconocer, se pregunta enseguida
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Después de lanzar, cancelar o reconocer una corrida, la pestaña pregunta lo que corre enseguida, sin esperar su
>    ritmo.
> 2. Si en ese momento ya había una pregunta en viaje, pregunta otra vez cuando vuelve, para no quedarse con una
>    respuesta de antes.

<a id="F-CORRE-R005"></a>
### Rule[F-CORRE-R005] - Si el tablero se reinició, todas las pantallas piden recargar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cuando el tablero contesta que la sesión de la página no es la suya (se reinició), la pestaña queda con la sesión
>    vencida, sea cual sea el pedido que se enteró.
> 2. Con la sesión vencida, las diez pantallas muestran arriba un cartel que lo dice, con un botón «Recargar» que
>    recarga la página. El cartel no se cierra ni se va solo.
> 3. Con la sesión vencida, la pestaña deja de preguntar lo que corre, tampoco al volver a la pestaña o a la conexión.
> 4. Sólo eso vence la sesión: un rechazo por estar en sólo lectura o por venir de otro sitio no.

<a id="F-CORRE-R006"></a>
### Rule[F-CORRE-R006] - Sin conexión, se dice y se conserva lo último
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Si un pedido no llega al tablero, arriba dice «Sin conexión con el tablero · Se reintenta solo; lo que ves es lo
>    último que se supo.», sin botón, y se sigue viendo lo último que se supo de lo que corre.
> 2. Cuando vuelve la conexión, o cuando cualquier otro pedido de la página vuelve a llegar, la pestaña pregunta lo que
>    corre enseguida y el aviso se va.

<a id="F-CORRE-R007"></a>
### Rule[F-CORRE-R007] - El trabajo de un problema se cierra al terminar su corrida, aunque nadie mire
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cuando termina la corrida de un paso de un Problema, el trabajo queda cerrado (con su resultado) en 2 s como mucho,
>    aunque no haya ninguna pantalla abierta.
> 2. Leer Problemas o Exámenes no cierra ningún trabajo: sólo lo muestra como está.
> 3. Un trabajo cuya corrida terminó mientras el tablero estaba apagado se cierra cuando el tablero arranca.

## Journeys

```yaml
journeys:
  - id: F-CORRE-J001
    name: José lanza algo y todas las pestañas lo ven
    flow:
      - id: lanzar
        action: "José lanza una corrida desde cualquier pantalla"
        gate: [F-CORRE-R004]
        outcomes:
          - when: "El tablero la aceptó"
            then: ver
      - id: ver
        action: "La franja, la insignia, Ejecuciones y el diálogo dicen lo mismo en todas las pestañas"
        gate: [F-CORRE-R001, F-CORRE-R002, F-CORRE-R003]
        outcomes:
          - when: "La corrida termina"
            then: terminar
      - id: terminar
        action: "Las pestañas dejan de decir «corriendo»; si era de un Problema, su paso queda cerrado"
        gate: [F-CORRE-R003, F-CORRE-R007]
        result: success
  - id: F-CORRE-J002
    name: El tablero se reinicia con pestañas abiertas
    flow:
      - id: reiniciar
        action: "José reinicia el tablero con varias pantallas abiertas"
        gate: [F-CORRE-R005, F-CORRE-R006]
        outcomes:
          - when: "El tablero vuelve a contestar"
            then: vencida
          - when: "El tablero todavía no contesta"
            then: sin-conexion
      - id: sin-conexion
        action: "Arriba dice «Sin conexión con el tablero» y se ve lo último que se supo"
        gate: [F-CORRE-R006]
        outcomes:
          - when: "El tablero vuelve a contestar"
            then: vencida
      - id: vencida
        action: "Cada pantalla muestra el cartel de la sesión vencida con «Recargar»"
        gate: [F-CORRE-R005]
        outcomes:
          - when: "José toca «Recargar»"
            then: recargada
      - id: recargada
        action: "La página vuelve con la sesión nueva"
        gate: [F-CORRE-R005]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Las pruebas del navegador usan un servidor de mentira y un reloj de mentira; las del servidor, un CLI falso. Ninguna
consulta a un modelo.

| Regla | Qué se comprueba |
|---|---|
| R001 | Lo que contesta el tablero coincide con el resumen de Ejecuciones en cada fixture dorada (quieto, corriendo, huérfana), cada corrida una vez; sin la sesión o con otro Host no contesta. Ejecuciones y el diálogo no guardan otra copia: leen la misma respuesta que la franja. La franja crea su lugar una sola vez, se prende con algo corriendo y prende la insignia. Las diez pantallas se arman igual, cada una con su nombre, después de la barra |
| R002 | Tres partes que preguntan a la vez hacen un pedido; dejar de mirar una no corta a las otras. Ninguna pantalla nombra la ruta de lo que corre ni vuelve a tener un sondeo propio |
| R003 | Con un reloj de mentira: 3 s mientras corre, una vez más a los 3 s al terminar, después 30 s. Un lanzamiento a medias cuenta como corriendo. Oculta no pregunta; al volver a la pestaña pregunta enseguida |
| R004 | Preguntar ya pregunta enseguida y, si había una pregunta en viaje, otra vez al volver. Confirmar en el diálogo, cancelar y reconocer en Ejecuciones preguntan ya |
| R005 | El 403 de la sesión trae su razón; el de otro origen no. Si la puerta o cualquier otro pedido lo oye, la sesión vence y deja de preguntar, tampoco al volver a la pestaña o a la conexión. El cartel aparece en todas las pantallas, no se va y «Recargar» recarga. Un 403 de sólo lectura no vence la sesión |
| R006 | Un pedido que no llega deja la pestaña sin conexión: el aviso dice el texto, sin botón, y se sigue viendo lo último. Al volver la conexión, o al contestar otro pedido, pregunta enseguida y el aviso se va |
| R007 | Con el tablero andando y ninguna pantalla abierta, el trabajo de un paso pasa a «hecho» en el disco en 2 s como mucho después de que termina su corrida. Leer Problemas no lo cierra. Un tablero que arranca cierra lo que terminó mientras estaba apagado |

Además, en una copia de verificación de la campaña (sin modelos): lanzar algo y ver que las diez pantallas lo dicen a la
vez; reiniciar el tablero con pestañas abiertas y ver el cartel en todas.

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 1.
- **FEAT-CORTE**: el freno del diálogo de lanzar
  ([F-CORTE-R008](../2026-10-19.02_cortar-sin-trabar-la-tanda/REQUIREMENT.md#F-CORTE-R008)), que ahora lee lo que corre
  de esta misma respuesta.
