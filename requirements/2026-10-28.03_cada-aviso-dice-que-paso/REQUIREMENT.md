---
feature:
  id: FEAT-DICE
  code: F-DICE
  name: Cada aviso dice qué pasó - la pasada, el «resuelto» y tus aprobaciones sin todos los jueces
  priority: major
---

# Cada aviso dice qué pasó

## TL;DR

**Qué**: El aviso de una pasada dice qué la paró y qué le pasó a este ejercicio. El «resuelto» de un cambio dice si su
ejercicio está en «Todos los jueces lo aprueban» y, si no, por qué. Y desde «Vos lo aprobás», un botón devuelve a «sin
revisar» todos los ejercicios que aprobaste y que hoy no aprueban todos los jueces.

**Por qué**: El 28/10/2026, José vio en la C2 de A1 «Pasada interrumpida: revisá los cambios guardados» y no entendió
qué se había interrumpido. Era la pasada «Volver a corregir» del 26/10, que paró al llegar a su tope: gastó sus 5.500
consultas, y 302 de sus 348 ejercicios quedaron con un cambio. Tampoco entendió por qué un cambio que «Lo dan por
bueno» no está en «Todos los jueces lo aprueban»: una lista cuenta cambios y mira sólo al juez que lo pidió, la otra
cuenta ejercicios enteros (brief 237). José: «Si falta algo o algún juez no lo ve bien, debería ser claro». Y sobre
«Vos lo aprobás»: «prefiero que si no están todos los jueces de acuerdo, que no cuente como que yo lo estoy
aprobando», con un botón para devolverlos de una vez.

## Recorrido de uso (estado actual)

1. José abre un ejercicio de la pasada del 26/10 en Correcciones ✓.
2. ⚠️ El aviso dice «Pasada interrumpida» sin decir qué la cortó ni si este ejercicio quedó corregido.
3. José filtra «Lo dan por bueno» y abre un cambio ✓.
4. ⚠️ El chip dice «resuelto», pero el ejercicio no aparece en «Todos los jueces lo aprueban», y nada dice por qué.
5. José abre «Vos lo aprobás»: ejercicios que aprobó cuando los jueces los aprobaban ✓.
6. ⚠️ Para sacarles la aprobación tiene que abrirlos de a uno.

**Punto de fricción**: pasos 2, 4 y 6. Lo que José necesita saber para decidir no está a la vista.

**Cambio mínimo necesario**: que el aviso de la pasada diga la causa y el resultado para el ejercicio; que el
«resuelto» diga dónde quedó el ejercicio; y un botón que devuelva tus aprobaciones sin todos los jueces.

## Reglas de Negocio

<a id="F-DICE-R001"></a>
### Rule[F-DICE-R001] - El aviso de una pasada dice qué la paró y qué le pasó a este ejercicio
**Severity**: major | **Validation**: VALIDATED

> Cuando una pasada no terminó, el aviso del ejercicio dice dos cosas, con la fecha de la pasada.
>
> **Qué la paró:**
> - si gastó todo su tope: «La pasada del 26/10 paró al llegar a su tope: gastó 5.500 de 5.500 consultas»;
> - si la paró el tope de la corrección: «paró al llegar al tope de consultas de la corrección»;
> - si se canceló: «se canceló»;
> - si falló: «falló», con el motivo;
> - si no: «se cortó antes de terminar», con el motivo si lo hay.
>
> **Qué le pasó a este ejercicio:** «Este ejercicio quedó con un cambio de esa pasada» (o con N cambios), o «Este
> ejercicio no tiene ningún cambio de esa pasada». Sólo cuenta un cambio cuyo motivo es una tarea de la pasada y de
> este ejercicio ([F-MOTIVO-R002](../2026-10-28.02_cada-cambio-con-sus-motivos/REQUIREMENT.md#F-MOTIVO-R002)).
>
> En «Comparar una pasada», el estado de una pasada que gastó su tope se dice «Paró en su tope», no «Interrumpida».
> La cabecera dice qué la paró con las mismas palabras que el aviso, en lugar del motivo crudo. «Pasada interrumpida»
> no se dice en ningún lado.

<a id="F-DICE-R002"></a>
### Rule[F-DICE-R002] - El «resuelto» dice dónde quedó el ejercicio
**Severity**: major | **Validation**: VALIDATED

> En el ejercicio abierto, debajo del chip de un cambio que los jueces dan por bueno, una línea dice dónde quedó su
> ejercicio:
> - si está preparado: «El ejercicio está preparado para entregar»;
> - si está en «Todos los jueces lo aprueban»: «El ejercicio está en «Todos los jueces lo aprueban»»;
> - si no, «No entra en «Todos los jueces lo aprueban»:», seguido del porqué:
>   - quién falta, quién se abstuvo y con qué motivo, o quién opinó con otra versión, con las mismas palabras de
>     «Quién falta»;
>   - o qué jueces o analizadores le ven hoy un problema al ejercicio («Una sola respuesta le ve un problema»);
>   - o, si ninguno opinó al día sobre el ejercicio, eso.
>
> Sin análisis vigente de la corrección no se dice nada, porque no se sabe.

<a id="F-DICE-R003"></a>
### Rule[F-DICE-R003] - Volver a «sin revisar» lo que aprobaste y no aprueban todos los jueces
**Severity**: major | **Validation**: VALIDATED

> Con «Vos lo aprobás» elegido, arriba de la lista se ve cuántos ejercicios son y por qué no están preparados:
> cuántos tienen un problema que ve un juez, el revisor o un analizador, y cuántos tienen una opinión que falta, una abstención o
> una opinión de otra versión. Al lado, el botón «Volver a sin revisar los N».
>
> Tocarlo pide confirmación: cuántos son, cuántos con cambios y cuántos sin cambios, y que los cambios y las opiniones
> quedan como están. Con «Todavía no» o Esc no pasa nada.
>
> Al confirmar, se borra tu aprobación de esos N ejercicios:
> - en uno con cambios, sus cambios vuelven a «sin revisar»;
> - en uno sin cambios, su revisión vuelve a pendiente.
>
> Lo «Preparado para entregar» y lo demás no se toca. Al terminar, un solo aviso dice cuántos volvieron y, si alguno
> no se guardó, cuántos. No se deshace con «Deshacer»: para volver a aprobar uno, se aprueba de nuevo. Con la
> corrección publicada o fija, el botón está apagado.

<a id="F-DICE-R004"></a>
### Rule[F-DICE-R004] - El ejercicio va primero
**Severity**: major | **Validation**: VALIDATED

> En Correcciones, debajo de la cabecera del ejercicio abierto se ve primero el ejercicio: la consigna, el ejercicio, las
> opciones, la traducción y la miniteoría. El recuadro de la pasada va debajo del ejercicio: con qué se compara, qué
> cambió, la leyenda y el aviso de la pasada ([F-DICE-R001](#F-DICE-R001)). Vale con cambios y sin cambios, y en las
> tres vistas.
>
> José, el 1/11/2026: el recuadro de la pasada le tapaba el ejercicio cuando ocupaba toda la pantalla; «poné ese panel
> más arriba, que sea lo más importante para ver».

<a id="F-DICE-R005"></a>
### Rule[F-DICE-R005] - Al revisar, el ejercicio marca todo lo que se aprueba
**Severity**: major | **Validation**: VALIDATED

> Al revisar un ejercicio en Correcciones, sus marcas comparan con el original de la corrección: cada parte que cambió
> alguna de sus correcciones —la consigna, el ejercicio, las opciones, la traducción o la miniteoría— se ve tachada y
> con lo nuevo, sea de la última pasada o de una anterior. Es lo que José aprueba con «Aprobar las N».
>
> Con una pasada elegida en «Filtrar por pasada», la comparación empieza en lo de antes de esa pasada, para ver lo que
> hizo ella. Al quitar la pasada, vuelve al original. El recuadro de la pasada sigue dejando elegir cualquiera de las
> dos.
>
> José, el 4/11/2026, en «Am I (I / be) wrong?»: la regla automática le había sacado a la consigna «Usa $short forms$ si
> es posible.» en una pasada anterior, y sólo se veía marcada la miniteoría de la última. «La mini-teoría siempre
> aparece cuál fue la corrección, pero en la consigna o en la quiz a veces no aparece. Eso hace que no sepa a simple
> vista qué cambió ni por qué».

## Contexto

- **Las dos listas**: «Lo dan por bueno» es un veredicto de un cambio: el juez que lo pidió ya no ve el problema y
  nadie encontró un error nuevo. «Todos los jueces lo aprueban» es un ejercicio entero: ningún juez ni analizador le ve
  un problema y todos los jueces que se aplican opinaron al día sin error. Con tu aprobación pasa a «Preparados para
  entregar»
  ([F-NADIE-R001](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R001)).
- **«Vos lo aprobás»** son los ejercicios con tu aprobación guardada que hoy no aprueban todos los jueces.
- **Fuera de alcance**: si una abstención «no aplica» cuenta como aprobación es una pregunta abierta del brief 237. No
  cambia acá.

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | `comparacion-versiones.test.js`: con la pasada que gastó su tope, el aviso dice la fecha, «paró al llegar a su tope: gastó 5.500 de 5.500 consultas» y si el ejercicio quedó con un cambio; los otros estados dicen su causa; «Pasada interrumpida» no aparece. `pasadas.test.js`: el estado «Paró en su tope» y la cabecera con la causa. En el tablero, `test/tablero_html/vista_de_la_tanda_test.dart`: la pasada lleva lo gastado, el tope, la fecha y el motivo, y un cambio con una tarea de otro ejercicio no cuenta como cambio de la pasada |
| R002 | `resuelto-dice-donde.test.js`: preparado, en «Todos los jueces lo aprueban», con una abstención, con un analizador que le ve un problema, sin opiniones al día y sin análisis vigente |
| R003 | `volver-a-sin-revisar.test.js`: el bloque sólo en «Vos lo aprobás», con la cuenta y el porqué; confirmar manda «sin revisar» a los cambios y a las revisiones de los ejercicios sin cambios, no toca lo preparado y da un aviso; «Todavía no» y Esc no mandan nada; apagado con la corrección publicada |
| R004 | `comparacion-versiones.test.js`: con cambios y sin cambios, en Marcas, Lado a lado y Como el alumno, el ejercicio está antes que el recuadro de la pasada |
| R005 | `comparacion-versiones.test.js`: al revisar, con la traducción cambiada en una pasada anterior y la miniteoría en la última, las dos filas se marcan y el recuadro dice «Original de esta corrección»; con una pasada elegida compara con lo de antes de ella, y al quitarla vuelve al original |

## References

- Brief 237, la C2 busca las tareas de la C1 en su propio plan (28/10/2026).
- [F-NADIE-R003](../2026-10-20.01_nadie-le-ve-problemas/REQUIREMENT.md#F-NADIE-R003): «Quién falta».
- [F-AVISO-R008](../2026-10-20.03_el-servidor-avisa/REQUIREMENT.md#F-AVISO-R008): un cambio, un aviso.
