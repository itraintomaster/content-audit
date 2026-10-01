# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — developer — Dos `exam run` en la misma carpeta no son dos corridas: el examen guarda sus vectores en
  `.content-audit/evaluations/exam_…` y el segundo los reusa. Para «dos corridas marcan las mismas copias», cada una en
  su carpeta de estado (`WORKDIR=… correr.sh`) y sin libro.
2026-10-01 — developer — El libro guarda un vector por texto (contentFingerprint): un ejercicio con el mismo ejercicio
  terminado que otro no tiene renglón propio (…2405); al leer vectores del libro, saltear esos pares.

2026-10-01 — developer — El ejercicio terminado del renderer (FILL) es igual al del prototipo en los 11.287
  sólo después de pegar contracciones y signos y sacar el «-» de la respuesta que no dice nada (813 → 331 → 0).

2026-09-30 — analyst — nomic-embed-text pide un prefijo de tarea; se midió con «clustering: ». Sin misma respuesta, el
  coseno solo no separa: la mediana de los pares del mismo tema es 0,66 y el p99,9 0,92.
