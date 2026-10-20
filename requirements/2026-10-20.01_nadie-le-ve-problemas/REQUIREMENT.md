---
feature:
  id: FEAT-NADIE
  code: F-NADIE
  name: Nadie le ve problemas - Correcciones junta los ejercicios que los jueces y los analizadores dan por buenos, para validar a los jueces
  priority: high
---

# Nadie le ve problemas: Correcciones junta los ejercicios que los jueces y los analizadores dan por buenos

## TL;DR

**Qué**: En Correcciones, un filtro nuevo, «Nadie le ve problemas», junta los ejercicios de la corrección en los que
ningún analizador y ningún juez que opinó encontró nada. Se recorren uno por uno, y en cada uno se ve qué dijo cada
juez: «dijo: sin error», con qué versión, si se abstuvo o si todavía no opinó. Debajo del filtro, una fila por cada
combinación de jueces que falta, para ver aparte los que vieron todos. Si José ve un problema que ningún juez marcó,
lo anota como Problema desde el mismo ejercicio, tenga o no cambios: el caso queda con el estado y con lo que dijo cada
juez, y desde el Problema llega a la recalibración del juez.

**Por qué**: José quiere validar lo que los jueces dan por bueno. Lo que está mal para los jueces se sigue mandando a
corregir. Si algo está bien para los jueces y mal para él, hay que corregir al juez: lo revisa con este filtro y, si no
está de acuerdo, recalibra al juez hasta que lleguen a la misma conclusión. Hoy no hay dónde verlos: «Lo dan por
bueno» es de los **cambios** que los jueces dan por resueltos (3 en la tanda A1), no de los ejercicios. Los 311
ejercicios de la tanda que no tuvieron ninguna tarea ni siquiera aparecen en Correcciones.

El tablero es `pipeline/vocabulary_dashboard` en ittm. Las palabras **juez**, **al día**, **de antes** y **sin medir**
son las de [FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md). Las marcas son las de
[FEAT-TALDIA](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md).

## Recorrido de uso (estado actual)

1. José abre Correcciones de la tanda A1 ✓.
2. Toca «Lo dan por bueno» ⚠️: ve 3 cambios, los que arreglaron lo suyo. No ve los ejercicios que nadie marcó.
3. Busca un ejercicio que no tuvo tarea ⚠️: no está en la lista. Correcciones sólo trae los ejercicios del plan.
4. Abre un ejercicio sin corrección ⚠️: dice «Hallazgos sin corrección propuesta» con los hallazgos del plan, aunque el
   análisis de hoy no le encuentra nada. No dice qué juez opinó.

**Punto de fricción**: pasos 2 a 4. No hay forma de recorrer lo que los jueces dan por bueno.

**Cambio mínimo necesario**: un filtro por ejercicio, que traiga también los que no tuvieron tarea, y en cada ejercicio
lo que dijo cada juez.

## Reglas de Negocio

Un **analizador** es lo que mide sin modelo: largo de la oración, palabras poco vistas, palabras de otro nivel, datos
coherentes, duplicados. Un **juez** es un analizador que consulta a un modelo; los jueces de cada estado son los que
cuenta content-audit en el análisis vigente. La **corrección** es el estado «Corrección N» del análisis vigente: el
curso con los cambios propuestos.

<a id="F-NADIE-R001"></a>
### Rule[F-NADIE-R001] - Qué ejercicio no tiene problemas para nadie
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Un ejercicio de la tanda entra en «Nadie le ve problemas» si, en la corrección del análisis vigente, se cumplen las
> cuatro condiciones:
> 1. no tiene ningún hallazgo, de ningún analizador ni de ningún juez, de ninguna gravedad. Un hallazgo de una opinión
>    de antes también cuenta;
> 2. ningún analizador que no es juez lo dejó sin evaluar o evaluado a medias;
> 3. al menos un juez opinó de él con su versión de hoy, sin abstenerse;
> 4. lo que se midió es el contenido de hoy: si el ejercicio se editó después de medir, no entra.
>
> Invariantes:
> 1. un juez que todavía no opinó, que se abstuvo o que opinó con una versión anterior nunca cuenta como que lo da por
>    bueno. El ejercicio puede entrar por los demás, pero ese juez se nombra aparte ([F-NADIE-R003](#F-NADIE-R003));
> 2. un juez que no se aplica al ejercicio (no lo alcanza, o content-audit lo da por «no aplica») no se cuenta ni se
>    nombra;
> 3. si el ejercicio tiene cambios propuestos, lo que se juzga es el después: el ejercicio con los cambios;
> 4. entran los ejercicios con cambios y sin cambios, también los que no tuvieron ninguna tarea en el plan;
> 5. la marca de cada juez (al día, de antes, abstención) es la que manda content-audit. El tablero no la calcula;
> 6. sin análisis vigente de la corrección no hay filtro: el botón se ve apagado y dice por qué;
> 7. armarlo no consulta a ningún modelo.

<details><summary>Detail</summary>

La condición 3 es lo que José pidió: «para todos los jueces que opinaron». Pedir que opinen todos los jueces dejaba,
el 20/10, 3 ejercicios de 2.402: Calidad didáctica A1 no midió 2.377 y lo que dijo Miniteoría es, en 2.382, de una versión de antes. Por eso
la versión estricta no es el filtro: es su primera fila ([F-NADIE-R003](#F-NADIE-R003)).

Una opinión de antes no basta para entrar, como no basta para aprobar en grupo
([F-TALDIA-R006](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R006)): puede no decir lo mismo
hoy. Pero si encontró un error, el error está en el análisis y el ejercicio no entra.

La condición 4 es la misma de «Otro contenido»: lo que dijeron los jueces es de otra versión del ejercicio.

**Criterio**, con la tanda A1 del 20/10 (Corrección 1, 2.402 ejercicios): entran 1.111. De ésos, 793 tienen cambios
propuestos, 7 ya estaban en la lista sin cambio y 311 no tenían ninguna tarea.

</details>

<a id="F-NADIE-R002"></a>
### Rule[F-NADIE-R002] - El filtro en Correcciones recorre esos ejercicios uno por uno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En el navegador de Correcciones, debajo de los chips de «Según los jueces», va el botón «Nadie le ve problemas» con
> cuántos ejercicios entran y dónde: «1.111 ejercicios · Corrección 1». Invariantes:
> 1. cuenta ejercicios, no cambios: no suma con los chips de «Según los jueces»;
> 2. elegirlo saca el motivo y los otros chips; elegir un motivo o un chip lo saca a él. «Todos» lo saca;
> 3. con el filtro puesto, la lista de temas muestra los que tienen algún ejercicio que entra, en el orden del curso,
>    con «Por revisar» o con «Todos»: estos ejercicios no tienen nada que decidir por sí mismos;
> 4. en un tema, la tira y las flechas pasan sólo por los ejercicios que entran, en el orden del curso;
> 5. un ejercicio que no tuvo tarea se ve con su contenido del curso, como uno sin cambio;
> 6. un ejercicio con cambios se ve como siempre, con el antes, el después y los botones para decidir;
> 7. sin el filtro, la lista es la de siempre: los ejercicios sin tarea no aparecen;
> 8. «Revisar en grupo» no se muestra con el filtro puesto: no hay nada para aprobar de una vez.

<details><summary>Detail</summary>

«Por revisar» deja de esconder temas con este filtro porque José no recorre decisiones pendientes: recorre lo que los
jueces dan por bueno, decidido o no.

**Criterio**: con la tanda A1, al tocar «Nadie le ve problemas» la lista muestra los temas en el orden del curso y
«Negativas con be» abre en su primer ejercicio que entra. Las flechas no se detienen en los que tienen hallazgos.

</details>

<a id="F-NADIE-R003"></a>
### Rule[F-NADIE-R003] - Debajo del filtro, quién falta
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Con el filtro puesto, el navegador muestra «Quién falta», con una fila por combinación de jueces que no lo dio por
> bueno con su versión de hoy, y cuántos ejercicios tiene cada una. Invariantes:
> 1. la primera fila es «Opinaron todos los jueces»: los que vieron todos los jueces que se aplican, con su versión de
>    hoy y sin abstenerse;
> 2. cada otra fila nombra a cada juez que falta y por qué: «Falta Calidad didáctica A1», «Miniteoría con error opinó
>    con la versión del 5/10», «Pragmatismo comunicativo se abstuvo (no aplica)». Dos versiones de antes distintas son dos
>    filas;
> 3. las filas no se pisan: cada ejercicio está en una sola, y suman el total del filtro;
> 4. tocar una fila acota la lista a esos ejercicios; tocarla otra vez la suelta;
> 5. van de la más numerosa a la menos, con «Opinaron todos los jueces» siempre primera.

<details><summary>Detail</summary>

Es la misma idea que «Por qué quedan afuera»: con la cuenta a la vista, José elige si recorre sólo lo que vieron
todos o también lo que vieron casi todos.

**Criterio**, con la tanda A1 del 20/10: «Opinaron todos los jueces» tiene 3. La fila más numerosa nombra a Calidad
didáctica A1 (falta) y a Miniteoría con error (opinó con la versión del 5/10).

</details>

<a id="F-NADIE-R004"></a>
### Rule[F-NADIE-R004] - En cada ejercicio, lo que dijo cada juez
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el filtro puesto, cada ejercicio que entra lleva arriba el bloque «Nadie le ve problemas en Corrección N», con
> un renglón por juez que se aplica, en el orden del catálogo:
> 1. «dijo: sin error» con la marca «al día», si opinó con su versión de hoy;
> 2. «dijo: sin error» con la marca ámbar de su versión («versión del 5/10»), si opinó con una de antes;
> 3. «se abstuvo» con el motivo que da content-audit («no aplica», «falta contexto»);
> 4. «todavía no opinó», con la marca gris de «sin medir».
>
> Invariantes:
> 1. después de los jueces, una línea nombra los analizadores que lo evaluaron sin observaciones;
> 2. cada juez lleva «Ver su examen», que abre su examen en Exámenes: es donde se recalibra;
> 3. si el ejercicio tiene cambios propuestos, el bloque dice que lo que opinaron es del después;
> 4. en un ejercicio sin cambio que entra, no se muestran los hallazgos que traía el plan: el análisis de hoy no los
>    tiene;
> 5. mostrarlo no consulta.

<details><summary>Detail</summary>

Lo que un juez «dijo» de un ejercicio sin error es que no le encontró nada: el análisis guarda los hallazgos, no un
texto de aprobación. Por eso el renglón dice «sin error» y no inventa un motivo.

**Criterio**: en un ejercicio de «Negativas con be» que entra, el bloque dice «Consigna que no se cumple: dijo: sin
error · al día», «Miniteoría con error: dijo: sin error · versión del 5/10» y «Calidad didáctica A1: todavía no opinó · sin
medir». Abajo: «Sin observaciones de los analizadores: …».

</details>

<a id="F-NADIE-R005"></a>
### Rule[F-NADIE-R005] - Anotar un problema que ningún juez marcó, aunque el ejercicio no tenga cambios
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En un ejercicio que entra en «Nadie le ve problemas», José puede anotar un Problema aunque el ejercicio no tenga
> cambios propuestos o no haya tenido ninguna tarea. El bloque de [F-NADIE-R004](#F-NADIE-R004) lleva «Anotar un
> problema» (también con **P**), que abre el cajón de siempre: elegir un Problema o crear uno. El caso queda en ese
> Problema con:
> 1. el ejercicio y la tanda;
> 2. el estado del que sale y su nombre: «Corrección 1»;
> 3. el ejercicio congelado como está en ese estado. Sin cambios, el antes y el después son el mismo;
> 4. lo que dijo cada juez que se aplica, como lo muestra el bloque: los que dijeron «sin error», al día o con una
>    versión de antes, son **los que no lo vieron**; el que se abstuvo o todavía no opinó queda nombrado con su estado y
>    su motivo, sin contarlo entre los que no lo vieron;
> 5. los analizadores que lo evaluaron sin observaciones.
>
> Invariantes:
> 1. sin cambios no hay corrección que rechazar: el cajón no ofrece «Rechazar la corrección al anotar», anotar no
>    cambia ninguna decisión y se puede anotar también con la tanda publicada;
> 2. en el cajón, antes de anotar, José ve lo que va a quedar anotado: el estado y lo que dijo cada juez;
> 3. lo que dijo cada juez se toma del análisis vigente en el momento de anotar, no de lo que quedó en la pantalla.
>    Si el ejercicio ya no entra en el filtro, no se anota nada y se pide volver a leer;
> 4. un ejercicio que entra y tiene cambios se anota como siempre (el antes, el después y la corrección, que se puede
>    rechazar), y el caso lleva además lo que dijo cada juez, que es del después;
> 5. un ejercicio sin cambios que no entra en el filtro no se puede anotar: el caso no tendría ni corrección ni lo que
>    dijeron los jueces;
> 6. anotar otra vez el mismo ejercicio, con el mismo contenido, en el mismo Problema no lo repite; «Deshacer
>    anotación» lo saca;
> 7. en Problemas, el caso dice de qué estado sale y quiénes no lo vieron; el pedido para el agente del Problema trae
>    las guías de esos jueces;
> 8. anotar no consulta a ningún modelo.

<details><summary>Detail</summary>

Lo que un juez dijo es lo que hay que recalibrar: el Problema junta los casos que el juez debió marcar, el agente
ajusta al juez y el examen fijo de ese juez sale de la entrega del Problema. Por eso el caso no se guarda aparte: va
al mismo registro de Problemas que los casos con corrección, y el agente lo recibe con todo lo demás.

Un juez que se abstuvo o que todavía no opinó no se equivocó: no hay nada que recalibrar en él por este ejercicio.
Queda nombrado para que se sepa que no lo vio nadie más, no para corregirlo.

La invariante 3 es la de «Otro contenido»: la pantalla puede haber quedado de antes de una medición nueva, y lo que se
guarda tiene que ser lo que dice el análisis de hoy.

**Criterio**: con la tanda A1, en «Negativas con be», un ejercicio sin tarea que entra: José toca «Anotar un
problema», ve «Corrección 1 · no lo vieron: Consigna que no se cumple (al día), Miniteoría con error (versión del
5/10) · todavía no opinó: Calidad didáctica A1», crea «Negación sin contraste» y anota. En Problemas, el caso dice lo
mismo, con el ejercicio igual antes y después.

</details>

## Journeys

```yaml
journeys:
  - id: F-NADIE-J001
    name: José valida lo que los jueces dan por bueno
    flow:
      - id: abrir
        action: "José abre Correcciones de la tanda y toca «Nadie le ve problemas»"
        gate: [F-NADIE-R001, F-NADIE-R002]
        outcomes:
          - when: "Hay análisis vigente de la corrección"
            then: elegir
          - when: "No hay análisis vigente de la corrección"
            then: apagado
      - id: apagado
        action: "El botón está apagado y dice por qué"
        gate: [F-NADIE-R001]
        result: failure
      - id: elegir
        action: "José mira «Quién falta» y elige una fila, o recorre todos"
        gate: [F-NADIE-R003]
        outcomes:
          - when: "José eligió qué recorrer"
            then: recorrer
      - id: recorrer
        action: "José pasa de un ejercicio al siguiente y lee lo que dijo cada juez"
        gate: [F-NADIE-R002, F-NADIE-R004]
        outcomes:
          - when: "José está de acuerdo con los jueces"
            then: recorrer
          - when: "José ve un problema que ningún juez marcó"
            then: anotar
      - id: anotar
        action: "José anota el problema en el ejercicio, tenga o no cambios"
        gate: [F-NADIE-R005]
        outcomes:
          - when: "El ejercicio sigue entrando en el filtro"
            then: recalibrar
          - when: "El ejercicio ya no entra en el filtro"
            then: releer
      - id: releer
        action: "No se anota nada: el tablero pide volver a leer"
        gate: [F-NADIE-R005]
        result: failure
      - id: recalibrar
        action: "El caso queda en el Problema con el estado y los jueces que no lo vieron; José abre el examen del juez que debió marcarlo"
        gate: [F-NADIE-R004, F-NADIE-R005]
        result: success
```

## Open Questions

No quedan abiertas. El 20/10 content-audit dejó de mirar el motor: sólo la versión decide si una opinión está al día
([F-ALDIA-R003](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#F-ALDIA-R003), invariante 7). Este filtro lee la marca
que manda content-audit, así que lo siguió solo: lo que antes decía «otro motor» ahora dice la versión que opinó.

## Cómo se prueba

Las pruebas usan respuestas armadas a mano del análisis vigente. Ninguna consulta a un modelo.

| Regla | Qué se comprueba |
|---|---|
| R001 | Entra uno sin hallazgos con un juez al día. Quedan afuera: uno con un hallazgo de cualquier gravedad, uno con un hallazgo de una opinión de antes, uno con un analizador sin evaluar, uno en el que ningún juez opinó al día, uno medido sobre otro contenido. Un juez que no opinó, se abstuvo u opinó con otra versión queda nombrado con su estado, y uno que no se aplica no se nombra. Sin estado de corrección no hay filtro |
| R002 | El botón cuenta ejercicios. Elegirlo saca el motivo; elegir un motivo lo saca. Con el filtro, la lista muestra los temas con ejercicios que entran también con «Por revisar», y la tira pasa sólo por ellos, incluido uno sin tarea. Sin el filtro, el tema sin tarea no aparece y «Revisar en grupo» vuelve |
| R003 | «Opinaron todos los jueces» va primera; dos versiones de antes son dos filas; las filas suman el total; tocar una acota y tocarla otra vez suelta |
| R004 | El bloque dice «dijo: sin error» con la marca de cada versión, «se abstuvo (motivo)», «todavía no opinó», los analizadores sin observaciones y «Ver su examen» por juez. Con cambios, dice que es del después. En un ejercicio sin cambio no aparecen los hallazgos del plan |
| R005 | En un ejercicio sin cambios que entra, «Anotar un problema» abre el cajón sin «Rechazar la corrección» y con el estado y lo que dijo cada juez; anotar manda que viene del filtro y no pide rechazar. El tablero toma lo que dijo cada juez de su vista vigente y no anota si el ejercicio ya no entra. content-audit congela el ejercicio igual antes y después, con el estado, los que no lo vieron y los que no cuentan; no cambia ninguna decisión, también con la tanda publicada; no repite el caso; sin lo que dijeron los jueces no anota un ejercicio sin corrección; el pedido trae las guías de los jueces que no lo vieron. Con cambios, el caso es el de siempre más lo que dijo cada juez. En Problemas, el caso dice el estado y quiénes no lo vieron |

## References

- **FEAT-ALDIA**: la marca de cada opinión ([F-ALDIA-R005](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md)) y las
  abstenciones.
- **FEAT-TALDIA**: las marcas en el tablero ([F-TALDIA-R001](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R001))
  y por qué lo de antes no cuenta para el grupo ([F-TALDIA-R006](../2026-10-18.02_jueces-al-dia-en-el-tablero/REQUIREMENT.md#F-TALDIA-R006)).
