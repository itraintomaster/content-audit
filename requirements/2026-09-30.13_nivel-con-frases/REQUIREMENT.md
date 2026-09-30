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
**Severity**: critical | **Validation**: ASSUMPTION

> Una palabra de contenido es de otro nivel cuando: 1. su nivel del catálogo está dos o más niveles arriba del ejercicio
> (C1 = 5 y C2 = 6, como [F-LABS-R033](#F-LABS-R033)); o 2. no está en el catálogo y cae en la banda de descuento fuerte
> de [F-LABS-R034](#F-LABS-R034) para ese nivel. Una palabra un solo nivel arriba nunca es de otro nivel para este analizador.

<details><summary>Detalle: por qué el umbral, con datos</summary>

De los 337 de A1, 329 sólo tienen palabras un nivel arriba, y 281 son ejercicios escritos para A2 que la fusión del 26/9
pasó a A1 (el 14/8, A1 tenía 56 marcados de 1.868). Las más marcadas son útiles de A2: let 20, arrive 9, few 9, slowly 8.

| | A1 | A2 | B1 | B2 | Curso |
|---|---:|---:|---:|---:|---:|
| `lemma-absence`, palabra de otro nivel | 337 | 185 | 225 | 1 | 748 |
| El 077, nivel | 17 | 35 | 50 | 23 | 125 |
| `off-level-words`, con R003 y R004 | 18 | 33 | 81 | 38 | 170 |

En A1 marca 11 palabras fuera del EVP y 9 de B1 o B2; en B1 y B2, casi todo es fuera del EVP. Umbral:
[DOUBT-UMBRAL-DE-ERROR](#DOUBT-UMBRAL-DE-ERROR).

</details>

<a id="F-NIVFR-R003"></a>
### Rule[F-NIVFR-R003] - Lo que el tema enseña no es error
**Severity**: major | **Validation**: ASSUMPTION

> Una palabra no cuenta en un ejercicio cuando su tema la enseña: 1. figura entre los términos en inglés del título del
> tema, los que van entre `$`; o 2. es parte de la respuesta de al menos 3 ejercicios del tema y del 20 % o más de ellos.
> Sale de los datos del curso, nunca de una lista escrita a mano.

Saca 9 ejercicios de A2 («$Invest time$ + gerundio»: invest es B2) y 28 de B1 («Orden o simultaneidad», con sequence y
simultaneous como respuesta; «¿Con coma o sin coma?», con non-defining), y ninguno del 077. El semáforo usa la misma
excepción ([F-SEMAF-R004](#F-SEMAF-R004)).

<a id="F-NIVFR-R004"></a>
### Rule[F-NIVFR-R004] - La palabra que la traducción repite no es error
**Severity**: major | **Validation**: ASSUMPTION

> Una palabra fuera del catálogo no cuenta cuando aparece igual, entera y sin tildes, en la [traducción](glossary:Traducción)
> del ejercicio: un préstamo o un nombre que el alumno lee en castellano.

Saca 19 ejercicios: «We make sushi and pizza.», «on a USB», «Is Emily walking?», tango, polo, paella, Frodo. Pierde uno
del 077, «Do any of your relatives reside in Germany?», porque «reside» también es castellano. No es parecido de
cognados: «adore» y «adoran» siguen marcando, como pidió el 077.

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

Mantiene [F-HALL-R007](#F-HALL-R007), [F-HALL-R009](#F-HALL-R009) y [F-HALL-R013](#F-HALL-R013). Los 170 ya tienen tarea
LEMMA_ABSENCE: el plan abre una en cada uno de los 950 ejercicios con alguna marca de `lemma-absence`. Quién corrige
cuando los dos marcan la misma palabra: [DOUBT-TAREA-COMPARTIDA](#DOUBT-TAREA-COMPARTIDA).

<a id="F-NIVFR-R007"></a>
### Rule[F-NIVFR-R007] - Se describe en su ficha con un solo nombre
**Severity**: major | **Validation**: AUTO_VALIDATED

> `off-level-words` figura en `get analyzers` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Hay palabras que el
> alumno de este nivel no puede conocer?»; lee la oración terminada, el título y las respuestas del tema y la traducción;
> familia errores, evalúa el ejercicio, meta 0, resolución mesa, costo instantáneo.

## Contexto

La propuesta «Corregir sin empeorar» pone las palabras de otro nivel entre los errores. `lemma-absence` las ve como
descuento de puntaje (0,1 por nivel); el 077 las marcó con otra vara, la de R002 a R004, que este analizador toma.

**El examen.** En `escribir/` (A1 y A2) encuentra 26 de 52: 7 de 17 en A1 y 19 de 35 en A2. Los 26 que se le escapan no
se ven por palabra: 9 son un sentido o una categoría que el EVP no distingue (sport, drum, bottle y journey como verbos,
«run the business», «some direction»); 7, una frase de nivel superior o la rebaja de R042 que la tapa («run out»,
«fail to», «wish to», remain, locate); 5, un nivel curado o una forma derivada (labor A2, concerned, packed); 4, el
tokenizador («implies'hope», singed → sing); y 1, la excepción de R004. Marca 25 que el 077 no tiene: a mi lectura, unos
9 son reales (disorganized, smelly, takeout, bedtime) y el resto cognados o internacionales (ceramic, spaghetti) o
adverbios del tema «Adverbios de modo» (nicely, sadly).

**Meta propuesta** en `examinar/` (B1 y B2, 73 casos): encontrar al menos 4 de cada 10, y que en la muestra de marcas
nuevas al menos 6 de cada 10 sean reales. **Costo**: segundos, en la pasada de `lemma-absence`, sin modelo.

## Alcance

- **Adentro**: R001 a R007, sobre la oración terminada de cada ejercicio.
- **Afuera**: el sentido de una palabra (juez de traducción y sentido, paquete C); la pista, las opciones incorrectas y
  la miniteoría; el tipo de tarea (paquete E); cambiar `lemma-absence`, el catálogo o sus curaciones.

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
- [ ] A (recomendada): dos niveles arriba o fuera del EVP en la banda fuerte: 170 ejercicios, 26 de 52 del examen.
- [ ] B: un nivel arriba ya es error: 797 ejercicios (318 en A1, casi todos palabras A2 de la fusión), 33 de 52.
- [ ] C: sólo fuera del EVP: 156 ejercicios, 20 de 52; pierde switch, bench, brick, spread, bounce y shed.

**Answer**: Pendiente. Recomiendo A, la vara del 077: B gana 7 casos a cambio de 627 ejercicios más en «algún error».

<a id="DOUBT-FRASES-SUPERIORES"></a>
### Doubt[DOUBT-FRASES-SUPERIORES] - ¿Marca también frases del EVP de nivel superior, como «run out» (B1) en A1?
- [ ] A (recomendada): no por ahora; [F-LABS-R005](#F-LABS-R005) ya dice que una frase no sube una palabra.
- [ ] B: sólo verbos frasales sin objeto después de la partícula, dos niveles arriba, como error.
- [ ] C: lo mismo que B, pero sólo ordena.

**Answer**: Pendiente. Recomiendo A. B marca 23 ejercicios del curso, cerca de la mitad falsos («take videos in», «have
got to», «Let's go out»), y encuentra 1 de los 52. Sin esa restricción salen 1.654 en A1.

<a id="DOUBT-REBAJA-EN-CONTEXTO"></a>
### Doubt[DOUBT-REBAJA-EN-CONTEXTO] - ¿La frase que baja una palabra vale siempre, o sólo si está en la oración?
- [ ] A (recomendada): siempre, como [F-LABS-R042](#F-LABS-R042), igual que `lexis` y el eval #7.
- [ ] B: sólo si está: «wish to come» deja de heredar el A2 de «best wishes».

**Answer**: Pendiente. Recomiendo A. Hoy, en el 65 % de las 381 palabras que baja R042 la frase está en la oración. B
suma 34 palabras y 1 caso del examen, pero da falsos como «Where do you live?» (live B1) y rompe la paridad con `lexis`.

<a id="DOUBT-TAREA-COMPARTIDA"></a>
### Doubt[DOUBT-TAREA-COMPARTIDA] - Cuando los dos marcan la misma palabra, ¿quién abre la tarea?
- [ ] A (recomendada): la tarea LEMMA_ABSENCE que ya existe; el hallazgo se declara cubierto por ella.
- [ ] B: cada analizador su tarea, y la corrección las junta por ejercicio y palabra.
- [ ] C: este analizador sólo cuenta y no pide tarea.

**Answer**: Pendiente. Recomiendo A: el plan no cambia. El paquete E tiene que aceptar «cubierto por otra tarea» junto a
las dos salidas de [F-HALL-R004](#F-HALL-R004).

<a id="DOUBT-LECTURAS-NUEVAS"></a>
### Doubt[DOUBT-LECTURAS-NUEVAS] - ¿Suma lecturas que lemma-absence no hace, como «sport» usado como verbo?
- [ ] A (recomendada): no; el sentido lo mira el juez de traducción y sentido (paquete C).
- [ ] B: marcar, como sólo ordena, el verbo de un lema que el EVP trae sólo como sustantivo.
- [ ] C: buscar además por la forma de la palabra cuando su lema no está (concerned, B2).

**Answer**: Pendiente. Recomiendo A. B aparece en 214 ejercicios, casi todos por el etiquetado (rain, «hates» → hat,
practise), aunque suma 5 casos; C rescata 127 palabras, suma 1 caso y da falsos como «living-room» (living B2).

## References

- **FEAT-LABS** — Nivel de cada palabra, frases que bajan y bandas fuera del catálogo: R001, R002, R004.
- **FEAT-CLEX** — Paridad con `lexis`: R001.
- **FEAT-HALL** — Hallazgo, gravedad, familia, números, ficha y lo que no cambia: R005, R006, R007.
- **FEAT-SEMAF** — La misma excepción de lo que enseña el tema: R003.
