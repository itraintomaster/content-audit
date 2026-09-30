---
feature:
  id: FEAT-UNICA
  code: F-UNICA
  name: Juez de una sola respuesta
  priority: high
---

# Juez de una sola respuesta

## TL;DR

**Qué**: Un analizador nuevo, `single-answer`, lista con lo que el alumno ve antes de contestar las respuestas que
también valdrían y el corrector no acepta: la misma en otra forma la deriva a respuestas completas; la distinta, la marca.

**Por qué**: El 077 encontró 476 ejercicios con otra respuesta igual de buena, y el juez de consigna, que revisa la
resolubilidad, dio «cumple» en los 179 de A1 y A2.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar ejercicios que marcan mal una respuesta correcta:

1. Corre la auditoría → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible.
2. Busca quién lista las otras respuestas válidas → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Nadie: el de consigna mira
   la resolubilidad ([F-QINST-R014](#F-QINST-R014)) pero no lista alternativas, y en A1 y A2 no marcó ninguna.
3. Juzga si un ejercicio es resoluble → `AssessCandidateCommand.assessCandidate(...)`. ⚠️ Sólo sobre un candidato de una
   tarea de consigna, comparado con el original ([F-QICOR-R004](#F-QICOR-R004), criterio 5).

**Punto de fricción**: pasos 2 y 3.

**Cambio mínimo necesario**: un analizador `single-answer`, de la familia de errores ([F-HALL-R007](#F-HALL-R007)), que
juzga cada CLOZE en modo absoluto, separa la otra forma de la otra respuesta y usa lo común de FEAT-JEXAM.

## Reglas de Negocio

<a id="F-UNICA-R001"></a>
### Rule[F-UNICA-R001] - Lee lo que el alumno ve antes de contestar
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Por cada ejercicio CLOZE, el juez lee el nombre del tema, la consigna que ve el alumno, el enunciado con su pista y las
> respuestas aceptadas. No lee la [traducción](glossary:Traducción), la miniteoría ni el audio, que la app muestra recién
> al corregir. La huella es exactamente eso. Alcanza los 8.477 CLOZE; los 2.810 de opción múltiple son de FEAT-MCVALE
> [ASSUMPTION].

Por nivel: A1 1.692, A2 1.551, B1 3.554, B2 1.680. El nombre del tema no está en la ficha de la propuesta, pero la app lo
muestra arriba del ejercicio [ASSUMPTION]. La opción múltiple: [DOUBT-UNICA-MC](#DOUBT-UNICA-MC).

<a id="F-UNICA-R002"></a>
### Rule[F-UNICA-R002] - Lista las respuestas que también valdrían
**Severity**: critical | **Validation**: AUTO_VALIDATED

> El veredicto lista toda respuesta no aceptada que un alumno podría escribir con lo que ve y que es correcta en ese
> contexto, y clasifica cada una como:
> 1. **la misma en otra forma**: difiere sólo en contracción o forma larga, en una variante ortográfica, en mayúscula,
>    puntuación o espacios, o en una alternativa que la consigna misma ofrece;
> 2. **distinta y válida**: cualquier otra.
>
> Con la lista vacía, el ejercicio pasa.

La frontera de la primera clase se fija con el paquete A, que imita al corrector: [DOUBT-UNICA-FORMA](#DOUBT-UNICA-FORMA).

<a id="F-UNICA-R003"></a>
### Rule[F-UNICA-R003] - La misma en otra forma se deriva a respuestas completas
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una respuesta de la primera clase no cuenta como error de `single-answer`: da un hallazgo que sólo ordena, «la misma
> respuesta en otra forma», cuya evidencia nombra la respuesta y al analizador de respuestas completas (paquete A) como
> quien la arregla con una regla. Si todas son de esa clase, el ejercicio no suma error por este analizador.

Evita contar dos veces lo que respuestas completas cuenta, y si ese analizador no marcó el ejercicio, este hallazgo lo deja
ver. Ejemplo (`…2464`): «He was in Italy ___ .» acepta «from Friday to Sunday» y rechaza «from Friday until Sunday», que la
consigna ofrece («to / until / till»).

<a id="F-UNICA-R004"></a>
### Rule[F-UNICA-R004] - La distinta y válida se marca, con su gravedad
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Una respuesta de la segunda clase cuenta como error y se resuelve en la mesa. La gravedad es:
> 1. **bloqueante** si con lo que ve el alumno no se puede saber cuál acepta el corrector: varias igual de buenas y ninguna
>    pista que elija;
> 2. **alta** si rechaza otra igual de buena;
> 3. **media** si rechaza otra correcta, pero menos esperable.
>
> El puntaje es 1 sin errores y, si no, 0, 0,3 o 0,6 según la más grave [ASSUMPTION].

<details><summary>Ejemplos del curso (`67fab6d5…`)</summary>

| Gravedad | Ejercicio | Qué lista |
|---|---|---|
| bloqueante | «I ___ a lot of vegetables.», acepta «love» (`…1ecd`) | eat, like, buy y want encajan igual, y no hay pista |
| alta | «Does he need water? He ___ .», acepta «does» (`…1f57`) | «doesn't» vale igual: sólo la traducción, que se ve después, dice cuál |
| media | «She ___ (not / come).», acepta «isn't going to come» (`…2189`) | «won't come» también es natural |

Usa las definiciones de [F-HALL-R002](#F-HALL-R002). El 077 puso en los 86 CLOZE de A1 y A2 46 altas y 40 medias.

</details>

<a id="F-UNICA-R005"></a>
### Rule[F-UNICA-R005] - Reuso, tope, fallas y cobertura, como el juez de consigna
**Severity**: critical | **Validation**: AUTO_VALIDATED

> Con el nombre `single-answer`:
> 1. reusa el veredicto por la huella, sin tope y sin consultar ([F-EVCOST-R003](#F-EVCOST-R003));
> 2. no hace más consultas nuevas por corrida que su tope;
> 3. una falla del servicio o una salida inutilizable no corta la corrida ni queda registrada ([F-EVCOST-R005](#F-EVCOST-R005),
>    [F-EVCOST-R009](#F-EVCOST-R009), [F-QINST-R007](#F-QINST-R007), [F-QINST-R013](#F-QINST-R013));
> 4. la cobertura declara alcanzados, evaluados y sin evaluar ([F-HALL-R008](#F-HALL-R008)), y lo sin evaluar no tiene
>    puntaje ni hallazgo ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a tema, topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)).

<a id="F-UNICA-R006"></a>
### Rule[F-UNICA-R006] - El examen de single-answer
**Severity**: critical | **Validation**: ASSUMPTION

> Cada versión aprueba el examen fijo ([F-JEXAM-R001](#F-JEXAM-R001)) si reprueba las verduras y no reprueba más de 2 de
> los 38 buenos CLOZE; el grupo b se informa sin contar. Sobre `escribir/unica.json` la meta es encontrar 60 de sus 86
> casos CLOZE, y que 7 de cada 10 de 50 marcas fuera del oro, revisadas a mano, sean reales.

`escribir/unica.json` tiene 179 casos de A1 (82) y A2 (97): 86 CLOZE (34 y 52) y 93 de opción múltiple, que rinde
FEAT-MCVALE. Se mide juzgando los 86 y 300 CLOZE sin fila al azar: 386 consultas; el examen fijo, 39. Del grupo b, los
imposibles por pista faltante o alternativa cortada deberían salir marcados: se informa cuántos.

<a id="F-UNICA-R007"></a>
### Rule[F-UNICA-R007] - Se arregla con una regla o en la mesa, y la mesa juzga con este juez
**Severity**: major | **Validation**: AUTO_VALIDATED

> La otra forma se arregla con la regla de respuestas completas, agregándola con «|». La distinta va a la mesa, y cada
> candidato —que la agrega como aceptada, pone una pista o cambia la oración para que quede una sola— lo juzga este juez
> ([F-JEXAM-R005](#F-JEXAM-R005)): pasa si su lista queda vacía o sólo con otras formas.

Ejemplo de la propuesta: «Does he need water? He ___ .» se arregla con «Yes, he ___». Cómo propone y decide la mesa es del
paquete E.

## Contexto

| Ficha | `single-answer` |
|---|---|
| Pregunta | Con lo que ve el alumno, ¿hay otra respuesta igual de buena que el corrector no acepta? |
| Lee | nombre del tema, consigna, enunciado con pista, respuestas aceptadas |
| Reglas | otra respuesta distinta vale · la misma respuesta en otra forma (sólo ordena) |
| Meta y familia | 0 ejercicios con error · errores |
| Evalúa y resuelve | ejercicio CLOZE · mesa, o regla de respuestas completas |
| Costo | modelo pago: una consulta por ejercicio |

**Costo.** El curso entero son 8.477 consultas: A1 1.692, A2 1.551, B1 3.554, B2 1.680. Al ritmo medido del juez de
consigna, 45 horas de a una, unas 11 con 4 a la vez. Después de una tanda, una consulta por CLOZE cuyo enunciado, pista,
consigna, respuestas o nombre de tema cambió.

## Alcance

- **Adentro**: el analizador, la lista de alternativas y su clase, la derivación, gravedad, puntaje, examen y ficha.
- **Afuera**: la regla que agrega la otra forma y el imitador del corrector (A); la opción múltiple (FEAT-MCVALE); la mesa
  (E); lo común de los jueces (FEAT-JEXAM).

## User Journeys

### Journey[F-UNICA-J001] - Buscar otra respuesta que también valga
**Validation**: AUTO_VALIDATED

```yaml
journeys:
  - id: F-UNICA-J001
    name: Buscar otra respuesta que también valga
    flow:
      - id: correr
        action: "El operador corre analyze pidiendo single-answer con un tope, con una versión que aprobó su examen"
        gate: [F-UNICA-R001]
        outcomes:
          - when: "El ejercicio ya tiene veredicto para ese contenido, o se lo consulta dentro del tope"
            then: leer_lista
          - when: "No tiene veredicto y no queda tope"
            then: sin_evaluar
      - id: leer_lista
        action: "El veredicto lista las otras respuestas válidas, cada una con su clase"
        gate: [F-UNICA-R002]
        outcomes:
          - when: "La lista está vacía"
            then: pasa
          - when: "Sólo hay la misma respuesta en otra forma"
            then: derivada
          - when: "Hay al menos una distinta y válida"
            then: error
      - id: pasa
        action: "El ejercicio no tiene hallazgo de single-answer y su puntaje es 1"
        result: success
      - id: derivada
        action: "El ejercicio queda marcado para respuestas completas, sin contar como error de single-answer"
        gate: [F-UNICA-R003]
        result: success
      - id: error
        action: "El ejercicio tiene su hallazgo con gravedad y evidencia, y cuenta como error en su tema, nivel y curso"
        gate: [F-UNICA-R004]
        result: success
      - id: sin_evaluar
        action: "El ejercicio queda sin evaluar, sin puntaje ni hallazgo, y la cobertura lo cuenta"
        gate: [F-UNICA-R005]
        result: success
```

## Open Questions

Proveedor, tope, curso entero o muestra, y consultas a la vez se deciden una vez para los cinco jueces, en las dudas
DOUBT-JUECES-* de [FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR). El choque
con la vara comparativa de «resoluble» (criterio 5 de F-QICOR-R004) es el mismo de [DOUBT-TRSEN-CATALOGO](../2026-09-30.08_traduccion-y-sentido/REQUIREMENT.md#DOUBT-TRSEN-CATALOGO).

<a id="DOUBT-UNICA-MC"></a>
### Doubt[DOUBT-UNICA-MC] - ¿Una sola respuesta mira también la opción múltiple?
- [ ] Opción A (recomendada): no. En opción múltiple la pregunta es «¿otra opción también vale?» y la hace FEAT-MCVALE:
  una consulta por ejercicio y un solo conteo.
- [ ] Opción B: sí, como dice la ficha de la propuesta («lee las opciones»): 2.810 consultas más y el mismo defecto
  contado por dos analizadores.

**Answer**: Pendiente. Recomiendo A. Los 93 casos de opción múltiple de `escribir/unica.json` pasan al examen de FEAT-MCVALE.

<a id="DOUBT-UNICA-FORMA"></a>
### Doubt[DOUBT-UNICA-FORMA] - ¿Qué cuenta como «la misma respuesta en otra forma»?
- [ ] Opción A (recomendada): contracción o forma larga, variante ortográfica, mayúscula, puntuación o espacios, y las
  alternativas que la consigna ofrece: lo que una regla puede agregar con «|», acordado con el paquete A.
- [ ] Opción B: además sinónimos y paráfrasis: lo que hoy es «distinta y válida» pasa a derivarse y deja de contarse acá.

**Answer**: Pendiente. Recomiendo A: separa lo que arregla una regla de lo que necesita la mesa.

## References

- **FEAT-HALL** — Familia de errores, hallazgo que sólo ordena, gravedad y conteos. Citado por R002 a R005.
- **FEAT-JEXAM** — Examen, conjunto, candidato de la mesa, consultas a la vez y dudas comunes. Citado por R005 a R007.
- **FEAT-EVCOST** — Huella, reuso y fallas. Citado por R005.
- **FEAT-QINST** — Resolubilidad que el juez de consigna ya mira; fallas y lo no evaluado. Citado por R005.
- **FEAT-QICOR** — «Resoluble y del mismo tipo» en modo comparativo. Citado en el recorrido.
- **FEAT-TRSEN, FEAT-MCVALE** — La duda del catálogo y la opción múltiple.
