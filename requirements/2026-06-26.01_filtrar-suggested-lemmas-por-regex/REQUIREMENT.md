---
feature:
  id: FEAT-SLREGEX
  code: F-SLREGEX
  name: Filtrar Suggested Lemmas por expresión regular
  priority: major
---

# Filtrar Suggested Lemmas por expresión regular

## TL;DR

**Qué**: Agrega un filtro `--regex` a `GetCommand.get("suggested-lemmas", <taskId>, ...)` que, cuando se informa, conserva únicamente los lemas candidatos cuyo texto coincide con el patrón, manteniendo el resto de los filtros y el orden por prioridad intactos.

**Por qué**: En varios casos el agente revisor necesita buscar tipos de palabra específicos por su forma superficial (terminaciones, prefijos, secuencias de letras) y hoy no hay forma de filtrar por el texto del lema; el filtro textual permite acotar la lista a esos casos sin pedir un tipo gramatical entero.

## Business Rules

<a id="F-SLREGEX-R001"></a>
### Rule[F-SLREGEX-R001] - Comportamiento por defecto inalterado
**Severity**: critical | **Validation**: VALIDATED

> Cuando la consulta no informa `--regex`, `GetCommand.get("suggested-lemmas", <taskId>, ...)` se comporta exactamente como hoy: no se aplica ningún filtrado textual y el resultado es idéntico al de la consulta sin esta feature.

<details><summary>Detalle</summary>

Esta regla fija la paridad con el comportamiento histórico de [FEAT-CSLATDC](#references) y [FEAT-INCEXP](#references). Ausente `--regex`, el conjunto de candidatos, los filtros aplicados (`Level`, `Part of Speech`, `limit`, inclusión de lemas ya expuestos) y el orden por prioridad de recomendación quedan exactamente como están definidos por esas features. Esta feature es una evolución aditiva: introduce el primer filtro léxico textual sobre la consulta, pero sólo actúa cuando el patrón se informa.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R002"></a>
### Rule[F-SLREGEX-R002] - El patrón matchea contra el texto del lema
**Severity**: critical | **Validation**: VALIDATED

> Cuando la consulta informa `--regex`, el patrón se evalúa únicamente contra el texto del lema sugerido (su forma superficial), no contra el `Part of Speech` ni ningún otro dato del resultado.

<details><summary>Detalle</summary>

El único campo sometido a coincidencia es la forma textual del lema. Pedir `--regex` no filtra por tipo gramatical (eso lo hace `Part of Speech`), ni por nivel, ni por señales de exposición: sólo conserva los lemas cuyo texto coincide con el patrón. Esto mantiene los ejes de filtrado separados y predecibles: `Part of Speech` decide la categoría gramatical, `--regex` decide la forma de la palabra.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R003"></a>
### Rule[F-SLREGEX-R003] - Coincidencia parcial estilo grep
**Severity**: critical | **Validation**: VALIDATED

> El patrón coincide si aparece en cualquier parte del texto del lema (coincidencia parcial, no anclada); el cliente ancla con `^` y `$` cuando quiere coincidencia desde el inicio o hasta el final.

<details><summary>Detalle</summary>

La coincidencia es de tipo búsqueda (grep-like): el patrón no necesita cubrir el lema completo para coincidir. Quien consulta controla el anclaje explícitamente con `^` (inicio) y `$` (final).

Ejemplos del comportamiento esperado:

| Patrón | Coincide con | Por qué |
|--------|--------------|---------|
| `ing$` | `running` | el lema termina en "ing" |
| `^un` | `undo` | el lema empieza con "un" |
| `tt` | `butter` | "tt" aparece dentro del lema |
| `^cat$` | `cat` (no `category`) | anclado a ambos extremos, coincidencia exacta |

La coincidencia parcial es deliberada: permite buscar terminaciones, prefijos y secuencias internas sin obligar a describir la palabra entera. Es responsabilidad de quien consulta anclar el patrón cuando quiere una coincidencia más estricta.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R004"></a>
### Rule[F-SLREGEX-R004] - Coincidencia insensible a mayúsculas y minúsculas
**Severity**: major | **Validation**: VALIDATED

> La coincidencia del patrón ignora la diferencia entre mayúsculas y minúsculas: por ejemplo, `ING$` coincide con `running` igual que `ing$`.

<details><summary>Detalle</summary>

El match es case-insensitive en ambos sentidos: ni el patrón ni el texto del lema necesitan compartir la caja de las letras para coincidir. Esto evita que quien consulta tenga que conocer de antemano la caja con que se almacena cada lema. El comportamiento es estable para `ing$`, `ING$` o `Ing$` frente a un lema `running` o `Running`.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R005"></a>
### Rule[F-SLREGEX-R005] - El filtro es aditivo y no relaja los demás filtros
**Severity**: critical | **Validation**: VALIDATED

> `--regex` se combina con los filtros existentes (`Level` obligatorio, `Part of Speech`, inclusión de lemas ya expuestos) sin relajarlos: se aplica como un filtro adicional sobre los candidatos que ya pasaron esos filtros, y nunca incorpora lemas que esos filtros habrían excluido.

<details><summary>Detalle</summary>

El filtro textual es restrictivo, no expansivo. Un lema sólo aparece en la respuesta si cumple TODOS los criterios: el `Level` aplicable, el `Part of Speech` pedido (cuando se informa), la condición de exposición vigente para la consulta —incluida la ampliación de [FEAT-INCEXP](#references) cuando esa opción está activada— y además coincide con el patrón. `--regex` no reintroduce lemas de otro nivel, de otro tipo gramatical ni ya expuestos cuando la opción de incluirlos no está activada.

En particular, el filtro obligatorio por `Level` ([F-CSLATDC-R003](../2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli/REQUIREMENT.md#F-CSLATDC-R003)) sigue rigiendo sin cambios: informar `--regex` no permite obtener lemas sin un nivel aplicable para filtrar.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R006"></a>
### Rule[F-SLREGEX-R006] - El orden por prioridad se preserva
**Severity**: critical | **Validation**: VALIDATED

> Aplicar `--regex` no cambia el orden de los resultados: los lemas que coinciden con el patrón se devuelven en el mismo orden de prioridad de recomendación que tendrían sin el filtro, sólo que sin los lemas que no coinciden.

<details><summary>Detalle</summary>

El filtro textual quita lemas de la lista pero no reordena los que quedan. La prioridad de recomendación y el desempate vigentes (frecuencia COCA ascendente, según [F-INCEXP-R004](../2026-06-18.01_incluir-lemas-ya-expuestos-en-suggested-lemmas/REQUIREMENT.md#F-INCEXP-R004)) se conservan: si dos lemas coincidentes aparecían en cierto orden relativo sin el filtro, mantienen ese mismo orden relativo con el filtro aplicado.

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R007"></a>
### Rule[F-SLREGEX-R007] - El limit recorta después de aplicar el regex
**Severity**: major | **Validation**: VALIDATED

> El `limit` se aplica como último paso, sobre el conjunto ya filtrado por `--regex` y ordenado por prioridad: la respuesta devuelve como máximo `limit` lemas de entre los que coinciden con el patrón.

<details><summary>Detalle</summary>

El orden de operaciones observable es: filtrar por `Level`, `Part of Speech` y condición de exposición; conservar sólo los lemas que coinciden con el patrón; ordenar por prioridad de recomendación; y recién entonces recortar por `limit`. Así, el `limit` cuenta lemas que ya pasaron el regex, no candidatos descartados por el patrón. Si tras aplicar el regex quedan menos lemas que el `limit`, se devuelven todos los disponibles sin error (paridad con [F-CSLATDC-R009](../2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli/REQUIREMENT.md#F-CSLATDC-R009)).

**Error**: "No aplica."

</details>

<a id="F-SLREGEX-R008"></a>
### Rule[F-SLREGEX-R008] - Patrón inválido rechaza la consulta con validación clara
**Severity**: critical | **Validation**: VALIDATED

> Cuando el patrón informado en `--regex` está mal formado, la consulta se rechaza con una validación clara y no produce resultados ni una falla inesperada.

<details><summary>Detalle</summary>

Un patrón sintácticamente inválido (por ejemplo un grupo o una clase de caracteres sin cerrar) no se trata como "sin coincidencias": se trata como una consulta inválida. El sistema rechaza la consulta con un mensaje de validación claro y funcional, coherente con el estilo de las demás validaciones de la consulta de suggested-lemmas, en lugar de devolver una lista vacía o interrumpirse de forma abrupta.

**Error**: "El patrón de búsqueda informado en --regex no es válido." [ASSUMPTION]

</details>

<a id="F-SLREGEX-R009"></a>
### Rule[F-SLREGEX-R009] - Sin coincidencias devuelve lista vacía sin error
**Severity**: major | **Validation**: VALIDATED

> Cuando ningún lema candidato coincide con el patrón, la respuesta es una lista vacía con una explicación funcional clara, sin error.

<details><summary>Detalle</summary>

Que un patrón válido no coincida con ningún lema es un resultado legítimo, no una falla. En ese caso la respuesta es una lista vacía con una explicación funcional, en paridad con el comportamiento de "lista vacía sin error" ya vigente en la consulta ([F-CSLATDC-R012](../2026-05-21.05_consultar-suggested-lemmas-a-trav-s-del-cli/REQUIREMENT.md#F-CSLATDC-R012), [F-INCEXP-R009](../2026-06-18.01_incluir-lemas-ya-expuestos-en-suggested-lemmas/REQUIREMENT.md#F-INCEXP-R009)). El sistema no inventa lemas para llenar la respuesta cuando el patrón no coincide con ninguno.

**Error**: "Ningún lema sugerido coincide con el patrón informado en --regex." [ASSUMPTION]

</details>

<a id="F-SLREGEX-R010"></a>
### Rule[F-SLREGEX-R010] - La ayuda del comando documenta la semántica del filtro
**Severity**: major | **Validation**: VALIDATED

> La ayuda del comando (`--help`) debe documentar la semántica de `--regex`: que el patrón coincide contra el texto del lema, que la coincidencia es parcial estilo grep (se ancla con `^` y `$`) y que es insensible a mayúsculas y minúsculas, con al menos un ejemplo.

<details><summary>Detalle</summary>

El usuario remarcó explícitamente que esta capacidad debe quedar bien documentada de cara a quien usa el comando. Por eso la documentación de ayuda es un requisito observable, no opcional: al consultar la ayuda del comando, el texto del filtro `--regex` debe dejar claro, en lenguaje de quien lo usa, los tres aspectos de la semántica.

El texto de ayuda debe comunicar, como mínimo:

| Aspecto a documentar | Qué debe quedar claro |
|----------------------|-----------------------|
| Qué matchea | El patrón se compara con el texto (la forma) del lema sugerido, no con su tipo gramatical ni otros campos. |
| Coincidencia parcial | El patrón coincide si aparece en cualquier parte del lema; se ancla con `^` (inicio) y `$` (final) cuando se quiere algo más estricto. |
| Insensible a la caja | La coincidencia ignora mayúsculas y minúsculas. |
| Ejemplos | Al menos un ejemplo ilustrativo, por ejemplo `ing$` para terminaciones, `^un` para prefijos o `tt` para secuencias internas. |

La ayuda es la superficie observable de esta regla: un cliente que pide la ayuda del comando obtiene la descripción de la semántica de `--regex`. No se prescribe la redacción exacta, sólo que esos puntos estén cubiertos.

**Error**: "No aplica."

</details>

## Context

La consulta dinámica de `Suggested Lemmas` introducida por [FEAT-CSLATDC](#references) permite acotar la lista por `Level` (obligatorio), `Part of Speech`, `limit` y orden por prioridad, y [FEAT-INCEXP](#references) sumó la opción de incluir lemas ya expuestos. Ambas features cerraron su alcance dejando explícitamente afuera cualquier filtro léxico textual: el `OUT` de [FEAT-CSLATDC](#references) dice "No se agregan filtros léxicos adicionales fuera de `partOfSpeech`, `level` y `limit`", y el de [FEAT-INCEXP](#references) repite la misma restricción.

En la práctica, el agente revisor necesita con frecuencia buscar tipos de palabra específicos por su forma superficial: verbos terminados en cierta secuencia, palabras con un prefijo, lemas que contienen una combinación de letras. Pedir un `Part of Speech` entero es demasiado grueso para ese objetivo, y no existe hoy ninguna forma de filtrar por el texto del lema. Esa es la fricción que esta feature resuelve.

Esta feature es una evolución aditiva que supera deliberadamente la decisión de `OUT` de las dos features anteriores: introduce el primer filtro léxico textual sobre la consulta. El filtro es opcional; sin `--regex` el comportamiento por defecto queda idéntico al actual ([F-SLREGEX-R001](#F-SLREGEX-R001)). Con `--regex`, el patrón se evalúa sólo contra el texto del lema ([F-SLREGEX-R002](#F-SLREGEX-R002)), con coincidencia parcial estilo grep ([F-SLREGEX-R003](#F-SLREGEX-R003)) e insensible a la caja ([F-SLREGEX-R004](#F-SLREGEX-R004)), combinándose con los filtros existentes sin relajarlos ([F-SLREGEX-R005](#F-SLREGEX-R005)) y preservando el orden por prioridad ([F-SLREGEX-R006](#F-SLREGEX-R006)). Por pedido explícito del usuario, la semántica del filtro debe quedar documentada en la ayuda del comando ([F-SLREGEX-R010](#F-SLREGEX-R010)).

## Scope

### IN

- Agregar el filtro `--regex` a `GetCommand.get("suggested-lemmas", <taskId>, ...)`.
- Mantener el comportamiento por defecto idéntico al de [FEAT-CSLATDC](#references) y [FEAT-INCEXP](#references) cuando `--regex` no se informa.
- Evaluar el patrón únicamente contra el texto (forma superficial) del lema sugerido.
- Coincidencia parcial estilo grep (no anclada), con anclaje explícito mediante `^` y `$`.
- Coincidencia insensible a mayúsculas y minúsculas.
- Combinar `--regex` de forma aditiva con `Level`, `Part of Speech`, `limit` y la opción de incluir lemas ya expuestos, sin relajar ninguno.
- Preservar el orden por prioridad de recomendación sobre los lemas que coinciden.
- Aplicar el `limit` después del filtrado por patrón.
- Rechazar la consulta con una validación clara cuando el patrón está mal formado.
- Devolver lista vacía con explicación funcional cuando el patrón no coincide con ningún lema, sin error.
- Documentar en la ayuda del comando la semántica del filtro (qué matchea, parcial/grep-like, insensible a la caja, con ejemplos).

### OUT

- No se cambia el comportamiento por defecto de la consulta sin `--regex`.
- No se introduce un nuevo verbo de consulta.
- No se evalúa el patrón contra el `Part of Speech` ni contra otros campos del resultado distintos del texto del lema.
- No se relaja ni reemplaza ningún filtro existente (`Level`, `Part of Speech`, `limit`, inclusión de lemas ya expuestos).
- No se cambia el criterio de prioridad ni el de desempate existentes.
- No se agregan otros filtros léxicos fuera de `--regex` y los ya soportados.
- No se define en esta iteración el dialecto exacto de expresiones regulares soportado más allá de lo observable en las reglas (ver [DOUBT-REGEX-DIALECT](#DOUBT-REGEX-DIALECT)).

## User Journeys

### Journey[F-SLREGEX-J001] - El agente filtra los lemas sugeridos por patrón de texto

```yaml
flow:
  - id: task_with_candidates
    action: "Existe una Task/Quiz identificada por taskId con lemas candidatos para el Level y Part of Speech pedidos"
    then: request_with_regex

  - id: request_with_regex
    action: "El agente consulta los suggested-lemmas para esa task informando un patrón en --regex, junto con limit y los filtros habituales"
    then: validate_pattern

  - id: validate_pattern
    action: "El sistema valida que el patrón informado en --regex esté bien formado"
    gate: [F-SLREGEX-R008]
    outcomes:
      - when: "El patrón está bien formado"
        then: apply_existing_filters
      - when: "El patrón está mal formado"
        then: reject_invalid_pattern

  - id: apply_existing_filters
    action: "El sistema aplica los filtros existentes (Level, Part of Speech, condición de exposición) sobre los candidatos, sin relajarlos"
    gate: [F-SLREGEX-R005]
    then: apply_regex_filter

  - id: apply_regex_filter
    action: "El sistema conserva únicamente los lemas cuyo texto coincide con el patrón, con coincidencia parcial estilo grep e insensible a mayúsculas y minúsculas"
    gate: [F-SLREGEX-R002, F-SLREGEX-R003, F-SLREGEX-R004]
    then: order_and_limit

  - id: order_and_limit
    action: "El sistema preserva el orden por prioridad sobre los lemas que coinciden y recorta por limit"
    gate: [F-SLREGEX-R006, F-SLREGEX-R007]
    outcomes:
      - when: "Al menos un lema coincide con el patrón"
        then: return_matching_results
      - when: "Ningún lema coincide con el patrón"
        then: return_empty_results

  - id: return_matching_results
    action: "La respuesta contiene hasta limit lemas cuyo texto coincide con el patrón, en el mismo orden de prioridad que tendrían sin el filtro"
    gate: [F-SLREGEX-R006, F-SLREGEX-R007]
    result: success

  - id: return_empty_results
    action: "La respuesta es una lista vacía con una explicación funcional de que ningún lema coincide con el patrón, sin error"
    gate: [F-SLREGEX-R009]
    result: success

  - id: reject_invalid_pattern
    action: "La respuesta rechaza la consulta con una validación clara porque el patrón informado en --regex no es válido"
    gate: [F-SLREGEX-R008]
    result: failure
```

### Journey[F-SLREGEX-J002] - Contraste entre la consulta por defecto y la consulta con --regex

```yaml
flow:
  - id: setup_candidates
    action: "Existe una Task/Quiz identificada por taskId donde, para el mismo Level y Part of Speech, algunos lemas candidatos coinciden con un patrón de texto y otros no"
    then: query_default

  - id: query_default
    action: "El agente consulta los suggested-lemmas para esa task sin informar --regex"
    gate: [F-SLREGEX-R001]
    then: query_with_regex

  - id: query_with_regex
    action: "El agente repite la consulta para la misma task, Level y Part of Speech, informando un patrón en --regex que coincide con un subconjunto de los lemas"
    gate: [F-SLREGEX-R002, F-SLREGEX-R003]
    then: compare_results

  - id: compare_results
    action: "La consulta con --regex devuelve únicamente los lemas cuyo texto coincide con el patrón, en el mismo orden relativo de prioridad que tenían en la consulta por defecto, sin lemas que no coinciden"
    gate: [F-SLREGEX-R005, F-SLREGEX-R006]
    result: success
```

## Open Questions

<a id="DOUBT-REGEX-DIALECT"></a>
### Doubt[DOUBT-REGEX-DIALECT] - Dialecto de expresiones regulares soportado

Quedó acordada la semántica observable (coincidencia parcial estilo grep, anclaje con `^` y `$`, insensible a la caja) y el rechazo de patrones mal formados. No quedó definido qué dialecto exacto de expresiones regulares se soporta más allá de esos elementos (por ejemplo, clases de caracteres, cuantificadores, grupos, alternancia), ni si hay construcciones que se documentarán como no soportadas. Las reglas actuales sólo fijan lo observable; la definición fina del dialecto queda diferida.

<a id="DOUBT-EXACT-ERROR-COPY-REGEX"></a>
### Doubt[DOUBT-EXACT-ERROR-COPY-REGEX] - Textos exactos de validación y de lista vacía

Los textos de error para patrón inválido ([F-SLREGEX-R008](#F-SLREGEX-R008)) y de lista vacía sin coincidencias ([F-SLREGEX-R009](#F-SLREGEX-R009)) están marcados como `[ASSUMPTION]`. Quedó acordado que las validaciones deben ser claras y funcionales, pero no se fijó la redacción exacta. Esta duda es coherente con `DOUBT-EXACT-ERROR-COPY` de [FEAT-CSLATDC](#references); la decisión debería mantenerse consistente con el estilo de las validaciones existentes.

## References

- **FEAT-CSLATDC** — Define la consulta dinámica de `Suggested Lemmas` por `taskId` que esta feature evoluciona de forma aditiva: identidad por task, filtro obligatorio por `Level`, filtro por `Part of Speech`, `limit`, orden por prioridad y lista vacía sin error. Su `OUT` excluía explícitamente filtros léxicos textuales; esta feature supera esa decisión introduciendo `--regex`. El comportamiento por defecto preservado por [F-SLREGEX-R001](#F-SLREGEX-R001) incluye el suyo. Citada por [F-SLREGEX-R005](#F-SLREGEX-R005), [F-SLREGEX-R007](#F-SLREGEX-R007) y [F-SLREGEX-R009](#F-SLREGEX-R009).
- **FEAT-INCEXP** — Agrega la opción de incluir lemas ya expuestos en la consulta dinámica y reafirmaba el mismo `OUT` de no agregar filtros léxicos. `--regex` se combina de forma aditiva con esa opción sin relajarla, y preserva su orden por prioridad y su desempate por frecuencia COCA ascendente. El comportamiento por defecto preservado por [F-SLREGEX-R001](#F-SLREGEX-R001) incluye el suyo. Citada por [F-SLREGEX-R005](#F-SLREGEX-R005) y [F-SLREGEX-R006](#F-SLREGEX-R006).
