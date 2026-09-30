# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

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
