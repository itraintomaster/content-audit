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
