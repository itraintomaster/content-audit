---
feature:
  id: FEAT-PUBPROD
  code: F-PUBPROD
  name: Publicar a producción - la tanda deja la lista exacta de lo que puede llegar a producción, y sólo eso llega
  priority: critical
---

# Publicar a producción: la tanda deja la lista exacta de lo que puede llegar a producción, y sólo eso llega

## TL;DR

**Qué**: Publicar una tanda deja, antes de escribir el curso, la lista exacta de lo que puede llegar a producción, con el
antes y el después de cada campo. Lo que José rechazó al revisar no entra, sólo «pasa» publica, y llevar la lista a
producción es un paso aparte de José que toma sólo lo de la lista, ensaya antes, verifica lo escrito y deja registro.

**Por qué**: Hoy lo que José rechaza en la revisión entra igual al curso; la bandeja publica con «no se puede decidir»
si la compuerta pasa; y el paso a producción toma los valores del curso en el disco tal como estén ese día, publica
cualquier propuesta aprobada de un plan, salta la opción múltiple y no deja ni el destino ni lo que pisó.

## Recorrido de uso (estado actual)

Cómo José lleva hoy una tanda hasta producción. El caso: una tanda de A1 cerrada, con la muestra ciega respondida.

1. Revisa los cambios en Correcciones → aprueba o rechaza cada paso ([F-TANDA-R007](#F-TANDA-R007)). ✓ Disponible, pero
   ⚠️ nadie lee esas decisiones al publicar: lo rechazado entra al curso igual.
2. Cierra → `batch close`. ✓ Disponible, pero ⚠️ con «no se puede decidir» dice «La compuerta de "no empeorar" pasa» y
   termina bien.
3. Publica → `tray publish`. ✓ Disponible, pero ⚠️ la bandeja exige la compuerta y no el veredicto, y publicar exige el
   veredicto y no la compuerta: con «no se puede decidir» y la compuerta en verde, publica. No queda ninguna lista de lo
   que cambió.
4. Lleva el curso a producción → `scripts/publish_revisions.py --plan <id>`. ⚠️ Toma los valores del curso en el disco
   —que una tanda posterior o una edición a mano pudieron cambiar—, publica cualquier propuesta aprobada del plan, salta
   toda la opción múltiple, no lleva consigna ni miniteoría, y su informe no dice contra qué servidor corrió ni guarda lo
   que pisó.

**Punto de fricción**: pasos 3 y 4, publicar y llevar a producción.

**Cambio mínimo necesario**: que publicar deje la lista exacta, sólo con «pasa» y sin lo rechazado; y que el paso a
producción tome sólo esa lista, ensaye antes de escribir, verifique lo escrito y deje registro de cada corrida.

## Reglas de Negocio

Acá, **producción** es la base de Learney que usan los alumnos. Un **campo** es un dato de un ejercicio o de un tema tal
como lo guarda producción: la oración guardada, la traducción, las partes del enunciado, la consigna. La **lista** es la
lista de publicación de una tanda ([F-PUBPROD-R001](#F-PUBPROD-R001)); su **huella** es la del archivo exacto, byte por
byte. Un **rechazo vigente** es la última decisión de José sobre un paso de un cambio en un ejercicio, «rechazado», que
todavía coincide con el antes y el después del paso ([F-TANDA-R007](#F-TANDA-R007)).

<a id="F-PUBPROD-R001"></a>
### Rule[F-PUBPROD-R001] - Publicar una tanda deja la lista exacta de lo que puede llegar a producción
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al publicar, antes de escribir el curso, la tanda deja la lista de lo que puede llegar a producción: por cada ejercicio
> o tema que cambió, los campos que cambiaron, cada uno con su valor de antes y de después, y los cambios y propuestas
> que lo originaron. Invariantes:
> 1. el después de cada campo es el del curso que se publica, y el antes, el del curso del que partió la tanda;
> 2. un campo que no cambió no está en la lista, y un ejercicio o tema sin campos cambiados tampoco;
> 3. si la lista no se puede dejar, no se publica y el curso no se toca;
> 4. la publicación registra la huella de la lista: una lista que cambió después ya no es la de la tanda;
> 5. un campo que la tabla de la corrección declara y no tiene lugar en producción frena la publicación y lo nombra;
> 6. lo que cambió y no se lleva a producción por este camino —un cambio de tipo de ejercicio, o de la opción múltiple
>    más allá del texto de sus opciones ([F-PUBPROD-R002](#F-PUBPROD-R002), inv. 4)— se declara aparte, con el cambio,
>    el ejercicio, la parte y el motivo «no se publica por script», y ese ejercicio no va en la lista.
> 7. la lista dice de qué tanda, plan y nivel sale, cuándo se publicó y quién la aprobó; si falta alguno de esos datos
>    la lista no se deja —como en el inv. 3— y la negativa nombra lo que falta.

<details><summary>Detail</summary>

La lista queda en `.content-audit/batch-publications/<tanda>/manifest.json`, junto al registro de la tanda; la huella
es el sha256 del archivo. Además de los ejercicios y temas, la lista dice el veredicto y la compuerta
([F-PUBPROD-R005](#F-PUBPROD-R005)), las cuentas de la revisión ([F-PUBPROD-R004](#F-PUBPROD-R004)), los cambios que
quedaron afuera con su motivo —rechazado al revisar, encadenado con uno que salió, regla que José sacó, o sacado por José
antes de publicar— y cuántos ejercicios y campos lleva, por campo. Como el después ya está en la lista, el paso a
producción nunca necesita leer el curso en el disco ([F-PUBPROD-R006](#F-PUBPROD-R006)).

Un cambio de tipo de ejercicio (por ejemplo, un tema que José manda a opción múltiple) no se lleva por este camino:
escribir las opciones nuevas sobre un ejercicio que en producción sigue siendo de otro tipo lo rompería.

**Criterio**: una tanda de A1 que cambió la traducción y la miniteoría de un ejercicio y la consigna de un tema publica
una lista con ese ejercicio —`translation` y `miniTheory`, cada uno con su antes y su después— y ese tema —
`instructions`—, y nada más; la publicación registra la huella de ese archivo.

</details>

<a id="F-PUBPROD-R002"></a>
### Rule[F-PUBPROD-R002] - Los campos que se llevan salen de la tabla de la corrección
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Los campos de la lista salen de la misma tabla de partes a campos que decide qué puede cambiar una corrección
> ([F-TAREA-R006](#F-TAREA-R006)): la oración guardada, la traducción, la consigna, la miniteoría, las partes del
> enunciado —enteras—, el texto de cada opción de la opción múltiple y la consigna del tema. Invariantes:
> 1. nunca el identificador del ejercicio o del tema, ni el tipo de ejercicio, ni el tipo, la incidencia, la etiqueta o
>    el nombre del formulario;
> 2. el título de los ejercicios y el nombre del tema no van en la lista: siguen su camino por plan; lo que se oye
>    tampoco: se regraba ([F-TANDA-R012](#F-TANDA-R012));
> 3. los valores van como los guarda producción: los números como números, un texto vacío como texto vacío —nunca
>    ausente— y la oración guardada ausente como una lista vacía;
> 4. de un ejercicio de opción múltiple —lo dice su propio tipo— sólo se publica, de su formulario, el texto de cada
>    opción: un cambio de su enunciado, de su oración guardada, de cuál es la correcta, de la forma de elegir, de la
>    cantidad de opciones o del identificador de una opción no se publica por script; se declara aparte, con cada
>    cambio que lo tocó, como un cambio de tipo ([F-PUBPROD-R001](#F-PUBPROD-R001), inv. 6), y ese ejercicio no va en
>    la lista; su traducción, su consigna y su miniteoría van como las de cualquier ejercicio.

<details><summary>Detail</summary>

| Lo que la corrección puede cambiar | Campo de producción |
|---|---|
| la oración guardada | `form.sentences` |
| la traducción | `translation` |
| la consigna del ejercicio | `instructions` |
| la miniteoría | `miniTheory` |
| las partes del enunciado, su texto y las respuestas de los huecos | `form.sentenceParts`, entero (salvo en la opción múltiple) |
| el texto de cada opción de la opción múltiple | `form.items`, entero, con sólo la etiqueta de cada opción cambiada |
| de la opción múltiple: el enunciado, la oración guardada, cuál es la correcta, la forma de elegir, la cantidad o el identificador de las opciones | no se publica por script (inv. 4) |
| la consigna del tema | `instructions` del tema |
| el nombre del tema, el título de los ejercicios | no va: camino por plan |
| lo que se oye | no va: regrabación |
| el tipo de ejercicio | no se publica por script ([F-PUBPROD-R001](#F-PUBPROD-R001), inv. 6) |

Una parte nueva de la tabla de la corrección sin su fila acá frena la publicación ([F-PUBPROD-R001](#F-PUBPROD-R001),
inv. 5): nada viaja sin lugar declarado.

**Criterio**: un cambio del texto de una opción de un ejercicio de opción múltiple de B2 lleva `form.items` entero,
con cada incidencia como número (`1.0`, no `{"$numberDouble": "1.0"}`) y sólo esa etiqueta cambiada; un cambio de cuál
es la correcta en ese ejercicio no va en la lista y queda declarado aparte; si en otro ejercicio de opción múltiple
cambiaron el enunciado y la traducción, ese ejercicio no va —tampoco su traducción— y el cambio del enunciado queda
declarado aparte; una parte del enunciado sin texto lleva `"text": ""`.

</details>

<a id="F-PUBPROD-R003"></a>
### Rule[F-PUBPROD-R003] - Lo que José rechazó en la revisión no se publica
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Al publicar, cada cambio con un rechazo vigente en alguno de sus pasos sale de la tanda entero, como si José lo sacara
> ([F-TANDA-R008](#F-TANDA-R008)). Invariantes:
> 1. salen con él los cambios posteriores del mismo ejercicio que tocaron lo mismo, y las tareas de todos vuelven a
>    pendiente;
> 2. la salida y la lista nombran los rechazados y los que salieron con ellos;
> 3. si salió algo, la tanda se vuelve a comparar y se publica sólo si sigue dando «pasa»
>    ([F-PUBPROD-R005](#F-PUBPROD-R005));
> 4. un rechazo sobre un contenido que cambió después —la decisión ya no coincide con el antes y el después del paso—
>    frena la publicación y la nombra, hasta que José lo vuelva a mirar;
> 5. si las decisiones de José no se pueden leer, no se publica;
> 6. si la publicación no termina, nada queda sacado: la tanda y sus propuestas quedan como estaban
>    ([F-PUBPROD-R010](#F-PUBPROD-R010));
> 7. sin rechazos, lo que se publica es lo mismo que antes de esta regla.

<details><summary>Detail</summary>

Sale el cambio entero porque un paso no se puede sacar solo de su cambio. Rechazar sigue sin tocar el curso por sí
mismo ([F-TANDA-R007](#F-TANDA-R007)): el rechazo se aplica al publicar. Que lo rechazado no entre es lo que hace que
la revisión de Correcciones sirva: hoy se guarda y nadie la lee.

**Criterio**: con una tanda de A1 que pasa y un rechazo vigente de José sobre el cambio `rule-3` en `…256f`, publicar
deja ese ejercicio como estaba, nombra `rule-3` como rechazado, vuelve a comparar y publica con «pasa»; con un rechazo
de `rule-3` que José dio antes de que saliera un cambio anterior del mismo ejercicio, publicar se niega y nombra
`rule-3` en `…256f`.

</details>

<a id="F-PUBPROD-R004"></a>
### Rule[F-PUBPROD-R004] - Lo que nadie revisó viaja y se cuenta
**Severity**: major | **Validation**: AUTO_VALIDATED

> Lo aprobado y lo que José no revisó viajan: la tanda ya se validó con la muestra ciega, los jueces y la compuerta
> ([F-TANDA-R009](#F-TANDA-R009), [F-BANDEJA-R008](#F-BANDEJA-R008)). Invariantes:
> 1. la publicación y la lista cuentan los pasos aprobados, los sin revisar y los rechazados;
> 2. cada ejercicio o tema de la lista dice si todos sus pasos están aprobados o si alguno quedó sin revisar;
> 3. un paso aprobado cuyo antes o después cambió al salir un rechazo cuenta como sin revisar;
> 4. ningún ejercicio o tema de la lista queda como rechazado.

<details><summary>Detail</summary>

La regla de la muestra de regla sin responder no cambia: una regla cuya muestra José no aprobó queda afuera
([F-BANDEJA-R004](#F-BANDEJA-R004), [F-BANDEJA-R007](#F-BANDEJA-R007)).

**Criterio**: una tanda de 3 pasos, uno aprobado, uno sin revisar y uno rechazado, publica con «1 aprobado, 1 sin revisar
(viajó), 1 rechazado (quedó afuera)»; el ejercicio del paso sin revisar va en la lista como «sin revisar».

</details>

<a id="F-PUBPROD-R005"></a>
### Rule[F-PUBPROD-R005] - Sólo «pasa» publica
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Publicar exige las dos cosas: que la compuerta de «no empeorar» del último cierre pase y que el veredicto de la tanda
> —comparada otra vez si cambió desde ese cierre— sea «pasa» ([F-METAS-R008](#F-METAS-R008)). Invariantes:
> 1. con «no se puede decidir» no se publica, aunque la compuerta pase; con un piso sin decidir, tampoco;
> 2. la negativa nombra el veredicto o la compuerta que falta;
> 3. la publicación y la lista registran el veredicto, si la compuerta pasó y los pisos sin decidir;
> 4. al cerrar con «no se puede decidir», la salida no dice que la tanda pasa: dice que no se puede decidir y termina con
>    error, con el cierre guardado;
> 5. «no se puede decidir» se dice sólo del veredicto que no se puede decidir; cualquier otro veredicto se nombra por lo
>    que es y sus pisos sin decidir se cuentan aparte, con el nombre de cada uno cuando se sabe: un «no pasa» con pisos
>    sin decidir se nombra «no pasa, y además N pisos sin decidir», porque ya se sabe que empeora el curso; un «pasa» con
>    pisos sin decidir se nombra «pasa, pero con N pisos sin decidir», no publica y su cierre termina con error. Las
>    negativas de publicar —desde la bandeja y desde la tanda— y la salida del cierre lo dicen con las mismas palabras.

<details><summary>Detail</summary>

La compuerta de «no empeorar» sigue diciendo «pasa» cuando ningún piso se rompe, aunque alguno no se pueda decidir
([F-NOEMP-R005](#F-NOEMP-R005)); por eso hacen falta las dos. Que el cierre termine con error y con el cierre guardado es
lo que el tablero ya lee como «resultado de la compuerta», no como una falla de la ejecución.

**Criterio**: una tanda cuya compuerta pasa y cuyo veredicto es «no se puede decidir: 1 piso sin decidir» no se
publica, ni desde la bandeja ni desde la tanda, y la negativa dice «no se puede decidir»; `batch close` sobre esa tanda
dice «no se puede decidir» y termina con 1. Si en cambio el veredicto es «no pasa» con 2 pisos sin decidir, la negativa
y el cierre dicen «no pasa, y además 2 pisos sin decidir», nunca «no se puede decidir».

</details>

<a id="F-PUBPROD-R006"></a>
### Rule[F-PUBPROD-R006] - El paso a producción toma sólo lo que dice la lista
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Llevar una tanda publicada a producción es un paso de José, aparte de publicar, que toma sólo lo de la lista de esa
> tanda. Se niega, sin conectarse a producción, si la tanda no está publicada, si la huella de la lista no es la que
> registró la publicación, si el veredicto registrado no es «pasa», si la compuerta no pasó, si hay un piso sin decidir,
> o si la lista trae un ejercicio rechazado, un campo prohibido ([F-PUBPROD-R002](#F-PUBPROD-R002), inv. 1) o un texto
> ausente. Invariantes:
> 1. escribe sólo esos campos, con el después de la lista, nunca con lo que diga el curso en el disco;
> 2. campo por campo: si producción tiene el antes, lo escribe; si ya tiene el después, ya estaba; si tiene otra cosa, es
>    deriva: no pisa nada y no escribe nada, salvo que José pida seguir sin esos ejercicios, que quedan registrados;
> 3. si la lista declara algo sin lugar en producción, se niega, salvo que José pida seguir sin eso;
> 4. una tanda publicada sin lista no se lleva por este camino;
> 5. el camino viejo por plan se niega a escribir un plan que es de una tanda.

<a id="F-PUBPROD-R007"></a>
### Rule[F-PUBPROD-R007] - Lo escrito se verifica
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Después de escribir, el paso a producción vuelve a leer cada ejercicio o tema escrito: cada campo tiene que tener el
> después de la lista, y todo lo demás tiene que haber quedado idéntico a como estaba. Si algo no coincide, termina con
> error y lo registra, sin reintentar ni corregir.

<a id="F-PUBPROD-R008"></a>
### Rule[F-PUBPROD-R008] - Cada corrida deja su renglón
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada corrida del paso a producción —ensayo, aplicación o negada— deja un renglón en el registro de producción, que
> sólo crece. Invariantes:
> 1. el destino se registra como servidor y base, nunca la dirección completa ni las credenciales;
> 2. el renglón dice la tanda, la huella de la lista, el modo, el resultado, los ejercicios y temas escritos, si se
>    verificó y cuántos pasos sin revisar viajaron; el valor previo de cada campo escrito queda en el detalle de la
>    corrida, como respaldo;
> 3. una aplicación deja un renglón de inicio antes de la primera escritura —si no puede dejarlo, no escribe— y uno de
>    fin;
> 4. los otros dos escritores de producción, el de los nombres y el de las consignas de los temas, también dejan su
>    renglón;
> 5. un ensayo que encuentra problemas —por ejemplo, un tema o un ejercicio que no está en la base— queda registrado
>    como ensayo con problemas y termina con error, en cualquiera de los escritores; sólo un ensayo limpio termina bien.

<a id="F-PUBPROD-R009"></a>
### Rule[F-PUBPROD-R009] - Escribir exige ensayar y nombrar el destino
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El ensayo hace todo menos escribir: no toca producción ni el curso. Aplicar exige un ensayo limpio previo de la misma
> lista contra el mismo destino, registrado, y que José nombre el destino al pedirlo: si el destino que nombra no es el
> real, no escribe. No hay forma de saltear el ensayo.

<a id="F-PUBPROD-R010"></a>
### Rule[F-PUBPROD-R010] - Una publicación que no termina no deja nada a medias
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Si publicar falla en cualquier punto —antes o después de escribir el curso, hasta guardar la tanda como publicada—,
> todo vuelve a como estaba y la falla sube tal cual. Invariantes:
> 1. el curso vuelve a ser el del que partió la tanda;
> 2. la lista que dejó ese intento se borra, o vuelve la que había antes; una lista que el intento no llegó a escribir no
>    se toca;
> 3. las propuestas, los cambios sacados, las tareas de la tanda y las del plan quedan como estaban;
> 4. el análisis vigente sigue siendo el de antes: el análisis del curso publicado que se alcanzó a guardar queda, pero no
>    es el vigente;
> 5. la tanda no queda publicada;
> 6. si algo no se puede volver atrás, la falla lo dice —qué quedó a medio hacer— junto con la falla original;
> 7. volver a publicar después de una falla deja la misma lista que si la falla no hubiera pasado.

<details><summary>Detail</summary>

Una tanda está publicada sólo cuando su registro lo dice, y eso es lo último que se guarda. El paso a producción exige
la tanda publicada y que la huella de la lista sea la que registró la publicación ([F-PUBPROD-R006](#F-PUBPROD-R006)):
aunque una lista quedara de un intento que no terminó, nunca pasaría por la de la tanda.

**Criterio**: una tanda de A1 que pasa y cuyo análisis del curso publicado no se puede guardar deja el curso como
estaba, sin lista, sin publicar y con sus propuestas pendientes; lo mismo si lo que falla es guardar la tanda; si además
el curso no se puede volver a escribir, la falla dice que el curso quedó a medio hacer; publicarla otra vez después deja
la misma lista que si no hubiera fallado.

</details>

## Contexto

**Lo que estaba mal** (leído en el código del 11/10):
- Publicar desde la tanda exigía el veredicto «pasa» pero no la compuerta del último cierre; publicar desde la bandeja
  exigía la compuerta pero no el veredicto, y la compuerta pasa con «no se puede decidir». `batch close` decía «pasa» y
  terminaba bien con «no se puede decidir».
- Las decisiones de la revisión (aprobado, rechazado) se guardaban aparte y nadie las leía al publicar: lo rechazado
  entraba al curso.
- El único escritor de producción por tanda comparaba todo el curso del disco contra producción y publicaba cualquier
  propuesta aprobada de un plan, con una lista fija de campos —título, traducción, oración guardada, partes del
  enunciado—, saltando toda la opción múltiple; no se podía probar sin conectarse y su informe no decía contra qué
  servidor corrió ni guardaba lo que pisó. Los escritores de nombres y consignas de los temas tampoco dejaban registro.

**Por qué una lista y no el curso del disco.** El curso del disco es el de hoy: una tanda posterior, una edición a mano
o una corrida a medias lo cambian. La lista fija lo que la tanda decidió, con el antes para poder comparar y no pisar
lo que alguien cambió en producción mientras tanto, y su huella ata la lista a la publicación.

## Alcance

- **Adentro**: R001 a R009: la lista de publicación, sus campos, lo rechazado, lo sin revisar, «pasa» para publicar, y
  el paso a producción: tomar sólo la lista, verificar, registrar y ensayar.
- **Afuera**:
  - grabar los audios ([F-TANDA-R012](#F-TANDA-R012));
  - el nombre de los temas y el título de los ejercicios, que siguen su camino por plan;
  - las tandas publicadas antes de esta regla, que no tienen lista ([DOUBT-PUBPROD-SIN-LISTA](#DOUBT-PUBPROD-SIN-LISTA));
  - la revisión misma ([F-TANDA-R007](#F-TANDA-R007)) y la compuerta ([F-NOEMP-R005](#F-NOEMP-R005)).

## User Journeys

### Journey[F-PUBPROD-J001] - Publicar la tanda con lo que José rechazó afuera
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-PUBPROD-J001
    name: Publicar la tanda con lo que José rechazó afuera
    flow:
      - id: publicar
        action: "José aprobó unos pasos y rechazó otros en Correcciones, y publica la tanda cerrada desde la bandeja"
        gate: [F-PUBPROD-R003, F-PUBPROD-R005]
        outcomes:
          - when: "Un rechazo quedó sobre un contenido que cambió después"
            then: rechazo_vencido
          - when: "La compuerta o el veredicto no dan «pasa»"
            then: no_pasa
          - when: "Los rechazos están vigentes y la tanda pasa"
            then: sacar
      - id: rechazo_vencido
        action: "La publicación se niega y nombra el cambio y el ejercicio del rechazo vencido; el curso no se toca"
        gate: [F-PUBPROD-R003]
        result: failure
      - id: no_pasa
        action: "La publicación se niega y nombra el veredicto o la compuerta; el curso no se toca"
        gate: [F-PUBPROD-R005]
        result: failure
      - id: sacar
        action: "Salen los cambios rechazados con sus encadenados, sus tareas vuelven a pendiente y la tanda se compara de nuevo"
        gate: [F-PUBPROD-R003, F-PUBPROD-R005]
        outcomes:
          - when: "Sigue dando «pasa»"
            then: lista
      - id: lista
        action: "La tanda deja la lista con el antes y el después de cada campo y las cuentas de la revisión, registra su huella y recién entonces escribe el curso"
        gate: [F-PUBPROD-R001, F-PUBPROD-R002, F-PUBPROD-R004]
        result: success
```

## Open Questions

<a id="DOUBT-PUBPROD-SIN-LISTA"></a>
### Doubt[DOUBT-PUBPROD-SIN-LISTA] - ¿Qué pasa con las tandas publicadas antes de que hubiera lista?
**Status**: OPEN

Una tanda publicada antes de esta regla no tiene lista ni huella.

- [ ] Opción A (recomendada, la que rige): el paso a producción las rechaza; el registro corre sólo hacia adelante.
- [ ] Opción B: armar su lista después, a partir del registro de la tanda y del curso del que partió.

**Answer**: Pendiente. Recomiendo A: armar una lista después dependería del curso del disco de hoy, que es justo lo que
esta regla evita. Hasta que José conteste, rige A.

<a id="DOUBT-PUBPROD-POR-PASO"></a>
### Doubt[DOUBT-PUBPROD-POR-PASO] - ¿Un rechazo saca el cambio entero o sólo ese paso?
**Status**: OPEN

Un cambio de una regla puede tener pasos en varios ejercicios; José puede rechazar uno solo.

- [ ] Opción A (recomendada, la que rige): sale el cambio entero, con sus encadenados ([F-TANDA-R008](#F-TANDA-R008)).
- [ ] Opción B: sale sólo el paso rechazado; hace falta poder sacar un paso de un cambio.

**Answer**: Pendiente. Rige A: nunca deja viajar algo que José rechazó, y no inventa una forma nueva de sacar. Si José
quiere conservar los otros pasos, B es una feature aparte.

## References

- **FEAT-TANDA** — La revisión de Correcciones ([F-TANDA-R007](#F-TANDA-R007)), sacar un cambio con sus encadenados
  ([F-TANDA-R008](#F-TANDA-R008)), publicar ([F-TANDA-R009](#F-TANDA-R009)) y la regrabación
  ([F-TANDA-R012](#F-TANDA-R012)). Citado por R002 a R004.
- **FEAT-BANDEJA** — Publicar es un solo OK ([F-BANDEJA-R008](#F-BANDEJA-R008)) y la muestra de regla
  ([F-BANDEJA-R004](#F-BANDEJA-R004), [F-BANDEJA-R007](#F-BANDEJA-R007)). Citado por R004 y R005.
- **FEAT-METAS** — El veredicto de la tanda ([F-METAS-R008](#F-METAS-R008)). Citado por R005.
- **FEAT-NOEMP** — La compuerta de «no empeorar» ([F-NOEMP-R005](#F-NOEMP-R005)). Citado por R005.
- **FEAT-TAREA** — La tabla de partes a campos ([F-TAREA-R006](#F-TAREA-R006)). Citado por R002.
