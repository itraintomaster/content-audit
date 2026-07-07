---
name: eval_redundancy
kind: script
script: knowledge_titles_eval_redundancy
entry_point: ../scripts/eval_redundancy.sh
label: Eval — Title Redundancy
description: |
  Eval #3, bloqueante y DETERMINISTA: el título no debe repetir palabras que el
  alumno ya ve al lado. Dos chequeos léxicos:
    (a) título ↔ topic: ninguna content-word del título (fuera de stopwords
        inglesas) puede aparecer en el topicLabel;
    (b) título ↔ instructions: los términos CITADOS en las instructions del
        candidato (entre comillas: 'be', 'short forms'…) no pueden reaparecer
        en el título — si las instructions ya lo nombran, el título no lo repite.
  La redundancia SEMÁNTICA cross-idioma (título en inglés vs instructions en
  español) la juzga eval_quality (#8). Computa, no rutea: publica redundancy_ok
  (+ palabras ofensoras) al data bus.
edges:
  - { to: eval_language }
---
