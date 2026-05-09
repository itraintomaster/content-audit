# Progress - FEAT-REVCTX

2026-05-09 — analyst — Feature creado como spin-off del Gap 2 reportado por
  el operador al integrar el dashboard.
  why: el operador identifico que revise hoy opera contra el plan persistido
  y no respeta el contexto proyectado que el cliente le muestra al operador.
  Consecuencia concreta: el LLM de LAPS recibe el contexto basal y puede
  proponer una palabra que ya consumio una propuesta pendiente, anulando el
  beneficio visual del plan proyectado. Este feature habilita un opt-in para
  que revise reciba el correctionContext desde el cliente y lo consuma sin
  combinarlo. Reglas R001 a R006 definen el contrato observable; los doubts
  cubren shape del flag, formato del input, profundidad de validacion de
  coherencia, trazabilidad del artefacto, e interaccion con las invariantes
  secundarias de las estrategias activas. Pendiente: que el operador revise
  doubts y elija opciones, despues pasa a @architect.

2026-05-09 — analyst — Cerrados los 5 doubts con decisiones del operador.
  Reglas resultantes: R001..R008 (R007 y R008 son nuevas).
  why: el operador volvio con decisiones explicitas para los 5 doubts.
  Cambios materiales: (1) R001 incorpora los dos flags mutuamente
  excluyentes y la regla "ambos a la vez = error claro antes de hacer
  nada". (2) R003 cambio el sanity check de coherencia: ya no compara
  taskId (rechazado por el operador con argumento valido — dos planes
  distintos producen taskIds distintos para el mismo problema logico),
  sino que compara la dupla (nodeId, diagnosisKind), que es la identidad
  logica estable entre planes. R005 reformulado en consecuencia. (3)
  R007 nueva: el artefacto persistido lleva indicador de origen del
  contexto + snapshot literal del payload cuando hubo override. (4)
  R008 nueva: el payload es JSON con la misma forma observable que la
  salida de get task, para permitir copiar-pegar-editar como flujo del
  operador. (5) R002 #3 reformulado: la confianza explicita en el
  override es ahora una invariante (el sistema NO re-deriva ni
  cruza-valida; las dos defensas son R003 y R007). Agregado J004 para
  el camino de "ambos flags a la vez = rechazo". Validacion sentinel
  pasa OK. Siguiente paso: handoff a @architect.

2026-05-09 — architect — Implement kickoff. Patch ARCH-REVCTX-001 ya
  aplicado y `sentinel generate` ejecutado; stubs materializados en los
  6 archivos productivos (parser, dos validators, DefaultRevisionEngine,
  DefaultRevisionEngineFactory, DispatchingCorrectionContextResolver,
  ReviseCmd, Main.java composition root) y 10 archivos de test
  (DefaultCorrectionContextOverrideParserTest, dos validator tests,
  DefaultRevisionEngineTest +8, DispatchingCorrectionContextResolverTest +1,
  ReviseCmdTest +4, FRevctxJ001..J004). Build actualmente roto:
  DefaultRevisionEngine linea 80 sigue con la firma vieja de revise; linea
  155 construye RevisionArtifact con 6 args en lugar de 8;
  DefaultRevisionEngineFactory linea 42 no inyecta el parser; ReviseCmd
  no tiene los flags --correction-context y --correction-context-file y
  el override revise() de la interfaz; Main.java linea 388 construye el
  RevisionEngineConfig pero no setea correctionContextOverrideParser.
  Siguiente paso: handoff a @developer (cuerpo productivo) y @test-writer
  (cuerpo de tests) en paralelo. SendMessage no esta disponible en este
  preset; el lead rutea los briefs.

2026-05-09 — developer — Implementation complete. All tests pass:
  revision-domain 165/165, audit-cli 281/281. Production code:
  - LemmaAbsenceContextStructuralValidator + SentenceLengthContextStructuralValidator
    implemented (flat JSON validation, required fields, nodeId identity check)
  - DefaultCorrectionContextOverrideParser implemented (JSON parse, kind check,
    delegates to validator)
  - DefaultRevisionEngine: new 4-arg revise overload with override path
    (supports check → parse → use override context); DERIVED path preserved;
    contextSource + contextOverridePayload added to RevisionArtifact (R007)
  - DefaultRevisionEngineFactory: passes correctionContextOverrideParser
  - DefaultProposalDecisionService: updated to 8-arg RevisionArtifact
  - ReviseCmd: --correction-context and --correction-context-file flags with
    mutual exclusion; OVERRIDE_INVALID + OVERRIDE_NOT_APPLICABLE handled
  Test fixes applied to: 7 revision-domain tests (6→8 arg RevisionArtifact),
  2 engine tests (missing parser arg + 3→4 arg revise), 1 audit-infrastructure
  test (6→8 arg RevisionArtifact), DefaultCorrectionContextOverrideParserTest
  (record-style→getter accessor fix).
  Pre-existing failures unchanged: refiner-domain FDslenJ001 (3 unimplemented
  stubs), audit-domain 23 unimplemented stubs — not FEAT-REVCTX work.

2026-05-09 — test-writer — Todos los cuerpos de test implementados y verdes.
  19 handwrittenTests + 4 journey path tests. Tests afectados:
  revision-domain: 165 pass (0 fail); audit-cli: 281 pass (0 fail);
  refiner-domain: DispatchingCorrectionContextResolverTest 6/6 pass.
  Nota clave descubierta via red/green: el engine llama
  contextResolver.supports(kind) ANTES de invocar el parser cuando
  overridePayload != null; si retorna false → OVERRIDE_NOT_APPLICABLE
  (sin llegar al parser). Para llegar al parser, supports() debe retornar
  true. Los tests R002/R005/R006/R007 necesitan ese stub. El engine NO
  llama auditReportStore.load() en el override path (R002 cero derivacion).
  Siguiente paso: @developer implementa production code.
