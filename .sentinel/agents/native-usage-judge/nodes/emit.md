---
name: emit
kind: script
script: native_usage_judge_emit
entry_point: ../scripts/emit.sh
label: Emit validated verdict
completion:
  produces:
    - kind: nativeUsageVerdict
      pointer: "{run_dir}/nativeUsageVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
