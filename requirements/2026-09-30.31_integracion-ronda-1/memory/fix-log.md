# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-09-30 — developer — generate no agregó a los stubs de configuración de C el describe() de SelfDescribingConfig, que su
  interfaz extiende: no compilaban. Se les sumó el método sin implementar, como el resto del stub (posible bug de Sentinel).
2026-09-30 — developer — QuizDiagnoses, CourseDiagnoses, sus Default* y AuditNode no llevan @Generated: generate no los
  toca y se editan a mano según sentinel.yaml, como en 0.3.
2026-09-30 — developer — Aridades de la ronda en los tests: PadRonda1.java (scratchpad/impl-ronda1-comun, JavaParser del jar
  de Sentinel) completa 447 llamadas en 72 archivos; a la ficha le pasa su nombre único como nombre en castellano, porque
  con null el catálogo la rechaza. Siete journeys arman GetCmd por reflexión con 8 argumentos: se ajustan a mano a 11.
2026-09-30 — developer — La prueba de F-HALL-R013 compara diagnósticos contra JSON grabados antes de la ronda: hay que sacar del
  actual los campos nuevos vacíos (lugares de la ronda, bands y exposures de lemma-count) antes de comparar.
2026-09-30 — developer — En un test, una variable local llamada `java` tapa el paquete en `java.nio...` (cannot find symbol).

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
