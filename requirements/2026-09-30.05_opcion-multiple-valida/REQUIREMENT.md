---
feature:
  id: FEAT-OMVAL
  code: F-OMVAL
  name: Opción múltiple válida - las reglas de las opciones
  priority: high
---

# Opción múltiple válida: una sola correcta, opciones distintas y ninguna que se delate

## TL;DR

**Qué**: Un analizador de la familia de errores revisa las opciones de cada ejercicio de opción múltiple: una sola
correcta, todas distintas —también en la forma—, con la mayúscula que pide la oración y sin un artículo que delate la
respuesta.

**Por qué**: El 077 marcó 315 de los 2.810 ejercicios de opción múltiple (11,2 %), casi todos por forma, y hoy
content-audit los mide pero no mira sus opciones.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy saber si las opciones de un ejercicio están bien armadas:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: desde el 30/9 mide los
   2.810 de opción múltiple por su oración con la correcta en el hueco ([F-OPMUL-R004](#F-OPMUL-R004)).
2. Mira las opciones de un ejercicio → ⚠️ ningún analizador las lee; el juez de consigna las recibe
   ([F-OPMUL-R005](#F-OPMUL-R005)) pero juzga la consigna, no las opciones.
3. Pide ese analizador solo → ⚠️ no existe en `GetCommand.get("analyzers", …)`.

**Punto de fricción**: pasos 2 y 3. «___ a child? (He)» ofrece «am i», «is he»… en minúscula, y nada lo marca.

**Cambio mínimo necesario**: sumar `multiple-choice-validity` al catálogo de FEAT-HALL, con las reglas de este
requirement. El juez de «otra opción también vale» es del paquete C.

## Reglas de Negocio

Las reglas leen las opciones como las ve el alumno, cuál es la correcta, el enunciado y dónde está el hueco. Una opción
**vale** si el curso le da puntaje; comparar **como el corrector** es sin mayúsculas, espacios, signos ni tildes, y
tomando como iguales las formas que iguala la tabla de formas equivalentes del corrector ([F-RESPC-R001](#F-RESPC-R001)).

<a id="F-OMVAL-R001"></a>
### Rule[F-OMVAL-R001] - Una sola opción vale, y vale el puntaje entero
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo ejercicio de opción múltiple tiene exactamente una opción que vale, y vale el puntaje entero. Si no vale
> ninguna, o valen dos o más, el ejercicio tiene un hallazgo *bloqueante*: eligiendo una sola opción, el alumno nunca
> llega al puntaje entero.

El backend da por buena la respuesta sólo si lo elegido suma el puntaje entero, y rechaza un ejercicio cuyas opciones
no suman eso (`checkEvaluationMultipleChoice`). Hoy: 0 de 2.810. Que una incorrecta *también* sea válida en el contexto
(*because*, *as* y *since*) no es esta regla: lo decide el juez del paquete C
([DOUBT-OMVAL-DOS-CORRECTAS](#DOUBT-OMVAL-DOS-CORRECTAS)).

<a id="F-OMVAL-R002"></a>
### Rule[F-OMVAL-R002] - Las opciones son distintas entre sí, también en la forma
**Severity**: critical | **Validation**: VALIDATED

> Ninguna opción es igual a otra comparadas como el corrector: dos opciones que sólo difieren en la forma contraída y
> la larga («isn't» e «is not»), o en una grafía de la lista, son la misma respuesta. Si son iguales y una es la
> correcta, hay dos correctas: *bloqueante* si se ven idénticas, porque el alumno no puede distinguirlas; *alta* si
> cambia la mayúscula, un signo, la contracción o la grafía, porque marca mal a quien eligió la otra. Si las dos son
> incorrectas: *baja*, sobra una.

Es también la lectura segura de «ninguna distinta de la correcta sólo en la forma»
([DOUBT-OMVAL-FORMA](#DOUBT-OMVAL-FORMA)). Contraída y larga, y las grafías, lo decidió José el 30/9: son la misma
respuesta, como en el corrector nuevo. Hoy: 0 con la correcta; 28 con dos incorrectas que son la misma, *baja* (A1 20
en *Elegir la forma natural*: «'re going to» y «are going to»; A2 8 en *Obligación*: «'ve got to» y «have got to»).
Se resuelven en la *mesa*, que rearma el ejercicio con una sola correcta. La tabla es la de
[F-RESPC-R001](#F-RESPC-R001): mientras la copia sea la de hoy, sólo cuentan la mayúscula y los signos.

<a id="F-OMVAL-R003"></a>
### Rule[F-OMVAL-R003] - Si el hueco abre la oración, todas las opciones abren con mayúscula
**Severity**: major | **Validation**: AUTO_VALIDATED

> El hueco abre la oración cuando va primero, o después de un punto, un signo de pregunta o de exclamación o un «A:» o
> «B:» de diálogo, y el texto que sigue termina en punto, pregunta o exclamación. Entonces:
> 1. toda opción empieza con mayúscula, y el pronombre «I» va en mayúscula en toda opción, abra o no la oración;
> 2. ninguna opción es el guion de «no poner nada», porque la oración quedaría empezando en minúscula;
> 3. el hallazgo es *baja* si todas las opciones tienen la misma mayúscula, y *media* si unas sí y otras no, porque la
>    forma delata la respuesta.

<details><summary>Detalle y casos del curso</summary>

- `67fab6d59930102295341e87` (A1, *Be: preguntas yes/no*): «___ a child? (He)» con *am i · are we · are you · **is he**
  · is it · is she*. Hallazgo: abre la pregunta en minúscula, y «i» va en minúscula.
- `67fab6d599301022953429d4` (A2, *Con artículo o guion*): «___ golf is difficult to learn.» con ***-*** · *a*: con la
  correcta la oración empieza «golf is…».
- No abre una oración: «___ excited man» (*A o an*, 20 ejercicios) no tiene punto final; queda sin hallazgo.

Resolución ([F-HALL-R002](#F-HALL-R002)): *regla* para la mayúscula, en todas las opciones a la vez y nunca en una sola;
*mesa* para el guion, porque hay que rearmar la oración. Hoy no hay ningún caso de la invariante 3 en *media*: el
cambio a opción múltiple dejó todas las opciones en minúscula.

</details>

<a id="F-OMVAL-R004"></a>
### Rule[F-OMVAL-R004] - Ninguna opción queda delatada por el artículo de antes del hueco
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si el texto justo antes del hueco termina en «a» o «an», toda opción empieza con el sonido que ese artículo pide. Si
> alguna no, el ejercicio tiene un hallazgo *media*: el artículo delata cuál es la correcta.

Hoy: 2, los dos de B2 en *Grados de énfasis*: «He's an ___ amazing man.» con ***absolutely*** · *very*
(`67fab6d599301022953448f4`) y «It was an ___ wonderful experience.» (`67fab6d599301022953448fc`). Se resuelve en la
*mesa*. Singular o plural quedan afuera: [DOUBT-OMVAL-FORMA](#DOUBT-OMVAL-FORMA).

<a id="F-OMVAL-R005"></a>
### Rule[F-OMVAL-R005] - `multiple-choice-validity` evalúa sólo los ejercicios de opción múltiple
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `multiple-choice-validity` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Hay una sola
> opción correcta, y ninguna se delata por la forma?», familia *errores*, evalúa *ejercicio*, resoluciones *regla* y
> *mesa*. Alcanza sólo a los de opción múltiple (2.810), y un ejercicio de hueco para escribir nunca tiene hallazgo
> suyo. Con hallazgo, el ejercicio tiene puntaje 0 en este analizador; sin ninguno, 1.

Las cuatro reglas cuestan *instantáneo*. El juez de «otra opción también vale» (FEAT-MCVALE, paquete C) se suma como
otra regla del mismo analizador, con costo *modelo pago*, y la ficha pasa a declarar ése, el mayor
([F-HALL-R006](#F-HALL-R006)): una sola fila y una sola ficha (José, 30/9; [DOUBT-OMVAL-JUEZ](#DOUBT-OMVAL-JUEZ)). La
identidad del hallazgo es regla + ejercicio + la opción marcada ([F-HALL-R014](#F-HALL-R014)). El nombre es
[ASSUMPTION].

## Contexto

La opción múltiple entró el 26/9: 2.810 ejercicios que producción convirtió desde huecos para escribir. content-audit
los carga, conserva y mide desde el 30/9 (FEAT-OPMUL), pero no corrige ninguno ([F-OPMUL-R006](#F-OPMUL-R006)). La
conversión dejó las opciones en minúscula aunque el hueco abra la pregunta, y eso es casi todo lo que marcó el 077.

Base: curso vivo del 29/9 en `db/english-course`, medido con un prototipo de las reglas en el scratchpad del analista:

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---|---|---|---|---|
| Ejercicios de opción múltiple | 710 | 860 | 1.000 | 240 | 2.810 |
| Marcados por el 077 | 84 | 102 | 86 | 43 | 315 |
| R003, mayúscula al abrir la oración | 89 | 107 | 87 | 41 | 324 |
| … de ellos, con el guion (R003.2) | 14 | 40 | 6 | 9 | 69 |
| R004, artículo que delata | 0 | 0 | 0 | 2 | 2 |
| R001, y R002 con la correcta | 0 | 0 | 0 | 0 | 0 |
| R002, dos incorrectas que son la misma (José, 30/9) | 20 | 8 | 0 | 0 | 28 |

Hay opción múltiple en 98 de los 413 temas; R003 marca en 32. «i» en minúscula aparece en 40 de A1, 20 de ellos sin
abrir la oración, como «A: Can you call Miss Green? B: No, ___ .» con *i can't* (`67fab6d5993010229534250b`).

**El examen.** `escribir/opcion.json` trae 186 casos (A1 84, A2 102), todos de forma y de gravedad *baja*: 170 de
mayúscula y 16 de guion o diálogo. El prototipo encuentra los 186 y marca 10 más con la misma falta, como
«___ next week we're going to call my sister.» con *- · at · in · on* (`67fab6d59930102295342435`) y «___ will
understand.» con *anybody · nobody · somebody* (`67fab6d59930102295342c50`). La otra mitad tiene 129 y puede traer
casos del juez. **Meta propuesta**: 9 de cada 10 de los casos de forma de la otra mitad, y 8 de cada 10 marcas nuevas
reales en una muestra. **Costo**: segundos, sin modelo.

## Alcance

- **Adentro**: R001 a R004 sobre las opciones, sus hallazgos y la ficha, y el lugar por donde entran los veredictos
  del juez de «otra opción también vale».
- **Afuera**: el juez mismo —evaluador, libro de evaluaciones, prompt y examen— (paquete C); aplicar el arreglo, que choca con
  [F-OPMUL-R006](#F-OPMUL-R006) ([DOUBT-OMVAL-CORREGIR](#DOUBT-OMVAL-CORREGIR)); la consigna y la [traducción](glossary:Traducción) (otros
  analizadores); y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

## User Journeys

### Journey[F-OMVAL-J001] - Revisar las opciones de un ejercicio de opción múltiple
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-OMVAL-J001
    name: Revisar las opciones de un ejercicio de opción múltiple
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo sólo multiple-choice-validity"
        then: leer_opciones
      - id: leer_opciones
        action: "El informe lee las opciones de cada ejercicio de opción múltiple, cuál vale y dónde está el hueco"
        gate: [F-OMVAL-R005]
        outcomes:
          - {when: "No vale ninguna opción, o valen dos", then: bloqueante}
          - {when: "Dos opciones son la misma respuesta con otra forma, y una es la correcta", then: dos_correctas}
          - {when: "Dos opciones incorrectas son la misma respuesta", then: sobra_una}
          - {when: "El hueco abre la oración y las opciones van en minúscula", then: mayuscula}
          - {when: "El artículo antes del hueco sólo encaja con la correcta", then: delata}
          - {when: "Una sola vale, son distintas y la forma no delata nada", then: sin_hallazgo}
      - {id: bloqueante, action: "Hallazgo bloqueante que cuenta en los errores de su tema, su nivel y el curso", gate: [F-OMVAL-R001, F-OMVAL-R002], result: success}
      - {id: dos_correctas, action: "Hallazgo alta para la mesa, con las dos opciones que son la misma respuesta como evidencia", gate: [F-OMVAL-R002], result: success}
      - {id: sobra_una, action: "Hallazgo baja para la mesa: sobra una de las dos opciones incorrectas", gate: [F-OMVAL-R002], result: success}
      - {id: mayuscula, action: "Hallazgo de gravedad baja que se resuelve con una regla sobre todas las opciones a la vez", gate: [F-OMVAL-R003], result: success}
      - {id: delata, action: "Hallazgo de gravedad media para la mesa, con el artículo y las opciones como evidencia", gate: [F-OMVAL-R004], result: success}
      - {id: sin_hallazgo, action: "Sin hallazgo de este analizador; el ejercicio queda con puntaje 1", gate: [F-OMVAL-R005], result: success}
```

## Open Questions

<a id="DOUBT-OMVAL-DOS-CORRECTAS"></a>
### Doubt[DOUBT-OMVAL-DOS-CORRECTAS] - Cuando otra opción también vale, ¿qué se hace?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): se deja una sola correcta: la mesa cambia la oración o la opción que sobra, y deciden
  los jueces.
- [ ] Opción B: se pasa a «elegí todas las que valen», con selección múltiple.
- [ ] Opción C: se saca la opción que sobra, sin reemplazarla.

**Answer**: **Opción A** (José, 30/9): siempre una sola correcta, y la mesa rearma. Dar puntaje a las dos no es una
salida: con una sola selección el alumno nunca llega al puntaje entero (R001).

<a id="DOUBT-OMVAL-FORMA"></a>
### Doubt[DOUBT-OMVAL-FORMA] - ¿Qué es «distinta de la correcta sólo en la forma»?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): mayúscula o signos (R002) y el artículo «a/an» de antes del hueco (R004).
- [ ] Opción B: además, las opciones que difieren de la correcta sólo en el artículo o en singular y plural.

**Answer**: **Opción A** (José, 30/9, con la recomendación), a la que José sumó la forma contraída y la larga y las
grafías de la lista (R002). Con B se marcaban 73 ejercicios de cuatro temas donde esa diferencia es justo lo
que se enseña: *one* o *ones* (A2, `67fab6d59930102295343951`, «Could I get another ___, please?»), *other* o *the
other* (B1, *Con o sin artículo*), *few* o *a few* (B2, *Escasez*) y *other*, *another* o *the other* (B2).

<a id="DOUBT-OMVAL-CORREGIR"></a>
### Doubt[DOUBT-OMVAL-CORREGIR] - El arreglo por regla choca con que no se corrige ninguna opción múltiple
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): el hallazgo declara su resolución (*regla* o *mesa*) y el paquete E la aplica recién
  cuando José conteste DOUBT-CORREGIR-MC de FEAT-OPMUL.
- [ ] Opción B: mientras tanto, los hallazgos se declaran *sólo ordena*.

**Answer**: **Opción A** (José, 30/9, con la recomendación): el hallazgo declara su resolución y el arreglo espera a
que José conteste DOUBT-CORREGIR-MC de FEAT-OPMUL. Analizar no escribe nada, así que detectar no choca con
[F-OPMUL-R006](#F-OPMUL-R006); aplicar sí.

<a id="DOUBT-OMVAL-JUEZ"></a>
### Doubt[DOUBT-OMVAL-JUEZ] - ¿El juez de «otra opción también vale» es una regla de este analizador?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí, una regla más de `multiple-choice-validity`, con su costo.
- [ ] Opción B: es un analizador aparte, con su propio nombre y su fila.

**Answer**: **Opción A** (José, 30/9): el juez es parte de Opción múltiple, con una sola fila y una sola ficha. Este
paquete diseña el analizador y el lugar por donde entran los veredictos; el paquete C diseña el juez (FEAT-MCVALE) y usa
el mismo nombre.

<a id="DOUBT-OMVAL-JUEZ-A-MEDIAS"></a>
### Doubt[DOUBT-OMVAL-JUEZ-A-MEDIAS] - ¿Cómo cuenta un ejercicio cuyas reglas corrieron pero el juez no?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): evaluado a medias: sus hallazgos de regla cuentan, el nodo declara que el juez no lo vio
  y «algún error» se lee como piso. Pide un campo más en el contrato del hallazgo, que suma el integrador.
- [ ] Opción B: evaluado del todo: los hallazgos de regla cuentan y lo que el juez no vio sólo figura en su cobertura.
- [ ] Opción C: sin evaluar hasta que el juez lo vea.

**Answer**: **Opción A** (José, 30/9: va con su recomendación, entre las once dudas chicas del tablero). La implementa
la integración de la ronda 1 ([F-RONDA1-R006](#F-RONDA1-R006)): el ejercicio cuenta como evaluado, sus hallazgos de
regla valen y el nodo declara al analizador «a medias». El razonamiento: con el juez adentro, choca el contrato: un
ejercicio está evaluado o no, y no hay hallazgos en uno sin evaluar ([F-HALL-R001](#F-HALL-R001)). Sin pedido explícito no se paga, así que en una corrida normal el juez
no ve ninguno de los 2.810. Recomiendo A: la fila muestra los unos 354 con error de regla y avisa que falta el juez.
Con B la fila parece completa sin serlo ([F-HALL-R008](#F-HALL-R008), invariante 3); con C diría que el 100 % de lo
evaluado tiene error, porque sólo quedarían evaluados los que ya tienen un error de regla.

<a id="DOUBT-OMVAL-CORRIDA-BASE"></a>
### Doubt[DOUBT-OMVAL-CORRIDA-BASE] - ¿La vista consolidada muestra los errores de opción múltiple?
**Status**: RESOLVED (2026-09-30)

- [x] Opción A (recomendada): sí: la corrida base corre `multiple-choice-validity` con tope 0, sin pagar: sus reglas
  siempre y del juez sólo lo que ya está registrado.
- [ ] Opción B: no: la corrida base sigue siendo sólo de analizadores instantáneos, y la opción múltiple aparece sólo
  en `analyze`.

**Answer**: **Opción A** (José, 30/9: va con su recomendación, entre las once dudas chicas del tablero). La implementa
la integración de la ronda 1 ([F-RONDA1-R007](#F-RONDA1-R007)). El razonamiento: por el juez, la ficha declara *modelo pago* ([F-HALL-R006](#F-HALL-R006)), y el contrato deja
fuera de la corrida base —la de la vista consolidada y la vista previa— a todo lo que no es instantáneo. Con B esas
dos vistas pierden los unos 354 errores de regla, que no cuestan nada. Recomiendo A: no paga nada y es la misma lectura
que José eligió para `stats analyzer` de un juez (tope 0 con reuso libre).

## References

- **FEAT-HALL** — Hallazgo, gravedad, resolución, costo, ficha, conteo e identidad: R001 a R005.
- **FEAT-OPMUL** — Qué es un ejercicio de opción múltiple, su oración medible y que no se corrige: R005 y dudas.
- **FEAT-RESPC** — Comparar como el corrector, con su tabla de formas equivalentes: R002.
- **FEAT-MCVALE** (paquete C) — El juez de «otra opción también vale», que entra como regla de este analizador: R005.
