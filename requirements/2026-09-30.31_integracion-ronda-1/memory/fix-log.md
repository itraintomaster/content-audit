# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — qa-tester (integrador) — Fusión de pruebas sin --replace: PatchMerger sólo pierde domainLinks y glossarySuggestions
  de las implementaciones que fusiona, y ninguna de las tocadas los tenía; comparar el parche antes y después, sin las
  pruebas (scratchpad/integrador-1/comparar.py), dio 0 pérdidas.
2026-09-30 — integrador — Simular un apply con Sentinel arreglado sin tocar el jar: compilar una copia de PatchApplier.java con el
  arreglo contra el jar y ponerla antes en el classpath (scratchpad/integrador-1/arreglo, simular-cadena.sh arreglo).

2026-09-30 — integrador — Rebasar con `patch propose --replace` y el parche entero armado desde HEAD (scratchpad/integrador-1/rebase.py):
  sin --replace, PatchMerger pierde domainLinks (bug 08 de Sentinel) y la fusión conserva lo que había que sacar.
2026-09-30 — integrador — `-i` de cada propose = el modelo que deja el anterior (simulado con la librería): OMVAL y MCVALE implementan
  interfaces que declara la integración, y contra el sentinel.yaml a secas su implements no resuelve.
2026-09-30 — integrador — Al modificar una interfaz, patch apply toma `sealed` del parche (false si no lo dice): repetir sealed true.
  Y al agregar un paquete descarta su allowedClients (pasó con findingengine de 0.3; pasa con offlevelwords, topicwords y
  judgingengine): la restricción queda sólo en el diseño.
