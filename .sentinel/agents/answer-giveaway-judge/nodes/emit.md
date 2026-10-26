---
name: emit
kind: script
script: answer_giveaway_judge_emit
entry_point: ../scripts/emit.sh
label: Emit validated verdict
completion:
  produces:
    - kind: answerGiveawayVerdict
      pointer: "{run_dir}/answerGiveawayVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
