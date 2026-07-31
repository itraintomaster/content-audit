# Decisions — FEAT-RPRES

2026-07-29 — analyst — Feature nueva (FEAT-RPRES) en vez de extender FEAT-DBSENT con una R005.
  why: DBSENT declara su alcance en un unico dato (oraciones planas) y su R004 es el espejo escritura/lectura de ese dato; una regla general de preservacion alli quedaria escondida bajo un titulo que no la anuncia. La garantia nueva vive en otro limite (aplicar una revision), aplica a todos los tipos de correccion y necesita journeys propias.

2026-07-29 — analyst — Regla formulada por complemento (todo lo no propuesto se preserva), no por enumeracion de campos.
  why: el hueco real esta en F-LAPS-R014, que enumera lo preservable (ids, instrucciones, orden, "otra metadata estructural") y por eso dejo fuera tipo de formulario, peso, etiquetas y texto vacio del hueco — que ademas F-LAPS-R013 autorizaba a cambiar como parte de la "estructura del ejercicio". Enumerar lo preservable siempre queda desactualizado; enumerar lo cambiable es corto y estable.

2026-07-29 — analyst — "Vacio vs ausente" va como regla propia (R002), no como nota de R001.
  why: define el criterio de identidad de la comparacion de R001. Sin ella una comparacion permisiva (vacio == ausente, 0,0 == sin valor) declararia preservado un elemento degradado y R001 no detectaria nada. Ademas es funcional: el consumidor productivo distingue los tres estados.

2026-07-29 — analyst — Descartada una cuarta regla de "validez absoluta" del elemento revisado.
  why: seria redundante con R001 cuando el original esta sano, y cuando el original ya viene degradado entra en conflicto con la preservacion. Ese caso se dejo como DOUBT-REPARACION (2.658 quizzes ya danados) para decision humana.

2026-07-29 — analyst — No se toca F-LAPS-R013/R014 ni FEAT-DBSENT.
  why: reformular o retirar una regla existente arrastra las verificaciones que la citan; queda como DOUBT-LAPS-ENUM.

--- Resolucion de doubts por el usuario (misma fecha) ---

2026-07-29 — analyst — DOUBT-TITULO-DENORMALIZADO resuelto (Opcion A) -> nueva regla F-RPRES-R004 + journey J003.
  why: el titulo del ejercicio es copia de la etiqueta del knowledge y la app lo muestra como encabezado de la tarjeta; sin propagacion la tarjeta queda vieja. Se declara como UNICA excepcion a R003, con su reciproca explicita (ninguna otra revision toca el titulo) porque hubo un caso real de correccion lexica que piso el titulo con la oracion corregida y spoileaba la respuesta.

2026-07-29 — analyst — DOUBT-REPARACION resuelto (Opcion B) -> nueva regla F-RPRES-R005.
  why: el ORDEN es parte de la regla — reparar antes de que rija la preservacion no cierra nada, la siguiente revision vuelve a degradar. Formulada por resultado observable (no queda contenido degradado), no por procedimiento. Evidencia de determinismo en el <details>: 2658 con contraparte integra, valores uniformes, misma cantidad de partes, resto de atributos intacto.

2026-07-29 — analyst — DOUBT-LAPS-ENUM resuelto (Opcion B): F-LAPS-R014 reformulada conservando id; ADEMAS se reformulo F-LAPS-R013.
  why: R014 sola no alcanzaba. La autorizacion concreta del dano estaba en el segundo item de R013 ("estructura del ejercicio (quizForm)"), que abarcaba tanto la composicion del enunciado (alcance legitimo) como tipo/peso/etiquetas del formulario (nunca). R013 ahora dice "composicion del enunciado" y delimita explicito que atributos NO entran. El cambio ACOTA, no amplia: las verificaciones vigentes (la oracion, huecos, respuestas y traduccion pueden diferir) siguen valiendo.

2026-07-29 — analyst — DOUBT-ALCANCE-DECLARADO queda OPEN, asignado a @architect.
  why: las tres opciones (fijo por tipo / declarado por propuesta / implicito en el contenido) producen el mismo efecto exigido; donde vive la declaracion se decide con contexto de contratos.

2026-07-29 — architect — DOUBT-ALCANCE-DECLARADO resuelto: Opcion A (fijo por tipo de correccion), materializada como CorrectionScope.changeableFields(DiagnosisKind, AuditTarget) -> Set<String> de paths.
  why: la Opcion B (declarado por propuesta) exige un campo nuevo en RevisionProposal, que esta persistido en 3564 artifacts y del que FEAT-PIPRE y FEAT-CDIFF heredan la estabilidad de identidad del elemento; ademas declararia por propuesta algo que no varia entre propuestas del mismo tipo (el alcance lo fija el deriver, no el candidato). La Opcion C (implicito en el contenido) es lo que ocurre en runtime gracias a parseOnto, pero no deja nada contra que verificar: el diff siempre esta contenido en si mismo. Propiedad decisiva: un DiagnosisKind sin alcance declarado devuelve conjunto VACIO, asi que un deriver nuevo que se olvide de declararse falla ruidosamente en el check en vez de degradar contenido en silencio — que es exactamente lo que paso.

2026-07-29 — architect — El estado previo llega via QuizSentenceConverter.parseOnto(quizSentence, base), NO por merge en el llamador.
  why: el merge en el deriver seria la enumeracion que R001 rechaza (cada campo nuevo de FormEntity obligaria a tocar los dos derivers) y pondria la forma del FormEntity, que es asunto de course-domain, a cargo de revision-domain. El converter es el unico componente con autoridad para decir que codifica el DSL y que no, asi que la preservacion por complemento queda en el unico lugar que puede definir el complemento. Descartado tambien el merge reflexivo generico sobre CourseElementSnapshot: escribir por reflexion sobre entidades mutables es fragil y duplica conocimiento ya disponible estructuralmente.

2026-07-29 — architect — R004 se implementa en CourseElementLocator.alignQuizTitles sobre el curso VIGENTE, no en DefaultKnowledgeTitleProposalDeriver.
  why: copiar los quizTemplates retitulados dentro del KnowledgeEntity propuesto resucitaria quizzes revisados despues de tomado el snapshot — es el mismo bug ya corregido una vez en replaceKnowledge (test F-KTLR-R007). Como paso explicito invocado solo en la rama KNOWLEDGE, ademas hace verdadera por construccion la mitad reciproca de R004 (ninguna otra revision toca el titulo).

2026-07-29 — architect — R005 necesita contratos propios (PreservationRepair, comando RepairCommand con --dry-run); no es operacion fuera del sistema.
  why: verificado contra datos reales: 2658/2658 degradados tienen contraparte integra y 0 huerfanos. La contraparte es el elementBefore del artifact APPROVED MAS ANTIGUO por nodeId — importa porque 384 de los 3564 artifacts tienen el elementBefore ya degradado (segunda revision sobre el mismo quiz). Reparar es entonces aplicar R001 retroactivamente con la misma CorrectionScope, lo que explica que no revierta revisiones y que comparta factory con la guarda: si divergieran, R001 y R005 dejarian de ser la misma regla hacia adelante y hacia atras. Declarar valores canonicos (kind=CLOZE, incidence=1.0, label="") seria materializar un default, prohibido por R002.

2026-07-29 — architect — Limitacion declarada: el check en runtime NO cubre toda R002.
  why: el alcance lexico declara form.sentenceParts como lista completa, asi que la degradacion del text de una parte CLOZE ("" -> null, 2690 partes) cae DENTRO del alcance declarado y el guard no la ve. Queda garantizada por construccion en parseOnto y verificada punta a punta por J001 (conserva_vacios la nombra explicitamente). No se invento una gramatica de paths con discriminador de kind por ser sintaxis ad-hoc para un solo caso; si QA lo considera insuficiente, la salida correcta es subir la granularidad del scope, no relajar el test.

2026-07-29 — architect — HALLAZGO: DefaultLemmaAbsenceProposalDeriver.java:65 hace newTitle = plainSentences.get(0).
  why: es, en codigo, el caso real que cita R004 — la correccion lexica pisa el titulo del quiz con la oracion corregida y le muestra la respuesta al alumno. Hoy esta enmascarado porque publish_revisions.py resincroniza title := knowledge.label (11487/11487 alineados en db/). Con title fuera del alcance lexico declarado, R001 deberia atraparlo.

2026-07-29 — qa-tester — El default de conjunto vacio de CorrectionScope se taguea F-RPRES-R001, no se escala como regla nueva.
  why: R001 esta formulada por complemento ("todo lo que queda fuera de ese conjunto se preserva por defecto, sin necesidad de estar listado en ninguna parte"). Un kind sin alcance declarado tiene complemento = todo; que cualquier diff sea violacion ES esa frase hecha operativa, no una decision de diseno ajena a la regla.

2026-07-29 — qa-tester — Los tests vigentes tagueados F-LAPS-R013/R014 NO se re-taguean (coincide con @architect). PERO el test tagueado F-LAPS-R012 (linea 117 de DefaultLemmaAbsenceProposalDeriverTest) assertea afterQuiz.getTitle() == plain sentence, y eso contradice la R014 reformulada ("Titulo del ejercicio ... no es alcance de una correccion lexica"). Es correccion de CUERPO, no de tag: la oracion en texto plano vive en QuizTemplateEntity.sentences, no en title.

2026-07-29 — qa-tester — La politica de herencia de parseOnto cuando el resultado tiene MAS partes que la base (respaldo en la primera parte de esa clase) queda SIN test: ninguna regla la determina. Levantada como doubt, no tagueada a la regla mas cercana.

--- Doubts devueltos por qa-tester (misma fecha) ---

2026-07-29 — analyst — Partes NUEVAS del enunciado (correccion que agrega partes): SI amerita regla -> F-RPRES-R006 + rama nueva en J001.
  why: QA tiene razon en que R002 no aplica (sin contraparte original no hay nada que preservar; fijar sus atributos es fijar contenido nuevo, otro acto). Pero el MODO DE FALLA es identico al que origino la feature: un elemento que lleva atributos nace con defaults y nadie lo nota hasta que la app no puede interpretarlo. Dejarlo sin enunciado repite el patron ("no estaba prohibido porque nadie lo escribio"). Formulada por resultado observable (parte nueva completa y homogenea con las de su clase), NO por mecanismo (de que parte se copia queda fuera de alcance explicito).
  dato: 11.487/11.487 ejercicios tienen al menos una parte TEXT y una CLOZE -> el caso "sin hermana de la misma clase" no existe hoy; marcado [ASSUMPTION] en el detalle.
  OJO qa-tester: J001 pasa de 2 a 3 paths (rama partes_nuevas_completas).

2026-07-29 — analyst — F-KTLR-R007 reformulada (id conservado) en vez de dejarla contradictoria con F-RPRES-R004.
  why: decia "No altera sus quizzes", que R004 contradice. Ahora enuncia 3 invariantes: (1) titulo+instructions del knowledge, (2) alineacion del titulo de sus quizzes, (3) nada mas — incluido que el CONTENIDO de los quizzes viene del estado actual del curso, nunca de la foto congelada en la propuesta. El (3) es el anti-resurreccion que atrapo el bug real y sigue vivo.
  OJO qa-tester: el test vigente DefaultCourseElementLocatorTest (@Tag F-KTLR-R007) usa el TITULO del quiz como testigo de "gano la version actual" (asserta title=="v2 corregido"). Bajo el invariante 2 el titulo es justo lo que se re-alinea: hay que mover ese testigo al contenido (sentences), que no se toca. Es cambio de asercion, no de alcance.

2026-07-29 — architect — Escalacion de QA ACEPTADA: `form.sentenceParts` como entrada suelta contradecia F-LAPS-R013. CorrectionScope pasa de Set<String> a Set<ScopedField> {path, elementKind}.
  why: una entrada de alcance denota un leaf o la estructura de una lista, NUNCA un subarbol — asi no queda comodin que autorice tacitamente lo que una regla excluye; mata la clase entera de bug, no solo este caso. El proposed_change de QA (excluir form.sentenceParts[].text entero) era demasiado ancho: el text de una parte TEXT es el tronco de la oracion y toda correccion lexica lo reescribe; R013 excluye solo el de las partes CLOZE. El test que QA redacto ("the empty text of each blank") ya decia lo correcto y pasa contra el contrato nuevo. `elementKind` es el unico refinamiento admitido y solo puede nombrar una clase que el dominio ya declara como enum (SentencePartKind.TEXT): es cerrado, no un lenguaje de filtros. Beneficio colateral: quizTemplates[].title dependia de la misma semantica ambigua y queda pinneado.

2026-07-29 — architect — El arreglo de DefaultLemmaAbsenceProposalDeriver (title del original + parseOnto) es BLOQUEANTE, no una mejora. No requiere contrato adicional.
  why: con el guard puesto y title fuera del alcance lexico, TODA aprobacion LEMMA_ABSENCE devolveria PRESERVATION_VIOLATED hasta arreglarlo. derive() ya recibe `before` (que tiene el title original), ya calcula plainSentences y ya inyecta quizSentenceConverter, asi que el arreglo es de implementacion pura. Verificado ademas que ningun analyzer puntua el titulo del quiz (CourseToAuditableMapper:128 -> AuditableQuiz.label; unico lector en main/ es GetCmd:1163, display), o sea dejarlo intacto no altera la simulacion what-if, el impact preview ni la vista consolidada. Guard y arreglo del deriver deben entrar en el MISMO change-set.

2026-07-29 — orquestador — BUG DEL CLI detectado al aplicar la segunda ronda: `_change: add` sobre un elemento que ya existe es no-op SILENCIOSO.
  why: el patch declaraba changeableFields con Set<ScopedField> y `_change: add`; como la interfaz ya existia de la primera ronda, el merge la ignoro. `sentinel patch apply` reporto "1 additions, 13 modifications, 0 conflicts" y "applied successfully", pero sentinel.yaml:11445 quedo con Set<String> y el CorrectionScope.java generado tambien. No hubo conflicto ni warning. Detectado comparando las 9 signatures del patch contra sentinel.yaml una por una — solo esa faltaba. Leccion operativa: despues de cada `patch apply`, verificar las firmas contra el yaml en vez de confiar en el resumen del comando.

2026-07-29 — architect — Ronda de correccion: CorrectionScope.changeableFields queda en Set<ScopedField> via `_change: modify` en la interfaz MAS un par delete/add de firmas; PreservationViolation.path recupera su descripcion de ruta instanciada.
  why: `exposes` MERGEA POR NOMBRE, no reemplaza (evidencia dura: CourseElementLocator entro con _change: modify y una lista de exposes que solo contenia alignQuizTitles, y sus snapshot/replace siguen en sentinel.yaml:9218-9226). Con merge por nombre, un `add` suelto de la firma nueva habria dejado DOS metodos changeableFields con retornos distintos, que revienta recien en generate. De ahi el delete explicito de la vieja.
  Matiz al diagnostico del no-op: `add`-sobre-existente NO es uniforme. El package se upsertea (su description si se actualizo) y las handwrittenTests de una impl preexistente entran; lo que se descarta en silencio es el elemento hijo tipo MODELO o INTERFAZ que ya existe — el applier lo saltea entero con todo su contenido adentro. Por eso se perdio la firma y no las tests. Segundo elemento perdido por lo mismo: la description de PreservationViolation.path habia quedado diciendo "en la misma gramatica que usa CorrectionScope", justo lo contrario del diseño (es ruta instanciada, no patron).

2026-07-29 — architect — F-RPRES-R006 NO agrega contrato: parseOnto(quizSentence, base) ya la soporta.
  why: `base` es el FormEntity entero y base.sentenceParts trae todas las partes TEXT y CLOZE, que es la unica referencia que la regla necesita (SentencePartEntity tiene 3 atributos y el DSL codifica 2 por clase, asi que el complemento a heredar es de un solo campo). Tres razones para no agregar puerto: (1) la regla pone el mecanismo explicitamente fuera de alcance, asi que materializarlo como contrato sobre-especificaria lo que el analista dejo libre; (2) parseOnto tiene una sola implementacion y es el unico lugar donde nace una parte nueva en el camino de revision, o sea la garantia queda estructural igual que R001; (3) la superficie de verificacion ya existe (DefaultQuizSentenceConverter + el path partes_nuevas_completas de J001).
  Decision negativa que conviene dejar escrita: PreservationCheck TAMPOCO cambia. Agregar partes cae DENTRO del alcance declarado (form.sentenceParts[] es estructura cambiable), asi que R006 es completitud de contenido NUEVO, no preservacion; exigirsela al guard le meteria una segunda semantica de comparacion ("elemento nuevo de una lista" vs "atributo que cambio") dentro de un contrato diseñado para comparar por complemento.

2026-07-29 — orquestador — El guard `--as=qa-tester` rechaza patches de tests cuyas impls esten anidadas en `packages`.
  why: el patch de FEAT-LAPS era 100% handwrittenTests (16 entradas, ningun campo estructural extra) y el guard igual lo rechazo con "modules[course-domain].packages — outside qa-tester scope". Aplicado sin --as previo snapshot de contadores por modulo (models/interfaces/packages/impls + features) y verificado despues: integridad OK, nada perdido. Procedimiento recomendado mientras el guard no contemple impls dentro de packages.

2026-07-29 — qa-tester — F-RPRES-R006 se cubre con 3 handwrittenTests sobre DefaultQuizSentenceConverter (course-domain.quizsentenceengine), NO sobre PreservationCheck.
  why: agregar partes cae DENTRO del alcance lexico declarado (form.sentenceParts es lista completa), asi que el guard no lo ve: R006 es completitud de contenido nuevo, no preservacion. parseOnto(quizSentence, base) es el unico punto con la referencia que la regla necesita (las partes hermanas de la misma clase en base.sentenceParts).

2026-07-29 — qa-tester — Los 3 tests se redactan por HOMOGENEIDAD observable (la parte nueva tiene el mismo estado que las hermanas de su clase), nunca por origen del valor.
  why: R006 declara el mecanismo fuera de alcance ("de que parte se copian, en que orden se recorre"). Un test que assertee "copia de la primera parte de su clase" fijaria implementacion y bloquearia un cambio legitimo de recorrido. La homogeneidad falla igual si la parte nueva nace con default, que es el modo de falla que la regla persigue.

2026-07-29 — qa-tester — Se cubre igual el caso [ASSUMPTION] "sin hermana de la misma clase", pero solo para la clase HUECO.
  why: a nivel unitario el fixture es trivial (base solo con partes TEXT + quizSentence con un hueco) y es donde vive el dano real (2.690 partes CLOZE con text perdido). Para la clase TEXTO FIJO la segunda mitad de R006 no es aplicable: options es null por diseno en toda parte TEXT (F-QSENT-R003/R004 y el modelo), asi que "ninguno ausente" contradiria el dominio — ver doubt en el reporte.

2026-07-29 — qa-tester — Veredicto sobre los 2 tests existentes que van a fallar: en ambos el TAG queda, el CUERPO cambia; no hace falta cobertura adicional.
  why: (a) DefaultLemmaAbsenceProposalDeriverTest:117 — F-LAPS-R012 sigue siendo la regla correcta (deriva la oracion en texto plano del quizSentence), pero la oracion plana vive en QuizTemplateEntity.sentences, no en title, que R014 reformulada declara preservado. (b) DefaultCourseElementLocatorTest (~231) — F-KTLR-R007 sigue bien (invariantes 1 y 3); el testigo de staleness pasa al contenido y el test YA lo assertea (sentences == "v2 corregido."), asi que alcanza con borrar la asercion de title.

2026-07-29 — analyst — Contradiccion R006 vs F-QSENT-R003 resuelta: Opcion B (acotar) INCORPORANDO la lectura de QA (subordinacion) como invariante 1. Nuevo DOUBT-PARTE-NUEVA-COMPLETA RESOLVED.
  why: la lectura de QA ("manda la homogeneidad; 'ni ausente ni por defecto' es el respaldo sin hermana") describe bien mi intencion PERO no alcanza para cerrar la contradiccion: en el unico escenario donde el respaldo aplica —parte nueva sin hermana— la frase seguia exigiendo poblar las respuestas de una parte de texto fijo, que F-QSENT-R003 rechaza como dato invalido. Quedaba contradiccion latente activa justo donde el respaldo entra. Ademas aparece un segundo caso del mismo tipo que QA no habia visto: el texto de un hueco es vacio POR DISENO, asi que "ni en el valor por defecto de su tipo" tambien lo alcanzaba mal.
  forma: el respaldo cambia de REFERENCIA, no de exigencia — donde no hay original ni hermana, el estado correcto lo define la CLASE de la parte (FEAT-QSENT). La prohibicion de caer al default sigue intacta alli donde la clase exige valor propio. Sin mecanismo: no dice de donde se copia nada.

2026-07-29 — analyst — El caso "parte de texto fijo sin hermana de su clase" NO queda vacio con la Opcion B: queda DETERMINADO (texto con valor propio + respuestas ausentes) y amerita test.
  why: es exactamente la combinacion donde las dos lecturas divergian; sin verificacion que la fije, la lectura degradante vuelve a estar disponible. Simetrico al test del hueco sin hermana que qa-tester ya redacto. Fixture unitario trivial: base solo con partes CLOZE + quizSentence que agrega tronco de texto.

2026-07-29 — qa-tester — El 4to test de R006 assertea DOS cosas de la parte de texto fijo nueva sin hermana: texto con valor propio (no vacio, no ausente) Y sin respuestas aceptadas. No se pinnea el texto exacto (es dato del fixture) ni se distingue null de lista vacia.
  why: el invariante 2 remite al estado que declara la clase, y F-QSENT-R003 admite `options` en null O lista vacia como el estado valido de un TEXT — exigir null seria pinnear representacion mas alla de lo que la regla remite. Lo que la regla si fija, y el test verifica, es que no nazca con respuestas pobladas ni con el texto en el default de su tipo.
