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

2026-08-09 — analyst — La traduccion SALE del alcance de la edicion acotada (R003) y queda con un unico juez: el criterio de traduccion fiel (R004 criterio 4), que la juzga contra la oracion PROPUESTA. R003 conserva sobre ella una sola exigencia: no puede cambiar si la oracion no cambio.
  why: primer lote real de 20 tareas = 1 propuesta / 19 no logradas, y 17 de las 19 reprobaron SCOPED_EDIT. La evidencia que emite el juez habla del ingles (respuesta + cue) y nunca del español, asi que "cambio señalado por la violacion" volvia la traduccion incorregible justo en la familia de defectos donde arreglar la respuesta OBLIGA a arreglar la traduccion (13/20 candidatos la cambiaron, y en los 13 el cambio era correcto). Peor que bloquear: premiaba la inconsistencia — la unica correccion que paso fue la que no la toco. Y tener dos criterios opinando sobre la traduccion con exigencias opuestas era una contradiccion de la especificacion, no una doble precaucion.

2026-08-09 — analyst — Descartada la lectura "la traduccion es una parte mas del ejercicio y se congela salvo que una violacion la señale".
  why: la traduccion no tiene contenido propio — es la oracion dicha en español. Congelarla mientras la oracion cambia legitimamente no conserva nada: produce un ejercicio que se contradice a si mismo. R003 existe contra el cambio COLATERAL que nadie pidio; una traduccion que sigue a su oracion no es colateral sino consecuencia obligada del cambio que la violacion SI autorizo. El incentivo perverso lo cierra ahora R004 criterio 4: la traduccion vieja de una oracion nueva reprueba fidelidad.

2026-08-10 — analyst — Una parte VACIA del enunciado (texto fijo sin un solo caracter) NO es contenido del ejercicio. Nueva F-QICOR-R010, con dos mitades: (1) queda fuera de la comparacion de R003 —traerla o no nunca decide si un candidato se propone—; (2) al aprobar, el ejercicio guardado la CONSERVA en su misma posicion.
  why: bloqueo estructural medido — 192/519 tareas (37%) y 4015/11487 ejercicios (35%) incorregibles. La parte vacia no deja caracteres en la forma de texto por la que viaja el ejercicio, asi que se pierde en el ida-y-vuelta por construccion (no es un parser arreglable: no hay de donde recuperarla). Tres mediciones propias sobre db/english-course sostienen que no es contenido: (a) 0 caracteres; (b) SIEMPRE en un borde — 470 al principio, 3545 al final, CERO entre dos partes con texto; (c) el curso ya guarda 884 ejercicios (648 TEXT+CLOZE + 236 CLOZE+TEXT) en la forma exacta que produce la correccion, y funcionan. Ademas R003 siempre se enuncio sobre caracteres ("identico caracter por caracter") — contar partes era MAS estricto que la regla, y ese exceso era el que bloqueaba.

2026-08-10 — analyst — La mitad "se conserva al guardar" se eligio contra la alternativa de normalizar (dejar que la correccion la pierda). Registrada como DOUBT-PARTE-VACIA-AL-GUARDAR, opcion A.
  why: "no es contenido" corta para los dos lados — si ninguna violacion pidio quitarla, nada autoriza a quitarla. La asimetria de costos decide: conservar cuesta cero (es una parte sin texto) y equivocarse es reversible; perderla reestructura hasta 4015 ejercicios DE A UNO, en silencio, por la puerta de una correccion de consigna, y es irreversible sobre un tercio del curso. Es la forma de daño que dio origen a FEAT-RPRES (2658 ejercicios degradados porque nadie lo habia escrito). FEAT-RPRES ya legislo el caso simetrico —mas partes que el original, F-RPRES-R006—; menos partes era el hueco.

2026-08-10 — analyst — Guarda explicita en R010: la DESAPARICION de una parte vacia tampoco es un cambio. Un candidato cuya unica diferencia con el original es no traerla NO se propone.
  why: sin esa clausula la relajacion abria la puerta a que un ejercicio textualmente identico al original contara como "estado propuesto distinto" (criterio de aceptacion de R001). Con ella, ese candidato cae por R003 (oracion identica => traduccion tambien) y por R005 (sigue incumpliendo), y termina en NO LOGRADA.

2026-08-10 — analyst — R010 NO toca F-RPRES-R002 (vacio != ausente). Habla de una PARTE ENTERA sin caracteres, no del atributo de texto de un hueco.
  why: la distincion es facil de conflar y el error seria caro — el propio ejemplo real del curso tiene un hueco con text=null en un ejercicio y text="" en otro. Nada de R010 autoriza a convertir un valor vacio en ausente.

2026-08-09 — analyst — Nueva DOUBT-TRADUCCION-COLATERAL (opcion A: alcanza con la fidelidad).
  why: cambiada la oracion, cualquier traduccion fiel pasa — tambien una reescrita de mas (churn que el operador tiene que leer). La opcion B (edicion minima tambien sobre la traduccion) reintroduce el "cuanto cambio es demasiado" que R003 evito a proposito, sobre un texto que admite redacciones muy distintas. Revisar con datos de las proximas tandas.

2026-08-11 — analyst — La longitud SALE del conjunto de criterios que pueden rechazar una correccion. Nueva **F-QICOR-R011** (Validation: VALIDATED, decision explicita del usuario): se sigue midiendo con la medicion real de la auditoria, la propuesta DECLARA que quedo fuera de rango (longitud medida + rango del nivel) y la corrida cuenta cuantas propuestas quedaron asi. El criterio 1 de R004 conserva su numero y su lugar en el catalogo, pero pierde el veto.
  why: la decision del usuario es que importa mas que la quiz este bien escrita que si entra en rango. Y la medicion la respalda por una razon mas fuerte que la preferencia: de los **78** candidatos rechazados SOLO por longitud (el motivo mas frecuente: 78 vs 71 de edicion no acotada vs 10 el resto), **75 tenian ademas su propia tarea de longitud en el mismo plan** — o sea que el ORIGINAL ya estaba fuera de rango. Solo 3 estaban dentro y salieron por la correccion. El criterio no impedia un empeoramiento: exigia que la correccion de consigna arreglara de paso un defecto preexistente que ninguna violacion señalaba — exactamente lo que R004 ya prohibe para el criterio 2 ("los flags preexistentes no bloquean"). Era el unico criterio ABSOLUTO dentro de una regla titulada "No empeorar".

2026-08-11 — analyst — Descartada la opcion "preferencia" (entre dos candidatos que pasan todo lo demas, gana el que entra en rango).
  why: dos razones y la primera ya esta medida. (1) Los 78 consumieron 3 intentos CADA UNO (234 consultas) con el motivo del fallo a la vista en cada reintento, y en 78/78 el reintento informado nunca produjo un candidato en rango; en esa misma corrida las 125 propuestas salieron TODAS al primer intento (ningun reintento rescato una sola tarea). La preferencia vuelve a comprar lo que ya se compro y se perdio. (2) El catalogo NO puede ver "bien escrito" —no hay criterio de naturalidad—, asi que "gana el que entra en rango" selecciona sistematicamente la variante comprimida sobre la natural: es la inversion exacta de la decision, y silenciosa.

2026-08-11 — analyst — Descartada tambien la opcion "no-regresion" (bloquear solo si el candidato empeora la longitud respecto del original), pese a ser la mas fiel al titulo de R004.
  why: destrabaria 75 de 78, pero (a) deja viva la veto justo en el caso sobre el que el usuario decidio —el que estaba dentro y sale—; (b) seguiria bloqueando a los que ya estaban fuera y quedan mas afuera, que es el subconjunto MAS probable porque estas correcciones alargan (`is he` → `are they walking`); (c) obliga a fijar un umbral de "peor" en tokens sobre una magnitud que ya tiene margen de tolerancia propio (F-SLEN-R009), umbral que ninguna violacion pidio.

2026-08-11 — analyst — Quitar el veto de longitud NO deja la oracion sin control, y la interaccion con el analizador de longitud es la red de seguridad, no un problema.
  why: lo que impide que la oracion se infle nunca fue el criterio 1 sino R003 (solo se toca lo que la violacion señala; el resto vuelve identico caracter por caracter). Y si el corregido queda fuera de rango, el analizador lo vuelve a marcar y le abre SU PROPIA tarea con SU PROPIO corrector (FEAT-RCSL): cada defecto en su carril. No contradice R005 —esa regla existe para que no vuelva el MISMO diagnostico; que aparezca OTRO es el sistema funcionando—. Ademas es la postura que el resto del sistema ya tenia: FEAT-RCLA declaro desde su primera entrega que no valida la longitud del resultado y que un resultado fuera de rango reaparece como tarea de longitud; F-LASAG-R009 entrega el mejor candidato aunque reprobe. FEAT-QICOR era la UNICA correccion del sistema donde la longitud rechazaba.

2026-08-11 — analyst — El recuento de propuestas fuera de rango NO es un quinto desenlace de R008: es un atributo de la correccion propuesta. R008 queda con sus cuatro desenlaces y un puntero; la decision vive entera en R011.
  why: misma disciplina que R010/R007 — no legislar lo mismo en dos lugares. Y el recuento es lo que vuelve REVISABLE la concesion: si mañana casi todas las propuestas quedaran fuera de rango, el que hay que mirar es el corrector, no el criterio, y sin el recuento nadie se entera.

2026-08-11 — analyst — Corregida una premisa del Contexto que la medicion desmiente: ya no se afirma que el ejercicio a corregir "ya esta dentro del rango de longitud de su nivel". 211 de las 519 tareas de consigna (41%) caen sobre ejercicios que TAMBIEN tienen tarea de longitud.
  why: esa frase era la que hacia parecer razonable un criterio absoluto de longitud. El piso a superar se define ahora eje por eje: no empeorar donde el original estaba bien (R004), no exigir donde ya estaba mal (R011).

2026-08-11 — analyst — Dos dudas nuevas: DOUBT-LONGITUD-NO-BLOQUEANTE (RESOLVED, opcion A, con B y C documentadas y refutadas con datos) y DOUBT-LONGITUD-IDA-Y-VUELTA (OPEN).
  why: la segunda es un riesgo real que NO se legisla porque no esta medido: la correccion de longitud acorta la oracion y nada garantiza hoy que respete la consigna, asi que las dos correcciones podrian empujarse mutuamente. Ninguna de sus tres opciones cambia una regla de este documento —la duda es sobre OTRA correccion—, y los 211 ejercicios que ya tienen las dos tareas hacen la medicion inmediata.

2026-08-11 — analyst — **El lazo de reintentos pasa del sistema a quien corrige, y al agotarlo se ENTREGA el mejor candidato en vez de descartarlo.** Cuatro reglas nuevas (todas VALIDATED, decision explicita del usuario): **R012** (una sola solicitud de correccion por tarea; el lazo, el champion y la decision de reintentar viven del lado de quien corrige; maximo de intentos configurable fijado por la corrida), **R013** (que es "mejor": elegibilidad = cumple consigna + cambio real; despues cantidad de criterios cumplidos; despues precedencia fija resolubilidad > traduccion fiel > vocabulario > distincion > edicion acotada; la longitud ultima y sin sumar; empate final = el primero visto), **R014** (lo entregado que no cumple todo se entrega DICIENDOLO: criterios incumplidos en la propuesta + recuento en la corrida), **R015** (los criterios se pueden consultar por separado con la misma medicion — capacidad nueva, sin la cual mover el lazo significa copiar el catalogo).
  why: el usuario lo pidio como uniformidad ("tenemos que tener la misma forma de hacer las cosas") con lemma-absence, que ya hace exactamente esto (F-LASAG-R009). La medicion lo respalda: de 162 tareas sin corregir, **157 tenian un candidato producido, juzgado y tirado**; los 318 reintentos de las 159 NOT_CORRECTED rescataron **cero** tareas y las 125 propuestas salieron TODAS al primer intento. El lazo de afuera solo sabe pedir otro candidato desde cero y solo ve uno por vez: para quedarse con el mejor de los tres hay que recordar los tres, y quien los tiene es quien los escribio.

2026-08-11 — analyst — Queda **un unico criterio absoluto: cumplir la consigna (R005)**. Ninguno de los cinco criterios de R004 —ni todos juntos— impide entregar al mejor candidato.
  why: R004 mide lo que la correccion no debe EMPEORAR; R005 mide si hizo LO QUE VINO A HACER. Un corregido fuera de rango abre OTRO diagnostico con su propio carril; uno que sigue incumpliendo reabre EL MISMO y le quema al operador una revision. Ademas un candidato que no cumple no puede ser "el mejor": no es un candidato. Alternativas (ninguno absoluto / tambien resolubilidad y traduccion) en DOUBT-CRITERIO-ABSOLUTO, opcion A: de 159 descartes, 149 fueron por longitud o edicion acotada —que no dañan al alumno— y solo 5 por resolubilidad/traduccion.

2026-08-11 — analyst — R006 sobrevive pero acotada: "correccion no lograda" = **ningun candidato resulto entregable** (ninguno cumplio la consigna o ninguno fue un cambio real). Sobre la corrida medida pasa de 159 tareas a **2**.
  why: de los dos desenlaces que R006 prohibia, "entregar lo mejor" dejo de estar prohibido y "entregar sin decirlo" sigue prohibido — pero ahora lo prohibe R014, no R006. El argumento del piso alto ("aca la correccion compite contra un ejercicio que ya funcionaba, no contra la nada") NO se cayo: sobrevive como razon para DECLARAR, no para descartar. Lo que lo volvio insostenible como veto fue el precio medido.

2026-08-11 — analyst — La longitud pasa a ser el **ultimo desempate** de R013 y no reabre la Opcion B de DOUBT-LONGITUD-NO-BLOQUEANTE (longitud como preferencia).
  why: aquella preferencia (a) gastaba reintentos persiguiendo el rango y (b) elegia sistematicamente la variante comprimida sobre la natural. Un desempate que solo se consulta cuando dos candidatos son INDISTINGUIBLES para todos los demas criterios no hace ninguna de las dos cosas: no dispara un reintento y no le gana a nada. La longitud sigue sin sumar en el recuento y sigue con declaracion y recuento propios (R011).

2026-08-11 — analyst — R015 (criterios consultables) es la pieza que evita el defecto que la arquitectura queria evitar en 2026-08-04 al poner los criterios en Java (Opcion A del architect: "reusar eval_length.sh significaba COPIARLOS al grafo nuevo").
  why: mover el lazo sin R015 obliga a que quien corrige estime por su cuenta si entra en rango o si introdujo una palabra de otro nivel — elegiria su champion con una regla de medir y el sistema lo juzgaria con otra, que es exactamente el defecto que R004 nombra como el peor posible. Con la consulta, el catalogo sigue existiendo UNA vez y del lado del sistema (cada criterio conserva su test directo), y quien corrige lo consume. Precedente de forma: FEAT-CLEX, y el `content-audit tokens` que ya usa eval_length.sh de lemma-absence.

2026-08-11 — analyst — Tres dudas nuevas: DOUBT-CRITERIO-ABSOLUTO (A: solo la consigna), DOUBT-ORDEN-DESEMPATE (A: orden por daño al alumno), DOUBT-REVERIFICACION (A: el sistema rehace los criterios deterministas —gratis— y toma del corrector los de juicio). DOUBT-SIN-CORRECCION queda **RESOLVED opcion B** (se propone con los criterios reprobados a la vista, SIN marcarla como no apta) y DOUBT-REINTENTOS se reformula: el numero sigue abierto pero el lazo ya tiene dueño.
  why: el orden de precedencia y el reparto de la re-verificacion son inferencias mias, no decisiones del usuario, y hay que poder revisarlas. Ojo con DOUBT-REINTENTOS: el dato "ningun reintento rescato nada" se produjo con un lazo que volvia a pedir DESDE CERO; un reintento que parte del candidato anterior no es el mismo experimento, asi que no alcanza para bajar el tope a 1.

2026-08-11 — architect — El lazo se va al grafo pero el ORDEN no: R013 se expresa como
  `rankKey` (clave total comparable como string) + `CandidateRanking.better`.
  why: el campeón lo conserva quien corrige, así que el orden tiene que ser consumible
  desde afuera; escrito en bash, R013 queda sin superficie Java testeable — la misma vara
  con la que se decidió la Opción A del 2026-08-04. Con rankKey la regla vive una vez, con
  test propio, y el grafo compara dos strings.

2026-08-11 — architect — `CriterionOutcome` NO gana un cuarto valor. El veredicto describe
  qué pasó al medir; la consecuencia se mudó a `CandidateAssessment`.
  why: un valor "reprobó pero no bloquea" metía política adentro de la medición y obligaba a
  cada evaluador a saber si su criterio decide — el acoplamiento que el puerto neutro evitó.

2026-08-11 — architect — R015 se apoya en un seam nuevo (`QuizInstructionCandidateAssessor`),
  no sólo en el subcomando: es el único lugar donde se arma el `CandidateAssessmentInput`.
  why: sin él el catálogo existe una vez pero el PEDIDO existe dos; basta que uno pueble
  `signalledFragments` distinto para que R015(c) se rompa sin que nada lo detecte. El
  subcomando recibe --plan/--task y toma el original del curso: nada del original viaja por
  el agente y vuelve.

2026-08-11 — architect — La medición de longitud se extrae a `CandidateLengthMeter` y el
  criterio pasa a traducir medición→veredicto.
  why: R011(b) obliga a declarar tokens+rango en la propuesta y R015(b) a que coincida con el
  analizador. Con la medición enterrada en el criterio sólo salía un `detail` de texto libre:
  o los números no llegaban, o se medían dos veces — el defecto que la regla vino a impedir.

2026-08-11 — architect — DOUBT-REVERIFICACION queda sin decidir por diseño: es cableado
  (qué evaluadores de juicio se registran en el catálogo activo), no contrato.
  why: las dos ramas funcionan con el mismo assessor y ninguna necesita otro patch.

2026-08-11 — qa-tester — Un fixture puede dejar un test SIN SUSTENTO aunque su nombre y su tag esten bien. Los dos tests de F-QICOR-R006 usaban `LENGTH_IN_RANGE` como unico criterio reprobado y esperaban NOT_CORRECTED — exactamente lo que R006 (c) y R011 (c) prohiben. Se reformulan los dos: el desenlace sin correccion solo se prueba con "ningun candidato cumplio su consigna" o "ninguno fue un cambio real".
  why: el del runner **compilaba** —nada de su firma cambio en el tramo— asi que se habria puesto verde blindando la conducta prohibida, y contradiciendo de frente al test de R011 que vive en esa misma clase ("never name the length among the failed criteria"). Un test roto se nota; un test verde que afirma lo contrario de la regla, no. Regla de trabajo que queda: cuando una regla se reescribe, revisar los FIXTURES de sus tests, no solo los nombres y los tags — y desconfiar especialmente de los que siguen compilando.

2026-08-11 — qa-tester — El renombre es tambien el mecanismo para forzar la reescritura de un CUERPO cuyo fixture quedo invalido, no solo para arreglar el nombre o el `@Tag`.
  why: `sentinel generate` nunca sobrescribe un metodo existente, asi que re-taggear o cambiar la intencion sin cambiar el nombre deja el cuerpo viejo intacto. Con nombre nuevo sale un stub nuevo y el viejo queda huerfano, visible y borrable.

2026-08-11 — developer — `QuizInstructionReviser` pierde el campo viejo `assessor: CandidateAssessor`; NO se conserva junto al nuevo `candidateAssessor`. Constructor final: 6 args (strategyRegistry, deriver, complianceChecker, subjectViewFactory, config, candidateAssessor).
  why: sentinel.yaml es inequivoco — la description de `candidateAssessor` dice literal "Reemplaza la inyeccion directa del catalogo neutro", y `requiresInject` solo declara 6 entradas. El merge aditivo de `sentinel generate` habia dejado los 7 params en el `.java` real (el viejo nunca se borra solo), pero eso es la deriva ya documentada el mismo dia, no una segunda fuente de verdad. Consecuencia no anticipada por el pedido original: los 9 handwrittenTests YA ESCRITOS de R011/R012/R014 tambien instanciaban con el `.java` real de 7 args (`assessor` mockeado y sin usar) porque se escribieron contra ESE estado transitorio; se migraron con el mismo criterio que los 4 protegidos — quitar el mock `assessor` y el argumento, sin tocar ninguna asercion.

2026-08-11 — developer — `DefaultRevisionEngineFactory` arma `QuizInstructionCandidateAssessor` con `new DefaultQuizInstructionCandidateAssessor(config.getCandidateAssessor(), config.getLemmaAbsenceProposalDeriver())` DENTRO del factory, sin agregar ningun campo a `RevisionEngineConfig`.
  why: `DefaultQuizInstructionCandidateAssessor` es package-private y vive en el mismo paquete `engine` que el factory — no hace falta exponerlo. `RevisionEngineConfig.candidateAssessor: CandidateAssessor` (el catalogo neutro, sin cambios de firma) y `lemmaAbsenceProposalDeriver` ya estaban declarados y wireados por Main.java; reusarlos ahi adentro evita declarar una dependencia nueva en sentinel.yaml solo para pasar el mismo objeto un nivel mas abajo. Confirmado contra FQicorJ001JourneyTest: `config.setCandidateAssessor(mock)` sigue siendo el mock que responde `assess(any(CandidateAssessmentInput.class))` — la firma de `CandidateAssessor` no cambio, solo la de `CandidateAssessment` que devuelve.

2026-08-11 — developer — `DefaultCandidateAssessor.assess()`: `unmetCriteria` incluye `INSTRUCTION_COMPLIANCE` cuando reprueba (necesario para que `NoAcceptableCandidateException`/R006 puedan nombrarlo), pero NUNCA `LENGTH_IN_RANGE` (R011). `satisfiedCriteria` (int) cuenta solo el catalogo de 5 (SCOPED_EDIT, LEVEL_VOCABULARY, DISTINCTNESS, FAITHFUL_TRANSLATION, SOLVABLE_SAME_KIND) — ni INSTRUCTION_COMPLIANCE ni LENGTH_IN_RANGE suman.
  why: `INSTRUCTION_COMPLIANCE` es el gate de `eligible`, no un miembro del catalogo de "no empeorar" (R004 lo dice expreso) — por eso no cuenta en `satisfiedCriteria` — pero SI es un criterio con nombre y motivo que el operador necesita ver cuando es la unica razon de que nada resulto entregable (R006 "nombrando al menos un criterio reprobado"); excluirlo de `unmetCriteria` habria dejado a R006 sin nada que nombrar en el caso mas comun (candidato `eligible=false` solo por consigna).

2026-08-11 — developer — `DefaultCandidateRanking.rankKey()`: string de ancho fijo, MSB-primero — 1 char eligible, 1 digito satisfiedCriteria (0-9), 5 bits de precedencia (SOLVABLE_SAME_KIND primero, 1=cumplido), 1 bit inRange. `better(a, b) := b > a` (comparacion de string estricta, identica a la de `tally.sh`).
  why: la cantidad tiene que dominar sobre la precedencia (R013 "por que cuenta primero la cantidad y despues la precedencia") — un bitmask de precedencia puro NO alcanza porque un candidato que solo cumple el criterio de mayor precedencia puede superar numericamente a uno que cumple 4 de menor precedencia; hace falta el digito de cantidad como segmento MAS significativo, separado y antes. Verificado a mano contra los 7 casos de `DefaultCandidateRankingTest` (incluidos los 2 que citan la decision del usuario) antes de implementar — los 7 corrieron en verde sin ajustes posteriores.

2026-08-11 — developer — ESCALACION 1 (missing_field): `QuizInstructionCorrectionContext` no tiene `planId`, y ningun otro objeto que llegue a `QuizInstructionAgentGenerator`/`QuizInstructionAgentStrategy`/`QuizInstructionReviser` lo tiene tampoco (ni `RefinementTask`, ni `QuizInstructionGenerationRequest`). El grafo `quiz-instruction-corrector` declara `planId` entre sus `dependencies` (lo necesita `assess.sh` para llamar `content-audit assess-candidate --plan <id> --task <id>`), pero no hay ningun canal para poblarlo hoy.
  why: `sourceAuditId` tiene EXACTAMENTE el mismo problema y ya esta resuelto — `QuizInstructionContextResolver` lo deja null a proposito y `DefaultRevisionEngine.revise()` lo estampa despues (`qic.setSourceAuditId(plan.getSourceAuditId())`, linea ~156-163), porque el resolver solo ve el `AuditReport`, que no carga su propio id, y el motor si tiene `planId` disponible en ese mismo punto (es el primer parametro de `revise(planId, taskId, coursePath, override)`). La correccion simetrica es un campo `planId` en `QuizInstructionCorrectionContext` + una linea `qic.setPlanId(planId)` junto a la de `sourceAuditId` — pero agregar el campo es sentinel.yaml, no mio. Mientras tanto `QuizInstructionAgentGenerator.buildInputs()` manda `"planId": ""` (nunca inventado) — `assess.sh` ya tiene manejo explicito para planId/taskId vacios (falla "no se pudo consultar el catalogo", candidato marcado inelegible, NUNCA un veredicto fabricado), asi que el peor caso es una corrida que agota intentos sin champion, no un dato falso.

2026-08-11 — developer — ESCALACION 2 (boundary_violation / missing_dependency): `AssessCandidateCmd` (audit-cli) declara `CourseElementLocator elementLocator` en su `requiresInject`, pero `DefaultCourseElementLocator` (revision-domain.engine) es la UNICA implementacion y es package-private — Main.java no puede construir una instancia. NO wireado en Main.java por esta razon; la clase esta completa e implementada (`AssessCandidateCmdTest` 2/2 verde probandola con mocks), pero el subcomando `assess-candidate` queda inalcanzable desde la CLI hasta que se resuelva.
  why: es la PRIMERA vez que algo fuera de `revision-domain` necesita `CourseElementLocator` de forma directa — todo lo demas (incluido `ConsolidatedViewBuilderConfig`, que tambien lo necesita) pasa `null` y deja que un factory INTERNO al modulo (`DefaultRevisionEngineFactory`, tambien en el paquete `engine`) lo construya puertas adentro; `AssessCandidateCmd` no es una factory, es la implementacion final, asi que ese patron no le sirve. Arreglo sugerido: `visibility: "public"` en `DefaultCourseElementLocator` (no tiene `requiresInject`, constructor trivial) — o un `CourseElementLocatorFactory` nuevo, mismo patron que `PreservationFactory`.

2026-08-11 — architect — `planId` va en `QuizInstructionCorrectionContext` y no en el
  request ni como parámetro: es el ÚNICO carrier que alcanza al reviser, a la estrategia
  y al adaptador del grafo.
  why: verificado — RefinementTask no tiene planId (7 campos) y Reviser.propose(task,
  context, before) tampoco lo recibe. Cualquier otra ubicación obliga a enhebrar un
  parámetro nuevo por un puerto compartido por todos los revisers. La simetría con
  sourceAuditId es consecuencia, no el argumento.

2026-08-11 — architect — RECHAZADO abrir `DefaultCourseElementLocator`. La resolución
  plan/tarea/contexto/original se muda de `AssessCandidateCmd` al seam (`assessTask`).
  why: Main.java:852 pasa `null` y la factory lo construye adentro — el composition root
  NUNCA ve un locator, por diseño. Abrirlo habría perforado esa encapsulación para
  sostener un error mío anterior: el comando repetía los pasos 2 y 3 de
  DefaultRevisionEngine. Con la resolución en el seam, assessTask delega en assess y la
  garantía de R015(c) pasa de disciplina a estructural. Costo aceptado: AssessCandidateCmd
  y sus 2 tests verdes se re-alojan.

2026-08-11 — architect — `CandidateAssessmentUnavailableException` como tipo propio.
  why: quien corrige tiene que separar "no se pudo evaluar" de "evalué y reprobó"; tratar
  una caída de infra como veredicto negativo fabrica un reproche que nadie midió y
  contamina el orden (R013) y la declaración (R014). Formaliza la mitigación que el
  developer ya había puesto a mano en assess.sh.

2026-08-11 — architect — TECH_SPEC.md NO se regeneró: aplicado el patch consolidado, los
  30 fences previos dejan de validar contra el architectural_patch.yaml chico.
  why: el CLI valida antes de escribir, así que el intento fue inocuo, pero sobrescribir
  habría tirado 68 KB de narrativa. Pendiente: re-proponer consolidado no-aplicable y
  regenerar, que es la forma establecida el 2026-08-04.

2026-08-11 — qa-tester — El test "el original sale del curso y no del DSL" NO es re-fixture:
  cambia de dueño. Se BORRA de `AssessCandidateCmd` y renace en
  `DefaultQuizInstructionCandidateAssessor`. El otro de la pareja ("un veredicto por
  criterio, con motivo cuando reprueba") SI es re-fixture puro y conserva su declaracion.
  why: con el seam mockeado el comando ya no puede observar de donde salio el original —
  perdio `elementLocator` y `courseRepository`. Re-fixturearlo ahi solo permitiria afirmar
  "delega sin resolver", que es un claim distinto y mas debil, y dejaria el claim original
  —incluido el ejemplo de F-QICOR-R010, la parte vacia que el DSL no puede representar— sin
  ninguna casa. El render de veredictos, en cambio, sigue siendo observable en el comando.

2026-08-11 — qa-tester — Las 6 ramas de resolucion de `assessTask` NO colapsan en un solo
  test, aunque las 6 emitan la misma señal (`CandidateAssessmentUnavailableException`).
  why: la asercion es la misma pero cada rama es un puerto distinto que puede fallar
  ABIERTO por su cuenta — `Optional.get()` → NoSuchElementException, NPE, o el peor caso,
  seguir con null y fabricar un veredicto. Lo que si colapsa es el MOTIVO: `reason` es un
  String libre y ningun criterio de R015 lo constriñe, asi que ningun test asierta su texto
  (seria asertar implementacion). La señal compartida es parte del claim: quien corrige
  tiene que poder tratar "no se pudo evaluar" de forma uniforme.

2026-08-11 — qa-tester — Los 6 casos de resolucion se anclan en R015, no en R013/R014.
  why: R013 (f) y R014 (e) hablan de un CRITERIO que no se pudo evaluar dentro de una
  evaluacion que si ocurrio; aca no hay criterio ninguno porque la consulta no llego a
  armarse. Quien constriñe es R015: su (a) define la consulta "junto a su original y su
  contexto" y su Error dice literal "se juzgo con una medicion distinta de la que aplica el
  sistema" — devolver veredictos sin haber resuelto original ni contexto es juzgar sin
  ninguna medicion. R013/R014 son la CONSECUENCIA que cita el architect (se contamina el
  orden y la declaracion), no la regla verificada.

2026-08-11 — developer — CIERRE de R015. `DefaultQuizInstructionCandidateAssessor.assessTask()` estampa `sourceAuditId`/`planId` sobre el context recien resuelto (mismo patron que `DefaultRevisionEngine.revise()`) ANTES de delegar en `assess()`.
  why: el resolver real (`QuizInstructionContextResolver`) siempre deja esos dos campos en null — por diseño, el mismo motivo documentado para `DefaultRevisionEngine` (el resolver solo ve el `AuditReport`, nunca el id bajo el que se cargo ni el plan que despacho la tarea). Sin el estampado, `CandidateAssessmentInput.sourceAuditId` (que el criterio de distincion usa para atar la comparacion al mismo analisis, F-QICOR-R004 criterio 3) habria llegado null en toda consulta real via `assess-candidate`/el grafo — invisible en la suite existente porque los 10 handwrittenTests de `DefaultQuizInstructionCandidateAssessorTest` mockean `contextResolver` para devolver contexts ya pre-poblados a mano. No es un cambio de contrato (ninguna firma se toco) — es completar la implementacion de un metodo que hasta ahora era `UnsupportedOperationException`.
  Ademas: `DefaultQuizInstructionCandidateAssessorFactory` y `DefaultRevisionEngineFactory` seguian con `new DefaultQuizInstructionCandidateAssessor(assessor, deriver)` (2 args, la firma de cuando el seam solo tenia `assess()`) contra el constructor real de 7 — corregidas ambas para pasar los 7 colaboradores; `DefaultRevisionEngineFactory` reordena la resolucion de `elementLocator` para reusar la misma instancia en el reviser y en el assessor. `AssessCandidateCmd` colapsado al constructor de 1 arg que `sentinel.yaml` ya declaraba, wireado en `Main.java` como subcomando `assess-candidate`. `QuizInstructionAgentGenerator.buildInputs()` aplana `context.getPlanId()`, retirado el `""` provisorio.
  Verificado: reactor completo 1646/1646 tests verdes, `sentinel verify` OK, `content-audit assess-candidate --help` responde exit 0.

2026-08-11 — analyst — **F-QICOR-R016 nueva**: el ejercicio corregido, leido con el hueco resuelto, tiene que describir algo que pueda pasar en el mundo real. **BLOQUEANTE por decision explicita del usuario** — segundo criterio absoluto de la feature junto con R005. Entra al catalogo de R004 como criterio 6, pero NO al orden de desempate de R013: es condicion para **competir** (junto a "cumple su consigna" y "es un cambio real"), no un escalon del orden.
  why: `Does she drink meat?` (task-876, plan 2026-08-09T22-30-12) cumplia los 5 criterios de no-empeoramiento + consigna + edicion acotada y se escribio al curso; hubo que revertirla a mano. Cada criterio se cumple LOCALMENTE y ninguno miraba la oracion entera. Tres hechos sostienen el veto: (1) no hay otro carril — ninguna medicion de la auditoria detecta el sinsentido, a diferencia de la longitud, que tiene su propio analizador y su propia tarea; (2) vetar cuesta un reintento y no una tarea — el mismo ejercicio corregido 4 veces produjo 3 salidas buenas (`drink milk`, `drink water`, `drink milk`), o sea que quien corrige ya encuentra la salida y lo que faltaba era el rechazo que lo hace reintentar; (3) la doctrina de R012 (entregar declarando) falla aca porque lo aprobado distraidamente no cuesta trabajo extra sino contenido falso.

2026-08-11 — analyst — El sentido NO entra en el orden de precedencia de R013 (sigue teniendo 6 renglones: los 5 de no-empeoramiento + longitud ultima).
  why: dos razones que se suman. (a) Misma que la consigna: si fuera el primer renglon del orden, un imposible podria ganar cuando ningun otro cumple nada. (b) Aritmetica: como TODO candidato que compite lo cumple, contarlo en `satisfiedCriteria` sumaria la misma unidad a todos y no desempataria jamas — o sea que el catalogo que CUENTA sigue teniendo 5 miembros y ninguna fixture existente de `CandidateAssessment` cambia por esto.

2026-08-11 — analyst — Interaccion R016 × R003 (edicion acotada), resuelta sin tocar R003: si respetar la marca del hueco vuelve absurda la oracion, lo correcto es cambiar el resto (`meat`→`milk`), y ese candidato **reprueba la edicion acotada** — se entrega igual con el reproche declarado (R012/R014). Reparto: `drink meat` cumple R003 y reprueba R016 → no compite; `drink milk` reprueba R003 y cumple R016 → compite y se entrega.
  why: R003 ya preveia literalmente este caso ("cuando la correccion no logra resolver la violacion sin tocar el resto, lo que corresponde es que ese candidato repruebe este criterio y se vuelva a intentar"), asi que no hizo falta relajarla. Y es el orden de R013 funcionando: la edicion acotada esta anteultima porque su daño lo paga el operador (lee mas texto), el sinsentido lo paga el alumno.

2026-08-11 — analyst — La asimetria longitud (informa) vs sentido (veta) es deliberada y quedo escrita en R011 con tabla propia. NO se propaga a la correccion de lemas ausentes: alli el criterio equivalente (Pragmatismo, F-LASAG-R007 criterio 4) sigue siendo **deseable**, por decision explicita del usuario.
  why: alli el ejercicio se genera contra la nada (rechazar devuelve la nada); aca se edita uno que ya funcionaba (rechazar devuelve el ejercicio anterior, bueno en todo salvo su consigna). El precio de rechazar es opuesto en cada lado, asi que la misma exigencia toma clasificaciones opuestas sin contradiccion.

2026-08-11 — qa-tester — El test de la excepcion por evaluador absoluto faltante se ancla en **R016**, no en R004.
  why: R004 tolera que el catalogo activo se reduzca —`judgmentEvaluators` vacia es aceptable "justamente porque ninguno de ellos decide"—, asi que su (f) "seis veredictos" no es un absoluto y no puede sostener el test. Lo que NO admite ausencia es el criterio que veta: R016 dice que de un ejercicio del que nadie pudo decir si puede pasar no se escribe nada al curso, y un catalogo construido sin ese evaluador convierte eso en aprobar por omision sobre TODA la corrida. La mitad "nombra el criterio que falta" es diagnosticabilidad y no esta en el texto de ninguna regla — queda anotada como duda para el usuario en vez de forzada a un tag.

2026-08-11 — qa-tester — R014 (f) no recibe test propio en el reviser; su cobertura honesta es el test de `unmetCriteria` en el assessor.
  why: la garantia es por construccion (lo emitido siempre es elegible, y un elegible tiene el sentido en PASSED). Un test en el reviser solo podria montar un `CandidateAssessment` incoherente —elegible y con el sentido entre los incumplidos— y eso empujaria al developer a agregar el filtro defensivo que el architect descarto expresamente ("se sostiene por construccion y no por un filtro"). Lo observable y no vacuo es el acoplamiento en el assessor: el sentido figura en `unmetCriteria` solo cuando `eligible` es false — la misma pieza que ademas habilita que R006 lo nombre.

2026-08-11 — qa-tester — La asimetria longitud/sentido va en UN test y no en dos.
  why: separadas, cada punta se lee como un caso mas de su propia regla y la relacion queda solo en la prosa de R011; juntas sobre el mismo assessor, el test falla si alguien le devuelve poder de veto a la longitud o se lo quita al sentido, que son los dos movimientos que R011 y R016 prohiben en direcciones opuestas.
