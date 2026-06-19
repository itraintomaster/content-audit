---
name: emit
kind: script
script: lemma_absence_emit
entry_point: ../scripts/emit.sh
label: Emit QuizCandidate
description: |
  Salida del run: produce el artifact `quizCandidate` (lo que el adaptador
  Java AgentCandidateParser lee del RunState). Emite el CHAMPION — el mejor
  candidato visto (R009 best-effort): en éxito es el que pasó todos los
  bloqueantes; en exhaustion es el de más bloqueantes pasados. Exactamente un
  QuizCandidate {quizSentence, translation} (R003).
completion:
  produces:
    - kind: quizCandidate
      pointer: "{run_dir}/quizCandidate.json"
      required: true
edges:
  - { to: __end__ }
---
