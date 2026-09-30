---
feature:
  id: FEAT-MINIT
  code: F-MINIT
  name: Juez de miniteoría
  priority: high
---

# Juez de miniteoría

## TL;DR

**Qué**: Un analizador nuevo, `mini-theory`, juzga cada tema de una vez: lee sus miniteorías y todos sus ejercicios, y
dice si cada regla es cierta y si algún ejercicio del tema la contradice.

**Por qué**: El 077 encontró 335 ejercicios con una tarjeta falsa, en pocos temas, y nada la mira: el de consigna no lee
la miniteoría y dio «cumple» en los 112 de A1 y A2.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar miniteorías falsas:

1. Corre la auditoría → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible.
2. Busca quién lea la miniteoría → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Nadie: ningún analizador la recibe, tampoco el
   de consigna ([F-JEXAM-R007](#F-JEXAM-R007)).
3. Compara la regla con los ejercicios del tema → ⚠️ no hay juicio sobre un tema entero: los jueces leen un ejercicio.

**Punto de fricción**: pasos 2 y 3.

**Cambio mínimo necesario**: un analizador `mini-theory`, de la familia de errores ([F-HALL-R007](#F-HALL-R007)), que
evalúa el tema con una consulta, cuenta el error en los ejercicios que muestran la miniteoría con problema y usa lo común
de FEAT-JEXAM.

## Reglas de Negocio

<a id="F-MINIT-R001"></a>
### Rule[F-MINIT-R001] - Un juez por tema, que lee las miniteorías y el tema entero
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El juez evalúa el tema, no el ejercicio: hace una consulta por tema y lee el nivel, el nombre del tema, su consigna,
> cada miniteoría distinta con los ejercicios que la muestran, y cada ejercicio con su enunciado, su pista y sus
> respuestas aceptadas, o sus opciones con la correcta marcada. No lee [traducciones](glossary:Traducción), audios ni la
> teoría larga. La huella es todo eso: cambiar una miniteoría o un ejercicio deja al tema entero sin veredicto.

Son 413 temas: A1 83, A2 91, B1 147, B2 92. La miniteoría es de cada ejercicio: sólo 154 temas tienen una; 259 tienen dos
o más, hasta 50, y hay 1.488 distintas. La cascada es buscada, como la de la consigna en [F-QINST-R009](#F-QINST-R009).

<a id="F-MINIT-R002"></a>
### Rule[F-MINIT-R002] - Por cada miniteoría: si es cierta y si el tema la contradice
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Por cada miniteoría distinta del tema, el veredicto dice:
> 1. si la regla es cierta: no afirma como regla del inglés algo falso, ni un «siempre» o un «nunca» con excepciones
>    comunes;
> 2. si algún ejercicio del tema que la muestra la contradice, o queda fuera de lo que dice.
>
> Por cada problema da la gravedad y una evidencia que cumple [F-HALL-R003](#F-HALL-R003): el texto de la miniteoría, lo
> que tiene de falso con un contraejemplo, y los ejercicios involucrados. Que una miniteoría pida una forma no es un
> problema de este juez: lo marca el paquete A ([F-JEXAM-R012](#F-JEXAM-R012)).

<details><summary>Ejemplos de `escribir/miniteoria.json`</summary>

| Tema | Miniteoría | Qué encuentra |
|---|---|---|
| A1 · Contraste inicial (9 ej.) | «Be, want, enjoy y otros verbos de estado no llevan continuo» | falso: «Are you enjoying the party?» |
| A2 · Artículos: a, the o nada (19) | «Si después viene that o who, va the» | falso: «a friend who lives in Paris» |
| A2 · Gerundio obligatorio (9) | «stop … piden siempre -ing» | exagera: «They stopped to eat» |
| A1 · Sujeto u objeto (28) | «Antes del verbo va she. Después … va her» | cierta, pero la muestran ejercicios de he/him |

</details>

<a id="F-MINIT-R003"></a>
### Rule[F-MINIT-R003] - La miniteoría con problema cuenta como error sólo donde se muestra
**Severity**: critical | **Validation**: VALIDATED

> Cada miniteoría con problema da un hallazgo en cada ejercicio que la muestra, identificado por su texto
> ([F-HALL-R014](#F-HALL-R014)), y cuenta como error ahí, para `mini-theory` y para «algún error»
> ([F-HALL-R008](#F-HALL-R008)); en los ejercicios del tema que no la muestran, no. El puntaje de `mini-theory` está en
> cada ejercicio y sube por la agregación genérica.

José, 30/9 ([DOUBT-MINIT-VARIAS](#DOUBT-MINIT-VARIAS)): «cuenta sólo donde se muestra». Corrige su decisión anterior
(DOUBT-HALLAZGO-DE-TEMA de FEAT-HALL, «en cada ejercicio del tema»), que partió de un dato equivocado: hay 1.488
miniteorías, no una por tema. ⚠ Se confirma con José al volver. Contar «en todo el tema» queda como un parámetro del
analizador, no como otro diseño: con él, una miniteoría falsa cuenta en cada ejercicio del tema; un ejercicio que la
contradice o queda afuera cuenta siempre sólo donde se muestra. En «Contraste inicial» (A1, 80 ejercicios), la de «enjoy»
cuenta en 9, como contó el 077; con el parámetro, en 80.

<a id="F-MINIT-R004"></a>
### Rule[F-MINIT-R004] - Gravedad y puntaje de cada ejercicio
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La gravedad es **alta** si la regla es falsa o un ejercicio la contradice, **media** si exagera («siempre» o «nunca»
> donde hay excepciones comunes) y **baja** si es cierta pero no cubre el caso del ejercicio que la muestra. Nunca es
> bloqueante: la miniteoría se ve después de contestar. El puntaje de cada ejercicio es 1 sin hallazgo y, si no, 0,3, 0,6
> u 0,8 según el más grave [ASSUMPTION]; el del tema, el nivel y el curso sube por la agregación genérica.

Usa las definiciones de [F-HALL-R002](#F-HALL-R002). El 077 puso en A1 y A2 43 altas, 41 medias y 28 bajas.

<a id="F-MINIT-R005"></a>
### Rule[F-MINIT-R005] - Reuso, tope, fallas y cobertura, contando temas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el nombre `mini-theory`:
> 1. reusa el veredicto de cada tema por la huella, sin tope y sin consultar ([F-EVCOST-R003](#F-EVCOST-R003));
> 2. el tope cuenta temas: no juzga más temas nuevos por corrida que su tope;
> 3. una falla del servicio o una salida inutilizable no corta la corrida ni queda registrada ([F-EVCOST-R005](#F-EVCOST-R005),
>    [F-EVCOST-R009](#F-EVCOST-R009), [F-QINST-R007](#F-QINST-R007), [F-QINST-R013](#F-QINST-R013));
> 4. la cobertura declara temas alcanzados, evaluados y sin evaluar, y los ejercicios que contienen; los ejercicios de un
>    tema sin evaluar no tienen puntaje ni hallazgo de `mini-theory` y cuentan como sin evaluar
>    ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a tema, topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)). Tope por omisión: 0 temas ([F-JEXAM-R010](#F-JEXAM-R010)).

<a id="F-MINIT-R006"></a>
### Rule[F-MINIT-R006] - El examen de mini-theory
**Severity**: critical | **Validation**: ASSUMPTION

> Cada versión aprueba el examen fijo ([F-JEXAM-R001](#F-JEXAM-R001)) juzgando los temas de las verduras y de los 50
> buenos: no marca la miniteoría de las verduras, que es cierta, ni más de 2 de las que muestran los buenos. Sobre
> `escribir/miniteoria.json` la meta es encontrar 16 de sus 20 temas, y que 7 de cada 10 temas marcados fuera del oro,
> revisados a mano, sean reales.

`escribir/`: 112 ejercicios en 20 temas, A1 47 en 8 y A2 65 en 12. Las verduras aprueban acá: su problema es la pista y
la traducción, no la regla. 123 ejercicios comparten miniteoría con uno marcado sin tener fila (LEEME): se miran antes de
contarlos como marcas de más. Se mide con los 20 temas y 40 sin fila al azar: 60 consultas.

<a id="F-MINIT-R007"></a>
### Rule[F-MINIT-R007] - Se resuelve una vez por miniteoría
**Severity**: major | **Validation**: AUTO_VALIDATED

> Lo que marca se resuelve en la mesa con una reescritura de la miniteoría que llega a todos los ejercicios que la
> muestran: los ejercicios marcados por la misma miniteoría se resuelven juntos, con un solo candidato, y cada candidato lo
> juzga este mismo juez ([F-JEXAM-R005](#F-JEXAM-R005)).

Mantiene [F-HALL-R004](#F-HALL-R004): tarea sólo con puntaje menor que 1, que ahora está en los ejercicios que la
muestran; los hallazgos de una misma miniteoría comparten su texto como identidad, y así se agrupan. El tipo de tarea y la
mesa son del paquete E.

## Contexto

| Ficha | `mini-theory` |
|---|---|
| Pregunta | ¿La regla de la tarjeta es cierta, y ningún ejercicio del tema la contradice? |
| Lee | nivel, nombre y consigna del tema, sus miniteorías y sus ejercicios con sus respuestas u opciones |
| Reglas | la regla es falsa o exagera · un ejercicio la contradice o queda afuera |
| Meta y familia | 0 ejercicios con error · errores |
| Evalúa y resuelve | consulta por tema, cuenta en cada ejercicio que la muestra · mesa, una reescritura por miniteoría |
| Costo | modelo pago: una consulta por tema |

**Costo.** El curso entero son 413 consultas: A1 83, A2 91, B1 147, B2 92. Son más largas que las de un ejercicio (media
de 27 ejercicios por tema, hasta 170): a un minuto cada una, unas 7 horas de a una [ASSUMPTION]. Después de una tanda, una
consulta por tema tocado.

## Alcance

- **Adentro**: el analizador, qué lee del tema, qué decide, el hallazgo en cada ejercicio que muestra la miniteoría, el
  parámetro «en todo el tema», gravedad, puntaje, examen y ficha.
- **Afuera**: la reescritura y su tipo de tarea (E); la teoría larga de cada tema; lo común de los jueces (FEAT-JEXAM).

## User Journeys

### Journey[F-MINIT-J001] - Juzgar las miniteorías de un tema
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MINIT-J001
    name: Juzgar las miniteorías de un tema
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo mini-theory con un tope de temas, con una versión que aprobó su examen"
        gate: [F-MINIT-R001, F-MINIT-R005]
        outcomes:
          - when: "El tema ya tiene veredicto para ese contenido, o se lo consulta dentro del tope"
            then: leer
          - when: "No tiene veredicto y no queda tope"
            then: sin_evaluar
      - id: leer
        action: "El veredicto dice, por cada miniteoría, si es cierta y si algún ejercicio que la muestra la contradice"
        gate: [F-MINIT-R002]
        outcomes:
          - when: "Todas están bien"
            then: pasa
          - when: "Al menos una tiene problema"
            then: error
      - id: pasa
        action: "Ningún ejercicio del tema tiene hallazgo de mini-theory y el puntaje de cada uno es 1"
        result: success
      - id: error
        action: "Cada ejercicio que muestra una miniteoría con problema tiene su hallazgo, con gravedad y evidencia, y cuenta como error; los que no la muestran, no"
        gate: [F-MINIT-R003, F-MINIT-R004]
        result: success
      - id: sin_evaluar
        action: "El tema queda sin evaluar: sus ejercicios no tienen puntaje ni hallazgo de mini-theory, y la cobertura lo cuenta con sus ejercicios"
        gate: [F-MINIT-R005]
        result: success
```

## Open Questions

Proveedor (`claude-cli`), tope (0 sin pedido), orden (examen fijo, `escribir/` y A1) y consultas a la vez (4) se
decidieron una vez para los cinco jueces, en las dudas DOUBT-JUECES-* de
[FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR) (José, 30/9). Este es el más
barato: 413 consultas para el curso entero.

<a id="DOUBT-MINIT-VARIAS"></a>
### Doubt[DOUBT-MINIT-VARIAS] - Si el tema tiene varias miniteorías, ¿en qué ejercicios cuenta la que tiene problema?
- [ ] Opción A: en todos los del tema, como decía [F-MINIT-R003](#F-MINIT-R003) por la decisión del 30/9 a la mañana.
- [x] Opción B (recomendada): en los que la muestran; en los 154 temas de una sola miniteoría es lo mismo que A.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción B** (José, 30/9 a la tarde): «cuenta sólo donde se muestra». Corrige su decisión de la mañana, que
partió de un dato equivocado (hay 1.488 miniteorías, no una por tema). ⚠ Se confirma con José al volver; por eso A queda
como un parámetro del analizador y no como otro diseño ([F-MINIT-R003](#F-MINIT-R003)). En «Sujeto u objeto» (A1, 28
ejercicios) 16 muestran la de they/them y 12 la de she/her: con B, una tarjeta falsa cuenta donde se ve.

<a id="DOUBT-MINIT-TEMA-GRANDE"></a>
### Doubt[DOUBT-MINIT-TEMA-GRANDE] - ¿Los temas grandes se juzgan en una sola consulta?
- [x] Opción A (recomendada): sí, siempre una por tema, que es lo que permite comparar la regla con todo el tema.
- [ ] Opción B: partir por miniteoría los temas de 100 ejercicios o más (hoy son 4; el mayor tiene 170 y 18 miniteorías).

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción A** (José, 30/9, «por lo demás, continúa»; la presentación mostraba «una consulta, aunque tenga 170
ejercicios»). Se pasa a B sólo si el examen muestra que en los temas grandes se le escapan.

## References

- **FEAT-HALL** — Hallazgo, gravedad, conteos, identidad y tareas. Citado por R002 a R005 y R007. El hallazgo va en el
  ejercicio y no en el tema, así que la regla de F-HALL-R008 para hallazgos de tema no se aplica acá.
- **FEAT-JEXAM** — Examen, conjunto de temas, candidato de la mesa, consultas a la vez, tope 0, formas y dudas comunes.
  Citado por R002 y R005 a R007.
- **FEAT-EVCOST** — Huella, reuso y fallas. Citado por R005.
- **FEAT-QINST** — La cascada de la consigna, fallas y lo no evaluado. Citado por R001 y R005.
