---
feature:
  id: FEAT-JUECES
  code: F-JUECES
  name: Los jueces - un catálogo de jueces con su estado, que leen todas las pantallas, y una sola cuenta de «habilitado»
  priority: major
---

# Los jueces: un catálogo, y el estado de cada juez se cuenta una vez

## TL;DR

**Qué**: El tablero lee una sola vez el catálogo de jueces: de cada uno, su versión de hoy, con qué motores está
habilitada y su último examen. Una sola cuenta dice si un juez está habilitado, sin aprobar o habilitado sólo con otro
motor, y si deja cerrar la tanda. Las siete pantallas que tenían su copia del juez la leen de ahí: Exámenes, el Mapa,
Correcciones, Publicar, Analizadores, Estadísticas y el diálogo de lanzar. Registrar un examen, promover un juez,
sumarle un caso permanente o cambiar su versión, desde el tablero o desde la terminal, avisa «jueces», y todas las
pantallas cambian su marca en un segundo. Exámenes deja de preguntar cada 10 s.

**Por qué**: Hasta ahora el estado de un juez se contaba de cuatro maneras: la del análisis, la de Exámenes, la de cada
pasada y la del diálogo de lanzar. Cada una miraba un motor distinto: el mismo juez podía salir habilitado en el Mapa y
«sin examen aprobado» en Vueltas. Siete pantallas guardaban su copia y cinco memorias del tablero leían el catálogo, cada
una por su lado. Promover un juez desde la terminal no cambiaba ninguna pantalla; ni Exámenes se enteraba.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 5
«Jueces». El diagnóstico y las preguntas están en el brief 199. Las decisiones son de José (21 y 24/10/2026):
- habilitada con cualquier motor deja cerrar la tanda, pero eso necesita que content-audit consulte a cada juez con un
  motor con el que está habilitado: es una tarea aparte, que llegó el mismo 24/10
  ([FEAT-CMOTOR](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md)). «Deja cerrar» sigue al motor con que
  content-audit va a consultar a cada juez al cerrar, y se decide en un solo lugar;
- el nombre de la versión de hoy («la del 19/10») lo sigue dando el análisis;
- mientras no llegó el estado de un juez, no se dibuja su marca y lo que gasta queda apagado;
- para Exámenes alcanza el aviso, sin control periódico.

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José promueve la Naturalidad desde la terminal ✓.
2. Exámenes sigue diciendo que necesita rendir ⚠️: su memoria no mira las promociones.
3. El Mapa sigue diciendo «sin aprobar» y con «Medir lo que falta» apagado ⚠️, hasta que José relee a mano.
4. Abre el diálogo de lanzar: los motores que ofrece salen de la última estimación ⚠️, no de la promoción.
5. Un juez aprobado sólo con Gemini sale habilitado en el Mapa, «habilitado con otro motor» en Exámenes y «sin examen
   aprobado» en Vueltas ⚠️.

**Punto de fricción**: pasos 2 a 5. Cada pantalla guarda su copia del juez y la cuenta a su manera.

**Cambio mínimo necesario**: que el tablero lea el catálogo una vez, cuente el estado de cada juez en un solo lugar y
avise cuando cambia, y que las pantallas lo lean de ahí.

## Reglas de Negocio

Un **juez** es un analizador que juzga con un modelo y rinde examen. Su **versión de hoy** es la que corre ahora. Un
**motor** es proveedor, modelo y esfuerzo; el **motor de siempre** es con el que el tablero consulta si no se elige otro.
La versión de hoy está **habilitada con un motor** si aprobó su examen con ese motor o si José la promovió con él. El
**estado de un juez** es: su versión de hoy, con qué motores está habilitada, si lo está con el de siempre, cómo está su
examen y si deja cerrar la tanda. El **catálogo de jueces** es el estado de todos, leído junto. Lo que dice cada veredicto
(al día, anterior, sin medir) no es el estado del juez: lo pone content-audit en el análisis.

### Lo que no es el estado de hoy

Estas cosas nombran a un juez y no son su estado de hoy: se quedan donde están y no se leen del catálogo.

- **Lo que dice cada veredicto** (al día, de una versión anterior, sin medir, y el nombre de la versión de hoy, «la
  del 19/10»): lo pone content-audit en el análisis.
- **Lo que anotó cada pasada**: cómo estaba cada juez al cerrar esa tanda, con qué motor consultó y cuánto gastó. Es
  el informe de cierre de Correcciones, Vueltas, Comparar y, en Analizadores, «Su juez en el último cierre».
- **Quién juzga**, en Estadísticas: con qué modelo y proveedor corrió el análisis que se mira.
- **Lo que calcula la estimación** para el motor elegido en el diálogo de lanzar: si cada juez es elegible con él.
- **Problemas** sigue leyendo la vista de Exámenes con el estado de cada juez hasta su etapa (la 8 de la propuesta). El
  estado lo cuenta el mismo lugar, así que dice lo mismo.

<a id="F-JUECES-R001"></a>
### Rule[F-JUECES-R001] - Un juez dice lo mismo en todas las pantallas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Exámenes, el Mapa, Correcciones, Publicar, Analizadores, Estadísticas y el diálogo de lanzar muestran el mismo
>    estado de un juez: el de la última lectura del catálogo de la pestaña.
> 2. Mirado en Exámenes y en el Mapa, un juez dice lo mismo: si está habilitado, con qué motores y si deja cerrar.
> 3. Una versión promovida se dice «Promovida» en todas; el examen se nombra con las mismas palabras en todas.

<a id="F-JUECES-R002"></a>
### Rule[F-JUECES-R002] - El estado de un juez se cuenta una vez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El tablero cuenta el estado de cada juez en un solo lugar. Ninguna pantalla lo vuelve a contar.
> 2. La versión de hoy está **habilitada** si lo está con algún motor; si no lo está con ninguno, el juez está **sin
>    aprobar**, aunque haya aprobado con una versión anterior
>    ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003), inv. 4).
> 3. Habilitada, pero no con el motor de siempre, es **habilitada con otro motor**: no es «sin aprobar».
> 4. El estado del juez no cambia lo que dice cada veredicto. Lo que dio la versión de hoy está al día con cualquier
>    motor, y la marca de cada veredicto la sigue poniendo content-audit
>    ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003), inv. 7). El tablero no compara
>    versiones entre veredictos ([F-TALDIA-R001](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R001),
>    inv. 1).

<a id="F-JUECES-R003"></a>
### Rule[F-JUECES-R003] - «Deja cerrar» sigue al motor del cierre
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Un juez deja cerrar la tanda si content-audit va a consultarlo al cerrar con un motor con el que su versión de hoy
>    está habilitada. Eso lo dice content-audit: con qué motor cerraría cada juez, o que no tiene con cuál
>    ([F-CMOTOR-R006](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md#F-CMOTOR-R006)). El tablero no lo
>    vuelve a contar.
> 2. Un content-audit de antes, que no dice con qué motor cierra, consulta a todos los jueces con el motor de siempre:
>    con él, deja cerrar sólo el juez habilitado con el de siempre.
> 3. Un juez habilitado sólo con otro motor que el cierre no va a consultar no deja cerrar, y la pantalla dice con qué
>    motor sí está habilitado. Si el cierre lo consulta con otro motor, Exámenes dice con cuál.
> 4. Se decide en un solo lugar del tablero, sin tocar pantallas.
> 5. El tablero nunca dice que un juez deja cerrar si content-audit no lo va a dejar.

<details><summary>Detail</summary>

José eligió (21/10) que habilitada con cualquier motor deje cerrar. Hasta FEAT-CMOTOR una corrida de content-audit
consultaba a todos los jueces con el mismo motor, el que le pasa el tablero: un juez habilitado sólo con Gemini, en un
cierre con ChatGPT, consultaba 0 veces y la tanda quedaba «no se puede decidir». José decidió (24/10) que eso se
resolviera en una tarea aparte de content-audit; llegó el mismo día. Desde entonces `exam status -f json` dice, de la
versión de hoy de cada juez, `closesWith`: el motor con que lo consultaría el cierre, o null si no tiene ninguno.

</details>

<a id="F-JUECES-R004"></a>
### Rule[F-JUECES-R004] - Ninguna pantalla guarda el estado de un juez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El estado de cada juez está una sola vez en la pestaña, en el catálogo. Ninguna pantalla guarda su propia copia.
> 2. Lo que una pantalla lee del tablero ya no trae el estado del juez: trae lo suyo (las cuentas del análisis, la
>    historia de los exámenes, lo que gastó cada pasada).
> 3. Un juez nuevo aparece en las siete pantallas sin tocarlas.
> 4. Si la versión de hoy de un juez está sin aprobar lo dice el catálogo también en el carril de mediciones del Mapa
>    y en la ficha de una corrida suelta: el análisis ya no manda si la versión de hoy aprobó en cada corrida.

<a id="F-JUECES-R005"></a>
### Rule[F-JUECES-R005] - Cuando cambia un juez, todas se enteran
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Registrar un examen, promover un juez, sumarle un caso permanente o cambiar su versión de hoy, desde el tablero o
>    desde la terminal, avisa «jueces».
> 2. Con el aviso, la pestaña relee el catálogo una vez, sólo si está a la vista; una oculta relee al volver.
> 3. Se registra un examen y el Mapa y Correcciones cambian su marca en un segundo o menos.
>
>    *Medido el 24/10 en la copia de verificación, con el CLI de verdad:* el aviso llega en menos de un segundo, pero la
>    marca cambia unos 3 s después de escribir la promoción, porque casi todo ese tiempo (2,9 s) es lo que tarda
>    `exam status` en arrancar. Con el CLI falso de las pruebas, en menos de un segundo. Queda para José decidir si
>    alcanza o si hay que leer el estado sin arrancar el CLI.
> 4. Se promueve un juez desde la terminal y el diálogo de lanzar ofrece ese motor, aunque esté abierto.
> 5. Las pantallas que muestran lo que dijo cada veredicto vuelven a leer lo suyo cuando cambia el catálogo.

<a id="F-JUECES-R006"></a>
### Rule[F-JUECES-R006] - Exámenes no pregunta a cada rato
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Exámenes no tiene reloj propio. Relee la historia de los exámenes con cada aviso de «jueces», cuando termina la
>    rendición que está abierta y al volver a la pestaña.
> 2. Lee una vez por vez: lo que pide releer mientras está leyendo sale una sola vez, al terminar.
> 3. Lo que cambia sin aviso (lo que Problemas prepara del examen fijo) se ve al volver a la pestaña.

<a id="F-JUECES-R007"></a>
### Rule[F-JUECES-R007] - Sin el estado del juez, no se gasta
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Mientras no llegó el estado de un juez, o si el catálogo no lo tiene, no se dice «habilitado» ni «sin aprobar».
> 2. Los botones que gastan con ese juez quedan apagados, y su (!) dice que se están leyendo los jueces.
> 3. Cuando llega, se dibuja con lo que dice, sin recargar la página.

<a id="F-JUECES-R008"></a>
### Rule[F-JUECES-R008] - El diálogo de lanzar ofrece lo que dice el catálogo
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Los motores que el diálogo ofrece para medir con un juez son los del catálogo: los que habilitan la versión de hoy.
> 2. Los demás quedan apagados con «no aprobó con la versión de hoy», como hasta ahora
>    ([F-TALDIA-R004](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R004)).
> 3. Si el catálogo cambia con el diálogo abierto, lo ofrece sin reabrirlo y vuelve a estimar.

<a id="F-JUECES-R009"></a>
### Rule[F-JUECES-R009] - En el tablero, el catálogo se lee una vez
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. El tablero lee el catálogo de jueces una vez, para todas las pantallas.
> 2. Lo vuelve a leer sólo cuando cambia algo que decide el estado de un juez: un examen, una promoción, un caso
>    permanente o la carpeta del juez.
> 3. Dos pantallas que lo piden a la vez esperan la misma lectura.

## Journeys

```yaml
journeys:
  - id: F-JUECES-J001
    name: Registrar un examen
    flow:
      - id: registrar
        action: "José registra el examen aprobado de un juez en Exámenes"
        gate: [F-JUECES-R005]
        outcomes:
          - when: "El tablero avisa «jueces»"
            then: releer
      - id: releer
        action: "Cada pestaña relee el catálogo una vez"
        gate: [F-JUECES-R005, F-JUECES-R009]
        outcomes:
          - when: "La versión de hoy quedó habilitada"
            then: marcas
      - id: marcas
        action: "El Mapa y Correcciones dejan de decir «sin aprobar» en un segundo y vuelven a leer lo suyo"
        gate: [F-JUECES-R001, F-JUECES-R005]
        result: success
  - id: F-JUECES-J002
    name: Promover un juez desde la terminal
    flow:
      - id: promover
        action: "José promueve un juez con un motor desde la terminal"
        gate: [F-JUECES-R005]
        outcomes:
          - when: "El tablero ve la promoción y avisa «jueces»"
            then: dialogo
      - id: dialogo
        action: "El diálogo de lanzar, abierto, ofrece ese motor y vuelve a estimar"
        gate: [F-JUECES-R008]
        outcomes:
          - when: "José mira el juez en Exámenes"
            then: examenes
      - id: examenes
        action: "Exámenes dice «Promovida», igual que el Mapa"
        gate: [F-JUECES-R001]
        result: success
  - id: F-JUECES-J003
    name: Un juez habilitado sólo con otro motor
    flow:
      - id: mirar
        action: "José mira un juez habilitado sólo con Gemini"
        gate: [F-JUECES-R002]
        outcomes:
          - when: "Lo mira en cualquier pantalla"
            then: dice
      - id: dice
        action: "Todas dicen «habilitada con Gemini» y que no deja cerrar con el motor de siempre"
        gate: [F-JUECES-R001, F-JUECES-R003]
        result: success
```

## Open Questions

No quedan abiertas. La tarea aparte «Cerrar la tanda consultando a cada juez con su motor» llegó como
[FEAT-CMOTOR](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md) y [F-JUECES-R003](#F-JUECES-R003) la sigue.

## Cómo se prueba

Ninguna prueba consulta a un modelo ni rinde un examen de verdad: el registro y la promoción se simulan. Las del
navegador están en `html/test/` (node --test, con un servidor de mentira y el store de verdad); las del tablero, en
`test/tablero_html/` (con carpetas de verdad y un CLI falso). Cada prueba nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-JUECES-R001) | Lo que manda `GET /api/jueces` con los datos de la copia a1-v11, en el orden del catálogo, y que la página lo lee igual; Exámenes y el Mapa dicen lo mismo de cada juez, con lo del servidor y en cada caso (con el de siempre, sólo con otro motor, promovido, sin aprobar) | `test/tablero_html/jueces/rutas_de_jueces_test.dart`, `html/test/los-jueces-del-servidor.test.js`, `html/test/los-jueces.test.js` |
| [R002](#F-JUECES-R002) | Una sola cuenta: habilitada con el de siempre, sólo con otro motor, promovida, sin aprobar, aprobación sólo histórica, sin versión de hoy; motores sin repetir; la vista de Exámenes (la de Problemas) y el catálogo cuentan igual; el carril de mediciones y el resto del Mapa dicen lo mismo, antes y después de promover | `test/tablero_html/jueces/estado_de_un_juez_test.dart`, `rutas_de_jueces_test.dart`, `html/test/marca-del-juez.test.js`, `html/test/mapa-mediciones-por-juez.test.js` |
| [R003](#F-JUECES-R003) | `closesWith` decide; sin él, el motor de siempre; null no deja cerrar; el diálogo de lanzar nombra el otro motor sólo si el cierre no lo consulta | `estado_de_un_juez_test.dart`, `rutas_de_jueces_test.dart`, `html/test/lanzar-selectores.test.js`, `html/test/los-jueces.test.js` |
| [R004](#F-JUECES-R004) | Ninguna pantalla guarda un juez en ningún estado de su recorrido (ni el estado del examen de una cobertura ni si aprobó en una corrida suelta); ningún archivo del navegador lee los campos del estado de hoy; `/api/examenes/jueces`, Estadísticas y el análisis ya no los mandan; el carril y la ficha de una corrida avisan «sin aprobar» con el catálogo | `html/test/ninguna-pantalla-guarda-un-juez.test.js`, `rutas_de_jueces_test.dart`, `test/tablero_html/estadisticas/vista_de_estadisticas_test.dart`, `test/tablero_html/analisis_vigente_test.dart`, `html/test/mapa-mediciones-por-juez.test.js` |
| [R005](#F-JUECES-R005) | Registrar sube «jueces»; el vigía sube «jueces» con un examen, una promoción, un caso permanente o la carpeta de un juez, y no con lo que deja Python; una promoción desde la terminal llega como aviso en menos de un segundo y `/api/jueces` ya la dice; el aviso relee el catálogo en cada pestaña y la marca cambia; el diálogo abierto ofrece el motor promovido | `test/tablero_html/novedades_test.dart`, `test/tablero_html/tablero_con_el_cli_test.dart`, `html/test/estado-avisos.test.js`, `html/test/lanzar-motores-habilitados.test.js` |
| [R006](#F-JUECES-R006) | Exámenes relee con el aviso y al volver; una lectura en vuelo deja una sola pendiente; sin `setInterval` | `html/test/examenes-al-dia.test.js` |
| [R007](#F-JUECES-R007) | Sin leer: ni habilitado ni sin aprobar, lo que gasta apagado con su (!); 404 apagado; error sin nada leído | `html/test/estado-jueces.test.js`, `html/test/marca-del-juez.test.js`, `html/test/lanzar-motores-habilitados.test.js`, `html/test/mapa-motores-habilitados.test.js` |
| [R008](#F-JUECES-R008) | Los motores del diálogo salen del catálogo; los demás apagados; un catálogo nuevo con el diálogo abierto vuelve a estimar, uno igual no | `html/test/lanzar-motores-habilitados.test.js` |
| [R009](#F-JUECES-R009) | Una sola lectura de `exam status` y `get analyzers` para todas las pantallas; el número de «jueces», la huella (una vez por segundo), cinco minutos y una falla que no se recuerda | `test/tablero_html/jueces/catalogo_de_jueces_test.dart`, `tablero_con_el_cli_test.dart`, `html/test/estado-jueces.test.js` |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 5.
- **Briefs**: 199 (diagnóstico y preguntas).
- **FEAT-CMOTOR**: con qué motor consulta el cierre a cada juez
  ([F-CMOTOR-R006](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md#F-CMOTOR-R006)).
- **FEAT-ALDIA**: la regla de «al día» y de «habilitada» ([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)).
- **FEAT-TALDIA**: cómo se muestran las marcas ([F-TALDIA-R001](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R001),
  [F-TALDIA-R004](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R004)).
- **FEAT-AVISO**: [F-JUECES-R005](#F-JUECES-R005) completa
  [F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002) para «jueces».
