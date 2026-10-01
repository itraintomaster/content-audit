# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — architect (C) — El constructor del sujeto del juez de consigna recibe QuizStudentViewRenderer inyectado
  (requiresInject), como los de los otros cuatro jueces, y las dos fábricas que lo crean también: la del análisis y la
  de los candidatos de revise-instructions. No se pasa por parámetro ni se arma adentro.
2026-10-01 — architect (C) — Las claves, en este orden: cefrLevel, topic, title, instructions, exercise,
  acceptedAnswers, options. Son las mismas que usan los otros jueces para lo mismo («Hueco N: a | b» y «- opción
  (correcta)»). Nivel y topic se quedan como contexto, porque la guía los usa; la traducción y la miniteoría no entran.
2026-10-01 — architect (C) — buildFromView pasa la vista de un candidato por el mismo renderizador, con un AuditableQuiz
  armado con sus partes, su opción múltiple, su título y su consigna: un solo lugar arma el contenido para los dos
  caminos (F-QICOR-R005).
2026-10-01 — analyst (C) — revise-instructions le pasa a la vista el título y la consigna de la copia del original y
  del candidato (F-CALUM-R002). El corrector sigue con la del tema: queda en DOUBT-CALUM-CORRECTOR.
2026-10-01 — analyst (C) — Las dos pruebas del analizador que fijaban F-JEXAM-R007 (curso juzgado antes se resuelve sin
  consultar; huella de agosto) se borran y se reemplazan por las de F-CALUM-R001. La de la traducción y la
  miniteoría se queda: sigue siendo cierta.
  why: decisión del coordinador del 1/10 —el juez de consigna lee la consigna que ve el alumno—, por la regla de José
  de que todo juez lee el ejercicio entero como lo ve el alumno (C-07).
