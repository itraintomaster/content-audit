---
name: emit
kind: script
script: quiz_instruction_emit
entry_point: ../scripts/emit.sh
label: Emit Quiz Instruction Verdict
description: |
  Produce el artifact final `quizInstructionVerdict`. Emite el veredicto
  canónico validado o, si el juez agotó los intentos de forma, un fallo
  conservador y explícito de infraestructura con confidence=0.
completion:
  produces:
    - kind: quizInstructionVerdict
      pointer: "{run_dir}/quizInstructionVerdict.json"
      required: true
edges:
  - { to: __end__ }
---
