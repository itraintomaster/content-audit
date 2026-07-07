---
name: eval_language
kind: script
script: knowledge_titles_eval_language
entry_point: ../scripts/eval_language.sh
label: Eval — Language Guard
description: |
  Eval #4, bloqueante y DETERMINISTA: guardia de idioma conservadora.
  Título e instructions van en ESPAÑOL; el inglés solo citado.
    - Título: fuera de los términos citados ('be', 'going to'…) y las glosas
      entre paréntesis, no puede tener palabras inglesas inequívocas
      (sentences, review, past…) — el título va en español.
    - Instructions: fuera de los términos citados, deben mostrar señales
      claras de español (≥2 palabras funcionales o algún carácter acentuado)
      y no acumular palabras inglesas sin citar — las instructions van en
      español.
  Es un guard-rail léxico deliberadamente conservador: la naturalidad fina la
  juzga eval_quality (#7). Computa, no rutea: publica title_lang_ok /
  instructions_lang_ok al data bus.
edges:
  - { to: eval_distinct }
---
