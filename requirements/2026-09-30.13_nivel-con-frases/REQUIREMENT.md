---
feature:
  id: FEAT-NIVFR
  code: F-NIVFR
  name: Palabras de otro nivel, mirando las frases del EVP
  priority: high
---

# Palabras de otro nivel, mirando las frases del EVP

## TL;DR

**Qué**: Un analizador nuevo de la familia de errores, `off-level-words`, cuenta los ejercicios con palabras que el alumno
de su nivel no puede conocer, dos niveles arriba o fuera del EVP, leídas con el catálogo y las frases de `lemma-absence`.

**Por qué**: `lemma-absence` marca 337 ejercicios de A1 y el 077 marcó 17. José decidió el 30/9 que el error lo cuente un
analizador aparte, con su ficha, y que `lemma-absence` siga entero en el 73,9 %.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber qué ejercicios usan palabras que el alumno no conoce:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: `lemma-absence` deja en cada
   ejercicio sus palabras de otro nivel y fuera del catálogo ([F-LABS-R017](#F-LABS-R017), [F-LABS-R038](#F-LABS-R038)).
2. Prueba una oración suelta → `LexisCommand.queryLexis(quizSentence, mode, level)`. ✓ Disponible ([F-CLEX-R006](#F-CLEX-R006)).
3. Cuenta los ejercicios con error por tema y nivel. ⚠️ Sólo hay un puntaje, que marca todo lo que queda un nivel arriba:
   337 ejercicios en A1, 185 en A2, 225 en B1 y 1 en B2. No sabe qué palabra enseña el tema ni que «sushi» se entiende sola.

**Punto de fricción**: paso 3. Contar como errores las marcas de `lemma-absence` llenaría «algún error» de ruido, y
cambiarlo movería el 73,9 %.

**Cambio mínimo necesario**: un analizador de errores que lee el nivel de cada palabra como `lemma-absence`, cuenta sólo
lo que queda lejos del nivel y exceptúa dos casos que salen del curso. No cambia ningún puntaje, diagnóstico ni tarea.

## Reglas de Negocio

Grupos: **A**, qué palabra es de otro nivel (R001–R004); **B**, qué deja y qué no toca (R005–R007). Base: análisis
2026-09-30T11-54-02 (curso del 29/9, 11.287 ejercicios), medido con un prototipo que reproduce las marcas de
`lemma-absence` en los 11.287.

### Grupo A — Qué palabra es de otro nivel

<a id="F-NIVFR-R001"></a>
### Rule[F-NIVFR-R001] - El nivel de cada palabra es el que resuelve lemma-absence
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Para cada palabra de contenido de la oración terminada, el analizador usa el [Nivel CEFR](glossary:Nivel CEFR) que
> resuelve `lemma-absence`: mismo tokenizador, mismo catálogo, mismas reglas. Invariantes: 1. una frase del EVP de nivel
> más bajo baja el nivel de su [lema](glossary:Lema), y ninguna lo sube; 2. nunca asigna un nivel que el catálogo no
> tenga; 3. toda palabra que marca figura en lo que `lexis` marca para esa oración y ese nivel.

Mantiene [F-LABS-R005](#F-LABS-R005), [F-LABS-R033](#F-LABS-R033) a [F-LABS-R036](#F-LABS-R036), [F-LABS-R040](#F-LABS-R040) a
[F-LABS-R046](#F-LABS-R046) y [F-CLEX-R006](#F-CLEX-R006). Por [F-LABS-R042](#F-LABS-R042) las frases ya dejan dentro de su
nivel 381 palabras en 369 ejercicios (168 en A1): «a lot of tea» («lot» A2, «a lot/lots» A1, 33 veces), «She lives in
London» («live» B1, «live in/at» A1, 44 veces). Dudas: [DOUBT-REBAJA-EN-CONTEXTO](#DOUBT-REBAJA-EN-CONTEXTO) y
[DOUBT-FRASES-SUPERIORES](#DOUBT-FRASES-SUPERIORES).

<a id="F-NIVFR-R002"></a>
### Rule[F-NIVFR-R002] - Cuenta como error sólo lo que queda lejos del nivel
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una palabra de contenido es de otro nivel cuando: 1. su nivel del catálogo está dos o más niveles arriba del ejercicio
> (C1 = 5 y C2 = 6, como [F-LABS-R033](#F-LABS-R033)); o 2. no está en el catálogo y cae en la banda de descuento fuerte
> de [F-LABS-R034](#F-LABS-R034) para ese nivel. Una palabra un solo nivel arriba nunca es de otro nivel para este analizador.
> Tampoco la que una lectura regular deja a menos de dos niveles, medida con el mismo puntuador de R001: a. la flexión
> regular de un lema del catálogo cuando la del lematizador es absurda («hoped» no sale de «hop», que daría «hopped»: es
> «hope», A2; «partied» es «party»); b. el mismo lema en otra categoría, cuando el etiquetador lee una lista de palabras
> y no una oración, sin determinante, pronombre, auxiliar ni preposición («run» en «run - running» es el verbo, A1; en
> «He paid a fine», fine es el sustantivo, B1, y no el adjetivo); c. un nombre propio, un gentilicio o un idioma, con
> mayúscula en medio de la oración («Portuguese», «Argentinian»); y, sólo fuera del catálogo: d. su forma británica, por
> los patrones de ortografía (color, colour) o por una lista cerrada de pares del inglés americano y británico que
> nombran lo mismo en el uso corriente («takeout» es «takeaway», B1; «recess» no es «break»); e. un compuesto de dos
> palabras de tres letras o más, cada una del nivel del ejercicio o de antes («hairbrush», «headphones», «lifeguard»);
> f. un derivado con un afijo transparente (-y, -ing, -ed, -er, -ful, -less, -ness, -ist, -ian, un-, mal-), que toma el
> nivel de su base («creamy», «calming», «stimulating», «dietitian»), o un adverbio en -ly, que toma dos niveles más,
> como hace el EVP (beautiful A1, beautifully B1): «bitterly» en B2, nunca «soundly» ni «awfully» en A2. **El nivel que
> el catálogo le da a la palabra misma gana** a d., e. y f. y al préstamo ([R004](#F-NIVFR-R004)), que toman el nivel de
> un par, una parte, una base o la palabra castellana: valen sólo para la palabra que el catálogo no trae, ni por su lema
> ni como forma derivada o compuesta de otro lema («rewarding», C1, forma de reward). Sólo le gana el tema
> ([R003](#F-NIVFR-R003)), con el sufijo que enseña: en «Adverbios de modo», formar «nicely» (B2) desde «nice» es el
> ejercicio.

<details><summary>Detalle: por qué el umbral, con datos</summary>

De los 337 de A1, 329 sólo tienen palabras un nivel arriba, y 281 son ejercicios escritos para A2 que la fusión del 26/9
pasó a A1 (el 14/8, A1 tenía 56 marcados de 1.868). Las más marcadas son útiles de A2: let 20, arrive 9, few 9, slowly 8.

| | A1 | A2 | B1 | B2 | Curso |
|---|---:|---:|---:|---:|---:|
| `lemma-absence`, palabra de otro nivel | 337 | 185 | 225 | 1 | 748 |
| El 077, nivel | 17 | 35 | 50 | 23 | 125 |
| `off-level-words`, con R003 y R004 (30/9) | 18 | 33 | 81 | 38 | 170 |
| `off-level-words`, con las lecturas de a. a f. y R003 y R004 ampliadas (1/10) | 9 | 25 | 42 | 19 | 95 |
| `off-level-words`, con el nivel propio del catálogo primero y el tema por encima (1/10, segunda corrección) | 9 | 27 | 43 | 20 | 99 |

En A1 marca 11 palabras fuera del EVP y 9 de B1 o B2; en B1 y B2, casi todo es fuera del EVP. Umbral:
[DOUBT-UMBRAL-DE-ERROR](#DOUBT-UMBRAL-DE-ERROR), que José resolvió el 30/9: dos niveles; esa parte sigue validada.

**Las lecturas (1/10).** Un verificador independiente revisó 25 marcas fuera del examen y sólo 5 eran errores: el resto
eran lecturas absurdas del lematizador (partied, hoped), compuestos y derivados transparentes, variantes americanas y
préstamos, vocabulario que el tema enseña y metalenguaje del curso. Las lecturas a. a f. salen de reglas generales del
inglés y de los datos del curso; la única lista cerrada es la de pares del inglés americano y británico. El compuesto pide
sus dos partes del nivel o de antes porque hay compuestos opacos: «sideboard» (side A2, board A1) sigue siendo error en
A1, como marcó el 077. El adverbio en -ly toma dos niveles más porque el EVP lo pone así sobre su adjetivo (nice A1 y
nicely B2, bright A2 y brightly B2) y porque el 077 marcó los de A2 que no están en el EVP. Ninguna lectura sube un nivel
ni marca una palabra que `lexis` no marque ([R001](#F-NIVFR-R001), invariante 3).

**El nivel propio primero (1/10, segunda corrección).** Medidas contra la mitad del examen que nadie vio, las lecturas
perdían casos reales que antes se encontraban: la heurística de la base pisaba el nivel que el EVP le da a la palabra
misma. Tres reglas: el nivel propio gana a la base, la parte y el par británico; el préstamo es sólo de sustantivos
([R004](#F-NIVFR-R004)); la variante americana toma el nivel de su par sólo si es la misma palabra con otra grafía o un
sinónimo exacto de uso común, y por eso salieron de la lista highway, closet, semester, recess, attorney, stove,
comforter y pacifier. La otra categoría pasó a valer sólo en una lista de palabras por la misma razón: en una oración, la
otra categoría suele ser otra palabra. El sufijo que el tema enseña sigue por encima del nivel propio (decisión del
orquestador, 1/10): nicely, sadly, warmly, beautifully y smoking no marcan donde el tema enseña a formarlas. Vuelven 4
marcas y no se va ninguna: allergic y ceramic (A2), recess (B1) y diligently (B2). Lo que el catálogo trae como forma
derivada o compuesta de otro lema (rewarding C1, de reward; smoking A1, de smoke) F-LABS-R040 lo deja, con razón, fuera
del nivel de su lema, y ningún método del puerto lo consultaba: el parche del 1/10 lo suma ([TECH_SPEC](TECH_SPEC.md)).
En el curso del 29/9 no cambia ninguna marca, porque las dos están en temas que enseñan su sufijo («-ed vs -ing»,
«Doble consonante y e»); cuida al derivado que el EVP trae en un nivel alto y que la lectura de su base soltaría.

</details>

<a id="F-NIVFR-R003"></a>
### Rule[F-NIVFR-R003] - Lo que el tema enseña no es error
**Severity**: major | **Validation**: AUTO_VALIDATED

> Una palabra no cuenta en un ejercicio cuando su tema la enseña: 1. figura entre los términos en inglés del título del
> tema, los que van entre `$`; o 2. es parte de la respuesta de al menos 3 ejercicios del tema y del 20 % o más de ellos;
> o 3. la miniteoría de un ejercicio del tema la nombra sola entre `$` («$headphones$, $pants$ y $pajamas$ van siempre en
> plural»): un término más largo, como «$a warranty for$», enseña la combinación y no la palabra; o 4. un sufijo que el
> tema enseña entre `$`, en su título o sus miniteorías («Adjetivo + $-ly$», «$+ing$»), la forma desde una palabra del
> nivel del ejercicio o de antes («nicely» en A1, por «nice»), aunque el EVP la traiga con su nivel: cuando el tema enseña
> justamente a formarla, usarla es el ejercicio ([R002](#F-NIVFR-R002)); o 5. es parte del nombre de un tema del curso, su propio
> metalenguaje («modal» en «modal verbs», por «Modal Verbs»), sin los ejemplos que ese nombre cite entre `$`; o 6. es
> palabra del rótulo que abre el enunciado de al menos 3 ejercicios del tema y del 20 % o más de ellos: una o dos palabras
> y dos puntos, al principio del enunciado o justo después de una oración («Polite request: I want you to help me.»,
> «Play the radio. Polite:»). El rótulo es la indicación del ejercicio, no su contenido; dos puntos en medio de una
> oración («I have two things:») no son un rótulo.
> Sale de los datos del curso, nunca de una lista escrita a mano.

Saca 9 ejercicios de A2 («$Invest time$ + gerundio»: invest es B2) y 28 de B1 («Orden o simultaneidad», con sequence y
simultaneous como respuesta; «¿Con coma o sin coma?», con non-defining), y ninguno del 077. El semáforo usa la misma
excepción en 1, 2 y 6 ([F-SEMAF-R004](#F-SEMAF-R004)), y también la medición del vocabulario
([F-LABS-R047](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R047)); 3 a 5 son, por ahora, sólo de este analizador. El 6 (25/10) salió de «Pedidos: formal o informal», donde
polite y request (A2) del rótulo «Polite request:» se marcaban en cada ejercicio de A1 y el corrector borraba el rótulo.

<details><summary>Detalle: la miniteoría, el sufijo y el metalenguaje (1/10)</summary>

En «Siempre plurales» las respuestas son «correct» y «not correct», así que 2 no encuentra las palabras del tema; la
miniteoría sí: «$headphones$», «$pajamas$», «$couscous$», «$spaghetti$», «$veritable$», «$Apprehend$». Sólo cuenta el
término de una palabra: «$a warranty for$» (A2) enseña la preposición, y el 077 marcó warranty como error; lo mismo
«$an invoice for$», «$a knack for$». La teoría del tema, que también enseña palabras («Los más frecuentes: … tweezers»,
«concur with: coincidir con»), no la lee content-audit: vive en `ittm/content/theory` y el ejercicio la nombra con un
`theoryId` que el modelo auditable no trae; el parche que la suma está propuesto y en espera hasta después de la ronda 2
(`en-espera/`, con su TECH_SPEC). El sufijo: «Adverbios de modo» enseña «$-ly$» y saca sweetly, nicely, sadly, warmly y
beautifully en A1, aunque el EVP traiga las cuatro últimas en B1 y B2 (el orquestador, 1/10: el nivel propio gana a la
base, salvo cuando el tema enseña justamente a formar la palabra); «Doble consonante y e» enseña «$+ing$» y saca smoking;
«$-ed$ vs $-ing$» saca rewarding, C1 en el EVP. El metalenguaje: «modal verbs» en un ejercicio de B1, porque el curso
tiene temas «Modal Verbs».

</details>

<a id="F-NIVFR-R004"></a>
### Rule[F-NIVFR-R004] - La palabra que la traducción repite no es error
**Severity**: major | **Validation**: AUTO_VALIDATED

> Una palabra fuera del catálogo no cuenta cuando aparece igual, entera y sin tildes, en la [traducción](glossary:Traducción)
> del ejercicio: un préstamo o un nombre que el alumno lee en castellano. Un sustantivo tampoco cuenta cuando la
> traducción lo trae adaptado a la ortografía del castellano, como se escriben los préstamos: igual una vez plegados los
> dos (ph y f, th y t, y e i, ou y u, una consonante por la doble, el «es» de «espaguetis», sin vocales finales ni
> plural), o uno el comienzo del otro desde cinco letras («meteor», «meteoritos»), o con una letra de diferencia
> («porcelain», «porcelana»); siempre desde la misma primera letra. Sólo un sustantivo: un adjetivo o un adverbio que se
> parece al castellano es un cognado que el alumno igual tiene que saber («allergic», «ceramic», «diligently»), y un
> verbo nunca es préstamo, tampoco el que el etiquetador lee como sustantivo donde la oración lo usa como verbo: escrito
> con -ed o -ing sobre su lema, o después de un pronombre sujeto (I, he, she, we, they), del «to» del infinitivo o de
> una negación.

Saca 19 ejercicios: «We make sushi and pizza.», «on a USB», «Is Emily walking?», tango, polo, paella, Frodo. Pierde uno
del 077, «Do any of your relatives reside in Germany?», porque «reside» también es castellano. Un verbo no es préstamo:
«adore» y «adoran» siguen marcando, como pidió el 077, y «reside» y «residimos» también. La adaptación (1/10) saca
hamburger, limousine, pollen, spaghetti, triathlon, porcelain, autograph, dynamite, doctorate y binoculars, y deja
warranty («garantía» empieza con otra letra) y hydrated («hidratado» no es «hydrated» plegado), que el 077 marcó; desde
la segunda corrección del 1/10 vale sólo para sustantivos, y ceramic, allergic y diligently, que también sacaba,
vuelven a marcar. Ningún sustantivo del curso que saca es un verbo mal etiquetado; la guarda es para el que venga. Lo
que el catálogo trae como forma derivada de otro lema, dos niveles arriba o más, tampoco es préstamo: eso lo hace el
parche de la forma derivada del 1/10 ([R002](#F-NIVFR-R002)).

### Grupo B — Qué deja y qué no toca

<a id="F-NIVFR-R005"></a>
### Rule[F-NIVFR-R005] - Un hallazgo por palabra, contado como error
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Cada palabra de otro nivel deja en su ejercicio un hallazgo del contrato ([F-HALL-R001](#F-HALL-R001)) con gravedad
> *media*, resolución *mesa* y costo *instantáneo*. La evidencia da la oración como la ve el alumno, la palabra, su nivel o
> «fuera del EVP» con su puesto en COCA, y el nivel del ejercicio. Un ejercicio sin palabras de otro nivel queda con
> puntaje 1; uno con al menos una, con puntaje menor que 1 ([F-HALL-R004](#F-HALL-R004)).

«She spread the warm blue quilt on the bed. → spread y quilt son B2; el ejercicio es A2». «We reside in London with our
granddad. → reside está fuera del EVP (puesto 5.333); el ejercicio es A1». La gravedad la decidió José el 30/9. La
identidad nombra la palabra por lema y [categoría gramatical](glossary:Categoría gramatical), como `lemma-absence`
([F-HALL-R014](#F-HALL-R014)); se cuenta por tema, topic, nivel y curso ([F-HALL-R008](#F-HALL-R008)), CLOZE y opción
múltiple, sobre la oración que mide `lemma-absence`.

<a id="F-NIVFR-R006"></a>
### Rule[F-NIVFR-R006] - No mueve nada de lemma-absence ni del vocabulario
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con y sin este analizador, `lemma-absence` da los mismos puntajes, diagnósticos y tareas en cada nodo, el puntaje de
> vocabulario del curso sigue en 73,9 % y el plan en 4.036 tareas. El analizador no entra al promedio de vocabulario.

Mantiene [F-HALL-R007](#F-HALL-R007), [F-HALL-R009](#F-HALL-R009) y [F-HALL-R013](#F-HALL-R013). Los 170 del 30/9, y
los 99 que quedan con las lecturas y el nivel propio del 1/10, ya tienen tarea
LEMMA_ABSENCE: el plan abre una en cada uno de los 950 ejercicios con alguna marca de `lemma-absence`. Quién corrige
cuando los dos marcan la misma palabra: [DOUBT-TAREA-COMPARTIDA](#DOUBT-TAREA-COMPARTIDA), resuelta el 30/9: la tarea
LEMMA_ABSENCE que ya existe. El plan no abre tarea de `off-level-words` y declara sus ejercicios con error como cubiertos
por esa tarea, nunca como descartados ([F-HALL-R004](#F-HALL-R004), invariante 3).

<a id="F-NIVFR-R007"></a>
### Rule[F-NIVFR-R007] - Se describe en su ficha con un solo nombre
**Severity**: major | **Validation**: AUTO_VALIDATED

> `off-level-words` figura en `get analyzers` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Hay palabras que el
> alumno de este nivel no puede conocer?»; lee la oración terminada, el título, las respuestas y las miniteorías del tema,
> los nombres de los temas del curso y la traducción; familia errores, evalúa el ejercicio, meta 0, resolución mesa,
> costo instantáneo.

## Contexto

La propuesta «Corregir sin empeorar» pone las palabras de otro nivel entre los errores. `lemma-absence` las ve como
descuento de puntaje (0,1 por nivel); el 077 las marcó con otra vara, la de R002 a R004, que este analizador toma.

**El examen.** En `escribir/` (A1 y A2) encuentra 26 de 52: 7 de 17 en A1 y 19 de 35 en A2. Los 26 que se le escapan no
se ven por palabra: 9 son un sentido o una categoría que el EVP no distingue (sport, drum, bottle y journey como verbos,
«run the business», «some direction»); 7, una frase de nivel superior o la rebaja de R042 que la tapa («run out»,
«fail to», «wish to», remain, locate); 5, un nivel curado o una forma derivada (labor A2, concerned, packed); 4, el
tokenizador («implies'hope», singed → sing); y 1, la excepción de R004. Marca 25 que el 077 no tiene: a mi lectura, unos
9 son reales (disorganized, smelly, takeout, bedtime) y el resto cognados o internacionales (ceramic, spaghetti) o
adverbios del tema «Adverbios de modo» (nicely, sadly). Con las lecturas de R002 y R003 y R004 ampliadas (1/10) sigue en
26 de 52, con 34 marcas en la parte y 8 fuera del oro (chopstick, smile, snail, stew, smoothie, doughnut, disorganized,
brightly). Con el nivel propio primero y el sufijo del tema por encima (segunda corrección del 1/10) sigue en 26 de 52,
con 36 marcas en la parte y 10 fuera del oro: vuelven allergic y ceramic, cognados adjetivos que el 077 no marcó.

**Cómo se mide.** Se corre `analyze` pidiendo sólo `off-level-words` sobre el curso del 29/9; cada ejercicio con un
hallazgo suyo es una marca de la categoría «nivel» del examen, y `medir.py <resumen del informe> --parte escribir
--categoria nivel` da 26 de 52. Reproducido el 30/9 con un prototipo sobre el análisis 2026-09-30T11-54-02: 26 de 52, con
51 marcas dentro de la parte y 25 fuera del oro. La precisión se mide a mano, sobre una muestra al azar (semilla 87) de
30 marcas del curso fuera del oro de `escribir/` y fuera de las que ya revisó el verificador: el 1/10, 24 de 30 reales.
Con el nivel propio primero se revisaron a mano todas las demás marcas fuera del oro (43, las 4 que vuelven incluidas):
38 reales y 5 dudosas (smoothie, doughnut, lavender, cashier, skyscraper), ninguna falsa; con la muestra anterior, 62 de
73 reales (85 %). Las 4 que vuelven son reales.

**El lematizador compartido** (no se arregla en esta ronda): «hoped» queda como «hop», C1, y «partied» como «partie»;
desde el 1/10 la lectura absurda no dispara una marca (R002, lectura a.), pero el lema sigue mal en `lemma-absence` y en
`lexis`: el arreglo de fondo es de la ronda 2.

**Meta propuesta** en `examinar/` (B1 y B2, 73 casos): encontrar al menos 4 de cada 10, y que en la muestra de marcas
nuevas al menos 6 de cada 10 sean reales. **Costo**: segundos, en la pasada de `lemma-absence`, sin modelo.

## Alcance

- **Adentro**: R001 a R007, sobre la oración terminada de cada ejercicio.
- **Afuera**: el sentido de una palabra (juez de traducción y sentido, paquete C); buscar palabras de otro nivel en la
  pista, las opciones incorrectas y la miniteoría (la miniteoría sí se lee para saber qué enseña el tema, R003); el tipo
  de tarea (paquete E); cambiar `lemma-absence`, el catálogo o sus curaciones; la teoría del tema, que content-audit
  todavía no lee (parche propuesto el 1/10, en espera hasta después de la ronda 2).

## User Journeys

### Journey[F-NIVFR-J001] - Contar las palabras de otro nivel de un ejercicio
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-NIVFR-J001
    name: Contar las palabras de otro nivel de un ejercicio
    flow:
      - id: leer_palabra
        action: "El operador corre analyze con off-level-words, que lee cada palabra con el nivel de lemma-absence"
        gate: [F-NIVFR-R001, F-NIVFR-R003, F-NIVFR-R004]
        outcomes:
          - when: "La palabra queda dos niveles arriba o fuera del EVP, y ni el tema la enseña ni la traducción la repite"
            then: con_error
          - when: "La palabra queda en el nivel o uno arriba, la baja una frase, la enseña el tema o la repite la traducción"
            then: sin_error
      - id: con_error
        action: "El ejercicio queda con un hallazgo media por palabra, puntaje menor que 1, y suma una vez en los errores de su tema, nivel y curso"
        gate: [F-NIVFR-R002, F-NIVFR-R005]
        then: intacto
      - id: sin_error
        action: "El ejercicio queda con puntaje 1 y sin hallazgo de este analizador"
        then: intacto
      - id: intacto
        action: "lemma-absence, el puntaje de vocabulario de 73,9 % y las 4.036 tareas del plan quedan iguales"
        gate: [F-NIVFR-R006]
        result: success
```

## Open Questions

<a id="DOUBT-UMBRAL-DE-ERROR"></a>
### Doubt[DOUBT-UMBRAL-DE-ERROR] - ¿Desde qué distancia una palabra es un error?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): dos niveles arriba o fuera del EVP en la banda fuerte: 170 ejercicios, 26 de 52 del examen.
- [ ] B: un nivel arriba ya es error: 797 ejercicios (318 en A1, casi todos palabras A2 de la fusión), 33 de 52.
- [ ] C: sólo fuera del EVP: 156 ejercicios, 20 de 52; pierde switch, bench, brick, spread, bounce y shed.

**Answer**: **A** (José, 30/9): «es error desde dos niveles de distancia». B ganaba 7 casos a cambio de 627 ejercicios
más en «algún error». [F-NIVFR-R002](#F-NIVFR-R002) queda validada.

<a id="DOUBT-FRASES-SUPERIORES"></a>
### Doubt[DOUBT-FRASES-SUPERIORES] - ¿Marca también frases del EVP de nivel superior, como «run out» (B1) en A1?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no por ahora; [F-LABS-R005](#F-LABS-R005) ya dice que una frase no sube una palabra.
- [ ] B: sólo verbos frasales sin objeto después de la partícula, dos niveles arriba, como error.
- [ ] C: lo mismo que B, pero sólo ordena.

**Answer**: **A** (José, 30/9): «no se marcan por ahora». B marcaba 23 ejercicios del curso, cerca de la mitad falsos
(«take videos in», «have got to», «Let's go out»), y encontraba 1 de los 52.

<a id="DOUBT-REBAJA-EN-CONTEXTO"></a>
### Doubt[DOUBT-REBAJA-EN-CONTEXTO] - ¿La frase que baja una palabra vale siempre, o sólo si está en la oración?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): siempre, como [F-LABS-R042](#F-LABS-R042), igual que `lexis` y el eval #7.
- [ ] B: sólo si está: «wish to come» deja de heredar el A2 de «best wishes».

**Answer**: **A**, la recomendada (José, 30/9: «por lo demás, continúa»). La paridad con `lexis` se mantiene; B sumaba
34 palabras y 1 caso del examen, con falsos como «Where do you live?» (live B1).

<a id="DOUBT-TAREA-COMPARTIDA"></a>
### Doubt[DOUBT-TAREA-COMPARTIDA] - Cuando los dos marcan la misma palabra, ¿quién abre la tarea?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): la tarea LEMMA_ABSENCE que ya existe; el hallazgo se declara cubierto por ella.
- [ ] B: cada analizador su tarea, y la corrección las junta por ejercicio y palabra.
- [ ] C: este analizador sólo cuenta y no pide tarea.

**Answer**: **A**, la recomendada (José, 30/9: «por lo demás, continúa»). El plan sigue en 4.036 tareas; el paquete E
acepta «cubierto por otra tarea» junto a las dos salidas de [F-HALL-R004](#F-HALL-R004) ([F-NIVFR-R006](#F-NIVFR-R006)).

<a id="DOUBT-LECTURAS-NUEVAS"></a>
### Doubt[DOUBT-LECTURAS-NUEVAS] - ¿Suma lecturas que lemma-absence no hace, como «sport» usado como verbo?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no; el sentido lo mira el juez de traducción y sentido (paquete C).
- [ ] B: marcar, como sólo ordena, el verbo de un lema que el EVP trae sólo como sustantivo.
- [ ] C: buscar además por la forma de la palabra cuando su lema no está (concerned, B2).

**Answer**: **A**, la recomendada (José, 30/9: «por lo demás, continúa»). B aparecía en 214 ejercicios, casi todos por
el etiquetado (rain, «hates» → hat, practise); C rescataba 127 palabras con falsos como «living-room».

## References

- **FEAT-LABS** — Nivel de cada palabra, frases que bajan y bandas fuera del catálogo: R001, R002, R004.
- **FEAT-CLEX** — Paridad con `lexis`: R001.
- **FEAT-HALL** — Hallazgo, gravedad, familia, números, ficha y lo que no cambia: R005, R006, R007.
- **FEAT-SEMAF** — La misma excepción de lo que enseña el tema: R003.
