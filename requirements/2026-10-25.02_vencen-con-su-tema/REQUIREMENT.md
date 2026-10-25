---
feature:
  id: FEAT-VENCE
  code: F-VENCE
  name: Vencen con su tema - el Mapa, Analizadores, los cambios de Correcciones y el análisis vigente al día solos, sin «releer»
  priority: major
---

# Vencen con su tema: nada que releer en el tablero

## TL;DR

**Qué**: Lo que el tablero guarda de la tanda, del análisis vigente y de la vista de Analizadores queda viejo cuando sube
el número de un tema suyo, venga el cambio del tablero o de la terminal. El Mapa, Analizadores y Correcciones vuelven a
pedir lo suyo con el aviso de esos temas y reciben lo nuevo, sin «Volver a leer» ni el botón ↻. Ningún pedido fuerza al
tablero a leer de nuevo: la lista de lo que todavía mandaba o leía `releer` queda vacía.

**Por qué**: Después de la etapa 9 quedaban ocho «releer» (brief 212): el Mapa, Analizadores, los cambios de
Correcciones, el análisis vigente y Evolución. Unas memorias del tablero vencían por la huella de la campaña y otras
sólo con «releer»: la huella no mira las mediciones sueltas, el catálogo de analizadores ni lo que cambia una conversión,
así que el Mapa, Analizadores y Correcciones mostraban lo viejo hasta tocar el botón, y cada pantalla lo volvía a leer
forzando al CLI al terminar una corrida. Integrar leía la tanda dos veces: una por adelantado y otra al pedirla la
pantalla.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html). Cierra los tres
problemas que quedaron a medias (brief 213): 2.4 «Dos maneras de vencer una memoria», 9.2 «Memorias que sólo vencen con
releer» y 9.4 «Releer en todos lados». Las decisiones son de José (25/10/2026):
- los ocho «releer» salen en una etapa propia (brief 212, segunda pregunta);
- Evolución pasa a ser un resumen, y eso lo hace la etapa 9
  ([F-RESUM-R015](../2026-10-24.09_los-resumenes/REQUIREMENT.md)): acá no se toca;
- un analizador nuevo que aparece a lo sumo a los 5 minutos alcanza: no hace falta un aviso propio (brief 218,
  primera pregunta);
- la frase de la ayuda de Exámenes que mandaba a volver a leer sale, y el «Volver a leer» de Problemas queda como
  salida de emergencia (brief 218, segunda pregunta; F-VENCE-R009 inv. 3 y F-VENCE-R011);
- la ficha de Problemas se pone al día sola también con el aviso de «jueces», que cubre las entregas que son
  definiciones de jueces; para las demás queda el «Volver a leer» (F-VENCE-R012).

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. Termina una medición suelta, lanzada desde la terminal ✓.
2. El Mapa y Analizadores muestran las mediciones de antes ⚠️: la huella no mira las mediciones, y lo guardado sigue
   valiendo hasta tocar ↻.
3. José suma un juez en content-audit; Analizadores no lo muestra ⚠️ hasta tocar «Releer».
4. José sugiere una conversión en Correcciones: la pantalla le pide al tablero que relea la tanda entera ⚠️ para verla.
5. José integra una medición: el tablero relee la tanda antes de contestar y la vuelve a leer cuando la pide la pantalla
   ⚠️.

**Punto de fricción**: pasos 2 a 5. Cada memoria vence a su manera y la pantalla tiene que forzar la lectura.

**Cambio mínimo necesario**: que cada memoria sepa de qué temas depende y quede vieja cuando sube uno, y que cada
pantalla vuelva a pedir lo suyo con esos avisos, como hacen los resúmenes desde la etapa 9.

## Reglas de Negocio

Un **tema** es un grupo de datos que cambian juntos, con un número que sube con cada escritura del tablero y con lo que
el vigía de las carpetas ve escribirse desde la terminal (la tanda, los cambios, lo decidido, el análisis, los jueces,
las corridas). Una **memoria** es lo que el tablero guarda leído para todas las pestañas: la **tanda leída** (lo que
leen el Mapa, Correcciones y Vueltas), el **análisis vigente** de una tanda y la **vista de Analizadores** de una
tanda. La **huella** es cómo están los archivos de la campaña de los que sale la tanda. Algo **está corriendo** mientras
el registro de actividad lo tiene en curso.

<a id="F-VENCE-R001"></a>
### Rule[F-VENCE-R001] - La tanda leída vence con sus temas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. La tanda leída queda vieja cuando sube «cambios» (incorporar una miniteoría, sugerir o restaurar una conversión,
>    anotar un problema que cambia un caso permanente) o «análisis» (integrar, reemplazar o quitar, una medición que
>    termina, lo que la terminal escribe en las mediciones), aunque la huella no haya cambiado.
> 2. El próximo pedido la lee de nuevo, una sola vez para todas las pestañas que la piden.
> 3. Mientras algo corre, «análisis» no la pone vieja: durante una medición sube hasta dos veces por segundo. Al
>    terminar, sí, una vez ([F-RESUM-R002](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R002)).
> 4. Lo que no es de sus temas no la pone vieja: decidir (se aplica lo decidido sin volver a leer,
>    [F-DECIDE-R009](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md#F-DECIDE-R009)), lanzar, guardar un
>    borrador.
> 5. La huella sigue de respaldo: lo escrito en sus carpetas la pone vieja aunque el vigía no lo haya visto.

<a id="F-VENCE-R002"></a>
### Rule[F-VENCE-R002] - El análisis vigente vence con «análisis»
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El análisis vigente guardado de una tanda queda viejo cuando sube «análisis», con la misma regla del ruido que
>    F-VENCE-R001 inv. 3, o cuando cambia la huella.
> 2. Integrar, reemplazar o quitar lo deja viejo enseguida, aunque algo corra.

<a id="F-VENCE-R003"></a>
### Rule[F-VENCE-R003] - Integrar lee la tanda una vez
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Integrar, reemplazar o quitar contesta apenas content-audit lo guarda: el tablero no vuelve a leer la tanda antes
>    de contestar.
> 2. Las pantallas piden la tanda con el aviso, y se lee una sola vez para todas.

<a id="F-VENCE-R004"></a>
### Rule[F-VENCE-R004] - La vista de Analizadores vence con el catálogo y la tanda
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. La vista de Analizadores guardada de una tanda queda vieja cuando cambia el catálogo de analizadores o el archivo
>    de la tanda ([F-LATAN-R009](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R009)). El catálogo cambia con
>    «jueces» y, lo que ningún tema avisa (un analizador nuevo en el código de content-audit), a lo sumo a los 5 minutos
>    ([F-JUECES-R009](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R009)).
> 2. Lo del análisis vigente que muestra sale de F-VENCE-R002, en cada pedido.

<a id="F-VENCE-R005"></a>
### Rule[F-VENCE-R005] - Nadie lo pide a mano
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El Mapa, Analizadores y Correcciones no ofrecen volver a leer: ni el botón ↻ de arriba, ni el renglón del menú ⋯ ni
>    el de la búsqueda, ni el aviso «Releída». Si una lectura falla sin nada que mostrar, ofrecen «Volver a intentar»,
>    que pide lo mismo de siempre.
> 2. Ningún pedido de la tanda, de los cambios, del análisis vigente ni de Analizadores fuerza al tablero a leer de
>    nuevo: si llega `releer`, se ignora, y ya no hay una ruta para volver a leer los cambios.
> 3. La lista de lo que todavía manda o lee `releer`
>    ([F-RESUM-R005](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R005) inv. 3) baja en siete: no queda
>    ninguno del Mapa, Analizadores, los cambios ni el análisis vigente. Sigue siendo un tope: uno nuevo hace fallar las
>    pruebas.

<a id="F-VENCE-R006"></a>
### Rule[F-VENCE-R006] - El Mapa se pone al día solo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El Mapa vuelve a pedir lo suyo, sin que nadie lo pida, cuando:
>    - termina una corrida;
>    - sube «decisiones» o «cambios»;
>    - sube «tanda» sin nada corriendo (con algo corriendo, al terminar);
>    - sube «análisis» o «jueces», o cambia la tanda que se mira, como ya hacía
>      ([F-JUECES-R005](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R005),
>      [F-LATAN-R006](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R006));
>    - se integra, reemplaza o quita desde su diálogo del análisis vigente;
>    - se vuelve a la pestaña.
> 2. Lo pide de fondo: lo que se ve no desaparece ni se mueve lo elegido mientras tanto.
> 3. Varios motivos juntos piden una vez. Si llega un motivo mientras lee, lee una vez más al terminar.
> 4. Una pestaña oculta no pide: al volver, pide.

<a id="F-VENCE-R007"></a>
### Rule[F-VENCE-R007] - Analizadores se pone al día solo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Analizadores vuelve a pedir su vista, sin que nadie lo pida, cuando:
>    - termina una corrida;
>    - sube «tanda» sin nada corriendo (con algo corriendo, al terminar);
>    - sube «análisis» o «jueces», o cambia la tanda que se mira, como ya hacía;
>    - se integra, reemplaza o quita desde su diálogo del análisis vigente;
>    - se vuelve a la pestaña.
> 2. Lo pide de fondo, con las mismas condiciones que F-VENCE-R006 inv. 2 a 4.

<a id="F-VENCE-R008"></a>
### Rule[F-VENCE-R008] - Los cambios de Correcciones se ponen al día solos
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Correcciones vuelve a pedir los cambios de la tanda, sin forzar al tablero, cuando termina una corrida, se
>    incorpora una miniteoría, se sugiere o restaura una conversión o se integra desde su diálogo; y con lo que ya los
>    pedía: «cambios», otra tanda, otra edición
>    ([F-DECIDE-R007](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md#F-DECIDE-R007)). Llegan los nuevos porque
>    el tablero ya los dio por viejos (F-VENCE-R001).
> 2. Cada pedido sigue diciendo qué edición tiene: si el tablero tiene la misma, no baja nada
>    ([F-DECIDE-R008](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md#F-DECIDE-R008)).
> 3. Si el tablero rechaza una decisión porque la tanda cambió por debajo, Correcciones la vuelve a leer sola y el
>    aviso lo dice; no ofrece volver a leerla.

<a id="F-VENCE-R009"></a>
### Rule[F-VENCE-R009] - Ni la ayuda ni los mensajes mandan a releer
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Ningún tema de la ayuda del Mapa, Analizadores y Correcciones, en ningún estado de su recorrido, manda a tocar ↻,
>    «Volver a leer» ni «Releer». Dice que la pantalla se pone al día sola.
> 2. Los mensajes del Mapa, Analizadores y Correcciones no piden volver a leer: cuando algo cambió mientras se leía,
>    dicen que se vuelve a leer solo o qué esperar.
> 3. Tampoco la ayuda de Exámenes: cuando se aprueba la medición de un juez que bloquea, dice que la pantalla lo muestra
>    sola. Exámenes se pone al día con cada aviso de «jueces»
>    ([F-RESUM-R005](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R005)), y anotar la medición lo avisa.

<a id="F-VENCE-R010"></a>
### Rule[F-VENCE-R010] - Lo de la terminal llega solo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Con el Mapa, Analizadores y Correcciones abiertos, una medición que termina, una línea base nueva o una decisión
>    escritas desde la terminal se ven en esas pantallas sin tocar nada, en lo que tarda content-audit en leerlas.

<a id="F-VENCE-R011"></a>
### Rule[F-VENCE-R011] - El «Volver a leer» de Problemas queda, a propósito
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Problemas conserva su botón «Volver a leer», arriba. Vuelve a leer la lista y la ficha que se mira, con su
>    entrega, sin esperar ningún aviso.
> 2. Queda por decisión de José (25/10/2026), no como un resto de los «releer» que esta etapa sacó: es la salida de
>    emergencia si se corta el canal de avisos. Problemas ya se pone al día solo con «problemas», al volver a la pestaña
>    o a la ventana y al terminar la corrida de un trabajo abierto
>    ([F-PROBS-R003](../2026-10-24.08_los-problemas/REQUIREMENT.md#F-PROBS-R003),
>    [F-PROBS-R005](../2026-10-24.08_los-problemas/REQUIREMENT.md#F-PROBS-R005)).
> 3. Hay una razón más para no sacarlo: la entrega se compara con los archivos que hay hoy en el proyecto (el código y
>    las definiciones de los jueces). De los 36 archivos de las entregas registradas al 25/10/2026, 10 son definiciones
>    de jueces: avisan «jueces», y con eso la ficha se pone al día sola (F-VENCE-R012). Los otros 26 (el código, sobre
>    todo) no los avisa ningún tema: para ésos queda el botón.
> 4. No fuerza al tablero: pide lo mismo de siempre. Por eso no cuenta en la lista de lo que manda o lee `releer`
>    ([F-RESUM-R005](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R005) inv. 3).

<a id="F-VENCE-R012"></a>
### Rule[F-VENCE-R012] - La ficha de Problemas se pone al día con «jueces»
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Con un aviso de «jueces», Problemas vuelve a leer la ficha que se mira: su entrega, sus trabajos y la prueba de la
>    solución. Los trabajos y lo que cuesta probar salen del catálogo de los jueces y de sus casos permanentes, y una
>    entrega puede traer la definición de un juez, que avisa «jueces» al cambiar.
> 2. La lista de problemas no se vuelve a leer con «jueces», porque no depende de ellos. Tampoco cambia lo que lee el
>    cajón de problemas de Correcciones, que es esa lista.

## Journeys

```yaml
journeys:
  - id: F-VENCE-J001
    name: Termina una medición desde la terminal
    flow:
      - id: termina
        action: "Termina una medición suelta lanzada desde la terminal, con el Mapa y Analizadores abiertos"
        gate: [F-VENCE-R010]
        outcomes:
          - when: "El vigía ve la medición y avisa «análisis»"
            then: vence
      - id: vence
        action: "El tablero da por viejos la tanda leída y el análisis vigente"
        gate: [F-VENCE-R001, F-VENCE-R002]
        outcomes:
          - when: "Cada pantalla pide lo suyo"
            then: nuevo
      - id: nuevo
        action: "El Mapa y Analizadores muestran la medición nueva sin que nadie toque ↻"
        gate: [F-VENCE-R006, F-VENCE-R007, F-VENCE-R005]
        result: success
  - id: F-VENCE-J002
    name: Sugerir una conversión
    flow:
      - id: sugiere
        action: "José sugiere pasar a opción múltiple un ejercicio en Correcciones"
        gate: [F-VENCE-R001]
        outcomes:
          - when: "El tablero sube «cambios»"
            then: pide
      - id: pide
        action: "Correcciones pide los cambios con la edición que tiene, sin forzar al tablero"
        gate: [F-VENCE-R008]
        outcomes:
          - when: "El tablero lee la tanda una vez y contesta la edición nueva"
            then: ve
      - id: ve
        action: "José ve la conversión entre los cambios"
        gate: [F-VENCE-R008]
        result: success
  - id: F-VENCE-J003
    name: Integrar una medición
    flow:
      - id: integra
        action: "José integra una medición desde el diálogo del análisis vigente en el Mapa"
        gate: [F-VENCE-R003]
        outcomes:
          - when: "El tablero contesta apenas content-audit guarda, y avisa «análisis»"
            then: lee
      - id: lee
        action: "El Mapa, Correcciones y Analizadores piden lo suyo; la tanda se lee una vez"
        gate: [F-VENCE-R002, F-VENCE-R003, F-VENCE-R006]
        result: success
```

## Open Questions

- (decidido por José, 25/10/2026) Un analizador nuevo escrito en content-audit no lo avisa ningún tema: aparece en
  Analizadores a lo sumo a los 5 minutos (F-VENCE-R004 inv. 1). Alcanza: no hace falta un aviso propio.
- (decidido por José, 25/10/2026) El «Volver a leer» de Problemas queda como salida de emergencia (F-VENCE-R011), y la
  frase de la ayuda de Exámenes que mandaba a volver a leer sale (F-VENCE-R009 inv. 3).
- (decidido por José, 25/10/2026) Los trabajos de una ficha de Problemas y lo que cuesta probar salen del catálogo de
  los jueces y de sus casos permanentes: la ficha se pone al día también con «jueces» (F-VENCE-R012).
- (aclarado) «Volver a leer» de Ejecuciones no es una memoria del tablero: pide de nuevo lo que corre, que ya se pone
  al día con el aviso de «corridas». No es de esta etapa.

## Cómo se prueba

Ninguna prueba consulta a un modelo. Las del navegador están en `html/test/` (node --test, con un servidor de mentira y
el store de verdad); las del tablero, en `test/tablero_html/` (con carpetas de verdad y un CLI falso). Cada prueba nombra
su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-VENCE-R001) | Con otra clave de «cambios» y «análisis» la tanda se lee de nuevo aunque la campaña no cambió, una vez para las tres pantallas; «decisiones», «jueces» y «corridas» no; mientras algo corre «análisis» no cuenta y «cambios» sí; una decisión propia no la deja al día; un pedido posterior no se queda con la lectura que ya viajaba; la huella sigue de respaldo | `revision_compartida_test.dart` (grupo «F-VENCE-R001: la tanda leída vence con sus temas», con inv. 3 e inv. 5); `tablero_con_el_cli_test.dart` «F-VENCE-R001: «cambios» y «análisis» la ponen vieja…»; `rutas_de_mapa_test.dart` «F-VENCE-R001: cuando sube un tema suyo vuelve a leer…» |
| [R002](#F-VENCE-R002) | Sube «análisis»: se lee de nuevo aunque la huella sea la misma; mientras algo corre, no, y al terminar una vez; integrar lo deja viejo enseguida aunque algo corra; un pedido con otra clave no se queda con la lectura en vuelo | `analisis_vigente_test.dart` (grupo «F-VENCE-R002: el análisis vence con «análisis»») |
| [R003](#F-VENCE-R003) | Integrar contesta sin leer la tanda; la pantalla que la pide después la lee una vez, para todas | `tablero_con_el_cli_test.dart` «F-VENCE-R003: integrar contesta sin volver a leer la tanda…» |
| [R004](#F-VENCE-R004) | Otro catálogo de los jueces arma otra vista; el mismo catálogo releído, no; lo del análisis vigente llega nuevo en cada pedido sin volver a armar la vista | `rutas_de_analizadores_test.dart` «F-VENCE-R004: con otro catálogo de los jueces…» y «F-VENCE-R004 inv. 2: lo del análisis vigente sale de cada pedido…» |
| [R005](#F-VENCE-R005) | `releer=1` se ignora en el Mapa, Analizadores y el análisis vigente; `POST /api/cambios/releer` contesta 404; el navegador no lo manda; no hay ↻ ni «Volver a leer» en ningún estado del Mapa ni de Analizadores, ni en el menú ni la búsqueda de Correcciones; «Volver a intentar» pide lo de siempre; la lista de `releer` queda vacía | `rutas_de_mapa_test.dart`, `rutas_de_analizadores_test.dart`, `analisis_vigente_test.dart`, `servidor_del_tablero_test.dart`, `tablero_con_el_cli_test.dart` («F-VENCE-R005: …»); `sin-releer.test.js`; `mapa-operaciones.test.js`, `mapa-gestos.test.js`, `mapa-vistas.test.js`, `analizadores-estado.test.js`, `analizadores-gestos.test.js`, `analizadores-vistas.test.js`, `gestos.test.js`, `operaciones.test.js` («F-VENCE-R005: …») |
| [R006](#F-VENCE-R006) | El Mapa se pone al día al terminar una corrida y con «tanda», «cambios» y «decisiones» (la regla del ruido), al integrar desde su diálogo y al volver a la pestaña; de fondo; una vez aunque lleguen varios motivos y otra más si llega uno mientras lee; un fallo seguido de otra lectura no se avisa; oculta no pide | `vencen-con-su-tema.test.js` y `mapa-operaciones.test.js` («F-VENCE-R006: …») |
| [R007](#F-VENCE-R007) | Analizadores se pone al día al terminar una corrida y con «tanda», no con «decisiones» ni «cambios»; de fondo, sin aviso; una vez aunque lleguen varios; un motivo durante una lectura pedida se pide al terminar ésa; el fallo, abajo | `vencen-con-su-tema.test.js` y `analizadores-estado.test.js` («F-VENCE-R007: …») |
| [R008](#F-VENCE-R008) | Correcciones vuelve a pedir los cambios al terminar una corrida, al integrar, al incorporar una miniteoría y al sugerir una conversión, sin forzar al tablero y con la edición que tiene; si el pedido que contestó salió antes, los pide otra vez; un 409 al decidir o al convertir la relee sola y el aviso lo dice | `vencen-con-su-tema.test.js`, `miniteoria.test.js`, `conversion.test.js`, `operaciones.test.js` («F-VENCE-R008: …») |
| [R009](#F-VENCE-R009) | Ningún tema de la ayuda del Mapa, Analizadores y Correcciones, en ningún estado de su recorrido, manda a ↻, «Volver a leer» ni «Releer»; los avisos de un fallo, de una muestra que cambió y de una comparación de problemas de otra versión no piden volver a leer; la ayuda de Exámenes, cuando se aprueba un juez que bloquea, dice que la pantalla lo muestra sola, y la guardia reconoce «Volvé a leer» también al empezar la frase | `ayuda-sin-releer.test.js`, `mapa-ayuda.test.js`, `mapa-vistas.test.js`, `operaciones.test.js`, `muestra.test.js`, `evolucion-problemas.test.js` («F-VENCE-R009: …») |
| [R010](#F-VENCE-R010) | Una medición que termina en la terminal (el vigía ve `evaluations/`) sube «análisis»: el Mapa y el análisis vigente se leen de nuevo sin `releer`, aunque la huella no mira las mediciones. Medido además a mano en una copia de verificación (brief de la etapa) | `tablero_con_el_cli_test.dart` «F-VENCE-R010, F-VENCE-R001, F-VENCE-R002: …» |

| [R011](#F-VENCE-R011) | El «Volver a leer» de Problemas sigue arriba y vuelve a leer la lista y la entrega de la ficha sin esperar ningún aviso | `estado-problemas.test.js` «F-VENCE-R011: …» |
| [R012](#F-VENCE-R012) | Un aviso de «jueces» vuelve a leer la entrega, los pasos y la prueba de la ficha que se mira, y no la lista; el tema «jueces» marca viejas también las lecturas de la ficha | `estado-problemas.test.js` «F-VENCE-R012: …»; `estado-avisos.test.js` (la prueba de las etiquetas de cada tema, que nombra F-VENCE-R012) |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), problemas 2.4, 9.2 y
  9.4.
- **Briefs**: 212 (los ocho «releer»), 213 (las nueve etapas) y 218 (esta etapa, con las dos preguntas que José
  contestó).
- **FEAT-RESUM**: los resúmenes, la regla del ruido y la lista que sólo baja
  ([F-RESUM-R002](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R002),
  [F-RESUM-R005](../2026-10-24.09_los-resumenes/REQUIREMENT.md#F-RESUM-R005)).
- **FEAT-AVISO**: los temas, sus números y el vigía de las carpetas
  ([F-AVISO-R008](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R008)).
- **FEAT-DECIDE**: lo decidido sin volver a leer
  ([F-DECIDE-R009](../2026-10-24.04_cambios-y-decisiones/REQUIREMENT.md#F-DECIDE-R009)).
- **FEAT-JUECES**: el catálogo leído una vez
  ([F-JUECES-R009](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R009)).
