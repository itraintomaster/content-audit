# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer (paquete D) — Una copia del worktree para correr Main necesita ../ittm/pipeline/src/main/resources/
  vocabulary/lemmas_20k_words.txt (COCA_DATA_PATH del spaCy) o la caché .cache/spacy-nlp del 29/9: sin ellas los tokens
  salen sin frequencyRank y off-level-words marca 524 ejercicios (todo «fuera del EVP» en la banda fuerte).

2026-09-30 — qa-tester — `patch propose` rechaza una prueba de otra feature en la carpeta («references feature FEAT-SEMAF but patch targets FEAT-NIVFR»): let's como let quedó en el resolutor trazado a F-NIVFR-R003 y en la consulta del semáforo trazado a F-SEMAF-R004.
  why: la traza se valida contra el REQUIREMENT.md de la carpeta, no contra todas las features.
