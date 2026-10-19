---
feature:
  id: FEAT-MINIT
  code: F-MINIT
  name: Juez de miniteoría
  priority: high
---

# Juez de miniteoría

## TL;DR

**Qué**: `mini-theory` juzga la miniteoría propia de cada quiz contra el caso concreto que ese quiz enseña; puede leer
varios quizzes juntos, pero nunca convierte sus tarjetas en una explicación global del tema.

**Por qué**: El 077 encontró 335 ejercicios con una tarjeta falsa, en pocos temas, y nada la mira: el de consigna no lee
la miniteoría y dio «cumple» en los 112 de A1 y A2.

## Recorrido de uso (estado actual)

Cómo quien revisa el curso intenta hoy encontrar miniteorías falsas:

1. Corre la auditoría → `AnalyzeCommand.analyze(coursePath, AnalyzeOptions)`. ✓ Disponible.
2. Busca quién lea la miniteoría → `AnalyzerRegistry.listAnalyzers()`. ⚠️ Nadie: ningún analizador la recibe, tampoco el
   de consigna ([F-JEXAM-R007](#F-JEXAM-R007)).
3. Busca un juicio independiente de la tarjeta contra el caso de cada quiz → ⚠️ no existe: el diseño actual agrupa por
   texto de miniteoría y lo contrasta con el tema.

**Punto de fricción**: pasos 2 y 3.

**Cambio mínimo necesario**: un analizador `mini-theory`, de la familia de errores ([F-HALL-R007](#F-HALL-R007)), que
puede enviar el tema en una consulta para ahorrar costo, pero produce y corrige un veredicto independiente por quiz,
usando lo común de FEAT-JEXAM.

## Reglas de Negocio

<a id="F-MINIT-R001"></a>
### Rule[F-MINIT-R001] - La unidad juzgada es el quiz, aunque la consulta agrupe el tema
**Severity**: critical | **Validation**: VALIDATED

> El juez produce un veredicto independiente para la miniteoría de cada quiz. Puede hacer una sola consulta que reúna los
> quizzes de un tema, pero ese agrupamiento sólo ahorra costo: no vuelve temática ni global a la miniteoría. Para cada
> quiz lee su propia miniteoría, su enunciado, su pista y sus respuestas aceptadas, o sus opciones con la correcta marcada,
> además del nivel, nombre y consigna del tema como contexto. No lee [traducciones](glossary:Traducción), audios ni la
> teoría larga.

Dos quizzes que muestran el mismo texto siguen siendo dos casos juzgados: cada aparición se interpreta según lo que pide
y responde ese quiz. La consulta puede seguir siendo una por tema; la huella incluye todo el contenido enviado, por lo que
cambiar una miniteoría o un quiz deja ese lote sin veredicto. Son 413 temas y 1.488 miniteorías en el curso.

<a id="F-MINIT-R002"></a>
### Rule[F-MINIT-R002] - Cada miniteoría explica sólo el caso concreto de su quiz
**Severity**: critical | **Validation**: VALIDATED

> Para cada quiz, el veredicto decide si su miniteoría es una corrección cierta, suficiente y específica para la forma,
> función y palabras de ese caso. Si un tema mezcla afirmación, negación, pregunta u otros subtemas, la tarjeta de un quiz de pregunta
> debe explicar sólo el caso de pregunta: no tiene que cubrir afirmación, negación ni los demás quizzes del tema, y no debe
> introducir contenido que pertenece únicamente a esos otros casos.
>
> La miniteoría falla si enseña algo falso para su quiz, si no explica la decisión que corrige ese caso o si enseña
> como parte de ese caso una regla que corresponde a otro subtema. No falla por omitir variantes que su propio quiz no
> practica ni porque otro quiz del tema requiera una explicación distinta.
>
> Por cada problema da la gravedad y una evidencia que cumple [F-HALL-R003](#F-HALL-R003): el texto de la miniteoría, lo
> que tiene de falso, insuficiente o ajeno al caso, y el quiz involucrado. Que una miniteoría pida una forma no es un
> problema de este juez: lo marca el paquete A ([F-JEXAM-R012](#F-JEXAM-R012)).

<details><summary>Ejemplos de alcance por quiz</summary>

| Tema con varios subtemas | Quiz | Qué debe juzgar |
|---|---|---|
| Afirmación, negación y pregunta | Pregunta | La tarjeta explica cómo construir la pregunta; no necesita enseñar afirmación ni negación. |
| Afirmación, negación y pregunta | Afirmación | Una tarjeta que sólo explica el orden de pregunta es ajena a este caso, aunque sea cierta. |
| Una regla aplicada con palabras distintas | Caso con vocabulario A | La tarjeta explica la decisión que exige este caso; no necesita enumerar el vocabulario de los otros quizzes. |
| Dos quizzes muestran el mismo texto | Cada uno por separado | El texto puede aprobar en uno y fallar en otro según la forma, función y palabras que cada quiz pida. |

</details>

<a id="F-MINIT-R003"></a>
### Rule[F-MINIT-R003] - El error y su identidad pertenecen al quiz concreto
**Severity**: critical | **Validation**: VALIDATED

> Una miniteoría con problema da un hallazgo únicamente en el quiz cuyo caso juzgó y cuenta como error ahí, para
> `mini-theory` y para «algún error» ([F-HALL-R008](#F-HALL-R008)). La identidad combina el quiz y su miniteoría: que el
> mismo texto aparezca en otros quizzes no propaga el hallazgo ni obliga a darles el mismo veredicto. El puntaje está en
> cada quiz y sube por la agregación genérica.

José, 5/10 ([DOUBT-MINIT-GRANULARIDAD](#DOUBT-MINIT-GRANULARIDAD)): «miniteoría es por quiz» y debe ser «la corrección
específica del caso». Esto reemplaza el agrupamiento anterior por texto compartido y deja sin efecto el alcance opcional
«en todo el tema».

<a id="F-MINIT-R004"></a>
### Rule[F-MINIT-R004] - Gravedad y puntaje de cada quiz
**Severity**: critical | **Validation**: AUTO_VALIDATED

> La gravedad es **alta** si la regla es falsa para el caso del quiz o no explica su corrección, **media** si exagera dentro
> de ese caso («siempre» o «nunca» donde hay excepciones comunes) y **baja** si agrega contenido verdadero pero propio de
> otro subtema que ese quiz no practica. Nunca es
> bloqueante: la miniteoría se ve después de contestar. El puntaje de cada quiz es 1 sin hallazgo y, si no, 0,3, 0,6
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
> 4. la cobertura declara temas alcanzados, evaluados y sin evaluar, y los quizzes que contienen; cada quiz evaluado tiene
>    su propio veredicto, puntaje y eventual hallazgo; los quizzes de un tema sin evaluar cuentan como sin evaluar
>    ([F-QINST-R004](#F-QINST-R004)).

Valen además los grupos A y B de FEAT-JEXAM. Llega a tema, topic, nivel y curso ([F-HALL-R011](#F-HALL-R011)) sin mover el
puntaje de vocabulario ([F-HALL-R009](#F-HALL-R009)). Tope por omisión: 0 temas ([F-JEXAM-R010](#F-JEXAM-R010)).

<a id="F-MINIT-R006"></a>
### Rule[F-MINIT-R006] - El examen de mini-theory compara casos por quiz
**Severity**: critical | **Validation**: VALIDATED

> Cada versión aprueba el examen fijo ([F-JEXAM-R001](#F-JEXAM-R001)) comparando el veredicto de cada quiz con un oro que
> identifica qué caso practica y qué miniteoría le corresponde. Un oro que sólo clasifique el tema completo no alcanza
> cuando el tema mezcla afirmación, negación, pregunta u otros subtemas: antes de usarlo para aprobar o reprobar, esos
> casos deben quedar separados por quiz.

Las verduras aprueban acá: su problema es la pista y la traducción, no la regla. Para evitar premiar el sobreajuste, el
examen conserva quizzes buenos de subtemas vecinos y casos malos que requieren distinguir el alcance local de la tarjeta.

<a id="F-MINIT-R007"></a>
### Rule[F-MINIT-R007] - Se corrige la miniteoría del quiz, sin propagación por texto
**Severity**: major | **Validation**: VALIDATED

> Lo que marca se resuelve con una reescritura de la miniteoría de ese quiz, específica para el caso que el quiz enseña.
> La corrección no se propaga automáticamente a otros quizzes sólo porque hoy compartan el mismo texto. Cada candidato lo
> juzga este mismo juez ([F-JEXAM-R005](#F-JEXAM-R005)) contra el quiz al que pertenece.

Mantiene [F-HALL-R004](#F-HALL-R004): hay tarea sólo para el quiz con puntaje menor que 1. Dos quizzes equivalentes pueden
terminar con la misma redacción, pero esa coincidencia es un resultado de sus correcciones, no una identidad compartida
que permita modificar uno por arrastre. El tipo de tarea y la mesa son del paquete E.

<a id="F-MINIT-R008"></a>
### Rule[F-MINIT-R008] - La estructura no se enseña como una receta lexical del ítem
**Severity**: major | **Validation**: VALIDATED

> Una tarjeta verdadera y suficiente puede especificar de más si fija palabras
> incidentales del quiz como parte de una regla de estructura. El juez la clasifica
> como `OVER_SPECIFIC_FOR_QUIZ`, gravedad baja, después de descartar los defectos
> anteriores. La evidencia nombra el detalle incidental y propone cómo abstraer
> su papel sin perder la decisión focal. Un nombre que sólo determina concordancia
> se puede expresar por su categoría gramatical; un pronombre que fija el auxiliar
> puede conservarse. Las palabras necesarias para una elección léxica, colocación,
> significado, irregularidad o excepción siguen siendo parte de la explicación.
> Los marcadores estructurales, como una apertura interrogativa que muestra el
> tipo de pregunta y la posición del auxiliar, pueden mantenerse en un esquema
> reutilizable. Que ya estén dados en el enunciado no los convierte por sí solo
> en un defecto; el criterio no exige abstraer todos los tokens.
>
> La extensión por sí sola no causa un hallazgo. Una razón de concordancia o un
> ejemplo que distingue rutas reales puede ser útil aunque alargue la tarjeta.
> El corrector conserva esas distinciones y evita copiar la respuesta completa o
> repetir en prosa lo que un esquema ya explica. La nueva categoría mantiene el
> puntaje de los hallazgos bajos y la corrección por quiz, sin propagación.

Pedido del operador, 13/10/2026: mejorar también al juez para encontrar los casos
de PRB-f35c962c; las respuestas anteriores gustaron aunque quedaron largas.

## Contexto

| Ficha | `mini-theory` |
|---|---|
| Pregunta | ¿La tarjeta explica correctamente el caso concreto de este quiz, sin traer reglas de otros subtemas? |
| Lee | cada quiz con su miniteoría, enunciado, pista y respuestas u opciones; nivel, nombre y consigna del tema como contexto |
| Reglas | es falsa para el caso · no alcanza para resolverlo · trae contenido de otro subtema |
| Meta y familia | 0 quizzes con error · errores |
| Evalúa y resuelve | puede consultar por tema, pero decide y corrige por quiz |
| Costo | modelo pago: una consulta por tema |

**Costo.** El curso entero son 413 consultas: A1 83, A2 91, B1 147, B2 92. Son más largas que las de un quiz (media
de 27 quizzes por tema, hasta 170): a un minuto cada una, unas 7 horas de a una [ASSUMPTION]. Después de una tanda, una
consulta por tema tocado.

## Alcance

- **Adentro**: el analizador, qué lee del tema, qué decide por quiz, el hallazgo y la corrección en ese quiz, gravedad,
  puntaje, examen y ficha.
- **Afuera**: la reescritura y su tipo de tarea (E); la teoría larga de cada tema; lo común de los jueces (FEAT-JEXAM).

## User Journeys

### Journey[F-MINIT-J001] - Juzgar la miniteoría propia de cada quiz
**Validation**: VALIDATED

```yaml
journeys:
  - id: F-MINIT-J001
    name: Juzgar la miniteoría propia de cada quiz
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
        action: "El veredicto dice, por cada quiz, si su miniteoría explica de forma cierta, suficiente y específica el caso que ese quiz practica"
        gate: [F-MINIT-R002]
        outcomes:
          - when: "Todas están bien"
            then: pasa
          - when: "Al menos una tiene problema"
            then: error
      - id: pasa
        action: "Ningún quiz del tema tiene hallazgo de mini-theory y el puntaje de cada uno es 1"
        result: success
      - id: error
        action: "Sólo el quiz cuyo caso y miniteoría no coinciden recibe el hallazgo, la gravedad y una corrección específica; otros quizzes no cambian por compartir texto o tema"
        gate: [F-MINIT-R003, F-MINIT-R004, F-MINIT-R007]
        result: success
      - id: sin_evaluar
        action: "El tema queda sin evaluar: sus quizzes no tienen puntaje ni hallazgo de mini-theory, y la cobertura los cuenta"
        gate: [F-MINIT-R005]
        result: success
```

## Open Questions

Proveedor por omisión (`codex-cli` con sesión local de ChatGPT), tope (0 sin pedido), orden (examen fijo, `escribir/` y A1) y consultas a la vez (4) se
decidieron una vez para los cinco jueces, en las dudas DOUBT-JUECES-* de
[FEAT-JEXAM](../2026-09-30.12_juez-de-consigna-examen/REQUIREMENT.md#DOUBT-JUECES-PROVEEDOR) (proveedor actualizado por José, 4/10). Este es el más
barato: 413 consultas para el curso entero.

<a id="DOUBT-MINIT-GRANULARIDAD"></a>
### Doubt[DOUBT-MINIT-GRANULARIDAD] - ¿La miniteoría explica el tema o el caso de un quiz?
- [ ] Opción A: es una explicación común para todos los quizzes del ejercicio o tema.
- [x] Opción B: pertenece a un quiz concreto y explica únicamente el caso que ese quiz practica.

**Status**: RESOLVED (2026-10-05)

**Answer**: **Opción B** (José, 5/10): «miniteoría es POR QUIZ» y cada tarjeta debe ser «la corrección específica del
caso». Si un ejercicio agrupa afirmación, negación, pregunta o vocabulario diferente, el quiz de pregunta sólo recibe la
miniteoría de pregunta. Esto gobierna el veredicto, el hallazgo y la reescritura; reunir el tema en una consulta sigue
siendo únicamente una optimización de costo ([F-MINIT-R001](#F-MINIT-R001), [F-MINIT-R002](#F-MINIT-R002),
[F-MINIT-R003](#F-MINIT-R003), [F-MINIT-R007](#F-MINIT-R007)).

<a id="DOUBT-MINIT-VARIAS"></a>
### Doubt[DOUBT-MINIT-VARIAS] - Si el tema tiene varias miniteorías, ¿en qué ejercicios cuenta la que tiene problema?
- [ ] Opción A: en todos los del tema, como decía [F-MINIT-R003](#F-MINIT-R003) por la decisión del 30/9 a la mañana.
- [x] Opción B (recomendada): en los que la muestran; en los 154 temas de una sola miniteoría es lo mismo que A.

**Status**: RESOLVED (2026-09-30)

**Answer**: **Opción B** (José, 30/9 a la tarde): «cuenta sólo donde se muestra». La aclaración posterior de
[DOUBT-MINIT-GRANULARIDAD](#DOUBT-MINIT-GRANULARIDAD) vuelve esto aún más preciso: cuenta sólo en el quiz cuyo caso fue
juzgado, aunque el mismo texto se muestre también en otros. El antiguo parámetro «en todo el tema» queda retirado.

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
