---
feature:
  id: FEAT-RESUM
  code: F-RESUM
  name: Los resúmenes - las cuatro pantallas de resumen al día solas, la bandeja leída una vez y una sola estimación
  priority: major
---

# Los resúmenes: al día solos, la bandeja una vez y una sola estimación

## TL;DR

**Qué**: Estadísticas, Comparar, Vueltas y Publicar muestran su resumen desde el estado común de la pestaña y se
actualizan solas cuando cambia algo que muestran, sin «Releer». Al terminar una corrida, cada una ve lo anterior marcado
«actualizando» y en segundos lo nuevo, sin hacer esperar a las demás pantallas. Vueltas y Publicar leen la misma bandeja,
una vez. Toda estimación de lo que gasta una corrida —el diálogo de lanzar, Vueltas, Exámenes y Problemas— sale de un
solo cálculo: la misma pregunta da el mismo número en todas.

**Por qué**: Cada una de las cuatro pantallas guardaba su copia del resumen, tenía su botón para volver a leer y lo
volvía a pedir forzando al CLI cuando terminaba una corrida. Al terminar una, las pantallas ocupaban todas las
conexiones del navegador: el aviso siguiente esperó 6,3 s (FEAT-AVISO, 20/10). Vueltas y Publicar leían la bandeja cada
una por su lado. Cuatro lugares estimaban de cuatro maneras: Vueltas ignoraba el motor y la etapa «Examen fijo» de
Problemas contaba distinto que Exámenes.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 9
«Resúmenes». El diseño está en el brief 209. Las decisiones son de José (24/10/2026):
- Vueltas estima con el motor con que abre el diálogo de lanzar;
- el examen fijo se cuenta una vez, para Exámenes y para Problemas;
- la guardia de «releer» es un tope que sólo baja.

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. Termina una medición de jueces ✓.
2. Comparar, Vueltas, Publicar y Estadísticas vuelven a pedir lo suyo forzando al CLI ⚠️: Comparar tarda 7 s y Vueltas
   4,5 s, y entre todas ocupan las conexiones del navegador.
3. La franja de corridas quiere mostrar el aviso siguiente y espera 6,3 s ⚠️.
4. José decide un par desde Correcciones: Publicar no se entera ⚠️ hasta que José toca «Volver a leer».
5. José estima la próxima vuelta en Vueltas y después la lanza: el diálogo dice otro número ⚠️, porque Vueltas estimó sin
   el motor.

**Punto de fricción**: pasos 2 a 5. Cada pantalla guarda su resumen y lo relee a su manera, y cada una estima a su
manera.

**Cambio mínimo necesario**: que cada resumen viva en el estado común de la pestaña, que se pida sólo cuando cambia un
tema suyo, que el tablero lo lea una vez para todas las pestañas sin hacerlas esperar, y que haya un solo cálculo de la
estimación.

## Reglas de Negocio

Un **resumen** es lo que muestra una de las cuatro pantallas de resumen sobre la tanda que se mira: la vista de
Estadísticas (y el detalle de un tema del alumno), la comparación de Comparar, las vueltas de Vueltas y el estado de
Publicar; y la **bandeja**, lo que falta decidir antes de publicar, que muestran Vueltas y Publicar. Cada resumen depende
de unos **temas** del canal de avisos (la tanda, el análisis, los jueces, publicar). Una **estimación** es cuánto
gastaría una corrida: consultas, tope, tiempo y lo que la frena. Una corrida **está corriendo** mientras el registro de
actividad la tiene en curso, la haya lanzado el tablero o la terminal.

<a id="F-RESUM-R001"></a>
### Rule[F-RESUM-R001] - Un resumen se vuelve a pedir cuando cambia un tema suyo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada pantalla de resumen vuelve a pedir lo suyo, sin que nadie lo pida a mano, cuando:
>    - termina una corrida;
>    - cambia la tanda que se mira, o su estado, versión, corrección o análisis integrado;
>    - sube el número de uno de sus temas;
>    - el tablero avisa que un resumen que le dio como «actualizando» ya está;
>    - se vuelve a la pestaña y algo de lo anterior pasó mientras estaba oculta, o lo que tiene es de hace más de
>      5 minutos;
>    - se elige otra selección de opiniones del análisis vigente (sube «análisis»: mientras algo corre, cuenta al
>      terminar, como dice F-RESUM-R002).
> 2. Lo pide una vez aunque lleguen varios motivos juntos. Si llega un motivo mientras lo está pidiendo, lo pide una vez
>    más al terminar.
> 3. Una pestaña oculta no pide nada; al volver, pide lo que corresponda.
> 4. Qué temas tiene cada resumen lo dice el tablero en cada respuesta; la pantalla no tiene su propia lista.

<a id="F-RESUM-R002"></a>
### Rule[F-RESUM-R002] - El ruido de una corrida no hace volver a leer
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Mientras algo está corriendo, los avisos de «tanda» y «análisis» no hacen pedir un resumen: durante una medición
>    llegan hasta dos por segundo. Al terminar la corrida se pide una vez.
> 2. Sin nada corriendo, un aviso de «tanda» o «análisis» sí lo hace pedir: es algo que se hizo desde el tablero o la
>    terminal.
> 3. Mientras algo está corriendo, el tablero tampoco da por viejo un resumen por el ruido de la tanda o del análisis:
>    lo que tiene guardado sigue valiendo, aunque lo pidan muchas pestañas, hasta que la corrida termina. No hay un
>    ritmo fijo de lecturas.
> 4. Lo firme —decidir, cerrar la muestra ciega, publicar, cambiar un juez— lo pone viejo enseguida, aunque algo
>    corra.

<a id="F-RESUM-R003"></a>
### Rule[F-RESUM-R003] - Al terminar una corrida, nadie se queda esperando
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Si lo que el tablero tiene guardado de un resumen quedó viejo, contesta enseguida con eso, marcado «actualizando»,
>    y lo lee de nuevo aparte.
> 2. Lee cada resumen una sola vez para todas las pestañas que lo piden. Cuando termina, avisa «resumenes», y cada
>    pestaña que tenía ese resumen marcado lo vuelve a pedir y recibe lo nuevo.
> 3. La pantalla muestra lo anterior, con su lugar y lo elegido, marcado «actualizando», hasta que llega lo nuevo.
> 4. Si no tiene nada guardado (la primera vez), espera la lectura.
> 5. Al terminar una corrida con las cuatro pantallas de resumen abiertas, un aviso de corridas no espera más de un
>    segundo por una conexión.

<a id="F-RESUM-R004"></a>
### Rule[F-RESUM-R004] - Lo que nadie avisa vence a los 5 minutos
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Un resumen guardado en el tablero se vuelve a leer, aunque ningún tema suyo haya cambiado, si tiene más de 5
>    minutos: cubre lo que cambia en una carpeta que nadie vigila.
> 2. Una pestaña que vuelve a verse con un resumen de hace más de 5 minutos lo vuelve a pedir.

<a id="F-RESUM-R005"></a>
### Rule[F-RESUM-R005] - Sin «releer»
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Las cuatro pantallas de resumen no tienen botón para volver a leer. Si un pedido falla y no hay nada que mostrar,
>    ofrecen «Volver a intentar», que pide lo mismo de siempre.
> 2. Ningún pedido de un resumen ni de una estimación fuerza al tablero a leer de nuevo; si llega `releer`, se ignora.
> 3. Los archivos del navegador y del tablero que todavía mandan o leen `releer` están en una lista, con cuántas veces
>    y qué etapa los saca. La lista sólo puede bajar: un archivo nuevo, o uno con más, hace fallar las pruebas, y uno
>    con menos obliga a bajar la lista. Hoy quedan ocho, del Mapa, Analizadores, los cambios de Correcciones, el
>    análisis vigente y Evolución, y ninguna de las diez etapas los saca todavía: lo dicen así.

<a id="F-RESUM-R006"></a>
### Rule[F-RESUM-R006] - Ninguna pantalla guarda un resumen ni una estimación
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada resumen y cada estimación están una sola vez en la pestaña, en el estado común. Ninguna pantalla guarda su
>    copia.
> 2. Una sola parte del navegador pide los resúmenes y una sola pide las estimaciones.

<a id="F-RESUM-R007"></a>
### Rule[F-RESUM-R007] - La bandeja se lee una vez
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Vueltas y Publicar muestran la misma bandeja, leída una vez por el tablero para las dos.
> 2. Tiene una sola forma, con todo lo que usaban las dos.
> 3. Publicar ya no vuelve a leer la tanda entera para tener la bandeja.
> 4. Mirando una tanda que no es la de la bandeja, Vueltas y Publicar muestran el mismo mensaje: cuál es la tanda abierta
>    y cuál la por omisión. No es un error ([F-LATAN-R008](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R008)).

<a id="F-RESUM-R008"></a>
### Rule[F-RESUM-R008] - Decidir pone vieja la bandeja enseguida
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Decidir una corrección, desde el tablero o desde la terminal, sube «decisiones», que es un tema de Publicar, de
>    la bandeja y de las vueltas (que cuentan lo rechazado): Publicar y Vueltas se actualizan sin esperar.
> 2. Cerrar la muestra ciega, también.

<a id="F-RESUM-R009"></a>
### Rule[F-RESUM-R009] - La misma pregunta da el mismo número
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. El diálogo de lanzar, Vueltas, Exámenes y Problemas piden la estimación al mismo lugar, con la misma forma de
>    pregunta que usa lanzar.
> 2. La misma pregunta, desde dos pantallas, se calcula una vez y da el mismo número.
> 3. Sin motor elegido, se estima con el de siempre, y la estimación lo dice.
> 4. Toda estimación dice con qué motor se calculó y, de cada juez, con qué motor lo consultaría la corrida.
> 5. Estimar es gratis: no consulta modelos, no cambia nada que se muestre y anda en sólo lectura.

<a id="F-RESUM-R010"></a>
### Rule[F-RESUM-R010] - Vueltas estima como el diálogo
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Vueltas estima la próxima vuelta con el motor con que abre el diálogo de lanzar, y lo dice.
> 2. La estimación se pide al tocar «Estimar». Una vez pedida, se mantiene al día como la del diálogo: si cambian los
>    jueces, se vuelve a calcular.

<a id="F-RESUM-R011"></a>
### Rule[F-RESUM-R011] - El examen fijo se cuenta una vez
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Lo que falta juzgar del examen fijo de un juez se cuenta en un solo lugar. Exámenes y la etapa «Examen fijo» de
>    Problemas dicen el mismo número, también con trabajos a medias.
> 2. Los minutos que Problemas dice que tarda probar una solución salen de la estimación.

<a id="F-RESUM-R012"></a>
### Rule[F-RESUM-R012] - Lanzar sigue comprobando antes de gastar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Lanzar vuelve a estimar en el momento, sin usar lo guardado. Si el número cambió desde que se mostró, no lanza y
>    muestra la estimación nueva.
> 2. Una recorrección grande, de hasta 2.400 preguntas, se puede estimar y lanzar.

<a id="F-RESUM-R013"></a>
### Rule[F-RESUM-R013] - Una falla no borra lo que se ve
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Si volver a leer un resumen falla, la pantalla sigue mostrando lo que tenía y dice que no se pudo actualizar.
> 2. Si falla sin nada que mostrar, dice el error y ofrece «Volver a intentar».
> 3. Una falla se recuerda 5 segundos: los avisos de esos segundos no la reintentan en bucle.
> 4. Una respuesta de otra tanda que la que se mira no se muestra.

<a id="F-RESUM-R014"></a>
### Rule[F-RESUM-R014] - Estadísticas no pierde el lugar
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Cuando llega de nuevo el mismo análisis, Estadísticas conserva el lugar, lo elegido y lo resaltado.
> 2. Otro análisis arranca de cero.
> 3. Si el análisis que se miraba ya no existe, vuelve al más nuevo y lo avisa.

## Journeys

```yaml
journeys:
  - id: F-RESUM-J001
    name: Termina un cierre
    flow:
      - id: termina
        action: "Termina un cierre de la tanda con Estadísticas, Comparar, Vueltas y Publicar abiertas"
        gate: [F-RESUM-R001]
        outcomes:
          - when: "Cada pantalla pide su resumen una vez"
            then: contesta
      - id: contesta
        action: "El tablero contesta lo guardado, marcado «actualizando», y lee cada resumen una vez"
        gate: [F-RESUM-R003]
        outcomes:
          - when: "Termina de leer y avisa «resumenes»"
            then: nuevo
      - id: nuevo
        action: "Cada pantalla muestra el análisis nuevo sin que nadie toque «Releer», y la franja no esperó"
        gate: [F-RESUM-R003, F-RESUM-R005]
        result: success
  - id: F-RESUM-J002
    name: Estimar la próxima vuelta
    flow:
      - id: estimar
        action: "José toca «Estimar» en Vueltas"
        gate: [F-RESUM-R010]
        outcomes:
          - when: "Vueltas dice el número y el motor"
            then: lanzar
      - id: lanzar
        action: "José abre el diálogo de lanzar para esa vuelta"
        gate: [F-RESUM-R009]
        outcomes:
          - when: "El diálogo dice el mismo número, sin calcular otra vez"
            then: lanza
      - id: lanza
        action: "Lanza: el tablero vuelve a estimar y, como no cambió, la corrida arranca"
        gate: [F-RESUM-R012]
        result: success
  - id: F-RESUM-J003
    name: Decidir y publicar
    flow:
      - id: decide
        action: "José decide un par en Correcciones con Publicar abierta en otra pestaña"
        gate: [F-RESUM-R008]
        outcomes:
          - when: "El tablero avisa «publicar»"
            then: publicar
      - id: publicar
        action: "Publicar y la bandeja de Vueltas muestran lo que queda por decidir, sin releer"
        gate: [F-RESUM-R001, F-RESUM-R007]
        result: success
```

## Open Questions

- (respondida por José, 24/10) Al terminar una corrida, ¿se ve lo anterior marcado «actualizando» hasta tener lo
  nuevo? Sí (F-RESUM-R003).
- (respondida por José, 24/10) El «releer» de Exámenes y del catálogo de jueces, ¿se saca en esta etapa? Sí: Exámenes
  se pone al día sola y sólo ofrece «Volver a intentar» cuando falla (F-RESUM-R005).
- (pendiente de José) Los ocho archivos que todavía mandan o leen `releer` (F-RESUM-R005 inv. 3), ¿en qué etapa salen?

## Cómo se prueba

Ninguna prueba consulta a un modelo: las estimaciones son gratis y las corridas se simulan. Las del navegador están en
`html/test/` (node --test, con un servidor de mentira y el store de verdad); las del tablero, en `test/tablero_html/`
(con carpetas de verdad y un CLI falso). Cada prueba nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-RESUM-R001) | Cada pantalla pide su resumen una vez al saber la tanda, al terminar una corrida y con un aviso de un tema suyo; uno ajeno no; varios motivos juntos, una vez | `estado-resumenes`, `comparar-resumen`, `estadisticas-operaciones`, `vueltas-operaciones`, `publicar`; `memoria_de_un_resumen_test`, `rutas_de_los_resumenes_test` |
| [R002](#F-RESUM-R002) | Mientras algo corre, tanda, análisis y corridas no hacen pedir ni ponen viejo lo guardado; lo firme sí; al terminar, una vez | `estado-resumenes`, `estado-estimacion`, `vueltas-operaciones`, `publicar`; `memoria_de_un_resumen_test`, `rutas_de_los_resumenes_test`, `rutas_de_la_estimacion_test` |
| [R003](#F-RESUM-R003) | Lo viejo se contesta marcado «actualizando» y se lee una vez de fondo; al terminar avisa «resumenes»; la pantalla muestra lo anterior en su lugar | `memoria_de_un_resumen_test`, `rutas_de_los_resumenes_test`, `novedades_test`; `estado-resumenes`, `estado-avisos`, `estadisticas-*`, `comparar-*`, `vueltas-operaciones` |
| [R004](#F-RESUM-R004) | Lo guardado vence a los 5 minutos en el tablero; al volver a la pestaña, lo de más de 5 minutos se vuelve a pedir | `memoria_de_un_resumen_test`; `estado-resumenes` |
| [R005](#F-RESUM-R005) | Ninguna pantalla de resumen ni Exámenes ofrece «Volver a leer», tampoco en su ayuda; «Volver a intentar» pide lo mismo; el tablero ignora `releer`; la lista que sólo baja | `sin-releer`, `ayuda-sin-releer`, `publicar-vistas`, `vueltas-vistas`, `examenes-*`; `rutas_de_los_resumenes_test`, `rutas_de_examenes_test`, `rutas_de_jueces_test`, `tablero_con_el_cli_test` |
| [R006](#F-RESUM-R006) | Cada resumen y cada estimación están una vez en `calculos`; ninguna pantalla guarda su copia en ningún estado de su recorrido; un solo archivo pide cada ruta | `ninguna-pantalla-guarda-un-resumen`, `arquitectura`, `estado-resumenes`, `estado-estimacion`, `lanzar-operaciones` |
| [R007](#F-RESUM-R007) | La bandeja se lee una vez para Vueltas y Publicar, con una sola forma; otra tanda da el mismo mensaje en las dos | `rutas_de_los_resumenes_test`, `estado_de_publicar_test`, `vista_de_vueltas_test`; `vueltas-*` |
| [R008](#F-RESUM-R008) | Decidir y cerrar la muestra ciega ponen vieja la bandeja y Publicar la vuelve a pedir; lo decidido llega a Vueltas | `rutas_de_los_resumenes_test`, `rutas_de_publicar_test`, `tablero_con_el_cli_test` |
| [R009](#F-RESUM-R009) | Diálogo, Vueltas, Exámenes y Problemas estiman en `/api/estimar`; la misma pregunta es la misma entrada; la estimación dice el motor | `estado-estimacion`, `lanzar-operaciones`, `vueltas-estado`, `examenes-operaciones`, `problemas`, `arquitectura`; `rutas_de_la_estimacion_test` |
| [R010](#F-RESUM-R010) | Vueltas estima con la pregunta del diálogo al tocar «Estimar» y se mantiene al día con los mismos temas que el tablero | `estado-estimacion`, `vueltas-*`; `rutas_de_la_estimacion_test` |
| [R011](#F-RESUM-R011) | Exámenes y Problemas cuentan igual lo que falta del examen fijo, también con trabajos a medias; los minutos salen de la estimación | `conteo_del_examen_fijo_test`; `probar-solucion` |
| [R012](#F-RESUM-R012) | Lanzar estima de nuevo sin lo guardado; una recorrección de 2.400 preguntas se estima y se lanza | `contrato_de_punta_a_punta_test`, `rutas_de_la_estimacion_test`, `servidor_del_tablero_test` |
| [R013](#F-RESUM-R013) | Una falla deja lo que se veía con el error; sin nada, «Volver a intentar»; se recuerda 5 s; otra tanda no se muestra | `memoria_de_un_resumen_test`, `rutas_de_los_resumenes_test`; `estado-resumenes`, `estado-estimacion`, `estadisticas-*`, `comparar-*`, `vueltas-*`, `publicar*` |
| [R014](#F-RESUM-R014) | El mismo análisis con otros números conserva lugar, lo elegido y lo resaltado; otro arranca de cero; uno que ya no está vuelve al más nuevo y avisa | `estadisticas-operaciones`, `estadisticas-estado` |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 9.
- **Brief**: 209 (diseño).
- **FEAT-AVISO**: el canal de avisos y la cola al terminar una corrida
  ([F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002)).
- **FEAT-LATAN**: la tanda que se mira ([F-LATAN-R008](../2026-10-21.02_la-tanda/REQUIREMENT.md#F-LATAN-R008)).
- **FEAT-JUECES**: lo que dice cada veredicto se vuelve a leer cuando cambia el catálogo
  ([F-JUECES-R005](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R005), inv. 5).
- **FEAT-CMOTOR**: con qué motor consulta la corrida a cada juez
  ([F-CMOTOR-R006](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md#F-CMOTOR-R006)).
- **FEAT-TALDIA**: el diálogo abre con un motor habilitado
  ([F-TALDIA-R010](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R010)).
