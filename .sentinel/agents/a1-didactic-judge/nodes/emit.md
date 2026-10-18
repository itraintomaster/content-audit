---
name: emit
kind: script
script: a1_didactic_judge_emit
entry_point: ../scripts/emit.sh
label: Emit validated verdict
completion:
  produces:
    - kind: a1DidacticVerdict
      pointer: "{run_dir}/a1DidacticVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
