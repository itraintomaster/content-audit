---
feature:
  id: FEAT-DUPAR
  code: F-DUPAR
  name: Duplicados parecidos - dos ejercicios casi iguales del mismo tema
  priority: medium
---

# Duplicados parecidos: dos ejercicios casi iguales del mismo tema

## TL;DR

**Qué**: `near-duplicates`, de la familia de errores, marca cada ejercicio que repite casi igual a otro de su tema: la
misma respuesta con otro pronombre, otro demostrativo u otro tiempo del auxiliar, o la misma idea con otras palabras.

**Por qué**: El 077 marcó 6 así en la mitad para escribir, como «He is leaving (certainly)» y «She is leaving
(certainly)»; la regla exacta del paquete A no los ve. Compara con un modelo local que ya está instalado.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar ejercicios que se repiten con un cambio mínimo:

1. Corre el análisis → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible, pero ningún analizador compara
   un ejercicio con sus hermanos.
2. Pide los duplicados → ⚠️ no hay: `exact-duplicates` (FEAT-DUPEX, ronda 1) está diseñado y ve sólo los iguales tal cual.
3. Busca los casi iguales → ⚠️ «I don't have ___ time.» y «I didn't have ___ time.», los dos con *much*, no son iguales
   tal cual y nada los junta.

**Punto de fricción**: pasos 2 y 3. El alumno contesta dos veces lo mismo y ningún número lo muestra.

**Cambio mínimo necesario**: sumar `near-duplicates` al catálogo de FEAT-HALL, con las reglas de este requirement.

## Reglas de Negocio

**Ejercicio terminado** es el de FEAT-DUPEX ([F-DUPEX-R001](#F-DUPEX-R001)): el texto fijo que ve el alumno, sin la
pista, con la primera respuesta aceptada —en opción múltiple, la correcta—, también la oración de partida. **Tema** es
el *knowledge* (413).

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
leaving.» sí piden lo mismo: el pronombre lo copia el alumno del enunciado.

</details>

<a id="F-DUPAR-R002"></a>
### Rule[F-DUPAR-R002] - Casi iguales: cambia sólo el sujeto, el demostrativo, el posesivo o el tiempo, o dicen lo mismo
**Severity**: major | **Validation**: VALIDATED

> Un candidato es casi duplicado si cumple una de dos condiciones:
> 1. los dos ejercicios terminados quedan iguales al cambiar por un comodín los pronombres, los demostrativos, los
>    posesivos y el tiempo del auxiliar;
> 2. el modelo local que compara oraciones les da un parecido de 0,95 o más.

<details><summary>Casos y medidas del curso</summary>

| Condición | Pares | Ejemplo |
|---|---:|---|
| 1, comodín | 24 | «I don't have much time.» / «I didn't have much time.» (A2, en el 077) |
| 2, parecido | 21 | «Have you ever been to Rome?» / «Have you ever visited Rome?» (B1, en el 077) |

Sin el modelo, la condición 1 sola encuentra 5 de los 6 del 077 en `escribir/`; el que falta es el de Rome. El modelo
suma «They cooked lunch…» / «They prepared lunch…» y «The show started on time.» / «Did the show begin on time?». El
umbral es 0,95 (José, 30/9: [DOUBT-DUPAR-UMBRAL](#DOUBT-DUPAR-UMBRAL)).

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
> ([F-HALL-R002](#F-HALL-R002)) es *instantáneo* si lo encontró el comodín y *modelo local* si lo encontró el parecido.

*Baja* porque se puede acertar, como los exactos. *Mesa* porque el arreglo es escribir un ejercicio nuevo con palabras
del semáforo del tema. Cuenta como error y no sólo ordena (José, 30/9: [DOUBT-DUPAR-CUENTA](#DOUBT-DUPAR-CUENTA)).

<a id="F-DUPAR-R005"></a>
### Rule[F-DUPAR-R005] - La evidencia muestra los dos ejercicios y qué cambia
**Severity**: major | **Validation**: AUTO_VALIDATED

> La evidencia trae la copia y el ejercicio que queda, cada uno como lo ve el alumno, y una línea que dice qué cambia
> y por qué condición: «Casi igual a …2f8b del mismo tema: cambia He por She» o «Dice lo mismo que …355e: parecido 0,99».

Nunca el formato interno del curso ([F-HALL-R003](#F-HALL-R003)).

<a id="F-DUPAR-R006"></a>
### Rule[F-DUPAR-R006] - `near-duplicates` es un analizador de errores que evalúa ejercicios
**Severity**: major | **Validation**: ASSUMPTION

> El catálogo lista `near-duplicates` con su ficha ([F-HALL-R006](#F-HALL-R006)): pregunta «¿Dos ejercicios del mismo
> tema piden casi lo mismo?», familia *errores*, evalúa *ejercicio*, resolución *mesa*, costo *modelo local* —el mayor
> de sus dos reglas—, y nombra el modelo con su versión ([F-MLOCAL-R002](#F-MLOCAL-R002)).

El nombre sigue el estilo de `exact-duplicates`.

<a id="F-DUPAR-R007"></a>
### Rule[F-DUPAR-R007] - El examen de Duplicados parecidos
**Severity**: major | **Validation**: ASSUMPTION

> El examen tiene tres partes:
> 1. encuentra los casi iguales del 077 en las dos mitades: los 6 de `escribir/duplicados.json` y los de `examinar/`;
> 2. en una muestra ciega de 40 pares marcados y 40 no marcados de parecido 0,90 a 0,95 con la misma respuesta,
>    mezclados y revisados por una persona, 8 de cada 10 marcados son «el mismo ejercicio para el alumno» y 2 de cada
>    10 no marcados, como mucho;
> 3. dos corridas marcan las mismas copias.

El 077 no tiene categoría propia: sus casi iguales son 6 de los 15 duplicados de `escribir/` y quizá uno de
`examinar/`, pocos para medir la precisión. La muestra ciega cuesta unos 20 minutos de una persona y nada de modelos
pagos.

## Contexto

Base: curso vivo del 29/9 en `db/english-course`, medido con un prototipo en el scratchpad del analista, sin tocar
content-audit. Modelo: `nomic-embed-text` en Ollama, el que la propuesta da por instalado.

| Medida | A1 | A2 | B1 | B2 | Total |
|---|---:|---:|---:|---:|---:|
| Pares casi duplicados (R001 + R002) | 24 | 9 | 12 | 0 | 45 |
| Ejercicios con hallazgo (copias) | 23 | 9 | 12 | 0 | 44 |

Se compararon los 200.953 pares del mismo tema; los casi duplicados caen en 26 temas, y los 23 pares exactos quedan
aparte. Revisados a mano los 45 pares: 4 claramente no son («That is his car.» / «That car is his.», que contrasta
adjetivo y pronombre; «They have spent a lot of time in Paris.» / «They often visit Paris.»), 10 son dudosos, como
«Can I…» / «Could I…» en *Respuestas cortas*, y 31 son el mismo ejercicio. **El examen**: los 6 de `escribir/`, con los 9
exactos aparte. **Costo**: el modelo compara los 11.287 ejercicios en unos 4 minutos, en la máquina; después, sólo los
que cambian.

## Alcance

- **Adentro**: R001 a R007: el par candidato, las dos condiciones, el hallazgo en la copia, su ficha y el examen.
- **Afuera**: los iguales tal cual (FEAT-DUPEX); los parecidos entre temas ([DOUBT-DUPEX-ENTRE-TEMAS](#DOUBT-DUPEX-ENTRE-TEMAS));
  cómo se reemplaza la copia (paquete E); y escribir el curso al analizar ([F-HALL-R015](#F-HALL-R015)).

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
          - when: "El modelo local les da un parecido de 0,95 o más"
            then: marca
          - when: "Ningún par del tema cumple las condiciones"
            then: sin_hallazgo
      - id: marca
        action: "La copia lleva un hallazgo baja para la mesa, con los dos ejercicios y qué cambia; el primero del tema queda sin hallazgo"
        gate: [F-DUPAR-R002, F-DUPAR-R003, F-DUPAR-R004, F-DUPAR-R005]
        result: success
      - id: sin_hallazgo
        action: "Ningún ejercicio del tema tiene hallazgo de near-duplicates y todos quedan con puntaje 1"
        gate: [F-DUPAR-R006]
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

## References

- **FEAT-HALL** — Hallazgo, gravedad, conteo, identidad, ficha y plan: R003 a R006.
- **FEAT-DUPEX** — Ejercicio terminado, los exactos y cuál queda: R001, R003.
- **FEAT-MLOCAL** — El modelo local, su versión y qué pasa si falta: R006.
