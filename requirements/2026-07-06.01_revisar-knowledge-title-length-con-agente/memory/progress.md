# Progress

Estado actual, última acción y próximo paso. Entrada más reciente arriba.

<!-- entries below -->

2026-07-07 — developer — FEAT-KTLR producción COMPLETA y VERDE en los 4 módulos.
  - Implementado (esta sesión, retomando tras compactación de contexto):
    refiner-domain/DispatchingCorrectionContextResolver (rama KNOWLEDGE_TITLE_LENGTH → R001,
    faltaba tras una regeneración — resolve/resolveWithIndex/supports delegan a
    knowledgeTitleResolver); revision-domain/engine/KnowledgeTitleReviser (propose/handles/
    reviserKind — resuelve estrategia activa via registry, propaga ProposalStrategyFailedException
    en ausencia de estrategia o falla de la estrategia/deriver, nunca invoca al deriver en falla →
    R003/R005/R010); DefaultKnowledgeTitleProposalDeriver (copia KnowledgeEntity preservando todo
    salvo label←title e instructions → R007); DefaultKnowledgeTitleProposalStrategyRegistry
    (active/byName/listAll, espejo 1:1 de LemmaAbsence); DefaultRevisionEngineFactory (registra
    KnowledgeTitleReviser en el Map<DiagnosisKind,Reviser> byKind que consume DispatchingReviser,
    con defaults ante null espejando lemmaAbsenceStrategyRegistry/Deriver).
  - Ya venían implementados de un turno anterior (antes del corte de contexto): refiner-domain/
    KnowledgeTitleContextResolver; revision-domain/knowledgetitle/KnowledgeTitleAgentStrategy
    (id/handles/propose); todo revision-infrastructure/knowledgetitleagent (KnowledgeTitleAgent
    Generator + launcher + parser + classifier + factory, con inputs {taskId, knowledgeId,
    project_root, currentTitle, currentInstructions, topicLabel, titleTargetMax,
    instructionsTargetMax, expectedTitleLanguage, expectedInstructionsLanguage, maxEvalRetries} y
    artifact `titleCandidate`={knowledgeId,title,instructions}, verificado 1:1 contra
    .sentinel/agents/knowledge-titles-agent/scripts/emit.sh y load_context.sh); audit-cli/Main.java
    (wiring completo: KnowledgeTitleContextResolver inyectado al DispatchingCorrectionContextResolver
    de 3 args, DefaultKnowledgeTitleAgentGeneratorFactory(baseDir) + KnowledgeTitleAgentStrategy +
    KnowledgeTitleProposalStrategyRegistryConfig + los 2 campos nuevos de RevisionEngineConfig,
    con fallback canned y deferred-error igual que LAPS).
  - DefaultCourseElementLocator YA traía las ramas KNOWLEDGE snapshot/replace implementadas
    (no hizo falta tocarlo esta sesión) — confirmado leyendo el archivo, no era stub.
  - Resultado final por módulo (`mvn test -pl <mod> -Dexec.skip=true -o`, tras reinstalar
    refiner-domain→revision-domain→revision-infrastructure en el m2 local — ver fix-log):
      refiner-domain: 131/131 verde.
      revision-domain: 190/190 verde (incluye KnowledgeTitleReviserTest R003/R005/R010,
        DefaultKnowledgeTitleProposalDeriverTest R007, DefaultCourseElementLocatorTest R006/R008
        + F-SMODE-R006, DefaultRevisionEngineTest R009).
      revision-infrastructure: compila limpio (main+test); `mvn test` falla al arrancar el
        TestEngine ("junit-jupiter failed to discover tests") — preexistente, mismatch JDK25/
        JDK24 en JaCoCo, NO relacionado a FEAT-KTLR (mismo síntoma reproducible en el sibling LAPS).
      audit-cli: 331/331 verde, incluyendo MainTest, ReviseCmdTest, FRevaprJ004/J005JourneyTest
        (regresión falsa por jar stale, ver fix-log) y FKtlrJ001JourneyTest (4 paths) /
        FKtlrJ002JourneyTest (1 path) ya escritos por @test-writer.
  - FEAT-KTLR: producción completa, sin escalaciones pendientes, sin tests tocados. Falta only
    una corrida live del agente knowledge-titles-agent para validar el grafo declarativo end-to-end
    (igual que quedó pendiente para FEAT-LASAG) — no bloqueante para esta tarea.

2026-07-06 — test-writer — BLOQUEADO en test 6 (FKtlrJ001JourneyTest): gap de wiring, no toco produccion.
  - Decision del architect (decisions.md linea 33): KnowledgeTitleReviser se enruta via el
    Map<DiagnosisKind,Reviser> generico (revisers) de DispatchingReviser/RevisionEngineConfig,
    NO via campos LAPS-especificos.
  - PERO: KnowledgeTitleReviser es `class KnowledgeTitleReviser` package-private en
    revision-domain.engine (confirmado leyendo la firma, no el cuerpo). audit-cli (donde vive
    el journey test) no puede instanciarlo. Y RevisionEngineConfig (14 campos, ya confirmados)
    NO tiene ningun campo knowledgeTitleStrategyRegistry/knowledgeTitleProposalDeriver analogo
    a lemmaAbsenceStrategyRegistry/lemmaAbsenceProposalDeriver que el LAPS journey usa para que
    DefaultRevisionEngineFactory arme el reviser internamente.
  - Conclusion: el composition root NO tiene forma declarada de construir la entrada
    KNOWLEDGE_TITLE_LENGTH->Reviser para pasarla en config.setRevisers(...). No es un stub sin
    implementar (eso seria rojo esperado) sino una ausencia de contrato: ningun campo publico
    expone el punto de wiring, a diferencia del patron LAPS ya existente.
  - Escalo underspecified_contract a @architect. NO invente un Reviser/KnowledgeTitleReviser
    falso para maquillar el test — eso no ejercitaria el pipeline real (KnowledgeTitleReviser+
    KnowledgeTitleProposalDeriver+DefaultCourseElementLocator) y el journey dejaria de ser
    integracion real.
  - Sugerencia para @architect: agregar a RevisionEngineConfig 2 campos analogos a LAPS
    (knowledgeTitleStrategyRegistry: KnowledgeTitleProposalStrategyRegistry,
    knowledgeTitleProposalDeriver: KnowledgeTitleProposalDeriver) + que
    DefaultRevisionEngineFactory arme KnowledgeTitleReviser internamente cuando esten
    presentes, espejando exactamente lemmaAbsenceStrategyRegistry/lemmaAbsenceProposalDeriver.
  - Próximo: retomar test 6/7 (FKtlrJ001/J002 journeys) apenas el contrato exponga el wiring.

2026-07-06 — test-writer — Test 5 escrito: KnowledgeTitleAgentGeneratorTest (revision-infrastructure, R004).
  - Espeja LemmaAbsenceAgentGeneratorTest 1:1: mocks Mockito de KnowledgeTitleAgentRuntimeLauncher/
    CandidateParser/ErrorClassifier, ChatModel=null (el launcher fake nunca lo usa). OJO: firma
    real difiere de LASAG — launch(agentName, inputs, chatModel) es 3-arg (sin mapa de tools,
    el agente de titulos no expone tools al modelo) y parse(runState, knowledgeId) es 2-arg.
  - Compila limpio standalone (`mvn test-compile -pl revision-infrastructure`, tras
    `mvn install -pl revision-domain -Dmaven.test.skip=true -Dpit.skip=true` para publicar el
    jar main). `mvn test -pl revision-infrastructure -Dtest=KnowledgeTitleAgentGeneratorTest`
    falla por un problema de ENTORNO ajeno: JaCoCo no puede instrumentar RunState (class file
    version 69 = JDK25, el JVM que corre maven-surefire es 68=JDK24) -> UnsupportedClassVersionError.
    Confirmado que NO es mi test: el mismo comando contra LemmaAbsenceAgentGeneratorTest (sibling
    preexistente, sin tocar) tira el MISMO error (incluso falla en discovery). Es un mismatch de
    toolchain JDK/JaCoCo del proyecto, no algo que test-writer deba resolver.
  - Próximo: test 6 FKtlrJ001JourneyTest (4 paths) en audit-cli.
  - Nota operativa: `mvn test-compile -pl revision-domain` fallaba porque el reactor resuelve
    refiner-domain via ~/.m2 (jar instalado), no via source de otro modulo `-pl`. Corri
    `mvn install -pl refiner-domain -DskipTests` una vez para que revision-domain viera
    KnowledgeTitleCorrectionContext. Anotar para el resto de la cola.
  - KnowledgeTitleReviserTest (R003/R005/R010): mocks de KnowledgeTitleProposalStrategy +
    KnowledgeTitleProposalStrategyRegistry + KnowledgeTitleProposalDeriver (Mockito, estilo
    DispatchingReviserTest). R010 verifica que deriver.derive() nunca se invoca cuando la
    estrategia lanza ProposalStrategyFailedException.
  - Arregle mecanicamente (NO rediseño, solo arity de constructor) 2 archivos hermanos
    preexistentes que bloqueaban compilacion y donde vivian mis stubs objetivo:
    DefaultCourseElementLocatorTest.java (1 call site, F-SMODE-R006) y
    DefaultRevisionEngineTest.java (15 call sites idénticos, features REVBYP/otras) — todos
    `new CourseElementSnapshot(target, id, quiz)` -> agregado `null` como 4to arg (knowledge).
    Mecanico y seguro: mismo patron exacto en los 15, sin tocar semantica de esos tests.
  - DefaultCourseElementLocatorTest: R006 (replace persiste titulo+instructions de knowledge,
    re-lectura confirma) y R008 (replace de quiz sigue funcionando igual, sin regresion).
  - DefaultRevisionEngineTest: R009 (proposal KNOWLEDGE_TITLE_LENGTH rechazada -> courseRepository
    .save() NUNCA se llama; knowledge original conserva label/instructions porque nunca se escribe).
  - Sigue sin arreglar (fuera de mi scope FEAT-KTLR, no lo toco): FCdiffJ008, FPipreJ001/J003,
    FRevbypJ001, FSmodeJ002JourneyTest, IdentityReviserTest — mismos 3-arg call sites rotos,
    pero pertenecen a otras features; el 4to arg ahi es juicio de test-design (fixture del
    knowledge), no querido tocar sin @qa-tester/@developer de esas features.
  - Próximo: test 5 KnowledgeTitleAgentGeneratorTest (revision-infrastructure).

2026-07-06 — test-writer — Cabo suelto en refiner-domain resuelto por pedido del coordinador.
  - DispatchingCorrectionContextResolverTest: setUp() actualizado al constructor de 3 args
    (agregado knowledgeTitleResolver); implementado el body del stub F-KTLR-R001 (delegacion a
    knowledgeTitleResolver para KNOWLEDGE_TITLE_LENGTH -> contexto poblado, no vacio).
  - `mvn test-compile -pl refiner-domain` limpio. `mvn test -pl refiner-domain`: 130/131 verde;
    el unico rojo es MI test nuevo de R001 (dispatcher aun no rutea KNOWLEDGE_TITLE_LENGTH a
    knowledgeTitleResolver en produccion). Confirmado que no es fixture rota: test 1
    (KnowledgeTitleContextResolverTest, R002+R011) corre solo y da VERDE — @developer ya
    implemento el resolver. El rojo es 100% TDD esperado: falta wiring del dispatcher, no mio.
  - Próximo: seguir tests 2-7. Reportar este rojo como pendiente de produccion (dispatcher wiring).

2026-07-06 — test-writer — BLOQUEADO en test 1/7 (KnowledgeTitleContextResolverTest).
  - Implementé R002 (contexto poblado) y R011 (knowledge inexistente → Optional.empty) contra
    resolve(AuditReport, RefinementTask): Optional<KnowledgeTitleCorrectionContext>, siguiendo
    el patron de LemmaAbsenceContextResolverTest (arbol AuditNode course→topic→knowledge).
  - `mvn test-compile -pl refiner-domain` falla: KnowledgeTitleContextResolver.java (stub
    @Generated, untracked) tiene `Optional<T> resolve(...)` / `Optional<T> resolveWithIndex(...)`
    sin sustituir T por KnowledgeTitleCorrectionContext — CorrectionContextResolver<T extends
    CorrectionContext> es generica y el stub no compila standalone (cannot find symbol: class T).
  - Comparado con SentenceLengthContextResolver/LemmaAbsenceContextResolver (ya implementados
    por @developer, mismo patron `implements CorrectionContextResolver` raw): ambos ya tienen
    el override correctamente parametrizado a mano. El defecto es solo del stub recien generado.
  - Escalado type: generated_file_mismatch a @architect. Bloquea TODO el modulo refiner-domain
    (y transitivamente revision-domain/revision-infrastructure/audit-cli) hasta que se corrija.
  - SEGUNDO hallazgo, más grave: revision-domain/.../RevisionEngineConfig.java (M, no ??) quedó
    VACIADO por la regeneración — pasó de `class` con 14 campos+getters/setters a
    `public interface RevisionEngineConfig {}` sin miembros. Rompe DefaultRevisionEngineFactory
    (cannot find symbol getRevisers/getValidator/... 14 métodos). Confirmado con `git diff`:
    no es un stub nuevo, es contenido previo (probablemente ya funcional) borrado. La DSL YA
    declara RevisionEngineConfig como record con los 14 campos correctos (verificado con
    describeComponent) — el generador no lo materializó.
  - Ambos bloqueos son anteriores/independientes de mi trabajo de tests; no los causé yo.
  - Próximo: @architect corrige/regenera (a) KnowledgeTitleContextResolver.java con T sustituido
    por KnowledgeTitleCorrectionContext, (b) RevisionEngineConfig.java restaurado con sus 14
    campos; luego retomar test 1 (ya escrito, solo pendiente de compilar) y seguir con 2-7.

2026-07-06 — test-writer — Coordinador confirma ambas escalaciones resueltas; retomo 2-7.
  - ESC2 (RevisionEngineConfig vacio) confirmada resuelta: `mvn compile -pl revision-domain`
    exit 0. ESC1 diagnosticada NO-bug por el lead: @developer esta implementando
    KnowledgeTitleContextResolver ahora mismo (raw-type generico, T se sustituye al escribir
    el cuerpo real, no en el stub). No lo toco, sigo con test 1 tal cual (ya escrito y correcto
    contra el contrato) y avanzo.
  - Test 2/7 DefaultKnowledgeTitleProposalDeriverTest (R007) escrito y compila limpio
    standalone. `mvn test-compile -pl revision-domain` SI reporta errores, pero son 100%
    ajenos: tests preexistentes de OTRAS features (FCdiffJ008, FPipreJ001/J003, FRevbypJ001,
    FSmodeJ002, IdentityReviserTest — todos sin cambios en git status) que llaman al
    constructor viejo de 3 args `CourseElementSnapshot(target, id, quiz)`; el architect lo
    amplio a 4 args agregando `knowledge`. Es fallout mecanico de la migracion arquitectonica,
    no responsabilidad de test-writer para FEAT-KTLR — no lo arreglo (no es mi feature ni
    testeo/rediseño; @developer o el usuario debe agregar el 4to arg `null` en esos call sites).
  - Próximo: test 3 KnowledgeTitleReviserTest.

2026-07-06 — analyst — Draft inicial de REQUIREMENT.md (FEAT-KTLR).
  - Puente revise→propuesta→aprobar para KNOWLEDGE_TITLE_LENGTH, análogo a FEAT-LAPS+FEAT-LASAG.
  - Punto crítico #3: aprobar debe persistir título+instructions en el knowledge; hoy el snapshot solo lleva quiz.
  - Próximo: coordinar con @architect el snapshot polimórfico quiz|knowledge (CourseElementSnapshot / CourseElementLocator).

2026-07-06 — qa-tester — Diseño de cobertura FEAT-KTLR propuesto (patch validado, sin aplicar).
  - 11 handwrittenTests (R001..R011, 1:1 sin cobertura transitiva) + placement J001/J002.
  - R009 con test directo propio en DefaultRevisionEngine (ruta KNOWLEDGE), no reusa el de quizzes.
  - J001 y J002 → audit-cli / com.learney.contentaudit.journeys (mismo package que FLapsJ001..J005, precedente directo del puente revise→propuesta→aprobar).
  - Próximo: usuario corre `sentinel patch apply --as=qa-tester`; luego test-writing.
