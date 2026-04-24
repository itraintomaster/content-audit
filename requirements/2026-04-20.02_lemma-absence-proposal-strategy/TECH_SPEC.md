---
patch: FEAT-LAPS
requirement: 2026-04-20.02_lemma-absence-proposal-strategy
generated: 2026-04-22T12:00:00Z
---

# Tech Spec: Primera estrategia real de propuesta para LEMMA_ABSENCE (re-evolucion)

Este tech spec es la **re-evolucion** del arquitectural de FEAT-LAPS despues del refinamiento del REQUIREMENT (cerrar DOUBT-CANDIDATE-NOTATION como opcion A y alineacion con el DSL de FEAT-QSENT via `CorrectionContext.quizSentence` que ya shipeo FEAT-RCLAQS). La version anterior del patch ya aplicado al `sentinel.yaml` contenia la gran mayoria de los contratos correctos: ports de estrategia y deriver, carrier de candidato de dos campos, registry en `engine`, dispatcher extendido, dos outcomes nuevos, selector por env var en `bootstrap`. El scaffolding Java correspondiente vive commiteado (7311a9b) y en su mayor parte refleja la arquitectura. Lo que queda por cerrar no son estructuras nuevas sino **precision de contratos**: las declaraciones `throws` en los ports y las descripciones de los campos del candidato que ahora deben apuntar explicitamente al DSL de FEAT-QSENT consumido desde el contexto. Resultado: 7 modificaciones, 0 altas, 0 bajas. Ninguna interfaz sealed externa al scope se toca. Ningun `@Generated` preexistente queda huerfano — todas las firmas que cambian son las mismas clases ya generadas, a las que Sentinel reenganchara el `throws` al regenerar.

## Declarar `throws ProposalStrategyFailedException` en `LemmaAbsenceProposalStrategy.propose`

R015 dice que si la estrategia no puede producir un candidato (proveedor caido, respuesta vacia, output no interpretable) la revision aborta antes de persistir; R016 dice que eso **no** es un `REJECTED` ni un `PENDING_APPROVAL`. El flujo arquitectonico que cumple eso es: la estrategia tira `ProposalStrategyFailedException`, el dispatcher la captura como outcome `STRATEGY_FAILED` y no arma `RevisionProposal`. La version previa del patch declaro la excepcion pero no la cableo en la firma — quedaba como contrato implicito. Al regenerar sin `throws: [...]` en el DSL, la firma Java de `propose()` salia sin `throws`, obligando a todo llamador a envolver manualmente o a tratar la excepcion como unchecked-only. Agregar `throws` cierra el contrato: la presencia del modo de falla es ahora visible en el port.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalStrategy
        _change: modify
        stereotype: port
        sealed: false
        exposes:
          - signature: "id(): StrategyId"
          - signature: "handles(DiagnosisKind kind): boolean"
          - signature: "propose(RefinementTask task, LemmaAbsenceCorrectionContext context): LemmaAbsenceQuizCandidate"
            throws: [ProposalStrategyFailedException]
```

## Declarar `throws ProposalDerivationException` en `LemmaAbsenceProposalDeriver.derive`

R012 dice que la derivacion usa el conversor publico de FEAT-QSENT (`QuizSentenceConverter.parse`) y que si el `quizSentence` es malformado segun la gramatica de QSENT la conversion falla de forma atomica (QSENT R016/R024). La propagacion natural desde el deriver es una `ProposalDerivationException` que el dispatcher consolida al mismo outcome `STRATEGY_FAILED`. Igual que con el strategy port, la excepcion existia como modelo en el patch previo pero no estaba declarada en la firma del port. Al agregar `throws` las dos ramas del outcome "falla antes de persistir" quedan tipadas: una por falla de generacion, otra por falla de derivacion, mismo exit code para el operador.

```architecture
modules:
  - name: revision-domain
    _change: modify
    interfaces:
      - name: LemmaAbsenceProposalDeriver
        _change: modify
        stereotype: service
        sealed: true
        exposes:
          - signature: "derive(CourseElementSnapshot before, LemmaAbsenceQuizCandidate candidate): CourseElementSnapshot"
            throws: [ProposalDerivationException]
```

## Tighten `LemmaAbsenceQuizCandidate` field docs — DSL-as-carrier, no structured record

DOUBT-CANDIDATE-NOTATION quedo `RESOLVED` en el REQUIREMENT: opcion A (DSL textual de FEAT-QSENT). La pregunta posterior de si el candidato deberia colapsarse a dos raw strings sin record intermedio se responde con "no": el record de dos campos preserva la **huella de traceabilidad** (tipo dedicado que aparece en la firma del port, en el outcome y en los logs) y no cuesta nada vs. un `Map<String,String>` o un `Pair`. Mantenemos el record y modernizamos las descripciones para que (a) refieran explicitamente al DSL de FEAT-QSENT; (b) aclaren que `translation` es un campo hermano porque la DSL no cubre traducciones (Assumption 6); (c) digan que el `quizSentence` se parsea con `QuizSentenceConverter.parse` en el paso de derivacion (R012). La decision de no agregar campos adicionales (confidence, model-id, prompt-hash) queda cerrada: esos datos viven en `StrategyId` (name/version/providerId), no duplicados en el candidato.

```architecture
modules:
  - name: revision-domain
    _change: modify
    models:
      - name: LemmaAbsenceQuizCandidate
        _change: modify
        type: record
        fields:
          - name: quizSentence
            type: String
            description: "Candidate exercise expressed in the FEAT-QSENT quizSentence DSL (blanks '____', answer blocks '[variant1|variant2]', hints inline in parentheses). One formal string encodes the sentence stem + blanks + correct answers + accepted variants (F-LAPS-R009, R011, R019). Parsed by QuizSentenceConverter.parse at derivation time (F-LAPS-R012). The strategy MAY but is not required to reuse substrings of the context's quizSentence (FEAT-RCLAQS) — it emits a fresh candidate."
            _change: modify
          - name: translation
            type: String
            description: "Spanish translation of the new exercise (F-LAPS-R009). Copied verbatim into elementAfter by the deriver (F-LAPS-R012). May differ from elementBefore.translation when the candidate changed the sentence meaning (F-LAPS-R013). The DSL of FEAT-QSENT does not cover translations, so this is a sibling field, not an additional entry inside quizSentence."
            _change: modify
```

## Declarar `throws ProposalStrategyFailedException` en `LemmaAbsenceQuizCandidateGenerator.generate`

La estrategia MVP delega toda la generacion concreta a `LemmaAbsenceQuizCandidateGenerator` (provider-agnostic port en el package `strategy`). El adapter concreto (LLM, fixture, etc) sigue **fuera de scope**; el seam queda declarado. La falla natural del adapter — servicio externo caido, respuesta vacia, payload no interpretable — es un `ProposalStrategyFailedException`. Declararlo en `generate()` deja al adapter implementar "fallo o produjo un `LemmaAbsenceGeneratorResponse` valido": no necesita distinguir modos de falla, los consolida en una sola excepcion con `(strategyName, taskId, reason)`. La MVP strategy re-lanza o envuelve esa excepcion tal cual, sin capturarla, preservando la trazabilidad hasta el dispatcher.

```architecture
modules:
  - name: revision-domain
    _change: modify
    packages:
      - name: strategy
        _change: modify
        interfaces:
          - name: LemmaAbsenceQuizCandidateGenerator
            _change: modify
            stereotype: port
            visibility: public
            sealed: false
            exposes:
              - signature: "generate(LemmaAbsenceCorrectionContext context): LemmaAbsenceGeneratorResponse"
                throws: [ProposalStrategyFailedException]
```

## Declarar `throws InvalidProposalStrategyException` en `ProposalStrategySelector.select`

DOUBT-STRATEGY-SELECTION quedo resuelto como opcion A: env var `CONTENT_AUDIT_LAPS_STRATEGY` al startup del CLI, mismo patron que `CONTENT_AUDIT_APPROVAL_MODE`. Si el operador setea un valor que no coincide con ninguna estrategia registrada, el selector tira `InvalidProposalStrategyException(value, registered)` — el mensaje incluye la lista de nombres validos del registry para que la correccion sea inmediata. La excepcion existia como modelo pero la firma del selector la omitia. Declararla en `throws` cierra el contrato: cualquier bootstrap alternativo (tests, embedding del engine) sabe desde la firma que el selector puede fallar por misconfiguracion y debe decidir como reportarla antes de llamar al factory.

```architecture
modules:
  - name: audit-cli
    _change: modify
    packages:
      - name: bootstrap
        _change: modify
        interfaces:
          - name: ProposalStrategySelector
            _change: modify
            stereotype: port
            sealed: true
            exposes:
              - signature: "select(String envValue, LemmaAbsenceProposalStrategyRegistry registry): String"
                throws: [InvalidProposalStrategyException]
```

## Como quedan los doubts del REQUIREMENT despues de esta re-evolucion

- **DOUBT-CANDIDATE-NOTATION (RESOLVED in REQUIREMENT).** Candidato = `{ String quizSentence, String translation }`, con `quizSentence` en el DSL de FEAT-QSENT. La version anterior del patch ya reflejaba esto; esta re-evolucion solo tightea las descripciones de los campos para hacerlo explicito.
- **DOUBT-STRATEGY-SELECTION.** Cerrado como opcion A (env var `CONTENT_AUDIT_LAPS_STRATEGY`). Default unset -> `"lemma-absence-mvp"` (el composition root aplica el default sobre el output del selector). Selector tira `InvalidProposalStrategyException` ante valor desconocido (ver seccion anterior).
- **DOUBT-STRATEGY-REGISTRY.** `LemmaAbsenceProposalStrategyRegistry` (port root) + `DefaultLemmaAbsenceProposalStrategyRegistry` (package `engine`, `sealed: true`) + `LemmaAbsenceProposalStrategyRegistryConfig` (record nullable). El composition root (audit-cli) construye un registry por codigo. Sin archivo declarativo hoy; aditivo si se requiere.
- **DOUBT-STRATEGY-METADATA.** `StrategyId { name, version, providerId }`. Campos adicionales (prompt hash, duracion) quedan para una iteracion posterior y seran aditivos nullable sin romper wire-compat.
- **DOUBT-PROMPT-PERSISTENCE.** Opcion A: solo identidad de estrategia en la propuesta; `CorrectionContext` reconstruible via plan+audit. Si se necesita mas adelante un `inputSnapshot` dentro de `RevisionProposal`, se agrega nullable.
- **DOUBT-FAILURE-TRACEABILITY.** Opcion A: cero rastro persistente; solo stdout/stderr via el outcome `STRATEGY_FAILED`. Reversible a opcion B (log file) sin impacto arquitectonico.

## Pendiente explicito fuera de este patch

El adapter concreto del `LemmaAbsenceQuizCandidateGenerator` (modulo `revision-infrastructure` nuevo, o subpackage de `audit-infrastructure`, o fixture-only en test scope) **sigue fuera de scope**. Es decision del usuario (LLM vendor / modelo local / fixture). Hasta que exista, el registry puede arrancar vacio (todas las invocaciones LEMMA_ABSENCE resuelven `NO_ACTIVE_STRATEGY`) o con un fixture generator que habilita QA end-to-end sin proveedor externo.

Tests tambien quedan fuera: este patch no declara `handwrittenTests`. `@qa-tester` cubre:
- `LemmaAbsenceMvpStrategy` con generator stub (R007/R008): happy path + `ProposalStrategyFailedException` propagado.
- `DefaultLemmaAbsenceProposalDeriver` con fixtures QSENT reales (R012/R013/R014): happy path + `ProposalDerivationException` ante DSL invalida + preservacion literal de identificadores del `elementBefore`.
- Rama LEMMA_ABSENCE del `DispatchingReviser` (R002/R006/R015): con/sin registry, con/sin estrategia activa, captura de ambas excepciones como `STRATEGY_FAILED`.
- `DefaultLemmaAbsenceProposalStrategyRegistry` (R004): `active()`, `byName()`, `listAll()`.
- `DefaultProposalStrategySelector` (DOUBT-STRATEGY-SELECTION): env var unset -> default, valor valido, valor invalido -> `InvalidProposalStrategyException`.
- Journeys F-LAPS-J001..J005 end-to-end con generator fixture.
