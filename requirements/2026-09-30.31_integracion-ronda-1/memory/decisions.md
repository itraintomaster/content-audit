# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer — Main construye todo lo de la ronda y registra sólo los ocho de hoy: un proveedor sin implementar
  tira en analyzerName(), el catálogo lo lee al arrancar y frenaría cada comando. Las nueve líneas de registro, `exam`
  (ExamCmd no tiene todavía su @Command) y el formateador de recurrencia quedan comentadas, cada una separada por una
  línea en blanco para que dos paquetes no choquen al encender las suyas.
  why: registrar sin implementar rompe el analyze de hoy, que es lo que el orquestador pidió no romper.
2026-09-30 — developer — En Main, la comparación del corrector y los cinco jueces nuevos se piden a su fábrica en el primer uso
  (la tabla la carga el primer analizador que compara; el juez, la primera consulta); el registro de grabaciones es uno
  vacío mientras no exista `<db>/grabaciones.json`, y `<db>` es la carpeta del curso por omisión, porque el catálogo se
  arma antes de que el comando lea su curso.
  why: que ningún comando pague ni falle por piezas que no usa, y que cada paquete no tenga que tocar Main para eso.
2026-09-30 — developer — Largo de oración: reglas `shorter-than-goal` y `longer-than-goal`, en ese orden (R005 inv. 4). El
  renderizador numera los huecos desde 0; la pista de un hueco es el primer paréntesis entre él y el hueco siguiente (o,
  para el primero, el último antes de él); las respuestas se cortan en «|», se recortan y se descartan las vacías; en la
  opción múltiple el hueco acepta sólo la etiqueta de la correcta, tal cual. `finishedSentence` sale del conversor del
  curso y da null en una opción múltiple sin correcta.
2026-09-30 — developer — findingCounts se publica en tema, topic, nivel y curso, no en el ejercicio; una fila por analizador que
  corrió y tipo de nodo que evalúa, sólo los de ese nivel para abajo, y byRule con todas las reglas de la ficha. El
  colector rechaza un nodo a medias sin puntaje o a medias y sin evaluar a la vez.
2026-09-30 — developer — `--reevaluate nombre=ids`, `--reevaluate-file nombre=archivo` (se resuelven como el conjunto del juez de
  consigna) y `--concurrency N`, que va a la política de cada juez que la corrida declara; sin tope un juez no consulta
  (F-JEXAM-R010), así que no hace falta una política sólo para llevar N.
2026-09-30 — developer — off-level-words cubierto por LEMMA_ABSENCE: una tabla en DefaultRefinerEngine (COVERED_BY_TASK_OF),
  porque AnalyzerPlanBinding no tiene cómo decirlo; qué tarea hace cada analizador nuevo es del paquete E.
2026-09-30 — developer — stats analyzer muestra la vista detallada sólo de los analizadores que Main pone en su propio mapa
  (hoy ninguno; lemma-recurrence cuando RECUR construya su formateador): con el mapa de analyze, stats de coca o de
  quiz-instruction cambiaría de salida.

2026-09-30 — integrador — allowedClients de paquete: el apply del jar los descarta al crear (stripPackageAnnotations) y al
  modificar (mergePackage se queda con los de antes); findingengine ya los perdió en 0.3. No hay camino por el CLI hasta
  arreglar PatchApplier (dos líneas, borrador del bug 09): el paso 16 queda preparado y el script lo corre sólo si una
  prueba en memoria con el jar instalado los conserva.
  why: editar sentinel.yaml a mano está prohibido; arreglar antes o después de la ronda da el mismo modelo final.
2026-09-30 — qa-tester (integrador) — Las pruebas de FEAT-RONDA1 van sólo sobre implementaciones que existen cuando se aplica la
  integración (las suyas y las del sentinel.yaml real): una prueba sobre una pieza de un paquete entraría como
  implementación vacía, porque la integración se aplica primero.

2026-09-30 — integrador — Juez de opción múltiple: queda el contrato de C, en dos tiempos (request en onQuiz, resolveAll y
  judgmentOf en onCourseComplete), en el paquete choicealternative de ESTE parche. A cede: salen sus dos interfaces y cuatro
  modelos, cambian dos inyecciones, un campo del diagnóstico y el nombre de la fábrica «sin juez».
  why: judge() por ejercicio consulta de a uno (José eligió 4 a la vez, F-JEXAM-R006) y AlsoValidOption no traía gravedad ni
  clave invertida; en la integración ningún paquete depende del otro.
2026-09-30 — integrador — Los proveedores de C y D pasan a implements AnalyzerProvider en SUS parches (C-01 a C-03, D-01), no acá:
  un modify en la integración de algo que todavía no existe entraría como elemento vacío.
2026-09-30 — integrador — CorrectorMatcher sigue en RESPC (bloque propio, como dicen los ganchos), con GapContext en accepts y un
  sameAnswer con contexto para opciones de un hueco; por eso A se aplica antes que C.
2026-09-30 — integrador — Ningún DiagnosisKind nuevo en la ronda 1: el plan declara los nuevos como sin convertir y off-level-words
  como cubierto por LEMMA_ABSENCE (F-RONDA1-R008). Los tipos de tarea son del paquete E.
2026-09-30 — integrador — La etiqueta de ejercicio del resumen la pone el almacén con el renderizador; AuditableQuiz.label no cambia
  porque el juez de consigna la lee como título (huella de 13.000 veredictos).
2026-09-30 — integrador — get student-view lee el análisis activo o el último; el texto del audio sale del registro de DATOS o
  queda vacío. Los conteos por gravedad y regla son una lista aparte (findingCounts), para las dos familias.
2026-09-30 — integrador — Bandas 8·6·4·3 por omisión (José, 30/9 noche): se renombró la prueba del cargador en POCOV (misma regla).
  LemmaCountConfig: el parche de POCOV lo modificaba con sealed false y el apply lo habría abierto; ahora repite sealed true.
