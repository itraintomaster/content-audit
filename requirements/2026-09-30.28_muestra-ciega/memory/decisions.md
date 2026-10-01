# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — integrador — Rebasado sobre ronda1/integracion y FEAT-RONDA2 (propose --replace, tech-spec write y validate en exit 0): sin cambios de estructura.

2026-09-30 — architect — Muestra en revision-domain: contrato `blindsample` y motor `blindsampleengine` (público sólo para audit-cli). Dos portadores por par: el guardado con el lado (`BlindPair`) y el que ve quien revisa (`BlindPairView`), sin lado, motivo, fecha ni autor.

2026-09-30 — architect — Sorteo reproducible: candidatos (cambios que no vuelven según METAS) ordenados por fecha e id, barajados con un generador inicializado con la semilla publicada; el mismo generador sortea el lado. Intervalos de Wilson al 95 % (95/100 → [88,8; 97,8]) y cota exacta a una cola para el criterio «cota» y la suma (0/100 → 3,0 %). Kappa de Cohen.

2026-09-30 — architect — `SampleOutcomeSource` lo implementa `DefaultSampleOutcomeSource` (lee la muestra guardada), no el servicio.
  why: el comparador inyecta la fuente y el servicio inyecta al comparador: con el servicio como fuente había un ciclo de construcción.

2026-09-30 — architect — Usa `QuizStudentViewRenderer` (gancho A-01 del integrador de la ronda 1); no se arma otro. Único tipo sin declarar en el conjunto de F.

2026-09-30 — analyst — José, 30/9: DOUBT-QUIEN-CIEGA → A (la hace José antes de abrir la bandeja; en la primera tanda un tester repite 30 pares; la tasa de error de 400 la hace un tester) y DOUBT-TAMANO-CIEGA → A (100 pares por tanda, meta sobre lo contado; el «menos de 2 %» se afirma sumando tandas). Las dos RESOLVED; R004 suma la cuenta acumulada desde la línea base; el ASSUMPTION del contexto queda decidido.

2026-09-30 — analyst — Tanda = las pendientes de la tanda abierta (FEAT-TANDA); sin tanda, las del plan activo. La bandeja tiene que poner la muestra primero y sola hasta cerrarla (propuesta para FEAT-BANDEJA, marcada para el integrador).

2026-09-30 — analyst — Código FEAT-CIEGA / F-CIEGA: nuevo.

2026-09-30 — analyst — La muestra se sortea entre los cambios que no vuelven (R001).
  why: mide lo que se publicaría; lo que vuelve ya tiene motivo.

2026-09-30 — analyst — El resultado lleva intervalos al 95 % y la meta declara su criterio, observado o cota (R004).
  why: con 100 pares la cota mínima de peores es 3,0 % (0 peores): «menos de 2 %» no se puede afirmar por tanda; con 150 y 0 peores, 1,98 %, pero una tanda con 1 % real fallaría 78 de cada 100.

2026-09-30 — analyst — La tasa de error a ciegas (R006) es a pedido; 400 ejercicios dan ±4,3 puntos al 25,7 % y ±2,1 al 5 %.
  why: es la medida de la propuesta v2 («400 al azar, ±4 puntos») y prueba si «algún error» de los analizadores mide lo que dice.

2026-09-30 — analyst — ASSUMPTION: la muestra se responde antes de abrir la tanda en la bandeja, o la responde otra persona.
  why: si quien responde ya aprobó los cambios, sabe cuál es el nuevo.
