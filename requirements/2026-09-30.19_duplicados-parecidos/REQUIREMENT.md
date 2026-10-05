---
feature:
  id: FEAT-DUPAR
  code: F-DUPAR
  name: Duplicados parecidos - dos ejercicios casi iguales del mismo tema
  priority: medium
---

# Duplicados parecidos: dos ejercicios casi iguales del mismo tema

## TL;DR

**Qué**: `near-duplicates` marca cada ejercicio que repite casi igual a otro de su tema: lo que iguala el comodín y lo
que claude -p, como juez, dice que es el mismo ejercicio; con Ollama, los vectores sólo le proponen los pares.

**Por qué**: Con el parecido de 0,95 marcando solo, reprobó su examen: de 40 pares que no marcó, 20 eran el mismo
ejercicio. Y desde el 1/10 ninguna tanda de correcciones se publica sin todos los jueces aprobados.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar ejercicios que se repiten con un cambio mínimo:

1. Corre `analyze` pidiendo sólo `near-duplicates` → ✓ disponible: el comodín y, con Ollama, el parecido de
   nomic-embed-text de 0,95 o más, que marca solo (45 copias: 25 del comodín y 20 del parecido); sin Ollama, claude -p
   juzga los pares de texto parecido.
2. Le hace rendir el examen a la versión con Ollama (`exam run near-duplicates`) → ⚠️ reprobada el 1/10: encuentra los
   casi iguales del 077 en las dos mitades y dos corridas marcan lo mismo, pero de los 40 pares de la muestra ciega que
   no marcó —de 0,90 a 0,95 de parecido y con la misma respuesta—, 20 eran el mismo ejercicio, y el tope es 8. Mientras
   no apruebe, no publica nada ([F-MLOCAL-R006](#F-MLOCAL-R006)).
3. Baja el umbral → ⚠️ no alcanza: el parecido solo no separa. De 0,85 a 0,90, de 30 pares al azar que piden lo mismo,
   4 son el mismo ejercicio, 5 son dudosos y 21 no.
4. Publica una tanda → ⚠️ José, 1/10: ninguna se publica sin todos los jueces aprobados, y `near-duplicates` no aprobó.

**Punto de fricción**: pasos 2 a 4. Los vectores no distinguen la misma oración dicha de otro modo —otro sujeto, la
negativa, otras palabras— de otra oración que pide la misma respuesta.

**Cambio mínimo necesario**: el paso por pares. Con Ollama, los vectores sólo proponen candidatos, desde 0,80, y cada
uno lo decide el mismo juez que sin Ollama: claude -p, con la pregunta «para el alumno, ¿es el mismo ejercicio?»
([F-DUPAR-R002](#F-DUPAR-R002), [F-DUPAR-R009](#F-DUPAR-R009), [F-DUPAR-R010](#F-DUPAR-R010)). Sin Ollama, nada
cambia salvo el juez, que es ese mismo.

## Reglas de Negocio

Desde el 4/10, toda mención a `claude -p` como juez activo significa por omisión `codex-cli` autenticado con la sesión local de
ChatGPT; una selección explícita de otro proveedor prevalece ([F-JEXAM-R013](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R013)). Las mediciones anteriores con Claude conservan valor histórico, pero la versión
de Codex tiene procedencia distinta y vuelve a rendir examen antes de consultar el curso
([F-JEXAM-R014](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#F-JEXAM-R014)).

**Ejercicio terminado** es el de FEAT-DUPEX ([F-DUPEX-R001](#F-DUPEX-R001)): el texto fijo que ve el alumno, sin la
pista, con la primera respuesta aceptada —en opción múltiple, la correcta—, también la oración de partida. **Tema** es
el *knowledge* (413). **Candidato del juez** es un par de R001 que el comodín no resuelve y que propone su ruta: con
Ollama, el parecido de los vectores ([F-DUPAR-R009](#F-DUPAR-R009)); sin Ollama, el de las palabras
([F-DUPAR-R008](#F-DUPAR-R008)). **El juez de pares** es claude -p con la guía de `near-duplicates-pair-judge`, el
mismo en las dos rutas.

<a id="F-DUPAR-R001"></a>
### Rule[F-DUPAR-R001] - Sólo son casi duplicados dos ejercicios del mismo tema que piden lo mismo
**Severity**: major | **Validation**: ASSUMPTION

> Un par de ejercicios es candidato sólo si cumple las tres condiciones:
> 1. son del mismo tema;
> 2. su respuesta es la misma comparada como el corrector, con la contraída igual a la larga y sin contar un pronombre
>    o un demostrativo que el alumno copia del enunciado;
> 3. no son duplicados exactos: ésos son de `exact-duplicates` ([F-DUPEX-R001](#F-DUPEX-R001)).

<details><summary>Por qué la misma respuesta</summary>

Sin esa condición, los pares más parecidos del curso son contrastes a propósito: «I observed him clean the room.» y «I
observed him cleaning the room.» (B2, *Acción completa o parcial*, parecido 0,995) o «I am great.» y «You are great.»
(*Contraste inicial*). Ahí la diferencia es justo lo que se enseña. «He is certainly leaving.» y «She is certainly
leaving.» sí piden lo mismo: el pronombre lo copia el alumno del enunciado. En la base del 29/9 cumplen las tres 24.889
de los 200.953 pares del mismo tema (A1 7.085, A2 6.048, B1 10.421, B2 1.335).

</details>

<a id="F-DUPAR-R002"></a>
### Rule[F-DUPAR-R002] - Casi iguales: el comodín los iguala o el juez dice que son el mismo ejercicio
**Severity**: major | **Validation**: ASSUMPTION

> Un par de R001 es casi duplicado si cumple una de dos condiciones:
> 1. los dos ejercicios terminados quedan iguales al cambiar por un comodín los pronombres, los demostrativos, los
>    posesivos y el tiempo del auxiliar;
> 2. es candidato del juez y el juez de pares dice que, para el alumno, es el mismo ejercicio.
>
> Ningún parecido marca un par por sí solo: el de los vectores (0,80 o más, con Ollama) y el de las palabras (0,6 o
> más, sin Ollama) sólo eligen qué pares se le preguntan al juez.

<details><summary>Qué es el mismo ejercicio, y los casos del curso</summary>

El juez lee los dos ejercicios como los ve el alumno y resueltos, el que queda primero, y contesta si al hacer el
segundo después del primero el alumno vuelve a hacer lo mismo con la misma oración. Su guía nombra los casos; los
ejemplos de abajo son los inventados de la guía y los del 077 en `escribir/`:

| Es el mismo ejercicio | No es el mismo ejercicio |
|---|---|
| Otro sujeto: «Carlos takes the bus to work.» / «He takes the bus to work.» | Otra oración con otra idea, aunque tenga la misma forma: «I am cold.» / «I am late.» |
| Una palabra o un trozo de más o de menos: «Offer: I'll wash the dishes for you.» / «Offer: I'll wash the dishes.» | El contraste que enseña el tema, cuando cambia el papel de lo que escribe el alumno: «This is his bike.» / «This bike is his.» |
| Un sinónimo fuera del hueco: «The film was really boring.» / «The movie was really boring.» | En un diálogo, otra frase de A, aunque la respuesta de B se repita |
| Otro orden, o la negativa o la pregunta de la misma oración | |
| La misma idea con otras palabras: «Have you ever been to Rome?» / «Have you ever visited Rome?» | |

La negativa o la pregunta de la misma oración es el mismo ejercicio aunque el tema sea justo negativas y preguntas, si
el alumno escribe la misma palabra con el mismo papel. Ante la duda, el juez dice que no: un no deja el ejercicio como
está, y un sí lo manda a reemplazar.

En la base del 29/9, con las reglas de producción, el comodín resuelve 25 pares (A1 16, A2 6, B1 2, B2 1), como «I
don't have much time.» / «I didn't have much time.» (A2, en el 077). Candidatos del juez: 1.468 con Ollama y 696 sin
Ollama, todavía sin juzgar.

Cambia lo que José validó el 30/9: el parecido de 0,95 marcaba solo ([DOUBT-DUPAR-UMBRAL](#DOUBT-DUPAR-UMBRAL)), y con
él la versión reprobó su examen (ver Contexto). Las dudas: [DOUBT-DUPAR-UMBRAL-CANDIDATOS](#DOUBT-DUPAR-UMBRAL-CANDIDATOS)
y [DOUBT-DUPAR-MLOCAL-EXCEPCION](#DOUBT-DUPAR-MLOCAL-EXCEPCION).

</details>

<a id="F-DUPAR-R003"></a>
### Rule[F-DUPAR-R003] - Un hallazgo por copia, nunca en el ejercicio que queda
**Severity**: major | **Validation**: ASSUMPTION

> En un grupo de *n* ejercicios casi iguales del tema, el primero en el orden del tema queda sin hallazgo y cada uno de
> los otros lleva uno, que nombra al que queda ([F-HALL-R003](#F-HALL-R003)). La identidad es la regla y el ejercicio
> que queda ([F-HALL-R014](#F-HALL-R014)), así que dos corridas marcan las mismas copias.

Es la regla de FEAT-DUPEX ([F-DUPEX-R002](#F-DUPEX-R002)), con su misma duda sobre cuál queda.

<a id="F-DUPAR-R004"></a>
### Rule[F-DUPAR-R004] - El hallazgo es de gravedad baja y va a la mesa
**Severity**: major | **Validation**: VALIDATED

> Todo hallazgo de `near-duplicates` tiene gravedad *baja* y resolución *mesa*, y cuenta como error
> ([F-HALL-R008](#F-HALL-R008)); con uno, el ejercicio tiene puntaje 0 en este analizador, y sin ninguno, 1. Su costo
> ([F-HALL-R002](#F-HALL-R002)) es *instantáneo* si lo encontró el comodín y *modelo pago* si lo juzgó claude -p
> ([F-DUPAR-R002](#F-DUPAR-R002)); la regla de *modelo local* sólo propone pares y no da hallazgos
> ([F-DUPAR-R006](#F-DUPAR-R006)).

*Baja* porque se puede acertar, como los exactos. *Mesa* porque el arreglo es escribir un ejercicio nuevo con palabras
del semáforo del tema. Cuenta como error y no sólo ordena (José, 30/9: [DOUBT-DUPAR-CUENTA](#DOUBT-DUPAR-CUENTA)). Un
par que resuelve el comodín nunca llega al juez, así que su hallazgo es siempre *instantáneo*.

<a id="F-DUPAR-R005"></a>
### Rule[F-DUPAR-R005] - La evidencia muestra los dos ejercicios y qué cambia
**Severity**: major | **Validation**: AUTO_VALIDATED

> La evidencia trae la copia y el ejercicio que queda, cada uno como lo ve el alumno, y una línea que dice qué cambia y
> por qué condición: con el comodín, «Casi igual a …2f8b del mismo tema: cambia He por She»; con el juez, el parecido
> que lo propuso y la razón que dio: con Ollama, «Para claude -p es el mismo ejercicio que …355e: parecido 0,87», y
> sin Ollama, «Para claude -p es el mismo ejercicio que …355e: cambian 2 de 6 palabras».

Nunca el formato interno del curso ([F-HALL-R003](#F-HALL-R003)). La razón es la línea del juez, en castellano, como
«Cambia sólo el sujeto, Carlos por He: el alumno escribe «takes» en la misma oración.».

<a id="F-DUPAR-R006"></a>
### Rule[F-DUPAR-R006] - `near-duplicates` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `near-duplicates` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Dos ejercicios del mismo
> tema piden casi lo mismo?», familia *errores*, evalúa *ejercicio*, resolución *mesa*, tres reglas —el comodín
> (`same-but-wildcard`, *instantáneo*); los candidatos por parecido (`similar-candidates`, *modelo local*: con Ollama,
> el modelo local que compara oraciones propone los pares desde 0,80 de parecido, y nunca marca solo); y el mismo
> ejercicio para el juez (`same-exercise`, *modelo pago*)—, costo de la ficha *modelo pago*, y nombra a
> nomic-embed-text, que propone los pares con Ollama, y a claude -p, que los decide, cada uno con su versión, junto al
> resultado del examen de cada versión del analizador ([F-MLOCAL-R002](#F-MLOCAL-R002), [F-DUPAR-R010](#F-DUPAR-R010)).

El nombre sigue el estilo de `exact-duplicates`. `same-meaning` sale de la ficha: cambia de nombre porque cambió lo que
hace, de marcar a sólo proponer. La regla de modelo local sigue en la ficha porque es la verdad —los vectores, de modelo
local, eligen qué se le pregunta al juez— y, por ella, `near-duplicates` sigue fuera de la corrida base, como decidió
José el 30/9 ([F-MLOCAL-R004](#F-MLOCAL-R004), [F-RONDA2-R003](#F-RONDA2-R003)).

<a id="F-DUPAR-R007"></a>
### Rule[F-DUPAR-R007] - El examen de cada versión de Duplicados parecidos
**Severity**: major | **Validation**: ASSUMPTION

> Cada versión rinde su examen por separado: la de Ollama —vectores, umbral y juez ([F-DUPAR-R010](#F-DUPAR-R010))— y
> la de sin Ollama —parecido de palabras y juez ([F-DUPAR-R008](#F-DUPAR-R008))—. Cada una aprueba si:
> 1. encuentra los casi iguales del 077 en las dos mitades: los 6 de `escribir/duplicados.json` y los de `examinar/`;
> 2. en una muestra ciega de 40 pares marcados y 40 no marcados, revisada por una persona, de los que marca 8 de cada 10
>    son el mismo ejercicio para el alumno, y de los que no marca, 2 de cada 10 como mucho;
> 3. dos corridas marcan las mismas copias.
>
> En las dos versiones, una corrida de examen sólo juzga los pares cuya copia es un ejercicio del archivo del examen.
>
> Con Ollama, la muestra es la que ya armó el paquete B —40 pares que marcó la versión reprobada y 40 que no marcó, de
> parecido 0,90 a 0,95 y con la misma respuesta, con las respuestas de su revisor—, y un par cuenta como marcado si la
> versión nueva deja su copia con un hallazgo de `near-duplicates`. Sin Ollama, la muestra sale de los candidatos que
> juzgó.

<details><summary>Por qué así, y cuánto cuesta rendir</summary>

El 077 no tiene categoría propia: sus casi iguales son 6 de los 15 duplicados de `escribir/` y quizá uno de
`examinar/`, pocos para medir la precisión; por eso la muestra ciega. La del paquete B ya está armada y revisada, así
que rendir la versión nueva no le cuesta tiempo a nadie. Mira sólo de 0,90 para arriba, y 1.374 de los 1.468
candidatos del curso están por debajo: si José elige A en [DOUBT-DUPAR-MUESTRA-NUEVA](#DOUBT-DUPAR-MUESTRA-NUEVA), la
parte 2 suma una muestra ciega nueva de 20 pares que la versión marca y 20 que juzga y no marca, de por debajo de 0,90
y que nadie revisó, con la misma meta.

El examen mira sólo las copias de su archivo: juzgar un par cuya copia está fuera no cambia ninguna de sus partes, y
filtrar baja el costo de rendir.

| Versión | Consultas para rendir | Horas, de a una · 4 a la vez |
|---|---:|---|
| Con Ollama, con el filtro: los candidatos de los temas de la muestra ciega | 255 como mucho (155 a 0,85) | 1,4 · 0,4 |
| Con Ollama, sin el filtro | 978 | 5,6 · 1,4 |
| Sin Ollama, sin el filtro y sin recontar con las reglas de producción: los candidatos de los temas de cada mitad, 437 en `escribir/` y 262 en `examinar/` (prototipo del 30/9) | 699 | 4,0 · 1,0 |

Sin Ollama y con el filtro cuesta menos que esos 699; no está contado. `exam run` sin `--budget` es un ensayo: dice
cuántas consultas faltan, sin pagar ninguna. Como el examen no escribe en el libro del curso
([F-JEXAM-R002](#F-JEXAM-R002)), juzgar después el curso las vuelve a pagar.

</details>

<a id="F-DUPAR-R008"></a>
### Rule[F-DUPAR-R008] - Sin Ollama, claude -p juzga los pares de texto parecido
**Severity**: major | **Validation**: ASSUMPTION

> Sin su modelo local ([F-MLOCAL-R007](#F-MLOCAL-R007)), los candidatos del juez los propone el parecido de palabras, y
> los juzga el mismo juez de pares que con Ollama ([F-DUPAR-R009](#F-DUPAR-R009)). Invariantes:
> 1. son candidatos los pares de R001 que el comodín no resuelve y cuyos ejercicios terminados cambian, como mucho, 4 de
>    cada 10 palabras: un parecido de palabras de 0,6 o más;
> 2. a cada candidato se le pregunta a claude -p, con la misma pregunta y la misma guía que con Ollama, si para el
>    alumno es el mismo ejercicio; un sí lo hace casi duplicado, con R003 a R005 como cualquier otro, y un no lo
>    descarta;
> 3. el comodín corre siempre, con su modelo local o sin él;
> 4. consulta sólo dentro de su tope ([F-MLOCAL-R008](#F-MLOCAL-R008)); un ejercicio con algún candidato sin juzgar
>    queda evaluado a medias ([F-RONDA1-R006](#F-RONDA1-R006)): valen sus hallazgos del comodín y el informe dice
>    cuántos pares faltan.

<details><summary>Los candidatos del curso, contados</summary>

**Parecido de palabras**: 1 menos las palabras que hay que cambiar, agregar o sacar para pasar de un ejercicio terminado
al otro, sobre el largo del más largo. Es texto, sin modelo, y barato: los 24.788 pares de R001 que el comodín no
resuelve se recorren en segundos.

| Parecido de palabras | Candidatos, curso | A1 | A2 | B1 | B2 | De los 21 del parecido de 0,95 | Del 077, escribir · examinar |
|---|---:|---:|---:|---:|---:|---:|---|
| 0,50 | 1.919 | 696 | 317 | 856 | 50 | 17 | 6 de 6 · 1 de 1 |
| **0,60** | **699** | **276** | **110** | **299** | **14** | **14** | **6 de 6 · 1 de 1** |
| 0,65 | 409 | 176 | 76 | 154 | 3 | 10 | 6 de 6 · 1 de 1 |
| 0,70 | 211 | 98 | 46 | 65 | 2 | 7 | 5 de 6 · 1 de 1 |

La tabla es la del prototipo del 30/9; con las reglas de producción, a 0,60 son 696: A1 276, A2 110, B1 297 y B2 13.
En el 077 cuentan también los 5 que resuelve el comodín. Con 0,60:

- **Candidato y del 077**: «Have you ever been to Rome?» / «Have you ever visited Rome?» (`…355e` / `…4242`, B1, 0,67) y
  «Offer: I'll wash the dishes for you.» / «Offer: I'll wash the dishes.» (`…2725` / `…2fd5`, A2, 0,71).
- **Candidato que claude -p tiene que descartar**: «I am sorry.» / «I am hot.» (`…1e61` / `…1e64`, A1, *Afirmativas con
  be*, 0,67): misma respuesta, *am*, y dos ejercicios distintos. Por eso el parecido de palabras sólo propone.
- **No es candidato**: «That is his car.» / «That car is his.» (`…23f9` / `…23fe`, A1, 0,50). El modelo local lo
  marcaba (0,97) y la revisión a mano dijo que no: contrasta adjetivo y pronombre.

De los 7 que el parecido de 0,95 marcaba y no son candidatos, 3 la revisión a mano los dio por no duplicados (el de
*car*, dos veces, y «…a lot of time in Paris.» / «They often visit Paris.»). Al ritmo de los jueces, 176 por hora de a
una: el curso, 696 consultas, 4,0 horas (1,0 con 4 a la vez); A1, 276, 1,6 horas (0,4 con 4). Después de una tanda,
sólo los candidatos nuevos. Las dudas: [DOUBT-DUPAR-PARECIDO-TEXTO](#DOUBT-DUPAR-PARECIDO-TEXTO) y
[DOUBT-DUPAR-A-MEDIAS](#DOUBT-DUPAR-A-MEDIAS).

</details>

<a id="F-DUPAR-R009"></a>
### Rule[F-DUPAR-R009] - Con Ollama, los vectores sólo proponen y claude -p decide cada par
**Severity**: major | **Validation**: ASSUMPTION

> Con su modelo local disponible, los candidatos del juez los proponen los vectores de nomic-embed-text, y cada uno lo
> decide claude -p. Invariantes:
> 1. son candidatos los pares de R001 que el comodín no resuelve y cuyos vectores tienen un parecido de 0,80 o más; el
>    parecido solo no marca ningún par;
> 2. a cada candidato se le pregunta a claude -p, con la misma pregunta y la misma guía que sin Ollama
>    ([F-DUPAR-R008](#F-DUPAR-R008)), si para el alumno es el mismo ejercicio: un sí lo hace casi duplicado, con R003 a
>    R005, y un no lo descarta;
> 3. el comodín y los vectores corren siempre, sin tope y en todos los temas que alcanza la corrida:
>    `--budget near-duplicates=N` limita sólo las consultas a claude -p, nunca los vectores, y sin ese pedido no se le
>    consulta nada ([F-MLOCAL-R008](#F-MLOCAL-R008)); y si la corrida declara un conjunto de ejercicios para el juez
>    —una re-evaluación ([F-JEXAM-R005](#F-JEXAM-R005))—, ese conjunto elige qué pares se le vuelven a preguntar a
>    claude -p, nunca qué vectores se calculan: un tema fuera del conjunto cuyos pares quedan todos por debajo de 0,80
>    se evalúa entero;
> 4. sin tope o sin respuesta del juez —tope agotado, sesión vencida, servicio caído o una respuesta que no sirve—, el
>    ejercicio con algún candidato sin juzgar queda evaluado a medias ([F-RONDA1-R006](#F-RONDA1-R006)): valen sus
>    hallazgos del comodín, y el informe dice cuántos pares faltan y cuántas horas llevan, de a una y a la vez
>    ([F-JEXAM-R004](#F-JEXAM-R004)).

<details><summary>Los candidatos por umbral, y cuánto cuestan</summary>

Pares de R001 que el comodín no resuelve, por el parecido de sus vectores (nomic-embed-text, con el prefijo
«clustering: »), en la base del 29/9; las horas, al ritmo de los jueces de FEAT-JEXAM, 176 consultas por hora de a una:

| Umbral | A1 | A2 | B1 | B2 | Curso | Horas, curso (de a una · 4 a la vez) | Horas, A1 |
|---|---:|---:|---:|---:|---:|---|---|
| 0,95 | 8 | 3 | 10 | 0 | 21 | minutos | minutos |
| 0,90 | 41 | 24 | 27 | 2 | 94 | 0,5 · 0,1 | 0,2 · 0,1 |
| 0,85 | 174 | 65 | 190 | 11 | 440 | 2,5 · 0,6 | 1,0 · 0,2 |
| **0,80** | **513** | **200** | **710** | **45** | **1.468** | **8,3 · 2,1** | **2,9 · 0,7** |
| 0,75 | 1.223 | 618 | 1.646 | 141 | 3.628 | 20,6 · 5,2 | 6,9 · 1,7 |

Consulta como un juez, hasta 4 a la vez ([F-MLOCAL-R007](#F-MLOCAL-R007) inv. 3). Después de una tanda se juzgan sólo
los candidatos nuevos: lo ya contestado se reusa ([F-DUPAR-R010](#F-DUPAR-R010)). La CLI informa un costo equivalente
de unos US$ 0,017 por consulta —1.468 serían unos US$ 25—, pero con la suscripción no se cobra por consulta: gasta cupo
y horas. El umbral es [DOUBT-DUPAR-UMBRAL-CANDIDATOS](#DOUBT-DUPAR-UMBRAL-CANDIDATOS), y consultar con Ollama es una
excepción a FEAT-MLOCAL: [DOUBT-DUPAR-MLOCAL-EXCEPCION](#DOUBT-DUPAR-MLOCAL-EXCEPCION).

En un examen se juzgan sólo los pares cuya copia está en su archivo, como en el de la versión sin Ollama
([F-DUPAR-R007](#F-DUPAR-R007)): rendir son unas 255 consultas como mucho, en vez de 978.

Cambia, para `near-duplicates` con Ollama, el «`--budget nombre=N` limita la ruta que toque» de
[F-RONDA2-R002](#F-RONDA2-R002): con Ollama consulta a los dos, y el tope es sólo de claude -p. En la proyección vale lo
mismo que en `analyze` ([F-PROYU-R003](#F-PROYU-R003)): los vectores, sin tope; los candidatos nuevos, sólo con tope.

</details>

<a id="F-DUPAR-R010"></a>
### Rule[F-DUPAR-R010] - La versión con Ollama nombra sus tres piezas y rinde un solo examen
**Severity**: major | **Validation**: ASSUMPTION

> La versión de `near-duplicates` con Ollama se nombra por sus tres piezas —nomic-embed-text con su versión y su prefijo,
> el umbral de candidatos y la versión del juez de pares— y rinde un solo examen ([F-DUPAR-R007](#F-DUPAR-R007)).
> Invariantes:
> 1. cambiar cualquiera de las tres es otra versión, que vuelve a rendir antes de publicar
>    ([F-MLOCAL-R006](#F-MLOCAL-R006));
> 2. lo que el juez ya contestó sobre un par se reusa con Ollama y sin él, mientras no cambie lo que se le preguntó
>    ([F-MLOCAL-R002](#F-MLOCAL-R002));
> 3. si el juez de pares no enuncia su versión —falta su definición o su configuración—, la versión con Ollama no se
>    puede nombrar entera ni rendir examen: no se le consulta nada ni a nomic-embed-text ni a claude -p,
>    `near-duplicates` queda sin evaluar —el comodín incluido, como toda versión sin examen aprobado
>    ([F-MLOCAL-R006](#F-MLOCAL-R006))— y el informe muestra el motivo del juez.

La versión reprobada era nomic-embed-text con «clustering: » y 0,95, sin juez: la nueva no hereda su examen. Hoy serían
nomic-embed-text en su versión instalada, con «clustering: », 0,80 y la versión vigente del juez. La versión sin Ollama
es el parecido de palabras de 0,6 con ese mismo juez ([F-DUPAR-R008](#F-DUPAR-R008)): si cambia la guía del juez,
cambian las dos y las dos vuelven a rendir. Lo que se le preguntó son los dos ejercicios como los ve el alumno,
resueltos, y su tema: quién propuso el par no cambia la pregunta.

## Contexto

Base: la foto del 29/9 en `db/english-course`, 11.287 ejercicios en 413 temas, con 200.953 pares del mismo tema.
Contado con las reglas de producción, sin consultar ningún modelo.

**El examen reprobado.** La versión del 30/9 —el comodín y, con Ollama, el parecido de nomic-embed-text de 0,95 o más,
que marcaba solo— rindió el 1/10. Encontró los casi iguales del 077 (6 de 6 en `escribir/`, 1 de 1 en `examinar/`) y dos
corridas marcaron las mismas copias, pero de los 40 pares de la muestra ciega que no marcó —de 0,90 a 0,95 y con la
misma respuesta— 20 eran el mismo ejercicio, y el tope es 8: se le escapan las paráfrasis. Ese día José decidió que
ninguna tanda de correcciones se publica sin todos los jueces aprobados.

**El censo.** De los 200.953 pares, 24.813 piden lo mismo (R001; el prototipo del 30/9 contaba 24.889 porque comparaba
sólo el primer hueco); el comodín resuelve 25 y quedan 24.788. El conteo reproduce las 45 copias de la versión
reprobada y los candidatos sin Ollama:

| Medida | A1 | A2 | B1 | B2 | Curso |
|---|---:|---:|---:|---:|---:|
| Pares que resuelve el comodín | 16 | 6 | 2 | 1 | 25 |
| Copias de la versión reprobada: 25 del comodín y 20 del parecido | 23 | 9 | 12 | 1 | 45 |
| Candidatos sin Ollama: parecido de palabras de 0,6 o más | 276 | 110 | 297 | 13 | 696 |
| Candidatos con Ollama: vectores de 0,80 o más | 513 | 200 | 710 | 45 | 1.468 |

La franja de 0,90 a 0,95 tiene 73 pares; la muestra del paquete B usó 71, con una lectura más estricta de «la misma
respuesta». Los vectores se calculan en la máquina: alrededor de un minuto la primera vez y, después, sólo los de los
ejercicios que cambian.

**La calibración del umbral.** A mano, con el criterio de la guía del juez, sobre pares al azar por debajo de 0,90 y
fuera de la zona de la muestra ciega: nunca un ejercicio de un par del comodín o de 0,90 o más.

| Franja de parecido de los vectores | Candidatos en el curso | Revisados | El mismo ejercicio | Dudosos |
|---|---:|---:|---:|---:|
| 0,85 a 0,90 | 346 | 30 | 4 | 5 |
| 0,80 a 0,85 | 1.028 | 70 | 1 | 5 |
| 0,75 a 0,80 | 2.160 | 50 | 0 | 2 |

Y de 15 pares que el parecido de palabras propone y los vectores dejan por debajo de 0,85, ninguno es el mismo
ejercicio (1 dudoso): lo que sólo ve la ruta sin Ollama no suele ser una copia. La franja de 0,80 a 0,85 es grande:
aunque la tasa sea baja, puede esconder decenas de copias. Por eso 0,80, pensado para no perder ninguna, y no 0,85.

**Por qué claude -p y no llama3.** Con la mecánica de Naturalidad —un token de sí o no y sus probabilidades—, llama3:8b
en Ollama dijo «sí» a los 50 pares de un corpus inventado (20 el mismo ejercicio y 30 no, 10 de ellos con una sola
palabra de contenido cambiada), con tres guías distintas: no distingue (AUC de 0,49 a 0,67). claude -p con la guía del
juez acertó 23 de 24 pares inventados de la zona de candidatos (11 de 12 iguales y 12 de 12 distintos); el error, una
negativa y su pregunta en un tema de negativas y preguntas, se arregló aclarando la guía, y la re-prueba dio 3 de 3. Se
gastaron 27 consultas pagas, todas sobre pares inventados.

## Alcance

- **Adentro**: R001 a R010: el par candidato, las dos condiciones, el hallazgo en la copia, su ficha, el examen de cada
  versión y el juez de pares, con los candidatos que propone cada ruta.
- **Afuera**: los iguales tal cual (FEAT-DUPEX); los parecidos entre temas ([DOUBT-DUPEX-ENTRE-TEMAS](#DOUBT-DUPEX-ENTRE-TEMAS));
  cómo se reemplaza la copia (paquete E); la guía del juez palabra por palabra, que es parte de su versión; cuándo se
  usa claude -p y su tope (FEAT-MLOCAL), salvo la excepción de [F-DUPAR-R009](#F-DUPAR-R009); y escribir el curso al
  analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-DUPAR-J001] - Encontrar las copias casi iguales de un tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-DUPAR-J001
    name: Encontrar las copias casi iguales de un tema
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo near-duplicates"
        then: candidatos
      - id: candidatos
        action: "El informe junta los pares del tema que piden la misma respuesta y no son iguales tal cual"
        gate: [F-DUPAR-R001]
        outcomes:
          - when: "El par sólo cambia pronombres, demostrativos, posesivos o el tiempo del auxiliar"
            then: marca
          - when: "Con Ollama, el par llega a 0,80 de parecido y el juez dice que es el mismo ejercicio"
            then: marca_juez
          - when: "Sin Ollama, el par cambia como mucho 4 de cada 10 palabras y claude -p dice que es el mismo ejercicio"
            then: marca_alternativa
          - when: "Sin Ollama y sin tope, el par cambia como mucho 4 de cada 10 palabras y el comodín no lo resuelve"
            then: a_medias
          - when: "Ningún par del tema cumple las condiciones"
            then: sin_hallazgo
          - when: "Con Ollama, el par llega a 0,80 de parecido y el juez dice que no es el mismo ejercicio"
            then: descartado
          - when: "Con Ollama y sin tope, el par llega a 0,80 de parecido y el comodín no lo resuelve"
            then: a_medias_juez
      - id: marca
        action: "La copia lleva un hallazgo baja para la mesa, de costo instantáneo, con los dos ejercicios y qué cambia; el primero del tema queda sin hallazgo"
        gate: [F-DUPAR-R002, F-DUPAR-R003, F-DUPAR-R004, F-DUPAR-R005]
        result: success
      - id: marca_juez
        action: "La copia lleva un hallazgo baja para la mesa, de costo modelo pago, con los dos ejercicios, el parecido de los vectores y la razón del juez, y el informe nombra la versión por sus tres piezas; el primero del tema queda sin hallazgo"
        gate: [F-DUPAR-R002, F-DUPAR-R004, F-DUPAR-R005, F-DUPAR-R009, F-DUPAR-R010]
        result: success
      - id: marca_alternativa
        action: "La copia lleva un hallazgo baja para la mesa, de costo modelo pago, con los dos ejercicios y cuántas palabras cambian; el primero del tema queda sin hallazgo"
        gate: [F-DUPAR-R004, F-DUPAR-R005, F-DUPAR-R008]
        result: success
      - id: a_medias
        action: "El ejercicio queda evaluado a medias por near-duplicates: valen sus hallazgos del comodín y el informe dice cuántos pares faltan juzgar"
        gate: [F-DUPAR-R008]
        result: success
      - id: sin_hallazgo
        action: "Ningún ejercicio del tema tiene hallazgo de near-duplicates y todos quedan con puntaje 1"
        gate: [F-DUPAR-R006]
        result: success
      - id: descartado
        action: "El par no deja ningún hallazgo de near-duplicates aunque su parecido llegue a 0,80, y los dos ejercicios quedan con puntaje 1"
        gate: [F-DUPAR-R002, F-DUPAR-R009]
        result: success
      - id: a_medias_juez
        action: "El ejercicio queda evaluado a medias por near-duplicates sin ninguna consulta a claude -p: valen sus hallazgos del comodín y el informe dice cuántos pares faltan juzgar y cuántas horas llevan"
        gate: [F-DUPAR-R009]
        result: success
```

## Open Questions

<a id="DOUBT-DUPAR-CUENTA"></a>
### Doubt[DOUBT-DUPAR-CUENTA] - ¿Un casi duplicado cuenta como error o sólo se marca para revisar?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): cuenta como error, *baja*, como los exactos.
- [ ] Opción B: sólo ordena: se marca «para revisar», como dice la propuesta, y no entra en «algún error».

**Answer**: **Opción A** (José, 30/9): cuenta como error de gravedad baja, como los exactos. El 077 los contó como
duplicados, y con la misma respuesta como condición son reales entre 7 y 9 de cada 10, según cuenten los dudosos.

<a id="DOUBT-DUPAR-UMBRAL"></a>
### Doubt[DOUBT-DUPAR-UMBRAL] - ¿Desde qué parecido dos ejercicios dicen lo mismo?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): 0,95. Da 45 pares y los 6 del 077.
- [ ] Opción B: 0,97. Da 36 pares y los 6 del 077; pierde «Did the show begin on time?».
- [ ] Opción C: sin modelo, sólo la condición del comodín: 24 pares, 5 de los 6.

**Answer**: **Opción A** (José, 30/9): 0,95, confirmada con la muestra ciega de R007. C evitaba la dependencia pero
dejaba de ver la misma idea con otro verbo, como el de Rome.

**Reemplazada** el 1/10 por [DOUBT-DUPAR-UMBRAL-CANDIDATOS](#DOUBT-DUPAR-UMBRAL-CANDIDATOS): con 0,95 la versión
reprobó su examen, y el parecido ya no marca solo, sólo propone ([F-DUPAR-R002](#F-DUPAR-R002)).

<a id="DOUBT-DUPAR-PARECIDO-TEXTO"></a>
### Doubt[DOUBT-DUPAR-PARECIDO-TEXTO] - Sin Ollama, ¿desde qué parecido de palabras se le pregunta a claude -p?
**Status**: OPEN

- [ ] Opción A (recomendada): 0,60, como mucho 4 de cada 10 palabras cambian: 699 consultas en el curso y 276 en A1;
  los 7 casi iguales del 077 que no son exactos y 14 de los 21 del modelo local.
- [ ] Opción B: 0,50: 1.919 y 696; 17 de los 21.
- [ ] Opción C: 0,65: 409 y 176; 10 de los 21, y pierde pares como «Could you give me a cup of tea?» / «Could I have a
  cup of tea?».

**Answer**: Pendiente. Recomiendo A, y se diseñó con A: con 0,65 se pierden cuatro pares que parecen copias, y con 0,50 se
triplican las consultas para sumar tres.

<a id="DOUBT-DUPAR-A-MEDIAS"></a>
### Doubt[DOUBT-DUPAR-A-MEDIAS] - Sin modelo que consultar, ¿el comodín corre igual?
**Status**: OPEN

- [ ] Opción A (recomendada): sí: el comodín corre siempre, y el ejercicio con algún par sin juzgar queda evaluado a
  medias ([F-RONDA1-R006](#F-RONDA1-R006)), como la opción múltiple con su juez sin correr.
- [ ] Opción B: no: sin modelo o sin tope, todo `near-duplicates` queda sin evaluar, como decía el diseño del 30/9 a la
  tarde.

**Answer**: Pendiente. Recomiendo A, y se diseñó con A: los 25 pares del comodín son gratis y seguros, y «a medias» ya
avisa que «algún error» es un piso. La versión sin examen aprobado igual deja todo sin evaluar ([F-MLOCAL-R006](#F-MLOCAL-R006)).
Desde el 1/10 vale también con Ollama: sus candidatos sin juzgar dejan el ejercicio a medias ([F-DUPAR-R009](#F-DUPAR-R009)).

<a id="DOUBT-DUPAR-UMBRAL-CANDIDATOS"></a>
### Doubt[DOUBT-DUPAR-UMBRAL-CANDIDATOS] - Con Ollama, ¿desde qué parecido de los vectores se le pregunta al juez?
**Status**: OPEN

- [ ] Opción A (recomendada): 0,80. 1.468 candidatos en el curso, 8,3 horas de a una y 2,1 con 4 a la vez; en A1, 513,
  2,9 y 0,7.
- [ ] Opción B: 0,85. 440 en el curso, 2,5 y 0,6 horas; en A1, 174, 1,0 y 0,2. Deja afuera la franja de 0,80 a 0,85:
  1.028 pares, en la que la calibración encontró 1 igual y 5 dudosos de 70.
- [ ] Opción C: 0,90. 94 en el curso y 41 en A1, media hora de a una. Deja afuera también la franja de 0,85 a 0,90, con
  4 iguales y 5 dudosos de 30.

**Answer**: Pendiente. Recomiendo A, y se diseñó con A: está pensado para no perder ninguna copia. B ahorra unas 6 horas
de a una, pero la franja que deja afuera es grande y, aunque la tasa sea baja, puede esconder decenas de copias. Bajar
a 0,75 multiplica las consultas por 2,5 (3.628) por una franja en la que, de 50 pares, la calibración no encontró
ninguna copia.

<a id="DOUBT-DUPAR-MLOCAL-EXCEPCION"></a>
### Doubt[DOUBT-DUPAR-MLOCAL-EXCEPCION] - Con Ollama, ¿`near-duplicates` le puede preguntar a claude -p?
**Status**: OPEN

Choca con dos reglas que José validó el 30/9: [F-MLOCAL-R001](#F-MLOCAL-R001) inv. 2 —con su modelo local disponible,
`naturalness` y `near-duplicates` no mandan nada fuera de la máquina— y [F-MLOCAL-R007](#F-MLOCAL-R007) inv. 1 —con su
modelo local disponible, nunca consultan a claude -p—. Las dos quedaron escritas con la excepción y marcadas como
supuesto hasta esta respuesta.

- [ ] Opción A (recomendada): con Ollama, `near-duplicates` le pregunta a claude -p sólo por sus candidatos —los dos
  ejercicios de cada par que proponen sus vectores—, dentro de su tope ([F-DUPAR-R009](#F-DUPAR-R009)). `naturalness`
  no cambia.
- [ ] Opción B: con Ollama no le pregunta: los candidatos quedan a medias hasta que haya un juez local que apruebe su
  examen, y mientras tanto marca sólo el comodín.

**Answer**: Pendiente. Recomiendo A, y se diseñó con A. La opción de claude -p la puso José sobre la mesa en el pedido de
esta tarea («llama3 local, gratis y más flojo, o claude -p, cupo de José»), y hoy no hay un juez local que apruebe:
llama3:8b dijo «sí» a todos los pares de un corpus inventado, con tres guías (ver Contexto). Con B, las paráfrasis que
hicieron reprobar a la versión de 0,95 siguen sin marcarse, y `near-duplicates` marca menos con Ollama que sin él.

<a id="DOUBT-DUPAR-MUESTRA-NUEVA"></a>
### Doubt[DOUBT-DUPAR-MUESTRA-NUEVA] - ¿El examen con Ollama suma una muestra ciega de por debajo de 0,90?
**Status**: OPEN

- [ ] Opción A (recomendada): además de la muestra del paquete B, una de 20 pares que la versión marca y 20 que juzga y
  no marca, de por debajo de 0,90 y que nadie revisó, con la misma meta: de los 20 marcados, al menos 16 son el mismo
  ejercicio, y de los otros 20, 4 como mucho. Unos 10 minutos de una persona.
- [ ] Opción B: sólo la muestra del paquete B.

**Answer**: Pendiente. Recomiendo A: la muestra del paquete B mira de 0,90 para arriba, y 1.374 de los 1.468 candidatos
están por debajo; sin A, el examen no mide al juez donde más decide. [F-DUPAR-R007](#F-DUPAR-R007) trae las partes que
valen con las dos opciones; con A, la parte 2 suma ésta.

## References

- **FEAT-HALL** — Hallazgo, gravedad, conteo, identidad, ficha y plan: R003 a R006.
- **FEAT-DUPEX** — Ejercicio terminado, los exactos y cuál queda: R001, R003.
- **FEAT-MLOCAL** — El modelo local, su versión y su examen, qué pasa si falta, la alternativa con su tope y la
  excepción que hace el juez de pares con Ollama: R006 a R010.
- **FEAT-RONDA1** — El ejercicio evaluado a medias: R008, R009.
- **FEAT-JEXAM** — El examen que no escribe en el libro del curso, el ritmo de claude -p y lo que falta, declarado: R007,
  R009.
- **FEAT-RONDA2, FEAT-PROYU** — La corrida base, que sigue sin correr `near-duplicates`, y el tope por ruta, también en
  la proyección: R006, R009.
