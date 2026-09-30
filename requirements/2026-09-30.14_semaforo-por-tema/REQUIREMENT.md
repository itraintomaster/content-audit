---
feature:
  id: FEAT-SEMAF
  code: F-SEMAF
  name: Semáforo de palabras por tema
  priority: high
---

# Semáforo de palabras por tema

## TL;DR

**Qué**: Una consulta por tema que devuelve sus palabras en verde (el nivel las espera y el curso casi no las usa) y en
rojo (están por encima del nivel), más las palabras verdes para sumar, salvo las que el propio tema enseña.

**Por qué**: Cuando una corrección tiene que cambiar una palabra, hoy no sabe cuál elegir: «wears» o una más rara. El
curso necesita repetir, no evitar: de los 1.750 lemas que vuelven al menos una vez, sólo 134 lo hacen a un ritmo sano.

## Recorrido de uso (estado actual)

Cómo el que propone un arreglo intenta hoy saber qué palabras usar y cuáles evitar en un tema:

1. Pide palabras sugeridas → `GetCommand.get("suggested-lemmas", taskId, SuggestedLemmasFilter)`. ✓ Disponible
   ([F-CSLATDC-R001](#F-CSLATDC-R001)), ordenadas en seis grupos ([F-SLEM-R003](#F-SLEM-R003)).
2. Revisa una oración → `LexisCommand.queryLexis(quizSentence, mode, level)`. ✓ Disponible ([F-CLEX-R005](#F-CLEX-R005)).
3. Mira el tema entero. ⚠️ Las sugeridas piden una tarea LEMMA_ABSENCE, y 950 ejercicios de 11.287 la tienen; `lexis`
   va de a una oración, unos 4 s cada una; y ninguna de las dos sabe qué palabra enseña el tema.

**Punto de fricción**: paso 3. No hay una vista del tema con sus palabras, su color y por qué.

**Cambio mínimo necesario**: una consulta de sólo lectura por tema que junta las dos respuestas que ya existen, con el
nivel del tema y la excepción de lo que el tema enseña. No calcula puntajes nuevos ni cambia ninguno.

## Reglas de Negocio

Grupos: **A**, qué pide y qué devuelve (R001–R003); **B**, los colores (R004–R007). Acá **tema** es el *knowledge*.
Base: análisis 2026-09-30T11-54-02 (curso del 29/9); N es el umbral de `lemma-count`, hoy 4.

### Grupo A — Qué pide y qué devuelve

<a id="F-SEMAF-R001"></a>
### Rule[F-SEMAF-R001] - Una consulta por tema, de sólo lectura
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Dado un tema del curso, `GetCommand.get` devuelve su semáforo: 1. cada palabra de contenido de sus ejercicios, una vez,
> con su [lema](glossary:Lema), su [categoría](glossary:Categoría gramatical), su nivel o «fuera del EVP», en cuántas
> oraciones del curso aparece y su color; 2. las palabras verdes para sumar ([F-SEMAF-R003](#F-SEMAF-R003)). El nivel es el
> del tema. No escribe nada ni calcula puntajes; un tema que no existe rechaza la consulta.

<details><summary>Detalle</summary>

El recurso y sus parámetros los decide la arquitectura. Lee el mismo análisis fuente que las sugeridas
([F-CSLATDC-R015](#F-CSLATDC-R015)). Ejemplo: A2 «Posición del adjetivo», 157 palabras: 42 rojas (bench, brick, spread,
quilt, shed, y B1 como metal y stone) y 27 verdes propias (lamp, leather y pillow en 4 oraciones; rubber y silver en 3).

El nivel que se muestra de cada palabra es el de la consulta léxica cuando la marca (su nivel, o «fuera del EVP» con su
puesto) y, si no, el que le asigna `lemma-count` ([F-SEMAF-R002](#F-SEMAF-R002)); una palabra que `lemma-count` no puede
ubicar sale «sin nivel». Las oraciones son las que cuenta `lemma-count`, así que el análisis tiene que incluirlo.

**Error**: "Knowledge '<id>' not found in the analysis."

**Error** (análisis sin `lemma-count`): "The analysis '<id>' has no lemma-count data. Run 'content-audit analyze' with lemma-count."

</details>

<a id="F-SEMAF-R002"></a>
### Rule[F-SEMAF-R002] - Verde propio: la palabra del tema que conviene que vuelva
**Severity**: critical | **Validation**: ASSUMPTION

> Una palabra del tema es verde cuando `lemma-count` le asigna el nivel del tema y el curso la usa en menos de 2N
> oraciones, con N el umbral de la palabra en `lemma-count`, el de su banda ([F-POCOV-R001](#F-POCOV-R001)). Lleva su
> estado: *poco vista* (1 a N−1) o *escasa* (N a 2N−1), los mismos cortes de [F-SLEM-R014](#F-SLEM-R014). Ninguna
> palabra roja es verde, y toda palabra *poco vista* verde es un hallazgo de `lemma-count` en su nivel ([F-POCOV-R003](#F-POCOV-R003)).

<details><summary>Detalle</summary>

Por nivel, con 4 para todas (hoy): A1 134 palabras verdes en 57 de 83 temas, A2 633 en 88 de 91, B1 1.012 en 137 de 147
y B2 307 en 69 de 92. Con las bandas 8 · 6 · 4 · 3: A1 242, A2 771, B1 1.080 y B2 307. En A1 hay pocas porque sus palabras
se repiten: el tema de las verduras, «Afirmativas regulares», no tiene ninguna (vegetable aparece en 10 oraciones, y con
su banda, la de hasta 3.000, sigue pidiendo 4). Contar también la recurrencia:
[DOUBT-VERDE-POR-RECURRENCIA](#DOUBT-VERDE-POR-RECURRENCIA).

**Por qué el nivel de `lemma-count` y no el de `lemma-absence`** (arquitecto, 30/9; [ASSUMPTION] que José puede vetar):
es el nivel con que se cuentan las oraciones de la palabra, así que el verde dice lo mismo que el hallazgo de palabras
poco vistas. Con el de `lemma-absence` eran 154 · 653 · 1.039 · 316: los 70 pares palabra-tema de diferencia son
categorías mal etiquetadas o que el EVP no trae, como «pizza» y «orange» como verbos, «our» y «write» como sustantivos o
«drum» como verbo, que el 077 marcó como error.

</details>

<a id="F-SEMAF-R003"></a>
### Rule[F-SEMAF-R003] - Verde para sumar: las sugeridas del nivel del tema
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Las palabras para sumar son las [palabras sugeridas](glossary:Palabra sugerida) del nivel del tema, con el orden de seis
> grupos y el desempate COCA de [F-SLEM-R003](#F-SLEM-R003) a [F-SLEM-R005](#F-SLEM-R005), y con los filtros de
> categoría, límite, patrón e inclusión de expuestas de [F-CSLATDC-R007](#F-CSLATDC-R007), [F-CSLATDC-R008](#F-CSLATDC-R008),
> [F-INCEXP-R002](#F-INCEXP-R002) y [F-SLREGEX-R002](#F-SLREGEX-R002). Pedida con los mismos filtros, la lista coincide con
> la de `get suggested-lemmas` para una tarea de ese nivel, sin las palabras que el tema ya usa.

<details><summary>Detalle</summary>

Sin incluir expuestas, salen de los lemas ausentes y de los poco vistos del nivel: A1 24 y 62 (January, Saturday; nose,
husband), A2 92 y 418, B1 594 y 688, B2 1.574 y 366. «wear» (A1, 42 oraciones) no es verde, pero tampoco roja: por eso
«She wears hats» puede reemplazar a «She sports hats». El límite se aplica después de sacar las palabras que el tema ya
usa, así que pedir 10 devuelve 10 mientras alcancen.

</details>

### Grupo B — Los colores

<a id="F-SEMAF-R004"></a>
### Rule[F-SEMAF-R004] - Lo que el tema enseña nunca es rojo
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una palabra que el tema enseña no es roja: 1. figura entre los términos en inglés del título del tema, los que van
> entre `$` (let's cuenta como let); o 2. es parte de la respuesta de al menos 3 ejercicios del tema y del 20 % o más de
> ellos. Se muestra como «la enseña el tema». Es la excepción de [F-NIVFR-R003](#F-NIVFR-R003).

«$Invest time$ + gerundio» (A2) enseña invest, que es B2; «Orden o simultaneidad» (B1) tiene sequence y simultaneous como
respuesta. Sale del curso, nunca de una lista escrita a mano ([F-LABS-R035](#F-LABS-R035)).

<a id="F-SEMAF-R005"></a>
### Rule[F-SEMAF-R005] - Rojo: lo que `lexis` marca para el nivel del tema
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una palabra del tema es roja cuando `lexis` la marca para el nivel del tema en alguno de sus ejercicios, mal ubicada o
> fuera del catálogo con descuento, salvo [F-SEMAF-R004](#F-SEMAF-R004). Lleva su nivel o «fuera del EVP», y si además es
> un error de `off-level-words` ([F-NIVFR-R002](#F-NIVFR-R002)).

<details><summary>Detalle</summary>

Mantiene [F-CLEX-R006](#F-CLEX-R006) y el eval #7 de [F-LASAG-R007](#F-LASAG-R007), que ya impide sumar una roja. Hay
951 palabras rojas en 183 temas (A1 359 en 37, A2 231 en 27, B1 293 en 82, B2 68 en 37); 168 son además error (19 en
A1). A1 tiene tantas por la fusión del 26/9: «Verbos de movimiento» trae arrive, airport y tourist, que son A2, junto a
reside y disembark, que están fuera del EVP. El tema enseña 8 de las marcadas. Que una roja sea además error se lee del
hallazgo de `off-level-words` en ese ejercicio; si el análisis no corrió ese analizador, la marca sale «sin dato» y el
color no cambia. El lematizador compartido pone 6 de las rojas: «drunk», «swam» y «hop» (de «hoped»).

</details>

<a id="F-SEMAF-R006"></a>
### Rule[F-SEMAF-R006] - Una palabra nunca es roja por aparecer mucho
**Severity**: major | **Validation**: VALIDATED

> Ninguna palabra es roja por la cantidad de oraciones en que aparece ni por lo seguido que vuelve: el rojo es sólo
> [F-SEMAF-R005](#F-SEMAF-R005). Una palabra del nivel que el curso repite mucho es neutral.

440 lemas aparecen en más de 15 oraciones, y 290 son A1 (go, have, eat). Los 46 que vuelven cada menos de 50 palabras
son sobre todo palabras que su tema enseña (define 24 veces, uncountable 20, however 16) o dos apariciones seguidas
(oven, blood). Ver [DOUBT-SOBREEXPOSICION](#DOUBT-SOBREEXPOSICION).

<a id="F-SEMAF-R007"></a>
### Rule[F-SEMAF-R007] - El verde se ofrece; el rojo sólo avisa
**Severity**: critical | **Validation**: AUTO_VALIDATED

> 1. Sólo las palabras verdes se ofrecen como candidatas: ninguna roja aparece en la lista para sumar. 2. El semáforo
> no crea tareas, hallazgos ni puntajes: una palabra roja que no es error no pide cambiar su ejercicio. 3. Con y sin la
> consulta, el análisis, el 73,9 % y las 4.036 tareas del plan son idénticos.

El cambio mínimo sigue mandando: no se suman palabras por sumar (brief 085). Mantiene [F-HALL-R013](#F-HALL-R013) y
[F-HALL-R015](#F-HALL-R015). Si va o no al catálogo de analizadores: [DOUBT-CONSULTA-O-ANALIZADOR](#DOUBT-CONSULTA-O-ANALIZADOR).

## Contexto

El brief 085 propuso un semáforo por palabra y por nivel, y la propuesta lo dibuja al costado del tema («Palabras del
tema»). El verde ya existe como palabras sugeridas y el rojo como `lexis`; lo que falta es verlos juntos por tema, con la
excepción del tema, y que la corrección elija de ahí. En el curso falta repetir: `lemma-count` cuenta 1.535 lemas en
menos de 4 oraciones, y `lemma-recurrence` da 8 %. Por eso el verde manda y el rojo se limita a lo que está arriba del
nivel.

**El examen.** No tiene casos propios en el 077. Se prueba con el paquete E: en la muestra ciega, qué parte de las
palabras que la corrección sumó eran verdes y cuántas rojas entraron. **Meta propuesta**: ninguna roja nueva y la
mayoría de las palabras sumadas, verdes. **Costo**: segundos, sin modelo.

## Alcance

- **Adentro**: R001 a R007, para un tema del curso analizado.
- **Afuera**: cómo la corrección usa el semáforo y el efecto neto de cada tanda (paquetes E y F); dibujarlo (G); los
  umbrales de `lemma-count` ([F-POCOV-R001](#F-POCOV-R001)); el sentido de una palabra, como «sport» usado como verbo.

## User Journeys

### Journey[F-SEMAF-J001] - Pedir el semáforo de un tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-SEMAF-J001
    name: Pedir el semáforo de un tema
    flow:
      - id: pedir
        action: "El que propone pide el semáforo de un tema del análisis, con una categoría y un límite"
        gate: [F-SEMAF-R001]
        outcomes:
          - when: "El tema existe en el análisis"
            then: colorear
          - when: "El tema no existe"
            then: rechazo
      - id: colorear
        action: "Cada palabra del tema sale con su nivel, sus oraciones y su color; lo que el tema enseña nunca es rojo"
        gate: [F-SEMAF-R002, F-SEMAF-R004, F-SEMAF-R005, F-SEMAF-R006]
        then: sumar
      - id: sumar
        action: "La lista para sumar trae las sugeridas del nivel en su orden, sin rojas, y el análisis y el plan quedan iguales"
        gate: [F-SEMAF-R003, F-SEMAF-R007]
        result: success
      - id: rechazo
        action: "La consulta se rechaza con el mensaje de tema inexistente y no devuelve ninguna lista"
        gate: [F-SEMAF-R001]
        result: failure
```

## Open Questions

<a id="DOUBT-SOBREEXPOSICION"></a>
### Doubt[DOUBT-SOBREEXPOSICION] - ¿El rojo incluye la palabra que aparece demasiado?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): no; el rojo es sólo lo que está arriba del nivel (R006).
- [ ] B: sí, con los umbrales por banda que proponga [DOUBT-UMBRALES-POR-BANDA](#DOUBT-UMBRALES-POR-BANDA).
- [ ] C: sí, con los del brief 085: más de 15 oraciones o menos de 50 palabras entre una y otra.

**Answer**: **A** (José, 30/9): «una palabra muy repetida no se pinta de rojo». C pintaba de rojo 440 lemas, casi todos
la base de A1. [F-SEMAF-R006](#F-SEMAF-R006) queda validada.

<a id="DOUBT-VERDE-POR-RECURRENCIA"></a>
### Doubt[DOUBT-VERDE-POR-RECURRENCIA] - ¿El verde propio cuenta también la palabra que vuelve poco, aunque esté en 8 o más oraciones?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): todavía no; primero la recurrencia por nivel ([F-RECUR-R004](#F-RECUR-R004)).
- [ ] B: sí, como el brief 085: más de 1.000 palabras entre una y otra.

**Answer**: **A**, la recomendada (José, 30/9: «por lo demás, continúa»). Con B, en A1 era verde la mitad de todo (1.878
de 3.717 palabras propias) y el verde dejaba de orientar.

<a id="DOUBT-CONSULTA-O-ANALIZADOR"></a>
### Doubt[DOUBT-CONSULTA-O-ANALIZADOR] - ¿El semáforo es una consulta o un analizador del catálogo?
**Status**: RESOLVED (2026-09-30)

- [x] A (recomendada): una consulta, como `lexis` y las sugeridas: sin ficha, puntaje ni hallazgos.
- [ ] B: un analizador que sólo ordena, con un hallazgo por palabra roja en cada tema.

**Answer**: **A** (José, 30/9): «es una consulta, no un analizador». No tiene meta, y sus hallazgos repetirían los de
`lemma-absence` y `off-level-words` ([F-SEMAF-R007](#F-SEMAF-R007)).

## References

- **FEAT-SLEM, FEAT-CSLATDC, FEAT-INCEXP, FEAT-SLREGEX** — El verde: orden, filtros y análisis fuente: R001, R002, R003.
- **FEAT-CLEX, FEAT-LASAG** — El rojo y el eval #7: R005.
- **FEAT-NIVFR** — La excepción del tema y qué rojas son error: R004, R005.
- **FEAT-HALL, FEAT-LABS** — Nada cambia de número; nunca listas a mano: R004, R007.
- **FEAT-POCOV** — El nivel, las oraciones y el umbral de cada palabra, el de su banda: R001, R002.
- **FEAT-RECUR** — La recurrencia por nivel, que el verde todavía no usa: dudas.
