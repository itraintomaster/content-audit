# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): lleva la lista entera de patrones de revision-domain. SpokenTextBuilder, interna de datacoherence, la abre FEAT-RONDA2 (sin eso la mesa no compilaba); B-08 (DOUBT-MLOCAL-MESA) con A, sin cambios de estructura.

2026-09-30 — architect — La mesa mide con el motor y los analizadores del análisis de partida sobre el curso recortado
  al tema (nivel → topic → tema): mismos hallazgos de ejercicio y tema que en el curso, y un juez sólo puede consultar ese
  tema. Jueces con el libro: lo que no cambió reusa por huella.
  why: F-MESA-R004/R005 sin criterios propios, y sin gastar el tope en ejercicios de otros temas.

2026-09-30 — architect — Un cupo compartido (`QueryAllowance`) lleva N, C y el tope de la tanda; cada medición pide a los
  jueces de a una consulta (C-17), así que con N mesas nunca hay más de N en curso. Una falla corta el cupo.

2026-09-30 — architect — Quien propone es un puerto: agente en revision-infrastructure (`panelagent`, molde de la consigna)
  o `ScriptedPanelProposer`, guionado, para pruebas y repeticiones sin pagar.

2026-09-30 — analyst — José, 30/9: DOUBT-TEMAS-A-LA-VEZ → B (4 temas y 4 consultas; 8 y 8 sólo si A1 corre sin cortes,
  como pedido al abrir la tanda siguiente). R010 inv. 4.

2026-09-30 — analyst — José, 30/9: DOUBT-MC-REARMA → A (reglas de forma + mesa; sigue siendo opción múltiple, una sola
  correcta, misma cantidad; cambia F-OPMUL-R006 sólo dentro de las tandas). R011 → VALIDATED.

2026-09-30 — analyst — José, 30/9: el tema «Contracciones» de A1 pasa a opción múltiple con contracciones distintas;
  lo arma la mesa → R015 nueva (VALIDATED) y DOUBT-OPCIONES-CONTRACCIONES (OPEN, recomendada A: tres opciones).
  why: en la copia del 29/9 son dos temas con ese nombre, 20 + 20 ejercicios CLOZE (Present Simple y Present Continuous).

2026-09-30 — analyst — José, 30/9: DOUBT-MINITEORIA-ALCANCE → A (R012 → VALIDATED; la miniteoría va antes que los
  candidatos del tema) y DOUBT-QUIEN-REFUTA → A (R013 → VALIDATED; sin agente nuevo).

2026-09-30 — analyst — Código FEAT-MESA / F-MESA: nuevo.

2026-09-30 — analyst — Un candidato por ejercicio que intenta resolver todas sus tareas; la miniteoría, una vez por tema (R001).
  why: tres cambios sobre el mismo ejercicio se pisan (F-CDIFF-R012 marcaría no aplicable al segundo).

2026-09-30 — analyst — Decide un script con vara absoluta (resuelve + no empeora); el desempate prefiere no regrabar [ASSUMPTION] (R006).
  why: es la vara de la propuesta v2; la comparativa de F-QICOR-R013 sigue valiendo en revise-instructions (R014).

2026-09-30 — analyst — Sin ganador: queda el original y José decide con el candidato a la vista (R007).
  why: mantiene el espíritu de DOUBT-SIN-CORRECCION (B) de FEAT-QICOR sin que la mesa acepte sola lo que no pasó.

2026-09-30 — analyst — Refutar = los controles que ya buscan contraejemplos, sin agente nuevo [ASSUMPTION] (R013, DOUBT-QUIEN-REFUTA).
  why: el agente aparte suma una consulta por candidato (A1: 2.600 en vez de 2.200) para buscar lo que ya buscan single-answer/otra vale, lexis y TRSEN cond. 2.

2026-09-30 — analyst — Cambios a decisiones existentes, sólo como duda: F-OPMUL-R006 (DOUBT-MC-REARMA) y F-RPRES-R003 (DOUBT-MINITEORIA-ALCANCE).
