# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — Control sobre una copia (foto del 29/9, 17 analizadores, el parche de
  construcción simulado sólo en la copia; scratchpad impl-E2/control): plan de 5.382 tareas (1.346 de error); la tanda de
  A1 toma 749 (621 por regla, 128 de la mesa), estima 580 consultas (0,77 h); `batch run` sin tope: 621 resueltas por
  regla, 128 sin intentar, 0 consultas; `batch close`: la compuerta pasa en 1 vuelta, veredicto UNDECIDABLE (los cuatro
  jueces sin evaluar), algún error en A1 711 → 116, vocabulario del curso 73,86 → 73,86 y de los niveles sin cambio;
  muestra ciega de 100 pares; `tray show` sólo la ciega. Sin publicar; el curso de la copia idéntico a la foto; 0
  consultas a claude. Arreglados en el camino: el pedido de regrabar por mayúsculas (NOEMP), `tray show` con el ejercicio
  entero (BANDEJA) y el nivel de cada fila del informe.
  Next: aplicar el parche de construcción (a4544a85) para que `batch` y `tray` corran fuera de la copia.

2026-10-01 — developer (construcción E) — TANDA construida: las 66 pruebas en verde (ledger 4, selección 7, costo 1,
  reglas 5, mesa 5, cierre 7, informe 5, publicación 7, servicio 16, almacén 2, BatchCmd 5, FTandaJ001 2) y F-NOEMP-J001
  (3). `batch` registrado en Main a pedido (E-09); falla con un mensaje claro hasta que se aplique el parche de
  construcción (la fábrica arma el localizador y la tabla de partes a campos). Las pruebas los leen por reflexión.

2026-10-01 — developer (construcción E) — Propuesto el parche de construcción (ver decisions.md). Hasta que se aplique y se
  genere, esperan: los cuerpos y las 3 pruebas de los dos almacenes, `CappedQueryAllowance` (2 pruebas) y la fábrica de la
  mesa, y en Main la tanda y la bandeja. Se construye todo lo demás.

2026-10-01 — qa-tester (como arquitecto) — Segunda vuelta: RuleFixResult suma rerecordings (RuleFixRerecording, clave y
  texto); AnalyzerCorrection suma concernsAudio (qué hallazgo es del audio lo dice el analizador); CorrectionBatch suma
  pendingRerecords, BatchPublication rerecords, el desenlace PENDING_RERECORDING y select recibe los pendientes. 77
  pruebas (R004 13, R005 5, R006 8, R012 5). `propose --replace` desde lo commiteado, comparado con la copia de antes (sólo
  lo pedido), `tech-spec write` (+2 fences) y `validate -i` contra M12 y contra la cadena: exit 0, 0 conflictos.

2026-10-01 — qa-tester — `patch propose` (fusión): 66 handwrittenTests (R001 8, R002 6, R003 5, R004 9, R005 4, R006 7,
  R007 5, R008 6, R009 7, R010 6, R011 3) y F-TANDA-J001 en audit-cli, paquete commands. Los cinco patrones de
  revision-domain y el enum de RefinementTaskStatus intactos. `tech-spec write` y `patch validate -i` contra M12 del
  integrador y contra la cadena con pruebas: exit 0, 0 conflictos. Sin aplicar.
  Huecos para el analista: (1) regrabar no tiene lugar en la tanda: F-DATOS-R002 inv. 2 (85 audios con «/») y F-DATOS-R004
  se arreglan «por regla» regrabando con el texto de hoy, sin cambiar el ejercicio, así que la compuerta no pide regrabar
  (F-NOEMP-R004) y RuleFixResult no lo puede declarar; y después de publicar, el audio pendiente de una tanda aparece como
  hallazgo de mesa de F-DATOS-R003 que otra tanda intentaría arreglar cambiando el ejercicio. (2) R005 no nombra el
  desenlace «sin alcance declarado», que R010 cuenta: sumarlo dejaría probar acá lo que hoy espera (F-TAREA-R006).

2026-10-01 — qa-tester (como analista) — REQUIREMENT.md: F-TANDA-R011 (abrir fija las metas), pedido del qa-tester de F
  vía el orquestador; `requirement validate` → [OK]. Sin cambios de estructura: el servicio ya recibe el GoalBook (F-10).

2026-09-30 — architect — Parche (7 altas, 7 cambios, 0 conflictos) y TECH_SPEC (13 fragmentos) en exit 0. Medido para las
  notas: `analyze` con el curso en caché 2,35 s y un lanzamiento de spaCy 2,2 s (scratchpad arq-E/medir). Next: el orquestador revisa; se aplica después de la ronda 1 integrada y de F, en el orden TAREA → NOEMP → MESA → TANDA → BANDEJA; luego `definitions`, `feature sync`, @qa-tester. Ganchos en tablero/ronda2/ganchos-E.md.

2026-09-30 — analyst — REQUIREMENT v2 con José, 30/9 y alineado con F: encargos al abrir, línea base fija, la tanda son sus propias pendientes; DOUBT-SACAR-ENCADENADO abierta. `requirement validate` → [OK].

2026-09-30 — analyst — REQUIREMENT.md v1 de FEAT-TANDA: 10 reglas, 1 journey, sin dudas propias (las suyas viven en MESA y
  BANDEJA). Medido: `analyze` completo sin juez 38 s con una oración cambiada, 1,7 s con el curso ya tokenizado.
  `requirement validate` → [OK].
  Next: José contesta las dudas del paquete E; el integrador suma la carpeta a `definitions` y corre `feature sync`
  (no se tocó sentinel.yaml ni el glosario); después @architect, que parte de las notas para el arquitecto del informe.
