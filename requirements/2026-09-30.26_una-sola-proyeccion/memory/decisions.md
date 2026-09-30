# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
