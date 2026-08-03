# Decisions — FEAT-EVCOST

2026-07-29 — analyst — Feature propia en vez de escribir la capacidad dentro del analizador de consigna.
  why: pedido del usuario, y el patron (resultado costoso, reutilizable, reanudable, compartido entre analisis) reaparece con cualquier evaluador basado en juicio. Dentro del primer analizador quedaria invisible para el segundo, que lo reinventaria con criterios distintos (uno identificando por id de elemento y otro por huella, uno guardando las fallas y otro no).

2026-07-29 — analyst — Identidad = evaluador + version + huella del contenido; el identificador del elemento NO participa (R001).
  why: corregir un elemento no cambia su identificador (FEAT-RPRES). Beneficio no buscado: dos elementos con contenido identico comparten resultado y se pagan una vez.

2026-07-29 — analyst — La huella se calcula sobre exactamente lo que el evaluador recibe: ni menos ni mas (R002).
  why: un dato juzgado fuera de la huella produce reuso silencioso de un resultado obsoleto (el peor error, nadie lo detecta); un dato ajeno dentro de la huella invalida resultados vigentes y devuelve el costo que la feature evita.

2026-07-29 — analyst — "Completo o inexistente" es parte de la regla de escritura incremental (R004), no un detalle.
  why: un registro truncado por la interrupcion es peor que ninguno — se reutilizaria como valido. Debe comportarse como si nunca se hubiera intentado registrarlo.

2026-07-29 — analyst — Criterio para separar resultado definitivo de falla transitoria: si el evaluador se pronuncio sobre el contenido (R005). Un resultado de cortesia ante su propia falla NO es pronunciamiento.
  why: cachear una falla congela un problema de infraestructura como si fuera juicio; tratar un resultado desfavorable como falla hace repagar justo el contenido que ya se sabe que esta mal. Cada consumidor declara como reconoce el caso en su evaluador (F-QINST-R013).

2026-07-29 — analyst — Cambiar la version del evaluador invalida vigencia pero no borra ni dispara re-evaluacion (R006 + R008).
  why: decision del usuario. Ademas la historia por version es la unica forma de saber si un cambio del evaluador lo mejoro.

2026-07-29 — analyst — DOUBT-CONCURRENCIA RESUELTO (Opcion B: simultaneidad segura) tras el diseño de @architect. La garantia quedo explicita en el enunciado de F-EVCOST-R004; no se agrego ninguna regla.
  why: la forma del registro ya la da — un registro por evaluador, una entrada por linea, append sin reescribir lo anterior, y lectura que descarta la linea que no parsea. De ahi salen las tres propiedades: nadie pisa lo ajeno, ninguna linea a medio escribir se toma como valida, una interrupcion solo daña la ultima. Costo aceptado: dos corridas simultaneas pueden pagar dos veces el mismo contenido; nunca corromper ni perder resultados. La Opcion A que estaba como respuesta provisoria contradecia a R004 y se retiro del texto.

2026-07-29 — analyst — Opcion C (coordinar entre corridas para no duplicar) descartada por ahora; reabrir si el paralelismo se vuelve habitual y el gasto duplicado se mide.
  why: complejidad permanente (una corrida esperando a otra) a cambio de un ahorro que solo aparece si alguien corre dos auditorias a la vez del mismo evaluador.

2026-07-29 — analyst — Ajustado tambien el bullet de "Fuera de alcance" que declaraba fuera de alcance la ejecucion simultanea: ahora lo que queda fuera es la *coordinacion* para no duplicar.
  why: tras resolver la duda como Opcion B, ese bullet contradecia a R004. Es el unico cambio fuera del bloque de la duda.

2026-07-29 — analyst — El presupuesto por corrida NO se especifico aca; quedo en el consumidor (F-QINST-R006) con DOUBT-PRESUPUESTO-GENERAL abierto.
  why: el usuario enumero explicitamente las reglas de esta capacidad y el presupuesto no estaba entre ellas. Con un solo consumidor no hay evidencia para fijar la forma general; reabrir la duda cuando aparezca el segundo evaluador costoso.

2026-07-30 — architect — La huella la deriva la sesion a partir del mapa de contenido (EvaluationSubject.content usado dos veces: se canonicaliza para la huella y se entrega tal cual al evaluador), no la calcula el consumidor.
  why: hace estructural a F-EVCOST-R002 en vez de dejarla a disciplina de cada consumidor.

2026-07-30 — architect — EvaluationOutcome con dos tipos (EvaluationEmitted / EvaluationNotEmitted), no un booleano: solo el emitido lleva payload.
  why: "registrar una falla transitoria como si fuera resultado" (lo que F-EVCOST-R005 prohibe) pasa a ser codigo que no compila.

2026-07-30 — architect — EvaluationFailureKind separa falla de UN contenido (OUTPUT_UNUSABLE, el caso F-QINST-R013) de falla del SERVICIO (EVALUATOR_UNAVAILABLE).
  why: con la primera la corrida sigue; con la segunda la sesion corta consultas y devuelve PENDING. Sin la distincion, o se aborta la auditoria o se golpea 11.500 veces un servicio caido.

2026-07-30 — architect — agent-runtime-infrastructure extraido como runtime compartido de grafos declarativos; los dos lanzadores legados (lemmaabsenceagent, knowledgetitleagent) NO migrados en este patch.
  why: FEAT-RPRES tiene el arbol a medio compilar y migrar toca dos features verdes y sus handwrittenTests. Patch de migracion pendiente.

2026-07-30 — architect — DOUBT-PRESUPUESTO-GENERAL implementada como Opcion A con matiz: el MECANISMO del tope vive en la capacidad (EvaluationRunPolicy.maxNewEvaluations) y el VALOR y su defecto en el consumidor (QuizInstructionConfig, 500).

2026-07-30 — architect — DOUBT-CONCURRENCIA: el formato append-line deja dos corridas simultaneas en la Opcion B de hecho (a lo sumo se paga dos veces, sin corromper), no en la Opcion A declarada. Mejor que lo especificado; conviene que el requerimiento lo diga.

2026-07-30 — architect — ARCH-002: consolidacion del runtime de grafos. Se borran las IMPLEMENTACIONES duplicadas de los dos lanzadores y se conservan sus interfaces locales.
  why: borrar tambien las interfaces cambiaba el requiresInject de los dos generators y con el la firma que 9 handwrittenTests verdes de FEAT-LASAG/FEAT-KTLR mockean. Los lanzadores no tienen tests propios, asi que borrarlos cuesta cero.

2026-07-30 — architect — Los dos patches declaran agent-runtime-infrastructure identico (3 allowedClients) a proposito.
  why: `_change: add` sobre un elemento existente es no-op silencioso; sin la duplicacion deliberada, aplicar la migracion segunda dejaba revision-infrastructure sin autorizar por la regla ArchUnit. Con ambos identicos, el segundo en aplicarse es no-op y el resultado no depende del orden.

2026-07-30 — architect — AgentGraphRunner resuelve rutas en cadena: config -> inputs["project_root"] -> CWD.
  why: los dos lanzadores NO eran identicos —lemma-absence resuelve relativo al CWD, knowledge-title relativo a project_root—. Extraer solo el denominador comun regresionaba a knowledge-title; la cadena es la union de ambos comportamientos.

2026-07-30 — architect — Los clasificadores de error de los dos agentes quedan duplicados a proposito (deuda consciente, anotada en el TECH_SPEC).
  why: devuelven LlmGenerationFailureCategory, tipo publico del paquete lagen que audit-cli ya consume. Compartirlos exige taxonomia neutra + mapeo por consumidor: tres piezas nuevas para deduplicar una clasificacion por strings.

2026-07-30 — architect — El patch de migracion quedo anclado en esta carpeta por falta de una propia, con `code: FEAT-EVCOST` auto-seteado, aunque NO implementa ninguna regla de FEAT-EVCOST. Trazabilidad engañosa conocida y documentada en la primera seccion del TECH_SPEC. Pendiente de decision del usuario.

2026-07-30 — qa-tester — Placement de journeys por visibilidad, no por "modulo de la feature": J001 en evaluation-ledger-domain (las 4 salidas de "¿ya tengo esto?" las decide la sesion; la factory y el fingerprinter son publicos), J002 en evaluation-ledger-infrastructure (la mitad "se lee completa o no existe" solo se observa sobre el archivo append-line real), J003 en audit-cli (unico modulo que ve a la vez audit-infrastructure y evaluation-ledger-infrastructure, necesario para "eliminar informes").

2026-07-30 — qa-tester — R002 y R004 se testean en las DOS mitades a proposito: R002 = (huella distinta ante cualquier cambio) + (misma huella al reconstruir en otra corrida) sobre Sha256ContentFingerprinter, mas un tercer test sobre la sesion que ancla la huella al mismo mapa que recibe el evaluador; R004 = disponibilidad inmediata + entrada truncada tratada como inexistente + dos corridas simultaneas sin perdida.
  why: un test de una sola mitad deja pasar justo el error silencioso que la regla existe para evitar.

2026-07-30 — qa-tester — Sin regla que las respalde y por lo tanto SIN handwrittenTest: (a) el corte de consultas de la sesion ante EVALUATOR_UNAVAILABLE, (b) los contadores de EvaluationCoverage, (c) el cableado de DefaultEvaluationSessionFactory. Escaladas al usuario en vez de forzar tag a la regla mas cercana.

2026-07-30 — analyst — AGREGADA F-EVCOST-R009: ante una falla, la corrida distingue si es atribuible a ese contenido puntual (sigue consultando por el resto) o al servicio (deja de consultar; el resto queda pendiente). En ninguno de los dos casos se aborta el analisis. Cierra el hueco (a) que escalo @qa-tester.
  why: R005 exige que la falla no se registre y el contenido vuelva a estar pendiente, pero NO dice nada sobre seguir o cortar — una implementacion que insiste con los 11.500 contenidos contra un servicio caido cumple R005 sin objecion. Sin corte: tarda lo mismo que una corrida completa y no produce ni un resultado.

2026-07-30 — analyst — El criterio de la distincion es el ORIGEN de la falla, no su gravedad aparente; y "cortar consultas" != "abortar el analisis" (la corrida termina con la cobertura que alcanzo).
  why: clasificar por como se ve el sintoma rompe las dos mitades: una salida inutilizable se ve alarmante y solo afecta a ese contenido, un tiempo de espera agotado se ve trivial e indica servicio caido. La mitad "no aborta" ya la exige F-QINST-R007 del lado del consumidor; aca queda como garantia general para que el segundo consumidor la herede.

2026-07-30 — analyst — La mitad "no cortar ante falla de un contenido" se especifico explicitamente, no como default implicito.
  why: sin ella, la lectura natural de "cortar ante falla" se generaliza a toda falla, y un unico contenido con el que el evaluador tropieza dejaria pendiente todo lo que sigue, corrida tras corrida: gasto conservado pero avance nulo.

2026-07-30 — analyst — J002 MODIFICADA (no se creo journey nueva): el outcome falla_no_registrada se acoto a la falla del contenido puntual y se agrego un cuarto outcome/terminal falla_del_servicio. Requiere `sentinel feature sync`.
  why: el `when` viejo decia literalmente "servicio caido, tiempo agotado, credito agotado" y lo mandaba a una rama cuya unica asercion era "no se registra" — o sea, la journey afirmaba algo incompleto justo para el caso que R009 gobierna. Se aprovecho que el patch de @qa-tester sigue SIN aplicar y las clases de journey aun no se generaron: es el momento mas barato para tocarla. El outcome nuevo va ultimo para no renumerar los paths 1-3 existentes.

2026-07-30 — analyst — Alerta de placement para @qa-tester: J002 estaba asignada a evaluation-ledger-infrastructure porque "se lee completa o no existe" solo se observa sobre el archivo append-line real. La rama falla_del_servicio se observa en la sesion, no en el store.
  why: puede obligar a re-decidir el testModule de la journey o a mockear el evaluador desde infrastructure. Decision de @qa-tester, no del analista.

2026-07-30 — qa-tester — R009 se testea en sus dos mitades sobre DefaultEvaluationSession, con series de N contenidos identicas y la MISMA falla en el contenido i, discriminando solo por EvaluationFailureKind: OUTPUT_UNUSABLE -> la corrida sigue consultando i+1..N; EVALUATOR_UNAVAILABLE -> no consulta mas, i..N pendientes, y la corrida TERMINA (no lanza). El criterio (c) del requerimiento (no se registra nada) NO se tagea a R009: el propio enunciado lo delega a R005, que ya tiene su test.
  why: un test de una sola mitad deja pasar "corta siempre" (un ejercicio problematico deja pendiente el curso entero, corrida tras corrida) o "no corta nunca" (11.500 llamadas a un servicio caido).

2026-07-30 — qa-tester — J002 SE QUEDA en evaluation-ledger-infrastructure pese al 4to path (falla_del_servicio), que es de sesion y no de store.
  why: la restriccion es unidireccional. El path conserva_lo_hecho exige el archivo append-line real, y evaluation-ledger-domain NO ve a FileSystemEvaluationLedger (esta aguas abajo). Al reves si: desde infrastructure la sesion es alcanzable entera por el seam publico DefaultEvaluationSessionFactory.open() -> puerto EvaluationSession, sin necesidad de nombrar la clase package-private. Ademas los paths 2, 3 y 4 afirman todos algo sobre "la corrida siguiente", que con el ledger real es literal (instancia nueva sobre el mismo dir) en vez de un fake.

2026-07-31 — analyst — Pendiente (c) de @architect RESUELTO: F-EVCOST-R008 explicita que el alcance de la re-evaluacion lo acota el CONSUMIDOR, contenido por contenido. La capacidad general vuelve a evaluar el contenido puntual que se le pide y ningun otro; el contenido vecino con resultado vigente se sigue reutilizando en esa misma corrida. Agregado al enunciado + parrafo "Quien acota ese alcance" en el Detalle + criterio (b) ampliado + item nuevo en Fuera de alcance.
  why: los recortes del consumidor estan en su vocabulario (p.ej. "todos los ejercicios de un knowledge") y de un contenido esta capacidad solo conoce una referencia opaca al elemento del curso: no puede agrupar por algo que no interpreta. No cambia ninguna decision (todo/parte ya estaban admitidos), solo dice donde vive el recorte. Reparto: el consumidor elige cuales, la capacidad garantiza que re-evaluar uno no arrastre a los demas.

2026-07-31 — analyst — Sin ids nuevos y sin tocar journeys (`feature status` FEAT-EVCOST = OK, no hace falta `sentinel feature sync`). AVISO a @qa-tester: el criterio (b) de R008 ahora afirma algo mas fuerte ("el resto con resultado vigente se reutiliza en la misma corrida"), que el test actual de R008 no verifica; conviene un handwrittenTest para esa mitad.

2026-08-02 — analyst — CAMBIO DE FONDO: la VERSION del evaluador SALE de la identidad de un resultado. Identidad = evaluador + huella del contenido. La version pasa a ser PROCEDENCIA: se registra junto al resultado, viaja con el al consultarlo, es consultable, pero NO decide vigencia. Reglas tocadas: R001 (identidad + procedencia obligatoria), R003 (la consulta ignora la version y devuelve la procedencia), R006 (todo se reutiliza sea de la version que sea; gana el registrado mas reciente), R008 (la re-evaluacion explicita es el UNICO mecanismo por el que un resultado se rehace). Duda nueva DOUBT-IDENTIDAD-VERSION dejada RESOLVED con las 4 opciones.
  why: ARGUMENTO DEL USUARIO, textual: "me interesa lo que se testea, no quien lo hace". Si un contenido cumple o no lo que se le pregunta es propiedad DEL CONTENIDO, no de quien lo juzgo; cambiar de modelo (costo, disponibilidad, migracion de proveedor) no puede invalidar 11.487 veredictos ya pagados. CONTRA QUE SE LE PLANTEO Y DESCARTO A CONCIENCIA: la definicion del evaluador ES el criterio de juicio, asi que al arreglar un prompt que juzgaba mal los veredictos viejos quedan vigentes siendo ya sabidamente incorrectos. Se le ofrecieron dos alternativas intermedias (declarar compatibilidad entre versiones; reutilizar pero marcar "de otra version" y que el consumidor decida) y eligio la mas simple. NO REVERTIR sin leer DOUBT-IDENTIDAD-VERSION en el REQUIREMENT.md.

2026-08-02 — analyst — El evaluatorId SIGUE en la identidad y no contradice el criterio anterior.
  why: no identifica QUIEN juzga sino QUE PREGUNTA SE HIZO. El veredicto de un futuro evaluador de calidad de traduccion sobre un contenido no es intercambiable con el del juez de consigna sobre el mismo contenido: son dos preguntas distintas.

2026-08-02 — analyst — R006 fija ademas cual aplica cuando hay MAS DE UN resultado para el mismo evaluador+huella: el registrado mas recientemente; los anteriores quedan como historia consultable con su procedencia.
  why: antes la pregunta casi no se planteaba (la version desempataba). Ahora una re-evaluacion explicita produce dos resultados bajo la MISMA clave, y sin esta regla el reuso quedaria indefinido: si ganara el viejo, pedir la re-evaluacion no tendria efecto observable y el usuario habria pagado por nada.

2026-08-02 — analyst — R001 exige la procedencia como dato OBLIGATORIO (error propio: "se registro un resultado sin la version que lo produjo").
  why: tras sacar la version de la identidad, la procedencia es la unica señal que le queda al usuario para decidir cuando re-evaluar. Sin ella la Opcion A elegida se vuelve indefendible: nadie sabria que resultados vienen del evaluador que acaba de corregir.

2026-08-02 — analyst — J001 MODIFICADA (mismos 4 outcomes, mismos node ids, mismo path count): el outcome/terminal `version_anterior` INVIERTE su asercion (antes: no se reutiliza y queda pendiente; ahora: se reutiliza igual y la consulta informa la procedencia). Tambien cambio el texto del `when` del outcome 1 y del terminal `reevalua_a_pedido` de J003.
  why: el path-3 de FEvcostJ001JourneyTest afirmaba exactamente lo contrario de la decision nueva. Se conservaron los node ids y el orden de los outcomes para no renumerar paths.

2026-08-02 — architect — Contratos de FEAT-EVCOST adaptados (el patch vive en la carpeta de FEAT-QINST, requirements/2026-07-29.02_.../architectural_patch.yaml): EvaluationKey = (evaluatorId, contentFingerprint); version -> EvaluationRecord.evaluatorVersion (obligatoria) y EvaluationResolution.evaluatorVersion (solo REUSED/EVALUATED); EvaluationLedger.find -> findLatest y history(String,String) -> history(EvaluationKey); Evaluator.evaluatorVersion(): Optional<String>; EvaluationSession.evaluatorVersion() -> consultedEvaluatorVersion(): Optional<String>; descripciones de EvaluationCoverage.reused/pending desvinculadas de la palabra "vigente".
  why: detalle completo y razones en requirements/2026-07-29.02_validacion-de-consigna-de-quiz/memory/decisions.md (misma fecha) y en su TECH_SPEC.md.

2026-08-02 — architect — Un Evaluator que no puede enunciar su version (Optional.empty()) es tratado por DefaultEvaluationSession como evaluador NO DISPONIBLE desde el primer sujeto, reutilizando la mitad EVALUATOR_UNAVAILABLE de F-EVCOST-R009: no consulta, no registra, deja pendiente lo que falta — y el reuso sigue intacto porque la ruta de reuso ya no mira la version.
  why: cierra la invariante 3 de F-QINST-R010 sin agregar ningun mecanismo ni regla nueva a la capacidad general, y sin que evaluation-ledger-domain sepa nada del juez de consigna.
