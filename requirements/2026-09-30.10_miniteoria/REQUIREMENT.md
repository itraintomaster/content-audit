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
> quiz lee su propia miniteoría, su consigna efectiva, su enunciado, su pista y sus respuestas aceptadas, o sus opciones con la correcta marcada,
> además del nivel, nombre y consigna del tema como contexto. No lee [traducciones](glossary:Traducción), audios ni la
> teoría larga.

Dos quizzes que muestran el mismo texto siguen siendo dos casos juzgados: cada aparición se interpreta según lo que pide
y responde ese quiz. La consulta puede seguir siendo una por tema; la huella incluye todo el contenido enviado, por lo que
cambiar una miniteoría o un quiz deja ese lote sin veredicto. Son 413 temas y 1.488 miniteorías en el curso.
La consigna general del tema no sustituye a la consigna propia del quiz: ambas se consideran para entender el caso
cuando el alumno las ve.

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
Los defectos editoriales específicos de [F-MINIT-R008](#F-MINIT-R008), [F-MINIT-R009](#F-MINIT-R009) y
[F-MINIT-R010](#F-MINIT-R010) tienen gravedad baja cuando no hay otro defecto de mayor gravedad.

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

> Cada versión aprueba el examen permanente ([F-JEXAM-R001](#F-JEXAM-R001)) comparando el veredicto de cada quiz con un oro que
> identifica qué caso practica y qué miniteoría le corresponde. Un oro que sólo clasifique el tema completo no alcanza
> cuando el tema mezcla afirmación, negación, pregunta u otros subtemas: antes de usarlo para aprobar o reprobar, esos
> casos deben quedar separados por quiz.

Las verduras aprueban acá: su problema es la pista y la traducción, no la regla. Para evitar premiar el sobreajuste, el
examen conserva quizzes buenos de subtemas vecinos y casos malos que requieren distinguir el alcance local de la tarjeta.
Los casos confirmados de problemas se incorporan al examen permanente con su referencia de origen y deben superarse
en las ediciones siguientes, según [F-JEXAM-R016](#F-JEXAM-R016) a [F-JEXAM-R019](#F-JEXAM-R019). Esto incluye el rechazo
de las miniteorías invertidas de `PRB-fbaeac6f`, aunque describan una correspondencia verdadera, como exige
[F-MINIT-R010](#F-MINIT-R010).

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

<a id="F-MINIT-R009"></a>
### Rule[F-MINIT-R009] - La miniteoría evita el metalenguaje teórico y la jerga gramatical abstracta
**Severity**: major | **Validation**: VALIDATED

> La miniteoría es una herramienta de aprendizaje breve para el alumno, no una clase de
> lingüística. Si la tarjeta es verdadera y suficiente pero recurre a terminología teórica
> o etiquetas gramaticales abstractas (como «adjetivo atributivo/predicativo», «estilo indirecto»,
> «verbo de estado/estativo/copulativo» o «perífrasis aspectual») en lugar de una explicación
> funcional e intuitiva en lenguaje corriente («la cualidad va antes de lo que describe»,
> «al contar lo que alguien dijo», «para hablar de gustos o hábitos»), el juez la clasifica
> como `THEORETICAL_JARGON`, gravedad baja, después de descartar sobreespecificación.
> La evidencia señala el término abstracto y propone una formulación comunicativa en lenguaje
> llano. El corrector también es instruido para evitar este defecto al generar nuevas propuestas.

Pedido del operador, 19/10/2026: evitar exceso de conceptos teóricos; las miniteorías deben ser
focalizadas y sencillamente explicadas para el alumno.

<a id="F-MINIT-R010"></a>
### Rule[F-MINIT-R010] - La explicación de concordancia parte del sujeto y lleva a su forma verbal
**Severity**: major | **Validation**: VALIDATED

> Cuando la miniteoría explica qué forma verbal corresponde al sujeto dado en el quiz, parte de ese sujeto y lleva a
> la forma que debe usar el alumno. El juez marca la explicación invertida —«am va con I», «is va con it»— incluso si
> la correspondencia es verdadera o exclusiva: es un defecto de dirección didáctica, no necesariamente una falsedad
> gramatical. Si éste es el defecto, la clasifica como `DIDACTIC_DIRECTION`, gravedad baja, tras descartar los defectos
> de verdad y alcance del caso.

<details><summary>Dirección y criterios de aceptación</summary>

La tarjeta debe ayudar al alumno a pasar del dato que tiene —el sujeto del ejercicio— a la elección que debe hacer
—la forma verbal—. Se aceptan formulaciones equivalentes, como «I lleva am», «Con I se usa am», «I va con am» o
«En presente afirmativo, con it se usa is». No se exige una frase literal ni un verbo explicativo determinado.

Este criterio editorial no se satisface sólo porque la tarjeta permita acertar entre las opciones. Por eso la
correspondencia exclusiva entre I y am no justifica aprobar «am va con I». Tampoco se elimina el defecto porque una
relación de compatibilidad, leída aisladamente, sea cierta.

La dirección correcta no exime a la tarjeta de explicar el caso concreto según [F-MINIT-R002](#F-MINIT-R002): debe
conservar el tiempo, la afirmación o negación y la función que practica ese quiz. Una tarjeta de afirmación no reemplaza
la explicación de una pregunta ni la de una negación. Las explicaciones sanas de esos subcasos siguen siendo válidas;
no se reescriben ni rechazan por no enseñar otros subcasos.

La evidencia identifica la relación invertida y propone orientarla desde el sujeto hacia la forma que corresponde,
sin acusarla de falsa por esa inversión. Si además hay una falsedad, una restricción engañosa u otro defecto, se
conserva su motivo propio y la gravedad que corresponda según [F-MINIT-R004](#F-MINIT-R004).
«is va con it» no dice por sí sola «is sólo va con it»: no se inventa una exclusividad para justificar otro defecto.
Una restricción explícita o inequívoca, como «is sólo va con it», sí se juzga por su propio contenido.

| Quiz | Miniteoría | Resultado esperado por este criterio |
|---|---|---|
| I ___ hot. · am / are / is | am va con I. | Rechazada por dirección invertida, aun siendo una correspondencia exclusiva. |
| I ___ hot. · am / are / is | I lleva am. | Aprobada si cumple las demás reglas del caso. |
| I ___ hot. · am / are / is | Con I se usa am. | Aprobada si cumple las demás reglas del caso; no se impone una frase única. |
| It ___ under the chair. · am / are / is | is va con it. | Rechazada por dirección invertida. |
| It ___ under the chair. · am / are / is | En presente afirmativo, it lleva is. | Aprobada si cumple las demás reglas del caso. |

**Criterio de aceptación del examen**: los dos casos invertidos «am va con I» e «is va con it» quedan como rechazos
obligatorios del examen permanente, con origen en `PRB-fbaeac6f` y la confirmación del operador. Los controles sanos
conservan su procedencia real: un control propuesto por el asistente no se convierte por eso en «Está bien» confirmado
por el operador ([F-JEXAM-R017](#F-JEXAM-R017)).

Pedido explícito del operador, 21/10/2026: corregir también al juez para que detecte estas explicaciones invertidas,
incluido el caso I/am, y conservar los casos en el examen para las versiones siguientes.

</details>

<a id="F-MINIT-R011"></a>
### Rule[F-MINIT-R011] - Cada aparición de inglés clave se resalta dentro de la miniteoría
**Severity**: minor | **Validation**: VALIDATED

> La miniteoría encierra entre `$...$` cada aparición de las palabras o formas inglesas que cita para enseñar el caso:
> pronombres, auxiliares, contracciones, expresiones, letras o terminaciones y transformaciones. La explicación en español
> queda fuera de esas marcas. Si falta alguna, el juez señala `MISSING_ENGLISH_HIGHLIGHT`, gravedad baja (`LOW`), después
> de descartar los demás defectos, incluido [F-MINIT-R009](#F-MINIT-R009), y antes de declarar sana la tarjeta.

<details><summary>Resaltado, corrección y criterios de aceptación</summary>

El resaltado se exige por aparición: haber marcado una palabra una vez no permite dejar sin marcar su repetición.
Una transformación completa puede quedar dentro de una misma pareja de marcas, como `$watch → watches$`.
Las negritas, cursivas, etiquetas HTML o comillas de código no reemplazan `$...$`.
Una tarjeta íntegramente en español no falla por carecer de estas marcas; una tarjeta cuyo inglés ya está marcado
tampoco recibe este hallazgo. En ambos casos siguen aplicándose las demás reglas del quiz.

La evidencia identifica los fragmentos ingleses sin resaltar y muestra la misma redacción con las marcas que faltan,
sin aprovechar este defecto para cambiar la explicación, el sentido ni el alcance del caso. Una falsedad o cualquier
otro defecto prioritario conserva su clasificación y gravedad propias; agregar resaltado no vuelve cierta una regla falsa.
Si la redacción completa corregida supera el límite de la evidencia, se muestra un fragmento literal corregido
y se identifican las demás apariciones en el motivo; esto no autoriza recortar la tarjeta al corregirla.

El corrector aplica este criterio tanto cuando corrige a partir de un hallazgo como cuando revisa una propuesta de
miniteoría. Antes de reutilizar literalmente una tarjeta comprueba también su resaltado; si falta, lo completa aunque
el contenido sea cierto. El juez vuelve a evaluar la tarjeta corregida para comprobar que resolvió el problema y que
sigue cumpliendo las demás reglas, de acuerdo con [F-MINIT-R007](#F-MINIT-R007).

| Quiz de origen | Texto original conservado | Corrección de resaltado esperada |
|---|---|---|
| He watches TV. | Con he, she, it, los verbos que terminan en ch agregan $-es$ en presente simple: watch → watches. | Con $he$, $she$, $it$, los verbos que terminan en $ch$ agregan $-es$ en presente simple: $watch → watches$. |
| The student studies hard. | Con he, she, it, si el verbo termina en consonante + y, cambiamos la y por $-ies$ en presente simple: study → studies. | Con $he$, $she$, $it$, si el verbo termina en consonante + $y$, cambiamos la $y$ por $-ies$ en presente simple: $study → studies$. |

**Criterio de aceptación del examen**: la propuesta en `analysis/problems/PRB-1fc8487f/highlight-exam` incorpora
12 casos: siete con defecto de resaltado, cuatro controles sanos y uno con falsedad semántica que conserva su motivo
prioritario. Los casos de origen de watches y studies mantienen exactamente el texto observado, incluido el resaltado
parcial que ya tenían. Las variantes comprueban pronombres y auxiliares, contracciones, expresiones, letras y terminaciones,
transformaciones, repeticiones y formatos que no sustituyen las marcas requeridas.

Los controles propuestos son referencias editoriales del asistente, no aprobaciones humanas confirmadas
([F-JEXAM-R017](#F-JEXAM-R017)). Este conjunto es una propuesta de examen: su documentación no acredita una ejecución
real ni la aprobación del juez.

**Fuente**: pedido explícito del operador del 21/10/2026 y problema `PRB-1fc8487f`: ampliar las pruebas para que primero
el juez detecte el inglés clave sin resaltar y luego el corrector produzca el cambio correspondiente.

</details>

<a id="F-MINIT-R012"></a>
### Rule[F-MINIT-R012] - La explicación conserva la decisión y la fórmula de la lección
**Severity**: critical | **Validation**: VALIDATED

> La miniteoría explica la decisión concreta que exige la consigna efectiva y la pista del quiz, incluida la fórmula
> que practica. No reemplaza una elección didáctica entre formas por una explicación general que vuelva indistintas
> las respuestas. La convención de la lección y la naturalidad del inglés se juzgan por separado.

<details><summary>Alcance y criterios de aceptación</summary>

En la lección de pedidos confirmada por José el 24/10/2026, el selector «Polite request» pide `could` y el pedido normal
pide `can`, conforme a lo que se explica al alumno. Una tarjeta que sólo dice que ambos pueden usarse en pedidos
corteses no explica la elección que corrige ese quiz. Debe conservar la distinción de la lección y la fórmula visible,
sin convertirla en una afirmación de que `can` nunca puede ser cortés fuera del ejercicio.

Si el alumno no recibe antes de contestar una fórmula o convención necesaria, la tarjeta posterior no repara esa falta
de información. Tampoco se inventa una fórmula completa a partir de la respuesta guardada
([F-UNICA-R008](#F-UNICA-R008)). Cada defecto se describe por su propio motivo: una explicación puede ser verdadera
como observación general y, aun así, insuficiente para la decisión didáctica de este quiz
([F-MINIT-R002](#F-MINIT-R002)).

**Fuente**: aclaración explícita de José del 24/10/2026 sobre `can/could`, el selector «Polite request» y la práctica
de una fórmula específica; la propuesta observada había eliminado el selector y ampliado las respuestas.

</details>

## Contexto

| Ficha | `mini-theory` |
|---|---|
| Pregunta | ¿La tarjeta explica correctamente el caso concreto de este quiz, sin traer reglas de otros subtemas? |
| Lee | cada quiz con su miniteoría, consigna efectiva, enunciado, pista y respuestas u opciones; nivel, nombre y consigna del tema como contexto |
| Reglas | es falsa para el caso · no alcanza para resolverlo · trae contenido de otro subtema · invierte la explicación de sujeto a forma verbal |
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
        action: "El veredicto dice, por cada quiz, si su miniteoría explica de forma cierta, suficiente y específica el caso que ese quiz practica, con la dirección didáctica adecuada"
        gate: [F-MINIT-R002, F-MINIT-R010]
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
  Citado por R002 y R005 a R007. Las reglas [F-JEXAM-R016](#F-JEXAM-R016) a [F-JEXAM-R019](#F-JEXAM-R019) conservan los
  casos de problemas y su referencia de origen; [F-MINIT-R006](#F-MINIT-R006) y [F-MINIT-R010](#F-MINIT-R010) las aplican
  a las explicaciones invertidas confirmadas.
- **FEAT-EVCOST** — Huella, reuso y fallas. Citado por R005.
- **FEAT-QINST** — La cascada de la consigna, fallas y lo no evaluado. Citado por R001 y R005.
- **FEAT-UNICA** — Las alternativas respetan la fórmula y el selector visibles; citado por
  [F-MINIT-R012](#F-MINIT-R012) mediante [F-UNICA-R008](#F-UNICA-R008).
