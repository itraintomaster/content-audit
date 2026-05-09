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

2026-05-09 — test-writer — DefaultEphemeralPlanRendererTest adaptacion R001: renderer instanciado como
  new DefaultEphemeralPlanRenderer(auditNodeIndexFactory, correctionContextResolver, correctionContextJsonMapper)
  con @Mock Mockito en la clase de test (no @InjectMocks, renderer no es @Command picocli).
  render(plan) -> render(plan, report, new EphemeralRenderOptions(false)). AuditReport report = new AuditReport(null).
  why: constructor cambio para R002; los tests R001 (withCorrectionContext=false) usan lenient stubs solo
  para lo que invocan; los mocks de context resolver y mapper no se usan con false (ni se stubbean).

2026-05-09 — test-writer — PlanCmdTest: verify(ephemeralPlanRenderer, never()).render(any()) ->
  verify(ephemeralPlanRenderer, never()).render(any(), any(), any()).
  why: render ahora tiene 3 parametros; un solo any() no matchea metodo de 3 parametros con Mockito.

2026-05-09 — test-writer — DefaultEphemeralPlanRendererTest R002 invariante #4 (correctionContextError):
  El campo esperado cuando el resolver retorna Optional.empty se llama "correctionContextError".
  Si la produccion usa un nombre distinto, el test fallara con "campo correctionContextError no encontrado".
  Escalacion al developer si el campo tiene otro nombre.

2026-05-09 — developer — DefaultEphemeralPlanRenderer: campo correctionContextResolver es raw type
  CorrectionContextResolver (sin param generico); rawtype no resuelve resolveWithIndex hasta que
  el JAR actualizado de refiner-domain esta instalado en .m2. Solucion: instalar refiner-domain antes
  de compilar audit-cli.
  why: CorrectionContextResolver<T> con raw type borra genericos; el compilador busca el .class en el
  JAR instalado; si el JAR es viejo (sin resolveWithIndex), falla con "cannot find symbol".

2026-05-09 — developer — PlanCmd: interface PlanCommand ya fue regenerada con firma plan(String, PlanStorageMode, boolean).
  No agregar un delegate plan(String, PlanStorageMode) con @Override; el compilador falla porque
  la interface ya no tiene ese metodo. Mantener solo el @Override al metodo de 3 args.

2026-05-09 — test-writer — J002/J003 journey tests al nivel PlanCmd (no al nivel renderer).
  path-1 y path-2 de J002 son distinguibles semanticamente (todas tareas vs parcial) pero producen
  el mismo arrange/act/assert a nivel PlanCmd: la distincion real esta en el renderer (cubierta por
  DefaultEphemeralPlanRendererTest). El journey test de PlanCmd verifica que el comando despacha al
  renderer con la opcion correcta (EphemeralRenderOptions(true)) y no toca el store. Es el nivel
  de abstraccion correcto para un journey test de la capa de comandos.
  why: no hay ninguna decision en PlanCmd que diferencie path-1 de path-2; la diferencia es interna
  al renderer (resolver retorna Optional.empty para algunas tareas). Forzar esa distincion en el
  journey test de PlanCmd requerina mockear tipos no declarados en requiresInject.

2026-05-08 — developer — DefaultEphemeralPlanRenderer.render(): usa ObjectMapper con JavaTimeModule
  + disable(WRITE_DATES_AS_TIMESTAMPS) + writerWithDefaultPrettyPrinter().writeValueAsString(plan).
  Mismo config exacto que FileSystemRefinementPlanStore para garantizar schema-equivalencia.
  Imprime con System.out.println (agrega trailing newline; captura de stdout en tests la recibe).
  Retorna 0 en exito, 1 en excepcion (nada a stdout en error path).
  why: round-trip test requiere deserializacion a RefinementPlan; usar la misma config que el store
  garantiza que ambos producen el mismo JSON y el mismo schema.
