---
name: load_context
kind: script
script: knowledge_titles_load_context
entry_point: ../scripts/load_context.sh
label: Load Knowledge Context
description: |
  Carga al data bus todo el contexto del knowledge a mejorar, leyendo la db
  del proyecto (fuente de verdad del contenido):
    - originalTitle / originalInstructions (label + instructions actuales)
    - topicLabel / levelLabel (cadena KNOWLEDGE → TOPIC → MILESTONE)
    - siblingTitles (labels de los hermanos del mismo topic, uno por línea)
    - sampleQuizzes (hasta 5 oraciones de quiz-templates del knowledge, para
      que el generador y el juez sepan QUÉ practica el ejercicio realmente)
    - carryForward (feedback del intento previo, escrito por tally/gate)
  También borra un verdicts.json remanente para que un reintento no combine
  el candidato nuevo con veredictos viejos. Es el initial node y el destino
  del retry.
edges:
  - { to: generate }
---
