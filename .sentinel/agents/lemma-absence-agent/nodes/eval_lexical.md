---
name: eval_lexical
kind: script
script: lemma_absence_eval_lexical
entry_point: ../scripts/eval_lexical.sh
label: Eval — Lexical Non-Regression
description: |
  Criterio #7 de F-LASAG-R007, bloqueante y DETERMINISTA: no-regresión léxica.
  El candidato no debe introducir lemas flaggeados (mal ubicados para el nivel
  o fuera de catálogo) ausentes en la oración ORIGINAL: flags(candidato) debe
  ser SUBCONJUNTO de flags(original). Los flags preexistentes que la edición no
  tocó no bloquean (residuo inevitable no vuelve la tarea infalible); solo
  bloquea un lema flaggeado nuevo.

  Reusa el MISMO análisis léxico del audit (no una réplica) vía
  `content-audit lexis --quiz-sentence <DSL> --level <CEFR>` (FEAT-CLEX),
  análogo directo a como `eval_length` reutiliza `content-audit tokens` para
  la longitud. Compara el original (`inputs.quizSentence`) contra el candidato
  (`{run_dir}/candidate.json`, ya canonicalizado por gate_candidate).

  Robustez: a diferencia de otros evals deterministas, un fallo de la consulta
  `lexis` (exit != 0 o salida no parseable) NO es soft-pass — bloquea
  (`lexical_ok=False`) con motivo técnico, porque este eval existe justamente
  para no dejar pasar en silencio un candidato no verificado (caso live
  2026-07-22: remitted→transferred sin detección). Computa, no rutea: publica
  `lexical_ok` (+ detalle de lemas nuevos con su nivel) en el data bus; el
  routing lo decide route_verdict tras combinar con los demás evals en `tally`.
edges:
  - { to: eval_quality }
---
