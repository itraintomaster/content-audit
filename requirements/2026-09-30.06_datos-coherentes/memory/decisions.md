# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
