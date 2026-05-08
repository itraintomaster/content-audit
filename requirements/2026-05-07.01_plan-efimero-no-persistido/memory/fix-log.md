# Fix Log - FEAT-PLANEF

2026-05-08 — test-writer — EphemeralPlanRenderer es package-private en auditcli.commands;
  los tests en el mismo paquete pueden usarla con @Mock sin reflection ni workarounds.
  why: visibilidad internal en sentinel.yaml => interface sin modificador public en Java.

2026-05-08 — test-writer — Journey test usa @ExtendWith(MockitoExtension.class) + @InjectMocks PlanCmd
  + reflection initPicocliOptionDefaults (mismo patron que PlanCmdTest) para inicializar campos
  @Option de picocli que Mockito deja null. Sin esto NPE antes del business logic.
  why: PlanCmd es un @Command picocli; sus @Option fields no tienen valor inicial cuando Mockito
  los crea; el reflection loop busca String fields null y los setea a "table".

2026-05-08 — test-writer — Path-2 failure usa auditReportStore.load(unknownId) -> Optional.empty()
  como trigger de falla. Es el caso mas representativo de "auditoria fuente no encontrada" del journey
  YAML, y coincide con el patron establecido en PlanCmdTest#shouldReportAuditIdNotFoundAndExitNonzero.
  why: evita acoplar el test a comportamiento interno de RefinerEngine (throw vs return); la falla
  a nivel de store es mas proxima al nodo de journey "invocar_plan_efimero" que la falla del engine.

2026-05-08 — test-writer — DefaultEphemeralPlanRendererTest: captura de streams en @BeforeEach/@AfterEach
  (no en try/finally por metodo).
  why: si cada test captura/restaura localmente y falla antes del finally, el stream queda
  redirigido para tests posteriores. @BeforeEach/@AfterEach garantiza restauracion aunque el
  test lance excepcion en cualquier punto.

2026-05-08 — developer — DefaultEphemeralPlanRenderer.render(): usa ObjectMapper con JavaTimeModule
  + disable(WRITE_DATES_AS_TIMESTAMPS) + writerWithDefaultPrettyPrinter().writeValueAsString(plan).
  Mismo config exacto que FileSystemRefinementPlanStore para garantizar schema-equivalencia.
  Imprime con System.out.println (agrega trailing newline; captura de stdout en tests la recibe).
  Retorna 0 en exito, 1 en excepcion (nada a stdout en error path).
  why: round-trip test requiere deserializacion a RefinementPlan; usar la misma config que el store
  garantiza que ambos producen el mismo JSON y el mismo schema.
