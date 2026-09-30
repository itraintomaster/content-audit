# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — architect — Dos puertos de sólo lectura en la raíz de audit-domain: AudioRecordingRegistry (db/grabaciones.json) y
  ProductionSentenceSource (db/quiz-templates.json, sólo _id y form.sentences); adaptadores en audit-infrastructure/exercisecopies.
  why: cambian por motivos distintos (la regrabación escribe el registro; la importación reemplaza la copia); el curso se sigue leyendo por CourseRepository.
2026-09-30 — architect — El texto de hoy del audio lo arma SpokenTextBuilder con la regla del 24/8; la oración armada, el renderizador
  del integrador reusando QuizSentenceConverter. Las claves compartidas se marcan en onCourseComplete, antes de agregar.
2026-09-30 — architect — El registro se arma una vez con un script en scripts/ (fuera de Sentinel); es lo primero, porque el manifiesto
  del 24/8 vive en un worktree que se puede borrar.

2026-09-30 — analyst — DOUBT-DATOS-PRODUCCION → A (José, 30/9): se leen las dos oraciones guardadas, la del árbol de content-audit y la
  de la copia de producción (db/quiz-templates.json). R001 pasa a hablar de las dos; un hallazgo por copia que no coincide.

2026-09-30 — analyst — DOUBT-DATOS-718 → A y DOUBT-DATOS-REGISTRO → A (José, 30/9, con la recomendación): espacios y reescrituras no
  cuentan; el registro de grabaciones vive en db/ de content-audit, armado una vez y actualizado por cada regrabación.

2026-09-30 — analyst — «Como el corrector» ahora incluye la tabla de formas equivalentes (F-RESPC-R001): medido, no cambia nada acá
  (R001 da 0 en el árbol con o sin tabla; R003 ya tomaba igual la contraída y la larga).

2026-09-30 — analyst — Código FEAT-DATOS / F-DATOS: nuevo.

2026-09-30 — analyst — El texto grabado de cada clave: el del manifiesto del 24/8 si la clave está ahí (2.958); si no, el aiSentence
  (o sentence) del generador de marzo 2025. El texto de hoy se arma con la regla de texto hablado del script del 24/8
  (tts_text / spoken_from_choice_parts de scripts/regenerate_quiz_audios.py).
  why: es lo que pide la propuesta; el script declara paridad con el pipeline original, verificada contra los aiSentence.

2026-09-30 — analyst — R003 compara contra todas las respuestas aceptadas, toma igual contraída y larga, y acepta que uno contenga al otro.
  why: sin eso marcaba ~250 audios correctos (dicen otra respuesta aceptada, o sólo la respuesta); con eso quedan 7, todos reales.

2026-09-30 — analyst — Las 9 oraciones con barra del 077 no están en el árbol (el importador guardó la derivada, db/.import-metadata.json:
  «la del backup está desactualizada: 9»); sólo en db/quiz-templates.json → DOUBT-DATOS-PRODUCCION.

2026-09-30 — analyst — Choque con DOUBT-718-CLOZE (FEAT-OPMUL, abierta): R001 no cuenta espacios ni recortes de reescritura → DOUBT-DATOS-718.
