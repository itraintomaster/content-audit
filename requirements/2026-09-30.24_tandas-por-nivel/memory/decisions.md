# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — Lo que se decidió construyendo la tanda:
  - selección: una tarea de un tipo de vocabulario (SENTENCE_LENGTH, LEMMA_ABSENCE, COCA_BUCKETS, LEMMA_RECURRENCE y los
    dos largos de tema) entra sólo si cubre un hallazgo de errores, aunque su hallazgo vaya por regla o a la mesa (R001);
  - publicar: la preservación de cada ejercicio se mira contra un curso donde sólo cambió él (si no, con dos propuestas,
    los cambios de una eran violaciones de la otra); sacar una regla al publicar obliga a comparar de nuevo;
  - cerrar otra vez no sortea otra muestra ciega: una por tanda;
  - el arreglo de José (WRITE_FIX de la bandeja) llega como RETURN_TO_PANEL con candidato: la tanda lo mide y lo juzga con
    su mesa y las tareas del ejercicio, a lo sumo 4 consultas [ASSUMPTION]; «no pasa» de un candidato en desempate gasta un
    intento y deja sin ganador al llegar al tope; aceptar un candidato conserva el veredicto de su compuerta (y su pedido
    de regrabar); TASK_FROM_TIE abre la tarea del desempate en el plan y en la tanda.
  why: lo pedían las reglas R001, R008, R009 y FEAT-BANDEJA R002/R003; la tanda es la única puerta de las decisiones.

2026-10-01 — arquitecto (construcción E) — Parche de construcción, propuesto y validado contra el sentinel.yaml de
  ronda2/impl-correccion (0 conflictos), SIN aplicar: `architectural_patch.yaml` de esta carpeta. (1) `FileSystemBatchStore`
  y `FileSystemTrayDecisionStore` reciben `baseDir: Path`, como los otros diez almacenes (E-09 lo suponía); (2)
  `DefaultCourseElementLocator` y `DefaultCorrectionScope` pasan a públicas dentro de revision-domain, para que
  `DefaultBatchServiceFactory` las arme cuando el registro trae null, como las fábricas de revise, approve y la vista
  consolidada con el localizador; (3) `CappedQueryAllowance` recibe `cap: Integer` y `queriesAtOnce: int`, sin los que
  `PanelRunnerFactory.allowance(cap, C)` no lo puede configurar. Sin contratos nuevos.
  why: ni Main ni batchengine pueden construir un CourseElementLocator ni un CorrectionScope (internas de engine y
  preservationengine), y sin ruta los almacenes escribirían donde está parado el proceso, no en --workdir.
  El TECH_SPEC no lleva la sección: `tech-spec append` exige que todas las fences de la carpeta estén en el parche nuevo.

2026-10-01 — qa-tester (como arquitecto) — Lo pendiente de regrabar vive en la publicación de cada tanda
  (BatchPublication.rerecords): la siguiente los recibe al abrir (select) y no toma la tarea del audio. Sin almacén nuevo:
  un pedido deja de estar pendiente cuando data-coherence ya no marca ese audio.
  why: la tanda ya guarda sus publicaciones; un registro aparte de regrabaciones se desalinearía del de grabaciones (DATOS).

2026-10-01 — qa-tester (como analista) — El audio sigue al ejercicio (orquestador, 1/10): R004 inv. 4 (un arreglo por
  regla puede ser sólo un pedido de regrabar), R006 inv. 3 (esa tarea queda «pendiente de regrabar», no resuelta), R010
  cuenta las pendientes de regrabar y R012 nueva: publicar deja pendientes los pedidos, y una tanda no toma, ni por regla
  ni para la mesa, una tarea del audio pendiente; la bandeja la muestra.
  why: sin R012, el audio viejo de un ejercicio que una tanda cambió volvía como hallazgo de mesa (F-DATOS-R003) y otra
  tanda lo «arreglaba» cambiando el ejercicio. Las 7 diferencias de audio de hoy siguen en la mesa: el ejercicio está mal.

2026-10-01 — qa-tester (como analista) — R005 nombra el desenlace «sin alcance declarado», que R010 ya contaba: una tarea cuyo
  tipo no declara su alcance no se intenta (F-TAREA-R006). Decisión del orquestador del 1/10.
  why: así la parte de F-TAREA-R006 que vive en la mesa se prueba en la tanda, con su regla, y no espera al apply.

2026-10-01 — qa-tester — F-TANDA-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands` (2 caminos).
  why: sus pasos son batch open, estimate, run y close y el OK de tray publish; BatchCmd y TrayCmd son package-private ahí,
  como ExamCmd en F-JEXAM-J001, que es el molde del verbo.
2026-10-01 — qa-tester — El arreglo por regla de grammar (GrammarRuleFix) es de F-RONDA2-R009 y espera al apply. Lo que
  F-NOEMP-R008 pide a quien devuelve (un intento menos, sin intentos a la bandeja) se prueba con R006, que pasa la
  compuerta de F-NOEMP-R005 a R008.

2026-10-01 — qa-tester (como analista) — R011 nueva: abrir una tanda fija sus metas (la mitad de F-METAS-R006 que cumple
  la tanda); entra en la puerta de `abrir` de J001. Invariantes: fija las de la declaración al abrir, la comparación evalúa
  contra ellas y un pedido de abrir rechazado no fija nada. AUTO_VALIDATED: sale de F-METAS-R006, validada por José.
  why: el qa-tester de F no la puede probar en METAS: el que abre es DefaultBatchService, que no existe al aplicar METAS.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): BatchServiceConfig y DefaultBatchService reciben el GoalBook de F (abrir fija las metas, F-10); suma los cinco RuleFix (E-06 de A y el de grammar); batch suma audit-infrastructure a sus clientes; RefinementTaskStatus declara type: enum; lista entera de patrones.

2026-09-30 — architect — Una propuesta pendiente por ejercicio, compuesta por pasos con sus partes (`BatchChangeLedger`):
  sacar un cambio quita sus pasos y los posteriores que tocaron lo mismo y reemplaza la propuesta.
  why: con una propuesta por cambio, F declararía no aplicables a las posteriores apenas se saca una (F-CDIFF-R012).

2026-09-30 — architect — Alineado con F (su TECH_SPEC del 30/9): la línea base se lee de `BaselineStore` (sin base no abre);
  la tanda pone el par activo en su punto de partida al abrir y lo mueve al publicar; cierra con `BatchComparator` y un
  `ProjectionScope` con sus propuestas; pide la muestra ciega a `BlindSampleService` cuando pasa la compuerta.

2026-09-30 — architect — Tareas de la tanda en un estado nuevo `IN_BATCH`: `nextTask` y revise-instructions ya no las ven;
  `revise` las rechaza en el comando con el mensaje de R002. Reglas por el puerto `RuleFix` de cada analizador de A (E-06).

2026-09-30 — analyst — José, 30/9 (vía MESA y BANDEJA): 4 y 4 a la vez; la mesa acepta; la opción múltiple entra a la
  tanda (R004); encargos de José al abrir, como «Contracciones» a opción múltiple (R001.4, R010).

2026-09-30 — analyst — Alineado con F: una tanda son SUS propuestas pendientes sobre el curso del que partió, nunca «las
  pendientes del plan activo» (R006 inv. 1); la línea base queda fija y la heredan las tandas; el punto de partida es el
  análisis de la última publicada (R001, R009 inv. 4).
  why: el plan puede tener correcciones de vocabulario pendientes de `revise`; y lo que avanza al publicar no es la base.

2026-09-30 — analyst — R008: sacar un cambio saca los posteriores del mismo ejercicio que tocaron lo mismo; los demás se
  conservan y se vuelven a juzgar; la miniteoría no sale [ASSUMPTION] → DOUBT-SACAR-ENCADENADO (OPEN, recomendada A).

2026-09-30 — analyst — La proyección y la comparación de la tanda son las del paquete F (F-PROYU-R001, F-METAS-R001/R008).

2026-09-30 — analyst — Código FEAT-TANDA / F-TANDA: nuevo.

2026-09-30 — analyst — La tanda toma sólo errores (+ LEMMA_ABSENCE que cubre un error); el largo y el resto del vocabulario quedan fuera (R001).
  why: José 11/8 (el largo no rechaza), el 077 no corrigió largos y 3.051 tareas triplicarían la mesa; siguen con revise.

2026-09-30 — analyst — Una sola tanda abierta; sus tareas no las toman revise ni revise-instructions (R002, mensaje [ASSUMPTION]).

2026-09-30 — analyst — La vista previa por propuesta (F-PIPRE-R001) se mantiene; la tanda suma el recálculo combinado.
  why: sacarla sería cambiar una decisión; su costo (~38 s cada una con el caché de hoy) es tarea del arquitecto (caché por oración).

2026-09-30 — analyst — Publicar = aprobar juntos los pendientes de la tanda; nunca producción (R009).
