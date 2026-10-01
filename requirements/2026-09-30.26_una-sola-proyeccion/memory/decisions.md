# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — architect — El tope de la proyección dice lo mismo que R003 (pedido del orquestador): `ProjectionScope.analyzerBudgets` vale sólo para los analizadores que el pedido nombra; el resto mide con la política de analyze, y `DefaultProjectionMeasurer` pasa vacía su política, que el proveedor completa según a quién consulta (lo local sin tope, claude -p con 0). Alineadas también «Dar a la proyección un solo contrato» y «Medir cada foto…». Sin cambios de estructura.
  why: F-RONDA2-R002 inv. 1, el runner no completa la política; si el medidor pusiera 0, naturalness y near-duplicates con Ollama quedarían sin evaluar en la proyección.

2026-10-01 — analyst — R003 con la política de tope de `analyze` (decidido por el orquestador el 1/10, gana F-RONDA2-R002): lo local sin tope y claude -p con tope 0 salvo pedido. Antes decía «0 si no se pide» para todo. Validation sigue AUTO_VALIDATED; FEAT-RONDA2 suma a References.
  why: con Ollama, naturalness y near-duplicates corren sin tope en analyze; con 0 en la proyección dejaban sin evaluar lo cambiado y R001 no se cumplía.
2026-10-01 — qa-tester — Una prueba más de R003 en DefaultProjectionMeasurer: naturalness y near-duplicates con llama3 en Ollama corren sin tope, como en analyze. Para el arquitecto: «Ausente = 0» en ProjectionScope.analyzerBudgets, «tope 0 salvo pedido» en la inyección auditEngine de DefaultProjectionMeasurer y dos secciones del TECH_SPEC todavía dicen 0 para todo.

2026-10-01 — qa-tester — F-PROYU-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: cruza get consolidated, save-projection, plan --audit, get audits y prune audits, package-private ahí (como F-JEXAM-J001 y F-CDIFF-J001); el proyector se alcanza por su fábrica pública.
2026-10-01 — qa-tester — Para el analista: R003 dice «0 si no se pide» y F-RONDA2-R002, que lo local no tiene tope sin --budget. ¿La proyección corre naturalness y near-duplicates con Ollama sin tope (como analyze, R001) o con 0? Las pruebas de R003 fijan sólo claude -p, donde coinciden.
2026-10-01 — qa-tester — R007, «el número de cada nivel es el puntaje de vocabulario publicado», no se repite: ya lo fija la prueba de F-HALL-R010 en DefaultImpactPreviewComputer.

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
