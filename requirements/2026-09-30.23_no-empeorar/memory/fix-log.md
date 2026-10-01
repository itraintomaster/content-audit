# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer (construcción E) — R004 inv. 1 compara lo que se escucha, no el texto: sin mayúsculas, espacios
  de más ni la forma del apóstrofo (`GateRules.heardAlike`, 485f2244, `HeardAlikeGateIntegrationTest`). why: en el control
  sobre la copia del 29/9 la regla de multiple-choice-validity pone en mayúscula las opciones de 75 ejercicios de A1 y la
  compuerta pedía regrabar 62 audios; en 61 la grabación ya decía la oración con mayúscula y en el otro sólo cambiaba
  la mayúscula. Una contracción, una palabra o el signo del final siguen pidiendo regrabar.

2026-09-30 — analyst — `analyze` sin tocar el checkout: `java -Duser.dir=<dir>` con symlinks `<dir>/analysis` y `<dir>/../ittm`,
  `CONTENT_AUDIT_HOME` en el scratchpad y `--exclude-analyzers quiz-instruction --instruction-budget 0` (script `run_analyze.sh`).
  why: Main arma rutas desde user.dir y el caché de spaCy se escribe en user.dir/.cache; así nada cae en el checkout ni consulta modelos.
