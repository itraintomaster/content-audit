---
feature:
  id: FEAT-RESPC
  code: F-RESPC
  name: Respuestas completas - el corrector acepta todas las formas de la respuesta y ninguna consigna pide una
  priority: high
---

# Respuestas completas: el corrector acepta todas las formas de la respuesta, y ninguna consigna pide una

## TL;DR

**Qué**: Un analizador de la familia de errores arma, para cada respuesta aceptada, sus formas equivalentes —contraída
y larga, las dos negaciones cortas, las dos grafías, until y till—, marca las que el corrector rechazaría y marca las
consignas y miniteorías que piden una forma.

**Por qué**: El 077 encontró 484 ejercicios donde una respuesta correcta da mal, y José decidió el 30/9 que el
corrector acepte siempre las dos formas y que ninguna consigna pida una; hoy nadie lo mide.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber si el corrector rechaza respuestas buenas:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible, pero sólo mide la primera
   respuesta de cada ejercicio ([F-QSENT-R018](#F-QSENT-R018)).
2. Mira qué acepta el corrector → ⚠️ content-audit no sabe cómo compara el corrector del backend: «They ___ (not / be)
   from Ireland.» acepta «'re not» y rechaza «aren't», la que enseña su propia miniteoría, y ningún número lo muestra.
3. Busca las consignas que piden una forma → ⚠️ nada las cuenta: 694 ejercicios dicen «Usa $short forms$ si es
   posible.» o «Usa $long forms$.».
4. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 a 4.

**Cambio mínimo necesario**: sumar `answer-completeness` al catálogo de FEAT-HALL, con la comparación del corrector
—y su tabla de formas equivalentes— escrita como regla de content-audit, y la regla de que ninguna consigna pide una
forma.

## Reglas de Negocio

Aplica a los ejercicios de hueco para escribir (8.477). En opción múltiple el alumno no escribe: allí rige FEAT-OMVAL.
Todas las respuestas aceptadas son formas de la misma respuesta ([F-QSENT-R001](#F-QSENT-R001)). La **tabla de formas
equivalentes** es la del corrector del backend: content-audit guarda una copia de la que está en producción.

<a id="F-RESPC-R001"></a>
### Rule[F-RESPC-R001] - Una forma está aceptada si el corrector del backend la acepta
**Severity**: critical | **Validation**: VALIDATED

> Una forma escrita en el hueco está aceptada si es igual a alguna respuesta aceptada después de: separar cada
> respuesta en «|» (la «/» no separa), pasar a minúsculas y sacar los espacios, los signos de exclamación,
> pregunta, dos puntos, punto y coma, coma, punto, guion y guion bajo, los apóstrofos, las comillas y las tildes; y
> de tomar como iguales las formas que iguala la tabla de formas equivalentes. Invariantes: «cafe» está aceptada si
> lo está «café»; «every day» y «everyday» son iguales; con la tabla vacía, «aren't», «are not» y «'re not» son tres
> formas distintas; con la del corrector nuevo, son la misma.

Confirmado leyendo `be/domain/quiz/pkg/shared/cloze_sentence_part.go` (`Evaluate` y `normalize`) en ittm: coincide con
la propuesta («parte en «|», pasa a minúsculas y saca signos»), y además saca espacios y tildes. Las tildes se sacan
desde el 19/5/2025 (#884); `escribir/respuestas.json` dice lo contrario de `67fab6d599301022953422c5` («Don't run in
the café.»), y el corrector acepta «cafe». La tabla la construye la rama `claude/corrector-formas-equivalentes` de
ittm, con sus casos de oro en un archivo que comparten los dos lados ([DOUBT-RESPC-IMITAR](#DOUBT-RESPC-IMITAR)). La
copia de content-audit es la del corrector que está en producción: se actualiza cuando el corrector sale.

<a id="F-RESPC-R002"></a>
### Rule[F-RESPC-R002] - Están la contraída y la larga, pida lo que pida la consigna
**Severity**: critical | **Validation**: VALIDATED

> Para cada respuesta aceptada están aceptadas también su forma contraída y su forma larga: la negación (*don't* y *do
> not*, *can't* y *cannot*, *won't* y *will not*) y el auxiliar pegado al sujeto (*I'm* y *I am*, *there's* y *there
> is*, *I'd like* y *I would like*), también cuando el sujeto queda fuera del hueco («'m not» y «am not») o es un nombre
> («Peter's cooking» y «Peter is cooking»). Vale siempre, pida lo que pida la consigna.

Ejemplos: «I ___ (not / eat) meat often.» acepta «don't eat» y falta «do not eat» (`67fab6d5993010229534263c`, A2);
«A house is near the school. ___» acepta «There is a house near the school.» y falta «There's…»
(`67fab6d59930102295342407`, A1). En una pregunta negativa la larga pone el *not* después del sujeto: «Doesn't she
dance?» y «Does she not dance?» (142 ejercicios de B1 y B2, contando las *question tags*). Lo decidió José el 30/9
([DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA)): no hay excepción por consigna.

<a id="F-RESPC-R003"></a>
### Rule[F-RESPC-R003] - Las dos negaciones cortas de *be* están aceptadas
**Severity**: critical | **Validation**: VALIDATED

> Si una respuesta usa *isn't* o *aren't*, la otra forma corta (*'s not*, *'re not*) también está aceptada, y al revés,
> pida lo que pida la consigna: las dos son cortas.

«They ___ (not / be) from Ireland.» acepta «'re not» y falta «aren't», que es la que enseña la miniteoría del ejercicio
(`67fab6d599301022953425ac`, A1). También lo decide [DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA).

<a id="F-RESPC-R004"></a>
### Rule[F-RESPC-R004] - Están la grafía británica y la americana, y *until* y *till*
**Severity**: major | **Validation**: VALIDATED

> Si una respuesta lleva una palabra de la lista cerrada de grafías de la tabla del corrector (*travelled* y
> *traveled*, *learnt* y *learned*, *dreamt* y *dreamed*, *colour* y *color*, *centre* y *center*, *realise* y
> *realize*, *grey* y *gray*, *has got* y *has gotten*, y las de su familia), la otra grafía también está aceptada. Lo
> mismo con *until* y *till*.

«She ___ (travel) to Europe.» acepta «travelled» y falta «traveled» (`67fab6d59930102295342211`, A1); «Have you ever
traveled» falta «travelled» (`67fab6d599301022953428dc`, A2): el curso usa las dos. «Why had she worked there until
then?» falta «till» (`67fab6d5993010229534326a`, B1). Lo decidió José el 30/9 ([DOUBT-GRAFIAS](#DOUBT-GRAFIAS),
[DOUBT-PREPOSICIONES](#DOUBT-PREPOSICIONES)): las dos listas van a la misma tabla del corrector.

<a id="F-RESPC-R005"></a>
### Rule[F-RESPC-R005] - Un hallazgo por forma que falta, que se arregla en el corrector o agregándola
**Severity**: major | **Validation**: ASSUMPTION

> Cada forma que falta es un hallazgo del ejercicio, y su identidad es la forma ([F-HALL-R014](#F-HALL-R014)). Gravedad:
> *alta* si la forma que falta está en la miniteoría o en el enunciado del ejercicio; *baja* si es la forma larga en
> una respuesta corta («No, it isn't» e «is not»); *media* en el resto. Resolución *regla*: si la forma es de una clase
> que iguala el corrector nuevo —contracción, negación de *be*, grafía de la lista, *until* y *till*—, se arregla en el
> corrector y no en el curso; si no, se agrega con «|» después de las que hay. Costo *instantáneo*.

La primera respuesta no cambia, así que el audio tampoco. La evidencia es el ejercicio como lo ve el alumno, lo que
acepta y lo que falta: «They ___ (not / be) from Ireland. → acepta 're not; falta aren't». La gravedad por miniteoría
y enunciado es una propuesta: el 077 puso *alta* a 33, *media* a 83 y *baja* a 12 de los 128 de su mitad. Mientras la
copia de la tabla sea la de hoy, estos hallazgos muestran lo que el corrector de hoy rechaza; cuando sale el corrector
y se actualiza la copia, las clases que iguala dejan de dar hallazgos. Lo que queda es lo que la tabla no cubre, y es
el control de que nada vuelve atrás.

<a id="F-RESPC-R006"></a>
### Rule[F-RESPC-R006] - `answer-completeness` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `answer-completeness` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿El corrector acepta
> todas las formas de la respuesta, y ninguna consigna pide una?», familia *errores*, evalúa *ejercicio*, resoluciones
> *regla* y *mesa*, costo *instantáneo*. Alcanza sólo a los de hueco para escribir. Con hallazgo, el ejercicio tiene
> puntaje 0; sin ninguno, 1.

El nombre es [ASSUMPTION]. Una forma que el juez de «una sola respuesta» (paquete C) encuentre y sea la misma con otra
forma llega acá, según la propuesta; cómo, lo diseña C. Desde el 30/9, la otra forma, contraída o larga, nunca es
«otra respuesta que falta» para ese juez: la acepta el corrector.

<a id="F-RESPC-R007"></a>
### Rule[F-RESPC-R007] - Ninguna consigna ni miniteoría pide una forma
**Severity**: major | **Validation**: VALIDATED

> Si la consigna que ve el alumno o la miniteoría del ejercicio le piden usar o evitar la forma contraída o la larga
> («Usa $short forms$ si es posible.», «Usa $long forms$.», «sin contracciones»), el ejercicio tiene un hallazgo cuyo
> arreglo es sacar el pedido; el audio no cambia. «Escribe la respuesta corta» no pide ninguna forma.

<details><summary>Detalle y casos del curso</summary>

Lo pidió José el 30/9: «No quiero más ejercicios cuyas instrucciones pidan "short-form" o "long-form". Que acepte
siempre ambas.» Censo del 29/9: 694 ejercicios, todos de hueco para escribir, con 27 consignas distintas en 22 temas:
574 piden *short forms* y 120 *long forms* (A1 554, A2 100, B1 20, B2 20). Desde el 26/9 casi todas empiezan con la
oración que agregó `be/scripts/unificar-short-forms.py` (#1152): en 654 el pedido es esa primera oración y el arreglo
es una *regla* que la saca. En los 40 de *Contracciones* (A1) el pedido es la lección: ver
[DOUBT-RESPC-CONTRACCIONES](#DOUBT-RESPC-CONTRACCIONES). La consigna del tema no la ve el alumno: se iguala con la del
ejercicio en un arreglo de datos fuera de este paquete (hoy 21 temas nombran una forma en la suya).

Miniteorías: una pide una forma, «Elige $am$, $is$ o $are$ según el sujeto, y usa $short forms$.» (3 ejercicios, A1).
Dos más nombran una forma sin pedirla (40 ejercicios de B2): [DOUBT-RESPC-DESCRIBE](#DOUBT-RESPC-DESCRIBE).

Gravedad *baja* [ASSUMPTION]: con el corrector nuevo, el pedido no le hace perder a nadie. El orden de salida es
corrector primero y contenido después: si el pedido se saca antes, quien escribe la otra forma da mal. La evidencia
muestra la consigna o la miniteoría y la frase que pide; la identidad nombra cuál de las dos es.

</details>

## Contexto

Base: curso vivo del 29/9 en `db/english-course`. Las filas del analista salen de su prototipo de R002 a R004; las del
arquitecto, de un segundo prototipo del 30/9 que repite la regla y agrega lo que cambió José. Los dos dan dos números
porque la contracción después de un nombre («Peter's cooking») y con el sujeto fuera del hueco («They» + «'re going to
break») entran o no:

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Ejercicios de hueco para escribir | 1.692 | 1.551 | 3.554 | 1.680 | 8.477 |
| … con más de una respuesta aceptada | 158 | 163 | 461 | 392 | 1.174 |
| Marcados por el 077 | 75 | 53 | 235 | 121 | 484 |
| Analista, sólo sujeto pronombre | 95 | 131 | 623 | 402 | 1.251 |
| Analista, cualquier sujeto | 181 | 211 | 885 | 466 | 1.743 |
| … sólo por grafía (R004) | 4 | 16 | 32 | 24 | 76 |
| Arquitecto, sólo sujeto pronombre | 73 | 132 | 656 | 385 | 1.246 |
| Arquitecto, cualquier sujeto | 170 | 229 | 782 | 427 | 1.608 |
| … sin la excepción por consigna (José, 30/9) | 398 | 273 | 787 | 440 | 1.898 |
| Quedan si el corrector iguala las contracciones | 7 | 16 | 151 | 61 | 235 |
| … y también las grafías de la lista | 3 | 0 | 114 | 30 | 147 |
| … y también *until* y *till* | 0 | 0 | 112 | 30 | 142 |
| Consignas que piden una forma (R007) | 554 | 100 | 20 | 20 | 694 |
| Miniteorías que piden una forma (R007) | 3 | 0 | 0 | 0 | 3 |

Los 142 que quedan son preguntas negativas y *question tags* («Doesn't she…?», «…won't he?»): el corrector tiene que
mover el *not* detrás del sujeto; si lo hace, no queda ninguno. Si además el corrector toma «'s» sólo como *is* y «'d»
sólo como *would*, quedan 345: los casos de oro tienen que incluir «'s» como *has* y «'d» como *had* («He's visited»,
«She'd run»). Con cualquier sujeto hay marcas en 183 temas; 25 ejercicios más mezclan grafía y contracción. El 077
anotó menos porque fue selectivo: marcó «There is» sin «There's», pero no «He is a boss of ours.» sin «He's…»
(`67fab6d59930102295342bfc`, A2, *Doble posesivo*), que es la misma falta.

**El examen.** `escribir/respuestas.json` trae 128 casos (A1 75, A2 53). Con la tabla de hoy, el prototipo del
analista encuentra 127 con cualquier sujeto (122 con sujeto pronombre) y el del arquitecto, 123; el que no encuentra
ninguno es el de la tilde, que el corrector acepta (R001). Con la tabla del corrector nuevo, los 128 dejan de ser
errores: el corrector arregla 127 y «café» ya da bien. **Meta propuesta**: con la tabla de hoy, 95 de cada 100 del oro
y 8 de cada 10 marcas nuevas reales en una muestra revisada a mano; con la del corrector nuevo, ninguna marca de las
clases que iguala; y R007, que es un censo, los 694 y los 3. **Costo**: segundos.

## Alcance

- **Adentro**: R001 a R007 sobre los huecos para escribir, sus hallazgos y la ficha.
- **Afuera**: respuestas distintas e igual de buenas (juez de «una sola respuesta», paquete C); la «/» dentro de una
  respuesta (FEAT-DATOS); cambiar el corrector del backend, que hace la rama `claude/corrector-formas-equivalentes` de
  ittm; aplicar los arreglos, que es del paquete E; y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-RESPC-J001] - Encontrar las formas que el corrector rechaza y los pedidos de forma
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RESPC-J001
    name: Encontrar las formas que el corrector rechaza y los pedidos de forma
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo answer-completeness"
        then: armar
      - id: armar
        action: "Para cada respuesta aceptada, el informe arma sus formas equivalentes y las compara como el corrector, con su tabla; y lee la consigna y la miniteoría del ejercicio"
        gate: [F-RESPC-R001, F-RESPC-R006]
        outcomes:
          - {when: "Falta la forma contraída o la larga", then: falta}
          - {when: "Falta la otra negación corta de be", then: falta}
          - {when: "Falta la otra grafía de una palabra de la lista, o until o till", then: falta}
          - {when: "La consigna o la miniteoría piden short forms o long forms", then: pide_forma}
          - {when: "Todas las formas equivalentes están aceptadas y nada pide una forma", then: completo}
      - {id: falta, action: "Un hallazgo por forma que falta, con lo que acepta y lo que falta, que se arregla en el corrector o agregándola con «|»", gate: [F-RESPC-R002, F-RESPC-R003, F-RESPC-R004, F-RESPC-R005], result: success}
      - {id: pide_forma, action: "Un hallazgo con la consigna o la miniteoría y la frase que pide, que se arregla sacando el pedido", gate: [F-RESPC-R007], result: success}
      - {id: completo, action: "Sin hallazgo de este analizador; el ejercicio queda con puntaje 1", gate: [F-RESPC-R002, F-RESPC-R006], result: success}
```

## Open Questions

<a id="DOUBT-CONTRAIDA-LARGA"></a>
### Doubt[DOUBT-CONTRAIDA-LARGA] - ¿El corrector trata igual la forma contraída y la larga?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí; el corrector del backend las iguala antes de comparar (carril 1 del brief 081), y
  R002 y R003 dejan de dar hallazgos porque el analizador compara como el corrector.
- [ ] Opción B: no; se agrega la forma que falta con «|», ejercicio por ejercicio.

**Answer**: **Opción A, sin excepción por consigna** (José, 30/9): «No quiero más ejercicios cuyas instrucciones pidan
"short-form" o "long-form". Que acepte siempre ambas.» El corrector iguala la forma contraída y la larga siempre, y
ninguna consigna ni miniteoría puede pedir una forma ([F-RESPC-R007](#F-RESPC-R007)). Se borró la excepción de R002 («si
pide cortas, sin la larga»). El orden de salida es corrector primero y contenido después.

<a id="DOUBT-GRAFIAS"></a>
### Doubt[DOUBT-GRAFIAS] - ¿Se aceptan la grafía británica y la americana?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí, las dos, con la lista cerrada de R004 (76 ejercicios, 101 con los mixtos).
- [ ] Opción B: una sola para todo el curso, y se corrigen los ejercicios que usan la otra.
- [ ] Opción C: no se exige.

**Answer**: **Opción A** (José, 30/9): se aceptan las dos, con lista cerrada (*travelled* y *traveled*), y la lista va
a la misma tabla del corrector.

<a id="DOUBT-PREPOSICIONES"></a>
### Doubt[DOUBT-PREPOSICIONES] - ¿Se agregan con «|» las preposiciones sinónimas?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sólo *until* y *till*, que valen igual en toda oración, como una regla más.
- [ ] Opción B: una lista más larga, como *on* o *at the weekend*.
- [ ] Opción C: ninguna; lo decide el juez de «una sola respuesta».

**Answer**: **Opción A** (José, 30/9): sólo *until* y *till*, y van a la misma tabla del corrector (R004), no con «|»
ejercicio por ejercicio. Hoy son 2 en respuestas, como «Why had she worked there until then?»
(`67fab6d5993010229534326a`, B1).

<a id="DOUBT-RESPC-IMITAR"></a>
### Doubt[DOUBT-RESPC-IMITAR] - ¿Cómo sigue content-audit al corrector si el corrector cambia?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): content-audit escribe la misma comparación y los dos proyectos pasan la misma tabla de
  casos («cafe», «every day», «aren't»…).
- [ ] Opción B: content-audit le pregunta al backend.

**Answer**: **Opción A** (José, 30/9): una sola tabla de formas equivalentes, la del corrector de Go, con sus casos de
oro en un archivo que comparten los dos lados; content-audit imita al corrector corriendo esos mismos casos.

<a id="DOUBT-RESPC-CONTRACCIONES"></a>
### Doubt[DOUBT-RESPC-CONTRACCIONES] - ¿Qué pasa con el tema *Contracciones*, cuya lección es contraer?
**Status**: OPEN

- [ ] Opción A (recomendada): va a la mesa y se pasa a opción múltiple con contracciones distintas como opciones
  («don't · doesn't · isn't»), sin la forma larga entre ellas.
- [ ] Opción B: se deja como está, aceptando las dos formas.
- [ ] Opción C: el corrector no iguala en ese tema.

**Answer**: Pendiente. Sus 40 ejercicios (A1) piden la contracción de lo que está entre paréntesis: «You ___ (do not)
work at the bank.» → «don't». Con el corrector nuevo, «do not» también da bien y el ejercicio deja de medir nada; y
sacar el pedido lo deja sin consigna. Recomiendo A: con opciones que son contracciones distintas, el alumno sigue
eligiendo cuál y no hay dos correctas ([F-OMVAL-R002](#F-OMVAL-R002)). B conserva un tema que ya no enseña, y C es
la excepción por consigna que José descartó.

<a id="DOUBT-RESPC-DESCRIBE"></a>
### Doubt[DOUBT-RESPC-DESCRIBE] - ¿Una miniteoría que nombra una forma sin pedirla es hallazgo?
**Status**: OPEN

- [ ] Opción A (recomendada): no; sólo cuenta la que la pide (1 miniteoría, 3 ejercicios).
- [ ] Opción B: sí, toda la que la nombra (3 miniteorías, 43 ejercicios, como el conteo del 26/9).

**Answer**: Pendiente. Las dos de B2 enseñan el orden de la pregunta negativa: «La pregunta negativa arranca con el
auxiliar en $short form$: $Doesn't she dance$?» y «También vale con $long forms$: $Do you not live in London$?». No
piden nada, y lo que dicen es lo que el corrector nuevo tiene que aceptar. Recomiendo A; con B, sacar esas frases
borra la única explicación de «Does she not…?».

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, conteo e identidad: R005 a R007.
- **FEAT-QSENT** — Todas las respuestas son la misma, separadas por «|» ([F-QSENT-R001](#F-QSENT-R001),
  [F-QSENT-R002](#F-QSENT-R002)), y la primera es la canónica ([F-QSENT-R018](#F-QSENT-R018)): R001 y R005.
- **FEAT-OMVAL** — Dos opciones que sólo difieren en la forma son la misma respuesta ([F-OMVAL-R002](#F-OMVAL-R002)):
  DOUBT-RESPC-CONTRACCIONES.
- **FEAT-DATOS**, **FEAT-DUPEX** — Usan la comparación de R001.
