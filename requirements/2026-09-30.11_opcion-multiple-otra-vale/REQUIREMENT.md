---
feature:
  id: FEAT-MCVALE
  code: F-MCVALE
  name: Juez de opción múltiple, ¿otra opción también vale?
  priority: high
---

# Juez de opción múltiple: ¿otra opción también vale?

## TL;DR

**Qué**: Una regla con modelo del analizador de opción múltiple válida lee cada ejercicio de opción múltiple como lo ve
el alumno y dice si alguna opción marcada como incorrecta también vale en ese contexto, o si la correcta no vale.

**Por qué**: En A1 y A2 el 077 encontró 93 ejercicios de opción múltiple con dos opciones correctas y una sola aceptada;
las reglas de forma no los ven y el juez de consigna los dio por buenos.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar opciones múltiples con dos respuestas válidas:

1. Corre la auditoría → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible: los 2.810 se miden desde el
   brief 022.
2. Busca quién juzgue las opciones → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Nadie: el de consigna ya ve las opciones con
   la correcta marcada, pero pregunta por la consigna, no por las otras opciones.
3. Revisa la forma de las opciones → ⚠️ el analizador de opción múltiple válida (paquete A) se está diseñando: sus reglas
   miran mayúsculas y formas, no el sentido.

**Punto de fricción**: pasos 2 y 3: nada decide si una incorrecta también vale.

**Cambio mínimo necesario**: una regla con modelo dentro del analizador de opción múltiple válida, de la familia de
errores ([F-HALL-R007](#F-HALL-R007)), que usa lo común de FEAT-JEXAM.

## Reglas de Negocio

<a id="F-MCVALE-R001"></a>
### Rule[F-MCVALE-R001] - Lee cada opción múltiple como la ve el alumno
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Por cada ejercicio de opción múltiple, el juez lee el nombre del tema, la consigna que ve el alumno, el enunciado con su
> pista y todas las opciones en el orden en que se muestran, con la correcta marcada. No lee la
> [traducción](glossary:Traducción), la miniteoría ni el audio. La huella es exactamente eso.

Alcanza los 2.810: A1 710, A2 860, B1 1.000, B2 240. Mantiene [F-EVCOST-R001](#F-EVCOST-R001) y
[F-EVCOST-R002](#F-EVCOST-R002). Los CLOZE son de FEAT-UNICA.

<a id="F-MCVALE-R002"></a>
### Rule[F-MCVALE-R002] - Qué decide: si otra opción también vale, y si la correcta vale
**Severity**: critical | **Validation**: AUTO_VALIDATED

> En el contexto del enunciado, el veredicto dice qué opciones marcadas como incorrectas también valen y si la marcada
> como correcta vale. Una opción que difiere de la correcta sólo en la forma —mayúscula, a o an, singular o plural— no
> cuenta acá: la juzgan las reglas del paquete A.

<details><summary>Ejemplos de `escribir/unica.json` (`67fab6d5…`)</summary>

| Ejercicio | Opciones (correcta con *) | Qué encuentra |
|---|---|---|
| «She wants ___ hot tea.» (`…232e`) | a / *some | «a hot tea» también vale: una taza de té |
| «There's a cat ___ the window.» (`…2cb8`) | *at / in / on | «in the window» es igual de natural |
| «I don't enjoy extra food ___ .» (`…2f1b`) | a lot / a lot of / *much / really | «a lot» al final también es correcto |
| «Are you thirsty? ___ , let's drink water soon.» (`…47cc`, examen fijo) | *if not / if so | la correcta no vale: vale «if so» |

</details>

<a id="F-MCVALE-R003"></a>
### Rule[F-MCVALE-R003] - Gravedad y puntaje
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Todo lo que encuentra cuenta como error y se resuelve en la mesa. La gravedad es **bloqueante** si la correcta no vale y
> otra sí, porque el alumno no puede acertar; **alta** si otra opción vale igual; **media** si otra es defendible pero
> menos natural. El puntaje es 1 sin errores y, si no, 0, 0,3 o 0,6 según la más grave [ASSUMPTION].

Usa las definiciones de [F-HALL-R002](#F-HALL-R002). El 077 puso en los 93 de A1 y A2 48 altas y 45 medias.

<a id="F-MCVALE-R004"></a>
### Rule[F-MCVALE-R004] - Es una regla del analizador de opción múltiple válida
**Severity**: major | **Validation**: ASSUMPTION

> El juez es una regla con modelo del analizador de opción múltiple válida (paquete A): sus hallazgos nombran ese
> analizador y la regla «otra opción también vale», con costo modelo pago; el analizador tiene una sola ficha y un solo
> nombre para correrlo, excluirlo y fijarle tope.

Es como lo dibuja la propuesta («Analizador 3 · regla y juez») y el ejemplo de [F-HALL-R002](#F-HALL-R002): costo por
hallazgo, y el de la ficha es el mayor ([F-HALL-R006](#F-HALL-R006)). Con tope 0 por omisión, correr el analizador sólo
aplica sus reglas y reusa lo juzgado. Alternativa: [DOUBT-MCVALE-ANALIZADOR](#DOUBT-MCVALE-ANALIZADOR).

<a id="F-MCVALE-R005"></a>
### Rule[F-MCVALE-R005] - Reuso, tope, fallas y cobertura, como el juez de consigna
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el nombre del analizador:
> 1. reusa el veredicto por la huella, sin tope y sin consultar ([F-EVCOST-R003](#F-EVCOST-R003));
> 2. no hace más consultas nuevas por corrida que su tope;
> 3. una falla del servicio o una salida inutilizable no corta la corrida ni queda registrada ([F-EVCOST-R005](#F-EVCOST-R005),
>    [F-EVCOST-R009](#F-EVCOST-R009), [F-QINST-R007](#F-QINST-R007), [F-QINST-R013](#F-QINST-R013));
> 4. la cobertura del juez declara alcanzados (los 2.810), evaluados y sin evaluar ([F-HALL-R008](#F-HALL-R008)), y lo
>    sin evaluar no tiene puntaje ni hallazgo de esta regla ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a tema, topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)).

<a id="F-MCVALE-R006"></a>
### Rule[F-MCVALE-R006] - El examen del juez de opción múltiple
**Severity**: critical | **Validation**: ASSUMPTION

> Cada versión aprueba el examen fijo ([F-JEXAM-R001](#F-JEXAM-R001)) si reprueba el imposible de opción múltiple del
> grupo b (`67fab6d599301022953447cc`) y no reprueba más de 1 de los 12 buenos de opción múltiple; las verduras no le
> tocan. Sobre `escribir/` la meta es encontrar 65 de los 93 casos de opción múltiple de `unica.json`, y que 7 de cada 10
> de 50 marcas fuera del oro, revisadas a mano, sean reales.

Los 93 son de A1 (48) y A2 (45). De los 186 de `escribir/opcion.json`, que son de forma (170 de mayúsculas), se informa
cuántos marca y se miran a mano: alguno puede tener además otra opción válida. Se mide con los 93 y 100 sin fila al
azar: 193 consultas; el examen fijo, 13.

<a id="F-MCVALE-R007"></a>
### Rule[F-MCVALE-R007] - Lo que marca se resuelve en la mesa, con este mismo juez
**Severity**: major | **Validation**: AUTO_VALIDATED

> Cuando hay dos correctas, o la correcta no vale, la mesa cambia la oración o la opción que sobra, y cada candidato lo
> juzga este mismo juez ([F-JEXAM-R005](#F-JEXAM-R005)): pasa si ninguna incorrecta vale y la correcta sí.

Es el «cómo se resuelve» de la propuesta. Cómo propone y decide la mesa es del paquete E; las mayúsculas se arreglan con
una regla del paquete A.

## Contexto

| Ficha de la regla | otra opción también vale |
|---|---|
| Pregunta | En opción múltiple, ¿alguna incorrecta también vale en ese contexto? |
| Lee | nombre del tema, consigna, enunciado con pista, opciones en orden con la correcta marcada |
| Meta y familia | 0 ejercicios con error · errores |
| Evalúa y resuelve | ejercicio de opción múltiple · mesa |
| Costo | modelo pago: una consulta por ejercicio |

**Costo.** El curso entero son 2.810 consultas: A1 710, A2 860, B1 1.000, B2 240. Al ritmo medido del juez de consigna,
15 horas de a una, unas 4 con 4 a la vez. Después de una tanda, una consulta por ejercicio cuyo enunciado, opciones,
consigna o nombre de tema cambió.

## Alcance

- **Adentro**: la regla con modelo, qué lee, qué decide, gravedad, puntaje, examen y su lugar en el analizador.
- **Afuera**: las reglas de forma y el nombre del analizador (paquete A); la mesa (E); los CLOZE (FEAT-UNICA); lo común de
  los jueces (FEAT-JEXAM).

## User Journeys

### Journey[F-MCVALE-J001] - Juzgar si otra opción también vale
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-MCVALE-J001
    name: Juzgar si otra opción también vale
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo el analizador de opción múltiple válida con un tope, con una versión del juez que aprobó su examen"
        gate: [F-MCVALE-R001, F-MCVALE-R004]
        outcomes:
          - when: "El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope"
            then: leer
          - when: "No tiene veredicto y no queda tope"
            then: sin_evaluar
      - id: leer
        action: "El veredicto dice qué incorrectas también valen y si la correcta vale"
        gate: [F-MCVALE-R002]
        outcomes:
          - when: "Sólo vale la correcta"
            then: pasa
          - when: "Otra opción también vale"
            then: error
          - when: "La correcta no vale y otra sí"
            then: bloqueante
      - id: pasa
        action: "El ejercicio no tiene hallazgo de esta regla"
        result: success
      - id: error
        action: "El ejercicio tiene su hallazgo, alta o media, con evidencia, y cuenta como error en su tema, nivel y curso"
        gate: [F-MCVALE-R003]
        result: success
      - id: bloqueante
        action: "El ejercicio tiene su hallazgo bloqueante, con puntaje 0, y cuenta como error"
        gate: [F-MCVALE-R003]
        result: success
      - id: sin_evaluar
        action: "El ejercicio queda sin evaluar por esta regla, sin puntaje ni hallazgo del juez, y la cobertura lo cuenta"
        gate: [F-MCVALE-R005]
        result: success
```

## Open Questions

Proveedor, tope, curso entero o muestra, y consultas a la vez se deciden una vez para los cinco jueces, en las dudas
DOUBT-JUECES-* de [FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR). Que la
opción múltiple no la juzgue también una sola respuesta es [DOUBT-UNICA-MC](../2026-09-30.09_una-sola-respuesta/REQUIREMENT.md#DOUBT-UNICA-MC).

<a id="DOUBT-MCVALE-ANALIZADOR"></a>
### Doubt[DOUBT-MCVALE-ANALIZADOR] - ¿Un analizador de opción múltiple con reglas y juez, o dos analizadores?
- [ ] Opción A (recomendada): uno, el de opción múltiple válida del paquete A, con el juez como una de sus reglas: una
  fila y una ficha, como dibuja la propuesta.
- [ ] Opción B: un analizador aparte, `choice-alternative`, con su ficha: la opción múltiple ocupa dos filas, y el paquete
  A no depende de este.

**Answer**: Pendiente. Recomiendo A. Hay que acordar con el analista del paquete A el nombre y que su ficha declare costo
modelo pago; si eso traba la ronda, B no cambia ninguna otra regla de este requirement.

## References

- **FEAT-HALL** — Familia de errores, gravedad y costo por hallazgo, ficha y conteos. Citado por R003 a R005.
- **FEAT-JEXAM** — Examen, conjunto, candidato de la mesa, consultas a la vez y dudas comunes. Citado por R005 a R007.
- **FEAT-EVCOST** — Huella, reuso y fallas. Citado por R001 y R005.
- **FEAT-QINST** — Fallas y lo no evaluado, como en el juez de consigna. Citado por R005.
- **FEAT-UNICA** — Los CLOZE y la duda de quién juzga la opción múltiple.
