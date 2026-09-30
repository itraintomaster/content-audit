# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-TRSEN / F-TRSEN; nombre del analizador `translation-sense`, familia de errores.

2026-09-30 — analyst — Los dos jueces son dos evaluadores distintos en el libro (R003).
  why: F-EVCOST-R001 identifica por evaluador + huella y F-EVCOST-R006 hace ganar al más reciente: con un solo evaluador,
  el segundo veredicto pisaría al primero y «dos jueces» sería uno. [ASSUMPTION] uno parte del inglés y otro de la traducción.

2026-09-30 — analyst — Combinación [ASSUMPTION]: los dos «no» = error; desacuerdo = hallazgo que sólo ordena (no cuenta).
  why: no infla «algún error» y lleva el desacuerdo a la bandeja de desempates de la propuesta; queda como DOUBT-TRSEN-COMBINAR.

2026-09-30 — analyst — Lee la consigna de la copia del ejercicio, no la del tema; no lee miniteoría ni audio.
  why: la app muestra la copia (quiz.dart, template.instructions); la miniteoría y la traducción salen en la corrección
  (QuizFeedback). La traducción sí entra: es lo que se juzga. Menos cosas leídas = huella más estable.

2026-09-30 — analyst — Nunca bloqueante: la traducción se ve después de contestar. Escala 1 / 0,8 / 0,6 / 0,3 [ASSUMPTION].
  why: F-HALL-R002 define bloqueante como «no se puede acertar»; el inglés raro se puede acertar.

2026-09-30 — analyst — Choque señalado, no resuelto: F-QICOR-R015 pide una sola definición de cada criterio, y «traducción
  fiel» queda comparativa en FEAT-QICOR y absoluta acá. Va como DOUBT-TRSEN-CATALOGO (recomendado: convivir hasta que la
  mesa de E reemplace a la corrección de consigna).
