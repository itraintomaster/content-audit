# Progress

2026-05-03 — analyst — Initial REQUIREMENT.md authored for FEAT-PIPRE (preview de impacto de propuestas).
  why: Llena el gap funcional donde el operador no puede saber el efecto de aprobar una propuesta sin aprobarla y reauditar. Eager por propuesta individual; agnostico de dimension; reglas R001..R012 cubren cuando, que, como se muestra y que pasa si falla. Multiples doubts abiertas para arquitecto y para futuras iteraciones (batch preview, staleness detection, exposicion como recurso CLI).

2026-05-03 — analyst — DOUBT-PRESENTATION-FORMAT cerrado; agregada regla F-PIPRE-R013 (presentacion porcentual al operador) y enganchada al journey J002.
  why: El usuario decidio porcentual. La regla nueva mantiene la disciplina "una regla = un test directo" sin contaminar F-PIPRE-R006 (que sigue siendo sobre los tres valores estructurales antes/despues/diferencia). Doubts pendientes restantes: UNAVAILABILITY-TAXONOMY (architect), BATCH-PREVIEW (feature.future), STALENESS-DETECTION (feature.future), PREVIEW-CLI-EXPOSURE (architect).

2026-05-03 — qa-tester — Patch de coverage propuesto: 22 handwrittenTests cubren las 13 reglas R001..R013 de FEAT-PIPRE; ningun gap, ninguna escalacion abierta.
  why: 1 regla = al menos 1 test directo. R001/R010 -> DefaultRevisionEngine; R002..R006/R009/R011/R012 -> DefaultImpactPreviewComputer; R007 -> GetCmd; R008 -> FileSystemImpactPreviewStore; R013 -> DefaultImpactPreviewFormatter. Los 3 journeys F-PIPRE-J001/J002/J003 son flow-based, asi que se cubren via path tests auto-generadas; el patch declara testModule/testPackage para cada uno (J001/J003 -> revision-domain.engine, J002 -> audit-cli.commands). Patch en architectural_patch.yaml; pendiente sentinel patch apply.

2026-05-03 — test-writer — Pre-carga completada. Contratos leidos: ImpactPreview, ImpactPreviewAvailability, ImpactPreviewUnavailability, ImpactPreviewUnavailabilityReason, LevelImpact, ScoreDelta, DimensionDelta (revision-domain/impactpreview), ImpactPreviewStore (revision-domain root), ImpactPreviewComputer (puerto), DefaultImpactPreviewComputer (requiresInject: courseMapper, auditEngine, elementLocator, auditReportStore), DefaultRevisionEngine (constructor aun tiene 8 params — dev no ha agregado impactPreviewComputer/impactPreviewStore todavia), DefaultImpactPreviewFormatter, ImpactPreviewView, LevelImpactView, DimensionDeltaView (audit-cli/formatting). Ready para recibir asignaciones una por una.

2026-05-05 — developer — Tercera restauracion post-regeneracion completada. BUILD SUCCESS en todos los 6 modulos. 461 tests totales, 0 failures, 0 errors. FEAT-PIPRE: 28/28 verde.
  why: El tercer sentinel generate stripeo ~35+ campos/constructores en 15+ archivos. Restaurados + record-style accessors re-agregados a modelos @Generated (ImpactPreview, ScoreDelta, LevelImpact, DimensionDelta, ImpactPreviewUnavailability, RevisionProposal, CourseEntity). Tests @Generated tambien actualizados para compatibilidad con constructores nuevos (FEAT-RCLALEN/FEAT-PIPRE).

2026-05-05 — developer — Restauracion post-segunda-regeneracion completada. Build verde para todos los modulos excepto audit-cli (23 fallas FEAT-RCLALEN pre-existentes). Todos los tests FEAT-PIPRE verdes (22 handwritten + 6 path tests = 28 tests).
  why: El segundo sentinel generate volvio a stripear ~30 campos handwritten. Restaurados manualmente + record-style accessors agregados a modelos @Generated para compatibilidad con tests. FClirvJ001-J005 fallan por classpath (pre-existing).

2026-05-04 — developer — Implementacion completada salvo BLOQUEANTE: CourseMapper boundary violation.
  why: revision-domain genera codigo que importa CourseMapper de audit-application, pero revision-domain no puede depender de audit-application (ciclo: audit-application depende de revision-domain). Escalacion enviada al architect. Todos los demas arreglos de regresion completados (14 archivos restaurados con campos/constructores eliminados por la regeneracion).

2026-05-03 — test-writer — TODOS los 22 tests implementados (17 impl + 5 journey paths). Estado: produccion tiene errores de compilacion (developer en progreso), tests tienen errores de compilacion en cascada esperados. Pendiente critico: revision-domain/pom.xml no tiene audit-application como dependencia — bloquea compilacion de DefaultImpactPreviewComputerTest, FPipreJ001JourneyTest, FPipreJ003JourneyTest, y en cascada FileSystemImpactPreviewStoreTest y los tests de audit-cli. Developer debe agregar audit-application a revision-domain/pom.xml para desbloquear compilacion completa.
  why: TDD — tests escritos contra el contrato (interfaces/modelos), no contra implementacion. La ausencia de la dependencia en pom.xml es trabajo del developer (no del test-writer) segun la regla F4.
