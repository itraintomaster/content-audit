# Fix Log

Fixes that worked, with a short why. Future agents hitting the same
symptom read this before trying new approaches. Newest entries on top.

<!-- entries below -->

2026-10-01 — qa-tester — El aviso «reglas sin prueba en el parche» de la simulación (TestCoverageValidator) cuenta
  sólo pruebas declarativas, no handwrittenTests: la cobertura real la da `tool listModules --with-coverage --root
  <modelo simulado>` (las 8 reglas [covered]).

2026-09-30 — architect — `patch propose --replace -i <sentinel.yaml de ronda1/integracion>` no fusiona: domainLinks y
  glossarySuggestions quedan (comparado con `arq-B2/comparar.py`); sólo agrega `suppressed: false` en patterns.
  why: el bug 08 de Sentinel pierde domainLinks al fusionar; con --replace no hay fusión.

2026-09-30 — architect — Simulado en memoria (arq-B2/sim.sh, Simular de arq-B): al aplicar, `localtoolsengine` queda sin
  `allowedClients: [audit-cli]` (bug 10), igual que `judgingengine` en la ronda 1. Va como gancho para el integrador.

2026-09-30 — architect — `patch propose` y `patch validate` no resuelven tipos (requiresInject, firmas, `extends`), pero sí
  rechazan un `implements` hacia una interfaz que no está ni en sentinel.yaml ni en el mismo parche; y aceptan `_change: modify`
  sobre un módulo que no existe. Para parches que se aplican en cadena: cada uno implementa sólo interfaces propias o ya aplicadas.

2026-09-30 — architect — El buscador de Maven Central (`solrsearch`) daba 6.6 como última de `org.languagetool:language-en`;
  `repo1.maven.org/…/maven-metadata.xml` dice 6.8 (5/5/2026). Para la versión vigente, leer el maven-metadata.xml.

2026-09-30 — architect — En el YAML de un parche, un campo llamado `yes` o `no` se lee como booleano (YAML 1.1): se usaron
  `yesProbability` y `noProbability`.
