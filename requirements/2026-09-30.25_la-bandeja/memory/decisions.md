# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — F-BANDEJA-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands` (2 caminos).
  why: sus pasos son tray show, tray answer y tray publish; TrayCmd es package-private ahí, como ExamCmd en F-JEXAM-J001.

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): tray suma audit-infrastructure a sus clientes (FileSystemTrayDecisionStore); lleva la lista entera de patrones de revision-domain, las seis.

2026-09-30 — architect — La bandeja es una vista que arma content-audit y convierte cada respuesta en un ajuste de la tanda
  (`BatchService.adjust`); la muestra ciega es la de F (`BlindSampleService`, `BlindPairView`, `PairAnswer`), sin puerto propio.

2026-09-30 — architect — Lo decidido se recuerda con la identidad del hallazgo y la huella del ejercicio como lo ve el
  alumno, con el `ContentFingerprinter` del libro de evaluaciones: revision-domain pasa a depender de evaluation-ledger-domain.

2026-09-30 — analyst — José, 30/9: DOUBT-QUIEN-APRUEBA → A (la mesa acepta; José decide desempates y sin ganador, mira
  las muestras y publica con un solo OK).

2026-09-30 — analyst — Orden de la bandeja alineado con F (DOUBT-QUIEN-CIEGA A, José 30/9): primero la muestra ciega y,
  mientras no se cierra, nada más de la tanda; después las decisiones por gravedad; al final las muestras de regla (R001,
  R005, J001 reordenado).
  why: si José ve un cambio en otra cosa de la bandeja antes de la ciega, deja de ser ciega.

2026-09-30 — analyst — La muestra ciega es la de FEAT-CIEGA (paquete F): la bandeja sólo la presenta; publicar exige el
  veredicto «pasa» de F-METAS-R008 además de la compuerta.
  why: F ya define sorteo, tamaño, respuestas y margen; repetirlo acá daba dos definiciones.

2026-09-30 — analyst — Código FEAT-BANDEJA / F-BANDEJA: nuevo.

2026-09-30 — analyst — Las propuestas de la tanda quedan pendientes hasta publicar: es el modo humano con un OK por tanda (R008).
  why: no cambia F-REVAPR; `approve` de una propuesta de la tanda abierta se rechaza (mensaje [ASSUMPTION]).

2026-09-30 — analyst — «Aprobar el candidato» sin ganador queda como excepción declarada de José; la compuerta no la vuelve a contar (R003).
  why: si no, la compuerta devolvería lo que José aprobó y la tanda no cerraría.

2026-09-30 — analyst — Lo decidido se recuerda por identidad del hallazgo + huella del contenido (R006), como el libro de evaluaciones.
