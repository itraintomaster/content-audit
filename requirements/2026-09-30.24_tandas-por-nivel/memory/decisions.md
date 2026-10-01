# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
