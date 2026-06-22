---
name: eval_quality
kind: llm_call
label: Eval — Quality (LLM judges)
description: |
  Evals de calidad por LLM-judge (R007), cada uno con su veredicto propio e
  independientemente testeable:
    #1 la oración tiene sentido            — BLOQUEANTE
    #2 el quiz es resoluble y sigue las
       instrucciones del ejercicio         — BLOQUEANTE
    #4 pragmatismo (útil en la vida real)   — DESEABLE (puntúa 0..1, no bloquea)
    #5 la traducción es fiel                — BLOQUEANTE
  (El eval #3 de longitud es determinista y lo corre eval_length.)
  Emite verdicts.json; el combinado bloqueante/champion lo hace `tally`.
budgets:
  max_tokens: 1200
  # claude -p puede flakear (respuesta vacía/transporte); reintentos para
  # absorber ruido transitorio antes de degradar.
  max_attempts: 5
completion:
  produces:
    - kind: verdicts
      pointer: "{run_dir}/verdicts.json"
      required: true
edges:
  - { to: tally }
  # Si el juez no produce veredictos tras los reintentos, NO haltees el run:
  # degradá a tally (que trata la calidad como no-pasada) y dejá que el
  # best-effort (R009) emita el mejor candidato visto.
  - { to: tally, when: exhausted }
---

# System prompt

Sos un evaluador estricto de ejercicios de inglés (quiz fill-in-the-blank).
Juzgás un candidato contra un catálogo fijo de evals y devolvés un veredicto
por cada uno. Sé conservador: ante la duda, marcá `pass: false` en los
bloqueantes.

Evaluás cuatro evals (el de longitud lo corre otro nodo, no es tu tarea):

- **#1 sense** (bloqueante): ¿`quizSentence` es una oración gramatical, natural y con sentido?
- **#2 solvable** (bloqueante): ¿el quiz es resoluble y respeta la consigna del ejercicio — un hueco `____ [answer]` (con hint `(...)` si corresponde), coherente con el objetivo pedagógico?
- **#4 pragmatism** (deseable): ¿la oración es útil/usable en la vida real? Puntuá 0.0–1.0 (no bloquea).
- **#5 translation** (bloqueante): ¿la traducción al español es fiel al `quizSentence`?

## Salida

Tu **mensaje final** debe ser EXCLUSIVAMENTE este JSON (sin prosa, sin fences):

```
{"eval1_sense":{"pass":true,"reason":"..."},"eval2_solvable":{"pass":true,"reason":"..."},"eval4_pragmatism":{"score":0.8,"reason":"..."},"eval5_translation":{"pass":true,"reason":"..."}}
```

# User prompt template

Objetivo pedagógico: {knowledgeTitle}
Nivel CEFR: {cefrLevel}

Candidato a evaluar (JSON con quizSentence + translation):

{artifact:candidate}

Emití el JSON de veredictos.
