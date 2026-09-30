# Progress

Current state, last action, next step. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (lo compartido) — FEAT-RONDA1 construido en ronda1/integracion (0b0ee22f..931f7d1b): el
  renderizador y get student-view, el nombre en castellano, los conteos por gravedad y por regla, el ejercicio a medias,
  la corrida base con tope 0, los lugares tipados, la miniteoría y la clave del audio, las opciones de analyze por juez y
  de consultas a la vez, la configuración de los jueces nuevos, get word-lights, el formateador de stats, off-level-words
  cubierto por LEMMA_ABSENCE y las listas nuevas del diff. Main construye todo lo de la ronda; los nueve proveedores
  nuevos, `exam` y el formateador de recurrencia quedan en líneas comentadas, separadas, para que cada paquete encienda las
  suyas. Tests: 1.821 → 1.863 en verde (39 de las 43 de FEAT-RONDA1 y 3 de ArchUnit), 507 apagados: los 503 stubs de A,
  C y D (@Disabled("Stub de FEAT-X (paquete Y), sin implementar")) y 4 de FEAT-RONDA1 que esperan a los paquetes;
  `mvn -o test` BUILD SUCCESS con `sentinel verify` OK. Control sobre una copia: 11.760 nodos con los mismos puntajes,
  el mismo orden de claves y los mismos diagnósticos que 2026-09-30T11-54-02; vocabulario 73,92 % y 96,54 · 94,16 ·
  79,45 · 63,51 (las bandas 8 · 6 · 4 · 3 son de FEAT-POCOV); plan de 4.036 tareas idéntico tarea por tarea; el curso,
  886 archivos idénticos; get student-view de Afirmativas regulares, de In time u on time y de Adjetivos atributivos.
  Próximo: A, C y D construyen lo suyo; al terminar, encienden su línea en Main, sacan sus @Disabled y corren las 4 de
  FEAT-RONDA1 que esperan (MainTest, AnalyzeCmdTest y DefaultAuditRunnerTest ×2).

2026-09-30 — qa-tester (integrador) — 43 handwrittenTests para las 8 reglas de FEAT-RONDA1 en 14 implementaciones,
  con `patch propose` en modo fusión (exit 0; comparado sin las pruebas: 0 pérdidas) y TECH_SPEC con dos secciones más
  (16 fences, exit 0). Paso 16: `clientes-de-paquetes.yaml` (propose -o, __proposal__) devuelve los allowedClients de
  findingengine, judgingengine, offlevelwords y topicwords; con el jar de hoy no entra (queda PENDIENTE en el script).
  Simulado: 16 de 16 `patch validate` con 0 conflictos; generate no aborta; 499 pruebas trazadas; con el PatchApplier
  arreglado, 4 de 4 paquetes con sus clientes y las 4 reglas de ArchUnit.

2026-09-30 — integrador (analista + arquitecto) — REQUIREMENT.md (FEAT-RONDA1, 8 reglas, sin journeys), `patch propose` exit 0
  contra el sentinel.yaml real (8 altas, 17 cambios, 0 conflictos) y `tech-spec write` exit 0 (15 fences). Los 14 parches
  de A, C y D rebasados con `patch propose --replace`, cada uno contra el modelo que deja el anterior: 14 exit 0; sus
  TECH_SPEC validan (9 reescritos con `--replace`). Nada aplicado.
  Simulado en memoria (scratchpad/integrador-1, librería del jar): los 15 `patch validate` del orden pasan con 0 conflictos;
  sobre el modelo final generate no aborta, 0 choques de nombre, 0 tipos sin resolver, 456 pruebas de la ronda trazadas,
  82 clases de stubs y 14 journeys nuevos.
  Próximo: el orquestador corre scratchpad/integrador-1/aplicar-ronda-1.sh, después generate con la ronda entera; @qa-tester
  diseña las pruebas de las 8 reglas; construye primero la integración (lo compartido y Main) y después cada paquete.
