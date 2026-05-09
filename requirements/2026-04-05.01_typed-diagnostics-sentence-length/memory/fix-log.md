# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-05-09 — test-writer — SentenceLengthDiagnosis usa getters JavaBean (getDelta(), getTokenCount()), no accessors de record
  why: sentinel.yaml dice type: record pero el código generado es una clase regular con getXxx(); los accessors sin prefijo compilaban en ECJ pero fallaban en runtime
