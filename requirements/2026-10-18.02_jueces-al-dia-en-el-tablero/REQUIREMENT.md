---
feature:
  id: FEAT-TALDIA
  code: F-TALDIA
  name: Jueces al día en el tablero - el Mapa, Correcciones, Analizadores y Publicar dicen qué juez cambió y llevan a medirlo
  priority: critical
---

# Jueces al día en el tablero: el Mapa, Correcciones, Analizadores y Publicar dicen qué juez cambió y llevan a medirlo

## TL;DR

**Qué**: El tablero muestra, con una sola marca, si cada veredicto es de la versión de hoy de su juez: «al día»,
«anterior» u «otro motor», o «sin medir». La muestra en el Mapa, en Correcciones, en Analizadores y en Publicar. Cada
versión se nombra por su fecha de aprobación. Desde cualquiera de esas pantallas se abre un diálogo que vuelve a medir
sólo lo de antes, con estimación gratis y tope. La ficha del Mapa también manda a medir lo que está sin medir de un
juez. Para aprobar en grupo, lo de antes cuenta como que falta medir.

**Por qué**: José buscó en el Mapa y en Correcciones si la miniteoría nueva había medido Corrección 1, y no lo encontró.
82 de 83 temas tienen su veredicto de una versión anterior. El Mapa da el análisis A4 por vigente. El grupo «Los
jueces lo dan por bueno» junta 14 cambios, los 14 con la miniteoría del 5/10. Analizadores dice
«738388a5 · fecha no registrada». La pantalla no dice cuál es la versión de hoy.

Las reglas de content-audit están en [FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md). El tablero es
`pipeline/vocabulary_dashboard` en ittm. Fuente: [propuesta de producto «Jueces al día»](../../docs/propuestas/jueces-al-dia.html),
v3 del 18/10, con sus cinco decisiones.

## Recorrido de uso (estado actual)

Cómo José busca hoy si un juez que cambió ya midió. El caso es la miniteoría sobre Corrección 1 de la tanda A1.

1. Abre el Mapa y toca Corrección 1 ✓. ⚠️ La tarjeta y el anillo de A4 no dicen que la miniteoría midió casi todo con
   una versión anterior.
2. Abre la ficha de jueces. ⚠️ En la base, la miniteoría dice «aprobado»: compara la base contra sí misma, no contra la
   versión de hoy.
3. Entra a Correcciones por «Revisarlos en grupo» ✓. ⚠️ El grupo «Los jueces lo dan por bueno» junta 14 cambios, y los
   14 tienen la miniteoría del 5/10. Ningún renglón lo dice.
4. Mira Analizadores. ⚠️ Lee «738388a5 · fecha no registrada» o «versiones mezcladas». No dice cuál es la de hoy ni
   cuánto midió cada una.
5. Abre Publicar. ⚠️ La comparación «no se puede decidir», sin decir por qué juez. Si las dos puntas tuvieran la misma
   versión vieja, la puerta estaría abierta.

**Punto de fricción**: pasos 1 a 3. Lo de antes se ve igual que lo de hoy, y entra al grupo que se aprueba de una vez.

**Cambio mínimo necesario**: una marca en cada veredicto, el aviso de qué juez cambió y un solo diálogo para volver a
medir lo de antes. Además, que el grupo no cuente lo de antes y que Publicar nombre al juez.

## Reglas de Negocio

Las palabras **juez**, **motor**, **versión de hoy**, **al día**, **de antes** y **sin medir** son las de
[FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md). Una **opinión** es lo que un juez dijo de un cambio en
Correcciones. **Lo dan por bueno** es el grupo de cambios que los jueces dan por resueltos, que se revisa con una
muestra de 12 antes de aprobar.

<a id="F-TALDIA-R001"></a>
### Rule[F-TALDIA-R001] - Una sola marca, la que dice content-audit, en todas las pantallas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo veredicto, opinión o cuenta de un juez que el tablero muestra lleva una de tres marcas, con las mismas palabras y
> colores en el Mapa, Correcciones, Analizadores y Publicar:
> - **al día**, en verde: lo midió la versión de hoy;
> - **anterior**, en ámbar: lo midió otra versión. Si la causa es el motor, la marca dice **otro motor**;
> - **sin medir**, gris y punteada: todavía no lo midió nadie.
>
> Invariantes:
> 1. el tablero no calcula la marca: la lee de content-audit ([F-ALDIA-R005](#F-ALDIA-R005)). Nunca compara códigos de
>    versión para decidirla;
> 2. cada versión se nombra como dice [F-ALDIA-R004](#F-ALDIA-R004), en la zona horaria del navegador y a partir del
>    instante de aprobación que da content-audit. Así queda «la del 5/10», o «la del 13/10, 22:12» si ese juez aprobó
>    otra ese mismo día;
> 3. el código de la versión va en chico, al lado del nombre. Al pasar por encima se ve entero y se puede copiar;
> 4. una versión de un juez nunca se muestra como «versión N»: eso sigue siendo del contenido;
> 5. la fecha de medición, donde se muestre, va aparte y nunca como fecha de la versión;
> 6. los analizadores que no son jueces no llevan marca;
> 7. mostrarla no consulta.

<details><summary>Detail</summary>

Es una sola marca para que se lea igual en todas partes y sin saber qué versión es cuál. Viene de content-audit por la
misma razón que el análisis vigente: «El servidor decide cobertura, errores, compatibilidad y comparabilidad. La UI no
elige». El tablero no sabe con qué motores aprobó cada versión.

**Criterio**: en la ficha de Corrección 1, la miniteoría dice «anterior 82 · al día 1» y la base, «anterior 83». La
naturalidad de la base dice «otro motor». Al pasar por encima de `738388a5` se ve el código entero y «copiar».

</details>

<a id="F-TALDIA-R002"></a>
### Rule[F-TALDIA-R002] - El Mapa dice qué juez cambió y cuánto quedó atrás
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En el Mapa:
> 1. la tarjeta de un estado suma, bajo «Jueces», una línea que nombra los jueces que cambiaron, cuánto quedó atrás y
>    un «Volver a medir»: «1 juez cambió · Miniteoría: 82 de 83 temas con versión anterior · Volver a medir». Aparece
>    sólo si algún juez tiene algo de antes en ese estado;
> 2. debajo de cada anillo de análisis dice «N jueces de antes» o «todos al día». Un arco ámbar en el anillo marca la
>    parte de los jueces que midió con una versión anterior;
> 3. la ficha de jueces dice, por juez, en la base y en la corrección, cuánto tiene de cada marca, en la unidad del juez
>    (temas o ejercicios). Compara contra la versión de hoy, nunca un estado contra el otro;
> 4. con todo al día no aparece la línea ni el arco;
> 5. el cruce «De tus N pendientes, M ya los dan por buenos» no cuenta lo de antes ([F-TALDIA-R006](#F-TALDIA-R006)).

<details><summary>Detail</summary>

Hoy la ficha dice «aprobado» en la base de la miniteoría porque compara la base contra sí misma: la versión que midió
la base es la misma que la del examen que mira. Con la versión de hoy como referencia, dice «anterior 83».

El Mapa no mide nada: «Volver a medir» abre el diálogo de [F-TALDIA-R004](#F-TALDIA-R004).

**Criterio**, con los datos del 18/10: la tarjeta de Corrección 1 dice «1 juez cambió · Miniteoría: 82 de 83 temas con
versión anterior · Volver a medir». Debajo de A0 dice «2 jueces de antes» y debajo de A4, «1 juez de antes».

</details>

<a id="F-TALDIA-R003"></a>
### Rule[F-TALDIA-R003] - Al tocar «anterior» se ve qué cambió del juez
**Severity**: normal | **Validation**: AUTO_VALIDATED

> Tocar la marca «anterior» de un juez, en la ficha, en Analizadores o en Correcciones, abre qué cambió:
> 1. cada versión que midió ese estado, con su nombre, su código y cuánto midió;
> 2. cuándo aprobó la versión de hoy;
> 3. qué archivos cambiaron de una versión a la siguiente, como los da content-audit ([F-ALDIA-R006](#F-ALDIA-R006)).
>    Van en dos renglones, «cambió: …» y «no cuenta: eval/… · no corre al medir». Si content-audit no lo sabe, dice «no
>    se sabe qué cambió»;
> 4. un «Volver a medir lo viejo» que abre el diálogo de [F-TALDIA-R004](#F-TALDIA-R004).

<details><summary>Detail</summary>

Sirve para decidir si vale la pena medir de nuevo o pedir la versión anterior por nombre al publicar
([F-ALDIA-R009](#F-ALDIA-R009)). Las pruebas de `eval/` se muestran aparte para que se vea que no cambian la versión.

**Criterio**: para la miniteoría en Corrección 1 dice, de la más nueva a la más vieja: la del 13/10, 22:12, la de hoy,
que midió 1 tema; la del 13/10, 13:00, 5 temas; la del 5/10, 77 temas. Entre la segunda y la de hoy cambiaron
`nodes/evaluate.md`, `schema/verdict.schema.json` y `scripts/gate_verdict.sh`. En otro renglón: «no cuenta:
eval/fixtures.json».

</details>

<a id="F-TALDIA-R004"></a>
### Rule[F-TALDIA-R004] - Un solo diálogo vuelve a medir lo viejo, con estimación y tope
**Severity**: critical | **Validation**: AUTO_VALIDATED

> «Volver a medir lo viejo» es un diálogo. Se abre desde la línea del Mapa, la ficha, el aviso de Correcciones,
> Analizadores y la condición de Publicar, con el juez y el estado ya elegidos. Invariantes:
> 1. en «Dónde» hay dos casillas, marcadas por defecto: «Corrección N · X temas con versión anterior» y «La base · Y
>    temas, para poder comparar y publicar». Debajo dice «K ya está al día: se reusa gratis»;
> 2. la estimación es gratis y la da content-audit, por estado ([F-ALDIA-R007](#F-ALDIA-R007)). El tope viene propuesto
>    con margen, como en el diálogo común, y se puede cambiar;
> 3. sólo se ofrecen perfiles cuyo motor aprobó con la versión de hoy;
> 4. el botón dice cuánto mide: «Medir 165 temas». Nada se consulta antes de confirmarlo, y el servidor exige el tope;
> 5. mientras mide, la línea dice «Midiendo miniteoría · 12 de 165 temas», y la corrida se ve en Ejecuciones como
>    cualquier otra;
> 6. al terminar, el Mapa pierde el aviso de lo que se midió ([DOUBT-ALDIA-INTEGRAR](#DOUBT-ALDIA-INTEGRAR)). Si el tope
>    no alcanzó, el aviso queda con lo que falta;
> 7. si la versión de hoy no aprobó su examen, el botón se apaga y dice «La versión actual todavía no rindió su examen.
>    Medir con ella no sirve para publicar.»;
> 8. «Pedir de nuevo», en un renglón de Correcciones, abre el diálogo sólo con ese tema;
> 9. lo sin medir no entra: eso se mide con «Medir lo que falta» ([F-TALDIA-R009](#F-TALDIA-R009)) o con «Completar
>    opiniones».

<details><summary>Detail</summary>

La base viene marcada por la decisión 2 de la propuesta (18/10). Al publicar, cada cambio se compara contra la base con
el mismo juez en las dos puntas. Sin la base medida, lo de antes sigue sin compararse y la tanda no se publica. Cumple
la decisión 6 del tablero: gasta sólo por suscripción, con estimación, tope y confirmación, y nunca publica.

Hoy, «Reanalizar» con fuerza mide todo de nuevo, también lo que ya está al día. El diálogo nuevo mide sólo lo de antes.

**Criterio**: desde la línea de Corrección 1, el diálogo dice «Corrección 1 · 82 temas con versión anterior», «La base ·
83 temas, para poder comparar y publicar» y «1 tema ya está al día: se reusa gratis». La estimación es de unas 165
consultas y el botón, «Medir 165 temas» [ASSUMPTION]: el número real lo da la estimación. Con la casilla de la base
desmarcada, el botón dice «Medir 82 temas».

</details>

<a id="F-TALDIA-R005"></a>
### Rule[F-TALDIA-R005] - Correcciones dice qué opinión es de una versión anterior
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En Correcciones:
> 1. arriba de la lista va un aviso por juez que cambió, no uno por renglón. Dice cuándo aprobó la versión de hoy y
>    cuánto quedó atrás, y lleva al diálogo: «Miniteoría con error cambió el 13/10, 22:12. 82 de 83 temas tienen su
>    opinión de la versión anterior. Volver a medir»;
> 2. en el renglón, la opinión de una versión anterior lleva su marca en ámbar: «versión del 5/10». Al pasar por encima
>    dice «cambió el juez: puede no decir lo mismo hoy»;
> 3. esa marca es distinta de la de otro contenido, que avisa que el ejercicio se editó después de medir. Si pasan las
>    dos cosas, se ven las dos;
> 4. la opinión sigue diciendo lo que dijo («dijo: sin error»), con su marca. El renglón se puede aprobar o rechazar de
>    a uno como hoy;
> 5. «Pedir de nuevo», junto a la marca, abre el diálogo con ese tema solo ([F-TALDIA-R004](#F-TALDIA-R004)).

<details><summary>Detail</summary>

El aviso lleva la hora porque la miniteoría aprobó dos versiones el 13/10. Con el día solo, «cambió el 13/10» no
distingue las opiniones del 13/10 a las 13:00, que son de antes ([F-ALDIA-R004](#F-ALDIA-R004)). La propuesta lo
dibujaba sin hora.

**Criterio**: en un cambio de «Prepositions · Rangos temporales» de Corrección 1, la miniteoría dice «versión del 5/10»
en ámbar, «dijo: sin error» y «Pedir de nuevo».

</details>

<a id="F-TALDIA-R006"></a>
### Rule[F-TALDIA-R006] - Para aprobar en grupo, lo de antes cuenta como que falta medir
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para el grupo «Los jueces lo dan por bueno» y para el cruce del Mapa, una opinión de antes cuenta como «falta medir».
> Esto vale tanto para la versión anterior como para otro motor. Invariantes:
> 1. un cambio con una opinión de antes no entra en el grupo, aunque esa opinión diga que está resuelto;
> 2. «Por qué quedan afuera» lo dice con el juez y la versión: «Falta medir la miniteoría · opinó con la versión del
>    5/10»;
> 3. la barra del grupo dice cuántos esperan a qué juez y ofrece medir lo que falta: «Lo dan por bueno 0 · 14 esperan a
>    la miniteoría de hoy · Medir 1 tema». El botón abre el diálogo con esos temas ([F-TALDIA-R004](#F-TALDIA-R004));
> 4. de a uno, el cambio se aprueba como hoy, con la marca a la vista;
> 5. lo demás de la opinión no cambia: otro contenido, empeoró y no consta se leen como hoy, en el mismo orden;
> 6. la muestra de 12 sale de lo que queda en el grupo.

<details><summary>Detail</summary>

Es la decisión 1 de la propuesta (18/10): en grupo no cuenta, de a uno sí, con la marca. Aprobar en grupo es aprobar
sin mirar cada cambio, y eso se apoya en que el juez de hoy lo vio. De a uno, José mira el cambio y la marca le dice
que la opinión puede no valer.

En el orden de la opinión, una opinión de antes queda donde queda la falta de medición de ese juez. Puede ser el juez
que pidió el cambio o cualquier otro.

**Criterio**: en Corrección 1, hoy el grupo junta 14 cambios. Con esto junta 0, y la barra dice «Lo dan por bueno 0 ·
14 esperan a la miniteoría de hoy · Medir 1 tema». De a uno, cada uno de esos 14 se puede aprobar.

</details>

<a id="F-TALDIA-R007"></a>
### Rule[F-TALDIA-R007] - Analizadores dice qué versión midió cuánto
**Severity**: major | **Validation**: AUTO_VALIDATED

> En Analizadores:
> 1. cada juez lleva su marca con la cuenta de lo de antes («anterior 82»), en vez de «738388a5 · fecha no registrada» o
>    «versiones mezcladas»;
> 2. al abrir la fila, una barra por versión dice su nombre, cuándo aprobó, cuánto midió (temas y ejercicios) y cuál es
>    la de hoy. Lo de antes va en ámbar y lo de hoy en verde;
> 3. debajo dice cómo está la base, con un «Volver a medir lo viejo» ([F-TALDIA-R004](#F-TALDIA-R004));
> 4. el código va en chico, al lado de cada versión ([F-TALDIA-R001](#F-TALDIA-R001), inv. 3);
> 5. la fecha de medición se muestra aparte, nunca como fecha de la versión.

<details><summary>Detail</summary>

«Fecha no registrada» se refiere a la fecha de medición, y hoy se lee como si fuera de la versión. Las 17 versiones
aprobadas tienen la fecha de su aprobación.

**Criterio**: en Corrección 1, la miniteoría dice «anterior 82». Al abrirla se ve: la del 5/10, 77 temas y 2.202
ejercicios; la del 13/10, 13:00, 5 temas y 180 ejercicios; la del 13/10, 22:12, la de hoy, 1 tema y 20 ejercicios.
Debajo: «La base: los 83 temas con la del 5/10».

</details>

<a id="F-TALDIA-R008"></a>
### Rule[F-TALDIA-R008] - Publicar suma una quinta condición: los jueces están al día
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La puerta de Publicar suma, a sus cuatro condiciones, «Los jueces están al día». Sale de lo que dice la bandeja
> ([F-ALDIA-R008](#F-ALDIA-R008), inv. 5), y el tablero no la calcula. Invariantes:
> 1. con todos al día: «Los jueces están al día · Cada uno, en los dos lados, con su versión de hoy.»;
> 2. si no: «Dos jueces no están al día», y un renglón por juez con su marca, su nombre, cada causa y cuántos cambios.
>    Lleva un «Volver a medir lo viejo» que abre el diálogo con ese juez y los dos estados;
> 3. sin bandeja leída: «No se sabe si los jueces están al día». Nunca se da por sí;
> 4. la condición de la comparación nombra a los jueces, como dice content-audit ([F-ALDIA-R010](#F-ALDIA-R010)): «21
>    cambios sin comparar: la naturalidad en los 21 y la miniteoría en 7.»;
> 5. el comando para la terminal suma `--accept-previous <juez>` sólo con los jueces a los que se aplica
>    ([F-ALDIA-R009](#F-ALDIA-R009)), y dice qué seguiría frenando;
> 6. se sigue publicando desde la terminal: el tablero no publica;
> 7. una tanda publicada con una versión anterior pedida por nombre muestra lo anotado: «Anotado: miniteoría, la del
>    5/10, en 14 cambios».

<details><summary>Detail</summary>

Hoy la puerta mira cuatro cosas: la tanda cerrada, la compuerta, la comparación y la muestra ciega. Ninguna mira la
versión de los jueces. Con la misma versión vieja en las dos puntas, las cuatro pueden estar en verde. La quinta lo
cubre, y separa lo que no se puede comparar de lo que se compara con un juez viejo.

**Criterio**, con Corrección 1 al 18/10: la condición dice «Dos jueces no están al día». Debajo van dos renglones:
«otro motor · Naturalidad idiomática · la base es de llama3 local · 21 cambios» y «anterior · Miniteoría con error ·
la del 5/10 en los dos lados · 14 cambios; una versión distinta en cada lado · 7 cambios». El comando sugiere
`--accept-previous mini-theory` y aclara que la naturalidad y los 7 cambios seguirían frenando.

</details>

<a id="F-TALDIA-R009"></a>
### Rule[F-TALDIA-R009] - La ficha del Mapa mide lo que le falta a un juez
**Severity**: normal | **Validation**: AUTO_VALIDATED

> En la ficha de jueces del Mapa ([F-TALDIA-R002](#F-TALDIA-R002), inv. 3), cada celda de un juez en un estado que
> tiene algo «sin medir» suma un botón «Medir lo que falta». Invariantes:
> 1. si la versión de hoy del juez aprobó su examen, el botón abre el diálogo común de lanzar con ese juez y ese
>    estado. El diálogo estima gratis, propone un tope y no consulta nada hasta que José confirma;
> 2. mide sin forzar: reusa todo lo que ya está medido, también lo de antes, y consulta sólo lo sin medir;
> 3. si la versión de hoy no aprobó su examen, el botón está apagado. La fila dice una sola vez por qué: «La versión de
>    hoy no aprobó su examen: sin eso no consulta sobre el curso.», con un «Ver su examen» que lleva a Exámenes con
>    ese juez;
> 4. con la tanda publicada no hay botón ni motivo: no queda nada que medir;
> 5. un juez que content-audit no sabe medir en esa tanda no tiene botón;
> 6. lo medido queda como una corrida suelta, que el carril de mediciones ofrece integrar
>    ([F-MEDJ-R003](../2026-10-19.01_mediciones-por-juez/REQUIREMENT.md#F-MEDJ-R003)).

<details><summary>Detail</summary>

El 19/10 José vio «sin medir» en la ficha de A4 y no lo pudo mandar a medir desde ahí. «Volver a medir» sólo aparece
con algo de antes, y «Completar opiniones» pide a todos los jueces a la vez, sólo en la corrección. Este botón mide un
juez en un estado, también en la base.

El motivo del botón apagado es el de [F-JEXAM-R003](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R003): sin el examen aprobado, el juez no consulta sobre
el curso. No es que «no sirve para publicar»: no mide.

**Criterio**, con los datos del 19/10: Pragmatismo comunicativo dice «sin medir 88» y «Medir lo que falta» en
Corrección 1. Calidad de distractores dice «sin medir 244» y el botón en la base. Naturalidad idiomática y Calidad
didáctica A1 tienen el botón apagado, el motivo y «Ver su examen».

</details>

## Contexto

| Pantalla | Hoy | Con esto |
|---|---|---|
| Mapa, tarjeta | Nada sobre la versión | «1 juez cambió · Miniteoría: 82 de 83 temas con versión anterior · Volver a medir» |
| Mapa, anillo A4 | Vigente, sin más | «1 juez de antes», con arco ámbar |
| Mapa, ficha | La base dice «aprobado» | La base dice «anterior 83» |
| Mapa, ficha | «sin medir 88», sin cómo medirlo | «Medir lo que falta», o apagado con el motivo y «Ver su examen» |
| Correcciones, grupo | 14 cambios que lo dan por bueno | 0, y «14 esperan a la miniteoría de hoy» |
| Correcciones, renglón | La opinión sin versión | «versión del 5/10», en ámbar, y «Pedir de nuevo» |
| Analizadores | «738388a5 · fecha no registrada» | «anterior 82», y la barra por versión |
| Publicar | Cuatro condiciones; «no se puede decidir» sin nombre | Cinco, con los jueces nombrados |

Medir no está autorizado por este requisito: el diálogo gasta sólo con el tope que José confirma.

## Alcance

- **Adentro**: la marca en cada pantalla; el aviso en el Mapa y en Correcciones; qué cambió del juez; el diálogo de
  volver a medir lo viejo; medir desde la ficha lo que le falta a un juez; que el grupo no cuente lo de antes; la mezcla
  por versión en Analizadores; la quinta condición de Publicar.
- **Afuera**: las reglas de content-audit ([FEAT-ALDIA](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md)); publicar desde
  el tablero; completar las opiniones de todos los jueces a la vez, que sigue en «Completar opiniones»; cambiar qué es
  «lo dan por bueno» fuera de la versión; la pantalla de Exámenes.

## User Journeys

### Journey[F-TALDIA-J001] - Ver que un juez cambió, ponerlo al día y publicar
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-TALDIA-J001
    name: Ver que un juez cambió, ponerlo al día y publicar
    flow:
      - id: mapa
        action: "José abre el Mapa y toca la corrección"
        gate: [F-TALDIA-R001, F-TALDIA-R002]
        outcomes:
          - when: "Todos los jueces están al día"
            then: correcciones
          - when: "Algún juez tiene algo de antes"
            then: aviso
      - id: aviso
        action: "La tarjeta dice qué juez cambió y cuánto quedó atrás; José toca «anterior» y ve qué cambió"
        gate: [F-TALDIA-R002, F-TALDIA-R003]
        outcomes:
          - when: "José decide medir de nuevo"
            then: dialogo
          - when: "José decide revisar igual"
            then: correcciones
      - id: dialogo
        action: "El diálogo estima gratis la corrección y la base; José confirma el tope"
        gate: [F-TALDIA-R004]
        outcomes:
          - when: "La versión de hoy aprobó su examen y el tope alcanzó"
            then: correcciones
          - when: "La versión de hoy no aprobó su examen"
            then: sin_examen
      - id: sin_examen
        action: "El botón está apagado y dice por qué"
        gate: [F-TALDIA-R004]
        result: failure
      - id: correcciones
        action: "José revisa en Correcciones; lo de antes lleva su marca y no entra en el grupo"
        gate: [F-TALDIA-R005, F-TALDIA-R006]
        outcomes:
          - when: "José terminó de decidir"
            then: publicar
      - id: publicar
        action: "José abre Publicar"
        gate: [F-TALDIA-R008]
        outcomes:
          - when: "Las cinco condiciones están en verde"
            then: listo
          - when: "Un juez no está al día"
            then: dialogo
      - id: listo
        action: "José publica desde la terminal con el comando que muestra la puerta"
        gate: [F-TALDIA-R008]
        result: success
```

## Open Questions

No quedan abiertas. La que afectaba a este requisito era de content-audit:
[DOUBT-ALDIA-INTEGRAR](../2026-10-18.01_jueces-al-dia/REQUIREMENT.md#DOUBT-ALDIA-INTEGRAR), sobre si lo medido se
integra solo al terminar. José la resolvió el 18/10: se integra solo y reemplaza sólo lo de antes de ese juez, así que
el aviso se va al terminar.

## Cómo se prueba

Las pruebas usan respuestas grabadas de content-audit. Ninguna consulta a un modelo.

| Regla | Qué se comprueba |
|---|---|
| R001 | Las tres marcas salen de lo que dice content-audit, con las mismas palabras en las cuatro pantallas. Dos aprobaciones el mismo día en la zona del navegador llevan la hora, y ninguna versión de juez se muestra como «versión N» |
| R002 | La línea y el arco aparecen sólo con algo de antes. La ficha de la base dice «anterior», no «aprobado» |
| R003 | Lo que no cuenta va en su propio renglón; sin lista, dice «no se sabe qué cambió» |
| R004 | Las dos casillas vienen marcadas, el botón dice el número y nada se consulta sin confirmar. Sin examen aprobado, el botón está apagado y dice por qué |
| R005 | Hay un aviso por juez, no por renglón. La marca de versión y la de otro contenido son distintas y se ven juntas |
| R006 | Un cambio con una opinión de antes no entra en el grupo y sí se aprueba de a uno. «Por qué quedan afuera» nombra al juez y la versión |
| R007 | La mezcla dice cuánto midió cada versión y la fecha de medición va aparte |
| R008 | La quinta condición sale de la bandeja y sin bandeja dice «no se sabe». `--accept-previous` aparece sólo donde se aplica |
| R009 | El botón aparece sólo donde hay algo sin medir y abre el diálogo común sin forzar, con ese juez, ese estado y la tanda; nada se consulta. Sin examen aprobado está apagado, con el motivo una vez y «Ver su examen». Publicada la tanda, o con un juez que no se sabe medir, no hay botón |

## References

- **FEAT-ALDIA**: la marca ([F-ALDIA-R003](#F-ALDIA-R003)), el nombre de cada versión ([F-ALDIA-R004](#F-ALDIA-R004)), la
  lectura por juez ([F-ALDIA-R005](#F-ALDIA-R005)), qué cambió ([F-ALDIA-R006](#F-ALDIA-R006)), volver a medir
  ([F-ALDIA-R007](#F-ALDIA-R007)), publicar ([F-ALDIA-R008](#F-ALDIA-R008) a [F-ALDIA-R010](#F-ALDIA-R010)) y
  [DOUBT-ALDIA-INTEGRAR](#DOUBT-ALDIA-INTEGRAR).
- **El tablero** (`html/README.md` de ittm): la decisión 6 (gastar sólo con estimación, tope y confirmación, y no
  publicar), la 7 (el orden de la opinión), la 8 (la muestra de 12 del grupo) y la 9 (cómo entra el Mapa a
  Correcciones).
- **La propuesta**: [Jueces al día](../../docs/propuestas/jueces-al-dia.html), v3 del 18/10.
