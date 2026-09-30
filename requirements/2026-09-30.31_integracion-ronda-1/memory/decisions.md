# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
