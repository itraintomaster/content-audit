# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-03 — developer (CJUEZ) — Cómo leer el paso de los jueces cuando una prueba falla: (1) el ensayo corre TODOS los
  jueces de la tanda sobre los cuatro recortes con tope 0 explícito, y de cada juez se lee sólo el recorte de su `unit`
  (la cobertura del recorte ajeno se ignora); (2) `reusadas` y `units` salen siempre del ensayo, `faltan` del ensayo
  salvo en el lado que se pagó (ahí, de la corrida de pago) y `gastadas` de Σ `evaluatedInRun` de las corridas de pago;
  (3) el cierre toma `gastadas/reusadas/units/tope` del paso previo a la compuerta y `faltan` de un ensayo nuevo sobre la
  composición que quedó, más los pisos de las filas de la última comparación.
  why: son los tres lugares donde mezclar de qué corrida sale cada número rompe las pruebas del cierre, del servicio y del
  camino 3 del journey sin que el compilador diga nada.
2026-10-03 — developer (CJUEZ) — picocli entrega `--cap -5` a `closeBatch` como un entero negativo (la prueba de CLI lo
  muestra), así que el rechazo con «El tope tiene que ser 0 o más: --cap -5.» sale del propio `BatchCmd`, sin depender de
  que el servicio lo atrape; el servicio lo repite igual porque `BatchService.close` es el puerto y valida su propio
  pedido, con el mismo texto.
  why: evitar dos mensajes distintos para el mismo error según por dónde entre el pedido.

2026-10-03 — test-writer (CJUEZ) — Lo que `FCjuezJ001JourneyTest` da por sentado, medido con una prueba descartable
  sobre el comparador, la compuerta y la publicación de hoy (sin leer su código): (1) con jueces que dejan sin evaluar lo
  que la tanda cambió, el comparador real da UNDECIDABLE con los pisos de «curso, A1, Present Simple y Afirmativas
  regulares» sin decidir (4 por juez, de 11) y los otros 7 decididos; sin faltantes, 11 de 11 y BLIND_SAMPLE_MISSING (no hay
  muestra); (2) la compuerta pasa con un veredicto UNDECIDABLE y la tanda queda CLOSED; publicar la rechaza con «el veredicto
  de F es «UNDECIDABLE», no «pasa»: <detalle>» (`getReasons()` copia el detalle); (3) la cuenta del cierre sale del diseño:
  reusadas = lo que el libro tenía al empezar (no el ensayo final: en el camino 3 son 4, no 8), faltan = el ensayo final,
  gastadas = Σ `evaluatedInRun` de las corridas de pago, tope = su --budget o lo que queda de --cap en orden de catálogo
  (--cap 12 → 12, 8 y 4), pisos = filas con `subject` = el juez y un piso. Un juez sin aprobar nombrado con --budget 600 no
  gasta (su `queriesSpent` es 0) y no suma a `judgeQueries`.
  why: son los puntos donde una implementación razonable puede desviarse sin romper la compilación; el recorte tiene que
  conservar las etiquetas reales del nivel y del tema, o la comparación no encuentra en el libro lo que el cierre pagó.

2026-10-03 — test-writer (CJUEZ) — Lo que `DefaultBatchJudgePassTest` da por sentado de la cuenta (todo sale del diseño,
  nada del código): (1) `units` = Σ lados de `reached` de la cobertura cuyo `unit` coincide con el recorte (QUIZ → ejercicios,
  KNOWLEDGE → temas); sin cobertura (grammar), los nodos QUIZ del recorte de ejercicios con una marca del juez; el ensayo
  puede correr todos los jueces en cada recorte, pero hay que ignorar la cobertura del recorte ajeno. (2) `reason` es null
  si el juez está aprobado aunque su `standing.reason` traiga la línea del resultado («aprobado el 30/9»). (3) `queriesMissing`
  de un lado pagado sale de la corrida de pago; `queriesReused` sale del ensayo (el libro al empezar). (4) `budget` = su
  --budget, o lo que queda de --cap, o null; null para un local; las pruebas no afirman nada para un juez sin aprobar ni
  para uno al que ya no le queda tope. (5) Un juez local corrido sin política consulta sin tope (como `DefaultLocalToolGate`):
  el ensayo tiene que darle política de tope 0 explícita también a los locales. (6) Un juez sin aprobar no recibe un tope
  positivo en ninguna corrida.
  why: son los puntos donde una implementación razonable puede desviarse sin romper la compilación ni los números más
  evidentes; cada uno tiene al menos una prueba que lo atrapa (comprobado inyectando la desviación en una referencia).

2026-10-03 — test-writer (CJUEZ) — Hechos del comparador de hoy, medidos con una prueba descartable (sin leer su código),
  que las pruebas de R004 dan por sentados: (1) una marca `unevaluatedBy` en un ejercicio cambiado ya deja sin decidir
  los pisos de ese analizador (si tiene filas) y los de «algún error» en los 4 ancestros del ejercicio, y el cambio
  figura en «N cambios sin evaluar por algún analizador»; un analizador sin filas sólo deja sin decidir el cambio;
  `partiallyEvaluatedBy` o un puntaje en `scores`, solos, no hacen nada. (2) Precedencia del veredicto: FAILS >
  UNDECIDABLE > BLIND_SAMPLE_MISSING > PASSES; el detalle de FAILS y el de UNDECIDABLE no terminan en punto (el de PASSES
  y el de BLIND_SAMPLE_MISSING sí). (3) Un `NonApplicablePending` en la proyección no cambia el detalle de ningún
  veredicto.
  why: «si el detalle terminaba en punto, se le saca» y «también de lo que la tanda deja pendiente fuera del curso» no
  tienen escenario observable: un veredicto que nombra a un juez es UNDECIDABLE o FAILS (el cambio sin decidir fuerza
  UNDECIDABLE) y ninguno termina en punto. Las pruebas lo cubren comparando contra el mismo veredicto con todos los
  jueces aprobados: el detalle idéntico más el sufijo.

2026-10-01 — developer (CJUEZ) — `mvn test -pl revision-domain,audit-cli -am` saltea `audit-cli` por completo
  (SKIPPED) aunque se pase `-fae`: como `revision-domain` tiene fallos reales de test (los 57 esbozos, no sólo de
  compilación), Maven saltea cualquier módulo que dependa de uno fallado, con o sin `-fae` (`-fae` sólo sigue con
  módulos independientes del que falló). Para ver el resultado de `audit-cli` en la misma corrida hace falta sumar
  `-Dmaven.test.failure.ignore=true` (no instala nada, sólo evita que el build aborte; surefire sigue reportando cada
  fallo igual).
2026-10-01 — developer (CJUEZ) — `BatchClosing`/`BatchEstimate`/`BatchReport`/`BatchServiceConfig`: `generate` ya
  había actualizado solo las construcciones de producción que no cambiaban de método (`DefaultBatchCostEstimator`,
  `DefaultBatchReportBuilder`) con los campos nuevos al final en null/0; sólo faltaban los call-sites de las pruebas,
  mismo patrón (null para `judges`/`request`, 0/0.0 para los contadores agregados al final).
  why: evita tocar dos veces lo que el smart-merge de sentinel ya había resuelto.
