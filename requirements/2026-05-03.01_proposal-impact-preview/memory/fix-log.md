# Fix Log

2026-05-05 — developer — Tercera regeneracion stripeo ~35+ campos en 15+ archivos; restaurados. BUILD SUCCESS 461/0/0. FEAT-PIPRE 28/28.
  why: Patron identico a pasadas anteriores. Adicionalmente: DefaultRevisionEngineFactoryTest y DispatchingReviserTest/CannedTest actualizados para nuevas firmas de constructor (FEAT-RCLALEN/FEAT-PIPRE). LemmaAbsenceLlmGenerator y DefaultLemmaAbsenceLlmGeneratorFactory ajustados por cambio de firma del constructor. FRevbypJ001JourneyTest extendido a 10 params.

2026-05-05 — developer — Segunda regeneracion stripeo ~15 campos mas; restaurados. Ver lista en reporte final.
  why: sentinel generate no preserva zonas handwritten en clases @Generated cuando el YAML no las referencia.

2026-05-05 — developer — Record-style accessors agregados a modelos @Generated para compatibilidad con tests.
  why: Tests usan .availability(), .levelImpacts(), etc. pero modelos generados tienen getX() JavaBean. Patron: metodos delegados sin mutar el modelo.

2026-05-05 — developer — DefaultImpactPreviewComputer: reordenado replace-before-snapshot y uso de course.id() en error message.
  why: Tests esperaban replace() antes de snapshot(); y esperaban course.id() para stub de Mockito strict.

2026-05-05 — developer — LemmaAbsenceContextResolver: lectura de SentenceLengthDiagnosis del nodo quiz para poblar campos de longitud. Fallback cefrLevel desde label del milestone.
  why: FEAT-RCLALEN requeria que el resolver poblara tokenCount/delta/lengthDirection; FEAT-RCLALEN tambien esperaba cefrLevel sin LemmaAbsenceLevelDiagnosis.

2026-05-04 — developer — Regeneracion elimino campos/constructores de 14+ archivos; restaurados manualmente.
  why: La regeneracion elimino zonas handwritten de multiples clases. Patron: constructor condensado a una linea sin campos, luego borrado del original. Ver lista en progress.md.

2026-05-04 — developer — DefaultRevisionEngine.revise(): wiring de preview compute+save post-persist (R001/R010).
  why: Despues de artifactStore.save(artifact), se llama impactPreviewComputer.compute() con try/catch defensivo.

2026-05-04 — developer — DefaultRevisionEngineFactory.create(): construye DefaultImpactPreviewComputer y pasa 10 args a DefaultRevisionEngine.
  why: La factory seguia pasando 8 args al constructor que ahora requiere 10.

2026-05-04 — developer — LemmaAbsenceContextResolver: nuevos campos LemmaAbsenceCorrectionContext pasados como 0/UNKNOWN.
  why: tokenCount/targetMin/targetMax/delta/lengthDirection son del contexto de sentence-length; no aplican para lemma-absence.

2026-05-03 — test-writer — DefaultRevisionEngineTest: extendido buildEngine() de 8 a 10 params para incluir impactPreviewComputer e impactPreviewStore. @Mock fields agregados; tests existentes FEAT-REVBYP/FEAT-REVAPR no son afectados porque Mockito usa defaults para los nuevos mocks donde no estan stubbed.
  why: El constructor de DefaultRevisionEngine ahora tiene 10 params (sentinel.yaml actualizado por architect). Los tests existentes llaman buildEngine() y se benefician automaticamente sin cambios adicionales.

2026-05-03 — test-writer — GetCmdTest R007: inyeccion de impactPreviewStore e impactPreviewFormatter via reflection (getDeclaredMethod + setAccessible + invoke) en lugar de @Mock class-level. Patron necesario porque @BeforeEach existente no puede ser modificado (test existente) y los setters del GetCmd siguen el patron setRevisionArtifactStore() ya establecido.
  why: GetCmd usa setter injection para dependencias opcionales; los tests de journey F-PIPRE-J002 usan BeforeEach propio para montar setUp completo con los nuevos setters.

2026-05-03 — test-writer — FPipreJ002JourneyTest: usa DefaultImpactPreviewFormatter real (no mock) para el caso path-2 que prueba la cadena completa de presentacion. Esto permite verificar que unavailabilityText contiene la causa sin acoplar el test a la implementacion del formatter.
  why: El formatter es una funcion pura sin side effects; usar la instancia real da cobertura mas fidedigna al journey de extremo a extremo.
