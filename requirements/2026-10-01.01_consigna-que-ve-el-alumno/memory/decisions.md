# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (C) — Las pruebas de get y del corrector 1.1.0 entran antes de declararse, con el nombre que
  les dará generate, como las de RESPC. La de la estrategia es una clase nueva y va sin @Generated hasta que el parche
  de seguimiento la declare. Con la marca, sentinel verify la toma por huérfana; sin ella, generate no la toca y
  verify da OK, simulado.
2026-10-01 — developer (C) — La aridad de FEAT-NIVFR va en un commit propio, porque el árbol no compilaba sin ella. No
  toca el cuerpo de NIVFR: sus 3 pruebas en rojo son de quien lo construya.

2026-10-01 — architect (C) — R003 suma un campo, quizInstructions, en vez de renombrar knowledgeInstructions.
  Cambiarle el contenido a knowledgeInstructions dejaba un nombre de tema con un valor de copia. Renombrarlo arrastraba
  la mentira a CandidateAssessmentInput y al juez de candidatos, que siguen con la consigna del tema y quedan afuera.
  La etiqueta y la consigna del tema siguen como contexto, con la descripción al día. El corrector recibe
  quizInstructions y deja de recibir knowledgeInstructions; la lección sigue siendo knowledgeTitle.
2026-10-01 — analyst (C) — DOUBT-CALUM-CORRECTOR, resuelta con la B (coordinador): el corrector apunta a la consigna
  de la copia. Igualar el dato no alcanza: en 113 de 413 temas las copias tienen consignas distintas entre sí. Igualar
  queda como limpieza aparte, para la mesa.

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
