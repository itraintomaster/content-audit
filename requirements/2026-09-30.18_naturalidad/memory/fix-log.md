# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — Con Ollama, forzar la salida con `format` (enum de un valor) da la probabilidad real de cada token
  (antes de la restricción) pero con la tokenización que elige el muestreador, no la canónica («"S»+«he», « »+«English»):
  errores de 5 a 15 nats por palabra. Leer de a un token con top_logprobs=20 y la palabra entera evita eso, con tope.

2026-09-30 — analyst — Leído así, el modelo castiga la contraída («We're» fuera de las 20): la cola del 5 % se llenaba de
  contracciones de A1. Pasarlas a la forma larga antes de medir lo corrigió (sólo hace falta con SLOR).
