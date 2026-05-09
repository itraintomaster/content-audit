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

2026-05-09 — qa-tester — Patch propuesto y validado para F-PLANEF-R002 (architectural_patch.yaml).
  6 handwrittenTests nuevos: 1 sobre PlanCmd (rechazo DISK + withCorrectionContext) +
  5 sobre DefaultEphemeralPlanRenderer (opt-in OFF, contexto inline ON, equivalencia
  funcional con get task via mapper compartido, contexto nulo + motivo, plan parcial).
  Todos taggeados a F-PLANEF-R002. Cero adiciones/eliminaciones, solo modifications.
  Tests existentes de R001 (6 PlanCmd + 3 renderer) preservados sin cambios en
  sentinel.yaml; @test-writer adaptara cuerpos a las nuevas firmas plan(...,boolean)
  y render(plan,report,options).
  why: las 5 invariantes de R002 mapean directo a los dos puntos de observacion
  naturales (PlanCmd para el rechazo DISK, renderer para el contenido del JSON
  emitido). Inv #5 (ortogonal a la fuente del analisis) ya cubierta por R001 inv 3.
  Detalles arquitectonicos sin rule observable (mapper compartido, AuditNodeIndex,
  resolveWithIndex) cubiertos transitivamente — confirmado con el usuario, no se
  fuerza traceability.

2026-05-09 — test-writer — Tests R002 implementados: 1 stub PlanCmdTest (DISK+withCorrectionContext=true rechazo
  explicito) + 5 stubs DefaultEphemeralPlanRendererTest (opt-in OFF, contexto inline ON, equivalencia
  mapper compartido, contexto null+motivo, plan parcial exitCode=0). 9 tests R001 adaptados: firmas
  plan(auditId, storageMode, false) y render(plan, report, EphemeralRenderOptions(false)).
  Test-compile OK contra bytecode previo. Blocker produccion: PlanCmd.plan firma vieja + DefaultEphemeralPlanRenderer.render
  firma vieja + Main.java sin nuevas dependencias + GetCmd sin CorrectionContextJsonMapper.
  @developer debe actualizar para que el modulo vuelva a compilar completamente.
  why: TDD normal — tests implementados primero, produccion pendiente.

2026-05-09 — qa-tester — ESCALACION pendiente al @architect: F-PLANEF-J002 y
  F-PLANEF-J003 (ambos flow journeys con outcomes/then) declaradas en REQUIREMENT.md
  pero NO listadas en sentinel.yaml#features[FEAT-PLANEF].journeys. listModules
  emite warnings y sentinel generate las saltea. Tests path-based de R002 (3 outcomes
  J002: completo/parcial/error + 1 outcome J003: sin opcion preserva R001) no se
  generan. Propuesta: agregar ambas con testModule=audit-cli y
  testPackage=com.learney.contentaudit.auditcli.commands (mismo paquete que J001
  para mantener visibilidad package-private a EphemeralPlanRenderer y PlanCmd).
  Usuario confirma que la rutea a @architect despues del patch QA para evitar
  conflictos en sentinel.yaml.
  why: flow journeys se cubren via *JourneyTest auto-generados; usar
  traceability.journey en handwrittenTests para flow journeys es validation error.
  Por eso los 6 handwrittenTests R002 propuestos taguean SOLO a la rule, dejando
  los outcomes a las journey classes que el architect debe materializar.

2026-05-09 — developer — R002 implementado. 10 archivos modificados.
  audit-domain: MapAuditNodeIndex (walk+index), DefaultAuditNodeIndexFactory.
  refiner-domain: SentenceLengthContextResolver/LemmaAbsenceContextResolver resolveWithIndex+supports,
    DispatchingCorrectionContextResolver resolveWithIndex+supports.
  audit-cli: DefaultCorrectionContextJsonMapper (migrado desde GetCmd), GetCmd (addContextToJsonMap
    usa mapper, metodos privados borrados), DefaultEphemeralPlanRenderer (render con opciones),
    PlanCmd (firma nueva, opcion --with-correction-context, validacion R002), Main (wiring actualizado).
  Verificacion: audit-domain 208 tests OK, refiner-domain 69 tests OK.
  audit-cli test-compile falla SOLO en ReviseCmd (FEAT-REVCTX, out of scope).

2026-05-09 — architect — Lead session iniciada. Verificado estado de partida:
  - mvn -pl audit-cli test-compile FALLA con 7 errores en scope FEAT-PLANEF
    (PlanCmd no override, DefaultEphemeralPlanRenderer no override, Main:425
    constructor 0-args, Main:453 GetCmd sin mapper, render con firma vieja en
    PlanCmd:111). 2 errores en scope FEAT-REVCTX (ReviseCmd) — fuera de scope.
  - audit-domain compila/instala OK; AuditNodeIndex/Factory + impls existen
    pero retornan throw new UnsupportedOperationException.
  - DefaultCorrectionContextJsonMapper.toJsonMap retorna throw new UOE.
  - SentenceLengthContextResolver/LemmaAbsenceContextResolver/Dispatching:
    resolveWithIndex y supports retornan throw new UOE.
  - GetCmd ya tiene CorrectionContextJsonMapper inyectado (constructor
    7-args linea 138), pero todavia llama buildSentenceLengthContextMap/
    buildLemmaAbsenceContextMap privados (lineas 778, 806). Migracion al
    mapper compartido pendiente.
  - Bug Sentinel reportado para J002/J003 deadlock. Avanzando sin journeys
    materializados (cobertura R002 funcional por handwrittenTests).
  Proximo: spec entregado a developer (un solo lote: 7 archivos).

2026-05-09 — test-writer — FPlanefJ002JourneyTest (3 paths) y FPlanefJ003JourneyTest (1 path) implementados.
  Misma estructura que J001: @InjectMocks PlanCmd, @Mock EphemeralPlanRenderer/etc., refleccion picocli.
  J002 path-1/path-2: plan(auditId, EPHEMERAL, true) + render(plan, report, EphemeralRenderOptions(true)) mock 0.
  J002 path-3: auditReportStore.load -> Optional.empty(), verifica exitCode!=0, ni render ni save invocados.
  J003 path-1: plan(auditId, EPHEMERAL, false) + render(plan, report, EphemeralRenderOptions(false)) mock 0.
  J001 actualizado simultaneamente: firma plan() de 2-args -> 3-args y render() de 1-arg -> 3-args.
  why: J001 usaba API vieja (plan(id,mode) + render(plan)); la interface PlanCommand y EphemeralPlanRenderer
  ya tienen las firmas de 3 argumentos desde el patch de R002.
