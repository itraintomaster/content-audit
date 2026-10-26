---
name: emit
kind: script
script: change_reviewer_emit
entry_point: ../scripts/emit.sh
label: Emit validated mark
completion:
  produces:
    - kind: changeReviewVerdict
      pointer: "{run_dir}/changeReviewVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
