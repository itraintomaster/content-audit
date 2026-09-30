# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-SEMAF / F-SEMAF. El semáforo es una consulta de sólo lectura por tema (knowledge), no un analizador (DOUBT-CONSULTA-O-ANALIZADOR, recomendado A).
  why: no tiene meta ni puntaje; sus hallazgos repetirían los de lemma-absence y off-level-words; así no toca el 73,9 %.

2026-09-30 — analyst — Verde = sugeridas del nivel del tema (F-SLEM/F-CSLATDC, mismo orden y filtros) más las palabras propias del tema con nivel del tema y menos de 2N oraciones. Rojo = lo que `lexis` marca para el nivel del tema, salvo lo que el tema enseña.
  why: es lo que pidió el plan (reusar sugeridas y consulta léxica); los cortes por N son los de F-SLEM-R014, sin literales.

2026-09-30 — analyst — Sin rojo por sobreexposición (ASSUMPTION, DOUBT-SOBREEXPOSICION) y sin verde por recurrencia hasta tenerla por nivel (DOUBT-VERDE-POR-RECURRENCIA).
  why: >15 oraciones = 440 lemas, 290 de A1; con intervalo >1.000 es verde la mitad de A1 (1.878 de 3.717). En el curso falta repetir (134 de 1.750 lemas a ritmo sano).

2026-09-30 — analyst — Numeros del pool de sugeridas: la lista absentLemmas del informe está recortada por F-LABS-R026 (máx. 100 por nivel), así que no se publica un total de pool; se citan ausentes (totalAbsent) y poco vistas por separado.
  why: evitar un número que depende del recorte del informe.
