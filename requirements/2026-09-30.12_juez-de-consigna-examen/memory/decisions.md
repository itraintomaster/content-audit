# Decisions

Decisions future sessions should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-JEXAM / F-JEXAM: nuevo. Lo común a los cinco jueces vive acá (grupos A y B) y los
  otros cuatro lo citan; cada juez fija en su propio requirement qué grupos del examen le tocan y su meta (R006 de cada uno).
  why: evita repetir cinco veces el examen, el costo, el paralelo y las dudas de proveedor y presupuesto.

2026-09-30 — analyst — «Aprueba 178 de los 187 imposibles» (propuesta d-6-13) se lee como que los DEJA PASAR.
  why: verificado con el libro `.content-audit/evaluations/quiz-instruction-validator.jsonl` (último veredicto por
  subjectRef, 3–14/8): «cumple» en 181 de 187 imposibles CLOZE, en el de opción múltiple, en las verduras y en los 50
  buenos. Encaja con «se afinó para que dejara de marcar de más, y nunca se midió lo que deja pasar».

2026-09-30 — analyst — El examen no escribe en el libro del curso y reusa sólo veredictos de la MISMA versión (R002).
  why: en el curso gana el más reciente (F-EVCOST-R006); si el examen escribiera ahí, una versión reprobada quedaría
  vigente en 238 ejercicios. En el examen la versión es lo que se mide; no reabre DOUBT-IDENTIDAD-VERSION.

2026-09-30 — analyst — Medido sin consultar modelos: comparando lo que el juez de consigna leía el 14/8
  (audit-2026-08-14T14-39-40.json del archivo) con la base del 29/9, conservan veredicto unos 3.889 CLOZE si la consigna
  sale del tema (1.065 si sale de la copia del ejercicio); pendientes unos 7.400 (2.810 MC + ~4.590 CLOZE cambiados).
  why: mismos datos de entrada => misma huella; no leí el código del fingerprint. Queda como estimación.

2026-09-30 — analyst — La app muestra la consigna de la copia del ejercicio (mobile quiz.dart usa template.instructions) y
  la traducción y la miniteoría recién en la corrección (QuizFeedback). Copia y tema difieren en 9.923 de 11.287.
  why: base de DOUBT-CONSIGNA-QUE-VE-EL-ALUMNO y de lo que leen los jueces nuevos «con lo que ve el alumno».

2026-09-30 — analyst — Ritmo medido del juez de consigna: mediana 189 veredictos por hora de corrida (unos 19 s cada
  uno, de a una), sobre 15.351 registros del 3 al 14/8. Base de todos los costos del paquete C.

2026-09-30 — analyst — Numeración que citan los otros cuatro: examen de cada juez = su R006; examen del de consigna =
  F-JEXAM-R009; habilitación = F-JEXAM-R003; costo pendiente = R004; conjunto y candidato de la mesa = R005; a la vez = R006.
  why: los requirements 08 a 11 enlazan esas reglas; renumerar acá obliga a tocarlos.
