---
feature:
  id: FEAT-PROBS
  code: F-PROBS
  name: Los problemas - una sola lista para Problemas y el cajón de Correcciones, una ficha que se lee sin escribir y una prueba que avisa sola
  priority: major
---

# Los problemas: una lista, y leer no escribe

## TL;DR

**Qué**: Los problemas entran al estado de la pestaña: la lista, y de cada problema que se mira, su inspección, sus pasos,
la prueba de la solución y lo que costaría probar. Problemas y el cajón de Correcciones (la tecla P) leen la misma lista.
Cada paso guarda el número de su corrida y pregunta a la tabla de corridas si corre, como Ejecuciones. Problemas deja de
preguntar cada 10 s: relee con el aviso «problemas». Leer una ficha nunca cambia un archivo. Probar la solución avisa
«problemas» cada vez que algo cambia. Problemas y el examen fijo leen el estado de cada juez del catálogo de jueces, sin
contarlo de nuevo.

**Por qué**: Hasta ahora había dos listas de problemas en la pestaña, cada una pedida por su lado: anotar con P no aparecía
en Problemas hasta 10 s después, y en otra pestaña de Correcciones, nunca. Leer los pasos de un problema guardaba trabajos
viejos y preparaba el examen fijo (hasta un minuto); abrir «Rendir» en Exámenes también. Probar la solución avanzaba cada
2 s en el servidor y nadie se enteraba hasta preguntar. Y Problemas contaba al juez por su cuenta: un juez habilitado
sólo con otro motor decía «Primero debe aprobar» mientras el resto de las pantallas decía «habilitada».

Fuente: la propuesta [«Un solo estado para el tablero»](../../docs/propuestas/estado-centralizado.html), etapa 8
«Problemas». El diseño aprobado está en el brief 206. Las decisiones son de José (24/10/2026):
- un juez habilitado sólo con otro motor no frena: el paso y Probar consultan con el motor con que content-audit lo
  consultaría al cerrar la tanda ([F-CMOTOR-R006](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md#F-CMOTOR-R006)),
  y la confirmación lo dice antes de gastar;
- el examen fijo se prepara al tocar Probar, un paso o «Rendir», no al leer.

El tablero es `pipeline/vocabulary_dashboard` en ittm. La estimación del costo de Probar entra al estado, pero cómo se
calcula es de la etapa 9; el resto de Correcciones, de la 7; las opiniones, de la 6.

## Recorrido de uso (estado actual)

1. José anota un problema con P en Correcciones ✓.
2. En otra pestaña tiene Problemas abierto: el problema aparece hasta 10 s después ⚠️. Una segunda pestaña de Correcciones
   no lo ve en su cajón ⚠️ hasta volver a abrirlo.
3. Abre la ficha: leerla prepara el examen fijo de la suite de hoy y guarda la medición de un trabajo viejo ⚠️. Tarda hasta
   un minuto la primera vez.
4. Prueba la solución: el servidor avanza cada 2 s, pero la ficha se entera cuando vuelve a preguntar ⚠️.
5. El juez está habilitado sólo con Gemini: Problemas frena con «Primero debe aprobar» ⚠️; Exámenes y el Mapa dicen
   «habilitada».

**Punto de fricción**: pasos 2 a 5. Cada pantalla guarda su copia de la lista y la pide a su ritmo, leer escribe archivos,
y Problemas cuenta al juez aparte.

**Cambio mínimo necesario**: que la pestaña lea los problemas una vez, por la puerta, que el servidor avise «problemas»
cada vez que algo cambia y nunca escriba al leer, y que Problemas lea al juez del catálogo.

## Reglas de Negocio

Un **problema** es una observación anotada sobre el contenido, con sus casos, en el registro del curso. Su **ficha** es lo
que se sabe de él (nombre, descripción, casos, estado y la entrega del agente); su **revisión** sube con cada cambio. Un
**paso** es una ejecución tipada de la entrega (un examen, una medición, un registro, un reanálisis), y su **trabajo**
es lo que el tablero guarda de él en `problem-runs/`, con el número de su **corrida**. **Probar la solución** es la prueba
de cinco etapas que el servidor sostiene en `problem-solutions/`. El **examen fijo** (examen permanente) es el examen
histórico de un juez con todos los casos confirmados: **prepararlo** arma su carpeta en `permanent-exams/`. **Leer** es
contestar un pedido que no cambia nada: la lista, una ficha, su inspección, sus pasos, la prueba o lo que costaría
rendir. El **aviso «problemas»** es el tema de las novedades del tablero ([F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002)).

<a id="F-PROBS-R001"></a>
### Rule[F-PROBS-R001] - Una sola lista de problemas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. La lista de problemas está una sola vez en la pestaña. Problemas y el cajón de Correcciones la leen de ahí: ninguna
>    pantalla guarda su propia copia de un problema.
> 2. Las dos la piden por el mismo camino; mirándola las dos a la vez, sale un solo pedido.
> 3. El cajón de Correcciones abre con la lista que ya hay, sin esperar una lectura.

<a id="F-PROBS-R002"></a>
### Rule[F-PROBS-R002] - Anotar un problema llega solo a todas las pantallas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Anotás un problema con P (o lo creás, editás, cambiás de estado o deshacés una anotación): la pestaña donde lo hiciste
>    lo tiene en la lista enseguida, sin releer.
> 2. El servidor avisa «problemas» y las otras pestañas releen la lista una vez, sólo si están a la vista; una oculta relee
>    al volver.
> 3. Lo mismo si el problema cambia desde la terminal (el agente que entrega, `problem` del CLI).

<a id="F-PROBS-R003"></a>
### Rule[F-PROBS-R003] - Problemas no pregunta a cada rato
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Problemas no tiene reloj propio. Con el canal de avisos abierto, relee con el aviso «problemas», al terminar la
>    corrida de un trabajo abierto ([F-CORRI-R007](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R007)) y al volver a
>    la pestaña.
> 2. Sin el canal (un tablero de antes o el canal cortado), pregunta cada 10 s, sólo con la pestaña a la vista, como antes.
> 3. Lo que ya se está leyendo no se vuelve a pedir: un aviso que llega en medio sale una sola vez, al terminar.

<a id="F-PROBS-R004"></a>
### Rule[F-PROBS-R004] - La ficha que mirás se lee por la puerta
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. De cada problema que se mira, la pestaña pide una vez su inspección, sus pasos (con lo que costaría probar) y la
>    prueba de la solución, y las guarda por la puerta, con el número del problema. Las pantallas no guardan nada de eso.
> 2. Si la ficha más nueva trae otra revisión que la inspección, la inspección se vuelve a pedir una vez.
> 3. Dejar de mirar un problema suelta sus lecturas.

<a id="F-PROBS-R005"></a>
### Rule[F-PROBS-R005] - Un paso guarda el número de su corrida
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Un paso y cada etapa de la prueba guardan el número de su corrida. Si corre, si quedó huérfana o si terminó lo dice la
>    tabla de corridas de la pestaña ([F-CORRI-R001](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R001)); el
>    trabajo sólo dice cómo quedó.
> 2. Termina la corrida de un paso: el paso y Ejecuciones dicen lo mismo en la misma lectura, y la ficha relee el
>    resultado sin esperar.
> 3. Si la pestaña no sabe nada de esa corrida, vale lo que dijo el servidor del trabajo.

<a id="F-PROBS-R006"></a>
### Rule[F-PROBS-R006] - Leer nunca escribe
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Leer la lista, una ficha, su inspección, sus pasos, la prueba de la solución o lo que costaría rendir el examen fijo
>    no cambia ningún archivo del tablero ni de la campaña.
> 2. Lo que le falta a un trabajo cerrado (la medición con la lectura de hoy, por qué falló) se calcula al leer y lo guarda
>    el vigilante de corridas, una sola vez. Leer tampoco cierra un trabajo: cierra sólo el vigilante
>    ([F-CORRE-R007](../2026-10-20.02_lo-que-corre/REQUIREMENT.md#F-CORRE-R007)).
> 3. El examen fijo se prepara al tocar Probar, un paso o «Rendir» en Exámenes. Mientras no está preparado, la ficha lo
>    dice («se prepara al probar, gratis») y no lo cuenta en lo que costaría probar; Probar y los pasos siguen habilitados.
> 4. «Rendir» en Exámenes es un pedido al tablero: prepara y estima, gratis.

<details><summary>Detail</summary>

content-audit tampoco escribe al leer en la práctica: `exam regressions` toma el candado del curso y reescribe su catálogo
sólo si le falta un problema confirmado, y una escritura de problema ya lo suma. Se verifica en la copia de la campaña con
el CLI de verdad; no se cambia content-audit.

</details>

<a id="F-PROBS-R007"></a>
### Rule[F-PROBS-R007] - Probar la solución avisa en cada paso
**Severity**: major | **Validation**: AUTO_VALIDATED

> 1. Cada vez que la prueba de una solución cambia (lanza una etapa, termina un tema o una etapa, frena, pausa, pide
>    revisión o termina), el servidor lo guarda y avisa «problemas», aunque ninguna pantalla lo esté mirando.
> 2. Una vuelta en la que nada cambió no reescribe el archivo ni avisa.
> 3. Cerrar un trabajo, lanzar un paso o volver a medir también avisa «problemas».

<a id="F-PROBS-R008"></a>
### Rule[F-PROBS-R008] - El juez se lee del catálogo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Problemas y el examen fijo leen el estado de cada juez del catálogo de jueces
>    ([F-JUECES-R002](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R002)), en el servidor y en la pestaña. Ninguno
>    lo vuelve a contar.
> 2. Un paso que pide examen aprobado frena con «Primero debe aprobar» sólo si la versión de hoy no está habilitada con
>    ningún motor.
> 3. Habilitada con el motor de siempre, los pasos y Probar consultan con él. Habilitada sólo con otro motor, consultan con
>    el motor con que content-audit consultaría a ese juez al cerrar la tanda; la confirmación nombra ese motor antes de
>    gastar. Si content-audit no dice ninguno, frena y dice con qué motores está habilitada.
> 4. El examen fijo de Exámenes rinde con el motor de siempre: si ya está habilitada con él, no hace falta rendir.

## Journeys

```yaml
journeys:
  - id: F-PROBS-J001
    name: Anotar un problema desde Correcciones
    flow:
      - id: anotar
        action: "José anota un problema con P en el cajón de Correcciones"
        gate: [F-PROBS-R001, F-PROBS-R002]
        outcomes:
          - when: "El tablero lo guarda"
            then: lista
      - id: lista
        action: "La lista de la pestaña lo tiene enseguida y el servidor avisa «problemas»"
        gate: [F-PROBS-R002]
        outcomes:
          - when: "Problemas está abierto en otra pestaña"
            then: problemas
      - id: problemas
        action: "Problemas relee la lista una vez y lo muestra, sin recargar"
        gate: [F-PROBS-R002, F-PROBS-R003]
        result: success
  - id: F-PROBS-J002
    name: Probar la solución
    flow:
      - id: ficha
        action: "José abre la ficha de un problema con su entrega"
        gate: [F-PROBS-R004, F-PROBS-R006]
        outcomes:
          - when: "El examen fijo de hoy no está preparado"
            then: probar
      - id: probar
        action: "Toca Probar: el tablero prepara el examen fijo y muestra la cuenta y el motor"
        gate: [F-PROBS-R006, F-PROBS-R008]
        outcomes:
          - when: "José confirma con un tope"
            then: avanza
      - id: avanza
        action: "Cada tema y cada etapa avisan «problemas»; la ficha muestra el avance sin preguntar"
        gate: [F-PROBS-R007, F-PROBS-R003]
        outcomes:
          - when: "Termina la corrida de una etapa"
            then: termina
      - id: termina
        action: "La etapa y Ejecuciones dicen «terminó» en la misma lectura"
        gate: [F-PROBS-R005]
        result: success
  - id: F-PROBS-J003
    name: Un juez habilitado sólo con otro motor
    flow:
      - id: paso
        action: "José prepara un paso que pide examen aprobado, con un juez habilitado sólo con Gemini"
        gate: [F-PROBS-R008]
        outcomes:
          - when: "content-audit lo consultaría con Gemini al cerrar"
            then: confirma
      - id: confirma
        action: "La confirmación dice que consulta con Gemini, sin «Primero debe aprobar»"
        gate: [F-PROBS-R008]
        result: success
```

## Open Questions

No quedan abiertas.

## Cómo se prueba

Ninguna prueba consulta a un modelo ni rinde un examen de verdad. Las del navegador están en `html/test/` (node --test,
con un servidor de mentira y el store de verdad); las del tablero, en `test/tablero_html/` (con carpetas de verdad y el
CLI falso `test/support/problem_cli.py`). Cada prueba nombra su regla en el título.

| Regla | Qué se comprueba | Pruebas |
|---|---|---|
| [R001](#F-PROBS-R001) | Ninguna pantalla (Problemas, Probar la solución, Correcciones con el cajón) guarda la lista, un problema, un trabajo, una inspección ni la prueba en ningún estado de su recorrido; sólo `js/estado/problemas.js` nombra `/api/problemas`; dos que miran la lista hacen un pedido; el cajón abre con la lista que hay, sin pedir; la ficha sigue lo que trae la lista | `html/test/ninguna-pantalla-guarda-un-problema.test.js`, `html/test/arquitectura.test.js`, `html/test/estado-problemas.test.js`, `html/test/problemas.test.js` |
| [R002](#F-PROBS-R002) | Anotar desde el cajón, crear o cambiar de estado desde Problemas: la lista la tiene sin releer, y una respuesta atrasada no la pisa; con el aviso, las otras pestañas a la vista releen y lo muestran, y una oculta, al volver; el servidor sube «problemas» con cada escritura de problemas y con un cambio desde la terminal | `html/test/estado-avisos.test.js`, `html/test/estado-problemas.test.js`, `html/test/problemas.test.js`, `test/tablero_html/novedades_test.dart` |
| [R003](#F-PROBS-R003) | Ni Problemas ni el cajón tienen reloj propio; con el canal abierto no pregunta; sin canal, cada 10 s (con reloj de mentira); 404 o sesión vencida, nunca; dos avisos en medio de una lectura salen una vez; «problemas» relee la lista | `html/test/estado-problemas.test.js`, `html/test/arquitectura.test.js`, `html/test/estado-avisos.test.js` |
| [R004](#F-PROBS-R004) | Mirar una ficha pide una vez su inspección y, si trae pasos que se ejecutan, sus pasos y la prueba; sin entrega no pide nada; otra revisión relee la inspección una vez; la ficha más nueva de los pasos entra a la lista; mirar otra suelta la anterior y volver la relee | `html/test/estado-problemas.test.js`, `html/test/ninguna-pantalla-guarda-un-problema.test.js` |
| [R005](#F-PROBS-R005) | El paso y la etapa de la prueba dicen la corrida de la tabla, el mismo objeto que Ejecuciones, también al terminar; con la pestaña de verdad, termina la corrida de un paso y la ficha relee sus pasos ya, una vez; si la pestaña no sabe nada, lo del trabajo | `html/test/corridas-de-los-trabajos.test.js` |
| [R006](#F-PROBS-R006) | Leer la lista, la ficha, la inspección, los pasos y la prueba no cambia ningún archivo (con un trabajo viejo sin medir, un examen fijo sin preparar y una prueba en curso); el vigilante guarda lo que faltaba una vez; leer no cierra un trabajo; leer no prepara el examen fijo y tocar Probar, un paso o «Rendir» sí, una vez aunque se pida dos; la ficha dice «se prepara al probar, gratis», no lo cuenta y deja Probar habilitado | `test/tablero_html/problemas/pasos_de_problemas_test.dart`, `test/tablero_html/examenes/examen_fijo_test.dart`, `html/test/probar-solucion.test.js`, `html/test/examenes-operaciones.test.js` |
| [R007](#F-PROBS-R007) | Probar avisa «problemas» al empezar, al cerrar un paso, al seguir, al frenar y al pedir revisión; una vuelta sin novedades no reescribe ni avisa; preparar el examen fijo avisa una vez; un trabajo guardado avisa; lanzar un paso, volver a medir, probar, seguir y frenar suben «problemas» | `test/tablero_html/problemas/pasos_de_problemas_test.dart`, `test/tablero_html/novedades_test.dart` |
| [R008](#F-PROBS-R008) | Habilitada con el de siempre, sólo con otro motor (con y sin `closesWith`) y sin aprobar: qué motor usa el paso, cuándo frena y qué dice; el examen fijo con el de siempre y «no hace falta rendir»; la página lee el juez de `elJuez` y ninguna pantalla lo guarda; la ficha y la cuenta de Probar nombran el motor antes de gastar | `test/tablero_html/problemas/pasos_de_problemas_test.dart`, `test/tablero_html/examenes/examen_fijo_test.dart`, `html/test/ninguna-pantalla-guarda-un-juez.test.js`, `html/test/problemas.test.js`, `html/test/probar-solucion.test.js` |

## References

- **Propuesta**: [Un solo estado para el tablero](../../docs/propuestas/estado-centralizado.html), etapa 8.
- **Brief**: 206 (el diseño aprobado) y 207 (hecho y probado en la copia).
- **FEAT-CORRI**: la tabla de corridas y el cierre por el registro ([F-CORRI-R007](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R007),
  [F-CORRI-R009](../2026-10-21.01_las-corridas/REQUIREMENT.md#F-CORRI-R009)).
- **FEAT-JUECES**: el catálogo ([F-JUECES-R002](../2026-10-24.02_los-jueces/REQUIREMENT.md#F-JUECES-R002)); la regla
  F-PROBS-R008 cierra el pendiente «Problemas y el examen fijo» de esa etapa.
- **FEAT-AVISO**: [F-PROBS-R002](#F-PROBS-R002) y [F-PROBS-R007](#F-PROBS-R007) completan
  [F-AVISO-R002](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R002) para «problemas».
- **FEAT-CMOTOR**: con qué motor consulta el cierre a cada juez ([F-CMOTOR-R006](../2026-10-24.01_cierre-con-cualquier-motor/REQUIREMENT.md#F-CMOTOR-R006)).
