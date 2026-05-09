# Progress - FEAT-PLANEF

2026-05-07 — analyst — Primer draft creado bajo FEAT-DSUGW (sugerencias dinamicas).
  why: el operador reporto el problema de las suggestedLemmas estaticas; el
  primer draft modelaba content-audit como responsable de "consumo de lemas",
  orden-independencia, matcheo cross-plan, etc.

2026-05-07 — analyst — Reescrito como FEAT-PLANEF (plan efimero / no persistido).
  why: el operador rechazo el primer draft por over-engineering. El alcance
  real es mucho menor: content-audit solo aporta un modo de invocacion del
  comando `plan` que no persiste y devuelve el JSON por stdout. Toda la
  orquestacion (analisis simulado via F-CDIFF + matcheo por nodeId + reemplazo
  de campos en UI) la hace el cliente externo (dashboard de Learney). Los
  efectos sobre suggestedLemmas son emergentes del pipeline estandar, no
  reglas de este feature.

2026-05-07 — analyst — Carpeta renombrada de
  `2026-05-07.01_dynamic-suggested-words/` a
  `2026-05-07.01_plan-efimero-no-persistido/`.
  why: el nombre del feature ya no es sobre suggestedWords; el efecto sobre
  esa estructura es un side-effect natural y no aparece como regla.

2026-05-08 — qa-tester — Patch de handwrittenTests propuesto y validado para
  FEAT-PLANEF (architectural_patch.yaml). 9 tests en total: 6 sobre PlanCmd
  (3 invariantes x 2 path con control negativo) y 3 sobre
  DefaultEphemeralPlanRenderer (schema-equivalencia, round-trip JSON, canal
  stdout). Todos tageados a F-PLANEF-R001. Journey J001 recibe testModule
  audit-cli y testPackage com.learney.contentaudit.auditcli.commands para
  package-private access a PlanCmd, EphemeralPlanRenderer y
  DefaultEphemeralPlanRenderer. Listo para sentinel patch apply +
  sentinel generate -> @test-writer.
  why: la regla unica R001 expone tres invariantes observables y cada uno
  apunta directamente a una de las dos implementaciones; el cruce DISK vs
  EPHEMERAL del invariante 3 se testea sobre PlanCmd con mocks de RefinerEngine
  observando que la firma sealed (auditId, storageMode) entrega el mismo plan
  pre-dispatch en ambos modos. La schema-equivalencia del invariante 2 se
  ancla a FileSystemRefinementPlanStore como referencia del contrato de
  serializacion; round-trip via ObjectMapper(JavaTimeModule) garantiza que un
  cliente que parsea persistidos parsea efimeros sin cambios.

2026-05-08 — test-writer — PlanCmdTest.java actualizado: 5 tests F-CLIRV adaptados a firma
  plan(auditId, PlanStorageMode.DISK), 6 stubs F-PLANEF-R001 implementados, @Mock
  EphemeralPlanRenderer agregado al fixture. Tests no compilan aun por produccion blocker.
  why: produccion PlanCmd.java tiene metodo plan(String) (firma vieja) y Main.java
  instancia PlanCmd sin EphemeralPlanRenderer — @developer debe corregirlos.

2026-05-08 — test-writer — FPlanefJ001JourneyTest implementado (path-1 success + path-2 failure).
  Path-1: arrange auditReportStore.load + refinerEngine.plan exitoso, verifica exitCode=0,
  ephemeralPlanRenderer.render invocado con el plan, refinementPlanStore sin interacciones.
  Path-2: auditReportStore.load retorna Optional.empty() (auditoria no encontrada), verifica
  exitCode!=0, ni save ni render invocados. Produccion aun incompleta: PlanCmd.plan(String, PlanStorageMode)
  no sobreescribe la firma sealed (tiene plan(String) con signature vieja). Compilation falla en
  produccion — blocker para @developer.

2026-05-08 — test-writer — DefaultEphemeralPlanRendererTest: 3 cuerpos implementados.
  Test 1 (schema-equiv): render() -> captura stdout -> ObjectMapper parse -> fieldNames()
  comparados con FileSystemRefinementPlanStore.save() -> fichero json -> fieldNames(). Por tarea idem.
  Test 2 (round-trip): render() -> stdout -> ObjectMapper+JavaTimeModule.readValue -> assertEquals(original, deserialized).
  Test 3 (canal): stdout no blank, stderr empty.
  Captura/restauracion en @BeforeEach/@AfterEach para todos los tests.
  Estado: tests NO pueden correr aun porque audit-cli no compila (PlanCmd firma vieja + Main sin EphemeralPlanRenderer).
  Blocker: @developer debe completar PlanCmd.plan(String, PlanStorageMode) + Main.java.

2026-05-08 — developer — Produccion implementada: PlanCmd.plan(String, PlanStorageMode),
  @Option --storage, call() actualizado, DefaultEphemeralPlanRenderer.render() con ObjectMapper
  identico a FileSystemRefinementPlanStore, Main.java instancia EphemeralPlanRenderer y lo inyecta.
  Compilacion OK. Blocker remanente: PlanCmdTest lineas 364-365 llaman plan.id() y plan.sourceAuditId()
  (record-style), pero RefinementPlan @Generated usa getId()/getSourceAuditId() (JavaBean). Escalacion
  necesaria: @architect debe agregar metodos id() / sourceAuditId() al modelo o regenerarlo como record.

2026-05-09 — analyst — R002 agregada al REQUIREMENT.md: el modo efimero acepta
  una opcion opt-in para emitir el correctionContext inline por tarea (mismo
  shape que get task entrega sobre planes persistidos). Tres doubts nuevos
  abiertos. Journeys J002 (con contexto, exito + parcial + error) y J003 (sin
  contexto, default actual) agregadas. Alcance ampliado para reflejar el segundo
  in-scope. References actualizado para citar FEAT-RCSL adicionalmente y para
  apuntar al feature complementario FEAT-REVCTX.
  why: el cliente del dashboard descubrio que get task no opera sobre planes
  efimeros; la unica via barata para que el plan proyectado sea consumible es
  emitir el contexto inline al momento de calcularlo. Ya implementado el modo
  efimero base (R001), R002 es estrictamente aditivo. Pendiente: pasar a
  @architect para que decida shape de flag y como interactua con el patch
  de R001 ya existente.
