# Decisions — FEAT-QINST

2026-07-29 — analyst — La identidad del veredicto es la huella del contenido juzgado, nunca el identificador del ejercicio (R009).
  why: corregir un ejercicio no cambia su identificador (FEAT-RPRES). Con identidad por id, el sistema seguiria mostrando el juicio de la version vieja: acusaria a un ejercicio ya corregido o daria por bueno uno recien roto. La huella incluye nivel, topic, titulo e instrucciones del knowledge y el ejercicio completo; cambiar las instrucciones deja pendientes a TODOS los ejercicios del knowledge (cascada buscada).

2026-07-29 — analyst — Escala de puntaje graduada 1,0 / 0,6 / 0,3 / 0,0 por severidad, no binaria (R002).
  why: el puntaje se promedia por la jerarquia; con escala binaria un knowledge con un defecto discutible y otro con un ejercicio irresoluble se leen igual y el usuario pierde el criterio de priorizacion. Con ~19 quizzes por knowledge, un critico baja ~0,05 y un menor ~0,02 el promedio del knowledge. Valores concretos en DOUBT-ESCALA-SEVERIDAD.

2026-07-29 — analyst — Combinacion inconsistente del veredicto: manda la severidad; incumple con severidad "ninguna" se puntua como menor (R002).
  why: criterio conservador. Dar por bueno un ejercicio malo lo hace llegar al alumno; un falso positivo solo cuesta una revision humana.

2026-07-29 — analyst — El veredicto conservador de infraestructura del juez (confianza 0 + violacion de salida invalida) es falla transitoria, no juicio (R013).
  why: es el modo de falla mas caro de la feature. Tratado como juicio daria 0,0 por R002 (acusacion falsa) y quedaria cacheado para siempre por R008: un problema de infraestructura convertido en defecto permanente del contenido.

2026-07-29 — analyst — Lo no evaluado no recibe puntaje ni diagnostico, y el informe declara la cobertura (R004 + R005).
  why: puntuar 1,0 lo pendiente reportaria un curso impecable en la primera corrida; puntuar 0,0 hundiria todo knowledge parcialmente evaluado; un valor intermedio mezcla "se juzgo asi" con "no se juzgo". La cobertura explicita es lo que hace legitimo el informe parcial.

2026-07-29 — analyst — Corre por omision (R001) y se excluye explicitamente (R011); excluido NO aparece en el informe, en vez de aparecer con cobertura cero.
  why: cobertura cero significa "corri y no pude evaluar nada" — un hecho digno de atencion; excluido significa "no me pediste que corriera". Correr por omision es viable solo porque el reuso es gratis (R008) y las consultas nuevas tienen tope (R006).

2026-07-29 — analyst — Cambiar la version del juez no borra ni re-evalua; los viejos quedan como historia y los quizzes figuran pendientes (R010). Re-evaluar es explicito y respeta el presupuesto (R012).
  why: decision del usuario. Ajustar el prompt es barato y frecuente; re-juzgar 11.500 quizzes es lento y caro. Si lo primero disparara lo segundo, nadie tocaria el juez.

2026-07-29 — analyst — DOUBT-QUIZ-COMPLETO declarado BLOQUEANTE, con la opcion C (juzgar con la representacion parcial de hoy) descartada desde el requerimiento.
  why: la representacion auditable conserva oraciones y tokens pero no todas las partes ni las opciones aceptadas. Sin ellas el juez no puede verificar la restriccion mas frecuente (la forma de la respuesta) Y la huella deja de proteger: cambiar una opcion aceptada no invalidaria el veredicto guardado — error silencioso. A (enriquecer la representacion auditable) vs B (leer el contenido completo al evaluar) es decision de arquitectura.

2026-07-29 — analyst — R014 (alcance = todos los ejercicios) queda como ASSUMPTION con duda propia.
  why: gramatica, significado y resolubilidad aplican aunque el knowledge no declare instrucciones. Pero es la unica duda abierta que cambia cuanto se paga, asi que conviene medir cuantos quizzes quedarian fuera con B/C antes de la primera pasada.

2026-07-29 — analyst — DOUBT-ALCANCE-EJERCICIOS RESUELTO (Opcion A) con medicion sobre el curso real; F-QINST-R014 pasa de ASSUMPTION a VALIDATED. Ninguna regla cambia (A era lo ya especificado).
  why: medicion sobre db/quiz-templates.json (11.487 quizzes) y db/knowledges.json (668 knowledges): sin `instructions` = 20 quizzes (0,17%), con `isSentence=false` = 0. La Opcion B ahorraria 20 consultas de 11.487 y dejaria esos 20 sin ninguna revision de resolubilidad (ningun otro analisis la hace); la Opcion C no excluiria ni uno. Contexto: los 11.487 son CLOZE, todos con form.sentences y >=1 opcion aceptada, 321 con mas de una.

2026-07-29 — analyst — Hallazgo colateral de esa medicion, anotado en la duda y NO llevado a regla: instructions/title/topicName viven tambien en el propio quiz template, no solo en el knowledge.
  why: no altera lo que exige R009 (la huella cubre el contenido que el juez recibe, venga de donde venga), pero si toca decidir de donde se toma cada dato al armarlo. Queda para DOUBT-QUIZ-COMPLETO, que esta en manos de @architect; no se toco R009 para no pisar su trabajo en paralelo.

2026-07-30 — architect — DOUBT-QUIZ-COMPLETO resuelta por Opcion A: AuditableQuiz lleva sentenceParts (copia verbatim del entity) + instructions; AuditableKnowledge suma topicName.
  why: B obliga a probar que la segunda lectura ve el mismo estado que audita la corrida; A elimina la pregunta. Ademas la copia verbatim mantiene fuera del camino el bug de QuizSentenceParser.buildForm(), que mutilaria el contenido sobre el que se calcula la huella. Costo aceptado: ~+15% sobre los ~117MB de informes, pagado por todos los analizadores aunque solo este use el campo (almacenamiento, no tiempo ni dinero).

2026-07-30 — architect — EvaluationAnalyzerFactory permite analizadores construidos por corrida; DefaultAuditRunner no conoce ningun evaluador concreto.
  why: presupuesto y cobertura son estado de corrida, no del analizador.

2026-07-30 — architect — Discrepancia abierta R009 vs. el juez probado: R009 dice que title/instructions vienen del knowledge; las fixtures con las que el juez fue validado usan las del quiz template. El payload se diseno para que ambos viajen. Pendiente: que @analyst ajuste R009 o que el usuario confirme cual manda.

2026-07-30 — architect — El quizId del veredicto vuelve vacio: el payload al juez se despoja de identificadores, porque si viajara el id entraria en la huella por R002 y contradiria R001. emit.sh tolera quizId "". Trazabilidad por id exigiria excluir el id de la huella explicitamente, o sea relajar R002.

2026-07-30 — analyst — Cerrada la discrepancia R009 que abrio @architect: NO existe. El knowledge queda como fuente canonica y el detalle de R009 explicita que el ejercicio lleva copias desnormalizadas.
  why: medicion sobre el curso real: quiz.instructions == knowledge.instructions en 11.487/11.487 y quiz.title == knowledge.label en 11.487/11.487 — mismo contenido, no dos fuentes en conflicto. La primera comparacion dio "todos distintos" por comparar contra knowledge.title, campo que NO EXISTE (el knowledge tiene `label`; el title del quiz es su copia desnormalizada). Se mantiene el knowledge porque es lo que da sentido a la cascada de R009, y porque hay un paso de publicacion que sincroniza quiz.title := knowledge.label: la divergencia es defecto conocido, no fuente alternativa legitima.

2026-07-30 — analyst — Vocabulario corregido en todo el documento: "titulo del knowledge" -> "etiqueta del knowledge" (enunciado y tabla de R009, accion de J001, Contexto).
  why: knowledge.title no existe. El cambio es de nomenclatura; no altera ninguna regla, ni el grafo de J001 ni la cantidad de paths.

2026-07-31 — architect — Hueco 1 (evaluatorVersion siempre null): lo expone la SESION (`EvaluationSession.evaluatorVersion(): String`), no la factory por constructor.
  why: la sesion ya usa esa version para armar cada EvaluationKey (F-EVCOST-R001); pasarla por constructor al analyzer habria duplicado la fuente de verdad y cambiado su requiresInject. Con esta forma `QuizInstructionAnalyzer` NO cambia de constructor: los 21 tests + 9 paths de journeys siguen compilando (mockean EvaluationSession; el metodo nuevo sin stub devuelve null, exactamente el valor de hoy).

2026-07-31 — architect — Hueco 2 (degradacion silenciosa del digest): se extrae `AgentDefinitionLocator` (+ Found/Unresolved + factory) a agent-runtime-infrastructure y el RUNNER pasa a usarlo tambien.
  why: si solo el resolutor de version usara el locator quedarian dos cadenas que pueden volver a divergir. Con el runner delegando, "el directorio del que sale el digest" y "el directorio que se carga para correr el grafo" son el mismo por construccion. El resultado es un valor con dos formas (no un boolean descartable), asi que "no encontre nada" no puede confundirse con "encontre un directorio vacio"; y al no localizar la definicion la version sale con prefijo reservado `unresolved-agent-definition:` (nunca coincide con una registrada) y la misma corrida da EVALUATOR_UNAVAILABLE al cargar el grafo (F-QINST-R007).

2026-07-31 — architect — Hueco 3 (dos mecanismos de re-evaluacion): manda el CONSUMIDOR. La sesion se abre con `EvaluationBudget` (solo maxNewEvaluations) y `EvaluationRunPolicy` se muda a audit-domain con sus 3 campos intactos.
  why: `reevaluationScope` es un id de knowledge y de un sujeto la sesion solo conoce `subjectRef` (id de ejercicio): la capacidad general no puede aplicar ese alcance ni aunque quisiera. La respuesta de EVCOST a "una parte acotada" (F-EVCOST-R008) es `resolveForced(subject)` y la seleccion de sujetos es del consumidor — que es lo que `QuizInstructionAnalyzer` ya hace hoy y queda sin tocar. Consecuencia: FEvcostJ003 path-2 (audit-cli) esta mal escrito (abre con policy(10,true,null) y llama a `resolve()` esperando EVALUATED); debe usar `resolveForced()`. Re-diseño de test → @qa-tester/@test-writer, NO lo toco.

2026-07-31 — architect — Pendientes para @analyst (no resueltos aca): (a) ninguna regla exige hoy que la cobertura declare la version del juez, asi que el campo `evaluatorVersion` de QuizInstructionCoverageDiagnosis no tiene test posible — conviene extender F-QINST-R005 (o R010); (b) ninguna regla exige que una version que no pudo derivarse de la definicion se distinga de una legitima — conviene extender F-QINST-R010; (c) opcional, F-EVCOST-R008 dice "una parte acotada" sin decir quien la acota: aclarar que la selecciona el consumidor.
  why: sin regla no hay test tageable, y en este proyecto la cobertura transitiva no cuenta.

2026-07-31 — architect — TECH_SPEC.md reemplazado (752→247 lineas) porque `tech-spec append` valida TODAS las fences del archivo contra el patch vigente y las 15 originales pertenecen al patch ya aplicado. El original queda en git: `git show 17449eca:requirements/2026-07-29.02_validacion-de-consigna-de-quiz/TECH_SPEC.md` (y su patch en `.applied-patches/`). El puntero quedo escrito dentro del nuevo TECH_SPEC.

2026-07-31 — analyst — Pendiente (a) de @architect RESUELTO extendiendo F-QINST-R005 (no R010): el informe declara **con que version del juez** se evaluo, y esa version es unica por informe (la vigente) porque solo se reutilizan veredictos de la version vigente. Titulo de R005 ampliado; fila nueva en su tabla de cobertura; criterio de aceptacion y Error actualizados.
  why: R005 es la regla que enumera lo que el informe declara, y el dato vive en la cobertura. R010 habria quedado con dos invariantes ortogonales (no-borrar/no-re-evaluar + declarar). La version es lo que vuelve AUDITABLE a R010: sin ella, los numeros de reutilizados se leen igual estando bien y estando mal, y con versiones conviviendo en el registro (F-EVCOST-R006) no hay forma de saber cual mira el informe.

2026-07-31 — analyst — Pendiente (b) RESUELTO: F-QINST-R010 reformulada como tres invariantes numeradas y retitulada "La version del juez gobierna la vigencia de los veredictos". La invariante 3 exige que una version que NO pudo derivarse de la definicion del juez no se presente como valida: no coincide con ninguna derivada de una definicion accesible, ningun veredicto se da por vigente bajo ella, y esa corrida trata al juez como no disponible (R007).
  why: las invariantes 1 y 2 se apoyan enteramente en que la version se derive de la definicion; si el sistema no accede a esa definicion y aun asi produce una version con la misma forma que una legitima, retocar el prompt deja de invalidar nada y la corrida puntua con veredictos de OTRA version. Error silencioso: sin error visible ni diferencia en el informe. Desenlace exigido = conservador (perder una corrida del analisis) para no perder la garantia de que un puntaje corresponde al juez que el informe declara. Enunciado sin nombrar directorios, prefijos, archivos ni tipos.

2026-07-31 — analyst — Ids intactos (R005/R010 se extienden, no se crean reglas nuevas) y NINGUNA journey tocada: `feature status` = OK, no hace falta `sentinel feature sync`. AVISO a @qa-tester: la cobertura transitiva no cuenta en este proyecto, asi que hacen falta handwrittenTests NUEVOS para (a) "la cobertura declara la version vigente del juez" bajo R005 y (b) las dos mitades de la invariante 3 de R010 (no reutiliza nada / trata al juez como no disponible) — los tests existentes tageados a R005/R010 no las cubren.

2026-08-01 — developer — ESCALACION type: interface_change (visibilidad) — `QuizInstructionJudgeConfigResolver` (interfaz) esta declarada `visibility: internal` en sentinel.yaml y `DefaultQuizInstructionJudgeConfigResolver` (impl) no declara `visibility` (default package-private), ambas en `audit-cli/.../bootstrap`. Todos sus 5 hermanos en el mismo paquete usados igual desde `Main` (`WorkdirResolver`/`DefaultWorkdirResolver`, `ApprovalModeResolver`/`DefaultApprovalModeResolver`, `ProposalStrategySelector`/`DefaultProposalStrategySelector`, `LagenModeResolver`/`DefaultLagenModeResolver`, `LagenConfigResolver`/`DefaultLagenConfigResolver`) son `visibility: public`. Confirmado empiricamente (probe de compilacion): `Main` (paquete `commands`) NO puede referenciar ni instanciar el resolver de consigna hoy — "is not public ... cannot be accessed from outside package". Pedido: alinear la visibilidad de ambos (interfaz + impl) a `public`, igual que sus 5 hermanos, para que `Main` los use directo. Mientras tanto se agrego un bridge NO generado (`QuizInstructionJudgeConfigResolverBootstrap`, mismo paquete `bootstrap`, publico, delega en el resolver real) para no duplicar el contrato de env-vars ni bloquear el cableado; una vez aplicado el patch, `Main` deberia llamar al resolver directo y el bridge se puede borrar.
  why: sin este ajuste el resolver real (con su logica de env-vars ya implementada) queda inalcanzable desde el composition root, que es exactamente donde el requerimiento pide que se resuelva la config del juez ("cableado del composition root", nota de @qa-tester en el mismo stub).

2026-08-01 — architect — Hueco 4: EvaluationAnalyzerFactory expone analyzerName() y PIERDE evaluatorId(); AuditRunRequest.evaluationPolicies -> analyzerPolicies, indexado por nombre de analizador. Evaluator.evaluatorId() intacto (clave del registro, F-EVCOST-R001).
  why: el runner filtraba y presupuestaba por el nombre del agente ("quiz-instruction-validator") mientras el informe muestra el del analizador ("quiz-instruction"). Dos sintomas verificados con el binario: --exclude-analyzers quiz-instruction no excluia, y --instruction-budget N corria con el default (500) porque policies.get() nunca acertaba. Opcion B (dejar los dos metodos) descartada: conservaba la trampa —dos String intercambiables en la interfaz que el runner mira— sin consumidor concreto.

2026-08-01 — architect — analyzerName() se expone en la FACTORY, no en el analizador.
  why: F-QINST-R011 exige que un analisis excluido nunca se construya, asi que el nombre tiene que conocerse sin instanciar. Del lado de implementacion, DefaultQuizInstructionAnalyzerFactory vive en el mismo paquete que QuizInstructionAnalyzer y puede devolver la misma constante que getName(): la coincidencia entre lo que se filtra y lo que se muestra queda por construccion, no por disciplina.

2026-08-01 — El punto ciego, documentado: DefaultAuditRunnerTest (18) + FQinstJ003JourneyTest (4) estaban 22/22 VERDES con el bug vivo en produccion.
  why: los dobles usaban un unico literal para tres roles distintos (nombre del analizador, clave de exclusion/politica, id del evaluador del registro). Con un solo string, cualquier identidad que el runner mire da el mismo resultado. AnalyzeCmdTest en cambio ya asertaba la clave "quiz-instruction": ese test estaba bien y el producto estaba mal.

2026-08-01 — analyst — F-QINST-R015 nueva: "El analisis tiene un solo nombre: el que se lee es el que se escribe". FEAT-QINST pasa a 15 reglas.
  why: sin ella el test que atrapa el hueco 4 no tiene a que taggearse y qa-tester terminaria forzando el tag. La propiedad es general (los demas analisis ya cumplen), pero este es el primer analisis con DOS nombres candidatos —el suyo y el del juez que lo respalda—, asi que la coincidencia deja de ser un hecho y pasa a ser una exigencia.
