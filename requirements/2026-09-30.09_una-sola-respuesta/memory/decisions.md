# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): SingleAnswerAnalyzerProvider implements AnalyzerProvider (C-02).

2026-09-30 — qa-tester — F-UNICA-J001 en audit-domain, `com.learney.contentaudit.auditdomain.singleanswerengine`.
2026-09-30 — qa-tester — El criterio de F-JEXAM-R012 («isn't» aceptada, «is not» como único reparo: sin hallazgo) se prueba acá bajo F-UNICA-R002, con CorrectorMatcher doble.
  why: propose no deja trazar a FEAT-JEXAM desde este parche; con la tabla vacía de hoy «is not» no es «isn't» (A-15, A-16).
2026-09-30 — qa-tester — Duda para el analista, sin prueba: si el juez lista «won't come» y «will not come» para «She ___ (not / come).», ¿son una respuesta o dos? R002 descarta lo que el corrector acepta para el ejercicio, y no acepta ninguna de las dos.

2026-09-30 — architect — Mapa de contenido: title, instructions (la del ejercicio), exercise, acceptedAnswers; sólo
  CLOZE. El clasificador descarta con `CorrectorMatcher.accepts` (paquete A) lo que el corrector ya acepta antes de contar:
  la regla de las formas no depende del modelo. Un hallazgo por respuesta, con la respuesta como marca.

2026-09-30 — analyst — Cambio de José, 30/9 (short/long forms, «que acepte siempre ambas»): lo que el corrector acepta
  —contraída o larga, grafías británica/americana de la lista cerrada, until/till, y lo que ya pasa por alto— deja de ser
  «otra respuesta» y no da hallazgo. La primera clase pasa a ser «una opción que ofrece la consigna» (rank-only, la arregla
  una regla de A con «|»). R002 reescrita (VALIDATED), R003 y R007 renombradas, J001 ajustado.
  why: el corrector va a aceptar las dos formas siempre; listarlas sería contar un problema que no existe.
2026-09-30 — analyst — DOUBT-UNICA-MC → A (José, 30/9, «por lo demás, continúa»): sólo CLOZE; R001 pierde el [ASSUMPTION].
2026-09-30 — analyst — DOUBT-UNICA-FORMA → A ajustada por el cambio de arriba. Lo que el juez liste igual se descarta
  con la tabla del corrector (la que imita el paquete A): depende de ese imitador, anotado en los ganchos del integrador.

2026-09-30 — analyst — Código FEAT-UNICA / F-UNICA; nombre del analizador `single-answer`, familia de errores.

2026-09-30 — analyst — Alcance sólo CLOZE (8.477) [ASSUMPTION]; la opción múltiple la juzga FEAT-MCVALE (DOUBT-UNICA-MC).
  why: en opción múltiple «otra respuesta igual de buena» es «otra opción también vale»: dos jueces con la misma pregunta
  pagarían 2.810 consultas dos veces y contarían el mismo defecto en dos filas. En escribir/unica.json hay 86 CLOZE y 93 MC.

2026-09-30 — analyst — Lee lo que el alumno ve ANTES de contestar: nombre del tema, consigna (copia del ejercicio),
  enunciado con pista y respuestas aceptadas. No lee traducción ni miniteoría.
  why: la app las muestra en la corrección (QuizFeedback en interaction.dart); el 077 lo dice: «sólo la traducción, que se
  ve después de contestar, dice cuál era». El nombre del tema se agregó a la lista de la propuesta porque la app lo muestra
  (quiz.dart, template.title) [ASSUMPTION].

2026-09-30 — analyst — «La misma en otra forma» no cuenta como error acá: hallazgo que sólo ordena que nombra a respuestas
  completas (paquete A). why: F-HALL-R008 cuenta un ejercicio una vez en «algún error», pero cada analizador contaría el
  mismo defecto; así cada fila cuenta lo suyo y el paquete A ve lo que se le escapa.

2026-09-30 — analyst — Gravedad: bloqueante cuando no se puede saber cuál acepta (varias igual de buenas, sin pista, p. ej.
  las verduras); alta cuando rechaza otra igual de buena; media cuando rechaza otra correcta menos esperable. Escala
  1 / 0,6 / 0,3 / 0 [ASSUMPTION].
