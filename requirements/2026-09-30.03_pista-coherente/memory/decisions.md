# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester — F-PISTA-J001 en audit-domain, paquete hintcoherence: el analizador y sus colaboradores son package-private y los puertos se simulan; mismo criterio que F-QINST-J001/J002 y F-LREC.
2026-09-30 — qa-tester — Umbral: una prueba propia (tres cuartos exactos alcanzan; One y ones, 12 de 20, sin hallazgo).
2026-09-30 — qa-tester — «bad» → «the best» a la mesa sigue la opción A de DOUBT-PISTA-CUANDO-MESA (abierta): si José elige otra, cambia esa prueba.
2026-09-30 — qa-tester — isPastParticiple se traza a R002 (inv. 2): «'ve swum» y «has not cut» tienen auxiliar porque sigue un participio.

2026-09-30 — architect — hint-coherence compara con AuditableQuiz.tokens (lemas del tokenizador compartido) más flexión regular y una
  tabla cerrada de formas irregulares (IrregularFormsCatalog, raíz de audit-domain; adaptador en vocabulary-infrastructure). No
  lematiza nada nuevo ni toca el lematizador compartido (DOUBT-PISTA-LEMATIZADOR).
2026-09-30 — architect — El umbral de tres cuartos se cuenta en onKnowledge (el motor lo llama antes que a los ejercicios del tema).
2026-09-30 — architect — Resolución por tipo de falta: HINT_WORD_ABSENT → mesa; el resto → regla con la pista propuesta ya calculada
  (recomendación de DOUBT-PISTA-CUANDO-MESA, en un solo mapeo). Con el prototipo del analista: 60 de 87 a la mesa.

2026-09-30 — analyst — DOUBT-PISTA-UMBRAL → B (José, 30/9): se exige desde tres cuartos del tema. R001 y el journey dicen tres cuartos.

2026-09-30 — analyst — DOUBT-PISTA-RESPUESTA → A (José, 30/9): si lo que está mal es la respuesta, va a la mesa aunque haya que regrabar.
  Una regla instantánea no sabe qué enseña el tema → DOUBT-PISTA-CUANDO-MESA (recomendado: mesa cuando la palabra de la pista
  no está en la respuesta; regla para pista que falta, verbo sin anunciar, sujeto y not).

2026-09-30 — analyst — DOUBT-PISTA-TRANSFORMAR → A (José, 30/9): la oración que se transforma no cuenta como pista.

2026-09-30 — analyst — Lematizador compartido (pedido del orquestador: no arreglarlo en esta ronda) → DOUBT-PISTA-LEMATIZADOR.
  Medido 29/9 con en_core_web_sm: de 4.490 palabras de pista, 761 sólo por lema, 75 con lema errado pero flexión regular
  (hated→hat), 24 sólo con una tabla cerrada de irregulares (good→well/better, swum). Sólo hint-coherence usa lemas en el paquete A.

2026-09-30 — analyst — Código FEAT-PISTA / F-PISTA; nombre `hint-coherence`, el del brief 089.

2026-09-30 — analyst — Pista = paréntesis pegado al hueco (F-QSENT-R007) o, si la respuesta es la oración entera, el paréntesis del
  enunciado y el enunciado con «/» [ASSUMPTION]. No son pista: tiempo verbal, instrucción («be extra polite»), nota de contexto.
  why: sin excluirlas, «(now)», «(in general)» o «(use the present simple)» daban decenas de falsos «verbos».

2026-09-30 — analyst — R001 compara ejercicios del mismo tipo (hueco dentro de la oración / respuesta = oración entera).
  why: los temas fusionados mezclan tipos; en «El posesivo» 20 transformaciones sin pista no son la falta de los 20 huecos con pista.

2026-09-30 — analyst — «Comparar por lema» (brief 089) no alcanza: en_core_web_sm da hated→hat, worried→worried (ADJ), gerundios
  como sustantivo y no lematiza drunk/swum/rang. R002 acepta la forma flexionada; el diseño tiene que reconocer irregulares.

2026-09-30 — analyst — La categoría «pista» del examen son los 188 IMPOSIBLE del 077, no sólo los de pista: la meta se propone
  sobre los anotados PISTA_* (15 de 46 en escribir) y aparte sobre la categoría entera (~4 de 10).

2026-09-30 — analyst — R004 sólo en una dirección («not» en la pista ⇒ respuesta negativa): una respuesta negativa sin «not»
  en la pista puede venir de la consigna («Oraciones negativas») o del diálogo.
