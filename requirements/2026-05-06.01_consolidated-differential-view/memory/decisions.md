# Decisions

2026-05-06 - analyst - Hoja con aceptada+pendiente: la pendiente se compara contra el vigente (=aceptada), no contra el baseline original. Maximo dos valores por campo.
  why: usuario lo pidio explicitamente; cualquier otra opcion introduce un valor "fantasma" del baseline que ya no es recuperable porque la aceptada ya esta materializada en el curso.

2026-05-06 - analyst - Deltas de estadisticas son puntos absolutos sobre la escala del dominio, no porcentajes relativos. La UI puede presentarlos como `+5%` por convencion, pero matematicamente son diferencias absolutas.
  why: usuario lo aclaro explicitamente; evita el caso ambiguo de "+5%" interpretado como "cinco por ciento mas que el original" (relativo) vs. "cinco puntos porcentuales mas" (absoluto).

2026-05-06 - analyst - Padre se calcula re-agregando subarbol con la estrategia del analizador, no sumando deltas de hijos. Aplica explicitamente a casos no lineales como COCA_BUCKETS (acumulacion de conteos).
  why: FEAT-COCA usa estrategia de agregacion polimorfica (F-COCA-R029); sumar deltas individuales daria resultados incorrectos para analizadores no-lineales.

2026-05-06 - analyst - La vista consolidada NO es un AuditReport nuevo. La generacion de la vista nunca emite auditId.
  why: paralelo a F-PIPRE-R002 generalizado al curso entero. Preserva la separacion entre historia oficial (AuditReports escritos por el motor) y vista derivada (lo que la UI ve combinando esa historia con decisiones del operador).

2026-05-06 - analyst - Refactor del REQUIREMENT.md: pasa de "vista" a "contrato de datos del CLI". Reglas describen los campos canonicos (consolidated, pendingProjection, acceptedDelta, pendingDelta, acceptedProposalIds, pendingProposalId, pendingApplicability, consolidatedAvailability) y su semantica. Lo de "como lo ve el operador" queda como contexto, no como reglas.
  why: pedido explicito del usuario via team-lead. content-audit es CLI/backend; la UI es un consumidor externo. Las reglas tienen que hablar de datos emitidos, no de presentacion. Se incorpora R016 explicito que supersede F-PIPRE-R011/DOUBT-BATCH-PREVIEW como pidio architect.

2026-05-06 - analyst - DOUBT-FIELD-IDENTITY se marca RESOLVED con Opcion C (granularidad coarse a nodo entero). El consumidor que necesite diff fino lo computa comparando consolidated vs pendingProjection.
  why: feedback de architect; CourseElementSnapshot hoy solo carga quiz: QuizTemplateEntity para QUIZ y los demas niveles tienen snapshot vacio. Distinguir campo dentro del nodo es ingenieria especulativa sin caso de uso real.

2026-05-06 - analyst - DOUBT-PENDING-CONFLICT se marca RESOLVED con Opcion A (orden por createdAt; choque cae en R012 NOT_APPLICABLE).
  why: feedback de architect; F-REVAPR-R010 ya cubre el caso comun y R012 ya soporta exactamente la semantica de "elementBefore no coincide con vigente".

2026-05-06 - architect - DOUBT-FIELD-IDENTITY resuelto a Opcion C (granularidad coarse a nodo entero) acordado con analyst.
  why: hoy todas las propuestas son QUIZ-target via FEAT-LAPS; CourseElementSnapshot solo carga quiz para QUIZ. Sin propuestas multi-campo reales, distinguir "campo dentro del nodo" requeriria tocar RevisionProposal (alto blast radius sobre FEAT-REVBYP/REVAPR/PIPRE/LAPS) sin caso de uso real. NodeImpact reusa CourseElementSnapshot existente; el consumidor que necesite diff fino compara consolidated vs pendingProjection.

2026-05-06 - architect - DOUBT-PENDING-CONFLICT resuelto a Opcion A (orden createdAt + R012 catch).
  why: F-REVAPR-R010 cubre el caso comun (no acumular pendientes sobre la misma tarea). Para "dos pendientes sobre el mismo nodo desde tareas distintas", aplicar en orden createdAt y dejar que la segunda caiga en R012 (elementBefore != consolidated -> NonApplicablePending con ELEMENT_BEFORE_MISMATCH) reusa la maquinaria de R012 sin agregar mecanismo nuevo.

2026-05-06 - architect - DOUBT-MATERIALIZATION resuelto a Opcion A (on-demand sin cache) para MVP.
  why: dos invocaciones de AuditEngine.runAudit por request (consolidated + pendingProjection) en cursos de ~11.500 quizzes puede ser caro pero todavia no medimos. Agregar cache especulativo seria over-engineering. La signatura narrow del puerto (build(coursePath): ConsolidatedView) deja libre meter cache detras sin tocar consumidores en una iteracion futura.

2026-05-06 - architect - DOUBT-ACTIVE-PERSISTENCE resuelto a Opcion A (archivo dedicado .content-audit/active-analysis.json).
  why: simetria con FileSystemAuditReportStore / FileSystemRefinementPlanStore (todos viven en .content-audit/). Un solo escritor, atomic-replace, idempotencia (write con mismo par = no-op) implementada en el adapter. Centraliza las garantias de F-CDIFF-R002 en un solo lugar testeable.

2026-05-06 - architect - DOUBT-UNAVAILABILITY-TAXONOMY: reusa el patron categoria-enum + detalle-string ya elegido para FEAT-PIPRE.
  why: consistencia entre features. ConsolidatedViewUnavailabilityReason tiene 5 valores (NO_ACTIVE_ANALYSIS / ACTIVE_PLAN_UNAVAILABLE / INCONSISTENT_PROPOSAL / REAGGREGATION_FAILED / OTHER) mas detalle textual. Mismo shape que ImpactPreviewUnavailability + ImpactPreviewUnavailabilityReason de FEAT-PIPRE.

2026-05-06 - architect - ActiveAnalysisSelectionStore vive en audit-domain (no revision-domain).
  why: la seleccion de "que AuditReport esta vigente" es un concepto del bounded context de auditoria, no de revision. El consolidador es solo uno de sus consumidores; futuros (CLI 'get audit --active', otros visualizadores) lo leen del mismo store. revision-domain ya depende de audit-domain, asi que no hay friccion para que el consolidador lo consuma.

2026-05-06 - architect - Package consolidatedview en revision-domain (no modulo nuevo).
  why: la feature consume RevisionArtifact + AuditReport + CourseEntity, exactamente las dependsOn que revision-domain ya tiene. Crear un modulo "consolidation-domain" duplicaria el grafo sin ganar encapsulacion. El package es public; la implementacion (DefaultConsolidatedViewBuilder + factory) vive en el package interno engine. Patron Public Port + Hidden Adapter intra-modulo, identico al de impactpreview.

2026-05-06 - architect - NodeImpact reusa CourseElementSnapshot existente; snapshots solo poblados para QUIZ.
  why: para padres (KNOWLEDGE/TOPIC/MILESTONE/COURSE) consolidated y pendingProjection son null porque CourseElementSnapshot solo lleva quiz. El impacto de los padres se expresa via la lista StatisticImpact que comparten (nodeTarget, nodeId). Esta separacion entre "snapshot del nodo hoja" y "metricas del padre" simplifica la salida y respeta el modelo de datos existente.

2026-05-06 - architect - StatisticImpact con Double boxed para nullables (pendingProjection, pendingDelta).
  why: distingue "no hay pendiente que toque la estadistica" (null) de "delta cero" (0.0). Una estadistica solo tocada por aceptadas tiene pendingProjection=null y pendingDelta=null; una solo tocada por pendientes tiene acceptedDelta=0.0 pero pendingDelta!=null.

2026-05-06 - architect - DefaultConsolidatedViewBuilder NUNCA llama AuditReportStore.save.
  why: F-CDIFF-R015 prohibe que la salida emita un AuditReport oficial. AuditReportStore es inyectado solo para .load(); el contrato del puerto en el dominio sigue siendo el mismo, pero el builder NO usa save. La regla es testeable directamente en DefaultConsolidatedViewBuilderTest verificando que mockedAuditReportStore.save() nunca se invoca en cualquier path.

2026-05-06 - architect - GetConsolidatedCommand y SetActiveAnalysisCommand son verbs sealed dedicados.
  why: el shape de la respuesta (ConsolidatedView con NodeImpacts/StatisticImpacts/NonApplicablePendings) es estructuralmente distinto del retorno simple de GetCommand. Meterlo dentro de GetCommand acoplaria dos contratos en una signatura. Un verb sealed dedicado por command sigue el patron de los demas (AnalyzeCommand, PlanCommand, etc.).

2026-05-06 - architect - ConsolidatedViewFormatter / DefaultConsolidatedViewFormatter visibility: public a nivel Java.
  why: viven en audit-cli.formatting (package internal a nivel modulo) pero los consumidores reales son DefaultGetConsolidatedCommand (package hermano commands) y la composition root (Main). Sin visibility: public a nivel Java son package-private y no se importan desde commands. Patron drift-corregido en ARCH-PIPRE-VIS para ImpactPreviewFormatter / DefaultImpactPreviewFormatter; aplica igual aqui.

2026-05-06 - architect - Refinamiento texto: revisionArtifactStore.description en DefaultConsolidatedViewBuilder hace explicito que REJECTED se descarta (F-CDIFF-R005).
  why: Nota minor de analyst en review. El particionado por verdict ya estaba implicito en el algoritmo, pero el contrato del puerto debia decirlo en el description para que qa-tester pueda taggear directamente el comportamiento de R005 sobre la inyeccion. Cambio no-arquitectural; no toca fences del TECH_SPEC.
