# Fix Log — FEAT-EVCOST

2026-07-31 — test-writer — R002 tercer test (sesion): en vez de assertSame sobre el mapa, se usa ArgumentCaptor + assertEquals para fingerprinter.fingerprint(...) y evaluator.evaluate(...).getContent(), ambos comparados contra el mapa original.
  why: assertSame (identidad de referencia) es mas fragil de lo que exige R002 — el criterio de aceptacion habla de contenido, no de instancia de objeto; una implementacion que hiciera una copia defensiva equivalente seguiria siendo correcta y no deberia romper el test.

2026-07-31 — test-writer — R001 segundo test y R006: policy con maxNewEvaluations=0 para forzar PENDING determinista y aislar la mitad "no reuso" sin mezclarla con el comportamiento de nueva evaluacion (que es R003).
  why: sin aislar el presupuesto, el resultado de "no reuso" quedaria acoplado a si la sesion decide evaluar de nuevo o no, hasta el punto de volver ambiguo cual regla esta realmente fallando si el test rompe.

2026-07-31 — test-writer — R009 (ambas mitades): la corrida se modela con 3 contenidos y la MISMA falla en el contenido 1 (OUTPUT_UNUSABLE vs EVALUATOR_UNAVAILABLE), discriminando solo por EvaluationFailureKind; el corte-de-servicio se envuelve en assertDoesNotThrow para no dejar pasar la implementacion ingenua que propaga la excepcion.
  why: el criterio (c) del enunciado (nada se registra) esta delegado a R005 por decision de @qa-tester — no se duplica aqui a proposito.

2026-07-31 — test-writer — R004 mitad dificil (entrada truncada), FileSystemEvaluationLedgerTest, sin leer produccion: append() dos records reales via FileSystemEvaluationLedger, medir Files.size() del unico archivo regular bajo el @TempDir tras el 1ro y tras el 2do, y FileChannel.truncate() a un punto intermedio entre ambos tamaños. Deja la 1ra linea intacta y la 2da a medio escribir sin necesitar conocer el formato/nombre del archivo.
  why: simula fielmente "interrupcion abrupta a mitad de la 2da entrada" sin inventar formato ni leer FileSystemEvaluationLedger.java; localizar el archivo por Files.walk (unico regular file esperado) evita acoplarse a un nombre de archivo que es detalle de implementacion.

2026-07-31 — test-writer — EvaluationResolutionKind.FAILED vs PENDING en FEvcostJ002JourneyTest path-2/path-4: FAILED = contenido que SI fue consultado y el evaluador no emitio veredicto (OUTPUT_UNUSABLE o EVALUATOR_UNAVAILABLE); PENDING = contenido NUNCA consultado (tope de presupuesto o la sesion ya dejo de consultar). Deducido de las descripciones de campos de EvaluationCoverage (failed/pending), no de leer DefaultEvaluationSession.
  why: la regla R009 en prosa dice "el contenido i queda pendiente" para ambos casos, ambiguo entre los dos enums; el criterio del EvaluationCoverage.failed ("el evaluador no llego a pronunciarse sobre ellos EN ESTA CORRIDA") desambigua a favor de FAILED para el contenido efectivamente consultado.
