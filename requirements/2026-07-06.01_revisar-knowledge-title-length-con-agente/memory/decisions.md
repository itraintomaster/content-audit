# Decisions

Decisiones y resoluciones de escalación que futuras sesiones no deberían
re-litigar. Entrada más reciente arriba.

<!-- entries below -->

2026-07-06 — analyst — El snapshot de revisión debe volverse polimórfico (quiz | knowledge).
  - Hoy CourseElementSnapshot lleva solo `QuizTemplateEntity quiz`; CourseElementLocator
    solo sabe snapshot/replace de quizzes. Para persistir una corrección de título+instructions
    de knowledge hace falta que el snapshot pueda portar un knowledge y que el locator lo
    escriba. Es alcance de @architect (punto #3 de la feature). Ver DOUBT-SNAPSHOT-POLIMORFICO.
  - why: sin esto, aprobar una propuesta de título de knowledge no puede escribir al curso.

2026-07-06 — analyst — El agente knowledge-titles-agent es la estrategia LLM de esta feature.
  - Produce {knowledgeId, title, instructions}. Contrato externo análogo a QuizCandidate
    de LASAG pero sobre knowledge (no quiz). Best-effort: exhaustion no es falla; runtime error sí.

2026-07-06 — architect — DOUBT-SNAPSHOT-POLIMORFICO resuelto: Opcion A (snapshot polimorfico).
  - CourseElementSnapshot gana `knowledge: KnowledgeEntity`; nodeTarget (ya existente) es el discriminante.
    DefaultCourseElementLocator.snapshot()/replace() enrutan por nodeTarget: QUIZ intacto, KNOWLEDGE reescribe
    label+instructions por id preservando el resto. Rechazada Opcion B (ruta separada) por duplicar
    RevisionProposal/Artifact/Reviser/Engine y multiplicar regresion sobre R008.
  - why: un solo seam de escritura tocado => R008 (quiz intacto) verificable por construccion, sin duplicar logica.

2026-07-06 — architect — DOUBT-INSTRUCTIONS-SIEMPRE resuelto por el usuario: Opcion A (title+instructions juntos).
  - Aprobar reescribe label(=title) + instructions y SOLO esos dos campos del knowledge (R006/R007).
    El snapshot porta el nodo completo, robusto a la eleccion; el deriver setea los dos campos sobre copia del before.

2026-07-06 — architect — Puente espeja lemma-absence en 3 capas (patch ARCH-KTLR, 12 add / 6 mod, 0 conflicts).
  - Contexto: KnowledgeTitleCorrectionContext + KnowledgeTitleContextResolver, 3ra rama del DispatchingCorrectionContextResolver (R001/R002/R011).
  - SPI propuesta: package revision-domain.knowledgetitle [public] = strategy + registry(sealed) + deriver(sealed) + KnowledgeTitleCandidate{title,instructions} + port KnowledgeTitleCandidateGenerator (R003/R004/R005).
  - Enrutamiento: KnowledgeTitleReviser dedicado en el Map<DiagnosisKind,Reviser> byKind del DispatchingReviser (seam de FEAT-REVBYP), NO comparte codigo con el pipeline lemma => R008.
  - Adapter: revision-infrastructure.knowledgetitleagent [public] factory seam espejo de lagen/lemmaabsenceagent; motor package-private; reusa ChatModel/RunState, sin deps nuevas (R003/R010).
  - Mapeo title->label: responsabilidad EXPLICITA del deriver, no del carrier (el carrier mantiene vocabulario del agente `title`).

## 2026-07-06 — architect — diagnóstico escalaciones generated_file_mismatch (puenteado por el lead)

- **ESC2 (RevisionEngineConfig vaciado)** = interfaz espuria en el bloque `interfaces:` del patch ARCH-KTLR (homónima del record de 14 campos en `models:`); el merge insertó una interface duplicada y `generate` la emitió pisando el record. Fix **ARCH-KTLR-FIX** (propuesto, 1 deletion, 0 conflicts) borra la interface; al re-generar, el record se re-materializa desde la DSL intacta. `patch propose`/`generate` no validan colisiones cross-kind (record vs interface homónimos) — candidato a reporte al equipo Sentinel.
- **ESC1 (KnowledgeTitleContextResolver con `Optional<T>`)** = NO es bug: comportamiento normal de stub sin implementar cuando la impl declara `implements: [CorrectionContextResolver]` raw. Resolución: @developer sustituye `T` por `KnowledgeTitleCorrectionContext` en los overrides (`resolve`, `resolveWithIndex`, `supports` → KNOWLEDGE_TITLE_LENGTH), igual que hizo LemmaAbsenceContextResolver.
