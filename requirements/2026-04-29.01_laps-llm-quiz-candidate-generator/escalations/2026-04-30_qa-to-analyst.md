# Escalación: QA → Analyst

**De**: @qa-tester
**A**: @analyst
**Feature**: FEAT-LAGEN
**Fecha**: 2026-04-30
**Tipo**: missing_field (3 reglas que faltan en REQUIREMENT.md)

## Contexto

Estoy diseñando la cobertura de tests para FEAT-LAGEN (`requirements/2026-04-29.01_laps-llm-quiz-candidate-generator/`). El patch architect-to-developer agregó tres componentes con behavior observable que no encajan bajo ninguna de las 11 reglas existentes (R001..R011). Sin reglas que los constraine, no puedo proponer handwrittenTests sin violar la regla de "no transitive coverage" (feedback explícito del usuario). Necesito que extiendas REQUIREMENT.md con las reglas listadas abajo para que pueda tagear los tests correspondientes.

Componentes huérfanos:

1. **Generador canned** (sin nombre técnico — desde la perspectiva del operador es "el modo de respuesta fija para tests / desarrollo offline"). Materializa DOUBT-CANNED-MODE-AVAILABILITY → Opción B, decisión cerrada por el architect en `decisions.md` línea 25.
2. **Selector del modo de generación** (canned vs dinámica). Decisión arquitectónica firme: default=dinámica, canned=opt-in.
3. **Mecanismo de configuración del operador** (R008 declara *qué* perillas existen, pero no *cómo* se aplican: si el sistema falla atómicamente al startup o lazy en cada invocación; si valores inválidos son detectables antes de ejecutar revisión).

## Reglas pedidas

Por favor agregalas con tono funcional (sin nombrar adapter / port / clase / módulo / env-var-name; behavior observable al sistema desde la perspectiva del operador). Mantené el formato scan-first existente del REQUIREMENT.md (anchor + blockquote summary + opcional `<details>`).

### F-LAGEN-R012 — Comportamiento del modo de generación fija (canned)

> Cuando el sistema opera en modo de generación de respuesta fija ("canned"), el candidato producido para una tarea LEMMA_ABSENCE es el `quizSentence` + `translation` predeterminados, idénticos en cada invocación, independientes del `LemmaAbsenceCorrectionContext` recibido. Este modo materializa DOUBT-CANNED-MODE-AVAILABILITY → Opción B (opt-in).

**Severity sugerido**: minor | **Validation**: AUTO_VALIDATED

Justificación funcional: el operador necesita una manera de ejercitar journeys de FEAT-LAPS sin un proveedor de LLM disponible (development offline, tests reproducibles del CLI). El requirement R002 ya indica que esto es admisible si "queda claramente no activado en una corrida normal". R012 cierra el behavior observable: misma respuesta cada vez, sin consultar contexto, sin contactar proveedor.

### F-LAGEN-R013 — Selección del modo de generación de candidato

> El operador puede elegir entre dos modos de generación de candidato: dinámica (consultando un modelo generativo, comportamiento por defecto observable de R002) y fija (canned, R012). El default es dinámica; canned solo se activa por opt-in explícito del operador.

**Severity sugerido**: critical | **Validation**: AUTO_VALIDATED

Justificación funcional: cierra DOUBT-CANNED-MODE-AVAILABILITY como decisión declarada en REQUIREMENT.md (hoy solo está en decisions.md, fuera de la fuente de verdad funcional). Un operador o auditor que abre REQUIREMENT.md tiene que poder leer "el default es dinámica y nadie activa canned por accidente" sin tener que leer notas internas del equipo.

### F-LAGEN-R014 (o extensión de R008 — vos decidís) — Atomicidad de la configuración

Pregunta funcional para vos: ¿la traducción de la configuración del operador a los parámetros que el sistema usa (proveedor, modelo, temperatura, etc.) es comportamiento que el requirement debe constreñir, o es detalle de implementación que cae bajo R008?

- **Si lo primero (regla nueva)**:

> **F-LAGEN-R014** — La configuración declarada por el operador (R008) se interpreta atómicamente al startup del sistema; cualquier valor inválido (modo desconocido, temperatura no numérica, timeout negativo, etc.) se reporta de forma observable antes de ejecutar cualquier operación de revisión, no durante.
> **Severity sugerido**: major | **Validation**: AUTO_VALIDATED

  Justificación: el operador no debe descubrir un typo en su configuración recién después de haber gastado tokens consultando un proveedor mal apuntado. Hoy es decisión arquitectónica (decisions.md línea 33: "Map<String,String> env... unit-testable... explicit Map argument"); falta declararla como regla funcional.

- **Si lo segundo (extensión de R008)**: agregá un párrafo final al `<details>` de R008 que diga "los valores se interpretan en una sola pasada y errores de configuración son reportados antes de ejecutar operaciones de revisión" o equivalente. En ese caso no creés F-LAGEN-R014 y yo tageo los tests del config-resolver contra R008.

Tu decisión sobre R014 vs extensión de R008 me sirve igual: lo único que necesito es saber cuál de las dos es la fuente canónica de la regla.

## Restricciones

- Mantené formato scan-first del REQUIREMENT.md actual (anchor `<a id="F-LAGEN-RXXX"></a>`, header con `### Rule[F-LAGEN-RXXX] - <título>`, blockquote summary, opcional `<details>`).
- Tono funcional. Nada de "adapter", "port", "factory", "env-var", "Map<String,String>". Si tenés que aludir al mecanismo, decí "el sistema" / "la configuración del operador".
- No toques R001..R011 (estables y ya tageadas).
- Después de insertar, validá con `sentinel requirement validate` y confirmame que las reglas quedaron registradas. Si necesitás reasignar IDs (por ejemplo si R012 ya existía), avisame el mapping nuevo y lo aplico al patch.

## Próximo paso mío

En cuanto confirmes (o decidas R014 vs extender R008), re-emito el patch con ~7 handwrittenTests adicionales:
- `CannedLemmaAbsenceQuizCandidateGenerator` → 2-3 tests (idempotencia, independencia de contexto) tagged a R012
- `DefaultLagenModeResolver` → 2-3 tests (default LLM, opt-in CANNED, valor inválido) tagged a R013
- `DefaultLagenConfigResolver` → 4-5 tests (parseo válido, defaults, errores tipados) tagged a R008 o R014 según tu decisión

Ya tengo redactados los nombres pero los emito recién después de tu confirmación para no tagear contra reglas inexistentes (`sentinel patch propose` rechazaría la traceability).
