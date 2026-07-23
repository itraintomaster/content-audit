---
feature:
  id: FEAT-CLEX
  code: F-CLEX
  name: Consulta lexica de oracion por CLI
  priority: critical
---

# Consulta lexica de oracion por CLI

## TL;DR

**Que**: Agrega una consulta de **solo lectura** del CLI que, dada una
[Oración de quiz](glossary:Oración de quiz) (misma DSL que la consulta de tokens) y
un [Nivel CEFR](glossary:Nivel CEFR), devuelve en JSON las palabras que el analisis de
mal-ubicacion **flaggearia** para ese nivel: mal ubicadas para el nivel y fuera
de catalogo penalizantes.

**Por que**: El eval de no-regresion lexica del agente de lemma-absence
(criterio #7 de [F-LASAG-R007](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R007))
necesita verificar candidatos con el **mismo** analisis lexico del audit, no una
replica aproximada; hoy ese analisis solo corre sobre un curso persistido al
auditarlo, nunca sobre una oracion suelta.

## Recorrido de uso (estado actual)

Como intenta hoy el agente revisor verificar que una oracion candidata no
introduce vocabulario problematico para su nivel:

1. Genera una oracion candidata para un nivel y quiere comprobar su **longitud**
   con el mismo conteo que el audit → ya lo resuelve con la consulta de tokens
   del CLI (`content-audit tokens`), que reutiliza el tokenizador real del
   audit. ✓ Disponible (via la infraestructura de evals de
   [FEAT-LASAG](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md), criterio #3).
2. Quiere comprobar el nivel lexico de cada palabra del candidato con el mismo
   catalogo del audit → la unica via publica que corre el analisis de
   mal-ubicacion es `AnalyzeCommand.analyze(coursePath, format, level, topic, knowledge, analyzers, detailed)`,
   que audita entidades de un **curso persistido** completo. ⚠️ No acepta una
   oracion suelta con un nivel arbitrario ni devuelve la lista de palabras
   flaggeadas por oracion.

**Friction point**: paso 2. Para verificar una sola oracion candidata el agente
tendria que persistirla dentro de un curso y auditar el curso entero, y aun asi
el audit expone scores por entidad, no la lista de palabras que flaggeo con su
lema, categoria y nivel esperado. El resultado observado (caso 2026-07-22): el
agente reemplazo "remitted" por "transferred" en un quiz A1 y **ningun eval**
detecto que `transfer` es B1, porque no existe una consulta lexica por oracion.

**Cambio minimo necesario**: introducir una **consulta lexica de oracion** de
solo lectura, analoga a la consulta de tokens pero para el eje lexico: recibe una
[Oración de quiz](glossary:Oración de quiz) (misma DSL, incluyendo modo `FILL`/`REWRITE`) y
un [Nivel CEFR](glossary:Nivel CEFR), y devuelve las palabras que el analisis de
mal-ubicacion flaggearia para ese nivel, cada una con su [Lema](glossary:Lema),
su [Categoría gramatical](glossary:Categoría gramatical) observada, su tipo (mal ubicada
para el nivel | fuera de catalogo) y el dato de nivel/frecuencia que motiva el
flag. El analisis se **reutiliza** intacto del audit; esta consulta no lo
reimplementa. La forma concreta del comando (nombre del verbo, flags, formato de
salida) es decision de arquitectura.

## Business Rules

<a id="F-CLEX-R001"></a>
### Rule[F-CLEX-R001] - Nueva consulta lexica de oracion, de solo lectura
**Severity**: critical | **Validation**: VALIDATED

> El CLI expone una consulta que, dada una [Oración de quiz](glossary:Oración de quiz)
> y un [Nivel CEFR](glossary:Nivel CEFR), devuelve las palabras que el analisis de
> mal-ubicacion flaggearia para esa oracion en ese nivel. La consulta es de
> **solo lectura**: no requiere un curso persistido, no crea ni modifica planes,
> propuestas, cursos ni analisis, y no depende de que la oracion pertenezca a
> ningun curso.

<details><summary>Detalle</summary>

Es el analogo lexico de la consulta de tokens del CLI (que reutiliza el conteo
real del audit para la longitud). Su unica salida es la lista de palabras
flaggeadas; no emite score de oracion, no persiste nada y no altera estado.

La **forma concreta** del comando (nombre del verbo, flags, formato exacto del
JSON) es decision de arquitectura, en linea con la resolucion de
[DOUBT-EVAL-LEXICO-CLI](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#DOUBT-EVAL-LEXICO-CLI)
(Opcion A). Esta feature fija el **contrato observable**, no la sintaxis del CLI.

**Error**: "No aplica."

</details>

<a id="F-CLEX-R002"></a>
### Rule[F-CLEX-R002] - Entrada: la DSL de quizSentence, incluido el modo de frase
**Severity**: major | **Validation**: VALIDATED

> La consulta acepta la oracion como una [Oración de quiz](glossary:Oración de quiz)
> expresada en la **misma DSL** que la consulta de tokens (por ejemplo
> `"He ____ [sips] (sip) a lot of tea."`), incluyendo el modo de frase
> `FILL`/`REWRITE`. La oracion que se analiza lexicamente es la **misma frase
> canonica** que el audit puntuaria para ese modo: en `FILL`, la oracion completa
> con el hueco relleno; en `REWRITE`, solo la oracion respuesta.

<details><summary>Detalle</summary>

Paridad estricta con como el audit deriva la frase canonica por modo
([F-SMODE-R003](../2026-06-25.01_sentence-mode-canonical-sentence/REQUIREMENT.md#F-SMODE-R003),
[F-SMODE-R004](../2026-06-25.01_sentence-mode-canonical-sentence/REQUIREMENT.md#F-SMODE-R004)):
en `REWRITE` la oracion fuente/prompt no entra en el analisis lexico, igual que
no entra en el conteo de tokens. Sin esta paridad, la consulta reportaria
palabras de la oracion fuente que el audit nunca flaggearia, rompiendo la
garantia de [F-CLEX-R006](#F-CLEX-R006).

**Error**: "No aplica."

</details>

<a id="F-CLEX-R003"></a>
### Rule[F-CLEX-R003] - Salida: cada palabra mal ubicada
**Severity**: major | **Validation**: VALIDATED

> Por cada palabra de contenido **mal ubicada para el nivel** (nivel esperado
> superior al [Nivel CEFR](glossary:Nivel CEFR) de la oracion), la respuesta incluye
> una entrada con: su [Lema](glossary:Lema), su [Categoría gramatical](glossary:Categoría gramatical)
> observada, el **nivel esperado** del lema y el **nivel de la oracion**, y su
> tipo marcado como *mal ubicada*.

**Error**: "No aplica."

<a id="F-CLEX-R004"></a>
### Rule[F-CLEX-R004] - Salida: cada palabra fuera de catalogo penalizante
**Severity**: major | **Validation**: VALIDATED

> Por cada palabra de contenido **fuera de catalogo que penaliza** en el nivel de
> la oracion (segun las bandas de frecuencia de
> [F-LABS-R034](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R034)), la respuesta
> incluye una entrada con: su [Lema](glossary:Lema), su
> [Categoría gramatical](glossary:Categoría gramatical), su **rango de frecuencia**
> (`null` cuando la palabra no tiene ranking disponible) y su tipo marcado como
> *fuera de catalogo*.

<details><summary>Detalle</summary>

Cada palabra fuera de catalogo reportada penaliza por definicion (ver
[F-CLEX-R005](#F-CLEX-R005)): solo aparecen en la lista las que caen en una banda
con descuento para el nivel de la oracion. El rango de frecuencia se expone como
evidencia del flag; `null` corresponde a una palabra sin ranking, que siempre
cae en la banda de descuento fuerte
([F-LABS-R034](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R034)).

**Error**: "No aplica."

</details>

<a id="F-CLEX-R005"></a>
### Rule[F-CLEX-R005] - La lista contiene solo flags; vacia significa oracion limpia
**Severity**: critical | **Validation**: VALIDATED

> La respuesta contiene **unicamente** palabras flaggeadas: mal ubicadas para el
> nivel ([F-CLEX-R003](#F-CLEX-R003)) mas fuera de catalogo penalizantes
> ([F-CLEX-R004](#F-CLEX-R004)). Una palabra **justificada** para el nivel NO
> aparece en la lista. Una lista **vacia** significa que la oracion esta limpia
> para ese nivel.

<details><summary>Detalle</summary>

Una palabra esta justificada (y por lo tanto se omite) cuando cumple cualquiera
de los motivos del caso base del audit
([F-LABS-R020](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R020)):

1. Es un lema del catalogo con nivel esperado **igual o inferior** al de la
   oracion.
2. Es una palabra fuera de catalogo **frecuente** que no penaliza en la banda del
   nivel ([F-LABS-R034](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R034)).
3. Es un nombre propio u otro token excluido
   ([F-LABS-R035](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R035)).

**Decision de diseño**: reportar **solo** las palabras flaggeadas (y no todas las
palabras de contenido con un marcador `penaliza: si/no`) es lo que hace que
"lista vacia = oracion limpia" sea cierto, y hace que la lista sea directamente
el conjunto `flags(oracion)` que compara el eval #7
([F-LASAG-R007](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R007)).
La alternativa (listar tambien las palabras justificadas, marcadas) queda
diferida como [DOUBT-VERBOSE-MODE](#DOUBT-VERBOSE-MODE).

**Error**: "No aplica."

</details>

<a id="F-CLEX-R006"></a>
### Rule[F-CLEX-R006] - Paridad exacta con el analisis del audit
**Severity**: critical | **Validation**: VALIDATED

> Los flags que reporta esta consulta para una oracion en un nivel son
> **exactamente** los que el audit reportaria para esa misma oracion en ese
> nivel. La resolucion de nivel de cada palabra aplica **todo el pipeline
> vigente del catalogo**, con el **mismo tokenizador** y el **mismo catalogo con
> todas sus reglas de nivel**; no es una replica aproximada.

<details><summary>Detalle</summary>

El pipeline reutilizado, sin desviacion, incluye:

- Exclusion de entradas de frase de toda consulta de nivel
  ([F-LABS-R005](../2026-03-28.03_lemma-absence/REQUIREMENT.md)).
- Exclusion de formas derivadas y compuestas
  ([F-LABS-R040](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R040)).
- Categoria gramatical autoritativa tomada de EVP
  ([F-LABS-R041](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R041)).
- Rebajas asimetricas por frases y por categorias no de contenido
  ([F-LABS-R042](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R042),
  [F-LABS-R043](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R043)).
- Puente sustantivo-adverbio
  ([F-LABS-R044](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R044)).
- Repliegue por lema normalizado a minusculas
  ([F-LABS-R045](../2026-03-28.03_lemma-absence/REQUIREMENT.md)).
- Techos de vocabulario superior al curso C1/C2
  ([F-LABS-R033](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R033)).
- Bandas de frecuencia de palabras fuera de catalogo por nivel
  ([F-LABS-R034](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R034)).
- Exclusiones de nombres propios y tokens no lexicos
  ([F-LABS-R035](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R035)).
- Metalenguaje curado que resuelve nivel pero no fija esperados
  ([F-LABS-R046](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R046)).

Contrato observable: para una misma oracion y un mismo nivel, el conjunto de
lemas flaggeados que devuelve esta consulta coincide con el conjunto de lemas que
el audit marca como mal ubicados o fuera de catalogo penalizantes para esa
oracion a ese nivel. Verificable comparando ambos conjuntos sobre casos
representativos (mal ubicada por catalogo, C1/C2, fuera de catalogo por banda,
palabra justificada por rebaja de frase, palabra justificada por metalenguaje).

**Error**: "No aplica."

</details>

<a id="F-CLEX-R007"></a>
### Rule[F-CLEX-R007] - Errores: DSL invalida y nivel invalido
**Severity**: major | **Validation**: VALIDATED

> Cuando la oracion no respeta la DSL de [Oración de quiz](glossary:Oración de quiz) o
> cuando el [Nivel CEFR](glossary:Nivel CEFR) informado no es valido, la consulta se
> **rechaza** con una validacion clara y no devuelve una lista de flags. Son los
> mismos patrones de error que la consulta de tokens.

<details><summary>Detalle</summary>

Un rechazo por entrada invalida es distinguible de una respuesta exitosa con
lista vacia ([F-CLEX-R005](#F-CLEX-R005)): lista vacia significa "oracion limpia
para el nivel"; un rechazo significa "no pude analizar la entrada". El nivel es
obligatorio: sin un nivel valido no hay contra que resolver la mal-ubicacion.

**Error**: "quizSentence invalida: no respeta la DSL." / "Nivel CEFR invalido." [ASSUMPTION]

</details>

## Context

El criterio #7 de no-regresion lexica
([F-LASAG-R007](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R007))
exige que el eval del agente de lemma-absence verifique cada candidato con el
**mismo** analisis lexico del audit: el candidato no debe introducir palabras mal
ubicadas para el nivel ni fuera de catalogo que la oracion original no tuviera ya
(`flags(candidato) ⊆ flags(original)`). El precedente directo es el eval de
longitud (criterio #3), que dejo de aproximar la longitud con un proxy de bash y
paso a reutilizar el conteo real del audit a traves de la consulta de tokens del
CLI. Esta feature es el **analogo lexico** de aquella consulta.

Hoy la unica via publica que corre el analisis de mal-ubicacion es la auditoria
de un curso persistido (`AnalyzeCommand.analyze(...)`), que expone scores por
entidad, no la lista de palabras flaggeadas por oracion, y no acepta una oracion
suelta con un nivel arbitrario. El caso que motivo esta feature (2026-07-22): un
candidato A1 reemplazo "remitted" por "transferred" sin que ningun eval detectara
que `transfer` es B1, porque no existia una consulta lexica por oracion.

La solucion es una consulta de solo lectura que reutiliza el analisis intacto:
recibe la oracion y el nivel, y devuelve exactamente las palabras que el audit
flaggearia ([F-CLEX-R006](#F-CLEX-R006)). El eval #7 la consume comparando el
conjunto de lemas flaggeados del candidato contra el de la oracion original.

## Scope

### IN

- Consulta de solo lectura oracion + nivel → lista de palabras flaggeadas.
- Entrada en la DSL de `quizSentence`, incluido el modo `FILL`/`REWRITE`.
- Salida por palabra mal ubicada: lema, categoria, nivel esperado, nivel de la
  oracion.
- Salida por palabra fuera de catalogo penalizante: lema, categoria, rango de
  frecuencia (`null` si no hay).
- Lista vacia = oracion limpia para el nivel; la lista contiene solo flags.
- Reutilizacion **intacta** de todo el pipeline lexico del audit (mismo
  tokenizador y catalogo, todas las reglas de nivel).
- Rechazo claro ante DSL invalida y ante nivel invalido.

### OUT

- No emite score de oracion ni score por analyzer (solo la lista de flags).
- No persiste ni modifica cursos, planes, propuestas ni analisis.
- No lista las palabras justificadas (no flaggeadas); ver
  [DOUBT-VERBOSE-MODE](#DOUBT-VERBOSE-MODE).
- No define la sintaxis concreta del comando (nombre del verbo, flags, formato de
  salida): es decision de arquitectura.
- No cambia ninguna regla del analisis de mal-ubicacion de FEAT-LABS; solo la
  **expone** por oracion.
- No cubre otros ejes de analisis (longitud, COCA, recurrencia): esta consulta es
  estrictamente el eje lexico de mal-ubicacion.

## User Journeys

### Journey[F-CLEX-J001] - El agente consulta los flags lexicos de un candidato

```yaml
flow:
  - id: request_query
    action: "El agente invoca la consulta lexica con una quizSentence (DSL, modo FILL/REWRITE) y un Nivel CEFR"
    gate: [F-CLEX-R001, F-CLEX-R002]
    then: validate_input

  - id: validate_input
    action: "El sistema valida la DSL de la quizSentence y el Nivel CEFR"
    gate: [F-CLEX-R007]
    outcomes:
      - when: "La quizSentence no respeta la DSL"
        then: reject_dsl
      - when: "El Nivel CEFR no es valido"
        then: reject_level
      - when: "La quizSentence y el Nivel CEFR son validos"
        then: resolve_lexical

  - id: resolve_lexical
    action: "El sistema deriva la frase canonica segun el modo y resuelve el nivel de cada palabra de contenido reutilizando el pipeline completo del audit"
    gate: [F-CLEX-R006]
    outcomes:
      - when: "Al menos una palabra queda flaggeada (mal ubicada o fuera de catalogo penalizante)"
        then: return_flags
      - when: "Ninguna palabra queda flaggeada"
        then: return_empty

  - id: return_flags
    action: "La respuesta es la lista de palabras flaggeadas: cada mal ubicada con lema, categoria, nivel esperado y nivel de la oracion; cada fuera de catalogo con lema, categoria y rango de frecuencia (null si no hay)"
    gate: [F-CLEX-R003, F-CLEX-R004, F-CLEX-R005]
    result: success

  - id: return_empty
    action: "La respuesta es una lista vacia, que significa que la oracion esta limpia para ese nivel"
    gate: [F-CLEX-R005]
    result: success

  - id: reject_dsl
    action: "La consulta se rechaza con una validacion clara porque la quizSentence no respeta la DSL"
    gate: [F-CLEX-R007]
    result: failure

  - id: reject_level
    action: "La consulta se rechaza con una validacion clara porque el Nivel CEFR no es valido"
    gate: [F-CLEX-R007]
    result: failure
```

## Open Questions

### Doubt[DOUBT-VERBOSE-MODE] - Modo verboso que liste tambien las palabras justificadas
**Status**: OPEN

El contrato base reporta **solo** las palabras flaggeadas
([F-CLEX-R005](#F-CLEX-R005)), lo que sirve directamente al eval #7. Queda abierto
si una version futura ofrece un modo verboso que liste todas las palabras de
contenido con un marcador (justificada / mal ubicada / fuera de catalogo) para
depuracion manual. No bloquea esta feature; el consumidor real (eval #7) solo
necesita el conjunto de flags.

### Doubt[DOUBT-CEFR-RANGE] - Niveles C1/C2 como nivel de la oracion consultada
**Status**: OPEN

El audit maneja C1/C2 como techos de vocabulario superior al curso
([F-LABS-R033](../2026-03-28.03_lemma-absence/REQUIREMENT.md#F-LABS-R033)), pero los
cursos son A1-B2. Queda abierto si la consulta acepta C1/C2 como **nivel de la
oracion** (no solo como nivel esperado de una palabra) o si se restringe a
A1-B2. El consumidor actual (eval #7) siempre consulta al nivel del quiz, que es
A1-B2.

### Doubt[DOUBT-ERROR-COPY] - Textos exactos de validacion
**Status**: OPEN

No se acordaron textos exactos de error para DSL invalida y nivel invalido; solo
se acordo que el rechazo sea claro y distinguible de una lista vacia
([F-CLEX-R007](#F-CLEX-R007)).

## References

- **FEAT-LASAG** — Define el eval #7 de no-regresion lexica
  ([F-LASAG-R007](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#F-LASAG-R007))
  y resolvio en
  [DOUBT-EVAL-LEXICO-CLI](../2026-06-18.01_lemma-absence-agent-migration/REQUIREMENT.md#DOUBT-EVAL-LEXICO-CLI)
  (Opcion A) que esta consulta lexica vive como capacidad propia del CLI con su
  contrato y regla dedicados. Es el consumidor de esta feature. Citada por
  [F-CLEX-R005](#F-CLEX-R005) y el Contexto.
- **FEAT-LABS** — Define todo el analisis de mal-ubicacion que esta consulta
  reutiliza intacto: identificacion de mal ubicados, techos C1/C2, bandas de
  fuera de catalogo, exclusiones, rebajas asimetricas, puente NOUN-ADV, repliegue
  a minusculas y metalenguaje curado. Citada por
  [F-CLEX-R003](#F-CLEX-R003), [F-CLEX-R004](#F-CLEX-R004),
  [F-CLEX-R005](#F-CLEX-R005) y [F-CLEX-R006](#F-CLEX-R006).
- **FEAT-SMODE** — Define la frase canonica puntuable por modo `FILL`/`REWRITE`;
  esta consulta analiza esa misma frase canonica. Citada por
  [F-CLEX-R002](#F-CLEX-R002).
- **FEAT-QSENT** — Define la DSL de `quizSentence` que la consulta acepta como
  entrada. Citada por [F-CLEX-R002](#F-CLEX-R002).
