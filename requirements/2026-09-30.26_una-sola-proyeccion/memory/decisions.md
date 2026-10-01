# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): GetConsolidatedCommand repite sealed: true (el apply la abría, bug 09) y nodeFieldDiffer conserva su descripción. La medida de cada analizador en el informe (F-01 a F-04) es F-RONDA2-R004; el caché por oración (F-07), R005.

2026-09-30 — analyst — Duda nueva DOUBT-LINEA-BASE-TANDAS (OPEN, recomendada A: fijar como línea base un análisis nuevo con la ronda 1 integrada, no el del 29/9, que no trae hallazgos, errores ni medida).

2026-09-30 — architect — Un solo proyector (`CourseProjector`, paquete público `projection`, motor en `engine` con fábrica y `CourseProjectorConfig`) arma las tres fotos para la vista consolidada, la proyección guardada, la comparación y la muestra de F y la tanda de E. Adentro, dos colaboradores internos: `PendingApplier` (R002, una sola regla de aplicación) y `ProjectionMeasurer` (R001/R003, mapear y correr con los analizadores del análisis de partida, tope 0 salvo pedido).
  why: tres caminos no coincidían; con uno solo, «la misma medida» sale por construcción. El motor queda en `engine` porque el localizador de nodos es de paquete.

2026-09-30 — architect — `ProjectionScope(scopeId, proposalIds, analyzerBudgets)` = «qué es una tanda» para F: E pasa las pendientes de su tanda; null = todo el plan activo.
  why: F no conoce el modelo de E; así no hay dependencia de F hacia E.

2026-09-30 — architect — Vista consolidada e impacto se rehacen sobre aplicador y medidor sin cambiar sus registros de configuración: las fábricas los arman con los campos que ya tienen. El impacto usa el análisis de la tarea (`sourceAuditId`), nunca `loadLatest`.

2026-09-30 — architect — Proyección guardada: puerto `ProjectionStore` (raíz de revision-domain) + `FileSystemProjectionStore` en `.content-audit/projections/`, uno por par activo. Que `load(id)` la resuelva es cuerpo de `FileSystemAuditReportStore` (gancho F-05).
  why: el validador no resuelve un `implements` de la raíz de un módulo contra un paquete (fix-log de HALL); list/loadLatest/prune no la ven por construcción.

2026-09-30 — architect — Línea base aparte: `Baseline` + `BaselineStore` (raíz de audit-domain) + `FileSystemBaselineStore` + `set-baseline`; `DeleteCmd` y `PruneCmd` reciben los dos puertos de selección. La medida de cada analizador en el informe (F-01 a F-04) es gancho.

2026-09-30 — analyst — José, 30/9: DOUBT-OTROS-CAMINOS → A (Stats se retira; el plan proyectado y el impacto por propuesta se rehacen sobre la proyección) y DOUBT-PRUNE-ACTIVO → A (`prune` nunca borra el análisis activo y avisa; cambia F-CLIRV-R003). Las dos RESOLVED.

2026-09-30 — analyst — R006 reescrita: la línea base se separa del par activo (`set-baseline`), queda fija mientras el activo avanza al publicar cada tanda (paquete E), y delete/prune protegen a las dos. VALIDATED.
  why: nota del orquestador para alinear con E (FEAT-TANDA abre cada tanda sobre el último publicado); con base = activo, la base se iba con él. Las definiciones dicen ahora que, con tanda abierta, se proyectan sólo sus pendientes.

2026-09-30 — analyst — Confirmado con `get-consolidated` del CLI real, sobre un workdir de scratch con las 4 pendientes sembradas (el store real escribió los artefactos): niveles con COCA 0,75 / 0,75 / 0,761 / 0,513 en la foto con pendientes; los 55 topics salen como cambiados, con 0 tokens COCA en la foto con lo aprobado; y el curso NO aparece en la salida. Tardó 4 s con los tokens en caché.
  why: `indexAuditNodes` indexa por id de entidad y la raíz no tiene entidad; por eso R001 dice «el curso incluido».

2026-09-30 — analyst — Código FEAT-PROYU / F-PROYU: nuevo; no aparece en ningún requirement ni en los worktrees de las rondas 1 y 2.

2026-09-30 — analyst — La proyección única es la de la vista consolidada, y su equivalencia con `analyze` sobre el curso escrito es la regla (R001).
  why: el brief 089 manda reusarla («copia de los reportes y no recalcula»). Medido con las 4 propuestas de «Afirmativas regulares»: foto con pendientes 72,90 / 92,38 · 89,99 · 82,48 · 66,70 contra 73,92 / 96,54 · 94,16 · 79,45 · 63,51 de `analyze` de la copia.

2026-09-30 — analyst — Línea base = el análisis activo de `set-active`; no se crea una «línea base con nombre» aparte (R006).
  why: el par activo ya existe (F-CDIFF-R001/R002); lo que se rompe es «el último»: loadLatest lo usan el impacto por propuesta, `plan` sin `--audit`, el corpus de criterios (fallback) y la selección inicial del dashboard.

2026-09-30 — analyst — La proyección se puede guardar, pero fuera del historial y direccionable por id (R004, R005), en vez de prohibir guardarla.
  why: F-PLANEF-R002 y F-CSLATDC-R015 necesitan un análisis fuente por id (plan proyectado y palabras sugeridas); hoy el dashboard lo escribe en el historial principal desde el 15/7 (commit 5c2b831a8 de ittm).

2026-09-30 — analyst — El impacto por propuesta (FEAT-PIPRE) se conserva y se recalcula con la proyección (R007).
  why: el código incumple F-PIPRE-R003/R004: usa loadLatest y, para cada contenedor, el primer nodo de ese tipo del curso. Medido: para «grandmother» reporta el tema «Afirmativas con $be$» (el primero del curso) sin cambio; el suyo baja 0,5 puntos.

2026-09-30 — analyst — `prune audits` y `delete audit` protegen al activo (R006); cambia F-CLIRV-R003 → DOUBT-PRUNE-ACTIVO.
  why: PruneCmd borra los más viejos sin mirar referencias ni el activo.

2026-09-30 — analyst — Anotado: 143 ejercicios tienen dos tareas en el plan 2026-09-30T11-54-12 (SENTENCE_LENGTH + LEMMA_ABSENCE); ahí el dashboard (gana la última pendiente) y la vista consolidada (gana la primera, la otra no aplicable) proyectan distinto (R002).
