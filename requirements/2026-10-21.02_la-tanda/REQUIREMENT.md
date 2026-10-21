---
feature:
  id: FEAT-LATAN
  code: F-LATAN
  name: La tanda - se lee una vez para las diez pantallas, y cada pestaña mira la suya, en el link
  priority: major
---

# La tanda: una sola lectura, y cada pestaña mira la suya

## TL;DR

**Qué**: La tanda se lee una sola vez por pestaña y la usan las diez pantallas, la barra lateral y el diálogo de lanzar.
La tanda que mirás es de la pestaña y va en el link (`?tanda=`): elegir una tanda vieja en Correcciones ya no cambia lo
que ve otra pestaña. El encabezado dice lo mismo en todas: «Tanda A1 · Corrección 1 · versión 8 · cerrada». Las diez
pantallas muestran la tanda en la barra lateral. En el tablero, un solo lector de la tanda, por número, contesta sin
llamar al CLI y lee de cada archivo sólo lo justo. Comparar y Analizadores le piden la tanda a ese lector. El orden del
curso tiene versión y se vuelve a leer cuando cambia.

**Por qué**: Hasta ahora la tanda se leía en once lugares de ocho pantallas, cada uno a su momento y con su forma. La
tanda elegida en Correcciones era una sola para todo el tablero: Publicar, en otra pestaña, pasaba a mirar ésa sin
avisar. Para la misma tanda, el Mapa decía «Corrección 1» y Vueltas «vuelta 2». Comparar y Analizadores leían enteros
los 300 MB del archivo de la tanda y no se enteraban si cambiaba. El orden del curso no se volvía a leer nunca: después
de publicar seguía el viejo hasta reiniciar el tablero.

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 4 «La
tanda». El diseño aprobado está en el brief 179. Las decisiones son de José (21/10/2026):
- la tanda elegida es de cada pestaña, en el link;
- el encabezado dice «Corrección N · versión V», y Vueltas deja de sumar 1;
- una marca común en la barra lateral de las diez pantallas;
- con una tanda vieja, todas las pantallas muestran ésa, y lanzar, medir y estimar se apagan;
- sin elegir, se mira la abierta y, si no hay, la última publicada, como el CLI;
- un link viejo a «las corridas de la tanda X» mira X sin filtrar.

El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

1. José abre Publicar en una pestaña: muestra la tanda abierta ✓.
2. En otra pestaña, en Correcciones, elige una tanda vieja del historial ✓.
3. Vuelve a la pestaña de Publicar y recarga: muestra la tanda vieja ⚠️, sin que nada lo diga.
4. El Mapa dice «Corrección 1» y Vueltas, «vuelta 2», para la misma tanda ⚠️.
5. Cierra la tanda desde la terminal: cada pantalla lo dice cuando le toca releer ⚠️; Estadísticas, Ejecuciones,
   Exámenes y Problemas no dicen qué tanda es.
6. Publica la tanda desde la terminal: Correcciones sigue ordenando los ejercicios con el orden de antes ⚠️ hasta
   reiniciar el tablero.

**Punto de fricción**: pasos 3 a 6. Cada pantalla lee y guarda su propia tanda, y la tanda elegida es del tablero, no de
quien la mira.

**Cambio mínimo necesario**: una sola lectura de la tanda por pestaña, una tanda elegida por pestaña, en el link, y un
solo lector en el tablero.

## Reglas de Negocio

Una **tanda** es una corrección del curso, con su nivel, su estado (abierta, corriendo, interrumpida, cerrada o
publicada), su **número de corrección** (el que le pone content-audit al abrirla; otra vuelta no lo cambia) y su
**versión** del contenido (que sube con cada vuelta). La **tanda por omisión** es la abierta y, si no hay ninguna, la
última publicada. **La tanda que mirás** es la que eligió la pestaña o, si no eligió ninguna, la tanda por omisión. Una
**tanda vieja** es una tanda que mirás y que no es la tanda por omisión. **Lo que leyó una pantalla** es lo suyo
(correcciones, el mapa, las vueltas…) con el número y la versión de la tanda con que lo leyó.

<a id="F-LATAN-R001"></a>
### Rule[F-LATAN-R001] - La tanda se lee una vez, para todas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Cada pestaña pide la tanda una sola vez y la usan las diez pantallas, la barra lateral y el diálogo de lanzar.
>    Ninguna otra parte del navegador la pide.
> 2. El tablero la contesta sin llamar al CLI: dice todas las tandas de la campaña, cada una con su nivel, estado,
>    número de corrección y versión, y cuál es la tanda por omisión.
> 3. Contestar cuesta poco: de cada archivo de tanda se lee lo justo, y sólo cuando el archivo cambió. Un archivo que no
>    se puede leer se nombra aparte y no tapa a los demás.
> 4. La tanda por omisión es la abierta, la última por fecha de apertura entre las que no están publicadas. Si todas
>    están publicadas, es la última publicada. Es la misma regla del CLI.

<a id="F-LATAN-R002"></a>
### Rule[F-LATAN-R002] - Cada pestaña mira la suya
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Elegir una tanda en una pestaña cambia sólo esa pestaña. Las otras siguen mirando la suya.
> 2. El tablero no guarda ninguna tanda elegida: cada lectura dice qué tanda quiere, y sin decirlo recibe la tanda por
>    omisión.
> 3. Dos pestañas que miran tandas distintas en Correcciones deciden cada una sobre la suya, sin pisarse ni obligar a la
>    otra a releer.

<a id="F-LATAN-R003"></a>
### Rule[F-LATAN-R003] - La tanda va en el link
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. La tanda que mirás va en la dirección de la página (`?tanda=<id>`): recargar la mantiene y mandar el link abre la
>    misma. Si la pestaña no eligió ninguna, la dirección no la nombra.
> 2. Los links de la barra lateral la llevan: pasar de pantalla sigue en la misma tanda.
> 3. Atrás y adelante llevan a la tanda que dice la dirección.
> 4. Un link a una tanda que no existe muestra la tanda por omisión y dice que ésa no existe. Lo que no es un número de
>    tanda válido se descarta.

<a id="F-LATAN-R004"></a>
### Rule[F-LATAN-R004] - El mismo número en todas las pantallas
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Todas las pantallas nombran la tanda igual: su nivel, «Corrección N», «versión V» y su estado.
> 2. N es el número de content-audit; una tanda guardada sin número es la corrección 1. Ninguna pantalla le suma ni le
>    resta.
> 3. En Vueltas, cada análisis se nombra por su versión y su fecha.

<a id="F-LATAN-R005"></a>
### Rule[F-LATAN-R005] - La barra lateral dice qué tanda mirás
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Las diez pantallas muestran en la barra lateral la tanda que mirás: nivel, corrección y estado.
> 2. Con una tanda vieja, la barra lo marca y una línea arriba lo dice en las diez, con un botón para volver a la tanda
>    por omisión.

<a id="F-LATAN-R006"></a>
### Rule[F-LATAN-R006] - Se pone al día con el aviso
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Con cada aviso de «tanda», la pestaña relee la tanda una vez; si lo que contesta es igual, no se redibuja nada.
> 2. Si la tanda se cierra desde la terminal, las diez pantallas cambian el encabezado y la barra con esa misma lectura.
> 3. Cuando cambia la tanda que mirás (otra tanda, otro estado, otra versión u otra corrección), la pantalla vuelve a
>    leer lo suyo. Si no cambió, no.
> 4. Cuando el tablero cierra una corrida que terminó, avisa también «tanda».

<details><summary>Detail</summary>

Completa [F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002) para el tema «tanda»: hasta
esta etapa, un aviso de «tanda» no hacía releer nada.

</details>

<a id="F-LATAN-R007"></a>
### Rule[F-LATAN-R007] - Ninguna pantalla guarda una tanda
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Ninguna pantalla guarda el estado ni el nivel de una tanda, ni muestra una corrección propia: los toma de la tanda
>    que mirás.
> 2. Lo que leyó una pantalla lleva sólo su sello: el número y la versión de la tanda con que lo leyó. Correcciones
>    suma la corrección con que leyó y manda las tres con cada decisión, para no decidir sobre algo que no se vio.
> 3. Una guardia falla si una pantalla guarda una tanda propia: su número con su estado, su nivel o su historial.

<a id="F-LATAN-R008"></a>
### Rule[F-LATAN-R008] - Con una tanda vieja, todo muestra ésa y nada gasta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Correcciones, el Mapa, Vueltas, Comparar, Analizadores y Publicar muestran la tanda que mirás, también si es vieja.
> 2. Con una tanda vieja, lanzar, medir y estimar quedan apagados, y su (!) dice que mirás una tanda vieja.
> 3. La bandeja y la estimación son de la tanda por omisión: pedidas para otra tanda, el tablero dice por qué no, sin
>    llamar al CLI.
> 4. Decidir en Correcciones sigue como antes: frena sólo si la tanda está publicada.

<a id="F-LATAN-R009"></a>
### Rule[F-LATAN-R009] - Un lector de la tanda, por número
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Comparar y Analizadores piden la tanda por su número al mismo lector del tablero; ninguna pregunta al CLI cuál es.
> 2. El archivo de la tanda se lee entero una vez por cada cambio, para las dos.
> 3. Si el archivo cambió, la próxima lectura lo nota sola, sin pedir releer.

<a id="F-LATAN-R010"></a>
### Rule[F-LATAN-R010] - El orden del curso tiene versión
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. El orden del curso se vuelve a leer cuando cambian los archivos de los que sale: al publicar, sin reiniciar el
>    tablero.
> 2. Mientras no cambien, se lee una sola vez.

<a id="F-LATAN-R011"></a>
### Rule[F-LATAN-R011] - Ejecuciones filtra por la tanda que mirás
**Severity**: minor | **Validation**: AUTO_VALIDATED

> 1. «Sólo las corridas de esta tanda» filtra por la tanda que mirás, y va en la dirección (`?solo=tanda`).
> 2. Un link viejo a las corridas de una tanda (`?tanda=X`, sin `solo`) mira la tanda X sin filtrar.

<details><summary>Detail</summary>

Afina [F-CORRI-R003](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R003): el filtro por tanda deja de ser una
tanda propia de Ejecuciones y pasa a ser «la tanda que mirás».

</details>

## Journeys

```yaml
journeys:
  - id: F-LATAN-J001
    name: Mirar una tanda vieja sin cambiar otra pestaña
    flow:
      - id: elegir
        action: "José elige una tanda vieja en Correcciones"
        gate: [F-LATAN-R002, F-LATAN-R003]
        outcomes:
          - when: "La pestaña pasa a la tanda vieja"
            then: vieja
      - id: vieja
        action: "La barra y la línea de arriba dicen que mira una tanda vieja; lanzar y medir se apagan"
        gate: [F-LATAN-R005, F-LATAN-R008]
        outcomes:
          - when: "José mira Publicar en otra pestaña"
            then: otra
      - id: otra
        action: "Publicar sigue en la tanda abierta"
        gate: [F-LATAN-R002]
        result: success
  - id: F-LATAN-J002
    name: Se cierra la tanda desde la terminal
    flow:
      - id: cerrar
        action: "Alguien cierra la tanda desde la terminal"
        gate: [F-LATAN-R006]
        outcomes:
          - when: "El tablero avisa «tanda»"
            then: releer
      - id: releer
        action: "Cada pestaña relee la tanda una vez"
        gate: [F-LATAN-R001, F-LATAN-R006]
        outcomes:
          - when: "La tanda pasó a cerrada"
            then: encabezado
      - id: encabezado
        action: "Las diez pantallas cambian el encabezado y la barra, y cada una relee lo suyo"
        gate: [F-LATAN-R004, F-LATAN-R005, F-LATAN-R006]
        result: success
  - id: F-LATAN-J003
    name: Abrir un link con una tanda vieja
    flow:
      - id: abrir
        action: "José abre un link con ?tanda= de una tanda vieja"
        gate: [F-LATAN-R003]
        outcomes:
          - when: "La tanda existe"
            then: muestra
          - when: "La tanda no existe"
            then: no-existe
      - id: muestra
        action: "La pantalla muestra esa tanda; recargar y pasar de pantalla la mantienen"
        gate: [F-LATAN-R003, F-LATAN-R008]
        result: success
      - id: no-existe
        action: "Muestra la tanda por omisión y dice que ésa no existe"
        gate: [F-LATAN-R003]
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
| R001 | Una lectura por pestaña para las diez pantallas, la barra y el diálogo de lanzar; sólo `js/estado/tanda.js` nombra la ruta; `GET /api/tanda` sin CLI, con nivel, estado, corrección, versión, la abierta y la por omisión (la regla del CLI); el escáner lee el principio y el final del archivo (sangrías, claves dentro de textos, ventanas que crecen, archivos viejos y cortados); cada archivo se escanea una vez por cambio; un ilegible se nombra aparte | `html/test/estado-tanda.test.js`, `html/test/estado-conectar.test.js`, `html/test/la-tanda.test.js`, `html/test/arquitectura.test.js`, `test/tablero_html/tanda/escaner_de_la_tanda_test.dart`, `test/tablero_html/tanda/resumen_de_la_tanda_test.dart`, `test/tablero_html/tanda/lector_de_la_tanda_test.dart`, `test/tablero_html/tanda/rutas_de_tanda_test.dart`, `test/tablero_html/tablero_con_el_cli_test.dart` |
| R002 | Dos pestañas: elegir en una no cambia la otra; elegir no le pide nada al servidor y la ruta para elegir ya no existe; el menú de Correcciones elige para la pestaña y ofrece volver; una lectura que llega tarde no se muestra; con decisiones sin guardar no se cambia; dos pestañas deciden cada una sobre su tanda sin pisarse | `html/test/estado-tanda.test.js`, `html/test/otra-tanda.test.js`, `html/test/pedidos.test.js`, `html/test/publicar.test.js`, `html/test/arquitectura.test.js`, `test/tablero_html/correcciones/dos_pestanas_test.dart`, `test/tablero_html/correcciones/rutas_de_correcciones_test.dart`, `test/tablero_html/servidor_del_tablero_test.dart` |
| R003 | Un link con una tanda vieja abre ésa y recargar la mantiene; elegir escribe la dirección, atrás y adelante la siguen; una que no existe se reemplaza y la franja lo dice; los links de la barra y de la página la llevan; sólo `lib/direccion-de-la-tanda.js` lee y escribe el parámetro; cada pantalla pide lo suyo con `?tanda=` y el servidor lo acepta en todas sus rutas | `html/test/estado-tanda.test.js`, `html/test/ejecuciones-direccion.test.js`, `html/test/api.test.js`, `html/test/otra-tanda.test.js`, `html/test/mapa-operaciones.test.js`, `html/test/vueltas-operaciones.test.js`, `html/test/comparar-operaciones.test.js`, `html/test/analizadores-estado.test.js`, `html/test/publicar.test.js`, `test/tablero_html/mapa/rutas_de_mapa_test.dart`, `test/tablero_html/vueltas/rutas_de_vueltas_test.dart`, `test/tablero_html/comparar/rutas_de_comparar_test.dart`, `test/tablero_html/analizadores/rutas_de_analizadores_test.dart`, `test/tablero_html/publicar/rutas_de_publicar_test.dart` |
| R004 | «Tanda A1 · Corrección N · versión V · estado» en el encabezado y la ayuda de cada pantalla; sin número es la corrección 1; Vueltas ya no suma 1 y nombra cada análisis por su versión y su fecha; el filtro de Ejecuciones nombra la tanda igual | `html/test/la-tanda.test.js`, `html/test/vistas.test.js`, `html/test/otra-tanda.test.js`, `html/test/mapa-vistas.test.js`, `html/test/vueltas-vistas.test.js`, `html/test/comparar-vistas.test.js`, `html/test/analizadores-vistas.test.js`, `html/test/publicar-vistas.test.js`, `html/test/ejecuciones-vistas.test.js`, `test/tablero_html/tanda/resumen_de_la_tanda_test.dart`, `test/tablero_html/vueltas/vista_de_vueltas_test.dart`, `test/tablero_html/mapa/vista_del_mapa_test.dart` |
| R005 | La marca de la barra dice nivel, corrección y el color del estado; con una vieja se marca, la franja lo dice y «Volver» deja la pestaña sin elegida | `html/test/estado-tanda.test.js`, `html/test/la-tanda.test.js` |
| R006 | Un aviso de «tanda» relee la tanda una vez y no lo que corre; una respuesta igual no cambia nada; cerrada desde la terminal, las diez pantallas y su barra cambian con esa lectura; otra versión hace releer; lo que cambia mientras se lee se vuelve a leer con la de ahora; el vigilante avisa «tanda» al cerrar una corrida | `html/test/estado-tanda.test.js`, `html/test/estado-avisos.test.js`, `html/test/vueltas-operaciones.test.js`, `html/test/comparar-operaciones.test.js`, `html/test/analizadores-estado.test.js`, `html/test/publicar.test.js`, `test/tablero_html/ejecuciones/aviso_del_vigilante_test.dart` |
| R007 | Guardia que recorre todos los estados de nueve pantallas y falla si lo suyo guarda una tanda (número con estado, nivel o historial); otra que falla si el código lee el estado, el nivel o el historial de lo leído; Correcciones guarda número, versión y corrección, y anotar un problema lleva ese sello | `html/test/ninguna-pantalla-guarda-una-tanda.test.js`, `html/test/arquitectura.test.js`, `html/test/otra-tanda.test.js`, `html/test/comparar-estado.test.js`, `html/test/analizadores-estado.test.js`, `html/test/mapa-estado.test.js`, `html/test/vueltas-estado.test.js`, `html/test/publicar.test.js` |
| R008 | Con una tanda vieja cada pantalla muestra ésa; lanzar, medir y estimar se apagan con su (!) en cada botón y en el diálogo de lanzar; la bandeja y la estimación de otra tanda dan 409 sin CLI; con la tanda publicada, los mismos frenos; decidir frena sólo con la publicada | `html/test/la-tanda.test.js`, `html/test/boton-de-lanzar.test.js`, `html/test/lanzar-operaciones.test.js`, `html/test/otra-tanda.test.js`, `html/test/conversion.test.js`, `html/test/mapa-gestos.test.js`, `html/test/vueltas-vistas.test.js`, `html/test/comparar-gestos.test.js`, `html/test/analizadores-gestos.test.js`, `html/test/analizadores-vistas.test.js`, `html/test/publicar-vistas.test.js`, `test/tablero_html/vueltas/rutas_de_vueltas_test.dart`, `test/tablero_html/publicar/rutas_de_publicar_test.dart`, `test/tablero_html/ejecuciones/lanzador_test.dart`, `test/tablero_html/ejecuciones/estimacion_test.dart` |
| R009 | Comparar y Analizadores piden la tanda por número al lector, sin CLI; el archivo se lee entero una vez por cambio para las dos; dos tandas, una después de otra; si el archivo cambió lo nota solo | `test/tablero_html/tanda/lector_de_la_tanda_test.dart`, `test/tablero_html/comparar/rutas_de_comparar_test.dart`, `test/tablero_html/analizadores/rutas_de_analizadores_test.dart`, `test/tablero_html/analizadores/recorte_de_analizadores_test.dart`, `test/tablero_html/tablero_con_el_cli_test.dart` |
| R010 | La versión del orden cambia cuando cambia un archivo del que sale; con otra versión se vuelve a leer y se rearma la vista; sin cambios, una sola lectura | `test/tablero_html/orden_del_curso_test.dart`, `test/tablero_html/revision_compartida_test.dart` |
| R011 | «Sólo esta tanda» filtra por la que mirás y va como `?solo=tanda`; un link viejo `?tanda=X` mira X sin filtrar | `html/test/ejecuciones-direccion.test.js`, `html/test/ejecuciones-operaciones.test.js`, `html/test/ejecuciones-selectores.test.js`, `html/test/corridas.test.js` |

Además, en una copia de verificación de la campaña (sin modelos, el 21/10, con una tanda vieja publicada armada a
mano): un link con la tanda vieja la abrió en Correcciones con la barra marcada y la franja, y recargar la mantuvo; con
cuatro pestañas en la tanda por omisión (Publicar, Mapa, Vueltas y Comparar) y una en la vieja, poner la tanda en curso,
cerrarla y publicarla desde la terminal cambió el encabezado de las cuatro en menos de diez segundos, y la de la tanda
vieja no se movió. Detalle en el brief 180.

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 4.
- **Briefs**: 179 (el diseño aprobado) y 180 (lo hecho).
- **FEAT-AVISO**: [F-LATAN-R006](#F-LATAN-R006) completa
  [F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002) para «tanda».
- **FEAT-CORRI**: [F-LATAN-R011](#F-LATAN-R011) afina
  [F-CORRI-R003](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R003).
- **FEAT-TANDA**: las tandas por nivel ([2026-09-30.24_tandas-por-nivel](../2026-09-30.24_tandas-por-nivel/REQUIREMENT.md)).
