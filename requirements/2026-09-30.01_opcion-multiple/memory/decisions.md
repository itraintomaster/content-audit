# Decisions — FEAT-OPMUL

2026-09-30 — analyst — Feature nueva (FEAT-OPMUL) en vez de repartir las reglas entre FEAT-COURSE, FEAT-RPRES y FEAT-QINST.
  why: "medir si, corregir no" es una sola decision que cruza carga, medicion, juez y correccion; repartida, ninguna feature la enunciaria entera.

2026-09-30 — analyst — Requirement retroactivo: enuncia el comportamiento de 83228e54 tal como quedo; lo discutible va a Doubts con recomendacion, nunca a reglas.
  why: pedido explicito; las reglas tienen que poder trazar los tests MC ya escritos sin inventar conducta.

2026-09-30 — analyst — R001 acotada a "un curso escrito en el formato en que el sistema lo guarda".
  why: el byte a byte vale para el curso vivo porque el importador escribe en ese formato; un JSON con otra indentacion u orden no vuelve identico (F-COURSE-R003 admite reordenar).

2026-09-30 — analyst — "La tarea queda SKIPPED" se precisa por verbo en R006: SKIPPED en revise y revise-instructions; approve deja la propuesta sin decidir y assess-candidate no escribe nada.
  why: confirmado en el codigo; approve y assess-candidate no cambian el estado de la tarea.

2026-09-30 — analyst — El juez va en una sola regla (R005: MC con opciones + CLOZE sin cambios) y la linea base como criterio de R003.
  why: tope de ~300 lineas; "que recibe el juez" es un solo concepto, y la linea base es la medicion que prueba R003, no un invariante aparte.

2026-09-30 — analyst — Quinta doubt, no pedida: DOUBT-MC-HERMANOS.
  why: el brief 021 elegia una forma CLOZE para el MC para mantenerlo en el control anti-duplicado; lo implementado (sin forma) lo saca de la comparacion exacta con los hermanos y el brief 022 no lo dice.

2026-09-30 — analyst — Journeys en YAML de flujo compacto (outcomes y nodos terminales en una linea).
  why: tope de lineas; verificado que el validador los parsea: una copia con un `then` roto da FAIL (nodo inexistente y nodo inalcanzable).

2026-09-30 — analyst — Los briefs dicen "10 MC en modo REWRITE"; en la base hay 60 MC en knowledges REWRITE y en 10 el modo habria recortado algo. R004 usa las dos cifras.

2026-09-30 — architect — Parche retroactivo: declara solo lo que el codigo ya tiene y el DSL puede expresar. FormKind, MultipleChoiceEntity y MultipleChoiceItemEntity; campos nuevos al final de FormEntity, QuizTemplateEntity, AuditableQuiz, QuizInstructionSubjectView y QuizInstructionCorrectionRunReport; MULTIPLE_CHOICE_UNSUPPORTED en 3 enums; FEAT-OPMUL (id + code). Ninguna interfaz ni implementacion.
  why: verify, analyze y una simulacion de generate no encontraron otra deriva: los 19 @Generated restantes (18 de 83228e54 + DefaultCandidateAssessor de bf9ecca8) son implementaciones y generate les preserva el cuerpo.

2026-09-30 — architect — Fuera del parche, porque el DSL no lo expresa: constructores de copia y formKind() de FormEntity/QuizTemplateEntity, FormKind.from, MultipleChoiceEntity.correctItem() y los comentarios. Tampoco la aridad vieja de los constructores completos (FormEntity 5, QuizTemplateEntity 21, AuditableQuiz 9, QuizInstructionSubjectView 6).
  why: un modelo del DSL es solo campos; ModelGenerator reescribe el archivo entero y emite siempre el constructor con todos. Cerrar la brecha (codigo, Sentinel o ambos) lo decide José.

2026-09-30 — architect — Los tres tipos nuevos se declaran aunque sus archivos siguen escritos a mano, sin @Generated.
  why: los campos nuevos los referencian; generate y verify saltean un archivo existente sin @Generated, asi que from() y correctItem() sobreviven.

2026-09-30 — architect — Enums modificados con `type: enum` explicito en el parche.
  why: sin type, DslWriter serializa el default `record` (paso en un parche de QICOR); PatchApplier conserva el tipo real, pero el parche quedaria enganoso.

2026-09-30 — architect — FEAT-OPMUL se registra con `features: [{id, code}]`, sin ubicar journeys.
  why: ubicar J001-J003 es del qa-tester; `patch apply` copia la compuerta con los 3 sin testModule y generate aborta hasta que se ubiquen.

2026-09-30 — architect — DOUBT-MC-HERMANOS intacta: el MC se declara sin forma CLOZE, como esta en el codigo.
  why: es decision de José.

2026-09-30 — architect — Glosario: dos sugerencias (Ejercicio de opcion multiple sobre MultipleChoiceEntity, Dato no interpretado sobre FormEntity) y ningun domainLink.
  why: ningun termino existente cubre los dos conceptos que usan todas las reglas; promoverlos es del analista.

2026-09-30 — qa-tester — Declarar un test MC existente = portar su cuerpo al stub que generate crea en {Impl}Test y borrar el original de la clase *MultipleChoiceTest.
  why: generate ignora className/sourceFile (el stub va siempre a {Impl}Test) y el reporte busca {Impl}Test.{nombre normalizado}; declararlos "en su lugar" deja stubs rojos duplicados.

2026-09-30 — qa-tester — 32 handwrittenTests: 27 portan tests de 83228e54 y 5 son nuevos (R002 orden al guardar, R003 kind desconocido = CLOZE, R004 la auditoria lee la oracion guardada, R006 propuesta cuyo quiz ya es MC, R008 approve que perderia datos de hoy). 12 tests MC quedan sin declarar.
  why: OG1; solo se traza lo que verifica la regla. Los 12 son soporte de carga, internos de modelo o guardas de FEAT-QSENT, y las clausulas sin test van como test nuevo.

2026-09-30 — qa-tester — Journeys: J001 en audit-application (com.learney.contentaudit.auditapplication), J002 en revision-domain (...revisiondomain.engine), J003 en audit-cli (com.learney.contentaudit.journeys).
  why: modulo con visibilidad a todos los participantes y paquete exacto; mismo criterio que F-QINST-J003/J004, F-REVBYP-J001 y F-RPRES-J001..J003.

2026-09-30 — qa-tester — Los 2 tests de CourseElementFieldDiff (helper estatico no declarado) se declaran en DefaultPreservationCheck y se portan por verify().
  why: handwrittenTests solo cuelgan de implementaciones declaradas.
