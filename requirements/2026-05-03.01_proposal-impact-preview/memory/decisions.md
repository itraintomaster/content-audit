# Decisions

2026-05-03 — analyst — DOUBT-BASE-COURSE-VERSION resuelto a Opcion A (eager al momento de generacion).
  why: Asociacion 1:1 entre proposalId y preview, evita recomputo en cada lectura, alineado con el caso de uso primario (decidir una propuesta a la vez). La obsolescencia ante cambios posteriores en el curso queda como feature.future en DOUBT-STALENESS-DETECTION.

2026-05-03 — analyst — DOUBT-PROPOSAL-LIFECYCLE resuelto a Opcion A (cada propuesta su propio preview).
  why: Coherente con la inmutabilidad de [F-PIPRE-R008] y con el ciclo de FEAT-REVAPR R010 (re-revision genera proposalId nuevo).

2026-05-03 — analyst — Preview tratado como dato adicional, no bloqueante: si falla, la propuesta se persiste igual ([F-PIPRE-R010]).
  why: Distinto al caso de FEAT-LAPS R015 (estrategia falla -> sin propuesta). Aqui la propuesta ya existe; el preview es informativo.

2026-05-03 — analyst — DOUBT-BATCH-PREVIEW marcado feature.future, no fuera de alcance silencioso.
  why: El usuario lo anticipo explicitamente; queda como feature.future con justificacion para cerrar la conversacion en esta iteracion.

2026-05-03 — analyst — DOUBT-PRESENTATION-FORMAT resuelto a Opcion A (notacion porcentual).
  why: Los scores del dominio van de 0 a 1, asi que la presentacion porcentual al operador es la mas human-readable. Los valores subyacentes en 0..1 quedan como decision de modelado del architect; lo que esta normado funcionalmente es la presentacion al operador.

2026-05-03 — analyst — Agregada regla F-PIPRE-R013 (los deltas se presentan en notacion porcentual) y atada al gate del journey J002.mostrar_preview_completo.
  why: Mantener "una regla = un test directo": la decision del DOUBT necesitaba una regla testeable independiente, no podia colarse como nota dentro de F-PIPRE-R006 (esa regla estructura los tres valores; R013 norma su presentacion). El detail de R006 fue ajustado para apuntar a R013 en lugar de al doubt resuelto.

2026-05-03 — architect — DOUBT-UNAVAILABILITY-TAXONOMY resuelto a Opcion C (categoria enum + detalle texto).
  why: Sigue exactamente el patron de F-LAGEN-R006 / LlmGenerationFailureCategory. Enum cerrado ImpactPreviewUnavailabilityReason con TARGET_NODE_ABSENT / BASE_AUDIT_UNAVAILABLE / SIMULATION_FAILED / OTHER (uno por causa de la tabla R009 mas escape). El detalle es String libre para el operador. Ventajas: filtrable / agrupable (Opcion A no lo permitia), no requiere mantener enum gigante (Opcion B), y reutiliza un patron ya validado en el codebase.

2026-05-03 — architect — DOUBT-PREVIEW-CLI-EXPOSURE resuelto a Opcion A (sin recurso CLI propio).
  why: R007 ya pide visibilidad junto a la propuesta y el alcance del REQUIREMENT lo confirma ("Operaciones nuevas de CLI especificas para preview... fuera de alcance"). GetCmd carga el preview en la misma operacion que el RevisionArtifact via ImpactPreviewStore.findByProposalId, sin verbo / recurso nuevo. Si el batch lazy de DOUBT-BATCH-PREVIEW se materializa, alli si conviene un recurso propio para listar y filtrar — ahora seria expansion de superficie sin caso de uso.

2026-05-03 — architect — Persistencia del preview: store sidecar (ImpactPreviewStore), no campo en RevisionArtifact.
  why: El RevisionArtifact ya tiene un contrato consolidado por FEAT-REVBYP / FEAT-REVAPR; embeberle un campo impactPreview rompe los tests existentes del store y mezcla el "veredicto de la propuesta" con "informacion derivada". El sidecar (FileSystemImpactPreviewStore en audit-infrastructure) escribe un archivo hermano <proposalId>.preview.json en la misma raiz .content-audit/revisions/<planId>/, asi un consumidor que ignore el preview sigue leyendo el artifact viejo igual. Bonus: deja el camino abierto al batch lazy futuro sin tocar el artifact store.

2026-05-03 — architect — ImpactPreviewComputer nunca lanza; las tres causas R009 se materializan como availability:UNAVAILABLE.
  why: Hace que R010 (preview no aborta persistencia de la propuesta) sea propiedad del puerto, no del cliente. DefaultRevisionEngine no necesita try/catch defensivo: invoca compute(), persiste lo que recibe (AVAILABLE o UNAVAILABLE), y sigue. Beneficio secundario: tests del computer no necesitan testear "el caller maneja la excepcion" — solo verifican que la categoria y el detalle salgan correctamente en cada modo de falla.

2026-05-03 — architect — Ubicacion del puerto ImpactPreviewStore: module root de revision-domain (no en package impactpreview).
  why: Consistencia con RevisionArtifactStore que tambien vive en module root. El package impactpreview agrupa carriers + el puerto de calculo (ImpactPreviewComputer); la persistencia es Public Port + Hidden Adapter clasico, paralelo a la persistencia de los artifacts. Ademas hay limitacion practica del validador: una interface declarada dentro de un package nuevo no se resolvia desde el implements de un adapter en otro modulo en el mismo patch.

2026-05-03 — architect — Modelo de diff con queries puntuales pero sin helpers explicitos en sentinel.yaml.
  why: ImpactPreview / LevelImpact / DimensionDelta son records inmutables; las queries puntuales (getLevelImpact(level, nodeId), getDimensionDelta(...)) se pueden agregar como metodos de instancia en el record una vez que el dev las necesite — no hace falta declararlas en sentinel.yaml porque no son contrato cruzado, son utilidades internas del consumidor. Si test-writer / qa-tester las pide explicitas, las declaramos despues.

2026-05-03 — architect — RevisionEngineConfig recibe courseMapper / auditEngine / impactPreviewStore como required (no default).
  why: El factory no puede fabricar courseMapper ni auditEngine — su wiring depende de NlpTokenizer, QuizSentenceConverter y ContentAudit, que viven en otros modulos. El composition root (audit-cli) ya construye esas instancias para DefaultAuditRunner; las reusa al armar el RevisionEngineConfig. impactPreviewStore tambien es required: la raiz de filesystem es decision del composition root.

2026-05-03 — architect — Boundary violation CourseMapper resuelto: puerto movido de audit-application a audit-domain; audit-domain pasa a depender de course-domain.
  why: revision-domain (DefaultImpactPreviewComputer + RevisionEngineConfig) necesita el puerto pero no puede importar de audit-application sin generar ciclo (audit-application ya depende de revision-domain). Mover el puerto a audit-domain es la opcion menos invasiva: revision-domain ya ve audit-domain, audit-application sigue viendolo por herencia, y conceptualmente el puerto pertenece al bounded context que produce AuditableCourse. La impl CourseToAuditableMapper se queda en audit-application porque su wiring (NlpTokenizer + QuizSentenceConverter) solo lo conoce ese modulo. Patron resultante: Public Port (audit-domain) + Hidden Adapter (audit-application). Patch ARCH-PIPRE-002.

2026-05-03 — architect — Detectada regresion preexistente NO relacionada con FEAT-PIPRE: DefaultAuditRunner en sentinel.yaml declara requiresInject de un tipo "ContentAudit" que no existe (es el system name) y de un "courseMapper: CourseMapper" que no esta en el .java. La baseline sentinel-baseline.yaml ya tenia esa drift al momento de proponer FEAT-PIPRE — el dev hizo bien en no reintroducir esos campos al revertir el regen. Reportar como bug separado de Sentinel; queda fuera del alcance del patch ARCH-PIPRE-002.

2026-05-03 — architect — ImpactPreviewFormatter / DefaultImpactPreviewFormatter pasan a visibility: public (patch ARCH-PIPRE-VIS).
  why: Drift detectado en implementacion. Ambos viven en audit-cli.formatting (package internal) pero los consumidores reales — Main (composition root) y GetCmd — viven en audit-cli.commands, package hermano dentro del mismo modulo. Con visibility: internal a nivel Java son package-private y no se pueden importar desde commands. El developer ya habia parchado los .java a public para destrabar la build; esto alinea el DSL para que el proximo sentinel generate no lo revierta. El package formatting se mantiene internal: la apertura es solo Java-class-level (intra-modulo), no cross-module.
