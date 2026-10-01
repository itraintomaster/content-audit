# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-10-01 — architect (impl A) — R006 es un método más de HintAnswerCheck, checkRewrite(AuditableQuiz), y no un
  colaborador nuevo: necesita lo que ya tiene DefaultHintAnswerCheck (tokens de la oración guardada, tabla de irregulares,
  comparación de R002). Sin puerto, sin inyección, sin cambiar constructores. El analizador la llama sólo en WHOLE_SENTENCE;
  R002 inv. 2 sigue sólo en huecos: las dos nunca miran lo mismo.
2026-10-01 — architect (impl A) — El valor nuevo es REWRITE_WORD_NOT_ANNOUNCED y no WORD_NOT_FROM_SOURCE (la propuesta
  del coordinador): sigue a VERB_NOT_ANNOUNCED y SUBJECT_NOT_ANNOUNCED, y la fuente no es sólo la oración de partida (también
  la pista y la consigna). Reusar VERB_NOT_ANNOUNCED mentiría: R006 marca sustantivos y adverbios. Un hallazgo por palabra,
  marca = lema; proposedHint null (no hay arreglo por regla).
2026-10-01 — qa-tester (impl A) — R006 con 5 pruebas y no una: el positivo (24f7), la consigna como fuente (4186), lo que
  la reescritura agrega sola (447b), el hallazgo del analizador (44f8) y que un hueco dentro de la oración sigue siendo de
  R002 (215c), que es lo que impide un hallazgo doble. Los 4 falsos no van como prueba: son límite del REQUIREMENT.

2026-10-01 — analyst (impl A) — R006 es AUTO_VALIDATED: el coordinador la aprobó el 1/10 por la precisión medida (56 de
  60 marcas reales, 25 de 30 en una muestra nueva). Va a la mesa siempre: una regla no sabe si se arregla la respuesta
  (la reescritura fiel, con audio nuevo) o el enunciado (DOUBT-PISTA-RESPUESTA).
2026-10-01 — analyst (impl A) — R006 no reabre DOUBT-PISTA-TRANSFORMAR: la oración que se transforma sigue sin ser pista
  para R002. R006 mira al revés (lo que la respuesta trae sin que nada lo anuncie) y cuenta la consigna; por eso «I want
  a sandwich.» → «I would like a sandwich.» (2def) pasa, que era el ejemplo de por qué José eligió A.
2026-10-01 — analyst (impl A) — R006 no entra al journey F-PISTA-J001: un camino nuevo cambiaría el journey generado (un
  path-4 que escribir) y el validador no exige que cada regla esté en un gate (en FEAT-QINST, R015 a R017 y R019 no lo
  están). Su prueba son sus handwrittenTests.
2026-10-01 — analyst (impl A) — Afuera de R006: el tramo opcional entre paréntesis de una respuesta («Peter was admired
  (by Mary).»); lo arregla el corrector, que va a leer el paréntesis como opcional (coordinador, 1/10).

2026-10-01 — developer (impl A) — R001 es la unión: un ejercicio sin pista tiene hallazgo si su grupo estructural O el
  tema entero (todos los de hueco para escribir) llega a tres cuartos. Medido sobre el curso del 29/9:
    (i) tipo estructural, lo de antes: 7 missing-hint; examen escribir 14 de 46, 9 de 15 anotados, 2 fuera del oro.
    (ii) por modo, como el prototipo (el tema entero): 14; 20 de 46, 15 de 15, 3 fuera del oro.
    (iii) unión: 14; 20 de 46, 15 de 15, 3 fuera del oro. En este curso marca lo mismo que (ii).
  Las marcas que cambian entre variantes son 7, todas nuevas (ninguna se pierde), revisadas a mano: 6 reales (los huecos
  sin pista de temas de transformaciones que llevan todas su pista: 2572, 2575 y 257d en Adverbios de frecuencia, 2589 y
  2591 en Frecuencia: parte 2, 2e83 en Oraciones con than; el alumno no puede saber qué adverbio o qué adjetivo va) y 1
  falsa (2596: «(100%)» anuncia always, y el lector lo tomaba por una nota; ahora es una pista descriptiva).
  why: la unión anda como (ii) en la muestra del curso sin perder anotados, nunca pierde una marca del tipo estructural y
  deja la prueba de El posesivo en verde (sus 20 transformaciones sin pista no llegan ni por tipo, 0 de 20, ni por tema,
  20 de 40). El diagnóstico lleva los números del grupo que decidió y la evidencia lo dice («38 de 40 ejercicios del tema
  la llevan», sin «del mismo tipo»). Con el porcentaje como pista: 13 missing-hint; examen 20 de 46, 15 de 15, 2 fuera.

2026-09-30 — developer (impl A) — R001 cuenta por tipo estructural, como dice la regla y su prueba de El posesivo:
  hueco dentro de la oración, o respuesta que es la oración entera (sin texto después del hueco y la respuesta de dos
  palabras o más tras un enunciado vacío o cerrado; en REWRITE, también la respuesta que es una oración).
  why: el prototipo del analista agrupaba por modo (REWRITE = oración entera) y así encontraba 6 anotados más (los huecos
  sin pista de Adverbios de frecuencia, Frecuencia: parte 2 y 2e83); con el tipo literal no salen. Escalado al orquestador.
2026-09-30 — developer (impl A) — La pista de un hueco: el paréntesis pegado (después o antes); si no, uno corto que
  cierra el enunciado («(always)», «(brown / large)»); si no, las opciones con «/» junto al hueco («Our / ours ___»).
  Paréntesis de 4 palabras o más sin «/» = pista descriptiva (cuenta para R001, no se compara); en oración entera, un
  paréntesis cuyas palabras están en la oración de partida apunta a ella («(the fish)») y tampoco se compara.
2026-09-30 — developer (impl A) — R002 inv. 2 (verbo sin anunciar) sólo cuando la respuesta no es una oración: en una
  oración entera los verbos vienen de la oración que se reescribe (aplicado a todo daba 500 marcas falsas).
2026-09-30 — developer (impl A) — Ids de las reglas de la ficha: missing-hint, hint-words, hint-subject, hint-negation;
  la marca de la identidad es la palabra en minúscula (vacía en missing-hint).

2026-09-30 — integrador — Rebasado sobre el sentinel.yaml real y FEAT-RONDA1 (propose --replace, exit 0): sin cambios de estructura.

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
