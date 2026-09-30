# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — analyst — `analyze` sin tocar el checkout: `java -Duser.dir=<dir>` con symlinks `<dir>/analysis` y `<dir>/../ittm`,
  `CONTENT_AUDIT_HOME` en el scratchpad y `--exclude-analyzers quiz-instruction --instruction-budget 0` (script `run_analyze.sh`).
  why: Main arma rutas desde user.dir y el caché de spaCy se escribe en user.dir/.cache; así nada cae en el checkout ni consulta modelos.
