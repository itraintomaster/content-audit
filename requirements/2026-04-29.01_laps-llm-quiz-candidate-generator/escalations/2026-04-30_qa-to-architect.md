# Escalación: QA → Architect

**De**: @qa-tester
**A**: @architect
**Feature**: FEAT-LAGEN
**Fecha**: 2026-04-30
**Tipo**: underspecified_contract (3 contratos que limitan los nombres de tests)

## Contexto

Diseñé 58 handwrittenTests para FEAT-LAGEN distribuidos en 6 implementaciones (validados, patch en `architectural_patch.yaml`). Tres contratos quedaron under-specified — los nombres de tests que asumí pueden no reflejar el behavior exacto que tenés en mente. Sin cierre, el developer puede implementarlos con suposiciones distintas a las tuyas y romper el contrato silenciosamente. Necesito que pinees los tres en TECH_SPEC.md (y opcionalmente refines signatures en sentinel.yaml).

Los tres son contratos donde la signature actual sólo declara la firma sin semántica.

## Contrato 1 — `DefaultLangChainErrorClassifier.classify(Throwable) → LlmGenerationFailureCategory`

**Signature actual** (sentinel.yaml línea 6608, internal en `revision-infrastructure.lagenopenai`):
```yaml
- signature: "classify(Throwable cause): LlmGenerationFailureCategory"
```

**Lo que falta**: tabla de mapeo concreta. ¿Qué tipos / causas de Throwable de LangChain4j (legacy `generate(List<ChatMessage>)`) corresponden a cada una de las 6 categorías?

**Mi sugerencia (descartá si tenés mejor mapping)**:

| Throwable | Categoría |
|---|---|
| `java.net.ConnectException`, `java.net.UnknownHostException`, `java.net.NoRouteToHostException` | `LLM_UNREACHABLE` |
| `java.net.SocketTimeoutException`, `java.util.concurrent.TimeoutException`, deadline-exceeded de HTTP client subyacente | `LLM_TIMEOUT` |
| Excepción del proveedor cuyo HTTP status es 401/403, o cuyo `getMessage()` contiene "auth"/"unauthorized" (vos decidís el mecanismo de detección) | `LLM_AUTH_FAILED` |
| Cualquier `Throwable` no matcheado por las anteriores | `LLM_OTHER` |
| `LLM_RESPONSE_EMPTY` y `LLM_RESPONSE_MALFORMED` | NO aparecen acá — esas las decide el response parser sobre el texto crudo, antes de llegar al classifier |

**Pregunta**: ¿el classifier inspecciona el HTTP status code de la respuesta del proveedor (necesita un wrapper exception específico de LangChain4j) o solo matchea por tipo de Throwable? El tipo de matching exacto cambia los nombres de mis tests del classifier.

**Pedido**: pineá la tabla de mapping en TECH_SPEC.md como sección "Contrato cerrado: clasificación de errores". Opcional: agregá `description:` al `signature` en sentinel.yaml resumiendo el mapping. Si querés un modelo nuevo (e.g., `Map<Class<? extends Throwable>, LlmGenerationFailureCategory>`) que el classifier consulte, declaralo y lo testeo aparte.

## Contrato 2 — `LemmaAbsenceLlmGeneratorFactory.providerIdFor(LagenConfig) → String`

**Signature actual** (sentinel.yaml línea 6571, public en `revision-infrastructure.lagen`):
```yaml
- signature: "providerIdFor(LagenConfig config): String"
```

**Lo que falta**: formato canónico del string devuelto. `decisions.md` línea 27 dice `"providerName:modelId"` (e.g., `lmstudio:gemma-3-4b-it`), pero la signature no lo declara y mis tests lo asumen.

**Preguntas concretas**:

1. ¿Confirmás formato `"providerName:modelId"` con `:` literal como separador?
2. ¿Qué pasa si `providerName` o `modelId` contienen `:` ellos mismos? ¿Se rechazan en `LagenConfig` (validación temprana), se escapan, se trimean? Mi default sería: el factory los rechaza con `IllegalArgumentException` porque romperían el round-trip de la trazabilidad.
3. Case sensitivity: `LMStudio:gemma-3-4b-IT` vs `lmstudio:gemma-3-4b-it` — ¿el factory normaliza a lowercase, o respeta el case que el operador eligió?
4. Whitespace: ¿se trimea antes de concatenar?

**Pedido**: pinealo en TECH_SPEC.md como sección "Contrato cerrado: formato de providerId". Mínimo que necesito: confirmación del formato `provider:model` y comportamiento ante `:` embebido. El resto (case, whitespace) son nice-to-have.

## Contrato 3 — Defaults numéricos del factory para `LagenConfig` con campos null

**Lo que dice REQUIREMENT.md**:
- `LagenConfig.temperature` nullable; Asunción 4 dice "default no-cero"
- `LagenConfig.maxTokens` nullable; "sensible default" (sin número)
- `LagenConfig.timeout` nullable; "default budget" (sin número)
- `LagenConfig.apiKey` nullable; sin default (no Authorization header)

**Lo que asumí en mis tests**: temperature ≈ 0.7, maxTokens ≈ 2048, timeout ≈ 30s. Pero los tests dicen "non-zero default temperature" / "default timeout" sin pinear el número, lo cual es laxo. Si pineás los valores en TECH_SPEC.md, refino los tests para asertar el valor exacto (e.g., "create applies default temperature=0.7 when LagenConfig.temperature is null").

**Pedido**: pineá los 3 defaults numéricos en TECH_SPEC.md como "Contrato cerrado: defaults del factory". Sugerencias razonables (descartá si tenés mejores):
- `temperature = 0.7` (Assumption 4 cumplida, no determinístico)
- `maxTokens = 2048` (suficiente para un quizSentence típico de cloze + traducción)
- `timeout = Duration.ofSeconds(30)` (alineado con el ejemplo `LLM_TIMEOUT: deadline 30s exceeded` en REQUIREMENT.md R006)

Si decidís un valor distinto, los uso. Lo único que importa es que sean fijos y declarados.

## Restricciones

- Mantené el TECH_SPEC.md en castellano (convención del proyecto, decisions.md línea 42).
- Si los cierres requieren cambiar signatures en sentinel.yaml (e.g., agregar `description` a `signature`, agregar el mapping table como modelo), emití un patch correctivo separado — no lo hago yo (single-writer rule).
- Si alguno de los tres contratos amerita una decisión funcional (no técnica) que cambie la superficie observable al operador, escalalo al usuario antes de cerrarlo. Mi lectura es que los tres son técnicos y caen 100% bajo tu autoridad.

## Próximo paso mío

Cuando confirmes los tres cierres (con o sin patch correctivo en sentinel.yaml):

1. Refino los nombres de los 5 tests del classifier para que cada uno asierte un mapping específico de la tabla (e.g., "classify returns LLM_UNREACHABLE for ConnectException" en lugar de "for connection-refused / unknown-host / DNS error").
2. Refino los 3 tests del factory de defaults (temperature, maxTokens, timeout) para asertar el valor exacto en lugar de "non-zero" / "sensible default".
3. Agrego 1-2 tests al factory para `providerIdFor` que cubran el comportamiento ante `:` embebido y normalización (case/whitespace) según lo que decidas.
4. Re-emito el patch.

Si optás por dejar los contratos abiertos por diseño (e.g., "el classifier mapping queda flexible para iteración"), lo respeto y dejo los nombres actuales — pero confirmámelo así lo documento en `decisions.md` para no relitigar la próxima iteración.
