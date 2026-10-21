---
feature:
  id: FEAT-AVISO
  code: F-AVISO
  name: El servidor avisa - cada pestaña del tablero se entera en un segundo de lo que cambió, sin preguntar a cada rato
  priority: major
---

# El servidor avisa: cada pestaña se entera en un segundo de lo que cambió

## TL;DR

**Qué**: El tablero lleva la cuenta de qué cambió, por tema (corridas, tanda, jueces, análisis, problemas, publicar), y
se lo avisa a las pestañas abiertas apenas pasa: cuando lo cambia el propio tablero y cuando lo cambia un comando desde la
terminal. Las pestañas comparten un solo canal de avisos; con cada aviso, cada una relee sólo ese tema, una vez, y sólo si
alguien la está mirando. Lo que corre deja de preguntarse cada 3 s: se relee con el aviso y se controla una vez por
minuto. Si el canal se corta, todas lo dicen y se recuperan solas; si el tablero no sabe avisar, todo sigue como antes.

**Por qué**: Hasta ahora cada pestaña preguntaba a su ritmo (cada 3 s si algo corría, cada 30 s si no) y Ejecuciones,
Problemas y Exámenes tenían sus propios relojes. Con varias pantallas abiertas eso es muchas preguntas iguales, y aun así
una pantalla podía tardar hasta 30 s en ver algo que se hizo desde la terminal. Un canal por pestaña tampoco sirve: el
navegador da seis conexiones por servidor, y diez pestañas con su canal abierto las agotarían.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 2 «El
servidor avisa». La decisión de un solo canal compartido entre todas las pestañas es de José (20/10/2026). El tablero es
`pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José tiene abiertas Correcciones, el Mapa y Ejecuciones ✓.
2. Desde la terminal registra o termina una corrida ⚠️: cada pestaña se entera cuando le toca preguntar, entre 3 y 30 s
   después, y no todas a la vez.
3. Con nada corriendo, las tres pestañas preguntan lo mismo cada 30 s ⚠️; mientras algo corre, cada 3 s, aunque no haya
   cambiado nada.
4. Abre más pantallas ⚠️: cada una suma sus preguntas.

**Punto de fricción**: pasos 2 a 4. Las pestañas preguntan sin saber si algo cambió, y lo que cambia desde afuera del
tablero llega tarde.

**Cambio mínimo necesario**: que el tablero diga qué tema cambió apenas cambia, por un solo canal para todas las
pestañas, y que cada pestaña relea sólo eso.

## Reglas de Negocio

Un **tema** es una parte de lo que muestra el tablero que cambia junta: **corridas** (lo que corre y lo que terminó),
**tanda** (las correcciones, sus decisiones, la bandeja y la muestra ciega), **jueces** (sus exámenes y promociones),
**análisis** (las mediciones y el análisis vigente), **problemas** y **publicar**. Un **aviso** dice qué tema cambió, con
su número. El **canal de avisos** es la conexión por la que llegan. Una pestaña está **a la vista** si no está oculta.
**Releer** un tema es volver a pedirle al tablero lo que la pantalla muestra de ese tema.

<a id="F-AVISO-R001"></a>
### Rule[F-AVISO-R001] - El tablero numera cada tema y avisa cuando cambia
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El tablero lleva un número por tema, que empieza en cero al arrancar y sube de a uno cada vez que el tema cambia.
> 2. Lo suben las escrituras del tablero que salen bien, cada una el tema que cambia (por ejemplo: una decisión sube
>    tanda y publicar; lanzar, cancelar o reconocer una corrida sube corridas; rendir un examen sube corridas y jueces).
>    Lo que sólo estima, prepara o lee, y una escritura que falla, no suben nada.
> 3. Lo suben también los cambios en el disco hechos desde afuera del tablero (la terminal, una corrida que termina): un
>    archivo de la tanda escrito desde afuera da el aviso de «tanda» en menos de un segundo. Lo que se escribe sólo para
>    leer (cachés, candados) no sube nada.
> 4. El primer cambio de un tema se avisa enseguida; los que siguen dentro del medio segundo se juntan en un solo aviso
>    con el último número. El log de una corrida, que se escribe a cada rato mientras corre, avisa como mucho cada 3 s;
>    el registro de la corrida (que arrancó, avanzó o terminó) avisa enseguida.
> 5. Al abrirse el canal, lo primero que llega son los números de todos los temas en ese momento.

<details><summary>Detail</summary>

Que el log de una corrida avise como mucho cada 3 s es el mismo ritmo con el que antes se preguntaba mientras algo
corría: releer lo que corre con cada línea del log sería pedir más que antes. Lo que dice que una corrida arrancó, avanzó
o terminó lo escribe su registro, que avisa enseguida.

</details>

<a id="F-AVISO-R002"></a>
### Rule[F-AVISO-R002] - Cada aviso relee sólo su tema, una vez y sólo si alguien mira
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Con un aviso, cada pestaña relee lo que muestra de ese tema y nada más: un aviso de otro tema no relee nada.
> 2. Lo relee una sola vez aunque varias partes de la pantalla muestren ese tema.
> 3. Si la pestaña está oculta, no relee; al volver a verse, relee lo que se ve.
> 4. Dos avisos seguidos del mismo tema: la pantalla termina mostrando lo último. Un aviso repetido o con un número más
>    viejo que el último no relee nada.

<details><summary>Detail</summary>

En esta etapa sólo lo que corre se relee con los avisos (tema «corridas»): los demás temas ya se avisan, y cada pantalla
los empieza a usar cuando pase al estado único en las etapas que siguen de la propuesta.

</details>

<a id="F-AVISO-R003"></a>
### Rule[F-AVISO-R003] - Un solo canal para todas las pestañas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Con varias pestañas del tablero abiertas en el mismo navegador, una sola abre el canal de avisos y todas reciben
>    cada aviso.
> 2. Una pestaña que se abre después no abre otro canal: se suma al que hay y se entera de cómo está.
> 3. Si se cierra la pestaña que tenía el canal, otra lo abre enseguida y todas releen lo que se ve.
> 4. Las pestañas de antes de reiniciar el tablero no comparten el canal con las de después.
> 5. Ninguna otra parte del tablero abre el canal de avisos.

<a id="F-AVISO-R004"></a>
### Rule[F-AVISO-R004] - Si el canal se corta, se dice y se recupera solo
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Si el canal se corta, todas las pestañas dicen «Sin conexión con el tablero» y la que lo tenía vuelve a intentar,
>    esperando cada vez más entre intento e intento (1, 2, 5 y 10 s).
> 2. Al volver, todas releen lo que se ve y el aviso de sin conexión se va.
> 3. Un canal que no da señales durante 45 s se da por cortado.
> 4. Al volver a una pestaña con el canal cortado, se reintenta enseguida.
> 5. Si al reintentar el tablero contesta que la sesión venció (se reinició), todas las pestañas lo dicen como siempre y
>    nadie vuelve a pedir el canal.

<a id="F-AVISO-R005"></a>
### Rule[F-AVISO-R005] - El aviso reemplaza a las preguntas de lo que corre
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Con el canal abierto, lo que corre se relee con cada aviso de «corridas» y, por las dudas, una vez por minuto,
>    corra algo o no. Ya no se pregunta cada 3 s ni cada 30 s.
> 2. Lo que Ejecuciones refresca de lo que corre (la lista, el detalle de la elegida, su log) se refresca con cada aviso
>    de «corridas».

<details><summary>Detail</summary>

Problemas y Exámenes siguen con sus relojes de 10 s: esta etapa no los toca. Pasan a los avisos cuando cada una pase al
estado único, en las etapas que siguen de la propuesta.

</details>

<a id="F-AVISO-R006"></a>
### Rule[F-AVISO-R006] - Un tablero que no sabe avisar sigue como antes
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Si el tablero no tiene canal de avisos (uno de antes de esta etapa), las pestañas siguen preguntando lo que corre a
>    su ritmo de siempre (3 s mientras corre, 30 s si no) y nadie vuelve a pedir el canal.
> 2. Lo mismo si el navegador no permite compartir el canal entre pestañas.

<a id="F-AVISO-R007"></a>
### Rule[F-AVISO-R007] - El canal pide la misma sesión que el resto del tablero
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El canal de avisos se abre con la misma sesión y desde el mismo servidor que el resto del tablero: sin la sesión,
>    contesta que la sesión venció y no avisa nada; desde otro sitio, no contesta.
> 2. Sólo se lee: no acepta escrituras.
> 3. Un aviso dice el tema y su número, nunca rutas, nombres de archivos ni contenido.

## Journeys

```yaml
journeys:
  - id: F-AVISO-J001
    name: Un comando en la terminal llega a todas las pestañas
    flow:
      - id: abrir
        action: "José abre varias pantallas del tablero"
        gate: [F-AVISO-R003, F-AVISO-R007]
        outcomes:
          - when: "El tablero sabe avisar"
            then: comando
          - when: "El tablero es de antes y no tiene canal"
            then: como-antes
      - id: comando
        action: "Desde la terminal registra o termina una corrida"
        gate: [F-AVISO-R001]
        outcomes:
          - when: "El tablero ve el cambio en el disco"
            then: releer
      - id: releer
        action: "Cada pestaña a la vista relee lo que corre y lo muestra en menos de un segundo"
        gate: [F-AVISO-R002, F-AVISO-R005]
        result: success
      - id: como-antes
        action: "Las pestañas preguntan lo que corre a su ritmo de siempre"
        gate: [F-AVISO-R006]
        result: success
  - id: F-AVISO-J002
    name: Se cierra la pestaña que tenía el canal, o se corta
    flow:
      - id: cerrar
        action: "José cierra la pestaña que tenía el canal"
        gate: [F-AVISO-R003]
        outcomes:
          - when: "Quedan otras pestañas abiertas"
            then: relevo
      - id: relevo
        action: "Otra pestaña abre el canal y todas releen lo que se ve"
        gate: [F-AVISO-R003, F-AVISO-R002]
        outcomes:
          - when: "El tablero se apaga"
            then: cortado
      - id: cortado
        action: "Todas dicen «Sin conexión con el tablero»; la que tiene el canal reintenta"
        gate: [F-AVISO-R004]
        outcomes:
          - when: "El tablero vuelve con la misma sesión"
            then: vuelve
          - when: "El tablero volvió reiniciado"
            then: vencida
      - id: vuelve
        action: "Todas releen lo que se ve y el aviso de sin conexión se va"
        gate: [F-AVISO-R004]
        result: success
      - id: vencida
        action: "Todas muestran el cartel de la sesión vencida y nadie vuelve a pedir el canal"
        gate: [F-AVISO-R004]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta a un modelo. Las del navegador (`html/test/estado-avisos.test.js`, con candados, difusión, reloj
y canal de mentira de `html/test/avisos-de-prueba.js`) arman varias pestañas en el mismo proceso; las del servidor
(`test/tablero_html/novedades_test.dart`, `canal_de_avisos_test.dart`, `tablero_con_el_cli_test.dart`,
`ejecuciones/aviso_del_vigilante_test.dart`) usan carpetas de verdad y un CLI falso.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| R001 | Seis temas que empiezan en 0 y suben de a uno; el primer aviso enseguida y los siguientes juntos con el último número; qué escritura sube qué tema (y que estimar, preparar o fallar no sube nada); de qué tema es cada archivo; con carpetas de verdad, un archivo de la tanda escrito desde afuera da «tanda» en menos de un segundo, el log avisa como mucho cada tanto y el registro enseguida, la caché no sube nada, una carpeta que aparece después se mira; el canal manda primero los números y después cada aviso, sin guardarlo ni comprimirlo; el tablero armado como en el arranque avisa; una corrida desprendida que termina sube «corridas» | `novedades_test.dart` (grupos «F-AVISO-R001: …»), `canal_de_avisos_test.dart` («el canal dice qué tema cambió», «una escritura que sale bien sube su tema…»), `tablero_con_el_cli_test.dart` («el tablero armado como en bin avisa»), `aviso_del_vigilante_test.dart` |
| R002 | Un aviso de «corridas» relee lo que corre una vez; uno de otro tema no relee; dos avisos seguidos terminan con lo último; un aviso repetido o atrasado no relee; oculta no relee y al volver sí; el canal no toca la página, sabe si está a la vista por el estado | `estado-avisos.test.js` («F-AVISO-R002: …»), `novedades_test.dart` («dos cambios seguidos salen juntos…»), `arquitectura.test.js` («avisos.js no toca el DOM») |
| R003 | Tres pestañas abren una sola conexión y las tres reciben el aviso; una que llega después no abre otra; si se cierra la que lo tenía, otra lo abre y todas releen; cada sesión tiene su nombre de canal; sólo `js/estado/avisos.js` nombra el canal y nadie más arma una difusión entre pestañas sin motivo anotado | `estado-avisos.test.js` («F-AVISO-R003: …», «cada sesión tiene su nombre de canal…»), `arquitectura.test.js` («F-AVISO-R003: …») |
| R004 | Se corta el canal: todas sin conexión, la que lo tenía reintenta y al volver todas releen; las esperas 1, 2, 5 y 10 s; al volver a una pestaña con el canal cortado se reintenta ya; un canal mudo 45 s se da por cortado; si la sesión venció nadie vuelve a pedir el canal; una pestaña con la sesión vencida lo suelta y otra lo toma; al cerrar el servidor el canal se corta | `estado-avisos.test.js` («F-AVISO-R004: …»), `canal_de_avisos_test.dart` («al cerrar el servidor, el canal se corta») |
| R005 | Con el canal abierto, el control es cada 60 s corra algo o no; sin canal, el ritmo de siempre; cada aviso de «corridas» le trae a Ejecuciones lo que corre | `estado-avisos.test.js` («F-AVISO-R005: …») |
| R006 | Un tablero sin canal contesta 404 y las pestañas preguntan a su ritmo sin volver a pedirlo; sin candados o sin difusión en el navegador, tampoco se abre | `canal_de_avisos_test.dart` («F-AVISO-R006: …»), `estado-avisos.test.js` («F-AVISO-R006: …», «sin candados o sin difusión…») |
| R007 | Sin el token, 403 con la razón de la sesión vencida y ningún aviso; con otro Host, 403; un POST es 405; después de escrituras de verdad, el hola trae sólo los números y cada aviso sólo su tema y su número, sin rutas ni nombres de archivos | `canal_de_avisos_test.dart` («F-AVISO-R007: …», «un aviso dice sólo el tema y su número…») |

Además, en una copia de verificación de la campaña (sin modelos), con las diez pantallas abiertas: un comando en la
terminal que registra una corrida y otro que la termina se ven en las diez sin tocar nada; se cierra la pestaña que
tenía el canal y otra lo toma; se reinicia el tablero y todas dicen primero «Sin conexión» y después que la sesión venció.

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 2.
- **FEAT-CORRE**: lo que corre en un solo lugar ([F-CORRE-R003](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R003)):
  su ritmo de 3 y 30 s sigue valiendo cuando no hay canal; con el canal abierto lo reemplaza
  [F-AVISO-R005](#F-AVISO-R005). La sesión vencida y sin conexión son las de
  [F-CORRE-R005](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R005) y
  [F-CORRE-R006](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R006).
