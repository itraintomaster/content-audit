---
feature:
  id: FEAT-MEDJ
  code: F-MEDJ
  name: Mediciones por juez en el Mapa - una fila por juez dice qué trae cada corrida suelta y ofrece lo que conviene integrar
  priority: major
---

# Mediciones por juez en el Mapa: una fila por juez dice qué trae cada corrida suelta y ofrece lo que conviene integrar

## TL;DR

**Qué**: En el paso «Hoy» del Mapa, debajo del dibujo, va una fila por juez. Cada fila tiene una celda debajo de cada
análisis vigente. La celda dice cuánto está medido y ofrece la corrida suelta que más aporta: huecos gratis, la versión
de hoy del juez o resultados distintos. Lo que sólo repite lo medido, o trae una versión anterior, queda en la fila
abierta, con su motivo. content-audit dice qué trae cada corrida a cada estado; el tablero no compara versiones.

**Por qué**: Hoy el Mapa dibuja una tarjeta por corrida suelta, con flechas a los análisis. El 19/10 son 51 tarjetas y
ocupan 9.500 de los 10.100 px de alto. Las pastillas «75 de 90» se enciman y no dicen si integrar sirve. José preguntó si
«11 reemplazarían 88» era bueno o malo, y la tarjeta no lo podía contestar: reemplazar es bueno si trae el juez de hoy
o cambia resultados, e inútil si repite lo medido.

Fuente: [propuesta «Mediciones por juez»](../../docs/propuestas/mapa-mediciones-por-juez.html), v2 del 19/10, y
[brief 154](../../docs/briefs/154-dos-formas-de-dibujar-el-carril.html) (opción A: el carril como lista debajo del dibujo).
El tablero es `pipeline/vocabulary_dashboard` en ittm.

## Recorrido de uso (estado actual)

Cómo José busca hoy qué corrida suelta conviene integrar. El caso es la tanda A1 al 19/10.

1. Abre el Mapa en «Hoy» ✓. ⚠️ Debajo del dibujo empiezan 51 tarjetas, una abajo de otra; la primera pantalla es la
   única que muestra la tanda.
2. Busca el pragmatismo ✓. ⚠️ Hay 13 tarjetas suyas, entreveradas con las de otros jueces. Once dicen lo mismo: «75 de
   90» hacia la base y «88 de 90» hacia la corrección.
3. Lee «11 reemplazarían 88» ⚠️. No sabe si eso trae algo nuevo: no dice que esas 11 repiten el mismo juez y el mismo
   resultado.
4. Busca si la miniteoría de anoche se puede integrar ⚠️. Su tarjeta dice «2.402 de 2.402», igual que una que repitiera
   todo. No dice que trae la versión de hoy para 2.382 ejercicios.
5. Abre la ficha de una corrida ✓ e integra.

**Punto de fricción**: pasos 2 a 4. Se ve cuánto encaja, no qué trae.

**Cambio mínimo necesario**: que content-audit diga qué trae cada corrida a cada estado, y que el Mapa lo agrupe por
juez, ofreciendo sólo lo que aporta.

## Reglas de Negocio

Una **corrida suelta** es una medición de un juez que no está integrada en ningún análisis vigente. **Encajar** en un
estado es haber medido su mismo contenido, con la misma vista del alumno. **Integrar** es elegir la corrida para que
cuente en ese estado; no consulta. Las palabras **juez**, **versión de hoy** y **al día** son las de
[FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md).

<a id="F-MEDJ-R001"></a>
### Rule[F-MEDJ-R001] - content-audit dice qué trae cada corrida a cada estado
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada corrida suelta y cada estado, lo que encaja se parte en seis grupos que no se pisan y suman lo que encaja:
> - **ya cuenta**: esa corrida ya es la que cuenta ahí;
> - **hueco**: ahí no cuenta ningún veredicto de ese juez;
> - **juez de hoy**: la corrida es del juez de hoy ([F-MEDJ-R002](#F-MEDJ-R002)) y lo que cuenta no;
> - **otra versión**: la corrida no es del juez de hoy, y lo que cuenta sí lo es o es de otra versión;
> - **otro resultado**: misma versión, pero con otras marcas de error;
> - **repite**: lo mismo que ya cuenta.
>
> Invariantes:
> 1. un ejercicio va al primer grupo que le corresponde, en ese orden;
> 2. «reemplaza» sigue siendo la suma de los cuatro últimos;
> 3. los errores que se perderían o se sumarían siguen contando también los huecos;
> 4. leer la partición no consulta ni escribe.

<details><summary>Detail</summary>

El orden pone primero lo que José quiere saber: si una corrida trae el juez de hoy, eso manda aunque además cambie
resultados. Una versión anterior que cambia resultados es «otra versión», no «otro resultado»: no conviene integrarla.

**Criterio**, con la tanda A1 al 19/10 (las horas son las de Buenos Aires, como las muestra el tablero): la corrida de
pragmatismo del 14/10 a las 20:21 trae 1.656 huecos a la base; en la corrección, 2.312 ya cuentan y 88 repiten. La
miniteoría del 19/10 a las 00:54, en la corrección: 2.382 son juez de hoy, 16 son otro resultado y 4 repiten.

</details>

<a id="F-MEDJ-R002"></a>
### Rule[F-MEDJ-R002] - Qué es el juez de hoy, aprobado o no
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un veredicto es del **juez de hoy**:
> - si la versión de hoy aprobó su examen: cuando su marca es «al día» ([F-ALDIA-R003](#F-ALDIA-R003)), con versión y
>   motor;
> - si no aprobó: cuando su versión es la de hoy o una registrada como la misma ([F-ALDIA-R002](#F-ALDIA-R002)). El motor
>   no se mira.
>
> Invariantes:
> 1. un juez sin versión de hoy no tiene veredictos de hoy;
> 2. de cada corrida y estado se dice si todo lo que encaja es del juez de hoy, y si la versión de hoy aprobó. Sin versión
>    de hoy no se dice ninguna de las dos; sin nada que encaje, no se dice la primera;
> 3. la versión de la corrida se juzga con el motor con que midió.

<details><summary>Detail</summary>

José decidió el 19/10 ofrecer la versión de hoy aunque no haya aprobado, avisando ([F-MEDJ-R005](#F-MEDJ-R005)). Por eso
una versión sin aprobar también es «de hoy»: con la marca de F-ALDIA, todo sería «anterior» y nada se ofrecería.

**Criterio**: la naturalidad no aprobó su versión de hoy, `313ec3de`. Su corrida del 14/10 a las 15:00 es de esa versión
y en la base cuenta la de llama3 del 1/10: son 293 «juez de hoy». Los duplicados cercanos no tienen versión de hoy: sus
corridas no traen nada de hoy y no se dice si aprobó.

</details>

<a id="F-MEDJ-R003"></a>
### Rule[F-MEDJ-R003] - Una fila por juez, con una celda debajo de cada análisis vigente
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En «Hoy», debajo del dibujo del Mapa, va una fila por juez con corridas sueltas. Invariantes:
> 1. a la izquierda, el nombre del juez, cuántas corridas tiene y un punto por corrida; las repetidas se cuentan «×11»;
> 2. hay una celda debajo de cada análisis vigente, alineada con su dial;
> 3. la celda dice cuánto está medido en la unidad del juez («2.314 de 2.402», «83 de 83 temas»), de dónde sale («cierre»
>    o las fechas de lo integrado) y cuántos tienen error. Si el juez nunca midió ese estado, dice «no corrió», punteada;
> 4. la celda ofrece una sola corrida: la que más aporta, sumando huecos, juez de hoy y otro resultado;
> 5. lo que ofrece va en un botón que dice qué trae: «+1.656 gratis», «versión de hoy · actualiza 2.382» o «cambia 16
>    resultados». Si trae varias cosas, las suma: «versión de hoy · actualiza 293 · suma 6»;
> 6. el botón abre la ficha de esa corrida con ese estado primero; integrar se confirma ahí, como hoy.

<details><summary>Detail</summary>

Las celdas siguen a los diales para que se lean como columnas del dibujo: lo que está debajo de A4 es de A4. El carril
es parte de la página, no del dibujo: la letra no cambia con la ventana y un texto largo baja de renglón (brief 154).

**Criterio**, con la tanda A1 al 19/10: el pragmatismo dice «no corrió · +1.656 gratis» debajo de A0 y «2.314 de 2.402 ·
0 con error» debajo de A4. La miniteoría ofrece en A4 «versión de hoy · actualiza 2.382 · cambia 16».

</details>

<a id="F-MEDJ-R004"></a>
### Rule[F-MEDJ-R004] - Lo que repite o trae una versión anterior no se ofrece
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La celda no ofrece una corrida que en ese estado sólo trae «repite», «otra versión» o «ya cuenta». Invariantes:
> 1. esas corridas quedan en la fila abierta ([F-MEDJ-R007](#F-MEDJ-R007)) con su motivo: «repite lo medido», «juez
>    anterior» o «integrada»;
> 2. un juez con versión de hoy dice «juez anterior»; uno sin versión de hoy dice «otra versión»;
> 3. una fila sin nada que ofrecer muestra igual su cobertura.

<details><summary>Detail</summary>

Es la regla que aprobó José el 19/10: «en la celda ofrecé el reemplazo cuando trae un juez distinto o cambia
resultados, y mandá a la fila abierta las que sólo repiten lo medido». Una versión anterior no se ofrece porque bajaría
lo medido a un juez de antes.

**Criterio**: las 11 corridas de pragmatismo del 14/10 20:38 al 18/10 04:55 no aparecen en la celda de A4; en la fila
abierta dicen «repite lo medido». Más de una respuesta válida y Traducción no ofrecen nada: sus corridas integradas
cuentan y las otras repiten.

</details>

<a id="F-MEDJ-R005"></a>
### Rule[F-MEDJ-R005] - La versión de hoy sin aprobar se ofrece, avisando
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si la versión de hoy no aprobó su examen, lo que trae se ofrece igual, con aviso:
> 1. el botón agrega «sin aprobar»: «versión de hoy · sin aprobar · actualiza 293 · suma 6»;
> 2. al pasar por encima, y en la ficha, dice «La versión de hoy todavía no aprobó su examen: lo que integres no sirve
>    para publicar.»;
> 3. los huecos de un juez sin aprobar también llevan el aviso: «+19 gratis · sin aprobar».

<details><summary>Detail</summary>

José eligió el 19/10 «ofrecerla, avisando». Publicar exige jueces al día ([F-TALDIA-R008](#F-TALDIA-R008)), y un juez
sin aprobar nunca lo está.

**Criterio**: la naturalidad ofrece en A0 «versión de hoy · sin aprobar · actualiza 293 · suma 6». La calidad didáctica
A1 ofrece en A0 «+19 gratis · sin aprobar».

</details>

<a id="F-MEDJ-R006"></a>
### Rule[F-MEDJ-R006] - Primero lo que conviene; al pie, lo que no tiene fila
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Invariantes:
> 1. las filas con algo que ofrecer van primero; después, las demás. Dentro de cada grupo, en el orden de los jueces del
>    tablero;
> 2. al pie, un renglón nombra los jueces sin corridas sueltas: «Sin sueltas: Consigna que no se cumple · Opción múltiple
>    mal armada»;
> 3. al pie, otro cuenta las corridas que no encajan en ningún estado, con un enlace a Ejecuciones: «23 corridas no
>    encajan · Ver en Ejecuciones»;
> 4. sin corridas sueltas no hay carril: sólo el renglón de los jueces, si hay jueces.

<details><summary>Detail</summary>

Las corridas que no encajan midieron otro contenido o no dejaron veredicto: no hay nada que integrar, pero conviene saber
que existen.

**Criterio**, con la tanda A1 al 19/10: arriba van la miniteoría, la naturalidad, el pragmatismo y la calidad didáctica
A1. Al pie: «23 corridas no encajan». Son 12 de naturalidad y 11 de calidad didáctica A1.

</details>

<a id="F-MEDJ-R007"></a>
### Rule[F-MEDJ-R007] - La fila abierta muestra cada corrida y qué trae a cada estado
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Tocar el nombre de un juez abre su fila; se abre una a la vez. Invariantes:
> 1. una línea de tiempo pone un punto por corrida en su día. Los días sin corridas se achican y un grupo de repetidas
>    lleva su banda «×11»;
> 2. debajo va un renglón por corrida, de la más nueva a la más vieja. Cada renglón dice fecha y hora, cuántos midió,
>    cuántos con error y cuántas consultas gastó. Por cada estado, dice qué trae: «integrada 2.312», «+1.656 gratis»,
>    «versión de hoy · actualiza 293», «cambia 16 resultados», «juez anterior 82», «repite lo medido» o «no encaja»;
> 3. las corridas seguidas que traen lo mismo a cada estado van en un solo renglón: «×11 · 14/10 20:38 → 18/10 04:55 ·
>    mismo juez y mismo resultado»;
> 4. una corrida que estuvo integrada y ya no cuenta dice «Histórica», con su motivo: «Actualizada por otra medición» o
>    «Se integró en un contenido anterior»;
> 5. cada cosa que trae algo es un botón que abre la ficha con ese estado primero ([F-MEDJ-R003](#F-MEDJ-R003), inv. 6);
> 6. en la ficha, el estado elegido va primero y dice «desde el mapa». Si sólo repite, dice «repite lo medido», nunca
>    «reemplaza».

<details><summary>Detail</summary>

La fila abierta es donde se ve todo: lo que la celda no ofrece queda acá con su motivo. La marca «Histórica» es la que
ya tenían las tarjetas del Mapa.

**Criterio**: la fila del pragmatismo abierta muestra 13 corridas en 3 renglones: 18/10 07:02 con «integrada 90» en A4 y
«+77 gratis» en A0; el grupo «×11» con «repite lo medido» en A4 y «+75 gratis» en A0; y el 14/10 20:21, con «integrada
2.312» en A4 y «+1.656 gratis» en A0.

</details>

<a id="F-MEDJ-R008"></a>
### Rule[F-MEDJ-R008] - El carril se ve sólo en «Hoy»
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Invariantes:
> 1. el carril de mediciones aparece sólo en el paso «Hoy» del Mapa;
> 2. en los otros pasos, en su lugar, una línea dice «Las mediciones sueltas se ven en Hoy»;
> 3. el dibujo no lleva más tarjetas ni flechas de corridas sueltas: mide lo que mide la tanda.

<details><summary>Detail</summary>

José eligió el 19/10 «sólo en Hoy». Los pasos anteriores muestran cómo se llegó; integrar es de hoy.

**Criterio**: en «Hoy» el Mapa de la tanda A1 vuelve a medir el alto del dibujo, y el carril va abajo. En el paso de la
corrección dice «Las mediciones sueltas se ven en Hoy».

</details>

<a id="F-MEDJ-R009"></a>
### Rule[F-MEDJ-R009] - El tablero no calcula lo que trae una corrida
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Invariantes:
> 1. el tablero lee de content-audit la partición ([F-MEDJ-R001](#F-MEDJ-R001)) y si cada corrida es del juez de hoy
>    ([F-MEDJ-R002](#F-MEDJ-R002)). Nunca compara versiones ni marcas para decidirlo;
> 2. elegir la corrida que más aporta y ordenar las filas sí es del tablero: sólo suma los grupos que dio content-audit;
> 3. si content-audit no da la partición, la celda no ofrece nada y la fila abierta dice «content-audit no dice qué trae
>    esta corrida»;
> 4. mostrar el carril no consulta ni integra.

<details><summary>Detail</summary>

Es lo mismo que pide el análisis vigente: el servidor decide cobertura, errores y compatibilidad, y la UI no elige.

**Criterio**: con una respuesta de content-audit sin la partición, ninguna celda ofrece nada y las coberturas se ven
igual.

</details>

## Contexto

| Pieza | Hoy | Con esto |
|---|---|---|
| Mapa, abajo del dibujo | 51 tarjetas y sus flechas, 9.500 px | Una fila por juez, unos 500 px |
| Pragmatismo | 13 tarjetas | 1 fila: «+1.656 gratis» en A0 |
| Miniteoría de anoche | «2.402 de 2.402» | «versión de hoy · actualiza 2.382 · cambia 16» |
| Las 11 repetidas | «11 reemplazarían 88» | «×11 · repite lo medido», en la fila abierta |
| Lo que no encaja | 23 tarjetas sin flecha | «23 corridas no encajan · Ver en Ejecuciones» |
| Leyenda | «encaja en» | sin ese trazo |

## Alcance

- **Adentro**: la partición y el juez de hoy en content-audit; el carril por juez en «Hoy»; la fila abierta; el estado
  elegido en la ficha; sacar las tarjetas y flechas del dibujo.
- **Afuera**: cómo se integra (la ficha y sus confirmaciones no cambian); medir de nuevo, que sigue en el diálogo de
  [F-TALDIA-R004](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R004); Ejecuciones; Analizadores
  y Estadísticas, que siguen abriendo la ficha como hoy.

## User Journeys

### Journey[F-MEDJ-J001] - Ver qué trae cada juez e integrar lo que conviene
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MEDJ-J001
    name: Ver qué trae cada juez e integrar lo que conviene
    flow:
      - id: mapa
        action: "José abre el Mapa en «Hoy»"
        gate: [F-MEDJ-R003, F-MEDJ-R006, F-MEDJ-R008]
        outcomes:
          - when: "Una celda ofrece una corrida"
            then: ficha
          - when: "José quiere ver todas las corridas de un juez"
            then: fila
          - when: "Ninguna celda ofrece nada"
            then: nada
      - id: fila
        action: "José abre la fila del juez y ve qué trae cada corrida a cada estado"
        gate: [F-MEDJ-R004, F-MEDJ-R007]
        outcomes:
          - when: "Una corrida trae algo"
            then: ficha
          - when: "Todas repiten lo medido o traen un juez anterior"
            then: nada
      - id: ficha
        action: "La ficha abre con el estado elegido primero; si el juez no aprobó, lo avisa"
        gate: [F-MEDJ-R005, F-MEDJ-R007]
        outcomes:
          - when: "José integra"
            then: integrada
      - id: integrada
        action: "La celda muestra la cobertura nueva y deja de ofrecer esa corrida"
        gate: [F-MEDJ-R001, F-MEDJ-R009]
        result: success
      - id: nada
        action: "No hay nada que integrar: lo medido ya cuenta"
        gate: [F-MEDJ-R004]
        result: success
```

## Open Questions

No quedan abiertas. José decidió el 19/10: content-audit calcula si cada corrida trae el juez de hoy; la versión sin
aprobar se ofrece avisando; una versión anterior no se ofrece; la celda ofrece la que más aporta; las filas que ofrecen
van primero; los jueces sin sueltas y lo que no encaja van al pie; el carril va sólo en «Hoy», como lista debajo del
dibujo.

## Cómo se prueba

Las pruebas de content-audit arman la tanda en memoria. Las del tablero usan respuestas grabadas. Ninguna consulta a un
modelo.

| Regla | Qué se comprueba |
|---|---|
| R001 | Los seis grupos suman lo que encaja y «reemplaza» suma los cuatro últimos. Un ejercicio que trae el juez de hoy y cambia resultados cuenta como juez de hoy. Los huecos siguen en los errores ganados |
| R002 | Aprobado, sólo lo «al día» es de hoy. Sin aprobar, cuenta la versión y no el motor. Sin versión de hoy, nada es de hoy y no se dice si aprobó |
| R003 | Cada celda va debajo de su dial y ofrece la corrida que más aporta, con un botón que dice qué trae |
| R004 | Lo que sólo repite o trae una versión anterior no se ofrece y queda en la fila abierta con su motivo |
| R005 | Con la versión de hoy sin aprobar, el botón dice «sin aprobar» y avisa que no sirve para publicar |
| R006 | Las filas que ofrecen van primero. El pie nombra los jueces sin sueltas y cuenta lo que no encaja |
| R007 | La fila abierta agrupa las repetidas, marca las históricas y abre la ficha con el estado primero |
| R008 | El carril aparece sólo en «Hoy». El dibujo no tiene tarjetas ni flechas |
| R009 | Sin la partición no se ofrece nada. Mostrar el carril no consulta |

## References

- **FEAT-ALDIA**: la versión de hoy y las que son la misma ([F-ALDIA-R002](#F-ALDIA-R002)), la marca de cada veredicto
  ([F-ALDIA-R003](#F-ALDIA-R003)).
- **FEAT-TALDIA**: el diálogo de volver a medir ([F-TALDIA-R004](#F-TALDIA-R004)) y la condición de publicar
  ([F-TALDIA-R008](#F-TALDIA-R008)).
- **docs/CLI.md**, «Análisis vigente e integración de corridas»: la ficha de integración y los campos de la partición.
- **La propuesta**: [Mediciones por juez](../../docs/propuestas/mapa-mediciones-por-juez.html), v2 del 19/10; y el
  [brief 154](../../docs/briefs/154-dos-formas-de-dibujar-el-carril.html).
