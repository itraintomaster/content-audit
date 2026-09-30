# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

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
