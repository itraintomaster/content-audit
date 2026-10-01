# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — equipo de una tarea (FILAS) — `mvn -o -pl revision-domain` compila contra el audit-domain viejo del
  repositorio local y da cientos de «package does not exist». Arreglo: `-pl <módulo> -am`, que arma las
  dependencias del reactor.
2026-10-01 — equipo de una tarea (FILAS) — `sentinel verify -i <copia>` falla si la copia no está en la raíz: las
  rutas de `definitions` se resuelven contra la carpeta del YAML. Arreglo: copia temporal en la raíz, borrada después
  (verify puede reescribir el YAML que lee, bug 04).
2026-10-01 — equipo de una tarea (FILAS) — patch propose rechaza una prueba con trazabilidad de otro feature
  («references feature FEAT-RONDA1 but patch targets FEAT-FILAS»). Arreglo: un parche por feature, propuesto en la
  carpeta de ese feature, con --replace. Las líneas base de esas carpetas no cambian.
