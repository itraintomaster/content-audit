---
feature:
  id: FEAT-RCLALEN
  code: F-RCLALEN
  name: Conciencia de Longitud de Oracion en el Contexto de Correccion LEMMA_ABSENCE
  priority: high
---

# Conciencia de Longitud de Oracion en el Contexto de Correccion LEMMA_ABSENCE

## TL;DR

**Que**: Enriquece el contexto de correccion LEMMA_ABSENCE con la longitud actual de la oracion del quiz (tokens, rango esperado para el nivel CEFR, desviacion respecto al rango y direccion accionable de ajuste), leyendo el diagnostico de longitud que ya existe sobre el mismo nodo quiz.

**Por que**: Hoy el LLM que reescribe la oracion no sabe si esta en el limite alto o bajo del rango de tokens para su nivel; agregar esa senal evita que la correccion empuje la oracion fuera de rango y dispare en la siguiente auditoria una tarea SENTENCE_LENGTH redundante.

## Reglas de Negocio

### Grupo A - Que se agrega al contexto y de donde sale

<a id="F-RCLALEN-R001"></a>
### Rule[F-RCLALEN-R001] - Campos de longitud en el contexto de correccion LEMMA_ABSENCE
**Severity**: high | **Validation**: AUTO_VALIDATED

> El contexto de correccion definido en F-RCLA-R003 se extiende con `tokenCount`, `targetMin`, `targetMax`, `delta` y `lengthDirection`, que describen el estado de longitud actual de la oracion del quiz y la direccion en que el LLM deberia llevarla al reemplazar las palabras fuera de nivel.

<details><summary>Detail</summary>

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| tokenCount | Entero (opcional) | SentenceLengthDiagnosis.tokenCount | Cantidad actual de tokens en la oracion del quiz |
| targetMin | Entero (opcional) | SentenceLengthDiagnosis.targetMin | Minimo de tokens esperados para el nivel CEFR del quiz |
| targetMax | Entero (opcional) | SentenceLengthDiagnosis.targetMax | Maximo de tokens esperados para el nivel CEFR del quiz |
| delta | Entero (opcional) | SentenceLengthDiagnosis.delta | Desviacion respecto al rango: 0 si esta dentro, positivo si excede el maximo, negativo si esta por debajo del minimo |
| lengthDirection | Enum | Derivado del delta (ver [F-RCLALEN-R002](#F-RCLALEN-R002)) | Indicacion accionable para el LLM: SHORTEN, LENGTHEN, KEEP_SAME o UNKNOWN |

Los nombres `tokenCount`, `targetMin`, `targetMax` y `delta` son **los mismos** que usa [F-RCSL-R001](../2026-04-05.02_refiner-correction-context-slen/REQUIREMENT.md#F-RCSL-R001) para el contexto SENTENCE_LENGTH. La razon es que ambos contextos describen la misma realidad observable y deben hablar el mismo idioma para que un LLM (o un humano leyendo la salida) los entienda sin diccionario.

Estos campos **no** sustituyen ni alteran los definidos en F-RCLA-R003, F-RCLA-R004 ni F-RCLA-R004b. Solo se agregan al mismo registro.

</details>

<a id="F-RCLALEN-R002"></a>
### Rule[F-RCLALEN-R002] - Calculo de lengthDirection a partir de delta
**Severity**: high | **Validation**: AUTO_VALIDATED

> `lengthDirection` se deriva del signo de `delta`: positivo => SHORTEN, negativo => LENGTHEN, cero => KEEP_SAME, sin diagnostico de longitud disponible => UNKNOWN.

<details><summary>Detail</summary>

| Condicion | lengthDirection | Significado para el LLM |
|-----------|-----------------|-------------------------|
| delta > 0 | SHORTEN | La oracion ya excede el rango, el LLM debe acortarla mientras reemplaza vocabulario |
| delta < 0 | LENGTHEN | La oracion esta por debajo del rango, el LLM tiene margen para incorporar mas vocabulario sugerido al reescribir |
| delta == 0 | KEEP_SAME | La oracion esta dentro del rango, el LLM debe mantener una longitud similar al reemplazar |
| Sin diagnostico de longitud disponible | UNKNOWN | No hay senal de longitud, el LLM solo se gua por las reglas existentes de FEAT-RCLA |

Pre-calcular esta direccion del lado del sistema (en lugar de delegar la aritmetica al prompt del LLM) cumple dos objetivos: hace deterministica la senal que recibe el LLM y permite que la salida CLI muestre una etiqueta legible para humanos.

</details>

<a id="F-RCLALEN-R003"></a>
### Rule[F-RCLALEN-R003] - Origen de datos: SentenceLengthDiagnosis del mismo nodo quiz
**Severity**: high | **Validation**: AUTO_VALIDATED

> Los campos definidos en [F-RCLALEN-R001](#F-RCLALEN-R001) se obtienen del `SentenceLengthDiagnosis` del **mismo nodo quiz** del arbol de auditoria al que F-RCLA-R005 ya navega para obtener el `LemmaPlacementDiagnosis`.

<details><summary>Detail</summary>

El acceso es a traves del metodo tipado `QuizDiagnoses.getSentenceLengthDiagnosis()` definido en [F-DSLEN-R004](../2026-04-05.01_typed-diagnostics-sentence-length/REQUIREMENT.md#F-DSLEN-R004).

Esto significa que:

1. No se introduce un nuevo recorrido del arbol. La resolucion del nodo quiz es la misma de F-RCLA-R005.
2. No se introduce un nuevo analyzer. El diagnostico ya existe en el reporte gracias a FEAT-SLEN y FEAT-DSLEN.
3. Si el nodo quiz tiene `LemmaPlacementDiagnosis` (que es la condicion para que la tarea LEMMA_ABSENCE exista), normalmente tambien tendra `SentenceLengthDiagnosis` porque ambos analizadores corren sobre los mismos quizzes-oracion. La excepcion son los quizzes excluidos por [F-DSLEN-R002](../2026-04-05.01_typed-diagnostics-sentence-length/REQUIREMENT.md#F-DSLEN-R002) (no son oracion); ver [F-RCLALEN-R004](#F-RCLALEN-R004) para el manejo.

</details>

<a id="F-RCLALEN-R004"></a>
### Rule[F-RCLALEN-R004] - Comportamiento cuando SentenceLengthDiagnosis esta ausente
**Severity**: major | **Validation**: AUTO_VALIDATED

> Si el nodo quiz tiene `LemmaPlacementDiagnosis` pero no tiene `SentenceLengthDiagnosis` disponible, el contexto de correccion se construye igualmente: `lengthDirection` se establece a `UNKNOWN` y los valores de `tokenCount`, `targetMin`, `targetMax` y `delta` no se exponen al consumidor. `lengthDirection == UNKNOWN` es el indicador unico de que no hay diagnostico de longitud disponible.

<details><summary>Detail</summary>

Concretamente, lo que ve el consumidor del contexto:

- `lengthDirection` toma el valor `UNKNOWN` y este es el unico discriminador de la ausencia de diagnostico de longitud.
- Los valores de `tokenCount`, `targetMin`, `targetMax` y `delta` no llegan al consumidor: se omiten del JSON ([F-RCLALEN-R005](#F-RCLALEN-R005)), no forman parte del prompt que recibe el LLM, y la salida texto los reemplaza por la marca `(unavailable)` en la linea `Length:` ([F-RCLALEN-R006](#F-RCLALEN-R006)).
- El resto de los campos del contexto (los definidos por F-RCLA-R003 a F-RCLA-R004b) se llenan normalmente.

Casos en los que esto puede ocurrir: el quiz fue excluido del analyzer de longitud por [F-DSLEN-R002](../2026-04-05.01_typed-diagnostics-sentence-length/REQUIREMENT.md#F-DSLEN-R002), o la estructura de diagnosticos del nodo no expone el campo.

La justificacion para no fallar: la informacion de longitud es un **enriquecedor**, no la informacion central de LEMMA_ABSENCE. La informacion central (lemas fuera de nivel y lemas sugeridos) sigue siendo suficiente para que el LLM reescriba la oracion. La ausencia de la senal de longitud simplemente significa que el LLM no tiene esa pista adicional, no que no pueda corregir.

Esto es coherente con como F-RCLA-R004c trata la ausencia de lemas sugeridos: degradacion silenciosa, no error.

</details>

---

### Grupo B - Como se ve el contexto enriquecido en la salida

<a id="F-RCLALEN-R005"></a>
### Rule[F-RCLALEN-R005] - Extension del formato JSON con campos de longitud
**Severity**: high | **Validation**: AUTO_VALIDATED

> La salida JSON del contexto de correccion definida en F-RCLA-R008 se extiende con `tokenCount`, `targetRange: { min, max }`, `delta` y `lengthDirection`. Cuando no hay diagnostico de longitud disponible, los campos numericos se omiten y `lengthDirection` se serializa con valor `"UNKNOWN"`.

<details><summary>Detail</summary>

Ejemplo cuando hay diagnostico de longitud disponible y la oracion excede el rango:

```
{
  "taskNumber": 1,
  "totalTasks": 42,
  "id": "task-014",
  "target": "QUIZ",
  "nodeId": "quiz-id-123",
  "nodeLabel": "Quiz 14 - L1.T2.K3",
  "diagnosis": "LEMMA_ABSENCE",
  "priority": 1,
  "status": "PENDING",
  "correctionContext": {
    "sentence": "She needs to negotiate the contract before Friday afternoon",
    "translation": "Ella necesita negociar el contrato antes del viernes por la tarde",
    "knowledgeTitle": "Affirmative sentences in the present simple",
    "knowledgeInstructions": "Escribe la forma afirmativa",
    "topicLabel": "Present Simple",
    "cefrLevel": "A1",
    "tokenCount": 10,
    "targetRange": { "min": 5, "max": 8 },
    "delta": 2,
    "lengthDirection": "SHORTEN",
    "misplacedLemmas": [
      { "lemma": "negotiate", "pos": "VERB", "expectedLevel": "B2", "quizLevel": "A1", "cocaRank": 2840 },
      { "lemma": "contract", "pos": "NOUN", "expectedLevel": "B1", "quizLevel": "A1", "cocaRank": 1205 }
    ],
    "suggestedLemmas": [
      { "lemma": "like", "pos": "VERB", "reason": "COMPLETELY_ABSENT", "cocaRank": 52 }
    ]
  }
}
```

Cuando no hay diagnostico de longitud disponible ([F-RCLALEN-R004](#F-RCLALEN-R004)), los campos `tokenCount`, `targetRange` y `delta` se omiten del JSON, y `lengthDirection` se incluye con valor `"UNKNOWN"`. El resto del registro se serializa igual que en F-RCLA-R008.

El agrupamiento `targetRange: { min, max }` es identico al usado por [F-RCSL-R007](../2026-04-05.02_refiner-correction-context-slen/REQUIREMENT.md#F-RCSL-R007) para el contexto SENTENCE_LENGTH, manteniendo la consistencia entre ambos contextos.

</details>

<a id="F-RCLALEN-R006"></a>
### Rule[F-RCLALEN-R006] - Extension del formato texto con campos de longitud
**Severity**: major | **Validation**: AUTO_VALIDATED

> La salida texto del contexto de correccion definida en F-RCLA-R009 se extiende mostrando una linea `Length:` entre `CEFR Level` y `Misplaced lemmas`, con tokens actuales, rango objetivo, delta con signo explicito y direccion derivada.

<details><summary>Detail</summary>

Ejemplo cuando la oracion excede el rango:

```
Next task (#1 of 42):
  Target:    QUIZ
  Node:      Quiz 14 - L1.T2.K3 (id: quiz-id-123)
  Diagnosis: LEMMA_ABSENCE
  Priority:  1
  Status:    PENDING

  Correction context:
    Sentence:     She needs to negotiate the contract before Friday afternoon
    Translation:  Ella necesita negociar el contrato antes del viernes por la tarde
    Knowledge:    Affirmative sentences in the present simple
    Instructions: Escribe la forma afirmativa
    Topic:        Present Simple
    CEFR Level:   A1
    Length:       10 tokens (target: 5-8, delta: +2, direction: SHORTEN)
    Misplaced lemmas:
      1. negotiate (VERB) - expected B2, found in A1 [COCA #2840]
      2. contract (NOUN) - expected B1, found in A1 [COCA #1205]
    Suggested replacements:
      1. like (VERB) - COMPLETELY_ABSENT [COCA #52]
```

Variantes de la linea `Length:`:

- Oracion dentro del rango (delta == 0): `Length:       6 tokens (target: 5-8, delta: 0, direction: KEEP_SAME)`
- Oracion demasiado corta (delta < 0): `Length:       3 tokens (target: 5-8, delta: -2, direction: LENGTHEN)`
- Sin diagnostico de longitud: `Length:       (unavailable, direction: UNKNOWN)`

El formato del numero `delta` muestra signo explicito cuando es positivo (`+2`), sin signo cuando es cero (`0`), y con signo cuando es negativo (`-2`). Esto sigue la misma convencion que usa [F-RCSL-R008](../2026-04-05.02_refiner-correction-context-slen/REQUIREMENT.md#F-RCSL-R008) para el contexto SENTENCE_LENGTH (linea `Tokens: ... delta: +7`).

</details>

---

## Contexto

### El gap actual

Cuando el LLM recibe la tarea LEMMA_ABSENCE para un quiz A1 cuya oracion ya tiene 8 tokens (rango A1: 5-8), no sabe que esta en el limite. Si los lemas sugeridos lo invitan a anadir una palabra rica de vocabulario, podria empujar la oracion fuera de rango. La consecuencia practica es que la siguiente auditoria generara una tarea SENTENCE_LENGTH para esa misma oracion, creando un ciclo de correccion innecesario.

A la inversa, cuando la oracion tiene 4 tokens y el rango A1 es 5-8, el LLM tiene margen para incorporar mas vocabulario sugerido, pero hoy no lo sabe.

### Origen de los datos: ya existen

El diagnostico necesario ya esta producido por el analizador de longitud de oraciones (FEAT-SLEN) y expuesto como registro tipado por FEAT-DSLEN. El **mismo nodo quiz** que hoy provee `LemmaPlacementDiagnosis` (consumido por FEAT-RCLA) tambien provee `SentenceLengthDiagnosis` accesible via `QuizDiagnoses.getSentenceLengthDiagnosis()` ([F-DSLEN-R004](../2026-04-05.01_typed-diagnostics-sentence-length/REQUIREMENT.md#F-DSLEN-R004)).

No se introduce ningun nuevo analyzer, ningun nuevo diagnostico y ningun nuevo recorrido del arbol. Solo se lee un diagnostico hermano del que ya se lee.

### Por que reusar la nomenclatura de RCSL

Los nombres `tokenCount`, `targetMin`, `targetMax` y `delta` se reusan a proposito de [F-RCSL-R001](../2026-04-05.02_refiner-correction-context-slen/REQUIREMENT.md#F-RCSL-R001), que ya los aplica al contexto SENTENCE_LENGTH. Ambos contextos describen la misma realidad observable (longitud de una oracion respecto al rango esperado para su nivel CEFR), y un consumidor (LLM o humano) que ya leyo uno debe poder leer el otro sin diccionario adicional. El criterio de naming, el origen de datos y el formato de presentacion en CLI siguen el patron ya establecido alli.

### Actor principal

El consumidor sigue siendo un **LLM** que reescribe la oracion. Este requerimiento le da una senal mas: cuanto margen tiene para mover la longitud al hacerlo.

---

## Alcance

**En alcance**:

- Enriquecimiento del registro existente del contexto LEMMA_ABSENCE con cinco campos de longitud.
- Extension de las dos salidas existentes (JSON y texto) para mostrar esos campos.
- Derivacion de `lengthDirection` del lado del sistema a partir de `delta`, no delegada al LLM.

**Limitaciones explicitas de esta iteracion** (decisiones de alcance, no reglas de negocio):

- **Sin estadisticas de longitud a nivel curso**: no se incluyen metricas como promedio, mediana o p90 de tokens en quizzes del mismo nivel CEFR del curso. Esa informacion requeriria un nuevo agregador y un nuevo registro de diagnostico, que no existen hoy. Ver [DOUBT-COURSE-LENGTH-TRENDS](#DOUBT-COURSE-LENGTH-TRENDS).
- **Sin recalculo post-correccion**: si la oracion reescrita por el LLM termina fuera del rango de longitud, eso se detectara en la proxima auditoria como una tarea SENTENCE_LENGTH. Este requerimiento no intenta verificar la salida del LLM (mismo criterio que la limitacion ya documentada en FEAT-RCLA).
- **Sin pesado entre senales contradictorias**: si la oracion esta cerca del minimo (`LENGTHEN`) y los lemas sugeridos podrian aumentarla, no se intenta resolver el conflicto. La direccion se reporta tal cual; el LLM decide. La interaccion entre `lengthDirection` y la cantidad de `suggestedLemmas` a usar es responsabilidad del prompt del LLM, no del sistema.
- **`lengthDirection` no es vinculante**: el LLM puede decidir, dado el resto del contexto, cambiar la longitud aunque la direccion sea `KEEP_SAME` (por ejemplo, si reemplazar dos palabras de B2 por dos palabras de A1 implica naturalmente reducir un token). El sistema no impone restricciones sobre la oracion final.
- **Sin nuevas reglas para FEAT-RCLA**: F-RCLA-R001..F-RCLA-R009 siguen vigentes sin cambios. La mecanica de re-routing y la construccion del contexto base son las de FEAT-RCLA; aqui solo se anaden campos al registro y se ajusta la presentacion en CLI.

---

## User Journeys

### Journey[F-RCLALEN-J001] - LLM recibe contexto LEMMA_ABSENCE con senal de acortar para una oracion en limite alto
**Validation**: AUTO_VALIDATED

Cubre el flujo completo: desde la solicitud de tarea hasta la presentacion del contexto enriquecido, pasando por las tres ramas posibles del calculo de `lengthDirection` y la rama de ausencia de diagnostico de longitud.

```yaml
journeys:
  - id: F-RCLALEN-J001
    name: LLM recibe contexto LEMMA_ABSENCE con senal de acortar para una oracion en limite alto
    flow:
      - id: solicitar_tarea
        action: "El usuario ejecuta 'content-audit get tasks --status pending --sort priority --limit 1' y obtiene una tarea LEMMA_ABSENCE con target QUIZ"
        then: construir_contexto_base

      - id: construir_contexto_base
        action: "El sistema construye el contexto de correccion base de FEAT-RCLA con la oracion, traduccion, contexto pedagogico, lemas fuera de nivel y lemas sugeridos"
        then: leer_diagnostico_longitud

      - id: leer_diagnostico_longitud
        action: "El sistema lee el SentenceLengthDiagnosis del mismo nodo quiz que ya se uso para el LemmaPlacementDiagnosis"
        gate: [F-RCLALEN-R003]
        outcomes:
          - when: "El SentenceLengthDiagnosis esta presente en el nodo quiz"
            then: clasificar_direccion
          - when: "El SentenceLengthDiagnosis esta ausente"
            then: marcar_unknown

      - id: clasificar_direccion
        action: "El sistema lee tokenCount, targetMin, targetMax y delta del diagnostico, y deriva lengthDirection a partir del signo de delta"
        gate: [F-RCLALEN-R001, F-RCLALEN-R002]
        outcomes:
          - when: "delta es mayor que cero (oracion excede el rango)"
            then: completar_shorten
          - when: "delta es exactamente cero (oracion dentro del rango)"
            then: completar_keep_same
          - when: "delta es menor que cero (oracion por debajo del rango)"
            then: completar_lengthen

      - id: completar_shorten
        action: "El contexto se enriquece con tokenCount, targetMin, targetMax, delta positivo y lengthDirection = SHORTEN"
        gate: [F-RCLALEN-R001]
        then: presentar_contexto

      - id: completar_keep_same
        action: "El contexto se enriquece con tokenCount, targetMin, targetMax, delta = 0 y lengthDirection = KEEP_SAME"
        gate: [F-RCLALEN-R001]
        then: presentar_contexto

      - id: completar_lengthen
        action: "El contexto se enriquece con tokenCount, targetMin, targetMax, delta negativo y lengthDirection = LENGTHEN"
        gate: [F-RCLALEN-R001]
        then: presentar_contexto

      - id: marcar_unknown
        action: "El contexto se enriquece sin tokenCount, sin targetMin, sin targetMax, sin delta, y con lengthDirection = UNKNOWN"
        gate: [F-RCLALEN-R004]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra la tarea con el contexto de correccion enriquecido en el formato solicitado (texto o JSON), incluyendo la linea de longitud y la direccion calculada"
        gate: [F-RCLALEN-R005, F-RCLALEN-R006]
        result: success
```

### Journey[F-RCLALEN-J002] - LLM recibe contexto LEMMA_ABSENCE con senal de mantener longitud
**Validation**: AUTO_VALIDATED

Camino concreto: oracion dentro del rango. Ilustra el caso `KEEP_SAME` end-to-end con valores realistas (6 tokens en rango A1 5-8).

```yaml
journeys:
  - id: F-RCLALEN-J002
    name: LLM recibe contexto LEMMA_ABSENCE con senal de mantener longitud
    flow:
      - id: solicitar_tarea
        action: "El usuario obtiene una tarea LEMMA_ABSENCE para un quiz cuya oracion ya esta dentro del rango de tokens esperado para su nivel CEFR"
        then: construir_y_enriquecer

      - id: construir_y_enriquecer
        action: "El sistema construye el contexto base de FEAT-RCLA y lo enriquece leyendo el SentenceLengthDiagnosis del mismo nodo quiz"
        gate: [F-RCLALEN-R001, F-RCLALEN-R003]
        then: derivar_direccion

      - id: derivar_direccion
        action: "El sistema deriva lengthDirection del delta. El quiz tiene 6 tokens y el rango es 5-8, por lo que delta es 0 y lengthDirection se establece a KEEP_SAME"
        gate: [F-RCLALEN-R002]
        then: presentar_contexto

      - id: presentar_contexto
        action: "El comando muestra el contexto enriquecido. La linea Length indica '6 tokens (target: 5-8, delta: 0, direction: KEEP_SAME)', senalizando al LLM que reemplace el vocabulario sin alterar la longitud significativamente"
        gate: [F-RCLALEN-R005, F-RCLALEN-R006]
        result: success
```

---

## Open Questions

<a id="DOUBT-COURSE-LENGTH-TRENDS"></a>
### Doubt[DOUBT-COURSE-LENGTH-TRENDS] - Incluir estadisticas de longitud a nivel curso?
**Status**: OPEN

El usuario sugirio incluir estadisticas de tendencia de longitud de oracion del curso para el nivel CEFR del quiz (por ejemplo: promedio, mediana o p90 de tokens en quizzes del mismo nivel) para que el LLM sepa no solo el rango teorico permitido sino tambien donde estan apuntando las oraciones reales del curso.

- [ ] Opcion A: Incluir en esta iteracion. Requiere un nuevo agregador a nivel curso, un nuevo registro de diagnostico (e.g., `CourseSentenceLengthTrendDiagnosis` con avg/p50/p90 por nivel CEFR), y agregar tres campos mas al contexto. Es una funcionalidad nueva, no solo enriquecimiento.
- [x] Opcion B: Postergar a una iteracion siguiente. Mantener esta iteracion enfocada en informacion ya disponible. Cuando el agregador exista, este contexto se puede extender de forma analoga sin romper consumidores actuales.
- [ ] Opcion C: Descartar. El rango oficial CEFR es la unica referencia que deberia usar el LLM.

**Answer**: Opcion B. Esta iteracion se limita a exponer datos que ya existen en el reporte (SentenceLengthDiagnosis del mismo nodo quiz). Las estadisticas de tendencia de curso requieren agregacion nueva y deberian especificarse en su propia feature, idealmente despues de medir si la senal `lengthDirection` simple ya soluciona el problema en la practica.

<a id="DOUBT-DIRECTION-WHEN-NEAR-EDGES"></a>
### Doubt[DOUBT-DIRECTION-WHEN-NEAR-EDGES] - Refinar lengthDirection cerca de los bordes del rango?
**Status**: RESOLVED

Cuando la oracion esta dentro del rango pero muy cerca de un borde (por ejemplo, 8 tokens en rango A1 5-8), `lengthDirection` se calcula como `KEEP_SAME` aunque el LLM tenga muy poco margen para alargar. Convendria una variante mas granular como `KEEP_SAME_NEAR_MAX` o `KEEP_SAME_NEAR_MIN`?

- [ ] Opcion A: Anadir las variantes `KEEP_SAME_NEAR_MAX` y `KEEP_SAME_NEAR_MIN` con un umbral configurable.
- [x] Opcion B: Mantener tres direcciones simples (SHORTEN/KEEP_SAME/LENGTHEN) mas UNKNOWN. El LLM puede inferir la cercania al borde a partir de tokenCount, targetMin y targetMax que tambien recibe.

**Answer**: Opcion B. La cercania al borde es derivable de los campos numericos que el LLM ya recibe. Anadir mas variantes de la enum complica la salida sin aportar informacion nueva. Si en el futuro se observa que el LLM ignora la cercania al borde, se puede revisitar.

---

## Referencias

- **FEAT-RCLA** (`requirements/2026-03-28.03_lemma-absence/`) — Define la base del contexto LEMMA_ABSENCE (registro, mecanica de construccion, formatos JSON y texto). Este requerimiento extiende ese registro con campos de longitud sin redefinir las reglas existentes (F-RCLA-R001..F-RCLA-R009 siguen vigentes). Citado por [F-RCLALEN-R001](#F-RCLALEN-R001), [F-RCLALEN-R003](#F-RCLALEN-R003), [F-RCLALEN-R004](#F-RCLALEN-R004), [F-RCLALEN-R005](#F-RCLALEN-R005) y [F-RCLALEN-R006](#F-RCLALEN-R006).
- **FEAT-DSLEN** (`requirements/2026-04-05.01_typed-diagnostics-sentence-length/`) — Provee `SentenceLengthDiagnosis` con `tokenCount`, `targetMin`, `targetMax`, `cefrLevel`, `delta` y `toleranceMargin` a nivel quiz, accesible via `QuizDiagnoses.getSentenceLengthDiagnosis()`. Citado por [F-RCLALEN-R001](#F-RCLALEN-R001), [F-RCLALEN-R003](#F-RCLALEN-R003) y [F-RCLALEN-R004](#F-RCLALEN-R004).
- **FEAT-RCSL** (`requirements/2026-04-05.02_refiner-correction-context-slen/`) — Ya consume estos mismos campos para el contexto SENTENCE_LENGTH; este requerimiento reusa nombres, agrupamiento JSON y convencion de signo para que ambos contextos hablen el mismo idioma. Citado por [F-RCLALEN-R001](#F-RCLALEN-R001), [F-RCLALEN-R005](#F-RCLALEN-R005) y [F-RCLALEN-R006](#F-RCLALEN-R006).
- **FEAT-SLEN** (`requirements/2026-03-18.01_sentence-length/`) — Analizador subyacente que produce el diagnostico de longitud que FEAT-DSLEN expone como registro tipado. No se cita inline porque ningun rule introduce restricciones nuevas sobre el analizador; se menciona solo como dependencia de origen de datos.
