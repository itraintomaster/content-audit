---
feature:
  id: FEAT-RESPC
  code: F-RESPC
  name: Respuestas completas - el corrector acepta todas las formas que la consigna permite
  priority: high
---

# Respuestas completas: el corrector acepta todas las formas que la consigna permite

## TL;DR

**Qué**: Un analizador de la familia de errores arma, para cada respuesta aceptada, sus formas equivalentes —contraída
y larga, las dos negaciones cortas, la grafía británica y la americana— y marca las que el corrector rechazaría.

**Por qué**: El 077 encontró 484 ejercicios donde una respuesta correcta da mal, casi todos por una forma que falta, y
hoy nadie lo mide.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber si el corrector rechaza respuestas buenas:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible, pero sólo mide la primera
   respuesta de cada ejercicio ([F-QSENT-R018](#F-QSENT-R018)).
2. Mira qué acepta el corrector → ⚠️ content-audit no sabe cómo compara el corrector del backend: «They ___ (not / be)
   from Ireland.» acepta «'re not» y rechaza «aren't», la que enseña su propia miniteoría, y ningún número lo muestra.
3. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3.

**Cambio mínimo necesario**: sumar `answer-completeness` al catálogo de FEAT-HALL, con la comparación del corrector
escrita como regla de content-audit.

## Reglas de Negocio

Aplica a los ejercicios de hueco para escribir (8.477). En opción múltiple el alumno no escribe: allí rige FEAT-OMVAL.
Todas las respuestas aceptadas son formas de la misma respuesta ([F-QSENT-R001](#F-QSENT-R001)).

<a id="F-RESPC-R001"></a>
### Rule[F-RESPC-R001] - Una forma está aceptada si el corrector del backend la acepta
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una forma escrita en el hueco está aceptada si es igual a alguna respuesta aceptada después de: separar cada
> respuesta en «|» (la «/» no separa), pasar a minúsculas y sacar los espacios, los signos de exclamación,
> pregunta, dos puntos, punto y coma, coma, punto, guion y guion bajo, los apóstrofos, las comillas y las tildes.
> Invariantes: «cafe» está aceptada si lo está «café»; «every day» y «everyday» son iguales; «aren't», «are not» y
> «'re not» son tres formas distintas.

Confirmado leyendo `be/domain/quiz/pkg/shared/cloze_sentence_part.go` (`Evaluate` y `normalize`) en ittm: coincide con
la propuesta («parte en «|», pasa a minúsculas y saca signos»), y además saca espacios y tildes. Las tildes se sacan
desde el 19/5/2025 (#884); `escribir/respuestas.json` dice lo contrario de `67fab6d599301022953422c5` («Don't run in
the café.»), y el corrector acepta «cafe». Cómo se mantiene igual a la del backend: [DOUBT-RESPC-IMITAR](#DOUBT-RESPC-IMITAR).

<a id="F-RESPC-R002"></a>
### Rule[F-RESPC-R002] - Si la consigna no pide una forma, están la contraída y la larga
**Severity**: critical | **Validation**: ASSUMPTION

> Para cada respuesta aceptada están aceptadas también su forma contraída y su forma larga: la negación (*don't* y *do
> not*, *can't* y *cannot*, *won't* y *will not*) y el auxiliar pegado al sujeto (*I'm* y *I am*, *there's* y *there
> is*, *I'd like* y *I would like*), también cuando el sujeto queda fuera del hueco («'m not» y «am not»). Excepciones:
> si la consigna pide *short forms*, la larga no hace falta; si pide *long forms*, la contraída no hace falta.
> «Escribe la respuesta corta» no pide ninguna forma.

Ejemplos: «I ___ (not / eat) meat often.» acepta «don't eat» y falta «do not eat» (`67fab6d5993010229534263c`, A2);
«A house is near the school. ___» acepta «There is a house near the school.» y falta «There's…»
(`67fab6d59930102295342407`, A1). Lo decide [DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA). Consignas del curso: 574
piden *short forms* (A1 434), 120 piden *long forms* (todas de A1) y 345 piden la respuesta corta.

<a id="F-RESPC-R003"></a>
### Rule[F-RESPC-R003] - Las dos negaciones cortas de *be* están aceptadas
**Severity**: critical | **Validation**: ASSUMPTION

> Si una respuesta usa *isn't* o *aren't*, la otra forma corta (*'s not*, *'re not*) también está aceptada, y al revés,
> aunque la consigna pida *short forms*: las dos lo son.

«They ___ (not / be) from Ireland.» acepta «'re not» y falta «aren't», que es la que enseña la miniteoría del ejercicio
(`67fab6d599301022953425ac`, A1). También lo decide [DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA).

<a id="F-RESPC-R004"></a>
### Rule[F-RESPC-R004] - Están la grafía británica y la americana
**Severity**: major | **Validation**: ASSUMPTION

> Si una respuesta lleva una palabra de la lista cerrada de grafías (*travelled* y *traveled*, *learnt* y *learned*,
> *dreamt* y *dreamed*, *colour* y *color*, *centre* y *center*, *realise* y *realize*, *grey* y *gray*, *has got* y
> *has gotten*, y las de su familia), la otra grafía también está aceptada.

«She ___ (travel) to Europe.» acepta «travelled» y falta «traveled» (`67fab6d59930102295342211`, A1); «Have you ever
traveled» falta «travelled» (`67fab6d599301022953428dc`, A2): el curso usa las dos. Lo decide
[DOUBT-GRAFIAS](#DOUBT-GRAFIAS). Las preposiciones sinónimas no están en esta regla:
[DOUBT-PREPOSICIONES](#DOUBT-PREPOSICIONES).

<a id="F-RESPC-R005"></a>
### Rule[F-RESPC-R005] - Un hallazgo por forma que falta, que se arregla agregándola
**Severity**: major | **Validation**: ASSUMPTION

> Cada forma que falta es un hallazgo del ejercicio, y su identidad es la forma ([F-HALL-R014](#F-HALL-R014)). Gravedad:
> *alta* si la forma que falta está en la miniteoría o en el enunciado del ejercicio; *baja* si es la forma larga en
> una respuesta corta («No, it isn't» e «is not»); *media* en el resto. Resolución *regla*: se agrega con «|» después
> de las que hay. Costo *instantáneo*.

La primera respuesta no cambia, así que el audio tampoco. La evidencia es el ejercicio como lo ve el alumno, lo que
acepta y lo que falta: «They ___ (not / be) from Ireland. → acepta 're not; falta aren't». La gravedad por miniteoría
y enunciado es una propuesta: el 077 puso *alta* a 33, *media* a 83 y *baja* a 12 de los 128 de su mitad.

<a id="F-RESPC-R006"></a>
### Rule[F-RESPC-R006] - `answer-completeness` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `answer-completeness` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿El corrector acepta
> todas las formas que la consigna permite?», familia *errores*, evalúa *ejercicio*, resolución *regla*, costo
> *instantáneo*. Alcanza sólo a los de hueco para escribir. Con hallazgo, el ejercicio tiene puntaje 0; sin ninguno, 1.

El nombre es [ASSUMPTION]. Una forma que el juez de «una sola respuesta» (paquete C) encuentre y sea la misma con otra
forma llega acá, según la propuesta; cómo, lo diseña C.

## Contexto

Base: curso vivo del 29/9 en `db/english-course`, medido con un prototipo de R002 a R004 en el scratchpad del analista.
Da dos números porque la contracción después de un nombre («Peter's cooking») y con el sujeto fuera del hueco
(«They» + «'re going to break») entra o no según [DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA):

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Ejercicios de hueco para escribir | 1.692 | 1.551 | 3.554 | 1.680 | 8.477 |
| … con más de una respuesta aceptada | 158 | 163 | 461 | 392 | 1.174 |
| Marcados por el 077 | 75 | 53 | 235 | 121 | 484 |
| Prototipo, sólo sujeto pronombre | 95 | 131 | 623 | 402 | 1.251 |
| Prototipo, cualquier sujeto | 181 | 211 | 885 | 466 | 1.743 |
| … sólo por grafía (R004) | 4 | 16 | 32 | 24 | 76 |

Con cualquier sujeto hay marcas en 183 temas; 25 ejercicios más mezclan grafía y contracción. El 077 anotó menos
porque fue selectivo: marcó «There is» sin «There's», pero no «He is a boss of ours.» sin «He's…»
(`67fab6d59930102295342bfc`, A2, *Doble posesivo*), que es la misma falta.

**El examen.** `escribir/respuestas.json` trae 128 casos (A1 75, A2 53). Con cualquier sujeto el prototipo encuentra
127; el que falta es el de la tilde, que el corrector acepta (R001). Con sujeto pronombre encuentra 122. Marca 265 más
en A1–A2 (104 con sujeto pronombre), de la misma clase. La otra mitad tiene 356. **Meta propuesta**: 95 de cada 100
del oro, porque son mecánicas; y 8 de cada 10 marcas nuevas reales en una muestra revisada a mano con la pregunta
«¿el corrector rechaza una forma correcta que la consigna permite?». Contra el oro solo, la precisión sale cerca de 1
de cada 3, por construcción. **Costo**: segundos.

## Alcance

- **Adentro**: R001 a R006 sobre los huecos para escribir, sus hallazgos y la ficha.
- **Afuera**: respuestas distintas e igual de buenas (juez de «una sola respuesta», paquete C); la «/» dentro de una
  respuesta (FEAT-DATOS); cambiar el corrector del backend, que es una decisión de José
  ([DOUBT-CONTRAIDA-LARGA](#DOUBT-CONTRAIDA-LARGA)); y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-RESPC-J001] - Encontrar las formas que el corrector rechaza
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-RESPC-J001
    name: Encontrar las formas que el corrector rechaza
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo answer-completeness"
        then: armar
      - id: armar
        action: "Para cada respuesta aceptada, el informe arma sus formas equivalentes y las compara como el corrector"
        gate: [F-RESPC-R001, F-RESPC-R006]
        outcomes:
          - {when: "Falta la forma contraída o la larga y la consigna no pide ninguna", then: falta}
          - {when: "La consigna pide short forms y sólo falta la forma larga", then: completo}
          - {when: "Falta la otra negación corta de be", then: falta}
          - {when: "Falta la otra grafía de una palabra de la lista", then: falta}
          - {when: "Todas las formas equivalentes están aceptadas", then: completo}
      - {id: falta, action: "Un hallazgo por forma que falta, con lo que acepta y lo que falta, que se arregla agregándola con «|»", gate: [F-RESPC-R002, F-RESPC-R003, F-RESPC-R004, F-RESPC-R005], result: success}
      - {id: completo, action: "Sin hallazgo de este analizador; el ejercicio queda con puntaje 1", gate: [F-RESPC-R002, F-RESPC-R006], result: success}
```

## Open Questions

<a id="DOUBT-CONTRAIDA-LARGA"></a>
### Doubt[DOUBT-CONTRAIDA-LARGA] - ¿El corrector trata igual la forma contraída y la larga?
**Status**: OPEN

- [ ] Opción A (recomendada): sí; el corrector del backend las iguala antes de comparar (carril 1 del brief 081), y
  R002 y R003 dejan de dar hallazgos porque el analizador compara como el corrector.
- [ ] Opción B: no; se agrega la forma que falta con «|», ejercicio por ejercicio.

**Answer**: Pendiente. Recomiendo A: arregla la clase entera de una vez, también en ejercicios futuros, y evita tocar
entre 1.251 y 1.743 ejercicios; unas 500 propuestas del 077 sólo agregan la otra forma. El costo: en los 694 ejercicios cuya
consigna pide *short* o *long forms*, el corrector aceptaría las dos. Con B, decidir también si entra el sujeto nombre.

<a id="DOUBT-GRAFIAS"></a>
### Doubt[DOUBT-GRAFIAS] - ¿Se aceptan la grafía británica y la americana?
**Status**: OPEN

- [ ] Opción A (recomendada): sí, las dos, con la lista cerrada de R004 (76 ejercicios, 101 con los mixtos).
- [ ] Opción B: una sola para todo el curso, y se corrigen los ejercicios que usan la otra.
- [ ] Opción C: no se exige.

**Answer**: Pendiente. Recomiendo A: el curso usa las dos («travelled» en *Cambios ortográficos*, «traveled» en
*Experiencias con ever*), así que el alumno puede haber aprendido cualquiera.

<a id="DOUBT-PREPOSICIONES"></a>
### Doubt[DOUBT-PREPOSICIONES] - ¿Se agregan con «|» las preposiciones sinónimas?
**Status**: OPEN

- [ ] Opción A (recomendada): sólo *until* y *till*, que valen igual en toda oración, como una regla más.
- [ ] Opción B: una lista más larga, como *on* o *at the weekend*.
- [ ] Opción C: ninguna; lo decide el juez de «una sola respuesta».

**Answer**: Pendiente. Recomiendo A: hoy son 2, como «Why had she worked there until then?»
(`67fab6d5993010229534326a`, B1). Con B, la lista depende de la oración y deja de ser regla: eso es C.

<a id="DOUBT-RESPC-IMITAR"></a>
### Doubt[DOUBT-RESPC-IMITAR] - ¿Cómo sigue content-audit al corrector si el corrector cambia?
**Status**: OPEN

- [ ] Opción A (recomendada): content-audit escribe la misma comparación y los dos proyectos pasan la misma tabla de
  casos («cafe», «every day», «aren't»…).
- [ ] Opción B: content-audit le pregunta al backend.

**Answer**: Pendiente. Recomiendo A: content-audit corre sin red, y la tabla avisa si uno cambia sin el otro. B ata cada
análisis al backend.

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, conteo e identidad: R005 y R006.
- **FEAT-QSENT** — Todas las respuestas son la misma, separadas por «|» ([F-QSENT-R001](#F-QSENT-R001),
  [F-QSENT-R002](#F-QSENT-R002)), y la primera es la canónica ([F-QSENT-R018](#F-QSENT-R018)): R001 y R005.
- **FEAT-OMVAL**, **FEAT-DATOS**, **FEAT-DUPEX** — Usan la comparación de R001.
