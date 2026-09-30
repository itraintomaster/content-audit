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
evalúa el tema con una consulta y usa lo común de FEAT-JEXAM.

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
> que tiene de falso con un contraejemplo, y los ejercicios involucrados.

<details><summary>Ejemplos de `escribir/miniteoria.json`</summary>

| Tema | Miniteoría | Qué encuentra |
|---|---|---|
| A1 · Contraste inicial (9 ej.) | «Be, want, enjoy y otros verbos de estado no llevan continuo» | falso: «Are you enjoying the party?» |
| A2 · Artículos: a, the o nada (19) | «Si después viene that o who, va the» | falso: «a friend who lives in Paris» |
| A2 · Gerundio obligatorio (9) | «stop … piden siempre -ing» | exagera: «They stopped to eat» |
| A1 · Sujeto u objeto (28) | «Antes del verbo va she. Después … va her» | cierta, pero la muestran ejercicios de he/him |

</details>

<a id="F-MINIT-R003"></a>
### Rule[F-MINIT-R003] - El hallazgo es del tema y cuenta en cada ejercicio del tema
**Severity**: critical | **Validation**: VALIDATED

> Cada miniteoría con problema da un hallazgo en el tema, identificado por su texto ([F-HALL-R014](#F-HALL-R014)). El
> puntaje de `mini-theory` está en el tema y sube por la agregación genérica; el hallazgo cuenta como error en cada
> ejercicio del tema, para `mini-theory` y para «algún error» ([F-HALL-R008](#F-HALL-R008)).

Es la decisión de José del 30/9 sobre DOUBT-HALLAZGO-DE-TEMA. Se tomó con la premisa de que la miniteoría es una por tema,
que vale en 154 de 413: [DOUBT-MINIT-VARIAS](#DOUBT-MINIT-VARIAS).

<a id="F-MINIT-R004"></a>
### Rule[F-MINIT-R004] - Gravedad y puntaje del tema
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La gravedad es **alta** si la regla es falsa o un ejercicio la contradice, **media** si exagera («siempre» o «nunca»
> donde hay excepciones comunes) y **baja** si es cierta pero no cubre el caso del ejercicio que la muestra. Nunca es
> bloqueante: la miniteoría se ve después de contestar. El puntaje del tema es 1 sin errores y, si no, 0,3, 0,6 u 0,8
> según la más grave [ASSUMPTION].

Usa las definiciones de [F-HALL-R002](#F-HALL-R002). El 077 puso en A1 y A2 43 altas, 41 medias y 28 bajas.

<a id="F-MINIT-R005"></a>
### Rule[F-MINIT-R005] - Reuso, tope, fallas y cobertura, contando temas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el nombre `mini-theory`:
> 1. reusa el veredicto de cada tema por la huella, sin tope y sin consultar ([F-EVCOST-R003](#F-EVCOST-R003));
> 2. el tope cuenta temas: no juzga más temas nuevos por corrida que su tope;
> 3. una falla del servicio o una salida inutilizable no corta la corrida ni queda registrada ([F-EVCOST-R005](#F-EVCOST-R005),
>    [F-EVCOST-R009](#F-EVCOST-R009), [F-QINST-R007](#F-QINST-R007), [F-QINST-R013](#F-QINST-R013));
> 4. la cobertura declara temas alcanzados, evaluados y sin evaluar, y los ejercicios que contienen; un tema sin evaluar
>    no tiene puntaje ni hallazgo ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)).

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
### Rule[F-MINIT-R007] - Se resuelve una vez por tema
**Severity**: major | **Validation**: AUTO_VALIDATED

> Lo que marca se resuelve en la mesa con una reescritura de la miniteoría que llega a todos los ejercicios que la
> muestran; como el puntaje menor que 1 está en el tema, su tarea es del tema y no de cada ejercicio, y cada candidato lo
> juzga este mismo juez ([F-JEXAM-R005](#F-JEXAM-R005)).

Mantiene [F-HALL-R004](#F-HALL-R004): tarea sólo con puntaje menor que 1. El tipo de tarea y la mesa son del paquete E.

## Contexto

| Ficha | `mini-theory` |
|---|---|
| Pregunta | ¿La regla de la tarjeta es cierta, y ningún ejercicio del tema la contradice? |
| Lee | nivel, nombre y consigna del tema, sus miniteorías y sus ejercicios con sus respuestas u opciones |
| Reglas | la regla es falsa o exagera · un ejercicio la contradice o queda afuera |
| Meta y familia | 0 ejercicios con error · errores |
| Evalúa y resuelve | tema · mesa, una reescritura por miniteoría |
| Costo | modelo pago: una consulta por tema |

**Costo.** El curso entero son 413 consultas: A1 83, A2 91, B1 147, B2 92. Son más largas que las de un ejercicio (media
de 27 ejercicios por tema, hasta 170): a un minuto cada una, unas 7 horas de a una [ASSUMPTION]. Después de una tanda, una
consulta por tema tocado.

## Alcance

- **Adentro**: el analizador, qué lee del tema, qué decide, el hallazgo del tema, gravedad, puntaje, examen y ficha.
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
        action: "El tema no tiene hallazgo de mini-theory y su puntaje es 1"
        result: success
      - id: error
        action: "El tema tiene un hallazgo por miniteoría con problema, con gravedad y evidencia, y cuenta como error en cada ejercicio del tema"
        gate: [F-MINIT-R003, F-MINIT-R004]
        result: success
      - id: sin_evaluar
        action: "El tema queda sin evaluar, sin puntaje ni hallazgo, y la cobertura lo cuenta con sus ejercicios"
        gate: [F-MINIT-R005]
        result: success
```

## Open Questions

Proveedor, tope, curso entero o muestra, y consultas a la vez se deciden una vez para los cinco jueces, en las dudas
DOUBT-JUECES-* de [FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR). Este es el
más barato: 413 consultas para el curso entero.

<a id="DOUBT-MINIT-VARIAS"></a>
### Doubt[DOUBT-MINIT-VARIAS] - Si el tema tiene varias miniteorías, ¿en qué ejercicios cuenta la que tiene problema?
- [ ] Opción A: en todos los del tema, como dice hoy [F-MINIT-R003](#F-MINIT-R003) por la decisión del 30/9.
- [ ] Opción B (recomendada): en los que la muestran; en los 154 temas de una sola miniteoría es lo mismo que A.

**Answer**: Pendiente. Recomiendo B. La decisión se tomó con la razón de que «la ven todos sus ejercicios», que no se
cumple en 259 temas: en «Sujeto u objeto» (A1, 28 ejercicios) 16 muestran la de they/them y 12 la de she/her. Con A, una
tarjeta falsa en 12 ejercicios de un tema de 170 contaría 170. Cambiar la decisión es tuyo.

<a id="DOUBT-MINIT-TEMA-GRANDE"></a>
### Doubt[DOUBT-MINIT-TEMA-GRANDE] - ¿Los temas grandes se juzgan en una sola consulta?
- [ ] Opción A (recomendada): sí, siempre una por tema, que es lo que permite comparar la regla con todo el tema.
- [ ] Opción B: partir por miniteoría los temas de 100 ejercicios o más (hoy son 4; el mayor tiene 170 y 18 miniteorías).

**Answer**: Pendiente. Recomiendo A, y pasar a B sólo si el examen muestra que en los temas grandes se le escapan.

## References

- **FEAT-HALL** — Hallazgo de tema, gravedad, conteos, identidad y tareas. Citado por R002 a R005 y R007.
- **FEAT-JEXAM** — Examen, conjunto de temas, candidato de la mesa, consultas a la vez y dudas comunes. Citado por R005 a R007.
- **FEAT-EVCOST** — Huella, reuso y fallas. Citado por R005.
- **FEAT-QINST** — La cascada de la consigna, fallas y lo no evaluado. Citado por R001 y R005.
