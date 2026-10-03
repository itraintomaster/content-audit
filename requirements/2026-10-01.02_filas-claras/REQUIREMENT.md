---
feature:
  id: FEAT-FILAS
  code: F-FILAS
  name: Filas claras - cada fila dice qué cuenta y si su cambio es bueno o malo
  priority: high
---

# Filas claras: cada fila dice qué cuenta y si su cambio es bueno o malo

## TL;DR

**Qué**: Las filas de «Antes y después» se llaman por lo que cuentan. Las de problemas dicen el problema, y menos es
mejor; las de puntaje dicen lo bueno, y más es mejor. Además, cada fila de `compare -f json` dice:
- para qué lado mejora;
- si su cambio mejoró, empeoró, quedó igual o no se evaluó;
- una frase que la explica;
- un ejemplo real del análisis.

**Por qué**: José vio dos problemas en la tabla de la app de revisión (1/10):
- «Respuestas completas» cuenta los ejercicios con respuestas *incompletas*: cuando el número baja parece que empeoró,
  y es al revés. Pasa lo mismo con otras filas.
- El cambio no dice si es bueno o malo: en errores bajar es bueno, en vocabulario subir es bueno. Y un analizador que no
  evaluó sale como «0 (0,0 %)», que parece un cero real.

## Recorrido de uso (estado actual)

Cómo José lee hoy una tanda en la tabla «Antes y después»:

1. Abre la tanda en la app → la app pinta `compare <curso> -f json` ([F-METAS-R001](#F-METAS-R001)). ✓ Disponible.
2. Lee el nombre de la fila → ⚠️ la ficha dice lo bueno aunque la fila cuente lo malo: «Respuestas completas», «Pista
   coherente», «Datos coherentes».
3. Lee el cambio → ⚠️ «−2 ejercicios» o «−0,5 puntos», sin decir si es bueno. La app lo tendría que deducir de la
   familia.
4. Lee la fila de un juez que corrió sin tope → ⚠️ «0 (0,0 %)», igual que un cero de verdad.
5. Quiere saber qué cuenta la fila → ⚠️ no hay frase ni ejemplo.

**Punto de fricción**: pasos 2 a 5.

**Cambio mínimo necesario**: nombres nuevos en las fichas, y en cada fila de la comparación su nombre visible y cuatro
datos que decide content-audit: para qué lado mejora, el veredicto del cambio, la frase y el ejemplo. La app sólo los
pinta.

## Reglas de Negocio

Acá una **fila de problemas** es la de un analizador de la familia de errores o la de «algún error», y una **fila de
puntaje**, la de un analizador de la familia de vocabulario o la de «vocabulario».

<a id="F-FILAS-R001"></a>
### Rule[F-FILAS-R001] - Cada fila se llama por lo que cuenta
**Severity**: major | **Validation**: VALIDATED

> El nombre en castellano de cada analizador, el que muestran `get analyzers` y la comparación, es el de la tabla de
> abajo:
> - una fila de problemas dice el problema, y menos es mejor;
> - una fila de puntaje dice lo bueno, y más es mejor.
>
> La comparación muestra la fila de «vocabulario» como «Vocabulario, puntaje total» y la de «algún error» como
> «Ejercicios con algún error». Invariantes:
> 1. el nombre único no cambia, y sigue siendo el único con que se nombra a un analizador
>    ([F-HALL-R005](#F-HALL-R005));
> 2. «vocabulario» y «algún error» siguen siendo la clave de sus filas: las metas que los nombran, las declaradas y las
>    fijadas, valen igual ([F-METAS-R005](#F-METAS-R005), [F-METAS-R006](#F-METAS-R006));
> 3. los nombres son distintos entre sí y nunca vacíos ([F-RONDA1-R004](#F-RONDA1-R004));
> 4. un analizador que el catálogo de hoy no conoce se muestra con su nombre único.

Reemplaza los nombres de la tabla de [F-RONDA1-R004](#F-RONDA1-R004) y los tres de [F-RONDA2-R001](#F-RONDA2-R001).

<details><summary>Los veinte analizadores y las dos filas generales, en el orden de la comparación</summary>

| Nombre único | Familia | Nombre hasta el 1/10 | Nombre en castellano |
|---|---|---|---|
| `sentence-length` | vocabulario | Largo de oración | Oraciones con el largo de su nivel |
| `knowledge-title-length` | vocabulario | Largo del título | Títulos de largo adecuado |
| `knowledge-instructions-length` | vocabulario | Largo de la consigna | Consignas de largo adecuado |
| `coca-buckets-distribution` | vocabulario | Bandas de frecuencia | Palabras de la frecuencia del nivel |
| `lemma-recurrence` | vocabulario | Recurrencia | Palabras que vuelven a tiempo |
| `lemma-absence` | vocabulario | Palabras que faltan | Vocabulario del nivel presente |
| `off-level-words` | errores | Palabras de otro nivel | Palabra de otro nivel |
| `lemma-count` | vocabulario | Palabras poco vistas | Palabras vistas suficientes veces |
| `quiz-instruction` | errores | Consigna | Consigna que no se cumple |
| `hint-coherence` | errores | Pista coherente | Pista que falta, confunde o regala la respuesta |
| `answer-completeness` | errores | Respuestas completas | Exige una forma o rechaza una respuesta válida |
| `multiple-choice-validity` | errores | Opción múltiple válida | Opción múltiple mal armada |
| `data-coherence` | errores | Datos coherentes | Audio o respuesta que no coincide |
| `exact-duplicates` | errores | Duplicados exactos | Ejercicio repetido igual |
| `translation-sense` | errores | Traducción y sentido | Traducción que no corresponde |
| `single-answer` | errores | Una sola respuesta | Más de una respuesta válida |
| `mini-theory` | errores | Miniteoría | Miniteoría con error |
| `grammar` | errores | Gramática | Error de gramática |
| `naturalness` | errores | Naturalidad | Oración poco natural |
| `near-duplicates` | errores | Duplicados parecidos | Ejercicio casi repetido |
| «vocabulario» | vocabulario | vocabulario | Vocabulario, puntaje total |
| «algún error» | errores | algún error | Ejercicios con algún error |

Los aprobó José el 1/10. El de `hint-coherence` lo cambió José el mismo día, a pedido de la sesión de prueba del
contenido: la fila va a contar también la pista que regala la respuesta, una regla que suma otra rama
(`ronda2/pista-que-regala`). La observación de `single-answer` que manda a `answer-completeness` nombra a los dos con
sus nombres nuevos, entre comillas angulares.

</details>

<a id="F-FILAS-R002"></a>
### Rule[F-FILAS-R002] - Cada fila dice para qué lado mejora
**Severity**: major | **Validation**: VALIDATED

> Cada fila de la comparación dice si es mejor que su número baje —las de problemas— o que suba —las de puntaje—.

**Criterio**: la fila de `grammar` en un tema dice que es mejor que baje; la de `lemma-absence` y la de «vocabulario»,
que suba; la de «algún error», que baje.

<a id="F-FILAS-R003"></a>
### Rule[F-FILAS-R003] - Cada fila dice si su cambio mejoró, empeoró, quedó igual o no se evaluó
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada fila de la comparación trae el veredicto de su cambio contra la línea base, la re-medida si cambió la medida
> ([F-METAS-R007](#F-METAS-R007)):
> - **no evaluado**, si en la línea base o en la proyección la fila no tiene número, o el analizador no evaluó ningún
>   ejercicio del nodo; en «algún error», si ningún analizador de errores evaluó un ejercicio del nodo;
> - si no, **igual**, si el cambio, redondeado como se muestra, es cero: a dos decimales en puntos, a ejercicios enteros
>   en errores;
> - si no, **mejoró**, si fue para el lado bueno de la fila ([F-FILAS-R002](#F-FILAS-R002)), y **empeoró**, si fue para
>   el otro.
>
> Invariantes:
> 1. lo decide content-audit: la app pinta el veredicto y no lo deduce de los números;
> 2. una fila no evaluada nunca se lee como un cero: sus números salen igual que hoy, con sus ejercicios sin evaluar;
> 3. el veredicto no cambia el estado de ninguna meta ni el de la tanda ([F-METAS-R002](#F-METAS-R002),
>    [F-METAS-R008](#F-METAS-R008)).

<details><summary>Criterio, con la tanda de «Afirmativas regulares»</summary>

| Fila | Antes | Después | Veredicto |
|---|---:|---:|---|
| `lemma-absence` · «Afirmativas regulares» | 100,0 | 99,5 | empeoró |
| `grammar` · tema, con un error que la tanda arregla | 1 | 0 | mejoró |
| «vocabulario» · curso | 73,92 | 73,92 | igual |
| `single-answer` · tema, sin tope: ningún ejercicio juzgado | 0 | 0 | no evaluado (hoy, «0 (0,0 %)») |
| `grammar` · la base del 29/9, que corrió sin analizadores de errores | — | 0 | no evaluado |

Un ejercicio evaluado a medias cuenta como evaluado ([F-RONDA1-R006](#F-RONDA1-R006)), así que la fila tiene veredicto
aunque queden ejercicios abiertos; si su meta se puede decidir lo dice su estado ([F-METAS-R002](#F-METAS-R002)).

</details>

<a id="F-FILAS-R004"></a>
### Rule[F-FILAS-R004] - Cada fila trae una frase que la explica
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cada ficha trae, además de su nombre en castellano, una frase que dice qué ejercicios cuenta, en una de problemas, o
> qué puntúa de 0 a 100, en una de puntaje. `get analyzers` la muestra, en texto y en JSON, y cada fila de la comparación
> la lleva. «vocabulario» y «algún error» traen la suya. Invariantes:
> 1. es la de la tabla de abajo;
> 2. cambiarla no es un cambio de medida ([F-RONDA2-R004](#F-RONDA2-R004)), como no lo es el nombre;
> 3. un analizador que el catálogo de hoy no conoce no trae frase.

<details><summary>Las frases</summary>

| Nombre único | Frase |
|---|---|
| `sentence-length` | Puntaje de 0 a 100: qué tanto las oraciones tienen el largo que corresponde a su nivel. |
| `knowledge-title-length` | Puntaje de 0 a 100: qué tanto los títulos de tema entran en la pantalla del teléfono. |
| `knowledge-instructions-length` | Puntaje de 0 a 100: qué tanto las consignas de los temas entran en la pantalla del teléfono. |
| `coca-buckets-distribution` | Puntaje de 0 a 100: qué tanto las palabras de cada nivel son tan comunes como corresponde. |
| `lemma-recurrence` | Puntaje de 0 a 100: qué tanto las palabras vuelven a aparecer a intervalos sanos. |
| `lemma-absence` | Puntaje de 0 a 100: cuántas de las palabras que el nivel tiene que enseñar aparecen, sin palabras de un nivel más alto. |
| `off-level-words` | Ejercicios con una palabra que el alumno de ese nivel todavía no puede conocer: de dos niveles más arriba, o rara y fuera del catálogo. |
| `lemma-count` | Puntaje de 0 a 100: qué tanto cada palabra del nivel aparece en suficientes oraciones. |
| `quiz-instruction` | Ejercicios que no cumplen lo que pide su consigna. |
| `hint-coherence` | Ejercicios para escribir sin pista cuando la llevan tres cuartos o más de los de su tema, con una pista que no le dice al alumno qué escribir, o con una pista que es la respuesta misma. |
| `answer-completeness` | Ejercicios cuya consigna o miniteoría pide una forma (la corta o la larga), o cuyo corrector rechaza otra forma igual de correcta de la respuesta. |
| `multiple-choice-validity` | Opción múltiple con más de una opción correcta (o ninguna), con opciones repetidas o con una que se delata por la forma. |
| `data-coherence` | Ejercicios cuyas copias no dicen lo mismo: la oración guardada o el audio no coinciden con el enunciado y su respuesta, una respuesta o el audio llevan «/», o el audio falta o no se sabe qué dice. |
| `exact-duplicates` | Ejercicios que, terminados, dicen exactamente lo mismo que otro anterior del mismo tema. |
| `translation-sense` | Ejercicios cuyo inglés y traducción no dicen lo mismo, que un nativo no diría así, con palabras que no van juntas o con un registro que no es del nivel. |
| `single-answer` | Ejercicios para escribir donde, con lo que ve el alumno, hay otra respuesta igual de buena que el corrector no acepta. |
| `mini-theory` | Ejercicios cuya tarjeta de teoría dice una regla falsa o exagerada, o que contradicen esa regla o quedan fuera de lo que explica. |
| `grammar` | Ejercicios cuya oración terminada, con la respuesta puesta, tiene un error de gramática, de ortografía o de puntuación final. |
| `naturalness` | Ejercicios cuya oración terminada un modelo duda que un nativo diría así. Sólo ordena qué revisar primero: no cuenta como error. |
| `near-duplicates` | Ejercicios que piden la misma respuesta y casi lo mismo que otro anterior del mismo tema: otro pronombre, otro tiempo o la misma idea con otras palabras. |
| «vocabulario» | El puntaje de vocabulario de 0 a 100: el promedio de las filas de puntaje de arriba. |
| «algún error» | Ejercicios con al menos uno de los problemas de arriba; cada ejercicio cuenta una vez. |

Las frases eran un borrador y se compararon con lo que mide cada analizador: su descripción, su pregunta, sus reglas y
su meta. Se corrigieron las que no lo decían bien:
- `hint-coherence`: sin pista es error sólo cuando la llevan tres cuartos o más de su tema;
- `answer-completeness`: la forma la puede pedir también la miniteoría;
- `multiple-choice-validity`: también marca ninguna correcta y opciones repetidas;
- `data-coherence`: también marca la «/» y el audio que falta o no se conoce;
- `exact-duplicates` y `near-duplicates`: cuenta la copia que viene después, no las dos; la parecida pide la misma
  respuesta;
- `off-level-words`: una palabra de un solo nivel más arriba no cuenta acá;
- `translation-sense`: también cuenta palabras que no van juntas y el registro;
- `mini-theory`: también la regla exagerada y el ejercicio que queda fuera de la regla;
- `grammar`: también la ortografía y la puntuación final;
- `naturalness`: sólo ordena, nunca cuenta como error, así que su fila no sube;
- `knowledge-instructions-length`: mide la consigna del tema, no la de cada ejercicio;
- `lemma-absence`: una palabra de un nivel más bajo no le resta;
- «vocabulario»: es el promedio de los puntajes ([F-HALL-R009](#F-HALL-R009)).

</details>

<a id="F-FILAS-R005"></a>
### Rule[F-FILAS-R005] - Cada fila trae un ejemplo real del análisis, o ninguno
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cada fila de la comparación trae como ejemplo un hallazgo real del análisis, o ninguno si no hay:
> - en la fila de un analizador, un hallazgo suyo en el nodo o debajo; en una de problemas, sólo uno que cuenta como
>   error, nunca uno que sólo ordena;
> - en «algún error», un hallazgo que cuenta como error, de cualquier analizador; en «vocabulario», uno de cualquier
>   analizador de vocabulario, y su línea nombra al analizador.
>
> El ejemplo dice el ejercicio como lo ve el alumno —el enunciado con sus huecos y sus pistas, y las opciones en la
> opción múltiple— y, en una línea, qué tiene. Un hallazgo que no es de un ejercicio, como un título o una palabra del
> nivel, va sin ejercicio. Se elige siempre igual:
> 1. primero, uno que la tanda arregla: estaba al inicio de la tanda, no está en la proyección, y el analizador sí
>    evaluó ese ejercicio en la proyección; su línea dice que la tanda lo arregla;
> 2. si no hay, uno de la proyección;
> 3. entre ellos, el más concreto —un ejercicio antes que un tema, un topic, un nivel o el curso— y, a igual nivel, el de
>    identificador menor; después, la regla y lo que marca.
>
> Invariante: la misma comparación da siempre el mismo ejemplo.

**Criterio**: en «Afirmativas regulares», la fila de `lemma-absence` trae el ejercicio «My grandmother ___ (come) every
year.» y la línea «grandmother es una palabra de A2 en un ejercicio de A1». La fila de `lemma-count` en A1 trae una
palabra poco vista, como «butter (A1): 2 oraciones de 6», sin ejercicio. Un error que la tanda arregla gana sobre uno
que queda, y un ejercicio que el analizador dejó sin evaluar en la proyección no cuenta como arreglado.

<a id="F-FILAS-R006"></a>
### Rule[F-FILAS-R006] - `compare -f json` publica todo esto en cada fila
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En `compare <curso> -f json`, cada fila, también la de un piso roto, lleva:
> - su nombre visible ([F-FILAS-R001](#F-FILAS-R001));
> - para qué lado mejora, «LOWER» o «HIGHER» ([F-FILAS-R002](#F-FILAS-R002));
> - el veredicto de su cambio, «IMPROVED», «WORSENED», «UNCHANGED» o «NOT_EVALUATED» ([F-FILAS-R003](#F-FILAS-R003));
> - su frase ([F-FILAS-R004](#F-FILAS-R004));
> - su ejemplo, como `{quizId, exercise, detail}`, o `null` cuando no hay ([F-FILAS-R005](#F-FILAS-R005)).
>
> Invariantes:
> 1. los campos de antes no cambian ([F-METAS-R001](#F-METAS-R001));
> 2. el ejemplo sale aunque sea `null`: la app no tiene que adivinar si falta.

**Criterio**: el JSON de muestra contra el que se hace la app trae al menos una fila de cada familia, una no evaluada y
una con ejemplo. Lo produce una prueba con el comando de verdad, sobre una tanda de dos ejercicios.

<a id="F-FILAS-R007"></a>
### Rule[F-FILAS-R007] - El veredicto dice cuántos pisos no se pueden decidir y de qué filas
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando algún piso no se puede decidir ([F-METAS-R002](#F-METAS-R002)), el veredicto lo dice aparte de su texto:
> - cuántos son, los mismos que cuenta el texto del veredicto;
> - de qué filas, cada una con su clave y su nombre visible ([F-FILAS-R001](#F-FILAS-R001)), sin repetir y en el orden de
>   las filas: el del catálogo, y después «vocabulario» y «algún error».
>
> Si no hay ninguno, lo dice vacío. Invariantes:
> 1. lo trae con cualquier veredicto, también con «no pasa»;
> 2. el texto del veredicto y su tipo no cambian ([F-METAS-R008](#F-METAS-R008)).

**Criterio**: en `compare -f json` es `verdict.undecidableFloors`, como
`{"count": 32, "analyzers": [{"subject": "quiz-instruction", "displayName": "Consigna que no se cumple"}, …]}`, y `null`
cuando todos los pisos se deciden. Lo pidió la app el 1/10: hasta entonces sólo tenía el texto largo del veredicto.

<a id="F-FILAS-R008"></a>
### Rule[F-FILAS-R008] - Cada número de la fila dice si su foto se evaluó
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cada número de una fila dice si el analizador evaluó algún ejercicio del nodo en su foto, con el mismo criterio que
> [F-FILAS-R003](#F-FILAS-R003). Vale para el de la línea base, el de la línea base re-medida si la hay, el del inicio de
> la tanda y el de la proyección. Invariantes:
> 1. una foto sin evaluar no tapa los números de las otras: cada una dice lo suyo;
> 2. el veredicto del cambio no cambia ([F-FILAS-R003](#F-FILAS-R003)).

**Criterio**: en `compare -f json`, cada uno de `baseline`, `remeasuredBaseline`, `approved` y `projected` lleva
`evaluated` en `true` o `false`. Con `quiz-instruction` sin tope sólo en la proyección, la fila dice «no evaluado», pero la
línea base y el inicio de la tanda siguen diciendo que se evaluaron. Lo pidió la app el 1/10.

## Contexto

- **El criterio y los nombres** los aprobó José el 1/10: las filas de problemas dicen el problema y las de puntaje, lo
  bueno.
- **Las frases** eran un borrador; acá están corregidas (R004).
- **La app** es la de revisión (Vocabulary Dashboard). Hoy busca el nombre de cada fila en la ficha de su analizador, y
  si no la encuentra muestra la clave: por eso «vocabulario» y «algún error» salían en minúscula.
- **Dos nombres no dicen todo lo que la fila cuenta**:
  - «Traducción que no corresponde» también cuenta el inglés que un nativo no diría, las palabras que no van juntas y el
    registro;
  - «Oración poco natural» es de un analizador que sólo ordena, así que su fila nunca suma.

## Alcance

- **Adentro**: R001 a R008.
- **Afuera**:
  - cómo pinta la app las filas;
  - el texto de `compare` en la terminal, que sigue igual;
  - la regla de `hint-coherence` de la pista que regala la respuesta, que suma otra rama.

## Referencias

- **FEAT-METAS** — Las filas, sus metas, la re-medida y el veredicto de la tanda: R001, R002, R005 a R008.
- **FEAT-RONDA1** — El nombre en castellano de cada ficha y lo evaluado a medias: R004 y R006.
- **FEAT-RONDA2** — Los nombres de los tres analizadores de B y la medida de cada uno: R001 y R004.
- **FEAT-HALL** — El nombre único y el puntaje de vocabulario: R005 y R009.
