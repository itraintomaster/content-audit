# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): sin cambios de estructura (la inyección de GetCmd, D-05, va en la integración).

2026-09-30 — qa-tester — F-SEMAF-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: GetCmd es package-private ahí; mismo paquete que F-SLEM-J001 y F-CLEX-J001.

2026-09-30 — qa-tester — 28 pruebas en DefaultTopicWordLightsQuery y 4 en GetCmd (D-05); la de R007 compara el análisis y el plan del 29/9 byte a byte, en 73,92 % y 4.036.
  why: R007 inv. 3 trae números: se fijan sobre una copia del análisis 2026-09-30T11-54-02 y su plan.

2026-09-30 — analyst — José resolvió DOUBT-CONSULTA-O-ANALIZADOR → A, consulta, y DOUBT-SOBREEXPOSICION → A, una palabra muy repetida no es roja (José, 30/9). R006 pasa a VALIDATED.

2026-09-30 — analyst — DOUBT-VERDE-POR-RECURRENCIA → A, la recomendada (José, 30/9: «por lo demás, continúa»).

2026-09-30 — architect — El verde propio usa el nivel y el umbral que `lemma-count` le da a cada palabra (EVP exacto, N de su banda), no la resolución de lemma-absence (R002 pasa a ASSUMPTION, avisado a José).
  why: el verde tiene que decir lo mismo que el hallazgo de poco vistas; los 70 pares palabra-tema de diferencia son etiquetado malo («pizza», «orange» como verbos, «our» sustantivo). Hoy 134/633/1.012/307; con bandas 242/771/1.080/307.

2026-09-30 — analyst — Código FEAT-SEMAF / F-SEMAF. El semáforo es una consulta de sólo lectura por tema (knowledge), no un analizador (DOUBT-CONSULTA-O-ANALIZADOR, recomendado A).
  why: no tiene meta ni puntaje; sus hallazgos repetirían los de lemma-absence y off-level-words; así no toca el 73,9 %.

2026-09-30 — analyst — Verde = sugeridas del nivel del tema (F-SLEM/F-CSLATDC, mismo orden y filtros) más las palabras propias del tema con nivel del tema y menos de 2N oraciones. Rojo = lo que `lexis` marca para el nivel del tema, salvo lo que el tema enseña.
  why: es lo que pidió el plan (reusar sugeridas y consulta léxica); los cortes por N son los de F-SLEM-R014, sin literales.

2026-09-30 — analyst — Sin rojo por sobreexposición (ASSUMPTION, DOUBT-SOBREEXPOSICION) y sin verde por recurrencia hasta tenerla por nivel (DOUBT-VERDE-POR-RECURRENCIA).
  why: >15 oraciones = 440 lemas, 290 de A1; con intervalo >1.000 es verde la mitad de A1 (1.878 de 3.717). En el curso falta repetir (134 de 1.750 lemas a ritmo sano).

2026-09-30 — analyst — Numeros del pool de sugeridas: la lista absentLemmas del informe está recortada por F-LABS-R026 (máx. 100 por nivel), así que no se publica un total de pool; se citan ausentes (totalAbsent) y poco vistas por separado.
  why: evitar un número que depende del recorte del informe.
