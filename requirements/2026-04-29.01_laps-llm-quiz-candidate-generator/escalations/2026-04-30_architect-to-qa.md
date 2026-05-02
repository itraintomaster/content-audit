# Reply: Architect → QA

**De**: @architect
**A**: @qa-tester
**Feature**: FEAT-LAGEN
**Fecha**: 2026-04-30
**Tipo**: contract_closure_response (3 contratos cerrados)
**Re**: `2026-04-30_qa-to-architect.md`

## Resumen

Los 3 contratos under-specified estan cerrados. Toque sentinel.yaml via patch correctivo (no solo TECH_SPEC.md): agregue 2 modelos publicos nuevos (`LagenDefaults`, `InvalidProviderIdException`) en `revision-infrastructure.lagen` para que tus aserciones tengan tipos concretos a los que aferrarse, en lugar de literales magicos en los nombres de tests. Tambien re-emiti los handwrittenTests del factory y del classifier directamente en el patch — vas a verlos ya escritos cuando inspecciones la nueva version. TECH_SPEC.md actualizado a 14 fences en castellano.

## Contrato 1 — `DefaultLangChainErrorClassifier.classify(Throwable) → LlmGenerationFailureCategory`

**Resolucion**: matching por **tipo de Throwable sobre la cadena de causas** (`getCause()` iterativo), con OpenAiHttpException tipado como detector preferente para auth y heuristica regex como fallback.

**Tabla pinneada en TECH_SPEC.md** (fence 9, "Contrato cerrado: tabla de mapeo de DefaultLangChainErrorClassifier.classify"):

| Detector | Categoria |
|---|---|
| `ConnectException`/`UnknownHostException`/`NoRouteToHostException` en cause chain | `LLM_UNREACHABLE` |
| `SocketTimeoutException`/`java.util.concurrent.TimeoutException` en cause chain | `LLM_TIMEOUT` |
| Raiz es `dev.ai4j.openai4j.OpenAiHttpException` con `statusCode()` 401 o 403 | `LLM_AUTH_FAILED` |
| `getMessage()` de raiz matchea `(?i)\b(unauthorized\|forbidden\|invalid api key\|401\|403)\b` | `LLM_AUTH_FAILED` |
| Cualquier otro Throwable no-null | `LLM_OTHER` |

**`LLM_RESPONSE_EMPTY`/`LLM_RESPONSE_MALFORMED` NO aparecen en este classifier** — los emite `LemmaAbsenceResponseParser`. Consistente con tu sugerencia.

**Tipos exactos para nombres de tests** (ya re-emitidos en el patch como handwrittenTests de `DefaultLangChainErrorClassifier`):

```
classify returns LLM_UNREACHABLE for a ConnectException anywhere in the cause chain
classify returns LLM_UNREACHABLE for an UnknownHostException anywhere in the cause chain
classify returns LLM_UNREACHABLE for a NoRouteToHostException anywhere in the cause chain
classify returns LLM_TIMEOUT for a SocketTimeoutException anywhere in the cause chain
classify returns LLM_TIMEOUT for a java.util.concurrent.TimeoutException anywhere in the cause chain
classify returns LLM_AUTH_FAILED for an OpenAiHttpException whose statusCode is 401
classify returns LLM_AUTH_FAILED for an OpenAiHttpException whose statusCode is 403
classify returns LLM_AUTH_FAILED when the root cause message matches the auth heuristic regex (case-insensitive 'unauthorized' / 'forbidden' / '401' / '403' / 'invalid api key')
classify returns LLM_OTHER for a Throwable not matched by any specific category
classify never returns null for any non-null Throwable
```

Los 3 tests viejos genericos (`connection-refused / unknown-host / DNS error`, `request-deadline-exceeded`, `HTTP 401 / 403`) estan marcados con `_change: delete` y se eliminaran al aplicar el patch. Si los ves duplicados en el patch persistido, es ese estado intermedio.

**Para `LemmaAbsenceLlmGenerator`** los 6 tests viejos genericos (`when the model client throws a connection-refused error`, etc.) tambien fueron marcados delete (3 de 6: UNREACHABLE/TIMEOUT/AUTH_FAILED genericos), reemplazados por 3 especificos (`throws ConnectException`, `throws SocketTimeoutException`, `throws an OpenAiHttpException with status 401`). Los 3 que NO marque delete (RESPONSE_EMPTY, RESPONSE_MALFORMED, LLM_OTHER) quedan como estan porque ya eran especificos.

**No agregue un modelo `Map<Class, LlmGenerationFailureCategory>`**: la logica del classifier no es tabla pura — tiene iteracion sobre la cadena de causas, inspeccion de un statusCode tipado, y matching regex sobre message. Un map estatico no representaria fielmente el algoritmo.

## Contrato 2 — `LemmaAbsenceLlmGeneratorFactory.providerIdFor(LagenConfig) → String`

**Resolucion**: formato `provider:model` literal, sin normalizacion case/whitespace, rechazo con excepcion si provider o model contienen `:`.

**Tipo exacto para asertar el rechazo**: `InvalidProviderIdException` (nuevo modelo publico que agregue en `revision-infrastructure.lagen`):

```yaml
- name: InvalidProviderIdException
  type: exception
  extends: RuntimeException
  visibility: public
  message: "providerName and modelId must not contain ':' (got providerName='%s', modelId='%s'). The ':' is reserved as the providerId separator (provider:model)."
  fields:
    - { name: providerName, type: String }
    - { name: modelId, type: String }
```

**Respuestas a tus 4 preguntas**:

1. **Formato `providerName:modelId` con `:` literal**: SI confirmado.
2. **`:` embebido**: rechaza con `InvalidProviderIdException` (validacion temprana en el factory, no en el constructor de `LagenConfig`).
3. **Case sensitivity**: el factory NO normaliza. `LMStudio:gemma-3-4b-IT` y `lmstudio:gemma-3-4b-it` producen providerIds distintos. El operador es responsable.
4. **Whitespace**: el factory NO trimea. Se preserva verbatim. (Consistente con la decision de no normalizar case.)

**Tipos exactos para nombres de tests** (ya re-emitidos en el patch como handwrittenTests de `DefaultLemmaAbsenceLlmGeneratorFactory`):

```
providerIdFor returns 'providerName:modelId' joining the two LagenConfig fields with a literal ':'
providerIdFor preserves the case of providerName and modelId verbatim (no lowercasing)
providerIdFor preserves leading and trailing whitespace inside providerName and modelId verbatim (no trimming)
providerIdFor raises InvalidProviderIdException when LagenConfig.providerName contains ':'
providerIdFor raises InvalidProviderIdException when LagenConfig.modelId contains ':'
providerIdFor yields different values for two LagenConfigs that differ in providerName  (preservado del original)
providerIdFor yields different values for two LagenConfigs that differ in modelId       (preservado del original)
```

El test viejo generico (`providerIdFor returns 'providerName:modelId' joining the two LagenConfig fields` sin `with a literal ':'`) esta marcado delete. Reemplazado por la version mas explicita.

**Nota tecnica**: intente agregar `throws: [InvalidProviderIdException]` a la signature `providerIdFor` en sentinel.yaml, pero el merge no lo preserva (el `throws:` desaparece cuando se reemplaza el array `exposes` de la interface). Es no-blocker — el contrato del comportamiento queda pinneado en TECH_SPEC.md y en el handwrittenTest. `throws:` en sentinel es informativo de todos modos.

## Contrato 3 — Defaults numericos del factory para `LagenConfig` con campos null

**Resolucion**: 0.7 / 2048 / 30s materializados como un record publico nominado, no como literales escondidos en el factory.

**Tipo exacto para asertar igualdad** (nuevo modelo publico que agregue en `revision-infrastructure.lagen`):

```yaml
- name: LagenDefaults
  type: record
  visibility: public
  fields:
    - { name: temperature, type: double }    # pinneado a 0.7
    - { name: maxTokens, type: int }         # pinneado a 2048
    - { name: timeout, type: Duration }      # pinneado a Duration.ofSeconds(30)
```

**Por que un record y no constantes en el factory**: (1) los tests pueden asertar igualdad contra `LagenDefaults` getters en lugar de re-tipear `0.7`, lo que acopla el test a la decision pinneada y no al literal; (2) cualquier futura iteracion que quiera re-pinear los defaults toca un unico punto observable (el call-site donde se construye la instancia de `LagenDefaults`); (3) `LagenDefaults` queda publico por la misma razon que `LagenConfig`: contrato cross-module legible.

El developer DEBE inicializar la unica instancia de `LagenDefaults` con `temperature=0.7, maxTokens=2048, timeout=Duration.ofSeconds(30)`. Esto es responsabilidad del developer; el QA puede leer los getters y asertar igualdad.

**Tipos exactos para nombres de tests** (ya re-emitidos en el patch):

```
create applies LagenDefaults.temperature (0.7) when LagenConfig.temperature is null
create applies LagenDefaults.maxTokens (2048) when LagenConfig.maxTokens is null
create applies LagenDefaults.timeout (Duration.ofSeconds(30)) when LagenConfig.timeout is null
```

Los 3 tests viejos genericos (`non-zero default temperature`, `sensible default maxTokens`, `default timeout`) estan marcados delete.

## Resumen de cambios en sentinel.yaml

**Patch agrega 2 modelos publicos en `revision-infrastructure.lagen`**:
- `LagenDefaults` (record, 3 campos)
- `InvalidProviderIdException` (exception, 2 campos)

**Patch re-emite handwrittenTests** en 3 implementaciones:
- `DefaultLemmaAbsenceLlmGeneratorFactory`: +5 tests (preserves case, preserves whitespace, raises x2, applies LagenDefaults x3) y 4 deletes (1 generico providerIdFor + 3 genericos defaults)
- `LemmaAbsenceLlmGenerator`: +3 tests (throws ConnectException, SocketTimeoutException, OpenAiHttpException 401) y 3 deletes (genericos UNREACHABLE/TIMEOUT/AUTH_FAILED)
- `DefaultLangChainErrorClassifier`: +8 tests (3 unreachable + 2 timeout + 2 auth tipados + 1 auth regex) y 3 deletes (genericos)

**Patch validado**: 2 additions, 16 modifications, 0 deletions, 0 conflicts.

**TECH_SPEC.md actualizado**: 14 fences en castellano, 2 nuevas secciones ("Contrato cerrado: defaults numericos del factory materializados en `LagenDefaults`", "Contrato cerrado: formato de `providerId` y rechazo ante `:` embebido", "Contrato cerrado: tabla de mapeo de `DefaultLangChainErrorClassifier.classify`").

## Que tenes que hacer ahora

1. Re-leer `architectural_patch.yaml` y verificar que los handwrittenTests nuevos esten OK con tu pipeline.
2. Si queres refinar mas alguno de los nombres que prepuse, hacelo via tu flujo normal.
3. Confirmame por chat (via el usuario) que los 3 cierres son aceptables.

## Notas y warnings tecnicos para tu memoria

- Bug de merge ya documentado en `decisions.md` linea 48 se reprodujo a escala mayor: cuando emiti `_change: modify` sobre `revision-infrastructure` con sus packages, perdi audit-cli ENTERO + LlmGenerationFailureCategory + atributos top-level del modulo. Workaround: SIEMPRE re-emitir el modulo entero con todos sus atributos top-level y todos los packages a preservar.
- `_change: modify` sobre dependencies aliases que ya estan en sentinel.yaml requiere `_change: modify` (no `add`) — error claro.
- `throws:` en signatures dentro de `exposes` no sobrevive al merge cuando se reemplaza el array `exposes`. No-blocker (informativo).
