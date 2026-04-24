---
feature:
  id: FEAT-RCLAQS
  code: F-RCLAQS
  name: Exponer quizSentence en el CorrectionContext de LEMMA_ABSENCE
  priority: major
---

# Exponer quizSentence en el CorrectionContext de LEMMA_ABSENCE

FEAT-RCLA construye un `CorrectionContext` para cada tarea de refinamiento de tipo `LEMMA_ABSENCE` con la oracion del quiz en texto plano (`sentence`), la traduccion al espanol, el contexto pedagogico (knowledge, topic, CEFR), la lista de palabras fuera de nivel (`misplacedLemmas`) y lemas sugeridos de reemplazo (`suggestedLemmas`). FEAT-QSENT formalizo la DSL `quizSentence` como representacion textual compacta del ejercicio (tronco + blanks + respuestas aceptadas + hints) y dejo explicitamente como "micro-update posterior" extender el `CorrectionContext` de RCLA para incluir ese campo. FEAT-LAPS, la primera estrategia real de propuesta para `LEMMA_ABSENCE`, consume exactamente ese `quizSentence` como unica fuente de entrada estructural y declara dependencia sobre este cambio.

Este micro-requerimiento es un delta aislado: agrega el campo `quizSentence` al `CorrectionContext` de tareas `LEMMA_ABSENCE` alimentandolo desde la misma fuente de datos desde la que se deriva la `sentence` plana (el `FormEntity` del quiz original). No se modifica ni el modelo de curso, ni la DSL de FEAT-QSENT, ni el resto del `CorrectionContext`, ni la estrategia de propuesta de FEAT-LAPS. La `sentence` plana existente se mantiene sin cambios para preservar a los consumidores actuales que dependen de ella.

## Contexto

### Relacion con features existentes

- **FEAT-RCLA**: aporta el `CorrectionContext` actual con sus campos ya validados y shipeados (48 tests pasando). Este requerimiento **extiende** ese contexto con un campo adicional `quizSentence`. No se tocan las reglas existentes de RCLA (R001, R003, R004, R004b, R004c, R005, R006, R007, R008, R009) ni sus journeys (J001, J002); sus invariantes y salidas actuales se preservan.
- **FEAT-QSENT**: formaliza la DSL `quizSentence` y expone el conversor publico del course-domain (desde `sentenceParts` hacia la DSL y viceversa, con derivacion canonica a plain sentence). Este requerimiento **consume** esa funcionalidad como unica via para obtener el `quizSentence` correspondiente al quiz original; no reimplementa ni paraleliza la conversion.
- **FEAT-LAPS**: estrategia MVP para `LEMMA_ABSENCE` que ya declaro dependencia sobre este cambio (ver FEAT-LAPS R007 y Assumption 1). Este requerimiento **desbloquea** a LAPS: una vez el `quizSentence` esta disponible en el contexto, la estrategia puede operar. Este micro-feature no modifica LAPS.
- **FEAT-CSTRUCT**: provee el `FormEntity` del quiz con su `sentenceParts`, fuente ultima desde la cual se deriva tanto la `sentence` plana existente como el nuevo `quizSentence`. No se modifica ese modelo.

### Alcance deliberado

Este requerimiento es estrictamente un **delta al contexto de correccion**:

- Agrega un campo `quizSentence` (String, DSL de FEAT-QSENT) al `CorrectionContext` de tareas `LEMMA_ABSENCE`.
- Mantiene el campo `sentence` (String, texto plano) existente sin cambios.
- El valor de `quizSentence` se deriva del mismo `FormEntity` del quiz original desde el que ya se deriva la `sentence` plana, delegando la conversion `sentenceParts -> quizSentence` en la funcionalidad publica de FEAT-QSENT.
- La derivacion debe ser consistente con la que hoy produce `sentence`: ambos campos corresponden al mismo quiz y al mismo paso de derivacion; bajo FEAT-QSENT R017/R018/R019 la `sentence` plana es la variante canonica y debe ser equivalente (whitespace-normalizada) a la plain sentence derivada del `quizSentence` de este contexto via el conversor canonico.
- La derivacion falla explicitamente y no se produce `correctionContext` cuando el `FormEntity` del quiz original tiene `sentenceParts` malformado segun las invariantes de FEAT-QSENT. El mecanismo de falla reutiliza el existente de RCLA R005/R006: el contexto no se construye y el comando reporta el motivo.

Quedan **fuera** de este requerimiento:

- Cualquier cambio al resto del `CorrectionContext` (los campos ya validados de FEAT-RCLA).
- Cualquier cambio al modelo de curso, al `FormEntity`, al `SentencePartEntity` o a la persistencia en disco.
- Cualquier cambio a la DSL de FEAT-QSENT o a sus conversiones.
- Cualquier cambio a FEAT-LAPS (este feature se limita a exponer el campo que LAPS consumira).
- Cualquier decision de arquitectura (donde vive la derivacion, como se inyecta el conversor de FEAT-QSENT, etc.) — pertenecen a la fase de arquitectura.
- Cualquier cambio no-obvio a la salida CLI (`get task` / JSON / texto): si es necesario exponer el campo para que sea observable, se cubre en R004; si no es estrictamente necesario, la forma de presentacion queda como decision de arquitectura/producto posterior.

### Actor principal

El consumidor inmediato del nuevo campo es FEAT-LAPS (estrategia MVP), que recibe el `CorrectionContext` con el `quizSentence` ya materializado. El operador humano via CLI es un consumidor indirecto: en el formato JSON y texto actual de `get task`, si el campo se expone, aparecera como parte de la seccion `correctionContext`.

---

## Reglas de Negocio

### Grupo A - Extension del CorrectionContext

### Rule[F-RCLAQS-R001] - El CorrectionContext de LEMMA_ABSENCE expone un campo quizSentence
**Severity**: critical | **Validation**: AUTO_VALIDATED

El `CorrectionContext` que el sistema construye para una tarea de refinamiento cuyo `diagnosisKind` es `LEMMA_ABSENCE` expone, ademas de los campos ya definidos por FEAT-RCLA R003, un campo adicional **`quizSentence`** de tipo texto, cuyo contenido es la DSL de FEAT-QSENT del quiz original asociado a la tarea. Este campo viaja junto con `sentence` (texto plano) y los demas campos existentes del contexto; no los reemplaza.

| Campo | Tipo | Origen | Descripcion |
|-------|------|--------|-------------|
| quizSentence | Texto | Derivado del `FormEntity` del quiz original via el conversor de FEAT-QSENT | Representacion DSL del ejercicio (tronco con blanks `____`, bloques `[variantes|por|pipe]` y hints `(hint)` inline) tal como la define FEAT-QSENT |

**Error**: N/A (esta regla define la adicion de un campo al contexto)

### Rule[F-RCLAQS-R002] - El valor de quizSentence se deriva via el conversor canonico de FEAT-QSENT sobre el mismo FormEntity del quiz original
**Severity**: critical | **Validation**: AUTO_VALIDATED

El valor expuesto en `quizSentence` es el resultado de aplicar la conversion publica `sentenceParts -> quizSentence` de FEAT-QSENT (ver FEAT-QSENT R011, R012) sobre el mismo `FormEntity` del quiz original desde el que hoy se deriva el campo `sentence` plano del contexto. La derivacion es delegacion exclusiva: el sistema no reimplementa la DSL ni paraleliza la conversion fuera de la funcionalidad publica de FEAT-QSENT.

Bajo la equivalencia whitespace-normalizada definida en FEAT-QSENT R010, el `quizSentence` expuesto debe ser identico al producido por el conversor canonico de FEAT-QSENT cuando se le entrega el mismo `sentenceParts`.

**Error**: N/A (esta regla define el origen canonico del campo)

### Rule[F-RCLAQS-R003] - Consistencia entre quizSentence y sentence (plana) dentro del mismo contexto
**Severity**: critical | **Validation**: AUTO_VALIDATED

El `quizSentence` y el `sentence` (plano) expuestos en un mismo `CorrectionContext` corresponden al **mismo quiz** y al **mismo paso de derivacion**. Concretamente, la plain sentence canonica que FEAT-QSENT deriva del `quizSentence` expuesto por este campo (la primera sub-variante del primer entry, sin hints, con whitespace normalizado — FEAT-QSENT R017/R018/R019) debe ser equivalente, bajo normalizacion de whitespace (FEAT-QSENT R010), al `sentence` plano presente en el mismo contexto.

Esta invariante ata el nuevo campo al ya existente: no es admisible que `quizSentence` describa un ejercicio y `sentence` describa otro; tampoco es admisible que se deriven de versiones diferentes del `FormEntity` original. La consistencia es parte del contrato del contexto.

**Error**: "Inconsistencia entre quizSentence y sentence del CorrectionContext para la tarea '{taskId}'"

### Rule[F-RCLAQS-R004] - La derivacion falla atomicamente si el FormEntity original es invalido segun FEAT-QSENT
**Severity**: critical | **Validation**: AUTO_VALIDATED

Si el `FormEntity` del quiz original viola las invariantes de FEAT-QSENT (por ejemplo: un TEXT con `options` no vacio — FEAT-QSENT R003; un CLOZE con `options` nulo o vacio — FEAT-QSENT R004; o cualquier otro caso cubierto por FEAT-QSENT R013/R024), la derivacion del `quizSentence` falla de forma atomica: no se produce un `quizSentence` parcial y, como consecuencia, **no se produce un `CorrectionContext` para la tarea**. La falla se propaga con el mismo mecanismo que hoy usa FEAT-RCLA cuando no logra construir el contexto (FEAT-RCLA R005/R006): el comando reporta que el contexto no pudo construirse y la tarea se muestra sin `correctionContext`.

Esta regla no introduce un mecanismo nuevo de falla; reutiliza el existente de RCLA y lo aplica al caso "el quiz original tiene sentenceParts malformado".

**Error**: "No se pudo derivar el quizSentence del quiz original '{nodeId}' para la tarea '{taskId}': {razon}"

---

### Grupo B - Observabilidad del campo en la salida existente

### Rule[F-RCLAQS-R005] - El campo quizSentence aparece en la salida JSON del contexto
**Severity**: major | **Validation**: AUTO_VALIDATED

Cuando los comandos `get task` o `get tasks --status pending --sort priority --limit 1` muestran una tarea `LEMMA_ABSENCE` en formato JSON y el `correctionContext` esta presente (es decir, no hubo falla de construccion segun FEAT-RCLA R005/R006 ni segun R004 de este feature), el campo `quizSentence` se incluye dentro del objeto `correctionContext` como un campo adicional, junto a los existentes (`sentence`, `translation`, `knowledgeTitle`, etc., definidos en FEAT-RCLA R008). El orden exacto de campos y el formato concreto (prettyprint, escaping) es decision de arquitectura/presentacion; lo unico funcional es que el campo sea observable en la salida JSON.

Ejemplo ilustrativo (no prescriptivo sobre orden):

```
{
  "correctionContext": {
    "quizSentence": "He ____ [is|'s] (to be) great.",
    "sentence": "He is great.",
    "translation": "El es genial.",
    "knowledgeTitle": "...",
    "...": "..."
  }
}
```

Si el contexto no pudo construirse (FEAT-RCLA R005/R006 o R004 de este feature), no hay `correctionContext` y por tanto no hay `quizSentence`; aplica el `correctionContextError` existente de FEAT-RCLA R008.

**Error**: N/A (esta regla define que el campo sea observable en JSON)

---

## Limitaciones de alcance de esta iteracion

Las siguientes limitaciones son decisiones explicitas de alcance, no reglas de negocio:

- **No se deprecia ni se elimina el campo `sentence` plano.** Se mantiene tal cual lo definio FEAT-RCLA R003/R008/R009 para no romper consumidores existentes. La eventual deprecacion queda como DOUBT-DEPRECATE-SENTENCE; mientras no se resuelva, ambos campos coexisten.
- **No se define un formato texto especifico para `quizSentence`.** La salida texto de `get task` (FEAT-RCLA R009) hoy muestra la `sentence` plana; que se muestre el `quizSentence` ademas y de que manera es decision de presentacion posterior. Este requerimiento solo exige la observabilidad en JSON (R005), que es el formato que FEAT-LAPS consume.
- **No se toca FEAT-LAPS.** Este feature expone el dato; LAPS consume el dato. La coordinacion es por contrato del `CorrectionContext`, no por cambios cruzados.
- **No se tocan otros `DiagnosisKind`.** El campo `quizSentence` solo se agrega al contexto de `LEMMA_ABSENCE`. Si en el futuro otros diagnosticos (por ejemplo `SENTENCE_LENGTH` de FEAT-RCSL) quisieran exponer `quizSentence`, seran requerimientos propios.
- **No se cambia la fuente de verdad.** El `FormEntity` del quiz original sigue siendo la unica fuente; el `quizSentence` se deriva de alli en cada construccion de contexto.

---

## User Journeys

### Journey[F-RCLAQS-J001] - El CorrectionContext de LEMMA_ABSENCE expone quizSentence derivado del quiz original
**Validation**: AUTO_VALIDATED

Happy path y fallo de derivacion para el nuevo campo. Cubre R001-R005.

```yaml
journeys:
  - id: F-RCLAQS-J001
    name: El CorrectionContext de LEMMA_ABSENCE expone quizSentence derivado del quiz original
    flow:
      - id: solicitar_tarea
        action: "El operador solicita al sistema una tarea LEMMA_ABSENCE en formato JSON usando el comando get que ya expone el correctionContext"
        then: localizar_quiz

      - id: localizar_quiz
        action: "El sistema localiza el quiz original asociado a la tarea (mecanica heredada de FEAT-RCLA R005) y obtiene el FormEntity con su sentenceParts"
        outcomes:
          - when: "El FormEntity del quiz original se obtiene correctamente"
            then: derivar_quizsentence
          - when: "El FormEntity del quiz original no se puede obtener (ej. reporte de auditoria ausente o nodo quiz no encontrado, caso heredado de FEAT-RCLA R005)"
            then: contexto_no_construido

      - id: derivar_quizsentence
        action: "El sistema delega en la funcionalidad publica de FEAT-QSENT la conversion del sentenceParts del FormEntity original a quizSentence"
        gate: [F-RCLAQS-R001, F-RCLAQS-R002]
        outcomes:
          - when: "El sentenceParts es valido segun las invariantes de FEAT-QSENT y la conversion produce un quizSentence"
            then: construir_contexto_con_quizsentence
          - when: "El sentenceParts es invalido segun FEAT-QSENT (TEXT con options, CLOZE sin options, etc.) y la conversion falla atomicamente"
            then: fallar_derivacion_quizsentence

      - id: construir_contexto_con_quizsentence
        action: "El sistema construye el CorrectionContext con el quizSentence derivado junto a sentence (plano) y los demas campos heredados de FEAT-RCLA, verificando que quizSentence y sentence corresponden al mismo quiz y al mismo paso de derivacion"
        gate: [F-RCLAQS-R001, F-RCLAQS-R003]
        then: exponer_json

      - id: exponer_json
        action: "El sistema emite la tarea en formato JSON incluyendo el campo quizSentence dentro de correctionContext, junto a los campos existentes"
        gate: [F-RCLAQS-R005]
        result: success

      - id: fallar_derivacion_quizsentence
        action: "El sistema aborta la construccion del CorrectionContext para esta tarea y reporta que la derivacion del quizSentence fallo por datos invalidos en el quiz original; la tarea se muestra sin correctionContext y con el correctionContextError heredado de FEAT-RCLA"
        gate: [F-RCLAQS-R004]
        result: failure

      - id: contexto_no_construido
        action: "El sistema no logra construir el CorrectionContext por un motivo heredado de FEAT-RCLA (reporte ausente, nodo no localizado, etc.); la tarea se muestra sin correctionContext con el correctionContextError heredado"
        result: failure
```

---

## Open Questions

### Doubt[DOUBT-DEPRECATE-SENTENCE] - Se debe deprecar o eliminar el campo `sentence` plano una vez que `quizSentence` este disponible?
**Status**: OPEN

Con `quizSentence` en el contexto, cualquier consumidor que necesite la oracion plana puede obtenerla del conversor canonico de FEAT-QSENT sobre el `quizSentence` (la variante canonica es la posicion 0 de la lista de plain sentences de FEAT-QSENT R017/R018/R019). Eso hace tecnicamente redundante el campo `sentence` actual del contexto de FEAT-RCLA.

- [ ] Opcion A: Deprecar `sentence` en un requerimiento posterior, migrando a los consumidores conocidos a derivar desde `quizSentence`, y luego removerlo. Reduce superficie pero requiere coordinacion con consumidores.
- [ ] Opcion B: Mantener `sentence` permanentemente como atajo conveniente; es barato de producir (es la plain sentence canonica ya computada) y evita forzar a cada consumidor a invocar FEAT-QSENT.
- [ ] Opcion C: Diferido; se decide mas adelante segun cuantos consumidores reales terminen usando `quizSentence` vs `sentence`.

**Recomendacion del analista**: Opcion C en el corto plazo. Para este micro-feature, se mantiene `sentence` sin cambios (ver "Limitaciones de alcance"). La decision definitiva se retoma una vez que FEAT-LAPS este shipeado y se pueda observar si algun consumidor actual de `sentence` plano queda obsoleto.

**Answer**: Pendiente. No bloquea este requerimiento: ambos campos coexisten.

---

## Assumptions

1. **El `FormEntity` del quiz original es accesible en el mismo paso en que FEAT-RCLA construye el `CorrectionContext` actual.** Hoy, FEAT-RCLA ya deriva `sentence` plano desde el `FormEntity` (o desde el `AuditableQuiz` que lo expone tras la migracion de FEAT-QSENT). Se asume que la misma fuente que alimenta `sentence` puede alimentar `quizSentence`, invocando el conversor de FEAT-QSENT en lugar de (o ademas de) el derivador a plain sentence. Si esa asuncion no se cumpliera (por ejemplo, porque el contexto se construye a partir de datos ya truncados), la arquitectura debera ajustar el punto de derivacion; la regla funcional de este requerimiento se limita a exigir que el campo este disponible.
2. **La funcionalidad publica de FEAT-QSENT esta disponible en el modulo que construye el `CorrectionContext`.** El conversor `sentenceParts -> quizSentence` de FEAT-QSENT es publico (FEAT-QSENT R025) y consumible desde otros modulos; se asume que el modulo que hoy construye el `CorrectionContext` de RCLA puede invocarlo o recibirlo por inyeccion. Como se inyecta y en que capa (port, adapter, colaborador directo) es decision de arquitectura.
3. **La invariante de consistencia entre `quizSentence` y `sentence` (R003) se cumple naturalmente si ambos campos se derivan del mismo `FormEntity`.** Por FEAT-QSENT R020, la plain sentence canonica derivada directamente de `sentenceParts` coincide con la plain sentence canonica derivada via `quizSentence` (whitespace-normalizada). Si ambos campos se computan en la misma pasada sobre el mismo `FormEntity`, la invariante es subproducto de FEAT-QSENT y no requiere verificacion adicional mas alla de tests de contrato.
4. **No hay consumidores actuales del `CorrectionContext` que rompan si aparece un campo nuevo.** Los consumidores conocidos hoy son el formateador JSON y el formateador texto de `get task`/`get tasks` (FEAT-RCLA R008/R009). Ambos se construyen campo-a-campo; agregar un campo no rompe el JSON (los consumidores externos toleran campos adicionales) ni el texto (solo se muestra si el formateador lo incluye). FEAT-LAPS aun no consume el contexto — este feature lo habilita.
