---
name: emit
kind: script
script: pragmatism_judge_emit
entry_point: ../scripts/emit.sh
label: Emit Communicative Verdict
description: |
  Emite únicamente el veredicto validado. La salida ausente o inválida se
  señala como outputUnusable; nunca se transforma en un PASS.
completion:
  produces:
    - kind: pragmatismVerdict
      pointer: "{run_dir}/pragmatismVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
