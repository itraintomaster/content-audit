---
name: gate_verdict
kind: gate
gate: single_answer_judge_gate_verdict
entry_point: ../scripts/gate_verdict.sh
label: Gate Verdict Shape
description: |
  Valida verdicts.json contra el esquema cerrado de singleAnswerVerdict
  (schema/verdict.schema.json): alternatives y rationale, sin claves de más ni
  de menos, con sus tipos. Invariantes: gap es uno de los huecos que llegaron
  en acceptedAnswers («Hueco 1» → 1); kind OFFERED_BY_INSTRUCTION o
  DIFFERENT_VALID; severity BLOCKING, HIGH o MEDIUM (F-UNICA-R004); cada
  respuesta una sola vez por hueco, comparada como la compara el corrector.
  Canonicaliza únicamente una salida válida —enums en mayúsculas, textos en una
  línea— en validated-verdict.json. Ante JSON malformado, tipos inválidos o
  contradicciones vuelve a evaluate, que recibe el motivo.
  Una clave de más que viene vacía (null o texto en blanco) no dice nada y no
  cuenta; con contenido, frena.
  Una coma colgante antes de cerrar un objeto o una lista tampoco: se lee el
  JSON sin ella.
  Un elemento de relleno en una lista, sin texto y con lo demás vacío o NONE,
  tampoco: se descarta.
  Un `undefined` suelto, que no es JSON, se lee como null, y la clave de
  relleno que lo traía se descarta.
edges:
  - { to: emit, when: passed }
  - { to: evaluate, when: failed }
---
