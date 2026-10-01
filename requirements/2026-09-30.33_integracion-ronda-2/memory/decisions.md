# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — integrador — Número de carpeta: 2026-09-30.33. Al empezar la .32 estaba libre; FEAT-CIERRE la tomó a las 23:36 en
  ronda1/integracion, que ya está junta en ronda2/integracion. El sentinel-baseline.yaml de esta carpeta se rehízo con el
  modelo con FEAT-CIERRE: es la foto de lo que esta integración tiene delante, y aplicar-ronda-2.sh compara contra la
  huella de huellas-de-la-cadena.txt.

2026-10-01 — integrador — B-08 (DOUBT-MLOCAL-MESA, abierta): se construye con la A, sin cambios de estructura. La mesa mide
  cada candidato con los analizadores del análisis de partida, que incluyen a los tres de B; con Ollama es gratis, y sin
  Ollama la alternativa recibe la política de la mesa (lo reservado del cupo, de a una, E-11), así que nunca pasa su tope.
  B-24 (ritmo por omisión de los jueces, 176 por hora y no 189) queda como gancho opcional de C: es configuración.

2026-10-01 — integrador — B-12 (opennlp-tools): una sola versión, la 1.9.4 de LanguageTool, fijada en el
  `<dependencyManagement>` del pom raíz. Comprobar al construir: la prueba `herramienta-real` de LanguageTool (su agrupador
  la usa en cada chequeo) y las de agent-runtime y revision-infrastructure. Plan B si algo de langchain4j la pide: excluirla
  de `sentinel-agents` en agent-runtime-infrastructure.
  why: la 2.5.4 la trae langchain4j 1.13.0 por sentinel-agents, y sólo la usa su separador de oraciones, que content-audit
  no llama; LanguageTool en cambio la carga siempre. Las dos en el mismo classpath (audit-cli) no pueden convivir.
2026-10-01 — integrador — B-09 (libro de evaluaciones): sin regla en la ronda. Medido sobre el scratchpad con 11.287 vectores
  de 768 números (76 MB): con 71ba5182, caliente 0,24 s; frío de a una consulta y un agregado, 554 s, porque cada agregado
  hace releer el archivo; con un prototipo que suma lo agregado al índice, 6,7 s. Pero B ya abre por `resolveAll`, que
  consulta todo antes de escribir y no relee: el caso de B queda resuelto. Fuera de la ronda: el juez de consigna resuelve
  de a uno (`LedgerBackedQuizInstructionComplianceChecker`) y relee su libro de 25 MB después de cada consulta nueva, unos
  0,08 s contra los segundos de cada consulta a claude -p: no vale la pena tocarlo ahora.
  why: el borrador tenía una R010 para esto; con resolveAll no hace falta, y no se suma lo que nadie de la ronda usa.
2026-10-01 — integrador — B-06 invertido (B, 30/9 noche), regla R002: el tope por omisión depende de a quién se consulta.
  El runner NO completa la política (sin --budget llega vacía); el proveedor de B usa la política sin tope de su modelo
  local o le pasa la de la corrida al runtime de C, que pone el 0 de F-JEXAM-R010 a claude -p.
  why: si el runner diera «sin tope» a los de B, sin Ollama le llegaría a claude -p: 11.873 consultas que nadie pidió.
2026-10-01 — integrador — B-10, regla R003: la corrida base deja afuera a todo analizador con una regla de modelo local, aunque
  tenga otra instantánea. No por costo de ficha (las dos de B pasaron a modelo pago por la alternativa), ni por nombre, ni
  con una marca nueva en la ficha.
  why: José: de B, en la corrida base sólo Gramática; las reglas de la ficha ya dicen qué mide con un modelo local.
2026-10-01 — integrador — R004 (F-01 a F-04): la huella de la medida deja afuera los textos de la ficha (nombre en castellano,
  pregunta, descripciones, meta), el estado del examen y los motivos de las herramientas; entra la herramienta con que
  midió (la local o claude -p) con su versión examinada.
  why: con los textos, corregir una descripción se leería como cambio de medida; sin la herramienta, Naturalidad con
  claude -p (marca por respuesta, sin número) se leería como un curso que cambió.
2026-10-01 — integrador — R008: `GRAMMAR` y `NEAR_DUPLICATES` son tipos de tarea (además de los ocho de A y C de E-01);
  `naturalness` no tiene (sólo ordena). Sus declaraciones de corrección y el arreglo por regla de grammar, en R009 y en los
  parches de TAREA y TANDA. Queda DOUBT-RONDA2-TAREAS-B para José, con la A.
  why: F-GRAMA-R005 y F-DUPAR-R004 ya dicen regla y mesa; sin tipo, F-TAREA-R001 los declararía no convertidos y ninguna
  tanda bajaría «algún error».
2026-10-01 — integrador — Riesgo de calidad del orquestador (30/9): R006 (huellas de la lista de lemas y del procesador en la
  clave del caché) y R007 (sin lista o sin procesador, analyze frena con NlpResourcesUnavailableException, sin informe). Va
  junto con E-10 = F-07 (R005, caché por oración). R007 reemplaza el modo reducido de F-NLP-R030: DOUBT-RONDA2-SPACY, con A.
  why: una corrida sin ../ittm daba A1 y A2 en 50,0 y el curso en 72,75 sin avisar, y el caché lo guardaba.
2026-10-01 — integrador — SpokenTextBuilder y DefaultSpokenTextBuilder pasan a públicas (en FEAT-RONDA2).
  why: la mesa (CatalogTopicMeter, PanelRunnerConfig) la usa desde revision-domain y era interna de su paquete: no
  compilaba. Lo encontró la revisión de visibilidad del modelo final (integrador-2/visibilidad.py); es lo único.
2026-10-01 — integrador — Lo que implementa un contrato de E o inyecta un tipo de F va en el parche que lo declara, al rebasar
  (integrador-2/rebase.py): TAREA lleva las once AnalyzerCorrection (E-03, E-04, E-05, más grammar y near-duplicates) y
  GetCmd + errorTaskBriefer (E-07); TANDA, los cinco RuleFix (E-06 más grammar) y el GoalBook de la tanda (F-10); METAS,
  el TopicTeachingResolver del comparador (F-14).
  why: `patch validate` rechaza un implements hacia una interfaz que no existe; un modify de algo que no existe entra vacío.
2026-10-01 — integrador — Tres pérdidas silenciosas del apply, corregidas en cualquier parche al rebasar: sealed sin repetir
  (bug 09: GetConsolidatedCommand en PROYU), enum sin type (bug 03: RefinementTaskStatus en TANDA) y los patrones de módulo,
  que el apply reemplaza en vez de sumar (nuevo: NOEMP, MESA, TANDA y BANDEJA dejaban a revision-domain sólo con la fábrica
  de la bandeja). Cada parche lleva la lista entera, armada contra el modelo que deja el anterior.
2026-10-01 — integrador — allowedClients: un parche aparte al final (`clientes-de-paquetes.yaml`), con la lista ENTERA de once
  paquetes: los siete nuevos de la ronda 2 (B-23 incluido), los cuatro del paso 16 de la ronda 1, revision-domain en
  offlevelwords y topicwords (F-14 y el semáforo de la mesa) y audit-infrastructure en batch y tray (sus almacenes).
  why: el jar de hoy los descarta (bug 10); con la lista entera da igual si el arreglo une o reemplaza y si entró antes el
  paso 16 de la ronda 1, que queda reemplazado.
2026-10-01 — integrador — Alineaciones E/F de las notas del orquestador que ya estaban resueltas en los textos de los paquetes,
  sin cambios: el orden de la bandeja (F-BANDEJA-R001: la muestra ciega primero y sola); qué es una tanda (sus pendientes y
  ninguna otra, F-TANDA-R006 inv. 1, y así lo definen METAS y CIEGA); la línea base fija (F-PROYU-R006, `set-baseline`
  aparte del par activo, que E mueve al publicar); y F-METAS-R003 ya dice que el largo nunca devuelve un cambio. La
  «palabra roja» de F-NOEMP-R005 es, por referencia, el hallazgo de vocabulario de gravedad media o más de F-METAS-R003.
2026-10-01 — integrador — Los 4 s de F contra los 1,7 s de E no miden lo mismo: E, un analyze con el curso ya tokenizado
  (re-medido 2,35 s); F, un get-consolidated, que lee el informe activo, mapea dos cursos y corre el motor dos veces. Lo
  que manda en los dos es tokenizar lo nuevo: 38-39 s hoy, unos 5-8 s con R005.
2026-10-01 — qa-tester (integrador) — Las pruebas de FEAT-RONDA2 van sólo sobre implementaciones que existen cuando se aplica la
  integración. Las de R009 y la parte de B de R008 caen en piezas que crean TAREA, TANDA y B: se proponen después de aplicar.
2026-10-01 — integrador — Los trece J001 de B, E y F tienen flujo y ningún paquete los ubicó: sin el qa-tester de cada paquete
  antes, `generate` aborta. FEAT-RONDA2 no trae journeys.
