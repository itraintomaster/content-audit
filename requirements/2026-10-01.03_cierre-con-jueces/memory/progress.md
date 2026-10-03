# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-03 — orquestador (CJUEZ) — FEAT-CJUEZ terminada en `ronda2/cierre-con-jueces` (13 commits sobre
  `ronda2/integracion` 780f1203, sin push): 59 pruebas de F-CJUEZ en verde; suite completa 3.190, 0 fallas, 1 apagada
  (spaCy); `sentinel verify` OK. Control de punta a punta sin cupo sobre una copia (foto del 29/9, 20 analizadores,
  exámenes de B y C de hoy, claude falso, Ollama local), en el scratchpad de la sesión (`control/`, guiones `fase1.sh`,
  `fase2.sh`, `fase2b.sh`; `vuelta1/` es la anterior al arreglo de R004): la tanda A1 cambia 611 ejercicios en 33 temas
  tras las reglas; `batch estimate` pide 2.444 consultas de translation-sense y 66 de mini-theory (2.510, ~3,3 h a 4 a
  la vez); el cierre sin tope da UNDECIDABLE con 24 pisos sin decidir y termina en «juez sin aprobar: Consigna que no
  se cumple, Opción múltiple mal armada, Más de una respuesta válida, Ejercicio casi repetido» (grammar aprobó su
  examen el 1/10 a la tarde); `tray publish` se rechaza copiando eso; 0 consultas, curso igual a la foto, historial
  igual. La primera vuelta encontró dos huecos de R004 (near-duplicates no se nombraba; la bandeja rechazaba sólo por la
  muestra ciega): dos pruebas más (qa-tester), sus cuerpos y el arreglo, en 762d8815..f97182bd.
  Next: que `tray show` nombre a los jueces sin aprobar mientras la muestra ciega está abierta (FEAT-BANDEJA, no
  pedido); una corrida con consultas reales (`batch close --budget …`) con los jueces aprobados.

2026-10-03 — developer (CJUEZ) — Los 2 arreglos de R004 del control de A1 hechos, sin tocar las pruebas: `UnapprovedJudges.reading`
  lee también las marcas del propio ejercicio para un juez de temas (near-duplicates), y `DefaultTrayService.publish` copia el
  «juez sin aprobar: …» del veredicto del último cierre en el rechazo por la muestra ciega. Las 59 de F-CJUEZ en verde y las
  suites completas sin fallas: revision-domain 711 y audit-cli 540, reactor en BUILD SUCCESS.
  Next: en la bandeja, `Tray.blockers` con la muestra abierta sigue diciendo sólo «falta la muestra ciega» (la
  `DefaultTrayAssemblerTest` lo fija sin jueces); si José quiere verlo también al mirar la bandeja y no sólo al publicar,
  falta ese bloqueo.

2026-10-03 — test-writer (CJUEZ) — Los 2 esbozos nuevos de F-CJUEZ-R004 (huecos del control de A1) implementados:
  `DefaultBatchComparatorTest` (near-duplicates reprobado, cobertura en KNOWLEDGE, marcas en los ejercicios) y
  `DefaultTrayServiceTest` (publicar desde la bandeja copia «juez sin aprobar: …» del último cierre). Compilan; fallan sólo
  por el comportamiento que falta (el comparador deja los pisos sin decidir pero no nombra al juez; la bandeja rechaza con
  «falta la muestra ciega» sin nombrarlo); las otras 82 de las dos clases siguen verdes.
  Next: el developer: para un juez de temas el comparador tiene que leer también las marcas del nodo del ejercicio, y la
  bandeja tiene que copiar el «juez sin aprobar: …» del veredicto del último cierre en su rechazo; ver fix-log.

2026-10-03 — qa-tester (CJUEZ) — Parche sólo de pruebas propuesto (--replace) y validado: 2 handwrittenTests nuevas de
  F-CJUEZ-R004 por los huecos del control de A1. DefaultBatchComparator: near-duplicates reprobado, con cobertura por temas
  y sus marcas (puntaje o sin evaluar) en los ejercicios que compara, bloquea sus pisos sobre lo que cambió y el veredicto lo
  nombra («Ejercicio casi repetido»). DefaultTrayService: publicar desde la bandeja copia «juez sin aprobar: …» del veredicto
  del último cierre aunque la muestra ciega no esté respondida. propose y validate: 0 altas, 3 cambios, 0 bajas, 0 conflictos.
  Next: el orquestador aplica sin --as y genera. generate saltea el esbozo del comparador (`hasMatchingTaggedTest`: el archivo
  ya tiene "FEAT-CJUEZ" y "F-CJUEZ-R004" entre comillas) y hay que traerlo del generado fresco; el de la bandeja sale solo (la
  clase no tenía R004). `sentinel verify` no detecta el faltante (de las clases de prueba sólo mira que exista el archivo):
  verificar con grep del nombre del método.

2026-10-03 — developer (CJUEZ) — Comportamiento implementado: las 57 pruebas de F-CJUEZ en verde (54 `@Tag("FEAT-CJUEZ")`
  y los 3 caminos de F-CJUEZ-J001) y las suites completas de revision-domain (709) y audit-cli (540) sin una falla, con
  el reactor completo en BUILD SUCCESS (`-pl revision-domain,audit-cli -am test`, ~5 min). Producción tocada:
  `DefaultBatchJudgePass`, `DefaultBatchCloser`, `DefaultBatchService`, `DefaultBatchReportBuilder`,
  `DefaultBatchCostEstimator`, `DefaultBatchComparator` (sólo R004) y `BatchCmd`; nuevos, package-private y sin
  @Generated: `BatchChanges` («lo que cambia», lo usan el paso y el servicio) y `ChangedCourseCuts` (los cuatro recortes)
  en `batchengine`, y `UnapprovedJudges` en `comparisonengine`. Sin tocar sentinel.yaml, contratos @Generated, pruebas ni
  `.sentinel/agents/`; sin commit.
  Next: una corrida en vivo de `batch estimate` y `batch close --budget …` sobre una tanda real: ningún modelo real se
  consultó (las pruebas usan jueces de mentira y el `claude` falso); lo que no queda cubierto es la lectura de las
  coberturas de los jueces reales (unidad, `reached`, `cost.pendingConsultations`) y el costo de mapear los cuatro
  recortes de A1 con spaCy (la estimación y el cierre lo hacen una vez cada uno, el cierre dos).

2026-10-03 — test-writer (CJUEZ) — Los 3 caminos de `FCjuezJ001JourneyTest` (path-1 ensayo, path-2 bloqueo, path-3
  informe) implementados. Compilan; los 3 fallan sólo por comportamiento que falta (`BatchEstimate.getChangedQuizzes()`
  da 0: ni el servicio ni el cierre llaman todavía al paso de los jueces, y el comparador no nombra al juez sin aprobar).
  Servicio armado con `DefaultBatchServiceFactory` + `BatchServiceConfig` (con `analyzerCatalog`); cierre, compuerta,
  publicación y comparador REALES; a mano, las tiendas, la muestra ciega y el proyector (mide el curso de partida y el
  compuesto con los 3 jueces a tope 0). Banco nuevo `CjuezJourneyBench` (batchengine, package-private, sin @Generated):
  motor real, 3 jueces de mentira (quiz-instruction, translation-sense con 2 evaluadores, mini-theory por tema) con libro
  por contenido, consultas registradas y examen por juez; el libro arranca con translation-sense juzgando todo el curso.
  Validado contra una referencia descartable escrita del diseño (paso de los jueces, y lo que falta en servicio y cierre,
  más el sufijo del veredicto), guardada fuera del repo en el scratchpad de la sesión: 3/3 verdes; con un recorte que
  pierde la etiqueta del nivel fallan los 3.
  Next: el developer implementa `DefaultBatchJudgePass`, el paso 1 y el ensayo final del cierre, `estimate()/close()/report()`
  del servicio y el nombre del juez sin aprobar en el comparador; el fix-log dice qué dan por sentado los caminos.

2026-10-03 — test-writer (CJUEZ) — Los 23 esbozos de `DefaultBatchJudgePassTest.java` (R001 7, R002 4, R003 9, R004 1,
  R005 2) implementados. Compilan; los 23 fallan sólo por `UnsupportedOperationException` (la clase no está implementada).
  Banco nuevo `JudgePassBench` (batchengine, package-private, sin @Generated): el motor real con un catálogo de jueces de
  mentira (libro de veredictos por lo que lee cada uno con sus ancestros, examen por juez, falla de servicio, Ollama sí/no
  para naturalness, grammar como herramienta local sin cobertura), el curso chico de los casos (13 ejercicios) y un curso
  grande (`largeCourse`) para los topes literales 500, 1000/1200 y 600.
  Validado contra una implementación de referencia descartable escrita del diseño (G), ya borrada: 23/23 verdes; 23
  desviaciones inyectadas (una por vez, salvo un par) (recortes sin ancestros o sin temas enteros, sin tope 0 a los locales, pagar a un
  local o a un sin aprobar, tope por lado, cap reservado, seguir tras la falla, etc.) las atrapa al menos una prueba; tres
  variantes legítimas (ensayo por juez, pagar primero el después, saltear el lado sin faltantes) pasan.
  Next: el developer implementa `DefaultBatchJudgePass` (estimate/judge/judges) según el diseño; el fix-log dice qué dan
  por sentado las pruebas.

2026-10-03 — test-writer (CJUEZ) — Los 6 esbozos `@Tag("FEAT-CJUEZ")` de `DefaultBatchComparatorTest.java` (R004)
  implementados. Compilan; fallan los 6 por comportamiento que falta (los pisos del juez sin aprobar siguen MET, el
  detalle no termina con «; juez sin aprobar: …», el cambio no queda sin decidir para él); las otras 53 siguen verdes.
  Auxiliares nuevos dentro de la clase, sin tocar lo que usan las pruebas viejas: `Foto.judge/judged/judgedEverywhere/
  partially/coverage/toolRun`, `Mesa.catalog`, `CARDS_WITH_JUDGES`, `coverage(...)`, `toolRun(...)`, `hoy(...)`,
  `mesaOfJudges(...)`, `assertFloors(...)` y los escenarios `compareVegetables…`/`compareTheA1Batch`.
  Next: el developer implementa R004 en `DefaultBatchComparator` (jueces sin aprobar de la raíz proyectada, lectura por
  marcas en el nodo del ejercicio o, para un juez de temas, en el del tema, nombres al final del detalle); ver fix-log
  para lo que las pruebas dan por sentado del comparador de hoy.

2026-10-01 — test-writer (CJUEZ) — Los 9 esbozos `@Tag("FEAT-CJUEZ")` de `DefaultBatchServiceTest.java` (R001 1,
  R002 4, R003 4) implementados. Compilan; 8 fallan por assertion + 1 NPE (`BatchEstimate.getJudges()` null), todas
  por el puente sin comportamiento nuevo (nada llama todavía a `judgePass`). Resto del archivo (16 pruebas) sigue
  verde. Nuevo doble `ScriptedJudgePass` (batchengine, package-private) graba estimate/judge/judges y devuelve
  cuentas/jueces preparados; se agregó un campo mutable `Service.judgePass` (default `NoJudgesPass`) para poder
  inyectarlo sin tocar las pruebas existentes.
  Next: developer debe implementar `DefaultBatchJudgePass` (hoy 3x UnsupportedOperationException) y cablear sus
  llamadores en `DefaultBatchService.estimate()/close()` y `DefaultBatchCloser.close()` (validación de --budget/--cap,
  changedQuizzes, judgeQueries/judgeHours) para que estos 9 pasen a verde.

2026-10-01 — developer (CJUEZ) — Puente de compilación tras `sentinel generate`: todo el reactor compila (main y
  test), 18 archivos tocados (6 de producción + 12 de pruebas) más `NoJudgesPass.java` nuevo (fixture de prueba,
  package-private, sin @Generated). `DefaultBatchJudgePass` queda con sus 3 `UnsupportedOperationException`, sin
  llamador todavía. 3126/3183 pruebas del reactor completo pasan; fallan exactamente los 57 esbozos de F-CJUEZ (54
  `@Tag("FEAT-CJUEZ")` + 3 de `FCjuezJ001JourneyTest`), todos con «Not implemented yet» y nada más.
  Next: implementar `DefaultBatchJudgePass` (estimate/judge/judges) y el paso 1 del cierre en `DefaultBatchCloser`
  (hoy `close(batch, startCourse, request)` ignora `request`); recién ahí los 57 esbozos dejan de tirar
  UnsupportedOperationException.

2026-10-01 — qa-tester (CJUEZ) — Parche sólo de pruebas propuesto (--replace) y validado: 54 handwrittenTests
  (R001 9, R002 10, R003 20, R004 8, R005 7; 23 en DefaultBatchJudgePass, el resto en el cierre, el servicio, la
  publicación, el informe, DefaultBatchComparator, DefaultTrayService y BatchCmd) y F-CJUEZ-J001 en revision-domain,
  com.learney.contentaudit.revisiondomain.batchengine. propose y validate: 0 altas, 11 cambios, 0 bajas, 0 conflictos.
  Next: el orquestador aplica sin --as y genera; verificar en sentinel.yaml los 54 nombres y la ubicación del journey,
  y que generate materialice los 54 stubs y FCjuezJ001JourneyTest (3 caminos).

2026-10-01 — architect (CJUEZ) — Parche propuesto y validado (0 conflictos) y TECH_SPEC.md escrito, sin pruebas.
  Next: el orquestador aplica y genera; después, @qa-tester declara las pruebas de R001 a R005 y J001.
2026-10-01 — analyst (CJUEZ) — REQUIREMENT.md escrito: R001 a R005, F-CJUEZ-J001 (5 nodos, 3 caminos) y
  DOUBT-CJUEZ-VERSION (abierta, rige A). `feature validate`: OK. `requirement validate`: OK, con un solo aviso
  propio, el término de glosario «Translation», que sale del nombre `translation-sense` (FEAT-FILAS tiene el mismo).
  Next: el arquitecto propone el parche sobre el diseño compartido. José contesta DOUBT-CJUEZ-VERSION y confirma las
  interpretaciones anotadas en decisions.md.
