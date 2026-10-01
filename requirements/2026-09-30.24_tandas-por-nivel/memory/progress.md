# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

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
