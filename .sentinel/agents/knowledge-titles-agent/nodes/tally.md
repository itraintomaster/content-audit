---
name: tally
kind: script
script: knowledge_titles_tally
entry_point: ../scripts/tally.sh
label: Tally Verdicts + Champion
description: |
  Combina los veredictos deterministas (#1 título ≤28, #2 instructions ≤70,
  #3 redundancia léxica, #4 idioma, #5 distinción — via data bus) con los del
  juez (#6 fidelidad, #7 idioma natural, #8 no-redundancia semántica — via
  verdicts.json). Mantiene el champion (mejor candidato visto: más bloqueantes
  pasados, desempate por estilo #9) y escribe carry_forward.md con feedback
  accionable en fallo. Publica all_blocking_pass al data bus; el routing lo
  decide route_verdict.
edges:
  - { to: route_verdict }
---
