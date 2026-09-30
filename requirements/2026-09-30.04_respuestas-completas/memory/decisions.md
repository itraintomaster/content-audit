# Decisions

Architectural decisions and escalation resolutions that future sessions
should not re-litigate. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Código FEAT-RESPC / F-RESPC: nuevo.

2026-09-30 — analyst — La comparación del corrector sale del código, no del 077: be/domain/quiz/pkg/shared/cloze_sentence_part.go
  (Evaluate/normalize): separa sólo en «|», minúsculas, saca espacios, ! ? : ; , . - _ ' " ` ′ ‘ ’ “ ” y tildes (desde #884, 19/5/2025).
  why: el pedido decía «confirmalo con el brief 077 y la propuesta», pero el LEEME del examen prohíbe abrir el 077 (tiene el oro);
  se confirmó con la propuesta y el código. El caso 22c5 del oro («café») contradice al código.

2026-09-30 — analyst — La consigna manda: «Usa $short forms$» libera la larga; «Usa $long forms$» libera la contraída;
  «respuesta corta» no pide forma. Las dos negaciones cortas de be se exigen siempre (R003).

2026-09-30 — analyst — Gravedad propuesta: alta si la forma que falta está en la miniteoría o el enunciado; baja si es la larga en
  respuesta corta; media el resto. Es determinista; no calca al 077 (33/83/12 en su mitad).
