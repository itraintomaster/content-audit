# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (rama ronda1/recur-posiciones) — En B, una posición es un token cuyo TEXTO tiene una letra o una
  cifra («'s» de «There's» ocupa posición aunque su lema sea «'»; «1900» también). Por eso el fixture stream.tsv.gz, que
  sólo guarda lemas, da 15.996 posiciones en A2 y la corrida real 15.997: cada prueba fija las de su fuente.
  why: un intervalo mide palabras que el alumno lee; el lema no sirve para decidir si un token se lee.
2026-10-01 — developer (rama ronda1/recur-posiciones) — Las pruebas que fijan «el vocabulario de antes» (preContract
  de Base299, F-HALL-R013 de DefaultAuditRunner) usan 0,09 en vez del 0,08 publicado para lemma-recurrence en el curso.
  why: B mueve sólo la clave de recurrencia del curso; el resto de los 11.760 nodos sigue igual al análisis del 29/9.

2026-10-01 — orquestador + developer (paquete D) — Lo que no es una palabra no entra a la recurrencia (opción A):
  el criterio es el lema (letras, apóstrofo o guion internos), no el texto, porque el análisis cuenta lemas y el
  fixture stream.tsv.gz guarda sólo lema y categoría; así la corrida real y el fixture dan lo mismo. Los ordinales con
  cifras quedan afuera: no son vocabulario que se recicla y lemma-absence/lexis tampoco evalúan tokens con cifras.
  why: la vista mostraba «'», «-» y «3rd» como primeras marcas; el puntaje sigue en 0,08 y el vocabulario no se mueve.
2026-10-01 — orquestador — La opción B (posiciones sin puntuación) mueve el vocabulario a 74,00 %: queda en
  ronda1/recur-posiciones, sin juntar, para que decida José con el antes y el después.

2026-09-30 — developer (paquete D) — El detalle por nivel usa los mismos umbrales y el mismo tope de 2.000 que el curso,
  medido con las posiciones de los ejercicios de ese nivel. Regla «F-RECUR-R005» (antes unhealthy-recurrence); cada
  hallazgo suma «Curso: 8,0 % de las 1.750 palabras analizadas…»; con puntaje menor que 1 y ningún lema fuera de rango
  (ninguna palabra vuelve) deja un hallazgo de respaldo, para no romper F-HALL-R001.
2026-09-30 — developer (paquete D) — La prueba F-HALL-R003 de lemma-recurrence pasa de un hallazgo de curso a uno por lema
  fuera de rango (92 en su curso de prueba), como pide R005.

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): sin cambios de estructura (lugar en CourseDiagnoses y formateador en stats, D-10 y D-15, van en la integración).

2026-09-30 — qa-tester — F-RECUR-J001 en audit-cli, `com.learney.contentaudit.auditcli.commands`.
  why: pide analyze y stats analyzer, package-private ahí, y su último paso mira el plan.

2026-09-30 — qa-tester — R003 se prueba en el analizador, el formateador y StatsAnalyzerCmd; R004 prohíbe la clave de puntaje en el milestone en el analizador y fija los niveles de vocabulario en IAuditEngine.
  why: hoy DefaultRefinerEngine abre LEMMA_RECURRENCE en un milestone con puntaje: una clave de nivel crearía tareas.

2026-09-30 — analyst — José resolvió DOUBT-PUNTAJE-POR-NIVEL → A, en cada nivel es detalle y no puntaje, y DOUBT-UNA-SOLA-VEZ → A, no cuenta las palabras vistas una vez (José, 30/9). R004 pasa a VALIDATED.

2026-09-30 — analyst — R003: `get audit` sale de la regla (muestra sólo el resumen del curso, para todo analizador); los datos por palabra los muestra `stats analyzer lemma-recurrence` y los publica el informe.

2026-09-30 — architect — El detalle por nivel viaja dentro del diagnóstico de curso de lemma-recurrence, nunca como clave de puntaje del nivel.
  why: una clave «lemma-recurrence/…» en el nivel subiría al curso por la agregación genérica y sería un número más que nadie pidió; el diagnóstico tipado es lo propio del analizador (F-HALL-R001 inv. 3).

2026-09-30 — architect — Lematizador medido: con las 17 formas corregidas vuelven 1.744 lemas, 134 normales; el puntaje sigue en 0,08.

2026-09-30 — analyst — Código FEAT-RECUR / F-RECUR; el analizador conserva su nombre `lemma-recurrence`.
  why: F-HALL-R005 (un solo nombre).

2026-09-30 — analyst — Umbrales 50/1.000, top 2.000, orden A1→B2 y puntaje de curso quedan como regla (R001); nada nuevo es puntaje.
  why: regla de oro del 73,9 %; el top 2.000 no corta hoy (el puesto 2.000 cae entre los 1.041 lemas vistos una vez), que equivale a la opción B que José marcó en DOUBT-TOP-N-VALUE.

2026-09-30 — analyst — La recurrencia por nivel se mide dentro del nivel y se publica como sub-métrica (ASSUMPTION, R004); como puntaje de nivel es DOUBT-PUNTAJE-POR-NIVEL.
  why: José marcó B en DOUBT-PER-LEVEL-ANALYSIS de FEAT-LREC, pero como puntaje bajaría los niveles (A1 96,5 → 88,4); como detalle no mueve nada (F-HALL-R010).

2026-09-30 — analyst — Se publica el «hueco más largo» (nuevo) además de la desviación (F-LREC-R011 sigue informativa).
  why: responde en palabras de pantalla a la pregunta de José sobre qué es la desviación («suppose»: promedio 696, hueco 27.379).

2026-09-30 — analyst — Lemas vistos una vez: siguen excluidos (F-LREC-R006); incluirlos es DOUBT-UNA-SOLA-VEZ (bajaría a 0,05 y 73,5 %).
  why: ya son hallazgo de lemma-count (FEAT-POCOV); es la superposición que José temía.
