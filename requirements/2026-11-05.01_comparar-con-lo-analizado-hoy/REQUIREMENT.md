---
feature:
  id: FEAT-HOY
  code: F-HOY
  name: Comparar con lo analizado hoy - la comparación de la tanda sin cerrarla
  priority: major
---

# Comparar con lo analizado hoy

## TL;DR

**Qué**: Comparar muestra «Hoy»: el curso con todo lo que la tanda propone, según su análisis vigente, contra la base y
el inicio de la tanda. Son los mismos números y la misma compuerta que daría un cierre, pero no se cierra nada, no se
consulta a ningún juez y no se escribe nada.

**Por qué**: El 5/11/2026, José quiso comparar la C2 de A1 con producción y Comparar le dijo «La tanda A1 todavía no
tiene una medición». Comparar sólo leía los cierres, y la C2 nunca se cerró: cerrar no tiene vuelta, devuelve cambios y
saca la muestra ciega. Pero la C2 tiene todas sus opiniones al día y su análisis vigente es el que mira un cierre.
José: «Es lamentable que no pueda comparar dado que tengo todo tan analizado».

## Recorrido de uso (estado actual)

1. José revisa la C2 en Correcciones, con todas las opiniones al día ✓.
2. Abre Comparar para ver cuánto mejora la C2 contra producción, juez por juez y nivel por nivel ✓.
3. ⚠️ Comparar dice «La tanda A1 todavía no tiene una medición» y no muestra nada.
4. ⚠️ Para ver algo tendría que cerrar la tanda, que no tiene vuelta.

**Punto de fricción**: pasos 3 y 4. El análisis está hecho, pero Comparar no lo lee.

**Cambio mínimo necesario**: que Comparar arme la comparación desde el análisis vigente, como ya lo hace Publicar antes
de publicar, sin cerrar ni escribir.

## Reglas de Negocio

<a id="F-HOY-R001"></a>
### Rule[F-HOY-R001] - Comparar muestra «Hoy», aunque la tanda no se haya cerrado
**Severity**: major | **Validation**: VALIDATED

> Con un análisis vigente, Comparar suma una medición más, «Hoy»: el curso con todo lo que la tanda propone, según el
> análisis vigente de esa versión del contenido. Se compara contra la base y el inicio de la tanda que trae esa misma
> lectura del análisis, igual que un cierre
> ([F-METAS-R001](../2026-09-30.27_antes-y-despues-con-metas/REQUIREMENT.md#F-METAS-R001)). Lo que el análisis no midió
> queda sin medir: no cuenta como 0 ni como 100.
>
> «Hoy» va después de las mediciones de los cierres. Al abrir Comparar se ve «Hoy» contra la Base. Los cierres siguen
> estando, cada uno con su número.

<a id="F-HOY-R002"></a>
### Rule[F-HOY-R002] - Comparar con lo de hoy no cambia nada
**Severity**: critical | **Validation**: VALIDATED

> Calcular «Hoy» no cierra la tanda ni le cambia el estado. Tampoco devuelve cambios, no saca la muestra ciega, no le
> pregunta a ningún juez, no gasta consultas y no escribe la tanda, el curso ni el análisis. Si la tanda está tomada por
> otra operación, igual se puede leer.
>
> Extiende [F-METAS-R009](../2026-09-30.27_antes-y-despues-con-metas/REQUIREMENT.md#F-METAS-R009) a «Hoy».

<a id="F-HOY-R003"></a>
### Rule[F-HOY-R003] - «Hoy» dice qué es y si la tanda pasaría
**Severity**: major | **Validation**: VALIDATED

> «Hoy» se presenta como «el curso con todo lo propuesto, según el análisis vigente», con su versión del contenido, y
> dice que no es un cierre.
>
> Con «Hoy» elegido, el bloque de la compuerta dice si la tanda pasaría hoy («pasaría» o «no pasaría»):
> - los pisos rotos;
> - cuántos cambios volverían a revisión;
> - lo que quedó sin decidir.
>
> No dice que la comparación está aprobada: eso lo dice sólo un cierre. Tampoco muestra consultas ni jueces pagados,
> porque «Hoy» no gastó nada. «Volver a medir y cerrar la tanda» sigue siendo lo único que cierra.

<a id="F-HOY-R004"></a>
### Rule[F-HOY-R004] - Sin «Hoy», Comparar dice por qué
**Severity**: major | **Validation**: VALIDATED

> - Si no se pudo leer «Hoy», los cierres se siguen viendo y un aviso dice por qué falta, con el motivo.
> - Sin «Hoy» y sin ningún cierre, Comparar dice que la tanda todavía no tiene nada medido para comparar.
> - Mirando la Base, dice que la Base es el punto de partida y que se compara desde una corrección.

## Contexto

- **Lo que se compara** es lo mismo que mira un cierre: la base, el inicio de la tanda y el curso con todo lo propuesto,
  con las opiniones guardadas de cada juez. Un cierre además paga a los jueces lo que falta, devuelve los cambios que
  rompen un piso, saca la muestra ciega y cierra la tanda. «Hoy» no hace nada de eso.
- **Publicar** ya compara así antes de publicar
  ([F-PUBPROD-R005](../2026-10-11.01_publicar-a-produccion/REQUIREMENT.md#F-PUBPROD-R005)).

## Cómo se prueba

| Regla | Prueba |
|---|---|
| R001 | content-audit, `DefaultBatchCloserTest`: el después es el análisis vigente, comparado una vez con todas las propuestas, y la compuerta dice que «My grandmother ___ (come) every year.» volvería. Tablero: `vista_de_comparar_test.dart`, «Hoy» va último, con sus valores, y la base y el inicio salen de su lectura; una tanda sin cierres tiene la base, el inicio y «Hoy». `rutas_de_comparar_test.dart`, se pide para la tanda que se mira. `comparar-hoy.test.js`, «Hoy» se ve por omisión contra la Base, también sin cierres |
| R002 | content-audit, `DefaultBatchCloserTest`: la tanda sigue abierta y con sus cambios, sin cierres ni muestra ciega, ningún juez consultado y nada guardado. `BatchCmdTest`: `batch compare <tanda>` imprime la comparación en JSON sin correr, cerrar, devolver ni decidir nada. `rutas_de_comparar_test.dart`: el tablero corre sólo `batch compare` y lo recorta como un cierre |
| R003 | `vista_de_comparar_test.dart`: lo de hoy trae la compuerta y el veredicto, sin jueces ni topes. `comparar-hoy.test.js`: con «Hoy», el bloque dice «pasaría» o «no pasaría», los pisos rotos y los cambios que volverían, que no es un cierre, sin «aprobada» ni consultas; con un cierre elegido, el bloque es el de siempre |
| R004 | `rutas_de_comparar_test.dart` y `vista_de_comparar_test.dart`: si leer «Hoy» falla, la vista trae los cierres y el motivo; la Base no se le pregunta al CLI. `comparar-hoy.test.js`: el aviso con el motivo, el mensaje de nada medido y el de la Base |

## References

- Brief 239 y [F-ABIER](../2026-10-27.01_medir-con-la-tanda-abierta/REQUIREMENT.md): medir con la tanda abierta.
- [F-METAS](../2026-09-30.27_antes-y-despues-con-metas/REQUIREMENT.md): la comparación, sus filas y su compuerta.
