# Decisions — FEAT-QICOR

2026-08-04 — analyst — El objeto corregido es el EJERCICIO, no la consigna del knowledge (R001).
  why: el diagnóstico de FEAT-QINST afirma que la consigna es correcta y el ejercicio la incumple. Corregir el knowledge cambiaría el criterio contra el que se juzgó, alcanzaría a sus ~19 ejercicios de una vez y se pisaría con FEAT-KTLR. El caso inverso (la consigna es la culpable) quedó fuera de alcance en DOUBT-CONSIGNA-CULPABLE.

2026-08-04 — analyst — "No empeorar" es UNA sola regla con cinco criterios (R004), no cinco reglas.
  why: los cinco enuncian la misma exigencia y se observan igual (someter el corregido a cada criterio y mirar su veredicto). Lo que difiere es cómo se verifica cada uno, y eso vive en la tabla del detalle: 1 y 2 deterministas reutilizando la medición real del audit (F-SLEN, FEAT-CLEX), 3/4/5 por juicio. Precedente directo: F-LASAG-R007.

2026-08-04 — analyst — R004 exige reutilizar la MEDICIÓN REAL de la auditoría para longitud y léxico, no una aproximación.
  why: aprobar con una regla de medir distinta de la que después juzga hace que la auditoría siguiente vuelva a marcar el mismo ejercicio y el trabajo se haga dos veces. Ya pasó: F-LASAG-R007 nació de una corrección que reemplazó una palabra fuera de catálogo por una mal ubicada para el nivel y nadie lo notó.

2026-08-04 — analyst — Ruptura deliberada con el best-effort de F-LASAG-R009: acá NO se propone el mejor candidato reprobado (R006).
  why: la asimetría es el piso a superar. En lemma-absence el ejercicio emitido es NUEVO y compite contra la nada; acá compite contra un ejercicio existente que ya cumplía longitud, vocabulario, distinción, traducción y resolubilidad — proponer un candidato que reprueba deja el curso peor que antes. Se prohíben los dos desenlaces malos: callarse y entregar-sin-decirlo. Alternativas en DOUBT-SIN-CORRECCION.

2026-08-04 — analyst — R005 verifica el cumplimiento ANTES de proponer, con la misma validación de consigna que detectó el incumplimiento, y sobre el ejercicio entero (no violación por violación).
  why: una propuesta que el operador aprueba y la auditoría siguiente vuelve a marcar es peor que ninguna: consume revisión humana, altera el curso y deja la tarea de vuelta con el operador convencido de haberla resuelto. Contar violaciones resueltas sería progreso invisible para el alumno. Costo (una consulta más por corrección) anotado en DOUBT-COSTO-VERIFICACION.

2026-08-04 — analyst — R003 (edición mínima) se enunció como "lo que ninguna violación menciona vuelve idéntico", no como un tamaño máximo de cambio.
  why: una violación puede señalar legítimamente la oración entera; fijar un tamaño sería arbitrario e infalsificable. La formulación elegida es observable comparando estado anterior vs. propuesto carácter por carácter en las partes no señaladas.

2026-08-04 — analyst — R008 distingue "no intentada por tope" de "intentada y fallida" (R006).
  why: en la primera no hubo gasto y no hay motivo que informar; en la segunda sí. Mezclarlas haría ilegible el resultado de una corrida y ocultaría cuánto se pagó.

2026-08-04 — analyst — Ninguna regla propia sobre la invalidación del veredicto al corregir: ya la garantiza F-QINST-R009 (identidad por huella del contenido).
  why: repetirla acá crearía dos enunciados que pueden divergir. Queda citada en el Contexto y en el detalle de R005 como consecuencia buscada (segunda opinión de la auditoría siguiente) y como costo diferido (una consulta por ejercicio corregido).

2026-08-04 — analyst — Seis dudas abiertas, todas con ASSUMPTION marcada: SIN-CORRECCION (A), REINTENTOS (A, 3 informados), PRESUPUESTO-CORRECCION (A, 20/corrida), DISTINCION-ALCANCE (A, hermanos completo + curso aproximado), COSTO-VERIFICACION (A, siempre), CONSIGNA-CULPABLE (A, fuera de alcance).
  why: ninguna bloquea la arquitectura; las tres que cambian cuánto se paga (REINTENTOS, PRESUPUESTO, COSTO-VERIFICACION) conviene resolverlas con datos de las primeras corridas, no a ojo.

2026-08-04 — architect — R005 no exige tocar `allowedClients`: el juez ya se alcanza por `Evaluator` + `QuizInstructionVerdictReader`; el único hueco era `QuizInstructionSubjectBuilder` (internal). Se abre en su lugar (public + `buildFromView`) sin tocar `serializeQuiz`.
  why: mover la interfaz de paquete no es representable en un patch (el validador ve el alta antes de la baja), y abrirla en su lugar preserva las 1329 huellas ya registradas. Además, al compartir el mismo render, la huella de la verificación de R005 coincide con la que calculará la auditoría siguiente, así que el veredicto queda reutilizable y la consulta se paga una sola vez.

2026-08-04 — architect — Opción A: los 7 criterios en Java detrás de un puerto único (`CandidateCriterionEvaluator`); los de juicio (traducción fiel, resolubilidad) por adaptador de agente, no dentro del grafo generador.
  why: dejar el lazo en el grafo deja R003 y R004(a,b,c) y R005 sin superficie testeable directa, y en este proyecto la cobertura transitiva no cuenta. El argumento decisivo fue otro: como el grafo de corrección es nuevo igual, "reusar" `eval_length.sh` / `eval_lexical.sh` significaba COPIARLOS al grafo nuevo — el criterio quedaba en dos lugares lo mismo. Con A la medición se llama directo (`NlpTokenizer`, `SentenceLexicalEvaluator`), sin subproceso y sin copia. Un eval separado del generador tampoco es el generador corrigiéndose a sí mismo.

2026-08-04 — architect — `CandidateCriterionEvaluator` se diseñó NEUTRO: `evaluate(CandidateAssessmentInput)` no menciona violaciones, tarea ni diagnóstico; `signalledFragments` generaliza "lo que la violación señala".
  why: para que lemma-absence pueda adoptarlo después como migración y no como rediseño. Deuda conocida y aceptada: `FAITHFUL_TRANSLATION` y `SOLVABLE_SAME_KIND` se juzgan hoy en dos lugares — el adaptador de juicio de esta feature y el nodo `eval_quality` de `.sentinel/agents/lemma-absence-agent`. Migrar lemma-absence es alcance ajeno.

2026-08-04 — orchestrator — El renombre del contrato del deriver (`LemmaAbsenceProposalDeriver` → `QuizSentenceProposalDeriver`, `LemmaAbsenceQuizCandidate` → `QuizSentenceCandidate`) SE SACA de esta entrega.
  why: la afirmación de que los tests de FEAT-LAPS quedaban intactos vale para las declaraciones en `sentinel.yaml`, no para el código Java. 25 archivos mencionan los tipos y el smart-merge no reescribe referencias dentro de cuerpos escritos a mano (`DefaultLemmaAbsenceProposalDeriverTest:91`, `LemmaAbsenceMvpStrategy:42`, `FLapsJ001JourneyTest:266`), así que dejan de compilar hasta barrerlos. Peor: el renombre del patch era PARCIAL —contrato sí, clase no—, dejaba `DefaultLemmaAbsenceProposalDeriver implements QuizSentenceProposalDeriver` y necesitaba un segundo patch de FEAT-LAPS igual. FEAT-QICOR usa el contrato existente con su nombre feo; el renombre completo va después en un patch propio de FEAT-LAPS, que es su dueño.

2026-08-04 — analyst — Agregada F-QICOR-R009: antes de corregir se revalida el ejercicio ORIGINAL con la validación vigente; si cumple, no se corrige nada y la tarea termina con un desenlace propio ("diagnóstico caído").
  why: hueco real detectado revisando la arquitectura. El veredicto se reutiliza bajo la huella del contenido cualquiera sea el criterio que lo emitió (F-QINST-R008/R010) y sólo se rehace a pedido (F-QINST-R012): un veredicto viejo genera tarea, el corrector reescribe un ejercicio que estaba bien, R005 pregunta "¿cumple?" y responde que sí (siempre cumplió), y el operador aprueba una reescritura innecesaria. No es hipotético: el criterio castigaba de forma inconsistente una convención presente en 1814 ejercicios (15,8% del curso) — sobre 20 idénticos aprobaba 17 y marcaba 3. Efecto buscado: auto-reparación, y el veredicto de la revalidación queda registrado bajo la huella, así que la auditoría siguiente lo reutiliza sin volver a pagarlo.

2026-08-04 — analyst — R009 se numeró al final (no se insertó como R005 renumerando), aunque se lee entre R003 y R004.
  why: misma práctica que FEAT-QINST, que agregó R015/R016/R017 al final estando su lugar lógico en otro lado. El orden de lectura se resuelve en la lista de tramos del encabezado (ahora cuatro tramos, con "Antes de tocar nada (R009)" en segundo lugar).

2026-08-04 — analyst — "Diagnóstico caído" es un TERCER desenlace, nunca un fracaso; R006 y R008 se ajustaron para convivir.
  why: no fracasó nada — no se generó candidato ni reprobó criterio alguno. Contarlo como corrección no lograda le diría al operador que tiene decenas de correcciones imposibles cuando tiene decenas de diagnósticos viejos que se cayeron solos, y reacciona de forma opuesta ante cada cosa. R006 suma la cláusula "supone que HABÍA algo que corregir" + párrafo de no-confusión; R008 pasa a exigir CUATRO recuentos separados (propuesta / no lograda / caída / no intentada).

2026-08-04 — analyst — Tres dudas nuevas: DOUBT-REVALIDACION-PRESUPUESTO (A: el tope cuenta tareas tocadas), DOUBT-TAREA-CAIDA (B: se marca y queda visible), DOUBT-CAUSA-DEL-DESENLACE (A: un solo desenlace, sin distinguir causa).
  why: presupuesto = A por prudencia (es la única en que ninguna corrida gasta más de lo que su tope dice); si la limpieza resulta masiva, C (dos topes) pasa a ser la sensata. Tarea caída = B porque la limpieza definitiva llega sola —la auditoría siguiente puntúa el ejercicio como cumplidor y el plan ya no genera la tarea (F-QINST-R017)—, la marca sólo tiene que sobrevivir hasta ahí; C paga una consulta por corrida para llegar siempre a la misma conclusión. Causa = A porque la acción del sistema es idéntica en ambos casos; B es mejora de reporte y NO requiere cambiar ninguna regla.

2026-08-04 — architect — Todo elemento nuevo que se enchufa a un framework tiene que copiar la forma COMPLETA de su vecino más cercano (`externalImplements`, `visibility`, `implements`), no solo la parte que expresa la intención de diseño.
  why: tres arreglos consecutivos en esta misma feature compartieron causa — el genérico del puerto (`implements` sobre `CorrectionContextResolver<T>`), los dos ejes de visibilidad (paquete interno vs. consumidor en `commands`), y el contrato de picocli (`Callable<Integer>`). En los tres el precedente estaba a la vista en el mismo paquete y no se comparó campo por campo. El síntoma siempre aparece recién al compilar o al ejecutar, nunca al validar el patch.

2026-08-04 — orchestrator — `serializeQuiz` delega en `serializeParts`; NO se duplica el algoritmo.
  why: la instrucción original al desarrollador ("no toques `serializeQuiz` ni una coma") se leyó al pie de la letra y produjo una copia. Lo que no podía cambiar era la SALIDA, no el archivo: duplicar derrota el motivo de haber promovido el seam, que era que el render viva en un solo lugar para el día que se decida incluir `form.sentences`. Delegar es demostrablemente inocuo —los dos cuerpos eran idénticos carácter por carácter— y el test de paridad de R005 lo confirma en verde.

2026-08-04 — architect — Declarar `implements` sobre un puerto genérico FUNCIONA si la clase ya tiene los métodos con tipo concreto; falla solo sobre stub vacío.
  why: el generador no sustituye variables de tipo, pero el merge conserva los métodos escritos a mano y solo aporta la cláusula. `KnowledgeTitleContextResolver` lleva así desde siempre. ARCH-QICOR-002 generalizó desde una única observación sobre un stub y creó una mina: cada `sentinel generate` borraba la cláusula y rompía el build. Cerrado por ARCH-QICOR-005, verificado regenerando tres veces.

2026-08-04 — architect — En `audit-cli` el composition root vive en `commands`: todo lo que instancia o tipa tiene que ser `public` aunque su paquete sea interno.
  why: son dos ejes de visibilidad —la clase Java y el alcance del paquete— y mirar solo uno produjo tres errores de una sola pasada.

2026-08-04 — orchestrator — El patch consolidado que acompaña al TECH_SPEC reconstruido NO se aplica.
  why: describe el estado que `sentinel.yaml` ya tiene (verify OK y suite verde lo prueban). Aplicarlo solo agrega el riesgo de que un `modify` pise algo, sin ganar nada. Existe para que los 18 fences del spec validen contra un patch; la historia real está en `.applied-patches/`.

2026-08-04 — developer — `LedgerBackedQuizInstructionComplianceChecker.revalidate` usa `resolveForced`, no `resolve`.
  why: sin eso R009 sería un no-op elegante — la revalidación devolvería el veredicto viejo guardado bajo la misma huella y el diagnóstico se confirmaría a sí mismo, que es exactamente el agujero que la regla vino a tapar.

2026-08-04 — developer — `--workdir` para el juez (`quiz-instruction-validator`) y `candidate-judgment`: se fija en el `AgentGraphRunnerConfig` (agentsBaseDir/runsBaseDir explícitos), NO poblando `inputs["project_root"]` como hacen quiz-instruction-corrector/knowledge-title-agent.
  why: esos dos SÍ pueden agregar una key de framework a sus inputs porque no tienen un contrato de estabilidad de huella declarado sobre ese mapa. El juez sí lo tiene (F-QINST-R009: "no key added, removed or reordered" sobre `EvaluationSubject.getContent()`, de la que depende la reutilización de veredictos), y `candidate-judgment` comparte el mismo mecanismo de evaluación por huella. Meter `project_root` ahí para resolver un problema de path habría abierto un problema de invalidación de cache. El config es un nivel más abajo y no lo toca.
