# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — F-TRSEN-J001 en audit-domain, `com.learney.contentaudit.auditdomain.translationsenseengine`, como los del juez de consigna.
  why: ahí son visibles el analizador, el combinador y el constructor del sujeto; ArchUnit no mira las clases de test.
2026-09-30 — qa-tester — Dos pruebas sobre AgentJudge (de FEAT-JEXAM): salida inutilizable (R005) y entradas tal cual (R001).
  why: son de todo juez nuevo pero sólo se pueden trazar a uno; si TRSEN se aplicara antes que JEXAM, AgentJudge entraría vacía.

2026-09-30 — architect — Dos evaluadores (translation-sense-from-english / -from-translation) con el mismo mapa de
  contenido: cefrLevel, instructions (la del ejercicio), exercise, acceptedSentences, translation. El analizador reparte el
  tope de a pares con `recorded()` en el orden del recorrido y resuelve cada juez con resolveAll.
  why: con tope por sesión el primer juez se comería el tope y ningún ejercicio quedaría con los dos veredictos.
2026-09-30 — architect — Combinación en `DefaultTranslationSenseCombiner`: BOTH_FAIL un hallazgo por condición con la
  gravedad más alta; SPLIT uno solo `judges-disagree` que sólo ordena, puntaje 1 (José, 30/9). Escala en la config.

2026-09-30 — analyst — DOUBT-TRSEN-COMBINAR → C (José, 30/9: «si los dos de traducción no coinciden, queda marcado
  para la bandeja»). R003 pasa a VALIDATED y pierde sus [ASSUMPTION].
2026-09-30 — analyst — DOUBT-TRSEN-CATALOGO → A (José, 30/9: «por lo demás, continúa»; la presentación lo mostraba en
  «Lo que no toca»). Las dos definiciones de «traducción fiel» conviven hasta que la mesa de E reemplace a QICOR.
2026-09-30 — analyst — R005: tope por omisión 0 (F-JEXAM-R010) y 4 consultas a la vez (F-JEXAM-R006), por José, 30/9.

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
