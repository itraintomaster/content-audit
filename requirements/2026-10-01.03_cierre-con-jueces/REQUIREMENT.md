---
feature:
  id: FEAT-CJUEZ
  code: F-CJUEZ
  name: Cierre con jueces - al cerrar, la tanda juzga lo que cambió, antes y después
  priority: critical
---

# Cierre con jueces: al cerrar, la tanda juzga lo que cambió, antes y después

## TL;DR

**Qué**: Al cerrar, la tanda juzga con cada juez lo que cambió, antes y después: avisa antes cuántas consultas faltan,
paga sólo con un tope pedido, y un juez sin examen aprobado deja la tanda sin publicar y con su nombre en el veredicto.

**Por qué**: Hoy cerrar no le paga a ningún juez: la tanda de A1 cambió 601 ejercicios y quedó «no se puede decidir»,
con 28 pisos sin decidir y sin decir qué juez falta. José, 1/10: «no publiques sin los jueces».

## Recorrido de uso (estado actual)

Cómo José cierra hoy una tanda y decide si se puede publicar. El caso: la tanda de A1 de prueba —el control del paquete E
sobre una copia del curso, el 1/10—, que cambió 601 ejercicios en 30 temas.

1. Abre la tanda → `batch open A1`. ✓ Disponible.
2. Pregunta cuánto cuesta → `batch estimate`. ✓ Disponible, pero ⚠️ cuenta sólo las consultas de la mesa (580 en A1);
   de los jueces que el cierre necesita, nada.
3. Corre → `batch run`. ✓ Disponible: sin tope aplica las reglas; con `--cap N`, también la mesa.
4. Cierra → `batch close`. ✓ Disponible, pero ⚠️ no le paga a ningún juez: sólo reusa lo que está en el libro. La tanda
   de A1 quedó «no se puede decidir», con «28 pisos sin decidir» y «601 cambios sin evaluar por algún analizador», sin
   decir qué juez falta, por qué, ni cuánto costaría decidirlo.
5. Lee el informe → `batch report`. ⚠️ Da las consultas por puesto ([F-TANDA-R010](#F-TANDA-R010)), pero ninguna cuenta
   por juez: ni lo gastado, ni lo reusado, ni los pisos que decide cada uno.

**Punto de fricción**: paso 4, cerrar.

**Cambio mínimo necesario**: que el cierre juzgue lo que la tanda cambia, antes y después, con un tope pedido; que se
pueda estimar antes cuánto falta; y que el veredicto nombre al juez sin aprobar.

## Reglas de Negocio

Desde el 4/10, las menciones a `claude -p` como proveedor activo significan por omisión `codex-cli`, autenticado con la sesión local
de ChatGPT; una selección explícita de otro proveedor prevalece ([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)). Los números medidos con Claude siguen siendo historia y deben recalcularse
antes de estimar una corrida real con la versión nueva ([F-JEXAM-R014](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R014)).

Acá, un **juez** es un analizador de la tanda que le pregunta a un modelo —a claude -p o a uno local— o que pasa una
herramienta local con examen, como la gramática. **Pagar** una consulta es hacérsela a claude -p: lo que se le pregunta
a un modelo local es gratis ([F-MLOCAL-R008](#F-MLOCAL-R008)). El **libro** guarda los veredictos de los jueces por
contenido, y lo que ya tiene se reusa ([F-EVCOST-R003](#F-EVCOST-R003)). Un ejercicio **cambia** si tiene una propuesta
pendiente de la tanda ([F-CJUEZ-R001](#F-CJUEZ-R001)).

<a id="F-CJUEZ-R001"></a>
### Rule[F-CJUEZ-R001] - Al cerrar, se juzga lo que la tanda cambia, antes y después
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al cerrar, la tanda juzga dos veces cada ejercicio que cambia —como está en el curso del que partió (el antes) y como
> queda con sus cambios (el después)— con cada juez que lee ese tipo de ejercicio. El juez de miniteoría juzga por tema:
> antes y después, cada tema que tiene un ejercicio que cambia. Lo que se paga, se paga dentro del tope pedido
> ([F-CJUEZ-R003](#F-CJUEZ-R003)) y sólo con un juez de examen aprobado ([F-CJUEZ-R004](#F-CJUEZ-R004)). Invariantes:
> 1. no se paga ninguna consulta por otro ejercicio ni por otro tema del curso;
> 2. lo que el libro ya tiene de ese juez para ese mismo contenido se reusa y no se paga
>    ([F-EVCOST-R003](#F-EVCOST-R003), [F-EVCOST-R006](#F-EVCOST-R006));
> 3. un contenido que el juez lee igual antes y después —un cambio que no toca lo que ese juez lee, como pedir regrabar
>    un audio— se paga una vez;
> 4. cerrar no escribe el curso ni el historial de análisis; fuera de la propia tanda, sólo quedan los veredictos en el
>    libro.

<details><summary>Detail</summary>

Un ejercicio **cambia** si tiene una propuesta pendiente de la tanda; si la propuesta es del tema —su consigna—, cambian
el tema y todos sus ejercicios. Es la misma cuenta con que la comparación decide los pisos de errores contra el inicio de
la tanda ([F-METAS-R002](#F-METAS-R002)). Es el conjunto de [F-JEXAM-R005](#F-JEXAM-R005), armado por la tanda:
ejercicios, o temas en el de miniteoría, y ninguno más.

Juzgar el antes hace falta: sin él, un error que el ejercicio ya tenía aparece como nuevo en el después, y el cambio
vuelve sin motivo ([F-METAS-R003](#F-METAS-R003)). Los jueces de ejercicio leen el ejercicio con el nivel y el tema; la
miniteoría lee el tema entero, por eso va por tema.

**Criterio**, con la tanda de A1: los jueces de ejercicio juzgan los 601 ejercicios que la tanda cambió, antes y después,
y ningún otro de A1; la miniteoría, sus 30 temas, antes y después. Un ejercicio que sólo pidiera regrabar su audio se
pagaría una vez por juez.

</details>

<a id="F-CJUEZ-R002"></a>
### Rule[F-CJUEZ-R002] - Antes de pagar, la tanda dice cuántas consultas faltan por juez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `batch estimate` y el cierre sin tope declaran, por cada juez, cuántos ejercicios o temas juzga —antes y después—,
> cuántas consultas reusa del libro y cuántas faltan para decidir sus pisos; y el total de consultas y horas de los
> jueces aprobados. `batch estimate` no hace ninguna consulta, y el cierre sin tope no paga ninguna. `batch estimate`
> cuenta los cambios que la tanda tiene en ese momento y lo dice: «sobre los N ejercicios que la tanda cambia hoy»; si
> todavía no cambió ninguno, lo dice y remite a `batch run`.

<details><summary>Detail</summary>

La cuenta puede sobrar cuando un cambio no toca lo que lee un juez (inv. 3 de [F-CJUEZ-R001](#F-CJUEZ-R001)): pagar el
antes ya cubre el después. Las horas van a 19 s por consulta, con las consultas a la vez de la tanda —4 por omisión—,
como la estimación de [F-TANDA-R003](#F-TANDA-R003). Mantiene [F-JEXAM-R004](#F-JEXAM-R004): cada juez dice cuánto
cuesta, sin consultar.

Un juez sin aprobar se lista con su motivo y no suma al total: no consulta con ningún tope
([F-CJUEZ-R004](#F-CJUEZ-R004)). Lo que un juez le pregunta a su modelo local —`naturalness` con Ollama— tampoco suma: lo
juzga la comparación al cerrar, sin tope y sin pagar ([F-RONDA2-R002](#F-RONDA2-R002)) [ASSUMPTION].

Sin cambios, el mensaje es «la tanda todavía no cambió ningún ejercicio: los jueces se estiman después de `batch run`».

**Criterio**, con la tanda de A1: `batch estimate` dice «sobre los 601 ejercicios que la tanda cambia hoy». Los
aprobados que consultan a claude -p son `translation-sense` —dos jueces por ejercicio: hasta dos consultas por lado— y
`mini-theory` —un tema por lado—; la estimación nombra cuántas faltan de cada uno y no consulta. `naturalness`, aprobado,
le pregunta a su modelo local; los cinco sin aprobar van con su motivo y no suman.

</details>

<a id="F-CJUEZ-R003"></a>
### Rule[F-CJUEZ-R003] - Se paga sólo con un tope pedido, y nunca más
**Severity**: critical | **Validation**: AUTO_VALIDATED

> `batch close --budget <juez>=N[,<juez>=N…]` le da a cada juez nombrado su tope; `batch close --cap N` es el tope de la
> tanda, que comparten en orden los jueces que `--budget` no nombra, y con los dos el total nunca pasa N. Invariantes:
> 1. sin tope, el cierre no le paga ninguna consulta a ningún juez por lo que la tanda cambia: es un ensayo, como hoy;
> 2. ningún juez gasta más que su tope ni el cierre más que el de la tanda; lo gastado se suma a las consultas de la
>    tanda;
> 3. si el tope no alcanza, los pisos que dependen de lo que falta quedan sin decidir, y el informe dice cuántas consultas
>    faltan por juez y el pedido para completarlas;
> 4. un nombre que no es de un juez de la tanda, o un tope negativo, se rechaza sin consultar nada;
> 5. tras una falla del servicio de un juez, ese juez no empieza ninguna consulta más y su cuenta dice por qué
>    ([F-JEXAM-R006](#F-JEXAM-R006), [F-JEXAM-R011](#F-JEXAM-R011));
> 6. cerrar otra vez no paga dos veces: lo que se juzgó queda en el libro y se reusa.

<details><summary>Detail</summary>

«En orden» es el orden del catálogo de analizadores. Cada pedido de cierre trae su tope; lo que se pagó en un cierre
anterior ya está en el libro (inv. 6). Mantiene [F-TANDA-R003](#F-TANDA-R003) («sin pedido explícito no se paga nada») y
[F-JEXAM-R010](#F-JEXAM-R010). La comparación de la tanda sigue como en [F-PROYU-R003](#F-PROYU-R003): no le paga a
claude -p, y lo que el cierre juzgó lo encuentra en el libro.

Los jueces locales siguen como en [F-RONDA2-R002](#F-RONDA2-R002) al comparar: lo que le preguntan a su modelo local no
tiene tope y no gasta ningún tope del cierre ([F-MLOCAL-R008](#F-MLOCAL-R008)); sin Ollama, lo que le preguntan a
claude -p se paga como lo de cualquier juez [ASSUMPTION].

El informe propone el pedido con lo que falta, por ejemplo: «para decidir lo que falta: `batch close --budget
translation-sense=<F>,mini-theory=<F>`».

**Criterio**: con `--budget translation-sense=1000 --cap 1200`, `translation-sense` gasta hasta 1.000 y los demás jueces
aprobados comparten lo que quede de los 1.200. Con `--budget quiz-instruction=600` el pedido se acepta, pero el juez de
consigna no consulta: no rindió su examen ([F-CJUEZ-R004](#F-CJUEZ-R004)). `--budget consigna=100` se rechaza: no es el
nombre de un juez de la tanda.

**Error**: "«consigna» no es un juez de la tanda <id>. Sus jueces: <lista>." [ASSUMPTION]

**Error**: "El tope tiene que ser 0 o más: <pedido>." [ASSUMPTION]

</details>

<a id="F-CJUEZ-R004"></a>
### Rule[F-CJUEZ-R004] - Un juez sin examen aprobado bloquea, y el veredicto lo nombra
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un juez cuya versión vigente reprobó su examen o no lo rindió ([F-JEXAM-R003](#F-JEXAM-R003)) no consulta, con
> cualquier tope. Si lee algo de lo que la tanda cambió —un ejercicio, o su tema en el de miniteoría—:
> 1. sus pisos sobre lo que la tanda cambió no se deciden, aunque el libro tenga un veredicto viejo de ese contenido, y
>    los de «algún error» tampoco;
> 2. el veredicto de la tanda lo nombra al final de su detalle, después de todo lo demás: «juez sin aprobar: <nombre
>    visible>», cada juez una vez; un veredicto que no pasa por otra cosa sigue «no pasa» y lo nombra igual;
> 3. la tanda no se puede publicar mientras tanto, y el rechazo de publicar copia ese detalle.
>
> Vale también para una herramienta local con examen, como la gramática. Un juez que no lee nada de lo que la tanda
> cambió no bloquea.

<details><summary>Detail</summary>

Endurece [F-JEXAM-R003](#F-JEXAM-R003) dentro de la tanda: un juez sin aprobar sigue reusando lo registrado, pero lo
reusado de lo que la tanda cambió no decide ningún piso. Cada ejercicio que la tanda cambió y ese juez lee cuenta como
sin evaluar para él y para «algún error» ([F-METAS-R002](#F-METAS-R002)); si ningún piso falla, el veredicto es «no se
puede decidir» ([F-METAS-R008](#F-METAS-R008)). Publicar ya exige «pasa» ([F-TANDA-R009](#F-TANDA-R009),
[F-BANDEJA-R008](#F-BANDEJA-R008)); lo que suma esta regla es que el veredicto y el rechazo digan por qué.

El nombre visible es el de [F-FILAS-R001](#F-FILAS-R001); sin él, el nombre único. Los nombres van en el orden del
catálogo. La herramienta local rinde su examen como en [F-MLOCAL-R006](#F-MLOCAL-R006). Un juez que no lee nada de lo que
cambió —el de opción múltiple en una tanda sólo de huecos— no bloquea.

**Criterio**, con la tanda de A1 y los exámenes de la foto del 1/10:

| Juez | Examen | En el veredicto |
|---|---|---|
| `quiz-instruction` | sin examen | Consigna que no se cumple |
| `multiple-choice-validity` | reprobado | Opción múltiple mal armada |
| `translation-sense` | aprobado | — |
| `single-answer` | reprobado | Más de una respuesta válida |
| `mini-theory` | aprobado | — |
| `grammar` | reprobado | Error de gramática |
| `naturalness` | aprobado | — |
| `near-duplicates` | reprobado | Ejercicio casi repetido |

El detalle del veredicto termina con «juez sin aprobar: Consigna que no se cumple, Opción múltiple mal armada, Más de una
respuesta válida, Error de gramática, Ejercicio casi repetido», y publicar se rechaza con ese mismo detalle. Ningún tope
lo cambia.

</details>

<a id="F-CJUEZ-R005"></a>
### Rule[F-CJUEZ-R005] - El informe dice, por juez, lo gastado, lo reusado y los pisos decididos
**Severity**: major | **Validation**: AUTO_VALIDATED

> La salida del cierre y `batch report` dan, por cada juez que lee lo que la tanda cambió: su nombre visible, cuántos
> ejercicios o temas juzga —antes y después—, las consultas gastadas en ese cierre, las reusadas del libro, las que
> faltan, el tope que tuvo, sus pisos decididos y sin decidir en la comparación, y si está sin aprobar, con el motivo.

<details><summary>Detail</summary>

«Reusadas» es lo que el libro ya tenía al empezar el cierre; «faltan», lo que falta después de comparar: lo que un juez
local juzgó al comparar ya no falta. Los pisos son las filas de la comparación de ese juez que tienen un piso
([F-METAS-R002](#F-METAS-R002)). El tope que tuvo es el suyo de `--budget`, lo que le quedó del de la tanda cuando le
tocó, o ninguno. Los jueces van con su nombre visible ([F-FILAS-R001](#F-FILAS-R001)), en el orden del catálogo.
`batch report` da las cuentas del último cierre. Sigue a [F-TANDA-R010](#F-TANDA-R010), que ya da las
consultas por puesto.

**Criterio**: cerrada sin tope, la tanda de A1 da una línea por cada uno de los ocho jueces que leen lo que cambió: los
tres aprobados con 0 gastadas, sus reusadas, las que faltan, sin tope y sus pisos decididos y sin decidir; los cinco sin
aprobar, con «sin examen» o «reprobado». `batch report` da las mismas.

</details>

## Contexto

**Lo que pidió José (1/10).** «No publiques sin los jueces»; «al cerrar la tanda corre a los jueces». Hoy `batch close`
recalcula el curso y compara ([F-TANDA-R006](#F-TANDA-R006)), pero no le paga a ningún juez: los jueces sólo reusan lo
que está en el libro ([F-PROYU-R003](#F-PROYU-R003)). FEAT-TANDA ya preveía pagar al cerrar «sólo lo que cambió y nadie
juzgó»; faltaban el tope para hacerlo, la cuenta antes de pagar, el antes, y qué pasa con un juez sin aprobar.

**La tanda de A1 de prueba** (control del paquete E sobre una copia, 1/10). Cambió 601 ejercicios en 30 temas, todos por
reglas: sin tope, la mesa no corrió y ningún juez vio esos cambios. Quedó «no se puede decidir», con «28 pisos sin
decidir» y «601 cambios sin evaluar por algún analizador», sin decir qué juez faltaba ni por qué.

**Por qué alcanza con lo que cambió, y por qué antes y después.** Los pisos de los jueces —todos de la familia de
errores— se deciden contra el inicio de la tanda, y sólo quedan abiertos por ejercicios que la tanda cambió y el juez no
evaluó ([F-METAS-R002](#F-METAS-R002)); el resto del curso tiene el mismo contenido y reusa lo que el libro ya tiene. Un
cambio vuelve si gana un hallazgo que no tenía ([F-METAS-R003](#F-METAS-R003)): sin juzgar el antes, todo lo que el juez
marque en el después parece nuevo.

**Cuánto cuesta en A1.** Como mucho, 2.404 consultas de `translation-sense` (601 ejercicios, antes y después, dos jueces)
y 60 de `mini-theory` (30 temas, antes y después), menos lo que el libro ya tenga: unas 13 horas de a una, unas 3,3 con 4
a la vez.

**Pagar no alcanza para publicar A1.** De los ocho jueces que leen lo que la tanda cambió, a la foto del 1/10 sólo tres
tienen su examen aprobado: `translation-sense`, `mini-theory` y `naturalness`. Mientras `quiz-instruction` no rinda el
suyo y `single-answer`, `multiple-choice-validity`, `grammar` y `near-duplicates` no aprueben una versión nueva, la tanda
no se publica, con cualquier tope ([F-CJUEZ-R004](#F-CJUEZ-R004)). Es lo que pidió José.

## Alcance

- **Adentro**: R001 a R005: qué juzga el cierre, la estimación antes de pagar, el tope, el juez sin aprobar y las cuentas
  por juez del informe.
- **Afuera**:
  - qué pregunta cada juez y su examen (FEAT-JEXAM y los de cada juez);
  - la mesa (FEAT-MESA);
  - la comparación misma, salvo nombrar al juez sin aprobar (FEAT-METAS);
  - la bandeja (FEAT-BANDEJA);
  - cambiar la regla de reuso del libro (FEAT-EVCOST).

## User Journeys

### Journey[F-CJUEZ-J001] - Cerrar la tanda con los jueces
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-CJUEZ-J001
    name: Cerrar la tanda con los jueces
    flow:
      - id: estimar
        action: "José pide la estimación de la tanda, que ya corrió sus reglas; dice por juez cuántas consultas reusa y cuántas faltan, sin consultar"
        gate: [F-CJUEZ-R001, F-CJUEZ-R002]
        outcomes:
          - when: "José cierra sin tope"
            then: ensayo
          - when: "José cierra con un tope por juez o de la tanda"
            then: pagar
      - id: ensayo
        action: "El cierre no le paga ninguna consulta a ningún juez, los pisos que dependen de lo que falta quedan sin decidir y el informe dice cuántas consultas faltan por juez"
        gate: [F-CJUEZ-R003, F-CJUEZ-R005]
        result: failure
      - id: pagar
        action: "El cierre juzga con cada juez aprobado lo que la tanda cambia, antes y después, hasta su tope, y reusa lo que el libro ya tiene"
        gate: [F-CJUEZ-R001, F-CJUEZ-R003]
        outcomes:
          - when: "Un juez que lee lo que la tanda cambió no tiene su examen aprobado"
            then: bloqueo
          - when: "Todos los jueces que leen lo que cambió están aprobados"
            then: informe
      - id: bloqueo
        action: "Ese juez no consulta, sus pisos quedan sin decidir, el veredicto dice «juez sin aprobar» con su nombre visible y la tanda no se puede publicar"
        gate: [F-CJUEZ-R004]
        result: failure
      - id: informe
        action: "El informe dice por juez las consultas gastadas, las reusadas, las que faltan y los pisos decididos"
        gate: [F-CJUEZ-R005]
        result: success
```

## Open Questions

<a id="DOUBT-CJUEZ-VERSION"></a>
### Doubt[DOUBT-CJUEZ-VERSION] - ¿Un veredicto del libro de otra versión del juez cuenta para decidir?
**Status**: RESOLVED (2026-10-18)

El libro reusa un veredicto por contenido, sea cual sea la versión del juez que lo dio. Importa porque, en una tanda, el
antes puede venir del libro, juzgado por una versión vieja, y el después, de la vigente: si las dos juzgan distinto, el
cambio puede volver —o pasar— por el juez y no por el ejercicio.

- [x] Opción A (recomendada, la de hoy): sí, se reusa como en todo el curso ([F-EVCOST-R006](#F-EVCOST-R006)), y el
  informe sigue mostrando las versiones ([F-JEXAM-R001](#F-JEXAM-R001), inv. 2).
- [ ] Opción B: el cierre sólo cuenta los veredictos de la versión vigente y vuelve a pagar los demás; cambia
  [F-EVCOST-R006](#F-EVCOST-R006) dentro de la tanda.

**Answer**: **Opción A, con marca**. La resolvieron las decisiones 1 y 3 de José del 18/10, en la propuesta «Jueces
al día». El cierre sigue reusando lo que juzgó otra versión, pero cada veredicto dice si está al día
([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003)) y el informe dice cuánto de lo reusado es de antes
([F-ALDIA-R005](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R005)). Publicar no cuenta lo de antes, salvo que José lo pida por nombre
([F-ALDIA-R008](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R008), [F-ALDIA-R009](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R009)). Un juez sin examen aprobado sigue sin decidir
nada ([F-CJUEZ-R004](#F-CJUEZ-R004)).

## References

- **FEAT-TANDA** — La tanda que se cierra: su costo y su tope ([F-TANDA-R003](#F-TANDA-R003)), el recálculo al cerrar
  ([F-TANDA-R006](#F-TANDA-R006)), la publicación ([F-TANDA-R009](#F-TANDA-R009)) y el informe
  ([F-TANDA-R010](#F-TANDA-R010)). Citado por R002 a R005, y en el Recorrido y el Contexto.
- **FEAT-METAS** — El estado de cada piso, lo que vuelve y el veredicto de la tanda ([F-METAS-R002](#F-METAS-R002),
  [F-METAS-R003](#F-METAS-R003), [F-METAS-R008](#F-METAS-R008)). Citado por R001, R004 y R005, y en el Contexto.
- **FEAT-JEXAM** — El examen por versión, el costo sin consultar, el conjunto, las consultas a la vez, el tope 0 sin
  pedido y la sesión vencida ([F-JEXAM-R001](#F-JEXAM-R001), [F-JEXAM-R003](#F-JEXAM-R003) a
  [F-JEXAM-R006](#F-JEXAM-R006), [F-JEXAM-R010](#F-JEXAM-R010), [F-JEXAM-R011](#F-JEXAM-R011)). Citado por R001 a R004 y
  por la duda.
- **FEAT-EVCOST** — El reuso por contenido, de cualquier versión ([F-EVCOST-R003](#F-EVCOST-R003),
  [F-EVCOST-R006](#F-EVCOST-R006)). Citado por R001 y por la duda.
- **FEAT-PROYU** — La proyección no le paga a claude -p y declara lo no evaluado ([F-PROYU-R003](#F-PROYU-R003)). Citado
  por R003 y en el Contexto.
- **FEAT-RONDA2, FEAT-MLOCAL** — El tope según a quién se consulta, lo local que es gratis y el examen de una herramienta
  local ([F-RONDA2-R002](#F-RONDA2-R002), [F-MLOCAL-R006](#F-MLOCAL-R006), [F-MLOCAL-R008](#F-MLOCAL-R008)). Citado por
  R002, R003 y R004.
- **FEAT-FILAS** — El nombre visible de cada juez ([F-FILAS-R001](#F-FILAS-R001)). Citado por R004 y R005.
- **FEAT-BANDEJA** — Publicar exige «pasa» ([F-BANDEJA-R008](#F-BANDEJA-R008)). Citado por R004.
