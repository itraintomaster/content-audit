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
